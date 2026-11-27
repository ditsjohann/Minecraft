# Handoff: continuing work on Armageddon Arsenal

Everything someone (a person or a new Claude session on another account) needs to pick up this mod where it was
left. Read this first, then [CONTENT.md](CONTENT.md) for the full list of items, and the main [README](../README.md)
for what each bomb does in play.

## 1. Where the code is

- Repository: `ditsjohann/Minecraft` on GitHub.
- **The newest work is on the branch `claude/cool-ramanujan-da0d9h`.** The repository's default branch
  (`claude/gallant-bohr-nzzps0`) is older: it is fully contained in `claude/cool-ramanujan-da0d9h`, which adds
  97 commits on top (the particle engine visuals for every tier, the fossils, Xenoform, the horror celestials, AI Takeover, the city rework, the
  redos). Start from `claude/cool-ramanujan-da0d9h`, or first merge it into the default branch (a fast-forward,
  no conflicts) so new sessions start from the latest code.
- Last commit at handoff: `Prism bomb: redone as a refraction storm` plus this documentation commit. CI is green.

```sh
git clone https://github.com/ditsjohann/Minecraft.git
cd Minecraft
git checkout claude/cool-ramanujan-da0d9h
```

## 2. Versions and building

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1, Loom 1.11-SNAPSHOT |
| Mappings | Yarn 1.21.1+build.3 |
| Java | JDK 21 |
| Python (assets) | Python 3 with `pillow` and `numpy` |

```sh
./gradlew build                 # compile + jar in build/libs/
./gradlew runClient             # dev client
python3 tools/generate_assets.py   # regenerate every texture, model, lang string, recipe, sound, shader
python3 tools/list_content.py      # regenerate docs/CONTENT.md
```

CI: `.github/workflows/build.yml` builds every push and attaches the jar. Maven Central sometimes answers
`429 Too Many Requests`; the workflow retries that by itself, and if it still fails just re-run the job (it is
never a code problem).

## 3. Layout

```
src/main/java/com/armageddon/
  registry/      ModItems, ModBlocks (every item/block + its Tier), Catalog (creative tabs, the "New" tab set),
                 ModCommands (/armageddon ...), ModGameRules, ModSounds, ModParticles, ...
  block/         BombBlock, BombType (enum: blast radius + colour for every bomb)
  event/         WorldEvent, EventEntity, Events (id -> event factory map), Kit, Ruin, Climax
  event/events/  one file per family of scripted bombs (FossilEvents, XenoEvents, HorrorEvents, AiEvents,
                 WonderEvents, OddBombEvents, SciFiEvents, CelestialEvents, WorldCutsceneEvents, ...)
  util/          *Shapes classes: timings and geometry shared by the server event and the client effect
                 (FossilShapes, FossilPoses, XenoShapes, HorrorShapes, AiShapes, WonderShapes, WildShapes, NewBombShapes ...)
  detonation/    crater/slice/shell jobs, DetonationManager
  destruction/   Destruction (safe block setting, protected regions), celestial/city/world aftermaths
  network/       Vfx (server -> client effect calls), FxPayload, FxType, VfxPayload
  vfx/VfxIds     ids of every particle-engine effect
  world/         CityBuildJob (test city), ModWorldGen
  showcase/      /armageddon showcase gallery
src/client/java/com/armageddon/client/
  fx/            code-driven effects: one class per bomb or family (FossilFx, XenoFx, HorrorFx, AiFx, RocketFx,
                 ShiftFx, WildFx, NewFx, OddFx, SciFx, Cinematics, PostFx, Vision ...)
  vfx/           the data-driven particle engine (FxEngine, VfxLibrary, EffectDef, EmitterDef ...) and the effect
                 definitions (FossilVfx, XenoVfx, HorrorVfx, AiVfx, RedoVfx, WildVfx, ...), registered in ThrowVfx
  render/        FxRenderLayers (GLOW additive, SHADE alpha, SOLID opaque), FxDraw, Solids
  mixin/, hud/, particle/
src/preview/java/  offline preview harness (renders effects to JPGs without the game)
tools/             asset generator (Python) and helper scripts
docs/vfx/          visual-effects research and roadmap
```

## 4. How a bomb works, end to end

