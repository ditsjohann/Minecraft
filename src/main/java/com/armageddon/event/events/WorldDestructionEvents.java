package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.ShellJob;
import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.entity.DevourerEntity;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * WORLD DESTRUCTION tier. Every event here opens with letterbox bars and a full-screen title,
 * hits with anime impact frames, FOV punches and shake, and then really does reshape the map
 * (always within the max-radius gamerule and protected regions).
 */
public final class WorldDestructionEvents {
	private WorldDestructionEvents() {
	}

	static void open(ServerWorld w, Vec3d c, String key, int barTicks) {
		CelestialEvents.announce(w, c, key);
		Vfx.cutscene(w, c, barTicks);
	}

	/** A rapid burst of impact frames, one every other tick: styles 0 ink, 1 inverted, 2 red, 3 white. */
	static void frames(ServerWorld w, Vec3d c, int... styles) {
		for (int i = 0; i < styles.length; i++) {
			final int style = styles[i];
			if (i == 0) {
				Vfx.impact(w, c, style, 2);
			} else {
				DetonationManager.schedule(w, i * 2, () -> Vfx.impact(w, c, style, 2));
			}
		}
	}

	static int hsv(float h) {
		return MathHelper.hsvToRgb(((h % 1.0F) + 1.0F) % 1.0F, 0.7F, 1.0F);
	}

