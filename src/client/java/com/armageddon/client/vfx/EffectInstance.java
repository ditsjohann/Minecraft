package com.armageddon.client.vfx;

import com.armageddon.Armageddon;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** One playing effect: its origin, parameters, emitters and pending cues. */
public final class EffectInstance {
	/** Live emitters per effect, so a shower of sub-emitters cannot run away. */
	private static final int MAX_EMITTERS = 384;

	final EffectDef def;
	public final double ox, oy, oz;
	/** Unit direction (aim, surface normal); straight up by default. */
	public final float[] dir;
	public final float scale;
	/** Tint for {@link EmitterDef.Builder#tinted()} emitters, 0 for none. */
	public final int tint;
	public final long seed;
	/** Detail kept at this distance: 1 up close, down to 0.3 far away. */
	final float lod;
	final Rng rng;
	final Rng cueRng;
	public int age;
	final List<EmitterInstance> emitters = new ArrayList<>();
	private final List<EmitterInstance> pending = new ArrayList<>();
	private final int[] cueAt;
	private int nextEntry;
	private float[] curl;

	EffectInstance(EffectDef def, double x, double y, double z, float dx, float dy, float dz, float scale, int tint, long seed, double camDist) {
		this.def = def;
		this.ox = x;
		this.oy = y;
		this.oz = z;
		float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		this.dir = l < 1.0E-4F ? new float[]{0, 1, 0} : new float[]{dx / l, dy / l, dz / l};
		this.scale = Math.max(0.05F, scale);
		this.tint = tint != 0 ? tint : def.tint;
		this.seed = seed;
		this.rng = new Rng(seed);
		this.cueRng = new Rng(seed ^ 0x5DEECE66DL);
		float k = (float) Math.sqrt(this.scale);
		float near = def.lodNear * k, far = def.lodFar * k;
		this.lod = (float) MathHelper.clamp(1.0 - (camDist - near) / Math.max(1, far - near), 0.3, 1.0);
		this.cueAt = new int[def.cues.size()];
		for (int i = 0; i < cueAt.length; i++) {
			EffectDef.Cue c = def.cues.get(i);
			cueAt[i] = c.at() + (c.sonic() ? (int) (camDist / EffectDef.SOUND_SPEED) : 0);
		}
	}

	public Vec3d origin() {
		return new Vec3d(ox, oy, oz);
	}

	public int particles() {
		int n = 0;
		for (EmitterInstance e : emitters) {
			n += e.count();
		}
		return n;
	}

	/** Three travelling waves for {@link Module#curl}: unit k̂ (3), unit amplitude (3), phase (1) each. */
	float[] curlWaves() {
		if (curl == null) {
			Rng r = new Rng(seed * 0x9E3779B97F4A7C15L + 1);
			curl = new float[21];
			for (int n = 0; n < 21; n += 7) {
				r.unit(curl, n);
				r.unit(curl, n + 3);
				curl[n + 6] = r.next() * (float) (Math.PI * 2);
			}
		}
		return curl;
	}

	/** Starts sub-emitters at a point in effect space (from a particle's death, hit or trail). */
	void children(List<EmitterDef> defs, float x, float y, float z, float vx, float vy, float vz) {
		for (EmitterDef def : defs) {
			if (emitters.size() + pending.size() >= MAX_EMITTERS) {
				return;
			}
			pending.add(new EmitterInstance(def, this, x, y, z, vx, vy, vz, rng.nextLong()));
		}
	}

	/** @return false once everything has played out. */
	boolean tick(ClientWorld world) {
		if (world != null) {
			for (int i = 0; i < cueAt.length; i++) {
				if (cueAt[i] == age) {
					try {
						def.cues.get(i).action().run(this);
					} catch (RuntimeException ex) {
						Armageddon.LOGGER.warn("Effect {} cue failed", def.name, ex);
					}
				}
			}
		}
		while (nextEntry < def.entries.size() && def.entries.get(nextEntry).at() <= age) {
			EmitterDef e = def.entries.get(nextEntry++).emitter();
			emitters.add(new EmitterInstance(e, this, e.offsetX * scale, e.offsetY * scale, e.offsetZ * scale, 0, 0, 0, rng.nextLong()));
		}
		for (EmitterInstance e : emitters) {
			e.tick(world);
		}
		emitters.removeIf(EmitterInstance::done);
		if (!pending.isEmpty()) {
			emitters.addAll(pending);
			pending.clear();
		}
		age++;
		if (age >= def.maxAge) {
			return false;
		}
		if (!emitters.isEmpty() || nextEntry < def.entries.size()) {
			return true;
		}
		for (int at : cueAt) {
			if (at >= age) {
				return true;
			}
		}
		return false;
	}
}
