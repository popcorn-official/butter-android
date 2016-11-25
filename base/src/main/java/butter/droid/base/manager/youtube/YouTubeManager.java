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

/**
 * The pct.droid.base.youtube package contains code that is based on https://code.google.com/p/android-youtube-player/
 */

package butter.droid.base.manager.youtube;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butter.droid.base.manager.youtube.model.Format;
import butter.droid.base.manager.youtube.model.VideoStream;

public class YouTubeManager {

    private static final String YOUTUBE_VIDEO_INFORMATION_URL = "http://www.youtube.com/get_video_info?&video_id=";

    private final OkHttpClient client;

    @Inject
    public YouTubeManager(OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    private static int getSupportedFallbackId(int oldId) {
        final int supportedFormatIds[] = {13,  //3GPP (MPEG-4 encoded) Low quality
                17,  //3GPP (MPEG-4 encoded) Medium quality
                18,  //MP4  (H.264 encoded) Normal quality
                22,  //MP4  (H.264 encoded) High quality
                37   //MP4  (H.264 encoded) High quality
        };
        int fallbackId = oldId;
        for (int i = supportedFormatIds.length - 1; i >= 0; i--) {
            if (oldId == supportedFormatIds[i] && i > 0) {
                fallbackId = supportedFormatIds[i - 1];
            }
        }
        return fallbackId;
    }

    public boolean isYouTubeUrl(String youtubeUrl) {
        String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*"; // var regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#\&\?]*).*/;
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(youtubeUrl);
        return matcher.matches();
    }

    public String getYouTubeVideoId(String youtubeUrl) {
        String videoId = "";
        if (youtubeUrl != null && youtubeUrl.trim().length() > 0 && youtubeUrl.startsWith("http")) {
            String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*"; // var regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#\&\?]*).*/;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(youtubeUrl);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(7);
                if (groupIndex1 != null && groupIndex1.length() == 11)
                    videoId = groupIndex1;
            }
        }
        return videoId;
    }

    /**
     * Calculate the YouTube URL to load the video.  Includes retrieving a token that YouTube
     * requires to play the video.
     *
     * @param quality  quality of the video.  17=low, 18=high
     * @param fallback whether to fallback to lower quality in case the supplied quality is not available
     * @param videoId  the id of the video
     * @return the url string that will retrieve the video
     * @throws java.io.IOException
     */
    public String calculateYouTubeUrl(String quality, boolean fallback, String videoId) throws IOException {

        String uriStr = null;

        Request.Builder request = new Request.Builder();
        request.url(YOUTUBE_VIDEO_INFORMATION_URL + videoId);
        Call call = client.newCall(request.build());
        Response response = call.execute();
        if (!response.isSuccessful())
            return null;

        String infoStr = response.body().string();

        String[] args = infoStr.split("&");
        Map<String, String> argMap = new HashMap<>();
        for (String arg : args) {
            String[] valStrArr = arg.split("=");
            if (valStrArr.length >= 2) {
                argMap.put(valStrArr[0], URLDecoder.decode(valStrArr[1]));
            }
        }

        //Find out the URI string from the parameters

        //Populate the list of formats for the video
        String fmtList = URLDecoder.decode(argMap.get("fmt_list"), "utf-8");
        ArrayList<Format> formats = new ArrayList<>();
        if (null != fmtList) {
            String formatStrs[] = fmtList.split(",");

            for (String lFormatStr : formatStrs) {
                Format format = new Format(lFormatStr);
                formats.add(format);
            }
        }

        //Populate the list of streams for the video
        String streamList = argMap.get("url_encoded_fmt_stream_map");
        if (null != streamList) {
            String streamStrs[] = streamList.split(",");
            ArrayList<VideoStream> streams = new ArrayList<>();
            for (String streamStr : streamStrs) {
                VideoStream lStream = new VideoStream(streamStr);
                streams.add(lStream);
            }

            //Search for the given format in the list of video formats
            // if it is there, select the corresponding stream
            // otherwise if fallback is requested, check for next lower format
            int formatId = Integer.parseInt(quality);

            Format searchFormat = new Format(formatId);
            while (!formats.contains(searchFormat) && fallback) {
                int oldId = searchFormat.getId();
                int newId = getSupportedFallbackId(oldId);

                if (oldId == newId) {
                    break;
                }
                searchFormat = new Format(newId);
            }

            int index = formats.indexOf(searchFormat);
            if (index >= 0) {
                VideoStream searchStream = streams.get(index);
                uriStr = searchStream.getUrl();
            }

        }
        //Return the URI string. It may be null if the format (or a fallback format if enabled)
        // is not found in the list of formats for the video
        return uriStr;
    }
}
