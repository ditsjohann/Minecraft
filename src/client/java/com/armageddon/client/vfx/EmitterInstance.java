package com.armageddon.client.vfx;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** A running emitter: its own particles, spawn clock and random stream, inside one {@link EffectInstance}. */
public final class EmitterInstance {
	static final int UNLIT = LightmapTextureManager.pack(0, 15);

	final EmitterDef def;
	final EffectInstance fx;
	final ParticlePool pool;
	/** Emitter origin in effect space. */
	public final float ox, oy, oz;
	/** Effect scale: sizes and distances. */
	public final float scale;
	/** Multiplier on speeds and forces (equal to scale: a bigger blast throws further in the same time). */
	public final float scaleSpeed;
	public final Rng rng;
	private final float ivx, ivy, ivz;
	/** Larger particles when the count is thinned, so coverage holds. */
	private final float sizeScale;
	private int age;
	private float acc;
	private final float[] shape = new float[6];
	private final float[] vel = new float[3];
	private final BlockPos.Mutable cursor = new BlockPos.Mutable();

	EmitterInstance(EmitterDef def, EffectInstance fx, float ox, float oy, float oz, float ivx, float ivy, float ivz, long seed) {
		this.def = def;
		this.fx = fx;
		this.ox = ox;
		this.oy = oy;
		this.oz = oz;
		this.ivx = ivx;
		this.ivy = ivy;
		this.ivz = ivz;
		this.scale = fx.scale;
		this.scaleSpeed = fx.scale;
		this.rng = new Rng(seed);
		this.pool = new ParticlePool(Math.min(def.max, 64));
		this.sizeScale = Math.min(1.6F, 1.0F / (float) Math.sqrt(Math.max(0.3F, fx.lod)));
	}

	public int count() {
		return pool.count;
	}

	boolean done() {
		return age > def.spawnEnd() && pool.count == 0;
	}

	/** Distance thins everything but the hero layers (a lone glob or spike must never vanish because it is far). */
	private float countScale() {
		float lod = def.priority == EmitterDef.Priority.HERO ? 1.0F : fx.lod;
		return lod * FxEngine.budget(def.priority);
	}

	/** Stochastic rounding, so 0.4 of a particle per tick still spawns about every 2.5 ticks. */
	private int round(float n) {
		int whole = (int) n;
		return whole + (rng.next() < n - whole ? 1 : 0);
	}

	void tick(ClientWorld world) {
		EmitterDef d = def;
		for (EmitterDef.Burst b : d.bursts) {
			if (b.at() == age) {
				int n = round(b.count() * countScale());
				for (int k = 0; k < n; k++) {
					spawn(world);
				}
			}
		}
		if (d.rate > 0 && age < d.rateDuration) {
			acc += d.rate * d.rateCurve.at(age / (float) d.rateDuration) * countScale();
			int n = (int) acc;
			acc -= n;
			for (int k = 0; k < n; k++) {
				spawn(world);
			}
		}
		ParticlePool p = pool;
		for (Module m : d.modules) {
			m.update(this, p);
		}
		for (int i = 0; i < p.count; i++) {
			p.px[i] = p.x[i];
			p.py[i] = p.y[i];
			p.pz[i] = p.z[i];
			p.x[i] += p.vx[i];
			p.y[i] += p.vy[i];
			p.z[i] += p.vz[i];
			p.prot[i] = p.rot[i];
			p.rot[i] += p.spin[i];
			p.age[i] += 1;
		}
		if (d.collide && world != null) {
			collide(world);
		}
		if (d.every != null) {
			int every = d.every.every();
			for (int i = 0; i < p.count; i++) {
				if (++p.timer[i] >= every) {
					p.timer[i] = 0;
					fx.children(d.every.defs(), p.x[i], p.y[i], p.z[i], p.vx[i], p.vy[i], p.vz[i]);
				}
			}
		}
		if (d.layer == EmitterDef.Layer.SMOKE) {
			int phase = age & 3;
			for (int i = phase; i < p.count; i += 4) {
				p.light[i] = sampleLight(world, i);
			}
		}
		for (int i = 0; i < p.count; ) {
			if (p.age[i] >= p.life[i]) {
				if (d.onDeath != null && rng.next() < d.onDeath.chance()) {
					fx.children(d.onDeath.defs(), p.x[i], p.y[i], p.z[i], p.vx[i], p.vy[i], p.vz[i]);
				}
				p.kill(i);
			} else {
				i++;
			}
		}
		age++;
	}

