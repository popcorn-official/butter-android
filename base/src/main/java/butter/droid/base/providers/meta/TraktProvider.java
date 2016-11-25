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

package butter.droid.base.providers.meta;

import android.os.AsyncTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Episode;
import com.uwetrottmann.trakt.v2.entities.Movie;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.services.Episodes;
import com.uwetrottmann.trakt.v2.services.Movies;

import retrofit.RetrofitError;

public class TraktProvider extends MetaProvider {

    private static final String API_KEY = "c7e20abc718e46fc75399dd6688afca9ac83cd4519c9cb1fba862b37b8640e89";
    private static TraktV2 TRAKT = new TraktV2();
    private static Movies MOVIES;
    private static Episodes EPISODES;

    static {
        TRAKT.setApiKey(API_KEY);
        MOVIES = TRAKT.movies();
        EPISODES = TRAKT.episodes();
    }


    public TraktProvider(OkHttpClient client, ObjectMapper mapper) {
        super(client, mapper);
    }

    /**
     * Get metadata from Trakt for a movie
     *
     * @param imdbId   IMDb id to get metadata for
     * @param callback Callback
     */
    @Override
    public void getMovieMeta(final String imdbId, Callback callback) {
        new TraktTask(callback) {
            @Override
            protected MetaData doInBackground(Void... params) {
                try {
                    Movie m = MOVIES.summary(imdbId, Extended.FULLIMAGES);

                    MetaData metaData = new MetaData();
                    metaData.certification = m.certification;
                    metaData.genres = m.genres.toArray(new String[m.genres.size()]);
                    metaData.imdb_id = imdbId;
                    metaData.overview = m.overview;
                    metaData.released = m.released;
                    metaData.year = m.year;
                    metaData.rating = m.rating;
                    metaData.runtime = m.runtime;
                    metaData.trailer = m.trailer;
                    metaData.tagline = m.tagline;
                    metaData.title = m.title;
                    metaData.images = new MetaData.Images(m.images.poster.full, m.images.fanart.full);

                    return metaData;
                } catch (RetrofitError e) {
                    mException = e;
                }
                return null;
            }
        }.execute();
    }

    /**
     * Get metadata from Trakt for episode
     *
     * @param imdbId   IMDb id to get metadata for
     * @param season   Season
     * @param episode  Episode
     * @param callback Callback
     */
    @Override
    public void getEpisodeMeta(final String imdbId, final int season, final int episode, Callback callback) {
        new TraktTask(callback) {
            @Override
            protected MetaData doInBackground(Void... params) {
                try {
                    Episode e = EPISODES.summary(imdbId, season, episode, Extended.FULLIMAGES);

                    MetaData metaData = new MetaData();
                    metaData.overview = e.overview;
                    metaData.title = e.title;
                    metaData.images = new MetaData.Images(e.images.screenshot.medium);
                    metaData.rating = e.rating;
                    metaData.released = e.first_aired;

                    return metaData;
                } catch (RetrofitError e) {
                    mException = e;
                }
                return null;
            }
        }.execute();
    }

    abstract class TraktTask extends AsyncTask<Void, Void, MetaData> {

        Exception mException;
        private Callback mCallback;

        public TraktTask(Callback callback) {
            mCallback = callback;
        }

        @Override
        protected void onPostExecute(MetaData metaData) {
            super.onPostExecute(metaData);
            mCallback.onResult(metaData, mException);
        }
    }
}