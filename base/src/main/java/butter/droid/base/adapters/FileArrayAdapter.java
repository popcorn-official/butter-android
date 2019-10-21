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

package butter.droid.base.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butter.droid.base.adapters.models.Option;
import butterknife.BindView;
import butterknife.ButterKnife;

public class FileArrayAdapter extends ArrayAdapter<Option> {

    private LayoutInflater mLayoutInflater;
    private int id;
    private List<Option> mItems;

    public FileArrayAdapter(Context context, int textViewResourceId, List<Option> items) {
        super(context, textViewResourceId, items);
        id = textViewResourceId;
        mItems = items;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Option getItem(int i) {
        return mItems.get(i);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(id, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Option option = mItems.get(position);
        if (option != null) {
            holder.text1.setText(option.getName());
            holder.text2.setText(option.getData());
        }

        return convertView;
    }

    static class ViewHolder {
        @BindView(android.R.id.text1)
        TextView text1;
        @BindView(android.R.id.text2)
        TextView text2;

        public ViewHolder(View v) {
            ButterKnife.bind(this, v);
        }
    }

}
