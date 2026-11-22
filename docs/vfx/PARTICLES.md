# A new particle and effect engine

This is a design for replacing the current particle setup with a proper **emitter-based VFX engine**, the kind that
Unreal's Niagara or Unity's VFX Graph use, sized for a Minecraft mod. It covers:
- the architecture;
- the modules;
- rendering modes;
- networking;
- budgets;
- an optional GPU path for very large particle counts.

Companion docs:
- [EFFECT_RECIPES.md](EFFECT_RECIPES.md): a cookbook of layered effects built with this engine.
- [TEXTURES.md](TEXTURES.md): how to author the textures the engine needs.

## Status (what's built)

Steps 1–2 and parts of 4 and 6 from the [migration plan](#7-migration-plan) are in `src/client/java/com/armageddon/client/vfx/`:

| Built | Where |
|---|---|
| Structure-of-arrays pool, baked `Curve` / `Gradient`, seeded `Rng` | `ParticlePool`, `Curve`, `Gradient`, `Rng` |
| Shapes: point, sphere, shell, dome, ring, disc, column, box | `Shape` |
| Starting velocities: radial, up-cone, along-aim cone, random, swirl (they stack) | `Velocity` |
| Modules: gravity, drag, buoyancy, curl noise, vortex, attract, wind, jitter; block collision | `Module`, `EmitterInstance` |
| Sub-emitters: on death, on hit, every N ticks (trails), each able to start several layers | `EmitterDef.onDeath/onHit/every` |
| Render modes: billboard, upright, stretched, flat; glow / glow-on-top / lit smoke layers through the FX atlas; smoke sorted back to front | `VfxRender` |
| Cues: flash, shake, light, sound, chain, with "sonic" cues delayed by the speed of sound | `EffectDef` |
| Budget (thins detail, then normal, never hero) and distance LOD | `FxEngine`, `EffectInstance` |
| One packet per effect (`VfxPayload`), `Vfx.play(...)` on the server | `network/` |
| 14 effects, listed in `VfxIds` | `VfxLibrary` |

**Try it:** `/armageddon vfx <effect> [scale]` plays an effect on the block you're looking at, or in the air ahead.

**Preview:** `vfx:<effect>[:<scale>]` scenes in `preview.txt` render through `./gradlew fxPreview`. They have no world, so no
collision or ground snapping.

**Not built yet:** JSON files and hot reload, ribbons, mesh particles, the light render mode, premultiplied blending,
flipbooks, soft particles, quality presets, porting the existing weapons and bombs onto the engine, and GPU particles.

---

## 0. What we have today, and why it runs out

| Piece | Today | Limit |
|---|---|---|
| `WmdParticle` | One billboard class with a `Preset`: a three-stop colour ramp, a linear size curve, a life range, gravity and drag, collision, an emissive window, fade in/out, sprite animation, spin, flicker | Only linear curves. No forces besides gravity and drag. No sub-emitters. No stretched or velocity-aligned billboards. No soft edges. No per-particle lighting beyond vanilla light or full bright |
| `ModParticleFactories` | 12 presets (`NUKE_FIRE`, `NUKE_SMOKE`, `SHOCK_DUST`, `PLASMA_SPARK`, `ARC_SPARK`, `RADIATION`, `CRYO_MIST`, `VOID_SPARK`, `NAPALM_FLAME`, `EMBER`, `TOXIC_GAS`, `MUZZLE_FLASH`) | Every new look means new Java and a recompile |
| Spawning | About 94 `ServerWorld.spawnParticles` calls on the server | Every burst goes over the network. Shapes are just random boxes (`dx, dy, dz`). Effects can't stay in sync with each other or adapt to the client's settings |
| Sheet | `PARTICLE_SHEET_TRANSLUCENT` | Alpha only, with no additive blending: fire and sparks can't truly glow, and they sort badly against smoke |
| `FxEffect` and `Vfx` packets | Code-driven effects (orbs, rings, beams, domes and so on) | Powerful, but each one is written by hand. Particles and geometry don't share a timeline |

**The goal:** one engine where an effect is a **data file** listing emitters and their modules. The server sends
one small packet (effect ID, position, seed and parameters), and each client simulates and renders it locally at its
own quality level.

---

## 1. Architecture

```
EffectDef (JSON, hot-reloadable)                   ← assets/armageddon/vfx/<name>.json
 ├── params: scale, color, intensity, direction, ...  (overridable per spawn)
 ├── timeline: [ {at: 0, emitter: "flash"}, {at: 2, emitter: "fireball"}, {at: 10, emitter: "smoke"} ]
 └── emitters
      ├── EmitterDef
      │    ├── spawn:   burst / rate / per-distance, count curves, LOD scaling
      │    ├── shape:   point / sphere / shell / disc / ring / cone / box / line / mesh-surface / blocks
      │    ├── init:    lifetime, size, rotation, velocity, color, sprite frame (all random ranges)
      │    ├── update:  forces, drag, curves over life, collision, noise, attractors, orbit, vortex
      │    ├── events:  on death / on collision / every N ticks → spawn sub-emitter
      │    └── render:  billboard / stretched / velocity-aligned / horizontal / ribbon / mesh / light
      └── ...
EffectInstance (client)    ← one per spawned effect: age, transform, params, RNG(seed)
 └── EmitterInstance       ← spawn accumulator, its own particle pool
      └── ParticlePool     ← structure-of-arrays storage, no per-particle objects
FxEngine (client singleton)
 ├── tick(): advance instances, apply global fields (wind, shockwave pressure), budget
 └── render(): sort and batch by render layer → FxBuffers
```

Everything renders through the existing **`FxBuffers`** and **`FxRenderLayers`**, so bloom, the shader-pack adapter
and the post passes keep working.

### 1.1 Structure-of-arrays pool (why we don't use `Particle` objects)

Vanilla gives every particle its own Java object with a virtual `tick()`. That's fine at a few thousand, but GC
pauses and pointer chasing hurt at 20,000 or more. A pool of flat arrays is cache-friendly and creates no garbage
while running:

```java
final class ParticlePool {
	final int cap;
	int count;
	final float[] x, y, z, px, py, pz, vx, vy, vz;   // position, previous position (for interpolation and stretch), velocity
	final float[] age, life, size, rot, rotV, seed;   // seed: a 0..1 random per particle, used for per-particle variation
	final int[] frame;                                // flipbook start frame, or a frame offset

	ParticlePool(int cap) { this.cap = cap; x = new float[cap]; /* ... */ }

	int spawn() { return count < cap ? count++ : -1; }

	void kill(int i) {                                // swap with the last one: O(1), no gaps
		int j = --count;
		x[i] = x[j]; y[i] = y[j]; /* ...copy every array... */
	}
}
```

- Positions are stored **relative to the effect origin**, as floats, to avoid float precision jitter far from (0,0).
  Add the origin back when rendering, as doubles minus the camera position.
- Keep `px/py/pz` and interpolate with `tickDelta` in `render()`. Without this, motion stutters at high frame rates.

### 1.2 Curves, gradients and random ranges

The main step up from `Preset` is **curves**. Every value can change over a particle's life (or the emitter's), in
any shape, not just linearly:

