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
package butter.droid.tv.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.PlaybackOverlayFragment;
import android.support.v17.leanback.app.PlaybackOverlaySupportFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import butter.droid.base.activities.TorrentActivity;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.R;
import butter.droid.tv.activities.TVStreamLoadingActivity;
import butter.droid.tv.activities.TVVideoPlayerActivity;
import butter.droid.tv.events.ConfigureSubtitleEvent;
import butter.droid.tv.events.PausePlaybackEvent;
import butter.droid.tv.events.PlaybackProgressChangedEvent;
import butter.droid.tv.events.ScaleVideoEvent;
import butter.droid.tv.events.SeekBackwardEvent;
import butter.droid.tv.events.SeekForwardEvent;
import butter.droid.tv.events.StartPlaybackEvent;
import butter.droid.tv.events.StreamProgressChangedEvent;
import butter.droid.tv.events.ToggleSubtitleEvent;
import butter.droid.tv.events.UpdatePlaybackStateEvent;

/*
 * Class for video playback with media control
 */
public class TVPlaybackOverlayFragment extends PlaybackOverlaySupportFragment
        implements OnActionClickedListener,
        OnItemViewSelectedListener,
        PlaybackOverlaySupportFragment.InputEventHandler {
    private static final String TAG = "PlaybackOverlayFragment";

    private static final int MODE_NOTHING = 0;
    private static final int MODE_FAST_FORWARD = 1;
    private static final int MODE_REWIND = 2;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private ClosedCaptioningAction mClosedCaptioningAction;
    private ScaleVideoAction mScaleVideoAction;
    private PlayPauseAction mPlayPauseAction;
    private RewindAction mRewindAction;
    private FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow.SkipNextAction mSkipNextAction;
    private PlaybackControlsRowPresenter mPlaybackControlsRowPresenter;
    private PlaybackControlsRow mPlaybackControlsRow;
    private Handler mHandlerPlayback;
    private Handler mHandlerPlaybackSpeed;
    private StreamInfo mStreamInfo;
    private Show mShow;
    private Episode mNextEpisode;
    private Episode mPreviousEpisode;

    private long mSelectedActionId = 0;
    private int mCurrentMode = MODE_NOTHING;
    private boolean mKeepEventBusRegistration = false;
    private boolean mIsMediaReady = false;
    private boolean mSubsButtonEnabled = true;
    private int mSeek;
    private int mBufferedTime;
    private int mCurrentTime;
    private int mFastForwardSpeed = SeekForwardEvent.MINIMUM_SEEK_SPEED;
    private int mRewindSpeed = SeekBackwardEvent.MINIMUM_SEEK_SPEED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setFadeCompleteListener(new OnFadeCompleteListener() {
            @Override
            public void onFadeInComplete() {
                super.onFadeInComplete();
                mPlaybackControlsRow.setCurrentTime(mCurrentTime);
                mPlaybackControlsRow.setBufferedProgress(mBufferedTime);
                mSelectedActionId = mPlayPauseAction.getId();

                if (mRowsAdapter != null)
                    mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
            }

            @Override
            public void onFadeOutComplete() {
                super.onFadeOutComplete();
                mCurrentMode = MODE_NOTHING;
                mSelectedActionId = 0;
            }
        });

        mHandlerPlayback = new Handler();
        mHandlerPlaybackSpeed = new Handler();

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        setFadingEnabled(false);
        setOnItemViewSelectedListener(this);
        setInputEventHandler(this);
        initialisePlaybackControlPresenter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mStreamInfo = ((TVVideoPlayerFragment.Callback) getActivity()).getInfo();
        setupPlaybackControlItemsToInitialisingState();
        setupTVShowNextPreviousEpisodes();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mKeepEventBusRegistration) {
            // Event is unregistered before playback paused event is sent
            mPlayPauseAction.setIndex(PlayPauseAction.PLAY);
            setFadingEnabled(false);
            notifyPlaybackControlActionChanged(mPlayPauseAction);
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onActionClicked(@NonNull Action action) {
        if (action.getId() == mPlayPauseAction.getId()) {
            invokeTogglePlaybackAction(mPlayPauseAction.getIndex() == PlayPauseAction.PLAY);
        }
        else if (action.getId() == mScaleVideoAction.getId()) {
            invokeScaleVideoAction();
        }
        else if (action.getId() == mClosedCaptioningAction.getId()) {
            invokeOpenSubtitleSettingsAction();
        }
        if (mSkipPreviousAction != null && action.getId() == mSkipPreviousAction.getId()) {
            playSelectedEpisode(mPreviousEpisode);
        }
        if (mSkipNextAction != null && action.getId() == mSkipNextAction.getId()) {
            playSelectedEpisode(mNextEpisode);
        }

        if (action instanceof PlaybackControlsRow.MultiAction) {
            notifyPlaybackControlActionChanged(action);
        }
    }

    @Override
    public void onItemSelected(
            Presenter.ViewHolder itemViewHolder,
            Object item,
            RowPresenter.ViewHolder rowViewHolder,
            Row row) {
        mCurrentMode = MODE_NOTHING;
        mSelectedActionId = 0;
        if (item != null && item instanceof Action) {
            Action action = (Action) item;
            mSelectedActionId = action.getId();
        }
    }

    @Override
    public boolean handleInputEvent(@NonNull InputEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getKeyCode() != KeyEvent.KEYCODE_DPAD_CENTER) return false;
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (mFastForwardAction != null && mSelectedActionId == mFastForwardAction.getId()) {
                    if (keyEvent.getRepeatCount() == 0) {
                        mCurrentMode = MODE_FAST_FORWARD;
                        invokeFastForwardAction();
                    }
                }
                else if (mRewindAction != null && mSelectedActionId == mRewindAction.getId()) {
                    if (keyEvent.getRepeatCount() == 0) {
                        mCurrentMode = MODE_REWIND;
                        invokeRewindAction();
                    }
                }
            }
            else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                mCurrentMode = MODE_NOTHING;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    public void onEvent(@NonNull Object event) {
        if (event instanceof UpdatePlaybackStateEvent) {
            UpdatePlaybackStateEvent updatePlaybackStateEvent = (UpdatePlaybackStateEvent) event;
            if (updatePlaybackStateEvent.isPlaying()) {
                if (!mIsMediaReady) {
                    setupPlaybackControlItemsToReadyState();
                    mIsMediaReady = true;
                }

                mPlayPauseAction.setIndex(PlayPauseAction.PAUSE);
                setFadingEnabled(true);
            }
            else {
                mPlayPauseAction.setIndex(PlayPauseAction.PLAY);
                setFadingEnabled(false);
            }

            notifyPlaybackControlActionChanged(mPlayPauseAction);
            if (mRowsAdapter != null) mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
        }
        else if (event instanceof PlaybackProgressChangedEvent) {
            // Ignore if currently seeking
            PlaybackProgressChangedEvent progressChangedEvent = (PlaybackProgressChangedEvent) event;
            if (mPlaybackControlsRow.getTotalTime() == 0) {
                mPlaybackControlsRow.setTotalTime((int) progressChangedEvent.getDuration());
            }
            if (mSeek != 0 && mCurrentMode != MODE_NOTHING) {
                return;
            }
            mCurrentTime = (int) progressChangedEvent.getCurrentTime();
            if (!isHidden()) {
                mPlaybackControlsRow.setCurrentTime(mCurrentTime);
                if (mRowsAdapter != null) mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
            }
        }
        else if (event instanceof StreamProgressChangedEvent) {
            StreamProgressChangedEvent streamProgressChangedEvent = (StreamProgressChangedEvent) event;
            mBufferedTime = (int) streamProgressChangedEvent.getBufferedTime();
            if (!isHidden()) {
                mPlaybackControlsRow.setBufferedProgress(mBufferedTime);
                if (mRowsAdapter != null) mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
            }
        }
        else if (event instanceof ToggleSubtitleEvent) {
            ToggleSubtitleEvent toggleSubtitleEvent = (ToggleSubtitleEvent) event;
            if (toggleSubtitleEvent.isEnabled()) {
                mClosedCaptioningAction.setIndex(ClosedCaptioningAction.ON);
            }
            else {
                mClosedCaptioningAction.setIndex(ClosedCaptioningAction.OFF);
            }

            notifyPlaybackControlActionChanged(mClosedCaptioningAction);
        }
    }

    public void toggleSubtitleAction(Boolean enabled) {
        mSubsButtonEnabled = enabled;

        if(mSecondaryActionsAdapter == null)
            return;

        if(enabled) {
            if(mSecondaryActionsAdapter.indexOf(mClosedCaptioningAction) == -1)
                mSecondaryActionsAdapter.add(mClosedCaptioningAction);
        } else {
            mSecondaryActionsAdapter.remove(mClosedCaptioningAction);
        }

        if(mRowsAdapter != null)
            mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
    }

    public void setKeepEventBusRegistration(boolean keepEventBusRegistration) {
        this.mKeepEventBusRegistration = keepEventBusRegistration;
    }

    private void initialisePlaybackControlPresenter() {
        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        mPlaybackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());
        mPlaybackControlsRowPresenter.setSecondaryActionsHidden(false);

        presenterSelector.addClassPresenter(PlaybackControlsRow.class, mPlaybackControlsRowPresenter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mRowsAdapter);
    }

    private void setupPlaybackControlItemsActions() {
        mPlaybackControlsRowPresenter.setOnActionClickedListener(this);
    }

    private void setupPlaybackControlItemsToInitialisingState() {
        mRowsAdapter.clear();
        mPlaybackControlsRow = new PlaybackControlsRow(mStreamInfo);
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);

        Activity activity = getActivity();
        mPlayPauseAction = new PlayPauseAction(activity);
        mPrimaryActionsAdapter.add(mPlayPauseAction);

        setupSecondaryRowPlaybackControl(presenterSelector);

        mRowsAdapter.add(mPlaybackControlsRow);
        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
    }

    private void setupPlaybackControlItemsToReadyState() {
        mRowsAdapter.clear();
        mPlaybackControlsRow = new PlaybackControlsRow(mStreamInfo);
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        setupPrimaryRowPlaybackControl(presenterSelector);
        setupSecondaryRowPlaybackControl(presenterSelector);

        mRowsAdapter.add(mPlaybackControlsRow);
        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());

        setupPlaybackControlItemsActions();
    }

    private void setupPrimaryRowPlaybackControl(@NonNull ControlButtonPresenterSelector presenterSelector) {
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);

        Activity activity = getActivity();
        mPlayPauseAction = new PlayPauseAction(activity);
        mFastForwardAction = new FastForwardAction(activity);
        mRewindAction = new RewindAction(activity);

        if (mPreviousEpisode != null) {
            mSkipPreviousAction = new PlaybackControlsRow.SkipPreviousAction(activity);
            mPrimaryActionsAdapter.add(mSkipPreviousAction);
        }

        // Add main controls to primary adapter.
        mPrimaryActionsAdapter.add(mRewindAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mFastForwardAction);

        if (mNextEpisode != null) {
            mSkipNextAction = new PlaybackControlsRow.SkipNextAction(activity);
            mPrimaryActionsAdapter.add(mSkipNextAction);
        }
    }

    private void setupSecondaryRowPlaybackControl(@NonNull PresenterSelector presenterSelector) {
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        Activity activity = getActivity();

        mScaleVideoAction = new ScaleVideoAction(activity);
        mClosedCaptioningAction = new ClosedCaptioningAction(activity);
        mClosedCaptioningAction.setIcon(getResources().getDrawable(R.drawable.ic_av_subs, null));

        if (mStreamInfo!= null && mStreamInfo.getSubtitleLanguage() != null && !mStreamInfo.getSubtitleLanguage().equals(SubsProvider.SUBTITLE_LANGUAGE_NONE)) {
            mClosedCaptioningAction.setIndex(ClosedCaptioningAction.ON);
        } else {
            mClosedCaptioningAction.setIndex(ClosedCaptioningAction.OFF);
        }

        // Add rest of controls to secondary adapter.
        mSecondaryActionsAdapter.add(mScaleVideoAction);
        if(mSubsButtonEnabled)
        mSecondaryActionsAdapter.add(mClosedCaptioningAction);
    }

    private void notifyPlaybackControlActionChanged(@NonNull Action action) {
        ArrayObjectAdapter adapter = mPrimaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = mSecondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    private void invokeOpenSubtitleSettingsAction() {
        EventBus.getDefault().post(new ConfigureSubtitleEvent());
    }

    private void invokeTogglePlaybackAction(boolean play) {
        if (play) {
            EventBus.getDefault().post(new StartPlaybackEvent());
            setFadingEnabled(true);
        } else {
            EventBus.getDefault().post(new PausePlaybackEvent());
            setFadingEnabled(false);
        }
    }

    private void invokeFastForwardAction() {
        final int refreshDuration = 100;
        final int speedRefreshDuration = 5000;

        Runnable runnablePlayback = new Runnable() {
            @Override
            public void run() {
                if (mCurrentMode == MODE_FAST_FORWARD) {
                    int currentTime = mPlaybackControlsRow.getCurrentTime();
                    currentTime += mFastForwardSpeed;

                    if (currentTime < mPlaybackControlsRow.getTotalTime()) {
                        mPlaybackControlsRow.setCurrentTime(currentTime);
                        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
                        mSeek += mFastForwardSpeed;
                    }

                    mHandlerPlayback.postDelayed(this, refreshDuration);
                } else if (mSelectedActionId == mFastForwardAction.getId()) {
                    triggerFastForwardEvent();
                }
            }
        };

        Runnable runnablePlaybackSpeed = new Runnable() {
            @Override
            public void run() {
                if (mCurrentMode == MODE_FAST_FORWARD) {
                    mFastForwardSpeed *= 2;
                    mHandlerPlaybackSpeed.postDelayed(this, speedRefreshDuration);
                }
                else {
                    mFastForwardSpeed = SeekForwardEvent.MINIMUM_SEEK_SPEED;
                }
            }
        };

        mHandlerPlayback.postDelayed(runnablePlayback, refreshDuration);
        mHandlerPlaybackSpeed.postDelayed(runnablePlaybackSpeed, speedRefreshDuration);
    }

    private void invokeRewindAction() {
        final int refreshDuration = 100;
        final int speedRefreshDuration = 5000;

        Runnable runnablePlayback = new Runnable() {
            @Override
            public void run() {
                if (mCurrentMode == MODE_REWIND) {
                    int currentTime = mPlaybackControlsRow.getCurrentTime();
                    currentTime -= mRewindSpeed;

                    if (currentTime > 0) {
                        mPlaybackControlsRow.setCurrentTime(currentTime);
                        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
                        mSeek += mRewindSpeed;
                    }

                    mHandlerPlayback.postDelayed(this, refreshDuration);
                } else if (mSelectedActionId == mRewindAction.getId()) {
                    triggerRewindEvent();
                }
            }
        };

        Runnable runnablePlaybackSpeed = new Runnable() {
            @Override
            public void run() {
                if (mCurrentMode == MODE_REWIND) {
                    mRewindSpeed *= 2;
                    mHandlerPlaybackSpeed.postDelayed(this, speedRefreshDuration);
                }
                else {
                    mRewindSpeed = SeekBackwardEvent.MINIMUM_SEEK_SPEED;
                }
            }
        };

        mHandlerPlayback.postDelayed(runnablePlayback, refreshDuration);
        mHandlerPlaybackSpeed.postDelayed(runnablePlaybackSpeed, speedRefreshDuration);
    }

    private void triggerFastForwardEvent() {
        SeekForwardEvent event = new SeekForwardEvent();
        event.setSeek(mSeek);
        mSeek = 0;
        EventBus.getDefault().post(event);
    }

    private void triggerRewindEvent() {
        SeekBackwardEvent event = new SeekBackwardEvent();
        event.setSeek(mSeek);
        mSeek = 0;
        EventBus.getDefault().post(event);
    }

    private void invokeScaleVideoAction() {
        EventBus.getDefault().post(new ScaleVideoEvent());
    }

    private void setupTVShowNextPreviousEpisodes() {
        if (mStreamInfo == null) {
            mStreamInfo = ((TVVideoPlayerFragment.Callback) getActivity()).getInfo();
        }

        if (!mStreamInfo.isShow()) {
            return;
        }

        Episode mEpisodeInfo = (Episode) mStreamInfo.getMedia();
        mShow = getActivity().getIntent().getParcelableExtra(TVVideoPlayerActivity.EXTRA_SHOW_INFO);
        if (mShow == null) return;

        SkipEpisodeAsyncTask skipEpisodeAsyncTask = new SkipEpisodeAsyncTask(mEpisodeInfo);
        skipEpisodeAsyncTask.execute(mShow);
    }

    private void playSelectedEpisode(@NonNull final Episode episode) {
        List<Map.Entry<String, Media.Torrent>> torrents = new ArrayList<>(episode.torrents.entrySet());

        if (torrents.size() == 0) {
            // probably will never happen, just in case
            new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.no_video_found))
                .show();
        }
        else if (torrents.size() == 1) {
            final Media.Torrent torrent = torrents.get(0).getValue();
            final String torrentKey = torrents.get(0).getKey();

            new AlertDialog.Builder(getActivity())
                .setTitle(episode.title)
                .setPositiveButton(getString(R.string.play), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onTorrentSelected(episode, torrent, torrentKey);
                    }
                }).show();
        }
        else {
            final ArrayList<String> choices = new ArrayList<>(episode.torrents.keySet());
            final ArrayList<Media.Torrent> torrentArray = new ArrayList<>(episode.torrents.values());
            new AlertDialog.Builder(getActivity())
                .setTitle(episode.title)
                .setSingleChoiceItems(
                    choices.toArray(new CharSequence[choices.size()]),
                    0,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int index) {
                            onTorrentSelected(episode, torrentArray.get(index), choices.get(index));
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    private void onTorrentSelected(@NonNull Episode episode, @NonNull Media.Torrent torrent, @NonNull String torrentKey) {
        if (getActivity() instanceof TorrentActivity) {
            TorrentActivity torrentActivity = (TorrentActivity) getActivity();
            torrentActivity.getTorrentService().stopStreaming();
        }

        String subtitleLanguage = PrefUtils.get(
            getActivity(),
            Prefs.SUBTITLE_DEFAULT,
            SubsProvider.SUBTITLE_LANGUAGE_NONE);

        StreamInfo info = new StreamInfo(
            episode,
            mShow,
            torrent.getUrl(),
            subtitleLanguage,
            torrentKey);

        if (getActivity() instanceof TVVideoPlayerActivity) {
            TVVideoPlayerActivity activity = (TVVideoPlayerActivity) getActivity();
            activity.skipTo(info, mShow);
        }
        else {
            TVStreamLoadingActivity.startActivity(
                    getActivity(),
                    info,
                    mShow);
        }
    }

    /**
     * Detail presenter to allow showing movie or TV show details properly.
     */
    static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            if (!(item instanceof StreamInfo)) return;
            StreamInfo streamInfo = (StreamInfo) item;
            if (streamInfo.isShow()) {
                viewHolder.getTitle().setText(streamInfo.getShowTitle());
                viewHolder.getSubtitle().setText(streamInfo.getShowEpisodeTitle());
            }
            else {
                viewHolder.getTitle().setText(streamInfo.getTitle());
            }
        }
    }

    static class ScaleVideoAction extends Action {
        public ScaleVideoAction(Context context) {
            super(R.id.control_scale);
            setIcon(ResourcesCompat.getDrawable(
                context.getResources(),
                R.drawable.ic_av_aspect_ratio,
                null));
            setLabel1(context.getString(R.string.scale));
        }
    }

    /**
     * Sort available episodes of a TV show. Then determine if a previous or next
     * episode is available. Sorting probably can take a while.
     */
    class SkipEpisodeAsyncTask extends AsyncTask<Show, Void, Void> {

        private final Episode mEpisode;

        public SkipEpisodeAsyncTask(Episode episode) {
            mEpisode = episode;
        }

        @Override
        protected Void doInBackground(Show... shows) {
            for (Show show : shows) {
                Collections.sort(show.episodes, new Comparator<Episode>() {
                    @Override
                    public int compare(Episode me, Episode them) {
                        return (me.season * 10 + me.episode) - (them.season * 10 + them.episode);
                    }
                });

                int episodeIndex = 0;
                int episodes = show.episodes.size() - 1;

                for (Episode episode : show.episodes) {
                    if (mEpisode.season == episode.season && mEpisode.episode == episode.episode) {
                        break;
                    }
                    episodeIndex++;
                }

                if (episodeIndex < episodes) {
                    mNextEpisode = show.episodes.get(episodeIndex + 1);
                }

                if (episodeIndex > 0) {
                    mPreviousEpisode = show.episodes.get(episodeIndex - 1);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mPreviousEpisode != null || mNextEpisode != null) {
                setupPlaybackControlItemsToReadyState();
            }
        }
    }
}