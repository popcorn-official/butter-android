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

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.subs.SubsProvider;

public class Movie extends Media implements Parcelable {
    public String trailer = "";
    public String runtime = "";
    public String synopsis = "No synopsis available";
    public String certification = "n/a";
    public Map<String, Map<String, Torrent>> torrents = new HashMap<>();

    public Movie(MediaProvider mediaProvider, SubsProvider subsProvider) {
        super(mediaProvider, subsProvider);
        isMovie = true;
    }

    protected Movie(Parcel in) {
        super(in);
        trailer = in.readString();
        runtime = in.readString();
        synopsis = in.readString();
        certification = in.readString();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            int mapSize = in.readInt();
            Map<String, Torrent> torrentMap = new HashMap<>();
            for (int j = 0; j < mapSize; j++) {
                String torrentKey = in.readString();
                Torrent torrent = in.readParcelable(Torrent.class.getClassLoader());
                torrentMap.put(torrentKey, torrent);
            }
            torrents.put(key, torrentMap);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(trailer);
        dest.writeString(runtime);
        dest.writeString(synopsis);
        dest.writeString(certification);
        if (torrents != null) {
            dest.writeInt(torrents.size());
            for (Map.Entry<String, Map<String, Torrent>> entry : torrents.entrySet()) {
                dest.writeString(entry.getKey());
                Map<String, Torrent> torrentMap = entry.getValue();
                if (torrentMap != null) {
                    dest.writeInt(torrentMap.size());
                    for (Map.Entry<String, Torrent> tmapEntry : torrentMap.entrySet()){
                        dest.writeString(tmapEntry.getKey());
                        dest.writeParcelable(tmapEntry.getValue(), flags);
                    }
                } else {
                    dest.writeInt(0);
                }
            }
        } else {
            dest.writeInt(0);
        }
    }

    @SuppressWarnings("unused")
    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

}
