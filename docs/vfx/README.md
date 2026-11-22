# Armageddon Arsenal: visual effects research

These notes cover how to make the mod's visuals much better than they are now, so that a detonation looks like a
film shot and not just "Minecraft with more particles".

Everything here is aimed at **Minecraft 1.21.1 on Fabric (Loader 0.19.x, Fabric API 0.116.x)**, the versions this
mod builds against. None of it has been implemented yet. Each idea is tagged by effort and risk, so we can pick
things off one at a time.

| File | What's in it |
|---|---|
| [LIBRARIES.md](LIBRARIES.md) | Rendering and VFX libraries that exist for 1.21.1 Fabric, what each would give us, and whether it's worth taking on |
| [TECHNIQUES.md](TECHNIQUES.md) | Rendering techniques we can build ourselves on top of the current `PostFx`, `Vision` and `FxBuffers` pipeline, with GLSL and Java sketches |
| [DIRECTION.md](DIRECTION.md) | Art direction: timing, colour, contrast, camera, sound sync, and what makes an effect read as "wow" rather than as noise |
| [BOMB_IDEAS.md](BOMB_IDEAS.md) | Concrete upgrades for each tier and each bomb, built from the techniques above |
| [PERFORMANCE.md](PERFORMANCE.md) | Frame budget, quality presets, Sodium/Iris compatibility, and a testing checklist |

## Where we are today

The mod already has more rendering infrastructure than most Minecraft mods. Anything new should build on it:

| System | File | What it does now |
|---|---|---|
| Main post pass | `client/fx/PostFx.java`, `shaders/program/armageddon_fx.fsh` | Flash and white-out, heat haze, chromatic aberration, screen-space god rays, black-hole lensing, ripple, radiation vision, the cutscene film look (bloom, anamorphic streaks, ACES tone curve, grain, vignette), and the glare limiter |
| Depth-aware vision pass | `client/fx/Vision.java`, `shaders/program/armageddon_vision.fsh` | Copies colour and depth, then rebuilds world position through `InvViewProj`. Modes: LIDAR, VOID, ECLIPSE, SONAR, PIXEL, COMIC, COMPOUND |
| Geometry FX | `client/render/FxRenderLayers.java`, `FxBuffers.java`, `FxDraw.java`, `Solids.java` | Additive glow layers (built on `LIGHTNING_PROGRAM`), shade, void, solid and lit variants, plus a shader-pack adapter (`FxBuffers.shaderPack()`) |
| Particles | `client/particle/WmdParticle.java` | A data-driven billboard with a three-stop colour ramp, a size curve, an emissive phase and sprite animation. There are 21 particle sprites |
| Camera | `mixin/CameraMixin.java`, `GameRendererMixin.java`, `client/fx/Cinematics.java` | Shake, FOV punch, letterbox, cutscene camera paths, and the hand hidden during cutscenes |
| Client effect kinds | `ClientFx`, `SciFx`, `CityFx`, `Celestial*Fx`, and others | Code-driven effects for each bomb, sent from the server through `Vfx` packets |

**What is missing**:
- Everything shares one lighting model. Explosions don't actually *light* the world around them; the sky tint and
  `lightStrength` are global.
- Bloom is a single full-screen pass, not a mip chain, so it can't produce the soft halo that spans a whole sky.
- There is no real volumetric smoke or fire. Smoke is made of billboards and shells.
- There are no soft particles, so billboards clip hard into terrain.
- There is no proper motion blur or shutter feel on big impacts.
- There are no quality presets, so effects either run at full cost or not at all.
- There is no in-game timeline or editor. Every effect is tuned by recompiling.

## Recommended order of work

Effort is S (a day), M (a few days) or L (a week or more).

1. **Mip-chain bloom (dual filter)** — S, low risk.
   It is the biggest jump in "expensive look" per hour of work. Everything bright gets a real halo.
   See [TECHNIQUES.md §1](TECHNIQUES.md#1-real-bloom-mip-chain-dual-filter).
2. **Depth-aware shockwave refraction plus ground-hugging dust rings** — S/M.
   The ring of distorted air racing across the land is the classic "a nuke just went off" shot.
   See §3.
3. **Soft particles plus a better flipbook set (CC0 Houdini explosion sheets)** — M.
   Smoke stops looking like cards stuck in the ground. See §5 and
   [LIBRARIES.md, asset sources](LIBRARIES.md#asset-sources).
4. **Raymarched fireball and mushroom-cloud volume in the vision pass** — L, high payoff.
   This is a real 3D cloud that lights itself from inside and casts a soft shadow on the terrain. See §6.
5. **Point lights from explosions (our own lightmap boost, or Veil's light system)** — M/L.
   The street goes orange when a bomb lands. See §7 and the Veil notes.
6. **Impact language: hit-stop, a two-frame impact frame, chromatic kick, sub-bass camera roll** — S.
   This costs almost nothing and is mostly timing. See [DIRECTION.md](DIRECTION.md).
7. **Quality presets plus GPU-time guards** — S, required before shipping 4–6.
   See [PERFORMANCE.md](PERFORMANCE.md).
8. **Optional: move to Veil for framebuffers and lights**, or bring in Cascade for authoring timelines.
   This is a big dependency decision, so read [LIBRARIES.md](LIBRARIES.md) first.

## Golden rules (short version)

- **Contrast beats brightness.** Dark first, then bright. Pull the world down by 10–20% just before the flash.
- **One hero, one read.** Each moment should have a single focal element. Everything else supports it.
- **Anticipation, impact, follow-through.** Give a short wind-up, hit it hard for two to four frames, then a long, slow
  decay.
- **Scale cues.** Huge things need reference: distant smoke columns, dust rolling over trees, and the shockwave
  arriving late.
- **Sound sync is half the visual.** The flash lands on the transient, and the shockwave arrives with a delay set by
  distance (about 1 second per 340 blocks).
