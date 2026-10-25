"""3D models for the arsenal expansion (tiers Uncommon → Celestial). Merged into models.py."""
from models import B, ball, cyl_x, cyl_y, cyl_z, grip, handle, cradle, grip_wraps, pommel, collar

GLOW = {"glow_gold", "glow_crimson", "glow_void", "celestial", "glow_rainbow", "glow_pink", "glow_lime", "glow_ice"}
RB = ["glow_red", "glow_orange", "glow_gold", "glow_green", "glow_cyan", "glow_blue", "glow_purple", "glow_magenta"]


# =========================================================================== guns (-Z forward)

def party_popper():
    m = cyl_z(8, 11, -4, 12, 3.6, "pink")
    m += [B(4.2, 7.2, -5, 11.8, 14.8, -4, "glow_rainbow")]
    for i, z in enumerate((-2, 2, 6, 10)):
        m += [B(4.3, 7.3, z, 11.7, 14.7, z + 1, RB[i * 2])]
    m += [B(6.5, 14.4, 0, 9.5, 16, 8, "gold"), B(7, 16, 3, 9, 17, 5, "glow_gold")]
    m += [B(6.5, 7, 12, 9.5, 12, 18, "white"), grip(8, 8, 14)]
    return m, (8, 4, 14)


def airhorn_cannon():
    m = [B(6, 8, 10, 10, 13, 20, "red"), B(5.5, 13, 12, 10.5, 16, 17, "white")]
    m += cyl_z(8, 10.5, 2, 10, 1.6, "chrome")
    m += [B(4, 6.5, -5, 12, 14.5, -3.5, "yellow"), B(4.8, 7.3, -3.5, 11.2, 13.7, -1, "yellow"),
          B(5.8, 8.3, -1, 10.2, 12.7, 2, "yellow"), B(5, 7.5, -5.2, 11, 13.5, -5, "black")]
    m += [B(7, 16, 13, 9, 17.5, 16, "glow_gold"), grip(8, 8, 17)]
    return m, (8, 4, 17)


def gravity_tether_gun():
    m = [B(6, 8, 4, 10, 12, 20, "gunmetal")]
    m += ring_y_frame(8, 10, -2, 4.0, "glow_blue") + ring_y_frame(8, 10, 1.5, 3.0, "glow_purple")
    m += cyl_z(8, 10, -4, 4, 1.0, "chrome")
    m += [B(7, 9, -5, 9, 11, -4, "glow_white"), B(10, 9, 8, 12, 13, 16, "steel"), B(12, 10, 9, 12.1, 12, 15, "screen")]
    m += [grip(8, 8, 16)]
    return m, (8, 4, 16)


def ring_y_frame(cx, cy, z, r, mat, t=0.7):
    return [B(cx - r, cy + r - t, z, cx + r, cy + r, z + t, mat), B(cx - r, cy - r, z, cx + r, cy - r + t, z + t, mat),
            B(cx - r, cy - r, z, cx - r + t, cy + r, z + t, mat), B(cx + r - t, cy - r, z, cx + r, cy + r, z + t, mat)]


def antimatter_lance():
    m = cyl_z(8, 10, -10, 22, 1.5, "pearl")
    for z in (-8, -3, 2):
        m += ring_y_frame(8, 10, z, 2.8, "glow_magenta", 0.6)
    m += [B(6.2, 8.2, 5, 9.8, 11.8, 16, "obsidian"), B(5.9, 9.4, 7, 6.2, 10.6, 14, "glow_purple"),
          B(9.8, 9.4, 7, 10.1, 10.6, 14, "glow_purple"), B(7.3, 9.3, -11, 8.7, 10.7, -10, "glow_white")]
    m += [B(6.5, 6, 18, 9.5, 11, 25, "obsidian"), grip(8, 8, 15)]
    return m, (8, 4, 15)


def cataclysm_mortar():
    m = cyl_z(8, 12, -4, 14, 4.2, "obsidian")
    m += [B(3.6, 7.6, -5, 12.4, 16.4, -4, "glow_purple"), B(3.5, 7.5, 4, 12.5, 16.5, 5, "glow_purple")]
    m += [B(5.5, 8, 14, 10.5, 15, 20, "gunmetal"), B(6, 15, 15, 10, 16.5, 19, "vent")]
    m += [B(4, 6, 6, 12, 8, 12, "gunmetal"), grip(8, 6, 16)]
    return m, (8, 2, 16)


def rift_borer():
    m = [B(5.5, 7.5, 6, 10.5, 13.5, 20, "obsidian")]
    m += cyl_z(8, 10.5, -2, 6, 2.6, "gunmetal")
    m += [B(6.5, 9, -5, 9.5, 12, -2, "steel", ("z", 45, [8, 10.5, -3])), B(7.2, 9.7, -8, 8.8, 11.3, -5, "glow_void")]
    m += [B(5.3, 10, 8, 5.5, 12, 18, "glow_void"), B(10.5, 10, 8, 10.7, 12, 18, "glow_void")]
    m += [B(6.5, 13.5, 9, 9.5, 15, 16, "vent"), grip(8, 7.5, 16), grip(8, 7.5, 8, length=4)]
    return m, (8, 3.5, 16)


def resonance_cannon():
    m = [B(6, 8, 6, 10, 12, 20, "steel")]
    for i, z in enumerate((-4, -1, 2)):
        r = 2.0 + i * 1.2
        m += ring_y_frame(8, 10, z, r, "glow_ice" if i % 2 == 0 else "copper", 0.8)
    m += cyl_z(8, 10, -4, 6, 1.0, "glass_cyan")
    m += [B(7, 12, 8, 9, 15, 12, "amethyst"), B(7.5, 15, 9, 8.5, 16, 11, "glow_purple"), grip(8, 8, 17)]
    return m, (8, 4, 17)


def solstice_sunlance():
    m = cyl_z(8, 10, -12, 20, 1.3, "gold")
    m += [B(5, 7, -3, 11, 13, 6, "white"), B(4.8, 9.5, -2, 5, 10.5, 5, "glow_gold"), B(11, 9.5, -2, 11.2, 10.5, 5, "glow_gold")]
    for z, r in ((-12, 2.6), (-8, 2.0), (-4, 1.6)):
        m += ring_y_frame(8, 10, z, r, "glow_gold", 0.5)
    m += [B(7.4, 9.4, -13, 8.6, 10.6, -12, "glow_white"), B(6.5, 13, 0, 9.5, 15, 4, "glow_gold", ("z", 45, [8, 14, 2]))]
    m += [B(6.5, 7, 12, 9.5, 11, 22, "white"), grip(8, 8, 12)]
    return m, (8, 4, 12)


def midas_cascade():
    m = cyl_z(8, 11, -3, 16, 3.2, "gold")
    m += [B(4.6, 7.6, -4, 11.4, 14.4, -3, "glow_gold")]
    m += ball(8, 11, -5.5, 2.0, "glow_gold")
    m += [B(4.5, 7.5, 5, 11.5, 14.5, 6, "black"), B(4.5, 7.5, 10, 11.5, 14.5, 11, "black")]
    m += [B(6.5, 6, 16, 9.5, 11, 22, "gold"), grip(8, 7.5, 13)]
    m += [B(4.4, 7.4, 5.2, 11.6, 14.6, 5.8, "glow_gold"), B(4.4, 7.4, 10.2, 11.6, 14.6, 10.8, "glow_gold")]
    for z in (0, 2, 13):
        m += [B(4.6, 7.6, z, 11.4, 14.4, z + 0.7, "pearl")]
    m += [B(6.5, 14.2, 0, 9.5, 15.6, 14, "gold"), B(7.2, 15.6, 1, 8.8, 16.4, 13, "glow_gold")]
    for z in (-1.5, 3.5, 8.5):
        m += [B(7.6, 16.4, z, 8.4, 17.6, z + 0.8, "gold", ("z", 45, [8, 17, z]))]
    m += [B(7.2, 10.2, -6.9, 8.8, 11.8, -6.6, "glow_white")]
    return m, (8, 3.5, 13)


def judgment_engine():
    m = []
    for (dx, dy) in ((-1.8, 0), (1.8, 0), (0, 1.8), (0, -1.8), (-1.3, 1.3), (1.3, -1.3)):
        m += [B(7.4 + dx, 9.4 + dy, -8, 8.6 + dx, 10.6 + dy, 6, "gold")]
    m += [B(5, 7, -9, 11, 13, -8, "glow_gold"), B(5, 7, -1, 11, 13, 0, "white")]
    m += cyl_z(8, 10, 6, 16, 4.0, "white")
    m += [B(3.8, 9, 8, 4, 11, 14, "glow_orange"), B(12, 9, 8, 12.2, 11, 14, "glow_orange")]
    m += [B(6, 14, 7, 10, 16, 14, "gold"), B(6.5, 16, 8, 9.5, 17, 13, "glow_gold")]
    m += [grip(8, 6, 15), grip(8, 6, 9, length=5)]
    return m, (8, 2, 15)


