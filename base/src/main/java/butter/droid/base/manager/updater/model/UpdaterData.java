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

package butter.droid.base.manager.updater.model;

import java.util.Map;

public class UpdaterData {

    public Map<String, Map<String, Arch>> mobile;
    public Map<String, Map<String, Arch>> tv;

    public class Arch {
        public int versionCode;
        public String versionName;
        public String checksum;
        public String updateUrl;
    }
}
