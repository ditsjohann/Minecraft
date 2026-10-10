"""3D cuboid model definitions (Blockbench-style JSON, written procedurally).

Coordinates are in model units (16 = one block). Guns point along -Z (muzzle at low Z),
melee weapons and tools point along +Y, projectile entity models point along +X.
"""
import math

GLOW = {"glow_cyan", "glow_green", "glow_orange", "glow_purple", "glow_red", "glow_blue",
        "glow_magenta", "glow_white", "screen", "screen_red"}


def B(x1, y1, z1, x2, y2, z2, mat, rot=None):
    return {"from": [x1, y1, z1], "to": [x2, y2, z2], "mat": mat, "rot": rot}


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


def gravity_hammer():
    m = handle(-6, 16)
    m += [B(6.5, -8, 6.5, 9.5, -6, 9.5, "gunmetal"), B(7.3, -8.6, 7.3, 8.7, -8, 8.7, "glow_purple")]
    m += [B(2, 16, 5, 14, 24, 11, "gunmetal")]
    m += [B(6, 15.5, 4.5, 10, 24.5, 11.5, "steel")]
    m += [B(1, 17, 6, 2, 23, 10, "glow_purple"), B(14, 17, 6, 15, 23, 10, "glow_purple")]
    m += [B(2.5, 24, 6, 13.5, 24.6, 10, "hazard")]
    m += [B(6.5, 18, 4.3, 9.5, 22, 4.5, "glow_purple"), B(6.5, 18, 11.5, 9.5, 22, 11.7, "glow_purple")]
    return m, (8, -2, 8)


def plasma_blade():
    m = [B(7, -4, 7, 9, 4, 9, "rubber"), B(6.8, -6, 6.8, 9.2, -4, 9.2, "gunmetal"),
         B(7.4, -6.6, 7.4, 8.6, -6, 8.6, "glow_magenta")]
    m += [B(4, 4, 6.5, 12, 6, 9.5, "chrome"), B(3.5, 4.5, 7, 4, 5.5, 9, "glow_magenta"), B(12, 4.5, 7, 12.5, 5.5, 9, "glow_magenta")]
    m += [B(6.5, 6, 7, 9.5, 8, 9, "gunmetal")]
    m += [B(7.25, 8, 7.6, 8.75, 30, 8.4, "glow_magenta")]
    m += [B(6.75, 8, 7.3, 9.25, 30.5, 8.7, "glass_magenta")]
    m += [B(7.6, 8, 7.8, 8.4, 31, 8.2, "glow_white")]
    return m, (8, 0, 8)


def storm_caller():
    m = [B(7.3, -8, 7.3, 8.7, 18, 8.7, "copper")]
    for y in (-6, 0, 6, 12):
        m += [B(7, y, 7, 9, y + 1, 9, "gunmetal")]
    m += [B(6, 18, 6, 10, 21, 10, "gunmetal")]
    m += [B(5, 19, 7.5, 6, 25, 8.5, "copper"), B(10, 19, 7.5, 11, 25, 8.5, "copper"),
          B(7.5, 19, 5, 8.5, 25, 6, "copper"), B(7.5, 19, 10, 8.5, 25, 11, "copper")]
    m += [B(6.5, 21, 6.5, 9.5, 27, 9.5, "glow_blue", ("y", 45, [8, 24, 8]))]
    m += [B(7.2, 27, 7.2, 8.8, 29, 8.8, "glow_white", ("y", 45, [8, 28, 8]))]
    return m, (8, 2, 8)


def atomic_pickaxe():
    m = handle(-6, 15)
    m += [B(-1, 14, 7, 17, 17, 9, "titanium")]
    m += [B(-3, 14.5, 7.3, -1, 16.5, 8.7, "steel"), B(-4, 15, 7.5, -3, 16, 8.5, "steel")]
    m += [B(17, 14.5, 7.3, 19, 16.5, 8.7, "steel"), B(19, 15, 7.5, 20, 16, 8.5, "steel")]
    m += [B(6, 13, 6, 10, 18, 10, "gunmetal")]
    m += [B(6.5, 13.5, 5.9, 9.5, 17.5, 6, "radiation"), B(6.5, 13.5, 10, 9.5, 17.5, 10.1, "radiation")]
    m += [B(6.8, 18, 6.8, 9.2, 19, 9.2, "glow_green")]
    return m, (8, -2, 8)


def fusion_shovel():
    m = handle(-6, 14)
    m += [B(6.5, 13, 6.5, 9.5, 15, 9.5, "gunmetal")]
    m += [B(4, 15, 7.4, 12, 24, 8.6, "titanium"), B(5, 24, 7.4, 11, 25.5, 8.6, "titanium")]
    m += [B(5, 25.5, 7.6, 11, 26, 8.4, "glow_orange")]
    m += [B(7, 16, 7.2, 9, 22, 8.8, "glow_orange")]
    return m, (8, -2, 8)


def plasma_cutter():
    m = handle(-6, 20)
    m += [B(6.5, 13, 6.5, 9.5, 20, 9.5, "gunmetal")]
    m += [B(9.5, 13, 7.4, 15, 21, 8.6, "titanium")]
    m += [B(15, 12, 7.6, 16.5, 22, 8.4, "glow_cyan")]
    m += [B(4, 15, 7.4, 6.5, 18, 8.6, "gunmetal")]
    m += [B(10.5, 15, 7.3, 13.5, 19, 7.4, "glow_cyan"), B(10.5, 15, 8.6, 13.5, 19, 8.7, "glow_cyan")]
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
    m += [B(5.4, 6, 5.4, 10.6, 7.5, 10.6, band)]
    if glow:
        m += [B(7, 8, 5.5, 9, 11, 5.6, glow)]
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
            B(6, 12, 6, 10, 13.5, 10, "white"), B(7.6, 0, 7.6, 8.4, 2, 8.4, "chrome"), B(7.5, 13.5, 7.5, 8.5, 15, 8.5, "white")]


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
            B(2.5, 1, 12.5, 4, 4, 13.5, "steel"), B(12, 1, 12.5, 13.5, 4, 13.5, "steel")]


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
    return m, (8, 3, 9.5)


def meteor_staff():
    m = [B(7.3, -8, 7.3, 8.7, 18, 8.7, "black")]
    for y in (-6, 2, 10):
        m += [B(7, y, 7, 9, y + 1, 9, "glow_orange")]
    m += [B(5.5, 17, 7.5, 6.5, 22, 8.5, "black"), B(9.5, 17, 7.5, 10.5, 22, 8.5, "black"),
          B(7.5, 17, 5.5, 8.5, 22, 6.5, "black"), B(7.5, 17, 9.5, 8.5, 22, 10.5, "black")]
    m += ball(8, 24, 8, 3.2, "rock")
    m += [B(6, 22, 5.6, 7, 26, 5.7, "glow_orange"), B(9, 23, 10.3, 10, 26, 10.4, "glow_orange"), B(10.3, 22, 7, 10.4, 25, 9, "glow_orange")]
    m += [B(3.5, 23.6, 3.5, 12.5, 24.4, 12.5, "glow_orange", ("y", 45, [8, 24, 8]))]
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
    return m


def overdrive_stim():
    return [B(7, 2, 7, 9, 12, 9, "glass_magenta"), B(7.3, 3, 7.3, 8.7, 11, 8.7, "glow_magenta"),
            B(6, 12, 6, 10, 13.5, 10, "gunmetal"), B(7.6, 0, 7.6, 8.4, 2, 8.4, "chrome"), B(7.5, 13.5, 7.5, 8.5, 15.5, 8.5, "glow_red")]


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
