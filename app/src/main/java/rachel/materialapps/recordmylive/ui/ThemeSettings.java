package rachel.materialapps.recordmylive.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import rachel.materialapps.recordmylive.R;

/** Persistent appearance preferences shared by application startup and the settings page. */
public final class ThemeSettings {
    private static final String PREFERENCES = "app_settings";
    public static final String DYNAMIC = "dynamic";
    public static final String[] COLOR_KEYS = {DYNAMIC, "celadon", "indigo", "rouge", "sandalwood"};
    public static final int[] COLOR_LABELS = {
            R.string.color_dynamic, R.string.color_celadon, R.string.color_indigo,
            R.string.color_rouge, R.string.color_sandalwood
    };
    private static final int[] COLOR_OVERLAYS = {
            0, R.style.ThemeOverlay_RecordMyLive_Celadon, R.style.ThemeOverlay_RecordMyLive_Indigo,
            R.style.ThemeOverlay_RecordMyLive_Rouge, R.style.ThemeOverlay_RecordMyLive_Sandalwood
    };

    private ThemeSettings() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public static int getNightMode(Context context) {
        return preferences(context).getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setNightMode(Context context, int mode) {
        preferences(context).edit().putInt("theme_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int getColorIndex(Context context) {
        String key = preferences(context).getString("theme_color", DYNAMIC);
        for (int i = 0; i < COLOR_KEYS.length; i++) {
            if (COLOR_KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    public static void setColorIndex(Context context, int index) {
        preferences(context).edit().putString("theme_color", COLOR_KEYS[index]).apply();
    }

    public static void applyColorOverlay(Context context) {
        int overlay = COLOR_OVERLAYS[getColorIndex(context)];
        if (overlay != 0) context.getTheme().applyStyle(overlay, true);
    }
}
