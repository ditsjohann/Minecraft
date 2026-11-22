package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.util.WildShapes;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.INNER_GLOW;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;

/**
 * Particle effects for the goo bomb, the hyperbeam raygun, the cactus, the Blackforge and the Blue Infrit. The flying
 * globs and spikes fall with exactly the gravity the server uses, so they land where the server's do.
 */
final class WildVfx {
	private WildVfx() {
	}

	static final Gradient GOO = Gradient.of(0, 0xC8FF70, 0.3F, 0x8CF040, 1, 0x4EA818);
	static final Gradient ACID_FUME = Gradient.of(0, 0xC8E070, 1, argb(0x9AA890, 0));
	static final Gradient SOUL_FIRE = Gradient.of(0, 0xA8F0FF, 0.15F, 0x48B8FF, 0.45F, 0x1E68F0, 0.75F, 0x1030A0, 1, argb(0x060A30, 0));
	static final Gradient SOUL_SPARK = Gradient.of(0, 0xFFFFFF, 0.3F, 0x80E8FF, 1, argb(0x1040C0, 0));
	static final Gradient MOLTEN = Gradient.of(0, 0xFFFFFF, 0.15F, 0xFFE090, 0.5F, 0xFF7018, 0.85F, 0xA02008, 1, argb(0x300800, 0));
	static final Gradient SAND = Gradient.of(0, 0xC8B488, 1, 0xA89470);

	static void register() {
		goo();
		raygun();
		cactus();
		forge();
		infrit();
		desert();
		anvils();
		hound();
	}

	// ------------------------------------------------------------------ goo bomb

