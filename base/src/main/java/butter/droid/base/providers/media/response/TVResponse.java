package butter.droid.base.providers.media.response;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.response.models.Response;
import butter.droid.base.providers.media.response.models.shows.Show;
import butter.droid.base.providers.subs.SubsProvider;

public class TVResponse extends Response<Show> {

    public TVResponse(List<Show> responseItems) {
        super(responseItems);
    }

    public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList, MediaProvider mediaProvider, SubsProvider subsProvider) {
        for (Show item : responseItems) {
            butter.droid.base.providers.media.models.Show show = new butter.droid.base.providers.media.models.Show();

            show.title = item.getTitle();
            show.videoId = item.getImdbId();
            show.seasons = item.getNumSeasons();
            show.tvdbId = item.getTvdbId();
            show.year = item.getYear();
            if (item.getImages().getPoster() != null && !item.getImages().getPoster().contains("images/posterholder.png")) {
                show.image = item.getImages().getPoster().replace("/posters/", "/_cache/posters/");
            }
            if (item.getImages().getFanart() != null && item.getImages().getFanart().contains("images/posterholder.png")) {
                show.headerImage = item.getImages().getFanart().replace("/original/", "/medium/");
            }
            existingList.add(show);
        }
        return existingList;
    }
}