package butter.droid.base.beaming;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import androidx.appcompat.app.NotificationCompat;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;

public class BeamPlayerNotificationService extends Service {

    private static final Integer NOTIFICATION_ID = 6991;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    private static final String ACTION_REWIND = "action_rewind";
    private static final String ACTION_FAST_FORWARD = "action_fast_foward";
    private static final String ACTION_STOP = "action_stop";

    private BeamManager mManager;
    private MediaControl mMediaControl;
    private Boolean mIsPlaying = false;
    private Bitmap mImage;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;

        String action = intent.getAction();

        if(mMediaControl == null) {
            Intent stopIntent = new Intent( getApplicationContext(), BeamPlayerNotificationService.class );
            stopService(stopIntent);
            return;
        }

        if( action.equalsIgnoreCase( ACTION_PLAY ) || action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            ResponseListener<Object> responseListener = new ResponseListener<Object>() {
                @Override
                public void onSuccess(Object object) {
                    mMediaControl.getPlayState(mPlayStateListener);
                }

                @Override
                public void onError(ServiceCommandError error) {
                    mMediaControl.getPlayState(mPlayStateListener);
                }
            };

            if(mIsPlaying) {
                mIsPlaying = false;
                mMediaControl.pause(responseListener);
                buildNotification(generateAction(R.drawable.ic_av_play, "Play", ACTION_PLAY));
            } else {
                mIsPlaying = true;
                mMediaControl.play(responseListener);
                buildNotification(generateAction(R.drawable.ic_av_pause, "Pause", ACTION_PAUSE));
            }
            mMediaControl.getPlayState(mPlayStateListener);
        } else if( action.equalsIgnoreCase( ACTION_FAST_FORWARD ) ) {
            mMediaControl.getPosition(new MediaControl.PositionListener() {
                @Override
                public void onSuccess(Long object) {
                    long seek = object + 10000;
                    mMediaControl.seek(seek, null);
                }

                @Override
                public void onError(ServiceCommandError error) {

                }
            });
        } else if( action.equalsIgnoreCase( ACTION_REWIND ) ) {
            mMediaControl.getPosition(new MediaControl.PositionListener() {
                @Override
                public void onSuccess(Long object) {
                    long seek = object - 10000;
                    mMediaControl.seek(seek, null);
                }

                @Override
                public void onError(ServiceCommandError error) {

                }
            });
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            mManager.stopVideo();
        }
    }

    private NotificationCompat.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), BeamPlayerNotificationService.class );
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }

    private void buildNotification( NotificationCompat.Action action ) {
        if(mManager.getStreamInfo() == null)
            return;

        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(this, BeamPlayerNotificationService.class);
        intent.setAction( ACTION_STOP );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_logo)
                .setContentTitle(mManager.getStreamInfo().getTitle() == null ? "Video" : mManager.getStreamInfo().getTitle())
                .setContentText(getResources().getString(R.string.app_name))
                .setDeleteIntent(pendingIntent)
                .setStyle(style)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        builder.addAction(generateAction(R.drawable.ic_av_rewind, "Rewind", ACTION_REWIND));
        builder.addAction(action);
        builder.addAction(generateAction(R.drawable.ic_av_forward, "Fast Foward", ACTION_FAST_FORWARD));
        style.setShowActionsInCompactView(0,1,2);

        if(mImage != null) {
            builder.setLargeIcon(mImage);
        }

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify( NOTIFICATION_ID, notification );
    }

    public static void cancelNotification() {
        // Remove beamplayer notification if still available
        NotificationManager notificationManager = (NotificationManager) ButterApplication.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(BeamPlayerNotificationService.NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( mManager == null ) {
            initMediaSessions();
        } else {
            handleIntent(intent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions() {
        mManager = BeamManager.getInstance(this);
        if(mManager.getConnectedDevice() != null) {

            mMediaControl = mManager.getMediaControl();
            mMediaControl.subscribePlayState(mPlayStateListener);
            mManager.addDeviceListener(mDeviceListener);

            mMediaControl.getPlayState(mPlayStateListener);

            if(mManager.getStreamInfo().getImageUrl() != null)
                Picasso.with(this).load(mManager.getStreamInfo().getImageUrl()).resize(400, 400).centerInside().into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        mImage = bitmap;

                        if(!mIsPlaying) {
                            buildNotification( generateAction(R.drawable.ic_av_play, "Play", ACTION_PLAY ) );
                        } else {
                            buildNotification( generateAction(R.drawable.ic_av_pause, "Pause", ACTION_PAUSE ) );
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });

        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }

    private MediaControl.PlayStateListener mPlayStateListener = new MediaControl.PlayStateListener() {
        @Override
        public void onSuccess(MediaControl.PlayStateStatus state) {
            mIsPlaying = state.equals(MediaControl.PlayStateStatus.Playing);

            if(state.equals(MediaControl.PlayStateStatus.Paused)) {
                buildNotification( generateAction(R.drawable.ic_av_play, "Play", ACTION_PLAY ) );
            } else {
                buildNotification( generateAction(R.drawable.ic_av_pause, "Pause", ACTION_PAUSE ) );
            }
        }

        @Override
        public void onError(ServiceCommandError error) {

        }
    };

    private BeamDeviceListener mDeviceListener = new BeamDeviceListener() {
        @Override
        public void onDeviceDisconnected(ConnectableDevice device) {
            super.onDeviceDisconnected(device);

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel( 1 );
            Intent intent = new Intent( getApplicationContext(), BeamPlayerNotificationService.class );
            stopService(intent);
        }
    };

}