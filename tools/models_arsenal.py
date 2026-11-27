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


def helix_disruptor():
    # Twin barrels wound round each other, cyan and violet coils, a lens at the muzzle.
    m = cyl_z(8, 10, -10, 16, 1.6, "gunmetal")
    for i in range(7):
        z = -9 + i * 3
        a = i * 0.9
        x, y = 8 + 2.2 * _m.cos(a), 10 + 2.2 * _m.sin(a)
        x2, y2 = 8 - 2.2 * _m.cos(a), 10 - 2.2 * _m.sin(a)
        m += [B(x - 0.7, y - 0.7, z, x + 0.7, y + 0.7, z + 2, "glow_cyan"), B(x2 - 0.7, y2 - 0.7, z, x2 + 0.7, y2 + 0.7, z + 2, "glow_purple")]
    m += ring_y_frame(8, 10, -11, 3.0, "chrome", 0.6) + [B(7.2, 9.2, -12, 8.8, 10.8, -11, "glow_white")]
    m += [B(5.5, 7, 4, 10.5, 12.5, 16, "titanium"), B(6, 12.5, 6, 10, 14, 12, "screen")]
    m += [B(6.5, 7, 16, 9.5, 11, 23, "gunmetal"), grip(8, 8, 13)]
    return m, (8, 4, 13)


def graviton_lance():
    # A long dark lance with a caged black core near the tip.
    m = cyl_z(8, 10, -14, 18, 1.0, "obsidian")
    m += [B(6, 8, -12, 10, 12, -8, "steel")]
    m += ring_y_frame(8, 10, -10, 3.2, "glow_purple", 0.5) + ring_y_frame(8, 10, -7, 2.6, "glow_purple", 0.5)
    m += [B(7, 9, -11, 9, 11, -9, "black")]
    m += [B(5.8, 7.5, 0, 10.2, 12.5, 12, "gunmetal"), B(5.6, 9.5, 2, 5.8, 10.5, 10, "glow_purple"), B(10.2, 9.5, 2, 10.4, 10.5, 10, "glow_purple")]
    m += [B(6.5, 7, 12, 9.5, 11, 22, "obsidian"), grip(8, 8, 11)]
    return m, (8, 4, 11)


def sunfall_mortar():
    # A stubby wide mortar tube with a glowing sun-orb loaded in the muzzle.
    m = cyl_z(8, 11, -6, 10, 3.4, "gold")
    m += cyl_z(8, 11, -7, -5, 3.9, "gunmetal")
    m += [B(5.6, 8.6, -8, 10.4, 13.4, -6.5, "glow_orange"), B(6.6, 9.6, -8.6, 9.4, 12.4, -8, "glow_gold")]
    m += [B(5, 7, 4, 11, 9, 14, "gunmetal"), B(6, 6, 9, 10, 7, 16, "steel")]
    m += [B(6.5, 7, 14, 9.5, 10.5, 20, "gunmetal"), grip(8, 7, 13)]
    return m, (8, 3, 13)


def halo_cutter():
    # A targeting designator crowned with a ring: a heavy body, a ring emitter and a red lens.
    m = [B(5.5, 7, -4, 10.5, 13, 14, "white"), B(5.3, 9.5, -2, 5.5, 10.5, 12, "glow_red"), B(10.5, 9.5, -2, 10.7, 10.5, 12, "glow_red")]
    m += ring_y_frame(8, 10, -6, 4.4, "chrome", 0.7) + ring_y_frame(8, 10, -6, 3.4, "glow_red", 0.4)
    m += cyl_z(8, 10, -9, -4, 1.0, "gunmetal") + [B(7.3, 9.3, -10, 8.7, 10.7, -9, "glow_white")]
    m += [B(6, 13, 0, 10, 15, 8, "gunmetal"), B(6.5, 15, 2, 9.5, 15.5, 6, "screen_red")]
    m += [B(6.5, 7, 14, 9.5, 11, 22, "white"), grip(8, 8, 12)]
    return m, (8, 4, 12)


def obliterator():
    # A heavy beam projector: a prism core between cooling fins, magenta emitter.
    m = [B(5, 7, -2, 11, 13.5, 16, "gunmetal")]
    for z in (0, 3, 6, 9, 12):
        m += [B(4, 8, z, 12, 12.5, z + 1, "steel")]
    m += cyl_z(8, 10.2, -10, -2, 1.8, "chrome") + ring_y_frame(8, 10.2, -9, 2.6, "glow_magenta", 0.5)
    m += [B(7.2, 9.4, -11, 8.8, 11, -10, "glow_white"), B(7, 13.5, 2, 9, 15, 12, "glow_magenta")]
    m += [B(6.5, 7, 16, 9.5, 11, 23, "gunmetal"), grip(8, 7, 13), grip(8, 7, 3, length=4)]
    return m, (8, 3, 13)


GUNS = {"party_popper": party_popper, "airhorn_cannon": airhorn_cannon, "gravity_tether_gun": gravity_tether_gun,
        "antimatter_lance": antimatter_lance, "cataclysm_mortar": cataclysm_mortar, "rift_borer": rift_borer,
        "resonance_cannon": resonance_cannon, "solstice_sunlance": solstice_sunlance, "midas_cascade": midas_cascade,
        "judgment_engine": judgment_engine, "reality_shear_projector": reality_shear_projector,
        "helix_disruptor": helix_disruptor, "graviton_lance": graviton_lance, "sunfall_mortar": sunfall_mortar,
        "halo_cutter": halo_cutter, "obliterator": obliterator}


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


# =========================================================================== upgraded models (round shapes from voxel shells)

import math as _m


def vsphere(cx, cy, cz, r, step=1.0, mat="chrome", pick=None, shell=1.1):
    """A sphere built from small cubes on its surface (reads round, unlike a cross of boxes). pick(x, y, z) -> mat."""
    out = []
    n = int(_m.ceil(r / step))
    for i in range(-n, n + 1):
        for j in range(-n, n + 1):
            for k in range(-n, n + 1):
                x, y, z = i * step, j * step, k * step
                d = _m.sqrt(x * x + y * y + z * z)
                if r - shell * step <= d <= r:
                    h = step / 2
                    out.append(B(cx + x - h, cy + y - h, cz + z - h, cx + x + h, cy + y + h, cz + z + h, pick(i, j, k) if pick else mat))
    if r > step * 1.5:
        c = r * 0.62
        out.append(B(cx - c, cy - c, cz - c, cx + c, cy + c, cz + c, mat if not pick else pick(0, 0, 0)))
    return out


def ring_y(cx, cy, cz, r, mat, t=0.5, segs=16):
    """A thin horizontal ring of 16 bars. Block models only rotate by -45..45 in 22.5 steps, so each bar is laid
    along x or z (a bar looks the same turned 180 degrees) and tilted by at most 45."""
    out = []
    L = 2 * _m.pi * r / 16 * 1.1
    for s in range(16):
        a = s * 22.5
        x, z = cx + _m.cos(_m.radians(a)) * r, cz + _m.sin(_m.radians(a)) * r
        phi = (a + 90) % 180  # direction of the tangent, folded to 0..180
        if phi <= 45 or phi >= 135:
            tilt = phi if phi <= 45 else phi - 180
            out.append(B(x - L / 2, cy - t / 2, z - t / 2, x + L / 2, cy + t / 2, z + t / 2, mat, ("y", -tilt, [x, cy, z])))
        else:
            tilt = phi - 90
            out.append(B(x - t / 2, cy - t / 2, z - L / 2, x + t / 2, cy + t / 2, z + L / 2, mat, ("y", -tilt, [x, cy, z])))
    return out


def disco_ball_v2():
    m = [B(0, 0, 0, 16, 1.5, 16, "black")]
    floor = ["glow_magenta", "glow_cyan", "glow_gold", "glow_lime", "glow_purple", "glow_pink"]
    for i in range(4):
        for j in range(4):
            m += [B(1 + i * 3.5, 1.5, 1 + j * 3.5, 4.3 + i * 3.5, 1.9, 4.3 + j * 3.5, floor[(i + j * 2) % len(floor)] if (i + j) % 2 == 0 else "black")]
    m += [B(7.6, 13.2, 7.6, 8.4, 16, 8.4, "steel"), B(6.5, 15.4, 6.5, 9.5, 16, 9.5, "steel")]
    m += vsphere(8, 8.8, 8, 4.6, 0.92, pick=lambda i, j, k: "glow_white" if (i * 7 + j * 3 + k * 5) % 11 == 0 else ("pearl" if (i + j + k) % 2 else "chrome"))
    for (x, z, c) in ((1.5, 1.5, "glow_magenta"), (13.5, 1.5, "glow_cyan"), (1.5, 13.5, "glow_gold"), (13.5, 13.5, "glow_lime")):
        m += [B(x - 0.8, 1.9, z - 0.8, x + 0.8, 4.4, z + 0.8, "black"), B(x - 0.6, 4.4, z - 0.6, x + 0.6, 5, z + 0.6, c)]
    return m


def main_stage():
    m = [B(0, 0, 0, 16, 1, 16, "black")]
    m += [B(1, 1, 3, 15, 4, 15, "copper"), B(1, 1, 3, 15, 4, 3.4, "black")]
    for x in (3, 6, 9, 12):
        m += [B(x, 3.2, 2.9, x + 1, 3.8, 3.0, "glow_gold")]
    m += [B(5.5, 1, 1, 10.5, 2, 3, "copper"), B(6, 2, 1.5, 10, 3, 3, "copper")]
    m += [B(1, 4, 13.5, 15, 13, 15, "crimson")]
    for x, w in ((1, 3.5), (11.5, 3.5)):
        m += [B(x, 4, 4, x + w, 12, 5.5, "red"), B(x + 0.6, 4, 5.5, x + w - 0.6, 12, 7, "red")]
    m += [B(0.5, 4, 3.5, 1.5, 14.5, 4.5, "black"), B(14.5, 4, 3.5, 15.5, 14.5, 4.5, "black"),
          B(0.5, 12.5, 3.5, 15.5, 14.5, 4.5, "black"), B(0.6, 12.2, 3.4, 15.4, 12.5, 4.6, "gold")]
    m += [B(3, 14.5, 3.6, 13, 16.5, 4.4, "black")]
    for x in range(4, 13, 2):
        m += [B(x, 15, 3.4, x + 1, 16, 3.6, "glow_gold")]
    m += [B(7, 4, 9, 9, 5, 11, "glow_white"), B(7.5, 5, 9.5, 8.5, 7.5, 10.5, "white")]
    return m


def alien_death_ray_v2():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 2.6, 15, "glow_void")]
    m += cyl_y(8, 8, 2.6, 5, 6.0, "obsidian") + vsphere(8, 6.4, 8, 3.8, 0.95, "titanium")
    m += [B(3.5, 4.6, 3.5, 12.5, 5.2, 12.5, "glow_lime")]
    # The barrel, raised 45 degrees, with coils and a three-prong emitter.
    rot = ("x", -45, [8, 7, 8])
    m += [B(6.6, 7, 6.6, 9.4, 18, 9.4, "obsidian", rot)]
    for y in (9.5, 12, 14.5):
        m += [B(6.1, y, 6.1, 9.9, y + 1, 9.9, "glow_lime", rot)]
    for (x, z) in ((6.0, 8), (10.0, 8), (8, 6.0)):
        m += [B(x - 0.4, 17.5, z - 0.4, x + 0.4, 20.5, z + 0.4, "titanium", rot)]
    m += [B(7.4, 19, 7.4, 8.6, 20.2, 8.6, "glow_white", rot)]
    for (x, z) in ((1.2, 1.2), (13.6, 1.2), (1.2, 13.6), (13.6, 13.6)):
        m += [B(x, 2.6, z, x + 1.2, 6.5, z + 1.2, "obsidian"), B(x + 0.3, 6.5, z + 0.3, x + 0.9, 7.2, z + 0.9, "glow_lime")]
    return m


