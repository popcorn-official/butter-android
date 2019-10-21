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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;

import butter.droid.R;
import butter.droid.base.ButterApplication;

public class MessageDialogFragment extends DialogFragment {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String CANCELABLE = "cancelable";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!getArguments().containsKey(TITLE) || !getArguments().containsKey(MESSAGE)) {
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(TITLE))
                .setMessage(getArguments().getString(MESSAGE));

        if(getArguments().getBoolean(CANCELABLE, true)) {
            builder.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            setCancelable(true);
        } else {
            setCancelable(false);
        }

        return builder.create();
    }

    public static void show(FragmentManager fm, String title, String message) {
        show(fm, title, message, true);
    }

    public static void show(FragmentManager fm, String title, String message, Boolean cancelable) {
        MessageDialogFragment dialogFragment = new MessageDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
        args.putBoolean(CANCELABLE, cancelable);
        dialogFragment.setArguments(args);
        dialogFragment.show(fm, "overlay_fragment");
    }

    public static void show(FragmentManager fm, int titleRes, int messageRes) {
        show(fm, ButterApplication.getAppContext().getString(titleRes), ButterApplication.getAppContext().getString(messageRes));
    }

    public static void show(FragmentManager fm, int titleRes, int messageRes, Boolean cancelable) {
        show(fm, ButterApplication.getAppContext().getString(titleRes), ButterApplication.getAppContext().getString(messageRes), cancelable);
    }
}
