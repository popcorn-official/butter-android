package butter.droid.base.content.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butter.droid.base.BuildConfig;
import butter.droid.base.Constants;
import butter.droid.base.R;
import butter.droid.base.compat.SupportedArchitectures;
import butter.droid.base.fragments.dialog.ChangeLogDialogFragment;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.manager.updater.ButterUpdateManager;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.PrefUtils;
import butter.droid.base.utils.StorageUtils;
import butter.droid.base.vlc.VLCOptions;

public interface PreferencesHandler {

    String ABOUT = "about";

    enum SelectionMode {
        NORMAL, ADVANCED_CHOICE, SIMPLE_CHOICE, COLOR, NUMBER, DIRECTORY
    }

    void openListSelection(String title, String[] items, SelectionMode mode, Object currentValue, int lowLimit, int highLimit, OnSelectionListener onClickListener);

    void showMessage(String message);

    interface OnSelectionListener {
        void onSelection(int position, Object value);
    }

    class ItemsGenerator {

        public static List<PrefItem> generate(final PreferencesHandler handler, ButterUpdateManager updateManager, boolean isTV) {
            if(!(handler instanceof Context)) {
                return new ArrayList<>();
            }

            return generate((Context) handler, handler, updateManager, isTV);
        }

        public static List<PrefItem> generate(final Context context, final PreferencesHandler handler, final ButterUpdateManager updateManager, boolean isTV) {
            List<PrefItem> prefItems = new ArrayList<>();

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.general).build());

            final String[] items = { context.getString(R.string.title_movies), context.getString(R.string.title_shows), context.getString(R.string.title_anime) };
            final String[] hwItems = { context.getString(R.string.hw_automatic), context.getString(R.string.disabled), context.getString(R.string.hw_decoding), context.getString(R.string.hw_full) };
            final String[] qualities = context.getResources().getStringArray(R.array.video_qualities);
            final String[] pixelFormats = { context.getString(R.string.rgb16), context.getString(R.string.rgb32), context.getString(R.string.yuv) };

