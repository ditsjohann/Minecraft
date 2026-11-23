package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.EMBER;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.INNER_GLOW;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * Support and mid-tier weapons: the warden wand's seal, the party popper, the airhorn, the microstrike beacon, the
 * gravity tether, the siegebreaker's missiles, the resonance cannon, the sunlance, the worldsplitter and the midas
 * cascade.
 */
final class GearVfx {
	private GearVfx() {
	}

	static final Gradient WARD = fade(0x60FFB0, 0x107050);
	static final Gradient HONK = fade(0xFFF060, 0xC08010);
	static final Gradient MICRO = fade(0x9AD8FF, 0x2050C0);
	static final Gradient TETHER = fade(0x80C0FF, 0x2040A0);
	static final Gradient SIEGE = fade(0xD8A0FF, 0x5020A0);
	static final Gradient RESO = fade(0x90F0FF, 0x2080B0);
	static final Gradient AMETHYST = fade(0xE0B0FF, 0x7030B0);
	static final Gradient SUN = Gradient.of(0, 0xFFFFF0, 0.3F, 0xFFE070, 0.7F, 0xFFA020, 1, argb(0x803000, 0));
	static final Gradient GOLD = Gradient.of(0, 0xFFFFE0, 0.3F, 0xFFE060, 0.7F, 0xE0A020, 1, argb(0x805010, 0));
	static final Gradient GOLD_DUST = Gradient.of(0, 0xB09060, 1, 0xC8B488);

	static void register() {
		support();
		heavy();
	}

	// ------------------------------------------------------------------ rare gear

