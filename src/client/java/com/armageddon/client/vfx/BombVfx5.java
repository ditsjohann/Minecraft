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
 * Placed bombs, batch five: redrawn from scratch, each around one idea. The rings, domes, pillars, beams, layered
 * blasts and vanilla particles are gone.
 * <ul>
 * <li>Sixteen Chunks: a slot machine. Each roll showers pixels off the chunk's border in the roll's colour; the fanfare
 * rains coins and confetti; swaps stream glitter from one chunk to the other; the cracked corners spew pixel geysers.</li>
 * <li>Disco Ball: where a beam lands, mirror sparkle; the drop is a floor-level wave of glitter and a fountain of mirror
 * shards; after it, the dance floor lights up in rings on the beat.</li>
 * <li>LiDAR: everything it strikes becomes a point cloud, a burst of dots that freeze where they fly and fade; the
 * overload scans the whole area into shells of points.</li>
 * <li>Shadow Void: ink. What it swallows goes in a black puff; its exhale is tendrils of ink and violet sparks.</li>
 * <li>Spectral Ice: ghost ice. Spikes erupt in pale shards and cyan wisps; shards land in splinters and cold mist.</li>
 * <li>Sonar: sound you can see. Brittle things burst in glints; the boom kicks dust up in concentric rings at once.</li>
 * <li>Pixel Bomb: GAME OVER in chunky pixel confetti of the six palette colours.</li>
 * <li>Magnetar: sparks riding field lines as the ground lifts, ground blocks to dust in orbit, a plasma splash where the
 * jet cuts, field lines snapping on the collapse.</li>
 * <li>Ink Bomb: comic book. Flat, bold, unshaded puffs in yellow and red with black speed lines.</li>
 * </ul>
 */
