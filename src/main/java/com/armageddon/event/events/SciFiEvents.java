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
import com.armageddon.util.SciShapes.Dyson;
import com.armageddon.util.SciShapes.Gravity;
import com.armageddon.util.SciShapes.Ion;
import com.armageddon.util.SciShapes.Resonance;
import com.armageddon.util.SciShapes.Tesla;
import com.armageddon.util.SciShapes.Wave;
import com.armageddon.vfx.VfxIds;
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
 * resonance that shakes every building apart, a tesla tower whose lightning tears structures to pieces, a mirror
 * swarm that cuts canyons across the map, gravity switched off and back on too hard, and a wave of rock that
 * scrapes the land down to stone.
 */
public final class SciFiEvents {
	private SciFiEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: SciFx). */
	public static final int FX_ION = 40, FX_RESONANCE = 41, FX_TESLA = 42, FX_DYSON = 43, FX_GRAVITY = 44, FX_WAVE = 45;
	/** Weapon effects. */
	public static final int FX_HELIX = 46, FX_GRAVITON = 47, FX_SUNFALL = 48, FX_HALO = 49;
	/** Cinematic scene ids (client: Cinematics.SciCine). */
	public static final int CINE_DYSON = 8, CINE_GRAVITY = 9, CINE_WAVE = 10;

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	abstract static class Base extends WorldEvent {
		final List<TerrainJob> jobs = new ArrayList<>();
		ProtectedRegions regions;

		net.minecraft.server.world.ServerWorld w() {
			return world;
		}

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
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.ION_CHARGE, g, (float) (Ion.R / 64.0));
			}
			if (t == Ion.IGNITE) {
				CityDestructionEvents.frames(world, g, 3, 1);
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.ION_IGNITE, g, (float) (Ion.R / 64.0));
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
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.ION_CARVE, new Vec3d(p.x, Kit.ground(world, p.x, p.z).y + 0.3, p.z), 1.0F);
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

	/** Turns the blocks of a structure into falling rubble (up to the debris budget); the rest are simply gone. */
	static int collapse(Base e, List<BlockPos> blocks, Vec3d kick, double spread) {
		int budget = Kit.debrisBudget(e.w(), Vec3d.ofCenter(blocks.isEmpty() ? BlockPos.ORIGIN : blocks.get(0)));
		int thrown = 0;
		for (BlockPos p : blocks) {
			BlockState s = e.w().getBlockState(p);
			if (!e.breakable(p, s)) {
				continue;
			}
			if (thrown < budget && (p.getX() + p.getY() * 3 + p.getZ() * 7) % 3 == 0) {
				Destruction.record(e.w(), p, s);
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(e.w(), p, s);
				Kit.velocity(fb, kick.add((e.w().random.nextDouble() - 0.5) * spread, e.w().random.nextDouble() * spread * 0.5,
						(e.w().random.nextDouble() - 0.5) * spread));
				thrown++;
			} else {
				e.put(p, AIR);
			}
		}
		return thrown;
	}

	// ====================================================================== RESONANCE CASCADE

	/**
	 * RESONANCE CASCADE (city destruction). A tuning fork the height of a house rises and starts to hum. The hum
	 * climbs until it finds the frequency of the town, and then everything built shakes itself apart: behind a
	 * spreading front, walls, roofs and trees come down in heaps of rubble, glass bursts and the ground turns to mud.
	 */
	public static class ResonanceCascade extends Base {
		private StrongBombEvents.RingSweep sweep;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				CelestialEvents.announce(world, g, "block.armageddon.resonance_cascade");
				Vfx.newFx(world, FX_RESONANCE, g, g, (float) Resonance.R, 0xA0F0FF, 0, 0, Resonance.END);
				Vfx.cutscene(world, g, 80);
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.5F);
			}
			double hum = Resonance.hum(t);
			if (t >= Resonance.HUM && t < Resonance.CASCADE_END && t % 6 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.5F + (float) hum * 1.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.4F + (float) hum);
				Vfx.shake(world, g, 300, 0.3F + (float) hum * 1.2F);
				Vfx.play(world, VfxIds.FORK_HUM, g.add(0, Resonance.FORK_H * 0.85, 0), 0.5F + (float) hum);
			}
			if (t >= Resonance.HUM && t < Resonance.CASCADE_END && t % 8 == 0 && hum > 0.05) {
				// The ground shivering like a speaker cone: dirt hopping wherever the note reaches.
				Vfx.play(world, VfxIds.DUST_DANCE, g, (float) Math.max(0.15, hum));
			}
			if (t >= Resonance.HUM && t < Resonance.CASCADE && t % 4 == 0 && Destruction.enabled(world)) {
				// Before the cascade: dust and loose pieces shaken off the buildings.
				for (int i = 0; i < 1 + (int) (hum * 6); i++) {
					Vec3d p = com.armageddon.event.Climax.around(world, g, 6, Resonance.R * hum + 8);
					int x = MathHelper.floor(p.x), z = MathHelper.floor(p.z);
					BlockPos top = new BlockPos(x, top(x, z), z);
					BlockState s = world.getBlockState(top);
					if (!Devastation.natural(s) && breakable(top, s)) {
						collapse(this, List.of(top), Vec3d.ZERO, 0.2);
					}
				}
			}
			if (t == Resonance.CASCADE) {
				CityDestructionEvents.frames(world, g, 0);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 1.2F);
				sweep = new StrongBombEvents.RingSweep((int) Resonance.R);
				Vfx.play(world, VfxIds.CASCADE_FRONT, g, 1.0F);
			}
			if (sweep != null && t <= Resonance.CASCADE_END + 4) {
				double front = Resonance.front(t);
				if (Destruction.enabled(world)) {
					sweep.advance(front, 260, (dx, dz, d) -> shake(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz));
				}
				if (t % 3 == 0) {
					for (LivingEntity e : Kit.living(world, g, front + 4)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false));
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 2, false, false));
							e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 1.5F);
						}
					}
					Kit.sound(world, g, SoundEvents.BLOCK_STONE_BREAK, 64.0F, 0.5F);
					Kit.sound(world, g, SoundEvents.BLOCK_GRAVEL_BREAK, 64.0F, 0.6F);
				}
			}
			if (t == Resonance.SHATTER) {
				CityDestructionEvents.frames(world, g, 3);
				Vfx.play(world, VfxIds.FORK_SHATTER, g.add(0, Resonance.FORK_H * 0.7, 0), 1.0F);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 64.0F, 0.5F);
				Vfx.shake(world, g, 400, 1.6F);
			}
			return t >= Resonance.END;
		}

		/** One column of the town shakes apart: everything built or grown on it comes down; the soil goes to mud. */
		private void shake(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			int surf = surface(x, z), top = Math.min(top(x, z), surf + 50);
			List<BlockPos> built = new ArrayList<>();
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = surf + 1; y <= top; y++) {
				BlockState s = world.getBlockState(p.set(x, y, z));
				if (s.isIn(BlockTags.IMPERMEABLE) || s.isOf(Blocks.GLASS_PANE) || s.getBlock() instanceof net.minecraft.block.StainedGlassPaneBlock) {
					put(p.toImmutable(), AIR);
					if (y % 3 == 0) {
						world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, s), x + 0.5, y + 0.5, z + 0.5, 4, 0.3, 0.3, 0.3, 0.1);
					}
				} else if (!s.isAir()) {
					built.add(p.toImmutable());
				}
			}
			// The surface block itself counts if it is built (a floor, a road, a wall's footing).
			BlockState ground = world.getBlockState(p.set(x, surf, z));
			if (!Devastation.natural(ground)) {
				built.add(p.toImmutable());
			}
			if (!built.isEmpty()) {
				collapse(this, built, new Vec3d(0, 0.05, 0), 0.25);
			}
			// Soil liquefies: the top settles a block into mud.
			if ((ground.isOf(Blocks.GRASS_BLOCK) || ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.SAND) || ground.isOf(Blocks.COARSE_DIRT)
					|| ground.isOf(Blocks.PODZOL)) && SciShapes.hash(x, z) < 0.55) {
				put(new BlockPos(x, surf, z), AIR);
				put(new BlockPos(x, surf - 1, z), Blocks.MUD.getDefaultState());
			}
		}
	}

	// ====================================================================== TESLA DOMINION

	/**
	 * TESLA DOMINION (city destruction). A lattice tower rises and the storm starts: arcs jump from its crown to the
	 * tallest things around. Lightning goes through buildings, not around them: each strike chains from block to
	 * block, blowing the pieces off as it goes, and ploughs a smoking furrow through the ground where it earths.
	 * Then ball lightning grows on the crown and discharges everywhere at once, and the tower comes down.
	 */
	public static class TeslaDominion extends Base {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d crown = Tesla.crown(g, t);
			if (t == 0) {
				CelestialEvents.announce(world, g, "block.armageddon.tesla_dominion");
				Vfx.newFx(world, FX_TESLA, g, new Vec3d(yaw(), 0, 0), (float) Tesla.REACH, 0x90D8FF, 0, 0, Tesla.END);
				Vfx.skyTint(world, g, 100000, 0x101830, 0.75F);
				world.setWeather(0, 1600, true, true);
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.4F);
			}
			if (t > Tesla.RISE && t < Tesla.RISEN && t % 10 == 0) {
				Vfx.shake(world, g, 300, 0.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_COPPER_PLACE, 48.0F, 0.5F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 48.0F, 0.6F);
			}
			if (t >= Tesla.RISEN && t < Tesla.FALL && t % 4 == 0) {
				Vfx.play(world, VfxIds.TESLA_CORONA, crown, 1.0F);
			}
			if (t >= Tesla.STORM && t < Tesla.DISCHARGE) {
				int every = Math.max(2, 9 - (t - Tesla.STORM) / 50);
				if (t % every == 0) {
					strike(g, crown, Tesla.REACH, 8 + world.random.nextInt(10));
				}
			}
			if (t >= Tesla.ORB && t < Tesla.DISCHARGE) {
				if (t % 4 == 0) {
					Kit.sound(world, crown, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F + (t - Tesla.ORB) * 0.02F);
					Vfx.shake(world, g, 400, 0.4F + (t - Tesla.ORB) * 0.015F);
				}
				if (t % 5 == 0) {
					com.armageddon.detonation.Detonations.pull(world, g, 60, 0.06);
				}
				if (t % 3 == 0) {
					// Sparks pulled out of the air into the ball lightning (it sits where the client draws it).
					float k = (t - Tesla.ORB) / (float) (Tesla.DISCHARGE - Tesla.ORB);
					Vfx.play(world, VfxIds.BALL_FEED, crown.add(0, 4 + (2 + k * 11) * 0.6, 0), 0.6F + k);
				}
			}
			if (t == Tesla.DISCHARGE) {
				CityDestructionEvents.frames(world, g, 3, 1, 3);
				Vfx.flash(world, crown, 0.4F, 100000, 0xC0E8FF);
				Vfx.play(world, VfxIds.TESLA_NOVA, crown, 1.0F);
				Vfx.shake(world, g, 100000, 2.6F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.8F);
				for (int i = 0; i < 36; i++) {
					double a = i * MathHelper.TAU / 36 + world.random.nextDouble() * 0.15;
					double d = 25 + world.random.nextDouble() * (Tesla.REACH - 25);
					Vec3d hit = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					DetonationManager.schedule(world, i % 5, () -> strikeAt(g, crown, hit, 20 + world.random.nextInt(18)));
				}
				for (LivingEntity e : Kit.living(world, g, Tesla.REACH)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 40.0F);
					}
				}
			}
			if (t == Tesla.FALL) {
				// The tower comes down: it falls along a line and smashes whatever it lands on.
				Kit.sound(world, g, SoundEvents.BLOCK_CHAIN_BREAK, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.4F);
			}
			if (t == Tesla.FALL + 30) {
				Vec3d dir = Kit.dir(yaw());
				Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_DEATH, 64.0F, 0.3F);
				Vfx.shake(world, g, 400, 2.0F);
				for (int i = 4; i < (int) Tesla.TOWER_H; i += 3) {
					Vec3d at = Kit.ground(world, g.x + dir.x * i, g.z + dir.z * i);
					int d = i;
					DetonationManager.schedule(world, i / 12, () -> {
						Vfx.play(world, VfxIds.TOWER_DUST, at, 1.0F);
						if (Destruction.enabled(world)) {
							plough(at.x, at.z, Math.atan2(dir.z, dir.x) + Math.PI / 2, 4, 0);
							plough(at.x, at.z, Math.atan2(dir.z, dir.x) - Math.PI / 2, 4, 0);
						}
						Kit.damage(world, at, 5, 30.0F, ModDamageTypes.SHRAPNEL, null, owner());
					});
				}
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
			Vfx.play(world, VfxIds.TESLA_HIT, hit, 1.0F);
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
				chain(BlockPos.ofFloored(hit).down(), 6);
				Vec3d out = new Vec3d(hit.x - g.x, 0, hit.z - g.z);
				double heading = Math.atan2(out.z, out.x);
				plough(hit.x, hit.z, heading, length, 1);
			}
		}

		/**
		 * The bolt goes through what it hit: it jumps from block to built block, and each one it passes through bursts
		 * apart with the blocks around it thrown off.
		 */
		private void chain(BlockPos from, int hops) {
			BlockPos at = from;
			for (int h = 0; h < hops; h++) {
				BlockPos next = null;
				for (int tries = 0; tries < 14 && next == null; tries++) {
					BlockPos c = at.add(world.random.nextInt(11) - 5, -world.random.nextInt(6), world.random.nextInt(11) - 5);
					BlockState s = world.getBlockState(c);
					if (!s.isAir() && !Devastation.natural(s) && breakable(c, s)) {
						next = c;
					}
				}
				if (next == null) {
					return;
				}
				Vfx.arc(world, Vec3d.ofCenter(at), Vec3d.ofCenter(next), 0.9F, 0xC8ECFF);
				List<BlockPos> burst = new ArrayList<>();
				for (BlockPos q : BlockPos.iterate(next.add(-1, -1, -1), next.add(1, 1, 1))) {
					burst.add(q.toImmutable());
				}
				Vec3d kick = Vec3d.ofCenter(next).subtract(Vec3d.ofCenter(at)).normalize().multiply(0.5).add(0, 0.4, 0);
				collapse(this, burst, kick, 0.6);
				world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, next.getX() + 0.5, next.getY() + 0.5, next.getZ() + 0.5, 12, 0.5, 0.5, 0.5, 0.3);
				at = next;
			}
		}

		/**
		 * Where the charge earths it ploughs: a forking furrow torn through the ground and whatever stands on it,
		 * the clods thrown up on either side, the bottom charred and smoking.
		 */
		private void plough(double x, double z, double heading, int length, int forks) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int i = 0; i < length; i++) {
				heading += world.random.nextGaussian() * 0.4;
				x += Math.cos(heading);
				z += Math.sin(heading);
				int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
				if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
					return;
				}
				int surf = surface(bx, bz), top = Math.min(top(bx, bz), surf + 14);
				int depth = i < length / 2 ? 3 : 2;
				List<BlockPos> torn = new ArrayList<>();
				for (int y = top; y > surf - depth; y--) {
					torn.add(new BlockPos(bx, y, bz));
				}
				Vec3d side = new Vec3d(-Math.sin(heading), 0, Math.cos(heading)).multiply(world.random.nextBoolean() ? 0.35 : -0.35);
				collapse(this, torn, side.add(0, 0.55, 0), 0.3);
				BlockState floor = world.getBlockState(p.set(bx, surf - depth, bz));
				if (breakable(p, floor) && Devastation.natural(floor)) {
					put(p.toImmutable(), SciShapes.hash(bx, bz) < 0.4 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
				}
				if (i % 4 == 0) {
					world.spawnParticles(ParticleTypes.LARGE_SMOKE, bx + 0.5, surf - depth + 1.5, bz + 0.5, 2, 0.3, 0.2, 0.3, 0.02);
				}
				if (forks > 0 && world.random.nextFloat() < 0.12F) {
					plough(x, z, heading + (world.random.nextBoolean() ? 0.8 : -0.8), (int) ((length - i) * 0.6), forks - 1);
				}
			}
		}
	}

	// ====================================================================== DYSON LANCE

	/**
	 * DYSON LANCE (world destruction). A swarm of orbital mirrors turns and focuses the sun into one beam. It comes
	 * down at the edge of the map and draws a straight line through the world: a canyon thirty blocks deep and three
	 * hundred long either side, floored with magma. Then a second, and a third, crossing at the bomb, until the land
	 * is cut into a six-pointed star. Last, it holds on the crossing and drills straight down.
	 */
	public static class DysonLance extends Base {
		private double yaw;
		private final TerrainJob[] cuts = new TerrainJob[3];
		private TerrainJob drill;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				yaw = Math.toRadians(yaw());
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.dyson_lance", CINE_DYSON, Kit.dir(yaw()), Dyson.CINE);
				Vfx.newFx(world, FX_DYSON, g, new Vec3d(yaw, 0, 0), (float) Dyson.L, 0xFFE0A0, 0, 0, Dyson.END);
				Vfx.skyTint(world, g, 100000, 0x201008, 0.55F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.5F);
			}
			if (t > Dyson.SWARM && t < Dyson.FOCUS && t % 15 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 64.0F, 0.6F + t * 0.005F);
			}
			if (t == Dyson.FOCUS) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
				Vfx.shake(world, g, 100000, 1.0F);
			}
			for (int k = 0; k < 3; k++) {
				double along = Dyson.along(k, t);
				if (t == Dyson.cutStart(k)) {
					Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.6F);
					Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.4F);
					if (Destruction.enabled(world)) {
						cuts[k] = canyon(g, Dyson.axis(k, yaw));
					}
				}
				if (!Double.isNaN(along)) {
					Vec3d ax = Dyson.axis(k, yaw);
					Vec3d at = Kit.ground(world, g.x + ax.x * along, g.z + ax.z * along);
					Kit.damage(world, at, Dyson.HALF_W + 4, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
					if (t % 4 == 0) {
						Kit.sound(world, at, SoundEvents.BLOCK_FIRE_EXTINGUISH, 64.0F, 0.5F);
						Vfx.shake(world, at, 200, 1.4F);
					}
					if (cuts[k] != null) {
						cuts[k].limit = along + Dyson.L;
					}
				}
				if (cuts[k] != null) {
					cuts[k].step(4500);
				}
			}
			if (t == Dyson.DRILL) {
				CityDestructionEvents.frames(world, g, 3, 1);
				Vfx.flash(world, g, 0.4F, 100000, 0xFFF0C0);
				Vfx.shake(world, g, 100000, 2.4F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.4F);
				Vfx.play(world, VfxIds.LANCE_DRILL, g, 1.0F);
				if (Destruction.enabled(world)) {
					drill = TerrainJob.disc(world, g, Destruction.radius(world, 22.0F), c -> {
						double d = Math.hypot(c.x + 0.5 - g.x, c.z + 0.5 - g.z);
						int floor = Math.max(c.bottomY() + 6, MathHelper.floor(g.y) - 90 + (int) (d * 1.5));
						c.clear(floor, Math.min(c.top(), MathHelper.floor(g.y) + 40));
						c.set(floor - 1, Blocks.MAGMA_BLOCK.getDefaultState());
					});
					drill.limit = 0;
				}
			}
			if (drill != null) {
				drill.limit = (t - Dyson.DRILL) * 0.4;
				drill.step(3000);
				if (t % 5 == 0 && t < Dyson.END) {
					Kit.damage(world, g, 24, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
				}
			}
			boolean done = drill == null || drill.isDone();
			for (TerrainJob c : cuts) {
				done &= c == null || c.isDone();
			}
			return t >= Dyson.END && done || t > Dyson.END + 1500;
		}

		/** The columns of one cut, keyed by distance along it, carved into a V-floored canyon with a magma bed. */
		private TerrainJob canyon(Vec3d g, Vec3d ax) {
			Vec3d side = new Vec3d(-ax.z, 0, ax.x);
			int n = (int) (2 * Dyson.L + 1), w = (int) Math.ceil(Dyson.HALF_W) + 1;
			long[] cols = new long[n * (2 * w + 1)];
			float[] keys = new float[cols.length];
			int i = 0;
			for (int a = 0; a < n; a++) {
				double along = -Dyson.L + a;
				for (int s2 = -w; s2 <= w; s2++) {
					cols[i] = BlockPos.asLong(MathHelper.floor(g.x + ax.x * along + side.x * s2), 0, MathHelper.floor(g.z + ax.z * along + side.z * s2));
					keys[i] = (float) a;
					i++;
				}
			}
			int base = MathHelper.floor(g.y);
			return new TerrainJob(world, cols, keys, c -> {
				Vec3d rel = new Vec3d(c.x + 0.5 - g.x, 0, c.z + 0.5 - g.z);
				double across = Math.abs(rel.dotProduct(side)) / Dyson.HALF_W;
				if (across > 1.15) {
					return;
				}
				double end = Math.abs(rel.dotProduct(ax)) / Dyson.L;
				double depth = Dyson.DEPTH * (1 - Math.pow(Math.min(1, across), 4)) * (1 - Math.pow(end, 6));
				int floor = base - (int) depth;
				int top = Math.min(c.top(), base + 60);
				if (across > 1.0) {
					// The lip: scorched and slumped one block.
					if (c.get(c.surface()).isOpaque()) {
						c.set(c.surface(), Blocks.BLACKSTONE.getDefaultState());
					}
					return;
				}
				c.clear(floor + 1, top);
				c.set(floor, c.noise(4) < 0.6 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.LAVA.getDefaultState());
				c.set(floor - 1, Blocks.MAGMA_BLOCK.getDefaultState());
			});
		}
	}

	// ====================================================================== GRAVITY KILLSWITCH

	/**
	 * GRAVITY KILLSWITCH (world destruction). A generator hums, and then gravity simply stops. The town floats up off
	 * the ground: houses coming apart, trees, the topsoil, people, all tumbling slowly upward. It hangs there. Then
	 * gravity comes back many times over and everything is slammed down at once, and the ground under it is crushed.
	 */
	public static class GravityKillswitch extends Base {
		private final List<FallingBlockEntity> floating = new ArrayList<>();
		private StrongBombEvents.RingSweep sweep;
		private TerrainJob crush;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.gravity_killswitch", CINE_GRAVITY, Kit.dir(yaw()), Gravity.CINE);
				Vfx.newFx(world, FX_GRAVITY, g, g, (float) Gravity.R, 0xB0A0FF, 0, 0, Gravity.END);
				Kit.sound(world, g, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.5F);
			}
			if (t > Gravity.HUM && t < Gravity.OFF && t % 10 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F + t * 0.01F);
				Vfx.shake(world, g, 300, 0.2F + t * 0.01F);
				Vfx.play(world, VfxIds.GRAVITY_HUM, g.add(0, 5, 0), 0.6F + 0.6F * t / Gravity.OFF);
			}
			if (t == Gravity.OFF) {
				CityDestructionEvents.frames(world, g, 1);
				Vfx.screen(world, g, 100000, 0, 0.4F, Gravity.SLAM - Gravity.OFF);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 64.0F, 0.4F);
				sweep = new StrongBombEvents.RingSweep((int) Gravity.R);
				// Everything loose starts drifting up; the effect drops it all again at the slam (SLAM - OFF ticks later).
				Vfx.play(world, VfxIds.GRAVITY_FLOAT, g, 1.0F);
			}
			if (t > Gravity.OFF && t < Gravity.SLAM) {
				double front = Gravity.R * Math.min(1.0, (t - Gravity.OFF) / 90.0);
				if (sweep != null && Destruction.enabled(world)) {
					sweep.advance(front, 400, (dx, dz, d) -> float_(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz));
				}
				if (t % 6 == 0) {
					for (LivingEntity e : Kit.living(world, g, front)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 0, false, false));
						}
					}
				}
				for (FallingBlockEntity fb : floating) {
					if (fb.isAlive() && fb.getY() > g.y + Gravity.CEIL) {
						Kit.velocity(fb, fb.getVelocity().multiply(0.5, 0.0, 0.5));
					}
				}
			}
			if (t == Gravity.SLAM) {
				CityDestructionEvents.frames(world, g, 0, 2, 0);
				Vfx.screen(world, g, 100000, 0, 0.0F, 1);
				Vfx.play(world, VfxIds.GRAVITY_SLAM, g, 1.0F);
				Vfx.shake(world, g, 100000, 3.0F);
				Vfx.fov(world, g, 18.0F, 30);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.3F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.2F);
				for (FallingBlockEntity fb : floating) {
					if (fb.isAlive()) {
						fb.setNoGravity(false);
						Kit.velocity(fb, new Vec3d(0, -3.5, 0));
					}
				}
				for (LivingEntity e : Kit.living(world, g, Gravity.R + 10)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.removeStatusEffect(StatusEffects.LEVITATION);
						Kit.push(e, new Vec3d(0, -4.0, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.NUCLEAR_BLAST, null, owner()), 30.0F);
					}
				}
				if (Destruction.enabled(world)) {
					crush = TerrainJob.disc(world, g, Gravity.R, c -> {
						int surf = c.surface();
						int sink = 1 + (int) (c.noise(2) * 3);
						for (int y = surf; y > surf - sink; y--) {
							c.set(y, AIR);
						}
						double n = c.noise(3);
						c.set(surf - sink, n < 0.3 ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : n < 0.65 ? Blocks.GRAVEL.getDefaultState()
								: Blocks.COBBLESTONE.getDefaultState());
					});
					crush.limit = 0;
				}
			}
			if (crush != null) {
				crush.limit = (t - Gravity.SLAM) * 6.0;
				crush.step(5000);
				if ((t - Gravity.SLAM) % 4 == 0 && crush.limit < Gravity.R) {
					Kit.ringCrack(world, g, crush.limit, 3, null, 0.25F);
				}
			}
			return t >= Gravity.END && (crush == null || crush.isDone()) || t > Gravity.END + 1200;
		}

		/** A column lets go: what stands on it, and its topsoil, float up; what the budget cannot carry just goes. */
		private void float_(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			int surf = surface(x, z), top = Math.min(top(x, z), surf + 40);
			int budget = Kit.debrisBudget(world, center());
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = top; y >= surf; y--) {
				BlockState s = world.getBlockState(p.set(x, y, z));
				if (!breakable(p, s) || !s.getFluidState().isEmpty()) {
					continue;
				}
				if (budget > 0 && SciShapes.hash(x * 3 + y, z * 5 - y) < 0.35) {
					Destruction.record(world, p.toImmutable(), s);
					FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
					fb.setNoGravity(true);
					fb.setDestroyedOnLanding();
					Kit.velocity(fb, new Vec3d((world.random.nextDouble() - 0.5) * 0.06, 0.06 + world.random.nextDouble() * 0.14, (world.random.nextDouble() - 0.5) * 0.06));
					floating.add(fb);
					budget--;
				} else {
					put(p.toImmutable(), AIR);
				}
			}
		}

		@Override
		public void finish() {
			Kit.vanish(floating);
			super.finish();
		}
	}

	// ====================================================================== SEISMIC TSUNAMI

	/**
	 * SEISMIC TSUNAMI (world destruction). A piston hammer the size of a tower slams into the ground four times, and
	 * on the last blow the land itself becomes a wave: a crest of rock and soil rolls out from the hammer, throwing
	 * everything on it, and behind it the ground is scraped down to bare stone for three hundred blocks.
	 */
	public static class SeismicTsunami extends Base {
		private StrongBombEvents.RingSweep sweep;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				WorldCutsceneEvents.begin(this, world, g, "block.armageddon.seismic_tsunami", CINE_WAVE, Kit.dir(yaw()), Wave.CINE);
				Vfx.newFx(world, FX_WAVE, g, g, (float) Wave.R, 0x8A7A60, 0, 0, Wave.END);
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.3F);
			}
			for (int i = 0; i < Wave.SLAMS; i++) {
				if (t == Wave.slam(i)) {
					float k = (i + 1) / (float) Wave.SLAMS;
					Vfx.shake(world, g, 100000, 1.0F + k * 1.8F);
					Vfx.ring(world, g.add(0, 0.5, 0), 40.0F + 40 * k, 2.0F, 0x8A7A60, false, 30);
					Vfx.play(world, VfxIds.HAMMER_BLOW, g, 1.0F + 1.2F * k);
					Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.3F);
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.4F - k * 0.15F);
					Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 48.0F * k, 0.5F);
					com.armageddon.event.Climax.groundWave(world, g, 10 + i * 6, 6, 30, 0.4 + k * 0.3);
					if (i == Wave.SLAMS - 1) {
						CityDestructionEvents.frames(world, g, 0, 2);
					}
				}
			}
			if (t == Wave.WAVE) {
				sweep = new StrongBombEvents.RingSweep((int) Wave.R);
			}
			if (sweep != null) {
				double front = Wave.front(t);
				if (Destruction.enabled(world)) {
					sweep.advance(front, 900, (dx, dz, d) -> scrape(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz, d));
				}
				if (t % 2 == 0 && front < Wave.R) {
					com.armageddon.event.Climax.groundWave(world, g, front, 4, 22, 0.9);
					for (LivingEntity e : Kit.living(world, g, front + 8)) {
						double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
						if (d > front - 8 && !Kit.creativeOrSpectator(e)) {
							Vec3d out = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z).normalize();
							Kit.push(e, out.multiply(1.4).add(0, 1.1, 0));
							e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 10.0F);
						}
					}
				}
				if (t % 5 == 0 && front < Wave.R) {
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
					Kit.sound(world, g, SoundEvents.BLOCK_GRAVEL_BREAK, 64.0F, 0.4F);
					Vfx.shake(world, g, 100000, 1.2F);
				}
			}
			return t >= Wave.END;
		}

		/** Behind the crest: scraped down to the rock, the soil and everything on it carried away. */
		private void scrape(int x, int z, double d) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			int surf = surface(x, z), top = Math.min(top(x, z), surf + 60);
			double k = Math.min(1.0, d / 40.0) * Math.min(1.0, (Wave.R - d) / 30.0 + 0.2);
			int scrape = (int) Math.round(Wave.SCRAPE * k * (0.7 + SciShapes.hash(x, z) * 0.6));
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = top; y > surf - scrape; y--) {
				BlockState s = world.getBlockState(p.set(x, y, z));
				if (breakable(p, s)) {
					put(p.toImmutable(), AIR);
				}
			}
			BlockState floor = world.getBlockState(p.set(x, surf - scrape, z));
			if (breakable(p, floor)) {
				double n = SciShapes.hash(x * 7, z * 3);
				put(p.toImmutable(), n < 0.55 ? Blocks.STONE.getDefaultState() : n < 0.8 ? Blocks.COBBLESTONE.getDefaultState()
						: n < 0.93 ? Blocks.GRAVEL.getDefaultState() : Blocks.ANDESITE.getDefaultState());
			}
		}
	}

	// ====================================================================== WEAPONS

	/**
	 * HELIX DISRUPTOR shot: two plasma bolts corkscrewing around one line, boring a spiral tunnel through anything in
	 * their way for 140 blocks, then folding in on each other in an implosion.
	 */
	public static class HelixBolt extends Base {
		public static final double SPEED = 2.6, LEN = 140, R = 2.4, TWIST = 0.32;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d start = center();
			Vec3d dir = new Vec3d(data.getDouble("Dx"), data.getDouble("Dy"), data.getDouble("Dz")).normalize();
			if (t == 0) {
				Vfx.newFx(world, FX_HELIX, start, dir, (float) LEN, 0x80F0FF, (float) SPEED, 0, (int) (LEN / SPEED) + 40);
			}
			Vec3d[] b = basis(dir);
			double s0 = Math.max(0, (t - 1) * SPEED), s1 = Math.min(LEN, t * SPEED);
			for (double s = s0; s < s1; s += 0.8) {
				Vec3d axis = start.add(dir.multiply(s));
				for (int k = 0; k < 2; k++) {
					double th = s * TWIST + k * Math.PI;
					Vec3d p = axis.add(b[0].multiply(Math.cos(th) * R)).add(b[1].multiply(Math.sin(th) * R));
					bore(p, 1.7);
				}
				bore(axis, 1.1);
			}
			if (t % 2 == 0) {
				Vec3d head = start.add(dir.multiply(s1));
				Kit.damage(world, head, R + 2.5, 30.0F, ModDamageTypes.PLASMA, null, owner());
				Kit.sound(world, head, ModSounds.PLASMA_IMPACT, 3.0F, 1.6F);
			}
			if (s1 >= LEN) {
				Vec3d end = start.add(dir.multiply(LEN));
				Ruin.implode(world, end, 6, 22);
				Kit.sound(world, end, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 16.0F, 1.6F);
				return true;
			}
			return false;
		}

		private void bore(Vec3d p, double r) {
			if (!Destruction.enabled(world)) {
				return;
			}
			int ri = MathHelper.ceil(r);
			BlockPos c = BlockPos.ofFloored(p);
			for (BlockPos q : BlockPos.iterate(c.add(-ri, -ri, -ri), c.add(ri, ri, ri))) {
				if (Vec3d.ofCenter(q).squaredDistanceTo(p) > r * r) {
					continue;
				}
				BlockState st = world.getBlockState(q);
				if (breakable(q, st)) {
					put(q.toImmutable(), AIR);
					if ((q.getX() + q.getY() + q.getZ()) % 7 == 0) {
						world.spawnParticles(ParticleTypes.END_ROD, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.05);
					}
				}
			}
		}

		static Vec3d[] basis(Vec3d d) {
			Vec3d up = Math.abs(d.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
			Vec3d u = d.crossProduct(up).normalize();
			return new Vec3d[]{u, u.crossProduct(d).normalize()};
		}
	}

	/** GRAVITON LANCE anchor: a point of crushing gravity that drags in the land around it and squeezes it to a core. */
	public static class Graviton extends Base {
		static final int LIFE = 110;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center().add(0, 3, 0);
			if (t == 0) {
				Vfx.newFx(world, FX_GRAVITON, c, c, 18, 0xB080FF, 0, 0, LIFE);
				Ruin.implode(world, c, 18, 95);
				Kit.sound(world, c, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 32.0F, 0.5F);
			}
			if (t % 10 == 0 && t < 95) {
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_AMBIENT, 32.0F, 0.5F + t * 0.01F);
				Vfx.shake(world, c, 80, 0.4F + t * 0.008F);
			}
			return t >= LIFE;
		}
	}

	/** SUNFALL MORTAR shell: a small plasma sun on a high arc; on landing the ground melts into a lava pool. */
	public static class Sunfall extends Base {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d at = center();
			int flight = Math.max(10, data.getInt("Flight"));
			if (t == 0) {
				Vec3d from = new Vec3d(data.getDouble("Fx"), data.getDouble("Fy"), data.getDouble("Fz"));
				Vfx.newFx(world, FX_SUNFALL, from, at, 18, 0xFFB040, flight, 0, flight + 90);
			}
			if (t == flight) {
				Vfx.flash(world, at, 0.3F, 120, 0xFFD080);
				Vfx.shake(world, at, 80, 1.2F);
				Kit.sound(world, at, SoundEvents.ITEM_FIRECHARGE_USE, 32.0F, 0.5F);
				Kit.sound(world, at, SoundEvents.BLOCK_LAVA_EXTINGUISH, 32.0F, 0.4F);
				for (LivingEntity e : Kit.living(world, at, 20)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.setOnFireFor(10);
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), (float) (40 * (1 - e.getPos().distanceTo(at) / 22)));
					}
				}
				Ruin.melt(world, at, 18, 1.2);
			}
			return t >= flight + 10;
		}
	}

	/**
	 * HALO CUTTER strike: a target circle is painted on the ground, an orbital beam comes down and draws a ring canyon
	 * around it, and then the whole disc inside drops out, leaving a round shaft with a magma floor.
	 */
	public static class HaloCut extends Base {
		public static final int MARK = 50, CUT = 50, CUT_LEN = 120, DROP = 185, END = 260;
		public static final double R = 34, HALF_W = 2.6, DEPTH = 30;
		private TerrainJob ring, plug;

		/** Angle the beam has reached around the ring (radians), from the player's heading. */
		public static double angle(double t, double yaw) {
			return yaw + MathHelper.clamp((t - CUT) / CUT_LEN, 0.0, 1.0) * MathHelper.TAU;
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			double yaw = Math.toRadians(data.getFloat("Yaw"));
			int base = MathHelper.floor(g.y);
			if (t == 0) {
				Vfx.newFx(world, FX_HALO, g, new Vec3d(yaw, 0, 0), (float) R, 0xFF5030, 0, 0, END);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 1.2F);
			}
			if (t == CUT) {
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.8F);
				if (Destruction.enabled(world)) {
					// The ring is keyed by angle, so the beam draws it round.
					ring = byAngle(world, g, yaw);
				}
			}
			if (t >= CUT && t <= CUT + CUT_LEN) {
				double a = angle(t, yaw);
				Vec3d p = Kit.ground(world, g.x + Math.cos(a) * R, g.z + Math.sin(a) * R);
				Kit.damage(world, p, HALF_W + 4, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
				world.spawnParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 6, 1.5, 0.5, 1.5, 0.2);
				if (t % 4 == 0) {
					Kit.sound(world, p, SoundEvents.BLOCK_FIRE_EXTINGUISH, 32.0F, 0.6F);
				}
				if (ring != null) {
					ring.limit = (a - yaw);
				}
			}
			if (ring != null) {
				ring.step(3500);
			}
			if (t == DROP) {
				CityDestructionEvents.frames(world, g, 0);
				Vfx.shake(world, g, 300, 2.0F);
				Vfx.ring(world, g.add(0, 0.5, 0), (float) R + 6, 3.0F, 0x8A7A68, true, 40);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.4F);
				if (Destruction.enabled(world)) {
					plug = TerrainJob.disc(world, g, R - HALF_W + 0.5, c -> {
						int floor = base - (int) DEPTH;
						c.clear(floor + 1, Math.min(c.top(), base + 50));
						c.set(floor, c.noise(2) < 0.5 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.BLACKSTONE.getDefaultState());
					});
					plug.limit = 0;
				}
			}
			if (plug != null) {
				plug.limit = (t - DROP) * 2.5;
				plug.step(6000);
			}
			return t >= END && (ring == null || ring.isDone()) && (plug == null || plug.isDone()) || t > END + 600;
		}

		/** The ring's columns, keyed by their angle around the centre starting from {@code yaw}. */
		private static TerrainJob byAngle(net.minecraft.server.world.ServerWorld w, Vec3d g, double yaw) {
			int r = MathHelper.ceil(R + HALF_W + 1);
			List<Long> cols = new ArrayList<>();
			List<Float> keys = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (Math.abs(d - R) > HALF_W + 0.5) {
						continue;
					}
					double a = Math.atan2(dz, dx) - yaw;
					a = ((a % MathHelper.TAU) + MathHelper.TAU) % MathHelper.TAU;
					cols.add(BlockPos.asLong(MathHelper.floor(g.x) + dx, 0, MathHelper.floor(g.z) + dz));
					keys.add((float) a);
				}
			}
			long[] c = new long[cols.size()];
			float[] k = new float[cols.size()];
			for (int i = 0; i < c.length; i++) {
				c[i] = cols.get(i);
				k[i] = keys.get(i);
			}
			int base = MathHelper.floor(g.y);
			return new TerrainJob(w, c, k, col -> {
				double d = Math.hypot(col.x + 0.5 - g.x, col.z + 0.5 - g.z);
				double across = Math.abs(d - R) / HALF_W;
				if (across > 1) {
					return;
				}
				int floor = base - (int) (DEPTH * (1 - across * across * 0.5));
				col.clear(floor + 1, Math.min(col.top(), base + 50));
				col.set(floor, Blocks.MAGMA_BLOCK.getDefaultState());
			});
		}
	}
}
