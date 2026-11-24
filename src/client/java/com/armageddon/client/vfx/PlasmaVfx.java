package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import com.armageddon.client.vfx.EmitterDef.Layer;
import com.armageddon.client.vfx.EmitterDef.Mode;
import com.armageddon.client.vfx.EmitterDef.Priority;
import com.armageddon.vfx.VfxIds;

import static com.armageddon.client.vfx.Gradient.argb;
import static com.armageddon.client.vfx.VfxLibrary.TWINKLE;
import static com.armageddon.client.vfx.VfxLibrary.flash;
import static com.armageddon.client.vfx.VfxLibrary.smoke;

/**
 * The plasma bombs' particles. Plasma Koi: the pond's shimmer and its opening, ripples where the koi swim, drips off
 * them as they leap, a breach fountain where they leave the surface, a scorching splash where they land, the geyser
 * when the dragon dives home, and the plasma rain after it. The koi and the pond themselves are drawn by WildFx.
 */
final class PlasmaVfx {
	private PlasmaVfx() {
	}

	static final Gradient PLASMA = Gradient.of(0, 0xFFFFFF, 0.3F, 0xC0FFFF, 0.65F, 0x50D8FF, 1, argb(0x1060C0, 0));
	static final Gradient WHITE = Gradient.of(0, 0xFFFFFF, 1, 0xF0F0F0);
	static final Gradient MIST = Gradient.of(0, 0xE0F8FF, 1, 0xA8D8E8);
	static final Gradient SCORCH = Gradient.of(0, 0x404850, 1, 0x2A3038);
	static final Gradient KOI_COLORS = Gradient.of(0, 0x20C8FF, 0.17F, 0xFF30B0, 0.33F, 0xFFA010, 0.5F, 0x90C8FF, 0.67F, 0x7040FF,
			0.83F, 0x30FF80, 1, 0xFF5030);

	static void register() {
		koi();
	}

	private static void koi() {
		// The pond opens: a ring of plasma mist spreading, sparks, light lifting off the new surface.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_POND).lod(200, 1400).maxAge(120)
				.cue(0, EffectDef.light(0.9F, 0x60F0FF))
				.emit(smoke("mist").rate(10, 30, Curve.FALL).shape(Shape.ring(2, 1)).expand(0.45F).velocity(Velocity.radial(0.05F, 0.12F)).life(50, 80)
						.size(2.5F, 4).color(MIST).alpha(0.45F).shade(1).emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("light").rate(6, 40).shape(Shape.disc(13)).velocity(Velocity.up(0.1F, 0.3F, 0.1F)).life(30, 50).size(0.2F, 0.35F)
						.sprite(FxSprites.FLARE).color(PLASMA).alphaOverLife(TWINKLE).build())
				.build());

