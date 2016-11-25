package butter.droid.base.subs;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.FileUtils;

public class SubtitleDownloader {

    private final SubsProvider subsProvider;
    private final Media media;
    private final WeakReference<Context> contextReference;

    private String subtitleLanguage;
    private WeakReference<ISubtitleDownloaderListener> listenerReference;

    public SubtitleDownloader(@NonNull SubsProvider subsProvider, @NonNull Context context, @NonNull StreamInfo streamInfo, @NonNull String language) {
        if (language.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) throw new IllegalArgumentException("language must be specified");

        this.subsProvider = subsProvider;
        contextReference = new WeakReference<>(context);
        subtitleLanguage = language;

        media = streamInfo.getMedia();
        if (media == null) throw new IllegalArgumentException("media from StreamInfo must not null");
    }

    public void downloadSubtitle() {
        if (listenerReference == null) throw new IllegalArgumentException("listener must not null. Call setSubtitleDownloaderListener() to sets one");
        if (contextReference.get() == null) return;
        subsProvider.download(media, subtitleLanguage, new Callback() {
            @Override
            public void onFailure(Request request, IOException exception) {
                onSubtitleDownloadFailed();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                onSubtitleDownloadSuccess();
            }
        });
    }

    public void parseSubtitle(@NonNull File subtitleFile) {
        if (listenerReference == null) throw new IllegalArgumentException("listener must not null. Call setSubtitleDownloaderListener() to sets one");
        if (listenerReference.get() == null) return;
        ISubtitleDownloaderListener listener = listenerReference.get();
        SubtitleParseTask task = new SubtitleParseTask(subtitleLanguage, listener);
        task.execute(subtitleFile);
    }

    public void setSubtitleDownloaderListener(ISubtitleDownloaderListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not null");
        listenerReference = new WeakReference<>(listener);
    }

    public static File getDownloadedSubtitleFile(
            @NonNull Context context,
            @NonNull Media media,
            @NonNull String language) throws FileNotFoundException {
        if (language.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) throw new IllegalArgumentException("language must be specified");

        File subtitleFile = new File(
                SubsProvider.getStorageLocation(context),
                media.videoId + "-" + language + ".srt");

        if (subtitleFile.exists()) return subtitleFile;
        throw new FileNotFoundException("Subtitle file does not exists");
    }

    /**
     * Invoked when subtitle download finished successfully.
     */
    private void onSubtitleDownloadSuccess() {
        if (contextReference.get() == null) return;
        if (listenerReference.get() == null) return;

        Context context = contextReference.get();
        ISubtitleDownloaderListener listener = listenerReference.get();

        try {
            File subtitleFile = getDownloadedSubtitleFile(context, media, subtitleLanguage);
            SubtitleParseTask task = new SubtitleParseTask(subtitleLanguage, listener);
            task.execute(subtitleFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            listener.onSubtitleDownloadCompleted(false, null);
        }
    }

    /**
     * Invoked when subtitle download failed.
     */
    private void onSubtitleDownloadFailed() {
        subtitleLanguage = SubsProvider.SUBTITLE_LANGUAGE_NONE;
        if (listenerReference.get() == null) return;
        ISubtitleDownloaderListener listener = listenerReference.get();
        listener.onSubtitleDownloadCompleted(false, null);
    }

    private class SubtitleParseTask extends AsyncTask<File, TimedTextObject, TimedTextObject> {
        String subtitleLanguage;
        WeakReference<ISubtitleDownloaderListener> listenerReference;

        public SubtitleParseTask(String language, ISubtitleDownloaderListener listener) {
            subtitleLanguage = language;
            listenerReference = new WeakReference<>(listener);
        }

        @Override
        protected TimedTextObject doInBackground(File... files) {
            for (File file : files) {
                try {
                    TimedTextObject text = parseAsTimedTextObject(file);
                    publishProgress(text);
                }
                catch (FileNotFoundException e) {
                    if (e.getMessage().contains("EBUSY")) {
                        try {
                            TimedTextObject text = parseAsTimedTextObject(file);
                            publishProgress(text);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                            return null;
                        }
                    }
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(TimedTextObject... values) {
            super.onProgressUpdate(values);
            for (TimedTextObject timedTextObject : values ) {
                if (listenerReference.get() == null) break;
                listenerReference.get().onSubtitleDownloadCompleted(true, timedTextObject);
            }
        }

        private TimedTextObject parseAsTimedTextObject(File file) throws IOException {
            FileInputStream fileInputStream = new FileInputStream(file);
            FormatSRT formatSRT = new FormatSRT();
            TimedTextObject result = formatSRT.parseFile(
                    file.toString(),
                    FileUtils.inputstreamToCharsetString(
                            fileInputStream,
                            subtitleLanguage).split("\n"));
            return result;
        }
    }

    public interface ISubtitleDownloaderListener {
        void onSubtitleDownloadCompleted(boolean isSuccessful, TimedTextObject subtitleFile);
    }
}
