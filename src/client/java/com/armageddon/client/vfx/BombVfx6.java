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
 * Placed bombs, the last nine: the showpieces. Each is several layers choreographed in time.
 * <ul>
 * <li>Seasons: each season is a living scene over the whole radius. Spring: cherry petals in gusts, pollen glinting,
 * butterflies. Summer: fireflies blinking, dandelion seeds lifting, a shimmer of heat. Autumn: leaves in four colours
 * spinning down in swirls, a low mist. Winter: big slow flakes on the wind and frost glitter. Then a century passes:
 * a sepia wave of the dust of ages rolls out.</li>
 * <li>Prism: every beam scorches the ground in its own colour, molten glass and rising motes; at the end seven colours
 * are drawn in, and the burst comes apart like light through a prism, seven shells, red outrunning violet.</li>
 * <li>Geode: as it resonates, a standing wave of violet motes shivers over the dome; every shard lands in splinters and
 * chimes; it shatters in amethyst, light rays and lilac dust.</li>
 * <li>Fossil: soil sliding off the bones as they rise, amber spores lifting; every roar blasts a cone of dust and stone
 * out of its jaws in a hot red breath.</li>
 * <li>Lava Lamp: heat shimmer off the cap, steam jetting from each cracking pane, then the glass bursts into glittering
 * shards and goo globs trailing drips, landing in molten splats that sizzle.</li>
 * <li>Ion Scythe: plasma sparks and vapour where it carves, the air above the trench ionising as it charges, and on
 * ignition a curtain of light rising off the whole ring with a shock of dust.</li>
 * <li>Goo, Cactus, Blackforge (already engine-drawn) gain a climax each: an acid storm after the goo bursts, a desert
 * superbloom when the cactus flowers, and a sparkfall over the forge strike.</li>
 * </ul>
 */
final class BombVfx6 {
	private BombVfx6() {
	}

