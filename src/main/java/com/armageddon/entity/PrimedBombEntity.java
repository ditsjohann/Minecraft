package com.armageddon.entity;

import com.armageddon.block.BombBlock;
import com.armageddon.block.BombType;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** An armed warhead counting down — the WMD equivalent of primed TNT. */
public class PrimedBombEntity extends Entity {
	private static final TrackedData<Integer> FUSE = DataTracker.registerData(PrimedBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> TYPE = DataTracker.registerData(PrimedBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> FACING = DataTracker.registerData(PrimedBombEntity.class, TrackedDataHandlerRegistry.INTEGER);

	@Nullable
	private Entity causingEntity;

	public PrimedBombEntity(EntityType<? extends PrimedBombEntity> type, World world) {
		super(type, world);
	}

	public static void spawn(World world, BlockPos pos, BombType type, BlockState state, @Nullable Entity cause, int fuse) {
		PrimedBombEntity e = new PrimedBombEntity(ModEntities.PRIMED_BOMB, world);
		e.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		e.setBombType(type);
		e.setFuse(fuse);
		if (state.contains(BombBlock.FACING)) {
			e.dataTracker.set(FACING, state.get(BombBlock.FACING).getHorizontal());
		}
		e.causingEntity = cause;
		world.spawnEntity(e);
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		builder.add(FUSE, 80);
		builder.add(TYPE, 0);
		builder.add(FACING, 2);
	}

	public int getFuse() {
		return this.dataTracker.get(FUSE);
	}

	public void setFuse(int fuse) {
		this.dataTracker.set(FUSE, fuse);
	}

	public BombType getBombType() {
		return BombType.byId(this.dataTracker.get(TYPE));
	}

	public void setBombType(BombType type) {
		this.dataTracker.set(TYPE, type.ordinal());
	}

	public BlockState getBlockState() {
		return ModBlocks.BOMBS.get(getBombType()).getDefaultState()
				.with(BombBlock.FACING, Direction.fromHorizontal(this.dataTracker.get(FACING)));
	}

	@Override
	public double getGravity() {
		return 0.04;
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	@Override
	public MoveEffect getMoveEffect() {
		return MoveEffect.NONE;
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 160 * 160;
	}

	@Override
	public void tick() {
		this.applyGravity();
		this.move(MovementType.SELF, this.getVelocity());
		this.setVelocity(this.getVelocity().multiply(0.98));
		if (this.isOnGround()) {
			this.setVelocity(this.getVelocity().multiply(0.7, -0.5, 0.7));
		}
		int fuse = getFuse() - 1;
		setFuse(fuse);
		BombType type = getBombType();
		World world = this.getWorld();
		if (fuse <= 0) {
			this.discard();
			if (world instanceof ServerWorld server) {
				type.detonate(server, this.getPos().add(0, 0.5, 0), causingEntity);
			}
			return;
		}
		this.updateWaterState();
		if (world.isClient) {
			world.addParticle(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 0, 0.02, 0);
			if (type.isStrategic()) {
				if (random.nextInt(3) == 0) {
					world.addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + random.nextGaussian() * 0.3, getY() + 0.9, getZ() + random.nextGaussian() * 0.3, 0, 0.05, 0);
				}
				if (type == BombType.ATOMIC || type == BombType.HYDROGEN || type == BombType.NEUTRON) {
					world.addParticle(ModParticles.RADIATION, getX() + random.nextGaussian() * 0.5, getY() + 0.5, getZ() + random.nextGaussian() * 0.5, 0, 0.03, 0);
				}
			}
		} else {
			// Beep faster as detonation approaches.
			int interval = fuse > 100 ? 20 : fuse > 40 ? 10 : fuse > 15 ? 5 : 2;
			if (fuse % interval == 0) {
				float pitch = 0.8F + (1.0F - Math.min(1.0F, fuse / (float) type.fuse)) * 1.2F;
				world.playSound(null, getX(), getY(), getZ(), ModSounds.BOMB_BEEP, SoundCategory.BLOCKS, type.isStrategic() ? 3.0F : 1.2F, pitch);
			}
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putShort("Fuse", (short) getFuse());
		nbt.putByte("BombType", (byte) getBombType().ordinal());
		nbt.putByte("Facing", (byte) (int) this.dataTracker.get(FACING));
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		setFuse(nbt.getShort("Fuse"));
		this.dataTracker.set(TYPE, (int) nbt.getByte("BombType"));
		this.dataTracker.set(FACING, (int) nbt.getByte("Facing"));
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
