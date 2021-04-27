package butter.droid.base.providers.media.response.models.movies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class Quality extends butter.droid.base.providers.media.response.models.common.Quality {

    @JsonProperty("filesize")
    private String filesize;
    @JsonProperty("size")
    private long size;

    /**
     * @return The filesize
     */
    public String getFilesize() {
        return filesize;
    }

    /**
     * @param filesize The filesize
     */
    public void setFilesize(String filesize) {
        this.filesize = filesize;
    }

    /**
     * @return The size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size The size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return The peer
     */
    @Override
    @JsonProperty("peer")
    public int getPeers() {
        return super.getPeers();
    }

    /**
     * @param peers The peer
     */
    @Override
    @JsonProperty("peer")
    public void setPeers(int peers) {
        super.setPeers(peers);
    }

    /**
     * @return The seed
     */
    @Override
    @JsonProperty("seed")
    public int getSeeds() {
        return super.getSeeds();
    }

    /**
     * @param seeds The seed
     */
    @Override
    @JsonProperty("seed")
    public void setSeeds(int seeds) {
        super.setSeeds(seeds);
    }

}