def galactic_smash_v2():
    m = [B(0, 0, 0, 16, 2, 16, "rock"), B(1, 2, 1, 15, 2.6, 15, "obsidian")]
    for (x0, z0, x1, z1, rot) in ((2, 7.6, 14, 8.4, 0), (2, 7.6, 14, 8.4, 45), (2, 7.6, 14, 8.4, -45), (7.6, 2, 8.4, 14, 0)):
        m += [B(x0, 2.6, z0, x1, 2.8, z1, "glow_orange", ("y", rot, [8, 2.7, 8]))]
    # A void gauntlet slamming down: knuckles, fingers, cracked starlight plates, a burning wrist band.
    m += [B(3.5, 3, 4.5, 12.5, 7.5, 11.5, "black")]
    for x in (3.6, 5.9, 8.2, 10.5):
        m += [B(x, 2.6, 3.8, x + 2.0, 4.6, 6.0, "gunmetal"), B(x + 0.2, 2.8, 3.6, x + 1.8, 4.4, 3.8, "black")]
    m += [B(2.6, 3.5, 6, 3.6, 6.5, 10, "gunmetal")]
    m += [B(4.5, 7.5, 5.5, 11.5, 15, 10.5, "black"), B(4.2, 9.5, 5.2, 11.8, 11, 10.8, "glow_orange"), B(4.2, 13, 5.2, 11.8, 13.6, 10.8, "gold")]
    for (x, y, z, h) in ((5, 4, 4.4, 2.6), (9, 5, 4.4, 2), (6.5, 8, 5.4, 4), (10.4, 12, 5.4, 2.5), (4.4, 6, 8, 3)):
        m += [B(x, y, z - 0.08, x + 0.35, y + h, z, "glow_ice")]
    m += [B(5.5, 15, 6.5, 10.5, 16, 9.5, "obsidian")]
    return m


def big_bang_v2():
    m = [B(0, 0, 0, 16, 1.5, 16, "black"), B(1, 1.5, 1, 15, 2, 15, "celestial")]
    m += [B(7.6, 2, 7.6, 8.4, 4, 8.4, "steel")]
    m += vsphere(8, 9, 8, 2.0, 0.8, "glow_white")
    for r, y, c in ((3.6, 9, "glow_cyan"), (5.4, 9.4, "glow_purple"), (7.0, 8.6, "glow_gold")):
        m += ring_y(8, y, 8, r, c, 0.25, 20)
    for (a, r, y, rad, mat) in ((0.4, 3.6, 9, 0.8, "glow_blue"), (2.6, 5.4, 9.4, 1.1, "rock"), (4.4, 7.0, 8.6, 1.4, "glow_orange"), (1.6, 7.0, 8.6, 0.7, "pearl")):
        x, z = 8 + _m.cos(a) * r, 8 + _m.sin(a) * r
        m += vsphere(x, y, z, rad, rad * 0.7, mat)
    for (x, y, z) in ((2, 13, 3), (13, 14, 12), (4, 5, 13), (12, 4, 2.5), (14, 11, 6)):
        m += [B(x - 0.25, y - 0.25, z - 0.25, x + 0.25, y + 0.25, z + 0.25, "glow_white")]
    return m


def chrono_break_v2():
    m = [B(1, 0, 1, 15, 1.5, 15, "gold"), B(1, 14.5, 1, 15, 16, 15, "gold")]
    for (x, z) in ((1.5, 1.5), (13.3, 1.5), (1.5, 13.3), (13.3, 13.3)):
        m += [B(x, 1.5, z, x + 1.2, 14.5, z + 1.2, "copper")]
    # The hourglass: two glass cones meeting at a waist, glowing sand running through.
    for k in range(6):
        s = 5.6 - k * 0.9
        m += [B(8 - s, 1.5 + k, 8 - s, 8 + s, 2.5 + k, 8 + s, "glass_ice"), B(8 - s, 13.5 - k, 8 - s, 8 + s, 14.5 - k, 8 + s, "glass_ice")]
    m += [B(4.6, 1.5, 4.6, 11.4, 3.5, 11.4, "glow_gold"), B(6.5, 3.5, 6.5, 9.5, 4.5, 9.5, "glow_gold")]
    m += [B(7.6, 4.5, 7.6, 8.4, 11, 8.4, "glow_gold"), B(5.6, 11.5, 5.6, 10.4, 12.5, 10.4, "glow_gold")]
    m += ring_y(8, 8, 8, 6.6, "glow_ice", 0.4, 20)
    return m


def zeropoint_v2():
    m = [B(1, 0, 1, 15, 1.5, 15, "chrome"), B(2, 1.5, 2, 14, 2, 14, "glow_ice")]
    m += vsphere(8, 8.5, 8, 1.4, 0.7, "black")
    m += ring_y(8, 8.5, 8, 3.0, "glow_white", 0.35, 16) + ring_y(8, 8.5, 8, 5.6, "glow_ice", 0.3, 20)
    for (x, y, z, h, rot) in ((3, 2, 4, 7, ("z", 22.5, [3.5, 2, 4.5])), (12, 2, 3.5, 6, ("z", -22.5, [12.5, 2, 4])),
                              (4, 2, 12, 5, ("x", 22.5, [4.5, 2, 12.5])), (11.5, 2, 12, 8, ("x", -22.5, [12, 2, 12.5])), (8, 2, 2.4, 4, None)):
        m += [B(x, y, z, x + 1.2, y + h, z + 1.2, "glass_ice", rot), B(x + 0.35, y + h, z + 0.35, x + 0.85, y + h + 1.2, z + 0.85, "glow_ice", rot)]
    m += [B(7.7, 1.5, 7.7, 8.3, 15.5, 8.3, "glow_ice")]
    return m


def last_light_v2():
    m = [B(0, 0, 0, 16, 1.5, 16, "pearl"), B(2, 1.5, 2, 14, 2.5, 14, "celestial")]
    for (x, z) in ((2.5, 2.5), (12.5, 2.5), (2.5, 12.5), (12.5, 12.5)):
        m += [B(x, 2.5, z, x + 1, 7, z + 1, "pearl", ("y", 45, [x + 0.5, 4, z + 0.5])), B(x + 0.2, 7, z + 0.2, x + 0.8, 8, z + 0.8, "glow_white")]
    m += vsphere(8, 8.5, 8, 4.0, 0.9, "glow_white")
    # Corona spikes in every direction.
    for ang in (0, 45, -45, 90):
        m += [B(7.7, 8.2, 2.2, 8.3, 8.8, 13.8, "glow_gold", ("y", ang, [8, 8.5, 8]))]
    for ang in (45, -45):
        m += [B(7.7, 2.6, 7.7, 8.3, 14.4, 8.3, "glow_gold", ("z", ang, [8, 8.5, 8])), B(7.7, 2.6, 7.7, 8.3, 14.4, 8.3, "glow_gold", ("x", ang, [8, 8.5, 8]))]
    m += [B(7.7, 2.6, 7.7, 8.3, 14.4, 8.3, "glow_gold")]
    m += ring_y(8, 8.5, 8, 6.2, "glow_white", 0.3, 20)
    return m


def dawnstar_v2():
    m = [B(0, 0, 0, 16, 1.5, 16, "white"), B(1, 1.5, 1, 15, 3, 15, "gold")]
    m += vsphere(8, 3.5, 8, 5.5, 1.0, "glow_gold")
    m += [B(1.5, 3, 1.5, 14.5, 4, 14.5, "gold")]
    for a in (-45, -22.5, 0, 22.5, 45):
        m += [B(7.6, 4, 7.6, 8.4, 15, 8.4, "glow_white", ("z", a, [8, 4, 8]))]
    m += [B(2, 3, 2, 3.2, 6, 3.2, "gold"), B(12.8, 3, 12.8, 14, 6, 14, "gold"), B(2, 3, 12.8, 3.2, 6, 14, "gold"), B(12.8, 3, 2, 14, 6, 3.2, "gold")]
    return m


def antimatter_v2():
    m = [B(1, 0, 1, 15, 2, 15, "gunmetal"), B(1.5, 2, 1.5, 14.5, 2.4, 14.5, "hazard")]
    for (x, z) in ((2, 7.2), (12.4, 7.2)):
        m += [B(x, 2.4, z, x + 1.6, 14, z + 1.6, "chrome")]
    for y in (5, 11.5):
        m += ring_y(8, y, 8, 4.4, "glow_magenta", 0.7, 18) + ring_y(8, y, 8, 5.2, "gunmetal", 0.5, 18)
    m += vsphere(8, 8.3, 8, 2.4, 0.8, "glow_magenta")
    m += [B(3, 13.6, 7.2, 13, 14.6, 8.8, "gunmetal"), B(7, 14.6, 7, 9, 15.6, 9, "glow_white")]
    return m


def nova_v2():
    m = [B(1, 0, 1, 15, 2, 15, "gunmetal"), B(1.5, 2, 1.5, 14.5, 2.6, 14.5, "fatman")]
    m += vsphere(8, 8.5, 8, 4.2, 0.9, pick=lambda i, j, k: "glow_white" if abs(i) + abs(j) + abs(k) <= 2 else ("glow_orange" if (i + j * 2 + k) % 3 else "glow_gold"))
    for rot in (0, 45, -45):
        m += [B(7.7, 8.2, 1.5, 8.3, 8.8, 14.5, "glow_gold", ("y", rot, [8, 8.5, 8]))]
    for (x, z) in ((2, 2), (12.8, 2), (2, 12.8), (12.8, 12.8)):
        m += [B(x, 2.6, z, x + 1.2, 14, z + 1.2, "fatman", ("y", 45, [x + 0.6, 8, z + 0.6]))]
    m += ring_y(8, 14, 8, 6.0, "gunmetal", 0.6, 16) + [B(7, 14, 7, 9, 15.5, 9, "glow_orange")]
    return m


BOMBS.update({
    "disco_ball": disco_ball_v2, "truman_show": main_stage, "alien_death_ray": alien_death_ray_v2, "galactic_smash": galactic_smash_v2,
    "big_bang": big_bang_v2, "chrono_break": chrono_break_v2, "zeropoint": zeropoint_v2, "last_light": last_light_v2,
    "dawnstar_payload": dawnstar_v2,
})


# ---------------------------------------------------------------- the strong five and the oddities

def lidar_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "gunmetal"), B(1, 1.5, 1, 15, 2, 15, "black")]
    for (x, z) in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):
        m += [B(x, 2, z, x + 1.5, 3, z + 1.5, "hazard")]
    m += cyl_y(8, 8, 2, 4, 4.5, "steel") + [B(7.2, 4, 7.2, 8.8, 10, 8.8, "gunmetal")]
    # The scanner head: a drum with a glowing lens band and a rotating mirror on top.
    m += cyl_y(8, 8, 10, 13.5, 3.2, "titanium") + cyl_y(8, 8, 11.2, 12.4, 3.4, "glow_cyan")
    m += [B(6.5, 13.5, 6.5, 9.5, 14.2, 9.5, "black"), B(7.5, 14.2, 5, 8.5, 15.6, 11, "chrome", ("y", 45, [8, 14.9, 8]))]
    # The laser fan, frozen mid-sweep.
    for a in (-45, 0, 45):
        m += [B(7.9, 11.6, 11.4, 8.1, 11.9, 16, "glow_cyan", ("y", a, [8, 11.75, 8]))]
    return m


def shadow_void():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 2.4, 15, "glow_void"), B(2, 2.4, 2, 14, 3, 14, "obsidian")]
    for (x, z) in ((1.5, 1.5), (13, 1.5), (1.5, 13), (13, 13)):
        m += [B(x, 3, z, x + 1.5, 7 + (x + z) % 3, z + 1.5, "obsidian"), B(x + 0.3, 7 + (x + z) % 3, z + 0.3, x + 1.2, 7.6 + (x + z) % 3, z + 1.2, "glow_purple")]
    m += vsphere(8, 9.5, 8, 4.4, 0.95, "black", pick=lambda i, j, k: "glow_void" if (i * 5 + j * 3 + k) % 13 == 0 else "black")
    m += ring_y(8, 9.5, 8, 6.2, "glow_purple", 0.45)
    return m


