package butter.droid.base.providers.media.response.models.movies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.response.models.ResponseItem;
import butter.droid.base.providers.media.response.models.common.Images;
import butter.droid.base.providers.media.response.models.common.Rating;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Movie extends ResponseItem {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("imdb_id")
    private String imdbId;
    @JsonProperty("title")
    private String title;
    @JsonProperty("year")
    private String year;
    @JsonProperty("synopsis")
    private String synopsis;
    @JsonProperty("runtime")
    private String runtime;
    @JsonProperty("released")
    private int released;
    @JsonProperty("trailer")
    private String trailer;
    @JsonProperty("certification")
    private String certification;
    @JsonProperty("torrents")
    private Torrents torrents;
    @JsonProperty("genres")
    private List<String> genres = new ArrayList<>();
    @JsonProperty("images")
    private Images images;
    @JsonProperty("rating")
    private Rating rating;

    public Movie() {

    }

    /**
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The _id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The imdbId
     */
    public String getImdbId() {
        return imdbId;
    }

    /**
     * @param imdbId The imdb_id
     */
    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    /**
     * @return The title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The year
     */
    public String getYear() {
        return year;
    }

    /**
     * @param year The year
     */
    public void setYear(String year) {
        this.year = year;
    }

    /**
     * @return The synopsis
     */
    public String getSynopsis() {
        return synopsis;
    }

    /**
     * @param synopsis The synopsis
     */
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    /**
     * @return The runtime
     */
    public String getRuntime() {
        return runtime;
    }

    /**
     * @param runtime The runtime
     */
    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    /**
     * @return The released
     */
    public int getReleased() {
        return released;
    }

    /**
     * @param released The released
     */
    public void setReleased(int released) {
        this.released = released;
    }

    /**
     * @return The trailer
     */
    public String getTrailer() {
        return trailer;
    }

    /**
     * @param trailer The trailer
     */
    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    /**
     * @return The certification
     */
    public String getCertification() {
        return certification;
    }

    /**
     * @param certification The certification
     */
    public void setCertification(String certification) {
        this.certification = certification;
    }

    /**
     * @return The torrents
     */
    public Torrents getTorrents() {
        return torrents;
    }

    /**
     * @param torrents The torrents
     */
    public void setTorrents(Torrents torrents) {
        this.torrents = torrents;
    }

    /**
     * @return The genres
     */
    public List<String> getGenres() {
        return genres;
    }

    /**
     * @param genres The genres
     */
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    /**
     * @return The images
     */
    public Images getImages() {
        return images;
    }

    /**
     * @param images The images
     */
    public void setImages(Images images) {
        this.images = images;
    }

    /**
     * @return The rating
     */
    public Rating getRating() {
        return rating;
    }

    /**
     * @param rating The rating
     */
    public void setRating(Rating rating) {
        this.rating = rating;
    }

}
