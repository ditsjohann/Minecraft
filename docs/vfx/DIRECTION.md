# Art direction: what makes an effect "wow"

Better rendering only gets us halfway. The other half is timing, contrast and restraint. This page covers the rules
real-time VFX artists use, adapted to Armageddon.

## 1. The three beats: anticipation, impact, follow-through

Every effect, from a frag grenade to Galactic Smash, should have three beats:

| Beat | Length | What happens |
|---|---|---|
| **Anticipation** | Small: 2–6 frames. Big: 1–4 s | Energy gathers: particles suck *in*, the light dims, the sound drops out, the FOV narrows slightly, the world desaturates |
| **Impact** | 2–6 frames | The brightest and hardest frame: flash, impact frame, hit-stop, chromatic kick, the sound transient. The *only* moment at full intensity |
| **Follow-through** | 5–60 s for cataclysms | A slow decay: the fireball rises and darkens, the smoke rolls out, the dust settles, embers fall, and the light fades over a long tail |

Most of what feels "cheap" comes from beats that run together: the flash and the smoke start at the same time and
fade at the same speed. **Make the impact short and the follow-through long.**

A common rule for small hits (from game-feel guides):
- anticipation: 0–3 frames, scaling up from nothing;
- main action: 3–6 frames;
- follow-through: 6–12 frames, fading while stretching.

Scale the same proportions up for bombs.

## 2. Contrast beats brightness

- **Darken before you brighten.** In the half second before the flash, pull exposure down 10–20% and lower the
  saturation. The flash then looks twice as bright without being any brighter.
- The **glare limiter** must stay. A full-white screen for half a second looks like a bug, not power. Bright *areas*
  next to dark areas read as powerful; brightness everywhere does not.
- **Value hierarchy:** the hottest core is near white with a slight tint, the fire is saturated orange, the smoke is
  dark. Never make the smoke as bright as the fire.
- **Complementary accents:** an orange fireball against a teal-graded sky, a violet void against warm terrain, a cyan
  plasma against dusk.

## 3. Colour language for each family

Keep a palette for each family so players can read the danger from the colour alone:

| Family | Core | Body | Smoke / afterglow | Grade |
|---|---|---|---|---|
| Nuclear / thermal | `#FFF6E0` | `#FF8A1E` → `#C2301A` | `#2B201C` | Teal shadows, warm highlights |
| Plasma / sci-fi | `#F0FFFF` | `#40E0FF` / `#B080FF` | `#102040` violet haze | Cool, high contrast |
| Void / gravity | `#000000` core | `#7A2CFF` rim | Ink-black wisps | Crushed and desaturated, violet lift |
| Cryo | `#FFFFFF` | `#A8E8FF` | Pale blue fog | Blue, low saturation |
| Divine / solar | `#FFFFF0` | `#FFD040` | Gold dust | Warm, soft bloom |
| Nature / spore | `#F0FFD0` | `#70E040` | Green spores | Saturated greens |
| Glitch / reality | `#FFFFFF` | RGB-split primaries | Pixel blocks | Posterized |

## 4. Scale cues

Huge effects only look huge next to something of known size:
- **Distant columns:** for city and world bombs, spawn secondary smoke columns at 100–300 blocks, so the horizon is on
  fire too.
- **Things that move through the frame:** dust waves rolling over trees, leaves stripped off, birds and bats
  scattering, small mobs flung.
- **Delay by distance:** the flash arrives instantly, but the sound and shockwave arrive at about 1 second per 340
  blocks. A cataclysm seen from far away should *flash, then go silent, then hit.* This is the most cinematic trick
  there is, and it costs nothing.
- **Atmospheric perspective:** effects far away should be bluer and lower in contrast, and they should be partly
  hidden by fog. Near effects should be saturated and sharp.
- **Slow motion of big things:** huge clouds should rise *slowly*. Fast motion reads as small.

## 5. Camera language

