"""Numpy toolkit for the procedural textures: tileable noise, lighting from height fields, and finishes.

Every material tile is "world mapped" by the model generator (a face shows the window of the tile at its
own position), so detail is spread over the whole tile instead of relying on a border bevel.
"""
import math

import numpy as np
from PIL import Image


def rgbf(h):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], dtype=float) / 255.0


def tnoise(seed, cells, size=16):
    """Tileable value noise: a random lattice of `cells`^2, smoothly interpolated with wrap-around."""
    r = np.random.default_rng(seed)
    g = r.random((cells, cells))
    t = (np.arange(size) + 0.5) * cells / size
    i0 = np.floor(t).astype(int) % cells
    i1 = (i0 + 1) % cells
    f = t - np.floor(t)
    f = f * f * (3 - 2 * f)
    a = g[np.ix_(i0, i0)] * (1 - f)[None, :] + g[np.ix_(i0, i1)] * f[None, :]
    b = g[np.ix_(i1, i0)] * (1 - f)[None, :] + g[np.ix_(i1, i1)] * f[None, :]
    return a * (1 - f)[:, None] + b * f[:, None]


def fbm(seed, size=16, octaves=(2, 4, 8, 16), gain=0.55):
    out = np.zeros((size, size))
    amp, tot = 1.0, 0.0
    for k, c in enumerate(octaves):
        if c > size:
            break
        out += tnoise(seed * 31 + k, c, size) * amp
        tot += amp
        amp *= gain
    return out / tot


