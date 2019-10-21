package butter.droid.adapters;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import butter.droid.R;
import butter.droid.fragments.ShowDetailAboutFragment;
import butter.droid.fragments.ShowDetailSeasonFragment;

public class ShowDetailPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> mFragments = new ArrayList<>();
    private Context mContext;
    private Boolean mHasAbout = false;

    public ShowDetailPagerAdapter(Context context, FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        mFragments = fragments;
        mContext = context;

        if (mFragments.size() > 0)
            mHasAbout = mFragments.get(0) instanceof ShowDetailAboutFragment;
    }

    public void setFragments(List<Fragment> fragments) {
        mFragments = fragments;

        if (mFragments.size() > 0)
            mHasAbout = mFragments.get(0) instanceof ShowDetailAboutFragment;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mFragments.get(position) instanceof ShowDetailSeasonFragment) {
            int seasonNumber = ((ShowDetailSeasonFragment) mFragments.get(position)).getSeasonNumber();
            if (seasonNumber == 0)
                return mContext.getString(R.string.specials);
            return mContext.getString(R.string.season) + " " + seasonNumber;
        }
        return mContext.getString(R.string.about_series);
    }

}