def reality_shear_projector():
    m = [B(6, 7, 4, 10, 13, 20, "pearl")]
    m += [B(7.6, 4, -8, 8.4, 16, 4, "glow_white"), B(7.2, 4, -8, 8.8, 16, 4, "glass_cyan")]
    m += [B(6.5, 3, 2, 9.5, 17, 4, "chrome")]
    m += [B(5.8, 9, 8, 6, 11, 18, "glow_ice"), B(10, 9, 8, 10.2, 11, 18, "glow_ice"), grip(8, 7, 16)]
    return m, (8, 3, 16)


GUNS = {"party_popper": party_popper, "airhorn_cannon": airhorn_cannon, "gravity_tether_gun": gravity_tether_gun,
        "antimatter_lance": antimatter_lance, "cataclysm_mortar": cataclysm_mortar, "rift_borer": rift_borer,
        "resonance_cannon": resonance_cannon, "solstice_sunlance": solstice_sunlance, "midas_cascade": midas_cascade,
        "judgment_engine": judgment_engine, "reality_shear_projector": reality_shear_projector}


# =========================================================================== melee (+Y)

def worldsplitter_hammer():
    m = [B(7.2, -8, 7.2, 8.8, 16, 8.8, "gold")]
    m += grip_wraps(-7.5, 2, "black", "glow_gold")
    m += pommel(-8, "glow_gold", "gold")
    m += collar(4, 1, 1.4, "gold") + collar(9, 0.8, 1.3, "obsidian") + collar(13.5, 2.5, 1.8, "gold")
    m += [B(7.1, 10, 7.1, 8.9, 13.5, 8.9, "glow_gold")]
    # obsidian head with gold armour plates and a glowing fissure
    m += [B(0.5, 16, 4, 15.5, 26, 12, "obsidian")]
    m += [B(-1, 15.5, 3.6, 1, 26.5, 12.4, "gold"), B(15, 15.5, 3.6, 17, 26.5, 12.4, "gold")]
    m += [B(-1.6, 17, 5, -1, 25, 11, "glow_gold"), B(17, 17, 5, 17.6, 25, 11, "glow_gold")]
    m += [B(-1.7, 19.5, 7, -1.5, 22.5, 9, "glow_white"), B(17.5, 19.5, 7, 17.7, 22.5, 9, "glow_white")]
    m += [B(0.5, 25.5, 3.6, 15.5, 26.5, 4.4, "gold"), B(0.5, 25.5, 11.6, 15.5, 26.5, 12.4, "gold"),
          B(0.5, 15.5, 3.6, 15.5, 16.5, 4.4, "gold"), B(0.5, 15.5, 11.6, 15.5, 16.5, 12.4, "gold")]
    # jagged fissure across both faces
    for (x, y) in ((2, 20.5), (4, 21.5), (6, 20.2), (8, 21.3), (10, 20.1), (12, 21.2)):
        m += [B(x, y, 3.8, x + 2.2, y + 0.8, 4.0, "glow_gold"), B(x, y, 12, x + 2.2, y + 0.8, 12.2, "glow_gold")]
    m += [B(7.4, 16.5, 3.75, 8.6, 25.5, 3.8, "glow_gold"), B(7.4, 16.5, 12.2, 8.6, 25.5, 12.25, "glow_gold")]
    # crown
    m += [B(5.5, 26, 5.5, 10.5, 27.5, 10.5, "gold")]
    for (x, z) in ((5.5, 5.5), (9.5, 5.5), (5.5, 9.5), (9.5, 9.5)):
        m += [B(x, 27.5, z, x + 1, 29, z + 1, "gold")]
    m += [B(7.2, 27.5, 7.2, 8.8, 29.6, 8.8, "glow_gold", ("y", 45, [8, 28.5, 8]))]
    return m, (8, -3, 8)


def warden_wand():
    m = [B(7.45, -6, 7.45, 8.55, 18, 8.55, "pearl")]
    m += pommel(-6, "glow_lime", "gold")
    m += grip_wraps(-5.2, -0.5, "white", None, 1.0)
    for y in (0, 6, 12):
        m += [B(7.0, y, 7.0, 9.0, y + 1.2, 9.0, "gold"), B(7.2, y + 1.2, 7.2, 8.8, y + 1.8, 8.8, "glow_lime")]
    m += [B(7.6, 2, 7.4, 8.4, 17, 7.45, "glow_lime"), B(7.6, 2, 8.55, 8.4, 17, 8.6, "glow_lime")]
    m += [B(6.4, 17.5, 6.4, 9.6, 19, 9.6, "gold"), B(6.9, 19, 6.9, 9.1, 19.6, 9.1, "glow_lime")]
    # gold cage around a floating warden crystal
    for (x, z) in ((5.6, 5.6), (9.9, 5.6), (5.6, 9.9), (9.9, 9.9)):
        m += [B(x, 19, z, x + 0.5, 26, z + 0.5, "gold")]
    m += [B(5.6, 26, 5.6, 10.4, 26.6, 10.4, "gold"), B(7.6, 26.6, 7.6, 8.4, 28, 8.4, "gold")]
    m += [B(6.2, 20, 6.2, 9.8, 25.5, 9.8, "glass_cyan", ("y", 45, [8, 22.75, 8]))]
    m += [B(6.9, 21, 6.9, 9.1, 24.5, 9.1, "glow_lime", ("y", 45, [8, 22.75, 8]))]
    m += [B(7.5, 22, 7.5, 8.5, 23.5, 8.5, "glow_white", ("y", 45, [8, 22.75, 8]))]
    m += [B(7.4, 28, 7.4, 8.6, 29.2, 8.6, "glow_lime", ("y", 45, [8, 28.6, 8]))]
    return m, (8, 2, 8)


MELEE = {"worldsplitter_hammer": worldsplitter_hammer, "warden_wand": warden_wand}


# =========================================================================== throwables & devices

def orb_device(shell, core, band):
    m = ball(8, 7, 8, 3.4, shell)
    m += [B(4.4, 6.4, 4.4, 11.6, 7.6, 11.6, band)]
    m += [B(4.2, 6.75, 4.2, 11.8, 7.25, 11.8, core)]
    m += ball(8, 7, 8, 2.2, core)
    m += [B(7, 10.2, 7, 9, 11.4, 9, "gunmetal"), B(6.4, 11.4, 7.6, 9.6, 11.9, 8.4, "steel"), B(9.2, 10.8, 7.6, 9.7, 11.9, 8.4, "steel")]
    m += [B(7.4, 2.4, 7.4, 8.6, 3.6, 8.6, "gunmetal")]
    for (x, z) in ((4.0, 7.6), (11.5, 7.6), (7.6, 4.0), (7.6, 11.5)):
        m += [B(x, 6.6, z, x + 0.5, 7.4, z + 0.8, "chrome")]
    return m


def disc_mine(top, light, base="gunmetal"):
    m = [B(3, 0, 3, 13, 2, 13, base), B(4, 2, 4, 12, 3, 12, top)]
    m += [B(2.4, 0.4, 4.5, 13.6, 1.6, 11.5, base), B(4.5, 0.4, 2.4, 11.5, 1.6, 13.6, base)]
    m += [B(7, 3, 7, 9, 4, 9, light), B(2.0, 0.7, 7, 2.4, 1.3, 9, light), B(13.6, 0.7, 7, 14, 1.3, 9, light),
          B(7, 0.7, 2.0, 9, 1.3, 2.4, light), B(7, 0.7, 13.6, 9, 1.3, 14, light)]
    m += [B(6.2, 3, 6.2, 9.8, 3.4, 9.8, "gunmetal"), B(7.5, 4, 7.5, 8.5, 4.5, 8.5, "glow_white")]
    for (x, z) in ((4.2, 4.2), (11.2, 4.2), (4.2, 11.2), (11.2, 11.2)):
        m += [B(x, 2.8, z, x + 0.6, 3.3, z + 0.6, "chrome")]
    return m


def can(body, band, glow, cap="steel"):
    m = cyl_y(8, 8, 2, 12, 2.6, body)
    m += cyl_y(8, 8, 12, 13.5, 1.8, cap) + cyl_y(8, 8, 1, 2, 2.0, cap)
    m += cyl_y(8, 8, 1.6, 2.3, 2.8, "gunmetal") + cyl_y(8, 8, 11.6, 12.3, 2.8, "gunmetal")
    m += [B(5.2, 6, 5.2, 10.8, 7.5, 10.8, band), B(7, 8, 5.3, 9, 11, 5.4, glow), B(7, 8, 10.6, 9, 11, 10.7, glow)]
    m += [B(5.3, 8, 7, 5.4, 11, 9, glow), B(10.6, 8, 7, 10.7, 11, 9, glow), B(7.4, 8.4, 5.25, 8.6, 10.6, 5.3, "glow_white")]
    m += [B(7.4, 13.5, 7.4, 8.6, 14.5, 8.6, "chrome"), B(7.6, 14.5, 7.6, 8.4, 15, 8.4, glow)]
    return m


def snow_globe():
    m = [B(4.5, 0, 4.5, 11.5, 2, 11.5, "gold")]
    m += ball(8, 7, 8, 4.4, "glass_ice")
    m += [B(7, 2, 7, 9, 5, 9, "glow_white"), B(6, 3, 6, 10, 3.5, 10, "white")]
    return m


