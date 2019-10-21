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

package butter.droid.base.content.preferences;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import butter.droid.base.utils.PrefUtils;

public class PrefItem {

    private Context mContext;
    private int mIconRes;
    private int mTitleRes;
    private String mPrefKey;
    private Object mDefaultValue;
    private OnClickListener mOnClickListener;
    private SubtitleGenerator mSubtitleGenerator;
    private Boolean mHasNext = false;

    public static Builder newBuilder(Context context) {
        return new Builder(context);
    }

    private PrefItem() {

    }

    public Object getValue() {
        if (mDefaultValue instanceof Integer) {
            return PrefUtils.get(mContext, mPrefKey, (Integer) mDefaultValue);
        } else if (mDefaultValue instanceof Long) {
            return PrefUtils.get(mContext, mPrefKey, (Long) mDefaultValue);
        } else if (mDefaultValue instanceof Boolean) {
            return PrefUtils.get(mContext, mPrefKey, (Boolean) mDefaultValue);
        } else {
            return PrefUtils.get(mContext, mPrefKey, mDefaultValue.toString());
        }
    }

    public void saveValue(Object value) {
        if (mDefaultValue instanceof Integer) {
            PrefUtils.save(mContext, mPrefKey, (Integer) value);
        } else if (mDefaultValue instanceof Long) {
            PrefUtils.save(mContext, mPrefKey, (Long) value);
        } else if (mDefaultValue instanceof Boolean) {
            PrefUtils.save(mContext, mPrefKey, (Boolean) value);
        } else {
            PrefUtils.save(mContext, mPrefKey, value.toString());
        }
    }

    public void clearValue() {
        PrefUtils.remove(mContext, mPrefKey);
    }

    public int getIconResource() {
        return mIconRes;
    }

    public String getTitle() {
        return mContext.getResources().getString(mTitleRes);
    }

    public String getPrefKey() {
        return mPrefKey;
    }

    public Object getDefaultValue() {
        return mDefaultValue;
    }

    public String getSubtitle() {
        if (mSubtitleGenerator != null) {
            return mSubtitleGenerator.get(this);
        }
        return "";
    }

    public boolean isClickable() {
        return mOnClickListener != null;
    }


    public boolean hasNext() {
        return mHasNext;
    }

    public boolean isTitle() {
        return mPrefKey == null;
    }

    public void onClick() {
        if (mOnClickListener != null)
            mOnClickListener.onClick(this);
    }

    public interface OnClickListener {
        void onClick(PrefItem item);
    }

    public interface SubtitleGenerator {
        String get(PrefItem item);
    }

    public static class Builder {

        private PrefItem mItem;

        public Builder(Context context) {
            mItem = new PrefItem();
            mItem.mContext = context;
        }

        public Builder setIconResource(@DrawableRes int iconRes) {
            mItem.mIconRes = iconRes;
            return this;
        }

        public Builder setTitleResource(@StringRes int titleRes) {
            mItem.mTitleRes = titleRes;
            return this;
        }

        public Builder setPreferenceKey(String prefKey) {
            mItem.mPrefKey = prefKey;
            return this;
        }

        public Builder setDefaultValue(Object defaultValue) {
            mItem.mDefaultValue = defaultValue;
            return this;
        }

        public Builder setOnClickListener(OnClickListener onClickListener) {
            mItem.mOnClickListener = onClickListener;
            return this;
        }

        public Builder setSubtitleGenerator(SubtitleGenerator subtitleGenerator) {
            mItem.mSubtitleGenerator = subtitleGenerator;
            return this;
        }

        public Builder hasNext(Boolean hasNext) {
            mItem.mHasNext = hasNext;
            return this;
        }

        public PrefItem build() {
            return mItem;
        }

    }

}
