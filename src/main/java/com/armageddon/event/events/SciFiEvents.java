package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.destruction.world.Devastation;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.event.Ruin;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.SciShapes;
import com.armageddon.util.SciShapes.Horizon;
import com.armageddon.util.SciShapes.Inversion;
import com.armageddon.util.SciShapes.Ion;
import com.armageddon.util.SciShapes.Nanite;
import com.armageddon.util.SciShapes.Phase;
import com.armageddon.util.SciShapes.Tesla;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The plasma and sci-fi weapons. None of them is an explosion: an orbital ion beam that cuts and then vaporises, a
 * force field that scans a town out of existence, a tesla tower that burns lightning into the ground as glass, a
 * singularity that spins the land into an accretion disk, grey goo, and an engine that turns the world upside down.
 */
public final class SciFiEvents {
	private SciFiEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: SciFx). */
	public static final int FX_ION = 40, FX_PHASE = 41, FX_TESLA = 42, FX_HORIZON = 43, FX_NANITE = 44, FX_INVERSION = 45;
	/** Cinematic scene ids (client: Cinematics.SciCine). */
	public static final int CINE_HORIZON = 8, CINE_NANITE = 9, CINE_INVERSION = 10;

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	abstract static class Base extends WorldEvent {
		final List<TerrainJob> jobs = new ArrayList<>();
		ProtectedRegions regions;

		boolean put(BlockPos p, BlockState s) {
			return Destruction.set(world, p, s, FLAGS);
		}

		boolean breakable(BlockPos p, BlockState s) {
			if (regions == null) {
				regions = ProtectedRegions.get(world);
			}
			return !s.isAir() && s.getHardness(world, p) >= 0 && !s.hasBlockEntity() && (regions.isEmpty() || !regions.isProtected(p));
		}

		int surface(int x, int z) {
			return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		}

		int top(int x, int z) {
			return world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
		}

		void stepJobs(int budget) {
			for (TerrainJob j : jobs) {
				if (budget <= 0) {
					break;
				}
				budget = j.step(budget);
			}
			jobs.removeIf(TerrainJob::isDone);
		}

		float yaw() {
			return data.getFloat("Yaw");
		}
	}

	// ====================================================================== ION SCYTHE

	/**
	 * ION SCYTHE (celestial). An ion cannon in orbit locks on and drops a beam of blue-white plasma that carves a
	 * spiral trench outward through everything, molten at the bottom and glassed at the edges. The trench charges,
	 * and then the whole spiral ignites at once: a curtain of light along its length that disintegrates the land.
	 */
	public static class IonScythe extends Base {
		private double phase;
		private double lastTh;
		private final List<Vec3d> cut = new ArrayList<>();
		private double sinceSample;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				phase = Math.toRadians(yaw());
				CelestialEvents.announce(world, g, "block.armageddon.ion_scythe");
				Vfx.newFx(world, FX_ION, g, new Vec3d(phase, 0, 0), (float) Ion.R, 0x80E0FF, 0, 0, Ion.END);
				Vfx.skyTint(world, g, 100000, 0x081428, 0.6F);
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.6F);
			}
			if (t == Ion.DROP) {
				Vfx.flash(world, g, 0.3F, 400, 0xA0E8FF);
				Vfx.shake(world, g, 400, 1.0F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 1.2F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 1.4F);
			}
			if (t >= Ion.DROP && t < Ion.CARVE) {
				burn(g.add(0, 0, 0), 2.4);
			}
			if (t >= Ion.CARVE && t <= Ion.CARVE_END) {
				double th = Ion.theta(t);
				double a = Ion.GAP / MathHelper.TAU;
				for (double s = lastTh; s < th; s += 1.3 / Math.max(1.0, a * s)) {
					Vec3d p = g.add(Ion.at(s, phase));
					burn(p, 1.7);
					sinceSample += 1.3;
					if (sinceSample >= 6) {
						sinceSample = 0;
						cut.add(Kit.ground(world, p.x, p.z));
					}
				}
				lastTh = th;
				if (t % 3 == 0) {
					Vec3d p = g.add(Ion.at(th, phase));
					Kit.sound(world, p, SoundEvents.BLOCK_FIRE_EXTINGUISH, 32.0F, 0.6F);
					Kit.sound(world, p, ModSounds.DEATH_RAY, 24.0F, 1.6F);
				}
			}
			if (t > Ion.CARVE_END && t < Ion.IGNITE && t % 8 == 0) {
				// The trench charges: a rising whine and the glow brightening along it.
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.6F + (t - Ion.CARVE_END) * 0.015F);
				Vfx.shake(world, g, 300, 0.3F + (t - Ion.CARVE_END) * 0.01F);
			}
			if (t == Ion.IGNITE) {
				CityDestructionEvents.frames(world, g, 3, 1);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 1.2F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.7F);
				Vfx.shake(world, g, 600, 1.8F);
				for (int i = 0; i < cut.size(); i++) {
					Vec3d p = cut.get(i);
					int delay = i / 6;
					DetonationManager.schedule(world, delay, () -> {
						Ruin.disintegrate(world, p, 3.4, Blocks.SEA_LANTERN.getDefaultState(), ParticleTypes.END_ROD, 10);
						Kit.damage(world, p, 5, 60.0F, ModDamageTypes.ANNIHILATION, null, owner());
					});
				}
			}
			return t >= Ion.END;
		}

		/** One bite of the beam: a molten hole through whatever is there, glassed at the rim, and anyone near it burned. */
		private void burn(Vec3d p, double r) {
			for (LivingEntity e : Kit.living(world, p, r + 3)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 20.0F);
					e.setOnFireFor(6);
				}
			}
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, Kit.ground(world, p.x, p.z).y + 0.5, p.z, 6, 1.0, 0.3, 1.0, 0.3);
			if (!Destruction.enabled(world)) {
				return;
			}
			int ri = MathHelper.ceil(r + 1.2);
			BlockPos.Mutable q = new BlockPos.Mutable();
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					int x = MathHelper.floor(p.x) + dx, z = MathHelper.floor(p.z) + dz;
					if (d > r + 1.2 || !world.isChunkLoaded(x >> 4, z >> 4)) {
						continue;
					}
					int surf = surface(x, z), top = Math.min(top(x, z), surf + 30);
					BlockState was = world.getBlockState(q.set(x, surf, z));
					if (was.isOf(Blocks.MAGMA_BLOCK) || was.isOf(Blocks.OBSIDIAN) || was.isOf(Blocks.TINTED_GLASS)) {
						continue;
					}
					if (d <= r) {
						for (int y = top; y >= surf - 4; y--) {
							BlockState s = world.getBlockState(q.set(x, y, z));
							if (breakable(q, s)) {
								put(q.toImmutable(), AIR);
							}
						}
						put(new BlockPos(x, surf - 5, z), Blocks.MAGMA_BLOCK.getDefaultState());
					} else {
						// The rim: stone and soil fused to black glass and obsidian.
						BlockState s = world.getBlockState(q.set(x, surf, z));
						if (breakable(q, s)) {
							put(q.toImmutable(), SciShapes.hash(x, z) < 0.5 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.TINTED_GLASS.getDefaultState());
						}
					}
				}
			}
		}
	}

	// ====================================================================== PHASE SCRUBBER

	/**
	 * PHASE SCRUBBER (city destruction). A hexagonal force field drops over the town and a plane of blue light
	 * descends through it from the crown to the ground. Whatever it passes through is phased out of existence,
	 * hills included. When it reaches the bottom it prints a clean, lit test-chamber floor across the whole dome,
	 * and the field shatters.
	 */
	public static class PhaseScrubber extends Base {
		private int nextLayer = Integer.MAX_VALUE, ox, oz, y0;
		private final java.util.ArrayDeque<int[]> layers = new java.util.ArrayDeque<>();
		private int cursor;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				ox = MathHelper.floor(g.x);
				oz = MathHelper.floor(g.z);
				y0 = MathHelper.floor(g.y);
				CelestialEvents.announce(world, g, "block.armageddon.phase_scrubber");
				Vfx.newFx(world, FX_PHASE, g, g, (float) Phase.R, 0x60C8FF, 0, 0, Phase.END);
				Vfx.cutscene(world, g, 60);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.6F);
			}
			if (t == Phase.DOME_UP) {
				Vfx.shake(world, g, 400, 1.2F);
				Kit.sound(world, g, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.8F);
			}
			if (t >= Phase.SCAN && t <= Phase.SCAN_END) {
				int layer = MathHelper.floor(g.y + Phase.plane(t));
				if (nextLayer == Integer.MAX_VALUE) {
					nextLayer = MathHelper.floor(g.y + Phase.H);
				}
				while (nextLayer >= layer) {
					layers.addLast(new int[]{nextLayer});
					nextLayer--;
				}
				double py = g.y + Phase.plane(t);
				for (LivingEntity e : Kit.living(world, g, Phase.R)) {
					if (!Kit.creativeOrSpectator(e) && Math.abs(e.getY() + e.getHeight() * 0.5 - py) < e.getHeight() * 0.5 + 1.2
							&& Math.hypot(e.getX() - g.x, e.getZ() - g.z) < Phase.radiusAt(py - g.y)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 14.0F);
						world.spawnParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.6, 0.3, 0.05);
					}
				}
				if (t % 6 == 0) {
					Kit.sound(world, new Vec3d(g.x, py, g.z), SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 1.6F);
				}
			}
			scrub(4500);
			if (t == Phase.PRINT && Destruction.enabled(world)) {
				TerrainJob floor = TerrainJob.disc(world, g, Phase.R - 0.5, c -> c.set(y0 - 1, floorAt(c.x - ox, c.z - oz)));
				floor.limit = 0;
				jobs.add(floor);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 1.2F);
			}
			for (TerrainJob j : jobs) {
				j.limit = Math.max(j.limit, (t - Phase.PRINT) * 2.5);
			}
			stepJobs(3000);
			if (t == Phase.SHATTER) {
				CityDestructionEvents.frames(world, g, 3);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 64.0F, 0.6F);
				Vfx.shake(world, g, 400, 1.0F);
			}
			if (t > Phase.SHATTER && t < Phase.SHATTER + 40 && t % 4 == 0) {
				Kit.sound(world, g.add((world.random.nextDouble() - 0.5) * 100, 30, (world.random.nextDouble() - 0.5) * 100),
						SoundEvents.BLOCK_GLASS_BREAK, 48.0F, 0.6F + world.random.nextFloat() * 0.6F);
			}
			return t >= Phase.END && layers.isEmpty() && jobs.isEmpty() || t > Phase.END + 1200;
		}

		/** Erases queued layers, a slice at a time, within the dome's footprint at that height. */
		private void scrub(int budget) {
			if (!Destruction.enabled(world)) {
				layers.clear();
				return;
			}
			BlockPos.Mutable p = new BlockPos.Mutable();
			while (budget > 0 && !layers.isEmpty()) {
				int y = layers.peekFirst()[0];
				double rr = Phase.radiusAt(y - y0 + 0.5);
				int ri = MathHelper.ceil(rr);
				int side = 2 * ri + 1, total = side * side;
				while (cursor < total && budget > 0) {
					int dx = cursor % side - ri, dz = cursor / side - ri;
					cursor++;
					if (dx * dx + dz * dz > rr * rr) {
						continue;
					}
					budget--;
					p.set(ox + dx, y, oz + dz);
					BlockState s = world.getBlockState(p);
					if (breakable(p, s) || !s.getFluidState().isEmpty() && !s.isAir()) {
						put(p.toImmutable(), AIR);
						budget -= 2;
						if ((dx * 7 + dz * 13 + y) % 97 == 0) {
							world.spawnParticles(ParticleTypes.END_ROD, p.getX() + 0.5, y + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.01);
						}
					}
				}
				if (cursor >= total) {
					layers.pollFirst();
					cursor = 0;
				}
			}
		}

		/** The printed floor: white panels, a light-blue grid, a lamp at every crossing and a ring at the centre. */
		private static BlockState floorAt(int dx, int dz) {
			boolean gx = Math.floorMod(dx, 9) == 0, gz = Math.floorMod(dz, 9) == 0;
			double d = Math.sqrt(dx * dx + dz * dz);
			if (gx && gz) {
				return Blocks.SEA_LANTERN.getDefaultState();
			}
			if (Math.abs(d - 12) < 0.7 || Math.abs(d - Phase.R + 2) < 0.7) {
				return Blocks.CYAN_CONCRETE.getDefaultState();
			}
			if (gx || gz) {
				return Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
			}
			return SciShapes.hash(dx, dz) < 0.08 ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
		}
	}

	// ====================================================================== TESLA DOMINION

	/**
	 * TESLA DOMINION (city destruction). A lattice tower rises out of the ground and the storm starts: arcs jump
	 * from its crown to the highest things around, and every strike burns a branching fulgurite of glass into
	 * the ground. Ball lightning grows on the crown and discharges everywhere at once.
	 */
	public static class TeslaDominion extends Base {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d crown = Tesla.crown(g, t);
			if (t == 0) {
				CelestialEvents.announce(world, g, "block.armageddon.tesla_dominion");
				Vfx.newFx(world, FX_TESLA, g, g, (float) Tesla.REACH, 0x90D8FF, 0, 0, Tesla.END);
				Vfx.skyTint(world, g, 100000, 0x101830, 0.75F);
				world.setWeather(0, 1600, true, true);
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.4F);
			}
			if (t > Tesla.RISE && t < Tesla.RISEN && t % 10 == 0) {
				Vfx.shake(world, g, 300, 0.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_COPPER_PLACE, 48.0F, 0.5F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 48.0F, 0.6F);
			}
			if (t >= Tesla.STORM && t < Tesla.DISCHARGE) {
				int every = Math.max(2, 9 - (t - Tesla.STORM) / 50);
				if (t % every == 0) {
					strike(g, crown, Tesla.REACH, 10 + world.random.nextInt(14));
				}
			}
			if (t >= Tesla.ORB && t < Tesla.DISCHARGE) {
				if (t % 4 == 0) {
					Kit.sound(world, crown, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F + (t - Tesla.ORB) * 0.02F);
					Vfx.shake(world, g, 400, 0.4F + (t - Tesla.ORB) * 0.015F);
				}
				// It pulls: everything loose leans toward the tower.
				if (t % 5 == 0) {
					com.armageddon.detonation.Detonations.pull(world, g, 60, 0.06);
				}
			}
			if (t == Tesla.DISCHARGE) {
				CityDestructionEvents.frames(world, g, 3, 1, 3);
				Vfx.flash(world, crown, 0.5F, 100000, 0xC0E8FF);
				Vfx.shake(world, g, 100000, 2.6F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.8F);
				for (int i = 0; i < 40; i++) {
					double a = i * MathHelper.TAU / 40 + world.random.nextDouble() * 0.15;
					double d = 25 + world.random.nextDouble() * (Tesla.REACH - 25);
					Vec3d hit = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					DetonationManager.schedule(world, i % 4, () -> strikeAt(g, crown, hit, 24 + world.random.nextInt(26)));
				}
				for (LivingEntity e : Kit.living(world, g, Tesla.REACH)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 40.0F);
					}
				}
			}
			if (t == Tesla.FALL) {
				Kit.sound(world, g, SoundEvents.BLOCK_CHAIN_BREAK, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.4F);
			}
			return t >= Tesla.END;
		}

		/** Picks the tallest of a few spots in range and hits it. */
		private void strike(Vec3d g, Vec3d crown, double reach, int length) {
			Vec3d best = null;
			for (int i = 0; i < 4; i++) {
				double a = world.random.nextDouble() * MathHelper.TAU, d = 12 + Math.sqrt(world.random.nextDouble()) * (reach - 12);
				int x = MathHelper.floor(g.x + Math.cos(a) * d), z = MathHelper.floor(g.z + Math.sin(a) * d);
				Vec3d p = new Vec3d(x + 0.5, top(x, z) + 1, z + 0.5);
				if (best == null || p.y > best.y) {
					best = p;
				}
			}
			strikeAt(g, crown, best, length);
		}

		private void strikeAt(Vec3d g, Vec3d crown, Vec3d hit, int length) {
			Vfx.arc(world, crown, hit, 2.2F, 0xA8E0FF);
			Vfx.light(world, hit, 0.6F, 0xA0D8FF);
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				bolt.refreshPositionAfterTeleport(hit);
				bolt.setCosmetic(true);
				world.spawnEntity(bolt);
			}
			for (LivingEntity e : Kit.living(world, hit, 6)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 18.0F);
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 3, false, false));
				}
			}
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 40, 1.5, 1.0, 1.5, 0.4);
			if (Destruction.enabled(world)) {
				Vec3d out = new Vec3d(hit.x - g.x, 0, hit.z - g.z);
				double heading = Math.atan2(out.z, out.x);
				fulgurite(hit.x, hit.z, heading, length, 2);
				fulgurite(hit.x, hit.z, heading + Math.PI * (0.6 + world.random.nextDouble() * 0.8), length / 2, 1);
			}
		}

		/**
		 * Lightning frozen in the ground: a forking channel burnt through whatever stands in its way, its floor
		 * fused to glass and lit here and there where the charge pooled.
		 */
		private void fulgurite(double x, double z, double heading, int length, int forks) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int i = 0; i < length; i++) {
				heading += world.random.nextGaussian() * 0.45;
				x += Math.cos(heading);
				z += Math.sin(heading);
				int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
				if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
					return;
				}
				int surf = surface(bx, bz), top = Math.min(top(bx, bz), surf + 12);
				double fade = 1 - i / (double) length;
				int depth = fade > 0.5 ? 2 : 1;
				for (int y = top; y > surf - depth; y--) {
					BlockState s = world.getBlockState(p.set(bx, y, bz));
					if (breakable(p, s)) {
						put(p.toImmutable(), AIR);
					}
				}
				BlockState floor = i % 7 == 3 ? Blocks.SEA_LANTERN.getDefaultState()
						: SciShapes.hash(bx, bz) < 0.5 ? Blocks.TINTED_GLASS.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
				BlockState s = world.getBlockState(p.set(bx, surf - depth, bz));
				if (breakable(p, s)) {
					put(p.toImmutable(), floor);
				}
				if (forks > 0 && world.random.nextFloat() < 0.13F) {
					fulgurite(x, z, heading + (world.random.nextBoolean() ? 0.8 : -0.8), (int) ((length - i) * 0.6), forks - 1);
				}
			}
		}
	}

	// ====================================================================== EVENT HORIZON ENGINE

	/**
	 * EVENT HORIZON ENGINE (world destruction). Three rings assemble in the sky and spin up; a singularity opens
	 * between them. The ground below is torn up into an accretion disk that spins faster and faster and falls
	 * inward; then the rings collapse into it, the disk goes in all at once, and the hole closes on a flash that
	 * leaves a void-scarred bowl hundreds of blocks across.
	 */
	public static class EventHorizon extends Base {
		private final List<FallingBlockEntity> disk = new ArrayList<>();
		private Vec3d s;
		private TerrainJob scar;
		private int scarAt;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				s = g.add(0, Horizon.ALT, 0);
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.event_horizon", CINE_HORIZON, Kit.dir(yaw()), Horizon.CINE);
				Vfx.newFx(world, FX_HORIZON, s, g, (float) Horizon.RING_R, 0xFFB060, 0, 0, Horizon.END);
				Vfx.skyTint(world, g, 100000, 0x0A0614, 0.8F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.4F);
			}
			if (t > Horizon.ASSEMBLE && t < Horizon.OPEN && t % 12 == 0) {
				Kit.sound(world, s, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.3F + t * 0.003F);
				Kit.sound(world, s, SoundEvents.BLOCK_IRON_DOOR_CLOSE, 64.0F, 0.4F);
			}
			if (t == Horizon.OPEN) {
				CityDestructionEvents.frames(world, g, 0, 1);
				Vfx.flash(world, s, 0.4F, 100000, 0xFFD0A0);
				Vfx.shake(world, g, 100000, 1.8F);
				Kit.sound(world, s, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.5F);
				Kit.sound(world, s, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.3F);
			}
			double spin = Horizon.spin(t);
			if (t >= Horizon.FEED && t < Horizon.FEED_END) {
				double fr = Horizon.feedRadius(t);
				for (FallingBlockEntity fb : Kit.lift(world, g, fr, 10, true, true)) {
					fb.setNoGravity(true);
					Kit.velocity(fb, new Vec3d(0, 0.4, 0));
					disk.add(fb);
				}
				if (t % 6 == 0) {
					for (LivingEntity e : Kit.living(world, g, fr + 10)) {
						if (!Kit.creativeOrSpectator(e) && e.getY() < s.y - 6) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 2, false, false));
						}
					}
				}
				if (t % 20 == 0) {
					Kit.sound(world, s, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F + (float) spin);
					Vfx.shake(world, g, 100000, 0.5F + (float) spin * 6);
				}
			}
			orbit(t, spin);
			if (t == Horizon.COLLAPSE) {
				Kit.sound(world, s, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 64.0F, 0.3F);
				Kit.sound(world, s, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.3F);
				Vfx.shake(world, g, 100000, 2.4F);
			}
			if (t == Horizon.GONE) {
				Kit.vanish(disk);
				CityDestructionEvents.frames(world, g, 3, 1, 3, 0);
				Vfx.flash(world, s, 0.6F, 100000, 0xFFFFFF);
				Vfx.ring(world, g.add(0, 1, 0), 520.0F, 10.0F, 0xFFC080, true, 90);
				Vfx.shake(world, g, 100000, 3.0F);
				Vfx.fov(world, g, 20.0F, 40);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.25F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.25F);
				com.armageddon.detonation.Detonations.blast(world, g, 200, 4000.0F, ModDamageTypes.ANNIHILATION, owner(), 120, 6.0);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 40.0F), CraterJob.Mode.ANTIMATTER));
					scar = Devastation.of(world, g, Destruction.radius(world, 60.0F), Destruction.radius(world, 270.0F), MathHelper.floor(g.y) - 1, Devastation.VOID);
					scar.limit = 0;
					scarAt = t;
				}
			}
			if (scar != null) {
				CityDestructionEvents.devastate(world, scar, g, t - scarAt, 30, 5.0, 270, owner());
			}
			return t >= Horizon.END && (scar == null || scar.isDone()) || t > Horizon.END + 1500;
		}

		/** Steers the disk: around the hole, toward its plane, slowly inward; things that reach the hole are gone. */
		private void orbit(int t, double spin) {
			boolean collapse = t >= Horizon.COLLAPSE;
			disk.removeIf(fb -> {
				if (!fb.isAlive()) {
					return true;
				}
				Vec3d rel = fb.getPos().subtract(s);
				double r = Math.hypot(rel.x, rel.z);
				if (rel.length() < 6.5) {
					world.spawnParticles(ParticleTypes.REVERSE_PORTAL, fb.getX(), fb.getY(), fb.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
					fb.discard();
					return true;
				}
				if (collapse) {
					Kit.velocity(fb, rel.normalize().multiply(-3.2));
					return false;
				}
				Vec3d in = r > 1.0E-3 ? new Vec3d(-rel.x / r, 0, -rel.z / r) : Vec3d.ZERO;
				Vec3d tangent = new Vec3d(-in.z, 0, in.x);
				double want = Math.max(9, r * (0.994 - spin * 0.02));
				double plane = (SciShapes.hash(fb.getId(), 3) - 0.5) * (2 + r * 0.06);
				double speed = (0.4 + spin * 10) * Math.min(1.6, 30 / Math.max(10, r) + 0.4);
				Kit.velocity(fb, tangent.multiply(speed).add(in.multiply((r - want) * 0.12)).add(0, (plane - rel.y) * 0.06, 0));
				return false;
			});
		}

		@Override
		public void finish() {
			Kit.vanish(disk);
			super.finish();
		}
	}

	// ====================================================================== NANITE BLOOM

	/**
	 * NANITE BLOOM (world destruction). A canister cracks and grey goo pours out: branching fingers of nanite matter
	 * run out across the land and everything they touch, house, tree, hill or person, turns to grey metal in its
	 * own shape. The gaps fill in, and then the whole region resolves itself: it melts flat into a gleaming silver
	 * plain laid out in hexagons, with a pulse of light running through it.
	 */
	public static class NaniteBloom extends Base {
		private static final class Tip {
			double x, z, heading;

			Tip(double x, double z, double heading) {
				this.x = x;
				this.z = z;
				this.heading = heading;
			}
		}

		private final List<Tip> tips = new ArrayList<>();
		private final java.util.Set<Long> done = new java.util.HashSet<>();
		private TerrainJob fill, plain;
		private int y0;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				y0 = MathHelper.floor(g.y);
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.nanite_bloom", CINE_NANITE, Kit.dir(yaw()), Nanite.CINE);
				Vfx.newFx(world, FX_NANITE, g, g, (float) Nanite.R, 0x70E0FF, 0, 0, Nanite.END);
				Vfx.skyTint(world, g, 100000, 0x202830, 0.6F);
			}
			if (t == Nanite.RELEASE) {
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 32.0F, 0.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_SCULK_SPREAD, 64.0F, 0.5F);
				Vfx.ring(world, g.add(0, 0.5, 0), 20.0F, 1.0F, 0x80D0E0, false, 20);
				for (int i = 0; i < 14; i++) {
					tips.add(new Tip(g.x, g.z, i * MathHelper.TAU / 14 + world.random.nextGaussian() * 0.15));
				}
			}
			if (t >= Nanite.GROW && t < Nanite.GROW_END && Destruction.enabled(world)) {
				grow(g);
				if (t % 10 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_SCULK_SPREAD, 64.0F, 0.4F + world.random.nextFloat() * 0.3F);
				}
			}
			if (t == Nanite.FILL && Destruction.enabled(world)) {
				fill = TerrainJob.disc(world, g, Nanite.R, c -> convert(c.x, c.z));
				fill.limit = 0;
			}
			if (fill != null) {
				fill.limit = Nanite.fill(t);
				fill.step(300);
			}
			if (t == Nanite.PLAIN) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.4F);
				Vfx.shake(world, g, 100000, 1.2F);
				if (Destruction.enabled(world)) {
					plain = TerrainJob.disc(world, g, Nanite.R, this::flatten);
					plain.limit = 0;
				}
			}
			if (plain != null) {
				plain.limit = Math.max(0, (t - Nanite.PLAIN) * 3.2);
				plain.step(6000);
			}
			if (t == Nanite.PULSE) {
				CityDestructionEvents.frames(world, g, 3);
				Vfx.flash(world, g, 0.3F, 100000, 0xA0F0FF);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 1.0F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.8F);
			}
			return t >= Nanite.END && (fill == null || fill.isDone()) && (plain == null || plain.isDone()) || t > Nanite.END + 1600;
		}

		private void grow(Vec3d g) {
			List<Tip> born = new ArrayList<>();
			tips.removeIf(tip -> {
				tip.heading += world.random.nextGaussian() * 0.28;
				tip.x += Math.cos(tip.heading) * 1.4;
				tip.z += Math.sin(tip.heading) * 1.4;
				double d = Math.hypot(tip.x - g.x, tip.z - g.z);
				if (d > Nanite.R || world.random.nextFloat() < 0.0025F) {
					return true;
				}
				int r = d < 30 ? 3 : 2;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz <= r * r) {
							convert(MathHelper.floor(tip.x) + dx, MathHelper.floor(tip.z) + dz);
						}
					}
				}
				for (LivingEntity e : Kit.living(world, new Vec3d(tip.x, g.y, tip.z), 4)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 6.0F);
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, false, false));
					}
				}
				if (tips.size() + born.size() < 90 && world.random.nextFloat() < 0.035F) {
					born.add(new Tip(tip.x, tip.z, tip.heading + (world.random.nextBoolean() ? 0.7 : -0.7)));
				}
				return false;
			});
			tips.addAll(born);
			if (tips.size() < 6) {
				double a = world.random.nextDouble() * MathHelper.TAU, d = world.random.nextDouble() * Nanite.R * 0.4;
				tips.add(new Tip(g.x + Math.cos(a) * d, g.z + Math.sin(a) * d, a));
			}
		}

		/** Goo in the shape of whatever was there: every block of the column above the bedrock of the soil turns grey. */
		private void convert(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4) || !done.add(BlockPos.asLong(x, 0, z))) {
				return;
			}
			int surf = surface(x, z), top = Math.min(top(x, z), surf + 28);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = top; y >= surf - 2; y--) {
				BlockState s = world.getBlockState(p.set(x, y, z));
				if (!breakable(p, s) || isGoo(s)) {
					continue;
				}
				if (!s.isFullCube(world, p) && y > surf) {
					put(p.toImmutable(), AIR);
					continue;
				}
				put(p.toImmutable(), goo(x, y, z));
			}
		}

		private static boolean isGoo(BlockState s) {
			return s.isOf(Blocks.LIGHT_GRAY_CONCRETE) || s.isOf(Blocks.GRAY_CONCRETE) || s.isOf(Blocks.IRON_BLOCK) || s.isOf(Blocks.SEA_LANTERN)
					|| s.isOf(Blocks.WHITE_CONCRETE);
		}

		private static BlockState goo(int x, int y, int z) {
			double n = SciShapes.hash(x * 31 + y, z * 17 - y);
			return n < 0.012 ? Blocks.SEA_LANTERN.getDefaultState() : n < 0.12 ? Blocks.IRON_BLOCK.getDefaultState()
					: n < 0.4 ? Blocks.GRAY_CONCRETE.getDefaultState() : Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
		}

		/** The plain: everything above the anchor's height melts away, hollows fill, and the top is laid out in hexagons. */
		private void flatten(TerrainJob.Column c) {
			int surf = c.surface(), top = Math.min(c.top(), surf + 60);
			for (int y = top; y > y0 - 1; y--) {
				if (!c.get(y).isAir()) {
					c.set(y, AIR);
				}
			}
			for (int y = Math.max(surf + 1, y0 - 14); y < y0 - 1; y++) {
				c.set(y, Blocks.GRAY_CONCRETE.getDefaultState());
			}
			c.set(y0 - 1, hex(c.x, c.z));
		}

		/** Hexagons ten blocks across: iron edges, white-grey faces, a lamp in the middle of each. */
		private BlockState hex(int x, int z) {
			double size = 6.0;
			double qx = (x * Math.sqrt(3) / 3 - z / 3.0) / size, qz = (z * 2 / 3.0) / size;
			double cx = qx, cz = qz, cy = -qx - qz;
			double rx = Math.round(cx), rz = Math.round(cz), ry = Math.round(cy);
			double ddx = Math.abs(rx - cx), ddz = Math.abs(rz - cz), ddy = Math.abs(ry - cy);
			if (ddx > ddz && ddx > ddy) {
				rx = -rz - ry;
			} else if (ddz > ddy) {
				rz = -rx - ry;
			}
			double fx = cx - rx, fz = cz - rz, fy = -fx - fz;
			double edge = Math.max(Math.abs(fx), Math.max(Math.abs(fz), Math.abs(fy)));
			if (edge > 0.42) {
				return Blocks.IRON_BLOCK.getDefaultState();
			}
			if (edge < 0.07) {
				return Blocks.SEA_LANTERN.getDefaultState();
			}
			return SciShapes.hash(x, z) < 0.5 ? Blocks.LIGHT_GRAY_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState();
		}
	}

	// ====================================================================== INVERSION ENGINE

	/**
	 * INVERSION ENGINE (world destruction). A gyroscope of light spins up over the bomb and a mirror opens in the
	 * air. A sphere of the world, buildings, trees, soil and rock, peels away from the top down and reappears
	 * upside down above the mirror, until the whole town hangs inverted in the sky over a round pit. It hangs, the
	 * engine fails, and the island shatters: half of it rains down across the land, the rest comes apart into light.
	 */
	public static class InversionEngine extends Base {
		private long[] order;
		private int cursor;
		private final List<Long> island = new ArrayList<>();
		private int cx, cy, cz;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				cx = MathHelper.floor(g.x);
				cy = MathHelper.floor(g.y);
				cz = MathHelper.floor(g.z);
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.inversion_engine", CINE_INVERSION, Kit.dir(yaw()), Inversion.CINE);
				Vfx.newFx(world, FX_INVERSION, g, g, (float) Inversion.R, 0xC0A0FF, 0, 0, Inversion.END);
				Vfx.skyTint(world, g, 100000, 0x180C28, 0.7F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.5F);
			}
			if (t > Inversion.CHARGE && t < Inversion.LIFT && t % 10 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.5F + t * 0.006F);
				Vfx.shake(world, g, 400, 0.3F + t * 0.01F);
			}
			if (t == Inversion.LIFT) {
				CityDestructionEvents.frames(world, g, 1);
				Vfx.screen(world, g, 100000, 4, 0.9F, 6);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.7F);
				Kit.sound(world, g, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 64.0F, 0.5F);
				order = sphereTopDown();
			}
			if (t >= Inversion.LIFT && t < Inversion.HANG_END) {
				if (order != null && Destruction.enabled(world)) {
					int want = (int) (order.length * Inversion.lifted(t + 1));
					transfer(want, 9000);
				}
				if (t % 8 == 0) {
					for (LivingEntity e : Kit.living(world, g, Inversion.R + 4)) {
						if (!Kit.creativeOrSpectator(e) && t < Inversion.LIFT_END + 20) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 14, 4, false, false));
						}
					}
					if (t < Inversion.LIFT_END) {
						Kit.sound(world, g.add(0, Inversion.HEIGHT * 0.5, 0), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 48.0F, 0.5F + world.random.nextFloat());
					}
				}
			}
			if (t == Inversion.LIFT_END) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.4F);
				Vfx.shake(world, g, 100000, 1.4F);
			}
			if (t > Inversion.LIFT_END && t < Inversion.SHATTER && t % 30 == 0) {
				// It creaks: an upside-down town is not meant to hang there.
				Kit.sound(world, g.add(0, Inversion.HEIGHT, 0), SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, 64.0F, 0.3F);
				Kit.sound(world, g.add(0, Inversion.HEIGHT, 0), ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			}
			if (t == Inversion.SHATTER) {
				shatter(g);
			}
			return t >= Inversion.END;
		}

		/** Every position in the sphere, top layer first (roofs and treetops go before the ground and the rock). */
		private long[] sphereTopDown() {
			int r = MathHelper.ceil(Inversion.R);
			List<Long> list = new ArrayList<>();
			for (int dy = r; dy >= -r; dy--) {
				List<Long> layer = new ArrayList<>();
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dy * dy + dz * dz <= Inversion.R * Inversion.R) {
							layer.add(BlockPos.asLong(cx + dx, cy + dy, cz + dz));
						}
					}
				}
				Collections.shuffle(layer, new java.util.Random(dy * 31L + cx));
				list.addAll(layer);
			}
			long[] out = new long[list.size()];
			for (int i = 0; i < out.length; i++) {
				out[i] = list.get(i);
			}
			return out;
		}

		/** Moves blocks up to index {@code want}: each one is removed below and set again, mirrored, above the mirror. */
		private void transfer(int want, int budget) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			while (cursor < Math.min(want, order.length) && budget > 0) {
				p.set(order[cursor++]);
				if (!world.isChunkLoaded(p)) {
					continue;
				}
				budget--;
				BlockState s = world.getBlockState(p);
				if (s.isAir()) {
					continue;
				}
				if (!s.getFluidState().isEmpty()) {
					// Water and lava do not survive the trip; they just drain out of the pit.
					if (s.getBlock() instanceof net.minecraft.block.FluidBlock) {
						put(p.toImmutable(), AIR);
					}
					continue;
				}
				if (!breakable(p, s)) {
					continue;
				}
				Destruction.record(world, p.toImmutable(), s);
				BlockPos to = new BlockPos(p.getX(), cy + Inversion.mirror(p.getY() - cy), p.getZ());
				put(p.toImmutable(), AIR);
				if (put(to, stable(s))) {
					island.add(to.asLong());
				}
				budget -= 2;
			}
		}

		/** What a block becomes upside down in the sky: nothing that falls, and leaves that do not decay. */
		private static BlockState stable(BlockState s) {
			if (s.getBlock() instanceof FallingBlock) {
				if (s.isIn(BlockTags.SAND)) {
					return s.isOf(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.getDefaultState() : Blocks.SANDSTONE.getDefaultState();
				}
				return s.isOf(Blocks.GRAVEL) ? Blocks.TUFF.getDefaultState() : Blocks.STONE.getDefaultState();
			}
			if (s.getBlock() instanceof LeavesBlock) {
				return s.with(LeavesBlock.PERSISTENT, true);
			}
			if (s.contains(net.minecraft.state.property.Properties.BLOCK_HALF)) {
				net.minecraft.block.enums.BlockHalf h = s.get(net.minecraft.state.property.Properties.BLOCK_HALF);
				return s.with(net.minecraft.state.property.Properties.BLOCK_HALF,
						h == net.minecraft.block.enums.BlockHalf.TOP ? net.minecraft.block.enums.BlockHalf.BOTTOM : net.minecraft.block.enums.BlockHalf.TOP);
			}
			if (s.contains(net.minecraft.state.property.Properties.SLAB_TYPE)) {
				net.minecraft.block.enums.SlabType st = s.get(net.minecraft.state.property.Properties.SLAB_TYPE);
				return st == net.minecraft.block.enums.SlabType.DOUBLE ? s : s.with(net.minecraft.state.property.Properties.SLAB_TYPE,
						st == net.minecraft.block.enums.SlabType.TOP ? net.minecraft.block.enums.SlabType.BOTTOM : net.minecraft.block.enums.SlabType.TOP);
			}
			return s;
		}

		/** The engine fails: the island breaks up, much of it raining onto the land and the rest coming apart as light. */
		private void shatter(Vec3d g) {
			Vec3d isl = g.add(0, Inversion.HEIGHT, 0);
			CityDestructionEvents.frames(world, g, 3, 1, 3);
			Vfx.screen(world, g, 100000, 4, 0.8F, 4);
			Vfx.flash(world, isl, 0.5F, 100000, 0xD0C0FF);
			Vfx.ring(world, isl, 260.0F, 6.0F, 0xC0A0FF, false, 50);
			Vfx.shake(world, g, 100000, 2.6F);
			Kit.sound(world, isl, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.3F);
			Kit.sound(world, isl, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.4F);
			Kit.sound(world, isl, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			if (!Destruction.enabled(world)) {
				return;
			}
			Collections.shuffle(island, new java.util.Random(cx * 31L + cz));
			int falls = Math.min(island.size(), Math.min(1400, Kit.debrisBudget(world, g)));
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int i = 0; i < falls; i++) {
				p.set(island.get(i));
				BlockState s = world.getBlockState(p);
				if (s.isAir() || s.hasBlockEntity()) {
					continue;
				}
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
				Vec3d out = new Vec3d(p.getX() - isl.x, 0, p.getZ() - isl.z);
				double l = out.length();
				Vec3d dir = l > 1.0E-3 ? out.multiply(1 / l) : Vec3d.ZERO;
				Kit.velocity(fb, dir.multiply(0.4 + world.random.nextDouble() * 1.4).add(0, world.random.nextDouble() * 0.7, 0));
			}
			Ruin.disintegrate(world, isl, Inversion.R + 3, Blocks.PEARLESCENT_FROGLIGHT.getDefaultState(), ParticleTypes.END_ROD, 70);
		}
	}
}
