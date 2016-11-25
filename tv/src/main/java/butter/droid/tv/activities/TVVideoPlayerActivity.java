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

package butter.droid.tv.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import butter.droid.base.fragments.BaseVideoPlayerFragment;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentService;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.base.TVBaseActivity;
import butter.droid.tv.fragments.TVPlaybackOverlayFragment;
import butter.droid.tv.fragments.TVVideoPlayerFragment;
import butterknife.ButterKnife;

public class TVVideoPlayerActivity extends TVBaseActivity implements TVVideoPlayerFragment.Callback {

    private TVVideoPlayerFragment mPlayerFragment;
    private TVPlaybackOverlayFragment mPlaybackOverlayFragment;

    public final static String EXTRA_STREAM_INFO = "stream_info";
    public final static String EXTRA_SHOW_INFO = "episode_info";

    private StreamInfo mStreamInfo;
    private boolean mIsBackPressed = false;
    private boolean mCurrentStreamStopped = false;

    public static Intent startActivity(Context context, StreamInfo info) {
        return startActivity(context, info, 0);
    }

    public static Intent startActivity(
        Context context,
        StreamInfo info,
        @SuppressWarnings("UnusedParameters") long resumePosition) {
        Intent i = new Intent(context, TVVideoPlayerActivity.class);
        i.putExtra(EXTRA_STREAM_INFO, info);
        // todo: resume position
        context.startActivity(i);
        return i;
    }

    public static Intent startActivity(Context context, StreamInfo info, Show show) {
        Intent i = new Intent(context, TVVideoPlayerActivity.class);
        i.putExtra(EXTRA_STREAM_INFO, info);
        i.putExtra(EXTRA_SHOW_INFO, show);
        // todo: resume position
        context.startActivity(i);
        return i;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mIsBackPressed = true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        super.onCreate(savedInstanceState, R.layout.activity_videoplayer);
        ButterKnife.bind(this);

        createStreamInfo();

        mPlayerFragment = (TVVideoPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        mPlaybackOverlayFragment = (TVPlaybackOverlayFragment) getSupportFragmentManager().findFragmentById(R.id.playback_overlay_fragment);

        mPlayerFragment.onMediaReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsBackPressed = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mIsBackPressed) {
            mPlayerFragment.deactivateMediaSession();
        }

        if (mPlayerFragment.isMediaSessionActive()) {
            mPlaybackOverlayFragment.setKeepEventBusRegistration(true);
        }
        else {
            mPlaybackOverlayFragment.setKeepEventBusRegistration(false);
            PrefUtils.save(this, BaseVideoPlayerFragment.RESUME_POSITION, 0);
        }
    }

    @Override
    protected void onDestroy() {
        if (!mCurrentStreamStopped) {
            mService.stopStreaming();
            mCurrentStreamStopped = true;
        }
        super.onDestroy();
    }

    @Override
    public void onVisibleBehindCanceled() {
        mPlayerFragment.pause();
        super.onVisibleBehindCanceled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public StreamInfo getInfo() {
        if (mStreamInfo == null)
            createStreamInfo();
        return mStreamInfo;
    }

    @Override
    public TorrentService getService() {
        return mService;
    }

    @Override
    public void onTorrentServiceConnected() {
        mService.addListener(mPlayerFragment);
    }

    @Override
    public void onTorrentServiceDisconnected() {
        mService.removeListener(mPlayerFragment);
    }

    @Override
    public Long getResumePosition() {
        //todo: Implement ResumePosition on Android TV
        return 0L;
    }

    private void createStreamInfo() {
        mStreamInfo = getIntent().getParcelableExtra(EXTRA_STREAM_INFO);
        String location = mStreamInfo.getVideoLocation();

        if (!location.startsWith("file://") && !location.startsWith("http://") && !location.startsWith("https://")) {
            location = "file://" + location;
        }

        mStreamInfo.setVideoLocation(location);
    }

    public void skipTo(StreamInfo info, Show show) {
        mService.stopStreaming();
        mCurrentStreamStopped = true;

        mPlayerFragment.pause();
        mPlayerFragment.onPlaybackEndReached();

        finish();

        TVStreamLoadingActivity.startActivity(this, info, show);
    }
}
