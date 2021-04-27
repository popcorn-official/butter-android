package butter.droid.base.providers.media.response.models.shows;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ShowDetails extends Show {

    @JsonProperty("synopsis")
    private String synopsis;
    @JsonProperty("runtime")
    private String runtime;
    @JsonProperty("country")
    private String country;
    @JsonProperty("network")
    private String network;
    @JsonProperty("air_day")
    private String airDay;
    @JsonProperty("air_time")
    private String airTime;
    @JsonProperty("status")
    private String status;
    @JsonProperty("last_updated")
    private long lastUpdated;
    @JsonProperty("__v")
    private int v;
    @JsonProperty("episodes")
    private List<Episode> episodes = new ArrayList<>();
    @JsonProperty("genres")
    private List<String> genres = new ArrayList<>();

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
     * @return The country
     */
    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    /**
     * @param country The country
     */
    @JsonProperty("country")
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return The network
     */
    @JsonProperty("network")
    public String getNetwork() {
        return network;
    }

    /**
     * @param network The network
     */
    @JsonProperty("network")
    public void setNetwork(String network) {
        this.network = network;
    }

    /**
     * @return The airDay
     */
    @JsonProperty("air_day")
    public String getAirDay() {
        return airDay;
    }

    /**
     * @param airDay The air_day
     */
    @JsonProperty("air_day")
    public void setAirDay(String airDay) {
        this.airDay = airDay;
    }

    /**
     * @return The airTime
     */
    @JsonProperty("air_time")
    public String getAirTime() {
        return airTime;
    }

    /**
     * @param airTime The air_time
     */
    @JsonProperty("air_time")
    public void setAirTime(String airTime) {
        this.airTime = airTime;
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

    /**
     * @return The genres
     */
    @JsonProperty("genres")
    public List<String> getGenres() {
        return genres;
    }

    /**
     * @param genres The genres
     */
    @JsonProperty("genres")
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
}
