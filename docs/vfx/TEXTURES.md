# VFX textures: how to make sprites that look expensive

The engine in [PARTICLES.md](PARTICLES.md) is only as good as its textures. Today we have 21 particle sprites
(`flame_0-3`, `gas_0-3`, `smoke_*`, `glow`, ...), most of them 4-frame sequences. This guide covers what to make
next and how.

## 1. Rules for every VFX texture

- **Greyscale and gradient-mapped:** paint shape and value in greyscale, then colour it in the shader through a
  gradient. One smoke texture can then be black smoke, white steam, green gas or violet void.
- **Pre-multiplied or channel-packed**, never a "finished colour" PNG, unless it's a one-off (a logo, the cartoon
  "BOOM").
- **Soft edges, centred, and nothing touching the frame border.** If the shape touches the edge, you see the card's
  square.
- **Power-of-two sizes:** single sprites at 32–128 px, flipbooks as 8×8 grids of 64–128 px frames (512–1024 px
  sheets).
- **Pixel style:** keep the mod's Minecraft look by using **point (nearest) filtering** at 32–64 px per frame, and by
  posterizing the gradient to 6–8 steps. Use linear filtering only for glows and soft dust, where pixels would look
  broken.

## 2. Channel packing

Pack one texture read with several pieces of data. A good layout for our smoke and fire flipbook:

| Channel | Contents | Why |
|---|---|---|
| **R** | Density / shape (greyscale) | Drives the gradient-map lookup (colour) |
| **G** | Temperature / emission mask | How much the pixel glows: the hot core is bright, the smoke is 0 |
| **B** | Erosion threshold map (noise-like) | For alpha erosion: lets the edges break up over the particle's life |
| **A** | Alpha (coverage) | Keep the highest-contrast data in alpha, because it compresses and filters best |

The motion-vector flipbook goes in a **second** half-resolution texture: RG = motion XY. See §4.

## 3. Alpha erosion (dissolve)

Fading by alpha alone looks like a ghost that turns transparent. **Erosion** eats the shape from its thin parts
inward, which is how real smoke and fire break up:

```glsl
// erosion: 0 = full shape, 1 = fully eroded (driven by erosionOverLife)
float mask   = texel.b;                                   // erosion map
float edge   = 0.08;                                      // softness
float a      = texel.a * smoothstep(erosion, erosion + edge, mask);
// optional hot edge: glow right at the eroding border (burning paper, disintegration)
float rim    = smoothstep(erosion + edge * 2.0, erosion + edge, mask) * step(0.001, erosion);
color.rgb   += RimColor * rim * a;
```

- **Make the erosion map** by blurring the alpha, multiplying it by a cloudy noise, and normalising. Thin wisps
  should have low values (they vanish first) and the core high values (it vanishes last).
- The same shader gives:
  - **disintegrating mobs and blocks** (Unmaker, Obliterator): a burning rim;
  - **frost forming** (run it in reverse);
  - **Last Light's erase-to-white.**

## 4. Flipbooks and motion-vector blending

- **Frame blending:** sample frame N and N+1 and mix by the fraction. This is smooth, but the shapes double up
  ("ghosting") when frames differ a lot.
- **Motion-vector blending (the pro technique):**
  - Bake a motion-vector texture with the flipbook. RG holds how each pixel moves to the next frame.
  - At runtime, distort frame N forward and frame N+1 backward along the vectors, then blend.
  - This allows **far fewer frames** at the same smoothness. One report went from an 11×11 sheet (121 frames) to a
    6×6 sheet (32 frames) with the same quality.
  - The motion texture can be **half resolution**.
  - Tools: **TFlow**, a motion-vector generator from Polycount; EmberGen and Houdini can export vectors directly.

```glsl
vec2 mv0 = (texture(Motion, uv0).rg * 2.0 - 1.0) * MotionStrength;
vec2 mv1 = (texture(Motion, uv1).rg * 2.0 - 1.0) * MotionStrength;
vec4 a = texture(Sheet, uv0 - mv0 * blend);
vec4 b = texture(Sheet, uv1 + mv1 * (1.0 - blend));
vec4 texel = mix(a, b, blend);
```

