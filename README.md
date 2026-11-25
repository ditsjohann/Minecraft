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

**Categories** — every bomb and weapon is placed by what it actually does: how far its destruction reaches and how
deadly it is.

| Tier | Reach | Examples |
| --- | --- | --- |
| Fun | harmless or temporary | Disco Ball, Balloon, Party Popper, Chinese Lantern |
| Uncommon | under 5 blocks | Frag, Incendiary, Gravity Grenade, Flamethrower |
| Rare | 5 to 12 blocks, gadgets | Thermobaric, Thermite, Sinkhole, Pocket Nuke, Rocket Launcher, Plasma Rifle |
| Epic | 12 to 25 blocks | Cryo Bomb, Singularity, Tsunami, Volcano, Geode, KA-BOOM!, Lava Lamp, Nuke Launcher, Goo Bomb, Hyperbeam Raygun |
| Legendary | 25 to 45 blocks | Atomic, Antimatter, Neutron, Nova, Hellmouth, Doomsday Clock, Zeropoint, Chrono Break, Century, Helix Disruptor, Graviton Lance, Sunfall Mortar, Cactus Bomb, Firework Havoc, Pterosaur Fossil |
| Mythic | 45 to 90 blocks | Hydrogen, Omega, Bloodstar, Magnetar, Prism, Fossil, Last Light, Unmaker, Godfall, Big Bang, Colossal Spear, Zombie Apocalypse, Lidar, Shadow Void, Spectral Ice, Ion Scythe, 16 Chunks, Halo Cutter, Obliterator, Blackforge, Plasma Koi, Dimension Shift, Mammoth Fossil, Leviathan Fossil |
| Celestial | 90 to 150 blocks | Exploding Cows, Deadly Twister, UFO Invasion, Resonance Cascade, Tesla Dominion, Gravity Killswitch, Blue Infrit, Terror Maniac, Pumpkin Horseman, Portal of Hell |
| City Destruction | 150 to 300 blocks | Moonfall, World Eater, Ragnarok, Eternal Sapling, Main Stage, Termite Domain, Final Existence, AI Takeover |
| World Destruction | 300 blocks and up | Galactic Smash, Booming Tick, Alien Death Ray, The Sun Disappears, Dyson Lance, Seismic Tsunami, Xenoform |

Bombs of Mythic tier and up must be lit twice; Celestial and up are gated by `armageddonAllowCelestial` and live in
the Cataclysms tab.

**The wild ones** — GOO BOMB (alien goo swells, throbbing faster and faster, then bursts into acid globs; what they land
on turns to goo and dissolves), HYPERBEAM RAYGUN (a three-second charge, then a beam as thick as a house pours out for
six seconds, tunnelling nearly 100 blocks), CACTUS BOMB (a tower-sized saguaro spins faster and faster and flings its
spikes in every direction, each one blasting a hole, then blooms while a red sandstorm turns the land to desert),
BLACKFORGE (anvils rain from the sky, faster and faster, until a giant hammer slams the ground: every anvil turns to
iron and the land around breaks into shifting tectonic plates) and BLUE INFRIT (a summoning circle
burns into the ground in blue fire, and a hound's head of blue flame dives on the land six times, leaving pits of
lava, before blue fire sweeps the country).

