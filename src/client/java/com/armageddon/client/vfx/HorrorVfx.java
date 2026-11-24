package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the horror celestials: the Terror Maniac, the Pumpkin Horseman and the Portal of Hell. */
final class HorrorVfx {
	private HorrorVfx() {
	}

	static final Gradient BLOOD = Gradient.of(0, 0xFF6040, 0.3F, 0xFF2010, 0.7F, 0x900808, 1, argb(0x400404, 0));
	static final Gradient ASH = Gradient.of(0, 0x3A2020, 0.5F, 0x241414, 1, 0x140A0A);

	static void register() {
		maniac();
		horseman();
		hellgate();
	}

	static final Gradient HELLFIRE = Gradient.of(0, 0xFFF4C0, 0.25F, 0xFFA030, 0.6F, 0xE83810, 1, argb(0x601008, 0));
	static final Gradient SOUL = Gradient.of(0, 0xFFE0C0, 0.4F, 0xFF7040, 1, argb(0x802010, 0));

	private static void hellgate() {
		// Heat round the watcher: embers rising, ash falling, a shimmer of red.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_HEAT).lod(80, 400)
				.emit(EmitterDef.builder("embers").burst(4).shape(Shape.box(16, 2, 16)).velocity(Velocity.up(0.04F, 0.1F, 0.03F)).module(Module.curl(5, 0.006F))
						.life(60, 110).size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(HELLFIRE).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ash").burst(3).shape(Shape.box(16, 2, 16)).offset(0, 14, 0).velocity(Velocity.up(-0.05F, -0.02F, 0.03F))
						.module(Module.jitter(0.01F)).life(100, 160).size(0.08F, 0.14F).spin(-0.3F, 0.3F).sprite(FxSprites.DUST).color(0x302828).build())
				.build());

		// The gate heaving up: dust and stone bursting from its foot.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_RISE).lod(300, 2000)
				.emit(smoke("dust").burst(6).onGround().shape(Shape.box(20, 0, 4)).velocity(Velocity.up(0.2F, 0.5F, 0.6F)).life(40, 70).size(3, 5)
						.color(Gradient.of(0, 0x5A4A40, 1, 0x3A302A)).alpha(0.6F).build())
				.emit(EmitterDef.builder("stones").burst(8).onGround().shape(Shape.box(20, 0, 4)).velocity(Velocity.up(0.4F, 0.8F, 0.6F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(30, 50).size(0.25F, 0.45F).spin(-0.4F, 0.4F).sprite(FxSprites.DUST).color(0x2A2428).build())
				.build());

		// The portal igniting: a blast of fire out of the arch, a ring of it, a flash.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_OPEN).lod(500, 3000).maxAge(160)
				.cue(0, EffectDef.light(1.8F, 0xFF5010))
				.emit(flash("flash", 40, 14, 0xFFB060).build())
				.emit(EmitterDef.builder("blast").priority(Priority.HERO).burst(220).shape(Shape.disc(12)).velocity(Velocity.along(0.8F, 2.0F, 0.6F)).drag(0.05F)
						.life(24, 50).size(2.5F, 4.5F).sprite(FxSprites.FIRE, 2).color(HELLFIRE).build())
				.emit(smoke("smoke").burst(50).shape(Shape.disc(12)).velocity(Velocity.along(0.4F, 1.0F, 0.6F)).drag(0.03F).life(80, 140).size(5, 9)
						.color(Gradient.of(0, 0x301010, 1, 0x180808)).alpha(0.7F).build())
				.build());

