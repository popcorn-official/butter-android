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

package butter.droid.tv.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.base.TVBaseActivity;
import butter.droid.tv.fragments.TVMediaGridFragment;

public class TVMediaGridActivity extends TVBaseActivity implements TVMediaGridFragment.Callback {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SORT = "extra_sort";
    public static final String EXTRA_ORDER = "extra_order";
    public static final String EXTRA_GENRE = "extra_genre";

    private final MediaProvider.Filters mFilter = new MediaProvider.Filters();
    private MediaProvider.Filters.Order mDefOrder;
    private MediaProvider.Filters.Sort mSort;
    private String mGenre;

    public static Intent startActivity(Activity activity, String title, MediaProvider.Filters.Sort sort, MediaProvider.Filters.Order defOrder, String genre) {
        Intent intent = new Intent(activity, TVMediaGridActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_GENRE, genre);
        intent.putExtra(EXTRA_ORDER, defOrder);
        intent.putExtra(EXTRA_SORT, sort);
        activity.startActivity(intent);
        return intent;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        super.onCreate(savedInstanceState, R.layout.activity_movie_media_grid);

        mSort = (MediaProvider.Filters.Sort) getIntent().getExtras().getSerializable(EXTRA_SORT);
        mDefOrder = (MediaProvider.Filters.Order) getIntent().getExtras().getSerializable(EXTRA_ORDER);
        mGenre = getIntent().getExtras().getString(EXTRA_GENRE);
        String title = getIntent().getExtras().getString(EXTRA_TITLE);
        setTitle(title);

        mFilter.sort = mSort;
        mFilter.order = mDefOrder;
        mFilter.genre = mGenre;

        //add media fragment
        getFragmentManager().beginTransaction().replace(R.id.fragment, TVMediaGridFragment.newInstance()).commit();
    }

    @Override
    public MediaProvider.Filters getFilters() {
        return mFilter;
    }
}
