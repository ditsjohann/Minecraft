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

/**
 * Placed bombs, batch two. Each one is built around a single idea nothing else in the arsenal uses.
 * <ul>
 * <li>Cluster: the dispenser opens like a willow firework, eight smoke trails arcing out with the warheads.</li>
 * <li>Nova: solar prominences, loops of fire thrown off the star that fall back into it; where its rays land the
 * ground runs white-hot.</li>
 * <li>Omega: rivers of dust spiralling in across the ground toward the charge, then the planet cracks: black crust
 * slabs, magma spray and a red column.</li>
 * <li>Helios Crown: the burning ring leaves sun-gold flame behind it; at the centre a crown of gold arcs fountains up.</li>
 * <li>Continental: the plates heave, dust and slabs jumping, and the fissures spit molten gold.</li>
 * <li>Dawnstar: light is drawn up out of the land into the rising star; it falls as a sunrise, rose-gold clouds rolling
 * out across the ground.</li>
 * <li>Bloodstar: it weeps blood rain the whole time; every arc splashes crimson; it dies drawing its blood back in and
 * bursting it out as a red mist.</li>
 * <li>The Last Light: every heartbeat lifts a ring of bone-white dust and glints; after the exhale, white ash falls in
 * silence; the star dies as a burst of light needles and grey dust.</li>
 * <li>Zeropoint: motes hang perfectly still in the sphere while time is stopped; when it snaps, ice splinters race out
 * and diamond dust is left drifting.</li>
 * <li>Unmaker: where the pass sweeps, the world comes apart into rising scanlines, magenta and cyan, flickering out.</li>
 * </ul>
 */
final class BombVfx2 {
	private BombVfx2() {
	}

