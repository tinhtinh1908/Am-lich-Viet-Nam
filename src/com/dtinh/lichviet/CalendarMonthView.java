package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Native month screen combining Samsung's compact hierarchy with Xiaomi-style surfaces. */
public final class CalendarMonthView extends View {
    private static final Locale VI = new Locale("vi", "VN");
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();
    private final Calendar displayed = Calendar.getInstance();
    private final Calendar selected = Calendar.getInstance();
    private Calendar today = Calendar.getInstance();
    private final List<DayCell> dayCells = new ArrayList<DayCell>(42);
    private final float density;
    private final boolean night;

    private int background;
    private int surface;
    private int divider;
    private int textPrimary;
    private int textSecondary;
    private int textMuted;
    private int accent;
    private int accentSoft;
    private int sunday;

    private float headerBottom;
    private float weekTop;
    private float gridTop;
    private float gridBottom;
    private float rowHeight;
    private float cellWidth;
    private int visibleRows;
    private final RectF previousMonthButton = new RectF();
    private final RectF nextMonthButton = new RectF();
    private final RectF previousDayButton = new RectF();
    private final RectF todayButton = new RectF();
    private final RectF nextDayButton = new RectF();
    private final RectF backupButton = new RectF();
    private final RectF pickerButton = new RectF();
    private final RectF detailPanel = new RectF();
    private float downX;
    private float downY;
    private boolean moved;
    private ValueAnimator selectionAnimator;
    private float selectionPulse;
    private ValueAnimator monthAnimator;
    private Calendar outgoingDisplayed;
    private Calendar outgoingSelected;
    private float monthProgress = 1f;
    private int monthDirection = 1;

