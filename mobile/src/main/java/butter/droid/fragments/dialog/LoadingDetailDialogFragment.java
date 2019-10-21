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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import javax.inject.Inject;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.MediaProvider;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.utils.ThreadUtils;

public class LoadingDetailDialogFragment extends DialogFragment {

    @Inject
    ProviderManager providerManager;

    private Callback mCallback;

    public static final String EXTRA_MEDIA = "media_item";
    private Boolean mSavedInstanceState = false;

    public static LoadingDetailDialogFragment newInstance(Integer position) {
        LoadingDetailDialogFragment frag = new LoadingDetailDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_MEDIA, position);
        frag.setArguments(args);
        return frag;
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * on create view
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return LayoutInflater.from(new ContextThemeWrapper(getActivity(), R.style.Theme_Butter)).inflate(R.layout
                .fragment_loading_detail, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);
        mSavedInstanceState = false;
        setStyle(STYLE_NO_FRAME, R.style.Theme_Dialog_Transparent);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        providerManager.getCurrentMediaProvider().cancel();
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        if (null == getTargetFragment())
            throw new IllegalArgumentException("target fragment must be set");
        if (getTargetFragment() instanceof Callback) mCallback = (Callback) getTargetFragment();
        else throw new IllegalArgumentException("target fragment must implement callbacks");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayList<Media> currentList = mCallback.getCurrentList();
        int position = getArguments().getInt(EXTRA_MEDIA);
        final Media media = currentList.get(position);
        providerManager.getCurrentMediaProvider().getDetail(currentList, position, new MediaProvider.Callback() {
                    @Override
                    public void onSuccess(MediaProvider.Filters filters, ArrayList<Media> items) {
                        if (!isAdded()) return;
                        if (items.size() <= 0) return;

                        final Media item = items.get(0);
                        item.color = media.color;
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCallback.onDetailLoadSuccess(item);
                                if (!mSavedInstanceState) dismiss();
                            }
                        });

                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!e.getMessage().equals("Canceled")) {
                            e.printStackTrace();
                            ThreadUtils.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mCallback.onDetailLoadFailure();
                                    if (!mSavedInstanceState) dismiss();
                                }
                            });
                        }
                    }
                }

        );
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedInstanceState = true;
    }

    public interface Callback {
        void onDetailLoadFailure();

        void onDetailLoadSuccess(Media item);

        ArrayList<Media> getCurrentList();
    }


}
