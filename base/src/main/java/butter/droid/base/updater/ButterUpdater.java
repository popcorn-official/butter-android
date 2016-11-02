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

package butter.droid.base.updater;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;

import com.google.gson.Gson;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import okio.BufferedSink;
import okio.Okio;
import butter.droid.base.BuildConfig;
import butter.droid.base.Constants;
import butter.droid.base.ButterApplication;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.utils.NetworkUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.VersionUtils;

public class ButterUpdater extends Observable {

    private static ButterUpdater sThis;

    public static int NOTIFICATION_ID = 0x808C049;
    public static final String STATUS_NO_UPDATE = "no_updates";
    public static final String STATUS_GOT_UPDATE = "got_update";

    private static final long MINUTES = 60 * 1000;
    private static final long HOURS = 60 * MINUTES;
    private static final long DAYS = 24 * HOURS;
    private static final long WAKEUP_INTERVAL = 15 * MINUTES;
    private long UPDATE_INTERVAL = 3 * HOURS;

    public static final String ANDROID_PACKAGE = "application/vnd.android.package-archive";
    private final String DATA_URLS[] = BuildConfig.UPDATE_URLS;
    private Integer mCurrentUrl = 0;

    public static final String LAST_UPDATE_CHECK = "update_check";
    private static final String LAST_UPDATE_KEY = "last_update";
    public static final String UPDATE_FILE = "update_file";
    private static final String SHA1_TIME = "sha1_update_time";
    private static final String SHA1_KEY = "sha1_update";

    private final OkHttpClient mHttpClient = ButterApplication.getHttpClient();
    private final Gson mGson = new Gson();
    private final Handler mUpdateHandler = new Handler();

    private Context mContext = null;
    private long lastUpdate = 0;
    private String mPackageName;
    private Integer mVersionCode;
    private String mVariantStr;
    private String mChannelStr;
    private String mAbi;

    private Listener mListener;

    private ButterUpdater(Context context) {
        if (BuildConfig.DEBUG) {
            UPDATE_INTERVAL = 3 * HOURS;
        } else {
            UPDATE_INTERVAL = 2 * DAYS;
        }

        mContext = context;
        mPackageName = context.getPackageName();

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(mPackageName, 0);
            mVersionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        lastUpdate = PrefUtils.get(mContext, LAST_UPDATE_KEY, 0l);
        NOTIFICATION_ID += crc32(mPackageName);

        ApplicationInfo appinfo = context.getApplicationInfo();

        if (new File(appinfo.sourceDir).lastModified() > PrefUtils.get(mContext, SHA1_TIME, 0l)) {
            PrefUtils.save(mContext, SHA1_KEY, SHA1(appinfo.sourceDir));
            PrefUtils.save(mContext, SHA1_TIME, System.currentTimeMillis());

            String updateFile = PrefUtils.get(mContext, UPDATE_FILE, "");
            if (updateFile.length() > 0) {
                if (new File(updateFile).delete()) {
                    PrefUtils.remove(mContext, UPDATE_FILE);
                }
            }
        }

        context.registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public static ButterUpdater getInstance(Context context) {
        if (sThis == null) {
            sThis = new ButterUpdater(context);
        }
        sThis.mContext = context;
        return sThis;
    }

    public static ButterUpdater getInstance(Context context, Listener listener) {
        ButterUpdater instance = getInstance(context);
        instance.setListener(listener);
        return instance;
    }

    public void setListener(Listener  listener) {
        mListener = listener;
    }

    private Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            checkUpdates(false);
            mUpdateHandler.removeCallbacks(periodicUpdate);
            mUpdateHandler.postDelayed(this, WAKEUP_INTERVAL);
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void checkUpdates(boolean forced) {
        long now = System.currentTimeMillis();

        if ((!PrefUtils.get(mContext, Prefs.AUTOMATIC_UPDATES, true) || (PrefUtils.get(mContext, Prefs.WIFI_ONLY, true) && !NetworkUtils.isWifiConnected(mContext))) && !forced) {
            return;
        }

        PrefUtils.save(mContext, LAST_UPDATE_CHECK, now);

        if (forced || (lastUpdate + UPDATE_INTERVAL) < now) {
            lastUpdate = System.currentTimeMillis();
            PrefUtils.save(mContext, LAST_UPDATE_KEY, lastUpdate);

            if (!forced) return;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mAbi = Build.CPU_ABI.toLowerCase(Locale.US);
            } else {
                mAbi = Build.SUPPORTED_ABIS[0].toLowerCase(Locale.US);
            }

            if (mPackageName.contains("tv")) {
                mVariantStr = "tv";
            } else {
                mVariantStr = "mobile";
            }

            if (BuildConfig.RELEASE_TYPE.toLowerCase(Locale.US).contains("release")) {
                mChannelStr = "release";
            } else {
                mChannelStr = BuildConfig.GIT_BRANCH;
            }

            Request request = new Request.Builder()
                    .url(DATA_URLS[mCurrentUrl])
                    .build();

            mHttpClient.newCall(request).enqueue(mCallback);
        } else if(PrefUtils.contains(mContext, UPDATE_FILE)) {
            String fileName = PrefUtils.get(mContext, UPDATE_FILE, "");
            if (fileName.length() > 0) {
                if (!new File(fileName).exists()) {
                    PrefUtils.remove(mContext, UPDATE_FILE);
                } else {
                    if(mListener != null)
                        mListener.updateAvailable(fileName);
                }
            }
        }
    }

    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(Request request, IOException e) {
            if(mCurrentUrl < DATA_URLS.length - 1) {
                mCurrentUrl++;
                Request newRequest = new Request.Builder()
                        .url(DATA_URLS[mCurrentUrl])
                        .build();

                mHttpClient.newCall(newRequest).enqueue(mCallback);
            } else {
                setChanged();
                notifyObservers(STATUS_NO_UPDATE);
            }
        }

