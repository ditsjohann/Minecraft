package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.EMBER;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.INNER_GLOW;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;

/**
 * Launchers and strikes from the sky: MIRV bomblets, the singularity projector, the cataclysm mortar, meteors, rods from
 * God, airstrike bombs, the targeting lock the designators paint, the orbital strike's impact and the storm caller.
 * (Mini nukes and the barrage's warheads share the gatling's nuke-round trail.)
 */
final class StrikeVfx {
	private StrikeVfx() {
	}

	static final Gradient DUST = Gradient.of(0, 0x7A6A58, 1, 0xA89A88);
	static final Gradient STORM = Gradient.of(0, 0x1A1C24, 1, 0x30343E);

	static void register() {
		trails();
		strikes();
	}

	// ------------------------------------------------------------------ what flies

	private static void trails() {
		FxEngine.register(EffectDef.builder(VfxIds.BOMBLET_TRAIL).lod(32, 240)
				.emit(EmitterDef.builder("exhaust").burst(2).span().life(3, 5).size(0.25F, 0.35F).sprite(FxSprites.FIRE, 2)
						.color(Gradient.of(0, 0xFFFFE0, 1, 0xFF9030)).build())
				.emit(smoke("smoke").burst(2.5F).span().velocity(Velocity.random(0.01F, 0.03F)).life(30, 55).size(0.35F, 0.55F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.6F)).color(Gradient.of(0, 0x9A9690, 1, 0xD0CCC8)).alpha(0.7F).module(Module.wind(1)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SINGULARITY_TRAIL).lod(32, 240)
				.emit(smoke("core").priority(Priority.HERO).burst(1).inherit(1).size(0.4F, 0.4F).life(2, 2).fixedRotation().sprite(FxSprites.GLOW)
						.sizeOverLife(Curve.ONE).alphaOverLife(Curve.ONE).color(0x000000).shade(1).build())
				.emit(EmitterDef.builder("rim").burst(1).inherit(1).size(0.7F, 0.7F).life(2, 2).sprite(FxSprites.RING).color(0xA060FF).build())
				.emit(EmitterDef.builder("wisps").burst(4).shape(Shape.shell(1.6F)).module(Module.attract(0.03F, 0.2F)).inherit(1).life(8, 14)
						.size(0.25F, 0.4F).sprite(FxSprites.WISP).color(ThrowVfx.GRAVITY).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("streaks").priority(Priority.DETAIL).burst(2).span().velocity(Velocity.random(0.01F, 0.03F)).life(10, 18)
						.size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(ThrowVfx.GRAVITY).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.MORTAR_TRAIL).lod(48, 320)
				.emit(EmitterDef.builder("shell").priority(Priority.HERO).burst(1).inherit(1).size(0.5F, 0.5F).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xFFB060).build())
				.emit(smoke("smoke").burst(3).span().velocity(Velocity.random(0.01F, 0.03F)).life(50, 90).size(0.4F, 0.7F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 3)).color(Gradient.of(0, 0x4A4440, 1, 0x8A8480)).alpha(0.7F)
						.emissive(Curve.keys(0, 0.7F, 0.1F, 0)).module(Module.buoyancy(0.003F)).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(2).span().velocity(Velocity.random(0.03F, 0.1F)).gravity(0.02F)
						.life(8, 16).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.METEOR_TRAIL).lod(80, 600)
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).inherit(1).size(1.4F, 1.4F).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xFFD080).build())
				.emit(EmitterDef.builder("flames").priority(Priority.HERO).burst(6).span().shape(Shape.sphere(0.6F)).velocity(Velocity.random(0.02F, 0.08F))
						.inherit(0.2F).life(8, 14).size(0.7F, 1.2F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("smoke").burst(4).span().velocity(Velocity.random(0.01F, 0.04F)).life(60, 110).size(0.8F, 1.3F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 3)).color(SOOT).alpha(0.85F).emissive(INNER_GLOW)
						.module(Module.buoyancy(0.004F)).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(3).span().velocity(Velocity.random(0.05F, 0.15F)).gravity(0.01F)
						.life(15, 30).size(0.04F, 0.06F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());

		// A tungsten rod at seven blocks a tick: a white-hot line with shock sparks peeling off it.
		FxEngine.register(EffectDef.builder(VfxIds.ROD_TRAIL).lod(96, 800)
				.emit(EmitterDef.builder("streak").priority(Priority.HERO).burst(1).inherit(1).size(0.35F, 0.35F).life(2, 2).mode(Mode.STRETCHED)
						.stretch(1.2F).sprite(FxSprites.GLOW).color(0xFFF4E0).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(10).span().size(0.5F, 0.6F).life(3, 5).sprite(FxSprites.GLOW).color(0xFFF8E8)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("heat").burst(8).span().size(1.2F, 1.6F).life(6, 12).sprite(FxSprites.GLOW).color(0xFFA040).alpha(0.4F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("shock").priority(Priority.DETAIL).burst(6).span().velocity(Velocity.random(0.1F, 0.3F)).drag(0.1F)
						.life(6, 12).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(smoke("vapour").burst(6).span().velocity(Velocity.random(0.01F, 0.03F)).life(40, 80).size(0.5F, 0.8F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(0xF0F0F8).alpha(0.55F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.BOMB_WHISTLE).lod(48, 320)
				.emit(smoke("smoke").burst(3).span().velocity(Velocity.random(0.005F, 0.02F)).life(30, 50).size(0.3F, 0.45F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.2F)).color(Gradient.of(0, 0xB0ACA8, 1, 0xE0DCD8)).alpha(0.65F).module(Module.wind(1)).build())
				.build());
	}

	// ------------------------------------------------------------------ what lands

	private static void strikes() {
		// The designator's lock: rings closing in on the spot, a pillar of light and sparks climbing it.
		FxEngine.register(EffectDef.builder(VfxIds.TARGET_LOCK).tint(0xFF3322).lod(64, 600).maxAge(100)
				.emit(EmitterDef.builder("rings").rate(0.25F, 60).onGround().mode(Mode.FLAT).fixedRotation().size(5, 5).life(18, 18)
						.sprite(FxSprites.RING).tinted().sizeOverLife(Curve.keys(0, 1, 1, 0.1F)).color(WHITE_FADE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("dot").burst(1).onGround().mode(Mode.FLAT).size(0.8F, 0.8F).life(60, 60).sprite(FxSprites.GLOW).tinted()
						.color(WHITE_FADE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("pillar").rate(1, 60).onGround().velocity(Velocity.up(0.6F, 0.8F, 0)).life(30, 40).size(0.4F, 0.5F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.GLOW).tinted().color(WHITE_FADE).alpha(0.5F).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).rate(2, 60).onGround().shape(Shape.disc(1.5F))
						.velocity(Velocity.up(0.15F, 0.3F, 0.2F)).drag(0.02F).life(15, 30).size(0.04F, 0.06F).sprite(FxSprites.FLARE).tinted()
						.color(WHITE_FADE).alphaOverLife(VfxLibrary.TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ORBITAL_IMPACT).lod(120, 1000)
				.emit(EmitterDef.builder("column").priority(Priority.HERO).burst(70).shape(Shape.column(2.2F, 60)).life(10, 20).size(2.4F, 3.6F)
						.sprite(FxSprites.GLOW).color(Gradient.of(0, 0xFFFFFF, 0.4F, 0xFF9070, 1, 0xFF3020)).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(flash("flash", 8, 6, 0xFF6040).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(14, 14).life(16, 16).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFF8040).build())
				.emit(EmitterDef.builder("debris").burst(40).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.5F, 1.1F, 0.6F))
						.gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 60).size(0.25F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(0x4A3A2A).emissive(Curve.keys(0, 0.9F, 0.3F, 0)).spin(0.1F, 0.3F).build())
				.emit(4, smoke("column_smoke").rate(5, 60).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.3F, 0.5F, 0.1F)).drag(0.03F)
						.life(90, 150).size(2.5F, 4).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(SOOT).alpha(0.8F).emissive(INNER_GLOW)
						.module(Module.wind(1.5F)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.METEOR_IMPACT).lod(80, 600)
				.emit(flash("flash", 3, 4, 0xFFC060).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(28).shape(Shape.dome(2)).velocity(Velocity.radial(0.15F, 0.35F))
						.module(Module.buoyancy(0.015F)).drag(0.1F).life(15, 28).size(1.2F, 2).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("rocks").burst(30).velocity(Velocity.up(0.4F, 0.9F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 1)
						.life(30, 60).size(0.15F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A3A2E).spin(0.1F, 0.3F)
						.emissive(Curve.keys(0, 0.8F, 0.3F, 0)).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(10, 10).life(14, 14).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFA060).build())
				.emit(smoke("dust").burst(14).onGround().shape(Shape.ring(1.5F, 1)).velocity(Velocity.radial(0.25F, 0.5F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.4F)).drag(0.05F).life(40, 80).size(1.2F, 2).sprite(FxSprites.DUST, 2).color(DUST)
						.alpha(0.8F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ROD_IMPACT).lod(96, 800)
				.emit(flash("flash", 7, 5, 0xFFF4E0).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(12, 12).life(12, 12).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFF0D0).build())
				.emit(smoke("plume").priority(Priority.HERO).burst(20).shape(Shape.column(1.2F, 4)).velocity(Velocity.up(0.4F, 0.9F, 0.15F))
						.drag(0.05F).life(60, 110).size(1, 1.8F).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.6F)).color(DUST).alpha(0.85F).build())
				.emit(EmitterDef.builder("debris").burst(30).velocity(Velocity.up(0.5F, 1.1F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 1)
						.life(30, 60).size(0.12F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A4A3A).spin(0.1F, 0.3F).build())
				.emit(sparkShower("sparks", 50, 0.4F, 1.0F, SPARK, null))
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.STORM_CLOUDS).lod(150, 1000).maxAge(160)
				.emit(smoke("clouds").priority(Priority.HERO).rate(6, 70, Curve.keys(0, 1, 0.8F, 1, 1, 0)).offset(0, 30, 0).shape(Shape.disc(26))
						.velocity(Velocity.swirl(0.05F, 0.1F)).module(Module.vortex(0.004F, 0)).life(70, 100).size(8, 13)
						.sizeOverLife(Curve.keys(0, 0.5F, 0.3F, 1, 1, 1.3F)).color(STORM).alpha(0.85F).shade(0.6F).build())
				.emit(EmitterDef.builder("flicker").rate(1.2F, 70).offset(0, 28, 0).shape(Shape.disc(22)).life(3, 6).size(10, 16).sprite(FxSprites.GLOW)
						.color(0xB0C8FF).alpha(0.5F).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.STORM_STRIKE).lod(64, 500)
				.emit(EmitterDef.builder("flash").burst(1).size(6, 6).life(5, 5).sprite(FxSprites.GLOW).color(0xD0E0FF).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(10, 10).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xA0C0FF).build())
				.emit(sparkShower("sparks", 40, 0.25F, 0.6F, Gradient.of(0, 0xFFFFFF, 0.4F, 0xA0C8FF, 1, argb(0x2040A0, 0)), null))
				.emit(smoke("scorch").burst(6).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(40, 70).size(0.8F, 1.3F).color(SOOT).alpha(0.6F)
						.module(Module.buoyancy(0.006F)).build())
				.build());
	}
}
