/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.media;

import android.accounts.NetworkErrorException;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.R;
import butter.droid.base.providers.BaseProvider;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.subs.SubsProvider;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

/**
 * MediaProvider.java
 * <p/>
 * Base class for all media providers. Any media providers has to extend this class and use the callback defined here.
 */
public abstract class MediaProvider extends BaseProvider {

    @Nullable
    private final SubsProvider subsProvider;

    private static final int DEFAULT_NAVIGATION_INDEX = 1;
    private String[] apiUrls = new String[0];
    private String itemsPath = "";
    private String itemDetailsPath = "";
    private Integer currentApi = 0;

    public MediaProvider(OkHttpClient client, ObjectMapper mapper, @Nullable SubsProvider subsProvider, String[] apiUrls, String itemsPath, String itemDetailsPath, Integer currentApi) {
        super(client, mapper);
        this.subsProvider = subsProvider;
        this.apiUrls = apiUrls;
        this.itemsPath = itemsPath;
        this.itemDetailsPath = itemDetailsPath;
        this.currentApi = currentApi;
    }

    /**
     * Get a list of Media items from the provider
     *
     * @param filters  Filters the provider can use to sort or search
     * @param callback MediaProvider callback
     */
    public void getList(Filters filters, Callback callback) {
        getList(null, filters, callback);
    }

    /**
     * Get a list of Media items from the provider
     *
     * @param existingList Input the current list so it can be extended
     * @param filters      Filters the provider can use to sort or search
     * @param callback     MediaProvider callback
     */
    public void getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = new ArrayList<>(existingList);
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
        params.add(new AbstractMap.SimpleEntry<>("limit", "30"));

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new AbstractMap.SimpleEntry<>("keywords", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new AbstractMap.SimpleEntry<>("genre", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new AbstractMap.SimpleEntry<>("order", "1"));
        } else {
            params.add(new AbstractMap.SimpleEntry<>("order", "-1"));
        }

        if (filters.langCode != null) {
            params.add(new AbstractMap.SimpleEntry<>("lang", filters.langCode));
        }

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "popularity";
                break;
            case YEAR:
                sort = "year";
                break;
            case DATE:
                sort = "last added";
                break;
            case RATING:
                sort = "rating";
                break;
            case ALPHABET:
                sort = "name";
                break;
            case TRENDING:
                sort = "trending";
                break;
        }

        params.add(new AbstractMap.SimpleEntry<>("sort", sort));

        String url = apiUrls[currentApi] + itemsPath;
        if (filters.page != null) {
            url += filters.page;
        } else {
            url += "1";
        }

        Request.Builder requestBuilder = new Request.Builder();
        String query = buildQuery(params);
        url = url + "?" + query;
        requestBuilder.url(url);

        Timber.d(this.getClass().getSimpleName(), "Making request to: " + url);

        fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from API
     *
     * @param currentList    Current shown list to be extended
     * @param requestBuilder Request to be executed
     * @param callback       Network callback
     */
    private void fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        enqueue(requestBuilder.build(), new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String url = requestBuilder.build().url().toString();
                if (currentApi >= apiUrls.length - 1) {
                    callback.onFailure(e);
                } else {
                    if (url.contains(apiUrls[currentApi])) {
                        url = url.replace(apiUrls[currentApi], apiUrls[currentApi + 1]);
                        currentApi++;
                    } else {
                        url = url.replace(apiUrls[currentApi - 1], apiUrls[currentApi]);
                    }
                    requestBuilder.url(url);
                    fetchList(currentList, requestBuilder, filters, callback);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {

                    String responseStr = response.body().string();

                    if (responseStr.isEmpty()) {
                        onFailure(call, new IOException("Empty response"));
                    }
                    int actualSize = currentList.size();
                    ArrayList<Media> responseItems = getResponseFormattedList(responseStr, currentList);
                    callback.onSuccess(filters, responseItems);
                    return;
                }
                onFailure(call, new IOException("Couldn't connect to API"));
            }
        });
    }

    public void getDetail(ArrayList<Media> currentList, Integer index, final Callback callback) {
        Request.Builder requestBuilder = new Request.Builder();
        String url = apiUrls[currentApi] + itemDetailsPath + currentList.get(index).videoId;
        requestBuilder.url(url);

        Timber.d(this.getClass().getSimpleName(), "Making request to: " + url);

        enqueue(requestBuilder.build(), new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {

                        String responseStr = response.body().string();

                        if (responseStr.isEmpty()) {
                            callback.onFailure(new NetworkErrorException("Empty response"));
                        }

                        ArrayList<Media> formattedData = getResponseDetailsFormattedList(responseStr);
                        if (formattedData.size() > 0) {
                            callback.onSuccess(null, formattedData);
                            return;
                        }
                        callback.onFailure(new IllegalStateException("Empty list"));
                        return;
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
                callback.onFailure(new NetworkErrorException("Couldn't connect to API"));
            }
        });
    }

    public int getLoadingMessage() {
        return R.string.loading;
    }

    public ArrayList<Media> getResponseFormattedList(String responseStr, ArrayList<Media> currentList) throws IOException {
        return new ArrayList<>();
    }

    public ArrayList<Media> getResponseDetailsFormattedList(String responseStr) throws IOException {
        return new ArrayList<>();
    }

    public List<NavInfo> getNavigation() {
        return new ArrayList<>();
    }

    public int getDefaultNavigationIndex() {
        return DEFAULT_NAVIGATION_INDEX;
    }

    public List<Genre> getGenres() {
        return new ArrayList<>();
    }

    @Nullable
    public SubsProvider getSubsProvider() {
        return subsProvider;
    }

    public boolean hasSubsProvider() {
        return subsProvider != null;
    }


    public interface Callback {
        void onSuccess(Filters filters, ArrayList<Media> items);

        void onFailure(Exception e);
    }

    public static class Filters {
        String keywords = null;
        String genre = null;
        Order order = Order.DESC;
        Sort sort = Sort.POPULARITY;
        Integer page = null;
        String langCode = "en";

        public Filters() {
        }

        public Filters(Filters filters) {
            keywords = filters.keywords;
            genre = filters.genre;
            order = filters.order;
            sort = filters.sort;
            page = filters.page;
            langCode = filters.langCode;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public String getLangCode() {
            return langCode;
        }

        public void setLangCode(String langCode) {
            this.langCode = langCode;
        }

        public enum Order {ASC, DESC}

        public enum Sort {POPULARITY, YEAR, DATE, RATING, ALPHABET, TRENDING}
    }

    public static class NavInfo {
        private final Integer mIconId;
        private int mId;
        private Filters.Sort mSort;
        private Filters.Order mDefOrder;
        private String mLabel;

        NavInfo(int id, Filters.Sort sort, Filters.Order defOrder, String label, @Nullable @DrawableRes Integer icon) {
            mId = id;
            mSort = sort;
            mDefOrder = defOrder;
            mLabel = label;
            mIconId = icon;
        }

        public Filters.Sort getFilter() {
            return mSort;
        }

        public int getId() {
            return mId;
        }

        @DrawableRes
        public int getIcon() {
            return mIconId;
        }

        public Filters.Order getOrder() {
            return mDefOrder;
        }

        public String getLabel() {
            return mLabel;
        }
    }

}
