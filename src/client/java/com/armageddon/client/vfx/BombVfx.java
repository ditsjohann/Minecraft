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
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/**
 * The placed bombs below the cataclysms, ten at a time. Each gets a look of its own instead of the shared fireball.
 * <p>
 * Batch one:
 * <ul>
 * <li>Demolition Charge, a job site: a hard white crack, a curtain of grey concrete dust, rubble bouncing, rebar sparks.</li>
 * <li>Thermobaric, fuel-air: a pale mist spreads, catches, and a rolling wall of fire runs through it; then the
 * vacuum pulls the dust back in.</li>
 * <li>Cryo, the cold: ice shards, frost mist rolling over the ground, diamond dust glittering down.</li>
 * <li>Singularity, the drain: dust and light spiralling in and stretching as they go; a last violet pop.</li>
 * <li>Neutron, quiet and wrong: a thin green-white flash, a sphere of glittering dose spreading through everything,
 * no fire to speak of.</li>
 * <li>Atomic and Hydrogen: the mushroom already exists; under it now a base surge, and for the hydrogen bomb a second
 * stage, condensation rings and the air itself glowing.</li>
 * <li>Antimatter, clean annihilation: everything drawn in, then a prismatic white burst of gamma sparks and no smoke.</li>
 * <li>Tectonic, grinding plates: rock and dust thrown off the travelling wave, with the glow of what's underneath.</li>
 * <li>Bass Drop, a rave: dust jumping off the ground on every beat in the beat's colour, and a confetti-laser finale.</li>
 * </ul>
 */
final class BombVfx {
	private BombVfx() {
	}