	static Vec3d around(ServerWorld w, Vec3d c, double radius) {
		double a = w.random.nextDouble() * Math.PI * 2;
		double d = Math.sqrt(w.random.nextDouble()) * radius;
		return Kit.ground(w, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
	}

	// ====================================================================== MOONFALL

	/** MOONFALL: the moon leaves orbit. It takes fifteen seconds to arrive. It does not leave. */
	public static class Moonfall extends WorldEvent {
		private static final int FALL = 300;
		private static final float MOON_R = 56.0F;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				open(world, g, "block.armageddon.moonfall", 440);
				Vfx.moon(world, g, MOON_R, 380.0F, FALL);
				Vfx.skyTint(world, g, 100000, 0x080818, 0.6F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
			}
			if (t > 0 && t < FALL && t % 20 == 0) {
				Vfx.shake(world, g, 4096, 0.3F + 2.2F * t / FALL);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F + 0.4F * t / FALL);
			}
			if (t == 100 || t == 200) {
				Vfx.fov(world, g, 12.0F, 30);
			}
			if (t == 150) {
				Kit.title(world, g, 100000, Text.translatable("message.armageddon.moonfall.title").formatted(Formatting.WHITE, Formatting.BOLD),
						Text.translatable("message.armageddon.moonfall.sub").formatted(Formatting.GRAY));
			}
			if (t == 240) {
				frames(world, g, 0);
			}
			if (t == 262) {
				frames(world, g, 1, 0);
			}
			if (t == 280) {
				Vfx.fov(world, g, 30.0F, 24);
				frames(world, g, 2, 1, 0);
			}
			if (t >= 286 && t < FALL && t % 3 == 0) {
				Vfx.screen(world, g, 100000, 4, 0.5F, 2);
			}
			if (t == FALL) {
				frames(world, g, 3, 1, 0, 2, 3);
				Vfx.screen(world, g, 100000, 1, 1.0F, 40);
				Vfx.fov(world, g, 50.0F, 40);
				Vfx.shake(world, g, 100000, 7.0F);
				Vfx.layeredBlast(world, g, 45.0F, 0xFFFFFF);
				Vfx.dome(world, g, 170.0F, 0xFFE0B0, 100);
				Vfx.ring(world, g, 340.0F, 8.0F, 0xFFFFFF, true, 120);
				Vfx.ring(world, g, 220.0F, 6.0F, 0xFF8030, true, 100);
				Vfx.pillar(world, g, 14.0F, 0xFFB060, false, 120);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.4F);
				Detonations.blast(world, g, 300, 9000.0F, ModDamageTypes.ANNIHILATION, owner(), 160, 11.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 130.0F), CraterJob.Mode.NUKE));
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g.add(0, 40, 0)), 90, false, ShellJob.Pattern.NONE, null, 0.0F));
				}
				Detonations.debris(world, g, 90, 500);
				for (int i = 0; i < 18; i++) {
					Vec3d d = Kit.dir(i * 20.0F + world.random.nextFloat() * 10);
					Kit.fissure(world, g.add(d.multiply(40)), d, 130, 12, 4, Blocks.LAVA.getDefaultState());
				}
				EnergyFields.emit(world, EnergyFields.Energy.ANTIMATTER, g, 60, 600, owner());
			}
			if (t > FALL && t < FALL + 220 && t % 2 == 0) {
				Kit.meteor(world, around(world, g, 190), owner());
			}
			if (t == FALL + 20) {
				Vfx.skyTint(world, g, 100000, 0x303030, 0.9F);
			}
			if (t > FALL + 20 && t % 10 == 0 && t < FALL + 220) {
				Vfx.ring(world, g, 60.0F + (t - FALL) * 2, 4.0F, 0xA09080, true, 30);
			}
			return t > FALL + 230;
		}
	}

	// ====================================================================== BIG BANG

	/** BIG BANG: silence and dark. A point. Then everything, outward, all at once. */
	public static class BigBang extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d point = g.add(0, 8, 0);
			if (t == 0) {
				open(world, g, "block.armageddon.big_bang", 360);
				Vfx.screen(world, g, 100000, 2, 0.92F, 70);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.2F);
			}
			if (t == 2) {
				Vfx.charge(world, host.getId(), point, 60.0F, 0xFFFFFF, 80);
			}
			if (t < 86 && t % 20 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 0.5F - t * 0.003F);
				Vfx.shake(world, g, 512, 0.5F + t * 0.02F);
			}
			if (t == 70 || t == 76 || t == 80 || t == 83 || t == 85) {
				frames(world, g, (t & 1) == 0 ? 1 : 0);
				Vfx.fov(world, g, 6.0F, 6);
			}
			if (t == 86) {
				Vfx.orb(world, point, 0.3F, 0.3F, 0xFFFFFF, 0, 6);
			}
			if (t == 90) {
				frames(world, g, 3, 1, 2, 0, 3);
				Vfx.screen(world, g, 100000, 1, 1.0F, 40);
				Vfx.screen(world, g, 100000, 4, 0.8F, 4);
				Vfx.fov(world, g, 55.0F, 50);
				Vfx.shake(world, g, 100000, 7.0F);
				Vfx.orb(world, point, 0.5F, 130.0F, 0xFFFFFF, 2, 60);
				Vfx.layeredBlast(world, point, 50.0F, 0xFFFFFF);
				for (int k = 0; k < 8; k++) {
					final int kk = k;
					DetonationManager.schedule(world, k * 4, () -> {
						Vfx.ring(world, point, 150.0F, 3.0F, hsv(kk / 8.0F), false, 50);
						Vfx.dome(world, point, 100.0F + kk * 6, hsv(kk / 8.0F + 0.5F), 40);
					});
				}
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 1.4F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
				Detonations.blast(world, g, 240, 9000.0F, ModDamageTypes.ANNIHILATION, owner(), 120, 10.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(point), 170, false, ShellJob.Pattern.NONE,
							ModBlocks.LUMINOUS_GLASS.getDefaultState(), 0.2F));
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 90.0F), CraterJob.Mode.ANTIMATTER));
				}
			}
			if (t == 120) {
				Vfx.skyTint(world, g, 100000, 0xFFB0FF, 0.8F);
			}
			if (t == 150) {
				Vfx.screen(world, g, 100000, 0, 0.6F, 60);
			}
			if (t == 200) {
				Vfx.skyTint(world, g, 100000, 0x6020A0, 0.6F);
			}
			if (t > 100 && t < 300) {
				if (t % 2 == 0) {
					world.spawnParticles(ParticleTypes.END_ROD, g.x, g.y + 25, g.z, 30, 60, 25, 60, 0.04);
					world.spawnParticles(ParticleTypes.FIREWORK, g.x, g.y + 20, g.z, 12, 60, 20, 60, 0.06);
				}
				if (t % 20 == 0) {
					Vfx.pillar(world, around(world, g, 70), 3.0F, hsv(world.random.nextFloat()), true, 60);
				}
			}
			return t > 340;
		}
	}

	// ====================================================================== WORLD EATER

	/** WORLD EATER: six rifts open on a ring; six Devourers crawl inward eating the land; a black hole finishes it. */
	public static class WorldEater extends WorldEvent {
		private static final int RING = 38;

		private Vec3d rift(int i) {
			double a = i * Math.PI / 3;
			return Kit.ground(world, center().x + Math.cos(a) * RING, center().z + Math.sin(a) * RING);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				open(world, g, "block.armageddon.world_eater", 340);
				Vfx.skyTint(world, g, 100000, 0x1A0830, 0.8F);
				Vfx.ring(world, g, RING + 6.0F, 2.0F, 0x8030FF, true, 40);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
			}
			if (t >= 30 && t < 102 && (t - 30) % 12 == 0) {
				int i = (t - 30) / 12;
				Vec3d p = rift(i);
				Vfx.portal(world, p.add(0, 3, 0), new Vec3d(0, 1, 0), 6.0F, 0xA040FF, 100);
				frames(world, g, i % 2 == 0 ? 0 : 1);
				Vfx.fov(world, g, 14.0F, 14);
				Vfx.shake(world, g, 1024, 2.0F + i * 0.3F);
				Kit.sound(world, p, ModSounds.BLACK_HOLE, 32.0F, 0.5F);
			}
			if (t == 120) {
				for (int i = 0; i < 6; i++) {
					Vec3d p = rift(i);
					Vec3d dir = g.subtract(p).multiply(1, 0, 1).normalize().multiply(0.32);
					DevourerEntity.spawn(world, p.add(0, 2, 0), dir, owner());
					DevourerEntity.spawn(world, p.add(world.random.nextGaussian() * 4, 2, world.random.nextGaussian() * 4), dir.multiply(0.8), owner());
				}
				Vfx.screen(world, g, 100000, 2, 0.5F, 40);
				frames(world, g, 2, 0);
			}
			if (t >= 200 && t < 300 && t % 10 == 0) {
				Detonations.pull(world, g, 70, 0.4);
				Vfx.shake(world, g, 1024, 2.0F + (t - 200) * 0.03F);
			}
			if (t == 260) {
				Vfx.orb(world, g.add(0, 10, 0), 2.0F, 22.0F, 0x000000, 1, 100);
				Vfx.charge(world, host.getId(), g.add(0, 10, 0), 50.0F, 0xA040FF, 40);
			}
			if (t == 300) {
				BlackHoleEntity.spawn(world, g.add(0, 8, 0), 22.0F, 420, true, owner(), 14000);
				frames(world, g, 1, 0, 2, 1);
				Vfx.screen(world, g, 100000, 4, 0.9F, 3);
				Vfx.screen(world, g, 100000, 2, 0.8F, 30);
				Vfx.fov(world, g, 40.0F, 40);
				Vfx.shake(world, g, 100000, 6.0F);
				Vfx.ring(world, g, 140.0F, 5.0F, 0xA040FF, true, 60);
				Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.4F);
				Detonations.blast(world, g, 110, 3000.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 7.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new ShellJob(world, BlockPos.ofFloored(g), 64, true, ShellJob.Pattern.HEX,
							ModBlocks.VOID_REMNANT.getDefaultState(), 0.4F));
				}
			}
			return t > 340;
		}
	}

	// ====================================================================== CHRONO BREAK

	/** CHRONO BREAK: time stops, a sphere of world is erased, and then rewinds back into existence from the outside in. */
	public static class ChronoBreak extends WorldEvent {
		private static final int ERASE_AT = 100;
		private final Map<Entity, Boolean> frozen = new HashMap<>();
		private final Map<ServerPlayerEntity, Vec3d> players = new HashMap<>();
		private int radius;
		private int restoreAt;

		private void freeze(Vec3d g, int r) {
			for (Entity e : Kit.entities(world, g, r, x -> !(x instanceof EventEntity) && !(x instanceof PlayerEntity) && !Kit.creativeOrSpectator(x))) {
				if (!frozen.containsKey(e)) {
					frozen.put(e, e instanceof MobEntity m && !m.isAiDisabled());
					if (e instanceof MobEntity m) {
						m.setAiDisabled(true);
					}
				}
				e.setVelocity(Vec3d.ZERO);
				e.velocityModified = true;
				e.setNoGravity(true);
			}
		}

		private void release() {
			for (Map.Entry<Entity, Boolean> en : frozen.entrySet()) {
				Entity e = en.getKey();
				if (e.isAlive()) {
					e.setNoGravity(false);
					if (en.getValue() && e instanceof MobEntity m) {
						m.setAiDisabled(false);
					}
				}
			}
			frozen.clear();
		}

		private void eraseShell(Vec3d g, int r) {
			BlockPos b = BlockPos.ofFloored(g);
			long inner = (long) r * r, outer = (long) (r + 1) * (r + 1);
			int expiry = Math.max(2, restoreAt + (radius - r) * 2 - age());
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int x = b.getX() + dx, z = b.getZ() + dz;
					if (!world.isChunkLoaded(x >> 4, z >> 4)) {
						continue;
					}
					for (int dy = -r; dy <= r; dy++) {
						long d2 = (long) dx * dx + (long) dy * dy + (long) dz * dz;
						if (d2 < inner || d2 >= outer) {
							continue;
						}
						p.set(x, b.getY() + dy, z);
						if (!world.getBlockState(p).isAir()) {
							TemporaryBlocks.place(world, p.toImmutable(), Blocks.AIR.getDefaultState(), expiry);
						}
					}
				}
			}
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				radius = Math.min(38, Destruction.radius(world, 38));
				restoreAt = ERASE_AT + radius + 90;
				open(world, g, "block.armageddon.chrono_break", restoreAt + radius * 2 + 40);
				Vfx.screen(world, g, 100000, 0, 1.0F, restoreAt + radius * 2);
				frames(world, g, 1, 0);
				Vfx.fov(world, g, 25.0F, 20);
				Vfx.grid(world, g.add(0, 0.1, 0), new Vec3d(0, 1, 0), radius, 0x50FFD0, 2, ERASE_AT + radius + 80);
				Vfx.dome(world, g, radius + 4, 0x50FFD0, 60);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.3F);
			}
			if (t <= restoreAt) {
				freeze(g, radius + 24);
			}
			if (t < ERASE_AT) {
				// Clock hands sweep while time is frozen.
				double hourA = t * 0.07, secA = t * 0.45;
				Vec3d hub = g.add(0, 2, 0);
				Vfx.beam(world, hub, hub.add(Math.cos(hourA) * radius * 0.6, 0, Math.sin(hourA) * radius * 0.6), 0.8F, 0x50FFD0, 2);
				Vfx.beam(world, hub, hub.add(Math.cos(secA) * radius * 0.9, 0, Math.sin(secA) * radius * 0.9), 0.3F, 0xFFFFFF, 2);
				if (t % 25 == 0) {
					Kit.sound(world, g, ModSounds.BOMB_BEEP, 32.0F, 1.5F);
				}
			}
			if (t == ERASE_AT) {
				for (ServerPlayerEntity p : world.getPlayers()) {
					if (p.squaredDistanceTo(g) <= (radius + 8) * (radius + 8)) {
						players.put(p, p.getPos());
						p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, restoreAt + radius * 2 - ERASE_AT, 0, false, false));
					}
				}
				Vfx.screen(world, g, 100000, 2, 0.8F, 10);
				Vfx.screen(world, g, 100000, 3, 1.0F, 20);
				frames(world, g, 0, 1, 0);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.6F);
				Vfx.shake(world, g, 1024, 4.0F);
			}
			if (t >= ERASE_AT && t < ERASE_AT + radius) {
				int r = t - ERASE_AT;
				if (Destruction.enabled(world)) {
					eraseShell(g, r);
				}
				if (r % 3 == 0) {
					Vfx.ring(world, g, r + 1.0F, 1.0F, 0x50FFD0, false, 8);
				}
			}
			if (t == 150) {
				frames(world, g, 2);
				Vfx.fov(world, g, 20.0F, 20);
			}
			if (t == restoreAt) {
				for (Map.Entry<ServerPlayerEntity, Vec3d> en : players.entrySet()) {
					Vec3d home = en.getValue();
					en.getKey().teleport(world, home.x, home.y, home.z, en.getKey().getYaw(), en.getKey().getPitch());
				}
				Vfx.glitch(world, g, radius, 0x50FFD0, radius * 2 + 20);
				frames(world, g, 1, 0, 1);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.4F);
			}
			if (t > restoreAt && t < restoreAt + radius * 2 && t % 6 == 0) {
				Vfx.screen(world, g, 100000, 4, 0.5F, 2);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 32.0F, 0.5F + (t - restoreAt) * 0.02F);
			}
			if (t == restoreAt + radius * 2 + 4) {
				release();
				Vfx.screen(world, g, 100000, 1, 0.7F, 4);
				frames(world, g, 3);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 1.5F);
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			release();
		}
	}

	// ====================================================================== RAGNAROK

	/** RAGNAROK: the sky splits, lightning hammers the land, fissures run with lava, a giant blade circles, and then the end. */
	public static class Ragnarok extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				open(world, g, "block.armageddon.ragnarok", 470);
				Vfx.skyTint(world, g, 100000, 0x600000, 1.0F);
				Vfx.skyCrack(world, g, 1.0F, 0xFF3010, 2, 420);
				Vfx.shake(world, g, 4096, 2.0F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			}
			if (t >= 20 && t < 240) {
				Vec3d hit = around(world, g, 110);
				Vec3d sky = hit.add(world.random.nextGaussian() * 8, 60, world.random.nextGaussian() * 8);
				Vfx.beam(world, sky, hit, 1.6F, 0xFFFFFF, 6);
				Vfx.flash(world, hit, 0.3F, 128, 0xFFE0C0);
				Kit.sound(world, hit, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 8.0F, 0.6F);
				Kit.damage(world, hit, 4, 18.0F, ModDamageTypes.ELECTROCUTION, null, owner());
				if (world.random.nextInt(3) == 0) {
					Destruction.explode(world, null, hit.x, hit.y, hit.z, 3.0F, true);
				}
			}
			if (t >= 60 && t < 220 && t % 10 == 0) {
				Vec3d d = Kit.dir(world.random.nextFloat() * 360);
				Kit.fissure(world, g.add(d.multiply(3)), d, 120, 12, 4, Blocks.LAVA.getDefaultState());
				Vfx.shake(world, g, 2048, 3.0F);
			}
			if (t >= 100 && t < 340 && t % 2 == 0) {
				Kit.meteor(world, around(world, g, 110), owner());
			}
			if (t == 60 || t == 150 || t == 260) {
				frames(world, g, 2, 0);
				Vfx.fov(world, g, 16.0F, 16);
			}
			if (t == 236) {
				Kit.title(world, g, 100000, Text.translatable("message.armageddon.ragnarok.title").formatted(Formatting.DARK_RED, Formatting.BOLD),
						Text.translatable("message.armageddon.ragnarok.sub").formatted(Formatting.RED));
			}
			if (t >= 240 && t < 360) {
				double a = (t - 240) * Math.PI * 2 / 120.0;
				Vec3d pos = Kit.ground(world, g.x + Math.cos(a) * 70, g.z + Math.sin(a) * 70);
				Vfx.beam(world, pos.add(0, 250, 0), pos, 5.0F, 0xFFFFFF, 3);
				Vfx.pillar(world, pos, 5.0F, 0xFF4010, false, 6);
				if (Destruction.enabled(world)) {
					Kit.sphere(world, BlockPos.ofFloored(pos), 5.0, Blocks.AIR.getDefaultState());
				}
				Detonations.blast(world, pos, 8, 300.0F, ModDamageTypes.PLASMA, owner(), 6, 2.0);
				if (t % 4 == 0) {
					Kit.sound(world, pos, ModSounds.ORBITAL_STRIKE, 24.0F, 0.5F);
					Vfx.shake(world, g, 2048, 3.5F);
				}
			}
			if (t == 380) {
				for (int i = 0; i < 16; i++) {
					double a = i * Math.PI / 8;
					Vec3d p = g.add(Math.cos(a) * 60, 0, Math.sin(a) * 60);
					Vfx.beam(world, p.add(0, 120, 0), g.add(0, 2, 0), 3.0F, 0xFF3010, 30);
				}
				frames(world, g, 3, 1, 0, 2);
				Vfx.layeredBlast(world, g, 34.0F, 0xFF2010);
				Vfx.screen(world, g, 100000, 1, 1.0F, 20);
				Vfx.fov(world, g, 60.0F, 40);
				Vfx.shake(world, g, 100000, 7.0F);
				Vfx.dome(world, g, 140.0F, 0xFF3010, 80);
				Vfx.ring(world, g, 260.0F, 7.0F, 0xFF6040, true, 90);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.4F);
				Detonations.blast(world, g, 220, 9000.0F, ModDamageTypes.ANNIHILATION, owner(), 140, 10.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 90.0F), CraterJob.Mode.NUKE));
				}
				Detonations.debris(world, g, 80, 450);
			}
			return t > 430;
		}
	}
}
