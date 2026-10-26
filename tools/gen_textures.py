"""Procedural pixel-art texture generator for Armageddon Arsenal.

Run from the repository root:  python3 tools/generate_assets.py
(this module is imported by generate_assets.py)
"""
import math
import random

import zlib

import numpy as np
from PIL import Image, ImageDraw

import texkit as K

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

def seed_of(seed):
    return seed if isinstance(seed, int) else zlib.crc32(str(seed).encode()) & 0xFFFF


def mat_metal(base, seed, streaks=False, rivets=False, panel=False, finish=None, wear=0.45):
    if finish is None:
        finish = "brushed" if streaks else "painted"
    return K.metal(base, seed_of(seed), finish=finish, panel=panel, rivets=rivets, wear=wear)


def mat_hazard(c1=0xF2C200, c2=0x1A1A1A, seed=3):
    yy, xx = np.mgrid[0:16, 0:16]
    band = ((xx + yy) // 4) % 2 == 0
    rgb = np.where(band[..., None], K.rgbf(c1)[None, None, :], K.rgbf(c2)[None, None, :])
    # Each stripe is a slightly domed, worn strip of paint.
    phase = ((xx + yy) % 4) / 3.0
    rgb = rgb * (1.08 - 0.16 * phase)[..., None]
    w = K.fbm(seed + 40)
    chip = w > 0.7
    rgb[chip] = rgb[chip] * 0.4 + np.array([0.28, 0.28, 0.3]) * 0.6
    rgb *= (1 + (K.fbm(seed) - 0.5) * 0.18)[..., None]
    rgb *= (1 - np.clip(yy / 15.0 - 0.6, 0, 1) * 0.2)[..., None]
    return K.to_img(rgb)


def mat_glow(c, seed, phase=0.0):
    return K.glow_frame(c, seed_of(seed), phase)


def mat_glass(c, alpha=150, seed=11):
    return K.glass(c, seed_of(seed), alpha)


def mat_carbon():
    yy, xx = np.mgrid[0:16, 0:16]
    weave = ((xx // 2 + yy // 2) % 2 == 0)
    along = np.where(weave, (xx % 2) / 1.0, (yy % 2) / 1.0)
    v = 0.12 + 0.07 * np.where(weave, np.sin((yy % 2 + 0.5) * math.pi), np.sin((xx % 2 + 0.5) * math.pi)) + 0.02 * along
    diag = (xx + yy) / 30.0
    v = v + np.exp(-((diag - 0.35) / 0.18) ** 2) * 0.1
    rgb = np.dstack([v, v, v * 1.12])
    return K.to_img(rgb)


def mat_vent(base=0x3A3F44):
    img = np.array(mat_metal(base, 7, streaks=True, wear=0.2)).astype(float) / 255
    for y in range(2, 14, 3):
        for x in range(1, 15):
            img[y, x, :3] = np.array([0.03, 0.03, 0.04]) + 0.05 * (x / 15)
            img[y + 1, x, :3] = np.minimum(1, img[y + 1, x, :3] * 1.35)
            if y > 2:
                img[y - 1, x, :3] *= 0.8
    return K.to_img(img[..., :3])


def mat_screen(c=0x2AFF6A, phase=0.0, seed=21):
    base = K.rgbf(c)
    rng = np.random.default_rng(seed)
    rgb = np.zeros((16, 16, 3)) + base * 0.06 + np.array([0.02, 0.03, 0.04])
    yy, xx = np.mgrid[0:16, 0:16]
    # Scrolling lines of "text", a waveform and a sweep bar.
    shift = int(phase * 16)
    lines = rng.integers(3, 12, 32)
    for row in range(2, 14, 2):
        n = lines[(row // 2 + shift) % 32]
        for x in range(2, 2 + n):
            k = 0.65 + 0.35 * ((x * 7 + row * 3 + shift) % 5) / 4
            rgb[row, x] = base * k
    wave = (8 + 2.2 * np.sin(xx[0] * 0.8 + phase * math.tau)).astype(int)
    for x in range(10, 14):
        rgb[np.clip(wave[x] + (x % 2), 2, 13), x] = np.minimum(1, base * 1.2 + 0.2)
    sweep = (phase * 16) % 16
    bar = np.exp(-((yy - sweep) / 1.2) ** 2) * 0.25
    rgb = rgb + base[None, None, :] * bar[..., None]
    rgb *= (0.88 + 0.12 * (yy % 2))[..., None]
    glow = K.soft_disc(16, 7.5, 7.5, 11, 1.0) * 0.12
    rgb = rgb + base[None, None, :] * glow[..., None]
    frame = (xx == 0) | (yy == 0) | (xx == 15) | (yy == 15)
    rgb[frame] = np.array([0.16, 0.17, 0.19])
    rgb[0, :] = np.array([0.24, 0.25, 0.28])
    rgb[(xx == 1) & (yy == 1)] = np.minimum(1, base + 0.3)
    return K.to_img(rgb)


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


def worn_sign(big, bg_hex, seed):
    """Downsample a 64px symbol onto a worn painted-metal plate."""
    sym = np.array(big.resize((16, 16), Image.LANCZOS)).astype(float) / 255
    plate = np.array(mat_metal(bg_hex, seed, wear=0.35)).astype(float) / 255
    bg = K.rgbf(bg_hex)
    # Where the symbol differs from the background colour, take the symbol (slightly worn).
    diff = np.abs(sym[..., :3] - bg[None, None, :]).sum(axis=2)
    k = np.clip(diff * 2.5, 0, 1)
    wear = K.fbm(seed + 3) > 0.74
    k[wear] *= 0.45
    light = (plate[..., :3].mean(axis=2) / max(1e-3, bg.mean()))[..., None]
    rgb = plate[..., :3] * (1 - k[..., None]) + sym[..., :3] * k[..., None] * np.clip(light, 0.7, 1.3)
    return K.to_img(rgb)


def mat_radiation():
    big = Image.new("RGBA", (64, 64), rgb(0xF2C200) + (255,))
    d = ImageDraw.Draw(big)
    trefoil(d, 32, 32, 26, (20, 20, 20, 255), rgb(0xF2C200) + (255,))
    return worn_sign(big, 0xF2C200, 81)


def mat_biohazard():
    big = Image.new("RGBA", (64, 64), rgb(0xA8D820) + (255,))
    d = ImageDraw.Draw(big)
    for k in range(3):
        a = math.radians(-90 + k * 120)
        cx, cy = 32 + math.cos(a) * 11, 32 + math.sin(a) * 11
        d.ellipse([cx - 13, cy - 13, cx + 13, cy + 13], outline=(20, 30, 10, 255), width=5)
    d.ellipse([26, 26, 38, 38], outline=(20, 30, 10, 255), width=4)
    return worn_sign(big, 0xA8D820, 82)


def mat_speaker():
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    h = np.clip(1 - d / 7.5, 0, 1) ** 0.7 * 0.6 + np.cos(d * 2.2) * 0.08
    f = K.lit(h, 2.5)
    v = (0.16 + 0.14 * np.clip(1 - d / 7.5, 0, 1)) * f
    rgb = np.dstack([v, v, v * 1.12])
    outer = d >= 7.4
    rgb[outer] = np.array([0.08, 0.08, 0.1])
    cap = d < 2.2
    rgb[cap] = np.array([0.3, 0.3, 0.36]) * (1.2 - d[cap] / 3)[..., None]
    return K.to_img(rgb)


def mat_rock(seed=77):
    n = K.fbm(seed)
    f = K.lit(n * 3.0, 1.6)
    rgb = K.tint(np.array([0.24, 0.2, 0.18]), (0.75 + 0.5 * n) * f)
    crack = np.abs(K.tnoise(seed + 1, 4) - K.tnoise(seed + 2, 4))
    lava = np.clip(1 - crack * 9, 0, 1)
    hot = np.dstack([np.ones_like(lava), 0.35 + 0.45 * lava, 0.05 + 0.1 * lava])
    rgb = rgb * (1 - lava[..., None]) + hot * lava[..., None]
    return K.to_img(rgb)


def mat_rubber():
    """Diamond-knurled grip rubber: raised pyramids lit from the top-left, slightly blue-black."""
    yy, xx = np.mgrid[0:16, 0:16]
    u, v = (xx + yy) % 4, (xx - yy) % 4
    h = (np.minimum(u, 4 - u) + np.minimum(v, 4 - v)) / 4.0
    f = K.lit(h, 1.6)
    val = (0.13 + 0.03 * (K.fbm(12) - 0.5)) * f
    return K.to_img(np.dstack([val, val * 1.02, val * 1.1]))


def mat_c4():
    n = K.fbm(15)
    rgb = K.tint(K.rgbf(0xCDC4A0), (0.85 + 0.3 * n) * K.lit(n * 2, 1.0))
    tape = (np.arange(16) >= 6) & (np.arange(16) < 9)
    rgb[:, tape] = rgb[:, tape] * 0.4 + np.array([0.14, 0.15, 0.17]) * 0.6
    return K.to_img(rgb)


def material_tiles():
    t = {}
    t["gunmetal"] = mat_metal(0x3B4148, 1, rivets=True, finish="cast")
    t["steel"] = mat_metal(0x9AA3AB, 2, streaks=True)
    t["chrome"] = mat_metal(0xD8E0E6, 3, finish="polished", wear=0.1)
    t["titanium"] = mat_metal(0x8C97A6, 4, panel=True, finish="anodised")
    t["olive"] = mat_metal(0x55603A, 5, rivets=True, finish="painted")
    t["white"] = mat_metal(0xE4E6E2, 6, panel=True, finish="painted", wear=0.3)
    t["red"] = mat_metal(0xB3241E, 8, finish="painted")
    t["yellow"] = mat_metal(0xE6B800, 9, rivets=True, finish="painted")
    t["copper"] = mat_metal(0xC5743A, 10, finish="polished", wear=0.6)
    t["black"] = mat_metal(0x1C1E23, 11, finish="cast", wear=0.15)
    t["rubber"] = mat_rubber()
    t["tsar"] = mat_metal(0xC9CCC4, 13, rivets=True, panel=True, finish="painted")
    t["fatman"] = mat_metal(0xD9B23A, 14, rivets=True, finish="painted", wear=0.6)
    t["c4"] = mat_c4()
    t["toxic"] = mat_metal(0x7A9A20, 16, finish="painted", wear=0.7)
    t["hazard"] = mat_hazard()
    t["carbon"] = mat_carbon()
    t["vent"] = mat_vent()
    t["screen"] = mat_screen()
    t["screen_red"] = mat_screen(0xFF3A2A, seed=22)
    t["radiation"] = mat_radiation()
    t["biohazard"] = mat_biohazard()
    for name, c in GLOWS:
        t["glow_" + name] = mat_glow(c, name)
    t["speaker"] = mat_speaker()
    t["rock"] = mat_rock()
    t["glass_cyan"] = mat_glass(0x8AF0FF)
    t["glass_magenta"] = mat_glass(0xFF8AF0, 120)
    t["glass_purple"] = mat_glass(0xB070FF, 130)
    return t


GLOWS = [("cyan", 0x3AF2FF), ("green", 0x5AFF3A), ("orange", 0xFF8A1A), ("purple", 0xA040FF),
         ("red", 0xFF2A2A), ("blue", 0x3A8CFF), ("magenta", 0xFF3AD8), ("white", 0xFFF4E0)]


# --------------------------------------------------------------------- 2D item sprites

def outline(img, color=None):
    """1px outline; by default a deep shade of the neighbouring colour (like vanilla items), not flat black."""
    px = img.load()
    w, h = img.size
    src = img.copy().load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 0:
                        if color is None:
                            n = src[nx, ny]
                            px[x, y] = (clamp8(n[0] * 0.28 + 6), clamp8(n[1] * 0.28 + 6), clamp8(n[2] * 0.3 + 9), 255)
                        else:
                            px[x, y] = color
                        break
    return img


def shaded_mask(mask, base, seed=0, light=1.3, dark=0.6, noise=0.08, height=None):
    """mask: 16x16 bools. Dome-lit from the top-left with a soft rim and fine grain."""
    m = np.array(mask, dtype=bool)
    f = K.sprite_light(m, height, strength=1.6 * (light - dark) / 0.7)
    f *= 1 + (K.fbm(seed) - 0.5) * 2 * noise
    rgb = K.tint(np.array(base, dtype=float) / 255.0, f)
    return K.to_img(rgb, m.astype(float))


def mask_from_draw(fn):
    big = Image.new("L", (16, 16), 0)
    d = ImageDraw.Draw(big)
    fn(d)
    px = big.load()
    return [[px[x, y] > 0 for x in range(16)] for y in range(16)]


def sparkle(img, pts, color=(255, 255, 255)):
    px = img.load()
    for (x, y) in pts:
        if px[x, y][3] > 0:
            px[x, y] = color + (255,)
    return img


def sprite_raw(base, seed):
    def f(d):
        d.polygon([(3, 6), (6, 3), (10, 3), (13, 6), (13, 10), (10, 13), (5, 13), (3, 10)], fill=255)
    lumps = (K.fbm(seed, octaves=(4, 8)) - 0.5) * 1.4
    img = shaded_mask(mask_from_draw(f), rgb(base), seed, noise=0.12, height=lumps)
    px = img.load()
    r = random.Random(seed)
    # Crystal inclusions: a bright glint with a darker socket.
    for _ in range(4):
        x, y = r.randint(5, 10), r.randint(5, 10)
        px[x + 1, y + 1] = shade(rgb(base), 0.45) + (255,)
        px[x, y] = mix(rgb(base), (255, 255, 255), 0.65) + (255,)
    return outline(img)


def sprite_ingot(base, seed):
    def f(d):
        d.polygon([(2, 10), (6, 6), (14, 6), (14, 9), (10, 13), (2, 13)], fill=255)
    img = shaded_mask(mask_from_draw(f), rgb(base), seed, noise=0.04)
    px = img.load()
    c = rgb(base)
    for x in range(7, 14):
        px[x, 7] = mix(c, (255, 255, 255), 0.45) + (255,)
    for x in range(3, 10):
        px[x, 12] = shade(c, 0.62) + (255,)
    for (x, y) in [(5, 9), (4, 10), (3, 11)]:
        px[x, y] = mix(c, (255, 255, 255), 0.3) + (255,)
    sparkle(img, [(12, 7)])
    return outline(img)


def sprite_plate(base, seed):
    def f(d):
        d.rectangle([2, 3, 13, 12], fill=255)
    img = shaded_mask(mask_from_draw(f), rgb(base), seed, noise=0.05)
    px = img.load()
    c = rgb(base)
    for (x, y) in [(3, 4), (12, 4), (3, 11), (12, 11)]:
        px[x, y] = mix(c, (255, 255, 255), 0.7) + (255,)
        px[x + (1 if x < 8 else -1), y + 1] = shade(c, 0.55) + (255,)
    for x in range(4, 12):
        px[x, 7] = shade(c, 0.72) + (255,)
        px[x, 8] = mix(c, (255, 255, 255), 0.25) + (255,)
    for k in range(4):
        px[4 + k, 4 + k // 2] = mix(c, (255, 255, 255), 0.35) + (255,)
    return outline(img)


def sprite_circuit():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 3, 14, 12], fill=(26, 96, 46, 255))
    for x in range(1, 15):
        img.putpixel((x, 3), (52, 150, 78, 255))
        img.putpixel((x, 12), (16, 62, 30, 255))
    gold = (226, 182, 70, 255)
    for y in (5, 8, 10):
        d.line([(2, y), (13, y)], fill=gold)
    d.line([(4, 5), (4, 10)], fill=gold)
    for (x, y) in [(2, 5), (13, 8), (2, 10), (13, 10)]:
        img.putpixel((x, y), (255, 236, 150, 255))
    d.rectangle([5, 5, 9, 9], fill=(22, 22, 26, 255))
    d.line([(5, 5), (9, 5)], fill=(60, 60, 70, 255))
    d.rectangle([11, 4, 13, 6], fill=(22, 22, 26, 255))
    img.putpixel((11, 4), (70, 70, 80, 255))
    img.putpixel((7, 7), (120, 255, 160, 255))
    img.putpixel((6, 7), (40, 140, 70, 255))
    img.putpixel((12, 5), (255, 90, 60, 255))
    return outline(img)


def sprite_orb(core, glow, ring=None):
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    m = d < 5.6
    nz = np.sqrt(np.clip(1 - (d / 5.6) ** 2, 0, 1))
    lam = np.clip((-(xx - 7.5) * 0.5 - (yy - 7.5) * 0.6) / 5.6 + nz * 0.7, 0, 1.4)
    cc, gc = K.rgbf(core), K.rgbf(glow)
    col = cc[None, None, :] * (0.35 + 0.75 * lam[..., None])
    rim = np.clip((d - 4.2) / 1.4, 0, 1) * m
    col = col * (1 - rim[..., None] * 0.6) + gc[None, None, :] * rim[..., None] * 0.9
    spec = np.exp(-(np.hypot(xx - 5.6, yy - 5.4) / 1.1) ** 2)
    col = col * (1 - spec[..., None]) + spec[..., None]
    halo = np.clip(1 - (d - 5.6) / 1.8, 0, 1) * (~m)
    out = np.where(m[..., None], col, gc[None, None, :])
    a = np.where(m, 1.0, halo * 0.75)
    img = K.to_img(out, a)
    if ring:
        dr = ImageDraw.Draw(img)
        dr.arc([1, 5, 14, 10], 0, 180, fill=rgb(ring) + (255,))
        dr.arc([1, 5, 14, 10], 180, 360, fill=shade(rgb(ring), 0.6) + (200,))
    return img


def sprite_canister(body, window, cap=0x9AA3AB):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def cyl(x, x0, x1):
        u = (x - x0 + 0.5) / (x1 - x0 + 1)
        return 0.55 + 0.75 * math.sin(u * math.pi) - 0.25 * u

    for y in range(16):
        for x in range(16):
            if 5 <= x <= 10 and 2 <= y <= 3 or 5 <= x <= 10 and 13 <= y <= 14:
                px[x, y] = shade(rgb(cap), cyl(x, 5, 10) * (1.1 if y in (2, 13) else 0.85)) + (255,)
            elif 4 <= x <= 11 and 4 <= y <= 12:
                px[x, y] = shade(rgb(body), cyl(x, 4, 11)) + (255,)
    # Window with a glowing fluid, a meniscus and bubbles.
    for y in range(5, 12):
        for x in range(6, 10):
            k = cyl(x, 6, 9)
            c = mix(rgb(window), (255, 255, 255), 0.25 if y == 5 else 0)
            px[x, y] = shade(c, 0.75 + 0.35 * k) + (255,)
    for (x, y) in [(7, 9), (8, 7), (7, 6)]:
        px[x, y] = mix(rgb(window), (255, 255, 255), 0.7) + (255,)
    px[6, 6] = (255, 255, 255, 255)
    for x in range(4, 12):
        px[x, 4] = shade(px[x, 4], 1.2) + (255,)
    return outline(img)


def bevel_sprite(img):
    px = img.load()
    src = img.copy().load()
    for y in range(16):
        for x in range(16):
            if src[x, y][3] == 0:
                continue
            if x == 0 or src[x - 1, y][3] == 0 or y == 0 or src[x, y - 1][3] == 0:
                px[x, y] = shade(src[x, y], 1.22) + (src[x, y][3],)
            elif x == 15 or src[x + 1, y][3] == 0 or y == 15 or src[x, y + 1][3] == 0:
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
    arr = np.array(img).astype(float) / 255
    m = arr[..., 3] > 0
    f = K.sprite_light(m, strength=1.2) * (1 + (K.fbm(seed_of(kind)) - 0.5) * 0.08)
    arr[..., :3] = np.clip(arr[..., :3] * f[..., None], 0, 1)
    img = Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA").copy()
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
    """Vanilla-flavoured stone: soft lit blotches, a few darker pits; deepslate gets horizontal strata."""
    n = K.fbm(seed, octaves=(2, 4, 8, 16))
    base = np.array([0.29, 0.29, 0.33]) if deep else np.array([0.5, 0.5, 0.5])
    f = (0.82 + 0.36 * n) * K.lit(n * 2.2, 1.2)
    if deep:
        yy = np.arange(16)[:, None]
        f = f * (1 - 0.1 * ((yy % 4) == 0)) * (1 + 0.04 * np.sin(np.arange(16)[None, :] * 0.9 + yy))
    rgb = K.tint(base, f)
    pits = K.tnoise(seed + 9, 16) > 0.86
    rgb[pits] *= 0.72
    return K.to_img(rgb)


def ore(seed, deep, color, glow=False):
    img = stone_base(seed, deep)
    arr = np.array(img).astype(float) / 255
    r = random.Random(seed + 100)
    c = K.rgbf(color)
    for _ in range(5):
        cx, cy = r.randint(2, 13), r.randint(2, 13)
        cells = [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)]
        for dx, dy in cells:
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16 and r.random() < 0.8:
                k = 1.35 if (dx, dy) == (0, 0) else (1.1 if dx + dy < 0 else 0.75)
                arr[y, x, :3] = np.minimum(1, c * k)
                if 0 <= y + 1 < 16 and (dx, dy) == (1, 1):
                    arr[y + 1, x, :3] *= 0.6
        arr[cy, cx, :3] = np.minimum(1, c * 0.5 + 0.55)
    if glow:
        arr[..., :3] = np.minimum(1, arr[..., :3] + c * 0.06)
    return K.to_img(arr[..., :3])


def charred_side(seed=36):
    yy, xx = np.mgrid[0:16, 0:16]
    bark = K.tnoise(seed, 4) * 0.4 + K.tnoise(seed + 1, 16) * 0.6
    ridges = 0.5 + 0.5 * np.sin(xx * 1.4 + K.tnoise(seed + 2, 4) * 4)
    h = ridges * 0.7 + bark * 0.5
    f = (0.55 + 0.6 * h) * K.lit(h * 2, 1.3)
    rgb = K.tint(np.array([0.13, 0.11, 0.1]), f)
    crack = (ridges < 0.06)
    ember = crack & (K.tnoise(seed + 3, 8) > 0.62)
    rgb[crack] = np.array([0.04, 0.03, 0.03])
    rgb[ember] = np.array([1.0, 0.42, 0.1]) * (0.7 + 0.3 * K.tnoise(seed + 4, 16)[ember])[:, None]
    return K.to_img(rgb)


def charred_top(seed=37):
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5) + (K.tnoise(seed, 4) - 0.5) * 1.2
    rings = 0.5 + 0.5 * np.cos(d * 2.4)
    rgb = K.tint(np.array([0.2, 0.17, 0.15]), 0.7 + 0.4 * rings)
    core = np.clip(1 - d / 3.2, 0, 1)
    rgb = rgb * (1 - core[..., None]) + np.array([1.0, 0.45, 0.12])[None, None, :] * core[..., None]
    crack = (np.abs(np.arctan2(yy - 7.5, xx - 7.5) - 0.7) < 0.12) & (d < 7)
    rgb[crack] = np.array([0.9, 0.3, 0.08])
    bark = d >= 7.0
    rgb[bark] = np.array([0.1, 0.08, 0.07]) * (0.8 + 0.4 * K.tnoise(seed + 2, 16)[bark])[:, None]
    return K.to_img(rgb)


def block_textures():
    b = {}
    b["uranium_ore"] = ore(31, False, 0x7CFF2E, True)
    b["deepslate_uranium_ore"] = ore(32, True, 0x7CFF2E, True)
    b["titanium_ore"] = ore(33, False, 0xC8D4E4)
    b["deepslate_titanium_ore"] = ore(34, True, 0xC8D4E4)
    u = Image.new("RGBA", (16, 16))
    px = u.load()
    g8 = mat_glow(0x5AFF3A, 1)
    for y in range(16):
        for x in range(16):
            edge = x % 8 in (0, 7) or y % 8 in (0, 7)
            g = g8.getpixel((x % 8 * 2, y % 8 * 2))
            px[x, y] = (shade(g, 0.45) + (255,)) if edge else g
    b["uranium_block"] = u
    b["titanium_block"] = mat_metal(0x9EAAB8, 35, rivets=True, panel=True, finish="anodised", wear=0.25)
    # Trinitite: green glass fused from sand, full of bubbles and glowing flecks.
    n = K.fbm(36)
    tr = K.tint(np.array([0.2, 0.5, 0.24]), (0.7 + 0.5 * n) * K.lit(n * 3, 1.5))
    rr = random.Random(36)
    for _ in range(7):
        x, y = rr.randint(0, 14), rr.randint(0, 14)
        tr[y, x] = np.array([0.08, 0.2, 0.1])
        tr[y + 1, x + 1] = np.array([0.6, 1.0, 0.55])
    fleck = K.tnoise(37, 16) > 0.85
    tr[fleck] = np.array([0.65, 1.0, 0.5])
    b["trinitite"] = K.to_img(tr)
    n = K.fbm(38)
    sc = K.tint(np.array([0.15, 0.13, 0.12]), (0.6 + 0.6 * n) * K.lit(n * 3, 1.4))
    hot = K.tnoise(39, 16) > 0.88
    sc[hot] = np.array([1.0, 0.38, 0.08])
    ash = K.tnoise(40, 8) > 0.75
    sc[ash] = sc[ash] * 0.5 + np.array([0.42, 0.4, 0.38]) * 0.5
    b["scorched_earth"] = K.to_img(sc)
    b["charred_log"] = charred_side()
    b["charred_log_top"] = charred_top()
    n = K.fbm(41)
    rc = K.tint(np.array([0.6, 0.6, 0.59]), (0.88 + 0.2 * n) * K.lit(n * 1.5, 1.0))
    for i in range(16):
        for j in (4, 12):
            rc[j, i] = np.array([0.42, 0.36, 0.32]) * (0.9 + 0.2 * ((i + j) % 3 == 0))
            rc[i, j] = np.array([0.42, 0.36, 0.32]) * (0.9 + 0.2 * ((i + j) % 3 == 0))
            if j + 1 < 16:
                rc[j + 1, i] *= 0.85
    stain = np.clip((np.arange(16)[:, None] / 15.0) - 0.5, 0, 1) * (K.tnoise(42, 8) > 0.45) * 0.25
    rc *= (1 - stain)[..., None]
    b["reinforced_concrete"] = K.to_img(rc)
    b["hazard_block"] = mat_hazard(seed=9)
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

    grain = K.fbm(seed, 64, octaves=(4, 8, 16, 32))
    cb = np.array(rgb(base), dtype=float) / 255

    def plate(bx, by, w, h):
        c = cb
        f = 0.9 + 0.2 * grain[(by * 7) % 64, (bx * 5) % 64]
        if by == 0 or bx == 0:
            f *= 1.18
        if by == h - 1 or bx == w - 1:
            f *= 0.7
        # Panel seam across the middle of larger faces.
        if h >= 8 and by == h // 2:
            f *= 0.78
        if h >= 8 and by == h // 2 + 1:
            f *= 1.08
        return tuple(clamp8(v * 255 * f) for v in c) + (255,)

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
    yy, xx = np.mgrid[0:8, 0:8]
    d = np.hypot(xx - 3.5, yy - 3.5) / 4.0
    a = np.exp(-(d * 2.1) ** 2) * 0.85 + np.clip(1 - d, 0, 1) ** 3 * 0.35
    v = np.clip(0.85 + 0.15 * (1 - d), 0, 1)
    return K.to_img(np.dstack([v, v, v]), np.clip(a, 0, 1))


def particle_spark(i):
    """Star glints: a hot core with long tapering rays; odd frames add diagonal rays."""
    size = 8
    yy, xx = np.mgrid[0:size, 0:size]
    dx, dy = xx - 3.5, yy - 3.5
    reach = 3.6 - 0.5 * (i % 2)
    ray = np.exp(-(dy / 0.75) ** 2) * np.clip(1 - np.abs(dx) / (reach + 0.6), 0, 1) ** 0.8
    ray = np.maximum(ray, np.exp(-(dx / 0.75) ** 2) * np.clip(1 - np.abs(dy) / (reach + 0.6), 0, 1) ** 0.8)
    if i >= 2:
        diag = np.exp(-((dx - dy) / 0.6) ** 2) * np.clip(1 - np.hypot(dx, dy) / 3.2, 0, 1) ** 1.5 * 0.6
        diag2 = np.exp(-((dx + dy) / 0.6) ** 2) * np.clip(1 - np.hypot(dx, dy) / 3.2, 0, 1) ** 1.5 * 0.6
        ray = np.maximum(ray, np.maximum(diag, diag2))
    core = np.exp(-(np.hypot(dx, dy) / 1.4) ** 2)
    a = np.clip(np.maximum(ray * 1.25, core * 1.2), 0, 1)
    return K.to_img(np.ones((size, size, 3)), a)


def particle_puff(seed, size=16, softness=1.0):
    """A lit, billowy smoke puff (white so the game can tint it): fbm silhouette, top-left lit, soft rim."""
    r = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.hypot(xx - c, yy - c) / (size / 2)
    n = K.fbm(seed, size, octaves=(2, 4, 8))
    lobes = np.zeros((size, size))
    for _ in range(4):
        lx, ly = c + r.normal(0, size * 0.12), c + r.normal(0, size * 0.12)
        lr = size * (0.22 + r.random() * 0.12)
        lobes = np.maximum(lobes, np.clip(1 - np.hypot(xx - lx, yy - ly) / lr, 0, 1))
    body = np.clip(np.maximum(lobes, 1 - d * 1.15) - (n - 0.5) * 0.45, 0, 1) ** softness
    lightdir = np.clip(1 - ((xx - c) * 0.55 + (yy - c) * 0.75) / size, 0.3, 1.3)
    v = np.clip(0.62 + 0.22 * (lightdir - 0.8) + 0.18 * n + 0.12 * (1 - d), 0.45, 1)
    alpha = np.clip(body * 1.5 * (0.65 + 0.5 * n), 0, 1)
    # Fade fully out before the tile edge, or the puff shows a clipped square outline in game.
    alpha = alpha * _edge_fade(size)
    return K.to_img(np.dstack([v, v, v]), alpha)


def _edge_fade(size):
    """Round mask: 1 in the middle, smoothly 0 a pixel inside the tile border."""
    yy, xx = np.mgrid[0:size, 0:size]
    c = (size - 1) / 2
    d = np.hypot(xx - c, yy - c) / (size / 2 - 1)
    t = np.clip((1.0 - d) / 0.35, 0, 1)
    return t * t * (3 - 2 * t)


def particle_flame(i):
    """Flame tongue (white, tinted in game): turbulent outline, bright inner core, wispy tip."""
    size = 16
    yy, xx = np.mgrid[0:size, 0:size]
    t = yy / 15.0  # 0 tip .. 1 base
    turb = (K.fbm(400 + i, size, octaves=(2, 4, 8)) - 0.5)
    cx = 7.5 + np.sin(yy * 0.55 + i * 1.7) * 1.4 * (1 - t) + turb * 2.0
    width = (t ** 0.75) * 6.2 * (0.9 + 0.25 * np.sin(yy * 0.9 + i))
    d = np.abs(xx - cx) / np.maximum(width, 0.3)
    shape = np.clip(1 - d, 0, 1) * np.clip(t * 1.6, 0, 1)
    shape = np.where(t > 0.92, shape * (1 - (t - 0.92) * 6), shape)
    core = np.clip(1 - d * 1.8, 0, 1) * np.clip(t * 1.3 - 0.2, 0, 1)
    a = np.clip(shape ** 0.8 * 1.25, 0, 1)
    a = a * np.clip((15 - yy) / 2.5, 0, 1) * np.clip(np.minimum(xx, 15 - xx) / 1.5, 0, 1)
    v = np.clip(0.72 + 0.28 * core, 0, 1)
    return K.to_img(np.dstack([v, v, v]), a)


def mob_effect_icon():
    big = Image.new("RGBA", (72, 72), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    for k in range(6, 0, -1):
        d.ellipse([2 - k, 2 - k, 69 + k, 69 + k], outline=(140, 255, 60, 30 + k * 6), width=1)
    d.ellipse([2, 2, 69, 69], fill=(242, 194, 0, 255), outline=(30, 30, 30, 255), width=4)
    d.ellipse([10, 8, 50, 30], fill=(255, 236, 120, 120))
    trefoil(d, 36, 36, 26, (25, 25, 25, 255), (242, 194, 0, 255))
    return big.resize((18, 18), Image.LANCZOS)


def mod_icon():
    """128px: a lit mushroom cloud over a burning horizon with a radiation badge."""
    size = 128
    yy, xx = np.mgrid[0:size, 0:size]
    t = yy / size
    sky = np.array([0.08, 0.03, 0.12]) * (1 - t[..., None]) + np.array([1.0, 0.45, 0.12]) * t[..., None] ** 2.2
    n = K.fbm(1945, size, octaves=(4, 8, 16, 32, 64))
    n2 = K.fbm(1946, size, octaves=(8, 16, 32))
    stem = np.clip(1 - np.abs(xx - 64 + np.sin(yy * 0.08) * 3) / (8 + (yy - 60) * 0.08), 0, 1) * (yy > 50) * (yy < 116)
    cap = np.clip(1 - np.hypot((xx - 64) / 46.0, (yy - 42) / 27.0), 0, 1)
    ring = np.clip(1 - np.abs(np.hypot((xx - 64) / 32.0, (yy - 76) / 7.0) - 1) * 4, 0, 1) * 0.9
    field = np.maximum(np.maximum(stem * 1.4, cap * 2.4), ring) + (n - 0.5) * 0.9
    dens = np.clip((field - 0.25) * 4.0, 0, 1)
    # Lighting: billows lit from the fireball below and from the sky above.
    shade_ = 0.5 + 0.9 * (n2 - 0.5) + 0.35 * (1 - t)
    heat = np.clip(1 - np.hypot((xx - 64) / 34.0, (yy - 54) / 14.0), 0, 1) ** 0.8
    soot = np.array([0.22, 0.13, 0.12])[None, None, :] * (0.7 + 0.8 * shade_[..., None])
    fire = np.array([1.0, 0.62, 0.22])[None, None, :] * (1 - heat[..., None] * 0.4) + np.array([1.0, 0.95, 0.75])[None, None, :] * heat[..., None] * 0.4
    under = np.clip((yy - 30) / 30.0, 0, 1) * cap * 1.6
    k = np.clip(heat * 1.4 + under * 0.5 + stem * 0.35, 0, 1)[..., None]
    cloud = soot * (1 - k) + fire * k
    rim = np.clip(dens - np.roll(dens, 2, 0), 0, 1)
    cloud = K.screen_blend(cloud, np.array([1.0, 0.7, 0.4])[None, None, :] * rim[..., None] * 0.5)
    a = dens[..., None]
    rgb = sky * (1 - a) + cloud * a
    glow = np.clip(1 - np.hypot(xx - 64, (yy - 116) * 1.6) / 80.0, 0, 1) ** 2
    rgb = K.screen_blend(rgb, np.array([1.0, 0.55, 0.18])[None, None, :] * glow[..., None] * 0.7)
    ground = yy > 114
    rgb[ground] = np.array([0.06, 0.03, 0.03]) + np.array([0.9, 0.3, 0.06]) * (np.clip(1 - np.abs(xx[ground] - 64) / 64.0, 0, 1) ** 2)[:, None] * 0.35
    img = K.to_img(rgb)
    d = ImageDraw.Draw(img)
    d.ellipse([78, 78, 122, 122], fill=(20, 20, 20, 255))
    trefoil(d, 100, 100, 19, (20, 20, 20, 255), (242, 194, 0, 255))
    d.ellipse([78, 78, 122, 122], outline=(255, 230, 120, 255), width=2)
    return img
