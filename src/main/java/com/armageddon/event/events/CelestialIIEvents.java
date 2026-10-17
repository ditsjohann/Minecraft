package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.entity.FalloutCloudEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Paths;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Celestial expansion II: five long, loud, heavily animated world events. */
public final class CelestialIIEvents {
	private CelestialIIEvents() {
	}

	static BlockPos top(ServerWorld w, double x, double z) {
		return Kit.surface(w, MathHelper.floor(x), MathHelper.floor(z));
	}

	// ====================================================================== EXPLODING COWS

	/** Cows fall from the sky as bombs. Each wave is bigger than the last, until one enormous cow. */
	public static class ExplodingCows extends WorldEvent {
		private static final int BIG_AT = 540;
		private final Map<CowEntity, Integer> cows = new HashMap<>();
		private final Map<CowEntity, Float> scales = new HashMap<>();
		private CowEntity big;

		private void spawn(Vec3d c, float scale, double spread, double height) {
			if (cows.size() > 70) {
				return;
			}
			CowEntity cow = EntityType.COW.create(world);
			if (cow == null) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(world.random.nextDouble()) * spread;
			double x = c.x + Math.cos(a) * d, z = c.z + Math.sin(a) * d;
			cow.refreshPositionAndAngles(x, Kit.ground(world, x, z).y + height, z, world.random.nextFloat() * 360, 0);
			EntityAttributeInstance scaleAttr = cow.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
			if (scaleAttr != null) {
				scaleAttr.setBaseValue(scale);
			}
			// NoAI mobs never run travel(), so gravity does nothing: the event moves the cows itself (see fall()).
			cow.setAiDisabled(true);
			cow.setNoGravity(true);
			cow.setInvulnerable(true);
			cow.setPersistent();
			cow.setVelocity(world.random.nextGaussian() * 0.05, -0.9, world.random.nextGaussian() * 0.05);
			world.spawnEntity(cow);
			cows.put(cow, age());
			scales.put(cow, scale);
			world.playSound(null, x, cow.getY(), z, SoundEvents.ENTITY_COW_AMBIENT, net.minecraft.sound.SoundCategory.HOSTILE,
					Math.min(8.0F, 1.0F + scale * 0.5F), Math.max(0.2F, 1.7F - scale * 0.12F));
		}

