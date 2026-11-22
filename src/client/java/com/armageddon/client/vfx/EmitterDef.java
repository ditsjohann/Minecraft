package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;

import java.util.ArrayList;
import java.util.List;

/**
 * One layer of an effect: how many particles, where, how they start, which forces act on them, how they change over
 * their lives, what they set off, and how they are drawn. Immutable once built; share freely.
 *
 * <p>Times are ticks, lengths blocks and speeds blocks per tick, all at scale 1.
 */
public final class EmitterDef {
	/** How hard the budget may cut this emitter when the screen is busy. */
	public enum Priority { HERO, NORMAL, DETAIL }

	/** How each particle's quad is oriented. */
	public enum Mode {
		/** Faces the camera. */
		BILLBOARD,
		/** Faces the camera but stays upright: flames, pillars. */
		UPRIGHT,
		/** Stretched along its motion: sparks, streaks, rain. */
		STRETCHED,
		/** Lies flat on the ground: rings, ground dust. */
		FLAT
	}

	/** Which buffer it draws into. */
	public enum Layer {
		/** Additive and full-bright: fire, sparks, plasma. Never darkens, needs no sorting. */
		GLOW,
		/** Additive and drawn through everything: cores that must read through their own smoke. */
		GLOW_TOP,
		/** Alpha-blended and lit by the world: smoke, dust, gas. Sorted back to front. */
		SMOKE
	}

	public record Burst(int at, float count) {
	}

	public record Sub(List<EmitterDef> defs, float chance, int every) {
	}

	final String name;
	final Priority priority;
	final int max;
	final List<Burst> bursts;
	final float rate;
	final Curve rateCurve;
	final int rateDuration;
	final Shape shape;
	final float offsetX, offsetY, offsetZ;
	final List<Velocity> velocity;
	final float inherit;
	final int lifeMin, lifeMax;
	final float sizeMin, sizeMax;
	final boolean randomRotation;
	final float spinMin, spinMax;
	final List<Module> modules;
	final boolean collide;
	final float bounce, friction;
	final int maxHits;
	final Sub onDeath, onHit, every;
	final Mode mode;
	final Layer layer;
	final int cell, cells;
	final float stretch;
	final Curve sizeCurve, alphaCurve, emissive;
	final Gradient color;
	final float alpha;
	final float shade;
	final boolean tint;
	final boolean groundSnap;
	final float expand;

	private EmitterDef(Builder b) {
		name = b.name;
		priority = b.priority;
		bursts = List.copyOf(b.bursts);
		rate = b.rate;
		rateCurve = b.rateCurve;
		rateDuration = b.rateDuration;
		shape = b.shape;
		offsetX = b.offsetX;
		offsetY = b.offsetY;
		offsetZ = b.offsetZ;
		velocity = List.copyOf(b.velocity);
		inherit = b.inherit;
		lifeMin = b.lifeMin;
		lifeMax = Math.max(b.lifeMin, b.lifeMax);
		sizeMin = b.sizeMin;
		sizeMax = b.sizeMax;
		randomRotation = b.randomRotation;
		spinMin = b.spinMin;
		spinMax = b.spinMax;
		modules = List.copyOf(b.modules);
		collide = b.collide;
		bounce = b.bounce;
		friction = b.friction;
		maxHits = b.maxHits;
		onDeath = b.onDeath;
		onHit = b.onHit;
		every = b.every;
		mode = b.mode;
		layer = b.layer;
		cell = b.cell;
		cells = Math.max(1, b.cells);
		stretch = b.stretch;
		sizeCurve = b.sizeCurve;
		alphaCurve = b.alphaCurve;
		emissive = b.emissive;
		color = b.color;
		alpha = b.alpha;
		shade = b.shade;
		tint = b.tint;
		groundSnap = b.groundSnap;
		expand = b.expand;
		float burst = 0;
		for (Burst x : bursts) {
			burst += x.count;
		}
		// Enough room for everything that can be alive at once, with headroom for scale.
		int est = (int) (burst + rate * Math.min(rateDuration, lifeMax) + 8);
		max = b.max > 0 ? b.max : Math.min(20000, est * 4);
	}

