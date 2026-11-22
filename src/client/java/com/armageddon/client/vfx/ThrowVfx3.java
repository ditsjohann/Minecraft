package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The fun throwables (bounce bomb, loot tornado, moonwalk mine, hologram, magnet mayhem, rainbow paint) and four
 * weapons: the antimatter lance, plasma blade, apocalypse gatling and the WUB-9000.
 */
final class ThrowVfx3 {
	private ThrowVfx3() {
	}

	static final int[] RAINBOW = ThrowVfx2.RAINBOW;
	static final Gradient VIOLET = fade(0xE070FF, 0x401060);
	static final Gradient PINK = fade(0xFF4FD8, 0x60104A);
	static final Gradient GOLD = fade(0xFFD040, 0x806010);

	static void register() {
		fun();
		weapons();
	}

	// ------------------------------------------------------------------ the fun throwables

	private static void fun() {
		FxEngine.register(EffectDef.builder(VfxIds.BOUNCE_TRAIL).lod(16, 96)
				.emit(EmitterDef.builder("sparkle").burst(1).velocity(Velocity.random(0.01F, 0.04F)).life(8, 16).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xFF90E0).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.3F, 0.3F).life(2, 2).sprite(FxSprites.GLOW).color(0xFF60C0)
						.alpha(0.6F).build())
				.build());
		EffectDef.Builder confetti = EffectDef.builder(VfxIds.BOUNCE_BURST).lod(32, 200)
				.emit(EmitterDef.builder("ring").burst(1).size(3, 3).life(8, 8).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(0xFF90E0).alphaOverLife(Curve.FADE_OUT).build());
		for (int k = 0; k < RAINBOW.length; k++) {
			confetti.emit(EmitterDef.builder("confetti" + k).burst(9).velocity(Velocity.random(0.15F, 0.4F)).velocity(Velocity.up(0.1F, 0.3F, 0.9F))
					.gravity(0.012F).drag(0.06F).module(Module.jitter(0.01F)).collide(0.1F, 0.4F, 2).life(40, 80).size(0.1F, 0.16F).spin(0.2F, 0.5F)
					.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(RAINBOW[k]).emissive(Curve.constant(0.4F)).build());
		}
		FxEngine.register(confetti.build());

		FxEngine.register(EffectDef.builder(VfxIds.LOOT_GLINT).lod(16, 96)
				.emit(EmitterDef.builder("coins").burst(1).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.004F).life(10, 20).size(0.07F, 0.11F)
						.sprite(FxSprites.FLARE).color(0xFFE070).alphaOverLife(TWINKLE).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.LOOT_BURST).lod(40, 260).maxAge(200)
				.emit(flash("flash", 2, 4, 0xFFE070).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).rate(6, 80).onGround().shape(Shape.ring(1.5F, 1))
						.velocity(Velocity.swirl(0.2F, 0.3F)).velocity(Velocity.up(0.12F, 0.2F, 0.2F)).module(Module.vortex(0.03F, 0.015F))
						.drag(0.02F).life(30, 50).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(0xFFE070).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("streaks").rate(3, 80).onGround().shape(Shape.ring(1.5F, 1)).velocity(Velocity.swirl(0.2F, 0.3F))
						.velocity(Velocity.up(0.12F, 0.2F, 0.2F)).module(Module.vortex(0.03F, 0.015F)).drag(0.02F).life(20, 35)
						.size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(GOLD).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.MOON_DUST).lod(16, 96)
				.emit(EmitterDef.builder("dust").burst(1).velocity(Velocity.random(0.005F, 0.02F)).gravity(-0.003F).life(20, 40).size(0.05F, 0.08F)
						.sprite(FxSprites.GLOW).color(0xD8D0FF).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.MOON_BURST).lod(32, 240).maxAge(300)
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(16, 16).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xD8D0FF).build())
				.emit(smoke("dust").burst(16).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.02F, 0.06F, 0.5F)).gravity(-0.002F).drag(0.03F)
						.life(100, 180).size(0.6F, 1).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.2F)).color(Gradient.of(0, 0xC8C4D8, 1, 0xA8A4B8))
						.alpha(0.5F).emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("stars").priority(Priority.DETAIL).rate(1, 160).shape(Shape.sphere(5)).gravity(-0.002F).life(30, 60)
						.size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0xF0F0FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.HOLO_FLICKER).lod(16, 96)
				.emit(EmitterDef.builder("pixels").burst(1).shape(Shape.sphere(0.4F)).module(Module.jitter(0.05F)).drag(0.6F).life(3, 7)
						.size(0.05F, 0.09F).fixedRotation().sprite(FxSprites.GLOW).color(0x40FFFF).alphaOverLife(FLICKER).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.HOLO_BURST).lod(32, 240)
				.emit(EmitterDef.builder("scan").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(7, 7).life(20, 20).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x40FFFF).build())
				.emit(EmitterDef.builder("pixels").burst(60).shape(Shape.sphere(3)).module(Module.jitter(0.04F)).drag(0.5F).life(15, 40)
						.size(0.08F, 0.14F).fixedRotation().sprite(FxSprites.GLOW).color(0x40FFFF).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(20).shape(Shape.sphere(3)).life(10, 25).size(0.05F, 0.08F)
						.sprite(FxSprites.FLARE).color(0xC0FFFF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.MAGNET_SPARKS).lod(16, 96)
				.emit(pole("north", 0xFF4040, 1))
				.emit(pole("south", 0x4080FF, 1))
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.MAGNET_PULSE).lod(32, 240)
				.emit(EmitterDef.builder("red").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(14, 14).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFF4040).build())
				.emit(4, EmitterDef.builder("blue").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(14, 14).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x4080FF).build())
				.emit(EmitterDef.builder("fieldN").rate(4, 40).shape(Shape.shell(4)).module(Module.vortex(0.02F, 0.025F)).module(Module.attract(0.015F, 0.4F))
						.life(20, 30).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(0xFF6060).build())
				.emit(EmitterDef.builder("fieldS").rate(4, 40).shape(Shape.shell(4)).module(Module.vortex(-0.02F, 0.025F)).module(Module.attract(0.015F, 0.4F))
						.life(20, 30).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(0x60A0FF).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(20).shape(Shape.sphere(3)).life(15, 30).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xE0E8F0).alphaOverLife(TWINKLE).build())
				.build());

		EffectDef.Builder drip = EffectDef.builder(VfxIds.PAINT_DRIP).lod(16, 96);
		EffectDef.Builder splash = EffectDef.builder(VfxIds.PAINT_SPLASH).lod(32, 240);
		for (int k = 0; k < RAINBOW.length; k++) {
			drip.emit(EmitterDef.builder("drip" + k).burst(0.2F).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.03F).collide(0, 0.3F, 0)
					.life(15, 30).size(0.06F, 0.09F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(RAINBOW[k]).emissive(Curve.constant(0.4F)).build());
			EmitterDef splat = EmitterDef.builder("splat" + k).burst(1).onGround().mode(Mode.FLAT).life(80, 120).size(0.5F, 0.9F)
					.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(RAINBOW[k]).emissive(Curve.constant(0.4F))
					.alphaOverLife(Curve.keys(0, 1, 0.8F, 1, 1, 0)).build();
			splash.emit(EmitterDef.builder("blob" + k).burst(6).velocity(Velocity.random(0.15F, 0.4F)).velocity(Velocity.up(0.15F, 0.3F, 0.9F))
					.gravity(0.05F).collide(0, 0.3F, 0).onHit(1, splat).life(30, 50).size(0.16F, 0.26F).layer(Layer.SMOKE).sprite(FxSprites.GLOW)
					.color(RAINBOW[k]).emissive(Curve.constant(0.4F)).build());
		}
		FxEngine.register(drip.build());
		FxEngine.register(splash.emit(EmitterDef.builder("flash").burst(1).size(2, 2).life(4, 4).sprite(FxSprites.GLOW).color(0xFFFFFF)
				.alphaOverLife(Curve.FADE_OUT).build()).build());
	}

	/** Sparks of one magnetic pole, drawn in towards the bomb. */
	private static EmitterDef pole(String name, int rgb, float count) {
		return EmitterDef.builder(name).burst(count).shape(Shape.shell(0.8F)).module(Module.attract(0.02F, 0.1F)).inherit(1).drag(0.1F)
				.life(6, 10).size(0.015F, 0.025F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(rgb).build();
	}

	// ------------------------------------------------------------------ weapons

	private static void weapons() {
		FxEngine.register(EffectDef.builder(VfxIds.LANCE_CHARGE).lod(16, 96)
				.emit(EmitterDef.builder("inflow").burst(5).shape(Shape.shell(1.4F)).module(Module.attract(0.03F, 0.08F)).drag(0.1F).life(8, 12)
						.size(0.02F, 0.03F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(VIOLET).alphaOverLife(Curve.BELL).build())
				.emit(smoke("void").burst(1).shape(Shape.sphere(0.6F)).module(Module.attract(0.02F, 0.1F)).life(8, 12).size(0.15F, 0.25F)
						.sprite(FxSprites.WISP).color(0x100018).alpha(0.6F).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.LANCE_MUZZLE).lod(24, 160)
				.emit(flash("flash", 1.4F, 4, 0xE070FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("ring").burst(1).size(1.6F, 1.6F).life(8, 8).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(VIOLET).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("cone").priority(Priority.DETAIL).burst(16).velocity(Velocity.along(0.4F, 1.0F, 0.3F)).drag(0.1F)
						.life(5, 10).size(0.025F, 0.04F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(VIOLET).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.LANCE_TRAIL).lod(64, 400)
				.emit(EmitterDef.builder("motes").priority(Priority.HERO).burst(110).span().velocity(Velocity.random(0.005F, 0.02F)).life(15, 35)
						.size(0.15F, 0.24F).sprite(FxSprites.FLARE).color(0xF0B0FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("haze").burst(40).span().velocity(Velocity.random(0.002F, 0.01F)).life(15, 25).size(0.5F, 0.8F)
						.sprite(FxSprites.GLOW).color(0xB050E0).alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("wisps").burst(16).span().velocity(Velocity.random(0.01F, 0.03F)).module(Module.curl(2, 0.005F)).life(20, 40)
						.size(0.3F, 0.5F).sprite(FxSprites.WISP).color(0x14061C).alpha(0.3F).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.ANTIMATTER_FLASH).lod(48, 300)
				.emit(flash("flash", 4, 5, 0xF0C8FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("inrush").burst(50).shape(Shape.shell(4)).module(Module.attract(0.08F, 0.3F)).life(8, 12)
						.size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(VIOLET).build())
				.emit(6, sparkShower("outrush", 70, 0.4F, 1.1F, VIOLET, null))
				.emit(6, EmitterDef.builder("ring").burst(1).size(6, 6).life(12, 12).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0xE0A0FF).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.BLADE_HIT).lod(16, 128)
				.emit(EmitterDef.builder("slash").burst(1).velocity(Velocity.along(0.05F, 0.05F, 0)).size(0.12F, 0.12F).life(4, 4)
						.mode(Mode.STRETCHED).stretch(30).sprite(FxSprites.STREAK).color(PINK).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.9F, 0.9F).life(4, 4).sprite(FxSprites.GLOW).color(0xFF4FD8).alphaOverLife(Curve.FADE_OUT).build())
				.emit(sparkShower("sparks", 14, 0.15F, 0.45F, PINK, null))
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.BLADE_DASH).lod(24, 160)
				.emit(EmitterDef.builder("ghosts").burst(8).span().size(0.9F, 1.1F).life(6, 10).sprite(FxSprites.GLOW).color(0xFF4FD8).alpha(0.4F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(30).span().velocity(Velocity.random(0.05F, 0.2F)).drag(0.08F)
						.life(8, 18).size(0.025F, 0.04F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(PINK).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(20).span().life(10, 20).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xFFA0F0).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GATLING_MUZZLE).lod(24, 160)
				.emit(flash("flash", 1.0F, 3, 0xFFE080).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("fire").burst(4).velocity(Velocity.along(0.2F, 0.4F, 0.3F)).drag(0.2F).life(3, 5).size(0.3F, 0.5F)
						.sprite(FxSprites.FIRE, 2).color(Gradient.of(0, 0xFFFFF0, 1, 0xFF9030)).build())
				.emit(smoke("smoke").burst(3).velocity(Velocity.along(0.03F, 0.08F, 0.5F)).life(30, 50).size(0.35F, 0.6F)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.4F)).color(Gradient.of(0, 0x7A7470, 1, 0xA8A4A0)).alpha(0.6F)
						.emissive(Curve.keys(0, 0.7F, 0.15F, 0)).module(Module.wind(1)).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.NUKE_ROUND_TRAIL).lod(48, 320)
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).inherit(1).size(0.4F, 0.4F).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xD0FF60).build())
				.emit(EmitterDef.builder("exhaust").burst(2).span().velocity(Velocity.along(0.03F, 0.08F, 0.3F)).life(3, 5).size(0.2F, 0.3F)
						.sprite(FxSprites.FIRE, 2).color(Gradient.of(0, 0xFFFFE0, 1, 0xFF9030)).build())
				.emit(smoke("smoke").burst(2).span().velocity(Velocity.random(0.01F, 0.03F)).life(30, 60).size(0.3F, 0.5F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.6F)).color(Gradient.of(0, 0x8A8680, 1, 0xB8B4B0)).alpha(0.55F)
						.module(Module.wind(1)).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).life(15, 30)
						.size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0xC8FF40).alphaOverLife(TWINKLE).build())
				.build());

		// The WUB-9000: a ring at the muzzle on every note and a bass hit where it lands, in the beat's colour.
		FxEngine.register(EffectDef.builder(VfxIds.WUB_RING).tint(0xFF50C0).lod(24, 160)
				.emit(EmitterDef.builder("ring").burst(1).size(0.8F, 0.8F).life(6, 6).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.6F, 0.6F).life(3, 3).sprite(FxSprites.GLOW).tinted().color(WHITE_FADE).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.WUB_HIT).tint(0xFF50C0).lod(32, 200)
				.emit(EmitterDef.builder("ring").burst(1).size(1.8F, 1.8F).life(8, 8).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(10).velocity(Velocity.random(0.15F, 0.4F))
						.velocity(Velocity.along(0.1F, 0.3F, 0.8F)).drag(0.08F).life(6, 14).size(0.025F, 0.04F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).tinted().color(WHITE_FADE).build())
				.build());
		EffectDef.Builder drop = EffectDef.builder(VfxIds.BASS_DROP).lod(64, 400)
				.emit(flash("flash", 4, 5, 0xFFFFFF).layer(Layer.GLOW_TOP).build())
				.emit(smoke("smoke").burst(14).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.08F, 0.2F)).life(40, 70).size(1, 1.6F)
						.color(SOOT).alpha(0.6F).build());
		for (int k = 0; k < RAINBOW.length; k++) {
			drop.emit(k * 2, EmitterDef.builder("wave" + k).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(12, 12).life(16, 16)
					.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(RAINBOW[k]).build());
		}
		FxEngine.register(drop.emit(sparkShower("sparks", 80, 0.4F, 1.1F, SPARK, null)).build());
	}
}
