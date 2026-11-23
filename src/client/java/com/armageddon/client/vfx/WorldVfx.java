package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.EMBER;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The World Destruction bombs. Each one has its own cinematic and set piece; these are the textures in the air, so
 * each reads as its own kind of end of the world.
 * <ul>
 * <li>Galactic Smash, a god's fist: molten orange inside a violet nebula, cosmic glass thrown for miles, the ground
 * ringing like a bell in glittering dust, then the giant falling apart into crystal stars.</li>
 * <li>Booming Tick, clockwork: brass sparks, a red heartbeat, chimes shedding gold, and time stopping dead with the
 * dust hanging in the air until it all lets go at once.</li>
 * <li>Alien Death Ray, sterile and alien: blue static, a column of green fire with ions spiralling up it, glassed
 * spray, bioluminescent spores, tractor beams that lift the ground away in rings.</li>
 * <li>The Sun Disappears, the light dying: a gold thread climbing to the sun, then cold, ice glitter falling in the
 * dark, a burning corpse shedding its crust, and dawn coming back in warm motes after the blast.</li>
 * <li>Dyson Lance, a focused star: glints off the mirror swarm, a cut that sprays molten rock and steam and leaves
 * cooling slag behind it, and the drill erupting.</li>
 * <li>Seismic Tsunami, the earth itself: hammer blows that throw rock and grit, and a crest of soil and stone that
 * sheds clods and dust as it rolls.</li>
 * </ul>
 */
final class WorldVfx {
	private WorldVfx() {
	}

