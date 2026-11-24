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
 * Placed bombs, batch four: redrawn from scratch. Nothing here reuses an older effect; the rings, domes, pillars,
 * beams, layered blasts and vanilla particles these bombs used are gone and these stand in their place.
 * <ul>
 * <li>Tsunami: a crest that boils white, a curtain of spray thrown off its lip, foam lace left on the ground behind.</li>
 * <li>Volcano: the cone shrugging off ash as it grows, then a Plinian column with lightning in it, a lava fountain
 * arcing out, and strombolian bursts that throw glowing blobs with trails.</li>
 * <li>Ice Age: rime crystals growing flat over the ground at the front, a low freezing fog rolling out, a blizzard
 * driven round the centre; every spire cracks up out of the ground in a puff of diamond dust.</li>
 * <li>Hellmouth: the rings crack and breathe embers; the pit opens in a black-and-crimson eruption; hellfire geysers
 * roar round the rim and souls spiral up out of the throat; it ends in one vast eruption.</li>
 * <li>Cyclone: a wedge, not a rope: a wide dark wall of spinning dust, a rain curtain wrapped round it, a debris band
 * orbiting halfway up and a churning skirt where it touches down.</li>
 * <li>Doomsday Clock: each hour marked by embers rising in a thread; the hand is a line of sparks cutting the trench;
 * every hour a red bolt of falling streaks and a burst; midnight all twelve and the centre.</li>
 * <li>Big Bang: the primordial burst of mottled plasma, then newborn stars twinkling everywhere and each planet
 * forming in a swirl of dust its own colour.</li>
 * <li>Chrono Break: the erasing shells flicker as teal grains run backwards; when time restores, grains of sand flow
 * back in from every side to rebuild it.</li>
 * <li>Colossal Spear: golden light spiralling down onto the target as it falls; the impact throws stone and draws
 * glowing fracture lines; the drill sprays molten rock and sparks round the shaft; the core answers with a geyser of
 * light and petals.</li>
 * <li>Zombie Apocalypse: each zombie claws out in clods and green rot; giants rise in an eruption of earth, stomp in
 * rings of dust, burst into a rolling toxic cloud with slime and bone; at the end the dead crumble to ash.</li>
 * </ul>
 */
final class BombVfx4 {
	private BombVfx4() {
	}

