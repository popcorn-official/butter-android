package butter.droid.fragments.base;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;

import butter.droid.activities.MediaDetailActivity;
import butter.droid.base.torrent.StreamInfo;

public abstract class BaseDetailFragment extends Fragment {

    protected FragmentListener mCallback;
    protected MediaDetailActivity mActivity;
    protected View mRoot;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MediaDetailActivity)
            mActivity = (MediaDetailActivity) context;
    }

    public interface FragmentListener {
        void playStream(StreamInfo streamInfo);
    }

}
