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

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.github.sv244.torrentstream.Torrent;

import javax.inject.Inject;

import butter.droid.base.manager.updater.ButterUpdateManager;
import butter.droid.base.utils.VersionUtils;
import butter.droid.tv.activities.TVSearchActivity;

public abstract class TVBaseActivity extends TVTorrentBaseActivity {

	@Inject
	ButterUpdateManager updateManager;

	@Override
	protected void onCreate(Bundle savedInstanceState, int layoutId) {
		super.onCreate(savedInstanceState, layoutId);

		if(!VersionUtils.isUsingCorrectBuild()) {
			new AlertDialog.Builder(this)
					.setMessage(butter.droid.base.R.string.wrong_abi)
					.setCancelable(false)
					.show();

			updateManager.setListener(new ButterUpdateManager.Listener() {
				@Override
				public void updateAvailable(String updateFile) {
					Intent installIntent = new Intent(Intent.ACTION_VIEW);
					installIntent.setDataAndType(Uri.parse("file://" + updateFile), ButterUpdateManager.ANDROID_PACKAGE);
					installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(installIntent);
				}
			});
			updateManager.checkUpdatesManually();
		}
	}

	@Override
	public boolean onSearchRequested() {
		TVSearchActivity.startActivity(this);
		return true;
	}

	@Override
	public void onStreamPrepared(Torrent torrent) {
		super.onStreamPrepared(torrent);

		// todo?
	}

}