        @Override
        public void onResponse(Response response) {
            try {
                if (response.isSuccessful()) {
                    UpdaterData data = mGson.fromJson(response.body().string(), UpdaterData.class);
                    Map<String, Map<String, UpdaterData.Arch>> variant;
                    if (mVariantStr.equals("tv")) {
                        variant = data.tv;
                    } else {
                        variant = data.mobile;
                    }

                    UpdaterData.Arch channel = null;
                    if (variant.containsKey(mChannelStr) && variant.get(mChannelStr).containsKey(mAbi)) {
                        channel = variant.get(mChannelStr).get(mAbi);
                    }

                    ApplicationInfo appinfo = mContext.getApplicationInfo();
                    if ((channel == null || channel.checksum.equals(SHA1(appinfo.sourceDir)) || channel.versionCode <= mVersionCode) && VersionUtils.isUsingCorrectBuild()) {
                        setChanged();
                        notifyObservers(STATUS_NO_UPDATE);
                    } else {
                        downloadFile(channel.updateUrl);
                        setChanged();
                        notifyObservers(STATUS_GOT_UPDATE);
                    }
                } else {
                    setChanged();
                    notifyObservers(STATUS_NO_UPDATE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void downloadFile(final String location) {
        Request request = new Request.Builder()
                .url(location)
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                // uhoh
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String fileName = location.substring(location.lastIndexOf('/') + 1);
                    File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                    sink.writeAll(response.body().source());
                    sink.close();

                    String updateFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName;

                    PrefUtils.getPrefs(mContext).edit()
                            .putString(SHA1_KEY, SHA1(updateFilePath))
                            .putString(UPDATE_FILE, updateFilePath)
                            .putLong(SHA1_TIME, System.currentTimeMillis())
                            .apply();

                    if(mListener != null) {
                        mListener.updateAvailable(updateFilePath);
                    }
                }
            }
        });
    }

    public void checkUpdatesManually() {
        checkUpdates(true);
    }

    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // do application-specific task(s) based on the current network state, such
            // as enabling queuing of HTTP requests when currentNetworkInfo is connected etc.
            if (NetworkUtils.isWifiConnected(context)) {
                checkUpdates(false);
                mUpdateHandler.postDelayed(periodicUpdate, UPDATE_INTERVAL);
            } else {
                mUpdateHandler.removeCallbacks(periodicUpdate);    // no network anyway
            }
        }
    };

    private String SHA1(String filename) {
        final int BUFFER_SIZE = 8192;
        byte[] buf = new byte[BUFFER_SIZE];
        int length;
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(filename));
            MessageDigest md = MessageDigest.getInstance("SHA1");
            while ((length = bis.read(buf)) != -1) {
                md.update(buf, 0, length);
            }

            byte[] array = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "sha1bad";
    }

    private static int crc32(String str) {
        byte bytes[] = str.getBytes();
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return (int) checksum.getValue();
    }

    public interface Listener {
        void updateAvailable(String fileName);
    }

}