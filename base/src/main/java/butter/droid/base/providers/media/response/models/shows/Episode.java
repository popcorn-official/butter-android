package butter.droid.base.providers.media.response.models.shows;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import butter.droid.base.providers.media.response.models.common.Torrents;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Episode {

    @JsonProperty("torrents")
    private Torrents torrents;
    @JsonProperty("watched")
    private Watched watched;
    @JsonProperty("first_aired")
    private int firstAired;
    @JsonProperty("date_based")
    private boolean dateBased;
    @JsonProperty("overview")
    private String overview;
    @JsonProperty("title")
    private String title;
    @JsonProperty("episode")
    private int episode;
    @JsonProperty("season")
    private int season;
    @JsonProperty("tvdb_id")
    private int tvdbId;

    /**
     * @return The torrents
     */
    @JsonProperty("torrents")
    public Torrents getTorrents() {
        return torrents;
    }

    /**
     * @param torrents The torrents
     */
    @JsonProperty("torrents")
    public void setTorrents(Torrents torrents) {
        this.torrents = torrents;
    }

    /**
     * @return The watched
     */
    @JsonProperty("watched")
    public Watched getWatched() {
        return watched;
    }

    /**
     * @param watched The watched
     */
    @JsonProperty("watched")
    public void setWatched(Watched watched) {
        this.watched = watched;
    }

    /**
     * @return The firstAired
     */
    @JsonProperty("first_aired")
    public int getFirstAired() {
        return firstAired;
    }

    /**
     * @param firstAired The first_aired
     */
    @JsonProperty("first_aired")
    public void setFirstAired(int firstAired) {
        this.firstAired = firstAired;
    }

    /**
     * @return The dateBased
     */
    @JsonProperty("date_based")
    public boolean isDateBased() {
        return dateBased;
    }

    /**
     * @param dateBased The date_based
     */
    @JsonProperty("date_based")
    public void setDateBased(boolean dateBased) {
        this.dateBased = dateBased;
    }

    /**
     * @return The overview
     */
    @JsonProperty("overview")
    public String getOverview() {
        return overview;
    }

    /**
     * @param overview The overview
     */
    @JsonProperty("overview")
    public void setOverview(String overview) {
        this.overview = overview;
    }

    /**
     * @return The title
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title
     */
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The episode
     */
    @JsonProperty("episode")
    public int getEpisode() {
        return episode;
    }

    /**
     * @param episode The episode
     */
    @JsonProperty("episode")
    public void setEpisode(int episode) {
        this.episode = episode;
    }

    /**
     * @return The season
     */
    @JsonProperty("season")
    public int getSeason() {
        return season;
    }

    /**
     * @param season The season
     */
    @JsonProperty("season")
    public void setSeason(int season) {
        this.season = season;
    }

    /**
     * @return The tvdbId
     */
    @JsonProperty("tvdb_id")
    public int getTvdbId() {
        return tvdbId;
    }

    /**
     * @param tvdbId The tvdb_id
     */
    @JsonProperty("tvdb_id")
    public void setTvdbId(int tvdbId) {
        this.tvdbId = tvdbId;
    }

}
