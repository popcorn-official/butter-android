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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.leanback.app.BrowseFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.ThreadUtils;
import butter.droid.tv.BuildConfig;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.TVMediaDetailActivity;
import butter.droid.tv.activities.TVMediaGridActivity;
import butter.droid.tv.activities.TVPreferencesActivity;
import butter.droid.tv.activities.TVSearchActivity;
import butter.droid.tv.activities.TVVideoPlayerActivity;
import butter.droid.tv.presenters.MediaCardPresenter;
import butter.droid.tv.presenters.MorePresenter;
import butter.droid.tv.utils.BackgroundUpdater;
import hugo.weaving.DebugLog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class TVOverviewFragment extends BrowseFragment implements OnItemViewClickedListener, OnItemViewSelectedListener {

    @Inject
    ProviderManager providerManager;

    private Integer mSelectedRow = 0;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mShowAdapter;
    private ArrayObjectAdapter mMoviesAdapter;


    private BackgroundUpdater mBackgroundUpdater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //setup background updater
        mBackgroundUpdater = new BackgroundUpdater();
        mBackgroundUpdater.initialise(getActivity(), R.color.black);

        //setup main adapter
        ListRowPresenter mainMenuRowPresenter = new ListRowPresenter();
        mainMenuRowPresenter.setShadowEnabled(false);
        mRowsAdapter = new ArrayObjectAdapter(mainMenuRowPresenter);
        setAdapter(mRowsAdapter);

        setupUIElements();
        setupEventListeners();
        setupAdapters();
        loadData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundUpdater) mBackgroundUpdater.destroy();
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof MediaCardPresenter.MediaCardItem) {
            onMediaItemClicked((MediaCardPresenter.CustomImageCardView) itemViewHolder.view, (MediaCardPresenter.MediaCardItem) item);
        } else if (item instanceof MorePresenter.MoreItem) {
            onMoreItemClicked((MorePresenter.MoreItem) item);
        }
    }

    @Override
    public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item, RowPresenter.ViewHolder rowViewHolder, Row row) {
        mSelectedRow = mRowsAdapter.indexOf(row);

        if (item instanceof MediaCardPresenter.MediaCardItem) {
            MediaCardPresenter.MediaCardItem overviewItem = (MediaCardPresenter.MediaCardItem) item;
            if (overviewItem.isLoading()) return;
            mBackgroundUpdater.updateBackgroundAsync(((MediaCardPresenter.MediaCardItem) item).getMedia().headerImage);
        }
    }

    private void setupUIElements() {
        setBadgeDrawable(ActivityCompat.getDrawable(getActivity(), R.drawable.header_logo));
        setTitle(getString(R.string.app_name)); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        // set fastLane (or headers) background color
        setBrandColor(ActivityCompat.getColor(getActivity(), R.color.primary));
        // set search icon color
        setSearchAffordanceColor(ActivityCompat.getColor(getActivity(), R.color.primary_dark));
    }

    private void loadData() {
        final MediaProvider.Filters showsFilter = new MediaProvider.Filters();
        showsFilter.setSort(MediaProvider.Filters.Sort.YEAR);
        showsFilter.setOrder(MediaProvider.Filters.Order.DESC);

        providerManager.getMediaProvider(ProviderManager.PROVIDER_TYPE_SHOW)
                .getList(null, showsFilter, new MediaProvider.Callback() {
            @DebugLog
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items) {
                final List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mShowAdapter.clear();
                        mShowAdapter.addAll(0, list);
                    }
                });

                if(mSelectedRow == 1)
                    mBackgroundUpdater.updateBackgroundAsync(items.get(0).headerImage);
            }

            @DebugLog
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), R.string.encountered_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        final MediaProvider.Filters movieFilters = new MediaProvider.Filters();
        movieFilters.setSort(MediaProvider.Filters.Sort.TRENDING);
        movieFilters.setOrder(MediaProvider.Filters.Order.DESC);

        providerManager.getMediaProvider(ProviderManager.PROVIDER_TYPE_MOVIE)
                .getList(null, movieFilters, new MediaProvider.Callback() {
            @DebugLog
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items) {
                final List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMoviesAdapter.clear();
                        mMoviesAdapter.addAll(0, list);
                    }
                });

                if(mSelectedRow == 0)
                    mBackgroundUpdater.updateBackgroundAsync(items.get(0).headerImage);
            }

            @DebugLog
            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), R.string.movies_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TVSearchActivity.startActivity(getActivity());
            }
        });

        setOnItemViewClickedListener(this);
        setOnItemViewSelectedListener(this);
    }

    private void setupAdapters() {
        setupMovies();
        setupTVShows();
        setupMoreMovies();
        setupMoreTVShows();
        setupMore();
    }

    private void setupMovies() {
        HeaderItem moviesHeader = new HeaderItem(0, getString(R.string.top_movies));
        MediaCardPresenter mediaCardPresenter = new MediaCardPresenter(getActivity());
        mMoviesAdapter = new ArrayObjectAdapter(mediaCardPresenter);
        mMoviesAdapter.add(new MediaCardPresenter.MediaCardItem(true));
        mRowsAdapter.add(new ListRow(moviesHeader, mMoviesAdapter));
    }

    private void setupMoreMovies() {
        HeaderItem moreMoviesHeader = new HeaderItem(1, getString(R.string.more_movies));
        MorePresenter morePresenter = new MorePresenter(getActivity());
        ArrayObjectAdapter moreRowAdapter = new ArrayObjectAdapter(morePresenter);

        // add items
        List<MediaProvider.NavInfo> navigation = providerManager.getMediaProvider(ProviderManager.PROVIDER_TYPE_MOVIE).getNavigation();
        for (MediaProvider.NavInfo info : navigation) {
            moreRowAdapter.add(new MorePresenter.MoreItem(
                    info.getId(),
                    info.getLabel(),
                    info.getIcon(),
                    info));
        }

        mRowsAdapter.add(new ListRow(moreMoviesHeader, moreRowAdapter));
    }

    private void setupTVShows() {
        HeaderItem showsHeader = new HeaderItem(0, getString(R.string.latest_shows));
        MediaCardPresenter mediaCardPresenter = new MediaCardPresenter(getActivity());
        mShowAdapter = new ArrayObjectAdapter(mediaCardPresenter);
        mShowAdapter.add(new MediaCardPresenter.MediaCardItem(true));
        mRowsAdapter.add(new ListRow(showsHeader, mShowAdapter));
    }

    private void setupMoreTVShows() {
        HeaderItem moreHeader = new HeaderItem(1, getString(R.string.more_shows));
        MorePresenter morePresenter = new MorePresenter(getActivity());
        ArrayObjectAdapter moreRowAdapter = new ArrayObjectAdapter(morePresenter);

        // add items
        List<MediaProvider.NavInfo> navigation = providerManager.getMediaProvider(ProviderManager.PROVIDER_TYPE_SHOW).getNavigation();
        for (MediaProvider.NavInfo info : navigation) {
            moreRowAdapter.add(new MorePresenter.MoreItem(
                    info.getId(),
                    info.getLabel(),
                    info.getIcon(),
                    info));
        }

        mRowsAdapter.add(new ListRow(moreHeader, moreRowAdapter));
    }

    private void setupMore() {
        HeaderItem gridHeader = new HeaderItem(0, getString(R.string.more));
        MorePresenter gridPresenter = new MorePresenter(getActivity());
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        if (BuildConfig.DEBUG) {
            gridRowAdapter.add(new MorePresenter.MoreItem(R.id.more_player_tests, getString(R.string.tests), R.drawable.more_player_tests, null));
        }
        gridRowAdapter.add(new MorePresenter.MoreItem(R.id.more_item_settings, getString(R.string.preferences), R.drawable.ic_settings, null));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
    }

    private void onMediaItemClicked(MediaCardPresenter.CustomImageCardView view, MediaCardPresenter.MediaCardItem media) {
        if (media.isLoading()) return;
        Bundle options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(),
                view.getMainImageView(),
                TVMediaDetailActivity.SHARED_ELEMENT_NAME).toBundle();

        Media mediaItem = media.getMedia();
        if (view.getCustomSelectedSwatch() != null) {
            mediaItem.color = view.getCustomSelectedSwatch().getRgb();
        }

        TVMediaDetailActivity.startActivity(
                getActivity(),
                options,
                mediaItem);
    }

    private void onMoreItemClicked(MorePresenter.MoreItem moreItem) {
        switch (moreItem.getId()) {
            case R.id.more_player_tests:
                openPlayerTestDialog();
                break;
            case R.id.more_item_settings:
                TVPreferencesActivity.startActivity(getActivity());
                break;
            case R.id.movie_filter_a_to_z:
            case R.id.movie_filter_trending:
            case R.id.movie_filter_release_date:
            case R.id.movie_filter_popular_now:
            case R.id.movie_filter_year:
            case R.id.movie_filter_top_rated:
                providerManager.setCurrentProviderType(ProviderManager.PROVIDER_TYPE_MOVIE);
                TVMediaGridActivity.startActivity(getActivity(), moreItem.getNavInfo().getLabel(),
                        moreItem.getNavInfo().getFilter(), moreItem.getNavInfo().getOrder(), null);
                break;
            case R.id.tvshow_filter_a_to_z:
            case R.id.tvshow_filter_trending:
            case R.id.tvshow_filter_last_updated:
            case R.id.tvshow_filter_popular_now:
            case R.id.tvshow_filter_year:
            case R.id.tvshow_filter_top_rated:
                providerManager.setCurrentProviderType(ProviderManager.PROVIDER_TYPE_SHOW);
                TVMediaGridActivity.startActivity(getActivity(), moreItem.getNavInfo().getLabel(),
                        moreItem.getNavInfo().getFilter(), moreItem.getNavInfo().getOrder(), null);
                break;
            case R.id.movie_filter_genres:
                Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void openPlayerTestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final String[] file_types = getResources().getStringArray(R.array.file_types);
        final String[] files = getResources().getStringArray(R.array.files);

        builder.setTitle("Player Tests")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setSingleChoiceItems(file_types, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                dialogInterface.dismiss();
                final String location = files[index];
                if (location.equals("dialog")) {
                    final EditText dialogInput = new EditText(getActivity());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                            .setView(dialogInput)
                            .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Movie media = new Movie();

                                    media.videoId = "dialogtestvideo";
                                    media.title = "User input test video";

                                    TVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location), 0);
                                }
                            });
                    builder.show();
                }

                final Movie media = new Movie();
                media.videoId = "bigbucksbunny";
                media.title = file_types[index];
                media.subtitles = new HashMap<>();
                media.subtitles.put("en", "http://sv244.cf/bbb-subs.srt");

                providerManager.getCurrentSubsProvider().download(media, "en", new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        TVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        TVVideoPlayerActivity.startActivity(getActivity(), new StreamInfo(media, null, null, null, null, location));
                    }
                });
            }
        });

        builder.show();
    }
}
