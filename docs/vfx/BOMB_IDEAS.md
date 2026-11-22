# Upgrade ideas for each tier and bomb

These ideas build on [TECHNIQUES.md](TECHNIQUES.md) (§ numbers refer to it). They're grouped by the current tiers. The
higher the tier, the more of the expensive techniques (raymarched volumes, the light pass, cutscene depth of field)
it earns.

## Shared upgrades (every explosive)

These are a cheap, global step up, done once in shared code:
- **Light pass flash (§7):** every explosion adds a short coloured point light sized to its power. Grenades get 0.2 s,
  nukes get 6 s with a slow fade.
- **Soft particles plus 64-frame flipbooks (§5):** all smoke and fire.
- **Scorch decals (§10):** every crater gets a fading burn ring.
- **Client debris (§11):** the nearest blocks removed fly out as tumbling chunks, capped per preset.
- **Delayed shockwave (DIRECTION §4):** sound and shake arrive based on distance.

## FUN

- **Disco Ball / Disco Bomb:**
  - Real moving light spots (§7 with 6–8 coloured lights orbiting).
  - A volumetric beam per spot (a raymarched cone at low resolution, or additive cones with soft particles).
  - Mirror sparkles on the bloom.
- **Glitter, Party Popper, Rainbow Paint:**
  - Confetti as small tumbling quads with a sparkle when they turn edge-on to the sun (a fresnel).
  - Ribbons for streamers (§12).
- **Snow Globe:** a refractive dome edge (shockwave §3 with a fixed radius) and slow-falling snow inside it.
- **Hologram:** scanlines, a flicker, a chromatic split, and an emissive-only bloom (§2) on cyan wireframes.
- **Airhorn Cannon:** a visible sound cone, with rings distorting air along the cone (screen-space refraction with
  lines).

## UNCOMMON / RARE (grenades, small bombs, most guns)

- **Muzzle flashes:**
  - 1–2 frame star flashes with a random rotation;
  - a light pass pulse;
  - a tiny smoke puff;
  - shell casings as client debris.
- **Plasma weapons:** ribbon trails, a hot core plus halo, and impact splashes that leave small emissive decals
  cooling on the ground.
- **Cryo Grenade / Cryo Cannon / Shatterfrost:**
  - frost decals spreading over terrain (§10 with a crystal texture);
  - a refraction "ice lens" shimmer;
  - the shatter as glassy debris catching the light.
- **Thermite:** dripping, white-hot sparks bouncing (collide on), a strong local light that flickers, molten emissive
  decals.
- **Tesla Bomb / Arc Thrower / Arcflash:** branching lightning (§13) that hops between entities and conductive
  blocks, plus a light pulse per arc.
- **Sinkhole:** dust falling *into* the hole (particles pulled down), and the rim crumbling as client debris.
- **Mirror Dimension Bomb:** a screen-space reflection flip inside the radius, using the vision pass (mirror the
  world position across a plane).

## EPIC

- **Singularity / Micro Event Horizon:**
  - Keep the lensing.
  - Add an accretion disk with Doppler beaming: one side brighter and bluer, the other dimmer and redder.
  - Add a photon ring.
  - Pull in debris (§11) that stretches as it falls in ("spaghettification").
- **Volcano:**
  - A raymarched ash column (§6 with grey smoke and a red base glow).
  - Lava bombs as emissive debris with smoke trails.
  - Lightning in the ash cloud (§13).
  - Lava decals that cool over time.
- **Tsunami:**
  - A wave wall made of a scrolling, displaced water strip with foam particles on the crest.
  - Mist (soft particles).
  - A refraction band at the front.
- **Bass Drop / WUB-9000:**
  - Pulse every effect to the beat: bloom strength, ring shockwaves (§3) on each drop, FOV pumps.
  - Speed lines (§18) on the drop.
- **Gauss Railgun / Antimatter Lance:**
  - A hard core beam plus a halo, an air-ionisation trail that lingers and slowly bends with noise.
  - An impact frame (§9) on a kill.
- **Crystal Cataclysm / Geode:** crystals as glassy quads with fresnel rims and refraction behind them; bloom on the
  edges.

## LEGENDARY

- **Atomic / Neutron / Antimatter / Cluster Nuke (the flagship nukes):**
  1. **Anticipation (1 s):** a world dim and a sound duck.
  2. **Flash:** an exposure spike held for 2 frames, then a fireball that is a **raymarched sphere** (§6), very hot.
  3. A **Wilson condensation shell** (§3) for about 1 second.
  4. A **refraction shockwave** with a ground dust skirt racing outward.
  5. **The mushroom forms:** a raymarched stem and a rolling torus cap, lit from inside and cooling from white to
     orange to dull red to grey over 20 seconds.
  6. **Light pass:** the town is lit orange with long shadows away from the fireball (§7 screen-space shadows).
  7. **Aftermath:** falling ash particles and a dim warm sky for about 60 seconds.
  - Neutron is the same, with a blue-white palette, little fire and strong Cherenkov-blue decals.
  - Antimatter has a violet-white core and lensing at the moment of the flash.
- **Rods from God:** speed lines (§18), a white-hot streak entering from the top of the frame, a hit-stop and impact
  frame, then a dust column.
- **Orbital Strike / Oblivion Death Ray:** a beam with heat haze down its length (§4), the ground glowing and molten
  along the path, and a cooling cut line (§12).
- **Ice Age:** a frost decal front spreading for 100+ blocks, a blue grade (§16), snow that thickens over 30 s, and
  breath-fog particles.
