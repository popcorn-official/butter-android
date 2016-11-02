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

package butter.droid.base.beaming.server;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.HttpDate;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import butter.droid.base.ButterApplication;
import butter.droid.base.subs.FormatSRT;
import butter.droid.base.subs.FormatVTT;
import butter.droid.base.subs.TimedTextObject;
import butter.droid.base.utils.FileUtils;
import timber.log.Timber;

public class BeamServer {

    public static final FileType
            MP4 = new FileType("mp4", "video/mp4", "DLNA.ORG_PN=AVC_MP4_BL_L3L_SD_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            AVI = new FileType("avi", "video/x-msvideo", "DLNA.ORG_PN=AVC_MP4_BL_L3L_SD_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            MKV = new FileType("mkv", "video/x-matroska", "DLNA.ORG_PN=AVC_MKV_MP_HD_AC3;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000", "Streaming"),
            SRT = new FileType("srt", "application/x-subrip", "*", ""),
            VTT = new FileType("vtt", "text/vtt", "*", "");
    private static FileType[] FILE_TYPES = {MP4, AVI, MKV};
    private static FileType[] SUB_TYPES = {SRT, VTT};
    private static HashMap<String, FileType> EXTENSIONS, CONTENT_TYPES;
    private static String sHost;
    private static Integer sPort;
    private static File sCurrentVideo = null, sCurrentSubs = null;
    private AsyncServer mAsyncServer = new AsyncServer();
    private AsyncHttpServer mHttpServer;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    static {
        EXTENSIONS = new HashMap<>();
        CONTENT_TYPES = new HashMap<>();
        for (FileType localFileType : FILE_TYPES) {
            EXTENSIONS.put(localFileType.extension, localFileType);
            CONTENT_TYPES.put(localFileType.mimeType, localFileType);
        }
        CONTENT_TYPES.put("video/mpeg4", MP4);
        EXTENSIONS.put("3gp", MP4);
        EXTENSIONS.put("mov", MP4);
    }

    public BeamServer(String host, int port) {
        mHttpServer = new AsyncHttpServer() {
            protected boolean onRequest(AsyncHttpServerRequest httpServerRequest, AsyncHttpServerResponse httpServerResponse) {
                Timber.d(httpServerRequest.toString());
                httpServerResponse.getHeaders().set("Access-Control-Allow-Origin", "*");
                return super.onRequest(httpServerRequest, httpServerResponse);
            }
        };

        sHost = host;
        sPort = port;

        for (FileType localFileType : FILE_TYPES) {
            VideoFileReponse localVideoFileReponse = new VideoFileReponse(localFileType);
            mHttpServer.get("/video." + localFileType.extension, localVideoFileReponse);
            mHttpServer.addAction("HEAD", "/video." + localFileType.extension, localVideoFileReponse);
        }

        for(FileType localSubsFileType : SUB_TYPES) {
            SubtitleFileResponse localSubsFileReponse = new SubtitleFileResponse(localSubsFileType);
            mHttpServer.get("/video." + localSubsFileType.extension, localSubsFileReponse);
            mHttpServer.addAction("HEAD", "/video." + localSubsFileType.extension, localSubsFileReponse);
        }

        mHttpServer.get("/(.*?)", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("Not found");
                response.code(404);
            }
        });
    }

    public static void setCurrentVideo(File file) {
        sCurrentVideo = file;
    }

    public static void setCurrentSubs(File file) {
        if(file == null) {
            sCurrentSubs = null;
            return;
        }

        sCurrentSubs = new File(file.getAbsolutePath().replace(".srt", "").replace(".vtt", ""));
        File vttFile = new File(sCurrentSubs.getAbsolutePath() + ".vtt");
        File srtFile = new File(sCurrentSubs.getAbsolutePath() + ".srt");

        try {
            if (FileUtils.getFileExtension(file.getName()).equals("srt") && !vttFile.exists() && srtFile.exists()) {
                FormatSRT srt = new FormatSRT();
                TimedTextObject timedTextObject = srt.parseFile(file.getName(), FileUtils.getContentsAsString(file.getAbsolutePath()));
                String[] vttStr = timedTextObject.toVTT();
                FileUtils.saveStringFile(vttStr, vttFile);
            } else if (FileUtils.getFileExtension(file.getName()).equals("vtt") && !srtFile.exists() && vttFile.exists()) {
                FormatVTT vtt = new FormatVTT();
                TimedTextObject timedTextObject = vtt.parseFile(file.getName(), FileUtils.getContentsAsString(file.getAbsolutePath()));
                String[] srtStr = timedTextObject.toSRT();
                FileUtils.saveStringFile(srtStr, srtFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setCurrentVideo(String file) {
        setCurrentVideo(new File(file));
    }

    public static void setCurrentSubs(String file) {
        setCurrentSubs(new File(file));
    }

    public static void removeSubs() {
        sCurrentSubs = null;
    }

    public static String getHost() {
        return "http://" + sHost + ":" + sPort;
    }

    public static String getVideoURL() {
        return "http://" + sHost + ":" + sPort + "/video.mp4";
    }

    public static String getSubsURL() {
        return "http://" + sHost + ":" + sPort + "/video." + SRT.extension;
    }

    public static String getSubsURL(FileType fileType) {
        return "http://" + sHost + ":" + sPort + "/video." + fileType.extension;
    }

    public void start() {
        mHttpServer.listen(mAsyncServer, sPort);

        PowerManager powerManager = (PowerManager) ButterApplication.getAppContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CastingServer");
        mWakeLock.acquire();
        WifiManager wifiManager = (WifiManager) ButterApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "CastingServer");
        mWifiLock.acquire();
    }

    public void stop() {
        if(mHttpServer != null)
            mHttpServer.stop();
        AsyncServer.getDefault().stop();
        if(mAsyncServer != null)
            mAsyncServer.stop();

        if(mWifiLock != null && mWifiLock.isHeld())
        mWifiLock.release();
        if(mWakeLock != null && mWakeLock.isHeld())
        mWakeLock.release();
    }

    public static class FileType {
        public final String dlnaContentFeatures;
        public final String dlnaTransferMode;
        public final String extension;
        public final String mimeType;

        public FileType(String extension, String mimeType, String dlnaContentFeatures, String dlnaTransferMode) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.dlnaContentFeatures = dlnaContentFeatures;
            this.dlnaTransferMode = dlnaTransferMode;
        }

        public final boolean isImage() {
            return this.mimeType.startsWith("image/");
        }

        public boolean isVideo() {
            return this.mimeType.startsWith("video/");
        }

        public void setHeaders(Headers paramHeaders) {
            paramHeaders.set("contentFeatures.dlna.org", this.dlnaContentFeatures);
            paramHeaders.set("TransferMode.DLNA.ORG", this.dlnaTransferMode);
            paramHeaders.set("DAAP-Server", "iTunes/11.0.5 (OS X)");
            paramHeaders.set("Date", HttpDate.format(new Date()));
            paramHeaders.set("Last-Modified", "2015-01-01T10:00:00Z");
            paramHeaders.set("Content-Type", this.mimeType);
            paramHeaders.set("CaptionInfo.sec", getSubsURL(SRT));
        }

        public void setHeaders(AsyncHttpServerResponse httpServerResponse) {
            setHeaders(httpServerResponse.getHeaders());
        }
    }

    static class VideoFileReponse implements HttpServerRequestCallback {
        FileType mFileType;

        public VideoFileReponse(FileType fileType) {
            mFileType = fileType;
        }

        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse httpServerResponse) {
            if (sCurrentVideo != null && sCurrentVideo.exists()) {
                mFileType.setHeaders(httpServerResponse);
                httpServerResponse.sendFile(sCurrentVideo);
            } else {
                httpServerResponse.send("Not found");
                httpServerResponse.code(404);
            }

            Timber.i(httpServerResponse.toString());
        }
    }

    static class SubtitleFileResponse implements HttpServerRequestCallback {
        FileType mFileType;

        public SubtitleFileResponse(FileType fileType) {
            mFileType = fileType;
        }

        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse httpServerResponse) {
            File file = new File(sCurrentSubs.getAbsolutePath() + "." + mFileType.extension);
            if (sCurrentSubs != null && file.exists()) {
                mFileType.setHeaders(httpServerResponse);
                httpServerResponse.sendFile(file);
            } else {
                httpServerResponse.send("Not found");
                httpServerResponse.code(404);
            }

            Timber.i(httpServerResponse.toString());
        }
    }
}
