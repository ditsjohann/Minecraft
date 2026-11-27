package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.util.WorldBombShapes.Wyrm;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/** Effects for the two world bombs: the Mega Creeper and the Elder Wyrm. */
final class WorldBombVfx {
	private WorldBombVfx() {
	}

	static final Gradient EARTH = Gradient.of(0, 0x4A3C2E, 1, 0x6A5C4A);
	static final Gradient DUST = Gradient.of(0, 0x8A7A62, 1, 0xA49680);
	/** A creeper's blast: white puffs going grey. */
	static final Gradient PUFF = Gradient.of(0, 0xFFFFFF, 0.35F, 0xE8E8E8, 0.7F, 0xB8B8B8, 1, 0x909090);
	static final Gradient CREEPER = Gradient.of(0, 0xD0FFC0, 0.5F, 0x60E050, 1, argb(0x208020, 0));
	static final Gradient FIRE = Gradient.of(0, 0xFFFFE0, 0.2F, 0xFFD060, 0.45F, 0xFF7018, 0.75F, 0xC02808, 1, argb(0x300800, 0));
	static final Gradient EMBER = Gradient.of(0, 0xFFF0B0, 0.3F, 0xFFA040, 0.7F, 0xE04010, 1, argb(0x701008, 0));
	static final Gradient SOOT = Gradient.of(0, 0x2A2220, 0.5F, 0x3E3632, 1, 0x5A524C);

	static void register() {
		creeper();
		wyrm();
	}

	private static EmitterDef.Builder blob(String name) {
		return EmitterDef.builder(name).layer(Layer.SMOKE).sprite(FxSprites.DUST).shade(0.8F);
	}

	// ------------------------------------------------------------------ Mega Creeper

