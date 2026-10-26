#!/usr/bin/env python3
"""Generates every texture, model, blockstate, recipe, loot table, tag, worldgen entry, sound
definition, language entry and shader for Armageddon Arsenal.

Usage (from the repository root):
    python3 tools/generate_assets.py

Requires Pillow and numpy. Output goes to src/main/resources. Safe to re-run.
"""
import json
import math
import os
import shutil
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import gen_textures as T  # noqa: E402
import models as M  # noqa: E402
import lang as L  # noqa: E402
import arsenal_data as AD  # noqa: E402
import tex_arsenal as TA  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources")
NS = "armageddon"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def save_png(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def r3(v):
    return round(float(v), 3)


# =========================================================================== model building

def face_uv(face, fr, to):
    x1, y1, z1 = fr
    x2, y2, z2 = to
    if face in ("north", "south"):
        w, h, u0, v0 = x2 - x1, y2 - y1, x1, 16 - y2
    elif face in ("east", "west"):
        w, h, u0, v0 = z2 - z1, y2 - y1, z1, 16 - y2
    else:
        w, h, u0, v0 = x2 - x1, z2 - z1, x1, z1
    w, h = min(w, 16), min(h, 16)
    u0 = u0 % 16
    v0 = v0 % 16
    u0 = min(max(u0, 0), 16 - w)
    v0 = min(max(v0, 0), 16 - h)
    return [r3(u0), r3(v0), r3(u0 + w), r3(v0 + h)]


def build_elements(boxes):
    textures = {}
    elements = []
    for b in boxes:
        mat = b["mat"]
        textures[mat] = f"{NS}:item/mat/{mat}"
        fr = [r3(v) for v in b["from"]]
        to = [r3(v) for v in b["to"]]
        for i in range(3):
            if fr[i] > to[i]:
                fr[i], to[i] = to[i], fr[i]
            fr[i] = max(-16.0, min(32.0, fr[i]))
            to[i] = max(-16.0, min(32.0, to[i]))
        faces = {f: {"uv": face_uv(f, fr, to), "texture": "#" + mat} for f in ("north", "south", "east", "west", "up", "down")}
        e = {"from": fr, "to": to, "faces": faces}
        if mat in M.GLOW:
            e["shade"] = False
        if b.get("rot"):
            axis, angle, origin = b["rot"]
            e["rotation"] = {"angle": angle, "axis": axis, "origin": [r3(v) for v in origin]}
        elements.append(e)
    textures["particle"] = textures[boxes[0]["mat"]]
    return textures, elements


def rot_matrix(rx, ry, rz):
    ax, ay, az = (math.radians(a) for a in (rx, ry, rz))
    Rx = np.array([[1, 0, 0], [0, math.cos(ax), -math.sin(ax)], [0, math.sin(ax), math.cos(ax)]])
    Ry = np.array([[math.cos(ay), 0, math.sin(ay)], [0, 1, 0], [-math.sin(ay), 0, math.cos(ay)]])
    Rz = np.array([[math.cos(az), -math.sin(az), 0], [math.sin(az), math.cos(az), 0], [0, 0, 1]])
    return Rx @ Ry @ Rz


def corners(boxes):
    pts = []
    for b in boxes:
        (x1, y1, z1), (x2, y2, z2) = b["from"], b["to"]
        for x in (x1, x2):
            for y in (y1, y2):
                for z in (z1, z2):
                    pts.append((x - 8, y - 8, z - 8))
    return np.array(pts, dtype=float)


def place(rot, scale, grip, target):
    R = rot_matrix(*rot)
    q = np.array(grip, dtype=float) - 8
    t = np.array(target, dtype=float) - R @ (q * scale)
    t = np.clip(t, -80, 80)
    return {"rotation": list(rot), "translation": [r3(v) for v in t], "scale": [r3(scale)] * 3}


def fit(boxes, rot, box=15.0, cap=1.0):
    R = rot_matrix(*rot)
    p = (R @ corners(boxes).T).T
    ext = max(p[:, 0].max() - p[:, 0].min(), p[:, 1].max() - p[:, 1].min())
    s = min(cap, box / ext)
    cx = (p[:, 0].max() + p[:, 0].min()) / 2 * s
    cy = (p[:, 1].max() + p[:, 1].min()) / 2 * s
    return {"rotation": list(rot), "translation": [r3(-cx), r3(-cy), 0], "scale": [r3(s)] * 3}


def gun_display(boxes, grip):
    return {
        "thirdperson_righthand": place((90, 0, 0), 0.85, grip, (0, -1.5, 1.5)),
        "firstperson_righthand": place((0, 3, 0), 0.75, grip, (1.5, -3.5, 1.5)),
        "gui": fit(boxes, (25, -120, 0)),
        "ground": fit(boxes, (0, 0, 0), box=10),
        "fixed": fit(boxes, (0, 90, 0), box=15),
        "head": fit(boxes, (0, 90, 0), box=14),
    }


def melee_display(boxes, grip):
    return {
        "thirdperson_righthand": place((0, -90, -15), 0.9, grip, (0, -3.1, 1.75)),
        "firstperson_righthand": place((0, -90, 25), 0.68, grip, (1.13, -2.2, -0.84)),
        "gui": fit(boxes, (15, 30, -45)),
        "ground": fit(boxes, (0, 0, 0), box=10),
        "fixed": fit(boxes, (0, 0, -45), box=15),
    }


def small_display(boxes, gui_rot=(30, 225, 0)):
    return {
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.45, 0.45, 0.45]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "gui": fit(boxes, gui_rot, box=14.5, cap=1.0),
        "ground": fit(boxes, (0, 0, 0), box=8),
        "fixed": fit(boxes, (0, 0, 0), box=12),
    }


