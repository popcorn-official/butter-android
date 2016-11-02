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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.AirPlayService;
import com.connectsdk.service.CastService;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.NetcastTVService;
import com.connectsdk.service.RokuService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.command.ServiceCommandError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.Bind;
import butter.droid.base.R;
import butter.droid.base.utils.ThreadUtils;

public class BeamDeviceAdapter extends BaseAdapter {

    private Context mContext;
    private BeamManager mBeamManager;
    private Map<String, ConnectableDevice> mDevices = new HashMap<>();
    private ArrayList<String> mKeys = new ArrayList<>();

    static class ViewHolder {
        @Bind(android.R.id.icon)
        ImageView icon;
        @Bind(android.R.id.text1)
        TextView text1;
        @Bind(android.R.id.text2)
        TextView text2;

        public ViewHolder(View v) {
            ButterKnife.bind(this, v);
        }
    }

    public BeamDeviceAdapter(Context context) {
        mContext = context;
        mBeamManager = BeamManager.getInstance(context);

        mDevices = mBeamManager.getDevices();
        mKeys = new ArrayList<>(mDevices.keySet());

        mBeamManager.addDiscoveryListener(mListener);
    }

    public void destroy() {
        mBeamManager.removeDiscoveryListener(mListener);
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public ConnectableDevice getItem(int position) {
        return mDevices.get(mKeys.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.casting_dialog_listitem, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ConnectableDevice device = getItem(position);

        int imgResource = R.drawable.ic_dlna;
        String serviceText = "";
        for (DeviceService service : device.getServices()) {
            String addText = "";
            if (service instanceof CastService) {
                imgResource = R.drawable.ic_googlecast;
                addText += "Google Cast";
            } else if (service instanceof DLNAService) {
                imgResource = R.drawable.ic_dlna;
                addText += "DLNA";
            } else if (service instanceof AirPlayService) {
                imgResource = R.drawable.ic_airplay;
                addText += "AirPlay";
            } else if (service instanceof RokuService) {
                imgResource = R.drawable.ic_dlna;
                addText += "Roku";
            } else if (service instanceof WebOSTVService) {
                imgResource = R.drawable.ic_dlna;
                addText += "webOS TV";
            } else if (service instanceof NetcastTVService) {
                imgResource = R.drawable.ic_dlna;
                addText += "Netcast";
            }

            if (!addText.isEmpty()) {
                if (serviceText.isEmpty()) {
                    serviceText = addText;
                } else {
                    serviceText += ", " + addText;
                }
            }
        }

        if (serviceText.isEmpty()) {
            serviceText = "Beaming Device";
        }

        holder.icon.setImageResource(imgResource);
        holder.text1.setText(device.getFriendlyName());
        holder.text2.setText(serviceText);

        return convertView;
    }

    DiscoveryManagerListener mListener = new DiscoveryManagerListener() {
        @Override
        public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
            mDevices = mBeamManager.getDevices();
            mKeys = new ArrayList<>(mDevices.keySet());
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
            mDevices = mBeamManager.getDevices();
            mKeys = new ArrayList<>(mDevices.keySet());
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {
            mDevices = mBeamManager.getDevices();
            mKeys = new ArrayList<>(mDevices.keySet());
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {

        }
    };
}
