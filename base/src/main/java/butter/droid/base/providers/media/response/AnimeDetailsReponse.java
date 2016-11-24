package butter.droid.base.providers.media.response;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.providers.media.models.Show;
import butter.droid.base.providers.media.response.models.DetailsResponse;
import butter.droid.base.providers.media.response.models.anime.AnimeDetails;
import butter.droid.base.providers.media.response.models.anime.Episode;
import butter.droid.base.providers.media.response.models.common.Quality;
import butter.droid.base.providers.meta.MetaProvider;
import butter.droid.base.providers.subs.SubsProvider;

public class AnimeDetailsReponse extends DetailsResponse<AnimeDetails> {

    public AnimeDetailsReponse() {
    }

    public ArrayList<Media> formatDetailForPopcorn(AnimeDetails item, MediaProvider mediaProvider, SubsProvider subsProvider, MetaProvider metaProvider) {
        ArrayList<Media> list = new ArrayList<>();
        try {

            Media media = new Movie();
                /*
                 * Chris Alderson:
                 * As of version 2.2.0 of the Anime API there are no movies in the database.
                 * And movies won't be added to the database, so there is no need to check for it.
                 */
            if (item.getType().equalsIgnoreCase("show")) {
                Show show = new Show();
                show.seasons = item.getNumSeasons();
                show.runtime = item.getRuntime();
                show.synopsis = item.getSynopsis();
                if (item.getStatus() != null) {
                    String status = item.getStatus();
                    if (status.equalsIgnoreCase("finished airing")) {
                        show.status = Show.Status.ENDED;
                    } else if (status.equalsIgnoreCase("currently airing")) {
                        show.status = Show.Status.CONTINUING;
                    } else if (status.equalsIgnoreCase("not aird yet")) {
                        show.status = Show.Status.NOT_AIRED_YET;
                    }
                }

                SparseArray<butter.droid.base.providers.media.models.Episode> episodeMap = new SparseArray<>();
                for (Episode episode : item.getEpisodes()) {
                    try {
                        butter.droid.base.providers.media.models.Episode episodeObject = new butter.droid.base.providers.media.models.Episode(metaProvider);

                        if (episode.getTorrents() != null) {
                            for (Map.Entry<String, Quality> entry : episode.getTorrents().getQualities().entrySet()) {
                                if (!entry.getKey().equals("0")) {
                                    Media.Torrent torrent = new Media.Torrent(entry.getValue().getUrl(), entry.getValue().getSeeds(), entry.getValue().getPeers());
                                    episodeObject.torrents.put(entry.getKey(), torrent);
                                }
                            }
                        }

                        episodeObject.showName = show.title;
                        episodeObject.dateBased = false;
                        episodeObject.aired = -1;
                        episodeObject.title = episode.getTitle();
                        episodeObject.overview = episode.getOverview();
                        episodeObject.season = Integer.parseInt(episode.getSeason());
                        episodeObject.episode = Integer.parseInt(episode.getEpisode());
                        episodeObject.videoId = show.videoId + episodeObject.season + episodeObject.episode;
                        episodeObject.imdbId = show.imdbId;
                        episodeObject.image = episodeObject.fullImage = episodeObject.headerImage = show.headerImage;

                        episodeMap.put(episodeObject.episode, episodeObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                show.episodes = new LinkedList<>();
                for (int i = 0; i < episodeMap.size(); i++)
                    show.episodes.add(episodeMap.valueAt(i));

                media = show;
            }

            media.title = item.getTitle();
            media.videoId = item.getId();
            media.imdbId = "mal-" + media.videoId;
            media.year = item.getYear();
            if (item.getImages().getPoster() != null && !item.getImages().getPoster().contains("images/posterholder.png")) {
                media.image = item.getImages().getPoster().replace("/original/", "/medium/");
                media.fullImage = item.getImages().getPoster();
            }
            if (item.getImages().getFanart() != null && item.getImages().getFanart().contains("images/posterholder.png")) {
                media.headerImage = item.getImages().getFanart().replace("/original/", "/medium/");
            }

            media.genre = item.getGenres().get(0);
            media.rating = Double.toString(item.getRating().getPercentage() / 10);

            list.add(media);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

}