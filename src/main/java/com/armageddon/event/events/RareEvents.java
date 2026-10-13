package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.item.GunItem;
import com.armageddon.item.TargetingItem;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Blue tier: tactical control and localised terrain effects. */
public final class RareEvents {
	private RareEvents() {
	}

	/** 11 Shatterfrost: frost build-up → ice shell → crystalline shatterwave. */
	public static class Shatterfrost extends WorldEvent {
		private final List<BlockPos> shell = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			BlockPos b = BlockPos.ofFloored(c);
			if (t == 0) {
				Vfx.spiral(world, c, 6.0F, 5.0F, 0xBFEFFF, 40);
				Kit.sound(world, c, ModSounds.FREEZE, 3.0F, 0.6F);
				EnergyFields.emit(world, EnergyFields.Energy.CRYO, c, 7, 200, owner());
			}
			if (t < 30) {
				for (LivingEntity e : Kit.living(world, c, 7)) {
					e.setFrozenTicks(Math.max(e.getFrozenTicks(), 200));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 6, false, false));
				}
			}
			if (t == 20) {
				// Ice shell grows over everything in the zone.
				BlockPos.Mutable p = new BlockPos.Mutable();
				for (int dx = -7; dx <= 7; dx++) {
					for (int dy = -2; dy <= 7; dy++) {
						for (int dz = -7; dz <= 7; dz++) {
							double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
							if (d < 6.0 || d > 7.0) {
								continue;
							}
							p.set(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
							if (world.getBlockState(p).isAir() && TemporaryBlocks.place(world, p.toImmutable(), Blocks.PACKED_ICE.getDefaultState(), 30)) {
								shell.add(p.toImmutable());
							}
						}
					}
				}
				Vfx.cage(world, c, 7.0F, 0xBFEFFF, 30);
				Kit.sound(world, c, SoundEvents.BLOCK_GLASS_PLACE, 4.0F, 0.5F);
			}
			if (t == 45) {
				for (BlockPos p : shell) {
					if (world.getBlockState(p).isOf(Blocks.PACKED_ICE)) {
						world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
						world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()),
								p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.6);
					}
				}
				Vfx.ring(world, c, 14.0F, 1.0F, 0xE8F8FF, false, 20);
				Vfx.flash(world, c, 0.4F, 64, 0xE8F8FF);
				Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 6.0F, 0.6F);
				Kit.damage(world, c, 8, 12.0F, ModDamageTypes.CRYO, null, owner());
				EnergyFields.shatter(world, c, 4);
			}
			return t > 46;
		}
	}

	/** 12 Arcflash Charge: branching electricity jumping between creatures and metal blocks; lamps flicker. */
	public static class Arcflash extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.flash(world, c, 0.5F, 48, 0x9FD8FF);
				EnergyFields.emit(world, EnergyFields.Energy.ELECTRIC, c, 12, 100, owner());
			}
			if (t % 4 == 0) {
				EnergyFields.chainLightning(world, c.add(0, 1, 0), 12, owner());
				Kit.sound(world, c, ModSounds.ARC_ZAP, 2.0F, 0.8F + world.random.nextFloat() * 0.6F);
				flickerLamps(world, BlockPos.ofFloored(c), 16);
			}
			return t > 60;
		}
	}

	static void flickerLamps(net.minecraft.server.world.ServerWorld world, BlockPos c, int r) {
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int i = 0; i < 60; i++) {
			p.set(c.getX() + world.random.nextInt(r * 2 + 1) - r, c.getY() + world.random.nextInt(r + 1) - r / 2, c.getZ() + world.random.nextInt(r * 2 + 1) - r);
			BlockState s = world.getBlockState(p);
			if (s.isOf(Blocks.REDSTONE_LAMP)) {
				TemporaryBlocks.place(world, p.toImmutable(), s.with(Properties.LIT, !s.get(Properties.LIT)), 3 + world.random.nextInt(6));
			}
		}
	}

	/** 13 Seismic Pulse Mine: a ground-hugging circular fissure that travels outward. */
	public static class SeismicPulse extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			double r = t * 0.5;
			if (t == 0) {
				Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 4.0F, 1.2F);
				EnergyFields.emit(world, EnergyFields.Energy.SONIC, c, 14, 60, owner());
			}
			if (t % 2 == 0 && r > 1) {
				Kit.ringCrack(world, c, r, 2, null, 0.7F);
				Vfx.ring(world, c, (float) r, 0.6F, 0xB09070, true, 8);
				for (LivingEntity e : Kit.living(world, c, r + 1.5)) {
					if (Math.abs(e.getPos().distanceTo(c) - r) < 1.5 && e.isOnGround()) {
						Kit.push(e, new Vec3d(0, 0.7, 0));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3, false, false));
						e.damage(world.getDamageSources().fall(), 3.0F);
					}
				}
			}
			if (t % 10 == 0) {
				Vfx.shake(world, c, 64, 0.8F);
			}
			return r > 14;
		}
	}

	/** 14 Implosion Grenade: pull, compress into a tight sphere, then fling everything outward. */
	public static class Implosion extends WorldEvent {
		private List<FallingBlockEntity> blocks = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			Vec3d core = c.add(0, 1.5, 0);
			if (t == 0) {
				blocks = Kit.lift(world, c, 5, 30, true, false);
				Vfx.orb(world, core, 6.0F, 0.8F, 0x80B0FF, 0, 30);
				Kit.sound(world, c, ModSounds.BLACK_HOLE, 3.0F, 1.2F);
				EnergyFields.emit(world, EnergyFields.Energy.GRAVITY, c, 10, 40, owner());
			}
			List<Entity> all = new ArrayList<>(blocks);
			all.addAll(Kit.entities(world, c, 9, e -> !(e instanceof FallingBlockEntity) && !Kit.creativeOrSpectator(e)));
			if (t < 30) {
				for (Entity e : all) {
					Vec3d to = core.subtract(e.getPos());
					double d = Math.max(0.3, to.length());
					Kit.velocity(e, to.multiply(Math.min(0.6, d * 0.12) / d));
				}
			} else if (t == 30) {
				Vfx.layeredBlast(world, core, 2.5F, 0x80B0FF);
				Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 3.0F, 1.4F);
				for (Entity e : all) {
					Vec3d out = e.getPos().subtract(core);
					double d = Math.max(0.3, out.length());
					Kit.velocity(e, out.multiply(1.6 / d).add(0, 0.5, 0));
					if (e instanceof FallingBlockEntity fb) {
						fb.setNoGravity(false);
					}
					if (e instanceof LivingEntity l) {
						l.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 6.0F);
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(blocks, Vec3d.ZERO);
		}
	}

	/** 15 Microstrike: six precision strikes that follow the owner's crosshair, one by one. */
	public static class Microstrike extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Entity owner = owner();
			if (!(owner instanceof PlayerEntity player) || !player.isAlive()) {
				return true;
			}
			BlockHitResult hit = TargetingItem.target(world, player, 160);
			if (hit.getType() != HitResult.Type.BLOCK) {
				return t > 120;
			}
			Vec3d target = hit.getPos();
			if (t % 4 == 0) {
				Vfx.grid(world, target.add(0, 0.05, 0), new Vec3d(0, 1, 0), 3.0F, 0x3A8CFF, 0, 6);
			}
			if (t >= 20 && (t - 20) % 16 == 0) {
				Vec3d sky = target.add(world.random.nextGaussian() * 2, 60, world.random.nextGaussian() * 2);
				Vfx.streak(world, sky, target, 0.6F, 0x7AC8FF, 5);
				com.armageddon.detonation.DetonationManager.schedule(world, 5, () -> {
					Vfx.layeredBlast(world, target, 2.5F, 0x7AC8FF);
					Vfx.pillar(world, target, 1.0F, 0x7AC8FF, false, 12);
					Destruction.explode(world, player, target.x, target.y, target.z, 2.6F, false);
					Kit.damage(world, target, 4, 14.0F, ModDamageTypes.PLASMA, null, player);
				});
				Kit.sound(world, target, ModSounds.RAILGUN_FIRE, 4.0F, 1.4F);
			}
			return t > 20 + 16 * 6;
		}
	}

	/** 17 Gravity Tether: elastic beam that yanks a creature in, or grapples the owner to a surface. */
	public static class Tether extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Entity owner = owner();
			if (owner == null || !owner.isAlive()) {
				return true;
			}
			Vec3d anchor;
			int targetId = data.getInt("Target");
			Entity target = targetId != 0 ? world.getEntityById(targetId) : null;
			Vec3d hand = owner.getEyePos().subtract(0, 0.3, 0);
			if (target != null && target.isAlive()) {
				anchor = target.getPos().add(0, target.getHeight() / 2, 0);
				Vec3d to = hand.subtract(anchor);
				if (to.length() < 2.0 || t > 30) {
					return true;
				}
				Kit.velocity(target, to.normalize().multiply(Math.min(1.6, 0.4 + t * 0.08)));
				target.fallDistance = 0;
			} else {
				anchor = new Vec3d(data.getDouble("Ax"), data.getDouble("Ay"), data.getDouble("Az"));
				Vec3d to = anchor.subtract(owner.getPos());
				if (to.length() < 2.0 || t > 30) {
					return true;
				}
				Kit.velocity(owner, to.normalize().multiply(Math.min(2.0, 0.6 + t * 0.1)).add(0, 0.05, 0));
				owner.fallDistance = 0;
			}
			Vfx.continuousBeam(world, hand, anchor, 0.12F + 0.05F * MathHelper.sin(t * 0.8F), 0x5AA8FF);
			return false;
		}
	}

	/** 18 Ion Fog: luminous fog that blinds hostile targeting and outlines everything inside. */
	public static class IonFog extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.BLOCK_FIRE_EXTINGUISH, 3.0F, 0.6F);
				EnergyFields.emit(world, EnergyFields.Energy.ELECTRIC, c, 10, 400, owner());
			}
			double r = Math.min(10, 2 + t * 0.2) * (t > 340 ? (400 - t) / 60.0 : 1.0);
			if (t % 2 == 0) {
				for (int i = 0; i < 12; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(world.random.nextDouble()) * r;
					world.spawnParticles(ModParticles.CRYO_MIST, c.x + Math.cos(a) * d, c.y + world.random.nextDouble() * 3, c.z + Math.sin(a) * d,
							0, 0, 0.01, 0, 1.0);
				}
			}
			if (t % 10 == 0) {
				for (LivingEntity e : Kit.living(world, c, r)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false));
					if (e instanceof HostileEntity mob) {
						mob.setTarget(null);
						mob.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false));
					}
				}
			}
			return t > 400;
		}
	}

	/** 19 Stasis Mine: hexagonal cage that freezes creatures and projectiles mid-air, then releases them. */
	public static class Stasis extends WorldEvent {
		private final Map<Entity, Vec3d> frozen = new HashMap<>();
		private final List<MobEntity> disabled = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.cage(world, c.add(0, 1, 0), 6.0F, 0x5AC8FF, 100);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 3.0F, 1.6F);
			}
			for (Entity e : Kit.entities(world, c, 6, x -> !Kit.creativeOrSpectator(x) && !(x instanceof com.armageddon.event.EventEntity))) {
				if (!frozen.containsKey(e)) {
					frozen.put(e, e.getVelocity());
					if (e instanceof MobEntity mob && !mob.isAiDisabled()) {
						mob.setAiDisabled(true);
						disabled.add(mob);
					}
				}
				e.setVelocity(Vec3d.ZERO);
				e.velocityModified = true;
				e.setNoGravity(true);
				e.fallDistance = 0;
			}
			if (t >= 100) {
				Vfx.ring(world, c.add(0, 1, 0), 9.0F, 0.6F, 0xBFEFFF, false, 15);
				Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 3.0F, 1.4F);
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			for (Map.Entry<Entity, Vec3d> en : frozen.entrySet()) {
				Entity e = en.getKey();
				if (e.isAlive()) {
					e.setNoGravity(false);
					e.setVelocity(e instanceof ProjectileEntity ? en.getValue() : Vec3d.ZERO);
					e.velocityModified = true;
				}
			}
			for (MobEntity m : disabled) {
				m.setAiDisabled(false);
			}
		}
	}

	/** 20 EMP Disruptor: lamps, torches, repeaters and rails die; guns jam; creepers defuse. Builds are untouched. */
	public static class Emp extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			int dur = 300;
			int R = data.contains("R") ? data.getInt("R") : 16;
			if (t == 0) {
				Vfx.ring(world, c, R, 1.5F, 0x5AA8FF, false, 25);
				Vfx.dome(world, c, R, 0x5AA8FF, 20);
				Vfx.screen(world, c, R + 8, 3, 0.8F, 60);
				Kit.sound(world, c, ModSounds.ORBITAL_STRIKE, 4.0F, 2.0F);
				EnergyFields.emit(world, EnergyFields.Energy.ELECTRIC, c, R, 60, owner());
				BlockPos b = BlockPos.ofFloored(c);
				BlockPos.Mutable p = new BlockPos.Mutable();
				for (int dx = -R; dx <= R; dx++) {
					for (int dy = -R / 2; dy <= R / 2; dy++) {
						for (int dz = -R; dz <= R; dz++) {
							if (dx * dx + dy * dy + dz * dz > R * R) {
								continue;
							}
							p.set(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
							BlockState s = world.getBlockState(p);
							BlockState off = null;
							if (s.contains(Properties.LIT) && s.get(Properties.LIT) && (s.isOf(Blocks.REDSTONE_LAMP) || s.isOf(Blocks.REDSTONE_TORCH) || s.isOf(Blocks.REDSTONE_WALL_TORCH))) {
								off = s.with(Properties.LIT, false);
							} else if (s.contains(Properties.POWERED) && s.get(Properties.POWERED)) {
								off = s.with(Properties.POWERED, false);
							} else if (s.isOf(Blocks.REDSTONE_WIRE) && s.get(Properties.POWER) > 0) {
								off = s.with(Properties.POWER, 0);
							}
							if (off != null) {
								TemporaryBlocks.place(world, p.toImmutable(), off, dur);
							}
						}
					}
				}
			}
			if (t % 10 == 0) {
				for (LivingEntity e : Kit.living(world, c, Math.max(16, R * 3))) {
					if (e instanceof CreeperEntity creeper) {
						creeper.setFuseSpeed(-1);
					}
					if (e instanceof PlayerEntity player && t == 0) {
						for (int i = 0; i < player.getInventory().size(); i++) {
							ItemStack s = player.getInventory().getStack(i);
							if (s.getItem() instanceof GunItem) {
								player.getItemCooldownManager().set(s.getItem(), 160);
							}
						}
					}
					if (e instanceof net.minecraft.entity.passive.IronGolemEntity golem) {
						golem.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 6, false, false));
					}
				}
			}
			return t > 60;
		}
	}
}
