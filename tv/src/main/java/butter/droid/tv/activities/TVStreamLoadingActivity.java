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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import android.view.View;

import butter.droid.base.fragments.BaseStreamLoadingFragment;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.base.TVBaseActivity;

public class TVStreamLoadingActivity extends TVBaseActivity implements BaseStreamLoadingFragment.FragmentListener {

	public final static String EXTRA_STREAM_INFO = "stream_info";
	public final static String EXTRA_SHOW_INFO = "show_info";

	private StreamInfo mInfo;
	private BaseStreamLoadingFragment mFragment;

	public static Intent startActivity(Activity activity, StreamInfo info) {
		Intent i = new Intent(activity, TVStreamLoadingActivity.class);
		i.putExtra(EXTRA_STREAM_INFO, info);
		activity.startActivity(i);
		return i;
	}

	public static Intent startActivity(Activity activity, StreamInfo info, Show show) {
		Intent i = new Intent(activity, TVStreamLoadingActivity.class);
		i.putExtra(EXTRA_STREAM_INFO, info);
		i.putExtra(EXTRA_SHOW_INFO, show);
		activity.startActivity(i);
		return i;
	}

	public static Intent startActivity(Activity activity, StreamInfo info, Pair<View, String>... elements) {
		Intent i = new Intent(activity, TVStreamLoadingActivity.class);
		i.putExtra(EXTRA_STREAM_INFO, info);

		ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
			activity, elements);
		ActivityCompat.startActivity(activity, i, options.toBundle());
		return i;
	}

	@SuppressLint("MissingSuperCall")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		TVButterApplication.getAppContext()
				.getComponent()
				.inject(this);

		super.onCreate(savedInstanceState, R.layout.activity_streamloading);

		if (!getIntent().hasExtra(EXTRA_STREAM_INFO)) finish();
		mInfo = getIntent().getParcelableExtra(EXTRA_STREAM_INFO);
		mFragment = (BaseStreamLoadingFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
	}

    @Override
	public StreamInfo getStreamInformation() {
		return mInfo;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mFragment.cancelStream();
	}

	@Override
    public void onTorrentServiceDisconnected() {
		if (null != mFragment) {
			mFragment.onTorrentServiceDisconnected();
		}
	}

	@Override
	public void onTorrentServiceConnected() {
		if (null != mFragment) {
			mFragment.onTorrentServiceConnected();
		}
	}
}