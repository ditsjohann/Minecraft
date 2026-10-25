package com.armageddon.destruction.world;

import com.armageddon.destruction.Destruction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Arrays;

/**
 * Repaints terrain column by column, nearest-key first, at most {@code budget} block writes per step.
 * A moving {@link #limit} lets the caller reveal the work as a front (a ripple, a spreading stain) instead
 * of all at once. Every write goes through {@link Destruction#set}, so protected regions and the
 * block-damage rule hold; block entities, bedrock and unloaded chunks are left alone.
 */
public class TerrainJob {
	/** Paints one column. */
	public interface Painter {
		void paint(Column c);
	}

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private final ServerWorld world;
	private final long[] cols;
	private final float[] keys;
	private final Painter painter;
	private final Column column;
	private int cursor;
	/** Only columns whose key is at most this are painted (yet). */
	public double limit = Double.MAX_VALUE;

	/** Every column within {@code radius} of {@code c} (horizontally) that {@code keep} accepts, ordered by distance. */
	public static TerrainJob disc(ServerWorld world, Vec3d c, double radius, Painter painter) {
		return disc(world, c, 0, radius, painter);
	}

	public static TerrainJob disc(ServerWorld world, Vec3d c, double inner, double radius, Painter painter) {
		int r = MathHelper.ceil(radius);
		int cx = MathHelper.floor(c.x), cz = MathHelper.floor(c.z);
		long[] tmp = new long[(2 * r + 1) * (2 * r + 1)];
		float[] key = new float[tmp.length];
		int n = 0;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d <= radius && d >= inner) {
					tmp[n] = BlockPos.asLong(cx + dx, 0, cz + dz);
					key[n] = (float) d;
					n++;
				}
			}
		}
		return new TerrainJob(world, Arrays.copyOf(tmp, n), Arrays.copyOf(key, n), painter);
	}

	public TerrainJob(ServerWorld world, long[] cols, float[] keys, Painter painter) {
		this.world = world;
		this.painter = painter;
		this.column = new Column(world);
		// Sort by key (indices sorted, then permuted).
		Integer[] idx = new Integer[cols.length];
		for (int i = 0; i < idx.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, (a, b) -> Float.compare(keys[a], keys[b]));
		this.cols = new long[cols.length];
		this.keys = new float[cols.length];
		for (int i = 0; i < idx.length; i++) {
			this.cols[i] = cols[idx[i]];
			this.keys[i] = keys[idx[i]];
		}
	}

	public boolean isDone() {
		return cursor >= cols.length;
	}

	/** Spends up to {@code budget} work units; returns what is left. */
	public int step(int budget) {
		while (budget > 0 && cursor < cols.length && keys[cursor] <= limit) {
			long packed = cols[cursor++];
			int x = BlockPos.unpackLongX(packed), z = BlockPos.unpackLongZ(packed);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			column.reset(x, z);
			painter.paint(column);
			budget -= 1 + column.writes + column.reads / 16;
		}
		return budget;
	}

	/** One column being painted: surface lookup plus guarded reads and writes. */
	public static final class Column {
		private final ServerWorld world;
		private final BlockPos.Mutable pos = new BlockPos.Mutable();
		public int x, z;
		int writes, reads;

		Column(ServerWorld world) {
			this.world = world;
		}

		void reset(int x, int z) {
			this.x = x;
			this.z = z;
			writes = 0;
			reads = 0;
		}

		public ServerWorld world() {
			return world;
		}

		/** Top solid block (ignores leaves and plants). */
		public int surface() {
			return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		}

		/** Top of anything that blocks motion, leaves included. */
		public int top() {
			return world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		}

		public int bottomY() {
			return world.getBottomY();
		}

		public BlockState get(int y) {
			reads++;
			return world.getBlockState(pos.set(x, y, z));
		}

		/** Writes a block unless it is protected, disabled, unchanged, unbreakable or a block entity. */
		public boolean set(int y, BlockState state) {
			if (y < world.getBottomY() || y >= world.getTopY()) {
				return false;
			}
			BlockState old = world.getBlockState(pos.set(x, y, z));
			if (old == state || old.hasBlockEntity()) {
				return false;
			}
			writes++;
			return Destruction.set(world, pos.toImmutable(), state, FLAGS);
		}

		/** Clears everything from {@code from} up to {@code to} (inclusive), skipping air quickly. */
		public void clear(int from, int to) {
			for (int y = from; y <= to; y++) {
				BlockState s = get(y);
				if (!s.isAir()) {
					set(y, Blocks.AIR.getDefaultState());
				}
			}
		}

		/** True for logs, leaves and plants: the stuff standing on the ground rather than the ground. */
		public static boolean growth(BlockState s) {
			return s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.REPLACEABLE)
					|| s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.FERN) || s.isOf(Blocks.LARGE_FERN)
					|| s.isOf(Blocks.VINE) || s.isOf(Blocks.SNOW) || s.isOf(Blocks.BAMBOO) || s.isOf(Blocks.SUGAR_CANE) || s.isOf(Blocks.CACTUS);
		}

		/** Hash noise for this column, 0..1. */
		public double noise(int salt) {
			long h = x * 734287L ^ z * 912931L ^ salt * 1013904223L;
			h = (h ^ (h >>> 13)) * 1274126177L;
			return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
		}
	}
}
