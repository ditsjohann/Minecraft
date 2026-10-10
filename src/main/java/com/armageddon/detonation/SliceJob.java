package com.armageddon.detonation;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Removes everything on one side of a vertical cutting plane inside a box, leaving a perfectly
 * flat cross-section of the world. The freshly exposed edge partially collapses.
 */
public class SliceJob implements WorldJob {
	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private final ServerWorld world;
	private final Vec3d origin;
	private final Vec3d along;
	private final Vec3d normal;
	private final int halfLength;
	private final int depth;
	private final int yMin;
	private final int yMax;
	private long cursor;
	private final long total;
	private final ProtectedRegions regions;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	/** @param normal horizontal unit vector pointing into the side that gets erased */
	public SliceJob(ServerWorld world, Vec3d origin, Vec3d normal, int halfLength, int depth, int down, int up) {
		this.world = world;
		this.origin = origin;
		this.normal = new Vec3d(normal.x, 0, normal.z).normalize();
		this.along = new Vec3d(-this.normal.z, 0, this.normal.x);
		this.halfLength = Destruction.radius(world, halfLength);
		this.depth = Destruction.radius(world, depth);
		this.yMin = MathHelper.floor(origin.y) - down;
		this.yMax = MathHelper.floor(origin.y) + up;
		this.total = (long) (this.halfLength * 2 + 1) * (this.depth + 1);
		this.regions = ProtectedRegions.get(world);
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return cursor >= total;
	}

	@Override
	public int step(int budget) {
		int w = halfLength * 2 + 1;
		while (budget > 0 && cursor < total) {
			int a = (int) (cursor % w) - halfLength;
			int n = (int) (cursor / w);
			cursor++;
			Vec3d c = origin.add(along.multiply(a)).add(normal.multiply(n + 0.5));
			int x = MathHelper.floor(c.x), z = MathHelper.floor(c.z);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			for (int y = Math.max(yMin, world.getBottomY()); y <= Math.min(yMax, world.getTopY() - 1); y++) {
				budget--;
				pos.set(x, y, z);
				BlockState s = world.getBlockState(pos);
				if (s.isAir() || s.getHardness(world, pos) < 0 || (!regions.isEmpty() && regions.isProtected(pos))) {
					continue;
				}
				Destruction.record(world, pos, s);
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), FLAGS);
			}
			// The first row behind the cut collapses a little.
			if (n == 0 && world.random.nextFloat() < 0.08F) {
				int y = yMin + world.random.nextInt(Math.max(1, yMax - yMin));
				BlockPos edge = BlockPos.ofFloored(c.subtract(normal.multiply(1.0))).withY(y);
				BlockState es = world.getBlockState(edge);
				if (!es.isAir() && es.getHardness(world, edge) >= 0 && es.getHardness(world, edge) < 20 && !es.hasBlockEntity()) {
					FallingBlockEntity.spawnFromBlock(world, edge, es).setDestroyedOnLanding();
				}
			}
		}
		return budget;
	}
}