1. **Block**: `ModBlocks` registers `tiered(bomb("id", BombType.ID, Rarity.X), Tier.Y)`. `BombType` holds the radius
   and colour. Mythic and up must be lit twice; Celestial and up are gated by the `armageddonAllowCelestial` game rule
   and capped by `armageddonMaxRadius`.
2. **Event**: `Events.EVENTS.put("id", SomeEvents.Thing::new)`. The event is a `WorldEvent` whose `tick()` runs every
   server tick and returns false when done. Most new ones extend `SciFiEvents.Base` (terrain jobs, `put`, `yaw`) or
   `OddBombEvents.Base` (a `BlockQueue` via `q()`, `drain`, `name`, `surface`).
   - Terrain is changed over many ticks with budgets: `TerrainJob.disc(world, centre, r, painter)` plus `.step(budget)`,
     `TerrainJob.Column` helpers, `Ruin.Front`, `DetonationManager.add`, and `Kit.lift / fissure / ringCrack`.
   - Announcements go through `CelestialEvents.announce(world, pos, "block.armageddon.id")`.
3. **Visuals**: the server calls `Vfx.newFx(world, kind, a, b, scale, color, p1, p2, ticks)` once. The client routes
   `NEW_FX` by kind in `NewFx` then `WildFx.create`:
   - below 40: `OddFx`; 40-49: `SciFx`; 50 and up: `WildFx`, where
   - 55-57 FossilFx (mammoth, leviathan, pterosaur), 58 XenoFx, 59-61 HorrorFx (maniac, horseman, hell gate),
     62 AiFx, 63 RocketFx (Firework Havoc), 64 ShiftFx (Dimension Shift), 65-67 FossilFx2 (amber, sabre-tooth,
     ammonite), 68-69 PlasmaBombFx (plasmoid swarm, magnetic bottle). **The next free kind is 70.**
   - Smaller bursts use the particle engine: `Vfx` calls by id from `VfxIds`.
   - Screen effects: `Vfx.screen` (0 desaturate, 1 whiteout, 2 blackout, 3 static, 4 invert), `Vfx.beam`,
     `Vfx.flash`, `Vfx.skyTint`.
4. **Shared timing**: the server event and the client effect both read the same `util/*Shapes` class, so they never
   drift apart. Put every tick number and every path function there.
5. **Assets**: add the name, description and recipe to `tools/arsenal_data.py` (`NEW_BOMBS`, `BLOCK_DESC`, `shaped(...)`),
   the model to `tools/models_arsenal.py` (a function plus `BOMBS.update({...})`), any new texture material to
   `tools/tex_arsenal.py`, then run `python3 tools/generate_assets.py`.
6. **Tab and docs**: add the `BombType` to `Catalog.NEWEST` if it should show in the "New" tab, write a paragraph in
   `README.md`, and re-run `python3 tools/list_content.py`.

### Rules that bite

- **Every id in `VfxIds.ALL` must have a definition** in one of the client vfx classes registered in
  `ThrowVfx.register()`, or the `VfxLibrary` bootstrap throws when the game starts. Removing a definition means removing
  the id too. Ids must be unique (one clash already happened: the Hell Gate ids are prefixed `HELLGATE_`).
- Layered glow geometry needs real spacing between layers or it z-fights (it happened on the AI eye lens).
- Keep model element counts sane (the Portal of Hell model was merged down to about 100 elements).
- Use `smooth()`/`hash()` from the matching `*Shapes` class; make anything that must be public for the client public.
- Never block the server tick: change terrain through budgeted jobs, never in one go.

## 5. Testing

In game:
- `/armageddon event <id>` starts any scripted event at your feet facing where you look (ids are in CONTENT.md).
- `/armageddon city [1-6]` builds a test city (size 6 is over 300 blocks across), or use a Test City Capsule.
- `/armageddon vfx <id> [scale]` plays one particle-engine effect.
- `/armageddon purge` clears fallout, black holes, stars, strikes, world events and pending crater jobs.
- `/armageddon restore [count]` undoes recorded destruction (turn on the `armageddonRecordRestore` game rule first).
- `/armageddon protect add <name> <from> <to> | remove <name> | list` manages regions no weapon can damage.
- All `/armageddon` commands need operator permission (level 2).
- `/armageddon showcase [...]` builds a screenshot gallery.