def slimequake():
    m = [B(4, 2, 4, 12, 10, 12, "glass_slime"), B(5.5, 3.5, 5.5, 10.5, 8.5, 10.5, "glow_lime")]
    m += [B(3.6, 5, 3.6, 12.4, 6, 12.4, "gunmetal"), B(7, 10, 7, 9, 11.5, 9, "red")]
    return m


def hologram_bomb():
    m = [B(5, 0, 5, 11, 3, 11, "gunmetal"), B(6, 3, 6, 10, 4, 10, "glow_cyan")]
    m += [B(6.5, 5, 7.5, 9.5, 11, 8.5, "glass_cyan"), B(7, 11.5, 7.2, 9, 13.5, 8.8, "glass_cyan")]
    return m


def magnet():
    m = [B(3, 2, 7, 6, 12, 9, "red"), B(10, 2, 7, 13, 12, 9, "red"), B(3, 12, 7, 13, 15, 9, "red")]
    m += [B(3, 0, 7, 6, 2, 9, "chrome"), B(10, 0, 7, 13, 2, 9, "chrome"), B(7, 13, 6.8, 9, 14, 9.2, "glow_gold")]
    for y in (4, 7, 10):
        m += [B(2.8, y, 6.8, 6.2, y + 0.6, 9.2, "copper"), B(9.8, y, 6.8, 13.2, y + 0.6, 9.2, "copper")]
    m += [B(3.4, 13, 6.85, 12.6, 14.6, 6.9, "white"), B(3.4, 13, 9.1, 12.6, 14.6, 9.15, "white")]
    m += [B(3.6, -0.3, 7.3, 5.4, 0, 8.7, "glow_red"), B(10.6, -0.3, 7.3, 12.4, 0, 8.7, "glow_blue")]
    for x in (4.5, 11.5):
        m += [B(x - 1.2, 0.6, 7.95, x + 1.2, 0.9, 8.05, "glow_white"), B(x - 0.9, 1.3, 7.95, x + 0.9, 1.6, 8.05, "glow_ice")]
    return m


def paint_bomb():
    m = ball(8, 7, 8, 3.6, "white")
    for i, mat in enumerate(RB[:6]):
        m += [B(4.2 + i * 1.3, 3.8, 4.2, 5.4 + i * 1.3, 10.2, 11.8, mat)]
    m += [B(7, 10.6, 7, 9, 12, 9, "black")]
    return m


def shatterfrost():
    m = ball(8, 7, 8, 3.0, "glass_ice")
    for rot in (0, 45):
        m += [B(7.4, 1.5, 7.4, 8.6, 12.5, 8.6, "glow_ice", ("y", rot, [8, 7, 8])),
              B(2.5, 6.4, 7.4, 13.5, 7.6, 8.6, "glow_ice", ("y", rot, [8, 7, 8]))]
    return m


def arcflash():
    m = [B(4, 0, 4, 12, 5, 12, "copper"), B(4.5, 5, 4.5, 11.5, 6, 11.5, "gunmetal")]
    m += [B(5.5, 6, 5.5, 6.5, 11, 6.5, "chrome"), B(9.5, 6, 9.5, 10.5, 11, 10.5, "chrome"),
          B(5, 11, 5, 7, 12, 7, "glow_blue"), B(9, 11, 9, 11, 12, 11, "glow_blue"), B(3.9, 2, 7, 12.1, 3, 9, "glow_blue")]
    return m


def stasis_mine():
    m = disc_mine("chrome", "glow_ice")
    m += [B(5, 3, 5, 11, 7, 11, "glass_cyan"), B(6.5, 7, 6.5, 9.5, 8, 9.5, "glow_ice")]
    return m


def emp():
    m = can("gunmetal", "hazard", "glow_blue")
    m += [B(7.6, 13.5, 7.6, 8.4, 16, 8.4, "copper"), B(6.6, 15.5, 6.6, 9.4, 16, 9.4, "glow_blue")]
    return m


def quantum_bomb():
    m = [B(4, 3, 4, 12, 11, 12, "glass_cyan", ("y", 45, [8, 7, 8]))]
    m += [B(5.5, 4.5, 5.5, 10.5, 9.5, 10.5, "glow_cyan"), B(6.5, 5.5, 2, 9.5, 8.5, 3, "glow_magenta"),
          B(13, 5.5, 6.5, 14, 8.5, 9.5, "glow_magenta")]
    m += [B(6.6, 5.6, 6.6, 9.4, 8.4, 9.4, "glow_white", ("x", 45, [8, 7, 8]))]
    for y in (2.4, 11):
        m += [B(4.6, y, 4.6, 11.4, y + 0.6, 11.4, "gunmetal", ("y", 45, [8, 7, 8]))]
    m += [B(1.8, 5.8, 6.8, 2.8, 8.2, 9.2, "glow_cyan"), B(6.8, 5.8, 13.2, 9.2, 8.2, 14.2, "glow_cyan"),
          B(7.3, 11.6, 7.3, 8.7, 13.4, 8.7, "glow_magenta", ("y", 45, [8, 12.5, 8]))]
    return m


def crystal_seed():
    m = [B(6, 1, 6, 10, 13, 10, "amethyst", ("y", 45, [8, 7, 8])), B(7, 13, 7, 9, 15, 9, "glow_purple", ("y", 45, [8, 7, 8]))]
    m += [B(3, 1, 7, 5, 8, 9, "amethyst", ("z", 22.5, [4, 1, 8])), B(11, 1, 7, 13, 7, 9, "amethyst", ("z", -22.5, [12, 1, 8]))]
    return m


def voidstorm():
    m = [B(4, 0, 4, 12, 2, 12, "obsidian"), B(4, 12, 4, 12, 14, 12, "obsidian")]
    for (x, z) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        m += [B(x, 2, z, x + 1, 12, z + 1, "gunmetal")]
    m += ball(8, 7, 8, 2.6, "glow_void") + [B(5, 6.6, 5, 11, 7.4, 11, "glow_purple")]
    return m


def micro_horizon():
    m = ball(8, 7, 8, 2.0, "black")
    m += [B(3, 6.7, 3, 13, 7.3, 13, "glow_orange", ("y", 45, [8, 7, 8])), B(4.5, 6.6, 4.5, 11.5, 7.4, 11.5, "glow_gold")]
    return m


def mirror_bomb():
    m = [B(4, 0, 4, 12, 2, 12, "chrome"), B(5, 2, 7.6, 11, 13, 8.4, "glass_cyan"), B(5.5, 2.5, 7.9, 10.5, 12.5, 8.1, "pearl")]
    m += [B(4.6, 2, 7.4, 5, 13.5, 8.6, "chrome"), B(11, 2, 7.4, 11.4, 13.5, 8.6, "chrome"), B(4.6, 13, 7.4, 11.4, 13.5, 8.6, "chrome")]
    return m


def solar_cell():
    m = cyl_y(8, 8, 3, 12, 2.4, "glass_gold") + cyl_y(8, 8, 4, 11, 1.3, "glow_gold")
    m += cyl_y(8, 8, 12, 13.5, 2.6, "gold") + cyl_y(8, 8, 1.5, 3, 2.6, "gold")
    m += [B(7.6, 4.5, 7.6, 8.4, 10.5, 8.4, "glow_white")]
    for y in (5.5, 9):
        m += cyl_y(8, 8, y, y + 0.5, 2.6, "gold")
    m += [B(7.2, 13.5, 7.2, 8.8, 14.6, 8.8, "chrome"), B(7.5, 14.6, 7.5, 8.5, 15.1, 8.5, "glow_gold"), B(7.2, 0.4, 7.2, 8.8, 1.5, 8.8, "chrome")]
    for (x, z) in ((5.4, 7.6), (10.1, 7.6), (7.6, 5.4), (7.6, 10.1)):
        m += [B(x, 12.2, z, x + 0.5, 13.2, z + 0.8, "glow_gold")]
    return m


def rift_shard():
    return [B(6.5, 1, 6.5, 9.5, 13, 9.5, "glow_void", ("z", 22.5, [8, 7, 8])),
            B(7, 2, 7, 9, 12, 9, "glow_magenta", ("x", 22.5, [8, 7, 8])),
            B(7.5, 0, 7.5, 8.5, 14, 8.5, "glow_white", ("z", 22.5, [8, 7, 8])),
            B(4.5, 9, 7.5, 5.5, 10, 8.5, "glow_purple", ("y", 45, [5, 9.5, 8])), B(11, 3.5, 7.5, 12, 4.5, 8.5, "glow_purple", ("y", 45, [11.5, 4, 8])),
            B(9.5, 11.5, 9.5, 10.2, 12.2, 10.2, "glow_magenta"), B(5.6, 2.6, 5.8, 6.3, 3.3, 6.5, "glow_magenta"),
            B(3, 6.8, 3, 13, 7.2, 3.6, "glow_void", ("y", 45, [8, 7, 8])), B(3, 6.8, 12.4, 13, 7.2, 13, "glow_void", ("y", 45, [8, 7, 8])),
            B(3, 6.8, 3, 3.6, 7.2, 13, "glow_void", ("y", 45, [8, 7, 8])), B(12.4, 6.8, 3, 13, 7.2, 13, "glow_void", ("y", 45, [8, 7, 8]))]


