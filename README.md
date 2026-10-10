# Armageddon Arsenal

A stylized **weapons-of-mass-destruction** mod for **Minecraft 1.21.1 (Fabric, Java 21)**.
Nukes with real mushroom clouds, Tsar hydrogen bombs, antimatter, black holes, orbital lasers,
railguns, plasma weapons, power armour and more. Every weapon has a hand-built 3D model and
dedicated visual effects: custom particles, energy beams, shockwaves, screen shake and a
full-screen post-processing shader.

## Highlights

| | |
|---|---|
| **Mushroom clouds** | Hundreds of emissive smoke particles form a rising, rolling toroidal cap, stem, base surge and condensation ring. Light reaches you first; the shockwave, shake and boom arrive later depending on distance (speed of sound ≈ 17 blocks/tick). |
| **Progressive craters** | Terrain is carved over several ticks with a per-tick budget, so even a 52-block hydrogen bomb doesn't freeze the server. Craters are bowl-shaped with noisy edges, glassed trinitite linings, scorched earth, charred forests and fires. |
| **Fallout** | Nukes leave radioactive zones that cause Radiation Sickness. Wear a hazmat suit, carry a Geiger counter, pack Rad-X. |
| **Black holes** | Pull in mobs and players, rip blocks out of the ground and swallow them, then collapse. Rendered with an event horizon, photon ring and tilted accretion disc, plus real-time **gravitational lensing** in the post shader. |
| **Post-processing shader** | Nuclear flash bloom, heat haze, chromatic aberration, radiation-sickness vision (green cast, film grain, vignette, glitches) and black-hole lensing. |

## Content

**Warheads (blocks)** — Demolition Charge, Thermobaric Bomb, Cryogenic Bomb, Singularity Bomb, Neutron
Bomb, Atomic Bomb, Antimatter Bomb, Tsar Hydrogen Bomb. Arm with flint & steel, a fire charge, redstone,
or a Remote Detonator. Warheads caught in an explosion detonate sympathetically.

**Heavy weapons**
- M-42 "Fat Boy" Nuke Launcher — catapults mini-nukes (full mushroom cloud + fallout)
- Hydra Quad Rocket Pod — four homing rockets that lock onto what you aim at
- Cerberus MIRV Launcher — splits into ten bomblets mid-air
- Helios Plasma Rifle — fully automatic plasma bolts
- Gauss Railgun — charge and release; pierces every entity on the line and punches through soft blocks
- Shredder Rotary Cannon — spin-up minigun with hitscan tracers
- Inferno Flamethrower — napalm stream that sets everything alight
- Tesla Arc Thrower — chain lightning that jumps between six targets
- Absolute Zero Cryo Cannon — flash-freezes targets and terrain
- Event Horizon Singularity Projector — fires a micro black hole
- Orbital Strike Designator — paint a target; three seconds later a laser from orbit bores a molten shaft
- Storm Caller — summons a lightning storm
- Gravity Hammer — launches enemies; right-click for a seismic ground slam
- Annihilator Plasma Blade — ignites on hit; right-click to phase-dash through enemies

**Insane tier** — WUB-9000 Dubstep Gun, Oblivion Death Ray (bores tunnels), Apocalypse Gatling (fires nukes),
BFG Annihilator Cannon, Staff of Extinction (meteor shower), Rods from God Uplink (kinetic bombardment), Airstrike
Beacon (bomber carpet-bombs your aim line), Nuclear Football (8-nuke barrage), Titan Boots (super jump + shockwave
landings), Overdrive Stim. Bombs: Tectonic Earthquake Bomb, Bass Drop Bomb, Cluster Nuke, Nova Bomb (mini sun →
supernova) and the OMEGA Planet Cracker (78-block crater, blood-red sky).

**Test city** — use a Test City Capsule on the ground, or run `/armageddon city [1-4]`, to build a city of
skyscrapers, roads, street lights and parks (with villagers) to destroy. `/armageddon purge` clears fallout,
black holes, stars, pending strikes and crater jobs.

**Grenades** — Frag, Plasma (sticky), Cryo, Napalm, Gravity Well, Toxin, Pocket Nuke.

**Tools & gear** — Atomic Excavator (3x3 pickaxe), Fusion Shovel (3x3), Plasma Cutter (tree feller),
Quantum Tunnel Drill (3x3x3 tunnels), Remote Detonator, Geiger Counter, Rad-X, Nano-Medkit.

**Armour** — Hazmat Suit (radiation immunity), Juggernaut power armour (immune to explosions and fire),
Thruster Pack (flight).

**Materials & worldgen** — Uranium and Titanium ore (Overworld, deep), enriched uranium, plutonium cores,
fusion cores, antimatter cells, singularity cores, circuit boards, titanium plates. Everything is craftable;
check recipes in-game (REI/EMI/JEI work out of the box).

## Controls

- **Semi-auto weapons** (launchers, cannons): right-click to fire.
- **Automatic weapons** (plasma rifle, minigun, flamethrower, arc thrower): hold right-click. You keep full
  movement speed while firing.
- **Railgun**: hold right-click to charge, release to fire.
- **Ammo**: weapons reload automatically from the matching ammo item in your inventory. The ammo bar on the
  item and the counter beside the crosshair show the magazine.
- **Detonator**: sneak + right-click warheads to link them, then right-click in the air to fire them all.
- **Thruster Pack**: double-tap jump to fly while wearing it.

## Game rules

| Rule | Default | Effect |
|---|---|---|
| `armageddonBlockDamage` | `true` | When `false`, weapons hurt entities and look spectacular but leave terrain untouched. |
| `armageddonFallout` | `true` | When `false`, nukes leave no radioactive fallout zones. |
| `armageddonCraterSpeed` | `60000` | Block checks per tick for crater carving. Lower it on weak servers. |

## Building

Requirements: JDK 21.

```sh
./gradlew build          # jar ends up in build/libs/
./gradlew runClient      # launch a dev client
```

Every push is also built by GitHub Actions (`.github/workflows/build.yml`); the jar is attached to the run
as an artifact.

### Regenerating assets

All textures, 3D models, blockstates, recipes, loot tables, tags, worldgen, sounds, language strings and
shaders are generated procedurally:

```sh
pip install pillow numpy
python3 tools/generate_assets.py
```

- `tools/models.py` — the cuboid 3D models (guns point along -Z, melee along +Y, projectiles along +X).
  Display transforms (first/third person, GUI) are computed automatically from each model's grip point.
- `tools/gen_textures.py` — pixel-art texture painter (materials, ores, armour layers, particles, icons).
- `tools/armageddon_fx.fsh` — the post-processing fragment shader.
- `tools/lang.py` — English strings.

## Project layout

```
src/main/java/com/armageddon/      common code (items, blocks, entities, detonation engine, networking)
src/client/java/com/armageddon/    client code (renderers, particles, FX engine, post shader, HUD, mixins)
src/main/resources/                generated assets + data
tools/                             asset generator
```

## Notes

- Sounds reuse vanilla audio files under new sound events (`assets/armageddon/sounds.json`), so you can drop
  in your own `.ogg` files without touching code.
- The post-processing shader degrades gracefully: if it fails to load, flashes fall back to a HUD overlay.