	static final Gradient CONCRETE = Gradient.of(0, 0xA8A49C, 1, 0xC8C4BC);
	static final Gradient RUBBLE = Gradient.of(0, 0x6A6660, 1, 0x8A867E);
	static final Gradient FUEL = Gradient.of(0, 0xE8E4D0, 1, 0xC8C0A0);
	static final Gradient ICE = Gradient.of(0, 0xFFFFFF, 0.3F, 0xD8F4FF, 0.7F, 0x88D0FF, 1, argb(0x4090D0, 0));
	static final Gradient FROST_MIST = Gradient.of(0, 0xF0FAFF, 1, 0xC8E4F4);
	static final Gradient INFALL = Gradient.of(0, 0x4060FF, 0.5F, 0xA060FF, 1, 0xFFFFFF);
	static final Gradient VOID_DUST = Gradient.of(0, 0x2A2038, 1, 0x4A3A60);
	static final Gradient DOSE = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC8FFA0, 0.7F, 0x60FF30, 1, argb(0x20A020, 0));
	static final Gradient CHERENKOV = fadeTo(0xE0FFF0, 0x60F0C0);
	static final Gradient SURGE = Gradient.of(0, 0xC8B8A0, 1, 0xA09080);
	static final Gradient CONDENSATION = Gradient.of(0, 0xFFFFFF, 1, 0xE8ECF0);
	static final Gradient IONIZED = Gradient.of(0, 0xFFE0FF, 0.5F, 0xC080FF, 1, argb(0x6040C0, 0));
	static final Gradient PRISM = Gradient.of(0, 0xFFFFFF, 0.25F, 0xFFB0F0, 0.5F, 0xB0C0FF, 0.75F, 0xB0FFE0, 1, argb(0xFFF0B0, 0));
	static final Gradient EARTH = Gradient.of(0, 0x6A5638, 0.5F, 0x8A7658, 1, 0x9A8E7A);
	static final Gradient MAGMA = Gradient.of(0, 0xFFE0A0, 0.3F, 0xFF7020, 1, argb(0x601808, 0));
	static final Gradient CONFETTI = Gradient.of(0, 0xFF60C0, 0.2F, 0xFFE040, 0.4F, 0x40FF80, 0.6F, 0x40C0FF, 0.8F, 0xB060FF, 1, 0xFF60C0);

	private static Gradient fadeTo(int from, int to) {
		return Gradient.of(0, from, 1, argb(to, 0));
	}

	static void register() {
		demolition();
		thermobaric();
		cryo();
		singularity();
		neutron();
		nukes();
		antimatter();
		tectonic();
		bassDrop();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	// ------------------------------------------------------------------ Demolition Charge

	private static void demolition() {
		FxEngine.register(EffectDef.builder(VfxIds.DEMO_BLAST).lod(128, 900).maxAge(220)
				.cue(0, EffectDef.light(0.8F, 0xFFE8C0))
				.emit(flash("crack", 10, 4, 0xFFF4E0).build())
				.emit(EmitterDef.builder("core").burst(10).shape(Shape.sphere(1.5F)).velocity(Velocity.radial(0.2F, 0.4F)).drag(0.12F).life(6, 10)
						.size(2, 3).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("curtain").priority(Priority.HERO).burst(36).onGround().shape(Shape.ring(2, 2)).velocity(Velocity.radial(0.35F, 0.6F))
						.velocity(Velocity.up(0.05F, 0.25F, 0.4F)).drag(0.05F).life(80, 140).size(3, 5).color(CONCRETE).alpha(0.75F).shade(0.6F).build())
				.emit(smoke("column").burst(10).shape(Shape.dome(2)).velocity(Velocity.up(0.25F, 0.45F, 0.3F)).drag(0.04F).life(100, 160).size(3, 4.5F)
						.color(CONCRETE).alpha(0.65F).build())
				.emit(EmitterDef.builder("rubble").burst(40).shape(Shape.sphere(1)).velocity(Velocity.up(0.4F, 0.9F, 0.9F)).gravity(0.05F)
						.collide(0.3F, 0.5F, 3).life(40, 70).size(0.15F, 0.3F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(RUBBLE)
						.spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("rebar").burst(30).velocity(Velocity.random(0.4F, 0.9F)).velocity(Velocity.up(0.1F, 0.3F, 0.8F)).gravity(0.04F)
						.collide(0.3F, 0.6F, 1).life(10, 22).size(0.04F, 0.07F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(SPARK).build())
				.emit(groundRing("ring", 9, 12, 0xD0CCC4, 0.5F))
				.build());
	}

	// ------------------------------------------------------------------ Thermobaric

	private static void thermobaric() {
		// Authored at power 11. The mist goes out first, then the whole cloud catches.
		FxEngine.register(EffectDef.builder(VfxIds.THERMO_BLAST).lod(200, 1400).maxAge(320)
				.emit(smoke("mist").priority(Priority.HERO).burst(40).shape(Shape.dome(3)).velocity(Velocity.radial(0.6F, 1.0F)).drag(0.12F).life(10, 12)
						.size(4, 6).color(FUEL).alpha(0.5F).alphaOverLife(Curve.FADE).build())
				.cue(8, EffectDef.light(1.2F, 0xFF8030))
				.emit(8, flash("catch", 40, 10, 0xFFC060).build())
				.emit(8, EmitterDef.builder("wall").priority(Priority.HERO).burst(110).shape(Shape.dome(8)).velocity(Velocity.radial(0.6F, 1.1F))
						.velocity(Velocity.up(0.05F, 0.2F, 0.4F)).drag(0.07F).module(Module.buoyancy(0.012F)).life(24, 40).size(5, 8)
						.sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(10, EmitterDef.builder("roll").burst(50).onGround().shape(Shape.ring(10, 3)).expand(0.8F).velocity(Velocity.radial(0.3F, 0.5F))
						.velocity(Velocity.up(0.05F, 0.15F, 0.3F)).drag(0.05F).life(20, 34).size(4, 6).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(14, smoke("soot").burst(30).shape(Shape.dome(10)).velocity(Velocity.up(0.15F, 0.35F, 0.5F)).drag(0.03F).life(120, 200)
						.size(8, 12).color(SOOT).alpha(0.7F).build())
				// The vacuum: the dust it pushed out is sucked back toward the middle.
				.emit(30, smoke("suck").burst(30).onGround().shape(Shape.ring(26, 4)).module(Module.attract(0.03F, 4)).drag(0.05F).life(50, 70)
						.size(3, 5).color(VfxLibrary.DUST).alpha(0.5F).build())
				.emit(EmitterDef.builder("embers").burst(80).velocity(Velocity.random(0.3F, 0.9F)).velocity(Velocity.up(0.1F, 0.3F, 0.8F)).gravity(0.02F)
						.drag(0.02F).life(30, 60).size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());
	}

	// ------------------------------------------------------------------ Cryo

	private static void cryo() {
		// Authored at radius 36.
		FxEngine.register(EffectDef.builder(VfxIds.CRYO_NOVA).lod(200, 1400).maxAge(300)
				.cue(0, EffectDef.light(0.8F, 0xA8E8FF))
				.emit(flash("flash", 20, 8, 0xD8F4FF).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(160).shape(Shape.sphere(2)).velocity(Velocity.radial(0.8F, 1.8F))
						.velocity(Velocity.up(0.1F, 0.4F, 0.6F)).gravity(0.025F).drag(0.02F).collide(0.3F, 0.6F, 2).life(30, 60).size(0.3F, 0.55F)
						.mode(Mode.STRETCHED).stretch(1.2F).sprite(FxSprites.FLARE).color(ICE).build())
				.emit(smoke("mist").priority(Priority.HERO).rate(14, 40, Curve.FALL).onGround().shape(Shape.ring(4, 3)).expand(0.9F)
						.velocity(Velocity.radial(0.4F, 0.7F)).drag(0.03F).life(100, 160).size(5, 8).color(FROST_MIST).alpha(0.55F).shade(0.85F).build())
				.emit(10, EmitterDef.builder("diamond").rate(14, 160).shape(Shape.disc(36)).offset(0, 14, 0).velocity(Velocity.up(-0.06F, -0.03F, 0.3F))
						.module(Module.jitter(0.003F)).life(100, 160).size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(ICE).alphaOverLife(TWINKLE).build())
				.emit(groundRing("ring", 36, 40, 0xA8E8FF, 0.45F))
				.build());
	}

	// ------------------------------------------------------------------ Singularity

	private static void singularity() {
		// One tick of infall, played at the hole's current radius: dust and light spiralling in, stretching as they go.
		FxEngine.register(EffectDef.builder(VfxIds.HOLE_INFALL).lod(64, 600)
				.emit(EmitterDef.builder("light").burst(4).shape(Shape.shell(4)).velocity(Velocity.swirl(0.08F, 0.14F)).module(Module.attract(0.012F, 0.5F))
						.module(Module.vortex(0.01F, 0)).life(60, 90).size(0.09F, 0.15F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
						.color(INFALL).build())
				.emit(EmitterDef.builder("dust").priority(Priority.DETAIL).burst(1).shape(Shape.ring(4.5F, 1)).velocity(Velocity.swirl(0.06F, 0.1F))
						.module(Module.attract(0.008F, 0.6F)).life(80, 120).size(0.25F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(VOID_DUST).spin(0.05F, 0.2F).build())
				.build());

		// The hole closes: a violet pop, streaks flung out, a ring of dust.
		FxEngine.register(EffectDef.builder(VfxIds.HOLE_POP).lod(128, 900)
				.emit(flash("pop", 6, 8, 0xC080FF).build())
				.emit(EmitterDef.builder("streaks").burst(120).velocity(Velocity.random(0.4F, 1.0F)).drag(0.04F).life(20, 40).size(0.08F, 0.14F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(INFALL).build())
				.emit(smoke("dust").burst(24).shape(Shape.shell(1.5F)).velocity(Velocity.radial(0.2F, 0.4F)).drag(0.05F).life(60, 100).size(1.5F, 2.5F)
						.color(VOID_DUST).alpha(0.6F).emissive(Curve.keys(0, 0.6F, 1, 0)).build())
				.build());
	}

	// ------------------------------------------------------------------ Neutron

	private static void neutron() {
		// Authored at radius 110: no fireball worth the name, a sphere of dose spreading through everything.
		FxEngine.register(EffectDef.builder(VfxIds.NEUTRON_PULSE).lod(300, 2400).maxAge(400)
				.cue(0, EffectDef.light(1.0F, 0x80FFA0))
				.emit(flash("flash", 60, 10, 0xE0FFF0).build())
				.emit(EmitterDef.builder("cherenkov").burst(1).size(18, 18).life(30, 30).sprite(FxSprites.GLOW).color(CHERENKOV).alpha(0.6F).build())
				.emit(EmitterDef.builder("front").priority(Priority.HERO).burst(500).shape(Shape.shell(3)).velocity(Velocity.radial(2.6F, 3.0F)).drag(0.012F)
						.life(40, 50).size(0.9F, 1.3F).sprite(FxSprites.FLARE).color(DOSE).alphaOverLife(TWINKLE).build())
				.emit(20, EmitterDef.builder("dose").rate(24, 300).shape(Shape.sphere(90)).velocity(Velocity.random(0.005F, 0.02F)).life(60, 120)
						.size(0.4F, 0.7F).sprite(FxSprites.FLARE).color(DOSE).alphaOverLife(TWINKLE).build())
				.emit(groundRing("ring", 110, 50, 0x7CFF2E, 0.4F))
				.emit(smoke("haze").burst(14).shape(Shape.sphere(6)).velocity(Velocity.radial(0.2F, 0.4F)).life(80, 120).size(8, 12)
						.color(Gradient.of(0, 0xD8FFC8, 1, 0xA0C090)).alpha(0.3F).emissive(Curve.keys(0, 1, 1, 0.2F)).build())
				.build());
	}

	// ------------------------------------------------------------------ Atomic and Hydrogen

	private static void nukes() {
		// Under every mushroom, authored at radius 26: the base surge, a low ring of dust rolling out, debris skipping.
		FxEngine.register(EffectDef.builder(VfxIds.NUKE_SURGE).lod(300, 2400).maxAge(400)
				.emit(smoke("surge").priority(Priority.HERO).rate(40, 40, Curve.FALL).onGround().shape(Shape.ring(14, 4)).expand(2.2F)
						.velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F)).drag(0.02F).life(140, 220).size(8, 13)
						.color(SURGE).alpha(0.7F).shade(0.6F).build())
				.emit(EmitterDef.builder("debris").burst(140).onGround().shape(Shape.disc(10)).velocity(Velocity.radial(0.8F, 1.6F))
						.velocity(Velocity.up(0.3F, 0.8F, 0.5F)).gravity(0.04F).collide(0.35F, 0.6F, 3).life(60, 110).size(0.3F, 0.6F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(VfxLibrary.DUST).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("embers").burst(120).shape(Shape.sphere(8)).velocity(Velocity.up(0.6F, 1.6F, 0.9F)).gravity(0.02F).drag(0.01F)
						.life(60, 120).size(0.15F, 0.25F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());

		// What the blast front throws up as it rolls over the real world (played by the client's surge sampler, aimed
		// outward, tinted with the ground's own colour).
		Gradient tintable = Gradient.of(0, 0xFFFFFF, 1, 0xE8E8E8);
		FxEngine.register(EffectDef.builder(VfxIds.SURGE_DUST).tint(0x9A8A70).lod(96, 260)
				.emit(smoke("dust").priority(Priority.DETAIL).burst(3).onGround().shape(Shape.disc(2)).velocity(Velocity.along(0.4F, 0.9F, 0.35F))
						.velocity(Velocity.up(0.08F, 0.25F, 0.3F)).drag(0.025F).life(80, 140).size(3.5F, 6).color(tintable).tinted().alpha(0.6F).build())
				.emit(EmitterDef.builder("grit").priority(Priority.DETAIL).burst(6).onGround().shape(Shape.disc(2)).offset(0, 0.3F, 0)
						.velocity(Velocity.along(0.7F, 1.3F, 0.3F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F)).gravity(0.03F).collide(0.3F, 0.6F, 1)
						.life(20, 40).size(0.1F, 0.18F).mode(Mode.STRETCHED).stretch(2).layer(Layer.SMOKE).sprite(FxSprites.STREAK).color(tintable)
						.tinted().build())
				.emit(EmitterDef.builder("clods").priority(Priority.DETAIL).burst(3).onGround().shape(Shape.disc(2)).velocity(Velocity.along(0.4F, 0.8F, 0.4F))
						.velocity(Velocity.up(0.3F, 0.6F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 2).life(30, 60).size(0.2F, 0.35F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(tintable).tinted().spin(0.05F, 0.2F).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SURGE_SPRAY).tint(0xE8F4FF).lod(96, 260)
				.emit(smoke("mist").priority(Priority.DETAIL).burst(4).onGround().shape(Shape.disc(2)).velocity(Velocity.along(0.3F, 0.7F, 0.35F))
						.velocity(Velocity.up(0.25F, 0.6F, 0.3F)).drag(0.03F).life(60, 110).size(4, 7).color(tintable).tinted().alpha(0.5F).shade(0.9F).build())
				.emit(EmitterDef.builder("drops").priority(Priority.DETAIL).burst(16).onGround().shape(Shape.disc(2)).velocity(Velocity.along(0.3F, 0.8F, 0.4F))
						.velocity(Velocity.up(0.5F, 1.1F, 0.3F)).gravity(0.045F).life(25, 45).size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFFFFFF, 1, argb(0xC0E0FF, 0))).tinted().build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SURGE_LEAVES).tint(0x4A8A30).lod(96, 260)
				.emit(EmitterDef.builder("leaves").priority(Priority.DETAIL).burst(14).shape(Shape.sphere(2.5F)).velocity(Velocity.along(0.5F, 1.0F, 0.5F))
						.velocity(Velocity.up(0.1F, 0.4F, 0.5F)).gravity(0.008F).drag(0.03F).module(Module.curl(3, 0.02F)).life(60, 110).size(0.22F, 0.34F)
						.layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(tintable).tinted().spin(0.1F, 0.3F).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.SURGE_WALL).tint(0x9A8A70).lod(96, 260)
				.emit(smoke("climb").priority(Priority.DETAIL).burst(7).shape(Shape.box(1.5F, 0.5F, 1.5F)).velocity(Velocity.up(0.6F, 1.2F, 0.15F))
						.velocity(Velocity.along(0.05F, 0.2F, 0.3F)).drag(0.04F).life(60, 100).size(3, 5).color(tintable).tinted().alpha(0.6F).build())
				.emit(EmitterDef.builder("debris").priority(Priority.DETAIL).burst(10).shape(Shape.box(1.5F, 0.5F, 1.5F)).offset(0, 5, 0)
						.velocity(Velocity.along(0.3F, 0.7F, 0.6F)).velocity(Velocity.up(0.4F, 0.8F, 0.4F)).gravity(0.05F).collide(0.3F, 0.6F, 2)
						.life(30, 60).size(0.25F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(tintable).tinted().spin(0.05F, 0.2F).build())
				.build());

		// The hydrogen bomb's second stage, authored at radius 52: a second, brighter flash, condensation rings shocked out
		// of the sky, and the air above glowing with ionisation.
		FxEngine.register(EffectDef.builder(VfxIds.HYDROGEN_STAGE).lod(500, 3600).maxAge(500)
				.cue(12, EffectDef.flash(1.0F, 6000, 0xFFFFFF))
				.emit(12, flash("secondary", 200, 16, 0xFFF8E8).build())
				.emit(20, EmitterDef.builder("wilson1").priority(Priority.HERO).burst(160).shape(Shape.ring(10, 2)).offset(0, 80, 0).expand(3.2F)
						.life(60, 80).size(10, 14).layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6).color(CONDENSATION).alpha(0.4F)
						.alphaOverLife(Curve.FADE).emissive(Curve.keys(0, 0.8F, 1, 0.5F)).build())
				.emit(26, EmitterDef.builder("wilson2").burst(120).shape(Shape.ring(10, 2)).offset(0, 140, 0).expand(2.6F).life(60, 80).size(9, 12)
						.layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6).color(CONDENSATION).alpha(0.35F).alphaOverLife(Curve.FADE)
						.emissive(Curve.keys(0, 0.8F, 1, 0.5F)).build())
				.emit(16, EmitterDef.builder("ionised").rate(10, 140).shape(Shape.dome(160)).offset(0, 60, 0).velocity(Velocity.up(0.05F, 0.15F, 0.6F))
						.life(40, 80).size(10, 18).sprite(FxSprites.GLOW).color(IONIZED).alpha(0.25F).alphaOverLife(Curve.FADE).build())
				.build());
	}

	// ------------------------------------------------------------------ Antimatter

	private static void antimatter() {
		// Authored at radius 30. The pull for 28 ticks, then a clean prismatic burst with no smoke at all.
		FxEngine.register(EffectDef.builder(VfxIds.ANTI_ANNIHILATE).lod(300, 2400).maxAge(320)
				.emit(EmitterDef.builder("inhale").priority(Priority.HERO).rate(30, 26).shape(Shape.shell(60)).module(Module.attract(0.2F, 3))
						.life(26, 30).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(PRISM).build())
				.emit(EmitterDef.builder("dust").rate(6, 26).onGround().shape(Shape.ring(40, 10)).module(Module.attract(0.08F, 3)).life(40, 50)
						.size(2, 3).layer(Layer.SMOKE).sprite(FxSprites.SMOKE, 6).color(VfxLibrary.DUST).alpha(0.5F).alphaOverLife(Curve.FADE).build())
				.cue(28, EffectDef.light(1.6F, 0xF0D0FF))
				.emit(28, flash("annihilate", 120, 12, 0xF8E8FF).build())
				.emit(28, EmitterDef.builder("gamma").priority(Priority.HERO).burst(500).shape(Shape.sphere(2)).velocity(Velocity.random(1.8F, 3.4F))
						.drag(0.03F).life(30, 60).size(0.35F, 0.6F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(PRISM).build())
				.emit(28, EmitterDef.builder("glitter").burst(200).shape(Shape.sphere(30)).velocity(Velocity.random(0.02F, 0.08F)).life(60, 120)
						.size(0.3F, 0.5F).sprite(FxSprites.FLARE).color(PRISM).alphaOverLife(TWINKLE).build())
				.emit(28, EmitterDef.builder("halo").burst(1).size(90, 90).life(50, 50).sprite(FxSprites.GLOW).color(0xE0B0FF).alpha(0.4F)
						.sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ Tectonic

	private static void tectonic() {
		// The travelling wave, authored at radius 20 and played at the wave's radius: rock and dust thrown off it, and
		// the glow of what's underneath breathing out of the cracks.
		FxEngine.register(EffectDef.builder(VfxIds.TECTONIC_FRONT).lod(200, 1200)
				.emit(smoke("dust").burst(30).onGround().shape(Shape.ring(20, 2)).velocity(Velocity.up(0.08F, 0.2F, 0.3F)).velocity(Velocity.radial(0.05F, 0.15F))
						.life(50, 80).size(2.5F, 4).color(EARTH).alpha(0.6F).build())
				.emit(EmitterDef.builder("rocks").burst(30).onGround().shape(Shape.ring(20, 1.5F)).velocity(Velocity.up(0.4F, 0.8F, 0.4F)).gravity(0.05F)
						.collide(0.3F, 0.6F, 2).life(30, 60).size(0.25F, 0.5F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(EARTH)
						.spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("magma").burst(12).onGround().shape(Shape.ring(20, 1)).velocity(Velocity.up(0.2F, 0.5F, 0.3F)).gravity(0.03F)
						.life(20, 40).size(0.12F, 0.2F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(MAGMA).build())
				.build());
	}

	// ------------------------------------------------------------------ Bass Drop

	private static void bassDrop() {
		// A beat, played at the beat's radius / 10 and tinted the beat's colour: dust jumping off the ground in a ring
		// and coloured sparks bouncing.
		FxEngine.register(EffectDef.builder(VfxIds.BASS_THUMP).tint(0xFF60FF).lod(128, 900)
				.emit(smoke("jump").burst(30).onGround().shape(Shape.ring(10, 1.5F)).velocity(Velocity.up(0.15F, 0.3F, 0.2F)).gravity(0.008F).life(20, 30)
						.size(1.2F, 2).color(VfxLibrary.DUST).alpha(0.5F).build())
				.emit(EmitterDef.builder("sparks").burst(40).onGround().shape(Shape.ring(10, 1)).velocity(Velocity.up(0.2F, 0.5F, 0.4F)).gravity(0.03F)
						.collide(0.4F, 0.6F, 2).life(20, 34).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFFF, 1, 0xFFFFFF))
						.tinted().alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(11, 11).life(10, 10).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).color(0xFFFFFF).tinted().build())
				.build());

		// The drop: a column of confetti and laser streaks, a dance floor of glitter, everything in every colour.
		FxEngine.register(EffectDef.builder(VfxIds.DROP_FINALE).lod(300, 2400).maxAge(260)
				.emit(EmitterDef.builder("confetti").priority(Priority.HERO).burst(500).shape(Shape.sphere(3)).velocity(Velocity.up(0.8F, 2.0F, 0.9F))
						.gravity(0.02F).drag(0.03F).module(Module.jitter(0.01F)).life(100, 180).size(0.25F, 0.4F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(CONFETTI).emissive(Curve.constant(0.6F)).spin(0.1F, 0.3F).build())
				.emit(EmitterDef.builder("lasers").rate(10, 60).shape(Shape.sphere(2)).velocity(Velocity.up(2.0F, 3.4F, 0.7F)).life(14, 24).size(0.3F, 0.4F)
						.mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(CONFETTI).build())
				.emit(EmitterDef.builder("floor").rate(12, 120).onGround().shape(Shape.disc(40)).offset(0, 0.4F, 0).life(10, 20).size(0.4F, 0.7F)
						.sprite(FxSprites.FLARE).color(CONFETTI).alphaOverLife(FLICKER).build())
				.emit(smoke("smoke").burst(30).shape(Shape.dome(6)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.4F))
						.life(60, 100).size(5, 8).color(Gradient.of(0, 0xFFC8F4, 0.5F, 0xC8C8FF, 1, 0xA8A8C0)).alpha(0.35F)
						.emissive(Curve.keys(0, 0.8F, 1, 0.1F)).build())
				.build());
	}
}
