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

package butter.droid.base.providers.subs;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Movie;
import de.timroes.axmlrpc.XMLRPCCallback;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLRPCServerException;

public class OpenSubsProvider extends SubsProvider {

    private String mApiUrl = "http://api.opensubtitles.org/xml-rpc";
    private String mUserAgent = "Popcorn Time v1";//"Popcorn Time Android v1";

    public OpenSubsProvider(Context context, OkHttpClient client, ObjectMapper mapper) {
        super(context, client, mapper);
    }

    @Override
    public void getList(Movie movie, Callback callback) {
        // Movie subtitles not supported
        callback.onFailure(new NoSuchMethodException("Movie subtitles not supported"));
    }

    @Override
    public void getList(final Episode episode, final Callback callback) {
        login(new XMLRPCCallback() {
            @Override
            public void onResponse(long id, Object result) {
                Map<String, Object> response = (Map<String, Object>) result;
                String token = (String) response.get("token");

                final String episodeStr = Integer.toString(episode.episode);
                final String seasonStr = Integer.toString(episode.season);

                if (!token.isEmpty()) {
                    search(episode, token, new XMLRPCCallback() {
                        @Override
                        public void onResponse(long id, Object result) {
                            Map<String, Map<String, String>> returnMap = new HashMap<>();
                            Map<String, Integer[]> scoreMap = new HashMap<>();
                            Map<String, String> episodeMap = new HashMap<>();
                            Map<String, Object> subData = (Map<String, Object>) result;
                            if (subData != null && subData.get("data") != null && subData.get("data") instanceof Object[]) {
                                Object[] dataList = (Object[]) subData.get("data");
                                for (Object dataItem : dataList) {
                                    Map<String, String> item = (Map<String, String>) dataItem;
                                    if (!item.get("SubFormat").equals("srt")) {
                                        continue;
                                    }

                                    // episode check
                                    if (Integer.parseInt(item.get("SeriesIMDBParent")) != Integer.parseInt(episode.imdbId.replace("tt", ""))) {
                                        continue;
                                    }
                                    if (!item.get("SeriesSeason").equals(seasonStr)) {
                                        continue;
                                    }
                                    if (!item.get("SeriesEpisode").equals(episodeStr)) {
                                        continue;
                                    }

                                    String url = item.get("SubDownloadLink").replace(".gz", ".srt");
                                    String lang = item.get("ISO639").replace("pb", "pt-br");
                                    int downloads = Integer.parseInt(item.get("SubDownloadsCnt"));
                                    int score = 0;

                                    if (item.get("MatchedBy").equals("tag")) {
                                        score += 50;
                                    }
                                    if (item.get("UserRank").equals("trusted")) {
                                        score += 100;
                                    }
                                    if (!episodeMap.containsKey(lang)) {
                                        episodeMap.put(lang, url);
                                        scoreMap.put(lang, new Integer[]{score, downloads});
                                    } else if (score > scoreMap.get(lang)[0] || (score == scoreMap.get(lang)[0] && downloads > scoreMap.get(lang)[1])) {
                                        episodeMap.put(lang, url);
                                        scoreMap.put(lang, new Integer[]{score, downloads});
                                    }
                                }
                                returnMap.put(episode.videoId, episodeMap);
                                callback.onSuccess(returnMap.get(episode.videoId));
                            } else {
                                callback.onFailure(new XMLRPCException("No subs found"));
                            }
                        }

                        @Override
                        public void onError(long id, XMLRPCException error) {
                            callback.onFailure(error);
                        }

                        @Override
                        public void onServerError(long id, XMLRPCServerException error) {
                            callback.onFailure(error);
                        }
                    });
                } else {
                    callback.onFailure(new XMLRPCException("Token not correct"));
                }
            }

            @Override
            public void onError(long id, XMLRPCException error) {
                callback.onFailure(error);
            }

            @Override
            public void onServerError(long id, XMLRPCServerException error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Login to server and get token
     *
     * @param callback XML RPC callback
     */
    private void login(XMLRPCCallback callback) {
        try {
            XMLRPCClient client = new XMLRPCClient(new URL(mApiUrl.replace("http://", "https://")), mUserAgent);
            client.callAsync(callback, "LogIn", "", "", "en", mUserAgent);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            // Just catch and fail
        }
    }

    /**
     * @param episode  Episode
     * @param token    Login token
     * @param callback XML RPC callback callback
     */
    private void search(Episode episode, String token, XMLRPCCallback callback) {
        try {
            XMLRPCClient client = new XMLRPCClient(new URL(mApiUrl), mUserAgent);
            Map<String, String> option = new HashMap<>();
            option.put("imdbid", episode.imdbId.replace("tt", ""));
            option.put("season", String.format(Locale.US, "%d", episode.season));
            option.put("episode", String.format(Locale.US, "%d", episode.episode));
            option.put("sublanguageid", "all");
            client.callAsync(callback, "SearchSubtitles", token, new Object[]{option});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

}