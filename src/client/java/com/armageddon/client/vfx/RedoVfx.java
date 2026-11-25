package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/**
 * Effects for the bombs rebuilt from scratch: the Aurora, the Big Bang, Sixteen Chunks, the Zombie Apocalypse, the
 * Spectral Ice and the Prism.
 */
final class RedoVfx {
	private RedoVfx() {
	}

	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient STAR = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE8F0FF, 1, argb(0xC0D0FF, 0));
	static final Gradient AURORA = Gradient.of(0, 0xC0FFD8, 0.3F, 0x50FF90, 0.65F, 0x40D0FF, 1, argb(0xA040FF, 0));

	static void register() {
		aurora();
		bigBang();
		prism();
		spectralIce();
		chunks();
		blight();
	}

	// ------------------------------------------------------------------ Aurora

	private static void aurora() {
		// Night: stars coming out over the whole sky, and now and then a shooting star.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_STARS).lod(600, 4000).maxAge(640)
				.emit(EmitterDef.builder("stars").rate(5, 560).shape(Shape.dome(150)).offset(0, 40, 0).life(80, 160).size(0.6F, 1.2F)
						.sprite(FxSprites.FLARE).color(STAR).alphaOverLife(Curve.of(x -> Math.min(1, x * 6) * Math.min(1, (1 - x) * 4) * (0.7 + 0.3 * Math.sin(x * 40)))).build())
				.emit(EmitterDef.builder("meteors").rate(0.06F, 560).shape(Shape.dome(120)).offset(0, 70, 0).velocity(Velocity.random(2.5F, 3.5F))
						.velocity(Velocity.up(-1.5F, -1.0F, 0.3F)).life(10, 16).size(0.4F, 0.6F).mode(Mode.STRETCHED).stretch(6).sprite(FxSprites.STREAK)
						.color(STAR).build())
				.build());

		// Where a hem burns the land, tinted the curtain's colour: sparks lifting off, glass shards, a pool of light.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_SPARK).tint(0x50FF90).lod(160, 1000)
				.emit(EmitterDef.builder("sparks").burst(14).velocity(Velocity.up(0.3F, 0.8F, 0.4F)).module(Module.jitter(0.02F)).life(16, 30).size(0.1F, 0.16F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WHITE).tinted().build())
				.emit(EmitterDef.builder("glass").burst(8).velocity(Velocity.up(0.2F, 0.5F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(20, 34)
						.size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.4F, 3).life(24, 30)
						.sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The solar storm: a flash, three shockwaves of coloured light over the ground, a fountain of light up the
		// column, and the colours raining back down.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_STORM).lod(500, 3600).maxAge(260)
				.cue(0, EffectDef.light(1.6F, 0x80FFC0))
				.emit(flash("flash", 50, 14, 0xE0FFF0).build())
				.emit(EmitterDef.builder("ring1").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(70, 70).life(40, 40).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0x50FF90).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(6, EmitterDef.builder("ring2").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(100, 100).life(46, 46).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0x40D0FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(12, EmitterDef.builder("ring3").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(130, 130).life(52, 52).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xA040FF).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).rate(40, 30, Curve.FALL).shape(Shape.disc(3)).velocity(Velocity.up(3.0F, 5.0F, 0.06F))
						.life(20, 34).size(0.6F, 1.0F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(AURORA).build())
				.emit(EmitterDef.builder("wisps").priority(Priority.HERO).burst(60).shape(Shape.disc(4)).velocity(Velocity.radial(1.0F, 2.0F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.2F)).velocity(Velocity.swirl(0.4F, 0.8F)).drag(0.03F).module(Module.curl(10, 0.02F)).life(60, 100)
						.size(2.5F, 4).sprite(FxSprites.WISP).color(AURORA).alpha(0.7F).build())
				.emit(20, EmitterDef.builder("rain").rate(24, 140).shape(Shape.disc(80)).offset(0, 50, 0).velocity(Velocity.up(-0.3F, -0.15F, 0.2F))
						.module(Module.jitter(0.01F)).life(80, 140).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(AURORA).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Big Bang

	static final Gradient INFALL = Gradient.of(0, 0x6080FF, 0.5F, 0xC0A0FF, 1, 0xFFFFFF);
	static final Gradient CMB = Gradient.of(0, 0xFF9050, 0.25F, 0xFFC070, 0.5F, 0x70A0FF, 0.75F, 0xFF7040, 1, 0x5080FF);
	static final Gradient GALAXY = Gradient.of(0, 0xFFF4E0, 0.3F, 0xFFD8A0, 0.6F, 0xA0B8FF, 1, argb(0x6070FF, 0));
	static final Gradient NEBULA = Gradient.of(0, 0xFF80E0, 0.35F, 0xB060FF, 0.7F, 0x4080FF, 1, 0x302060);
	static final Gradient SUNFIRE = Gradient.of(0, 0xFFFFF0, 0.3F, 0xFFE090, 0.7F, 0xFF9030, 1, argb(0xC04010, 0));

	private static void bigBang() {
		// The singularity: everything streaming in toward the point, faster and faster, dust spiralling up off the land.
		FxEngine.register(EffectDef.builder(VfxIds.BANG_INHALE).lod(400, 3000).maxAge(100)
				.emit(EmitterDef.builder("light").priority(Priority.HERO).rate(30, 80, Curve.RISE).shape(Shape.shell(45)).module(Module.attract(0.08F, 1.5F))
						.module(Module.vortex(0.02F, 0)).life(30, 50).size(0.2F, 0.35F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(INFALL).build())
				.emit(smoke("dust").rate(8, 80, Curve.RISE).onGround().shape(Shape.ring(30, 10)).velocity(Velocity.swirl(0.3F, 0.5F)).module(Module.attract(0.03F, 2))
						.module(Module.vortex(0.02F, 0)).life(50, 80).size(2, 3.5F).color(Gradient.of(0, 0x8A7A6A, 1, 0x6A5A50)).alpha(0.5F).build())
				.build());

		// The afterglow of creation: a vast sphere of mottled orange and blue specks, the universe's first light.
		FxEngine.register(EffectDef.builder(VfxIds.BANG_CMB).lod(600, 4000).maxAge(200)
				.emit(EmitterDef.builder("specks").priority(Priority.HERO).burst(1600).shape(Shape.shell(6)).velocity(Velocity.radial(2.6F, 3.0F)).drag(0.04F)
						.life(120, 180).size(1.6F, 2.4F).fixedRotation().sprite(FxSprites.GLOW).color(CMB).alpha(0.5F)
						.alphaOverLife(Curve.keys(0, 0, 0.1F, 1, 0.6F, 0.7F, 1, 0)).build())
				.build());

		// A galaxy wheeling into shape: a disc of stars turning round a glowing bulge, two dust lanes.
		FxEngine.register(EffectDef.builder(VfxIds.GALAXY_SWIRL).lod(500, 3600).maxAge(260)
				.emit(EmitterDef.builder("stars").priority(Priority.HERO).rate(30, 60).shape(Shape.disc(16)).velocity(Velocity.swirl(0.2F, 0.35F))
						.module(Module.vortex(0.025F, 0.004F)).life(160, 220).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(GALAXY).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("bulge").rate(1, 200).life(10, 14).size(5, 7).sprite(FxSprites.GLOW).color(0xFFE8C0).alpha(0.45F).build())
				.emit(smoke("lanes").rate(3, 60).shape(Shape.ring(9, 3)).velocity(Velocity.swirl(0.15F, 0.25F)).module(Module.vortex(0.02F, 0.002F))
						.life(150, 200).size(2.5F, 4).color(Gradient.of(0, 0x9070E0, 1, 0x6050A0)).alpha(0.22F).shade(1).emissive(Curve.constant(0.9F)).build())
				.build());

		// A nebula billowing where the glass cloud forms: glowing wisps with young stars inside.
		FxEngine.register(EffectDef.builder(VfxIds.NEBULA_CLOUD).lod(500, 3600).maxAge(300)
				.emit(smoke("cloud").priority(Priority.HERO).rate(6, 60).shape(Shape.sphere(10)).velocity(Velocity.random(0.02F, 0.06F)).module(Module.curl(12, 0.006F))
						.life(180, 240).size(6, 10).color(NEBULA).alpha(0.35F).shade(1).emissive(Curve.constant(0.9F)).build())
				.emit(EmitterDef.builder("young").rate(3, 200).shape(Shape.sphere(9)).life(30, 60).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(STAR)
						.alphaOverLife(TWINKLE).build())
				.build());

		// The newborn sun ignites: a flash, a fountain of fire round it, a corona of sparks spreading.
		FxEngine.register(EffectDef.builder(VfxIds.SUN_IGNITE).lod(500, 3600).maxAge(200)
				.cue(0, EffectDef.light(1.5F, 0xFFD080))
				.emit(flash("flash", 30, 14, 0xFFF4D0).build())
				.emit(EmitterDef.builder("corona").priority(Priority.HERO).burst(300).shape(Shape.shell(4)).velocity(Velocity.radial(0.4F, 1.0F)).drag(0.04F)
						.life(30, 60).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(SUNFIRE).build())
				.emit(EmitterDef.builder("flares").rate(4, 160).shape(Shape.shell(4.5F)).velocity(Velocity.radial(0.2F, 0.4F)).module(Module.attract(0.02F, 3)).life(30, 50)
						.size(1.2F, 2).sprite(FxSprites.FIRE, 2).color(SUNFIRE).alphaOverLife(Curve.FADE).build())
				.build());
	}

	// ------------------------------------------------------------------ Prism

	static final Gradient RAINBOW = Gradient.of(0, 0xFF2020, 0.17F, 0xFF8A10, 0.33F, 0xFFE020, 0.5F, 0x30E030, 0.67F, 0x20A0FF, 0.83F, 0x3040FF, 1, 0xB030FF);

	private static void prism() {
		// The gem shattering: a burst of rainbow glass shards and sparkles flying out of the flash.
		FxEngine.register(EffectDef.builder(VfxIds.PRISM_SHARDS).lod(400, 2600).maxAge(160)
				.cue(0, EffectDef.light(1.8F, 0xFFFFFF))
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(320).shape(Shape.sphere(3)).velocity(Velocity.random(0.6F, 1.8F)).gravity(0.03F)
						.drag(0.02F).life(40, 80).size(0.3F, 0.6F).spin(-0.6F, 0.6F).fixedRotation().sprite(FxSprites.FLARE).color(RAINBOW).build())
				.emit(EmitterDef.builder("sparkle").burst(200).shape(Shape.sphere(4)).velocity(Velocity.random(0.2F, 0.8F)).drag(0.04F).life(40, 90)
						.size(0.12F, 0.22F).sprite(FxSprites.FLARE).color(RAINBOW).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(60, 60).life(26, 26).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xFFFFFF).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Sunlight pouring down into the prism: sparkling motes riding the beam.
		FxEngine.register(EffectDef.builder(VfxIds.PRISM_INBEAM).lod(300, 2000)
				.emit(EmitterDef.builder("motes").burst(6).shape(Shape.column(2, 60)).velocity(Velocity.up(-1.4F, -0.9F, 0.02F)).life(20, 40).size(0.2F, 0.35F)
						.sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, argb(0xFFF0C0, 0))).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Spectral Ice

	static final Gradient FROST = Gradient.of(0, 0xFFFFFF, 0.4F, 0xC8F0FF, 1, argb(0x70C0FF, 0));
	static final Gradient GHOST = Gradient.of(0, 0xE0FFFF, 0.5F, 0x80E0FF, 1, argb(0x4070FF, 0));

	private static void spectralIce() {
		// Frost glitter climbing a spike.
		FxEngine.register(EffectDef.builder(VfxIds.SPECTRAL_MOTES).lod(160, 900)
				.emit(EmitterDef.builder("glitter").burst(4).shape(Shape.sphere(0.8F)).velocity(Velocity.up(0.08F, 0.2F, 0.3F)).life(20, 40).size(0.15F, 0.25F)
						.sprite(FxSprites.FLARE).color(FROST).alphaOverLife(TWINKLE).build())
				.build());

		// Ghostly wisps circling and climbing the pyramid.
		FxEngine.register(EffectDef.builder(VfxIds.SPECTRAL_WISPS).lod(300, 1600)
				.emit(EmitterDef.builder("wisps").burst(3).onGround().shape(Shape.ring(26, 4)).offset(0, 2, 0).velocity(Velocity.swirl(0.3F, 0.5F))
						.velocity(Velocity.up(0.15F, 0.3F, 0.1F)).module(Module.vortex(0.02F, 0.01F)).module(Module.curl(6, 0.01F)).life(80, 120).size(1.4F, 2.2F)
						.sprite(FxSprites.WISP).color(GHOST).alpha(0.7F).build())
				.emit(EmitterDef.builder("trails").burst(6).onGround().shape(Shape.ring(26, 4)).offset(0, 2, 0).velocity(Velocity.swirl(0.3F, 0.5F))
						.velocity(Velocity.up(0.15F, 0.3F, 0.1F)).module(Module.vortex(0.02F, 0.01F)).life(40, 70).size(0.15F, 0.25F).sprite(FxSprites.FLARE)
						.color(FROST).alphaOverLife(TWINKLE).build())
				.build());

		// The spray: a spectral frost nova out of the apex.
		FxEngine.register(EffectDef.builder(VfxIds.SPECTRAL_NOVA).lod(400, 2400).maxAge(200)
				.cue(0, EffectDef.light(1.4F, 0xA0E8FF))
				.emit(flash("flash", 30, 12, 0xE8FCFF).build())
				.emit(EmitterDef.builder("needles").priority(Priority.HERO).burst(400).shape(Shape.sphere(1)).velocity(Velocity.random(1.4F, 2.8F)).drag(0.03F)
						.life(30, 50).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FROST).build())
				.emit(EmitterDef.builder("ghosts").burst(30).shape(Shape.sphere(2)).velocity(Velocity.random(0.4F, 0.9F)).drag(0.03F).module(Module.curl(8, 0.02F))
						.life(60, 100).size(2.5F, 4).sprite(FxSprites.WISP).color(GHOST).alpha(0.7F).build())
				.emit(smoke("frost").burst(40).shape(Shape.shell(3)).velocity(Velocity.radial(0.6F, 1.2F)).drag(0.05F).life(100, 160).size(5, 8)
						.color(Gradient.of(0, 0xF0FAFF, 1, 0xC8E4F4)).alpha(0.45F).shade(0.95F).emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.build());
	}

	// ------------------------------------------------------------------ Sixteen Chunks

	private static void chunks() {
		// A rolling chunk corrupting: dead pixels flickering through it, data pouring down, flat scanline slivers.
		FxEngine.register(EffectDef.builder(VfxIds.CHUNK_STATIC).tint(0x60E0FF).lod(200, 1400)
				.emit(EmitterDef.builder("pixels").burst(7).shape(Shape.box(8, 10, 8)).offset(0, 11, 0).life(3, 8).size(0.25F, 0.6F).fixedRotation()
						.sprite(FxSprites.GLOW).color(WHITE).tinted().alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("rain").burst(4).shape(Shape.box(8, 1, 8)).offset(0, 26, 0).velocity(Velocity.up(-1.3F, -0.8F, 0)).life(18, 30)
						.size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFFFFF, 1, argb(0xFFFFFF, 0)))
						.tinted().build())
				.emit(EmitterDef.builder("slivers").burst(1).shape(Shape.box(6, 10, 6)).offset(0, 11, 0).mode(Mode.FLAT).fixedRotation().life(2, 5).size(3, 5)
						.sprite(FxSprites.STREAK).color(WHITE).tinted().alpha(0.6F).build())
				.build());
	}

	// ------------------------------------------------------------------ Zombie Apocalypse

	static final Gradient ROT = Gradient.of(0, 0xD8FF70, 0.4F, 0x9CE040, 1, argb(0x507020, 0));
	static final Gradient MIASMA = Gradient.of(0, 0x7A9A50, 0.5F, 0x5A7040, 1, 0x3A4630);

	private static void blight() {
		// Life on the blight: glowing rot spores rising, flies buzzing in knots, a sickly wisp crawling over the ground.
		FxEngine.register(EffectDef.builder(VfxIds.BLIGHT_AMBIENT).lod(120, 800)
				.emit(EmitterDef.builder("spores").burst(4).shape(Shape.disc(3)).offset(0, 0.5F, 0).velocity(Velocity.up(0.03F, 0.08F, 0.3F))
						.module(Module.curl(4, 0.006F)).life(60, 110).size(0.12F, 0.22F).sprite(FxSprites.FLARE).color(ROT).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("flies").burst(6).shape(Shape.sphere(0.6F)).offset(0, 1.2F, 0).module(Module.attract(0.02F, 0)).module(Module.jitter(0.08F))
						.drag(0.15F).life(40, 70).size(0.05F, 0.08F).sprite(FxSprites.DUST).color(0x141810).build())
				.emit(smoke("wisp").burst(1).onGround().offset(0, 0.4F, 0).velocity(Velocity.random(0.02F, 0.05F)).module(Module.curl(5, 0.004F)).life(70, 110)
						.size(1.6F, 2.6F).color(MIASMA).alpha(0.35F).shade(1).emissive(Curve.constant(0.35F)).build())
				.build());

		// The edge of the blight: a wall of miasma boiling up where it is spreading, with spores spat ahead of it.
		FxEngine.register(EffectDef.builder(VfxIds.BLIGHT_EDGE).lod(300, 2000)
				.emit(smoke("wall").burst(3).onGround().shape(Shape.box(3, 0, 3)).velocity(Velocity.up(0.04F, 0.1F, 0.2F)).module(Module.curl(8, 0.006F))
						.life(70, 110).size(3, 5).sizeOverLife(Curve.RISE).color(MIASMA).alpha(0.4F).shade(0.9F).emissive(Curve.keys(0, 0.4F, 1, 0.1F)).build())
				.emit(EmitterDef.builder("spit").burst(3).onGround().velocity(Velocity.along(0.15F, 0.3F, 0.5F)).velocity(Velocity.up(0.1F, 0.2F, 0.2F))
						.gravity(0.01F).life(30, 50).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(ROT).build())
				.build());

		// The heartbeat: a sickly ring thumping out over the blight and a flare of glowing rot from the heart.
		FxEngine.register(EffectDef.builder(VfxIds.BLIGHT_PULSE).lod(400, 2400).maxAge(60)
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(40, 40).life(26, 26)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0x9CFF50).alpha(0.55F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("heart").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(10, 10).life(14, 14)
						.sprite(FxSprites.GLOW).color(0xB0FF60).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("veins").burst(40).onGround().shape(Shape.disc(4)).offset(0, 0.3F, 0).velocity(Velocity.radial(0.8F, 1.4F)).drag(0.06F)
						.life(16, 26).size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(ROT).build())
				.build());
	}
}
