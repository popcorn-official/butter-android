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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import androidx.annotation.Px;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;

import butter.droid.R;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PixelUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;


public class MediaGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int NORMAL = 0, LOADING = 1;
    private int itemWidth, itemHeight, margin, columns;
    private ArrayList<OverviewItem> mItems = new ArrayList<>();
    //	private ArrayList<Media> mData = new ArrayList<>();
    private MediaGridAdapter.OnItemClickListener mItemClickListener;

    public MediaGridAdapter(Context context, ArrayList<Media> items, Integer columns) {
        this.columns = columns;

        int screenWidth = PixelUtils.getScreenWidth(context);
        itemWidth = (screenWidth / columns);
        itemHeight = (int) ((double) itemWidth / 0.677);
        margin = PixelUtils.getPixelsFromDp(context, 2);

        setItems(items);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case LOADING:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_griditem_loading, parent, false);
                return new MediaGridAdapter.LoadingHolder(v);
            case NORMAL:
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_griditem, parent, false);
                return new MediaGridAdapter.ViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        @Px int doubleMargin = margin * 2;
        @Px int topMargin = (position < columns) ? margin * 2 : margin;

        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        layoutParams.height = itemHeight;
        layoutParams.width = itemWidth;
        int mod = LocaleUtils.currentLocaleIsRTL() ? 1 : 0;
        if (position % columns == mod) {
            viewHolder.itemView.setPadding(doubleMargin, topMargin, margin, margin);
        } else if (position % columns == columns - 1) {
            viewHolder.itemView.setPadding(margin, topMargin, doubleMargin, margin);
        } else {
            viewHolder.itemView.setPadding(margin, topMargin, margin, margin);
        }
        viewHolder.itemView.setLayoutParams(layoutParams);

        if (getItemViewType(position) == NORMAL) {
            final ViewHolder videoViewHolder = (ViewHolder) viewHolder;
            final OverviewItem overviewItem = getItem(position);
            Media item = overviewItem.media;

            videoViewHolder.title.setText(item.title);
            videoViewHolder.year.setText(item.year);

            if (item.image != null && !item.image.equals("")) {
                Picasso.with(videoViewHolder.coverImage.getContext()).cancelRequest(videoViewHolder.coverImage);
                Picasso.with(videoViewHolder.coverImage.getContext())
                        .load(item.image)
                        .resize(itemWidth, itemHeight)
                        .transform(DrawGradient.getInstance())
                        .into(videoViewHolder.coverImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).isLoadingItem) {
            return LOADING;
        }
        return NORMAL;
    }

    public OverviewItem getItem(int position) {
        if (position < 0 || mItems.size() <= position) return null;
        return mItems.get(position);
    }

    public void setOnItemClickListener(MediaGridAdapter.OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @DebugLog
    public void removeLoading() {
        if (getItemCount() <= 0) return;
        OverviewItem item = mItems.get(getItemCount() - 1);
        if (item.isLoadingItem) {
            mItems.remove(getItemCount() - 1);
            notifyDataSetChanged();
        }
    }

    @DebugLog
    public void addLoading() {
        OverviewItem item = null;
        if (getItemCount() != 0) {
            item = mItems.get(getItemCount() - 1);
        }

        if (getItemCount() == 0 || (item != null && !item.isLoadingItem)) {
            mItems.add(new OverviewItem(true));
            notifyDataSetChanged();
        }
    }

    @DebugLog
    public boolean isLoading() {
        return getItemCount() > 0 && getItemViewType(getItemCount() - 1) == LOADING;
    }

    @DebugLog
    public void setItems(ArrayList<Media> items) {
        // Clear items
        mItems.clear();
        // Add new items, if available
        if (null != items) {
            for (Media item : items) {
                mItems.add(new OverviewItem(item));
            }
        }
        notifyDataSetChanged();
    }

    public void clearItems() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(View v, Media item, int position);
    }

    private static class OverviewItem {
        Media media;
        boolean isLoadingItem = false;

        OverviewItem(Media media) {
            this.media = media;
        }

        OverviewItem(boolean loading) {
            this.isLoadingItem = loading;
        }
    }

    private static class DrawGradient implements Transformation {
        private static Transformation instance;

        public static Transformation getInstance() {
            if (instance == null) {
                instance = new DrawGradient();
            }
            return instance;
        }

        @Override
        public Bitmap transform(Bitmap src) {
            // Code borrowed from https://stackoverflow.com/questions/23657811/how-to-mask-bitmap-with-lineargradient-shader-properly
            int w = src.getWidth();
            int h = src.getHeight();
            Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlay);

            canvas.drawBitmap(src, 0, 0, null);
            src.recycle();

            Paint paint = new Paint();
            float gradientHeight = h / 2f;
            LinearGradient shader = new LinearGradient(0, h - gradientHeight, 0, h, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawRect(0, h - gradientHeight, w, h, paint);
            return overlay;
        }

        @Override
        public String key() {
            return "gradient()";
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View itemView;
        @BindView(R.id.focus_overlay)
        View focusOverlay;
        @BindView(R.id.cover_image)
        ImageView coverImage;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.year)
        TextView year;

        private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                focusOverlay.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        };

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.itemView = itemView;
            itemView.setOnClickListener(this);
            coverImage.setMinimumHeight(itemHeight);

            itemView.setOnFocusChangeListener(mOnFocusChangeListener);
        }

        public ImageView getCoverImage() {
            return coverImage;
        }

        @Override
        public void onClick(View view) {
            if (mItemClickListener != null) {
                int position = getLayoutPosition();
                Media item = getItem(position).media;
                mItemClickListener.onItemClick(view, item, position);
            }
        }

    }

    private class LoadingHolder extends RecyclerView.ViewHolder {

        View itemView;

        LoadingHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setMinimumHeight(itemHeight);
        }

    }
}