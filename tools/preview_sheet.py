"""Tiles the frames written by `./gradlew fxPreview` (build/fx-previews/*.jpg) into one contact sheet.

    python3 tools/preview_sheet.py [out.jpg] [scale] [columns]

Defaults: build/fx-previews-sheet.jpg, scale 0.5, 4 columns. Each frame is labelled with its file name.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("build/fx-previews-sheet.jpg")
    scale = float(sys.argv[2]) if len(sys.argv) > 2 else 0.5
    cols = int(sys.argv[3]) if len(sys.argv) > 3 else 4
    frames = sorted(Path("build/fx-previews").glob("*.jpg"))
    if not frames:
        sys.exit("no frames in build/fx-previews: run ./gradlew fxPreview first")
    imgs = [Image.open(f).convert("RGB") for f in frames]
    w, h = int(imgs[0].width * scale), int(imgs[0].height * scale)
    rows = (len(imgs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * w, rows * h), (20, 20, 20))
    draw = ImageDraw.Draw(sheet)
    for i, (f, img) in enumerate(zip(frames, imgs)):
        x, y = (i % cols) * w, (i // cols) * h
        sheet.paste(img.resize((w, h)), (x, y))
        draw.rectangle([x, y, x + 8 + 7 * len(f.stem), y + 16], fill=(0, 0, 0))
        draw.text((x + 4, y + 3), f.stem, fill=(255, 255, 255))
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out, quality=88)
    print(f"{out}: {len(imgs)} frames")


if __name__ == "__main__":
    main()
