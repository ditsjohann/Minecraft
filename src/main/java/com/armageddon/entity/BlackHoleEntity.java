package com.armageddon.entity;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModSounds;
import com.armageddon.block.BombBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A miniature black hole. Spins up, drags in entities, tears blocks out of the ground and
 * swallows them, then collapses in a burst of Hawking radiation.
 */
public class BlackHoleEntity extends Entity {
	private static final TrackedData<Float> RADIUS = DataTracker.registerData(BlackHoleEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(BlackHoleEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** World time at spawn, synced so late-joining clients animate the correct phase. */
	private static final TrackedData<Long> START = DataTracker.registerData(BlackHoleEntity.class, TrackedDataHandlerRegistry.LONG);

	private boolean ripBlocks;
	/** Max blocks this hole may tear out (Integer.MAX_VALUE = unlimited). */
	private int blockBudget = Integer.MAX_VALUE;
	@Nullable
	private Entity owner;

	public BlackHoleEntity(EntityType<? extends BlackHoleEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public static void spawn(ServerWorld world, Vec3d pos, float radius, int lifetime, boolean ripBlocks, @Nullable Entity owner, int blockBudget) {
		spawn(world, pos, radius, lifetime, ripBlocks, owner).blockBudget = blockBudget;
	}

	public static BlackHoleEntity spawn(ServerWorld world, Vec3d pos, float radius, int lifetime, boolean ripBlocks, @Nullable Entity owner) {
		BlackHoleEntity e = new BlackHoleEntity(ModEntities.BLACK_HOLE, world);
		e.setPosition(pos.x, pos.y, pos.z);
		e.dataTracker.set(RADIUS, radius);
		e.dataTracker.set(LIFETIME, lifetime);
		e.dataTracker.set(START, world.getTime());
		e.ripBlocks = ripBlocks;
		e.owner = owner;
		world.spawnEntity(e);
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 2.0F + radius * 0.4F, 0.5F);
		return e;
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		builder.add(RADIUS, 4.0F);
		builder.add(LIFETIME, 200);
		builder.add(START, 0L);
	}

	/** Ticks since this black hole formed (consistent on server and every client). */
	public int getAgeTicks() {
		return (int) Math.max(0, getWorld().getTime() - this.dataTracker.get(START));
	}

	/** The last tick the client renderer drew this hole's infall for; client-side bookkeeping only. */
	public int fxTick = -1;

	public float getBaseRadius() {
		return this.dataTracker.get(RADIUS);
	}

	public int getLifetime() {
		return this.dataTracker.get(LIFETIME);
	}

	/** Event-horizon scale over the hole's life: spin-up, plateau, collapse. */
	public float getStrength(float tickDelta) {
		float t = getAgeTicks() + tickDelta;
		float life = getLifetime();
		float grow = MathHelper.clamp(t / 30.0F, 0.0F, 1.0F);
		float shrink = MathHelper.clamp((life - t) / 20.0F, 0.0F, 1.0F);
		return grow * grow * (3 - 2 * grow) * shrink;
	}

	public float getCurrentRadius(float tickDelta) {
		return getBaseRadius() * getStrength(tickDelta);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 512 * 512;
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		float r = getCurrentRadius(0);
		Vec3d c = getPos();

		if (world.isClient) {
			// The infall is drawn by the renderer with the particle engine.
			return;
		}

		ServerWorld server = (ServerWorld) world;
		if (getAgeTicks() >= getLifetime()) {
			collapse(server);
			return;
		}
		if (this.age % 40 == 0) {
			world.playSound(null, c.x, c.y, c.z, ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 1.5F + r * 0.3F, 0.4F + random.nextFloat() * 0.1F);
		}

		double reach = r * 4.0 + 2.0;
		for (Entity e : world.getOtherEntities(this, new Box(c, c).expand(reach))) {
			if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) {
				continue;
			}
			Vec3d to = c.subtract(e.getPos().add(0, e.getHeight() * 0.5, 0));
			double d = to.length();
			if (d > reach) {
				continue;
			}
			if (d < Math.max(0.8, r * 0.45)) {
				if (e instanceof FallingBlockEntity || e instanceof ItemEntity || e instanceof ExperienceOrbEntity || e instanceof WmdProjectileEntity) {
					e.discard();
					continue;
				}
				if (e instanceof LivingEntity living && this.age % 4 == 0) {
					living.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, this, owner), 6.0F + r);
				}
			}
			double pull = (0.06 + 0.02 * r) * (1.0 - d / reach) + 0.6 / Math.max(d * d, 1.0);
			Vec3d dir = to.multiply(1.0 / Math.max(d, 0.01));
			// Slight swirl for style.
			Vec3d swirl = new Vec3d(-dir.z, 0, dir.x).multiply(pull * 0.35);
			Vec3d v = e.getVelocity().multiply(e instanceof FallingBlockEntity ? 0.9 : 0.96).add(dir.multiply(pull)).add(swirl);
			e.setVelocity(v);
			e.velocityModified = true;
			e.fallDistance = 0;
		}

