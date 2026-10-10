package com.armageddon.entity;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Server-side timer for delayed strikes: orbital lasers and called-in lightning storms. */
public class StrikeMarkerEntity extends Entity {
	public static final int ORBITAL = 0;
	public static final int STORM = 1;
	public static final int ORBITAL_DELAY = 60;

	private int mode;
	@Nullable
	private Entity owner;

	public StrikeMarkerEntity(EntityType<? extends StrikeMarkerEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public static void spawn(ServerWorld world, Vec3d pos, int mode, @Nullable Entity owner) {
		StrikeMarkerEntity e = new StrikeMarkerEntity(ModEntities.STRIKE_MARKER, world);
		e.setPosition(pos.x, pos.y, pos.z);
		e.mode = mode;
		e.owner = owner;
		world.spawnEntity(e);
		if (mode == ORBITAL) {
			ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.ORBITAL_TARGET, pos, ORBITAL_DELAY, 0xFF3322));
			world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 6.0F, 0.6F);
		}
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
		if (mode == ORBITAL) {
			if (this.age == ORBITAL_DELAY - 20) {
				world.playSound(null, getX(), getY(), getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 8.0F, 1.2F);
			}
			if (this.age >= ORBITAL_DELAY) {
				Detonations.orbitalStrike(world, getPos(), owner);
				discard();
			}
		} else {
			if (this.age >= 8 && this.age <= 70 && this.age % 3 == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				double d = random.nextDouble() * 8.0;
				int x = (int) Math.floor(getX() + Math.cos(a) * d);
				int z = (int) Math.floor(getZ() + Math.sin(a) * d);
				int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
				LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
				if (bolt != null) {
					bolt.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(new BlockPos(x, y, z)));
					if (owner instanceof ServerPlayerEntity player) {
						bolt.setChanneler(player);
					}
					world.spawnEntity(bolt);
				}
			}
			if (this.age > 72) {
				discard();
			}
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("Mode", mode);
		nbt.putInt("Age", age);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		mode = nbt.getInt("Mode");
		age = nbt.getInt("Age");
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
