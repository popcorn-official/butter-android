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
import android.annotation.SuppressLint;
import android.text.Html;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butter.droid.base.BuildConfig;
import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.meta.MetaProvider;
import butter.droid.base.providers.meta.TraktProvider;
import butter.droid.base.providers.subs.OpenSubsProvider;
import butter.droid.base.providers.subs.SubsProvider;
import timber.log.Timber;

@SuppressLint("ParcelCreator")
public class AnimeProvider extends MediaProvider {

    private static Integer CURRENT_API = 0;
    private static final String[] API_URLS = BuildConfig.ANIME_URLS;
    private static final MediaProvider sMediaProvider = new AnimeProvider();

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

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "popularity";
                break;
            case YEAR:
                sort = "year";
                break;
            case RATING:
                sort = "rating";
                break;
            case ALPHABET:
                sort = "name";
                break;
        }

        params.add(new NameValuePair("sort", sort));

        String url = API_URLS[CURRENT_API] + "animes/";
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

        Timber.d("AnimeProvider", "Making request to: " + url);

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from Haruhichan
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

                        AnimeResponse result = new AnimeResponse(list);
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
                callback.onFailure(new NetworkErrorException("Couldn't connect to AnimeAPI"));
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, final Callback callback) {
        Request.Builder requestBuilder = new Request.Builder();
        String url = API_URLS[CURRENT_API] + "anime/" + currentList.get(index).videoId;
        requestBuilder.url(url);
        requestBuilder.tag(MEDIA_CALL);

        Timber.d("AnimeProvider", "Making request to: " + url);

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
                    AnimeResponse result = new AnimeResponse(map);
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
                callback.onFailure(new NetworkErrorException("Couldn't connect to AnimeAPI"));
            }
        });
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_data;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.anime_filter_popular,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.anime_filter_popular));
        tabs.add(new NavInfo(R.id.anime_filter_year,Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year),R.drawable.anime_filter_year));
        tabs.add(new NavInfo(R.id.anime_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.anime_filter_a_to_z));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        List<Genre> returnList = new ArrayList<>();
        returnList.add(new Genre("All", R.string.genre_all));
        returnList.add(new Genre("Action", R.string.genre_action));
        returnList.add(new Genre("Adventure", R.string.genre_adventure));
        returnList.add(new Genre("Racing", R.string.genre_cars));
        returnList.add(new Genre("Comedy", R.string.genre_comedy));
        returnList.add(new Genre("Dementia", R.string.genre_dementia));
        returnList.add(new Genre("Demons", R.string.genre_demons));
        returnList.add(new Genre("Drama", R.string.genre_drama));
        returnList.add(new Genre("Ecchi", R.string.genre_ecchi));
        returnList.add(new Genre("Fantasy", R.string.genre_fantasy));
        returnList.add(new Genre("Game", R.string.genre_game));
        returnList.add(new Genre("Gender Bender", R.string.gender_bender));
        returnList.add(new Genre("Gore", R.string.gore));
        returnList.add(new Genre("Harem", R.string.genre_harem));
        returnList.add(new Genre("Historical", R.string.genre_history));
        returnList.add(new Genre("Horror", R.string.genre_horror));
        returnList.add(new Genre("Kids", R.string.genre_kids));
        returnList.add(new Genre("Magic", R.string.genre_magic));
        returnList.add(new Genre("Mahou Shoujo", R.string.mahou_shoujo));
        returnList.add(new Genre("Mahou Shounen", R.string.mahou_shounen));
        returnList.add(new Genre("Martial Arts", R.string.genre_martial_arts));
        returnList.add(new Genre("Mecha", R.string.genre_mecha));
        returnList.add(new Genre("Military", R.string.genre_military));
        returnList.add(new Genre("Music", R.string.genre_music));
        returnList.add(new Genre("Mystery", R.string.genre_mystery));
        returnList.add(new Genre("Parody", R.string.genre_parody));
        returnList.add(new Genre("Police", R.string.genre_police));
        returnList.add(new Genre("Psychological", R.string.genre_psychological));
        returnList.add(new Genre("Romance", R.string.genre_romance));
        returnList.add(new Genre("Samurai", R.string.genre_samurai));
        returnList.add(new Genre("School", R.string.genre_school));
        returnList.add(new Genre("Sci-Fi", R.string.genre_sci_fi));
        returnList.add(new Genre("Shoujo Ai", R.string.genre_shoujo_ai));
        returnList.add(new Genre("Shounen Ai", R.string.genre_shounen_ai));
        returnList.add(new Genre("Slice of Life", R.string.genre_slice_of_life));
        returnList.add(new Genre("Space", R.string.genre_space));
        returnList.add(new Genre("Sports", R.string.genre_sport));
        returnList.add(new Genre("Super Power", R.string.genre_super_power));
        returnList.add(new Genre("Supernatural", R.string.genre_supernatural));
        returnList.add(new Genre("Thriller", R.string.genre_thriller));
        returnList.add(new Genre("Vampire", R.string.genre_vampire));
        returnList.add(new Genre("Yuri", R.string.genre_yuri));
        return returnList;
    }

    static private class AnimeResponse {
        LinkedTreeMap<String, Object> detailData;
        ArrayList<LinkedTreeMap<String, Object>> showsList;

        Show.Status[] statusArray = new Show.Status[]{Show.Status.NOT_AIRED_YET, Show.Status.CONTINUING, Show.Status.ENDED};

        public AnimeResponse(LinkedTreeMap<String, Object> detailData) {
            this.detailData = detailData;
        }

        public ArrayList<Media> formatDetailForPopcorn() {
            ArrayList<Media> list = new ArrayList<>();
            try {
                List<LinkedTreeMap<String, Object>> episodes = (List<LinkedTreeMap<String, Object>>) detailData.get("episodes");

                Media media = new Movie(null, null);
                /*
                 * Chris Alderson:
                 * As of version 2.2.0 of the Anime API there are no movies in the database.
                 * And movies won't be added to the database, so there is no need to check for it.
                 */
                if (detailData.get("type").toString().equalsIgnoreCase("show")) {
                    Show show = new Show(sMediaProvider, null);

                    show.seasons = ((Double) detailData.get("num_seasons")).intValue();
                    show.runtime = (String) detailData.get("runtime");
                    show.synopsis = (String) detailData.get("synopsis");
                    if (detailData.get("status") != null) {
                        String status = (String) detailData.get("status");
                        if (status.equalsIgnoreCase("finished airing")) {
                            show.status = Show.Status.ENDED;
                        } else if (status.equalsIgnoreCase("currently airing")) {
                            show.status = Show.Status.CONTINUING;
                        } else if (status.equalsIgnoreCase("not aird yet")) {
                            show.status = Show.Status.NOT_AIRED_YET;
                        }
                    }

                    Map<Integer, Episode> episodeMap = new HashMap<>();
                    for (LinkedTreeMap<String, Object> episode : episodes) {
                        try {
                            Episode episodeObject = new Episode(sMediaProvider, null, null);

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
                            episodeObject.dateBased = false;
                            episodeObject.aired = -1;
                            episodeObject.title = (String) episode.get("title");
                            episodeObject.overview = (String) episode.get("overview");
                            episodeObject.season = Integer.parseInt(episode.get("season").toString());
                            episodeObject.episode = Integer.parseInt(episode.get("episode").toString());
                            episodeObject.videoId = show.videoId + episodeObject.season + episodeObject.episode;
                            episodeObject.imdbId = show.imdbId;
                            episodeObject.image = episodeObject.fullImage = episodeObject.headerImage = show.headerImage;

                            episodeMap.put(episodeObject.episode, episodeObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    show.episodes = new LinkedList<>(episodeMap.values());
                    media = show;
                }

                media.title = (String) detailData.get("title");
                media.videoId = (String) detailData.get("_id");
                media.imdbId = "mal-" + media.videoId;
                media.year = (String) detailData.get("year");
                LinkedTreeMap<String, String> images = (LinkedTreeMap<String, String>) detailData.get("images");
                if(!images.get("poster").contains("images/posterholder.png")) {
                    media.image = images.get("poster").replace("/original/", "/medium/");
                    media.fullImage = images.get("poster");
                }
                if(!images.get("poster").contains("images/posterholder.png"))
                    media.headerImage = images.get("fanart").replace("/original/", "/medium/");

                media.genre = ((ArrayList<String>) detailData.get("genres")).get(0);
                media.rating = Double.toString(((LinkedTreeMap<String, Double>) detailData.get("rating")).get("percentage") / 10);

                list.add(media);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list;
        }

        public AnimeResponse(ArrayList<LinkedTreeMap<String, Object>> showsList) {
            this.showsList = showsList;
        }

        public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList) {
            for (LinkedTreeMap<String, Object> item : showsList) {
                Media media = null;
                if (item.get("type").toString().equalsIgnoreCase("movie")) {
                    Movie movie = new Movie(null, null);
                    media = movie;
                    /*
                     * Chris Alderson:
                     * As of version 2.2.0 of the Anime API there are no movies in the database.
                     * And movies won't be added to the database, so there is no need to check for it.
                     */
                } else if (item.get("type").toString().equalsIgnoreCase("show")) {
                    Show show = new Show(sMediaProvider, null);

                    show.title = (String) item.get("title");
                    show.videoId = (String) item.get("_id");
                    show.seasons = (Integer) item.get("seasons");
                    // media.tvdbId = (String) item.get("tvdb_id");
                    show.year = (String) item.get("year");
                    LinkedTreeMap<String, String> images = (LinkedTreeMap<String, String>) item.get("images");
                    if(!images.get("poster").contains("images/posterholder.png"))
                        show.image = images.get("poster").replace("/original/", "/medium/");
                    if(!images.get("poster").contains("images/posterholder.png"))
                        show.headerImage = images.get("fanart").replace("/original/", "/medium/");

                    media = show;
                }
                existingList.add(media);
            }
            return existingList;
        }
    }

}
