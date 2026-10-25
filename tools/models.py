"""3D cuboid model definitions (Blockbench-style JSON, written procedurally).

Coordinates are in model units (16 = one block). Guns point along -Z (muzzle at low Z),
melee weapons and tools point along +Y, projectile entity models point along +X.
"""
import math

GLOW = {"glow_cyan", "glow_green", "glow_orange", "glow_purple", "glow_red", "glow_blue",
        "glow_magenta", "glow_white", "screen", "screen_red"}


def B(x1, y1, z1, x2, y2, z2, mat, rot=None):
    if rot:
        rot = _legal_rot(rot)
        if rot and rot[1] == 90:
            # A quarter turn is exact on a box: swap the extents about the pivot instead.
            axis, _, (ox, oy, oz) = rot
            if axis == "y":
                x1, z1, x2, z2 = ox - (z1 - oz), oz + (x1 - ox), ox - (z2 - oz), oz + (x2 - ox)
            elif axis == "x":
                y1, z1, y2, z2 = oy - (z1 - oz), oz + (y1 - oy), oy - (z2 - oz), oz + (y2 - oy)
            else:
                x1, y1, x2, y2 = ox - (y1 - oy), oy + (x1 - ox), ox - (y2 - oy), oy + (x2 - ox)
            rot = None
        elif rot and rot[1] == 0:
            rot = None
    return {"from": [x1, y1, z1], "to": [x2, y2, z2], "mat": mat, "rot": rot}


def _legal_rot(rot):
    """Minecraft only accepts -45/-22.5/0/22.5/45 degrees: fold and snap anything else (90 is handled exactly)."""
    axis, angle, origin = rot
    a = ((angle + 90) % 180) - 90
    if a == -90:
        a = 90
    if a == 90:
        return (axis, 90, origin)
    a = min((-45, -22.5, 0, 22.5, 45), key=lambda v: abs(v - a))
    return (axis, a, origin)


def ring_z(cx, cy, z1, z2, r_in, r_out, mat):
    """Square frame around the Z axis."""
    return [
        B(cx - r_out, cy + r_in, z1, cx + r_out, cy + r_out, z2, mat),
        B(cx - r_out, cy - r_out, z1, cx + r_out, cy - r_in, z2, mat),
        B(cx - r_out, cy - r_in, z1, cx - r_in, cy + r_in, z2, mat),
        B(cx + r_in, cy - r_in, z1, cx + r_out, cy + r_in, z2, mat),
    ]


def cyl_z(cx, cy, z1, z2, r, mat, cap=None):
    """Octagonal prism along Z built from a cross of boxes."""
    k = r * 0.42
    out = [B(cx - r, cy - k, z1, cx + r, cy + k, z2, mat),
           B(cx - k, cy - r, z1, cx + k, cy + r, z2, mat),
           B(cx - r * 0.72, cy - r * 0.72, z1, cx + r * 0.72, cy + r * 0.72, z2, mat)]
    if cap:
        out.append(B(cx - r * 0.6, cy - r * 0.6, z1 - 0.5, cx + r * 0.6, cy + r * 0.6, z1, cap))
    return out


def cyl_y(cx, cz, y1, y2, r, mat):
    k = r * 0.42
    return [B(cx - r, y1, cz - k, cx + r, y2, cz + k, mat),
            B(cx - k, y1, cz - r, cx + k, y2, cz + r, mat),
            B(cx - r * 0.72, y1, cz - r * 0.72, cx + r * 0.72, y2, cz + r * 0.72, mat)]


def cyl_x(cy, cz, x1, x2, r, mat):
    k = r * 0.42
    return [B(x1, cy - k, cz - r, x2, cy + k, cz + r, mat),
            B(x1, cy - r, cz - k, x2, cy + r, cz + k, mat),
            B(x1, cy - r * 0.72, cz - r * 0.72, x2, cy + r * 0.72, cz + r * 0.72, mat)]


def ball(cx, cy, cz, r, mat):
    k = r * 0.55
    return [B(cx - r, cy - k, cz - k, cx + r, cy + k, cz + k, mat),
            B(cx - k, cy - r, cz - k, cx + k, cy + r, cz + k, mat),
            B(cx - k, cy - k, cz - r, cx + k, cy + k, cz + r, mat),
            B(cx - r * 0.8, cy - r * 0.8, cz - r * 0.8, cx + r * 0.8, cy + r * 0.8, cz + r * 0.8, mat)]


def grip(x, y_top, z, mat="rubber", length=6):
    return B(x - 1, y_top - length, z - 1.5, x + 1, y_top, z + 1.5, mat, ("x", -22.5, [x, y_top, z]))


# =========================================================================== guns (-Z forward)

def nuke_launcher():
    m = []
    m += [B(6, 7, -4, 10, 9, 20, "gunmetal")]                       # frame rail
    m += [B(5, 9, -5, 11, 10, 6, "steel")]                          # launch cradle
    m += ball(8, 13.5, -1.5, 4.5, "olive")                           # mini-nuke warhead
    m += [B(3.4, 12.5, -2.5, 12.6, 14.5, -0.5, "hazard")]           # warhead band
    m += [B(7.5, 9.5, 2.5, 8.5, 18, 5, "gunmetal"), B(3, 13, 2.5, 13, 14, 5, "gunmetal")]  # fins
    m += cyl_z(8, 10.5, 5, 11, 1.2, "chrome")                        # piston
    m += [B(5, 6, 11, 11, 13, 20, "gunmetal")]                      # catapult housing
    m += [B(4.8, 9, 12, 5, 12, 18, "radiation")]                    # side placard
    m += [B(10.6, 11, 16, 11.4, 12, 18.5, "glow_green")]            # status light
    m += [B(6.5, 13, 13, 9.5, 14.5, 18, "vent")]                    # top vent
    m += [grip(8, 7, 15), grip(8, 7, 3, length=5)]
    m += [B(6.5, 6, 20, 9.5, 11, 27, "gunmetal"), B(6, 5, 27, 10, 11.5, 28, "rubber")]  # stock
    return m, (8, 4, 15)


def rocket_launcher():
    m = []
    for (x, y) in ((4, 9), (8.5, 9), (4, 13.5), (8.5, 13.5)):
        m += [B(x, y, -4, x + 3.5, y + 3.5, 18, "olive")]
        m += [B(x - 0.25, y - 0.25, -5, x + 3.75, y + 3.75, -4, "gunmetal")]
        m += [B(x + 0.6, y + 0.6, -5.1, x + 2.9, y + 2.9, -4.9, "black")]
    m += [B(3.6, 8.6, 15, 12.4, 17.4, 16.5, "hazard")]
    m += [B(12, 13, 2, 14, 15.5, 10, "gunmetal"), B(12.2, 13.3, 1.9, 13.8, 15.2, 2, "glow_red")]
    m += [B(5, 8.5, 18, 11, 17, 20, "rubber")]
    m += [B(6.5, 7, 6, 9.5, 9, 14, "gunmetal")]
    m += [grip(8, 7, 12), grip(8, 7, 5, length=5)]
    m += [B(3.9, 12.6, 4, 12.6, 13.5, 8, "glow_green")]
    return m, (8, 3.5, 12)


def mirv_launcher():
    m = []
    m += cyl_z(8, 12, -5, 21, 4.5, "white")
    m += [B(4.5, 8.5, -6, 11.5, 15.5, -5, "red")]
    m += [B(6, 10, -6.6, 10, 14, -6, "red")]
    m += [B(3.2, 7.2, 0, 12.8, 16.8, 1.5, "red"), B(3.2, 7.2, 10, 12.8, 16.8, 11.5, "hazard")]
    m += [B(12, 9, 3, 14.5, 15, 15, "gunmetal"), B(14.4, 10, 5, 14.6, 14, 13, "screen")]
    m += [B(6, 6, 4, 10, 8, 16, "gunmetal")]
    m += [grip(8, 6, 13), grip(8, 6, 5, length=5)]
    m += [B(5, 16, 6, 11, 18, 10, "glow_red")]
    return m, (8, 2.5, 13)


def plasma_rifle():
    m = []
    m += [B(6, 7.5, 0, 10, 12, 20, "white")]
    m += cyl_z(8, 10, -8, 0, 1.2, "chrome")
    for z in (-6.5, -3.5):
        m += [B(6.6, 8.6, z, 9.4, 11.4, z + 1, "copper")]
    m += [B(7.3, 9.3, -9, 8.7, 10.7, -8, "glow_cyan")]
    m += [B(6.5, 12, 3, 9.5, 14, 13, "glow_cyan"), B(6.3, 11.8, 2.5, 9.7, 12.2, 13.5, "gunmetal")]
    m += [B(5.8, 8.5, 5, 6, 11, 16, "glow_cyan"), B(10, 8.5, 5, 10.2, 11, 16, "glow_cyan")]
    m += [B(7, 4, 7, 9, 7.5, 11, "glass_cyan")]
    m += [B(7.5, 12, 14, 8.5, 13, 18, "gunmetal")]
    m += [grip(8, 7.5, 15)]
    m += [B(7, 7, 20, 9, 11, 26, "white"), B(6.5, 5, 26, 9.5, 11.5, 27.5, "rubber")]
    return m, (8, 4.5, 15)


def railgun():
    m = []
    m += [B(6, 11.5, -12, 10, 13, 14, "chrome"), B(6, 6.5, -12, 10, 8, 14, "chrome")]
    m += [B(7, 8, -11, 9, 11.5, 13, "glow_blue")]
    for z in (-10, -6, -2, 2, 6):
        m += [B(5.7, 6.2, z, 10.3, 13.3, z + 0.8, "gunmetal")]
    m += [B(5, 5.5, 12, 11, 14, 22, "gunmetal")]
    for z in (13.5, 16.5, 19.5):
        m += [B(4.6, 6, z, 11.4, 13.5, z + 1.2, "copper")]
    m += [B(7, 14, 12, 9, 16, 20, "gunmetal"), B(7.2, 14.2, 11.9, 8.8, 15.8, 12, "glow_blue")]
    m += [B(11, 7, 14, 11.2, 12, 20, "screen")]
    m += [grip(8, 5.5, 18)]
    m += [B(7, 7, 22, 9, 12.5, 28, "carbon"), B(6.5, 6, 28, 9.5, 13, 29.5, "rubber")]
    return m, (8, 2.5, 18)


def minigun():
    m = []
    for k in range(6):
        a = math.radians(k * 60)
        bx, by = 8 + 2.6 * math.cos(a), 10 + 2.6 * math.sin(a)
        m += [B(bx - 0.75, by - 0.75, -10, bx + 0.75, by + 0.75, 8, "gunmetal")]
    m += [B(7.4, 9.4, -9, 8.6, 10.6, 8, "black")]
    for z in (-8.5, -1):
        m += [B(4.4, 6.4, z, 11.6, 13.6, z + 1.5, "steel")]
    m += [B(4, 6, 8, 12, 14, 18, "olive"), B(3.9, 5.9, 15.5, 12.1, 14.1, 16.5, "hazard")]
    m += [B(9, 1.5, 9, 14, 7.5, 17, "olive"), B(10, 7.5, 10, 11.5, 9.5, 14, "copper")]
    m += [B(7, 14, 10, 9, 17, 12, "rubber"), B(5.5, 16, 8, 10.5, 17, 15, "rubber")]
    m += [grip(8, 6, 18, length=5)]
    m += [B(4, 9, 17.9, 6, 11, 18.1, "glow_orange")]
    m += [B(4.6, 6.6, -10.6, 11.4, 13.4, -10, "steel"), B(6.6, 8.6, -10.9, 9.4, 11.4, -10.6, "glow_orange")]
    for z in (-5, 3):
        m += [B(4.7, 6.7, z, 11.3, 13.3, z + 0.8, "glow_orange"), B(4.5, 6.5, z + 0.8, 11.5, 13.5, z + 1.3, "gunmetal")]
    m += [B(11.9, 8, 9, 12.1, 12, 15, "glow_orange"), B(3.9, 8, 9, 4.1, 12, 15, "glow_orange")]
    m += [B(12, 6.5, 8.5, 12.4, 13.5, 9.1, "chrome"), B(3.6, 6.5, 8.5, 4, 13.5, 9.1, "chrome")]
    m += [B(13.9, 2, 10, 14.1, 7, 16, "radiation")]
    return m, (8, 3, 18)