def model_json(boxes, display=None, parent=None):
    textures, elements = build_elements(boxes)
    out = {}
    if parent:
        out["parent"] = parent
    out["gui_light"] = "side"
    out["textures"] = textures
    out["elements"] = elements
    if display:
        out["display"] = display
    return out


# =========================================================================== registry of names

MATERIAL_SPRITES = ["raw_uranium", "uranium_ingot", "enriched_uranium", "plutonium_core", "raw_titanium",
                    "titanium_ingot", "titanium_plate", "circuit_board", "fusion_core", "antimatter_cell", "singularity_core"]
ARMOR = ["hazmat_helmet", "hazmat_suit", "hazmat_leggings", "hazmat_boots", "juggernaut_helmet", "juggernaut_chestplate",
         "juggernaut_leggings", "juggernaut_boots", "thruster_pack", "titan_boots"]
SIMPLE_BLOCKS = ["uranium_ore", "deepslate_uranium_ore", "titanium_ore", "deepslate_titanium_ore", "uranium_block",
                 "titanium_block", "trinitite", "scorched_earth", "reinforced_concrete", "hazard_block"]
BOMBS = list(M.BOMBS.keys())


def gen_textures():
    for name, img in T.material_tiles().items():
        save_png(img, os.path.join(ASSETS, "textures/item/mat", name + ".png"))
    for name, img in T.item_sprites().items():
        save_png(img, os.path.join(ASSETS, "textures/item", name + ".png"))
    for name, img in T.block_textures().items():
        save_png(img, os.path.join(ASSETS, "textures/block", name + ".png"))
    for name, img in TA.tiles().items():
        path = os.path.join(ASSETS, "textures/item/mat", name + ".png")
        save_png(img, path)
        if name in TA.ANIMATED:
            AD.anim(write_json, path, TA.ANIMATED[name])
    for name, img in TA.blocks().items():
        path = os.path.join(ASSETS, "textures/block", name + ".png")
        save_png(img, path)
        if name in TA.BLOCK_ANIMATED:
            AD.anim(write_json, path, TA.BLOCK_ANIMATED[name])
    for name, args in {"hazmat": (0xE6C21A, 0x8AF0FF, 0x2A2A2A, 0x8AF0FF, 41, "hazmat"),
                       "juggernaut": (0x3B4148, 0x9AA3AB, 0x22262A, 0xFF8A1A, 42, "juggernaut"),
                       "thruster": (0x5A636E, 0x3AF2FF, 0x2A2D31, 0x3AF2FF, 43, "thruster"),
                       "titan": (0xD8701A, 0x3AF2FF, 0x2A2D31, 0x3AF2FF, 44, "juggernaut")}.items():
        l1, l2 = T.armor_layers(*args)
        save_png(l1, os.path.join(ASSETS, "textures/models/armor", name + "_layer_1.png"))
        save_png(l2, os.path.join(ASSETS, "textures/models/armor", name + "_layer_2.png"))
    p = os.path.join(ASSETS, "textures/particle")
    save_png(T.particle_glow(), os.path.join(p, "glow.png"))
    for i in range(4):
        save_png(T.particle_spark(i), os.path.join(p, f"spark_{i}.png"))
        save_png(T.particle_flame(i), os.path.join(p, f"flame_{i}.png"))
    for i in range(8):
        save_png(T.particle_puff(500 + i), os.path.join(p, f"smoke_{i}.png"))
    for i in range(4):
        save_png(T.particle_puff(600 + i, softness=1.8), os.path.join(p, f"gas_{i}.png"))
    save_png(T.mob_effect_icon(), os.path.join(ASSETS, "textures/mob_effect/radiation.png"))
    save_png(T.mod_icon(), os.path.join(ASSETS, "icon.png"))


def gen_particles():
    smoke = [f"{NS}:smoke_{i}" for i in range(8)]
    gas = [f"{NS}:gas_{i}" for i in range(4)]
    flame = [f"{NS}:flame_{i}" for i in range(4)]
    spark = [f"{NS}:spark_{i}" for i in range(4)]
    glow = [f"{NS}:glow"]
    table = {
        "nuke_fire": smoke, "nuke_smoke": smoke, "shock_dust": smoke, "plasma_spark": glow, "arc_spark": spark,
        "radiation": glow, "cryo_mist": gas, "void_spark": glow, "napalm_flame": flame, "ember": glow,
        "toxic_gas": gas, "muzzle_flash": spark[2:3],
    }
    for name, tex in table.items():
        write_json(os.path.join(ASSETS, "particles", name + ".json"), {"textures": tex})