	// Galactic Smash
	static final Gradient NEBULA = Gradient.of(0, 0xF0C8FF, 0.35F, 0xB070FF, 0.75F, 0x6A3AB0, 1, argb(0x2A1450, 0));
	static final Gradient COSMIC_GLASS = Gradient.of(0, 0xFFFFFF, 0.3F, 0xD8C0FF, 0.7F, 0x9070FF, 1, argb(0x5030A0, 0));
	static final Gradient STARDUST = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0D0FF, 1, argb(0x8070C0, 0));
	static final Gradient BELL_DUST = Gradient.of(0, 0x8A7CA0, 1, 0xA89CC0);
	// Booming Tick
	static final Gradient BRASS = Gradient.of(0, 0xFFF4C0, 0.25F, 0xFFC860, 0.6F, 0xC07820, 1, argb(0x502008, 0));
	static final Gradient GOLD = fade(0xFFE890, 0xC09020);
	static final Gradient HEART = fade(0xFF6060, 0xA01010);
	static final Gradient STILL = Gradient.of(0, 0x9A9AA2, 1, 0xB4B4BA);
	// Alien Death Ray
	static final Gradient STATIC = Gradient.of(0, 0xFFFFFF, 0.3F, 0xA0D0FF, 1, argb(0x3060FF, 0));
	static final Gradient ALIEN = Gradient.of(0, 0xFFFFFF, 0.25F, 0xC0FFE0, 0.6F, 0x40E090, 1, argb(0x104830, 0));
	static final Gradient ALIEN_HAZE = Gradient.of(0, 0xD8FFE8, 0.4F, 0x80C0A0, 1, 0x5A6A70);
	static final Gradient VIOLET_HAZE = Gradient.of(0, 0xC8A8F0, 0.5F, 0x8060B0, 1, 0x4A3A60);
	static final Gradient SPORE = Gradient.of(0, 0xC0FFD8, 0.5F, 0x60F0A0, 1, argb(0x8060FF, 0));
	static final Gradient SALT = Gradient.of(0, 0xE8ECE4, 1, 0xC0C8C0);
	// The Sun Disappears
	static final Gradient SUNLIGHT = fade(0xFFF0C0, 0xFFB040);
	static final Gradient ICE = Gradient.of(0, 0xFFFFFF, 0.5F, 0xD0E8FF, 1, argb(0x90B8E0, 0));
	static final Gradient CRUST = Gradient.of(0, 0xFFE0A0, 0.2F, 0xFF7020, 0.6F, 0x8A2A0A, 1, argb(0x200804, 0));
	static final Gradient DAWN = Gradient.of(0, 0xFFF4D0, 0.5F, 0xFFD890, 1, argb(0xFFB060, 0));
	// Dyson Lance
	static final Gradient MOLTEN = Gradient.of(0, 0xFFFFE0, 0.2F, 0xFFC050, 0.55F, 0xFF5010, 1, argb(0x501004, 0));
	static final Gradient SLAG = Gradient.of(0, 0xFFD080, 0.3F, 0xFF6010, 0.8F, 0x601808, 1, argb(0x200804, 0));
	static final Gradient STEAM = Gradient.of(0, 0xF0ECE8, 1, 0xC8C4C0);
	// Seismic Tsunami
	static final Gradient EARTH = Gradient.of(0, 0x6A5638, 0.5F, 0x8A7658, 1, 0x9A8E7A);
	static final Gradient ROCK = Gradient.of(0, 0x5A5650, 1, 0x7A7268);

	static void register() {
		galacticSmash();
		boomingTick();
		alienDeathRay();
		sunDisappears();
		dysonLance();
		seismicTsunami();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	private static EmitterDef.Builder glow(String name, float size, int life, int rgb, float alpha) {
		return EmitterDef.builder(name).burst(1).size(size, size).life(life, life).sprite(FxSprites.GLOW).color(rgb).alpha(alpha)
				.alphaOverLife(Curve.FADE_OUT);
	}

	// ------------------------------------------------------------------ Galactic Smash

	private static void galacticSmash() {
		// The fist lands: molten rock blooming inside a violet nebula, cosmic glass thrown for miles, a wall of ground.
		FxEngine.register(EffectDef.builder(VfxIds.SMASH_IMPACT).lod(400, 3200).maxAge(520)
				.cue(0, EffectDef.light(1.5F, 0xFF9050))
				.emit(flash("flash", 160, 16, 0xC090FF).build())
				.emit(glow("bloom", 220, 90, 0xA060FF, 0.45F).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(70).shape(Shape.sphere(18)).velocity(Velocity.radial(0.7F, 1.5F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.4F)).drag(0.06F).module(Module.buoyancy(0.01F)).life(40, 70).size(14, 22)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("nebula").priority(Priority.HERO).burst(64).shape(Shape.shell(30)).velocity(Velocity.radial(0.9F, 1.8F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.5F)).drag(0.035F).module(Module.curl(40, 0.01F)).life(200, 300).size(20, 32)
						.color(NEBULA).alpha(0.65F).shade(0.8F).emissive(Curve.keys(0, 1, 0.6F, 0.6F, 1, 0.25F)).build())
				.emit(EmitterDef.builder("glass").priority(Priority.HERO).burst(320).shape(Shape.sphere(14)).velocity(Velocity.up(1.6F, 3.6F, 0.85F))
						.gravity(0.05F).drag(0.004F).collide(0.3F, 0.6F, 1).life(90, 150).size(0.9F, 1.5F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.FLARE).color(COSMIC_GLASS).build())
				.emit(4, smoke("wall").priority(Priority.HERO).rate(50, 60, Curve.FALL).onGround().shape(Shape.ring(60, 10)).expand(4)
						.velocity(Velocity.up(0.15F, 0.45F, 0.3F)).drag(0.02F).life(140, 200).size(10, 16).color(BELL_DUST).alpha(0.6F).shade(0.7F)
						.build())
				.emit(30, EmitterDef.builder("stardust").rate(20, 200).onGround().shape(Shape.disc(170)).velocity(Velocity.up(0.04F, 0.14F, 0.3F))
						.module(Module.jitter(0.003F)).life(80, 140).size(0.4F, 0.7F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE)
						.build())
				.build());

		// A bell stroke: the ground ring kicks up violet dust and glitter as it passes. Played at scale front / 40.
		FxEngine.register(EffectDef.builder(VfxIds.SMASH_BELL).lod(400, 3200)
				.emit(smoke("dust").burst(70).onGround().shape(Shape.ring(40, 2)).velocity(Velocity.up(0.04F, 0.12F, 0.2F)).velocity(Velocity.radial(0.02F, 0.06F))
						.life(40, 70).size(2.4F, 3.6F).color(BELL_DUST).alpha(0.5F).build())
				.emit(EmitterDef.builder("glints").burst(50).onGround().shape(Shape.ring(40, 1)).velocity(Velocity.up(0.15F, 0.45F, 0.3F))
						.gravity(0.01F).life(20, 40).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.build());

		// A star fragment lands: a cold flash, crystal splinters and twinkling dust. Tinted with the star's colour.
		FxEngine.register(EffectDef.builder(VfxIds.STAR_LAND).tint(0xC8B0FF).lod(200, 1600)
				.emit(flash("flash", 9, 6, 0xC8B0FF).tinted().build())
				.emit(EmitterDef.builder("splinters").burst(40).velocity(Velocity.up(0.4F, 1.0F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1)
						.life(30, 60).size(0.25F, 0.45F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(COSMIC_GLASS).tinted().build())
				.emit(smoke("puff").burst(10).onGround().shape(Shape.ring(2.5F, 1)).velocity(Velocity.radial(0.06F, 0.14F)).life(40, 60).size(2, 3)
						.color(BELL_DUST).alpha(0.5F).build())
				.emit(EmitterDef.builder("twinkle").rate(3, 30).shape(Shape.sphere(3)).velocity(Velocity.up(0.03F, 0.08F, 0.3F)).life(30, 50)
						.size(0.2F, 0.3F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.emit(groundRing("ring", 7, 20, 0xB090FF, 0.5F))
				.build());

		// The heart: a breath of stars drawn in, then thrown out, and a column of stardust climbing the strike.
		FxEngine.register(EffectDef.builder(VfxIds.HEART_NOVA).lod(400, 3200).maxAge(260)
				.cue(0, EffectDef.light(1.2F, 0xD0C0FF))
				.emit(EmitterDef.builder("inhale").priority(Priority.HERO).burst(220).shape(Shape.shell(70)).module(Module.attract(0.22F, 5))
						.life(24, 28).size(1.6F, 2.6F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(STARDUST).build())
				.emit(20, flash("flash", 110, 14, 0xD0C0FF).build())
				.emit(20, EmitterDef.builder("nova").priority(Priority.HERO).burst(360).shape(Shape.sphere(4)).velocity(Velocity.random(2.2F, 4.2F))
						.drag(0.04F).life(40, 80).size(1.8F, 2.8F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(COSMIC_GLASS).build())
				.emit(20, EmitterDef.builder("column").rate(30, 100, Curve.FALL).onGround().shape(Shape.disc(12)).velocity(Velocity.up(1.2F, 2.2F, 0.05F))
						.life(60, 100).size(1.0F, 1.6F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.emit(20, smoke("haze").burst(30).shape(Shape.shell(20)).velocity(Velocity.radial(0.5F, 1.0F)).drag(0.03F).life(120, 200).size(16, 24)
						.color(NEBULA).alpha(0.5F).emissive(Curve.keys(0, 1, 1, 0.2F)).build())
				.build());

		// Star dust settling over the imprint and lifting again, like it can't decide. Played by the aftermath.
		FxEngine.register(EffectDef.builder(VfxIds.SMASH_DUST).lod(200, 1200)
				.emit(EmitterDef.builder("dust").priority(Priority.DETAIL).burst(14).onGround().shape(Shape.disc(60)).offset(0, 1, 0)
						.velocity(Velocity.up(0.01F, 0.04F, 0.3F)).module(Module.jitter(0.003F)).life(80, 140).size(0.25F, 0.4F)
						.sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("wisps").priority(Priority.DETAIL).burst(2).onGround().shape(Shape.disc(60)).velocity(Velocity.up(0.01F, 0.03F, 0.3F))
						.module(Module.curl(8, 0.004F)).life(80, 140).size(1.2F, 1.8F).sprite(FxSprites.WISP).color(NEBULA).alpha(0.35F).build())
				.build());
	}

	// ------------------------------------------------------------------ Booming Tick

	private static void boomingTick() {
		// The heartbeat: a red pulse on the ground and a few motes. Grows with the scale it is played at.
		FxEngine.register(EffectDef.builder(VfxIds.TICK_BEAT).lod(128, 1200)
				.emit(groundRing("pulse", 2.4F, 14, 0xFF3030, 0.7F))
				.emit(glow("core", 1.6F, 10, 0xFF4040, 0.5F).sizeOverLife(Curve.POP).build())
				.emit(EmitterDef.builder("motes").burst(6).onGround().shape(Shape.disc(1.5F)).velocity(Velocity.up(0.03F, 0.08F, 0.3F))
						.life(14, 24).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(HEART).build())
				.build());

		// One boom, authored at size 10: a fireball in the band's colour, brass sparks flung flat like gear teeth,
		// smoke and a ring of dust.
		FxEngine.register(EffectDef.builder(VfxIds.TICK_BOOM).tint(0xFFB060).lod(300, 2400).maxAge(240)
				.cue(0, EffectDef.light(1.0F, 0xFFB060))
				.emit(flash("flash", 40, 8, 0xFFE0A0).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(30).shape(Shape.sphere(4)).velocity(Velocity.radial(0.3F, 0.7F))
						.velocity(Velocity.up(0.1F, 0.3F, 0.4F)).drag(0.08F).module(Module.buoyancy(0.01F)).life(20, 35).size(6, 9)
						.sprite(FxSprites.FIRE, 2).color(FIRE).tinted().sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("teeth").priority(Priority.HERO).burst(140).shape(Shape.ring(1, 0.3F)).velocity(Velocity.radial(0.8F, 1.6F))
						.velocity(Velocity.up(0.1F, 0.4F, 0.2F)).gravity(0.025F).drag(0.01F).collide(0.35F, 0.6F, 2).life(25, 45).size(0.3F, 0.5F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(BRASS).build())
				.emit(smoke("smoke").burst(20).shape(Shape.dome(6)).velocity(Velocity.radial(0.1F, 0.3F)).velocity(Velocity.up(0.1F, 0.3F, 0.4F))
						.drag(0.03F).life(80, 140).size(6, 10).color(SOOT).alpha(0.65F).build())
				.emit(smoke("dust").burst(26).onGround().shape(Shape.ring(9, 3)).expand(1.2F).velocity(Velocity.radial(0.15F, 0.3F)).life(50, 90)
						.size(4, 6).color(VfxLibrary.DUST).alpha(0.55F).build())
				.build());

		// A small blast out in the countryside (the carpet bombing, ejecta landing, the mass go): fire, brass sparks, smoke.
		FxEngine.register(EffectDef.builder(VfxIds.FIELD_BLAST).tint(0xFF6020).lod(200, 1600)
				.emit(flash("flash", 10, 5, 0xFFD080).build())
				.emit(EmitterDef.builder("fire").burst(10).shape(Shape.sphere(1.5F)).velocity(Velocity.radial(0.15F, 0.35F)).velocity(Velocity.up(0.05F, 0.2F, 0.4F))
						.drag(0.08F).life(14, 24).size(2.5F, 4).sprite(FxSprites.FIRE, 2).color(FIRE).tinted().sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("sparks").burst(28).velocity(Velocity.random(0.3F, 0.8F)).velocity(Velocity.up(0.1F, 0.3F, 0.8F)).gravity(0.03F)
						.life(15, 30).size(0.15F, 0.25F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(BRASS).build())
				.emit(smoke("smoke").burst(6).shape(Shape.dome(2)).velocity(Velocity.up(0.08F, 0.2F, 0.4F)).life(60, 100).size(3, 5).color(SOOT)
						.alpha(0.6F).build())
				.build());

		// Midnight: the last and biggest, a gold fireball, a fountain of brass and a column of fire and soot.
		FxEngine.register(EffectDef.builder(VfxIds.TICK_FINAL).lod(400, 3200).maxAge(420)
				.cue(0, EffectDef.light(1.5F, 0xFFB060))
				.emit(flash("flash", 120, 16, 0xFFD080).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(80).shape(Shape.sphere(12)).velocity(Velocity.radial(0.8F, 1.6F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.4F)).drag(0.06F).module(Module.buoyancy(0.01F)).life(40, 70).size(16, 24)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("brass").priority(Priority.HERO).burst(420).shape(Shape.sphere(4)).velocity(Velocity.up(1.4F, 3.0F, 0.7F))
						.gravity(0.045F).drag(0.005F).collide(0.35F, 0.6F, 1).life(70, 120).size(0.6F, 1.0F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.STREAK).color(BRASS).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).rate(20, 80, Curve.FALL).onGround().shape(Shape.disc(6))
						.velocity(Velocity.up(1.2F, 2.0F, 0.1F)).drag(0.01F).life(40, 60).size(8, 12).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(6, smoke("soot").rate(10, 150, Curve.FALL).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.5F, 0.9F, 0.1F))
						.module(Module.curl(30, 0.008F)).life(200, 300).size(14, 22).color(SOOT).alpha(0.7F).build())
				.emit(smoke("wall").rate(30, 40, Curve.FALL).onGround().shape(Shape.ring(30, 8)).expand(3).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.life(140, 200).size(10, 16).color(VfxLibrary.DUST).alpha(0.7F).build())
				.build());

		// A chime: gold struck off the band like dust off a bell. Played at scale bandRadius / 20, tinted the band's colour.
		FxEngine.register(EffectDef.builder(VfxIds.TICK_CHIME).tint(0xFFD080).lod(300, 2400)
				.emit(EmitterDef.builder("gold").burst(60).onGround().shape(Shape.ring(20, 1)).offset(0, 1, 0).velocity(Velocity.up(0.1F, 0.3F, 0.2F))
						.gravity(0.004F).life(30, 50).size(0.45F, 0.7F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.emit(smoke("dust").burst(24).onGround().shape(Shape.ring(20, 2)).velocity(Velocity.up(0.03F, 0.08F, 0.2F)).life(40, 60).size(2, 3)
						.color(VfxLibrary.DUST).tinted().alpha(0.3F).build())
				.build());

		// Time stops: the dust and embers in the air around you just hang there, grey, until it all lets go.
		FxEngine.register(EffectDef.builder(VfxIds.TIME_STILL).lod(64, 256).maxAge(90)
				.emit(EmitterDef.builder("dust").burst(380).shape(Shape.sphere(22)).life(72, 74).size(0.08F, 0.16F).fixedRotation()
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(STILL).alpha(0.8F).alphaOverLife(Curve.keys(0, 0, 0.05F, 1, 0.95F, 1, 1, 0))
						.build())
				.emit(EmitterDef.builder("embers").burst(90).shape(Shape.sphere(22)).life(72, 74).size(0.08F, 0.14F).fixedRotation()
						.sprite(FxSprites.FLARE).color(0xD0D0D8).alpha(0.7F).alphaOverLife(Curve.keys(0, 0, 0.05F, 1, 0.95F, 1, 1, 0)).build())
				.emit(EmitterDef.builder("drops").burst(60).shape(Shape.sphere(18)).life(72, 74).size(0.03F, 0.05F).fixedRotation()
						.sprite(FxSprites.STREAK).color(0xC0C4CC).alpha(0.6F).alphaOverLife(Curve.keys(0, 0, 0.05F, 1, 0.95F, 1, 1, 0)).build())
				.build());

		// ...and time comes back: everything hanging in the air is flung at once, warm again.
		FxEngine.register(EffectDef.builder(VfxIds.TIME_SNAP).lod(64, 256)
				.emit(EmitterDef.builder("fling").burst(260).shape(Shape.sphere(20)).velocity(Velocity.radial(0.3F, 0.9F)).drag(0.03F).gravity(0.01F)
						.life(16, 32).size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());
	}

	// ------------------------------------------------------------------ Alien Death Ray

	private static void alienDeathRay() {
		// The EMP: a fast flat sheet of blue static racing out over the ground.
		FxEngine.register(EffectDef.builder(VfxIds.ALIEN_EMP).lod(400, 3200)
				.emit(flash("flash", 60, 8, 0x80C0FF).build())
				.emit(EmitterDef.builder("sheet").priority(Priority.HERO).burst(700).onGround().shape(Shape.ring(3, 1)).offset(0, 0.4F, 0)
						.velocity(Velocity.radial(2.6F, 4.0F)).drag(0.012F).life(30, 44).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(2)
						.sprite(FxSprites.STREAK).color(STATIC).alphaOverLife(FLICKER).build())
				.emit(groundRing("ring", 120, 40, 0x60A0FF, 0.45F))
				.build());

		// Static crawling near you: a spit of sparks where an arc touches down. Played by the EMP effect.
		FxEngine.register(EffectDef.builder(VfxIds.EMP_CRAWL).lod(32, 96)
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(7).velocity(Velocity.random(0.06F, 0.18F)).velocity(Velocity.up(0.04F, 0.1F, 0.6F))
						.gravity(0.02F).life(4, 10).size(0.04F, 0.07F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(STATIC).build())
				.emit(EmitterDef.builder("flick").burst(1).life(6, 10).size(0.4F, 0.6F).sprite(FxSprites.FLARE).color(STATIC).alphaOverLife(FLICKER).build())
				.build());

		// The ray swells into a column of green fire: ions spiral up it, the ground boils and spits glass at its foot.
		FxEngine.register(EffectDef.builder(VfxIds.RAY_COLUMN).lod(400, 3200).maxAge(160)
				.emit(EmitterDef.builder("ions").priority(Priority.HERO).rate(40, 44).shape(Shape.column(16, 220)).velocity(Velocity.up(1.5F, 3.0F, 0.05F))
						.velocity(Velocity.swirl(0.4F, 0.8F)).module(Module.vortex(0.04F, 0.02F)).life(20, 40).size(0.7F, 1.2F).sprite(FxSprites.FLARE)
						.color(ALIEN).alphaOverLife(TWINKLE).build())
				.emit(smoke("boil").rate(12, 44).onGround().shape(Shape.ring(18, 4)).velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.life(60, 100).size(8, 12).color(ALIEN_HAZE).alpha(0.6F).emissive(Curve.keys(0, 0.8F, 1, 0.1F)).build())
				.emit(EmitterDef.builder("glass").rate(14, 44).onGround().shape(Shape.disc(14)).velocity(Velocity.up(0.8F, 1.6F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 1).life(30, 50).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(ALIEN).build())
				.emit(glow("foot", 60, 6, 0x60FFA0, 0.3F).rate(1, 44).build())
				.build());

		// The ray hits: green plasma inside a violet shell, glass spray for hundreds of blocks, a bleached wall of salt
		// dust, and spores rising off the new land for a long time after.
		FxEngine.register(EffectDef.builder(VfxIds.RAY_IMPACT).lod(500, 3600).maxAge(600)
				.cue(0, EffectDef.light(1.6F, 0x60FFA0))
				.emit(flash("flash", 200, 18, 0xC0FFE0).build())
				.emit(glow("bloom", 240, 100, 0x40E090, 0.4F).build())
				.emit(EmitterDef.builder("plasma").priority(Priority.HERO).burst(90).shape(Shape.sphere(20)).velocity(Velocity.radial(1.0F, 2.0F))
						.velocity(Velocity.up(0.3F, 0.8F, 0.4F)).drag(0.06F).module(Module.buoyancy(0.01F)).life(40, 70).size(18, 28)
						.sprite(FxSprites.FIRE, 2).color(ALIEN).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("shell").priority(Priority.HERO).burst(70).shape(Shape.shell(40)).velocity(Velocity.radial(1.0F, 2.0F))
						.velocity(Velocity.up(0.3F, 0.7F, 0.5F)).drag(0.035F).module(Module.curl(50, 0.01F)).life(220, 320).size(24, 36)
						.color(VIOLET_HAZE).alpha(0.6F).shade(0.8F).emissive(Curve.keys(0, 1, 0.6F, 0.55F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("glass").priority(Priority.HERO).burst(420).shape(Shape.sphere(16)).velocity(Velocity.up(2.0F, 4.2F, 0.8F))
						.gravity(0.05F).drag(0.004F).collide(0.3F, 0.6F, 1).life(90, 150).size(1.0F, 1.6F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.FLARE).color(ALIEN).build())
				.emit(4, smoke("salt").priority(Priority.HERO).rate(50, 60, Curve.FALL).onGround().shape(Shape.ring(70, 10)).expand(5)
						.velocity(Velocity.up(0.1F, 0.3F, 0.3F)).drag(0.02F).life(140, 200).size(12, 18).color(SALT).alpha(0.5F).shade(0.7F).build())
				.emit(40, EmitterDef.builder("spores").rate(20, 320).onGround().shape(Shape.disc(200)).velocity(Velocity.up(0.04F, 0.12F, 0.3F))
						.module(Module.curl(10, 0.004F)).life(100, 160).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(SPORE).alphaOverLife(TWINKLE).build())
				.build());

		// The alien land spreading: spores and violet dust along its edge. Played at scale radius / 40.
		FxEngine.register(EffectDef.builder(VfxIds.TERRA_PULSE).lod(400, 3200)
				.emit(EmitterDef.builder("spores").burst(60).onGround().shape(Shape.ring(40, 3)).velocity(Velocity.up(0.08F, 0.25F, 0.2F))
						.module(Module.curl(6, 0.004F)).life(40, 70).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(SPORE).alphaOverLife(TWINKLE).build())
				.emit(smoke("dust").burst(24).onGround().shape(Shape.ring(40, 2)).velocity(Velocity.up(0.03F, 0.08F, 0.2F)).life(40, 70).size(3, 4)
						.color(ALIEN_HAZE).alpha(0.3F).shade(0.85F).emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.build());

		// A tractor beam at work: the ground torn up and spun into the sky, motes and rings of light climbing.
		FxEngine.register(EffectDef.builder(VfxIds.HARVEST_LIFT).lod(300, 2400).maxAge(120)
				.emit(EmitterDef.builder("debris").burst(22).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.4F, 0.8F, 0.1F))
						.velocity(Velocity.swirl(0.2F, 0.4F)).module(Module.vortex(0.03F, 0.02F)).life(50, 80).size(0.4F, 0.8F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(EARTH).spin(0.05F, 0.2F).build())
				.emit(smoke("dust").burst(6).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.2F, 0.4F, 0.1F)).velocity(Velocity.swirl(0.1F, 0.2F))
						.life(60, 90).size(4, 6).color(VfxLibrary.DUST).alpha(0.4F).build())
				.emit(EmitterDef.builder("motes").burst(16).shape(Shape.column(8, 60)).velocity(Velocity.up(0.6F, 1.2F, 0.05F)).life(30, 50)
						.size(0.6F, 1.0F).sprite(FxSprites.FLARE).color(ALIEN).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 1, 0).velocity(Velocity.up(0.9F, 0.9F, 0)).mode(Mode.FLAT).fixedRotation()
						.life(50, 50).size(16, 16).sprite(FxSprites.RING).color(0x80FFB0).alpha(0.45F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// A beam cuts out and takes what it held apart: a green flash and glass.
		FxEngine.register(EffectDef.builder(VfxIds.HARVEST_CUT).lod(300, 2400)
				.emit(flash("flash", 30, 10, 0x80FFB0).build())
				.emit(EmitterDef.builder("glass").burst(90).shape(Shape.disc(8)).velocity(Velocity.up(0.6F, 1.6F, 0.7F)).gravity(0.04F)
						.collide(0.3F, 0.6F, 1).life(40, 80).size(0.4F, 0.7F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(ALIEN).build())
				.emit(smoke("haze").burst(12).shape(Shape.dome(8)).velocity(Velocity.up(0.1F, 0.3F, 0.4F)).life(60, 100).size(6, 9).color(ALIEN_HAZE)
						.alpha(0.5F).emissive(Curve.keys(0, 1, 1, 0.1F)).build())
				.build());

		// The alien land alive near you: bioluminescent spores drifting up.
		FxEngine.register(EffectDef.builder(VfxIds.BLOOM_SPORES).lod(48, 128)
				.emit(EmitterDef.builder("spores").priority(Priority.DETAIL).burst(4).onGround().shape(Shape.disc(18)).offset(0, 0.5F, 0)
						.velocity(Velocity.up(0.02F, 0.05F, 0.3F)).module(Module.curl(4, 0.003F)).life(60, 110).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(SPORE).alphaOverLife(TWINKLE).build())
				.build());

		// Motes pouring up out of the shaft. Played at scale shaft / 10.
		FxEngine.register(EffectDef.builder(VfxIds.SHAFT_MOTES).lod(300, 1600)
				.emit(EmitterDef.builder("motes").burst(6).shape(Shape.disc(5)).velocity(Velocity.up(0.3F, 0.6F, 0.05F)).life(40, 80).size(0.25F, 0.4F)
						.sprite(FxSprites.FLARE).color(ALIEN).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ The Sun Disappears

	private static void sunDisappears() {
		// A thread of gold climbing to the sun: motes rising up it, the ground under it hazed with heat.
		FxEngine.register(EffectDef.builder(VfxIds.SUN_BEAM).lod(300, 2400).maxAge(160)
				.emit(EmitterDef.builder("motes").rate(6, 110).shape(Shape.column(3, 120)).velocity(Velocity.up(0.8F, 1.6F, 0.05F)).life(30, 60)
						.size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(SUNLIGHT).alphaOverLife(TWINKLE).build())
				.emit(smoke("haze").rate(3, 110).onGround().shape(Shape.ring(4, 2)).velocity(Velocity.radial(0.1F, 0.2F)).life(50, 80).size(3, 5)
						.color(DAWN).alpha(0.4F).emissive(Curve.keys(0, 0.8F, 1, 0.3F)).build())
				.emit(glow("foot", 12, 4, 0xFFE0A0, 0.3F).rate(1, 110).build())
				.build());

		// The cold: ice glitter falling out of the dark around you. Played every second at each player.
		FxEngine.register(EffectDef.builder(VfxIds.SUN_FROST).lod(48, 160)
				.emit(EmitterDef.builder("ice").burst(40).shape(Shape.box(28, 1, 28)).offset(0, 16, 0).velocity(Velocity.up(-0.08F, -0.04F, 0.2F))
						.module(Module.jitter(0.003F)).life(140, 200).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(ICE).alpha(0.7F)
						.alphaOverLife(TWINKLE).build())
				.emit(smoke("breath").burst(2).onGround().shape(Shape.disc(20)).velocity(Velocity.radial(0.01F, 0.03F)).life(120, 180).size(5, 8)
						.color(0x8A98B0).alpha(0.22F).build())
				.build());

		// The corpse sheds its crust as it falls: burning plates peeling off and a smoke wake. Played moving.
		FxEngine.register(EffectDef.builder(VfxIds.CORPSE_SHED).lod(600, 3200)
				.emit(EmitterDef.builder("crust").priority(Priority.HERO).burst(6).shape(Shape.shell(62)).inherit(0.7F)
						.velocity(Velocity.random(0.2F, 0.6F)).life(30, 60).size(2.0F, 3.5F).mode(Mode.STRETCHED).stretch(0.6F)
						.sprite(FxSprites.STREAK).color(CRUST).build())
				.emit(smoke("wake").burst(2).shape(Shape.shell(66)).inherit(0.3F).velocity(Velocity.random(0.05F, 0.15F)).life(100, 160)
						.size(20, 30).color(Gradient.of(0, 0x4A2A20, 1, 0x1A1210)).alpha(0.6F).emissive(Curve.keys(0, 0.6F, 0.3F, 0)).build())
				.build());

		// The corpse hits and the light comes back: a white-gold flash, a wall of fire, the crust thrown into the sky,
		// a burning plume, and gold motes everywhere like the first light of a day.
		FxEngine.register(EffectDef.builder(VfxIds.SUN_IMPACT).lod(600, 4000).maxAge(700)
				.cue(0, EffectDef.light(2.0F, 0xFFB060))
				.emit(flash("flash", 300, 22, 0xFFE8B0).build())
				.emit(glow("bloom", 320, 120, 0xFFA040, 0.4F).build())
				.emit(EmitterDef.builder("firewall").priority(Priority.HERO).rate(60, 40, Curve.FALL).onGround().shape(Shape.ring(70, 10)).expand(6)
						.velocity(Velocity.up(0.5F, 1.2F, 0.3F)).drag(0.03F).module(Module.buoyancy(0.01F)).life(40, 70).size(18, 28)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("crust").priority(Priority.HERO).burst(320).shape(Shape.dome(60)).velocity(Velocity.up(2.0F, 4.0F, 0.7F))
						.gravity(0.05F).drag(0.004F).collide(0.3F, 0.6F, 1).life(100, 160).size(1.8F, 3.0F).mode(Mode.STRETCHED).stretch(1.2F)
						.sprite(FxSprites.STREAK).color(CRUST).build())
				.emit(10, EmitterDef.builder("plume_fire").rate(10, 120, Curve.FALL).onGround().shape(Shape.disc(20)).velocity(Velocity.up(1.0F, 1.8F, 0.1F))
						.module(Module.buoyancy(0.01F)).life(40, 60).size(14, 20).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(10, smoke("plume").priority(Priority.HERO).rate(20, 200, Curve.FALL).onGround().shape(Shape.disc(30))
						.velocity(Velocity.up(0.6F, 1.2F, 0.1F)).module(Module.curl(60, 0.008F)).life(220, 320).size(24, 36).color(SOOT).alpha(0.75F)
						.emissive(Curve.keys(0, 0.5F, 0.3F, 0)).build())
				.emit(40, EmitterDef.builder("light").rate(20, 220).shape(Shape.sphere(220)).velocity(Velocity.up(0.04F, 0.12F, 0.3F)).life(80, 140)
						.size(0.8F, 1.2F).sprite(FxSprites.FLARE).color(DAWN).alphaOverLife(TWINKLE).build())
				.build());

		// Dawn, around each player: warm motes lifting off the ground and a soft gold mist.
		FxEngine.register(EffectDef.builder(VfxIds.DAWN_MOTES).lod(48, 160).maxAge(400)
				.emit(EmitterDef.builder("motes").rate(6, 220).shape(Shape.box(30, 6, 30)).velocity(Velocity.up(0.02F, 0.06F, 0.3F))
						.module(Module.jitter(0.002F)).life(80, 140).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(DAWN).alphaOverLife(TWINKLE).build())
				.emit(smoke("mist").rate(1, 180).onGround().shape(Shape.disc(30)).velocity(Velocity.radial(0.01F, 0.03F)).life(120, 200).size(6, 10)
						.color(0xFFF0DC).alpha(0.08F).emissive(Curve.keys(0, 0.6F, 1, 0.3F)).build())
				.build());
	}

	// ------------------------------------------------------------------ Dyson Lance

	private static void dysonLance() {
		// Sun off the mirrors: glints winking across the swarm as it turns.
		FxEngine.register(EffectDef.builder(VfxIds.MIRROR_GLINT).lod(700, 3200)
				.emit(EmitterDef.builder("glints").burst(8).shape(Shape.disc(170)).offset(0, 20, 0).life(6, 12).size(6, 10).sprite(FxSprites.FLARE)
						.color(Gradient.of(0, 0xFFFFFF, 1, 0xFFE0A0)).alphaOverLife(FLICKER).build())
				.build());

		// Where the lance touches: molten rock spraying, steam boiling off, slag left cooling on the floor of the cut.
		FxEngine.register(EffectDef.builder(VfxIds.LANCE_CUT).lod(300, 2400)
				.emit(EmitterDef.builder("spray").burst(10).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.6F, 1.4F, 0.6F)).gravity(0.05F)
						.collide(0.2F, 0.5F, 1).life(20, 40).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(MOLTEN).build())
				.emit(smoke("steam").burst(2).onGround().shape(Shape.disc(5)).velocity(Velocity.up(0.3F, 0.6F, 0.2F)).life(60, 100).size(5, 8)
						.color(STEAM).alpha(0.45F).shade(0.8F).build())
				.emit(EmitterDef.builder("slag").burst(3).onGround().shape(Shape.disc(5)).offset(0, 0.3F, 0).life(60, 120).size(1.2F, 2.0F)
						.sprite(FxSprites.GLOW).color(SLAG).alpha(0.8F).build())
				.emit(glow("foot", 20, 3, 0xFFC080, 0.35F).build())
				.build());

		// The drill: the crossing erupts, molten rock fountaining and a column of steam over it.
		FxEngine.register(EffectDef.builder(VfxIds.LANCE_DRILL).lod(400, 3200).maxAge(320)
				.cue(0, EffectDef.light(1.2F, 0xFFC080))
				.emit(flash("flash", 60, 10, 0xFFF0C0).build())
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).rate(20, 140).onGround().shape(Shape.disc(10)).velocity(Velocity.up(1.2F, 2.4F, 0.4F))
						.gravity(0.04F).collide(0.2F, 0.5F, 1).life(40, 70).size(0.6F, 1.0F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(MOLTEN)
						.build())
				.emit(smoke("steam").priority(Priority.HERO).rate(6, 160).onGround().shape(Shape.disc(14)).velocity(Velocity.up(0.6F, 1.2F, 0.1F))
						.module(Module.curl(30, 0.008F)).life(120, 200).size(12, 18).color(STEAM).alpha(0.55F).shade(0.75F).build())
				.emit(smoke("rim").rate(4, 140).onGround().shape(Shape.ring(22, 3)).velocity(Velocity.radial(0.05F, 0.15F)).life(60, 100).size(4, 6)
						.color(VfxLibrary.DUST).alpha(0.5F).build())
				.emit(glow("heat", 40, 6, 0xFF8030, 0.35F).rate(1, 140).build())
				.build());
	}

	// ------------------------------------------------------------------ Seismic Tsunami

	private static void seismicTsunami() {
		// A hammer blow: dust thrown out in a ring, rocks bouncing, grit skidding flat over the ground.
		FxEngine.register(EffectDef.builder(VfxIds.HAMMER_BLOW).lod(300, 2400)
				.emit(smoke("dust").priority(Priority.HERO).burst(40).onGround().shape(Shape.ring(10, 4)).expand(1.2F).velocity(Velocity.radial(0.3F, 0.6F))
						.velocity(Velocity.up(0.1F, 0.2F, 0.3F)).drag(0.02F).life(60, 100).size(4, 7).color(EARTH).alpha(0.7F).build())
				.emit(EmitterDef.builder("rocks").burst(60).onGround().shape(Shape.disc(8)).velocity(Velocity.up(0.5F, 1.0F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 2).life(40, 70).size(0.4F, 0.8F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(ROCK)
						.spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("grit").burst(90).onGround().shape(Shape.ring(4, 1)).offset(0, 0.3F, 0).velocity(Velocity.radial(0.8F, 1.6F))
						.velocity(Velocity.up(0.02F, 0.08F, 0.1F)).gravity(0.02F).drag(0.02F).life(15, 30).size(0.15F, 0.25F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(ROCK).build())
				.emit(groundRing("ring", 40, 24, 0x9A8E7A, 0.55F))
				.build());

		// The crest sheds clods and dust off its top as it rolls. Played along the front, aimed outward.
		FxEngine.register(EffectDef.builder(VfxIds.CREST_SPRAY).lod(160, 400)
				.emit(EmitterDef.builder("clods").priority(Priority.DETAIL).burst(8).shape(Shape.sphere(3)).velocity(Velocity.along(0.4F, 0.8F, 0.5F))
						.velocity(Velocity.up(0.3F, 0.6F, 0.3F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 50).size(0.3F, 0.6F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(EARTH).spin(0.05F, 0.2F).build())
				.emit(smoke("dust").burst(2).shape(Shape.sphere(3)).velocity(Velocity.along(0.1F, 0.3F, 0.4F)).velocity(Velocity.up(0.1F, 0.2F, 0.3F))
						.life(50, 80).size(4, 6).color(EARTH).alpha(0.55F).build())
				.build());
	}
}
