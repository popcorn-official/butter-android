package butter.droid.base.providers.media.response.models.anime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AnimeDetails extends Anime {

    @JsonProperty("synopsis")
    private String synopsis;
    @JsonProperty("runtime")
    private String runtime;
    @JsonProperty("status")
    private String status;
    @JsonProperty("last_updated")
    private long lastUpdated;
    @JsonProperty("__v")
    private int v;
    @JsonProperty("episodes")
    private List<Episode> episodes = new ArrayList<>();

    /**
     * @return The synopsis
     */
    @JsonProperty("synopsis")
    public String getSynopsis() {
        return synopsis;
    }

    /**
     * @param synopsis The synopsis
     */
    @JsonProperty("synopsis")
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    /**
     * @return The runtime
     */
    @JsonProperty("runtime")
    public String getRuntime() {
        return runtime;
    }

    /**
     * @param runtime The runtime
     */
    @JsonProperty("runtime")
    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    /**
     * @return The status
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return The lastUpdated
     */
    @JsonProperty("last_updated")
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated The last_updated
     */
    @JsonProperty("last_updated")
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return The v
     */
    @JsonProperty("__v")
    public int getV() {
        return v;
    }

    /**
     * @param v The __v
     */
    @JsonProperty("__v")
    public void setV(int v) {
        this.v = v;
    }

    /**
     * @return The episodes
     */
    @JsonProperty("episodes")
    public List<Episode> getEpisodes() {
        return episodes;
    }

    /**
     * @param episodes The episodes
     */
    @JsonProperty("episodes")
    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }
}
