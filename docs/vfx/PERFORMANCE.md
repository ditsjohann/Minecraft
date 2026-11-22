# Performance, quality presets and compatibility

Nothing in [TECHNIQUES.md](TECHNIQUES.md) ships without a quality switch. These are the rules.

## Frame budget (target: 60 fps on a mid-range GPU at 1080p)

| Budget slot | Medium preset | Notes |
|---|---|---|
| Main post pass (`armageddon_fx`) | ≤ 0.5 ms | Already in place |
| Bloom mip chain | ≤ 0.4 ms | Runs at 1/2 resolution and below, so it's cheap |
| Vision pass with shockwave and light pass | ≤ 0.8 ms | 8 lights, 8-step contact shadows |
| Raymarched volume | ≤ 1.5 ms | Half resolution, 32 steps, temporal blend |
| FX geometry and particles | ≤ 2 ms | Watch the particle count, not just the draw calls |
| Debris | ≤ 1 ms | Capped by count |

## Quality presets

Add a client config value, `vfxQuality`: `LOW` / `MEDIUM` / `HIGH` / `ULTRA`. Every system reads from it.

| Feature | LOW | MEDIUM | HIGH | ULTRA |
|---|---|---|---|---|
| Bloom | Off (current single pass) | 4 mips | 6 mips, HDR | 6 mips, HDR, selective |
| Soft particles | Off | On | On | On |
| Flipbook frames | 16 | 64 | 64 with frame blend | 64 with frame blend |
| Raymarched volumes | Billboards | 24 steps, 1/2 res | 32 steps, 1/2 res | 48 steps, full res |
| Light pass | Global tint only | 4 lights | 8 lights with shadows | 8 lights with shadows |
| Debris | 50 | 200 | 400 | 600 |
| Decals | 4 | 8 | 16 | 16 |
| Depth of field / motion blur in cutscenes | Off | Off | On | On |

**Automatic guard:**
- Measure the frame time for each effect with `GL_TIME_ELAPSED` queries, or the CPU time of
  `FxWorldRenderer` as a cheaper proxy.
- If the average frame time stays above 22 ms for 2 seconds during an event, drop one preset level until the event
  ends.

## Sodium

- Sodium replaces terrain rendering but leaves `WorldRenderEvents`, entity and particle rendering, and
  `PostEffectProcessor` alone. Our FX pipeline should work as it does now.
- **Watch for:** mixins into `WorldRenderer` internals. A Fabric 1.21.1 report shows a crash when changing block
  render layers *without* Sodium, so test both with and without it.

## Iris shader packs

- `FxBuffers.shaderPack()` already detects an active pack. Keep a **pack-safe path** for every new feature:
  - **Post passes (bloom, vision, light pass):** still run after Iris's own composite. Make them **gentler** when a
    pack is on, because most packs already add bloom and tone-mapping. Use half the bloom strength and skip our ACES.
  - **Custom core shaders (soft particles):** Iris may not run them. Fall back to the vanilla particle sheet.
  - **Depth:** the depth we copy is the pack's depth. It's still valid for world-position reconstruction, but test it.
- For a compile-time-safe check, use the Iris API as an optional dependency. Otherwise keep the current reflection or
  detection approach. (The exact API method name wasn't confirmed in this research; check the Iris source.)

## Testing checklist (before each VFX merge)

- [ ] Vanilla, Sodium only, Sodium plus Iris (Complementary or BSL pack), and Sodium plus Iris with shaders off.
- [ ] LOW and ULTRA presets.
- [ ] Resize the window mid-effect (framebuffers re-created, no black screen).
- [ ] F1 (hidden HUD), F5 (third person), and spectator mode.
- [ ] Two effects at once (for example a nuke plus Tesla Dominion) for stacking and uniform clashes.
- [ ] An effect off screen or behind the camera (no stray flares or lensing).
- [ ] The preview harness (`src/preview`, `preview.txt`) renders frames for the new effect, and the CI preview job
  publishes them.

## Dev workflow tips

- Add **Shader DevTools** as a `modLocalRuntime` dependency. It hot-reloads `.fsh` and `.vsh` files without a game
  restart. It's dev only and never shipped.
- Expose effect tunables (bloom weights, wave speed, step count) as a debug `/armageddon vfx set <key> <value>`
  command in dev builds. Tune live, then copy the numbers into code.
- Record reference clips of real footage (the Trinity test, Castle Bravo, volcano plumes, lightning in slow motion)
  next to our captures. Compare the timing frame by frame.
