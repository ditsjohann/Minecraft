# Techniques we can build ourselves

Every technique here fits the pipeline the mod already has:

- **`PostFx`**: a colour-only post chain.
- **`Vision`**: the depth-aware pass. It copies colour and depth, and it has `InvViewProj`, so each pixel knows its
  world position.
- **`FxBuffers` / `FxRenderLayers`**: world-space geometry. Additive glow layers are built on `LIGHTNING_PROGRAM`.
- **`WmdParticle`**: billboards.

The GLSL is `#version 150`, the same as our existing programs. The snippets are sketches: they show the idea and the
maths, not drop-in code.

Contents:
1. [Real bloom: a mip chain with a dual filter](#1-real-bloom-mip-chain-dual-filter)
2. [Selective (emissive-only) bloom](#2-selective-emissive-only-bloom)
3. [Shockwave refraction ring in world space](#3-shockwave-refraction-ring-in-world-space)
4. [Volumetric heat haze](#4-volumetric-heat-haze)
5. [Soft particles and better flipbooks](#5-soft-particles-and-better-flipbooks)
6. [A raymarched fireball and mushroom cloud](#6-raymarched-fireball-and-mushroom-cloud)
7. [Explosions that light the world](#7-explosions-that-light-the-world)
8. [Motion blur, zoom blur and shutter](#8-motion-blur-zoom-blur-and-shutter)
9. [Impact frames](#9-impact-frames)
10. [Projected scorch decals](#10-projected-scorch-decals)
11. [Client-only debris with real tumbling](#11-client-only-debris)
12. [Ribbons and trails](#12-ribbons-and-trails)
13. [Better lightning](#13-better-lightning)
14. [Event skies](#14-event-skies)
15. [Depth of field and lens flares in cutscenes](#15-depth-of-field-and-lens-flares)
16. [A per-bomb colour grade with LUTs](#16-per-bomb-colour-grade-with-luts)
17. [Noise and texture toolkit](#17-noise-toolkit)
18. [Afterimage, stop-motion and time effects](#18-time-effects)

---

## 1. Real bloom (mip chain, dual filter)

**Why:** a single full-screen blur gives either a tight glow or a muddy screen. A mip chain adds up halos at five or
six scales at once, which gives the "the whole sky is glowing" look for very little cost. This is the technique from
Marius Bjørge's SIGGRAPH 2015 "dual filter" talk: four downsample passes and four upsample passes, faster than a
Gaussian at nearly the same quality. The 13-tap downsample with a 9-tap tent upsample is the popular "next-gen post"
variant.

**How, in our code:**
- A post JSON in 1.21.1 can't easily declare targets sized relative to the screen. So build the chain in Java, the
  same way `Vision` already manages its own `SimpleFramebuffer`s.
  - Make 6 framebuffers at 1/2, 1/4 … 1/64 of the screen size.
  - Re-create them on resize, in the same place `PostFx` calls `setupDimensions`.
- **Use HDR:** a `GL_RGBA16F` colour attachment, created by re-allocating the texture with
  `GlStateManager._texImage2D(..., GL30.GL_RGBA16F, ..., GL11.GL_FLOAT, null)`. Without it, bright values clip to 1.0
  and a fireball blooms no harder than a white wool block.
- Passes:
  1. **Prefilter / bright pass** (full size to 1/2): a soft-knee threshold, plus a **Karis average** on this first
     downsample. Without it, single hot pixels flicker ("fireflies").
  2. **Downsample** ×5: a 13-tap box filter.
  3. **Upsample** ×5: a 3×3 tent, added onto the next larger mip.
  4. **Composite** in `armageddon_fx.fsh`: `col += bloom * BloomStrength`, before the ACES curve that's already there.

```glsl
// prefilter: soft-knee threshold + Karis average (luma-weighted) to kill fireflies
vec3 prefilter(vec3 c) {
    float br = max(c.r, max(c.g, c.b));
    float knee = Threshold * 0.5;
    float soft = clamp(br - Threshold + knee, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee + 1e-4);
    float contrib = max(soft, br - Threshold) / max(br, 1e-4);
    return c * contrib;
}
float karis(vec3 c) { return 1.0 / (1.0 + dot(c, vec3(0.2126, 0.7152, 0.0722))); }

// 13-tap downsample (texel = 1.0 / source size)
vec3 down13(sampler2D s, vec2 uv, vec2 t) {
    vec3 a = texture(s, uv + t * vec2(-2, 2)).rgb, b = texture(s, uv + t * vec2(0, 2)).rgb, c = texture(s, uv + t * vec2(2, 2)).rgb;
    vec3 d = texture(s, uv + t * vec2(-2, 0)).rgb, e = texture(s, uv).rgb,                f = texture(s, uv + t * vec2(2, 0)).rgb;
    vec3 g = texture(s, uv + t * vec2(-2,-2)).rgb, h = texture(s, uv + t * vec2(0,-2)).rgb, i = texture(s, uv + t * vec2(2,-2)).rgb;
    vec3 j = texture(s, uv + t * vec2(-1, 1)).rgb, k = texture(s, uv + t * vec2(1, 1)).rgb;
    vec3 l = texture(s, uv + t * vec2(-1,-1)).rgb, m = texture(s, uv + t * vec2(1,-1)).rgb;
    return e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
}

// 9-tap tent upsample, added on top of the next-larger mip
vec3 up9(sampler2D s, vec2 uv, vec2 t, float radius) {
    vec4 d = t.xyxy * vec4(1, 1, -1, 0) * radius;
    vec3 sum = texture(s, uv - d.xy).rgb + texture(s, uv - d.wy).rgb * 2.0 + texture(s, uv - d.zy).rgb
             + texture(s, uv + d.zw).rgb * 2.0 + texture(s, uv).rgb * 4.0 + texture(s, uv + d.xw).rgb * 2.0
             + texture(s, uv + d.zy).rgb + texture(s, uv + d.wy).rgb * 2.0 + texture(s, uv + d.xy).rgb;
    return sum / 16.0;
}
```

**Tuning:**
- Give each mip its own weight. Weight the low mips (big halos) higher for nukes and stars, and the high mips (tight
  glow) higher for plasma weapons. Expose `BloomTint` per event: a nuclear bloom is warm white, a void bloom is
  violet.
- Our glare limiter must sit **after** bloom, so a cutscene can't white out the screen.

## 2. Selective (emissive-only) bloom

**Why:** a threshold bloom also lights up snow, clouds and white UI. Cascade advertises "bloom that only affects
Cascade effects". We can do the same.

**How:**
- `FxBuffers` already sorts geometry by layer. While the `GLOW*` layers draw, **draw them a second time** into an
  `emissive` framebuffer:
  - it shares the main depth buffer, so the world still hides them;
  - it starts cleared to black.
- Use the emissive buffer as the bloom prefilter input instead of the bright-passed main colour. Threshold becomes 0,
  and intensity is whatever the effect's vertex colour says.
- The extra cost is one more draw of the FX geometry, but only of the FX geometry, which is small.
- **Mix the two:** 80% emissive bloom plus 20% threshold bloom (high threshold) still lets lava and the sun bloom a
  little, without the snow problem.

## 3. Shockwave refraction ring in world space

**Why:** the iconic nuke shot is a sphere of bent air racing outward, a white condensation shell (a "Wilson cloud"),
then a wall of dust that hugs the ground. Our `ripple` uniform is screen-space today, so it can't wrap correctly over
hills or pass behind buildings.

**How (vision pass, which already has the world position):**

```glsl
uniform vec3  WaveCentre;   // camera-relative
uniform float WaveRadius;   // blocks, grows ~ 30-120 blocks/s
uniform float WaveWidth;    // 3-8 blocks
uniform float WaveStrength;

vec3 worldAt(vec2 uv) { vec4 w = InvViewProj * vec4(uv * 2.0 - 1.0, texture(DepthSampler, uv).r * 2.0 - 1.0, 1.0); return w.xyz / w.w; }

void shockwave(inout vec2 uv, inout vec3 col) {
    vec3 p = worldAt(uv);
    float d = length(p - WaveCentre);
    float band = exp(-pow((d - WaveRadius) / WaveWidth, 2.0));     // 1 on the front
    // refraction: push the lookup along the screen-space direction away from the centre's projection
    vec4 c = ViewProj * vec4(WaveCentre, 1.0);
    vec2 cuv = c.xy / c.w * 0.5 + 0.5;
    vec2 dir = normalize(uv - cuv + 1e-5);
    uv += dir * band * WaveStrength * 0.02 / (1.0 + length(p) * 0.01);   // weaker far away
    // dust skirt: only near the ground, behind the front
    float behind = smoothstep(WaveRadius, WaveRadius - 25.0, d) * step(d, WaveRadius);
    float ground = smoothstep(8.0, 0.0, p.y - GroundY);
    col = mix(col, DustColor, behind * ground * 0.35 * WaveStrength);
    col += vec3(1.0) * band * 0.08 * WaveStrength;                  // bright compression edge
}
```

- **The Wilson cloud:** a short-lived (about 1 second) translucent white sphere at `WaveRadius * 0.6`, drawn with
  `SHADE`. Use a fresnel alpha (`1 - |N·V|`, bright at the rims) and fbm-eroded edges. It's cheap, and it reads
  instantly as "real nuke footage".
- **Sync:** compute when the wave reaches the camera and fire the big shake and FOV punch at that moment, not at
  detonation.

## 4. Volumetric heat haze

- Limit the haze to a world-space **cylinder or sphere above the fireball and lava**, using the reconstructed
  position, instead of distorting the whole screen.
- Strength should scale with the length of the view ray inside the volume (a ray-capsule intersection). Looking
  through the column distorts a lot; looking past it distorts nothing.
- Offset the UV with two scrolling noise octaves that rise (`uv.y - time`). Never distort pixels that are closer than
  the volume (`sceneDepth < entryDepth`): this keeps the hand and nearby blocks sharp.

## 5. Soft particles and better flipbooks

**Soft particles:**
- Billboards that cut hard into terrain are the number-one "cheap" tell. Fade alpha by how close the particle is to
  the scene behind it.
- **How:** register a custom core shader for our particle sheet. Fabric API's rendering module has a core-shader
  registration callback (`CoreShaderRegistrationCallback`; check the exact name in 0.116.x). Bind the depth copy that
  `Vision` already makes as an extra sampler, then:

```glsl
float scene = linearize(texture(SceneDepth, gl_FragCoord.xy / ScreenSize).r);
float self  = linearize(gl_FragCoord.z);
alpha *= clamp((scene - self) / SoftRange, 0.0, 1.0);   // SoftRange ~ 1.5 blocks for smoke, 0.3 for sparks
```

- **Fallback:** when the depth copy isn't ready, or a shader pack is on, use the current particle shader.

**Flipbooks:**
- Replace the 4-frame `smoke_*` and `flame_*` sprites with **8×8 sheets** (64 frames) downsampled from the CC0 Unity
  Houdini sequences. See [LIBRARIES.md](LIBRARIES.md#asset-sources).
- **Frame blending:** sample frame N and N+1 and mix by the fractional part, so the animation runs smoothly at any
  speed.
- **Fake lighting on flipbooks:** bake a *normal* or "light-from-below" channel into the sheet's blue channel. In the
  shader, tint the underside with `FireColor` and the top with the sky colour. Smoke instantly looks lit by the
  fireball.
- **Colour by temperature, not by texture:** keep sheets in greyscale and map their value through our three-stop ramp
  (`WmdParticle.Preset`). That way one sheet serves fire, plasma, void and frost.

## 6. Raymarched fireball and mushroom cloud

**Why:** this is the effect that makes people say "how is this Minecraft". A real 3D, self-lit, slowly churning cloud
that has a silhouette from every angle.

**How (in the vision pass, or a dedicated half-resolution pass):**
- **Shape (SDF):** the stem is a tapered capsule. The cap is a squashed torus with a sphere filling its centre. Both
  grow over time.
- **Density:** `fbm(p * freq + vec3(0, -time * rise, 0))` eroding the SDF edge (the standard cloud trick). Add a
  rolling flow with a torus swirl: rotate the sample position around the torus ring as time goes on, so the cap rolls
  outward and down like real footage.
- **Lighting:**
  - emission from a temperature field: hot at the core and base, cooling with height and age;
  - mapped through a blackbody ramp (1,000 K dark red, 1,500 K orange, 3,000 K yellow-white);
  - Beer–Lambert absorption for the smoke (`T *= exp(-density * sigma * step)`);
  - one cheap light sample towards the sun, for a lit top and a dark underside.
- **Depth-correct:** stop the march at the scene depth from `DepthSampler`, so terrain and buildings cut the cloud
  properly.

```glsl
for (int i = 0; i < STEPS; i++) {                  // 32-48 steps, jittered start with blue noise
    if (t > tScene || T < 0.02) break;
    vec3 p = ro + rd * t;
    float den = cloudDensity(p, Age);              // SDF + fbm erosion
    if (den > 0.0) {
        float temp = heat(p, Age);                 // 0..1
        vec3 emit = blackbody(mix(900.0, 3200.0, temp)) * temp * temp * EmitBoost;
        vec3 lit  = mix(ShadowCol, SunCol, lightMarch(p));
        vec3 c = emit + lit * (1.0 - temp);
        float a = 1.0 - exp(-den * Sigma * dt);
        acc += T * a * c;
        T   *= 1.0 - a;
    }
    t += dt;
}
col = col * T + acc;
```

- **Budget:** draw at **half resolution** into its own target, then upsample with a depth-aware (bilateral) filter
  to avoid halos at the edges. Use 32 steps plus a blue-noise jitter. Optionally blend 85% with last frame's result
  (temporal accumulation) to hide the noise.
  - This should run in about 1–2 ms on a mid-range GPU.
  - The low-quality preset falls back to today's billboard cloud.
- **Reference:** Iñigo Quilez's volumetric plasma flame (with a Godot port) for fbm-driven fire. Maxime Heckel's
  "real-time dreamy cloudscapes" for the raymarch loop. Both are linked in the sources.
- **Reuse:** the same raymarcher with a different density function gives us:
  - the Tesla orb (noise plus electric filaments);
  - the void sphere (a VOID mode upgrade);
  - Deadly Twister (a funnel SDF with a swirl);
  - Moonfall dust;
  - Dyson Lance smoke;
  - Volcano ash plumes.

## 7. Explosions that light the world

**Why:** today a fireball glows, but the houses next to it don't turn orange. Coloured light on the surroundings is
what "sells" an explosion in every film.

**Option A: a screen-space light pass (ours, works with any setup):**
- In the vision pass, reconstruct the world position. Get a normal from depth derivatives:
  `normalize(cross(dFdx(p), dFdy(p)))`, or better, three depth taps.
- Accumulate up to **8 point lights** passed as uniforms. Each light needs a position (camera-relative), a colour, an
  intensity and a radius, which packs into two `vec4`s, so a `mat4` uniform holds two lights.
- `col += albedoGuess * lightCol * max(dot(N, L), 0) * falloff`, where `albedoGuess = col / (lum(col) + 0.25)` keeps
  the hue of whatever block is there.
- Add a tiny **screen-space shadow**: march 8 steps from the pixel towards the light through the depth buffer. Hills
  then cast long shadows away from a fireball as it rises. This is the money shot.
- Feed lights from: fireballs, plasma bolts, muzzle flashes (one or two frames), lightning (flicker), lava pools,
  Tesla arcs.

**Option B: Veil's deferred lights.** Real point and area lights in its deferred pipeline. Better quality, but it
needs the dependency and `VeilDeferredRenderer.isSupported()`. See [LIBRARIES.md](LIBRARIES.md#veil-foundrymc--the-strongest-candidate).

**Option C: a lightmap boost (cheapest):**
- Push the vanilla lightmap's block-light row warmer and brighter while a fireball is alive, and nudge the sky light.
- It's a global effect with no position, so it only suits world-scale events (The Sun Disappears, Helios Crown).

## 8. Motion blur, zoom blur and shutter

- **Camera motion blur by reprojection:**
  - Store last frame's `ViewProj`.
  - For each pixel, reproject `worldAt(uv)` through it to get a velocity, and blur along it (8 taps).
  - Turn it **only on during shake spikes, cutscene whip-pans and shockwave arrival**, not all the time. Players hate
    constant blur.
- **Zoom (radial) blur toward the impact point** for 6–10 frames on a big detonation. It's cheap and reads as an
  "impact".
- **180° shutter feel in cutscenes:** blend 50% of last frame's (blurred) colour. This gives cinematic motion
  smoothing on camera paths.

## 9. Impact frames

These come from anime and fighting games. On the exact frame of impact, show 1–3 frames of a **high-contrast
silhouette**:
- the world goes black;
- the energy goes pure white (or the inverse);
- with depth edges as ink lines (our COMIC mode already finds edges).

Follow it with an instant return plus chromatic aberration that decays over about 0.3 seconds.

- **In code:** an `ImpactMode` already exists in `armageddon_fx.fsh`. Add a mode that uses the emissive buffer from
  §2 as the white mask.
- **Use it sparingly:** only for the weapon finishers (Halo Cutter landing, Galactic Smash punch, Colossal Spear
  hit). If every bomb has it, it stops being special.

## 10. Projected scorch decals

- After an explosion, draw **screen-space decals**: for each pixel inside a projected box (from the reconstructed
  position), sample a scorch texture by world XZ.
- This gives soft-edged scorch rings, glassed sand, radioactive glow and frost patterns that wrap over any terrain.
  **No block changes are needed**, and it fades out over 30–60 seconds.
- Emissive decals (cooling lava cracks, a blue Cherenkov glow at ground zero, Tesla burn lines) also feed the light
  pass from §7.
- Keep a cap of 16 decals, oldest first out. Pass them as an array of `vec4`s.

## 11. Client-only debris

- Today, destruction removes blocks and spawns particles.
- **Better:** for the 200–600 nearest removed blocks, spawn **client-only fake block chunks**. Render the real block
  model with `BlockRenderManager` into `FxBuffers` at 0.3–0.6 scale.
  - Give each chunk real tumbling (an angular velocity), gravity, one or two bounces off the ground (check
    `world.getBlockState` below it), then a fade or sink.
  - Batch them all into a single buffer per layer.
- **Hero debris:** a few large slabs (2×2×1) that fly past the camera in cutscenes. Huge sense of scale.
- **Smoke trails** behind the burning debris (one particle every 2 ticks). This is the "Michael Bay" look.

## 12. Ribbons and trails

- Every weapon projectile and orbital beam should leave a **ribbon**: a camera-facing triangle strip built from the
  last N positions, with UVs scrolling along its length, tapered width, and alpha fading with age.
- Use a noise texture with UV scrolling for plasma, and a hard core plus soft halo (two strips) for lasers.
- For the Ion Scythe, Halo Cutter and Dyson Lance, a **glowing cut line** left on the ground that cools from white to
  orange to dark red over about 10 seconds (emissive decals, §10) is far more striking than the beam alone.

## 13. Better lightning

- **Shape:** recursive midpoint displacement, giving a main bolt plus 3–6 branches whose brightness falls with depth.
  Re-randomise 2–3 times in about 8 frames (the real "flicker").
- **Render:** a white core strip plus a blue-violet halo strip (additive), plus emissive bloom (§2).
- **Light:** feed the light pass (§7) with a strong, very short light, and flash the sky (`SkyTint`) for one frame.
- **Afterimage:** keep a dim violet copy for about 0.5 seconds (the retina burn).

## 14. Event skies

`BackgroundRendererMixin` and `SkyTint` already exist. Go further and draw a **custom sky dome** during big events:
- **Gradient by event:** a blood-red horizon for Bloodstar, a black sky with sharp stars for The Sun Disappears, a
  green aurora for Aurora.
- **Aurora curtains:** ribbons with vertical noise, gently waving, additive.
- **Planetary bodies** (Moonfall, Omega Planet Cracker): a big lit sphere with a terminator (the day/night line),
  atmospheric rim glow and craters taken from a texture, growing over minutes.
- **Clouds parting** (Galactic Smash, Godfall): a hole cut in a cloud layer that spreads out from the centre, with
  light shafts (god rays) through it.

## 15. Depth of field and lens flares

**Depth of field** (cutscenes only):
- The circle of confusion comes from linear depth versus the focus distance.
- Blur with a 16–24-tap disc, weighted by CoC. A hexagonal pattern gives anamorphic or hex bokeh on bright points.
- **Rack focus:** shift focus from the near foreground to the explosion as the cutscene starts. It feels very
  cinematic.

**Lens flares:**
- From the main light's screen position, draw 4–6 ghost discs along the line through the screen centre, plus an
  anamorphic streak and a halo ring. Tint them by the event colour.
- **Occlusion:** sample depth around the light's screen position with a few taps. A building in the way should hide
  the flare.

## 16. Per-bomb colour grade with LUTs

- Ship a **3D LUT as a 2D strip PNG** (16 tiles of 16×16, which is 256×16) for each mood:
  - nuclear: teal shadows, orange highlights;
  - cryo: blue and desaturated;
  - void: crushed violet;
  - divine: gold and bloom-friendly.
- Blend from the neutral LUT to the event LUT by `CineGrade`. Colourists can then edit the look in any image editor
  (grade a screenshot, then apply the same change to the LUT PNG).

## 17. Noise toolkit

- **Blue noise** (a 64×64 PNG): use it for raymarch jitter and dithering away gradient banding in the dark fades. No
  more stepped black.
- **Tileable 3D noise:** upload a 64³ Worley-plus-Perlin texture once with `GL12.glTexImage3D`. Bind it by raw GL ID
  in our own passes. It's much faster than computing fbm in the shader for the raymarcher.
- **A curl-noise texture** for swirling particle motion (smoke vortices, the Deadly Twister). Sample it on the CPU in
  `WmdParticle.tick` from a small baked array.

## 18. Time effects

These come from VFX Weaver's list of effects; we don't have them yet:
- **Afterimage / echo:** a ring buffer of 4 frames blended with decreasing weight. Use it for Chrono Break and the
  Unmaker.
- **Stop-motion:** hold every second or third frame for a set time. A jarring "reality is broken" cue (Glitch, 16
  Chunks).
- **Slice shift:** split the screen into horizontal bands and shift each one sideways (Reality Shear).
- **Speed lines:** radial streaks toward the impact (the Rods from God fall, the Titanfall drop).
- **Iris wipe / eyelids:** a cutscene transition, blinking in and out of "knocked-out" vision after a close blast.

---

## Sources
- [Bjørge, "Bandwidth-efficient rendering" (SIGGRAPH 2015) notes](https://community.arm.com:443/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-20-66/siggraph2015_2D00_mmg_2D00_marius_2D00_notes.pdf) and [slides](https://community.arm.com:443/cfs-file/__key/communityserver-blogs-components-weblogfiles/00-00-00-20-66/siggraph2015_2D00_mmg_2D00_marius_2D00_slides.pdf)
- [Dual Kawase write-up](https://blog.frost.kiwi/dual-kawase/), [13-tap and tent bloom shader (kiss3d)](https://docs.rs/crate/kiss3d/0.45.1/source/src/builtin/hdr_bloom.wgsl), [Unity URP bloom filter modes](https://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@17.3/api/UnityEngine.Rendering.Universal.BloomFilterMode.html)
- [Godot: reconstructing world position from depth](https://docs.godotengine.org/en/stable/tutorials/shaders/advanced_postprocessing.html)
- [Volumetric plasma flame (Quilez, Godot port)](https://godotshaders.com/shader/volumetric-plasma-flame/), [Maxime Heckel: volumetric raymarching](https://blog.maximeheckel.com/posts/real-time-cloudscapes-with-volumetric-raymarching/), [Volumetric rendering part 1](https://wallisc.github.io/rendering/2020/05/02/Volumetric-Rendering-Part-1.html), [real-time volumetric explosions paper (2025)](https://lgm.fri.uni-lj.si/wp-content/uploads/2025/09/244517635.pdf), [NVIDIA "Bigger bang with fewer sprites"](https://developer.download.nvidia.com/SDK/9.5/Samples/DEMOS/OpenGL/src/cg_explosion/docs/Bigger%20bang%20with%20fewer%20sprites.pdf)
- [Valve: deferred lighting](https://developer.valvesoftware.com/wiki/Deferred_lighting), [particles as deferred lights](https://developer.valvesoftware.com/wiki/Particle_System_Operators/Render_Deferred_Light)
- [VFX Weaver effect list](https://modrinth.com/project/HBhB8m4j), [Cascade feature list](https://modrinth.com/mod/cascademc)
