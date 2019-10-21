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

package butter.droid.base.manager.provider;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.subs.SubsProvider;

public class ProviderManager {

    // region IntDef

    public static final int PROVIDER_TYPE_MOVIE = 0;
    public static final int PROVIDER_TYPE_SHOW = 1;
    public static final int PROVIDER_TYPE_ANIME = 2;
    @Nullable
    private final MediaProvider movieProvider;

    // endregion IntDef
    @Nullable
    private final MediaProvider showProvider;
    @Nullable
    private final MediaProvider animeProvider;
    private final List<OnProviderChangeListener> listeners = new ArrayList<>();
    @ProviderType
    private int currentProviderType;

    public ProviderManager(@Nullable MediaProvider movieProvider, @Nullable MediaProvider showProvider, @Nullable MediaProvider animeProvider) {
        this.movieProvider = movieProvider;
        this.showProvider = showProvider;
        this.animeProvider = animeProvider;

        if (movieProvider != null) {
            currentProviderType = PROVIDER_TYPE_MOVIE;
        } else if (showProvider != null) {
            currentProviderType = PROVIDER_TYPE_SHOW;
        } else if (animeProvider != null) {
            currentProviderType = PROVIDER_TYPE_ANIME;
        } else {
            throw new IllegalStateException("No media providers vere provider");
        }
    }

    @ProviderType
    public int getCurrentMediaProviderType() {
        return currentProviderType;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    public MediaProvider getCurrentMediaProvider() {
        return getMediaProvider(getCurrentMediaProviderType());
    }

    @MainThread
    public void setCurrentProviderType(@ProviderType int providerType) {
        if (getMediaProvider(providerType) != null) {
            if (this.currentProviderType != providerType) {
                this.currentProviderType = providerType;
                if (listeners.size() > 0) {
                    for (OnProviderChangeListener listener : listeners) {
                        listener.onProviderChanged(providerType);
                    }
                }
            }
        } else {
            throw new IllegalStateException("Provider for type no provided");
        }
    }

    @Nullable
    public MediaProvider getMediaProvider(@ProviderType int providerType) {
        switch (providerType){
            case PROVIDER_TYPE_ANIME:
                return animeProvider;
            case PROVIDER_TYPE_MOVIE:
                return movieProvider;
            case PROVIDER_TYPE_SHOW:
                return showProvider;
            default:
                return null;
        }
    }

    public boolean hasProvider(@ProviderType int providerType) {
        return getMediaProvider(providerType) != null;
    }

    public void addProviderListener(@NonNull OnProviderChangeListener listener) {
        //noinspection ConstantConditions
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeProviderListener(@NonNull OnProviderChangeListener listener) {
        if (listener != null && listeners.size() > 0) {
            listeners.remove(listener);
        }
    }

    public SubsProvider getCurrentSubsProvider() {
        return getCurrentMediaProvider().getSubsProvider();
    }

    public boolean hasCurrentSubsProvider() {
        return getCurrentMediaProvider().hasSubsProvider();
    }

    @IntDef({PROVIDER_TYPE_MOVIE, PROVIDER_TYPE_SHOW, PROVIDER_TYPE_ANIME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProviderType {
    }

    public interface OnProviderChangeListener {
        @MainThread
        void onProviderChanged(@ProviderType int provider);
    }

}
