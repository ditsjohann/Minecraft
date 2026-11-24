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
 * Placed bombs, batch three: Godfall and the toys, then the first rares.
 * <ul>
 * <li>Godfall: holy. Every spear lands in a burst of gold light and white feathers that drift down long after; the great
 * spear sends a column of light up and feathers over the whole temple.</li>
 * <li>Disco: a mirror ball nobody can see: glints dancing over everything in the beat's colour, and a low fog machine
 * haze lit from below.</li>
 * <li>Glitter: actual glitter, flat flakes that tumble and catch the light in every colour, settling slowly.</li>
 * <li>Pumpkin Patch: Halloween. Each pumpkin sprouts with a pop of soil and a ghost-flame wisp; purple-green fog creeps
 * over the patch with will-o'-the-wisps in it.</li>
 * <li>Cobweb: silk. Long threads shoot out, fluffy silk drifts, and little dark spiders run off across the ground.</li>
 * <li>Balloon: helium shimmer as they lift, then each one pops into rubber shreds in its own colour.</li>
 * <li>Sprinkler: a soft grey cloud, real rain with splashes where it lands, and a rainbow when it's done.</li>
 * <li>Thermite: the real thing. A white-hot core too bright to look at, a fountain of sparks bouncing everywhere, and
 * white smoke.</li>
 * <li>Tesla: a dark cloud lid lit from inside; every bolt fuses the ground into glowing glass veins and steams.</li>
 * <li>Sinkhole: the ground pours in. Dust and soil slide down the funnel, clods tumble in, the rim crumbles.</li>
 * </ul>
 */
final class BombVfx3 {
	private BombVfx3() {
	}

