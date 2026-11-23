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
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The Celestial bombs, each with its own mood around the set piece it already draws.
 * <ul>
 * <li>Exploding Cows: pastoral slapstick. Shadows swell under falling cows, cartoon speed lines, real milk
 * splashes, and the giant cow's milk tidal wave ends under a rainbow.</li>
 * <li>Deadly Twister: dread. Leaves and grass spiral up the funnel, sideways rain and bouncing hail, and when it
 * ropes out, the calm: leaves drifting down through shafts of sun.</li>
 * <li>UFO Invasion: cold and precise. Motes and dust drift up the tractor beam, crop circles lift glowing chaff,
 * light rings climb the abduction shafts, the ship sheds a blue drive haze.</li>
 * <li>Resonance Cascade: harmony turned violent. Rings and glints off the fork, dust dancing on the ground like
 * sand on a speaker cone, glass shards along the collapse front, the fork shattering.</li>
 * <li>Tesla Dominion: ozone and voltage. St. Elmo's fire on the crown, scorching strikes, sparks pulled into the
 * ball lightning, a blue nova that leaves the air glowing.</li>
 * <li>Gravity Killswitch: weightless silence, then violence. A silent bubble, a field of dust, pebbles and water
 * drops drifting up, all yanked down at the slam, and a carpet of dust thrown up by it.</li>
 * </ul>
 */
final class CelestialVfx {
	private CelestialVfx() {
	}

	static final Gradient MILK = Gradient.of(0, 0xFFFFFF, 0.6F, 0xFFFBF0, 1, argb(0xF0E8D8, 0));
	static final Gradient GRASS = Gradient.of(0, 0x5A8A34, 1, 0x6A5434);
	static final Gradient HAY = fade(0xF0D070, 0xB08A30);
	static final Gradient LEAF = Gradient.of(0, 0x5A9A34, 0.5F, 0x7AA040, 1, 0x8A6A30);
	static final Gradient RAIN = Gradient.of(0, 0xB8C8D8, 1, argb(0x8898A8, 0));
	static final Gradient BEAM = fade(0xC0F0FF, 0x3080C0);
	static final Gradient CHAFF = Gradient.of(0, 0xFFF0B0, 0.5F, 0xC0F0FF, 1, argb(0x60A0D0, 0));
	static final Gradient CRYSTAL = Gradient.of(0, 0xFFFFFF, 0.4F, 0xC0F4FF, 1, argb(0x60B0E0, 0));
	static final Gradient VOLT = Gradient.of(0, 0xFFFFFF, 0.3F, 0xB8DCFF, 0.7F, 0x5A90FF, 1, argb(0x2040A0, 0));
	static final Gradient VIOLET = fade(0xD0C0FF, 0x5030A0);
	static final Gradient EARTH = Gradient.of(0, 0x7A6A58, 1, 0x9A8C7A);

	static void register() {
		cows();
		twister();
		ufo();
		resonance();
		tesla();
		gravity();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	/** A small white milk splat left where a droplet lands. */
	private static final EmitterDef DROP_SPLAT = EmitterDef.builder("drop_splat").priority(Priority.DETAIL).burst(1).mode(Mode.FLAT)
			.size(0.35F, 0.6F).life(40, 70).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0xF8F4EA).shade(1).alpha(0.8F)
			.sizeOverLife(Curve.keys(0, 0.4F, 0.2F, 1, 1, 1.1F)).alphaOverLife(Curve.keys(0, 1, 0.7F, 0.8F, 1, 0)).build();

	// ------------------------------------------------------------------ Exploding Cows

	private static void cows() {
		// A shadow that swells on the ground under a falling cow, darker as it gets close: you see where it will land.
		for (String id : new String[]{VfxIds.COW_SHADOW, VfxIds.COW_SHADOW_BIG}) {
			int life = id.equals(VfxIds.COW_SHADOW) ? 48 : 88;
			FxEngine.register(EffectDef.builder(id).lod(64, 600)
					.emit(EmitterDef.builder("shadow").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation()
							.size(2.4F, 2.4F).life(life, life).layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0x0A0A0A).shade(1)
							.sizeOverLife(Curve.keys(0, 0.25F, 1, 1.0F)).alphaOverLife(Curve.keys(0, 0, 0.3F, 0.3F, 0.92F, 0.65F, 1, 0))
							.build())
					.build());
		}

