# Effect recipe book

These are layered recipes for the engine in [PARTICLES.md](PARTICLES.md). Every great effect is **several simple
layers with different timing**, never one emitter doing everything. The numbers are starting points at
`scale = 1`: about grenade size, with times in ticks (20 = 1 s) and sizes in blocks.

How to read a recipe:
- Each table is one effect, and each row is one layer.
- **t** is the start tick of the layer.
- **Render:** add = additive glow, pre = premultiplied, alpha = smoke.
- **★** marks the layer that makes the effect.

Contents: [Explosions](#explosions) · [Fire and heat](#fire-and-heat) · [Smoke and dust](#smoke-and-dust) ·
[Energy and sci-fi](#energy-and-sci-fi) · [Electricity](#electricity) · [Ice and water](#ice-and-water) ·
[Void and gravity](#void-and-gravity) · [Nature and magic](#nature-and-magic) · [Weapons](#weapons) ·
[Impacts and aftermath](#impacts-and-aftermath) · [Fun](#fun)

---

## Explosions

### Grenade / small blast (frag, demolition)
| Layer | t | Count | Life | Size | Motion | Render | Notes |
|---|---|---|---|---|---|---|---|
| Flash | 0 | 1 | 3 | 4 | — | add, billboard | White to orange; size "pop"; plus a light of 0.2 s with radius 12 |
| ★ Fire puff | 0 | 12 | 10–18 | 1–1.8 | radial 0.15, buoyancy | pre, flipbook | Hot core to smoke edge (premultiplied) |
| Sparks | 0 | 40 | 10–25 | 0.06 | radial 0.6–1.4, gravity, collide (bounce 0.4) | add, stretched ×3 | A few spawn a spark puff on hitting the ground |
| Debris chunks | 0 | 8 | 20–40 | 0.25 | cone up 0.4–0.8, gravity, collide | mesh (block) | Use the block that was destroyed (`blocks` shape) |
| Smoke | 3 | 10 | 40–80 | 1.5→3 | buoyancy 0.01, curl noise, wind | alpha, flipbook, erosion | Lit orange by `selfLight` for the first 10 ticks |
| Ground ring | 1 | 20 | 15–25 | 0.8 | ring r 0.5, radial 0.3, ground-hugging | alpha, horizontal | Dust skirt |
| Scorch decal | 2 | 1 | 600 | 3 | — | decal | Fades out over its last 200 ticks |

### Nuke (Atomic, Neutron, Tsar)
Multiply all sizes by `scale` (4–16). The timeline spans about 60 s. The key is the **long, slow rise**.

| Layer | t | Notes |
|---|---|---|
| Pre-dim | −10 | Pull the world exposure down 15% and duck the sound |
| Flash | 0 | Full-screen exposure spike of 2 ticks, then the glare limiter. A light of intensity 8 with radius 300 |
| ★ Fireball | 0 | Raymarched sphere ([TECHNIQUES §6](TECHNIQUES.md#6-raymarched-fireball-and-mushroom-cloud)); fallback: 400 premultiplied fire particles in a sphere |
| Wilson shell | 4 | A white fresnel sphere growing from 0.6 to 1.4 × r over 20 ticks, alpha 0.5 → 0 |
| Shockwave | 2 | World-space refraction ring at 40 blocks/s ([TECHNIQUES §3](TECHNIQUES.md#3-shockwave-refraction-ring-in-world-space)), and the `Pressure` force is active |
| Dust skirt | 10 | `ring` emitter following the shockwave front: alpha horizontal, sizes 4–8, a rate of 600/s while the ring is within 120 blocks |
| Stem | 20 | A `Rate` up a cylinder: buoyancy is high at the bottom, the swirl is `Vortex(axis Y)`, and it cools over its life |
| ★ Cap | 40 | A torus emitter: `Orbit` around the ring axis (the rolling cap), colour cooling white → orange → red → grey over 400 ticks |
| Condensation ring | 120 | A thin white horizontal ring around the stem at cloud height |
| Embers | 30 | 2,000 small add particles falling from the cap (GPU path when available) |
| Ash fall | 200 | Grey flakes falling for 60 s within 150 blocks, drifting with the wind, plus a dim warm sky |
| Secondary fires | 60 | 6–12 small smoke columns placed on flammable blocks within the radius |
| Light | 0–600 | Decays slowly over 30 s from orange to deep red |

**Variants:**
- **Neutron:** a blue-white palette, a tiny fireball, a strong blue Cherenkov decal and a radiation shimmer.
- **Antimatter:** a violet-white palette, a lensing flash and no smoke (just a clean annihilation ring).

### Thermobaric / fuel-air
1. A **fuel cloud** for 40 ticks: alpha, a pale haze of 80 particles slowly expanding.
2. Then **ignition races through it**: each cloud particle gets a sub-emitter `onDeath → fire puff`, and their
   lifetimes are ordered by distance from the centre.
3. The flame front sweeps through the haze, then a big fireball and a strong pressure wave.

### KA-BOOM! / cartoon
- Use a white flash, then a **star-shaped** flash sprite and comic hit lines (stretched add, radial).
- Smoke as round, flat cartoon puffs with a hard edge and posterized colour, bouncing in size ("pop" ×1.4).
- Optionally add a "BOOM" text decal for 10 ticks, as a fun easter egg.

---

## Fire and heat

### Campfire-grade fire (napalm, Hellmouth vents, burning ground)
| Layer | Rate | Life | Size | Motion | Render |
|---|---|---|---|---|---|
| ★ Flame tongues | 30/s | 12–20 | 0.5–1 | buoyancy 0.05, curl noise 0.2 | pre, `billboard_axis_y`, flipbook by life |
| Core glow | 4/s | 20 | 1.5 | — | add, soft glow sprite, flicker |
| Embers | 6/s | 30–60 | 0.04 | buoyancy, turbulence | add, stretched |
| Smoke | 5/s | 60–100 | 1→2.5 | buoyancy 0.01, wind | alpha, erosion |
| Light | — | — | — | Flicker noise 8 Hz ±20% | light, orange, radius 8 |
| Heat haze | — | — | — | — | Vision-pass haze volume above the fire |

### Thermite / welding sparks
- **The hero layer:** white-hot sparks with `CollideWorld(bounce 0.3, friction 0.6)` that slide and pile up before
  they die, stretched, plus a molten emissive decal.
- Add a blinding small core with a strong flickering light, and white smoke (pale, not black).

### Lava fountain (Volcano)
- **Lava bombs:** mesh particles (magma block), velocity-aligned, gravity. A `PerDistance` smoke trail.
  `onCollision →` a splash of 12 lava drops, a decal and a light of 1 s.
- **Lava spray:** a `Rate` 200/s cone up, pre, an orange-red gradient going dark at the end of life.
- **Ash column:** see [Smoke and dust](#smoke-and-dust).

---

## Smoke and dust

### Big smoke column (burning city, volcano ash)
- **Body:** 30–60 large particles (size 6–12) that live 400–800 ticks. Buoyancy weakens with height. The wind
  increases with height, so the column bends.
- **Billowing:** each particle's size curve ends at about 2.5×. Use slow rotation (±0.5°/tick). Erosion only in the
  last 30% of life.
- **Lighting:** a dark top and an orange-lit base (`selfLight` at the fire), and six-way lighting when available.
- **Density from far away:** use fewer, larger particles at a distance (LOD), so the silhouette stays the same.

### Shockwave dust wall
- Use a `ring` shape spawning on the **ground surface** (a heightmap lookup per particle), moving outward at the
  front speed.
- Render horizontal and billboard dust together. Keep the height low (2–6 blocks) at the front and higher behind it,
  as if it's settling.
- Colour comes from the block under the spawn point: sand-coloured dust in deserts, white in snow.

### Debris dust (block break)
- At each destroyed block face, add 1–3 dust puffs in the block's map colour and a few small `mesh` chips.
- Cap this per tick globally: big destructions can remove 10,000 blocks per tick.

---

## Energy and sci-fi

### Plasma bolt (Plasma Rifle, Helix Disruptor)
| Layer | Notes |
|---|---|
| ★ Core | A small, very bright add billboard (white with a cyan rim) |
| Halo | A larger soft add, 0.3 alpha, pulsing at 10 Hz |
| Trail | A ribbon with 20 points, width tapering 1 → 0, a scrolling noise texture, cyan → violet |
| Sparkle | `PerDistance` 2/block: tiny add stretched sparks with drag, living 8 ticks |
| Light | Moving point light, cyan, radius 6 |
| Impact | Burst of 30 sparks, a flash, a horizontal shockwave ring and an emissive decal that cools from cyan to dark blue |

### Charging weapon / orb (Halo Cutter, the Tesla orb)
- **Inflow:** particles spawn on a sphere of radius 3 with an `Attractor` to the muzzle. They're killed on arrival,
  stretched, and the rate grows with the charge (a curve).
- **The core:** grows with the charge and shakes slightly; it has a ring of `Orbit` particles around it.
- **Full charge:** the inflow stops for 4 ticks (that silence is anticipation), then the release.

### Orbital beam (Ion Scythe, Dyson Lance, Alien Death Ray)
- **Beam:** a geometry core plus halo, as now. Add heat haze along the beam and a **ground contact** emitter: molten
  spray, add sparks and a rising smoke wall.
- **The cut:** a `PerDistance` emissive decal along the path that cools white → orange → red → black over 10 s.
- **Air ionisation:** faint violet sparks drifting near the beam, with curl noise.

### Hologram / scan (Hologram, Lidar)
- Particles on a grid (`box` shape, quantised positions), add, flickering, plus slow vertical scrolling scanlines.
- A **"scan front"** wiping through the world in the vision pass.

---

## Electricity

### Arc (Tesla, Arc Thrower, Storm Caller)
- **Bolt:** a ribbon made by midpoint displacement (6–8 levels) between A and B, with 2–5 branches. Re-randomise
  every 2–3 ticks, and keep each shape for 1–2 frames only.
- **Two strips:** a 0.05 white core and a 0.4 blue-violet halo, both add.
- **At the endpoints:** spark bursts (stretched, gravity, bounce) and a scorch dot decal.
- **Light:** a flickering light at the midpoint (random 50–150% each tick).
- **Afterimage:** the previous bolt stays at 30% alpha in violet for 6 ticks.

### Ball lightning / plasma orb
- Use 6–10 short arcs from the centre to random points on the sphere, re-rolled every tick, plus a pulsing core and
  a faint violet haze.

---

## Ice and water

### Cryo burst (Cryo Grenade, Shatterfrost, Ice Age)
| Layer | Notes |
|---|---|
| Flash | Pale blue, short |
| ★ Ice shards | `mesh` particles (thin crystals), radial and fast, gravity, `CollideWorld(killOnHit)` → `onCollision` gives a small sparkle |
| Frost mist | Alpha, pale blue, low and heavy (**negative** buoyancy): it pours down slopes and pools |
| Sparkle | Tiny add dots that twinkle (alpha curve noise) for 60 ticks |
| Frost decal | A crystal-pattern decal spreading outward over 40 ticks |
| Breath fog | Entities in the radius exhale small white puffs every 40 ticks for 30 s |

### Water splash / tsunami crest
- **Sheet:** a crown of stretched drops (velocity-aligned) in a ring cone, gravity, and `onCollision` with water →
  a ripple ring (horizontal).
- **Foam:** white alpha particles on the crest with drag. **Mist:** a large soft alpha haze downwind.
- For the tsunami, spawn along the **front line** at a rate proportional to the crest height.

---

## Void and gravity

### Singularity / black hole
| Layer | Notes |
|---|---|
| Lensing | Post pass (already exists) |
| ★ Accretion disk | `Orbit` particles on a disc with Keplerian speed (`v ∝ 1/sqrt(r)`), hot white inside, red outside, add, stretched along the orbit; one side brighter (Doppler beaming) |
| Infall | Debris and dust with an `Attractor` and stretch increasing near the centre (spaghettification), killed at the horizon |
| Jets | Two thin add ribbons along the axis, flickering |
| Photon ring | A thin bright ring at 1.5 × the horizon, in the post pass |

### Void wisps (Shadow Void, Unmaker)
- **Ink-black** particles (alpha, black with a violet rim through a gradient map) with curl noise and slow inward
  drift.
- **Negative glow:** an emissive *subtract*. Draw these on a layer that darkens what's behind them
  (`ZERO, ONE_MINUS_SRC_COLOR` blending). Darkness that seems to **eat light** is very unsettling and very cool.

---

## Nature and magic

### Spores (Eternal Sapling)
- Soft add dots with a slow, swaying curl noise and random bob, plus a pale green light pulsing per cluster.
- Tiny falling leaves as `mesh` quads that flutter: rotation over life plus a side-to-side sine wobble.

### Divine light (Godfall, Helios Crown)
- **God rays:** additive vertical quads with `billboard_axis_y`, a soft gradient sprite and dust motes drifting
  inside the shafts.
- **Feathers or gold dust:** slow falling with flutter. A bloom-friendly gold.

### Aurora
- 5–8 horizontal ribbons high in the sky. Wave the vertices with noise, and add green → violet vertical gradients.
- Fade alpha in and out on a slow noise.

---

## Weapons

### Muzzle flash
- 1–2 ticks: a 4-point star sprite with random rotation (add), plus a side flash and a forward cone of 6 stretched
  sparks.
- A light of 1 tick, plus a small smoke puff that drifts.
- **Variety:** pick from 3 flash sprites at random. Repetition is what makes guns look cheap.

### Bullet tracer / rail slug
- Stretched add with length equal to its speed. For the railgun, also add a **lingering ionised ribbon** that drifts
  upward and fades over 40 ticks.

### Melee swing (Gravity Hammer, Plasma Blade)
- **Swing arc ribbon:** a ribbon from the blade tip positions over the last 6 ticks, with a bright inner edge and an
  alpha gradient.
- **Impact:** a 3-tick hit-stop, a flash, a ring and a radial burst of sparks matching the surface hit.

---

## Impacts and aftermath

### Generic surface impact (match it to the block)
| Block group | Particles |
|---|---|
| Stone, ore | Grey chips (mesh), dust puff, sparks if it's metal |
| Dirt, grass | Brown clumps, grass bits, a soft dust puff |
| Wood | Splinters (thin mesh), light dust |
| Sand, gravel | Granular spray (many tiny particles, gravity), a big dust puff |
| Water | Splash crown, ripple |
| Glass, ice | Glinting shards, sparkle |
| Metal (iron, copper) | Bright sparks, a ricochet streak |

### Aftermath kit (every big event)
- Lingering low smoke (alpha, very slow, 30–60 s).
- Cooling emissive decals and **heat shimmer** above the crater for 20 s.
- Falling ash or debris dust in the air (small, slow, drifting with the wind).
- Small fires on flammable blocks (the campfire recipe at 0.3 × scale).
- A dim sky tint fading back over about 60 s.

---

## Fun

### Confetti (Party Popper, Disco Bomb)
- Small `mesh` quads in 6 palette colours, with fast spin (a random axis), flutter and strong drag.
- When a quad faces the sun, use an emissive glint (fresnel) so the confetti **sparkles**.

### Firework (all the fun bombs)
- **Rocket:** a `PerDistance` spark trail. `onDeath →` the burst:
  - a sphere, heart, star or creeper shape (sample shape points);
  - stretched add with gravity and drag;
  - colour fading to a second colour;
  - optionally a **crackle**: each spark has a 30% `onDeath` chance to make a tiny white flash.
- Add a light flash for the burst, and the boom delayed by distance.

### Disco ball
- 30–60 thin add beams from the ball (ribbons of 2 points), rotating with the ball.
- Where each beam hits a surface (raycast every 5 ticks), draw a small coloured light dot (a decal plus a small
  light).
- A tiny sparkle on the ball wherever a beam starts.