```java
/** A piecewise-linear curve on 0..1, baked to a 64-entry table on load for O(1) lookup. */
record Curve(float[] table) {
	float at(float t) { float f = Math.min(t, 0.9999f) * 63f; int i = (int) f; return table[i] + (table[i + 1] - table[i]) * (f - i); }
	static Curve of(float... keys) { /* keys: t0,v0, t1,v1, ... ; bake to 64 samples */ }
}

/** An RGBA gradient with any number of stops, baked to 64 entries. */
record Gradient(int[] table) { int at(float t) { /* ... */ } }

/** A random range: "between a and b", or "between curve A and curve B". */
record Range(float min, float max) { float pick(float r) { return min + (max - min) * r; } }
```

A curve accepts these shorthand shapes in JSON: `"pop"`, `"ease_out"`, `"ease_in"`, `"bell"`, `"linear"`, or a list
of explicit keys. **`size: "pop"`** (0 → 1.2 in 10% of the life → 1.0 → 0.8) is the single most effective upgrade
for explosion particles.

### 1.3 The module list

These are the modules that give the most look per line of code. Each one is a small class with
`apply(pool, from, to, dt, ctx)`, and they run in the order they're listed (as in Niagara's stack).

**Spawn modules**

| Module | What it does | Use |
|---|---|---|
| `Burst(count, at)` | N particles at a given emitter time | Flashes, debris |
| `Rate(perSecond curve)` | Steady stream, rate can change over time | Smoke columns, fire |
| `PerDistance(perBlock)` | Spawns as the emitter moves | Trails behind projectiles, rockets, falling meteors |
| `Loop(period, count)` | Repeated pulses | Sonar pings, the Bass Drop beat |

