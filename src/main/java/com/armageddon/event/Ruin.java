package com.armageddon.event;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.WorldJob;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ways to wreck a place that are not explosions. Each is a {@link WorldJob} that plays out over a few seconds, so it
 * reads as a process: matter disintegrating into light, collapsing into a point, turning to crystal and shattering,
 * or melting down into a pool of lava. All of them honour the block-damage rule and protected regions.
 */
public final class Ruin {
	private Ruin() {
	}

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	/** Every block position in a sphere, ordered by {@code distance + noise * jitter} (so fronts are ragged, not perfect). */
	private static long[] sphere(Vec3d c, double r, double jitter, boolean outsideIn) {
		int ri = MathHelper.ceil(r);
		int cx = MathHelper.floor(c.x), cy = MathHelper.floor(c.y), cz = MathHelper.floor(c.z);
		List<double[]> list = new ArrayList<>();
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dy = -ri; dy <= ri; dy++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d > r) {
						continue;
					}
					double n = hash(cx + dx, cy + dy, cz + dz);
					list.add(new double[]{outsideIn ? r - d + n * jitter : d + n * jitter, BlockPos.asLong(cx + dx, cy + dy, cz + dz)});
				}
			}
		}
		list.sort((a, b) -> Double.compare(a[0], b[0]));
		long[] out = new long[list.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = (long) list.get(i)[1];
		}
		return out;
	}

	static double hash(int x, int y, int z) {
		long h = x * 734287L ^ y * 1913L ^ z * 912931L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	private static boolean breakable(ServerWorld w, BlockPos p, BlockState s, ProtectedRegions regions) {
		return !s.isAir() && s.getHardness(w, p) >= 0 && !s.hasBlockEntity() && (regions.isEmpty() || !regions.isProtected(p));
	}

	// ------------------------------------------------------------------ disintegrate

	/**
	 * Sci-fi disintegration: a front grows out from the centre and every block it reaches lights up as pure energy
	 * for a moment, then is gone in a puff of sparks. Leaves a clean, smooth-walled spherical cavity.
	 */
	public static final class Disintegrate implements WorldJob {
		private final ServerWorld w;
		private final long[] order;
		private final BlockState glow;
		private final ParticleEffect spark;
		private final int perTick;
		private final ArrayDeque<long[]> lit = new ArrayDeque<>();
		private final ProtectedRegions regions;
		private int cursor;
		private long tick;

		public Disintegrate(ServerWorld w, Vec3d c, double r, BlockState glow, ParticleEffect spark, int ticks) {
			this.w = w;
			this.order = sphere(c, r, 2.5, false);
			this.glow = glow;
			this.spark = spark;
			this.perTick = Math.max(40, order.length / Math.max(1, ticks));
			this.regions = ProtectedRegions.get(w);
			Kit.sound(w, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 24.0F + (float) r, 1.6F);
		}

		@Override
		public ServerWorld world() {
			return w;
		}

		@Override
		public int step(int budget) {
			tick++;
			BlockPos.Mutable p = new BlockPos.Mutable();
			int sparks = 0;
			while (!lit.isEmpty() && lit.peekFirst()[1] <= tick) {
				p.set(lit.pollFirst()[0]);
				if (w.getBlockState(p) == glow) {
					Destruction.set(w, p.toImmutable(), Blocks.AIR.getDefaultState(), FLAGS);
					if (sparks++ < 30 && (p.getX() + p.getY() + p.getZ()) % 5 == 0) {
						w.spawnParticles(spark, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.04);
					}
				}
			}
			int n = 0;
			while (cursor < order.length && n < perTick && budget > 0) {
				p.set(order[cursor++]);
				BlockState s = w.getBlockState(p);
				if (!w.isChunkLoaded(p) || !breakable(w, p, s, regions)) {
					continue;
				}
				Destruction.record(w, p.toImmutable(), s);
				if (Destruction.set(w, p.toImmutable(), glow, FLAGS)) {
					lit.addLast(new long[]{p.asLong(), tick + 3 + (n & 3)});
				}
				n++;
				budget--;
			}
			if (tick % 10 == 0 && !isDone()) {
				Kit.sound(w, Vec3d.ofCenter(p), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 16.0F, 1.8F);
			}
			return budget;
		}

		@Override
		public boolean isDone() {
			return cursor >= order.length && lit.isEmpty();
		}
	}

	/** Disintegrates a sphere over {@code ticks} (no-op without block damage). */
	public static void disintegrate(ServerWorld w, Vec3d c, double r, BlockState glow, ParticleEffect spark, int ticks) {
		if (Destruction.enabled(w)) {
			DetonationManager.add(new Disintegrate(w, c, r, glow, spark, ticks));
		}
	}

	// ------------------------------------------------------------------ implode

	/**
	 * Implosion: the ground around a point tears loose and is drawn in from every side, faster and faster, crushed
	 * together into one small ball of dense, crying obsidian sitting in a perfectly round hole.
	 */
	public static final class Implode implements WorldJob {
		private final ServerWorld w;
		private final Vec3d c;
		private final double r;
		private final int duration;
		private final List<FallingBlockEntity> blocks = new ArrayList<>();
		private int age;
		private boolean done;

		public Implode(ServerWorld w, Vec3d c, double r, int duration) {
			this.w = w;
			this.c = c;
			this.r = r;
			this.duration = duration;
			Kit.sound(w, c, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 32.0F + (float) r, 0.5F);
		}

		@Override
		public ServerWorld world() {
			return w;
		}

		@Override
		public int step(int budget) {
			age++;
			if (age < duration * 0.7 && age % 2 == 0) {
				for (FallingBlockEntity fb : Kit.lift(w, c, r, 14, false, true)) {
					fb.setNoGravity(true);
					blocks.add(fb);
				}
			}
			double k = Math.min(1.0, age / (double) duration);
			blocks.removeIf(fb -> {
				if (!fb.isAlive()) {
					return true;
				}
				Vec3d to = c.subtract(fb.getPos());
				if (to.length() < 1.6) {
					fb.discard();
					return true;
				}
				Kit.velocity(fb, to.normalize().multiply(0.25 + 1.4 * k * k));
				return false;
			});
			if (age % 3 == 0) {
				for (LivingEntity e : Kit.living(w, c, r + 6)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.push(e, c.subtract(e.getPos()).normalize().multiply(0.15 + 0.3 * k));
					}
				}
				w.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 20, r * 0.4, r * 0.4, r * 0.4, 0.2);
			}
			if (age == duration) {
				Kit.vanish(blocks);
				Kit.sound(w, c, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 48.0F, 0.4F);
				Kit.sound(w, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 32.0F, 1.4F);
				com.armageddon.network.Vfx.ring(w, c, (float) r * 2.2F, 2.0F, 0x7A30C0, true, 30);
				com.armageddon.network.Vfx.flash(w, c, 0.3F, 200, 0xB070FF);
				Kit.damage(w, c, r, 30.0F, com.armageddon.registry.ModDamageTypes.ANNIHILATION, null, null);
				if (Destruction.enabled(w)) {
					DetonationManager.add(new com.armageddon.detonation.CraterJob(w, BlockPos.ofFloored(c), (float) (r * 0.75),
							com.armageddon.detonation.CraterJob.Mode.ANTIMATTER));
					DetonationManager.schedule(w, 4, () -> Kit.sphere(w, BlockPos.ofFloored(c), Math.max(1.5, r * 0.18), Blocks.CRYING_OBSIDIAN.getDefaultState()));
				}
				done = true;
			}
			return budget;
		}

		@Override
		public boolean isDone() {
			return done;
		}
	}

	public static void implode(ServerWorld w, Vec3d c, double r, int duration) {
		DetonationManager.add(new Implode(w, c, r, duration));
	}

	// ------------------------------------------------------------------ crystallize

	/**
	 * Crystallization: a sphere of the world turns to crystal from the middle out and throws long crystal spikes,
	 * rings for a while, and then shatters into nothing with a sound like a cathedral window going.
	 */
	public static final class Crystallize implements WorldJob {
		private final ServerWorld w;
		private final Vec3d c;
		private final long[] order;
		private final BlockState[] crystal;
		private final int perTick, hold;
		private final ProtectedRegions regions;
		private final List<Long> grown = new ArrayList<>();
		private int cursor, shatter, age;

		public Crystallize(ServerWorld w, Vec3d c, double r, BlockState[] crystal, int ticks, int hold) {
			this.w = w;
			this.c = c;
			this.crystal = crystal;
			this.hold = hold;
			this.regions = ProtectedRegions.get(w);
			long[] body = sphere(c, r, 1.5, false);
			// Spikes: straight crystal needles out of the body, mostly upward.
			List<Long> spikes = new ArrayList<>();
			int n = 7 + (int) (r * 0.4);
			for (int i = 0; i < n; i++) {
				double a = hash(i, 3, (int) c.x) * MathHelper.TAU, up = 0.2 + hash(i, 5, (int) c.z) * 0.8;
				Vec3d dir = new Vec3d(Math.cos(a) * (1 - up), up, Math.sin(a) * (1 - up)).normalize();
				double len = r * (1.3 + hash(i, 7, 1) * 0.9);
				for (double s = r * 0.6; s < len; s += 0.7) {
					double thick = Math.max(0.0, 1.2 * (1 - s / len));
					Vec3d q = c.add(dir.multiply(s));
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx * dx + dz * dz <= thick * thick + 0.01) {
								spikes.add(BlockPos.asLong(MathHelper.floor(q.x) + dx, MathHelper.floor(q.y), MathHelper.floor(q.z) + dz));
							}
						}
					}
				}
			}
			this.order = Arrays.copyOf(body, body.length + spikes.size());
			for (int i = 0; i < spikes.size(); i++) {
				order[body.length + i] = spikes.get(i);
			}
			this.perTick = Math.max(30, order.length / Math.max(1, ticks));
			Kit.sound(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 32.0F + (float) r, 0.6F);
		}

		@Override
		public ServerWorld world() {
			return w;
		}

		@Override
		public int step(int budget) {
			age++;
			BlockPos.Mutable p = new BlockPos.Mutable();
			if (cursor < order.length) {
				int n = 0;
				int bodyEnd = order.length;
				while (cursor < order.length && n < perTick && budget > 0) {
					p.set(order[cursor++]);
					if (!w.isChunkLoaded(p)) {
						continue;
					}
					BlockState s = w.getBlockState(p);
					boolean spike = s.isAir();
					if (!spike && !breakable(w, p, s, regions) || spike && regions.isProtected(p)) {
						continue;
					}
					if (!spike) {
						Destruction.record(w, p.toImmutable(), s);
					}
					BlockState to = crystal[(int) (hash(p.getX(), p.getY(), p.getZ()) * crystal.length) % crystal.length];
					if (Destruction.set(w, p.toImmutable(), to, FLAGS)) {
						grown.add(p.asLong());
					}
					n++;
					budget--;
				}
				if (age % 6 == 0) {
					Kit.sound(w, c, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, 24.0F, 0.6F + (float) cursor / bodyEnd);
				}
				if (cursor >= order.length) {
					shatter = age + hold;
				}
				return budget;
			}
			if (age == shatter) {
				Kit.sound(w, c, SoundEvents.BLOCK_GLASS_BREAK, 48.0F, 0.5F);
				Kit.sound(w, c, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 48.0F, 0.4F);
				com.armageddon.network.Vfx.ring(w, c, 30.0F, 2.0F, 0xE0D0FF, false, 24);
			}
			if (age >= shatter && !grown.isEmpty()) {
				int n = Math.min(grown.size(), perTick * 3);
				for (int i = 0; i < n; i++) {
					p.set(grown.remove(grown.size() - 1));
					BlockState s = w.getBlockState(p);
					if (Arrays.asList(crystal).contains(s)) {
						Destruction.set(w, p.toImmutable(), Blocks.AIR.getDefaultState(), FLAGS);
						if (i % 9 == 0) {
							w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.2);
						}
					}
				}
				if (age % 4 == 0) {
					Kit.sound(w, c, SoundEvents.BLOCK_GLASS_BREAK, 24.0F, 0.6F + w.random.nextFloat() * 0.6F);
				}
			}
			return budget;
		}

		@Override
		public boolean isDone() {
			return cursor >= order.length && age > shatter && grown.isEmpty();
		}
	}

	public static void crystallize(ServerWorld w, Vec3d c, double r, BlockState[] crystal, int ticks, int hold) {
		if (Destruction.enabled(w)) {
			DetonationManager.add(new Crystallize(w, c, r, crystal, ticks, hold));
		}
	}

	public static final BlockState[] AMETHYST = {Blocks.AMETHYST_BLOCK.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState(),
			Blocks.MAGENTA_STAINED_GLASS.getDefaultState()};

	// ------------------------------------------------------------------ melt

	/** A {@link TerrainJob} whose front advances by itself at {@code speed} blocks a tick. */
	public static final class Front implements WorldJob {
		private final ServerWorld w;
		private final TerrainJob job;
		private final double speed;
		private int age;

		public Front(ServerWorld w, TerrainJob job, double speed) {
			this.w = w;
			this.job = job;
			this.speed = speed;
		}

		@Override
		public ServerWorld world() {
			return w;
		}

		@Override
		public int step(int budget) {
			age++;
			job.limit = age * speed;
			return job.step(budget);
		}

		@Override
		public boolean isDone() {
			return job.isDone();
		}
	}

	/**
	 * Meltdown: heat rather than blast. Everything standing slumps and is gone, and the ground sinks into a glowing
	 * pool of lava ringed with magma, cooling to blackstone and basalt toward the edge.
	 */
	public static TerrainJob meltJob(ServerWorld w, Vec3d c, double r) {
		BlockState air = Blocks.AIR.getDefaultState();
		return TerrainJob.disc(w, c, r, col -> {
			double d = Math.hypot(col.x + 0.5 - c.x, col.z + 0.5 - c.z);
			double s = 1 - d / r;
			int surf = col.surface(), top = Math.min(col.top(), surf + 40);
			for (int y = top; y > surf; y--) {
				if (!col.get(y).isAir() && col.noise(y * 7 + 1) < s * 1.8) {
					col.set(y, air);
				}
			}
			double n = col.noise(5);
			if (s > 0.6) {
				col.set(surf, air);
				col.set(surf - 1, Blocks.LAVA.getDefaultState());
				col.set(surf - 2, Blocks.MAGMA_BLOCK.getDefaultState());
			} else if (s > 0.32) {
				col.set(surf, n < 0.55 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.BLACKSTONE.getDefaultState());
			} else if (s > 0.06) {
				col.set(surf, n < 0.35 ? Blocks.BASALT.getDefaultState() : n < 0.6 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
				if (col.noise(9) < 0.05 && col.get(surf + 1).isAir()) {
					col.set(surf + 1, Blocks.FIRE.getDefaultState());
				}
			}
		});
	}

	/**
	 * A cone of force out of {@code from} along {@code dir}: the ground in it is torn up and thrown forward, deepest
	 * at the mouth and shallower toward the end, everything standing in it ripped away. Runs out at {@code speed}.
	 */
	public static void cone(ServerWorld w, Vec3d from, Vec3d dir, double length, double halfAngleDeg, int depth, double speed) {
		if (!Destruction.enabled(w)) {
			return;
		}
		Vec3d d = new Vec3d(dir.x, 0, dir.z).normalize();
		double cos = Math.cos(Math.toRadians(halfAngleDeg));
		int r = MathHelper.ceil(length);
		List<Long> cols = new ArrayList<>();
		List<Float> keys = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > length || dist < 1 || (dx * d.x + dz * d.z) / dist < cos) {
					continue;
				}
				cols.add(BlockPos.asLong(MathHelper.floor(from.x) + dx, 0, MathHelper.floor(from.z) + dz));
				keys.add((float) dist);
			}
		}
		long[] c = new long[cols.size()];
		float[] k = new float[cols.size()];
		for (int i = 0; i < c.length; i++) {
			c[i] = cols.get(i);
			k[i] = keys.get(i);
		}
		int[] thrown = {0};
		int budget = Kit.debrisBudget(w, from);
		TerrainJob job = new TerrainJob(w, c, k, col -> {
			double dist = Math.hypot(col.x + 0.5 - from.x, col.z + 0.5 - from.z);
			double s = 1 - dist / length;
			int surf = col.surface(), top = Math.min(col.top(), surf + 30);
			int dig = (int) Math.round(depth * s * (0.7 + col.noise(1) * 0.6));
			for (int y = top; y > surf - dig; y--) {
				BlockState st = col.get(y);
				if (st.isAir()) {
					continue;
				}
				if (thrown[0] < budget && col.noise(y * 3 + 7) < 0.12) {
					BlockPos p = new BlockPos(col.x, y, col.z);
					Destruction.record(w, p, st);
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, p, st);
					fb.setDestroyedOnLanding();
					Kit.velocity(fb, d.multiply(1.2 + s * 1.5).add(0, 0.5 + s * 0.6, 0));
					thrown[0]++;
				} else {
					col.set(y, Blocks.AIR.getDefaultState());
				}
			}
		});
		DetonationManager.add(new Front(w, job, speed));
	}

	public static void melt(ServerWorld w, Vec3d c, double r, double speed) {
		if (Destruction.enabled(w)) {
			DetonationManager.add(new Front(w, meltJob(w, c, r), speed));
		}
		w.spawnParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 30, r * 0.4, 0.5, r * 0.4, 0.1);
		Kit.sound(w, c, SoundEvents.BLOCK_LAVA_EXTINGUISH, 32.0F, 0.5F);
		Kit.sound(w, c, SoundEvents.ITEM_BUCKET_EMPTY_LAVA, 32.0F, 0.6F);
	}
}
