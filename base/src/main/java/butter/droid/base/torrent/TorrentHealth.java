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

package butter.droid.base.torrent;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.content.ContextCompat;

import butter.droid.base.R;
import butter.droid.base.utils.VersionUtils;

public enum TorrentHealth {
    EXCELLENT, GOOD, MEDIUM, BAD, UNKNOWN;

    public static TorrentHealth calculate(int seeds, int peers) {
        double ratio;
        if (peers > 0) {
            ratio = seeds / peers;
        } else {
            ratio = seeds;
        }

        double normalizedRatio = Math.min(ratio / 5 * 100, 100);
        double normalizedSeeds = Math.min(seeds / 30 * 100, 100);

        double weightedRatio = normalizedRatio * 0.6;
        double weightedSeeds = normalizedSeeds * 0.4;
        double weightedTotal = weightedRatio + weightedSeeds;

        int scaledTotal = (int) (weightedTotal * 3 / 100);

        switch (scaledTotal) {
            case 0:
                return BAD;
            case 1:
                return MEDIUM;
            case 2:
                return GOOD;
            case 3:
                return EXCELLENT;
        }

        return UNKNOWN;
    }

    public int getImageResource() {
        switch (this) {
            case BAD:
                return R.drawable.ic_health_bad;
            case MEDIUM:
                return R.drawable.ic_health_medium;
            case GOOD:
                return R.drawable.ic_health_good;
            case EXCELLENT:
                return R.drawable.ic_health_excellent;
            default:
            case UNKNOWN:
                return R.drawable.ic_health_unknown;
        }
    }

    public int getStringResource() {
        switch (this) {
            case BAD:
                return R.string.health_bad;
            case MEDIUM:
                return R.string.health_medium;
            case GOOD:
                return R.string.health_good;
            case EXCELLENT:
                return R.string.health_excellent;
            default:
            case UNKNOWN:
                return R.string.health_unknown;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Drawable getImageDrawable(Context context) {
        if(VersionUtils.isLollipop()) {
            return context.getResources().getDrawable(getImageResource(), null);
        }
        return ContextCompat.getDrawable(context, getImageResource());
    }
}