	static final Gradient TRAIL_SMOKE = Gradient.of(0, 0xF0EEEA, 1, 0xB8B4B0);
	static final Gradient PROMINENCE = Gradient.of(0, 0xFFFFE0, 0.25F, 0xFFD060, 0.6F, 0xFF6010, 1, argb(0x801000, 0));
	static final Gradient WHITE_HOT = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFFF0B0, 0.7F, 0xFFA040, 1, argb(0x802008, 0));
	static final Gradient OMEGA_DUST = Gradient.of(0, 0x5A3A30, 1, 0x7A5A48);
	static final Gradient OMEGA_RED = Gradient.of(0, 0xFFB0A0, 0.3F, 0xFF4030, 0.7F, 0xA01008, 1, argb(0x300404, 0));
	static final Gradient CRUST = Gradient.of(0, 0x2A1A18, 1, 0x1A1010);
	static final Gradient SUN_GOLD = Gradient.of(0, 0xFFFFF0, 0.3F, 0xFFE070, 0.7F, 0xFFA020, 1, argb(0x804008, 0));
	static final Gradient MOLTEN_GOLD = Gradient.of(0, 0xFFFFD0, 0.3F, 0xFFD040, 1, argb(0xC07010, 0));
	static final Gradient SLAB = Gradient.of(0, 0x8A7A68, 1, 0xA89A88);
	static final Gradient ROSE_GOLD = Gradient.of(0, 0xFFE8D0, 0.4F, 0xFFB8A0, 1, 0xE8A0A8);
	static final Gradient DAWN_MOTE = Gradient.of(0, 0xFFFFFF, 0.5F, 0xFFE0A0, 1, argb(0xFFA060, 0));
	static final Gradient BLOOD = Gradient.of(0, 0xFF5050, 0.5F, 0xC01010, 1, 0x600404);
	static final Gradient BLOOD_MIST = Gradient.of(0, 0xA01818, 0.5F, 0x701010, 1, 0x401010);
	static final Gradient BONE = Gradient.of(0, 0xF4F0E8, 1, 0xE0DCD4);
	static final Gradient STARLIGHT = Gradient.of(0, 0xFFFFFF, 0.6F, 0xFFF8E8, 1, argb(0xFFF0D0, 0));
	static final Gradient ICE = Gradient.of(0, 0xFFFFFF, 0.3F, 0xCFF8FF, 0.7F, 0x80D8FF, 1, argb(0x3090D0, 0));
	static final Gradient SCANLINE = Gradient.of(0, 0xFFFFFF, 0.2F, 0xFF40FF, 0.5F, 0x40FFFF, 0.8F, 0xFF40FF, 1, argb(0x200020, 0));

	static void register() {
		cluster();
		nova();
		omega();
		helios();
		continental();
		dawnstar();
		bloodstar();
		lastLight();
		zeropoint();
		unmaker();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color, float alpha) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alpha(alpha).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	// ------------------------------------------------------------------ Cluster

	private static void cluster() {
		// Each carrier leaves a puff every tick, so eight arcing smoke trails draw themselves like a willow firework.
		EmitterDef puff = smoke("puff").burst(1).life(70, 110).size(0.6F, 0.9F).sizeOverLife(Curve.keys(0, 0.4F, 1, 2.2F))
				.color(TRAIL_SMOKE).alpha(0.7F).shade(0.8F).build();
		EmitterDef spark = EmitterDef.builder("spark").burst(1).life(4, 8).size(0.25F, 0.35F).sprite(FxSprites.FLARE).color(0xFFD080)
				.alphaOverLife(Curve.FADE_OUT).build();
		FxEngine.register(EffectDef.builder(VfxIds.CLUSTER_POP).lod(160, 1200).maxAge(200)
				.cue(0, EffectDef.light(0.8F, 0xFFC080))
				.emit(flash("pop", 8, 5, 0xFFE0B0).build())
				.emit(EmitterDef.builder("carriers").priority(Priority.HERO).burst(8).shape(Shape.ring(0.5F, 0)).velocity(Velocity.radial(0.7F, 1.0F))
						.velocity(Velocity.up(1.4F, 1.7F, 0.05F)).gravity(0.06F).life(34, 40).size(0.2F, 0.2F).sprite(FxSprites.GLOW).color(0xFFE0A0)
						.every(1, puff, spark).build())
				.emit(smoke("burst").burst(12).shape(Shape.sphere(1)).velocity(Velocity.random(0.1F, 0.25F)).drag(0.05F).life(60, 90).size(2, 3)
						.color(SOOT).alpha(0.6F).build())
				.build());
	}

	// ------------------------------------------------------------------ Nova

	private static void nova() {
		// Thrown off the star's surface and pulled back into it: loops of fire.
		EmitterDef ember = EmitterDef.builder("ember").burst(1).life(10, 16).size(0.4F, 0.6F).sprite(FxSprites.GLOW).color(PROMINENCE)
				.alphaOverLife(Curve.FADE_OUT).build();
		FxEngine.register(EffectDef.builder(VfxIds.NOVA_PROMINENCE).lod(300, 2000).maxAge(80)
				.emit(EmitterDef.builder("loop").priority(Priority.HERO).burst(3).shape(Shape.shell(6.6F)).velocity(Velocity.radial(0.7F, 1.1F))
						.module(Module.attract(0.05F, 6.2F)).life(30, 40).size(1.4F, 2.2F).sprite(FxSprites.FIRE, 2).color(PROMINENCE)
						.alphaOverLife(Curve.FADE).every(1, ember).build())
				.emit(EmitterDef.builder("granules").burst(6).shape(Shape.shell(6.8F)).velocity(Velocity.radial(0.02F, 0.06F)).life(8, 14).size(1.0F, 1.6F)
						.sprite(FxSprites.GLOW).color(PROMINENCE).alpha(0.6F).alphaOverLife(FLICKER).build())
				.build());

		// Where a ray lands: the ground runs white-hot and shimmers.
		FxEngine.register(EffectDef.builder(VfxIds.NOVA_SCORCH).lod(96, 600)
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(2.2F, 2.8F).life(30, 40)
						.sprite(FxSprites.GLOW).color(WHITE_HOT).build())
				.emit(EmitterDef.builder("spatter").burst(8).velocity(Velocity.up(0.3F, 0.6F, 0.7F)).gravity(0.05F).collide(0.2F, 0.5F, 1).life(14, 24)
						.size(0.06F, 0.1F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(WHITE_HOT).build())
				.emit(smoke("shimmer").burst(2).velocity(Velocity.up(0.1F, 0.2F, 0.2F)).life(40, 60).size(1.2F, 2).color(0xD8D0C8).alpha(0.3F).build())
				.build());
	}

	// ------------------------------------------------------------------ Omega

	private static void omega() {
		// Rivers of dust spiralling in across the ground toward the charge, red sparks riding them.
		FxEngine.register(EffectDef.builder(VfxIds.OMEGA_DRAW).lod(300, 2000).maxAge(120)
				.emit(smoke("rivers").burst(40).onGround().shape(Shape.ring(55, 15)).velocity(Velocity.swirl(0.3F, 0.5F)).module(Module.attract(0.025F, 4))
						.module(Module.vortex(0.01F, 0)).drag(0.02F).life(80, 110).size(1.4F, 2.4F).color(OMEGA_DUST).alpha(0.45F).build())
				.emit(EmitterDef.builder("sparks").burst(30).onGround().shape(Shape.ring(50, 15)).offset(0, 0.5F, 0).velocity(Velocity.swirl(0.4F, 0.6F))
						.module(Module.attract(0.03F, 3)).life(60, 90).size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
						.color(OMEGA_RED).build())
				.build());

		// The planet cracks: black crust slabs heaved up, magma spraying from the seams, a red column.
		FxEngine.register(EffectDef.builder(VfxIds.OMEGA_CRACK).lod(500, 3600).maxAge(420)
				.cue(0, EffectDef.light(1.5F, 0xFF4020))
				.emit(EmitterDef.builder("slabs").priority(Priority.HERO).burst(160).onGround().shape(Shape.disc(30)).velocity(Velocity.up(1.0F, 2.4F, 0.5F))
						.gravity(0.05F).collide(0.25F, 0.5F, 1).life(80, 130).size(1.0F, 2.0F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(CRUST)
						.emissive(Curve.keys(0, 0.4F, 0.3F, 0)).spin(0.05F, 0.2F).build())
				.emit(EmitterDef.builder("magma").priority(Priority.HERO).rate(30, 100, Curve.FALL).onGround().shape(Shape.ring(14, 10))
						.velocity(Velocity.up(1.0F, 2.2F, 0.35F)).gravity(0.05F).collide(0.2F, 0.5F, 1).life(40, 70).size(0.4F, 0.7F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(OMEGA_RED).build())
				.emit(smoke("column").priority(Priority.HERO).rate(14, 160, Curve.FALL).onGround().shape(Shape.disc(10)).velocity(Velocity.up(0.8F, 1.4F, 0.1F))
						.module(Module.curl(40, 0.008F)).life(200, 300).size(14, 22).color(Gradient.of(0, 0x6A1010, 0.5F, 0x3A1010, 1, 0x2A1818))
						.alpha(0.8F).emissive(Curve.keys(0, 0.8F, 0.4F, 0.1F, 1, 0)).build())
				.emit(groundRing("ring", 120, 60, 0xFF4030, 0.5F))
				.build());
	}

	// ------------------------------------------------------------------ Helios Crown

	private static void helios() {
		// The burning ring, authored at radius 20: sun-gold flame licking up along it and sparks lifting off.
		FxEngine.register(EffectDef.builder(VfxIds.CROWN_RING).lod(200, 1400)
				.emit(EmitterDef.builder("flame").burst(36).onGround().shape(Shape.ring(20, 0.6F)).velocity(Velocity.up(0.08F, 0.2F, 0.1F))
						.module(Module.buoyancy(0.01F)).life(14, 24).size(0.8F, 1.3F).sprite(FxSprites.FIRE, 2).color(SUN_GOLD).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("sparks").burst(16).onGround().shape(Shape.ring(20, 0.6F)).velocity(Velocity.up(0.2F, 0.5F, 0.3F))
						.module(Module.jitter(0.01F)).life(20, 40).size(0.06F, 0.1F).sprite(FxSprites.FLARE).color(SUN_GOLD).alphaOverLife(TWINKLE).build())
				.build());

		// The collapse: a crown of gold arcs fountaining up and over, gold glass, a gold haze.
		FxEngine.register(EffectDef.builder(VfxIds.CROWN_BURST).lod(300, 2000).maxAge(220)
				.cue(0, EffectDef.light(1.3F, 0xFFD060))
				.emit(flash("flash", 30, 10, 0xFFF0B0).build())
				.emit(EmitterDef.builder("crown").priority(Priority.HERO).burst(360).shape(Shape.ring(2, 0.2F)).velocity(Velocity.radial(0.5F, 0.6F))
						.velocity(Velocity.up(1.1F, 1.3F, 0.03F)).gravity(0.03F).life(60, 80).size(0.45F, 0.6F).mode(Mode.STRETCHED).stretch(2)
						.sprite(FxSprites.STREAK).color(SUN_GOLD).build())
				.emit(EmitterDef.builder("glass").burst(80).velocity(Velocity.up(0.5F, 1.2F, 0.9F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(40, 70)
						.size(0.2F, 0.35F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(MOLTEN_GOLD).build())
				.emit(smoke("haze").burst(20).shape(Shape.dome(6)).velocity(Velocity.radial(0.2F, 0.5F)).velocity(Velocity.up(0.1F, 0.3F, 0.4F))
						.life(80, 130).size(5, 8).color(Gradient.of(0, 0xFFE8B0, 1, 0xC0A070)).alpha(0.25F).emissive(Curve.keys(0, 1, 1, 0.2F)).build())
				.emit(groundRing("ring", 30, 30, 0xFFD060, 0.6F))
				.build());
	}

	// ------------------------------------------------------------------ Continental

	private static void continental() {
		// One heave, authored at radius 20: slabs and dust jumping off the ring, the fissures spitting molten gold.
		FxEngine.register(EffectDef.builder(VfxIds.CONTINENT_HEAVE).lod(256, 1600)
				.emit(EmitterDef.builder("slabs").burst(40).onGround().shape(Shape.ring(20, 3)).velocity(Velocity.up(0.4F, 0.8F, 0.3F)).gravity(0.05F)
						.collide(0.25F, 0.5F, 2).life(30, 50).size(0.6F, 1.1F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(SLAB)
						.spin(0.03F, 0.1F).build())
				.emit(smoke("dust").burst(36).onGround().shape(Shape.ring(20, 4)).velocity(Velocity.up(0.1F, 0.3F, 0.3F)).velocity(Velocity.radial(0.05F, 0.15F))
						.life(50, 90).size(3, 5).color(SLAB).alpha(0.45F).shade(0.8F).build())
				.emit(EmitterDef.builder("gold").burst(60).onGround().shape(Shape.ring(20, 6)).velocity(Velocity.up(0.5F, 1.0F, 0.25F)).gravity(0.05F)
						.collide(0.2F, 0.5F, 1).life(30, 50).size(0.15F, 0.25F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(MOLTEN_GOLD).build())
				.build());
	}

	// ------------------------------------------------------------------ Dawnstar

	private static void dawnstar() {
		// Light drawn up out of the land into the star (played at the star).
		FxEngine.register(EffectDef.builder(VfxIds.DAWN_GATHER).lod(200, 1400).maxAge(120)
				.emit(EmitterDef.builder("motes").burst(30).shape(Shape.disc(22)).offset(0, -19, 0).velocity(Velocity.up(0.05F, 0.15F, 0.2F))
						.module(Module.attract(0.012F, 3)).life(70, 100).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(DAWN_MOTE)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("corona").burst(4).shape(Shape.shell(5)).velocity(Velocity.radial(0.05F, 0.12F)).life(20, 30).size(2, 3)
						.sprite(FxSprites.GLOW).color(0xFFD060).alpha(0.4F).alphaOverLife(Curve.FADE).build())
				.build());

		// The star falls as a sunrise: a warm flash, light sweeping flat across the ground, rose-gold clouds rolling out.
		FxEngine.register(EffectDef.builder(VfxIds.DAWN_RISE).lod(300, 2400).maxAge(300)
				.cue(0, EffectDef.light(1.4F, 0xFFB070))
				.emit(flash("flash", 50, 12, 0xFFE0C0).build())
				.emit(EmitterDef.builder("sweep").burst(1).onGround().offset(0, 0.3F, 0).mode(Mode.FLAT).fixedRotation().size(70, 70).life(40, 40)
						.sprite(FxSprites.GLOW).color(Gradient.of(0, 0xFFF0D0, 1, 0xFFB080)).alpha(0.55F).sizeOverLife(Curve.EASE_OUT)
						.alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("clouds").priority(Priority.HERO).rate(30, 30, Curve.FALL).onGround().shape(Shape.ring(10, 4)).expand(1.6F)
						.velocity(Velocity.radial(0.3F, 0.5F)).velocity(Velocity.up(0.05F, 0.2F, 0.3F)).drag(0.02F).life(100, 150).size(4, 7)
						.color(ROSE_GOLD).alpha(0.4F).shade(0.85F).emissive(Curve.keys(0, 0.9F, 0.5F, 0.5F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("rays").priority(Priority.HERO).burst(200).shape(Shape.sphere(2)).velocity(Velocity.up(0.8F, 1.8F, 0.9F))
						.drag(0.03F).life(30, 50).size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(DAWN_MOTE).build())
				.emit(10, EmitterDef.builder("motes").rate(10, 160).shape(Shape.disc(40)).velocity(Velocity.up(0.03F, 0.08F, 0.3F)).life(60, 100)
						.size(0.2F, 0.35F).sprite(FxSprites.FLARE).color(DAWN_MOTE).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Bloodstar

	private static void bloodstar() {
		// It weeps: drops falling from the star the whole time (played every few ticks at the star).
		FxEngine.register(EffectDef.builder(VfxIds.BLOOD_RAIN).lod(200, 1400)
				.emit(EmitterDef.builder("drops").burst(8).shape(Shape.sphere(7)).velocity(Velocity.up(-0.2F, -0.1F, 0.4F)).gravity(0.03F)
						.collide(0, 0.5F, 1).life(40, 60).size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(1.5F).sprite(FxSprites.STREAK).color(BLOOD)
						.onHit(0.5F, EmitterDef.builder("splat").burst(1).onGround().offset(0, 0.05F, 0).mode(Mode.FLAT).fixedRotation().size(0.4F, 0.7F)
								.life(60, 100).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(0x7A0808).alphaOverLife(Curve.FADE_OUT).build())
						.build())
				.build());

		// Where an arc strikes: a crimson splash and a breath of red mist.
		FxEngine.register(EffectDef.builder(VfxIds.BLOOD_STRIKE).lod(128, 900)
				.emit(flash("flash", 4, 4, 0xFF4040).build())
				.emit(EmitterDef.builder("splash").burst(24).velocity(Velocity.up(0.3F, 0.7F, 0.8F)).gravity(0.05F).collide(0, 0.5F, 1).life(16, 30)
						.size(0.1F, 0.18F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(BLOOD).build())
				.emit(smoke("mist").burst(6).velocity(Velocity.up(0.05F, 0.15F, 0.5F)).life(40, 60).size(1.5F, 2.5F).color(BLOOD_MIST).alpha(0.5F).build())
				.build());

		// It dies: its blood is drawn back in for a breath, then bursts out as a red mist with black-red streaks.
		FxEngine.register(EffectDef.builder(VfxIds.BLOOD_NOVA).lod(400, 3000).maxAge(320)
				.emit(EmitterDef.builder("inhale").priority(Priority.HERO).burst(200).shape(Shape.shell(40)).module(Module.attract(0.12F, 3)).life(20, 24)
						.size(0.3F, 0.5F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(BLOOD).build())
				.cue(20, EffectDef.light(1.4F, 0xFF2020))
				.emit(20, flash("flash", 60, 12, 0xFF6060).build())
				.emit(20, smoke("mist").priority(Priority.HERO).burst(80).shape(Shape.sphere(6)).velocity(Velocity.radial(0.8F, 1.6F)).drag(0.04F)
						.life(140, 220).size(9, 14).color(BLOOD_MIST).alpha(0.65F).emissive(Curve.keys(0, 0.8F, 0.4F, 0.2F, 1, 0)).build())
				.emit(20, EmitterDef.builder("streaks").priority(Priority.HERO).burst(300).shape(Shape.sphere(3)).velocity(Velocity.random(1.2F, 2.6F))
						.drag(0.03F).gravity(0.01F).life(30, 60).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK)
						.color(Gradient.of(0, 0xFF6060, 0.5F, 0xA00808, 1, argb(0x200000, 0))).build())
				.emit(40, EmitterDef.builder("rain").rate(30, 120).shape(Shape.disc(60)).offset(0, 40, 0).velocity(Velocity.up(-0.4F, -0.2F, 0.1F))
						.gravity(0.03F).life(50, 70).size(0.1F, 0.18F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(BLOOD).build())
				.build());
	}

	// ------------------------------------------------------------------ The Last Light

	private static void lastLight() {
		// A heartbeat, authored at radius 20: a ring of bone-white dust lifts and light glints off the bleached ground.
		FxEngine.register(EffectDef.builder(VfxIds.LL_BEAT).lod(300, 2000)
				.emit(smoke("bone").burst(50).onGround().shape(Shape.ring(20, 4)).velocity(Velocity.up(0.06F, 0.18F, 0.3F)).life(60, 100).size(3, 5)
						.color(BONE).alpha(0.55F).shade(0.9F).build())
				.emit(EmitterDef.builder("glints").burst(40).onGround().shape(Shape.disc(20)).offset(0, 0.5F, 0).life(20, 40).size(0.2F, 0.35F)
						.sprite(FxSprites.FLARE).color(STARLIGHT).alphaOverLife(TWINKLE).build())
				.emit(groundRing("ring", 22, 20, 0xFFF4E0, 0.5F))
				.build());

		// After the exhale: white ash falling in the silence. Authored at radius 60.
		FxEngine.register(EffectDef.builder(VfxIds.LL_ASH).lod(200, 1400)
				.emit(EmitterDef.builder("ash").burst(40).shape(Shape.disc(60)).offset(0, 24, 0).velocity(Velocity.up(-0.05F, -0.02F, 0.3F))
						.module(Module.jitter(0.004F)).life(160, 240).size(0.14F, 0.22F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(BONE)
						.spin(0.02F, 0.06F).build())
				.build());

		// The star dies: a breath of white drawn in, then needles of light and a slow grey ring of dust.
		FxEngine.register(EffectDef.builder(VfxIds.LL_DEATH).lod(400, 3000).maxAge(320)
				.emit(EmitterDef.builder("inhale").burst(120).shape(Shape.shell(24)).module(Module.attract(0.1F, 2)).life(14, 18).size(0.2F, 0.35F)
						.mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(STARLIGHT).build())
				.cue(14, EffectDef.light(1.4F, 0xFFF4E0))
				.emit(14, flash("flash", 60, 14, 0xFFFFFF).build())
				.emit(14, EmitterDef.builder("needles").priority(Priority.HERO).burst(260).shape(Shape.sphere(1)).velocity(Velocity.random(1.6F, 3.0F))
						.drag(0.02F).life(30, 50).size(0.3F, 0.45F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(STARLIGHT).build())
				.emit(14, smoke("dust").burst(60).shape(Shape.shell(4)).velocity(Velocity.radial(0.4F, 0.8F)).drag(0.03F).life(160, 240).size(8, 12)
						.color(Gradient.of(0, 0xE8E4DC, 1, 0x9A9690)).alpha(0.5F).build())
				.build());
	}

	// ------------------------------------------------------------------ Zeropoint

	private static void zeropoint() {
		// Time stopped: motes hanging perfectly still in the sphere. Authored at radius 30; played every few ticks.
		FxEngine.register(EffectDef.builder(VfxIds.ZP_HUSH).lod(160, 1000)
				.emit(EmitterDef.builder("motes").burst(24).shape(Shape.sphere(30)).life(50, 70).size(0.18F, 0.28F).fixedRotation()
						.sprite(FxSprites.FLARE).color(ICE).alphaOverLife(Curve.keys(0, 0, 0.2F, 0.8F, 0.8F, 0.8F, 1, 0)).build())
				.emit(EmitterDef.builder("dust").burst(16).shape(Shape.sphere(30)).life(50, 70).size(0.1F, 0.2F).fixedRotation().layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(0xA8B8C8).alphaOverLife(Curve.keys(0, 0, 0.2F, 0.8F, 0.8F, 0.8F, 1, 0)).build())
				.build());

		// The snap: ice splinters racing out of the pinhole, a cold shock, and diamond dust left drifting.
		FxEngine.register(EffectDef.builder(VfxIds.ZP_SNAP).lod(300, 2000).maxAge(260)
				.cue(0, EffectDef.light(1.0F, 0x9FEFFF))
				.emit(flash("flash", 20, 8, 0xE0FAFF).build())
				.emit(EmitterDef.builder("splinters").priority(Priority.HERO).burst(300).shape(Shape.sphere(0.5F)).velocity(Velocity.random(1.2F, 2.4F))
						.drag(0.04F).life(20, 36).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(ICE).build())
				.emit(smoke("frost").burst(24).shape(Shape.shell(2)).velocity(Velocity.radial(0.5F, 0.9F)).drag(0.06F).life(60, 100).size(4, 6)
						.color(Gradient.of(0, 0xF0FAFF, 1, 0xC8E8F8)).alpha(0.5F).shade(0.9F).build())
				.emit(6, EmitterDef.builder("diamond").rate(10, 120).shape(Shape.sphere(26)).velocity(Velocity.random(0.005F, 0.02F)).gravity(0.001F)
						.life(60, 100).size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(ICE).alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Unmaker

	private static void unmaker() {
		// The pass, played along the sweep line (dir is the line, end to end): the world comes apart into rising
		// scanlines, magenta and cyan, flickering out; a thin black haze where it was.
		FxEngine.register(EffectDef.builder(VfxIds.UM_PLANE).lod(256, 1600)
				.emit(EmitterDef.builder("scan").priority(Priority.HERO).burst(70).span().shape(Shape.box(0.5F, 6, 0.5F)).offset(0, 4, 0)
						.velocity(Velocity.up(0.3F, 0.8F, 0)).life(10, 22).size(0.12F, 0.2F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK)
						.color(SCANLINE).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("bits").burst(24).span().shape(Shape.box(0.5F, 10, 0.5F)).offset(0, 6, 0).velocity(Velocity.random(0.02F, 0.06F))
						.life(8, 16).size(0.25F, 0.4F).fixedRotation().sprite(FxSprites.FLARE).color(SCANLINE).alphaOverLife(FLICKER).build())
				.emit(smoke("haze").burst(6).span().shape(Shape.box(0.5F, 4, 0.5F)).offset(0, 3, 0).velocity(Velocity.up(0.02F, 0.05F, 0.2F))
						.life(30, 50).size(2, 3).color(0x301030).alpha(0.22F).build())
				.build());
	}
}
