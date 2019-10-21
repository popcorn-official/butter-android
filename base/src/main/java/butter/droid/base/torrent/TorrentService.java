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

package butter.droid.base.torrent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamNotInitializedException;
import com.github.se_bastiaan.torrentstreamserver.TorrentStreamServer;
import com.sjl.foreground.Foreground;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butter.droid.base.ButterApplication;
import butter.droid.base.Constants;
import butter.droid.base.R;
import butter.droid.base.activities.TorrentActivity;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.utils.NetworkUtils;
import butter.droid.base.utils.PrefUtils;
import timber.log.Timber;

public class TorrentService extends Service implements TorrentServerListener {

    private static final Integer NOTIFICATION_ID = 3423423;

    private static TorrentService sThis;

    private TorrentStreamServer mTorrentStreamServer;
    private Torrent mCurrentTorrent;
    private String mCurrentStreamUrl;
    private StreamStatus mStreamStatus;

    private boolean mInForeground = false, mIsReady = false, mStopped = false;

    private IBinder mBinder = new ServiceBinder();
    private List<TorrentServerListener> mListener = new ArrayList<>();

    private PowerManager.WakeLock mWakeLock;
    private Class mCurrentActivityClass;
    private Timer mUpdateTimer;

    public class ServiceBinder extends Binder {
        public TorrentService getService() {
            return TorrentService.this;
        }
    }

