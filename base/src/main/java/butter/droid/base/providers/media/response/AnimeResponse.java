package butter.droid.base.providers.media.response;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.media.response.models.Response;
import butter.droid.base.providers.media.response.models.anime.Anime;
import butter.droid.base.providers.subs.SubsProvider;

public class AnimeResponse extends Response<Anime> {

    public AnimeResponse(List<Anime> responseItems) {
        super(responseItems);
    }

    public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList, MediaProvider mediaProvider, SubsProvider subsProvider) {
        for (Anime item : responseItems) {
            Media media = null;
            if (item.getType().equalsIgnoreCase("movie")) {
                media = new Movie(null, null);
                    /*
                     * Chris Alderson:
                     * As of version 2.2.0 of the Anime API there are no movies in the database.
                     * And movies won't be added to the database, so there is no need to check for it.
                     */
            } else if (item.getType().equalsIgnoreCase("show")) {
                Show show = new Show(mediaProvider, null);

                show.title = item.getTitle();
                show.videoId = item.getId();
                show.seasons = item.getNumSeasons();
                // media.tvdbId = (String) item.get("tvdb_id");
                show.year = item.getYear();
                if (!item.getImages().getPoster().contains("images/posterholder.png")) {
                    show.image = item.getImages().getPoster().replace("/original/", "/medium/");
                    show.headerImage = item.getImages().getFanart().replace("/original/", "/medium/");
                }
                media = show;
            }
            existingList.add(media);
        }
        return existingList;
    }
}