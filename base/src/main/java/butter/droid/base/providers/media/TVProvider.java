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

import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.BuildConfig;
import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.response.TVDetailsReponse;
import butter.droid.base.providers.media.response.TVResponse;
import butter.droid.base.providers.media.response.models.shows.ShowDetails;
import butter.droid.base.providers.subs.SubsProvider;
import okhttp3.OkHttpClient;

public class TVProvider extends MediaProvider {

    public TVProvider(OkHttpClient client, ObjectMapper mapper, @Nullable SubsProvider subsProvider) {
        super(client, mapper, subsProvider, BuildConfig.TV_URLS, "shows/", "show/", 0);
    }

    @Override
    public ArrayList<Media> getResponseFormattedList(String responseStr, ArrayList<Media> currentList) throws IOException {
        ArrayList<Media> formattedData = currentList;
        List<butter.droid.base.providers.media.response.models.shows.Show> list = mapper.readValue(responseStr, mapper.getTypeFactory().constructCollectionType(List.class, butter.droid.base.providers.media.response.models.shows.Show.class));
        if (!list.isEmpty()) {
            formattedData = new TVResponse(list).formatListForPopcorn(currentList, this, getSubsProvider());
        }
        return formattedData;
    }

    @Override
    public ArrayList<Media> getResponseDetailsFormattedList(String responseStr) throws IOException {
        ShowDetails detail = mapper.readValue(responseStr, ShowDetails.class);
        return new TVDetailsReponse().formatDetailForPopcorn(detail, this, getSubsProvider());
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.tvshow_filter_trending, Filters.Sort.TRENDING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.trending), R.drawable.tvshow_filter_trending));
        tabs.add(new NavInfo(R.id.tvshow_filter_popular_now, Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular), R.drawable.tvshow_filter_popular_now));
        tabs.add(new NavInfo(R.id.tvshow_filter_top_rated, Filters.Sort.RATING, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.top_rated), R.drawable.tvshow_filter_top_rated));
        tabs.add(new NavInfo(R.id.tvshow_filter_last_updated, Filters.Sort.DATE, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.last_updated), R.drawable.tvshow_filter_last_updated));
        tabs.add(new NavInfo(R.id.tvshow_filter_year, Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year), R.drawable.tvshow_filter_year));
        tabs.add(new NavInfo(R.id.tvshow_filter_a_to_z, Filters.Sort.ALPHABET, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.a_to_z), R.drawable.tvshow_filter_a_to_z));
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
}
