# Libraries and tools for 1.21.1 Fabric

These notes come from web research in November 2026. Versions change often, so check each project's page before
adding a dependency.

**Rule of thumb:** our pipeline is hand-written and already works. A library is only worth it if it gives us
something we can't build in about a week, *and* it doesn't break Sodium or Iris for players. Every hard dependency
also means players have to install another jar.

## Rendering frameworks

### Veil (FoundryMC) — the strongest candidate
- Repo: https://github.com/FoundryMC/Veil, Modrinth: https://modrinth.com/mod/veil
- **Versions:** Fabric builds exist for 1.21.1. Veil 4.4.1 for Fabric 1.21.1 was listed in July 2026.
- **What it gives us:**
  - JSON-driven custom framebuffers;
  - a post-processing pipeline with easy texture binding;
  - a full GLSL shader pipeline with hot reload;
  - a deferred renderer with dynamic lights (point and area lights): `VeilDeferredRenderer`, see the
    [API docs](https://foundrymc.github.io/Veil/foundry/veil/api/client/render/deferred/VeilDeferredRenderer.html);
  - data-driven "Quasar" particles;
  - ImGui-based debug editors in game.
- **Who uses it:** Ladysnake's *Effective* has been moving off Lodestone and onto Veil for 1.21. See its
  [changelog](https://modrinth.com/mod/effective/changelog).
- **What we'd gain:**
  - real coloured point lights from fireballs, plasma bolts and lightning, which is the single biggest thing we can't
    easily do ourselves;
  - named framebuffers instead of our hand-rolled `SimpleFramebuffer` copies in `Vision.java`;
  - live shader reloading, which would make tuning far faster.
- **Costs and risks:**
  - It's a hard dependency for every player.
  - The deferred path has its own `isSupported()` check and can be off on some systems.
  - Interaction with Iris shader packs needs testing; an "Iris Veil compat" mod exists, which suggests there are
    rough edges.
  - It's a large API to learn.
- **Verdict:** prototype it on a branch, starting with its light system only. If the lights look as good as hoped,
  keep it as an optional "enhanced lighting" module that activates only when Veil is present (`FabricLoader.isModLoaded("veil")`).

### Satin API (Ladysnake)
- Repo: https://github.com/Ladysnake/Satin
- **Versions:** 2.0.0 covers 1.21. The 3.0 alpha targets 1.21.2–1.21.4 and moved the post-effect paths.
- **What it gives us:**
  - `ManagedShaderEffect`, which reloads automatically on resize;
  - simple uniform setters;
  - render targets in RGBA16F and RGBA32F, which is important for HDR bloom.
- **Verdict:** it's a small, low-risk helper. That said, we already handle resize and uniforms ourselves in `PostFx`.
  The **HDR target formats** are the only part we really want, and we can copy the idea by creating our own
  `GL_RGBA16F` framebuffer. **No need to depend on it.**

### Lodestone (Lodestar team)
- CurseForge has a Fabric port: https://www.curseforge.com/minecraft/mc-mods/lodestone-fabric-port.
  Modrinth: https://modrinth.com/mod/lodestonelib
- **What it gives us:** screenshake, a particle system with lots of per-particle settings, and world VFX helpers.
- **Verdict:** the official line is mostly Forge and NeoForge, and Effective left it for Veil. **Skip it.** Our
  `WmdParticle` and `ClientFx.shake` already cover this.

### Cascade (cascademc)
- Modrinth: https://modrinth.com/mod/cascademc. It is MIT-licensed, supports Fabric and NeoForge, and has 1.21.1
  builds.
- **What it gives us:**
  - effects sent from server code to nearby clients, with no entities created;
  - composable timelines with delays and parallel branches;
  - particle emitters, beams and volumes;
  - **raymarched signed-distance volumes**;
  - dynamic light casting;
  - **selective bloom that only affects Cascade effects**;
  - HUD-layer particles;
  - effects that can be defined in datapack JSON.
- **Verdict:** this is very close to what our `Vfx` packets and `Climax` and `Cinematics` timelines already do.
  - It's worth reading its source for ideas on **selective bloom** and **SDF volumes**.
  - Adopting it would mean rewriting a lot of working code for little gain.
  - **Borrow ideas, not the dependency.**

### Flywheel (Engine Room, used by Create)
- **What it gives us:** GPU instancing, so 1,000 copies cost one draw call instead of 1,000.
- **Catch:** its instancing turns off when a shader pack is active, unless the player installs a separate compat mod.
- **Verdict:** this only matters if we start rendering thousands of falling blocks as real models. We can get most of
  the benefit ourselves by batching into one `BufferBuilder` per layer, which `FxBuffers` already does. **Skip it.**

## Particle and VFX authoring tools

### Photon (KilaBash), plus its unofficial Fabric 1.21.1 port
- Official (NeoForge 1.21.1): https://modrinth.com/project/photon-editor. The Fabric port is ELBGG/Photon on GitHub.
- **What it gives us:** a Unity-style VFX editor in game (`/photon_editor`). It has node-based emitters, curves and
  materials.
- **Licence:** **CC BY-NC-SA 4.0.** The non-commercial and share-alike terms carry over to anything that depends on
  it, so be careful.
- **Verdict:** use it as a **design sandbox**: rough out an effect in its editor, then port the curves and timings by
  hand into our `WmdParticle` presets. Don't ship it as a dependency.

### AAA Particles (an Effekseer loader)
- Modrinth: https://modrinth.com/mod/aaa-particles. Supports Fabric 1.21.1.
- **What it gives us:** it plays `.efkefc` effects made in the free **Effekseer** editor (https://effekseer.github.io),
  which is a mature tool used in many commercial games. It supports ribbons, trails, distortion, meshes and GPU
  particles.
- **Verdict:** this is the fastest route to effects that look commercial, such as sword trails and magic circles.
  - It's a dependency, and it renders outside our post pipeline.
  - Worth a spike for the **weapons**, like muzzle flashes and plasma trails, where the effects are small and
    reusable.

### Shader DevTools
- Modrinth: https://modrinth.com/project/shader-devtools
- **What it gives us:** it reloads core and post shaders without restarting the game. It's a **dev-only** tool.
- **Verdict:** add it as a `modLocalRuntime` dependency so it runs in dev but never ships. It would make tuning the
  GLSL in `armageddon_fx.fsh` and `armageddon_vision.fsh` many times faster.

### VFX Weaver API
- Modrinth: https://modrinth.com/project/HBhB8m4j. It targets Minecraft **26.1**, so it's the wrong version for us.
- **What it gives us:** its catalogue reads like a checklist of post effects to copy: bloom, film grain, scanlines,
  depth of field, letterbox, invert, vortex, speed lines, slice shift, noise warp, solarize, double vision, eyelids,
  iris wipe, digital glitch, VHS, shockwave, afterimage, stop motion.
- **Verdict:** use it as a **reference only**. The ones we lack and want are depth of field, speed lines, slice shift,
  afterimage and stop motion.

### Similar mods worth playing for reference
- **Effective** (splashes, cascades, soft particles), **Subtle Effects**, **Particle Rain**.
- **Mas Effects**: a mace smash shockwave on Fabric 1.21.x.
- **Savapeter Cinematic Explosions**: a ground shockwave on explosions, NeoForge 1.21.1.

## Asset sources

| Source | What | Licence | Use |
|---|---|---|---|
| Unity Labs free VFX flipbooks — https://unity.com/en/blog/engine-platform/free-vfx-image-sequences-flipbooks | Explosions and smoke, simulated in Houdini, as frame sequences or packed sheets | **CC0** | Our new smoke, fire and explosion flipbooks. Downscale to 64 or 128 px frames to fit the pixel style |
| EmberGen (JangaFX) — https://jangafx.com/software/embergen | A real-time fluid sim that exports EXR, PNG or TGA flipbooks | Commercial tool with a trial; check the EULA for exports | Making custom flipbooks: mushroom-cloud stems, plasma fire |
| The itch.io "fire, magic and explosion flipbooks" Lite pack (6 free effects) | EmberGen-made sheets | **Licence not stated**, ask first | Only once the licence is confirmed |
| Effekseer sample effects | `.efkefc` effects | Check each sample | Only with AAA Particles |

**Pixel-art note:** realistic flipbooks clash with Minecraft's look. Use them at low resolution with point
filtering, posterize the colour ramp to 6–8 steps, and add our three-stop colour ramp on top. That way the result
reads as "Minecraft, but cinematic" rather than "a photo pasted in".

## Iris and Sodium compatibility

- Iris often ignores or replaces custom `RenderLayer`s. Our `FxBuffers.shaderPack()` adapter already detects shader
  packs. Keep every effect usable on that path.
- One reported fix for an Iris incompatibility (in the Lighty mod) was moving to **custom render pipelines** instead
  of vanilla ones. That's a 1.21.5+ concept, and noted for when we port.
- Iris replaces the vanilla post chain, so **our `PostEffectProcessor` passes still run on top of it**. That's good:
  post effects are the most shader-pack-proof tool we have. Put "wow" in post where we can.
- Flywheel instancing turns off under shader packs, which is one more reason not to depend on it.

## Sources
- [Veil GitHub](https://github.com/FoundryMC/Veil), [Veil on Modrinth](https://modrinth.com/mod/veil), [VeilDeferredRenderer docs](https://foundrymc.github.io/Veil/foundry/veil/api/client/render/deferred/VeilDeferredRenderer.html)
- [Satin GitHub](https://github.com/Ladysnake/Satin), [Satin changelog](https://modrinth.com/mod/satin-api/changelog)
- [Lodestone Fabric port](https://www.curseforge.com/minecraft/mc-mods/lodestone-fabric-port), [Effective changelog](https://modrinth.com/mod/effective/changelog)
- [Cascade](https://modrinth.com/mod/cascademc)
- [Flywheel explainer](https://djefrey.github.io/colorwheel/whatis/flywheel), [Iris Flywheel compat](https://modrinth.com/mod/iris-flw-compat)
- [Photon editor](https://modrinth.com/project/photon-editor), [Photon Fabric port](https://modrinth.com/project/uY7AsRR7), [AAA Particles](https://modrinth.com/mod/aaa-particles/changelog)
- [Shader DevTools](https://modrinth.com/project/shader-devtools), [VFX Weaver](https://modrinth.com/project/HBhB8m4j)
- [Iris custom render layer issue](https://github.com/IrisShaders/Iris/issues/3208), [Lighty Iris fix](https://modrinth.com/mod/lighty/version/3.0.2+1.21.6?dep=1)
- [Unity CC0 flipbooks](https://unity.com/en/blog/engine-platform/free-vfx-image-sequences-flipbooks), [EmberGen](https://jangafx.com/software/embergen), [itch.io flipbook pack thread](https://forum.godotengine.org/t/fire-magic-and-explosion-flipbooks-png-sprite-sheets-free-lite-for-godot-4-and-other-2d-engines/145612)