	// Tsunami
	static final Gradient FOAM = Gradient.of(0, 0xFFFFFF, 1, 0xE0F0F8);
	static final Gradient SEA_SPRAY = Gradient.of(0, 0xFFFFFF, 0.5F, 0xC8E8FF, 1, argb(0x6AA8E0, 0));
	// Volcano
	static final Gradient ASH = Gradient.of(0, 0x4A4440, 0.5F, 0x5A5450, 1, 0x7A746E);
	static final Gradient LAVA = Gradient.of(0, 0xFFF0A0, 0.2F, 0xFFA030, 0.6F, 0xE04010, 1, argb(0x501004, 0));
	static final Gradient CINDER = Gradient.of(0, 0x3A2A24, 1, 0x2A2220);
	// Ice Age
	static final Gradient RIME = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0F6FF, 1, argb(0xB0E0FF, 0));
	static final Gradient FREEZE_FOG = Gradient.of(0, 0xF0FAFF, 1, 0xD0E8F4);
	static final Gradient SNOW = Gradient.of(0, 0xFFFFFF, 1, 0xF0F4F8);
	// Hellmouth
	static final Gradient HELLFIRE = Gradient.of(0, 0xFFA050, 0.2F, 0xFF4010, 0.55F, 0xA00808, 1, argb(0x300202, 0));
	static final Gradient BRIMSTONE = Gradient.of(0, 0x2A1010, 0.5F, 0x1A0A0A, 1, 0x302020);
	static final Gradient SOUL_RED = Gradient.of(0, 0xFFC0A0, 0.4F, 0xFF4030, 1, argb(0x600808, 0));
	// Cyclone
	static final Gradient STORM_DUST = Gradient.of(0, 0x5A5450, 0.5F, 0x6A6660, 1, 0x7A7874);
	static final Gradient STORM_RAIN = Gradient.of(0, 0xC8D0D8, 1, argb(0x8090A0, 0));
	static final Gradient DEBRIS = Gradient.of(0, 0x5A4A38, 0.5F, 0x6A6A6A, 1, 0x4A6A34);
	// Doomsday Clock
	static final Gradient DOOM = Gradient.of(0, 0xFFFFFF, 0.25F, 0xFF8080, 0.6F, 0xFF2020, 1, argb(0x600000, 0));
	// Big Bang
	static final Gradient PRIMORDIAL = Gradient.of(0, 0xFFFFFF, 0.2F, 0xFFD8A0, 0.45F, 0xFF8050, 0.7F, 0x6080FF, 1, argb(0x301060, 0));
	static final Gradient NEWBORN = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0E8FF, 1, argb(0xA0B0FF, 0));
	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	// Chrono Break
	static final Gradient CHRONO = Gradient.of(0, 0xFFFFFF, 0.3F, 0xA0FFE8, 0.7F, 0x50FFD0, 1, argb(0x108070, 0));
	static final Gradient SAND = Gradient.of(0, 0xF0E0B0, 1, 0xD8C890);
	// Colossal Spear
	static final Gradient HEAVEN = Gradient.of(0, 0xFFFFFF, 0.4F, 0xFFF0C0, 1, argb(0xFFC860, 0));
	static final Gradient STONE = Gradient.of(0, 0x8A8680, 1, 0xA8A49C);
	static final Gradient MOLTEN = Gradient.of(0, 0xFFFFE0, 0.2F, 0xFFC050, 0.55F, 0xFF5010, 1, argb(0x501004, 0));
	static final Gradient PETAL = Gradient.of(0, 0xFFFFFF, 0.5F, 0xFFF4E0, 1, 0xFFE8C8);
	// Zombie Apocalypse
	static final Gradient PODZOL = Gradient.of(0, 0x5A3A20, 1, 0x6A4A2A);
	static final Gradient ROT = Gradient.of(0, 0xA0FF70, 0.5F, 0x50A030, 1, argb(0x204010, 0));
	static final Gradient TOXIC = Gradient.of(0, 0x9CC860, 0.5F, 0x6A9040, 1, 0x4A6030);
	static final Gradient SLIME = Gradient.of(0, 0x9CFF6A, 1, 0x60C040);
	static final Gradient BONE = Gradient.of(0, 0xF0ECE0, 1, 0xD8D0C0);
	static final Gradient ZOMBIE_ASH = Gradient.of(0, 0x707A60, 1, 0x8A9078);

	static void register() {
		tsunami();
		volcano();
		iceAge();
		hellmouth();
		cyclone();
		doomsday();
		bigBang();
		chrono();
		spear();
		zombies();
	}

	// ------------------------------------------------------------------ Tsunami

	private static void tsunami() {
		// The crest, authored at radius 20 (played at the front's radius): white water boiling along its top, a
		// curtain of spray off the lip, drops falling back, and foam lace left lying behind.
		FxEngine.register(EffectDef.builder(VfxIds.TSUNAMI_CREST).lod(160, 1000)
				.emit(smoke("boil").burst(24).onGround().shape(Shape.ring(20, 1)).offset(0, 3, 0).velocity(Velocity.radial(0.1F, 0.25F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.3F)).life(16, 26).size(1.4F, 2.2F).color(FOAM).alpha(0.75F).shade(0.9F).build())
				.emit(EmitterDef.builder("curtain").burst(40).onGround().shape(Shape.ring(20, 0.5F)).offset(0, 3.5F, 0).velocity(Velocity.radial(0.2F, 0.4F))
						.velocity(Velocity.up(0.3F, 0.6F, 0.15F)).gravity(0.04F).life(16, 28).size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(SEA_SPRAY).build())
				.emit(smoke("mist").burst(6).onGround().shape(Shape.ring(20, 1)).offset(0, 5, 0).velocity(Velocity.radial(0.1F, 0.2F)).life(30, 50).size(2.5F, 4)
						.color(FOAM).alpha(0.35F).shade(0.95F).build())
				.emit(EmitterDef.builder("lace").burst(16).onGround().shape(Shape.ring(18, 2)).offset(0, 0.15F, 0).mode(Mode.FLAT).fixedRotation().life(30, 50)
						.size(0.6F, 1.1F).layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6).color(FOAM).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ Volcano

	private static void volcano() {
		// The cone growing, authored at radius 10: ash and grit shrugged off its flanks.
		FxEngine.register(EffectDef.builder(VfxIds.VOLCANO_RISE).lod(128, 900)
				.emit(smoke("ash").burst(10).onGround().shape(Shape.ring(10, 2)).velocity(Velocity.radial(0.1F, 0.25F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.life(40, 70).size(2, 3.5F).color(ASH).alpha(0.6F).build())
				.emit(EmitterDef.builder("grit").burst(16).onGround().shape(Shape.ring(10, 1)).velocity(Velocity.radial(0.15F, 0.3F)).velocity(Velocity.up(0.2F, 0.4F, 0.3F))
						.gravity(0.05F).collide(0.3F, 0.6F, 2).life(20, 40).size(0.12F, 0.22F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(CINDER)
						.spin(0.05F, 0.2F).build())
				.build());

		// It opens: a hot breath, a cap of cinders and a ring of ash rolling down the cone.
		FxEngine.register(EffectDef.builder(VfxIds.VOLCANO_OPEN).lod(200, 1400).maxAge(200)
				.cue(0, EffectDef.light(1.2F, 0xFF6020))
				.emit(flash("breath", 12, 8, 0xFFA040).build())
				.emit(EmitterDef.builder("cinders").priority(Priority.HERO).burst(120).shape(Shape.disc(2)).velocity(Velocity.up(0.8F, 1.6F, 0.6F)).gravity(0.04F)
						.collide(0.3F, 0.5F, 1).life(40, 70).size(0.2F, 0.35F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(LAVA).build())
				.emit(smoke("roll").burst(30).shape(Shape.ring(3, 1)).velocity(Velocity.radial(0.3F, 0.5F)).velocity(Velocity.up(-0.1F, 0.05F, 0.2F)).gravity(0.004F)
						.life(80, 120).size(3, 5).color(ASH).alpha(0.7F).build())
				.build());

		// A few ticks of eruption (played every few ticks at the vent): a Plinian column of ash with lightning flickering
		// in it, a lava fountain arcing out, glowing blobs trailing sparks.
		EmitterDef trail = EmitterDef.builder("trail").burst(1).life(6, 12).size(0.15F, 0.25F).sprite(FxSprites.GLOW).color(LAVA)
				.alphaOverLife(Curve.FADE_OUT).build();
		FxEngine.register(EffectDef.builder(VfxIds.VOLCANO_PLUME).lod(256, 1600).maxAge(260)
				.emit(smoke("column").priority(Priority.HERO).burst(3).shape(Shape.disc(2)).offset(0, 2, 0).velocity(Velocity.up(0.5F, 0.8F, 0.1F))
						.module(Module.buoyancy(0.004F)).module(Module.curl(12, 0.006F)).drag(0.01F).life(160, 240).size(4, 6)
						.sizeOverLife(Curve.keys(0, 0.5F, 1, 3.0F)).color(ASH).alpha(0.75F).shade(0.5F).emissive(Curve.keys(0, 0.5F, 0.15F, 0)).build())
				.emit(EmitterDef.builder("lightning").burst(0.25F).shape(Shape.sphere(6)).offset(0, 26, 0).life(2, 4).size(5, 8).sprite(FxSprites.GLOW)
						.color(0xD0C0FF).alpha(0.7F).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("fountain").burst(8).shape(Shape.disc(1.5F)).velocity(Velocity.up(0.6F, 1.2F, 0.5F)).gravity(0.045F)
						.collide(0.1F, 0.4F, 1).life(30, 60).size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(LAVA).every(2, trail).build())
				.build());

		// A strombolian burst: one hard pop of glowing blobs, cinders and a black puff.
		FxEngine.register(EffectDef.builder(VfxIds.VOLCANO_BURST).lod(200, 1400)
				.cue(0, EffectDef.light(0.8F, 0xFF6020))
				.emit(EmitterDef.builder("blobs").priority(Priority.HERO).burst(24).shape(Shape.disc(1.5F)).velocity(Velocity.up(0.9F, 1.6F, 0.7F)).gravity(0.045F)
						.collide(0.1F, 0.4F, 1).life(40, 70).size(0.5F, 0.8F).sprite(FxSprites.GLOW).color(LAVA).every(2, trail).build())
				.emit(smoke("puff").burst(12).shape(Shape.sphere(2)).velocity(Velocity.up(0.3F, 0.6F, 0.6F)).life(60, 100).size(3, 5).color(CINDER).alpha(0.8F).build())
				.build());
	}

	// ------------------------------------------------------------------ Ice Age

	private static void iceAge() {
		// The front, authored at radius 20: rime crystals growing flat over the ground and a low freezing fog rolling out.
		FxEngine.register(EffectDef.builder(VfxIds.ICE_FRONT).lod(160, 1000)
				.emit(EmitterDef.builder("rime").burst(40).onGround().shape(Shape.ring(20, 2)).offset(0, 0.1F, 0).mode(Mode.FLAT).life(40, 70).size(0.3F, 0.6F)
						.sizeOverLife(Curve.keys(0, 0, 0.3F, 1, 1, 1)).sprite(FxSprites.FLARE).color(RIME).alpha(0.8F).build())
				.emit(smoke("fog").burst(10).onGround().shape(Shape.ring(20, 3)).velocity(Velocity.radial(0.06F, 0.15F)).life(70, 110).size(3, 5)
						.color(FREEZE_FOG).alpha(0.45F).shade(0.95F).build())
				.emit(EmitterDef.builder("glints").burst(14).onGround().shape(Shape.ring(20, 3)).offset(0, 0.6F, 0).life(10, 24).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(RIME).alphaOverLife(TWINKLE).build())
				.build());

		// The blizzard over the frozen ground, driven round the centre.
		FxEngine.register(EffectDef.builder(VfxIds.ICE_BLIZZARD).lod(128, 900)
				.emit(EmitterDef.builder("snow").burst(60).shape(Shape.disc(26)).offset(0, 12, 0).velocity(Velocity.swirl(0.3F, 0.5F)).velocity(Velocity.up(-0.15F, -0.08F, 0.2F))
						.module(Module.vortex(0.02F, 0)).module(Module.jitter(0.01F)).life(50, 80).size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(1.5F)
						.sprite(FxSprites.STREAK).color(SNOW).alpha(0.8F).build())
				.build());

		// A spire cracks up out of the ground: ice shards and a puff of diamond dust.
		FxEngine.register(EffectDef.builder(VfxIds.SPIRE_CRACK).lod(96, 600)
				.emit(EmitterDef.builder("shards").burst(24).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(20, 40)
						.size(0.12F, 0.22F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(RIME).build())
				.emit(EmitterDef.builder("diamond").burst(30).shape(Shape.column(1.5F, 10)).velocity(Velocity.random(0.01F, 0.04F)).life(40, 70)
						.size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(RIME).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Hellmouth

	private static void hellmouth() {
		// A ring cracks, authored at radius 16: embers breathing up out of the fissure and a red smoke.
		FxEngine.register(EffectDef.builder(VfxIds.HELL_CRACK).lod(200, 1200)
				.emit(EmitterDef.builder("embers").burst(80).onGround().shape(Shape.ring(16, 0.6F)).velocity(Velocity.up(0.3F, 0.8F, 0.2F)).module(Module.jitter(0.01F))
						.life(30, 60).size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(HELLFIRE).alphaOverLife(TWINKLE).build())
				.emit(smoke("smoke").burst(24).onGround().shape(Shape.ring(16, 0.8F)).velocity(Velocity.up(0.15F, 0.3F, 0.2F)).life(60, 90).size(2, 3)
						.color(Gradient.of(0, 0x8A2010, 0.5F, 0x4A1810, 1, 0x302020)).alpha(0.5F).emissive(Curve.keys(0, 1, 0.5F, 0.2F)).build())
				.build());

		// The pit opens: a black-and-crimson eruption, magma spraying.
		FxEngine.register(EffectDef.builder(VfxIds.HELL_OPEN).lod(300, 2000).maxAge(300)
				.cue(0, EffectDef.light(1.3F, 0xFF3010))
				.emit(flash("flash", 30, 10, 0xFF6040).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(60).shape(Shape.disc(8)).velocity(Velocity.up(0.6F, 1.4F, 0.4F))
						.module(Module.buoyancy(0.01F)).drag(0.04F).life(30, 50).size(4, 7).sprite(FxSprites.FIRE, 2).color(HELLFIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(smoke("black").priority(Priority.HERO).burst(40).shape(Shape.disc(8)).velocity(Velocity.up(0.4F, 0.9F, 0.4F)).module(Module.curl(20, 0.008F))
						.life(160, 240).size(7, 11).color(BRIMSTONE).alpha(0.85F).emissive(Curve.keys(0, 0.8F, 0.3F, 0)).build())
				.emit(EmitterDef.builder("magma").burst(140).shape(Shape.disc(6)).velocity(Velocity.up(0.8F, 1.8F, 0.6F)).gravity(0.05F).collide(0.1F, 0.4F, 1)
						.life(40, 70).size(0.25F, 0.4F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(HELLFIRE).build())
				.build());

		// A hellfire geyser on the rim, a few ticks of roar.
		FxEngine.register(EffectDef.builder(VfxIds.HELL_GEYSER).lod(160, 1000)
				.emit(EmitterDef.builder("jet").rate(4, 8).shape(Shape.disc(0.8F)).velocity(Velocity.up(1.0F, 1.6F, 0.08F)).drag(0.03F).life(14, 24)
						.size(0.8F, 1.2F).sprite(FxSprites.FIRE, 2).color(HELLFIRE).alpha(0.5F).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").rate(3, 8).velocity(Velocity.up(0.6F, 1.2F, 0.3F)).module(Module.jitter(0.02F)).life(30, 50)
						.size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(HELLFIRE).alphaOverLife(TWINKLE).build())
				.build());

		// The throat: hellfire climbing out of the pit with souls spiralling up through it.
		FxEngine.register(EffectDef.builder(VfxIds.HELL_THROAT).lod(256, 1600)
				.emit(EmitterDef.builder("fire").rate(6, 8).shape(Shape.disc(4)).offset(0, -3, 0).velocity(Velocity.up(1.2F, 2.0F, 0.06F))
						.module(Module.vortex(0.04F, 0.02F)).life(20, 40).size(1.4F, 2.2F).sprite(FxSprites.FIRE, 2).color(HELLFIRE).alpha(0.5F).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("souls").rate(1.5F, 8).shape(Shape.disc(4)).velocity(Velocity.up(0.4F, 0.7F, 0.1F)).velocity(Velocity.swirl(0.15F, 0.3F))
						.module(Module.vortex(0.03F, 0.01F)).life(60, 100).size(0.9F, 1.4F).sprite(FxSprites.WISP).color(SOUL_RED).alpha(0.8F).build())
				.build());

		// The final eruption.
		FxEngine.register(EffectDef.builder(VfxIds.HELL_ERUPT).lod(400, 3000).maxAge(400)
				.cue(0, EffectDef.light(1.6F, 0xFF4020))
				.emit(flash("flash", 60, 14, 0xFFC0A0).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).rate(20, 60, Curve.FALL).shape(Shape.disc(6)).velocity(Velocity.up(2.0F, 3.4F, 0.05F))
						.drag(0.02F).life(30, 50).size(4, 6).sprite(FxSprites.FIRE, 2).color(HELLFIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("black").priority(Priority.HERO).rate(10, 100, Curve.FALL).shape(Shape.disc(10)).velocity(Velocity.up(0.8F, 1.6F, 0.2F))
						.module(Module.curl(30, 0.008F)).life(200, 300).size(10, 16).color(BRIMSTONE).alpha(0.85F).emissive(Curve.keys(0, 0.8F, 0.3F, 0)).build())
				.emit(EmitterDef.builder("souls").burst(60).shape(Shape.disc(6)).velocity(Velocity.up(1.0F, 2.0F, 0.5F)).velocity(Velocity.swirl(0.3F, 0.6F))
						.drag(0.02F).life(60, 100).size(1.2F, 1.8F).sprite(FxSprites.WISP).color(SOUL_RED).build())
				.emit(EmitterDef.builder("magma").burst(300).shape(Shape.disc(8)).velocity(Velocity.up(1.2F, 2.6F, 0.7F)).gravity(0.05F).collide(0.1F, 0.4F, 1)
						.life(50, 90).size(0.3F, 0.5F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(HELLFIRE).build())
				.build());
	}

	// ------------------------------------------------------------------ Cyclone

	private static void cyclone() {
		// A wedge: wide and dark. Played every couple of ticks where it stands.
		Module wedge = Module.funnel(40, 5, 20, 0.6F, 0.9F, 0.12F);
		FxEngine.register(EffectDef.builder(VfxIds.CYCLONE_WEDGE).lod(300, 2000)
				.emit(smoke("wall").priority(Priority.HERO).burst(10).shape(Shape.column(6, 10)).velocity(Velocity.up(0.8F, 1.4F, 0.05F)).module(wedge)
						.life(50, 70).size(4, 6).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.0F)).color(STORM_DUST).alpha(0.6F).shade(0.45F).build())
				.emit(EmitterDef.builder("rain").burst(14).shape(Shape.ring(24, 3)).offset(0, 30, 0).velocity(Velocity.swirl(0.6F, 0.9F)).velocity(Velocity.up(-1.0F, -0.8F, 0.05F))
						.life(30, 40).size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(STORM_RAIN).alpha(0.6F).build())
				.emit(EmitterDef.builder("band").burst(4).shape(Shape.ring(14, 2)).offset(0, 18, 0).velocity(Velocity.swirl(0.8F, 1.1F)).module(Module.vortex(0.0F, 0.04F))
						.life(40, 60).size(0.3F, 0.7F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(DEBRIS).spin(0.1F, 0.4F).build())
				.emit(smoke("skirt").burst(6).onGround().shape(Shape.ring(9, 4)).velocity(Velocity.swirl(0.4F, 0.6F)).velocity(Velocity.radial(0.1F, 0.3F))
						.velocity(Velocity.up(0.05F, 0.2F, 0.3F)).life(40, 60).size(4, 7).color(Gradient.of(0, 0x7A6A54, 1, 0x8A7C68)).alpha(0.55F).build())
				.emit(EmitterDef.builder("vortices").burst(2).onGround().shape(Shape.ring(7, 1)).velocity(Velocity.swirl(1.0F, 1.4F)).velocity(Velocity.up(0.4F, 0.8F, 0.05F))
						.life(14, 22).size(0.6F, 0.9F).layer(Layer.SMOKE).sprite(FxSprites.WISP).color(STORM_DUST).alpha(0.6F).build())
				.build());
	}

	// ------------------------------------------------------------------ Doomsday Clock

	private static void doomsday() {
		// An hour mark: a thread of embers rising.
		FxEngine.register(EffectDef.builder(VfxIds.HOUR_MARK).lod(128, 900)
				.emit(EmitterDef.builder("thread").rate(0.8F, 20).shape(Shape.disc(0.3F)).velocity(Velocity.up(0.2F, 0.35F, 0.02F)).life(40, 60).size(0.12F, 0.18F)
						.sprite(FxSprites.FLARE).color(DOOM).alphaOverLife(TWINKLE).build())
				.build());

		// The hand, played along its line: sparks and cut dust where it carves.
		FxEngine.register(EffectDef.builder(VfxIds.CLOCK_HAND).lod(128, 900)
				.emit(EmitterDef.builder("sparks").burst(10).span().velocity(Velocity.up(0.2F, 0.5F, 0.7F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(10, 20)
						.size(0.05F, 0.08F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(DOOM).build())
				.emit(smoke("dust").burst(2).span().velocity(Velocity.up(0.05F, 0.12F, 0.4F)).life(30, 50).size(0.8F, 1.3F).color(VfxLibrary.DUST).alpha(0.5F).build())
				.emit(EmitterDef.builder("glow").burst(4).span().life(3, 5).size(0.6F, 0.8F).sprite(FxSprites.GLOW).color(0xFF3030).alpha(0.6F).build())
				.build());

		// The hour strikes: a red bolt of falling streaks out of the sky and a burst where it lands.
		FxEngine.register(EffectDef.builder(VfxIds.HOUR_STRIKE).lod(256, 1600)
				.cue(10, EffectDef.light(0.9F, 0xFF3030))
				.emit(EmitterDef.builder("bolt").priority(Priority.HERO).burst(60).shape(Shape.column(0.6F, 120)).offset(0, 10, 0)
						.velocity(Velocity.up(-12.0F, -10.0F, 0)).life(10, 12).size(0.6F, 0.8F).mode(Mode.STRETCHED).stretch(1).sprite(FxSprites.STREAK)
						.color(DOOM).build())
				.emit(10, flash("flash", 14, 8, 0xFF6060).build())
				.emit(10, EmitterDef.builder("burst").burst(80).velocity(Velocity.up(0.5F, 1.2F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 1).life(20, 40)
						.size(0.12F, 0.2F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(DOOM).build())
				.emit(10, smoke("smoke").burst(14).shape(Shape.dome(2)).velocity(Velocity.up(0.15F, 0.35F, 0.6F)).life(60, 100).size(2.5F, 4)
						.color(Gradient.of(0, 0x4A2020, 1, 0x3A2828)).alpha(0.7F).build())
				.build());

		// Midnight at the centre.
		FxEngine.register(EffectDef.builder(VfxIds.MIDNIGHT).lod(300, 2000).maxAge(300)
				.cue(0, EffectDef.light(1.5F, 0xFF3030))
				.emit(flash("flash", 50, 14, 0xFFFFFF).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(400).shape(Shape.sphere(2)).velocity(Velocity.random(1.0F, 2.2F)).drag(0.03F)
						.life(30, 60).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(DOOM).build())
				.emit(smoke("smoke").priority(Priority.HERO).burst(50).shape(Shape.dome(6)).velocity(Velocity.radial(0.3F, 0.7F)).velocity(Velocity.up(0.3F, 0.8F, 0.5F))
						.drag(0.03F).life(140, 220).size(7, 11).color(Gradient.of(0, 0x5A2020, 1, 0x3A2A2A)).alpha(0.8F).emissive(Curve.keys(0, 0.8F, 0.3F, 0)).build())
				.build());
	}

	// ------------------------------------------------------------------ Big Bang

	private static void bigBang() {
		// The bang: one breath of white, then the primordial plasma, mottled hot and cold, flying outward with
		// glitter of the first light. Authored at scale 60.
		FxEngine.register(EffectDef.builder(VfxIds.BANG_BIRTH).lod(500, 3600).maxAge(400)
				.cue(0, EffectDef.light(2.0F, 0xFFFFFF))
				.emit(flash("white", 120, 16, 0xFFFFFF).build())
				.emit(smoke("plasma").priority(Priority.HERO).burst(120).shape(Shape.sphere(6)).velocity(Velocity.random(1.0F, 2.6F)).drag(0.03F)
						.module(Module.curl(20, 0.01F)).life(120, 200).size(14, 22).color(PRIMORDIAL).alpha(0.7F).shade(1.0F)
						.emissive(Curve.keys(0, 1, 0.6F, 0.8F, 1, 0.3F)).build())
				.emit(EmitterDef.builder("light").priority(Priority.HERO).burst(600).shape(Shape.sphere(2)).velocity(Velocity.random(1.5F, 3.5F)).drag(0.025F)
						.life(60, 120).size(0.4F, 0.7F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(NEWBORN).build())
				.build());

		// Newborn stars twinkling all through the new space (played every few ticks, authored at scale 60).
		FxEngine.register(EffectDef.builder(VfxIds.BANG_STARS).lod(500, 3600)
				.emit(EmitterDef.builder("stars").burst(14).shape(Shape.sphere(50)).life(40, 80).size(1.0F, 1.6F).sprite(FxSprites.FLARE).color(NEWBORN)
						.alphaOverLife(TWINKLE).build())
				.build());

		// A planet forms: a swirl of dust its own colour drawn into it, and a flash as it lights.
		FxEngine.register(EffectDef.builder(VfxIds.PLANET_FORM).tint(0x80A0FF).lod(400, 3000).maxAge(160)
				.emit(smoke("disc").priority(Priority.HERO).burst(60).shape(Shape.ring(14, 3)).velocity(Velocity.swirl(0.3F, 0.5F)).module(Module.attract(0.02F, 3))
						.module(Module.vortex(0.02F, 0)).life(60, 90).size(1.5F, 2.5F).color(WHITE).tinted().alpha(0.6F).emissive(Curve.constant(0.5F)).build())
				.emit(30, flash("light", 10, 10, 0xFFFFFF).tinted().build())
				.build());
	}

	// ------------------------------------------------------------------ Chrono Break

	private static void chrono() {
		// An erasing shell, authored at radius 10: teal grains flicker on it and run backwards, inward.
		FxEngine.register(EffectDef.builder(VfxIds.CHRONO_SHELL).lod(160, 1000)
				.emit(EmitterDef.builder("grains").burst(140).shape(Shape.shell(10)).velocity(Velocity.radial(-0.15F, -0.05F)).life(20, 30).size(0.25F, 0.4F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(CHRONO).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("ticks").burst(20).shape(Shape.shell(10)).life(4, 8).size(0.5F, 0.8F).fixedRotation().sprite(FxSprites.FLARE).color(CHRONO)
						.alphaOverLife(FLICKER).build())
				.build());

		// Time restores: grains of sand flowing in from every side to rebuild what was erased.
		FxEngine.register(EffectDef.builder(VfxIds.CHRONO_SAND).lod(160, 1000)
				.emit(EmitterDef.builder("sand").burst(60).shape(Shape.shell(40)).module(Module.attract(0.03F, 3)).drag(0.02F).life(50, 70).size(0.2F, 0.3F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(SAND).emissive(Curve.constant(0.4F)).build())
				.emit(EmitterDef.builder("light").burst(20).shape(Shape.shell(36)).module(Module.attract(0.03F, 3)).life(50, 70).size(0.3F, 0.45F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(CHRONO).build())
				.build());

		// The moment it breaks: a hush of teal, then everything on the dial flies apart.
		FxEngine.register(EffectDef.builder(VfxIds.CHRONO_SNAP).lod(256, 1600)
				.emit(flash("flash", 30, 10, 0xA0FFE8).build())
				.emit(EmitterDef.builder("shatter").priority(Priority.HERO).burst(300).shape(Shape.sphere(3)).velocity(Velocity.random(0.8F, 2.0F)).drag(0.04F)
						.life(30, 50).size(0.2F, 0.35F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(CHRONO).build())
				.build());
	}

	// ------------------------------------------------------------------ Colossal Spear

	private static void spear() {
		// As it falls: golden light spiralling down onto the target.
		FxEngine.register(EffectDef.builder(VfxIds.SPEAR_HERALD).lod(300, 2000)
				.emit(EmitterDef.builder("light").burst(10).shape(Shape.ring(30, 6)).offset(0, 60, 0).velocity(Velocity.swirl(0.6F, 0.9F)).velocity(Velocity.up(-0.8F, -0.5F, 0.05F))
						.module(Module.attract(0.01F, 3)).life(60, 90).size(0.55F, 0.85F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(HEAVEN).build())
				.build());

		// It lands: stone thrown, glowing fracture lines racing out over the ground, a dust wall.
		FxEngine.register(EffectDef.builder(VfxIds.SPEAR_IMPACT).lod(400, 3000).maxAge(300)
				.cue(0, EffectDef.light(1.5F, 0xFFE0B0))
				.emit(flash("flash", 40, 12, 0xFFF0D0).build())
				.emit(EmitterDef.builder("fractures").priority(Priority.HERO).burst(36).onGround().offset(0, 0.2F, 0).shape(Shape.ring(12, 0)).velocity(Velocity.radial(1.4F, 2.0F))
						.drag(0.06F).life(30, 40).size(0.3F, 0.45F).mode(Mode.STRETCHED).stretch(8).sprite(FxSprites.STREAK).color(HEAVEN).build())
				.emit(EmitterDef.builder("stone").priority(Priority.HERO).burst(200).onGround().shape(Shape.ring(12, 3)).velocity(Velocity.up(0.8F, 1.8F, 0.6F))
						.gravity(0.05F).collide(0.3F, 0.6F, 2).life(50, 90).size(0.4F, 0.8F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(STONE)
						.spin(0.05F, 0.2F).build())
				.emit(smoke("wall").priority(Priority.HERO).rate(30, 30, Curve.FALL).onGround().shape(Shape.ring(14, 4)).expand(1.4F).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.life(120, 180).size(6, 10).color(Gradient.of(0, 0xD8D0C0, 1, 0xB0A898)).alpha(0.6F).shade(0.75F).build())
				.build());

		// The drill at work: molten spray and grinding sparks round the shaft, smoke rising out of it.
		FxEngine.register(EffectDef.builder(VfxIds.SPEAR_DRILL).lod(200, 1400)
				.emit(EmitterDef.builder("sparks").burst(40).shape(Shape.ring(12, 0.5F)).velocity(Velocity.swirl(0.5F, 0.9F)).velocity(Velocity.up(0.3F, 0.8F, 0.3F))
						.gravity(0.04F).collide(0.3F, 0.6F, 1).life(14, 26).size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(MOLTEN).build())
				.emit(EmitterDef.builder("molten").burst(10).shape(Shape.ring(11, 1)).velocity(Velocity.up(0.5F, 1.0F, 0.4F)).gravity(0.05F).collide(0.1F, 0.4F, 1)
						.life(30, 50).size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(MOLTEN).build())
				.emit(smoke("smoke").burst(4).shape(Shape.disc(10)).velocity(Velocity.up(0.4F, 0.8F, 0.1F)).module(Module.curl(20, 0.006F)).life(80, 130).size(4, 7)
						.color(Gradient.of(0, 0x6A6058, 1, 0x8A8078)).alpha(0.4F).emissive(Curve.keys(0, 0.5F, 0.3F, 0)).build())
				.build());

		// The core answers: a geyser of light up the shaft and petals of light falling over the sigil.
		FxEngine.register(EffectDef.builder(VfxIds.CORE_GEYSER).lod(400, 3000).maxAge(320)
				.cue(0, EffectDef.light(1.6F, 0xFFF4D0))
				.emit(flash("flash", 40, 14, 0xFFF8E8).build())
				.emit(EmitterDef.builder("geyser").priority(Priority.HERO).rate(30, 120, Curve.FALL).shape(Shape.disc(6)).velocity(Velocity.up(2.0F, 3.4F, 0.04F))
						.drag(0.01F).life(40, 70).size(0.5F, 0.9F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(HEAVEN).build())
				.emit(20, EmitterDef.builder("petals").rate(8, 200).shape(Shape.disc(30)).offset(0, 40, 0).velocity(Velocity.up(-0.1F, -0.05F, 0.4F)).gravity(0.002F)
						.drag(0.04F).module(Module.curl(4, 0.006F)).life(120, 200).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PETAL)
						.emissive(Curve.constant(0.7F)).spin(0.05F, 0.15F).build())
				.build());
	}

	// ------------------------------------------------------------------ Zombie Apocalypse

	private static void zombies() {
		FxEngine.register(EffectDef.builder(VfxIds.ZOMBIE_CLAW).lod(64, 300)
				.emit(EmitterDef.builder("clods").burst(14).shape(Shape.disc(0.5F)).velocity(Velocity.up(0.2F, 0.45F, 0.7F)).gravity(0.05F).collide(0.3F, 0.5F, 1)
						.life(20, 40).size(0.1F, 0.18F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PODZOL).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("rot").burst(3).offset(0, 0.5F, 0).velocity(Velocity.up(0.03F, 0.08F, 0.4F)).module(Module.curl(2, 0.004F)).life(30, 50)
						.size(0.35F, 0.55F).sprite(FxSprites.WISP).color(ROT).alpha(0.7F).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GIANT_RISE).lod(256, 1600).maxAge(200)
				.emit(EmitterDef.builder("earth").priority(Priority.HERO).burst(160).shape(Shape.disc(4)).velocity(Velocity.up(0.6F, 1.4F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 2).life(40, 70).size(0.3F, 0.6F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PODZOL).spin(0.05F, 0.2F).build())
				.emit(smoke("dust").burst(30).onGround().shape(Shape.ring(4, 2)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.life(80, 120).size(3, 5).color(Gradient.of(0, 0x6A5038, 1, 0x7A6450)).alpha(0.6F).build())
				.emit(EmitterDef.builder("spores").burst(40).shape(Shape.disc(5)).velocity(Velocity.up(0.1F, 0.3F, 0.5F)).module(Module.curl(4, 0.006F)).life(60, 100)
						.size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(ROT).alphaOverLife(TWINKLE).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GIANT_STOMP).lod(128, 900)
				.emit(smoke("dust").burst(20).onGround().shape(Shape.ring(3, 1)).velocity(Velocity.radial(0.3F, 0.5F)).velocity(Velocity.up(0.05F, 0.15F, 0.3F))
						.life(40, 60).size(2, 3).color(Gradient.of(0, 0x7A6A50, 1, 0x8A7C64)).alpha(0.55F).build())
				.emit(EmitterDef.builder("clods").burst(16).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.3F, 0.6F, 0.6F)).gravity(0.05F).collide(0.3F, 0.5F, 1)
						.life(20, 40).size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PODZOL).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.GIANT_BURST).lod(300, 2000).maxAge(360)
				.cue(0, EffectDef.light(1.2F, 0x9CFF6A))
				.emit(flash("flash", 20, 10, 0xC8FF90).build())
				.emit(smoke("cloud").priority(Priority.HERO).rate(20, 30, Curve.FALL).shape(Shape.dome(6)).velocity(Velocity.radial(0.4F, 0.9F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F))
						.drag(0.02F).life(200, 300).size(8, 13).color(TOXIC).alpha(0.65F).shade(0.75F).emissive(Curve.keys(0, 0.4F, 0.3F, 0.1F, 1, 0)).build())
				.emit(EmitterDef.builder("slime").priority(Priority.HERO).burst(80).shape(Shape.sphere(3)).velocity(Velocity.up(0.6F, 1.4F, 0.9F)).gravity(0.05F)
						.collide(0.4F, 0.6F, 2).life(40, 70).size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(SLIME).build())
				.emit(EmitterDef.builder("bone").burst(40).shape(Shape.sphere(3)).velocity(Velocity.up(0.5F, 1.2F, 0.9F)).gravity(0.05F).collide(0.3F, 0.6F, 2)
						.life(50, 80).size(0.15F, 0.3F).mode(Mode.STRETCHED).stretch(0.6F).layer(Layer.SMOKE).sprite(FxSprites.STREAK).color(BONE).build())
				.build());

		// The dead crumble to ash.
		FxEngine.register(EffectDef.builder(VfxIds.ZOMBIE_DUST).lod(64, 300)
				.emit(smoke("ash").burst(6).shape(Shape.column(0.4F, 1.8F)).velocity(Velocity.up(-0.03F, 0.02F, 0.6F)).gravity(0.003F).life(40, 70).size(0.5F, 0.8F)
						.color(ZOMBIE_ASH).alpha(0.65F).build())
				.emit(EmitterDef.builder("flecks").burst(12).shape(Shape.column(0.4F, 1.8F)).velocity(Velocity.random(0.02F, 0.06F)).gravity(0.02F).life(20, 40)
						.size(0.06F, 0.1F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(ZOMBIE_ASH).build())
				.build());
	}
}
