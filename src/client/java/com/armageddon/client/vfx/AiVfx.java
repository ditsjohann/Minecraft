package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** AI TAKEOVER's effects: cold red light, sparks and smoke from the drones' lasers, and the EMP. */
final class AiVfx {
	private AiVfx() {
	}

	static final Gradient RED = Gradient.of(0, 0xFFE0D0, 0.3F, 0xFF4030, 0.7F, 0xE01010, 1, argb(0x600404, 0));
	static final Gradient EMP = Gradient.of(0, 0xFFFFFF, 0.4F, 0xB0E8FF, 1, argb(0x4080FF, 0));

	static void register() {
		// The core rising: dust and grit bursting from its foot.
		FxEngine.register(EffectDef.builder(VfxIds.AI_RISE).lod(300, 2000)
				.emit(smoke("dust").burst(5).onGround().shape(Shape.ring(6, 2)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.2F))
						.life(40, 70).size(3, 5).color(Gradient.of(0, 0x5A5450, 1, 0x3A3632)).alpha(0.6F).build())
				.emit(EmitterDef.builder("grit").burst(8).onGround().shape(Shape.ring(6, 2)).velocity(Velocity.up(0.4F, 0.8F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(30, 50).size(0.2F, 0.35F).spin(-0.4F, 0.4F).sprite(FxSprites.DUST).color(0x2A2A2E).build())
				.build());

		// It wakes: a red flash, a ring of red light racing out, red pixels scattering.
		FxEngine.register(EffectDef.builder(VfxIds.AI_BOOT).lod(500, 3200).maxAge(120)
				.cue(0, EffectDef.light(1.6F, 0xFF2010))
				.emit(flash("flash", 30, 12, 0xFF4030).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(80, 80).life(30, 30).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).color(0xFF2010).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("pixels").priority(Priority.HERO).burst(240).shape(Shape.sphere(3)).velocity(Velocity.random(0.6F, 1.6F)).drag(0.05F)
						.life(30, 70).size(0.25F, 0.4F).fixedRotation().sprite(FxSprites.GLOW).color(RED).alphaOverLife(FLICKER).build())
				.build());

		// A drone's laser hits: sparks, a burst of red, a puff of smoke.
		FxEngine.register(EffectDef.builder(VfxIds.AI_ZAP).lod(160, 1000).maxAge(50)
				.emit(flash("flash", 3, 5, 0xFF4030).build())
				.emit(EmitterDef.builder("sparks").burst(14).velocity(Velocity.random(0.3F, 0.7F)).gravity(0.04F).life(10, 22).size(0.08F, 0.14F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(RED).build())
				.emit(smoke("smoke").burst(3).velocity(Velocity.up(0.05F, 0.15F, 0.2F)).life(30, 50).size(1, 1.8F)
						.color(Gradient.of(0, 0x3A3030, 1, 0x201A1A)).alpha(0.5F).build())
				.build());

		// Uploading: red data rising out of the core in straight lines, and pixels winking out.
		FxEngine.register(EffectDef.builder(VfxIds.AI_UPLOAD).lod(500, 3200)
				.emit(EmitterDef.builder("data").burst(6).shape(Shape.disc(3)).velocity(Velocity.up(2.0F, 3.5F, 0)).life(30, 60).size(0.2F, 0.35F)
						.mode(Mode.STRETCHED).stretch(6).sprite(FxSprites.STREAK).color(RED).build())
				.emit(EmitterDef.builder("bits").burst(4).shape(Shape.disc(5)).velocity(Velocity.up(1.0F, 2.0F, 0.1F)).life(20, 50).size(0.3F, 0.5F)
						.fixedRotation().sprite(FxSprites.GLOW).color(RED).alphaOverLife(FLICKER).build())
				.build());

		// The EMP: a white-blue flash, a shell of light racing out flat over the city, crackling arcs.
		FxEngine.register(EffectDef.builder(VfxIds.AI_EMP).lod(700, 4000).maxAge(120)
				.cue(0, EffectDef.light(2.2F, 0xC0E8FF))
				.emit(flash("flash", 60, 12, 0xF0FAFF).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 1, 0).mode(Mode.FLAT).fixedRotation().size(170, 170).life(34, 34)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xC0E8FF).alpha(0.85F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("arcs").priority(Priority.HERO).burst(300).shape(Shape.dome(30)).velocity(Velocity.radial(1.5F, 3.0F)).drag(0.03F)
						.life(16, 30).size(0.2F, 0.35F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(EMP).build())
				.emit(EmitterDef.builder("sparks").burst(200).shape(Shape.sphere(6)).offset(0, 60, 0).velocity(Velocity.random(0.6F, 1.6F)).drag(0.04F)
						.life(20, 50).size(0.15F, 0.3F).sprite(FxSprites.FLARE).color(EMP).alphaOverLife(FLICKER).build())
				.build());
	}
}
