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

package butter.droid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.Bind;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.base.utils.PrefUtils;
import butter.droid.utils.ToolbarUtils;

public class TermsActivity extends ButterBaseActivity {

    public static final String TERMS_ACCEPTED = "terms_accepted";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_terms);
        setSupportActionBar(toolbar);

        ToolbarUtils.updateToolbarHeight(this, toolbar);
    }

    public void acceptClick(View v) {
        PrefUtils.save(this, TERMS_ACCEPTED, true);
        Intent overviewIntent = new Intent(this, MainActivity.class);
        startActivity(overviewIntent);
        finish();
    }

    public void leaveClick(View v) {
        finish();
    }

}
