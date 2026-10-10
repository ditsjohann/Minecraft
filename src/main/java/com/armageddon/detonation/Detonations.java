package com.armageddon.detonation;

import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.entity.FalloutCloudEntity;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.registry.ModEntities;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModEffects;
import com.armageddon.registry.ModGameRules;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.ArmorSets;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** The actual payloads. Every WMD in the mod funnels into one of these. */
public final class Detonations {
	/** How far (blocks) nuclear-scale visual effects are broadcast. */
	public static final double STRATEGIC_FX_RANGE = 1536;

	public static boolean blockDamage(ServerWorld world) {
		return world.getGameRules().getBoolean(ModGameRules.BLOCK_DAMAGE);
	}

	// ------------------------------------------------------------------ strategic

	/** Fission/fusion warhead. radius = crater radius in blocks (blast reaches ~3x). */
	public static void nuke(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		nuke(world, pos, cause, radius, true);
	}

	public static void nuke(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius, boolean leaveFallout) {
		ModNetworking.sendFx(world, pos, STRATEGIC_FX_RANGE, new FxPayload(FxType.NUKE, pos, radius, 0xFFDD88));
		if (radius >= 8) {
			debris(world, pos, radius * 1.1, Math.min(140, radius * 3));
		}
		world.emitGameEvent(cause, net.minecraft.world.event.GameEvent.EXPLODE, pos);

		blast(world, pos, radius * 3.2, 40.0F + radius * 5.0F, ModDamageTypes.NUCLEAR_BLAST, cause, radius * 2.4, 3.5);
		irradiate(world, pos, radius * 3.0, radius > 30 ? 2 : 1, 900);

		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius, CraterJob.Mode.NUKE));
		}
		if (leaveFallout && world.getGameRules().getBoolean(ModGameRules.FALLOUT)) {
			int minutes = radius >= 40 ? 10 : radius >= 15 ? 5 : 2;
			FalloutCloudEntity.spawn(world, pos, radius * 2.6F, 20 * 60 * minutes, radius >= 40 ? 2 : radius >= 15 ? 1 : 0);
		}
	}

	public static void antimatter(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		ModNetworking.sendFx(world, pos, STRATEGIC_FX_RANGE, new FxPayload(FxType.ANTIMATTER, pos, radius, 0xEE88FF));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ANTIMATTER, SoundCategory.BLOCKS, 16.0F, 0.6F);
		// Implosion first (pull), then annihilation.
		pull(world, pos, radius * 2.5, 1.2);
		DetonationManager.schedule(world, 28, () -> {
			blast(world, pos, radius * 2.2, 500.0F, ModDamageTypes.ANNIHILATION, cause, 0, 4.0);
			if (blockDamage(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius, CraterJob.Mode.ANTIMATTER));
			}
		});
	}

	public static void neutron(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		ModNetworking.sendFx(world, pos, STRATEGIC_FX_RANGE, new FxPayload(FxType.NEUTRON, pos, radius, 0x66FF44));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.NUKE_DETONATE, SoundCategory.BLOCKS, 12.0F, 1.4F);
		blast(world, pos, 8, 30, ModDamageTypes.NUCLEAR_BLAST, cause, 4, 1.5);
		Box box = new Box(pos, pos).expand(radius);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
			double d = e.getPos().distanceTo(pos);
			if (d > radius) {
				continue;
			}
			if (ArmorSets.isRadiationSealed(e)) {
				e.addStatusEffect(new StatusEffectInstance(ModEffects.RADIATION, 200, 0));
				continue;
			}
			float dose = (float) (1.0 - d / radius);
			e.damage(ModDamageTypes.source(world, ModDamageTypes.RADIATION, null, cause), 20.0F + dose * 400.0F);
			e.addStatusEffect(new StatusEffectInstance(ModEffects.RADIATION, 2400, dose > 0.5F ? 3 : 2));
		}
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius * 0.7F, CraterJob.Mode.NEUTRON));
		}
		if (world.getGameRules().getBoolean(ModGameRules.FALLOUT)) {
			FalloutCloudEntity.spawn(world, pos, radius * 0.6F, 20 * 90, 1);
		}
	}

	public static void cryo(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		ModNetworking.sendFx(world, pos, Math.max(256, radius * 8), new FxPayload(FxType.CRYO_WAVE, pos, radius, 0x99E6FF));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.FREEZE, SoundCategory.BLOCKS, Math.max(4.0F, radius / 4.0F), 0.7F);
		Box box = new Box(pos, pos).expand(radius);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
			double d = e.getPos().distanceTo(pos);
			if (d > radius) {
				continue;
			}
			float f = (float) (1.0 - d / radius);
			e.extinguish();
			e.setFrozenTicks(Math.max(e.getFrozenTicks(), 300 + (int) (f * 600)));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100 + (int) (f * 300), f > 0.5F ? 5 : 3));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 2));
			e.damage(ModDamageTypes.source(world, ModDamageTypes.CRYO, null, cause), 3.0F + 14.0F * f);
		}
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius, CraterJob.Mode.CRYO));
		}
	}

	public static void singularity(ServerWorld world, Vec3d pos, @Nullable Entity cause, float radius, int lifetime, boolean ripBlocks) {
		BlackHoleEntity.spawn(world, pos, radius, lifetime, ripBlocks, cause);
	}

	public static void orbitalStrike(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.ORBITAL_BEAM, pos, 4.5F, 0x9FE8FF));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_STRIKE, SoundCategory.PLAYERS, 12.0F, 0.8F);
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 8.0F, 0.5F);
		blast(world, pos, 14, 70.0F, ModDamageTypes.PLASMA, cause, 9, 2.2);
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), 4.5F, CraterJob.Mode.SHAFT));
		}
	}


	// ------------------------------------------------------------------ insane tier

	/** "Planet Cracker": antimatter-boosted thermonuclear device. Called after its charge-up sequence. */
	public static void omega(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		int radius = 78;
		ModNetworking.sendFx(world, pos, 4096, new FxPayload(FxType.OMEGA, pos, radius, 0xFF3020));
		ModNetworking.sendFx(world, pos, 4096, new FxPayload(FxType.NUKE, pos, radius, 0xFFDD88));
		debris(world, pos, radius, 220);
		blast(world, pos, radius * 3.5, 2000.0F, ModDamageTypes.ANNIHILATION, cause, radius * 3.0, 6.0);
		irradiate(world, pos, radius * 4.0, 3, 2400);
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius, CraterJob.Mode.NUKE));
		}
		if (world.getGameRules().getBoolean(ModGameRules.FALLOUT)) {
			FalloutCloudEntity.spawn(world, pos, radius * 2.8F, 20 * 60 * 15, 3);
		}
	}

	/** Heavy missile impact that gouges along its flight direction. */
	public static void directional(ServerWorld world, Vec3d pos, Vec3d velocity, @Nullable Entity cause, float radius) {
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.LAYERED_BLAST, pos.x, pos.y, pos.z, pos.x, pos.y, pos.z, radius, 0xB070FF, 0, 0, 0, 0));
		blast(world, pos, radius * 2.5, 40.0F, ModDamageTypes.NUCLEAR_BLAST, cause, radius, 1.8);
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), radius, CraterJob.Mode.DIRECTIONAL).withAxis(velocity));
		}
		debris(world, pos, radius, 20);
	}

	/** One of the Midas Cascade's escalating golden spheres. */
	public static void midas(ServerWorld world, Vec3d pos, @Nullable Entity cause, int index) {
		float size = 2.0F + index * 0.9F;
		ModNetworking.sendFx(world, pos, 768, new FxPayload(FxType.LAYERED_BLAST, pos.x, pos.y, pos.z, pos.x, pos.y, pos.z, size, 0xFFC830, 0, 0, 0, 0));
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 4.0F + index, 1.3F - index * 0.1F);
		blast(world, pos, size * 2.5, 10.0F + index * 6, ModDamageTypes.NUCLEAR_BLAST, cause, size, 1.2);
		if (index >= 6) {
			if (blockDamage(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), 12, CraterJob.Mode.GLASSED)
						.withLining(Blocks.YELLOW_STAINED_GLASS.getDefaultState()));
			}
			ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.DOME, pos.x, pos.y, pos.z, pos.x, pos.y, pos.z, 40, 0xFFD060, 0, 0, 0, 40));
		} else {
			com.armageddon.destruction.Destruction.explode(world, null, pos.x, pos.y, pos.z, 2.0F + index * 0.6F, false);
		}
		com.armageddon.destruction.EnergyFields.emit(world, com.armageddon.destruction.EnergyFields.Energy.SOLAR, pos, size * 1.5, 100, cause);
	}

	/** Judgment Engine plasma bolt; hotter barrels hit harder. */
	public static void judgment(ServerWorld world, Vec3d pos, @Nullable Entity source, int heat) {
		float power = 1.4F + heat / 45.0F;
		ModNetworking.sendFx(world, pos, 256, new FxPayload(FxType.FIREBALL, pos, power, 0xFFB020));
		com.armageddon.destruction.Destruction.explode(world, source, pos.x, pos.y, pos.z, power, false);
		com.armageddon.destruction.EnergyFields.emit(world, com.armageddon.destruction.EnergyFields.Energy.SOLAR, pos, 3, 60, source);
	}

	/** Micro Event Horizon: small, hungry, short-lived. */
	public static void microHorizon(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		BlackHoleEntity.spawn(world, pos, 3.0F, 150, true, cause, 160);
		com.armageddon.destruction.EnergyFields.emit(world, com.armageddon.destruction.EnergyFields.Energy.GRAVITY, pos, 16, 150, cause);
	}

	/** Cluster nuke: pops, then flings eight warheads that rain down in a ring. */
	public static void clusterNuke(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		highExplosive(world, pos, cause, 4.0F, false);
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.NUKE_LAUNCH, SoundCategory.BLOCKS, 10.0F, 0.6F);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + world.random.nextDouble() * 0.3;
			double speed = 0.7 + world.random.nextDouble() * 0.5;
			WmdProjectileEntity p = new WmdProjectileEntity(ModEntities.PROJECTILE, world);
			p.setOwner(cause);
			p.setKind(ProjectileKind.FALLING_NUKE);
			p.setPosition(pos.x, pos.y + 1.5, pos.z);
			p.setVelocity(Math.cos(a) * speed, 1.4 + world.random.nextDouble() * 0.4, Math.sin(a) * speed);
			world.spawnEntity(p);
		}
		if (world.getGameRules().getBoolean(ModGameRules.FALLOUT)) {
			FalloutCloudEntity.spawn(world, pos, 60, 20 * 60 * 5, 1);
		}
	}

	/** Meteor strike: fiery crater, burning splatter, shockwave. */
	public static void meteorImpact(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.FIREBALL, pos, 6.0F, 0xFF5010));
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.SHOCKWAVE, pos, 18.0F, 0xFF9050));
		world.createExplosion(null, pos.x, pos.y, pos.z, 5.0F, true, blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
		blast(world, pos, 10, 30.0F, ModDamageTypes.NUCLEAR_BLAST, cause, 8, 1.6);
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), 4.5F, CraterJob.Mode.NUKE));
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int i = 0; i < 10; i++) {
				p.set(pos.x + world.random.nextGaussian() * 3, pos.y - 1 - world.random.nextInt(3), pos.z + world.random.nextGaussian() * 3);
				if (!world.getBlockState(p).isAir() && world.getBlockState(p).getHardness(world, p) >= 0) {
					world.setBlockState(p, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
				}
			}
		}
		debris(world, pos, 5, 18);
	}

	/** Tungsten rod at Mach 10: deep narrow shaft, enormous shock. */
	public static void kineticImpact(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		ModNetworking.sendFx(world, pos, 1024, new FxPayload(FxType.KINETIC_IMPACT, pos, 10.0F, 0xFFF0D0));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.ORBITAL_STRIKE, SoundCategory.BLOCKS, 10.0F, 0.6F);
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 10.0F, 0.4F);
		blast(world, pos, 22, 80.0F, ModDamageTypes.NUCLEAR_BLAST, cause, 4, 3.0);
		if (blockDamage(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), 3.0F, CraterJob.Mode.SHAFT));
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(pos), 7.0F, CraterJob.Mode.NUKE));
		}
		debris(world, pos, 7, 40);
	}

	/** BFG orb impact: green annihilation wave that arcs to everything in range. */
	public static void bfgBurst(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.BFG_BURST, pos, 9.0F, 0x5AFF3A));
		world.playSound(null, pos.x, pos.y, pos.z, ModSounds.BLACK_HOLE_COLLAPSE, SoundCategory.PLAYERS, 6.0F, 1.4F);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(24), LivingEntity::isAlive)) {
			if (e == cause || e.getPos().distanceTo(pos) > 24) {
				continue;
			}
			ModNetworking.sendFx(world, pos, 256, new FxPayload(FxType.ARC, pos, e.getPos().add(0, e.getHeight() / 2, 0), 0.2F, 0x5AFF3A, world.random.nextInt()));
			e.timeUntilRegen = 0;
			e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, cause), 40.0F);
		}
		world.createExplosion(null, pos.x, pos.y, pos.z, 6.0F, false, blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
	}

	/**
	 * Hurls real blocks out of the ground around a blast as flying debris. Blocks come from the
	 * terrain surface on a ring and are launched outward and upward; they land as rubble.
	 */
	public static void debris(ServerWorld world, Vec3d center, double radius, int count) {
		if (!blockDamage(world)) {
			return;
		}
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int i = 0; i < count; i++) {
			double a = world.random.nextDouble() * Math.PI * 2;
			double r = radius * (0.8 + world.random.nextDouble() * 0.6);
			int x = MathHelper.floor(center.x + Math.cos(a) * r);
			int z = MathHelper.floor(center.z + Math.sin(a) * r);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			p.set(x, y, z);
			BlockState state = world.getBlockState(p);
			float hardness = state.getHardness(world, p);
			if (state.isAir() || hardness < 0 || hardness > 20 || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
				continue;
			}
			FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), state);
			double speed = 0.6 + world.random.nextDouble() * (0.6 + radius / 40.0);
			fb.setVelocity(Math.cos(a) * speed, 0.8 + world.random.nextDouble() * (0.8 + radius / 50.0), Math.sin(a) * speed);
			fb.velocityModified = true;
			if (world.random.nextFloat() < 0.5F) {
				fb.setDestroyedOnLanding();
			}
		}
	}

	// ------------------------------------------------------------------ tactical

	public static void thermobaric(ServerWorld world, Vec3d pos, @Nullable Entity cause, float power) {
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.FIREBALL, pos, power, 0xFF6622));
		ModNetworking.sendFx(world, pos, 512, new FxPayload(FxType.SHOCKWAVE, pos, power * 3.0F, 0xFFAA66));
		world.createExplosion(cause, pos.x, pos.y, pos.z, power, true, blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
		blast(world, pos, power * 2.5, power * 2.0F, ModDamageTypes.NUCLEAR_BLAST, cause, power * 2.5, 1.0);
		// Vacuum effect: suffocate anything nearby.
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(power * 2), LivingEntity::isAlive)) {
			e.setAir(Math.min(e.getAir(), 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
		}
		if (blockDamage(world)) {
			ignite(world, pos, (int) (power * 1.6F), 0.35F);
		}
	}

	public static void demolition(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		highExplosive(world, pos, cause, 6.0F, false);
	}

	public static void highExplosive(ServerWorld world, Vec3d pos, @Nullable Entity cause, float power, boolean fire) {
		ModNetworking.sendFx(world, pos, 256, new FxPayload(FxType.FIREBALL, pos, power, 0xFF9944));
		if (power >= 4.0F) {
			ModNetworking.sendFx(world, pos, 256, new FxPayload(FxType.SHOCKWAVE, pos, power * 2.0F, 0xFFCC88));
		}
		world.createExplosion(cause, pos.x, pos.y, pos.z, power, fire, blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
	}

	public static void incendiary(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		ModNetworking.sendFx(world, pos, 256, new FxPayload(FxType.FIREBALL, pos, radius * 0.8F, 0xFF5500));
		world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 3.0F, 0.6F);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(radius), LivingEntity::isAlive)) {
			if (e.getPos().distanceTo(pos) <= radius) {
				e.setOnFireFor(10.0F);
				e.damage(world.getDamageSources().onFire(), 4.0F);
			}
		}
		ignite(world, pos, radius, 0.7F);
	}

	public static void freezeBurst(ServerWorld world, Vec3d pos, @Nullable Entity cause, int radius) {
		cryo(world, pos, cause, radius);
	}

	// ------------------------------------------------------------------ helpers

	/** Radial blast damage with quadratic falloff, ignition and knockback. Vaporises dropped items near ground zero. */
	public static void blast(ServerWorld world, Vec3d c, double range, float maxDamage, RegistryKey<DamageType> type,
			@Nullable Entity cause, double fireRange, double knock) {
		Box box = new Box(c, c).expand(range);
		for (Entity e : world.getOtherEntities(cause instanceof PlayerEntity ? null : cause, box)) {
			double d = e.getPos().distanceTo(c);
			if (d > range || !e.isAlive()) {
				continue;
			}
			double f = 1.0 - d / range;
			if (e instanceof ItemEntity || e instanceof ExperienceOrbEntity) {
				if (d < range * 0.35) {
					e.discard();
					continue;
				}
			}
			if (e instanceof LivingEntity living) {
				float dmg = (float) Math.max(1.0, maxDamage * f * f);
				if (ArmorSets.hasFullJuggernaut(living)) {
					dmg *= 0.15F;
				}
				living.damage(ModDamageTypes.source(world, type, null, cause), dmg);
				if (d < fireRange) {
					living.setOnFireFor((float) (4 + 8 * f));
				}
			}
			Vec3d dir = e.getPos().subtract(c);
			dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0, 1, 0) : dir.normalize();
			double k = knock * f;
			e.addVelocity(dir.x * k, Math.max(0.25, dir.y + 0.35) * k, dir.z * k);
			e.velocityModified = true;
		}
	}

	public static void pull(ServerWorld world, Vec3d c, double range, double strength) {
		for (Entity e : world.getOtherEntities(null, new Box(c, c).expand(range))) {
			Vec3d dir = c.subtract(e.getPos());
			double d = dir.length();
			if (d < 0.5 || d > range) {
				continue;
			}
			e.addVelocity(dir.multiply(strength * (1.0 - d / range) / d));
			e.velocityModified = true;
		}
	}

	public static void irradiate(ServerWorld world, Vec3d c, double range, int amplifier, int duration) {
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(range), LivingEntity::isAlive)) {
			if (e.getPos().distanceTo(c) <= range && !ArmorSets.isRadiationSealed(e)) {
				e.addStatusEffect(new StatusEffectInstance(ModEffects.RADIATION, duration, amplifier));
			}
		}
	}

	/** Sets fire to exposed surfaces in a radius. */
	public static void ignite(ServerWorld world, Vec3d c, int radius, float chance) {
		BlockPos center = BlockPos.ofFloored(c);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius || world.random.nextFloat() > chance) {
					continue;
				}
				for (int dy = radius / 2; dy >= -radius / 2; dy--) {
					p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (world.getBlockState(p).isAir() && AbstractFireBlock.canPlaceAt(world, p, Direction.UP)) {
						world.setBlockState(p, AbstractFireBlock.getState(world, p), Block.NOTIFY_ALL);
						break;
					}
				}
			}
		}
	}

	/** Spherical falloff helper. */
	public static float falloff(double distance, double range) {
		return (float) MathHelper.clamp(1.0 - distance / range, 0.0, 1.0);
	}

	private Detonations() {
	}
}
