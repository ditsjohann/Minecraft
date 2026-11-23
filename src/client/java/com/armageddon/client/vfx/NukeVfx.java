package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/**
 * The mushroom cloud every nuke in the mod raises. About 30 blocks to the cap at scale 1.
 *
 * <p>The cap is born as a ring of fire and smoke around the fireball and climbs as one rolling vortex ring
 * ({@link Module#roll}): smoke goes up through the middle, out over the top and back under the rim, widening as it
 * slows. The stem is dust drawn up off the ground and narrowed by the inflow. Hot at first (orange, lit from inside),
 * the cloud cools to its palette's smoke.
 *
 * <p>Six looks share the name and each blast picks one from its seed: classic, tall and thin with condensation
 * collars, squat and wide on a dust skirt, humid with an ice cap, sooty and leaning in the wind, and a rosy one.
 */
final class NukeVfx {
	private NukeVfx() {
	}

	/** A cloud's palette: the smoke as it cools (hot, mid, cold) and the stem's dust. */
	private record Palette(int hot, int mid, int cold, int dustLow, int dustHigh) {
		Gradient cap() {
			return Gradient.of(0, 0xFFB060, 0.06F, hot, 0.3F, mid, 1, cold);
		}

		Gradient stem() {
			return Gradient.of(0, dustLow, 1, dustHigh);
		}
	}

	private static final Palette CLASSIC = new Palette(0x8A3A1C, 0x6E5A50, 0x968A82, 0x6A5444, 0x9A8C80);
	private static final Palette ASH = new Palette(0x6A2A18, 0x4E4844, 0x7E7C7A, 0x504640, 0x7A7470);
	private static final Palette DESERT = new Palette(0x9A4A1A, 0x8E7050, 0xB8A486, 0x8A7050, 0xB8A486);
	private static final Palette HUMID = new Palette(0x8A3E24, 0x8E8682, 0xD2D0D2, 0x6E6258, 0xB4B0AE);
	private static final Palette SOOT = new Palette(0x5A1C10, 0x3A302C, 0x5E5652, 0x3E3632, 0x6A625C);
	private static final Palette ROSY = new Palette(0xA03A22, 0x9A6A5C, 0xBCA8A0, 0x7A5E50, 0xAE9A90);

	/**
	 * One look. {@code height}: blocks to the cap at scale 1. {@code width}: the cap ring's starting radius.
	 * {@code stem}: stem thickness factor. {@code lean}: how hard the wind pushes it. {@code collars}: condensation
	 * rings around the stem. {@code pileus}: a thin ice cap above. {@code skirt}: a dust skirt round the base.
	 * {@code wilson}: a white condensation shell flashes round the fireball. {@code roll}: circulation speed.
	 * {@code grow}: how fast the cap widens.
	 */
	private record Look(float height, float width, float stem, Palette palette, float lean, int collars, boolean pileus, boolean skirt,
			boolean wilson, float roll, float grow) {
	}

	private static final Look[] LOOKS = {
			new Look(30, 3.0F, 1.0F, CLASSIC, 0.6F, 0, false, true, true, 0.25F, 0.068F),
			new Look(38, 2.4F, 0.75F, ASH, 0.4F, 2, false, false, false, 0.3F, 0.051F),
			new Look(24, 4.2F, 1.35F, DESERT, 0.8F, 0, false, true, false, 0.2F, 0.102F),
			new Look(32, 3.0F, 1.0F, HUMID, 0.5F, 1, true, true, true, 0.25F, 0.076F),
			new Look(30, 3.0F, 0.9F, SOOT, 1.8F, 1, false, false, false, 0.25F, 0.068F),
			new Look(34, 3.6F, 1.1F, ROSY, 0.7F, 0, true, true, false, 0.3F, 0.085F),
	};

	static void register() {
		EffectDef[] defs = new EffectDef[LOOKS.length];
		for (int i = 0; i < LOOKS.length; i++) {
			defs[i] = mushroom(VfxIds.MUSHROOM + "#" + i, LOOKS[i]);
		}
		FxEngine.registerVariants(VfxIds.MUSHROOM, defs);
	}

