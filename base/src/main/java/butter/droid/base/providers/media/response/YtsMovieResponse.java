package butter.droid.base.providers.media.response;

import android.text.TextUtils;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butter.droid.base.providers.media.YtsMoviesProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.subs.SubsProvider;
import butter.droid.base.utils.LocaleUtils;

public class YtsMovieResponse {
    private ArrayList<LinkedTreeMap<String, Object>> moviesList;

    public YtsMovieResponse(ArrayList<LinkedTreeMap<String, Object>> moviesList) {
        this.moviesList = moviesList;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList, YtsMoviesProvider mediaProvider, SubsProvider subsProvider) {
        for (LinkedTreeMap<String, Object> item : moviesList) {
            Movie movie = new Movie(mediaProvider, subsProvider);

            movie.videoId = Integer.toString(((Double) item.get("year")).intValue());
            movie.imdbId = (String) item.get("imdb_code");
            movie.title = (String) item.get("title_english");
            int year = ((Double) item.get("year")).intValue();
            movie.year = Integer.toString(year);
            movie.genre = TextUtils.join(", ", (ArrayList<String>) item.get("genres"));
            movie.rating = Double.toString((Double) item.get("rating"));
            movie.trailer = "http://youtube.com/watch?v=" + item.get("yt_trailer_code");
            movie.runtime = Double.toString((Double) item.get("runtime"));
            movie.synopsis = (String) item.get("synopsis");
            movie.certification = (String) item.get("mpa_rating");

            movie.image = (String) item.get("medium_cover_image");
            movie.fullImage = (String) item.get("background_image_original");
            movie.headerImage = (String) item.get("background_image_original");

            ArrayList<LinkedTreeMap<String, Object>> torrents =
                    (ArrayList<LinkedTreeMap<String, Object>>) item.get("torrents");
            if (torrents != null) {
                for (LinkedTreeMap<String, Object> torrentEntry : torrents) {
                    int seeds = ((Double) torrentEntry.get("seeds")).intValue();
                    int peers = ((Double) torrentEntry.get("peers")).intValue();
                    String hash = (String) torrentEntry.get("hash");
                    String url = (String) torrentEntry.get("url");
                    Media.Torrent torrent = new Media.Torrent(url, seeds, peers, hash);
                    String quality = (String) torrentEntry.get("quality");
                    Map<String, Media.Torrent> torrentMap = new HashMap<>();
                    torrentMap.put(quality, torrent);
                    movie.torrents.put(LocaleUtils.toLocale("en").getDisplayLanguage() , torrentMap);
                }
            }

            existingList.add(movie);
        }

        return existingList;
    }
}
