package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.city.CosmosTerrain;
import com.armageddon.destruction.city.MawTerrain;
import com.armageddon.destruction.city.MoonTerrain;
import com.armageddon.destruction.city.RagnarokTerrain;
import com.armageddon.destruction.city.TimeScarTerrain;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.detonation.ShellJob;
import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.entity.DevourerEntity;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.CityShapes;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CITY DESTRUCTION tier. Every event opens with letterbox bars, then reshapes the map in its own
 * way, and none of them end the same: a moon that stays, a newborn solar system, a bitten-out city
 * around a throat, a rewind that leaves time scars, a world split into fire and frost. All block
 * edits go through a budgeted {@link BlockQueue} and respect the max-radius gamerule, protected
 * regions and the block-damage rule.
 */
public final class CityDestructionEvents {
	private CityDestructionEvents() {
	}

	/** Effect ids for {@link FxType#CITY_FX}; the client's CityFx switches on these. */
	public static final int FX_MOON_TIDE = 1, FX_INFLATION = 2, FX_JAWS = 3, FX_MAW = 4, FX_CHRONO_DIAL = 5, FX_SERPENT = 6, FX_WORLD_TREE = 7;

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

	/**
	 * MOONFALL: cold, slow and inevitable. Tides pull the loose ground up toward it for fifteen
	 * seconds; it lands without a flash, sinks in, and stays: a pitted grey moon half-buried in a ring
	 * of upthrust crust, hollow with amethyst, the land around it grey with regolith and pocked by the
	 * fragments that followed it down.
	 */
	public static class Moonfall extends WorldEvent {
		private static final int FALL = CityShapes.MOON_FALL, END = FALL + 170;
		private final List<FallingBlockEntity> tide = new ArrayList<>();
		private final List<BlockPos> pocks = new ArrayList<>();
		private BlockQueue queue;
		private BlockPos ground;
		private int r;
		private long seed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				ground = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z));
				r = (int) CityShapes.moonRadius(Destruction.radius(world, 68.0F));
				seed = ground.asLong();
				open(world, g, "block.armageddon.moonfall", FALL + 60);
				Vfx.moon(world, g, r, 380.0F, FALL);
				Vfx.custom(world, FxType.CITY_FX, Vec3d.ofBottomCenter(ground.up()), g, r, 0xC8CCD8, FX_MOON_TIDE, FALL, 0, END, Vfx.FAR);
				Vfx.skyTint(world, g, 100000, 0x080818, 0.6F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
				for (int i = 0; i < 14; i++) {
					double a = CityShapes.hash(seed, i + 200) * MathHelper.TAU;
					double d = r * (1.3 + 1.3 * CityShapes.hash(seed, i + 220));
					pocks.add(BlockPos.ofFloored(g.x + Math.cos(a) * d, g.y, g.z + Math.sin(a) * d));
				}
			}
			if (t > 0 && t < FALL && t % 20 == 0) {
				Vfx.shake(world, g, 4096, 0.2F + 1.4F * t / FALL);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.25F + 0.3F * t / FALL);
			}
			// The tide: loose ground drifts up toward the approaching moon.
			if (t >= 140 && t < FALL - 10 && t % 5 == 0 && tide.size() < 160) {
				tide.addAll(Kit.lift(world, g, r * 2.2, 6, true, true));
			}
			if (t < FALL) {
				double pull = Math.max(0, (t - 140) / (double) (FALL - 140));
				for (FallingBlockEntity fb : tide) {
					if (fb.isAlive()) {
						Vec3d to = new Vec3d(g.x - fb.getX(), 0, g.z - fb.getZ()).multiply(0.004);
						Kit.velocity(fb, to.add(0, 0.04 + 0.22 * pull * pull, 0));
					}
				}
			}
			if (t == FALL) {
				Kit.vanish(tide);
				Vfx.shake(world, g, 100000, 3.0F);
				Vfx.fov(world, g, 22.0F, 50);
				Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.2F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.22F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.3F);
				Detonations.blast(world, g, r * 2.4, 2500.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 6.0);
				if (Destruction.enabled(world)) {
					MoonTerrain.moon(world, queue, ground, r, seed);
					MoonTerrain.berm(world, queue, ground, r, seed);
					MoonTerrain.regolith(world, queue, ground, r, seed);
				}
			}
			if (t == FALL + 24) {
				Vfx.skyTint(world, g, 100000, 0x0C1020, 0.85F);
			}
			// Fragments that followed it down, one after another, each leaving its own pockmark.
			int k = (t - FALL - 12) / 9;
			if (t > FALL + 12 && (t - FALL - 12) % 9 == 0 && k < pocks.size()) {
				BlockPos p = pocks.get(k);
				Vec3d hit = Kit.ground(world, p.getX() + 0.5, p.getZ() + 0.5);
				Vec3d from = hit.add((world.random.nextDouble() - 0.5) * 60, 160, (world.random.nextDouble() - 0.5) * 60);
				Vfx.streak(world, from, hit, 1.2F, 0xB8BCC8, 8);
				DetonationManager.schedule(world, 8, () -> {
					Vfx.ring(world, hit.add(0, 0.3, 0), 9.0F, 1.2F, 0xA8A8B0, true, 24);
					Kit.sound(world, hit, SoundEvents.BLOCK_GRAVEL_BREAK, 16.0F, 0.4F);
					Kit.damage(world, hit, 6, 30.0F, ModDamageTypes.SHRAPNEL, null, owner());
					if (Destruction.enabled(world)) {
						MoonTerrain.pock(world, queue, p.getX(), p.getZ(), 3 + world.random.nextInt(4), seed + 300 + p.asLong());
					}
				});
			}
			// It keeps settling: low groans and dust slumping off its flanks.
			if (t > FALL && t % 30 == 0) {
				Vfx.shake(world, g, 2048, 0.6F);
				Vfx.ring(world, g.add(0, 0.3, 0), r + 6.0F, 2.0F, 0x9A9AA4, true, 30);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 48.0F, 0.2F);
			}
			queue.drain(BlockQueue.PER_TICK);
			return t > END;
		}

		@Override
		public void finish() {
			Kit.vanish(tide);
			if (queue != null) {
				queue.handOff();
			}
		}
	}

	// ====================================================================== BIG BANG

	/**
	 * BIG BANG creates rather than destroys. Darkness and silence, a point, then everything outward at
	 * once; as the light cools, matter condenses into a newborn system above the city: small worlds of
	 * geode, magma, ocean, jungle, ice, copper and end stone, nebulae of stained glass and clusters of
	 * stars, over ground painted with the arms of the new galaxy.
	 */
	public static class BigBang extends WorldEvent {
		private static final int BANG = CityShapes.BANG, END = 300;
		private BlockQueue queue;
		private Vec3d point;
		private double scale;
		private long seed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				point = g.add(0, 8, 0);
				scale = Destruction.radius(world, 60.0F);
				seed = BlockPos.ofFloored(point).asLong();
				open(world, g, "block.armageddon.big_bang", 330);
				Vfx.screen(world, g, 100000, 2, 0.92F, BANG - 16);
				Vfx.custom(world, FxType.CITY_FX, point, g, (float) scale, 0xFFFFFF, FX_INFLATION, BANG, 0, END, Vfx.FAR);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.2F);
			}
			if (t < BANG - 4 && t % 20 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 0.5F - t * 0.003F);
				Vfx.shake(world, g, 512, 0.3F + t * 0.006F);
			}
			if (t == BANG) {
				frames(world, g, 1);
				Vfx.fov(world, g, 30.0F, 40);
				Vfx.shake(world, g, 100000, 2.0F);
				Vfx.skyTint(world, g, 100000, 0x2A0A50, 0.85F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 1.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 1.2F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
				// Everything is flung outward: a shove, not a crater.
				Detonations.blast(world, point, scale * 1.2, 600.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 4.0);
				if (Destruction.enabled(world)) {
					BlockPos c = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z));
					CosmosTerrain.scar(world, queue, c, 8, seed);
					CosmosTerrain.galaxy(world, queue, c, (int) Math.min(56, scale * 0.9), seed);
					CosmosTerrain.stars(queue, point, scale, seed);
				}
			}
			for (int i = 0; i < CityShapes.PLANETS; i++) {
				if (t == CityShapes.planetFormAt(i)) {
					Vec3d at = point.add(CityShapes.planetOffset(seed, i, scale));
					int type = CityShapes.planetType(seed, i);
					if (Destruction.enabled(world)) {
						CosmosTerrain.planet(queue, BlockPos.ofFloored(at), CityShapes.planetRadius(seed, i, scale), type,
								CityShapes.planetRinged(seed, i), seed + i);
					}
					Vfx.light(world, at, 0.9F, CityShapes.planetColor(type));
					Kit.sound(world, at, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 48.0F, 0.5F + i * 0.18F);
					Kit.sound(world, at, SoundEvents.BLOCK_BEACON_POWER_SELECT, 32.0F, 0.6F + i * 0.1F);
				}
			}
			for (int k = 0; k < CityShapes.NEBULAE; k++) {
				if (t == CityShapes.nebulaAt(k) && Destruction.enabled(world)) {
					Vec3d at = point.add(CityShapes.nebulaOffset(seed, k, scale));
					CosmosTerrain.nebula(queue, BlockPos.ofFloored(at), (int) Math.max(6, scale * 0.16), k, seed);
					Kit.sound(world, at, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F, 0.4F + k * 0.2F);
				}
			}
			if (t > BANG && t < BANG + 160 && t % 3 == 0) {
				world.spawnParticles(ParticleTypes.END_ROD, point.x, point.y + scale * 0.4, point.z, 14, scale * 0.6, scale * 0.3, scale * 0.6, 0.02);
			}
			queue.drain(BlockQueue.PER_TICK);
			return t > END;
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.handOff();
			}
		}
	}

	// ====================================================================== WORLD EATER

	/**
	 * WORLD EATER: something under the city is hungry. A heartbeat; then jaws of bone burst out of the
	 * ground and bite, again and again, spiralling inward, each bite leaving a sawtooth bowl soaked in
	 * sculk. Then the throat opens in the middle and swallows what's left.
	 */
	public static class WorldEater extends WorldEvent {
		private static final int GULP = CityShapes.gulpAt(), END = GULP + 140, THROAT = 14;
		private BlockQueue queue;
		private BlockPos ground;
		private double reach;
		private long seed;

		private Vec3d bite(int i) {
			Vec3d o = CityShapes.biteOffset(i, reach);
			return Kit.ground(world, center().x + o.x, center().z + o.z);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				ground = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z));
				reach = Destruction.radius(world, 56.0F);
				seed = ground.asLong();
				open(world, g, "block.armageddon.world_eater", GULP + 60);
				Vfx.skyTint(world, g, 100000, 0x14061E, 0.85F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_EMERGE, 64.0F, 0.5F);
			}
			if (t < GULP && t % 20 == 0) {
				// The heartbeat speeds up as it gets closer.
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 64.0F, 0.6F + t * 0.0015F);
				Vfx.shake(world, g, 1024, 0.4F);
			}
			for (int i = 0; i < CityShapes.BITES; i++) {
				int at = CityShapes.biteAt(i);
				if (t == at - CityShapes.JAW_LEAD) {
					Vec3d p = bite(i);
					double rb = CityShapes.biteRadius(i, reach);
					double toward = Math.atan2(g.z - p.z, g.x - p.x);
					Vfx.custom(world, FxType.CITY_FX, p, g, (float) rb, 0xF0E8D8, FX_JAWS, (float) toward, CityShapes.JAW_LEAD,
							CityShapes.JAW_LEAD + 34, Vfx.FAR);
					Kit.sound(world, p, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 32.0F, 0.3F);
				}
				if (t == at) {
					Vec3d p = bite(i);
					double rb = CityShapes.biteRadius(i, reach);
					double toward = Math.atan2(g.z - p.z, g.x - p.x);
					Kit.sound(world, p, SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, 48.0F, 0.4F);
					Kit.sound(world, p, SoundEvents.ENTITY_RAVAGER_ATTACK, 48.0F, 0.35F);
					Vfx.shake(world, p, 1024, 1.3F);
					Vfx.fov(world, g, 6.0F, 10);
					// Whatever stood inside the bite is simply gone.
					for (LivingEntity e : Kit.living(world, p, rb)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 60.0F);
						}
					}
					if (Destruction.enabled(world)) {
						MawTerrain.bite(world, queue, BlockPos.ofFloored(p).down(), rb, toward, seed + i * 31L);
					}
				}
			}
			if (t == GULP - 24) {
				Vfx.custom(world, FxType.CITY_FX, Vec3d.ofBottomCenter(ground.up()), g, THROAT, 0x30D0C0, FX_MAW, 0, 0, END - GULP + 24, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 64.0F, 0.5F);
			}
			if (t == GULP) {
				frames(world, g, 2, 2);
				Vfx.shake(world, g, 100000, 2.5F);
				Vfx.fov(world, g, 18.0F, 30);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, 64.0F, 0.25F);
				if (Destruction.enabled(world)) {
					int depth = Math.min(70, ground.getY() - world.getBottomY() - 4);
					MawTerrain.throat(world, queue, ground, THROAT, depth, seed);
					MawTerrain.gnaw(world, queue, ground, reach, seed);
				}
			}
			if (t > GULP && t < GULP + 120 && t % 4 == 0) {
				Detonations.pull(world, g, reach, 0.22);
				for (LivingEntity e : Kit.living(world, g.add(0, -6, 0), THROAT)) {
					if (e.getY() < ground.getY() - 2 && !Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 40.0F);
					}
				}
			}
			if (t == GULP + 36) {
				Kit.sound(world, g, SoundEvents.ENTITY_PLAYER_BURP, 64.0F, 0.35F);
			}
			queue.drain(BlockQueue.PER_TICK);
			return t > END;
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.handOff();
			}
		}
	}

	// ====================================================================== CHRONO BREAK

	/**
	 * CHRONO BREAK: time stops under a dial of light, a sphere of the world is erased, then it rewinds
	 * back into existence from the outside in. Almost: the rewind slips, and the ground is left banded
	 * with time scars, ringed by copper hour stones of different ages around a sundial.
	 */
	public static class ChronoBreak extends WorldEvent {
		private static final int ERASE_AT = 100;
		private final Map<Entity, Boolean> frozen = new HashMap<>();
		private final Map<Entity, Boolean> floating = new HashMap<>();
		private final Map<ServerPlayerEntity, Vec3d> players = new HashMap<>();
		private int radius;
		private int restoreAt;
		private BlockQueue queue;
		private Vec3d fwd;

		private void freeze(Vec3d g, int r) {
			for (Entity e : Kit.entities(world, g, r, x -> !(x instanceof EventEntity) && !(x instanceof PlayerEntity) && !Kit.creativeOrSpectator(x))) {
				if (!frozen.containsKey(e)) {
					frozen.put(e, e instanceof MobEntity m && !m.isAiDisabled());
					floating.put(e, e.hasNoGravity());
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
					e.setNoGravity(floating.getOrDefault(e, false));
					if (en.getValue() && e instanceof MobEntity m) {
						m.setAiDisabled(false);
					}
				}
			}
			frozen.clear();
			floating.clear();
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
				queue = new BlockQueue(world);
				fwd = Kit.dir(data.getFloat("Yaw"));
				open(world, g, "block.armageddon.chrono_break", restoreAt + radius * 2 + 40);
				Vfx.screen(world, g, 100000, 0, 1.0F, restoreAt + radius * 2);
				Vfx.fov(world, g, 14.0F, 20);
				Vfx.custom(world, FxType.CITY_FX, g, fwd, radius, 0x50FFD0, FX_CHRONO_DIAL, ERASE_AT, restoreAt, restoreAt + radius * 2 + 70, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.3F);
			}
			if (t <= restoreAt) {
				freeze(g, radius + 24);
			}
			if (t < ERASE_AT && t % 10 == 0) {
				// The dial (client side) sweeps; the world only hears the ticking.
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 32.0F, t % 20 == 0 ? 0.7F : 0.9F);
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
				frames(world, g, 1);
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
			if (t >= restoreAt && t <= restoreAt + radius * 2 + 4) {
				// Hold players at home until the last shell is back, or they fall into the hole and get buried by it.
				for (Map.Entry<ServerPlayerEntity, Vec3d> en : players.entrySet()) {
					if (!en.getKey().isAlive()) {
						continue;
					}
					Vec3d home = en.getValue();
					en.getKey().teleport(world, home.x, home.y, home.z, en.getKey().getYaw(), en.getKey().getPitch());
					Kit.velocity(en.getKey(), Vec3d.ZERO);
					en.getKey().fallDistance = 0;
				}
			}
			if (t == restoreAt) {
				Vfx.glitch(world, g, radius, 0x50FFD0, radius * 2 + 20);
				frames(world, g, 1);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.4F);
			}
			if (t > restoreAt && t < restoreAt + radius * 2 && t % 6 == 0) {
				Vfx.screen(world, g, 100000, 4, 0.5F, 2);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 32.0F, 0.5F + (t - restoreAt) * 0.02F);
			}
			if (t == restoreAt + radius * 2 + 4) {
				release();
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 1.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.6F);
				if (Destruction.enabled(world)) {
					BlockPos c = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z));
					long seed = c.asLong();
					TimeScarTerrain.bands(world, queue, c, radius + 10, seed);
					TimeScarTerrain.hours(world, queue, c, radius + 4, fwd);
					TimeScarTerrain.sundial(world, queue, c, radius, fwd);
				}
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			release();
			if (queue != null) {
				queue.handOff();
			}
		}
	}

	// ====================================================================== RAGNAROK

	/**
	 * RAGNAROK: the horn sounds and the world tree burns across the sky. The world serpent coils in
	 * around the city, burning its path into the ground; the horn sounds again, the land splits along
	 * a chasm, and the two halves become fire and frost. Then the tree falls.
	 */
	public static class Ragnarok extends WorldEvent {
		private static final int END = 440;
		private BlockQueue queue;
		private Vec3d fwd;
		private double reach, phase, lastHead = -1;
		private long seed;
		private long[] columns;
		private int columnCursor;

		/** Every surface column in range, ordered by distance from the dividing line so the halves spread outward. */
		private void planColumns(Vec3d g) {
			int r = (int) reach;
			Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
			List<Long> list = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz > r * r) {
						continue;
					}
					double d = Math.abs(dx * side.x + dz * side.z);
					list.add(((long) (d * 8) << 40) | ((long) (dx + 4096) << 20) | (dz + 4096));
				}
			}
			columns = list.stream().mapToLong(Long::longValue).sorted().toArray();
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				fwd = Kit.dir(data.getFloat("Yaw"));
				reach = Destruction.radius(world, 90.0F);
				phase = CityShapes.serpentPhase(fwd);
				seed = BlockPos.ofFloored(g).asLong();
				open(world, g, "block.armageddon.ragnarok", 460);
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.5F);
				Vfx.skyTint(world, g, 100000, 0x400808, 0.85F);
				Vfx.custom(world, FxType.CITY_FX, g, fwd, (float) reach, 0xFF5020, FX_SERPENT, 0, 0, CityShapes.SERPENT_END + 30, Vfx.FAR);
				Vfx.custom(world, FxType.CITY_FX, g, fwd, (float) reach, 0xFF8030, FX_WORLD_TREE, 0, 0, END, Vfx.FAR);
			}
			// Jormungandr: the head runs the coil; the server burns the same path into the ground.
			if (t >= CityShapes.SERPENT_START && t <= CityShapes.SERPENT_END) {
				double s = CityShapes.serpentHead(t);
				Vec3d head = Kit.ground(world, g.x + CityShapes.serpent(s, reach, phase).x, g.z + CityShapes.serpent(s, reach, phase).z);
				if (Destruction.enabled(world)) {
					double from = lastHead < 0 ? s : lastHead;
					Vec3d a = CityShapes.serpent(from, reach, phase), b = CityShapes.serpent(s, reach, phase);
					int steps = Math.max(1, (int) Math.ceil(a.distanceTo(b) / 1.5));
					for (int i = 1; i <= steps; i++) {
						Vec3d o = a.lerp(b, i / (double) steps);
						RagnarokTerrain.trench(world, queue, new Vec3d(g.x + o.x, g.y, g.z + o.z), 3, seed);
					}
				}
				lastHead = s;
				for (LivingEntity e : Kit.living(world, head, 8)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.setOnFireFor(6);
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 12.0F);
					}
				}
				if (t % 12 == 0) {
					Kit.sound(world, head, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 48.0F, 0.4F);
					Vfx.shake(world, head, 160, 0.8F);
				}
			}
			if (t == CityShapes.SPLIT - 14) {
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.4F);
			}
			if (t == CityShapes.SPLIT) {
				frames(world, g, 2, 0);
				Vfx.shake(world, g, 100000, 2.0F);
				Vfx.fov(world, g, 14.0F, 24);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.4F);
				for (int i = -4; i <= 4; i++) {
					Vfx.steam(world, g.add(fwd.multiply(i * reach / 5.0)), 6.0F);
				}
				if (Destruction.enabled(world)) {
					RagnarokTerrain.chasm(world, queue, g, fwd, (int) reach, 28, seed);
				}
				planColumns(g);
			}
			// Fire on one side, frost on the other, spreading out from the chasm.
			if (columns != null && t > CityShapes.SPLIT && Destruction.enabled(world)) {
				double front = reach * Math.min(1.0, (t - CityShapes.SPLIT) / (double) CityShapes.SPLIT_SWEEP);
				Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
				while (columnCursor < columns.length && (columns[columnCursor] >>> 40) <= front * 8) {
					long c = columns[columnCursor++];
					int dx = (int) ((c >>> 20) & 0xFFFFF) - 4096, dz = (int) (c & 0xFFFFF) - 4096;
					int x = MathHelper.floor(g.x) + dx, z = MathHelper.floor(g.z) + dz;
					boolean fire = dx * side.x + dz * side.z > 0;
					RagnarokTerrain.column(world, queue, x, z, fire, seed);
					if (CityShapes.hash(seed, x * 7349 + z) < 0.004) {
						RagnarokTerrain.spire(world, queue, x, z, fire, 6 + (int) (CityShapes.hash(seed, x + z * 31) * 9));
					}
				}
			}
			if (t > CityShapes.SPLIT && t < CityShapes.SPLIT + CityShapes.SPLIT_SWEEP && t % 7 == 0) {
				// Thor's lightning walks the dividing line.
				Vec3d p = Kit.ground(world, g.x + fwd.x * (world.random.nextDouble() - 0.5) * reach * 1.6,
						g.z + fwd.z * (world.random.nextDouble() - 0.5) * reach * 1.6);
				Vfx.beam(world, p.add(world.random.nextGaussian() * 6, 90, world.random.nextGaussian() * 6), p, 1.4F, 0xD8E8FF, 5);
				Kit.sound(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 12.0F, 0.7F);
			}
			if (t == CityShapes.TREE_FALL) {
				Kit.sound(world, g, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.3F);
				Vfx.shake(world, g, 100000, 1.5F);
			}
			if (t == CityShapes.TREE_FALL + 30) {
				Vfx.skyTint(world, g, 100000, 0x2A2020, 0.9F);
			}
			queue.drain(BlockQueue.PER_TICK);
			return t > END;
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.handOff();
			}
		}
	}
}
