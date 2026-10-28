package com.armageddon.detonation;

import com.armageddon.registry.ModGameRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Spreads huge terrain edits over many ticks so a hydrogen bomb doesn't freeze the server,
 * and runs short delayed actions (implosion → detonation timing, etc).
 */
public final class DetonationManager {
	private static final List<WorldJob> JOBS = new ArrayList<>();
	private static final List<Delayed> DELAYED = new ArrayList<>();
	/** Jobs added while the job list is being stepped (a job that starts another job); merged in after the loop. */
	private static final List<WorldJob> PENDING = new ArrayList<>();
	private static boolean stepping;

	private record Delayed(ServerWorld world, long at, Runnable action) {
	}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(DetonationManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			JOBS.clear();
			DELAYED.clear();
			PENDING.clear();
		});
	}

	public static void add(WorldJob job) {
		// Adding straight to JOBS from inside a job's step would break the iteration and crash the server.
		(stepping ? PENDING : JOBS).add(job);
	}

	public static void schedule(ServerWorld world, int delayTicks, Runnable action) {
		DELAYED.add(new Delayed(world, world.getTime() + delayTicks, action));
	}

	public static boolean isBusy() {
		return !JOBS.isEmpty();
	}

	/** Cancels every pending crater/city job and delayed action. */
	public static int clear() {
		int n = JOBS.size() + DELAYED.size() + PENDING.size();
		JOBS.clear();
		DELAYED.clear();
		PENDING.clear();
		return n;
	}

	private static void tick(ServerWorld world) {
		if (!DELAYED.isEmpty()) {
			List<Delayed> due = new ArrayList<>();
			Iterator<Delayed> it = DELAYED.iterator();
			while (it.hasNext()) {
				Delayed d = it.next();
				if (d.world == world && world.getTime() >= d.at) {
					due.add(d);
					it.remove();
				}
			}
			for (Delayed d : due) {
				try {
					d.action.run();
				} catch (RuntimeException ex) {
					com.armageddon.Armageddon.LOGGER.error("Armageddon delayed action failed", ex);
				}
			}
		}

		if (JOBS.isEmpty()) {
			return;
		}
		int budget = world.getGameRules().getInt(ModGameRules.CRATER_SPEED);
		stepping = true;
		try {
			Iterator<WorldJob> it = JOBS.iterator();
			while (it.hasNext() && budget > 0) {
				WorldJob job = it.next();
				if (job.world() != world) {
					continue;
				}
				try {
					budget = job.step(budget);
				} catch (RuntimeException ex) {
					// A broken job is dropped, never allowed to take the server down with it.
					com.armageddon.Armageddon.LOGGER.error("Armageddon world job {} failed; dropping it", job.getClass().getSimpleName(), ex);
					it.remove();
					continue;
				}
				if (job.isDone()) {
					it.remove();
				}
			}
		} finally {
			stepping = false;
			JOBS.addAll(PENDING);
			PENDING.clear();
		}
	}

	private DetonationManager() {
	}
}
