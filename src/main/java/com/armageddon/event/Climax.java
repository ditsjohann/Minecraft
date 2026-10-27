package com.armageddon.event;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.destruction.world.Devastation;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.network.Vfx;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Building blocks for the big finishes of the city- and world-destruction events: strikes out of the sky, a vortex
 * that swallows or abducts the land, a ground wave, a frozen moment, and terrain painters (overgrowth, sinkholes).
 * Everything honours the block-damage rule, protected regions and the debris budget like the rest of the mod.
 */
public final class Climax {
	private Climax() {
	}

	/** A random point on the ground in the ring {@code r0..r1} around {@code c}. */
	public static Vec3d around(ServerWorld w, Vec3d c, double r0, double r1) {
		double a = w.random.nextDouble() * MathHelper.TAU;
		double d = Math.sqrt(MathHelper.lerp(w.random.nextDouble(), r0 * r0 / Math.max(1, r1 * r1), 1.0)) * r1;
		return Kit.ground(w, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
	}

	/**
	 * Something falls out of the sky onto {@code hit}: a streak from {@code height} up, then a real explosion and a
	 * crater of {@code crater} blocks ({@code 0} for none) when it lands {@code delay} ticks later.
	 */
	public static void strike(ServerWorld w, Vec3d hit, double height, int color, float boom, float crater, CraterJob.Mode mode, int delay,
			@Nullable Entity owner) {
		Vec3d from = hit.add(w.random.nextGaussian() * height * 0.25, height, w.random.nextGaussian() * height * 0.25);
		Vfx.streak(w, from, hit, 1.0F + boom * 0.25F, color, delay + 2);
		DetonationManager.schedule(w, delay, () -> {
			Kit.boom(w, hit.add(0, 0.5, 0), boom, color, owner, true);
			if (crater > 0 && Destruction.enabled(w)) {
				DetonationManager.add(new CraterJob(w, BlockPos.ofFloored(hit), crater, mode));
			}
		});
	}

	/** Something falls out of the sky onto {@code hit} and {@code land} runs when it arrives (no explosion of its own). */
	public static void fall(ServerWorld w, Vec3d hit, double height, int color, float width, int delay, Runnable land) {
		Vec3d from = hit.add(w.random.nextGaussian() * height * 0.25, height, w.random.nextGaussian() * height * 0.25);
		Vfx.streak(w, from, hit, width, color, delay + 2);
		DetonationManager.schedule(w, delay, land);
	}

	/** Surface blocks in the ring {@code r0..r1}, turned into flying blocks (that land back as blocks unless {@code vanish}). */
	public static List<FallingBlockEntity> liftRing(ServerWorld w, Vec3d c, double r0, double r1, int max, boolean vanish) {
		List<FallingBlockEntity> out = new ArrayList<>();
		if (!Destruction.enabled(w)) {
			return out;
		}
		max = Math.min(max, Kit.debrisBudget(w, c));
		ProtectedRegions regions = ProtectedRegions.get(w);
		int tries = max * 5;
		while (out.size() < max && tries-- > 0) {
			double a = w.random.nextDouble() * MathHelper.TAU;
			double d = MathHelper.lerp(w.random.nextDouble(), r0, r1);
			int x = MathHelper.floor(c.x + Math.cos(a) * d), z = MathHelper.floor(c.z + Math.sin(a) * d);
			if (!w.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos p = Kit.surface(w, x, z);
			BlockState s = w.getBlockState(p);
			if (!Kit.liftable(w, p, s, regions)) {
				continue;
			}
			Destruction.record(w, p, s);
			FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, p, s);
			if (vanish) {
				fb.setDestroyedOnLanding();
			}
			out.add(fb);
		}
		return out;
	}

	/**
	 * The ground heaves in a ring: a band of surface blocks is thrown up and falls back a little further out, and
	 * anyone standing on it is bounced into the air. Call it with a growing radius to send a wave across the land.
	 */
	public static void groundWave(ServerWorld w, Vec3d c, double r, double width, int blocks, double kick) {
		for (FallingBlockEntity fb : liftRing(w, c, r, r + width, blocks, false)) {
			Vec3d out = new Vec3d(fb.getX() - c.x, 0, fb.getZ() - c.z).normalize();
			Kit.velocity(fb, out.multiply(0.12).add(0, kick * (0.7 + w.random.nextDouble() * 0.5), 0));
		}
		for (LivingEntity e : Kit.living(w, c, r + width + 2)) {
			double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
			if (d >= r - 2 && e.isOnGround() && !Kit.creativeOrSpectator(e)) {
				Kit.push(e, new Vec3d(0, kick * 0.9, 0));
			}
		}
	}

	/** Holds everything within {@code r} still: no walking, no swinging, mobs frozen mid-step, for {@code ticks}. */
	public static void freeze(ServerWorld w, Vec3d c, double r, int ticks) {
		for (LivingEntity e : Kit.living(w, c, r)) {
			if (Kit.creativeOrSpectator(e)) {
				continue;
			}
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 9, false, false));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, ticks, 4, false, false));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ticks, 250, false, false));
			e.setVelocity(Vec3d.ZERO);
			e.velocityModified = true;
		}
	}

	/**
	 * Flying blocks steered every tick: they spiral around {@code centre} and are drawn in ({@code inward}) and up
	 * ({@code rise}), and disappear once they arrive. A swallowing maw (rise negative, into a pit) or an abduction
	 * (rise positive, up a beam).
	 */
	public static final class Vortex {
		public final List<FallingBlockEntity> blocks = new ArrayList<>();
		private final Vec3d centre;
		private final double spin, inward, rise, swallowAt;

		public Vortex(Vec3d centre, double spin, double inward, double rise, double swallowAt) {
			this.centre = centre;
			this.spin = spin;
			this.inward = inward;
			this.rise = rise;
			this.swallowAt = swallowAt;
		}

		public void feed(List<FallingBlockEntity> more) {
			for (FallingBlockEntity fb : more) {
				fb.setNoGravity(true);
				fb.setDestroyedOnLanding();
				blocks.add(fb);
			}
		}

		public void tick() {
			blocks.removeIf(fb -> {
				if (!fb.isAlive()) {
					return true;
				}
				Vec3d to = new Vec3d(centre.x - fb.getX(), 0, centre.z - fb.getZ());
				double d = to.length();
				boolean arrived = rise >= 0 ? fb.getY() - centre.y > swallowAt : d < swallowAt || fb.getY() < centre.y - 6;
				if (arrived) {
					fb.discard();
					return true;
				}
				Vec3d in = d > 1.0E-3 ? to.multiply(1 / d) : Vec3d.ZERO;
				Vec3d tangent = new Vec3d(-in.z, 0, in.x);
				double pull = rise >= 0 ? inward * Math.min(1.0, d / 6.0) : inward;
				Kit.velocity(fb, tangent.multiply(spin).add(in.multiply(pull)).add(0, rise >= 0 ? rise : rise * Math.min(1.0, 12.0 / Math.max(1, d)), 0));
				return false;
			});
		}

		public void release() {
			Kit.vanish(blocks);
		}
	}

	// ------------------------------------------------------------------ terrain painters

	/** Mud and rooted earth: what a collapsing, hollowed-out ground leaves behind. */
	public static final Devastation.Palette SINK = new Devastation.Palette(Blocks.MUD.getDefaultState(), Blocks.PACKED_MUD.getDefaultState(),
			Blocks.COARSE_DIRT.getDefaultState(), Blocks.MUD.getDefaultState(), 0.0F);

	/** The ground gives way: a deep, muddy bowl of {@code radius}, with everything near its edge torn down. */
	public static TerrainJob sinkhole(ServerWorld w, Vec3d c, double radius, int base) {
		TerrainJob j = Devastation.of(w, c, radius, radius * 1.25, base, SINK);
		j.limit = 0;
		return j;
	}

	private static final BooleanProperty[] VINES = {VineBlock.NORTH, VineBlock.SOUTH, VineBlock.WEST, VineBlock.EAST};
	private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	/** What moss and time do to a block, or null to leave it. */
	private static @Nullable BlockState reclaimed(BlockState s) {
		if (s.isOf(Blocks.COBBLESTONE)) {
			return Blocks.MOSSY_COBBLESTONE.getDefaultState();
		}
		if (s.isOf(Blocks.STONE_BRICKS) || s.isOf(Blocks.CRACKED_STONE_BRICKS) || s.isOf(Blocks.CHISELED_STONE_BRICKS)) {
			return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
		}
		if (s.isOf(Blocks.COBBLESTONE_WALL)) {
			return Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState();
		}
		if (s.isOf(Blocks.STONE_BRICK_WALL)) {
			return Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState();
		}
		if (s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.DIRT) || s.isOf(Blocks.SAND)
				|| s.isOf(Blocks.STONE) || s.isOf(Blocks.ANDESITE) || s.isOf(Blocks.SMOOTH_STONE) || s.isOf(Blocks.POLISHED_ANDESITE)) {
			return Blocks.MOSS_BLOCK.getDefaultState();
		}
		return null;
	}

	/**
	 * Nature takes the city back: stone goes mossy, roads and yards turn to moss and grass, glass breaks out, vines
	 * climb every wall, roofs sprout azalea, flowers and ferns fill the ground and young trees push up between the
	 * houses. Stronger toward the centre; nothing is knocked down, it is all simply overgrown.
	 */
	public static TerrainJob overgrow(ServerWorld w, Vec3d c, double radius) {
		TerrainJob j = TerrainJob.disc(w, c, radius, col -> {
			double d = Math.hypot(col.x + 0.5 - c.x, col.z + 0.5 - c.z);
			double s = MathHelper.clamp(1.15 - d / radius, 0.0, 1.0);
			int surf = col.surface(), top = Math.min(col.top(), surf + 48);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = top; y >= surf - 1; y--) {
				BlockState st = col.get(y);
				double n = col.noise(y * 17 + 3);
				if (st.isAir()) {
					// Vines on any wall this air touches.
					if (y > surf && n < 0.32 * s) {
						for (int k = 0; k < 4; k++) {
							Direction dir = SIDES[k];
							p.set(col.x + dir.getOffsetX(), y, col.z + dir.getOffsetZ());
							BlockState wall = w.getBlockState(p);
							if (wall.isOpaqueFullCube(w, p) && !Devastation.natural(wall)) {
								col.set(y, Blocks.VINE.getDefaultState().with(VINES[k], true));
								break;
							}
						}
					}
					continue;
				}
				if (st.isIn(BlockTags.IMPERMEABLE) || st.isOf(Blocks.GLASS_PANE) || st.getBlock() instanceof net.minecraft.block.StainedGlassPaneBlock) {
					if (n < s) {
						col.set(y, Blocks.AIR.getDefaultState());
					}
					continue;
				}
				BlockState r = reclaimed(st);
				if (r != null && n < 0.4 + 0.6 * s && (y >= surf - 1)) {
					col.set(y, r);
					st = r;
				}
			}
			// Whatever ends up on top gets something growing on it.
			int y = col.top();
			BlockState t = col.get(y);
			double n = col.noise(91);
			if (!col.get(y + 1).isAir()) {
				return;
			}
			boolean ground = y <= surf + 1 && (t.isOf(Blocks.GRASS_BLOCK) || t.isOf(Blocks.MOSS_BLOCK) || t.isOf(Blocks.DIRT) || t.isOf(Blocks.PODZOL));
			if (ground) {
				if (col.noise(92) < 0.006 * s) {
					tree(col, y + 1, col.noise(93) < 0.35);
				} else if (n < 0.55 * s) {
					BlockState plant = n < 0.08 ? Blocks.AZALEA.getDefaultState() : n < 0.16 ? Blocks.FERN.getDefaultState()
							: n < 0.22 ? Blocks.CORNFLOWER.getDefaultState() : n < 0.27 ? Blocks.POPPY.getDefaultState()
							: n < 0.3 ? Blocks.OXEYE_DAISY.getDefaultState() : Blocks.SHORT_GRASS.getDefaultState();
					col.set(y + 1, plant);
				}
			} else if (t.isOpaqueFullCube(w, new BlockPos(col.x, y, col.z)) && n < 0.5 * s) {
				// Roofs and wall-tops: moss carpets and flowering azalea bushes.
				col.set(y + 1, n < 0.18 * s ? Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true)
						: n < 0.3 * s ? Blocks.AZALEA_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true)
						: Blocks.MOSS_CARPET.getDefaultState());
			}
		});
		j.limit = 0;
		return j;
	}

	/** A young oak or birch: a short trunk and a ragged crown, written straight into the world. */
	private static void tree(TerrainJob.Column col, int y0, boolean birch) {
		ServerWorld w = col.world();
		int h = 4 + (int) (col.noise(94) * 4);
		BlockState log = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).getDefaultState();
		BlockState leaf = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true);
		for (int i = 0; i < h; i++) {
			col.set(y0 + i, log);
		}
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy <= 2; dy++) {
					int rr = dx * dx + dz * dz + dy * dy;
					if (rr > 5 || (dx == 0 && dz == 0 && dy < 1) || col.noise(dx * 31 + dz * 7 + dy * 3 + 95) < 0.15) {
						continue;
					}
					p.set(col.x + dx, y0 + h - 1 + dy, col.z + dz);
					if (w.getBlockState(p).isAir()) {
						Destruction.set(w, p.toImmutable(), leaf, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
				}
			}
		}
		Kit.sound(w, new Vec3d(col.x, y0, col.z), SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 6.0F, 0.7F);
	}
}
