package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.LineCarveJob;
import com.armageddon.detonation.ShellJob;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Red tier: world-scarring catastrophes with long, readable build-ups. */
public final class MythicEvents {
	private MythicEvents() {
	}

	static final int CRIMSON = 0xFF2020;

	static void warn(WorldEvent e, net.minecraft.server.world.ServerWorld world, Vec3d c, String key) {
		Vfx.warning(world, c, 1);
		Kit.title(world, c, 384, Text.translatable("message.armageddon.mythic_warning").formatted(Formatting.DARK_RED, Formatting.BOLD),
				Text.translatable(key).formatted(Formatting.RED));
	}

	/** 41 Bloodstar Collapse: a crimson star bleeds the sky, cracks the land, then implodes the world sphere. */
	public static class Bloodstar extends WorldEvent {
		private static final int LIFE = 240;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d star = g.add(0, 28, 0);
			if (t == 0) {
				warn(this, world, g, "message.armageddon.bloodstar");
				// Ends as the collapse orb (t == LIFE - 40) begins, or it would hide the implosion.
				Vfx.orb(world, star, 1.0F, 7.0F, CRIMSON, 0, LIFE - 40);
				Vfx.skyTint(world, g, 512, 0x500808, 0.9F);
				Vfx.screen(world, g, 256, 0, 0.4F, LIFE);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 16.0F, 0.4F);
			}
			if (t > 30 && t < LIFE && t % 6 == 0) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double d = 6 + world.random.nextDouble() * 34;
				Vec3d hit = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
				Vfx.arc(world, star, hit, 0.8F, 0xFF3030);
				Kit.sound(world, hit, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 8.0F, 0.5F);
				Kit.damage(world, hit, 3, 14.0F, ModDamageTypes.ELECTROCUTION, null, owner());
				if (world.random.nextInt(3) == 0) {
					Destruction.explode(world, null, hit.x, hit.y, hit.z, 2.4F, true);
				}
			}
			if (t > 60 && t < LIFE && t % 30 == 0) {
				double a = world.random.nextDouble() * Math.PI * 2;
				Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
				Kit.fissure(world, g.add(d.multiply(4)), d, 30, 7, 3, ModBlocks.CRIMSON_FISSURE.getDefaultState());
				Vfx.shake(world, g, 256, 1.6F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 10.0F, 0.6F);
			}
			if (t == LIFE - 40) {
				Vfx.orb(world, star, 7.0F, 0.5F, CRIMSON, 2, 40);
				Vfx.charge(world, host.getId(), star, 30.0F, CRIMSON, 40);
				Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 16.0F, 0.5F);
			}
			if (t == LIFE) {
				Vfx.layeredBlast(world, g, 18.0F, CRIMSON);
				Vfx.dome(world, g, 48.0F, 0xFF3030, 50);
				Vfx.ring(world, g, 110.0F, 4.0F, 0xFF5050, true, 50);
				Vfx.flash(world, g, 1.0F, 1024, 0xFF4040);
				Vfx.shake(world, g, 512, 3.5F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 20.0F, 0.6F);
				Detonations.blast(world, g, 70, 400.0F, ModDamageTypes.ANNIHILATION, owner(), 40, 4.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), 40, false, ShellJob.Pattern.NONE,
							ModBlocks.CRIMSON_FISSURE.getDefaultState(), 0.35F));
				}
				EnergyFields.emit(world, EnergyFields.Energy.ANTIMATTER, g, 50, 400, owner());
				return true;
			}
			return false;
		}
	}

	/** 42 Riftfall Engine: four sky rifts open and pour beams, meteors and warheads, then fold shut. */
	public static class Riftfall extends WorldEvent {
		private Vec3d rift(int i) {
			double a = i * Math.PI / 2 + Math.PI / 4;
			return center().add(Math.cos(a) * 24, 60, Math.sin(a) * 24);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				warn(this, world, g, "message.armageddon.riftfall");
				for (int i = 0; i < 4; i++) {
					Vfx.portal(world, rift(i), new Vec3d(0, -1, 0), 10.0F, 0xFF3060, 280);
					Vfx.play(world, VfxIds.RIFT_MAW, rift(i), 1.0F);
				}
				Vfx.skyTint(world, g, 384, 0x401020, 0.7F);
				Kit.sound(world, g, ModSounds.BLACK_HOLE, 16.0F, 0.5F);
			}
			int i = t % 4;
			if (t >= 40 && t < 100 && t % 3 == 0) {
				Vec3d hit = Kit.ground(world, g.x + world.random.nextGaussian() * 18, g.z + world.random.nextGaussian() * 18);
				Vfx.beam(world, rift(i), hit, 1.0F, 0xFF3060, 8);
				Vfx.play(world, VfxIds.RIFT_HIT, hit, 1.0F);
				Destruction.explode(world, null, hit.x, hit.y, hit.z, 3.0F, true);
				Kit.damage(world, hit, 4, 16.0F, ModDamageTypes.PLASMA, null, owner());
				Kit.sound(world, hit, ModSounds.PLASMA_IMPACT, 6.0F, 0.6F);
			}
			if (t >= 100 && t < 170 && t % 6 == 0) {
				// t is always even here, so t % 4 would only ever pick rifts 0 and 2.
				Vec3d from = rift((t / 6) % 4);
				Vec3d hit = Kit.ground(world, g.x + world.random.nextGaussian() * 22, g.z + world.random.nextGaussian() * 22);
				Kit.projectile(world, com.armageddon.entity.ProjectileKind.METEOR, from, hit.subtract(from).normalize().multiply(2.8), owner());
			}
			if (t >= 180 && t < 220 && t % 10 == 0) {
				Vec3d hit = Kit.ground(world, g.x + Math.cos(t) * 20, g.z + Math.sin(t) * 20);
				Vfx.streak(world, rift((t / 10) % 4), hit, 1.5F, 0xFF8040, 8);
				Detonations.nuke(world, hit, owner(), 6, false);
				Vfx.play(world, VfxIds.NUKE, hit, 0.6F);
			}
			if (t == 240) {
				for (int k = 0; k < 4; k++) {
					Vfx.beam(world, rift(k), g, 3.0F, 0xFF3060, 30);
				}
				Vfx.orb(world, g, 18.0F, 0.0F, 0x200010, 1, 30);
				Vfx.play(world, VfxIds.RIFT_COLLAPSE, g, 1.0F);
				Vfx.screen(world, g, 256, 4, 0.8F, 10);
				Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 16.0F, 0.6F);
			}
			if (t == 270) {
				Vfx.layeredBlast(world, g, 12.0F, 0xFF3060);
				Vfx.play(world, VfxIds.RIFT_BLAST, g, 1.0F);
				Vfx.flash(world, g, 1.0F, 768, 0xFFFFFF);
				Vfx.shake(world, g, 384, 3.0F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 16.0F, 0.8F);
				Detonations.blast(world, g, 36, 200.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 3.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), 18, CraterJob.Mode.ANTIMATTER));
				}
				return true;
			}
			return false;
		}
	}

	/** 43 Apocalypse Spindle: drills a molten shaft, sweeps a rotating trench, then pulses. */
	public static class Spindle extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d top = g.add(0, 30, 0);
			if (t == 0) {
				warn(this, world, g, "message.armageddon.spindle");
				Vfx.pillar(world, g, 0.6F, CRIMSON, false, 60);
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 12.0F, 0.5F);
			}
			if (t == 40) {
				Vfx.beam(world, top.add(0, 200, 0), g.add(0, -40, 0), 5.0F, 0xFF3020, 160);
				Vfx.shake(world, g, 256, 2.4F);
				Vfx.play(world, VfxIds.SPINDLE_SHAFT, g, 1.0F);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 16.0F, 0.4F);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new LineCarveJob(world, g.add(0, 4, 0), g.add(0, -40, 0), 5.0, ModBlocks.MOLTEN_CORE.getDefaultState(), 0.5F, 0));
				}
				Kit.damage(world, g, 8, 200.0F, ModDamageTypes.PLASMA, null, owner());
			}
			if (t >= 60 && t < 180) {
				double a = (t - 60) * Math.PI * 2 / 120;
				Vec3d p = Kit.ground(world, g.x + Math.cos(a) * 20, g.z + Math.sin(a) * 20);
				Vfx.beam(world, top, p, 1.4F, 0xFF4030, 3);
				Kit.sphere(world, BlockPos.ofFloored(p), 2.6, net.minecraft.block.Blocks.AIR.getDefaultState());
				Kit.damage(world, p, 4, 40.0F, ModDamageTypes.PLASMA, null, owner());
				if (t % 2 == 0) {
					Vfx.play(world, VfxIds.SPINDLE_SWEEP, p, 1.0F);
				}
				if (t % 4 == 0) {
					Kit.sound(world, p, ModSounds.FLAMETHROWER, 6.0F, 0.5F);
				}
			}
			if (t == 195) {
				Vfx.ring(world, g, 80.0F, 3.0F, CRIMSON, true, 40);
				Vfx.layeredBlast(world, top, 10.0F, CRIMSON);
				Vfx.play(world, VfxIds.SPINDLE_PULSE, g, 1.0F);
				Vfx.flash(world, g, 1.0F, 768, 0xFF6060);
				Vfx.shake(world, g, 384, 3.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 16.0F, 0.7F);
				Detonations.blast(world, g, 40, 120.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 20, 3.5);
				Kit.ringCrack(world, g, 21, 5, ModBlocks.CRIMSON_FISSURE.getDefaultState(), 0.9F);
				return true;
			}
			return false;
		}
	}

	/** 45 Extinction Choir: the sky cracks, four meteor waves sing down, then one giant impactor. */
	public static class Extinction extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				warn(this, world, g, "message.armageddon.extinction");
				Vfx.skyCrack(world, g, 1.0F, 0xFF6020, 0, 320);
				Vfx.skyTint(world, g, 512, 0x502010, 0.8F);
				Vfx.play(world, VfxIds.EXTINCTION_ASH, g, 1.0F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 16.0F, 0.4F);
			}
			int wave = (t - 30) / 40;
			if (t >= 30 && wave < 4 && (t - 30) % 40 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 16.0F, 0.5F + wave * 0.15F);
			}
			if (t >= 30 && wave < 4 && (t - 30) % 40 < 20 && t % 3 == 0) {
				double spread = 20 + wave * 8;
				Kit.meteor(world, Kit.ground(world, g.x + world.random.nextGaussian() * spread, g.z + world.random.nextGaussian() * spread), owner());
			}
			if (t == 200) {
				Vfx.streak(world, g.add(-120, 260, -60), g, 8.0F, 0xFF7020, 40);
				Vfx.orb(world, g.add(-120, 260, -60), 6.0F, 10.0F, 0xFF5010, 0, 40);
				// Scale 1.9 also widens the send range to reach players on the ground under the sky crack.
				Vfx.play(world, VfxIds.IMPACTOR_STREAK, g.add(-120, 260, -60), new Vec3d(120, -260, 60), 1.9F, 0);
				Kit.title(world, g, 384, Text.translatable("message.armageddon.impact").formatted(Formatting.DARK_RED, Formatting.BOLD), Text.empty());
				Kit.sound(world, g, ModSounds.JET_FLYBY, 16.0F, 0.3F);
			}
			if (t == 240) {
				Vfx.layeredBlast(world, g, 20.0F, 0xFF6020);
				Vfx.play(world, VfxIds.NUKE, g, 2.0F);
				Vfx.play(world, VfxIds.EXTINCTION_WALL, g, 1.0F);
				Vfx.dome(world, g, 60.0F, 0xFF8040, 60);
				Vfx.ring(world, g, 130.0F, 4.0F, 0xFFA060, true, 60);
				Vfx.flash(world, g, 1.0F, 1024, 0xFFE0C0);
				Vfx.screen(world, g, 256, 1, 0.9F, 25);
				Vfx.shake(world, g, 768, 4.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 24.0F, 0.5F);
				Detonations.nuke(world, g, owner(), 20, false);
				Detonations.debris(world, g, 20, 120);
				return true;
			}
			return false;
		}
	}
}
