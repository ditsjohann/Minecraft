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
 * Effects for the bombs rebuilt from scratch: the Aurora, the Big Bang, Sixteen Chunks, the Zombie Apocalypse, the
 * Spectral Ice and the Prism.
 */
final class RedoVfx {
	private RedoVfx() {
	}

	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient STAR = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE8F0FF, 1, argb(0xC0D0FF, 0));
	static final Gradient AURORA = Gradient.of(0, 0xC0FFD8, 0.3F, 0x50FF90, 0.65F, 0x40D0FF, 1, argb(0xA040FF, 0));

	static void register() {
		aurora();
	}

	// ------------------------------------------------------------------ Aurora

	private static void aurora() {
		// Night: stars coming out over the whole sky, and now and then a shooting star.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_STARS).lod(600, 4000).maxAge(640)
				.emit(EmitterDef.builder("stars").rate(5, 560).shape(Shape.dome(150)).offset(0, 40, 0).life(80, 160).size(0.6F, 1.2F)
						.sprite(FxSprites.FLARE).color(STAR).alphaOverLife(Curve.of(x -> Math.min(1, x * 6) * Math.min(1, (1 - x) * 4) * (0.7 + 0.3 * Math.sin(x * 40)))).build())
				.emit(EmitterDef.builder("meteors").rate(0.06F, 560).shape(Shape.dome(120)).offset(0, 70, 0).velocity(Velocity.random(2.5F, 3.5F))
						.velocity(Velocity.up(-1.5F, -1.0F, 0.3F)).life(10, 16).size(0.4F, 0.6F).mode(Mode.STRETCHED).stretch(6).sprite(FxSprites.STREAK)
						.color(STAR).build())
				.build());

		// Where a hem burns the land, tinted the curtain's colour: sparks lifting off, glass shards, a pool of light.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_SPARK).tint(0x50FF90).lod(160, 1000)
				.emit(EmitterDef.builder("sparks").burst(14).velocity(Velocity.up(0.3F, 0.8F, 0.4F)).module(Module.jitter(0.02F)).life(16, 30).size(0.1F, 0.16F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WHITE).tinted().build())
				.emit(EmitterDef.builder("glass").burst(8).velocity(Velocity.up(0.2F, 0.5F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(20, 34)
						.size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.4F, 3).life(24, 30)
						.sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The solar storm: a flash, three shockwaves of coloured light over the ground, a fountain of light up the
		// column, and the colours raining back down.
		FxEngine.register(EffectDef.builder(VfxIds.AURORA_STORM).lod(500, 3600).maxAge(260)
				.cue(0, EffectDef.light(1.6F, 0x80FFC0))
				.emit(flash("flash", 50, 14, 0xE0FFF0).build())
				.emit(EmitterDef.builder("ring1").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(70, 70).life(40, 40).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0x50FF90).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(6, EmitterDef.builder("ring2").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(100, 100).life(46, 46).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0x40D0FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(12, EmitterDef.builder("ring3").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(130, 130).life(52, 52).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xA040FF).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).rate(40, 30, Curve.FALL).shape(Shape.disc(3)).velocity(Velocity.up(3.0F, 5.0F, 0.06F))
						.life(20, 34).size(0.6F, 1.0F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(AURORA).build())
				.emit(EmitterDef.builder("wisps").priority(Priority.HERO).burst(60).shape(Shape.disc(4)).velocity(Velocity.radial(1.0F, 2.0F))
						.velocity(Velocity.up(0.2F, 0.6F, 0.2F)).velocity(Velocity.swirl(0.4F, 0.8F)).drag(0.03F).module(Module.curl(10, 0.02F)).life(60, 100)
						.size(2.5F, 4).sprite(FxSprites.WISP).color(AURORA).alpha(0.7F).build())
				.emit(20, EmitterDef.builder("rain").rate(24, 140).shape(Shape.disc(80)).offset(0, 50, 0).velocity(Velocity.up(-0.3F, -0.15F, 0.2F))
						.module(Module.jitter(0.01F)).life(80, 140).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(AURORA).alphaOverLife(TWINKLE).build())
				.build());
	}
}
