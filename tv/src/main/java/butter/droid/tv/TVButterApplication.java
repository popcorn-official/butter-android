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

package butter.droid.tv;

import android.content.Context;

import butter.droid.base.BaseApplicationModule;
import butter.droid.base.ButterApplication;
import butter.droid.base.utils.VersionUtils;

public class TVButterApplication extends ButterApplication {

    private ApplicationComponent component;

    public static TVButterApplication getAppContext() {
        return (TVButterApplication) ButterApplication.getAppContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        component = DaggerApplicationComponent.builder()
                .baseApplicationModule(new BaseApplicationModule(this))
                .build();
        component.inject(this);
        super.onCreate();
    }

    @Override
    public void updateAvailable(String filePath) {
        if(!VersionUtils.isAndroidTV()) {
            super.updateAvailable(filePath);
        }
    }

    public ApplicationComponent getComponent() {
        return component;
    }
}
