package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the Amber, Sabre-Tooth and Ammonite fossils. */
final class FossilVfx2 {
	private FossilVfx2() {
	}

	static final Gradient GOLD = Gradient.of(0, 0xFFF4C0, 0.35F, 0xFFC040, 0.75F, 0xF08010, 1, argb(0x902808, 0));
	static final Gradient RESIN = Gradient.of(0, 0xFFD060, 0.5F, 0xE88A18, 1, 0xB05A10);
	static final Gradient HONEY_MIST = Gradient.of(0, 0xFFE0A0, 1, 0xE0A050);
	static final Gradient SWARM = Gradient.of(0, 0xFFF0A0, 0.5F, 0xA0FF90, 1, argb(0x40C0FF, 0));
	static final Gradient TAR = Gradient.of(0, 0x221A14, 1, 0x0C0907);
	static final Gradient FUME = Gradient.of(0, 0x5A4A38, 0.5F, 0x6E6252, 1, 0x80786C);
	static final Gradient EARTH = Gradient.of(0, 0x4A3C2E, 1, 0x6A5C4A);
	static final Gradient NACRE = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFC8EC, 0.65F, 0x90E8FF, 1, argb(0xC0A0FF, 0));
	static final Gradient CHALK = Gradient.of(0, 0xF4F0E6, 1, 0xD4CEC0);

	static void register() {
		amber();
		sabre();
		ammonite();
	}

	/** Dark droplets: they have to be drawn alpha-blended and lit, since additive light cannot be black. */
	private static EmitterDef.Builder blob(String name) {
		return EmitterDef.builder(name).layer(Layer.SMOKE).sprite(FxSprites.DUST).shade(0.8F).alphaOverLife(Curve.FADE_OUT);
	}

	// ------------------------------------------------------------------ Amber

	private static void amber() {
		// The drop welling up: honey spattering out of the ground, golden motes, sticky mist.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_HEAVE).lod(250, 1600)
				.emit(blob("honey").burst(26).onGround().shape(Shape.disc(7)).velocity(Velocity.up(0.3F, 0.8F, 0.5F)).gravity(0.05F).collide(0.1F, 0.3F, 1)
						.life(24, 44).size(0.22F, 0.4F).color(RESIN).emissive(Curve.constant(0.5F)).build())
				.emit(EmitterDef.builder("motes").burst(14).onGround().shape(Shape.disc(8)).velocity(Velocity.up(0.05F, 0.15F, 0.4F)).module(Module.curl(6, 0.006F))
						.life(40, 80).size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.emit(smoke("mist").burst(4).onGround().shape(Shape.ring(8, 3)).velocity(Velocity.radial(0.05F, 0.15F)).velocity(Velocity.up(0.02F, 0.06F, 0.4F))
						.life(60, 100).size(2.5F, 4).color(HONEY_MIST).alpha(0.35F).shade(0.95F).emissive(Curve.constant(0.35F)).build())
				.build());

		// Resin dripping off the drop's skin, beading and falling.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_DRIP).lod(150, 900)
				.emit(blob("drop").burst(3).velocity(Velocity.up(-0.05F, 0.0F, 0.2F)).gravity(0.035F).collide(0.0F, 0.2F, 0).life(30, 60).size(0.18F, 0.3F)
						.sizeOverLife(Curve.keys(0, 0.4F, 0.3F, 1, 1, 0.9F)).color(RESIN).emissive(Curve.constant(0.6F)).build())
				.emit(EmitterDef.builder("glint").burst(1).life(10, 16).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(0xFFE8A0).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The resin's front reaching a spot: a glossy splash and a sigh of sweet mist.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_FRONT).lod(220, 1400)
				.emit(blob("splash").burst(12).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.up(0.2F, 0.5F, 0.6F)).gravity(0.05F).collide(0.0F, 0.2F, 0)
						.life(16, 30).size(0.18F, 0.32F).color(RESIN).emissive(Curve.constant(0.55F)).build())
				.emit(smoke("mist").burst(1).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.03F, 0.08F, 0.3F)).life(50, 80).size(2.5F, 3.5F)
						.color(HONEY_MIST).alpha(0.3F).shade(1).emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("glint").burst(3).onGround().shape(Shape.disc(3)).offset(0, 0.3F, 0).life(8, 16).size(0.3F, 0.6F).sprite(FxSprites.FLARE)
						.color(0xFFE0A0).alphaOverLife(TWINKLE).build())
				.build());

		// The amber bursting: a storm of glittering shards, a golden flash, a ring of resin mist.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_SHATTER).lod(500, 3000).maxAge(220)
				.cue(0, EffectDef.light(1.6F, 0xFFC060))
				.emit(flash("flash", 34, 14, 0xFFE0A0).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(260).shape(Shape.shell(9)).velocity(Velocity.radial(0.5F, 1.5F))
						.velocity(Velocity.up(0.1F, 0.5F, 0.6F)).gravity(0.05F).drag(0.01F).collide(0.3F, 0.5F, 2).life(40, 80).size(0.25F, 0.6F).spin(-0.5F, 0.5F)
						.sprite(FxSprites.FLARE).color(GOLD).build())
				.emit(EmitterDef.builder("sparkle").burst(120).shape(Shape.sphere(9)).velocity(Velocity.random(0.05F, 0.3F)).drag(0.03F).life(30, 70).size(0.1F, 0.2F)
						.sprite(FxSprites.FLARE).color(0xFFF0C0).alphaOverLife(TWINKLE).build())
				.emit(smoke("dust").burst(50).shape(Shape.shell(8)).velocity(Velocity.radial(0.3F, 0.8F)).drag(0.05F).module(Module.curl(10, 0.008F)).life(70, 120)
						.size(4, 7).sizeOverLife(Curve.RISE).color(HONEY_MIST).alpha(0.45F).shade(0.95F).emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(40, 40).life(28, 28)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xFFD080).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The swarm: glowing insects darting and wheeling round the dragonfly.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_SWARM).lod(200, 1400)
				.emit(EmitterDef.builder("flies").burst(18).inherit(0.6F).shape(Shape.sphere(9)).velocity(Velocity.swirl(0.25F, 0.5F)).velocity(Velocity.random(0.05F, 0.15F))
						.module(Module.vortex(0.05F, 0.01F)).module(Module.jitter(0.06F)).life(30, 60).size(0.1F, 0.18F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(SWARM).alphaOverLife(TWINKLE).build())
				.build());

		// A strafing run's downdraught: dust, leaves and resin whipped up off the ground.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_GUST).lod(250, 1600)
				.emit(smoke("dust").burst(4).onGround().shape(Shape.disc(4)).velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.1F, 0.25F, 0.4F)).drag(0.05F)
						.life(30, 60).size(2, 3.5F).color(EARTH).alpha(0.45F).build())
				.emit(EmitterDef.builder("flecks").burst(14).onGround().shape(Shape.disc(4)).velocity(Velocity.radial(0.4F, 0.9F)).velocity(Velocity.up(0.3F, 0.7F, 0.5F))
						.gravity(0.04F).collide(0.2F, 0.5F, 1).life(20, 40).size(0.12F, 0.22F).spin(-0.4F, 0.4F).sprite(FxSprites.FLARE).color(GOLD).build())
				.build());

		// Sealed: resin gushing up round the dragonfly, a halo of gold, motes settling.
		FxEngine.register(EffectDef.builder(VfxIds.AMBER_SEAL).lod(400, 2400).maxAge(200)
				.cue(0, EffectDef.light(1.2F, 0xFFB040))
				.emit(flash("flash", 26, 16, 0xFFD080).build())
				.emit(blob("gush").priority(Priority.HERO).rate(30, 50, Curve.FALL).shape(Shape.ring(11, 2)).offset(0, -10, 0).velocity(Velocity.up(0.5F, 0.9F, 0.15F))
						.module(Module.attract(0.02F, 1)).gravity(0.02F).life(30, 50).size(0.3F, 0.5F).color(RESIN).emissive(Curve.constant(0.7F)).build())
				.emit(EmitterDef.builder("motes").rate(6, 120).shape(Shape.sphere(12)).velocity(Velocity.up(-0.04F, 0.0F, 0.3F)).module(Module.curl(6, 0.004F))
						.life(60, 100).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Sabre-Tooth

	private static void sabre() {
		// The tar's front: black ooze heaving up, a brown fume coming off it.
		FxEngine.register(EffectDef.builder(VfxIds.TAR_FRONT).lod(220, 1400)
				.emit(blob("ooze").burst(10).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.up(0.15F, 0.4F, 0.5F)).gravity(0.05F).collide(0.0F, 0.1F, 0)
						.life(16, 30).size(0.2F, 0.35F).color(TAR).build())
				.emit(smoke("fume").burst(1).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.03F, 0.08F, 0.3F)).module(Module.wind(0.01F)).life(60, 100)
						.size(2.5F, 4).color(FUME).alpha(0.3F).build())
				.build());

		// A splash of tar: claws dragging, the cat climbing out.
		FxEngine.register(EffectDef.builder(VfxIds.TAR_SPLASH).lod(250, 1600)
				.emit(blob("tar").burst(40).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.4F, 1.0F, 0.6F)).gravity(0.06F).collide(0.0F, 0.1F, 0)
						.life(20, 40).size(0.25F, 0.5F).color(TAR).build())
				.emit(smoke("fume").burst(5).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.04F, 0.1F, 0.4F)).module(Module.curl(6, 0.006F)).life(60, 100)
						.size(3, 5).color(FUME).alpha(0.35F).build())
				.build());

		// A bubble swelling out of a pit and popping: a black splash, a breath of fume.
		FxEngine.register(EffectDef.builder(VfxIds.TAR_BUBBLE).lod(150, 900)
				.emit(blob("pop").burst(1, 12).velocity(Velocity.up(0.15F, 0.35F, 0.8F)).gravity(0.05F).life(12, 22).size(0.15F, 0.28F).color(TAR).build())
				.emit(smoke("fume").burst(1, 1).velocity(Velocity.up(0.03F, 0.06F, 0.2F)).life(40, 70).size(1.2F, 2).color(FUME).alpha(0.3F).build())
				.emit(EmitterDef.builder("ring").burst(1, 1).offset(0, 0.06F, 0).mode(Mode.FLAT).fixedRotation().size(0.8F, 1.4F).life(14, 18).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).color(0x8A7050).alpha(0.4F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Tar running off the bones in strings and drops.
		FxEngine.register(EffectDef.builder(VfxIds.TAR_DRIP).lod(150, 900)
				.emit(blob("drip").burst(2).velocity(Velocity.up(-0.05F, 0.0F, 0.2F)).gravity(0.04F).collide(0.0F, 0.1F, 0).life(24, 40).size(0.15F, 0.26F)
						.sizeOverLife(Curve.keys(0, 0.5F, 0.3F, 1, 1, 0.8F)).color(TAR).build())
				.build());

		// The cat going under: great gloops of tar, bubbles rising all round, a pall of fume.
		FxEngine.register(EffectDef.builder(VfxIds.TAR_SINK).lod(300, 1800).maxAge(140)
				.emit(blob("gloop").burst(30).onGround().shape(Shape.disc(7)).velocity(Velocity.up(0.2F, 0.6F, 0.5F)).gravity(0.05F).life(20, 40).size(0.3F, 0.6F)
						.color(TAR).build())
				.emit(EmitterDef.builder("rings").burst(8).onGround().shape(Shape.disc(8)).offset(0, 0.06F, 0).mode(Mode.FLAT).fixedRotation().size(1.5F, 3)
						.life(20, 30).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).color(0x8A7050).alpha(0.4F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("fume").burst(6).onGround().shape(Shape.disc(7)).velocity(Velocity.up(0.04F, 0.1F, 0.3F)).module(Module.curl(8, 0.006F)).life(70, 120)
						.size(3, 5).color(FUME).alpha(0.35F).build())
				.build());

		// A pounce landing: a crown of tar thrown up, earth and dust rolling out, a shock ring.
		FxEngine.register(EffectDef.builder(VfxIds.SABRE_POUNCE).lod(400, 2600).maxAge(200)
				.cue(0, EffectDef.light(0.8F, 0xFFB060))
				.emit(blob("crown").priority(Priority.HERO).burst(160).onGround().shape(Shape.ring(3, 2)).velocity(Velocity.radial(0.4F, 1.0F))
						.velocity(Velocity.up(0.6F, 1.3F, 0.3F)).gravity(0.06F).collide(0.0F, 0.1F, 0).life(30, 55).size(0.3F, 0.6F).color(TAR).build())
				.emit(smoke("earth").burst(30).onGround().shape(Shape.ring(3, 1)).velocity(Velocity.radial(0.9F, 1.5F)).velocity(Velocity.up(0.05F, 0.2F, 0.2F))
						.drag(0.06F).life(50, 90).size(3, 5).color(EARTH).alpha(0.6F).build())
				.emit(smoke("fume").burst(12).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.2F, 0.5F, 0.4F)).drag(0.03F).module(Module.curl(8, 0.01F))
						.life(70, 120).size(4, 6).sizeOverLife(Curve.RISE).color(FUME).alpha(0.5F).build())
				.emit(EmitterDef.builder("shock").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(28, 28).life(20, 20)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xC0A080).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The roar: rings of breath pouring out of the jaws, tar spattered from the teeth.
		FxEngine.register(EffectDef.builder(VfxIds.SABRE_ROAR).lod(300, 2000).maxAge(80)
				.emit(EmitterDef.builder("rings").rate(1, 30).velocity(Velocity.along(0.9F, 1.0F, 0)).mode(Mode.FLAT).life(18, 24).size(2, 3).sizeOverLife(Curve.RISE)
						.sprite(FxSprites.SHOCK).color(0xFFD0A0).alpha(0.45F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(blob("spit").burst(30).velocity(Velocity.along(0.5F, 1.0F, 0.5F)).gravity(0.04F).life(20, 36).size(0.12F, 0.22F).color(TAR).build())
				.emit(smoke("breath").rate(3, 24, Curve.FALL).velocity(Velocity.along(0.3F, 0.6F, 0.3F)).drag(0.05F).life(30, 50).size(1.5F, 2.5F)
						.color(FUME).alpha(0.3F).build())
				.build());
	}

	// ------------------------------------------------------------------ Ammonite

	private static void ammonite() {
		// The ground heaving in a spiral as the shell starts to grow: chalk dust and pearl motes.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_HEAVE).lod(300, 2000).maxAge(200)
				.emit(smoke("chalk").burst(30).onGround().shape(Shape.disc(14)).velocity(Velocity.swirl(0.1F, 0.3F)).velocity(Velocity.up(0.05F, 0.15F, 0.4F))
						.module(Module.vortex(0.01F, 0.002F)).life(70, 120).size(3, 5).color(CHALK).alpha(0.45F).shade(0.9F).build())
				.emit(EmitterDef.builder("motes").burst(30).onGround().shape(Shape.disc(12)).velocity(Velocity.up(0.05F, 0.2F, 0.4F)).module(Module.curl(6, 0.006F))
						.life(50, 100).size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(NACRE).alphaOverLife(TWINKLE).build())
				.build());

		// The shell growing: flakes of chalk and a glimmer of pearl where the newest whorl is forming.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_GROW).lod(220, 1400)
				.emit(smoke("chalk").burst(3).shape(Shape.sphere(2)).velocity(Velocity.random(0.03F, 0.1F)).module(Module.curl(5, 0.005F)).life(40, 70)
						.size(1.5F, 2.5F).color(CHALK).alpha(0.4F).shade(0.95F).build())
				.emit(EmitterDef.builder("glimmer").burst(8).shape(Shape.sphere(2.5F)).velocity(Velocity.random(0.02F, 0.08F)).life(20, 40).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(NACRE).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("flakes").burst(6).shape(Shape.sphere(2)).velocity(Velocity.random(0.05F, 0.15F)).gravity(0.03F).collide(0.2F, 0.5F, 1)
						.life(30, 50).size(0.1F, 0.18F).spin(-0.3F, 0.3F).sprite(FxSprites.DUST).color(0xF0ECE0).build())
				.build());

		// Pearl motes drifting round the living shell.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_MOTES).lod(200, 1200)
				.emit(EmitterDef.builder("motes").burst(10).shape(Shape.sphere(14)).velocity(Velocity.swirl(0.02F, 0.05F)).module(Module.curl(8, 0.004F)).life(60, 120)
						.size(0.1F, 0.2F).sprite(FxSprites.FLARE).color(NACRE).alphaOverLife(TWINKLE).build())
				.build());

		// The head of the spiral of light: stone heaving up, sparks of pearl, a wave of chalk dust.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_FRONT).lod(300, 2200)
				.emit(EmitterDef.builder("sparks").burst(16).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.3F, 0.8F, 0.5F)).gravity(0.04F).life(16, 30)
						.size(0.12F, 0.22F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(NACRE).build())
				.emit(smoke("chalk").burst(3).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.06F, 0.16F, 0.4F)).velocity(Velocity.radial(0.05F, 0.15F))
						.module(Module.curl(6, 0.006F)).life(60, 100).size(3, 4.5F).color(CHALK).alpha(0.5F).shade(0.9F).emissive(Curve.keys(0, 0.5F, 1, 0)).build())
				.emit(EmitterDef.builder("glow").burst(1).onGround().offset(0, 1, 0).life(10, 14).size(3, 4).sprite(FxSprites.GLOW).color(0xFFD8F0).alpha(0.6F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The shell's last pulse: a pearl flash and a ring of light rolling out over the stone.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_PULSE).lod(600, 4000).maxAge(160)
				.cue(0, EffectDef.light(1.6F, 0xFFD0F0))
				.emit(flash("flash", 40, 16, 0xFFE8F8).offset(0, 13, 0).build())
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().offset(0, 0.5F, 0).mode(Mode.FLAT).fixedRotation().size(80, 80).life(40, 40)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xFFD8F4).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("chalk").burst(80).onGround().shape(Shape.ring(10, 4)).velocity(Velocity.radial(1.0F, 1.6F)).velocity(Velocity.up(0.05F, 0.2F, 0.2F))
						.drag(0.04F).life(60, 100).size(3, 5).color(CHALK).alpha(0.5F).shade(0.9F).emissive(Curve.keys(0, 0.6F, 1, 0)).build())
				.emit(EmitterDef.builder("pearls").burst(120).shape(Shape.sphere(12)).offset(0, 12, 0).velocity(Velocity.radial(0.3F, 0.9F)).drag(0.03F)
						.life(30, 60).size(0.15F, 0.3F).sprite(FxSprites.FLARE).color(NACRE).build())
				.build());

		// Someone turned to stone: grey grit flaking off them, a crackle of pearl.
		FxEngine.register(EffectDef.builder(VfxIds.AMMO_PETRIFY).lod(150, 900)
				.emit(EmitterDef.builder("grit").burst(24).shape(Shape.box(0.4F, 1, 0.4F)).offset(0, 1, 0).velocity(Velocity.random(0.03F, 0.12F)).gravity(0.04F)
						.collide(0.2F, 0.5F, 1).life(20, 40).size(0.08F, 0.16F).sprite(FxSprites.DUST).color(0x9A968C).build())
				.emit(smoke("dust").burst(3).shape(Shape.box(0.4F, 1, 0.4F)).offset(0, 1, 0).velocity(Velocity.up(0.02F, 0.06F, 0.5F)).life(40, 60).size(1, 1.6F)
						.color(Gradient.of(0, 0xA8A498, 1, 0xC0BCB0)).alpha(0.4F).build())
				.emit(EmitterDef.builder("crackle").burst(6).shape(Shape.box(0.4F, 1, 0.4F)).offset(0, 1, 0).life(6, 12).size(0.2F, 0.35F).sprite(FxSprites.FLARE)
						.color(NACRE).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
