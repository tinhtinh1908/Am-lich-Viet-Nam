#![deny(unsafe_op_in_unsafe_fn)]

use jni::objects::JClass;
use jni::sys::{jint, jintArray};
use jni::JNIEnv;

const VERSION: jint = 151;

#[no_mangle]
pub extern "system" fn Java_com_dtinh_lichviet_NativeCore_nativeVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    VERSION
}

#[no_mangle]
pub extern "system" fn Java_com_dtinh_lichviet_NativeCore_nativeLunarFromSolar(
    env: JNIEnv,
    _class: JClass,
    day: jint,
    month: jint,
    year: jint,
) -> jintArray {
    if !(1..=31).contains(&day) || !(1..=12).contains(&month) || !(1800..=2300).contains(&year) {
        return std::ptr::null_mut();
    }
    let lunar = lunar_from_solar(day, month, year);
    let result = match env.new_int_array(5) {
        Ok(value) => value,
        Err(_) => return std::ptr::null_mut(),
    };
    let values = [lunar.day, lunar.month, lunar.year, lunar.leap as jint, lunar.julian_day];
    if env.set_int_array_region(&result, 0, &values).is_err() {
        return std::ptr::null_mut();
    }
    result.into_raw()
}

#[derive(Debug, PartialEq)]
struct LunarDate {
    day: i32,
    month: i32,
    year: i32,
    leap: bool,
    julian_day: i32,
}

fn lunar_from_solar(day: i32, month: i32, year: i32) -> LunarDate {
    const TZ: f64 = 7.0;
    let day_number = jd_from_date(day, month, year);
    let k = ((day_number as f64 - 2_415_021.076_998_695) / 29.530_588_853).floor() as i32;
    let mut month_start = new_moon_day(k + 1, TZ);
    if month_start > day_number {
        month_start = new_moon_day(k, TZ);
    }
    let mut a11 = lunar_month_11(year, TZ);
    let mut b11 = a11;
    let mut lunar_year;
    if a11 >= month_start {
        lunar_year = year;
        a11 = lunar_month_11(year - 1, TZ);
    } else {
        lunar_year = year + 1;
        b11 = lunar_month_11(year + 1, TZ);
    }
    let lunar_day = day_number - month_start + 1;
    let diff = ((month_start - a11) as f64 / 29.0).floor() as i32;
    let mut lunar_month = diff + 11;
    let mut lunar_leap = false;
    if b11 - a11 > 365 {
        let leap_diff = leap_month_offset(a11, TZ);
        if diff >= leap_diff {
            lunar_month = diff + 10;
            lunar_leap = diff == leap_diff;
        }
    }
    if lunar_month > 12 {
        lunar_month -= 12;
    }
    if lunar_month >= 11 && diff < 4 {
        lunar_year -= 1;
    }
    LunarDate {
        day: lunar_day,
        month: lunar_month,
        year: lunar_year,
        leap: lunar_leap,
        julian_day: day_number,
    }
}

fn jd_from_date(day: i32, month: i32, year: i32) -> i32 {
    let a = (14 - month) / 12;
    let y = year + 4800 - a;
    let m = month + 12 * a - 3;
    let mut jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    if jd < 2_299_161 {
        jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
    }
    jd
}

