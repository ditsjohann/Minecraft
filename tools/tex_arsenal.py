"""Textures for the arsenal expansion. Animated strips get a .mcmeta written by generate_assets."""
import math
import random

import numpy as np
from PIL import Image

import gen_textures as T
import texkit as K

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
    """Deep space: a drifting violet/magenta/cyan nebula with twinkling stars."""
    rnd = random.Random(4242)
    stars = [(rnd.randint(0, 15), rnd.randint(0, 15), rnd.random(), rnd.random()) for _ in range(16)]
    n1, n2 = K.fbm(4242), K.fbm(4343)
    stops = [(0.0, (0.03, 0.02, 0.10)), (0.35, (0.22, 0.08, 0.42)), (0.6, (0.75, 0.22, 0.75)),
             (0.8, (0.35, 0.75, 1.0)), (1.0, (1.0, 0.95, 1.0))]

    def ramp(v):
        out = np.zeros(v.shape + (3,))
        for (a, ca), (b, cb) in zip(stops, stops[1:]):
            m = (v >= a) & (v <= b)
            u = ((v - a) / (b - a))[m][:, None]
            out[m] = np.array(ca) * (1 - u) + np.array(cb) * u
        return out

    frames = []
    for k in range(n):
        t = k / n
        sh = k * 16 // n
        dens = np.clip(np.roll(n2, sh, axis=1) * 0.65 + np.roll(n1, -sh, axis=0) * 0.55 - 0.15, 0, 1)
        rgb = ramp(dens)
        for (sx, sy, ph, big) in stars:
            tw = 0.5 + 0.5 * math.sin(ph * math.tau + t * math.tau * 2)
            if tw > 0.3:
                rgb[sy, sx] = K.screen_blend(rgb[sy, sx], np.full(3, tw))
                if big > 0.7 and tw > 0.75:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        q = ((sy + dy) % 16, (sx + dx) % 16)
                        rgb[q] = K.screen_blend(rgb[q], np.array([0.45, 0.5, 0.65]))
        frames.append(K.to_img(rgb))
    return strip(frames)


def rainbow_glow(n=16):
    """Tileable rainbow: hue runs once across the tile on a whole-period diagonal (so edges meet), drifting in time."""
    frames = []
    yy, xx = np.mgrid[0:16, 0:16]
    for k in range(n):
        rgb = np.zeros((16, 16, 3))
        for y in range(16):
            for x in range(16):
                rgb[y, x] = np.array(hsv(((x + y) / 16.0 + k / n) % 1.0, 0.78, 1.0)) / 255.0
        sheen = (K.tnoise(707, 4) - 0.5) * 0.2
        rgb = np.minimum(1, rgb * (1 + sheen)[..., None] + 0.08)
        frames.append(K.to_img(rgb))
    return strip(frames)


def glow_anim(c, name, n=12):
    return strip([T.mat_glow(c, name, k / n) for k in range(n)])


def screen_anim(c, seed, n=16):
    return strip([T.mat_screen(c, k / n, seed) for k in range(n)])


def molten(base, hot, dark, seed, n=8, crust=0.45):
    """Flowing lava under a lit, cracked crust."""
    rnd = random.Random(seed)
    cells = [(rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(0, math.tau)) for _ in range(10)]
    grit = K.fbm(seed)
    frames = []
    for k in range(n):
        t = k / n * math.tau
        v = np.zeros((16, 16))
        for y in range(16):
            for x in range(16):
                acc = 0
                for (cx, cy, ph) in cells:
                    dx = min(abs(x - cx), 16 - abs(x - cx))
                    dy = min(abs(y - cy - 2 * math.sin(t + ph)), 16 - abs(y - cy - 2 * math.sin(t + ph)))
                    acc += math.exp(-(dx * dx + dy * dy) / 9.0)
                v[y, x] = min(1.0, acc * 0.8)
        crustm = v < crust
        light = K.lit((1 - v) * 2.5 + grit, 1.2)
        rock = K.tint(K.rgbf(dark), (0.75 + 0.5 * grit) * light)
        u = np.clip((v - crust) / (1 - crust), 0, 1)
        lava = K.rgbf(base)[None, None, :] * (1 - u[..., None]) + K.rgbf(hot)[None, None, :] * u[..., None]
        rim = np.clip(1 - np.abs(v - crust) * 12, 0, 1)
        rgb = np.where(crustm[..., None], rock, lava)
        rgb = K.screen_blend(rgb, K.rgbf(base)[None, None, :] * rim[..., None] * 0.8)
        frames.append(K.to_img(rgb))
    return strip(frames)


