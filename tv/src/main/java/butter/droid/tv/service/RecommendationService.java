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

package butter.droid.tv.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.utils.VersionUtils;
import butter.droid.tv.R;
import butter.droid.tv.TVButterApplication;
import butter.droid.tv.activities.TVMediaDetailActivity;
import butter.droid.tv.service.recommendation.RecommendationBuilder;
import butter.droid.tv.service.recommendation.RecommendationContentProvider;
import timber.log.Timber;

public class RecommendationService extends IntentService {

    @Inject
    ProviderManager providerManager;

    private ArrayList<Media> mMovies = new ArrayList<>();
    private ArrayList<Media> mShows = new ArrayList<>();

    public RecommendationService() {
        super("RecommendationService");
    }

    private static final int MAX_MOVIE_RECOMMENDATIONS = 3;
    private static final int MAX_SHOW_RECOMMENDATIONS = 3;

    private int PRIORITY = MAX_MOVIE_RECOMMENDATIONS + MAX_SHOW_RECOMMENDATIONS;
    private int TOTAL_COUNT=0;

    @Override
    public void onCreate() {
        super.onCreate();

        TVButterApplication.getAppContext()
                .getComponent()
                .inject(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if(!VersionUtils.isAndroidTV()) {
            return;
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MediaProvider.Filters movieFilter = new MediaProvider.Filters();
        movieFilter.order = MediaProvider.Filters.Order.DESC;
        movieFilter.sort = MediaProvider.Filters.Sort.POPULARITY;

        /*
        Disabled, since no shows provider
        MediaProvider.Filters showsFilter = new MediaProvider.Filters();
        showsFilter.order = MediaProvider.Filters.Order.DESC;
        showsFilter.sort = MediaProvider.Filters.Sort.DATE;
        */

        final AtomicBoolean mMoviesCallFinished = new AtomicBoolean(false);
        final AtomicBoolean mShowsCallFinished = new AtomicBoolean(false);


        Timber.d("Fetching movies");
        //fetch movies
        if (providerManager.hasProvider(ProviderManager.PROVIDER_TYPE_MOVIE)) {
            Timber.d("Fetching movies");
            //noinspection ConstantConditions
            providerManager.getMediaProvider(ProviderManager.PROVIDER_TYPE_MOVIE)
                    .getList(movieFilter, new MediaProvider.Callback() {
                        @Override
                        public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items, boolean changed) {
                            Timber.d(String.format("loaded %s movies", items.size()));
                            mMovies.addAll(items);
                            mMoviesCallFinished.set(true);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Timber.d("Failed to fetch movies");
                            mMoviesCallFinished.set(true);
                        }
                    });
        }

        /*
        Disabled, since no shows provider

        Timber.d("Fetching shows");
        //fetch shows
        mShowProvider.getList(showsFilter, new MediaProvider.Callback() {
            @Override
            public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items, boolean changed) {
                Timber.d(String.format("loaded %s shows", items.size()));
                mShows.addAll(items);
                mShowsCallFinished.set(true);
            }

            @Override
            public void onFailure(Exception e) {
                Timber.d("Failed to fetch shows");
                mShowsCallFinished.set(true);
            }
        });
        */
        mShowsCallFinished.set(true);

        //wait for callbacks to finish
        while (!mShowsCallFinished.get() || !mMoviesCallFinished.get()) {
            Timber.d("Waiting on callbacks");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Timber.d("mShowsCallFinished: " + mShowsCallFinished.get());
        Timber.d("mMoviesCallFinished: " + mMoviesCallFinished.get());
        //process items

        Timber.d("Updating recommendation cards");
        if (mMovies.size() == 0 && mShows.size() == 0)
            return;


        RecommendationBuilder builder = new RecommendationBuilder()
                .setContext(getApplicationContext())
                .setSmallIcon(R.drawable.header_logo);

        buildMovieRecommendations(builder);
        buildShowRecommendations(builder);
    }

    private void buildMovieRecommendations(@NonNull RecommendationBuilder builder) {
        Timber.d("building movie recommendations");
        try {
            int count = 0;
            for (Media media : mMovies) {
                Movie movie = (Movie) media;

                Timber.d("Recommendation - " + movie.title);
                PRIORITY--;
                TOTAL_COUNT--;
                builder.setBackgroundContentUri(RecommendationContentProvider.CONTENT_URI + URLEncoder.encode(movie.headerImage, "UTF-8"))
                        .setId(TOTAL_COUNT)
                        .setPriority(PRIORITY)
                        .setTitle(movie.title)
                        .setDescription(movie.synopsis)
                        .setImage(movie.image)
                        .setIntent(buildPendingIntent(movie))
                        .build();

                if (++count >= MAX_MOVIE_RECOMMENDATIONS) {
                    break;
                }

            }
        } catch (IOException e) {
            Timber.e("Unable to update recommendation", e);
        }
    }

    private void buildShowRecommendations(@NonNull RecommendationBuilder builder) {
        Timber.d("building show recommendations");
        try {
            int count = 0;
            for (Media media : mShows) {
                Show show = (Show) media;

                Timber.d("Recommendation - " + show.title);

                Episode latestEpisode = findLatestEpisode(show);

                PRIORITY--;
                TOTAL_COUNT--;
                builder.setBackgroundContentUri(RecommendationContentProvider.CONTENT_URI + URLEncoder.encode(show.headerImage, "UTF-8"))
                        .setId(TOTAL_COUNT)
                        .setPriority(PRIORITY)
                        .setTitle(show.title)
                        .setDescription(latestEpisode == null ? "" : getString(R.string.episode_number_format, latestEpisode.episode))
                        .setImage(show.image)
                        .setIntent(buildPendingIntent(show))
                        .build();

                if (++count >= MAX_SHOW_RECOMMENDATIONS) {
                    break;
                }

            }
        } catch (IOException e) {
            Timber.e("Unable to update recommendation", e);
        }
    }

    private Episode findLatestEpisode(Show show) {
        if (show.episodes == null || show.episodes.size() == 0) return null;
        return show.episodes.get(show.episodes.size());
    }

    private PendingIntent buildPendingIntent(Media media) {
        Intent detailIntent = TVMediaDetailActivity.buildIntent(this, media);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(TVMediaDetailActivity.class);
        stackBuilder.addNextIntent(detailIntent);
        // Ensure a unique PendingIntents, otherwise all recommendations end up with the same
        // PendingIntent
        detailIntent.setAction(media.videoId);

        PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return intent;
    }
}
