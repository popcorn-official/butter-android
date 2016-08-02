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
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butter.droid.R;
import butter.droid.activities.PreferencesActivity;
import butter.droid.adapters.NavigationAdapter;
import butter.droid.adapters.decorators.OneShotDividerDecorator;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.providers.media.AnimeProvider;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.MoviesProvider;
import butter.droid.base.providers.media.TVProvider;
import butter.droid.base.providers.media.YtsMoviesProvider;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.vpn.VPNHTChecker;
import butter.droid.base.vpn.VPNManager;
import butter.droid.fragments.dialog.VPNInfoDialogFragment;



public class NavigationDrawerFragment extends Fragment implements NavigationAdapter.Callback {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";


    // Central VPN menu item
    private NavDrawerItem mVPNItem;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * views
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    RecyclerView mRecyclerView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ViewGroup mNavigationDrawerContainer;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * variables
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    private int mCurrentSelectedPosition = 0;
    private boolean mFromSavedInstanceState;
    private boolean mUserLearnedDrawer;
    private NavigationAdapter mAdapter;

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private Callbacks mCallbacks;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * life cycle methods
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (Callbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserLearnedDrawer = PrefUtils.get(getActivity(), Prefs.DRAWER_LEARNED, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            mFromSavedInstanceState = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRecyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return mRecyclerView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new NavigationAdapter(getActivity(), this, initItems());
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.addItemDecoration(new OneShotDividerDecorator(getActivity(), mAdapter.getItemCount() - 2));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.requestFocus();
    }

    public List<NavDrawerItem> initItems() {
        //todo: make list items dynamic
        List<NavDrawerItem> navItems = new ArrayList<>();
        navItems.add(new NavDrawerItem(true));
        navItems.add(new NavDrawerItem(getString(R.string.title_movies), R.drawable.ic_nav_movies, new YtsMoviesProvider()));
        navItems.add(new NavDrawerItem(getString(R.string.title_shows), R.drawable.ic_nav_tv, new TVProvider()));
        navItems.add(new NavDrawerItem(getString(R.string.title_anime), R.drawable.ic_nav_anime, new AnimeProvider()));
        navItems.add(new NavDrawerItem(getString(R.string.preferences), R.drawable.ic_nav_settings, mOnSettingsClickListener));
        if(PrefUtils.get(getActivity(), Prefs.SHOW_VPN, true) && VPNHTChecker.isDownloadAvailable(getActivity())) {
            navItems.add(mVPNItem = new NavDrawerItem(getString(R.string.vpn), R.drawable.ic_nav_vpn, mOnVPNClickListener, VPNManager.getLatestInstance().isConnected()));
        }
        if(mAdapter != null)
            mAdapter.setItems(navItems);
        VPNManager.State state = VPNManager.getCurrentState();
        NavigationDrawerFragment.NavDrawerItem vpnItem = getVPNItem();
        if(vpnItem != null) {
            if (state.equals(VPNManager.State.DISCONNECTED)) {
                vpnItem.setSwitchValue(false);
                vpnItem.showProgress(false);
            } else if(state.equals(VPNManager.State.CONNECTING)) {
                vpnItem.showProgress(true);
            } else if(state.equals(VPNManager.State.CONNECTED)) {
                vpnItem.setSwitchValue(true);
                vpnItem.showProgress(false);
            }
        }

        return navItems;
    }

    private NavDrawerItem.OnClickListener mOnSettingsClickListener = new NavDrawerItem.OnClickListener() {
        @Override
        public void onClick(View v, NavigationAdapter.ItemRowHolder rowHolder, int position) {
            PreferencesActivity.startActivity(getActivity());
            mDrawerLayout.closeDrawer(mNavigationDrawerContainer);
        }
    };

    private NavDrawerItem.OnClickListener mOnVPNClickListener = new NavDrawerItem.OnClickListener() {
        @Override
        public void onClick(View v, NavigationAdapter.ItemRowHolder vh, int position) {
            if(vh.getSwitch() != null) {
                VPNManager manager = VPNManager.getLatestInstance();
                if(manager.isVPNInstalled()) {
                    if (!manager.isConnected()) {
                        manager.connect();
                        vh.getSwitch().setChecked(true);
                    } else {
                        manager.disconnect();
                        vh.getSwitch().setChecked(false);
                    }
                } else {
                    VPNInfoDialogFragment.show(getChildFragmentManager());
                }
            }
        }
    };
    private NavigationAdapter.OnItemClickListener mOnItemClickListener = new NavigationAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View v, NavigationAdapter.ItemRowHolder vh, NavDrawerItem item, int position) {
            if (null != item.getOnClickListener()) {
                item.onClick(v, vh, position);
                return;
            }

            selectItem(mAdapter.getCorrectPosition(position));
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public int getSelectedPosition() {
        return mCurrentSelectedPosition;
    }

    public NavDrawerItem getCurrentItem() {
        return mAdapter.getItem(getSelectedPosition() + 1);
    }

    public NavDrawerItem getVPNItem() {
        return mVPNItem;
    }
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * initialise 
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    /**
     * Called by the implementing activity to initialise the navigation drawer
     *
     * @param navigationContainer
     * @param drawerLayout
     */
    public void initialise(ViewGroup navigationContainer, DrawerLayout drawerLayout) {
        mNavigationDrawerContainer = navigationContainer;
        mDrawerLayout = drawerLayout;

        //ensure the mToolbar displays the home icon (will be overriden with the burger icon)
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) return;

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    PrefUtils.save(getActivity(), Prefs.DRAWER_LEARNED, true);
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mNavigationDrawerContainer);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);

    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //consume the home button press
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface Callbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(NavDrawerItem item, String s);
    }

    /**
     * Called when a list item is selected.
     * <p/>
     * Updates the state of the list, closes the drawer, and fowards the event to the parent activity to handle.
     *
     * @param position position of the item in the list
     */
    public void selectItem(int position) {
        mCurrentSelectedPosition = position;

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mNavigationDrawerContainer);
        }

        if (mCallbacks != null) {
            NavDrawerItem navDrawerItem = mAdapter.getItem(position + 1);
            mCallbacks.onNavigationDrawerItemSelected(navDrawerItem, null != navDrawerItem ? navDrawerItem.getTitle() : null);
        }

        mAdapter.notifyDataSetChanged();
    }

    /**
     * Describes an item to be displayed in the navigation list
     */
    public static class NavDrawerItem {
        private NavDrawerItem.OnClickListener mOnClickListener;
        private boolean mIsHeader = false, mIsSwitch = false, mSwitchValue = false, mShowProgress = false;
        private String mTitle;
        private int mIcon;
        private MediaProvider mMediaProvider;
        private NavigationAdapter.ItemRowHolder mRowHolder;

        public NavDrawerItem(String title, int icon) {
            mTitle = title;
            mIcon = icon;
        }

        public NavDrawerItem(String title, int icon, MediaProvider mediaProvider) {
            this(title, icon);
            mMediaProvider = mediaProvider;
        }

        public NavDrawerItem(String title, int icon, OnClickListener listener) {
            this(title, icon);
            mOnClickListener = listener;
        }

        public NavDrawerItem(String title, int icon, OnClickListener listener, boolean isSwitch) {
            this(title, icon);
            mOnClickListener = listener;
            mIsSwitch = true;
            mSwitchValue = isSwitch;
        }

        public NavDrawerItem(boolean isHeader) {
            mIsHeader = true;
        }

        public void setRowHolder(NavigationAdapter.ItemRowHolder rowHolder) {
            mRowHolder = rowHolder;
        }

        public String getTitle() {
            return mTitle;
        }

        public int getIcon() {
            return mIcon;
        }

        public MediaProvider getMediaProvider() {
            return mMediaProvider;
        }

        public boolean isHeader() {
            return mIsHeader;
        }

        public boolean isSwitch() {
            return mIsSwitch;
        }

        public boolean getSwitchValue() {
            return mSwitchValue;
        }

        public boolean hasProvider() {
            return mMediaProvider != null;
        }

        public OnClickListener getOnClickListener() {
            return mOnClickListener;
        }

        public void onClick(View v, NavigationAdapter.ItemRowHolder itemRowHolder, int position) {
            mOnClickListener.onClick(v, itemRowHolder, position);
        }

        public interface OnClickListener {
            void onClick(View v, NavigationAdapter.ItemRowHolder rowHolder, int position);
        }

        public void showProgress(boolean b) {
            mShowProgress = b;
            if(mRowHolder != null) {
                mRowHolder.getProgressBar().setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                if(mIsSwitch) {
                    mRowHolder.getSwitch().setVisibility(b ? View.INVISIBLE : View.VISIBLE);
                }
            }
        }

        public boolean showProgress() {
            return mShowProgress;
        }

        public void setSwitchValue(boolean b) {
            if(mRowHolder != null)
                mRowHolder.getSwitch().setChecked(b);
        }
    }

}