	// Seasons
	static final Gradient PETAL = Gradient.of(0, 0xFFD0E4, 0.5F, 0xF8A8C8, 1, 0xFFE8F0);
	static final Gradient POLLEN = Gradient.of(0, 0xFFFFE0, 0.5F, 0xFFE880, 1, argb(0xE0C040, 0));
	static final Gradient BUTTERFLY = Gradient.of(0, 0xFFA0E0, 0.25F, 0x80C0FF, 0.5F, 0xFFE060, 0.75F, 0xA0FF90, 1, 0xFFA0E0);
	static final Gradient FIREFLY = Gradient.of(0, 0xF0FF90, 0.5F, 0xC8F060, 1, argb(0x80C030, 0));
	static final Gradient SEED = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0E8);
	static final Gradient LEAVES = Gradient.of(0, 0xE04010, 0.33F, 0xF08020, 0.66F, 0xE0B030, 1, 0x8A5020);
	static final Gradient SNOW = Gradient.of(0, 0xFFFFFF, 1, 0xF0F4FA);
	static final Gradient FROST = Gradient.of(0, 0xFFFFFF, 0.5F, 0xD8F0FF, 1, argb(0xA0D0F0, 0));
	static final Gradient SEPIA = Gradient.of(0, 0xD8C8A0, 0.5F, 0xB8A078, 1, 0x8A7858);
	// Prism
	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient GLASS = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0F0FF, 1, argb(0xC0D0FF, 0));
	// Geode
	static final Gradient AMETHYST = Gradient.of(0, 0xFFFFFF, 0.3F, 0xE0B8FF, 0.7F, 0xA060E0, 1, argb(0x6030A0, 0));
	static final Gradient LILAC = Gradient.of(0, 0xE8D8F8, 1, 0xC0A8E0);
	// Fossil
	static final Gradient SOIL = Gradient.of(0, 0x6A5438, 1, 0x8A7458);
	static final Gradient AMBER = Gradient.of(0, 0xFFF0B0, 0.5F, 0xFFB030, 1, argb(0xC06010, 0));
	static final Gradient BREATH = Gradient.of(0, 0xC08070, 0.5F, 0x9A7060, 1, 0x7A6050);
	static final Gradient STONE = Gradient.of(0, 0x8A8680, 1, 0xA8A49C);
	// Lava Lamp
	static final Gradient GOO = Gradient.of(0, 0xFFE0A0, 0.3F, 0xFF8030, 0.7F, 0xE04010, 1, argb(0x601004, 0));
	static final Gradient AMBER_GLASS = Gradient.of(0, 0xFFFFFF, 0.4F, 0xFFC080, 1, argb(0xFF8020, 0));
	static final Gradient STEAM = Gradient.of(0, 0xF0ECE8, 1, 0xC8C4C0);
	// Ion Scythe
	static final Gradient ION = Gradient.of(0, 0xFFFFFF, 0.3F, 0xB0F0FF, 0.7F, 0x60C0FF, 1, argb(0x2060C0, 0));
	// Goo, Cactus, Blackforge climaxes
	static final Gradient ACID = Gradient.of(0, 0xE0FF90, 0.5F, 0x9AFF40, 1, argb(0x40A010, 0));
	static final Gradient ACID_CLOUD = Gradient.of(0, 0x9AC060, 0.5F, 0x6A9040, 1, 0x4A6030);
	static final Gradient BLOOM = Gradient.of(0, 0xFF60A0, 0.25F, 0xFFE040, 0.5F, 0xFF8030, 0.75F, 0xE060FF, 1, 0xFFFFFF);
	static final Gradient FORGE = Gradient.of(0, 0xFFFFE0, 0.2F, 0xFFD070, 0.55F, 0xFF7020, 1, argb(0x601004, 0));

	static void register() {
		seasons();
		prism();
		geode();
		fossil();
		lavaLamp();
		ionScythe();
		climaxes();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	// ------------------------------------------------------------------ Seasons

	private static void seasons() {
		// All four are played every few ticks over the radius (48) while their season lasts.
		FxEngine.register(EffectDef.builder(VfxIds.SEASON_SPRING).lod(128, 600)
				.emit(EmitterDef.builder("petals").burst(30).shape(Shape.disc(44)).offset(0, 9, 0).velocity(Velocity.swirl(0.05F, 0.12F)).velocity(Velocity.up(-0.06F, -0.02F, 0.4F))
						.drag(0.02F).module(Module.curl(6, 0.01F)).life(100, 160).size(0.2F, 0.32F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PETAL)
						.emissive(Curve.constant(0.4F)).spin(0.1F, 0.25F).build())
				.emit(EmitterDef.builder("pollen").burst(10).onGround().shape(Shape.disc(40)).offset(0, 1.5F, 0).velocity(Velocity.up(0.01F, 0.03F, 0.5F))
						.module(Module.jitter(0.004F)).life(60, 110).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(POLLEN).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("butterflies").burst(1).onGround().shape(Shape.disc(36)).offset(0, 1.5F, 0).velocity(Velocity.random(0.05F, 0.1F))
						.module(Module.jitter(0.05F)).module(Module.curl(3, 0.02F)).life(80, 140).size(0.3F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.WISP)
						.color(BUTTERFLY).emissive(Curve.constant(0.5F)).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SEASON_SUMMER).lod(128, 600)
				.emit(EmitterDef.builder("fireflies").burst(8).onGround().shape(Shape.disc(44)).offset(0, 1.2F, 0).velocity(Velocity.random(0.01F, 0.03F))
						.module(Module.jitter(0.01F)).life(60, 120).size(0.18F, 0.26F).sprite(FxSprites.GLOW).color(FIREFLY)
						.alphaOverLife(Curve.of(x -> Math.max(0, Math.sin(x * 19)) * (1 - x))).build())
				.emit(EmitterDef.builder("seeds").burst(6).onGround().shape(Shape.disc(44)).offset(0, 0.6F, 0).velocity(Velocity.up(0.03F, 0.07F, 0.6F))
						.module(Module.curl(5, 0.006F)).life(100, 160).size(0.12F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.WISP).color(SEED)
						.emissive(Curve.constant(0.4F)).build())
				.emit(smoke("heat").burst(2).onGround().shape(Shape.disc(40)).velocity(Velocity.up(0.04F, 0.08F, 0.2F)).life(60, 90).size(4, 6)
						.color(Gradient.of(0, 0xFFF4E0, 1, 0xF0E8D8)).alpha(0.03F).shade(1).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SEASON_AUTUMN).lod(128, 600)
				.emit(EmitterDef.builder("leaves").burst(24).shape(Shape.disc(44)).offset(0, 10, 0).velocity(Velocity.swirl(0.08F, 0.18F)).velocity(Velocity.up(-0.08F, -0.04F, 0.4F))
						.drag(0.02F).module(Module.curl(5, 0.012F)).collide(0, 0.9F, 1).life(120, 180).size(0.16F, 0.26F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(LEAVES).emissive(Curve.constant(0.2F)).spin(0.15F, 0.35F).build())
				.emit(smoke("mist").burst(2).onGround().shape(Shape.disc(44)).velocity(Velocity.radial(0.01F, 0.03F)).life(100, 160).size(5, 8)
						.color(Gradient.of(0, 0xE0D8C8, 1, 0xC8C0B0)).alpha(0.06F).shade(0.9F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SEASON_WINTER).lod(128, 600)
				.emit(EmitterDef.builder("flakes").burst(40).shape(Shape.disc(46)).offset(0, 14, 0).velocity(Velocity.up(-0.1F, -0.06F, 0.3F)).module(Module.wind(0.6F))
						.module(Module.jitter(0.005F)).life(120, 200).size(0.16F, 0.26F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(SNOW)
						.emissive(Curve.constant(0.6F)).spin(0.05F, 0.12F).build())
				.emit(EmitterDef.builder("frost").burst(10).onGround().shape(Shape.disc(40)).offset(0, 0.5F, 0).life(20, 40).size(0.1F, 0.16F)
						.sprite(FxSprites.FLARE).color(FROST).alphaOverLife(TWINKLE).build())
				.build());

		// A century passes: a sepia wave of the dust of ages rolls out, motes of old light rising off everything.
		FxEngine.register(EffectDef.builder(VfxIds.CENTURY_WAVE).lod(256, 1600).maxAge(260)
				.emit(flash("flash", 16, 10, 0xF0E0B0).build())
				.emit(smoke("ages").priority(Priority.HERO).rate(40, 24, Curve.FALL).onGround().shape(Shape.ring(6, 2)).expand(2.2F)
						.velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F)).life(100, 160).size(4, 7).color(SEPIA).alpha(0.55F)
						.shade(0.85F).build())
				.emit(10, EmitterDef.builder("motes").rate(14, 120).onGround().shape(Shape.disc(46)).velocity(Velocity.up(0.04F, 0.1F, 0.3F)).life(60, 100)
						.size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFF4D0, 1, argb(0xC0A060, 0))).alphaOverLife(TWINKLE).build())
				.emit(groundRing("ring", 50, 40, 0xE8D8B0, 0.4F))
				.build());
	}

	// ------------------------------------------------------------------ Prism

	private static void prism() {
		// Where a beam burns, tinted its colour: molten glass spitting, a pool of its light, motes rising.
		FxEngine.register(EffectDef.builder(VfxIds.PRISM_SCORCH).tint(0xFF2020).lod(160, 1000)
				.emit(EmitterDef.builder("glass").burst(10).velocity(Velocity.up(0.3F, 0.7F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(14, 26)
						.size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(WHITE).tinted().build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.4F, 3).life(14, 20)
						.sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("motes").burst(4).shape(Shape.disc(1.2F)).velocity(Velocity.up(0.1F, 0.25F, 0.2F)).life(20, 40).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(TWINKLE).build())
				.build());

		// The convergence: seven colours drawn into one point, then the burst disperses like light through a prism,
		// seven concentric shells, red fastest and violet slowest, and glass falling out of the sky after.
		int[] colors = {0xFF2020, 0xFF8A10, 0xFFE020, 0x30E030, 0x20A0FF, 0x3040FF, 0xB030FF};
		EffectDef.Builder nova = EffectDef.builder(VfxIds.PRISM_NOVA).lod(400, 3000).maxAge(320)
				.cue(10, EffectDef.light(1.6F, 0xFFFFFF))
				.emit(10, flash("white", 60, 14, 0xFFFFFF).build())
				.emit(40, EmitterDef.builder("glass").rate(16, 120).shape(Shape.disc(70)).offset(0, 30, 0).velocity(Velocity.up(-0.4F, -0.2F, 0.2F)).gravity(0.02F)
						.collide(0.3F, 0.6F, 1).life(60, 90).size(0.15F, 0.25F).mode(Mode.STRETCHED).stretch(1).sprite(FxSprites.FLARE).color(GLASS).build());
		for (int k = 0; k < colors.length; k++) {
			Gradient c = Gradient.of(0, 0xFFFFFF, 0.25F, colors[k], 1, argb(colors[k], 0));
			nova.emit(EmitterDef.builder("in" + k).priority(Priority.HERO).burst(40).shape(Shape.shell(40)).module(Module.attract(0.15F, 2)).life(10, 12)
					.size(0.3F, 0.45F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(c).build());
			float sp = 2.6F - k * 0.22F;
			nova.emit(10, EmitterDef.builder("shell" + k).priority(Priority.HERO).burst(200).shape(Shape.sphere(1)).velocity(Velocity.random(sp, sp + 0.1F))
					.drag(0.02F).life(40, 50).size(0.5F, 0.7F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(c).build());
		}
		FxEngine.register(nova.build());
	}

	// ------------------------------------------------------------------ Geode

	private static void geode() {
		// Resonance, authored at R 14 round the geode's centre: violet motes shiver on the dome, a standing wave.
		FxEngine.register(EffectDef.builder(VfxIds.GEODE_HUM).lod(160, 1000)
				.emit(EmitterDef.builder("wave").burst(60).shape(Shape.dome(15)).module(Module.jitter(0.05F)).life(8, 14).size(0.15F, 0.25F).fixedRotation()
						.sprite(FxSprites.FLARE).color(AMETHYST).alphaOverLife(FLICKER).build())
				.emit(smoke("dust").burst(6).onGround().shape(Shape.ring(15, 1)).velocity(Velocity.up(0.05F, 0.15F, 0.2F)).module(Module.jitter(0.03F)).life(30, 50)
						.size(1.5F, 2.5F).color(LILAC).alpha(0.35F).emissive(Curve.constant(0.3F)).build())
				.build());

		// A shard lands: amethyst splinters, a chime of glints, a violet glow.
		FxEngine.register(EffectDef.builder(VfxIds.GEODE_SHARD).lod(128, 900)
				.emit(EmitterDef.builder("splinters").burst(30).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(20, 40)
						.size(0.12F, 0.22F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(AMETHYST).build())
				.emit(EmitterDef.builder("chime").burst(10).shape(Shape.sphere(1.5F)).life(10, 24).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(AMETHYST)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).offset(0, 0.5F, 0).size(2.5F, 2.5F).life(14, 14).sprite(FxSprites.GLOW).color(0xB070FF).alpha(0.5F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// It shatters: amethyst everywhere, rays of violet light, lilac dust.
		FxEngine.register(EffectDef.builder(VfxIds.GEODE_SHATTER).lod(300, 2000).maxAge(260)
				.cue(0, EffectDef.light(1.3F, 0xC080FF))
				.emit(flash("flash", 30, 12, 0xE0C0FF).build())
				.emit(EmitterDef.builder("rays").priority(Priority.HERO).burst(70).shape(Shape.sphere(2)).velocity(Velocity.random(1.8F, 2.6F)).drag(0.05F)
						.life(14, 22).size(0.6F, 0.9F).mode(Mode.STRETCHED).stretch(6).sprite(FxSprites.STREAK).color(AMETHYST).build())
				.emit(EmitterDef.builder("crystals").priority(Priority.HERO).burst(300).shape(Shape.dome(10)).velocity(Velocity.up(0.6F, 1.6F, 0.9F)).gravity(0.04F)
						.collide(0.3F, 0.6F, 2).life(50, 90).size(0.25F, 0.45F).mode(Mode.STRETCHED).stretch(1.2F).sprite(FxSprites.FLARE).color(AMETHYST).build())
				.emit(smoke("dust").priority(Priority.HERO).burst(40).shape(Shape.dome(10)).velocity(Velocity.radial(0.3F, 0.7F)).velocity(Velocity.up(0.1F, 0.4F, 0.4F))
						.life(120, 180).size(5, 8).color(LILAC).alpha(0.55F).shade(0.85F).emissive(Curve.keys(0, 0.6F, 1, 0.1F)).build())
				.emit(10, EmitterDef.builder("glitter").rate(10, 120).shape(Shape.dome(30)).velocity(Velocity.up(-0.03F, 0.03F, 1)).life(40, 80).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(AMETHYST).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Fossil

	private static void fossil() {
		// The bones rising, authored at radius 12: soil sliding off them and ancient amber spores lifting.
		FxEngine.register(EffectDef.builder(VfxIds.FOSSIL_HEAVE).lod(128, 900)
				.emit(smoke("soil").burst(14).onGround().shape(Shape.disc(12)).velocity(Velocity.up(0.05F, 0.15F, 0.4F)).life(50, 80).size(2.5F, 4).color(SOIL)
						.alpha(0.55F).build())
				.emit(EmitterDef.builder("clods").burst(20).onGround().shape(Shape.disc(12)).velocity(Velocity.up(0.2F, 0.45F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(20, 40).size(0.15F, 0.28F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(SOIL).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("amber").burst(8).onGround().shape(Shape.disc(12)).offset(0, 1, 0).velocity(Velocity.up(0.04F, 0.1F, 0.3F))
						.module(Module.curl(4, 0.006F)).life(60, 110).size(0.1F, 0.16F).sprite(FxSprites.GLOW).color(AMBER).alphaOverLife(TWINKLE).build())
				.build());

		// A roar, aimed down the heading: a cone of dust and stone blasted out of the jaws in a hot red breath.
		FxEngine.register(EffectDef.builder(VfxIds.FOSSIL_ROAR).lod(256, 1600).maxAge(200)
				.emit(smoke("breath").priority(Priority.HERO).rate(20, 16, Curve.FALL).shape(Shape.sphere(2)).velocity(Velocity.along(1.0F, 1.8F, 0.45F))
						.drag(0.03F).life(60, 100).size(3, 6).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.4F)).color(BREATH).alpha(0.55F)
						.emissive(Curve.keys(0, 0.5F, 0.4F, 0)).build())
				.emit(EmitterDef.builder("stone").priority(Priority.HERO).burst(160).shape(Shape.sphere(3)).velocity(Velocity.along(1.2F, 2.4F, 0.5F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(40, 70).size(0.25F, 0.5F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(STONE).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("streaks").burst(80).shape(Shape.sphere(2)).velocity(Velocity.along(2.0F, 3.2F, 0.4F)).drag(0.04F).life(14, 24)
						.size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(4).layer(Layer.SMOKE).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xE8DCC8, 1, argb(0xC0B0A0, 0))).build())
				.build());
	}

	// ------------------------------------------------------------------ Lava Lamp

	private static void lavaLamp() {
		// Heating: shimmer rising off the cap and a few bright bubbles of goo popping at the glass.
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_SIMMER).lod(128, 900)
				.emit(smoke("shimmer").burst(3).shape(Shape.disc(4)).velocity(Velocity.up(0.1F, 0.2F, 0.2F)).module(Module.curl(4, 0.006F)).life(40, 60).size(2, 3)
						.color(Gradient.of(0, 0xFFE8D0, 1, 0xF0E0D0)).alpha(0.12F).shade(1).build())
				.build());

		// A pane cracks: a jet of steam and spitting goo out of the gap.
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_CRACK).lod(128, 900)
				.emit(smoke("steam").burst(6).velocity(Velocity.radial(0.2F, 0.4F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F)).life(30, 50).size(1.2F, 2).color(STEAM)
						.alpha(0.55F).shade(0.9F).build())
				.emit(EmitterDef.builder("spit").burst(8).velocity(Velocity.random(0.2F, 0.4F)).gravity(0.04F).life(12, 22).size(0.1F, 0.16F)
						.sprite(FxSprites.GLOW).color(GOO).build())
				.build());

		// The burst: amber glass glittering everywhere, goo globs flung out trailing drips, steam rolling off.
		EmitterDef drip = EmitterDef.builder("drip").burst(1).life(10, 16).size(0.12F, 0.2F).gravity(0.03F).sprite(FxSprites.GLOW).color(GOO)
				.alphaOverLife(Curve.FADE_OUT).build();
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_BURST).lod(300, 2000).maxAge(240)
				.cue(0, EffectDef.light(1.2F, 0xFF8030))
				.emit(flash("flash", 24, 10, 0xFFC080).build())
				.emit(EmitterDef.builder("glass").priority(Priority.HERO).burst(260).shape(Shape.column(6, 16)).offset(0, -8, 0).velocity(Velocity.radial(0.5F, 1.2F))
						.velocity(Velocity.up(0.1F, 0.6F, 0.6F)).gravity(0.04F).collide(0.4F, 0.6F, 2).life(50, 90).size(0.18F, 0.32F).mode(Mode.STRETCHED).stretch(1)
						.sprite(FxSprites.FLARE).color(AMBER_GLASS).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("globs").priority(Priority.HERO).burst(45).shape(Shape.sphere(3)).velocity(Velocity.up(0.6F, 1.4F, 0.9F)).gravity(0.04F)
						.collide(0.1F, 0.4F, 1).life(40, 70).size(0.9F, 1.4F).sprite(FxSprites.GLOW).color(GOO).every(2, drip).build())
				.emit(smoke("steam").burst(30).shape(Shape.column(6, 16)).offset(0, -8, 0).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.life(80, 130).size(3, 5).color(STEAM).alpha(0.5F).shade(0.85F).build())
				.build());

		// A glob lands: a molten splat with a pool of glow, sizzling steam, and spatter.
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_SPLAT).lod(128, 900)
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.2F, 3).life(60, 80)
						.sprite(FxSprites.GLOW).color(GOO).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("spatter").burst(18).velocity(Velocity.up(0.25F, 0.55F, 0.9F)).gravity(0.05F).collide(0.1F, 0.4F, 1).life(16, 30)
						.size(0.12F, 0.2F).sprite(FxSprites.GLOW).color(GOO).build())
				.emit(smoke("sizzle").burst(5).velocity(Velocity.up(0.08F, 0.18F, 0.4F)).life(40, 70).size(1.2F, 2).color(STEAM).alpha(0.5F).build())
				.build());
	}

	// ------------------------------------------------------------------ Ion Scythe

	private static void ionScythe() {
		// Where it carves: blue-white plasma sparks spitting and ionised vapour.
		FxEngine.register(EffectDef.builder(VfxIds.ION_CARVE).lod(160, 1000)
				.emit(EmitterDef.builder("sparks").burst(14).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(10, 22)
						.size(0.06F, 0.1F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ION).build())
				.emit(smoke("vapour").burst(2).velocity(Velocity.up(0.08F, 0.2F, 0.3F)).life(30, 50).size(1.2F, 2).color(Gradient.of(0, 0xD8E8F4, 1, 0xB0C0D0))
						.alpha(0.4F).emissive(Curve.keys(0, 0.6F, 1, 0)).build())
				.emit(EmitterDef.builder("glow").burst(1).offset(0, 0.3F, 0).size(2, 2).life(6, 6).sprite(FxSprites.GLOW).color(0x80E0FF).alpha(0.5F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Charging, authored at the ring's radius 64: the air over the whole trench ionising, motes lifting.
		FxEngine.register(EffectDef.builder(VfxIds.ION_CHARGE).lod(300, 2000)
				.emit(EmitterDef.builder("motes").burst(80).onGround().shape(Shape.ring(64, 3)).offset(0, 0.5F, 0).velocity(Velocity.up(0.1F, 0.35F, 0.1F))
						.life(30, 50).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(ION).alphaOverLife(TWINKLE).build())
				.build());

		// Ignition: a curtain of light rising off the whole ring, and a shock of dust rolling out from it.
		FxEngine.register(EffectDef.builder(VfxIds.ION_IGNITE).lod(400, 3000).maxAge(220)
				.cue(0, EffectDef.light(1.5F, 0xA0E8FF))
				.emit(EmitterDef.builder("curtain").priority(Priority.HERO).rate(90, 40, Curve.FALL).onGround().shape(Shape.ring(64, 3))
						.velocity(Velocity.up(1.6F, 2.8F, 0.03F)).drag(0.01F).life(30, 50).size(0.5F, 0.8F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
						.color(ION).build())
				.emit(smoke("shock").priority(Priority.HERO).burst(160).onGround().shape(Shape.ring(64, 4)).velocity(Velocity.radial(0.4F, 0.9F))
						.velocity(Velocity.up(0.1F, 0.3F, 0.3F)).life(80, 130).size(4, 7).color(Gradient.of(0, 0xD8E4F0, 1, 0xA8B4C0)).alpha(0.5F).shade(0.8F)
						.emissive(Curve.keys(0, 0.7F, 0.4F, 0)).build())
				.emit(groundRing("inner", 64, 30, 0xA0E8FF, 0.6F))
				.build());
	}

	// ------------------------------------------------------------------ climaxes for the three already on the engine

	private static void climaxes() {
		// After the goo bursts: a sickly green cloud lid over the land and acid rain hissing down out of it.
		EmitterDef hiss = EmitterDef.builder("hiss").burst(2).velocity(Velocity.up(0.04F, 0.1F, 0.6F)).life(14, 24).size(0.3F, 0.5F).layer(Layer.SMOKE)
				.sprite(FxSprites.SMOKE, 6).color(Gradient.of(0, 0xD0F0B0, 1, 0xA0C090)).alpha(0.5F).build();
		FxEngine.register(EffectDef.builder(VfxIds.ACID_STORM).lod(300, 2000).maxAge(420)
				.emit(smoke("lid").priority(Priority.HERO).rate(4, 300).shape(Shape.disc(40)).offset(0, 28, 0).velocity(Velocity.random(0.01F, 0.04F))
						.module(Module.curl(20, 0.004F)).life(80, 120).size(9, 14).color(ACID_CLOUD).alpha(0.42F).shade(0.6F).emissive(Curve.constant(0.15F))
						.alphaOverLife(Curve.FADE).build())
				.emit(20, EmitterDef.builder("rain").rate(30, 260).shape(Shape.disc(36)).offset(0, 26, 0).velocity(Velocity.up(-1.0F, -0.8F, 0.04F)).collide(0, 0, 1)
						.onHit(0.4F, hiss).life(30, 36).size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ACID).build())
				.build());

		// When the cactus flowers: a desert superbloom, petals and pollen blowing out across the sand in every colour.
		FxEngine.register(EffectDef.builder(VfxIds.SUPERBLOOM).lod(300, 2000).maxAge(320)
				.emit(EmitterDef.builder("petals").priority(Priority.HERO).rate(32, 160, Curve.FALL).shape(Shape.sphere(4)).velocity(Velocity.radial(0.2F, 0.5F))
						.velocity(Velocity.up(0.05F, 0.2F, 0.6F)).gravity(0.004F).drag(0.02F).module(Module.wind(0.8F)).module(Module.curl(5, 0.01F)).life(120, 180)
						.size(0.3F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(BLOOM).emissive(Curve.constant(0.5F)).spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("pollen").rate(12, 180).shape(Shape.disc(30)).offset(0, 1, 0).velocity(Velocity.up(0.02F, 0.06F, 0.4F)).module(Module.jitter(0.005F))
						.life(60, 120).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFE0, 1, argb(0xFFD040, 0))).alphaOverLife(TWINKLE).build())
				.build());

		// The forge strike: a sparkfall, a fountain of forge sparks raining for seconds, glowing iron dust settling,
		// and the quench steam rolling off.
		EmitterDef bounce = EmitterDef.builder("bounce").burst(2).velocity(Velocity.up(0.08F, 0.2F, 1)).gravity(0.05F).life(6, 12).size(0.04F, 0.06F)
				.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(FORGE).build();
		FxEngine.register(EffectDef.builder(VfxIds.FORGE_SPARKFALL).lod(400, 3000).maxAge(320)
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).rate(50, 100, Curve.FALL).shape(Shape.disc(3)).velocity(Velocity.up(1.4F, 2.6F, 0.6F))
						.gravity(0.04F).collide(0.4F, 0.6F, 2).onHit(0.3F, bounce).life(60, 100).size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(2)
						.sprite(FxSprites.STREAK).color(FORGE).build())
				.emit(20, EmitterDef.builder("dust").rate(10, 160).shape(Shape.disc(40)).offset(0, 24, 0).velocity(Velocity.up(-0.06F, -0.02F, 0.4F))
						.module(Module.jitter(0.004F)).life(80, 140).size(0.1F, 0.16F).sprite(FxSprites.GLOW).color(FORGE).alphaOverLife(TWINKLE).build())
				.emit(10, smoke("quench").rate(6, 120).onGround().shape(Shape.ring(20, 8)).velocity(Velocity.up(0.2F, 0.4F, 0.3F)).module(Module.curl(20, 0.006F))
						.life(100, 160).size(6, 10).color(Gradient.of(0, 0xF0ECE8, 1, 0xB8B4B0)).alpha(0.45F).shade(0.85F).build())
				.build());
	}
}