def flamethrower():
    m = []
    m += cyl_z(8, 10, -6, 2, 1.2, "steel")
    m += [B(6.4, 8.4, -7, 9.6, 11.6, -6, "gunmetal"), B(8.6, 8, -6.5, 9.6, 9, -5.5, "glow_orange")]
    m += cyl_z(8, 10, 2, 17, 1.8, "gunmetal")
    m += cyl_z(8, 5.5, 2, 12, 2.4, "red")
    m += [B(7, 2.8, 4, 9, 3.2, 10, "hazard"), B(7.3, 7.8, 6, 8.7, 8.5, 7, "steel")]
    m += [B(6, 7, 17, 10, 12, 21, "gunmetal")]
    m += [grip(8, 8, 15)]
    m += [B(7.3, 11.5, 8, 8.7, 13, 12, "steel")]
    for z in (-5, -3.5, -2, -0.5):
        m += [B(6.5, 8.5, z, 9.5, 11.5, z + 0.6, "copper")]
    m += [B(7.4, 8.6, -7.8, 8.6, 9.6, -7, "glow_orange"), B(7.6, 8.8, -8.4, 8.4, 9.4, -7.8, "glow_gold"), B(7.75, 9.0, -8.8, 8.25, 9.3, -8.4, "glow_white")]
    m += [B(5.5, 3.5, 4.5, 10.5, 4.2, 5.2, "hazard"), B(5.5, 3.5, 9, 10.5, 4.2, 9.7, "hazard")]
    m += [B(10.3, 4.5, 6.3, 10.6, 6.5, 8.3, "screen_red"), B(10.2, 4.3, 6.1, 10.4, 6.7, 8.5, "chrome")]
    m += [B(7.6, 6.5, 11.5, 8.4, 8.5, 12.3, "rubber"), B(7.6, 8, 12.3, 8.4, 8.8, 15, "rubber")]
    m += [B(7.5, 12, 2.5, 8.5, 12.3, 16, "glow_orange")]
    return m, (8, 5, 15)


def arc_thrower():
    m = []
    m += [B(6, 12, -6, 7, 13, 2, "copper"), B(9, 12, -6, 10, 13, 2, "copper")]
    m += [B(6, 7, -6, 7, 8, 2, "copper"), B(9, 7, -6, 10, 8, 2, "copper")]
    m += [B(5.8, 11.8, -7, 7.2, 13.2, -6, "glow_blue"), B(8.8, 11.8, -7, 10.2, 13.2, -6, "glow_blue")]
    m += [B(5.8, 6.8, -7, 7.2, 8.2, -6, "glow_blue"), B(8.8, 6.8, -7, 10.2, 8.2, -6, "glow_blue")]
    m += ball(8, 10, -3, 2.0, "glow_blue")
    m += cyl_z(8, 10, -1, 8, 1.5, "copper")
    for z in (0, 2.5, 5):
        m += ring_z(8, 10, z, z + 1, 1.6, 2.6, "gunmetal")
    m += [B(6, 7, 8, 10, 12.5, 20, "gunmetal")]
    m += [B(6.5, 12.5, 10, 9.5, 15, 16, "glow_cyan"), B(6.3, 12.4, 9.5, 9.7, 12.6, 16.5, "steel")]
    m += [grip(8, 7, 16)]
    m += [B(7, 7.5, 20, 9, 11, 25, "gunmetal")]
    return m, (8, 4, 16)


def cryo_cannon():
    m = []
    m += cyl_z(8, 10, -4, 12, 3, "white")
    m += [B(5.5, 7.5, -5, 10.5, 12.5, -4, "glow_blue")]
    for z in (-1, 3, 7):
        m += ring_z(8, 10, z, z + 1, 2.8, 3.4, "glow_blue")
    m += [B(2.5, 8, 4, 4.8, 12, 14, "glass_cyan"), B(11.2, 8, 4, 13.5, 12, 14, "glass_cyan")]
    m += [B(2.7, 9.5, 5, 4.6, 10.5, 13, "glow_blue"), B(11.4, 9.5, 5, 13.3, 10.5, 13, "glow_blue")]
    m += [B(6, 7, 12, 10, 12.5, 20, "steel")]
    m += [grip(8, 7, 16)]
    return m, (8, 4, 16)


def singularity_projector():
    m = []
    m += ball(8, 10, -4, 1.6, "black")
    for z, r in ((-6, 2.6), (-3.5, 3.2), (-1, 2.6)):
        m += ring_z(8, 10, z, z + 0.8, r, r + 0.8, "glow_purple")
    m += [B(7.6, 6, -6, 8.4, 7, 0, "gunmetal"), B(7.6, 13, -6, 8.4, 14, 0, "gunmetal")]
    m += [B(5, 7, 0, 11, 13, 16, "carbon")]
    m += [B(4.8, 9, 2, 5, 11, 14, "glow_purple"), B(11, 9, 2, 11.2, 11, 14, "glow_purple")]
    m += [B(6, 13, 3, 10, 15, 11, "glass_purple"), B(7, 13.2, 4, 9, 14.6, 10, "glow_purple")]
    m += [grip(8, 7, 13)]
    m += [B(7, 8, 16, 9, 12, 22, "carbon")]
    return m, (8, 4, 13)


def quantum_drill():
    m = []
    m += [B(6, 8, -2, 10, 12, 2, "chrome"), B(6.5, 8.5, -5, 9.5, 11.5, -2, "chrome"),
          B(7, 9, -8, 9, 11, -5, "chrome"), B(7.5, 9.5, -10, 8.5, 10.5, -8, "chrome")]
    for z, (dx, dy) in zip((-1, -3.5, -6.5), ((2, 1), (-1.5, 1.5), (1, -1))):
        m += [B(8 + dx - 0.4, 10 + dy - 0.4, z, 8 + dx + 0.4, 10 + dy + 0.4, z + 1, "glow_cyan")]
    m += [B(5, 7, 2, 11, 13, 14, "yellow"), B(4.9, 8, 4, 5, 12, 12, "hazard"), B(11, 8, 4, 11.1, 12, 12, "hazard")]
    m += [B(6.5, 13, 5, 9.5, 14, 11, "glow_cyan")]
    m += [grip(8, 7, 11)]
    m += [B(6, 2, 9, 10, 4, 14, "glow_green"), B(5.8, 1.6, 8.6, 10.2, 2, 14.4, "gunmetal")]
    return m, (8, 4, 11)


def orbital_designator():
    m = []
    m += [B(4, 6, 2, 12, 12, 14, "gunmetal")]
    m += cyl_z(6, 9, 0.5, 2, 1.6, "black") + cyl_z(10, 9, 0.5, 2, 1.6, "black")
    m += [B(5, 8, 0.4, 7, 10, 0.5, "glow_red"), B(9, 8, 0.4, 11, 10, 0.5, "glow_red")]
    m += [B(11, 12, 11, 12, 18, 12, "steel"), B(10.8, 18, 10.8, 12.2, 19, 12.2, "glow_red")]
    m += [B(5, 7, 14, 11, 11, 14.2, "screen_red")]
    m += [B(3.8, 8, 4, 4, 10, 12, "hazard"), B(12, 8, 4, 12.2, 10, 12, "hazard")]
    m += [B(7, 2, 8, 9, 6, 11, "rubber")]
    m += [B(3.6, 6.6, 1.6, 12.4, 12.4, 2.2, "steel"), B(3.6, 6.6, 13.8, 12.4, 12.4, 14.4, "steel")]
    m += [B(5, 12, 5, 11, 12.6, 11, "vent"), B(5.5, 12.6, 9, 9.5, 13.4, 11, "gunmetal"), B(6, 13.4, 9.5, 9, 13.6, 10.5, "glow_red")]
    for k in range(3):
        m += [B(10.9, 12 + k * 2.2, 10.9, 12.3, 12.4 + k * 2.2, 12.3, "glow_red")]
    m += [B(12.2, 6.8, 6, 12.5, 7.4, 13, "glow_red"), B(3.5, 6.8, 6, 3.8, 7.4, 13, "glow_red")]
    return m, (8, 3, 9.5)


GUNS = {
    "nuke_launcher": nuke_launcher, "rocket_launcher": rocket_launcher, "mirv_launcher": mirv_launcher,
    "plasma_rifle": plasma_rifle, "railgun": railgun, "minigun": minigun, "flamethrower": flamethrower,
    "arc_thrower": arc_thrower, "cryo_cannon": cryo_cannon, "singularity_projector": singularity_projector,
    "quantum_drill": quantum_drill, "orbital_designator": orbital_designator,
}


# =========================================================================== melee / tools (+Y up)

def handle(y1, y2, mat="carbon", wrap="rubber"):
    out = [B(7.2, y1, 7.2, 8.8, y2, 8.8, mat)]
    out += [B(7, y1 + 1, 7, 9, y1 + 6, 9, wrap)]
    return out


def grip_wraps(y1, y2, mat="rubber", accent=None, step=1.4):
    """Ridged grip: alternating raised wraps (with an optional glowing gap) along +Y."""
    out = []
    y = y1
    i = 0
    while y + 0.9 <= y2:
        out.append(B(6.85, y, 6.85, 9.15, y + 0.9, 9.15, mat))
        if accent and i % 2 == 1:
            out.append(B(7.05, y + 0.9, 7.05, 8.95, y + step, 8.95, accent))
        y += step
        i += 1
    return out


def pommel(y, gem, metal="gunmetal"):
    return [B(6.6, y - 1.6, 6.6, 9.4, y, 9.4, metal), B(7.1, y - 2.4, 7.1, 8.9, y - 1.6, 8.9, metal),
            B(7.25, y - 3.2, 7.25, 8.75, y - 2.4, 8.75, gem, ("y", 45, [8, y - 2.8, 8]))]


def collar(y, h=1.0, r=1.35, mat="steel"):
    return [B(8 - r, y, 8 - r, 8 + r, y + h, 8 + r, mat)]


