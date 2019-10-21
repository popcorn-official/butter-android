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

package butter.droid.tv.adapters;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.Presenter;

import java.util.ArrayList;
import java.util.Collection;

import butter.droid.base.providers.media.models.Media;


public class MediaObjectAdapter extends ArrayObjectAdapter {
	private ArrayList<Media> mItems = new ArrayList<>();
	private Boolean isUpdating = false;
	private int lastPage = -1;

	//region Constructors

	public MediaObjectAdapter(Presenter presenter) {
		super(presenter);
	}

	//endregion

	//region Getters/Setters

	public int getLastPage() {
		return lastPage;
	}

	public void setLastPage(int lastPage) {
		this.lastPage = lastPage;
	}

	@Override
	public int size() {
		return mItems.size();
	}

	@Override
	public Object get(int index) {
		return mItems.get(index);
	}

	public int indexOf(Object item) {
		return mItems.indexOf(item);
	}

	public void notifyArrayItemRangeChanged(int positionStart, int itemCount) {
		notifyItemRangeChanged(positionStart, itemCount);
	}

	public Boolean getIsUpdating() {
		return isUpdating;
	}

	public void setIsUpdating(Boolean isUpdating) {
		this.isUpdating = isUpdating;
	}

	//endregion

	//region Getters/Setters

	public int getCount() {
		return mItems.size();
	}

	//endregion

	//region Addition

	public void add(Media media) {
		add(mItems.size(), media);
	}

	public void add(int index, Media media) {
		mItems.add(index, media);
		notifyItemRangeInserted(index, 1);
	}

	public void addAll(int index, Collection items) {
		int itemsCount = items.size();
		mItems.addAll(index, items);
		notifyItemRangeInserted(index, itemsCount);
	}
	//endregion

	//region removal

	public boolean remove(Media item) {
		int index = mItems.indexOf(item);
		if (index >= 0) {
			mItems.remove(index);
			notifyItemRangeRemoved(index, 1);
		}
		return index >= 0;
	}

	public int removeItems(int position, int count) {
		int itemsToRemove = Math.min(count, mItems.size() - position);

		for (int i = 0; i < itemsToRemove; i++) {
			mItems.remove(position);
		}
		notifyItemRangeRemoved(position, itemsToRemove);
		return itemsToRemove;
	}

	public void clear() {
		int itemCount = mItems.size();
		mItems.clear();
		notifyItemRangeRemoved(0, itemCount);
	}

	//endregion
}