def spectral_ice():
    m = [B(0, 0, 0, 16, 1.2, 16, "white"), B(0.5, 1.2, 0.5, 15.5, 1.6, 15.5, "ice")]
    # Spikes along the square footprint, leaning inward into one point.
    for (x, z, ax, a) in ((2, 2, "x", 22.5), (2, 12, "x", -22.5), (12, 2, "x", 22.5), (12, 12, "x", -22.5),
                          (2, 7, "z", 22.5), (12, 7, "z", -22.5), (7, 2, "x", 22.5), (7, 12, "x", -22.5)):
        m += [B(x, 1.6, z, x + 2, 11, z + 2, "ice", (ax, a, [x + 1, 1.6, z + 1]))]
    m += [B(6.6, 1.6, 6.6, 9.4, 13, 9.4, "ice"), B(7.2, 13, 7.2, 8.8, 15.4, 8.8, "glow_ice"), B(7.6, 15.4, 7.6, 8.4, 16, 8.4, "glow_white")]
    return m


def termite_domain():
    m = [B(0, 0, 0, 16, 1, 16, "mud")]
    # The mound: stacked, narrowing tiers with buttresses, plus two side spires.
    for i, (r, y0, y1) in enumerate(((6.5, 1, 4), (5.4, 4, 7), (4.2, 7, 10), (3, 10, 13), (1.8, 13, 15.5))):
        m += [B(8 - r, y0, 8 - r * 0.7, 8 + r, y1, 8 + r * 0.7, "mud" if i % 2 == 0 else "rock"),
              B(8 - r * 0.7, y0, 8 - r, 8 + r * 0.7, y1, 8 + r, "mud")]
    m += [B(7.4, 15.5, 7.4, 8.6, 16, 8.6, "mud")]
    for (x, z, h) in ((1.5, 2, 9), (12.5, 11.5, 7)):
        m += [B(x, 1, z, x + 2.2, h * 0.6, z + 2.2, "mud"), B(x + 0.5, h * 0.6, z + 0.5, x + 1.7, h, z + 1.7, "rock")]
    m += [B(7, 1, 1.4, 9, 2.8, 2, "black"), B(1.4, 1, 7, 2, 2.8, 9, "black")]
    m += [B(4 + i * 2.2, 1, 3 + (i * 5) % 9, 4.6 + i * 2.2, 1.3, 3.4 + (i * 5) % 9, "black") for i in range(5)]
    return m


def sun_disappears():
    m = [B(0, 0, 0, 16, 2, 16, "obsidian"), B(1, 2, 1, 15, 2.5, 15, "gold")]
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        m += [B(x, 2.5, z, x + 2, 5, z + 2, "obsidian"), B(x + 0.5, 5, z + 0.5, x + 1.5, 5.6, z + 1.5, "glow_gold")]
    # An eclipsed sun: the black disc over a blazing corona.
    m += vsphere(8, 9.5, 8, 4.8, 0.95, "glow_gold", pick=lambda i, j, k: "glow_orange" if (i + j * 2 + k) % 5 == 0 else "glow_gold")
    m += [B(3.6, 5.2, 3.0, 12.4, 13.8, 3.5, "black"), B(4.6, 4.2, 2.9, 11.4, 14.8, 3.4, "black"), B(2.6, 6.2, 2.9, 13.4, 12.8, 3.4, "black")]
    for a in (0, 45, -45):
        m += [B(7.6, 9.1, 0, 8.4, 9.9, 16, "glow_white", ("y", a, [8, 9.5, 8]))]
    m += [B(7.6, 3, 7.6, 8.4, 16, 8.4, "glow_white")]
    return m


def sonar_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "olive"), B(1, 1.5, 1, 15, 2, 15, "black")]
    m += cyl_y(8, 8, 2, 5, 5.5, "steel") + cyl_y(8, 8, 5, 5.6, 5.8, "glow_lime")
    # A dish pointing up, with the transducer in the middle.
    m += cyl_y(8, 8, 5.6, 7, 6.5, "steel") + cyl_y(8, 8, 7, 7.6, 7.5, "gunmetal")
    m += [B(7.3, 7.6, 7.3, 8.7, 12, 8.7, "gunmetal"), B(6.6, 12, 6.6, 9.4, 13.2, 9.4, "glow_lime")]
    for r in (3.6, 5.6):
        m += ring_y(8, 7.7, 8, r, "glow_lime", 0.3)
    return m


def pixel_bomb():
    m = [B(0, 0, 0, 16, 1, 16, "black")]
    pal = ["glow_magenta", "glow_cyan", "glow_gold", "glow_lime", "glow_red", "glow_blue"]
    # A blocky 8-bit bomb: stepped round body, fuse and spark.
    for y, half in ((1, 4), (3, 6), (5, 6.5), (8, 6.5), (11, 6), (13, 4)):
        m += [B(8 - half, y, 8 - half, 8 + half, y + 2 if y != 5 else y + 3, 8 + half, "black")]
    m += [B(4.5, 9, 1.4, 6.5, 11, 1.6, "white"), B(4.5, 11, 1.4, 5.5, 12, 1.6, "white")]
    m += [B(7, 15, 7, 9, 16, 9, "gunmetal"), B(8.5, 15.5, 8.5, 10.5, 16.5, 10.5, "glow_gold")]
    for i in range(6):
        x, z = 1 + (i * 5) % 14, 1 + (i * 3) % 14
        m += [B(x, 1, z, x + 1, 2, z + 1, pal[i])]
    return m


def magnetar():
    m = [B(0, 0, 0, 16, 1.5, 16, "steel")]
    # Two magnet horns, red and grey, cradling the star.
    m += [B(1, 1.5, 6, 4, 12, 10, "red"), B(12, 1.5, 6, 15, 12, 10, "steel"), B(1, 1.5, 6, 15, 4, 10, "gunmetal"),
          B(1, 10, 6, 4, 12, 10, "white"), B(12, 10, 6, 15, 12, 10, "white")]
    m += vsphere(8, 9, 8, 2.4, 1.0, "glow_white")
    for a in (-22.5, 22.5):
        m += [B(7.6, 2, 7.6, 8.4, 16, 8.4, "glow_cyan", ("z", a, [8, 9, 8]))]
    m += ring_y(8, 9, 8, 4.2, "glow_blue", 0.35)
    return m


def ink_bomb():
    m = [B(0, 0, 0, 16, 1.2, 16, "white")]
    # A cartoon bomb: round black body, white highlight, fuse cap, and an ink splash at its foot.
    m += vsphere(8, 7.5, 8, 5.6, 1.0, "black")
    m += [B(4.6, 9.5, 2.6, 6.4, 11.3, 3.0, "white"), B(6.6, 12.6, 6.6, 9.4, 14.4, 9.4, "gunmetal"),
          B(7.6, 14.4, 7.6, 8.4, 16, 8.4, "olive"), B(7.2, 15.6, 7.2, 8.8, 16.6, 8.8, "glow_gold")]
    for (x, z, w) in ((1, 3, 3), (12, 1.5, 2.5), (11, 12, 3.5), (2, 12, 2)):
        m += [B(x, 1.2, z, x + w, 1.5, z + w * 0.8, "black")]
    m += [B(0.5, 1.2, 0.5, 4, 1.6, 1.5, "yellow"), B(12, 1.2, 14.5, 15.5, 1.6, 15.5, "red")]
    return m


def aurora_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "white"), B(1, 1.5, 1, 15, 2, 15, "glass_cyan")]
    m += cyl_y(8, 8, 2, 4, 3, "steel") + [B(7.4, 4, 7.4, 8.6, 8, 8.6, "steel")]
    # Rippling curtains of light.
    cols = ["glow_lime", "glow_cyan", "glow_purple", "glow_green"]
    for i in range(6):
        x = 1.5 + i * 2.3
        h = 9 + (i % 3) * 1.5
        m += [B(x, 8, 3 + (i % 2) * 1.5, x + 2.1, h + 4, 3.4 + (i % 2) * 1.5, cols[i % 4]),
              B(x, h + 4, 3 + (i % 2) * 1.5, x + 2.1, h + 6, 3.4 + (i % 2) * 1.5, "glow_purple")]
    m += vsphere(8, 9, 8, 1.6, 1.0, "glow_white")
    return m


def seasons_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "copper")]
    # A four-quarter clock base (spring, summer, autumn, winter) and an hourglass of seasons.
    for (x0, z0, c) in ((1, 1, "pink"), (8, 1, "olive"), (1, 8, "copper"), (8, 8, "white")):
        m += [B(x0, 1.5, z0, x0 + 7, 2.2, z0 + 7, c)]
    m += [B(4, 2.2, 4, 12, 3.2, 12, "gold"), B(4, 14, 4, 12, 15, 12, "gold")]
    for (x, z) in ((4, 4), (11, 4), (4, 11), (11, 11)):
        m += [B(x, 3.2, z, x + 1, 14, z + 1, "gold")]
    m += [B(5.5, 3.2, 5.5, 10.5, 5.5, 10.5, "glass_gold"), B(6.8, 5.5, 6.8, 9.2, 8, 9.2, "glass_gold"),
          B(7.4, 8, 7.4, 8.6, 9.2, 8.6, "glow_gold"), B(6.8, 9.2, 6.8, 9.2, 11.5, 9.2, "glass_gold"), B(5.5, 11.5, 5.5, 10.5, 14, 10.5, "glass_gold")]
    m += [B(6, 3.2, 6, 10, 4.2, 10, "glow_gold")]
    return m


def prism_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "white"), B(1, 1.5, 1, 15, 2, 15, "black")]
    cols = ["glow_red", "glow_orange", "glow_gold", "glow_lime", "glow_cyan", "glow_blue", "glow_purple"]
    for i, c in enumerate(cols):
        m += [B(1 + i * 2, 2, 1, 2.6 + i * 2, 2.4, 15, c)]
    m += [B(7.4, 2, 7.4, 8.6, 5, 8.6, "steel")]
    # A triangular glass prism (three rotated slabs) with a white beam in.
    for a in (0, 45, -45):
        m += [B(4, 5, 7, 12, 13, 9, "glass_cyan", ("y", a, [8, 9, 8]))]
    m += [B(7.6, 13, 7.6, 8.4, 16, 8.4, "glow_white")]
    return m


def geode_bomb():
    m = [B(0, 0, 0, 16, 1.5, 16, "rock")]
    # A cracked-open geode: basalt crust, white calcite, amethyst crystals inside.
    m += vsphere(8, 7, 8, 6.2, 1.0, "black", pick=lambda i, j, k: "rock" if (i + j + k) % 3 else "black")
    m += [B(3.5, 8, 3.5, 12.5, 13.6, 12.5, "pearl"), B(4.3, 8.6, 4.3, 11.7, 14, 11.7, "amethyst")]
    for (x, z, h) in ((5, 5, 4), (9, 6, 5), (6, 9.5, 3.5), (10, 10, 4.5), (7.5, 7.5, 6)):
        m += [B(x, 12, z, x + 1.4, 12 + h, z + 1.4, "amethyst"), B(x + 0.3, 12 + h, z + 0.3, x + 1.1, 12.6 + h, z + 1.1, "glow_purple")]
    return m


def fossil_bomb():
    m = [B(0, 0, 0, 16, 2, 16, "rock"), B(1, 2, 1, 15, 2.6, 15, "mud")]
    # A skull in the dirt: brow, eye sockets glowing, jaw full of teeth.
    m += [B(3, 2.6, 4, 13, 9, 12, "bone"), B(2.5, 7, 3.5, 13.5, 10, 12.5, "bone"), B(4, 10, 5, 12, 11, 11, "bone")]
    m += [B(4, 6.5, 3.4, 6.5, 8.5, 3.6, "glow_red"), B(9.5, 6.5, 3.4, 12, 8.5, 3.6, "glow_red")]
    m += [B(5, 3, 3.4, 11, 5.6, 3.6, "black")]
    for x in range(5, 11):
        m += [B(x + 0.15, 4.4, 3.2, x + 0.75, 5.6, 3.5, "bone"), B(x + 0.15, 3, 3.2, x + 0.75, 4, 3.5, "bone")]
    m += [B(1, 2.6, 13, 6, 3.6, 14, "bone"), B(10, 2.6, 13.5, 15, 3.4, 14.3, "bone")]
    return m


