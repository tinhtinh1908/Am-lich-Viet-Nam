package com.dtinh.lichviet;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/** Vietnamese lunar calendar calculations for UTC+7, based on astronomical new moons. */
public final class LunarCalendar {
    public static final double VIETNAM_TIME_ZONE = 7.0;

    private static final String[] CAN = {
            "Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"
    };
    private static final String[] CHI = {
            "Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"
    };
    private static final Map<Integer, LunarDate> CACHE =
            new LinkedHashMap<Integer, LunarDate>(384, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, LunarDate> eldest) {
                    return size() > 384;
                }
            };

    private LunarCalendar() {}

    public static final class LunarDate {
        public final int day;
        public final int month;
        public final int year;
        public final boolean leap;
        public final int julianDay;

        LunarDate(int day, int month, int year, boolean leap, int julianDay) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
            this.julianDay = julianDay;
        }

        public String shortText() {
            return String.format(Locale.US, "%d/%d%s", day, month, leap ? " N" : "");
        }

        public String fullText() {
            return String.format(Locale.US, "Ngày %d tháng %d%s năm %d",
                    day, month, leap ? " nhuận" : "", year);
        }
    }

    public static LunarDate fromSolar(int day, int month, int year) {
        int key = year * 512 + month * 32 + day;
        synchronized (CACHE) {
            LunarDate cached = CACHE.get(Integer.valueOf(key));
            if (cached != null) return cached;
        }
        LunarDate calculated = calculate(day, month, year);
        synchronized (CACHE) {
            CACHE.put(Integer.valueOf(key), calculated);
        }
        return calculated;
    }

    private static LunarDate calculate(int day, int month, int year) {
        int[] nativeResult = NativeCore.calculateLunar(day, month, year);
        if (nativeResult != null && nativeResult.length == 5) {
            return new LunarDate(nativeResult[0], nativeResult[1], nativeResult[2],
                    nativeResult[3] != 0, nativeResult[4]);
        }
        // JVM test fallback when the Android Rust library is unavailable.
        int dayNumber = jdFromDate(day, month, year);
        int k = (int) Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, VIETNAM_TIME_ZONE);
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, VIETNAM_TIME_ZONE);
        }
        int a11 = getLunarMonth11(year, VIETNAM_TIME_ZONE);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = year;
            a11 = getLunarMonth11(year - 1, VIETNAM_TIME_ZONE);
        } else {
            lunarYear = year + 1;
            b11 = getLunarMonth11(year + 1, VIETNAM_TIME_ZONE);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (int) Math.floor((monthStart - a11) / 29.0);
        int lunarMonth = diff + 11;
        boolean lunarLeap = false;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, VIETNAM_TIME_ZONE);
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) lunarLeap = true;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
        return new LunarDate(lunarDay, lunarMonth, lunarYear, lunarLeap, dayNumber);
    }

    public static String yearCanChi(int lunarYear) {
        return CAN[positiveMod(lunarYear + 6, 10)] + " " + CHI[positiveMod(lunarYear + 8, 12)];
    }

    public static String dayCanChi(int julianDay) {
        return CAN[positiveMod(julianDay + 9, 10)] + " " + CHI[positiveMod(julianDay + 1, 12)];
    }

    public static String monthCanChi(int lunarMonth, int lunarYear) {
        int can = positiveMod(lunarYear * 12 + lunarMonth + 3, 10);
        int chi = positiveMod(lunarMonth + 1, 12);
        return CAN[can] + " " + CHI[chi];
    }

    private static int positiveMod(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    private static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) {
            jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        }
        return jd;
    }

    private static int getNewMoonDay(int k, double timeZone) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = Math.PI / 180.0;
        double jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
        jd1 += 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
        double mPrime = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
        double c1 = (0.1734 - 0.000393 * T) * Math.sin(m * dr)
                + 0.0021 * Math.sin(2 * m * dr)
                - 0.4068 * Math.sin(mPrime * dr)
                + 0.0161 * Math.sin(2 * mPrime * dr)
                - 0.0004 * Math.sin(3 * mPrime * dr)
                + 0.0104 * Math.sin(2 * f * dr)
                - 0.0051 * Math.sin((m + mPrime) * dr)
                - 0.0074 * Math.sin((m - mPrime) * dr)
                + 0.0004 * Math.sin((2 * f + m) * dr)
                - 0.0004 * Math.sin((2 * f - m) * dr)
                - 0.0006 * Math.sin((2 * f + mPrime) * dr)
                + 0.0010 * Math.sin((2 * f - mPrime) * dr)
                + 0.0005 * Math.sin((2 * mPrime + m) * dr);
        double deltaT;
        if (T < -11) {
            deltaT = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3;
        } else {
            deltaT = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        double jdNew = jd1 + c1 - deltaT;
        return (int) Math.floor(jdNew + 0.5 + timeZone / 24.0);
    }

    private static int getSunLongitude(int jdn, double timeZone) {
        double T = (jdn - 2451545.5 - timeZone / 24.0) / 36525.0;
        double T2 = T * T;
        double dr = Math.PI / 180.0;
        double m = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
        double l0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
        double dl = (1.914600 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * m);
        dl += (0.019993 - 0.000101 * T) * Math.sin(2 * dr * m) + 0.000290 * Math.sin(3 * dr * m);
        double l = (l0 + dl) * dr;
        l -= Math.PI * 2 * Math.floor(l / (Math.PI * 2));
        return (int) Math.floor(l / Math.PI * 6);
    }

    private static int getLunarMonth11(int yy, double timeZone) {
        int off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = getNewMoonDay(k, timeZone);
        int sunLong = getSunLongitude(nm, timeZone);
        if (sunLong >= 9) nm = getNewMoonDay(k - 1, timeZone);
        return nm;
    }

    private static int getLeapMonthOffset(int a11, double timeZone) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last = 0;
        int i = 1;
        int arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        do {
            last = arc;
            i++;
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone);
        } while (arc != last && i < 14);
        return i - 1;
    }
}
