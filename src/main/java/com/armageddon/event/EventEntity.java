package com.armageddon.event;

import com.armageddon.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/** Invisible host entity that runs one {@link WorldEvent}. */
public class EventEntity extends Entity {
	private String eventId = "";
	private NbtCompound data = new NbtCompound();
	@Nullable
	private WorldEvent event;
	@Nullable
	private UUID ownerUuid;
	@Nullable
	private Entity ownerCache;
	/**
	 * The event's own clock. Not {@link Entity#age}: the world increments that before calling
	 * {@link #tick()}, so it is already 1 on the first tick and every "t == 0" setup step was skipped.
	 */
	private int eventTicks;

	public EventEntity(EntityType<? extends EventEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	/** Starts an event at {@code pos}. {@code params} fills the event's persistent data. */
	public static EventEntity start(ServerWorld world, Vec3d pos, String id, @Nullable Entity owner, @Nullable Consumer<NbtCompound> params) {
		EventEntity e = new EventEntity(ModEntities.WORLD_EVENT, world);
		e.setPosition(pos.x, pos.y, pos.z);
		e.eventId = id;
		if (params != null) {
			params.accept(e.data);
		}
		if (owner != null) {
			e.ownerUuid = owner.getUuid();
			e.ownerCache = owner;
		}
		world.spawnEntity(e);
		return e;
	}

	@Nullable
	public Entity getOwner() {
		if (ownerCache == null && ownerUuid != null && getWorld() instanceof ServerWorld sw) {
			ownerCache = sw.getEntity(ownerUuid);
		}
		return ownerCache;
	}

	public int eventAge() {
		return eventTicks;
	}

	public String getEventId() {
		return eventId;
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		if (event == null) {
			event = Events.create(eventId);
			if (event == null) {
				discard();
				return;
			}
			event.attach(this, world, data);
		}
		boolean finished;
		try {
			finished = event.tick();
			eventTicks++;
		} catch (RuntimeException ex) {
			com.armageddon.Armageddon.LOGGER.error("Armageddon event {} crashed; stopping it", eventId, ex);
			net.minecraft.text.Text msg = net.minecraft.text.Text.literal("[Armageddon] event '" + eventId + "' failed at tick " + eventTicks
					+ ": " + ex.getClass().getSimpleName() + (ex.getMessage() != null ? " - " + ex.getMessage() : "") + " (details in the log)")
					.formatted(net.minecraft.util.Formatting.RED);
			for (net.minecraft.server.network.ServerPlayerEntity p : world.getPlayers()) {
				if (p.squaredDistanceTo(this) < 512 * 512) {
					p.sendMessage(msg, false);
				}
			}
			finished = true;
		}
		if (finished) {
			event.finish();
			discard();
		}
	}

	/** Stops the event immediately (purge command). */
	public void abort() {
		if (event != null) {
			event.finish();
		}
		discard();
	}

	/**
	 * Events keep live state in memory (lifted blocks, frozen mobs, spawned cows), so one reloaded
	 * from disk mid-way could not resume correctly. They are not saved; an unloaded event just ends.
	 */
	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public boolean shouldRender(double distance) {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putString("Event", eventId);
		nbt.put("Data", data);
		nbt.putInt("Age", eventTicks);
		if (ownerUuid != null) {
			nbt.putUuid("Owner", ownerUuid);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		eventId = nbt.getString("Event");
		data = nbt.getCompound("Data");
		eventTicks = nbt.getInt("Age");
		if (nbt.containsUuid("Owner")) {
			ownerUuid = nbt.getUuid("Owner");
		}
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}

}