def lava_lamp():
    m = [B(3, 0, 3, 13, 3, 13, "copper"), B(4, 3, 4, 12, 4, 12, "copper")]
    m += [B(4.5, 4, 4.5, 11.5, 13, 11.5, "glass_gold")]
    for (x, y, z, r) in ((6, 5, 6, 1.6), (8.5, 8, 7.5, 1.8), (6.5, 11, 8.5, 1.3), (9, 5.5, 9, 1.1)):
        m += [B(x - r, y - r, z - r, x + r, y + r, z + r, "glow_orange" if r > 1.4 else "glow_pink")]
    m += [B(4, 13, 4, 12, 14.5, 12, "copper"), B(5.5, 14.5, 5.5, 10.5, 16, 10.5, "copper")]
    return m


def ion_scythe():
    # A ground beacon for the orbital cannon: a steel plinth, a ring of capacitors and a lens that points up.
    m = [B(1, 0, 1, 15, 1.5, 15, "gunmetal"), B(2, 1.5, 2, 14, 2.2, 14, "steel")]
    for (x, z) in ((2, 2), (12, 2), (2, 12), (12, 12)):
        m += cyl_y(x + 1, z + 1, 2.2, 7, 0.9, "titanium") + [B(x + 0.4, 7, z + 0.4, x + 1.6, 7.6, z + 1.6, "glow_cyan")]
    m += cyl_y(8, 8, 2.2, 9, 3.2, "steel") + cyl_y(8, 8, 9, 10, 4.2, "gunmetal")
    m += ring_y(8, 10.3, 8, 4.0, "glow_cyan", 0.4) + ring_y(8, 11.6, 8, 2.8, "glow_blue", 0.35)
    m += cyl_y(8, 8, 10, 13.5, 1.4, "chrome") + [B(7.2, 13.5, 7.2, 8.8, 16, 8.8, "glow_white")]
    m += [B(7.6, 2.2, 1.2, 8.4, 9, 1.8, "glow_cyan"), B(1.2, 2.2, 7.6, 1.8, 9, 8.4, "glow_cyan")]
    return m


def resonance_cascade():
    # A tuning fork on a resonator: two tall tines on a chrome block, rings of light round the stem.
    m = [B(1, 0, 1, 15, 1.4, 15, "gunmetal"), B(2, 1.4, 2, 14, 2, 14, "steel")]
    m += cyl_y(8, 8, 2, 5, 3.2, "chrome") + ring_y(8, 4.2, 8, 3.6, "glow_cyan", 0.35)
    m += [B(7, 5, 6, 9, 7, 10, "steel")]
    m += [B(7.2, 7, 5.6, 8.8, 15.6, 6.8, "chrome"), B(7.2, 7, 9.2, 8.8, 15.6, 10.4, "chrome")]
    m += [B(7.4, 15.6, 5.8, 8.6, 16, 6.6, "glow_cyan"), B(7.4, 15.6, 9.4, 8.6, 16, 10.2, "glow_cyan")]
    for y in (9, 12):
        m += ring_y(8, y, 8, 3.0, "glow_white", 0.25)
    return m


def tesla_dominion():
    # A small tesla tower: braced copper core, coils, and a fat torus crown.
    m = [B(1, 0, 1, 15, 1.2, 15, "gunmetal")]
    for (x, z) in ((2, 2), (12.6, 2), (2, 12.6), (12.6, 12.6)):
        m += [B(x, 1.2, z, x + 1.4, 3, z + 1.4, "steel")]
    m += cyl_y(8, 8, 1.2, 12.5, 1.5, "copper")
    for y in (3.5, 5.5, 7.5, 9.5):
        m += cyl_y(8, 8, y, y + 0.8, 2.3 - y * 0.06, "copper")
    m += ring_y(8, 13.6, 8, 4.2, "steel", 0.9, segs=20) + ring_y(8, 13.6, 8, 3.4, "glow_cyan", 0.3)
    m += [B(7.2, 12.5, 7.2, 8.8, 14.5, 8.8, "chrome"), B(7.5, 14.5, 7.5, 8.5, 16, 8.5, "glow_white")]
    return m


def dyson_lance():
    # A focusing mirror array: a ring of angled gold panels around a lens, aimed at the sky.
    m = [B(1, 0, 1, 15, 1.2, 15, "gunmetal")]
    m += cyl_y(8, 8, 1.2, 7, 1.0, "steel")
    for i in range(8):
        a = i * _m.pi / 4
        x, z = 8 + 5.2 * _m.cos(a), 8 + 5.2 * _m.sin(a)
        m += [B(x - 1.4, 6.5, z - 1.4, x + 1.4, 7.2, z + 1.4, "gold", ("y", -i * 45, [x, 6.8, z]))]
        m += [B(x - 0.3, 1.2, z - 0.3, x + 0.3, 6.5, z + 0.3, "titanium")]
    m += vsphere(8, 9, 8, 2.0, 0.9, "glass_gold")
    m += [B(7.5, 11, 7.5, 8.5, 16, 8.5, "glow_gold")]
    return m


def gravity_killswitch():
    # A gravity generator: a heavy base, a floating cracked rock held in a cage of rings, off its pedestal.
    m = [B(1, 0, 1, 15, 2, 15, "obsidian"), B(2, 2, 2, 14, 2.4, 14, "glow_purple")]
    m += ring_y(8, 6, 8, 5.4, "steel", 0.6, segs=22) + ring_y(8, 11, 8, 5.0, "steel", 0.6, segs=22)
    m += [B(5.2, 6.6, 5.2, 10.8, 10.6, 10.8, "rock"), B(6, 10.6, 6.4, 9.4, 12, 9.6, "rock"), B(9, 5.4, 9, 11, 6.6, 11, "rock")]
    for (x, z) in ((2, 2), (12.6, 2), (2, 12.6), (12.6, 12.6)):
        m += [B(x, 2.4, z, x + 1.4, 14, z + 1.4, "gunmetal"), B(x + 0.2, 14, z + 0.2, x + 1.2, 14.8, z + 1.2, "glow_purple")]
    return m


def seismic_tsunami():
    # A piston hammer: a frame, a thick ram and a cracked anvil plate under it.
    m = [B(0, 0, 0, 16, 1.6, 16, "rock"), B(2, 1.6, 2, 14, 2.4, 14, "steel")]
    for (x, z) in ((1.5, 1.5), (12.5, 1.5), (1.5, 12.5), (12.5, 12.5)):
        m += [B(x, 2.4, z, x + 2, 14, z + 2, "gunmetal")]
    m += [B(1.5, 14, 1.5, 14.5, 16, 14.5, "gunmetal")]
    m += cyl_y(8, 8, 6, 14, 3.4, "chrome") + cyl_y(8, 8, 4, 6, 4.4, "steel")
    m += [B(3, 2.4, 7.6, 13, 2.6, 8.4, "glow_orange"), B(7.6, 2.4, 3, 8.4, 2.6, 13, "glow_orange")]
    return m


BOMBS.update({
    "ion_scythe": ion_scythe, "resonance_cascade": resonance_cascade, "tesla_dominion": tesla_dominion,
    "dyson_lance": dyson_lance, "gravity_killswitch": gravity_killswitch, "seismic_tsunami": seismic_tsunami,
})


BOMBS.update({
    "lidar_bomb": lidar_bomb, "shadow_void": shadow_void, "spectral_ice": spectral_ice, "termite_domain": termite_domain,
    "sun_disappears": sun_disappears, "sonar_bomb": sonar_bomb, "pixel_bomb": pixel_bomb, "magnetar": magnetar,
    "ink_bomb": ink_bomb, "aurora_bomb": aurora_bomb, "seasons_bomb": seasons_bomb, "prism_bomb": prism_bomb,
    "geode_bomb": geode_bomb, "fossil_bomb": fossil_bomb, "lava_lamp": lava_lamp,
})


# =========================================================================== the wild ones

def goo_bomb():
    # A cracked containment canister with alien goo bulging out of the top and running down its sides.
    m = [B(2, 0, 2, 14, 1.4, 14, "gunmetal"), B(3, 1.4, 3, 13, 2, 13, "hazard")]
    m += cyl_y(8, 8, 2, 8.5, 4.6, "steel") + ring_y(8, 5.2, 8, 5.0, "glow_lime", 0.35)
    m += vsphere(8, 10.4, 8, 4.4, 1.0, "glass_slime")
    m += vsphere(5.2, 9.0, 6.0, 2.0, 1.0, "glass_slime") + vsphere(10.8, 9.6, 10.2, 1.8, 1.0, "glass_slime")
    m += [B(6.2, 9.6, 6.2, 9.8, 13.2, 9.8, "glow_lime")]
    for (x, z, h) in ((3.2, 6.5, 4.0), (12.4, 9.5, 5.0), (7.0, 12.6, 3.0)):
        m += [B(x, 8.5 - h, z, x + 0.9, 8.5, z + 0.9, "glass_slime")]
    return m


def cactus_bomb():
    # A saguaro on a sandy pot, its arms raised and its spines pale against the green.
    m = [B(2, 0, 2, 14, 2.4, 14, "sand"), B(3, 2.4, 3, 13, 3, 13, "mud")]
    m += [B(6, 3, 6, 10, 15, 10, "cactus"), B(6.5, 15, 6.5, 9.5, 16, 9.5, "cactus")]
    m += [B(2, 7, 7, 6, 9.4, 9, "cactus"), B(2, 9.4, 7, 4, 13, 9, "cactus")]
    m += [B(10, 9, 7, 14, 11.2, 9, "cactus"), B(12, 11.2, 7, 14, 14, 9, "cactus")]
    for (x, y, z) in ((5.6, 5, 7.5), (10, 6.5, 8.2), (7.5, 8, 5.6), (8.2, 10.5, 10), (5.6, 12.5, 8.6), (10, 13.5, 7.2),
                      (1.6, 11, 7.8), (14, 12.5, 8.1), (7.6, 4.2, 10)):
        m += [B(x, y, z, x + 0.4, y + 0.4, z + 0.4, "bone")]
    m += [B(7.2, 16, 7.2, 8.8, 16.6, 8.8, "pink")]
    return m


def blackforge():
    # An anvil of black iron on a glowing forge bed, a hammer resting across it.
    m = [B(1, 0, 1, 15, 1.6, 15, "obsidian"), B(2, 1.6, 2, 14, 2, 14, "glow_orange")]
    m += [B(3, 2, 5, 13, 4, 11, "black"), B(5.5, 4, 6.2, 10.5, 7, 9.8, "black"), B(2, 7, 5.5, 13, 10, 10.5, "gunmetal")]
    m += [B(13, 7.6, 6.6, 15.4, 9.6, 9.4, "gunmetal"), B(15.4, 8.1, 7.3, 16, 9.1, 8.7, "gunmetal")]
    m += [B(5.4, 4.5, 6.15, 10.6, 4.9, 6.2, "glow_orange"), B(5.4, 4.5, 9.8, 10.6, 4.9, 9.85, "glow_orange")]
    m += [B(4, 10, 6.5, 9, 13, 9.5, "crimson"), B(5.5, 10.6, 6.3, 7.5, 12.4, 6.5, "glow_orange")]
    m += [B(8.5, 11, 7.3, 15, 12, 8.7, "mud")]
    return m


