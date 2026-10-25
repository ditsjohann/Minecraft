package com.armageddon.destruction.world;

import com.armageddon.destruction.Destruction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Individual block writes, placed in order (sort them first, e.g. bottom-up so structures visibly grow),
 * a budget at a time. Same guards as {@link TerrainJob}.
 */
public class VoxelQueue {
	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private record Voxel(long pos, BlockState state, int order) {
	}

	private final ServerWorld world;
	private final List<Voxel> voxels = new ArrayList<>();
	private int cursor;
	private boolean sorted = true;

	public VoxelQueue(ServerWorld world) {
		this.world = world;
	}

	/** Queues a write; lower {@code order} goes first. */
	public void add(BlockPos pos, BlockState state, int order) {
		voxels.add(new Voxel(pos.asLong(), state, order));
		sorted = false;
	}

	public boolean isDone() {
		return cursor >= voxels.size();
	}

	public int step(int budget) {
		if (!sorted) {
			List<Voxel> rest = new ArrayList<>(voxels.subList(cursor, voxels.size()));
			rest.sort((a, b) -> Integer.compare(a.order, b.order));
			voxels.subList(cursor, voxels.size()).clear();
			voxels.addAll(rest);
			sorted = true;
		}
		BlockPos.Mutable p = new BlockPos.Mutable();
		while (budget > 0 && cursor < voxels.size()) {
			Voxel v = voxels.get(cursor++);
			p.set(v.pos);
			if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) || p.getY() < world.getBottomY() || p.getY() >= world.getTopY()) {
				continue;
			}
			BlockState old = world.getBlockState(p);
			if (old == v.state || old.hasBlockEntity()) {
				continue;
			}
			Destruction.set(world, p.toImmutable(), v.state, FLAGS);
			budget--;
		}
		return budget;
	}
}
