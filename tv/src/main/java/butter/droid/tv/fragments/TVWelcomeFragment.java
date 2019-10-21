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

import java.util.List;

import butter.droid.base.content.preferences.Prefs;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.R;
import butter.droid.tv.activities.TVMainActivity;

public class TVWelcomeFragment extends GuidedStepFragment {

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getString(R.string.terms_and_conditions), getString(R.string.terms), getString(R.string.app_name), null);
    }

    @Override
    public GuidanceStylist onCreateGuidanceStylist() {
        return new TermsGuidanceStylist();
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction acceptAction = new GuidedAction.Builder().id(R.id.action_accept).hasNext(true).title(getString(R.string.accept)).build();
        GuidedAction declineAction = new GuidedAction.Builder().id(R.id.action_decline).hasNext(true).title(getString(R.string.leave)).build();
        actions.add(acceptAction);
        actions.add(declineAction);
        super.onCreateActions(actions, savedInstanceState);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch ((int) action.getId()) {
            case R.id.action_accept:
                //set first run flag to false, don't show welcome again
                PrefUtils.save(getActivity(), Prefs.FIRST_RUN, false);
                //start main activity

                TVMainActivity.startActivity(getActivity());
                getActivity().finish();
                return;
            case R.id.action_decline:
                getActivity().finish();
                return;
        }
        super.onGuidedActionClicked(action);
    }

    public static class TermsGuidanceStylist extends GuidanceStylist {

        @Override
        public int onProvideLayoutId() {
            return R.layout.guidance_type1;
        }
    }


}
