package butter.droid.base.providers.media.response.models.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Images {

    @JsonProperty("poster")
    private String poster;
    @JsonProperty("fanart")
    private String fanart;
    @JsonProperty("banner")
    private String banner;

    /**
     * @return The poster
     */
    public String getPoster() {
        return poster;
    }

    /**
     * @param poster The poster
     */
    public void setPoster(String poster) {
        this.poster = poster;
    }

    /**
     * @return The fanart
     */
    public String getFanart() {
        return fanart;
    }

    /**
     * @param fanart The fanart
     */
    public void setFanart(String fanart) {
        this.fanart = fanart;
    }

    /**
     * @return The banner
     */
    public String getBanner() {
        return banner;
    }

    /**
     * @param banner The banner
     */
    public void setBanner(String banner) {
        this.banner = banner;
    }

}
