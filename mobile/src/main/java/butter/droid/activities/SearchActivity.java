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

package butter.droid.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.fragments.MediaListFragment;
import butter.droid.utils.ToolbarUtils;
import butterknife.BindView;


/**
 * This activity handles searching a provider.
 * <p/>
 * It must be started with a provider id, which then gets forwarded to the overview fragment
 */
public class SearchActivity extends ButterBaseActivity {

    public static final String EXTRA_PROVIDER = "extra_provider";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.searchview)
    SearchView mSearchview;

    private MediaListFragment mFragment;

    public static Intent startActivity(Activity activity) {
        Intent intent = new Intent(activity, SearchActivity.class);
        activity.startActivity(intent);
//		activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out,);
        return intent;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_search);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setShowCasting(true);

        ToolbarUtils.updateToolbarHeight(this, toolbar);

        mSearchview.onActionViewExpanded();
        mSearchview.setOnQueryTextListener(mSearchListener);

        //dont re add the fragment if it exists
        if (null != savedInstanceState) {
            mFragment = (MediaListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
            return;
        }

        //create and add the media fragment
        mFragment = MediaListFragment.newInstance(MediaListFragment.Mode.SEARCH, MediaProvider.Filters.Sort.POPULARITY,
                MediaProvider.Filters.Order.DESC);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, mFragment).commit();
    }

    private SearchView.OnQueryTextListener mSearchListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String s) {
            if (null == mFragment) return false;//fragment not added yet.
            mFragment.triggerSearch(s);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (s.equals("")) {
                onQueryTextSubmit(s);
            }
            return false;
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onHomePressed();
                //to fade out the activity
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
