#!/usr/bin/env python3
"""Generate the 31 date-specific launcher bitmaps used by activity aliases."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


SIZE = 432
BLUE = "#5B96F7"
DARK = "#202124"
WHITE = "#FFFFFF"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
OUTPUT = Path(__file__).resolve().parents[1] / "res" / "drawable-nodpi"


def centered(draw, bounds, text, font, fill):
    left, top, right, bottom = bounds
    box = draw.textbbox((0, 0), text, font=font)
    width = box[2] - box[0]
    height = box[3] - box[1]
    x = left + (right - left - width) / 2 - box[0]
    y = top + (bottom - top - height) / 2 - box[1]
    draw.text((x, y), text, font=font, fill=fill)


def generate(day):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((18, 18, 414, 414), radius=94, fill=BLUE)
    draw.rounded_rectangle((66, 72, 366, 370), radius=48, fill=WHITE)
    draw.rounded_rectangle((114, 54, 142, 130), radius=14, fill=WHITE)
    draw.rounded_rectangle((290, 54, 318, 130), radius=14, fill=WHITE)
    draw.rectangle((66, 112, 366, 165), fill=BLUE)

    title_font = ImageFont.truetype(FONT_BOLD, 28)
    number_font = ImageFont.truetype(FONT_BOLD, 166 if day < 10 else 142)
    centered(draw, (76, 115, 356, 163), "LỊCH VIỆT", title_font, WHITE)
    centered(draw, (72, 165, 360, 352), str(day), number_font, DARK)

    path = OUTPUT / f"ic_launcher_day_{day:02d}.png"
    image.save(path, optimize=True)


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for day in range(1, 32):
        generate(day)


if __name__ == "__main__":
    main()
