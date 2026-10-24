package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.ShellJob;
import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModGameRules;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Celestial tier: world events. Every one is announced server-wide, honours protected regions,
 * the max-radius gamerule and the debris cap, and is cancellable with /armageddon purge.
 */
public final class CelestialEvents {
	private CelestialEvents() {
	}

	static void announce(ServerWorld world, Vec3d c, String key) {
		Vfx.warning(world, c, 2);
		Text name = Text.translatable(key).formatted(Formatting.WHITE, Formatting.BOLD);
		Kit.title(world, c, 100000, name, Text.translatable("message.armageddon.world_event").formatted(Formatting.GRAY, Formatting.ITALIC));
		Kit.broadcast(world, c, 100000, Text.translatable("message.armageddon.world_event_at", name,
				(int) c.x, (int) c.y, (int) c.z).formatted(Formatting.LIGHT_PURPLE));
	}

	static int maxRadius(ServerWorld world, int wanted) {
		return Math.min(wanted, world.getGameRules().getInt(ModGameRules.MAX_RADIUS));
	}

	/** 46 THE LAST LIGHT: a white star pulses the world pale, cracks the land with light, then shines outward. */
	public static class LastLight extends WorldEvent {
		private static final int LIFE = 300;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d star = g.add(0, 32, 0);
			if (t == 0) {
				announce(world, g, "block.armageddon.last_light");
				// Ends as the collapse orb (t == LIFE - 30) begins, or it would hide the shrink.
				Vfx.orb(world, star, 0.5F, 9.0F, 0xFFFFFF, 0, LIFE - 30);
				Vfx.skyTint(world, g, 100000, 0xF0F0FF, 0.8F);
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 24.0F, 0.3F);
			}
			if (t > 0 && t < LIFE && t % 40 == 0) {
				int pulse = t / 40;
				Vfx.screen(world, g, 1024, pulse % 2 == 0 ? 1 : 0, 0.35F + pulse * 0.06F, 20);
				Vfx.ring(world, star, 60.0F + pulse * 10, 2.0F, 0xFFFFFF, false, 30);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 24.0F, 0.5F + pulse * 0.08F);
				for (int i = 0; i < 3; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
					Kit.fissure(world, g.add(d.multiply(6 + pulse * 3)), d, 20 + pulse * 3, 4, 2, ModBlocks.LUMINOUS_GLASS.getDefaultState());
				}
				for (LivingEntity e : Kit.living(world, g, 96)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0, false, false));
				}
			}
			if (t == LIFE - 30) {
				Vfx.orb(world, star, 9.0F, 0.3F, 0xFFFFFF, 2, 30);
				Vfx.screen(world, g, 1024, 2, 0.6F, 30);
			}
			if (t == LIFE) {
				int r = maxRadius(world, 64);
				Vfx.layeredBlast(world, g, 24.0F, 0xFFFFFF);
				Vfx.dome(world, g, r, 0xFFFFFF, 80);
				Vfx.ring(world, g, r * 2.5F, 5.0F, 0xFFFFFF, true, 80);
				Vfx.screen(world, g, 100000, 1, 1.0F, 40);
				Vfx.shake(world, g, 1024, 4.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 32.0F, 1.6F);
				Detonations.blast(world, g, r * 1.3, 1000.0F, ModDamageTypes.ANNIHILATION, owner(), r, 4.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), r, false, ShellJob.Pattern.NONE,
							ModBlocks.LUMINOUS_GLASS.getDefaultState(), 0.5F));
				}
			}
			// Aftermath: a field of motes drifts upward for half a minute.
			if (t > LIFE && t % 2 == 0) {
				for (int i = 0; i < 20; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = Math.sqrt(world.random.nextDouble()) * 50;
					world.spawnParticles(ParticleTypes.END_ROD, g.x + Math.cos(a) * d, g.y - 20 + world.random.nextDouble() * 50, g.z + Math.sin(a) * d,
							0, 0, 1, 0, 0.08);
				}
			}
			return t > LIFE + 600;
		}
	}

	/** 47 ZEROPOINT ABSOLUTE: time stops, everything is drawn into a pinprick, then the void rings out. */
	public static class Zeropoint extends WorldEvent {
		private final Map<Entity, Boolean> frozen = new HashMap<>();
		/** Each frozen entity's own NoGravity flag, restored on release. */
		private final Map<Entity, Boolean> hadNoGravity = new HashMap<>();
		private List<FallingBlockEntity> pulled = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d core = g.add(0, 6, 0);
			int r = maxRadius(world, 36);
			if (t == 0) {
				announce(world, g, "block.armageddon.zeropoint");
				Vfx.screen(world, g, 512, 0, 0.9F, 120);
				Vfx.bubble(world, g, r, 0x60F0FF, 120);
				Vfx.grid(world, g, new Vec3d(0, 1, 0), r, 0x60F0FF, 1, 120);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 24.0F, 0.3F);
			}
			if (t < 120) {
				for (Entity e : Kit.entities(world, g, r, x -> !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
					if (!frozen.containsKey(e)) {
						frozen.put(e, e instanceof MobEntity m && !m.isAiDisabled());
						hadNoGravity.put(e, e.hasNoGravity());
						if (e instanceof MobEntity m) {
							m.setAiDisabled(true);
						}
					}
					e.setVelocity(Vec3d.ZERO);
					e.velocityModified = true;
					e.setNoGravity(true);
				}
			}
			if (t == 120) {
				release();
				pulled = Kit.lift(world, g, r * 0.8, 300, true, true);
				Vfx.orb(world, core, 4.0F, 0.2F, 0x000000, 1, 80);
				Vfx.charge(world, host.getId(), core, r, 0x60F0FF, 80);
				Kit.sound(world, g, ModSounds.BLACK_HOLE, 24.0F, 0.3F);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), r, true, ShellJob.Pattern.NONE,
							ModBlocks.VOID_REMNANT.getDefaultState(), 0.6F));
				}
			}
			if (t > 120 && t < 200) {
				for (FallingBlockEntity fb : pulled) {
					if (fb.isAlive()) {
						Vec3d to = core.subtract(fb.getPos());
						if (to.lengthSquared() < 2.0) {
							fb.discard();
						} else {
							Kit.velocity(fb, to.normalize().multiply(Math.min(2.0, 0.3 + (t - 120) * 0.03)));
						}
					}
				}
				for (Entity e : Kit.entities(world, g, r, x -> !(x instanceof FallingBlockEntity) && !(x instanceof EventEntity) && !Kit.creativeOrSpectator(x))) {
					Kit.push(e, core.subtract(e.getPos()).normalize().multiply(0.15));
				}
			}
			if (t == 200) {
				Kit.vanish(pulled);
				Vfx.layeredBlast(world, core, 10.0F, 0x60F0FF);
				Vfx.ring(world, core, r * 3.0F, 4.0F, 0x60F0FF, false, 50);
				Vfx.flash(world, g, 1.0F, 100000, 0xC0FFFF);
				Vfx.screen(world, g, 1024, 4, 0.8F, 10);
				Vfx.shake(world, g, 1024, 3.5F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 32.0F, 1.2F);
				Detonations.blast(world, g, r * 1.4, 1000.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 5.0);
				EnergyFields.emit(world, EnergyFields.Energy.CRYO, g, r, 600, owner());
				return true;
			}
			return false;
		}

		private void release() {
			for (Map.Entry<Entity, Boolean> en : frozen.entrySet()) {
				Entity e = en.getKey();
				if (e.isAlive()) {
					e.setNoGravity(hadNoGravity.getOrDefault(e, false));
					if (en.getValue() && e instanceof MobEntity m) {
						m.setAiDisabled(false);
					}
				}
			}
			frozen.clear();
			hadNoGravity.clear();
		}

		@Override
		public void finish() {
			release();
			Kit.vanish(pulled);
		}
	}

	/** 48 THE UNMAKER: the area is shown as a wireframe, then unmade shell by shell in geometric patterns. */
	public static class Unmaker extends WorldEvent {
		private ShellJob job;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			int r = maxRadius(world, 56);
			if (t == 0) {
				announce(world, g, "block.armageddon.unmaker");
				Vfx.wirebox(world, g.add(-r, -r, -r), g.add(r, r, r), 0xFFFFFF, 100);
				Vfx.grid(world, g, new Vec3d(0, 1, 0), r, 0xFFFFFF, 2, 100);
				Vfx.screen(world, g, 512, 0, 1.0F, 100);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 24.0F, 0.5F);
			}
			if (t > 0 && t < 100 && t % 10 == 0) {
				Vfx.glitch(world, g, r, 0xFFFFFF, 6);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 16.0F, 0.5F + t * 0.01F);
			}
			if (t == 100) {
				Vfx.flash(world, g, 0.8F, 1024, 0xFFFFFF);
				Vfx.screen(world, g, 1024, 4, 1.0F, 8);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 32.0F, 0.5F);
				if (Destruction.enabled(world)) {
					job = new ShellJob(world, BlockPos.ofFloored(g), r, false, ShellJob.Pattern.MIXED, ModBlocks.LUMINOUS_GLASS.getDefaultState(), 0.12F);
					DetonationManager.add(job);
				}
			}
			if (t > 100 && t % 4 == 0) {
				float shell = job != null ? job.currentShell() : (t - 100) * 0.5F;
				Vfx.ring(world, g, shell * 2 + 2, 1.0F, 0xFFFFFF, false, 6);
				// Damage the advancing shell itself, not ground zero (radial falloff left the front at ~1).
				for (LivingEntity e : Kit.living(world, g, Math.min(r, shell + 2))) {
					if (e.getPos().distanceTo(g) >= shell - 2) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 60.0F);
					}
				}
			}
			return t > 100 && (job == null ? t > 220 : job.isDone());
		}
	}

	/** 49 GODFALL PROTOCOL: sky panels open, then meteors, gravity wells, celestial beams and a final convergence. */
	public static class Godfall extends WorldEvent {
		private Vec3d panel(int i) {
			double a = i * Math.PI * 2 / 6;
			return center().add(Math.cos(a) * 40, 90, Math.sin(a) * 40);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				announce(world, g, "block.armageddon.godfall");
				for (int i = 0; i < 6; i++) {
					Vfx.grid(world, panel(i), new Vec3d(0, -1, 0), 12.0F, 0xFFE8A0, 0, 340);
				}
				Vfx.skyTint(world, g, 100000, 0xFFE0A0, 0.6F);
				Vfx.skyCrack(world, g, 0.6F, 0xFFE8A0, 1, 340);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 24.0F, 0.6F);
			}
			int i = t % 6;
			if (t >= 40 && t < 120 && t % 4 == 0) {
				Kit.meteor(world, Kit.ground(world, g.x + world.random.nextGaussian() * 30, g.z + world.random.nextGaussian() * 30), owner());
			}
			if (t == 120 || t == 140 || t == 160) {
				int k = (t - 120) / 20;
				double a = k * Math.PI * 2 / 3;
				Vec3d well = Kit.ground(world, g.x + Math.cos(a) * 25, g.z + Math.sin(a) * 25).add(0, 4, 0);
				Vfx.beam(world, panel(k * 2), well, 2.0F, 0xFFE8A0, 20);
				BlackHoleEntity.spawn(world, well, 3.0F, 120, true, owner(), 400);
			}
			if (t >= 200 && t < 270 && t % 5 == 0) {
				Vec3d hit = Kit.ground(world, g.x + world.random.nextGaussian() * 30, g.z + world.random.nextGaussian() * 30);
				Vfx.beam(world, panel(i), hit, 2.5F, 0xFFF0C0, 12);
				Vfx.pillar(world, hit, 2.0F, 0xFFE8A0, false, 15);
				Detonations.blast(world, hit, 8, 40.0F, ModDamageTypes.PLASMA, owner(), 4, 1.5);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(hit), 4, CraterJob.Mode.SHAFT));
				}
				Kit.sound(world, hit, ModSounds.ORBITAL_STRIKE, 12.0F, 1.0F);
			}
			if (t == 280) {
				for (int k = 0; k < 6; k++) {
					Vfx.beam(world, panel(k), g, 5.0F, 0xFFFFFF, 60);
				}
				Vfx.charge(world, host.getId(), g.add(0, 10, 0), 60.0F, 0xFFE8A0, 60);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 32.0F, 0.3F);
			}
			if (t == 340) {
				int r = maxRadius(world, 40);
				Vfx.layeredBlast(world, g, 26.0F, 0xFFE8A0);
				Vfx.dome(world, g, r * 1.4F, 0xFFE8A0, 80);
				Vfx.ring(world, g, r * 3.5F, 5.0F, 0xFFFFFF, true, 80);
				Vfx.flash(world, g, 1.0F, 100000, 0xFFFFFF);
				Vfx.screen(world, g, 1024, 1, 1.0F, 30);
				Vfx.shake(world, g, 2048, 4.5F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 32.0F, 0.4F);
				Detonations.blast(world, g, r * 1.6, 1000.0F, ModDamageTypes.ANNIHILATION, owner(), r, 5.0);
				if (Destruction.enabled(world)) {
					CraterJob crater = new CraterJob(world, BlockPos.ofFloored(g), r, CraterJob.Mode.NUKE);
					DetonationManager.add(crater);
					for (int k = 0; k < 10; k++) {
						Vec3d d = Kit.dir(k * 36 + world.random.nextFloat() * 20);
						Kit.fissure(world, g.add(d.multiply(r * 0.9)), d, r, 8, 3, ModBlocks.GOLD_FISSURE.getDefaultState());
					}
					// Floating islands of torn-up ground hang above the crater. Placed once the crater is
					// carved, since they sit inside its volume and would otherwise be erased with it.
					afterJob(world, crater, 120, () -> {
						for (int k = 0; k < 7; k++) {
							double a = world.random.nextDouble() * Math.PI * 2;
							double d = r * (0.3 + world.random.nextDouble() * 0.6);
							island(world, BlockPos.ofFloored(g.x + Math.cos(a) * d, g.y + 18 + world.random.nextInt(20), g.z + Math.sin(a) * d));
						}
					});
				}
				return true;
			}
			return false;
		}

		/** Runs {@code action} once {@code job} is done, polling every 10 ticks for up to {@code tries} polls. */
		private static void afterJob(ServerWorld world, CraterJob job, int tries, Runnable action) {
			DetonationManager.schedule(world, 10, () -> {
				if (job.isDone() || tries <= 0) {
					action.run();
				} else {
					afterJob(world, job, tries - 1, action);
				}
			});
		}

		private static void island(ServerWorld world, BlockPos c) {
			int rr = 2 + world.random.nextInt(3);
			for (int dx = -rr; dx <= rr; dx++) {
				for (int dz = -rr; dz <= rr; dz++) {
					int depth = rr - (int) Math.sqrt(dx * dx + dz * dz);
					for (int dy = 0; dy <= depth; dy++) {
						BlockPos p = c.add(dx, -dy, dz);
						if (!world.getBlockState(p).isAir()) {
							continue;
						}
						BlockState s = dy == 0 ? Blocks.GRASS_BLOCK.getDefaultState() : dy < 2 ? Blocks.DIRT.getDefaultState() : Blocks.STONE.getDefaultState();
						if (depth > 0 && dy == depth && world.random.nextBoolean()) {
							s = ModBlocks.GOLD_FISSURE.getDefaultState();
						}
						Destruction.set(world, p, s, Block.NOTIFY_ALL);
					}
				}
			}
		}
	}

	/** 50 OMEGA: FINAL EXISTENCE. Gravity reverses, the landscape rises, the sky breaks, and everything ends. */
	public static class FinalExistence extends WorldEvent {
		private List<FallingBlockEntity> rising = new ArrayList<>();
		private final Set<Entity> floated = new HashSet<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d sphere = g.add(0, 90, 0);
			int r = maxRadius(world, 160);
			if (t == 0) {
				announce(world, g, "block.armageddon.final_existence");
				// Ends as the collapse orb (t == 360) begins, or it would hide the collapse and the finale.
				Vfx.orb(world, sphere, 2.0F, 40.0F, 0x100008, 1, 360);
				Vfx.skyTint(world, g, 100000, 0x180010, 1.0F);
				Vfx.screen(world, g, 100000, 0, 0.6F, 400);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.2F);
			}
			if (t >= 60 && t < 300) {
				if (t == 60) {
					Vfx.gravityFlip(world, g, r, 240);
					Kit.title(world, g, 100000, Text.translatable("message.armageddon.gravity").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), Text.empty());
				}
				if (t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, g, r)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 15, 1, false, false));
							e.fallDistance = 0;
							floated.add(e);
						}
					}
				}
			}
			if (t == 300) {
				// Levitation is over: let everything that floated drift back down instead of dying of fall damage.
				for (Entity e : floated) {
					if (e.isAlive() && e instanceof LivingEntity l) {
						l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false));
					}
				}
				floated.clear();
			}
			if (t >= 100 && t < 260 && t % 20 == 0) {
				rising.addAll(Kit.lift(world, g, r * 0.6, 500 - rising.size() > 0 ? Math.min(80, 500 - rising.size()) : 0, true, true));
				Vfx.shake(world, g, 2048, 1.5F + (t - 100) * 0.01F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 32.0F, 0.4F);
			}
			if (t >= 100 && t < 360) {
				for (FallingBlockEntity fb : rising) {
					if (fb.isAlive()) {
						Vec3d to = sphere.subtract(fb.getPos());
						Kit.velocity(fb, new Vec3d(to.x * 0.004, 0.25 + MathHelper.sin(fb.getId()) * 0.05, to.z * 0.004));
					}
				}
			}
			if (t == 200) {
				Vfx.skyCrack(world, g, 1.0F, 0xFF40FF, 2, 260);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.2F);
			}
			if (t == 360) {
				Kit.vanish(rising);
				Vfx.orb(world, sphere, 40.0F, 0.1F, 0x100008, 2, 60);
				Vfx.charge(world, host.getId(), sphere, 160.0F, 0xFF40FF, 60);
				Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.3F);
			}
			if (t == 420) {
				Vfx.screen(world, g, 100000, 2, 1.0F, 30);
				Vfx.layeredBlast(world, g, 40.0F, 0xFF40FF);
				Vfx.dome(world, g, r, 0xC040FF, 120);
				Vfx.ring(world, g, r * 3.0F, 8.0F, 0xFFFFFF, true, 120);
				Vfx.shake(world, g, 100000, 5.0F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
				Detonations.blast(world, g, r * 1.2, 10000.0F, ModDamageTypes.ANNIHILATION, owner(), r * 0.5, 6.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), r, false, ShellJob.Pattern.NONE,
							ModBlocks.VOID_REMNANT.getDefaultState(), 0.25F));
				}
				EnergyFields.emit(world, EnergyFields.Energy.VOID, g, r, 1200, owner());
			}
			if (t == 460) {
				Vfx.screen(world, g, 100000, 3, 0.5F, 200);
				Vfx.glitch(world, g, r, 0xFF40FF, 200);
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.vanish(rising);
		}
	}
}
