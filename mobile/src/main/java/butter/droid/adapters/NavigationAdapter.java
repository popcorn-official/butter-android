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

package butter.droid.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.Bind;
import de.hdodenhof.circleimageview.CircleImageView;
import butter.droid.R;
import butter.droid.fragments.NavigationDrawerFragment;

public class NavigationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private OnItemClickListener mItemClickListener;
    private List<NavigationDrawerFragment.NavDrawerItem> mItems;
    static final int HEADER = 0, ITEM = 1;
    final int mNormalColor, mCheckedColor, mCheckedBackgroundRes, mNormalBackgroundRes;
    private Callback mCallback;

    public NavigationAdapter(@NonNull Context context, @NonNull Callback callback, List<NavigationDrawerFragment.NavDrawerItem> items) {
        mItems = items;
        mCallback = callback;
        mNormalColor = context.getResources().getColor(R.color.nav_drawer_deselected);
        mCheckedColor = context.getResources().getColor(R.color.primary);
        mNormalBackgroundRes = R.drawable.selectable_nav_background;
        mCheckedBackgroundRes = R.color.nav_drawer_selected_bg;
    }

    public void setItems(List<NavigationDrawerFragment.NavDrawerItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_drawer_header, parent, false);
                return new HeaderHolder(v);
            case ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_drawer_list_item, parent, false);
                return new ItemRowHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case HEADER:
                onBindHeaderViewHolder((HeaderHolder) holder, position);
                break;
            case ITEM:
                onBindItemViewHolder((ItemRowHolder) holder, position);
                break;
        }

    }

    private void onBindHeaderViewHolder(HeaderHolder holder, int position) {
        //do nothing for now
        holder.mBackgroundImageView.setBackgroundResource(R.color.primary_dark);
        holder.mProfileImageView.setVisibility(View.VISIBLE);
        holder.mProfileImageView.setImageResource(R.drawable.butter_profile);
    }

    private void onBindItemViewHolder(ItemRowHolder viewHolder, int position) {
        NavigationDrawerFragment.NavDrawerItem item = getItem(position);
        item.setRowHolder(viewHolder);

        if(item.isSwitch()) {
            if(item.showProgress()) {
                viewHolder.checkbox.setVisibility(View.INVISIBLE);
                viewHolder.progressBar.setVisibility(View.VISIBLE);
            } else {
                viewHolder.checkbox.setVisibility(View.VISIBLE);
                viewHolder.progressBar.setVisibility(View.INVISIBLE);
            }
            viewHolder.checkbox.setChecked(item.getSwitchValue());
        } else {
            viewHolder.checkbox.setVisibility(View.INVISIBLE);
            viewHolder.progressBar.setVisibility(View.INVISIBLE);
        }


        viewHolder.title.setText(item.getTitle());

        boolean isSelected = (getCorrectPosition(position)) == mCallback.getSelectedPosition();
        viewHolder.title.setTextColor(isSelected ? mCheckedColor : mNormalColor);
        viewHolder.itemView.setBackgroundResource(isSelected ? mCheckedBackgroundRes : mNormalBackgroundRes);
        //		vh.itemView.setBackgroundResource(isSelected ? R.color.nav_drawer_highlight : 0);

        if (item.getIcon() > 0) {
            viewHolder.icon.setImageResource(item.getIcon());
            if (isSelected) viewHolder.icon.setColorFilter(mCheckedColor, PorterDuff.Mode.SRC_IN);
            else viewHolder.icon.setColorFilter(mNormalColor, PorterDuff.Mode.SRC_IN);
        }


    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).isHeader()) {
            return HEADER;
        }
        return ITEM;
    }


    public interface OnItemClickListener {
        public void onItemClick(View v, ItemRowHolder vh,  NavigationDrawerFragment.NavDrawerItem item, int position);
    }

    public NavigationDrawerFragment.NavDrawerItem getItem(int position) {
        if (position < 0 || mItems.size() <= position) return null;
        return mItems.get(position);
    }

    /**
     * Accounts for non Navigation Item rows and returns a position minus the correct offset
     *
     * @param position
     * @return
     */
    public int getCorrectPosition(int position) {
        return position - 1;
    }

    public class ItemRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Bind(android.R.id.icon)
        ImageView icon;
        @Bind(android.R.id.text1)
        TextView title;
        @Bind(android.R.id.checkbox)
        Switch checkbox;
        @Bind(android.R.id.progress)
        ProgressBar progressBar;

        public ItemRowHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mItemClickListener != null) {
                int position = getAdapterPosition();
                NavigationDrawerFragment.NavDrawerItem item = getItem(position);
                mItemClickListener.onItemClick(view, this, item, position);
            }
        }

        public Switch getSwitch() {
            return checkbox;
        }
        public ProgressBar getProgressBar() {
            return progressBar;
        }
    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.bg_imageview)
        ImageView mBackgroundImageView;
        @Bind(R.id.profile_imageview)
        CircleImageView mProfileImageView;
        @Bind(R.id.title_textview)
        TextView mTitleTextView;
        @Bind(R.id.subtitle_textview)
        TextView mSubtitleTextView;

        public HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface Callback {
        int getSelectedPosition();
    }
}