def gen_models():
    im = os.path.join(ASSETS, "models/item")
    bm = os.path.join(ASSETS, "models/block")
    for name, fn in M.GUNS.items():
        boxes, grip = fn()
        write_json(os.path.join(im, name + ".json"), model_json(boxes, gun_display(boxes, grip)))
    for name, fn in M.MELEE.items():
        boxes, grip = fn()
        write_json(os.path.join(im, name + ".json"), model_json(boxes, melee_display(boxes, grip)))
    for name, fn in M.SMALL.items():
        boxes = fn()
        gui_rot = (0, 0, -40) if name in ("rocket",) else (25, 210, 0)
        write_json(os.path.join(im, name + ".json"), model_json(boxes, small_display(boxes, gui_rot)))
    for name, fn in M.ENTITIES.items():
        write_json(os.path.join(ASSETS, "models/entity", name + ".json"), model_json(fn()))
    for name in MATERIAL_SPRITES + ARMOR:
        write_json(os.path.join(im, name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})

    # Blocks
    for name in SIMPLE_BLOCKS:
        write_json(os.path.join(bm, name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
    write_json(os.path.join(bm, "charred_log.json"), {"parent": "minecraft:block/cube_column",
                                                      "textures": {"end": f"{NS}:block/charred_log_top", "side": f"{NS}:block/charred_log"}})
    write_json(os.path.join(bm, "charred_log_horizontal.json"), {"parent": "minecraft:block/cube_column_horizontal",
                                                                 "textures": {"end": f"{NS}:block/charred_log_top", "side": f"{NS}:block/charred_log"}})
    for name, fn in M.BOMBS.items():
        write_json(os.path.join(bm, name + ".json"), model_json(fn(), parent="minecraft:block/block"))
    for name in SIMPLE_BLOCKS + ["charred_log"] + BOMBS:
        write_json(os.path.join(im, name + ".json"), {"parent": f"{NS}:block/{name}"})

    # Blockstates
    bs = os.path.join(ASSETS, "blockstates")
    for name in SIMPLE_BLOCKS:
        write_json(os.path.join(bs, name + ".json"), {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    write_json(os.path.join(bs, "charred_log.json"), {"variants": {
        "axis=y": {"model": f"{NS}:block/charred_log"},
        "axis=z": {"model": f"{NS}:block/charred_log_horizontal", "x": 90},
        "axis=x": {"model": f"{NS}:block/charred_log_horizontal", "x": 90, "y": 90}}})
    for name in BOMBS:
        write_json(os.path.join(bs, name + ".json"), {"variants": {
            f"facing={f}": ({"model": f"{NS}:block/{name}", "y": y} if y else {"model": f"{NS}:block/{name}"})
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})


# =========================================================================== data

def item(i, count=1):
    return {"id": i if ":" in i else f"{NS}:{i}", "count": count}


def ing(x):
    if x.startswith("#"):
        return {"tag": x[1:]}
    return {"item": x if ":" in x else f"{NS}:{x}"}


def shaped(name, pattern, key, result=None, count=1, category="equipment"):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
        "key": {k: ing(v) for k, v in key.items()}, "result": item(result or name, count)})


def shapeless(name, ingredients, result=None, count=1, category="misc"):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": [ing(i) for i in ingredients], "result": item(result or name, count)})


def cooking(name, kind, inp, out, xp, time):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": f"minecraft:{kind}", "category": "misc", "ingredient": ing(inp), "result": item(out),
        "experience": xp, "cookingtime": time})


def gen_recipes():
    shutil.rmtree(os.path.join(DATA, "recipe"), ignore_errors=True)
    for src in ("raw_uranium", "uranium_ore", "deepslate_uranium_ore"):
        cooking(f"uranium_ingot_from_smelting_{src}", "smelting", src, "uranium_ingot", 1.0, 200)
        cooking(f"uranium_ingot_from_blasting_{src}", "blasting", src, "uranium_ingot", 1.0, 100)
    for src in ("raw_titanium", "titanium_ore", "deepslate_titanium_ore"):
        cooking(f"titanium_ingot_from_smelting_{src}", "smelting", src, "titanium_ingot", 0.8, 200)
        cooking(f"titanium_ingot_from_blasting_{src}", "blasting", src, "titanium_ingot", 0.8, 100)
    cooking("glass_from_trinitite", "smelting", "trinitite", "minecraft:green_stained_glass", 0.1, 200)

    U, Ti = "uranium_ingot", "titanium_ingot"
    shaped("uranium_block", ["UUU", "UUU", "UUU"], {"U": U}, category="building")
    shapeless("uranium_ingot_from_block", ["uranium_block"], "uranium_ingot", 9)
    shaped("titanium_block", ["TTT", "TTT", "TTT"], {"T": Ti}, category="building")
    shapeless("titanium_ingot_from_block", ["titanium_block"], "titanium_ingot", 9)
    shapeless("titanium_plate", [Ti, Ti, Ti], count=2)
    shaped("circuit_board", ["RQR", "GCG"], {"R": "minecraft:redstone", "Q": "minecraft:quartz", "G": "minecraft:gold_nugget", "C": "minecraft:copper_ingot"}, count=2, category="misc")
    shaped("enriched_uranium", ["UGU", "GRG", "UGU"], {"U": U, "G": "minecraft:glowstone_dust", "R": "minecraft:redstone_block"}, count=4, category="misc")
    shaped("plutonium_core", ["EEE", "EDE", "EEE"], {"E": "enriched_uranium", "D": "minecraft:diamond"}, category="misc")
    shaped("fusion_core", ["PBP", "GEG", "PCP"], {"P": "titanium_plate", "B": "minecraft:blaze_powder", "G": "minecraft:glass", "E": "enriched_uranium", "C": "circuit_board"}, category="misc")
    shaped("antimatter_cell", ["PGP", "EFE", "PGP"], {"P": "titanium_plate", "G": "minecraft:glass", "E": "minecraft:ender_pearl", "F": "fusion_core"}, count=2, category="misc")
    shaped("singularity_core", ["AOA", "ONO", "AOA"], {"A": "antimatter_cell", "O": "minecraft:crying_obsidian", "N": "minecraft:nether_star"}, category="misc")

    # Ammunition
    shaped("mini_nuke", ["ITI", "TET", "ITI"], {"I": "minecraft:iron_ingot", "T": "minecraft:tnt", "E": "enriched_uranium"}, count=2)
    shaped("rocket", [" I ", "ITI", "IGI"], {"I": "minecraft:iron_ingot", "T": "minecraft:tnt", "G": "minecraft:gunpowder"}, count=4)
    shaped("plasma_cell", [" T ", "RGR", " T "], {"T": Ti, "R": "minecraft:redstone", "G": "minecraft:glowstone_dust"}, count=4)
    shaped("railgun_slug", ["T", "T", "C"], {"T": Ti, "C": "minecraft:copper_ingot"}, count=8)
    shaped("bullet_belt", ["CCC", "GGG", "III"], {"C": "minecraft:copper_ingot", "G": "minecraft:gunpowder", "I": "minecraft:iron_ingot"}, count=2)
    shaped("fuel_canister", ["I I", "IBI", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:blaze_powder"}, count=2)
    shaped("cryo_canister", [" I ", "PSP", " I "], {"I": "minecraft:iron_ingot", "P": "minecraft:packed_ice", "S": "minecraft:snowball"}, count=2)

    # Weapons
    shaped("nuke_launcher", ["PPP", "CFT", " T "], {"P": "titanium_plate", "C": "circuit_board", "F": "fusion_core", "T": Ti})
    shaped("rocket_launcher", ["PPP", "TCT", "P  "], {"P": "titanium_plate", "T": "minecraft:tnt", "C": "circuit_board"})
    shaped("mirv_launcher", ["CRC", "RLR", "CRC"], {"C": "circuit_board", "R": "rocket", "L": "rocket_launcher"})
    shaped("plasma_rifle", ["PGG", "TCE", "T  "], {"P": "titanium_plate", "G": "minecraft:glass", "T": Ti, "C": "circuit_board", "E": "enriched_uranium"})
    shaped("railgun", ["CCC", "TFT", "P  "], {"C": "minecraft:copper_block", "T": "titanium_plate", "F": "fusion_core", "P": Ti})
    shaped("minigun", ["TTT", "TBC", " PP"], {"T": Ti, "B": "minecraft:iron_block", "C": "circuit_board", "P": "titanium_plate"})
    shaped("flamethrower", ["NB ", "PCP", "I  "], {"N": "minecraft:flint_and_steel", "B": "minecraft:blaze_rod", "P": "titanium_plate", "C": "circuit_board", "I": "minecraft:iron_ingot"})
    shaped("arc_thrower", ["CLC", "PRP", "T  "], {"C": "minecraft:copper_ingot", "L": "minecraft:lightning_rod", "P": "titanium_plate", "R": "minecraft:redstone_block", "T": Ti})
    shaped("cryo_cannon", ["PIP", "KCK", "T  "], {"P": "titanium_plate", "I": "minecraft:blue_ice", "K": "cryo_canister", "C": "circuit_board", "T": Ti})
    shaped("singularity_projector", ["OAO", "ESE", "T  "], {"O": "minecraft:obsidian", "A": "antimatter_cell", "E": "minecraft:ender_eye", "S": "singularity_core", "T": Ti})
    shaped("orbital_designator", ["GEG", "CFC", "PPP"], {"G": "minecraft:glass", "E": "minecraft:ender_eye", "C": "circuit_board", "F": "fusion_core", "P": "titanium_plate"})
    shaped("storm_caller", ["GCG", " L ", " T "], {"G": "minecraft:gold_block", "C": "minecraft:copper_block", "L": "minecraft:lightning_rod", "T": Ti})
    shaped("gravity_hammer", ["PBP", "PEP", " T "], {"P": "titanium_plate", "B": "titanium_block", "E": "minecraft:ender_eye", "T": Ti})
    shaped("plasma_blade", [" G ", " G ", "TFT"], {"G": "minecraft:glass_pane", "T": Ti, "F": "fusion_core"})

    grenade_core = {"frag_grenade": "minecraft:iron_ingot", "plasma_grenade": "plasma_cell", "cryo_grenade": "cryo_canister",
                    "incendiary_grenade": "minecraft:blaze_powder", "gravity_grenade": "minecraft:ender_pearl",
                    "toxic_grenade": "minecraft:fermented_spider_eye"}
    for name, core in grenade_core.items():
        shaped(name, [" I ", "CTC", " I "], {"I": "minecraft:iron_nugget", "C": core, "T": "minecraft:tnt"}, count=4)
    shaped("pocket_nuke", ["IEI", "ETE", "IEI"], {"I": "minecraft:iron_ingot", "E": "enriched_uranium", "T": "minecraft:tnt"}, count=2)

    # Tools & gear
    shaped("atomic_pickaxe", ["PEP", " T ", " T "], {"P": "titanium_plate", "E": "enriched_uranium", "T": Ti}, category="equipment")
    shaped("fusion_shovel", [" P ", " C ", " T "], {"P": "titanium_plate", "C": "circuit_board", "T": Ti})
    shaped("plasma_cutter", ["PF ", "PT ", " T "], {"P": "titanium_plate", "F": "fusion_core", "T": Ti})
    shaped("quantum_drill", ["DDP", "CFP", " TP"], {"D": "minecraft:diamond", "C": "circuit_board", "F": "fusion_core", "P": "titanium_plate", "T": Ti})
    shaped("detonator", [" R ", "ICI", "III"], {"R": "minecraft:redstone_torch", "C": "circuit_board", "I": "minecraft:iron_ingot"})
    shaped("geiger_counter", ["IGI", "ICI", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "C": "circuit_board"})
    shapeless("rad_x", ["minecraft:glass_bottle", "minecraft:golden_carrot", "minecraft:sugar"], count=2)
    shapeless("nano_medkit", ["minecraft:golden_apple", "circuit_board", "minecraft:white_wool", "minecraft:red_dye"], count=2)

    W = "minecraft:yellow_wool"
    shaped("hazmat_helmet", ["WWW", "WGW"], {"W": W, "G": "minecraft:glass_pane"})
    shaped("hazmat_suit", ["W W", "WLW", "WWW"], {"W": W, "L": "minecraft:leather"})
    shaped("hazmat_leggings", ["WWW", "W W", "W W"], {"W": W})
    shaped("hazmat_boots", ["W W", "L L"], {"W": W, "L": "minecraft:leather"})
    P, N = "titanium_plate", "minecraft:netherite_ingot"
    shaped("juggernaut_helmet", ["PPP", "PNP"], {"P": P, "N": N})
    shaped("juggernaut_chestplate", ["P P", "PNP", "PPP"], {"P": P, "N": N})
    shaped("juggernaut_leggings", ["PPP", "PNP", "P P"], {"P": P, "N": N})
    shaped("juggernaut_boots", ["PNP", "P P"], {"P": P, "N": N})
    shaped("thruster_pack", ["PFP", "PCP", "B B"], {"P": P, "F": "fusion_core", "C": "circuit_board", "B": "minecraft:blaze_rod"})

    # Blocks & warheads
    shaped("reinforced_concrete", ["SIS", "ISI", "SIS"], {"S": "minecraft:smooth_stone", "I": "minecraft:iron_bars"}, count=8, category="building")
    shaped("hazard_block", ["YBY", "BIB", "YBY"], {"Y": "minecraft:yellow_dye", "B": "minecraft:black_dye", "I": "minecraft:iron_block"}, count=8, category="building")
    shaped("demolition_charge", ["CTC", "TRT", "CTC"], {"C": "minecraft:clay_ball", "T": "minecraft:tnt", "R": "minecraft:redstone"}, count=2, category="redstone")
    shaped("thermobaric_bomb", ["TBT", "ICI", "TBT"], {"T": "minecraft:tnt", "B": "minecraft:blaze_powder", "I": "minecraft:iron_block", "C": "circuit_board"}, category="redstone")
    shaped("cryo_bomb", ["KPK", "PCP", "KPK"], {"K": "cryo_canister", "P": "titanium_plate", "C": "circuit_board"}, category="redstone")
    shaped("singularity_bomb", ["ASA", "PCP", "APA"], {"A": "antimatter_cell", "S": "singularity_core", "P": "titanium_plate", "C": "circuit_board"}, category="redstone")
    shaped("neutron_bomb", ["ETE", "PCP", "EPE"], {"E": "enriched_uranium", "T": "titanium_block", "P": "titanium_plate", "C": "circuit_board"}, category="redstone")
    shaped("atomic_bomb", ["PEP", "ECE", "PUP"], {"P": "titanium_plate", "E": "enriched_uranium", "C": "circuit_board", "U": "plutonium_core"}, category="redstone")
    shaped("antimatter_bomb", ["GAG", "AFA", "PCP"], {"G": "minecraft:glass", "A": "antimatter_cell", "F": "fusion_core", "P": "titanium_plate", "C": "circuit_board"}, category="redstone")
    shaped("hydrogen_bomb", ["PUP", "FAF", "PUP"], {"P": "titanium_plate", "U": "plutonium_core", "F": "fusion_core", "A": "atomic_bomb"}, category="redstone")

    # Insane tier
    shaped("dubstep_gun", ["NJN", "PCP", "T  "], {"N": "minecraft:note_block", "J": "minecraft:jukebox", "P": "titanium_plate", "C": "circuit_board", "T": Ti})
    shaped("death_ray", ["AFA", "PCP", "T  "], {"A": "minecraft:amethyst_shard", "F": "fusion_core", "P": "titanium_plate", "C": "circuit_board", "T": Ti})
    shaped("apocalypse_gatling", ["NNN", "MCF", "PP "], {"N": "mini_nuke", "M": "minigun", "C": "circuit_board", "F": "fusion_core", "P": "titanium_plate"})
    shaped("annihilator_cannon", ["AEA", "PFP", "T  "], {"A": "antimatter_cell", "E": "minecraft:emerald_block", "P": "titanium_plate", "F": "fusion_core", "T": Ti})
    shaped("meteor_staff", ["FMF", " B ", " O "], {"F": "minecraft:fire_charge", "M": "minecraft:magma_block", "B": "minecraft:blaze_rod", "O": "minecraft:obsidian"})
    shaped("kinetic_designator", ["BBB", "CFC", "PPP"], {"B": "titanium_block", "C": "circuit_board", "F": "fusion_core", "P": "titanium_plate"})
    shaped("airstrike_beacon", [" R ", "ICI", " T "], {"R": "minecraft:redstone_torch", "I": "minecraft:iron_ingot", "C": "circuit_board", "T": "minecraft:tnt"})
    shaped("nuke_barrage", ["NNN", "CFC", "PUP"], {"N": "mini_nuke", "C": "circuit_board", "F": "fusion_core", "P": "titanium_plate", "U": "plutonium_core"})
    shaped("city_capsule", ["GGG", "BCB", "SSS"], {"G": "minecraft:glass", "B": "minecraft:bricks", "C": "circuit_board", "S": "minecraft:stone"}, count=2)
    shapeless("overdrive_stim", ["minecraft:glass_bottle", "minecraft:blaze_powder", "minecraft:sugar", "minecraft:redstone", "minecraft:glowstone_dust"], count=2)
    shaped("titan_boots", ["PFP", "S S"], {"P": "titanium_plate", "F": "fusion_core", "S": "minecraft:slime_block"})
    shaped("tectonic_bomb", ["TPT", "PCP", "TPT"], {"T": "minecraft:tnt", "P": "minecraft:piston", "C": "circuit_board"}, category="redstone")
    shaped("bass_drop_bomb", ["NJN", "JTJ", "NJN"], {"N": "minecraft:note_block", "J": "minecraft:jukebox", "T": "minecraft:tnt"}, category="redstone")
    shaped("cluster_nuke", ["MMM", "MCM", "MMM"], {"M": "mini_nuke", "C": "circuit_board"}, category="redstone")
    shaped("nova_bomb", ["GFG", "FUF", "GFG"], {"G": "minecraft:glass", "F": "fusion_core", "U": "plutonium_core"}, category="redstone")
    shaped("omega_bomb", ["AHA", "SCS", "AHA"], {"A": "antimatter_bomb", "H": "hydrogen_bomb", "S": "singularity_core", "C": "circuit_board"}, category="redstone")


def gen_loot():
    lt = os.path.join(DATA, "loot_table/blocks")
    for name in SIMPLE_BLOCKS + ["charred_log"] + BOMBS:
        if name.endswith("_ore"):
            continue
        write_json(os.path.join(lt, name + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"{NS}:blocks/{name}"})
    for ore, raw in (("uranium_ore", "raw_uranium"), ("deepslate_uranium_ore", "raw_uranium"),
                     ("titanium_ore", "raw_titanium"), ("deepslate_titanium_ore", "raw_titanium")):
        write_json(os.path.join(lt, ore + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{ore}", "conditions": [{"condition": "minecraft:match_tool",
                    "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": f"{NS}:{raw}", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"function": "minecraft:explosion_decay"}]}]}]}], "random_sequence": f"{NS}:blocks/{ore}"})


