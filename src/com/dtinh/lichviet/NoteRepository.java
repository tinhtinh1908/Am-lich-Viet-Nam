package com.dtinh.lichviet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Map;
import java.util.Locale;
import java.util.TreeMap;

/** Local per-day note store. Export is explicit and user initiated. */
public final class NoteRepository {
    private static final String PREFS = "calendar_notes_v1";
    private static final int MAX_LENGTH = 240;

    private NoteRepository() {}

    public static String get(Context context, Calendar date) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(date), "").trim();
    }

    public static boolean has(Context context, Calendar date) {
        return !get(context, date).isEmpty();
    }

    public static void put(Context context, Calendar date, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() > MAX_LENGTH) clean = clean.substring(0, MAX_LENGTH).trim();
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit();
        if (clean.isEmpty()) editor.remove(key(date));
        else editor.putString(key(date), clean);
        editor.apply();
        refreshWidgets(context.getApplicationContext());
    }

    public static Map<String, String> exportAll(Context context) {
        Map<String, ?> values = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getAll();
        TreeMap<String, String> notes = new TreeMap<String, String>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (isNoteKey(entry.getKey()) && value instanceof String) {
                String clean = ((String) value).trim();
                if (!clean.isEmpty()) notes.put(entry.getKey(), clean);
            }
        }
        return notes;
    }

    public static int mergeAll(Context context, Map<String, String> notes) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit();
        int imported = 0;
        for (Map.Entry<String, String> entry : notes.entrySet()) {
            if (!isNoteKey(entry.getKey())) continue;
            String clean = entry.getValue() == null ? "" : entry.getValue().trim();
            if (clean.isEmpty()) continue;
            if (clean.length() > MAX_LENGTH) clean = clean.substring(0, MAX_LENGTH).trim();
            editor.putString(entry.getKey(), clean);
            imported++;
        }
        if (imported > 0) {
            editor.apply();
            refreshWidgets(context.getApplicationContext());
        }
        return imported;
    }

    private static String key(Calendar date) {
        return String.format(Locale.US, "note_%04d%02d%02d",
                date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH));
    }

    private static boolean isNoteKey(String value) {
        if (value == null || value.length() != 13 || !value.startsWith("note_")) return false;
        for (int i = 5; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static void refreshWidgets(Context context) {
        refresh(context, new ComponentName(context, VerticalDateWidgetProvider.class));
        refresh(context, new ComponentName(context, HorizontalDateWidgetProvider.class));
        refresh(context, new ComponentName(context, MonthCalendarWidgetProvider.class));
    }

    private static void refresh(Context context, ComponentName component) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(component);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}
