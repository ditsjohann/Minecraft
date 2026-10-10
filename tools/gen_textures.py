"""Procedural pixel-art texture generator for Armageddon Arsenal.

Run from the repository root:  python3 tools/generate_assets.py
(this module is imported by generate_assets.py)
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

RNG = random.Random(1945)


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def clamp8(v):
    return max(0, min(255, int(v)))


def shade(c, f):
    return tuple(clamp8(x * f) for x in c[:3])


def mix(a, b, t):
    return tuple(clamp8(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noise_tile(base, amount=0.12, size=16, seed=None, scale=1):
    r = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            f = 1 + (r.random() - 0.5) * 2 * amount
            px[x, y] = shade(base, f) + (255,)
    return img


def bevel(img, light=1.18, dark=0.7):
    px = img.load()
    w, h = img.size
    for x in range(w):
        px[x, 0] = shade(px[x, 0], light) + (px[x, 0][3],)
        px[x, h - 1] = shade(px[x, h - 1], dark) + (px[x, h - 1][3],)
    for y in range(h):
        px[0, y] = shade(px[0, y], light) + (px[0, y][3],)
        px[w - 1, y] = shade(px[w - 1, y], dark) + (px[w - 1, y][3],)
    return img


# --------------------------------------------------------------------- material tiles (16x16)

def mat_metal(base, seed, streaks=False, rivets=False, panel=False):
    img = noise_tile(rgb(base), 0.07, seed=seed)
    px = img.load()
    r = random.Random(seed)
    if streaks:
        for y in range(16):
            f = 1 + (r.random() - 0.5) * 0.16
            for x in range(16):
                px[x, y] = shade(px[x, y], f) + (255,)
    if panel:
        for i in range(16):
            px[i, 7] = shade(px[i, 7], 0.75) + (255,)
            px[i, 8] = shade(px[i, 8], 1.12) + (255,)
            px[7, i] = shade(px[7, i], 0.8) + (255,)
    if rivets:
        for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
            px[x, y] = shade(rgb(base), 1.45) + (255,)
            px[x + 1, y + 1] = shade(rgb(base), 0.6) + (255,)
    return bevel(img)


def mat_hazard(c1=0xF2C200, c2=0x1A1A1A):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = rgb(c1) if ((x + y) // 4) % 2 == 0 else rgb(c2)
            px[x, y] = shade(c, 1 + (RNG.random() - 0.5) * 0.1) + (255,)
    return bevel(img)


def mat_glow(c, seed):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = rgb(c)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 10.6
            core = mix((255, 255, 255), base, min(1, d * 1.6))
            f = 1.0 - 0.25 * d + (0.06 if y % 4 == 0 else 0)
            px[x, y] = shade(core, f) + (255,)
    return img


def mat_glass(c, alpha=150):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = rgb(c)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            hl = (x + y) in (4, 5, 11)
            col = shade(base, 1.3 if hl else (0.8 if edge else 1.0))
            px[x, y] = col + (230 if edge else (200 if hl else alpha),)
    return img


def mat_carbon():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 34 + (14 if ((x // 2 + y // 2) % 2 == 0) == (x % 2 == 0) else 0)
            px[x, y] = (v, v, v + 4, 255)
    return bevel(img, 1.4, 0.6)


def mat_vent(base=0x3A3F44):
    img = mat_metal(base, 7)
    px = img.load()
    for y in range(2, 14, 3):
        for x in range(2, 14):
            px[x, y] = (12, 12, 14, 255)
            px[x, y + 1] = shade(rgb(base), 1.25) + (255,)
    return img


def mat_screen(c=0x2AFF6A):
    img = Image.new("RGBA", (16, 16), (8, 20, 12, 255))
    px = img.load()
    base = rgb(c)
    for y in range(2, 14, 2):
        n = RNG.randint(4, 12)
        for x in range(2, 2 + n):
            px[x, y] = shade(base, 0.7 + RNG.random() * 0.5) + (255,)
    for x in range(16):
        px[x, 0] = px[x, 15] = (40, 44, 48, 255)
    for y in range(16):
        px[0, y] = px[15, y] = (40, 44, 48, 255)
    return img


def trefoil(draw, cx, cy, r, fg, bg=None):
    if bg is not None:
        draw.ellipse([cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1], fill=bg)
    for k in range(3):
        a0 = -90 + k * 120 - 30
        draw.pieslice([cx - r, cy - r, cx + r, cy + r], a0, a0 + 60, fill=fg)
    hole = max(1, r // 3)
    draw.ellipse([cx - hole, cy - hole, cx + hole, cy + hole], fill=bg if bg else (0, 0, 0, 0))
    dot = max(1, r // 5)
    draw.ellipse([cx - dot, cy - dot, cx + dot, cy + dot], fill=fg)


def mat_radiation():
    big = Image.new("RGBA", (64, 64), rgb(0xF2C200) + (255,))
    d = ImageDraw.Draw(big)
    trefoil(d, 32, 32, 26, (20, 20, 20, 255), rgb(0xF2C200) + (255,))
    img = big.resize((16, 16), Image.LANCZOS)
    return bevel(img)


def mat_biohazard():
    big = Image.new("RGBA", (64, 64), rgb(0xA8D820) + (255,))
    d = ImageDraw.Draw(big)
    for k in range(3):
        a = math.radians(-90 + k * 120)
        cx, cy = 32 + math.cos(a) * 11, 32 + math.sin(a) * 11
        d.ellipse([cx - 13, cy - 13, cx + 13, cy + 13], outline=(20, 30, 10, 255), width=5)
    d.ellipse([26, 26, 38, 38], outline=(20, 30, 10, 255), width=4)
    return bevel(big.resize((16, 16), Image.LANCZOS))


def mat_speaker():
    img = Image.new("RGBA", (16, 16), (20, 20, 24, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 7.5:
                ring = int(d) % 3 == 0
                v = 34 + int(30 * (1 - d / 7.5)) + (18 if ring else 0)
                px[x, y] = (v, v, v + 6, 255)
            if d < 2.2:
                px[x, y] = (70, 70, 80, 255)
    return bevel(img, 1.3, 0.6)


def mat_rock():
    img = noise_tile((62, 50, 44), 0.3, seed=77)
    px = img.load()
    r = random.Random(77)
    for _ in range(4):
        x, y = r.randint(0, 15), r.randint(0, 15)
        for k in range(5):
            xx, yy = (x + k) % 16, (y + r.randint(-1, 1)) % 16
            px[xx, yy] = (255, 120 + r.randint(0, 60), 20, 255)
    return img


def material_tiles():
    t = {}
    t["gunmetal"] = mat_metal(0x3B4148, 1, rivets=True)
    t["steel"] = mat_metal(0x9AA3AB, 2, streaks=True)
    t["chrome"] = mat_metal(0xD8E0E6, 3, streaks=True)
    t["titanium"] = mat_metal(0x8C97A6, 4, panel=True)
    t["olive"] = mat_metal(0x55603A, 5, rivets=True)
    t["white"] = mat_metal(0xE4E6E2, 6, panel=True)
    t["red"] = mat_metal(0xB3241E, 8)
    t["yellow"] = mat_metal(0xE6B800, 9, rivets=True)
    t["copper"] = mat_metal(0xC5743A, 10, streaks=True)
    t["black"] = mat_metal(0x16171A, 11)
    t["rubber"] = noise_tile((30, 30, 32), 0.08, seed=12)
    t["tsar"] = mat_metal(0xC9CCC4, 13, rivets=True, panel=True)
    t["fatman"] = mat_metal(0xD9B23A, 14, rivets=True)
    t["c4"] = noise_tile((205, 196, 160), 0.06, seed=15)
    t["toxic"] = mat_metal(0x7A9A20, 16)
    t["hazard"] = mat_hazard()
    t["carbon"] = mat_carbon()
    t["vent"] = mat_vent()
    t["screen"] = mat_screen()
    t["screen_red"] = mat_screen(0xFF3A2A)
    t["radiation"] = mat_radiation()
    t["biohazard"] = mat_biohazard()
    for name, c in [("cyan", 0x3AF2FF), ("green", 0x5AFF3A), ("orange", 0xFF8A1A), ("purple", 0xA040FF),
                    ("red", 0xFF2A2A), ("blue", 0x3A8CFF), ("magenta", 0xFF3AD8), ("white", 0xFFF4E0)]:
        t["glow_" + name] = mat_glow(c, name)
    t["speaker"] = mat_speaker()
    t["rock"] = mat_rock()
    t["glass_cyan"] = mat_glass(0x8AF0FF)
    t["glass_magenta"] = mat_glass(0xFF8AF0, 120)
    t["glass_purple"] = mat_glass(0xB070FF, 130)
    return t


# --------------------------------------------------------------------- 2D item sprites

def outline(img, color=(18, 18, 22, 255)):
    px = img.load()
    w, h = img.size
    src = img.copy().load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                        px[x, y] = color
                        break
    return img


def shaded_mask(mask, base, seed=0, light=1.3, dark=0.6, noise=0.08):
    """mask: 16x16 list of bools; shades with top-left light."""
    r = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            up = y == 0 or not mask[y - 1][x]
            left = x == 0 or not mask[y][x - 1]
            down = y == 15 or not mask[y + 1][x]
            right = x == 15 or not mask[y][x + 1]
            f = 1.0
            if up or left:
                f = light
            elif down or right:
                f = dark
            f *= 1 + (r.random() - 0.5) * 2 * noise
            px[x, y] = shade(base, f) + (255,)
    return img


def mask_from_draw(fn):
    big = Image.new("L", (16, 16), 0)
    d = ImageDraw.Draw(big)
    fn(d)
    px = big.load()
    return [[px[x, y] > 0 for x in range(16)] for y in range(16)]


def sprite_raw(base, seed):
    def f(d):
        d.polygon([(3, 6), (6, 3), (10, 3), (13, 6), (13, 10), (10, 13), (5, 13), (3, 10)], fill=255)
    img = shaded_mask(mask_from_draw(f), rgb(base), seed, noise=0.2)
    px = img.load()
    r = random.Random(seed)
    for _ in range(6):
        x, y = r.randint(5, 11), r.randint(5, 11)
        px[x, y] = shade(rgb(base), 0.55) + (255,)
    return outline(img)


def sprite_ingot(base, seed):
    def f(d):
        d.polygon([(2, 10), (6, 6), (14, 6), (14, 9), (10, 13), (2, 13)], fill=255)
    img = shaded_mask(mask_from_draw(f), rgb(base), seed)
    px = img.load()
    for x in range(6, 14):
        px[x, 7] = shade(rgb(base), 1.4) + (255,)
    return outline(img)


def sprite_plate(base, seed):
    def f(d):
        d.rectangle([2, 3, 13, 12], fill=255)
    img = shaded_mask(mask_from_draw(f), rgb(base), seed)
    px = img.load()
    for (x, y) in [(3, 4), (12, 4), (3, 11), (12, 11)]:
        px[x, y] = (230, 235, 240, 255)
    for x in range(4, 12):
        px[x, 8] = shade(rgb(base), 0.8) + (255,)
    return outline(img)


def sprite_circuit():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 3, 14, 12], fill=(30, 110, 50, 255))
    for y in (5, 8, 10):
        d.line([(2, y), (13, y)], fill=(210, 170, 60, 255))
    d.rectangle([5, 5, 9, 9], fill=(25, 25, 28, 255))
    d.rectangle([11, 4, 13, 6], fill=(25, 25, 28, 255))
    img.putpixel((7, 7), (90, 255, 140, 255))
    return outline(img)


def sprite_orb(core, glow, ring=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 5.6:
                hl = max(0, 1 - math.hypot(x - 5.5, y - 5.5) / 4)
                c = mix(rgb(core), (255, 255, 255), hl * 0.7)
                px[x, y] = c + (255,)
            elif d < 7.2:
                a = int(200 * (1 - (d - 5.6) / 1.6))
                px[x, y] = rgb(glow) + (a,)
    if ring:
        d = ImageDraw.Draw(img)
        d.ellipse([1, 5, 14, 10], outline=rgb(ring) + (255,))
    return img


def sprite_canister(body, window, cap=0x9AA3AB):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([5, 2, 10, 3], fill=rgb(cap) + (255,))
    d.rectangle([4, 4, 11, 12], fill=rgb(body) + (255,))
    d.rectangle([6, 5, 9, 11], fill=rgb(window) + (255,))
    d.rectangle([5, 13, 10, 14], fill=rgb(cap) + (255,))
    img.putpixel((6, 6), (255, 255, 255, 255))
    img = bevel_sprite(img)
    return outline(img)


def bevel_sprite(img):
    px = img.load()
    src = img.copy().load()
    for y in range(16):
        for x in range(16):
            if src[x, y][3] == 0:
                continue
            if x == 0 or src[x - 1, y][3] == 0:
                px[x, y] = shade(src[x, y], 1.25) + (src[x, y][3],)
            elif x == 15 or src[x + 1, y][3] == 0:
                px[x, y] = shade(src[x, y], 0.7) + (src[x, y][3],)
    return img


def sprite_armor(kind, base, accent, trim):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    b, a, t = rgb(base) + (255,), rgb(accent) + (255,), rgb(trim) + (255,)
    if kind == "helmet":
        d.rectangle([3, 3, 12, 11], fill=b)
        d.rectangle([4, 6, 11, 9], fill=a)
        d.line([(3, 3), (12, 3)], fill=t)
    elif kind == "chestplate":
        d.polygon([(2, 3), (5, 2), (10, 2), (13, 3), (13, 7), (11, 7), (11, 13), (4, 13), (4, 7), (2, 7)], fill=b)
        d.rectangle([6, 5, 9, 10], fill=a)
        d.line([(4, 12), (11, 12)], fill=t)
    elif kind == "leggings":
        d.rectangle([3, 2, 12, 5], fill=b)
        d.rectangle([3, 5, 6, 14], fill=b)
        d.rectangle([9, 5, 12, 14], fill=b)
        d.line([(3, 3), (12, 3)], fill=t)
        d.rectangle([4, 8, 5, 10], fill=a)
        d.rectangle([10, 8, 11, 10], fill=a)
    elif kind == "boots":
        d.rectangle([2, 7, 6, 13], fill=b)
        d.rectangle([9, 7, 13, 13], fill=b)
        d.rectangle([1, 12, 6, 14], fill=t)
        d.rectangle([9, 12, 14, 14], fill=t)
        d.point([(4, 9), (11, 9)], fill=a)
    elif kind == "pack":
        d.rectangle([4, 2, 11, 11], fill=b)
        d.rectangle([3, 11, 6, 14], fill=t)
        d.rectangle([9, 11, 12, 14], fill=t)
        d.rectangle([6, 4, 9, 8], fill=a)
        d.point([(4, 15), (5, 15), (10, 15), (11, 15)], fill=rgb(0xFF8A1A) + (255,))
    return outline(bevel_sprite(img))


def item_sprites():
    s = {}
    s["raw_uranium"] = sprite_raw(0x6FBF3A, 21)
    s["raw_titanium"] = sprite_raw(0x9AA6B8, 22)
    s["uranium_ingot"] = sprite_ingot(0x7CE04A, 23)
    s["titanium_ingot"] = sprite_ingot(0xB4C0CE, 24)
    s["titanium_plate"] = sprite_plate(0x9EAAB8, 25)
    s["circuit_board"] = sprite_circuit()
    s["enriched_uranium"] = sprite_canister(0x2A3A20, 0x9CFF4A)
    s["plutonium_core"] = sprite_orb(0x8A9AA8, 0x5ABFFF)
    s["fusion_core"] = sprite_canister(0x4A4F55, 0xFF9A2A, 0xE6B800)
    s["antimatter_cell"] = sprite_canister(0x2A2440, 0xFF4AE0, 0xD8E0E6)
    s["singularity_core"] = sprite_orb(0x0A0610, 0xA040FF, 0xD890FF)
    for kind, name in [("helmet", "helmet"), ("chestplate", "suit"), ("leggings", "leggings"), ("boots", "boots")]:
        s["hazmat_" + name] = sprite_armor(kind, 0xE6C21A, 0x8AF0FF, 0x2A2A2A)
    for kind in ["helmet", "chestplate", "leggings", "boots"]:
        s["juggernaut_" + kind] = sprite_armor(kind, 0x3B4148, 0xFF8A1A, 0x9AA3AB)
    s["thruster_pack"] = sprite_armor("pack", 0x5A636E, 0x3AF2FF, 0x2A2D31)
    s["titan_boots"] = sprite_armor("boots", 0xD8701A, 0x3AF2FF, 0x2A2D31)
    return s


# --------------------------------------------------------------------- blocks

def stone_base(seed, deep=False):
    base = (70, 72, 80) if deep else (125, 125, 125)
    img = noise_tile(base, 0.13, seed=seed)
    px = img.load()
    r = random.Random(seed)
    for _ in range(18):
        x, y = r.randint(0, 15), r.randint(0, 15)
        px[x, y] = shade(base, 0.78) + (255,)
    if deep:
        for y in range(0, 16, 4):
            for x in range(16):
                if r.random() < 0.5:
                    px[x, y] = shade(base, 0.85) + (255,)
    return img


def ore(seed, deep, color, glow=False):
    img = stone_base(seed, deep)
    px = img.load()
    r = random.Random(seed + 100)
    c = rgb(color)
    for _ in range(5):
        cx, cy = r.randint(2, 13), r.randint(2, 13)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16 and r.random() < 0.85:
                f = 1.3 if (dx, dy) == (0, 0) else 0.9
                px[x, y] = shade(c, f) + (255,)
    return img


def block_textures():
    b = {}
    b["uranium_ore"] = ore(31, False, 0x7CFF2E)
    b["deepslate_uranium_ore"] = ore(32, True, 0x7CFF2E)
    b["titanium_ore"] = ore(33, False, 0xC8D4E4)
    b["deepslate_titanium_ore"] = ore(34, True, 0xC8D4E4)
    u = Image.new("RGBA", (16, 16))
    px = u.load()
    for y in range(16):
        for x in range(16):
            edge = x % 8 in (0, 7) or y % 8 in (0, 7)
            g = mat_glow(0x5AFF3A, 1).getpixel((x % 8 * 2, y % 8 * 2))
            px[x, y] = (shade(g, 0.55) + (255,)) if edge else g
    b["uranium_block"] = u
    b["titanium_block"] = mat_metal(0x9EAAB8, 35, rivets=True, panel=True)
    tr = noise_tile((60, 140, 70), 0.18, seed=36)
    px = tr.load()
    r = random.Random(36)
    for _ in range(14):
        x, y = r.randint(0, 15), r.randint(0, 15)
        px[x, y] = (150, 255, 120, 255)
    for _ in range(3):
        x0, y0 = r.randint(0, 15), r.randint(0, 15)
        for k in range(6):
            x, y = (x0 + k) % 16, (y0 + r.randint(-1, 1)) % 16
            px[x, y] = (20, 50, 25, 255)
    b["trinitite"] = tr
    sc = noise_tile((38, 34, 32), 0.25, seed=37)
    px = sc.load()
    for _ in range(5):
        x, y = r.randint(0, 15), r.randint(0, 15)
        px[x, y] = (200, 70, 20, 255)
    b["scorched_earth"] = sc
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for y in range(16):
        for x in range(16):
            v = 28 + (10 if x % 4 == 0 else 0) + r.randint(-6, 6)
            px[x, y] = (v, v - 2, v - 4, 255)
            if r.random() < 0.03:
                px[x, y] = (180, 60, 20, 255)
    b["charred_log"] = side
    top = Image.new("RGBA", (16, 16))
    px = top.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            v = 50 + (12 if int(d) % 3 == 0 else 0) + r.randint(-5, 5)
            px[x, y] = (v, v - 4, v - 8, 255) if d < 7 else (30, 28, 26, 255)
    b["charred_log_top"] = top
    rc = noise_tile((150, 150, 148), 0.08, seed=38)
    px = rc.load()
    for i in range(16):
        for j in (4, 12):
            px[i, j] = (110, 100, 95, 255)
            px[j, i] = (110, 100, 95, 255)
    b["reinforced_concrete"] = bevel(rc, 1.1, 0.8)
    b["hazard_block"] = mat_hazard()
    return b


# --------------------------------------------------------------------- armour layers (64x32)

HEAD = (0, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
ARM = (40, 16, 4, 12, 4)
LEG = (0, 16, 4, 12, 4)


def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def fill_rect(px, rect, color_fn):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color_fn(x - x0, y - y0, w, h)


def armor_layers(base, accent, trim, glow, seed, kind):
    r = random.Random(seed)

    def plate(bx, by, w, h):
        c = rgb(base)
        f = 1 + (r.random() - 0.5) * 0.12
        if by == 0 or bx == 0:
            f *= 1.15
        if by == h - 1 or bx == w - 1:
            f *= 0.75
        return shade(c, f) + (255,)

    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p1, p2 = l1.load(), l2.load()
    for part in (HEAD, BODY, ARM, LEG):
        for face, rect in box_faces(*part).items():
            fill_rect(p1, rect, plate)
    for part in (BODY, LEG):
        for face, rect in box_faces(*part).items():
            fill_rect(p2, rect, plate)

    a, t, g = rgb(accent) + (255,), rgb(trim) + (255,), rgb(glow) + (255,)
    hf = box_faces(*HEAD)["front"]
    if kind == "hazmat":
        for y in range(hf[1] + 2, hf[1] + 6):
            for x in range(hf[0] + 1, hf[0] + 7):
                p1[x, y] = mix(rgb(accent), (255, 255, 255), 0.3 if y == hf[1] + 2 else 0) + (255,)
        p1[hf[0] + 3, hf[1] + 7] = t
        p1[hf[0] + 4, hf[1] + 7] = t
    elif kind == "juggernaut":
        for x in range(hf[0] + 1, hf[0] + 7):
            p1[x, hf[1] + 3] = g
            p1[x, hf[1] + 4] = shade(rgb(glow), 0.7) + (255,)
        for y in range(hf[1] + 5, hf[1] + 8):
            p1[hf[0] + 3, y] = t
            p1[hf[0] + 4, y] = t
    bf = box_faces(*BODY)["front"]
    if kind == "hazmat":
        for x in range(bf[0], bf[0] + 8):
            p1[x, bf[1] + 6] = t
        for y in range(bf[1] + 2, bf[1] + 5):
            for x in range(bf[0] + 2, bf[0] + 6):
                p1[x, y] = rgb(0x1A1A1A) + (255,) if (x + y) % 2 else rgb(0xF2C200) + (255,)
    elif kind == "juggernaut":
        for y in range(bf[1] + 2, bf[1] + 5):
            for x in range(bf[0] + 3, bf[0] + 5):
                p1[x, y] = g
        for x in range(bf[0], bf[0] + 8):
            p1[x, bf[1] + 8] = t
    elif kind == "thruster":
        for y in range(bf[1] + 2, bf[1] + 6):
            for x in range(bf[0] + 2, bf[0] + 6):
                p1[x, y] = a
        bb = box_faces(*BODY)["back"]
        for y in range(bb[1] + 1, bb[1] + 11):
            for x in range(bb[0] + 1, bb[0] + 7):
                p1[x, y] = shade(rgb(trim), 1.1 if x in (bb[0] + 1, bb[0] + 6) else 0.9) + (255,)
        for x in (bb[0] + 2, bb[0] + 5):
            p1[x, bb[1] + 10] = rgb(0xFF8A1A) + (255,)
            p1[x, bb[1] + 11] = rgb(0xFFD040) + (255,)
    lf = box_faces(*LEG)["front"]
    for y in (lf[1] + 4, lf[1] + 5):
        for x in range(lf[0], lf[0] + 4):
            p2[x, y] = a if kind != "hazmat" else t
            p1[x, y + 4] = t
    return l1, l2


# --------------------------------------------------------------------- particles

def particle_glow():
    img = Image.new("RGBA", (8, 8))
    px = img.load()
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5) / 4.0
            a = clamp8(255 * max(0, 1 - d) ** 1.5)
            px[x, y] = (255, 255, 255, a)
    return img


def particle_spark(i):
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    arm = 3 - (i % 2)
    for k in range(-arm, arm + 1):
        a = clamp8(255 * (1 - abs(k) / (arm + 1)))
        px[3 + k, 3] = (255, 255, 255, a)
        px[3, 3 + k] = (255, 255, 255, a)
    if i >= 2:
        for k in (-1, 1):
            px[3 + k, 3 + k] = (255, 255, 255, 140)
            px[3 + k, 3 - k] = (255, 255, 255, 140)
    px[3, 3] = (255, 255, 255, 255)
    return img


def particle_puff(seed, size=16, softness=1.0):
    r = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.hypot(xx - c, yy - c) / (size / 2)
    n = np.zeros((size, size))
    for octave, amp in ((2, 0.5), (4, 0.3), (8, 0.2)):
        g = r.random((octave + 1, octave + 1))
        gi = np.array(Image.fromarray((g * 255).astype(np.uint8)).resize((size, size), Image.BICUBIC)) / 255.0
        n += gi * amp
    shape = np.clip(1.0 - d * (1.15 + 0.5 * (n - 0.5)), 0, 1) ** softness
    alpha = np.clip(shape * 1.4 * (0.6 + 0.6 * n), 0, 1)
    v = np.clip(0.75 + 0.35 * n - 0.15 * d, 0, 1)
    arr = np.zeros((size, size, 4), dtype=np.uint8)
    arr[..., 0] = arr[..., 1] = arr[..., 2] = (v * 255).astype(np.uint8)
    arr[..., 3] = (alpha * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def particle_flame(i):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    r = random.Random(400 + i)
    for y in range(16):
        width = (y / 15.0) ** 0.8 * 6.5 * (0.85 + 0.3 * r.random())
        cx = 7.5 + math.sin(y * 0.6 + i) * 1.2
        for x in range(16):
            d = abs(x - cx)
            if d < width:
                core = 1 - d / max(width, 0.1)
                a = clamp8(255 * min(1, core * 1.6) * (0.4 + 0.6 * y / 15))
                v = clamp8(180 + 75 * core)
                px[x, y] = (v, v, v, a)
    return img


def mob_effect_icon():
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    big = Image.new("RGBA", (72, 72), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse([2, 2, 69, 69], fill=(242, 194, 0, 255), outline=(30, 30, 30, 255), width=4)
    trefoil(d, 36, 36, 26, (25, 25, 25, 255), (242, 194, 0, 255))
    img = big.resize((18, 18), Image.LANCZOS)
    return img


def mod_icon():
    size = 128
    img = Image.new("RGBA", (size, size), (14, 12, 18, 255))
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / size
        c = mix((255, 140, 40), (30, 10, 30), t)
        d.line([(0, y), (size, y)], fill=c + (255,))
    # mushroom cloud
    d.rectangle([54, 60, 74, 118], fill=(90, 70, 60, 255))
    d.ellipse([22, 18, 106, 74], fill=(120, 90, 70, 255))
    d.ellipse([32, 26, 96, 64], fill=(255, 190, 90, 255))
    d.ellipse([46, 34, 82, 56], fill=(255, 245, 210, 255))
    d.rectangle([0, 112, size, size], fill=(25, 20, 18, 255))
    trefoil(d, 100, 100, 18, (20, 20, 20, 255), (242, 194, 0, 255))
    return img