**Where to get flipbooks** (see also [LIBRARIES.md](LIBRARIES.md#asset-sources)):
- **Unity Labs free VFX sequences (CC0):** explosions and smoke simulated in Houdini, as raw frames or packed sheets.
  This is the safest source for a shipped mod.
- **EmberGen:** a real-time fluid sim with a free trial. It exports PNG, TGA or EXR flipbooks plus motion vectors.
  Check the EULA before shipping its output.
- **Make our own in Blender** (free): Mantaflow smoke, rendered orthographic with an emission pass, then assembled
  into a grid with a short Python script (`tools/` already has a Python asset pipeline).

**Frame counts and layouts:**

| Use | Grid | Frame size | Mode |
|---|---|---|---|
| Fire puff | 8×8 | 64 | Over life, motion blend |
| Big smoke billow | 8×8 | 128 | Over life |
| Looping flame tongue | 4×4 | 64 | Looping by time, a random start frame per particle |
| Electric arc decal | 4×4 | 64 | Random frame each tick |
| Dust puff | 4×4 | 64 | Over life |
| Shockwave ring | 1 | 256 | Static; animate the UV and scale in the shader |

## 5. Gradient maps (colour ramps)

- Store ramps as a **256×N strip PNG**: one row per palette (nuke fire, plasma, void, cryo, spore and so on).
  Sample it with `texture(Ramps, vec2(value, (row + 0.5) / N))`.
- **8-bit banding warning:** the gradient texture itself has only 8 bits per channel. Keep ramps short and smooth,
  and dither with blue noise if you see steps.
- **The emission trick:** the ramp's top end should be over 1.0 in the shader (multiply by `EmissiveBoost`), so the
  HDR bloom picks up only the hottest part.
- Artists can edit the ramps PNG directly, with no code change. This replaces the hard-coded `c0/c1/c2` in today's
  `WmdParticle.Preset`.

## 6. Lit smoke: normal and six-way maps

- **Normal-mapped smoke (simple):** bake a normal map for the smoke flipbook (Blender: render a normal pass of the
  smoke volume, or estimate one from the density). Light it in the particle shader with `N·L` towards the sun and
  the fireball. The smoke immediately gains volume.
- **Six-way lighting (the current AAA standard):** render the smoke six times, lit from +X, −X, +Y, −Y, +Z and −Z.
  Pack the results into two RGB textures (right/top/back in one, left/bottom/front in the other). At runtime,
  blend the six by the light direction in the particle's space. It handles back-light, rim light and translucency,
  which normal maps can't.
  - Unity's VFX Graph has a six-way lit smoke mode, and recent EmberGen versions can export the six maps (check your
    version).
  - **Cost:** two texture reads instead of one. It's worth it for the hero smoke only.

## 7. Noise textures to have on hand

| Texture | Size | Use |
|---|---|---|
| Blue noise | 64×64 | Dithering gradients, raymarch jitter, sparkle timing |
| Tileable Perlin/fbm | 256×256 | UV distortion (heat haze, plasma), scrolling trails |
| Tileable Worley (cellular) | 256×256 | Cracks, lava crust, crystal patterns, energy cells |
| Curl noise (RG vector) | 128×128 | Flow-map distortion; CPU-side particle swirl lookup |
| Caustics | 256×256 animated (16 frames) | Water, plasma pools, the light under ice |
| 3D Worley+Perlin | 64³ (uploaded raw) | The raymarched clouds and fireballs ([TECHNIQUES §6](TECHNIQUES.md#6-raymarched-fireball-and-mushroom-cloud)) |

Generate them with a small Python script (numpy) in `tools/`, so they are reproducible and their licence is clean.

## 8. Shader tricks that turn one sprite into many

- **UV distortion:** offset the sprite's UV by a scrolling noise texture. Fire licks, plasma boils and portals
  shimmer.
- **Polar coordinates:** map a straight noise strip around a circle (`uv = vec2(atan(p.y, p.x)/TAU, length(p))`).
  This gives energy rings, vortex swirls, magic circles and the shockwave ring with streaks, all from one texture.
- **Flowmaps:** an RG texture that says "this pixel moves this way". Blend two phases to avoid stretching. This gives
  a lava surface flowing outward, or water pouring.
- **Fresnel and rim light on mesh particles:** `pow(1 - |N·V|, 3)` glows at the edges of shards, bubbles and the
  Wilson shell.
- **Depth fade:** soft particles; see [TECHNIQUES §5](TECHNIQUES.md#5-soft-particles-and-better-flipbooks).
- **Camera-distance fade:** fade particles out as they get close to the near plane, so smoke never fills the screen.
- **Random per-particle seed:** use the particle's seed to offset the UV, flip it, rotate it or pick its frame. Twelve
  copies of one texture then never look like copies.

## 9. Checklist for a new VFX texture

- [ ] Greyscale shape in R, with alpha in A.
- [ ] Nothing touches the border (at least 2 px of fully transparent margin).
- [ ] An erosion map in B, if it will fade.
- [ ] An emission mask in G, if any part glows.
- [ ] Power-of-two dimensions, with the grid noted in `vfx/_textures.json`.
- [ ] A motion-vector texture, if it's an over-life flipbook with fewer than 32 frames.
- [ ] Tested with three gradient ramps (fire, smoke, energy) to prove it's reusable.
- [ ] Licence recorded (CC0, self-made, or with written permission).

## Sources
- [VFX documentation: alpha erosion](https://vfxdoc.readthedocs.io/en/latest/shaders/alpha-erosion/), [VFX documentation: textures and channel packing](https://vfxdoc.readthedocs.io/en/latest/textures/overview/), [VFX documentation: flipbooks](https://vfxdoc.readthedocs.io/en/latest/textures/flipbooks/)
- [Realtime VFX dictionary](https://realtimevfx.com/t/realtime-vfx-dictionary-project/570), [Realtime VFX: erosion maps](https://realtimevfx.com/t/question-about-erosion-maps/12556), [Godot alpha erosion node](https://godotshaders.com/shader/alpha-erosion-visualshadernode-4-4/)
- [Creating dissolve VFX with Unity surface shaders](https://gamedeveloper.com/programming/the-great-disappearing-act-creating-dissolve-vfx-with-unity-surface-shaders)
- [TFlow motion vector generator](https://polycount.com/discussion/228130/tflow-motion-vector-generator), [Motion vectors for animated textures (Realtime VFX)](https://realtimevfx.com/t/motion-vectors-for-animated-textures-how-do-you-generate-them/575?page=4), [Optimized explosions for UE5 (frame-count reduction)](https://loronsodev.github.io/posts/explosion-fx), [UE5 flipbook motion vector tutorial](https://forums.unrealengine.com/t/community-tutorial-level-up-your-flipbooks-with-motion-vectors-and-subuv-blending-unreal-5-tutorial/2694470)
- [Star Citizen explosion VFX (80.lv)](https://80.lv/articles/vfx-of-star-citizen-working-on-explosions/)
- [Unity free VFX flipbooks (CC0)](https://unity.com/blog/engine-platform/free-vfx-image-sequences-flipbooks), [EmberGen](https://jangafx.com/software/embergen)
