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

package butter.droid.tv.activities.base;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.fragment.app.FragmentActivity;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstreamserver.TorrentServerListener;

import butter.droid.base.ButterApplication;
import butter.droid.base.activities.TorrentActivity;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.torrent.TorrentService;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PrefUtils;
import butterknife.ButterKnife;

public abstract class TVTorrentBaseActivity
    extends FragmentActivity
    implements TorrentServerListener, TorrentActivity, ServiceConnection {

    protected Handler mHandler;
    protected TorrentService mService;

    protected void onCreate(Bundle savedInstanceState, int layoutId) {
        String language = PrefUtils.get(this, Prefs.LOCALE, ButterApplication.getSystemLanguage());
        LocaleUtils.setCurrent(this, LocaleUtils.toLocale(language));
        super.onCreate(savedInstanceState);
        setContentView(layoutId);
        ButterKnife.bind(this);
        mHandler = new Handler(getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        TorrentService.bindHere(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mService) {
            mService.removeListener(this);
            unbindService(this);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        String language = PrefUtils.get(this, Prefs.LOCALE, ButterApplication.getSystemLanguage());
        LocaleUtils.setCurrent(this, LocaleUtils.toLocale(language));
        super.setContentView(layoutResID);
    }

    protected ButterApplication getApp() {
        return (ButterApplication) getApplication();
    }

    public TorrentService getTorrentService() {
        return mService;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((TorrentService.ServiceBinder) service).getService();
        mService.addListener(this);
        mService.setCurrentActivity(this);
        onTorrentServiceConnected();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService.removeListener(this);
        mService = null;
        onTorrentServiceDisconnected();
    }

    public void onTorrentServiceConnected() {
        // Placeholder
    }

    public void onTorrentServiceDisconnected() {
        // Placeholder
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {

    }

    @Override
    public void onStreamStarted(Torrent torrent) {

    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {

    }

    @Override
    public void onStreamReady(Torrent torrent) {

    }

    @Override
    public void onServerReady(String url) {

    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus streamStatus) {

    }

    @Override
    public void onStreamStopped() {

    }
}
