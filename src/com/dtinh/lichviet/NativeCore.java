package com.dtinh.lichviet;

/** JNI bridge for the Rust Vietnamese lunar-calendar calculation core. */
public final class NativeCore {
    private static final boolean LOADED;

    static {
        boolean loaded;
        try {
            System.loadLibrary("lichviet_core");
            loaded = nativeVersion() == 151;
        } catch (Throwable ignored) {
            loaded = false;
        }
        LOADED = loaded;
    }

    private NativeCore() {}

    static int[] calculateLunar(int day, int month, int year) {
        if (!LOADED) return null;
        try {
            return nativeLunarFromSolar(day, month, year);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static native int nativeVersion();
    private static native int[] nativeLunarFromSolar(int day, int month, int year);
}
