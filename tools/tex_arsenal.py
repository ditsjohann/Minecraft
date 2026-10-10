"""Textures for the arsenal expansion. Animated strips get a .mcmeta written by generate_assets."""
import math
import random

from PIL import Image

import gen_textures as T

ANIMATED = {}  # name -> frametime


def strip(frames):
    img = Image.new("RGBA", (16, 16 * len(frames)))
    for i, f in enumerate(frames):
        img.paste(f, (0, 16 * i))
    return img


def hsv(h, s, v):
    i = int(h * 6) % 6
    f = h * 6 - int(h * 6)
    p, q, t = v * (1 - s), v * (1 - f * s), v * (1 - (1 - f) * s)
    r, g, b = [(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)][i]
    return int(r * 255), int(g * 255), int(b * 255)


def celestial(n=12):
    rnd = random.Random(4242)
    stars = [(rnd.randint(0, 15), rnd.randint(0, 15), rnd.random()) for _ in range(14)]
    frames = []
    for k in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                h = ((x + y) / 32.0 + k / n) % 1.0
                wave = 0.5 + 0.5 * math.sin((x - y) * 0.5 + k * math.tau / n)
                r, g, b = hsv(h, 0.35 + 0.25 * wave, 0.75 + 0.25 * wave)
                px[x, y] = (r, g, b, 255)
        for (sx, sy, ph) in stars:
            tw = 0.5 + 0.5 * math.sin(ph * math.tau + k * math.tau / n * 2)
            if tw > 0.6:
                px[sx, sy] = (255, 255, 255, 255)
        frames.append(img)
    return strip(frames)


def rainbow_glow(n=16):
    frames = []
    for k in range(n):
        frames.append(T.mat_glow(int("%02x%02x%02x" % hsv(k / n, 0.85, 1.0), 16), "rb%d" % k))
    return strip(frames)


def molten(base, hot, dark, seed, n=8, crust=0.45):
    rnd = random.Random(seed)
    cells = [(rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(0, math.tau)) for _ in range(10)]
    frames = []
    for k in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        t = k / n * math.tau
        for y in range(16):
            for x in range(16):
                v = 0
                for (cx, cy, ph) in cells:
                    dx = min(abs(x - cx), 16 - abs(x - cx))
                    dy = min(abs(y - cy - 2 * math.sin(t + ph)), 16 - abs(y - cy - 2 * math.sin(t + ph)))
                    v += math.exp(-(dx * dx + dy * dy) / 9.0)
                v = min(1.0, v * 0.8)
                if v < crust:
                    c = T.shade(T.rgb(dark), 0.8 + 0.4 * v)
                else:
                    c = T.mix(T.rgb(base), T.rgb(hot), (v - crust) / (1 - crust))
                px[x, y] = tuple(c) + (255,)
        frames.append(img)
    return strip(frames)


def void_tile(n=8):
    rnd = random.Random(777)
    pts = [(rnd.randint(0, 15), rnd.randint(0, 15), rnd.random()) for _ in range(12)]
    frames = []
    for k in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                v = 10 + int(8 * math.sin(d * 0.9 - k * math.tau / n))
                px[x, y] = (v + 6, max(0, v - 4), v + 22, 255)
        for (sx, sy, ph) in pts:
            if math.sin(ph * math.tau + k * math.tau / n) > 0.3:
                px[sx, sy] = (190, 120, 255, 255)
        frames.append(img)
    return strip(frames)


def crystal_tile(c, alpha, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    base = T.rgb(c)
    for y in range(16):
        for x in range(16):
            facet = ((x * 3 + y * 5) // 7 + (x - y) // 5) % 3
            f = (0.75, 1.0, 1.3)[facet]
            edge = x in (0, 15) or y in (0, 15)
            col = T.shade(base, f * (1.25 if edge else 1.0))
            px[x, y] = tuple(col) + (235 if edge else alpha,)
    for _ in range(6):
        x, y = rnd.randint(1, 14), rnd.randint(1, 14)
        px[x, y] = (255, 255, 255, 255)
    return img


def tiles():
    t = {}
    t["pink"] = T.mat_metal(0xE85AA8, 101, panel=True)
    t["gold"] = T.mat_metal(0xE0B030, 102, streaks=True, rivets=True)
    t["pearl"] = T.mat_metal(0xEDEAF6, 103, panel=True)
    t["obsidian"] = T.mat_metal(0x1E1428, 104, rivets=True)
    t["crimson"] = T.mat_metal(0x7A1420, 105, rivets=True)
    t["amethyst"] = crystal_tile(0x9A5CE0, 240, 106)
    for name, c in (("gold", 0xFFC830), ("crimson", 0xFF2030), ("void", 0x7A30FF), ("pink", 0xFF70C0),
                    ("lime", 0x9AFF3A), ("ice", 0xBFEFFF)):
        t["glow_" + name] = T.mat_glow(c, name + "x")
    t["glass_ice"] = T.mat_glass(0xCFF4FF, 110)
    t["glass_slime"] = T.mat_glass(0x70E060, 150)
    t["glass_gold"] = T.mat_glass(0xFFD060, 140)
    t["celestial"] = celestial()
    ANIMATED["celestial"] = 2
    t["glow_rainbow"] = rainbow_glow()
    ANIMATED["glow_rainbow"] = 2
    return t


BLOCK_ANIMATED = {}


def blocks():
    b = {}
    b["energy_crystal"] = crystal_tile(0xB070FF, 170, 201)
    b["luminous_glass"] = crystal_tile(0xF4F6FF, 120, 202)
    b["gold_fissure"] = molten(0xFFB020, 0xFFF4A0, 0x3A2A10, 203)
    BLOCK_ANIMATED["gold_fissure"] = 3
    b["crimson_fissure"] = molten(0xC01020, 0xFF7050, 0x2A0A0E, 204, crust=0.55)
    BLOCK_ANIMATED["crimson_fissure"] = 3
    b["molten_core"] = molten(0xFF6010, 0xFFE070, 0x401808, 205, crust=0.3)
    BLOCK_ANIMATED["molten_core"] = 2
    b["void_remnant"] = void_tile()
    BLOCK_ANIMATED["void_remnant"] = 3
    return b