def gravity_hammer():
    m = [B(7.25, -6, 7.25, 8.75, 16, 8.75, "carbon")]
    m += grip_wraps(-5.5, 3.5, "rubber", "glow_purple")
    m += pommel(-6, "glow_purple")
    m += collar(4, 0.8) + collar(10, 0.8, 1.2, "gunmetal") + collar(14, 2, 1.6, "gunmetal")
    m += [B(7.1, 5, 7.1, 8.9, 9.5, 8.9, "glow_purple")]  # exposed field coil
    for y in (5.6, 7, 8.4):
        m += [B(6.9, y, 6.9, 9.1, y + 0.5, 9.1, "copper")]
    # head
    m += [B(2.5, 16, 4.5, 13.5, 25, 11.5, "gunmetal")]
    m += [B(6, 15.4, 4, 10, 25.6, 12, "steel")]                          # centre band
    m += [B(6.6, 17.5, 3.8, 9.4, 23.5, 4, "glow_purple"), B(6.6, 17.5, 12, 9.4, 23.5, 12.2, "glow_purple")]
    m += [B(7.4, 18.5, 3.7, 8.6, 22.5, 3.8, "glow_white"), B(7.4, 18.5, 12.2, 8.6, 22.5, 12.3, "glow_white")]
    for x0, x1, gx0, gx1 in ((0.5, 2.5, -0.2, 0.5), (13.5, 15.5, 15.5, 16.2)):
        m += [B(x0, 15.5, 4, x1, 25.5, 12, "steel")]                      # striking faces
        m += [B(gx0, 17, 5.5, gx1, 24, 10.5, "glow_purple")]
        m += [B(gx0 - 0.05 if gx0 < 1 else gx0, 18.5, 7, gx1 + 0.05 if gx1 > 15 else gx1, 22.5, 9, "glow_white")]
    for x in (3.5, 11):
        m += [B(x, 25, 5.5, x + 1.5, 25.6, 10.5, "vent")]
        m += [B(x, 15.4, 5.5, x + 1.5, 16, 10.5, "vent")]
    m += [B(2.5, 24.6, 4.5, 13.5, 25.2, 5.2, "hazard"), B(2.5, 24.6, 10.8, 13.5, 25.2, 11.5, "hazard")]
    for x in (3, 12.5):
        for y in (16.6, 23.8):
            m += [B(x, y, 4.3, x + 0.6, y + 0.6, 4.5, "chrome"), B(x, y, 11.5, x + 0.6, y + 0.6, 11.7, "chrome")]
    m += [B(7.5, 25.6, 7.5, 8.5, 28, 8.5, "gunmetal"), B(7.1, 28, 7.1, 8.9, 28.8, 8.9, "glow_purple", ("y", 45, [8, 28.4, 8]))]
    return m, (8, -2, 8)


def plasma_blade():
    m = [B(7.15, -5, 7.15, 8.85, 4, 8.85, "carbon")]
    m += grip_wraps(-4.6, 3.2, "rubber", "glow_magenta", 1.3)
    m += pommel(-5, "glow_magenta", "chrome")
    # guard: swept chrome wings with emitters
    m += [B(6.4, 4, 6.4, 9.6, 6, 9.6, "gunmetal")]
    m += [B(3.5, 4.4, 7, 7, 5.6, 9, "chrome", ("z", 22.5, [7, 5, 8])), B(9, 4.4, 7, 12.5, 5.6, 9, "chrome", ("z", -22.5, [9, 5, 8]))]
    m += [B(2.6, 5.4, 7.3, 3.6, 6.6, 8.7, "glow_magenta"), B(12.4, 5.4, 7.3, 13.4, 6.6, 8.7, "glow_magenta")]
    m += [B(6.8, 6, 6.8, 9.2, 7.6, 9.2, "chrome"), B(6.6, 7.6, 6.6, 9.4, 8.2, 9.4, "glow_magenta"), B(7, 8.2, 7, 9, 9, 9, "chrome")]
    # blade: white-hot core, magenta body, glass sheath, tapering tip
    m += [B(7.6, 9, 7.85, 8.4, 28, 8.15, "glow_white")]
    m += [B(7.2, 9, 7.6, 8.8, 28.5, 8.4, "glow_magenta")]
    m += [B(6.7, 9, 7.35, 9.3, 27.5, 8.65, "glass_magenta")]
    m += [B(7.0, 27.5, 7.4, 9.0, 29.5, 8.6, "glass_magenta"), B(7.4, 29.5, 7.5, 8.6, 31, 8.5, "glow_magenta"), B(7.75, 31, 7.75, 8.25, 31.8, 8.25, "glow_white")]
    for y in (12, 17, 22):
        m += [B(6.5, y, 7.6, 6.7, y + 1.4, 8.4, "glow_magenta"), B(9.3, y + 1.2, 7.6, 9.5, y + 2.6, 8.4, "glow_magenta")]
    return m, (8, 0, 8)


def storm_caller():
    m = [B(7.35, -8, 7.35, 8.65, 18, 8.65, "copper")]
    m += pommel(-8, "glow_blue", "gunmetal")
    for y in (-5, 1, 7, 13):
        m += [B(6.9, y, 6.9, 9.1, y + 0.8, 9.1, "gunmetal"), B(7.1, y + 0.8, 7.1, 8.9, y + 1.3, 8.9, "glow_blue")]
    m += grip_wraps(-3.6, 0.6, "rubber", None, 1.0)
    for y in range(3, 13, 2):                                   # wound coil
        m += [B(7.15, y, 7.15, 8.85, y + 0.6, 8.85, "copper"), B(7.25, y + 0.6, 7.25, 8.75, y + 1.0, 8.75, "glow_cyan")]
    m += [B(6, 18, 6, 10, 20.5, 10, "gunmetal"), B(6.4, 20.5, 6.4, 9.6, 21.2, 9.6, "glow_blue")]
    # four tesla prongs flaring out, each tipped with a spark
    for (x0, z0, ax, ang, sx, sz) in ((5, 7.5, "z", 22.5, -1, 0), (10, 7.5, "z", -22.5, 1, 0), (7.5, 5, "x", -22.5, 0, -1), (7.5, 10, "x", 22.5, 0, 1)):
        m += [B(x0, 19, z0, x0 + 1, 27, z0 + 1, "copper", (ax, ang, [x0 + 0.5, 19, z0 + 0.5]))]
        tx, tz = 8 + sx * 4.6, 8 + sz * 4.6
        m += [B(tx - 0.6, 26.3, tz - 0.6, tx + 0.6, 27.5, tz + 0.6, "glow_white", ("y", 45, [tx, 27, tz]))]
    m += [B(6.3, 21, 6.3, 9.7, 27.5, 9.7, "glass_cyan", ("y", 45, [8, 24, 8]))]
    m += [B(6.9, 22, 6.9, 9.1, 26.5, 9.1, "glow_blue", ("y", 45, [8, 24, 8]))]
    m += [B(7.4, 23, 7.4, 8.6, 25.5, 8.6, "glow_white", ("y", 45, [8, 24, 8]))]
    m += [B(7.3, 27.5, 7.3, 8.7, 30, 8.7, "glow_white", ("y", 45, [8, 28.5, 8]))]
    return m, (8, 2, 8)


def atomic_pickaxe():
    m = [B(7.25, -6, 7.25, 8.75, 15, 8.75, "carbon")]
    m += grip_wraps(-5.5, 3, "rubber", "glow_green")
    m += pommel(-6, "glow_green")
    m += collar(5, 0.8) + collar(11.5, 0.8)
    # pick arms: stepped curve down to points
    m += [B(0, 14.5, 7, 16, 17, 9, "titanium")]
    m += [B(-2.5, 14, 7.2, 0, 16.5, 8.8, "titanium"), B(-4.5, 13, 7.35, -2.5, 15.5, 8.65, "steel"), B(-6, 11.8, 7.5, -4.5, 14.2, 8.5, "steel"), B(-6.8, 11, 7.65, -6, 12.6, 8.35, "chrome")]
    m += [B(16, 14, 7.2, 18.5, 16.5, 8.8, "titanium"), B(18.5, 13, 7.35, 20.5, 15.5, 8.65, "steel"), B(20.5, 11.8, 7.5, 22, 14.2, 8.5, "steel"), B(22, 11, 7.65, 22.8, 12.6, 8.35, "chrome")]
    m += [B(0.5, 17, 7.6, 15.5, 17.4, 8.4, "glow_green")]               # spine glow
    m += [B(-4.4, 13.6, 7.3, 15.5 - 15.5 + -2.6, 13.9, 8.7, "glow_green"), B(18.6, 13.6, 7.3, 20.4, 13.9, 8.7, "glow_green")]
    # reactor core block
    m += [B(5.5, 12.5, 5.5, 10.5, 19, 10.5, "gunmetal")]
    m += [B(6.2, 13.2, 5.4, 9.8, 18.3, 5.5, "radiation"), B(6.2, 13.2, 10.5, 9.8, 18.3, 10.6, "radiation")]
    m += [B(5.4, 13.5, 6.5, 5.5, 18, 9.5, "glass_slime"), B(10.5, 13.5, 6.5, 10.6, 18, 9.5, "glass_slime")]
    m += [B(5.6, 14, 7, 10.4, 17.5, 9, "glow_green")]
    m += [B(6.5, 19, 6.5, 9.5, 20, 9.5, "steel"), B(7.2, 20, 7.2, 8.8, 21.4, 8.8, "glow_green", ("y", 45, [8, 20.7, 8]))]
    for x in (5.7, 9.8):
        for y in (12.8, 18.2):
            m += [B(x, y, 5.3, x + 0.5, y + 0.5, 5.5, "chrome"), B(x, y, 10.5, x + 0.5, y + 0.5, 10.7, "chrome")]
    return m, (8, -2, 8)


def fusion_shovel():
    m = [B(7.25, -6, 7.25, 8.75, 14, 8.75, "carbon")]
    m += grip_wraps(-5.5, 3, "rubber", "glow_orange")
    m += pommel(-6, "glow_orange")
    m += collar(6, 0.8) + collar(10, 0.8)
    m += [B(6.3, 12.5, 6.3, 9.7, 15.5, 9.7, "gunmetal"), B(6.7, 13, 6.2, 9.3, 15, 6.3, "glow_orange"), B(6.7, 13, 9.7, 9.3, 15, 9.8, "glow_orange")]
    # blade: rimmed spade with fusion vents and a white-hot edge
    m += [B(3.5, 15.5, 7.3, 12.5, 25, 8.7, "titanium")]
    m += [B(3, 15.5, 7.2, 3.5, 25, 8.8, "steel"), B(12.5, 15.5, 7.2, 13, 25, 8.8, "steel")]
    m += [B(4.2, 25, 7.35, 11.8, 26.5, 8.65, "titanium"), B(5.2, 26.5, 7.45, 10.8, 27.3, 8.55, "steel")]
    m += [B(5.2, 27.3, 7.6, 10.8, 27.8, 8.4, "glow_orange"), B(6.2, 27.8, 7.7, 9.8, 28.2, 8.3, "glow_white")]
    for y in (17, 19.3, 21.6):
        m += [B(5, y, 7.1, 11, y + 1.2, 7.3, "vent"), B(5, y, 8.7, 11, y + 1.2, 8.9, "vent")]
        m += [B(5.4, y + 1.2, 7.15, 10.6, y + 1.6, 7.25, "glow_orange"), B(5.4, y + 1.2, 8.75, 10.6, y + 1.6, 8.85, "glow_orange")]
    m += [B(7.2, 24, 7.0, 8.8, 25.6, 9.0, "glow_orange", ("z", 45, [8, 24.8, 8]))]
    return m, (8, -2, 8)


