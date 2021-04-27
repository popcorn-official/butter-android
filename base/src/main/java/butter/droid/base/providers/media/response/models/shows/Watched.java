package butter.droid.base.providers.media.response.models.shows;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Watched {

    @JsonProperty("watched")
    private boolean watched;

    /**
     * @return The watched
     */
    @JsonProperty("watched")
    public boolean isWatched() {
        return watched;
    }

    /**
     * @param watched The watched
     */
    @JsonProperty("watched")
    public void setWatched(boolean watched) {
        this.watched = watched;
    }

}
