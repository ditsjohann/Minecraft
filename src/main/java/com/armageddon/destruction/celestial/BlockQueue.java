package com.armageddon.destruction.celestial;

import com.armageddon.destruction.Destruction;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.WorldJob;
import com.armageddon.event.Kit;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;

/**
 * An ordered queue of block edits applied a few thousand per tick through {@link DetonationManager}.
 * Edits run first-in first-out, so a carve queued before a build is finished before the build starts.
 * Every edit goes through {@link Destruction#set} (block-damage rule, protected regions, unbreakables)
 * and skips block entities and unloaded chunks. The queue stays alive until {@link #close()}.
 */
public final class BlockQueue implements WorldJob {
	/** Replace whatever is there. */
	public static final int ANY = 0;
	/** Only fill air (or replaceable plants). */
	public static final int AIR = 1;
	/** Only replace something solid. */
	public static final int SOLID = 2;

	private static final int PER_TICK = 2500;

	private record Op(long pos, BlockState state, int mode) {
	}

	private final ServerWorld world;
	private final ArrayDeque<Op> ops = new ArrayDeque<>();
	private boolean open = true;
	/** Ticks spent empty; an abandoned queue (its event unloaded without finishing) retires itself. */
	private int idle;

	private BlockQueue(ServerWorld world) {
		this.world = world;
	}

	/** A running queue, or null when block damage is off (callers then skip all block work). */
	public static BlockQueue start(ServerWorld world) {
		if (!Destruction.enabled(world)) {
			return null;
		}
		BlockQueue q = new BlockQueue(world);
		DetonationManager.add(q);
		return q;
	}

	public void add(BlockPos pos, BlockState state, int mode) {
		ops.add(new Op(pos.asLong(), state, mode));
	}

	public void add(BlockPos pos, BlockState state) {
		add(pos, state, ANY);
	}

	public int pending() {
		return ops.size();
	}

	/** No more edits will be added; the job ends once the queue drains. */
	public void close() {
		open = false;
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public int step(int budget) {
		idle = ops.isEmpty() ? idle + 1 : 0;
		int work = Math.min(budget, PER_TICK);
		int used = 0;
		BlockPos.Mutable p = new BlockPos.Mutable();
		while (used < work && !ops.isEmpty()) {
			Op op = ops.poll();
			used++;
			p.set(op.pos);
			if (p.getY() < world.getBottomY() || p.getY() >= world.getTopY() || !world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
				continue;
			}
			BlockState cur = world.getBlockState(p);
			if (cur == op.state || cur.hasBlockEntity()) {
				continue;
			}
			if (op.mode == AIR && !(cur.isAir() || cur.isReplaceable())) {
				continue;
			}
			if (op.mode == SOLID && (cur.isAir() || cur.isReplaceable())) {
				continue;
			}
			Destruction.set(world, p.toImmutable(), op.state, Kit.FLAGS);
		}
		return budget - used;
	}

	@Override
	public boolean isDone() {
		return ops.isEmpty() && (!open || idle > 6000);
	}
}