def plasma_cutter():
    m = [B(7.25, -6, 7.25, 8.75, 21, 8.75, "carbon")]
    m += grip_wraps(-5.5, 3, "rubber", "glow_cyan")
    m += pommel(-6, "glow_cyan")
    m += collar(5, 0.8) + collar(9, 0.8)
    m += [B(6.4, 12.5, 6.4, 9.6, 21, 9.6, "gunmetal")]
    m += [B(6.3, 14, 7.2, 6.4, 19.5, 8.8, "glow_cyan"), B(9.6, 14, 7.2, 9.7, 19.5, 8.8, "glow_cyan")]
    # axe blade: stepped crescent with a glowing plasma edge
    m += [B(9.6, 13.5, 7.4, 13.5, 20.5, 8.6, "titanium")]
    m += [B(13.5, 12.5, 7.45, 15, 21.5, 8.55, "titanium"), B(15, 11.5, 7.5, 16, 22.5, 8.5, "steel")]
    m += [B(16, 11, 7.6, 16.7, 23, 8.4, "glow_cyan"), B(16.7, 12, 7.7, 17.2, 22, 8.3, "glow_white")]
    m += [B(15.3, 22.5, 7.6, 16.3, 23.6, 8.4, "glow_cyan"), B(15.3, 10.4, 7.6, 16.3, 11.5, 8.4, "glow_cyan")]
    m += [B(10.5, 15.5, 7.3, 14, 18.5, 7.4, "glow_cyan"), B(10.5, 15.5, 8.6, 14, 18.5, 8.7, "glow_cyan")]
    m += [B(11, 16.2, 7.25, 13.5, 17.8, 7.3, "glow_white"), B(11, 16.2, 8.7, 13.5, 17.8, 8.75, "glow_white")]
    # back spike + power cell
    m += [B(3.8, 15.5, 7.4, 6.4, 18, 8.6, "gunmetal"), B(2.4, 16, 7.6, 3.8, 17.5, 8.4, "steel"), B(1.6, 16.4, 7.75, 2.4, 17.1, 8.25, "chrome")]
    m += [B(6.8, 21, 6.8, 9.2, 23, 9.2, "glass_cyan"), B(7.3, 21.4, 7.3, 8.7, 22.6, 8.7, "glow_cyan"), B(6.6, 23, 6.6, 9.4, 23.6, 9.4, "steel")]
    return m, (8, -2, 8)


MELEE = {
    "gravity_hammer": gravity_hammer, "plasma_blade": plasma_blade, "storm_caller": storm_caller,
    "atomic_pickaxe": atomic_pickaxe, "fusion_shovel": fusion_shovel, "plasma_cutter": plasma_cutter,
}


# =========================================================================== small objects

def frag_grenade():
    m = ball(8, 6.5, 8, 3.2, "olive")
    m += [B(4.6, 5.5, 4.6, 11.4, 6, 11.4, "gunmetal"), B(4.6, 7.5, 4.6, 11.4, 8, 11.4, "gunmetal")]
    m += [B(7, 9.5, 7, 9, 11.5, 9, "steel"), B(9, 6, 7.5, 10, 11.5, 8.5, "steel"),
          B(9.5, 11.5, 7.8, 11.5, 12, 8.2, "steel"), B(11.5, 10, 7.8, 12, 12, 8.2, "steel")]
    return m


def plasma_grenade():
    m = ball(8, 7, 8, 3.4, "glow_cyan")
    m += [B(4.2, 6.5, 4.2, 11.8, 7.5, 11.8, "white"), B(6.5, 3, 6.5, 9.5, 4, 9.5, "white"),
          B(6.5, 10, 6.5, 9.5, 11, 9.5, "white")]
    return m


def canister(body, band, cap="steel", glow=None):
    m = cyl_y(8, 8, 3, 12, 2.4, body)
    m += cyl_y(8, 8, 12, 13.5, 1.6, cap) + cyl_y(8, 8, 2, 3, 1.8, cap)
    m += cyl_y(8, 8, 2.6, 3.3, 2.6, "gunmetal") + cyl_y(8, 8, 11.7, 12.4, 2.6, "gunmetal")
    m += [B(5.4, 6, 5.4, 10.6, 7.5, 10.6, band)]
    m += [B(7.4, 13.5, 7.4, 8.6, 14.6, 8.6, "chrome"), B(6.6, 14.2, 7.6, 9.4, 14.6, 8.4, cap)]
    if glow:
        m += [B(7, 8, 5.5, 9, 11, 5.6, glow), B(7, 8, 10.4, 9, 11, 10.5, glow)]
        m += [B(5.5, 8, 7, 5.6, 11, 9, glow), B(10.4, 8, 7, 10.5, 11, 9, glow)]
        m += [B(7.4, 8.4, 5.45, 8.6, 10.6, 5.5, "glow_white")]
    for (x, z) in ((5.2, 7.7), (10.3, 7.7), (7.7, 5.2), (7.7, 10.3)):
        m += [B(x, 6.3, z, x + 0.5, 7.2, z + 0.6, "chrome")]
    return m


def gravity_grenade():
    m = ball(8, 7, 8, 2.2, "black")
    # Horizontal orbit ring (frame in the XZ plane).
    m += [B(3.5, 6.6, 3.5, 12.5, 7.4, 4.3, "glow_purple"), B(3.5, 6.6, 11.7, 12.5, 7.4, 12.5, "glow_purple"),
          B(3.5, 6.6, 4.3, 4.3, 7.4, 11.7, "glow_purple"), B(11.7, 6.6, 4.3, 12.5, 7.4, 11.7, "glow_purple")]
    # Vertical orbit ring (frame in the XY plane), tilted.
    m += [B(4, 2.5, 7.6, 12, 3.3, 8.4, "glow_magenta", ("z", 22.5, [8, 7, 8])),
          B(4, 10.7, 7.6, 12, 11.5, 8.4, "glow_magenta", ("z", 22.5, [8, 7, 8])),
          B(4, 3.3, 7.6, 4.8, 10.7, 8.4, "glow_magenta", ("z", 22.5, [8, 7, 8])),
          B(11.2, 3.3, 7.6, 12, 10.7, 8.4, "glow_magenta", ("z", 22.5, [8, 7, 8]))]
    m += [B(7, 9.2, 7, 9, 10.2, 9, "gunmetal")]
    return m


def pocket_nuke():
    m = ball(8, 7, 7, 3.6, "fatman")
    m += [B(5, 4, 10.5, 11, 10, 13, "gunmetal"), B(4.5, 6.5, 12, 11.5, 7.5, 13.5, "gunmetal"),
          B(7.5, 3.5, 12, 8.5, 10.5, 13.5, "gunmetal")]
    m += [B(6.2, 5.2, 3.3, 9.8, 8.8, 3.4, "radiation")]
    return m


def rocket_item():
    m = cyl_y(8, 8, 1, 12, 1.6, "white")
    m += [B(6.8, 12, 6.8, 9.2, 14, 9.2, "red"), B(7.3, 14, 7.3, 8.7, 15.5, 8.7, "red")]
    m += [B(5, 1, 7.6, 11, 4, 8.4, "gunmetal"), B(7.6, 1, 5, 8.4, 4, 11, "gunmetal")]
    m += [B(7, 0, 7, 9, 1, 9, "black"), B(6.3, 7, 6.3, 9.7, 8, 9.7, "hazard")]
    return m


def mini_nuke_item():
    m = ball(8, 7, 8, 4.2, "olive")
    m += [B(3.6, 6.3, 3.6, 12.4, 7.7, 12.4, "hazard")]
    m += [B(7.5, 11, 4, 8.5, 15, 12, "gunmetal"), B(4, 11, 7.5, 12, 15, 8.5, "gunmetal")]
    m += [B(6.5, 2.4, 6.5, 9.5, 3, 9.5, "olive")]
    return m


def plasma_cell():
    m = cyl_y(8, 8, 4, 12, 2.2, "glass_cyan") + cyl_y(8, 8, 5, 11, 1.2, "glow_cyan")
    m += cyl_y(8, 8, 12, 13.5, 2.5, "steel") + cyl_y(8, 8, 2.5, 4, 2.5, "steel")
    return m


def railgun_slug():
    m = []
    for i, dx in enumerate((-2.5, 0, 2.5)):
        m += [B(7.2 + dx, 2, 7.2, 8.8 + dx, 12 + i, 8.8, "chrome"), B(7.5 + dx, 12 + i, 7.5, 8.5 + dx, 13.5 + i, 8.5, "copper")]
    return m


def bullet_belt():
    m = [B(3, 2, 4, 13, 9, 12, "olive"), B(2.8, 7, 3.8, 13.2, 7.6, 12.2, "gunmetal"), B(6, 4, 3.9, 10, 6, 4, "yellow")]
    for i in range(5):
        x = 4 + i * 1.8
        m += [B(x, 9, 7, x + 1.2, 12, 8.2, "copper"), B(x + 0.2, 12, 7.2, x + 1.0, 13, 8.0, "gunmetal")]
    return m


def fuel_canister():
    return [B(4, 2, 5, 12, 13, 11, "red"), B(5, 13, 7, 11, 15, 9, "red"), B(6, 13, 7.5, 10, 14, 8.5, "black"),
            B(10, 13, 6.5, 12, 15, 9.5, "steel"), B(5, 6, 4.9, 11, 9, 5, "hazard"), B(5, 6, 11, 11, 9, 11.1, "hazard")]


def cryo_canister():
    return canister("white", "glow_blue", glow="glow_blue")


def detonator():
    return [B(5, 1, 6, 11, 11, 10, "red"), B(6, 7, 5.9, 10, 10, 6, "screen_red"),
            B(6, 11, 7, 8, 12, 9, "glow_red"), B(5.5, 11, 6.5, 8.5, 11.4, 9.5, "gunmetal"),
            B(9, 11, 7.5, 10, 17, 8.5, "steel"), B(8.7, 17, 7.2, 10.3, 18, 8.8, "glow_red"),
            B(6, 3, 5.9, 10, 5, 6, "hazard")]


def geiger_counter():
    return [B(4, 2, 5, 12, 10, 11, "yellow"), B(5, 5, 4.9, 11, 9, 5, "screen"), B(6, 3, 4.9, 8, 4, 5, "glow_green"),
            B(6, 10, 7, 10, 11, 9, "rubber"), B(5.5, 11, 7, 6.5, 13, 9, "rubber"), B(9.5, 11, 7, 10.5, 13, 9, "rubber"),
            B(5.5, 12, 7, 10.5, 13, 9, "rubber"), B(12, 3, 7, 13.5, 12, 9, "steel"), B(11.8, 12, 6.8, 13.7, 13.5, 9.2, "black"),
            B(7, 6, 11, 9, 8, 11.1, "radiation")]


def rad_x():
    return [B(7, 2, 7, 9, 12, 9, "glass_cyan"), B(7.3, 3, 7.3, 8.7, 10, 8.7, "glow_orange"),
            B(6, 12, 6, 10, 13.5, 10, "white"), B(7.6, 0, 7.6, 8.4, 2, 8.4, "chrome"), B(7.5, 13.5, 7.5, 8.5, 15, 8.5, "white"),
            B(7.8, -1.2, 7.8, 8.2, 0, 8.2, "chrome"), B(6.8, 5, 6.8, 9.2, 5.4, 9.2, "radiation"), B(6.8, 8, 6.8, 9.2, 8.4, 9.2, "gunmetal"),
            B(7.55, 3.4, 7.55, 8.45, 9.6, 8.45, "glow_white"), B(4.8, 12.4, 7.6, 6, 13.1, 8.4, "white"), B(10, 12.4, 7.6, 11.2, 13.1, 8.4, "white"),
            B(7.3, 15, 7.3, 8.7, 15.6, 8.7, "glow_orange")]


def nano_medkit():
    return [B(3, 2, 4, 13, 10, 12, "white"), B(7, 4, 3.9, 9, 9, 4, "red"), B(5.5, 5.5, 3.9, 10.5, 7.5, 4, "red"),
            B(6, 10, 7, 10, 11, 9, "gunmetal"), B(3, 6, 3.8, 13, 6.5, 12.2, "glow_green")]