fn new_moon_day(k: i32, time_zone: f64) -> i32 {
    let kf = k as f64;
    let t = kf / 1236.85;
    let t2 = t * t;
    let t3 = t2 * t;
    let dr = std::f64::consts::PI / 180.0;
    let mut jd1 = 2_415_020.759_33 + 29.530_588_68 * kf + 0.000_117_8 * t2 - 0.000_000_155 * t3;
    jd1 += 0.000_33 * ((166.56 + 132.87 * t - 0.009_173 * t2) * dr).sin();
    let m = 359.2242 + 29.105_356_08 * kf - 0.000_033_3 * t2 - 0.000_003_47 * t3;
    let mp = 306.0253 + 385.816_918_06 * kf + 0.010_730_6 * t2 + 0.000_012_36 * t3;
    let f = 21.2964 + 390.670_506_46 * kf - 0.001_652_8 * t2 - 0.000_002_39 * t3;
    let c1 = (0.1734 - 0.000_393 * t) * (m * dr).sin()
        + 0.0021 * (2.0 * m * dr).sin()
        - 0.4068 * (mp * dr).sin()
        + 0.0161 * (2.0 * mp * dr).sin()
        - 0.0004 * (3.0 * mp * dr).sin()
        + 0.0104 * (2.0 * f * dr).sin()
        - 0.0051 * ((m + mp) * dr).sin()
        - 0.0074 * ((m - mp) * dr).sin()
        + 0.0004 * ((2.0 * f + m) * dr).sin()
        - 0.0004 * ((2.0 * f - m) * dr).sin()
        - 0.0006 * ((2.0 * f + mp) * dr).sin()
        + 0.0010 * ((2.0 * f - mp) * dr).sin()
        + 0.0005 * ((2.0 * mp + m) * dr).sin();
    let delta_t = if t < -11.0 {
        0.001 + 0.000_839 * t + 0.000_226_1 * t2 - 0.000_008_45 * t3 - 0.000_000_081 * t * t3
    } else {
        -0.000_278 + 0.000_265 * t + 0.000_262 * t2
    };
    (jd1 + c1 - delta_t + 0.5 + time_zone / 24.0).floor() as i32
}

fn sun_longitude(jdn: i32, time_zone: f64) -> i32 {
    let t = (jdn as f64 - 2_451_545.5 - time_zone / 24.0) / 36_525.0;
    let t2 = t * t;
    let dr = std::f64::consts::PI / 180.0;
    let m = 357.529_10 + 35_999.050_30 * t - 0.000_155_9 * t2 - 0.000_000_48 * t * t2;
    let l0 = 280.466_45 + 36_000.769_83 * t + 0.000_303_2 * t2;
    let mut dl = (1.914_600 - 0.004_817 * t - 0.000_014 * t2) * (dr * m).sin();
    dl += (0.019_993 - 0.000_101 * t) * (2.0 * dr * m).sin()
        + 0.000_290 * (3.0 * dr * m).sin();
    let mut l = (l0 + dl) * dr;
    l -= std::f64::consts::PI * 2.0 * (l / (std::f64::consts::PI * 2.0)).floor();
    (l / std::f64::consts::PI * 6.0).floor() as i32
}

fn lunar_month_11(year: i32, time_zone: f64) -> i32 {
    let offset = jd_from_date(31, 12, year) - 2_415_021;
    let k = (offset as f64 / 29.530_588_853).floor() as i32;
    let mut new_moon = new_moon_day(k, time_zone);
    if sun_longitude(new_moon, time_zone) >= 9 {
        new_moon = new_moon_day(k - 1, time_zone);
    }
    new_moon
}

fn leap_month_offset(a11: i32, time_zone: f64) -> i32 {
    let k = ((a11 as f64 - 2_415_021.076_998_695) / 29.530_588_853 + 0.5).floor() as i32;
    let mut i = 1;
    let mut arc = sun_longitude(new_moon_day(k + i, time_zone), time_zone);
    loop {
        let last = arc;
        i += 1;
        arc = sun_longitude(new_moon_day(k + i, time_zone), time_zone);
        if arc == last || i >= 14 {
            break;
        }
    }
    i - 1
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn vietnamese_new_year_dates() {
        assert_eq!(lunar_from_solar(10, 2, 2024), LunarDate {
            day: 1, month: 1, year: 2024, leap: false, julian_day: 2_460_351,
        });
        let date = lunar_from_solar(29, 1, 2025);
        assert_eq!((date.day, date.month, date.year, date.leap), (1, 1, 2025, false));
        let date = lunar_from_solar(17, 2, 2026);
        assert_eq!((date.day, date.month, date.year, date.leap), (1, 1, 2026, false));
    }
}
