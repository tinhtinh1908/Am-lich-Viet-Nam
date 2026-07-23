package com.dtinh.lichviet;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Minimal native event editor used by standard Android calendar INSERT/EDIT intents. */
public final class SystemEventActivity extends Activity {
    private static final int REQUEST_CALENDAR = 460;
    private static final Locale VI = new Locale("vi", "VN");

    private final Calendar begin = Calendar.getInstance();
    private final Calendar end = Calendar.getInstance();
    private EditText title;
    private EditText location;
    private EditText description;
    private Switch allDay;
    private Button beginButton;
    private Button endButton;
    private Uri editUri;
    private boolean pendingSave;
    private boolean existingLoaded;
    private int primary;
    private int secondary;
    private int surface;
    private int accent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        primary = night ? Color.rgb(245, 247, 250) : Color.rgb(24, 27, 34);
        secondary = night ? Color.rgb(168, 172, 182) : Color.rgb(100, 105, 116);
        surface = night ? Color.rgb(29, 31, 37) : Color.WHITE;
        accent = Color.rgb(66, 133, 244);
        getWindow().setStatusBarColor(night ? Color.rgb(18, 19, 23) : Color.rgb(247, 248, 252));
        getWindow().setNavigationBarColor(getWindow().getStatusBarColor());

