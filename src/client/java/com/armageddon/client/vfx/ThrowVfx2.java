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
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The second set of throwables: seismic pulse mine, ion fog, stasis mine, quantum fracture, crystal cataclysm,
 * voidstorm, micro event horizon, mirror dimension, snow globe and slimequake. Trails while they fly and sit, and the
 * extra layers of each one going off.
 */
final class ThrowVfx2 {
	private ThrowVfx2() {
	}

	static final Gradient EARTH = Gradient.of(0, 0x8A7A60, 1, 0xA89A80);
	static final Gradient ION = Gradient.of(0, 0xC0FFF0, 1, 0x60D0C0);
	static final Gradient STASIS = fade(0x80E0FF, 0x1060A0);
	static final Gradient AMETHYST = fade(0xC080FF, 0x401070);
	static final Gradient VOID = fade(0x9060FF, 0x200840);
	static final Gradient ACCRETION = Gradient.of(0, 0xFFFFFF, 0.2F, 0xFFD080, 0.6F, 0xFF6020, 1, argb(0x601000, 0));
	static final Gradient SLIME = Gradient.of(0, 0xB8FF70, 1, 0x58B030);
	static final int[] RAINBOW = {0xFF4040, 0xFFA030, 0xFFF040, 0x50FF60, 0x40C0FF, 0x6060FF, 0xC050FF};

	static void register() {
		trails();
		detonations();
	}

	// ------------------------------------------------------------------ in flight and on the ground

