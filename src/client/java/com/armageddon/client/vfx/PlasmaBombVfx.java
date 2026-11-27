package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.util.PlasmaShapes.Arch;
import com.armageddon.util.PlasmaShapes.Bottle;
import com.armageddon.util.PlasmaShapes.Geyser;
import com.armageddon.util.PlasmaShapes.Pinch;
import com.armageddon.util.PlasmaShapes.Plasmoid;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.FLICKER;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/**
 * The plasma bombs, built almost entirely from the engine. Each has its own way of moving: ball lightning hopping
 * with crackles ({@link Module#jitter}), a twisted flux rope ({@link Module#arch}), a geyser with a rolling cap
 * ({@link Module#roll}) and rain that splashes where it lands, a column crushed into beads ({@link Module#pinch}) and
 * plasma streaming along a magnetic bottle's field lines ({@link Module#dipole}).
 */
final class PlasmaBombVfx {
	private PlasmaBombVfx() {
	}

	static final Gradient VIOLET = Gradient.of(0, 0xFFFFFF, 0.25F, 0xE6B8FF, 0.6F, 0xA050FF, 1, argb(0x4010A0, 0));
	static final Gradient SOLAR = Gradient.of(0, 0xFFFFE8, 0.3F, 0xFFD060, 0.65F, 0xFF7010, 1, argb(0x801000, 0));
	static final Gradient CYAN = Gradient.of(0, 0xFFFFFF, 0.3F, 0xB0FFFF, 0.65F, 0x30D0FF, 1, argb(0x0040A0, 0));
	static final Gradient ARC = Gradient.of(0, 0xFFFFFF, 0.4F, 0xC8DCFF, 0.8F, 0x6A88FF, 1, argb(0x2020A0, 0));
	static final Gradient MAGENTA = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFB0F0, 0.7F, 0xFF40D0, 1, argb(0x800060, 0));
	static final Gradient SCORCH = Gradient.of(0, 0x2A2420, 0.5F, 0x4A4038, 1, 0x6A625A);
	static final Gradient STEAM = Gradient.of(0, 0xF0FAFF, 1, 0xC8D8E0);

	static void register() {
		debris();
		plasmoids();
		arch();
		geyser();
		pinch();
		bottle();
	}

	private static EmitterDef.Builder spark(String name, Gradient g) {
		return EmitterDef.builder(name).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(g).alphaOverLife(Curve.FADE_OUT);
	}

	private static EmitterDef.Builder orb(String name, Gradient g) {
		return EmitterDef.builder(name).sprite(FxSprites.FLARE).color(g).alphaOverLife(Curve.FADE);
	}

	/** A blast of plasma on the ground: a flash, a sphere of streaks, a shock ring, smoke and embers. */
	private static EffectDef.Builder pop(String id, Gradient g, int light, float size) {
		return EffectDef.builder(id).lod(300, 2000).maxAge(140)
				.cue(0, EffectDef.light(1.0F, light))
				.emit(flash("flash", 10 * size, 8, light).build())
				.emit(spark("streaks", g).priority(Priority.HERO).burst(70 * size).velocity(Velocity.random(0.5F, 1.1F)).drag(0.08F).life(8, 16)
						.size(0.12F, 0.22F).stretch(2.5F).build())
				.emit(orb("blobs", g).burst(30 * size).shape(Shape.sphere(1.5F)).velocity(Velocity.random(0.1F, 0.4F)).velocity(Velocity.up(0.1F, 0.3F, 0.6F))
						.gravity(0.03F).collide(0.2F, 0.5F, 1).life(14, 30).size(0.25F, 0.45F).build())
				.emit(EmitterDef.builder("ring").burst(1).offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(8 * size, 8 * size).life(14, 14)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).color(light).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("smoke").burst(5 * size).shape(Shape.sphere(1.5F)).velocity(Velocity.up(0.05F, 0.2F, 0.6F)).module(Module.curl(6, 0.006F)).life(50, 90)
						.size(2, 3.5F).color(SCORCH).alpha(0.55F).build());
	}

	// ------------------------------------------------------------------ shared

	/** White-hot to the bomb's own colour: the tint is multiplied in. */
	static final Gradient HOT = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFFFFF, 0.7F, 0xC8C8C8, 1, argb(0x606060, 0));

	private static void debris() {
		// Torn-up land in flight: it burns, shedding plasma (tinted with the bomb's colour) and smoke.
		FxEngine.register(EffectDef.builder(VfxIds.DEBRIS_EMBER).lod(200, 1400)
				.emit(EmitterDef.builder("flame").burst(2).inherit(0.3F).shape(Shape.sphere(0.6F)).life(6, 12).size(0.6F, 1.0F).sprite(FxSprites.FIRE, 2)
						.color(HOT).tinted().alphaOverLife(Curve.FADE).build())
				.emit(orb("sparks", HOT).burst(2).inherit(0.1F).velocity(Velocity.random(0.05F, 0.15F)).gravity(0.02F).life(8, 16).size(0.1F, 0.18F).tinted().build())
				.emit(smoke("smoke").burst(1).inherit(0.05F).velocity(Velocity.up(0.02F, 0.06F, 0.4F)).life(30, 50).size(0.8F, 1.4F).color(SCORCH).alpha(0.4F).build())
				.build());

		// A block of it coming down: it breaks up in a puff of dust, a few embers dying in it. Quiet on purpose.
		FxEngine.register(EffectDef.builder(VfxIds.DEBRIS_IMPACT).lod(200, 1200).maxAge(100)
				.emit(smoke("dust").burst(3).shape(Shape.disc(1.2F)).velocity(Velocity.up(0.03F, 0.1F, 0.5F)).velocity(Velocity.radial(0.04F, 0.12F)).life(40, 70)
						.size(1.6F, 2.6F).color(Gradient.of(0, 0x7A6A56, 1, 0x9A8C78)).alpha(0.45F).build())
				.emit(orb("embers", HOT).burst(4).velocity(Velocity.up(0.08F, 0.25F, 0.8F)).gravity(0.02F).life(10, 20).size(0.08F, 0.14F).tinted()
						.alphaOverLife(VfxLibrary.FLICKER).build())
				.build());
	}

	// ------------------------------------------------------------------ Plasmoid Swarm

	private static void plasmoids() {
		// The core: a churning ball of violet plasma throwing out crackles, its glow throbbing.
		FxEngine.register(EffectDef.builder(VfxIds.PLASMOID_CORE).lod(250, 1600).maxAge(Plasmoid.BURST + 4)
				.emit(orb("churn", VIOLET).priority(Priority.HERO).rate(3, Plasmoid.BURST).shape(Shape.sphere(1.8F)).velocity(Velocity.random(0.02F, 0.08F))
						.module(Module.curl(2, 0.03F)).module(Module.attract(0.02F, 0.2F)).life(10, 20).size(0.6F, 1.2F).build())
				.emit(spark("crackle", ARC).rate(1.6F, Plasmoid.BURST).shape(Shape.shell(1.6F)).velocity(Velocity.radial(0.6F, 1.2F)).module(Module.jitter(0.25F))
						.life(3, 6).size(0.06F, 0.1F).stretch(3).build())
				.emit(EmitterDef.builder("throb").rate(0.5F, Plasmoid.BURST).life(6, 8).size(4, 5.5F).sprite(FxSprites.GLOW).color(0xC890FF).alpha(0.3F)
						.alphaOverLife(Curve.BELL).build())
				.emit(spark("spit", VIOLET).rate(2.5F, Plasmoid.BURST, Curve.RISE).shape(Shape.shell(2)).velocity(Velocity.radial(0.4F, 0.9F)).gravity(0.03F)
						.module(Module.jitter(0.08F)).collide(0.5F, 0.6F, 2).life(12, 26).size(0.08F, 0.14F).stretch(2).build())
				.build());

		// A plasmoid slamming down: sparks bouncing off the ground, a scorch puff, a flat crackle of light.
		FxEngine.register(EffectDef.builder(VfxIds.PLASMOID_SKIP).lod(200, 1400).maxAge(80)
				.emit(spark("sparks", ARC).burst(22).onGround().velocity(Velocity.up(0.3F, 0.8F, 0.9F)).gravity(0.04F).collide(0.5F, 0.6F, 2).module(Module.jitter(0.05F))
						.life(8, 20).size(0.08F, 0.14F).stretch(2.5F).build())
				.emit(EmitterDef.builder("crackle").burst(1).onGround().offset(0, 0.2F, 0).mode(Mode.FLAT).fixedRotation().size(3, 4).life(6, 8).sprite(FxSprites.SHOCK)
						.color(0xC8A0FF).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("scorch").burst(2).onGround().velocity(Velocity.up(0.04F, 0.1F, 0.5F)).life(30, 60).size(1.5F, 2.5F).color(SCORCH).alpha(0.5F).build())
				.build());

		// A plasmoid's wake: violet wisps peeling off it, sparks dropping away.
		FxEngine.register(EffectDef.builder(VfxIds.PLASMOID_TRAIL).lod(150, 900)
				.emit(EmitterDef.builder("wisp").burst(3).inherit(0.3F).shape(Shape.sphere(0.6F)).life(8, 14).size(0.4F, 0.7F).sprite(FxSprites.WISP)
						.color(VIOLET).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(spark("sparks", ARC).burst(2).velocity(Velocity.random(0.1F, 0.3F)).gravity(0.02F).module(Module.jitter(0.05F)).life(5, 10)
						.size(0.05F, 0.09F).stretch(2).build())
				.build());

		FxEngine.register(pop(VfxIds.PLASMOID_POP, VIOLET, 0xD8B0FF, 1.0F).build());

		// The core going: a burst of violet, a web of lightning thrown out over the ground, a ring rolling away.
		FxEngine.register(pop(VfxIds.PLASMOID_BURST, VIOLET, 0xE0C0FF, 2.6F)
				.emit(spark("web", ARC).priority(Priority.HERO).burst(60).velocity(Velocity.random(1.2F, 2.0F)).module(Module.jitter(0.4F)).life(6, 12)
						.size(0.1F, 0.16F).stretch(4).build())
				.build());
	}

	// ------------------------------------------------------------------ Solar Arch

	private static void arch() {
		// A footpoint: plasma fountaining and falling back, flames boiling round it, a hot glow.
		int life = Arch.SNAP - Arch.FEET;
		FxEngine.register(EffectDef.builder(VfxIds.ARCH_FOOT).lod(300, 2000).maxAge(life + 60)
				.cue(0, EffectDef.light(1.0F, 0xFFC060))
				.emit(flash("flash", 12, 10, 0xFFE0A0).build())
				.emit(orb("fountain", SOLAR).rate(7, life).shape(Shape.disc(1.5F)).velocity(Velocity.up(0.4F, 0.8F, 0.45F)).gravity(0.03F).collide(0.1F, 0.4F, 0)
						.life(20, 36).size(0.25F, 0.45F).build())
				.emit(EmitterDef.builder("boil").rate(3, life).shape(Shape.disc(2.5F)).velocity(Velocity.up(0.05F, 0.12F, 0.3F)).module(Module.buoyancy(0.01F))
						.life(14, 24).size(1.0F, 1.8F).sprite(FxSprites.FIRE, 2).color(SOLAR).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").rate(0.5F, life).offset(0, 1.5F, 0).life(8, 10).size(5, 6).sprite(FxSprites.GLOW).color(0xFFB040).alpha(0.35F)
						.alphaOverLife(Curve.BELL).build())
				.emit(smoke("smoke").rate(0.4F, life).shape(Shape.disc(2)).velocity(Velocity.up(0.08F, 0.16F, 0.3F)).module(Module.curl(8, 0.006F)).life(80, 120)
						.size(3, 5).color(SCORCH).alpha(0.4F).build())
				.build());

		// The rope itself: thousands of points of plasma streaming foot to foot round a twisting core, bright filaments
		// winding tighter through it, drops of plasma raining off it.
		int rope = Arch.SNAP - Arch.ARCH;
		int lie = rope + Arch.LASH + 20;
		EmitterDef drip = orb("drip", SOLAR).burst(1).velocity(Velocity.up(-0.1F, 0.0F, 0.4F)).gravity(0.03F).collide(0.1F, 0.3F, 0).life(20, 40)
				.size(0.15F, 0.25F).build();
		float span = (float) Arch.SPAN, h0 = (float) Arch.HEIGHT, rise = (float) Arch.RISE;
		FxEngine.register(EffectDef.builder(VfxIds.ARCH_ROPE).lod(400, 2600).maxAge(lie + 70)
				.emit(orb("rope", SOLAR).priority(Priority.HERO).max(4000).rate(42, lie).module(Module.arch(span, h0, rise, 2.4F, 1.5F, rope, Arch.LASH))
						.life(40, 60).size(0.35F, 0.6F).onDeath(0.06F, drip).build())
				.emit(spark("filaments", Gradient.of(0, 0xFFFFFF, 0.5F, 0xFFF0B0, 1, argb(0xFFA040, 0))).max(1200).rate(10, lie)
						.module(Module.arch(span, h0, rise, 1.1F, 3.2F, rope, Arch.LASH)).life(50, 70).size(0.12F, 0.18F).stretch(1.5F)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").max(400).rate(2, lie).module(Module.arch(span, h0, rise, 0.2F, 0, rope, Arch.LASH))
						.life(40, 60).size(3.5F, 4.5F).sprite(FxSprites.GLOW).color(0xFFA030).alpha(0.22F).alphaOverLife(Curve.FADE).build())
				.build());

		// The rope slamming down: molten rock sprayed up along the trench, flames, a wall of smoke.
		FxEngine.register(EffectDef.builder(VfxIds.ARCH_LASH).lod(400, 2600).maxAge(160)
				.cue(0, EffectDef.light(1.2F, 0xFFA040))
				.emit(orb("spray", SOLAR).priority(Priority.HERO).burst(50).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.5F, 1.2F, 0.5F)).gravity(0.05F)
						.collide(0.1F, 0.3F, 0).life(20, 40).size(0.25F, 0.45F).build())
				.emit(EmitterDef.builder("flames").burst(8).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.3F, 0.4F)).module(Module.buoyancy(0.012F))
						.life(14, 26).size(1.5F, 2.5F).sprite(FxSprites.FIRE, 2).color(SOLAR).alphaOverLife(Curve.FADE).build())
				.emit(smoke("wall").burst(5).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.15F, 0.35F, 0.3F)).module(Module.curl(8, 0.008F)).life(70, 120)
						.size(4, 6).color(SCORCH).alpha(0.55F).emissive(Curve.keys(0, 0.9F, 0.3F, 0.2F, 1, 0)).build())
				.build());

		// The eruption: the arch blown forward as a bubble of plasma, streamers raking the land, a ring of shock.
		EmitterDef scorch = EmitterDef.builder("scorch").burst(4).velocity(Velocity.up(0.05F, 0.15F, 0.5F)).life(10, 18).size(0.8F, 1.4F)
				.sprite(FxSprites.FIRE, 2).color(SOLAR).alphaOverLife(Curve.FADE).build();
		FxEngine.register(EffectDef.builder(VfxIds.ARCH_CME).lod(600, 4000).maxAge(220)
				.cue(0, EffectDef.light(2.0F, 0xFFC060))
				.emit(flash("flash", 50, 18, 0xFFF0C0).build())
				.emit(orb("bubble", SOLAR).priority(Priority.HERO).burst(450).shape(Shape.shell(6)).velocity(Velocity.radial(0.5F, 0.9F))
						.velocity(Velocity.along(0.9F, 1.4F, 0.35F)).drag(0.012F).life(40, 70).size(0.6F, 1.1F).build())
				.emit(EmitterDef.builder("flames").burst(80).shape(Shape.sphere(5)).velocity(Velocity.along(0.6F, 1.2F, 0.5F)).drag(0.02F).life(20, 40).size(3, 5)
						.sprite(FxSprites.FIRE, 2).color(SOLAR).alphaOverLife(Curve.FADE).build())
				.emit(spark("streamers", SOLAR).burst(160).velocity(Velocity.along(1.4F, 2.4F, 0.5F)).gravity(0.03F).collide(0.1F, 0.3F, 0).life(30, 55)
						.size(0.2F, 0.35F).stretch(3).onHit(0.35F, scorch).build())
				.emit(EmitterDef.builder("shock").burst(1).mode(Mode.FLAT).fixedRotation().size(70, 70).life(30, 30).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xFFD080).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("plume").burst(40).shape(Shape.sphere(6)).velocity(Velocity.along(0.4F, 0.8F, 0.6F)).velocity(Velocity.up(0.1F, 0.3F, 0.5F)).drag(0.02F)
						.module(Module.curl(14, 0.01F)).life(90, 150).size(6, 10).sizeOverLife(Curve.RISE).color(SCORCH).alpha(0.5F)
						.emissive(Curve.keys(0, 1, 0.25F, 0.3F, 1, 0)).build())
				.build());

		// The eruption's front reaching a spot: flames licking up, embers, smoke.
		FxEngine.register(EffectDef.builder(VfxIds.ARCH_SCORCH).lod(250, 1800)
				.emit(EmitterDef.builder("flames").burst(6).shape(Shape.disc(3)).velocity(Velocity.up(0.1F, 0.25F, 0.4F)).module(Module.buoyancy(0.01F)).life(12, 22)
						.size(1.2F, 2.2F).sprite(FxSprites.FIRE, 2).color(SOLAR).alphaOverLife(Curve.FADE).build())
				.emit(orb("embers", SOLAR).burst(10).shape(Shape.disc(3)).velocity(Velocity.up(0.2F, 0.5F, 0.6F)).gravity(0.02F).module(Module.curl(4, 0.01F))
						.life(20, 40).size(0.1F, 0.18F).alphaOverLife(FLICKER).build())
				.emit(smoke("smoke").burst(2).shape(Shape.disc(3)).velocity(Velocity.up(0.08F, 0.16F, 0.3F)).life(60, 100).size(3, 5).color(SCORCH).alpha(0.5F).build())
				.build());
	}

	// ------------------------------------------------------------------ Fusion Geyser

	private static void geyser() {
		// The pocket of plasma building under the ground: a glow seeping up, steam, sparks spitting from the cracks.
		int build = Geyser.ERUPT - Geyser.BULGE;
		FxEngine.register(EffectDef.builder(VfxIds.GEYSER_BULGE).lod(300, 2000).maxAge(build + 40)
				.emit(EmitterDef.builder("glow").rate(0.6F, build).offset(0, 0.5F, 0).mode(Mode.FLAT).fixedRotation().life(10, 14).size(6, 9)
						.sizeOverLife(Curve.RISE).sprite(FxSprites.GLOW).color(0x40E0FF).alpha(0.4F).alphaOverLife(Curve.BELL).build())
				.emit(smoke("steam").rate(1.2F, build, Curve.RISE).shape(Shape.disc(7)).velocity(Velocity.up(0.08F, 0.18F, 0.3F)).module(Module.curl(6, 0.006F))
						.life(50, 80).size(2, 3.5F).color(STEAM).alpha(0.4F).shade(1).build())
				.emit(spark("spits", CYAN).rate(3, build, Curve.RISE).shape(Shape.disc(6)).velocity(Velocity.up(0.4F, 0.9F, 0.3F)).gravity(0.04F).life(10, 20)
						.size(0.1F, 0.18F).stretch(2).build())
				.build());

		// The eruption: a column of plasma eighty blocks high spreading into a rolling cap, steam pouring off it.
		int erupt = Geyser.RAIN_END - Geyser.ERUPT;
		FxEngine.register(EffectDef.builder(VfxIds.GEYSER_ERUPT).lod(600, 4000).maxAge(erupt + 120)
				.cue(0, EffectDef.light(2.0F, 0x80F0FF))
				.emit(flash("flash", 40, 16, 0xD0FFFF).offset(0, 10, 0).build())
				.emit(EmitterDef.builder("shock").burst(1).offset(0, 0.4F, 0).mode(Mode.FLAT).fixedRotation().size(50, 50).life(26, 26).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xA0F8FF).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(orb("column", CYAN).priority(Priority.HERO).max(3000).rate(34, erupt - 60, Curve.keys(0, 1, 0.8F, 1, 1, 0.2F)).shape(Shape.disc(3))
						.velocity(Velocity.up(1.7F, 2.2F, 0.07F)).drag(0.012F).life(38, 50).size(1.2F, 2.0F).build())
				.emit(EmitterDef.builder("core").rate(6, erupt - 60).shape(Shape.disc(1.2F)).velocity(Velocity.up(1.9F, 2.3F, 0.03F)).drag(0.012F).life(30, 40)
						.size(3, 4).sprite(FxSprites.GLOW).color(0xE8FFFF).alpha(0.4F).alphaOverLife(Curve.FADE).build())
				.emit(20, orb("cap", CYAN).max(2000).rate(12, erupt - 70).shape(Shape.torus(10, 4)).offset(0, 70, 0).velocity(Velocity.radial(0.15F, 0.35F))
						.module(Module.roll(0.35F, 4, 0.04F)).life(50, 80).size(1.2F, 2.2F).build())
				.emit(20, smoke("capsteam").rate(2, erupt - 70).shape(Shape.torus(11, 5)).offset(0, 70, 0).velocity(Velocity.radial(0.1F, 0.3F))
						.module(Module.roll(0.3F, 5, 0.04F)).life(80, 140).size(6, 9).color(STEAM).alpha(0.4F).shade(1).emissive(Curve.keys(0, 0.9F, 1, 0.3F)).build())
				.emit(smoke("steam").rate(2, erupt - 40).shape(Shape.disc(5)).velocity(Velocity.up(0.4F, 0.8F, 0.2F)).drag(0.02F).module(Module.curl(10, 0.01F))
						.life(70, 120).size(4, 7).sizeOverLife(Curve.RISE).color(STEAM).alpha(0.35F).shade(1).emissive(Curve.keys(0, 0.8F, 1, 0.1F)).build())
				.build());

		// The rain: drops of plasma streaking down out of the cap, splashing where they land.
		int rain = Geyser.RAIN_END - Geyser.RAIN;
		EmitterDef splash = spark("splash", CYAN).burst(5).velocity(Velocity.up(0.2F, 0.45F, 0.8F)).gravity(0.04F).life(6, 12).size(0.08F, 0.14F).stretch(2)
				.build();
		FxEngine.register(EffectDef.builder(VfxIds.GEYSER_RAIN).lod(500, 3000).maxAge(rain + 120)
				.emit(spark("drops", CYAN).priority(Priority.HERO).max(2500).rate(26, rain, Curve.keys(0, 0.4F, 0.2F, 1, 0.8F, 1, 1, 0)).shape(Shape.disc((float) Geyser.R))
						.offset(0, 18, 0).velocity(Velocity.up(-0.6F, -0.35F, 0.25F)).gravity(0.03F).collide(0.0F, 0.2F, 0).life(70, 100).size(0.22F, 0.38F)
						.stretch(2.5F).alphaOverLife(Curve.ONE).onHit(0.5F, splash).build())
				.build());

		// A drop landing: a splash of cyan sparks, a lick of flame, a puff of steam.
		FxEngine.register(EffectDef.builder(VfxIds.GEYSER_SPLASH).lod(200, 1200)
				.emit(spark("sparks", CYAN).burst(12).velocity(Velocity.up(0.2F, 0.5F, 0.8F)).gravity(0.04F).life(8, 16).size(0.08F, 0.15F).stretch(2).build())
				.emit(EmitterDef.builder("flame").burst(2).velocity(Velocity.up(0.05F, 0.12F, 0.3F)).life(10, 18).size(0.8F, 1.2F).sprite(FxSprites.FIRE, 2)
						.color(Gradient.of(0, 0xE0FFFF, 0.4F, 0x60D8FF, 1, argb(0x1040A0, 0))).alphaOverLife(Curve.FADE).build())
				.emit(smoke("steam").burst(1).velocity(Velocity.up(0.05F, 0.1F, 0.3F)).life(40, 60).size(1.5F, 2.5F).color(STEAM).alpha(0.35F).shade(1).build())
				.build());
	}

	// ------------------------------------------------------------------ Z-Pinch

	private static void pinch() {
		// The column: plasma held to a pinching column that breaks into beads, a white-hot core down its middle,
		// magnetic rings sliding along it and closing, crackles jumping off it.
		int life = Pinch.COLLAPSE;
		int squeeze = Pinch.COLLAPSE - 40;
		FxEngine.register(EffectDef.builder(VfxIds.PINCH_COLUMN).lod(500, 3200).maxAge(life + 30)
				.cue(0, EffectDef.light(1.2F, 0xB0C8FF))
				.emit(orb("plasma", ARC).priority(Priority.HERO).max(3500).rate(55, life).span().shape(Shape.disc((float) Pinch.RADIUS))
						.velocity(Velocity.random(0.0F, 0.05F)).module(Module.pinch((float) Pinch.RADIUS, (float) Pinch.WAVELENGTH, squeeze, 0.28F))
						.life(16, 26).size(0.4F, 0.7F).build())
				.emit(EmitterDef.builder("core").rate(8, life).span().life(6, 10).size(1.4F, 2.4F).sprite(FxSprites.GLOW).color(0xF4F8FF).alpha(0.5F)
						.alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("rings").rate(1.4F, life).span().mode(Mode.FLAT).fixedRotation().life(30, 40).size(8, 9)
						.sizeOverLife(Curve.keys(0, 1.3F, 1, 0.3F)).sprite(FxSprites.RING).color(0x88A8FF).alpha(0.6F).alphaOverLife(Curve.BELL).build())
				.emit(spark("crackle", ARC).rate(5, life).span().shape(Shape.shell(2)).velocity(Velocity.radial(0.5F, 1.0F)).module(Module.jitter(0.3F)).life(3, 6)
						.size(0.06F, 0.1F).stretch(3).build())
				.build());

		// A bead squeezed out: a white-hot ball flying to the ground, shedding sparks.
		EmitterDef shed = spark("shed", ARC).burst(3).inherit(0.1F).velocity(Velocity.random(0.05F, 0.2F)).life(6, 12).size(0.08F, 0.14F).stretch(2).build();
		FxEngine.register(EffectDef.builder(VfxIds.PINCH_BEAD).lod(400, 2600).maxAge(Pinch.BEAD_FLIGHT + 20)
				.emit(EmitterDef.builder("bead").priority(Priority.HERO).burst(1).velocity(Velocity.along(1, 1, 0)).life(Pinch.BEAD_FLIGHT, Pinch.BEAD_FLIGHT)
						.size(1.3F, 1.3F).layer(Layer.GLOW_TOP).sprite(FxSprites.GLOW).color(0xF0F4FF).every(1, shed).build())
				.emit(EmitterDef.builder("halo").burst(1).velocity(Velocity.along(1, 1, 0)).life(Pinch.BEAD_FLIGHT, Pinch.BEAD_FLIGHT).size(3.2F, 3.2F)
						.sprite(FxSprites.GLOW).color(0x7090FF).alpha(0.45F).build())
				.build());

		FxEngine.register(pop(VfxIds.PINCH_POP, ARC, 0xC0D0FF, 1.6F).build());

		// The collapse: the column falls in on itself, then a fusion flash, a sphere of streaks and a shock ring
		// racing out over the ground with a wall of dust behind it.
		FxEngine.register(EffectDef.builder(VfxIds.PINCH_COLLAPSE).lod(800, 5000).maxAge(220)
				.cue(0, EffectDef.light(2.5F, 0xE0E8FF))
				.emit(EmitterDef.builder("implode").priority(Priority.HERO).burst(400).shape(Shape.column(10, (float) Pinch.HEIGHT))
						.module(Module.attract(0.08F, 2)).life(10, 14).size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(ARC).build())
				.emit(10, flash("flash", 60, 20, 0xF4F8FF).offset(0, 20, 0).build())
				.emit(10, spark("nova", ARC).priority(Priority.HERO).burst(500).shape(Shape.sphere(4)).offset(0, 18, 0).velocity(Velocity.radial(1.2F, 2.4F))
						.drag(0.02F).life(30, 60).size(0.2F, 0.4F).stretch(3).build())
				.emit(10, EmitterDef.builder("shock").burst(1).offset(0, 0.5F, 0).mode(Mode.FLAT).fixedRotation().size(120, 120).life(40, 40).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xD0E0FF).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(10, smoke("dust").burst(140).shape(Shape.ring(8, 4)).velocity(Velocity.radial(1.4F, 1.8F)).velocity(Velocity.up(0.05F, 0.25F, 0.2F)).drag(0.03F)
						.life(60, 110).size(4, 7).color(Gradient.of(0, 0x8A7A62, 1, 0xA09078)).alpha(0.6F).build())
				.emit(10, smoke("stem").burst(40).shape(Shape.column(5, 40)).velocity(Velocity.up(0.2F, 0.5F, 0.2F)).drag(0.02F).module(Module.curl(12, 0.01F))
						.life(90, 150).size(6, 9).color(SCORCH).alpha(0.5F).emissive(Curve.keys(0, 1, 0.3F, 0.2F, 1, 0)).build())
				.build());
	}

	// ------------------------------------------------------------------ Magnetic Bottle

	private static void bottle() {
		// Plasma streaming along the bottle's field lines, out of one pole and round into the other.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_FIELD).lod(300, 2000)
				.emit(spark("lines", MAGENTA).priority(Priority.HERO).burst(46).shape(Shape.shell((float) Bottle.SUN * 1.05F))
						.module(Module.dipole((float) Bottle.CAGE, 0.55F)).life(30, 46).size(0.12F, 0.2F).stretch(3).alphaOverLife(Curve.FADE).build())
				.emit(orb("motes", MAGENTA).burst(10).shape(Shape.shell((float) Bottle.SUN * 1.2F)).module(Module.dipole((float) Bottle.CAGE, 0.35F)).life(30, 50)
						.size(0.2F, 0.35F).alphaOverLife(TWINKLE).build())
				.build());

		// The cage failing: field lines whipping loose in a burst of sparks.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_SNAP).lod(400, 2600).maxAge(80)
				.cue(0, EffectDef.light(1.6F, 0xFF90E8))
				.emit(flash("flash", 30, 12, 0xFFE0F8).build())
				.emit(spark("whips", MAGENTA).priority(Priority.HERO).burst(240).shape(Shape.shell((float) Bottle.CAGE)).velocity(Velocity.radial(0.8F, 1.6F))
						.module(Module.jitter(0.2F)).drag(0.04F).life(12, 26).size(0.12F, 0.22F).stretch(3).build())
				.build());

		// A polar jet: a lance of plasma blasting out of the pole with a soft sheath round it.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_JET).lod(500, 3200).maxAge(100)
				.emit(orb("jet", MAGENTA).priority(Priority.HERO).max(2500).rate(40, 60, Curve.keys(0, 1, 0.7F, 1, 1, 0)).shape(Shape.sphere(1.2F))
						.velocity(Velocity.along(2.4F, 3.2F, 0.04F)).life(18, 28).size(0.8F, 1.4F).build())
				.emit(EmitterDef.builder("sheath").rate(10, 60, Curve.FALL).velocity(Velocity.along(1.4F, 2.0F, 0.12F)).life(14, 24).size(2, 3)
						.sprite(FxSprites.GLOW).color(0xFF70E0).alpha(0.3F).alphaOverLife(Curve.FADE).build())
				.build());

		// A field line lashing the ground: molten rock thrown up along it, magenta sparks, smoke.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_LASH).lod(400, 2600).maxAge(140)
				.cue(0, EffectDef.light(0.9F, 0xFF70E0))
				.emit(orb("spray", MAGENTA).priority(Priority.HERO).burst(40).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.up(0.5F, 1.2F, 0.5F)).gravity(0.05F)
						.collide(0.1F, 0.3F, 0).life(18, 36).size(0.25F, 0.4F).build())
				.emit(spark("whip", MAGENTA).burst(24).onGround().velocity(Velocity.up(0.6F, 1.4F, 0.3F)).module(Module.jitter(0.1F)).life(6, 12).size(0.12F, 0.2F)
						.stretch(3).build())
				.emit(smoke("smoke").burst(4).onGround().shape(Shape.disc(2.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F)).module(Module.curl(8, 0.008F)).life(60, 100)
						.size(3, 5).color(SCORCH).alpha(0.5F).emissive(Curve.keys(0, 0.9F, 0.3F, 0.1F, 1, 0)).build())
				.build());

		// The nova: the emptied sun falls in on itself, then a white-out, a sphere of magenta streaks, a ring of
		// light racing out over the ground, a mushroom of smoke climbing out of the crater.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_NOVA).lod(800, 5000).maxAge(260)
				.cue(0, EffectDef.light(2.5F, 0xFFA0F0))
				.emit(EmitterDef.builder("implode").priority(Priority.HERO).burst(300).shape(Shape.shell(14)).module(Module.attract(0.1F, 1.5F)).life(10, 14)
						.size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(MAGENTA).build())
				.emit(10, flash("flash", 60, 22, 0xFFF0FC).build())
				.emit(10, spark("nova", MAGENTA).priority(Priority.HERO).burst(600).shape(Shape.sphere(3)).velocity(Velocity.radial(1.3F, 2.6F)).drag(0.02F)
						.life(30, 60).size(0.2F, 0.4F).stretch(3).build())
				.emit(10, EmitterDef.builder("shock").burst(1).offset(0, 0.5F, 0).mode(Mode.FLAT).fixedRotation().size(110, 110).life(40, 40).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xFFC8F4).alpha(0.85F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(10, smoke("dust").burst(140).shape(Shape.ring(8, 4)).velocity(Velocity.radial(1.4F, 1.9F)).velocity(Velocity.up(0.05F, 0.25F, 0.2F)).drag(0.03F)
						.life(60, 110).size(4, 7).color(Gradient.of(0, 0x8A7A62, 1, 0xA09078)).alpha(0.6F).build())
				.emit(10, smoke("stem").burst(50).shape(Shape.column(5, 12)).velocity(Velocity.up(0.4F, 0.8F, 0.15F)).drag(0.025F).module(Module.curl(12, 0.01F))
						.life(100, 160).size(6, 9).color(SCORCH).alpha(0.55F).emissive(Curve.keys(0, 1, 0.3F, 0.3F, 1, 0)).build())
				.emit(30, smoke("cap").burst(40).shape(Shape.torus(10, 4)).offset(0, 40, 0).velocity(Velocity.radial(0.2F, 0.4F)).module(Module.roll(0.3F, 5, 0.05F))
						.life(100, 160).size(7, 10).color(SCORCH).alpha(0.5F).emissive(Curve.keys(0, 0.8F, 0.4F, 0.2F, 1, 0)).build())
				.build());

		// The ring: a sheet of plasma flying out of the equator, thin and bright, dust thrown up under it.
		FxEngine.register(EffectDef.builder(VfxIds.BOTTLE_RING).lod(700, 4400).maxAge(120)
				.cue(0, EffectDef.light(2.0F, 0xFF90E8))
				.emit(flash("flash", 36, 14, 0xFFE8FA).build())
				.emit(orb("ring", MAGENTA).priority(Priority.HERO).burst(900).shape(Shape.ring((float) Bottle.SUN, 0.6F)).velocity(Velocity.radial(2.2F, 2.25F))
						.drag(0.04F).life(60, 66).size(0.5F, 0.8F).alphaOverLife(Curve.keys(0, 1, 0.8F, 1, 1, 0)).build())
				.emit(spark("sheet", Gradient.of(0, 0xFFFFFF, 0.6F, 0xFFD8F8, 1, argb(0xFF60D0, 0))).priority(Priority.HERO).burst(320)
						.shape(Shape.ring((float) Bottle.SUN, 1.0F)).velocity(Velocity.radial(2.0F, 2.3F)).drag(0.04F).life(50, 64).size(0.1F, 0.16F).stretch(2).build())
				.emit(smoke("dust").burst(90).shape(Shape.ring((float) Bottle.SUN + 2, 2)).offset(0, (float) -Bottle.LOW, 0).velocity(Velocity.radial(1.6F, 2.0F))
						.velocity(Velocity.up(0.05F, 0.2F, 0.2F)).drag(0.04F).life(50, 90).size(3, 5).color(Gradient.of(0, 0x8A7A62, 1, 0xA09078)).alpha(0.5F).build())
				.build());
	}
}
