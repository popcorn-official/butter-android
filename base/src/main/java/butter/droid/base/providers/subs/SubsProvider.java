/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.subs;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.providers.BaseProvider;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.subs.FormatASS;
import butter.droid.base.subs.FormatSRT;
import butter.droid.base.subs.TimedTextObject;
import butter.droid.base.utils.FileUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.StorageUtils;
import timber.log.Timber;

public abstract class SubsProvider extends BaseProvider {
    public static final String SUBS_CALL = "subs_http_call";
    public static final String SUBTITLE_LANGUAGE_NONE = "no-subs";

    private static List<String> SUB_EXTENSIONS = Arrays.asList("srt", "ssa", "ass");

    private final Context context;
    private final OkHttpClient client;

    public SubsProvider(Context context, OkHttpClient client, ObjectMapper mapper) {
        super(client, mapper);
        this.context = context;
        this.client = client;
    }

    public static File getStorageLocation(Context context) {
        return new File(PrefUtils.get(context, Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(context).toString()) + "/subs/");
    }

    /**
     * @param media        Media data
     * @param languageCode Code of language
     * @param callback     Network callback
     * @return Call
     */
    public Call download(final Media media, final String languageCode, final com.squareup.okhttp.Callback callback) {
        if (media.subtitles != null && media.subtitles.containsKey(languageCode)) {
            try {
                Request request = new Request.Builder().url(media.subtitles.get(languageCode)).build();
                Call call = client.newCall(request);

                final File subsDirectory = getStorageLocation(context);
                final String fileName = media.videoId + "-" + languageCode;
                final File srtPath = new File(subsDirectory, fileName + ".srt");

                if (srtPath.exists()) {
                    callback.onResponse(null);
                    return call;
                }

                call.enqueue(new com.squareup.okhttp.Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        callback.onFailure(request, e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        if (response.isSuccessful()) {
                            InputStream inputStream = null;
                            boolean failure = false;
                            try {
                                if (!subsDirectory.mkdirs()) {
                                    Timber.w("Could not create directory: " + subsDirectory.getAbsolutePath());
                                }
                                if (srtPath.exists()) {
                                    File to = new File(subsDirectory, "temp" + System.currentTimeMillis());
                                    if (!srtPath.renameTo(to)) {
                                        Timber.w("Could not rename file: " + srtPath.getAbsolutePath());
                                    }
                                    if (!to.delete()) {
                                        Timber.w("Could not delete file: " + to.getAbsolutePath());
                                    }
                                }

                                inputStream = response.body().byteStream();
                                String urlString = response.request().urlString();

                                if (urlString.contains(".zip") || urlString.contains(".gz")) {
                                    SubsProvider.unpack(inputStream, srtPath, languageCode);
                                } else if (SubsProvider.isSubFormat(urlString)) {
                                    parseFormatAndSave(urlString, srtPath, languageCode, inputStream);
                                } else {
                                    callback.onFailure(response.request(), new IOException("FatalParsingException"));
                                    failure = true;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                callback.onFailure(response.request(), e);
                                failure = true;
                            } finally {
                                if (inputStream != null)
                                    inputStream.close();

                                if (!failure) callback.onResponse(response);
                            }
                        } else {
                            callback.onFailure(response.request(), new IOException("Unknown error"));
                        }
                    }
                });

                return call;
            } catch (RuntimeException | IOException e) {
                e.printStackTrace();
            }
        }
        callback.onFailure(null, new IOException("Wrong media"));
        return null;
    }

    /**
     * Unpack ZIP and save SRT
     *
     * @param is           InputStream from network
     * @param srtPath      Path where SRT should be saved
     * @param languageCode The language code
     * @throws IOException
     */
    private static void unpack(InputStream is, File srtPath, String languageCode) throws IOException {
        String filename;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze;

        while ((ze = zis.getNextEntry()) != null) {
            filename = ze.getName();
            if (filename.contains("_MACOSX")) continue;

            if (isSubFormat(filename)) {
                parseFormatAndSave(filename, srtPath, languageCode, zis);
                try {
                    zis.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        zis.close();
    }

    /**
     * Test if file is subtitle format
     *
     * @param filename Name of file
     * @return is subtitle?
     */
    private static boolean isSubFormat(String filename) {
        for (String ext : SUB_EXTENSIONS) {
            if (filename.contains("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the text, convert and save to SRT file
     *
     * @param inputUrl     Original network location
     * @param srtPath      Place where SRT should be saved
     * @param languageCode The language code
     * @param inputStream  InputStream of data  @throws IOException
     */
    private static void parseFormatAndSave(String inputUrl, File srtPath, String languageCode, InputStream inputStream) throws IOException {
        TimedTextObject subtitleObject = null;

        String inputString = FileUtils.inputstreamToCharsetString(inputStream, languageCode);
        String[] inputText = inputString.split("\n|\r\n");

        if (inputUrl.contains(".ass") || inputUrl.contains(".ssa")) {
            FormatASS formatASS = new FormatASS();
            subtitleObject = formatASS.parseFile(inputUrl, inputText);
        } else if (inputUrl.contains(".srt")) {
            FormatSRT formatSRT = new FormatSRT();
            subtitleObject = formatSRT.parseFile(inputUrl, inputText);
        }

        if (subtitleObject != null) {
            FileUtils.saveStringFile(subtitleObject.toSRT(), srtPath);
        }
    }

    public abstract void getList(Movie movie, Callback callback);

    public abstract void getList(Episode episode, Callback callback);

    public interface Callback {
        void onSuccess(Map<String, String> items);

        void onFailure(Exception e);
    }
}