final class BombVfx5 {
	private BombVfx5() {
	}

	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient GLITCH = Gradient.of(0, 0xFFFFFF, 0.25F, 0xFF40FF, 0.5F, 0x40FFFF, 0.75F, 0xFFFF40, 1, argb(0xFF40FF, 0));
	static final Gradient CONFETTI = Gradient.of(0, 0xFF60C0, 0.2F, 0xFFE040, 0.4F, 0x40FF80, 0.6F, 0x40C0FF, 0.8F, 0xB060FF, 1, 0xFF60C0);
	static final Gradient COIN = Gradient.of(0, 0xFFF0A0, 1, 0xE0B040);
	static final Gradient MIRROR = Gradient.of(0, 0xFFFFFF, 0.5F, 0xE0F0FF, 1, argb(0xC0D0FF, 0));
	static final Gradient POINTS = Gradient.of(0, 0xFFFFFF, 0.2F, 0x90FFFF, 1, argb(0x40C0FF, 0));
	static final Gradient TARGET = Gradient.of(0, 0xFFFFFF, 0.3F, 0xFF6050, 1, argb(0xC01010, 0));
	static final Gradient INK = Gradient.of(0, 0x100818, 1, 0x1A1024);
	static final Gradient VOID_SPARK = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC080FF, 1, argb(0x6020C0, 0));
	static final Gradient GHOST_ICE = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC8F0FF, 0.7F, 0x70C0FF, 1, argb(0x3060C0, 0));
	static final Gradient GHOST_WISP = Gradient.of(0, 0xE0FFFF, 0.5F, 0x80E0FF, 1, argb(0x4080FF, 0));
	static final Gradient SONAR = Gradient.of(0, 0xFFFFFF, 0.3F, 0x80FFB0, 1, argb(0x20A060, 0));
	static final Gradient PIXELS = Gradient.of(0, 0xFF40FF, 0.2F, 0x40FFFF, 0.4F, 0xFFFF40, 0.6F, 0x60FF40, 0.8F, 0xFF4040, 1, 0x4060FF);
	static final Gradient FIELD = Gradient.of(0, 0xFFFFFF, 0.3F, 0xA0D8FF, 1, argb(0x4080FF, 0));
	static final Gradient COMIC = Gradient.of(0, 0xFFF060, 0.4F, 0xFFC020, 0.75F, 0xFF5020, 1, 0xE03010);

	static void register() {
		sixteenChunks();
		discoBall();
		lidar();
		shadowVoid();
		spectralIce();
		sonar();
		pixels();
		magnetar();
		comic();
	}

	// ------------------------------------------------------------------ Sixteen Chunks

	private static void sixteenChunks() {
		// A roll lands, tinted the roll's colour: pixels shower off the chunk's border and blink out.
		FxEngine.register(EffectDef.builder(VfxIds.CHUNK_REROLL).tint(0xFF40FF).lod(128, 900)
				.emit(EmitterDef.builder("border").priority(Priority.HERO).burst(130).onGround().shape(Shape.box(8, 0.2F, 8)).velocity(Velocity.up(0.2F, 0.6F, 0.1F))
						.gravity(0.01F).life(20, 40).size(0.35F, 0.5F).fixedRotation().sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("scan").burst(20).shape(Shape.box(8, 6, 8)).offset(0, 6, 0).velocity(Velocity.up(0.4F, 0.8F, 0)).life(8, 14)
						.size(0.12F, 0.18F).mode(Mode.STRETCHED).stretch(5).sprite(FxSprites.STREAK).color(GLITCH).alphaOverLife(FLICKER).build())
				.build());

		// The fanfare: coins and confetti raining over the whole grid.
		FxEngine.register(EffectDef.builder(VfxIds.GRID_FANFARE).lod(300, 2000).maxAge(220)
				.emit(EmitterDef.builder("coins").priority(Priority.HERO).rate(14, 80).shape(Shape.box(64, 1, 64)).offset(0, 30, 0).velocity(Velocity.up(-0.4F, -0.2F, 0.1F))
						.gravity(0.02F).collide(0.4F, 0.6F, 2).life(80, 120).size(0.6F, 0.9F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(COIN)
						.emissive(Curve.constant(0.8F)).spin(0.3F, 0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("confetti").rate(20, 80).shape(Shape.box(64, 1, 64)).offset(0, 30, 0).velocity(Velocity.up(-0.2F, -0.1F, 0.4F))
						.gravity(0.006F).drag(0.03F).module(Module.jitter(0.01F)).life(100, 160).size(0.3F, 0.45F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(CONFETTI).emissive(Curve.constant(0.6F)).spin(0.1F, 0.3F).build())
				.build());

		// A swap, played along the line from one chunk to the other: glitter streaming across.
		FxEngine.register(EffectDef.builder(VfxIds.CHUNK_SWAP).lod(128, 900)
				.emit(EmitterDef.builder("stream").burst(60).span().shape(Shape.sphere(1.5F)).velocity(Velocity.along(0.3F, 0.6F, 0.2F)).life(14, 26).size(0.2F, 0.3F)
						.sprite(FxSprites.FLARE).color(Gradient.of(0, 0xFFFFE0, 1, argb(0xFFE040, 0))).alphaOverLife(TWINKLE).build())
				.build());

		// A cracked corner: a geyser of pixels, tinted, and static crawling round it.
		FxEngine.register(EffectDef.builder(VfxIds.CORNER_GEYSER).tint(0xFF40FF).lod(200, 1400).maxAge(120)
				.emit(EmitterDef.builder("geyser").priority(Priority.HERO).rate(12, 30, Curve.FALL).shape(Shape.disc(1.5F)).velocity(Velocity.up(1.0F, 2.0F, 0.12F))
						.gravity(0.03F).life(30, 50).size(0.3F, 0.45F).fixedRotation().sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("static").burst(30).onGround().shape(Shape.disc(4)).offset(0, 0.3F, 0).velocity(Velocity.random(0.05F, 0.15F)).life(4, 10)
						.size(0.05F, 0.08F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(GLITCH).build())
				.build());
	}

	// ------------------------------------------------------------------ Disco Ball

	private static void discoBall() {
		// Where a beam lands, tinted the beam's colour: mirror sparkle.
		FxEngine.register(EffectDef.builder(VfxIds.BEAM_SPARKLE).tint(0xFF60FF).lod(96, 600)
				.emit(EmitterDef.builder("sparkle").burst(5).shape(Shape.disc(1.2F)).offset(0, 0.4F, 0).velocity(Velocity.up(0.04F, 0.12F, 0.4F)).life(8, 16)
						.size(0.25F, 0.4F).sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("spot").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(1.6F, 1.6F).life(6, 6)
						.sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.5F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The drop: a floor-level wave of glitter racing out and a fountain of mirror shards off the ball.
		FxEngine.register(EffectDef.builder(VfxIds.DISCO_DROP).lod(500, 3000).maxAge(300)
				.emit(EmitterDef.builder("wave").priority(Priority.HERO).burst(900).onGround().shape(Shape.ring(4, 1)).offset(0, 0.5F, 0)
						.velocity(Velocity.radial(2.6F, 3.4F)).drag(0.008F).life(70, 90).size(0.8F, 1.1F).sprite(FxSprites.FLARE).color(CONFETTI)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(300).shape(Shape.sphere(4)).offset(0, 40, 0).velocity(Velocity.random(0.8F, 1.8F))
						.gravity(0.03F).collide(0.4F, 0.6F, 2).life(80, 140).size(0.3F, 0.5F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(MIRROR)
						.emissive(Curve.constant(0.9F)).spin(0.2F, 0.5F).alphaOverLife(TWINKLE).build())
				.build());

		// On the beat after the drop, tinted: a ring of the dance floor lights up and fades.
		FxEngine.register(EffectDef.builder(VfxIds.FLOOR_PULSE).tint(0xFF40FF).lod(200, 1400)
				.emit(EmitterDef.builder("tiles").burst(90).onGround().shape(Shape.ring(40, 2)).offset(0, 0.15F, 0).mode(Mode.FLAT).fixedRotation().life(10, 16)
						.size(1.3F, 1.6F).sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ LiDAR

	private static void lidar() {
		// A strike: a red scan line comes down and what it hits bursts into a point cloud that freezes and fades.
		FxEngine.register(EffectDef.builder(VfxIds.LIDAR_HIT).lod(200, 1400).maxAge(140)
				.emit(EmitterDef.builder("scan").burst(30).shape(Shape.column(0.3F, 60)).offset(0, 2, 0).velocity(Velocity.up(-8.0F, -6.0F, 0)).life(8, 10)
						.size(0.3F, 0.4F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(TARGET).build())
				.emit(6, flash("flash", 8, 5, 0xFF8070).build())
				.emit(6, EmitterDef.builder("cloud").priority(Priority.HERO).burst(260).shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.3F, 0.9F)).drag(0.12F)
						.life(60, 100).size(0.1F, 0.14F).fixedRotation().sprite(FxSprites.FLARE).color(POINTS).alphaOverLife(Curve.keys(0, 1, 0.7F, 0.8F, 1, 0)).build())
				.emit(6, EmitterDef.builder("red").burst(60).shape(Shape.sphere(0.5F)).velocity(Velocity.random(0.2F, 0.6F)).drag(0.12F).life(40, 70)
						.size(0.1F, 0.14F).fixedRotation().sprite(FxSprites.FLARE).color(TARGET).build())
				.build());

		// The overload: the whole area scanned into shells of points that stop where they reach and hang there.
		EffectDef.Builder over = EffectDef.builder(VfxIds.LIDAR_OVERLOAD).lod(500, 3000).maxAge(260)
				.cue(0, EffectDef.light(1.2F, 0x90FFFF))
				.emit(flash("flash", 40, 10, 0xE0FFFF).build());
		for (int k = 0; k < 4; k++) {
			over.emit(k * 6, EmitterDef.builder("shell" + k).priority(Priority.HERO).burst(500).shape(Shape.sphere(1)).velocity(Velocity.random(1.6F + k * 0.8F, 1.8F + k * 0.8F))
					.drag(0.06F).life(120, 160).size(0.4F, 0.55F).fixedRotation().sprite(FxSprites.FLARE).color(POINTS)
					.alphaOverLife(Curve.keys(0, 1, 0.6F, 0.7F, 1, 0)).build());
		}
		FxEngine.register(over.build());
	}

	// ------------------------------------------------------------------ Shadow Void

	private static void shadowVoid() {
		// Something swallowed: a black puff of ink and a few violet motes drawn in.
		FxEngine.register(EffectDef.builder(VfxIds.VOID_SWALLOW).lod(96, 600)
				.emit(smoke("ink").burst(3).velocity(Velocity.random(0.02F, 0.06F)).life(30, 50).size(0.8F, 1.3F).color(INK).alpha(0.85F).shade(1).build())
				.emit(EmitterDef.builder("motes").burst(4).shape(Shape.shell(1.5F)).module(Module.attract(0.02F, 0.3F)).life(10, 18).size(0.08F, 0.12F)
						.sprite(FxSprites.FLARE).color(VOID_SPARK).build())
				.build());

		// The exhale: tendrils of ink thrown out and curling, violet sparks, a dark wash rolling over the ground.
		FxEngine.register(EffectDef.builder(VfxIds.VOID_EXHALE).lod(500, 3000).maxAge(360)
				.cue(0, EffectDef.light(1.0F, 0x8030FF))
				.emit(smoke("tendrils").priority(Priority.HERO).burst(120).shape(Shape.sphere(4)).velocity(Velocity.random(1.0F, 2.2F)).drag(0.03F)
						.module(Module.curl(14, 0.03F)).life(140, 220).size(5, 9).color(INK).alpha(0.8F).shade(1).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.HERO).burst(400).shape(Shape.sphere(2)).velocity(Velocity.random(1.2F, 2.8F)).drag(0.03F)
						.life(30, 60).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(VOID_SPARK).build())
				.emit(smoke("wash").rate(40, 30, Curve.FALL).onGround().shape(Shape.ring(20, 6)).expand(3).velocity(Velocity.radial(0.4F, 0.8F)).life(100, 160)
						.size(8, 12).color(Gradient.of(0, 0x1A1028, 1, 0x2A2038)).alpha(0.6F).build())
				.build());
	}

	// ------------------------------------------------------------------ Spectral Ice

	private static void spectralIce() {
		// A spike erupts: pale ghost-ice shards and cyan wisps curling up off it.
		FxEngine.register(EffectDef.builder(VfxIds.ICE_ERUPT).lod(160, 1000)
				.emit(EmitterDef.builder("shards").burst(50).shape(Shape.disc(2)).velocity(Velocity.up(0.5F, 1.2F, 0.6F)).gravity(0.04F).collide(0.3F, 0.6F, 2)
						.life(30, 50).size(0.2F, 0.35F).mode(Mode.STRETCHED).stretch(1.5F).sprite(FxSprites.FLARE).color(GHOST_ICE).build())
				.emit(EmitterDef.builder("wisps").burst(8).shape(Shape.disc(2)).velocity(Velocity.up(0.1F, 0.3F, 0.3F)).module(Module.curl(4, 0.008F)).life(50, 90)
						.size(0.8F, 1.3F).sprite(FxSprites.WISP).color(GHOST_WISP).alpha(0.7F).build())
				.build());

		// A shard lands: crystal splinters and a cold mist.
		FxEngine.register(EffectDef.builder(VfxIds.SHARD_LAND).lod(128, 900)
				.emit(EmitterDef.builder("splinters").burst(30).velocity(Velocity.up(0.3F, 0.8F, 0.8F)).gravity(0.04F).collide(0.3F, 0.6F, 2).life(20, 40)
						.size(0.12F, 0.2F).mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(GHOST_ICE).build())
				.emit(smoke("mist").burst(6).onGround().shape(Shape.ring(1, 1)).velocity(Velocity.radial(0.06F, 0.12F)).life(50, 80).size(1.5F, 2.5F)
						.color(Gradient.of(0, 0xE8F8FF, 1, 0xC0E0F0)).alpha(0.5F).shade(0.95F).build())
				.build());
	}

	// ------------------------------------------------------------------ Sonar

	private static void sonar() {
		// Something brittle bursts in the echo: glints.
		FxEngine.register(EffectDef.builder(VfxIds.SONAR_SHATTER).lod(64, 300)
				.emit(EmitterDef.builder("glints").burst(8).velocity(Velocity.random(0.1F, 0.3F)).gravity(0.03F).life(10, 20).size(0.08F, 0.12F)
						.mode(Mode.STRETCHED).sprite(FxSprites.FLARE).color(SONAR).build())
				.build());

		// The boom: dust kicked up in concentric rings all at once, a green pulse on every ring.
		EffectDef.Builder boom = EffectDef.builder(VfxIds.SONAR_BOOM).lod(400, 2400).maxAge(160)
				.cue(0, EffectDef.light(0.8F, 0x40FF80));
		for (int k = 0; k < 6; k++) {
			float r = 12 + k * 22;
			boom.emit(k * 2, smoke("dust" + k).burst(30 + k * 10).onGround().shape(Shape.ring(r, 1.5F)).velocity(Velocity.up(0.15F, 0.35F, 0.2F)).life(40, 70)
					.size(2, 3.5F).color(VfxLibrary.DUST).alpha(0.55F).build());
			boom.emit(k * 2, EmitterDef.builder("pulse" + k).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(r + 2, r + 2).life(12, 12)
					.sprite(FxSprites.SHOCK).alpha(0.5F).alphaOverLife(Curve.FADE_OUT).color(0x40FF80).build());
		}
		FxEngine.register(boom.build());
	}

	// ------------------------------------------------------------------ Pixel Bomb

	private static void pixels() {
		FxEngine.register(EffectDef.builder(VfxIds.PIXEL_BURST).lod(200, 1400).maxAge(200)
				.emit(flash("flash", 10, 6, 0xFF80FF).build())
				.emit(EmitterDef.builder("pixels").priority(Priority.HERO).burst(400).shape(Shape.sphere(1)).velocity(Velocity.up(0.5F, 1.4F, 1.0F)).gravity(0.04F)
						.collide(0.5F, 0.7F, 3).life(60, 100).size(0.25F, 0.4F).fixedRotation().layer(Layer.SMOKE).sprite(FxSprites.DUST, 2).color(PIXELS)
						.emissive(Curve.constant(0.9F)).build())
				.emit(EmitterDef.builder("coins").burst(30).shape(Shape.sphere(1)).velocity(Velocity.up(0.4F, 1.0F, 0.8F)).gravity(0.04F).collide(0.5F, 0.7F, 3)
						.life(60, 100).size(0.3F, 0.4F).sprite(FxSprites.FLARE).color(COIN).alphaOverLife(FLICKER).build())
				.build());
	}

	// ------------------------------------------------------------------ Magnetar

	private static void magnetar() {
		// Ground lifting: sparks riding the field lines up.
		FxEngine.register(EffectDef.builder(VfxIds.MAG_LIFT).lod(96, 600)
				.emit(EmitterDef.builder("sparks").burst(6).velocity(Velocity.swirl(0.2F, 0.4F)).velocity(Velocity.up(0.3F, 0.6F, 0.2F)).life(10, 20).size(0.06F, 0.1F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FIELD).build())
				.build());

		// A block in orbit ground to dust.
		FxEngine.register(EffectDef.builder(VfxIds.MAG_GRIND).lod(96, 600)
				.emit(smoke("dust").burst(3).velocity(Velocity.random(0.05F, 0.12F)).life(30, 50).size(0.8F, 1.2F).color(VfxLibrary.DUST).alpha(0.55F).build())
				.emit(EmitterDef.builder("crumbs").burst(8).velocity(Velocity.random(0.1F, 0.25F)).gravity(0.03F).life(16, 30).size(0.08F, 0.14F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(Gradient.of(0, 0x7A6A5A, 1, 0x5A4A3A)).build())
				.build());

		// Where the jet cuts: a cyan plasma splash.
		FxEngine.register(EffectDef.builder(VfxIds.MAG_JET_HIT).lod(128, 900)
				.emit(flash("flash", 3, 3, 0xC0E8FF).build())
				.emit(EmitterDef.builder("splash").burst(14).velocity(Velocity.up(0.3F, 0.7F, 0.8F)).gravity(0.04F).life(10, 20).size(0.08F, 0.14F)
						.mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(FIELD).build())
				.build());

		// The collapse: field lines snap, streaks flung out along a dipole, top and bottom hardest.
		FxEngine.register(EffectDef.builder(VfxIds.MAG_COLLAPSE).lod(300, 2000).maxAge(160)
				.emit(flash("flash", 24, 10, 0xC0E8FF).build())
				.emit(EmitterDef.builder("poles").priority(Priority.HERO).burst(200).velocity(Velocity.up(1.6F, 3.0F, 0.35F)).drag(0.03F).life(30, 50).size(0.3F, 0.45F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FIELD).build())
				.emit(EmitterDef.builder("poles_down").burst(200).velocity(Velocity.up(-3.0F, -1.6F, 0.35F)).drag(0.03F).life(30, 50).size(0.3F, 0.45F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FIELD).build())
				.emit(EmitterDef.builder("equator").burst(200).shape(Shape.ring(1, 0.2F)).velocity(Velocity.radial(0.8F, 1.6F)).drag(0.03F).life(30, 50)
						.size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(FIELD).build())
				.build());
	}

	// ------------------------------------------------------------------ Ink Bomb

	private static void comic() {
		// Comic book: flat, bold, unshaded puffs in banded yellow-orange-red, black speed lines, ink spatter.
		FxEngine.register(EffectDef.builder(VfxIds.COMIC_BLAST).lod(160, 1000).maxAge(120)
				.emit(smoke("puffs").priority(Priority.HERO).burst(18).shape(Shape.sphere(1.5F)).velocity(Velocity.random(0.25F, 0.5F)).drag(0.12F).life(20, 30)
						.size(2.2F, 3.4F).sizeOverLife(Curve.POP).color(COMIC).alpha(1).shade(1).emissive(Curve.constant(1)).alphaOverLife(Curve.keys(0, 1, 0.8F, 1, 1, 0))
						.build())
				.emit(EmitterDef.builder("lines").burst(40).shape(Shape.sphere(2)).velocity(Velocity.radial(0.8F, 1.4F)).drag(0.08F).life(8, 14).size(0.12F, 0.18F)
						.mode(Mode.STRETCHED).stretch(6).layer(Layer.SMOKE).sprite(FxSprites.STREAK).color(0x101010).build())
				.emit(EmitterDef.builder("ink").burst(20).velocity(Velocity.up(0.3F, 0.7F, 0.9F)).gravity(0.04F).life(20, 30).size(0.15F, 0.3F).layer(Layer.SMOKE)
						.sprite(FxSprites.DUST, 2).color(0x101010).build())
				.build());
	}

	// ------------------------------------------------------------------ Aurora

}
