package com.dtinh.lichviet;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.Window;

public final class MainActivity extends Activity {
    private CalendarMonthView calendarView;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicIconManager.update(this);
        Window window = getWindow();
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int chrome = night ? Color.rgb(18, 19, 23) : Color.rgb(247, 248, 252);
        window.setStatusBarColor(chrome);
        window.setNavigationBarColor(chrome);
        window.getDecorView().setSystemUiVisibility(night ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        calendarView = new CalendarMonthView(this);
        setContentView(calendarView);
        applyIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        if (calendarView == null) return;
        if (XiaomiNotesBackup.handleIncomingShare(this, intent, new Runnable() {
            @Override public void run() { calendarView.invalidate(); }
        })) {
            setIntent(new Intent(this, MainActivity.class));
            return;
        }
        Long time = SystemCalendarCompat.resolveDateMillis(this, intent);
        if (time != null) calendarView.showDate(time.longValue());
    }

    @Override
    protected void onResume() {
        super.onResume();
        DynamicIconManager.update(this);
        if (calendarView != null) calendarView.refreshToday();
    }
}
