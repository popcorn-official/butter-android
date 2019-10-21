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

package butter.droid.base.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.List;

import butter.droid.base.R;


public class NumberPickerDialogFragment extends DialogFragment {

    public static final String TITLE = "title";
    public static final String MAX_VALUE = "max_val";
    public static final String MIN_VALUE = "min_val";
    public static final String DEFAULT_VALUE = "default_val";
    public static final String FOCUSABLE = "focusable";

    private ResultListener mOnResultListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (getArguments() == null || !getArguments().containsKey(MAX_VALUE) || !getArguments().containsKey(MIN_VALUE) || !getArguments().containsKey(TITLE) || mOnResultListener == null) {
            return builder.create();
        }

        final NumberPicker numberPicker = new NumberPicker(getActivity());
        numberPicker.setLayoutParams(new NumberPicker.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        numberPicker.setWrapSelectorWheel(false);

        final int minValue = getArguments().getInt(MIN_VALUE);
        final int maxValue = getArguments().getInt(MAX_VALUE);
        final int currentValue = getArguments().getInt(DEFAULT_VALUE, (int) Math.floor((numberPicker.getMaxValue() - numberPicker.getMinValue()) / 2));

        List<String> displayValues = new ArrayList<>();
        for(int i = minValue; i < maxValue + 1; i++) {
            displayValues.add(Integer.toString(i));
        }
        numberPicker.setDisplayedValues(displayValues.toArray(new String[displayValues.size()]));

        if(minValue < 0) {
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(maxValue + Math.abs(minValue));
            numberPicker.setValue(currentValue + Math.abs(minValue));
        } else {
            numberPicker.setMinValue(minValue);
            numberPicker.setMaxValue(maxValue);
            numberPicker.setValue(currentValue);
        }

        if(getArguments().containsKey(FOCUSABLE) && !getArguments().getBoolean(FOCUSABLE))
            numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        builder
                .setView(numberPicker)
                .setTitle(getArguments().getString(TITLE))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mOnResultListener.onNewValue(numberPicker.getValue() + (minValue < 0 ? minValue : 0));
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );

        return builder.create();
    }

    public void setOnResultListener(ResultListener resultListener) {
        mOnResultListener = resultListener;
    }

    public interface ResultListener {
        void onNewValue(int value);
    }

}