SMALL = {
    "frag_grenade": frag_grenade, "plasma_grenade": plasma_grenade,
    "cryo_grenade": lambda: canister("white", "glow_blue", glow="glow_blue"),
    "incendiary_grenade": lambda: canister("red", "hazard", glow="glow_orange"),
    "gravity_grenade": gravity_grenade,
    "toxic_grenade": lambda: canister("toxic", "biohazard", glow="glow_green"),
    "pocket_nuke": pocket_nuke, "mini_nuke": mini_nuke_item, "plasma_cell": plasma_cell, "railgun_slug": railgun_slug,
    "bullet_belt": bullet_belt, "fuel_canister": fuel_canister, "cryo_canister": cryo_canister,
    "detonator": detonator, "geiger_counter": geiger_counter, "rad_x": rad_x, "nano_medkit": nano_medkit,
    "rocket": rocket_item,
}


# =========================================================================== entity models (+X forward)

def entity_rocket():
    m = cyl_x(8, 8, -2, 14, 1.6, "white")
    m += [B(14, 6.8, 6.8, 16, 9.2, 9.2, "red"), B(16, 7.3, 7.3, 17.5, 8.7, 8.7, "red")]
    m += [B(-2, 4.5, 7.6, 2, 11.5, 8.4, "gunmetal"), B(-2, 7.6, 4.5, 2, 8.4, 11.5, "gunmetal")]
    m += [B(-3, 7, 7, -2, 9, 9, "black"), B(6, 6.3, 6.3, 7, 9.7, 9.7, "hazard")]
    return m


def entity_mini_nuke():
    m = ball(8, 8, 8, 4.5, "olive")
    m += [B(7.5, 3.4, 3.4, 8.5, 12.6, 12.6, "hazard")]
    m += [B(12.4, 6, 6, 13.5, 10, 10, "olive")]
    m += [B(0, 7.5, 3, 4, 8.5, 13, "gunmetal"), B(0, 3, 7.5, 4, 13, 8.5, "gunmetal"), B(0, 6, 6, 1, 10, 10, "gunmetal")]
    return m


def entity_bomblet():
    m = cyl_x(8, 8, 4, 11, 1.8, "gunmetal")
    m += [B(11, 6.8, 6.8, 13, 9.2, 9.2, "red"), B(3, 5.5, 7.6, 5, 10.5, 8.4, "steel"), B(3, 7.6, 5.5, 5, 8.4, 10.5, "steel")]
    return m


ENTITIES = {"rocket": entity_rocket, "mini_nuke": entity_mini_nuke, "bomblet": entity_bomblet}


# =========================================================================== bomb blocks (facing north)

def cradle():
    return [B(2, 0, 2, 14, 1, 4, "steel"), B(2, 0, 12, 14, 1, 14, "steel"),
            B(2.5, 1, 2.5, 4, 4, 3.5, "steel"), B(12, 1, 2.5, 13.5, 4, 3.5, "steel"),
            B(2.5, 1, 12.5, 4, 4, 13.5, "steel"), B(12, 1, 12.5, 13.5, 4, 13.5, "steel"),
            B(1.6, 0, 1.6, 2.6, 1.6, 4.4, "gunmetal"), B(13.4, 0, 1.6, 14.4, 1.6, 4.4, "gunmetal"),
            B(1.6, 0, 11.6, 2.6, 1.6, 14.4, "gunmetal"), B(13.4, 0, 11.6, 14.4, 1.6, 14.4, "gunmetal"),
            B(4, 0.3, 1.9, 12, 0.7, 2, "hazard"), B(4, 0.3, 14, 12, 0.7, 14.1, "hazard")]


def demolition_charge():
    m = [B(3, 0, 2, 13, 4, 14, "c4"), B(3.5, 4, 2.5, 12.5, 5, 13.5, "c4")]
    m += [B(2.9, 1.5, 4, 13.1, 2.5, 5, "black"), B(2.9, 1.5, 11, 13.1, 2.5, 12, "black")]
    m += [B(5.5, 5, 6, 10.5, 6, 10, "gunmetal"), B(6, 5.1, 6.5, 10, 6.1, 9.5, "screen_red")]
    m += [B(10.5, 5, 7, 11.5, 6.5, 8, "glow_red"), B(4, 4.5, 4, 5, 5.5, 12, "red"), B(11, 4.5, 4, 12, 5.5, 12, "copper")]
    return m


def thermobaric_bomb():
    m = cradle()
    m += cyl_z(8, 8.5, 1.5, 14, 5, "gunmetal")
    m += [B(4.5, 5, 0.5, 11.5, 12, 1.5, "gunmetal"), B(6, 6.5, 0, 10, 10.5, 0.5, "red")]
    m += [B(2.6, 3.1, 4, 13.4, 13.9, 5, "red"), B(2.6, 3.1, 9, 13.4, 13.9, 10, "hazard")]
    m += [B(7.5, 3, 14, 8.5, 14, 16, "steel"), B(2.5, 8, 14, 13.5, 9, 16, "steel")]
    return m


def cryo_bomb():
    m = [B(1, 0, 1, 15, 2, 15, "steel")]
    m += ball(8, 8, 8, 5.5, "white")
    m += [B(2.2, 7.4, 2.2, 13.8, 8.6, 13.8, "glow_blue"), B(7.4, 2.5, 2.2, 8.6, 13.5, 13.8, "glow_blue")]
    m += [B(6.5, 13.5, 6.5, 9.5, 15, 9.5, "steel"), B(7, 15, 7, 9, 15.5, 9, "glow_blue")]
    return m


def singularity_bomb():
    m = [B(1, 0, 1, 15, 1.5, 15, "gunmetal"), B(1, 13.5, 1, 15, 15, 15, "gunmetal")]
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m += [B(x, 1.5, z, x + 1.5, 13.5, z + 1.5, "chrome")]
    m += ball(8, 7.5, 8, 2.5, "black")
    m += [B(3, 7, 3, 13, 8, 13, "glow_purple"), B(7.5, 2.5, 3, 8.5, 12.5, 13, "glow_purple"), B(3, 2.5, 7.5, 13, 12.5, 8.5, "glow_purple")]
    m += [B(6, 15, 6, 10, 15.6, 10, "glow_purple")]
    return m


def neutron_bomb():
    m = cradle()
    m += cyl_z(8, 8, 0, 16, 3.6, "olive")
    for z in (2, 6, 10):
        m += ring_z(8, 8, z, z + 1, 3.4, 4.2, "glow_green")
    m += [B(5.8, 5.8, -0.4, 10.2, 10.2, 0, "radiation")]
    m += [B(3, 7.5, 13, 13, 8.5, 16, "gunmetal")]
    return m


def atomic_bomb():
    m = cradle()
    m += ball(8, 8.5, 6.5, 5.6, "fatman")
    m += [B(3, 3, 1, 13, 14, 12, "fatman")]
    m += [B(4.5, 4.5, 12, 11.5, 12.5, 16, "gunmetal"), B(3.5, 3.5, 15, 12.5, 13.5, 16, "gunmetal"),
          B(7.5, 3.5, 12, 8.5, 13.5, 16, "steel"), B(3.5, 8, 12, 12.5, 9, 16, "steel")]
    m += [B(5.5, 6, 0.4, 10.5, 11, 0.5, "radiation"), B(2.9, 7, 3, 13.1, 8, 9, "black")]
    return m


def antimatter_bomb():
    m = [B(1, 0, 1, 15, 2.5, 15, "gunmetal"), B(2, 12.5, 2, 14, 14.5, 14, "gunmetal"), B(6, 14.5, 6, 10, 15.5, 10, "glow_magenta")]
    for (x, z) in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):
        m += [B(x, 2.5, z, x + 1.5, 12.5, z + 1.5, "chrome")]
    m += [B(3.5, 2.5, 3.5, 12.5, 12.5, 12.5, "glass_magenta")]
    m += ball(8, 7.5, 8, 2.4, "glow_magenta")
    m += [B(1.5, 1, 0.9, 6, 2, 1, "hazard"), B(10, 1, 0.9, 14.5, 2, 1, "hazard")]
    return m


def hydrogen_bomb():
    m = cradle()
    m += cyl_z(8, 8, 2, 13, 5, "tsar")
    m += [B(4.5, 4.5, 0.5, 11.5, 11.5, 2, "tsar"), B(6, 6, 0, 10, 10, 0.5, "tsar")]
    m += [B(2.6, 2.6, 6, 13.4, 13.4, 7, "red"), B(2.6, 2.6, 7, 13.4, 13.4, 7.5, "white")]
    m += [B(5, 5, 13, 11, 11, 16, "gunmetal"), B(1, 7.5, 13, 15, 8.5, 16, "gunmetal"), B(7.5, 1, 13, 8.5, 15, 16, "gunmetal")]
    m += [B(12.9, 6, 3, 13.1, 10, 5, "radiation")]
    return m


BOMBS = {
    "demolition_charge": demolition_charge, "thermobaric_bomb": thermobaric_bomb, "cryo_bomb": cryo_bomb,
    "singularity_bomb": singularity_bomb, "neutron_bomb": neutron_bomb, "atomic_bomb": atomic_bomb,
    "antimatter_bomb": antimatter_bomb, "hydrogen_bomb": hydrogen_bomb,
}


# =========================================================================== INSANE TIER

RAINBOW = ["glow_red", "glow_orange", "glow_green", "glow_cyan", "glow_blue", "glow_purple", "glow_magenta"]


def dubstep_gun():
    m = [B(4, 7, 2, 12, 14, 18, "carbon")]
    m += [B(3.8, 8, 5, 4, 13, 15, "speaker"), B(12, 8, 5, 12.2, 13, 15, "speaker")]
    m += [B(4.5, 7.5, 1, 11.5, 13.5, 2, "speaker")]
    m += ring_z(8, 10.5, 0, 1, 3.6, 4.4, "glow_magenta")
    m += [B(6, 8.5, -4, 10, 12.5, 0, "chrome"), B(5.5, 8, -5, 10.5, 13, -4, "glow_cyan")]
    for i, mat in enumerate(RAINBOW):
        h = [2, 4, 3, 5, 3.5, 2.5, 4.5][i]
        m += [B(5 + i * 0.85, 14, 6 + i * 1.4, 5.7 + i * 0.85, 14 + h, 7 + i * 1.4, mat)]
    m += [B(6, 14, 3, 10, 15, 4, "gunmetal"), B(6, 14, 16, 10, 15, 17, "gunmetal")]
    m += [grip(8, 7, 15)]
    m += [B(6.5, 6, 18, 9.5, 12, 24, "carbon"), B(6, 5, 24, 10, 12.5, 25, "glow_magenta")]
    return m, (8, 4, 15)


def death_ray():
    m = [B(6, 7, 4, 10, 12, 20, "white"), B(5.9, 9, 6, 6, 11, 18, "glow_red"), B(10, 9, 6, 10.1, 11, 18, "glow_red")]
    m += cyl_z(8, 9.5, -10, 4, 1.4, "chrome")
    for z in (-8, -5, -2, 1):
        m += ring_z(8, 9.5, z, z + 0.8, 1.5, 2.3, "glow_red")
    m += [B(7.2, 8.7, -11, 8.8, 10.3, -10, "glow_white")]
    m += [B(6.5, 12, 6, 9.5, 15, 14, "glass_magenta"), B(7, 12.5, 7, 9, 14.5, 13, "glow_red")]
    m += [B(4.5, 8, 10, 6, 11, 16, "vent"), B(10, 8, 10, 11.5, 11, 16, "vent")]
    m += [B(7, 4, 8, 9, 7, 12, "glow_orange")]
    m += [grip(8, 7, 16)]
    m += [B(7, 7, 20, 9, 11, 26, "white"), B(6.5, 6, 26, 9.5, 11.5, 27, "red")]
    return m, (8, 4, 16)


