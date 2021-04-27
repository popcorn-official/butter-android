package butter.droid.base.providers.media.response.models.shows;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import butter.droid.base.providers.media.response.models.ResponseItem;
import butter.droid.base.providers.media.response.models.common.Images;
import butter.droid.base.providers.media.response.models.common.Rating;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Show extends ResponseItem {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("imdb_id")
    private String imdbId;
    @JsonProperty("tvdb_id")
    private String tvdbId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("year")
    private String year;
    @JsonProperty("slug")
    private String slug;
    @JsonProperty("rating")
    private Rating rating;
    @JsonProperty("num_seasons")
    private int numSeasons;
    @JsonProperty("images")
    private Images images;

    /**
     * @return The id
     */
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    /**
     * @param id The _id
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The imdbId
     */
    @JsonProperty("imdb_id")
    public String getImdbId() {
        return imdbId;
    }

    /**
     * @param imdbId The imdb_id
     */
    @JsonProperty("imdb_id")
    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

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
     * @return The year
     */
    @JsonProperty("year")
    public String getYear() {
        return year;
    }

    /**
     * @param year The year
     */
    @JsonProperty("year")
    public void setYear(String year) {
        this.year = year;
    }

    /**
     * @return The slug
     */
    @JsonProperty("slug")
    public String getSlug() {
        return slug;
    }

    /**
     * @param slug The slug
     */
    @JsonProperty("slug")
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * @return The rating
     */
    @JsonProperty("rating")
    public Rating getRating() {
        return rating;
    }

    /**
     * @param rating The rating
     */
    @JsonProperty("rating")
    public void setRating(Rating rating) {
        this.rating = rating;
    }

    /**
     * @return The numSeasons
     */
    @JsonProperty("num_seasons")
    public int getNumSeasons() {
        return numSeasons;
    }

    /**
     * @param numSeasons The num_seasons
     */
    @JsonProperty("num_seasons")
    public void setNumSeasons(int numSeasons) {
        this.numSeasons = numSeasons;
    }

    /**
     * @return The images
     */
    @JsonProperty("images")
    public Images getImages() {
        return images;
    }

    /**
     * @param images The images
     */
    @JsonProperty("images")
    public void setImages(Images images) {
        this.images = images;
    }
}
