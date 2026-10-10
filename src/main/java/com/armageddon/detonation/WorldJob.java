package com.armageddon.detonation;

import net.minecraft.server.world.ServerWorld;

/** A large world edit spread over many ticks. */
public interface WorldJob {
	ServerWorld world();

	/** Spend up to {@code budget} work units; return what is left. */
	int step(int budget);

	boolean isDone();
}
