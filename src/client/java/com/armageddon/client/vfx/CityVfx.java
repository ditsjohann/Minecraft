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
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.WHITE_FADE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The City Destruction bombs. Each already has its set piece (the moon, the worm, Surtr's sword, the dark sphere, the
 * termite hill, the world tree, the stage); these are the air around it.
 * <ul>
 * <li>Moonfall, cold and silent: tidal dust lifting, regolith that hangs, a landing with no fire, only a grey wall,
 * cold fog and frost; then the geode's violet.</li>
 * <li>World Eater, cosmic hunger: a heartbeat with souls in it, the earth heaving over the worm, stars shed from its
 * skin, a maw that drags everything down, and the souls let go when it bites shut.</li>
 * <li>Ragnarok, Norse doom: ash and embers the whole time, quakes, lava bombs, Surtr's fire, and a land split into
 * rising embers on one side and falling snow on the other.</li>
 * <li>Final Existence, the void: rain and dust falling up, an accretion disc, a collapse and a cosmic nova, and
 * stardust drifting down the bottomless pit forever.</li>
 * <li>Termite Domain, swarming earth: dirt spurts, skittering specks, a glinting winged swarm, mud and the cave-in.</li>
 * <li>Eternal Sapling, wonder: sparkling sprouts, leaf and petal bursts, fireflies spiralling up the world tree,
 * spores where the roots land and petals over the whole land.</li>
 * <li>Main Stage, showbiz: lamp sparks, stage haze in the spotlights, scorch, confetti and a gold glitter finale.</li>
 * </ul>
 */
final class CityVfx {
	private CityVfx() {
	}