    private TorrentOptions getTorrentOptions() {
        return new TorrentOptions.Builder()
                .saveLocation(PrefUtils.get(this, Prefs.STORAGE_LOCATION, ButterApplication.getStreamDir()))
                .removeFilesAfterStop(true)
                .maxConnections(PrefUtils.get(this, Prefs.LIBTORRENT_CONNECTION_LIMIT, 200))
                .maxDownloadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_DOWNLOAD_LIMIT, 0))
                .maxUploadSpeed(PrefUtils.get(this, Prefs.LIBTORRENT_UPLOAD_LIMIT, 0))
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sThis = this;
        Foreground.get().addListener(mForegroundListener);
        mTorrentStreamServer = TorrentStreamServer.getInstance();
        mTorrentStreamServer.setServerHost(NetworkUtils.getWifiIPAddress());
        mTorrentStreamServer.setServerPort(Constants.SERVER_PORT);
        mTorrentStreamServer.setTorrentOptions(getTorrentOptions());
        mTorrentStreamServer.startTorrentStream();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();
        mTorrentStreamServer.stopTorrentStream();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind");

        if(mInForeground) {
            stopForeground();
        }

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Timber.d("onRebind");

        if(mInForeground) {
            stopForeground();
        }
    }

    public void setCurrentActivity(TorrentActivity activity) {
        mCurrentActivityClass = activity.getClass();

        if(mInForeground) {
            stopForeground();
            startForeground();
        }
    }

    public void startForeground() {
        if (Foreground.get().isForeground()) return;
        if (mCurrentActivityClass == null) return;

        Intent notificationIntent = new Intent(this, mCurrentActivityClass);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopIntent = new Intent();
        stopIntent.setAction(TorrentBroadcastReceiver.STOP);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this, TorrentBroadcastReceiver.REQUEST_CODE, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(R.drawable.abc_ic_clear_material, getString(R.string.stop), pendingStopIntent).build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_logo)
                .setContentTitle(getString(R.string.app_name) + " - " + getString(R.string.running))
                .setContentText(getString(R.string.tap_to_resume))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .addAction(stopAction)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        if(mStreamStatus != null && mIsReady) {
            String downloadSpeed;
            DecimalFormat df = new DecimalFormat("#############0.00");
            if (mStreamStatus.downloadSpeed / 1024 < 1000) {
                downloadSpeed = df.format(mStreamStatus.downloadSpeed / 1024) + " KB/s";
            } else {
                downloadSpeed = df.format(mStreamStatus.downloadSpeed / (1024 * 1024)) + " MB/s";
            }
            String progress = df.format(mStreamStatus.progress);
            builder.setContentText(progress + "%, ↓" + downloadSpeed);
        }

        Notification notification = builder.build();

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notifManager.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);

        if(mUpdateTimer == null) {
            mUpdateTimer = new Timer();
            mUpdateTimer.scheduleAtFixedRate(new UpdateTask(), 5000, 5000);
        }
    }

    private void stopForeground() {
        stopForeground(true);
        if(mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer.purge();
            mUpdateTimer = null;
        }
    }

    public void streamTorrent(@NonNull final String torrentUrl) {
        Timber.d("streamTorrent");
        mStopped = false;

        if (mTorrentStreamServer.isStreaming()) return;

        Timber.d("Starting streaming");

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if(mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        String WAKE_LOCK = "PCT:TorrentService_WakeLock";
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK);
        mWakeLock.acquire();

        mTorrentStreamServer.setTorrentOptions(getTorrentOptions());

        mIsReady = false;
        mTorrentStreamServer.addListener(this);
        try {
            mTorrentStreamServer.startStream(torrentUrl);
        } catch (IOException | TorrentStreamNotInitializedException e) {
            Timber.e("Error occurred", e);
        }
    }

    public void stopStreaming() {
        mStopped = true;
        mTorrentStreamServer.removeListener(this);

        if (mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();

        if(!mTorrentStreamServer.isStreaming())
            return;

        stopForeground();

        mTorrentStreamServer.stopStream();
        mIsReady = false;

        Timber.d("Stopped torrent and removed files if possible");
    }

    public boolean isStreaming() {
        return mTorrentStreamServer.isStreaming();
    }

    public boolean isReady() {
        return mIsReady;
    }

    public boolean checkStopped() {
        if(mStopped) {
            mStopped = false;
            return true;
        }
        return false;
    }

    public void addListener(@NonNull TorrentServerListener listener) {
        mListener.add(listener);
    }

    public void removeListener(@NonNull TorrentServerListener listener) {
        mListener.remove(listener);
    }

    public static void bindHere(Context context, ServiceConnection serviceConnection) {
        Intent torrentServiceIntent = new Intent(context, TorrentService.class);
        context.bindService(torrentServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public static void start(Context context) {
        Intent torrentServiceIntent = new Intent(context, TorrentService.class);
        context.startService(torrentServiceIntent);
    }

    static void stop() {
        sThis.stopStreaming();
    }

    private Foreground.Listener mForegroundListener = new Foreground.Listener() {
        @Override
        public void onBecameForeground() {
            if (!mTorrentStreamServer.isStreaming()) {
                mTorrentStreamServer.resumeSession();
            } else {
                mInForeground = false;
                stopForeground();
            }
        }

        @Override
        public void onBecameBackground() {
            if (!mTorrentStreamServer.isStreaming()) {
                mTorrentStreamServer.pauseSession();
            } else {
                mInForeground = true;
                startForeground();
            }
        }
    };

    public Torrent getCurrentTorrent() {
        return mCurrentTorrent;
    }

    public String getCurrentTorrentUrl() {
        return mTorrentStreamServer.getCurrentTorrentUrl();
    }

    public String getCurrentStreamUrl() {
        return mCurrentStreamUrl;
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        mCurrentTorrent = torrent;

        for(TorrentServerListener listener : mListener) {
            listener.onStreamPrepared(torrent);
        }
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        for(TorrentServerListener listener : mListener) {
            listener.onStreamStarted(torrent);
        }
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        for(TorrentServerListener listener : mListener) {
            listener.onStreamError(torrent, e);
        }
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        mCurrentTorrent = torrent;

        for(TorrentServerListener listener : mListener) {
            listener.onStreamReady(torrent);
        }
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {
        for(TorrentServerListener listener : mListener) {
            if (null != listener) {
                listener.onStreamProgress(torrent, streamStatus);
            }
        }

        if(mInForeground) {
            mStreamStatus = streamStatus;
        }
    }

    @Override
    public void onStreamStopped() {
        for(TorrentServerListener listener : mListener) {
            if (listener != null) {
                listener.onStreamStopped();
            }
        }
    }

    @Override
    public void onServerReady(String url) {
        mIsReady = true;
        mCurrentStreamUrl = url;

        for(TorrentServerListener listener : mListener) {
            if (listener != null) {
                listener.onServerReady(url);
            }
        }
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            if(mInForeground) {
                startForeground();
            } else {
                stopForeground();
            }
        }
    }

}
