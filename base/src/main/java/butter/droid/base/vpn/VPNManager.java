/*
 * This file is part of Popcorn Time.
 *
 * Popcorn Time is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Popcorn Time is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Popcorn Time. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.vpn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import ht.vpn.android.api.IOpenVPNAPIService;
import ht.vpn.android.api.IOpenVPNStatusCallback;
import butter.droid.base.utils.PackageUtils;
import butter.droid.base.utils.ThreadUtils;

public class VPNManager {

    public enum State {CONNECTED, CONNECTING, DISCONNECTED}

    private static final int START_PROFILE_EMBEDDED = 1;
    private static final int OPENVPN_PERMISSION = 2;
    public static final String PACKAGE_VPNHT = "ht.vpn.android";

    private Activity mActivity;
    private IOpenVPNAPIService mService = null;
    private Listener mListener;
    private static VPNManager sLatestInstance;
    private static State sCurrentState = State.DISCONNECTED;
    private boolean mIsInstalled = false;

    public static VPNManager start(Activity activity) {
        if(activity instanceof Listener) {
            VPNManager manager = new VPNManager(activity);
            if(PackageUtils.isInstalled(activity, PACKAGE_VPNHT)) {
                manager.init();
            }
            sLatestInstance = manager;
            return manager;
        }
        throw new UnsupportedOperationException("Activity does not implement VPNManager.Listener");
    }

    public VPNManager(Activity activity) {
        mActivity = activity;
        mListener = (Listener) mActivity;
    }

    public static VPNManager getLatestInstance() {
        if(sLatestInstance == null) {
            sLatestInstance = new VPNManager(null);
        }
        return sLatestInstance;
    }

    public static State getCurrentState() {
        return sCurrentState;
    }

    private void init() {
        mIsInstalled = true;
        Intent serviceIntent = new Intent(IOpenVPNAPIService.class.getName());
        serviceIntent.setPackage(PACKAGE_VPNHT);
        mActivity.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void stop() {
        mListener = null;
        if(mService != null) {
            try {
                mService.unregisterStatusCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mActivity.unbindService(mServiceConnection);
        }
    }

    public void connect() {
        if(mActivity == null || mService == null) return;

        try {
            Intent intent = mService.prepareVPNService();
            if (intent == null) {
                onActivityResult(START_PROFILE_EMBEDDED, Activity.RESULT_OK, null);
            } else {
                mActivity.startActivityForResult(intent, START_PROFILE_EMBEDDED);
            }
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if(mService == null) return;
        try {
                mService.disconnect();
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startEmbeddedProfile()
    {
        BufferedReader br = null;
        try {
            InputStream conf = mActivity.getAssets().open("vpnht.conf");
            br = new BufferedReader(new InputStreamReader(conf));
            StringBuilder config = new StringBuilder();
            String line;
            while(true) {
                line = br.readLine();
                if(line == null)
                    break;
                config.append(line).append("\n");
            }
            br.readLine();

            mService.startVPN("Popcorn Time", config.toString());
        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        if(mService != null) {
            try {
                return mService.isConnectedOrConnecting();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isVPNInstalled() {
        return mIsInstalled;
    }

    public void onActivityResult(int requestCode, int resultCode, Bundle data) {
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == START_PROFILE_EMBEDDED) {
                startEmbeddedProfile();
            } else if (requestCode == OPENVPN_PERMISSION) {
                try {
                    mService.registerStatusCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mService = IOpenVPNAPIService.Stub.asInterface(binder);
            try {
                Intent intent = mService.prepare(mActivity.getPackageName());
                if (intent != null) {
                    mActivity.startActivityForResult(intent, OPENVPN_PERMISSION);
                } else {
                    onActivityResult(OPENVPN_PERMISSION, Activity.RESULT_OK, null);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if(mListener != null) {
                mListener.onVPNServiceReady();

                try {
                    if (mService.isConnectedOrConnecting())
                        mListener.onVPNStatusUpdate(State.CONNECTED, "");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

    private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
        @Override
        public void newStatus(String uuid, final String state, final String message, String level) throws RemoteException {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mListener != null)
                        mListener.onVPNStatusUpdate(strToState(state), message);
                }
            });
        }
    };

    private State strToState(String state) {
        if(state.equalsIgnoreCase("CONNECTED")) {
            sCurrentState = State.CONNECTED;
        } else if(state.equalsIgnoreCase("NOPROCESS") || state.equalsIgnoreCase("USER_VPN_PASSWORD_CANCELLED")) {
            sCurrentState = State.DISCONNECTED;
        } else {
            sCurrentState = State.CONNECTING;
        }
        return sCurrentState;
    }

    public interface Listener {
        public void onVPNServiceReady();
        public void onVPNStatusUpdate(State state, String message);
    }

}