def tag(path, values):
    write_json(path, {"replace": False, "values": values})


def gen_tags():
    mc = os.path.join(ROOT, "data", "minecraft", "tags")
    ores = [f"{NS}:{n}" for n in ("uranium_ore", "deepslate_uranium_ore", "titanium_ore", "deepslate_titanium_ore")]
    pick = ores + [f"{NS}:{n}" for n in ("uranium_block", "titanium_block", "trinitite", "reinforced_concrete", "hazard_block")] + [f"{NS}:{b}" for b in BOMBS]
    pick += [f"{NS}:{b}" for b in AD.PICKAXE if f"{NS}:{b}" not in pick]
    tag(os.path.join(mc, "block/mineable/pickaxe.json"), pick)
    tag(os.path.join(mc, "block/mineable/shovel.json"), [f"{NS}:scorched_earth"])
    tag(os.path.join(mc, "block/mineable/axe.json"), [f"{NS}:charred_log"])
    tag(os.path.join(mc, "block/needs_iron_tool.json"), ores + [f"{NS}:uranium_block", f"{NS}:titanium_block"])
    tag(os.path.join(mc, "block/needs_diamond_tool.json"), [f"{NS}:reinforced_concrete"])
    tag(os.path.join(mc, "block/logs.json"), [f"{NS}:charred_log"])
    tag(os.path.join(DATA, "tags/block/mineable/drill.json"), ["#minecraft:mineable/pickaxe", "#minecraft:mineable/shovel", "#minecraft:mineable/axe"])

    c = os.path.join(ROOT, "data", "c", "tags")
    tag(os.path.join(c, "item/ingots/uranium.json"), [f"{NS}:uranium_ingot"])
    tag(os.path.join(c, "item/ingots/titanium.json"), [f"{NS}:titanium_ingot"])
    tag(os.path.join(c, "item/raw_materials/uranium.json"), [f"{NS}:raw_uranium"])
    tag(os.path.join(c, "item/raw_materials/titanium.json"), [f"{NS}:raw_titanium"])
    tag(os.path.join(c, "block/ores/uranium.json"), ores[:2])
    tag(os.path.join(c, "block/ores/titanium.json"), ores[2:])

    dt = os.path.join(mc, "damage_type")
    tag(os.path.join(dt, "is_explosion.json"), [f"{NS}:nuclear_blast", f"{NS}:annihilation"])
    tag(os.path.join(dt, "bypasses_armor.json"), [f"{NS}:radiation", f"{NS}:annihilation"])
    tag(os.path.join(dt, "bypasses_shield.json"), [f"{NS}:radiation", f"{NS}:nuclear_blast", f"{NS}:annihilation"])
    tag(os.path.join(dt, "no_knockback.json"), [f"{NS}:radiation"])
    tag(os.path.join(dt, "is_projectile.json"), [f"{NS}:railgun", f"{NS}:shrapnel"])
    tag(os.path.join(dt, "is_fire.json"), [f"{NS}:plasma"])
    tag(os.path.join(dt, "is_freezing.json"), [f"{NS}:cryo"])
    tag(os.path.join(dt, "is_lightning.json"), [f"{NS}:electrocution"])
    tag(os.path.join(dt, "bypasses_cooldown.json"), [f"{NS}:radiation"])


