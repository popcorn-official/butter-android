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
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PresenterSelector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.torrent.StreamInfo;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.TVStreamLoadingActivity;
import butter.droid.tv.presenters.ShowDetailsDescriptionPresenter;
import butter.droid.tv.presenters.showdetail.EpisodeCardPresenter;

public class TVShowDetailsFragment extends TVBaseDetailsFragment
        implements MediaProvider.Callback,
        OnActionClickedListener,
        EpisodeCardPresenter.Listener {

    @Inject
    ProviderManager providerManager;

    public static Fragment newInstance(Media media) {
        TVShowDetailsFragment fragment = new TVShowDetailsFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_ITEM, media);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
    }

    @Override
    void loadDetails() {
        ArrayList<Media> mediaList = new ArrayList<>();
        mediaList.add(getShowItem());

        providerManager.getCurrentMediaProvider().getDetail(mediaList, 0, this);
    }

    @Override
    AbstractDetailsDescriptionPresenter getDetailPresenter() {
        return new ShowDetailsDescriptionPresenter();
    }

    @Override
    void onDetailLoaded() {
        updateShowsAdapterContent();
    }

    @Override
    ClassPresenterSelector createPresenters(ClassPresenterSelector selector) {
        selector.addClassPresenter(DetailsOverviewRow.class, new EpisodeCardPresenter(getActivity()));
        return null;
    }

    @Override
    void addActions(Media item) { }

    @Override
    protected ArrayObjectAdapter createAdapter(PresenterSelector selector) {
        return new ArrayObjectAdapter(selector);
    }

    @Override
    public void onActionClicked(Action action) {
        //no actions yet
    }

    @Override
    public void onEpisodeClicked(Episode episode) {
        if (null == episode) {
            return;
        }

        // start first torrent
        if (episode.torrents.size() == 1) {
            List<Map.Entry<String, Media.Torrent>> torrents = new ArrayList<>(episode.torrents.entrySet());
            onTorrentSelected(episode, torrents.get(0));
        }
        // ask user which torrent
        else {
            showTorrentsDialog(episode, episode.torrents);
        }
    }

    private void updateShowsAdapterContent() {
        final TreeMap<Integer, List<Episode>> seasons = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer me, Integer other) {
                return me - other;
            }
        });

        for (Episode episode : getShowItem().episodes) {
            // create list of season if does not exists
            if (!seasons.containsKey(episode.season)) {
                seasons.put(episode.season, new ArrayList<Episode>());
            }

            // add episode to the list
            final List<Episode> seasonEpisodes = seasons.get(episode.season);
            seasonEpisodes.add(episode);
        }

        ArrayObjectAdapter objectAdapter = getObjectArrayAdapter();

        for (Integer seasonKey : seasons.descendingKeySet()) {
            Collections.sort(seasons.get(seasonKey), new Comparator<Episode>() {
                @Override
                public int compare(Episode me, Episode other) {
                    if (me.episode < other.episode) return -1;
                    else if (me.episode > other.episode) return 1;
                    return 0;
                }
            });

            EpisodeCardPresenter presenter = new EpisodeCardPresenter(getActivity());
            presenter.setOnClickListener(this);
            ArrayObjectAdapter episodes = new ArrayObjectAdapter(presenter);

            for (Episode episode : seasons.get(seasonKey)) {
                episodes.add(episode);
            }
            HeaderItem header = new HeaderItem(seasonKey, String.format("Season %d", seasonKey));
            objectAdapter.add(new ListRow(header, episodes));
        }

        objectAdapter.notifyArrayItemRangeChanged(0, objectAdapter.size());
    }

    private Show getShowItem() {
        return (Show) getMediaItem();
    }

    @SuppressWarnings("unchecked")
    private void showTorrentsDialog(final Episode episode, final Map<String, Media.Torrent> torrents) {
        ArrayList<String> choices = new ArrayList<>(torrents.keySet());
        final ArrayList torrent = new ArrayList(torrents.entrySet());
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.choose_quality))
                .setSingleChoiceItems(choices.toArray(new CharSequence[choices.size()]), 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onTorrentSelected(episode, (Map.Entry<String, Media.Torrent>) torrent.get(which));
                        dialog.dismiss();
                    }
                }).show();
    }

    private void onTorrentSelected(Episode episode, Map.Entry<String, Media.Torrent> torrent) {
        String subtitleLanguage = PrefUtils.get(
            getActivity(),
            Prefs.SUBTITLE_DEFAULT,
            SubsProvider.SUBTITLE_LANGUAGE_NONE);

        Show show = getShowItem();

        StreamInfo info = new StreamInfo(
                episode,
                show,
                torrent.getValue().getUrl(),
                subtitleLanguage,
                torrent.getKey());

        TVStreamLoadingActivity.startActivity(getActivity(), info, show);
    }
}