	private static void goo() {
		EmitterDef drop = EmitterDef.builder("drop").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0, 0.02F))
				.gravity(0.03F).life(10, 18).size(0.1F, 0.16F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(GOO)
				.emissive(Curve.constant(0.7F)).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build();
		EmitterDef bubbles = EmitterDef.builder("bubbles").priority(Priority.DETAIL).burst(2).shape(Shape.sphere(0.3F))
				.life(8, 16).size(0.15F, 0.3F).sprite(FxSprites.RING).color(0xB8FF60).sizeOverLife(Curve.RISE)
				.alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build();

		FxEngine.register(EffectDef.builder(VfxIds.GOO_DRIP).lod(24, 160)
				.emit(EmitterDef.builder("drips").burst(3).velocity(Velocity.along(0.03F, 0.08F, 0.6F)).gravity(0.035F)
						.collide(0, 0.3F, 0).life(25, 45).size(0.12F, 0.2F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(GOO)
						.emissive(Curve.constant(0.7F)).sizeOverLife(Curve.keys(0, 0.6F, 0.2F, 1, 1, 0.7F)).alphaOverLife(Curve.FADE).build())
				.emit(bubbles)
				.emit(smoke("fume").burst(1).velocity(Velocity.up(0.01F, 0.03F, 0.3F)).life(30, 50).size(0.4F, 0.7F)
						.color(ACID_FUME).alpha(0.35F).emissive(Curve.constant(0.4F)).module(Module.buoyancy(0.008F)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GOO_PULSE).lod(32, 200)
				.emit(EmitterDef.builder("ring").burst(1).size(1.3F, 1.3F).life(10, 10).sprite(FxSprites.RING).color(0x9AFF40)
						.sizeOverLife(Curve.keys(0, 0.8F, 1, 1.3F)).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glow").burst(1).size(1.6F, 1.6F).life(8, 8).sprite(FxSprites.GLOW).color(0x60C020)
						.alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GOO_BURST).lod(48, 320)
				.emit(flash("flash", 3, 4, 0x9AFF40).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(3, 3).life(10, 10).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xE0FFB0, 1, 0x60C020)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("droplets").burst(140).shape(Shape.sphere(1.5F))
						.velocity(Velocity.radial(0.3F, 0.9F)).velocity(Velocity.up(0.05F, 0.3F, 1.2F)).gravity(0.05F).drag(0.02F)
						.collide(0.1F, 0.5F, 0).life(20, 40).size(0.08F, 0.16F).mode(Mode.STRETCHED).stretch(1.5F)
						.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(GOO).emissive(Curve.constant(0.8F)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("mist").burst(24).shape(Shape.dome(2)).velocity(Velocity.radial(0.05F, 0.15F)).life(50, 90).size(1, 1.6F)
						.color(Gradient.of(0, 0x9AD050, 1, 0x8A9A78)).alpha(0.6F).emissive(Curve.keys(0, 0.8F, 0.3F, 0.3F, 1, 0))
						.module(Module.buoyancy(0.008F)).module(Module.wind(0.6F)).drag(0.05F).build())
				.emit(EmitterDef.builder("shock").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(8, 8).life(12, 12)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xB8FF60).build())
				.build());

		// One glob of acid on the server's own arc: same start, speed and gravity, so it lands on the server's splat.
		FxEngine.register(EffectDef.builder(VfxIds.GOO_GLOB).lod(64, 400)
				.emit(EmitterDef.builder("glob").priority(Priority.HERO).burst(1).velocity(Velocity.along(1, 1, 0))
						.gravity((float) WildShapes.Goo.GRAVITY).collide(0, 0, 0).life(220, 220).size(0.45F, 0.55F).spin(0.05F, 0.15F)
						.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(Gradient.of(0, 0x9CFF50, 1, 0x60D020))
						.emissive(Curve.constant(0.85F)).alphaOverLife(Curve.ONE).every(2, drop).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GOO_SPLAT).lod(40, 240)
				.emit(EmitterDef.builder("splash").burst(26).velocity(Velocity.random(0.08F, 0.25F)).velocity(Velocity.up(0.08F, 0.2F, 0.8F))
						.gravity(0.05F).collide(0, 0.5F, 0).life(12, 24).size(0.06F, 0.12F).mode(Mode.STRETCHED)
						.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(GOO).emissive(Curve.constant(0.8F)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("puddle").burst(1).onGround().mode(Mode.FLAT).size(2.2F, 2.2F).life(40, 40)
						.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0x70E030).emissive(Curve.constant(0.6F))
						.alpha(0.7F).alphaOverLife(Curve.keys(0, 1, 0.7F, 0.7F, 1, 0)).build())
				.emit(smoke("hiss").rate(1.2F, 30).shape(Shape.disc(1.5F)).velocity(Velocity.up(0.02F, 0.05F, 0.4F))
						.life(30, 50).size(0.4F, 0.8F).color(ACID_FUME).alpha(0.55F).emissive(Curve.keys(0, 0.6F, 1, 0))
						.module(Module.buoyancy(0.01F)).module(Module.wind(0.6F)).build())
				.emit(bubbles)
				.build());
	}

	// ------------------------------------------------------------------ hyperbeam raygun

	private static void raygun() {
		int blue = 0x60C8FF;
		FxEngine.register(EffectDef.builder(VfxIds.RAY_CHARGE).tint(blue).lod(24, 128)
				.emit(EmitterDef.builder("inflow").burst(10).shape(Shape.shell(1.8F)).module(Module.attract(0.035F, 0.1F)).drag(0.1F)
						.life(10, 16).size(0.025F, 0.04F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("core").burst(1).size(0.35F, 0.35F).life(6, 6).sprite(FxSprites.GLOW).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.RAY_MUZZLE).tint(blue).lod(32, 200)
				.emit(flash("flash", 2.5F, 4, 0xC0F0FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("ring").burst(1).size(2.5F, 2.5F).life(10, 10).sprite(FxSprites.RING).tinted()
						.sizeOverLife(Curve.EASE_OUT).color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(40).velocity(Velocity.along(0.4F, 1.0F, 0.6F))
						.drag(0.08F).life(6, 14).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.RAY_HEAD).tint(blue).lod(40, 260)
				.emit(EmitterDef.builder("glow").priority(Priority.HERO).burst(1).size(2.2F, 2.2F).life(3, 3).sprite(FxSprites.GLOW)
						.tinted().color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(flash("flare", 1.5F, 2, 0xE0F8FF).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(18).velocity(Velocity.random(0.3F, 0.9F))
						.velocity(Velocity.along(0.2F, 0.5F, 1.0F)).gravity(0.03F).drag(0.05F).collide(0.3F, 0.6F, 1)
						.life(8, 18).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).tinted().color(WHITE_FADE)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("chips").priority(Priority.DETAIL).burst(6).velocity(Velocity.random(0.2F, 0.5F))
						.velocity(Velocity.up(0.1F, 0.3F, 0.8F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(20, 40)
						.size(0.08F, 0.14F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A4644).build())
				.emit(smoke("dust").burst(3).velocity(Velocity.random(0.03F, 0.08F)).life(30, 50).size(0.6F, 1.0F)
						.color(Gradient.of(0, 0x5A5A60, 1, 0x8A8A90)).alpha(0.6F).module(Module.buoyancy(0.004F)).module(Module.wind(1)).build())
				.build());

	}

	// ------------------------------------------------------------------ cactus

	private static void cactus() {
		EmitterDef trail = smoke("trail").priority(Priority.DETAIL).burst(1).sprite(FxSprites.DUST, 2).life(15, 25).size(0.25F, 0.4F)
				.sizeOverLife(Curve.keys(0, 0.5F, 1, 1.6F)).color(SAND).alpha(0.4F).alphaOverLife(Curve.FADE_OUT).build();

		FxEngine.register(EffectDef.builder(VfxIds.CACTUS_DUST).lod(48, 300)
				.emit(smoke("dust").burst(10).onGround().shape(Shape.ring(4.5F, 1.5F)).velocity(Velocity.radial(0.08F, 0.18F))
						.velocity(Velocity.up(0.02F, 0.06F, 0.5F)).life(30, 60).size(0.8F, 1.3F).sprite(FxSprites.DUST, 2)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2)).color(SAND).alpha(0.75F).shade(0.8F).build())
				.emit(EmitterDef.builder("pebbles").priority(Priority.DETAIL).burst(6).onGround().shape(Shape.ring(4, 1))
						.velocity(Velocity.up(0.15F, 0.3F, 0.6F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(20, 40)
						.size(0.06F, 0.1F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x6A5A44).build())
				.build());

		// One spike on the server's own arc (see the goo glob): a long pale needle trailing dust.
		FxEngine.register(EffectDef.builder(VfxIds.CACTUS_SPIKE).lod(64, 400)
				.emit(EmitterDef.builder("spike").priority(Priority.HERO).burst(1).velocity(Velocity.along(1, 1, 0))
						.gravity((float) WildShapes.Cactus.GRAVITY).collide(0, 0, 0).life(260, 260).size(0.14F, 0.14F)
						.mode(Mode.STRETCHED).stretch(3.5F).layer(Layer.SMOKE).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xF0E8C0, 1, 0xD8C890)).emissive(Curve.constant(0.3F)).alphaOverLife(Curve.ONE)
						.every(3, trail).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SPIKE_IMPACT).lod(48, 300)
				.emit(EmitterDef.builder("flash").burst(1).size(1.2F, 1.2F).life(4, 4).sprite(FxSprites.FIRE).color(0xFFC060)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("dust").burst(14).shape(Shape.dome(1)).velocity(Velocity.radial(0.08F, 0.25F)).velocity(Velocity.up(0.05F, 0.2F, 0.6F))
						.life(40, 80).size(0.7F, 1.2F).color(Gradient.of(0, 0x6A5A48, 1, 0x9A8A70)).alpha(0.8F).drag(0.06F).build())
				.emit(EmitterDef.builder("chunks").priority(Priority.DETAIL).burst(12).velocity(Velocity.random(0.2F, 0.5F))
						.velocity(Velocity.up(0.2F, 0.45F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 50)
						.size(0.1F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A4A38).spin(0.1F, 0.3F).build())
				.build());
	}

	// ------------------------------------------------------------------ blackforge

	private static void forge() {

		FxEngine.register(EffectDef.builder(VfxIds.FORGE_STRIKE).lod(120, 900)
				.emit(flash("flash", 10, 5, 0xFFC080).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(8, 8).life(12, 12).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xFFF0C0, 1, 0xFF5010)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("molten").priority(Priority.HERO).burst(220).shape(Shape.disc(6))
						.velocity(Velocity.random(0.5F, 1.4F)).velocity(Velocity.up(0.3F, 0.8F, 1.0F)).gravity(0.05F).drag(0.015F)
						.collide(0.25F, 0.5F, 2).life(30, 70).size(0.06F, 0.1F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
						.color(MOLTEN).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("drops").burst(40).shape(Shape.disc(5)).velocity(Velocity.random(0.3F, 0.8F))
						.velocity(Velocity.up(0.4F, 0.7F, 0.8F)).gravity(0.06F).collide(0, 0.5F, 0).life(40, 70).size(0.15F, 0.3F)
						.sprite(FxSprites.GLOW).color(MOLTEN).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(14, 14).life(10, 10).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFB060).build())
				.emit(smoke("smoke").burst(20).shape(Shape.dome(6)).velocity(Velocity.radial(0.05F, 0.15F)).life(80, 140).size(2.5F, 4)
						.color(VfxLibrary.SOOT).alpha(0.8F).emissive(INNER_GLOW).module(Module.buoyancy(0.012F)).module(Module.wind(1)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.FORGE_SHOCK).lod(150, 1200)
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
						.size(35, 35).life(22, 22).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(0.8F)
						.alphaOverLife(Curve.FADE_OUT).color(0xFFA050).build())
				.emit(smoke("dust_wall").rate(40, 18).onGround().shape(Shape.ring(4, 2)).expand(1.9F).velocity(Velocity.radial(0.3F, 0.5F))
						.velocity(Velocity.up(0.05F, 0.12F, 0.4F)).life(40, 70).size(2, 3).sprite(FxSprites.DUST, 2)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.2F)).color(Gradient.of(0, 0x3A302A, 1, 0x6A5A50)).alpha(0.8F)
						.emissive(Curve.keys(0, 0.6F, 0.2F, 0)).drag(0.04F).build())
				.build());

	}

	// ------------------------------------------------------------------ blue infrit

	private static void infrit() {
		FxEngine.register(EffectDef.builder(VfxIds.SOUL_FLARE).lod(48, 320)
				.emit(EmitterDef.builder("flames").burst(10).shape(Shape.disc(1.2F)).velocity(Velocity.up(0.05F, 0.12F, 0.3F))
						.module(Module.buoyancy(0.01F)).module(Module.curl(2, 0.006F)).life(12, 22).size(0.5F, 0.9F)
						.mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(SOUL_FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").burst(1).size(2, 2).life(6, 6).sprite(FxSprites.GLOW).color(0x60D0FF).alpha(0.5F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(6).velocity(Velocity.up(0.1F, 0.25F, 0.5F))
						.drag(0.05F).life(10, 20).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SOUL_SPARK)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SOUL_PILLAR).lod(80, 500).maxAge(300)
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).rate(5, 100, Curve.keys(0, 0.4F, 0.15F, 1, 0.8F, 1, 1, 0))
						.shape(Shape.disc(1.2F)).velocity(Velocity.up(0.35F, 0.5F, 0.06F)).module(Module.buoyancy(0.02F)).drag(0.02F)
						.life(20, 36).size(0.9F, 1.5F).mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(SOUL_FIRE).alpha(0.6F)
						.sizeOverLife(Curve.keys(0, 0.4F, 0.2F, 1, 1, 0.5F)).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").rate(0.5F, 100).size(2.5F, 2.5F).life(10, 10).sprite(FxSprites.GLOW).color(0x3A9AFF)
						.alpha(0.4F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).rate(1.5F, 100).shape(Shape.disc(1.5F))
						.velocity(Velocity.up(0.3F, 0.6F, 0.2F)).drag(0.03F).life(20, 40).size(0.04F, 0.06F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(SOUL_SPARK).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SOUL_VORTEX).lod(120, 800).maxAge(400)
				.emit(EmitterDef.builder("flames").priority(Priority.HERO).rate(14, 100, Curve.keys(0, 0.3F, 0.3F, 1, 1, 0.6F))
						.onGround().shape(Shape.ring(36, 10)).velocity(Velocity.swirl(0.25F, 0.45F)).velocity(Velocity.up(0.05F, 0.12F, 0.3F))
						.module(Module.vortex(0.012F, 0.01F)).module(Module.buoyancy(0.015F)).drag(0.03F).life(40, 70).size(1.2F, 2.2F)
						.sprite(FxSprites.FIRE, 2).color(SOUL_FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("wisps").rate(3, 100).onGround().shape(Shape.ring(36, 10)).velocity(Velocity.swirl(0.25F, 0.45F))
						.velocity(Velocity.up(0.05F, 0.12F, 0.3F)).module(Module.vortex(0.012F, 0.01F)).module(Module.buoyancy(0.01F))
						.drag(0.03F).life(60, 100).size(2, 3.5F).color(Gradient.of(0, 0x0A1430, 1, 0x1A2850)).alpha(0.6F)
						.emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).rate(3, 100).onGround().shape(Shape.ring(36, 10))
						.velocity(Velocity.swirl(0.3F, 0.5F)).velocity(Velocity.up(0.2F, 0.4F, 0.3F)).module(Module.vortex(0.012F, 0.01F))
						.drag(0.02F).life(30, 50).size(0.05F, 0.08F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SOUL_SPARK)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Played by the client itself around the hound's head, every couple of ticks.
		FxEngine.register(EffectDef.builder(VfxIds.SOUL_TRAIL).lod(80, 600)
				.emit(EmitterDef.builder("flames").burst(7).shape(Shape.sphere(3)).velocity(Velocity.random(0.02F, 0.08F))
						.module(Module.buoyancy(0.02F)).drag(0.08F).life(12, 22).size(1.2F, 2.2F).sprite(FxSprites.FIRE, 2)
						.color(SOUL_FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(3).shape(Shape.sphere(3))
						.velocity(Velocity.random(0.05F, 0.15F)).drag(0.03F).life(15, 30).size(0.05F, 0.08F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(SOUL_SPARK).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// A wall of blue fire riding the sweep front out from the circle to its full reach.
		FxEngine.register(EffectDef.builder(VfxIds.SOUL_SWEEP).lod(200, 1200).maxAge(200)
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
						.size((float) WildShapes.Infrit.SWEEP, (float) WildShapes.Infrit.SWEEP).life(64, 64).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.keys(0, 0.25F, 1, 1)).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).color(0x60D0FF).build())
				.emit(EmitterDef.builder("wall").priority(Priority.HERO).rate(40, 41).onGround().shape(Shape.ring((float) WildShapes.Infrit.R, 3))
						.expand(2.5F).velocity(Velocity.up(0.05F, 0.12F, 0.3F)).module(Module.buoyancy(0.015F)).life(12, 22)
						.size(1.6F, 2.6F).mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(SOUL_FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.INFRIT_IMPACT).lod(100, 700)
				.emit(flash("flash", 6, 5, 0x80E0FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).burst(40).shape(Shape.sphere(2)).velocity(Velocity.radial(0.1F, 0.3F))
						.module(Module.buoyancy(0.02F)).drag(0.1F).life(20, 35).size(1.5F, 2.5F).sprite(FxSprites.FIRE, 2)
						.color(SOUL_FIRE).alpha(0.55F).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(16, 16).life(16, 16)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x80E0FF).build())
				.emit(sparkShower("sparks", 80, 0.3F, 0.9F, SOUL_SPARK, null))
				.emit(sparkShower("lava", 30, 0.2F, 0.6F, SPARK, null))
				.emit(smoke("smoke").burst(16).shape(Shape.dome(2)).velocity(Velocity.radial(0.06F, 0.16F)).life(80, 140).size(2, 3)
						.color(Gradient.of(0, 0x1A2030, 1, 0x3A4050)).alpha(0.8F).emissive(Curve.keys(0, 0.8F, 0.3F, 0))
						.module(Module.buoyancy(0.01F)).module(Module.wind(1)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.INFRIT_BURST).lod(150, 1000)
				.emit(flash("flash", 10, 6, 0x90E8FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).burst(80).shape(Shape.sphere(3)).velocity(Velocity.radial(0.2F, 0.5F))
						.module(Module.buoyancy(0.015F)).drag(0.08F).life(25, 45).size(2, 3.5F).sprite(FxSprites.FIRE, 2)
						.color(SOUL_FIRE).alpha(0.5F).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ring").burst(1).size(20, 20).life(16, 16).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.alphaOverLife(Curve.FADE_OUT).color(0x80E0FF).build())
				.emit(sparkShower("sparks", 160, 0.4F, 1.2F, SOUL_SPARK, null))
				.emit(EmitterDef.builder("wisps").burst(20).shape(Shape.sphere(4)).velocity(Velocity.random(0.05F, 0.2F))
						.module(Module.curl(6, 0.01F)).drag(0.05F).life(40, 70).size(1, 2).sprite(FxSprites.WISP).color(SOUL_FIRE)
						.alpha(0.7F).alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ cactus aftermath

	private static void desert() {
		FxEngine.register(EffectDef.builder(VfxIds.CACTUS_BLOOM).lod(48, 320)
				.emit(EmitterDef.builder("glow").burst(1).size(3, 3).life(10, 10).sprite(FxSprites.GLOW).color(0xFFC0E0)
						.alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(petals("pink", 0xF080B0))
				.emit(petals("gold", 0xF8E070))
				.emit(petals("white", 0xF8F0E0))
				.emit(EmitterDef.builder("pollen").priority(Priority.DETAIL).burst(40).shape(Shape.sphere(2))
						.velocity(Velocity.random(0.05F, 0.2F)).module(Module.jitter(0.01F)).drag(0.05F).life(40, 90).size(0.08F, 0.14F)
						.sprite(FxSprites.FLARE).color(0xFFE890).alphaOverLife(VfxLibrary.TWINKLE).build())
				.build());

		Gradient red = Gradient.of(0, 0xD8884A, 1, 0xB86A38);
		FxEngine.register(EffectDef.builder(VfxIds.SANDSTORM).lod(80, 500).maxAge(400)
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
						.size((float) WildShapes.Cactus.DESERT_R, (float) WildShapes.Cactus.DESERT_R).life(34, 34).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).color(0xE0904A).build())
				.emit(smoke("wall").priority(Priority.HERO).rate(30, 30).onGround().shape(Shape.ring(6, 2)).expand(1.3F)
						.velocity(Velocity.radial(0.15F, 0.3F)).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(40, 80).size(2, 3.5F)
						.sprite(FxSprites.DUST, 2).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.2F)).color(red).alpha(0.8F).shade(0.75F)
						.drag(0.03F).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("grit").priority(Priority.DETAIL).rate(4, 30).onGround().shape(Shape.ring(6, 3)).expand(1.3F)
						.velocity(Velocity.radial(0.4F, 0.7F)).velocity(Velocity.up(0.05F, 0.15F, 0.5F)).gravity(0.02F).drag(0.03F)
						.life(15, 30).size(0.03F, 0.05F).mode(Mode.STRETCHED).layer(Layer.SMOKE).sprite(FxSprites.STREAK)
						.color(0xC07848).build())
				.emit(10, smoke("haze").rate(1.5F, 200).onGround().shape(Shape.disc((float) WildShapes.Cactus.DESERT_R - 5))
						.velocity(Velocity.up(0.01F, 0.03F, 0.5F)).life(80, 140).size(1.5F, 2.5F).sprite(FxSprites.DUST, 2)
						.color(red).alpha(0.35F).module(Module.wind(2)).build())
				.build());
	}

	private static EmitterDef petals(String name, int rgb) {
		return EmitterDef.builder(name).burst(36).shape(Shape.sphere(2.0F)).velocity(Velocity.random(0.12F, 0.35F))
				.velocity(Velocity.up(0.1F, 0.25F, 1.0F)).gravity(0.006F).drag(0.05F).module(Module.jitter(0.01F))
				.life(60, 110).size(0.28F, 0.45F).spin(0.1F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(rgb)
				.emissive(Curve.constant(0.3F)).alphaOverLife(Curve.FADE).build();
	}

	// ------------------------------------------------------------------ blackforge: anvils, iron, plates

	private static void anvils() {
		FxEngine.register(EffectDef.builder(VfxIds.ANVIL_IMPACT).lod(48, 300)
				.emit(smoke("dust").burst(10).shape(Shape.dome(0.8F)).velocity(Velocity.radial(0.1F, 0.22F))
						.velocity(Velocity.up(0.03F, 0.1F, 0.6F)).life(30, 60).size(0.8F, 1.3F).color(Gradient.of(0, 0x6A6058, 1, 0x9A9088))
						.alpha(0.75F).drag(0.06F).build())
				.emit(sparkShower("sparks", 14, 0.15F, 0.45F, SPARK, null))
				.emit(EmitterDef.builder("chips").priority(Priority.DETAIL).burst(6).velocity(Velocity.random(0.15F, 0.35F))
						.velocity(Velocity.up(0.15F, 0.3F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(20, 40)
						.size(0.08F, 0.14F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A4A38).spin(0.1F, 0.3F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.IRON_WAVE).lod(150, 1000)
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
						.size((float) WildShapes.Forge.IRON_R, (float) WildShapes.Forge.IRON_R).life(28, 28).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xD8E0E8).build())
				.emit(EmitterDef.builder("glints").priority(Priority.HERO).rate(30, 26).onGround().shape(Shape.ring(4, 2)).expand(3.0F)
						.velocity(Velocity.up(0.05F, 0.2F, 0.4F)).drag(0.05F).life(10, 22).size(0.3F, 0.6F).sprite(FxSprites.FLARE)
						.color(0xF0F4FF).alphaOverLife(VfxLibrary.TWINKLE).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).rate(20, 26).onGround().shape(Shape.ring(4, 2)).expand(3.0F)
						.velocity(Velocity.up(0.2F, 0.5F, 0.5F)).gravity(0.03F).drag(0.03F).life(15, 30).size(0.04F, 0.06F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(Gradient.of(0, 0xFFFFFF, 1, argb(0xA0B0C0, 0)))
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.PLATE_DUST).lod(80, 600)
				.emit(smoke("dust").burst(12).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.1F, 0.25F))
						.velocity(Velocity.up(0.1F, 0.3F, 0.5F)).life(40, 80).size(1.2F, 2).color(Gradient.of(0, 0x5A4A3A, 1, 0x8A7A68))
						.alpha(0.8F).drag(0.05F).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("rocks").priority(Priority.DETAIL).burst(10).velocity(Velocity.random(0.15F, 0.4F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.6F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 50)
						.size(0.12F, 0.22F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A3E34).spin(0.1F, 0.3F).build())
				.emit(sparkShower("lava", 10, 0.15F, 0.4F, SPARK, null))
				.build());
	}

	// ------------------------------------------------------------------ the hound's fire

	private static void hound() {
		// The mane: played every tick at the head, aimed backwards; it rides along with the head and streams off it.
		FxEngine.register(EffectDef.builder(VfxIds.INFRIT_MANE).lod(100, 800)
				.emit(EmitterDef.builder("flames").priority(Priority.HERO).burst(9).shape(Shape.sphere(3.2F))
						.velocity(Velocity.along(0.12F, 0.35F, 0.6F)).velocity(Velocity.random(0.02F, 0.08F)).inherit(0.9F)
						.module(Module.buoyancy(0.025F)).drag(0.12F).life(10, 20).size(1.2F, 2.4F).sprite(FxSprites.FIRE, 2)
						.color(SOUL_FIRE).alpha(0.65F).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("core").burst(3).shape(Shape.sphere(1.8F)).velocity(Velocity.along(0.05F, 0.15F, 0.5F))
						.inherit(0.95F).drag(0.1F).life(6, 10).size(0.8F, 1.4F).sprite(FxSprites.FIRE, 2)
						.color(Gradient.of(0, 0xF0FFFF, 1, argb(0x60C8FF, 0))).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("wisp").burst(0.5F).shape(Shape.sphere(3)).velocity(Velocity.along(0.05F, 0.12F, 0.4F)).inherit(0.8F)
						.life(14, 26).size(1.2F, 2.0F).color(Gradient.of(0, 0x203A70, 1, 0x30508A)).alpha(0.16F)
						.emissive(Curve.constant(0.6F)).build())
				.build());

		// Sparks and embers wheeling round the head as it flies.
		FxEngine.register(EffectDef.builder(VfxIds.INFRIT_SPARKS).lod(100, 800)
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(12).shape(Shape.shell(4.5F))
						.velocity(Velocity.swirl(0.3F, 0.6F)).velocity(Velocity.random(0.1F, 0.3F)).inherit(1.0F).drag(0.06F)
						.life(12, 26).size(0.06F, 0.1F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SOUL_SPARK)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(6).shape(Shape.shell(5))
						.velocity(Velocity.random(0.05F, 0.15F)).inherit(0.95F).drag(0.04F).life(15, 30).size(0.12F, 0.22F)
						.sprite(FxSprites.FLARE).color(0x90F0FF).alphaOverLife(VfxLibrary.TWINKLE).build())
				.emit(EmitterDef.builder("wisps").burst(2).shape(Shape.shell(4)).velocity(Velocity.random(0.05F, 0.15F)).inherit(0.9F)
						.module(Module.curl(4, 0.02F)).drag(0.05F).life(15, 30).size(1, 1.8F).sprite(FxSprites.WISP).color(SOUL_FIRE)
						.alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.build());

		// Blue fire pouring out of its throat while the jaw hangs open.
		FxEngine.register(EffectDef.builder(VfxIds.INFRIT_BREATH).lod(100, 800)
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(14).velocity(Velocity.along(0.8F, 1.4F, 0.22F)).inherit(1.0F)
						.module(Module.buoyancy(0.01F)).drag(0.07F).life(10, 18).size(0.8F, 1.6F).sprite(FxSprites.FIRE, 2)
						.color(SOUL_FIRE).alpha(0.75F).sizeOverLife(Curve.keys(0, 0.4F, 1, 2.4F)).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(8).velocity(Velocity.along(1.0F, 1.6F, 0.3F)).inherit(1.0F)
						.drag(0.05F).life(8, 16).size(0.05F, 0.08F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SOUL_SPARK)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
