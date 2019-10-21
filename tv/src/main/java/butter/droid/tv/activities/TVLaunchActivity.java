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

package butter.droid.tv.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.service.RecommendationService;

public class TVLaunchActivity extends Activity {

	private static final int PERMISSIONS_REQUEST = 1232;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent recommendationIntent = new Intent(this, RecommendationService.class);
		startService(recommendationIntent);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
			return;
		}

		proceedCreate();
	}

	private void proceedCreate() {
		Boolean firstRun = PrefUtils.get(this, Prefs.FIRST_RUN, true);

		if (firstRun) {
			//run the welcome wizard
			TVWelcomeActivity.startActivity(this);
		} else {
			String action = getIntent().getAction();
			Uri data = getIntent().getData();
			if (action != null && action.equals(Intent.ACTION_VIEW) && data != null) {
				String streamUrl = data.toString();
				try {
					streamUrl = URLDecoder.decode(streamUrl, "utf-8");
					TVStreamLoadingActivity.startActivity(this, new StreamInfo(streamUrl));
					finish();
					return;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			TVMainActivity.startActivity(this);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST: {
				if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					proceedCreate();
				} else {
					finish();
				}
			}
		}
	}

}
