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
 * Particles for the Dimension Shift (the rift, the shift front, Nether ash), the Firework Havoc (rocket, burst,
 * cascade, fountains, willow) and the Chinese Lantern (rising lanterns, the dragon's sparkle, its golden burst).
 */
final class WonderVfx {
	private WonderVfx() {
	}

	// Dimension Shift
	static final Gradient PORTAL = Gradient.of(0, 0xFFFFFF, 0.3F, 0xD080FF, 0.7F, 0x8030FF, 1, argb(0x300860, 0));
	static final Gradient CRIMSON_SPORE = Gradient.of(0, 0xFF6050, 1, argb(0xA01010, 0));
	static final Gradient WARPED_SPORE = Gradient.of(0, 0x60FFE0, 1, argb(0x108070, 0));
	static final Gradient ASH = Gradient.of(0, 0xC8C0B8, 1, 0x8A8078);
	static final Gradient NETHER_SMOKE = Gradient.of(0, 0x9A3020, 0.5F, 0x6A2818, 1, 0x4A2420);
	static final Gradient OBSIDIAN = Gradient.of(0, 0x2A1840, 1, 0x140A20);
	// Firework Havoc
	static final Gradient ROCKET_SPARK = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFE0A0, 0.7F, 0xFF8030, 1, argb(0x802008, 0));
	static final Gradient SMOKE_WHITE = Gradient.of(0, 0xF0F0F0, 1, 0xB8B8B8);
	static final Gradient WILLOW = Gradient.of(0, 0xFFFFE0, 0.3F, 0xFFD060, 0.7F, 0xE09020, 1, argb(0x804008, 0));
	static final int[] STAR_COLORS = {0xFF3040, 0xFF9020, 0xFFE040, 0x40FF70, 0x30C0FF, 0x6050FF, 0xE040FF, 0xFFFFFF};
	// Chinese Lantern
	static final Gradient LANTERN = Gradient.of(0, 0xFF8030, 0.5F, 0xF05020, 1, 0xE04018);
	static final Gradient GOLD = Gradient.of(0, 0xFFFFE0, 0.4F, 0xFFD060, 1, argb(0xE09020, 0));

	static Gradient star(int rgb) {
		return Gradient.of(0, 0xFFFFFF, 0.2F, rgb, 0.8F, rgb, 1, argb(rgb, 0));
	}

	static void register() {
		dimensionShift();
		fireworkHavoc();
		chineseLantern();
	}

	// ------------------------------------------------------------------ Dimension Shift

	private static void dimensionShift() {
		// The rift: portal light swirling into a tear in the air, obsidian shards and a violet glow.
		FxEngine.register(EffectDef.builder(VfxIds.DIM_RIFT).lod(200, 1400).maxAge(120)
				.cue(0, EffectDef.light(1.0F, 0x8030FF))
				.emit(flash("flash", 10, 8, 0xC080FF).build())
				.emit(EmitterDef.builder("swirl").priority(Priority.HERO).rate(30, 50).shape(Shape.shell(9)).velocity(Velocity.swirl(0.2F, 0.4F))
						.module(Module.attract(0.03F, 1)).module(Module.vortex(0.04F, 0)).life(30, 50).size(0.18F, 0.3F).sprite(FxSprites.FLARE).color(PORTAL)
						.build())
				.emit(EmitterDef.builder("tear").rate(2, 50).shape(Shape.sphere(1)).life(6, 10).size(4, 6).sprite(FxSprites.GLOW).color(0xA040FF).alpha(0.5F)
						.alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("shards").burst(40).velocity(Velocity.random(0.3F, 0.7F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(40, 70)
						.size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(OBSIDIAN).emissive(Curve.constant(0.3F)).spin(0.1F, 0.3F).build())
				.build());

		// The shift front, authored at radius 20: portal light and spores rising off the line, a low red smoke.
		FxEngine.register(EffectDef.builder(VfxIds.DIM_FRONT).lod(200, 1400)
				.emit(EmitterDef.builder("portal").burst(50).onGround().shape(Shape.ring(20, 1)).offset(0, 0.5F, 0).velocity(Velocity.up(0.1F, 0.4F, 0.2F))
						.module(Module.jitter(0.01F)).life(20, 40).size(0.2F, 0.32F).sprite(FxSprites.FLARE).color(PORTAL).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("spores").burst(20).onGround().shape(Shape.ring(20, 2)).offset(0, 1, 0).velocity(Velocity.up(0.03F, 0.1F, 0.6F))
						.module(Module.jitter(0.006F)).life(60, 100).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(CRIMSON_SPORE).build())
				.emit(smoke("smoke").burst(8).onGround().shape(Shape.ring(20, 2)).velocity(Velocity.up(0.06F, 0.15F, 0.3F)).life(50, 80).size(2.5F, 4)
						.color(NETHER_SMOKE).alpha(0.5F).emissive(Curve.keys(0, 0.5F, 1, 0)).build())
				.build());

		// The Nether's air round you: ash falling, crimson and warped spores drifting.
		FxEngine.register(EffectDef.builder(VfxIds.DIM_ASH).lod(48, 160)
				.emit(EmitterDef.builder("ash").burst(16).shape(Shape.box(16, 6, 16)).offset(0, 6, 0).velocity(Velocity.up(-0.04F, -0.01F, 0.6F))
						.module(Module.jitter(0.006F)).life(80, 140).size(0.06F, 0.1F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(ASH).spin(0.05F, 0.15F)
						.build())
				.emit(EmitterDef.builder("crimson").burst(6).shape(Shape.box(14, 5, 14)).offset(0, 3, 0).velocity(Velocity.random(0.005F, 0.02F))
						.module(Module.jitter(0.008F)).life(80, 140).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(CRIMSON_SPORE).build())
				.emit(EmitterDef.builder("warped").burst(4).shape(Shape.box(14, 5, 14)).offset(0, 3, 0).velocity(Velocity.random(0.005F, 0.02F))
						.module(Module.jitter(0.008F)).life(80, 140).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(WARPED_SPORE).build())
				.build());

		// The rift snaps shut: a violet flash, streaks thrown out, a ring of portal light.
		FxEngine.register(EffectDef.builder(VfxIds.DIM_CLOSE).lod(300, 2000)
				.emit(flash("flash", 24, 10, 0xD0A0FF).build())
				.emit(EmitterDef.builder("streaks").priority(Priority.HERO).burst(200).velocity(Velocity.random(1.0F, 2.2F)).drag(0.04F).life(20, 40)
						.size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(PORTAL).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(30, 30).life(24, 24).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xA040FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ Firework Havoc

	private static void fireworkHavoc() {
		// Off the pad: a billow of smoke and a fountain of sparks.
		FxEngine.register(EffectDef.builder(VfxIds.HAVOC_LAUNCH).lod(160, 1000).maxAge(160)
				.emit(flash("flash", 6, 5, 0xFFE0A0).build())
				.emit(smoke("billow").burst(30).onGround().shape(Shape.ring(1.5F, 1)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F))
						.drag(0.04F).life(80, 130).size(3, 5).color(SMOKE_WHITE).alpha(0.6F).shade(0.8F).build())
				.emit(EmitterDef.builder("sparks").rate(10, 30).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(14, 28)
						.size(0.06F, 0.1F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(ROCKET_SPARK).build())
				.build());

		// The rocket climbing (played each tick where it is): a blinding head, sparks pouring down, a white smoke trail.
		FxEngine.register(EffectDef.builder(VfxIds.HAVOC_TRAIL).lod(256, 1600)
				.emit(EmitterDef.builder("head").priority(Priority.HERO).burst(1).life(2, 2).size(2.4F, 2.8F).sprite(FxSprites.GLOW).color(0xFFF0C0).build())
				.emit(EmitterDef.builder("sparks").burst(10).shape(Shape.sphere(0.5F)).velocity(Velocity.up(-0.6F, -0.2F, 0.6F)).gravity(0.03F).life(14, 30)
						.size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ROCKET_SPARK).build())
				.emit(smoke("trail").burst(2).shape(Shape.sphere(0.6F)).velocity(Velocity.random(0.01F, 0.04F)).life(80, 140).size(1.2F, 2)
						.sizeOverLife(Curve.keys(0, 0.4F, 1, 2.4F)).color(SMOKE_WHITE).alpha(0.55F).shade(0.85F).build())
				.emit(EmitterDef.builder("glitter").burst(3).shape(Shape.sphere(1)).velocity(Velocity.random(0.02F, 0.06F)).gravity(0.01F).life(14, 24)
						.size(0.1F, 0.16F).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, argb(0xFFE080, 0))).alphaOverLife(FLICKER).build())
				.build());

		// The burst: one huge peony in eight colours, crackling sub-rockets flung through it, a white heart.
		EmitterDef crackle = EmitterDef.builder("crackle").burst(10).velocity(Velocity.random(0.1F, 0.3F)).drag(0.05F).life(6, 12).size(0.1F, 0.16F)
				.sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, argb(0xFFF0C0, 0))).alphaOverLife(FLICKER).build();
		EmitterDef sparkle = EmitterDef.builder("sparkle").burst(1).life(10, 18).size(0.12F, 0.18F).gravity(0.01F).sprite(FxSprites.FLARE)
				.color(Gradient.of(0, 0xFFFFFF, 1, argb(0xFFE0A0, 0))).alphaOverLife(TWINKLE).build();
		EffectDef.Builder burst = EffectDef.builder(VfxIds.HAVOC_BURST).lod(500, 3000).maxAge(220)
				.cue(0, EffectDef.light(1.6F, 0xFFF0E0))
				.emit(flash("flash", 50, 12, 0xFFFFFF).build())
				.emit(EmitterDef.builder("heart").burst(1).size(30, 30).life(20, 20).sprite(FxSprites.GLOW).color(0xFFF4E0).alpha(0.5F).alphaOverLife(Curve.FADE_OUT)
						.build())
				.emit(EmitterDef.builder("rockets").priority(Priority.HERO).burst(140).shape(Shape.sphere(2)).velocity(Velocity.random(1.6F, 3.0F)).drag(0.03F)
						.gravity(0.02F).life(26, 44).size(0.5F, 0.7F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ROCKET_SPARK)
						.every(1, sparkle).onDeath(1.0F, crackle).build());
		for (int k = 0; k < STAR_COLORS.length; k++) {
			burst.emit(EmitterDef.builder("stars" + k).priority(Priority.HERO).burst(110).shape(Shape.sphere(2)).velocity(Velocity.random(2.6F, 3.4F))
					.drag(0.045F).gravity(0.012F).life(45, 70).size(0.9F, 1.3F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
					.color(star(STAR_COLORS[k])).build());
			burst.emit(EmitterDef.builder("heads" + k).burst(40).shape(Shape.sphere(2)).velocity(Velocity.random(2.6F, 3.4F)).drag(0.045F).gravity(0.012F)
					.life(40, 60).size(1.4F, 2.0F).sprite(FxSprites.FLARE).color(star(STAR_COLORS[k])).alphaOverLife(TWINKLE).build());
		}
		FxEngine.register(burst.build());

		// A ground fountain / roman candle: a gush of coloured sparks and a puff of smoke.
		EffectDef.Builder fountain = EffectDef.builder(VfxIds.HAVOC_FOUNTAIN).lod(160, 1000).maxAge(80)
				.emit(smoke("smoke").burst(6).onGround().velocity(Velocity.up(0.1F, 0.25F, 0.4F)).life(50, 80).size(1.5F, 2.5F).color(SMOKE_WHITE).alpha(0.5F).build());
		for (int k = 0; k < 4; k++) {
			fountain.emit(EmitterDef.builder("gush" + k).rate(3, 20).velocity(Velocity.up(0.6F, 1.1F, 0.25F)).gravity(0.04F).collide(0.3F, 0.6F, 1)
					.life(20, 34).size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(star(STAR_COLORS[(k * 3) % STAR_COLORS.length])).build());
		}
		FxEngine.register(fountain.build());

		// The finale: a golden willow, drooping trails hanging over everything.
		EmitterDef willowGlitter = EmitterDef.builder("glitter").burst(1).life(20, 40).size(0.4F, 0.6F).gravity(0.004F).sprite(FxSprites.FLARE).color(WILLOW)
				.alphaOverLife(TWINKLE).build();
		FxEngine.register(EffectDef.builder(VfxIds.HAVOC_WILLOW).lod(500, 3000).maxAge(260)
				.cue(0, EffectDef.light(1.4F, 0xFFD080))
				.emit(flash("flash", 40, 12, 0xFFE8B0).build())
				.emit(EmitterDef.builder("willow").priority(Priority.HERO).burst(500).shape(Shape.sphere(2)).velocity(Velocity.random(1.2F, 2.0F)).drag(0.06F)
						.gravity(0.02F).life(80, 120).size(0.8F, 1.1F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WILLOW)
						.every(2, willowGlitter).build())
				.build());
	}

	// ------------------------------------------------------------------ Chinese Lantern

	private static void chineseLantern() {
		// A few lanterns let go (played every couple of ticks over the area): warm paper glows rising and swaying.
		FxEngine.register(EffectDef.builder(VfxIds.LANTERN_RISE).lod(400, 2400)
				.emit(EmitterDef.builder("lanterns").priority(Priority.HERO).burst(2).onGround().shape(Shape.disc(40)).offset(0, 1.2F, 0)
						.velocity(Velocity.up(0.08F, 0.14F, 0.1F)).module(Module.jitter(0.004F)).module(Module.wind(0.25F)).life(340, 420)
						.size(0.8F, 1.05F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(LANTERN).alpha(0.95F).shade(1).emissive(Curve.ONE)
						.alphaOverLife(Curve.of(x -> (0.85 + 0.15 * Math.sin(x * 90)) * Math.min(1, x * 20) * Math.min(1, (1 - x) * 6))).build())
				.emit(EmitterDef.builder("flames").burst(2).onGround().shape(Shape.disc(40)).offset(0, 1.0F, 0).velocity(Velocity.up(0.08F, 0.14F, 0.1F))
						.module(Module.jitter(0.004F)).module(Module.wind(0.25F)).life(340, 420).size(0.3F, 0.4F).sprite(FxSprites.GLOW).color(0xFFE8A0)
						.alphaOverLife(Curve.of(x -> (0.7 + 0.3 * Math.sin(x * 140)) * Math.min(1, x * 20) * Math.min(1, (1 - x) * 6))).build())
				.build());

		// Off the dragon as it flies (played moving): gold glitter drifting down from its body.
		FxEngine.register(EffectDef.builder(VfxIds.LANTERN_SPARKLE).lod(300, 2000)
				.emit(EmitterDef.builder("glitter").burst(3).shape(Shape.sphere(1.5F)).inherit(0.2F).velocity(Velocity.random(0.01F, 0.04F)).gravity(0.004F)
						.life(30, 60).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.build());

		// The dragon bursts into gold: a golden peony, glitter raining, and a last flight of lanterns.
		FxEngine.register(EffectDef.builder(VfxIds.LANTERN_BURST).lod(500, 3000).maxAge(260)
				.cue(0, EffectDef.light(1.4F, 0xFFD060))
				.emit(flash("flash", 40, 12, 0xFFF0C0).build())
				.emit(EmitterDef.builder("peony").priority(Priority.HERO).burst(500).shape(Shape.sphere(2)).velocity(Velocity.random(1.4F, 2.4F)).drag(0.05F)
						.gravity(0.012F).life(50, 80).size(0.9F, 1.3F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WILLOW).build())
				.emit(10, EmitterDef.builder("glitter").rate(20, 120).shape(Shape.sphere(40)).velocity(Velocity.up(-0.08F, -0.03F, 0.3F)).life(60, 100)
						.size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("red").burst(200).shape(Shape.sphere(2)).velocity(Velocity.random(0.8F, 1.6F)).drag(0.05F).gravity(0.01F).life(40, 70)
						.size(0.8F, 1.1F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(star(0xFF3020)).build())
				.build());
	}
}
