package com.armageddon.destruction;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Blocks that revert after a while: slime patches, snow piles, EMP-disabled redstone, glowing
 * void boundaries. Survives only while the server runs (everything reverts on shutdown).
 */
public final class TemporaryBlocks {
	private record Temp(ServerWorld world, BlockPos pos, BlockState original, BlockState placed, long expires) {
	}

	private static final Map<Long, Temp> ACTIVE = new HashMap<>();
	private static final List<Temp> ORDER = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(TemporaryBlocks::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Temp t : ORDER) {
				revert(t);
			}
			ORDER.clear();
			ACTIVE.clear();
		});
	}

	/**
	 * Places {@code state} at {@code pos} for {@code ticks}, then restores whatever was there.
	 * Respects protected regions; returns false if refused.
	 */
	public static boolean place(ServerWorld world, BlockPos pos, BlockState state, int ticks) {
		if (Destruction.isProtected(world, pos)) {
			return false;
		}
		BlockState current = world.getBlockState(pos);
		if (current.getHardness(world, pos) < 0 || current.hasBlockEntity()) {
			return false;
		}
		long key = pos.asLong() ^ ((long) world.getRegistryKey().getValue().hashCode() << 40);
		Temp existing = ACTIVE.get(key);
		BlockState original = existing != null ? existing.original : current;
		Temp t = new Temp(world, pos.toImmutable(), original, state, world.getTime() + ticks);
		if (existing != null) {
			ORDER.remove(existing);
		}
		ACTIVE.put(key, t);
		ORDER.add(t);
		world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		return true;
	}

	/** Reverts every temporary block in {@code world} right now (purge). */
	public static int revertAll(ServerWorld world) {
		int n = 0;
		Iterator<Temp> it = ORDER.iterator();
		while (it.hasNext()) {
			Temp t = it.next();
			if (t.world == world) {
				revert(t);
				it.remove();
				ACTIVE.remove(t.pos.asLong() ^ ((long) world.getRegistryKey().getValue().hashCode() << 40));
				n++;
			}
		}
		return n;
	}

	private static void tick(ServerWorld world) {
		if (ORDER.isEmpty()) {
			return;
		}
		long now = world.getTime();
		Iterator<Temp> it = ORDER.iterator();
		int budget = 4000;
		while (it.hasNext() && budget-- > 0) {
			Temp t = it.next();
			if (t.world != world || t.expires > now) {
				continue;
			}
			revert(t);
			it.remove();
			ACTIVE.remove(t.pos.asLong() ^ ((long) world.getRegistryKey().getValue().hashCode() << 40));
		}
	}

	private static void revert(Temp t) {
		// Only revert if nobody changed the block since (avoid erasing player builds).
		if (t.world.getBlockState(t.pos) == t.placed) {
			t.world.setBlockState(t.pos, t.original, Block.NOTIFY_ALL);
		}
	}

	private TemporaryBlocks() {
	}
}
