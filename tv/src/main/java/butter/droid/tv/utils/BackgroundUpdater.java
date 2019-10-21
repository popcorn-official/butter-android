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

package butter.droid.tv.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.leanback.app.BackgroundManager;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.Timer;
import java.util.TimerTask;

import butter.droid.base.utils.PixelUtils;
import butter.droid.base.utils.ThreadUtils;

public class BackgroundUpdater {


    private int mDisplayWidth;
    private int mDisplayHeight;
    private static int BACKGROUND_UPDATE_DELAY = 300;
    private int mDefaultBackground;
    private Context mContext;
    private Target mBackgroundImageTarget;
    private Timer mBackgroundTimer;
    private BackgroundManager mBackgroundManager;
    private String mBackgroundUrl;

    public void initialise(Activity activity, @DrawableRes int defaultBackground) {
        mContext = activity.getApplicationContext();

        mBackgroundManager = BackgroundManager.getInstance(activity);
        mBackgroundManager.attach(activity.getWindow());
        mBackgroundImageTarget = new PicassoBackgroundManagerTarget(mBackgroundManager);
        mDefaultBackground = defaultBackground;
        mDisplayWidth = PixelUtils.getScreenWidth(mContext);
        mDisplayHeight = PixelUtils.getScreenWidth(mContext);
    }

    public BackgroundUpdater() {
    }


    /**
     * Updates the background asynchronously with the given image url, after a short delay.
     *
     * @param url
     */
    public void updateBackgroundAsync(String url) {
        mBackgroundUrl = url;
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }


    public void updateBackground(final String uri) {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }

        //load default background image
        if (null == uri) {
            Picasso.with(mContext).load(mDefaultBackground).into(mBackgroundImageTarget);
            return;
        }

        //load actual background image
        Picasso.with(mContext)
                .load(uri)
                .error(mDefaultBackground)
                .into(mBackgroundImageTarget);
    }

//    protected void setDefaultBackground(Drawable background) {
//        mDefaultBackground = background;
//    }


    protected void setDefaultBackground(int resourceId) {
        mDefaultBackground = resourceId;
    }

    /**
     * Updates the background immediately with a drawable
     *
     * @param drawable
     */
    public void updateBackground(Drawable drawable) {
        mBackgroundManager.setDrawable(drawable);
    }

    /**
     * Clears the background immediately
     */
    public void clearBackground() {
        mBackgroundManager.setThemeDrawableResourceId(mDefaultBackground);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundUrl != null) {
                        updateBackground(mBackgroundUrl.toString());
                    }

                }
            });
        }
    }

    public void destroy() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }

        Picasso.with(mContext).cancelRequest(mBackgroundImageTarget);
        mBackgroundManager.release();
    }
}