		// Souls pouring out of the portal, wailing and spiralling.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_SOULS).lod(300, 2000)
				.emit(EmitterDef.builder("souls").burst(3).shape(Shape.disc(10)).velocity(Velocity.along(0.4F, 0.8F, 0.6F)).velocity(Velocity.swirl(0.2F, 0.4F))
						.module(Module.curl(10, 0.02F)).life(50, 90).size(1.2F, 2.2F).sprite(FxSprites.WISP).color(SOUL).alpha(0.7F).build())
				.emit(EmitterDef.builder("sparks").burst(5).shape(Shape.disc(10)).velocity(Velocity.along(0.5F, 1.0F, 0.5F)).life(20, 40).size(0.12F, 0.2F)
						.sprite(FxSprites.FLARE).color(HELLFIRE).build())
				.build());

		// A fireball in flight: flame and black smoke streaming off it.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_FIRE_TRAIL).lod(250, 1800)
				.emit(EmitterDef.builder("fire").burst(4).inherit(0.15F).shape(Shape.sphere(1.6F)).life(10, 22).size(1.8F, 3).sprite(FxSprites.FIRE, 2)
						.color(HELLFIRE).build())
				.emit(smoke("smoke").burst(2).inherit(0.05F).life(30, 60).size(2, 3.5F).sizeOverLife(Curve.RISE).color(Gradient.of(0, 0x301818, 1, 0x1A1010))
						.alpha(0.6F).build())
				.build());

		// A fireball lands.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_IMPACT).lod(400, 2400).maxAge(140)
				.cue(0, EffectDef.light(1.4F, 0xFF5010))
				.emit(flash("flash", 20, 8, 0xFFB060).build())
				.emit(EmitterDef.builder("ball").priority(Priority.HERO).burst(40).shape(Shape.sphere(2)).velocity(Velocity.radial(0.4F, 0.9F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).drag(0.06F).life(16, 30).size(2.5F, 4.5F).sprite(FxSprites.FIRE, 2).color(HELLFIRE).build())
				.emit(smoke("smoke").burst(16).shape(Shape.sphere(3)).velocity(Velocity.up(0.3F, 0.6F, 0.4F)).drag(0.03F).life(60, 100).size(4, 7)
						.color(Gradient.of(0, 0x301818, 1, 0x1A1010)).alpha(0.65F).build())
				.emit(EmitterDef.builder("debris").burst(30).onGround().velocity(Velocity.up(0.6F, 1.2F, 0.8F)).gravity(0.05F).collide(0.3F, 0.5F, 1).life(30, 60)
						.size(0.2F, 0.4F).spin(-0.5F, 0.5F).sprite(FxSprites.DUST).color(0x3A2A24).build())
				.build());

		// Lava splashing at the foot of the fall.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_LAVA_SPLASH).lod(200, 1400)
				.emit(EmitterDef.builder("drops").burst(6).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.3F, 0.6F, 0.5F)).gravity(0.05F).life(14, 26)
						.size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(HELLFIRE).build())
				.emit(smoke("steam").burst(1).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.2F, 0.2F)).life(40, 70).size(2, 3.5F)
						.color(Gradient.of(0, 0x504040, 1, 0x302828)).alpha(0.4F).build())
				.build());

		// The fist comes down: a vast blast of fire and rock, a ring racing out, a column of black smoke.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_SLAM).lod(600, 3600).maxAge(220)
				.cue(0, EffectDef.light(2.0F, 0xFF5010))
				.emit(flash("flash", 50, 14, 0xFFC070).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(260).onGround().shape(Shape.disc(6)).velocity(Velocity.radial(0.8F, 2.0F))
						.velocity(Velocity.up(0.3F, 1.0F, 0.4F)).drag(0.05F).life(20, 44).size(3, 6).sprite(FxSprites.FIRE, 2).color(HELLFIRE).build())
				.emit(EmitterDef.builder("rock").burst(120).onGround().shape(Shape.disc(5)).velocity(Velocity.up(0.8F, 1.8F, 1.0F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 1).life(40, 80).size(0.4F, 0.9F).spin(-0.5F, 0.5F).sprite(FxSprites.DUST).color(0x2A2020).build())
				.emit(smoke("column").burst(60).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.5F, 1.2F, 0.4F)).drag(0.02F).module(Module.curl(12, 0.01F))
						.life(100, 180).size(6, 11).sizeOverLife(Curve.RISE).color(Gradient.of(0, 0x3A2020, 1, 0x1A1010)).alpha(0.75F).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(70, 70).life(30, 30)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xFF8040).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The portal implodes: everything drawn into a point, then a flash and a ring of fire blown out flat.
		FxEngine.register(EffectDef.builder(VfxIds.HELLGATE_IMPLODE).lod(500, 3000).maxAge(160)
				.cue(0, EffectDef.light(2.0F, 0xFFA060))
				.emit(EmitterDef.builder("inward").priority(Priority.HERO).rate(40, 12).shape(Shape.disc(18)).module(Module.attract(0.15F, 1.5F)).life(10, 16)
						.size(1.5F, 2.5F).sprite(FxSprites.FIRE, 2).color(HELLFIRE).build())
				.emit(12, flash("flash", 50, 14, 0xFFE0B0).build())
				.emit(12, EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).size(60, 60).life(26, 26).sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT)
						.color(0xFFA050).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(12, EmitterDef.builder("embers").burst(200).shape(Shape.sphere(3)).velocity(Velocity.random(0.8F, 2.0F)).drag(0.04F).life(30, 70)
						.size(0.15F, 0.3F).sprite(FxSprites.FLARE).color(HELLFIRE).build())
				.build());
	}

	static final Gradient PUMPKIN_FIRE = Gradient.of(0, 0xFFF0A0, 0.3F, 0xFFA030, 0.7F, 0xFF5010, 1, argb(0x801008, 0));
	static final Gradient GHOST_FIRE = Gradient.of(0, 0xE0FFC0, 0.35F, 0x80FF50, 0.7F, 0x30A020, 1, argb(0x104010, 0));

	private static void horseman() {
		// Halloween air round the watcher: low orange-violet mist, fireflies, now and then a bat-shaped flutter.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_MIST).lod(80, 400)
				.emit(smoke("mist").burst(2).onGround().shape(Shape.disc(18)).offset(0, 0.4F, 0).velocity(Velocity.random(0.02F, 0.05F)).module(Module.wind(0.004F))
						.life(80, 140).size(3, 5).color(Gradient.of(0, 0x6A4A7A, 1, 0x4A3050)).alpha(0.3F).shade(1).emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("fireflies").burst(2).shape(Shape.box(16, 3, 16)).offset(0, 2, 0).module(Module.curl(4, 0.006F)).life(60, 120)
						.size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(PUMPKIN_FIRE).alphaOverLife(TWINKLE).build())
				.build());

		// He arrives: a geyser of ghost fire and orange flame out of the ground, and a ring of it racing out.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_APPEAR).lod(400, 2600).maxAge(160)
				.cue(0, EffectDef.light(1.6F, 0xFF9030))
				.emit(flash("flash", 30, 12, 0xFFC060).build())
				.emit(EmitterDef.builder("ghost").priority(Priority.HERO).rate(40, 50, Curve.FALL).shape(Shape.disc(6)).velocity(Velocity.up(0.8F, 1.6F, 0.2F))
						.module(Module.curl(8, 0.02F)).life(30, 60).size(2.5F, 4.5F).sprite(FxSprites.FIRE, 2).color(GHOST_FIRE).build())
				.emit(EmitterDef.builder("flame").rate(30, 50, Curve.FALL).shape(Shape.disc(7)).velocity(Velocity.up(0.6F, 1.2F, 0.3F)).module(Module.curl(8, 0.02F))
						.life(24, 50).size(2, 4).sprite(FxSprites.FIRE, 2).color(PUMPKIN_FIRE).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(50, 50).life(30, 30)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0x90FF50).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// A hoof strikes: a burst of ghost fire and sparks, clods thrown up.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_HOOF).lod(200, 1400).maxAge(60)
				.emit(EmitterDef.builder("fire").burst(8).onGround().shape(Shape.disc(1.2F)).velocity(Velocity.up(0.2F, 0.5F, 0.4F)).life(14, 26).size(1, 1.8F)
						.sprite(FxSprites.FIRE, 2).color(GHOST_FIRE).build())
				.emit(EmitterDef.builder("sparks").burst(12).onGround().velocity(Velocity.up(0.3F, 0.7F, 0.6F)).gravity(0.04F).life(14, 28).size(0.1F, 0.18F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(PUMPKIN_FIRE).build())
				.emit(smoke("clods").burst(4).onGround().shape(Shape.disc(1.5F)).velocity(Velocity.up(0.15F, 0.35F, 0.5F)).gravity(0.03F).life(24, 40)
						.size(1.2F, 2).color(Gradient.of(0, 0x5A4636, 1, 0x3A2C20)).alpha(0.6F).build())
				.build());

		// The thrown lantern's trail: flame, sparks and a ribbon of smoke.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_TRAIL).lod(250, 1600)
				.emit(EmitterDef.builder("fire").burst(3).inherit(0.15F).shape(Shape.sphere(1.2F)).life(10, 20).size(1.4F, 2.2F).sprite(FxSprites.FIRE, 2)
						.color(PUMPKIN_FIRE).build())
				.emit(smoke("smoke").burst(1).inherit(0.05F).life(30, 50).size(1.5F, 2.5F).sizeOverLife(Curve.RISE)
						.color(Gradient.of(0, 0x3A2A24, 1, 0x241A18)).alpha(0.5F).build())
				.build());

		// A lantern bursts: a fireball, burning pumpkin chunks and seeds thrown everywhere, a grinning ring of flame.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_PUMPKIN).lod(400, 2400).maxAge(160)
				.cue(0, EffectDef.light(1.5F, 0xFF8020))
				.emit(flash("flash", 24, 10, 0xFFC060).build())
				.emit(EmitterDef.builder("ball").priority(Priority.HERO).burst(50).shape(Shape.sphere(2)).velocity(Velocity.radial(0.4F, 0.9F))
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).drag(0.06F).life(16, 32).size(2.5F, 4.5F).sprite(FxSprites.FIRE, 2).color(PUMPKIN_FIRE).build())
				.emit(EmitterDef.builder("chunks").burst(40).shape(Shape.sphere(1)).velocity(Velocity.up(0.6F, 1.2F, 0.9F)).gravity(0.05F).collide(0.3F, 0.5F, 1)
						.life(30, 60).size(0.3F, 0.55F).spin(-0.5F, 0.5F).sprite(FxSprites.DUST).color(Gradient.of(0, 0xE07818, 1, 0xA04A08)).build())
				.emit(EmitterDef.builder("seeds").burst(60).shape(Shape.sphere(1)).velocity(Velocity.up(0.4F, 1.0F, 1.0F)).gravity(0.04F).life(20, 40)
						.size(0.08F, 0.14F).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFF0C0, 1, 0xFFD080)).build())
				.emit(smoke("smoke").burst(20).shape(Shape.sphere(3)).velocity(Velocity.up(0.2F, 0.5F, 0.4F)).drag(0.03F).life(60, 100).size(4, 7)
						.color(Gradient.of(0, 0x3A2A24, 1, 0x2A201C)).alpha(0.6F).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(18, 18).life(22, 22)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xFFA040).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The charge: a wave of ghost fire peeling off the scythe's path, sparks and earth thrown aside.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_SLASH).lod(300, 2000).maxAge(80)
				.emit(EmitterDef.builder("fire").burst(20).onGround().shape(Shape.box(2, 0, 6)).velocity(Velocity.up(0.4F, 0.9F, 0.5F)).life(20, 40).size(1.5F, 3)
						.sprite(FxSprites.FIRE, 2).color(GHOST_FIRE).build())
				.emit(EmitterDef.builder("arc").burst(1).offset(0, 3, 0).mode(Mode.FLAT).size(10, 10).life(10, 10).sprite(FxSprites.RING).color(0xC0FF90)
						.alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("earth").burst(10).onGround().shape(Shape.box(2, 0, 5)).velocity(Velocity.up(0.4F, 0.8F, 0.6F)).gravity(0.03F).life(30, 50)
						.size(2, 3.5F).color(Gradient.of(0, 0x5A4636, 1, 0x3A2C20)).alpha(0.6F).build())
				.build());

		// He is gone: a burst of ghost fire and pumpkin flame in the sky, and a shower of embers falling.
		FxEngine.register(EffectDef.builder(VfxIds.HORSE_VANISH).lod(600, 3600).maxAge(200)
				.cue(0, EffectDef.light(1.8F, 0xFF9030))
				.emit(flash("flash", 40, 12, 0xFFD080).build())
				.emit(EmitterDef.builder("ghost").priority(Priority.HERO).burst(200).shape(Shape.sphere(4)).velocity(Velocity.radial(0.8F, 2.0F)).drag(0.04F)
						.life(30, 60).size(2, 4).sprite(FxSprites.FIRE, 2).color(GHOST_FIRE).build())
				.emit(EmitterDef.builder("flame").burst(150).shape(Shape.sphere(4)).velocity(Velocity.radial(0.6F, 1.6F)).drag(0.04F).life(30, 60).size(2, 3.5F)
						.sprite(FxSprites.FIRE, 2).color(PUMPKIN_FIRE).build())
				.emit(10, EmitterDef.builder("embers").rate(30, 100).shape(Shape.disc(40)).velocity(Velocity.up(-0.4F, -0.2F, 0.1F)).module(Module.jitter(0.02F))
						.life(60, 100).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(PUMPKIN_FIRE).alphaOverLife(TWINKLE).build())
				.build());
	}

	private static void maniac() {
		// The air in the inside-out world: red motes and flakes of black ash drifting up out of the ground.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_ASH).lod(80, 400)
				.emit(EmitterDef.builder("motes").burst(4).shape(Shape.box(16, 6, 16)).offset(0, 3, 0).velocity(Velocity.up(0.02F, 0.06F, 0.02F))
						.module(Module.curl(6, 0.004F)).life(80, 140).size(0.06F, 0.12F).sprite(FxSprites.FLARE).color(BLOOD).alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("ash").burst(5).shape(Shape.box(16, 6, 16)).offset(0, 3, 0).velocity(Velocity.up(0.01F, 0.04F, 0.03F))
						.module(Module.curl(5, 0.005F)).life(100, 160).size(0.08F, 0.16F).spin(-0.2F, 0.2F).sprite(FxSprites.DUST).color(ASH).build())
				.build());

		// The rift opening: a red flash, a column of black smoke boiling out, embers and earth thrown up.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_RIFT).lod(400, 2400).maxAge(200)
				.cue(0, EffectDef.light(1.4F, 0xFF2010))
				.emit(flash("flash", 24, 12, 0xFF4020).build())
				.emit(smoke("column").priority(Priority.HERO).rate(10, 80, Curve.FALL).shape(Shape.disc(5)).velocity(Velocity.up(0.4F, 0.9F, 0.2F))
						.module(Module.curl(10, 0.01F)).life(80, 140).size(5, 9).sizeOverLife(Curve.RISE).color(Gradient.of(0, 0x2A1010, 1, 0x100808))
						.alpha(0.75F).build())
				.emit(EmitterDef.builder("embers").rate(20, 80, Curve.FALL).shape(Shape.disc(4)).velocity(Velocity.up(0.5F, 1.2F, 0.4F)).gravity(0.01F)
						.module(Module.curl(6, 0.01F)).life(30, 60).size(0.15F, 0.3F).sprite(FxSprites.FLARE).color(BLOOD).build())
				.emit(smoke("earth").burst(30).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.4F, 0.9F, 0.6F)).gravity(0.03F).life(40, 70).size(2.5F, 4)
						.color(Gradient.of(0, 0x5A4A3A, 1, 0x3A2E24)).alpha(0.7F).build())
				.build());

		// The curse taking hold: red wisps spiralling round the victim, black threads pulling upward.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_CURSE).lod(120, 700).maxAge(40)
				.emit(EmitterDef.builder("wisps").burst(6).shape(Shape.ring(0.9F, 0.2F)).offset(0, 1, 0).velocity(Velocity.swirl(0.15F, 0.25F))
						.velocity(Velocity.up(0.05F, 0.12F, 0)).module(Module.vortex(0.08F, 0.02F)).life(20, 34).size(0.5F, 0.8F).sprite(FxSprites.WISP)
						.color(BLOOD).alpha(0.7F).build())
				.emit(EmitterDef.builder("threads").burst(5).shape(Shape.sphere(0.6F)).offset(0, 1, 0).velocity(Velocity.up(0.3F, 0.6F, 0.05F)).life(8, 16)
						.size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(8).sprite(FxSprites.STREAK).color(0x200808).build())
				.build());

		// Broken: a crack of red light, a puff of black dust.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_SNAP).lod(160, 900).maxAge(60)
				.emit(flash("flash", 4, 5, 0xFF2010).build())
				.emit(smoke("dust").burst(10).shape(Shape.sphere(0.6F)).velocity(Velocity.random(0.1F, 0.3F)).drag(0.08F).life(30, 50).size(0.8F, 1.4F)
						.color(Gradient.of(0, 0x301010, 1, 0x180808)).alpha(0.7F).build())
				.emit(EmitterDef.builder("shards").burst(16).shape(Shape.sphere(0.4F)).velocity(Velocity.random(0.3F, 0.6F)).gravity(0.04F).life(14, 24)
						.size(0.08F, 0.14F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(BLOOD).build())
				.build());

		// It comes apart: a storm of black moths bursting out and fluttering away, embers, a dark red shockwave.
		FxEngine.register(EffectDef.builder(VfxIds.MANIAC_SHATTER).lod(600, 3600).maxAge(240)
				.cue(0, EffectDef.light(1.6F, 0xFF2010))
				.emit(flash("flash", 40, 14, 0xFF3020).build())
				.emit(EmitterDef.builder("moths").priority(Priority.HERO).burst(900).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.4F, 1.2F))
						.velocity(Velocity.up(0.05F, 0.3F, 0.3F)).drag(0.02F).module(Module.curl(6, 0.03F)).module(Module.jitter(0.06F)).life(80, 180)
						.size(0.25F, 0.45F).spin(-1.5F, 1.5F).sprite(FxSprites.DUST).color(0x0E0808).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("embers").burst(300).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.3F, 1.0F)).module(Module.curl(8, 0.02F))
						.life(60, 140).size(0.12F, 0.22F).sprite(FxSprites.FLARE).color(BLOOD).alphaOverLife(TWINKLE).build())
				.emit(smoke("smoke").burst(60).shape(Shape.box(6, 20, 4)).velocity(Velocity.radial(0.3F, 0.7F)).drag(0.03F).life(100, 160).size(5, 9)
						.color(Gradient.of(0, 0x301010, 1, 0x120808)).alpha(0.6F).build())
				.emit(EmitterDef.builder("shock").burst(1).offset(0, -24, 0).onGround().mode(Mode.FLAT).fixedRotation().size(90, 90).life(36, 36)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xC01008).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
