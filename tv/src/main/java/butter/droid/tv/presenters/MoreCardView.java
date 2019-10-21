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
import androidx.leanback.widget.BaseCardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butter.droid.tv.R;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MoreCardView extends BaseCardView implements View.OnFocusChangeListener {

    @BindView(R.id.main_image)
    ImageView imageView;

    @BindView(R.id.title_text)
    TextView titleTextView;

    @BindView(R.id.info_field)
    RelativeLayout infoAreaView;

    public MoreCardView(Context context) {
        this(context, null);
    }

    public MoreCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.Widget_Leanback_MoreCardViewStyle);
    }

    public MoreCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.more_card_view, this);

        ButterKnife.bind(this, v);

        setBackgroundResource(R.color.default_background);
        setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
        setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnFocusChangeListener(this);
    }


    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Sets the title text.
     */
    public void setTitleText(CharSequence text) {
        if (titleTextView == null) {
            return;
        }

        titleTextView.setText(text);
    }

    /**
     * Returns the title text.
     */
    public CharSequence getTitleText() {
        if (titleTextView == null) {
            return null;
        }

        return titleTextView.getText();
    }


    public void setImageResource(int imageResource) {
        imageView.setImageResource(imageResource);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            setBackgroundResource(R.color.primary_dark);
        } else {
            setBackgroundResource(R.color.default_background);
        }
    }
}
