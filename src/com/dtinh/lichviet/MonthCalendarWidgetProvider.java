package com.dtinh.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.Calendar;

/** Full 4x4 Vietnamese month widget with solar dates, lunar dates and holidays. */
public final class MonthCalendarWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_PREVIOUS =
            "com.dtinh.lichviet.widget.MONTH_PREVIOUS";
    private static final String ACTION_NEXT =
            "com.dtinh.lichviet.widget.MONTH_NEXT";
    private static final String ACTION_TODAY =
            "com.dtinh.lichviet.widget.MONTH_TODAY";
    private static final String PREFS = "month_widget";
    private static final String KEY_OFFSET = "offset_";

    private static final int[] DAY_IDS = {
            R.id.month_day_01,
            R.id.month_day_02,
            R.id.month_day_03,
            R.id.month_day_04,
            R.id.month_day_05,
            R.id.month_day_06,
            R.id.month_day_07,
            R.id.month_day_08,
            R.id.month_day_09,
            R.id.month_day_10,
            R.id.month_day_11,
            R.id.month_day_12,
            R.id.month_day_13,
            R.id.month_day_14,
            R.id.month_day_15,
            R.id.month_day_16,
            R.id.month_day_17,
            R.id.month_day_18,
            R.id.month_day_19,
            R.id.month_day_20,
            R.id.month_day_21,
            R.id.month_day_22,
            R.id.month_day_23,
            R.id.month_day_24,
            R.id.month_day_25,
            R.id.month_day_26,
            R.id.month_day_27,
            R.id.month_day_28,
            R.id.month_day_29,
            R.id.month_day_30,
            R.id.month_day_31,
            R.id.month_day_32,
            R.id.month_day_33,
            R.id.month_day_34,
            R.id.month_day_35,
            R.id.month_day_36,
            R.id.month_day_37,
            R.id.month_day_38,
            R.id.month_day_39,
            R.id.month_day_40,
            R.id.month_day_41,
            R.id.month_day_42
    };

    private static final int[] LUNAR_IDS = {
            R.id.month_lunar_01,
            R.id.month_lunar_02,
            R.id.month_lunar_03,
            R.id.month_lunar_04,
            R.id.month_lunar_05,
            R.id.month_lunar_06,
            R.id.month_lunar_07,
            R.id.month_lunar_08,
            R.id.month_lunar_09,
            R.id.month_lunar_10,
            R.id.month_lunar_11,
            R.id.month_lunar_12,
            R.id.month_lunar_13,
            R.id.month_lunar_14,
            R.id.month_lunar_15,
            R.id.month_lunar_16,
            R.id.month_lunar_17,
            R.id.month_lunar_18,
            R.id.month_lunar_19,
            R.id.month_lunar_20,
            R.id.month_lunar_21,
            R.id.month_lunar_22,
            R.id.month_lunar_23,
            R.id.month_lunar_24,
            R.id.month_lunar_25,
            R.id.month_lunar_26,
            R.id.month_lunar_27,
            R.id.month_lunar_28,
            R.id.month_lunar_29,
            R.id.month_lunar_30,
            R.id.month_lunar_31,
            R.id.month_lunar_32,
            R.id.month_lunar_33,
            R.id.month_lunar_34,
            R.id.month_lunar_35,
            R.id.month_lunar_36,
            R.id.month_lunar_37,
            R.id.month_lunar_38,
            R.id.month_lunar_39,
            R.id.month_lunar_40,
            R.id.month_lunar_41,
            R.id.month_lunar_42
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        DynamicIconManager.update(context);
        for (int widgetId : appWidgetIds) updateWidget(context, manager, widgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        DynamicIconManager.update(context);
        String action = intent.getAction();
        if (ACTION_PREVIOUS.equals(action) || ACTION_NEXT.equals(action)
                || ACTION_TODAY.equals(action)) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;
            SharedPreferences preferences = preferences(context);
            int offset = preferences.getInt(KEY_OFFSET + widgetId, 0);
            if (ACTION_PREVIOUS.equals(action)) offset--;
            if (ACTION_NEXT.equals(action)) offset++;
            if (ACTION_TODAY.equals(action)) offset = 0;
            offset = Math.max(-1200, Math.min(1200, offset));
            preferences.edit().putInt(KEY_OFFSET + widgetId, offset).apply();
            updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
            return;
        }

        if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new android.content.ComponentName(context, MonthCalendarWidgetProvider.class));
            onUpdate(context, manager, ids);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences.Editor editor = preferences(context).edit();
        for (int widgetId : appWidgetIds) editor.remove(KEY_OFFSET + widgetId);
        editor.apply();
        super.onDeleted(context, appWidgetIds);
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        int offset = preferences(context).getInt(KEY_OFFSET + widgetId, 0);
        Calendar displayed = Calendar.getInstance();
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        displayed.add(Calendar.MONTH, offset);

        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.month_calendar_widget);
        views.setTextViewText(R.id.month_widget_title,
                "Tháng " + (displayed.get(Calendar.MONTH) + 1) + ", "
                        + displayed.get(Calendar.YEAR));

        Calendar cursor = (Calendar) displayed.clone();
        int mondayOffset = (cursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cursor.add(Calendar.DAY_OF_MONTH, -mondayOffset);
        Calendar today = Calendar.getInstance();

        int primary = context.getColor(R.color.widget_text);
        int secondary = context.getColor(R.color.widget_secondary);
        int muted = context.getColor(R.color.widget_muted);
        int sunday = context.getColor(R.color.widget_sunday);
        int accent = context.getColor(R.color.widget_blue);
        int white = context.getColor(R.color.white);

        for (int index = 0; index < 42; index++) {
            int day = cursor.get(Calendar.DAY_OF_MONTH);
            int month = cursor.get(Calendar.MONTH) + 1;
            int year = cursor.get(Calendar.YEAR);
            boolean inMonth = cursor.get(Calendar.MONTH) == displayed.get(Calendar.MONTH)
                    && cursor.get(Calendar.YEAR) == displayed.get(Calendar.YEAR);
            boolean isToday = sameDate(cursor, today);
            boolean isSunday = cursor.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
            String holiday = HolidayUtil.getHoliday(day, month, lunar);
            boolean hasNote = NoteRepository.has(context, cursor);

            views.setTextViewText(DAY_IDS[index], Integer.toString(day));
            String lunarLabel = HolidayUtil.getShortLabel(day, month, lunar);
            views.setTextViewText(LUNAR_IDS[index], hasNote ? lunarLabel + " •" : lunarLabel);
            views.setTextColor(DAY_IDS[index],
                    isToday ? white : (!inMonth ? muted : (isSunday ? sunday : primary)));
            views.setTextColor(LUNAR_IDS[index],
                    !inMonth ? muted : (!holiday.isEmpty() ? sunday
                            : (hasNote ? accent : secondary)));
            views.setInt(DAY_IDS[index], "setBackgroundResource",
                    isToday ? R.drawable.widget_today_circle : 0);
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        views.setOnClickPendingIntent(R.id.month_widget_root, openApp(context, widgetId));
        views.setOnClickPendingIntent(R.id.month_widget_prev,
                widgetAction(context, widgetId, ACTION_PREVIOUS, 1));
        views.setOnClickPendingIntent(R.id.month_widget_today,
                widgetAction(context, widgetId, ACTION_TODAY, 2));
        views.setOnClickPendingIntent(R.id.month_widget_next,
                widgetAction(context, widgetId, ACTION_NEXT, 3));
        manager.updateAppWidget(widgetId, views);
    }

    private static PendingIntent openApp(Context context, int widgetId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 4000 + widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent widgetAction(Context context, int widgetId,
                                               String action, int requestOffset) {
        Intent intent = new Intent(context, MonthCalendarWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse("lichviet://month-widget/" + widgetId + "/" + requestOffset));
        return PendingIntent.getBroadcast(context, widgetId * 10 + requestOffset, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static boolean sameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
}
