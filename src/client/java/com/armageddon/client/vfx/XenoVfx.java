package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;

/**
 * XENOFORM's effects. Everything here is precise and cold: no fire, no smoke, nothing tumbling. Light in straight
 * lines, points snapping into place, matter coming apart into ordered motes.
 */
final class XenoVfx {
	private XenoVfx() {
	}

	static final Gradient PALE = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0DCFF, 1, argb(0xA090FF, 0));
	static final Gradient CYAN = Gradient.of(0, 0xFFFFFF, 0.4F, 0xA0F4FF, 1, argb(0x40A0FF, 0));

	static void register() {
		// The unfolding: light drawn inward from everywhere to the point, then a flat ring of it snapping out.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_UNFOLD).lod(500, 4000).maxAge(160)
				.cue(0, EffectDef.light(1.2F, 0xC0B0FF))
				.emit(EmitterDef.builder("inward").priority(Priority.HERO).rate(40, 40).shape(Shape.shell(60)).module(Module.attract(0.12F, 2))
						.life(30, 50).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(PALE).build())
				.emit(40, flash("flash", 30, 10, 0xF0ECFF).build())
				.emit(40, EmitterDef.builder("plane").burst(1).mode(Mode.FLAT).fixedRotation().size(90, 90).life(30, 30).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xE0DCFF).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(40, EmitterDef.builder("points").burst(200).shape(Shape.sphere(2)).velocity(Velocity.radial(1.2F, 2.4F)).drag(0.08F).life(40, 70)
						.size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(PALE).alphaOverLife(TWINKLE).build())
				.build());

		// Marked by the scan: a ring of light closing round the target, a vertical bar flicking through it.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_TAG).lod(160, 900).maxAge(40)
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.5F, 2.5F).life(30, 30)
						.sprite(FxSprites.RING).sizeOverLife(Curve.FALL).color(0x80F0FF).alpha(0.9F).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("bar").burst(6).shape(Shape.column(0.6F, 2.2F)).life(6, 14).size(0.08F, 0.12F).mode(Mode.STRETCHED).stretch(6)
						.velocity(Velocity.up(0.05F, 0.1F, 0)).sprite(FxSprites.STREAK).color(CYAN).build())
				.build());

		// Disassembled: the body comes apart into a lattice of motes that rise and wink out, one after another.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_DEREZ).lod(200, 1400).maxAge(80)
				.cue(0, EffectDef.light(0.8F, 0xC0F0FF))
				.emit(flash("flash", 3, 6, 0xF0FAFF).build())
				.emit(EmitterDef.builder("motes").priority(Priority.HERO).burst(70).shape(Shape.box(0.4F, 1.0F, 0.4F)).velocity(Velocity.up(0.05F, 0.25F, 0.05F))
						.life(20, 60).size(0.1F, 0.18F).fixedRotation().sprite(FxSprites.GLOW).color(CYAN).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("lines").burst(10).shape(Shape.box(0.5F, 1.0F, 0.5F)).velocity(Velocity.up(0.4F, 0.8F, 0)).life(10, 20).size(0.05F, 0.08F)
						.mode(Mode.STRETCHED).stretch(8).sprite(FxSprites.STREAK).color(PALE).build())
				.build());

		// The edge of the harvest: the ground there lit from below, motes peeling off in straight lines.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_FRONT).lod(300, 2000)
				.emit(EmitterDef.builder("motes").burst(6).onGround().shape(Shape.disc(2)).velocity(Velocity.up(0.3F, 0.6F, 0)).life(20, 40).size(0.12F, 0.2F)
						.fixedRotation().sprite(FxSprites.GLOW).color(CYAN).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glow").burst(1).onGround().offset(0, 0.2F, 0).mode(Mode.FLAT).fixedRotation().size(3, 4).life(10, 16)
						.sprite(FxSprites.GLOW).color(0x90E8FF).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The monolith sets down: a ring of light racing out across the whole plain, a flat flash, motes thrown up.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_LAND).lod(600, 4000).maxAge(120)
				.cue(0, EffectDef.light(1.6F, 0xE0E0FF))
				.emit(flash("flash", 40, 12, 0xF4F4FF).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(130, 130).life(40, 40)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xE8E4FF).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(4, EmitterDef.builder("ring2").burst(1).onGround().offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(90, 90).life(40, 40)
						.sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).color(0x80F0FF).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("motes").priority(Priority.HERO).burst(300).onGround().shape(Shape.disc(12)).velocity(Velocity.radial(0.6F, 1.6F))
						.velocity(Velocity.up(0.1F, 0.4F, 0)).drag(0.05F).life(30, 70).size(0.15F, 0.3F).fixedRotation().sprite(FxSprites.GLOW).color(PALE)
						.alphaOverLife(FLICKER).build())
				.build());

		// After it leaves: glowing pixels drifting down out of the sky round the watcher, winking out.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_PIXELS).lod(80, 400)
				.emit(EmitterDef.builder("pixels").burst(10).shape(Shape.box(18, 4, 18)).offset(0, 16, 0).velocity(Velocity.up(-0.25F, -0.1F, 0.02F))
						.life(40, 80).size(0.15F, 0.3F).fixedRotation().sprite(FxSprites.GLOW).color(CYAN).alphaOverLife(FLICKER).build())
				.build());

		// Leaving: a white-out at the monolith's crown and the last of the light going straight up.
		FxEngine.register(EffectDef.builder(VfxIds.XENO_DEPART).lod(800, 5000).maxAge(120)
				.cue(0, EffectDef.light(2.0F, 0xF0F0FF))
				.emit(flash("flash", 60, 14, 0xFFFFFF).build())
				.emit(EmitterDef.builder("up").priority(Priority.HERO).rate(30, 50, Curve.FALL).shape(Shape.disc(2)).velocity(Velocity.up(4, 6, 0)).life(20, 40)
						.size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(8).sprite(FxSprites.STREAK).color(PALE).build())
				.emit(EmitterDef.builder("halo").burst(1).mode(Mode.FLAT).fixedRotation().size(60, 60).life(30, 30).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xF0ECFF).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