def apocalypse_gatling():
    m = []
    for k in range(6):
        a = math.radians(k * 60)
        bx, by = 8 + 3.4 * math.cos(a), 10.5 + 3.4 * math.sin(a)
        m += [B(bx - 1.1, by - 1.1, -12, bx + 1.1, by + 1.1, 8, "gunmetal"), B(bx - 1.2, by - 1.2, -12.5, bx + 1.2, by + 1.2, -11.5, "hazard")]
    m += [B(7, 9.5, -11, 9, 11.5, 8, "glow_green")]
    for z in (-9, -3, 3):
        m += ring_z(8, 10.5, z, z + 1.2, 4.5, 5.6, "fatman")
    m += [B(2.5, 5, 8, 13.5, 16, 19, "olive"), B(2.4, 4.9, 13, 13.6, 16.1, 14, "hazard")]
    m += cyl_z(8, 2, 9, 18, 3.6, "fatman") + [B(5.8, -1.8, 12, 10.2, -1.6, 15, "radiation")]
    m += [B(7, 16, 10, 9, 19, 12, "rubber"), B(5, 18, 8, 11, 19, 16, "rubber")]
    m += [B(2.4, 10, 16, 2.5, 12, 18, "glow_green"), B(13.5, 10, 16, 13.6, 12, 18, "glow_green")]
    m += [grip(8, 5, 19, length=5)]
    return m, (8, 2, 19)


def annihilator_cannon():
    m = [B(3, 5, 0, 13, 15, 18, "gunmetal")]
    m += [B(4, 6, -1, 12, 14, 0, "glow_green"), B(5.5, 7.5, -1.2, 10.5, 12.5, -1, "glow_white")]
    m += ring_z(8, 10, -2, -1, 4.5, 5.6, "chrome")
    for z in (3, 7, 11):
        m += [B(2.8, 7, z, 3, 13, z + 2, "glow_green"), B(13, 7, z, 13.2, 13, z + 2, "glow_green")]
    m += [B(5, 15, 4, 11, 16, 14, "vent"), B(7, 16, 6, 9, 18.5, 8, "rubber"), B(7, 16, 11, 9, 18.5, 13, "rubber"), B(7, 18, 6, 9, 18.5, 13, "rubber")]
    m += [B(2.9, 5.5, 14, 13.1, 14.5, 15, "hazard")]
    m += [grip(8, 5, 14)]
    m += [B(6, 6, 18, 10, 13, 24, "gunmetal")]
    return m, (8, 2, 14)


def kinetic_designator():
    m = [B(4, 6, 2, 12, 12, 14, "titanium")]
    m += cyl_z(8, 9, 0.3, 2, 2.2, "black") + [B(6, 7, 0.2, 10, 11, 0.3, "glow_white")]
    m += [B(5, 12, 4, 11, 13, 12, "gunmetal"), B(7.2, 13, 3, 8.8, 14.6, 13, "chrome"), B(7.5, 13.2, 1.5, 8.5, 14.4, 3, "copper")]
    m += [B(5, 7, 14, 11, 11, 14.2, "screen")]
    m += [B(3.8, 8, 4, 4, 10, 12, "yellow"), B(12, 8, 4, 12.2, 10, 12, "yellow")]
    m += [B(7, 2, 8, 9, 6, 11, "rubber")]
    m += [B(3.6, 6.6, 1.6, 12.4, 12.4, 2.2, "steel"), B(3.6, 6.6, 13.8, 12.4, 12.4, 14.4, "steel")]
    m += [B(12.2, 10.4, 4.5, 12.5, 10.8, 11.5, "glow_white"), B(3.5, 10.4, 4.5, 3.8, 10.8, 11.5, "glow_white"),
          B(12.2, 7.2, 4.5, 12.5, 7.6, 11.5, "glow_cyan"), B(3.5, 7.2, 4.5, 3.8, 7.6, 11.5, "glow_cyan")]
    m += [B(5.5, 14.6, 5, 10.5, 15.2, 11, "vent"), B(6, 15.2, 6, 10, 15.6, 10, "glow_white")]
    m += [B(8.8, 13, 12, 9.4, 17, 12.6, "steel"), B(8.6, 17, 11.8, 9.6, 17.8, 12.8, "glow_white")]
    return m, (8, 3, 9.5)


def meteor_staff():
    m = [B(7.35, -8, 7.35, 8.65, 18, 8.65, "black")]
    m += pommel(-8, "glow_orange", "obsidian")
    for y in (-5, 1, 7, 13):
        m += [B(6.9, y, 6.9, 9.1, y + 0.7, 9.1, "obsidian"), B(7.05, y + 0.7, 7.05, 8.95, y + 1.4, 8.95, "glow_orange")]
    m += [B(7.5, -4, 7.3, 8.5, 16, 7.35, "glow_red"), B(7.5, -4, 8.65, 8.5, 16, 8.7, "glow_red")]   # magma veins
    m += [B(6.2, 16.5, 6.2, 9.8, 18.5, 9.8, "obsidian"), B(6.6, 18.5, 6.6, 9.4, 19.2, 9.4, "glow_orange")]
    # four obsidian talons gripping the meteor
    for (x0, z0, ax, ang) in ((5.4, 7.5, "z", 22.5), (9.6, 7.5, "z", -22.5), (7.5, 5.4, "x", -22.5), (7.5, 9.6, "x", 22.5)):
        m += [B(x0, 18, z0, x0 + 1, 25, z0 + 1, "obsidian", (ax, ang, [x0 + 0.5, 18, z0 + 0.5]))]
    m += ball(8, 24.5, 8, 3.4, "rock")
    m += [B(5.8, 22, 5.0, 7, 27, 5.2, "glow_orange"), B(9, 22.5, 10.8, 10.4, 26.5, 11, "glow_orange"),
          B(10.9, 22.5, 6.5, 11.1, 25.5, 9.5, "glow_orange"), B(4.9, 23, 8, 5.1, 26.5, 10, "glow_red"),
          B(6.5, 27.8, 6.5, 9.5, 28, 9.5, "glow_orange"), B(7.2, 27.9, 7.2, 8.8, 28.1, 8.8, "glow_white")]
    m += [B(2.5, 24.1, 2.5, 13.5, 24.7, 13.5, "glow_orange", ("y", 45, [8, 24.4, 8]))]
    m += [B(3.5, 23.4, 3.5, 12.5, 23.8, 12.5, "glow_red", ("y", 22.5, [8, 23.6, 8]))]
    return m, (8, 2, 8)


def airstrike_beacon():
    return [B(6, 1, 6, 10, 11, 10, "red"), B(6.5, 11, 6.5, 9.5, 12, 9.5, "black"), B(7, 12, 7, 9, 14, 9, "glow_red"),
            B(5.9, 4, 5.9, 10.1, 6, 10.1, "hazard"), B(9.5, 9, 7.5, 10.5, 16, 8.5, "steel"), B(9.3, 16, 7.3, 10.7, 17, 8.7, "glow_red"),
            B(6.5, 7, 5.8, 9.5, 9, 6, "screen_red")]


def nuke_barrage():
    return [B(2, 1, 4, 14, 4, 12, "black"), B(2, 4, 11, 14, 13, 12, "black"), B(3, 5, 10.9, 13, 12, 11, "screen_red"),
            B(3, 4, 5, 13, 4.2, 10, "gunmetal"), B(6.5, 4.2, 6, 9.5, 5.5, 9, "glow_red"), B(6, 4.1, 5.5, 10, 4.3, 9.5, "hazard"),
            B(6, 1, 3, 10, 2.5, 4, "gunmetal"), B(4, 7, 10.8, 7, 10, 10.9, "radiation")]


def city_capsule():
    m = [B(4, 1, 4, 12, 2, 12, "gunmetal"), B(4, 13, 4, 12, 14, 12, "gunmetal")]
    m += [B(5, 2, 5, 7, 9, 7, "white"), B(8, 2, 5, 11, 12, 8, "glass_cyan"), B(5, 2, 8, 8, 6, 11, "yellow"), B(9, 2, 9, 11, 8, 11, "white")]
    m += [B(8.8, 12, 6.3, 9.2, 13, 6.7, "glow_red")]
    m += [B(3.8, 2, 3.8, 12.2, 13, 12.2, "glass_cyan")]
    for (x, z) in ((3.4, 3.4), (11.8, 3.4), (3.4, 11.8), (11.8, 11.8)):
        m += [B(x, 0.6, z, x + 0.8, 14.4, z + 0.8, "chrome")]
    m += [B(3.6, 0.4, 3.6, 12.4, 1, 12.4, "glow_cyan"), B(3.6, 14, 3.6, 12.4, 14.6, 12.4, "glow_cyan")]
    m += [B(6, 14.6, 6, 10, 15.2, 10, "gunmetal"), B(7.4, 15.2, 7.4, 8.6, 16.2, 8.6, "glow_white")]
    for (x, z, h) in ((5.2, 9.6, 3), (9.6, 5.4, 5), (6.2, 5.6, 2)):
        m += [B(x, 9 if h == 3 else 12 if h == 5 else 9, z, x + 0.3, 9.4 if h == 3 else 12.4 if h == 5 else 9.4, z + 0.3, "glow_gold")]
    return m


def overdrive_stim():
    return [B(7, 2, 7, 9, 12, 9, "glass_magenta"), B(7.3, 3, 7.3, 8.7, 11, 8.7, "glow_magenta"),
            B(6, 12, 6, 10, 13.5, 10, "gunmetal"), B(7.6, 0, 7.6, 8.4, 2, 8.4, "chrome"), B(7.5, 13.5, 7.5, 8.5, 15.5, 8.5, "glow_red"),
            B(7.8, -1.2, 7.8, 8.2, 0, 8.2, "chrome"), B(6.8, 4, 6.8, 9.2, 4.4, 9.2, "gunmetal"), B(6.8, 7, 6.8, 9.2, 7.4, 9.2, "gunmetal"),
            B(6.8, 10, 6.8, 9.2, 10.4, 9.2, "gunmetal"), B(7.55, 3.4, 7.55, 8.45, 10.6, 8.45, "glow_white"),
            B(4.8, 12.4, 7.6, 6, 13.1, 8.4, "gunmetal"), B(10, 12.4, 7.6, 11.2, 13.1, 8.4, "gunmetal"), B(7.3, 15.5, 7.3, 8.7, 16.1, 8.7, "gunmetal")]


INSANE_GUNS = {"dubstep_gun": dubstep_gun, "death_ray": death_ray, "apocalypse_gatling": apocalypse_gatling,
               "annihilator_cannon": annihilator_cannon, "kinetic_designator": kinetic_designator}
INSANE_MELEE = {"meteor_staff": meteor_staff}
INSANE_SMALL = {"airstrike_beacon": airstrike_beacon, "nuke_barrage": nuke_barrage, "city_capsule": city_capsule,
                "overdrive_stim": overdrive_stim}


