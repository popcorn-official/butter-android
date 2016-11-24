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

package butter.droid.tv;

import javax.inject.Singleton;

import butter.droid.tv.activities.TVMainActivity;
import butter.droid.tv.activities.TVMediaDetailActivity;
import butter.droid.tv.activities.TVMediaGridActivity;
import butter.droid.tv.activities.TVPreferencesActivity;
import butter.droid.tv.activities.TVSearchActivity;
import butter.droid.tv.activities.TVStreamLoadingActivity;
import butter.droid.tv.activities.TVTrailerPlayerActivity;
import butter.droid.tv.activities.TVUpdateActivity;
import butter.droid.tv.activities.TVVideoPlayerActivity;
import butter.droid.tv.activities.TVWelcomeActivity;
import butter.droid.tv.fragments.TVMediaGridFragment;
import butter.droid.tv.fragments.TVMovieDetailsFragment;
import butter.droid.tv.fragments.TVOverviewFragment;
import butter.droid.tv.fragments.TVPreferencesFragment;
import butter.droid.tv.fragments.TVSearchFragment;
import butter.droid.tv.fragments.TVStreamLoadingFragment;
import butter.droid.tv.fragments.TVVideoPlayerFragment;
import butter.droid.tv.service.RecommendationService;
import butter.droid.tv.service.recommendation.RecommendationContentProvider;
import dagger.Component;

@Singleton
@Component(
        modules = ApplicationModule.class
)
public interface ApplicationComponent {

    void inject(TVButterApplication application);

    void inject(TVMainActivity activity);

    void inject(TVTrailerPlayerActivity activity);

    void inject(TVMediaDetailActivity activity);

    void inject(TVMediaGridActivity activity);

    void inject(TVPreferencesActivity activity);

    void inject(TVSearchActivity activity);

    void inject(TVStreamLoadingActivity activity);

    void inject(TVUpdateActivity activity);

    void inject(TVVideoPlayerActivity activity);

    void inject(TVWelcomeActivity activity);

    void inject(RecommendationService service);

    void inject(TVOverviewFragment fragment);

    void inject(TVMovieDetailsFragment fragment);

    void inject(TVMediaGridFragment fragment);

    void inject(TVStreamLoadingFragment fragment);

    void inject(TVPreferencesFragment fragment);

    void inject(TVSearchFragment fragment);

    void inject(TVVideoPlayerFragment fragment);

    void inject(RecommendationContentProvider contentProvider);

}
