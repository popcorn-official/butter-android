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

package butter.droid.base.providers;

import android.os.AsyncTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.meta.MetaProvider;
import butter.droid.base.providers.subs.SubsProvider;

/**
 * BaseProvider.java
 * <p/>
 * Base class for providers, has code to enqueue network requests to the OkHttpClient
 */
public abstract class BaseProvider {

    private final OkHttpClient client;
    protected ObjectMapper mapper;
    protected Call mCurrentCall;

    public BaseProvider(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    private OkHttpClient getClient() {
        return client;
    }

    /**
     * Enqueue request with callback
     *
     * @param request         Request
     * @param requestCallback Callback
     * @return Call
     */
    protected Call enqueue(Request request, com.squareup.okhttp.Callback requestCallback) {
        Call mCurrentCall = getClient().newCall(request);
        if (requestCallback != null) mCurrentCall.enqueue(requestCallback);
        return mCurrentCall;
    }

    public void cancel(final String tag) {
        // Cancel in asynctask to prevent networkOnMainThreadException but make it blocking to prevent network calls to be made and then immediately cancelled.
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    getClient().cancel(tag);
                    getClient().cancel(MetaProvider.META_CALL);
                    getClient().cancel(SubsProvider.SUBS_CALL);
                    return null;
                }
            }.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        cancel(MediaProvider.MEDIA_CALL_TAG);
    }

    /**
     * Build URL encoded query
     *
     * @param valuePairs List with key-value items
     * @return Query string
     */
    protected String buildQuery(List<AbstractMap.SimpleEntry<String, String>> valuePairs) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            for (int i = 0; i < valuePairs.size(); i++) {
                AbstractMap.SimpleEntry<String, String> pair = valuePairs.get(i);
                stringBuilder.append(URLEncoder.encode(pair.getKey(), "utf-8"));
                stringBuilder.append("=");
                stringBuilder.append(URLEncoder.encode(pair.getValue(), "utf-8"));
                if (i + 1 != valuePairs.size()) stringBuilder.append("&");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        return stringBuilder.toString();
    }
}
