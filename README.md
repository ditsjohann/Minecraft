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
| **Fallout** | Nukes leave radioactive zones that cause Radiation Sickness. Wear a hazmat suit, carry a Geiger counter (and watch the HUD meter), pack Rad-X. |
| **Black holes** | Pull in mobs and players, rip blocks out of the ground and swallow them, then collapse. Rendered with an event horizon, photon ring and tilted accretion disc, plus real-time **gravitational lensing** in the post shader. |
| **Post-processing shader** | Nuclear flash bloom, heat haze, chromatic aberration and black-hole lensing. |

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

**Celestial II** — EXPLODING COWS (cows fall as bombs, wave after wave, bigger each time, then one enormous cow
that goes off and leaves a milky crater), DEADLY TWISTER (a mile-high twister that wanders and drags players from
safety), COLOSSAL SPEAR (a winged, building-sized spear falls for five seconds out of a heavenly sky and drills to
the core), ZOMBIE APOCALYPSE (a growing undead tide and wandering giants that explode green) and UFO INVASION (a
city-wide mothership with a blue beam of death, flanked by scout saucers leaving trails of explosions).

**CITY DESTRUCTION tier** — a city-sized area or more is remade: MOONFALL (a cratered moon breaks the sound
barrier and lands), WORLD EATER (a gigantic cosmic worm tunnels through the land), RAGNAROK (lightning, meteors,
quakes and volcanoes, then Surtr's burning sword splits the world into fire and frost), ETERNAL SAPLING (a sapling
climbs eight ever-bigger trees to a world tree whose roots slam down and turn the land to forest), 32 CHUNKS (the
thirty-two chunks around it each roll a random mutation: floods, flips, swaps, voids, mob parties) and MAIN STAGE
(studio rigging comes down, the land is struck into a studio floor with a real stage, and a painted-sky wall with an
EXIT door goes up around you). Celestial also gains BIG BANG (a new solar system, alone in a void) and CHRONO BREAK
(a sphere of world is erased, then rewound).

**WORLD DESTRUCTION tier (the real one)** — the world enders, with camera cutscenes. GALACTIC SMASH: a faceless void
titan tears the sky open, hunts the city with burning eyes, winds up and punches the ground, and the ejecta rains
back down for fifteen seconds. BOOMING TICK: a clock in the sky drops one burning numeral every five seconds, each
boom bigger than the last, then a cutscene over the carpet-bombed site and out to Earth exploding. ALIEN DEATH RAY:
a cutscene to an alien world where a colossal ray cannon rises, charges and fires at Earth, then an EMP, a tsunami
and the ray itself coming down.

**The strong five** — seen through a new depth-aware vision pass that rebuilds the world from the depth buffer.
LIDAR STRIKE (Celestial): the world turns into a lidar point cloud, four scan pulses tag everything alive, then the
strikes walk the scanned map and the mast overloads. SHADOW VOID (Celestial): a volumetric sphere of darkness with
an event core swallows the ground shell by shell, inhales, and exhales a shockwave of shadow. SPECTRAL ICE
(Celestial): ice spikes erupt into a pyramid with one point, the apex lights blue, the spikes glow harder and harder,
and the white apex sprays a storm of ice shards that each leave a small crater and a fresh spike. TERMITE DOMAIN
(City Destruction): thousands of ants eat every living thing out to 90 blocks, then raise a seventy-block termite
hill with spires, vents and tunnel mouths. THE SUN DISAPPEARS (World Destruction, the strongest yet): the sun goes
out for the whole world: endless night, a starless sky, fog, frost on the lens, frozen water, dying plants, and
freezing for anyone not standing by a fire; then the sun's corpse falls, and the light comes back in the blast.

**Plasma and sci-fi** — none of these is an explosion. ION SCYTHE (Celestial): an orbital ion beam carves a spiral
trench of molten glass outward, charges it, then the whole spiral ignites as a curtain of light that disintegrates
the land. PHASE SCRUBBER (City Destruction): a hex force field drops over the town and a scan plane descends through
it, erasing everything it passes, hills included, then prints a lit test-chamber floor and shatters. TESLA DOMINION
(City Destruction): a tesla tower rises and arcs at the tallest things around; each strike burns a branching
fulgurite of glass into the ground, and the ball lightning on its crown discharges everywhere at once. EVENT HORIZON
ENGINE (World Destruction): three rings open a singularity in the sky, the land is torn up into its accretion disk
and spun faster and faster until it all goes in. NANITE BLOOM (World Destruction): grey goo runs out in branching
fingers turning everything to metal in its own shape, then the region melts into a silver hex plain. INVERSION
ENGINE (World Destruction): a sphere of the world is flipped upside down into the sky over its own pit, hangs there,
and shatters back down.

**Oddities (Legendary and Mythic)** — Sonar (green echo vision, shoving rings that shatter glass, a sonic boom),
8-Bit (the world goes low-res on screen and in the blocks, then GAME OVER), Magnetar (iron, ore included, ripped into
orbit, pulsar jets), KA-BOOM! (comic ink vision, giant block letters, POW), Aurora (curtains overhead, then strikes
that glaze the ground in coloured glass), Century (a year in forty seconds, then a hundred years of decay), Prism
(seven rainbow beams burning coloured trenches), Geode (a giant amethyst geode that sprays crystal shards), Fossil (a
colossal skeleton rises and roars) and Lava Lamp (heats, cracks and bursts into molten splats).

**FUN tier** — toys, harmless or temporary: DISCO BALL (a giant disco ball dazzles everyone, stops, then drops the
bass, starts Pigstep and turns the land into a dance floor for five minutes), plus the Disco, Glitter, Pumpkin
Patch, Cobweb, Balloon and Sprinkler bombs.
All of these need two lights to confirm, respect protected regions and `armageddonMaxRadius` (default 256), and
can be disabled with `armageddonAllowCelestial`. Players are protected during camera cutscenes.

**Signature aftermaths** — every Celestial, Celestial II, City Destruction and world cutscene bomb leaves its own
mark instead of a generic crater: a cow-print countryside, a fist imprint lined with crying obsidian, a moon that
lands and stays, a time-stopped hollow, a terracotta bullseye with one terrace per boom, and so on.
Every bomb, weapon and grenade carries a tier that matches how much it destroys: FUN (harmless toys such as the
Party Popper, Airhorn Cannon and Bounce Bomb), Uncommon, Rare, Epic, Legendary,
Mythic, Celestial, City Destruction and World Destruction.

**Testing events** — `/armageddon event <id>` starts any scripted event (every bomb, Celestial and
cutscene) at your feet, facing where you look, without crafting or lighting anything.

**Creative tabs** — Weapons, Explosives, Bombs, Cataclysms (Celestial, City and World Destruction) and Gear & Materials,
each grouped by kind and sorted by power.

**Showcase (screenshots)** — `/armageddon showcase [all|weapons|explosives|bombs|cataclysms|gear|tiers|<tier>]`
builds a lit gallery in front of you: every item floats on a tier-coloured pedestal with its name and tier, rows
step upward so one screenshot shows them all. `/armageddon showcase item <item>` shows a single item large with
its description (hero shots). `/armageddon showcase clear` removes it and restores the ground. Tip: hide the HUD
with F1, and use `/time set noon` and `/weather clear`.

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
