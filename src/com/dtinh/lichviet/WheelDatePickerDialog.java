package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Xiaomi-style wheel date selector that remains consistent in light and dark mode. */
public final class WheelDatePickerDialog extends Dialog {
    public interface Listener {
        void onDateSelected(int year, int month, int day);
    }

    private static final Locale VI = new Locale("vi", "VN");
    private static final String[] MONTHS = {
            "Thg 1", "Thg 2", "Thg 3", "Thg 4", "Thg 5", "Thg 6",
            "Thg 7", "Thg 8", "Thg 9", "Thg 10", "Thg 11", "Thg 12"
    };

    private final Context context;
    private final Listener listener;
    private final Calendar value;
    private final float density;
    private final boolean night;
    private final int surface;
    private final int surfaceSoft;
    private final int textPrimary;
    private final int textSecondary;
    private final int divider;
    private final int accent;

    private NumberPicker dayPicker;
    private NumberPicker monthPicker;
    private NumberPicker yearPicker;
    private TextView preview;

    public WheelDatePickerDialog(Context context, Calendar initial, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.value = (Calendar) initial.clone();
        this.density = context.getResources().getDisplayMetrics().density;
        this.night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            surface = Color.rgb(31, 33, 39);
            surfaceSoft = Color.rgb(42, 44, 52);
            textPrimary = Color.rgb(247, 248, 251);
            textSecondary = Color.rgb(166, 170, 180);
            divider = Color.rgb(52, 55, 64);
            accent = Color.rgb(91, 150, 247);
        } else {
            surface = Color.WHITE;
            surfaceSoft = Color.rgb(241, 243, 248);
            textPrimary = Color.rgb(24, 27, 34);
            textSecondary = Color.rgb(112, 117, 129);
            divider = Color.rgb(228, 231, 237);
            accent = Color.rgb(66, 133, 244);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        setContentView(buildContent());
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(dp(420),
                    context.getResources().getDisplayMetrics().widthPixels - dp(28));
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.64f;
            window.setAttributes(params);
        }
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(21), dp(22), dp(18));
        root.setBackground(roundDrawable(surface, 28));

        TextView title = text("Chọn ngày", 22, textPrimary, Typeface.BOLD);
        root.addView(title, matchWrap());

        preview = text("", 13, accent, Typeface.BOLD);
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.topMargin = dp(6);
        root.addView(preview, previewParams);

        View line = new View(context);
        line.setBackgroundColor(divider);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lineParams.topMargin = dp(18);
        lineParams.bottomMargin = dp(10);
        root.addView(line, lineParams);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.addView(label("NGÀY"), weighted());
        labels.addView(label("THÁNG"), weighted());
        labels.addView(label("NĂM"), weighted());
        root.addView(labels, matchWrap());

        LinearLayout wheels = new LinearLayout(context);
        wheels.setGravity(Gravity.CENTER);
        wheels.setOrientation(LinearLayout.HORIZONTAL);
        dayPicker = picker();
        monthPicker = picker();
        yearPicker = picker();
        wheels.addView(dayPicker, weightedHeight(150));
        wheels.addView(monthPicker, weightedHeight(150));
        wheels.addView(yearPicker, weightedHeight(150));
        root.addView(wheels, matchWrap());

        configurePickers();

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonRow = matchWrap();
        buttonRow.topMargin = dp(12);

        TextView cancel = actionButton("HỦY", textPrimary, surfaceSoft);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        TextView done = actionButton("XONG", Color.WHITE, accent);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onDateSelected(yearPicker.getValue(), monthPicker.getValue(),
                        dayPicker.getValue());
                dismiss();
            }
        });
        LinearLayout.LayoutParams leftButton = weightedHeight(46);
        leftButton.rightMargin = dp(7);
        LinearLayout.LayoutParams rightButton = weightedHeight(46);
        rightButton.leftMargin = dp(7);
        buttons.addView(cancel, leftButton);
        buttons.addView(done, rightButton);
        root.addView(buttons, buttonRow);
        updatePreview();
        return root;
    }

    private void configurePickers() {
        dayPicker.setMinValue(1);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(MONTHS);
        yearPicker.setMinValue(1900);
        yearPicker.setMaxValue(2100);
        yearPicker.setWrapSelectorWheel(false);
        monthPicker.setValue(value.get(Calendar.MONTH));
        yearPicker.setValue(value.get(Calendar.YEAR));
        refreshDayMaximum(value.get(Calendar.DAY_OF_MONTH));

        NumberPicker.OnValueChangeListener change = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldValue, int newValue) {
                if (picker == monthPicker || picker == yearPicker) {
                    refreshDayMaximum(dayPicker.getValue());
                }
                updatePreview();
            }
        };
        dayPicker.setOnValueChangedListener(change);
        monthPicker.setOnValueChangedListener(change);
        yearPicker.setOnValueChangedListener(change);
    }

    private void refreshDayMaximum(int preferredDay) {
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(yearPicker.getValue(), monthPicker.getValue(), 1);
        int maximum = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        dayPicker.setMaxValue(maximum);
        dayPicker.setValue(Math.min(preferredDay, maximum));
    }

    private void updatePreview() {
        if (preview == null || dayPicker == null) return;
        Calendar date = Calendar.getInstance();
        date.set(yearPicker.getValue(), monthPicker.getValue(), dayPicker.getValue());
        String week = new SimpleDateFormat("EEEE", VI).format(date.getTime());
        if (week.length() > 0) week = week.substring(0, 1).toUpperCase(VI) + week.substring(1);
        preview.setText(week + ", " + dayPicker.getValue() + " tháng "
                + (monthPicker.getValue() + 1) + ", " + yearPicker.getValue());
    }

    private NumberPicker picker() {
        NumberPicker picker = new NumberPicker(context);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setWrapSelectorWheel(true);
        picker.setBackgroundColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) picker.setTextColor(textPrimary);
        tintChildren(picker);
        return picker;
    }

    private void tintChildren(View view) {
        if (view instanceof EditText) {
            EditText edit = (EditText) view;
            edit.setTextColor(textPrimary);
            edit.setTextSize(18);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) tintChildren(group.getChildAt(i));
        }
    }

    private TextView label(String value) {
        TextView text = text(value, 9.5f, textSecondary, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private TextView actionButton(String value, int color, int background) {
        TextView button = text(value, 12, color, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundDrawable(background, 16));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        text.setIncludeFontPadding(false);
        return text;
    }

    private GradientDrawable roundDrawable(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightedHeight(float height) {
        return new LinearLayout.LayoutParams(0, dp(height), 1f);
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