def celestial_core():
    m = ball(8, 7, 8, 3.0, "celestial")
    m += [B(3, 6.6, 3, 13, 7.4, 13, "glow_white", ("y", 45, [8, 7, 8])),
          B(7.6, 2, 3, 8.4, 12, 13, "glow_white", ("x", 45, [8, 7, 8]))]
    return m


def boomerang():
    m = [B(2, 6, 7.4, 9, 8, 8.6, "glow_cyan", ("y", 0, [8, 7, 8])), B(7, 6, 2, 9, 8, 9, "glow_cyan")]
    m += [B(6.5, 5.5, 6.5, 9.5, 8.5, 9.5, "white"), B(1.5, 6.5, 7.6, 2, 7.5, 8.4, "glow_white"), B(7.6, 6.5, 1.5, 8.4, 7.5, 2, "glow_white")]
    return m


def beacon_device(top, light, body="gunmetal"):
    m = [B(5, 0, 5, 11, 9, 11, body), B(4.5, 9, 4.5, 11.5, 10, 11.5, top)]
    m += [B(4.6, 0, 4.6, 11.4, 1, 11.4, top)]
    for (x, z) in ((4.6, 4.6), (10.6, 4.6), (4.6, 10.6), (10.6, 10.6)):
        m += [B(x, 1, z, x + 0.8, 9, z + 0.8, "steel")]
    m += [B(7.5, 10, 7.5, 8.5, 15, 8.5, "chrome"), B(7, 15, 7, 9, 16, 9, light)]
    m += [B(6.8, 11.5, 6.8, 9.2, 12, 9.2, light), B(7.1, 13.3, 7.1, 8.9, 13.7, 8.9, "gunmetal"), B(7.3, 16, 7.3, 8.7, 16.8, 8.7, "glow_white")]
    m += [B(5.2, 3, 4.9, 10.8, 7, 5, "screen"), B(4.9, 5, 6, 5, 6, 10, light), B(11, 5, 6, 11.1, 6, 10, light)]
    m += [B(5.5, 7.6, 10.95, 10.5, 8.4, 11.1, "vent"), B(5.5, 1.6, 10.95, 10.5, 2.4, 11.1, "vent")]
    return m


def siegebreaker():
    m = [B(3, 0, 4, 13, 3, 12, "gunmetal")]
    for i, x in enumerate((3.5, 6.5, 9.5)):
        for z in (4.5, 8.5):
            m += cyl_y(x + 1.2, z + 1.2, 3, 11, 1.1, "white") + [B(x + 0.5, 11, z + 0.5, x + 1.9, 12.5, z + 1.9, "glow_purple")]
    return m


def spindle():
    m = [B(6, 0, 6, 10, 4, 10, "crimson"), B(5.5, 0, 5.5, 10.5, 1, 10.5, "obsidian"), B(6.4, 3.6, 6.4, 9.6, 4.3, 9.6, "glow_crimson")]
    m += [B(7, 4, 7, 9, 14, 9, "obsidian", ("y", 45, [8, 9, 8])), B(7.5, 14, 7.5, 8.5, 16, 8.5, "glow_crimson")]
    m += [B(7.6, 4.5, 6.6, 8.4, 13.5, 6.7, "glow_crimson", ("y", 45, [8, 9, 8]))]
    m += [B(4, 6, 7.5, 12, 7, 8.5, "glow_crimson"), B(7.5, 9, 4, 8.5, 10, 12, "glow_crimson")]
    m += [B(5, 11.6, 7.7, 11, 12.2, 8.3, "glow_red", ("y", 45, [8, 12, 8]))]
    for (x, y, z) in ((3.6, 6.5, 8), (12.4, 6.5, 8), (8, 9.5, 3.6), (8, 9.5, 12.4)):
        m += [B(x - 0.6, y - 0.6, z - 0.6, x + 0.6, y + 0.6, z + 0.6, "obsidian", ("y", 45, [x, y, z]))]
    return m


def choir():
    m = [B(4, 0, 4, 12, 3, 12, "crimson"), B(3.6, 0, 3.6, 12.4, 1, 12.4, "obsidian"), B(4.2, 2.6, 4.2, 11.8, 3.1, 11.8, "glow_orange")]
    for i, (x, z, h) in enumerate(((5, 5, 13), (9.5, 5, 11), (5, 9.5, 10), (9.5, 9.5, 14))):
        m += [B(x, 3, z, x + 1.5, h, z + 1.5, "gold"), B(x - 0.3, h, z - 0.3, x + 1.8, h + 1, z + 1.8, "glow_orange")]
        m += [B(x + 0.4, h + 1, z + 0.4, x + 1.1, h + 1.8, z + 1.1, "glow_white")]
        for y in range(5, int(h) - 1, 3):
            m += [B(x - 0.15, y, z - 0.15, x + 1.65, y + 0.4, z + 1.65, "obsidian")]
    return m


def devourer():
    m = ball(8, 7, 8, 3.2, "black")
    for rot in (0, 60, 120):
        m += [B(3.5, 6.6, 7.6, 12.5, 7.4, 8.4, "glow_crimson", ("y", rot, [8, 7, 8]))]
    m += [B(6.5, 5.5, 4.4, 9.5, 8.5, 4.6, "glow_crimson"), B(7.3, 6.3, 4.3, 8.7, 7.7, 4.4, "glow_white")]
    for k in range(6):
        import math
        a = k * math.pi / 3 + 0.3
        x, z = 8 + math.cos(a) * 3.6, 8 + math.sin(a) * 3.6
        m += [B(x - 0.35, 9.2, z - 0.35, x + 0.35, 11.2, z + 0.35, "obsidian"), B(x - 0.3, 3.0, z - 0.3, x + 0.3, 4.6, z + 0.3, "obsidian")]
    m += [B(7.4, 10.4, 7.4, 8.6, 11.6, 8.6, "glow_crimson", ("y", 45, [8, 11, 8]))]
    return m


def riftfall():
    m = beacon_device("crimson", "glow_crimson", "obsidian")
    m += [B(3, 12, 3, 13, 12.6, 13, "glow_crimson", ("y", 45, [8, 12, 8]))]
    return m


SMALL = {
    "bounce_bomb": lambda: orb_device("glass_purple", "glow_pink", "white"),
    "loot_tornado": lambda: can("gold", "black", "glow_gold", "gold"),
    "moonwalk_mine": lambda: disc_mine("white", "glow_white", "pearl"),
    "snow_globe_grenade": snow_globe,
    "slimequake_bomb": slimequake,
    "hologram_bomb": hologram_bomb,
    "magnet_mayhem": magnet,
    "rainbow_paint_bomb": paint_bomb,
    "shatterfrost_bomb": shatterfrost,
    "arcflash_charge": arcflash,
    "seismic_pulse_mine": lambda: disc_mine("olive", "glow_orange"),
    "implosion_grenade": lambda: orb_device("gunmetal", "glow_blue", "glow_ice"),
    "microstrike_beacon": lambda: beacon_device("hazard", "glow_blue"),
    "plasma_boomerang": boomerang,
    "ion_fog_canister": lambda: can("white", "glow_ice", "glow_ice"),
    "stasis_mine": stasis_mine,
    "emp_disruptor": emp,
    "quantum_fracture_bomb": quantum_bomb,
    "crystal_cataclysm": crystal_seed,
    "voidstorm_generator": voidstorm,
    "siegebreaker_array": siegebreaker,
    "micro_event_horizon": micro_horizon,
    "mirror_dimension_bomb": mirror_bomb,
    "solar_verdict_beacon": lambda: beacon_device("gold", "glow_gold", "white"),
    "titanfall_warhead": lambda: beacon_device("hazard", "glow_orange", "gold"),
    "riftfall_engine": riftfall,
    "apocalypse_spindle": spindle,
    "devourer_core": devourer,
    "extinction_choir": choir,
    "solar_cell": solar_cell,
    "rift_shard": rift_shard,
    "celestial_core": celestial_core,
}


# =========================================================================== warheads (blocks)

def helios_crown_bomb():
    m = [B(1, 0, 1, 15, 2, 15, "gold")]
    m += cyl_y(8, 8, 2, 9, 4.5, "white")
    for (x, z) in ((1.5, 7.4), (13.5, 7.4), (7.4, 1.5), (7.4, 13.5)):
        m += [B(x, 9, z, x + 1.2, 14, z + 1.2, "gold", ("y", 45, [8, 9, 8]))]
    for (x, z) in ((1.5, 7.4), (13.5, 7.4), (7.4, 1.5), (7.4, 13.5)):
        m += [B(x, 9, z, x + 1.2, 15, z + 1.2, "glow_gold")]
    m += [B(5, 9, 5, 11, 10, 11, "gold"), B(6.5, 10, 6.5, 9.5, 12, 9.5, "glow_white")]
    return m