- **Hellmouth:** a lava-glow light pass from below, an upward ember storm, a red grade, and heat haze columns.
- **Chrono Break:** afterimage (§18), desaturation, then stop-motion at the peak, then everything "snapping" back.
- **8-Bit:** the PIXEL mode already exists. Add a stepped animation (12 fps) for effects inside its radius, and
  chiptune-synced flashes.
- **Aurora:** proper aurora curtains (§14) plus a soft green light on the terrain from the sky.

## MYTHIC

- **Tsar Hydrogen:** the nuke recipe at triple scale.
  - Two-stage flash, with the second bigger.
  - A cap that touches the cloud layer and spreads into a ring (a pileus cloud).
  - A distant shot of the whole cloud above the horizon in the letterbox.
- **Bloodstar:** a blood-red sky dome (§14), a red star growing with a lens flare (§15), a red grade, and the light
  pass tinted red everywhere.
- **Last Light / Unmaker:**
  - Draining colour from the world (desaturation spreading in world space from the centre, vision pass).
  - Then erasing it to white or void, with an inked edge front (COMIC edges).
- **Godfall:** clouds parting (§14), light shafts, a divine spear falling with a ribbon trail, an impact frame, and a
  golden shockwave.
- **Big Bang:**
  - The singularity collapses to a single pixel of black (hold for 1 second of silence).
  - Then an expanding raymarched shell full of fbm colour, star particles flung out, and the lens flare at its
    strongest.
- **Colossal Spear:** keep the redesigned model; add speed lines on the fall, and a hit-stop of 4 frames on impact.
- **Ion Scythe:**
  - Already good. Add a glowing emissive spiral trench that cools over time (§10, §12).
  - On ignition, a light pass flicker along the spiral, rippling outward.
- **16 Chunks:** a stop-motion effect plus a slice shift (§18) on each chunk swap; each chunk "pops" with a short
  coloured flash at its edges.
- **Lidar Strike / Shadow Void / Spectral Ice:** the existing vision modes, with a better transition: a world-space
  sphere edge that wipes in, plus a chromatic edge.
- **Halo Cutter / Obliterator:** an impact frame on the ring closing or the beam starting; a cooling ring canyon
  decal; and debris falling into the dropped disc.

## CELESTIAL

- **Tesla Dominion:**
  - The orb as a raymarched volume with electric filament noise.
  - Every arc feeds the light pass, so the city flickers blue-white.
  - Discharge into a full-screen impact frame, then ozone-violet haze.
- **Resonance Cascade:**
  - Resonance bands visible as world-space ripples in the vision pass (concentric bright lines moving across the
    terrain).
  - Buildings shake (vertex wobble on client debris) before falling.
- **Gravity Killswitch:**
  - Floating debris at scale (§11, hundreds of chunks, slow tumble).
  - Dust hanging in the air (soft particles with zero gravity).
  - The slam: hit-stop, impact frame and a huge ground shockwave.
- **Deadly Twister:** a raymarched funnel (§6 with a swirl SDF), debris orbiting in the funnel, lightning inside.
- **UFO Invasion:**
  - Ship underside lights (light pass).
  - Tractor beams as volumetric cones with dust spirals.
  - Abduction lift with distortion.
- **Exploding Cows:** comedy timing, using a hit-stop before every pop.

## CITY DESTRUCTION

- **Moonfall:**
  - The moon grows in the sky with a lit terminator and rim (§14) over a minute.
  - The atmosphere ignites on entry (orange shell, heat haze).
  - The approach shakes the ground with a rumble.
  - Impact: a raymarched dust ejecta curtain.
- **World Eater:** a vast organic mouth. Use dark-red light from inside, debris pulled in, and a heat haze of breath.
- **Ragnarok:** fire everywhere (secondary columns at the horizon), a red sky, and lightning storms in the clouds.
- **Eternal Sapling:** letterbox only (as agreed).
  - Add spore particles with soft bloom.
  - A green light spreading over the terrain.
  - Leaves catching the light with a fresnel sparkle.
- **Main Stage:** concert light rigs as real coloured lights (§7), volumetric beams, beat-synced bloom, and
  pyrotechnic fountains.
- **Termite Domain:** millions of tiny particles are expensive. Instead, use a world-space "crawling" noise texture in
  the vision pass on surfaces being eaten, which reads as a swarm at any distance.

## WORLD DESTRUCTION (with cutscenes)

These get everything: raymarched volumes, the light pass, depth of field, LUT grading, lens flares, motion blur on
whip-pans and wide establishing shots.
- **Galactic Smash:**
  - The fist enters through parting clouds (§14).
  - A 4-frame hit-stop and black-and-white impact frame on the punch.
  - Then a planet-scale shockwave ring visible from orbit.
- **The Sun Disappears:**
  - The sun shrinks with a lens flare (§15).
  - A full eclipse grade (ECLIPSE mode).
  - The stars come out.
  - The light pass turns *off* for sun light, so only fires and torches light the world. That is creepy and
    beautiful.
- **Alien Death Ray:** an orbital beam with heat haze (§4), a ground cut that cools (§12), debris lifting into the
  beam.
- **Booming Tick:** a countdown with a pulsing red glow. Every tick: an FOV pump and a vignette pulse. The detonation
  follows the full nuke recipe at world scale.
- **Dyson Lance:**
  - A mirror swarm glinting in the sky (sparkle quads).
  - A focused beam with a blinding core.
  - Three cooling canyon cuts across the map, seen from a high drone shot.
- **Seismic Tsunami:** a rock-wave crest of client debris, a dust wall (soft particles plus raymarched dust), and a
  long rumble shake.
