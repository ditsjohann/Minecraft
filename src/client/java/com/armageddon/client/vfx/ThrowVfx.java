package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.EMBER;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.FROST;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The throwables: a whoosh as they leave the hand, what each leaks or crackles with while it flies and sits (with a
 * warning light that blinks faster as the fuse runs down), and the extra layers of its detonation.
 */
final class ThrowVfx {
	private ThrowVfx() {
	}

	static final Gradient GRAVITY = fade(0xB070FF, 0x301060);
	static final Gradient TOXIC = Gradient.of(0, 0x9AE040, 0.5F, 0x70B030, 1, 0x80A060);
	static final Gradient EMP = fade(0x60B0FF, 0x102060);
	static final Gradient SHRAPNEL = Gradient.of(0, 0xFFFFFF, 0.15F, 0xFFD080, 0.5F, 0x6A6058, 1, argb(0x3A3430, 0));

	/** The warning light's colour for each grenade, or 0 for none. */
	static int blinkColor(ProjectileKind kind) {
		return switch (kind) {
			case FRAG -> 0xFF3020;
			case PLASMA_GRENADE -> 0x55FFEE;
			case CRYO_GRENADE -> 0xA8E8FF;
			case INCENDIARY -> 0xFF8020;
			case GRAVITY -> 0xB070FF;
			case TOXIC -> 0x80FF30;
			case POCKET_NUKE -> 0xFF2010;
			case SHATTERFROST -> 0xC0F0FF;
			case ARCFLASH -> 0xE0F4FF;
			case IMPLOSION -> 0x9050FF;
			case EMP -> 0x40A0FF;
			case BOUNCE_BOMB -> 0xFF60C0;
			case LOOT_TORNADO -> 0xFFD040;
			case MOONWALK_MINE -> 0xD8D0FF;
			case HOLOGRAM -> 0x40FFFF;
			case MAGNET -> 0xFF4040;
			case PAINT_BOMB -> 0xFF40FF;
			case SEISMIC_MINE -> 0xFFA040;
			case ION_FOG -> 0x90FFE0;
			case STASIS -> 0x80E0FF;
			case QUANTUM -> 0xFF40FF;
			case CRYSTAL_SEED -> 0xC080FF;
			case VOIDSTORM -> 0x8040FF;
			case MICRO_HORIZON -> 0xFF8040;
			case MIRROR -> 0xE0E8FF;
			case SNOW_GLOBE -> 0xFFFFFF;
			case SLIMEQUAKE -> 0x80FF40;
			default -> 0;
		};
	}

	static void register() {
		common();
		trails();
		detonations();
		ThrowVfx2.register();
		ThrowVfx3.register();
		StrikeVfx.register();
		GearVfx.register();
		LegendVfx.register();
		CelestialVfx.register();
		CityVfx.register();
		SetPieceVfx.register();
		WorldVfx.register();
		BombVfx.register();
		BombVfx2.register();
		BombVfx3.register();
		BombVfx4.register();
		BombVfx5.register();
		BombVfx6.register();
		PlasmaVfx.register();
		PlasmaBombVfx.register();
		WorldBombVfx.register();
		WonderVfx.register();
		RedoVfx.register();
		FossilVfx.register();
		FossilVfx2.register();
		XenoVfx.register();
		HorrorVfx.register();
		AiVfx.register();
	}

	// ------------------------------------------------------------------ throw and warning light