def continental_pulse():
    m = [B(0, 0, 0, 16, 3, 16, "gunmetal"), B(0.5, 3, 0.5, 15.5, 4, 15.5, "glow_gold")]
    m += [B(2, 4, 2, 14, 10, 14, "olive"), B(1.9, 6, 1.9, 14.1, 7, 14.1, "hazard")]
    m += [B(6, 10, 6, 10, 15, 10, "gold"), B(5, 15, 5, 11, 16, 11, "glow_gold")]
    m += [B(7.6, 4, 1.8, 8.4, 10, 2, "glow_gold"), B(1.8, 4, 7.6, 2, 10, 8.4, "glow_gold")]
    return m


def dawnstar_payload():
    m = [B(2, 0, 2, 14, 2, 14, "white")]
    for (x, z) in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):
        m += [B(x, 2, z, x + 1.5, 15, z + 1.5, "gold")]
    m += ball(8, 8.5, 8, 4.0, "glow_gold") + [B(3.5, 4, 3.5, 12.5, 13, 12.5, "glass_gold")]
    m += [B(2, 15, 2, 14, 16, 14, "white")]
    return m


def bloodstar_collapse():
    m = [B(1, 0, 1, 15, 2, 15, "obsidian")]
    m += ball(8, 8, 8, 4.6, "crimson") + ball(8, 8, 8, 3.2, "glow_crimson")
    m += [B(5.6, 5.6, 5.6, 10.4, 10.4, 10.4, "glow_red", ("y", 45, [8, 8, 8]))]
    for rot in (0, 45, -45):
        m += [B(1.2, 7.7, 7.7, 14.8, 8.3, 8.3, "glow_crimson", ("y", rot, [8, 8, 8]))]
    m += [B(7.7, 1.6, 7.7, 8.3, 14.4, 8.3, "glow_red", ("z", 45, [8, 8, 8]))]
    # obsidian claws holding the dying star
    for (x, z) in ((2, 2), (12.8, 2), (2, 12.8), (12.8, 12.8)):
        m += [B(x, 2, z, x + 1.2, 6, z + 1.2, "obsidian"), B(x + 0.2, 6, z + 0.2, x + 1.0, 9, z + 1.0, "crimson")]
        m += [B(x + 0.3, 9, z + 0.3, x + 0.9, 9.6, z + 0.9, "glow_crimson")]
    for (x, y, z) in ((2.5, 11.5, 8), (13.5, 4.5, 8), (8, 12.5, 2.5), (8, 3.5, 13.5)):
        m += [B(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5, "glow_crimson", ("y", 45, [x, y, z]))]
    m += [B(6.5, 13, 6.5, 9.5, 15, 9.5, "obsidian"), B(7.3, 15, 7.3, 8.7, 16, 8.7, "glow_crimson")]
    return m


def last_light():
    m = [B(0, 0, 0, 16, 2, 16, "pearl"), B(1, 14, 1, 15, 16, 15, "pearl")]
    for (x, z) in ((0.5, 0.5), (14, 0.5), (0.5, 14), (14, 14)):
        m += [B(x, 2, z, x + 1.5, 14, z + 1.5, "celestial")]
    m += ball(8, 8, 8, 3.6, "glow_white") + [B(3, 3, 3, 13, 13, 13, "glass_cyan")]
    return m


def zeropoint():
    m = [B(1, 0, 1, 15, 2, 15, "chrome"), B(1, 14, 1, 15, 16, 15, "chrome")]
    m += [B(2, 2, 2, 14, 14, 14, "glass_ice")]
    m += ball(8, 8, 8, 1.4, "black") + [B(7.6, 2, 7.6, 8.4, 14, 8.4, "glow_ice"), B(2, 7.6, 7.6, 14, 8.4, 8.4, "glow_ice"),
                                        B(7.6, 7.6, 2, 8.4, 8.4, 14, "glow_ice")]
    m += [B(0.5, 2, 0.5, 1.5, 14, 1.5, "celestial"), B(14.5, 2, 14.5, 15.5, 14, 15.5, "celestial")]
    return m


def unmaker():
    m = []
    t = 0.8
    for (x, z) in ((1, 1), (14.2, 1), (1, 14.2), (14.2, 14.2)):
        m += [B(x, 0, z, x + t, 16, z + t, "glow_white")]
        m += [B(x - 0.2, 7.4, z - 0.2, x + t + 0.2, 8.6, z + t + 0.2, "celestial")]
    for y in (0, 15.2):
        m += [B(1, y, 1, 15, y + t, 1 + t, "glow_white"), B(1, y, 14.2, 15, y + t, 15, "glow_white"),
              B(1, y, 1, 1 + t, y + t, 15, "glow_white"), B(14.2, y, 1, 15, y + t, 15, "glow_white")]
    # a second frame, turned 45 degrees, unmaking the first
    for (x, z) in ((3.5, 3.5), (12, 3.5), (3.5, 12), (12, 12)):
        m += [B(x, 3, z, x + 0.5, 13, z + 0.5, "pearl", ("y", 45, [8, 8, 8]))]
    for y in (3, 12.5):
        m += [B(3.5, y, 3.5, 12.5, y + 0.5, 4, "pearl", ("y", 45, [8, 8, 8])), B(3.5, y, 12, 12.5, y + 0.5, 12.5, "pearl", ("y", 45, [8, 8, 8])),
              B(3.5, y, 3.5, 4, y + 0.5, 12.5, "pearl", ("y", 45, [8, 8, 8])), B(12, y, 3.5, 12.5, y + 0.5, 12.5, "pearl", ("y", 45, [8, 8, 8]))]
    m += [B(5, 5, 5, 11, 11, 11, "celestial", ("y", 45, [8, 8, 8])), B(6.5, 3, 6.5, 9.5, 13, 9.5, "black")]
    m += [B(6.2, 6.2, 6.2, 9.8, 9.8, 9.8, "glow_void", ("x", 45, [8, 8, 8]))]
    m += [B(7.2, 13, 7.2, 8.8, 14.6, 8.8, "glow_white", ("y", 45, [8, 13.8, 8])), B(7.2, 1.4, 7.2, 8.8, 3, 8.8, "glow_white", ("y", 45, [8, 2.2, 8]))]
    return m


def godfall():
    m = [B(0, 0, 0, 16, 3, 16, "gold"), B(0.5, 3, 0.5, 15.5, 4, 15.5, "celestial")]
    m += [B(3, 4, 3, 13, 12, 13, "white"), B(2.9, 7, 2.9, 13.1, 8, 13.1, "glow_gold")]
    m += [B(6, 12, 6, 10, 16, 10, "gold"), B(6.5, 16, 6.5, 9.5, 16.6, 9.5, "glow_white")]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m += [B(x, 4, z, x + 1, 13, z + 1, "glow_gold")]
    return m


def final_existence():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(0, 14, 0, 16, 16, 16, "obsidian")]
    m += [B(1, 2, 1, 15, 14, 15, "glow_void")]
    for y in (4, 8, 12):
        m += [B(0.8, y, 0.8, 15.2, y + 0.5, 15.2, "celestial")]
    m += [B(7, 16, 7, 9, 17, 9, "glow_magenta"), B(5.5, 6, 0.7, 10.5, 11, 0.8, "radiation")]
    return m


BOMBS = {"helios_crown_bomb": helios_crown_bomb, "continental_pulse": continental_pulse, "dawnstar_payload": dawnstar_payload,
         "bloodstar_collapse": bloodstar_collapse, "last_light": last_light, "zeropoint": zeropoint, "unmaker": unmaker,
         "godfall": godfall, "final_existence": final_existence}

_unused = (cyl_x, cradle)


# =========================================================================== bomb expansion II (blocks)

def _base(mat="gunmetal"):
    return [B(1, 0, 1, 15, 2, 15, mat)]


def disco_bomb():
    m = _base("black")
    m += ball(8, 8, 8, 5.2, "glow_rainbow")
    for y in (4, 7, 10, 13):
        m += [B(3, y, 3, 13, y + 0.5, 13, "glass_cyan")]
    m += [B(7, 13.5, 7, 9, 16, 9, "chrome")]
    return m


def glitter_bomb():
    m = _base("gold")
    m += cyl_y(8, 8, 2, 11, 4.5, "glass_gold")
    m += cyl_y(8, 8, 3, 10, 3.2, "glow_gold")
    for y in (4, 7.5):
        m += cyl_y(8, 8, y, y + 0.6, 4.8, "gold")
    for (x, y, z) in ((3.2, 5.5, 8), (12.8, 8.5, 8), (8, 9.5, 3.2), (8, 4, 12.8), (4.6, 9.8, 4.6), (11.4, 3.6, 11.4)):
        m += [B(x - 0.45, y - 0.45, z - 0.45, x + 0.45, y + 0.45, z + 0.45, "glow_white", ("y", 45, [x, y, z]))]
    m += [B(5, 11, 5, 11, 12.5, 11, "gold"), B(6, 12.5, 6, 10, 13.2, 10, "glow_gold")]
    m += [B(7.4, 13.2, 7.4, 8.6, 15.2, 8.6, "glow_white", ("y", 45, [8, 14.2, 8])), B(6.6, 13.8, 7.7, 9.4, 14.6, 8.3, "glow_gold"),
          B(7.7, 13.8, 6.6, 8.3, 14.6, 9.4, "glow_gold")]
    return m


