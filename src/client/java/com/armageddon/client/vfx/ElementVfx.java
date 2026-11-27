package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.util.ElementShapes.Downburst;
import com.armageddon.util.ElementShapes.Maelstrom;
import com.armageddon.util.ElementShapes.Stride;
import com.armageddon.util.ElementShapes.Thunder;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the Wrecking Ball, the Flattener, Titan's Stride and the five elemental celestials. */
final class ElementVfx {
	private ElementVfx() {
	}

	static final Gradient DUST = Gradient.of(0, 0x8A7E6C, 1, 0xA89C88);
	static final Gradient CONCRETE = Gradient.of(0, 0x9A968E, 1, 0xB4B0A8);
	static final Gradient EARTH = Gradient.of(0, 0x4A3C2E, 1, 0x6A5C4A);
	static final Gradient FIRE = Gradient.of(0, 0xFFFFE0, 0.2F, 0xFFD060, 0.45F, 0xFF7018, 0.75F, 0xC02808, 1, argb(0x300800, 0));
	static final Gradient EMBER = Gradient.of(0, 0xFFF0B0, 0.3F, 0xFFA040, 0.7F, 0xE04010, 1, argb(0x701008, 0));
	static final Gradient SOOT = Gradient.of(0, 0x2A2220, 0.5F, 0x3E3632, 1, 0x5A524C);
	static final Gradient WATER = Gradient.of(0, 0xF4FCFF, 0.5F, 0xC0E4F4, 1, 0x88C0DC);
	static final Gradient STORM = Gradient.of(0, 0x2A2E36, 0.5F, 0x3A404A, 1, 0x4A505A);
	static final Gradient BOLT = Gradient.of(0, 0xFFFFFF, 0.4F, 0xD0E0FF, 1, argb(0x6080FF, 0));

	static void register() {
		city();
		fire();
		water();
		earthAndAir();
		storm();
	}

	private static EmitterDef.Builder rubble(String name, int rgb) {
		return EmitterDef.builder(name).layer(Layer.SMOKE).sprite(FxSprites.DUST).color(rgb).shade(0.75F);
	}

	private static EmitterDef.Builder shock(float size, int life, int rgb, float alpha) {
		return EmitterDef.builder("shock").burst(1).onGround().offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(rgb).alpha(alpha).alphaOverLife(Curve.FADE_OUT);
	}

	// ------------------------------------------------------------------ city

