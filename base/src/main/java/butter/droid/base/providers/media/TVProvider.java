/*
 * This file is part of Popcorn Time.
 *
 * Popcorn Time is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Popcorn Time is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Popcorn Time. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.media;

import android.accounts.NetworkErrorException;

import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butter.droid.base.BuildConfig;
import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.meta.MetaProvider;
import butter.droid.base.providers.meta.TraktProvider;
import butter.droid.base.providers.subs.OpenSubsProvider;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.utils.StringUtils;
import timber.log.Timber;

public class TVProvider extends MediaProvider {

    private static Integer CURRENT_API = 0;
    private static final String[] API_URLS = BuildConfig.TV_URLS;
    private static final SubsProvider sSubsProvider = new OpenSubsProvider();
    private static final MetaProvider sMetaProvider = new TraktProvider();
    private static final MediaProvider sMediaProvider = new TVProvider();

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }

        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new NameValuePair("limit", "30"));

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("keywords", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new NameValuePair("genre", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new NameValuePair("order", "1"));
        } else {
            params.add(new NameValuePair("order", "-1"));
        }

        String sort = "";
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "popularity";
                break;
            case TRENDING:
                sort = "trending";
                break;
            case YEAR:
                sort = "year";
                break;
            case DATE:
                sort = "updated";
                break;
            case RATING:
                sort = "rating";
                break;
            case ALPHABET:
                sort = "name";
                break;
        }

        params.add(new NameValuePair("sort", sort));

        String url = API_URLS[CURRENT_API] + "shows/";
        if (filters.page != null) {
            url += filters.page;
        } else {
            url += "1";
        }

        Request.Builder requestBuilder = new Request.Builder();
        String query = buildQuery(params);
        url = url + "?" + query;
        requestBuilder.url(url);
        requestBuilder.tag(MEDIA_CALL);

        Timber.d("TVProvider", "Making request to: " + url);

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from EZTV
     *
     * @param currentList    Current shown list to be extended
     * @param requestBuilder Request to be executed
     * @param callback       Network callback
     * @return Call
     */
    private Call fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                String url = requestBuilder.build().urlString();
                if (CURRENT_API >= API_URLS.length - 1) {
                    callback.onFailure(e);
                } else {
                    if(url.contains(API_URLS[CURRENT_API])) {
                        url = url.replace(API_URLS[CURRENT_API], API_URLS[CURRENT_API + 1]);
                        CURRENT_API++;
                    } else {
                        url = url.replace(API_URLS[CURRENT_API - 1], API_URLS[CURRENT_API]);
                    }
                    requestBuilder.url(url);
                    fetchList(currentList, requestBuilder, filters, callback);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseStr = response.body().string();

                        ArrayList<LinkedTreeMap<String, Object>> list = null;
                        if (responseStr.isEmpty()) {
                            list = new ArrayList<>();
                        } else {
                            list = (ArrayList<LinkedTreeMap<String, Object>>) mGson.fromJson(responseStr, ArrayList.class);
                        }

                        TVReponse result = new TVReponse(list);
                        if (list == null) {
                            callback.onFailure(new NetworkErrorException("Empty response"));
                        } else {
                            ArrayList<Media> formattedData = result.formatListForPopcorn(currentList);
                            callback.onSuccess(filters, formattedData, list.size() > 0);
                            return;
                        }
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
                callback.onFailure(new NetworkErrorException("Couldn't connect to TVAPI"));
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, final Callback callback) {
        Request.Builder requestBuilder = new Request.Builder();
        String url = API_URLS[CURRENT_API] + "show/" + currentList.get(index).videoId;
        requestBuilder.url(url);
        requestBuilder.tag(MEDIA_CALL);

        Timber.d("TVProvider", "Making request to: " + url);

        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    LinkedTreeMap<String, Object> map = mGson.fromJson(responseStr, LinkedTreeMap.class);
                    TVReponse result = new TVReponse(map);
                    if (map == null) {
                        callback.onFailure(new NetworkErrorException("Empty response"));
                    } else {
                        ArrayList<Media> formattedData = result.formatDetailForPopcorn();

                        if (formattedData.size() > 0) {
                            callback.onSuccess(null, formattedData, true);
                            return;
                        }
                        callback.onFailure(new IllegalStateException("Empty list"));
                        return;
                    }
                }
                callback.onFailure(new NetworkErrorException("Couldn't connect to TVAPI"));
            }
        });
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_shows;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();

        tabs.add(new NavInfo(R.id.tvshow_filter_trending,Filters.Sort.TRENDING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.trending),R.drawable.tvshow_filter_trending));
        tabs.add(new NavInfo(R.id.tvshow_filter_popular_now,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.tvshow_filter_popular_now));
        tabs.add(new NavInfo(R.id.tvshow_filter_top_rated,Filters.Sort.RATING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.top_rated),R.drawable.tvshow_filter_top_rated));
        tabs.add(new NavInfo(R.id.tvshow_filter_last_updated,Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.last_updated),R.drawable.tvshow_filter_last_updated));
        tabs.add(new NavInfo(R.id.tvshow_filter_year,Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year),R.drawable.tvshow_filter_year));
        tabs.add(new NavInfo(R.id.tvshow_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.tvshow_filter_a_to_z));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        List<Genre> returnList = new ArrayList<>();
        returnList.add(new Genre("all", R.string.genre_all));
        returnList.add(new Genre("action", R.string.genre_action));
        returnList.add(new Genre("adventure", R.string.genre_adventure));
        returnList.add(new Genre("animation", R.string.genre_animation));
        returnList.add(new Genre("comedy", R.string.genre_comedy));
        returnList.add(new Genre("crime", R.string.genre_crime));
        returnList.add(new Genre("disaster", R.string.genre_disaster));
        returnList.add(new Genre("documentary", R.string.genre_documentary));
        returnList.add(new Genre("drama", R.string.genre_drama));
        returnList.add(new Genre("eastern", R.string.genre_eastern));
        returnList.add(new Genre("family", R.string.genre_family));
        returnList.add(new Genre("fantasy", R.string.genre_fantasy));
        returnList.add(new Genre("fan-film", R.string.genre_fan_film));
        returnList.add(new Genre("film-noir", R.string.genre_film_noir));
        returnList.add(new Genre("history", R.string.genre_history));
        returnList.add(new Genre("holiday", R.string.genre_holiday));
        returnList.add(new Genre("horror", R.string.genre_horror));
        returnList.add(new Genre("indie", R.string.genre_indie));
        returnList.add(new Genre("music", R.string.genre_music));
        returnList.add(new Genre("mystery", R.string.genre_mystery));
        returnList.add(new Genre("road", R.string.genre_road));
        returnList.add(new Genre("romance", R.string.genre_romance));
        returnList.add(new Genre("science-fiction", R.string.genre_sci_fi));
        returnList.add(new Genre("short", R.string.genre_short));
        returnList.add(new Genre("sports", R.string.genre_sport));
        returnList.add(new Genre("suspense", R.string.genre_suspense));
        returnList.add(new Genre("thriller", R.string.genre_thriller));
        returnList.add(new Genre("tv-movie", R.string.genre_tv_movie));
        returnList.add(new Genre("war", R.string.genre_war));
        returnList.add(new Genre("western", R.string.genre_western));
        return returnList;
    }

    static private class TVReponse {
        LinkedTreeMap<String, Object> showData;
        ArrayList<LinkedTreeMap<String, Object>> showsList;

        public TVReponse(LinkedTreeMap<String, Object> showData) {
            this.showData = showData;
        }

        public ArrayList<Media> formatDetailForPopcorn() {
            ArrayList<Media> list = new ArrayList<>();
            try {
                Show show = new Show(sMediaProvider, sSubsProvider);

                show.title = (String) showData.get("title");
                show.videoId = (String) showData.get("imdb_id");
                show.imdbId = (String) showData.get("imdb_id");
                show.tvdbId = (String) showData.get("tvdb_id");
                show.seasons = ((Double) showData.get("num_seasons")).intValue();
                show.year = (String) showData.get("year");
                LinkedTreeMap<String, String> images = (LinkedTreeMap<String, String>) showData.get("images");
                if(!images.get("poster").contains("images/posterholder.png")) {
                    show.image = images.get("poster").replace("/original/", "/medium/");
                    show.fullImage = images.get("poster");
                }
                if(!images.get("poster").contains("images/posterholder.png"))
                    show.headerImage = images.get("fanart").replace("/original/", "/medium/");

                if (showData.get("status") != null) {
                    String status = (String) showData.get("status");
                    if (status.equalsIgnoreCase("ended")) {
                        show.status = Show.Status.ENDED;
                    } else if (status.equalsIgnoreCase("returning series")) {
                        show.status = Show.Status.CONTINUING;
                    } else if (status.equalsIgnoreCase("in production")) {
                        show.status = Show.Status.CONTINUING;
                    } else if (status.equalsIgnoreCase("canceled")) {
                        show.status = Show.Status.CANCELED;
                    }
                }

                show.country = (String) showData.get("country");
                show.network = (String) showData.get("network");
                show.synopsis = (String) showData.get("synopsis");
                show.runtime = (String) showData.get("runtime");
                show.airDay = (String) showData.get("air_day");
                show.airTime = (String) showData.get("air_time");
                show.rating = Double.toString(((LinkedTreeMap<String, Double>) showData.get("rating")).get("percentage") / 10);

                List<String> genres = (ArrayList<String>) showData.get("genres");

                show.genre = "";
                if (genres.size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String genre : genres) {
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(", ");
                        }
                        stringBuilder.append(StringUtils.capWords(genre));
                    }
                    show.genre = stringBuilder.toString();
                }

                ArrayList<LinkedTreeMap<String, Object>> episodes = (ArrayList<LinkedTreeMap<String, Object>>) showData.get("episodes");
                Set<String> episodeSet = new HashSet<>();
                for (LinkedTreeMap<String, Object> episode : episodes) {
                    try {
                        String episodeStr = String.format(Locale.US, "S%dE%d", ((Double) episode.get("season")).intValue(), ((Double) episode.get("episode")).intValue());
                        if(episodeSet.contains(episodeStr))
                            continue;
                        episodeSet.add(episodeStr);

                        Episode episodeObject = new Episode(sMediaProvider, sSubsProvider, sMetaProvider);
                        LinkedTreeMap<String, LinkedTreeMap<String, Object>> torrents =
                                (LinkedTreeMap<String, LinkedTreeMap<String, Object>>) episode.get("torrents");
                        for (String key : torrents.keySet()) {
                            if (!key.equals("0")) {
                                LinkedTreeMap<String, Object> item = torrents.get(key);
                                Media.Torrent torrent = new Media.Torrent();
                                torrent.url = item.get("url").toString();
                                torrent.seeds = ((Double) item.get("seeds")).intValue();
                                torrent.peers = ((Double) item.get("peers")).intValue();
                                episodeObject.torrents.put(key, torrent);
                            }
                        }

                        episodeObject.showName = show.title;
                        episodeObject.dateBased = (Boolean) episode.get("date_based");
                        episodeObject.aired = ((Double) episode.get("first_aired")).intValue();
                        episodeObject.title = (String) episode.get("title");
                        episodeObject.overview = (String) episode.get("overview");
                        episodeObject.season = ((Double) episode.get("season")).intValue();
                        episodeObject.episode = ((Double) episode.get("episode")).intValue();
                        episodeObject.videoId = show.videoId + episodeObject.season + episodeObject.episode;
                        episodeObject.imdbId = show.imdbId;
                        episodeObject.image = episodeObject.fullImage = episodeObject.headerImage = show.headerImage;

                        show.episodes.add(episodeObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                list.add(show);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list;
        }

        public TVReponse(ArrayList<LinkedTreeMap<String, Object>> showsList) {
            this.showsList = showsList;
        }

        public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList) {
            for (LinkedTreeMap<String, Object> item : showsList) {
                Show show = new Show(sMediaProvider, sSubsProvider);

                show.title = (String) item.get("title");
                show.videoId = (String) item.get("imdb_id");
                show.seasons = (Integer) item.get("seasons");
                show.tvdbId = (String) item.get("tvdb_id");
                show.year = (String) item.get("year");
                LinkedTreeMap<String, String> images = (LinkedTreeMap<String, String>) item.get("images");
                if(!images.get("poster").contains("images/posterholder.png"))
                    show.image = images.get("poster").replace("/original/", "/medium/");
                if(!images.get("poster").contains("images/posterholder.png"))
                    show.headerImage = images.get("fanart").replace("/original/", "/medium/");

                existingList.add(show);
            }
            return existingList;
        }
    }

}
