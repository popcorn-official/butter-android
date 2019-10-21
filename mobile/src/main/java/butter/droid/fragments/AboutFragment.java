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

package butter.droid.fragments;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butter.droid.R;
import butter.droid.base.Constants;
import butter.droid.base.utils.IntentUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutFragment extends Fragment {

    @BindView(R.id.logo_imageview)
    ImageView mLogoImageView;
    @BindView(R.id.facebook_button)
    TextView mFacebookButton;
    @BindView(R.id.git_button)
    TextView mGitButton;
    @BindView(R.id.blog_button)
    TextView mBlogButton;
    @BindView(R.id.butter_button)
    TextView mButterButton;
    @BindView(R.id.discuss_button)
    TextView mDiscussButton;
    @BindView(R.id.twitter_button)
    TextView mTwitterButton;

    private OnFragmentInteractionListener mListener;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        //		public void onFragmentInteraction(Uri uri);
    }


    @OnClick(R.id.logo_imageview)
    void onLogoClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.BUTTER_URL));
    }

    @OnClick(R.id.facebook_button)
    void onFacebookClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.FB_URL));
    }

    @OnClick(R.id.git_button)
    void onGitClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.GIT_URL));
    }

    @OnClick(R.id.blog_button)
    void onBlogClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.BLOG_URL));
    }

    @OnClick(R.id.butter_button)
    void onButterClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.BUTTER_URL));
    }

    @OnClick(R.id.discuss_button)
    void onDiscussClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.DISCUSS_URL));
    }

    @OnClick(R.id.twitter_button)
    void onTwitterClick() {
        startActivity(IntentUtils.getBrowserIntent(getActivity(), Constants.TWITTER_URL));
    }

}
