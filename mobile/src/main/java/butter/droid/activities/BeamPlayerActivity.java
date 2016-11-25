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

package butter.droid.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.beaming.BeamManager;
import butter.droid.base.beaming.server.BeamServerService;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentService;
import butter.droid.fragments.BeamPlayerFragment;
import butter.droid.fragments.VideoPlayerFragment;
import butter.droid.fragments.dialog.OptionDialogFragment;

public class BeamPlayerActivity extends ButterBaseActivity implements VideoPlayerFragment.Callback {

    private BeamPlayerFragment mFragment;
    private BeamManager mBeamManager = BeamManager.getInstance(this);
    private StreamInfo mStreamInfo;
    private Long mResumePosition;
    private String mTitle;

    public static Intent startActivity(Context context, @NonNull StreamInfo info) {
        return startActivity(context, info, 0);
    }

    public static Intent startActivity(Context context, @NonNull StreamInfo info, long resumePosition) {
        Intent i = new Intent(context, BeamPlayerActivity.class);

        if (info == null){
            throw new IllegalArgumentException("StreamInfo must not be null");
        }

        i.putExtra(INFO, info);
        i.putExtra(RESUME_POSITION, resumePosition);
        context.startActivity(i);
        return i;
    }

    public final static String INFO = "stream_info";
    public final static String RESUME_POSITION = "resume_position";

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

        super.onCreate(savedInstanceState, R.layout.activity_beamplayer);

        setShowCasting(true);

        BeamServerService.getServer().start();

        mStreamInfo = getIntent().getParcelableExtra(INFO);

        mResumePosition = getIntent().getLongExtra(RESUME_POSITION, 0);

        mTitle = mStreamInfo.getTitle() == null ? getString(R.string.the_video) : mStreamInfo.getTitle();

        /*
        File subsLocation = new File(SubsProvider.getStorageLocation(context), media.videoId + "-" + subLanguage + ".srt");
        BeamServer.setCurrentSubs(subsLocation);
         */

        mFragment = (BeamPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.beam_fragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(null != mService && mService.checkStopped())
            finish();
    }

    @Override
    protected void onStop() {
        if(null != mService)
            mService.removeListener(mFragment);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showExitDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void showExitDialog() {
        OptionDialogFragment.show(getSupportFragmentManager(), getString(R.string.leave_videoplayer_title), String.format(getString(R.string.leave_videoplayer_message), mTitle), getString(android.R.string.yes), getString(android.R.string.no), new OptionDialogFragment.Listener() {
            @Override
            public void onSelectionPositive() {
                mBeamManager.stopVideo();
                BeamServerService.getServer().stop();
                if (mService != null)
                    mService.stopStreaming();
                finish();
            }

            @Override
            public void onSelectionNegative() {
            }
        });
    }

    @Override
    public StreamInfo getInfo() {
          return mStreamInfo;
    }

    @Override
    public TorrentService getService() {
        return mService;
    }

    public Long getResumePosition() {
        return mResumePosition;
    }

    @Override
    public void onTorrentServiceConnected() {
        super.onTorrentServiceConnected();

        if(mService.checkStopped()) {
            finish();
            return;
        }

        mService.addListener(mFragment);
    }
}
