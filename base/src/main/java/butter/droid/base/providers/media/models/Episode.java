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

package butter.droid.base.providers.media.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.meta.MetaProvider;

public class Episode extends Media implements Parcelable {
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Episode> CREATOR = new Parcelable.Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };
    public String showName;
    public int aired;
    public int episode;
    public int season;
    public String overview;
    public boolean dateBased;
    public Map<String, Torrent> torrents = new HashMap<>();
    private String tvdbId;
    private MetaProvider mMetaProvider;

    public Episode(MetaProvider metaProvider) {
        super();
        mMetaProvider = metaProvider;
    }

    @Override
    public int getProviderType() {
        return ProviderManager.PROVIDER_TYPE_SHOW;
    }
    private Episode(Parcel in) {
        super(in);
        aired = in.readInt();
        episode = in.readInt();
        season = in.readInt();
        title = in.readString();
        overview = in.readString();
        tvdbId = in.readString();
        showName = in.readString();
        dateBased = in.readInt() == 1;

        String className = in.readString();
        mMetaProvider = null;
        try {
            Class<?> clazz = Class.forName(className);
            mMetaProvider = (MetaProvider) clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            Torrent torrent = in.readParcelable(Torrent.class.getClassLoader());
            torrents.put(key, torrent);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(aired);
        dest.writeInt(episode);
        dest.writeInt(season);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(tvdbId);
        dest.writeString(showName);
        dest.writeInt(dateBased ? 1 : 0);
        dest.writeString(mMetaProvider != null ? mMetaProvider.getClass().getCanonicalName() : "");
        if (torrents != null) {
            dest.writeInt(torrents.size());
            for (Map.Entry<String, Torrent> entry : torrents.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeParcelable(entry.getValue(), flags);
            }
        } else {
            dest.writeInt(0);
        }
    }

    public MetaProvider getMetaProvider() {
        return mMetaProvider;
    }

}