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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import java.util.List;

import butter.droid.base.manager.updater.ButterUpdateManager;
import butter.droid.base.utils.PrefUtils;
import butter.droid.tv.R;

public class TVUpdateFragment extends GuidedStepFragment {

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return new GuidanceStylist.Guidance(getString(R.string.update_available), getString(R.string.new_update), getString(R.string.app_name), null);
    }

    @Override
    public GuidanceStylist onCreateGuidanceStylist() {
        return new TermsGuidanceStylist();
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction acceptAction = new GuidedAction.Builder().id(R.id.action_update_now).hasNext(true).title(getString(R.string.now)).build();
        GuidedAction declineAction = new GuidedAction.Builder().id(R.id.action_update_later).hasNext(true).title(getString(R.string.later)).build();
        actions.add(acceptAction);
        actions.add(declineAction);
        super.onCreateActions(actions, savedInstanceState);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        switch ((int) action.getId()) {
            case R.id.action_update_now:
                String updateFile = PrefUtils.get(getActivity(), ButterUpdateManager.UPDATE_FILE, "");
                Intent updateIntent = new Intent(Intent.ACTION_VIEW);
                updateIntent.setDataAndType(Uri.parse("file://" + updateFile), ButterUpdateManager.ANDROID_PACKAGE);

                getActivity().startActivity(updateIntent);
            case R.id.action_update_later:
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
