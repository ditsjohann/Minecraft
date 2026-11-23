package com.armageddon.entity;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side timeline for multi-stage events: orbital lasers, lightning storms, kinetic
 * bombardment, meteor showers, carpet bombing, nuke barrages, earthquakes, the Bass Drop and
 * the Omega charge-up.
 */
public class StrikeMarkerEntity extends Entity {
	public static final int ORBITAL = 0;
	public static final int STORM = 1;
	public static final int KINETIC = 2;
	public static final int METEOR = 3;
	public static final int AIRSTRIKE = 4;
	public static final int BARRAGE = 5;
	public static final int TECTONIC = 6;
	public static final int BASS_DROP = 7;
	public static final int OMEGA = 8;
	public static final int ORBITAL_DELAY = 60;
	public static final int OMEGA_CHARGE = 100;

	/** Bass-drop beat times (ticks): accelerating build-up, then the drop. */
	private static final int[] BEATS = {0, 12, 24, 34, 44, 52, 60, 66, 72, 76, 80, 84, 86, 88, 90, 92};
	private static final int DROP_AT = 110;

	private int mode;
	private float heading;
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
		e.heading = owner != null ? owner.getYaw() : world.random.nextFloat() * 360.0F;
		world.spawnEntity(e);
		int lock = switch (mode) {
			case ORBITAL -> 0xFF3322;
			case KINETIC -> 0xFFE070;
			case BARRAGE -> 0x7CFF2E;
			case METEOR -> 0xFF8030;
			default -> 0;
		};
		if (lock != 0) {
			Vfx.play(world, VfxIds.TARGET_LOCK, pos, new Vec3d(0, 1, 0), 1.0F, lock);
		}
		if (mode == STORM) {
			Vfx.play(world, VfxIds.STORM_CLOUDS, pos, 1.0F);
		}
		switch (mode) {
			case ORBITAL -> {
				ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.ORBITAL_TARGET, pos, ORBITAL_DELAY, 0xFF3322));
				world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 6.0F, 0.6F);
			}
			case KINETIC -> {
				ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.ORBITAL_TARGET, pos, 40, 0xFFE070));
				world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 6.0F, 1.4F);
			}
			case BARRAGE -> {
				ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.ORBITAL_TARGET, pos, 40, 0x7CFF2E));
				world.playSound(null, pos.x, pos.y, pos.z, ModSounds.BOMB_ARMED, SoundCategory.PLAYERS, 8.0F, 0.5F);
			}
			case OMEGA -> {
				ModNetworking.sendFx(world, pos, 4096, new FxPayload(FxType.OMEGA_CHARGE, pos, OMEGA_CHARGE, 0xFF3020));
				world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ANTIMATTER, SoundCategory.BLOCKS, 20.0F, 0.4F);
			}
			case AIRSTRIKE -> {
				Vec3d dir = Vec3d.fromPolar(0, e.heading);
				Vec3d from = pos.add(dir.multiply(-220)).add(0, 55, 0);
				Vec3d to = pos.add(dir.multiply(220)).add(0, 55, 0);
				ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.JET_FLYOVER, from, to, 1.0F, 0xFFFFFF, 0));
			}
			default -> {
			}
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
		switch (mode) {
			case ORBITAL -> tickOrbital(world);
			case STORM -> tickStorm(world);
			case KINETIC -> tickKinetic(world);
			case METEOR -> tickMeteors(world);
			case AIRSTRIKE -> tickAirstrike(world);
			case BARRAGE -> tickBarrage(world);
			case TECTONIC -> tickTectonic(world);
			case BASS_DROP -> tickBassDrop(world);
			case OMEGA -> tickOmega(world);
			default -> discard();
		}
	}

	// ------------------------------------------------------------------ strikes

	private void tickOrbital(ServerWorld world) {
		if (this.age == ORBITAL_DELAY - 20) {
			world.playSound(null, getX(), getY(), getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 8.0F, 1.2F);
		}
		if (this.age >= ORBITAL_DELAY) {
			Vfx.play(world, VfxIds.ORBITAL_IMPACT, getPos(), 1.5F);
			Detonations.orbitalStrike(world, getPos(), owner);
			discard();
		}
	}

	private void tickStorm(ServerWorld world) {
		if (this.age >= 8 && this.age <= 70 && this.age % 3 == 0) {
			Vec3d g = groundNear(world, 8.0);
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(g);
				Vfx.play(world, VfxIds.STORM_STRIKE, g, 1.0F);
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

	private void tickKinetic(ServerWorld world) {
		if (this.age >= 30 && this.age <= 62 && (this.age - 30) % 8 == 0) {
			Vec3d g = this.age == 30 ? getPos() : groundNear(world, 9.0);
			dropFromSky(world, ProjectileKind.ROD, g, 130, new Vec3d(0, -7.0, 0));
			world.playSound(null, g.x, g.y, g.z, ModSounds.RAILGUN_FIRE, SoundCategory.PLAYERS, 6.0F, 0.5F);
		}
		if (this.age > 90) {
			discard();
		}
	}

	private void tickMeteors(ServerWorld world) {
		if (this.age >= 10 && this.age <= 130 && this.age % 5 == 0) {
			Vec3d g = groundNear(world, 16.0);
			double a = random.nextDouble() * Math.PI * 2;
			Vec3d drift = new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(0.9);
			Vec3d start = g.subtract(drift.multiply(40)).add(0, 100, 0);
			dropAt(world, ProjectileKind.METEOR, start, drift.add(0, -2.6, 0));
		}
		if (this.age > 160) {
			discard();
		}
	}

	private void tickAirstrike(ServerWorld world) {
		Vec3d dir = Vec3d.fromPolar(0, heading);
		// The jet flies 440 blocks in 44 ticks (10 b/t), starting 220 blocks before the target.
		// Bombs are released under the jet between ticks 18 and 25, leading the target by the
		// distance they drift forward while falling.
		if (this.age >= 18 && this.age <= 25) {
			for (int k = 0; k < 3; k++) {
				double along = -220 + 10.0 * this.age + k * 3.3 - 18;
				Vec3d drop = getPos().add(dir.multiply(along)).add(0, 45, 0);
				for (int side = -1; side <= 1; side += 2) {
					Vec3d off = new Vec3d(-dir.z, 0, dir.x).multiply(side * (2.5 + random.nextDouble() * 2));
					dropAt(world, ProjectileKind.AIR_BOMB, drop.add(off), dir.multiply(0.8).add(0, -0.6, 0));
				}
			}
		}
		if (this.age == 22) {
			world.playSound(null, getX(), getY() + 50, getZ(), ModSounds.JET_FLYBY, SoundCategory.PLAYERS, 16.0F, 0.9F);
		}
		if (this.age > 120) {
			discard();
		}
	}

	private void tickBarrage(ServerWorld world) {
		if (this.age >= 40 && this.age <= 166 && (this.age - 40) % 18 == 0) {
			Vec3d g = this.age == 40 ? getPos() : groundNear(world, 26.0);
			dropFromSky(world, ProjectileKind.FALLING_NUKE, g, 110, new Vec3d(0, -3.2, 0));
		}
		if (this.age > 200) {
			discard();
		}
	}

	// ------------------------------------------------------------------ bombs

	/** Earthquake: an expanding ring of upheaval that throws the ground into the air, plus fissures. */
	private void tickTectonic(ServerWorld world) {
		int duration = 140;
		double maxR = 42;
		if (this.age % 10 == 0) {
			ModNetworking.sendFx(world, getPos(), 512, new FxPayload(FxType.QUAKE, getPos(), (float) maxR, 0xA08060));
			world.playSound(null, getX(), getY(), getZ(), ModSounds.NUKE_RUMBLE, SoundCategory.BLOCKS, 10.0F, 0.5F + random.nextFloat() * 0.2F);
		}
		double r = maxR * this.age / duration;
		if (this.age % 5 == 0 && r > 2) {
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.TECTONIC_FRONT, getPos(), (float) (r / 20.0));
		}
		boolean grief = Detonations.blockDamage(world);
		if (this.age % 3 == 0) {
			int n = (int) (6 + r * 0.9);
			for (int i = 0; i < n; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double rr = r + random.nextGaussian() * 1.5;
				int x = MathHelper.floor(getX() + Math.cos(a) * rr);
				int z = MathHelper.floor(getZ() + Math.sin(a) * rr);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
				BlockPos p = new BlockPos(x, y, z);
				BlockState state = world.getBlockState(p);
				if (grief && !state.isAir() && state.getHardness(world, p) >= 0 && state.getHardness(world, p) < 20 && !state.hasBlockEntity() && state.getFluidState().isEmpty()) {
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p, state);
					fb.setVelocity(Math.cos(a) * 0.15, 0.6 + random.nextDouble() * 0.7, Math.sin(a) * 0.15);
					fb.velocityModified = true;
				}
			}
			// Fling everything near the wavefront.
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(getPos(), getPos()).expand(r + 3), LivingEntity::isAlive)) {
				double d = Math.sqrt(e.squaredDistanceTo(getX(), e.getY(), getZ()));
				if (Math.abs(d - r) < 3 && e.isOnGround()) {
					e.addVelocity(random.nextGaussian() * 0.3, 0.9 + random.nextDouble() * 0.5, random.nextGaussian() * 0.3);
					e.velocityModified = true;
					e.damage(world.getDamageSources().fall(), 4.0F);
				}
			}
		}
		// Fissures: five cracks racing outward.
		if (grief && this.age % 2 == 0) {
			for (int k = 0; k < 5; k++) {
				double a = (Math.PI * 2 * k / 5) + Math.sin(this.age * 0.15 + k) * 0.25 + heading * 0.0174;
				int x = MathHelper.floor(getX() + Math.cos(a) * r);
				int z = MathHelper.floor(getZ() + Math.sin(a) * r);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
				for (int dy = 1; dy <= 9; dy++) {
					BlockPos p = new BlockPos(x, top - dy, z);
					BlockState s = world.getBlockState(p);
					if (s.getHardness(world, p) < 0) {
						break;
					}
					world.setBlockState(p, dy == 9 && random.nextFloat() < 0.6F ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
				}
			}
		}
		if (this.age >= duration) {
			discard();
		}
	}

	/** Bass Drop: accelerating rainbow bass pulses, then the drop. */
	private void tickBassDrop(ServerWorld world) {
		for (int i = 0; i < BEATS.length; i++) {
			if (BEATS[i] == this.age) {
				float radius = 10 + i * 2.5F;
				ModNetworking.sendFx(world, getPos(), 512, new FxPayload(FxType.BASS_PULSE, getPos(), getPos(), radius, 0xFFFFFF, i));
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.BASS_THUMP, getPos(), new Vec3d(0, 1, 0), radius / 10.0F,
						java.awt.Color.HSBtoRGB(i * 0.13F, 0.75F, 1.0F) & 0xFFFFFF);
				world.playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 6.0F, 0.5F + (i % 4) * 0.12F);
				world.playSound(null, getX(), getY(), getZ(), ModSounds.DUBSTEP_WUB, SoundCategory.BLOCKS, 6.0F, 0.6F + i * 0.04F);
				for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(getPos(), getPos()).expand(radius), LivingEntity::isAlive)) {
					Vec3d d = e.getPos().subtract(getPos());
					double len = Math.max(0.5, d.length());
					e.addVelocity(d.x / len * 0.4, 0.5, d.z / len * 0.4);
					e.velocityModified = true;
					e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, this, owner), 2.0F);
				}
			}
		}
		if (this.age == DROP_AT - 18) {
			world.playSound(null, getX(), getY(), getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.BLOCKS, 10.0F, 2.0F);
		}
		if (this.age == DROP_AT) {
			Vec3d c = getPos();
			ModNetworking.sendFx(world, c, 1024, new FxPayload(FxType.BASS_DROP, c, 48.0F, 0xFFFFFF));
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.DROP_FINALE, c, 1.0F);
			world.playSound(null, c.x, c.y, c.z, ModSounds.DUBSTEP_DROP, SoundCategory.BLOCKS, 16.0F, 0.6F);
			world.playSound(null, c.x, c.y, c.z, ModSounds.NUKE_DETONATE, SoundCategory.BLOCKS, 16.0F, 0.8F);
			world.createExplosion(null, c.x, c.y, c.z, 12.0F, false, Detonations.blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
			Detonations.blast(world, c, 48, 60.0F, ModDamageTypes.SHRAPNEL, owner, 0, 4.5);
			Detonations.debris(world, c, 14, 90);
			discard();
		}
	}

	/** Omega charge: pull the world inwards and lift debris into a spiral, then crack the planet. */
	private void tickOmega(ServerWorld world) {
		Vec3d c = getPos();
		if (this.age % 5 == 0) {
			Detonations.pull(world, c, 90, 0.35);
		}
		if (this.age % 20 == 0) {
			world.playSound(null, c.x, c.y, c.z, ModSounds.BLACK_HOLE, SoundCategory.BLOCKS, 16.0F, 0.3F + this.age * 0.006F);
		}
		if (Detonations.blockDamage(world) && this.age % 2 == 0 && this.age < OMEGA_CHARGE - 10) {
			for (int i = 0; i < 4; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double rr = 10 + random.nextDouble() * 50;
				int x = MathHelper.floor(c.x + Math.cos(a) * rr);
				int z = MathHelper.floor(c.z + Math.sin(a) * rr);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				BlockPos p = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
				BlockState state = world.getBlockState(p);
				if (!state.isAir() && state.getHardness(world, p) >= 0 && state.getHardness(world, p) < 20 && !state.hasBlockEntity() && state.getFluidState().isEmpty()) {
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p, state);
					fb.setDestroyedOnLanding();
					fb.setNoGravity(true);
					fb.setVelocity(-Math.sin(a) * 0.3, 0.25 + random.nextDouble() * 0.2, Math.cos(a) * 0.3);
					fb.velocityModified = true;
				}
			}
		}
		if (this.age >= OMEGA_CHARGE) {
			Detonations.omega(world, c, owner);
			discard();
		}
	}

	// ------------------------------------------------------------------ helpers

	private Vec3d groundNear(ServerWorld world, double spread) {
		double a = random.nextDouble() * Math.PI * 2;
		double d = Math.sqrt(random.nextDouble()) * spread;
		int x = MathHelper.floor(getX() + Math.cos(a) * d);
		int z = MathHelper.floor(getZ() + Math.sin(a) * d);
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
		return new Vec3d(x + 0.5, y, z + 0.5);
	}

	private void dropFromSky(ServerWorld world, ProjectileKind kind, Vec3d ground, double height, Vec3d velocity) {
		double top = Math.min(ground.y + height, world.getTopY() - 2);
		dropAt(world, kind, new Vec3d(ground.x, top, ground.z), velocity);
	}

	private void dropAt(ServerWorld world, ProjectileKind kind, Vec3d at, Vec3d velocity) {
		WmdProjectileEntity p = new WmdProjectileEntity(ModEntities.PROJECTILE, world);
		p.setOwner(owner);
		p.setKind(kind);
		p.setPosition(at.x, at.y, at.z);
		p.setVelocity(velocity);
		world.spawnEntity(p);
	}

	@Override
	public boolean shouldRender(double distance) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("Mode", mode);
		nbt.putInt("Age", age);
		nbt.putFloat("Heading", heading);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		mode = nbt.getInt("Mode");
		age = nbt.getInt("Age");
		heading = nbt.getFloat("Heading");
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry) {
		return new EntitySpawnS2CPacket(this, entry);
	}
}