	private static void city() {
		// The ball smashing through: rubble and glass blown out ahead of it, a cloud of concrete dust.
		FxEngine.register(EffectDef.builder(VfxIds.WRECK_SMASH).lod(400, 2600).maxAge(160)
				.emit(rubble("rubble", 0x8A867E).priority(Priority.HERO).burst(40).shape(Shape.sphere(8)).velocity(Velocity.along(0.6F, 1.4F, 0.6F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.6F)).gravity(0.05F).collide(0.3F, 0.5F, 1).life(30, 60).size(0.3F, 0.7F).spin(-0.4F, 0.4F).build())
				.emit(EmitterDef.builder("glass").burst(20).shape(Shape.sphere(8)).velocity(Velocity.along(0.6F, 1.2F, 0.7F)).gravity(0.04F).collide(0.3F, 0.5F, 1)
						.life(20, 40).size(0.15F, 0.3F).spin(-0.6F, 0.6F).sprite(FxSprites.FLARE).color(0xD8ECF4).build())
				.emit(smoke("dust").burst(10).shape(Shape.sphere(9)).velocity(Velocity.along(0.2F, 0.5F, 0.8F)).module(Module.curl(10, 0.008F)).life(80, 140)
						.size(5, 8).sizeOverLife(Curve.RISE).color(CONCRETE).alpha(0.6F).build())
				.build());

		// The ball landing: a ring of dust and rubble rolling out, a shock across the ground.
		FxEngine.register(EffectDef.builder(VfxIds.WRECK_LAND).lod(800, 5000).maxAge(240)
				.cue(0, EffectDef.light(1.0F, 0xE0E0E0))
				.emit(rubble("rubble", 0x7A766E).priority(Priority.HERO).burst(160).onGround().shape(Shape.disc(14)).velocity(Velocity.up(0.6F, 1.6F, 0.6F))
						.gravity(0.05F).collide(0.3F, 0.5F, 1).life(40, 80).size(0.4F, 0.9F).spin(-0.4F, 0.4F).build())
				.emit(smoke("dust").burst(120).onGround().shape(Shape.ring(16, 6)).velocity(Velocity.radial(1.2F, 2.0F)).velocity(Velocity.up(0.1F, 0.4F, 0.3F))
						.drag(0.03F).life(80, 140).size(6, 10).color(CONCRETE).alpha(0.65F).build())
				.emit(shock(120, 36, 0xE0E0E0, 0.6F).build())
				.build());

		// The roller pushing a wave of dust and wreckage ahead of it.
		FxEngine.register(EffectDef.builder(VfxIds.ROLLER_DUST).lod(400, 2600).maxAge(160)
				.emit(smoke("wave").burst(4).onGround().shape(Shape.disc(4)).velocity(Velocity.along(0.4F, 0.8F, 0.4F)).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.drag(0.04F).module(Module.curl(8, 0.008F)).life(60, 110).size(5, 8).sizeOverLife(Curve.RISE).color(CONCRETE).alpha(0.55F).build())
				.emit(rubble("chips", 0x8A867E).burst(12).onGround().shape(Shape.disc(4)).velocity(Velocity.along(0.4F, 0.9F, 0.6F)).velocity(Velocity.up(0.3F, 0.8F, 0.4F))
						.gravity(0.05F).collide(0.3F, 0.5F, 1).life(20, 40).size(0.2F, 0.45F).spin(-0.5F, 0.5F).build())
				.build());

		// The roller landing: dust blasted out from under its whole length.
		FxEngine.register(EffectDef.builder(VfxIds.ROLLER_LAND).lod(800, 5000).maxAge(240)
				.emit(smoke("dust").priority(Priority.HERO).burst(160).onGround().shape(Shape.box(18, 0, 80)).velocity(Velocity.radial(0.8F, 1.6F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).drag(0.03F).life(80, 140).size(6, 10).color(CONCRETE).alpha(0.65F).build())
				.emit(shock(200, 40, 0xE0E0E0, 0.5F).build())
				.build());

		// The giant's shadow falling: a dark blot growing on the ground where its foot will land.
		FxEngine.register(EffectDef.builder(VfxIds.STRIDE_SHADOW).lod(800, 5000).maxAge(Stride.SHADOW + 10)
				.emit(EmitterDef.builder("shadow").priority(Priority.HERO).burst(1).onGround().offset(0, 0.25F, 0).mode(Mode.FLAT).fixedRotation()
						.size((float) Stride.FOOT_L * 0.9F, (float) Stride.FOOT_L * 0.9F).life(Stride.SHADOW + 4, Stride.SHADOW + 4).layer(Layer.SMOKE)
						.sprite(FxSprites.GLOW).color(0x050508).shade(1).alpha(0.75F).sizeOverLife(Curve.keys(0, 1.6F, 0.85F, 0.75F, 1, 0.7F))
						.alphaOverLife(Curve.keys(0, 0, 0.7F, 1, 1, 1)).emissive(Curve.constant(0)).build())
				.emit(smoke("stir").rate(3, Stride.SHADOW).onGround().shape(Shape.disc((float) Stride.FOOT_L * 0.5F)).velocity(Velocity.up(0.05F, 0.15F, 0.6F))
						.velocity(Velocity.random(0.1F, 0.3F)).life(40, 70).size(3, 5).color(DUST).alpha(0.35F).build())
				.build());

		// The foot coming down: a burst of earth and rubble, a dust wall rolling out, a shock across the city.
		FxEngine.register(EffectDef.builder(VfxIds.STRIDE_STOMP).lod(800, 5000).maxAge(260)
				.cue(0, EffectDef.light(0.6F, 0xE0D8C8))
				.emit(rubble("earth", 0x5A4C3E).priority(Priority.HERO).burst(220).onGround().shape(Shape.ring(14, 8)).velocity(Velocity.radial(0.4F, 1.2F))
						.velocity(Velocity.up(0.6F, 1.6F, 0.4F)).gravity(0.05F).collide(0.3F, 0.5F, 1).life(40, 80).size(0.4F, 0.9F).spin(-0.4F, 0.4F).build())
				.emit(smoke("dust").burst(140).onGround().shape(Shape.ring(16, 6)).velocity(Velocity.radial(1.4F, 2.4F)).velocity(Velocity.up(0.1F, 0.5F, 0.3F))
						.drag(0.03F).life(80, 140).size(6, 11).color(DUST).alpha(0.65F).build())
				.emit(smoke("column").burst(30).onGround().shape(Shape.disc(12)).velocity(Velocity.up(0.4F, 0.9F, 0.2F)).drag(0.03F).module(Module.curl(12, 0.01F))
						.life(100, 160).size(7, 11).sizeOverLife(Curve.RISE).color(DUST).alpha(0.55F).build())
				.emit(shock(160, 40, 0xE8E0D0, 0.55F).build())
				.build());
	}

	// ------------------------------------------------------------------ fire

	private static void fire() {
		// A fire whirl, re-lit every tick where it is: flame wound up a funnel, embers flung off the top, black smoke.
		FxEngine.register(EffectDef.builder(VfxIds.INFERNO_WHIRL).lod(600, 4000)
				.emit(EmitterDef.builder("funnel").priority(Priority.HERO).burst(26).inherit(0.4F).shape(Shape.ring(3, 2)).velocity(Velocity.up(0.8F, 1.6F, 0.15F))
						.module(Module.funnel(40, 3, 12, 1.4F, 0.9F, 0.2F)).life(18, 28).size(1.6F, 3.0F).sizeOverLife(Curve.keys(0, 0.6F, 0.4F, 1.2F, 1, 0.8F))
						.sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(6).inherit(0.3F).shape(Shape.ring(10, 4)).offset(0, 34, 0).velocity(Velocity.radial(0.3F, 0.8F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.4F)).gravity(0.01F).life(30, 60).size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(EMBER)
						.alphaOverLife(FLICKER).build())
				.emit(smoke("smoke").burst(1).inherit(0.2F).shape(Shape.ring(12, 4)).offset(0, 40, 0).velocity(Velocity.up(0.2F, 0.4F, 0.3F)).module(Module.wind(0.03F))
						.life(80, 140).size(8, 12).color(SOOT).alpha(0.45F).emissive(Curve.keys(0, 0.6F, 0.3F, 0, 1, 0)).build())
				.build());

		// The pillar collapsing: a ring of flame racing out over the land, a shock, embers and a pall of smoke.
		FxEngine.register(EffectDef.builder(VfxIds.INFERNO_COLLAPSE).lod(1200, 8000).maxAge(300)
				.cue(0, EffectDef.light(2.6F, 0xFF8030))
				.emit(flash("flash", 90, 20, 0xFFE0A0).offset(0, 20, 0).build())
				.emit(EmitterDef.builder("sea").priority(Priority.HERO).burst(320).onGround().shape(Shape.ring(10, 6)).velocity(Velocity.radial(2.0F, 2.6F))
						.velocity(Velocity.up(0.05F, 0.3F, 0.2F)).drag(0.012F).life(50, 70).size(4, 8).sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(300).shape(Shape.column(14, 60)).velocity(Velocity.radial(0.6F, 1.8F)).velocity(Velocity.up(0.2F, 0.8F, 0.4F))
						.gravity(0.015F).life(60, 120).size(0.25F, 0.5F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(FLICKER).build())
				.emit(shock(280, 60, 0xFFB070, 0.7F).build())
				.emit(smoke("pall").burst(80).shape(Shape.column(18, 50)).velocity(Velocity.radial(0.3F, 0.8F)).velocity(Velocity.up(0.3F, 0.7F, 0.3F)).drag(0.02F)
						.module(Module.curl(18, 0.01F)).life(140, 220).size(10, 16).color(SOOT).alpha(0.6F).emissive(Curve.keys(0, 1, 0.3F, 0.3F, 1, 0)).build())
				.build());
	}

	// ------------------------------------------------------------------ water

	private static void water() {
		// Spray torn off the whirlpool's foam.
		FxEngine.register(EffectDef.builder(VfxIds.MAEL_SPRAY).lod(300, 2000)
				.emit(EmitterDef.builder("spray").burst(10).velocity(Velocity.along(0.4F, 0.9F, 0.5F)).velocity(Velocity.up(0.1F, 0.4F, 0.4F)).gravity(0.04F)
						.life(14, 28).size(0.2F, 0.4F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WATER).build())
				.emit(smoke("mist").burst(1).velocity(Velocity.along(0.1F, 0.3F, 0.5F)).life(40, 70).size(2.5F, 4).color(WATER).alpha(0.3F).shade(1).build())
				.build());

		// The waterspout: a column of white water roaring up a hundred blocks, spreading at the top, spray everywhere.
		FxEngine.register(EffectDef.builder(VfxIds.MAEL_SPOUT).lod(1200, 8000).maxAge(320)
				.emit(EmitterDef.builder("column").priority(Priority.HERO).max(3000).rate(40, 140, Curve.keys(0, 1, 0.7F, 1, 1, 0)).shape(Shape.disc(6))
						.velocity(Velocity.up(2.2F, 2.8F, 0.06F)).gravity(0.02F).drag(0.008F).life(40, 60).size(2, 3.5F).layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6)
						.color(WATER).alpha(0.75F).shade(1).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("spray").max(2000).rate(30, 140).shape(Shape.disc(8)).velocity(Velocity.up(1.4F, 2.4F, 0.35F)).gravity(0.04F).life(30, 60)
						.size(0.3F, 0.6F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WATER).build())
				.emit(smoke("base").burst(80).shape(Shape.ring(10, 6)).velocity(Velocity.radial(1.0F, 1.6F)).velocity(Velocity.up(0.1F, 0.4F, 0.3F)).drag(0.03F)
						.life(80, 140).size(6, 10).color(WATER).alpha(0.5F).shade(1).build())
				.build());

		// The deluge: rain sheeting down over everything.
		FxEngine.register(EffectDef.builder(VfxIds.MAEL_RAIN).lod(600, 4000).maxAge(220)
				.emit(EmitterDef.builder("rain").priority(Priority.HERO).max(4000).rate(45, 160, Curve.FALL).shape(Shape.disc((float) Maelstrom.R + 30))
						.velocity(Velocity.up(-1.8F, -1.4F, 0.08F)).collide(0, 0, 0).life(40, 60).size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(4)
						.sprite(FxSprites.STREAK).color(WATER).alpha(0.7F).build())
				.build());
	}

	// ------------------------------------------------------------------ earth and air

	private static void earthAndAir() {
		// The land cracking into its honeycomb: dust spurting from the cracks, a deep crack of shock.
		FxEngine.register(EffectDef.builder(VfxIds.BASALT_CRACK).lod(800, 5000).maxAge(200)
				.emit(smoke("spurts").burst(80).onGround().shape(Shape.disc(80)).velocity(Velocity.up(0.3F, 0.7F, 0.2F)).life(40, 80).size(2.5F, 4).color(DUST)
						.alpha(0.5F).build())
				.emit(shock(200, 40, 0x8A7A62, 0.45F).build())
				.build());

		// A column bursting up: rock chips and dust.
		FxEngine.register(EffectDef.builder(VfxIds.BASALT_RISE).lod(400, 2600).maxAge(120)
				.emit(rubble("chips", 0x3A3A3E).burst(14).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.5F, 1.2F, 0.5F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(20, 40).size(0.2F, 0.45F).spin(-0.5F, 0.5F).build())
				.emit(smoke("dust").burst(3).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.1F, 0.3F, 0.4F)).life(50, 90).size(3, 5).color(DUST).alpha(0.5F)
						.build())
				.build());

