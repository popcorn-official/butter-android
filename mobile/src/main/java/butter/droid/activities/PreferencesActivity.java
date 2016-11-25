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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.activities.base.ButterBaseActivity;
import butter.droid.adapters.PreferencesListAdapter;
import butter.droid.base.content.preferences.PrefItem;
import butter.droid.base.content.preferences.PreferencesHandler;
import butter.droid.base.fragments.dialog.NumberPickerDialogFragment;
import butter.droid.base.fragments.dialog.StringArraySelectorDialogFragment;
import butter.droid.base.manager.updater.ButterUpdateManager;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.ResourceUtils;
import butter.droid.fragments.dialog.ColorPickerDialogFragment;
import butter.droid.fragments.dialog.SeekBarDialogFragment;
import butter.droid.utils.ToolbarUtils;
import butterknife.BindView;

public class PreferencesActivity extends ButterBaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener, PreferencesHandler {

    private List<PrefItem> mPrefItems = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;

    @Inject
    ButterUpdateManager updateManager;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.rootLayout)
    ViewGroup rootLayout;

    public static Intent startActivity(Activity activity) {
        Intent intent = new Intent(activity, PreferencesActivity.class);
        activity.startActivity(intent);
        return intent;
    }

    public static Intent getIntent(Context context) {
        return new Intent(context, PreferencesActivity.class);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        PrefUtils.getPrefs(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);

        super.onCreate(savedInstanceState, R.layout.activity_preferences);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.preferences);

        ToolbarUtils.updateToolbarHeight(this, toolbar);

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);

        PrefUtils.getPrefs(this).registerOnSharedPreferenceChangeListener(this);

        refreshItems();
    }

    private void refreshItems() {
        mPrefItems = ItemsGenerator.generate(this, updateManager, false);

        if (recyclerView.getAdapter() != null && mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            View v = mLayoutManager.findViewByPosition(position);
            recyclerView.setAdapter(new PreferencesListAdapter(mPrefItems));
            if (v != null) {
                int offset = v.getTop();
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            } else {
                mLayoutManager.scrollToPosition(position);
            }
        } else {
            recyclerView.setAdapter(new PreferencesListAdapter(mPrefItems));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshItems();
        toolbar.setMinimumHeight((int) ResourceUtils.getAttributeDimension(this, this.getTheme(), R.attr.actionBarSize));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isUseChangeablePref(key)) {
            refreshItems();
        }
    }

    private boolean isUseChangeablePref(String key) {
        boolean b = false;
        for (Object item : mPrefItems) {
            if (item instanceof PrefItem) {
                PrefItem pref = (PrefItem) item;
                if (pref.getPrefKey() != null && pref.getPrefKey().equals(key))
                    b = true;
            }
        }
        return b;
    }

    @Override
    public void openListSelection(String title, String[] items, SelectionMode mode, Object value, int low, int high, final OnSelectionListener onSelectionListener) {
        DialogInterface.OnClickListener onDialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSelectionListener.onSelection(which, null);
                dialog.dismiss();
            }
        };

        if (mode == SelectionMode.NORMAL) {
            StringArraySelectorDialogFragment.show(getSupportFragmentManager(), title, items, (Integer) value, onDialogClickListener);
        } else if(mode == SelectionMode.ADVANCED_CHOICE || mode == SelectionMode.SIMPLE_CHOICE) {
            StringArraySelectorDialogFragment.showSingleChoice(getSupportFragmentManager(), title, items, (Integer) value, onDialogClickListener);
        } else if(mode == SelectionMode.COLOR) {
            Bundle args = new Bundle();
            args.putString(ColorPickerDialogFragment.TITLE, title);
            args.putInt(ColorPickerDialogFragment.DEFAULT_VALUE, (Integer) value);

            ColorPickerDialogFragment dialogFragment = new ColorPickerDialogFragment();
            dialogFragment.setArguments(args);
            dialogFragment.setOnResultListener(new ColorPickerDialogFragment.ResultListener() {
                @Override
                public void onNewValue(int value) {
                    onSelectionListener.onSelection(0, value);
                }
            });
            dialogFragment.show(getFragmentManager(), "pref_fragment");
        } else if(mode == SelectionMode.DIRECTORY) {
            String[] directoryOptions = {getString(R.string.storage_automatic), getString(R.string.storage_choose)};

            StringArraySelectorDialogFragment.show(getSupportFragmentManager(), title, directoryOptions, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int position) {
                    if (position == 0) {
                        onSelectionListener.onSelection(0, null);
                    } else {
                        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                                .newDirectoryName(getString(R.string.app_name))
                                .build();

                        final DirectoryChooserFragment directoryChooserFragment = DirectoryChooserFragment.newInstance(config);
                        directoryChooserFragment.setDirectoryChooserListener(new DirectoryChooserFragment.OnFragmentInteractionListener() {
                            @Override
                            public void onSelectDirectory(String s) {
                                File f = new File(s);
                                if (f.canWrite()) {
                                    onSelectionListener.onSelection(0, s);
                                    directoryChooserFragment.dismiss();
                                } else {
                                    Toast.makeText(PreferencesActivity.this, R.string.not_writable, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelChooser() {
                                directoryChooserFragment.dismiss();
                            }
                        });

                        dialog.dismiss();
                        directoryChooserFragment.show(getFragmentManager(), "pref_fragment");
                    }
                }
            });
        } else if(mode == SelectionMode.NUMBER && high - low > 200) {
            Bundle args = new Bundle();
            args.putString(SeekBarDialogFragment.TITLE, title);
            args.putInt(SeekBarDialogFragment.MAX_VALUE, high);
            args.putInt(SeekBarDialogFragment.MIN_VALUE, low);
            args.putInt(SeekBarDialogFragment.DEFAULT_VALUE, ((Integer) value) / 1000);

            SeekBarDialogFragment dialogFragment = new SeekBarDialogFragment();
            dialogFragment.setArguments(args);
            dialogFragment.setOnResultListener(new SeekBarDialogFragment.ResultListener() {
                @Override
                public void onNewValue(int value) {
                    onSelectionListener.onSelection(0, value);
                }
            });
            dialogFragment.show(getFragmentManager(), "pref_fragment");
        } else if(mode == SelectionMode.NUMBER) {
            Bundle args = new Bundle();
            args.putString(NumberPickerDialogFragment.TITLE, title);
            args.putInt(NumberPickerDialogFragment.MAX_VALUE, high);
            args.putInt(NumberPickerDialogFragment.MIN_VALUE, low);
            args.putInt(NumberPickerDialogFragment.DEFAULT_VALUE, (int) value);

            NumberPickerDialogFragment dialogFragment = new NumberPickerDialogFragment();
            dialogFragment.setArguments(args);
            dialogFragment.setOnResultListener(new NumberPickerDialogFragment.ResultListener() {
                @Override
                public void onNewValue(int value) {
                    onSelectionListener.onSelection(0, value);
                }
            });
            dialogFragment.show(getSupportFragmentManager(), "pref_fragment");
        }
    }

    @Override
    public void showMessage(String message) {
        if(message.equals(PreferencesHandler.ABOUT)) {
            AboutActivity.startActivity(this);
            return;
        }
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
    }
}
