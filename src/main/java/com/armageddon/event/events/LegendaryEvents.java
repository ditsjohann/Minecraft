package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.LineCarveJob;
import com.armageddon.detonation.SliceJob;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Gold tier: signature superweapons with multi-phase spectacle and real, large-scale destruction. */
public final class LegendaryEvents {
	private LegendaryEvents() {
	}

	static final int GOLD = 0xFFB000;

	/** 31 Solstice Sunlance: the released beam. Called by the item after its 3 s charge. */
	public static void sunlance(ServerWorld world, @Nullable Entity owner, Vec3d from, Vec3d to) {
		Vfx.beam(world, from, to, 1.6F, 0xFFE070, 30);
		Vfx.beam(world, from, to, 0.6F, 0xFFFFFF, 34);
		Vfx.flash(world, from, 0.5F, 96, 0xFFE070);
		Vfx.shake(world, from, 64, 1.0F);
		Vfx.light(world, to, 1.0F, 0xFFC040);
		Vfx.layeredBlast(world, to, 4.0F, 0xFFC040);
		Kit.sound(world, from, ModSounds.DEATH_RAY, 6.0F, 0.7F);
		Kit.sound(world, to, ModSounds.NUKE_DETONATE, 5.0F, 1.6F);
		if (Destruction.enabled(world)) {
			DetonationManager.add(new LineCarveJob(world, from.add(to.subtract(from).normalize().multiply(2)), to, 3.0,
					ModBlocks.MOLTEN_CORE.getDefaultState(), 0.35F, 600));
		}
		Vec3d d = to.subtract(from);
		double len = d.length();
		Vec3d n = d.multiply(1.0 / Math.max(1.0E-4, len));
		for (LivingEntity e : Kit.living(world, from.add(d.multiply(0.5)), len / 2 + 4)) {
			if (e == owner) {
				continue;
			}
			Vec3d rel = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
			double along = rel.dotProduct(n);
			if (along > 0 && along < len && rel.subtract(n.multiply(along)).length() < 4) {
				e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, owner, owner), 40.0F);
				e.setOnFireFor(10);
			}
		}
		EnergyFields.emit(world, EnergyFields.Energy.SOLAR, to, 8, 200, owner);
	}

	/** 32 Helios Crown: twelve golden pillars race outward, burn a ring, then collapse to the centre. */
	public static class HeliosCrown extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.warning(world, c, 0);
				Vfx.orb(world, c.add(0, 3, 0), 0.5F, 3.0F, GOLD, 0, 40);
				Vfx.skyTint(world, c, 256, 0xFFC060, 0.5F);
				Kit.sound(world, c, ModSounds.ORBITAL_CHARGE, 8.0F, 1.2F);
			}
			if (t >= 20 && t < 140 && (t - 20) % 3 == 0) {
				int s = t - 20;
				double r = s < 60 ? 3 + s * 37 / 60.0 : 40 - (s - 60) * 40 / 60.0;
				for (int i = 0; i < 12; i++) {
					double a = i * Math.PI / 6 + s * 0.012;
					Vec3d g = Kit.ground(world, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r);
					Vfx.pillar(world, g, 1.2F, GOLD, false, 4);
					BlockPos top = BlockPos.ofFloored(g).down();
					if (world.random.nextFloat() < 0.6F) {
						Destruction.set(world, top, ModBlocks.SCORCHED_EARTH.getDefaultState(), Block.NOTIFY_ALL);
						if (world.getBlockState(top.up()).isAir() && world.random.nextFloat() < 0.4F) {
							Destruction.set(world, top.up(), Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL);
						}
					}
					if (s % 9 == 0) {
						Kit.damage(world, g, 3, 12.0F, ModDamageTypes.PLASMA, null, owner());
					}
				}
				if (s % 12 == 0) {
					Kit.sound(world, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, 6.0F, 0.6F + s * 0.008F);
				}
			}
			if (t == 140) {
				Vec3d g = c;
				Vfx.pillar(world, g, 6.0F, 0xFFF0B0, false, 50);
				Vfx.dome(world, g, 30.0F, GOLD, 40);
				Vfx.flash(world, g, 1.0F, 512, 0xFFF0B0);
				Vfx.shake(world, g, 256, 2.6F);
				Vfx.screen(world, g, 128, 1, 0.7F, 25);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 12.0F, 1.1F);
				Detonations.blast(world, g, 44, 120.0F, ModDamageTypes.PLASMA, owner(), 22, 3.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), 22, CraterJob.Mode.GLASSED)
							.withLining(Blocks.YELLOW_STAINED_GLASS.getDefaultState()));
				}
				EnergyFields.emit(world, EnergyFields.Energy.SOLAR, g, 30, 400, owner());
				return true;
			}
			return false;
		}
	}

	/** 33 Worldsplitter Hammer slam: radial fissures, terrain lifted, then a second crushing shockwave. */
	public static class Worldsplit extends WorldEvent {
		private List<FallingBlockEntity> lifted = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.ring(world, c, 26.0F, 2.0F, 0xFFD070, true, 25);
				Vfx.shake(world, c, 128, 2.2F);
				Kit.sound(world, c, ModSounds.GRAVITY_SLAM, 8.0F, 0.5F);
				float yaw = data.getFloat("Yaw");
				for (int i = 0; i < 8; i++) {
					Vec3d d = Kit.dir(yaw + i * 45);
					Kit.fissure(world, c.add(d.multiply(2)), d, 18 + world.random.nextInt(8), 6, 3, ModBlocks.GOLD_FISSURE.getDefaultState());
					Vfx.streak(world, c, c.add(d.multiply(24)), 1.0F, 0xFFC040, 15);
				}
				lifted = Kit.lift(world, c, 10, 120, true, false);
				// Put back whatever was torn out from under the wielder's feet.
				Entity o = owner();
				if (o != null) {
					lifted.removeIf(fb -> {
						if (Math.abs(fb.getX() - o.getX()) < 0.8 && Math.abs(fb.getZ() - o.getZ()) < 0.8 && fb.getY() <= o.getY()) {
							world.setBlockState(fb.getBlockPos(), fb.getBlockState(), Block.NOTIFY_ALL);
							fb.discard();
							return true;
						}
						return false;
					});
				}
				for (LivingEntity e : Kit.living(world, c, 12)) {
					if (e != owner()) {
						// Damage first: its vanilla knockback would otherwise overwrite the launch.
						e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 20.0F);
						Kit.push(e, new Vec3d(0, 1.4, 0));
					}
				}
			}
			if (t < 30) {
				for (FallingBlockEntity fb : lifted) {
					if (fb.isAlive()) {
						Kit.velocity(fb, new Vec3d(0, 0.22 * (1 - t / 30.0) + 0.02, 0));
					}
				}
			}
			if (t == 30) {
				Kit.drop(lifted, new Vec3d(0, -1.6, 0));
			}
			if (t == 38) {
				Vfx.ring(world, c, 34.0F, 2.5F, 0xFFB000, true, 25);
				Vfx.layeredBlast(world, c, 5.0F, 0xFFC040);
				Vfx.shake(world, c, 160, 2.8F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 8.0F, 1.6F);
				Detonations.blast(world, c, 26, 70.0F, ModDamageTypes.SHRAPNEL, owner(), 0, 2.6, owner());
				Kit.ringCrack(world, c, 12, 3, ModBlocks.GOLD_FISSURE.getDefaultState(), 0.8F);
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(lifted, Vec3d.ZERO);
		}
	}

	/** 34 Solar Verdict Beacon: a public countdown, then nine judgement beams in sequence. */
	public static class SolarVerdict extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.warning(world, c, 0);
				Vfx.skyTint(world, c, 320, 0xFFD080, 0.6F);
				Kit.broadcast(world, c, 256, Text.translatable("message.armageddon.solar_verdict").formatted(Formatting.GOLD));
			}
			if (t < 100 && t % 20 == 0) {
				int n = 5 - t / 20;
				Kit.title(world, c, 128, Text.literal(String.valueOf(n)).formatted(Formatting.GOLD, Formatting.BOLD),
						Text.translatable("message.armageddon.verdict_sub").formatted(Formatting.YELLOW));
				Vfx.ring(world, c, 28.0F - t * 0.2F, 0.8F, GOLD, false, 20);
				Kit.sound(world, c, ModSounds.BOMB_BEEP, 8.0F, 0.6F + (100 - t) * 0.006F);
			}
			int k = (t - 100) / 8;
			if (t >= 100 && (t - 100) % 8 == 0 && k < 9) {
				Vec3d g;
				if (k == 8) {
					g = c;
				} else {
					double a = k * Math.PI / 4;
					g = Kit.ground(world, c.x + Math.cos(a) * 12, c.z + Math.sin(a) * 12);
				}
				Vfx.beam(world, g.add(0, 200, 0), g, k == 8 ? 4.0F : 2.0F, 0xFFF0A0, 20);
				Vfx.pillar(world, g, k == 8 ? 5.0F : 2.5F, GOLD, false, 30);
				Vfx.layeredBlast(world, g, k == 8 ? 6.0F : 3.5F, 0xFFC040);
				Vfx.flash(world, g, k == 8 ? 0.9F : 0.4F, 256, 0xFFF0B0);
				Vfx.shake(world, g, 128, k == 8 ? 2.4F : 1.0F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 8.0F, k == 8 ? 0.6F : 1.0F);
				Detonations.blast(world, g, k == 8 ? 20 : 8, k == 8 ? 80.0F : 30.0F, ModDamageTypes.PLASMA, owner(), k == 8 ? 8 : 3, 1.6);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), k == 8 ? 6 : 3, CraterJob.Mode.SHAFT));
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), k == 8 ? 12 : 5, CraterJob.Mode.GLASSED)
							.withLining(Blocks.ORANGE_STAINED_GLASS.getDefaultState()));
				}
				EnergyFields.emit(world, EnergyFields.Energy.SOLAR, g, 10, 200, owner());
			}
			return t > 100 + 8 * 9;
		}
	}

	/** 35 Titanfall Warhead: impact crater, debris thrown skyward, then a delayed secondary blast. */
	public static class Titanfall extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			Vec3d v = new Vec3d(data.getDouble("Vx"), -1, data.getDouble("Vz"));
			if (t == 0) {
				Vfx.layeredBlast(world, c, 7.0F, 0xFF9030);
				Vfx.flash(world, c, 0.7F, 256, 0xFFD090);
				Vfx.shake(world, c, 160, 2.2F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 8.0F, 1.3F);
				Detonations.blast(world, c, 20, 50.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 10, 2.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(c), 10, CraterJob.Mode.DIRECTIONAL).withAxis(v));
				}
				Detonations.debris(world, c, 10, 60);
			}
			if (t > 0 && t < 40 && t % 8 == 0) {
				Vfx.charge(world, host.getId(), c, 4.0F + t * 0.2F, 0xFF6020, 10);
				Kit.sound(world, c, ModSounds.BOMB_BEEP, 6.0F, 0.6F + t * 0.03F);
			}
			if (t == 45) {
				Vfx.layeredBlast(world, c, 11.0F, 0xFF6020);
				Vfx.dome(world, c, 24.0F, 0xFF9040, 30);
				Vfx.ring(world, c, 50.0F, 3.0F, 0xFFB070, true, 30);
				Vfx.flash(world, c, 1.0F, 512, 0xFFF0D0);
				Vfx.shake(world, c, 256, 3.0F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 14.0F, 0.8F);
				Detonations.blast(world, c, 32, 90.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 16, 2.6);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(c), 16, CraterJob.Mode.NUKE));
				}
				return true;
			}
			return false;
		}
	}

	/** 37 Reality Shear: a cutting plane previews, then erases one whole side of the world slice. */
	public static class RealityShear extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			Vec3d look = Kit.dir(data.getFloat("Yaw"));
			Vec3d normal = new Vec3d(-look.z, 0, look.x);
			if (t == 0) {
				Vfx.slice(world, c, normal, 32, 64, 0xE0F0FF, 60);
				Vfx.glitch(world, c, 16, 0xFFFFFF, 50);
				Kit.sound(world, c, ModSounds.RAILGUN_CHARGE, 8.0F, 0.5F);
			}
			if (t == 50) {
				Vfx.flash(world, c, 0.8F, 256, 0xFFFFFF);
				Vfx.screen(world, c, 128, 4, 0.8F, 6);
				Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 10.0F, 0.4F);
				Kit.sound(world, c, ModSounds.RAILGUN_FIRE, 10.0F, 0.5F);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new SliceJob(world, c, normal, 32, 24, 24, 64));
				}
				for (LivingEntity e : Kit.living(world, c, 40)) {
					Vec3d rel = e.getPos().subtract(c);
					double side = rel.dotProduct(normal);
					if (side > 0 && side < 24 && Math.abs(rel.dotProduct(look)) < 32 && e != owner()) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 60.0F);
					}
				}
				return true;
			}
			return false;
		}
	}

	/** 39 Continental Pulse: five expanding waves of cracks with molten gold seams. */
	public static class Continental extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.warning(world, c, 0);
				Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 10.0F, 0.5F);
			}
			int wave = (t - 20) / 30;
			if (t >= 20 && (t - 20) % 30 == 0 && wave < 5) {
				double r = 10 + wave * 11;
				Vfx.ring(world, c, (float) r * 1.6F, 2.0F, 0xFFA020, true, 30);
				Vfx.shake(world, c, 256, 1.4F + wave * 0.4F);
				Kit.sound(world, c, ModSounds.GRAVITY_SLAM, 10.0F, 0.5F + wave * 0.1F);
				Kit.ringCrack(world, c, r, 3 + wave, ModBlocks.GOLD_FISSURE.getDefaultState(), 0.75F);
				for (int i = 0; i < 6; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
					Kit.fissure(world, c.add(d.multiply(r - 8)), d, 14, 4 + wave, 2, ModBlocks.GOLD_FISSURE.getDefaultState());
				}
				for (LivingEntity e : Kit.living(world, c, r + 4)) {
					if (e.isOnGround() && e != owner()) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 6.0F + wave * 3);
						Kit.push(e, new Vec3d(0, 0.9, 0));
					}
				}
				Detonations.debris(world, c, r, 16);
			}
			return t > 20 + 30 * 5;
		}
	}

	/** 40 Dawnstar Payload: a golden star rises, gathers a ring of debris, then collapses onto the ground. */
	public static class Dawnstar extends WorldEvent {
		private static final int LIFE = 180;
		private List<FallingBlockEntity> debris = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			double h = Math.min(20, t * 0.4);
			Vec3d star = g.add(0, h, 0);
			if (t == 0) {
				Vfx.orb(world, g.add(0, 20, 0), 1.0F, 5.0F, 0xFFD040, 0, LIFE);
				Vfx.skyTint(world, g, 320, 0xFFB050, 0.7F);
				Vfx.warning(world, g, 0);
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 10.0F, 0.6F);
			}
			if (t == 50) {
				debris = Kit.lift(world, g, 20, 150, true, false);
			}
			if (t > 50 && t < LIFE) {
				for (FallingBlockEntity fb : debris) {
					if (!fb.isAlive()) {
						continue;
					}
					Vec3d off = fb.getPos().subtract(star);
					double r = MathHelper.clamp(Math.sqrt(off.x * off.x + off.z * off.z), 7, 18);
					double a = Math.atan2(off.z, off.x) + 0.06;
					Vec3d target = star.add(Math.cos(a) * r, Math.sin(t * 0.05 + fb.getId()) * 3, Math.sin(a) * r);
					Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.15));
				}
			}
			if (t % 20 == 0 && t > 40 && t < LIFE) {
				if (Destruction.enabled(world)) {
					Detonations.ignite(world, g, 14, 0.05F);
				}
				Vfx.light(world, star, 1.0F, 0xFFC040);
				for (LivingEntity e : Kit.living(world, g, 20)) {
					if (e != owner()) {
						e.setOnFireFor(4);
					}
				}
				Kit.sound(world, star, SoundEvents.BLOCK_FIRE_AMBIENT, 6.0F, 0.5F);
			}
			if (t >= LIFE - 20 && t < LIFE) {
				for (FallingBlockEntity fb : debris) {
					if (fb.isAlive()) {
						Kit.velocity(fb, star.subtract(fb.getPos()).multiply(0.25));
					}
				}
			}
			if (t == LIFE) {
				Kit.vanish(debris);
				Vfx.layeredBlast(world, g, 14.0F, 0xFFD040);
				Vfx.dome(world, g, 40.0F, 0xFFC040, 40);
				Vfx.ring(world, g, 70.0F, 3.0F, 0xFFE0A0, true, 40);
				Vfx.flash(world, g, 1.0F, 768, 0xFFFFFF);
				Vfx.screen(world, g, 160, 1, 0.8F, 20);
				Vfx.shake(world, g, 320, 3.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 16.0F, 0.9F);
				Detonations.blast(world, g, 44, 140.0F, ModDamageTypes.PLASMA, owner(), 22, 3.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), 24, CraterJob.Mode.GLASSED)
							.withLining(ModBlocks.GOLD_FISSURE.getDefaultState()));
				}
				EnergyFields.emit(world, EnergyFields.Energy.SOLAR, g, 40, 600, owner());
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.vanish(debris);
		}
	}
}
