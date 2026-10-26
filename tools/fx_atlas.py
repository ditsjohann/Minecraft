"""Procedural VFX sprite atlas (textures/fx/atlas.png): 4x4 cells of 128 px, white luminance + alpha, tinted at draw time.

Cells: 0-5 smoke puffs, 6-7 dust, 8-9 fire, 10 soft glow, 11 flare, 12 ring, 13 streak, 14 shock ring, 15 wisp.
"""
import numpy as np
from PIL import Image

N = 128


def _noise(seed, res):
    rng = np.random.default_rng(seed)
    g = rng.random((res + 1, res + 1))
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    x = np.linspace(0, res, N, endpoint=False)
    i = x.astype(int)
    f = x - i
    f = f * f * (3 - 2 * f)
    a = g[np.ix_(i, i)]
    b = g[np.ix_(i, i + 1)]
    c = g[np.ix_(i + 1, i)]
    d = g[np.ix_(i + 1, i + 1)]
    fx, fy = f[None, :], f[:, None]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(seed, base=4, octaves=5):
    out = np.zeros((N, N))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += _noise(seed + o * 17, base * 2 ** o) * amp
        tot += amp
        amp *= 0.5
    return out / tot


yy, xx = np.mgrid[0:N, 0:N]
U = (xx + 0.5) / N * 2 - 1
V = (yy + 0.5) / N * 2 - 1
R = np.sqrt(U * U + V * V)
A = np.arctan2(V, U)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def puff(seed, flat=1.0, grain=0.0):
    """A billowing cumulus puff: a cluster of soft round billows, self-shadowed underneath, eroded edges."""
    rng = np.random.default_rng(seed)
    field = np.zeros((N, N))
    light = np.zeros((N, N))
    for k in range(9):
        a = rng.random() * np.pi * 2
        d = 0.0 if k == 0 else 0.22 + rng.random() * 0.3
        cx, cy = np.cos(a) * d, np.sin(a) * d * flat
        r = (0.58 if k == 0 else 0.3 + rng.random() * 0.16)
        dist = np.sqrt((U - cx) ** 2 + ((V - cy) / flat) ** 2) / r
        blob = np.clip(1 - dist * dist, 0, 1) ** 0.8
        field = np.maximum(field, blob)
        # Each billow lit from above-left: brighter on its own upper side.
        lit = np.clip(0.55 + 0.6 * (-(V - cy) * 0.8 - (U - cx) * 0.3) / r, 0, 1) * blob
        light = np.maximum(light, lit)
    n = fbm(seed + 5, 5)
    edge = field + (n - 0.5) * 0.45
    alpha = smooth(0.02, 0.75, edge) * (0.82 + 0.18 * fbm(seed + 7, 8))
    detail = fbm(seed + 9, 10)
    shade = 0.5 + 0.5 * light + (detail - 0.5) * 0.3
    if grain:
        shade += (fbm(seed + 13, 16) - 0.5) * grain
    return np.clip(shade, 0, 1), np.clip(alpha, 0, 1) * smooth(1.0, 0.82, R)


def fire(seed):
    n = fbm(seed, 3)
    rr = R + (n - 0.5) * 0.45 + np.clip(-V, 0, 1) * 0.12
    alpha = smooth(0.85, 0.3, rr)
    lum = np.clip(smooth(0.7, 0.0, rr) * 0.65 + 0.4 + (fbm(seed + 3, 6) - 0.5) * 0.25, 0, 1)
    return lum, alpha


def glow():
    a = np.exp(-(R / 0.42) ** 2) * smooth(1.0, 0.85, R)
    return np.ones_like(a), a


def flare():
    rays = np.zeros_like(R)
    for k, w in ((0, 0.035), (np.pi / 2, 0.035), (np.pi / 4, 0.02), (-np.pi / 4, 0.02)):
        d = np.abs(U * np.sin(k) - V * np.cos(k))
        rays = np.maximum(rays, np.exp(-(d / w) ** 2) * np.clip(1 - R, 0, 1) ** 1.5)
    core = np.exp(-(R / 0.18) ** 2)
    halo = np.exp(-(R / 0.5) ** 2) * 0.35
    a = np.clip(core + halo + rays * 0.9, 0, 1) * smooth(1.0, 0.9, R)
    return np.ones_like(a), a


def ring():
    a = np.exp(-((R - 0.72) / 0.12) ** 2) * smooth(1.0, 0.92, R)
    return np.ones_like(a), a


def streak():
    a = np.exp(-(U / 0.7) ** 2 - (V / 0.08) ** 2)
    return np.ones_like(a), a


def shock(seed):
    n = fbm(seed, 6)
    a = np.exp(-((R - 0.8 + (n - 0.5) * 0.08) / 0.06) ** 2) * (0.6 + 0.4 * n) + np.exp(-((R - 0.72) / 0.18) ** 2) * 0.25
    return np.ones_like(a), np.clip(a * smooth(1.0, 0.95, R), 0, 1)


def wisp(seed):
    n = fbm(seed, 3)
    band = np.exp(-((V + (n - 0.5) * 0.6) / 0.35) ** 2) * smooth(1.0, 0.6, np.abs(U))
    a = band * (0.4 + 0.6 * fbm(seed + 4, 8))
    lum = 0.7 + 0.3 * smooth(0.5, -0.5, V)
    return np.full_like(a, 1.0) * lum, np.clip(a, 0, 1)


def build():
    cells = [puff(11), puff(23), puff(37), puff(41), puff(53, 0.85), puff(67, 0.9),
             puff(71, 0.55, 0.25), puff(83, 0.6, 0.25), fire(91), fire(97),
             glow(), flare(), ring(), streak(), shock(101), wisp(107)]
    img = np.zeros((N * 4, N * 4, 4))
    for i, (lum, alpha) in enumerate(cells):
        cx, cy = (i % 4) * N, (i // 4) * N
        img[cy:cy + N, cx:cx + N, 0] = lum
        img[cy:cy + N, cx:cx + N, 1] = lum
        img[cy:cy + N, cx:cx + N, 2] = lum
        img[cy:cy + N, cx:cx + N, 3] = alpha
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA")


def write(path):
    build().save(path)