        end.add(Calendar.HOUR_OF_DAY, 1);
        readIntentDefaults(getIntent());
        buildUi(night);
        if (editUri != null) requestOrLoadExisting();
    }

    private void buildUi(boolean night) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(night ? Color.rgb(18, 19, 23) : Color.rgb(247, 248, 252));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView heading = text(editUri == null ? "Thêm sự kiện" : "Sửa sự kiện", 25, primary);
        heading.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(heading, row(-1, dp(58)));

        title = input(R.string.event_title_hint, true);
        location = input(R.string.event_location_hint, false);
        description = input(R.string.event_description_hint, false);
        root.addView(title, row(-1, dp(58)));
        root.addView(location, row(-1, dp(54)));

        allDay = new Switch(this);
        allDay.setText(R.string.event_all_day);
        allDay.setTextColor(primary);
        allDay.setTextSize(16);
        allDay.setPadding(dp(3), dp(8), dp(3), dp(8));
        allDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshDateButtons();
            }
        });
        root.addView(allDay, row(-1, dp(56)));

        beginButton = dateButton();
        endButton = dateButton();
        beginButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { pickDateTime(begin); }
        });
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { pickDateTime(end); }
        });
        root.addView(label("Bắt đầu"));
        root.addView(beginButton, row(-1, dp(50)));
        root.addView(label("Kết thúc"));
        root.addView(endButton, row(-1, dp(50)));
        root.addView(description, row(-1, dp(100)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button cancel = actionButton(getString(R.string.event_cancel), secondary);
        Button save = actionButton(getString(R.string.event_save), Color.WHITE);
        save.setBackgroundColor(accent);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { saveEvent(); }
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(52), 1));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(52), 1.35f);
        saveParams.setMarginStart(dp(10));
        actions.addView(save, saveParams);
        LinearLayout.LayoutParams actionRow = row(-1, dp(52));
        actionRow.topMargin = dp(22);
        root.addView(actions, actionRow);
        setContentView(scroll);
        refreshDateButtons();
    }

    private void readIntentDefaults(Intent intent) {
        long start = intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                begin.getTimeInMillis());
        long finish = intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                start + 60L * 60L * 1000L);
        begin.setTimeInMillis(start);
        end.setTimeInMillis(Math.max(finish, start + 60L * 1000L));
        allDayValue = intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false);
        Uri data = intent.getData();
        if (Intent.ACTION_EDIT.equals(intent.getAction()) && isEventUri(data)) editUri = data;
    }

    private boolean allDayValue;

    private void requestOrLoadExisting() {
        if (hasCalendarPermission()) loadExisting();
        else requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR}, REQUEST_CALENDAR);
    }

    private void loadExisting() {
        if (existingLoaded || editUri == null) return;
        String[] projection = {CalendarContract.Events.TITLE, CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DESCRIPTION, CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND, CalendarContract.Events.ALL_DAY};
        try (Cursor cursor = getContentResolver().query(editUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                title.setText(cursor.getString(0));
                location.setText(cursor.getString(1));
                description.setText(cursor.getString(2));
                begin.setTimeInMillis(cursor.getLong(3));
                end.setTimeInMillis(cursor.getLong(4));
                allDay.setChecked(cursor.getInt(5) != 0);
                existingLoaded = true;
                refreshDateButtons();
            }
        } catch (RuntimeException ignored) {
            Toast.makeText(this, R.string.calendar_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private void saveEvent() {
        if (!hasCalendarPermission()) {
            pendingSave = true;
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR}, REQUEST_CALENDAR);
            return;
        }
        String eventTitle = title.getText().toString().trim();
        if (eventTitle.isEmpty()) {
            title.setError(getString(R.string.event_title_hint));
            return;
        }
        if (end.before(begin)) end.setTimeInMillis(begin.getTimeInMillis() + 60L * 60L * 1000L);
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.TITLE, eventTitle);
        values.put(CalendarContract.Events.EVENT_LOCATION, location.getText().toString().trim());
        values.put(CalendarContract.Events.DESCRIPTION, description.getText().toString().trim());
        values.put(CalendarContract.Events.DTSTART, begin.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND, end.getTimeInMillis());
        values.put(CalendarContract.Events.ALL_DAY, allDay.isChecked() ? 1 : 0);
        String zone = TimeZone.getDefault().getID();
        values.put(CalendarContract.Events.EVENT_TIMEZONE, zone);
        values.put(CalendarContract.Events.EVENT_END_TIMEZONE, zone);
        try {
            Uri result;
            if (editUri != null) {
                int changed = getContentResolver().update(editUri, values, null, null);
                result = changed > 0 ? editUri : null;
            } else {
                long calendarId = writableCalendarId();
                if (calendarId < 0) {
                    Toast.makeText(this, R.string.calendar_not_writable, Toast.LENGTH_LONG).show();
                    return;
                }
                values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
                result = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
            }
            if (result == null) throw new IllegalStateException("Calendar provider rejected event");
            setResult(RESULT_OK, new Intent().setData(result));
            Toast.makeText(this, R.string.event_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.calendar_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private long writableCalendarId() {
        String[] projection = {CalendarContract.Calendars._ID};
        String selection = CalendarContract.Calendars.VISIBLE + "=1 AND "
                + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=?";
        String[] args = {Integer.toString(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)};
        try (Cursor cursor = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                projection, selection, args, CalendarContract.Calendars.IS_PRIMARY + " DESC")) {
            return cursor != null && cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    private void pickDateTime(final Calendar value) {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                value.set(year, month, day);
                if (allDay.isChecked()) {
                    refreshDateButtons();
                } else {
                    new TimePickerDialog(SystemEventActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker picker, int hour, int minute) {
                                    value.set(Calendar.HOUR_OF_DAY, hour);
                                    value.set(Calendar.MINUTE, minute);
                                    refreshDateButtons();
                                }
                            }, value.get(Calendar.HOUR_OF_DAY),
                            value.get(Calendar.MINUTE), true).show();
                }
            }
        }, value.get(Calendar.YEAR), value.get(Calendar.MONTH),
                value.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void refreshDateButtons() {
        if (allDay == null || beginButton == null) return;
        allDay.setChecked(allDay.isChecked() || allDayValue);
        allDayValue = false;
        String pattern = allDay.isChecked() ? "EEEE, dd/MM/yyyy" : "EEEE, dd/MM/yyyy · HH:mm";
        SimpleDateFormat format = new SimpleDateFormat(pattern, VI);
        beginButton.setText(format.format(begin.getTime()));
        endButton.setText(format.format(end.getTime()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_CALENDAR) return;
        if (hasCalendarPermission()) {
            loadExisting();
            if (pendingSave) {
                pendingSave = false;
                saveEvent();
            }
        } else {
            pendingSave = false;
            Toast.makeText(this, R.string.calendar_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasCalendarPermission() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isEventUri(Uri uri) {
        return uri != null && CalendarContract.AUTHORITY.equals(uri.getAuthority())
                && uri.getPathSegments().contains("events");
    }

    private EditText input(int hint, boolean singleLine) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setHintTextColor(secondary);
        view.setTextColor(primary);
        view.setTextSize(16);
        view.setSingleLine(singleLine);
        view.setBackgroundColor(surface);
        view.setPadding(dp(14), dp(8), dp(14), dp(8));
        return view;
    }

    private Button dateButton() {
        Button view = new Button(this);
        view.setAllCaps(false);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        view.setTextColor(primary);
        view.setTextSize(15);
        view.setBackgroundColor(surface);
        return view;
    }

    private Button actionButton(String value, int color) {
        Button view = new Button(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(13);
        view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 12, secondary);
        view.setPadding(dp(3), dp(12), 0, dp(4));
        return view;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams row(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
