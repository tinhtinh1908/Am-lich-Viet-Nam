package com.dtinh.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;

/** Fixed-layout base for the two compact widgets; no adaptive RemoteViews maps. */
public abstract class CalendarWidgetProvider extends AppWidgetProvider {
    private static final String[] WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    protected abstract int layoutId();

    protected abstract Class<? extends CalendarWidgetProvider> providerClass();

    @Override
    public final void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        DynamicIconManager.update(context);
        for (int widgetId : appWidgetIds) updateWidget(context, manager, widgetId);
    }

    @Override
    public final void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                int appWidgetId, Bundle newOptions) {
        updateWidget(context, manager, appWidgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, providerClass()));
            onUpdate(context, manager, ids);
        }
    }

    private void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String holiday = HolidayUtil.getHoliday(day, month, lunar);
        String note = NoteRepository.get(context, now);
        boolean vertical = layoutId() == R.layout.calendar_widget_vertical;

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId());
        views.setTextViewText(R.id.widget_weekday,
                WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1]);
        views.setTextViewText(R.id.widget_day, Integer.toString(day));
        views.setTextViewText(R.id.widget_month_year,
                vertical ? "THÁNG " + month : "THÁNG " + month + ", " + year);
        views.setTextViewText(R.id.widget_lunar,
                "Âm " + lunar.day + "/" + lunar.month + (lunar.leap ? "N" : ""));

        String detail;
        int color;
        if (!holiday.isEmpty()) {
            detail = holiday;
            color = context.getColor(R.color.widget_sunday);
        } else if (!note.isEmpty()) {
            detail = note;
            color = context.getColor(R.color.widget_blue);
        } else {
            detail = LunarCalendar.yearCanChi(lunar.year);
            color = context.getColor(R.color.widget_secondary);
        }
        views.setTextViewText(R.id.widget_holiday, detail);
        views.setTextColor(R.id.widget_holiday, color);
        views.setViewVisibility(R.id.widget_holiday, View.VISIBLE);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, widgetId, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pending);
        manager.updateAppWidget(widgetId, views);
    }
}
