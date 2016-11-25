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

package butter.droid.base.data;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.StorageUtils;
import dagger.Module;
import dagger.Provides;

@Module
public class DataModule {

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Context context) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(30, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        client.setRetryOnConnectionFailure(true);

        int cacheSize = 10 * 1024 * 1024;
        File cacheLocation = new File(
                PrefUtils.get(context, Prefs.STORAGE_LOCATION, StorageUtils.getIdealCacheDirectory(context)
                        .toString()));
        cacheLocation.mkdirs();
        com.squareup.okhttp.Cache cache = null;
        try {
            cache = new com.squareup.okhttp.Cache(cacheLocation, cacheSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.setCache(cache);

        return client;
    }

    @Provides
    @Singleton
    public OkHttpDownloader provideOkHttpDownloader(Context context) {
        return new OkHttpDownloader(context);
    }

    @Provides
    @Singleton
    public Picasso providePicasso(Context context, OkHttpDownloader okHttpDownloader) {
        Picasso.Builder builder = new Picasso.Builder(context);
        builder.downloader(okHttpDownloader);
        return builder.build();
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

}