	private static void creeper() {
		// The ground bursting as it heaves itself up: earth and rock thrown high, a wall of dust rolling out.
		FxEngine.register(EffectDef.builder(VfxIds.CREEPER_RISE).lod(500, 3200).maxAge(220)
				.emit(blob("earth").priority(Priority.HERO).burst(110).onGround().shape(Shape.disc(14)).velocity(Velocity.up(0.6F, 1.5F, 0.5F)).gravity(0.05F)
						.collide(0.2F, 0.5F, 1).life(40, 70).size(0.4F, 0.8F).color(EARTH).build())
				.emit(EmitterDef.builder("rocks").burst(40).onGround().shape(Shape.disc(12)).velocity(Velocity.up(0.7F, 1.5F, 0.5F)).gravity(0.06F)
						.collide(0.3F, 0.5F, 2).life(50, 80).size(0.4F, 0.8F).spin(-0.4F, 0.4F).layer(Layer.SMOKE).sprite(FxSprites.DUST).color(0x6A625A)
						.shade(0.75F).build())
				.emit(smoke("wall").burst(50).onGround().shape(Shape.ring(12, 4)).velocity(Velocity.radial(0.7F, 1.3F)).velocity(Velocity.up(0.05F, 0.25F, 0.3F))
						.drag(0.04F).life(70, 120).size(5, 8).color(DUST).alpha(0.6F).build())
				.build());

		// A creeper of the rain falling: a green sparkle streaming off it.
		FxEngine.register(EffectDef.builder(VfxIds.CREEPER_FALL).lod(150, 900)
				.emit(EmitterDef.builder("sparkle").burst(3).inherit(0.2F).shape(Shape.sphere(0.6F)).velocity(Velocity.random(0.02F, 0.08F)).life(10, 20)
						.size(0.12F, 0.22F).sprite(FxSprites.FLARE).color(CREEPER).alphaOverLife(TWINKLE).build())
				.build());

		// It goes off. A white-out; a ball of white blast puffs swelling out and going grey, as a creeper's does, only
		// a mountain of it; a ring of shock racing out over the land; a column and cap of smoke; green sparks raining.
		FxEngine.register(EffectDef.builder(VfxIds.MEGA_BOOM).lod(2000, 12000).maxAge(420)
				.cue(0, EffectDef.light(3.0F, 0xF0FFF0))
				.emit(flash("flash", 160, 30, 0xF8FFF4).offset(0, 50, 0).build())
				.emit(smoke("puffs").priority(Priority.HERO).burst(420).shape(Shape.sphere(18)).offset(0, 45, 0).velocity(Velocity.radial(0.8F, 2.6F)).drag(0.035F)
						.life(60, 120).size(8, 16).sizeOverLife(Curve.keys(0, 0.4F, 0.15F, 1, 1, 1.6F)).color(PUFF).alpha(0.85F).shade(0.95F)
						.emissive(Curve.keys(0, 1, 0.2F, 0.8F, 0.6F, 0.1F, 1, 0)).build())
				.emit(EmitterDef.builder("shock").priority(Priority.HERO).burst(1).offset(0, 1, 0).mode(Mode.FLAT).fixedRotation().size(600, 600).life(70, 70)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(0xE8FFE0).alpha(0.9F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("dust").burst(300).shape(Shape.ring(60, 10)).velocity(Velocity.radial(3.0F, 4.2F)).velocity(Velocity.up(0.05F, 0.3F, 0.2F)).drag(0.025F)
						.life(80, 140).size(8, 14).color(DUST).alpha(0.6F).build())
				.emit(20, smoke("column").burst(80).shape(Shape.column(14, 60)).velocity(Velocity.up(0.6F, 1.2F, 0.15F)).drag(0.02F).module(Module.curl(20, 0.01F))
						.life(160, 240).size(12, 18).color(PUFF).alpha(0.6F).shade(0.9F).build())
				.emit(40, smoke("cap").burst(90).shape(Shape.torus(40, 14)).offset(0, 140, 0).velocity(Velocity.radial(0.3F, 0.6F)).module(Module.roll(0.4F, 14, 0.06F))
						.life(180, 260).size(14, 22).color(PUFF).alpha(0.55F).shade(0.9F).build())
				.emit(EmitterDef.builder("sparks").burst(400).shape(Shape.sphere(20)).offset(0, 45, 0).velocity(Velocity.radial(1.5F, 3.5F)).gravity(0.03F).drag(0.01F)
						.life(60, 120).size(0.4F, 0.8F).mode(Mode.STRETCHED).stretch(2.5F).sprite(FxSprites.STREAK).color(CREEPER).build())
				.build());
	}

	// ------------------------------------------------------------------ Elder Wyrm

	private static void wyrm() {
		// Ash falling over everything and embers drifting through it, the whole time.
		int life = Wyrm.END;
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_ASH).lod(400, 3000).maxAge(life + 120)
				.emit(EmitterDef.builder("ash").rate(5, life - 60).shape(Shape.box(90, 6, 90)).offset(0, 30, 0).velocity(Velocity.up(-0.12F, -0.05F, 0.4F))
						.module(Module.wind(0.05F)).module(Module.jitter(0.01F)).life(160, 260).size(0.08F, 0.16F).spin(-0.1F, 0.1F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST).color(0x3A3430).shade(0.9F).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").rate(3, life - 60).shape(Shape.box(80, 10, 80)).offset(0, 12, 0).velocity(Velocity.up(0.02F, 0.08F, 0.6F))
						.module(Module.curl(12, 0.006F)).module(Module.wind(0.04F)).life(100, 160).size(0.1F, 0.2F).sprite(FxSprites.FLARE).color(EMBER)
						.alphaOverLife(FLICKER).build())
				.build());

