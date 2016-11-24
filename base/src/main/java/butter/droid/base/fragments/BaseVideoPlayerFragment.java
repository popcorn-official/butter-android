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

package butter.droid.base.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.connectsdk.device.ConnectableDevice;
import com.github.sv244.torrentstream.StreamStatus;
import com.github.sv244.torrentstream.Torrent;
import com.github.sv244.torrentstream.listeners.TorrentListener;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.inject.Inject;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.beaming.BeamDeviceListener;
import butter.droid.base.beaming.BeamManager;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.fragments.dialog.FileSelectorDialogFragment;
import butter.droid.base.fragments.dialog.NumberPickerDialogFragment;
import butter.droid.base.fragments.dialog.StringArraySelectorDialogFragment;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.subs.Caption;
import butter.droid.base.subs.SubtitleDownloader;
import butter.droid.base.subs.TimedTextObject;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentService;
import butter.droid.base.utils.FragmentUtil;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.vlc.VLCInstance;
import butter.droid.base.vlc.VLCOptions;
import timber.log.Timber;

public abstract class BaseVideoPlayerFragment
    extends Fragment
    implements IVLCVout.Callback,
    TorrentListener,
    MediaPlayer.EventListener,
    LibVLC.HardwareAccelerationError,
    SubtitleDownloader.ISubtitleDownloaderListener {

    @Inject
    ProviderManager providerManager;

    public static final String RESUME_POSITION = "resume_position";
    private static final int SUBTITLE_MINIMUM_SIZE = 10;

    private LibVLC mLibVLC;
    private MediaPlayer mMediaPlayer;
    private Long mResumePosition = 1L;
    private Long mDuration = 1L;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;

    protected Media mMedia;
    protected boolean mShowReload = false;

    private int mCurrentSize = SURFACE_BEST_FIT;
    private int mStreamerProgress = 0;

    private String mCurrentSubsLang = SubsProvider.SUBTITLE_LANGUAGE_NONE;
    private TimedTextObject mSubs;
    private Caption mLastSub = null;
    private File mSubsFile = null;

    private boolean mEnded = false;
    private boolean mSeeking = false;
    private boolean mMediaReady = false;

    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    private int mSubtitleOffset = 0;

    // probably required when hardware acceleration selection during playback is implemented
    @SuppressWarnings("FieldCanBeLocal")
    private boolean mDisabledHardwareAcceleration = false;

    protected Callback mCallback;

    private View mRootView;

    private static LibVLC LibVLC() {
        return VLCInstance.get(ButterApplication.getAppContext());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callback) mCallback = (Callback) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof Callback && mCallback == null) {
            mCallback = (Callback) getActivity();
        }

        mResumePosition = mCallback.getResumePosition();
        PrefUtils.save(getActivity(), RESUME_POSITION, mResumePosition);

        if (!VLCInstance.hasCompatibleCPU(getContext())) {
            getActivity().finish();
            return;
        }

        StreamInfo streamInfo = mCallback.getInfo();

        if (streamInfo == null){
            getActivity().finish();
            return;
        }

        mMedia = streamInfo.getMedia();

        mLibVLC = LibVLC();
        mLibVLC.setOnHardwareAccelerationError(this);

        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);

        if (mCallback.getService() != null) {
            mCallback.getService().addListener(this);
        }

        setProgressVisible(true);

        if (null == streamInfo.getSubtitleLanguage()) {
            // Get selected default subtitle
            String defaultSubtitle = PrefUtils.get(
                    getActivity(),
                    Prefs.SUBTITLE_DEFAULT,
                    SubsProvider.SUBTITLE_LANGUAGE_NONE);
            streamInfo.setSubtitleLanguage(defaultSubtitle);
            mCurrentSubsLang = defaultSubtitle;
            Timber.d("Using default subtitle: " + mCurrentSubsLang);
        }

        if (!streamInfo.getSubtitleLanguage().equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) {
            Timber.d("Download default subtitle");
            mCurrentSubsLang = streamInfo.getSubtitleLanguage();
            loadOrDownloadSubtitle();
        }

        updateSubtitleSize(PrefUtils.get(getActivity(), Prefs.SUBTITLE_SIZE, getResources().getInteger(R.integer.player_subtitles_default_text_size)));

        prepareVlcVout();
        if(mMediaReady)
            loadMedia();
    }

    @Override
    public void onResume() {
        super.onResume();

        prepareVlcVout();
        BeamManager.getInstance(getActivity()).addDeviceListener(mDeviceListener);
        onProgressChanged(PrefUtils.get(getActivity(), RESUME_POSITION, mResumePosition), mDuration);
    }

    @Override
    public void onPause() {
        super.onPause();

        saveVideoCurrentTime();
        mMediaPlayer.stop();

        final IVLCVout vlcout = mMediaPlayer.getVLCVout();
        vlcout.removeCallback(this);
        vlcout.detachViews();

        BeamManager.getInstance(getActivity()).removeDeviceListener(mDeviceListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PrefUtils.save(getActivity(), RESUME_POSITION, 0);

        mMediaPlayer.release();
        mLibVLC.release();

        if (mCallback.getService() != null) {
            mCallback.getService().removeListener(this);
        }
    }

    public void onMediaReady() {
        if(!mMediaReady) {
            mMediaReady = true;
        }
        if(mLibVLC != null) {
            loadMedia();
        }
    }

    protected void disableHardwareAcceleration() {
        mDisabledHardwareAcceleration = true;
        saveVideoCurrentTime();
    }

    /**
     * External extras: - position (long) - position of the video to start with (in ms)
     */
    @SuppressWarnings({"unchecked"})
    protected void loadMedia() {
        StreamInfo streamInfo = mCallback.getInfo();

        String videoLocation;
        if (TextUtils.isEmpty(streamInfo.getVideoLocation())) {
            Toast.makeText(getActivity(), "Error loading media", Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        else {
            videoLocation = streamInfo.getVideoLocation();
            if (!videoLocation.startsWith("file://") && !videoLocation.startsWith("http://") && !videoLocation.startsWith("https://")) {
                videoLocation = "file://" + videoLocation;
            }
        }

        int flags = mDisabledHardwareAcceleration ? VLCOptions.MEDIA_NO_HWACCEL : 0;
        flags = flags | VLCOptions.MEDIA_VIDEO;

        org.videolan.libvlc.Media media = new org.videolan.libvlc.Media(mLibVLC, Uri.parse(videoLocation));
        VLCOptions.setMediaOptions(media, getActivity(), flags);

        mMediaPlayer.setMedia(media);

        long resumeFrom = PrefUtils.get(getActivity(), RESUME_POSITION, mResumePosition);
        if (resumeFrom > 0) {
            mMediaPlayer.setTime(resumeFrom);
        }

        mDuration = mMediaPlayer.getLength();
        mMediaPlayer.play();
        mEnded = false;
    }

    protected abstract void setProgressVisible(boolean visible);

    protected abstract void showOverlay();

    protected abstract void showPlayerInfo(String info);

    protected abstract void onProgressChanged(long currentTime, long duration);

    protected abstract void updatePlayPauseState();

    protected abstract void onErrorEncountered();

    protected abstract void onHardwareAccelerationError();

    protected abstract void showTimedCaptionText(Caption text);

    protected abstract SurfaceView getVideoSurface();

    protected void setSeeking(boolean seeking) {
        mSeeking = seeking;
    }

    protected boolean isSeeking() {
        return mSeeking;
    }

    private void prepareVlcVout() {
        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();

        if (!vlcVout.areViewsAttached()) {
            vlcVout.setVideoView(getVideoSurface());
            vlcVout.addCallback(this);
            vlcVout.attachViews();
        }
    }

    private void saveVideoCurrentTime() {
        long currentTime = mMediaPlayer.getTime();
        PrefUtils.save(getActivity(), RESUME_POSITION, currentTime);
    }

    private void resumeVideo() {
        if (getActivity() != null) {
            long resumePosition = PrefUtils.get(getActivity(), RESUME_POSITION, 0);
            mDuration = mMediaPlayer.getLength();
            if (mDuration > resumePosition && resumePosition > 0) {
                setCurrentTime(resumePosition);
                PrefUtils.save(getActivity(), RESUME_POSITION, 0);
            }
        }
    }

    private void play() {
        mMediaPlayer.play();
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public void togglePlayPause() {
        if (mEnded) {
            loadMedia();
        }

        if (mMediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void seekForwardClick() {
        seek(10000);
    }

    public void seekBackwardClick() {
        seek(-10000);
    }

    public void scaleClick() {
        if (mCurrentSize < SURFACE_ORIGINAL) {
            mCurrentSize++;
        } else {
            mCurrentSize = 0;
        }
        changeSurfaceSize(true);
        showOverlay();
    }

    protected void setCurrentTime(long time) {
        if (time / getDuration() * 100 <= getStreamerProgress()) {
            mMediaPlayer.setTime(time);
        }
    }

    protected long getCurrentTime() {
        return mMediaPlayer.getTime();
    }

    protected long getDuration() {
        return mDuration;
    }

    public int getStreamerProgress() {
        return mStreamerProgress;
    }

    /**
     * Is a video currently playing with VLC
     *
     * @return true if video is played using VLC
     */
    protected boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    private void endReached() {
        mEnded = true;
        onPlaybackEndReached();
    }

    public abstract void onPlaybackEndReached();

    private void handleHardwareAccelerationError() {
        saveVideoCurrentTime();
        mMediaPlayer.stop();
        onHardwareAccelerationError();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void changeSurfaceSize(boolean message) {
        int screenWidth = getActivity().getWindow().getDecorView().getWidth();
        int screenHeight = getActivity().getWindow().getDecorView().getHeight();

        if (mMediaPlayer != null) {
            final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
            vlcVout.setWindowSize(screenWidth, screenHeight);
        }

        double displayWidth = screenWidth, displayHeight = screenHeight;

        if (screenWidth < screenHeight) {
            displayWidth = screenHeight;
            displayHeight = screenWidth;
        }

        // sanity check
        if (displayWidth * displayHeight <= 1 || mVideoWidth * mVideoHeight <= 1) {
            Timber.e("Invalid surface size");
            onErrorEncountered();
            return;
        }

        // compute the aspect ratio
        double aspectRatio, visibleWidth;
        if (mSarDen == mSarNum) {
			/* No indication about the density, assuming 1:1 */
            visibleWidth = mVideoVisibleWidth;
            aspectRatio = (double) mVideoVisibleWidth / (double) mVideoVisibleHeight;
        } else {
			/* Use the specified aspect ratio */
            visibleWidth = mVideoVisibleWidth * (double) mSarNum / mSarDen;
            aspectRatio = visibleWidth / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double displayAspectRatio = displayWidth / displayHeight;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (message) showPlayerInfo(getString(R.string.best_fit));
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_FIT_HORIZONTAL:
                displayHeight = displayWidth / aspectRatio;
                if (message) showPlayerInfo(getString(R.string.fit_horizontal));
                break;
            case SURFACE_FIT_VERTICAL:
                displayWidth = displayHeight * aspectRatio;
                if (message) showPlayerInfo(getString(R.string.fit_vertical));
                break;
            case SURFACE_FILL:
                if (message) showPlayerInfo(getString(R.string.fill));
                break;
            case SURFACE_16_9:
                if (message) showPlayerInfo("16:9");
                aspectRatio = 16.0 / 9.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_4_3:
                if (message) showPlayerInfo("4:3");
                aspectRatio = 4.0 / 3.0;
                if (displayAspectRatio < aspectRatio)
                    displayHeight = displayWidth / aspectRatio;
                else
                    displayWidth = displayHeight * aspectRatio;
                break;
            case SURFACE_ORIGINAL:
                if (message) showPlayerInfo(getString(R.string.original_size));
                displayHeight = mVideoVisibleHeight;
                displayWidth = visibleWidth;
                break;
        }

        // set display size
        int finalWidth = (int) Math.ceil(displayWidth * mVideoWidth / mVideoVisibleWidth);
        int finalHeight = (int) Math.ceil(displayHeight * mVideoHeight / mVideoVisibleHeight);

        SurfaceHolder holder = getVideoSurface().getHolder();
        holder.setFixedSize(finalWidth, finalHeight);

        ViewGroup.LayoutParams lp = getVideoSurface().getLayoutParams();
        lp.width = finalWidth;
        lp.height = finalHeight;
        getVideoSurface().setLayoutParams(lp);
        getVideoSurface().invalidate();
    }

    protected void seek(int delta) {
        if (mMediaPlayer.getLength() <= 0 && !mSeeking) return;

        long position = mMediaPlayer.getTime() + delta;
        if (position < 0) position = 0;
        setCurrentTime(position);
        showOverlay();
        onProgressChanged(getCurrentTime(), getDuration());
        mLastSub = null;
    }

    protected void setLastSubtitleCaption(Caption sub) {
        mLastSub = sub;
    }

    protected void progressSubtitleCaption() {
        if (mLibVLC != null && mMediaPlayer != null && mMediaPlayer.isPlaying() && mSubs != null) {
            Collection<Caption> subtitles = mSubs.captions.values();
            double currentTime = getCurrentTime() - mSubtitleOffset;
            if (mLastSub != null && currentTime >= mLastSub.start.getMilliseconds() && currentTime <= mLastSub.end.getMilliseconds()) {
                showTimedCaptionText(mLastSub);
            } else {
                for (Caption caption : subtitles) {
                    if (currentTime >= caption.start.getMilliseconds() && currentTime <= caption.end.getMilliseconds()) {
                        mLastSub = caption;
                        showTimedCaptionText(caption);
                        break;
                    } else if (currentTime > caption.end.getMilliseconds()) {
                        showTimedCaptionText(null);
                    }
                }
            }
        }
    }

    private void onSubtitleLanguageSelected(String language) {
        if (mCurrentSubsLang != null && (language == null || mCurrentSubsLang.equals(language))) {
            return;
        }

        mCurrentSubsLang = language;
        mCallback.getInfo().setSubtitleLanguage(mCurrentSubsLang);

        if (mCurrentSubsLang.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) {
            mSubs = null;
            onSubtitleEnabledStateChanged(false);
            return;
        }

        if (mMedia == null || mMedia.subtitles == null || mMedia.subtitles.size() == 0) {
            mSubs = null;
            onSubtitleEnabledStateChanged(false);
            throw new IllegalArgumentException("Media doesn't have subtitle");
        }

        if (!mMedia.subtitles.containsKey(mCurrentSubsLang)) {
            mSubs = null;
            onSubtitleEnabledStateChanged(false);
            throw new IllegalArgumentException("Media doesn't have subtitle with specified language");
        }

        showTimedCaptionText(null);
        loadOrDownloadSubtitle();
    }

    private void loadOrDownloadSubtitle() {
        if (mMedia == null) throw new NullPointerException("Media is not available");
        if (mCurrentSubsLang.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) return;

        SubtitleDownloader subtitleDownloader = new SubtitleDownloader(
                providerManager.getCurrentSubsProvider(),
                getActivity(), mCallback.getInfo(), mCurrentSubsLang);
        subtitleDownloader.setSubtitleDownloaderListener(this);

        try {
            mSubsFile = SubtitleDownloader.getDownloadedSubtitleFile(getActivity(), mMedia, mCurrentSubsLang);
            if (mSubsFile != null && mSubsFile.exists()) {
                subtitleDownloader.parseSubtitle(mSubsFile);
            }
        }
        catch (FileNotFoundException e) {
            subtitleDownloader.downloadSubtitle();
        }
    }

    /**
     * This callback is called when the native vout call request a new Layout.
     *
     * @param vlcVout vlcVout
     * @param width Frame width
     * @param height Frame height
     * @param visibleWidth Visible frame width
     * @param visibleHeight Visible frame height
     * @param sarNum Surface aspect ratio numerator
     * @param sarDen Surface aspect ratio denominator
     */
    @Override
    public void onNewLayout(
        IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height <= 0) {
            return;
        }

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth  = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mSarNum = sarNum;
        mSarDen = sarDen;

        changeSurfaceLayout();
    }

    /**
     * Called when subtitle for current media successfully loaded or disabled.
     * @param enabled Whether subtitle is loaded or disabled.
     */
    protected void onSubtitleEnabledStateChanged(boolean enabled) {}

    @Override
    public void onSurfacesCreated(IVLCVout ivlcVout) { }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) { }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Playing:
                getVideoSurface().getHolder().setKeepScreenOn(true);
                mDuration = mMediaPlayer.getLength();
                resumeVideo();
                setProgressVisible(false);
                showOverlay();
                updatePlayPauseState();
                break;
            case MediaPlayer.Event.Paused:
                getVideoSurface().getHolder().setKeepScreenOn(false);
                saveVideoCurrentTime();
                updatePlayPauseState();
                break;
            case MediaPlayer.Event.Stopped:
                getVideoSurface().getHolder().setKeepScreenOn(false);
                updatePlayPauseState();
                break;
            case MediaPlayer.Event.EndReached:
                endReached();
                updatePlayPauseState();
                break;
            case MediaPlayer.Event.EncounteredError:
                onErrorEncountered();
                updatePlayPauseState();
                break;
            case MediaPlayer.Event.Opening:
                setProgressVisible(true);
                mDuration = mMediaPlayer.getLength();
                mMediaPlayer.play();
                break;
            case MediaPlayer.Event.TimeChanged:
            case MediaPlayer.Event.PositionChanged:
                onProgressChanged(getCurrentTime(), getDuration());
                progressSubtitleCaption();
                break;
        }
    }

    @Override
    public void eventHardwareAccelerationError() {
        handleHardwareAccelerationError();
    }

    private void changeSurfaceLayout() {
        changeSurfaceSize(false);
    }

    @Override
    public void onStreamPrepared(Torrent torrent) { }

    @Override
    public void onStreamStarted(Torrent torrent) { }

    @Override
    public void onStreamStopped() { }

    @Override
    public void onStreamError(Torrent torrent, Exception e) { }

    @Override
    public void onStreamReady(Torrent torrent) { }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {
        int newProgress = (int) ((getDuration() / 100) * streamStatus.progress);
        if (mStreamerProgress < newProgress) {
            mStreamerProgress = newProgress;
        }
    }

    public interface Callback {
        Long getResumePosition();
        StreamInfo getInfo();
        TorrentService getService();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_player, menu);
        menu.findItem(R.id.action_reload).setVisible(mShowReload);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_reload) {
            mMediaPlayer.stop();
            Canvas canvas = new Canvas();
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            getVideoSurface().draw(canvas);
            loadMedia();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void subsClick() {
        if (mMedia != null && mMedia.subtitles != null) {
            if (getChildFragmentManager().findFragmentByTag("overlay_fragment") != null) return;

            String[] subsOptions = {
                    getString(R.string.subtitle_language),
                    getString(R.string.subtitle_size),
                    getString(R.string.subtitle_timing)
            };

            StringArraySelectorDialogFragment.show(getChildFragmentManager(), R.string.subtitle_settings, subsOptions, -1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            switch (position) {
                                case 0:
                                    subsLanguageSettings();
                                    break;
                                case 1:
                                    subsSizeSettings();
                                    break;
                                case 2:
                                    subsTimingSettings();
                                    break;
                            }
                        }
                    });
        }
    }

    private void subsLanguageSettings() {
        String[] subtitles = mMedia.subtitles.keySet().toArray(new String[mMedia.subtitles.size()]);
        Arrays.sort(subtitles);
        final String[] adapterSubtitles = new String[subtitles.length + 2];
        System.arraycopy(subtitles, 0, adapterSubtitles, 1, subtitles.length);

        adapterSubtitles[0] = SubsProvider.SUBTITLE_LANGUAGE_NONE;
        adapterSubtitles[adapterSubtitles.length - 1] = "custom";
        String[] readableNames = new String[adapterSubtitles.length];

        for (int i = 0; i < readableNames.length - 1; i++) {
            String language = adapterSubtitles[i];
            if (language.equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) {
                readableNames[i] = getString(R.string.no_subs);
            } else {
                Locale locale = LocaleUtils.toLocale(language);
                readableNames[i] = locale.getDisplayName(locale);
            }
        }

        readableNames[readableNames.length - 1] = "Custom..";

        StringArraySelectorDialogFragment.showSingleChoice(
            getChildFragmentManager(),
            R.string.subtitles,
            readableNames,
            Arrays.asList(adapterSubtitles).indexOf(mCurrentSubsLang),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, int position) {
                    if (position == adapterSubtitles.length - 1) {
                        FileSelectorDialogFragment.show(getChildFragmentManager(), new FileSelectorDialogFragment.Listener() {
                            @Override
                            public void onFileSelected(File f) {
                                if (!f.getPath().endsWith(".srt")) {
                                    Snackbar.make(mRootView, R.string.unknown_error, Snackbar.LENGTH_SHORT).show();
                                    return;
                                }
                                FileSelectorDialogFragment.hide();
                                mSubsFile = f;
                                dialog.dismiss();
                            }
                        });
                        return;
                    }
                    onSubtitleLanguageSelected(adapterSubtitles[position]);
                    dialog.dismiss();
                }
            });
    }

    private void subsSizeSettings() {
        Bundle args = new Bundle();
        args.putString(NumberPickerDialogFragment.TITLE, getString(R.string.subtitle_size));
        args.putInt(NumberPickerDialogFragment.MAX_VALUE, 60);
        args.putInt(NumberPickerDialogFragment.MIN_VALUE, SUBTITLE_MINIMUM_SIZE);
        args.putInt(NumberPickerDialogFragment.DEFAULT_VALUE, getResources().getInteger(R.integer.player_subtitles_default_text_size));

        NumberPickerDialogFragment dialogFragment = new NumberPickerDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.setOnResultListener(new NumberPickerDialogFragment.ResultListener() {
            @Override
            public void onNewValue(int value) {
                updateSubtitleSize(value);
            }
        });
        dialogFragment.show(getChildFragmentManager(), "overlay_fragment");
    }

    private void subsTimingSettings() {
        Bundle args = new Bundle();
        args.putString(NumberPickerDialogFragment.TITLE, getString(R.string.subtitle_timing));
        args.putInt(NumberPickerDialogFragment.MAX_VALUE, 3600);
        args.putInt(NumberPickerDialogFragment.MIN_VALUE, -3600);
        args.putInt(NumberPickerDialogFragment.DEFAULT_VALUE, mSubtitleOffset / 60);
        args.putBoolean(NumberPickerDialogFragment.FOCUSABLE, true);

        NumberPickerDialogFragment dialogFragment = new NumberPickerDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.setOnResultListener(new NumberPickerDialogFragment.ResultListener() {
            @Override
            public void onNewValue(int value) {
                mSubtitleOffset = value * 60;
                showTimedCaptionText(null);
            }
        });
        dialogFragment.show(getChildFragmentManager(), "overlay_fragment");
    }

    protected abstract void updateSubtitleSize(int size);

    @Override
    public void onSubtitleDownloadCompleted(boolean isSuccessful, TimedTextObject subtitleFile) {
        onSubtitleEnabledStateChanged(isSuccessful);
        mSubs = subtitleFile;
    }

    private BeamDeviceListener mDeviceListener = new BeamDeviceListener() {

        @Override
        public void onDeviceReady(ConnectableDevice device) {
            super.onDeviceReady(device);

            if (FragmentUtil.isNotAdded(BaseVideoPlayerFragment.this)){
                return;
            }
            
            startBeamPlayerActivity();

            getActivity().finish();
        }
    };

    public abstract void startBeamPlayerActivity();
}