            if(!isTV)
            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_default_view)
                    .setTitleResource(R.string.default_view)
                    .setPreferenceKey(Prefs.DEFAULT_PROVIDER)
                    .setDefaultValue(ProviderManager.PROVIDER_TYPE_MOVIE)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.SIMPLE_CHOICE, item.getValue(), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(position);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return items[(Integer) item.getValue()];
                        }
                    })
                    .build());

            if(!isTV)
            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_default_player)
                    .setTitleResource(R.string.default_player)
                    .setPreferenceKey(Prefs.DEFAULT_PLAYER)
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            int currentPosition = 0;
                            String currentValue = item.getValue().toString();

                            final Map<String, String> players = DefaultPlayer.getVideoPlayerApps();
                            final String[] playerDatas = players.keySet().toArray(new String[players.size()]);
                            String[] items = new String[players.size() + 1];
                            items[0] = context.getString(R.string.internal_player);
                            for (int i = 0; i < playerDatas.length; i++) {
                                String playerData = playerDatas[i];
                                String playerName = players.get(playerData);

                                items[i + 1] = playerName;
                                if (playerData.equals(currentValue)) {
                                    currentPosition = i + 1;
                                }
                            }

                            handler.openListSelection(item.getTitle(), items, SelectionMode.ADVANCED_CHOICE, currentPosition, 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    if (position == 0) {
                                        DefaultPlayer.set("", "");
                                    } else {
                                        String playerData = playerDatas[position - 1];
                                        DefaultPlayer.set(players.get(playerData), playerData);
                                    }
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return PrefUtils.get(context, Prefs.DEFAULT_PLAYER_NAME, context.getString(R.string.internal_player));
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_action_quality)
                    .setTitleResource(R.string.quality)
                    .setPreferenceKey(Prefs.QUALITY_DEFAULT)
                    .hasNext(true)
                    .setDefaultValue("720p")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), qualities, SelectionMode.SIMPLE_CHOICE, Arrays.asList(qualities).indexOf(item.getValue()), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(qualities[position]);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return (String) item.getValue();
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_app_language)
                    .setTitleResource(R.string.i18n_language)
                    .setPreferenceKey(Prefs.LOCALE)
                    .hasNext(true)
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            int currentPosition = 0;
                            String currentValue = item.getValue().toString();

                            final String[] languages = context.getResources().getStringArray(R.array.translation_languages);
                            Arrays.sort(languages);

                            String[] items = new String[languages.length + 1];
                            items[0] = context.getString(R.string.device_language);
                            for (int i = 0; i < languages.length; i++) {
                                Locale locale = LocaleUtils.toLocale(languages[i]);
                                items[i + 1] = locale.getDisplayName(locale);
                                if (languages[i].equals(currentValue)) {
                                    currentPosition = i + 1;
                                }
                            }

                            handler.openListSelection(item.getTitle(), items, SelectionMode.ADVANCED_CHOICE, currentPosition, 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    if (position == 0) {
                                        item.clearValue();
                                    } else {
                                        item.saveValue(languages[position - 1]);
                                    }

                                    handler.showMessage(context.getString(R.string.restart_effect));
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            String langCode = item.getValue().toString();
                            if (langCode.isEmpty())
                                return context.getString(R.string.device_language);

                            Locale locale = LocaleUtils.toLocale(langCode);
                            return locale.getDisplayName(locale);
                        }
                    })
                    .build());

            if(!isTV)
            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_wifi_only)
                    .setTitleResource(R.string.stream_over_wifi_only)
                    .setPreferenceKey(Prefs.WIFI_ONLY)
                    .setDefaultValue(true)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            item.saveValue(!(boolean) item.getValue());
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            boolean enabled = (boolean) item.getValue();
                            return enabled ? context.getString(R.string.enabled) : context.getString(R.string.disabled);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.subtitles).build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_subtitle_color)
                    .setTitleResource(R.string.subtitle_color)
                    .setPreferenceKey(Prefs.SUBTITLE_COLOR)
                    .hasNext(true)
                    .setDefaultValue(Color.WHITE)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), null, SelectionMode.COLOR, item.getValue(), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return String.format("#%06X", 0xFFFFFF & (int) item.getValue());
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_subtitle_size)
                    .setTitleResource(R.string.subtitle_size)
                    .setPreferenceKey(Prefs.SUBTITLE_SIZE)
                    .hasNext(true)
                    .setDefaultValue(context.getResources().getInteger(R.integer.player_subtitles_default_text_size))
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.NUMBER, item.getValue(), 10, 60, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return Integer.toString((int) item.getValue());
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_subtitle_stroke_color)
                    .setTitleResource(R.string.subtitle_stroke_color)
                    .setPreferenceKey(Prefs.SUBTITLE_STROKE_COLOR)
                    .hasNext(true)
                    .setDefaultValue(Color.BLACK)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), null, SelectionMode.COLOR, item.getValue(), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return String.format("#%06X", 0xFFFFFF & (int) item.getValue());
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_subtitle_stroke_width)
                    .setTitleResource(R.string.subtitle_stroke_width)
                    .setPreferenceKey(Prefs.SUBTITLE_STROKE_WIDTH)
                    .hasNext(true)
                    .setDefaultValue(2)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.NUMBER, item.getValue(), 0, 5, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return Integer.toString((int) item.getValue());
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_subtitle_lang)
                    .setTitleResource(R.string.default_subtitle_language)
                    .setPreferenceKey(Prefs.SUBTITLE_DEFAULT)
                    .hasNext(true)
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            int currentPosition = 0;
                            String currentValue = item.getValue().toString();

                            final String[] languages = context.getResources().getStringArray(R.array.subtitle_languages);
                            String[] items = new String[languages.length + 1];
                            items[0] = context.getString(R.string.no_default_set);
                            for (int i = 0; i < languages.length; i++) {
                                Locale locale = LocaleUtils.toLocale(languages[i]);
                                items[i + 1] = locale.getDisplayName(locale);
                                if (languages[i].equals(currentValue)) {
                                    currentPosition = i + 1;
                                }
                            }

                            handler.openListSelection(item.getTitle(), items, SelectionMode.ADVANCED_CHOICE, currentPosition, 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    if (position == 0) {
                                        item.clearValue();
                                    } else {
                                        item.saveValue(languages[position - 1]);
                                    }
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            String langCode = item.getValue().toString();
                            if (langCode.isEmpty())
                                return context.getString(R.string.no_default_set);

                            Locale locale = LocaleUtils.toLocale(langCode);
                            return locale.getDisplayName(locale);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.torrents).build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_connections)
                    .setTitleResource(R.string.max_connections)
                    .setPreferenceKey(Prefs.LIBTORRENT_CONNECTION_LIMIT)
                    .hasNext(true)
                    .setDefaultValue(200)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.NUMBER, item.getValue(), 0, 200, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            int limit = (Integer) item.getValue();
                            return limit + " connections";
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_download_limit)
                    .setTitleResource(R.string.download_speed)
                    .setPreferenceKey(Prefs.LIBTORRENT_DOWNLOAD_LIMIT)
                    .hasNext(true)
                    .setDefaultValue(0)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.NUMBER, item.getValue(), 0, 3000, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            int limit = (Integer) item.getValue();
                            if (limit == 0) {
                                return context.getString(R.string.unlimited);
                            } else {
                                return (limit / 1000) + " kB/s";
                            }
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_upload_limit)
                    .setTitleResource(R.string.upload_speed)
                    .setPreferenceKey(Prefs.LIBTORRENT_UPLOAD_LIMIT)
                    .hasNext(true)
                    .setDefaultValue(0)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), items, SelectionMode.NUMBER, item.getValue(), 0, 3000, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(value);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            int limit = (Integer) item.getValue();
                            if (limit == 0) {
                                return context.getString(R.string.unlimited);
                            } else {
                                return (limit / 1000) + " kB/s";
                            }
                        }
                    })
                    .build());

            if(!isTV)
            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_storage_location)
                    .setTitleResource(R.string.storage_location)
                    .setPreferenceKey(Prefs.STORAGE_LOCATION)
                    .hasNext(true)
                    .setDefaultValue(StorageUtils.getIdealCacheDirectory(context))
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), null, SelectionMode.DIRECTORY, item.getValue(), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    if(value != null) {
                                        item.saveValue(value);
                                    } else {
                                        item.clearValue();
                                    }
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return item.getValue().toString();
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_remove_cache)
                    .setTitleResource(R.string.remove_cache)
                    .setPreferenceKey(Prefs.REMOVE_CACHE)
                    .setDefaultValue(true)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            item.saveValue(!(boolean) item.getValue());
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            boolean enabled = (boolean) item.getValue();
                            return enabled ? context.getString(R.string.enabled) : context.getString(R.string.disabled);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.advanced).build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_hw_accel)
                    .setTitleResource(R.string.hw_acceleration)
                    .setPreferenceKey(Prefs.HW_ACCELERATION)
                    .hasNext(true)
                    .setDefaultValue(VLCOptions.HW_ACCELERATION_AUTOMATIC)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.openListSelection(item.getTitle(), hwItems, SelectionMode.SIMPLE_CHOICE, ((Integer) item.getValue() + 1), 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    item.saveValue(position - 1);
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            switch ((int) item.getValue()) {
                                case VLCOptions.HW_ACCELERATION_DECODING:
                                    return context.getString(R.string.hw_decoding);
                                case VLCOptions.HW_ACCELERATION_DISABLED:
                                    return context.getString(R.string.disabled);
                                case VLCOptions.HW_ACCELERATION_FULL:
                                    return context.getString(R.string.hw_full);
                                default:
                                case VLCOptions.HW_ACCELERATION_AUTOMATIC:
                                    return context.getString(R.string.hw_automatic);
                            }
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_pixel_format)
                    .setTitleResource(R.string.pixel_format)
                    .setPreferenceKey(Prefs.PIXEL_FORMAT)
                    .hasNext(true)
                    .setDefaultValue("RV32")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            String currentValue = (String) item.getValue();
                            int current = 1;
                            if (currentValue.equals("YV12")) {
                                current = 2;
                            } else if (currentValue.equals("RV16")) {
                                current = 0;
                            }

                            handler.openListSelection(item.getTitle(), pixelFormats, SelectionMode.SIMPLE_CHOICE, current, 0, 0, new OnSelectionListener() {
                                @Override
                                public void onSelection(int position, Object value) {
                                    if(position == 2) {
                                        item.saveValue("YV12");
                                    } else if (position == 0) {
                                        item.saveValue("RV16");
                                    } else {
                                        item.saveValue("RV32");
                                    }
                                }
                            });
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            if (item.getValue().equals("YV12")) {
                                return context.getString(R.string.yuv);
                            } else if (item.getValue().equals("RV16")) {
                                return context.getString(R.string.rgb16);
                            }
                            return context.getString(R.string.rgb32);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_nav_vpn)
                    .setTitleResource(R.string.show_vpn)
                    .setPreferenceKey(Prefs.SHOW_VPN)
                    .setDefaultValue(true)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            item.saveValue(!(boolean) item.getValue());
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            boolean enabled = (boolean) item.getValue();
                            return enabled ? context.getString(R.string.enabled) : context.getString(R.string.disabled);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.updates).build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_auto_update)
                    .setTitleResource(R.string.auto_updates)
                    .setPreferenceKey(Prefs.AUTOMATIC_UPDATES)
                    .setDefaultValue(true)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            item.saveValue(!(boolean) item.getValue());
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            boolean enabled = (boolean) item.getValue();
                            return enabled ? context.getString(R.string.enabled) : context.getString(R.string.disabled);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_check_update)
                    .setTitleResource(R.string.check_for_updates)
                    .setPreferenceKey(ButterUpdateManager.LAST_UPDATE_CHECK)
                    .setDefaultValue(1)
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            updateManager.checkUpdatesManually();
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            long timeStamp = Long.parseLong(PrefUtils.get(context, ButterUpdateManager.LAST_UPDATE_CHECK, "0"));
                            Calendar cal = Calendar.getInstance(Locale.getDefault());
                            cal.setTimeInMillis(timeStamp);
                            String time = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()).format(timeStamp);
                            String date = DateFormat.format("dd-MM-yyy", cal).toString();
                            return context.getString(R.string.last_check) + ": " + date + " " + time;
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context).setTitleResource(R.string.about_app).build());

            if (!BuildConfig.DEBUG && !isTV) {
                prefItems.add(PrefItem.newBuilder(context)
                        .setIconResource(R.drawable.ic_prefs_report_bug)
                        .setTitleResource(R.string.report_a_bug)
                        .setPreferenceKey("")
                        .setDefaultValue("")
                        .setOnClickListener(new PrefItem.OnClickListener() {
                            @Override
                            public void onClick(final PrefItem item) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse("https://github.com/popcorn-official/popcorn-android/issues/new"));
                                context.startActivity(i);
                            }
                        })
                        .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                            @Override
                            public String get(PrefItem item) {
                                return context.getString(R.string.tap_to_open);
                            }
                        })
                        .build());
            }

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_changelog)
                    .setTitleResource(R.string.changelog)
                    .setPreferenceKey("")
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            ChangeLogDialogFragment changeLogDialogFragment = new ChangeLogDialogFragment();
                            changeLogDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "prefs_fragment");
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return context.getString(R.string.tap_to_open);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_open_source)
                    .setTitleResource(R.string.open_source_licenses)
                    .setPreferenceKey("")
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(Constants.GIT_URL + "/blob/master/NOTICE.md"));
                            context.startActivity(i);
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return context.getString(R.string.tap_to_open);
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_version)
                    .setTitleResource(R.string.version)
                    .setPreferenceKey("")
                    .setDefaultValue("")
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            try {
                                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                                return packageInfo.versionName + " - " + SupportedArchitectures.getAbi();
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                            return "?.? (?) - ?";
                        }
                    })
                    .build());

            prefItems.add(PrefItem.newBuilder(context)
                    .setIconResource(R.drawable.ic_prefs_about)
                    .setTitleResource(R.string.about_app)
                    .setPreferenceKey("")
                    .setDefaultValue("")
                    .setOnClickListener(new PrefItem.OnClickListener() {
                        @Override
                        public void onClick(final PrefItem item) {
                            handler.showMessage(ABOUT);
                        }
                    })
                    .setSubtitleGenerator(new PrefItem.SubtitleGenerator() {
                        @Override
                        public String get(PrefItem item) {
                            return context.getString(R.string.tap_to_open);
                        }
                    })
                    .build());

            return prefItems;
        }

    }

}
