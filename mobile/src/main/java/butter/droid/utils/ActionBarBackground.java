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

package butter.droid.utils;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import butter.droid.R;

public class ActionBarBackground {

    private Drawable mOldBackground;
    private AppCompatActivity mActivity;
    private View mToolbar;
    private ActionBar mActionBar;
    private int mNewColor;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public ActionBarBackground(AppCompatActivity actionBarActivity) {
        mNewColor = Color.parseColor("#FFFFFF");
        init(actionBarActivity);
    }

    public ActionBarBackground(AppCompatActivity actionBarActivity, int newColor) {
        mNewColor = newColor;
        init(actionBarActivity);
    }

    private void init(AppCompatActivity actionBarActivity) {
        mActionBar = actionBarActivity.getSupportActionBar();
        mActivity = actionBarActivity;

        getToolbar(actionBarActivity);
        if (mToolbar == null || mToolbar.getBackground() == null) {
            mOldBackground = getColoredBackground(R.color.primary);
        } else {
            mOldBackground = mToolbar.getBackground();
        }
    }

    private Resources getResources() {
        return mActivity.getResources();
    }

    private View getToolbar(AppCompatActivity actionBarActivity) {
        final int toolBarId = getResources().getIdentifier("toolbar", "id", actionBarActivity.getPackageName());
        mToolbar = actionBarActivity.findViewById(toolBarId);
        return mToolbar;
    }

    /**
     * Change color of ActionBar to mNewColor
     *
     * @return Instance of this class
     */
    private ActionBarBackground changeColor(Boolean fade) {
        if (fade) {
            fadeBackground(mOldBackground, getColoredBackground(mNewColor));
        } else {
            mActionBar.setBackgroundDrawable(getColoredBackground(mNewColor));
        }
        return this;
    }

    /**
     * Fade the ActionBar background to zero opacity
     *
     * @return Instance of this class
     */
    private ActionBarBackground fadeOut() {
        Drawable background = getColoredBackground(android.R.color.transparent);
        background.setAlpha(0);
        fadeBackground(mOldBackground, background);
        return this;
    }

    /**
     * Fade the ActionBar background to solid opacity
     *
     * @return Instance of this class
     */
    private ActionBarBackground fadeIn(Integer color) {
        Drawable transBackground = getColoredBackground(android.R.color.transparent);
        Drawable background = getColoredBackground(color);
        background.setAlpha(1);
        fadeBackground(transBackground, background);
        return this;
    }

    /**
     * Fade the ActionBar background to the provided newDrawable
     *
     * @param newDrawable New background of ActionBar
     * @return Instance of this class
     */
    private ActionBarBackground fadeBackground(Drawable newDrawable) {
        fadeBackground(mOldBackground, newDrawable);
        return this;
    }

    /**
     * Fade the ActionBar background from oldDrawable to newDrawable
     *
     * @param oldDrawable Drawable to be faded from
     * @param newDrawable Drawable to be faded to
     * @return Instance of this class
     */
    private ActionBarBackground fadeBackground(Drawable oldDrawable, Drawable newDrawable) {
        if (oldDrawable == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                newDrawable.setCallback(drawableCallback);
            } else {
                mActionBar.setBackgroundDrawable(newDrawable);
            }
        } else {
            TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldDrawable, newDrawable});
            td.setCrossFadeEnabled(true);

            // workaround for broken ActionBarContainer drawable handling on
            // pre-API 17 builds
            // https://github.com/android/platform_frameworks_base/commit/a7cc06d82e45918c37429a59b14545c6a57db4e4
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                td.setCallback(drawableCallback);
            } else {
                int paddingTop = mToolbar.getPaddingTop();
                mActionBar.setBackgroundDrawable(td);
                mToolbar.setPadding(mToolbar.getPaddingLeft(), paddingTop, mToolbar.getPaddingRight(), mToolbar.getPaddingBottom()); // fix for fitSystemWindows
            }

            td.startTransition(500);
        }

        mOldBackground = newDrawable;

        // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
        //mActionBar.setDisplayShowTitleEnabled(false);
        //mActionBar.setDisplayShowTitleEnabled(true);

        return this;
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            mActionBar.setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mHandler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            mHandler.removeCallbacks(what);
        }
    };

    /**
     * Get new drawable with provided color.
     *
     * @param color Color of the new drawable
     * @return Drawable with provided color
     */
    public static Drawable getColoredBackground(int color) {
        return new ColorDrawable(color);
    }

    /**
     * Fade the ActionBar background to zero opacity
     *
     * @param activity Activity where the ActionBar has to change
     * @return Instance of this class
     */
    public static ActionBarBackground fadeOut(AppCompatActivity activity) {
        ActionBarBackground abColor = new ActionBarBackground(activity);
        abColor.fadeOut();
        return abColor;
    }

    /**
     * Fade the ActionBar background to solid opacity
     *
     * @param activity Activity where the ActionBar has to change
     * @return Instance of this class
     */
    public static ActionBarBackground fadeIn(AppCompatActivity activity, Integer color) {
        ActionBarBackground abColor = new ActionBarBackground(activity);
        abColor.fadeIn(color);
        return abColor;
    }

    /**
     * Change the background color of the ActionBar to newColor
     *
     * @param activity Activity where the ActionBar has to change
     * @param newColor New background color of the ActionBar
     * @return Instance of this class
     */
    public static ActionBarBackground changeColor(AppCompatActivity activity, int newColor) {
        return changeColor(activity, newColor, true);
    }

    /**
     * Change the background color of the ActionBar to newColor, fading or not
     *
     * @param activity Activity where the ActionBar has to change
     * @param newColor New background color of the ActionBar
     * @return Instance of this class
     */
    public static ActionBarBackground changeColor(AppCompatActivity activity, int newColor, Boolean fade) {
        ActionBarBackground abColor = new ActionBarBackground(activity, newColor);
        abColor.changeColor(fade);
        return abColor;
    }

    /**
     * Fade background of the ActionBar to newDrawable
     *
     * @param activity    Activity where the ActionBar has to change
     * @param newDrawable New background color of the ActionBar
     * @return Instance of this class
     */
    public static ActionBarBackground fadeDrawable(AppCompatActivity activity, Drawable newDrawable) {
        ActionBarBackground abColor = new ActionBarBackground(activity);
        abColor.fadeBackground(newDrawable);
        return abColor;
    }

}
