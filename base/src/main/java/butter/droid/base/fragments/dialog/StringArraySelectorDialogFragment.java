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
import androidx.fragment.app.FragmentManager;

import java.util.List;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;

public class StringArraySelectorDialogFragment extends DialogFragment {

    private static final String ARRAY = "array";
    private static final String TITLE = "title";
    private static final String MODE = "mode";
    private static final String POSITION = "position";
    private static final int NORMAL = 0;
    private static final int SINGLE_CHOICE = 1;

    private DialogInterface.OnClickListener mOnClickListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (getArguments() == null || !getArguments().containsKey(ARRAY) || !getArguments().containsKey(TITLE) || mOnClickListener == null) {
            return builder.create();
        }

        Bundle b = getArguments();
        Object array = b.get(ARRAY);
        String[] stringArray;
        if (array instanceof List) {
            stringArray = (String[]) ((List) array).toArray(new String[((List) array).size()]);
        } else if (array instanceof String[]) {
            stringArray = b.getStringArray(ARRAY);
        } else {
            return builder.create();
        }
        String title = b.getString(TITLE);

        if (b.containsKey(MODE) && b.getInt(MODE) == SINGLE_CHOICE) {
            int defaultPosition = -1;
            if (b.containsKey(POSITION)) {
                defaultPosition = b.getInt(POSITION);
            }
            builder.setSingleChoiceItems(stringArray, defaultPosition, mOnClickListener);
        } else {
            builder.setItems(stringArray, mOnClickListener);
        }

        builder
                .setTitle(title)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }

    private void setDialogClickListener(DialogInterface.OnClickListener dialogClickListener) {
        mOnClickListener = dialogClickListener;
    }

    public static void show(FragmentManager fm, int titleRes, List<String> items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        show(fm, ButterApplication.getAppContext().getString(titleRes), items, defaultPosition, dialogClickListener);
    }

    public static void show(FragmentManager fm, int titleRes, String[] items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        show(fm, ButterApplication.getAppContext().getString(titleRes), items, defaultPosition, dialogClickListener);
    }

    private static void show(FragmentManager fm, String title, List<String> items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        String[] itemsArray = items.toArray(new String[items.size()]);
        show(fm, title, itemsArray, defaultPosition, dialogClickListener);
    }

    public static void show(FragmentManager fm, String title, String[] items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        show(fm, title, items, defaultPosition, NORMAL, dialogClickListener);
    }

    public static void showSingleChoice(FragmentManager fm, int titleRes, List<String> items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        showSingleChoice(fm, ButterApplication.getAppContext().getString(titleRes), items, defaultPosition, dialogClickListener);
    }

    public static void showSingleChoice(FragmentManager fm, int titleRes, String[] items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        showSingleChoice(fm, ButterApplication.getAppContext().getString(titleRes), items, defaultPosition, dialogClickListener);
    }

    private static void showSingleChoice(FragmentManager fm, String title, List<String> items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        String[] itemsArray = items.toArray(new String[items.size()]);
        showSingleChoice(fm, title, itemsArray, defaultPosition, dialogClickListener);
    }

    public static void showSingleChoice(FragmentManager fm, String title, String[] items, int defaultPosition, DialogInterface.OnClickListener dialogClickListener) {
        show(fm, title, items, defaultPosition, SINGLE_CHOICE, dialogClickListener);
    }

    private static void show(FragmentManager fm, String title, String[] items, int defaultPosition, int mode, DialogInterface.OnClickListener dialogClickListener) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putStringArray(ARRAY, items);
        args.putInt(MODE, mode);
        args.putInt(POSITION, defaultPosition);

        StringArraySelectorDialogFragment dialogFragment = new StringArraySelectorDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.setDialogClickListener(dialogClickListener);
        dialogFragment.show(fm, "overlay_fragment");
    }

}
