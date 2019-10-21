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

package butter.droid.fragments;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.BeamPlayerActivity;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.fragments.BaseVideoPlayerFragment;
import butter.droid.base.subs.Caption;
import butter.droid.base.utils.AnimUtils;
import butter.droid.base.utils.FragmentUtil;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PixelUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.StringUtils;
import butter.droid.base.utils.VersionUtils;
import butter.droid.widget.StrokedRobotoTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoPlayerFragment extends BaseVideoPlayerFragment implements View.OnSystemUiVisibilityChangeListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.progress_indicator)
    ProgressBar mProgressIndicator;
    @BindView(R.id.video_surface)
    SurfaceView videoSurface;
    @BindView(R.id.subtitle_text)
    StrokedRobotoTextView mSubtitleText;
    @BindView(R.id.control_layout)
    RelativeLayout mControlLayout;
    @BindView(R.id.player_info)
    TextView mPlayerInfo;
    @BindView(R.id.control_bar)
    butter.droid.widget.ButterSeekBar mControlBar;
    @BindView(R.id.play_button)
    ImageButton mPlayButton;
    @BindView(R.id.forward_button)
    ImageButton mForwardButton;
    @BindView(R.id.rewind_button)
    ImageButton mRewindButton;
    @BindView(R.id.subs_button)
    ImageButton mSubsButton;
    @BindView(R.id.current_time)
    TextView mCurrentTimeTextView;
    @BindView(R.id.length_time)
    TextView lengthTime;
    View mDecorView;

    private AudioManager mAudioManager;
    private GestureDetectorCompat mGestureDetector;

    private long mLastSystemShowTime = System.currentTimeMillis();

    private static final int FADE_OUT_OVERLAY = 5000;
    private static final int FADE_OUT_INFO = 1000;

    private int mLastSystemUIVisibility;
    private boolean mOverlayVisible = true;

    private Handler mDisplayHandler;

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction;
    private int mSurfaceYDisplayRange;
    private float mTouchY, mTouchX;

    private int mAudioMax;
    private float mVol;

    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;

    private final GestureDetector.SimpleOnGestureListener mGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            togglePlayPause();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mTouchAction == TOUCH_NONE) {
                if (!mOverlayVisible) {
                    showOverlay();
                } else {
                    hideOverlay();
                }
            } else {
                showOverlay();
            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        mShowReload = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_videoplayer, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onTouchEvent(event);
            }
        });
        ButterKnife.bind(this, view);

        if (LocaleUtils.isRTL(LocaleUtils.getCurrent())) {
            Drawable forward = mForwardButton.getDrawable();
            Drawable rewind = mRewindButton.getDrawable();
            mRewindButton.setImageDrawable(forward);
            mForwardButton.setImageDrawable(rewind);
        }

        int color = getResources().getColor(R.color.primary);
        LayerDrawable progressDrawable;
        if (!VersionUtils.isLollipop()) {
            progressDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.scrubber_progress_horizontal);
        } else {
            if (mControlBar.getProgressDrawable() instanceof StateListDrawable) {
                StateListDrawable stateListDrawable = (StateListDrawable) mControlBar.getProgressDrawable();
                progressDrawable = (LayerDrawable) stateListDrawable.getCurrent();
            } else {
                progressDrawable = (LayerDrawable) mControlBar.getProgressDrawable();
            }
        }
        progressDrawable.findDrawableByLayerId(android.R.id.progress).setColorFilter(color, PorterDuff.Mode.SRC_IN);
        progressDrawable.findDrawableByLayerId(android.R.id.secondaryProgress).setColorFilter(color, PorterDuff.Mode.SRC_IN);

        mControlBar.setProgressDrawable(progressDrawable);
        mControlBar.getThumbDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        mGestureDetector = new GestureDetectorCompat(getActivity(), mGestureListener);
        mGestureDetector.setOnDoubleTapListener(mGestureListener);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

        getAppCompatActivity().setSupportActionBar(mToolbar);

        videoSurface.setVisibility(View.VISIBLE);

        mToolbar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onTouchEvent(event);
                return true;
            }
        });

		/* Services and miscellaneous */
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mDisplayHandler = new Handler(Looper.getMainLooper());

        mDecorView = getActivity().getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mDecorView.setOnSystemUiVisibilityChangeListener(this);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            mToolbar.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material) +
                            PixelUtils.getStatusBarHeight(getActivity())));
            mToolbar.setPadding(mToolbar.getPaddingLeft(), PixelUtils.getStatusBarHeight(getActivity()), mToolbar.getPaddingRight(),
                    mToolbar.getPaddingBottom());
        }

        if(getAppCompatActivity().getSupportActionBar() != null) {
            if (null != mCallback.getInfo()) {
                if (mMedia != null && mMedia.title != null) {
                    if (null != mCallback.getInfo().getQuality()) {
                        getAppCompatActivity().getSupportActionBar().setTitle(
                                getString(R.string.now_playing) + ": " + mMedia.title + " (" + mCallback.getInfo().getQuality() + ")");
                    } else {
                        getAppCompatActivity().getSupportActionBar().setTitle(getString(R.string.now_playing) + ": " + mMedia.title);
                    }
                } else {
                    getAppCompatActivity().getSupportActionBar().setTitle(getString(R.string.now_playing));
                }
            } else {
                getAppCompatActivity().getSupportActionBar().setTitle(getString(R.string.now_playing));
            }
            getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        mSubtitleText.setTextColor(PrefUtils.get(getActivity(), Prefs.SUBTITLE_COLOR, Color.WHITE));
        mSubtitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, PrefUtils.get(getActivity(), Prefs.SUBTITLE_SIZE, 16));
        mSubtitleText.setStrokeColor(PrefUtils.get(getActivity(), Prefs.SUBTITLE_STROKE_COLOR, Color.BLACK));
        mSubtitleText.setStrokeWidth(TypedValue.COMPLEX_UNIT_DIP, PrefUtils.get(getActivity(), Prefs.SUBTITLE_STROKE_WIDTH, 2));

        mControlBar.setOnSeekBarChangeListener(mOnControlBarListener);

        getAppCompatActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onPlaybackEndReached() {
        getVideoSurface().setKeepScreenOn(false);
        //todod:
    }

    @Override
    public void onStop() {
        super.onStop();
        //restore brightness
        if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness * 255f);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAudioManager = null;
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    @Override
    protected SurfaceView getVideoSurface() {
        return videoSurface;
    }


    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        DisplayMetrics screen = new DisplayMetrics();
        getAppCompatActivity().getWindowManager().getDefaultDisplay().getMetrics(screen);

        if (mSurfaceYDisplayRange == 0) {
            mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);
        }

        float y_changed = event.getRawY() - mTouchY;
        float x_changed = event.getRawX() - mTouchX;

        // coef is the gradient's move to determine a neutral zone
        float coef = Math.abs(y_changed / x_changed);
        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);

        int[] offset = new int[2];
        videoSurface.getLocationOnScreen(offset);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = event.getRawY();
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                break;

            case MotionEvent.ACTION_MOVE:
                if (coef > 2) {
                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    if ((int) mTouchX > (screen.widthPixels / 2)) {
                        doVolumeTouch(y_changed);
                    }
                    if ((int) mTouchX < (screen.widthPixels / 2)) {
                        if (VersionUtils.isMarshmallow()) {
                            if (Settings.System.canWrite(getContext())) {
                                doVolumeTouch(y_changed);
                            }
                        } else {
                            doBrightnessTouch(y_changed);
                        }
                    }
                } else {
                    // Seek (Right or Left move)
                    doSeekTouch(coef, xgesturesize, false);
                }
                break;

            case MotionEvent.ACTION_UP:
                doSeekTouch(coef, xgesturesize, true);
                break;
        }
        return true;
    }


    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if ((mLastSystemUIVisibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0 &&
                (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
            showOverlay();
        }

        mLastSystemUIVisibility = visibility;
    }

    private void doSeekTouch(float coef, float gesturesize, boolean seek) {
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (coef > 0.5 || Math.abs(gesturesize) < 1) {
            return;
        }

        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK) {
            return;
        }
        mTouchAction = TOUCH_SEEK;

        // Size of the jump, 10 minutes max (600000), with a bi-cubic progression, for a 8cm gesture
        int jump = (int) (Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000));

        // Adjust the jump
        if ((jump > 0) && ((getCurrentTime() + jump) > mControlBar.getSecondaryProgress())) {
            jump = (int) (mControlBar.getSecondaryProgress() - getCurrentTime());
        }
        if ((jump < 0) && ((getCurrentTime() + jump) < 0)) {
            jump = (int) -getCurrentTime();
        }

        long currentTime = getCurrentTime();
        if (seek && mControlBar.getSecondaryProgress() > 0) {
            seek(jump);
        }

        if (getDuration() > 0) {
            showPlayerInfo(String.format("%s%s (%s)", jump >= 0 ? "+" : "", StringUtils.millisToString(jump), StringUtils.millisToString(currentTime + jump)));
        }
    }

    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        float delta = -((y_changed / mSurfaceYDisplayRange) * mAudioMax);
        mVol += delta;
        int vol = (int) Math.min(Math.max(mVol, 0), mAudioMax);
        if (delta != 0f) {
            setAudioVolume(vol);
        }
    }

    private void setAudioVolume(int vol) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);

        /* Since android 4.3, the safe volume warning dialog is displayed only with the FLAG_SHOW_UI flag.
         * We don't want to always show the default UI volume, so show it only when volume is not set. */
        int newVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (vol != newVol)
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);

        mTouchAction = TOUCH_VOLUME;
        showPlayerInfo(getString(R.string.volume) + '\u00A0' + Integer.toString(vol));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initBrightnessTouch() {
        float brightnesstemp = 0.6f;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
                    Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                if (VersionUtils.isMarshmallow() && Settings.System.canWrite(getActivity()))
                    Settings.System.putInt(getActivity().getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                mRestoreAutoBrightness = android.provider.Settings.System.getInt(getActivity().getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else {
                brightnesstemp = android.provider.Settings.System.getInt(getActivity().getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        WindowManager.LayoutParams lp = getAppCompatActivity().getWindow().getAttributes();
        lp.screenBrightness = brightnesstemp;
        getAppCompatActivity().getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture) initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;

        // Set delta : 2f is arbitrary for now, it possibly will change in the future
        float delta = -y_changed / mSurfaceYDisplayRange;

        changeBrightness(delta);
    }

    private void changeBrightness(float delta) {
        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = getAppCompatActivity().getWindow().getAttributes();
        lp.screenBrightness = Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1);
        // Set Brightness
        getAppCompatActivity().getWindow().setAttributes(lp);
        showPlayerInfo(getString(R.string.brightness) + '\u00A0' + Math.round(lp.screenBrightness * 15));
    }


    @Override
    protected void onErrorEncountered() {
        /* Encountered Error, exit player with a message */
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getAppCompatActivity().finish();
                    }
                })
                .setTitle(R.string.encountered_error_title)
                .setMessage(R.string.encountered_error)
                .create();
        dialog.show();
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void showOverlay() {
        if (!mOverlayVisible) {
            updatePlayPauseState();

            AnimUtils.fadeIn(mControlLayout);
            AnimUtils.fadeIn(mToolbar);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                mDecorView.setSystemUiVisibility(uiOptions);
            } else {
                getAppCompatActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getAppCompatActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            mLastSystemShowTime = System.currentTimeMillis();
        }

        mOverlayVisible = true;
        mDisplayHandler.removeCallbacks(mOverlayHideRunnable);
        mDisplayHandler.postDelayed(mOverlayHideRunnable, FADE_OUT_OVERLAY);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void hideOverlay() {
        // Can only hide 1000 millisec after show, because navbar doesn't seem to hide otherwise.
        if (mLastSystemShowTime + 1000 < System.currentTimeMillis()) {
            AnimUtils.fadeOut(mControlLayout);
            AnimUtils.fadeOut(mToolbar);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
                mDecorView.setSystemUiVisibility(uiOptions);
            } else {
                getAppCompatActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getAppCompatActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            }

            mDisplayHandler.removeCallbacks(mOverlayHideRunnable);
            mOverlayVisible = false;
        }
    }

    protected void showPlayerInfo(String text) {
        mPlayerInfo.setVisibility(View.VISIBLE);
        mPlayerInfo.setText(text);
        mDisplayHandler.removeCallbacks(mInfoHideRunnable);
        mDisplayHandler.postDelayed(mInfoHideRunnable, FADE_OUT_INFO);
    }

    private void hidePlayerInfo() {
        if (mPlayerInfo.getVisibility() == View.VISIBLE) {
            Animation fadeOutAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
            mPlayerInfo.startAnimation(fadeOutAnim);
        }
        mPlayerInfo.setVisibility(View.INVISIBLE);
    }

    public void updatePlayPauseState() {
        if(FragmentUtil.isNotAdded(this))
            return;

        if (isPlaying()) {
            mPlayButton.setImageResource(R.drawable.ic_av_pause);
            mPlayButton.setContentDescription(getString(R.string.pause));
        } else {
            mPlayButton.setImageResource(R.drawable.ic_av_play);
            mPlayButton.setContentDescription(getString(R.string.play));
        }
    }

    private SeekBar.OnSeekBarChangeListener mOnControlBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            setSeeking(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSeeking(false);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && isSeeking() && progress <= (getDuration() / 100 * seekBar.getSecondaryProgress())) {
                setLastSubtitleCaption(null);
                setCurrentTime(progress);
                VideoPlayerFragment.this.onProgressChanged(getCurrentTime(), getDuration());
                progressSubtitleCaption();
            }
        }
    };

    @Override
    protected void onHardwareAccelerationError() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        disableHardwareAcceleration();
                        loadMedia();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getAppCompatActivity().finish();
                    }
                })
                .setTitle(R.string.hardware_acceleration_error_title)
                .setMessage(R.string.hardware_acceleration_error_message)
                .create();
        if (!getAppCompatActivity().isFinishing())
            dialog.show();
    }

    private Runnable mOverlayHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideOverlay();
        }
    };

    private Runnable mInfoHideRunnable = new Runnable() {
        @Override
        public void run() {
            hidePlayerInfo();
        }
    };

    @Override
    protected void showTimedCaptionText(final Caption text) {
        mDisplayHandler.post(new Runnable() {
            @Override
            public void run() {
                if (text == null) {
                    if (mSubtitleText.getText().length() > 0) {
                        mSubtitleText.setText("");
                    }
                    return;
                }
                SpannableStringBuilder styledString = (SpannableStringBuilder) Html.fromHtml(text.content);

                ForegroundColorSpan[] toRemoveSpans = styledString.getSpans(0, styledString.length(), ForegroundColorSpan.class);
                for (ForegroundColorSpan remove : toRemoveSpans) {
                    styledString.removeSpan(remove);
                }

                if (!mSubtitleText.getText().toString().equals(styledString.toString())) {
                    mSubtitleText.setText(styledString);
                }
            }
        });
    }

    @Override
    protected void setProgressVisible(boolean visible) {
        if(mProgressIndicator.getVisibility() == View.VISIBLE && visible)
            return;

        if(mProgressIndicator.getVisibility() == View.GONE && !visible)
            return;

        mProgressIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates the overlay when the media playback progress has changed
     *
     * @param currentTime Current progress time
     * @param duration Duration of full medias
     */
    @Override
    protected void onProgressChanged(long currentTime, long duration) {
        mControlBar.setMax((int) duration);
        mControlBar.setProgress((int) currentTime);
        mControlBar.setSecondaryProgress(0); // hack to make the secondary progress appear on Android 5.0
        mControlBar.setSecondaryProgress(getStreamerProgress());

        if (getCurrentTime() >= 0)
            mCurrentTimeTextView.setText(StringUtils.millisToString(currentTime));
        if (getDuration() >= 0)
            lengthTime.setText(StringUtils.millisToString(duration));
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {
        super.onStreamProgress(torrent, streamStatus);
        mControlBar.setSecondaryProgress(getStreamerProgress());
    }

    public void enableSubsButton(boolean b) {
        mSubsButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }

    protected void updateSubtitleSize(int size) {
        mSubtitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    @OnClick(R.id.play_button)
    void onPlayPauseClick() {
        togglePlayPause();
    }

    @OnClick(R.id.rewind_button)
    void onRewindClick() {
        seekBackwardClick();
    }

    @OnClick(R.id.forward_button)
    void onForwardClick() {
        seekForwardClick();
    }

    @OnClick(R.id.scale_button)
    void onScaleClick() {
        scaleClick();
    }

    @OnClick(R.id.subs_button)
    void onSubsClick() {
        subsClick();
    }


    public void startBeamPlayerActivity(){
        BeamPlayerActivity.startActivity(getActivity(), mCallback.getInfo(), getCurrentTime());
    }

}