    public CalendarMonthView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        loadPalette();
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        normalize(displayed);
        normalize(selected);
        normalize(today);
        setBackgroundColor(background);
        setFocusable(true);
        setContentDescription("Lịch tháng Việt Nam");
    }

    private void loadPalette() {
        if (night) {
            background = Color.rgb(18, 19, 23);
            surface = Color.rgb(29, 31, 37);
            divider = Color.rgb(49, 52, 60);
            textPrimary = Color.rgb(245, 247, 250);
            textSecondary = Color.rgb(168, 172, 182);
            textMuted = Color.rgb(82, 86, 96);
            accent = Color.rgb(91, 150, 247);
            accentSoft = Color.rgb(38, 58, 87);
            sunday = Color.rgb(244, 116, 116);
        } else {
            background = Color.rgb(247, 248, 252);
            surface = Color.WHITE;
            divider = Color.rgb(229, 232, 238);
            textPrimary = Color.rgb(24, 27, 34);
            textSecondary = Color.rgb(113, 118, 130);
            textMuted = Color.rgb(189, 193, 202);
            accent = Color.rgb(66, 133, 244);
            accentSoft = Color.rgb(225, 236, 255);
            sunday = Color.rgb(221, 75, 75);
        }
    }

    public void refreshToday() {
        Calendar current = Calendar.getInstance();
        normalize(current);
        if (!sameDate(current, today)) {
            today = current;
            invalidate();
        }
    }

    /** Selects a date supplied by Android Calendar intents or another calendar app. */
    public void showDate(long timeMillis) {
        selected.setTimeInMillis(timeMillis);
        normalize(selected);
        displayed.setTimeInMillis(selected.getTimeInMillis());
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        normalize(displayed);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calculateLayout();
        if (outgoingDisplayed != null && monthProgress < 1f) {
            float sceneBottom = detailPanel.bottom + dp(1);
            float oldScale = 1f - monthProgress * 0.035f;
            float oldOffset = -monthDirection * getWidth() * 0.10f * monthProgress;
            int oldAlpha = Math.round(255f * (1f - monthProgress * 0.55f));
            drawTransitionLayer(canvas, outgoingDisplayed, outgoingSelected,
                    oldOffset, oldScale, oldAlpha, sceneBottom);

            float newScale = 0.965f + monthProgress * 0.035f;
            float newOffset = monthDirection * getWidth() * (1f - monthProgress);
            int newAlpha = Math.round(224f + monthProgress * 31f);
            drawTransitionLayer(canvas, displayed, selected,
                    newOffset, newScale, newAlpha, sceneBottom);
        } else {
            drawScene(canvas, displayed, selected);
        }
        drawBottomControls(canvas);
    }

    private void drawTransitionLayer(Canvas canvas, Calendar month, Calendar selectedDate,
                                     float offset, float scale, int alpha, float sceneBottom) {
        int checkpoint = canvas.save();
        canvas.clipRect(0, 0, getWidth(), sceneBottom);
        canvas.saveLayerAlpha(0, 0, getWidth(), sceneBottom, alpha);
        canvas.translate(offset, 0);
        canvas.scale(scale, scale, getWidth() / 2f, sceneBottom / 2f);
        drawScene(canvas, month, selectedDate);
        canvas.restoreToCount(checkpoint);
    }

    private void drawScene(Canvas canvas, Calendar month, Calendar selectedDate) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        canvas.drawRect(0, 0, getWidth(), detailPanel.bottom + dp(1), paint);
        drawHeader(canvas, month);
        drawWeekdays(canvas);
        buildCells(month);
        drawDays(canvas, selectedDate);
        drawDetailPanel(canvas, selectedDate);
    }

    private void calculateLayout() {
        float width = getWidth();
        cellWidth = width / 7f;
        headerBottom = dp(62);
        weekTop = headerBottom;
        gridTop = weekTop + dp(29);

        Calendar first = (Calendar) displayed.clone();
        int mondayOffset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int requiredRows = (mondayOffset + first.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7;
        visibleRows = Math.max(5, Math.min(6, requiredRows));
        float available = getHeight() - gridTop - dp(260);
        rowHeight = Math.max(dp(46), Math.min(dp(58), available / visibleRows));
        gridBottom = gridTop + rowHeight * visibleRows;

        float navTop = getHeight() - dp(62);
        float fabSize = dp(50);
        float barRight = width - dp(78);
        RectF bar = new RectF(dp(78), navTop, barRight, getHeight() - dp(12));
        float segment = bar.width() / 3f;
        previousDayButton.set(bar.left, bar.top, bar.left + segment, bar.bottom);
        todayButton.set(bar.left + segment, bar.top, bar.left + segment * 2f, bar.bottom);
        nextDayButton.set(bar.left + segment * 2f, bar.top, bar.right, bar.bottom);
        backupButton.set(dp(14), navTop, dp(64), navTop + fabSize);
        pickerButton.set(width - dp(64), navTop, width - dp(14), navTop + fabSize);

        float panelBottom = navTop - dp(10);
        float panelTop = Math.max(gridBottom + dp(10), panelBottom - dp(184));
        detailPanel.set(dp(12), panelTop, width - dp(12), panelBottom);

        float headerSize = dp(40);
        previousMonthButton.set(dp(14), dp(10), dp(14) + headerSize, dp(10) + headerSize);
        nextMonthButton.set(width - dp(54), dp(10), width - dp(14), dp(10) + headerSize);
    }

    private void drawHeader(Canvas canvas, Calendar month) {
        drawCircleButton(canvas, previousMonthButton, false);
        drawCircleButton(canvas, nextMonthButton, true);

        String title = "Tháng " + (month.get(Calendar.MONTH) + 1) + ", "
                + month.get(Calendar.YEAR);
        setText(dp(22), textPrimary, Typeface.BOLD, Paint.Align.CENTER);
        canvas.drawText(title, getWidth() / 2f, dp(36), paint);
    }

    private void drawCircleButton(Canvas canvas, RectF bounds, boolean right) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        canvas.drawRoundRect(bounds, dp(15), dp(15), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.8f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(textPrimary);
        float cx = bounds.centerX();
        float cy = bounds.centerY();
        float sign = right ? 1f : -1f;
        canvas.drawLine(cx - sign * dp(3), cy - dp(5), cx + sign * dp(3), cy, paint);
        canvas.drawLine(cx + sign * dp(3), cy, cx - sign * dp(3), cy + dp(5), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawWeekdays(Canvas canvas) {
        for (int column = 0; column < 7; column++) {
            setText(dp(11.5f), column == 6 ? sunday : textSecondary,
                    Typeface.BOLD, Paint.Align.CENTER);
            canvas.drawText(WEEKDAYS[column], cellWidth * column + cellWidth / 2f,
                    baselineCenter(weekTop, dp(27)), paint);
        }
        paint.setColor(divider);
        paint.setStrokeWidth(dp(0.7f));
        canvas.drawLine(dp(15), gridTop - dp(1), getWidth() - dp(15), gridTop - dp(1), paint);
    }

    private void buildCells(Calendar monthCalendar) {
        dayCells.clear();
        Calendar cursor = (Calendar) monthCalendar.clone();
        int mondayOffset = (cursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cursor.add(Calendar.DAY_OF_MONTH, -mondayOffset);
        int count = visibleRows * 7;
        for (int i = 0; i < count; i++) {
            int row = i / 7;
            int column = i % 7;
            RectF bounds = new RectF(column * cellWidth, gridTop + row * rowHeight,
                    (column + 1) * cellWidth, gridTop + (row + 1) * rowHeight);
            Calendar date = (Calendar) cursor.clone();
            boolean inMonth = date.get(Calendar.MONTH) == monthCalendar.get(Calendar.MONTH)
                    && date.get(Calendar.YEAR) == monthCalendar.get(Calendar.YEAR);
            dayCells.add(new DayCell(date, inMonth, bounds));
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void drawDays(Canvas canvas, Calendar selectedDate) {
        for (DayCell cell : dayCells) {
            Calendar date = cell.date;
            int day = date.get(Calendar.DAY_OF_MONTH);
            int month = date.get(Calendar.MONTH) + 1;
            int year = date.get(Calendar.YEAR);
            LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
            String holiday = HolidayUtil.getHoliday(day, month, lunar);
            boolean hasNote = NoteRepository.has(getContext(), date);
            boolean isToday = sameDate(date, today);
            boolean isSelected = sameDate(date, selectedDate);
            float cx = cell.bounds.centerX();
            float circleCy = cell.bounds.top + dp(19);
            float animatedRadius = dp(18 + (isSelected
                    ? (float) Math.sin(selectionPulse * Math.PI) * 2.2f : 0f));

            if (isToday) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(accent);
                canvas.drawCircle(cx, circleCy, animatedRadius, paint);
            } else if (isSelected) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(accentSoft);
                canvas.drawCircle(cx, circleCy, animatedRadius, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1.2f));
                paint.setColor(accent);
                canvas.drawCircle(cx, circleCy, animatedRadius, paint);
            }

            int dayColor;
            if (!cell.inDisplayedMonth) dayColor = textMuted;
            else if (isToday) dayColor = Color.WHITE;
            else if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) dayColor = sunday;
            else dayColor = textPrimary;
            setText(dp(16.5f), dayColor, isSelected || isToday ? Typeface.BOLD : Typeface.NORMAL,
                    Paint.Align.CENTER);
            canvas.drawText(Integer.toString(day), cx,
                    baselineCenter(circleCy - dp(18), dp(36)), paint);

            int lunarColor = !cell.inDisplayedMonth ? textMuted
                    : (!holiday.isEmpty() ? sunday : (isToday ? accent : textSecondary));
            setText(dp(9.2f), lunarColor,
                    !holiday.isEmpty() ? Typeface.BOLD : Typeface.NORMAL, Paint.Align.CENTER);
            String lunarLabel = HolidayUtil.getShortLabel(day, month, lunar);
            canvas.drawText(fitText(lunarLabel, cellWidth - dp(5)), cx,
                    cell.bounds.top + dp(45), paint);

            if (!holiday.isEmpty() && cell.inDisplayedMonth) {
                paint.setColor(isToday ? Color.WHITE : sunday);
                canvas.drawCircle(cx + dp(15), circleCy - dp(13), dp(2), paint);
            }
            if (hasNote && cell.inDisplayedMonth) {
                paint.setColor(isToday ? Color.WHITE : accent);
                canvas.drawCircle(cx - dp(15), circleCy - dp(13), dp(2), paint);
            }
        }
    }

    private void drawDetailPanel(Canvas canvas, Calendar selectedDate) {
        if (detailPanel.height() < dp(86)) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        canvas.drawRoundRect(detailPanel, dp(22), dp(22), paint);

        int day = selectedDate.get(Calendar.DAY_OF_MONTH);
        int month = selectedDate.get(Calendar.MONTH) + 1;
        int year = selectedDate.get(Calendar.YEAR);
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String holiday = HolidayUtil.getHoliday(day, month, lunar);
        String note = NoteRepository.get(getContext(), selectedDate);
        float left = detailPanel.left + dp(17);
        float right = detailPanel.right - dp(17);

        setText(dp(16.5f), textPrimary, Typeface.BOLD, Paint.Align.LEFT);
        String week = capitalize(new SimpleDateFormat("EEEE", VI).format(selectedDate.getTime()));
        canvas.drawText(week, left,
                detailPanel.top + dp(27), paint);
        setText(dp(12), textSecondary, Typeface.BOLD, Paint.Align.RIGHT);
        canvas.drawText(day + " tháng " + month + ", " + year,
                right, detailPanel.top + dp(26), paint);

        paint.setColor(divider);
        paint.setStrokeWidth(dp(0.8f));
        canvas.drawLine(left, detailPanel.top + dp(40), right,
                detailPanel.top + dp(40), paint);

        setText(dp(9.5f), accent, Typeface.BOLD, Paint.Align.LEFT);
        canvas.drawText("ÂM LỊCH VIỆT NAM", left, detailPanel.top + dp(59), paint);
        setText(dp(16), textPrimary, Typeface.BOLD, Paint.Align.LEFT);
        canvas.drawText(lunar.day + " tháng " + lunar.month
                        + (lunar.leap ? " nhuận" : "") + "  ·  "
                        + LunarCalendar.yearCanChi(lunar.year),
                left, detailPanel.top + dp(82), paint);

        if (detailPanel.height() >= dp(124)) {
            setText(dp(11.5f), textSecondary, Typeface.NORMAL, Paint.Align.LEFT);
            String canChi = "Ngày " + LunarCalendar.dayCanChi(lunar.julianDay)
                    + "  ·  Tháng " + LunarCalendar.monthCanChi(lunar.month, lunar.year);
            canvas.drawText(fitText(canChi, right - left), left,
                    detailPanel.top + dp(105), paint);
        }

        if (!holiday.isEmpty() && detailPanel.height() >= dp(138)) {
            paint.setColor(accentSoft);
            RectF holidayPill = new RectF(left, detailPanel.top + dp(110), right,
                    Math.min(detailPanel.bottom - dp(8), detailPanel.top + dp(136)));
            canvas.drawRoundRect(holidayPill, dp(11), dp(11), paint);
            setText(dp(10.8f), sunday, Typeface.BOLD, Paint.Align.CENTER);
            canvas.drawText(fitText(holiday, holidayPill.width() - dp(20)),
                    holidayPill.centerX(), baselineCenter(holidayPill.top, holidayPill.height()), paint);
        }

        float requiredNoteHeight = holiday.isEmpty() ? dp(151) : dp(176);
        if (detailPanel.height() >= requiredNoteHeight) {
            float noteTop = detailPanel.top + (holiday.isEmpty() ? dp(125) : dp(153));
            float noteTextBaseline = detailPanel.top
                    + (holiday.isEmpty() ? dp(145) : dp(171));
            setText(dp(9.5f), accent, Typeface.BOLD, Paint.Align.LEFT);
            canvas.drawText("GHI CHÚ", left, noteTop, paint);
            setText(dp(10.6f), note.isEmpty() ? textSecondary : textPrimary,
                    note.isEmpty() ? Typeface.NORMAL : Typeface.BOLD, Paint.Align.LEFT);
            String noteText = note.isEmpty() ? "Chạm để thêm ghi chú" : note;
            canvas.drawText(fitText(noteText, right - left - dp(2)), left,
                    noteTextBaseline, paint);
        }
    }

    private void drawBottomControls(Canvas canvas) {
        RectF bar = new RectF(previousDayButton.left, previousDayButton.top,
                nextDayButton.right, nextDayButton.bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        canvas.drawRoundRect(bar, dp(19), dp(19), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(0.8f));
        paint.setColor(divider);
        canvas.drawRoundRect(bar, dp(19), dp(19), paint);

        setText(dp(22), textSecondary, Typeface.NORMAL, Paint.Align.CENTER);
        canvas.drawText("‹", previousDayButton.centerX(),
                baselineCenter(previousDayButton.top, previousDayButton.height()), paint);
        canvas.drawText("›", nextDayButton.centerX(),
                baselineCenter(nextDayButton.top, nextDayButton.height()), paint);
        setText(dp(11.5f), accent, Typeface.BOLD, Paint.Align.CENTER);
        canvas.drawText("HÔM NAY", todayButton.centerX(),
                baselineCenter(todayButton.top, todayButton.height()), paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawRoundRect(backupButton, dp(18), dp(18), paint);
        drawBackupIcon(canvas, backupButton.centerX(), backupButton.centerY());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(accent);
        canvas.drawRoundRect(pickerButton, dp(18), dp(18), paint);
        drawCalendarIcon(canvas, pickerButton.centerX(), pickerButton.centerY());
    }

    private void drawBackupIcon(Canvas canvas, float cx, float cy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.WHITE);
        canvas.drawLine(cx, cy + dp(4), cx, cy - dp(8), paint);
        canvas.drawLine(cx, cy - dp(8), cx - dp(4), cy - dp(4), paint);
        canvas.drawLine(cx, cy - dp(8), cx + dp(4), cy - dp(4), paint);
        canvas.drawLine(cx - dp(9), cy + dp(3), cx - dp(9), cy + dp(9), paint);
        canvas.drawLine(cx - dp(9), cy + dp(9), cx + dp(9), cy + dp(9), paint);
        canvas.drawLine(cx + dp(9), cy + dp(9), cx + dp(9), cy + dp(3), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCalendarIcon(Canvas canvas, float cx, float cy) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.8f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.WHITE);
        RectF body = new RectF(cx - dp(9), cy - dp(8), cx + dp(9), cy + dp(9));
        canvas.drawRoundRect(body, dp(3), dp(3), paint);
        canvas.drawLine(body.left, cy - dp(3), body.right, cy - dp(3), paint);
        canvas.drawLine(cx - dp(5), cy - dp(11), cx - dp(5), cy - dp(6), paint);
        canvas.drawLine(cx + dp(5), cy - dp(11), cx + dp(5), cy - dp(6), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx - dp(4), cy + dp(2), dp(1.2f), paint);
        canvas.drawCircle(cx + dp(4), cy + dp(2), dp(1.2f), paint);
        canvas.drawCircle(cx - dp(4), cy + dp(6), dp(1.2f), paint);
        canvas.drawCircle(cx + dp(4), cy + dp(6), dp(1.2f), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) > dp(12)
                        || Math.abs(event.getY() - downY) > dp(12)) moved = true;
                return true;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (downY >= weekTop && downY <= gridBottom
                        && Math.abs(dx) > dp(62) && Math.abs(dx) > Math.abs(dy) * 1.35f) {
                    changeMonth(dx < 0 ? 1 : -1);
                    return true;
                }
                if (!moved) handleTap(event.getX(), event.getY());
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void handleTap(float x, float y) {
        if (previousMonthButton.contains(x, y)) {
            changeMonth(-1);
            return;
        }
        if (nextMonthButton.contains(x, y)) {
            changeMonth(1);
            return;
        }
        if (pickerButton.contains(x, y)) {
            showDatePicker();
            return;
        }
        if (backupButton.contains(x, y)) {
            showNotesBackup();
            return;
        }
        if (previousDayButton.contains(x, y)) {
            changeSelectedDay(-1);
            return;
        }
        if (nextDayButton.contains(x, y)) {
            changeSelectedDay(1);
            return;
        }
        if (todayButton.contains(x, y)) {
            goToday();
            return;
        }
        if (detailPanel.contains(x, y)) {
            showNoteEditor();
            return;
        }
        for (DayCell cell : dayCells) {
            if (cell.bounds.contains(x, y)) {
                Calendar oldDisplayed = (Calendar) displayed.clone();
                Calendar oldSelected = (Calendar) selected.clone();
                selected.setTimeInMillis(cell.date.getTimeInMillis());
                if (!cell.inDisplayedMonth) {
                    displayed.setTimeInMillis(cell.date.getTimeInMillis());
                    displayed.set(Calendar.DAY_OF_MONTH, 1);
                    startMonthTransition(oldDisplayed, oldSelected,
                            monthIndex(displayed) > monthIndex(oldDisplayed) ? 1 : -1);
                }
                performClick();
                startSelectionAnimation();
                invalidate();
                return;
            }
        }
    }

    private void showDatePicker() {
        WheelDatePickerDialog dialog = new WheelDatePickerDialog(getContext(), selected,
                new WheelDatePickerDialog.Listener() {
                    @Override
                    public void onDateSelected(int year, int month, int day) {
                        Calendar oldDisplayed = (Calendar) displayed.clone();
                        Calendar oldSelected = (Calendar) selected.clone();
                        selected.set(year, month, day);
                        normalize(selected);
                        displayed.setTimeInMillis(selected.getTimeInMillis());
                        displayed.set(Calendar.DAY_OF_MONTH, 1);
                        startTransitionIfMonthChanged(oldDisplayed, oldSelected);
                        startSelectionAnimation();
                        invalidate();
                    }
                });
        dialog.show();
    }

    private void showNotesBackup() {
        Context context = getContext();
        if (context instanceof Activity) {
            XiaomiNotesBackup.showBackupDialog((Activity) context);
        }
    }

    private void goToday() {
        Calendar oldDisplayed = (Calendar) displayed.clone();
        Calendar oldSelected = (Calendar) selected.clone();
        today = Calendar.getInstance();
        normalize(today);
        selected.setTimeInMillis(today.getTimeInMillis());
        displayed.setTimeInMillis(today.getTimeInMillis());
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        startTransitionIfMonthChanged(oldDisplayed, oldSelected);
        startSelectionAnimation();
        invalidate();
    }

    private void changeSelectedDay(int amount) {
        Calendar oldDisplayed = (Calendar) displayed.clone();
        Calendar oldSelected = (Calendar) selected.clone();
        selected.add(Calendar.DAY_OF_MONTH, amount);
        displayed.setTimeInMillis(selected.getTimeInMillis());
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        startTransitionIfMonthChanged(oldDisplayed, oldSelected);
        startSelectionAnimation();
        invalidate();
    }

    private void changeMonth(int amount) {
        Calendar oldDisplayed = (Calendar) displayed.clone();
        Calendar oldSelected = (Calendar) selected.clone();
        displayed.add(Calendar.MONTH, amount);
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        selected.setTimeInMillis(displayed.getTimeInMillis());
        startMonthTransition(oldDisplayed, oldSelected, amount > 0 ? 1 : -1);
        startSelectionAnimation();
        invalidate();
    }

    private void showNoteEditor() {
        NoteEditorDialog.show(getContext(), selected, new NoteEditorDialog.Listener() {
            @Override
            public void onNoteChanged() {
                startSelectionAnimation();
                invalidate();
            }
        });
    }

    private void startSelectionAnimation() {
        if (selectionAnimator != null) selectionAnimator.cancel();
        selectionAnimator = ValueAnimator.ofFloat(0f, 1f);
        selectionAnimator.setDuration(210);
        selectionAnimator.setInterpolator(new DecelerateInterpolator());
        selectionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                selectionPulse = ((Float) animation.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        selectionAnimator.start();
    }

    private void startTransitionIfMonthChanged(Calendar oldDisplayed, Calendar oldSelected) {
        int oldIndex = monthIndex(oldDisplayed);
        int newIndex = monthIndex(displayed);
        if (oldIndex != newIndex) {
            startMonthTransition(oldDisplayed, oldSelected, newIndex > oldIndex ? 1 : -1);
        }
    }

    private void startMonthTransition(Calendar oldDisplayed, Calendar oldSelected, int direction) {
        if (getWidth() == 0 || getHeight() == 0) return;
        if (monthAnimator != null) monthAnimator.cancel();
        outgoingDisplayed = (Calendar) oldDisplayed.clone();
        outgoingSelected = (Calendar) oldSelected.clone();
        monthDirection = direction < 0 ? -1 : 1;
        monthProgress = 0f;
        monthAnimator = ValueAnimator.ofFloat(0f, 1f);
        monthAnimator.setDuration(320);
        monthAnimator.setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f));
        monthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                monthProgress = ((Float) animation.getAnimatedValue()).floatValue();
                if (monthProgress >= 0.999f) {
                    outgoingDisplayed = null;
                    outgoingSelected = null;
                    monthProgress = 1f;
                }
                invalidate();
            }
        });
        monthAnimator.start();
    }

    private static int monthIndex(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void setText(float size, int color, int style, Paint.Align align) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        paint.setTextAlign(align);
    }

    private float baselineCenter(float top, float height) {
        paint.getFontMetrics(metrics);
        return top + (height - metrics.bottom + metrics.top) / 2f - metrics.top;
    }

    private String fitText(String value, float maxWidth) {
        if (paint.measureText(value) <= maxWidth) return value;
        String ellipsis = "…";
        int end = value.length();
        while (end > 1 && paint.measureText(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return value.substring(0, end) + ellipsis;
    }

    private String capitalize(String value) {
        if (value == null || value.length() == 0) return "";
        return value.substring(0, 1).toUpperCase(VI) + value.substring(1);
    }

    private float dp(float value) {
        return value * density;
    }

    private static void normalize(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static boolean sameDate(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static final class DayCell {
        final Calendar date;
        final boolean inDisplayedMonth;
        final RectF bounds;

        DayCell(Calendar date, boolean inDisplayedMonth, RectF bounds) {
            this.date = date;
            this.inDisplayedMonth = inDisplayedMonth;
            this.bounds = bounds;
        }
    }
}
