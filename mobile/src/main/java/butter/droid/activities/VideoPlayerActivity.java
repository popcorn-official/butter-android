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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.MenuItem;

import org.videolan.libvlc.util.AndroidUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.fragments.BaseVideoPlayerFragment;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.torrent.TorrentService;
import butter.droid.fragments.VideoPlayerFragment;
import butter.droid.fragments.dialog.OptionDialogFragment;
import timber.log.Timber;

public class VideoPlayerActivity extends ButterBaseActivity implements VideoPlayerFragment.Callback {

    private VideoPlayerFragment mFragment;
    private StreamInfo mStreamInfo;
    private String mTitle = "";
    private Long mResumePosition;

    public static Intent startActivity(Context context, @NonNull StreamInfo info) {
        return startActivity(context, info, 0);
    }

    public static Intent startActivity(Context context, @NonNull StreamInfo info, long resumePosition) {
        Intent i = new Intent(context, VideoPlayerActivity.class);

        if (info == null){
            throw new IllegalArgumentException("StreamInfo must not be null");
        }

        i.putExtra(INFO, info);
        i.putExtra(BaseVideoPlayerFragment.RESUME_POSITION, resumePosition);
        context.startActivity(i);
        return i;
    }

    public final static String INFO = "stream_info";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

        super.onCreate(savedInstanceState, R.layout.activity_videoplayer);

        mFragment = (VideoPlayerFragment) getSupportFragmentManager().findFragmentById(R.id.video_fragment);

        setShowCasting(true);

        mResumePosition = getIntent().getLongExtra(BaseVideoPlayerFragment.RESUME_POSITION, 0);
        mStreamInfo = getIntent().getParcelableExtra(INFO);

        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            Bundle extras = getIntent().getExtras();
            mStreamInfo = new StreamInfo("");
            /* Started from external application 'content' */
            Uri data = getIntent().getData();
            if (data != null && TextUtils.equals(data.getScheme(), "content")) {


                // Mail-based apps - download the stream to a temporary file and play it
                if(data.getHost().equals("com.fsck.k9.attachmentprovider")
                        || data.getHost().equals("gmail-ls")) {
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        Cursor cursor = getContentResolver().query(data,
                                new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();
                            String filename = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                            cursor.close();
                            Timber.i("Getting file " + filename + " from content:// URI");

                            is = getContentResolver().openInputStream(data);
                            os = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename);
                            byte[] buffer = new byte[1024];
                            int bytesRead = 0;
                            while((bytesRead = is.read(buffer)) >= 0) {
                                os.write(buffer, 0, bytesRead);
                            }
                            mStreamInfo.setVideoLocation(AndroidUtil.PathToUri(Environment.getExternalStorageDirectory().getPath() + "/Download/" + filename).toString());
                        }
                    } catch (Exception e) {
                        Timber.e("Couldn't download file from mail URI");
                        return;
                    } finally {
                        try {
                            if(is != null) is.close();
                            if(os != null) os.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // Media or MMS URI
                else if (TextUtils.equals(data.getAuthority(), "media")){
                    try {
                        Cursor cursor = getContentResolver().query(data,
                                new String[]{ MediaStore.Video.Media.DATA }, null, null, null);
                        if (cursor != null) {
                            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                            if (cursor.moveToFirst())
                                mStreamInfo.setVideoLocation(AndroidUtil.PathToUri(cursor.getString(column_index)).toString());
                            cursor.close();
                        }
                        // other content-based URI (probably file pickers)
                        else {
                            mStreamInfo.setVideoLocation(data.toString());
                        }
                    } catch (Exception e) {
                        mStreamInfo.setVideoLocation(data.toString());
                        if (data.getScheme() == null)
                            mStreamInfo.setVideoLocation(AndroidUtil.PathToUri(data.getPath()).toString());
                        Timber.e("Couldn't read the file from media or MMS");
                    }
                } else {
                    ParcelFileDescriptor inputPFD = null;
                    try {
                        inputPFD = getContentResolver().openFileDescriptor(data, "r");
                        if (AndroidUtil.isHoneycombMr1OrLater())
                            mStreamInfo.setVideoLocation(AndroidUtil.LocationToUri("fd://" + inputPFD.getFd()).toString());
                        else {
                            String fdString = inputPFD.getFileDescriptor().toString();
                            mStreamInfo.setVideoLocation(AndroidUtil.LocationToUri("fd://" + fdString.substring(15, fdString.length() - 1)).toString());
                        }
                    } catch (FileNotFoundException e) {
                        Timber.e("Couldn't understand the intent");
                        return;
                    }
                }
            } /* External application */
            else if (getIntent().getDataString() != null) {
                // Plain URI
                final String location = getIntent().getDataString();
                // Remove VLC prefix if needed
                if (location.startsWith("vlc://")) {
                    mStreamInfo.setVideoLocation(AndroidUtil.LocationToUri(location.substring(6)).toString());
                } else {
                    data = getIntent().getData();
                    if (data.getScheme() == null)
                        mStreamInfo.setVideoLocation(AndroidUtil.PathToUri(data.getPath()).toString());
                    else
                        mStreamInfo.setVideoLocation(data.toString());
                }
            } else {
                Timber.e("Couldn't understand the intent");
                return;
            }

            // Try to get the position
            if(extras != null)
                mResumePosition = extras.getLong("position", -1);
        }

        if(mStreamInfo == null) {
            finish();
            return;
        }

        mTitle = mStreamInfo.getTitle() == null ? getString(R.string.the_video) : mStreamInfo.getTitle();
        mFragment.onMediaReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(null != mService && mService.checkStopped())
            finish();
    }

    @Override
    protected void onPause() {
        if(mService != null)
            mService.removeListener(mFragment);
        super.onPause();
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
    public Long getResumePosition() {
        return mResumePosition;
    }

    @Override
    public StreamInfo getInfo() {
        return mStreamInfo;
    }

    @Override
    public TorrentService getService() {
        return mService;
    }

    @Override
    public void onTorrentServiceDisconnected() {
        if (null!=mFragment){
            mService.removeListener(mFragment);
        }
        super.onTorrentServiceDisconnected();
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