		if (ripBlocks && Detonations.blockDamage(server) && r > 1.0F) {
			ripBlocks(server, c, r);
		}
	}

	private void ripBlocks(ServerWorld world, Vec3d c, float r) {
		int flying = world.getEntitiesByClass(FallingBlockEntity.class, new Box(c, c).expand(r * 4), e -> true).size();
		if (flying > 90) {
			return;
		}
		int attempts = 2 + (int) r;
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int i = 0; i < attempts; i++) {
			double range = r * 1.8;
			p.set(c.x + (random.nextDouble() * 2 - 1) * range, c.y + (random.nextDouble() * 2 - 1) * range, c.z + (random.nextDouble() * 2 - 1) * range);
			if (p.getSquaredDistance(c) > range * range) {
				continue;
			}
			BlockState state = world.getBlockState(p);
			float hardness = state.getHardness(world, p);
			if (state.isAir() || hardness < 0 || hardness > 30 || state.hasBlockEntity() || state.getBlock() instanceof BombBlock) {
				continue;
			}
			if (!state.getFluidState().isEmpty()) {
				world.setBlockState(p, net.minecraft.block.Blocks.AIR.getDefaultState());
				continue;
			}
			if (blockBudget-- <= 0 || com.armageddon.destruction.Destruction.isProtected(world, p)) {
				continue;
			}
			FallingBlockEntity falling = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), state);
			falling.setDestroyedOnLanding();
			Vec3d dir = c.subtract(Vec3d.ofCenter(p)).normalize();
			falling.setVelocity(dir.multiply(0.3).add(0, 0.35, 0));
		}
	}

	private void collapse(ServerWorld world) {
		Vec3d c = getPos();
		float r = getBaseRadius();
		ModNetworking.sendFx(world, c, 512, new FxPayload(FxType.COLLAPSE, c, r, 0xCC88FF));
		world.playSound(null, c.x, c.y, c.z, ModSounds.BLACK_HOLE_COLLAPSE, SoundCategory.BLOCKS, 4.0F + r, 0.7F);
		Detonations.blast(world, c, r * 3, 8.0F + r * 2, ModDamageTypes.ANNIHILATION, owner, 0, 2.5);
		discard();
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
		nbt.putFloat("Radius", getBaseRadius());
		nbt.putInt("Lifetime", getLifetime());
		nbt.putBoolean("Rip", ripBlocks);
		nbt.putInt("AgeTicks", getAgeTicks());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		this.dataTracker.set(RADIUS, nbt.getFloat("Radius"));
		this.dataTracker.set(LIFETIME, nbt.getInt("Lifetime"));
		this.ripBlocks = nbt.getBoolean("Rip");
		this.dataTracker.set(START, getWorld().getTime() - nbt.getInt("AgeTicks"));
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
