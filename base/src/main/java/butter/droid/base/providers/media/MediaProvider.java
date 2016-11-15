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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.R;
import butter.droid.base.providers.BaseProvider;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import timber.log.Timber;

/**
 * MediaProvider.java
 * <p/>
 * Base class for all media providers. Any media providers has to extend this class and use the callback defined here.
 */
public abstract class MediaProvider extends BaseProvider implements Parcelable {

    public static final String MEDIA_CALL = "media_http_call";
    public static final int DEFAULT_NAVIGATION_INDEX = 1;
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MediaProvider> CREATOR = new Parcelable.Creator<MediaProvider>() {
        @Override
        public MediaProvider createFromParcel(Parcel in) {
            String className = in.readString();
            MediaProvider provider = null;
            try {
                Class<?> clazz = Class.forName(className);
                provider = (MediaProvider) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return provider;
        }

        @Override
        public MediaProvider[] newArray(int size) {
            return null;
        }
    };
    private String[] apiUrls = new String[0];
    private String itemsPath = "";
    private String itemDetailsPath = "";
    private Integer currentApi = 0;

    public MediaProvider(String[] apiUrls, String itemsPath, String itemDetailsPath, Integer currentApi) {
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
    public Call getList(Filters filters, Callback callback) {
        return getList(null, filters, callback);
    }

    /**
     * Get a list of Media items from the provider
     *
     * @param existingList Input the current list so it can be extended
     * @param filters      Filters the provider can use to sort or search
     * @param callback     MediaProvider callback
     * @return Call
     */
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
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
        requestBuilder.tag(MEDIA_CALL);

        Timber.d(this.getClass().getSimpleName(), "Making request to: " + url);

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from API
     *
     * @param currentList    Current shown list to be extended
     * @param requestBuilder Request to be executed
     * @param callback       Network callback
     * @return Call
     */
    private Call fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                String url = requestBuilder.build().urlString();
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
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {

                        String responseStr = response.body().string();

                        if (responseStr.isEmpty()) {
                            callback.onFailure(new NetworkErrorException("Empty response"));
                        }
                        int actualSize = currentList.size();
                        ArrayList<Media> responseItems = getResponseFormattedList(responseStr, currentList);
                        callback.onSuccess(filters, responseItems, responseItems.size() > actualSize);
                        return;
                    }
                } catch (Exception e) {
                    callback.onFailure(e);
                }
                callback.onFailure(new NetworkErrorException("Couldn't connect to API"));
            }
        });
    }

    public Call getDetail(ArrayList<Media> currentList, Integer index, final Callback callback) {
        Request.Builder requestBuilder = new Request.Builder();
        String url = apiUrls[currentApi] + itemDetailsPath + currentList.get(index).videoId;
        requestBuilder.url(url);
        requestBuilder.tag(MEDIA_CALL);

        Timber.d(this.getClass().getSimpleName(), "Making request to: " + url);

        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {

                        String responseStr = response.body().string();

                        if (responseStr.isEmpty()) {
                            callback.onFailure(new NetworkErrorException("Empty response"));
                        }

                        ArrayList<Media> formattedData = getResponseDetailsFormattedList(responseStr);
                        if (formattedData.size() > 0) {
                            callback.onSuccess(null, formattedData, true);
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
        return 1;
    }

    public List<Genre> getGenres() {
        return new ArrayList<>();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String className = getClass().getCanonicalName();
        dest.writeString(className);
    }

    public interface Callback {
        void onSuccess(Filters filters, ArrayList<Media> items, boolean changed);

        void onFailure(Exception e);
    }

    public static class Filters {
        public String keywords = null;
        public String genre = null;
        public Order order = Order.DESC;
        public Sort sort = Sort.POPULARITY;
        public Integer page = null;
        public String langCode = "en";

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

        public enum Order {ASC, DESC}

        public enum Sort {POPULARITY, YEAR, DATE, RATING, ALPHABET, TRENDING}
    }

    public static class NavInfo {
        private final Integer mIconId;
        private int mId;
        private Filters.Sort mSort;
        private Filters.Order mDefOrder;
        private String mLabel;

        NavInfo(int id, Filters.Sort sort, Filters.Order defOrder, String label, @Nullable @DrawableRes int icon) {
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
