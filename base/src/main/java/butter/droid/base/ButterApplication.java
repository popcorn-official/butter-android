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

package butter.droid.base;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;

import com.sjl.foreground.Foreground;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.picasso.Picasso;

import java.io.File;

import javax.inject.Inject;

import butter.droid.base.beaming.BeamManager;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.manager.updater.ButterUpdateManager;
import butter.droid.base.torrent.TorrentService;
import butter.droid.base.utils.FileUtils;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.StorageUtils;
import butter.droid.base.utils.VersionUtils;
import timber.log.Timber;

public class ButterApplication extends Application implements ButterUpdateManager.Listener {

    private static String sDefSystemLanguage;
    private static ButterApplication sThis;

    @Inject
    Picasso picasso;
    @Inject
    ButterUpdateManager updateManager;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static ButterApplication getAppContext() {
        return sThis;
    }

    public static String getSystemLanguage() {
        return sDefSystemLanguage;
    }

    public static String getStreamDir() {
        File path = new File(PrefUtils.get(getAppContext(), Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(getAppContext()).toString()));
        File directory = new File(path, "/torrents/");
        return directory.toString();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sDefSystemLanguage = LocaleUtils.getCurrentAsString();
    }

    @Override
    public void onTerminate() {
        // Just, so that it exists. Cause it is not executed in production, the whole application is closed anyways on OS level.
        BeamManager.getInstance(getAppContext()).onDestroy();
        super.onTerminate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sThis = this;

        sDefSystemLanguage = LocaleUtils.getCurrentAsString();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
        Foreground.init(this);

        //initialise logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        updateManager.setListener(this);
        updateManager.checkUpdates(false);

        if(VersionUtils.isUsingCorrectBuild()) {
            TorrentService.start(this);
        }

        File path = new File(PrefUtils.get(this, Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(this).toString()));
        File directory = new File(path, "/torrents/");
        if (PrefUtils.get(this, Prefs.REMOVE_CACHE, true)) {
            FileUtils.recursiveDelete(directory);
            FileUtils.recursiveDelete(new File(path + "/subs"));
        } else {
            File statusFile = new File(directory, "status.json");
            if(!statusFile.delete()){
                Timber.w("Could not delete file: " + statusFile.getAbsolutePath());
            }
        }

        Timber.d("StorageLocations: " + StorageUtils.getAllStorageLocations());
        Timber.i("Chosen cache location: " + directory);

        Picasso.setSingletonInstance(picasso);
    }

    @Override
    public void updateAvailable(String updateFile) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (updateFile.length() > 0) {
            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(updateFile));
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notif_logo)
                    .setContentTitle(getString(R.string.update_available))
                    .setContentText(getString(R.string.press_install))
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
            notificationIntent.setDataAndType(uri, ButterUpdateManager.ANDROID_PACKAGE);
            notificationIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));

            nm.notify(ButterUpdateManager.NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
