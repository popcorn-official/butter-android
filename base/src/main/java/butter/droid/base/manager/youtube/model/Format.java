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

package butter.droid.base.manager.youtube.model;

/**
 * Represents a format in the "fmt_list" parameter
 * Currently, only id is used
 */
public class Format {

    protected int mId;

    /**
     * Construct this object from one of the strings in the "fmt_list" parameter
     *
     * @param pFormatString one of the comma separated strings in the "fmt_list" parameter
     */
    public Format(String pFormatString) {
        String formatVars[] = pFormatString.split("/");
        mId = Integer.parseInt(formatVars[0]);
    }

    /**
     * Construct this object using a format id
     *
     * @param id id of this format
     */
    public Format(int id) {
        this.mId = id;
    }

    /**
     * Retrieve the id of this format
     *
     * @return the id
     */
    public int getId() {
        return mId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Format)) {
            return false;
        }
        return ((Format) object).mId == mId;
    }

}
