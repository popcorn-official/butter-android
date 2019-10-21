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

import android.content.Context;
import androidx.appcompat.widget.Toolbar;

import butter.droid.R;

public class ToolbarUtils {
    /**
     * This method fixes a bug when rotating an activity that contains a toolbar, with the minHeight set to actionBarHeight; When the
     * toolbar is rotate the height is updated, but the contents are not re layed out to match the new height which means they are aligned
     * incorrectly. By resetting the minHeight on rotation we fix this.
     *
     * @param context
     * @param toolbar
     */
    public static void updateToolbarHeight(Context context, Toolbar toolbar) {
        toolbar.getLayoutParams().height = context.getResources().getDimensionPixelSize(
                R.dimen.abc_action_bar_default_height_material);
    }
}