def tectonic_bomb():
    m = [B(1, 0, 1, 15, 1, 15, "gunmetal")]
    m += [B(7, -4, 7, 9, 2, 9, "chrome")]
    m += cyl_y(8, 8, 1, 10, 4.5, "yellow")
    m += [B(3.3, 4, 3.3, 12.7, 5, 12.7, "hazard"), B(3.4, 7, 3.4, 12.6, 8, 12.6, "black")]
    m += [B(6, 10, 6, 10, 14, 10, "gunmetal"), B(6.5, 14, 6.5, 9.5, 15, 9.5, "glow_orange")]
    for (x, z) in ((1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)):
        m += [B(x, 1, z, x + 1.5, 6, z + 1.5, "steel")]
    m += [B(3.6, 2, 7, 3.7, 6, 9, "vent"), B(12.3, 2, 7, 12.4, 6, 9, "vent")]
    return m


def bass_drop_bomb():
    m = [B(1, 0, 1, 15, 15, 15, "carbon")]
    m += [B(2, 2, 0.9, 14, 14, 1, "speaker"), B(2, 2, 15, 14, 14, 15.1, "speaker"),
          B(0.9, 2, 2, 1, 14, 14, "speaker"), B(15, 2, 2, 15.1, 14, 14, "speaker")]
    edges = [(0.6, 0.6), (14.6, 0.6), (0.6, 14.6), (14.6, 14.6)]
    for i, (x, z) in enumerate(edges):
        m += [B(x, 0, z, x + 0.8, 15, z + 0.8, RAINBOW[i * 2 % 7])]
    m += [B(1, 15, 1, 15, 15.6, 15, "glow_magenta"), B(5, 15.6, 5, 11, 16, 11, "glow_cyan")]
    return m


def cluster_nuke():
    m = [B(1, 0, 1, 15, 3, 15, "olive"), B(0.9, 1, 0.9, 15.1, 2, 15.1, "hazard")]
    for (x, z) in ((4, 4), (12, 4), (4, 12), (12, 12)):
        m += cyl_y(x, z, 3, 11, 2.4, "olive")
        m += [B(x - 1.6, 11, z - 1.6, x + 1.6, 13, z + 1.6, "fatman"), B(x - 0.8, 13, z - 0.8, x + 0.8, 14.5, z + 0.8, "red")]
    m += [B(6.5, 3, 6.5, 9.5, 9, 9.5, "gunmetal"), B(7, 9, 7, 9, 10, 9, "glow_red")]
    return m


def nova_bomb():
    m = [B(1, 0, 1, 15, 2, 15, "gunmetal"), B(1, 14, 1, 15, 15.5, 15, "gunmetal")]
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m += [B(x, 2, z, x + 1.5, 14, z + 1.5, "fatman")]
    m += ball(8, 8, 8, 3.6, "glow_orange")
    m += [B(3.5, 3, 3.5, 12.5, 13, 12.5, "glass_cyan")]
    m += [B(2.5, 7.6, 2.5, 13.5, 8.4, 13.5, "glow_white"), B(7.6, 2.5, 2.5, 8.4, 13.5, 13.5, "glow_white")]
    return m


def omega_bomb():
    m = [B(0, 0, 0, 16, 2, 16, "hazard"), B(2, 2, 2, 14, 16, 14, "black")]
    for y in (4, 8, 12):
        m += [B(1.8, y, 1.8, 14.2, y + 0.6, 14.2, "glow_red")]
    m += [B(7.5, 2, 1.9, 8.5, 16, 2, "glow_red"), B(7.5, 2, 14, 8.5, 16, 14.1, "glow_red"),
          B(1.9, 2, 7.5, 2, 16, 8.5, "glow_red"), B(14, 2, 7.5, 14.1, 16, 8.5, "glow_red")]
    m += [B(5, 16, 5, 11, 16.5, 11, "glow_white"), B(5.5, 6, 1.8, 10.5, 11, 1.9, "radiation")]
    return m


INSANE_BOMBS = {"tectonic_bomb": tectonic_bomb, "bass_drop_bomb": bass_drop_bomb, "cluster_nuke": cluster_nuke,
                "nova_bomb": nova_bomb, "omega_bomb": omega_bomb}


def entity_meteor():
    m = ball(8, 8, 8, 4.5, "rock")
    m += [B(3.6, 6, 6, 3.7, 10, 9, "glow_orange"), B(12.3, 7, 5, 12.4, 9, 11, "glow_orange"),
          B(6, 12.3, 6, 10, 12.4, 8, "glow_orange"), B(7, 3.6, 7, 9, 3.7, 11, "glow_orange")]
    return m


def entity_rod():
    m = cyl_x(8, 8, -6, 22, 1.1, "chrome")
    m += [B(22, 7.3, 7.3, 24, 8.7, 8.7, "copper")]
    m += [B(-6, 5.5, 7.6, -2, 10.5, 8.4, "gunmetal"), B(-6, 7.6, 5.5, -2, 8.4, 10.5, "gunmetal")]
    return m


ENTITIES["meteor"] = entity_meteor
ENTITIES["rod"] = entity_rod
GUNS.update(INSANE_GUNS)
MELEE.update(INSANE_MELEE)
SMALL.update(INSANE_SMALL)
BOMBS.update(INSANE_BOMBS)


# =========================================================================== arsenal expansion
import models_arsenal as _ARSENAL  # noqa: E402

GLOW |= _ARSENAL.GLOW
GUNS.update(_ARSENAL.GUNS)
MELEE.update(_ARSENAL.MELEE)
SMALL.update(_ARSENAL.SMALL)
BOMBS.update(_ARSENAL.BOMBS)


# =========================================================================== detailing passes
# Applied on top of every model: panel seams, rivets, glowing inlays, rails and emitters in each
# model's own accent colour, so the whole arsenal shares one finished, high-tech look.

def _is_glow(mat):
    return mat in GLOW or mat.startswith("glow_") or mat == "celestial"


def _dims(b):
    f, t = b["from"], b["to"]
    return [abs(t[i] - f[i]) for i in range(3)]


def _vol(b):
    d = _dims(b)
    return d[0] * d[1] * d[2]


def _bounds(b):
    f, t = b["from"], b["to"]
    return [min(f[i], t[i]) for i in range(3)], [max(f[i], t[i]) for i in range(3)]


def accent_of(boxes, default="glow_cyan"):
    score = {}
    for b in boxes:
        if _is_glow(b["mat"]) and b["mat"] not in ("glow_white",):
            score[b["mat"]] = score.get(b["mat"], 0) + _vol(b) + 0.5
    return max(score, key=score.get) if score else default


def _overlaps(lo, hi, blo, bhi, axes=(0, 1, 2)):
    return all(lo[i] < bhi[i] and blo[i] < hi[i] for i in axes)


def _free(boxes, lo, hi, skip):
    for b in boxes:
        if b is skip:
            continue
        blo, bhi = _bounds(b)
        if _overlaps(lo, hi, blo, bhi):
            return False
    return True


def detail_side_panels(boxes, body, accent, axis=0, seams=True, inlay=True, rivet=True, long_axis=2):
    """Decorate the two faces of `body` normal to `axis`: seam lines, an inlay strip, corner rivets."""
    lo, hi = _bounds(body)
    up = 1 if axis != 1 else 0
    out = []
    la = long_axis
    length = hi[la] - lo[la]
    height = hi[up] - lo[up]
    if length < 3 or height < 1.6:
        return out
    for side, face in ((0, lo[axis]), (1, hi[axis])):
        def box(a0, a1, u0, u1, l0, l1, mat):
            f = [0, 0, 0]; t = [0, 0, 0]
            f[axis], t[axis] = (a0, a1)
            f[up], t[up] = (u0, u1)
            f[la], t[la] = (l0, l1)
            return B(f[0], f[1], f[2], t[0], t[1], t[2], mat)
        a0, a1 = (face - 0.12, face) if side == 0 else (face, face + 0.12)
        b0, b1 = (face - 0.06, face) if side == 0 else (face, face + 0.06)
        probe_lo = [0, 0, 0]; probe_hi = [0, 0, 0]
        probe_lo[axis], probe_hi[axis] = (face - 0.3, face) if side == 0 else (face, face + 0.3)
        probe_lo[up], probe_hi[up] = lo[up] + 0.2, hi[up] - 0.2
        probe_lo[la], probe_hi[la] = lo[la] + 0.5, hi[la] - 0.5
        if not _free(boxes, probe_lo, probe_hi, body):
            continue
        mid = (lo[up] + hi[up]) / 2
        if inlay and height >= 2.2:
            gap = length * 0.5
            out.append(box(a0, a1, mid - 0.28, mid + 0.28, lo[la] + 1.0, lo[la] + gap - 0.6, accent))
            out.append(box(a0, a1, mid - 0.28, mid + 0.28, lo[la] + gap + 0.6, hi[la] - 1.0, accent))
            out.append(box(a0, a1, mid - 0.7, mid + 0.7, lo[la] + gap - 0.35, lo[la] + gap + 0.35, "glow_white"))
        if seams:
            for k in (1, 2):
                z = lo[la] + length * k / 3
                out.append(box(b0, b1, lo[up] + 0.35, hi[up] - 0.35, z - 0.12, z + 0.12, "black"))
            out.append(box(b0, b1, hi[up] - 0.55, hi[up] - 0.35, lo[la] + 0.35, hi[la] - 0.35, "black"))
        if rivet and height >= 2:
            for u in (lo[up] + 0.45, hi[up] - 0.95):
                for l in (lo[la] + 0.45, hi[la] - 0.95):
                    out.append(box(a0 - 0.05 if side == 0 else a0, a1 + 0.05 if side == 1 else a1, u, u + 0.5, l, l + 0.5, "chrome"))
    return out


def gun_details(boxes, grip_pt, accent=None):
    acc = accent or accent_of(boxes)
    gz = grip_pt[2]
    cand = [b for b in boxes if not _is_glow(b["mat"]) and not b.get("rot") and b["mat"] not in ("rubber",)
            and (_bounds(b)[0][2] + _bounds(b)[1][2]) / 2 < gz + 3 and _dims(b)[2] >= 4]
    if not cand:
        return []
    body = max(cand, key=_vol)
    lo, hi = _bounds(body)
    out = detail_side_panels(boxes, body, acc, axis=0)
    # picatinny rail on top, if nothing sits there
    rlo = [7.1, hi[1], lo[2] + 1]; rhi = [8.9, hi[1] + 1.2, hi[2] - 1]
    if rhi[2] - rlo[2] >= 4 and _free(boxes, rlo, rhi, body):
        out.append(B(7.2, hi[1], lo[2] + 1, 8.8, hi[1] + 0.4, hi[2] - 1, "gunmetal"))
        z = lo[2] + 1.3
        while z + 0.6 <= hi[2] - 1.2:
            out.append(B(6.95, hi[1] + 0.4, z, 9.05, hi[1] + 0.8, z + 0.6, "steel"))
            z += 1.3
        out.append(B(7.6, hi[1] + 0.4, lo[2] + 0.4, 8.4, hi[1] + 1.4, lo[2] + 1.0, acc))
    # second-largest forward part (barrel / shroud) gets seams + inlay along its sides too
    rest = [b for b in cand if b is not body and _dims(b)[2] >= 5 and _dims(b)[1] >= 2]
    if rest:
        out += detail_side_panels(boxes, max(rest, key=_vol), acc, axis=0, rivet=False)
    # energy coils around long, thin barrels ahead of the body
    barrels = [b for b in cand if b is not body and _dims(b)[0] <= 3.4 and _dims(b)[1] <= 3.4 and _dims(b)[2] >= 4
               and _bounds(b)[0][2] < lo[2] + 1]
    for b in sorted(barrels, key=lambda b: -_dims(b)[2])[:1]:
        blo, bhi = _bounds(b)
        n = min(4, int((bhi[2] - blo[2]) / 2.6))
        for k in range(n):
            z = blo[2] + (bhi[2] - blo[2]) * (k + 0.6) / (n + 0.4)
            ring_lo = [blo[0] - 0.35, blo[1] - 0.35, z]; ring_hi = [bhi[0] + 0.35, bhi[1] + 0.35, z + 0.55]
            mat = acc if k % 2 == 0 else "steel"
            out.append(B(ring_lo[0], ring_lo[1], z, ring_hi[0], ring_hi[1], z + 0.55, mat))
    # muzzle emitter
    allb = [b for b in boxes if not b.get("rot")]
    front = min(allb, key=lambda b: _bounds(b)[0][2])
    flo, fhi = _bounds(front)
    near_glow = any(_is_glow(b["mat"]) and _bounds(b)[0][2] < flo[2] + 0.8 for b in boxes)
    if not near_glow and fhi[0] - flo[0] >= 1 and fhi[1] - flo[1] >= 1:
        cx, cy = (flo[0] + fhi[0]) / 2, (flo[1] + fhi[1]) / 2
        w, h = (fhi[0] - flo[0]) * 0.32, (fhi[1] - flo[1]) * 0.32
        out.append(B(cx - w, cy - h, flo[2] - 0.15, cx + w, cy + h, flo[2], acc))
        out.append(B(cx - w * 0.45, cy - h * 0.45, flo[2] - 0.25, cx + w * 0.45, cy + h * 0.45, flo[2] - 0.15, "glow_white"))
    # trigger guard
    out.append(B(7.4, grip_pt[1] + 0.4, gz - 4.2, 8.6, grip_pt[1] + 0.9, gz - 1.6, "gunmetal"))
    out.append(B(7.4, grip_pt[1] + 0.4, gz - 4.2, 8.6, grip_pt[1] + 2.6, gz - 3.7, "gunmetal"))
    out.append(B(7.75, grip_pt[1] + 1.0, gz - 2.9, 8.25, grip_pt[1] + 2.4, gz - 2.4, acc))
    return out


