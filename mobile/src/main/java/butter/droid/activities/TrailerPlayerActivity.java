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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;

import java.net.URLDecoder;

import javax.inject.Inject;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.manager.youtube.YouTubeManager;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentService;
import butter.droid.fragments.VideoPlayerFragment;

public class TrailerPlayerActivity extends ButterBaseActivity implements VideoPlayerFragment.Callback {

    private StreamInfo mStreamInfo;
    private Media mMedia;
    private VideoPlayerFragment mVideoPlayerFragment;

    @Inject
    YouTubeManager youTubeManager;

    public final static String LOCATION = "stream_url";
    public final static String DATA = "video_data";

    public static Intent startActivity(Activity activity, String youTubeUrl, Media data) {
        Intent i = new Intent(activity, TrailerPlayerActivity.class);
        i.putExtra(DATA, data);
        i.putExtra(LOCATION, youTubeUrl);
        activity.startActivity(i);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        super.onCreate(savedInstanceState, R.layout.activity_videoplayer);

        mMedia = getIntent().getParcelableExtra(DATA);
        mMedia.title += " " + getString(R.string.trailer);
        String youTubeUrl = getIntent().getStringExtra(LOCATION);

        mStreamInfo = new StreamInfo(mMedia, null, null, null, null, null);

        mVideoPlayerFragment = (VideoPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.video_fragment);
        mVideoPlayerFragment.enableSubsButton(false);

        QueryYouTubeTask youTubeTask = new QueryYouTubeTask(youTubeManager);
        youTubeTask.execute(youTubeManager.getYouTubeVideoId(youTubeUrl));
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
    public Long getResumePosition() {
        return 0L;
    }

    @Override
    public StreamInfo getInfo() {
        return mStreamInfo;
    }

    @Override
    public TorrentService getService() {
        return null;
    }

    private class QueryYouTubeTask extends AsyncTask<String, Void, Uri> {

        private final YouTubeManager youTubeManager;

        private boolean mShowedError = false;

        private QueryYouTubeTask(YouTubeManager youTubeManager) {
            this.youTubeManager = youTubeManager;
        }

        @Override
        protected Uri doInBackground(String... params) {
            String uriStr = null;
            String quality = "17";   // 3gpp medium quality, which should be fast enough to view over EDGE connection
            String videoId = params[0];

            if (isCancelled())
                return null;

            try {
                WifiManager wifiManager = (WifiManager) TrailerPlayerActivity.this.getSystemService(Context.WIFI_SERVICE);
                TelephonyManager telephonyManager = (TelephonyManager) TrailerPlayerActivity.this.getSystemService(Context.TELEPHONY_SERVICE);

                // if we have a fast connection (wifi or 3g), then we'll get a high quality YouTube video
                if (wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getIpAddress() != 0) {
                    quality = "22";
                } else if (telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED &&
                        (
                                telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS ||
                                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSUPA ||
                                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSPA ||
                                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_HSDPA ||
                                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EVDO_A
                        )
                        ) {
                    quality = "18";
                }

                if (isCancelled())
                    return null;

                ////////////////////////////////////
                // calculate the actual URL of the video, encoded with proper YouTube token
                uriStr = youTubeManager.calculateYouTubeUrl(quality, true, videoId);

                if (isCancelled())
                    return null;

            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Error occurred while retrieving information from YouTube.", e);
            }

            if (uriStr != null) {
                return Uri.parse(uriStr);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri result) {
            super.onPostExecute(result);

            try {
                if (isCancelled())
                    return;

                if (result == null) {
                    throw new RuntimeException("Invalid NULL Url.");
                }

                mStreamInfo.setVideoLocation(URLDecoder.decode(result.toString()));

                mVideoPlayerFragment.onMediaReady();
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Error playing video!", e);

                if (!mShowedError) {
                    showErrorAlert();
                }
            }
        }

        private void showErrorAlert() {
            try {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TrailerPlayerActivity.this);
                alertDialogBuilder.setTitle(R.string.comm_error);
                alertDialogBuilder.setCancelable(false);
                alertDialogBuilder.setMessage(R.string.comm_message);

                alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TrailerPlayerActivity.this.finish();
                    }
                });

                AlertDialog lDialog = alertDialogBuilder.create();
                lDialog.show();
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Problem showing error dialog.", e);
            }
        }

        @Override
        protected void onProgressUpdate(Void... pValues) {
            super.onProgressUpdate(pValues);
        }

    }

}