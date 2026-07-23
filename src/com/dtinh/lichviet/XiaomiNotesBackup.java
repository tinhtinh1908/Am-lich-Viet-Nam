package com.dtinh.lichviet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.Base64;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** User-mediated encrypted backup transported by Xiaomi Notes and Mi Cloud. */
public final class XiaomiNotesBackup {
    private static final String XIAOMI_NOTES = "com.miui.notes";
    private static final String PREFIX = "LVB1:";
    private static final String FORMAT_MAGIC = "LichVietNotes";
    private static final int FORMAT_VERSION = 1;
    private static final int ITERATIONS = 180_000;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int MAX_IMPORT_NOTES = 5000;
    private static final int MAX_SHARED_CHARS = 700_000;
    private static final int MAX_PLAIN_BYTES = 2_000_000;
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "lich-viet-note-backup");
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                }
            });

    private XiaomiNotesBackup() {}

    public static void showBackupDialog(final Activity activity) {
        if (!isUsable(activity)) return;
        final PasswordForm form = passwordForm(activity, true);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.backup_dialog_title)
                .setMessage(R.string.backup_dialog_message)
                .setView(form.content)
                .setNegativeButton(R.string.note_cancel, null)
                .setPositiveButton(R.string.backup_create, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String first = form.password.getText().toString();
                    String second = form.confirm.getText().toString();
                    if (first.length() < 6) {
                        form.password.setError(activity.getString(R.string.backup_password_short));
                        return;
                    }
                    if (!first.equals(second)) {
                        form.confirm.setError(activity.getString(R.string.backup_password_mismatch));
                        return;
                    }
                    dialog.dismiss();
                    createAndShare(activity, first.toCharArray());
                }));
        dialog.show();
    }

    public static boolean handleIncomingShare(final Activity activity, Intent intent,
                                              final Runnable onImported) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())
                || !"text/plain".equals(intent.getType())) return false;
        final CharSequence shared;
        try {
            shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        } catch (RuntimeException malformedIntent) {
            Toast.makeText(activity, R.string.backup_invalid_share, Toast.LENGTH_LONG).show();
            return true;
        }
        if (shared == null || shared.toString().indexOf(PREFIX) < 0) {
            Toast.makeText(activity, R.string.backup_invalid_share, Toast.LENGTH_LONG).show();
            return true;
        }
        String sharedText = shared.toString();
        if (sharedText.length() > MAX_SHARED_CHARS) {
            Toast.makeText(activity, R.string.backup_too_large, Toast.LENGTH_LONG).show();
            return true;
        }
        showRestoreDialog(activity, sharedText, onImported);
        return true;
    }

    private static void createAndShare(final Activity activity, final char[] password) {
        final Map<String, String> notes = NoteRepository.exportAll(activity);
        if (notes.isEmpty()) {
            wipe(password);
            Toast.makeText(activity, R.string.backup_no_notes, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(activity, R.string.backup_creating, Toast.LENGTH_SHORT).show();
        BACKGROUND.execute(() -> {
            try {
                String payload = encrypt(serialize(notes), password);
                String created = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                        new Locale("vi", "VN")).format(new Date());
                String body = "LỊCH VIỆT BACKUP\n"
                        + "Ngày tạo: " + created + "\n"
                        + "Số ghi chú: " + notes.size() + "\n"
                        + "Không chỉnh sửa dòng dữ liệu bên dưới.\n\n"
                        + PREFIX + payload;
                if (body.length() > MAX_SHARED_CHARS) {
                    postToast(activity, R.string.backup_too_large);
                    return;
                }
                post(activity, () -> shareToNotes(activity, body));
            } catch (Exception error) {
                postToast(activity, R.string.backup_create_failed);
            } finally {
                wipe(password);
            }
        });
    }

    private static void showRestoreDialog(final Activity activity, final String sharedText,
                                          final Runnable onImported) {
        if (!isUsable(activity)) return;
        final PasswordForm form = passwordForm(activity, false);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.restore_dialog_title)
                .setMessage(R.string.restore_dialog_message)
                .setView(form.content)
                .setNegativeButton(R.string.note_cancel, null)
                .setPositiveButton(R.string.restore_decrypt, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = form.password.getText().toString();
                    if (value.length() < 6) {
                        form.password.setError(activity.getString(R.string.backup_password_short));
                        return;
                    }
                    dialog.dismiss();
                    decryptAndConfirm(activity, sharedText, value.toCharArray(), onImported);
                }));
        dialog.show();
    }

    private static void decryptAndConfirm(final Activity activity, final String sharedText,
                                          final char[] password, final Runnable onImported) {
        Toast.makeText(activity, R.string.restore_decrypting, Toast.LENGTH_SHORT).show();
        BACKGROUND.execute(() -> {
            try {
                String encoded = extractPayload(sharedText);
                final Map<String, String> notes = deserialize(decrypt(encoded, password));
                post(activity, () -> new AlertDialog.Builder(activity)
                        .setTitle(R.string.restore_confirm_title)
                        .setMessage(activity.getString(R.string.restore_confirm_message,
                                notes.size()))
                        .setNegativeButton(R.string.note_cancel, null)
                        .setPositiveButton(R.string.restore_apply, (ignored, which) -> {
                            int count = NoteRepository.mergeAll(activity, notes);
                            if (onImported != null) onImported.run();
                            Toast.makeText(activity,
                                    activity.getString(R.string.restore_success, count),
                                    Toast.LENGTH_LONG).show();
                        }).show());
            } catch (Exception error) {
                postToast(activity, R.string.restore_failed);
            } finally {
                wipe(password);
            }
        });
    }

    private static PasswordForm passwordForm(Activity activity, boolean withConfirmation) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int side = dp(activity, 24);
        content.setPadding(side, dp(activity, 8), side, 0);
        content.addView(fieldLabel(activity, activity.getString(R.string.backup_password)));
        EditText password = passwordField(activity, R.string.backup_password_hint);
        content.addView(password);
        EditText confirm = null;
        if (withConfirmation) {
            TextView confirmLabel = fieldLabel(activity,
                    activity.getString(R.string.backup_password_confirm));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dp(activity, 12);
            content.addView(confirmLabel, labelParams);
            confirm = passwordField(activity, R.string.backup_password_confirm_hint);
            content.addView(confirm);
        }
        return new PasswordForm(content, password, confirm);
    }

    private static TextView fieldLabel(Activity activity, String text) {
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextSize(12);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return label;
    }

    private static EditText passwordField(Activity activity, int hint) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setSelectAllOnFocus(false);
        return input;
    }

    private static void shareToNotes(Activity activity, String body) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.backup_note_subject));
        share.putExtra(Intent.EXTRA_TEXT, body);
        share.setPackage(XIAOMI_NOTES);
        try {
            activity.startActivity(share);
        } catch (ActivityNotFoundException missing) {
            share.setPackage(null);
            try {
                activity.startActivity(Intent.createChooser(share,
                        activity.getString(R.string.backup_share_chooser)));
            } catch (RuntimeException noShareTarget) {
                Toast.makeText(activity, R.string.backup_share_failed, Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException blockedBySystem) {
            Toast.makeText(activity, R.string.backup_share_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static byte[] serialize(Map<String, String> notes) {
        StringBuilder text = new StringBuilder();
        text.append(FORMAT_MAGIC).append('\n')
                .append(FORMAT_VERSION).append('\n')
                .append(System.currentTimeMillis()).append('\n')
                .append(notes.size()).append('\n');
        for (Map.Entry<String, String> entry : notes.entrySet()) {
            String value = Base64.encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            text.append(entry.getKey()).append('\t').append(value).append('\n');
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> deserialize(byte[] bytes) throws Exception {
        if (bytes.length > MAX_PLAIN_BYTES) throw new IllegalArgumentException("Oversized backup");
        String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\\n", -1);
        if (lines.length < 4 || !FORMAT_MAGIC.equals(lines[0])
                || Integer.parseInt(lines[1]) != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported backup");
        }
        int declared = Integer.parseInt(lines[3]);
        if (declared < 0 || declared > MAX_IMPORT_NOTES) {
            throw new IllegalArgumentException("Invalid note count");
        }
        LinkedHashMap<String, String> notes = new LinkedHashMap<String, String>();
        for (int i = 4; i < lines.length && notes.size() < declared; i++) {
            int tab = lines[i].indexOf('\t');
            if (tab <= 0) continue;
            String key = lines[i].substring(0, tab);
            String value = new String(Base64.decode(lines[i].substring(tab + 1),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), StandardCharsets.UTF_8);
            if (isValidKey(key) && !value.trim().isEmpty()) notes.put(key, value);
        }
        if (notes.size() != declared) throw new IllegalArgumentException("Incomplete backup");
        return notes;
    }

    private static String encrypt(byte[] plain, char[] password) throws Exception {
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv = new byte[IV_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);
        SecretKey key = deriveKey(password, salt, ITERATIONS);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plain);
        ByteBuffer output = ByteBuffer.allocate(1 + 4 + SALT_BYTES + IV_BYTES + encrypted.length);
        output.put((byte) FORMAT_VERSION).putInt(ITERATIONS).put(salt).put(iv).put(encrypted);
        return Base64.encodeToString(output.array(),
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static byte[] decrypt(String encoded, char[] password) throws Exception {
        byte[] all = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        if (all.length < 1 + 4 + SALT_BYTES + IV_BYTES + 16) {
            throw new IllegalArgumentException("Short backup");
        }
        ByteBuffer input = ByteBuffer.wrap(all);
        if (input.get() != FORMAT_VERSION) throw new IllegalArgumentException("Version");
        int iterations = input.getInt();
        if (iterations < 10_000 || iterations > 1_000_000) {
            throw new IllegalArgumentException("KDF");
        }
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv = new byte[IV_BYTES];
        input.get(salt);
        input.get(iv);
        byte[] encrypted = new byte[input.remaining()];
        input.get(encrypted);
        SecretKey key = deriveKey(password, salt, iterations);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(encrypted);
    }

    private static SecretKey deriveKey(char[] password, byte[] salt, int iterations)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private static String extractPayload(String text) {
        int start = text.indexOf(PREFIX);
        if (start < 0) throw new IllegalArgumentException("Missing payload");
        start += PREFIX.length();
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            boolean accepted = c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9' || c == '_' || c == '-';
            if (!accepted) break;
            end++;
        }
        if (end == start) throw new IllegalArgumentException("Empty payload");
        return text.substring(start, end);
    }

    private static boolean isValidKey(String key) {
        if (key == null || key.length() != 13 || !key.startsWith("note_")) return false;
        for (int i = 5; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) return false;
        }
        return true;
    }

    private static void wipe(char[] value) {
        if (value == null) return;
        for (int i = 0; i < value.length; i++) value[i] = '\0';
    }

    private static void post(final Activity activity, final Runnable action) {
        try {
            activity.runOnUiThread(() -> {
                if (isUsable(activity)) action.run();
            });
        } catch (RuntimeException ignored) {
            // Activity may have been destroyed while encryption was running.
        }
    }

    private static void postToast(final Activity activity, final int message) {
        post(activity, () -> Toast.makeText(activity, message, Toast.LENGTH_LONG).show());
    }

    private static boolean isUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static final class PasswordForm {
        final LinearLayout content;
        final EditText password;
        final EditText confirm;

        PasswordForm(LinearLayout content, EditText password, EditText confirm) {
            this.content = content;
            this.password = password;
            this.confirm = confirm;
        }
    }

    private static int dp(Activity activity, float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
