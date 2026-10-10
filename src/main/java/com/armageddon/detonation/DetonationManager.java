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
	private static final List<CraterJob> JOBS = new ArrayList<>();
	private static final List<Delayed> DELAYED = new ArrayList<>();

	private record Delayed(ServerWorld world, long at, Runnable action) {
	}

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(DetonationManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			JOBS.clear();
			DELAYED.clear();
		});
	}

	public static void add(CraterJob job) {
		JOBS.add(job);
	}

	public static void schedule(ServerWorld world, int delayTicks, Runnable action) {
		DELAYED.add(new Delayed(world, world.getTime() + delayTicks, action));
	}

	public static boolean isBusy() {
		return !JOBS.isEmpty();
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
			due.forEach(d -> d.action.run());
		}

		if (JOBS.isEmpty()) {
			return;
		}
		int budget = world.getGameRules().getInt(ModGameRules.CRATER_SPEED);
		Iterator<CraterJob> it = JOBS.iterator();
		while (it.hasNext() && budget > 0) {
			CraterJob job = it.next();
			if (job.world != world) {
				continue;
			}
			budget = job.step(budget);
			if (job.isDone()) {
				it.remove();
			}
		}
	}

	private DetonationManager() {
	}
}
