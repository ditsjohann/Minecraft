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
import static com.armageddon.client.vfx.VfxLibrary.INNER_GLOW;
import static com.armageddon.client.vfx.VfxLibrary.SOOT;
import static com.armageddon.client.vfx.VfxLibrary.SPARK;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;
import static com.armageddon.client.vfx.VfxLibrary.sparkShower;
import static com.armageddon.client.vfx.WeaponVfx.fade;

/**
 * The legendary and mythic callers: Solar Verdict, Titanfall, Reality Shear, the Judgment Engine, Riftfall, the
 * Apocalypse Spindle and the Extinction Choir.
 */
final class LegendVfx {
	private LegendVfx() {
	}

	static final Gradient SUN = Gradient.of(0, 0xFFFFF0, 0.3F, 0xFFE880, 0.7F, 0xFFB030, 1, argb(0x804010, 0));
	static final Gradient GOLD = fade(0xFFE070, 0x906010);
	static final Gradient SHEAR = Gradient.of(0, 0xFFFFFF, 0.4F, 0xC0E8FF, 1, argb(0x4080C0, 0));
	static final Gradient CRIMSON = Gradient.of(0, 0xFFE0E0, 0.25F, 0xFF4060, 0.7F, 0xA01030, 1, argb(0x300010, 0));
	static final Gradient MOLTEN = Gradient.of(0, 0xFFFFD0, 0.2F, 0xFFC040, 0.5F, 0xFF4010, 0.8F, 0x901008, 1, argb(0x200000, 0));
	static final Gradient ASH = Gradient.of(0, 0x2A2420, 0.6F, 0x4A4038, 1, 0x6A6058);
	static final Gradient DUST = Gradient.of(0, 0x6A5A48, 1, 0x9A8A78);

	static void register() {
		verdict();
		titanfall();
		shear();
		judgment();
		riftfall();
		spindle();
		extinction();
	}

	private static EmitterDef groundRing(String name, float size, int life, int color) {
		return EmitterDef.builder(name).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(size, size).life(life, life)
				.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(color).build();
	}

	// ------------------------------------------------------------------ Solar Verdict

