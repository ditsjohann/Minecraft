package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.city.CosmosTerrain;
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
 * way, and none of them end the same: a moon that stays, a newborn solar system alone in a void, a
 * city tunnelled through by a cosmic worm, a rewind that leaves time scars, a world split into fire and frost. All block
 * edits go through a budgeted {@link BlockQueue} and respect the max-radius gamerule, protected
 * regions and the block-damage rule.
 */
public final class CityDestructionEvents {
	private CityDestructionEvents() {
	}

	/** Effect ids for {@link FxType#CITY_FX}; the client's CityFx switches on these. */
	public static final int FX_MOON_TIDE = 1, FX_INFLATION = 2, FX_CHRONO_DIAL = 5, FX_WORM = 8, FX_SWORD = 9;

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

	/** Steps a devastation front at {@code speed} blocks a tick, throwing rubble and people off its edge. */
	static void devastate(ServerWorld w, com.armageddon.destruction.world.TerrainJob job, Vec3d c, int k, double start, double speed, double max,
			@org.jetbrains.annotations.Nullable net.minecraft.entity.Entity owner) {
		if (job == null) {
			return;
		}
		job.limit = start + k * speed;
		job.step(3500);
		if (job.limit < max && k % 2 == 0) {
			WorldCutsceneEvents.shockFront(w, c, job.limit, 8, 30.0F, owner);
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
		/** It breaks the sound barrier here (client: ArsenalEffects.Moon.SONIC); the shock reaches the ground soon after. */
		private static final int SONIC = (int) (FALL * 0.7), SHOCK = SONIC + 22;
		private int shockR = -1, shockMax;
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
			if (t == SONIC) {
				// The sonic boom: a crack that rolls across the whole world.
				frames(world, g, 3);
				Vfx.flash(world, g.add(0, 250, 0), 0.7F, 100000, 0xFFFFFF);
				Vfx.screen(world, g, 100000, 1, 0.45F, 8);
				Vfx.shake(world, g, 100000, 2.6F);
				Vfx.fov(world, g, 12.0F, 20);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.5F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.45F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.35F);
			}
			if (t == SHOCK) {
				shockR = 0;
				shockMax = (int) Math.min(200, r * 6.0);
				Vfx.ring(world, g.add(0, 0.5, 0), shockMax, 6.0F, 0xE8ECF4, true, 60);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
			}
			if (shockR >= 0 && shockR <= shockMax) {
				shockwave(g, shockR, shockR + 7);
				shockR += 7;
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
					// The landing's blast wave flattens everything for hundreds of blocks around the berm.
					devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, Destruction.radius(world, 300.0F),
							MathHelper.floor(g.y) - 1, com.armageddon.destruction.world.Devastation.MOON);
					devastation.limit = 0;
				}
			}
			if (devastation != null && t > FALL) {
				devastate(world, devastation, g, t - FALL, r * 1.6, 5.0, 300, owner());
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
			return t > END && (devastation == null || devastation.isDone()) || t > END + 1200;
		}

		private com.armageddon.destruction.world.TerrainJob devastation;

		/**
		 * The shock front sweeps the ring r0..r1: leaves are stripped, grass and flowers flattened, glass shattered,
		 * and everything standing is shoved outward and down.
		 */
		private void shockwave(Vec3d g, int r0, int r1) {
			for (LivingEntity e : Kit.living(world, g, r1)) {
				double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
				if (d >= r0 && !Kit.creativeOrSpectator(e)) {
					Vec3d out = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z).normalize();
					Kit.push(e, out.multiply(1.4).add(0, -0.3, 0));
					e.damage(ModDamageTypes.source(world, ModDamageTypes.NUCLEAR_BLAST, null, owner()), 4.0F);
				}
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			int cx = MathHelper.floor(g.x), cz = MathHelper.floor(g.z);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int dx = -r1; dx <= r1; dx++) {
				for (int dz = -r1; dz <= r1; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 < r0 * r0 || d2 >= r1 * r1) {
						continue;
					}
					int x = cx + dx, z = cz + dz;
					if (!world.isChunkLoaded(x >> 4, z >> 4)) {
						continue;
					}
					int top = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, x, z) - 1;
					for (int y = top; y > top - 28; y--) {
						p.set(x, y, z);
						net.minecraft.block.BlockState st = world.getBlockState(p);
						if (st.isAir()) {
							continue;
						}
						boolean glass = st.isIn(net.minecraft.registry.tag.BlockTags.IMPERMEABLE) || st.isOf(Blocks.GLASS_PANE)
								|| st.getBlock() instanceof net.minecraft.block.StainedGlassPaneBlock;
						if (st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES) || glass
								|| (st.isReplaceable() && st.getFluidState().isEmpty() && !st.isIn(net.minecraft.registry.tag.BlockTags.SNOW))) {
							queue.add(x, y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
							continue;
						}
						if (st.isIn(net.minecraft.registry.tag.BlockTags.LOGS) || !st.getFluidState().isEmpty() && y == top) {
							continue;
						}
						if (st.isOpaqueFullCube(world, p) || !st.getFluidState().isEmpty()) {
							break;
						}
					}
				}
			}
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
				// Runs on past the event so the void around the new system lingers a while.
				Vfx.custom(world, FxType.CITY_FX, point, g, (float) scale, 0xFFFFFF, FX_INFLATION, BANG, 0, END + 400, Vfx.FAR);
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
	 * WORLD EATER: something under the city is hungry. A heartbeat; then one gigantic cosmic worm, its skin a night
	 * sky, breaks out of the ground and runs through the earth, diving and surfacing again and again in a figure that
	 * crosses the whole field. It eats what it passes through: tunnels lined with sculk and crying obsidian, blown-out
	 * craters where it breaches, and nothing left alive near its mouth.
	 */
	public static class WorldEater extends WorldEvent {
		private static final int END = CityShapes.WORM_START + CityShapes.WORM_RUN + 60, BITE = 8;
		private BlockQueue queue;
		private BlockPos ground;
		private double reach, phase;
		private boolean above;

		private Vec3d head(double t) {
			Vec3d o = CityShapes.worm(t, reach, phase);
			return new Vec3d(center().x + o.x, ground.getY() + o.y, center().z + o.z);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				ground = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z));
				reach = Destruction.radius(world, 64.0F);
				phase = Math.atan2(Kit.dir(data.getFloat("Yaw")).z, Kit.dir(data.getFloat("Yaw")).x);
				open(world, g, "block.armageddon.world_eater", END);
				Vfx.skyTint(world, g, 100000, 0x14061E, 0.85F);
				Vfx.custom(world, FxType.CITY_FX, Vec3d.ofBottomCenter(ground), g, (float) reach, 0xA060FF, FX_WORM, (float) phase, 0, END, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_EMERGE, 64.0F, 0.5F);
			}
			if (t < CityShapes.WORM_START && t % 20 == 0) {
				// The heartbeat speeds up as it gets closer.
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 64.0F, 0.6F + t * 0.004F);
				Vfx.shake(world, g, 1024, 0.4F + t * 0.01F);
			}
			int run = t - CityShapes.WORM_START;
			if (run >= 0 && run <= CityShapes.WORM_RUN) {
				Vec3d h = head(t);
				double surface = Kit.ground(world, h.x, h.z).y;
				boolean up = h.y > surface;
				if (run > 0 && up != above) {
					breach(h, up);
				}
				above = up;
				devour(h);
				if (run % 10 == 0) {
					Kit.sound(world, h, ModSounds.NUKE_RUMBLE, 48.0F, 0.3F);
					Vfx.shake(world, h, 300, 1.0F);
					if (up) {
						Kit.sound(world, h, SoundEvents.ENTITY_WARDEN_ROAR, 48.0F, 0.4F);
					}
				}
			}
			if (t == CityShapes.WORM_START + CityShapes.WORM_RUN) {
				Kit.sound(world, g, SoundEvents.ENTITY_PLAYER_BURP, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.4F);
				Vfx.shake(world, g, 100000, 1.8F);
				// Gorged, it dives for the core and the hollowed land caves in after it.
				Kit.boom(world, g.add(0, 1, 0), 20.0F, 0x7040C0, owner(), true);
				Vfx.ring(world, g.add(0, 1, 0), 400.0F, 8.0F, 0x6030A0, true, 90);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 40.0F), CraterJob.Mode.ANTIMATTER));
					collapse = com.armageddon.destruction.world.Devastation.of(world, g, Destruction.radius(world, 46.0F), Destruction.radius(world, 240.0F),
							MathHelper.floor(g.y) - 1, com.armageddon.destruction.world.Devastation.VOID);
					collapse.limit = 0;
					collapseAt = t;
				}
			}
			for (com.armageddon.destruction.world.TerrainJob j : breaches) {
				j.step(1500);
			}
			breaches.removeIf(com.armageddon.destruction.world.TerrainJob::isDone);
			if (collapse != null) {
				devastate(world, collapse, g, t - collapseAt, 0, 4.0, 240, owner());
			}
			queue.drain(BlockQueue.PER_TICK);
			return t > END && breaches.isEmpty() && (collapse == null || collapse.isDone()) || t > END + 1200;
		}

		private final java.util.List<com.armageddon.destruction.world.TerrainJob> breaches = new java.util.ArrayList<>();
		private com.armageddon.destruction.world.TerrainJob collapse;
		private int collapseAt;

		/** Bursting out of the ground or diving back in: a blown-out crater, flying rock, a roar. */
		private void breach(Vec3d h, boolean out) {
			Vec3d at = Kit.ground(world, h.x, h.z);
			frames(world, at, out ? 2 : 0);
			Kit.boom(world, at, 12.0F, 0x9060FF, owner(), true);
			Detonations.debris(world, at, 24, 90);
			if (Destruction.enabled(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(at), 14.0F, CraterJob.Mode.NUKE));
				com.armageddon.destruction.world.TerrainJob j = com.armageddon.destruction.world.Devastation.of(world, at, 0, 70,
						MathHelper.floor(at.y) - 1, com.armageddon.destruction.world.Devastation.VOID);
				breaches.add(j);
			}
			Vfx.ring(world, at.add(0, 0.5, 0), 40.0F, 3.0F, 0x8A7A70, true, 40);
			Vfx.fov(world, at, 8.0F, 14);
			Kit.sound(world, at, out ? SoundEvents.ENTITY_WARDEN_ROAR : SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.35F);
			Kit.sound(world, at, SoundEvents.ENTITY_RAVAGER_ROAR, 64.0F, 0.3F);
		}

		/** The mouth eats a sphere of world every tick, lining what it leaves with sculk and crying obsidian. */
		private void devour(Vec3d h) {
			for (LivingEntity e : Kit.living(world, h, BITE + 6)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.timeUntilRegen = 0;
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 80.0F);
				}
			}
			if (age() % 2 == 0) {
				Detonations.pull(world, h, 36, 0.14);
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			int cx = MathHelper.floor(h.x), cy = MathHelper.floor(h.y), cz = MathHelper.floor(h.z), ro = BITE + 1;
			for (int dx = -ro; dx <= ro; dx++) {
				for (int dz = -ro; dz <= ro; dz++) {
					if (!world.isChunkLoaded((cx + dx) >> 4, (cz + dz) >> 4)) {
						continue;
					}
					for (int dy = -ro; dy <= ro; dy++) {
						double d2 = dx * dx + dy * dy + dz * dz;
						if (d2 > (BITE + 1.3) * (BITE + 1.3) || cy + dy <= world.getBottomY() + 2) {
							continue;
						}
						if (d2 <= BITE * BITE) {
							queue.add(cx + dx, cy + dy, cz + dz, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						} else if (dy < 0 && CityShapes.hash(cx + dx, (cy + dy) * 31 + cz + dz) < 0.35) {
							queue.add(cx + dx, cy + dy, cz + dz, (CityShapes.hash(cz + dz, cx + dx + cy) < 0.2 ? Blocks.CRYING_OBSIDIAN
									: Blocks.SCULK).getDefaultState(), BlockQueue.SOLID_ONLY);
						}
					}
				}
			}
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
	 * RAGNAROK: the end of the world, the old way. The horn sounds and the sky goes red; lightning storms walk the land,
	 * a meteor shower falls, the ground quakes open in fissures, volcanic vents burst out of the fields and the forests
	 * burn, faster and faster. Then Surtr's sword falls out of the sky: a blade of fire the size of a tower drives into
	 * the world, splits it along a chasm, and the two halves become fire and frost.
	 */
	public static class Ragnarok extends WorldEvent {
		private static final int METEORS = 90, SWORD_DROP = 420, STRIKE = 480, SWEEP = 120, END = STRIKE + SWEEP + 60;
		private static final int[] QUAKES = {140, 220, 300, 370}, VOLCANOES = {190, 260, 330};
		private BlockQueue queue;
		private Vec3d fwd;
		private double reach;
		private long seed;
		private long[] columns;
		private int columnCursor;
		private final List<Vec3d> vents = new ArrayList<>();
		private com.armageddon.destruction.world.TerrainJob devastation;
		private int devastAt;

		/** Every surface column in range, ordered by distance from the dividing line so the halves spread outward. */
		private void planColumns() {
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

		/** How wild things are: 0 at the horn, 1 just before the sword falls. */
		private double fury(int t) {
			return MathHelper.clamp(t / (double) SWORD_DROP, 0.0, 1.0);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				queue = new BlockQueue(world);
				fwd = Kit.dir(data.getFloat("Yaw"));
				reach = Destruction.radius(world, 90.0F);
				seed = BlockPos.ofFloored(g).asLong();
				open(world, g, "block.armageddon.ragnarok", END);
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.5F);
				Vfx.skyTint(world, g, 100000, 0x400808, 0.85F);
				world.setWeather(0, 1200, true, true);
			}
			double fury = fury(t);
			if (t < STRIKE) {
				storm(g, t, fury);
			}
			if (t >= METEORS && t < STRIKE && t % Math.max(3, 12 - (int) (fury * 9)) == 0) {
				// The sky falls in burning pieces.
				Kit.meteor(world, CityDestructionEvents.around(world, g, reach), owner());
			}
			for (int q : QUAKES) {
				if (t == q) {
					quake(g, q);
				}
			}
			for (int v : VOLCANOES) {
				if (t == v) {
					for (int i = 0; i < 2; i++) {
						Vec3d at = CityDestructionEvents.around(world, g, reach * 0.85);
						vents.add(at);
						erupt(at, true);
					}
				}
			}
			if (t % 6 == 0 && t < END) {
				for (Vec3d at : vents) {
					erupt(at, false);
				}
			}
			if (t == SWORD_DROP - 20) {
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.35F);
			}
			if (t == SWORD_DROP) {
				Vfx.custom(world, FxType.CITY_FX, g, fwd, (float) reach, 0xFF6020, FX_SWORD, STRIKE - SWORD_DROP, 0, END - SWORD_DROP, Vfx.FAR);
				Vfx.skyTint(world, g, 100000, 0x601004, 1.0F);
				Kit.sound(world, g, SoundEvents.ENTITY_BLAZE_SHOOT, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.ITEM_FIRECHARGE_USE, 64.0F, 0.3F);
			}
			if (t > SWORD_DROP && t < STRIKE && t % 10 == 0) {
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F + (t - SWORD_DROP) * 0.004F);
				Vfx.shake(world, g, 100000, 0.6F + (t - SWORD_DROP) * 0.03F);
			}
			if (t == STRIKE) {
				strike(g);
			}
			// Fire on one side, frost on the other, spreading out from the chasm.
			if (columns != null && t > STRIKE && Destruction.enabled(world)) {
				double front = reach * Math.min(1.0, (t - STRIKE) / (double) SWEEP);
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
			if (t == STRIKE + SWEEP + 30) {
				Vfx.skyTint(world, g, 100000, 0x2A2020, 0.9F);
				Kit.sound(world, g, SoundEvents.EVENT_RAID_HORN.value(), 64.0F, 0.25F);
			}
			queue.drain(BlockQueue.PER_TICK);
			if (devastation != null) {
				devastate(world, devastation, center(), t - devastAt, 0, 5.0, 300, owner());
			}
			return t > END && (devastation == null || devastation.isDone()) || t > END + 1200;
		}

		/** Thunderstorms that walk the land: real lightning, more and more of it. */
		private void storm(Vec3d g, int t, double fury) {
			int every = Math.max(2, 16 - (int) (fury * 14));
			if (t % every != 0) {
				return;
			}
			int bolts = 1 + (int) (fury * 3);
			for (int i = 0; i < bolts; i++) {
				Vec3d p = CityDestructionEvents.around(world, g, reach * 1.1);
				net.minecraft.entity.LightningEntity bolt = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
				if (bolt != null) {
					bolt.refreshPositionAfterTeleport(p);
					bolt.setCosmetic(!Destruction.enabled(world));
					world.spawnEntity(bolt);
				}
				Vfx.beam(world, p, p.add(world.random.nextGaussian() * 8, 120, world.random.nextGaussian() * 8), 1.6F, 0xD8E8FF, 5);
			}
			if (world.random.nextFloat() < 0.3F + fury * 0.4F) {
				Vfx.flash(world, g.add(0, 120, 0), 0.25F, 2000, 0xD0E0FF);
			}
		}

		/** An earthquake: the ground splits in fissures, the land shudders and the forests catch fire. */
		private void quake(Vec3d g, int k) {
			Vfx.shake(world, g, 100000, 2.2F);
			Vfx.custom(world, FxType.QUAKE, g, g, (float) reach, 0x8A6A50, 0, 0, 0, 60, Vfx.FAR);
			Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.3F);
			frames(world, g, 0);
			for (int i = 0; i < 4; i++) {
				Vec3d from = CityDestructionEvents.around(world, g, reach * 0.8);
				Vec3d d = Kit.dir(world.random.nextFloat() * 360.0F);
				Kit.fissure(world, from, d, 24 + world.random.nextInt(30), 5 + world.random.nextInt(5), 2, Blocks.LAVA.getDefaultState());
			}
			if (Destruction.enabled(world)) {
				Detonations.ignite(world, CityDestructionEvents.around(world, g, reach * 0.6), 18, 0.12F);
			}
			for (LivingEntity e : Kit.living(world, g, reach)) {
				if (e.isOnGround() && !Kit.creativeOrSpectator(e)) {
					Kit.push(e, new Vec3d(world.random.nextGaussian() * 0.3, 0.5, world.random.nextGaussian() * 0.3));
				}
			}
		}

		/** A volcanic vent bursts out of the fields; afterwards it keeps spitting lava bombs. */
		private void erupt(Vec3d at, boolean first) {
			if (first) {
				Kit.boom(world, at, 6.0F, 0xFF5010, owner(), true);
				Vfx.pillar(world, at, 5.0F, 0xFF6020, false, END - age());
				Kit.sound(world, at, SoundEvents.BLOCK_LAVA_POP, 64.0F, 0.3F);
				if (Destruction.enabled(world)) {
					BlockPos b = BlockPos.ofFloored(at);
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							int d2 = dx * dx + dz * dz;
							if (d2 > 16) {
								continue;
							}
							for (int dy = -3; dy <= 0; dy++) {
								queue.add(b.getX() + dx, b.getY() + dy, b.getZ() + dz,
										(d2 <= 4 ? Blocks.LAVA : d2 <= 9 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).getDefaultState(), BlockQueue.ANY);
							}
						}
					}
				}
				return;
			}
			world.spawnParticles(ParticleTypes.LAVA, at.x, at.y + 1, at.z, 12, 1.5, 1, 1.5, 0.0);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 3, at.z, 10, 2, 4, 2, 0.05);
			if (world.random.nextInt(3) == 0 && Destruction.enabled(world) && Kit.debrisBudget(world, at) > 0) {
				// A lava bomb: a molten block lobbed out of the vent.
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, BlockPos.ofFloored(at).up(2), Blocks.MAGMA_BLOCK.getDefaultState());
				double a = world.random.nextDouble() * Math.PI * 2;
				Kit.velocity(fb, new Vec3d(Math.cos(a) * 0.7, 1.4 + world.random.nextDouble(), Math.sin(a) * 0.7));
				fb.setDestroyedOnLanding();
			}
		}

		/** Surtr's sword strikes: the world splits along the chasm and the halves turn to fire and frost. */
		private void strike(Vec3d g) {
			frames(world, g, 3, 2, 0, 2);
			Kit.boom(world, g.add(0, 2, 0), 18.0F, 0xFF6020, owner(), false);
			Vfx.flash(world, g, 1.0F, 100000, 0xFFB060);
			Vfx.shake(world, g, 100000, 3.0F);
			Vfx.fov(world, g, 20.0F, 30);
			Vfx.ring(world, g.add(0, 1, 0), (float) reach * 1.6F, 6.0F, 0xFF8030, true, 70);
			Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.3F);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.4F);
			Detonations.blast(world, g, reach * 0.5, 500.0F, ModDamageTypes.ANNIHILATION, owner(), reach * 0.3, 4.0);
			for (int i = -4; i <= 4; i++) {
				Vfx.steam(world, g.add(fwd.multiply(i * reach / 5.0)), 6.0F);
			}
			if (Destruction.enabled(world)) {
				RagnarokTerrain.chasm(world, queue, g, fwd, (int) reach, 32, seed);
				devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, Destruction.radius(world, 300.0F), MathHelper.floor(g.y) - 1,
						com.armageddon.destruction.world.Devastation.FIRE);
				devastation.limit = 0;
				devastAt = age();
			}
			planColumns();
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.handOff();
			}
		}
	}
}
