import com.dtinh.lichviet.LunarCalendar;
import com.dtinh.lichviet.HolidayUtil;

public final class LunarCalendarTest {
    private static void expect(int solarDay, int solarMonth, int solarYear,
                               int lunarDay, int lunarMonth, int lunarYear) {
        LunarCalendar.LunarDate actual = LunarCalendar.fromSolar(solarDay, solarMonth, solarYear);
        if (actual.day != lunarDay || actual.month != lunarMonth || actual.year != lunarYear || actual.leap) {
            throw new AssertionError(solarDay + "/" + solarMonth + "/" + solarYear
                    + " expected " + lunarDay + "/" + lunarMonth + "/" + lunarYear
                    + " but was " + actual.shortText() + "/" + actual.year);
        }
    }

    public static void main(String[] args) {
        expect(10, 2, 2024, 1, 1, 2024);
        expect(29, 1, 2025, 1, 1, 2025);
        expect(17, 2, 2026, 1, 1, 2026);
        LunarCalendar.LunarDate today = LunarCalendar.fromSolar(22, 7, 2026);
        expectHoliday(2, 9, 2026, "Quốc khánh Việt Nam");
        expectHoliday(18, 4, 2024, "Giỗ Tổ Hùng Vương");
        expectHoliday(17, 2, 2026, "Tết Nguyên đán");
        System.out.println("22/7/2026 -> " + today.fullText());
        System.out.println("All lunar tests passed");
    }

    private static void expectHoliday(int day, int month, int year, String expected) {
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String actual = HolidayUtil.getHoliday(day, month, lunar);
        if (!actual.contains(expected)) {
            throw new AssertionError(day + "/" + month + "/" + year
                    + " expected holiday " + expected + " but was " + actual);
        }
    }
}
