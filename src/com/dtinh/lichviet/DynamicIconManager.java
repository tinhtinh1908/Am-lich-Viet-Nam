package com.dtinh.lichviet;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.util.Calendar;
import java.util.Locale;

/** Switches the launcher alias so the app icon shows today's date. */
public final class DynamicIconManager {
    private static final String PREFS = "dynamic_icon";
    private static final String KEY_ALIAS = "active_alias";
    private static final String DEFAULT_ALIAS = "IconDefault";
    private static final String DAY_ALIAS_PREFIX = "IconDay";

    private DynamicIconManager() {}

    public static void update(Context context) {
        Context app = context.getApplicationContext();
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        String desiredAlias = DAY_ALIAS_PREFIX + String.format(Locale.US, "%02d", day);
        SharedPreferences preferences = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String previousAlias = preferences.getString(KEY_ALIAS, DEFAULT_ALIAS);
        if (desiredAlias.equals(previousAlias) && isExplicitlyEnabled(app, desiredAlias)) return;

        PackageManager manager = app.getPackageManager();
        try {
            setAlias(manager, app, desiredAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            if (!desiredAlias.equals(previousAlias)) {
                setAlias(manager, app, previousAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
            if (!DEFAULT_ALIAS.equals(previousAlias)) {
                setAlias(manager, app, DEFAULT_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
            preferences.edit().putString(KEY_ALIAS, desiredAlias).apply();
        } catch (RuntimeException ignored) {
            // Some launchers delay alias refresh; the next resume or date broadcast retries it.
        }
    }

    private static boolean isExplicitlyEnabled(Context context, String alias) {
        ComponentName component = new ComponentName(context,
                context.getPackageName() + "." + alias);
        return context.getPackageManager().getComponentEnabledSetting(component)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private static void setAlias(PackageManager manager, Context context, String alias, int state) {
        ComponentName component = new ComponentName(context,
                context.getPackageName() + "." + alias);
        manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP);
    }
}
