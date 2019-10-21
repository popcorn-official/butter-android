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

package butter.droid.tv.presenters.showdetail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import butter.droid.base.providers.media.models.Episode;
import butter.droid.tv.R;

public class EpisodeCardPresenter extends Presenter {

	private Context mContext;
	private int mCardWidth;
	private int mCardHeight;
    private Listener clickListener;

    public EpisodeCardPresenter(Context context) {
		mCardWidth = (int) context.getResources().getDimension(R.dimen.card_thumbnail_width);
		mCardHeight = (int) context.getResources().getDimension(R.dimen.card_thumbnail_height);
	}

    public void setOnClickListener(@NonNull Listener listener) {
        clickListener = listener;
    }

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent) {
		mContext = parent.getContext();

		ImageCardView cardView = new ImageCardView(mContext);
		cardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
		cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
		cardView.setFocusable(true);
		cardView.setFocusableInTouchMode(true);
		cardView.setBackgroundColor(ActivityCompat.getColor(mContext, R.color.default_background));

		return new ViewHolder(cardView);
	}

	@Override
	public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
		final Episode episode = (Episode) item;
		((ViewHolder) viewHolder).setEpisode(episode);
		((ViewHolder) viewHolder).getCardView().setTitleText(episode.title);
		((ViewHolder) viewHolder).getCardView().setContentText(
            String.format(mContext.getString(R.string.episode_number_format), episode.episode));
		((ViewHolder) viewHolder).getCardView().setMainImageDimensions(mCardWidth, mCardHeight);
		((ViewHolder) viewHolder).updateCardViewImage(episode.image);
        ((ViewHolder) viewHolder).cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != clickListener) clickListener.onEpisodeClicked(episode);
            }
        });
	}

	@Override
	public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) { }

	public class ViewHolder extends Presenter.ViewHolder {

		private Episode episode;
		private ImageCardView cardView;
		private Drawable defaultImage;
		private PicassoImageCardViewTarget imageCardViewTarget;

		public ViewHolder(View view) {
			super(view);
			cardView = (ImageCardView) view;
			imageCardViewTarget = new PicassoImageCardViewTarget(cardView);
			defaultImage = ActivityCompat.getDrawable(mContext, R.drawable.banner);
            cardView.setMainImage(defaultImage);
		}

		public void setEpisode(Episode episode) {
			this.episode = episode;
		}

		public Episode getEpisode() {
			return episode;
		}

		public ImageCardView getCardView() {
			return cardView;
		}

		protected void updateCardViewImage(String uri) {
			Picasso.with(mContext)
				.load(uri)
				.resize(mCardWidth, mCardHeight)
                .centerCrop()
				.placeholder(defaultImage)
				.error(defaultImage)
				.into(imageCardViewTarget);
		}
	}

	public class PicassoImageCardViewTarget implements Target {
		private ImageCardView mImageCardView;

		public PicassoImageCardViewTarget(ImageCardView imageCardView) {
			mImageCardView = imageCardView;
		}

		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
			Drawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
			mImageCardView.setMainImage(bitmapDrawable);
		}

		@Override
		public void onBitmapFailed(Drawable drawable) {
			mImageCardView.setMainImage(drawable);
		}

		@Override
		public void onPrepareLoad(Drawable drawable) {
			// Do nothing, default_background manager has its own transitions
		}
	}

    public interface Listener {
        void onEpisodeClicked(Episode row);
    }
}
