package butter.droid.base.providers.media.response.models.anime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.response.models.ResponseItem;
import butter.droid.base.providers.media.response.models.common.Images;
import butter.droid.base.providers.media.response.models.common.Rating;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Anime extends ResponseItem {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("mal_id")
    private String malId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("year")
    private String year;
    @JsonProperty("slug")
    private String slug;
    @JsonProperty("type")
    private String type;
    @JsonProperty("num_seasons")
    private int numSeasons;
    @JsonProperty("genres")
    private List<String> genres = new ArrayList<>();
    @JsonProperty("images")
    private Images images;
    @JsonProperty("rating")
    private Rating rating;


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
     * @return The malId
     */
    @JsonProperty("mal_id")
    public String getMalId() {
        return malId;
    }

    /**
     * @param malId The mal_id
     */
    @JsonProperty("mal_id")
    public void setMalId(String malId) {
        this.malId = malId;
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
     * @return The type
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * @param type The type
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
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
}
