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

package butter.droid.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butter.droid.R;
import butter.droid.base.ButterApplication;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.fragments.MediaGenreSelectionFragment;
import butter.droid.fragments.MediaListFragment;

public class MediaPagerAdapter extends FragmentPagerAdapter {

    private FragmentManager mFragmentManager;
    private Map<Integer, String> mFragTags = new HashMap<>();
    private final List<MediaProvider.NavInfo> mTabs;
    private MediaProvider mProvider;
    private String mGenre;
    private int mHasGenreTabs = 0;
    private Fragment mGenreFragment;

    public MediaPagerAdapter(MediaProvider provider, FragmentManager fm, List<MediaProvider.NavInfo> tabs) {
        super(fm);
        mFragmentManager = fm;
        mTabs = tabs;
        mProvider = provider;
        mHasGenreTabs = (mProvider.getGenres() != null && mProvider.getGenres().size() > 0 ? 1 : 0);
    }

    @Override
    public int getCount() {
        return mTabs.size() + mHasGenreTabs;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (mHasGenreTabs > 0 && position == 0) {
            return ButterApplication.getAppContext().getString(R.string.genres).toUpperCase(LocaleUtils.getCurrent());
        }
        position -= mHasGenreTabs;
        return mTabs.get(position).getLabel().toUpperCase(LocaleUtils.getCurrent());
    }

    @Override
    public Fragment getItem(int position) {
        if (mHasGenreTabs > 0 && position == 0) {
            if (mGenreFragment == null) {
                mGenreFragment = MediaGenreSelectionFragment.newInstance(mMediaGenreSelectionFragment);
            }
            return mGenreFragment;
        }

        position -= mHasGenreTabs;
        return MediaListFragment.newInstance(MediaListFragment.Mode.NORMAL, mTabs.get(position).getFilter(), mTabs.get(position).getOrder(), mGenre);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object obj = super.instantiateItem(container, position);
        if (obj instanceof Fragment) {
            Fragment f = (Fragment) obj;
            String tag = f.getTag();
            mFragTags.put(position, tag);
        }

        if (obj instanceof MediaGenreSelectionFragment && mGenreFragment != null) {
            return mGenreFragment;
        }

        return obj;
    }

    public MediaListFragment getMediaListFragment(int position) {
        if (mFragTags.size() > position) {
            String tag = mFragTags.get(position);
            if (tag != null) {
                Fragment frag = mFragmentManager.findFragmentByTag(tag);
                if (frag instanceof MediaListFragment)
                    return (MediaListFragment) frag;
            }
        }
        return null;
    }

    private MediaGenreSelectionFragment.Listener mMediaGenreSelectionFragment = new MediaGenreSelectionFragment.Listener() {
        @Override
        public void onGenreSelected(String genre) {
            mGenre = genre;
            mProvider.cancel();
            for (int i = 0; i < getCount(); i++) {
                MediaListFragment mediaListFragment = getMediaListFragment(i);
                if (mediaListFragment != null)
                    mediaListFragment.changeGenre(genre);
            }
        }
    };

}