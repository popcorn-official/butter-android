package butter.droid.base.providers.media.response.models.anime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import butter.droid.base.providers.media.response.models.common.Torrents;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Episode {

    @JsonProperty("tvdb_id")
    private String tvdbId;
    @JsonProperty("overview")
    private String overview;
    @JsonProperty("episode")
    private String episode;
    @JsonProperty("season")
    private String season;
    @JsonProperty("torrents")
    private Torrents torrents;
    @JsonProperty("title")
    private String title;

    /**
     * @return The tvdbId
     */
    @JsonProperty("tvdb_id")
    public String getTvdbId() {
        return tvdbId;
    }

    /**
     * @param tvdbId The tvdb_id
     */
    @JsonProperty("tvdb_id")
    public void setTvdbId(String tvdbId) {
        this.tvdbId = tvdbId;
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
     * @return The episode
     */
    @JsonProperty("episode")
    public String getEpisode() {
        return episode;
    }

    /**
     * @param episode The episode
     */
    @JsonProperty("episode")
    public void setEpisode(String episode) {
        this.episode = episode;
    }

    /**
     * @return The season
     */
    @JsonProperty("season")
    public String getSeason() {
        return season;
    }

    /**
     * @param season The season
     */
    @JsonProperty("season")
    public void setSeason(String season) {
        this.season = season;
    }

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

}
