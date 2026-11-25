package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the three fossil bombs: the Mammoth, the Leviathan and the Pterosaur. */
final class FossilVfx {
	private FossilVfx() {
	}

	static final Gradient FROST = Gradient.of(0, 0xFFFFFF, 0.4F, 0xD0F0FF, 1, argb(0x80C8FF, 0));
	static final Gradient RIME = Gradient.of(0, 0xF4FAFF, 0.5F, 0xDCECF8, 1, 0xC0D4E8);
	static final Gradient SEA = Gradient.of(0, 0xE0FFFF, 0.4F, 0x60F0E0, 1, argb(0x2080C0, 0));
	static final Gradient FOAM = Gradient.of(0, 0xF4FFFF, 0.5F, 0xC8ECF0, 1, 0xA0D0E0);
	static final Gradient EMBER = Gradient.of(0, 0xFFF0C0, 0.3F, 0xFFB050, 0.7F, 0xFF6020, 1, argb(0xA02010, 0));
	static final Gradient BONEDUST = Gradient.of(0, 0xE8DCC0, 0.5F, 0xCFC2A2, 1, 0xA89C80);

	static void register() {
		mammoth();
		leviathan();
		ptero();
	}

	// ------------------------------------------------------------------ Mammoth

	private static void mammoth() {
		// Rising out of the permafrost: shards of ice spraying, frost mist rolling off, snow sliding from the bones.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_HEAVE).lod(200, 1400)
				.emit(EmitterDef.builder("shards").burst(16).onGround().shape(Shape.box(8, 0, 4)).velocity(Velocity.up(0.4F, 0.9F, 0.5F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(20, 40).size(0.2F, 0.4F).spin(-0.3F, 0.3F).sprite(FxSprites.FLARE).color(FROST).build())
				.emit(smoke("mist").burst(5).onGround().shape(Shape.box(9, 0, 5)).velocity(Velocity.up(0.03F, 0.08F, 0.6F)).module(Module.curl(8, 0.004F))
						.life(70, 110).size(3, 5).sizeOverLife(Curve.RISE).color(RIME).alpha(0.45F).shade(0.95F).emissive(Curve.constant(0.4F)).build())
				.emit(EmitterDef.builder("snow").burst(20).shape(Shape.box(8, 6, 4)).offset(0, 12, 0).velocity(Velocity.up(-0.15F, -0.05F, 0.2F)).gravity(0.01F)
						.module(Module.jitter(0.02F)).life(40, 70).size(0.12F, 0.2F).sprite(FxSprites.DUST).color(0xF8FCFF).build())
				.build());

		// Frost breath puffing from the skull.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_BREATH).lod(160, 900)
				.emit(smoke("breath").burst(4).velocity(Velocity.along(0.15F, 0.3F, 0.3F)).drag(0.04F).module(Module.curl(4, 0.006F)).life(40, 60).size(1.2F, 2)
						.sizeOverLife(Curve.RISE).color(RIME).alpha(0.4F).shade(1).emissive(Curve.keys(0, 0.7F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("glitter").burst(6).velocity(Velocity.along(0.2F, 0.4F, 0.4F)).drag(0.03F).life(20, 40).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(FROST).alphaOverLife(TWINKLE).build())
				.build());

		// The trumpet: a cone of frost roaring out of the jaws, a blue flash, a ring of cold rolling away.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_TRUMPET).lod(400, 2400).maxAge(160)
				.cue(0, EffectDef.light(1.4F, 0xA0E0FF))
				.emit(flash("flash", 24, 12, 0xE0F8FF).build())
				.emit(EmitterDef.builder("blast").priority(Priority.HERO).rate(60, 40, Curve.FALL).velocity(Velocity.along(1.6F, 2.6F, 0.35F)).drag(0.03F)
						.life(24, 40).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FROST).build())
				.emit(smoke("frost").rate(10, 40, Curve.FALL).velocity(Velocity.along(0.8F, 1.4F, 0.3F)).drag(0.04F).module(Module.curl(10, 0.01F)).life(60, 100)
						.size(4, 7).sizeOverLife(Curve.RISE).color(RIME).alpha(0.4F).shade(0.95F).emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(40, 40).life(30, 30).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xC0F0FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The ice age's front reaching a spot: ice crystals bursting up and a puff of rime.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_FRONT).lod(250, 1600)
				.emit(EmitterDef.builder("crystals").burst(10).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.25F, 0.6F, 0.4F)).gravity(0.03F)
						.life(16, 30).size(0.15F, 0.3F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(FROST).build())
				.emit(smoke("rime").burst(2).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.04F, 0.1F, 0.3F)).module(Module.curl(6, 0.005F)).life(50, 80)
						.size(2.5F, 4).sizeOverLife(Curve.RISE).color(RIME).alpha(0.5F).shade(0.9F).emissive(Curve.keys(0, 0.6F, 1, 0.1F)).build())
				.build());

		// The blizzard round whoever is under the ice age: driven snow, gusts of spindrift.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_SNOW).lod(80, 400)
				.emit(EmitterDef.builder("flakes").burst(24).shape(Shape.box(18, 8, 18)).offset(0, 10, 0).velocity(Velocity.up(-0.25F, -0.15F, 0)).module(Module.wind(0.02F))
						.module(Module.jitter(0.02F)).life(40, 70).size(0.08F, 0.16F).sprite(FxSprites.DUST).color(0xF8FCFF).build())
				.emit(EmitterDef.builder("streaks").burst(6).shape(Shape.box(14, 6, 14)).offset(0, 6, 0).velocity(Velocity.random(0.4F, 0.7F)).module(Module.wind(0.05F))
						.life(10, 18).size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(0xFFFFFF).alpha(0.6F).build())
				.emit(smoke("spindrift").burst(1).onGround().shape(Shape.disc(14)).velocity(Velocity.random(0.15F, 0.3F)).module(Module.wind(0.02F)).life(40, 70)
						.size(2, 3.5F).color(RIME).alpha(0.3F).shade(1).build())
				.build());

		// An ice spike bursting out of the ground: shards and frost blasted up, a ring of rime.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_SPIKE).lod(300, 2000).maxAge(80)
				.emit(EmitterDef.builder("shards").burst(26).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.6F, 1.3F, 0.5F)).gravity(0.06F)
						.collide(0.3F, 0.5F, 1).life(20, 40).size(0.25F, 0.5F).spin(-0.5F, 0.5F).sprite(FxSprites.FLARE).color(FROST).build())
				.emit(smoke("rime").burst(6).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.2F))
						.drag(0.05F).life(40, 70).size(2.5F, 4).sizeOverLife(Curve.RISE).color(RIME).alpha(0.55F).shade(0.9F).build())
				.emit(EmitterDef.builder("glint").burst(1).onGround().offset(0, 2, 0).life(8, 12).size(3, 4).sprite(FxSprites.GLOW).color(0xD0F4FF).alpha(0.6F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The stomp: a burst of shattered ice, a rolling wave of snow, a frost shockwave.
		FxEngine.register(EffectDef.builder(VfxIds.MAMMOTH_STOMP).lod(400, 2400).maxAge(160)
				.cue(0, EffectDef.light(1.0F, 0xC0F0FF))
				.emit(EmitterDef.builder("shock").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(36, 36).life(24, 24)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xD8F4FF).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ice").priority(Priority.HERO).burst(140).onGround().shape(Shape.disc(4)).velocity(Velocity.radial(0.6F, 1.4F))
						.velocity(Velocity.up(0.5F, 1.2F, 0.3F)).gravity(0.06F).collide(0.3F, 0.5F, 1).life(30, 60).size(0.25F, 0.5F).spin(-0.4F, 0.4F)
						.sprite(FxSprites.FLARE).color(FROST).build())
				.emit(smoke("snowwave").burst(60).onGround().shape(Shape.ring(4, 2)).velocity(Velocity.radial(0.9F, 1.4F)).velocity(Velocity.up(0.1F, 0.3F, 0.1F))
						.drag(0.05F).life(50, 90).size(3, 5).sizeOverLife(Curve.RISE).color(RIME).alpha(0.6F).shade(0.9F).build())
				.build());
	}

	// ------------------------------------------------------------------ Leviathan

	private static void leviathan() {
		// Breaching: earth thrown up, a spray of ghost water, bubbles streaming.
		FxEngine.register(EffectDef.builder(VfxIds.LEVI_BREACH).lod(250, 1600)
				.emit(smoke("earth").burst(6).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.2F, 0.5F, 0.5F)).gravity(0.02F).life(40, 70).size(2, 3.5F)
						.color(Gradient.of(0, 0x8A7A62, 1, 0x6A5C4A)).alpha(0.6F).build())
				.emit(EmitterDef.builder("spray").burst(30).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.up(0.6F, 1.2F, 0.35F)).gravity(0.05F).life(20, 40)
						.size(0.25F, 0.45F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(SEA).build())
				.emit(EmitterDef.builder("bubbles").burst(16).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.25F, 0.2F)).module(Module.jitter(0.02F))
						.life(40, 70).size(0.2F, 0.4F).sprite(FxSprites.RING).color(SEA).alpha(0.8F).build())
				.build());

		// Under the ghost sea: bubbles rising, motes of light drifting like plankton.
		FxEngine.register(EffectDef.builder(VfxIds.LEVI_BUBBLES).lod(80, 400)
				.emit(EmitterDef.builder("bubbles").burst(6).onGround().shape(Shape.disc(14)).velocity(Velocity.up(0.08F, 0.18F, 0.1F)).module(Module.jitter(0.015F))
						.life(50, 90).size(0.12F, 0.3F).sprite(FxSprites.RING).color(SEA).alpha(0.7F).build())
				.emit(EmitterDef.builder("plankton").burst(5).shape(Shape.box(14, 5, 14)).offset(0, 5, 0).module(Module.curl(6, 0.004F)).life(60, 120)
						.size(0.08F, 0.16F).sprite(FxSprites.FLARE).color(SEA).alphaOverLife(TWINKLE).build())
				.build());

		// The wave's crest breaking: foam and spray thrown forward off the lip, mist trailing.
		FxEngine.register(EffectDef.builder(VfxIds.LEVI_CREST).lod(300, 2000)
				.emit(EmitterDef.builder("spray").burst(14).shape(Shape.box(3, 1, 3)).offset(0, 12, 0).velocity(Velocity.along(0.5F, 1.0F, 0.4F))
						.velocity(Velocity.up(0.1F, 0.4F, 0.2F)).gravity(0.04F).life(20, 36).size(0.25F, 0.45F).mode(Mode.STRETCHED).stretch(2)
						.sprite(FxSprites.STREAK).color(FOAM).alpha(0.8F).build())
				.emit(smoke("foam").burst(4).shape(Shape.box(3, 1, 3)).offset(0, 12, 0).velocity(Velocity.along(0.3F, 0.6F, 0.4F)).gravity(0.02F).life(30, 50)
						.size(2.5F, 4).color(FOAM).alpha(0.6F).shade(1).emissive(Curve.constant(0.35F)).build())
				.emit(smoke("mist").burst(2).onGround().shape(Shape.box(3, 0, 3)).velocity(Velocity.up(0.1F, 0.2F, 0.3F)).life(50, 80).size(4, 6)
						.color(FOAM).alpha(0.3F).shade(1).build())
				.build());

		// The roar: a shockwave of water out of the jaws, spray and a column of bubbles.
		FxEngine.register(EffectDef.builder(VfxIds.LEVI_ROAR).lod(400, 2400).maxAge(140)
				.cue(0, EffectDef.light(1.2F, 0x60F0E0))
				.emit(flash("flash", 20, 10, 0xC0FFF4).build())
				.emit(EmitterDef.builder("wave").priority(Priority.HERO).rate(50, 30, Curve.FALL).velocity(Velocity.along(1.4F, 2.4F, 0.4F)).drag(0.03F).life(24, 40)
						.size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(SEA).build())
				.emit(EmitterDef.builder("rings").rate(1, 30).velocity(Velocity.along(0.8F, 0.9F, 0)).mode(Mode.FLAT).life(20, 26).size(4, 6).sizeOverLife(Curve.RISE)
						.sprite(FxSprites.SHOCK).color(0xA0FFF0).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("bubbles").burst(80).shape(Shape.sphere(3)).velocity(Velocity.along(0.6F, 1.2F, 0.6F)).velocity(Velocity.up(0.1F, 0.3F, 0.2F))
						.drag(0.04F).life(40, 70).size(0.25F, 0.5F).sprite(FxSprites.RING).color(SEA).build())
				.build());
	}

	// ------------------------------------------------------------------ Pterosaur

	private static void ptero() {
		// The bones bursting out of the ground: a fountain of earth and bone fragments, embers spitting.
		FxEngine.register(EffectDef.builder(VfxIds.PTERO_BURST).lod(300, 2000).maxAge(120)
				.emit(smoke("earth").burst(40).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.4F, 1.0F, 0.4F)).gravity(0.02F).drag(0.02F).life(60, 100)
						.size(3, 5).color(Gradient.of(0, 0x8A7A62, 1, 0x5E5040)).alpha(0.6F).build())
				.emit(EmitterDef.builder("bones").burst(60).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.8F, 1.6F, 0.4F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(40, 70).size(0.2F, 0.4F).spin(-0.5F, 0.5F).sprite(FxSprites.DUST).color(0xE8DCC0).build())
				.emit(EmitterDef.builder("embers").rate(10, 60, Curve.FALL).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.module(Module.curl(5, 0.01F)).life(30, 60).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(EMBER).build())
				.build());

		// The ghost of fire trailing off a wingtip.
		FxEngine.register(EffectDef.builder(VfxIds.PTERO_TRAIL).lod(250, 1600)
				.emit(EmitterDef.builder("wisp").burst(2).inherit(0.2F).life(16, 26).size(0.6F, 1.0F).sprite(FxSprites.WISP).color(EMBER).alpha(0.6F).build())
				.emit(EmitterDef.builder("sparks").burst(2).inherit(0.1F).velocity(Velocity.random(0.02F, 0.08F)).gravity(0.01F).life(20, 40).size(0.1F, 0.18F)
						.sprite(FxSprites.FLARE).color(EMBER).build())
				.build());

		// The screech: rings of sound pouring out of the beak.
		FxEngine.register(EffectDef.builder(VfxIds.PTERO_SCREECH).lod(300, 2000).maxAge(60)
				.emit(EmitterDef.builder("rings").rate(1, 20).velocity(Velocity.along(1.0F, 1.1F, 0)).mode(Mode.FLAT).life(16, 22).size(2, 3).sizeOverLife(Curve.RISE)
						.sprite(FxSprites.SHOCK).color(0xFFD090).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// A dive striking home: a fireball blast of earth, a ring of dust, burning bone shrapnel.
		FxEngine.register(EffectDef.builder(VfxIds.PTERO_IMPACT).lod(400, 2600).maxAge(200)
				.cue(0, EffectDef.light(1.4F, 0xFFA040))
				.emit(flash("flash", 24, 10, 0xFFE0B0).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(40).shape(Shape.sphere(2)).velocity(Velocity.radial(0.3F, 0.8F)).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.drag(0.06F).life(16, 30).size(2.5F, 4).sprite(FxSprites.FIRE, 2).color(EMBER).alphaOverLife(Curve.FADE).build())
				.emit(smoke("column").burst(30).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.4F, 0.9F, 0.3F)).drag(0.03F).module(Module.curl(8, 0.01F))
						.life(80, 140).size(4, 7).sizeOverLife(Curve.RISE).color(Gradient.of(0, 0x5A4C40, 1, 0x7A6C5E)).alpha(0.7F).build())
				.emit(smoke("ring").burst(40).onGround().shape(Shape.ring(3, 1)).velocity(Velocity.radial(1.0F, 1.5F)).drag(0.06F).life(50, 90).size(3, 5)
						.color(Gradient.of(0, 0x8A7A62, 1, 0x6A5C4A)).alpha(0.6F).build())
				.emit(EmitterDef.builder("shrapnel").burst(70).onGround().velocity(Velocity.up(0.6F, 1.4F, 0.8F)).gravity(0.05F).collide(0.3F, 0.5F, 1).life(30, 60)
						.size(0.15F, 0.3F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(EMBER).build())
				.emit(EmitterDef.builder("shock").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(26, 26).life(20, 20)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xFFD0A0).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Crumbling: a cloud of bone dust collapsing off the spire, embers dying out of it.
		FxEngine.register(EffectDef.builder(VfxIds.PTERO_CRUMBLE).lod(300, 2000).maxAge(200)
				.emit(smoke("dust").priority(Priority.HERO).rate(12, 60, Curve.FALL).shape(Shape.box(12, 3, 4)).velocity(Velocity.up(-0.2F, 0.05F, 0.3F))
						.gravity(0.01F).module(Module.curl(8, 0.008F)).life(80, 140).size(2.5F, 4.5F).sizeOverLife(Curve.RISE).color(BONEDUST).alpha(0.6F).build())
				.emit(EmitterDef.builder("flakes").rate(20, 60).shape(Shape.box(12, 3, 4)).velocity(Velocity.random(0.02F, 0.1F)).gravity(0.03F)
						.collide(0.3F, 0.5F, 1).life(40, 80).size(0.15F, 0.3F).spin(-0.3F, 0.3F).sprite(FxSprites.DUST).color(0xE8DCC0).build())
				.emit(EmitterDef.builder("embers").rate(6, 80, Curve.FALL).shape(Shape.box(10, 3, 4)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.module(Module.curl(5, 0.01F)).life(40, 80).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(EMBER).build())
				.build());
	}
}