Three horror celestials. TERROR MANIAC: the colour drains from the world, the sky turns to old blood with red lightning
in the clouds, and black veins crawl out pulsing with an underground heartbeat. A towering thing of roots and sinew with
a cracked white mask, weeping blood and drooling it through a stitched grin, rises from a geyser of blood under a blood
moon. It lifts everything living into the air, bleeding, twitching and blind, while a bell tolls and blood rains on the
land, and then closes its fists and bursts them in sprays of gore. It shrieks, bursts into a storm of black moths,
and the land rots to podzol, mud and wither roses. PUMPKIN HORSEMAN: a headless rider the height of a tower on a horse of
night gallops round the land leaving burning hoofprints, hurls his blazing jack-o'-lantern six times (each a crater
ringed with grinning pumpkins), then charges through the middle with his scythe and rides into the sky, leaving the land
hallowed with pumpkins, cobwebs and soul fire. PORTAL OF HELL: a gothic gate of blackstone the height of a cathedral heaves
out of the earth; its arch becomes a vortex of hellfire with a bloodshot eye that follows you, wave after wave of ghasts,
blazes, wither skeletons, magma cubes and zoglins come through, lava falls from its sill, and a huge clawed demon arm
reaches out, digs into the land, rips out a great chunk of it and drags it - and anyone on it - back through the portal
before it implodes, leaving the cracked gate over scorched land.

AI TAKEOVER (City Destruction): a black core erupts from the middle of the city and opens a red lens-eye, and static
crawls over every screen. Circuit traces race out across the streets behind a wall of red hologram, and everything they
reach is assimilated: glass blacked out, walls turned black, lights gone red, the ground a black circuit board. A swarm
of drones pours out of the core and hunts anything alive with lasers, missiles rain on the streets, three tripod war
machines rise out of the city and stride through it sweeping beams that blow burning trails through everything, red
lightning crackles between the rooftops, chunks of buildings corrupt into glitch blocks and a giant hologram of the eye
flickers over it all. Then the eye goes white, alarms sound, and a grid
of laser walls expands from the core, cutting every building straight down into blocks. The drones return, the core
uploads itself into the sky, and an EMP blast ends it.

PLASMA KOI (mythic): a pond of liquid light opens and seven koi made of plasma circle beneath its surface. One by one
they leap out on wilder and wilder arcs, each landing in a scorching splash; then they leap together, braid into a
dragon that climbs and circles over the pond, and dive back in. The pond erupts into a geyser and plasma rains down.

DIMENSION SHIFT (mythic): a rift tears open and a wave runs out from it, shifting everything it passes into the Nether:
nylium and netherrack, soul sand, crimson and warped trees, lava where the water was, a red sky and falling ash; then the
locals arrive. FIREWORK HAVOC (legendary): one giant firework climbs and bursts, setting off a cascade of real fireworks at
every angle while fountains go off across the ground, ending in a golden willow. CHINESE LANTERN (fun): hundreds of paper
lanterns rise and fill the sky while a translucent golden dragon flies over them to a flute tune, then bursts into gold.

Three more fossils, each crafted from a Fossil Bomb. MAMMOTH FOSSIL (mythic): a woolly mammoth's skeleton rises out of
a frozen fissure, trumpets, and an ice age rolls out over the land (frozen water, deep snow, spikes of ice, everyone
frozen) before it stomps the frozen ground to pieces; in the finale lines of giant ice spikes race out and a glacier
entombs the skeleton in ice. LEVIATHAN FOSSIL (mythic): a ghost of the ancient sea floods the
land, jellyfish and shoals drifting through it, while a sea serpent's spine breaches in great arches; its skull rears
up and the whole sea gathers into one great curling wave that rolls out and leaves a sea floor behind. PTEROSAUR FOSSIL (legendary): bones
burst out of the ground and assemble mid-air into a pterosaur thirty blocks across that flies, screeches, dive-bombs
the land three times, then lands on a spire of bone and turns to stone, its fossil left perched there.

The newest bombs (Goo, Cactus, Blackforge, Blue Infrit, Plasma Koi, Dimension Shift, Firework Havoc, Chinese Lantern and
the Mammoth, Leviathan and Pterosaur fossils, Xenoform, Terror Maniac, Pumpkin Horseman, Portal of Hell and AI Takeover) sit
in their own creative tab, "Armageddon: New", for now.