**Shape modules (initial position)**

| Shape | Notes |
|---|---|
| `sphere(r, surfaceOnly)` | Uniform in volume (`r * cbrt(u)`) or on the surface |
| `hemisphere`, `cone(angle)` | Ground-level blasts, muzzle flash cones |
| `ring(r, thickness)`, `disc(r)` | Shockwave dust, ground rings, halos |
| `line(a, b)` | Beams and lightning: particles along the path |
| `box`, `cylinder` | Areas |
| `blocks` | Spawn on the **exposed faces of real blocks** in a radius. Debris and dust *come from* the terrain. Sample the face colour using `MapColor` or the block sprite average |
| `entitySurface` | Particles on the entity's bounding box. Burning, freezing and dissolving mobs |
| `meshSurface(model)` | Spawn from points on our custom FX models (UFO hull, Tesla tower, the Galactic Smash fist) |

**Initial modules**

- Velocity:
  - `radial(speed)`: outward from the centre;
  - `directional(dir, spread)`;
  - `inheritVelocity(fraction)`: from the moving emitter;
  - `tangential(speed)`: swirls.
- Lifetime, size, rotation and colour: ranges or gradients.
- `seedColor`: pick from a palette. The confetti and the Disco Ball.

**Update modules (forces)**

| Module | Maths | Use |
|---|---|---|
| `Gravity(g)` | `v.y -= g·dt` | Everything heavy |
| `Drag(k)` | `v *= exp(-k·dt)` (frame-rate independent, unlike `v *= 0.96`) | Everything |
| `Buoyancy(temperature curve)` | Upward force that weakens as the particle cools | **Fire that rises and slows, then smoke that spreads.** This is the realism key |
| `CurlNoise(freq, strength, scroll)` | Divergence-free swirl (Bridson et al. 2007); particles curl without bunching | Smoke, magic, spores, void wisps |
| `Vortex(axis, strength, radius)` | Tangential force around an axis, plus an optional pull inward | Twister, black holes, the Tesla orb |
| `Attractor(point, strength, killRadius)` | Pull towards a point and kill on arrival | Charging weapons, implosions, the Singularity |
| `Orbit(axis, speed)` | Rotate positions around the emitter | Rings of debris, accretion disks |
| `Wind(global)` | A global wind field with gusts (from `FxEngine`) | All smoke drifts the same way: consistency sells |
| `Pressure(shockwave)` | When an active shockwave front passes a particle, push it outward | **Other effects react to explosions.** Smoke from earlier bombs is blown away |
| `CollideWorld(bounce, friction, killOnHit)` | A cheap block check at the new position (`world.getBlockState(..).isSolidBlock`) | Sparks bounce, embers settle, debris lands |
| `CollideGround(height)` | Clamp to a fixed Y | Very cheap, for big dust waves |
| `SpeedLimit`, `Turbulence(rand)` | Clamp speed; add random jitter | Electricity, glitch |