	static final Gradient REGOLITH = Gradient.of(0, 0x9A9CA4, 1, 0xB8BAC2);
	static final Gradient MOONDUST = Gradient.of(0, 0xD8DCE8, 1, argb(0xA8B0C0, 0));
	static final Gradient AMETHYST = fade(0xE0B8FF, 0x7030B0);
	static final Gradient SOUL = fade(0x60F0E0, 0x106060);
	static final Gradient VOID = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC090FF, 0.7F, 0x40F0E0, 1, argb(0x200840, 0));
	static final Gradient DIRT = Gradient.of(0, 0x5A4430, 1, 0x7A6450);
	static final Gradient ASH = Gradient.of(0, 0x3A3430, 1, 0x6A6460);
	static final Gradient FROST = Gradient.of(0, 0xFFFFFF, 0.5F, 0xD8F0FF, 1, argb(0xA0D0F0, 0));
	static final Gradient PLASMA = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFB0F0, 0.7F, 0xB060FF, 1, argb(0x401060, 0));
	static final Gradient STARDUST = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0D0FF, 1, argb(0x8070C0, 0));
	static final Gradient MUD = Gradient.of(0, 0x4A3420, 1, 0x6A5030);
	static final Gradient LEAVES = Gradient.of(0, 0x50A040, 0.5F, 0x70C050, 1, 0x4A8A30);
	static final Gradient PETAL = Gradient.of(0, 0xFFD0E4, 0.5F, 0xF4A8C8, 1, 0xFFE8F0);
	static final Gradient FIREFLY = fade(0xE8FF90, 0x80C030);
	static final Gradient GOLD = fade(0xFFE880, 0xC09020);

	static void register() {
		moonfall();
		worldEater();
		ragnarok();
		finalExistence();
		termites();
		sapling();
		mainStage();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	// ------------------------------------------------------------------ Moonfall

	private static void moonfall() {
		// The tide: silver dust and beads of water lifting off the land toward the approaching moon.
		FxEngine.register(EffectDef.builder(VfxIds.MOON_TIDE).lod(200, 1600).maxAge(160)
				.emit(EmitterDef.builder("dust").priority(Priority.DETAIL).rate(24, 20).onGround().shape(Shape.disc(70))
						.velocity(Velocity.up(0.04F, 0.12F, 0.2F)).drag(0.005F).module(Module.jitter(0.002F)).life(80, 140).size(0.25F, 0.4F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(REGOLITH).spin(0.01F, 0.04F).build())
				.emit(EmitterDef.builder("beads").rate(6, 20).onGround().shape(Shape.disc(70)).velocity(Velocity.up(0.05F, 0.1F, 0.2F))
						.module(Module.jitter(0.002F)).life(80, 140).size(0.18F, 0.26F).sprite(FxSprites.FLARE).color(MOONDUST).alphaOverLife(TWINKLE)
						.build())
				.build());

		// Moon rock hitting: regolith thrown up that hangs in the air far too long, cold and grey.
		FxEngine.register(EffectDef.builder(VfxIds.MOON_POCK).lod(128, 900)
				.emit(smoke("regolith").burst(14).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.15F, 0.35F)).velocity(Velocity.up(0.1F, 0.25F, 0.4F))
						.gravity(0.002F).drag(0.03F).life(100, 180).size(1.6F, 2.6F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.8F)).color(REGOLITH)
						.alpha(0.7F).shade(0.6F).build())
				.emit(EmitterDef.builder("rocks").burst(16).velocity(Velocity.up(0.3F, 0.6F, 0.7F)).gravity(0.03F).collide(0.4F, 0.6F, 2).life(40, 70)
						.size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x6A6C74).spin(0.05F, 0.2F).build())
				.emit(groundRing("ring", 10, 18, 0xC0C4D0, 0.5F))
				.build());

		// The landing, with no fire: a regolith wall running out, an ejecta curtain, cold fog rolling low, frost.
		FxEngine.register(EffectDef.builder(VfxIds.MOON_LANDING).lod(300, 2400).maxAge(500)
				.emit(smoke("wall").priority(Priority.HERO).rate(40, 50, Curve.FALL).onGround().shape(Shape.ring(30, 6)).expand(2.5F)
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).drag(0.02F).gravity(0.002F).life(120, 200).size(6, 10)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 1.8F)).color(REGOLITH).alpha(0.8F).shade(0.55F).build())
				.emit(EmitterDef.builder("ejecta").priority(Priority.HERO).burst(220).onGround().shape(Shape.ring(30, 8))
						.velocity(Velocity.up(0.8F, 1.5F, 0.5F)).velocity(Velocity.radial(0.4F, 0.8F)).gravity(0.025F).collide(0.3F, 0.6F, 1)
						.life(80, 140).size(0.4F, 0.7F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(0x8A8C94).build())
				.emit(10, smoke("fog").rate(16, 120).onGround().shape(Shape.ring(34, 10)).expand(0.9F).velocity(Velocity.radial(0.05F, 0.12F))
						.life(180, 260).size(8, 12).sizeOverLife(Curve.keys(0, 0.5F, 1, 1.6F)).color(0xD8E4F0).alpha(0.4F).shade(0.9F).build())
				.emit(30, EmitterDef.builder("frost").rate(12, 220).offset(0, 40, 0).shape(Shape.disc(90)).gravity(0.002F).module(Module.jitter(0.002F))
						.life(120, 200).size(0.1F, 0.16F).sprite(FxSprites.FLARE).color(FROST).alphaOverLife(TWINKLE).build())
				.build());

		// It keeps settling: dust slumping down off its flanks.
		FxEngine.register(EffectDef.builder(VfxIds.MOON_SETTLE).lod(200, 1600)
				.emit(smoke("slump").burst(24).onGround().shape(Shape.ring(36, 4)).velocity(Velocity.radial(0.06F, 0.14F)).life(80, 140).size(4, 6)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 1.6F)).color(REGOLITH).alpha(0.6F).build())
				.build());

		// The geode bursts: amethyst flung everywhere, violet light climbing out of the wound.
		FxEngine.register(EffectDef.builder(VfxIds.MOON_GEODE).lod(300, 2400).maxAge(260)
				.emit(flash("flash", 14, 6, 0xC090FF).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(180).shape(Shape.sphere(8)).velocity(Velocity.up(0.6F, 1.6F, 0.9F))
						.gravity(0.03F).collide(0.4F, 0.6F, 2).life(60, 110).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(AMETHYST)
						.build())
				.emit(EmitterDef.builder("motes").rate(8, 160).shape(Shape.sphere(12)).velocity(Velocity.up(0.1F, 0.3F, 0.3F)).module(Module.jitter(0.003F))
						.life(80, 140).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(AMETHYST).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").burst(1).size(40, 40).life(60, 60).sprite(FxSprites.GLOW).color(0xB070FF).alpha(0.4F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ World Eater

	private static void worldEater() {
		// A heartbeat under the city: the ground thumps and souls seep out of it.
		FxEngine.register(EffectDef.builder(VfxIds.EATER_PULSE).lod(200, 1600)
				.emit(groundRing("thump", 40, 16, 0x6A5A78, 0.4F))
				.emit(smoke("dust").burst(20).onGround().shape(Shape.ring(14, 8)).velocity(Velocity.up(0.05F, 0.15F, 0.3F)).life(40, 70).size(2, 3)
						.color(DIRT).alpha(0.5F).build())
				.emit(EmitterDef.builder("souls").burst(8).onGround().shape(Shape.disc(30)).velocity(Velocity.up(0.1F, 0.2F, 0.2F))
						.module(Module.curl(6, 0.01F)).life(50, 90).size(0.6F, 0.9F).sprite(FxSprites.WISP).color(SOUL).alpha(0.7F)
						.alphaOverLife(Curve.BELL).build())
				.build());

		// Over the worm while it tunnels: the earth heaves, splits and spurts.
		FxEngine.register(EffectDef.builder(VfxIds.WORM_HEAVE).lod(200, 1600)
				.emit(EmitterDef.builder("clods").burst(10).onGround().shape(Shape.disc(8)).velocity(Velocity.up(0.3F, 0.7F, 0.4F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 1).life(30, 50).size(0.3F, 0.5F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DIRT)
						.spin(0.1F, 0.3F).build())
				.emit(smoke("dust").burst(4).onGround().shape(Shape.disc(9)).velocity(Velocity.up(0.08F, 0.2F, 0.3F)).life(50, 80).size(3, 4.5F)
						.color(DIRT).alpha(0.6F).build())
				.build());

		// When it is out in the air its night-sky skin sheds stars and void.
		FxEngine.register(EffectDef.builder(VfxIds.WORM_STARS).lod(200, 1600)
				.emit(EmitterDef.builder("stars").burst(22).shape(Shape.sphere(9)).velocity(Velocity.random(0.02F, 0.08F)).module(Module.jitter(0.004F))
						.life(40, 80).size(0.35F, 0.55F).sprite(FxSprites.FLARE).color(VOID).alphaOverLife(TWINKLE).build())
				.emit(smoke("void").burst(1.5F).shape(Shape.sphere(8)).velocity(Velocity.random(0.02F, 0.05F)).life(30, 50).size(2, 3)
						.color(0x2A1440).alpha(0.35F).emissive(Curve.constant(0.4F)).shade(1).build())
				.build());

		// It breaches: earth and rock thrown sky-high and a spray of the void inside it.
		FxEngine.register(EffectDef.builder(VfxIds.WORM_BREACH).lod(256, 2000).maxAge(240)
				.emit(EmitterDef.builder("rock").priority(Priority.HERO).burst(90).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.9F, 1.7F, 0.5F))
						.gravity(0.04F).collide(0.3F, 0.6F, 1).life(60, 100).size(0.4F, 0.8F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DIRT)
						.spin(0.05F, 0.2F).build())
				.emit(smoke("column").priority(Priority.HERO).burst(40).shape(Shape.column(6, 20)).velocity(Velocity.up(0.3F, 0.7F, 0.3F)).drag(0.03F)
						.life(90, 150).size(5, 8).sizeOverLife(Curve.keys(0, 0.5F, 1, 1.8F)).color(DIRT).alpha(0.75F).build())
				.emit(EmitterDef.builder("void").burst(60).shape(Shape.sphere(6)).velocity(Velocity.up(0.5F, 1.2F, 0.6F)).gravity(0.02F).life(30, 60)
						.size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(VOID).build())
				.emit(groundRing("ring", 50, 30, 0x8A7A70, 0.6F))
				.build());

		// The maw open under the city: everything spiralling down into it, souls dragged in, a purple-black haze.
		FxEngine.register(EffectDef.builder(VfxIds.MAW_VORTEX).lod(256, 2000)
				.emit(smoke("debris").burst(14).onGround().shape(Shape.ring(40, 30)).velocity(Velocity.swirl(0.4F, 0.7F)).module(Module.vortex(0.04F, 0.05F))
						.gravity(0.01F).life(60, 100).size(2.5F, 4).color(DIRT).alpha(0.6F).build())
				.emit(EmitterDef.builder("souls").burst(10).onGround().shape(Shape.ring(45, 25)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.module(Module.attract(0.06F, 4)).life(60, 120).size(0.6F, 0.9F).sprite(FxSprites.WISP).color(SOUL).alpha(0.7F)
						.alphaOverLife(Curve.BELL).build())
				.emit(smoke("haze").burst(2).shape(Shape.disc(30)).velocity(Velocity.swirl(0.2F, 0.3F)).module(Module.vortex(0.02F, 0.02F)).life(80, 120)
						.size(10, 14).color(0x2A0A3A).alpha(0.4F).emissive(Curve.constant(0.4F)).build())
				.build());

		// It bites shut, and everything it swallowed lets go: a void blast, and the souls rising free.
		FxEngine.register(EffectDef.builder(VfxIds.MAW_SNAP).lod(300, 2400).maxAge(400)
				.emit(flash("flash", 20, 6, 0x9060FF).build())
				.emit(EmitterDef.builder("void").priority(Priority.HERO).burst(160).shape(Shape.sphere(10)).velocity(Velocity.random(0.8F, 1.8F))
						.drag(0.05F).life(20, 40).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(VOID).build())
				.emit(EmitterDef.builder("souls").priority(Priority.HERO).rate(10, 240, Curve.FALL).onGround().shape(Shape.disc(80))
						.velocity(Velocity.up(0.15F, 0.35F, 0.2F)).module(Module.curl(10, 0.01F)).life(100, 180).size(0.7F, 1.1F).sprite(FxSprites.WISP)
						.color(SOUL).alpha(0.75F).alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ Ragnarok

	private static void ragnarok() {
		// The end of days: ash and embers coming down over the whole land, all the time.
		FxEngine.register(EffectDef.builder(VfxIds.RAGNAROK_ASH).lod(300, 2400).maxAge(160)
				.emit(EmitterDef.builder("ash").rate(14, 40).offset(0, 50, 0).shape(Shape.disc(100)).gravity(0.003F).module(Module.jitter(0.004F))
						.module(Module.wind(1.5F)).life(140, 220).size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(ASH)
						.spin(0.05F, 0.15F).build())
				.emit(EmitterDef.builder("embers").rate(6, 40).offset(0, 50, 0).shape(Shape.disc(100)).gravity(0.002F).module(Module.jitter(0.004F))
						.module(Module.wind(1.5F)).life(120, 200).size(0.12F, 0.18F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(TWINKLE).build())
				.build());

		// An earthquake splits the ground: dust geysers out of the fissures.
		FxEngine.register(EffectDef.builder(VfxIds.QUAKE_GEYSER).lod(200, 1600)
				.emit(smoke("geyser").burst(16).shape(Shape.column(2, 4)).velocity(Velocity.up(0.4F, 0.8F, 0.2F)).drag(0.03F).life(60, 100).size(3, 5)
						.color(DIRT).alpha(0.7F).build())
				.emit(EmitterDef.builder("rocks").burst(20).velocity(Velocity.up(0.5F, 0.9F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(30, 60)
						.size(0.25F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x4A3A2A).spin(0.05F, 0.2F).build())
				.build());

		// A volcanic vent bursts out of the fields: a fountain of fire and an ash plume.
		EmitterDef lavaTrail = EmitterDef.builder("lava_trail").priority(Priority.DETAIL).burst(1).life(10, 18).size(0.3F, 0.4F)
				.sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build();
		FxEngine.register(EffectDef.builder(VfxIds.VENT_BURST).lod(200, 1600).maxAge(200)
				.emit(flash("flash", 6, 5, 0xFF8030).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(30).shape(Shape.disc(3)).velocity(Velocity.up(0.5F, 1.0F, 0.2F))
						.module(Module.buoyancy(0.02F)).life(20, 40).size(2.5F, 4).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("bombs").burst(16).velocity(Velocity.up(0.7F, 1.3F, 0.5F)).gravity(0.04F).collide(0.2F, 0.6F, 1).life(40, 80)
						.size(0.4F, 0.6F).sprite(FxSprites.GLOW).color(0xFF7020).every(2, lavaTrail).build())
				.emit(smoke("plume").rate(4, 120).shape(Shape.disc(3)).velocity(Velocity.up(0.4F, 0.6F, 0.15F)).drag(0.02F).life(120, 200).size(4, 7)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 2.4F)).color(SOOT).alpha(0.8F).emissive(INNER_GLOW).module(Module.wind(1.5F)).build())
				.build());
		// And keeps spitting: a lava bomb or two, a breath of smoke.
		FxEngine.register(EffectDef.builder(VfxIds.VENT_SPIT).lod(160, 1200)
				.emit(EmitterDef.builder("bombs").burst(3).velocity(Velocity.up(0.5F, 1.0F, 0.5F)).gravity(0.04F).collide(0.2F, 0.6F, 1).life(30, 60)
						.size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(0xFF7020).every(2, lavaTrail).build())
				.emit(EmitterDef.builder("fire").burst(4).shape(Shape.disc(2)).velocity(Velocity.up(0.2F, 0.4F, 0.2F)).life(12, 22).size(1.5F, 2.4F)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("smoke").burst(3).velocity(Velocity.up(0.2F, 0.35F, 0.2F)).life(60, 100).size(2.5F, 4).color(SOOT).alpha(0.7F)
						.module(Module.wind(1.5F)).build())
				.build());

		// Surtr's sword strikes: a fire nova, steam bursting where fire meets frost, ice shards flying.
		FxEngine.register(EffectDef.builder(VfxIds.SURTR_STRIKE).lod(300, 2400).maxAge(300)
				.emit(flash("flash", 26, 7, 0xFFB060).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(60).shape(Shape.dome(8)).velocity(Velocity.radial(0.4F, 0.9F))
						.module(Module.buoyancy(0.02F)).drag(0.06F).life(30, 60).size(5, 8).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("steam").priority(Priority.HERO).burst(50).shape(Shape.dome(10)).velocity(Velocity.radial(0.3F, 0.6F))
						.velocity(Velocity.up(0.1F, 0.3F, 0.4F)).drag(0.03F).life(120, 200).size(7, 11).color(Gradient.of(0, 0xFFFFFF, 1, 0xC8C8CC))
						.alpha(0.7F).shade(0.85F).build())
				.emit(EmitterDef.builder("ice").burst(80).shape(Shape.sphere(6)).velocity(Velocity.random(0.6F, 1.4F)).gravity(0.03F).collide(0.4F, 0.6F, 2)
						.life(40, 80).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(FROST).build())
				.emit(sparkShower("sparks", 120, 0.8F, 1.6F, SPARK, null))
				.build());

		// Surtr walking the chasm: fire bursting up out of the wound.
		FxEngine.register(EffectDef.builder(VfxIds.FIRE_GEYSER).lod(200, 1600)
				.emit(EmitterDef.builder("fire").burst(20).shape(Shape.disc(4)).velocity(Velocity.up(0.6F, 1.2F, 0.15F)).module(Module.buoyancy(0.02F))
						.life(20, 35).size(2.5F, 4).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(20).velocity(Velocity.up(0.4F, 0.9F, 0.4F)).gravity(0.01F).life(30, 60).size(0.12F, 0.18F)
						.sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(TWINKLE).build())
				.build());

		// The two halves: embers and heat rising on the fire side, snow and frost falling on the frost side.
		FxEngine.register(EffectDef.builder(VfxIds.FIRE_SIDE).lod(200, 1600).maxAge(80)
				.emit(EmitterDef.builder("embers").rate(10, 20).onGround().shape(Shape.disc(45)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.module(Module.jitter(0.004F)).life(50, 90).size(0.15F, 0.22F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(TWINKLE).build())
				.emit(smoke("heat").rate(0.5F, 20).onGround().shape(Shape.disc(45)).velocity(Velocity.up(0.1F, 0.2F, 0.2F)).life(60, 100).size(4, 6)
						.color(0x6A3820).alpha(0.2F).emissive(Curve.constant(0.6F)).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.FROST_SIDE).lod(200, 1600).maxAge(160)
				.emit(EmitterDef.builder("snow").rate(14, 20).offset(0, 30, 0).shape(Shape.disc(45)).gravity(0.003F).module(Module.jitter(0.004F))
						.life(100, 150).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(FROST).build())
				.emit(smoke("mist").rate(1, 20).onGround().shape(Shape.disc(45)).velocity(Velocity.radial(0.02F, 0.06F)).life(80, 120).size(5, 7)
						.color(0xD8E8F4).alpha(0.35F).shade(0.9F).build())
				.build());

		// A burning star landing in the twilight.
		FxEngine.register(EffectDef.builder(VfxIds.STAR_IMPACT).lod(200, 1600)
				.emit(flash("flash", 4, 4, 0xFF9040).build())
				.emit(EmitterDef.builder("fire").burst(14).shape(Shape.dome(1.5F)).velocity(Velocity.radial(0.15F, 0.35F)).module(Module.buoyancy(0.015F))
						.life(15, 30).size(1.5F, 2.4F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(sparkShower("sparks", 30, 0.3F, 0.8F, SPARK, null))
				.emit(groundRing("ring", 12, 16, 0xFF7030, 0.6F))
				.build());

		// A piece of the broken Bifrost lands: glass of its own colour bursting (tinted per piece).
		FxEngine.register(EffectDef.builder(VfxIds.PRISM_SHATTER).tint(0xFF2020).lod(200, 1600)
				.emit(EmitterDef.builder("glass").burst(50).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.4F, 0.6F, 2).life(30, 60)
						.size(0.2F, 0.35F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).tinted().color(WHITE_FADE).build())
				.emit(EmitterDef.builder("glow").burst(1).size(10, 10).life(14, 14).sprite(FxSprites.GLOW).tinted().color(WHITE_FADE).alpha(0.5F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(18, 18).life(24, 24).sprite(FxSprites.SHOCK)
						.tinted().sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(WHITE_FADE).build())
				.emit(EmitterDef.builder("glitter").burst(30).shape(Shape.sphere(4)).velocity(Velocity.up(0.05F, 0.15F, 0.6F)).life(40, 80)
						.size(0.12F, 0.2F).sprite(FxSprites.FLARE).tinted().color(WHITE_FADE).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Final Existence

	private static void finalExistence() {
		// Gravity reversed: rain, dust and pebbles falling upward toward the dark sphere.
		FxEngine.register(EffectDef.builder(VfxIds.INVERT_RAIN).lod(256, 2000).maxAge(120)
				.emit(EmitterDef.builder("rain").rate(30, 20).onGround().shape(Shape.disc(120)).velocity(Velocity.up(0.6F, 1.0F, 0.03F)).life(40, 80)
						.size(0.08F, 0.12F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(Gradient.of(0, 0xD0C8F0, 1, argb(0x8070C0, 0)))
						.alpha(0.6F).build())
				.emit(EmitterDef.builder("pebbles").rate(10, 20).onGround().shape(Shape.disc(120)).velocity(Velocity.up(0.15F, 0.35F, 0.1F))
						.module(Module.jitter(0.003F)).life(60, 100).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DIRT)
						.spin(0.05F, 0.2F).build())
				.build());

		// The black hole's accretion disc: hot plasma swirling in, round and down.
		FxEngine.register(EffectDef.builder(VfxIds.ACCRETION).lod(300, 2400)
				.emit(EmitterDef.builder("plasma").priority(Priority.HERO).burst(24).shape(Shape.ring(34, 16)).velocity(Velocity.swirl(0.9F, 1.4F))
						.module(Module.vortex(0.05F, 0.04F)).life(20, 40).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK)
						.color(PLASMA).build())
				.emit(EmitterDef.builder("glow").burst(4).shape(Shape.ring(30, 12)).velocity(Velocity.swirl(0.6F, 0.9F)).module(Module.vortex(0.04F, 0.03F))
						.life(20, 35).size(6, 9).sprite(FxSprites.GLOW).color(0xFF60FF).alpha(0.25F).alphaOverLife(Curve.BELL).build())
				.build());

		// Overfed, it collapses: everything rushes into one point.
		FxEngine.register(EffectDef.builder(VfxIds.VOID_COLLAPSE).lod(300, 2400).maxAge(40)
				.emit(EmitterDef.builder("inrush").priority(Priority.HERO).rate(60, 24).shape(Shape.shell(90)).module(Module.attract(0.3F, 3)).life(20, 30)
						.size(0.4F, 0.6F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(PLASMA).build())
				.build());

		// And combusts: a cosmic nova, and stardust that keeps drifting down into the pit.
		FxEngine.register(EffectDef.builder(VfxIds.COSMIC_NOVA).lod(400, 3000).maxAge(320)
				.emit(EmitterDef.builder("nova").priority(Priority.HERO).burst(300).velocity(Velocity.random(1.5F, 3.0F)).drag(0.05F).life(20, 40)
						.size(0.4F, 0.6F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(STARDUST).build())
				.emit(EmitterDef.builder("shell").burst(1).size(80, 80).life(20, 20).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT).color(0xF0E0FF)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("stardust").rate(12, 260).shape(Shape.sphere(40)).gravity(0.004F).module(Module.jitter(0.002F)).life(120, 200)
						.size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.build());

		// The bottomless pit: motes drifting down it forever, and a faint violet glow breathing up from below.
		FxEngine.register(EffectDef.builder(VfxIds.ABYSS_MOTES).lod(300, 2400).maxAge(720)
				.emit(EmitterDef.builder("motes").rate(4, 680).offset(0, 10, 0).shape(Shape.disc(30)).gravity(0.004F).module(Module.jitter(0.002F))
						.life(150, 250).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(STARDUST).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("glow").rate(0.5F, 680).offset(0, -30, 0).shape(Shape.disc(22)).velocity(Velocity.up(0.1F, 0.2F, 0.1F))
						.life(120, 180).size(8, 12).sprite(FxSprites.GLOW).color(0x8050C0).alpha(0.15F).alphaOverLife(Curve.BELL).build())
				.build());
	}

	// ------------------------------------------------------------------ Termite Domain

	private static void termites() {
		FxEngine.register(EffectDef.builder(VfxIds.DIRT_SPURT).lod(128, 900)
				.emit(EmitterDef.builder("clods").burst(14).velocity(Velocity.up(0.3F, 0.6F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 1).life(25, 45)
						.size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DIRT).spin(0.1F, 0.3F).build())
				.emit(smoke("dust").burst(3).velocity(Velocity.up(0.05F, 0.12F, 0.4F)).life(40, 60).size(1.2F, 1.8F).color(DIRT).alpha(0.5F).build())
				.build());

		// The swarm on the move: dark specks skittering over the ground, a haze of dust kicked up.
		FxEngine.register(EffectDef.builder(VfxIds.ANT_SKITTER).lod(96, 700)
				.emit(EmitterDef.builder("ants").priority(Priority.DETAIL).burst(30).onGround().shape(Shape.disc(6)).velocity(Velocity.radial(0.05F, 0.15F))
						.velocity(Velocity.up(0.02F, 0.06F, 0.4F)).gravity(0.02F).module(Module.jitter(0.03F)).collide(0.0F, 0.95F, 4).life(20, 40)
						.size(0.12F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x1A1008).shade(1).build())
				.emit(smoke("dust").burst(1).onGround().shape(Shape.disc(5)).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(30, 50).size(1.6F, 2.4F)
						.color(DIRT).alpha(0.25F).build())
				.build());

		// The nuptial flight: a swarm of winged termites, wings glinting, boiling through the air.
		FxEngine.register(EffectDef.builder(VfxIds.TERMITE_SWARM).lod(160, 1200)
				.emit(EmitterDef.builder("wings").burst(24).shape(Shape.sphere(6)).velocity(Velocity.random(0.1F, 0.25F)).module(Module.curl(5, 0.04F))
						.module(Module.jitter(0.02F)).life(40, 80).size(0.1F, 0.14F).sprite(FxSprites.FLARE).color(0xF0E8D0).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("bodies").burst(20).shape(Shape.sphere(6)).velocity(Velocity.random(0.1F, 0.25F)).module(Module.curl(5, 0.04F))
						.module(Module.jitter(0.02F)).life(40, 80).size(0.1F, 0.14F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x2A1A10).shade(1)
						.build())
				.build());

		// Mud flung up the mound as the builders work.
		FxEngine.register(EffectDef.builder(VfxIds.MUD_FLING).lod(160, 1200)
				.emit(EmitterDef.builder("mud").burst(16).shape(Shape.ring(10, 6)).velocity(Velocity.up(0.3F, 0.6F, 0.6F)).gravity(0.05F)
						.collide(0.1F, 0.8F, 1).life(30, 50).size(0.25F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(MUD).spin(0.1F, 0.3F)
						.build())
				.emit(smoke("mist").burst(3).shape(Shape.ring(10, 6)).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).life(40, 70).size(2.5F, 4).color(MUD)
						.alpha(0.4F).build())
				.build());

		// The domain caves in: dust pouring down into the pit and a wall of it rolling out.
		FxEngine.register(EffectDef.builder(VfxIds.TERMITE_CAVE).lod(256, 2000).maxAge(300)
				.emit(smoke("column").priority(Priority.HERO).burst(70).shape(Shape.column(20, 30)).velocity(Velocity.up(0.1F, 0.3F, 0.6F)).drag(0.02F)
						.life(120, 200).size(6, 10).sizeOverLife(Curve.keys(0, 0.5F, 1, 1.6F)).color(DIRT).alpha(0.75F).build())
				.emit(smoke("wall").rate(30, 40).onGround().shape(Shape.ring(30, 6)).expand(1.8F).velocity(Velocity.up(0.1F, 0.25F, 0.3F)).life(80, 140)
						.size(5, 8).color(DIRT).alpha(0.7F).build())
				.emit(EmitterDef.builder("clods").burst(80).onGround().shape(Shape.disc(40)).velocity(Velocity.up(0.4F, 0.9F, 0.5F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 1).life(40, 80).size(0.3F, 0.5F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(MUD).spin(0.1F, 0.3F)
						.build())
				.build());
	}

	// ------------------------------------------------------------------ Eternal Sapling

	private static void sapling() {
		// The sapling stirring: green sparkles and a leaf or two climbing out of it.
		FxEngine.register(EffectDef.builder(VfxIds.SAPLING_SPROUT).lod(48, 300)
				.emit(EmitterDef.builder("sparkle").burst(10).shape(Shape.sphere(0.6F)).velocity(Velocity.up(0.04F, 0.1F, 0.4F)).life(20, 40)
						.size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("leaves").burst(3).velocity(Velocity.up(0.08F, 0.15F, 0.6F)).gravity(0.004F).module(Module.jitter(0.006F))
						.life(30, 50).size(0.1F, 0.15F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(LEAVES).spin(0.1F, 0.3F).build())
				.build());

		// Each growth stage: a burst of leaves and petals, spores rising, grass blown flat around it.
		FxEngine.register(EffectDef.builder(VfxIds.TREE_STAGE).lod(200, 1600)
				.emit(EmitterDef.builder("leaves").burst(50).shape(Shape.sphere(6)).velocity(Velocity.random(0.15F, 0.4F)).gravity(0.006F)
						.module(Module.jitter(0.008F)).drag(0.03F).life(60, 110).size(0.25F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(LEAVES).spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("petals").burst(25).shape(Shape.sphere(6)).velocity(Velocity.random(0.1F, 0.3F)).gravity(0.004F)
						.module(Module.jitter(0.008F)).drag(0.03F).life(70, 120).size(0.2F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PETAL)
						.spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("spores").burst(30).shape(Shape.sphere(5)).velocity(Velocity.up(0.08F, 0.2F, 0.5F)).module(Module.jitter(0.004F))
						.life(60, 100).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE).build())
				.emit(groundRing("grass", 16, 18, 0x80FF90, 0.5F))
				.build());

		// The world tree growing: fireflies and pollen spiralling up round the trunk.
		FxEngine.register(EffectDef.builder(VfxIds.TREE_FIREFLIES).lod(256, 2000)
				.emit(EmitterDef.builder("fireflies").burst(16).shape(Shape.ring(14, 8)).velocity(Velocity.swirl(0.15F, 0.3F))
						.velocity(Velocity.up(0.15F, 0.35F, 0.1F)).module(Module.vortex(0.01F, 0.005F)).module(Module.jitter(0.004F)).life(80, 140)
						.size(0.25F, 0.4F).sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("pollen").burst(12).shape(Shape.ring(20, 12)).velocity(Velocity.up(0.08F, 0.2F, 0.3F)).module(Module.jitter(0.003F))
						.life(100, 160).size(0.15F, 0.22F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.build());

		// A root slams into the ground: earth flung up, then green spores where the forest will grow.
		FxEngine.register(EffectDef.builder(VfxIds.ROOT_SLAM).lod(200, 1600).maxAge(200)
				.emit(EmitterDef.builder("earth").priority(Priority.HERO).burst(60).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.5F, 1.1F, 0.6F))
						.gravity(0.04F).collide(0.3F, 0.6F, 1).life(40, 80).size(0.3F, 0.55F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DIRT)
						.spin(0.05F, 0.2F).build())
				.emit(smoke("dust").burst(24).onGround().shape(Shape.ring(6, 4)).velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.drag(0.04F).life(60, 110).size(3, 5).color(DIRT).alpha(0.7F).build())
				.emit(20, EmitterDef.builder("spores").rate(10, 60).onGround().shape(Shape.disc(20)).velocity(Velocity.up(0.08F, 0.2F, 0.3F))
						.module(Module.jitter(0.004F)).life(60, 100).size(0.2F, 0.3F).sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE).build())
				.build());

		// The bloom: a burst of pollen off the crown, petals coming down over the whole land, fireflies lingering.
		FxEngine.register(EffectDef.builder(VfxIds.TREE_BLOOM).lod(400, 3000).maxAge(420)
				.emit(EmitterDef.builder("pollen").priority(Priority.HERO).burst(200).shape(Shape.sphere(30)).velocity(Velocity.random(0.3F, 0.8F))
						.drag(0.03F).module(Module.jitter(0.004F)).life(80, 140).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(GOLD)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("petals").priority(Priority.HERO).rate(30, 360).shape(Shape.disc(120)).gravity(0.003F).module(Module.jitter(0.008F))
						.module(Module.wind(1)).life(160, 240).size(0.25F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PETAL)
						.spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("fireflies").rate(6, 360).offset(0, -60, 0).shape(Shape.disc(100)).velocity(Velocity.up(0.02F, 0.06F, 0.5F))
						.module(Module.jitter(0.006F)).life(100, 180).size(0.25F, 0.4F).sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE)
						.build())
				.build());

		// A seed lands and sprouts.
		FxEngine.register(EffectDef.builder(VfxIds.SEED_SPROUT).lod(128, 900)
				.emit(EmitterDef.builder("sparkle").burst(20).shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.25F, 0.4F)).life(30, 60).size(0.15F, 0.25F)
						.sprite(FxSprites.FLARE).color(FIREFLY).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("leaves").burst(14).velocity(Velocity.up(0.15F, 0.35F, 0.7F)).gravity(0.006F).module(Module.jitter(0.006F))
						.life(40, 80).size(0.2F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(LEAVES).spin(0.1F, 0.3F).build())
				.build());
	}

	// ------------------------------------------------------------------ Main Stage

	private static void mainStage() {
		// A studio lamp switching on: CHUNK, a shower of sparks out of the housing, dust shaken off the rig.
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_CHUNK).lod(200, 1600)
				.emit(sparkShower("sparks", 40, 0.2F, 0.6F, SPARK, null))
				.emit(smoke("dust").burst(10).shape(Shape.sphere(3)).velocity(Velocity.up(-0.1F, -0.02F, 0.6F)).life(40, 70).size(1.2F, 2)
						.color(0xB8B0A0).alpha(0.4F).build())
				.emit(EmitterDef.builder("glow").burst(1).size(8, 8).life(8, 8).sprite(FxSprites.GLOW).color(0xFFF0D0).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The loose light crashing down: its lens bursting into glass and sparks.
		FxEngine.register(EffectDef.builder(VfxIds.LAMP_SHATTER).lod(160, 1200)
				.emit(EmitterDef.builder("glass").burst(60).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.4F, 0.6F, 2).life(30, 60)
						.size(0.15F, 0.25F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, 0xFFF0C0)).build())
				.emit(sparkShower("sparks", 50, 0.3F, 0.8F, SPARK, null))
				.build());

		// Stage haze hanging over the whole floor, lit up wherever the spotlights pass through it.
		FxEngine.register(EffectDef.builder(VfxIds.STAGE_HAZE).lod(200, 1600).maxAge(700)
				.emit(EmitterDef.builder("motes").priority(Priority.DETAIL).rate(10, 620).offset(0, 4, 0).shape(Shape.disc(70)).module(Module.jitter(0.002F))
						.life(120, 200).size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(0xFFF0D0).alpha(0.5F).alphaOverLife(Curve.BELL).build())
				.emit(smoke("haze").rate(0.6F, 620).offset(0, 3, 0).shape(Shape.disc(70)).life(120, 200).size(10, 14).color(0xE8E0D8).alpha(0.12F)
						.shade(0.95F).build())
				.build());

		// Where a spotlight sweeps: scorch smoke and embers.
		FxEngine.register(EffectDef.builder(VfxIds.SPOT_SCORCH).lod(160, 1200)
				.emit(smoke("smoke").burst(2).shape(Shape.disc(1)).velocity(Velocity.up(0.05F, 0.12F, 0.3F)).life(40, 70).size(1.2F, 1.8F).color(SOOT)
						.alpha(0.6F).module(Module.buoyancy(0.005F)).build())
				.emit(EmitterDef.builder("embers").burst(3).velocity(Velocity.up(0.1F, 0.25F, 0.5F)).gravity(0.005F).life(20, 40).size(0.08F, 0.12F)
						.sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(TWINKLE).build())
				.build());

		// Curtain call: confetti cannons, gold glitter and streamers over the stage.
		EffectDef.Builder show = EffectDef.builder(VfxIds.SHOWTIME).lod(256, 2000).maxAge(300)
				.emit(EmitterDef.builder("glitter").burst(120).shape(Shape.ring(14, 4)).velocity(Velocity.up(0.6F, 1.1F, 0.4F)).gravity(0.01F).drag(0.04F)
						.module(Module.jitter(0.004F)).life(100, 180).size(0.35F, 0.5F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("streamers").burst(40).shape(Shape.ring(14, 4)).velocity(Velocity.up(0.8F, 1.3F, 0.4F)).gravity(0.015F).drag(0.03F)
						.life(60, 100).size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(GOLD).build());
		for (int k = 0; k < ThrowVfx2.RAINBOW.length; k++) {
			show.emit(EmitterDef.builder("confetti" + k).burst(45).shape(Shape.ring(14, 4)).velocity(Velocity.up(0.6F, 1.2F, 0.5F)).gravity(0.012F)
					.drag(0.05F).module(Module.jitter(0.01F)).life(120, 200).size(0.4F, 0.55F).spin(0.2F, 0.5F).layer(Layer.SMOKE).sprite(FxSprites.GLOW)
					.color(ThrowVfx2.RAINBOW[k]).emissive(Curve.constant(0.4F)).build());
		}
		FxEngine.register(show.build());

		// The finale: gold glitter raining down over everything.
		FxEngine.register(EffectDef.builder(VfxIds.FINALE_GLITTER).lod(400, 3000).maxAge(420)
				.emit(EmitterDef.builder("glitter").priority(Priority.HERO).rate(30, 300).offset(0, 60, 0).shape(Shape.disc(160)).gravity(0.004F)
						.module(Module.jitter(0.004F)).life(180, 260).size(0.4F, 0.6F).sprite(FxSprites.FLARE).color(GOLD).alphaOverLife(TWINKLE).build())
				.build());
	}
}
