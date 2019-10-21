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
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import butter.droid.R;
import butter.droid.fragments.NavigationDrawerFragment;
import butter.droid.fragments.NavigationDrawerFragment.AbsNavDrawerItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class NavigationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private OnItemClickListener mItemClickListener;
    private List<NavigationDrawerFragment.AbsNavDrawerItem> mItems;
    private final int mNormalColor, mCheckedColor, mCheckedBackgroundRes, mNormalBackgroundRes;
    private Callback mCallback;

    public NavigationAdapter(@NonNull Context context, @NonNull Callback callback,
                             List<NavigationDrawerFragment.AbsNavDrawerItem> items) {
        mItems = items;
        mCallback = callback;
        mNormalColor = ContextCompat.getColor(context, R.color.nav_drawer_deselected);
        mCheckedColor = ContextCompat.getColor(context, R.color.primary);
        mNormalBackgroundRes = R.drawable.selectable_nav_background;
        mCheckedBackgroundRes = R.color.nav_drawer_selected_bg;
    }

    public void setItems(List<NavigationDrawerFragment.AbsNavDrawerItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_drawer_header, parent, false);
                return new HeaderHolder(v);
            case VIEW_TYPE_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.nav_drawer_list_item, parent, false);
                return new ItemRowHolder(v);
            default:
                throw new IllegalStateException("Unknown view type");
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case VIEW_TYPE_HEADER:
                onBindHeaderViewHolder((HeaderHolder) holder);
                break;
            case VIEW_TYPE_ITEM:
                onBindItemViewHolder((ItemRowHolder) holder, position);
                break;
        }

    }

    private void onBindHeaderViewHolder(HeaderHolder holder) {
        //do nothing for now
        holder.mBackgroundImageView.setBackgroundResource(R.color.primary_dark);
        holder.mProfileImageView.setVisibility(View.VISIBLE);
        holder.mProfileImageView.setImageResource(R.drawable.butter_profile);
    }

    private void onBindItemViewHolder(ItemRowHolder viewHolder, int position) {

        AbsNavDrawerItem item = getItem(position);

        if (item instanceof AbsNavDrawerItem.VPNNavDrawerItem) {

            AbsNavDrawerItem.VPNNavDrawerItem vpnNavDrawerItem = (AbsNavDrawerItem.VPNNavDrawerItem) item;

            if (vpnNavDrawerItem.isSwitch()) {
                if (vpnNavDrawerItem.showProgress()) {
                    viewHolder.checkbox.setVisibility(View.INVISIBLE);
                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.checkbox.setVisibility(View.VISIBLE);
                    viewHolder.progressBar.setVisibility(View.INVISIBLE);
                }
                viewHolder.checkbox.setChecked(vpnNavDrawerItem.getSwitchValue());
            } else {
                viewHolder.checkbox.setVisibility(View.INVISIBLE);
                viewHolder.progressBar.setVisibility(View.INVISIBLE);
            }
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
        if (getItem(position).getType() == AbsNavDrawerItem.TYPE_HEADER) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_ITEM;
    }

    public NavigationDrawerFragment.AbsNavDrawerItem getItem(int position) {
        if (position < 0 || mItems.size() <= position) return null;
        return mItems.get(position);
    }

    public interface OnItemClickListener {
        void onItemClick(View v, ItemRowHolder vh, NavigationDrawerFragment.AbsNavDrawerItem item, int position);
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
        @BindView(android.R.id.icon)
        ImageView icon;
        @BindView(android.R.id.text1)
        TextView title;
        @BindView(android.R.id.checkbox)
        Switch checkbox;
        @BindView(android.R.id.progress)
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
                NavigationDrawerFragment.AbsNavDrawerItem item = getItem(position);
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

        @BindView(R.id.bg_imageview)
        ImageView mBackgroundImageView;
        @BindView(R.id.profile_imageview)
        CircleImageView mProfileImageView;
        @BindView(R.id.title_textview)
        TextView mTitleTextView;
        @BindView(R.id.subtitle_textview)
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