def gen_damage_types():
    for name, effects in (("nuclear_blast", "burning"), ("radiation", None), ("plasma", "burning"), ("railgun", None),
                          ("annihilation", None), ("electrocution", None), ("cryo", "freezing"), ("shrapnel", None)):
        obj = {"exhaustion": 0.1, "message_id": f"{NS}.{name}", "scaling": "when_caused_by_living_non_player"}
        if effects:
            obj["effects"] = effects
        write_json(os.path.join(DATA, "damage_type", name + ".json"), obj)


def gen_worldgen():
    for name, ore, deep, size, count, lo, hi, discard in (
            ("uranium", "uranium_ore", "deepslate_uranium_ore", 5, 4, -48, 24, 0.3),
            ("titanium", "titanium_ore", "deepslate_titanium_ore", 7, 6, -32, 56, 0.0)):
        write_json(os.path.join(DATA, "worldgen/configured_feature", f"ore_{name}.json"), {
            "type": "minecraft:ore", "config": {"discard_chance_on_air_exposure": discard, "size": size, "targets": [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": f"{NS}:{ore}"}},
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": {"Name": f"{NS}:{deep}"}}]}})
        write_json(os.path.join(DATA, "worldgen/placed_feature", f"ore_{name}.json"), {
            "feature": f"{NS}:ore_{name}", "placement": [
                {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                              "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
                {"type": "minecraft:biome"}]})


SOUNDS = {
    "nuke.detonate": [("random/explode1", 0.45), ("random/explode2", 0.4), ("random/explode3", 0.45), ("random/explode4", 0.4)],
    "nuke.rumble": [("ambient/weather/thunder1", 0.5), ("ambient/weather/thunder2", 0.45), ("ambient/weather/thunder3", 0.5)],
    "bomb.armed": [("block/beacon/activate", 0.8)],
    "bomb.beep": [("note/pling", 1.0)],
    "bomb.antimatter": [("mob/wither/spawn", 0.6)],
    "bomb.freeze": [("random/glass1", 0.5), ("random/glass2", 0.5), ("random/glass3", 0.5)],
    "weapon.plasma_fire": [("item/crossbow/shoot1", 1.8), ("item/crossbow/shoot2", 1.7), ("item/crossbow/shoot3", 1.9)],
    "weapon.plasma_impact": [("random/fizz", 1.4)],
    "weapon.railgun_charge": [("block/beacon/activate", 1.6)],
    "weapon.railgun_fire": [("item/trident/thunder1", 1.6), ("item/trident/thunder2", 1.5)],
    "weapon.minigun_fire": [("fireworks/blast1", 1.5), ("fireworks/blast_far1", 1.3)],
    "weapon.minigun_spin": [("minecart/base", 1.6)],
    "weapon.flamethrower": [("mob/blaze/breathe1", 0.6), ("mob/blaze/breathe2", 0.6), ("fire/fire", 1.4)],
    "weapon.arc_zap": [("random/fizz", 2.0), ("fire/ignite", 1.8)],
    "weapon.cryo_blast": [("random/glass1", 1.4), ("random/glass2", 1.3)],
    "weapon.rocket_launch": [("mob/ghast/fireball4", 0.9)],
    "weapon.nuke_launch": [("mob/ghast/fireball4", 0.55)],
    "weapon.reload": [("random/click", 0.9)],
    "weapon.dry_fire": [("random/click", 2.0)],
    "weapon.gravity_slam": [("random/anvil_land", 0.5)],
    "weapon.blade_dash": [("item/trident/riptide1", 1.4), ("item/trident/riptide2", 1.4)],
    "black_hole.hum": [("portal/portal", 0.5)],
    "black_hole.collapse": [("portal/travel", 0.6), ("random/explode3", 0.4)],
    "orbital.charge": [("block/beacon/activate", 1.2)],
    "orbital.strike": [("item/trident/thunder1", 0.7), ("item/trident/thunder2", 0.6)],
    "item.geiger_click": [("random/click", 2.0), ("note/hat", 2.0)],
    "item.jetpack": [("fire/fire", 1.8)],
    "item.detonator": [("random/click", 1.5)],
    "item.inject": [("item/bottle/fill", 1.6)],
    "item.equip_power_armor": [("item/armor/equip_netherite1", 0.8), ("item/armor/equip_netherite2", 0.8)],
    "item.equip_hazmat": [("item/armor/equip_leather1", 0.9), ("item/armor/equip_leather2", 0.9)],
    "weapon.dubstep_wub": [("note/bass", 1.0), ("note/bassattack", 1.0)],
    "weapon.dubstep_drop": [("mob/enderdragon/growl1", 0.5), ("random/explode1", 0.6)],
    "weapon.death_ray": [("mob/guardian/attack_loop", 1.4)],
    "strike.jet_flyby": [("item/elytra/elytra_loop", 0.5)],
    "item.titan_landing": [("random/anvil_land", 0.6), ("random/explode2", 0.8)],
}


def gen_sounds():
    out = {}
    for key, entries in SOUNDS.items():
        out[key] = {"subtitle": f"subtitles.{NS}.{key}",
                    "sounds": [{"name": f"minecraft:{n}", "pitch": p, "attenuation_distance": 64 if key.startswith(("nuke", "bomb", "orbital", "black_hole", "strike")) else 24}
                               for n, p in entries]}
    write_json(os.path.join(ASSETS, "sounds.json"), out)


def gen_fx_atlas():
    import fx_atlas
    out = os.path.join(ASSETS, "textures", "fx")
    os.makedirs(out, exist_ok=True)
    fx_atlas.write(os.path.join(out, "atlas.png"))
    from PIL import Image as _I
    _I.new("RGBA", (16, 16), (255, 255, 255, 255)).save(os.path.join(out, "white.png"))


def gen_shaders():
    write_json(os.path.join(ASSETS, "shaders/post/armageddon_fx.json"), {
        "targets": ["swap"],
        "passes": [{"name": "armageddon_fx", "intarget": "minecraft:main", "outtarget": "swap"},
                   {"name": "blit", "intarget": "swap", "outtarget": "minecraft:main"}]})
    uniforms = [{"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
                {"name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0]},
                {"name": "OutSize", "type": "float", "count": 2, "values": [1.0, 1.0]}]
    for n in ("Flash", "FlashR", "FlashG", "FlashB", "Distortion", "Radiation", "GTime", "LensX", "LensY", "LensR", "LensStrength",
              "GodX", "GodY", "GodStrength", "GodR", "GodG", "GodB", "Ripple", "Hue",
              "Desaturate", "Whiteout", "Blackout", "Static", "Invert", "Impact", "ImpactMode", "Cine", "CineFade"):
        uniforms.append({"name": n, "type": "float", "count": 1, "values": [0.0]})
    prog = os.path.join(ROOT, "assets", "minecraft", "shaders", "program")
    write_json(os.path.join(prog, "armageddon_fx.json"), {
        "blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"},
        "vertex": "sobel", "fragment": "armageddon_fx", "attributes": ["Position"],
        "samplers": [{"name": "DiffuseSampler"}], "uniforms": uniforms})
    src = os.path.join(os.path.dirname(os.path.abspath(__file__)), "armageddon_fx.fsh")
    os.makedirs(prog, exist_ok=True)
    shutil.copy(src, os.path.join(prog, "armageddon_fx.fsh"))
    # The depth-aware vision pass (drawn by the client's Vision class, not by a post chain).
    vu = [{"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
          {"name": "InvViewProj", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
          {"name": "Targets", "type": "matrix4x4", "count": 16, "values": [0] * 16},
          {"name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0]},
          {"name": "OutSize", "type": "float", "count": 2, "values": [1.0, 1.0]}]
    for n in ("Origin", "CamFrac", "Color"):
        vu.append({"name": n, "type": "float", "count": 3, "values": [0.0, 0.0, 0.0]})
    for n in ("Mode", "Strength", "Time", "Radius", "Reveal", "Param", "P11"):
        vu.append({"name": n, "type": "float", "count": 1, "values": [0.0]})
    write_json(os.path.join(prog, "armageddon_vision.json"), {
        "blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"},
        "vertex": "sobel", "fragment": "armageddon_vision", "attributes": ["Position"],
        "samplers": [{"name": "DiffuseSampler"}, {"name": "DepthSampler"}], "uniforms": vu})
    shutil.copy(os.path.join(os.path.dirname(os.path.abspath(__file__)), "armageddon_vision.fsh"), os.path.join(prog, "armageddon_vision.fsh"))


def gen_lang():
    d = L.build()
    d.update(AD.lang())
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), d)


def main():
    gen_lang()
    gen_textures()
    gen_particles()
    gen_models()
    gen_recipes()
    gen_loot()
    gen_tags()
    gen_damage_types()
    gen_worldgen()
    gen_sounds()
    gen_shaders()
    gen_fx_atlas()
    AD.blocks(write_json, ASSETS, DATA, NS)
    AD.recipes(shaped, shapeless)
    print("Assets generated into", ROOT)


if __name__ == "__main__":
    main()
