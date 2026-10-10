package com.armageddon.entity;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
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
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Devourer Core: a mobile gravitational anomaly that crawls across the land eating terrain,
 * growing with every block it absorbs, until it implodes.
 */
public class DevourerEntity extends Entity {
	private static final TrackedData<Float> RADIUS = DataTracker.registerData(DevourerEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final int LIFETIME = 520;

	private Vec3d heading = new Vec3d(1, 0, 0);
	private int consumed;
	@Nullable
	private Entity owner;

	public DevourerEntity(EntityType<? extends DevourerEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public static void spawn(ServerWorld world, Vec3d pos, Vec3d velocity, @Nullable Entity owner) {
		DevourerEntity e = new DevourerEntity(ModEntities.DEVOURER, world);
		e.setPosition(pos.x, pos.y + 1.5, pos.z);
		Vec3d flat = new Vec3d(velocity.x, 0, velocity.z);
		e.heading = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : flat.normalize();
		e.owner = owner;
		world.spawnEntity(e);
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 6.0F, 0.3F);
		Vfx.skyTint(world, pos, 512, 0x500808, 0.5F);
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		builder.add(RADIUS, 2.0F);
	}

	public float getRadius() {
		return this.dataTracker.get(RADIUS);
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 512 * 512;
	}

	@Override
	public void tick() {
		super.tick();
		World world = getWorld();
		float r = getRadius();
		if (world.isClient) {
			for (int i = 0; i < 4 + r; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double d = r * (2.5 + random.nextDouble() * 2);
				double vx = -Math.sin(a) * 0.2 - Math.cos(a) * 0.15;
				double vz = Math.cos(a) * 0.2 - Math.sin(a) * 0.15;
				world.addParticle(ModParticles.EMBER, getX() + Math.cos(a) * d, getY() + random.nextGaussian() * r * 0.3, getZ() + Math.sin(a) * d, vx, 0, vz);
			}
			return;
		}
		ServerWorld server = (ServerWorld) world;
		if (this.age >= LIFETIME || consumed > 2600) {
			implode(server);
			return;
		}
		// Crawl forward, hugging the terrain surface.
		int top = server.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(getX()), MathHelper.floor(getZ()));
		double targetY = Math.max(top - r * 0.4, getY() - 0.3);
		Vec3d next = getPos().add(heading.multiply(0.13)).add(0, (targetY - getY()) * 0.05, 0);
		setPosition(next.x, next.y, next.z);

		if (this.age % 30 == 0) {
			server.playSound(null, getX(), getY(), getZ(), ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 4.0F + r, 0.25F);
		}
		if (Destruction.enabled(server)) {
			eat(server, r);
		}
		double reach = r * 4 + 4;
		for (Entity e : server.getOtherEntities(this, new Box(getPos(), getPos()).expand(reach))) {
			if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) {
				continue;
			}
			Vec3d to = getPos().subtract(e.getPos());
			double d = to.length();
			if (d < r * 0.8) {
				if (e instanceof FallingBlockEntity || e instanceof ItemEntity) {
					e.discard();
					continue;
				}
				if (e instanceof LivingEntity l && this.age % 5 == 0) {
					l.damage(ModDamageTypes.source(server, ModDamageTypes.ANNIHILATION, this, owner), 8.0F + r);
				}
			}
			if (d > 0.1 && d < reach) {
				e.setVelocity(e.getVelocity().multiply(0.95).add(to.multiply((0.08 + r * 0.02) / d)));
				e.velocityModified = true;
				e.fallDistance = 0;
			}
		}
	}

	private void eat(ServerWorld world, float r) {
		ProtectedRegions regions = ProtectedRegions.get(world);
		BlockPos c = getBlockPos();
		int ri = MathHelper.ceil(r);
		BlockPos.Mutable p = new BlockPos.Mutable();
		int eaten = 0;
		for (int i = 0; i < 60 && eaten < 30; i++) {
			p.set(c.getX() + random.nextInt(ri * 2 + 1) - ri, c.getY() + random.nextInt(ri * 2 + 1) - ri, c.getZ() + random.nextInt(ri * 2 + 1) - ri);
			if (p.getSquaredDistance(c) > r * r) {
				continue;
			}
			BlockState s = world.getBlockState(p);
			if (s.isAir() || s.getHardness(world, p) < 0 || (!regions.isEmpty() && regions.isProtected(p))) {
				continue;
			}
			Destruction.record(world, p, s);
			if (random.nextInt(6) == 0 && s.getFluidState().isEmpty() && !s.hasBlockEntity()) {
				// Some blocks are visibly ripped in from further out.
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
				fb.setDestroyedOnLanding();
				fb.setNoGravity(true);
			} else {
				world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
			eaten++;
		}
		consumed += eaten;
		this.dataTracker.set(RADIUS, Math.min(8.0F, 2.0F + consumed / 330.0F));
	}

	private void implode(ServerWorld world) {
		Vec3d c = getPos();
		float r = getRadius();
		Vfx.orb(world, c, r * 1.5F, 0.2F, 0x200000, 1, 20);
		Vfx.layeredBlast(world, c, r * 3, 0xFF2020);
		Vfx.dome(world, c, r * 10, 0xFF4040, 40);
		Vfx.shake(world, c, 400, 3.0F);
		world.playSound(null, c.x, c.y, c.z, ModSounds.BLACK_HOLE_COLLAPSE, SoundCategory.BLOCKS, 16.0F, 0.4F);
		Detonations.blast(world, c, r * 6, 60.0F + r * 10, ModDamageTypes.ANNIHILATION, owner, r * 4, 4.0);
		if (Detonations.blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(c), r * 2.6F, CraterJob.Mode.NUKE));
		}
		Detonations.debris(world, c, r * 2.5, 120);
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
		nbt.putFloat("Radius", getRadius());
		nbt.putInt("Consumed", consumed);
		nbt.putDouble("Hx", heading.x);
		nbt.putDouble("Hz", heading.z);
		nbt.putInt("Age", age);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		this.dataTracker.set(RADIUS, nbt.getFloat("Radius"));
		consumed = nbt.getInt("Consumed");
		heading = new Vec3d(nbt.getDouble("Hx"), 0, nbt.getDouble("Hz"));
		age = nbt.getInt("Age");
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
