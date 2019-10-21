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

import androidx.leanback.widget.Presenter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butter.droid.tv.R;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MoreViewHolder extends Presenter.ViewHolder implements View.OnFocusChangeListener {

    @BindView(R.id.text)
    TextView textview;
    @BindView(R.id.icon)
    ImageView imageview;

    public MoreViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);

        view.setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            textview.setVisibility(View.VISIBLE);
        } else {
            textview.setVisibility(View.GONE);
        }
    }
}
