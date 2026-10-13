package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.LineCarveJob;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.BoneMealItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Bomb expansion II: uncommon through mythic warheads, each with its own signature. */
public final class BombEvents {
	private BombEvents() {
	}

	static final Block[] DISCO_FLOOR = {Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
			Blocks.CYAN_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE};
	static final int[] RAINBOW = {0xFF3A3A, 0xFFB000, 0xFFE040, 0x5AFF3A, 0x3AF2FF, 0x3A8CFF, 0xB050FF, 0xFF3AD8};

	static BlockPos top(ServerWorld w, double x, double z) {
		return Kit.surface(w, MathHelper.floor(x), MathHelper.floor(z));
	}

	// ====================================================================== UNCOMMON

	/** Disco Bomb: a checkerboard dance floor, pulsing rainbow rings, everyone bounces on the beat. */
	public static class Disco extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.pillar(world, c, 1.2F, 0xFF40FF, true, 200);
				Kit.sound(world, c, ModSounds.DUBSTEP_DROP, 5.0F, 1.0F);
			}
			if (t % 10 == 0) {
				int beat = t / 10;
				Vfx.ring(world, c.add(0, 0.2, 0), 9.0F, 0.6F, RAINBOW[beat % 8], false, 14);
				Kit.sound(world, c, ModSounds.DUBSTEP_WUB, 3.0F, 0.7F + (beat % 4) * 0.15F);
				BlockPos b = BlockPos.ofFloored(c);
				for (int dx = -7; dx <= 7; dx++) {
					for (int dz = -7; dz <= 7; dz++) {
						if (dx * dx + dz * dz > 49) {
							continue;
						}
						BlockPos p = top(world, b.getX() + dx, b.getZ() + dz);
						if (Math.abs(p.getY() - b.getY()) < 6) {
							TemporaryBlocks.place(world, p, DISCO_FLOOR[Math.floorMod(dx + dz + beat, 8)].getDefaultState(), 40);
						}
					}
				}
				for (LivingEntity e : Kit.living(world, c, 10)) {
					if (e.isOnGround()) {
						Kit.push(e, new Vec3d(0, 0.42, 0));
					}
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false));
				}
			}
			if (t % 20 == 5) {
				double a = world.random.nextDouble() * Math.PI * 2;
				Vfx.firework(world, c.add(Math.cos(a) * 6, 4 + world.random.nextInt(4), Math.sin(a) * 6), 1.5F, world.random.nextInt(2), RAINBOW[world.random.nextInt(8)]);
			}
			return t > 200;
		}
	}

	/** Glitter Bomb: a drifting storm of sparkle that makes everything glow. No damage, ever. */
	public static class Glitter extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.firework(world, c.add(0, 3, 0), 3.0F, 2, 0xFFE8A0);
				Kit.sound(world, c, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 4.0F, 1.5F);
			}
			world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 6, 6, 3, 6, 0.03);
			world.spawnParticles(ParticleTypes.FIREWORK, c.x, c.y + 2, c.z, 4, 6, 3, 6, 0.04);
			if (t % 30 == 0) {
				for (LivingEntity e : Kit.living(world, c, 12)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0, false, false));
				}
			}
			return t > 160;
		}
	}

	/** Pumpkin Patch: dozens of jack-o'-lanterns sprout from the ground, then wither away. */
	public static class PumpkinPatch extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.ENTITY_WITCH_CELEBRATE, 4.0F, 0.8F);
				for (int i = 0; i < 48; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(world.random.nextDouble()) * 9;
					BlockPos p = top(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d).up();
					if (world.getBlockState(p).isAir() && Math.abs(p.getY() - c.y) < 6) {
						TemporaryBlocks.place(world, p, (world.random.nextInt(3) == 0 ? Blocks.CARVED_PUMPKIN : Blocks.JACK_O_LANTERN).getDefaultState(),
								300 + world.random.nextInt(120));
						world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
					}
				}
				Vfx.ring(world, c, 12.0F, 0.8F, 0xFF8020, false, 20);
			}
			return t > 5;
		}
	}

	/** Cobweb Bomb: a web explodes outward and sticks to everything. */
	public static class Cobweb extends WorldEvent {
		@Override
		public boolean tick() {
			Vec3d c = center();
			BlockPos b = BlockPos.ofFloored(c).up();
			Vfx.cage(world, c.add(0, 1, 0), 6.0F, 0xE8E8E8, 40);
			Kit.sound(world, c, SoundEvents.BLOCK_COBWEB_PLACE, 4.0F, 0.6F);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -6; dx <= 6; dx++) {
				for (int dy = -2; dy <= 5; dy++) {
					for (int dz = -6; dz <= 6; dz++) {
						if (dx * dx + dy * dy + dz * dz > 36 || world.random.nextFloat() > 0.38F) {
							continue;
						}
						p.set(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
						if (world.getBlockState(p).isAir()) {
							TemporaryBlocks.place(world, p.toImmutable(), Blocks.COBWEB.getDefaultState(), 400);
						}
					}
				}
			}
			return true;
		}
	}

	/** Balloon Bomb: a bouquet of balloons lifts everything gently, then pops. */
	public static class Balloon extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, 4.0F, 1.2F);
				for (int i = 0; i < 9; i++) {
					double a = i * Math.PI * 2 / 9;
					Vfx.firework(world, c.add(Math.cos(a) * 3, 4 + (i % 3), Math.sin(a) * 3), 1.0F, 2, RAINBOW[i % 8]);
				}
				for (Entity e : Kit.entities(world, c, 8, x -> !Kit.creativeOrSpectator(x))) {
					Kit.push(e, new Vec3d(0, 0.9, 0));
					if (e instanceof LivingEntity l) {
						l.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 50, 1, false, false));
						l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 240, 0, false, false));
					}
				}
			}
			if (t == 50) {
				Kit.sound(world, c.add(0, 8, 0), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.0F, 2.0F);
				for (int i = 0; i < 9; i++) {
					double a = i * Math.PI * 2 / 9;
					Vfx.firework(world, c.add(Math.cos(a) * 4, 9 + (i % 3), Math.sin(a) * 4), 1.5F, 1, RAINBOW[(i + 3) % 8]);
				}
			}
			return t > 55;
		}
	}

	/** Sprinkler Bomb: a rain cloud that puts out fires, grows plants and heals. */
	public static class Sprinkler extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.WEATHER_RAIN, 5.0F, 1.0F);
				Vfx.dome(world, c, 9.0F, 0x60B0FF, 40);
			}
			world.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + 6, c.z, 4, 2.5, 0.3, 2.5, 0.01);
			world.spawnParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 5.5, c.z, 24, 4.5, 0.2, 4.5, 0.0);
			if (t % 5 == 0) {
				for (int i = 0; i < 6; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(world.random.nextDouble()) * 8;
					BlockPos g = top(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
					if (Destruction.isProtected(world, g.up())) {
						continue;
					}
					if (world.getBlockState(g.up()).isOf(Blocks.FIRE)) {
						world.setBlockState(g.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
					}
					BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), world, g);
					BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), world, g.up());
				}
			}
			if (t % 20 == 0) {
				for (LivingEntity e : Kit.living(world, c, 9)) {
					e.extinguish();
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 1, false, false));
				}
			}
			if (t == 190) {
				Vfx.pillar(world, c, 0.4F, 0xFFFFFF, true, 40);
			}
			return t > 200;
		}
	}

	// ====================================================================== RARE

	/** Thermite Bomb: burns a glowing shaft straight down through the ground. */
	public static class Thermite extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.pillar(world, c, 0.8F, 0xFF6A10, false, 40);
				Kit.sound(world, c, ModSounds.FLAMETHROWER, 4.0F, 0.5F);
			}
			if (t == 20) {
				Vfx.layeredBlast(world, c, 3.0F, 0xFF6A10);
				Kit.sound(world, c, ModSounds.PLASMA_IMPACT, 5.0F, 0.5F);
				Kit.damage(world, c, 4, 24.0F, ModDamageTypes.PLASMA, null, owner());
				Detonations.ignite(world, c, 5, 0.35F);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new LineCarveJob(world, c.add(0, 0.5, 0), c.add(0, -26, 0), 2.3, ModBlocks.MOLTEN_CORE.getDefaultState(), 0.7F, 500));
				}
			}
			if (t >= 20 && t < 70 && t % 2 == 0) {
				world.spawnParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 3, 0.8, 0.2, 0.8, 0.0);
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 2, c.z, 3, 0.5, 1.5, 0.5, 0.02);
			}
			return t > 70;
		}
	}

	/** Tesla Bomb: eight seconds of stormy lightning around the blast point. */
	public static class Tesla extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.skyTint(world, c, 96, 0x304060, 0.5F);
				EnergyFields.emit(world, EnergyFields.Energy.ELECTRIC, c, 12, 160, owner());
				Kit.sound(world, c, ModSounds.ARC_ZAP, 4.0F, 0.5F);
			}
			if (t % 6 == 0 && t < 150) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double d = Math.sqrt(world.random.nextDouble()) * 12;
				Vec3d hit = Kit.ground(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
				Vec3d sky = hit.add(world.random.nextGaussian() * 3, 45, world.random.nextGaussian() * 3);
				Vfx.arc(world, sky, hit, 0.7F, 0xA0D0FF);
				Vfx.flash(world, hit, 0.25F, 48, 0xC0E0FF);
				Kit.damage(world, hit, 3, 9.0F, ModDamageTypes.ELECTROCUTION, null, owner());
				EnergyFields.chainLightning(world, hit.add(0, 1, 0), 8, owner());
				Kit.sound(world, hit, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0F, 1.2F);
				if (world.random.nextInt(5) == 0) {
					Detonations.ignite(world, hit, 2, 0.2F);
				}
			}
			return t > 160;
		}
	}

	/** Sinkhole Bomb: the ground funnels away into a deep conical pit. */
	public static class Sinkhole extends WorldEvent {
		private int baseY;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			BlockPos b = BlockPos.ofFloored(c);
			if (t == 0) {
				baseY = Kit.surface(world, b.getX(), b.getZ()).getY();
				Vfx.ring(world, c, 12.0F, 1.0F, 0x8A6A40, true, 20);
				Vfx.shake(world, c, 96, 1.5F);
				Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 6.0F, 0.6F);
			}
			if (t >= 10 && t < 40 && Destruction.enabled(world)) {
				int layer = t - 10;
				int y = baseY - layer;
				double r = Math.max(1.5, 8.0 - layer * 0.22);
				int ri = MathHelper.ceil(r);
				for (int dx = -ri; dx <= ri; dx++) {
					for (int dz = -ri; dz <= ri; dz++) {
						if (dx * dx + dz * dz <= r * r) {
							Destruction.set(world, new BlockPos(b.getX() + dx, y, b.getZ() + dz), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
							if (layer < 2) {
								Destruction.set(world, new BlockPos(b.getX() + dx, y + 1 + layer, b.getZ() + dz), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
							}
						}
					}
				}
				world.spawnParticles(ModParticles.SHOCK_DUST, c.x, y + 1.0, c.z, 6, r * 0.5, 0.3, r * 0.5, 0.05);
				if (t % 6 == 0) {
					Vfx.shake(world, c, 64, 1.2F);
				}
			}
			if (t == 42) {
				Kit.ringCrack(world, c, 10, 2, null, 0.6F);
				Detonations.debris(world, c, 6, 25);
				Kit.sound(world, c, SoundEvents.BLOCK_GRAVEL_BREAK, 5.0F, 0.4F);
			}
			return t > 46;
		}
	}

	// ====================================================================== LEGENDARY

	/** Tsunami Bomb: a three-block wall of water races outward, sweeping everything with it, then drains. */
	public static class Tsunami extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			float scale = data.contains("Scale") ? data.getFloat("Scale") : 1.0F;
			int wall = Math.round(3 * scale);
			if (t == 0) {
				Vfx.warning(world, c, 0);
				Vfx.skyTint(world, c, 192, 0x2060A0, 0.4F);
				Kit.sound(world, c, ModSounds.ORBITAL_CHARGE, 8.0F, 0.5F);
			}
			if (t >= 20 && t < 150) {
				double r = 2 + (t - 20) * 0.38 * scale;
				if (t % 2 == 0) {
					int n = (int) Math.min(520, r * 6);
					for (int i = 0; i < n; i++) {
						double a = Math.PI * 2 * i / n;
						BlockPos g = top(world, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r).up();
						for (int h = 0; h < wall; h++) {
							BlockPos p = g.up(h);
							if (world.getBlockState(p).isAir()) {
								TemporaryBlocks.place(world, p, Blocks.WATER.getDefaultState(), 28);
							}
						}
						if (i % 4 == 0) {
							world.spawnParticles(ParticleTypes.SPLASH, g.getX() + 0.5, g.getY() + 3, g.getZ() + 0.5, 10, 0.6, 0.8, 0.6, 0.2);
						}
					}
				}
				if (t % 4 == 0) {
					Vfx.ring(world, c, (float) r, 2.5F, 0x2080FF, false, 8);
					Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_SPLASH, 8.0F, 0.5F);
					for (LivingEntity e : Kit.living(world, c, r + 4)) {
						double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
						if (Math.abs(d - r) < 3.5) {
							Vec3d out = new Vec3d(e.getX() - c.x, 0, e.getZ() - c.z).normalize();
							Kit.push(e, out.multiply(1.1).add(0, 0.55, 0));
							e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 4.0F);
						}
					}
				}
			}
			return t > 175;
		}
	}

	/** Volcano Bomb: a cone of basalt rises, then erupts lava bombs for ten seconds. */
	public static class Volcano extends WorldEvent {
		private BlockPos base = BlockPos.ORIGIN;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				base = top(world, c.x, c.z).up();
				Vfx.warning(world, c, 0);
				Vfx.skyTint(world, c, 192, 0x401008, 0.5F);
				Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 10.0F, 0.4F);
			}
			Vec3d topPos = Vec3d.ofCenter(base.up(14));
			if (t < 56 && t % 4 == 0 && Destruction.enabled(world)) {
				int layer = t / 4;
				double r = 12 - layer * 0.75;
				int ri = MathHelper.ceil(r);
				int y = base.getY() + layer;
				for (int dx = -ri; dx <= ri; dx++) {
					for (int dz = -ri; dz <= ri; dz++) {
						double d2 = dx * dx + dz * dz;
						if (d2 > r * r) {
							continue;
						}
						BlockPos p = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
						boolean crater = layer >= 11 && d2 <= 6.5;
						BlockState s;
						if (crater) {
							s = Blocks.AIR.getDefaultState();
						} else {
							int roll = world.random.nextInt(10);
							s = roll < 2 ? Blocks.MAGMA_BLOCK.getDefaultState() : roll < 6 ? Blocks.BASALT.getDefaultState()
									: roll < 8 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.NETHERRACK.getDefaultState();
						}
						Destruction.set(world, p, s, Block.NOTIFY_ALL);
						for (int k = 1; k <= 4 && !crater; k++) {
							if (world.getBlockState(p.down(k)).isAir()) {
								Destruction.set(world, p.down(k), Blocks.BASALT.getDefaultState(), Block.NOTIFY_ALL);
							}
						}
					}
				}
				world.spawnParticles(ModParticles.SHOCK_DUST, base.getX() + 0.5, y, base.getZ() + 0.5, 20, r * 0.6, 0.5, r * 0.6, 0.1);
				Vfx.shake(world, c, 96, 1.2F);
			}
			if (t == 58 && Destruction.enabled(world)) {
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						if (dx * dx + dz * dz <= 5) {
							Destruction.set(world, base.add(dx, 11, dz), Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
						}
					}
				}
				Vfx.pillar(world, topPos, 2.5F, 0xFF4010, false, 200);
				Vfx.dome(world, topPos, 18.0F, 0xFF6020, 30);
			}
			if (t >= 60 && t < 260) {
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, topPos.x, topPos.y + 2, topPos.z, 6, 2, 1, 2, 0.25);
				world.spawnParticles(ParticleTypes.LAVA, topPos.x, topPos.y + 1, topPos.z, 3, 1.5, 0.5, 1.5, 0.0);
				if (t % 3 == 0 && Kit.debrisBudget(world, c) > 0) {
					for (int i = 0; i < 2; i++) {
						BlockPos spawn = base.up(16 + world.random.nextInt(2));
						double a = world.random.nextDouble() * Math.PI * 2;
						double sp = 0.4 + world.random.nextDouble() * 1.0;
						FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, spawn, Blocks.MAGMA_BLOCK.getDefaultState());
						fb.setVelocity(Math.cos(a) * sp, 1.1 + world.random.nextDouble() * 0.9, Math.sin(a) * sp);
						fb.velocityModified = true;
						fb.setHurtEntities(2.0F, 40);
						if (world.random.nextBoolean()) {
							fb.setDestroyedOnLanding();
						}
					}
				}
				if (t % 12 == 0) {
					Vfx.layeredBlast(world, topPos, 2.0F, 0xFF5010);
					Kit.sound(world, topPos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 6.0F, 0.5F);
					Kit.damage(world, topPos, 7, 14.0F, ModDamageTypes.PLASMA, null, owner());
				}
			}
			return t > 270;
		}
	}

	/** Ice Age Bomb: a creeping wave of frost that freezes water, snows the land and grows ice spires. */
	public static class IceAge extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.warning(world, c, 0);
				Vfx.skyTint(world, c, 192, 0xBFEFFF, 0.6F);
				Kit.sound(world, c, ModSounds.FREEZE, 10.0F, 0.4F);
				EnergyFields.emit(world, EnergyFields.Energy.CRYO, c, 30, 400, owner());
			}
			if (t >= 10 && t < 150) {
				double r = 3 + (t - 10) * 0.2;
				if (t % 2 == 0 && Destruction.enabled(world)) {
					int n = (int) (r * 9);
					for (int i = 0; i < n; i++) {
						double a = world.random.nextDouble() * Math.PI * 2;
						double d = r - world.random.nextDouble() * 3;
						BlockPos g = top(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
						BlockState s = world.getBlockState(g);
						if (s.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER) || s.isOf(Blocks.WATER)) {
							Destruction.set(world, g, Blocks.ICE.getDefaultState(), Block.NOTIFY_ALL);
						} else if (!s.isAir() && !s.isOf(Blocks.ICE)) {
							Destruction.set(world, g, world.random.nextInt(5) < 2 ? Blocks.PACKED_ICE.getDefaultState() : Blocks.SNOW_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
							if (world.getBlockState(g.up()).isAir() && world.random.nextBoolean()) {
								Destruction.set(world, g.up(), Blocks.SNOW.getDefaultState(), Block.NOTIFY_ALL);
							}
						}
					}
				}
				if (t % 6 == 0 && Destruction.enabled(world)) {
					for (int k = 0; k < 2; k++) {
						double a = world.random.nextDouble() * Math.PI * 2;
						double d = world.random.nextDouble() * r;
						BlockPos g = top(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d).up();
						int h = 5 + world.random.nextInt(9);
						for (int y = 0; y < h; y++) {
							Destruction.set(world, g.up(y), Blocks.BLUE_ICE.getDefaultState(), Block.NOTIFY_ALL);
							if (y < h / 3) {
								for (net.minecraft.util.math.Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
									Destruction.set(world, g.up(y).offset(dir), Blocks.PACKED_ICE.getDefaultState(), Block.NOTIFY_ALL);
								}
							}
						}
					}
				}
				if (t % 20 == 0) {
					Vfx.ring(world, c, (float) r, 2.0F, 0xBFEFFF, false, 20);
				}
				if (t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, c, r)) {
						e.setFrozenTicks(Math.max(e.getFrozenTicks(), 160));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.CRYO, null, owner()), 2.0F);
					}
				}
			}
			return t > 170;
		}
	}

	// ====================================================================== MYTHIC

	/** Hellmouth: the ground opens into a lava-floored pit; hellfire pillars, beams and a final eruption. */
	public static class Hellmouth extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				MythicEvents.warn(this, world, g, "message.armageddon.hellmouth");
				Vfx.skyTint(world, g, 384, 0x600808, 0.8F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 14.0F, 0.4F);
			}
			if (t == 20 || t == 30) {
				Kit.ringCrack(world, g, t == 20 ? 16 : 11, 3, ModBlocks.CRIMSON_FISSURE.getDefaultState(), 0.9F);
				Vfx.ring(world, g, 20.0F, 1.5F, 0xFF3010, true, 20);
				Vfx.shake(world, g, 256, 2.0F);
			}
			if (t == 40) {
				Vfx.layeredBlast(world, g, 8.0F, 0xC01010);
				Vfx.flash(world, g, 0.8F, 256, 0xFF6040);
				Vfx.shake(world, g, 384, 3.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 12.0F, 0.6F);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 16.0F), CraterJob.Mode.NUKE));
					DetonationManager.add(new LineCarveJob(world, g.add(0, 2, 0), g.add(0, -40, 0), 6.0, ModBlocks.MOLTEN_CORE.getDefaultState(), 0.6F, 0));
					DetonationManager.schedule(world, 160, () -> Kit.sphere(world, BlockPos.ofFloored(g).down(30), 7, Blocks.LAVA.getDefaultState()));
				}
			}
			if (t >= 60 && t < 260) {
				if (t % 8 == 0) {
					for (int i = 0; i < 6; i++) {
						double a = i * Math.PI / 3 + t * 0.02;
						Vec3d p = Kit.ground(world, g.x + Math.cos(a) * 14, g.z + Math.sin(a) * 14);
						Vfx.pillar(world, p, 1.5F, 0xFF3010, false, 16);
					}
					Detonations.ignite(world, g, 16, 0.04F);
					Vfx.beam(world, g.add(0, -4, 0), g.add(0, 150, 0), 3.0F, 0xFF3010, 10);
					Detonations.pull(world, g, 24, 0.3);
				}
				if (t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, g, 16)) {
						e.setOnFireFor(4);
					}
					Kit.damage(world, g, 14, 6.0F, ModDamageTypes.PLASMA, null, owner());
				}
				if (t % 40 == 0) {
					Kit.meteor(world, Kit.ground(world, g.x + world.random.nextGaussian() * 30, g.z + world.random.nextGaussian() * 30), owner());
				}
			}
			if (t == 280) {
				Vfx.beam(world, g.add(0, -4, 0), g.add(0, 300, 0), 8.0F, 0xFF6040, 40);
				Vfx.layeredBlast(world, g, 12.0F, 0xFF3010);
				Vfx.flash(world, g, 1.0F, 512, 0xFFC0A0);
				Vfx.shake(world, g, 512, 3.5F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 16.0F, 0.5F);
				Detonations.blast(world, g, 40, 150.0F, ModDamageTypes.PLASMA, owner(), 20, 3.0);
				Detonations.debris(world, g, 16, 80);
			}
			return t > 290;
		}
	}

	/** Cyclone Bomb: a mile-high tornado that wanders, ripping up the ground and everything on it. */
	public static class Cyclone extends WorldEvent {
		private static final int LIFE = 420;
		private final List<FallingBlockEntity> blocks = new ArrayList<>();
		private double heading;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				MythicEvents.warn(this, world, c, "message.armageddon.cyclone");
				heading = world.random.nextDouble() * Math.PI * 2;
				Vfx.skyTint(world, c, 256, 0x404858, 0.8F);
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 12.0F, 0.4F);
			}
			if (t > 20) {
				heading += world.random.nextGaussian() * 0.06;
				double nx = c.x + Math.cos(heading) * 0.35, nz = c.z + Math.sin(heading) * 0.35;
				host.setPosition(nx, Kit.ground(world, nx, nz).y, nz);
				c = center();
			}
			if (t % 4 == 0 && blocks.size() < 120 && t < LIFE - 40) {
				blocks.addAll(Kit.lift(world, c, 9, 5, true, true));
			}
			for (FallingBlockEntity fb : blocks) {
				if (!fb.isAlive()) {
					continue;
				}
				Vec3d off = fb.getPos().subtract(c);
				double ang = Math.atan2(off.z, off.x) + 0.22;
				double h = Math.min(34, Math.max(0, off.y) + 0.35);
				double r = MathHelper.clamp(2.5 + h * 0.18, 2.5, 9);
				Vec3d target = c.add(Math.cos(ang) * r, h, Math.sin(ang) * r);
				Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.4));
			}
			for (Entity e : Kit.entities(world, c, 12, x -> !(x instanceof FallingBlockEntity) && !Kit.creativeOrSpectator(x)
					&& !(x instanceof com.armageddon.event.EventEntity))) {
				Vec3d d = c.subtract(e.getPos());
				Vec3d tangent = new Vec3d(-d.z, 0, d.x).normalize().multiply(0.45);
				Kit.push(e, tangent.add(d.multiply(0.02)).add(0, d.length() < 8 ? 0.12 : 0, 0));
				if (e instanceof LivingEntity l) {
					l.fallDistance = 0;
					if (t % 10 == 0 && d.length() < 6) {
						l.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 3.0F);
					}
				}
			}
			if (t % 25 == 0) {
				Vfx.spiral(world, c, 9.0F, 38.0F, 0xB8C4D0, 35);
				Kit.sound(world, c, SoundEvents.ITEM_ELYTRA_FLYING, 10.0F, 0.4F);
			}
			for (int k = 0; k < 6; k++) {
				double h = k * 6;
				world.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + h, c.z, 3, 2 + h * 0.15, 0.5, 2 + h * 0.15, 0.3);
			}
			if (t >= LIFE) {
				Kit.drop(blocks, new Vec3d(0, 0.2, 0));
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(blocks, Vec3d.ZERO);
		}
	}

	/** Doomsday Clock: a clock face of grid light; a sweeping hand carves a trench; each hour a strike; midnight ends it. */
	public static class DoomsdayClock extends WorldEvent {
		private static final int R = 24;

		private Vec3d hour(int i) {
			double a = i * Math.PI / 6;
			return Kit.ground(world, center().x + Math.cos(a) * R, center().z + Math.sin(a) * R);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				MythicEvents.warn(this, world, c, "message.armageddon.doomsday");
				Vfx.grid(world, c.add(0, 0.2, 0), new Vec3d(0, 1, 0), R + 2, 0xFF3030, 0, 420);
				for (int i = 0; i < 12; i++) {
					Vfx.pillar(world, hour(i), 0.6F, 0xFF3030, false, 400);
				}
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 16.0F, 0.4F);
			}
			if (t >= 40 && t < 400) {
				double a = (t - 40) * Math.PI * 2 / 360.0;
				Vec3d dir = new Vec3d(Math.cos(a), 0, Math.sin(a));
				Vec3d tip = Kit.ground(world, c.x + dir.x * R * 0.95, c.z + dir.z * R * 0.95);
				Vfx.beam(world, c.add(0, 2, 0), tip.add(0, 1, 0), 1.2F, 0xFF3030, 3);
				if (Destruction.enabled(world)) {
					for (int d = 3; d <= R; d += 3) {
						Vec3d p = Kit.ground(world, c.x + dir.x * d, c.z + dir.z * d);
						Kit.sphere(world, BlockPos.ofFloored(p), 1.4, Blocks.AIR.getDefaultState());
					}
				}
				if (t % 5 == 0) {
					for (LivingEntity e : Kit.living(world, c, R + 2)) {
						Vec3d rel = e.getPos().subtract(c);
						double along = rel.x * dir.x + rel.z * dir.z;
						double side = Math.abs(rel.x * dir.z - rel.z * dir.x);
						if (along > 0 && along < R && side < 2.2) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 6.0F);
						}
					}
				}
				if ((t - 40) % 30 == 0) {
					int h = (t - 40) / 30;
					Vec3d pos = hour(h);
					Vfx.beam(world, pos.add(0, 200, 0), pos, 3.0F, 0xFF6060, 14);
					Vfx.layeredBlast(world, pos, 4.0F, 0xFF3030);
					Destruction.explode(world, null, pos.x, pos.y, pos.z, 5.0F, true);
					Kit.damage(world, pos, 8, 30.0F, ModDamageTypes.NUCLEAR_BLAST, null, owner());
					Kit.sound(world, pos, SoundEvents.BLOCK_BELL_USE, 16.0F, 0.5F);
					Kit.title(world, c, 64, Text.literal((h == 0 ? 12 : h) + " / 12").formatted(Formatting.DARK_RED, Formatting.BOLD), Text.empty());
				}
			}
			if (t == 405) {
				for (int i = 0; i < 12; i++) {
					Vec3d pos = hour(i);
					Vfx.beam(world, pos.add(0, 220, 0), pos, 4.0F, 0xFFFFFF, 20);
					Vfx.layeredBlast(world, pos, 5.0F, 0xFF3030);
				}
				Vfx.layeredBlast(world, c, 14.0F, 0xFF3030);
				Vfx.screen(world, c, 512, 1, 1.0F, 12);
				Vfx.shake(world, c, 512, 4.0F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 16.0F, 0.6F);
				Detonations.blast(world, c, R + 6, 300.0F, ModDamageTypes.ANNIHILATION, owner(), 10, 3.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(c), Destruction.radius(world, 14.0F), CraterJob.Mode.NUKE));
				}
				return true;
			}
			return false;
		}
	}
}