**Test city** — use a Test City Capsule on the ground, or run `/armageddon city [1-6]`, to build a city to destroy:
roads, street lights, parks and villagers, ordinary skyscrapers (taller toward the centre, and taller the bigger the
city), and a catalogue of oddly shaped buildings made to come down well: round glass towers, twisted towers, inverted
pyramids, helix towers, TV towers with a glass pod, and gold-trimmed art-deco setbacks. Some blocks of four lots become
landmarks: an oval stadium with floodlights, a ribbed glass dome, a Ferris wheel in a park, a glass pyramid, twin
towers joined by a skybridge, or a megatower. Size 6 is over three hundred blocks across. `/armageddon purge` clears fallout,
black holes, stars, pending strikes and crater jobs.

**Celestial II** — EXPLODING COWS (cows fall as bombs, wave after wave, bigger each time, then one enormous cow
that goes off and leaves a milky crater), DEADLY TWISTER (a mile-high twister that wanders and drags players from
safety), COLOSSAL SPEAR (a winged, building-sized spear falls for five seconds out of a heavenly sky and drills to
the core), ZOMBIE APOCALYPSE (a growing undead tide and wandering giants that explode green) and UFO INVASION (a
city-wide mothership with a blue beam of death, flanked by scout saucers leaving trails of explosions).

**CITY DESTRUCTION tier** — a city-sized area or more is remade: MOONFALL (a cratered moon breaks the sound
barrier and lands), WORLD EATER (a gigantic cosmic worm tunnels through the land), RAGNAROK (lightning, meteors,
quakes and volcanoes, then Surtr's burning sword splits the world into fire and frost), ETERNAL SAPLING (a sapling
climbs eight ever-bigger trees to a world tree whose roots slam down, then blooms and the forest takes the city back), 16 CHUNKS (Mythic: the
sixteen chunks around it each roll: most turn into one random block all the way through, the rest get floods, flips,
swaps, x-ray, the floor is lava, voids or mob parties) and MAIN STAGE
(studio rigging comes down, the land is struck into a studio floor with a real stage, and a painted-sky wall with an
EXIT door goes up around you). Also BIG BANG (Mythic: a new solar system, alone in a void) and CHRONO BREAK
(Legendary: a sphere of world is erased, then rewound).

**WORLD DESTRUCTION tier (the real one)** — the world enders, with camera cutscenes. GALACTIC SMASH: a faceless void
titan tears the sky open, hunts the city with burning eyes, winds up and punches the ground, and the ejecta rains
back down for fifteen seconds. BOOMING TICK: a clock in the sky drops one burning numeral every five seconds, each
boom bigger than the last, then a cutscene over the carpet-bombed site and out to Earth exploding. ALIEN DEATH RAY:
a cutscene to an alien world where a colossal ray cannon rises, charges and fires at Earth, then an EMP, a tsunami
and the ray itself coming down.

**The strong five** — seen through a new depth-aware vision pass that rebuilds the world from the depth buffer.
LIDAR STRIKE (Mythic): the world turns into a lidar point cloud, four scan pulses tag everything alive, then the
strikes walk the scanned map and the mast overloads. SHADOW VOID (Mythic): a volumetric sphere of darkness with
an event core swallows the ground shell by shell, inhales, and exhales a shockwave of shadow. SPECTRAL ICE
(Mythic): ice spikes erupt into a pyramid with one point, the apex lights blue, the spikes glow harder and harder,
and the white apex sprays a storm of ice shards that each leave a small crater and a fresh spike. TERMITE DOMAIN
(City Destruction): thousands of ants eat every living thing out to 90 blocks, then raise a seventy-block termite
hill with spires, vents and tunnel mouths. THE SUN DISAPPEARS (World Destruction, the strongest yet): the sun goes
out for the whole world: endless night, a starless sky, fog, frost on the lens, frozen water, dying plants, and
freezing for anyone not standing by a fire; then the sun's corpse falls, and the light comes back in the blast.