	private static void verdict() {
		// Each count of the countdown: a ring of sunlight motes rising off the condemned ground.
		FxEngine.register(EffectDef.builder(VfxIds.VERDICT_COUNT).lod(160, 1200)
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(28, 28).life(20, 20).sprite(FxSprites.RING)
						.color(GOLD).alpha(0.3F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("motes").priority(Priority.HERO).burst(60).onGround().shape(Shape.ring(14, 0.5F))
						.velocity(Velocity.up(0.15F, 0.35F, 0)).drag(0.02F).life(30, 50).size(0.25F, 0.4F).sprite(FxSprites.FLARE).color(SUN)
						.alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("rays").burst(20).onGround().shape(Shape.ring(14, 0.5F)).velocity(Velocity.up(0.8F, 1.2F, 0)).drag(0.05F)
						.life(12, 20).size(0.2F, 0.3F).mode(Mode.STRETCHED).stretch(4).sprite(FxSprites.STREAK).color(SUN).build())
				.build());

		// A judgement beam: a column of sunlight, a sunburst, glassing fire, and a glow that stays on the ground.
		FxEngine.register(EffectDef.builder(VfxIds.VERDICT_STRIKE).lod(200, 1600)
				.emit(flash("flash", 8, 6, 0xFFF0B0).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).burst(80).shape(Shape.column(2.5F, 90)).life(10, 22).size(3, 4.5F)
						.sprite(FxSprites.GLOW).color(SUN).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("sunburst").burst(40).onGround().velocity(Velocity.radial(0.8F, 1.4F)).velocity(Velocity.up(0.1F, 0.4F, 0.6F))
						.drag(0.1F).life(10, 18).size(0.15F, 0.22F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(SUN).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(24).shape(Shape.dome(3)).velocity(Velocity.radial(0.15F, 0.35F))
						.module(Module.buoyancy(0.02F)).drag(0.1F).life(18, 32).size(1.8F, 3).sprite(FxSprites.FIRE, 2).color(FIRE)
						.sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(groundRing("ring", 22, 18, 0xFFD060))
				.emit(EmitterDef.builder("glass").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(10, 10).life(160, 160).sprite(FxSprites.GLOW)
						.color(0xFF9030).alpha(0.7F).alphaOverLife(Curve.keys(0, 1, 0.5F, 0.6F, 1, 0)).build())
				.emit(6, smoke("smoke").rate(3, 80).onGround().shape(Shape.disc(3)).velocity(Velocity.up(0.25F, 0.45F, 0.15F)).drag(0.03F)
						.life(80, 140).size(2.2F, 3.4F).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.2F)).color(SOOT).alpha(0.7F).emissive(INNER_GLOW)
						.module(Module.wind(1.5F)).build())
				.emit(EmitterDef.builder("embers").priority(Priority.DETAIL).rate(4, 100).onGround().shape(Shape.disc(5))
						.velocity(Velocity.up(0.1F, 0.25F, 0.5F)).gravity(-0.002F).life(30, 60).size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(EMBER)
						.alphaOverLife(TWINKLE).build())
				.build());
	}

	// ------------------------------------------------------------------ Titanfall

	private static void titanfall() {
		// The warhead screams in from orbit at four blocks a tick: a white-hot nose, a plasma sheath and a black smoke wake.
		FxEngine.register(EffectDef.builder(VfxIds.TITAN_TRAIL).lod(200, 1600)
				.emit(EmitterDef.builder("nose").priority(Priority.HERO).burst(1).inherit(1).size(3, 3).life(2, 2).sprite(FxSprites.GLOW)
						.color(0xFFF0D0).build())
				.emit(EmitterDef.builder("sheath").priority(Priority.HERO).burst(10).span().shape(Shape.sphere(1)).velocity(Velocity.random(0.05F, 0.15F))
						.inherit(0.2F).life(8, 16).size(1.4F, 2.4F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(smoke("wake").priority(Priority.HERO).burst(6).span().velocity(Velocity.random(0.01F, 0.05F)).life(90, 160).size(1.6F, 2.6F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 3.2F)).color(SOOT).alpha(0.85F).emissive(INNER_GLOW).module(Module.wind(1.5F)).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(6).span().velocity(Velocity.random(0.1F, 0.3F)).gravity(0.02F)
						.life(15, 30).size(0.08F, 0.12F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(EMBER).build())
				.build());

		// Between the impact and the secondary blast: the crater glows, cracks vent fire and embers climb.
		FxEngine.register(EffectDef.builder(VfxIds.TITAN_BUILDUP).lod(160, 1200)
				.emit(EmitterDef.builder("glow").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(14, 14).life(12, 12).sprite(FxSprites.GLOW)
						.color(0xFF6020).alpha(0.7F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("vents").burst(12).onGround().shape(Shape.disc(8)).velocity(Velocity.up(0.3F, 0.6F, 0.1F)).drag(0.05F)
						.life(12, 22).size(1, 1.6F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("embers").burst(20).onGround().shape(Shape.disc(8)).velocity(Velocity.up(0.2F, 0.5F, 0.3F)).gravity(-0.002F)
						.life(30, 50).size(0.08F, 0.12F).sprite(FxSprites.FLARE).color(EMBER).alphaOverLife(TWINKLE).build())
				.emit(smoke("smoke").burst(4).onGround().shape(Shape.disc(6)).velocity(Velocity.up(0.15F, 0.3F, 0.2F)).life(60, 100).size(2, 3)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(SOOT).alpha(0.7F).emissive(INNER_GLOW).build())
				.build());
	}

	// ------------------------------------------------------------------ Reality Shear

	private static void shear() {
		// The cutting plane, drawn along the line: white flickering shards and cyan glitch streaks in a standing sheet.
		FxEngine.register(EffectDef.builder(VfxIds.SHEAR_PLANE).lod(160, 1200).maxAge(60)
				.emit(EmitterDef.builder("sheet").priority(Priority.HERO).rate(60, 50).span().shape(Shape.column(0.15F, 40)).offset(0, -8, 0)
						.life(6, 14).size(1.2F, 2).sprite(FxSprites.GLOW).color(SHEAR).alpha(0.55F).alphaOverLife(FLICKER).build())
				.emit(EmitterDef.builder("glitch").rate(24, 50).span().shape(Shape.column(0.4F, 40)).offset(0, -8, 0)
						.velocity(Velocity.random(0.05F, 0.2F)).drag(0.2F).life(4, 10).size(0.08F, 0.12F).mode(Mode.STRETCHED).stretch(3)
						.sprite(FxSprites.STREAK).color(SHEAR).build())
				.emit(EmitterDef.builder("pixels").priority(Priority.DETAIL).rate(8, 50).span().shape(Shape.column(1.5F, 40)).offset(0, -8, 0)
						.velocity(Velocity.up(0.02F, 0.08F, 0.5F)).life(15, 30).size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(0xE0F4FF)
						.alphaOverLife(FLICKER).build())
				.build());

		// The cut: a blinding sheet, then shards and dust blown off the severed edge.
		FxEngine.register(EffectDef.builder(VfxIds.SHEAR_CUT).lod(200, 1600)
				.emit(EmitterDef.builder("blind").priority(Priority.HERO).burst(160).span().shape(Shape.column(0.4F, 48)).offset(0, -10, 0)
						.life(8, 16).size(2.5F, 4).sprite(FxSprites.GLOW).color(SHEAR).alpha(0.8F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("shards").priority(Priority.HERO).burst(120).span().shape(Shape.column(0.5F, 40)).offset(0, -8, 0)
						.velocity(Velocity.random(0.3F, 0.9F)).drag(0.06F).gravity(0.01F).life(20, 40).size(0.12F, 0.2F).mode(Mode.STRETCHED)
						.sprite(FxSprites.STREAK).color(SHEAR).build())
				.emit(smoke("dust").burst(40).span().onGround().velocity(Velocity.up(0.1F, 0.3F, 0.6F)).drag(0.04F).life(60, 110).size(2, 3.2F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).sprite(FxSprites.DUST, 2).color(DUST).alpha(0.75F).build())
				.build());
	}

	// ------------------------------------------------------------------ Judgment Engine

	private static void judgment() {
		FxEngine.register(EffectDef.builder(VfxIds.JUDGMENT_TRAIL).lod(48, 320)
				.emit(EmitterDef.builder("core").burst(1).inherit(1).size(0.5F, 0.5F).life(2, 2).sprite(FxSprites.GLOW).color(0xFFE070).build())
				.emit(EmitterDef.builder("plasma").burst(3).span().velocity(Velocity.random(0.01F, 0.04F)).life(5, 9).size(0.25F, 0.35F)
						.sprite(FxSprites.GLOW).color(GOLD).alpha(0.7F).build())
				.emit(EmitterDef.builder("sparks").priority(Priority.DETAIL).burst(1).span().velocity(Velocity.random(0.04F, 0.1F)).gravity(0.01F)
						.life(8, 14).size(0.03F, 0.05F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(SPARK).build())
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.JUDGMENT_HIT).lod(64, 400)
				.emit(flash("flash", 2.5F, 3, 0xFFD060).build())
				.emit(EmitterDef.builder("plasma").burst(10).velocity(Velocity.radial(0.1F, 0.25F)).drag(0.12F).life(8, 14).size(0.6F, 1)
						.sprite(FxSprites.GLOW).color(GOLD).build())
				.emit(EmitterDef.builder("fire").burst(6).shape(Shape.sphere(0.6F)).velocity(Velocity.radial(0.05F, 0.12F)).module(Module.buoyancy(0.01F))
						.life(10, 18).size(0.8F, 1.2F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(sparkShower("sparks", 14, 0.2F, 0.5F, SPARK, null))
				.build());
		FxEngine.register(EffectDef.builder(VfxIds.JUDGMENT_VENT).lod(48, 300)
				.emit(smoke("steam").burst(20).velocity(Velocity.random(0.1F, 0.3F)).velocity(Velocity.up(0.1F, 0.2F, 0.5F)).drag(0.08F)
						.life(25, 45).size(0.6F, 1).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.4F)).color(0xF0F0F4).alpha(0.65F)
						.module(Module.buoyancy(0.01F)).build())
				.emit(sparkShower("sparks", 30, 0.2F, 0.5F, SPARK, null))
				.emit(EmitterDef.builder("glow").burst(1).size(2, 2).life(10, 10).sprite(FxSprites.GLOW).color(0xFF8020).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}

	// ------------------------------------------------------------------ Riftfall

	private static void riftfall() {
		// Each sky rift: a crimson whirlpool facing the ground, a black eye, wisps sucked in and lightning in the rim.
		FxEngine.register(EffectDef.builder(VfxIds.RIFT_MAW).lod(256, 2000).maxAge(290)
				.emit(EmitterDef.builder("eye").priority(Priority.HERO).rate(1, 280).mode(Mode.FLAT).fixedRotation().size(9, 9).life(6, 6)
						.layer(Layer.SMOKE).sprite(FxSprites.GLOW).color(0x080004).shade(1).build())
				.emit(EmitterDef.builder("rim").priority(Priority.HERO).rate(0.5F, 280).mode(Mode.FLAT).fixedRotation().size(20, 20).life(10, 10)
						.sprite(FxSprites.RING).color(CRIMSON).alpha(0.8F).alphaOverLife(Curve.BELL).build())
				.emit(EmitterDef.builder("whirl").priority(Priority.HERO).rate(12, 280).shape(Shape.ring(10, 4)).velocity(Velocity.swirl(0.4F, 0.6F))
						.module(Module.vortex(0.05F, 0.03F)).life(20, 35).size(1, 1.8F).sprite(FxSprites.WISP).color(CRIMSON).alpha(0.7F)
						.alphaOverLife(Curve.BELL).build())
				.emit(smoke("cloud").rate(4, 280).shape(Shape.ring(14, 6)).velocity(Velocity.swirl(0.15F, 0.25F)).module(Module.vortex(0.02F, 0.01F))
						.life(50, 80).size(5, 8).color(Gradient.of(0, 0x301018, 1, 0x501828)).alpha(0.8F).emissive(Curve.constant(0.3F)).build())
				.emit(EmitterDef.builder("arcs").priority(Priority.DETAIL).rate(1.5F, 280).shape(Shape.ring(11, 3)).life(2, 4).size(3, 5)
						.sprite(FxSprites.GLOW).color(0xFFA0C0).alphaOverLife(FLICKER).build())
				.build());

		// A rift beam's landing.
		FxEngine.register(EffectDef.builder(VfxIds.RIFT_HIT).lod(96, 700)
				.emit(flash("flash", 4, 4, 0xFF6080).build())
				.emit(EmitterDef.builder("plasma").burst(14).velocity(Velocity.radial(0.15F, 0.35F)).drag(0.1F).life(10, 18).size(1, 1.6F)
						.sprite(FxSprites.GLOW).color(CRIMSON).build())
				.emit(groundRing("ring", 7, 12, 0xFF5070))
				.emit(sparkShower("sparks", 20, 0.3F, 0.7F, CRIMSON, null))
				.emit(smoke("smoke").burst(6).velocity(Velocity.up(0.05F, 0.12F, 0.5F)).life(40, 70).size(1, 1.6F).color(SOOT).alpha(0.7F)
						.emissive(INNER_GLOW).build())
				.build());

		// The rifts fold shut: everything around is dragged into one point.
		FxEngine.register(EffectDef.builder(VfxIds.RIFT_COLLAPSE).lod(256, 2000).maxAge(40)
				.emit(EmitterDef.builder("inrush").priority(Priority.HERO).rate(40, 28).shape(Shape.shell(30)).module(Module.attract(0.2F, 2))
						.life(20, 30).size(0.25F, 0.4F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(CRIMSON).build())
				.emit(EmitterDef.builder("dark").priority(Priority.HERO).burst(1).size(14, 14).life(30, 30).layer(Layer.SMOKE).sprite(FxSprites.GLOW)
						.color(0x0A0008).shade(1).sizeOverLife(Curve.keys(0, 0.4F, 0.8F, 1.2F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("halo").burst(1).size(30, 30).life(30, 30).sprite(FxSprites.RING).color(CRIMSON)
						.sizeOverLife(Curve.keys(0, 1.4F, 1, 0.2F)).alphaOverLife(Curve.BELL).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.RIFT_BLAST).lod(300, 2400).maxAge(600)
				.emit(flash("flash", 30, 8, 0xFFFFFF).layer(Layer.GLOW_TOP).build())
				.emit(EmitterDef.builder("core").priority(Priority.HERO).burst(1).size(26, 26).life(30, 30).sprite(FxSprites.GLOW)
						.color(Gradient.of(0, 0xFFFFFF, 0.3F, 0xFF90B0, 1, 0xA01030)).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("plasma").priority(Priority.HERO).burst(60).shape(Shape.sphere(4)).velocity(Velocity.radial(0.6F, 1.2F))
						.drag(0.08F).life(20, 40).size(3, 5).sprite(FxSprites.GLOW).color(CRIMSON).build())
				.emit(EmitterDef.builder("shock").priority(Priority.HERO).burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(90, 90).life(36, 36)
						.sprite(FxSprites.SHOCK).sizeOverLife(Curve.EASE_OUT).alphaOverLife(Curve.FADE_OUT).color(0xFF6080).build())
				.emit(smoke("dust").rate(30, 30).onGround().shape(Shape.ring(8, 3)).velocity(Velocity.radial(1.2F, 1.6F)).drag(0.035F)
						.life(60, 100).size(3, 5).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.6F)).sprite(FxSprites.DUST, 2).color(DUST).alpha(0.8F).build())
				.emit(10, smoke("pillar").priority(Priority.HERO).rate(10, 120, Curve.FALL).shape(Shape.disc(6)).velocity(Velocity.up(0.4F, 0.7F, 0.15F))
						.drag(0.03F).life(120, 200).size(4, 6).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(Gradient.of(0, 0x301018, 1, 0x5A4048))
						.alpha(0.85F).emissive(Curve.keys(0, 1, 0.3F, 0.4F, 0.6F, 0)).module(Module.wind(1)).build())
				.emit(sparkShower("sparks", 80, 0.8F, 1.6F, CRIMSON, null))
				.build());
	}

	// ------------------------------------------------------------------ Apocalypse Spindle

	private static void spindle() {
		// The drilled shaft erupts: a fountain of molten spray and fire out of the hole for the whole sweep.
		FxEngine.register(EffectDef.builder(VfxIds.SPINDLE_SHAFT).lod(200, 1600).maxAge(170)
				.emit(flash("flash", 8, 5, 0xFF6040).build())
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).rate(10, 150).shape(Shape.disc(3)).velocity(Velocity.up(0.8F, 1.4F, 0.2F))
						.gravity(0.05F).collide(0.2F, 0.7F, 1).life(30, 50).size(0.25F, 0.4F).sprite(FxSprites.FLARE).color(MOLTEN).build())
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).rate(6, 150).shape(Shape.disc(3)).velocity(Velocity.up(0.3F, 0.6F, 0.15F))
						.module(Module.buoyancy(0.01F)).life(15, 28).size(2, 3).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("glow").rate(0.5F, 150).onGround().mode(Mode.FLAT).fixedRotation().size(12, 12).life(8, 8).sprite(FxSprites.GLOW)
						.color(0xFF4020).alpha(0.7F).alphaOverLife(Curve.BELL).build())
				.emit(smoke("smoke").rate(3, 160).shape(Shape.disc(4)).velocity(Velocity.up(0.35F, 0.55F, 0.15F)).drag(0.03F).life(100, 160)
						.size(3, 4.5F).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(SOOT).alpha(0.8F).emissive(INNER_GLOW).module(Module.wind(1.5F))
						.build())
				.build());

		// Where the sweeping beam bites the ground: molten spray, fire and smoke along the trench.
		FxEngine.register(EffectDef.builder(VfxIds.SPINDLE_SWEEP).lod(160, 1200)
				.emit(EmitterDef.builder("spray").burst(8).velocity(Velocity.up(0.4F, 0.8F, 0.6F)).gravity(0.05F).collide(0.2F, 0.7F, 1).life(20, 35)
						.size(0.15F, 0.25F).sprite(FxSprites.FLARE).color(MOLTEN).build())
				.emit(EmitterDef.builder("fire").burst(3).shape(Shape.sphere(1.2F)).velocity(Velocity.up(0.1F, 0.2F, 0.3F)).module(Module.buoyancy(0.01F))
						.life(12, 22).size(1.2F, 2).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(4, 4).life(60, 60).sprite(FxSprites.GLOW)
						.color(0xFF5020).alpha(0.6F).alphaOverLife(Curve.keys(0, 1, 0.5F, 0.6F, 1, 0)).build())
				.emit(smoke("smoke").burst(1.5F).velocity(Velocity.up(0.1F, 0.2F, 0.3F)).life(60, 100).size(1.6F, 2.4F)
						.sizeOverLife(Curve.keys(0, 0.6F, 1, 2)).color(SOOT).alpha(0.7F).emissive(INNER_GLOW).module(Module.wind(1)).build())
				.build());

		FxEngine.register(EffectDef.builder(VfxIds.SPINDLE_PULSE).lod(300, 2400).maxAge(400)
				.emit(flash("flash", 24, 8, 0xFF6060).layer(Layer.GLOW_TOP).build())
				.emit(groundRing("ring", 90, 40, 0xFF4040))
				.emit(4, groundRing("ring2", 60, 30, 0xFFA080))
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).burst(50).shape(Shape.ring(20, 3)).onGround().velocity(Velocity.up(0.3F, 0.7F, 0.2F))
						.module(Module.buoyancy(0.02F)).life(20, 40).size(3, 4.5F).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(smoke("dust").rate(40, 30).onGround().shape(Shape.ring(10, 4)).velocity(Velocity.radial(1.3F, 1.8F)).drag(0.035F).life(60, 100)
						.size(3, 5).sizeOverLife(Curve.keys(0, 0.5F, 1, 2.6F)).sprite(FxSprites.DUST, 2).color(DUST).alpha(0.8F).build())
				.emit(sparkShower("sparks", 80, 0.8F, 1.6F, MOLTEN, null))
				.build());
	}

	// ------------------------------------------------------------------ Extinction Choir

	private static void extinction() {
		// The cracked sky sheds ash and embers over the whole valley until the end.
		FxEngine.register(EffectDef.builder(VfxIds.EXTINCTION_ASH).lod(300, 2400).maxAge(260)
				.emit(EmitterDef.builder("ash").rate(14, 250).offset(0, 40, 0).shape(Shape.disc(60)).velocity(Velocity.random(0.02F, 0.06F)).gravity(0.004F)
						.module(Module.wind(1)).module(Module.jitter(0.004F)).life(120, 200).size(0.15F, 0.25F).layer(Layer.SMOKE).sprite(FxSprites.DUST, 2)
						.color(ASH).spin(0.05F, 0.15F).build())
				.emit(EmitterDef.builder("embers").rate(8, 250).offset(0, 40, 0).shape(Shape.disc(60)).velocity(Velocity.random(0.02F, 0.06F))
						.gravity(0.003F).module(Module.wind(1)).life(100, 180).size(0.12F, 0.2F).sprite(FxSprites.FLARE).color(EMBER)
						.alphaOverLife(TWINKLE).build())
				.emit(smoke("haze").rate(0.6F, 250).offset(0, 50, 0).shape(Shape.disc(70)).velocity(Velocity.random(0.02F, 0.05F)).life(150, 220)
						.size(16, 24).color(Gradient.of(0, 0x402018, 1, 0x502820)).alpha(0.35F).emissive(Curve.constant(0.25F)).build())
				.build());

		// The giant impactor's fall: a burning seam drawn down the sky from the sky crack to the ground.
		FxEngine.register(EffectDef.builder(VfxIds.IMPACTOR_STREAK).lod(400, 3000).maxAge(80)
				.emit(EmitterDef.builder("fire").priority(Priority.HERO).rate(30, 40, Curve.RISE).span().shape(Shape.sphere(3))
						.velocity(Velocity.random(0.05F, 0.15F)).life(20, 40).size(4, 7).sprite(FxSprites.FIRE, 2).color(FIRE).sizeOverLife(Curve.POP)
						.alphaOverLife(Curve.FADE).build())
				.emit(smoke("wake").priority(Priority.HERO).rate(16, 40, Curve.RISE).span().shape(Shape.sphere(4)).velocity(Velocity.random(0.02F, 0.06F))
						.life(120, 200).size(5, 8).sizeOverLife(Curve.keys(0, 0.6F, 1, 2.4F)).color(SOOT).alpha(0.85F).emissive(INNER_GLOW)
						.module(Module.wind(1)).build())
				.emit(EmitterDef.builder("glow").rate(8, 40, Curve.RISE).span().life(6, 12).size(8, 12).sprite(FxSprites.GLOW).color(0xFFB060)
						.alpha(0.6F).build())
				.build());

		// The giant impact adds a firestorm wall racing outward and ash thrown to the sky on top of the mushroom cloud.
		FxEngine.register(EffectDef.builder(VfxIds.EXTINCTION_WALL).lod(400, 3000).maxAge(500)
				.emit(EmitterDef.builder("wall_fire").priority(Priority.HERO).rate(30, 40).onGround().shape(Shape.ring(10, 4)).expand(2.2F)
						.velocity(Velocity.up(0.3F, 0.6F, 0.2F)).module(Module.buoyancy(0.02F)).life(20, 35).size(5, 8).sprite(FxSprites.FIRE, 2)
						.color(FIRE).sizeOverLife(Curve.POP).alphaOverLife(Curve.FADE).build())
				.emit(smoke("wall_dust").priority(Priority.HERO).rate(30, 60).onGround().shape(Shape.ring(10, 4)).expand(2.0F)
						.velocity(Velocity.up(0.2F, 0.5F, 0.3F)).drag(0.02F).life(80, 140).size(6, 10).sizeOverLife(Curve.keys(0, 0.5F, 1, 2))
						.sprite(FxSprites.DUST, 2).color(DUST).alpha(0.85F).emissive(Curve.keys(0, 0.8F, 0.3F, 0)).build())
				.emit(EmitterDef.builder("ejecta").priority(Priority.HERO).burst(80).shape(Shape.disc(10)).velocity(Velocity.up(1.2F, 2.2F, 0.6F))
						.gravity(0.04F).life(60, 110).size(0.6F, 1).sprite(FxSprites.FLARE).color(MOLTEN).build())
				.build());
	}
}