def pumpkin_patch_bomb():
    m = _base("olive")
    m += ball(8, 7, 8, 5.4, "glow_orange")
    m += [B(7, 11.6, 7, 9, 14, 9, "olive"), B(5.2, 7.2, 2.6, 7, 9, 3, "black"), B(9, 7.2, 2.6, 10.8, 9, 3, "black"),
          B(5.5, 4.5, 2.6, 10.5, 5.6, 3, "black")]
    return m


def cobweb_bomb():
    m = _base("white")
    m += ball(8, 8, 8, 4.4, "pearl")
    for r in (0, 60, 120):
        m += [B(1.5, 7.7, 7.7, 14.5, 8.3, 8.3, "white", ("y", r, [8, 8, 8])), B(7.7, 1.5, 7.7, 8.3, 14.5, 8.3, "white", ("x", r, [8, 8, 8]))]
    return m


def balloon_bomb():
    m = _base("pink")
    m += [B(7.7, 2, 7.7, 8.3, 8, 8.3, "white")]
    m += ball(5, 11, 8, 3.0, "glow_pink") + ball(11, 11, 8, 3.0, "glow_cyan") + ball(8, 13.5, 8, 2.6, "glow_gold")
    return m


def sprinkler_bomb():
    m = _base("steel")
    m += cyl_y(8, 8, 2, 8, 3.2, "chrome")
    m += [B(3, 8, 7, 13, 9.4, 9, "steel"), B(7, 8, 3, 9, 9.4, 13, "steel")]
    for (x, z) in ((2.6, 7.4), (13, 7.4), (7.4, 2.6), (7.4, 13)):
        m += [B(x, 8.6, z, x + 0.5, 10, z + 0.5, "glow_ice")]
    m += [B(6, 9.4, 6, 10, 11.4, 10, "glass_cyan")]
    return m


def thermite_bomb():
    m = _base("black")
    m += cyl_y(8, 8, 2, 12, 4.2, "copper")
    for y in (3.2, 9.6):
        m += cyl_y(8, 8, y, y + 0.7, 4.5, "gunmetal")
    m += [B(3.6, 5, 3.6, 12.4, 6, 12.4, "hazard"), B(4.6, 2, 7, 11.4, 4, 9, "glow_orange")]
    for (x0, z0, x1, z1) in ((3.75, 6.6, 3.85, 9.4), (12.15, 6.6, 12.25, 9.4), (6.6, 3.75, 9.4, 3.85), (6.6, 12.15, 9.4, 12.25)):
        m += [B(x0, 6.6, z0, x1, 9.2, z1, "glow_orange")]
    m += [B(6.5, 12, 6.5, 9.5, 15, 9.5, "gunmetal"), B(7.2, 15, 7.2, 8.8, 16, 8.8, "glow_orange")]
    m += [B(6.2, 12.5, 6.2, 9.8, 13.2, 9.8, "steel"), B(7.6, 13.6, 9.5, 8.4, 14.4, 9.7, "glow_white")]
    m += [B(4.5, 2, 4.5, 5.3, 2.4, 5.3, "glow_orange"), B(10.6, 2, 11, 11.6, 2.4, 11.8, "glow_gold")]
    return m


def tesla_bomb():
    m = _base("gunmetal")
    m += cyl_y(8, 8, 2, 7, 4.6, "copper")
    for i in range(3):
        m += cyl_y(8, 8, 7 + i * 2.2, 8.6 + i * 2.2, 4.0 - i * 0.8, "chrome")
    m += [B(7.4, 13.5, 7.4, 8.6, 16, 8.6, "glow_blue"), B(7, 2, 3.4, 9, 3, 12.6, "glow_blue")]
    return m


def sinkhole_bomb():
    m = _base("rock")
    m += [B(3, 2, 3, 13, 6, 13, "rock"), B(5, 6, 5, 11, 7, 11, "black"), B(6, 7, 6, 10, 8, 10, "black")]
    m += [B(1.5, 2, 7, 3, 3, 9, "glow_orange"), B(13, 2, 7, 14.5, 3, 9, "glow_orange")]
    m += cyl_y(8, 8, 2, 12, 1.4, "gunmetal") + [B(7.3, 12, 7.3, 8.7, 14, 8.7, "red")]
    return m


def tsunami_bomb():
    m = _base("steel")
    m += [B(2, 2, 2, 14, 11, 14, "glass_cyan")]
    m += [B(2.4, 2.4, 2.4, 13.6, 7, 13.6, "glow_blue"), B(2.4, 7, 2.4, 13.6, 8.2, 13.6, "glow_white")]
    # breaking wave crests on every face
    for k, x in enumerate((3, 5.5, 8, 10.5)):
        y = 7.2 + (0.8 if k % 2 else 0)
        m += [B(x, y, 1.9, x + 2.2, y + 0.7, 2.0, "glow_ice"), B(x, y, 14, x + 2.2, y + 0.7, 14.1, "glow_ice"),
              B(1.9, y, x, 2.0, y + 0.7, x + 2.2, "glow_ice"), B(14, y, x, 14.1, y + 0.7, x + 2.2, "glow_ice")]
    for (x, z) in ((1.5, 1.5), (13.2, 1.5), (1.5, 13.2), (13.2, 13.2)):
        m += [B(x, 2, z, x + 1.3, 12, z + 1.3, "chrome"), B(x + 0.3, 12, z + 0.3, x + 1.0, 13.6, z + 1.0, "glow_ice")]
    m += [B(2, 11, 2, 14, 13, 14, "steel"), B(5, 13, 5, 11, 13.6, 11, "vent"), B(7, 13.6, 7, 9, 14.8, 9, "glow_blue", ("y", 45, [8, 14.2, 8]))]
    return m


def volcano_bomb():
    m = _base("obsidian")
    m += [B(2, 2, 2, 14, 5, 14, "rock"), B(3.5, 5, 3.5, 12.5, 8, 12.5, "rock"), B(5, 8, 5, 11, 11, 11, "rock"), B(6.2, 11, 6.2, 9.8, 13, 9.8, "rock")]
    m += [B(6.8, 12.4, 6.8, 9.2, 13.6, 9.2, "glow_orange"), B(7.4, 8, 3.4, 8.6, 11, 3.6, "glow_orange"), B(3.4, 4, 7.4, 3.6, 7, 8.6, "glow_orange")]
    return m


def ice_age_bomb():
    m = _base("white")
    m += ball(8, 8, 8, 4.6, "glass_ice")
    for r in (0, 45, 90, 135):
        m += [B(7.5, 1.5, 7.5, 8.5, 15, 8.5, "glow_ice", ("z", r, [8, 8, 8]))]
    m += [B(6, 14, 6, 10, 16, 10, "glow_ice")]
    return m


def hellmouth_bomb():
    m = _base("obsidian")
    m += [B(2, 2, 2, 14, 12, 14, "crimson")]
    m += [B(4, 12, 4, 12, 12.6, 12, "glow_crimson"), B(5.5, 12.6, 5.5, 10.5, 14, 10.5, "black")]
    m += [B(2, 5, 1.8, 14, 6, 2, "glow_orange"), B(2, 9, 1.8, 14, 10, 2, "glow_orange")]
    for (x, z) in ((1.2, 1.2), (13.8, 1.2), (1.2, 13.8), (13.8, 13.8)):
        m += [B(x, 2, z, x + 1, 15, z + 1, "obsidian"), B(x, 15, z, x + 1, 16, z + 1, "glow_crimson")]
    return m


def cyclone_bomb():
    m = _base("gunmetal")
    for i, (r, y) in enumerate(((5, 2), (4.2, 5), (3.4, 8), (2.6, 11), (1.8, 13))):
        m += cyl_y(8 + (i % 2) * 0.6, 8 - (i % 2) * 0.6, y, y + 3, r, "pearl" if i % 2 == 0 else "steel")
    m += [B(7.5, 15, 7.5, 8.5, 16, 8.5, "glow_ice")]
    return m


def doomsday_clock():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 3, 15, "crimson")]
    m += cyl_y(8, 8, 3, 6, 7.2, "gunmetal")
    for (x, z) in ((7.5, 1.6), (7.5, 13.6), (1.6, 7.5), (13.6, 7.5)):
        m += [B(x, 6, z, x + 1, 6.5, z + 1, "glow_crimson")]
    m += [B(7.6, 6, 8, 8.4, 6.6, 12.4, "glow_crimson"), B(8, 6, 7.6, 11, 6.6, 8.4, "glow_white")]
    m += [B(2, 3, 2, 14, 9, 14, "glass_gold"), B(7, 8, 7, 9, 12, 9, "glow_crimson")]
    return m