	private void spawn(ClientWorld world) {
		EmitterDef d = def;
		ParticlePool p = pool;
		int i = p.add(d.max);
		if (i < 0) {
			return;
		}
		d.shape.sample(rng, scale, shape);
		float x = ox + shape[0], y = oy + shape[1], z = oz + shape[2];
		if (d.expand != 0) {
			float h = (float) Math.sqrt(shape[3] * shape[3] + shape[5] * shape[5]);
			if (h > 1.0E-4F) {
				float push = d.expand * scale * age / h;
				x += shape[3] * push;
				z += shape[5] * push;
			}
		}
		if (d.groundSnap && world != null) {
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(fx.ox + x), (int) Math.floor(fx.oz + z));
			float rel = (float) (top - fx.oy);
			if (Math.abs(rel - y) < 24 * scale + 8) {
				y = rel + 0.15F;
			}
		}
		vel[0] = ivx * d.inherit;
		vel[1] = ivy * d.inherit;
		vel[2] = ivz * d.inherit;
		for (Velocity v : d.velocity) {
			v.add(rng, shape, fx.dir, scaleSpeed, vel);
		}
		p.x[i] = p.px[i] = x;
		p.y[i] = p.py[i] = y;
		p.z[i] = p.pz[i] = z;
		p.vx[i] = vel[0];
		p.vy[i] = vel[1];
		p.vz[i] = vel[2];
		p.age[i] = 0;
		p.life[i] = Math.max(1, rng.range(d.lifeMin, d.lifeMax));
		p.size[i] = rng.range(d.sizeMin, d.sizeMax) * scale * sizeScale;
		p.rot[i] = p.prot[i] = d.randomRotation ? rng.next() * (float) (Math.PI * 2) : 0;
		p.spin[i] = rng.range(d.spinMin, d.spinMax) * (rng.next() < 0.5F ? -1 : 1);
		p.seed[i] = rng.next();
		p.light[i] = d.layer == EmitterDef.Layer.SMOKE ? sampleLight(world, i) : UNLIT;
	}

	private int sampleLight(ClientWorld world, int i) {
		if (world == null) {
			return UNLIT;
		}
		ParticlePool p = pool;
		cursor.set(fx.ox + p.x[i], fx.oy + p.y[i], fx.oz + p.z[i]);
		if (!world.isChunkLoaded(cursor.getX() >> 4, cursor.getZ() >> 4)) {
			return UNLIT;
		}
		return WorldRenderer.getLightmapCoordinates(world, cursor);
	}

	private boolean solid(ClientWorld world, double x, double y, double z) {
		cursor.set(x, y, z);
		BlockState s = world.getBlockState(cursor);
		return !s.isAir() && s.isSolidBlock(world, cursor);
	}

	/** Bounce off blocks: a vertical hit when the block is entered from above or below, else a wall hit. */
	private void collide(ClientWorld world) {
		ParticlePool p = pool;
		EmitterDef d = def;
		double bx = fx.ox, by = fx.oy, bz = fx.oz;
		for (int i = 0; i < p.count; i++) {
			if (!solid(world, bx + p.x[i], by + p.y[i], bz + p.z[i])) {
				continue;
			}
			float speed2 = p.vx[i] * p.vx[i] + p.vy[i] * p.vy[i] + p.vz[i] * p.vz[i];
			if (!solid(world, bx + p.x[i], by + p.py[i], bz + p.z[i])) {
				p.y[i] = p.py[i];
				p.vy[i] = -p.vy[i] * d.bounce;
				p.vx[i] *= d.friction;
				p.vz[i] *= d.friction;
			} else {
				p.x[i] = p.px[i];
				p.z[i] = p.pz[i];
				p.vx[i] = -p.vx[i] * d.bounce;
				p.vz[i] = -p.vz[i] * d.bounce;
				if (solid(world, bx + p.x[i], by + p.y[i], bz + p.z[i])) {
					p.y[i] = p.py[i];
					p.vy[i] = 0;
				}
			}
			// Resting contact is not a hit; only real impacts count toward the particle's death.
			if (speed2 > 0.0025F) {
				if (d.onHit != null && rng.next() < d.onHit.chance()) {
					fx.children(d.onHit.defs(), p.x[i], p.y[i], p.z[i], 0, 0, 0);
				}
				if (++p.hits[i] > d.maxHits) {
					p.age[i] = p.life[i];
				}
			}
		}
	}
}
