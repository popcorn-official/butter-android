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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.PreferencesActivity;
import butter.droid.adapters.NavigationAdapter;
import butter.droid.adapters.decorators.OneShotDividerDecorator;
import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.ProviderUtils;
import butter.droid.base.vpn.VPNHTChecker;
import butter.droid.base.vpn.VPNManager;
import butter.droid.fragments.dialog.VPNInfoDialogFragment;


public class NavigationDrawerFragment extends Fragment implements NavigationAdapter.Callback, NavigationAdapter.OnItemClickListener {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";


    @Inject
    ProviderManager providerManager;

    @Inject
    VPNHTChecker vpnhtChecker;

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
    // Central VPN menu item
    private AbsNavDrawerItem.VPNNavDrawerItem mVPNItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

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
        mAdapter.setOnItemClickListener(this);

        mRecyclerView.addItemDecoration(new OneShotDividerDecorator(getActivity(), mAdapter.getItemCount() - 2));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.requestFocus();
    }

    public List<AbsNavDrawerItem> initItems() {

        List<AbsNavDrawerItem> navItems = new ArrayList<>();
        navItems.add(new AbsNavDrawerItem.HeaderNavDrawerItem());
        navItems.add(new AbsNavDrawerItem.ProviderNavDrawerItem(ProviderUtils.getProviderTitle(ProviderManager.PROVIDER_TYPE_MOVIE),
                ProviderUtils.getProviderIcon(ProviderManager.PROVIDER_TYPE_MOVIE),
                ProviderManager.PROVIDER_TYPE_MOVIE));
        navItems.add(new AbsNavDrawerItem.ProviderNavDrawerItem(ProviderUtils.getProviderTitle(ProviderManager.PROVIDER_TYPE_SHOW),
                ProviderUtils.getProviderIcon(ProviderManager.PROVIDER_TYPE_SHOW),
                ProviderManager.PROVIDER_TYPE_SHOW));
        navItems.add(new AbsNavDrawerItem.ProviderNavDrawerItem(ProviderUtils.getProviderTitle(ProviderManager.PROVIDER_TYPE_ANIME),
                ProviderUtils.getProviderIcon(ProviderManager.PROVIDER_TYPE_ANIME),
                ProviderManager.PROVIDER_TYPE_ANIME));
        navItems.add(new AbsNavDrawerItem.ScreenNavDrawerItem(R.string.preferences, R.drawable.ic_nav_settings,
                PreferencesActivity.getIntent(getContext())));
        if (PrefUtils.get(getActivity(), Prefs.SHOW_VPN, true) && vpnhtChecker.isDownloadAvailable()) {
            navItems.add(mVPNItem = new AbsNavDrawerItem.VPNNavDrawerItem(R.string.vpn, R.drawable.ic_nav_vpn, VPNManager.getLatestInstance().isConnected()));
        }

        if (mAdapter != null) {
            mAdapter.setItems(navItems);
        }

        VPNManager.State state = VPNManager.getCurrentState();
        AbsNavDrawerItem.VPNNavDrawerItem vpnItem = getVPNItem();
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


    public AbsNavDrawerItem.VPNNavDrawerItem getVPNItem() {
        return mVPNItem;
    }
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * initialise 
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    /**
     * Called by the implementing activity to initialise the navigation drawer
     *
     * @param navigationContainer The navigation container
     * @param drawerLayout The drawer layout
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

        mDrawerLayout.addDrawerListener(mDrawerToggle);

    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //consume the home button press
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

    }

    @Override
    public void onItemClick(View v, NavigationAdapter.ItemRowHolder vh, AbsNavDrawerItem item, int position) {
        switch (item.getType()) {
            case AbsNavDrawerItem.TYPE_HEADER:
                throw new IllegalStateException("Header item can not be clickable.");
            case AbsNavDrawerItem.TYPE_PROVIDER:
                providerManager.setCurrentProviderType(((AbsNavDrawerItem.ProviderNavDrawerItem) item).getProviderType());
                selectItem(mAdapter.getCorrectPosition(position));
                break;
            case AbsNavDrawerItem.TYPE_SCREEN:
                getActivity().startActivity(((AbsNavDrawerItem.ScreenNavDrawerItem) item).getIntent());
                mDrawerLayout.closeDrawer(mNavigationDrawerContainer);
                break;
            case AbsNavDrawerItem.TYPE_VPN:
                if (vh.getSwitch() != null) {
                    VPNManager manager = VPNManager.getLatestInstance();
                    if (manager.isVPNInstalled()) {
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
                break;
        }
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

        mAdapter.notifyDataSetChanged();
    }

    public abstract static class AbsNavDrawerItem {


        public static final int TYPE_HEADER = 0;
        static final int TYPE_PROVIDER = 1;
        static final int TYPE_SCREEN = 2;
        static final int TYPE_VPN = 3;
        @StringRes
        private final int title;
        @DrawableRes
        private final int icon;

        AbsNavDrawerItem(@StringRes int title, @DrawableRes int icon) {
            this.title = title;
            this.icon = icon;
        }

        public int getTitle() {
            return title;
        }

        public int getIcon() {
            return icon;
        }

        @NavType
        public abstract int getType();

        @IntDef({TYPE_HEADER, TYPE_PROVIDER, TYPE_SCREEN, TYPE_VPN})
        @Retention(RetentionPolicy.SOURCE)
        @interface NavType {
        }

        public static class VPNNavDrawerItem extends AbsNavDrawerItem {

            private boolean mIsSwitch = false, mSwitchValue = false, mShowProgress = false;
            private NavigationAdapter.ItemRowHolder mRowHolder;

            public VPNNavDrawerItem(@StringRes int title, @DrawableRes int icon, boolean isSwitch) {
                super(title, icon);
                mIsSwitch = true;
                mSwitchValue = isSwitch;
            }

            @Override
            public int getType() {
                return TYPE_VPN;
            }

            public void setRowHolder(NavigationAdapter.ItemRowHolder rowHolder) {
                mRowHolder = rowHolder;
            }

            public boolean isSwitch() {
                return mIsSwitch;
            }

            public boolean getSwitchValue() {
                return mSwitchValue;
            }

            public void setSwitchValue(boolean b) {
                if (mRowHolder != null) {
                    mRowHolder.getSwitch().setChecked(b);
                }
            }

            public void showProgress(boolean b) {
                mShowProgress = b;
                if (mRowHolder != null) {
                    mRowHolder.getProgressBar().setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                    if (mIsSwitch) {
                        mRowHolder.getSwitch().setVisibility(b ? View.INVISIBLE : View.VISIBLE);
                    }
                }
            }

            public boolean showProgress() {
                return mShowProgress;
            }
        }

        public static class ScreenNavDrawerItem extends AbsNavDrawerItem {

            @NonNull
            private final Intent intent;

            public ScreenNavDrawerItem(@StringRes int title, @DrawableRes int icon, @NonNull Intent intent) {
                super(title, icon);
                this.intent = intent;
            }

            @Override
            public int getType() {
                return TYPE_SCREEN;
            }

            @NonNull
            public Intent getIntent() {
                return intent;
            }
        }

        public static class HeaderNavDrawerItem extends AbsNavDrawerItem {
            public HeaderNavDrawerItem() {
                super(0, 0);
            }

            @Override
            public int getType() {
                return TYPE_HEADER;
            }
        }

        public static class ProviderNavDrawerItem extends AbsNavDrawerItem {

            @ProviderManager.ProviderType
            private final int providerType;

            public ProviderNavDrawerItem(@StringRes int title, @DrawableRes int icon, @ProviderManager.ProviderType int providerType) {
                super(title, icon);
                this.providerType = providerType;
            }

            @ProviderManager.ProviderType
            public int getProviderType() {
                return providerType;
            }

            @Override
            public int getType() {
                return TYPE_PROVIDER;
            }

        }

    }

}
