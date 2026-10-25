package com.armageddon.destruction.celestial2;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.WorldJob;
import com.armageddon.event.Kit;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;

/**
 * A budgeted queue of block edits for the Celestial II events: everything goes through {@link Destruction#set}
 * (block-damage rule, protected regions, unbreakables), skips block entities and unloaded chunks, and never
 * spends more than {@link #PER_TICK} edits in one tick. Edits with a lifetime revert on their own.
 */
public final class BlockQueue implements WorldJob {
	public static final int PER_TICK = 2500;

	private record Edit(long pos, BlockState state, int revert) {
	}

	private final ServerWorld world;
	private final ArrayDeque<Edit> edits = new ArrayDeque<>();
	private boolean registered;

	public BlockQueue(ServerWorld world) {
		this.world = world;
	}

	public void put(BlockPos pos, BlockState state) {
		put(pos, state, 0);
	}

	/** Queues an edit; {@code revertTicks > 0} makes it temporary. */
	public void put(BlockPos pos, BlockState state, int revertTicks) {
		if (!Destruction.enabled(world)) {
			return;
		}
		edits.addLast(new Edit(pos.asLong(), state, revertTicks));
		if (!registered) {
			registered = true;
			DetonationManager.add(this);
		}
	}

	public int size() {
		return edits.size();
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public int step(int budget) {
		int n = Math.min(budget, PER_TICK);
		BlockPos.Mutable p = new BlockPos.Mutable();
		while (n > 0 && !edits.isEmpty()) {
			Edit e = edits.pollFirst();
			n--;
			p.set(e.pos());
			if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) || world.isOutOfHeightLimit(p)) {
				continue;
			}
			BlockState old = world.getBlockState(p);
			if (old.hasBlockEntity() || old == e.state()) {
				continue;
			}
			if (e.revert() > 0) {
				if (!Destruction.isProtected(world, p)) {
					TemporaryBlocks.place(world, p.toImmutable(), e.state(), e.revert());
				}
			} else {
				Destruction.set(world, p.toImmutable(), e.state(), Kit.FLAGS);
			}
		}
		if (edits.isEmpty()) {
			registered = false;
		}
		return budget - (Math.min(budget, PER_TICK) - n);
	}

	@Override
	public boolean isDone() {
		return edits.isEmpty();
	}
}