def lit(height, strength=1.0, light=(-0.6, -0.75, 0.55)):
    """Lambert-ish shading factor from a tileable height field, lit from the top-left like vanilla."""
    gx = (np.roll(height, -1, 1) - np.roll(height, 1, 1)) * 0.5
    gy = (np.roll(height, -1, 0) - np.roll(height, 1, 0)) * 0.5
    n = np.dstack([-gx * strength, -gy * strength, np.ones_like(height)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    l = np.array(light, dtype=float)
    l /= np.linalg.norm(l)
    d = (n * (-l[0], -l[1], l[2])).sum(axis=2)
    base = l[2]
    return 1.0 + (d - base) * 1.6


def to_img(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 1)
    a = np.ones(rgb.shape[:2]) if alpha is None else np.clip(alpha, 0, 1)
    arr = np.dstack([rgb, a[..., None] if a.ndim == 2 else a])
    return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA").copy()


def tint(base, factor):
    return base[None, None, :] * factor[..., None]


def screen_blend(a, b):
    return 1 - (1 - a) * (1 - b)


# --------------------------------------------------------------------------- finishes

def metal(base, seed, finish="brushed", panel=False, rivets=False, wear=0.5, scratches=4, size=16):
    """Metal plate. finish: brushed | cast | painted | anodised | polished."""
    c = rgbf(base)
    rng = np.random.default_rng(seed)
    grain = fbm(seed, size) - 0.5
    f = 1.0 + grain * 0.16
    if finish in ("brushed", "polished", "anodised"):
        rows = rng.random(size) - 0.5
        streak = np.repeat(rows[:, None], size, 1) * (0.14 if finish == "brushed" else 0.06)
        streak += (tnoise(seed + 5, 8, size) - 0.5) * 0.06
        f += streak
    if finish == "cast":
        f += (tnoise(seed + 9, 16, size) - 0.5) * 0.18
    # Broad, non-directional mottling instead of a diagonal sheen: tiles sit side by side on big faces, and
    # any directional band in one tile repeats into stripes across the whole face.
    yy, xx = np.mgrid[0:size, 0:size]
    diag = (xx + yy) / (2.0 * (size - 1))
    mottle = tnoise(seed + 3, 2, size) - 0.5
    f += mottle * (0.16 if finish in ("polished", "anodised") else 0.1)
    height = grain * 0.6
    if panel:
        for k in (0, size // 2):
            f[k, :] *= 0.68
            f[(k + 1) % size, :] *= 1.14
            f[:, k] *= 0.74
            f[:, (k + 1) % size] *= 1.1
    if rivets:
        for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13)][: (6 if panel else 4)]:
            f[y, x] *= 1.55
            f[y, x + 1] *= 1.12
            f[y + 1, x] *= 0.9
            f[y + 1, x + 1] *= 0.55
    rgb = tint(c, f)
    if finish == "anodised":
        hue = np.dstack([np.full((size, size), 1.0), 1.0 + diag * 0.08, 1.0 + (1 - diag) * 0.08])
        rgb *= hue
    # A couple of faint, short, horizontal scratches (never diagonal, which tiles into stripes).
    for _ in range(min(scratches, 2)):
        x0, y0 = rng.integers(0, size), rng.integers(0, size)
        for k in range(rng.integers(2, 4)):
            x, y = (x0 + k) % size, y0
            rgb[y, x] = rgb[y, x] * 0.82 + 0.18 * np.minimum(1, c * 1.5 + 0.12)
    # Wear: chipped paint or burnished spots where a noise threshold crosses.
    if wear > 0:
        w = tnoise(seed + 77, 8, size) * 0.6 + tnoise(seed + 78, 16, size) * 0.4
        chip = w > (0.84 - wear * 0.08)
        under = np.array([0.36, 0.37, 0.4]) if finish == "painted" else np.minimum(1, c * 1.18 + 0.05)
        rgb[chip] = rgb[chip] * 0.3 + under * 0.7
        # A bright lip on the lower-right of each chip sells the depth.
        lip = (~chip) & np.roll(chip, 1, 0)
        rgb[lip] = np.minimum(1, rgb[lip] * 1.15)
        grime = (tnoise(seed + 79, 4, size) - 0.5) * 0.12 * wear
        rgb *= (1 - grime)[..., None]
    return to_img(rgb)


def glow_frame(base, seed, phase, size=16, core=0.62):
    """Emissive tile: hot white core, saturated body, flowing energy veins. `phase` 0..1 animates it."""
    c = rgbf(base)
    yy, xx = np.mgrid[0:size, 0:size]
    d = np.hypot(xx - (size - 1) / 2, yy - (size - 1) / 2) / (size / 2)
    flow = np.sin((xx * 0.55 + yy * 0.35) * 0.9 - phase * math.tau) * 0.5 + 0.5
    n = tnoise(seed, 4, size) * 0.7 + tnoise(seed + 1, 8, size) * 0.3
    veins = np.abs(np.sin((n * 3.0 + phase) * math.pi))
    veins = np.clip(1 - veins * 5.0, 0, 1)
    pulse = 0.95 + 0.05 * math.sin(phase * math.tau)
    # A soft, low core only: a bright blob per tile reads as a grid of dots on big faces.
    hot = np.clip(1.0 - d * 1.05, 0, 1) ** 2.0 * core * 0.35
    body = 0.9 + 0.1 * np.clip(1 - d, 0, 1) ** 1.5 + 0.1 * flow + (n - 0.5) * 0.12
    rgb = tint(c, body * pulse)
    # Saturate the body, then let the core blow out toward white-hot.
    rgb = np.minimum(1, rgb * 1.1)
    rgb = rgb * (1 - hot[..., None]) + hot[..., None] * np.minimum(1, c * 0.25 + 0.85)[None, None, :]
    rgb = screen_blend(rgb, np.minimum(1, c * 0.5 + 0.55)[None, None, :] * (veins * 0.4 * (0.6 + 0.4 * flow))[..., None])
    return to_img(rgb)


def glass(base, seed, alpha=150, size=16):
    """Tinted glass: a soft body with gentle internal variation, a faint rim and two small glints, no streaks."""
    c = rgbf(base)
    yy, xx = np.mgrid[0:size, 0:size]
    body = 0.86 + (fbm(seed, size) - 0.5) * 0.14 + (tnoise(seed + 2, 2, size) - 0.5) * 0.12
    rgb = tint(c, body)
    a = np.full((size, size), alpha / 255.0) + (tnoise(seed + 4, 4, size) - 0.5) * 0.08
    for (gx, gy, r, k) in ((3.5, 3.0, 1.6, 0.55), (11.5, 10.0, 1.0, 0.3)):
        s = np.clip(1 - np.hypot(xx - gx, yy - gy) / r, 0, 1) * k
        rgb = screen_blend(rgb, s[..., None])
        a = np.maximum(a, s * 1.2)
    edge = (xx == 0) | (yy == 0)
    rgb[edge] = np.minimum(1, rgb[edge] * 1.05 + 0.03)
    a[edge] = np.maximum(a[edge], alpha / 255.0 + 0.12)
    return to_img(rgb, a)


def crystal(base, seed, alpha=200, size=16):
    c = rgbf(base)
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:size, 0:size]
    pts = rng.random((9, 2)) * size
    best = np.full((size, size), 1e9)
    second = np.full((size, size), 1e9)
    idx = np.zeros((size, size), dtype=int)
    for i, (px, py) in enumerate(pts):
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                d = np.hypot(xx - px - ox, yy - py - oy)
                closer = d < best
                second = np.where(closer, best, np.minimum(second, d))
                idx = np.where(closer, i, idx)
                best = np.where(closer, d, best)
    shade = 0.7 + 0.55 * (np.sin(idx * 2.3 + seed) * 0.5 + 0.5)
    ridge = np.clip(1 - (second - best) / 1.2, 0, 1)
    rgb = tint(c, shade)
    rgb = rgb * (1 - ridge[..., None] * 0.55) + np.minimum(1, c + 0.45)[None, None, :] * ridge[..., None] * 0.55
    glint = (rng.random((size, size)) > 0.965)
    rgb[glint] = 1.0
    a = np.full((size, size), alpha / 255.0)
    a = np.maximum(a, ridge * 0.95)
    return to_img(rgb, a)


def soft_disc(size, cx, cy, r, power=1.6):
    yy, xx = np.mgrid[0:size, 0:size]
    d = np.hypot(xx - cx, yy - cy) / r
    return np.clip(1 - d, 0, 1) ** power


def sprite_light(mask, height=None, strength=1.4):
    """Shading for a 2D sprite: distance-to-edge as a dome height, then lit from the top-left."""
    m = mask.astype(float)
    size = m.shape[0]
    dist = m.copy()
    cur = m.copy()
    for k in range(1, 6):
        er = cur * np.roll(cur, 1, 0) * np.roll(cur, -1, 0) * np.roll(cur, 1, 1) * np.roll(cur, -1, 1)
        dist += er
        cur = er
    h = np.sqrt(dist / max(1.0, dist.max()))
    if height is not None:
        h = h + height
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1)) * 0.5
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0)) * 0.5
    f = 1.0 - (gx + gy) * strength
    rim = m - np.roll(np.roll(m, 1, 0), 1, 1) * m
    f += rim * 0.25
    return f