- **Shake has character:**
  - **Impact shake:** high frequency, short, with a strong first kick.
  - **Rumble:** low frequency and long, for tectonic and Moonfall events.
  - **Roll:** a slight camera tilt, ±2°, which feels like being knocked off balance.
  - Use Perlin noise, not random jitter, and always decay with an ease-out.
- **FOV punch:** narrow the FOV slightly during anticipation, widen it on impact, then ease back.
- **Hit-stop:** freeze the camera and any nearby effects for 2–4 frames on an impact. Use it on weapon finishers,
  not on every grenade.
- **In cutscenes:**
  - Use a slow push-in during anticipation, a hard cut or whip-pan on impact, then a **wide, slow pull-back** for the
    follow-through so the scale lands.
  - Use real lens choices: wide (FOV about 90) for scale, long (FOV about 30) for compression shots of a distant
    cloud over the town.
  - Use rack focus with depth of field.
- **Never take control without a reason.** The rule we already set holds: only World Destruction gets cutscenes;
  everything else gets letterbox bars at most.

## 6. Sound sync

Visuals and sound have to land on the same frame:
- The flash frame equals the transient of the explosion sound.
- The shockwave reaching the camera triggers the shake, the FOV punch, the dust and the bass hit, all on one tick.
- Leave **a moment of silence** before the biggest hits (a sound duck plus the world desaturating). Silence is
  anticipation.
- Layer the sound: a near crack, a mid boom, a far rumble, then a long tail of debris. Pick the layers by the
  player's distance.

## 7. Readability and restraint

- **One hero per moment.** If the lightning, the shockwave, the beam and the mushroom cloud all peak together, none
  of them reads. Stagger them by 5–20 ticks.
- **Cap the particle counts.** A few hundred big, well-lit, well-shaped particles look better than ten thousand tiny
  ones.
- **Silhouette first.** Check every effect in greyscale. It should still read from its shape alone.
- **The aftermath is part of the effect.** Scorch decals, embers, cooling lava, drifting ash and a dimmed sky for a
  minute afterwards make the event feel like it *happened*.

## 8. A checklist for every effect

- [ ] Does it have three clear beats?
- [ ] Is the impact shorter than about 6 frames, and the tail at least 5× longer?
- [ ] Is there one hero element at any moment?
- [ ] Does it light its surroundings (a light pass, or at least the sky tint)?
- [ ] Does the smoke look lit from inside or below?
- [ ] Is the sound synced, with delay by distance?
- [ ] Does it still read in greyscale?
- [ ] Is there an aftermath that lasts at least 30 seconds?
- [ ] Does it stay under its GPU budget on the Medium preset? (See [PERFORMANCE.md](PERFORMANCE.md).)
- [ ] Does it still work with Sodium, and with an Iris shader pack?

## Sources
- [GDC: Real-Time VFX, a visual language spectrum (Sarah Grissom, FXVille)](https://gdcvault.com/play/1025230/Real-Time-VFX-A-Visual)
- [VFX as game design tools: God of War Ragnarök and Halo Infinite](https://gamedeveloper.com/design/vfx-as-game-design-tools-the-ludology-of-vfx-in-god-of-war-ragnarok-and-halo-infinite)
- [How great game explosions are made (PC Gamer)](https://www.pcgamer.com/uk/how-great-game-explosions-are-made/)
- [Real-time VFX overview, Mederic Chasse (80.lv)](https://80.lv/articles/real-time-vfx-overview-from-mederic-chasse/), [VFX for games explained (80.lv)](https://80.lv/articles/vfx-for-games-explained/)
- [Combat feel and juice resources: hitstop, screenshake, VFX timing](https://gamineai.com/resources/20-free-combat-feel-and-juice-resources-hitstop-screenshake-vfx-timing-2026-edition)
- [Game feel / juice survey (arXiv 2011.09201)](https://arxiv.org/pdf/2011.09201)
- [Impact explosion study with EmberGen (80.lv)](https://80.lv/articles/impact-explosion-vfx-study-powered-by-jangafx-s-embergen)
