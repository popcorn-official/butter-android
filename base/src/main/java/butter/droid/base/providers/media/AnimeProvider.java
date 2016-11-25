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

import android.support.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.BuildConfig;
import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.response.AnimeDetailsReponse;
import butter.droid.base.providers.media.response.AnimeResponse;
import butter.droid.base.providers.media.response.models.anime.Anime;
import butter.droid.base.providers.media.response.models.anime.AnimeDetails;
import butter.droid.base.providers.subs.SubsProvider;

public class AnimeProvider extends MediaProvider {

    public AnimeProvider(OkHttpClient client, ObjectMapper mapper, @Nullable SubsProvider subsProvider) {
        super(client, mapper, subsProvider, BuildConfig.ANIME_URLS, "animes/", "anime/", 0);
    }

    @Override
    public String getMediaCallTag() {
        return "anime_http_call";
    }

    @Override
    public ArrayList<Media> getResponseFormattedList(String responseStr, ArrayList<Media> currentList) throws IOException {
        ArrayList<Media> formattedData = currentList;
        List<Anime> list = mapper.readValue(responseStr, mapper.getTypeFactory().constructCollectionType(List.class, Anime.class));
        if (!list.isEmpty()) {
            formattedData = new AnimeResponse(list).formatListForPopcorn(currentList, this, getSubsProvider());
        }
        return formattedData;
    }

    @Override
    public ArrayList<Media> getResponseDetailsFormattedList(String responseStr) throws IOException {
        AnimeDetails detail = mapper.readValue(responseStr, AnimeDetails.class);
        return new AnimeDetailsReponse().formatDetailForPopcorn(detail, this, null, null);
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_data;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.anime_filter_popular, Filters.Sort.POPULARITY, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.popular), R.drawable.anime_filter_popular));
        tabs.add(new NavInfo(R.id.anime_filter_year, Filters.Sort.YEAR, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.year), R.drawable.anime_filter_year));
        tabs.add(new NavInfo(R.id.anime_filter_a_to_z, Filters.Sort.ALPHABET, Filters.Order.DESC, ButterApplication.getAppContext().getString(R.string.a_to_z), R.drawable.anime_filter_a_to_z));
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
}