		// Over the pond while it lives: bubbles of light rising and a faint glowing steam.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_SHIMMER).lod(128, 800)
				.emit(EmitterDef.builder("bubbles").burst(8).shape(Shape.disc(13)).offset(0, 0.1F, 0).velocity(Velocity.up(0.04F, 0.1F, 0.1F)).life(20, 40)
						.size(0.1F, 0.18F).sprite(FxSprites.FLARE).color(PLASMA).alphaOverLife(TWINKLE).build())
				.emit(smoke("steam").burst(1).shape(Shape.disc(12)).velocity(Velocity.up(0.05F, 0.12F, 0.2F)).life(40, 70).size(2, 3).color(MIST).alpha(0.15F)
						.shade(1).emissive(Curve.constant(0.5F)).build())
				.build());

		// Where a koi swims near the surface, tinted its colour: a ripple and a few bubbles.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_RIPPLE).tint(0x60F0FF).lod(96, 500)
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(1.6F, 1.6F).life(18, 18).sprite(FxSprites.RING)
						.sizeOverLife(Curve.EASE_OUT).color(WHITE).tinted().alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("bubbles").burst(3).shape(Shape.disc(0.8F)).velocity(Velocity.up(0.03F, 0.08F, 0.2F)).life(10, 20).size(0.08F, 0.12F)
						.sprite(FxSprites.FLARE).color(WHITE).tinted().alphaOverLife(TWINKLE).build())
				.build());

		// Off a leaping koi, tinted its colour and carried a little with it: drips of plasma falling, glints.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_TRAIL).tint(0x60F0FF).lod(160, 1000)
				.emit(EmitterDef.builder("drips").burst(2).shape(Shape.sphere(0.4F)).inherit(0.3F).velocity(Velocity.random(0.02F, 0.06F)).gravity(0.04F)
						.collide(0.1F, 0.5F, 1).life(16, 28).size(0.12F, 0.2F).sprite(FxSprites.GLOW).color(WHITE).tinted().alphaOverLife(Curve.FADE_OUT).build())
				.emit(EmitterDef.builder("glints").burst(2).shape(Shape.sphere(0.6F)).inherit(0.1F).life(8, 16).size(0.12F, 0.2F).sprite(FxSprites.FLARE)
						.color(WHITE).tinted().alphaOverLife(TWINKLE).build())
				.emit(EmitterDef.builder("wake").burst(1).inherit(0.2F).life(10, 14).size(0.8F, 1.0F).sprite(FxSprites.GLOW).color(WHITE).tinted().alpha(0.3F)
						.alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// A koi breaks the surface: a fountain of plasma droplets, a crown on the water, a breath of mist.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_BREACH).lod(160, 1000)
				.emit(EmitterDef.builder("fountain").priority(Priority.HERO).burst(40).shape(Shape.disc(0.8F)).velocity(Velocity.up(0.5F, 1.1F, 0.45F))
						.gravity(0.05F).life(16, 30).size(0.15F, 0.25F).sprite(FxSprites.GLOW).color(PLASMA).build())
				.emit(EmitterDef.builder("crown").burst(24).shape(Shape.ring(1.2F, 0.2F)).velocity(Velocity.radial(0.15F, 0.3F)).velocity(Velocity.up(0.3F, 0.5F, 0.1F))
						.gravity(0.05F).life(12, 20).size(0.08F, 0.14F).mode(Mode.STRETCHED).sprite(FxSprites.STREAK).color(PLASMA).build())
				.emit(EmitterDef.builder("ring").burst(1).mode(Mode.FLAT).fixedRotation().size(3, 3).life(16, 16).sprite(FxSprites.RING).sizeOverLife(Curve.EASE_OUT)
						.color(0xC0FFFF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("mist").burst(4).velocity(Velocity.up(0.05F, 0.15F, 0.5F)).life(30, 50).size(1.5F, 2.5F).color(MIST).alpha(0.35F).shade(1).build())
				.build());

		// A koi comes down: a flash, plasma spraying, a glowing pool, scorched steam and embers.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_SPLASH).lod(200, 1400).maxAge(160)
				.cue(0, EffectDef.light(0.8F, 0x80F0FF))
				.emit(flash("flash", 6, 6, 0xC0F8FF).build())
				.emit(EmitterDef.builder("spray").priority(Priority.HERO).burst(60).velocity(Velocity.up(0.4F, 1.0F, 0.9F)).gravity(0.05F).collide(0.2F, 0.5F, 1)
						.life(20, 36).size(0.15F, 0.25F).sprite(FxSprites.GLOW).color(PLASMA).build())
				.emit(EmitterDef.builder("streaks").burst(30).velocity(Velocity.random(0.4F, 0.9F)).velocity(Velocity.up(0.1F, 0.3F, 0.8F)).drag(0.04F).life(10, 18)
						.size(0.1F, 0.16F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(KOI_COLORS).build())
				.emit(EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(3, 3.6F).life(40, 50).sprite(FxSprites.GLOW)
						.color(0x60E8FF).alpha(0.7F).alphaOverLife(Curve.FADE_OUT).build())
				.emit(smoke("steam").burst(8).velocity(Velocity.up(0.1F, 0.25F, 0.6F)).life(40, 70).size(2, 3.5F).color(MIST).alpha(0.45F).shade(0.9F).build())
				.emit(smoke("scorch").burst(5).onGround().shape(Shape.ring(1.5F, 1)).velocity(Velocity.radial(0.08F, 0.15F)).life(50, 80).size(1.5F, 2.5F)
						.color(SCORCH).alpha(0.5F).build())
				.build());

		// The dragon dives home and the pond erupts: a column of plasma, a crown falling back, mist rolling out, koi
		// colours streaking through it.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_GEYSER).lod(500, 3000).maxAge(360)
				.cue(0, EffectDef.light(1.8F, 0x80F0FF))
				.emit(flash("flash", 50, 14, 0xE0FFFF).build())
				.emit(EmitterDef.builder("column").priority(Priority.HERO).rate(40, 60, Curve.FALL).shape(Shape.disc(5)).velocity(Velocity.up(2.0F, 3.6F, 0.06F))
						.drag(0.01F).life(30, 50).size(0.8F, 1.3F).mode(Mode.STRETCHED).stretch(2).sprite(FxSprites.STREAK).color(PLASMA).build())
				.emit(EmitterDef.builder("crown").priority(Priority.HERO).rate(20, 50, Curve.FALL).shape(Shape.disc(4)).offset(0, 30, 0)
						.velocity(Velocity.radial(0.3F, 0.7F)).velocity(Velocity.up(0.4F, 1.0F, 0.9F)).gravity(0.04F).collide(0.2F, 0.5F, 1).life(60, 100)
						.size(0.3F, 0.5F).sprite(FxSprites.GLOW).color(PLASMA).build())
				.emit(EmitterDef.builder("koi").rate(8, 50).shape(Shape.disc(5)).velocity(Velocity.up(1.6F, 3.0F, 0.15F)).velocity(Velocity.swirl(0.4F, 0.8F))
						.drag(0.02F).life(30, 50).size(0.5F, 0.8F).mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(KOI_COLORS).build())
				.emit(smoke("mist").priority(Priority.HERO).rate(30, 30, Curve.FALL).onGround().shape(Shape.ring(10, 3)).expand(1.4F)
						.velocity(Velocity.radial(0.3F, 0.6F)).velocity(Velocity.up(0.1F, 0.3F, 0.3F)).life(80, 130).size(4, 7).color(MIST).alpha(0.3F).shade(0.95F)
						.emissive(Curve.keys(0, 0.8F, 1, 0.2F)).build())
				.emit(EmitterDef.builder("ring").burst(1).onGround().mode(Mode.FLAT).fixedRotation().size(40, 40).life(30, 30).sprite(FxSprites.SHOCK)
						.sizeOverLife(Curve.EASE_OUT).color(0xA0F0FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());

		// The plasma rain: a drop streaking down out of the sky and a little glowing splash where it lands.
		FxEngine.register(EffectDef.builder(VfxIds.KOI_RAIN).lod(200, 1200)
				.emit(EmitterDef.builder("drop").burst(1).offset(0, 30, 0).velocity(Velocity.up(-2.2F, -2.0F, 0)).life(14, 14).size(0.3F, 0.4F)
						.mode(Mode.STRETCHED).stretch(3).sprite(FxSprites.STREAK).color(PLASMA).build())
				.emit(14, EmitterDef.builder("splash").burst(14).velocity(Velocity.up(0.2F, 0.5F, 0.9F)).gravity(0.05F).life(12, 22).size(0.1F, 0.16F)
						.sprite(FxSprites.GLOW).color(PLASMA).build())
				.emit(14, EmitterDef.builder("pool").burst(1).onGround().offset(0, 0.1F, 0).mode(Mode.FLAT).fixedRotation().size(1.4F, 1.8F).life(30, 40)
						.sprite(FxSprites.GLOW).color(0x60E8FF).alpha(0.6F).alphaOverLife(Curve.FADE_OUT).build())
				.build());
	}
}
