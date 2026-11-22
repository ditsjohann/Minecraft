package com.armageddon.client.vfx;

import com.armageddon.client.fx.ClientFx;
import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.sound.SoundEvents;

import static com.armageddon.client.vfx.Gradient.argb;

/**
 * The particle-engine effects, each built as layers with their own timing (see docs/vfx/EFFECT_RECIPES.md). Sizes
 * are quad half-widths in blocks and speeds blocks per tick, at scale 1; the server picks the scale.
 */
public final class VfxLibrary {
	private VfxLibrary() {
	}

	// ------------------------------------------------------------------ shared ramps

	/** White-hot → yellow → orange → dull red → gone. */
	static final Gradient FIRE = Gradient.of(0, 0xFFFFF0, 0.2F, 0xFFD070, 0.45F, 0xFF7020, 0.75F, 0xB02808, 1, argb(0x300800, 0));
	static final Gradient SPARK = Gradient.of(0, 0xFFFFFF, 0.2F, 0xFFD080, 0.6F, 0xFF6010, 1, argb(0x801000, 0));
	static final Gradient EMBER = Gradient.of(0, 0xFFE080, 0.5F, 0xFF6010, 1, argb(0x401000, 0));
	static final Gradient SOOT = Gradient.of(0, 0x3A3430, 0.5F, 0x6E6660, 1, 0x8A8480);
	static final Gradient DUST = Gradient.of(0, 0x8A7A60, 1, 0xA09078);
	static final Gradient WHITE_FADE = Gradient.of(0, 0xFFFFFF, 0.6F, 0xFFFFFF, 1, argb(0xFFFFFF, 0));

	/** Smoke lit from inside at birth, handing over to world light. */
	static final Curve INNER_GLOW = Curve.keys(0, 1, 0.15F, 0.6F, 0.35F, 0);
	/** Smoke grows fast, then keeps spreading. */
	static final Curve BILLOW = Curve.keys(0, 0.5F, 0.3F, 1.3F, 1, 2.2F);
	static final Curve TWINKLE = Curve.of(x -> (0.55 + 0.45 * Math.sin(x * 37)) * (1 - x));
	static final Curve FLICKER = Curve.of(x -> ((x * 7) % 1 < 0.5 ? 1.0 : 0.35) * (1 - x));

	public static void bootstrap() {
		FxEngine.register(sparks());
		FxEngine.register(grenadeBlast());
		FxEngine.register(plasmaImpact());
		FxEngine.register(napalm());
		FxEngine.register(smokeColumn());
		FxEngine.register(cryoBurst());
		FxEngine.register(voidImplosion());
		FxEngine.register(toxicCloud());
		FxEngine.register(arcBurst());
		FxEngine.register(radiation());
		FxEngine.register(emberRain());
		FxEngine.register(firework());
		FxEngine.register(muzzleFlash());
		FxEngine.register(nuke());
		WildVfx.register();
		WeaponVfx.register();
		for (String id : VfxIds.ALL) {
			if (FxEngine.get(id) == null) {
				throw new IllegalStateException("particle effect " + id + " is named but not defined");
			}
		}
	}

	// ------------------------------------------------------------------ building blocks

	static EmitterDef.Builder flash(String name, float size, int life, int rgb) {
		return EmitterDef.builder(name).priority(Priority.HERO).burst(1).size(size, size).life(life, life)
				.sprite(FxSprites.FLARE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE_OUT)
				.color(Gradient.of(0, 0xFFFFFF, 1, rgb));
	}

	static EmitterDef sparkShower(String name, float count, float speedMin, float speedMax, Gradient color, EmitterDef onHit) {
		EmitterDef.Builder b = EmitterDef.builder(name).priority(Priority.DETAIL).burst(count)
				.velocity(Velocity.random(speedMin, speedMax)).velocity(Velocity.up(0.05F, 0.2F, 1.0F))
				.life(10, 26).size(0.035F, 0.06F).gravity(0.04F).drag(0.04F).collide(0.35F, 0.6F, 2)
				.mode(Mode.STRETCHED).stretch(2.0F).sprite(FxSprites.STREAK).color(color).alphaOverLife(Curve.FADE_OUT);
		if (onHit != null) {
			b.onHit(0.25F, onHit);
		}
		return b.build();
	}

