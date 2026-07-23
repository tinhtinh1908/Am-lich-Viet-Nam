package com.dtinh.lichviet;

public final class HolidayUtil {
    private HolidayUtil() {}

    public static String getHoliday(int solarDay, int solarMonth, LunarCalendar.LunarDate lunar) {
        String solar = solarHoliday(solarDay, solarMonth);
        String lunarText = lunar.leap ? "" : lunarHoliday(lunar.day, lunar.month);
        if (!solar.isEmpty() && !lunarText.isEmpty()) return solar + " · " + lunarText;
        return !solar.isEmpty() ? solar : lunarText;
    }

    public static String getShortLabel(int solarDay, int solarMonth, LunarCalendar.LunarDate lunar) {
        if (!lunar.leap) {
            if (lunar.day == 1 && lunar.month == 1) return "Tết";
            if (lunar.day == 2 && lunar.month == 1) return "Mùng 2 Tết";
            if (lunar.day == 3 && lunar.month == 1) return "Mùng 3 Tết";
            if (lunar.day == 15 && lunar.month == 1) return "Rằm tháng Giêng";
            if (lunar.day == 10 && lunar.month == 3) return "Giỗ Tổ";
            if (lunar.day == 15 && lunar.month == 4) return "Phật Đản";
            if (lunar.day == 5 && lunar.month == 5) return "Đoan Ngọ";
            if (lunar.day == 15 && lunar.month == 7) return "Vu Lan";
            if (lunar.day == 15 && lunar.month == 8) return "Trung Thu";
            if (lunar.day == 23 && lunar.month == 12) return "Táo quân";
        }
        if (solarDay == 1 && solarMonth == 1) return "Tết DL";
        if (solarDay == 3 && solarMonth == 2) return "Thành lập Đảng";
        if (solarDay == 27 && solarMonth == 2) return "Thầy thuốc VN";
        if (solarDay == 8 && solarMonth == 3) return "Quốc tế PN";
        if (solarDay == 26 && solarMonth == 3) return "Thành lập Đoàn";
        if (solarDay == 30 && solarMonth == 4) return "Thống nhất";
        if (solarDay == 1 && solarMonth == 5) return "Quốc tế LĐ";
        if (solarDay == 7 && solarMonth == 5) return "Điện Biên Phủ";
        if (solarDay == 19 && solarMonth == 5) return "Sinh nhật Bác";
        if (solarDay == 1 && solarMonth == 6) return "Thiếu nhi";
        if (solarDay == 21 && solarMonth == 6) return "Báo chí VN";
        if (solarDay == 28 && solarMonth == 6) return "Gia đình VN";
        if (solarDay == 27 && solarMonth == 7) return "Thương binh LS";
        if (solarDay == 19 && solarMonth == 8) return "Cách mạng T8";
        if (solarDay == 2 && solarMonth == 9) return "Quốc khánh";
        if (solarDay == 10 && solarMonth == 10) return "Giải phóng HN";
        if (solarDay == 13 && solarMonth == 10) return "Doanh nhân VN";
        if (solarDay == 20 && solarMonth == 10) return "Phụ nữ VN";
        if (solarDay == 9 && solarMonth == 11) return "Pháp luật VN";
        if (solarDay == 20 && solarMonth == 11) return "Nhà giáo VN";
        if (solarDay == 22 && solarMonth == 12) return "QĐND VN";
        if (!lunar.leap && lunar.day == 15) return "Rằm";
        if (lunar.day == 1) return "1/" + lunar.month;
        return Integer.toString(lunar.day);
    }

    private static String solarHoliday(int day, int month) {
        if (day == 1 && month == 1) return "Tết Dương lịch";
        if (day == 3 && month == 2) return "Ngày thành lập ĐCSVN";
        if (day == 27 && month == 2) return "Ngày Thầy thuốc Việt Nam";
        if (day == 8 && month == 3) return "Ngày Quốc tế Phụ nữ";
        if (day == 26 && month == 3) return "Ngày thành lập Đoàn TNCS Hồ Chí Minh";
        if (day == 30 && month == 4) return "Ngày Giải phóng miền Nam";
        if (day == 1 && month == 5) return "Ngày Quốc tế Lao động";
        if (day == 7 && month == 5) return "Ngày Chiến thắng Điện Biên Phủ";
        if (day == 19 && month == 5) return "Ngày sinh Chủ tịch Hồ Chí Minh";
        if (day == 1 && month == 6) return "Ngày Quốc tế Thiếu nhi";
        if (day == 21 && month == 6) return "Ngày Báo chí Cách mạng Việt Nam";
        if (day == 28 && month == 6) return "Ngày Gia đình Việt Nam";
        if (day == 27 && month == 7) return "Ngày Thương binh - Liệt sĩ";
        if (day == 19 && month == 8) return "Ngày Cách mạng Tháng Tám";
        if (day == 2 && month == 9) return "Quốc khánh Việt Nam";
        if (day == 10 && month == 10) return "Ngày Giải phóng Thủ đô";
        if (day == 13 && month == 10) return "Ngày Doanh nhân Việt Nam";
        if (day == 20 && month == 10) return "Ngày Phụ nữ Việt Nam";
        if (day == 9 && month == 11) return "Ngày Pháp luật Việt Nam";
        if (day == 20 && month == 11) return "Ngày Nhà giáo Việt Nam";
        if (day == 22 && month == 12) return "Ngày thành lập QĐND Việt Nam";
        return "";
    }

    private static String lunarHoliday(int day, int month) {
        if (day == 1 && month == 1) return "Tết Nguyên đán";
        if (day == 2 && month == 1) return "Mùng 2 Tết Nguyên đán";
        if (day == 3 && month == 1) return "Mùng 3 Tết Nguyên đán";
        if (day == 15 && month == 1) return "Rằm tháng Giêng";
        if (day == 10 && month == 3) return "Giỗ Tổ Hùng Vương";
        if (day == 15 && month == 4) return "Lễ Phật Đản";
        if (day == 5 && month == 5) return "Tết Đoan Ngọ";
        if (day == 15 && month == 7) return "Lễ Vu Lan";
        if (day == 15 && month == 8) return "Tết Trung Thu";
        if (day == 23 && month == 12) return "Ông Công Ông Táo";
        return "";
    }
}
