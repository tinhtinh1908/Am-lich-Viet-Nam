package com.dtinh.lichviet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

/** Standard Android CalendarContract/intent interoperability. */
public final class SystemCalendarCompat {
    private SystemCalendarCompat() {}

    public static Long resolveDateMillis(Context context, Intent intent) {
        if (intent == null) return null;
        long extra = intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, Long.MIN_VALUE);
        if (extra != Long.MIN_VALUE) return Long.valueOf(extra);
        Uri data = intent.getData();
        if (data == null) return null;
        String last = data.getLastPathSegment();
        if ("time".equals(data.getHost()) || "time".equals(firstSegment(data))) {
            try { return Long.valueOf(Long.parseLong(last)); } catch (RuntimeException ignored) {}
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())
                && CalendarContract.AUTHORITY.equals(data.getAuthority())
                && context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED) {
            try (Cursor cursor = context.getContentResolver().query(data,
                    new String[]{CalendarContract.Events.DTSTART}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) return Long.valueOf(cursor.getLong(0));
            } catch (RuntimeException ignored) {}
        }
        return null;
    }

    private static String firstSegment(Uri uri) {
        return uri.getPathSegments().isEmpty() ? "" : uri.getPathSegments().get(0);
    }
}
