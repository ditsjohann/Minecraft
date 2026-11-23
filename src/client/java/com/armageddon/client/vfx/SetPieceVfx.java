package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;

/**
 * The big set pieces rebuilt as living particle systems, driven by their client effects every tick: the twister's
 * funnel and wall cloud, the tesla's ball lightning, the Final Existence black hole, Surtr's burning sword, the UFO's
 * tractor beam, the termite swarm and the world tree's canopy. The solid parts that give them mass stay solid.
 */
final class SetPieceVfx {
	private SetPieceVfx() {
	}

	static void register() {
		twister();
		ballLightning();
		sword();
		canopy();
	}

	// ------------------------------------------------------------------ Deadly Twister

	/** The funnel's height and its radius at the ground and at the top (scale 1 = the event's 24-block twister). */
	static final float TWISTER_H = 130, TWISTER_R0 = 5, TWISTER_R1 = 24;

	private static void twister() {
		// Played at the foot every tick, riding the twister's motion: smoke and debris drawn in at the foot climb the
		// whole funnel round its wall, growing as they rise; wind streaks whip round it at every height.
		Module wall = Module.funnel(TWISTER_H, TWISTER_R0, TWISTER_R1, 1.4F, 1.6F, 0.08F);
		FxEngine.register(EffectDef.builder(VfxIds.TWISTER_FUNNEL).lod(300, 2400)
				.emit(VfxLibrary.smoke("wall").priority(Priority.HERO).burst(16).shape(Shape.column(3, 12))
						.velocity(Velocity.up(1.4F, 2.2F, 0.05F)).inherit(0.85F).module(wall).life(60, 80).size(3, 5)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.6F)).color(Gradient.of(0, 0x6A6460, 0.5F, 0x868C94, 1, 0xB4BCC4)).alpha(0.5F)
						.shade(0.6F).build())
				.emit(EmitterDef.builder("streaks").burst(8).shape(Shape.column(3, TWISTER_H)).velocity(Velocity.up(0.1F, 0.3F, 0.1F)).inherit(0.85F)
						.module(wall).life(8, 16).size(0.14F, 0.22F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xE8ECF0, 1, argb(0xA8B0B8, 0))).alpha(0.6F).build())
				.emit(EmitterDef.builder("debris").burst(3).shape(Shape.column(3, 20)).velocity(Velocity.up(0.8F, 1.6F, 0.1F))
						.inherit(0.85F).module(wall).life(60, 100).size(0.35F, 0.8F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(Gradient.of(0, 0x5A4A38, 0.5F, 0x6A6A6A, 1, 0x4A6A34)).spin(0.1F, 0.4F).build())
				.emit(VfxLibrary.smoke("foot").burst(4).shape(Shape.ring(7, 5)).velocity(Velocity.up(0.1F, 0.3F, 0.2F)).inherit(0.85F).module(wall)
						.life(30, 50).size(4, 6).color(Gradient.of(0, 0x7A6A54, 1, 0x8A7C68)).alpha(0.55F).build())
				.build());

		// The wall cloud over it: a dark rotating shelf of billows.
		FxEngine.register(EffectDef.builder(VfxIds.WALL_CLOUD).lod(400, 3000)
				.emit(VfxLibrary.smoke("shelf").priority(Priority.HERO).burst(5).shape(Shape.ring(44, 30)).velocity(Velocity.swirl(0.2F, 0.4F))
						.module(Module.vortex(0.01F, 0.006F)).inherit(0.85F).life(80, 120).size(14, 20).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.2F))
						.color(Gradient.of(0, 0x3A3E46, 1, 0x5A6068)).alpha(0.6F).shade(0.5F).build())
				.build());
	}

	// ------------------------------------------------------------------ Tesla Dominion

	static final Gradient VOLT = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC8E4FF, 0.7F, 0x6A9CFF, 1, argb(0x2040A0, 0));

	private static void ballLightning() {
		// The ball lightning, played on it every tick (scale 1 = a ball six blocks across): a white-hot core, roiling
		// plasma, wisps crawling over its surface, arcs crackling out of it and sparks dropping off.
		FxEngine.register(EffectDef.builder(VfxIds.BALL_LIGHTNING).lod(300, 2400)
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(2).shape(Shape.sphere(1.5F)).life(3, 5).size(3.5F, 4.5F).sprite(FxSprites.GLOW)
						.color(0xF0F8FF).alpha(0.6F).alphaOverLife(VfxLibrary.FLICKER).build())
				.emit(EmitterDef.builder("plasma").priority(Priority.HERO).burst(6).shape(Shape.sphere(5)).velocity(Velocity.random(0.02F, 0.08F))
						.life(4, 8).size(2.5F, 4).sprite(FxSprites.GLOW).color(VOLT).alpha(0.28F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("wisps").burst(4).shape(Shape.shell(6)).module(Module.curl(4, 0.06F)).life(8, 14).size(1.2F, 1.8F)
						.sprite(FxSprites.WISP).color(Gradient.of(0, 0xE0F0FF, 1, 0x8AB8FF)).alpha(0.7F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("arcs").burst(6).shape(Shape.shell(5.5F)).velocity(Velocity.radial(0.6F, 1.4F)).drag(0.3F).life(3, 6)
						.size(0.15F, 0.25F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(VOLT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(2).shape(Shape.shell(5)).gravity(0.03F).life(15, 30).size(0.12F, 0.18F)
						.sprite(FxSprites.FLARE).color(VOLT).alphaOverLife(VfxLibrary.TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Ragnarok: Surtr's sword

	private static void sword() {
		// Played along the blade every tick (spread from the point up to the guard): fire tongues licking up off it,
		// embers and heat smoke. The fire is left where it was shed, so while the sword falls it streams a wake.
		FxEngine.register(EffectDef.builder(VfxIds.SWORD_FLAME).lod(400, 3000)
				.emit(EmitterDef.builder("tongues").priority(Priority.HERO).burst(20).span().shape(Shape.sphere(9)).velocity(Velocity.up(0.3F, 0.7F, 0.3F))
						.module(Module.buoyancy(0.02F)).module(Module.curl(10, 0.02F)).life(10, 20).size(7, 12).sprite(FxSprites.FIRE, 2)
						.color(VfxLibrary.FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(8).span().shape(Shape.sphere(8)).velocity(Velocity.up(0.3F, 0.8F, 0.5F))
						.module(Module.jitter(0.006F)).life(30, 60).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(VfxLibrary.EMBER)
						.alphaOverLife(VfxLibrary.TWINKLE).build())
				.emit(VfxLibrary.smoke("heat").burst(2).span().shape(Shape.sphere(6)).velocity(Velocity.up(0.3F, 0.5F, 0.2F)).life(60, 100).size(6, 10)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(VfxLibrary.SOOT).alpha(0.5F).emissive(VfxLibrary.INNER_GLOW)
						.module(Module.wind(1.5F)).build())
				.build());
	}

	// ------------------------------------------------------------------ Eternal Sapling: the world tree's canopy

	private static void canopy() {
		// Played in the crown every few ticks once it has grown (scale = the tree's): fireflies wandering and blinking,
		// golden pollen hanging in the air, and petals and leaves drifting down out of the canopy.
		Gradient firefly = Gradient.of(0, 0xF0FFA0, 0.5F, 0xC8FF70, 1, argb(0x80C030, 0));
		Gradient gold = Gradient.of(0, 0xFFF0B0, 1, argb(0xD0A030, 0));
		FxEngine.register(EffectDef.builder(VfxIds.CANOPY).lod(400, 3000)
				.emit(EmitterDef.builder("fireflies").priority(Priority.HERO).burst(9).shape(Shape.sphere(40)).velocity(Velocity.random(0.02F, 0.06F))
						.module(Module.curl(12, 0.01F)).module(Module.jitter(0.004F)).life(80, 140).size(0.55F, 0.85F).sprite(FxSprites.FLARE).color(firefly)
						.alphaOverLife(VfxLibrary.TWINKLE).build())
				.emit(EmitterDef.builder("pollen").burst(4).shape(Shape.sphere(35)).velocity(Velocity.up(0.01F, 0.04F, 0.6F)).module(Module.jitter(0.002F))
						.life(100, 160).size(0.3F, 0.45F).sprite(FxSprites.FLARE).color(gold).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("petals").burst(4).offset(0, -5, 0).shape(Shape.disc(45)).gravity(0.003F).module(Module.jitter(0.008F))
						.module(Module.wind(1)).life(160, 240).size(0.3F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(Gradient.of(0, 0xFFD0E4, 0.5F, 0xF4A8C8, 1, 0xFFE8F0)).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("leaves").burst(2).offset(0, -5, 0).shape(Shape.disc(45)).gravity(0.004F).module(Module.jitter(0.008F))
						.module(Module.wind(1)).life(140, 220).size(0.3F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(Gradient.of(0, 0x50A040, 1, 0x70C050)).spin(0.05F, 0.2F).build())
				.build());
	}
}