def _wrap_gun(fn):
    def wrapped():
        boxes, grip_pt = fn()
        return boxes + gun_details(boxes, grip_pt), grip_pt
    wrapped.__name__ = fn.__name__
    return wrapped


GUNS = {k: _wrap_gun(v) for k, v in GUNS.items()}


def bomb_details(boxes, accent=None):
    acc = accent or accent_of(boxes, "glow_red")
    out = []
    bases = [b for b in boxes if _bounds(b)[0][1] <= 0.01 and _dims(b)[0] >= 10 and _dims(b)[2] >= 10 and _dims(b)[1] <= 4]
    if not bases:
        return out
    base = max(bases, key=lambda b: _dims(b)[0] * _dims(b)[2])
    lo, hi = _bounds(base)
    top = hi[1]
    h = top - lo[1]
    # glowing seam around the plinth and a dark bottom trim
    if h >= 1.2:
        y0, y1 = top - min(0.75, h * 0.45), top - min(0.45, h * 0.3)
        out += [B(lo[0] - 0.1, y0, lo[2] + 0.6, lo[0], y1, hi[2] - 0.6, acc), B(hi[0], y0, lo[2] + 0.6, hi[0] + 0.1, y1, hi[2] - 0.6, acc),
                B(lo[0] + 0.6, y0, lo[2] - 0.1, hi[0] - 0.6, y1, lo[2], acc), B(lo[0] + 0.6, y0, hi[2], hi[0] - 0.6, y1, hi[2] + 0.1, acc)]
        out += [B(lo[0] - 0.06, 0, lo[2] - 0.06, hi[0] + 0.06, 0.35, hi[2] + 0.06, "black")]
    # corner bolts and small status lights on the plinth's top corners
    for (x, z, sx, sz) in ((lo[0] + 0.5, lo[2] + 0.5, 1, 1), (hi[0] - 1.3, lo[2] + 0.5, -1, 1), (lo[0] + 0.5, hi[2] - 1.3, 1, -1), (hi[0] - 1.3, hi[2] - 1.3, -1, -1)):
        if _free(boxes, [x, top, z], [x + 0.8, top + 0.5, z + 0.8], base):
            out.append(B(x, top, z, x + 0.8, top + 0.45, z + 0.8, "chrome"))
            out.append(B(x + 0.2, top + 0.45, z + 0.2, x + 0.6, top + 0.6, z + 0.6, acc))
    # mid-edge vents where the plinth face is clear
    if h >= 1.6:
        for (x0, x1, z0, z1) in ((6, 10, lo[2] - 0.08, lo[2]), (6, 10, hi[2], hi[2] + 0.08)):
            out.append(B(x0, lo[1] + 0.45, z0, x1, top - 0.85, z1, "vent"))
    return out


def _wrap_bomb(fn):
    def wrapped():
        boxes = fn()
        return boxes + bomb_details(boxes)
    wrapped.__name__ = fn.__name__
    return wrapped


BOMBS = {k: _wrap_bomb(v) for k, v in BOMBS.items()}


# Hand-placed extras for small items whose base shapes read as too plain.
SMALL_EXTRA = {
    "hologram_bomb": lambda: [B(4.6, 0, 4.6, 11.4, 0.6, 11.4, "steel"), B(5.2, 2.2, 4.95, 10.8, 2.8, 5.05, "glow_cyan"),
                              B(5.2, 2.2, 10.95, 10.8, 2.8, 11.05, "glow_cyan"), B(7.2, 4, 7.2, 8.8, 14, 8.8, "glow_cyan"),
                              B(5.5, 6, 5.5, 10.5, 6.3, 10.5, "glow_cyan", ("y", 45, [8, 6.15, 8])), B(6, 9, 6, 10, 9.3, 10, "glow_cyan", ("y", 45, [8, 9.15, 8])),
                              B(6.6, 12, 6.6, 9.4, 12.3, 9.4, "glow_white", ("y", 45, [8, 12.15, 8]))],
    "slimequake_bomb": lambda: [B(4.4, 9.6, 4.4, 5.6, 10.8, 5.6, "glass_slime"), B(10.6, 8, 10.4, 11.4, 8.8, 11.2, "glow_lime"),
                                B(3.8, 3.8, 9.5, 4.2, 4.6, 10.3, "glow_lime"), B(11.8, 2.2, 5.5, 12.2, 3.2, 6.5, "glass_slime"),
                                B(3.8, 2, 3.8, 12.2, 2.6, 12.2, "gunmetal"), B(6.4, 11.5, 6.4, 9.6, 12, 9.6, "glow_red")],
    "snow_globe_grenade": lambda: [B(4.2, 1.6, 4.2, 11.8, 2.2, 11.8, "glow_ice"), B(5.5, 5, 7.3, 6.0, 5.5, 7.8, "white"), B(9.6, 8.2, 8.6, 10.1, 8.7, 9.1, "white"),
                                   B(7.2, 9.6, 6.0, 7.7, 10.1, 6.5, "white"), B(6.4, 3.5, 6.4, 9.6, 7.5, 9.6, "glow_ice", ("y", 45, [8, 5.5, 8])),
                                   B(7.5, 7.5, 7.5, 8.5, 8.7, 8.5, "glow_white", ("y", 45, [8, 8, 8])), B(4.1, 0.5, 7.4, 4.5, 1.5, 8.6, "glow_ice")],
    "mirror_dimension_bomb": lambda: [B(4.2, 13.5, 7.4, 11.8, 14.2, 8.6, "gold"), B(7.4, 14.2, 7.4, 8.6, 15.2, 8.6, "glow_white", ("y", 45, [8, 14.7, 8])),
                                      B(5.8, 9.5, 7.75, 7, 12, 7.85, "glow_white"), B(6.4, 4, 8.15, 7.2, 6, 8.25, "glow_white"),
                                      B(4.4, 2, 7.2, 4.6, 13.5, 8.8, "glow_cyan"), B(11.4, 2, 7.2, 11.6, 13.5, 8.8, "glow_cyan")],
    "micro_event_horizon": lambda: [B(x0, 6.85, z0, x1, 7.15, z1, "glow_red", ("y", 22.5, [8, 7, 8])) for (x0, z0, x1, z1) in
                                    ((1.5, 1.5, 14.5, 2.1), (1.5, 13.9, 14.5, 14.5), (1.5, 1.5, 2.1, 14.5), (13.9, 1.5, 14.5, 14.5))] + [B(7.6, 2, 7.6, 8.4, 12, 8.4, "glow_white"),
                                    B(6.4, 6.4, 6.4, 9.6, 7.6, 9.6, "glow_void")],
    "shatterfrost_bomb": lambda: [B(7.6, 0.5, 7.6, 8.4, 13.5, 8.4, "glow_white"), B(6.8, 6.2, 6.8, 9.2, 7.8, 9.2, "glow_white", ("y", 45, [8, 7, 8]))],
    "rainbow_paint_bomb": lambda: [B(6.2, 11.8, 6.2, 9.8, 12.4, 9.8, "chrome"), B(7.6, 12.4, 7.6, 8.4, 13.4, 8.4, "glow_rainbow"),
                                   B(4, 3.4, 4, 12, 3.8, 12, "gunmetal"), B(4, 10.2, 4, 12, 10.6, 12, "gunmetal")],
    "arcflash_charge": lambda: [B(5.2, 11.6, 5.2, 6.8, 13, 6.8, "glow_white", ("y", 45, [6, 12.3, 6])), B(9.2, 11.6, 9.2, 10.8, 13, 10.8, "glow_white", ("y", 45, [10, 12.3, 10])),
                                B(6.6, 11.5, 7.6, 9.4, 11.9, 8.4, "glow_blue", ("y", 45, [8, 11.7, 8])), B(4.2, 0, 4.2, 11.8, 0.5, 11.8, "black"),
                                B(5.4, 1, 3.9, 10.6, 1.6, 4.0, "hazard"), B(5.4, 1, 12.0, 10.6, 1.6, 12.1, "hazard")],
    "crystal_cataclysm": lambda: [B(7.4, 2, 7.4, 8.6, 12.5, 8.6, "glow_purple", ("y", 45, [8, 7, 8])), B(5.5, 0, 5.5, 10.5, 1, 10.5, "obsidian"),
                                  B(9.5, 1, 4.0, 11, 5, 5.5, "amethyst", ("x", -22.5, [10.25, 1, 4.75])), B(10, 5, 4.5, 10.5, 5.6, 5.0, "glow_purple")],
    "plasma_cell": lambda: [B(5.3, 6, 5.3, 10.7, 6.5, 10.7, "gunmetal"), B(5.3, 9.5, 5.3, 10.7, 10, 10.7, "gunmetal"),
                            B(7.6, 4.5, 7.6, 8.4, 11.5, 8.4, "glow_white"), B(7.3, 13.5, 7.3, 8.7, 14.3, 8.7, "glow_cyan")],
    "railgun_slug": lambda: [B(4.5 + dx, 4 + k * 3, 6.9, 6.1 + dx + 0.0, 4.6 + k * 3, 9.1, "glow_blue") for dx in (0, 2.5, 5) for k in range(3)]
    + [B(4.3, 1.4, 6.8, 11.7, 2.2, 9.2, "gunmetal")],
}


def _wrap_small(name, fn):
    def wrapped():
        out = fn()
        boxes = out[0] if isinstance(out, tuple) else out
        extra = SMALL_EXTRA[name]() if name in SMALL_EXTRA else []
        return (boxes + extra, out[1]) if isinstance(out, tuple) else boxes + extra
    return wrapped


SMALL = {k: _wrap_small(k, v) for k, v in SMALL.items()}
