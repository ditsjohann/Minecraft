package com.armageddon.destruction;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Blocks that revert after a while: slime patches, snow piles, EMP-disabled redstone, milk splashes,
 * Chrono Break's erased sphere. Entries live in a queue ordered by expiry, so even hundreds of
 * thousands of them cost O(log n) each. Survives only while the server runs (everything reverts on
 * shutdown).
 */
public final class TemporaryBlocks {
	private record Temp(ServerWorld world, BlockPos pos, BlockState original, BlockState placed, long expires) {
	}

	private static final int REVERTS_PER_TICK = 6000;
	private static final Map<ServerWorld, Map<Long, Temp>> ACTIVE = new HashMap<>();
	private static final PriorityQueue<Temp> QUEUE = new PriorityQueue<>(Comparator.comparingLong(Temp::expires));

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(TemporaryBlocks::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Map<Long, Temp> perWorld : ACTIVE.values()) {
				for (Temp t : perWorld.values()) {
					revert(t);
				}
			}
			ACTIVE.clear();
			QUEUE.clear();
		});
	}

	/**
	 * Places {@code state} at {@code pos} for {@code ticks}, then restores whatever was there.
	 * Re-placing an active position extends it and keeps the original block. Respects protected
	 * regions; returns false if refused.
	 */
	public static boolean place(ServerWorld world, BlockPos pos, BlockState state, int ticks) {
		if (Destruction.isProtected(world, pos)) {
			return false;
		}
		BlockState current = world.getBlockState(pos);
		if (current.getHardness(world, pos) < 0 || current.hasBlockEntity()) {
			return false;
		}
		Map<Long, Temp> perWorld = ACTIVE.computeIfAbsent(world, w -> new HashMap<>());
		Temp existing = perWorld.get(pos.asLong());
		BlockState original = existing != null ? existing.original : current;
		Temp t = new Temp(world, pos.toImmutable(), original, state, world.getTime() + Math.max(1, ticks));
		perWorld.put(pos.asLong(), t);
		QUEUE.add(t);
		world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		return true;
	}

	/** Reverts every temporary block in {@code world} right now (purge). */
	public static int revertAll(ServerWorld world) {
		Map<Long, Temp> perWorld = ACTIVE.remove(world);
		if (perWorld == null) {
			return 0;
		}
		for (Temp t : perWorld.values()) {
			revert(t);
		}
		QUEUE.removeIf(t -> t.world == world);
		return perWorld.size();
	}

	private static void tick(ServerWorld world) {
		if (QUEUE.isEmpty()) {
			return;
		}
		long now = world.getTime();
		List<Temp> otherWorlds = new ArrayList<>();
		int budget = REVERTS_PER_TICK;
		while (budget-- > 0 && !QUEUE.isEmpty() && QUEUE.peek().expires <= now) {
			Temp t = QUEUE.poll();
			if (t.world != world) {
				otherWorlds.add(t);
				continue;
			}
			Map<Long, Temp> perWorld = ACTIVE.get(world);
			// Stale entries (the position was re-placed later) are skipped; only the latest one reverts.
			if (perWorld != null && perWorld.get(t.pos.asLong()) == t) {
				perWorld.remove(t.pos.asLong());
				revert(t);
			}
		}
		QUEUE.addAll(otherWorlds);
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
