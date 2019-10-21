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

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.List;

import butter.droid.R;
import butter.droid.base.ButterApplication;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.fragments.MediaGenreSelectionFragment;
import butter.droid.fragments.MediaListFragment;

public class MediaPagerAdapter extends FragmentStatePagerAdapter {

    private final List<MediaProvider.NavInfo> mTabs;
    private FragmentManager mFragmentManager;
    private MediaProvider mProvider;
    private String mGenre;
    private int mHasGenreTabs = 0;
    private Fragment mGenreFragment;
    private MediaGenreSelectionFragment.Listener mMediaGenreSelectionFragment = new MediaGenreSelectionFragment.Listener() {
        @Override
        public void onGenreSelected(String genre) {
            mGenre = genre;
            mProvider.cancel();
            for (Fragment frag : mFragmentManager.getFragments()) {
                if (frag instanceof MediaListFragment) {
                    ((MediaListFragment) frag).changeGenre(genre);
                }
            }
        }
    };

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
        return super.instantiateItem(container, position);
    }

}