package com.armageddon.entity;

import com.armageddon.registry.ModEffects;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModParticles;
import com.armageddon.util.ArmorSets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Invisible radioactive zone left after a nuclear detonation. Irradiates anything unprotected
 * inside it and fills the air with glowing motes and drifting ash on the client.
 */
public class FalloutCloudEntity extends Entity {
	private static final TrackedData<Float> RADIUS = DataTracker.registerData(FalloutCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(FalloutCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> LEVEL = DataTracker.registerData(FalloutCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Long> START = DataTracker.registerData(FalloutCloudEntity.class, TrackedDataHandlerRegistry.LONG);

	public FalloutCloudEntity(EntityType<? extends FalloutCloudEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public static void spawn(ServerWorld world, Vec3d pos, float radius, int lifetime, int level) {
		FalloutCloudEntity e = new FalloutCloudEntity(ModEntities.FALLOUT_CLOUD, world);
		e.setPosition(pos.x, pos.y, pos.z);
		e.dataTracker.set(RADIUS, radius);
		e.dataTracker.set(LIFETIME, lifetime);
		e.dataTracker.set(LEVEL, level);
		e.dataTracker.set(START, world.getTime());
		world.spawnEntity(e);
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		builder.add(RADIUS, 30.0F);
		builder.add(LIFETIME, 6000);
		builder.add(LEVEL, 0);
		builder.add(START, 0L);
	}

	public int getAgeTicks() {
		return (int) Math.max(0, getWorld().getTime() - this.dataTracker.get(START));
	}

	public float getRadius() {
		return this.dataTracker.get(RADIUS);
	}

	public int getLevel() {
		return this.dataTracker.get(LEVEL);
	}

	/** 0..1 decay over lifetime. */
	public float getPotency() {
		return MathHelper.clamp(1.0F - getAgeTicks() / (float) this.dataTracker.get(LIFETIME), 0.0F, 1.0F);
	}

	/** Dose rate at a point, used by the Geiger counter HUD. Arbitrary "Sv/h". */
	public double doseAt(Vec3d p) {
		double d = Math.sqrt(p.squaredDistanceTo(getX(), p.y, getZ()));
		float r = getRadius();
		if (d > r) {
			return 0;
		}
		return (1.0 - d / r) * (1.5 + getLevel() * 2.0) * getPotency();
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		float r = getRadius();
		if (world.isClient) {
			clientEffects(world, r);
			return;
		}
		if (getAgeTicks() >= this.dataTracker.get(LIFETIME)) {
			discard();
			return;
		}
		if (this.age % 20 == 0) {
			Box box = new Box(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r * 1.5, getZ() + r);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
				double dose = doseAt(e.getPos());
				if (dose <= 0.05 || ArmorSets.isRadiationSealed(e)) {
					continue;
				}
				int amp = dose > 3.0 ? 2 : dose > 1.2 ? 1 : 0;
				StatusEffectInstance current = e.getStatusEffect(ModEffects.RADIATION);
				int duration = 200 + (int) (dose * 100);
				if (current == null || current.getAmplifier() < amp || current.getDuration() < duration / 2) {
					e.addStatusEffect(new StatusEffectInstance(ModEffects.RADIATION, duration, amp, false, true, true));
				}
			}
		}
	}

	private void clientEffects(World world, float r) {
		float potency = getPotency();
		int count = (int) (6 * potency) + 1;
		for (int i = 0; i < count; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(random.nextDouble()) * r;
			double x = getX() + Math.cos(a) * d;
			double z = getZ() + Math.sin(a) * d;
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
			double y = top + random.nextDouble() * 4.0;
			world.addParticle(ModParticles.RADIATION, x, y, z, 0, 0.01 + random.nextDouble() * 0.02, 0);
			if (random.nextInt(2) == 0) {
				world.addParticle(ParticleTypes.WHITE_ASH, x, y + 6 + random.nextDouble() * 10, z, 0, -0.05, 0);
			}
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
	public boolean shouldRender(double distance) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putFloat("Radius", getRadius());
		nbt.putInt("Lifetime", this.dataTracker.get(LIFETIME));
		nbt.putInt("Level", getLevel());
		nbt.putInt("AgeTicks", getAgeTicks());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		this.dataTracker.set(RADIUS, nbt.getFloat("Radius"));
		this.dataTracker.set(LIFETIME, nbt.getInt("Lifetime"));
		this.dataTracker.set(LEVEL, nbt.getInt("Level"));
		this.dataTracker.set(START, getWorld().getTime() - nbt.getInt("AgeTicks"));
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
