package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.ShellJob;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The true WORLD DESTRUCTION tier. Each event runs a scripted camera cinematic on every player's
 * client (the scenery and camera live there) while the server keeps the real destruction on the
 * exact same timeline, so the world is already breaking when control is handed back.
 */
public final class WorldCutsceneEvents {
	private WorldCutsceneEvents() {
	}

	/** Players cannot be hurt while the camera is out of their hands. */
	static void protect(ServerWorld w, int ticks) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks, 4, false, false));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ticks, 2, false, false));
		}
	}

	static void begin(WorldEvent e, ServerWorld w, Vec3d g, String key, int scene, Vec3d heading, int cineTicks) {
		CelestialEvents.announce(w, g, key);
		Vfx.cutscene(w, g, cineTicks);
		Vfx.cinematic(w, g, scene, heading, cineTicks);
		protect(w, cineTicks + 80);
	}

	// ====================================================================== GALACTIC SMASH

	/** A cosmic giant tears the sky open, looks around, winds up, and punches the ground so hard the planet rings. */
	public static class GalacticSmash extends WorldEvent {
		private static final int CINE = 760, SMASH = 460;
		private List<FallingBlockEntity> heaved = new ArrayList<>();
		private Vec3d fwd;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				begin(this, world, g, "block.armageddon.galactic_smash", 1, fwd, CINE);
				Vfx.skyCrack(world, g, 1.0F, 0xA070FF, 2, 220);
				Vfx.skyTint(world, g, 100000, 0x1A1040, 0.8F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.25F);
			}
			if (t > 20 && t < SMASH && t % 20 == 0) {
				Vfx.shake(world, g, 4096, 0.2F + 3.5F * t / SMASH);
			}
			if (t >= 150 && t < 330 && t % 25 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 0.35F);
			}
			if (t == 300) {
				frames(g, 0);
				Vfx.fov(world, g, 8.0F, 10);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.3F);
			}
			if (t == 330) {
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.3F);
				Vfx.spiral(world, g.add(fwd.multiply(230)).add(0, 340, 0), 150.0F, 120.0F, 0xFFB070, 130);
			}
			if (t == 420 || t == 440) {
				frames(g, t == 420 ? 1 : 2, 0);
				Vfx.fov(world, g, 14.0F, 12);
			}
			if (t == SMASH - 8) {
				frames(g, 3, 1, 0, 2);
			}
			if (t == SMASH) {
				frames(g, 3, 1, 0, 2, 3);
				Vfx.screen(world, g, 100000, 1, 1.0F, 36);
				Vfx.fov(world, g, 60.0F, 50);
				Vfx.shake(world, g, 100000, 9.0F);
				Vfx.layeredBlast(world, g, 70.0F, 0xFFFFFF);
				Vfx.dome(world, g, 220.0F, 0xFFE0B0, 120);
				Vfx.ring(world, g, 420.0F, 10.0F, 0xFFFFFF, true, 140);
				Vfx.pillar(world, g, 16.0F, 0xFFC080, false, 160);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.35F);
				Detonations.blast(world, g, 260, 9000.0F, ModDamageTypes.ANNIHILATION, owner(), 120, 10.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 100.0F), CraterJob.Mode.NUKE));
				}
				heaved = Kit.lift(world, g, 90, 260, true, false);
				for (int i = 0; i < 28; i++) {
					Vec3d d = Kit.dir(i * (360.0F / 28) + world.random.nextFloat() * 8);
					Kit.fissure(world, g.add(d.multiply(12)), d, 160, 14, 4, Blocks.LAVA.getDefaultState());
				}
				Detonations.debris(world, g, 90, 500);
			}
			if (t > SMASH && t < SMASH + 40) {
				for (FallingBlockEntity fb : heaved) {
					if (fb.isAlive()) {
						Kit.velocity(fb, new Vec3d(fb.getVelocity().x * 0.98, 0.35, fb.getVelocity().z * 0.98));
					}
				}
			}
			if (t == SMASH + 40) {
				Kit.drop(heaved, new Vec3d(0, -1.0, 0));
			}
			// The whole earth shudders: rolling shockwaves of cracked ground and thrown creatures.
			if (t > SMASH && t < SMASH + 280) {
				int k = t - SMASH;
				if (k % 6 == 0) {
					Kit.ringCrack(world, g, k * 3.4, 4, null, 0.8F);
				}
				if (k % 20 == 0) {
					for (LivingEntity e : Kit.living(world, g, k * 3.4 + 30)) {
						double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
						if (Math.abs(d - k * 3.4) < 22 && e.isOnGround()) {
							Kit.push(e, new Vec3d(0, 0.9, 0));
							e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 10.0F);
						}
					}
					Vfx.shake(world, g, 100000, 6.0F * (1.0F - k / 280.0F) + 0.5F);
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
				}
			}
			if (t == SMASH + 60) {
				Vfx.skyTint(world, g, 100000, 0x4A4038, 0.9F);
			}
			return t > SMASH + 300;
		}

		private void frames(Vec3d g, int... styles) {
			CityDestructionEvents.frames(world, g, styles);
		}

		@Override
		public void finish() {
			Kit.drop(heaved, Vec3d.ZERO);
		}
	}

	// ====================================================================== BOOMING TICK

	/** A ticking that starts laughably small and doubles in volume every five seconds for a full minute. */
	public static class BoomingTick extends WorldEvent {
		private static final int BOOMS = 12, GAP = 100, T0 = BOOMS * GAP, CINE = 400;
		private Vec3d fwd;

		private static double radius(int i) {
			return 1.5 * Math.pow(1.45, i);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				CelestialEvents.announce(world, g, "block.armageddon.booming_tick");
			}
			if (t < T0 && t % 20 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 8.0F + t * 0.04F, 0.6F + t * 0.0012F);
				// A visible heartbeat that grows with every tick.
				Vfx.ring(world, g.add(0, 0.3, 0), 2.0F + t * 0.03F, 0.4F, 0xFF3030, false, 12);
				Vfx.orb(world, g.add(0, 1.2, 0), 0.4F + t * 0.0015F, 0.2F + t * 0.001F, 0xFF3030, 0, 12);
			}
			if (t < T0 && t % GAP == 0) {
				int i = t / GAP;
				double r = radius(i);
				if (i == 0) {
					world.spawnParticles(ParticleTypes.POOF, g.x, g.y + 0.6, g.z, 12, 0.2, 0.1, 0.2, 0.01);
					Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.5F, 2.0F);
					Kit.sound(world, g, SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, 3.0F, 1.7F);
					Kit.title(world, g, 200, Text.literal("boom.").formatted(Formatting.GRAY), Text.literal("(that was it?)").formatted(Formatting.DARK_GRAY));
				} else {
					Vfx.layeredBlast(world, g, (float) (r * 0.55 + 1.0), 0xFF8040);
					Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), (float) Math.min(64, 1.0 + i * 5.5), (float) Math.max(0.35, 1.5 - i * 0.1));
					Vfx.shake(world, g, 128 + r * 10, (float) Math.min(9.0, 0.3 + i * 0.8));
					Vfx.fov(world, g, (float) Math.min(40.0, i * 3.5), 14);
					Destruction.explode(world, null, g.x, g.y, g.z, (float) Math.min(16.0, Math.max(1.0, r * 0.5)), i > 3);
					Detonations.blast(world, g, r * 1.8 + 4, (float) (i * i * 6), ModDamageTypes.NUCLEAR_BLAST, owner(), r * 0.5, 1.0 + i * 0.25);
					if (i >= 5 && Destruction.enabled(world)) {
						DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, (float) (r * 0.55)), CraterJob.Mode.NUKE));
					}
					if (i >= 8) {
						Vfx.ring(world, g, (float) (r * 3), 3.0F, 0xFF8040, true, 50);
						Vfx.screen(world, g, 4096, 1, 0.5F, 6);
					}
					Kit.title(world, g, 200, Text.literal("BOOM " + (i + 1) + " / 12").formatted(Formatting.RED, Formatting.BOLD),
							Text.literal(((T0 - t) / 20) + " s").formatted(Formatting.GRAY));
				}
			}
			if (t == T0) {
				Vfx.cutscene(world, g, CINE);
				Vfx.cinematic(world, g, 2, fwd, CINE);
				protect(world, CINE + 80);
			}
			if (t >= T0 && t < T0 + 330) {
				int k = t - T0;
				for (int i = 0; i < 2 + k / 40; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(world.random.nextDouble()) * 115;
					Vec3d p = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					Vfx.layeredBlast(world, p, 4.0F + world.random.nextInt(8), world.random.nextBoolean() ? 0xFF8040 : 0xFFD080);
					Destruction.explode(world, null, p.x, p.y, p.z, 5.0F + world.random.nextInt(6), true);
				}
				if (k % 6 == 0) {
					Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 40.0F, 0.5F);
				}
			}
			if (t == T0 + CINE - 4) {
				CityDestructionEvents.frames(world, g, 3, 1, 0, 2, 3, 1);
				Vfx.screen(world, g, 100000, 1, 1.0F, 60);
				Vfx.fov(world, g, 70.0F, 60);
				Vfx.shake(world, g, 100000, 10.0F);
				Vfx.layeredBlast(world, g, 110.0F, 0xFFFFFF);
				Vfx.dome(world, g, 400.0F, 0xFFC080, 140);
				Vfx.ring(world, g, 900.0F, 12.0F, 0xFFFFFF, true, 160);
				Vfx.pillar(world, g, 22.0F, 0xFFFFFF, false, 200);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
				Detonations.blast(world, g, 500, 20000.0F, ModDamageTypes.ANNIHILATION, owner(), 250, 14.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), 230, false, ShellJob.Pattern.NONE,
							ModBlocks.VOID_REMNANT.getDefaultState(), 0.25F));
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 130.0F), CraterJob.Mode.NUKE));
				}
				Detonations.debris(world, g, 120, 600);
			}
			if (t > T0 + CINE && t < T0 + CINE + 200 && t % 10 == 0) {
				Vfx.ring(world, g, 120.0F + (t - T0 - CINE) * 6, 6.0F, 0xFFC080, true, 40);
				Vfx.shake(world, g, 100000, 5.0F);
			}
			return t > T0 + CINE + 220;
		}
	}

	// ====================================================================== ALIEN DEATH RAY

	/** A cutscene to an alien world where the rocket is loaded and fired; then an EMP, a tsunami, and the biggest boom there is. */
	public static class AlienDeathRay extends WorldEvent {
		private static final int CINE = 480, ARRIVE = 350;
		private Vec3d fwd;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				begin(this, world, g, "block.armageddon.alien_death_ray", 3, fwd, CINE);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.4F);
			}
			if (t == 160 || t == 185 || t == 210) {
				int n = t == 160 ? 3 : t == 185 ? 2 : 1;
				Kit.title(world, g, 100000, Text.literal(String.valueOf(n)).formatted(Formatting.GREEN, Formatting.BOLD), Text.literal("firing sequence").formatted(Formatting.DARK_GREEN));
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 1.0F + n * 0.0F);
			}
			if (t == 240) {
				CityDestructionEvents.frames(world, g, 0, 1, 0);
				Vfx.shake(world, g, 100000, 3.0F);
				Kit.sound(world, g, ModSounds.ROCKET_LAUNCH, 64.0F, 0.4F);
			}
			if (t == 282) {
				CityDestructionEvents.frames(world, g, 2, 0);
				Kit.sound(world, g, ModSounds.NUKE_LAUNCH, 64.0F, 0.4F);
			}
			if (t == CINE - 14) {
				CityDestructionEvents.frames(world, g, 3, 1, 0, 2, 3);
			}
			if (t == CINE - 6) {
				Vfx.alienRocket(world, g.add(fwd.x * 900, 1000, fwd.z * 900), g.add(0, 2, 0), 150.0F, ARRIVE + 6);
				Vfx.skyTint(world, g, 100000, 0x20083A, 0.8F);
			}
			int k = t - CINE;
			if (k == 20) {
				// EMP: the whole planet's electronics die.
				Vfx.ring(world, g, 2200.0F, 20.0F, 0x60A0FF, false, 90);
				Vfx.dome(world, g, 900.0F, 0x60A0FF, 60);
				Vfx.screen(world, g, 100000, 3, 1.0F, 100);
				Vfx.screen(world, g, 100000, 0, 0.7F, 160);
				Vfx.flash(world, g, 0.7F, 4000, 0x80C0FF);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 2.0F);
				for (ServerPlayerEntity p : world.getPlayers()) {
					for (int i = 0; i < p.getInventory().size(); i++) {
						net.minecraft.item.ItemStack s = p.getInventory().getStack(i);
						if (s.getItem() instanceof com.armageddon.item.GunItem) {
							p.getItemCooldownManager().set(s.getItem(), 1200);
						}
					}
				}
				EventEntity.start(world, g, "emp", owner(), nbt -> nbt.putInt("R", 64));
			}
			if (k == 150) {
				Kit.title(world, g, 100000, Text.literal("WATER").formatted(Formatting.AQUA, Formatting.BOLD), Text.empty());
				EventEntity.start(world, g, "tsunami", owner(), nbt -> nbt.putFloat("Scale", 2.6F));
				Vfx.skyTint(world, g, 100000, 0x103060, 0.8F);
			}
			if (k >= 20 && k < ARRIVE && k % 10 == 0) {
				Vfx.shake(world, g, 100000, 1.5F + 4.0F * k / ARRIVE);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
			}
			if (k == ARRIVE - 16) {
				CityDestructionEvents.frames(world, g, 1, 0, 2);
				Vfx.fov(world, g, 30.0F, 20);
			}
			if (k == ARRIVE) {
				CityDestructionEvents.frames(world, g, 3, 1, 0, 2, 3, 1, 0, 3);
				Vfx.screen(world, g, 100000, 1, 1.0F, 70);
				Vfx.fov(world, g, 80.0F, 70);
				Vfx.shake(world, g, 100000, 12.0F);
				Vfx.layeredBlast(world, g, 130.0F, 0xFFFFFF);
				Vfx.layeredBlast(world, g.add(0, 20, 0), 90.0F, 0xA070FF);
				Vfx.dome(world, g, 520.0F, 0xFFE0FF, 160);
				Vfx.ring(world, g, 1400.0F, 14.0F, 0xFFFFFF, true, 200);
				Vfx.ring(world, g, 900.0F, 10.0F, 0xA070FF, true, 160);
				Vfx.pillar(world, g, 30.0F, 0xFFFFFF, false, 240);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.25F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
				Detonations.blast(world, g, 600, 100000.0F, ModDamageTypes.ANNIHILATION, owner(), 320, 20.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 200.0F), CraterJob.Mode.NUKE));
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), 256, false, ShellJob.Pattern.NONE,
							ModBlocks.VOID_REMNANT.getDefaultState(), 0.25F));
				}
				Detonations.debris(world, g, 160, 700);
			}
			if (k > ARRIVE && k < ARRIVE + 240 && k % 10 == 0) {
				Vfx.ring(world, g, 150.0F + (k - ARRIVE) * 7, 8.0F, 0xFFC0FF, true, 40);
				Vfx.shake(world, g, 100000, 6.0F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			}
			if (k == ARRIVE + 60) {
				Vfx.skyTint(world, g, 100000, 0x302038, 0.95F);
			}
			return k > ARRIVE + 260;
		}
	}
}
