package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.EventEntity;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Purple tier: experimental, physics-bending, terrain-transforming. */
public final class EpicEvents {
	private EpicEvents() {
	}

	/** 22 Cataclysm Mortar: impact crater, temporary molten pool, crystal shards, three rings. */
	public static class Cataclysm extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.layeredBlast(world, c, 6.0F, 0xB050FF);
				Vfx.flash(world, c, 0.6F, 160, 0xD0A0FF);
				Vfx.shake(world, c, 96, 1.6F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 6.0F, 1.5F);
				Kit.damage(world, c, 9, 30.0F, ModDamageTypes.PLASMA, null, owner());
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(c), 7, CraterJob.Mode.NUKE));
				}
				for (int i = 0; i < 9; i++) {
					double a = i * Math.PI * 2 / 9 + world.random.nextDouble() * 0.4;
					Vec3d base = c.add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
					Vfx.crystal(world, base, base.add(Math.cos(a) * 3, 3 + world.random.nextDouble() * 4, Math.sin(a) * 3), 0.6F, 0xC070FF, 200);
				}
				EnergyFields.emit(world, EnergyFields.Energy.PLASMA, c, 9, 200, owner());
			}
			if (t == 0 || t == 6 || t == 12) {
				Vfx.ring(world, c, 12.0F + t * 2.0F, 1.2F, t == 6 ? 0xFF70FF : 0xB050FF, t == 12, 20);
			}
			if (t == 30) {
				// Molten pool settles into the fresh crater bottom.
				BlockPos b = BlockPos.ofFloored(c);
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						if (dx * dx + dz * dz > 10) {
							continue;
						}
						BlockPos top = Kit.surface(world, b.getX() + dx, b.getZ() + dz);
						if (Math.abs(top.getY() - b.getY()) < 10) {
							TemporaryBlocks.place(world, top, ModBlocks.MOLTEN_CORE.getDefaultState(), 400 + world.random.nextInt(200));
						}
					}
				}
				Vfx.light(world, c, 1.0F, 0xFF6020);
			}
			if (t > 30 && t % 5 == 0) {
				world.spawnParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 3, 2.0, 0.2, 2.0, 0.0);
			}
			return t > 120;
		}
	}

	/** 23 Quantum Fracture: terrain splits into hovering, glitching fragments, then vanishes. */
	public static class QuantumFracture extends WorldEvent {
		private List<FallingBlockEntity> blocks = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				blocks = Kit.lift(world, c, 7, 250, false, true);
				Vfx.glitch(world, c, 9.0F, 0x50FFE0, 80);
				Vfx.grid(world, c, new Vec3d(0, 1, 0), 9.0F, 0x50FFE0, 1, 80);
				Kit.sound(world, c, ModSounds.ANTIMATTER, 4.0F, 1.6F);
			}
			if (t < 60) {
				for (FallingBlockEntity fb : blocks) {
					if (!fb.isAlive()) {
						continue;
					}
					Vec3d off = fb.getPos().subtract(c);
					double phase = (fb.getId() % 17) * 0.37;
					Vec3d target = c.add(off.x * 1.003, 1.5 + off.y * 0.98 + Math.sin(t * 0.2 + phase) * 0.15, off.z * 1.003);
					Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.3).add(0, t < 20 ? 0.06 : 0, 0));
					if (world.random.nextInt(60) == 0) {
						// Quantum "jitter": the fragment blinks a block sideways.
						fb.setPosition(fb.getX() + world.random.nextGaussian() * 0.6, fb.getY(), fb.getZ() + world.random.nextGaussian() * 0.6);
					}
				}
				if (t % 10 == 0) {
					Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 3.0F, 0.5F + t * 0.02F);
					Vfx.glitch(world, c, 9.0F, 0xFF50E0, 8);
				}
			}
			if (t == 60) {
				for (FallingBlockEntity fb : blocks) {
					if (fb.isAlive()) {
						world.spawnParticles(ModParticles.VOID_SPARK, fb.getX(), fb.getY() + 0.5, fb.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
					}
				}
				Kit.vanish(blocks);
				Vfx.layeredBlast(world, c.add(0, 2, 0), 3.0F, 0x50FFE0);
				Vfx.screen(world, c, 48, 4, 0.6F, 6);
				Destruction.explode(world, null, c.x, c.y + 1, c.z, 3.0F, false);
				Kit.damage(world, c, 8, 18.0F, ModDamageTypes.ANNIHILATION, null, owner());
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.vanish(blocks);
		}
	}

	/** 25 Crystal Cataclysm: energy crystal spires erupt, hum, then detonate in sequence. */
	public static class Crystals extends WorldEvent {
		private static final int SPIRES = 8;
		private final List<List<BlockPos>> spires = new ArrayList<>();
		private final List<Vec3d> dirs = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				for (int i = 0; i < SPIRES; i++) {
					double a = i * Math.PI * 2 / SPIRES + world.random.nextDouble() * 0.5;
					double d = 2 + world.random.nextDouble() * 5;
					dirs.add(new Vec3d(Math.cos(a) * 0.35, 1, Math.sin(a) * 0.35).normalize());
					List<BlockPos> s = new ArrayList<>();
					s.add(Kit.surface(world, MathHelper.floor(c.x + Math.cos(a) * d), MathHelper.floor(c.z + Math.sin(a) * d)).up());
					spires.add(s);
				}
				Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 4.0F, 0.5F);
			}
			if (t < 40 && t % 3 == 0) {
				for (int i = 0; i < SPIRES; i++) {
					List<BlockPos> s = spires.get(i);
					BlockPos base = s.get(0);
					Vec3d tip = Vec3d.ofCenter(base).add(dirs.get(i).multiply(s.size()));
					BlockPos next = BlockPos.ofFloored(tip);
					if (world.getBlockState(next).isAir() && Destruction.set(world, next, ModBlocks.ENERGY_CRYSTAL.getDefaultState(), Block.NOTIFY_ALL)) {
						s.add(next);
						world.spawnParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 3, 0.3, 0.3, 0.3, 0.05);
					}
				}
				Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, 3.0F, 0.6F + t * 0.03F);
			}
			if (t == 40) {
				Vfx.charge(world, host.getId(), c.add(0, 4, 0), 6.0F, 0xC070FF, 30);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, 4.0F, 0.6F);
			}
			int k = (t - 70) / 3;
			if (t >= 70 && (t - 70) % 3 == 0 && k < SPIRES) {
				List<BlockPos> s = spires.get(k);
				Vec3d mid = Vec3d.ofCenter(s.get(s.size() / 2));
				for (int j = 1; j < s.size(); j++) {
					if (world.getBlockState(s.get(j)).isOf(ModBlocks.ENERGY_CRYSTAL)) {
						world.setBlockState(s.get(j), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
					}
				}
				Vfx.layeredBlast(world, mid, 2.5F, 0xC070FF);
				for (int j = 0; j < 4; j++) {
					Vfx.crystal(world, mid, mid.add(world.random.nextGaussian() * 4, world.random.nextDouble() * 4, world.random.nextGaussian() * 4), 0.3F, 0xE0A0FF, 20);
				}
				Destruction.explode(world, null, mid.x, mid.y, mid.z, 3.2F, false);
				Kit.damage(world, mid, 5, 14.0F, ModDamageTypes.SHRAPNEL, null, owner());
				EnergyFields.shatter(world, mid, 3);
			}
			return t > 70 + SPIRES * 3;
		}
	}

	/** 26 Voidstorm Generator: a contained storm of gravity pulses, dark lightning and orbiting rubble. */
	public static class Voidstorm extends WorldEvent {
		private static final int LIFE = 300;
		private List<FallingBlockEntity> rubble = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			Vec3d eye = c.add(0, 8, 0);
			if (t == 0) {
				rubble = Kit.lift(world, c, 10, 60, true, false);
				Vfx.orb(world, eye, 1.0F, 3.0F, 0x200030, 1, LIFE);
				Vfx.skyTint(world, c, 96, 0x301040, 0.6F);
				Vfx.spiral(world, c, 14.0F, 16.0F, 0x8040FF, LIFE);
				Kit.sound(world, c, ModSounds.BLACK_HOLE, 6.0F, 0.6F);
				EnergyFields.emit(world, EnergyFields.Energy.VOID, c, 16, LIFE, owner());
				EnergyFields.emit(world, EnergyFields.Energy.GRAVITY, c, 16, LIFE, owner());
			}
			double ang = t * 0.08;
			for (FallingBlockEntity fb : rubble) {
				if (!fb.isAlive()) {
					continue;
				}
				Vec3d off = fb.getPos().subtract(c);
				double r = Math.max(3, Math.sqrt(off.x * off.x + off.z * off.z));
				double a = Math.atan2(off.z, off.x) + 0.08;
				Vec3d target = c.add(Math.cos(a) * r, 4 + Math.sin(ang + fb.getId()) * 2 + (fb.getId() % 5), Math.sin(a) * r);
				Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.25));
			}
			if (t % 20 == 10) {
				Vfx.ring(world, c, 16.0F, 0.8F, 0x8040FF, true, 15);
				for (Entity e : Kit.entities(world, c, 16, x -> !(x instanceof FallingBlockEntity) && !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
					Vec3d to = eye.subtract(e.getPos());
					Kit.push(e, to.normalize().multiply(0.6));
				}
				Kit.sound(world, c, ModSounds.DUBSTEP_WUB, 4.0F, 0.5F);
			}
			if (t % 7 == 0 && t < LIFE - 20) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double d = world.random.nextDouble() * 14;
				Vec3d hit = Kit.ground(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
				Vfx.arc(world, eye, hit, 0.5F, 0x9040FF);
				Kit.damage(world, hit, 2.5, 6.0F, ModDamageTypes.ELECTROCUTION, null, owner());
				if (world.random.nextInt(3) == 0) {
					Destruction.explode(world, null, hit.x, hit.y, hit.z, 1.6F, false);
				}
				Kit.sound(world, hit, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 3.0F, 0.6F);
			}
			if (t >= LIFE) {
				Vfx.layeredBlast(world, eye, 4.0F, 0x8040FF);
				Kit.drop(rubble, new Vec3d(0, -0.5, 0));
				Kit.sound(world, c, ModSounds.BLACK_HOLE_COLLAPSE, 6.0F, 1.0F);
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(rubble, Vec3d.ZERO);
		}
	}

	/** 30 Mirror Dimension Bomb: ghost overlay, then half the area phases into the mirror world for a while. */
	public static class Mirror extends WorldEvent {
		private static final int R = 9;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.mirror(world, c, R, 60);
				Vfx.bubble(world, c, R, 0xE0E8FF, 70);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_AMBIENT, 4.0F, 2.0F);
			}
			if (t == 60) {
				Vfx.screen(world, c, 32, 4, 1.0F, 8);
				Vfx.flash(world, c, 0.6F, 64, 0xFFFFFF);
				Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 5.0F, 0.5F);
				BlockPos b = BlockPos.ofFloored(c);
				BlockPos.Mutable p = new BlockPos.Mutable();
				for (int dx = -R; dx <= R; dx++) {
					for (int dy = -R; dy <= R; dy++) {
						for (int dz = -R; dz <= R; dz++) {
							if (dx * dx + dy * dy + dz * dz > R * R || ((dx + dy + dz) & 1) == 0) {
								continue;
							}
							p.set(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
							BlockState s = world.getBlockState(p);
							if (!s.isAir() && !s.hasBlockEntity() && s.getHardness(world, p) >= 0) {
								TemporaryBlocks.place(world, p.toImmutable(), Blocks.AIR.getDefaultState(), 1200 + world.random.nextInt(400));
							}
						}
					}
				}
				// Creatures inside are swapped to their mirror position.
				for (LivingEntity e : Kit.living(world, c, R)) {
					Vec3d m = new Vec3d(2 * c.x - e.getX(), e.getY(), 2 * c.z - e.getZ());
					if (world.isSpaceEmpty(e, e.getBoundingBox().offset(m.subtract(e.getPos())))) {
						e.requestTeleport(m.x, m.y, m.z);
					}
				}
				return true;
			}
			return false;
		}
	}
}