	private static EffectDef mushroom(String name, Look l) {
		// The cap climbs at v0 and drag k brings it to rest near the look's height (distance = v0 / k).
		float k = 0.035F;
		float v0 = l.height * k;
		Palette c = l.palette;
		EffectDef.Builder b = EffectDef.builder(name).lod(300, 2000).maxAge(700);

		// The fireball's heart: fire rolling up inside the newborn cap.
		b.emit(EmitterDef.builder("fire").priority(Priority.HERO).rate(40, 8, Curve.FALL).offset(0, 2, 0).shape(Shape.torus(l.width, 1.8F))
				.velocity(Velocity.up(v0 * 0.97F, v0 * 1.03F, 0.04F)).module(Module.roll(l.roll, 2.2F, l.grow)).drag(k)
				.life(45, 80).size(3.0F, 4.5F).spin(0.005F, 0.02F).sprite(FxSprites.FIRE, 2)
				.color(Gradient.of(0, 0xFFF4D0, 0.15F, 0xFFC060, 0.45F, 0xFF6018, 0.8F, 0x9A2008, 1, argb(0x300800, 0)))
				.sizeOverLife(Curve.keys(0, 0.7F, 0.4F, 1.2F, 1, 1.4F)).alphaOverLife(Curve.FADE).build());

		// The cap: the same rolling ring in smoke, lit from inside while it is hot, darker underneath once cool.
		b.emit(smoke("cap").priority(Priority.HERO).rate(44, 8, Curve.FALL).offset(0, 2, 0).shape(Shape.torus(l.width, 1.8F))
				.velocity(Velocity.up(v0 * 0.97F, v0 * 1.03F, 0.04F)).module(Module.roll(l.roll, 2.2F, l.grow)).drag(k)
				.module(Module.wind(l.lean)).module(Module.curl(14, 0.003F))
				.life(320, 480).size(2.6F, 4.0F).sizeOverLife(Curve.keys(0, 0.6F, 0.15F, 1.0F, 0.5F, 1.5F, 1, 2.0F))
				.alphaOverLife(Curve.keys(0, 0, 0.04F, 0.95F, 0.7F, 0.85F, 1, 0)).alpha(0.95F).shade(0.5F)
				.color(c.cap()).emissive(Curve.keys(0, 1, 0.08F, 0.8F, 0.25F, 0)).build());

		// The stem: dust sucked up off the ground, pinched by the inflow, climbing into the cap.
		float ks = 0.025F;
		b.emit(8, smoke("stem").priority(Priority.HERO).rate(6, 300, Curve.keys(0, 1, 0.6F, 0.8F, 1, 0)).shape(Shape.disc(2.6F * l.stem))
				.velocity(Velocity.up(l.height * ks * 0.9F, l.height * ks * 1.05F, 0.04F)).module(Module.vortex(0.012F, 0.006F * l.stem))
				.drag(ks).module(Module.wind(l.lean * 0.7F)).module(Module.curl(8, 0.003F)).life(80, 120).size(1.6F * l.stem, 2.6F * l.stem)
				.sizeOverLife(Curve.keys(0, 0.7F, 1, 1.6F)).alphaOverLife(Curve.keys(0, 0, 0.1F, 0.85F, 0.75F, 0.7F, 1, 0)).alpha(0.9F)
				.shade(0.6F).color(c.stem()).emissive(Curve.keys(0, 0.5F, 0.2F, 0)).build());

		if (l.skirt) {
			// A skirt of dust heaped round the foot of the stem.
			b.emit(6, smoke("skirt").rate(10, 40, Curve.FALL).onGround().shape(Shape.ring(5 * l.stem, 3)).velocity(Velocity.radial(0.04F, 0.1F))
					.velocity(Velocity.up(0.05F, 0.14F, 0.3F)).drag(0.03F).module(Module.wind(l.lean)).life(140, 220).size(3, 4.5F)
					.sizeOverLife(Curve.keys(0, 0.5F, 0.4F, 1.3F, 1, 1.8F)).alpha(0.8F).shade(0.55F).sprite(FxSprites.DUST, 2)
					.color(c.stem()).build());
		}
		if (l.wilson) {
			// The Wilson cloud: a white shell of condensation flashing outward round the fireball.
			b.emit(3, smoke("wilson").burst(70).shape(Shape.dome(6)).velocity(Velocity.radial(0.35F, 0.5F)).drag(0.08F).life(16, 26)
					.size(3, 4.5F).sizeOverLife(Curve.keys(0, 0.6F, 1, 1.8F)).alphaOverLife(Curve.keys(0, 0, 0.15F, 0.6F, 1, 0)).alpha(0.7F)
					.shade(0.9F).color(0xF4F4F6).build());
		}
		for (int i = 0; i < l.collars; i++) {
			// Condensation collars: white rings left behind as the stem climbs through damp air.
			float h = l.height * (0.38F + 0.22F * i);
			b.emit(22 + i * 8, smoke("collar" + i).rate(12, 22, Curve.BELL).offset(0, h, 0).shape(Shape.ring(3.5F * l.stem + 1.5F, 1))
					.velocity(Velocity.radial(0.06F, 0.14F)).drag(0.04F).module(Module.wind(l.lean)).life(70, 110).size(2.4F, 3.6F)
					.sizeOverLife(Curve.keys(0, 0.6F, 1, 2.0F)).alphaOverLife(Curve.keys(0, 0, 0.15F, 0.7F, 1, 0)).alpha(0.75F).shade(0.85F)
					.color(0xF0F0F4).build());
		}
		if (l.pileus) {
			// A pileus: the climbing cap pushes damp air ahead of it into a thin ice cap that hangs above.
			b.emit(24, smoke("pileus").rate(8, 26, Curve.BELL).offset(0, l.height * 1.3F, 0).shape(Shape.disc(l.width * 2.4F))
					.velocity(Velocity.radial(0.03F, 0.08F)).module(Module.wind(l.lean)).life(80, 130).size(6, 9).mode(Mode.FLAT)
					.sizeOverLife(Curve.keys(0, 0.6F, 1, 1.4F)).alphaOverLife(Curve.keys(0, 0, 0.25F, 0.5F, 1, 0)).alpha(0.55F).shade(0.9F)
					.color(0xF6F8FC).build());
		}
		// Burning debris dropping out of the cap.
		b.emit(30, EmitterDef.builder("embers").priority(Priority.DETAIL).rate(5, 120).offset(0, l.height * 0.85F, 0)
				.shape(Shape.ring(l.width * 2.2F, l.width * 1.5F)).velocity(Velocity.random(0.01F, 0.04F)).gravity(0.008F).drag(0.03F)
				.module(Module.jitter(0.002F)).life(60, 110).size(0.1F, 0.16F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK)
				.color(Gradient.of(0, 0xFFD070, 0.6F, 0xFF5010, 1, argb(0x200000, 0))).alphaOverLife(Curve.FADE).build());
		return b.build();
	}
}