		private void boom(CowEntity cow, float scale) {
			Vec3d p = cow.getPos();
			boolean giant = scale >= 12;
			float size = giant ? 40.0F : 1.5F + scale * 2.2F;
			Vfx.layeredBlast(world, p, size, 0xFFFFFF);
			Vfx.ring(world, p.add(0, 0.3, 0), size * 3.0F, size * 0.2F, 0xF8F8FF, true, giant ? 90 : 24);
			Vfx.flash(world, p, Math.min(1.0F, 0.1F * size), 64 + size * 20, 0xFFFFFF);
			Vfx.shake(world, p, 64 + size * 12, Math.min(8.0F, 0.3F * size));
			world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + scale, p.z, (int) (30 + size * 6), scale * 0.8, scale * 0.6, scale * 0.8, 0.4);
			world.spawnParticles(ParticleTypes.CLOUD, p.x, p.y + scale, p.z, (int) (20 + size * 4), scale, scale * 0.6, scale, 0.25);
			world.spawnParticles(ParticleTypes.SPLASH, p.x, p.y + scale, p.z, (int) (40 + size * 8), scale * 1.2, scale, scale * 1.2, 0.6);
			Kit.sound(world, p, SoundEvents.ENTITY_COW_DEATH, 8.0F + scale, Math.max(0.2F, 1.3F - scale * 0.07F));
			Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 6.0F + scale * 2, Math.max(0.4F, 1.4F - scale * 0.06F));
			Destruction.explode(world, null, p.x, p.y, p.z, Math.min(18.0F, 1.6F + scale * 1.5F), false);
			Kit.damage(world, p, size * 1.6, 12.0F + scale * 6, ModDamageTypes.NUCLEAR_BLAST, null, owner());
			// Milk splash: the ground goes white for a while.
			BlockPos b = BlockPos.ofFloored(p);
			int ri = (int) Math.min(60, 2 + size * 1.4);
			int step = giant ? 1 : 2;
			for (int dx = -ri; dx <= ri; dx += step) {
				for (int dz = -ri; dz <= ri; dz += step) {
					if (dx * dx + dz * dz <= ri * ri && world.random.nextFloat() < (giant ? 0.9F : 0.6F)) {
						BlockPos g = top(world, b.getX() + dx, b.getZ() + dz);
						if (Math.abs(g.getY() - b.getY()) < 8) {
							TemporaryBlocks.place(world, g, (world.random.nextInt(3) == 0 ? Blocks.SNOW_BLOCK : Blocks.WHITE_CONCRETE).getDefaultState(), 600 + world.random.nextInt(300));
						}
					}
				}
			}
			if (giant) {
				Vfx.dome(world, p, 140.0F, 0xFFFFFF, 80);
				Vfx.pillar(world, p, 12.0F, 0xFFFFFF, false, 100);
				Vfx.screen(world, p, 1024, 1, 1.0F, 20);
				Vfx.fov(world, p, 35.0F, 40);
				Detonations.blast(world, p, 140, 2000.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 30, 6.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, b, Destruction.radius(world, 32.0F), CraterJob.Mode.NUKE));
				}
				Detonations.debris(world, p, 30, 120);
			}
			cow.discard();
		}

		/** Moves a falling cow one tick (accelerating, tumbling); true once it reaches the ground or water. */
		private boolean fall(CowEntity cow, float scale) {
			Vec3d v = cow.getVelocity();
			v = new Vec3d(v.x * 0.92, Math.max(-3.2, Math.min(v.y, -0.6) - 0.06), v.z * 0.92);
			Vec3d next = cow.getPos().add(v);
			double floor = Kit.ground(world, next.x, next.z).y;
			boolean landed = next.y <= floor || !world.getBlockState(BlockPos.ofFloored(next)).getFluidState().isEmpty();
			if (landed) {
				next = new Vec3d(next.x, Math.max(floor, next.y), next.z);
			}
			cow.setVelocity(v);
			cow.setPosition(next);
			// Tumble: small cows flail, big ones turn slowly.
			float spin = 14.0F / Math.max(1.0F, scale * 0.6F);
			cow.setYaw(cow.getYaw() + spin);
			cow.setBodyYaw(cow.getYaw());
			cow.setHeadYaw(cow.getYaw());
			return landed;
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.exploding_cows");
				Vfx.skyTint(world, c, 100000, 0xE8F0FF, 0.5F);
				Kit.sound(world, c, SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.5F);
			}
			// Waves: tiny, small, medium, large — then the cow.
			if (t >= 20 && t < 150 && t % 3 == 0) {
				spawn(c, 0.45F, 36, 70 + world.random.nextInt(30));
				spawn(c, 0.45F, 36, 70 + world.random.nextInt(30));
			} else if (t >= 150 && t < 280 && t % 4 == 0) {
				spawn(c, 1.0F, 42, 80 + world.random.nextInt(30));
			} else if (t >= 280 && t < 400 && t % 8 == 0) {
				spawn(c, 2.6F, 48, 90 + world.random.nextInt(30));
			} else if (t >= 400 && t < 500 && t % 14 == 0) {
				spawn(c, 5.5F, 50, 100 + world.random.nextInt(30));
			}
			if (t == 150 || t == 280 || t == 400) {
				Kit.title(world, c, 100000, net.minecraft.text.Text.literal(t == 150 ? "BIGGER" : t == 280 ? "BIGGER." : "BIGGER!!").formatted(net.minecraft.util.Formatting.WHITE, net.minecraft.util.Formatting.BOLD),
						net.minecraft.text.Text.literal("moo").formatted(net.minecraft.util.Formatting.GRAY));
				Kit.sound(world, c, SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.9F - t * 0.001F);
			}
			if (t == BIG_AT - 20) {
				Vfx.warning(world, c, 2);
				Vfx.fov(world, c, 10.0F, 20);
			}
			if (t == BIG_AT && big == null) {
				spawn(c, 16.0F, 0.1, 230);
				for (CowEntity cow : cows.keySet()) {
					if (scales.get(cow) >= 12) {
						big = cow;
					}
				}
				if (big != null) {
					big.setVelocity(0, -0.8, 0);
					Vfx.pillar(world, Kit.ground(world, big.getX(), big.getZ()), 16.0F, 0xFFFFFF, false, 120);
					Vfx.screen(world, c, 100000, 0, 0.5F, 100);
				}
			}
			if (big != null && big.isAlive() && t % 8 == 0) {
				Kit.sound(world, big.getPos(), SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.2F);
				Vfx.shake(world, big.getPos(), 400, 2.0F + Math.max(0, (230 - (float) (big.getY() - c.y))) * 0.01F);
			}
			List<CowEntity> done = new ArrayList<>();
			for (Map.Entry<CowEntity, Integer> en : cows.entrySet()) {
				CowEntity cow = en.getKey();
				if (!cow.isAlive()) {
					done.add(cow);
					continue;
				}
				float sc = scales.get(cow);
				boolean landed = fall(cow, sc);
				world.spawnParticles(ParticleTypes.CLOUD, cow.getX(), cow.getY() + sc, cow.getZ(), 1 + (int) sc, sc * 0.3, sc * 0.3, sc * 0.3, 0.01);
				world.spawnParticles(ParticleTypes.FIREWORK, cow.getX(), cow.getY() + sc * 1.4, cow.getZ(), 1, 0.1, 0.1, 0.1, 0.02);
				if (landed || t - en.getValue() > 260) {
					boom(cow, scales.get(cow));
					done.add(cow);
				}
			}
			for (CowEntity cow : done) {
				cows.remove(cow);
				scales.remove(cow);
			}
			if (t > BIG_AT + 60 && cows.isEmpty() && t % 10 == 0) {
				world.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + 3, c.z, 40, 40, 4, 40, 0.05);
			}
			return t > BIG_AT + 200;
		}

		@Override
		public void finish() {
			for (CowEntity cow : cows.keySet()) {
				cow.discard();
			}
			cows.clear();
		}
	}

	// ====================================================================== DEADLY TWISTER

	/** A mile-high twister that wanders across the land and drags players out of safety. */
	public static class DeadlyTwister extends WorldEvent {
		private static final int LIFE = 760;
		private static final double R = 24, SPEED = 0.33;
		private final List<FallingBlockEntity> blocks = new ArrayList<>();
		private Vec3d start, dir;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start = center();
				dir = Kit.dir(data.getFloat("Yaw"));
				CelestialEvents.announce(world, start, "block.armageddon.deadly_twister");
				Vfx.twister(world, start, dir, (float) R, (float) SPEED, LIFE);
				Vfx.skyTint(world, start, 100000, 0x2A323A, 0.8F);
				Kit.sound(world, start, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.3F);
			}
			Vec3d p = Paths.twister(start, dir, SPEED, t);
			Vec3d c = Kit.ground(world, p.x, p.z);
			host.setPosition(c.x, c.y, c.z);
			double ramp = Math.min(1.0, t / 60.0) * (t > LIFE - 60 ? (LIFE - t) / 60.0 : 1.0);
			blocks.removeIf(fb -> !fb.isAlive());
			if (t % 4 == 0 && blocks.size() < 220 && t < LIFE - 80) {
				blocks.addAll(Kit.lift(world, c, R * 0.9, 9, true, true));
			}
			for (FallingBlockEntity fb : blocks) {
				if (!fb.isAlive()) {
					continue;
				}
				Vec3d off = fb.getPos().subtract(c);
				double ang = Math.atan2(off.z, off.x) + 0.26;
				double h = Math.min(120, Math.max(0, off.y) + 0.6);
				double r = MathHelper.clamp(3 + h * 0.2, 3, R);
				Vec3d target = c.add(Math.cos(ang) * r, h, Math.sin(ang) * r);
				Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.4));
			}
			for (Entity e : Kit.entities(world, c, 78, x -> !(x instanceof FallingBlockEntity) && !Kit.creativeOrSpectator(x)
					&& !(x instanceof com.armageddon.event.EventEntity))) {
				Vec3d d = new Vec3d(c.x - e.getX(), 0, c.z - e.getZ());
				double dist = d.length();
				double k = MathHelper.clamp(1.25 - dist / 78.0, 0.0, 1.25) * ramp;
				if (k <= 0.01 || dist < 0.5) {
					continue;
				}
				Vec3d tangent = new Vec3d(-d.z, 0, d.x).normalize().multiply(0.55 * k);
				Vec3d inward = d.normalize().multiply(0.34 * k);
				double lift = dist < R * 1.6 ? 0.32 * k : 0.04 * k;
				Kit.push(e, tangent.add(inward).add(0, lift, 0));
				e.fallDistance = 0;
				if (e instanceof LivingEntity l && dist < R && t % 5 == 0) {
					l.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 6.0F);
				}
			}
			if (t % 3 == 0) {
				Vfx.shake(world, c, 620, (float) (5.0 * ramp));
			}
			if (t % 14 == 0) {
				Kit.sound(world, c, SoundEvents.ITEM_ELYTRA_FLYING, 24.0F, 0.3F);
				Kit.sound(world, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 24.0F, 0.5F);
			}
			if (t % 10 == 0) {
				Vec3d bolt = c.add(world.random.nextGaussian() * 20, 0, world.random.nextGaussian() * 20);
				Vfx.arc(world, c.add(0, 90, 0), Kit.ground(world, bolt.x, bolt.z), 0.8F, 0xE0F0FF);
			}
			if (t % 6 == 0 && Destruction.enabled(world)) {
				Detonations.debris(world, c, 8, 6);
			}
			if (t >= LIFE) {
				Kit.drop(blocks, new Vec3d(0, 0.3, 0));
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(blocks, Vec3d.ZERO);
		}
	}

	// ====================================================================== COLOSSAL SPEAR

	/** A spear the size of a building lands, then spins down through the world, drilling a hole to the core. */
	public static class ColossalSpear extends WorldEvent {
		private static final int THROW = 36;
		private int surfaceY, bottomY, steps;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			BlockPos b = BlockPos.ofFloored(c);
			if (t == 0) {
				surfaceY = Kit.surface(world, b.getX(), b.getZ()).getY();
				bottomY = world.getBottomY() + 6;
				int depth = Math.max(20, surfaceY - bottomY);
				steps = (int) Math.ceil(depth / 0.6);
				CelestialEvents.announce(world, c, "block.armageddon.colossal_spear");
				Vfx.skyTint(world, c, 100000, 0x503010, 0.5F);
				Vfx.grid(world, c.add(0, 0.2, 0), new Vec3d(0, 1, 0), 34.0F, 0xFFB050, 1, THROW + 4);
				Vfx.spear(world, c, 92.0F, depth, THROW + 24 + steps + 50);
				Kit.sound(world, c, ModSounds.ORBITAL_CHARGE, 64.0F, 0.5F);
			}
			if (t == THROW) {
				Vfx.layeredBlast(world, c, 24.0F, 0xFFB050);
				Vfx.flash(world, c, 1.0F, 800, 0xFFE0B0);
				Vfx.shake(world, c, 800, 9.0F);
				Vfx.screen(world, c, 1024, 1, 0.9F, 14);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 64.0F, 0.6F);
				Detonations.blast(world, c, 70, 3000.0F, ModDamageTypes.ANNIHILATION, owner(), 24, 6.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, b, Destruction.radius(world, 18.0F), CraterJob.Mode.NUKE));
				}
				Detonations.debris(world, c, 24, 100);
			}
			int drill = t - (THROW + 24);
			if (drill >= 0 && drill < steps && Destruction.enabled(world)) {
				double y = surfaceY - drill * 0.6;
				double psi = drill * 0.38;
				int R = 12;
				for (int layer = 0; layer < 2; layer++) {
					int yy = (int) Math.floor(y) - layer;
					for (int dx = -R - 2; dx <= R + 2; dx++) {
						for (int dz = -R - 2; dz <= R + 2; dz++) {
							double d2 = dx * dx + dz * dz;
							BlockPos p = new BlockPos(b.getX() + dx, yy, b.getZ() + dz);
							if (d2 <= R * R) {
								Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
							} else if (d2 <= (R + 2) * (R + 2)) {
								double ang = Math.atan2(dz, dx);
								double diff = Math.abs(MathHelper.wrapDegrees((float) Math.toDegrees(ang - psi)));
								if (diff < 28 && layer == 0) {
									TemporaryBlocks.place(world, p, ModBlocksRef.molten(), 500);
								}
							}
						}
					}
				}
				if (drill % 4 == 0) {
					Vfx.shake(world, c, 700, 3.5F);
					Kit.sound(world, c, ModSounds.GRAVITY_SLAM, 40.0F, 0.4F + drill * 0.002F);
					Kit.damage(world, new Vec3d(c.x, Math.max(y, bottomY), c.z), R + 3, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
					world.spawnParticles(ParticleTypes.LAVA, c.x, surfaceY + 2, c.z, 10, R * 0.7, 2, R * 0.7, 0.0);
				}
				if (drill % 20 == 0) {
					Vfx.ring(world, c.add(0, 0.5, 0), 40.0F, 3.0F, 0xFFB050, true, 24);
				}
			}
			if (drill == steps) {
				Vfx.pillar(world, c, 7.0F, 0xFFD090, false, 220);
				Vfx.layeredBlast(world, c, 18.0F, 0xFFB050);
				Vfx.shake(world, c, 800, 6.0F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 64.0F, 0.5F);
				if (Destruction.enabled(world)) {
					for (int dx = -9; dx <= 9; dx++) {
						for (int dz = -9; dz <= 9; dz++) {
							if (dx * dx + dz * dz <= 81) {
								Destruction.set(world, new BlockPos(b.getX() + dx, bottomY, b.getZ() + dz), Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
							}
						}
					}
				}
			}
			return drill > steps + 60;
		}
	}

	/** Tiny indirection so the event file doesn't need a hard import of ModBlocks for one block. */
	static final class ModBlocksRef {
		static net.minecraft.block.BlockState molten() {
			return com.armageddon.registry.ModBlocks.MOLTEN_CORE.getDefaultState();
		}
	}

	// ====================================================================== ZOMBIE APOCALYPSE

	/** An undead tide that never stops growing, five stomping giants, and a green detonation each. */
	public static class ZombieApocalypse extends WorldEvent {
		private static final int LIFE = 1820;
		private static final int GIANT_FUSE = 420;
		private final List<ZombieEntity> zombies = new ArrayList<>();
		private final Map<GiantEntity, Integer> giants = new HashMap<>();

		private void spawnZombie(Vec3d c, double minR, double maxR) {
			if (zombies.size() >= 150) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = minR + world.random.nextDouble() * (maxR - minR);
			Vec3d g = Kit.ground(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
			ZombieEntity z = EntityType.ZOMBIE.create(world);
			if (z == null) {
				return;
			}
			z.refreshPositionAndAngles(g.x, g.y, g.z, world.random.nextFloat() * 360, 0);
			z.setPersistent();
			z.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100000, 0, false, false));
			z.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100000, 0, false, false));
			EntityAttributeInstance follow = z.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
			if (follow != null) {
				follow.setBaseValue(80);
			}
			world.spawnEntity(z);
			zombies.add(z);
			world.spawnParticles(ParticleTypes.SOUL, g.x, g.y + 0.5, g.z, 5, 0.3, 0.5, 0.3, 0.05);
		}

		private void spawnGiant(Vec3d c) {
			if (giants.size() >= 6) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			Vec3d g = Kit.ground(world, c.x + Math.cos(a) * 80, c.z + Math.sin(a) * 80);
			GiantEntity giant = EntityType.GIANT.create(world);
			if (giant == null) {
				return;
			}
			giant.refreshPositionAndAngles(g.x, g.y, g.z, 0, 0);
			giant.setPersistent();
			world.spawnEntity(giant);
			giants.put(giant, age());
			Vfx.pillar(world, g, 4.0F, 0x40C040, false, 40);
			Vfx.ring(world, g, 30.0F, 2.0F, 0x40C040, true, 30);
			Vfx.shake(world, g, 400, 3.0F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_EMERGE, 64.0F, 0.4F);
			Kit.title(world, c, 100000, net.minecraft.text.Text.literal("A GIANT RISES").formatted(net.minecraft.util.Formatting.DARK_GREEN, net.minecraft.util.Formatting.BOLD),
					net.minecraft.text.Text.empty());
		}

		private void giantBoom(GiantEntity giant) {
			Vec3d p = giant.getPos().add(0, 6, 0);
			Vfx.layeredBlast(world, p, 20.0F, 0x60FF40);
			Vfx.dome(world, p, 70.0F, 0x60FF40, 50);
			Vfx.ring(world, p.add(0, -5, 0), 120.0F, 5.0F, 0x70FF50, true, 60);
			Vfx.pillar(world, giant.getPos(), 8.0F, 0x60FF40, false, 120);
			Vfx.flash(world, p, 0.8F, 500, 0xB0FF90);
			Vfx.shake(world, p, 500, 6.0F);
			Kit.sound(world, p, ModSounds.NUKE_DETONATE, 64.0F, 0.7F);
			world.spawnParticles(ModParticles.TOXIC_GAS, p.x, p.y, p.z, 400, 14, 12, 14, 0.2);
			world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 150, 14, 12, 14, 0.4);
			Destruction.explode(world, giant, p.x, p.y, p.z, 14.0F, true);
			Detonations.blast(world, p, 40, 400.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 12, 4.0);
			if (Destruction.enabled(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(p), Destruction.radius(world, 14.0F), CraterJob.Mode.NUKE));
			}
			FalloutCloudEntity.spawn(world, p, 36.0F, 20 * 60, 2);
			giant.discard();
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.zombie_apocalypse");
				Vfx.skyTint(world, c, 100000, 0x1C3A18, 0.8F);
				Vfx.screen(world, c, 100000, 0, 0.35F, LIFE - 200);
				Kit.sound(world, c, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 64.0F, 0.4F);
			}
			if (t < LIFE - 250) {
				int every = Math.max(2, 10 - t / 180);
				if (t % every == 0) {
					int n = 1 + t / 150;
					for (int i = 0; i < n; i++) {
						spawnZombie(c, 28, 70);
					}
				}
			}
			if (t == 300 || t == 620 || t == 940 || t == 1160 || t == 1360) {
				spawnGiant(c);
			}
			zombies.removeIf(z -> !z.isAlive());
			List<GiantEntity> dead = new ArrayList<>();
			for (Map.Entry<GiantEntity, Integer> en : giants.entrySet()) {
				GiantEntity giant = en.getKey();
				if (!giant.isAlive()) {
					// Killed: it goes off. Removed without dying (e.g. Peaceful despawn): just forget it.
					if (giant.isDead()) {
						giantBoom(giant);
					}
					dead.add(giant);
					continue;
				}
				// Giants have no AI of their own: march on the nearest player, or the centre.
				PlayerEntity target = world.getClosestPlayer(giant, 240);
				Vec3d goal = target != null ? target.getPos() : c;
				Vec3d to = new Vec3d(goal.x - giant.getX(), 0, goal.z - giant.getZ());
				if (to.length() > 3) {
					Vec3d v = to.normalize().multiply(0.26);
					giant.setVelocity(v.x, giant.getVelocity().y, v.z);
					giant.velocityModified = true;
					giant.setYaw((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
					giant.setBodyYaw(giant.getYaw());
					giant.setHeadYaw(giant.getYaw());
				}
				if (t % 2 == 0 && Destruction.enabled(world)) {
					BlockPos f = giant.getBlockPos();
					Vec3d dirTo = to.length() > 0.1 ? to.normalize() : Vec3d.ZERO;
					BlockPos ahead = BlockPos.ofFloored(giant.getX() + dirTo.x * 4, giant.getY(), giant.getZ() + dirTo.z * 4);
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							for (int dy = 0; dy <= 18; dy++) {
								BlockPos p = ahead.add(dx, dy, dz);
								if (dx * dx + dz * dz <= 20 && !world.getBlockState(p).isAir()) {
									Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
								}
							}
						}
					}
					if (f.getY() > 0 && giant.isOnGround() && to.length() > 3 && giant.horizontalCollision) {
						giant.setVelocity(giant.getVelocity().x, 0.6, giant.getVelocity().z);
					}
				}
				if (t % 14 == 0) {
					Vec3d foot = giant.getPos();
					Vfx.shake(world, foot, 260, 2.5F);
					Vfx.ring(world, foot.add(0, 0.2, 0), 14.0F, 1.0F, 0x70A060, true, 10);
					Kit.sound(world, foot, SoundEvents.ENTITY_IRON_GOLEM_STEP, 32.0F, 0.2F);
					// Not Kit.damage: that would hit the giant itself (and the other giants) every stomp.
					for (net.minecraft.entity.LivingEntity e : Kit.living(world, foot, 8)) {
						if (e instanceof GiantEntity) {
							continue;
						}
						e.timeUntilRegen = 0;
						e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, giant, owner()), (float) Math.max(1.0, 24.0 * (1.0 - e.getPos().distanceTo(foot) / 8.0)));
					}
					Detonations.debris(world, foot, 6, 10);
				}
				if (t - en.getValue() > GIANT_FUSE) {
					giantBoom(giant);
					dead.add(giant);
				}
			}
			dead.forEach(giants::remove);
			if (t >= LIFE) {
				for (ZombieEntity z : zombies) {
					world.spawnParticles(ParticleTypes.POOF, z.getX(), z.getY() + 1, z.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
					z.discard();
				}
				zombies.clear();
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			for (ZombieEntity z : zombies) {
				z.discard();
			}
			for (GiantEntity g : giants.keySet()) {
				g.discard();
			}
			zombies.clear();
			giants.clear();
		}
	}

	// ====================================================================== UFO INVASION

	/** A city-wide saucer crosses the sky, painting the ground with a blue death beam, then leaves. */
	public static class UfoInvasion extends WorldEvent {
		private static final int LIFE = 640;
		private Vec3d from, to, dir;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				dir = Kit.dir(data.getFloat("Yaw"));
				double alt = Kit.ground(world, c.x, c.z).y + 170;
				from = new Vec3d(c.x - dir.x * 560, alt, c.z - dir.z * 560);
				to = new Vec3d(c.x + dir.x * 560, alt, c.z + dir.z * 560);
				CelestialEvents.announce(world, c, "block.armageddon.ufo_invasion");
				Vfx.ufo(world, from, to, 95.0F, LIFE);
				Vfx.skyTint(world, c, 100000, 0x0A1624, 0.6F);
				Kit.sound(world, c, ModSounds.DEATH_RAY, 64.0F, 0.3F);
			}
			Vec3d ship = Paths.ufo(from, to, t, LIFE);
			if (t % 20 == 0) {
				Kit.sound(world, new Vec3d(ship.x, c.y, ship.z), ModSounds.DEATH_RAY, 64.0F, 0.35F);
				Vfx.shake(world, new Vec3d(ship.x, c.y, ship.z), 500, 2.5F);
			}
			boolean on = t > LIFE * 0.12 && t < LIFE * 0.88;
			Vec3d b = on ? Paths.ufoBeam(new Vec3d(ship.x, 0, ship.z), dir, t) : null;
			// Far out of view the beam only paints the sky: carving unloaded chunks would load them (lag) and hit bedrock.
			if (on && world.isChunkLoaded(BlockPos.ofFloored(b))) {
				Vec3d g = Kit.ground(world, b.x, b.z);
				BlockPos bp = BlockPos.ofFloored(g).down();
				if (Destruction.enabled(world)) {
					Kit.sphere(world, bp, 9, Blocks.AIR.getDefaultState());
					for (int dx = -14; dx <= 14; dx += 1) {
						for (int dz = -14; dz <= 14; dz += 1) {
							if (dx * dx + dz * dz <= 196 && dx * dx + dz * dz > 60 && world.random.nextInt(3) == 0) {
								BlockPos s = top(world, bp.getX() + dx, bp.getZ() + dz);
								if (Math.abs(s.getY() - bp.getY()) < 6) {
									Destruction.set(world, s, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Block.NOTIFY_ALL);
								}
							}
						}
					}
				}
				// Tractor ring: things near the beam are lifted toward the ship before the core vaporises them.
				for (Entity e : Kit.entities(world, g, 22, x -> !Kit.creativeOrSpectator(x) && !(x instanceof FallingBlockEntity)
						&& !(x instanceof com.armageddon.event.EventEntity))) {
					Vec3d in = new Vec3d(g.x - e.getX(), 0, g.z - e.getZ());
					Kit.push(e, in.multiply(0.03).add(0, 0.16, 0));
					e.fallDistance = 0;
				}
				if (t % 2 == 0) {
					Kit.damage(world, g, 7, 1000.0F, ModDamageTypes.ANNIHILATION, null, owner());
					Kit.damage(world, g.add(0, 25, 0), 8, 1000.0F, ModDamageTypes.ANNIHILATION, null, owner());
				}
				if (t % 5 == 0) {
					Destruction.explode(world, null, g.x, g.y, g.z, 5.0F, true);
					Vfx.layeredBlast(world, g, 5.0F, 0x50B0FF);
				}
				if (t % 8 == 0) {
					Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 24.0F, 0.5F);
				}
			}
			return t > LIFE + 30;
		}
	}
}
