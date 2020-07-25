package butter.droid.tv.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import java.util.List;

import butter.droid.base.torrent.StreamInfo;
import butter.droid.tv.R;
import butter.droid.tv.activities.TVStreamLoadingActivity;

public class TVWatchOrDownloadGuidedStepFragment extends GuidedStepSupportFragment {
    private static final long WATCH_NOW = 100L;
    private static final long DOWNLOAD = 102L;
    private StreamInfo mStreamInfo;

    public TVWatchOrDownloadGuidedStepFragment(StreamInfo streamInfo) {
        mStreamInfo = streamInfo;
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.download_or_stream_title);
        String description = getString(R.string.download_or_stream_description);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_av_play);
        return new GuidanceStylist.Guidance(title, description, null, icon);
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        // Add "watch now" user action for this step
        actions.add(new GuidedAction.Builder()
                .id(WATCH_NOW)
                .title(getString(R.string.stream))
                .build());

        // Add "Download" user action for this step
        actions.add(new GuidedAction.Builder()
                .id(DOWNLOAD)
                .title(getString(R.string.download))
                .build());
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == WATCH_NOW) {
            TVStreamLoadingActivity.startActivity(getActivity(), mStreamInfo);
        } else if (action.getId() == DOWNLOAD) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mStreamInfo.getTorrentUrl()));
            startActivity(intent);
        }
    }
}