	/** Last tick (emitter time) at which this emitter can still spawn. */
	int spawnEnd() {
		int end = rate > 0 ? rateDuration : 0;
		for (Burst x : bursts) {
			end = Math.max(end, x.at);
		}
		return end;
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public static final class Builder {
		private final String name;
		private Priority priority = Priority.NORMAL;
		private int max;
		private final List<Burst> bursts = new ArrayList<>();
		private float rate;
		private Curve rateCurve = Curve.ONE;
		private int rateDuration;
		private Shape shape = Shape.POINT;
		private float offsetX, offsetY, offsetZ;
		private final List<Velocity> velocity = new ArrayList<>();
		private float inherit = 0.0F;
		private int lifeMin = 20, lifeMax = 30;
		private float sizeMin = 0.2F, sizeMax = 0.3F;
		private boolean randomRotation = true;
		private float spinMin, spinMax;
		private final List<Module> modules = new ArrayList<>();
		private boolean collide;
		private float bounce = 0.3F, friction = 0.7F;
		private int maxHits = 3;
		private Sub onDeath, onHit, every;
		private Mode mode = Mode.BILLBOARD;
		private Layer layer = Layer.GLOW;
		private int cell = FxSprites.GLOW, cells = 1;
		private float stretch = 1.5F;
		private Curve sizeCurve = Curve.ONE, alphaCurve = Curve.FADE, emissive = Curve.ONE;
		private Gradient color = Gradient.solid(0xFFFFFF);
		private float alpha = 1.0F;
		private float shade = 1.0F;
		private boolean tint;
		private boolean groundSnap;
		private float expand;

		private Builder(String name) {
			this.name = name;
		}

		public Builder priority(Priority p) {
			priority = p;
			return this;
		}

		/** Hard cap on live particles (by default sized from the spawn settings). */
		public Builder max(int n) {
			max = n;
			return this;
		}

		/** {@code count} particles at once, {@code at} ticks after the emitter starts. */
		public Builder burst(int at, float count) {
			bursts.add(new Burst(at, count));
			return this;
		}

		public Builder burst(float count) {
			return burst(0, count);
		}

		/** A steady stream of {@code perTick} particles for {@code duration} ticks, shaped by {@code curve}. */
		public Builder rate(float perTick, int duration, Curve curve) {
			rate = perTick;
			rateDuration = duration;
			rateCurve = curve;
			return this;
		}

		public Builder rate(float perTick, int duration) {
			return rate(perTick, duration, Curve.ONE);
		}

		public Builder shape(Shape s) {
			shape = s;
			return this;
		}

		/** Shifts the emitter from the effect's origin (blocks at scale 1). */
		public Builder offset(float x, float y, float z) {
			offsetX = x;
			offsetY = y;
			offsetZ = z;
			return this;
		}

		public Builder velocity(Velocity v) {
			velocity.add(v);
			return this;
		}

		/** For sub-emitters: carry this fraction of the parent particle's velocity. */
		public Builder inherit(float fraction) {
			inherit = fraction;
			return this;
		}

		public Builder life(int min, int max) {
			lifeMin = min;
			lifeMax = max;
			return this;
		}

		/** Half-width of the quad, blocks at scale 1. */
		public Builder size(float min, float max) {
			sizeMin = min;
			sizeMax = max;
			return this;
		}

		/** Spin in radians per tick, picked per particle (either way round). */
		public Builder spin(float min, float max) {
			spinMin = min;
			spinMax = max;
			return this;
		}

		public Builder fixedRotation() {
			randomRotation = false;
			return this;
		}

		public Builder module(Module m) {
			modules.add(m);
			return this;
		}

		public Builder gravity(float g) {
			return module(Module.gravity(g));
		}

		public Builder drag(float k) {
			return module(Module.drag(k));
		}

		/** Bounce off blocks; after {@code maxHits} hits the particle dies. */
		public Builder collide(float bounce, float friction, int maxHits) {
			collide = true;
			this.bounce = bounce;
			this.friction = friction;
			this.maxHits = maxHits;
			return this;
		}

		/** When a particle dies, start {@code defs} where it was (with probability {@code chance}). */
		public Builder onDeath(float chance, EmitterDef... defs) {
			onDeath = new Sub(List.of(defs), chance, 0);
			return this;
		}

		/** When a particle hits a block, start {@code defs} there. */
		public Builder onHit(float chance, EmitterDef... defs) {
			onHit = new Sub(List.of(defs), chance, 0);
			return this;
		}

		/** Every {@code ticks} ticks of a particle's life, start {@code defs} at it: trails. */
		public Builder every(int ticks, EmitterDef... defs) {
			every = new Sub(List.of(defs), 1.0F, Math.max(1, ticks));
			return this;
		}

		public Builder mode(Mode m) {
			mode = m;
			return this;
		}

		public Builder layer(Layer l) {
			layer = l;
			return this;
		}

		/** Atlas cell ({@link FxSprites}), and how many consecutive cells to pick from at random. */
		public Builder sprite(int cell, int variants) {
			this.cell = cell;
			cells = variants;
			return this;
		}

		public Builder sprite(int cell) {
			return sprite(cell, 1);
		}

		/** For {@link Mode#STRETCHED}: how many ticks of motion the streak covers. */
		public Builder stretch(float ticks) {
			stretch = ticks;
			return this;
		}

		public Builder sizeOverLife(Curve c) {
			sizeCurve = c;
			return this;
		}

		public Builder alphaOverLife(Curve c) {
			alphaCurve = c;
			return this;
		}

		/**
		 * For {@link Layer#SMOKE}: how lit-from-inside the particle is over its life (1 glows like fire, 0 takes the
		 * world's light). Glow layers are always full-bright.
		 */
		public Builder emissive(Curve c) {
			emissive = c;
			return this;
		}

		public Builder color(Gradient g) {
			color = g;
			return this;
		}

		public Builder color(int rgb) {
			return color(Gradient.solid(rgb));
		}

		public Builder alpha(float a) {
			alpha = a;
			return this;
		}

		/** Bottom edge brightness relative to the top (smoke lit from above): 1 flat, 0.6 strongly shaded. */
		public Builder shade(float bottom) {
			shade = bottom;
			return this;
		}

		/** Multiply this emitter's colours by the effect's tint colour, when one is given. */
		public Builder tinted() {
			tint = true;
			return this;
		}

		/** Spawn on the ground below the emitter instead of at its height (dust skirts, ground rings). */
		public Builder onGround() {
			groundSnap = true;
			return this;
		}

		/**
		 * The spawn shape grows outward by {@code blocksPerTick} (at scale 1) as the emitter runs: a ring becomes a front
		 * that travels, for flame walls and dust walls riding a shockwave.
		 */
		public Builder expand(float blocksPerTick) {
			expand = blocksPerTick;
			return this;
		}

		public EmitterDef build() {
			return new EmitterDef(this);
		}
	}
}