		// The middle caving in: dust pouring down into it, a glow from below.
		FxEngine.register(EffectDef.builder(VfxIds.BASALT_CAVE).lod(800, 5000).maxAge(240)
				.cue(0, EffectDef.light(1.0F, 0xFF6020))
				.emit(smoke("inrush").burst(120).onGround().shape(Shape.ring(22, 6)).velocity(Velocity.radial(-0.8F, -0.4F)).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.drag(0.03F).life(70, 120).size(5, 8).color(DUST).alpha(0.6F).build())
				.emit(EmitterDef.builder("glow").burst(30).onGround().shape(Shape.disc(16)).offset(0, -10, 0).velocity(Velocity.up(0.1F, 0.4F, 0.3F)).life(40, 80)
						.size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(FLICKER).build())
				.build());

		// The sky vortex: a ring of cloud turning over the land the whole time the air is drawn up.
		int life = Downburst.SLAM + 30;
		FxEngine.register(EffectDef.builder(VfxIds.DOWN_SKY).lod(800, 6000).maxAge(life + 160)
				.emit(smoke("ring").priority(Priority.HERO).max(1500).rate(6, life).shape(Shape.ring(60, 30)).velocity(Velocity.swirl(0.6F, 0.9F))
						.module(Module.vortex(0.03F, 0.01F)).life(120, 180).size(14, 22).color(STORM).alpha(0.6F).shade(0.9F).build())
				.emit(smoke("funnel").rate(2, life).shape(Shape.disc(20)).velocity(Velocity.up(-0.3F, -0.1F, 0.2F)).module(Module.vortex(0.06F, 0.02F)).life(80, 140)
						.size(8, 12).color(Gradient.of(0, 0x5A6270, 1, 0x7A8290)).alpha(0.45F).shade(0.9F).build())
				.emit(EmitterDef.builder("streaks").rate(8, life).shape(Shape.disc((float) Downburst.R)).offset(0, (float) -Downburst.SKY, 0)
						.velocity(Velocity.up(0.6F, 1.2F, 0.1F)).life(30, 60).size(0.1F, 0.18F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK)
						.color(0xD8E4F0).alpha(0.5F).build())
				.build());