**Over-life modules (curves)**

- `sizeOverLife(curve)` and `sizeBySpeed(curve)`: stretch when fast.
- `colorOverLife(gradient)`.
- `alphaOverLife(curve)`.
- `rotationOverLife(curve)`.
- `frameOverLife(mode)`: the flipbook frame by age, by speed, or random and fixed.
- `emissiveOverLife(curve)`: replaces `emissiveUntil`. Full bright fading smoothly into world light, instead of a
  hard switch.
- `erosionOverLife(curve)`: shader-side alpha erosion (see [TEXTURES.md](TEXTURES.md)). Smoke breaks up as it fades
  instead of just turning transparent.

**Event modules (sub-emitters)**

- `onDeath(emitter, inherit)`, `onCollision(emitter)`, `every(nTicks, emitter)`.
- Examples:
  - a firework rocket dies and spawns a burst;
  - a lava bomb hits the ground and spawns a splash, a scorch decal and a light;
  - a debris chunk spawns a smoke trail every 2 ticks while it flies.
- **This is what makes effects feel connected and alive**: things cause other things.

### 1.4 Render modes

All of these write quads into `FxBuffers` layers.

| Mode | How | Use |
|---|---|---|
| `billboard` | A camera-facing quad (we already compute camera yaw and pitch in `FxEffect.render`) | Smoke, glows |
| `billboard_axis_y` | Faces the camera but stays upright | Fire tongues, light pillars, tornado debris |
| `stretched` | Long axis along the screen-space velocity, length `= size + speed·stretch` | **Sparks, rain, shrapnel, speed streaks.** It's the cheapest way to make motion read |
| `velocity_aligned` | Stretched in world space along the velocity (not the screen) | Meteors, falling embers |
| `horizontal` | Flat on the XZ plane | Shockwave rings, ground dust, water ripples, decals |
| `ribbon` | A strip linking the particles of one emitter in spawn order, UV by age | Trails, lightning, aurora, sword swings |
| `mesh` | A small model per particle (a block model, or one of our `Solids`) | Debris, shards, crystals, shell casings |
| `light` | Doesn't draw anything; adds a point light to the light pass ([TECHNIQUES.md §7](TECHNIQUES.md#7-explosions-that-light-the-world)) | Fire lights, spark flickers, muzzle flashes |

**Blend modes:** every render module picks a layer.
- **Additive** (`GLOW`) for fire cores, sparks, plasma and electricity. They glow, need no sorting, and feed the
  selective bloom.
- **Alpha** (`SHADE`) for smoke and dust. Sort back to front within the emitter.
- **Premultiplied alpha:** one layer that does both. In the texture, colour × alpha is the smoke and colour with
  alpha 0 is glow. A single fire sprite can then be glowing at its core and smoky at its edge. This is what every
  modern engine uses for fire. It needs a blend func of `ONE, ONE_MINUS_SRC_ALPHA` as a new `RenderPhase.Transparency`.

### 1.5 Lighting particles properly

- **Sample the world light** at each particle: the vanilla `WorldRenderer.getLightmapCoordinates` at its block, once
  per particle every few ticks, cached. Smoke in a dark cave looks dark, and smoke in sunlight looks bright.
- **Light from the effect itself:** the emitter can declare a `selfLight` colour and position, for example the
  fireball centre. Tint each smoke particle by its distance to that light: `col += selfLight * 1/(1 + d²)`. This is
  the cheapest way to get "the smoke is lit orange from below".
