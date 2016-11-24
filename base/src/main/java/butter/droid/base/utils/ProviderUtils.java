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

package butter.droid.base.utils;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import butter.droid.base.R;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.manager.provider.ProviderManager.ProviderType;

public class ProviderUtils {

    @StringRes
    public static int getProviderTitle(@ProviderType int providerType) {
        switch (providerType) {
            case ProviderManager.PROVIDER_TYPE_MOVIE:
                return R.string.title_movies;
            case ProviderManager.PROVIDER_TYPE_SHOW:
                return R.string.title_shows;
            case ProviderManager.PROVIDER_TYPE_ANIME:
                return R.string.title_anime;
            default:
                throw new IllegalStateException("Unknown provider type");
        }
    }

    @DrawableRes
    public static int getProviderIcon(@ProviderType int providerType) {
        switch (providerType) {
            case ProviderManager.PROVIDER_TYPE_MOVIE:
                return R.drawable.ic_nav_movies;
            case ProviderManager.PROVIDER_TYPE_SHOW:
                return R.drawable.ic_nav_tv;
            case ProviderManager.PROVIDER_TYPE_ANIME:
                return R.drawable.ic_nav_anime;
            default:
                throw new IllegalStateException("Unknown provider type");
        }
    }

}
