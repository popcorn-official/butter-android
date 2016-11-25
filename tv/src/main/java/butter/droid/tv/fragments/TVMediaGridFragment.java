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

package butter.droid.tv.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.utils.StringUtils;
import butter.droid.base.utils.ThreadUtils;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.TVMediaDetailActivity;
import butter.droid.tv.presenters.MediaCardPresenter;
import butter.droid.tv.utils.BackgroundUpdater;
import hugo.weaving.DebugLog;
import timber.log.Timber;


/*
 * VerticalGridFragment shows a grid of videos
 */
public class TVMediaGridFragment extends VerticalGridFragment implements OnItemViewClickedListener, OnItemViewSelectedListener {

    private static final int NUM_COLUMNS = 6;

    @Inject
    ProviderManager providerManager;
    private List<MediaCardPresenter.MediaCardItem> mItems = new ArrayList<>();
    private ArrayObjectAdapter mAdapter;
    private Callback mCallback;
    private BackgroundUpdater mBackgroundUpdater;
    private int mCurrentPage = 1;

    public static TVMediaGridFragment newInstance() {
        return new TVMediaGridFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (Callback) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        super.onCreate(savedInstanceState);
        setupFragment();
   }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        setTitle(StringUtils.capWords((String) activity.getTitle()));

        if (activity instanceof Callback && mCallback == null) {
            mCallback = (Callback) getActivity();
        }

        loadItems();
    }

    private void setupFragment() {
        //setup background updater
        mBackgroundUpdater = new BackgroundUpdater();
        mBackgroundUpdater.initialise(getActivity(), R.color.black);

        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new MediaCardPresenter(getActivity()));
        setAdapter(mAdapter);

        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
    }

    private MediaProvider.Filters getFilters() {
        MediaProvider.Filters filters = new MediaProvider.Filters(mCallback.getFilters());
        filters.page = mCurrentPage;
        return filters;
    }

    private void loadItems() {

        providerManager.getCurrentMediaProvider().getList(null, getFilters(), new MediaProvider.Callback() {
            @DebugLog
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items, boolean changed) {
                mCurrentPage = filters.page;
                final List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);

                mItems.addAll(list);

                final int previousSize = mAdapter.size();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addAll(previousSize, list);
                        mAdapter.notifyArrayItemRangeChanged(previousSize, list.size());
                    }
                });
            }

            @DebugLog
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "error getting show list", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadMore() {
        mCurrentPage++;
        loadItems();
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof MediaCardPresenter.MediaCardItem) {
            onMediaItemClicked((ImageCardView) itemViewHolder.view, (MediaCardPresenter.MediaCardItem) item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundUpdater) mBackgroundUpdater.destroy();
    }

    private void onMediaItemClicked(ImageCardView view, MediaCardPresenter.MediaCardItem media) {
        if (media.isLoading()) return;
        Bundle options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(),
                view.getMainImageView(),
                TVMediaDetailActivity.SHARED_ELEMENT_NAME).toBundle();
        if (media.getMedia() instanceof Movie)
            TVMediaDetailActivity.startActivity(getActivity(), options, media.getMedia());
        else if (media.getMedia() instanceof Show)
            TVMediaDetailActivity.startActivity(getActivity(), options, media.getMedia());
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof MediaCardPresenter.MediaCardItem) {
            MediaCardPresenter.MediaCardItem overviewItem = (MediaCardPresenter.MediaCardItem) item;
            if (overviewItem.isLoading()) return;

            mBackgroundUpdater.updateBackgroundAsync(((MediaCardPresenter.MediaCardItem) item).getMedia().headerImage);
        }

        //really hacky way of making and 'endless' adapter

        //trigger items to update
        int itemPosition = mItems.indexOf(item);

        //when we are within 3 rows of the end, load more items
        if (itemPosition>getAdapter().size()-(NUM_COLUMNS*3)){
            Timber.d("Loading more items: page "+mCurrentPage);
            loadMore();
        }
    }

    public interface Callback {
        MediaProvider.Filters getFilters();
    }
}