**Plasma weapons (Legendary and Mythic)**
HELIX DISRUPTOR (Legendary): charge and release, two plasma bolts corkscrew around each other and bore a spiral tunnel
140 blocks long, then implode. GRAVITON LANCE (Legendary): plants a gravity anchor that drags everything around
it inward and crushes it into a crying-obsidian core. SUNFALL MORTAR (Legendary): lobs a small plasma sun on a
high arc; where it lands the ground melts into a lava pool. HALO CUTTER (Mythic): charge to paint a target,
an orbital beam draws a thirty-block-deep ring canyon round it, then the disc inside drops out. OBLITERATOR (Mythic): hold to fire a beam that takes apart everything it touches and keeps going.

**Plasma and sci-fi** — none of these is an explosion. ION SCYTHE (Mythic): an orbital ion beam carves a spiral
trench of molten glass outward, charges it, then the whole spiral ignites as a curtain of light that disintegrates
the land. RESONANCE CASCADE (Celestial): a tuning fork hums until it finds the town's frequency, then every
building, wall and tree behind the spreading front shakes apart into heaps of rubble and the soil goes to mud. TESLA
DOMINION (Celestial): a tesla tower arcs at the tallest things around; the lightning chains through buildings
blowing them apart and ploughs smoking furrows where it earths, then the ball lightning discharges everywhere and
the tower topples. DYSON LANCE (World Destruction): an orbital mirror swarm focuses the sun into a beam that cuts
three 600-block canyons through the world into a six-pointed star, then drills the centre. GRAVITY KILLSWITCH (Celestial): gravity switches off and the town floats up and tumbles, then it comes back many times over and slams
everything down. SEISMIC TSUNAMI (World Destruction): a piston hammer slams the ground until the land becomes a wave
of rock rolling out three hundred blocks, scraping everything behind it down to bare stone.

XENOFORM (World Destruction): technology five thousand years past ours, and utterly indifferent. There is no explosion.
A point of nothing opens in the sky and unfolds into a hypercube turning through the fourth dimension, ringed with alien
glyphs. A plane of ruled light passes down through the land and back up, marking everything alive. Then the land is
taken apart, block by block from the centre out: every block rises in a straight line and is sorted by colour into
spectrum rings round the hypercube, a catalogue of the place, and everything living goes up with it and is
disassembled. What is left is a perfectly flat black plain ruled with lines of light, cut clean at its rim. The rings
fold into the hypercube, the hypercube folds into a 1:4:9 monolith that sets itself down in the middle with eight
needles round it. All the while eight Watchers - tall faceless things with a slit of light for a face - stand round the
edge of the harvest facing you, glitching a step closer now and then, and an eye opens inside the hypercube. At the end
light races in along the grid to the monolith, a ring-shaped ship hundreds of blocks across comes down out of the sky,
the monolith's beam locks on to it, the Watchers dissolve up into it, and it leaves in a streak of light that throws a
shockwave across the sky. No cutscene: you watch it all yourself.

**Oddities (Legendary and Mythic)** — Sonar (green echo vision; every echo heaves the ground, then the converged
boom shakes the land around it to pieces), 8-Bit (the world goes low-res on screen and in the blocks, then GAME OVER),
Magnetar (the land torn up into orbit and ground to dust, pulsar jets cutting trenches, then the star collapses into
a hole), KA-BOOM! (comic ink vision, giant block letters, POW), Aurora (curtains overhead, then rays that take the
land apart into coloured light), Century (a year in forty seconds, then a hundred years of decay), Prism (seven
rainbow beams burning coloured trenches), Geode (the ground turns to amethyst crystal and shatters into a hollow),
Fossil (a colossal skeleton rises and its roar tears a cone out of the land) and Lava Lamp (heats, cracks and bursts
into molten splats).

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
docs/vfx/                          visual-effects research and roadmap (start at docs/vfx/README.md)
```

## Notes

- Sounds reuse vanilla audio files under new sound events (`assets/armageddon/sounds.json`), so you can drop
  in your own `.ogg` files without touching code.
- The post-processing shader degrades gracefully: if it fails to load, flashes fall back to a HUD overlay.
