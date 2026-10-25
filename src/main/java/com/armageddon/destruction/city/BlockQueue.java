package com.armageddon.destruction.city;

import com.armageddon.destruction.Destruction;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.WorldJob;
import com.armageddon.event.Kit;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

/**
 * An ordered queue of block writes for one event, applied at a fixed rate so even a moon landing
 * never stalls the server. Every write honours the block-damage rule, protected regions, unloaded
 * chunks and block entities. The event drains it each tick; anything left when the event ends is
 * handed to the {@link DetonationManager} to finish.
 */
public final class BlockQueue implements WorldJob {
	/** Write unconditionally. */
	public static final byte ANY = 0;
	/** Only into air or replaceable blocks (grass, snow layers...): builds without destroying. */
	public static final byte AIR_ONLY = 1;
	/** Only over existing solid blocks: repaints terrain without filling the sky. */
	public static final byte SOLID_ONLY = 2;
	/** Only into non-air: carving, without wasting writes on air. */
	public static final byte NOT_AIR = 3;

	public static final int PER_TICK = 2500;

	private final ServerWorld world;
	private long[] pos = new long[1024];
	private BlockState[] states = new BlockState[1024];
	private byte[] modes = new byte[1024];
	private int head, size;
	private final BlockPos.Mutable cursor = new BlockPos.Mutable();

	public BlockQueue(ServerWorld world) {
		this.world = world;
	}

	public void add(BlockPos p, BlockState s, byte mode) {
		add(p.getX(), p.getY(), p.getZ(), s, mode);
	}

	public void add(int x, int y, int z, BlockState s, byte mode) {
		if (size == pos.length) {
			if (head > pos.length / 2) {
				// Compact instead of growing once most of the queue is already spent.
				System.arraycopy(pos, head, pos, 0, size - head);
				System.arraycopy(states, head, states, 0, size - head);
				System.arraycopy(modes, head, modes, 0, size - head);
				Arrays.fill(states, size - head, size, null);
				size -= head;
				head = 0;
			} else {
				int n = pos.length * 2;
				pos = Arrays.copyOf(pos, n);
				states = Arrays.copyOf(states, n);
				modes = Arrays.copyOf(modes, n);
			}
		}
		pos[size] = BlockPos.asLong(x, y, z);
		states[size] = s;
		modes[size] = mode;
		size++;
	}

	public int pending() {
		return size - head;
	}

	/** Applies up to {@code max} queued writes (checks of skipped positions count a quarter). */
	public int drain(int max) {
		if (!Destruction.enabled(world)) {
			head = size;
			return 0;
		}
		int done = 0;
		int work = 0;
		while (head < size && work < max * 4) {
			long l = pos[head];
			BlockState s = states[head];
			byte mode = modes[head];
			states[head] = null;
			head++;
			cursor.set(l);
			work++;
			if (world.isOutOfHeightLimit(cursor) || !world.isChunkLoaded(cursor.getX() >> 4, cursor.getZ() >> 4)) {
				continue;
			}
			BlockState old = world.getBlockState(cursor);
			if (old.hasBlockEntity() || old == s) {
				continue;
			}
			boolean ok = switch (mode) {
				case AIR_ONLY -> old.isAir() || old.isReplaceable();
				case SOLID_ONLY -> !old.isAir() && old.getFluidState().isEmpty() && !old.isReplaceable();
				case NOT_AIR -> !old.isAir();
				default -> true;
			};
			if (ok && Destruction.set(world, cursor.toImmutable(), s, Kit.FLAGS)) {
				done++;
				work += 3;
			}
		}
		if (head == size) {
			head = size = 0;
		}
		return done;
	}

	/** Lets the manager finish whatever the event did not get to. */
	public void handOff() {
		if (pending() > 0) {
			DetonationManager.add(this);
		}
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public int step(int budget) {
		int n = Math.min(budget, PER_TICK);
		drain(n);
		return budget - n;
	}

	@Override
	public boolean isDone() {
		return pending() == 0;
	}
}