def moonfall():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian")]
    m += ball(8, 9, 8, 6.0, "steel") + ball(8, 9, 8, 4.0, "pearl")
    for (x, y) in ((6, 12), (10, 9), (7, 6)):
        m += [B(x - 1, y, 2.0, x + 1, y + 1.4, 3.0, "obsidian"), B(x - 0.6, y + 0.3, 1.9, x + 0.6, y + 1.1, 2.0, "rock")]
    for (y, z) in ((11, 6), (7, 10.5)):
        m += [B(2.0, y, z - 1, 3.0, y + 1.4, z + 1, "obsidian"), B(13.0, y - 2, z - 1.5, 14.0, y - 0.6, z + 0.5, "obsidian")]
    for (x0, z0, x1, z1) in ((0.5, 0.5, 15.5, 1.3), (0.5, 14.7, 15.5, 15.5), (0.5, 0.5, 1.3, 15.5), (14.7, 0.5, 15.5, 15.5)):
        m += [B(x0, 8.7, z0, x1, 9.1, z1, "glow_white", ("y", 22.5, [8, 9, 8]))]
    m += [B(0.5, 2, 0.5, 15.5, 3, 15.5, "glow_white"), B(7.4, 15.4, 7.4, 8.6, 16.5, 8.6, "celestial")]
    for (x, z) in ((1.6, 13.2), (13.4, 2.4)):
        m += [B(x - 0.5, 13.5, z - 0.5, x + 0.5, 14.5, z + 0.5, "rock", ("y", 45, [x, 14, z]))]
    return m


def big_bang():
    m = [B(0, 0, 0, 16, 2, 16, "black"), B(0, 14, 0, 16, 16, 16, "black")]
    m += [B(1, 2, 1, 15, 14, 15, "glass_cyan")]
    m += ball(8, 8, 8, 1.6, "glow_white")
    for r in (0, 45):
        m += [B(1.5, 7.7, 7.7, 14.5, 8.3, 8.3, "glow_rainbow", ("y", r, [8, 8, 8])), B(7.7, 1.5, 7.7, 8.3, 14.5, 8.3, "glow_rainbow", ("z", r, [8, 8, 8]))]
    m += [B(0, 2, 0, 1, 14, 1, "celestial"), B(15, 2, 15, 16, 14, 16, "celestial")]
    return m


def world_eater():
    m = [B(0, 0, 0, 16, 3, 16, "obsidian")]
    m += ball(8, 9, 8, 5.4, "black")
    for rot in (0, 60, 120):
        m += [B(1.5, 8.6, 7.6, 14.5, 9.4, 8.4, "glow_void", ("y", rot, [8, 9, 8]))]
    m += [B(6, 7.5, 2.4, 10, 11.5, 2.8, "glow_crimson"), B(7, 8.5, 2.2, 9, 10.5, 2.5, "black")]
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m += [B(x, 3, z, x + 1.5, 9, z + 1.5, "obsidian", ("z", 12 if x < 8 else -12, [x, 3, z]))]
    return m


def chrono_break():
    m = [B(0, 0, 0, 16, 2, 16, "chrome")]
    m += cyl_y(8, 8, 2, 4, 7.4, "titanium")
    m += cyl_y(8, 8, 4, 5, 6.6, "glass_cyan")
    for (x, z) in ((7.4, 1.4), (7.4, 14), (1.4, 7.4), (14, 7.4)):
        m += [B(x, 5, z, x + 1.2, 5.6, z + 1.2, "glow_ice")]
    m += [B(7.6, 5, 8, 8.4, 6, 13, "glow_ice"), B(8, 5, 7.6, 12, 6, 8.4, "glow_white")]
    m += [B(2, 5, 2, 14, 15, 14, "glass_cyan")]
    m += [B(6, 7, 6, 10, 13, 10, "glow_ice", ("y", 45, [8, 10, 8])), B(7.3, 15, 7.3, 8.7, 16, 8.7, "celestial")]
    return m


def ragnarok():
    m = [B(0, 0, 0, 16, 3, 16, "obsidian"), B(0.5, 3, 0.5, 15.5, 4, 15.5, "glow_crimson")]
    m += [B(3, 4, 3, 13, 11, 13, "crimson"), B(2.8, 6.5, 2.8, 13.2, 7.5, 13.2, "glow_orange")]
    m += [B(7, 11, 7, 9, 16, 9, "gold"), B(5, 14, 7.4, 11, 15, 8.6, "gold"), B(7.4, 14, 5, 8.6, 15, 11, "gold")]
    m += [B(7.3, 16, 7.3, 8.7, 17, 8.7, "glow_orange")]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m += [B(x, 4, z, x + 1, 13, z + 1, "glow_crimson")]
    return m


BOMBS.update({
    "disco_bomb": disco_bomb, "glitter_bomb": glitter_bomb, "pumpkin_patch_bomb": pumpkin_patch_bomb, "cobweb_bomb": cobweb_bomb,
    "balloon_bomb": balloon_bomb, "sprinkler_bomb": sprinkler_bomb, "thermite_bomb": thermite_bomb, "tesla_bomb": tesla_bomb,
    "sinkhole_bomb": sinkhole_bomb, "tsunami_bomb": tsunami_bomb, "volcano_bomb": volcano_bomb, "ice_age_bomb": ice_age_bomb,
    "hellmouth_bomb": hellmouth_bomb, "cyclone_bomb": cyclone_bomb, "doomsday_clock": doomsday_clock, "moonfall": moonfall,
    "big_bang": big_bang, "world_eater": world_eater, "chrono_break": chrono_break, "ragnarok": ragnarok,
})


# =========================================================================== celestial / world destruction II

def exploding_cows():
    m = _base("pearl")
    m += [B(3, 2, 4, 13, 9, 12, "white")]
    for (x, y, z, w, h) in ((4, 5, 3.8, 3, 2.5), (8.5, 3, 3.8, 3.5, 3), (5.5, 7, 12.1, 3, 1.8), (9, 5, 12.1, 3, 2.6)):
        m += [B(x, y, z, x + w, y + h, z + 0.2, "black")]
    m += [B(4, 6, 1, 12, 12, 5, "white"), B(3.2, 11, 1.8, 4.2, 13, 2.8, "gold"), B(11.8, 11, 1.8, 12.8, 13, 2.8, "gold")]
    m += [B(6, 6.5, 0.6, 10, 9.2, 1, "pink"), B(6.8, 7.2, 0.4, 7.4, 7.8, 0.6, "black"), B(8.6, 7.2, 0.4, 9.2, 7.8, 0.6, "black")]
    m += [B(6.5, 2, 12, 9.5, 3.8, 13.4, "pink"), B(7.4, 9, 5, 8.6, 13.4, 6, "glow_orange")]
    m += [B(7.5, 13, 5.3, 8.5, 15.5, 6.3, "black"), B(7.2, 15.5, 5.1, 8.8, 16.2, 6.5, "glow_red")]
    return m


def deadly_twister():
    m = _base("steel")
    for i, (r, y) in enumerate(((2.0, 2), (2.8, 4.5), (3.6, 7), (4.5, 9.5), (5.5, 12), (6.5, 14))):
        m += cyl_y(8 + (0.8 if i % 2 else -0.8), 8 + (-0.6 if i % 2 else 0.6), y, y + 2.6, r, "pearl" if i % 2 == 0 else "steel")
    m += [B(2, 14.2, 7.7, 14, 14.6, 8.3, "glow_ice"), B(7.7, 14.2, 2, 8.3, 14.6, 14, "glow_ice")]
    m += [B(7.4, 2, 7.4, 8.6, 15, 8.6, "glow_white")]
    # debris caught in the funnel and a lightning fork
    for (x, y, z, mat) in ((2.6, 10.5, 4, "rock"), (13, 12.5, 10, "olive"), (4, 13.6, 13, "rock"), (12, 7.5, 3.5, "copper")):
        m += [B(x - 0.6, y - 0.6, z - 0.6, x + 0.6, y + 0.6, z + 0.6, mat, ("y", 45, [x, y, z]))]
    m += [B(11.6, 9, 11.8, 12.0, 12.5, 12.2, "glow_ice", ("z", 22.5, [11.8, 9, 12])), B(11.0, 6, 11.8, 11.4, 9.2, 12.2, "glow_white", ("z", -22.5, [11.2, 9, 12]))]
    return m


def colossal_spear():
    m = _base("gold")
    m += [B(2, 2, 2, 14, 4, 14, "obsidian"), B(4, 4, 4, 12, 5, 12, "rock")]
    m += [B(7.2, 4, 7.2, 8.8, 14, 8.8, "copper"), B(6.7, 6, 6.7, 9.3, 7, 9.3, "glow_gold"), B(6.7, 9, 6.7, 9.3, 10, 9.3, "glow_gold")]
    m += [B(5.5, 14, 7, 10.5, 15, 9, "gold"), B(6.8, 15, 7, 9.2, 17, 9, "chrome"), B(7.4, 17, 7.4, 8.6, 19, 8.6, "chrome")]
    m += [B(6.6, 4, 6.6, 6.8, 13, 9.4, "glow_orange"), B(9.2, 4, 6.6, 9.4, 13, 9.4, "glow_orange")]
    return m


