package butter.droid.base.providers.media;

import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.response.YtsMovieResponse;
import butter.droid.base.providers.subs.YSubsProvider;

@SuppressLint("ParcelCreator")
public class YtsMoviesProvider extends MediaProvider {
    private LinkedHashMap<String, Integer> currentPages = new LinkedHashMap<>();

    public YtsMoviesProvider() {
        super(new String[] { "https://yts.ag/api/v2/list_movies.json" }, "data", "", 0);
    }

    @Override
    public Call getList(ArrayList<Media> existingList, Filters filters, Callback callback) {
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = new ArrayList<>(existingList);
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new AbstractMap.SimpleEntry<>("keywords", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new AbstractMap.SimpleEntry<>("genre", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new AbstractMap.SimpleEntry<>("order", "asc"));
        } else {
            params.add(new AbstractMap.SimpleEntry<>("order", "desc"));
        }

        if(filters.langCode != null) {
            params.add(new AbstractMap.SimpleEntry<>("lang", filters.langCode));
        }

        String sort;
        switch (filters.sort) {
            default:
            case TRENDING:
                sort = "seeds";
                break;
            case POPULARITY:
                sort = "like_count";
                break;
            case RATING:
                sort = "rating";
                break;
            case DATE:
                sort = "date_added";
                break;
            case YEAR:
                sort = "year";
                break;
            case ALPHABET:
                sort = "title";
                break;
        }

        Integer currentPage = 1;
        if (!currentPages.keySet().contains(sort)) {
            currentPages.put(sort, currentPage);
        }
        else {
            currentPage = currentPages.get(sort) + 1;
            currentPages.put(sort, currentPage);
        }

        filters.page = currentPage;
        params.add(new AbstractMap.SimpleEntry<>("sort", sort));
        params.add(new AbstractMap.SimpleEntry<>("limit", "50"));

        if (filters.page != null) {
            params.add(new AbstractMap.SimpleEntry<>("page", filters.page.toString()));
        } else {
            filters.page = 1;
            params.add(new AbstractMap.SimpleEntry<>("page", filters.page.toString()));
        }

        params.add(new AbstractMap.SimpleEntry<>("limit", "20"));
        params.add(new AbstractMap.SimpleEntry<>("with_rt_ratings", "true"));

        Request.Builder requestBuilder = new Request.Builder();
        String query = buildQuery(params);
        String moviesUrl = "https://yts.ag/api/v2/list_movies.json";
        requestBuilder.url(moviesUrl + "?" + query);
        requestBuilder.tag(getMediaCallTag());

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    private Call fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseStr = response.body().string();

                        ArrayList<LinkedTreeMap<String, Object>> list;
                        if (responseStr.isEmpty()) {
                            list = new ArrayList<>();
                        } else {
                            LinkedTreeMap<String, Object> responseObject = new Gson().fromJson(responseStr, LinkedTreeMap.class);
                            String status = (String) responseObject.get("status");
                            if (!status.equals("ok")) throw new Exception("API status is not OK");
                            if (!responseObject.containsKey("data")) throw new Exception("API content doesn't have 'data' element");
                            LinkedTreeMap<String, Object> data = (LinkedTreeMap<String, Object>) responseObject.get("data");
                            if (data.containsKey("movies")) list = (ArrayList<LinkedTreeMap<String, Object>>) data.get("movies");
                            else list = new ArrayList<>();
                        }

                        YtsMovieResponse result = new YtsMovieResponse(list);
                        ArrayList<Media> formattedData = result.formatListForPopcorn(currentList, YtsMoviesProvider.this, new YSubsProvider());
                        callback.onSuccess(filters, formattedData, list.size() > 0);
                    }
                    else callback.onFailure(new NetworkErrorException("Couldn't connect to YTS movies API"));
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, Callback callback) {
        ArrayList<Media> returnList = new ArrayList<>();
        returnList.add(currentList.get(index));
        callback.onSuccess(null, returnList, true);
        return null;
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.yts_filter_trending,Filters.Sort.TRENDING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.trending),R.drawable.yts_filter_trending));
        tabs.add(new NavInfo(R.id.yts_filter_popular_now,Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular),R.drawable.yts_filter_popular_now));
        tabs.add(new NavInfo(R.id.yts_filter_top_rated,Filters.Sort.RATING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.top_rated),R.drawable.yts_filter_top_rated));
        tabs.add(new NavInfo(R.id.yts_filter_release_date,Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.release_date),R.drawable.yts_filter_release_date));
        tabs.add(new NavInfo(R.id.yts_filter_year,Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year),R.drawable.yts_filter_year));
        tabs.add(new NavInfo(R.id.yts_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.ASC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.yts_filter_a_to_z));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        List<Genre> returnList = new ArrayList<>();
        returnList.add(new Genre(null, R.string.genre_all));
        returnList.add(new Genre("action", R.string.genre_action));
        returnList.add(new Genre("adventure", R.string.genre_adventure));
        returnList.add(new Genre("animation", R.string.genre_animation));
        returnList.add(new Genre("biography", R.string.genre_biography));
        returnList.add(new Genre("comedy", R.string.genre_comedy));
        returnList.add(new Genre("crime", R.string.genre_crime));
        returnList.add(new Genre("documentary", R.string.genre_documentary));
        returnList.add(new Genre("drama", R.string.genre_drama));
        returnList.add(new Genre("family", R.string.genre_family));
        returnList.add(new Genre("fantasy", R.string.genre_fantasy));
        returnList.add(new Genre("film-noir", R.string.genre_film_noir));
        returnList.add(new Genre("game-show", R.string.genre_game_show));
        returnList.add(new Genre("history", R.string.genre_history));
        returnList.add(new Genre("horror", R.string.genre_horror));
        returnList.add(new Genre("music", R.string.genre_music));
        returnList.add(new Genre("musical", R.string.genre_musical));
        returnList.add(new Genre("mystery", R.string.genre_mystery));
        returnList.add(new Genre("news", R.string.genre_news));
        returnList.add(new Genre("reality-tv", R.string.genre_reality));
        returnList.add(new Genre("romance", R.string.genre_romance));
        returnList.add(new Genre("science-fiction", R.string.genre_sci_fi));
        returnList.add(new Genre("sports", R.string.genre_sport));
        returnList.add(new Genre("talk-show", R.string.genre_talk_show));
        returnList.add(new Genre("thriller", R.string.genre_thriller));
        returnList.add(new Genre("war", R.string.genre_war));
        returnList.add(new Genre("western", R.string.genre_western));
        return returnList;
    }

    @Override
    public String getMediaCallTag() {
        return "yts_movies_http_call";
    }
}