def void_tile(n=8):
    rnd = random.Random(777)
    pts = [(rnd.randint(0, 15), rnd.randint(0, 15), rnd.random()) for _ in range(12)]
    yy, xx = np.mgrid[0:16, 0:16]
    swirl = K.fbm(778)
    frames = []
    for k in range(n):
        t = k / n
        d = np.hypot(xx - 7.5, yy - 7.5)
        ang = np.arctan2(yy - 7.5, xx - 7.5)
        arms = 0.5 + 0.5 * np.sin(ang * 2 + d * 0.9 - t * math.tau + swirl * 3)
        v = 0.03 + 0.1 * arms * np.clip(1 - d / 11, 0, 1)
        rgb = np.dstack([v * 1.3, v * 0.5, v * 2.6])
        rim = np.exp(-((d - (t * 11) % 11) / 1.0) ** 2) * 0.12
        rgb += np.array([0.5, 0.25, 1.0]) * rim[..., None]
        for (sx, sy, ph) in pts:
            b = math.sin(ph * math.tau + t * math.tau)
            if b > 0.2:
                rgb[sy, sx] = np.array([0.75, 0.5, 1.0]) * (0.5 + 0.5 * b)
        frames.append(K.to_img(rgb))
    return strip(frames)


def crystal_tile(c, alpha, seed):
    return K.crystal(c, seed, alpha)


def earth_tile(rgb, seed, grain=0.5):
    """Packed earth / bone: soft fbm lighting with pitting, no metal sheen."""
    n = K.fbm(seed)
    f = K.lit(n * 3.0, 1.4)
    pits = np.clip(1 - np.abs(K.tnoise(seed + 1, 6) - 0.5) * 9, 0, 1) * grain
    col = np.array([(rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0])
    return K.to_img(K.tint(col, (0.8 + 0.4 * n) * f * (1 - 0.35 * pits)))


def tiles():
    t = {}
    t["pink"] = T.mat_metal(0xE85AA8, 101, panel=True, finish="anodised", wear=0.25)
    t["gold"] = T.mat_metal(0xE0B030, 102, rivets=True, finish="polished", wear=0.3)
    t["pearl"] = T.mat_metal(0xEDEAF6, 103, panel=True, finish="polished", wear=0.15)
    t["obsidian"] = T.mat_metal(0x1E1428, 104, rivets=True, finish="polished", wear=0.2)
    t["crimson"] = T.mat_metal(0x7A1420, 105, rivets=True, finish="anodised", wear=0.35)
    t["amethyst"] = crystal_tile(0x9A5CE0, 240, 106)
    t["ice"] = crystal_tile(0xA8D8FF, 235, 107)
    t["mud"] = earth_tile(0x6A4A30, 108)
    t["bone"] = earth_tile(0xE6DCC0, 109, 0.3)
    # Every emissive material breathes: animated strips override the static base tiles.
    for name, c in T.GLOWS + [("gold", 0xFFC830), ("crimson", 0xFF2030), ("void", 0x7A30FF), ("pink", 0xFF70C0),
                              ("lime", 0x9AFF3A), ("ice", 0xBFEFFF)]:
        t["glow_" + name] = glow_anim(c, name)
        ANIMATED["glow_" + name] = 3
    t["screen"] = screen_anim(0x2AFF6A, 21)
    ANIMATED["screen"] = 3
    t["screen_red"] = screen_anim(0xFF3A2A, 22)
    ANIMATED["screen_red"] = 3
    t["glass_ice"] = T.mat_glass(0xCFF4FF, 110, 111)
    t["glass_slime"] = T.mat_glass(0x70E060, 150, 112)
    t["glass_gold"] = T.mat_glass(0xFFD060, 140, 113)
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
    b["uranium_block"] = uranium_block()
    BLOCK_ANIMATED["uranium_block"] = 4
    return b


def uranium_block(n=8):
    """Four glowing fuel cells in a dark frame, pulsing out of step with each other."""
    frames = []
    for k in range(n):
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for cell, ph in (((0, 0), 0.0), ((8, 0), 0.25), ((0, 8), 0.5), ((8, 8), 0.75)):
            g = T.mat_glow(0x5AFF3A, 1, (k / n + ph) % 1.0)
            for y in range(8):
                for x in range(8):
                    edge = x in (0, 7) or y in (0, 7)
                    c = g.getpixel((x * 2, y * 2))
                    if edge:
                        v = 0.2 + (0.12 if (x == 0 or y == 0) else 0)
                        c = (int(40 * v / 0.32), int(52 * v / 0.32), int(44 * v / 0.32), 255)
                    px[cell[0] + x, cell[1] + y] = c
        frames.append(img)
    return strip(frames)