		// Cartoon speed lines streaming off a falling cow, and a little puff of cloud it punched through.
		FxEngine.register(EffectDef.builder(VfxIds.COW_STREAK).lod(48, 400)
				.emit(EmitterDef.builder("lines").burst(3).shape(Shape.ring(1.0F, 0.4F)).velocity(Velocity.up(0.5F, 0.8F, 0.02F)).drag(0.25F)
						.life(4, 7).size(0.06F, 0.1F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(0xFFFFFF).alpha(0.75F)
						.build())
				.emit(smoke("vapour").priority(Priority.DETAIL).burst(0.5F).velocity(Velocity.up(0.05F, 0.1F, 0.4F)).life(15, 25).size(0.6F, 0.9F)
						.color(0xF6F8FC).alpha(0.45F).shade(0.9F).build())
				.build());

		// A cow lands: a milk crown, droplets that splat where they fall, flung clods and hay, a puddle of milk.
		FxEngine.register(EffectDef.builder(VfxIds.MILK_SPLASH).lod(64, 500)
				.emit(EmitterDef.builder("pow").burst(7).velocity(Velocity.radial(0.35F, 0.5F)).shape(Shape.ring(0.6F, 0.2F)).drag(0.3F)
						.life(5, 7).size(0.5F, 0.8F).sprite(FxSprites.FLARE).color(0xFFFFFF).sizeOverLife(Curve.POP).build())
				.emit(EmitterDef.builder("crown").priority(Priority.HERO).burst(36).shape(Shape.ring(1.0F, 0.3F)).velocity(Velocity.up(0.35F, 0.55F, 0.15F))
						.velocity(Velocity.radial(0.08F, 0.18F)).gravity(0.06F).life(12, 20).size(0.18F, 0.26F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.GLOW).color(MILK).build())
				.emit(EmitterDef.builder("drops").burst(50).velocity(Velocity.up(0.25F, 0.7F, 0.9F)).velocity(Velocity.radial(0.1F, 0.3F)).gravity(0.05F)
						.collide(0.0F, 0.9F, 1).onHit(0.5F, DROP_SPLAT).life(25, 45).size(0.1F, 0.16F).mode(Mode.STRETCHED).sprite(FxSprites.GLOW)
						.color(MILK).build())
				.emit(smoke("mist").burst(8).shape(Shape.dome(1.2F)).velocity(Velocity.radial(0.06F, 0.15F)).life(25, 45).size(1, 1.6F)
						.color(0xFAF8F2).alpha(0.5F).shade(0.9F).build())
				.emit(EmitterDef.builder("clods").burst(14).velocity(Velocity.up(0.2F, 0.5F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 50)
						.size(0.14F, 0.24F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(GRASS).spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("hay").priority(Priority.DETAIL).burst(8).velocity(Velocity.up(0.2F, 0.45F, 0.8F)).gravity(0.02F).drag(0.04F)
						.life(30, 50).size(0.04F, 0.06F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(HAY).build())
				.emit(EmitterDef.builder("puddle").burst(1).onGround().mode(Mode.FLAT).size(3, 3).life(80, 80).layer(Layer.SMOKE)
						.sprite(FxSprites.GLOW).color(0xF8F4EA).shade(1).alpha(0.85F).sizeOverLife(Curve.keys(0, 0.3F, 0.15F, 1, 1, 1.1F))
						.alphaOverLife(Curve.keys(0, 1, 0.7F, 0.85F, 1, 0)).build())
				.build());

		// The giant cow: a milk tidal wave, a crown taller than a house, cream clouds, a hay storm and milk raining.
		FxEngine.register(EffectDef.builder(VfxIds.COW_DOOM).lod(256, 2000).maxAge(400)
				.emit(flash("flash", 22, 6, 0xFFFFFF).layer(Layer.GLOW_TOP).build())
				.emit(groundRing("shock", 110, 40, 0xFFF6E8, 0.7F))
				.emit(EmitterDef.builder("wave").priority(Priority.HERO).rate(30, 10).shape(Shape.ring(8, 3)).velocity(Velocity.radial(1.0F, 1.4F))
						.velocity(Velocity.up(0.5F, 0.9F, 0.2F)).gravity(0.04F).collide(0.0F, 0.9F, 1).onHit(0.3F, DROP_SPLAT).life(40, 70)
						.size(0.5F, 0.9F).mode(Mode.STRETCHED).sprite(FxSprites.GLOW).color(MILK).build())
				.emit(EmitterDef.builder("crown").priority(Priority.HERO).burst(140).shape(Shape.ring(6, 2)).velocity(Velocity.up(1.2F, 1.8F, 0.08F))
						.velocity(Velocity.radial(0.15F, 0.3F)).gravity(0.04F).life(40, 60).size(0.8F, 1.2F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.GLOW).color(MILK).build())
				.emit(smoke("cream").priority(Priority.HERO).burst(60).shape(Shape.dome(10)).velocity(Velocity.radial(0.3F, 0.6F)).drag(0.04F)
						.life(120, 220).size(6, 10).color(Gradient.of(0, 0xFFFFFF, 1, 0xE8E4DC)).alpha(0.75F).shade(0.85F).build())
				.emit(10, EmitterDef.builder("hay").rate(10, 80).shape(Shape.ring(15, 10)).velocity(Velocity.swirl(0.4F, 0.6F))
						.velocity(Velocity.up(0.2F, 0.4F, 0.3F)).module(Module.vortex(0.02F, 0.005F)).gravity(0.01F).life(60, 100)
						.size(0.08F, 0.12F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(HAY).build())
				.emit(30, EmitterDef.builder("rain").rate(30, 140).offset(0, 50, 0).shape(Shape.disc(70)).velocity(Velocity.up(-0.2F, -0.1F, 0.05F))
						.gravity(0.04F).collide(0.0F, 0.9F, 1).onHit(0.25F, DROP_SPLAT).life(60, 90).size(0.1F, 0.16F).mode(Mode.STRETCHED)
						.sprite(FxSprites.GLOW).color(MILK).alpha(0.8F).build())
				.build());

		// Afterwards the sun comes out through the milk spray: a rainbow over the cow-print fields.
		EffectDef.Builder rainbow = EffectDef.builder(VfxIds.MILK_RAINBOW).lod(256, 2000).maxAge(260);
		for (int k = 0; k < ThrowVfx2.RAINBOW.length; k++) {
			rainbow.emit(EmitterDef.builder("band" + k).priority(Priority.HERO).rate(12, 220, Curve.keys(0, 0, 0.15F, 1, 0.75F, 1, 1, 0))
					.shape(Shape.arch(54 - k * 2.0F, 0.8F)).life(40, 60).size(2.0F, 2.4F).layer(Layer.SMOKE).sprite(FxSprites.GLOW).shade(1)
					.emissive(Curve.ONE).color(ThrowVfx2.RAINBOW[k]).alpha(0.32F).alphaOverLife(Curve.BELL).build());
		}
		FxEngine.register(rainbow.build());
	}

	// ------------------------------------------------------------------ Deadly Twister

	private static void twister() {
		// At the foot of the funnel: leaves, grass and sticks torn up and spiralling in, and a skirt of dust.
		FxEngine.register(EffectDef.builder(VfxIds.TWISTER_BASE).lod(128, 900)
				.emit(EmitterDef.builder("leaves").burst(12).onGround().shape(Shape.ring(12, 14)).velocity(Velocity.swirl(0.6F, 1.0F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.2F)).module(Module.vortex(0.05F, 0.03F)).drag(0.02F).life(40, 70).size(0.3F, 0.45F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(LEAF).spin(0.2F, 0.5F).build())
				.emit(smoke("skirt").burst(1.5F).onGround().shape(Shape.ring(18, 6)).velocity(Velocity.swirl(0.5F, 0.8F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.3F)).module(Module.vortex(0.03F, 0.02F)).life(50, 80).size(4, 6)
						.color(Gradient.of(0, 0x9A8A74, 1, 0x8A8480)).alpha(0.45F).build())
				.emit(EmitterDef.builder("sticks").priority(Priority.DETAIL).burst(2).onGround().shape(Shape.ring(10, 8)).velocity(Velocity.swirl(0.6F, 0.9F))
						.velocity(Velocity.up(0.3F, 0.6F, 0.2F)).module(Module.vortex(0.05F, 0.03F)).life(30, 50).size(0.06F, 0.09F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(0x5A4028).build())
				.build());

		// Rain driven sideways by the storm, and hail bouncing off the ground. Aimed along the effect's direction.
		FxEngine.register(EffectDef.builder(VfxIds.TWISTER_RAIN).lod(160, 1000).maxAge(120)
				.emit(EmitterDef.builder("rain").rate(28, 40).offset(0, 32, 0).shape(Shape.disc(60)).velocity(Velocity.along(1.2F, 1.6F, 0.05F))
						.collide(0.0F, 0.0F, 1).life(22, 32).size(0.07F, 0.09F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(RAIN)
						.alpha(0.55F).build())
				.emit(EmitterDef.builder("hail").priority(Priority.DETAIL).rate(4, 40).offset(0, 30, 0).shape(Shape.disc(45))
						.velocity(Velocity.along(0.8F, 1.0F, 0.1F)).gravity(0.04F).collide(0.5F, 0.6F, 3).life(50, 70).size(0.08F, 0.12F)
						.sprite(FxSprites.FLARE).color(0xF4F8FF).build())
				.build());

		// It ropes out, and there is a calm: leaves drifting down, dust settling, shafts of sun breaking through.
		FxEngine.register(EffectDef.builder(VfxIds.TWISTER_CALM).lod(160, 1000).maxAge(300)
				.emit(smoke("collapse").burst(30).shape(Shape.column(6, 40)).velocity(Velocity.radial(0.1F, 0.25F)).drag(0.03F).life(100, 160)
						.size(5, 8).color(Gradient.of(0, 0xA8A29C, 1, 0xC8C4BE)).alpha(0.45F).build())
				.emit(EmitterDef.builder("leaves").rate(10, 200).offset(0, 40, 0).shape(Shape.disc(30)).gravity(0.004F).drag(0.05F)
						.module(Module.jitter(0.006F)).module(Module.wind(1)).life(150, 250).size(0.3F, 0.45F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(LEAF).spin(0.05F, 0.2F).build())
				.emit(20, EmitterDef.builder("sun").rate(1.2F, 160, Curve.BELL).offset(0, 40, 0).shape(Shape.disc(28))
						.velocity(Velocity.along(0.12F, 0.15F, 0.02F)).life(80, 120).size(2.5F, 4).mode(Mode.STRETCHED).stretch(60)
						.sprite(FxSprites.GLOW).color(0xFFE8A0).alpha(0.16F).alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ UFO Invasion

	private static void ufo() {
		// Under the tractor beam (spread up the beam's length): motes and dust lifting, light rings climbing it.
		FxEngine.register(EffectDef.builder(VfxIds.UFO_BEAM).lod(160, 1200)
				.emit(EmitterDef.builder("motes").burst(6).span().shape(Shape.disc(3)).velocity(Velocity.up(0.3F, 0.6F, 0)).life(20, 40)
						.size(0.1F, 0.16F).sprite(FxSprites.FLARE).color(BEAM).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(6, 6).velocity(Velocity.up(0.8F, 0.8F, 0)).life(30, 30)
						.sprite(FxSprites.RING).color(BEAM).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("swirl").burst(3).onGround().shape(Shape.ring(6, 2)).velocity(Velocity.swirl(0.3F, 0.5F)).velocity(Velocity.up(0.05F, 0.12F, 0.2F))
						.module(Module.vortex(0.02F, 0.02F)).life(30, 50).size(1.2F, 2).color(Gradient.of(0, 0x7A6E60, 1, 0xA0B8C8)).alpha(0.5F).build())
				.emit(EmitterDef.builder("pebbles").priority(Priority.DETAIL).burst(1).onGround().shape(Shape.disc(4)).velocity(Velocity.up(0.15F, 0.25F, 0.1F))
						.module(Module.jitter(0.004F)).life(60, 90).size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(EARTH)
						.spin(0.05F, 0.15F).build())
				.build());

		// The mothership's drive: a cold blue haze and sparkle sinking from under the hull, left behind as it passes.
		FxEngine.register(EffectDef.builder(VfxIds.UFO_DRIVE).lod(256, 2000)
				.emit(smoke("haze").burst(3).shape(Shape.ring(16, 6)).velocity(Velocity.up(-0.08F, -0.03F, 0.2F)).life(40, 70).size(6, 9)
						.color(0x203048).alpha(0.3F).emissive(Curve.constant(0.6F)).shade(0.9F).build())
				.emit(EmitterDef.builder("sparkle").burst(6).shape(Shape.ring(15, 8)).velocity(Velocity.up(-0.15F, -0.05F, 0.3F)).life(30, 60)
						.size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(BEAM).alphaOverLife(TWINKLE).build())
				.build());

		// A crop circle is cut: a pulse of light, and glowing chaff floats up off the whole pattern.
		FxEngine.register(EffectDef.builder(VfxIds.CROP_PULSE).lod(160, 1200).maxAge(160)
				.emit(EmitterDef.builder("pulse").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(60, 60).life(30, 30).sprite(FxSprites.GLOW)
						.color(0x80D8FF).alpha(0.35F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(groundRing("ring", 66, 24, 0xA0E8FF, 0.5F))
				.emit(EmitterDef.builder("chaff").burst(140).onGround().shape(Shape.disc(28)).velocity(Velocity.up(0.06F, 0.18F, 0.2F))
						.module(Module.jitter(0.003F)).gravity(-0.001F).life(70, 130).size(0.2F, 0.3F).sprite(FxSprites.FLARE).color(CHAFF)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("column").burst(24).shape(Shape.column(1.5F, 60)).life(10, 20).size(3, 4).sprite(FxSprites.GLOW)
						.color(0xA0E8FF).alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// An abduction: the plug of ground rising inside the shaft, rings of light climbing it, sparkles streaming up.
		FxEngine.register(EffectDef.builder(VfxIds.ABDUCT_LIFT).lod(160, 1200).maxAge(160)
				.emit(EmitterDef.builder("rings").rate(0.6F, 100).mode(Mode.FLAT).fixedRotation().size(9, 9).velocity(Velocity.up(0.6F, 0.6F, 0))
						.life(60, 60).sprite(FxSprites.RING).color(BEAM).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("clods").rate(4, 100).shape(Shape.disc(3.6F)).velocity(Velocity.up(0.3F, 0.5F, 0.05F)).module(Module.jitter(0.004F))
						.life(60, 100).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(EARTH).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("sparkle").rate(6, 100).span().shape(Shape.disc(3.5F)).velocity(Velocity.up(0.2F, 0.4F, 0)).life(20, 40)
						.size(0.1F, 0.16F).sprite(FxSprites.FLARE).color(0xE0F8FF).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("core").rate(2, 100).span().life(10, 16).size(4, 5).sprite(FxSprites.GLOW).color(0x80D0FF).alpha(0.2F)
						.alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ Resonance Cascade

	private static void resonance() {
		// The fork ringing: sound rings rolling off the tines and crystal glints buzzing in the air around them.
		FxEngine.register(EffectDef.builder(VfxIds.FORK_HUM).lod(96, 700)
				.emit(EmitterDef.builder("rings").burst(2).shape(Shape.sphere(1)).life(18, 18).size(3, 3).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 0.3F, 1, 3)).color(0x90F0FF).alpha(0.55F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glints").burst(8).shape(Shape.sphere(3)).module(Module.jitter(0.02F)).life(8, 14).size(0.1F, 0.16F)
						.sprite(FxSprites.FLARE).color(CRYSTAL).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("shimmer").burst(3).shape(Shape.sphere(2)).life(6, 6).size(1.4F, 1.8F).sprite(FxSprites.GLOW).color(0x90F0FF)
						.alpha(0.12F).alphaOverLife(FLICKER).build())
				.build());

		// The ground vibrating like a speaker cone: grains of dirt hopping and bouncing everywhere it reaches.
		FxEngine.register(EffectDef.builder(VfxIds.DUST_DANCE).lod(96, 700)
				.emit(EmitterDef.builder("grains").priority(Priority.DETAIL).rate(60, 8).onGround().shape(Shape.disc(80))
						.velocity(Velocity.up(0.12F, 0.28F, 0.1F)).gravity(0.04F).collide(0.6F, 0.8F, 4).life(20, 40).size(0.14F, 0.22F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x8A7A62).build())
				.build());

		// The collapse front sweeping outward: dust billowing up and glass bursting out of every window it reaches.
		FxEngine.register(EffectDef.builder(VfxIds.CASCADE_FRONT).lod(160, 1200).maxAge(360)
				.emit(smoke("dust").priority(Priority.HERO).rate(10, 240).onGround().shape(Shape.ring(3, 2)).expand(0.396F)
						.velocity(Velocity.up(0.08F, 0.2F, 0.4F)).drag(0.02F).life(60, 100).size(3, 5).sizeOverLife(Curve.keys(0, 0.5F, 1, 1.8F))
						.color(Gradient.of(0, 0x8A8070, 1, 0xA89E90)).alpha(0.6F).build())
				.emit(EmitterDef.builder("glass").rate(6, 240).onGround().shape(Shape.ring(3, 2)).expand(0.396F).velocity(Velocity.up(0.3F, 0.6F, 0.6F))
						.gravity(0.05F).collide(0.3F, 0.6F, 2).life(20, 40).size(0.08F, 0.12F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE)
						.color(CRYSTAL).alphaOverLife(TWINKLE).build())
				.build());

		// The fork shatters: crystal shards flying, one last ring of sound, and glass dust glittering down for a long time.
		FxEngine.register(EffectDef.builder(VfxIds.FORK_SHATTER).lod(160, 1200).maxAge(260)
				.emit(flash("flash", 6, 5, 0xC0F4FF).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(120).shape(Shape.sphere(4)).velocity(Velocity.random(0.4F, 1.1F))
						.gravity(0.04F).collide(0.4F, 0.6F, 2).life(40, 80).size(0.14F, 0.22F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(CRYSTAL)
						.build())
				.emit(EmitterDef.builder("rings").burst(0, 1).burst(4, 1).burst(8, 1).life(30, 30).size(12, 12).sprite(FxSprites.RING)
						.sizeOverLife(Curve.keys(0, 0.2F, 1, 4)).color(0x90F0FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glitter").rate(6, 160).offset(0, 25, 0).shape(Shape.disc(50)).gravity(0.002F).module(Module.jitter(0.002F))
						.life(80, 140).size(0.18F, 0.28F).sprite(FxSprites.FLARE).color(CRYSTAL).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Tesla Dominion

	private static void tesla() {
		// A strike earths: blue-white flash, sparks and molten beads, clods thrown out of the furrow, a smell of ozone.
		FxEngine.register(EffectDef.builder(VfxIds.TESLA_HIT).lod(128, 900)
				.emit(flash("flash", 3.5F, 4, 0xC8E4FF).build())
				.emit(groundRing("ring", 6, 10, 0xA0C8FF, 0.8F))
				.emit(sparkShower("sparks", 30, 0.3F, 0.7F, VOLT, null))
				.emit(EmitterDef.builder("beads").burst(10).velocity(Velocity.up(0.2F, 0.5F, 0.7F)).gravity(0.05F).collide(0.3F, 0.6F, 2).life(20, 40)
						.size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(EMBER).build())
				.emit(EmitterDef.builder("clods").burst(12).velocity(Velocity.up(0.3F, 0.6F, 0.6F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 50)
						.size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A3A2A).spin(0.1F, 0.3F).build())
				.emit(smoke("scorch").burst(4).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(40, 70).size(0.8F, 1.2F).color(SOOT).alpha(0.6F)
						.module(Module.buoyancy(0.006F)).build())
				.emit(EmitterDef.builder("ozone").burst(3).shape(Shape.sphere(1.5F)).life(25, 40).size(2.5F, 3.5F).sprite(FxSprites.GLOW).color(0x80B8FF)
						.alpha(0.25F).alphaOverLife(Curve.BELL).build())
				.build());

		// St. Elmo's fire crawling round the crown torus.
		FxEngine.register(EffectDef.builder(VfxIds.TESLA_CORONA).lod(128, 900)
				.emit(EmitterDef.builder("wisps").burst(3).shape(Shape.ring(6.5F, 1)).velocity(Velocity.up(0.05F, 0.1F, 0.4F)).module(Module.jitter(0.01F))
						.life(10, 18).size(0.6F, 0.9F).sprite(FxSprites.WISP).color(0xA8B8FF).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("crackle").burst(4).shape(Shape.ring(6.5F, 0.5F)).velocity(Velocity.random(0.1F, 0.25F)).life(4, 8)
						.size(0.05F, 0.08F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(0xFFFFFF).build())
				.emit(EmitterDef.builder("glow").burst(1).size(9, 9).life(6, 6).sprite(FxSprites.GLOW).color(0x90B8FF).alpha(0.15F)
						.alphaOverLife(FLICKER).build())
				.build());

		// The ball lightning feeding: sparks pulled in from the air, crackling into the orb.
		FxEngine.register(EffectDef.builder(VfxIds.BALL_FEED).lod(160, 1200)
				.emit(EmitterDef.builder("inrush").burst(10).shape(Shape.shell(14)).module(Module.attract(0.12F, 2)).life(10, 20).size(0.08F, 0.12F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(VOLT).build())
				.emit(EmitterDef.builder("pulse").burst(1).size(6, 6).life(4, 4).sprite(FxSprites.GLOW).color(0xD0E8FF).alpha(0.3F).alphaOverLife(FLICKER)
						.build())
				.build());

		// The discharge: a blue nova off the crown, a ground ring the size of the town, and the air left glowing.
		FxEngine.register(EffectDef.builder(VfxIds.TESLA_NOVA).lod(256, 2000).maxAge(320)
				.emit(flash("flash", 20, 7, 0xD0E8FF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("nova").priority(Priority.HERO).burst(200).velocity(Velocity.random(1.0F, 2.2F)).drag(0.06F).life(14, 26)
						.size(0.18F, 0.26F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(VOLT).build())
				.emit(EmitterDef.builder("shell").burst(1).size(30, 30).life(14, 14).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).color(0xA0D0FF)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ozone").rate(4, 200).offset(0, -60, 0).shape(Shape.disc(100)).velocity(Velocity.up(0.01F, 0.03F, 0.4F))
						.module(Module.jitter(0.003F)).life(100, 160).size(0.6F, 0.9F).sprite(FxSprites.GLOW).color(0x7AB0FF).alpha(0.45F)
						.alphaOverLife(Curve.BELL).build())
				.build());

		// The tower crashing down along its length: dust, snapped copper sparking.
		FxEngine.register(EffectDef.builder(VfxIds.TOWER_DUST).lod(128, 900)
				.emit(smoke("dust").burst(8).shape(Shape.dome(3)).velocity(Velocity.radial(0.15F, 0.3F)).velocity(Velocity.up(0.05F, 0.15F, 0.4F))
						.drag(0.04F).life(60, 100).size(2.5F, 4).color(EARTH).alpha(0.7F).build())
				.emit(EmitterDef.builder("copper").burst(10).velocity(Velocity.up(0.3F, 0.6F, 0.8F)).gravity(0.05F).collide(0.3F, 0.6F, 2).life(20, 40)
						.size(0.06F, 0.1F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(FIRE).build())
				.emit(groundRing("ring", 9, 14, 0x8A8A90, 0.6F))
				.build());
	}

	// ------------------------------------------------------------------ Gravity Killswitch

	private static void gravity() {
		// The generator humming up: violet motes drawn into it and pulses of light.
		FxEngine.register(EffectDef.builder(VfxIds.GRAVITY_HUM).lod(128, 900)
				.emit(EmitterDef.builder("inrush").burst(16).shape(Shape.shell(14)).module(Module.attract(0.08F, 1.5F)).life(15, 30).size(0.08F, 0.12F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(VIOLET).build())
				.emit(EmitterDef.builder("pulse").burst(1).size(4, 4).life(10, 10).sprite(FxSprites.RING).sizeOverLife(Curve.keys(0, 1, 1, 2))
						.color(0xB0A0FF).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// Gravity goes. A silent violet bubble races out, and everything loose starts to drift up: dust, pebbles,
		// beads of water. 250 ticks later (the slam) gravity comes back many times over and all of it comes down at once.
		int slam = 250;
		FxEngine.register(EffectDef.builder(VfxIds.GRAVITY_FLOAT).lod(256, 2000).maxAge(slam + 120)
				.emit(EmitterDef.builder("bubble").priority(Priority.HERO).burst(140).shape(Shape.shell(4)).velocity(Velocity.radial(1.2F, 1.3F))
						.drag(0.03F).life(40, 50).size(2, 2.6F).sprite(FxSprites.GLOW).color(0xA090FF).alpha(0.3F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("dust").priority(Priority.DETAIL).rate(16, 240).onGround().shape(Shape.disc(130))
						.velocity(Velocity.up(0.02F, 0.06F, 0.3F)).drag(0.004F).module(Module.jitter(0.002F)).module(Module.gravityAt(slam, 0.3F))
						.collide(0.0F, 0.9F, 1).life(slam + 30, slam + 60).size(0.1F, 0.2F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(EARTH)
						.spin(0.01F, 0.05F).build())
				.emit(EmitterDef.builder("pebbles").rate(4, 240).onGround().shape(Shape.disc(130)).velocity(Velocity.up(0.03F, 0.08F, 0.3F))
						.drag(0.004F).module(Module.jitter(0.002F)).module(Module.gravityAt(slam, 0.3F)).collide(0.0F, 0.9F, 1)
						.life(slam + 30, slam + 60).size(0.25F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x6A6460)
						.spin(0.01F, 0.04F).build())
				.emit(EmitterDef.builder("water").rate(4, 240).onGround().shape(Shape.disc(130)).velocity(Velocity.up(0.02F, 0.05F, 0.3F))
						.drag(0.004F).module(Module.jitter(0.002F)).module(Module.gravityAt(slam, 0.3F)).collide(0.0F, 0.9F, 1)
						.life(slam + 30, slam + 60).size(0.07F, 0.1F).sprite(FxSprites.FLARE).color(0xC8E0FF).alphaOverLife(TWINKLE).build())
				.emit(smoke("haze").rate(0.5F, 150).offset(0, 20, 0).shape(Shape.disc(120)).life(80, 100).size(18, 26).color(0xA898D8)
						.alpha(0.07F).emissive(Curve.constant(0.5F)).build())
				.build());

		// The slam: a ring of dust running out across the whole disc and the dust carpet blown up off the ground.
		FxEngine.register(EffectDef.builder(VfxIds.GRAVITY_SLAM).lod(300, 2400).maxAge(240)
				.emit(flash("flash", 10, 5, 0xE0D8FF).build())
				.emit(groundRing("shock", 260, 40, 0xD8D0C8, 0.6F))
				.emit(smoke("carpet").priority(Priority.HERO).rate(60, 10).onGround().shape(Shape.disc(130)).velocity(Velocity.up(0.3F, 0.8F, 0.3F))
						.gravity(0.02F).drag(0.02F).life(60, 120).size(3, 6).sizeOverLife(Curve.keys(0, 0.5F, 1, 1.8F)).color(EARTH).alpha(0.7F).build())
				.emit(smoke("wall").rate(40, 30).onGround().shape(Shape.ring(10, 4)).expand(4.0F).velocity(Velocity.up(0.1F, 0.3F, 0.2F))
						.life(50, 90).size(4, 7).color(EARTH).alpha(0.75F).build())
				.emit(EmitterDef.builder("debris").burst(100).onGround().shape(Shape.disc(40)).velocity(Velocity.up(0.4F, 0.9F, 0.5F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 2).life(40, 80).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x5A5048)
						.spin(0.1F, 0.3F).build())
				.build());
	}
}