	static final Gradient HOLY = Gradient.of(0, 0xFFFFFF, 0.4F, 0xFFF0C0, 1, argb(0xFFD060, 0));
	static final Gradient FEATHER = Gradient.of(0, 0xFFFFFF, 0.7F, 0xFFF8E8, 1, 0xF0E8D8);
	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient HOLO = Gradient.of(0, 0xFFB0E0, 0.2F, 0xFFF0A0, 0.4F, 0xA0FFD0, 0.6F, 0xA0D0FF, 0.8F, 0xE0A0FF, 1, 0xFFB0E0);
	static final Gradient SOUL_FLAME = Gradient.of(0, 0xE0FFFF, 0.3F, 0x60F0E0, 0.7F, 0x30A0B0, 1, argb(0x104050, 0));
	static final Gradient SPOOK_FOG = Gradient.of(0, 0x9A80B0, 0.5F, 0x80A088, 1, 0x706880);
	static final Gradient WISP_ORANGE = Gradient.of(0, 0xFFE0A0, 0.5F, 0xFF9030, 1, argb(0xC04010, 0));
	static final Gradient SILK = Gradient.of(0, 0xFFFFFF, 1, argb(0xE8E8E8, 0));
	static final Gradient RAIN = Gradient.of(0, 0xE0F0FF, 1, argb(0xA0C8F0, 0));
	static final Gradient RAINCLOUD = Gradient.of(0, 0xC8CCD4, 1, 0x9AA0AA);
	static final Gradient THERMITE = Gradient.of(0, 0xFFFFFF, 0.25F, 0xFFF4D0, 0.6F, 0xFFA040, 1, argb(0xA03008, 0));
	static final Gradient ALUMINA = Gradient.of(0, 0xFFFFFF, 1, 0xD8D8D4);
	static final Gradient STATIC = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC0E8FF, 1, argb(0x4080FF, 0));
	static final Gradient STORM = Gradient.of(0, 0x5A6070, 1, 0x4A5060);
	static final Gradient FULGURITE = Gradient.of(0, 0xFFFFFF, 0.3F, 0x90E0FF, 1, argb(0x2060C0, 0));
	static final Gradient SOIL = Gradient.of(0, 0x6A5438, 1, 0x8A7458);

	static void register() {
		godfall();
		disco();
		glitter();
		pumpkins();
		cobweb();
		balloon();
		sprinkler();
		thermite();
		tesla();
		sinkhole();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	private static EmitterDef.Builder feathers(String name, float count, float radius) {
		return EmitterDef.builder(name).burst(count).shape(Shape.sphere(radius)).velocity(Velocity.up(0.1F, 0.4F, 1.2F)).gravity(0.004F).drag(0.06F)
				.module(Module.curl(4, 0.006F)).life(120, 200).size(0.35F, 0.55F).layer(Layer.SMOKE).sprite(FxSprites.WISP).color(FEATHER)
				.emissive(Curve.constant(0.5F)).spin(0.03F, 0.08F);
	}

	// ------------------------------------------------------------------ Godfall

	private static void godfall() {
		FxEngine.register(EffectDef.builder(VfxIds.SPEAR_STRIKE).lod(160, 1200).maxAge(240)
				.cue(0, EffectDef.light(0.8F, 0xFFE0A0))
				.emit(flash("flash", 10, 6, 0xFFF0C0).build())
				.emit(EmitterDef.builder("light").burst(40).velocity(Velocity.up(0.4F, 1.2F, 0.6F)).drag(0.05F).life(20, 40).size(0.2F, 0.35F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(HOLY).build())
				.emit(feathers("feathers", 14, 1.5F).build())
				.emit(smoke("dust").burst(10).onGround().shape(Shape.ring(1.5F, 1)).velocity(Velocity.radial(0.15F, 0.3F)).life(40, 70).size(1.5F, 2.5F)
						.color(Gradient.of(0, 0xE8DCC0, 1, 0xC8BCA0)).alpha(0.5F).build())
				.emit(groundRing("halo", 9, 22, 0xFFE8A0, 0.6F))
				.build());

		// The great spear: a column of light climbing out of the temple and feathers over all of it.
		FxEngine.register(EffectDef.builder(VfxIds.GODFALL_IMPACT).lod(400, 3000).maxAge(420)
				.cue(0, EffectDef.light(1.5F, 0xFFE0A0))
				.emit(flash("flash", 60, 14, 0xFFF4D0).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).rate(16, 160, Curve.FALL).shape(Shape.disc(7)).velocity(Velocity.up(1.0F, 2.0F, 0.03F))
						.life(40, 80).size(0.5F, 0.9F).sprite(FxSprites.FLARE).color(HOLY).alphaOverLife(TWINKLE).build())
				.emit(feathers("feathers", 0, 40).rate(6, 220).offset(0, 30, 0).build())
				.emit(smoke("dust").priority(Priority.HERO).rate(24, 30, Curve.FALL).onGround().shape(Shape.ring(10, 4)).expand(1.6F)
						.velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F)).life(100, 160).size(5, 8)
						.color(Gradient.of(0, 0xF0E4C8, 1, 0xC8BCA0)).alpha(0.55F).shade(0.85F).emissive(Curve.keys(0, 0.6F, 1, 0.1F)).build())
				.emit(groundRing("halo", 60, 40, 0xFFE8A0, 0.5F))
				.build());
	}

	// ------------------------------------------------------------------ Disco

	private static void disco() {
		// One beat, tinted the beat's colour: glints from a mirror ball dance over everything, the fog is lit from below.
		FxEngine.register(EffectDef.builder(VfxIds.DISCO_BEAT).tint(0xFF40FF).lod(96, 400)
				.emit(EmitterDef.builder("glints").burst(40).shape(Shape.dome(9)).life(6, 10).size(0.18F, 0.3F).fixedRotation().sprite(FxSprites.FLARE)
						.color(WHITE).tinted().alphaOverLife(FLICKER).build())
				.emit(smoke("fog").burst(2).onGround().shape(Shape.disc(8)).velocity(Velocity.radial(0.01F, 0.03F)).life(50, 70).size(2.5F, 3.5F)
						.color(Gradient.of(0, 0xFFFFFF, 1, 0xE0E0F0)).tinted().alpha(0.12F).shade(1.0F).emissive(Curve.constant(0.3F)).build())
				.build());
	}

	// ------------------------------------------------------------------ Glitter

	private static void glitter() {
		// A handful of glitter: flat flakes tumbling down, catching the light in every colour.
		FxEngine.register(EffectDef.builder(VfxIds.GLITTER_FLAKES).lod(96, 400)
				.emit(EmitterDef.builder("flakes").burst(60).shape(Shape.disc(7)).offset(0, 6, 0).velocity(Velocity.up(-0.04F, 0.02F, 1.0F)).gravity(0.002F)
						.drag(0.05F).module(Module.curl(3, 0.004F)).life(80, 140).size(0.09F, 0.14F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(HOLO)
						.emissive(Curve.constant(0.9F)).alphaOverLife(TWINKLE).spin(0.15F, 0.4F).collide(0, 0.9F, 1).build())
				.emit(EmitterDef.builder("sparkles").burst(10).shape(Shape.disc(7)).offset(0, 3, 0).life(4, 10).size(0.18F, 0.3F).sprite(FxSprites.FLARE)
						.color(HOLO).alphaOverLife(FLICKER).build())
				.build());

		// The opening pop.
		FxEngine.register(EffectDef.builder(VfxIds.GLITTER_POP).lod(96, 400)
				.emit(EmitterDef.builder("burst").burst(240).shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.3F, 0.8F)).velocity(Velocity.up(0.1F, 0.4F, 1))
						.drag(0.08F).gravity(0.004F).module(Module.curl(3, 0.004F)).life(100, 160).size(0.06F, 0.1F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(HOLO).emissive(Curve.constant(0.9F)).alphaOverLife(TWINKLE).spin(0.15F, 0.4F).build())
				.build());
	}

	// ------------------------------------------------------------------ Pumpkin Patch

	private static void pumpkins() {
		FxEngine.register(EffectDef.builder(VfxIds.PUMPKIN_SPROUT).lod(64, 300)
				.emit(EmitterDef.builder("soil").burst(8).velocity(Velocity.up(0.15F, 0.3F, 0.7F)).gravity(0.04F).life(14, 24).size(0.08F, 0.14F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(SOIL).build())
				.emit(EmitterDef.builder("wisp").burst(2).offset(0, 0.6F, 0).velocity(Velocity.up(0.03F, 0.06F, 0.3F)).module(Module.curl(2, 0.004F))
						.life(30, 50).size(0.35F, 0.5F).sprite(FxSprites.WISP).color(SOUL_FLAME).build())
				.build());

		// Creeping fog over the patch with will-o'-the-wisps bobbing in it.
		FxEngine.register(EffectDef.builder(VfxIds.PUMPKIN_FOG).lod(96, 400).maxAge(400)
				.emit(smoke("fog").rate(3, 260).onGround().shape(Shape.disc(10)).velocity(Velocity.radial(0.01F, 0.03F)).life(100, 160).size(3, 5)
						.color(SPOOK_FOG).alpha(0.13F).shade(0.9F).emissive(Curve.constant(0.2F)).build())
				.emit(EmitterDef.builder("wisps").rate(0.15F, 260).onGround().shape(Shape.disc(9)).offset(0, 1.2F, 0).module(Module.curl(3, 0.004F))
						.life(60, 120).size(0.25F, 0.35F).sprite(FxSprites.GLOW).color(WISP_ORANGE).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Cobweb

	private static void cobweb() {
		FxEngine.register(EffectDef.builder(VfxIds.WEB_BURST).lod(64, 300).maxAge(160)
				.emit(EmitterDef.builder("threads").priority(Priority.HERO).burst(60).shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.5F, 0.9F)).drag(0.12F)
						.life(16, 26).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(10).sprite(FxSprites.STREAK).color(SILK).build())
				.emit(EmitterDef.builder("fluff").burst(30).shape(Shape.sphere(5)).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.001F)
						.module(Module.curl(3, 0.004F)).life(80, 140).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.WISP).color(FEATHER).alpha(0.6F)
						.build())
				.emit(10, EmitterDef.builder("spiders").burst(14).onGround().shape(Shape.disc(4)).offset(0, 0.1F, 0).velocity(Velocity.radial(0.08F, 0.16F))
						.module(Module.jitter(0.03F)).life(40, 70).size(0.08F, 0.12F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x201818)
						.build())
				.build());
	}

	// ------------------------------------------------------------------ Balloon

	private static void balloon() {
		// Helium shimmer under the bouquet as it lifts.
		FxEngine.register(EffectDef.builder(VfxIds.BALLOON_LIFT).lod(64, 300)
				.emit(EmitterDef.builder("shimmer").rate(3, 40).shape(Shape.disc(4)).offset(0, 1, 0).velocity(Velocity.up(0.1F, 0.2F, 0.2F)).life(20, 40)
						.size(0.1F, 0.16F).sprite(FxSprites.FLARE).color(WHITE).alpha(0.6F).alphaOverLife(TWINKLE).build())
				.build());

		// One balloon pops into rubber shreds of its own colour.
		FxEngine.register(EffectDef.builder(VfxIds.BALLOON_POP).tint(0xFF4060).lod(64, 300)
				.emit(flash("pop", 1.2F, 3, 0xFFFFFF).build())
				.emit(EmitterDef.builder("shreds").burst(16).shape(Shape.sphere(0.4F)).velocity(Velocity.random(0.2F, 0.45F)).gravity(0.02F).drag(0.06F)
						.module(Module.curl(2, 0.006F)).life(50, 80).size(0.1F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(WHITE).tinted()
						.emissive(Curve.constant(0.4F)).spin(0.2F, 0.4F).build())
				.emit(smoke("puff").burst(3).velocity(Velocity.random(0.02F, 0.05F)).life(14, 22).size(0.5F, 0.8F).color(WHITE).alpha(0.4F).build())
				.build());
	}

	// ------------------------------------------------------------------ Sprinkler

	private static void sprinkler() {
		// A few ticks of rain from a soft grey cloud, splashing where it lands (played every few ticks).
		EmitterDef splash = EmitterDef.builder("splash").burst(3).velocity(Velocity.up(0.06F, 0.14F, 0.7F)).gravity(0.03F).life(6, 10)
				.size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RAIN).build();
		FxEngine.register(EffectDef.builder(VfxIds.SPRINKLER_RAIN).lod(64, 300)
				.emit(smoke("cloud").burst(3).shape(Shape.disc(4)).offset(0, 6, 0).velocity(Velocity.random(0.005F, 0.015F)).life(30, 50).size(2.5F, 3.5F)
						.color(RAINCLOUD).alpha(0.7F).shade(0.75F).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("rain").burst(30).shape(Shape.disc(5)).offset(0, 5.5F, 0).velocity(Velocity.up(-0.6F, -0.5F, 0.02F))
						.collide(0, 0, 1).onHit(0.5F, splash).life(14, 20).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
						.color(RAIN).build())
				.build());
	}

	// ------------------------------------------------------------------ Thermite

	private static void thermite() {
		// The burn, for 50 ticks: a white-hot core you can't look at, a fountain of bouncing sparks, white smoke.
		EmitterDef spit = EmitterDef.builder("spit").burst(2).velocity(Velocity.up(0.05F, 0.15F, 1)).gravity(0.04F).life(4, 8).size(0.03F, 0.05F)
				.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(THERMITE).build();
		FxEngine.register(EffectDef.builder(VfxIds.THERMITE_BURN).lod(128, 900).maxAge(160)
				.cue(0, EffectDef.light(1.0F, 0xFFF0D0))
				.emit(EmitterDef.builder("core").priority(Priority.HERO).rate(1, 50).offset(0, 0.4F, 0).life(3, 4).size(2.2F, 2.8F).sprite(FxSprites.GLOW)
						.color(0xFFFFFF).alpha(0.9F).build())
				.emit(EmitterDef.builder("glare").rate(1, 50).offset(0, 0.4F, 0).life(3, 4).size(7, 9).sprite(FxSprites.GLOW).color(0xFFE8C0).alpha(0.35F).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.HERO).rate(18, 50).shape(Shape.sphere(0.4F)).velocity(Velocity.up(0.3F, 0.8F, 1.0F))
						.gravity(0.04F).collide(0.45F, 0.6F, 3).onHit(0.4F, spit).life(20, 40).size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(2)
						.sprite(FxSprites.STREAK).color(THERMITE).build())
				.emit(smoke("smoke").rate(2, 55).shape(Shape.sphere(0.6F)).velocity(Velocity.up(0.12F, 0.25F, 0.3F)).module(Module.curl(6, 0.006F))
						.life(70, 110).size(1.5F, 2.5F).color(ALUMINA).alpha(0.6F).shade(0.85F).build())
				.emit(EmitterDef.builder("melt").rate(2, 50).onGround().shape(Shape.disc(1.5F)).offset(0, 0.05F, 0).mode(Mode.FLAT).fixedRotation()
						.life(20, 40).size(0.6F, 1.0F).sprite(FxSprites.GLOW).color(THERMITE).alpha(0.7F).build())
				.build());
	}

	// ------------------------------------------------------------------ Tesla

	private static void tesla() {
		// A dark lid of cloud lit from inside, for the whole storm.
		FxEngine.register(EffectDef.builder(VfxIds.TESLA_LID).lod(160, 900).maxAge(220)
				.emit(smoke("cloud").rate(3, 160).shape(Shape.disc(16)).offset(0, 40, 0).velocity(Velocity.random(0.01F, 0.03F)).module(Module.curl(20, 0.004F))
						.life(60, 90).size(7, 11).color(STORM).alpha(0.75F).shade(0.6F).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").rate(0.4F, 160).shape(Shape.disc(14)).offset(0, 40, 0).life(3, 6).size(8, 12).sprite(FxSprites.GLOW)
						.color(0xA0C8FF).alpha(0.4F).alphaOverLife(FLICKER).build())
				.build());

		// A bolt lands: the ground fuses into glowing glass veins running out from the strike, and steams.
		FxEngine.register(EffectDef.builder(VfxIds.FULGURITE).lod(96, 600)
				.emit(flash("flash", 4, 4, 0xC0E0FF).build())
				.emit(EmitterDef.builder("veins").burst(14).onGround().offset(0, 0.1F, 0).shape(Shape.ring(0.3F, 0)).velocity(Velocity.radial(0.25F, 0.45F))
						.drag(0.1F).life(20, 30).size(0.06F, 0.1F).mode(Mode.STRETCHED).stretch(6).sprite(FxSprites.STREAK).color(FULGURITE).build())
				.emit(EmitterDef.builder("sparks").burst(16).velocity(Velocity.up(0.2F, 0.5F, 0.9F)).gravity(0.03F).collide(0.3F, 0.6F, 1).life(8, 16)
						.size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(STATIC).build())
				.emit(smoke("steam").burst(4).velocity(Velocity.up(0.06F, 0.15F, 0.4F)).life(40, 70).size(1.2F, 2).color(0xE0E4EC).alpha(0.4F).build())
				.build());
	}

	// ------------------------------------------------------------------ Sinkhole

	private static void sinkhole() {
		// The ground pours in, played at the pit's bottom: dust and soil sliding down the funnel, clods tumbling in.
		FxEngine.register(EffectDef.builder(VfxIds.SINK_POUR).lod(96, 500)
				.emit(smoke("slide").burst(6).shape(Shape.ring(7, 1.5F)).offset(0, 6, 0).module(Module.attract(0.02F, 1)).drag(0.04F).life(40, 60).size(1.5F, 2.5F)
						.color(SOIL).alpha(0.55F).build())
				.emit(EmitterDef.builder("clods").burst(10).shape(Shape.ring(7, 1)).offset(0, 6, 0).velocity(Velocity.radial(-0.15F, -0.08F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.3F)).gravity(0.05F).collide(0.2F, 0.5F, 2).life(30, 50).size(0.15F, 0.3F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(SOIL).spin(0.05F, 0.2F).build())
				.build());

		// The rim gives way: a ring of dust collapsing inward.
		FxEngine.register(EffectDef.builder(VfxIds.SINK_RIM).lod(96, 500)
				.emit(smoke("rim").burst(30).onGround().shape(Shape.ring(9, 1.5F)).velocity(Velocity.radial(-0.12F, -0.05F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.life(50, 80).size(2, 3.5F).color(SOIL).alpha(0.6F).build())
				.emit(groundRing("ring", 12, 16, 0x8A6A40, 0.45F))
				.build());
	}
}
