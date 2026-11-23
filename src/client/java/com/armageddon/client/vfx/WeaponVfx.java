package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.EMBER;
import static com.armageddon.client.vfx.VfxLibrary.FIRE;
import static com.armageddon.client.vfx.VfxLibrary.INNER_GLOW;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;

/**
 * Muzzles, trails and impacts for the weapons: the plasma rifle, gauss railgun, rotary cannon, flamethrower, arc
 * thrower, cryo cannon, rocket launcher, gravity hammer, death ray and BFG. Projectile trails are drawn here on the
 * client every tick ({@link #trail}); the rest is played by the server.
 */
public final class WeaponVfx {
	private WeaponVfx() {
	}

	static Gradient fade(int from, int to) {
		return Gradient.of(0, 0xFFFFFF, 0.25F, from, 1, argb(to, 0));
	}

	static final Gradient PLASMA = fade(0x55FFEE, 0x108080);
	static final Gradient RAIL = fade(0x66E0FF, 0x1050A0);
	static final Gradient FROST = Gradient.of(0, 0xF0FAFF, 1, argb(0xA8D8F0, 0));
	static final Gradient HAMMER = fade(0xB58CFF, 0x401890);
	static final Gradient RED = fade(0xFF5040, 0x600008);
	static final Gradient GREEN = fade(0x80FF50, 0x106010);
	static final Gradient GREEN_FIRE = Gradient.of(0, 0xE8FFD0, 0.2F, 0x9AFF50, 0.5F, 0x40D020, 0.8F, 0x107010, 1, argb(0x042004, 0));
	static final Gradient BRASS = Gradient.of(0, 0xF0D070, 1, 0xB08A30);

	/**
	 * Draws a projectile's trail this tick, riding along with it. Called on the client by the projectile; returns false
	 * for kinds that keep their old particles.
	 */
	public static boolean trail(WmdProjectileEntity e) {
		ProjectileKind kind = e.getKind();
		String id = switch (kind) {
			case PLASMA -> VfxIds.PLASMA_TRAIL;
			case CRYO -> VfxIds.CRYO_TRAIL;
			case ROCKET, MIRV -> VfxIds.ROCKET_TRAIL;
			case BFG -> VfxIds.BFG_TRAIL;
			case FRAG -> VfxIds.FUSE_SPARKS;
			case PLASMA_GRENADE -> VfxIds.PLASMA_PULSE;
			case CRYO_GRENADE, SHATTERFROST -> VfxIds.FROST_LEAK;
			case INCENDIARY -> VfxIds.FIRE_LEAK;
			case GRAVITY, IMPLOSION -> VfxIds.GRAVITY_WARP;
			case TOXIC -> VfxIds.GAS_LEAK;
			case POCKET_NUKE -> VfxIds.NUKE_TICK;
			case ARCFLASH -> VfxIds.ARC_CRACKLE;
			case EMP -> VfxIds.EMP_PING;
			case BOOMERANG -> VfxIds.BOOMERANG_TRAIL;
			case BOUNCE_BOMB -> VfxIds.BOUNCE_TRAIL;
			case LOOT_TORNADO -> VfxIds.LOOT_GLINT;
			case MOONWALK_MINE -> VfxIds.MOON_DUST;
			case HOLOGRAM -> VfxIds.HOLO_FLICKER;
			case MAGNET -> VfxIds.MAGNET_SPARKS;
			case PAINT_BOMB -> VfxIds.PAINT_DRIP;
			case GATLING_NUKE, MINI_NUKE, FALLING_NUKE -> VfxIds.NUKE_ROUND_TRAIL;
			case BOMBLET -> VfxIds.BOMBLET_TRAIL;
			case SINGULARITY -> VfxIds.SINGULARITY_TRAIL;
			case MORTAR -> VfxIds.MORTAR_TRAIL;
			case METEOR -> VfxIds.METEOR_TRAIL;
			case ROD -> VfxIds.ROD_TRAIL;
			case AIR_BOMB -> VfxIds.BOMB_WHISTLE;
			case SEISMIC_MINE -> VfxIds.SEISMIC_HUM;
			case ION_FOG -> VfxIds.ION_GLOW;
			case STASIS -> VfxIds.STASIS_HEX;
			case QUANTUM -> VfxIds.QUANTUM_GLITCH;
			case CRYSTAL_SEED -> VfxIds.CRYSTAL_GLINT;
			case VOIDSTORM -> VfxIds.VOID_CRACKLE;
			case MICRO_HORIZON -> VfxIds.HORIZON_PULL;
			case MIRROR -> VfxIds.MIRROR_SHIMMER;
			case SNOW_GLOBE -> VfxIds.SNOW_SWIRL;
			case SLIMEQUAKE -> VfxIds.SLIME_DRIP;
			default -> null;
		};
		if (id == null) {
			return false;
		}
		var v = e.getVelocity();
		long seed = e.getId() * 0x9E3779B97F4A7C15L + e.age;
		FxEngine.playMoving(id, e.getX(), e.getY(), e.getZ(), (float) -v.x, (float) -v.y, (float) -v.z, 1.0F, 0,
				seed, (float) v.x, (float) v.y, (float) v.z);
		int tint = ThrowVfx.blinkColor(kind);
		if (kind.isGrenade() && tint != 0) {
			// The warning light: blinks faster and faster as the fuse runs down.
			int left = Math.max(0, kind.fuse - e.age);
			int every = Math.max(2, left / 6);
			if (e.age % every == 0) {
				FxEngine.playMoving(VfxIds.GRENADE_BLINK, e.getX(), e.getY() + 0.25, e.getZ(), 0, 1, 0, 1.0F, tint, seed + 1,
						(float) v.x, (float) v.y, (float) v.z);
			}
		}
		return true;
	}

