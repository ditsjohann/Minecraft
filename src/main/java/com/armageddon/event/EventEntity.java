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
		} catch (RuntimeException ex) {
			com.armageddon.Armageddon.LOGGER.error("Armageddon event {} crashed; stopping it", eventId, ex);
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
		nbt.putInt("Age", age);
		if (ownerUuid != null) {
			nbt.putUuid("Owner", ownerUuid);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		eventId = nbt.getString("Event");
		data = nbt.getCompound("Data");
		age = nbt.getInt("Age");
		if (nbt.containsUuid("Owner")) {
			ownerUuid = nbt.getUuid("Owner");
		}
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}

}
