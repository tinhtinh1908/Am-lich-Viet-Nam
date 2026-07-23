#!/usr/bin/env python3
"""Generate launcher-picker previews for the three supported widgets."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT = Path(__file__).resolve().parents[1] / "res" / "drawable-nodpi"
REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
BG = (29, 31, 37, 242)
PRIMARY = "#F5F7FA"
SECONDARY = "#A8ACB6"
MUTED = "#565A64"
BLUE = "#5B96F7"
SUNDAY = "#F47474"
DIVIDER = "#31343C"


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else REGULAR, size)


def centered(draw, box, text, selected_font, fill):
    left, top, right, bottom = box
    bounds = draw.textbbox((0, 0), text, font=selected_font)
    width = bounds[2] - bounds[0]
    height = bounds[3] - bounds[1]
    draw.text((left + (right - left - width) / 2 - bounds[0],
               top + (bottom - top - height) / 2 - bounds[1]),
              text, font=selected_font, fill=fill)


def base(width, height, radius=40):
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((4, 4, width - 4, height - 4), radius=radius, fill=BG)
    return image, draw


def preview_1x2():
    image, draw = base(280, 520, 48)
    centered(draw, (24, 18, 256, 66), "THÁNG 7", font(28, True), SECONDARY)
    draw.rounded_rectangle((25, 72, 255, 352), radius=42, fill="#4B8EEF")
    centered(draw, (30, 96, 250, 142), "T4", font(27, True), "#FFFFFF")
    centered(draw, (24, 128, 256, 330), "22", font(148, True), "#FFFFFF")
    centered(draw, (22, 362, 258, 416), "ÂM 9/6", font(40, True), PRIMARY)
    centered(draw, (20, 428, 260, 482), "BÍNH NGỌ", font(32, True), SECONDARY)
    image.save(OUT / "widget_preview_1x2.png", optimize=True)


def preview_2x1():
    image, draw = base(520, 260, 42)
    draw.rounded_rectangle((24, 20, 205, 240), radius=38, fill="#4B8EEF")
    centered(draw, (30, 46, 199, 88), "T4", font(27, True), "#FFFFFF")
    centered(draw, (24, 76, 205, 220), "22", font(110, True), "#FFFFFF")
    draw.text((232, 42), "THÁNG 7, 2026", font=font(32, True), fill=PRIMARY)
    draw.text((232, 104), "Âm 9/6", font=font(34, True), fill=SECONDARY)
    draw.text((232, 170), "Bính Ngọ", font=font(30, True), fill=SECONDARY)
    image.save(OUT / "widget_preview_2x1.png", optimize=True)


def lunar_label(day, month):
    if month == 6:
        return str(day - 14)
    if month == 7:
        if day <= 13:
            return str(day + 16)
        lunar = day - 13
        if lunar == 1:
            return "1/6"
        if lunar == 15:
            return "Rằm"
        return str(lunar)
    if month == 8:
        return str(day + 18)
    return ""


def preview_4x4():
    image, draw = base(620, 620, 46)
    draw.text((34, 30), "Tháng 7, 2026", font=font(31, True), fill=PRIMARY)
    draw.rounded_rectangle((410, 24, 455, 67), radius=18, fill="#263A57")
    draw.rounded_rectangle((462, 24, 542, 67), radius=18, fill="#263A57")
    draw.rounded_rectangle((550, 24, 595, 67), radius=18, fill="#263A57")
    centered(draw, (408, 22, 456, 66), "‹", font(30), SECONDARY)
    centered(draw, (454, 25, 550, 65), "HÔM NAY", font(12, True), BLUE)
    centered(draw, (548, 22, 596, 66), "›", font(30), SECONDARY)
    draw.rectangle((30, 78, 590, 80), fill=DIVIDER)

    weekdays = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]
    cell_w = 80
    left = 30
    for column, value in enumerate(weekdays):
        centered(draw, (left + column * cell_w, 84,
                        left + (column + 1) * cell_w, 118),
                 value, font(15, True), SUNDAY if column == 6 else SECONDARY)

    cells = [(29, 6), (30, 6)]
    cells.extend((day, 7) for day in range(1, 32))
    cells.extend((day, 8) for day in range(1, 10))
    row_top = 120
    row_h = 77
    for index, (day, month) in enumerate(cells):
        row = index // 7
        column = index % 7
        x0 = left + column * cell_w
        y0 = row_top + row * row_h
        in_month = month == 7
        today = in_month and day == 22
        sunday = column == 6
        if today:
            draw.ellipse((x0 + 24, y0 + 4, x0 + 56, y0 + 36), fill=BLUE)
        day_color = PRIMARY if in_month else MUTED
        if sunday and in_month:
            day_color = SUNDAY
        if today:
            day_color = "#FFFFFF"
        centered(draw, (x0, y0 + 2, x0 + cell_w, y0 + 38), str(day),
                 font(20, True), day_color)
        lunar = lunar_label(day, month)
        lunar_color = MUTED if not in_month else SECONDARY
        if lunar == "Rằm":
            lunar_color = SUNDAY
        centered(draw, (x0 + 2, y0 + 40, x0 + cell_w - 2, y0 + 65), lunar,
                 font(11, lunar == "Rằm"), lunar_color)

    image.save(OUT / "widget_preview_4x4.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    preview_1x2()
    preview_2x1()
    preview_4x4()


if __name__ == "__main__":
    main()