def blue_infrit():
    # A blackstone altar with a summoning circle of blue fire, a horned hound's skull glowing on top.
    m = [B(0.5, 0, 0.5, 15.5, 1.2, 15.5, "obsidian")]
    m += ring_y(8, 1.25, 8, 6.8, "glow_cyan", 0.3, segs=24) + ring_y(8, 1.25, 8, 5.0, "glow_blue", 0.25, segs=20)
    m += [B(4, 1.2, 4, 12, 5, 12, "black"), B(4.5, 5, 4.5, 11.5, 5.4, 11.5, "glow_cyan")]
    m += [B(5, 5.4, 5, 11, 10.5, 10.6, "gunmetal"), B(6, 6, 10.6, 10, 8.6, 14, "gunmetal"), B(6.6, 6, 14, 9.4, 7.6, 14.6, "black")]
    m += [B(5.6, 10.5, 6, 7.2, 14.5, 7.6, "black"), B(8.8, 10.5, 6, 10.4, 14.5, 7.6, "black")]
    m += [B(5.9, 8.4, 10.58, 7.4, 9.3, 10.65, "glow_cyan"), B(8.6, 8.4, 10.58, 10.1, 9.3, 10.65, "glow_cyan")]
    m += [B(6.4, 5.4, 12.6, 6.9, 6.2, 13, "glow_white"), B(9.1, 5.4, 12.6, 9.6, 6.2, 13, "glow_white")]
    return m


BOMBS.update({"goo_bomb": goo_bomb, "cactus_bomb": cactus_bomb, "blackforge": blackforge, "blue_infrit": blue_infrit})


def plasma_koi():
    # A dark stone basin holding a dome of liquid plasma, two koi of light circling outside it.
    m = [B(1, 0, 1, 15, 1.4, 15, "gunmetal"), B(2, 1.4, 2, 14, 2.2, 14, "steel")]
    m += ring_y(8, 2.25, 8, 6.4, "glow_cyan", 0.4, segs=24)
    m += [B(3.4, 2.2, 3.4, 12.6, 2.5, 12.6, "glow_cyan")]
    m += vsphere(8, 6.6, 8, 4.2, 1.0, "glass_cyan")
    m += [B(7.4, 10.8, 7.4, 8.6, 11.8, 8.6, "glow_white")]
    # A magenta koi swimming across the front, nose right, its tail fanned.
    m += [B(5.0, 5.0, 2.0, 9.6, 6.4, 3.2, "glow_pink"), B(9.6, 5.3, 2.2, 10.8, 6.1, 3.0, "glow_pink"),
          B(3.6, 4.6, 2.3, 5.0, 6.8, 2.9, "glow_pink"), B(9.9, 5.8, 1.95, 10.3, 6.2, 2.05, "glow_white")]
    m += [B(6.6, 6.4, 2.3, 7.8, 7.2, 2.9, "glow_pink")]
    # A gold koi climbing the right side the other way.
    m += [B(12.8, 7.4, 6.0, 14.0, 8.8, 10.4, "glow_gold"), B(13.0, 7.7, 4.8, 13.8, 8.5, 6.0, "glow_gold"),
          B(13.1, 7.0, 10.4, 13.7, 9.2, 11.8, "glow_gold"), B(13.95, 8.2, 5.2, 14.05, 8.6, 5.6, "glow_white")]
    return m


BOMBS.update({"plasma_koi": plasma_koi})


def dimension_shift():
    # A crying-obsidian portal frame standing on scorched crimson, its window full of swirling violet.
    m = [B(1, 0, 1, 15, 1.4, 15, "obsidian"), B(2, 1.4, 2, 14, 2.0, 14, "crimson")]
    m += [B(3, 2, 6.5, 13, 4, 9.5, "obsidian"), B(3, 14, 6.5, 13, 16, 9.5, "obsidian")]
    m += [B(3, 4, 6.5, 5, 14, 9.5, "obsidian"), B(11, 4, 6.5, 13, 14, 9.5, "obsidian")]
    m += [B(5, 4, 7.4, 11, 14, 8.6, "glass_purple"), B(6.5, 6, 7.2, 9.5, 12, 8.8, "glow_purple")]
    for (x, y) in ((3.4, 6), (12.2, 10), (6, 14.4), (10, 2.4)):
        m += [B(x, y, 6.3, x + 0.6, y + 0.6, 6.5, "glow_purple"), B(x, y, 9.5, x + 0.6, y + 0.6, 9.7, "glow_purple")]
    m += [B(2.5, 2.0, 2.5, 4.5, 2.6, 4.5, "glow_red"), B(11.2, 2.0, 11.8, 13.4, 2.4, 13.6, "glow_orange")]
    return m


def firework_havoc():
    # A firework the size of a tree on its launch stand: striped tube, gold nose cone, a fuse.
    m = [B(1, 0, 1, 15, 1.2, 15, "gunmetal"), B(2, 1.2, 2, 3, 6, 3, "steel"), B(13, 1.2, 2, 14, 6, 3, "steel"),
         B(2, 1.2, 13, 3, 6, 14, "steel"), B(13, 1.2, 13, 14, 6, 14, "steel")]
    for i in range(5):
        y0 = 2 + i * 2.2
        m += [B(5, y0, 5, 11, y0 + 1.1, 11, "red"), B(5, y0 + 1.1, 5, 11, y0 + 2.2, 11, "white")]
    m += [B(5.8, 13, 5.8, 10.2, 14.4, 10.2, "gold"), B(6.8, 14.4, 6.8, 9.2, 15.4, 9.2, "gold"), B(7.6, 15.4, 7.6, 8.4, 16, 8.4, "glow_gold")]
    m += [B(7.7, 0.8, 7.7, 8.3, 2, 8.3, "black"), B(7.6, 0.4, 7.6, 8.4, 0.8, 8.4, "glow_orange")]
    for (x, z, c) in ((4.6, 7, "glow_magenta"), (11, 8, "glow_cyan"), (7, 4.6, "glow_gold"), (8, 11, "glow_green")):
        m += [B(x, 6.6, z, x + 0.4, 7.2, z + 0.4, c)]
    return m


def chinese_lantern():
    # A red paper lantern with gold caps, ribs and a tassel, glowing from inside.
    m = [B(6, 0, 6, 10, 1, 10, "gold"), B(7.5, 1, 7.5, 8.5, 2, 8.5, "gold")]
    m += [B(4, 3, 4, 12, 12, 12, "red"), B(3.4, 5, 3.4, 12.6, 10, 12.6, "red")]
    m += [B(5, 2, 5, 11, 3, 11, "gold"), B(5, 12, 5, 11, 13, 11, "gold"), B(7.6, 13, 7.6, 8.4, 16, 8.4, "gold")]
    for x in (5.5, 8, 10.5):
        m += [B(x - 0.15, 3, 3.3, x + 0.15, 12, 3.4, "gold"), B(x - 0.15, 3, 12.6, x + 0.15, 12, 12.7, "gold")]
        m += [B(3.3, 3, x - 0.15, 3.4, 12, x + 0.15, "gold"), B(12.6, 3, x - 0.15, 12.7, 12, x + 0.15, "gold")]
    m += [B(5.5, 5.5, 3.3, 10.5, 9.5, 3.35, "glow_orange"), B(5.5, 5.5, 12.65, 10.5, 9.5, 12.7, "glow_orange")]
    m += [B(3.3, 5.5, 5.5, 3.35, 9.5, 10.5, "glow_orange"), B(12.65, 5.5, 5.5, 12.7, 9.5, 10.5, "glow_orange")]
    return m


BOMBS.update({"dimension_shift": dimension_shift, "firework_havoc": firework_havoc, "chinese_lantern": chinese_lantern})


def hyperbeam_raygun():
    # A chunky retro raygun: a bulbous charge chamber with fins, a wide bell emitter and a glowing coil.
    m = [B(5.5, 7, 6, 10.5, 12.5, 18, "white")]
    m += vsphere(8, 10, 3.5, 3.4, 1.0, "glass_cyan")
    m += [B(6.8, 8.8, 2.3, 9.2, 11.2, 4.7, "glow_cyan")]
    for z in (6.5, 9, 11.5):
        m += ring_y_frame(8, 9.8, z, 3.4, "chrome", 0.6)
    m += [B(7.5, 12.5, 8, 8.5, 15.5, 15, "red"), B(7.5, 4, 9, 8.5, 7, 14, "red")]
    m += cyl_z(8, 10, -4, 0.5, 1.4, "chrome") + ring_y_frame(8, 10, -4.5, 3.0, "chrome", 0.7) + ring_y_frame(8, 10, -4.6, 2.2, "glow_cyan", 0.4)
    m += [B(7.2, 9.2, -5.2, 8.8, 10.8, -4.6, "glow_white")]
    m += [B(6.5, 7, 18, 9.5, 11, 23, "white"), grip(8, 8, 15)]
    return m, (8, 4, 15)


GUNS["hyperbeam_raygun"] = hyperbeam_raygun


def mammoth_fossil():
    # A mammoth's skull frozen in a block of permafrost, its tusks curling out of the ice, cold light in its eyes.
    m = [B(0, 0, 0, 16, 1.5, 16, "rock"), B(1, 1.5, 1, 15, 2.2, 15, "mud")]
    m += [B(2, 2.2, 11, 14, 8, 15, "glass_ice"), B(3, 2.2, 6, 13, 4, 11, "ice"), B(1.4, 2.2, 10, 3.4, 5, 14, "ice"), B(12.6, 2.2, 9, 14.6, 4, 13, "ice")]
    # The dome of the skull and the face.
    m += [B(4.5, 4, 7.5, 11.5, 11.5, 13.5, "bone"), B(5.2, 11.5, 8.2, 10.8, 13, 12.8, "bone"), B(5.5, 3, 5, 10.5, 8, 7.5, "bone")]
    m += [B(5.4, 8.6, 7.3, 6.9, 9.8, 7.5, "glow_cyan"), B(9.1, 8.6, 7.3, 10.6, 9.8, 7.5, "glow_cyan")]
    # Tusks: forward, down, out and up.
    for side in (-1, 1):
        x0 = 8 + side * 1.8
        pts = [(x0, 4.5, 5), (x0 + side * 0.6, 3.3, 3.6), (x0 + side * 1.4, 3.0, 2.2), (x0 + side * 2.3, 3.6, 1.0), (x0 + side * 3.0, 5.0, 0.4),
               (x0 + side * 3.2, 6.6, 0.4), (x0 + side * 2.9, 8.0, 1.0)]
        for i, (x, y, z) in enumerate(pts):
            r = 0.75 - i * 0.07
            m += [B(x - r, y - r, z - r, x + r, y + r, z + r, "pearl")]
    return m


def leviathan_fossil():
    # A tide pool of ghostly sea with a serpent's spine breaching out of it in two arches, its skull rearing up.
    m = [B(0, 0, 0, 16, 1.5, 16, "sand"), B(1, 1.5, 1, 15, 2.4, 15, "glass_cyan")]
    for (cx, r0) in ((4.0, 3.0), (9.0, 2.6)):
        for i in range(9):
            a = 3.14159 * i / 8
            x, y = cx + _m.cos(a) * r0 * -1, 2 + _m.sin(a) * r0 * 1.5
            m += [B(x - 0.7, y - 0.7, 7.2, x + 0.7, y + 0.7, 8.8, "bone")]
            if i % 2 == 1:
                m += [B(x - 0.25, y + 0.6, 7.75, x + 0.25, y + 1.6, 8.25, "glass_cyan")]
    # The neck rising and the long open-jawed skull.
    for i, (x, y) in enumerate(((12.4, 2.6), (12.8, 4.2), (13.0, 5.8), (12.9, 7.4), (12.5, 9.0))):
        m += [B(x - 0.8, y - 0.8, 7.2, x + 0.8, y + 0.8, 8.8, "bone")]
    m += [B(9.0, 9.4, 6.6, 13.2, 11.0, 9.4, "bone"), B(9.6, 11.0, 6.9, 12.8, 11.6, 9.1, "bone"), B(9.4, 7.6, 7.0, 12.6, 8.4, 9.0, "bone")]
    for x in (9.6, 10.6, 11.6):
        m += [B(x, 8.4, 7.1, x + 0.35, 9.4, 7.45, "white"), B(x, 8.4, 8.55, x + 0.35, 9.4, 8.9, "white")]
    m += [B(12.2, 10.2, 6.5, 12.9, 10.8, 6.7, "glow_cyan"), B(12.2, 10.2, 9.3, 12.9, 10.8, 9.5, "glow_cyan")]
    return m


