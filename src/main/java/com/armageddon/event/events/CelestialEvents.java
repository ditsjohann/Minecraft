package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.celestial.BlockQueue;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModGameRules;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.CelestialTimeline;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Celestial tier: world events. Every one is announced server-wide, honours protected regions,
 * the max-radius gamerule and the debris cap, and is cancellable with /armageddon purge.
 * Each leaves a landscape nothing else does: a bleached, monochrome field of solid light (Last Light),
 * a frozen hollow with the moment of impact hanging inside it (Zeropoint), the land's own wireframe
 * (Unmaker), a field of golden spears around a stepped temple (Godfall), and a bottomless pit under
 * upside-down islands (Final Existence).
 */
public final class CelestialEvents {
	private CelestialEvents() {
	}

	static void announce(ServerWorld world, Vec3d c, String key) {
		// No title or chat spam: the warning siren and the sky say it well enough.
		Vfx.warning(world, c, 2);
	}

	static int maxRadius(ServerWorld world, int wanted) {
		return Math.min(wanted, world.getGameRules().getInt(ModGameRules.MAX_RADIUS));
	}

	/** Top block that is ground or a tree trunk (leaves ignored). */
	static int groundY(ServerWorld world, int x, int z) {
		return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
	}

	static double hash3(int x, int y, int z) {
		long h = x * 374761393L + y * 668265263L + z * 2147483647L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	static boolean loaded(ServerWorld world, int x, int z) {
		return world.isChunkLoaded(x >> 4, z >> 4);
	}

	// ====================================================================== THE LAST LIGHT

	/**
	 * 46 THE LAST LIGHT: a dying white star beats like a heart. Every beat bleaches a wider ring of the world
	 * to bone, quartz and calcite and freezes its light into solid glass needles aimed at the star. It flatlines,
	 * swells, and exhales once: the colour drains out of the world and the star goes dark.
	 */
	public static class LastLight extends WorldEvent {
		private BlockQueue queue;
		private int r;
		private Vec3d star;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				r = maxRadius(world, 64);
				star = g.add(0, 38, 0);
				announce(world, g, "block.armageddon.last_light");
				queue = BlockQueue.start(world);
				Vfx.custom(world, FxType.CELESTIAL_FX, star, g, r, 0xFFF4E0, CelestialTimeline.FX_LAST_LIGHT, 0, 0, CelestialTimeline.LL_END, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 32.0F, 0.5F);
			}
			int[] beats = CelestialTimeline.LL_BEATS;
			for (int k = 0; k < beats.length; k++) {
				if (t == beats[k]) {
					beat(g, k);
				}
				if (t == beats[k] + 6) {
					Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 40.0F, 0.55F + k * 0.04F);
				}
			}
			if (t == CelestialTimeline.LL_FLATLINE) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 48.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F, 1.6F);
			}
			if (t == CelestialTimeline.LL_EXHALE) {
				exhale(g);
			}
			if (t > CelestialTimeline.LL_EXHALE && t < CelestialTimeline.LL_END && t % 8 == 0) {
				// White ash falling in the silence.
				Vfx.play(world, VfxIds.LL_ASH, g, (float) (r / 60.0));
			}
			if (t == CelestialTimeline.LL_END) {
				// The dead star goes out with one last detonation in the sky.
				Vfx.custom(world, FxType.SUPERNOVA, star, star, (float) (r * 0.45), 0xFFF0C0, 0, 0, 0, 0, Vfx.FAR);
				Kit.blast(world, star, 12.0F, owner(), false);
				Vfx.play(world, VfxIds.LL_DEATH, star, 1.0F);
				Vfx.ring(world, g.add(0, 1, 0), (float) (r * 1.6), 4.0F, 0xFFF4E0, true, 50);
				Vfx.flash(world, star, 1.0F, 3000, 0xFFFFFF);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 1.5F);
				for (LivingEntity e : Kit.living(world, g, r)) {
					Vec3d away = e.getPos().subtract(star).normalize();
					Kit.velocity(e, away.multiply(1.6).add(0, 0.5, 0));
				}
			}
			return t > CelestialTimeline.LL_END + 70;
		}

		private void beat(Vec3d g, int k) {
			double r0 = k == 0 ? 0 : CelestialTimeline.llRadius(k - 1, r);
			double r1 = CelestialTimeline.llRadius(k, r);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 56.0F, 0.5F + k * 0.04F);
			Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 24.0F, 0.6F + k * 0.1F);
			Vfx.shake(world, g, r * 3, 0.3F + k * 0.04F);
			Vfx.play(world, VfxIds.LL_BEAT, g, (float) (r1 / 20.0));
			for (LivingEntity e : Kit.living(world, g, r1 + 2)) {
				double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
				if (d < r0 - 2) {
					continue;
				}
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false));
				e.timeUntilRegen = 0;
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 3.0F + k);
			}
			if (queue == null) {
				return;
			}
			bleachRing(g, r0, r1);
			for (int i = 0; i < 3 + k; i++) {
				needle(g, r1);
			}
			for (int i = 0; i < 2; i++) {
				Vec3d d = Kit.dir(world.random.nextFloat() * 360.0F);
				Kit.fissure(world, g.add(d.multiply(r0 + 2)), d, (int) (r1 - r0) + 4, 3, 1, ModBlocks.LUMINOUS_GLASS.getDefaultState());
			}
		}

		private void exhale(Vec3d g) {
			Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 64.0F, 0.4F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.3F);
			// The signature: the world is drained of colour while the last light goes out.
			Vfx.screen(world, g, 100000, 0, 0.8F, 300);
			Vfx.shake(world, g, r * 4, 1.0F);
			Kit.damage(world, g, r, 60.0F, ModDamageTypes.ANNIHILATION, null, owner());
			for (LivingEntity e : Kit.living(world, g, r)) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1, false, false));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false));
			}
			if (queue != null) {
				for (int i = 0; i < 18; i++) {
					needle(g, r);
				}
			}
		}

		/** Bleaches every column in the ring r0..r1 from the treetops down to three blocks of ground. */
		private void bleachRing(Vec3d g, double r0, double r1) {
			int cx = MathHelper.floor(g.x), cz = MathHelper.floor(g.z);
			int ri = MathHelper.ceil(r1);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < r0 || d >= r1) {
						continue;
					}
					int x = cx + dx, z = cz + dz;
					if (!loaded(world, x, z)) {
						continue;
					}
					int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
					int ground = 0;
					for (int y = top; y > top - 48 && ground < 3 && y > world.getBottomY(); y--) {
						p.set(x, y, z);
						BlockState s = world.getBlockState(p);
						if (s.isAir()) {
							continue;
						}
						BlockState to = bleach(s, p);
						if (to != null) {
							queue.add(p.toImmutable(), to);
						}
						if (!s.getFluidState().isEmpty()) {
							ground = 3;
						} else if (!s.isIn(BlockTags.LOGS) && !s.isIn(BlockTags.LEAVES) && s.isOpaqueFullCube(world, p)) {
							ground++;
						}
					}
				}
			}
		}

		private BlockState bleach(BlockState s, BlockPos p) {
			if (s.hasBlockEntity()) {
				return null;
			}
			double h = hash3(p.getX(), p.getY(), p.getZ());
			if (!s.getFluidState().isEmpty()) {
				if (s.isOf(Blocks.WATER)) {
					return Blocks.ICE.getDefaultState();
				}
				return s.isOf(Blocks.LAVA) ? Blocks.QUARTZ_BLOCK.getDefaultState() : null;
			}
			if (s.isIn(BlockTags.LOGS)) {
				BlockState b = Blocks.BONE_BLOCK.getDefaultState();
				return s.contains(PillarBlock.AXIS) ? b.with(PillarBlock.AXIS, s.get(PillarBlock.AXIS)) : b;
			}
			if (s.isIn(BlockTags.LEAVES)) {
				return h < 0.1 ? ModBlocks.LUMINOUS_GLASS.getDefaultState() : Blocks.WHITE_STAINED_GLASS.getDefaultState();
			}
			if (s.isReplaceable() || s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.SAPLINGS)) {
				return s.isIn(BlockTags.SNOW) ? null : Blocks.AIR.getDefaultState();
			}
			if (!s.isOpaqueFullCube(world, p) || s.isIn(BlockTags.SNOW) || s.isOf(Blocks.SNOW_BLOCK)) {
				return null;
			}
			if (s.isIn(BlockTags.SAND)) {
				return Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
			}
			if (s.isIn(BlockTags.DIRT) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.CLAY)) {
				return Blocks.CALCITE.getDefaultState();
			}
			if (s.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
				return h < 0.7 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.CALCITE.getDefaultState();
			}
			return Blocks.WHITE_CONCRETE.getDefaultState();
		}

		/** A shaft of light frozen solid: a glass needle rising from the ground, aimed straight at the star. */
		private void needle(Vec3d g, double maxR) {
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(world.random.nextDouble()) * maxR;
			int x = MathHelper.floor(g.x + Math.cos(a) * d), z = MathHelper.floor(g.z + Math.sin(a) * d);
			if (!loaded(world, x, z)) {
				return;
			}
			Vec3d base = new Vec3d(x + 0.5, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z + 0.5);
			Vec3d v = star.subtract(base).normalize();
			int len = 6 + world.random.nextInt(14);
			BlockPos last = null;
			for (int i = -2; i < len * 2; i++) {
				BlockPos q = BlockPos.ofFloored(base.add(v.multiply(i * 0.5)));
				if (q.equals(last)) {
					continue;
				}
				last = q;
				queue.add(q, ModBlocks.LUMINOUS_GLASS.getDefaultState(), i < 2 ? BlockQueue.ANY : BlockQueue.AIR);
			}
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.close();
			}
		}
	}

	// ====================================================================== ZEROPOINT

	/**
	 * 47 ZEROPOINT ABSOLUTE: time stops inside a sphere. The ticking slows and dies, twitches backwards, then the
	 * moment snaps shut: the sphere is cut out of the world as a hollow lined with blue ice and crying obsidian,
	 * and torn-off pieces of the land hang frozen inside it, mid-flight toward the point where it all ended.
	 */
	public static class Zeropoint extends WorldEvent {
		private static final int[] CLOCK = {0, 6, 12, 20, 30, 44, 62, 86};
		private final Map<Entity, Boolean> frozen = new HashMap<>();
		/** Each frozen entity's own NoGravity flag, restored on release. */
		private final Map<Entity, Boolean> hadNoGravity = new HashMap<>();
		private final List<Long> carve = new ArrayList<>();
		private final List<BlockPos> fragPos = new ArrayList<>();
		private final List<BlockState> fragState = new ArrayList<>();
		private BlockQueue queue;
		private int r;
		private int slice;
		private BlockPos c;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				r = maxRadius(world, 30);
				c = BlockPos.ofFloored(Kit.ground(world, g.x, g.z));
				slice = -r;
				announce(world, g, "block.armageddon.zeropoint");
				queue = BlockQueue.start(world);
				Vfx.custom(world, FxType.CELESTIAL_FX, Vec3d.ofCenter(c), g, r, 0x9FEFFF, CelestialTimeline.FX_ZEROPOINT, 0, 0, CelestialTimeline.ZP_END, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 32.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 32.0F, 0.4F);
			}
			Vec3d cc = Vec3d.ofCenter(c);
			if (t < CelestialTimeline.ZP_SNAP) {
				freeze(cc);
				if (t % 6 == 0) {
					Vfx.play(world, VfxIds.ZP_HUSH, cc, r / 30.0F);
				}
				for (int k : CLOCK) {
					if (t == k) {
						Kit.sound(world, cc, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 24.0F, 1.3F - k * 0.006F);
					}
				}
				if (t >= CelestialTimeline.ZP_REWIND && t % 3 == 0) {
					Kit.sound(world, cc, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 24.0F, 1.9F);
				}
				if (queue != null) {
					for (int i = 0; i < 2 && slice <= r; i++) {
						scanSlice(slice++);
					}
				}
			}
			if (t == CelestialTimeline.ZP_SNAP) {
				snap(cc);
			}
			if (t == CelestialTimeline.ZP_SNAP + 3) {
				// Time restarts with a slight, icy burst out of the pinhole.
				Kit.blast(world, cc, 4.5F, owner(), false);
				Vfx.play(world, VfxIds.ZP_SNAP, cc, r / 30.0F);
				Vfx.ring(world, cc, r * 1.4F, 2.0F, 0xCFF8FF, true, 30);
			}
			return t > CelestialTimeline.ZP_SNAP + 130;
		}

		private void freeze(Vec3d cc) {
			for (Entity e : Kit.entities(world, cc, r, x -> !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
				if (!frozen.containsKey(e)) {
					frozen.put(e, e instanceof MobEntity m && !m.isAiDisabled());
					hadNoGravity.put(e, e.hasNoGravity());
					if (e instanceof MobEntity m) {
						m.setAiDisabled(true);
					}
				}
				e.setVelocity(Vec3d.ZERO);
				e.velocityModified = true;
				e.setNoGravity(true);
			}
		}

		/** Records one z-slice of the sphere: what will be cut out, and a few pieces to leave hanging. */
		private void scanSlice(int dz) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -r; dx <= r; dx++) {
				if (!loaded(world, c.getX() + dx, c.getZ() + dz)) {
					continue;
				}
				for (int dy = -r; dy <= r; dy++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState s = world.getBlockState(p);
					if (s.isAir() || s.hasBlockEntity() || s.getHardness(world, p) < 0) {
						continue;
					}
					carve.add(p.asLong());
					if (s.isOpaqueFullCube(world, p) && world.random.nextFloat() < 0.015F) {
						fragPos.add(p.toImmutable());
						fragState.add(s);
					}
				}
			}
		}

		private void snap(Vec3d cc) {
			release();
			for (Entity e : Kit.entities(world, cc, r, x -> !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
				Kit.velocity(e, cc.subtract(e.getPos()).normalize().multiply(0.8));
				if (e instanceof LivingEntity l) {
					l.timeUntilRegen = 0;
					l.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 40.0F);
					l.setFrozenTicks(300);
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 140, 0, false, false));
				}
			}
			Kit.sound(world, cc, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.5F);
			Kit.sound(world, cc, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 64.0F, 0.6F);
			Kit.sound(world, cc, ModSounds.ANTIMATTER, 40.0F, 1.4F);
			Vfx.shake(world, cc, r * 4, 1.4F);
			if (queue == null) {
				return;
			}
			BlockState air = Blocks.AIR.getDefaultState();
			for (long l : carve) {
				queue.add(BlockPos.fromLong(l), air);
			}
			carve.clear();
			// A polished shell: blue ice against the cut, packed ice behind it, veins of crying obsidian below ground.
			int ro = r + 2;
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -ro; dx <= ro; dx++) {
				for (int dz = -ro; dz <= ro; dz++) {
					for (int dy = -ro; dy <= ro; dy++) {
						double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
						if (d <= r || d > r + 1.7) {
							continue;
						}
						p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
						BlockState s = dy < 0 && hash3(p.getX(), p.getY(), p.getZ()) < 0.1 ? Blocks.CRYING_OBSIDIAN.getDefaultState()
								: d < r + 0.85 ? Blocks.BLUE_ICE.getDefaultState() : Blocks.PACKED_ICE.getDefaultState();
						queue.add(p.toImmutable(), s, BlockQueue.SOLID);
					}
				}
			}
			// The frozen instant: pieces of the land caught halfway to the point, hanging in the hollow.
			int n = Math.min(36, fragPos.size());
			for (int i = 0; i < n; i++) {
				int k = world.random.nextInt(fragPos.size());
				BlockPos o = fragPos.get(k);
				BlockState s = fragState.get(k);
				double f = 0.3 + world.random.nextDouble() * 0.4;
				BlockPos q = BlockPos.ofFloored(cc.add(Vec3d.ofCenter(o).subtract(cc).multiply(f)));
				queue.add(q, s, BlockQueue.AIR);
				for (int j = 0; j < 3; j++) {
					if (world.random.nextBoolean()) {
						queue.add(q.add(world.random.nextInt(3) - 1, world.random.nextInt(3) - 1, world.random.nextInt(3) - 1), s, BlockQueue.AIR);
					}
				}
			}
			fragPos.clear();
			fragState.clear();
			queue.add(c, Blocks.CRYING_OBSIDIAN.getDefaultState());
		}

		private void release() {
			for (Map.Entry<Entity, Boolean> en : frozen.entrySet()) {
				Entity e = en.getKey();
				if (e.isAlive()) {
					e.setNoGravity(hadNoGravity.getOrDefault(e, false));
					if (en.getValue() && e instanceof MobEntity m) {
						m.setAiDisabled(false);
					}
				}
			}
			frozen.clear();
			hadNoGravity.clear();
		}

		@Override
		public void finish() {
			release();
			if (queue != null) {
				queue.close();
			}
		}
	}

	// ====================================================================== THE UNMAKER

	/**
	 * 48 THE UNMAKER: a render pass sweeps across the land in a straight line. Behind it only the world's
	 * wireframe is left: a lattice of every fourth row of every block, water and lava turned to stained glass,
	 * the border flickering in the magenta and black of a missing texture.
	 */
	public static class Unmaker extends WorldEvent {
		private BlockQueue queue;
		private int r, cx, cz, gy, y0, y1, ptr;
		private Vec3d dir;
		private int[] colX, colZ;
		private double[] colU;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				start(g);
			}
			int end = CelestialTimeline.UM_START + CelestialTimeline.UM_SCAN;
			if (t >= CelestialTimeline.UM_START && t <= end + 1) {
				double s = CelestialTimeline.umPlane(t, r);
				while (ptr < colU.length && colU[ptr] <= s) {
					if (queue != null) {
						unmakeColumn(cx + colX[ptr], cz + colZ[ptr], Math.sqrt(colX[ptr] * colX[ptr] + colZ[ptr] * colZ[ptr]));
					}
					ptr++;
				}
				Vec3d plane = new Vec3d(cx + 0.5 + dir.x * s, gy + 1, cz + 0.5 + dir.z * s);
				if (t % 2 == 0 && Math.abs(s) < r) {
					// The pass itself: the world coming apart into rising scanlines along the sweep line.
					double half = Math.sqrt(r * r - s * s);
					Vec3d side = new Vec3d(-dir.z, 0, dir.x);
					Vfx.play(world, VfxIds.UM_PLANE, plane.subtract(side.multiply(half)), side.multiply(2 * half), 1.0F, 0);
				}
				if (t % 4 == 0) {
					hitPlane(s);
				}
				if (t % 5 == 0) {
					float[] scale = {0.5F, 0.6F, 0.75F, 0.9F, 1.0F, 1.2F, 0.9F, 0.6F};
					Kit.sound(world, plane, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 24.0F, scale[(t / 5) % scale.length]);
				}
				if (t % 20 == 0) {
					Kit.sound(world, plane, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 24.0F, 0.5F);
				}
			}
			if (t == end + 2) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 40.0F, 0.4F);
				Vfx.glitch(world, g, r, 0xFF40FF, 16);
			}
			return t > CelestialTimeline.UM_END;
		}

		private void start(Vec3d g) {
			r = maxRadius(world, 48);
			cx = MathHelper.floor(g.x);
			cz = MathHelper.floor(g.z);
			gy = groundY(world, cx, cz);
			y0 = Math.max(world.getBottomY() + 1, gy - CelestialTimeline.UM_BELOW);
			y1 = Math.min(world.getTopY() - 1, gy + CelestialTimeline.UM_ABOVE);
			dir = Kit.dir(data.getFloat("Yaw"));
			announce(world, g, "block.armageddon.unmaker");
			queue = BlockQueue.start(world);
			Vfx.custom(world, FxType.CELESTIAL_FX, new Vec3d(cx + 0.5, gy + 1, cz + 0.5), dir, r, 0xFF40FF, CelestialTimeline.FX_UNMAKER,
					y0, y1, CelestialTimeline.UM_END, Vfx.FAR);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 40.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 40.0F, 0.5F);
			// Columns sorted by their position along the sweep, so the pass reaches them in order.
			List<double[]> cols = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						cols.add(new double[]{dx * dir.x + dz * dir.z, dx, dz});
					}
				}
			}
			cols.sort((a, b) -> Double.compare(a[0], b[0]));
			colU = new double[cols.size()];
			colX = new int[cols.size()];
			colZ = new int[cols.size()];
			for (int i = 0; i < cols.size(); i++) {
				colU[i] = cols.get(i)[0];
				colX[i] = (int) cols.get(i)[1];
				colZ[i] = (int) cols.get(i)[2];
			}
		}

		private void unmakeColumn(int x, int z, double d) {
			if (!loaded(world, x, z)) {
				return;
			}
			boolean edge = d > r - 2.5;
			BlockState air = Blocks.AIR.getDefaultState();
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = y0; y <= y1; y++) {
				p.set(x, y, z);
				BlockState s = world.getBlockState(p);
				if (s.isAir() || s.hasBlockEntity()) {
					continue;
				}
				if (!CelestialTimeline.umLattice(x - cx, y - gy, z - cz)) {
					queue.add(p.toImmutable(), air);
				} else if (!s.getFluidState().isEmpty() && !s.isFullCube(world, p)) {
					BlockState glass = s.getFluidState().isOf(net.minecraft.fluid.Fluids.LAVA) || s.getFluidState().isOf(net.minecraft.fluid.Fluids.FLOWING_LAVA)
							? Blocks.ORANGE_STAINED_GLASS.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
					queue.add(p.toImmutable(), glass);
				} else if (!s.isFullCube(world, p)) {
					queue.add(p.toImmutable(), air);
				} else if (edge) {
					queue.add(p.toImmutable(), ((x + y + z) & 1) == 0 ? Blocks.MAGENTA_CONCRETE.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState());
				}
			}
		}

		/** Anything the pass crosses is unmade with it; dropped items simply stop existing. */
		private void hitPlane(double s) {
			Vec3d c = new Vec3d(cx + 0.5, gy, cz + 0.5);
			for (Entity e : Kit.entities(world, c, r + 2, x -> !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
				double u = (e.getX() - c.x) * dir.x + (e.getZ() - c.z) * dir.z;
				if (Math.abs(u - s) > 2.0 || e.getY() < y0 || e.getY() > y1 + 4) {
					continue;
				}
				if (e instanceof ItemEntity) {
					e.discard();
				} else if (e instanceof LivingEntity l) {
					l.timeUntilRegen = 0;
					l.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 12.0F);
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0, false, false));
				}
			}
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.close();
			}
		}
	}

	// ====================================================================== GODFALL

	/**
	 * 49 GODFALL PROTOCOL: six gates open in the sky and the gods throw golden spears, volley after faster
	 * volley, each one left standing where it struck. The last and largest spear presses a stepped temple into
	 * the ground: sandstone terraces ringed with gold, an altar, and the spear standing in it.
	 */
	public static class Godfall extends WorldEvent {
		private record Spear(int land, Vec3d start, Vec3d target, int len, boolean big) {
		}

		private final List<Spear> spears = new ArrayList<>();
		private BlockQueue queue;
		private int r, cx, cz, gy, nextVolley;
		/** Temple ring being pressed (one block of radius per tick); -1 before, -2 when finished. */
		private int ring = -1;

		private Vec3d panel(int i) {
			double a = i * Math.PI * 2 / 6;
			return center().add(Math.cos(a) * 40, 90, Math.sin(a) * 40);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				r = maxRadius(world, 40);
				cx = MathHelper.floor(g.x);
				cz = MathHelper.floor(g.z);
				gy = groundY(world, cx, cz);
				nextVolley = CelestialTimeline.GF_VOLLEY_START;
				announce(world, g, "block.armageddon.godfall");
				queue = BlockQueue.start(world);
				for (int i = 0; i < 6; i++) {
					Vfx.grid(world, panel(i), new Vec3d(0, -1, 0), 12.0F, 0xFFE8A0, 0, CelestialTimeline.GF_TEMPLE + 40);
				}
				Vfx.skyTint(world, g, 100000, 0xFFE0A0, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 32.0F, 0.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 32.0F, 0.5F);
			}
			if (t >= nextVolley && t < CelestialTimeline.GF_FINAL - CelestialTimeline.GF_SPEAR_FALL) {
				int n = 1 + (t - CelestialTimeline.GF_VOLLEY_START) / 60;
				for (int i = 0; i < n; i++) {
					launch(t, g);
				}
				nextVolley = t + Math.max(5, 24 - (t - CelestialTimeline.GF_VOLLEY_START) / 9);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 32.0F, 0.5F + world.random.nextFloat() * 0.3F);
			}
			if (t == CelestialTimeline.GF_FINAL) {
				Vec3d target = new Vec3d(cx + 0.5, gy + 1, cz + 0.5);
				Vec3d start = target.add(world.random.nextGaussian() * 12, 190, world.random.nextGaussian() * 12);
				spears.add(new Spear(t + CelestialTimeline.GF_FINAL_FALL, start, target, 36, true));
				Vfx.custom(world, FxType.CELESTIAL_FX, start, target, 36, 0xFFE080, CelestialTimeline.FX_SPEAR, 1, 0, CelestialTimeline.GF_FINAL_FALL, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.3F);
			}
			Iterator<Spear> it = spears.iterator();
			while (it.hasNext()) {
				Spear sp = it.next();
				if (sp.land() <= t) {
					land(sp);
					it.remove();
				}
			}
			if (t == CelestialTimeline.GF_TEMPLE) {
				pressTemple(g);
			}
			if (ring >= 0) {
				if (queue != null) {
					templeRing(ring);
				}
				ring++;
				if (ring > r + 1) {
					if (queue != null) {
						altarSpear();
					}
					ring = -2;
				}
			}
			return ring == -2 && t > CelestialTimeline.GF_TEMPLE + 20;
		}

		private void launch(int t, Vec3d g) {
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = r * (0.5 + world.random.nextDouble() * 0.85);
			Vec3d target = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
			Vec3d start = panel(world.random.nextInt(6)).add(world.random.nextGaussian() * 6, world.random.nextDouble() * 20, world.random.nextGaussian() * 6);
			int len = 11 + world.random.nextInt(9);
			spears.add(new Spear(t + CelestialTimeline.GF_SPEAR_FALL, start, target, len, false));
			Vfx.custom(world, FxType.CELESTIAL_FX, start, target, len, 0xFFD060, CelestialTimeline.FX_SPEAR, 0, 0, CelestialTimeline.GF_SPEAR_FALL, Vfx.FAR);
		}

		private void land(Spear sp) {
			Vec3d at = sp.target();
			Kit.sound(world, at, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, sp.big() ? 64.0F : 12.0F, sp.big() ? 0.5F : 1.5F);
			Kit.sound(world, at, SoundEvents.BLOCK_ANVIL_LAND, sp.big() ? 48.0F : 12.0F, 0.6F);
			if (sp.big()) {
				// The great spear's blocks go up once the temple has been pressed around it.
				Kit.boom(world, at.add(0, 2, 0), 11.0F, 0xFFD060, owner(), false);
				Vfx.pillar(world, at, 9.0F, 0xFFE8A0, false, 70);
				Vfx.ring(world, at.add(0, 1, 0), r * 2.2F, 4.0F, 0xFFE8A0, true, 45);
				return;
			}
			// Every spear lands as a holy shell: a golden blast, a scorched hole and a ring of light.
			Kit.boom(world, at, 3.2F, 0xFFD060, owner(), true);
			Kit.damage(world, at, 7, 30.0F, ModDamageTypes.PLASMA, null, owner());
			Vfx.ring(world, at.add(0, 0.5, 0), 14.0F, 1.5F, 0xFFE8A0, true, 22);
			world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 30, 1.5, 1.5, 1.5, 0.35);
			if (queue == null) {
				return;
			}
			Vec3d dir = sp.target().subtract(sp.start()).normalize();
			BlockPos last = null;
			for (double i = -3; i <= sp.len(); i += 0.5) {
				BlockPos q = BlockPos.ofFloored(at.subtract(dir.multiply(i)));
				if (q.equals(last)) {
					continue;
				}
				last = q;
				BlockState s = i < 1 ? ModBlocks.GOLD_FISSURE.getDefaultState()
						: i > sp.len() - 2 ? Blocks.SHROOMLIGHT.getDefaultState() : Blocks.HONEYCOMB_BLOCK.getDefaultState();
				queue.add(q, s);
			}
		}

		private void pressTemple(Vec3d g) {
			ring = 0;
			Vec3d c = new Vec3d(cx + 0.5, gy + 1, cz + 0.5);
			Vfx.custom(world, FxType.CELESTIAL_FX, c, g, r, 0xFFD060, CelestialTimeline.FX_TEMPLE, gy, 0,
					CelestialTimeline.GF_END - CelestialTimeline.GF_TEMPLE, Vfx.FAR);
			Kit.damage(world, c, r, 26.0F, ModDamageTypes.ANNIHILATION, null, owner());
			for (LivingEntity e : Kit.living(world, c, r)) {
				Vec3d out = new Vec3d(e.getX() - c.x, 0, e.getZ() - c.z);
				Kit.push(e, (out.lengthSquared() > 1.0E-4 ? out.normalize().multiply(0.6) : Vec3d.ZERO).add(0, 0.6, 0));
			}
			Vfx.shake(world, c, r * 5, 1.8F);
			Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.6F);
			Kit.sound(world, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.6F);
		}

		/** Presses one ring of the temple: everything above each terrace cut away, the floor laid in sandstone and gold. */
		private void templeRing(int k) {
			BlockState air = Blocks.AIR.getDefaultState();
			int ri = k + 1;
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < k || d >= k + 1) {
						continue;
					}
					int tier = CelestialTimeline.gfTier(d, r);
					int x = cx + dx, z = cz + dz;
					if (tier < 0 || !loaded(world, x, z)) {
						continue;
					}
					int floor = gy - tier * 2;
					int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
					for (int y = floor + 1; y <= top; y++) {
						queue.add(new BlockPos(x, y, z), air);
					}
					boolean rimTop = CelestialTimeline.gfTier(Math.max(0, d - 1.0), r) > tier;
					double h = hash3(x, floor, z);
					BlockState f;
					if (tier == CelestialTimeline.GF_STEPS && d < 2.5) {
						f = ModBlocks.GOLD_FISSURE.getDefaultState();
					} else if (rimTop) {
						f = h < 0.2 ? ModBlocks.GOLD_FISSURE.getDefaultState() : Blocks.CHISELED_SANDSTONE.getDefaultState();
					} else {
						f = tier % 2 == 0 ? Blocks.SMOOTH_SANDSTONE.getDefaultState() : Blocks.CUT_SANDSTONE.getDefaultState();
					}
					queue.add(new BlockPos(x, floor, z), f);
					for (int y = floor - 1; y >= floor - 3; y--) {
						queue.add(new BlockPos(x, y, z), Blocks.SANDSTONE.getDefaultState());
					}
				}
			}
		}

		/** The great spear, standing upright in the altar. */
		private void altarSpear() {
			int base = gy - CelestialTimeline.GF_STEPS * 2 + 1;
			for (int i = 0; i <= 36; i++) {
				BlockPos q = new BlockPos(cx, base + i, cz);
				BlockState s = i < 2 ? ModBlocks.GOLD_FISSURE.getDefaultState() : i > 33 ? Blocks.SHROOMLIGHT.getDefaultState() : Blocks.HONEYCOMB_BLOCK.getDefaultState();
				queue.add(q, s);
				if (i < 30) {
					BlockState side = i < 2 ? ModBlocks.GOLD_FISSURE.getDefaultState() : Blocks.HONEYCOMB_BLOCK.getDefaultState();
					queue.add(q.east(), side);
					queue.add(q.west(), side);
					queue.add(q.north(), side);
					queue.add(q.south(), side);
				}
			}
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.close();
			}
		}
	}

	// ====================================================================== FINAL EXISTENCE

	/**
	 * 50 OMEGA: FINAL EXISTENCE. Gravity reverses and the landscape rises toward a dark sphere. The sky cracks,
	 * then shatters like glass onto an empty cosmos, and the floor of the world falls away: a bottomless pit
	 * walled in blackstone and crying obsidian, with pieces of the old land floating upside-down above it.
	 */
	public static class FinalExistence extends WorldEvent {
		private static final class Island {
			final int x, z, y;
			final List<int[]> offs = new ArrayList<>();
			final List<BlockState> states = new ArrayList<>();

			Island(int x, int z, int y) {
				this.x = x;
				this.z = z;
				this.y = y;
			}
		}

		private List<FallingBlockEntity> rising = new ArrayList<>();
		private final Set<Entity> floated = new HashSet<>();
		private final List<Island> islands = new ArrayList<>();
		private BlockQueue queue;
		private int pit, cx, cz, gy, bottom;
		/** Next layer of the pit to cut (top-down); MIN_VALUE until the floor falls, MAX_VALUE once done. */
		private int layer = Integer.MIN_VALUE;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d sphere = g.add(0, 90, 0);
			int r = maxRadius(world, 160);
			if (t == 0) {
				pit = maxRadius(world, 34);
				cx = MathHelper.floor(g.x);
				cz = MathHelper.floor(g.z);
				gy = groundY(world, cx, cz);
				bottom = world.getBottomY() + 5;
				queue = BlockQueue.start(world);
				announce(world, g, "block.armageddon.final_existence");
				// Ends as the collapse orb (t == FE_BREAK) begins, or it would hide the collapse.
				Vfx.orb(world, sphere, 2.0F, 40.0F, 0x100008, 1, CelestialTimeline.FE_BREAK);
				Vfx.skyTint(world, g, 100000, 0x180010, 1.0F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.2F);
			}
			if (t >= 60 && t < 300) {
				if (t == 60) {
					Vfx.gravityFlip(world, g, r, 240);
				}
				if (t % 20 == 0) {
					// Rain, dust and pebbles falling up toward the sphere.
					Vfx.play(world, VfxIds.INVERT_RAIN, g, 1.0F);
				}
				if (t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, g, r)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 15, 1, false, false));
							e.fallDistance = 0;
							floated.add(e);
						}
					}
				}
			}
			if (t == 300) {
				// Levitation is over: let everything that floated drift back down instead of dying of fall damage.
				for (Entity e : floated) {
					if (e.isAlive() && e instanceof LivingEntity l) {
						l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false));
					}
				}
				floated.clear();
			}
			if (t == 150 && queue != null) {
				snapshotIslands();
			}
			if (t == 80) {
				// The dark sphere becomes a black hole: its accretion disc, photon ring and jets are particles (ACCRETION).
			}
			if (t >= 80 && t < CelestialTimeline.FE_BREAK) {
				if (t % 6 == 0) {
					// The ground is torn up over a wide field, ever faster, and swallowed.
					double reach = r * Math.min(1.0, 0.35 + (t - 80) / 300.0);
					int n = Math.min(160, 60 + (t - 80) / 2);
					rising.addAll(Kit.lift(world, g, reach, n, true, true));
					if (t % 12 == 0) {
						rising.addAll(Kit.lift(world, new Vec3d(g.x, gy - 4, g.z), pit * 0.8, 40, false, true));
					}
				}
				if (t % 6 == 0) {
					// Sized to the horizon (the orb grows from 2 to 40 blocks by the collapse).
					float horizon = 2.0F + 38.0F * t / CelestialTimeline.FE_BREAK;
					Vfx.play(world, VfxIds.ACCRETION, sphere, Math.max(0.6F, horizon / 20.0F));
				}
				if (t % 20 == 0) {
					Vfx.shake(world, g, 2048, 0.8F + (t - 80) * 0.004F);
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 48.0F, 0.4F);
				}
				if (t % 40 == 0) {
					Kit.sound(world, sphere, ModSounds.BLACK_HOLE, 64.0F, 0.4F + (t - 80) * 0.002F);
				}
				double pull = 0.04 + (t - 80) * 0.0004;
				for (Entity e : Kit.entities(world, sphere, r, x -> !(x instanceof EventEntity) && !(x instanceof FallingBlockEntity) && !Kit.creativeOrSpectator(x))) {
					Vec3d to = sphere.subtract(e.getPos());
					double d = to.length();
					if (d < 7 && e instanceof LivingEntity l) {
						l.timeUntilRegen = 0;
						l.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 2.0F);
					}
					Kit.push(e, to.normalize().multiply(pull * Math.max(0.3, 1 - d / r)));
					e.fallDistance = 0;
				}
				Iterator<FallingBlockEntity> fit = rising.iterator();
				while (fit.hasNext()) {
					FallingBlockEntity fb = fit.next();
					if (!fb.isAlive()) {
						fit.remove();
						continue;
					}
					Vec3d to = sphere.subtract(fb.getPos());
					double d = to.length();
					if (d < 6) {
						fb.discard();
						fit.remove();
						continue;
					}
					// Spiral in: mostly straight down the well, with a swirl that tightens near the horizon.
					Vec3d in = to.multiply(1 / d);
					Vec3d swirl = new Vec3d(-in.z, 0, in.x);
					double speed = 0.5 + 2.2 * Math.max(0, 1 - d / 220.0);
					Kit.velocity(fb, in.multiply(speed).add(swirl.multiply(0.5 * Math.min(1, 40 / d))).add(0, 0.08, 0));
				}
			}
			if (t == CelestialTimeline.FE_CRACK) {
				Vfx.custom(world, FxType.CELESTIAL_FX, new Vec3d(cx + 0.5, gy + 1, cz + 0.5), g, 230, 0xFF60FF, CelestialTimeline.FX_SHATTER,
						CelestialTimeline.FE_BREAK - CelestialTimeline.FE_CRACK, 0, CelestialTimeline.FE_END - CelestialTimeline.FE_CRACK, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.2F);
			}
			if (t == CelestialTimeline.FE_BREAK + 24) {
				// Overfed, the black hole combusts into a cosmic beam of light that drives into the world.
				Vec3d floor = new Vec3d(cx + 0.5, gy + 1, cz + 0.5);
				Vfx.custom(world, FxType.SUPERNOVA, sphere, sphere, 70.0F, 0xF0E0FF, 0, 0, 0, 0, Vfx.FAR);
				Vfx.play(world, VfxIds.COSMIC_NOVA, sphere, 1.0F);
				Kit.boom(world, sphere, 16.0F, 0xE0C0FF, owner(), false);
				Vfx.beam(world, floor, sphere.add(0, 900, 0), 26.0F, 0xF4E8FF, 200);
				Vfx.pillar(world, floor, pit * 0.8F, 0xE8D8FF, true, 200);
				Vfx.ring(world, floor, r * 1.2F, 6.0F, 0xFFFFFF, true, 70);
				Vfx.flash(world, sphere, 1.0F, 6000, 0xFFFFFF);
				Vfx.screen(world, g, 100000, 1, 0.6F, 18);
				Vfx.skyTint(world, g, 100000, 0x6040A0, 0.7F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.4F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.5F);
			}
			if (t > CelestialTimeline.FE_BREAK + 24 && t < CelestialTimeline.FE_BREAK + 200 && t % 10 == 0) {
				Kit.damage(world, new Vec3d(cx + 0.5, gy + 1, cz + 0.5), pit * 0.8, 12.0F, ModDamageTypes.ANNIHILATION, null, owner());
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F);
			}
			if (t == CelestialTimeline.FE_BREAK) {
				Kit.vanish(rising);
				Vfx.orb(world, sphere, 40.0F, 0.1F, 0x100008, 2, 24);
				Vfx.play(world, VfxIds.VOID_COLLAPSE, sphere, 1.0F);
				Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 64.0F, 0.3F);
			}
			if (t == CelestialTimeline.FE_FALL) {
				floorFalls(g);
				// Stardust drifting down the bottomless pit, a faint glow breathing up from below.
				Vfx.play(world, VfxIds.ABYSS_MOTES, new Vec3d(cx + 0.5, gy, cz + 0.5), pit / 30.0F);
			}
			if (layer != Integer.MIN_VALUE && layer != Integer.MAX_VALUE) {
				for (int i = 0; i < 5 && layer >= bottom; i++) {
					carveLayer(layer--);
				}
				if (layer < bottom) {
					finishPit();
					layer = Integer.MAX_VALUE;
				}
			}
			return t > CelestialTimeline.FE_FALL + 40 && (queue == null || layer == Integer.MAX_VALUE);
		}

		private void floorFalls(Vec3d g) {
			Vec3d c = new Vec3d(cx + 0.5, gy, cz + 0.5);
			Kit.damage(world, c, pit * 1.3, 26.0F, ModDamageTypes.ANNIHILATION, null, owner());
			for (LivingEntity e : Kit.living(world, c, pit * 1.3)) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 500, 0, false, false));
			}
			Vfx.shake(world, c, pit * 6, 1.6F);
			Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.3F);
			if (queue != null) {
				layer = gy + 45;
			}
		}

		/** Copies cone-shaped plugs of the surface now, to hang them upside-down over the pit later. */
		private void snapshotIslands() {
			for (int i = 0; i < 7; i++) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double d = Math.sqrt(world.random.nextDouble()) * pit * 0.7;
				int ix = MathHelper.floor(cx + Math.cos(a) * d), iz = MathHelper.floor(cz + Math.sin(a) * d);
				int ir = 3 + world.random.nextInt(4);
				Island is = new Island(ix, iz, gy + 34 + world.random.nextInt(30));
				for (int dx = -ir; dx <= ir; dx++) {
					for (int dz = -ir; dz <= ir; dz++) {
						double dist = Math.sqrt(dx * dx + dz * dz);
						int x = ix + dx, z = iz + dz;
						if (dist > ir || !loaded(world, x, z)) {
							continue;
						}
						int depth = (int) ((ir - dist) * 1.4) + 1;
						int ys = groundY(world, x, z);
						for (int k = 0; k <= depth; k++) {
							BlockState s = world.getBlockState(new BlockPos(x, ys - k, z));
							if (s.isAir() || s.hasBlockEntity() || !s.getFluidState().isEmpty() || !s.isOpaqueFullCube(world, new BlockPos(x, ys - k, z))) {
								s = k > 2 ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState();
							}
							is.offs.add(new int[]{dx, dz, k});
							is.states.add(s);
						}
					}
				}
				islands.add(is);
			}
		}

		private void carveLayer(int y) {
			BlockState air = Blocks.AIR.getDefaultState();
			int ro = pit + 2;
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -ro; dx <= ro; dx++) {
				for (int dz = -ro; dz <= ro; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					int x = cx + dx, z = cz + dz;
					if (d > pit + 1.5 || !loaded(world, x, z)) {
						continue;
					}
					p.set(x, y, z);
					BlockState s = world.getBlockState(p);
					if (s.isAir()) {
						continue;
					}
					if (d <= pit) {
						queue.add(p.toImmutable(), air);
					} else {
						double h = hash3(x, y, z);
						BlockState wall = h < 0.1 ? Blocks.CRYING_OBSIDIAN.getDefaultState() : y > gy - 3 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
						queue.add(p.toImmutable(), wall, BlockQueue.SOLID);
					}
				}
			}
		}

		private void finishPit() {
			for (int dx = -pit; dx <= pit; dx++) {
				for (int dz = -pit; dz <= pit; dz++) {
					if (dx * dx + dz * dz <= pit * pit) {
						queue.add(new BlockPos(cx + dx, bottom, cz + dz), ModBlocks.VOID_REMNANT.getDefaultState());
					}
				}
			}
			for (Island is : islands) {
				for (int i = 0; i < is.offs.size(); i++) {
					int[] o = is.offs.get(i);
					// Upside-down: the old surface faces the pit, the deepest stone points at the sky.
					queue.add(new BlockPos(is.x + o[0], is.y + o[2], is.z + o[1]), is.states.get(i), BlockQueue.AIR);
					if (o[2] == 0 && hash3(is.x + o[0], is.y, is.z + o[1]) < 0.45) {
						queue.add(new BlockPos(is.x + o[0], is.y - 1, is.z + o[1]), Blocks.HANGING_ROOTS.getDefaultState(), BlockQueue.AIR);
					}
				}
			}
			islands.clear();
		}

		@Override
		public void finish() {
			Kit.vanish(rising);
			if (queue != null) {
				queue.close();
			}
		}
	}
}
