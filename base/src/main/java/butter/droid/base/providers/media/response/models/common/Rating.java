package butter.droid.base.providers.media.response.models.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Rating {

    @JsonProperty("percentage")
    private int percentage;
    @JsonProperty("watching")
    private int watching;
    @JsonProperty("votes")
    private int votes;
    @JsonProperty("loved")
    private int loved;
    @JsonProperty("hated")
    private int hated;

    /**
     * @return The percentage
     */
    public int getPercentage() {
        return percentage;
    }

    /**
     * @param percentage The percentage
     */
    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    /**
     * @return The watching
     */
    public int getWatching() {
        return watching;
    }

    /**
     * @param watching The watching
     */
    public void setWatching(int watching) {
        this.watching = watching;
    }

    /**
     * @return The votes
     */
    public int getVotes() {
        return votes;
    }

    /**
     * @param votes The votes
     */
    public void setVotes(int votes) {
        this.votes = votes;
    }

    /**
     * @return The loved
     */
    public int getLoved() {
        return loved;
    }

    /**
     * @param loved The loved
     */
    public void setLoved(int loved) {
        this.loved = loved;
    }

    /**
     * @return The hated
     */
    public int getHated() {
        return hated;
    }

    /**
     * @param hated The hated
     */
    public void setHated(int hated) {
        this.hated = hated;
    }

}
