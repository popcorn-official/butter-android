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

package butter.droid.base.beaming;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.discovery.provider.CastDiscoveryProvider;
import com.connectsdk.discovery.provider.FireTVDiscoveryProvider;
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider;
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider;
import com.connectsdk.service.AirPlayService;
import com.connectsdk.service.CastService;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.NetcastTVService;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.beaming.server.BeamServer;
import butter.droid.base.beaming.server.BeamServerService;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.torrent.StreamInfo;
import timber.log.Timber;

/**
 * CastingManager.java
 * <p/>
 * Wrapper for ConnectSDK
 */
public class BeamManager implements ConnectableDeviceListener, DiscoveryManagerListener {

    private static BeamManager sInstance;

    private Context mContext;
    private DiscoveryManager mDiscoveryManager;
    private ConnectableDevice mCurrentDevice;
    private LaunchSession mLaunchSession;
    private Boolean mConnected = false;
    private List<BeamListener> mListeners = new ArrayList<>();
    private List<ConnectableDeviceListener> mDeviceListeners = new ArrayList<>();
    private InputMethodManager mInputManager;
    private EditText mInput;
    private AlertDialog mPairingAlertDialog;
    private AlertDialog mPairingCodeDialog;
    private StreamInfo mStreamInfo;

    private BeamManager(Context context) {
        mContext = context;

        mInput = new EditText(context);
        mInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        mInputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        mPairingAlertDialog =
                new AlertDialog.Builder(context)
                        .setTitle(R.string.pairing_tv)
                        .setMessage(R.string.confirm_tv)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();

        mPairingCodeDialog =
                new AlertDialog.Builder(context)
                        .setTitle(R.string.enter_pairing_code)
                        .setView(mInput)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (mCurrentDevice != null) {
                                    String value = mInput.getText().toString().trim();
                                    mCurrentDevice.sendPairingKey(value);
                                    mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
                            }
                        })
                        .create();

        // CastService.setApplicationID(Constants.CAST_ID); Do not use since suspended by Google
        DiscoveryManager.init(ButterApplication.getAppContext());
        mDiscoveryManager = DiscoveryManager.getInstance();

        mDiscoveryManager.registerDeviceService(CastService.class, CastDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(RokuService.class, SSDPDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(DLNAService.class, SSDPDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(NetcastTVService.class, SSDPDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(WebOSTVService.class, SSDPDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(AirPlayService.class, ZeroconfDiscoveryProvider.class);
        mDiscoveryManager.registerDeviceService(FireTVService.class, FireTVDiscoveryProvider.class);
        mDiscoveryManager.unregisterDeviceService(DIALService.class, SSDPDiscoveryProvider.class);

        mDiscoveryManager.setPairingLevel(DiscoveryManager.PairingLevel.ON);
        mDiscoveryManager.setCapabilityFilters(new CapabilityFilter(
                MediaPlayer.Play_Video,
                MediaControl.Any
        ));
        mDiscoveryManager.start();
        mDiscoveryManager.addListener(this);

        Intent castServerService = new Intent(context, BeamServerService.class);
        context.startService(castServerService);
    }

    public static BeamManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BeamManager(context);
        }

        return sInstance;
    }

    public void onDestroy() {
        mDiscoveryManager.removeListener(this);
        mDiscoveryManager.stop();
        mDiscoveryManager.onDestroy();
        Intent castServerService = new Intent(mContext, BeamServerService.class);
        mContext.stopService(castServerService);
    }

    public Map<String, ConnectableDevice> getDevices() {
        Map<String, ConnectableDevice> devices = mDiscoveryManager.getCompatibleDevices();
        for (Map.Entry<String, ConnectableDevice> entry : devices.entrySet()){
            if (entry.getValue().getServices().isEmpty()) {
                devices.remove(entry.getKey());
            }
        }
        return devices;
    }

    public ConnectableDevice getConnectedDevice() {
        return mCurrentDevice;
    }

    public boolean hasCastDevices() {
        return mDiscoveryManager.getCompatibleDevices().size() > 0;
    }

    public boolean isConnected() {
        return mConnected;
    }

    public MediaControl getMediaControl() {
        return mCurrentDevice.getCapability(MediaControl.class);
    }

    public VolumeControl getVolumeControl() {
        if (mCurrentDevice != null && mCurrentDevice.hasCapability(VolumeControl.Volume_Get) && mCurrentDevice.hasCapability(VolumeControl.Volume_Get) && mCurrentDevice.hasCapability(VolumeControl.Volume_Subscribe)) {
            return mCurrentDevice.getCapability(VolumeControl.class);
        }
        return null;
    }

    public boolean stopVideo() {
        if (!mConnected || mLaunchSession == null) return false;

        mLaunchSession.close(null);
        mLaunchSession = null;

        mStreamInfo = null;

        return true;
    }

    public void playVideo(StreamInfo info) {
        playVideo(info, null);
    }

    public void playVideo(StreamInfo info, final MediaPlayer.LaunchListener listener) {
        if (!mConnected) listener.onError(ServiceCommandError.getError(503));

        mStreamInfo = info;

        String location = info.getVideoLocation();
        if(!location.startsWith("http")) {
            BeamServer.setCurrentVideo(location);
            location = BeamServer.getVideoURL();
        }

        String subsLocation = null;
        if(info.getSubtitleLanguage() != null && !info.getSubtitleLanguage().isEmpty() && !info.getSubtitleLanguage().equals("no-subs")) {
            File srtFile = new File(SubsProvider.getStorageLocation(mContext), mStreamInfo.getMedia().videoId + "-" + mStreamInfo.getSubtitleLanguage() + ".srt");
            BeamServer.setCurrentSubs(srtFile);
            if(mCurrentDevice.hasCapability(MediaPlayer.Subtitles_Vtt)) {
                subsLocation = BeamServer.getSubsURL(BeamServer.VTT);
            } else if (mCurrentDevice.hasCapability(MediaPlayer.Subtitles_Srt)) {
                subsLocation = BeamServer.getSubsURL(BeamServer.SRT);
            }
        } else {
            BeamServer.removeSubs();
        }

        try {
            URL url = new URL(location);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            location = uri.toString();

            if(subsLocation != null) {
                URL subsUrl = new URL(subsLocation);
                URI subsUri = new URI(subsUrl.getProtocol(), subsUrl.getUserInfo(), subsUrl.getHost(), subsUrl.getPort(), subsUrl.getPath(), subsUrl.getQuery(), subsUrl.getRef());
                subsLocation = subsUri.toString();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        String title = info.getTitle();
        String imageUrl = info.getImageUrl() == null ? "https://butterproject.org/images/header-logo.png" : info.getImageUrl();

        //String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, LaunchListener listener
        if (mCurrentDevice != null) {
            MediaInfo mediaInfo = new MediaInfo(location, subsLocation, "video/mp4", title, "");
            mediaInfo.addImages(new ImageInfo(imageUrl));
            mCurrentDevice.getCapability(MediaPlayer.class).playMedia(mediaInfo, false, new MediaPlayer.LaunchListener() {
                @Override
                public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                    mLaunchSession = object.launchSession;
                    if (listener != null)
                        listener.onSuccess(object);
                }

                @Override
                public void onError(ServiceCommandError error) {
                    Timber.e(error.getMessage());
                    if (listener != null)
                        listener.onError(error);
                }
            });
        }
    }

    public void connect(ConnectableDevice castingDevice) {
        if (castingDevice == mCurrentDevice) return;

        if (mCurrentDevice != null) {
            mCurrentDevice.removeListener(this);
            mCurrentDevice.disconnect();
            mConnected = false;
        }

        mCurrentDevice = castingDevice;

        if (castingDevice != null) {
            mCurrentDevice.addListener(this);
            mCurrentDevice.connect();
        }
    }

    public void disconnect() {
        if (mCurrentDevice != null) {
            mConnected = false;

            mCurrentDevice.disconnect();
            mCurrentDevice.removeListener(this);
        }

        for (BeamListener listener : mListeners)
            listener.updateBeamIcon();

        onDeviceDisconnected(mCurrentDevice);

        mCurrentDevice = null;
    }

    public void addDiscoveryListener(DiscoveryManagerListener listener) {
        mDiscoveryManager.addListener(listener);
    }

    public void removeDiscoveryListener(DiscoveryManagerListener listener) {
        mDiscoveryManager.removeListener(listener);
    }

    public void addListener(BeamListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(BeamListener listener) {
        mListeners.remove(listener);
    }

    public void addDeviceListener(ConnectableDeviceListener listener) {
        mDeviceListeners.add(listener);
    }

    public void removeDeviceListener(ConnectableDeviceListener listener) {
        mDeviceListeners.remove(listener);
    }

    public StreamInfo getStreamInfo() {
        return mStreamInfo;
    }

    @Override
    public void onDeviceReady(ConnectableDevice device) {
        if (mPairingAlertDialog.isShowing()) {
            mPairingAlertDialog.dismiss();
        }

        mConnected = true;
        for (BeamListener listener : mListeners)
            listener.updateBeamIcon();

        for(ConnectableDeviceListener listener : mDeviceListeners)
            listener.onDeviceReady(device);
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice device) {
        mConnected = false;
        for (BeamListener listener : mListeners)
            listener.updateBeamIcon();

        for(ConnectableDeviceListener listener : mDeviceListeners)
            listener.onDeviceDisconnected(device);
    }

    @Override
    public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {
        switch (pairingType) {
            case FIRST_SCREEN:
                mPairingAlertDialog.show();
                break;
            case PIN_CODE:
                mPairingCodeDialog.show();
                break;
            case NONE:
            default:
                break;
        }

        for(ConnectableDeviceListener listener : mDeviceListeners)
            listener.onPairingRequired(device, service, pairingType);
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {
        for(ConnectableDeviceListener listener : mDeviceListeners)
            listener.onCapabilityUpdated(device, added, removed);
    }

    @Override
    public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
        Toast.makeText(mContext, R.string.unknown_error, Toast.LENGTH_SHORT).show();
        for(ConnectableDeviceListener listener : mDeviceListeners)
            listener.onConnectionFailed(device, error);
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        for (BeamListener listener : mListeners)
            listener.updateBeamIcon();
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {

    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
        if (device == mCurrentDevice) {
            mConnected = false;
            mCurrentDevice = null;
        }

        for (BeamListener listener : mListeners)
            listener.updateBeamIcon();
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {

    }

    public interface BeamListener {
        void updateBeamIcon();
    }

}