	private static void support() {
		// Warden Wand: a green seal settles on the clicked block and wardlights drift up out of it.
		FxEngine.register(EffectDef.builder(VfxIds.WARDEN_SEAL).lod(48, 300)
				.emit(EmitterDef.builder("seal").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(3, 3).life(40, 40).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 1.6F, 0.25F, 1, 1, 1)).color(WARD).alphaOverLife(Curve.keys(0, 1, 0.7F, 1, 1, 0)).build())
				.emit(EmitterDef.builder("inner").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(1.6F, 1.6F).life(40, 40)
						.sprite(FxSprites.RING).color(WARD).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("motes").rate(2, 30).onGround().shape(Shape.ring(1.4F, 0.2F)).velocity(Velocity.up(0.04F, 0.1F, 0))
						.life(30, 50).size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(WARD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).onGround().size(2.4F, 2.4F).life(14, 14).sprite(FxSprites.GLOW).color(0x60FFB0).alpha(0.5F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Party Popper: a rocket that sheds rainbow sparkles.
		EffectDef.Builder party = EffectDef.builder(VfxIds.PARTY_TRAIL).lod(32, 200)
				.emit(EmitterDef.builder("core").burst(1).inherit(1).size(0.6F, 0.6F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFF0C0).build())
				.emit(smoke("smoke").burst(1.5F).span().velocity(Velocity.random(0.01F, 0.03F)).life(20, 35).size(0.25F, 0.4F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(0xE8E4F0).alpha(0.5F).build());
		for (int k = 0; k < ThrowVfx2.RAINBOW.length; k++) {
			party.emit(EmitterDef.builder("sparkle" + k).priority(k % 2 == 0 ? Priority.NORMAL : Priority.DETAIL).burst(1.2F).span()
					.velocity(Velocity.random(0.03F, 0.08F)).gravity(0.004F).life(18, 32).size(0.12F, 0.18F).sprite(FxSprites.FLARE)
					.color(ThrowVfx2.RAINBOW[k]).alphaOverLife(TWINKLE).build());
		}
		FxEngine.register(party.build());

		// Airhorn: a cone of fat yellow rings and air streaks, with a puff of comic dust at the bell.
		FxEngine.register(EffectDef.builder(VfxIds.HONK_BLAST).lod(32, 200)
				.emit(EmitterDef.builder("rings").burst(0, 1).burst(3, 1).burst(6, 1).velocity(Velocity.along(0.5F, 0.5F, 0)).drag(0.08F).size(1.2F, 1.2F)
						.life(14, 14).sprite(FxSprites.RING).sizeOverLife(Curve.keys(0, 0.4F, 1, 3.2F)).color(HONK).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("streaks").burst(30).velocity(Velocity.along(0.6F, 1.0F, 0.35F)).drag(0.1F).life(8, 14).size(0.08F, 0.12F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(WHITE_FADE).alpha(0.7F).build())
				.emit(smoke("puff").burst(12).velocity(Velocity.along(0.15F, 0.4F, 0.5F)).drag(0.1F).life(18, 30).size(0.5F, 0.8F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(0xF0ECE0).alpha(0.6F).build())
				.emit(EmitterDef.builder("notes").priority(Priority.DETAIL).burst(5).velocity(Velocity.along(0.2F, 0.35F, 0.6F)).drag(0.05F).gravity(-0.004F)
						.life(20, 30).size(0.15F, 0.2F).sprite(FxSprites.FLARE).color(HONK).alphaOverLife(TWINKLE).build())
				.build());

		// Microstrike: a small railgun hit from the sky.
		FxEngine.register(EffectDef.builder(VfxIds.MICRO_IMPACT).lod(48, 320)
				.emit(flash("flash", 3, 3, 0xA0D8FF).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(5, 5).life(10, 10).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x80C8FF).build())
				.emit(sparkShower("sparks", 30, 0.25F, 0.6F, MICRO, null))
				.emit(EmitterDef.builder("ions").burst(10).shape(Shape.sphere(1)).velocity(Velocity.up(0.05F, 0.15F, 0.5F)).life(15, 30)
						.size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(MICRO).alpha(0.6F).build())
				.emit(smoke("smoke").burst(5).velocity(Velocity.up(0.04F, 0.1F, 0.5F)).life(30, 55).size(0.6F, 1).color(SOOT).alpha(0.6F)
						.module(Module.buoyancy(0.005F)).build())
				.build());

		// Gravity Tether: blue sparks running down the line toward the hand, a halo where it bites.
		FxEngine.register(EffectDef.builder(VfxIds.TETHER_PULL).lod(32, 200)
				.emit(EmitterDef.builder("bite").burst(1).size(0.9F, 0.9F).life(3, 3).sprite(FxSprites.RING).color(TETHER).alpha(0.8F).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.6F, 0.6F).life(3, 3).sprite(FxSprites.GLOW).color(0x80C0FF).build())
				.emit(EmitterDef.builder("pull").burst(3).span().velocity(Velocity.along(0.2F, 0.4F, 0.05F)).life(4, 8).size(0.03F, 0.05F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(TETHER).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).burst(2).span().velocity(Velocity.random(0.01F, 0.04F)).life(8, 14)
						.size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(TETHER).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ epic and legendary gear

	private static void heavy() {
		// Siegebreaker missiles: violet exhaust and a white smoke trail.
		FxEngine.register(EffectDef.builder(VfxIds.SIEGE_TRAIL).lod(48, 320)
				.emit(EmitterDef.builder("flame").priority(Priority.HERO).burst(1).inherit(1).size(0.6F, 0.6F).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xE0B0FF).build())
				.emit(EmitterDef.builder("exhaust").burst(3).span().velocity(Velocity.random(0.02F, 0.05F)).life(4, 7).size(0.3F, 0.45F)
						.sprite(FxSprites.FIRE, 2).color(SIEGE).alphaOverLife(Curve.FADE).build())
				.emit(smoke("smoke").burst(4).span().velocity(Velocity.random(0.01F, 0.03F)).life(40, 70).size(0.5F, 0.8F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.6F)).color(Gradient.of(0, 0xC0B8D0, 1, 0xE0DCE8)).alpha(0.6F).module(Module.wind(1)).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SIEGE_BLAST).lod(64, 400)
				.emit(flash("flash", 4, 4, 0xE0B0FF).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(16).shape(Shape.sphere(1)).velocity(Velocity.radial(0.15F, 0.3F))
						.drag(0.12F).module(Module.buoyancy(0.01F)).life(12, 22).size(1, 1.6F).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("plasma").burst(10).velocity(Velocity.radial(0.2F, 0.4F)).drag(0.1F).life(10, 16).size(0.8F, 1.2F)
						.sprite(FxSprites.GLOW).color(SIEGE).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(7, 7).life(12, 12).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xC090FF).build())
				.emit(sparkShower("sparks", 24, 0.3F, 0.7F, VfxLibrary.SPARK, null))
				.emit(smoke("smoke").burst(10).shape(Shape.sphere(1.5F)).velocity(Velocity.up(0.04F, 0.1F, 0.6F)).life(50, 90).size(1, 1.6F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.2F)).color(SOOT).alpha(0.75F).emissive(INNER_GLOW).module(Module.buoyancy(0.004F)).build())
				.build());

		// Resonance Cannon: a tunnel of cyan rings down the line with amethyst glints shaken loose.
		FxEngine.register(EffectDef.builder(VfxIds.RESONANCE_WAVE).lod(64, 400)
				.emit(EmitterDef.builder("rings").burst(10).span().size(1.2F, 1.2F).life(10, 18).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 0.3F, 1, 2.6F)).color(RESO).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("core").burst(30).span().size(1, 1.3F).life(6, 10).sprite(FxSprites.GLOW).color(RESO).alpha(0.55F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glints").burst(40).span().shape(Shape.sphere(2)).velocity(Velocity.random(0.05F, 0.15F)).gravity(0.01F)
						.life(20, 40).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(AMETHYST).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("streaks").priority(Priority.DETAIL).burst(16).span().velocity(Velocity.along(0.6F, 1.0F, 0.15F)).drag(0.1F)
						.life(6, 10).size(0.04F, 0.06F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RESO).build())
				.build());

		// Sunlance: sunlight pours into the muzzle; then a molten line and a sunburst where it lands.
		FxEngine.register(EffectDef.builder(VfxIds.SUNLANCE_CHARGE).lod(32, 200)
				.emit(EmitterDef.builder("motes").burst(6).shape(Shape.shell(3)).module(Module.attract(0.04F, 0.3F)).life(14, 22).size(0.06F, 0.1F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SUN).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("core").burst(1).size(0.6F, 0.6F).life(3, 3).sprite(FxSprites.GLOW).color(0xFFE070).build())
				.emit(EmitterDef.builder("flare").priority(Priority.DETAIL).burst(1).size(1.2F, 1.2F).life(4, 4).sprite(FxSprites.FLARE).color(0xFFF0B0)
						.alpha(0.5F).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SUNLANCE_LINE).lod(160, 1200).maxAge(80)
				.emit(EmitterDef.builder("heat").priority(Priority.HERO).burst(60).span().size(1.6F, 2.4F).life(8, 16).sprite(FxSprites.GLOW)
						.color(SUN).alpha(0.6F).build())
				.emit(EmitterDef.builder("flames").rate(8, 40).span().shape(Shape.sphere(1.2F)).velocity(Velocity.up(0.05F, 0.12F, 0.3F))
						.life(10, 20).size(0.6F, 1.1F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).rate(6, 60).span().shape(Shape.sphere(1.5F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.6F)).gravity(-0.002F).life(20, 40).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(EMBER)
						.alphaOverLife(TWINKLE).build())
				.emit(smoke("smoke").rate(2, 60).span().shape(Shape.sphere(1)).velocity(Velocity.up(0.04F, 0.1F, 0.2F)).life(50, 90).size(0.8F, 1.3F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(SOOT).alpha(0.6F).module(Module.wind(1)).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SUNLANCE_IMPACT).lod(96, 600)
				.emit(flash("flash", 6, 5, 0xFFE080).build())
				.emit(EmitterDef.builder("sunburst").burst(30).velocity(Velocity.radial(0.6F, 1.1F)).drag(0.12F).life(8, 14).size(0.1F, 0.16F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(SUN).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(20).shape(Shape.dome(2)).velocity(Velocity.radial(0.1F, 0.25F))
						.module(Module.buoyancy(0.015F)).drag(0.1F).life(15, 30).size(1.2F, 2).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(10, 10).life(14, 14).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFD060).build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(5, 5).life(80, 80).sprite(FxSprites.GLOW)
						.color(0xFF8020).alpha(0.6F).alphaOverLife(Curve.keys(0, 1, 0.6F, 0.7F, 1, 0)).build())
				.build());

		// Worldsplitter: a golden slam, cracks spitting dust and embers along all eight fissures, and the final blast.
		FxEngine.register(EffectDef.builder(VfxIds.SPLIT_SLAM).lod(96, 600)
				.emit(flash("flash", 5, 4, 0xFFD070).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(18, 18).life(16, 16).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFC040).build())
				.emit(smoke("dust").burst(40).onGround().shape(Shape.ring(2, 1)).velocity(Velocity.radial(0.5F, 0.9F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.drag(0.06F).life(50, 90).size(1.6F, 2.6F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.8F)).sprite(FxSprites.DUST, 2).color(GOLD_DUST).alpha(0.85F).build())
				.emit(EmitterDef.builder("debris").burst(24).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.5F, 1.0F, 0.5F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 1).life(30, 60).size(0.15F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A4A3A)
						.spin(0.1F, 0.3F).build())
				.emit(sparkShower("gold", 30, 0.3F, 0.8F, GOLD, null))
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SPLIT_FISSURE).lod(96, 600).maxAge(60)
				.emit(EmitterDef.builder("glow").burst(20).span().onGround().size(0.8F, 1.2F).life(20, 40).sprite(FxSprites.GLOW).color(0xFFB030)
						.alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(smoke("dust").rate(3, 30).span().onGround().velocity(Velocity.up(0.08F, 0.2F, 0.3F)).drag(0.03F).life(30, 60).size(0.8F, 1.3F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).sprite(FxSprites.DUST, 2).color(GOLD_DUST).alpha(0.7F).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).rate(3, 40).span().onGround().velocity(Velocity.up(0.1F, 0.25F, 0.3F))
						.gravity(0.006F).life(20, 40).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SPLIT_FINALE).lod(128, 900)
				.emit(flash("flash", 9, 6, 0xFFD070).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(30, 30).life(20, 20).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFFB000).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(30).shape(Shape.dome(3)).velocity(Velocity.radial(0.2F, 0.45F))
						.module(Module.buoyancy(0.015F)).drag(0.1F).life(18, 32).size(2, 3.2F).sprite(FxSprites.FIRE, 2).color(GOLD)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("dust").burst(40).onGround().shape(Shape.ring(4, 2)).velocity(Velocity.radial(0.6F, 1.0F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F))
						.drag(0.05F).life(60, 110).size(2, 3.2F).sprite(FxSprites.DUST, 2).color(GOLD_DUST).alpha(0.8F).build())
				.emit(sparkShower("gold", 60, 0.5F, 1.2F, GOLD, null))
				.build());

		// Midas Cascade: a gold orb shedding glitter; every sphere it splits into bursts in coins of light.
		FxEngine.register(EffectDef.builder(VfxIds.MIDAS_TRAIL).lod(48, 300)
				.emit(EmitterDef.builder("orb").priority(Priority.HERO).burst(1).inherit(1).size(1.2F, 1.2F).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xFFE070).build())
				.emit(EmitterDef.builder("glitter").burst(8).span().shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.01F, 0.05F)).gravity(0.006F)
						.life(20, 36).size(0.12F, 0.18F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("drips").priority(Priority.DETAIL).burst(1).span().gravity(0.03F).life(15, 25).size(0.04F, 0.06F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(GOLD).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.MIDAS_BURST).lod(64, 400)
				.emit(flash("flash", 4, 4, 0xFFE070).build())
				.emit(EmitterDef.builder("shell").burst(1).size(3, 3).life(10, 10).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).color(0xFFD040)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("coins").burst(40).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.2F, 0.8F)).gravity(0.03F)
						.drag(0.04F).collide(0.4F, 0.6F, 2).life(30, 60).size(0.1F, 0.16F).spin(0.2F, 0.5F).sprite(FxSprites.FLARE).color(GOLD)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("streaks").burst(20).velocity(Velocity.radial(0.4F, 0.8F)).drag(0.1F).life(8, 14).size(0.05F, 0.08F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(GOLD).build())
				.emit(EmitterDef.builder("haze").burst(8).shape(Shape.sphere(1.5F)).velocity(Velocity.up(0.02F, 0.05F, 0.6F)).life(30, 50)
						.size(1.2F, 1.8F).sprite(FxSprites.GLOW).color(0xFFC040).alpha(0.35F).alphaOverLife(Curve.BELL).build())
				.build());
	}
}