	static final EmitterDef SPARK_PUFF = EmitterDef.builder("spark_puff").priority(Priority.DETAIL).burst(4)
			.velocity(Velocity.random(0.02F, 0.07F)).velocity(Velocity.up(0.02F, 0.05F, 0.8F))
			.life(5, 10).size(0.02F, 0.035F).gravity(0.03F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
			.color(Gradient.of(0, 0xFFE0A0, 1, argb(0xFF5010, 0))).alphaOverLife(Curve.FADE_OUT).build();

	static EmitterDef.Builder smoke(String name) {
		return EmitterDef.builder(name).layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6).spin(0.004F, 0.015F).shade(0.65F)
				.sizeOverLife(BILLOW).alphaOverLife(Curve.BELL).emissive(Curve.constant(0));
	}

	// ------------------------------------------------------------------ effects

	/** Sparks off a hit: a flare, a shower that bounces, little puffs where it lands. */
	static EffectDef sparks() {
		return EffectDef.builder(VfxIds.SPARKS).lod(24, 128)
				.emit(flash("flare", 0.6F, 3, 0xFFD080).build())
				.emit(sparkShower("sparks", 60, 0.25F, 0.8F, SPARK, SPARK_PUFF))
				.build();
	}

	/** A grenade-sized blast (radius ~4 at scale 1): flash, fireball, sparks, smoking debris, smoke, ground ring. */
	static EffectDef grenadeBlast() {
		EmitterDef trailPuff = smoke("trail_puff").priority(Priority.DETAIL).burst(1)
				.velocity(Velocity.random(0.0F, 0.01F)).life(18, 28).size(0.2F, 0.3F)
				.color(SOOT).alpha(0.7F).module(Module.wind(1)).build();
		return EffectDef.builder(VfxIds.GRENADE_BLAST).lod(48, 300)
				.emit(flash("flash", 4.0F, 4, 0xFFD080).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(3, 3).life(9, 9)
						.sprite(FxSprites.GLOW).color(Gradient.of(0, 0xFFE0A0, 1, 0xFF6020)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).burst(14).shape(Shape.sphere(1.0F))
						.velocity(Velocity.radial(0.08F, 0.22F)).life(10, 18).size(0.9F, 1.5F).spin(0.02F, 0.06F)
						.module(Module.buoyancy(0.02F)).drag(0.15F)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("shock").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(7, 7).life(9, 9)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.keys(0, 0.1F, 1, 1)).alpha(0.7F).alphaOverLife(Curve.FADE_OUT)
						.color(0xFFF0D0).build())
				.emit(sparkShower("sparks", 45, 0.3F, 1.0F, SPARK, SPARK_PUFF))
				.emit(EmitterDef.builder("debris").priority(Priority.NORMAL).burst(6).shape(Shape.sphere(0.5F))
						.velocity(Velocity.up(0.3F, 0.55F, 0.9F)).life(24, 40).size(0.07F, 0.1F).gravity(0.045F)
						.collide(0.2F, 0.5F, 1).sprite(FxSprites.FIRE).color(EMBER).alphaOverLife(Curve.FADE_OUT)
						.every(2, trailPuff).build())
				.emit(2, smoke("smoke").priority(Priority.HERO).burst(16).shape(Shape.sphere(1.2F))
						.velocity(Velocity.radial(0.03F, 0.09F)).velocity(Velocity.up(0.02F, 0.06F, 0.6F))
						.life(50, 90).size(1.0F, 1.6F).color(SOOT).alpha(0.85F).alphaOverLife(Curve.keys(0, 0, 0.1F, 1, 0.6F, 0.7F, 1, 0))
						.emissive(INNER_GLOW).module(Module.buoyancy(0.012F)).module(Module.curl(6, 0.004F))
						.module(Module.wind(1)).drag(0.06F).build())
				.emit(1, smoke("dust").burst(22).onGround().shape(Shape.ring(0.8F, 0.6F)).velocity(Velocity.radial(0.18F, 0.32F))
						.life(25, 45).size(0.6F, 1.0F).sprite(FxSprites.DUST, 2).color(DUST).alpha(0.8F).shade(0.8F)
						.sizeOverLife(Curve.keys(0, 0.4F, 1, 1.8F)).drag(0.1F).build())
				.cue(0, EffectDef.flash(0.35F, 48, 0xFFD8A0))
				.cue(0, EffectDef.light(0.8F, 0xFF9040))
				.sonic(0, EffectDef.shake(0.5F, 40))
				.sonic(0, EffectDef.sound(() -> SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.5F, 1.0F))
				.build();
	}

	/** An energy hit, coloured by the tint: hot core, tinted halo and ring, sparks along the surface normal, wisps. */
	static EffectDef plasmaImpact() {
		return EffectDef.builder(VfxIds.PLASMA_IMPACT).tint(0x60E8FF).lod(32, 200)
				.emit(flash("core", 1.2F, 4, 0xFFFFFF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("halo").burst(1).size(2.2F, 2.2F).life(8, 8).sprite(FxSprites.GLOW).tinted()
						.color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ring").burst(1).size(2.5F, 2.5F).life(8, 8).sprite(FxSprites.RING).tinted()
						.sizeOverLife(Curve.keys(0, 0.1F, 1, 1)).color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(35)
						.velocity(Velocity.along(0.2F, 0.6F, 1.3F)).velocity(Velocity.random(0.05F, 0.2F))
						.life(8, 18).size(0.03F, 0.05F).gravity(0.02F).drag(0.08F).collide(0.4F, 0.6F, 1)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).tinted().color(WHITE_FADE).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("wisps").priority(Priority.DETAIL).burst(10).shape(Shape.sphere(0.4F))
						.velocity(Velocity.random(0.02F, 0.06F)).life(20, 35).size(0.3F, 0.5F).module(Module.curl(3, 0.01F)).drag(0.05F)
						.sprite(FxSprites.WISP).tinted().color(WHITE_FADE).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.cue(0, fx -> ClientFx.light(fx.origin(), 0.4F, fx.tint))
				.cue(0, EffectDef.sound(() -> ModSounds.PLASMA_IMPACT, 1.2F, 1.0F))
				.build();
	}

	/** A patch of burning ground for five seconds: upright flames, rising embers, smoke. */
	static EffectDef napalm() {
		Curve burnOut = Curve.keys(0, 1, 0.7F, 0.8F, 1, 0);
		return EffectDef.builder(VfxIds.NAPALM).lod(40, 240).maxAge(400)
				.emit(EmitterDef.builder("flames").priority(Priority.HERO).rate(6, 100, burnOut).onGround().shape(Shape.disc(2.0F))
						.velocity(Velocity.up(0.03F, 0.07F, 0.3F)).life(10, 18).size(0.35F, 0.6F)
						.module(Module.buoyancy(0.01F)).module(Module.curl(2, 0.006F))
						.mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(Gradient.of(0, 0xFFF0C0, 0.3F, 0xFF9A30, 0.7F, 0xE04010, 1, argb(0x400800, 0)))
						.sizeOverLife(Curve.keys(0, 0.3F, 0.25F, 1, 1, 0.4F)).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).rate(0.6F, 100, burnOut).onGround().shape(Shape.disc(2.0F))
						.velocity(Velocity.up(0.05F, 0.12F, 0.5F)).life(25, 45).size(0.025F, 0.04F)
						.module(Module.buoyancy(0.003F)).module(Module.jitter(0.004F)).drag(0.03F)
						.mode(Mode.STRETCHED).stretch(1).sprite(FxSprites.STREAK).color(EMBER).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("smoke").rate(0.5F, 110, burnOut).onGround().shape(Shape.disc(2.0F)).velocity(Velocity.up(0.03F, 0.06F, 0.3F))
						.life(60, 100).size(0.6F, 1.0F).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.5F))
						.color(Gradient.of(0, 0x2A2420, 1, 0x5A5450)).alpha(0.7F)
						.emissive(Curve.keys(0, 0.8F, 0.3F, 0)).module(Module.buoyancy(0.01F)).module(Module.wind(1))
						.module(Module.curl(5, 0.003F)).build())
				.cue(0, EffectDef.light(0.5F, 0xFF8030))
				.cue(0, EffectDef.sound(() -> SoundEvents.ITEM_FIRECHARGE_USE, 1.0F, 0.8F))
				.build();
	}

	/** A long-lived column of smoke bending with the wind, lit orange at its base by the fire under it. */
	static EffectDef smokeColumn() {
		Curve fade = Curve.keys(0, 1, 0.8F, 1, 1, 0);
		return EffectDef.builder(VfxIds.SMOKE_COLUMN).lod(80, 600).maxAge(700)
				.emit(smoke("body").priority(Priority.HERO).rate(0.8F, 300, fade).shape(Shape.disc(1.5F))
						.velocity(Velocity.up(0.06F, 0.1F, 0.25F)).life(140, 220).size(1.2F, 2.0F)
						.sizeOverLife(Curve.keys(0, 0.4F, 0.3F, 1.2F, 1, 3.0F))
						.color(Gradient.of(0, 0x1E1A18, 0.4F, 0x3C3632, 1, 0x6A6460)).alpha(0.85F).shade(0.6F)
						.emissive(Curve.keys(0, 0.9F, 0.12F, 0.3F, 0.25F, 0))
						.module(Module.buoyancy(0.012F)).module(Module.wind(1.5F)).module(Module.curl(10, 0.003F)).drag(0.02F).build())
				.emit(EmitterDef.builder("fire").rate(0.5F, 300, fade).shape(Shape.disc(1.2F)).velocity(Velocity.up(0.04F, 0.08F, 0.2F))
						.life(10, 16).size(0.6F, 1.0F).mode(Mode.UPRIGHT).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.keys(0, 0.4F, 0.3F, 1, 1, 0.5F)).build())
				.build();
	}

	/** Ice: shards that shatter into glints where they land, heavy mist that pours along the ground, a frost ring. */
	static EffectDef cryoBurst() {
		EmitterDef glint = EmitterDef.builder("glint").priority(Priority.DETAIL).burst(3).velocity(Velocity.random(0.01F, 0.03F))
				.life(15, 30).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build();
		return EffectDef.builder(VfxIds.CRYO_BURST).lod(40, 240)
				.emit(flash("flash", 2.5F, 4, 0xA8E8FF).build())
				.emit(EmitterDef.builder("shards").burst(40).velocity(Velocity.random(0.3F, 0.7F)).velocity(Velocity.up(0.05F, 0.2F, 1.0F))
						.life(14, 26).size(0.05F, 0.08F).gravity(0.035F).drag(0.05F).collide(0.1F, 0.5F, 0).onHit(0.4F, glint)
						.mode(Mode.STRETCHED).stretch(1.2F).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFFFFFF, 0.5F, 0xA8E8FF, 1, argb(0x60A0FF, 0))).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("mist").burst(26).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.06F, 0.14F))
						.life(60, 100).size(0.8F, 1.4F).sizeOverLife(Curve.keys(0, 0.4F, 1, 2.0F))
						.gravity(0.004F).drag(0.06F).collide(0, 0.8F, 100)
						.color(Gradient.of(0, 0xD8F0FF, 1, 0xA8D8F0)).alpha(0.55F).shade(0.85F).emissive(Curve.constant(0.35F)).build())
				.emit(EmitterDef.builder("frost_ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(5, 5).life(12, 12)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xC0F0FF).build())
				.emit(EmitterDef.builder("glints").priority(Priority.DETAIL).burst(30).shape(Shape.sphere(2.5F))
						.life(30, 70).size(0.05F, 0.1F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.cue(0, EffectDef.light(0.4F, 0xA8E8FF))
				.cue(0, EffectDef.sound(() -> SoundEvents.BLOCK_GLASS_BREAK, 2.0F, 0.6F))
				.build();
	}

	/** Light and matter drawn into a darkening point for two seconds, then a violet pop. */
	static EffectDef voidImplosion() {
		int pop = 42;
		return EffectDef.builder(VfxIds.VOID_IMPLOSION).lod(40, 260)
				.emit(EmitterDef.builder("inflow").rate(6, 40).shape(Shape.shell(4)).module(Module.attract(0.025F, 0.3F)).drag(0.08F)
						.life(40, 60).size(0.04F, 0.07F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xC090FF, 1, 0x6020C0)).alphaOverLife(Curve.BELL).build())
				.emit(smoke("ink").rate(2, 40).shape(Shape.shell(3.5F)).module(Module.attract(0.01F, 0.4F)).module(Module.curl(3, 0.01F))
						.drag(0.1F).life(30, 50).size(0.5F, 0.9F)
						.sizeOverLife(Curve.keys(0, 1, 1, 0.2F)).color(Gradient.of(0, 0x0A0010, 1, 0x1A0830)).alpha(0.8F).build())
				.emit(smoke("core").priority(Priority.HERO).burst(1).life(pop, pop).size(1.0F, 1.0F).fixedRotation().sprite(FxSprites.GLOW)
						.sizeOverLife(Curve.keys(0, 0.1F, 0.9F, 1, 1, 0.2F)).alphaOverLife(Curve.keys(0, 0, 0.2F, 1, 1, 1))
						.color(0x000000).shade(1).build())
				.emit(EmitterDef.builder("rim").priority(Priority.HERO).burst(1).life(pop, pop).size(1.3F, 1.3F).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 0.1F, 0.9F, 1, 1, 0.2F)).alphaOverLife(Curve.keys(0, 0, 0.2F, 0.8F, 1, 1))
						.color(0x8040FF).spin(0.05F, 0.1F).build())
				.emit(pop, flash("pop", 3.0F, 5, 0xB080FF).build())
				.emit(pop, EmitterDef.builder("pop_ring").burst(1).size(6, 6).life(10, 10).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xC0A0FF).build())
				.emit(pop, sparkShower("pop_sparks", 60, 0.4F, 0.9F, Gradient.of(0, 0xFFFFFF, 0.3F, 0xC090FF, 1, argb(0x4010A0, 0)), null))
				.cue(0, EffectDef.sound(() -> ModSounds.BLACK_HOLE_COLLAPSE, 1.0F, 1.4F))
				.cue(pop, EffectDef.flash(0.25F, 40, 0xB080FF))
				.sonic(pop, EffectDef.shake(0.4F, 30))
				.build();
	}

	/** A sickly green cloud that drifts and swirls for several seconds, with floating specks. */
	static EffectDef toxicCloud() {
		return EffectDef.builder(VfxIds.TOXIC_CLOUD).lod(48, 300).maxAge(400)
				.emit(smoke("gas").priority(Priority.HERO).rate(3, 60).shape(Shape.sphere(1.0F)).velocity(Velocity.radial(0.04F, 0.1F))
						.life(100, 160).size(0.9F, 1.4F).sizeOverLife(Curve.keys(0, 0.4F, 1, 2.2F)).shade(0.8F)
						.color(Gradient.of(0, 0x5A8A20, 0.5F, 0x80B040, 1, 0x90A060)).alpha(0.6F).emissive(Curve.constant(0.25F))
						.drag(0.04F).module(Module.curl(4, 0.004F)).module(Module.wind(0.6F)).build())
				.emit(EmitterDef.builder("specks").priority(Priority.DETAIL).rate(0.5F, 120).shape(Shape.sphere(3))
						.life(30, 50).size(0.04F, 0.07F).module(Module.jitter(0.003F)).drag(0.1F)
						.color(0xB8FF50).alphaOverLife(Curve.BELL).build())
				.cue(0, EffectDef.sound(() -> SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.5F))
				.build();
	}

	/** Electricity: flickering jagged streaks, a crackling glow, a few hot sparks. */
	static EffectDef arcBurst() {
		return EffectDef.builder(VfxIds.ARC_BURST).tint(0x90D8FF).lod(32, 200)
				.emit(EmitterDef.builder("arcs").burst(30).rate(2, 10).velocity(Velocity.random(0.3F, 0.8F))
						.module(Module.jitter(0.15F)).drag(0.35F).life(4, 9).size(0.03F, 0.05F)
						.mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).tinted().color(WHITE_FADE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glow").burst(1).rate(0.3F, 10).size(1.5F, 1.5F).life(6, 6).sprite(FxSprites.GLOW).tinted()
						.color(WHITE_FADE).alphaOverLife(FLICKER).build())
				.emit(sparkShower("sparks", 20, 0.2F, 0.5F, SPARK, null))
				.cue(0, fx -> ClientFx.light(fx.origin(), 0.5F, fx.tint))
				.cue(0, EffectDef.sound(() -> SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F))
				.build();
	}

	/** Lingering radiation: green motes that twinkle and wander, and a faint glowing haze. */
	static EffectDef radiation() {
		return EffectDef.builder(VfxIds.RADIATION).lod(32, 200).maxAge(400)
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).rate(1.2F, 200).shape(Shape.sphere(3))
						.velocity(Velocity.random(0.005F, 0.015F)).module(Module.jitter(0.002F)).drag(0.05F)
						.life(30, 60).size(0.05F, 0.1F).color(0x80FF40).alphaOverLife(TWINKLE).build())
				.emit(smoke("haze").rate(0.15F, 200).shape(Shape.sphere(2.5F)).life(80, 120).size(1.0F, 1.6F)
						.color(0x4A7A20).alpha(0.25F).emissive(Curve.constant(0.5F)).module(Module.curl(5, 0.003F)).build())
				.build();
	}

	/** Glowing embers drifting down out of the sky, settling and dying where they land. */
	static EffectDef emberRain() {
		return EffectDef.builder(VfxIds.EMBER_RAIN).lod(48, 300).maxAge(500)
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).rate(4, 200).offset(0, 10, 0).shape(Shape.disc(8))
						.velocity(Velocity.random(0.01F, 0.03F)).gravity(0.003F).module(Module.jitter(0.003F))
						.module(Module.wind(1.2F)).drag(0.02F).collide(0, 0.5F, 0)
						.life(80, 140).size(0.03F, 0.05F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
						.color(EMBER).alphaOverLife(Curve.FADE).build())
				.build();
	}

	/** A rocket with a sparkling trail that bursts into a tinted sphere of stars, some crackling as they die. */
	static EffectDef firework() {
		EmitterDef trail = EmitterDef.builder("trail").priority(Priority.DETAIL).burst(2).velocity(Velocity.random(0.01F, 0.03F))
				.gravity(0.01F).drag(0.1F).life(8, 14).size(0.12F, 0.18F).color(Gradient.of(0, 0xFFD070, 1, 0xFF6010))
				.alphaOverLife(Curve.FADE_OUT).build();
		EmitterDef crackle = flash("crackle", 0.35F, 3, 0xFFFFFF).priority(Priority.DETAIL).build();
		EmitterDef stars = EmitterDef.builder("stars").priority(Priority.HERO).burst(90).velocity(Velocity.random(0.35F, 0.45F))
				.gravity(0.012F).drag(0.07F).life(30, 45).size(0.12F, 0.16F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
				.tinted().color(WHITE_FADE).alphaOverLife(Curve.FADE).onDeath(0.3F, crackle).build();
		EmitterDef bloom = flash("burst_flash", 4.0F, 6, 0xFFFFFF).tinted().build();
		EmitterDef glitter = EmitterDef.builder("glitter").priority(Priority.DETAIL).burst(40).velocity(Velocity.random(0.15F, 0.38F))
				.gravity(0.008F).drag(0.08F).life(35, 60).size(0.1F, 0.16F).tinted().color(WHITE_FADE).alphaOverLife(TWINKLE).build();
		return EffectDef.builder(VfxIds.FIREWORK).tint(0xFF4060).lod(64, 400)
				.emit(EmitterDef.builder("rocket").priority(Priority.HERO).burst(1).velocity(Velocity.up(0.9F, 1.0F, 0.08F))
						.drag(0.06F).gravity(0.01F).life(22, 26).size(0.3F, 0.3F).mode(Mode.STRETCHED).sprite(FxSprites.GLOW)
						.color(0xFFE0A0).every(1, trail).onDeath(1, stars, glitter, bloom).build())
				.cue(0, EffectDef.sound(() -> SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 1.0F))
				.sonic(24, EffectDef.sound(() -> SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 3.0F, 1.0F))
				.build();
	}

	/** A gunshot at the muzzle, along the aim: a star flash, a cone of sparks, a puff of smoke. */
	static EffectDef muzzleFlash() {
		return EffectDef.builder(VfxIds.MUZZLE_FLASH).lod(16, 64)
				.emit(flash("flash", 0.6F, 2, 0xFFF0C0).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("glow").burst(1).size(0.9F, 0.9F).life(3, 3).sprite(FxSprites.GLOW)
						.color(0xFFC060).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("cone").priority(Priority.DETAIL).burst(6).velocity(Velocity.along(0.3F, 0.6F, 0.25F))
						.life(3, 5).size(0.02F, 0.03F).drag(0.3F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFFF0C0, 1, 0xFFA040)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("puff").burst(3).velocity(Velocity.along(0.02F, 0.05F, 0.4F)).life(15, 25).size(0.12F, 0.2F)
						.sizeOverLife(Curve.keys(0, 0.4F, 1, 1.8F)).alphaOverLife(Curve.keys(0, 0.5F, 1, 0))
						.color(0x9A9690).module(Module.buoyancy(0.003F)).module(Module.wind(1)).build())
				.build();
	}

	/**
	 * A nuclear detonation (fireball radius ~5 at scale 1): flash, rising fireball, shock ring and condensation
	 * shell, a vortex stem, a mushroom cap lit from inside, a dust wall running out along the ground, falling
	 * embers, then ash. The boom and the shake arrive at the speed of sound.
	 */
	static EffectDef nuke() {
		return EffectDef.builder(VfxIds.NUKE).lod(200, 1400).maxAge(1100)
				.emit(flash("flash", 25, 6, 0xFFE0B0).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(18, 18).life(40, 40).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xFFFFF0, 0.3F, 0xFFD080, 1, 0xFF6020)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fireball").priority(Priority.HERO).rate(14, 30, Curve.FALL).shape(Shape.sphere(5))
						.velocity(Velocity.radial(0.05F, 0.15F)).life(30, 50).size(2.5F, 4).spin(0.005F, 0.02F)
						.module(Module.buoyancy(0.03F)).drag(0.1F).module(Module.curl(12, 0.01F))
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(2, EmitterDef.builder("shock").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
						.size(60, 60).life(40, 40).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(0.6F)
						.alphaOverLife(Curve.FADE_OUT).color(0xFFF0E0).build())
				.emit(4, EmitterDef.builder("condensation").priority(Priority.HERO).burst(1).size(14, 14).life(24, 24).layer(Layer.SMOKE)
						.sprite(FxSprites.RING).fixedRotation().sizeOverLife(Curve.keys(0, 0.5F, 1, 1.4F))
						.alphaOverLife(Curve.keys(0, 0, 0.1F, 0.6F, 1, 0)).emissive(Curve.ONE).color(0xF4F4F8).build())
				.emit(15, smoke("stem").priority(Priority.HERO).rate(6, 320, Curve.keys(0, 1, 0.4F, 0.8F, 1, 0)).shape(Shape.column(1.6F, 4))
						.velocity(Velocity.up(0.25F, 0.35F, 0.08F)).module(Module.vortex(0.01F, 0.004F)).module(Module.buoyancy(0.02F))
						.drag(0.04F).life(70, 110).size(1.6F, 2.6F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.6F)).alpha(0.9F).shade(0.6F)
						.color(Gradient.of(0, 0x3A2A22, 0.5F, 0x6A5A50, 1, 0x8A8078)).emissive(Curve.keys(0, 1, 0.25F, 0.6F, 0.5F, 0)).build())
				.emit(35, EmitterDef.builder("cap_fire").rate(4, 100, Curve.FALL).offset(0, 20, 0).shape(Shape.ring(3, 3))
						.velocity(Velocity.radial(0.02F, 0.05F)).life(20, 40).size(2, 3).module(Module.buoyancy(0.005F))
						.sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(40, smoke("cap").priority(Priority.HERO).rate(8, 300, Curve.keys(0, 1, 0.5F, 0.6F, 1, 0)).offset(0, 20, 0)
						.shape(Shape.ring(4, 3)).velocity(Velocity.radial(0.04F, 0.1F)).velocity(Velocity.up(0.02F, 0.05F, 0.4F))
						.life(260, 400).size(3, 5).sizeOverLife(Curve.keys(0, 0.5F, 0.3F, 1.6F, 1, 2.8F)).alpha(0.9F)
						.color(Gradient.of(0, 0x5A4038, 0.5F, 0x8A7A70, 1, 0xA09080)).emissive(Curve.keys(0, 0.9F, 0.3F, 0.4F, 0.6F, 0))
						.module(Module.buoyancy(0.006F)).module(Module.wind(0.5F)).module(Module.curl(20, 0.004F)).drag(0.03F).build())
				.emit(6, smoke("dust_wall").rate(30, 40).onGround().shape(Shape.ring(6, 2)).velocity(Velocity.radial(1.2F, 1.5F))
						.life(50, 90).size(2, 3.5F).sprite(FxSprites.DUST, 2).sizeOverLife(Curve.keys(0, 0.5F, 1, 3)).color(DUST).alpha(0.8F)
						.drag(0.035F).gravity(0.002F).shade(0.8F).build())
				.emit(60, EmitterDef.builder("embers").priority(Priority.DETAIL).rate(6, 200).offset(0, 20, 0).shape(Shape.ring(8, 10))
						.velocity(Velocity.random(0.01F, 0.04F)).gravity(0.006F).drag(0.03F).module(Module.jitter(0.002F))
						.life(60, 120).size(0.08F, 0.12F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFFD070, 0.6F, 0xFF5010, 1, argb(0x200000, 0))).alphaOverLife(Curve.FADE).build())
				.emit(200, smoke("ash").priority(Priority.DETAIL).rate(3, 400).offset(0, 30, 0).shape(Shape.disc(30))
						.gravity(0.003F).drag(0.05F).module(Module.jitter(0.003F)).module(Module.wind(1)).life(200, 300).size(0.06F, 0.1F)
						.sprite(FxSprites.DUST, 2).sizeOverLife(Curve.ONE).color(0x3A3632).alpha(0.8F).build())
				.cue(0, EffectDef.flash(0.38F, 400, 0xFFF0D0))
				.cue(0, EffectDef.light(1.5F, 0xFFB060))
				.sonic(0, EffectDef.shake(0.9F, 400))
				.sonic(0, EffectDef.sound(() -> ModSounds.NUKE_DETONATE, 6, 1))
				.sonic(40, EffectDef.sound(() -> ModSounds.NUKE_RUMBLE, 6, 1))
				.build();
	}
}
