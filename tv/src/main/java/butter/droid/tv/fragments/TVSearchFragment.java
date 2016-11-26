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

import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.MoviesProvider;
import butter.droid.base.providers.media.TVProvider;
import butter.droid.base.providers.media.YtsMoviesProvider;
import butter.droid.base.utils.ThreadUtils;
import hugo.weaving.DebugLog;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.tv.R;
import butter.droid.tv.activities.TVMediaDetailActivity;
import butter.droid.tv.presenters.MediaCardPresenter;
import butter.droid.tv.utils.BackgroundUpdater;

public class TVSearchFragment extends android.support.v17.leanback.app.SearchFragment
		implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {
	private static final int SEARCH_DELAY_MS = 300;

	private MediaProvider tvShowsProvider = new TVProvider();
	private MediaProvider ytsMoviesProvider = new YtsMoviesProvider();
	private MediaProvider moviesProvider = new MoviesProvider();
	private MediaProvider.Filters searchFilter = new MediaProvider.Filters();

	private ArrayObjectAdapter objectAdapter;
	private Handler handler = new Handler();
	private SearchRunnable searchRunnable;
	private ListRowPresenter listRowPresenter;
	private ListRow loadingListRow;
	private BackgroundUpdater backgroundUpdater = new BackgroundUpdater();

	@Override public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		backgroundUpdater.initialise(getActivity(), R.color.black);
		listRowPresenter = new ListRowPresenter();
		listRowPresenter.setShadowEnabled(false);
		objectAdapter = new ArrayObjectAdapter(listRowPresenter);
		setSearchResultProvider(this);
		setOnItemViewClickedListener(getDefaultItemClickedListener());
		setOnItemViewSelectedListener(new ItemViewSelectedListener());
		searchRunnable = new SearchRunnable();

		// setup row to use for loading
		loadingListRow = createLoadingRow();
	}

	@Override
	public ObjectAdapter getResultsAdapter() {
		return objectAdapter;
	}

	private ListRow createLoadingRow() {
		HeaderItem loadingHeader = new HeaderItem(0, getString(R.string.search_results));
		ArrayObjectAdapter loadingRowAdapter = new ArrayObjectAdapter(new MediaCardPresenter(getActivity()));
		loadingRowAdapter.add(new MediaCardPresenter.MediaCardItem(true));
		return new ListRow(loadingHeader, loadingRowAdapter);
	}

	private void queryByWords(String words) {
		objectAdapter.clear();
		if (!TextUtils.isEmpty(words)) {
			searchRunnable.setSearchQuery(words);
			handler.removeCallbacks(searchRunnable);
			handler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
		}
	}

	@Override
	public boolean onQueryTextChange(String newQuery) {
		if (newQuery.length() > 3) queryByWords(newQuery);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		queryByWords(query);
		return true;
	}

	@DebugLog
	private void loadRows(String query) {
		ytsMoviesProvider.cancel();
		moviesProvider.cancel();
		tvShowsProvider.cancel();
		objectAdapter.clear();

		addLoadingRow();

		searchFilter.keywords = query;
		searchFilter.page = 1;

        searchMovies();
    }

    private void searchMovies() {
        ytsMoviesProvider.getList(searchFilter, new MediaProvider.Callback() {
			@Override
			public void onSuccess(MediaProvider.Filters filters, final ArrayList<Media> items, boolean changed) {
				ThreadUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
                        List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                        addRow(getString(R.string.movie_results), list);
                        searchTvShows();
					}
				});
			}

            @Override public void onFailure(final Exception e) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        searchTvShows();
                    }
                });
            }
        });

        moviesProvider.getList(searchFilter, new MediaProvider.Callback() {
			@Override
			public void onSuccess(MediaProvider.Filters filters, final ArrayList<Media> items, boolean changed) {
				ThreadUtils.runOnUiThread(new Runnable() {
					@Override
					public void run() {
                        List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                        addRow(getString(R.string.movie_results), list);
					}
				});
			}

            @Override public void onFailure(final Exception e) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void searchTvShows() {
        tvShowsProvider.getList(searchFilter, new MediaProvider.Callback() {
            @Override
            public void onSuccess(MediaProvider.Filters filters, final ArrayList<Media> items, boolean changed) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<MediaCardPresenter.MediaCardItem> list = MediaCardPresenter.convertMediaToOverview(items);
                        addRow(getString(R.string.show_results), list);
                    }
                });
            }

            @Override
            public void onFailure(final Exception e) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void addRow(String title, List<MediaCardPresenter.MediaCardItem> items) {
		objectAdapter.remove(loadingListRow);
		HeaderItem header = new HeaderItem(0, title);
		ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new MediaCardPresenter(getActivity()));
		listRowAdapter.addAll(0, items);
		objectAdapter.add(new ListRow(header, listRowAdapter));
	}

	private void addLoadingRow() {
		objectAdapter.add(loadingListRow);
	}

	protected OnItemViewClickedListener getDefaultItemClickedListener() {
		return new OnItemViewClickedListener() {
			@Override public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object object, RowPresenter.ViewHolder rowViewHolder,
					Row row) {
				if (object instanceof MediaCardPresenter.MediaCardItem) {
					MediaCardPresenter.MediaCardItem item = (MediaCardPresenter.MediaCardItem) object;
					Media media = item.getMedia();
					TVMediaDetailActivity.startActivity(getActivity(), media);
				}
			}
		};
	}

	private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
		@Override
		public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
				RowPresenter.ViewHolder rowViewHolder, Row row) {
			if (item instanceof MediaCardPresenter.MediaCardItem) {
				MediaCardPresenter.MediaCardItem overviewItem = (MediaCardPresenter.MediaCardItem) item;
				if (overviewItem.isLoading()) return;

				backgroundUpdater.updateBackgroundAsync(overviewItem.getMedia().headerImage);
			}
		}
	}

	private class SearchRunnable implements Runnable {
		private volatile String searchQuery;

		public SearchRunnable() { }

		public void run() {
			loadRows(searchQuery);
		}

		public void setSearchQuery(String value) {
			this.searchQuery = value;
		}
	}
}