	private static void common() {
		FxEngine.register(EffectDef.builder(VfxIds.GRENADE_BLINK).tint(0xFF3020).lod(16, 96)
				.emit(EmitterDef.builder("flare").priority(Priority.HERO).burst(1).inherit(1).size(0.22F, 0.22F).life(3, 3)
						.sprite(FxSprites.FLARE).tinted().color(WHITE_FADE).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.5F, 0.5F).life(3, 3).sprite(FxSprites.GLOW).tinted()
						.color(WHITE_FADE).alpha(0.6F).build())
				.emit(EmitterDef.builder("ping").burst(1).inherit(1).size(0.35F, 0.35F).life(6, 6).sprite(FxSprites.RING).tinted()
						.sizeOverLife(Curve.keys(0, 0.4F, 1, 2.2F)).color(WHITE_FADE).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.THROW_WHOOSH).lod(8, 48)
				.emit(EmitterDef.builder("streaks").burst(5).velocity(Velocity.along(0.3F, 0.6F, 0.25F)).drag(0.25F).life(4, 7)
						.size(0.015F, 0.025F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(0xFFFFFF).alpha(0.35F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("puff").burst(1).velocity(Velocity.along(0.05F, 0.1F, 0.4F)).life(10, 16).size(0.2F, 0.3F)
						.color(0xD0D0D0).alpha(0.2F).build())
				.build());
	}

	// ------------------------------------------------------------------ in flight and on the ground

	private static void trails() {
		FxEngine.register(EffectDef.builder(VfxIds.FUSE_SPARKS).lod(16, 96)
				.emit(EmitterDef.builder("sparks").burst(2).velocity(Velocity.up(0.08F, 0.18F, 0.7F)).inherit(0.6F).gravity(0.03F)
						.life(4, 9).size(0.015F, 0.025F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(smoke("wisp").burst(0.5F).velocity(Velocity.up(0.01F, 0.03F, 0.4F)).life(15, 25).size(0.12F, 0.2F).color(SOOT)
						.alpha(0.35F).module(Module.buoyancy(0.004F)).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.15F, 0.15F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFA040).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.PLASMA_PULSE).lod(16, 96)
				.emit(EmitterDef.builder("core").burst(1).inherit(1).size(0.35F, 0.35F).life(2, 2).sprite(FxSprites.GLOW).color(0x80FFF4).build())
				.emit(EmitterDef.builder("halo").burst(1).inherit(1).size(0.8F, 0.8F).life(2, 2).sprite(FxSprites.GLOW).color(0x30C8C0)
						.alpha(0.25F).build())
				.emit(EmitterDef.builder("sparkle").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).inherit(0.1F)
						.life(8, 14).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0x80FFF0).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("arc").priority(Priority.DETAIL).burst(0.5F).velocity(Velocity.random(0.08F, 0.18F))
						.module(Module.jitter(0.06F)).drag(0.4F).inherit(0.8F).life(2, 4).size(0.012F, 0.02F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(0xC0FFFF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.FROST_LEAK).lod(16, 96)
				.emit(smoke("mist").burst(1).velocity(Velocity.random(0.005F, 0.02F)).inherit(0.1F).gravity(0.003F).life(15, 30)
						.size(0.2F, 0.35F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.8F)).color(FROST).alpha(0.45F).emissive(Curve.constant(0.35F)).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).inherit(0.1F)
						.life(10, 20).size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("snow").priority(Priority.DETAIL).burst(0.5F).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.008F)
						.life(15, 30).size(0.03F, 0.04F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xFFFFFF).emissive(Curve.constant(0.6F)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.FIRE_LEAK).lod(16, 96)
				.emit(EmitterDef.builder("flames").burst(2).velocity(Velocity.up(0.03F, 0.07F, 0.4F)).inherit(0.5F).module(Module.buoyancy(0.008F))
						.life(6, 12).size(0.15F, 0.3F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ember").priority(Priority.DETAIL).burst(1).velocity(Velocity.up(0.04F, 0.1F, 0.6F)).module(Module.jitter(0.004F))
						.life(12, 24).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.emit(smoke("smoke").burst(0.4F).velocity(Velocity.up(0.02F, 0.04F, 0.3F)).life(20, 35).size(0.2F, 0.35F).color(SOOT).alpha(0.4F)
						.module(Module.buoyancy(0.006F)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GRAVITY_WARP).lod(16, 96)
				.emit(EmitterDef.builder("wisps").burst(3).shape(Shape.shell(1.3F)).module(Module.attract(0.02F, 0.15F)).inherit(1)
						.life(8, 14).size(0.2F, 0.35F).sprite(FxSprites.WISP).color(GRAVITY).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("ring").burst(1).inherit(1).size(0.6F, 0.6F).life(6, 6).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 1.4F, 1, 0.2F)).color(0xB070FF).alpha(0.4F).alphaOverLife(Curve.FADE).build())
				.emit(smoke("core").burst(1).inherit(1).size(0.25F, 0.25F).life(2, 2).fixedRotation().sprite(FxSprites.GLOW).sizeOverLife(Curve.ONE)
						.alphaOverLife(Curve.ONE).color(0x000000).shade(1).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GAS_LEAK).lod(16, 96)
				.emit(smoke("gas").burst(1).velocity(Velocity.random(0.01F, 0.03F)).inherit(0.2F).life(25, 45).size(0.2F, 0.35F)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 3)).color(TOXIC).alpha(0.45F).emissive(Curve.constant(0.25F))
						.module(Module.buoyancy(0.002F)).module(Module.curl(2, 0.003F)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.NUKE_TICK).lod(16, 96)
				.emit(EmitterDef.builder("motes").burst(2).shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.01F, 0.03F)).life(15, 30)
						.size(0.05F, 0.08F).sprite(FxSprites.FLARE).color(0xC8FF40).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.4F, 0.4F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFE040)
						.alpha(0.4F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ARC_CRACKLE).lod(16, 96)
				.emit(EmitterDef.builder("arcs").burst(2).velocity(Velocity.random(0.1F, 0.25F)).module(Module.jitter(0.08F)).drag(0.4F)
						.inherit(0.8F).life(2, 4).size(0.012F, 0.02F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK)
						.color(0xE0F4FF).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.3F, 0.3F).life(2, 2).sprite(FxSprites.GLOW).color(0xA8E6FF)
						.alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.EMP_PING).lod(16, 96)
				.emit(EmitterDef.builder("sparks").burst(1).velocity(Velocity.random(0.05F, 0.15F)).inherit(0.5F).drag(0.2F).life(3, 6)
						.size(0.012F, 0.02F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMP).build())
				.emit(EmitterDef.builder("glow").burst(1).inherit(1).size(0.3F, 0.3F).life(2, 2).sprite(FxSprites.GLOW).color(0x4090FF)
						.alpha(0.5F).build())
				.build());

		// The spinning blade's blur, and ghost rings and sparkles left along its path.
		FxEngine.register(EffectDef.builder(VfxIds.BOOMERANG_TRAIL).lod(24, 160)
				.emit(EmitterDef.builder("blur").priority(Priority.HERO).burst(1).inherit(1).size(0.9F, 0.9F).life(2, 2).spin(0.8F, 1.2F)
						.sprite(FxSprites.RING).color(0x55FFEE).alpha(0.7F).build())
				.emit(EmitterDef.builder("streak").burst(1).inherit(1).size(0.2F, 0.2F).life(2, 2).mode(Mode.STRETCHED).stretch(1.2F)
						.sprite(FxSprites.GLOW).color(0x80FFF4).build())
				.emit(EmitterDef.builder("ghosts").burst(2).span().size(0.7F, 0.7F).life(4, 4).sprite(FxSprites.RING).color(0x40D0D0)
						.alpha(0.35F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparkle").priority(Priority.DETAIL).burst(4).span().velocity(Velocity.random(0.01F, 0.04F)).inherit(0.1F)
						.life(8, 14).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0x80FFF0).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ detonations

	private static void detonations() {
		EmitterDef dustKick = smoke("kick").priority(Priority.DETAIL).burst(2).velocity(Velocity.up(0.02F, 0.06F, 0.5F)).life(15, 30)
				.size(0.2F, 0.35F).color(Gradient.of(0, 0x7A6E60, 1, 0x9A9088)).alpha(0.6F).build();
		FxEngine.register(EffectDef.builder(VfxIds.FRAG_SHRAPNEL).lod(32, 200)
				.emit(EmitterDef.builder("shrapnel").burst(80).velocity(Velocity.random(0.6F, 1.4F)).gravity(0.03F).drag(0.03F)
						.collide(0.2F, 0.5F, 1).onHit(0.3F, dustKick).life(10, 25).size(0.025F, 0.04F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(SHRAPNEL).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.PLASMA_NOVA).lod(48, 300)
				.emit(EmitterDef.builder("sphere").priority(Priority.HERO).burst(1).size(5, 5).life(8, 8).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xE0FFFF, 1, 0x30C8C0)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ground").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(9, 9).life(12, 12).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x55FFEE).build())
				.emit(EmitterDef.builder("ring").burst(1).size(6, 6).life(10, 10).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(WeaponVfx.PLASMA).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("wisps").burst(16).shape(Shape.sphere(1.5F)).velocity(Velocity.radial(0.05F, 0.15F)).module(Module.curl(3, 0.01F))
						.drag(0.05F).life(20, 35).size(0.5F, 0.9F).sprite(FxSprites.WISP).color(0x55FFEE).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("sparkles").priority(Priority.DETAIL).burst(40).velocity(Velocity.random(0.1F, 0.35F)).drag(0.06F)
						.life(15, 35).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0x80FFF0).alphaOverLife(TWINKLE).build())
				.build());

		EmitterDef napalmPool = EmitterDef.builder("pool").burst(3).shape(Shape.disc(0.4F)).velocity(Velocity.up(0.01F, 0.04F, 0.3F))
				.module(Module.buoyancy(0.004F)).life(30, 60).size(0.4F, 0.7F).mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(FIRE)
				.sizeOverLife(Curve.keys(0, 0.3F, 0.2F, 1, 1, 0.4F)).alphaOverLife(Curve.FADE).build();
		FxEngine.register(EffectDef.builder(VfxIds.INCENDIARY_BURST).lod(40, 240)
				.emit(EmitterDef.builder("flash").burst(1).size(3, 3).life(6, 6).sprite(FxSprites.GLOW).color(0xFFA040).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).burst(12).shape(Shape.sphere(0.8F)).velocity(Velocity.radial(0.05F, 0.15F))
						.module(Module.buoyancy(0.015F)).drag(0.1F).life(14, 24).size(1, 1.6F).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("blobs").burst(30).shape(Shape.dome(0.5F)).velocity(Velocity.radial(0.2F, 0.45F))
						.velocity(Velocity.up(0.15F, 0.35F, 0.9F)).gravity(0.05F).collide(0, 0.3F, 0).onHit(1, napalmPool).life(30, 50)
						.size(0.25F, 0.4F).sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(smoke("smoke").burst(8).shape(Shape.dome(1)).velocity(Velocity.up(0.04F, 0.1F, 0.5F)).life(50, 90).size(1, 1.6F).color(SOOT)
						.alpha(0.8F).emissive(VfxLibrary.INNER_GLOW).module(Module.buoyancy(0.01F)).module(Module.wind(1)).build())
				.build());

		// Matter drawn in from all round for two seconds, then a violet pop.
		int pop = 44;
		FxEngine.register(EffectDef.builder(VfxIds.GRAVITY_WELL).lod(32, 240)
				.emit(EmitterDef.builder("inflow").rate(8, 40).shape(Shape.shell(5)).module(Module.attract(0.04F, 0.4F)).drag(0.06F)
						.life(30, 45).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(GRAVITY)
						.alphaOverLife(Curve.BELL).build())
				.emit(smoke("dust").rate(2, 36).onGround().shape(Shape.ring(5, 1)).module(Module.attract(0.02F, 0.6F)).drag(0.08F)
						.life(25, 40).size(0.4F, 0.7F).sprite(FxSprites.DUST, 2).color(0x7A6A60).alpha(0.6F).sizeOverLife(Curve.FALL).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(pop, pop).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 1, 1, 0.1F)).alpha(0.6F).alphaOverLife(Curve.FADE).color(0xB070FF).build())
				.emit(smoke("core").priority(Priority.HERO).burst(1).life(pop, pop).size(0.8F, 0.8F).fixedRotation().sprite(FxSprites.GLOW)
						.sizeOverLife(Curve.keys(0, 0.2F, 0.85F, 1, 1, 0.3F)).alphaOverLife(Curve.keys(0, 0, 0.2F, 1, 1, 1)).color(0x000000).shade(1).build())
				.emit(EmitterDef.builder("rim").burst(1).life(pop, pop).size(1.2F, 1.2F).sprite(FxSprites.RING).spin(0.08F, 0.15F)
						.sizeOverLife(Curve.keys(0, 0.2F, 0.85F, 1, 1, 0.3F)).color(0xB070FF).alphaOverLife(Curve.FADE).build())
				.emit(pop, flash("pop", 2.5F, 5, 0xC8A0FF).build())
				.emit(pop, EmitterDef.builder("shock").burst(1).size(5, 5).life(10, 10).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0xC0A0FF).alphaOverLife(Curve.FADE_OUT).build())
				.emit(pop, sparkShower("sparks", 50, 0.3F, 0.8F, GRAVITY, null))
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GAS_BURST).lod(32, 200)
				.emit(EmitterDef.builder("flash").burst(1).size(1.5F, 1.5F).life(4, 4).sprite(FxSprites.GLOW).color(0x9AE040).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("puff").burst(14).shape(Shape.dome(0.6F)).velocity(Velocity.radial(0.12F, 0.3F)).drag(0.06F).life(40, 70)
						.size(0.6F, 1).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.4F)).color(TOXIC).alpha(0.6F).emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("droplets").priority(Priority.DETAIL).burst(20).velocity(Velocity.random(0.15F, 0.4F)).gravity(0.04F)
						.collide(0, 0.5F, 0).life(15, 30).size(0.03F, 0.05F).mode(Mode.STRETCHED).layer(Layer.SMOKE).sprite(FxSprites.GLOW)
						.color(0x9AE040).emissive(Curve.constant(0.6F)).build())
				.build());

		EmitterDef glint = EmitterDef.builder("glint").priority(Priority.DETAIL).burst(2).velocity(Velocity.random(0.01F, 0.03F))
				.life(15, 30).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build();
		FxEngine.register(EffectDef.builder(VfxIds.SHATTER_BURST).lod(40, 240)
				.emit(flash("flash", 2.5F, 4, 0xC0F0FF).build())
				.emit(EmitterDef.builder("shards").burst(60).velocity(Velocity.random(0.3F, 0.8F)).velocity(Velocity.up(0.05F, 0.2F, 1.0F))
						.gravity(0.035F).drag(0.04F).collide(0.1F, 0.5F, 0).onHit(0.3F, glint).life(14, 28).size(0.05F, 0.08F)
						.mode(Mode.STRETCHED).stretch(1.2F).sprite(FxSprites.STREAK).color(Gradient.of(0, 0xFFFFFF, 0.5F, 0xC0F0FF, 1, argb(0x60A0FF, 0)))
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(12, 12).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xC0F0FF).build())
				.emit(smoke("mist").burst(12).shape(Shape.dome(1.2F)).velocity(Velocity.radial(0.05F, 0.12F)).gravity(0.004F).drag(0.06F)
						.life(50, 90).size(0.7F, 1.2F).sizeOverLife(Curve.keys(0, 0.4F, 1, 2)).color(FROST).alpha(0.55F)
						.emissive(Curve.constant(0.35F)).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(30).shape(Shape.sphere(2.5F)).life(30, 60).size(0.05F, 0.1F)
						.sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.EMP_PULSE).lod(48, 300)
				.emit(EmitterDef.builder("flash").burst(1).size(3, 3).life(4, 4).sprite(FxSprites.GLOW).color(0x80C0FF).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ground").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(14, 14)
						.life(18, 18).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0x60B0FF).build())
				.emit(EmitterDef.builder("ring").burst(1).size(10, 10).life(14, 14).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0x80C8FF).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("arcs").burst(40).velocity(Velocity.random(0.3F, 0.9F)).module(Module.jitter(0.15F)).drag(0.3F)
						.life(4, 10).size(0.025F, 0.04F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).color(EMP).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("fallout").priority(Priority.DETAIL).burst(40).shape(Shape.sphere(4)).gravity(0.03F).drag(0.02F)
						.life(20, 40).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(EmitterDef.builder("static").priority(Priority.DETAIL).burst(30).shape(Shape.sphere(5)).life(10, 25).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0x80C8FF).alphaOverLife(FLICKER).build())
				.build());
	}
}