def pterosaur_fossil():
    # A pterosaur splayed across a slab of stone, wings outstretched, crest swept back, embers in its eyes.
    m = [B(0, 0, 0, 16, 1.4, 16, "rock"), B(0.6, 1.4, 0.6, 15.4, 2.6, 15.4, "mud")]
    y = 2.6
    m += [B(7.2, y, 5.5, 8.8, y + 0.8, 10.5, "bone")]                        # body
    m += [B(7.5, y, 10.5, 8.5, y + 0.6, 12.4, "bone"), B(7.2, y, 12.4, 8.8, y + 1.0, 13.6, "bone")]  # neck, skull
    m += [B(7.6, y, 13.6, 8.4, y + 0.6, 15.6, "bone"), B(7.7, y, 10.8, 8.3, y + 0.5, 12.6, "bone")]  # beak
    m += [B(7.7, y + 0.5, 11.2, 8.3, y + 1.2, 12.4, "bone")]                 # crest
    m += [B(7.2, y + 0.8, 13.0, 7.5, y + 1.1, 13.3, "glow_orange"), B(8.5, y + 0.8, 13.0, 8.8, y + 1.1, 13.3, "glow_orange")]
    for side in (-1, 1):
        def bx(x0, z0, x1, z1, w=0.5):
            return B(min(x0, x1) - w, y, min(z0, z1) - w, max(x0, x1) + w, y + 0.6, max(z0, z1) + w, "bone")
        sx = 8 + side * 0.8
        ex, wx, tx = 8 + side * 3.2, 8 + side * 5.6, 8 + side * 7.4
        m += [bx(sx, 9.6, ex, 10.2), bx(ex, 10.2, wx, 11.0)]
        for k in range(4):
            z = 11.0 - k * 1.5
            m += [bx(wx + side * k * 0.45, z, wx + side * (k + 1) * 0.45, z - 1.5, 0.3)]
        m += [bx(tx, 5.0, tx, 4.0, 0.25)]
        m += [bx(8 + side * 0.5, 5.6, 8 + side * 1.4, 3.8, 0.28), bx(8 + side * 1.4, 3.8, 8 + side * 1.6, 2.2, 0.22)]
    m += [B(7.7, y, 2.6, 8.3, y + 0.5, 5.5, "bone")]                           # tail
    return m


BOMBS.update({"mammoth_fossil": mammoth_fossil, "leviathan_fossil": leviathan_fossil, "pterosaur_fossil": pterosaur_fossil})


def amber_fossil():
    # A drop of amber on a slab of stone with a dragonfly caught inside it, wings spread, a warm glow at its heart.
    m = [B(0, 0, 0, 16, 1.4, 16, "rock"), B(1, 1.4, 1, 15, 2.0, 15, "glass_amber")]
    # The drop: stacked rounded layers, widest low down, pinched to a point at the top.
    for (y0, y1, r) in ((2.0, 4.0, 5.6), (4.0, 7.5, 6.6), (7.5, 10.5, 6.0), (10.5, 12.5, 4.6), (12.5, 14.0, 3.0), (14.0, 15.4, 1.6)):
        m += [B(8 - r, y0, 8 - r * 0.85, 8 + r, y1, 8 + r * 0.85, "glass_amber")]
    m += [B(7.2, 6.6, 7.2, 8.8, 8.2, 8.8, "glow_gold")]
    # The dragonfly: head, thorax and a long banded abdomen, four clear wings.
    y = 7.6
    m += [B(7.3, y, 11.2, 8.7, y + 1.3, 12.4, "mud"), B(7.0, y + 0.3, 12.0, 7.5, y + 1.1, 12.6, "glass_cyan"), B(8.5, y + 0.3, 12.0, 9.0, y + 1.1, 12.6, "glass_cyan")]
    m += [B(7.2, y, 9.2, 8.8, y + 1.5, 11.2, "mud")]
    for i, z in enumerate((8.4, 7.4, 6.4, 5.4, 4.4, 3.4)):
        m += [B(7.5, y + 0.25, z, 8.5, y + 1.05, z + 0.9, "glow_cyan" if i % 2 else "mud")]
    for side in (-1, 1):
        x0 = 8 + side * 0.8
        x1 = 8 + side * 6.2
        m += [B(min(x0, x1), y + 1.3, 10.4, max(x0, x1), y + 1.5, 11.4, "glass_cyan"),
              B(min(x0, x1 - side * 0.4), y + 1.3, 9.1, max(x0, x1 - side * 0.4), y + 1.5, 10.2, "glass_cyan")]
    return m


def sabre_fossil():
    # A sabre-tooth's skull in profile, jaws gaping wide, sabres long and bared, on a slab of dark stone raked with
    # three glowing claw marks. Faces +x so the inventory shows its side.
    m = [B(0, 0, 0, 16, 1.6, 16, "obsidian")]
    for z in (3.0, 5.4, 7.8):
        m += [B(1.0, 1.6, z, 9.5, 1.75, z + 0.7, "glow_orange")]
    # Cranium: long and low, the crest rising behind; the brow and the short deep muzzle in front.
    m += [B(2.6, 8.0, 6.0, 8.8, 11.0, 10.0, "bone"), B(1.6, 8.6, 6.6, 3.0, 11.4, 9.4, "bone"), B(3.0, 11.0, 7.6, 7.6, 12.2, 8.4, "bone")]
    m += [B(8.8, 7.4, 6.4, 11.6, 10.0, 9.6, "bone"), B(11.6, 7.2, 6.8, 13.0, 9.0, 9.2, "bone")]
    # Cheekbone flaring out, the eye socket burning.
    m += [B(5.0, 7.4, 5.4, 9.4, 8.4, 10.6, "bone"), B(8.0, 9.2, 5.8, 9.2, 10.2, 6.0, "glow_orange"), B(8.0, 9.2, 10.0, 9.2, 10.2, 10.2, "glow_orange")]
    # The sabres: long, flat, curving down and back.
    for i, (x, y) in enumerate(((11.6, 7.2), (11.9, 6.0), (11.9, 4.8), (11.6, 3.7), (11.1, 2.8))):
        w = 0.55 - i * 0.07
        for z in (7.0, 9.0):
            m += [B(x - w, y - 1.2, z - 0.35, x + w, y, z + 0.35, "pearl")]
    # The lower jaw dropped open at a steep angle, chin on the slab.
    m += [B(3.0, 5.6, 6.6, 4.8, 7.4, 9.4, "bone"), B(4.4, 4.0, 6.8, 6.4, 5.8, 9.2, "bone"), B(6.0, 2.4, 7.0, 8.4, 4.2, 9.0, "bone"),
          B(8.0, 1.6, 7.2, 10.0, 2.8, 8.8, "bone")]
    # The back of the skull, the neck vertebra.
    m += [B(0.8, 6.2, 7.0, 2.6, 8.8, 9.0, "bone"), B(0.2, 1.6, 7.3, 1.6, 6.4, 8.7, "bone")]
    return m


def ammonite_fossil():
    # An ammonite standing on its keel on a slab of stone: a coiled, ribbed shell with a rim of pearl round its mouth.
    m = [B(0, 0, 0, 16, 1.4, 16, "rock"), B(1, 1.4, 1, 15, 2.0, 15, "chalk")]
    cx, cy, top = 7.0, 8.6, 13.0
    th, i = 2.0, 0
    while th <= top:
        rho = 0.5 * _m.exp(0.18 * th)
        a = th - top - _m.pi / 2 + 0.6
        x, y = cx + _m.cos(a) * rho, cy + _m.sin(a) * rho
        tr = 0.36 * rho + 0.25
        w = max(0.7, tr * 0.7)
        if th > top - 0.3:
            # The mouth: a thin rim of pearl.
            m += [B(x - 0.35, y - tr * 1.1, 8 - w * 1.1, x + 0.35, y + tr * 1.1, 8 + w * 1.1, "glow_pink")]
        else:
            mat = "bone" if i % 3 == 0 else ("nacre" if th < 8 else "chalk")
            k = 1.1 if i % 3 == 0 else 1.0
            m += [B(x - tr * k, y - tr * k, 8 - w * k, x + tr * k, y + tr * k, 8 + w * k, mat)]
        th += max(0.3, 0.8 / rho)
        i += 1
    return m


BOMBS.update({"amber_fossil": amber_fossil, "sabre_fossil": sabre_fossil, "ammonite_fossil": ammonite_fossil})


def plasmoid_swarm():
    # Three balls of lightning caught in a frame of dark glass over a tech plinth, a white core between them.
    m = [B(1, 0, 1, 15, 2, 15, "gunmetal"), B(2, 2, 2, 14, 2.6, 14, "black")]
    for (x, z) in ((2, 2), (13.4, 2), (2, 13.4), (13.4, 13.4)):
        m += [B(x, 2.6, z, x + 0.6, 14, z + 0.6, "glass_purple")]
    m += [B(2, 14, 2, 14, 14.6, 2.6, "glass_purple"), B(2, 14, 13.4, 14, 14.6, 14, "glass_purple"),
          B(2, 14, 2.6, 2.6, 14.6, 13.4, "glass_purple"), B(13.4, 14, 2.6, 14, 14.6, 13.4, "glass_purple")]
    m += ball(8, 8, 8, 2.6, "glow_purple") + ball(4.6, 5.2, 5.4, 1.4, "glow_purple") + ball(11.2, 10.8, 10.6, 1.6, "glow_purple")
    m += ball(8, 8, 8, 1.1, "glow_white")
    return m


def solar_arch():
    # A loop of plasma arching between two black pylons over a slab of scorched stone, glowing at its feet.
    m = [B(0, 0, 0, 16, 1.5, 16, "black"), B(1, 1.5, 6, 4, 4, 10, "obsidian"), B(12, 1.5, 6, 15, 4, 10, "obsidian")]
    m += [B(1.6, 4, 6.6, 3.4, 4.4, 9.4, "glow_gold"), B(12.6, 4, 6.6, 14.4, 4.4, 9.4, "glow_gold")]
    for i in range(13):
        a = 3.14159 * i / 12
        x, y = 8 - _m.cos(a) * 5.5, 4.2 + _m.sin(a) * 9.5
        m += [B(x - 0.9, y - 0.9, 7.1, x + 0.9, y + 0.9, 8.9, "glow_orange" if i % 2 else "glow_gold")]
    return m


def fusion_geyser():
    # A vent of black rock with a column of cyan plasma pouring up out of it through rings of field.
    m = [B(0, 0, 0, 16, 1.5, 16, "rock"), B(3, 1.5, 3, 13, 3.5, 13, "black"), B(4.5, 3.5, 4.5, 11.5, 5, 11.5, "black")]
    m += [B(6, 5, 6, 10, 5.4, 10, "glow_cyan")]
    m += [B(6.8, 5, 6.8, 9.2, 16, 9.2, "glow_cyan"), B(7.4, 5, 7.4, 8.6, 16.2, 8.6, "glow_white")]
    for y in (8.0, 12.0):
        m += [B(5.6, y, 5.6, 10.4, y + 0.7, 6.3, "chrome"), B(5.6, y, 9.7, 10.4, y + 0.7, 10.4, "chrome"),
              B(5.6, y, 6.3, 6.3, y + 0.7, 9.7, "chrome"), B(9.7, y, 6.3, 10.4, y + 0.7, 9.7, "chrome")]
    return m


def z_pinch():
    # A white-hot column pinched by blue rings, standing between two electrodes.
    m = [B(2, 0, 2, 14, 2, 14, "gunmetal"), B(5.5, 2, 5.5, 10.5, 3.2, 10.5, "chrome"), B(5.5, 14.8, 5.5, 10.5, 16, 10.5, "chrome")]
    for (y0, y1, r) in ((3.2, 5.2, 1.6), (5.2, 6.2, 0.9), (6.2, 8.2, 1.6), (8.2, 9.2, 0.9), (9.2, 11.2, 1.6), (11.2, 12.2, 0.9), (12.2, 14.8, 1.6)):
        m += [B(8 - r, y0, 8 - r, 8 + r, y1, 8 + r, "glow_white")]
    for y in (5.2, 8.2, 11.2):
        r = 2.4
        m += [B(8 - r, y, 8 - r, 8 + r, y + 1, 8 - r + 0.6, "glow_blue"), B(8 - r, y, 8 + r - 0.6, 8 + r, y + 1, 8 + r, "glow_blue"),
              B(8 - r, y, 8 - r, 8 - r + 0.6, y + 1, 8 + r, "glow_blue"), B(8 + r - 0.6, y, 8 - r, 8 + r, y + 1, 8 + r, "glow_blue")]
    return m


