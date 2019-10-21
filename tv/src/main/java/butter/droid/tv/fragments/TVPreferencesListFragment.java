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

package butter.droid.tv.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import android.view.View;

import java.util.List;

import butter.droid.base.utils.LocaleUtils;
import butter.droid.tv.R;

public class TVPreferencesListFragment extends GuidedStepFragment {

    private static final String TITLE_ARG = "titleres", ITEMS_ARG = "items", CURRENT_POS = "current";

    private SelectionListener mListener;


    public static TVPreferencesListFragment newInstance(@NonNull String title, @NonNull String[] items, int currentPos, @NonNull SelectionListener listener) {
        Bundle args = new Bundle();
        args.putString(TITLE_ARG, title);
        args.putStringArray(ITEMS_ARG, items);
        args.putInt(CURRENT_POS, currentPos);

        TVPreferencesListFragment fragment = new TVPreferencesListFragment();
        fragment.setArguments(args);
        fragment.mListener = listener;
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSelectedActionPosition(getArguments().getInt(CURRENT_POS, 0));
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getArguments().getString(TITLE_ARG, "").toUpperCase(LocaleUtils.getCurrent()), null, getString(R.string.app_name), null);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String[] items = getArguments().getStringArray(ITEMS_ARG);
        int current = getArguments().getInt(CURRENT_POS, -1);
        int i = 0;
        if (items != null) {
            for(String item : items) {
                actions.add(
                    new GuidedAction.Builder()
                            .id(i)
                            .checked(i == current)
                            .title(item)
                            .build()
                );
                i++;
            }
        }

        super.onCreateActions(actions, savedInstanceState);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);
        mListener.onSelect((int) action.getId());
        getFragmentManager().popBackStack();
    }

    interface SelectionListener {
        void onSelect(int position);
    }
}
