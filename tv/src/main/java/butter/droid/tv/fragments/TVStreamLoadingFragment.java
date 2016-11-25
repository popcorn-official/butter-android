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

package butter.droid.tv.fragments;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.sv244.torrentstream.StreamStatus;

import java.text.DecimalFormat;

import butter.droid.base.fragments.BaseStreamLoadingFragment;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.ThreadUtils;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.TVStreamLoadingActivity;
import butter.droid.tv.activities.TVVideoPlayerActivity;
import butter.droid.tv.utils.BackgroundUpdater;
import butterknife.BindView;
import butterknife.ButterKnife;

public class TVStreamLoadingFragment extends BaseStreamLoadingFragment {

	View mRoot;
	@BindView(R.id.progressIndicator)
	ProgressBar progressIndicator;
	@BindView(R.id.primary_textview)
	TextView mPrimaryTextView;
	@BindView(R.id.secondary_textview)
	TextView mSecondaryTextView;
	@BindView(R.id.tertiary_textview)
	TextView mTertiaryTextView;

	BackgroundUpdater mBackgroundUpdater = new BackgroundUpdater();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TVButterApplication.getAppContext()
				.getComponent()
				.inject(this);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mRoot = inflater.inflate(R.layout.fragment_streamloading, container, false);
		ButterKnife.bind(this, mRoot);

		return mRoot;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mBackgroundUpdater.initialise(getActivity(), R.color.black);
		updateBackground();
	}

	private void updateStatus(final StreamStatus status) {
		final DecimalFormat df = new DecimalFormat("#############0.00");
		ThreadUtils.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				progressIndicator.setIndeterminate(false);
				progressIndicator.setProgress(status.bufferProgress);
				mPrimaryTextView.setText(status.bufferProgress + "%");

				if (status.downloadSpeed / 1024 < 1000) {
					mSecondaryTextView.setText(df.format(status.downloadSpeed / 1024) + " KB/s");
				} else {
					mSecondaryTextView.setText(df.format(status.downloadSpeed / 1048576) + " MB/s");
				}
				mTertiaryTextView.setText(status.seeds + " " + getString(R.string.seeds));
			}
		});
	}

	@Override
	protected void updateView(State state, Object extra) {

		switch (state) {
			case UNINITIALISED:
				mTertiaryTextView.setText(null);
				mPrimaryTextView.setText(null);
				mSecondaryTextView.setText(null);
				progressIndicator.setIndeterminate(true);
				progressIndicator.setProgress(0);
				break;
			case ERROR:
				if (null != extra && extra instanceof String)
					mPrimaryTextView.setText((String) extra);
				mSecondaryTextView.setText(null);
				mTertiaryTextView.setText(null);
				progressIndicator.setIndeterminate(true);
				progressIndicator.setProgress(0);
				break;
			case BUFFERING:
				mPrimaryTextView.setText(R.string.starting_buffering);
				mTertiaryTextView.setText(null);
				mSecondaryTextView.setText(null);
				progressIndicator.setIndeterminate(true);
				progressIndicator.setProgress(0);
				break;
			case STREAMING:
				mPrimaryTextView.setText(R.string.streaming_started);
				if (null != extra && extra instanceof StreamStatus)
					updateStatus((StreamStatus) extra);
				break;
			case WAITING_SUBTITLES:
				mPrimaryTextView.setText(R.string.waiting_for_subtitles);
				mTertiaryTextView.setText(null);
				mSecondaryTextView.setText(null);
				progressIndicator.setIndeterminate(true);
				progressIndicator.setProgress(0);
				break;
			case WAITING_TORRENT:
				mPrimaryTextView.setText(R.string.waiting_torrent);
				mTertiaryTextView.setText(null);
				mSecondaryTextView.setText(null);
				progressIndicator.setIndeterminate(true);
				progressIndicator.setProgress(0);
				break;

		}
	}

	@Override
	protected void startPlayerActivity(String location, int resumePosition) {
		if (getActivity() != null && !mPlayerStarted) {
			mStreamInfo.setVideoLocation(location);
			if (getActivity().getIntent().hasExtra(TVStreamLoadingActivity.EXTRA_SHOW_INFO)) {
				Show show = getActivity().getIntent().getParcelableExtra(TVStreamLoadingActivity.EXTRA_SHOW_INFO);
				TVVideoPlayerActivity.startActivity(getActivity(), mStreamInfo, show);
			}
			else {
				TVVideoPlayerActivity.startActivity(getActivity(), mStreamInfo, resumePosition);
			}
		}
	}

	protected void updateBackground() {
		StreamInfo info = mCallback.getStreamInformation();
		  /* attempt to load background image */
		if (null != info) {
			String url = info.getHeaderImageUrl();
			mBackgroundUpdater.updateBackgroundAsync(url);
		}
	}
}