def magnetic_bottle():
    # A little magenta sun held in two crossed loops of chrome field coil over a plinth.
    m = [B(2, 0, 2, 14, 1.6, 14, "gunmetal"), B(6, 1.6, 6, 10, 3, 10, "chrome")]
    m += ball(8, 9, 8, 3.2, "glow_magenta") + ball(8, 9, 8, 1.4, "glow_white")
    r, t = 5.6, 0.6
    m += [B(8 - r, 9 - r, 7.7, 8 - r + t, 9 + r, 8.3, "chrome"), B(8 + r - t, 9 - r, 7.7, 8 + r, 9 + r, 8.3, "chrome"),
          B(8 - r, 9 + r - t, 7.7, 8 + r, 9 + r, 8.3, "chrome"), B(8 - r, 9 - r, 7.7, 8 + r, 9 - r + t, 8.3, "chrome")]
    m += [B(7.7, 9 - r, 8 - r, 8.3, 9 + r, 8 - r + t, "glass_magenta"), B(7.7, 9 - r, 8 + r - t, 8.3, 9 + r, 8 + r, "glass_magenta"),
          B(7.7, 9 + r - t, 8 - r, 8.3, 9 + r, 8 + r, "glass_magenta"), B(7.7, 9 - r, 8 - r, 8.3, 9 - r + t, 8 + r, "glass_magenta")]
    return m


BOMBS.update({"plasmoid_swarm": plasmoid_swarm, "solar_arch": solar_arch, "fusion_geyser": fusion_geyser, "z_pinch": z_pinch,
              "magnetic_bottle": magnetic_bottle})


def mega_creeper():
    # A creeper's head, eight pixels to a side, mottled green with its black face toward the front, on a block of TNT.
    m = [B(1, 0, 1, 15, 4, 15, "red"), B(1, 1.4, 1, 15, 2.6, 15, "white")]
    face = ["........", "........", ".##..##.", ".##..##.", "...##...", "..####..", "..####..", "..#..#.."]
    m += [B(3, 4, 3, 13, 14, 13, "leaf")]
    px = 1.25
    for y in range(8):
        for x in range(8):
            x0, y0 = 3 + x * px, 14 - (y + 1) * px
            if face[y][x] == "#":
                m += [B(x0, y0, 13.0, x0 + px, y0 + px, 13.1, "black")]
            elif (x * 7 + y * 3) % 5 == 0:
                m += [B(x0, y0, 13.0, x0 + px, y0 + px, 13.05, "cactus" if (x + y) % 2 else "glow_lime")]
    # Mottling on the other faces you see.
    for y in range(8):
        for z in range(8):
            if (z * 5 + y * 3) % 4 == 0:
                z0, y0 = 3 + z * px, 14 - (y + 1) * px
                m += [B(13.0, y0, z0, 13.05, y0 + px, z0 + px, "cactus" if (y + z) % 2 else "glow_lime")]
    for x in range(8):
        for z in range(8):
            if (x * 3 + z * 5) % 4 == 1:
                x0, z0 = 3 + x * px, 3 + z * px
                m += [B(x0, 14.0, z0, x0 + px, 14.05, z0 + px, "cactus" if (x + z) % 2 else "glow_lime")]
    return m


def elder_wyrm():
    # A dragon's skull of black scale on a block of obsidian cracked with magma: swept-back horns, burning eyes,
    # its snout toward the front.
    m = [B(0, 0, 0, 16, 3, 16, "obsidian")]
    for (x0, z0, x1, z1) in ((2, 12.4, 9, 13), (8, 4, 8.6, 12.4), (8.6, 4.4, 14, 5)):
        m += [B(x0, 2.95, z0, x1, 3.05, z1, "glow_orange")]
    m += [B(4, 3, 3, 12, 8, 12, "black"), B(5, 3, 12, 11, 6.5, 15, "black"), B(5.5, 6.5, 12, 10.5, 7.5, 14.5, "black")]
    m += [B(5.6, 3.2, 14.8, 10.4, 4.2, 15.2, "glow_orange")]
    for side in (-1, 1):
        x = 8 + side * 3.2
        m += [B(x - 0.7, 8, 4, x + 0.7, 9.6, 6, "obsidian"), B(x - 0.5 + side * 0.4, 9.6, 2, x + 0.5 + side * 0.4, 11.4, 4.5, "obsidian"),
              B(x - 0.35 + side * 0.8, 11.4, 0.6, x + 0.35 + side * 0.8, 13.2, 2.5, "obsidian")]
        m += [B(8 + side * 2.6 - 0.6, 6.6, 11.9, 8 + side * 2.6 + 0.6, 7.4, 12.2, "glow_orange")]
    return m


BOMBS.update({"mega_creeper": mega_creeper, "elder_wyrm": elder_wyrm})


def wrecking_ball():
    # A steel wrecking ball hanging on its chain from a gantry over a block of shattered concrete.
    m = [B(0, 0, 0, 16, 2, 16, "white"), B(2, 2, 2, 5, 4, 6, "rock"), B(11, 2, 10, 14, 3.4, 14, "rock")]
    m += [B(1, 2, 7, 2.2, 16, 9, "gunmetal"), B(13.8, 2, 7, 15, 16, 9, "gunmetal"), B(1, 15, 7, 15, 16, 9, "gunmetal")]
    for i, y in enumerate((14.6, 13.4, 12.2, 11.0)):
        m += [B(7.4, y - 1.1, 7.7, 8.6, y, 8.3, "chrome") if i % 2 else B(7.7, y - 1.1, 7.4, 8.3, y, 8.6, "chrome")]
    m += ball(8, 7, 8, 4.2, "gunmetal") + [B(7, 9.8, 7, 9, 10.8, 9, "black")]
    return m


def flattener():
    # A steel roller on its axle, arms rising from its hubs, over a slab pressed flat.
    m = [B(0, 0, 0, 16, 1.4, 16, "rock"), B(1, 1.4, 1, 15, 1.7, 15, "red"), B(3, 1.4, 6, 7, 1.7, 10, "yellow")]
    m += [B(1.5, 2, 4, 14.5, 10.4, 12, "steel"), B(1.5, 2.8, 3.2, 14.5, 9.6, 12.8, "steel")]
    for x in (3.5, 6.5, 9.5, 12.5):
        m += [B(x, 1.9, 3.1, x + 0.5, 10.6, 12.9, "gunmetal")]
    for side in (0.5, 14.5):
        m += [B(side, 5, 6.8, side + 1, 7.6, 9.2, "gunmetal"), B(side, 7.6, 7.4, side + 1, 16, 8.6, "gunmetal")]
    return m


def titans_stride():
    # A slab of street with a giant's footprint stamped dark into it: heel, sole, five toes, the earth heaved up round it.
    m = [B(0, 0, 0, 16, 4, 16, "rock"), B(0, 4, 0, 16, 4.6, 16, "white")]
    y0, y1 = 4.6, 4.72
    m += [B(5.4, y0, 1.6, 10.6, y1, 4.6, "black"), B(4.6, y0, 4.6, 11.4, y1, 8.6, "black"), B(4.2, y0, 8.6, 11.8, y1, 11.4, "black")]
    for i, x in enumerate((4.6, 6.4, 8.2, 10.0, 11.6)):
        z = 12.3 + (0.0 if i in (0, 4) else 0.8)
        r = 0.95 if i == 0 else 0.75
        m += [B(x - r, y0, z - r, x + r, y1, z + r, "black")]
    m += [B(2.8, 4.6, 1.4, 3.6, 5.3, 13.6, "mud"), B(12.4, 4.6, 1.4, 13.2, 5.3, 13.6, "mud"), B(4, 4.6, 0.4, 12, 5.0, 1.2, "mud")]
    return m


def inferno_vortex():
    # A funnel of fire twisting up out of a scorched base, widening as it climbs.
    m = [B(0, 0, 0, 16, 2, 16, "black"), B(2, 2, 2, 14, 2.4, 14, "glow_orange")]
    for i in range(7):
        y = 2.4 + i * 1.9
        r = 1.0 + i * 0.85
        a = i * 0.9
        cx, cz = 8 + _m.cos(a) * 0.8, 8 + _m.sin(a) * 0.8
        m += [B(cx - r, y, cz - r, cx + r, y + 1.3, cz + r, "glow_orange" if i % 2 else "glow_gold")]
    return m


def maelstrom():
    # A whirlpool: rings of foaming water spiralling down to a dark throat in a stone basin.
    m = [B(0, 0, 0, 16, 3, 16, "rock"), B(1, 3, 1, 15, 3.6, 15, "glass_cyan")]
    for i in range(10):
        a = i * 0.9
        r = 6.2 - i * 0.5
        x, z = 8 + _m.cos(a) * r, 8 + _m.sin(a) * r
        m += [B(x - 0.9, 3.4 - i * 0.12, z - 0.9, x + 0.9, 3.9 - i * 0.12, z + 0.9, "white")]
    m += [B(6.6, 1.2, 6.6, 9.4, 3.62, 9.4, "black")]
    return m


def basalt_uprising():
    # A cluster of basalt columns of all heights thrust up out of a slab, one glowing at its top.
    m = [B(0, 0, 0, 16, 1.6, 16, "rock")]
    cols = [(4, 4, 9), (8, 3.5, 12), (12, 4.5, 7), (3.5, 8.5, 13), (8, 8, 16), (12.5, 8.5, 11), (4.5, 12.5, 8), (8.5, 12.5, 10), (12, 12.5, 14)]
    for i, (x, z, h) in enumerate(cols):
        m += [B(x - 1.8, 1.6, z - 1.6, x + 1.8, h, z + 1.6, "black"), B(x - 1.4, h, z - 1.2, x + 1.4, h + 0.3, z + 1.2, "glow_orange" if i == 4 else "obsidian")]
    return m


def downburst():
    # A vortex of white cloud whirling over a block, streaks of wind pouring down out of it.
    m = [B(0, 0, 0, 16, 2, 16, "rock"), B(1, 2, 1, 15, 2.3, 15, "white")]
    for i in range(12):
        a = i * _m.pi / 6
        r = 5.6
        x, z = 8 + _m.cos(a) * r, 8 + _m.sin(a) * r
        m += [B(x - 1.4, 12.5 + (i % 2) * 0.6, z - 1.4, x + 1.4, 14.5 + (i % 2) * 0.6, z + 1.4, "white" if i % 3 else "pearl")]
    m += [B(6.6, 11.5, 6.6, 9.4, 13.5, 9.4, "pearl")]
    for x, z in ((5, 6), (10, 5), (7, 11), (11.5, 10)):
        m += [B(x - 0.2, 3, z - 0.2, x + 0.2, 11, z + 0.2, "glow_white")]
    return m


def thunderhead():
    # A black storm cloud over a block of glass, a bolt of lightning forking down out of it.
    m = [B(0, 0, 0, 16, 2, 16, "rock"), B(1, 2, 1, 15, 2.3, 15, "black")]
    for (x0, z0, x1, z1, y0, y1) in ((2, 3, 14, 13, 11.5, 14), (3.5, 2, 12.5, 14, 13.5, 15.6), (1, 5, 6, 11, 12.2, 14.6), (10, 4, 15, 12, 12.4, 14.8)):
        m += [B(x0, y0, z0, x1, y1, z1, "gunmetal")]
    bolt = [(8.0, 11.5), (7.0, 9.6), (8.6, 8.0), (7.2, 6.0), (8.4, 4.0), (7.6, 2.3)]
    for (xa, ya), (xb, yb) in zip(bolt, bolt[1:]):
        m += [B(min(xa, xb) - 0.35, yb, 7.65, max(xa, xb) + 0.35, ya, 8.35, "glow_white")]
    m += [B(5.6, 2.3, 7, 7.6, 2.5, 9, "glow_cyan"), B(8.4, 5, 7.7, 10.4, 5.6, 8.3, "glow_white")]
    return m


