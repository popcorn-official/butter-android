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

package butter.droid.tv.service.recommendation;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;

import java.io.IOException;

import butter.droid.base.utils.VersionUtils;
import butter.droid.tv.R;

/*
 * This class builds recommendations as notifications with videos as inputs.
 */
public class RecommendationBuilder {
    private static final String TAG = "RecommendationBuilder";

    private Context mContext;
    private NotificationManager mNotificationManager;

    private int mId;
    private int mPriority;
    private int mSmallIcon;
    private String mTitle;
    private String mDescription;
    private String mImageUri;
    private String mBackgroundUri;
    private PendingIntent mIntent;

    public RecommendationBuilder() {
    }

    public RecommendationBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    public RecommendationBuilder setId(int id) {
        mId = id;
        return this;
    }

    public RecommendationBuilder setPriority(int priority) {
        mPriority = priority;
        return this;
    }

    public RecommendationBuilder setTitle(String title) {
        mTitle = title;
        return this;
    }

    public RecommendationBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    public RecommendationBuilder setImage(String uri) {
        mImageUri = uri;
        return this;
    }

    public RecommendationBuilder setBackgroundContentUri(String uri) {
        mBackgroundUri = uri;
        return this;
    }

    public RecommendationBuilder setIntent(PendingIntent intent) {
        mIntent = intent;
        return this;
    }

    public RecommendationBuilder setSmallIcon(int resourceId) {
        mSmallIcon = resourceId;
        return this;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Notification build() throws IOException {
        if(VersionUtils.isLollipop()) {

            Log.d(TAG, "Building notification - " + this.toString());

            if (mNotificationManager == null) {
                mNotificationManager = (NotificationManager) mContext
                        .getSystemService(Context.NOTIFICATION_SERVICE);
            }

            Bundle extras = new Bundle();
            if (mBackgroundUri != null) {
                extras.putString(Notification.EXTRA_BACKGROUND_IMAGE_URI, mBackgroundUri);
            }

            Bitmap image = Picasso.with(mContext)
                    .load(mImageUri)
                    .resize((int) mContext.getResources().getDimension(R.dimen.card_width), (int) mContext.getResources().getDimension(R.dimen.card_height))
                    .get();

            Notification notification = new NotificationCompat.BigPictureStyle(
                    new NotificationCompat.Builder(mContext)
                            .setContentTitle(mTitle)
                            .setContentText(mDescription)
                            .setPriority(mPriority)
                            .setLocalOnly(true)
                            .setOngoing(true)
                            .setColor(mContext.getResources().getColor(R.color.primary))
                            .setCategory(Notification.CATEGORY_RECOMMENDATION)
                            .setLargeIcon(image)
                            .setSmallIcon(mSmallIcon)
                            .setContentIntent(mIntent)
                            .setExtras(extras))
                    .build();

            mNotificationManager.notify(mId, notification);
            mNotificationManager = null;
            return notification;
        }

        return null;
    }

    @Override
    public String toString() {
        return "RecommendationBuilder{" +
                ", mId=" + mId +
                ", mPriority=" + mPriority +
                ", mSmallIcon=" + mSmallIcon +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mImageUri='" + mImageUri + '\'' +
                ", mBackgroundUri='" + mBackgroundUri + '\'' +
                ", mIntent=" + mIntent +
                '}';
    }
}