		// The downburst: air crashing down - streaks raining out of the sky - and a wall of dust thrown out from under it.
		FxEngine.register(EffectDef.builder(VfxIds.DOWN_SLAM).lod(1000, 7000).maxAge(260)
				.emit(EmitterDef.builder("rush").priority(Priority.HERO).burst(500).shape(Shape.disc(70)).offset(0, 80, 0).velocity(Velocity.up(-3.2F, -2.6F, 0.05F))
						.life(24, 34).size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(0xE8F0FF).alpha(0.7F).build())
				.emit(20, smoke("wall").priority(Priority.HERO).burst(220).onGround().shape(Shape.ring(30, 10)).velocity(Velocity.radial(2.2F, 3.0F))
						.velocity(Velocity.up(0.05F, 0.4F, 0.2F)).drag(0.025F).life(80, 140).size(7, 12).color(DUST).alpha(0.65F).build())
				.emit(20, shock(260, 50, 0xE0E8F0, 0.6F).build())
				.build());
	}

	// ------------------------------------------------------------------ storm

	private static void storm() {
		// The supercell: a black anvil of cloud overhead, turning slowly, flickering from inside.
		int life = Thunder.END;
		FxEngine.register(EffectDef.builder(VfxIds.THUNDER_CLOUD).lod(1000, 8000).maxAge(life + 160)
				.emit(smoke("anvil").priority(Priority.HERO).max(1600).rate(8, life - 40, Curve.keys(0, 3, 0.15F, 1, 1, 1)).shape(Shape.disc((float) Thunder.R))
						.velocity(Velocity.swirl(0.05F, 0.15F)).module(Module.vortex(0.01F, 0)).life(160, 240).size(18, 28).color(STORM).alpha(0.75F).shade(0.85F).build())
				.emit(smoke("wall").rate(2, life - 40).shape(Shape.disc(30)).offset(0, -30, 0).velocity(Velocity.up(0.1F, 0.3F, 0.2F)).module(Module.vortex(0.03F, 0.01F))
						.life(120, 180).size(14, 20).color(STORM).alpha(0.6F).shade(0.85F).build())
				.emit(EmitterDef.builder("flicker").rate(1.2F, life - 40).shape(Shape.disc((float) Thunder.R * 0.8F)).life(3, 6).size(30, 50).sprite(FxSprites.GLOW)
						.color(0xB8C8FF).alpha(0.35F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Rain sheeting down under it.
		FxEngine.register(EffectDef.builder(VfxIds.THUNDER_RAIN).lod(600, 4000).maxAge(life + 60)
				.emit(EmitterDef.builder("rain").priority(Priority.HERO).max(5000).rate(50, Thunder.END - Thunder.STORM).shape(Shape.disc((float) Thunder.R))
						.velocity(Velocity.up(-1.8F, -1.4F, 0.1F)).module(Module.wind(0.08F)).collide(0, 0, 0).life(30, 50).size(0.1F, 0.18F).mode(Mode.STRETCHED)
						.stretch(4).sprite(FxSprites.STREAK).color(0xB8C8D8).alpha(0.6F).build())
				.build());

		// A strike landing: a white flash, sparks and molten spatter, smoke off the scorch.
		FxEngine.register(EffectDef.builder(VfxIds.THUNDER_HIT).lod(500, 3200).maxAge(140)
				.cue(0, EffectDef.light(1.4F, 0xD0E0FF))
				.emit(flash("flash", 16, 6, 0xF0F6FF).build())
				.emit(EmitterDef.builder("sparks").burst(50).onGround().velocity(Velocity.up(0.4F, 1.0F, 0.8F)).gravity(0.04F).collide(0.4F, 0.6F, 1)
						.module(Module.jitter(0.05F)).life(10, 24).size(0.1F, 0.18F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(BOLT).build())
				.emit(smoke("smoke").burst(6).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.25F, 0.3F)).life(60, 100).size(3, 5).color(SOOT).alpha(0.5F)
						.build())
				.emit(shock(24, 14, 0xD0E0FF, 0.6F).build())
				.build());

		// The superbolt: a white-out, a dome of light, a ring racing out, a column of smoke from the crater.
		FxEngine.register(EffectDef.builder(VfxIds.THUNDER_SUPER).lod(1200, 8000).maxAge(300)
				.cue(0, EffectDef.light(3.0F, 0xE0E8FF))
				.emit(flash("flash", 100, 24, 0xF8FAFF).offset(0, 10, 0).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.HERO).burst(400).shape(Shape.dome(6)).velocity(Velocity.radial(1.0F, 2.6F)).gravity(0.03F)
						.module(Module.jitter(0.06F)).life(20, 50).size(0.15F, 0.3F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(BOLT).build())
				.emit(shock(220, 44, 0xD8E4FF, 0.75F).build())
				.emit(smoke("column").burst(50).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.5F, 1.0F, 0.2F)).drag(0.02F).module(Module.curl(14, 0.01F))
						.life(120, 200).size(8, 12).color(SOOT).alpha(0.6F).emissive(Curve.keys(0, 1, 0.3F, 0.2F, 1, 0)).build())
				.build());
	}
}