		// The ground bursting as it claws its way out: rock and earth thrown high, lava spraying from below, dust.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_RISE).lod(500, 3200).maxAge(220)
				.cue(0, EffectDef.light(1.0F, 0xFF6020))
				.emit(blob("earth").priority(Priority.HERO).burst(90).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.6F, 1.5F, 0.5F)).gravity(0.05F)
						.collide(0.2F, 0.5F, 1).life(40, 70).size(0.4F, 0.8F).color(EARTH).build())
				.emit(EmitterDef.builder("lava").burst(50).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.6F, 1.3F, 0.4F)).gravity(0.05F)
						.collide(0.1F, 0.3F, 0).life(30, 50).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(EMBER).build())
				.emit(smoke("dust").burst(30).onGround().shape(Shape.ring(8, 4)).velocity(Velocity.radial(0.5F, 1.0F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F))
						.drag(0.04F).life(70, 120).size(5, 8).color(DUST).alpha(0.6F).build())
				.build());

		// The roar: rings of fire and sound pouring out of its jaws, embers blown ahead of them.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_ROAR).lod(600, 4000).maxAge(120)
				.cue(0, EffectDef.light(1.6F, 0xFF6020))
				.emit(EmitterDef.builder("rings").rate(1, 40).velocity(Velocity.along(1.6F, 1.8F, 0)).mode(Mode.FLAT).life(24, 30).size(8, 10).sizeOverLife(Curve.RISE)
						.sprite(FxSprites.SHOCK).color(0xFFA060).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("fire").burst(120).velocity(Velocity.along(1.2F, 2.4F, 0.4F)).drag(0.03F).life(20, 40).size(2, 4).sprite(FxSprites.FIRE, 2)
						.color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(160).velocity(Velocity.along(1.0F, 2.6F, 0.6F)).drag(0.02F).life(30, 60).size(0.2F, 0.4F)
						.sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(FLICKER).build())
				.build());

		// Its breath: a torrent of fire pouring out along the effect's direction, swelling and roiling as it goes.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_BREATH).lod(600, 4000)
				.emit(EmitterDef.builder("torrent").priority(Priority.HERO).burst(14).inherit(0.6F).shape(Shape.sphere(1.5F)).velocity(Velocity.along(2.2F, 3.0F, 0.12F))
						.drag(0.02F).module(Module.curl(6, 0.02F)).life(10, 16).size(1.5F, 2.5F).sizeOverLife(Curve.keys(0, 0.6F, 1, 3.2F)).sprite(FxSprites.FIRE, 2)
						.color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("core").burst(4).inherit(0.6F).velocity(Velocity.along(2.6F, 3.0F, 0.05F)).life(8, 12).size(1.2F, 1.8F)
						.sizeOverLife(Curve.RISE).sprite(FxSprites.GLOW).color(0xFFF4D0).alpha(0.6F).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(6).inherit(0.4F).velocity(Velocity.along(1.6F, 2.8F, 0.3F)).gravity(0.02F).life(14, 30).size(0.15F, 0.3F)
						.sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(FLICKER).build())
				.emit(smoke("smoke").burst(1).inherit(0.4F).velocity(Velocity.along(1.0F, 1.6F, 0.3F)).velocity(Velocity.up(0.1F, 0.2F, 0.3F)).drag(0.03F)
						.life(50, 90).size(4, 7).color(SOOT).alpha(0.45F).build())
				.build());

		// The breath striking: a wall of fire boiling up, molten rock flying, a column of black smoke.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_SCORCH).lod(600, 4000).maxAge(200)
				.cue(0, EffectDef.light(1.4F, 0xFF6020))
				.emit(EmitterDef.builder("wall").priority(Priority.HERO).burst(18).onGround().shape(Shape.disc(7)).velocity(Velocity.up(0.3F, 0.7F, 0.3F))
						.module(Module.buoyancy(0.02F)).life(20, 36).size(3, 5).sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("molten").burst(30).onGround().shape(Shape.disc(5)).velocity(Velocity.up(0.6F, 1.4F, 0.5F)).velocity(Velocity.along(0.2F, 0.6F, 0.6F))
						.gravity(0.05F).collide(0.1F, 0.3F, 0).life(30, 50).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(EMBER).build())
				.emit(smoke("column").burst(6).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.4F, 0.8F, 0.2F)).drag(0.02F).module(Module.curl(12, 0.01F))
						.life(120, 200).size(7, 11).sizeOverLife(Curve.RISE).color(SOOT).alpha(0.6F).emissive(Curve.keys(0, 1, 0.2F, 0.4F, 1, 0)).build())
				.build());

		// A wingbeat: a ring of dust and torn leaves racing out over the ground, a shock in the air.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_GALE).lod(600, 4000).maxAge(200)
				.emit(smoke("dust").priority(Priority.HERO).burst(160).onGround().shape(Shape.ring(12, 6)).velocity(Velocity.radial(1.8F, 2.6F))
						.velocity(Velocity.up(0.05F, 0.25F, 0.2F)).drag(0.025F).life(60, 100).size(5, 8).color(DUST).alpha(0.55F).build())
				.emit(EmitterDef.builder("leaves").burst(120).onGround().shape(Shape.ring(14, 8)).velocity(Velocity.radial(1.6F, 2.6F)).velocity(Velocity.up(0.2F, 0.6F, 0.4F))
						.gravity(0.02F).spin(-0.3F, 0.3F).life(50, 90).size(0.2F, 0.35F).layer(Layer.SMOKE).sprite(FxSprites.DUST).color(0x3E6A22).shade(0.8F).build())
				.emit(EmitterDef.builder("shock").burst(1).onGround().offset(0, 0.5F, 0).mode(Mode.FLAT).fixedRotation().size(150, 150).life(36, 36).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xE0D8C8).alpha(0.45F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The nova: a dome of fire bursting out of the ground, flames racing out over the land, a shock ring, and a
		// mushroom of black smoke shot through with fire climbing over the crater.
		FxEngine.register(EffectDef.builder(VfxIds.WYRM_NOVA).lod(1500, 9000).maxAge(420)
				.cue(0, EffectDef.light(3.0F, 0xFFA040))
				.emit(flash("flash", 120, 26, 0xFFF0C0).offset(0, 20, 0).build())
				.emit(EmitterDef.builder("dome").priority(Priority.HERO).burst(260).shape(Shape.dome(16)).velocity(Velocity.radial(1.2F, 2.6F)).drag(0.03F).life(30, 60)
						.size(6, 12).sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("race").priority(Priority.HERO).burst(220).onGround().shape(Shape.ring(14, 4)).velocity(Velocity.radial(2.2F, 3.2F))
						.velocity(Velocity.up(0.05F, 0.3F, 0.2F)).drag(0.02F).life(40, 70).size(4, 8).sprite(FxSprites.FIRE, 2).color(FIRE).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("shock").burst(1).offset(0, 1, 0).mode(Mode.FLAT).fixedRotation().size(260, 260).life(50, 50).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xFFC080).alpha(0.85F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(10, smoke("stem").burst(80).shape(Shape.column(12, 50)).velocity(Velocity.up(0.6F, 1.2F, 0.15F)).drag(0.02F).module(Module.curl(18, 0.01F))
						.life(160, 240).size(10, 16).color(SOOT).alpha(0.6F).emissive(Curve.keys(0, 1, 0.3F, 0.5F, 1, 0)).build())
				.emit(30, smoke("cap").burst(90).shape(Shape.torus(34, 12)).offset(0, 110, 0).velocity(Velocity.radial(0.3F, 0.6F)).module(Module.roll(0.4F, 12, 0.06F))
						.life(180, 260).size(12, 20).color(SOOT).alpha(0.55F).emissive(Curve.keys(0, 0.9F, 0.3F, 0.4F, 1, 0)).build())
				.emit(EmitterDef.builder("embers").burst(300).shape(Shape.dome(20)).velocity(Velocity.radial(1.0F, 3.0F)).velocity(Velocity.up(0.5F, 1.5F, 0.4F))
						.gravity(0.02F).life(60, 120).size(0.3F, 0.6F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(FLICKER).build())
				.build());
	}
}
