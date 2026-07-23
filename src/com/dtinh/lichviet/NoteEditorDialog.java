package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Xiaomi-style bottom editor for one locally stored calendar note. */
public final class NoteEditorDialog {
    public interface Listener {
        void onNoteChanged();
    }

    private static final Locale VI = new Locale("vi", "VN");
    private static final int NOTE_LIMIT = 240;

    private NoteEditorDialog() {}

    @SuppressWarnings("deprecation")
    public static void show(final Context context, final Calendar date,
                            final Listener listener) {
        final Calendar savedDate = (Calendar) date.clone();
        final String current = NoteRepository.get(context, savedDate);
        final boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        final int surface = night ? Color.rgb(29, 31, 37) : Color.WHITE;
        final int inputSurface = night ? Color.rgb(38, 40, 47) : Color.rgb(239, 242, 247);
        final int divider = night ? Color.rgb(58, 61, 70) : Color.rgb(221, 225, 233);
        final int primary = night ? Color.rgb(245, 247, 250) : Color.rgb(24, 27, 34);
        final int secondary = night ? Color.rgb(168, 172, 182) : Color.rgb(113, 118, 130);
        final int accent = night ? Color.rgb(91, 150, 247) : Color.rgb(66, 133, 244);
        final int danger = night ? Color.rgb(244, 116, 116) : Color.rgb(221, 75, 75);

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout outside = new FrameLayout(context);
        outside.setPadding(dp(context, 12), 0, dp(context, 12), dp(context, 12));
        LinearLayout sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(context, 22), dp(context, 10),
                dp(context, 22), dp(context, 20));
        sheet.setBackground(rounded(surface, dp(context, 28)));
        outside.addView(sheet, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));

        LinearLayout handleRow = new LinearLayout(context);
        handleRow.setGravity(Gravity.CENTER);
        View handle = new View(context);
        handle.setBackground(rounded(divider, dp(context, 2)));
        handleRow.addView(handle, new LinearLayout.LayoutParams(dp(context, 38), dp(context, 4)));
        sheet.addView(handleRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 18)));

        TextView title = label(context, context.getString(R.string.note_title), primary, 20, true);
        sheet.addView(title, matchWrap());

        String dateText = capitalize(new SimpleDateFormat("EEEE, d 'tháng' M, yyyy", VI)
                .format(savedDate.getTime()));
        TextView dateLabel = label(context, dateText, secondary, 12, true);
        LinearLayout.LayoutParams dateParams = matchWrap();
        dateParams.topMargin = dp(context, 5);
        sheet.addView(dateLabel, dateParams);

        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(
                savedDate.get(Calendar.DAY_OF_MONTH), savedDate.get(Calendar.MONTH) + 1,
                savedDate.get(Calendar.YEAR));
        String lunarText = "Âm " + lunar.day + "/" + lunar.month + "  ·  "
                + LunarCalendar.yearCanChi(lunar.year);
        TextView lunarLabel = label(context, lunarText, accent, 11, true);
        LinearLayout.LayoutParams lunarParams = matchWrap();
        lunarParams.topMargin = dp(context, 4);
        sheet.addView(lunarLabel, lunarParams);

        final EditText input = new EditText(context);
        input.setHint(R.string.note_hint);
        input.setHintTextColor(secondary);
        input.setTextColor(primary);
        input.setTextSize(15);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(4);
        input.setMaxLines(6);
        input.setPadding(dp(context, 16), dp(context, 14),
                dp(context, 16), dp(context, 14));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NOTE_LIMIT)});
        input.setBackground(stroked(inputSurface, divider, dp(context, 16), dp(context, 1)));
        input.setText(current);
        input.setSelection(input.length());
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 118));
        inputParams.topMargin = dp(context, 18);
        sheet.addView(input, inputParams);

        final TextView counter = label(context, input.length() + "/" + NOTE_LIMIT,
                secondary, 10, false);
        counter.setGravity(Gravity.END);
        LinearLayout.LayoutParams counterParams = matchWrap();
        counterParams.topMargin = dp(context, 6);
        sheet.addView(counter, counterParams);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                counter.setText(s.length() + "/" + NOTE_LIMIT);
            }
            @Override public void afterTextChanged(Editable value) {}
        });

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionRowParams = matchWrap();
        actionRowParams.topMargin = dp(context, 15);
        sheet.addView(actions, actionRowParams);

        if (!current.isEmpty()) {
            TextView delete = action(context, context.getString(R.string.note_delete), danger,
                    inputSurface, false);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    NoteRepository.put(context, savedDate, "");
                    if (listener != null) listener.onNoteChanged();
                    dialog.dismiss();
                }
            });
            actions.addView(delete, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 44)));
        }

        View spacer = new View(context);
        actions.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView cancel = action(context, context.getString(R.string.note_cancel), secondary,
                Color.TRANSPARENT, false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { dialog.dismiss(); }
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 44)));

        TextView save = action(context, context.getString(R.string.note_save), Color.WHITE,
                accent, true);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 44));
        saveParams.leftMargin = dp(context, 8);
        actions.addView(save, saveParams);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                NoteRepository.put(context, savedDate, input.getText().toString());
                if (listener != null) listener.onNoteChanged();
                dialog.dismiss();
            }
        });

        dialog.setContentView(outside);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.48f;
            window.setAttributes(attributes);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            window.setWindowAnimations(0);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        sheet.setAlpha(0f);
        sheet.setTranslationY(dp(context, 28));
        sheet.animate().alpha(1f).translationY(0f).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
        input.requestFocus();
    }

    private static TextView label(Context context, String text, int color,
                                  float size, boolean medium) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif",
                Typeface.NORMAL));
        return view;
    }

    private static TextView action(Context context, String text, int textColor,
                                   int background, boolean strong) {
        TextView view = label(context, text, textColor, 12, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(context, strong ? 22 : 16), 0,
                dp(context, strong ? 22 : 16), 0);
        if (background != Color.TRANSPARENT) {
            view.setBackground(rounded(background, dp(context, 15)));
        }
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static GradientDrawable rounded(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private static GradientDrawable stroked(int color, int stroke, float radius, int width) {
        GradientDrawable drawable = rounded(color, radius);
        drawable.setStroke(width, stroke);
        return drawable;
    }

    private static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String capitalize(String value) {
        if (value == null || value.length() == 0) return "";
        return value.substring(0, 1).toUpperCase(VI) + value.substring(1);
    }
}