- **Six-way lit smoke (advanced):** bake six light directions into the flipbook (front, back, left, right, top,
  bottom) across two RGB textures. At runtime, blend them by the direction to the sun and to the fireball. It's how
  current AAA smoke gets real volume from flat cards. See [TEXTURES.md §6](TEXTURES.md#6-lit-smoke-normal-and-six-way-maps).
- **Soft particles:** fade where a particle meets geometry using the depth copy. See
  [TECHNIQUES.md §5](TECHNIQUES.md#5-soft-particles-and-better-flipbooks).

### 1.6 Sorting and overdraw

- **Additive layers need no sorting.**
- **Alpha layers:** sort *emitters* by camera distance (the cheapest win). Sort *particles* within an emitter only
  when it has fewer than 2,000. Use a radix sort on a quantised depth stored in an `int[]`, so nothing is allocated.
- **Overdraw** is the real enemy, not the particle count. Fifty screen-filling smoke cards cost more than 10,000 tiny
  sparks.
  - Clamp the on-screen size: when a particle is closer than 2 blocks to the camera, fade it out.
  - Use fewer, larger, better-textured smoke particles in the distance, and more, smaller ones up close.

---

## 2. Networking: send effects, not particles

```java
// common
public record VfxSpawnPayload(Identifier effect, Vec3d pos, Vector3f dir, float scale, int color, long seed, int flags) implements CustomPayload {}

// server: one call replaces dozens of spawnParticles
Vfx.play(world, "armageddon:nuke_detonation", pos, VfxArgs.scale(4).color(0xFF8A1E));
```

- **Seeded RNG:** every client runs the same effect from the same seed, so two players see the same mushroom shape.
  Use a cheap PCG or SplitMix RNG per instance. Never use `Math.random()`.
- **Distance tiers:** the server sends one packet. The client picks its LOD by distance and its preset. A far-away
  client gets a "silhouette" version: fewer, bigger particles and no sparks.
- **Late join or re-sync:** for long effects (over 10 s), the server keeps a list of active effects with their start
  tick. Players who load the chunk later get `(effect, seed, startTick)` and fast-forward, either by simulating
  silently or by jumping to the pre-baked state at that time.
- **Bandwidth:** about 40 bytes per effect, against vanilla's roughly 30 bytes × number of spawn calls. A nuke today
  sends dozens of `spawnParticles`; afterwards it sends one packet.
- Migrate gradually. Keep `spawnParticles` for small vanilla-style effects, and move the big ones first.

---

## 3. Budget and LOD

A global **`FxBudget`**, ticked by `FxEngine`:

```java
final class FxBudget {
	static int maxParticles() { return switch (Quality.current()) { case LOW -> 4000; case MEDIUM -> 12000; case HIGH -> 30000; case ULTRA -> 60000; }; }
	/** 0..1 scale on spawn counts so the total stays under budget. Hero effects keep more. */
	static float spawnScale(int priority, double distSq) { ... }
}
```

- **Priority:** each emitter gets `hero` / `normal` / `detail`. When over budget, cut `detail` first (sparks, tiny
  dust), then `normal`. `hero` (the fireball, the main smoke column) is never cut, only made coarser: fewer, larger
  particles with the same total coverage.
- **Distance LOD:** `count *= clamp(1 - (d - near) / (far - near), minFraction, 1)` and `size *= 1/sqrt(countScale)`.
  Keep the coverage constant as the count drops.
- **Frustum culling:** skip *rendering* of emitters whose bounding sphere is off screen, but keep *simulating* them
  (cheaply), so they look right when you turn around.
- **Sleep:** an effect fully behind the camera and over 200 blocks away ticks at 1/4 rate.
- **Ageing out:** when the pool is full, steal the oldest `detail` particle instead of refusing to spawn new ones.

**Vanilla's limit:** vanilla's `ParticleManager` caps particles per texture sheet (bug reports mention 16,384, and
mods that "raise the particle limit" disagree about the exact number). Our own pool sits **outside**
`ParticleManager`, so it has no such cap and doesn't compete with vanilla rain or blocks.

---

## 4. GPU particles (optional, for 100k–1M)

For ash storms, ember rain over a city, the Termite Domain swarm, or a starfield:
- **Minecraft 1.21.1 uses an OpenGL 3.2 core context.** It's the macOS limit, so there are **no compute shaders**.
  **Transform feedback** (GL 3.0) is available everywhere and is the classic method.
- **Design:** two ping-pong VBOs holding `{pos, vel, age, seed}`.
  1. An *update* vertex shader reads one buffer and writes the other through transform feedback, with rasterising
     turned off (`GL_RASTERIZER_DISCARD`). The shader holds gravity, curl noise from a 3D noise texture, and age.
  2. Dead particles respawn in the same shader from a seed: no CPU readback.
  3. *Render:* draw instanced quads (`glDrawArraysInstanced`, GL 3.1) reading the buffer as per-instance attributes,
     or as a texture buffer.
- **Limits:** no world collision (only a height-field or the depth buffer), no Java logic, and Iris won't light them.
  Use this path only for **pure eye candy** with additive or simple alpha, and always provide a CPU fallback with 5%
  of the count.
- **Reference implementations:** ogldev tutorial 28 for the setup, the UCSD ParticlesTransformFeedback example for
  the ping-pong VAOs, and NVIDIA's feedback particles sample for splitting the emit and update passes.
- **Raw GL inside MC:** wrap it in `RenderSystem.assertOnRenderThread()`. Save and restore the bound VAO, VBO and
  program. Don't touch `BufferRenderer` state. Keep everything in one class, `GpuParticles`.

---

## 5. Effect definition format (JSON)

The format is hot-reloadable as a resource reload listener (`SimpleSynchronousResourceReloadListener`). Press F3+T
and you see your change immediately:

```json
{
  "params": { "scale": 1.0, "color": "#FF8A1E" },
  "timeline": [
    { "at": 0,  "emitter": "flash" },
    { "at": 0,  "emitter": "sparks" },
    { "at": 2,  "emitter": "fireball" },
    { "at": 8,  "emitter": "smoke" }
  ],
  "emitters": {
    "sparks": {
      "priority": "detail",
      "spawn":  { "burst": [[0, "120*scale"]] },
      "shape":  { "sphere": { "radius": "0.5*scale" } },
      "init":   { "life": [12, 30], "size": [0.05, 0.12], "velocity": { "radial": [0.6, 1.8] } },
      "update": [ { "gravity": 0.04 }, { "drag": 1.5 }, { "collide_world": { "bounce": 0.4, "kill_after": 2 } } ],
      "over_life": { "color": ["#FFFFFF", "#FFD080@0.2", "#FF5010@0.7", "#40100000"], "size": "ease_out" },
      "events": { "on_collision": { "emitter": "spark_puff", "chance": 0.15 } },
      "render": { "mode": "stretched", "stretch": 2.5, "layer": "glow", "sprite": "armageddon:spark" }
    },
    "fireball": {
      "priority": "hero",
      "spawn":  { "rate": { "curve": [[0, 400], [0.3, 120], [1, 0]], "duration": 30 } },
      "shape":  { "sphere": { "radius": "2*scale" } },
      "init":   { "life": [25, 45], "size": ["1.5*scale", "3*scale"], "velocity": { "radial": [0.05, 0.2] }, "rotation": "random" },
      "update": [ { "buoyancy": { "force": 0.03, "cooling": "ease_out" } }, { "curl_noise": { "freq": 0.15, "strength": 0.05 } }, { "drag": 0.8 } ],
      "over_life": { "color": "gradient:nuke_fire", "size": "pop", "emissive": "ease_out", "erosion": [[0, 0], [0.6, 0.1], [1, 1]] },
      "render": { "mode": "billboard", "layer": "premultiplied", "flipbook": { "sprite": "armageddon:fire_8x8", "mode": "over_life", "blend": true } },
      "light":  { "color": "#FF9040", "intensity": "curve:fire_light", "radius": "30*scale" }
    }
  }
}
```

- Expressions such as `"2*scale"` are parsed once at load into a tiny expression tree.
- Shared gradients and curves (`gradient:nuke_fire`) live in `vfx/_palettes.json`, so every nuke uses the same
  ramp. See the colour table in [DIRECTION.md §3](DIRECTION.md#3-colour-language-for-each-family).

---

## 6. Debug tools (they pay for themselves on day one)

- `/armageddon vfx play <effect> [scale] [seed]`: spawn any effect where you're looking.
- `/armageddon vfx loop <effect> <ticks>`: replay it every N ticks while you edit the JSON and press F3+T.
- `/armageddon vfx slowmo <0.1–1>`: scale `dt` for the FX engine only. Inspect the beats frame by frame.
- A HUD overlay (F3 page): active effects, particles per emitter, budget use, and sim ms / render ms.
- Wireframe mode: draw emitter shapes, bounding spheres and force fields (vortex axis, attractor radius).
- A **capture preset** for the preview harness (`src/preview`): render an effect at fixed ticks to PNGs, so CI can
  publish before/after comparison sheets.

---

## 7. Migration plan

1. **Engine core** (S/M):
   - `ParticlePool`, `Curve`, `Gradient`;
   - the five most valuable modules: Gravity, Drag, Buoyancy, CurlNoise, CollideWorld;
   - three render modes: billboard, stretched, horizontal;
   - additive and alpha layers;
   - no JSON yet: build the definitions in Java.
2. **Rebuild the 12 presets** on it and compare side by side with `/vfx play`.
3. **Premultiplied layer plus new flipbooks** ([TEXTURES.md](TEXTURES.md)). This is the big visual jump.
4. **Sub-emitters, ribbons, the light render mode.**
5. **The JSON format plus hot reload**, then move effect tuning out of Java.
6. **The network payload** (`VfxSpawnPayload`). Port the largest `spawnParticles` users first: nukes, Volcano,
   Tsunami, Tesla.
7. **Budget and LOD, plus quality presets** ([PERFORMANCE.md](PERFORMANCE.md)).
8. *(Optional)* **GPU particles** for ash, embers and stars.

---

## Sources
- [Niagara: key concepts (spawn and update stages, module stack)](https://dev.epicgames.com/documentation/unreal-engine/key-concepts-in-niagara-effects-for-unreal-engine?lang=en-US), [Niagara events and event handlers](https://dev.epicgames.com/documentation/unreal-engine/events-and-event-handlers-in-niagara-effects-for-unreal-engine), [Niagara emitter update reference](https://dev.epicgames.com/documentation/en-us/unreal-engine/emitter-update-group-reference-for-niagara-effects-in-unreal-engine)
- [Bridson, Hourihan and Nordenstam: Curl-noise for procedural fluid flow (SIGGRAPH 2007)](https://www.dneg.com/wp-content/uploads/2022/11/2007.pdf), [SIGGRAPH archive entry](https://history.siggraph.org/?p=115355)
- [ogldev tutorial 28: transform feedback particles](https://ogldev.org/www/tutorial28/tutorial28.html), [UCSD ParticlesTransformFeedback](https://mathweb.ucsd.edu/~sbuss/MathCG2/OpenGLsoft/ParticlesTransformFeedback/PTFexplain.html), [NVIDIA feedback particles sample](https://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/opengl_samples/feedbackparticlessample.htm)
- [Yarn ParticleManager docs (1.21.4)](https://maven.fabricmc.net/docs/yarn-1.21.4+build.5/net/minecraft/client/particle/ParticleManager.html), [MC-278675](https://bugs.mojang.com/browse/MC-278675), [MC-183528](https://bugs-legacy.mojang.com/browse/MC-183528), [Particle Enhancer](https://www.curseforge.com/minecraft/mc-mods/particle-enhancer)
