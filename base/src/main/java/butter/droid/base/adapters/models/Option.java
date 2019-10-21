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

package butter.droid.base.adapters.models;

import androidx.annotation.NonNull;

import butter.droid.base.utils.LocaleUtils;

public class Option implements Comparable<Option> {
    private String name;
    private String data;
    private String path;

    public Option(String n, String d, String p) {
        name = n;
        data = d;
        path = p;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Option option = (Option) o;

        return name != null ? name.equals(option.name) : option.name == null
                && (data != null ? data.equals(option.data) : option.data == null
                && (path != null ? path.equals(option.path) : option.path == null));

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NonNull Option o) {
        if (this.name != null)
            return this.name.toLowerCase(LocaleUtils.getCurrent()).compareTo(o.getName().toLowerCase(LocaleUtils.getCurrent()));
        else
            throw new IllegalArgumentException();
    }
}