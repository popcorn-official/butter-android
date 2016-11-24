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
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butter.droid.base.providers.media.models.Episode;
import butter.droid.base.providers.media.models.Movie;

public class YSubsProvider extends SubsProvider {

    private static final String API_URL = "http://api.yifysubtitles.com/subs/";
    private static final String MIRROR_URL = "http://api.ysubs.com/subs/";
    private static final String PREFIX = "http://www.yifysubtitles.com/";
    private static final HashMap<String, String> LANGUAGE_MAPPING = new HashMap<>();

    static {
        LANGUAGE_MAPPING.put("albanian", "sq");
        LANGUAGE_MAPPING.put("arabic", "ar");
        LANGUAGE_MAPPING.put("bengali", "bn");
        LANGUAGE_MAPPING.put("brazilian-portuguese", "pt-br");
        LANGUAGE_MAPPING.put("bulgarian", "bg");
        LANGUAGE_MAPPING.put("bosnian", "bs");
        LANGUAGE_MAPPING.put("chinese", "zh");
        LANGUAGE_MAPPING.put("croatian", "hr");
        LANGUAGE_MAPPING.put("czech", "cs");
        LANGUAGE_MAPPING.put("danish", "da");
        LANGUAGE_MAPPING.put("dutch", "nl");
        LANGUAGE_MAPPING.put("english", "en");
        LANGUAGE_MAPPING.put("estonian", "et");
        LANGUAGE_MAPPING.put("farsi-persian", "fa");
        LANGUAGE_MAPPING.put("finnish", "fi");
        LANGUAGE_MAPPING.put("french", "fr");
        LANGUAGE_MAPPING.put("german", "de");
        LANGUAGE_MAPPING.put("greek", "el");
        LANGUAGE_MAPPING.put("hebrew", "he");
        LANGUAGE_MAPPING.put("hungarian", "hu");
        LANGUAGE_MAPPING.put("indonesian", "id");
        LANGUAGE_MAPPING.put("italian", "it");
        LANGUAGE_MAPPING.put("japanese", "ja");
        LANGUAGE_MAPPING.put("korean", "ko");
        LANGUAGE_MAPPING.put("lithuanian", "lt");
        LANGUAGE_MAPPING.put("macedonian", "mk");
        LANGUAGE_MAPPING.put("malay", "ms");
        LANGUAGE_MAPPING.put("norwegian", "no");
        LANGUAGE_MAPPING.put("polish", "pl");
        LANGUAGE_MAPPING.put("portuguese", "pt");
        LANGUAGE_MAPPING.put("romanian", "ro");
        LANGUAGE_MAPPING.put("russian", "ru");
        LANGUAGE_MAPPING.put("serbian", "sr");
        LANGUAGE_MAPPING.put("slovenian", "sl");
        LANGUAGE_MAPPING.put("spanish", "es");
        LANGUAGE_MAPPING.put("swedish", "sv");
        LANGUAGE_MAPPING.put("thai", "th");
        LANGUAGE_MAPPING.put("turkish", "tr");
        LANGUAGE_MAPPING.put("urdu", "ur");
        LANGUAGE_MAPPING.put("ukrainian", "uk");
        LANGUAGE_MAPPING.put("vietnamese", "vi");
    }

    public YSubsProvider(Context context, OkHttpClient client, ObjectMapper mapper) {
        super(context, client, mapper);
    }

    @Override
    public void getList(final Movie media, final Callback callback) {
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(API_URL + media.imdbId);
        requestBuilder.tag(SUBS_CALL);

        fetch(requestBuilder, media, new Callback() {
            @Override
            public void onSuccess(Map<String, String> items) {
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Exception e) {
                requestBuilder.url(MIRROR_URL + media.imdbId);
                fetch(requestBuilder, media, callback);
            }
        });
    }

    @Override
    public void getList(Episode episode, Callback callback) {
        // Show subtitles not supported

        callback.onFailure(new NoSuchMethodException("Show subtitles not supported"));
    }

    private void fetch(Request.Builder requestBuilder, final Movie media, final Callback callback) {
        enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    YSubsResponse result = mapper.readValue(responseStr, YSubsResponse.class);
                    callback.onSuccess(result.formatForPopcorn(PREFIX, LANGUAGE_MAPPING).get(media.imdbId));
                }
            }
        });
    }

    // TODO move to responses equals to other providers
    private class YSubsResponse {
        public boolean success;
        public int subtitles;
        public HashMap<String, HashMap<String, ArrayList<HashMap<String, Object>>>> subs;

        public Map<String, Map<String, String>> formatForPopcorn(String prefix, HashMap<String, String> mapping) {
            Map<String, Map<String, String>> returnMap = new HashMap<>();
            if (success && subs != null) {
                String[] imdbIds = getKeys(subs);
                for (String imdbId : imdbIds) {
                    HashMap<String, String> imdbMap = new HashMap<>();
                    HashMap<String, ArrayList<HashMap<String, Object>>> langMap = subs.get(imdbId);
                    String[] langs = getKeys(langMap);
                    for (String lang : langs) {
                        if (langMap.get(lang).size() <= 0) continue;
                        ArrayList<HashMap<String, Object>> subMap = langMap.get(lang);
                        int currentRating = -1;
                        String currentSub = "";
                        for (HashMap<String, Object> sub : subMap) {
                            int itemRating = ((Double) sub.get("rating")).intValue();
                            if (currentRating < itemRating) {
                                currentSub = prefix + sub.get("url");
                                currentRating = itemRating;
                            }
                        }
                        imdbMap.put(mapLanguage(lang, mapping), currentSub);
                    }
                    returnMap.put(imdbId, imdbMap);
                }
            }
            return returnMap;
        }

        private String[] getKeys(HashMap<String, ?> map) {
            if (map != null && map.size() > 0) {
                return map.keySet().toArray(new String[map.size()]);
            }
            return new String[0];
        }

        private String mapLanguage(String input, HashMap<String, String> mapping) {
            if (mapping.containsKey(input)) {
                return mapping.get(input);
            }
            return SUBTITLE_LANGUAGE_NONE;
        }
    }

}