	private static void trails() {
		FxEngine.register(EffectDef.builder(VfxIds.SEISMIC_HUM).lod(16, 96)
				.emit(smoke("dust").burst(0.6F).velocity(Velocity.random(0.005F, 0.02F)).gravity(0.004F).life(15, 25).size(0.12F, 0.2F)
						.sprite(FxSprites.DUST, 2).color(EARTH).alpha(0.5F).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.2F, 0.2F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFA040).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ION_GLOW).lod(16, 96)
				.emit(smoke("mist").burst(1).velocity(Velocity.random(0.005F, 0.02F)).inherit(0.1F).life(20, 35).size(0.2F, 0.35F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(ION).alpha(0.4F).emissive(Curve.constant(0.9F)).build())
				.emit(EmitterDef.builder("sparkle").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).life(10, 20)
						.size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0xC0FFF0).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.STASIS_HEX).lod(16, 96)
				.emit(EmitterDef.builder("ring").burst(1).inherit(1).size(0.5F, 0.5F).life(2, 2).fixedRotation().spin(0.05F, 0.05F)
						.sprite(FxSprites.RING).color(0x80E0FF).alpha(0.6F).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).burst(1).shape(Shape.sphere(0.6F)).life(25, 45).size(0.05F, 0.08F)
						.sprite(FxSprites.FLARE).color(0xB0F0FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.QUANTUM_GLITCH).lod(16, 96)
				.emit(glitch("magenta", 0xFF40FF, 1))
				.emit(glitch("cyan", 0x40FFFF, 1))
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.CRYSTAL_GLINT).lod(16, 96)
				.emit(EmitterDef.builder("glints").burst(1).shape(Shape.sphere(0.4F)).velocity(Velocity.random(0.01F, 0.03F)).life(10, 20)
						.size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0xE0C0FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.35F, 0.35F).life(2, 2).sprite(FxSprites.GLOW).color(0xA060E0)
						.alpha(0.5F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.VOID_CRACKLE).lod(16, 96)
				.emit(smoke("wisps").burst(1).shape(Shape.sphere(0.4F)).velocity(Velocity.random(0.01F, 0.03F)).module(Module.curl(1.5F, 0.006F))
						.life(12, 22).size(0.2F, 0.35F).sprite(FxSprites.WISP).color(0x100420).alpha(0.7F).build())
				.emit(EmitterDef.builder("lightning").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.15F, 0.3F))
						.module(Module.jitter(0.1F)).drag(0.4F).inherit(0.8F).life(2, 4).size(0.015F, 0.025F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(0xB080FF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.HORIZON_PULL).lod(16, 96)
				.emit(EmitterDef.builder("inflow").burst(2).shape(Shape.shell(1.4F)).module(Module.attract(0.03F, 0.12F)).inherit(1).drag(0.1F)
						.life(8, 14).size(0.02F, 0.03F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ACCRETION).build())
				.emit(smoke("core").burst(1).inherit(1).size(0.22F, 0.22F).life(2, 2).fixedRotation().sprite(FxSprites.GLOW).sizeOverLife(Curve.ONE)
						.alphaOverLife(Curve.ONE).color(0x000000).shade(1).build())
				.build());

		EffectDef.Builder shimmer = EffectDef.builder(VfxIds.MIRROR_SHIMMER).lod(16, 96)
				.emit(EmitterDef.builder("glints").burst(1).shape(Shape.sphere(0.5F)).life(8, 16).size(0.06F, 0.1F).sprite(FxSprites.FLARE)
						.color(0xF0F4FF).alphaOverLife(TWINKLE).build());
		for (int k = 0; k < RAINBOW.length; k++) {
			shimmer.emit(EmitterDef.builder("prism" + k).priority(Priority.DETAIL).burst(0.15F).velocity(Velocity.random(0.03F, 0.08F))
					.drag(0.08F).life(8, 14).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RAINBOW[k]).build());
		}
		FxEngine.register(shimmer.build());

		FxEngine.register(EffectDef.builder(VfxIds.SNOW_SWIRL).lod(16, 96)
				.emit(EmitterDef.builder("flakes").burst(2).shape(Shape.sphere(0.4F)).velocity(Velocity.swirl(0.03F, 0.06F)).gravity(0.004F)
						.module(Module.jitter(0.004F)).life(20, 35).size(0.04F, 0.06F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xFFFFFF)
						.emissive(Curve.constant(0.6F)).build())
				.emit(EmitterDef.builder("glitter").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).life(10, 20)
						.size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0xF0F8FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SLIME_DRIP).lod(16, 96)
				.emit(EmitterDef.builder("drips").burst(0.6F).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.03F).collide(0, 0.3F, 0)
						.life(15, 30).size(0.07F, 0.11F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(SLIME).emissive(Curve.constant(0.5F)).build())
				.emit(EmitterDef.builder("bubble").priority(Priority.DETAIL).burst(0.4F).shape(Shape.sphere(0.25F)).life(8, 14).size(0.08F, 0.15F)
						.sprite(FxSprites.RING).color(0xB8FF70).sizeOverLife(Curve.RISE).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	/** Glitch squares that blink and jump about, in one colour. */
	private static EmitterDef glitch(String name, int rgb, float count) {
		return EmitterDef.builder(name).burst(count).shape(Shape.sphere(0.6F)).module(Module.jitter(0.12F)).drag(0.6F).life(3, 7)
				.size(0.06F, 0.12F).fixedRotation().sprite(FxSprites.GLOW).color(rgb).alphaOverLife(FLICKER).build();
	}

	// ------------------------------------------------------------------ detonations

	private static void detonations() {
		EffectDef.Builder seismic = EffectDef.builder(VfxIds.SEISMIC_BURST).lod(32, 240);
		for (int k = 0; k < 3; k++) {
			seismic.emit(k * 6, EmitterDef.builder("quake" + k).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(9, 9).life(14, 14)
					.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(0.8F - k * 0.2F).alphaOverLife(Curve.FADE_OUT).color(0xFFA040).build());
		}
		FxEngine.register(seismic
				.emit(smoke("wall").rate(20, 12).onGround().shape(Shape.ring(1, 0.6F)).expand(0.7F).velocity(Velocity.radial(0.12F, 0.25F))
						.velocity(Velocity.up(0.04F, 0.1F, 0.4F)).life(30, 55).size(0.7F, 1.2F).sprite(FxSprites.DUST, 2).color(EARTH).alpha(0.75F)
						.drag(0.05F).build())
				.emit(EmitterDef.builder("rocks").priority(Priority.DETAIL).burst(16).onGround().shape(Shape.disc(5)).velocity(Velocity.up(0.25F, 0.5F, 0.4F))
						.gravity(0.05F).collide(0.3F, 0.6F, 1).life(25, 45).size(0.1F, 0.2F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(0x5A4E44).spin(0.1F, 0.3F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ION_BLOOM).lod(48, 300).maxAge(300)
				.emit(EmitterDef.builder("flash").burst(1).size(3, 3).life(6, 6).sprite(FxSprites.GLOW).color(0xC0FFF0).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("fog").priority(Priority.HERO).burst(28).shape(Shape.dome(2)).velocity(Velocity.radial(0.06F, 0.16F)).drag(0.05F)
						.life(160, 220).size(1.4F, 2.2F).sizeOverLife(Curve.keys(0, 0.4F, 0.2F, 1.4F, 1, 2)).color(ION).alpha(0.45F)
						.emissive(Curve.constant(0.9F)).module(Module.curl(6, 0.002F)).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).rate(2, 180).shape(Shape.sphere(6)).module(Module.jitter(0.003F))
						.life(30, 60).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0xC0FFF0).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.STASIS_FIELD).lod(32, 240).maxAge(200)
				.emit(flash("flash", 2, 4, 0x80E0FF).build())
				.emit(EmitterDef.builder("ring").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(120, 120)
						.sprite(FxSprites.RING).spin(0.01F, 0.01F).color(0x80E0FF).alpha(0.7F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("inner").burst(1).onGround().mode(Mode.FLAT).size(4, 4).life(120, 120).sprite(FxSprites.SHOCK)
						.spin(0.02F, 0.02F).color(0x60C0FF).alpha(0.5F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("shell").burst(60).shape(Shape.shell(5)).life(80, 120).size(0.06F, 0.1F).sprite(FxSprites.FLARE)
						.color(0xB0F0FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("snap").burst(1).size(5, 5).life(8, 8).sprite(FxSprites.SHOCK).sizeOverLife(Curve.keys(0, 1.4F, 1, 1))
						.color(STASIS).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.QUANTUM_BURST).lod(32, 240)
				.emit(EmitterDef.builder("flash").burst(1).size(2.5F, 2.5F).life(4, 4).sprite(FxSprites.GLOW).color(0xFF80FF).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("ring").burst(1).size(5, 5).life(10, 10).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0xFF40FF).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("pixels_m").burst(40).velocity(Velocity.random(0.1F, 0.4F)).module(Module.jitter(0.08F)).drag(0.15F)
						.life(10, 30).size(0.1F, 0.2F).fixedRotation().layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xFF40FF)
						.emissive(Curve.ONE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("pixels_c").burst(40).velocity(Velocity.random(0.1F, 0.4F)).module(Module.jitter(0.08F)).drag(0.15F)
						.life(10, 30).size(0.1F, 0.2F).fixedRotation().layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0x40FFFF)
						.emissive(Curve.ONE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("static").priority(Priority.DETAIL).burst(30).shape(Shape.sphere(4)).life(6, 16).size(0.05F, 0.09F)
						.sprite(FxSprites.FLARE).color(0xFFFFFF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.CRYSTAL_BURST).lod(32, 240)
				.emit(EmitterDef.builder("glow").burst(1).size(2.5F, 2.5F).life(8, 8).sprite(FxSprites.GLOW).color(0xC080FF).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(12, 12).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xC080FF).build())
				.emit(EmitterDef.builder("shards").burst(40).velocity(Velocity.random(0.25F, 0.6F)).velocity(Velocity.up(0.1F, 0.3F, 0.8F))
						.gravity(0.035F).drag(0.03F).collide(0.2F, 0.5F, 1).life(15, 30).size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(1.2F)
						.sprite(FxSprites.STREAK).color(AMETHYST).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(30).shape(Shape.sphere(3)).life(25, 50).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xE0C0FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.VOIDSTORM_BURST).lod(40, 300).maxAge(200)
				.emit(flash("flash", 2.5F, 5, 0x9060FF).build())
				.emit(smoke("storm").priority(Priority.HERO).rate(4, 80).onGround().shape(Shape.ring(5, 2)).velocity(Velocity.swirl(0.15F, 0.3F))
						.velocity(Velocity.up(0.03F, 0.08F, 0.4F)).module(Module.vortex(0.01F, 0.006F)).drag(0.03F).life(40, 70).size(1.2F, 2)
						.color(Gradient.of(0, 0x0A0418, 1, 0x241040)).alpha(0.7F).emissive(Curve.constant(0.2F)).build())
				.emit(EmitterDef.builder("lightning").rate(1.5F, 80).shape(Shape.sphere(4)).velocity(Velocity.random(0.3F, 0.7F))
						.module(Module.jitter(0.15F)).drag(0.4F).life(3, 7).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(VOID).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("rubble").priority(Priority.DETAIL).rate(2, 70).onGround().shape(Shape.ring(6, 2))
						.velocity(Velocity.swirl(0.15F, 0.3F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F)).module(Module.vortex(0.012F, 0.008F))
						.drag(0.02F).life(30, 50).size(0.1F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x3A3440).spin(0.1F, 0.3F).build())
				.build());

		// A black hole for a moment: an accretion disk of hot light wheeling round a growing core, then a pop.
		int pop = 36;
		FxEngine.register(EffectDef.builder(VfxIds.HORIZON_COLLAPSE).lod(32, 240)
				.emit(EmitterDef.builder("disk").priority(Priority.HERO).rate(10, 32).shape(Shape.ring(2.2F, 0.8F)).velocity(Velocity.swirl(0.2F, 0.3F))
						.module(Module.vortex(0.03F, 0.02F)).life(12, 20).size(0.04F, 0.07F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(ACCRETION).build())
				.emit(EmitterDef.builder("inflow").rate(6, 32).shape(Shape.shell(5)).module(Module.attract(0.04F, 0.4F)).drag(0.05F)
						.life(25, 40).size(0.025F, 0.04F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(ACCRETION)
						.alphaOverLife(Curve.BELL).build())
				.emit(smoke("core").priority(Priority.HERO).burst(1).life(pop, pop).size(0.9F, 0.9F).fixedRotation().sprite(FxSprites.GLOW)
						.sizeOverLife(Curve.keys(0, 0.2F, 0.85F, 1, 1, 0.3F)).alphaOverLife(Curve.keys(0, 0, 0.2F, 1, 1, 1)).color(0x000000).shade(1).build())
				.emit(EmitterDef.builder("halo").burst(1).life(pop, pop).size(1.6F, 1.6F).sprite(FxSprites.GLOW).color(0xFF8040).alpha(0.4F)
						.alphaOverLife(Curve.BELL).build())
				.emit(pop, flash("pop", 2.5F, 5, 0xFFC080).build())
				.emit(pop, sparkShower("sparks", 50, 0.3F, 0.8F, ACCRETION, null))
				.build());

		EffectDef.Builder mirror = EffectDef.builder(VfxIds.MIRROR_FLASH).lod(32, 240)
				.emit(flash("flash", 3, 5, 0xF0F4FF).build())
				.emit(EmitterDef.builder("plane").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(9, 9).life(16, 16).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xE8F0FF).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(40).shape(Shape.sphere(4)).life(15, 35).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xF0F4FF).alphaOverLife(TWINKLE).build());
		for (int k = 0; k < RAINBOW.length; k++) {
			mirror.emit(EmitterDef.builder("prism" + k).burst(10).velocity(Velocity.random(0.2F, 0.5F)).drag(0.06F).life(10, 22)
					.size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RAINBOW[k]).alphaOverLife(Curve.FADE_OUT).build());
		}
		FxEngine.register(mirror.build());

		FxEngine.register(EffectDef.builder(VfxIds.SNOW_GLOBE_BURST).lod(32, 240).maxAge(200)
				.emit(EmitterDef.builder("dome").burst(1).size(7, 7).life(14, 14).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(0xF0F8FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("puff").burst(40).shape(Shape.sphere(1)).velocity(Velocity.random(0.15F, 0.4F)).drag(0.08F).gravity(0.004F)
						.life(30, 60).size(0.05F, 0.08F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xFFFFFF).emissive(Curve.constant(0.6F)).build())
				.emit(EmitterDef.builder("glitter").burst(30).shape(Shape.sphere(4)).life(20, 40).size(0.05F, 0.09F).sprite(FxSprites.FLARE)
						.color(0xF0F8FF).alphaOverLife(TWINKLE).build())
				.emit(4, EmitterDef.builder("snowfall").rate(4, 120).offset(0, 7, 0).shape(Shape.disc(6)).gravity(0.002F)
						.module(Module.jitter(0.004F)).drag(0.08F).life(60, 100).size(0.05F, 0.08F).layer(Layer.SMOKE).sprite(FxSprites.GLOW)
						.color(0xFFFFFF).emissive(Curve.constant(0.6F)).build())
				.build());

		EffectDef.Builder slime = EffectDef.builder(VfxIds.SLIME_BURST).lod(32, 240)
				.emit(EmitterDef.builder("splash").burst(40).velocity(Velocity.random(0.15F, 0.45F)).velocity(Velocity.up(0.15F, 0.35F, 0.9F))
						.gravity(0.05F).collide(0.5F, 0.6F, 2).life(20, 40).size(0.08F, 0.14F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(SLIME)
						.emissive(Curve.constant(0.5F)).build())
				.emit(smoke("puff").burst(10).shape(Shape.dome(1)).velocity(Velocity.radial(0.08F, 0.2F)).life(25, 45).size(0.6F, 1)
						.color(Gradient.of(0, 0x9AE060, 1, 0x7AA860)).alpha(0.5F).emissive(Curve.constant(0.3F)).build());
		for (int k = 0; k < 3; k++) {
			slime.emit(k * 8, EmitterDef.builder("bounce" + k).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(7, 7).life(12, 12)
					.sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).alpha(0.8F - k * 0.2F).alphaOverLife(Curve.FADE_OUT).color(0x80FF40).build());
		}
		FxEngine.register(slime.build());
	}
}
