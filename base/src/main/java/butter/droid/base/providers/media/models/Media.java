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

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.subs.SubsProvider;

public class Media implements Parcelable {
    public String videoId;
    public String imdbId;
    public String title;
    public String year;
    public String genre;
    public String rating;
    public Boolean isMovie = false;
    public String image;
    public String fullImage;
    public String headerImage;
    public Map<String, String> subtitles;
    public int color = Color.parseColor("#3F51B5");
    protected SubsProvider mSubsProvider = null;
    protected MediaProvider mMediaProvider = null;

    public Media(MediaProvider provider, SubsProvider subsProvider) {
        mMediaProvider = provider;
        mSubsProvider = subsProvider;
    }

    public Media(Parcel in) {
        videoId = in.readString();
        imdbId = in.readString();
        title = in.readString();
        year = in.readString();
        genre = in.readString();
        rating = in.readString();
        isMovie = in.readInt() == 1;
        image = in.readString();
        fullImage = in.readString();
        headerImage = in.readString();
        color = in.readInt();

        String className = in.readString();
        mSubsProvider = null;
        try {
            Class<?> clazz = Class.forName(className);
            mSubsProvider = (SubsProvider) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        className = in.readString();
        mMediaProvider = null;
        try {
            Class<?> clazz = Class.forName(className);
            mMediaProvider = (MediaProvider) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        int length = in.readInt();
        subtitles = new HashMap<>();
        for (int i = 0; i < length; i++) {
            subtitles.put(in.readString(), in.readString());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(videoId);
        dest.writeString(imdbId);
        dest.writeString(title);
        dest.writeString(year);
        dest.writeString(genre);
        dest.writeString(rating);
        dest.writeInt(isMovie ? 1 : 2);
        dest.writeString(image);
        dest.writeString(fullImage);
        dest.writeString(headerImage);
        dest.writeInt(color);
        dest.writeString(mSubsProvider != null ? mSubsProvider.getClass().getCanonicalName() : "");
        dest.writeString(mMediaProvider != null ? mMediaProvider.getClass().getCanonicalName() : "");
        if (subtitles != null) {
            dest.writeInt(subtitles.size());
            for (Map.Entry<String, String> entry : subtitles.entrySet()){
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(0);
        }
    }

    @SuppressWarnings("unused")
    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel in) {
            return new Media(in);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };

    public static class Torrent implements Parcelable {
        public String url;
        public Integer seeds;
        public Integer peers;
        public String hash;

        public Torrent() {

        }

        public Torrent(String url, Integer seeds, Integer peers, String hash) {
            this.url = url;
            this.seeds = seeds;
            this.peers = peers;
            this.hash = hash;
        }

        public Torrent(Parcel in) {
            url = in.readString();
            seeds = in.readInt();
            peers = in.readInt();
            hash = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(url);
            dest.writeInt(seeds);
            dest.writeInt(peers);
            dest.writeString(hash);
        }

        @SuppressWarnings("unused")
        public static final Creator<Torrent> CREATOR = new Creator<Torrent>() {
            @Override
            public Torrent createFromParcel(Parcel in) {
                return new Torrent(in);
            }

            @Override
            public Torrent[] newArray(int size) {
                return new Torrent[size];
            }
        };
    }

    public SubsProvider getSubsProvider() {
        return mSubsProvider;
    }

    public MediaProvider getMediaProvider() {
        return mMediaProvider;
    }

}