	static void register() {
		ThrowVfx.register();
		plasma();
		railgun();
		minigun();
		flamethrower();
		arc();
		cryo();
		rocket();
		bfg();
		hammer();
		deathRay();
	}

	// ------------------------------------------------------------------ plasma rifle

	private static void plasma() {
		FxEngine.register(EffectDef.builder(VfxIds.PLASMA_MUZZLE).lod(16, 96)
				.emit(flash("flash", 0.55F, 3, 0x55FFEE).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("ring").burst(1).size(0.7F, 0.7F).life(6, 6).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(PLASMA).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(8).velocity(Velocity.along(0.25F, 0.6F, 0.35F)).drag(0.2F)
						.life(4, 8).size(0.025F, 0.035F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(PLASMA)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.PLASMA_TRAIL).lod(24, 160)
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).inherit(1).size(0.55F, 0.55F).life(3, 3).sprite(FxSprites.GLOW)
						.color(0xA0FFF8).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("streak").priority(Priority.HERO).burst(1).inherit(1).size(0.22F, 0.22F).life(2, 2)
						.mode(Mode.STRETCHED).stretch(1.4F).sprite(FxSprites.GLOW).color(0x80FFF4).build())
				.emit(EmitterDef.builder("halo").burst(2).span().inherit(0.9F).size(0.9F, 1.1F).life(5, 5).sprite(FxSprites.GLOW).color(0x30C8C0)
						.alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparkle").priority(Priority.DETAIL).burst(5).span().shape(Shape.sphere(0.15F))
						.velocity(Velocity.random(0.01F, 0.04F)).inherit(0.15F).drag(0.1F).life(8, 16).size(0.06F, 0.1F)
						.sprite(FxSprites.FLARE).color(0x80FFF0).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("wisp").priority(Priority.DETAIL).burst(1).span().velocity(Velocity.random(0.01F, 0.03F))
						.module(Module.curl(2, 0.006F)).life(10, 14).size(0.25F, 0.35F).sprite(FxSprites.WISP).color(0x55FFEE).alpha(0.5F)
						.alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ gauss railgun

	private static void railgun() {
		FxEngine.register(EffectDef.builder(VfxIds.RAIL_CHARGE).lod(16, 96)
				.emit(EmitterDef.builder("inflow").burst(6).shape(Shape.shell(1.2F)).module(Module.attract(0.03F, 0.06F)).drag(0.1F)
						.life(8, 12).size(0.02F, 0.03F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(RAIL)
						.alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("core").burst(1).size(0.3F, 0.3F).life(3, 3).sprite(FxSprites.GLOW).color(0x9FEFFF)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("crackle").priority(Priority.DETAIL).burst(2).velocity(Velocity.random(0.1F, 0.25F))
						.module(Module.jitter(0.08F)).drag(0.4F).life(2, 4).size(0.015F, 0.025F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(0xF0FFFF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.RAIL_MUZZLE).lod(24, 160)
				.emit(flash("flash", 1.0F, 3, 0x9FEFFF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("shock").burst(1).size(1.4F, 1.4F).life(7, 7).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(RAIL).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("cone").priority(Priority.DETAIL).burst(20).velocity(Velocity.along(0.5F, 1.2F, 0.25F)).drag(0.12F)
						.life(5, 10).size(0.025F, 0.04F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RAIL).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("smoke").burst(4).velocity(Velocity.along(0.03F, 0.08F, 0.5F)).life(30, 50).size(0.3F, 0.5F)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.2F)).color(Gradient.of(0, 0x9AA0A8, 1, 0xC8CCD0)).alpha(0.5F)
						.module(Module.wind(1)).build())
				.build());

		// Ionised air left all along the slug's line: one packet, the direction's length is the line.
		FxEngine.register(EffectDef.builder(VfxIds.RAIL_ION).lod(64, 400)
				.emit(EmitterDef.builder("motes").priority(Priority.HERO).burst(110).span().velocity(Velocity.random(0.005F, 0.02F))
						.life(20, 40).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(0x9FE8FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("haze").burst(60).span().velocity(Velocity.random(0.002F, 0.01F)).life(15, 25).size(0.5F, 0.9F)
						.sprite(FxSprites.GLOW).color(0x4AA8FF).alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("arcs").priority(Priority.DETAIL).burst(24).span().velocity(Velocity.random(0.1F, 0.25F))
						.module(Module.jitter(0.1F)).drag(0.4F).life(3, 6).size(0.015F, 0.025F).mode(Mode.STRETCHED).stretch(2.5F)
						.sprite(FxSprites.STREAK).color(0xF0FFFF).alphaOverLife(FLICKER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.RAIL_IMPACT).lod(32, 240)
				.emit(flash("flash", 1.6F, 4, 0x9FEFFF).build())
				.emit(EmitterDef.builder("ring").burst(1).size(2, 2).life(8, 8).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(RAIL).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").burst(40).velocity(Velocity.random(0.2F, 0.7F)).velocity(Velocity.along(0.1F, 0.4F, 0.9F))
						.gravity(0.04F).drag(0.04F).collide(0.35F, 0.6F, 2).life(10, 24).size(0.03F, 0.05F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(SPARK).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("chips").priority(Priority.DETAIL).burst(8).velocity(Velocity.random(0.15F, 0.4F))
						.velocity(Velocity.along(0.1F, 0.3F, 0.8F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(20, 40).size(0.07F, 0.12F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A4644).spin(0.1F, 0.3F).build())
				.emit(smoke("smoke").burst(6).velocity(Velocity.along(0.02F, 0.08F, 0.8F)).life(30, 60).size(0.5F, 0.9F)
						.color(Gradient.of(0, 0x6A6A70, 1, 0x9A9AA0)).alpha(0.6F).module(Module.buoyancy(0.004F)).module(Module.wind(1)).build())
				.build());
	}

	// ------------------------------------------------------------------ rotary cannon

	private static void minigun() {
		FxEngine.register(EffectDef.builder(VfxIds.MINIGUN_MUZZLE).lod(16, 96)
				.emit(EmitterDef.builder("flash").priority(Priority.HERO).burst(1).size(0.35F, 0.5F).life(2, 2).layer(Layer.GLOW_TOP)
						.sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, 0xFFC060)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("petals").burst(3).velocity(Velocity.along(0.15F, 0.3F, 1.2F)).drag(0.5F).life(2, 2).size(0.05F, 0.07F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(0xFFD080).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(3).velocity(Velocity.along(0.4F, 0.8F, 0.2F)).drag(0.2F)
						.life(3, 5).size(0.015F, 0.025F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(smoke("smoke").priority(Priority.DETAIL).burst(1).velocity(Velocity.along(0.02F, 0.05F, 0.4F)).life(15, 25)
						.size(0.15F, 0.25F).sizeOverLife(Curve.keys(0, 0.5F, 1, 2)).color(0xA09A94).alpha(0.4F).module(Module.wind(1)).build())
				.build());

		// A spent brass casing kicked out of the side, tumbling and bouncing.
		FxEngine.register(EffectDef.builder(VfxIds.SHELL_CASING).lod(12, 48)
				.emit(EmitterDef.builder("casing").burst(1).velocity(Velocity.along(0.12F, 0.2F, 0.4F)).velocity(Velocity.up(0.08F, 0.15F, 0.4F))
						.gravity(0.04F).collide(0.35F, 0.6F, 3).life(40, 60).size(0.06F, 0.06F).spin(0.4F, 0.8F).layer(Layer.SMOKE)
						.sprite(FxSprites.STREAK).color(BRASS).emissive(Curve.constant(0.25F)).alphaOverLife(Curve.keys(0, 1, 0.85F, 1, 1, 0)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.BULLET_IMPACT).lod(16, 96)
				.emit(EmitterDef.builder("flash").burst(1).size(0.3F, 0.3F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFD080).build())
				.emit(EmitterDef.builder("sparks").burst(5).velocity(Velocity.random(0.1F, 0.3F)).gravity(0.04F).collide(0.3F, 0.6F, 1)
						.life(4, 8).size(0.015F, 0.025F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(smoke("dust").burst(1.5F).velocity(Velocity.random(0.01F, 0.03F)).life(15, 25).size(0.25F, 0.4F)
						.color(Gradient.of(0, 0x7A6E60, 1, 0x9A9088)).alpha(0.5F).build())
				.build());
	}

	// ------------------------------------------------------------------ flamethrower

	private static void flamethrower() {
		EmitterDef puff = smoke("puff").priority(Priority.DETAIL).burst(1).life(25, 45).size(0.5F, 0.8F)
				.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(SOOT).alpha(0.45F).emissive(INNER_GLOW)
				.module(Module.buoyancy(0.006F)).module(Module.wind(1)).build();
		FxEngine.register(EffectDef.builder(VfxIds.FLAME_JET).lod(16, 128)
				.emit(EmitterDef.builder("flames").priority(Priority.HERO).burst(7).velocity(Velocity.along(0.6F, 0.9F, 0.09F)).drag(0.06F)
						.module(Module.buoyancy(0.012F)).collide(0, 0.3F, 4).life(13, 18).size(0.15F, 0.3F).spin(0.05F, 0.15F)
						.sprite(FxSprites.FIRE, 2).color(FIRE).alpha(0.85F).sizeOverLife(Curve.keys(0, 0.4F, 0.5F, 3, 1, 4.5F))
						.alphaOverLife(Curve.FADE).onDeath(0.25F, puff).build())
				.emit(EmitterDef.builder("core").burst(2).velocity(Velocity.along(0.5F, 0.7F, 0.05F)).life(3, 5).size(0.1F, 0.15F)
						.sprite(FxSprites.GLOW).color(Gradient.of(0, 0xA0D0FF, 1, 0xFFF0C0)).alpha(0.8F).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(2).velocity(Velocity.along(0.5F, 0.8F, 0.15F)).gravity(0.01F)
						.drag(0.04F).life(15, 30).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.FLAME_SPLASH).lod(16, 128)
				.emit(EmitterDef.builder("flames").burst(4).shape(Shape.disc(0.8F)).velocity(Velocity.up(0.03F, 0.08F, 0.3F))
						.module(Module.buoyancy(0.01F)).life(10, 16).size(0.4F, 0.7F).mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(2).velocity(Velocity.up(0.05F, 0.12F, 0.6F))
						.module(Module.jitter(0.004F)).life(15, 30).size(0.03F, 0.04F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(EMBER).build())
				.build());
	}

	// ------------------------------------------------------------------ arc thrower

	private static void arc() {
		FxEngine.register(EffectDef.builder(VfxIds.ARC_MUZZLE).tint(0xA8E6FF).lod(16, 96)
				.emit(EmitterDef.builder("arcs").burst(6).velocity(Velocity.random(0.15F, 0.35F)).module(Module.jitter(0.1F)).drag(0.4F)
						.life(2, 5).size(0.02F, 0.03F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).tinted()
						.color(VfxLibrary.WHITE_FADE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.5F, 0.5F).life(3, 3).sprite(FxSprites.GLOW).tinted().color(VfxLibrary.WHITE_FADE)
						.alphaOverLife(FLICKER).build())
				.emit(sparkShower("sparks", 4, 0.1F, 0.25F, SPARK, null))
				.build());
	}

	// ------------------------------------------------------------------ cryo cannon

	private static void cryo() {
		FxEngine.register(EffectDef.builder(VfxIds.CRYO_MUZZLE).lod(16, 128)
				.emit(EmitterDef.builder("flash").burst(1).size(0.6F, 0.6F).life(3, 3).sprite(FxSprites.GLOW).color(0xC8F0FF)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("frost").burst(5).velocity(Velocity.along(0.05F, 0.15F, 0.5F)).life(20, 40).size(0.3F, 0.5F)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2)).color(FROST).alpha(0.6F).emissive(Curve.constant(0.35F)).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(6).velocity(Velocity.along(0.1F, 0.3F, 0.6F)).drag(0.1F)
						.life(10, 20).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.CRYO_TRAIL).lod(24, 160)
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).inherit(1).size(0.45F, 0.45F).life(2, 2)
						.sprite(FxSprites.GLOW).color(0xC0F0FF).build())
				.emit(smoke("mist").burst(2).span().velocity(Velocity.random(0.005F, 0.02F)).inherit(0.05F).gravity(0.002F).life(20, 35)
						.size(0.35F, 0.55F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.8F)).color(FROST).alpha(0.5F).emissive(Curve.constant(0.35F)).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(3).span().velocity(Velocity.random(0.01F, 0.03F)).inherit(0.1F)
						.life(10, 20).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("snow").priority(Priority.DETAIL).burst(1).velocity(Velocity.random(0.01F, 0.03F)).gravity(0.01F)
						.module(Module.jitter(0.004F)).life(20, 40).size(0.04F, 0.05F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xFFFFFF)
						.emissive(Curve.constant(0.6F)).build())
				.build());
	}

	// ------------------------------------------------------------------ rocket launcher

	private static void rocket() {
		FxEngine.register(EffectDef.builder(VfxIds.ROCKET_BACKBLAST).lod(24, 160)
				.emit(EmitterDef.builder("flash").burst(1).size(0.8F, 0.8F).life(3, 3).sprite(FxSprites.FIRE).color(0xFFC060)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("blast").burst(14).velocity(Velocity.along(-0.4F, -0.15F, 0.5F)).drag(0.08F).life(30, 60).size(0.8F, 1.4F)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.5F)).color(Gradient.of(0, 0x8A8480, 1, 0xC0BCB8)).alpha(0.7F)
						.emissive(Curve.keys(0, 0.7F, 0.15F, 0)).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(6).velocity(Velocity.along(-0.4F, -0.15F, 0.4F)).gravity(0.02F)
						.life(6, 12).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.ROCKET_TRAIL).lod(48, 320)
				.emit(EmitterDef.builder("exhaust").priority(Priority.HERO).burst(3).span().velocity(Velocity.along(0.05F, 0.15F, 0.2F)).inherit(0.3F)
						.life(3, 5).size(0.25F, 0.4F).sprite(FxSprites.FIRE, 2).color(Gradient.of(0, 0xFFFFF0, 1, 0xFF9030))
						.alpha(0.9F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("smoke").priority(Priority.HERO).burst(3).span().velocity(Velocity.random(0.01F, 0.03F)).velocity(Velocity.along(0.02F, 0.05F, 0.3F))
						.inherit(0.05F).life(50, 90).size(0.5F, 0.8F).sizeOverLife(Curve.keys(0, 0.7F, 0.3F, 1.8F, 1, 3.4F))
						.color(Gradient.of(0, 0x9A9690, 1, 0xC8C4C0)).alpha(0.7F).emissive(Curve.keys(0, 0.8F, 0.1F, 0))
						.module(Module.buoyancy(0.004F)).module(Module.wind(1)).module(Module.curl(4, 0.003F)).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(1).velocity(Velocity.along(0.05F, 0.15F, 0.5F)).gravity(0.02F)
						.life(8, 16).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());
	}

	// ------------------------------------------------------------------ BFG

	private static void bfg() {
		FxEngine.register(EffectDef.builder(VfxIds.BFG_MUZZLE).lod(32, 240)
				.emit(flash("flash", 2.2F, 5, 0x9AFF60).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("ring").burst(1).size(2.5F, 2.5F).life(10, 10).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(GREEN).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(30).velocity(Velocity.along(0.3F, 0.9F, 0.7F)).drag(0.08F)
						.life(6, 14).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(GREEN).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.BFG_TRAIL).lod(64, 400)
				.emit(EmitterDef.builder("aura").priority(Priority.HERO).burst(1).inherit(1).size(2.6F, 2.6F).life(3, 3).sprite(FxSprites.GLOW)
						.color(0x60FF40).alpha(0.35F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("wisps").priority(Priority.HERO).burst(4).shape(Shape.shell(1.6F)).velocity(Velocity.swirl(0.15F, 0.3F))
						.inherit(1).life(8, 14).size(0.5F, 0.9F).sprite(FxSprites.WISP).color(GREEN_FIRE).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("arcs").burst(3).velocity(Velocity.random(0.2F, 0.5F)).module(Module.jitter(0.12F)).drag(0.4F).inherit(0.9F)
						.life(3, 6).size(0.025F, 0.04F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).color(GREEN)
						.alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).burst(2).shape(Shape.sphere(1.2F)).velocity(Velocity.random(0.01F, 0.04F))
						.inherit(0.1F).life(20, 40).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(0x9AFF60).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.BFG_BLAST).lod(80, 600)
				.emit(flash("flash", 5, 5, 0xB0FF80).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(4.5F, 4.5F).life(14, 14).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xE8FFD0, 1, 0x30C020)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).burst(40).shape(Shape.sphere(1.5F)).velocity(Velocity.radial(0.1F, 0.3F))
						.module(Module.buoyancy(0.015F)).drag(0.1F).life(20, 35).size(1.2F, 2).sprite(FxSprites.FIRE, 2).color(GREEN_FIRE)
						.alpha(0.6F).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("ring").burst(1).size(8, 8).life(14, 14).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0x9AFF60).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("arcs").burst(40).velocity(Velocity.random(0.3F, 0.8F)).module(Module.jitter(0.15F)).drag(0.3F)
						.life(4, 10).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).color(GREEN)
						.alphaOverLife(FLICKER).build())
				.emit(sparkShower("sparks", 80, 0.3F, 1.0F, GREEN, null))
				.emit(smoke("smoke").burst(14).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.05F, 0.14F)).life(60, 110).size(1.4F, 2.2F)
						.color(Gradient.of(0, 0x203A20, 1, 0x4A5A48)).alpha(0.7F).emissive(Curve.keys(0, 0.9F, 0.3F, 0))
						.module(Module.buoyancy(0.01F)).module(Module.wind(1)).build())
				.build());
	}

	// ------------------------------------------------------------------ gravity hammer

	private static void hammer() {
		FxEngine.register(EffectDef.builder(VfxIds.HAMMER_SLAM).lod(32, 200)
				.emit(EmitterDef.builder("flash").burst(1).size(3, 3).life(6, 6).sprite(FxSprites.GLOW).color(0xC8A0FF)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("shock").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(8, 8)
						.life(12, 12).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xB58CFF).build())
				.emit(EmitterDef.builder("warp").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(6, 6).life(16, 16).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 0.2F, 1, 1.2F)).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).color(0x8050FF).build())
				.emit(smoke("dust").rate(24, 8).onGround().shape(Shape.ring(1, 0.6F)).expand(0.9F).velocity(Velocity.radial(0.15F, 0.3F))
						.velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(30, 50).size(0.8F, 1.4F).sprite(FxSprites.DUST, 2)
						.color(Gradient.of(0, 0x8A7A68, 1, 0xA89A88)).alpha(0.75F).drag(0.05F).build())
				.emit(EmitterDef.builder("rocks").priority(Priority.DETAIL).burst(20).onGround().shape(Shape.disc(5))
						.velocity(Velocity.up(0.25F, 0.5F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(25, 45).size(0.12F, 0.22F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A4E44).spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(30).onGround().shape(Shape.disc(3))
						.velocity(Velocity.up(0.2F, 0.5F, 0.6F)).velocity(Velocity.radial(0.1F, 0.3F)).gravity(0.03F).drag(0.03F)
						.life(12, 26).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(HAMMER).build())
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).burst(20).onGround().shape(Shape.disc(6)).gravity(-0.002F)
						.module(Module.jitter(0.004F)).life(30, 60).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(0xC8A0FF)
						.alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.HAMMER_HIT).lod(24, 128)
				.emit(EmitterDef.builder("ring").burst(1).size(1.2F, 1.2F).life(6, 6).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(HAMMER).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.8F, 0.8F).life(4, 4).sprite(FxSprites.GLOW).color(0xC8A0FF).build())
				.emit(sparkShower("sparks", 12, 0.15F, 0.4F, HAMMER, null))
				.build());
	}

	// ------------------------------------------------------------------ death ray

	private static void deathRay() {
		FxEngine.register(EffectDef.builder(VfxIds.DEATHRAY_BURN).lod(32, 240)
				.emit(EmitterDef.builder("glow").priority(Priority.HERO).burst(1).size(1.6F, 1.6F).life(3, 3).sprite(FxSprites.GLOW)
						.color(0xFF4030).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("spray").burst(10).velocity(Velocity.along(0.15F, 0.45F, 0.9F)).velocity(Velocity.random(0.05F, 0.15F))
						.gravity(0.05F).collide(0.2F, 0.5F, 1).life(10, 25).size(0.04F, 0.06F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFFFFFF, 0.15F, 0xFFE090, 0.5F, 0xFF7018, 1, argb(0xA02008, 0))).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("smoke").burst(1).velocity(Velocity.along(0.02F, 0.06F, 0.6F)).life(40, 70).size(0.6F, 1).color(SOOT).alpha(0.7F)
						.emissive(INNER_GLOW).module(Module.buoyancy(0.008F)).module(Module.wind(1)).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).burst(3).velocity(Velocity.random(0.02F, 0.08F))
						.module(Module.buoyancy(0.004F)).module(Module.jitter(0.004F)).life(20, 40).size(0.03F, 0.05F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(EMBER).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.DEATHRAY_MUZZLE).lod(16, 96)
				.emit(EmitterDef.builder("flare").burst(1).size(0.8F, 0.8F).life(3, 3).sprite(FxSprites.FLARE).color(RED).build())
				.emit(EmitterDef.builder("ring").burst(1).size(0.6F, 0.6F).life(5, 5).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(RED).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(4).velocity(Velocity.along(0.2F, 0.5F, 0.4F)).drag(0.15F)
						.life(4, 8).size(0.02F, 0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(RED).build())
				.build());
	}
}
