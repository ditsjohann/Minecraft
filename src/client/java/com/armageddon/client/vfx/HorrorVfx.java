package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the horror celestials: the Terror Maniac, the Pumpkin Horseman and the Portal of Hell. */
final class HorrorVfx {
	private HorrorVfx() {
	}

	static final Gradient BLOOD = Gradient.of(0, 0xFF6040, 0.3F, 0xFF2010, 0.7F, 0x900808, 1, argb(0x400404, 0));
	static final Gradient ASH = Gradient.of(0, 0x3A2020, 0.5F, 0x241414, 1, 0x140A0A);

	static void register() {
		maniac();
	}

	private static void maniac() {
		// The air in the inside-out world: red motes and flakes of black ash drifting up out of the ground.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_ASH).lod(80, 400)
				.emit(EmitterDef.builder("motes").burst(4).shape(Shape.box(16, 6, 16)).offset(0, 3, 0).velocity(Velocity.up(0.02F, 0.06F, 0.02F))
						.module(Module.curl(6, 0.004F)).life(80, 140).size(0.06F, 0.12F).sprite(FxSprites.FLARE).color(BLOOD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ash").burst(5).shape(Shape.box(16, 6, 16)).offset(0, 3, 0).velocity(Velocity.up(0.01F, 0.04F, 0.03F))
						.module(Module.curl(5, 0.005F)).life(100, 160).size(0.08F, 0.16F).spin(-0.2F, 0.2F).sprite(FxSprites.DUST).color(ASH).build())
				.build());

		// The rift opening: a red flash, a column of black smoke boiling out, embers and earth thrown up.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_RIFT).lod(400, 2400).maxAge(200)
				.cue(0, EffectDef.light(1.4F, 0xFF2010))
				.emit(flash("flash", 24, 12, 0xFF4020).build())
				.emit(smoke("column").priority(Priority.HERO).rate(10, 80, Curve.FALL).shape(Shape.disc(5)).velocity(Velocity.up(0.4F, 0.9F, 0.2F))
						.module(Module.curl(10, 0.01F)).life(80, 140).size(5, 9).sizeOverLife(Curve.RISE).color(Gradient.of(0, 0x2A1010, 1, 0x100808))
						.alpha(0.75F).build())
				.emit(EmitterDef.builder("embers").rate(20, 80, Curve.FALL).shape(Shape.disc(4)).velocity(Velocity.up(0.5F, 1.2F, 0.4F)).gravity(0.01F)
						.module(Module.curl(6, 0.01F)).life(30, 60).size(0.15F, 0.3F).sprite(FxSprites.FLARE).color(BLOOD).build())
				.emit(smoke("earth").burst(30).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.4F, 0.9F, 0.6F)).gravity(0.03F).life(40, 70).size(2.5F, 4)
						.color(Gradient.of(0, 0x5A4A3A, 1, 0x3A2E24)).alpha(0.7F).build())
				.build());

		// The curse taking hold: red wisps spiralling round the victim, black threads pulling upward.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_CURSE).lod(120, 700).maxAge(40)
				.emit(EmitterDef.builder("wisps").burst(6).shape(Shape.ring(0.9F, 0.2F)).offset(0, 1, 0).velocity(Velocity.swirl(0.15F, 0.25F))
						.velocity(Velocity.up(0.05F, 0.12F, 0)).module(Module.vortex(0.08F, 0.02F)).life(20, 34).size(0.5F, 0.8F).sprite(FxSprites.WISP)
						.color(BLOOD).alpha(0.7F).build())
				.emit(EmitterDef.builder("threads").burst(5).shape(Shape.sphere(0.6F)).offset(0, 1, 0).velocity(Velocity.up(0.3F, 0.6F, 0.05F)).life(8, 16)
						.size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(8).sprite(FxSprites.STREAK).color(0x200808).build())
				.build());

		// Broken: a crack of red light, a puff of black dust.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_SNAP).lod(160, 900).maxAge(60)
				.emit(flash("flash", 4, 5, 0xFF2010).build())
				.emit(smoke("dust").burst(10).shape(Shape.sphere(0.6F)).velocity(Velocity.random(0.1F, 0.3F)).drag(0.08F).life(30, 50).size(0.8F, 1.4F)
						.color(Gradient.of(0, 0x301010, 1, 0x180808)).alpha(0.7F).build())
				.emit(EmitterDef.builder("shards").burst(16).shape(Shape.sphere(0.4F)).velocity(Velocity.random(0.3F, 0.6F)).gravity(0.04F).life(14, 24)
						.size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(BLOOD).build())
				.build());

		// It comes apart: a storm of black moths bursting out and fluttering away, embers, a dark red shockwave.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_SHATTER).lod(600, 3600).maxAge(240)
				.cue(0, EffectDef.light(1.6F, 0xFF2010))
				.emit(flash("flash", 40, 14, 0xFF3020).build())
				.emit(EmitterDef.builder("moths").priority(Priority.HERO).burst(900).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.4F, 1.2F))
						.velocity(Velocity.up(0.05F, 0.3F, 0.3F)).drag(0.02F).module(Module.curl(6, 0.03F)).module(Module.jitter(0.06F)).life(80, 180)
						.size(0.25F, 0.45F).spin(-1.5F, 1.5F).sprite(FxSprites.DUST).color(0x0E0808).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("embers").burst(300).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.3F, 1.0F)).module(Module.curl(8, 0.02F))
						.life(60, 140).size(0.12F, 0.22F).sprite(FxSprites.FLARE).color(BLOOD).alphaOverLife(TWINKLE).build())
				.emit(smoke("smoke").burst(60).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.3F, 0.7F)).drag(0.03F).life(100, 160).size(5, 9)
						.color(Gradient.of(0, 0x301010, 1, 0x120808)).alpha(0.6F).build())
				.emit(EmitterDef.builder("shock").burst(1).offset(0, -24, 0).onGround().mode(Mode.FLAT).fixedRotation().size(90, 90).life(36, 36)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xC01008).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
