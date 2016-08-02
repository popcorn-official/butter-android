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

package butter.droid.tv.presenters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.providers.media.models.Media;
import butter.droid.base.utils.AnimUtils;
import butter.droid.tv.R;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand. 
 * It contains an Image CardView
 */
public class MediaCardPresenter extends Presenter {

	private static Context mContext;
	private static int mCardWidth;
	private static int mCardHeight;

	private final int mDefaultInfoBackgroundColor;
	private final int mDefaultSelectedInfoBackgroundColor;

	public MediaCardPresenter(Context context) {
		mDefaultSelectedInfoBackgroundColor = context.getResources().getColor(R.color.primary_dark);
		mDefaultInfoBackgroundColor = context.getResources().getColor(R.color.default_background);
		mCardWidth = (int) context.getResources().getDimension(R.dimen.card_width);
		mCardHeight = (int) context.getResources().getDimension(R.dimen.card_height);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent) {
		mContext = parent.getContext();

		final CustomImageCardView cardView = new CustomImageCardView(mContext) {
			@Override
			public void setSelected(boolean selected) {
				if (getCustomSelectedSwatch() != null && selected) {
					setInfoAreaBackgroundColor(getCustomSelectedSwatch().getRgb());
				} else setInfoAreaBackgroundColor(selected ? mDefaultSelectedInfoBackgroundColor : mDefaultInfoBackgroundColor);
				super.setSelected(selected);
			}
		};

		cardView.setInfoAreaBackgroundColor(mDefaultInfoBackgroundColor);
		cardView.setFocusable(true);
		cardView.setFocusableInTouchMode(true);
		return new ViewHolder(cardView);
	}

	@Override
	public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
		MediaCardItem overview = (MediaCardItem) object;

		if (overview.isLoading()) onBindLoadingViewHolder(viewHolder, overview);
		else onBindMediaViewHolder(viewHolder, overview);
	}

	public void onBindLoadingViewHolder(Presenter.ViewHolder viewHolder, MediaCardItem overview) {
		final CustomImageCardView cardView = (CustomImageCardView) viewHolder.view;
		cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE);
		cardView.setMainImage(mContext.getResources().getDrawable(R.drawable.placeholder_inset, null));
		cardView.setTitleText(mContext.getString(R.string.loading));
		cardView.setMainImageDimensions(mCardWidth, mCardHeight);
	}

	public void onBindMediaViewHolder(Presenter.ViewHolder viewHolder, MediaCardItem overview) {
		Media item = overview.getMedia();
		final CustomImageCardView cardView = (CustomImageCardView) viewHolder.view;

        cardView.setTitleText(item.title);
        cardView.setContentText(!TextUtils.isEmpty(item.genre) ?
            String.format("%s (%s) %s", item.year, item.rating, item.genre.toLowerCase()) :
                item.rating != null && !TextUtils.isEmpty(item.rating) ?
					String.format("%s (%s)", item.year, item.rating) :
					item.year);
        cardView.getMainImageView().setAlpha(1f);
        cardView.getMainImageView().setPadding(0,0,0,0);
        cardView.setMainImageDimensions(mCardWidth, mCardHeight);
        cardView.getMainImageView().setVisibility(View.GONE);
        cardView.setCustomSelectedSwatch(null);

		if (item.image != null) {
			Target target = new Target() {
				@Override public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
					Palette.from(bitmap).maximumColorCount(16).generate(new Palette.PaletteAsyncListener() {
						@Override public void onGenerated(Palette palette) {
							Palette.Swatch swatch = palette.getDarkMutedSwatch();
							cardView.setCustomSelectedSwatch(swatch);

                            cardView.getMainImageView().setImageBitmap(bitmap);
                            cardView.getMainImageView().setVisibility(View.GONE);
                            AnimUtils.fadeIn(cardView.getMainImageView());
						}
					});
				}

				@Override public void onBitmapFailed(Drawable errorDrawable) {
					cardView.getMainImageView().setImageResource(R.drawable.placeholder_inset);
                    cardView.getMainImageView().setAlpha(0.4f);
					cardView.getMainImageView().setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					cardView.getMainImageView().setVisibility(View.GONE);
					AnimUtils.fadeIn(cardView.getMainImageView());

				}

				@Override public void onPrepareLoad(Drawable placeHolderDrawable) {

				}
			};
			//load image
			Picasso.with(mContext).load(item.image).resize(mCardWidth, mCardHeight).centerCrop().into(target);
			cardView.setTarget(target);
		} else {
            cardView.getMainImageView().setImageResource(R.drawable.placeholder_inset);
            cardView.getMainImageView().setAlpha(0.4f);
            cardView.getMainImageView().setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            cardView.getMainImageView().setVisibility(View.GONE);
            AnimUtils.fadeIn(cardView.getMainImageView());
        }
	}

	@Override
	public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
		CustomImageCardView cardView = (CustomImageCardView) viewHolder.view;
		// Remove references to images so that the garbage collector can free up memory
		cardView.setBadgeImage(null);
		cardView.setMainImage(null);
		if (cardView.getTarget() != null) {
			Picasso.with(mContext).cancelRequest(cardView.getTarget());
			cardView.setTarget(null);
		}
	}

	public static class CustomImageCardView extends ImageCardView {
		private Palette.Swatch mCustomSelectedSwatch;
		private Target mTarget;

		public CustomImageCardView(Context context) {
			super(context);
			setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
		}

		public CustomImageCardView(Context context, AttributeSet attrs) {
			super(context, attrs);
			setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
		}

		public CustomImageCardView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
		}

		public Palette.Swatch getCustomSelectedSwatch() {
			return mCustomSelectedSwatch;
		}

		public void setCustomSelectedSwatch(Palette.Swatch customSelectedSwatch) {
			mCustomSelectedSwatch = customSelectedSwatch;
		}

		public Target getTarget() {
			return mTarget;
		}

		public void setTarget(Target target) {
			mTarget = target;
		}
	}

	public static class MediaCardItem {
		private Media mMedia;
		private boolean mLoading;

		public MediaCardItem(Media media) {
			mMedia = media;
		}

		public MediaCardItem(boolean loading) {
			mLoading = loading;
		}

		public Media getMedia() {
			return mMedia;
		}

		public boolean isLoading() {
			return mLoading;
		}
	}

	public static List<MediaCardItem> convertMediaToOverview(List<Media> items) {
		List<MediaCardItem> list = new ArrayList<>();
		for (Media media : items) list.add(new MediaCardItem(media));
		return list;
	}
}