BOMBS.update({"wrecking_ball": wrecking_ball, "flattener": flattener, "titans_stride": titans_stride, "inferno_vortex": inferno_vortex,
              "maelstrom": maelstrom, "basalt_uprising": basalt_uprising, "downburst": downburst, "thunderhead": thunderhead})


def xenoform():
    # A wireframe hypercube hanging over a black plinth ruled with light: a cube inside a cube, joined corner to corner.
    m = [B(1, 0, 1, 15, 1.4, 15, "black")]
    for k in (3, 8, 13):
        m += [B(k - 0.15, 1.4, 1.5, k + 0.15, 1.5, 14.5, "glow_cyan"), B(1.5, 1.4, k - 0.15, 14.5, 1.5, k + 0.15, "glow_cyan")]

    def frame(x0, y0, z0, x1, y1, z1, w, mat):
        out = []
        for (ya, yb) in ((y0, y0 + w), (y1 - w, y1)):
            out += [B(x0, ya, z0, x1, yb, z0 + w, mat), B(x0, ya, z1 - w, x1, yb, z1, mat),
                    B(x0, ya, z0, x0 + w, yb, z1, mat), B(x1 - w, ya, z0, x1, yb, z1, mat)]
        for (xa, za) in ((x0, z0), (x1 - w, z0), (x0, z1 - w), (x1 - w, z1 - w)):
            out += [B(xa, y0, za, xa + w, y1, za + w, mat)]
        return out
    m += frame(3, 3.5, 3, 13, 13.5, 13, 0.55, "glow_white")
    m += frame(6, 6.5, 6, 10, 10.5, 10, 0.45, "glow_purple")
    # Struts joining the corners of the two cubes.
    for (ox, ix) in ((3.3, 6.2), (12.7, 9.8)):
        for (oz, iz) in ((3.3, 6.2), (12.7, 9.8)):
            for (oy, iy) in ((3.8, 6.7), (13.2, 10.3)):
                for s in range(1, 4):
                    t = s / 4.0
                    x, y, z = ox + (ix - ox) * t, oy + (iy - oy) * t, oz + (iz - oz) * t
                    m += [B(x - 0.2, y - 0.2, z - 0.2, x + 0.2, y + 0.2, z + 0.2, "glow_cyan")]
    m += [B(7.4, 7.9, 7.4, 8.6, 9.1, 8.6, "black")]
    return m


BOMBS.update({"xenoform": xenoform})


def terror_maniac():
    # A cracked white mask with red eyes and a stitched grin, half sunk in black roots and red veins.
    m = [B(0, 0, 0, 16, 1.2, 16, "black"), B(1, 1.2, 1, 15, 1.6, 15, "crimson")]
    for (x0, z0, x1, z1) in ((8, 8, 1.5, 3), (8, 8, 14.5, 4), (8, 8, 2.5, 13.5), (8, 8, 13.5, 14), (8, 8, 8, 1)):
        for s in range(6):
            t = s / 6.0
            x, z = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
            m += [B(x - 0.4, 1.6, z - 0.4, x + 0.4, 2.0, z + 0.4, "glow_red")]
    m += [B(4.5, 1.6, 6, 11.5, 13.5, 10, "black")]                       # head under the mask
    m += [B(4.0, 2.6, 5.4, 12.0, 13.0, 5.9, "pearl"), B(4.6, 13.0, 5.6, 11.4, 14.2, 6.0, "pearl"), B(4.6, 1.8, 5.6, 11.4, 2.6, 6.0, "pearl")]
    m += [B(5.2, 8.6, 5.2, 7.2, 11.4, 5.4, "black"), B(8.8, 8.6, 5.2, 10.8, 11.4, 5.4, "black")]
    m += [B(5.8, 9.6, 5.1, 6.6, 10.4, 5.2, "glow_red"), B(9.4, 9.6, 5.1, 10.2, 10.4, 5.2, "glow_red")]
    m += [B(5.9, 6.0, 5.2, 6.3, 8.6, 5.3, "black"), B(9.7, 6.0, 5.2, 10.1, 8.6, 5.3, "black")]
    m += [B(5.0, 4.6, 5.2, 11.0, 5.0, 5.3, "black")]
    for x in (5.6, 6.8, 8.0, 9.2, 10.4):
        m += [B(x - 0.1, 4.0, 5.15, x + 0.1, 5.6, 5.25, "black")]
    for (x, y) in ((4.6, 12.8), (11.0, 11.6), (7.4, 13.6)):
        m += [B(x, y, 5.3, x + 0.3, y + 0.3, 5.4, "black")]
    for (x, z, h) in ((3, 9, 9), (13, 8, 11), (5, 12, 7), (11.5, 12, 8)):
        m += [B(x - 0.3, 1.6, z - 0.3, x + 0.3, 1.6 + h, z + 0.3, "black")]
    return m


BOMBS.update({"terror_maniac": terror_maniac})


def pumpkin_horseman():
    # A grinning jack-o'-lantern on a mound of dark earth, a scythe leaning behind it, green ghost fire at its stem.
    m = [B(0, 0, 0, 16, 1.4, 16, "mud"), B(2, 1.4, 2, 14, 2.2, 14, "black")]
    m += [B(3.5, 2.2, 3.5, 12.5, 10.5, 12.5, "pumpkin"), B(3, 3.2, 4.5, 13, 9.5, 11.5, "pumpkin"), B(4.5, 3.2, 3, 11.5, 9.5, 13, "pumpkin")]
    for x in (5.4, 8.0, 10.6):
        m += [B(x - 0.2, 2.4, 3.3, x + 0.2, 10.3, 3.45, "pumpkin_dark")]
    m += [B(7.4, 10.5, 7.4, 8.6, 12.4, 8.6, "leaf"), B(7.6, 12.4, 7.6, 8.4, 13.2, 8.4, "glow_lime")]
    m += [B(4.8, 7.0, 2.85, 6.8, 8.6, 3.05, "glow_gold"), B(9.2, 7.0, 2.85, 11.2, 8.6, 3.05, "glow_gold")]
    m += [B(7.4, 5.8, 2.85, 8.6, 6.8, 3.05, "glow_gold")]
    m += [B(4.6, 4.0, 2.85, 11.4, 5.0, 3.05, "glow_gold")]
    for x in (5.6, 7.4, 9.2):
        m += [B(x, 4.6, 2.8, x + 0.8, 5.0, 3.1, "pumpkin")]
    m += [B(13.4, 2.2, 12.6, 14.0, 15.6, 13.2, "black")]
    for i in range(6):
        m += [B(13.4 - i * 1.0, 15.0 - i * 0.35, 12.5, 14.0 - i * 1.0, 15.8 - i * 0.35, 13.3, "steel")]
    return m


BOMBS.update({"pumpkin_horseman": pumpkin_horseman})


def portal_of_hell():
    # A miniature of the gate: a stepped base, a pointed gothic arch of blackstone with spikes and a glowing rim,
    # and inside it a vortex of hellfire with the eye in it.
    import math as mm
    m = [B(0, 0, 0, 16, 1, 16, "obsidian"), B(1, 1, 3, 15, 2, 13, "black"), B(2, 2, 5, 14, 2.6, 11, "obsidian")]
    W, SIDES, T, Z0, Z1 = 4.6, 6.0, 1.4, 6.9, 9.1

    def inside(u, v, grow):
        if v < 0 or abs(u) > W + grow:
            return False
        if v <= SIDES:
            return True
        r = 2 * W + grow
        return mm.hypot(u - W, v - SIDES) <= r and mm.hypot(u + W, v - SIDES) <= r
    step = 0.5

    def cell(cu, cv):
        if inside(cu, cv, T) and not inside(cu, cv, 0):
            return ("glow_purple" if inside(cu, cv, 0.55) else "black", Z0, Z1)
        if inside(cu, cv, 0):
            r = mm.hypot(cu, cv - 5.2)
            return ("glow_gold" if r < 1.6 else "glow_orange" if r < 3.4 else "crimson", 7.6, 8.4)
        return None
    # Each column of the arch, with runs of the same material merged into one box.
    u = -W - T
    while u < W + T:
        v, run = 0.0, None
        while v <= 16 - 2.6:
            c = cell(u + step / 2, v + step / 2) if v < 16 - 2.6 else None
            if run and c != run[0]:
                mat, z0, z1 = run[0]
                m += [B(8 + u, 2.6 + run[1], z0, 8 + u + step, 2.6 + v, z1, mat)]
                run = None
            if c and not run:
                run = (c, v)
            v += step
        u += step
    # The eye: an amber sclera, a red iris ring, a black slit.
    m += [B(6.2, 7.0, 7.35, 9.8, 8.6, 7.5, "glow_gold"), B(7.2, 6.8, 7.3, 8.8, 8.8, 7.4, "glow_red"), B(7.8, 6.9, 7.2, 8.2, 8.7, 7.3, "black")]
    # Spikes along the outside of the arch.
    for (x, y, dx, dy) in ((2.0, 4.0, -1.6, 0), (2.0, 7.0, -1.6, 0.2), (14.0, 4.0, 1.6, 0), (14.0, 7.0, 1.6, 0.2), (3.0, 10.5, -1.2, 1.0),
                           (13.0, 10.5, 1.2, 1.0), (5.2, 13.2, -0.8, 1.2), (10.8, 13.2, 0.8, 1.2)):
        m += [B(min(x, x + dx), y, 7.6, max(x, x + dx) + 0.01, y + 0.6, 8.4, "obsidian")]
    m += [B(7.7, 15.0, 7.7, 8.3, 16, 8.3, "obsidian")]
    # Lava spilling down the steps.
    m += [B(6.5, 2.0, 9.1, 9.5, 2.65, 12.5, "glow_orange"), B(7, 1.0, 12.5, 9, 2.05, 13.6, "glow_orange")]
    return m


BOMBS.update({"portal_of_hell": portal_of_hell})


def ai_takeover():
    # A black monolithic core on a circuit board, red seams up its faces and a red lens-eye at its crown.
    m = [B(0, 0, 0, 16, 1.2, 16, "black"), B(0.5, 1.2, 0.5, 15.5, 1.5, 15.5, "carbon")]
    for (x0, z0, x1, z1) in ((1, 8, 5.5, 8), (10.5, 8, 15, 8), (8, 1, 8, 5.5), (8, 10.5, 8, 15), (2, 3, 5, 3), (11, 13, 14, 13), (3, 11, 3, 14)):
        m += [B(min(x0, x1) - 0.2, 1.5, min(z0, z1) - 0.2, max(x0, x1) + 0.2, 1.7, max(z0, z1) + 0.2, "glow_red")]
    m += [B(4.5, 1.5, 4.5, 11.5, 2.5, 11.5, "gunmetal")]
    m += [B(5.5, 2.5, 5.5, 10.5, 13.5, 10.5, "black")]
    for (x0, z0, x1, z1) in ((7.6, 5.3, 8.4, 5.5), (7.6, 10.5, 8.4, 10.7), (5.3, 7.6, 5.5, 8.4), (10.5, 7.6, 10.7, 8.4)):
        m += [B(x0, 3, z0, x1, 12.5, z1, "glow_red")]
    m += [B(6.0, 13.5, 6.0, 10.0, 14.5, 10.0, "gunmetal"), B(6.5, 14.5, 6.5, 9.5, 16, 9.5, "black")]
    m += [B(7.0, 14.7, 6.3, 9.0, 15.8, 6.5, "glow_red"), B(7.6, 15.0, 6.2, 8.4, 15.5, 6.3, "glow_white")]
    return m


BOMBS.update({"ai_takeover": ai_takeover})