Offline, without the game (what most of the visual work was tuned with):
- `preview.txt` holds one frame per line: `scene tick [camdx camdy camdz [tx ty tz]]`. Scenes are the `case`s in
  `src/preview/java/com/armageddon/client/fx/PreviewMain.java` (e.g. `xeno`, `maniac`, `horseman`, `hellgate`, `ai`,
  `mammoth`, `leviathan`, `ptero`, `rocket`, `shift`, or `vfx:<ID>` for an engine effect).
- `./gradlew fxPreview -q` renders every line to `build/fx-previews/NN_scene.jpg`.
- `python3 tools/preview_sheet.py out.jpg 0.5 4` tiles them into one contact sheet.
- `./gradlew boneDump -PdumpDir=<dir>` dumps the fossil skeletons; `./gradlew cityDump "-PdumpArgs=<out> <size> <seed>"`
  dumps a test city's blocks to a text file.
- The preview renders against a plain daytime sky with no world blocks, so it shows effects only, not the terrain
  changes.

## 6. State at handoff

Done and pushed (all builds, CI green):
- Fossils: Mammoth (ice age, stomps, ice-spike finale, glacier tomb), Leviathan (ghost sea, tidal wave, sea floor),
  Pterosaur (dives, then turns to stone on its spire and stays).
- Xenoform (world destruction): no cutscene; hypercube, scan, block-by-block harvest into colour rings, monolith, eight
  Watchers, alien ship arrival and departure.
- Horror celestials: Terror Maniac (blood and gore), Pumpkin Horseman (horse and rider drawn from particles, ride
  starts in view), Portal of Hell (horde waves, new eye, muscled demon arm that rips a chunk of land into the portal).
- AI Takeover (city destruction): core eye, assimilation, drones, missiles, tripods, glitch corruption, purge grid,
  upload, EMP.
- Test city sizes 1-6 with cylinder, twisted, inverted, helix, spire and setback towers, and stadium, dome, Ferris
  wheel, pyramid, twin towers and megatower landmarks.
- Redos: Firework Havoc (a real rocket that climbs and bursts), Plasma Koi (some koi swim through the air),
  Dimension Shift (purple portal outline), Prism (refraction storm with spotlights and crystal shards), and earlier
  Sixteen Chunks, Zombie Apocalypse, Spectral Ice, Aurora, Big Bang.

- Three more fossils: Amber (dragonfly sealed in amber, freed with a brood, then sealed again; an amber tide floods the
  land), Sabre-Tooth (a hunt at dusk: claw rakes, a cat skeleton with ghost ember flesh, a colossal ghost skull that
  bites two trenches, then it petrifies) and Ammonite (a coiled shell
  that grows chamber by chamber and presses the land into its imprint). Events in `FossilEvents2`, effects in
  `FossilFx2`/`FossilVfx2`, timings in `FossilShapes.Amber/Sabre/Ammonite`, skeleton poses in `util/FossilPoses`.
  New `BombType`s are appended at the end of the enum on purpose: primed bombs save their type by position.
- Five plasma bombs: Plasmoid Swarm, Solar Arch, Fusion Geyser, Z-Pinch, Magnetic Bottle. Events in `PlasmaEvents`,
  timings in `util/PlasmaShapes`, engine effects in `PlasmaBombVfx` (the weapons' `PlasmaVfx` is a different file),
  client parts in `PlasmaBombFx`. They added three engine modules: `Module.arch` (flux rope along the effect's
  direction), `Module.dipole` (particles stream along a magnetic dipole's field lines) and `Module.pinch` (a column
  crushed into beads).
- Terrain painters should find the ground with `FossilEvents2.groundY(column)`: a column's `surface()` counts tree
  trunks as ground.

Not done or not verified:
- **None of the latest batch has been played in the real game yet**, only built and previewed offline. Expect tuning
  (timings, sizes, brightness, sounds, performance on a big city) after the first in-game test.
- The "New" creative tab is meant to be folded back into the normal tabs at some point (empty `Catalog.NEWEST`).
- Long-term visual ideas are in `docs/vfx/README.md` (mip-chain bloom, soft particles, volumetric clouds, point
  lights).

## 7. Conventions used so far

- Commit one feature or one fix per commit, with a short plain title ("Portal of Hell: horde waves, a new eye ...").
- Player-facing text is plain English with no hype; bomb descriptions say what happens, in order.
- New content follows the tier table in the README: tier by how far the destruction reaches.
