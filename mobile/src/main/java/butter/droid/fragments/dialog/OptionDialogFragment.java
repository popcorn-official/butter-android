package butter.droid.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class OptionDialogFragment extends DialogFragment {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String POS_BUT = "pos_but";
    public static final String NEG_BUT = "neg_but";

    private Listener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString(TITLE))
                .setMessage(getArguments().getString(MESSAGE))
                .setPositiveButton(getArguments().getString(POS_BUT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null)
                            mListener.onSelectionPositive();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getArguments().getString(NEG_BUT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null)
                            mListener.onSelectionNegative();
                        dialog.dismiss();
                    }
                })
                .create();
    }

    public static void show(FragmentManager fm, String title, String message, String positiveButton, String negativeButton, Listener listener) {
        try {
            OptionDialogFragment dialogFragment = new OptionDialogFragment();
            Bundle args = new Bundle();
            args.putString(TITLE, title);
            args.putString(MESSAGE, message);
            args.putString(POS_BUT, positiveButton);
            args.putString(NEG_BUT, negativeButton);
            dialogFragment.setListener(listener);
            dialogFragment.setArguments(args);
            dialogFragment.show(fm, "overlay_fragment");
        } catch (IllegalStateException e) {
            // Eat exception
        }
    }

    public static void show(Context context, FragmentManager fm, int titleRes, int messageRes, int posButtonRes, int negButtonRes, Listener listener) {
        show(fm, context.getString(titleRes), context.getString(messageRes), context.getString(posButtonRes), context.getString(negButtonRes), listener);
    }

    private void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        public void onSelectionPositive();

        public void onSelectionNegative();
    }

}