def zombie_apocalypse():
    m = _base("black")
    m += [B(2, 2, 2, 14, 12, 14, "toxic"), B(1.8, 6, 1.8, 14.2, 7.5, 14.2, "biohazard")]
    m += [B(4, 12, 4, 12, 14, 12, "olive"), B(5, 12.2, 3.8, 7, 13.6, 4, "glow_red"), B(9, 12.2, 3.8, 11, 13.6, 4, "glow_red")]
    m += [B(5.5, 3.5, 1.8, 10.5, 4.6, 2, "glow_green"), B(3, 14, 3, 5, 16, 5, "glow_green"), B(11, 14, 11, 13, 16, 13, "glow_green")]
    m += [B(0.5, 2, 0.5, 2, 12, 2, "rock"), B(14, 2, 14, 15.5, 12, 15.5, "rock")]
    return m


def ufo_invasion():
    m = _base("obsidian")
    m += [B(7, 2, 7, 9, 5, 9, "chrome")]
    m += cyl_y(8, 8, 5, 7, 7.2, "steel") + cyl_y(8, 8, 7, 8.5, 5.6, "titanium")
    m += [B(1.2, 6.4, 1.2, 14.8, 6.8, 14.8, "glow_cyan", ("y", 45, [8, 6.6, 8]))]
    m += cyl_y(8, 8, 8.5, 12, 3.2, "glass_cyan") + [B(7, 9, 7, 9, 11, 9, "glow_green")]
    for (x, z) in ((3, 8), (13, 8), (8, 3), (8, 13)):
        m += [B(x - 0.5, 5.6, z - 0.5, x + 0.5, 6.1, z + 0.5, "glow_white")]
    m += [B(6, 2, 6, 10, 2.4, 10, "glow_cyan")]
    return m


def galactic_smash():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(0.5, 2, 0.5, 15.5, 2.8, 15.5, "glow_void")]
    m += ball(8, 9, 8, 6.0, "celestial") + ball(8, 9, 8, 4.4, "obsidian")
    m += [B(1, 8.6, 7.6, 15, 9.4, 8.4, "glow_rainbow", ("y", 0, [8, 9, 8])), B(7.6, 1.5, 7.6, 8.4, 14.5, 8.4, "glow_rainbow")]
    m += [B(5.5, 9, 1.4, 6.5, 10.2, 2, "glow_white"), B(9.5, 9, 1.4, 10.5, 10.2, 2, "glow_white")]
    m += [B(7, 14.5, 7, 9, 16.5, 9, "glow_white")]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m += [B(x, 2.8, z, x + 1, 13, z + 1, "celestial")]
    return m


def booming_tick():
    m = _base("black")
    m += ball(8, 8.5, 8, 5.6, "black") + [B(3, 8.2, 3, 13, 8.8, 13, "glow_red", ("y", 45, [8, 8.5, 8]))]
    m += [B(6.5, 13.5, 6.5, 9.5, 15, 9.5, "steel"), B(7.4, 15, 7.4, 8.6, 16.6, 8.6, "glow_orange")]
    m += [B(5.8, 6.5, 2.4, 10.2, 11, 2.6, "screen_red"), B(7.8, 7.2, 2.2, 8.2, 9.8, 2.5, "glow_white"), B(8, 8.7, 2.2, 9.6, 9.1, 2.5, "glow_white")]
    for k in range(12):
        import math
        a = k * math.pi / 6
        m += [B(8 + math.cos(a) * 3.6 - 0.3, 8.8 + math.sin(a) * 3.6 - 0.3, 2.0, 8 + math.cos(a) * 3.6 + 0.3, 8.8 + math.sin(a) * 3.6 + 0.3, 2.3, "glow_red")]
    return m


def alien_death_ray():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 3, 15, "glow_void")]
    m += cyl_y(8, 8, 3, 12, 3.0, "obsidian") + [B(4.8, 5, 4.8, 11.2, 5.6, 11.2, "glow_lime"), B(4.8, 8.4, 4.8, 11.2, 9, 11.2, "glow_lime")]
    m += [B(7, 12, 7, 9, 15, 9, "obsidian"), B(7.4, 15, 7.4, 8.6, 16.6, 8.6, "glow_lime")]
    for (x, z) in ((2, 7.4), (12.8, 7.4), (7.4, 2), (7.4, 12.8)):
        m += [B(x, 3, z, x + 1.2, 10, z + 1.2, "obsidian"), B(x + 0.3, 10, z + 0.3, x + 0.9, 13, z + 0.9, "glow_void")]
    m += [B(2.4, 9, 7.8, 5, 9.6, 8.2, "glow_lime"), B(11, 9, 7.8, 13.6, 9.6, 8.2, "glow_lime")]
    return m


def eternal_sapling():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 3, 15, "rock")]
    m += [B(7, 3, 7, 9, 11, 9, "copper"), B(6.5, 3, 6.5, 9.5, 4.5, 9.5, "copper")]
    for (x, z) in ((2.5, 8), (13.5, 8), (8, 2.5), (8, 13.5)):
        m += [B(min(x, 8) - 0.5 if x < 8 else 8, 3, min(z, 8) - 0.5 if z < 8 else 8, max(x, 8) + 0.5 if x > 8 else 8.6, 3.8, max(z, 8) + 0.5 if z > 8 else 8.6, "copper")]
    m += ball(8, 12.5, 8, 4.2, "olive") + ball(8, 13.2, 8, 3.0, "glow_green")
    m += [B(5, 10.5, 5, 6, 11.5, 6, "glow_gold"), B(10, 13.5, 10, 11, 14.5, 11, "glow_gold"), B(7.5, 16, 7.5, 8.5, 16.8, 8.5, "glow_white")]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m += [B(x, 2.8, z, x + 1, 8, z + 1, "celestial")]
    return m


def sixteen_chunks():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian")]
    mats = ["glow_magenta", "glow_cyan", "glow_lime", "glow_orange", "pearl", "glass_gold", "glow_purple", "red",
            "glass_slime", "glow_blue", "gold", "glow_pink", "glass_ice", "glow_red", "white", "glow_rainbow"]
    for i in range(4):
        for j in range(4):
            h = 3 + ((i * 7 + j * 5) % 5) * 1.6
            m += [B(0.5 + i * 3.8, 2, 0.5 + j * 3.8, 0.5 + i * 3.8 + 3.4, 2 + h, 0.5 + j * 3.8 + 3.4, mats[i * 4 + j])]
    m += [B(7.6, 2, 0, 8.4, 13, 16, "glow_white"), B(0, 2, 7.6, 16, 13, 8.4, "glow_white")]
    return m


def truman_show():
    m = [B(0, 0, 0, 16, 1.5, 16, "black")]
    m += [B(1, 1.5, 1, 15, 12, 15, "glass_cyan"), B(1.5, 1.5, 1.5, 14.5, 3, 14.5, "olive")]
    m += [B(3, 3, 3, 5, 6, 5, "white"), B(10, 3, 9, 12, 7, 11, "white"), B(6, 3, 10, 8, 4.5, 12, "red")]
    m += [B(1, 12, 1, 15, 13, 15, "steel"), B(7, 13, 7, 9, 16, 9, "steel")]
    m += [B(6.5, 9, 6.5, 9.5, 11.6, 9.5, "glow_white"), B(7.4, 11.6, 7.4, 8.6, 12, 8.6, "steel")]
    m += [B(12.2, 3, 0.8, 14.2, 7.5, 1, "white"), B(12.6, 3, 0.7, 13.8, 6.8, 0.85, "glow_gold")]
    m += [B(2, 13.2, 2, 4, 14.2, 4, "glow_red")]
    return m


def disco_ball():
    m = [B(0, 0, 0, 16, 2, 16, "black"), B(1, 2, 1, 15, 2.6, 15, "glow_rainbow")]
    m += [B(7.6, 13.5, 7.6, 8.4, 16, 8.4, "chrome")]
    m += ball(8, 8.5, 8, 5.5, "chrome")
    for k in range(10):
        import math
        a = k * math.pi / 5
        x, z = 8 + math.cos(a) * 5.6, 8 + math.sin(a) * 5.6
        m += [B(x - 0.4, 7.5 + (k % 3), z - 0.4, x + 0.4, 8.3 + (k % 3), z + 0.4, ["glow_magenta", "glow_cyan", "glow_gold", "glow_lime", "glow_white"][k % 5])]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        m += [B(x, 2.6, z, x + 1, 5, z + 1, "glow_white")]
    return m


BOMBS.update({
    "eternal_sapling": eternal_sapling, "sixteen_chunks": sixteen_chunks, "truman_show": truman_show, "disco_ball": disco_ball,
})

BOMBS.update({
    "exploding_cows": exploding_cows, "deadly_twister": deadly_twister, "colossal_spear": colossal_spear,
    "zombie_apocalypse": zombie_apocalypse, "ufo_invasion": ufo_invasion, "galactic_smash": galactic_smash,
    "booming_tick": booming_tick, "alien_death_ray": alien_death_ray,
})
