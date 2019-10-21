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

package butter.droid.fragments.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butter.droid.R;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LoadingBeamingDialogFragment extends DialogFragment {

    private DialogInterface.OnCancelListener mOnCancelListener;

    @BindView(R.id.progress_textview)
    TextView mTextView;

    public static LoadingBeamingDialogFragment newInstance() {
        return new LoadingBeamingDialogFragment();
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * on create view
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = LayoutInflater.from(new ContextThemeWrapper(getActivity(), R.style.Theme_Butter)).inflate(R.layout
                .fragment_loading_detail, container, false);
        ButterKnife.bind(this, v);
        mTextView.setText(R.string.starting_beam);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.Theme_Dialog_Transparent);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mOnCancelListener != null)
            mOnCancelListener.onCancel(dialog);
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
    }

}
