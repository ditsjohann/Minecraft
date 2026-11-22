package com.armageddon.entity;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEntities;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared implementation for every rocket, warhead, energy bolt and grenade. Three entity
 * types reuse it so each can get its own renderer (oriented model / glowing bolt / spinning item).
 */
public class WmdProjectileEntity extends ThrownItemEntity {
	private static final TrackedData<Integer> KIND = DataTracker.registerData(WmdProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> STUCK = DataTracker.registerData(WmdProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private int fuse = -1;
	private int homingTarget = -1;
	private int stuckTo = -1;
	private Vec3d stuckOffset = Vec3d.ZERO;
	private boolean detonated;
	/** Free parameter (Midas sphere index, Judgment heat...). */
	private int power;
	/** Optional homing destination (siege missiles). */
	@Nullable
	private Vec3d targetPos;
	private boolean returning;

	public WmdProjectileEntity(EntityType<? extends WmdProjectileEntity> type, World world) {
		super(type, world);
	}

	/** Creates a projectile at the shooter's muzzle, travelling along their look vector. */
	public static WmdProjectileEntity fire(World world, LivingEntity shooter, ProjectileKind kind, float speed, float divergence) {
		EntityType<WmdProjectileEntity> type = kind.isGrenade() ? ModEntities.GRENADE
				: kind.isEnergy() ? ModEntities.ENERGY_BOLT : ModEntities.PROJECTILE;
		WmdProjectileEntity e = new WmdProjectileEntity(type, world);
		e.setOwner(shooter);
		Vec3d look = shooter.getRotationVec(1.0F);
		e.setPosition(shooter.getX() + look.x * 0.8, shooter.getEyeY() - 0.15 + look.y * 0.8, shooter.getZ() + look.z * 0.8);
		e.setKind(kind);
		e.setVelocity(shooter, shooter.getPitch(), shooter.getYaw(), 0.0F, speed, divergence);
		world.spawnEntity(e);
		return e;
	}

	@Override
	public void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(KIND, 0);
		builder.add(STUCK, false);
	}

	public void setKind(ProjectileKind kind) {
		this.dataTracker.set(KIND, kind.ordinal());
		this.setItem(new ItemStack(kind.item()));
		this.fuse = kind.fuse;
	}

	public ProjectileKind getKind() {
		return ProjectileKind.byId(this.dataTracker.get(KIND));
	}

	public boolean isStuck() {
		return this.dataTracker.get(STUCK);
	}

	public void setPower(int power) {
		this.power = power;
	}

	public int getPower() {
		return power;
	}

	public void setTargetPos(@Nullable Vec3d target) {
		this.targetPos = target;
	}

	public void setHomingTarget(@Nullable Entity target) {
		this.homingTarget = target == null ? -1 : target.getId();
	}

	@Override
	public Item getDefaultItem() {
		return ModItems.ROCKET;
	}

	@Override
	public double getGravity() {
		return isStuck() ? 0.0 : getKind().gravity;
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 256 * 256;
	}

	@Override
	public void tick() {
		ProjectileKind kind = getKind();
		if (isStuck()) {
			tickStuck();
		} else {
			super.tick();
		}
		if (this.isRemoved()) {
			return;
		}
		World world = this.getWorld();
		if (world.isClient) {
			spawnTrail(kind);
			return;
		}

		if (kind == ProjectileKind.MIRV && this.age == 14) {
			splitMirv((ServerWorld) world);
			return;
		}
		if (kind == ProjectileKind.ROCKET || kind == ProjectileKind.MIRV) {
			steer(world);
		}
		if (kind == ProjectileKind.BFG && this.age % 3 == 0) {
			zapNearby((ServerWorld) world);
		}
		if (kind == ProjectileKind.BOOMERANG) {
			tickBoomerang((ServerWorld) world);
			return;
		}
		if (targetPos != null) {
			Vec3d to = targetPos.subtract(getPos());
			Vec3d v = getVelocity();
			double speed = Math.min(v.length() * 1.04 + 0.02, 2.8);
			double turn = this.age > 25 ? 0.35 : 0.12;
			Vec3d dir = v.lengthSquared() < 1.0E-6 ? to.normalize() : v.normalize().lerp(to.normalize(), turn).normalize();
			setVelocity(dir.multiply(speed));
			this.velocityDirty = true;
		}
		if (kind == ProjectileKind.MIDAS && this.age == 16) {
			splitMidas((ServerWorld) world);
			return;
		}
		// Gravity fields bend everything that flies through them.
		Vec3d bent = com.armageddon.destruction.EnergyFields.bend((ServerWorld) world, getPos(), getVelocity());
		if (bent != getVelocity()) {
			setVelocity(bent);
			this.velocityDirty = true;
		}
		if (kind.isGrenade()) {
			fuse--;
			if (fuse > 0 && fuse <= 20 && fuse % (fuse <= 8 ? 2 : 5) == 0) {
				world.playSound(null, getX(), getY(), getZ(), ModSounds.BOMB_BEEP, SoundCategory.PLAYERS, 0.8F, 1.4F + (20 - fuse) * 0.03F);
			}
			if (fuse <= 0) {
				detonate(getPos(), null);
			}
		} else if (this.age > (kind.isEnergy() ? 120 : 600)) {
			if (kind == ProjectileKind.SINGULARITY || kind == ProjectileKind.BFG) {
				detonate(getPos(), null);
			} else {
				discard();
			}
		}
	}

	private void tickStuck() {
		this.setVelocity(Vec3d.ZERO);
		if (stuckTo >= 0) {
			Entity host = this.getWorld().getEntityById(stuckTo);
			if (host != null && host.isAlive()) {
				Vec3d p = host.getPos().add(stuckOffset);
				this.setPosition(p.x, p.y, p.z);
			} else {
				stuckTo = -1;
				this.dataTracker.set(STUCK, false);
			}
		}
		this.baseTick();
	}

	private void steer(World world) {
		Vec3d v = getVelocity();
		double speed = Math.min(v.length() * 1.06, 2.6);
		Vec3d dir = v.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : v.normalize();
		if (homingTarget >= 0 && this.age > 3) {
			Entity target = world.getEntityById(homingTarget);
			if (target != null && target.isAlive()) {
				Vec3d desired = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(getPos()).normalize();
				dir = dir.lerp(desired, 0.22).normalize();
			} else {
				homingTarget = -1;
			}
		}
		setVelocity(dir.multiply(speed));
		this.velocityDirty = true;
	}

	/** Returning plasma blade: cuts fragile blocks, hits everything, comes back to the thrower. */
	private void tickBoomerang(ServerWorld world) {
		Entity owner = getOwner();
		if (!returning && (this.age > 14 || getVelocity().lengthSquared() < 0.05)) {
			returning = true;
		}
		if (returning) {
			if (owner == null || !owner.isAlive() || this.age > 200) {
				dropBoomerang(world);
				return;
			}
			Vec3d to = owner.getEyePos().subtract(0, 0.4, 0).subtract(getPos());
			if (to.length() < 1.6) {
				if (owner instanceof net.minecraft.entity.player.PlayerEntity p && !p.getAbilities().creativeMode) {
					if (!p.getInventory().insertStack(new ItemStack(ModItems.PLASMA_BOOMERANG))) {
						dropBoomerang(world);
						return;
					}
				}
				discard();
				return;
			}
			setVelocity(to.normalize().multiply(Math.min(1.8, 0.6 + this.age * 0.03)));
			this.velocityDirty = true;
		}
		// Slice fragile blocks along the path.
		if (com.armageddon.destruction.Destruction.enabled(world)) {
			net.minecraft.util.math.BlockPos bp = getBlockPos();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					net.minecraft.util.math.BlockPos p = bp.add(dx, 0, dz);
					net.minecraft.block.BlockState s = world.getBlockState(p);
					float h = s.getHardness(world, p);
					if (!s.isAir() && h >= 0 && h <= 0.8F && !s.hasBlockEntity()) {
						world.breakBlock(p, true, getOwner());
					}
				}
			}
		}
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(1.2), e -> e != owner && e.isAlive())) {
			if (e.timeUntilRegen <= 10) {
				e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, this, owner), 9.0F);
				ModNetworking.sendFx(world, getPos(), 96, new FxPayload(FxType.SLICE, getPos(), 1.0F, 0x5AC8FF));
			}
		}
	}

	private void dropBoomerang(ServerWorld world) {
		world.spawnEntity(new net.minecraft.entity.ItemEntity(world, getX(), getY(), getZ(), new ItemStack(ModItems.PLASMA_BOOMERANG)));
		discard();
	}

	/** Midas Cascade: splits at apex into seven golden spheres that land in a spiral, each bigger than the last. */
	private void splitMidas(ServerWorld world) {
		Vec3d v = getVelocity();
		Vec3d flat = new Vec3d(v.x, 0, v.z);
		Vec3d fwd = flat.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : flat.normalize();
		Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
		ModNetworking.sendFx(world, getPos(), 512, new FxPayload(FxType.FIREBALL, getPos(), 2.0F, 0xFFC830));
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 6.0F, 0.5F);
		for (int i = 0; i < 7; i++) {
			double a = i * 0.9;
			double r = 0.15 + i * 0.07;
			WmdProjectileEntity b = new WmdProjectileEntity(ModEntities.ENERGY_BOLT, world);
			b.setOwner(getOwner());
			b.setPosition(getX(), getY(), getZ());
			b.setKind(ProjectileKind.MIDAS_SPHERE);
			b.setPower(i);
			Vec3d out = fwd.multiply(Math.cos(a) * r).add(side.multiply(Math.sin(a) * r)).add(fwd.multiply(0.35));
			b.setVelocity(out.x, 0.25 + i * 0.03, out.z);
			world.spawnEntity(b);
		}
		discard();
	}

	/** BFG orb: lashes every living thing within 12 blocks with green lightning as it flies. */
	private void zapNearby(ServerWorld world) {
		Entity owner = getOwner();
		int zapped = 0;
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(12), e -> e.isAlive() && e != owner)) {
			if (zapped++ > 6) {
				break;
			}
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0);
			ModNetworking.sendFx(world, getPos(), 160, new FxPayload(FxType.ARC, getPos(), to, 0.14F, 0x5AFF3A, random.nextInt()));
			e.timeUntilRegen = 0;
			e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION, this, owner), 5.0F);
		}
	}

	private void splitMirv(ServerWorld world) {
		Vec3d v = getVelocity();
		ModNetworking.sendFx(world, getPos(), 256, new FxPayload(FxType.FIREBALL, getPos(), 1.5F, 0xFFCC66));
		world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 4.0F, 0.6F);
		for (int i = 0; i < 10; i++) {
			WmdProjectileEntity b = new WmdProjectileEntity(ModEntities.PROJECTILE, world);
			b.setOwner(getOwner());
			b.setPosition(getX(), getY(), getZ());
			b.setKind(ProjectileKind.BOMBLET);
			double a = (Math.PI * 2 * i) / 10.0;
			b.setVelocity(v.x * 0.55 + Math.cos(a) * 0.35, Math.min(v.y * 0.4, 0.15) - random.nextDouble() * 0.15, v.z * 0.55 + Math.sin(a) * 0.35);
			world.spawnEntity(b);
		}
		discard();
	}

	/**
	 * Set by the client: draws this projectile's trail with the particle engine, returning false for kinds it leaves to
	 * the vanilla particles below.
	 */
	public static java.util.function.Predicate<WmdProjectileEntity> clientTrail = e -> false;

	private void spawnTrail(ProjectileKind kind) {
		if (clientTrail.test(this)) {
			return;
		}
		World w = getWorld();
		Vec3d v = getVelocity();
		Vec3d tail = getPos().subtract(v.lengthSquared() > 1.0E-4 ? v.normalize().multiply(0.6) : Vec3d.ZERO);
		switch (kind) {
			case METEOR -> {
				for (int i = 0; i < 4; i++) {
					w.addImportantParticle(ModParticles.NAPALM_FLAME, true, getX() + random.nextGaussian() * 0.5, getY() + random.nextGaussian() * 0.5,
							getZ() + random.nextGaussian() * 0.5, -v.x * 0.2, -v.y * 0.2, -v.z * 0.2);
				}
				w.addImportantParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, tail.x, tail.y, tail.z, 0, 0.02, 0);
				w.addParticle(ModParticles.EMBER, getX(), getY(), getZ(), random.nextGaussian() * 0.2, random.nextGaussian() * 0.2, random.nextGaussian() * 0.2);
			}
			case ROD -> {
				w.addImportantParticle(ModParticles.PLASMA_SPARK, true, getX(), getY() + 1, getZ(), random.nextGaussian() * 0.1, 0.3, random.nextGaussian() * 0.1);
				w.addImportantParticle(ParticleTypes.CLOUD, true, getX(), getY() + 2, getZ(), 0, 0.1, 0);
			}
			case BFG -> {
				for (int i = 0; i < 4; i++) {
					w.addParticle(ModParticles.RADIATION, getX() + random.nextGaussian(), getY() + random.nextGaussian(), getZ() + random.nextGaussian(), 0, 0, 0);
				}
			}
			case ROCKET, MIRV, MINI_NUKE, BOMBLET, GATLING_NUKE, FALLING_NUKE, AIR_BOMB -> {
				w.addParticle(ParticleTypes.FLAME, tail.x, tail.y, tail.z, -v.x * 0.1, -v.y * 0.1, -v.z * 0.1);
				w.addParticle(ModParticles.EMBER, tail.x, tail.y, tail.z, 0, 0, 0);
				w.addImportantParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, tail.x, tail.y, tail.z, 0, 0.01, 0);
			}
			case PLASMA -> {
				w.addParticle(ModParticles.PLASMA_SPARK, getX(), getY(), getZ(), random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
			}
			case CRYO -> {
				w.addParticle(ModParticles.CRYO_MIST, getX(), getY(), getZ(), 0, 0, 0);
				w.addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
			}
			case SINGULARITY -> {
				for (int i = 0; i < 3; i++) {
					w.addParticle(ModParticles.VOID_SPARK, getX() + random.nextGaussian() * 0.6, getY() + random.nextGaussian() * 0.6, getZ() + random.nextGaussian() * 0.6, 0, 0, 0);
				}
				w.addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 0, 0, 0);
			}
			case PLASMA_GRENADE -> {
				if (age % 2 == 0) {
					w.addParticle(ModParticles.PLASMA_SPARK, getX(), getY() + 0.1, getZ(), 0, 0.05, 0);
				}
			}
			case CRYO_GRENADE -> {
				if (age % 3 == 0) {
					w.addParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.1, getZ(), 0, 0.03, 0);
				}
			}
			case TOXIC -> {
				if (age % 3 == 0) {
					w.addParticle(ModParticles.TOXIC_GAS, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
				}
			}
			case GRAVITY -> w.addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.1, getZ(), 0, 0, 0);
			case POCKET_NUKE -> {
				if (age % 4 == 0) {
					w.addParticle(ModParticles.RADIATION, getX(), getY() + 0.2, getZ(), 0, 0.03, 0);
				}
			}
			default -> {
				if (age % 4 == 0) {
					w.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0.02, 0);
				}
			}
		}
	}

	@Override
	public void onEntityHit(EntityHitResult hit) {
		ProjectileKind kind = getKind();
		Entity target = hit.getEntity();
		if (getWorld().isClient || detonated || kind == ProjectileKind.BOOMERANG) {
			return;
		}
		if (kind.isGrenade()) {
			if (kind.sticky && !isStuck()) {
				stuckTo = target.getId();
				stuckOffset = getPos().subtract(target.getPos());
				this.dataTracker.set(STUCK, true);
				getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.PLAYERS, 1.0F, 1.6F);
			} else if (!kind.sticky) {
				setVelocity(getVelocity().multiply(-0.15, 0.2, -0.15));
			}
			return;
		}
		Entity owner = getOwner();
		switch (kind) {
			case PLASMA -> {
				target.damage(ModDamageTypes.source(getWorld(), ModDamageTypes.PLASMA, this, owner), 9.0F);
				target.setOnFireFor(3.0F);
			}
			case ROCKET, MIRV -> target.damage(ModDamageTypes.source(getWorld(), ModDamageTypes.SHRAPNEL, this, owner), 14.0F);
			case CRYO -> target.damage(ModDamageTypes.source(getWorld(), ModDamageTypes.CRYO, this, owner), 8.0F);
			default -> {
			}
		}
		detonate(hit.getPos(), null);
	}

	@Override
	public void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (getWorld().isClient || detonated) {
			return;
		}
		ProjectileKind kind = getKind();
		if (kind == ProjectileKind.BOOMERANG) {
			returning = true;
			return;
		}
		if (kind.isGrenade()) {
			if (kind.sticky) {
				if (!isStuck()) {
					Vec3d p = hit.getPos();
					setPosition(p.x, p.y, p.z);
					this.dataTracker.set(STUCK, true);
					getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.PLAYERS, 1.0F, 1.6F);
				}
				return;
			}
			bounce(hit);
			return;
		}
		detonate(hit.getPos().add(Vec3d.of(hit.getSide().getVector()).multiply(0.5)), hit);
	}

	private void bounce(BlockHitResult hit) {
		Vec3d v = getVelocity();
		Direction side = hit.getSide();
		Vec3d n = Vec3d.of(side.getVector());
		Vec3d reflected = v.subtract(n.multiply(2 * v.dotProduct(n))).multiply(0.45);
		if (side == Direction.UP && Math.abs(v.y) < 0.12) {
			reflected = new Vec3d(v.x * 0.6, 0, v.z * 0.6);
		}
		Vec3d p = hit.getPos().add(n.multiply(0.06));
		setPosition(p.x, p.y, p.z);
		setVelocity(reflected);
		if (v.lengthSquared() > 0.02) {
			getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.6F, 1.5F);
		}
	}

	private void detonate(Vec3d at, @Nullable BlockHitResult hit) {
		if (detonated || !(getWorld() instanceof ServerWorld world)) {
			return;
		}
		detonated = true;
		Entity owner = getOwner();
		ProjectileKind k = getKind();
		if (k.event != null) {
			com.armageddon.event.EventEntity.start(world, at, k.event, owner, nbt -> {
				nbt.putFloat("Yaw", getYaw());
				nbt.putInt("Power", power);
				nbt.putDouble("Vx", getVelocity().x);
				nbt.putDouble("Vz", getVelocity().z);
			});
			discard();
			return;
		}
		switch (k) {
			case MINI_NUKE -> Detonations.nuke(world, at, owner, 8);
			case POCKET_NUKE -> Detonations.nuke(world, at, owner, 5);
			case ROCKET, MIRV -> {
				Detonations.highExplosive(world, at, this, 4.0F, false);
				Vfx.play(world, VfxIds.GRENADE_BLAST, at, 1.4F);
			}
			case BOMBLET -> Detonations.highExplosive(world, at, this, 2.6F, false);
			case PLASMA -> {
				ModNetworking.sendFx(world, at, 128, new FxPayload(FxType.PLASMA_BURST, at, 1.0F, 0x55FFEE));
				Vfx.play(world, VfxIds.PLASMA_IMPACT, at, getVelocity().multiply(-1), 0.9F, 0x55FFEE);
				world.playSound(null, at.x, at.y, at.z, ModSounds.PLASMA_IMPACT, SoundCategory.PLAYERS, 1.0F, 1.0F + random.nextFloat() * 0.3F);
			}
			case CRYO -> {
				Detonations.freezeBurst(world, at, owner, 5);
				Vfx.play(world, VfxIds.CRYO_BURST, at, 1.3F);
			}
			case SINGULARITY -> Detonations.singularity(world, at, owner, 4.0F, 180, true);
			case FRAG -> {
				Detonations.highExplosive(world, at, this, 2.8F, false);
				Detonations.blast(world, at, 7, 10.0F, ModDamageTypes.SHRAPNEL, owner, 0, 0.4);
			}
			case PLASMA_GRENADE -> {
				ModNetworking.sendFx(world, at, 128, new FxPayload(FxType.PLASMA_BURST, at, 3.0F, 0x55FFEE));
				Detonations.highExplosive(world, at, this, 3.4F, false);
				Detonations.blast(world, at, 5, 12.0F, ModDamageTypes.PLASMA, owner, 5, 0.6);
			}
			case CRYO_GRENADE -> Detonations.freezeBurst(world, at, owner, 6);
			case INCENDIARY -> Detonations.incendiary(world, at, owner, 6);
			case GRAVITY -> Detonations.singularity(world, at, owner, 2.4F, 100, false);
			case TOXIC -> toxicCloud(world, at);
			case METEOR -> Detonations.meteorImpact(world, at, owner);
			case ROD -> Detonations.kineticImpact(world, at, owner);
			case BFG -> {
				Detonations.bfgBurst(world, at, owner);
				Vfx.play(world, VfxIds.BFG_BLAST, at, 2.5F);
			}
			case GATLING_NUKE -> Detonations.nuke(world, at, owner, 3, false);
			case FALLING_NUKE -> Detonations.nuke(world, at, owner, 7, false);
			case AIR_BOMB -> Detonations.highExplosive(world, at, this, 4.5F, true);
			case SIEGE_MISSILE -> Detonations.directional(world, at, getVelocity(), owner, 7.0F);
			case MIDAS_SPHERE -> Detonations.midas(world, at, owner, power);
			case JUDGMENT_BOLT -> Detonations.judgment(world, at, this, power);
			case MICRO_HORIZON -> Detonations.microHorizon(world, at, owner);
			case DEVOURER -> DevourerEntity.spawn(world, at, getVelocity(), owner);
			case MIDAS -> Detonations.midas(world, at, owner, 3);
			default -> {
			}
		}
		discard();
	}

	private void toxicCloud(ServerWorld world, Vec3d at) {
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, at.x, at.y, at.z);
		if (getOwner() instanceof LivingEntity living) {
			cloud.setOwner(living);
		}
		cloud.setParticleType(ModParticles.TOXIC_GAS);
		cloud.setRadius(5.5F);
		cloud.setRadiusGrowth(0.004F);
		cloud.setDuration(400);
		cloud.setWaitTime(0);
		cloud.addEffect(new StatusEffectInstance(StatusEffects.POISON, 160, 2));
		cloud.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
		cloud.addEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
		world.spawnEntity(cloud);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.5F, 0.5F);
		Detonations.blast(world, at, 3, 2.0F, ModDamageTypes.SHRAPNEL, getOwner(), 0, 0.2);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Kind", getKind().ordinal());
		nbt.putInt("Fuse", fuse);
		nbt.putInt("Power", power);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.dataTracker.set(KIND, nbt.getInt("Kind"));
		this.fuse = nbt.getInt("Fuse");
		this.power = nbt.getInt("Power");
	}

	/** Finds the best lock-on target inside a cone in front of the shooter. */
	@Nullable
	public static LivingEntity findLockTarget(LivingEntity shooter, double range, double coneDegrees) {
		Vec3d eye = shooter.getEyePos();
		Vec3d look = shooter.getRotationVec(1.0F);
		double cos = Math.cos(Math.toRadians(coneDegrees));
		LivingEntity best = null;
		double bestScore = -1;
		Box box = shooter.getBoundingBox().expand(range);
		for (LivingEntity e : shooter.getWorld().getEntitiesByClass(LivingEntity.class, box, e -> e != shooter && e.isAlive() && !e.isSpectator())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d > range || d < 1) {
				continue;
			}
			double dot = to.multiply(1.0 / d).dotProduct(look);
			if (dot < cos || !shooter.canSee(e)) {
				continue;
			}
			double score = dot * 2 - d / range;
			if (score > bestScore) {
				bestScore = score;
				best = e;
			}
		}
		return best;
	}
}
