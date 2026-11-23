package com.armageddon.entity;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Nova bomb payload: a miniature star that rises, hangs in the sky scorching the land below with
 * heat rays, collapses, then goes supernova.
 */
public class StarEntity extends Entity {
	public static final int RISE = 40;
	public static final int BURN = 220;
	public static final int COLLAPSE = 24;
	public static final int LIFETIME = RISE + BURN + COLLAPSE;

	private static final TrackedData<Long> START = DataTracker.registerData(StarEntity.class, TrackedDataHandlerRegistry.LONG);

	private double groundY;
	@Nullable
	private Entity owner;

	public StarEntity(EntityType<? extends StarEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public static void spawn(ServerWorld world, Vec3d pos, @Nullable Entity owner) {
		StarEntity e = new StarEntity(ModEntities.STAR, world);
		e.setPosition(pos.x, pos.y, pos.z);
		e.groundY = pos.y;
		e.owner = owner;
		e.dataTracker.set(START, world.getTime());
		world.spawnEntity(e);
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_CHARGE, SoundCategory.BLOCKS, 12.0F, 0.4F);
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		builder.add(START, 0L);
	}

	public int getAgeTicks() {
		return (int) Math.max(0, getWorld().getTime() - this.dataTracker.get(START));
	}

	/** Visual radius of the photosphere. */
	public float getRadius(float tickDelta) {
		float t = getAgeTicks() + tickDelta;
		if (t < RISE) {
			float f = t / RISE;
			return 6.0F * f * f;
		}
		if (t < RISE + BURN) {
			return 6.0F + 0.4F * MathHelper.sin(t * 0.3F);
		}
		float f = MathHelper.clamp((t - RISE - BURN) / COLLAPSE, 0, 1);
		return 6.0F * (1 - f * f) + 0.4F;
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 1024 * 1024;
	}

	@Override
	public void tick() {
		super.tick();
		int t = getAgeTicks();
		World world = getWorld();
		if (world.isClient) {
			return;
		}
		ServerWorld server = (ServerWorld) world;
		if (t < RISE) {
			setPosition(getX(), groundY + 26.0 * smooth(t / (double) RISE), getZ());
		}
		if (t >= RISE && t < RISE + BURN) {
			if (t % 20 == 0) {
				world.playSound(null, getX(), getY(), getZ(), ModSounds.FLAMETHROWER, SoundCategory.BLOCKS, 8.0F, 0.4F);
			}
			if (t % 2 == 0) {
				heatRay(server);
			}
			if (t % 3 == 0) {
				// Solar prominences: loops of fire thrown off the surface and pulled back in.
				com.armageddon.network.Vfx.play(server, com.armageddon.vfx.VfxIds.NOVA_PROMINENCE, getPos(), 1.0F);
			}
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(getPos(), getPos()).expand(26, 40, 26), LivingEntity::isAlive)) {
				e.setOnFireFor(4.0F);
				if (t % 10 == 0) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, this, owner), 3.0F);
				}
			}
		}
		if (t == RISE + BURN) {
			world.playSound(null, getX(), getY(), getZ(), ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 16.0F, 0.3F);
		}
		if (t >= LIFETIME) {
			Vec3d c = getPos();
			ModNetworking.sendFx(server, c, 4096, new FxPayload(FxType.SUPERNOVA, c, 40, 0xFFF0C0));
			Detonations.nuke(server, new Vec3d(c.x, groundY + 4, c.z), owner, 32);
			discard();
		}
	}

	private static double smooth(double x) {
		return x * x * (3 - 2 * x);
	}

	/** One searing beam to a random point on the ground; melts and ignites what it hits. */
	private void heatRay(ServerWorld world) {
		double a = random.nextDouble() * Math.PI * 2;
		double d = Math.sqrt(random.nextDouble()) * 30;
		int x = MathHelper.floor(getX() + Math.cos(a) * d);
		int z = MathHelper.floor(getZ() + Math.sin(a) * d);
		if (!world.isChunkLoaded(x >> 4, z >> 4)) {
			return;
		}
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
		Vec3d hit = new Vec3d(x + 0.5, y, z + 0.5);
		ModNetworking.sendFx(world, getPos(), 1024, new FxPayload(FxType.HEAT_RAY, getPos(), hit, 0.6F, 0xFFB040, 0));
		com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.NOVA_SCORCH, hit, 1.0F);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(hit, hit).expand(3), LivingEntity::isAlive)) {
			e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, this, owner), 8.0F);
			e.setOnFireFor(8.0F);
		}
		if (!Detonations.blockDamage(world)) {
			return;
		}
		BlockPos below = BlockPos.ofFloored(hit).down();
		BlockState s = world.getBlockState(below);
		if (s.isIn(BlockTags.SAND)) {
			world.setBlockState(below, Blocks.GLASS.getDefaultState(), Block.NOTIFY_ALL);
		} else if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.LOGS)) {
			world.setBlockState(below, Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL);
		} else if (s.getHardness(world, below) >= 0 && s.getHardness(world, below) < 4 && random.nextFloat() < 0.35F && s.isOpaqueFullCube(world, below)) {
			world.setBlockState(below, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
		}
		BlockPos up = below.up();
		if (world.getBlockState(up).isAir() && AbstractFireBlock.canPlaceAt(world, up, Direction.UP)) {
			world.setBlockState(up, AbstractFireBlock.getState(world, up), Block.NOTIFY_ALL);
		}
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
		nbt.putInt("AgeTicks", getAgeTicks());
		nbt.putDouble("GroundY", groundY);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		this.dataTracker.set(START, getWorld().getTime() - nbt.getInt("AgeTicks"));
		this.groundY = nbt.getDouble("GroundY");
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
