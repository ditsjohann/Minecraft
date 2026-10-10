package com.armageddon.event;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A scripted, multi-phase event (a weapon's windup, warning, impact, shockwave, debris and
 * aftermath). Hosted by an {@link EventEntity} so it ticks with the world and survives saves.
 */
public abstract class WorldEvent {
	protected EventEntity host;
	protected ServerWorld world;
	/** Persistent parameters (radius, direction...). Saved with the host. */
	protected NbtCompound data;

	void attach(EventEntity host, ServerWorld world, NbtCompound data) {
		this.host = host;
		this.world = world;
		this.data = data;
	}

	protected Vec3d center() {
		return host.getPos();
	}

	@Nullable
	protected Entity owner() {
		return host.getOwner();
	}

	protected int age() {
		return host.age;
	}

	/** Advances the event by one tick. Return true when finished. */
	public abstract boolean tick();

	/** Called once when the event ends or is purged — release frozen entities, etc. */
	public void finish() {
	}
}
