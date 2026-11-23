package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.NewBombShapes;
import com.armageddon.util.NewBombShapes.Ice;
import com.armageddon.util.NewBombShapes.Lidar;
import com.armageddon.util.NewBombShapes.Sun;
import com.armageddon.util.NewBombShapes.Termite;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The five strong bombs: LIDAR STRIKE, SHADOW VOID, SPECTRAL ICE, TERMITE DOMAIN and THE SUN DISAPPEARS. Their
 * looks come from the depth-aware vision pass (lidar point clouds, a volumetric void, the eclipse) and from
 * client effects built off {@link NewBombShapes}, so blocks and visuals agree without extra packets.
 */
public final class StrongBombEvents {
	/** Client effect ids for {@link Vfx#newFx} (see the client's NewFx). */
	public static final int FX_LIDAR = 0, FX_LIDAR_STRIKE = 1, FX_VOID = 2, FX_ICE = 3, FX_TERMITES = 4, FX_SUN = 5;

	private StrongBombEvents() {
	}

	abstract static class Base extends WorldEvent {
		private BlockQueue queue;

		BlockQueue q() {
			if (queue == null) {
				queue = new BlockQueue(world);
			}
			return queue;
		}

		void drain() {
			if (queue != null) {
				queue.drain(BlockQueue.PER_TICK);
			}
		}

		@Override
		public void finish() {
			if (queue != null) {
				queue.handOff();
			}
		}

		void announce(String id) {
			CelestialEvents.announce(world, center(), "block.armageddon." + id);
		}

		void title(Text title, Text sub) {
			Kit.title(world, center(), 100000, title, sub);
		}

		BlockPos top(int x, int z) {
			return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).down();
		}
	}

	/** Column offsets inside a radius, nearest first, consumed as a front sweeps outwards. */
	static final class RingSweep {
		private final int[] dx, dz;
		private final double[] dist;
		private int next;

		RingSweep(int radius) {
			List<int[]> cols = new ArrayList<>();
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (x * x + z * z <= radius * radius) {
						cols.add(new int[]{x, z});
					}
				}
			}
			cols.sort(Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
			dx = new int[cols.size()];
			dz = new int[cols.size()];
			dist = new double[cols.size()];
			for (int i = 0; i < cols.size(); i++) {
				dx[i] = cols.get(i)[0];
				dz[i] = cols.get(i)[1];
				dist[i] = Math.sqrt(dx[i] * dx[i] + dz[i] * dz[i]);
			}
		}

		interface Column {
			void visit(int dx, int dz, double dist);
		}

		/** Visits every column the front has now reached (at most {@code max} per call). */
		void advance(double front, int max, Column c) {
			int n = 0;
			while (next < dx.length && dist[next] <= front && n++ < max) {
				c.visit(dx[next], dz[next], dist[next]);
				next++;
			}
		}
	}

	static boolean living(BlockState s) {
		return s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.SAPLINGS)
				|| s.isIn(BlockTags.CROPS) || s.isIn(BlockTags.REPLACEABLE_BY_TREES) || s.isOf(Blocks.SUGAR_CANE) || s.isOf(Blocks.CACTUS)
				|| s.isOf(Blocks.BAMBOO) || s.isOf(Blocks.PUMPKIN) || s.isOf(Blocks.MELON) || s.isOf(Blocks.HAY_BLOCK) || s.isOf(Blocks.VINE)
				|| s.isOf(Blocks.BROWN_MUSHROOM) || s.isOf(Blocks.RED_MUSHROOM) || s.isOf(Blocks.MOSS_CARPET) || s.isOf(Blocks.SWEET_BERRY_BUSH)
				|| s.isOf(Blocks.KELP) || s.isOf(Blocks.KELP_PLANT) || s.isOf(Blocks.SEAGRASS) || s.isOf(Blocks.TALL_SEAGRASS) || s.isOf(Blocks.LILY_PAD)
				|| s.isOf(Blocks.MUSHROOM_STEM) || s.isOf(Blocks.BROWN_MUSHROOM_BLOCK) || s.isOf(Blocks.RED_MUSHROOM_BLOCK) || s.isOf(Blocks.COCOA);
	}

	// ====================================================================== LIDAR STRIKE

	/**
	 * A survey mast unfolds and the world turns into a point cloud: four lidar pulses sweep out 150 blocks, and
	 * every living thing they touch is tagged. Then TARGETS LOCKED: each mark gets a red reticle, the strikes walk
	 * the scanned map, and the mast overloads in a white blast that rescans everything at once.
	 */
	public static class LidarStrike extends Base {
		private final List<Vec3d> marks = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				announce("lidar_bomb");
				Vfx.newFx(world, FX_LIDAR, g, g, Lidar.RANGE, 0x60FFFF, 0, 0, Lidar.END);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 32.0F, 1.6F);
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 16.0F, 0.6F);
			}
			if (t == Lidar.SCAN) {
				Vfx.vision(world, g, 1, Lidar.RANGE, Lidar.SPEED, 0, Lidar.PERIOD, 0, 0xFFFFFF, Lidar.LOCK - Lidar.SCAN + 40, Lidar.RANGE + 160);
			}
			if (t >= Lidar.SCAN && t < Lidar.LOCK && (t - Lidar.SCAN) % Lidar.PERIOD == 0) {
				pulse(g, (t - Lidar.SCAN) / Lidar.PERIOD + 1);
			}
			if (t >= Lidar.SCAN && t < Lidar.LOCK && t % 4 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 20.0F, 1.8F);
			}
			if (t == Lidar.LOCK) {
				lock(g);
			}
			for (int i = 0; i < marks.size(); i++) {
				if (t == Lidar.STRIKES + i * 3) {
					Vec3d p = marks.get(i);
					Vfx.beam(world, p.add(0, 220, 0), p, 2.2F, 0xFF3020, 8);
					Kit.boom(world, p, 3.4F, 0xFF5030, owner(), true);
				}
			}
			if (t == Lidar.OVERLOAD - 30) {
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 1.4F);
			}
			if (t == Lidar.OVERLOAD) {
				Vfx.vision(world, g, 1, Lidar.RANGE * 1.5F, 7.0F, 0, 0, 0, 0xFFFFFF, 80, Lidar.RANGE + 200);
				Vfx.flash(world, g, 0.9F, 600, 0xC0FFFF);
				Kit.boom(world, g.add(0, 2, 0), 9.0F, 0x90FFFF, owner(), true);
				Vfx.ring(world, g.add(0, 1, 0), 220.0F, 6.0F, 0x80FFFF, true, 50);
				Detonations.blast(world, g, 70, 40.0F, ModDamageTypes.ANNIHILATION, owner(), 3.0, 1.2);
				if (Destruction.enabled(world)) {
					DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 20.0F), CraterJob.Mode.NUKE));
				}
			}
			return t >= Lidar.END;
		}

		private void pulse(Vec3d g, int n) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 64.0F, 2.0F);
			Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 1.4F);
			int contacts = 0;
			for (LivingEntity e : Kit.living(world, g, Lidar.RANGE)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, Lidar.STRIKES - age() + 80, 0, false, false));
				contacts++;
			}
		}

		/** Marks every living thing still in range, then fills the map with terrain marks on a sunflower spiral. */
		private void lock(Vec3d g) {
			Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.8F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.6F);
			for (LivingEntity e : Kit.living(world, g, Lidar.RANGE)) {
				if (!Kit.creativeOrSpectator(e) && marks.size() < 24 && e.getPos().distanceTo(g) > 8) {
					marks.add(e.getPos());
				}
			}
			int terrain = 44 - marks.size();
			for (int i = 0; i < terrain; i++) {
				double a = i * 2.39996;
				double d = 18 + (Lidar.RANGE * 0.85 - 18) * Math.sqrt((i + 0.5) / terrain);
				marks.add(Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d));
			}
			for (int i = 0; i < marks.size(); i++) {
				Vfx.newFx(world, FX_LIDAR_STRIKE, marks.get(i), marks.get(i), 3.0F, 0xFF2020, Lidar.STRIKES - Lidar.LOCK + i * 3, 0, Lidar.STRIKES - Lidar.LOCK + i * 3 + 30);
			}
		}
	}

	// ====================================================================== SHADOW VOID

	/**
	 * A black orb rises and the light drains out of the world. The void grows: a volumetric darkness with an event
	 * core, swallowing the ground shell by shell, tearing blocks up and drinking them, staining the land with sculk
	 * and blackstone. It inhales, everything goes silent... and exhales a shockwave of shadow.
	 */
	public static class ShadowVoid extends Base {
		static final int RISE = 50, GROW_END = 290, INHALE = 300, EXHALE = 345, END = 620;
		static final float MAX_R = 46, CORE = 15, ORB_H = 22;
		private final List<FallingBlockEntity> drawn = new ArrayList<>();
		private double carved;
		private RingSweep stain;

		Vec3d orb() {
			return center().add(0, ORB_H * MathHelper.clamp(age() / (float) RISE, 0.0F, 1.0F), 0);
		}

		double radius(int t) {
			return 4 + (MAX_R - 4) * MathHelper.clamp((t - RISE) / (float) (GROW_END - RISE), 0.0F, 1.0F);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d orb = orb();
			if (t == 0) {
				announce("shadow_void");
				Vfx.newFx(world, FX_VOID, g.add(0, ORB_H, 0), g, MAX_R, 0x9040FF, CORE, 0, END);
				Kit.sound(world, g, ModSounds.BLACK_HOLE, 48.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 48.0F, 0.5F);
			}
			if (t == RISE) {
				Vfx.vision(world, orb, 2, MAX_R, (MAX_R - 4) / (GROW_END - RISE), CORE, 0, 4, 0x9040FF, INHALE - RISE + 25, 600);
				Kit.sound(world, orb, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 64.0F, 0.5F);
			}
			if (t > RISE && t < INHALE) {
				grow(orb, t);
			}
			if (t == INHALE) {
				Vfx.vision(world, orb, 2, MAX_R, -MAX_R / 40.0F, CORE, 0, MAX_R, 0x9040FF, 55, 600);
				Kit.sound(world, orb, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.5F);
			}
			if (t > INHALE && t < EXHALE) {
				for (FallingBlockEntity fb : drawn) {
					if (fb.isAlive()) {
						Kit.velocity(fb, orb.subtract(fb.getPos()).normalize().multiply(2.2));
					}
				}
				Detonations.pull(world, orb, 90, 0.12);
			}
			if (t == EXHALE) {
				exhale(g, orb);
			}
			if (stain != null) {
				double front = (t - EXHALE) * 1.6;
				stain.advance(front, 600, (dx, dz, d) -> stainColumn(g, dx, dz, d, 110));
			}
			if (t > INHALE && t % 20 == 0 && t < END - 40) {
				Vfx.skyTint(world, g, 600, 0x05000A, Math.max(0.15F, 0.9F - (t - EXHALE) * 0.004F));
			}
			drain();
			return t >= END;
		}

		private void grow(Vec3d orb, int t) {
			double r = radius(t);
			double core = CORE * r / MAX_R;
			if (t % 20 == 0) {
				Vfx.skyTint(world, orb, 600, 0x05000A, 0.92F);
				Kit.sound(world, orb, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 48.0F, 0.4F + (float) (r / MAX_R) * 0.4F);
			}
			// The core eats a sphere around itself, one new shell at a time.
			if (t % 6 == 0 && core > carved + 0.8) {
				int R = MathHelper.ceil(core);
				BlockPos c = BlockPos.ofFloored(orb);
				for (int x = -R; x <= R; x++) {
					for (int y = -R; y <= R; y++) {
						for (int z = -R; z <= R; z++) {
							double d = Math.sqrt(x * x + y * y + z * z);
							if (d <= core && d > carved - 0.5) {
								q().add(c.getX() + x, c.getY() + y, c.getZ() + z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
							}
						}
					}
				}
				carved = core;
			}
			// It tears the ground up and drinks it.
			if (t % 3 == 0 && drawn.size() < 160) {
				drawn.addAll(Kit.lift(world, orb.add(0, -ORB_H, 0), r * 1.3, 5, true, true));
			}
			for (FallingBlockEntity fb : drawn) {
				if (!fb.isAlive()) {
					continue;
				}
				Vec3d to = orb.subtract(fb.getPos());
				double d = to.length();
				if (d < core + 1) {
					world.spawnParticles(ParticleTypes.SQUID_INK, fb.getX(), fb.getY(), fb.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
					fb.discard();
					continue;
				}
				Vec3d swirl = to.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(0.25);
				Kit.velocity(fb, to.multiply(1 / d).multiply(0.35 + 12 / Math.max(d, 4)).add(swirl));
			}
			drawn.removeIf(fb -> !fb.isAlive());
			// The land around it rots into shadow.
			if (t % 4 == 0 && Destruction.enabled(world)) {
				for (int i = 0; i < 24; i++) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					double d = Math.sqrt(world.random.nextDouble()) * r * 1.6;
					BlockPos p = top(MathHelper.floor(orb.x + Math.cos(a) * d), MathHelper.floor(orb.z + Math.sin(a) * d));
					shadow(p, world.getBlockState(p));
				}
			}
			if (t % 10 == 0) {
				for (LivingEntity e : Kit.living(world, orb, r * 2.2)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false));
					if (e.getPos().distanceTo(orb) < r) {
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1, false, false));
					}
				}
				Kit.damage(world, orb, core + 2, 8.0F, ModDamageTypes.ANNIHILATION, null, owner());
			}
			Detonations.pull(world, orb, r * 2, 0.035);
		}

		private void shadow(BlockPos p, BlockState s) {
			if (s.isAir()) {
				return;
			}
			if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.REPLACEABLE_BY_TREES)) {
				q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
				return;
			}
			if (!s.getFluidState().isEmpty()) {
				q().add(p, Blocks.OBSIDIAN.getDefaultState(), BlockQueue.ANY);
				return;
			}
			double k = world.random.nextDouble();
			BlockState to = k < 0.4 ? Blocks.SCULK.getDefaultState() : k < 0.7 ? Blocks.BLACKSTONE.getDefaultState()
					: k < 0.88 ? Blocks.BLACK_CONCRETE.getDefaultState() : k < 0.96 ? Blocks.OBSIDIAN.getDefaultState() : Blocks.CRYING_OBSIDIAN.getDefaultState();
			q().add(p, to, BlockQueue.SOLID_ONLY);
		}

		private void exhale(Vec3d g, Vec3d orb) {
			Kit.vanish(drawn);
			Vfx.flash(world, orb, 0.5F, 600, 0x8030FF);
			Vfx.screen(world, g, 600, 4, 0.8F, 6);
			Vfx.impact(world, g, 1, 2);
			Kit.boom(world, orb, 11.0F, 0x5010B0, owner(), true);
			Vfx.ring(world, g.add(0, 1, 0), 260.0F, 10.0F, 0x300060, true, 70);
			Vfx.dome(world, orb, 120.0F, 0x6020C0, 40);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.5F);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.5F);
			Detonations.blast(world, orb, 110, 45.0F, ModDamageTypes.ANNIHILATION, owner(), 4.5, 1.6);
			for (LivingEntity e : Kit.living(world, g, 220)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 500, 0, false, false));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 30, 0, false, false));
				}
			}
			if (Destruction.enabled(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g.add(0, ORB_H * 0.5, 0)), Destruction.radius(world, 26.0F), CraterJob.Mode.ANTIMATTER));
				stain = new RingSweep(Destruction.radius(world, 110));
			}
		}

		/** The exhale's shadow wave: the nearer, the more completely the ground turns to shadow. */
		private void stainColumn(Vec3d g, int dx, int dz, double d, double R) {
			if (world.random.nextDouble() > 1.1 - d / R) {
				BlockPos p = top(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz);
				shadow(p, world.getBlockState(p));
			}
		}

		@Override
		public void finish() {
			Kit.vanish(drawn);
			super.finish();
		}
	}

	// ====================================================================== SPECTRAL ICE

	/**
	 * Ice spikes burst from the ground in a square and lean together into a pyramid with a single point. The point
	 * lights up blue, the spikes start to glow, vigorously, until the apex turns white and sprays a storm of smaller
	 * spikes in every direction; each one lands with a crack, a little crater and a fresh spike of ice.
	 */
	public static class SpectralIce extends Base {
		private Vec3d[] bases;
		private Vec3d[] tips;
		private final double[] done = new double[64];
		private Vec3d[] lands;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				announce("spectral_ice");
				bases = Ice.bases(g);
				tips = new Vec3d[bases.length];
				for (int i = 0; i < bases.length; i++) {
					tips[i] = Ice.tip(g, bases[i], i, bases.length);
				}
				Vfx.newFx(world, FX_ICE, g, g, (float) Ice.HALF, 0x60C0FF, 0, 0, Ice.END);
				Kit.sound(world, g, ModSounds.FREEZE, 48.0F, 0.5F);
				Vfx.skyTint(world, g, 600, 0x103060, 0.6F);
				frostFloor(g);
			}
			if (t >= Ice.ERUPT && t <= Ice.ERUPT_END + 80) {
				erupt(g, t);
			}
			if (t == Ice.APEX) {
				Vec3d apex = Ice.apex(g);
				Kit.sound(world, apex, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 1.2F);
				Kit.sound(world, apex, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.8F);
				for (int i = -1; i <= 1; i++) {
					TemporaryBlocks.place(world, BlockPos.ofFloored(apex.add(i * 2, 3, 0)), Blocks.LIGHT.getDefaultState(), Ice.END - Ice.APEX);
					TemporaryBlocks.place(world, BlockPos.ofFloored(apex.add(0, 3, i * 2)), Blocks.LIGHT.getDefaultState(), Ice.END - Ice.APEX);
				}
			}
			if (t >= Ice.GLOW && t < Ice.SPRAY && t % 10 == 0) {
				float k = (t - Ice.GLOW) / (float) (Ice.SPRAY - Ice.GLOW);
				Kit.sound(world, Ice.apex(g), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 64.0F, 0.6F + k * 1.2F);
				Vfx.shake(world, g, 120, 0.2F + k * 0.6F);
				for (LivingEntity e : Kit.living(world, g, 50)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.setFrozenTicks(Math.min(e.getFrozenTicks() + 30, 300));
					}
				}
			}
			if (t == Ice.SPRAY - 8) {
				Kit.sound(world, Ice.apex(g), ModSounds.CRYO_BLAST, 64.0F, 1.6F);
				Vfx.flash(world, Ice.apex(g), 0.6F, 300, 0xE0F8FF);
			}
			for (int i = 0; i < Ice.SHARDS; i++) {
				if (t == Ice.shardLaunch(i)) {
					Kit.sound(world, Ice.apex(g), SoundEvents.BLOCK_GLASS_BREAK, 32.0F, 1.2F + (i % 5) * 0.15F);
				}
				if (t == Ice.shardLaunch(i) + Ice.shardFlight(i)) {
					land(g, i);
				}
			}
			drain();
			return t >= Ice.END;
		}

		private void frostFloor(Vec3d g) {
			int R = (int) Ice.HALF + 8;
			for (int x = -R; x <= R; x++) {
				for (int z = -R; z <= R; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d > R || world.random.nextDouble() > 1.15 - d / R) {
						continue;
					}
					BlockPos p = top(MathHelper.floor(g.x) + x, MathHelper.floor(g.z) + z);
					BlockState s = world.getBlockState(p);
					q().add(p, !s.getFluidState().isEmpty() ? Blocks.ICE.getDefaultState()
							: world.random.nextInt(3) == 0 ? Blocks.PACKED_ICE.getDefaultState() : Blocks.SNOW_BLOCK.getDefaultState(), BlockQueue.ANY);
				}
			}
		}

		/** Grows every spike up its axis: a cone of spheres, packed-ice skin round a blue-ice heart. */
		private void erupt(Vec3d g, int t) {
			int n = bases.length;
			for (int i = 0; i < n; i++) {
				double len = tips[i].distanceTo(bases[i]);
				double to = Ice.grown(i, n, t) * len;
				if (to <= done[i]) {
					continue;
				}
				Vec3d dir = tips[i].subtract(bases[i]).normalize();
				double r0 = Ice.baseRadius(i, n);
				for (double s = Math.max(0, done[i] - 0.5); s <= to; s += 0.9) {
					Vec3d c = bases[i].add(dir.multiply(s - 3));
					double r = r0 * (1 - s / len) + 0.45;
					int R = MathHelper.ceil(r);
					for (int x = -R; x <= R; x++) {
						for (int y = -R; y <= R; y++) {
							for (int z = -R; z <= R; z++) {
								double d = Math.sqrt(x * x + y * y + z * z);
								if (d <= r) {
									BlockState st = d < r - 1.3 || s > len * 0.85 ? Blocks.BLUE_ICE.getDefaultState() : Blocks.PACKED_ICE.getDefaultState();
									q().add(MathHelper.floor(c.x) + x, MathHelper.floor(c.y) + y, MathHelper.floor(c.z) + z, st, BlockQueue.ANY);
								}
							}
						}
					}
				}
				if (done[i] == 0) {
					Kit.sound(world, bases[i], SoundEvents.BLOCK_GLASS_BREAK, 24.0F, 0.5F);
					Kit.sound(world, bases[i], ModSounds.FREEZE, 24.0F, 0.7F);
					world.spawnParticles(ParticleTypes.SNOWFLAKE, bases[i].x, bases[i].y + 1, bases[i].z, 60, 2, 1, 2, 0.2);
					for (LivingEntity e : Kit.living(world, bases[i], r0 + 3)) {
						if (!Kit.creativeOrSpectator(e)) {
							Kit.push(e, new Vec3d(0, 1.4, 0));
							e.damage(world.getDamageSources().freeze(), 6.0F);
						}
					}
				}
				done[i] = to;
			}
			if (t % 8 == 0 && t < Ice.ERUPT_END) {
				Kit.sound(world, g, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, 48.0F, 0.5F);
				Vfx.shake(world, g, 100, 0.5F);
			}
		}

		/** A shard comes down: a crack, a small crater, a frozen splash and a fresh little spike. */
		private void land(Vec3d g, int i) {
			Vec3d l = Ice.shardTarget(g, i);
			Vec3d p = Kit.ground(world, l.x, l.z);
			world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 1, p.z, 40, 1.5, 1, 1.5, 0.25);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState()), p.x, p.y + 1, p.z, 30, 1, 1, 1, 0.3);
			Kit.sound(world, p, SoundEvents.BLOCK_GLASS_BREAK, 16.0F, 0.6F);
			Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 10.0F, 1.3F);
			Kit.damage(world, p, 4.5, 9.0F, ModDamageTypes.CRYO, null, owner());
			if (!Destruction.enabled(world)) {
				return;
			}
			Destruction.explode(world, owner(), p.x, p.y, p.z, 2.6F, false);
			BlockPos b = BlockPos.ofFloored(p).down(2);
			int h = 4 + (int) (NewBombShapes.hash(i, 9) * 5);
			for (int y = 0; y < h; y++) {
				double r = 1.6 * (1 - y / (double) h) + 0.3;
				int R = MathHelper.ceil(r);
				for (int x = -R; x <= R; x++) {
					for (int z = -R; z <= R; z++) {
						if (x * x + z * z <= r * r) {
							q().add(b.getX() + x, b.getY() + y, b.getZ() + z, y > h - 3 ? Blocks.BLUE_ICE.getDefaultState() : Blocks.PACKED_ICE.getDefaultState(), BlockQueue.ANY);
						}
					}
				}
			}
			for (int x = -3; x <= 3; x++) {
				for (int z = -3; z <= 3; z++) {
					BlockPos s = top(b.getX() + x, b.getZ() + z);
					if (!world.getBlockState(s).getFluidState().isEmpty()) {
						q().add(s, Blocks.ICE.getDefaultState(), BlockQueue.ANY);
					}
				}
			}
		}
	}

	// ====================================================================== TERMITE DOMAIN

	/**
	 * Thousands of ants pour out of the earth and eat everything alive: grass, flowers, crops, leaves, whole trees,
	 * and whatever animals do not run. When the land is bare they turn home and raise a termite hill seventy blocks
	 * tall, buttressed, vented, ringed with spires. Then the guards come out.
	 */
	public static class TermiteDomain extends Base {
		private RingSweep eat, flight;
		private double builtH;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				announce("termite_domain");
				Vfx.newFx(world, FX_TERMITES, g, g, (float) Termite.RADIUS, 0x6A4020, 0, 0, Termite.END);
				Kit.sound(world, g, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 32.0F, 0.5F);
				eat = new RingSweep((int) Termite.RADIUS);
			}
			if (t >= Termite.EMERGE && t < Termite.EMERGE + 40 && t % 4 == 0) {
				Vfx.shake(world, g, 120, 0.4F);
				for (int i = 0; i < 6; i++) {
					Vec3d p = CityDestructionEvents.around(world, g, 20);
					Vfx.play(world, VfxIds.DIRT_SPURT, p, 1.0F);
					Kit.sound(world, p, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 8.0F, 0.6F);
				}
			}
			if (t == Termite.EMERGE + 60) {
				Vfx.vision(world, g, 7, 400, 0, 26, 0, 400, 0xFFFFFF, 110, Termite.RADIUS + 40);
			}
			if (t >= Termite.EMERGE && t <= Termite.EAT_END + 4) {
				double front = Termite.front(t);
				if (Destruction.enabled(world)) {
					eat.advance(front, 900, (dx, dz, d) -> eatColumn(g, dx, dz));
				}
				if (t % 5 == 0) {
					bite(g, front);
					// The swarm itself: dark specks skittering along the eating front.
					for (int i = 0; i < 2; i++) {
						double a = world.random.nextDouble() * MathHelper.TAU;
						Vfx.play(world, VfxIds.ANT_SKITTER, Kit.ground(world, g.x + Math.cos(a) * front, g.z + Math.sin(a) * front), 1.0F);
					}
				}
				if (t % 6 == 0) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					Kit.sound(world, g.add(Math.cos(a) * front, 0, Math.sin(a) * front), SoundEvents.ENTITY_SILVERFISH_AMBIENT, 24.0F, 0.6F + world.random.nextFloat() * 0.6F);
				}
			}
			if (t >= Termite.EMERGE + 40 && t < Termite.EAT_END && t % 14 == 0 && Destruction.enabled(world)) {
				sinkhole(g, Termite.front(t) * (0.3 + world.random.nextDouble() * 0.6));
			}
			if (t == Termite.BUILD) {
				// The mound bursts up out of the ground.
				Kit.boom(world, g.add(0, 2, 0), 9.0F, 0x7A5A38, owner(), true);
				Vfx.ring(world, g.add(0, 1, 0), 120.0F, 5.0F, 0x8A6A44, true, 50);
				Kit.lift(world, g, Termite.HILL_R, 60, true, true).forEach(fb -> Kit.velocity(fb,
						new Vec3d(fb.getX() - g.x, 0, fb.getZ() - g.z).normalize().multiply(0.8).add(0, 1.2 + world.random.nextDouble(), 0)));
			}
			if (t == Termite.FLIGHT) {
				Kit.sound(world, g, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_PHANTOM_FLAP, 64.0F, 0.4F);
				flight = new RingSweep((int) Termite.FLIGHT_R);
			}
			if (t >= Termite.FLIGHT && t <= Termite.FLIGHT_END + 4) {
				double front = Termite.flightFront(t);
				if (Destruction.enabled(world)) {
					flight.advance(front, 1200, (dx, dz, d) -> {
						if (d > Termite.RADIUS - 2) {
							eatColumn(g, dx, dz);
						}
					});
					if (t % 12 == 0) {
						sinkhole(g, front - 6);
					}
				}
				if (t % 5 == 0) {
					bite(g, front);
				}
				if (t % 8 == 0) {
					double a = world.random.nextDouble() * MathHelper.TAU;
					Kit.sound(world, g.add(Math.cos(a) * front, 6, Math.sin(a) * front), SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 32.0F, 0.6F);
				}
			}
			if (t >= Termite.BUILD && t <= Termite.BUILD_END) {
				build(g, Termite.built(t) * Termite.HILL_H);
				if (t % 7 == 0) {
					Kit.sound(world, g.add(0, builtH, 0), SoundEvents.BLOCK_MUD_PLACE, 32.0F, 0.6F + world.random.nextFloat() * 0.4F);
					Vfx.play(world, VfxIds.MUD_FLING, g.add(0, builtH * 0.6, 0), 1.0F);
				}
			}
			if (t == Termite.BUILD_END + 10) {
				guards(g);
			}
			collapse(g, t);
			drain();
			return t >= Termite.END && (cave == null || cave.isDone()) || t >= Termite.END + 900;
		}

		private com.armageddon.destruction.world.TerrainJob cave;

		/**
		 * The end of the domain: the colony has hollowed out everything under the town, and once the swarm is gone
		 * it gives way. The ground groans and splits in rings, the mound drops out of sight, and the whole heart of
		 * the domain caves in to a deep muddy pit.
		 */
		private void collapse(Vec3d g, int t) {
			if (t >= Termite.COLLAPSE && t < Termite.CAVE) {
				int k = t - Termite.COLLAPSE;
				if (k % 12 == 0) {
					Vfx.shake(world, g, 100000, 0.5F + k * 0.03F);
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
					Kit.sound(world, g, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 64.0F, 0.4F);
				}
				if (k % 20 == 0 && Destruction.enabled(world)) {
					Kit.ringCrack(world, g, 18 + k * 0.9, 6, null, 0.5F);
					Vfx.ring(world, g.add(0, 0.5, 0), (float) (18 + k * 0.9), 2.0F, 0x7A5A38, true, 30);
				}
			}
			if (t == Termite.CAVE) {
				CityDestructionEvents.frames(world, g, 0, 2);
				Kit.boom(world, g.add(0, 1, 0), 10.0F, 0x6A4A2A, owner(), false);
				Vfx.play(world, VfxIds.TERMITE_CAVE, g, 1.0F);
				Vfx.ring(world, g.add(0, 0.5, 0), (float) Termite.RADIUS * 1.6F, 5.0F, 0x8A6A44, true, 70);
				Vfx.shake(world, g, 100000, 2.4F);
				Vfx.fov(world, g, 14.0F, 30);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.BLOCK_MUD_BREAK, 64.0F, 0.3F);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.2F);
				if (Destruction.enabled(world)) {
					cave = com.armageddon.event.Climax.sinkhole(world, g, Destruction.radius(world, (float) (Termite.RADIUS * 0.72)), MathHelper.floor(g.y) - 1);
				}
			}
			if (cave != null && !cave.isDone()) {
				int k = t - Termite.CAVE;
				cave.limit = 4 + k * 1.8;
				cave.step(3500);
				if (k % 6 == 0) {
					com.armageddon.event.Climax.groundWave(world, g, cave.limit + 4, 3, 14, 0.35);
				}
				if (k % 10 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 48.0F, 0.5F);
				}
			}
		}

		/** The swarm strips a column: everything alive goes, the ground turns to bare, chewed dirt. */
		private void eatColumn(Vec3d g, int dx, int dz) {
			int x = MathHelper.floor(g.x) + dx, z = MathHelper.floor(g.z) + dz;
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			BlockPos.Mutable p = new BlockPos.Mutable(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z);
			for (int i = 0; i < 40; i++) {
				p.move(0, -1, 0);
				BlockState s = world.getBlockState(p);
				if (s.isAir()) {
					continue;
				}
				if (living(s) || edible(s)) {
					q().add(p.toImmutable(), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					continue;
				}
				if (!com.armageddon.destruction.world.Devastation.natural(s)) {
					// Stone walls and the like: they stand, hollowed out, while the wood inside is eaten.
					continue;
				}
				if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.PODZOL) || s.isOf(Blocks.MYCELIUM) || s.isOf(Blocks.MOSS_BLOCK) || s.isOf(Blocks.FARMLAND)) {
					double k = NewBombShapes.hash(x, z);
					q().add(p.toImmutable(), k < 0.5 ? Blocks.COARSE_DIRT.getDefaultState() : k < 0.85 ? Blocks.DIRT.getDefaultState() : Blocks.ROOTED_DIRT.getDefaultState(), BlockQueue.ANY);
				}
				break;
			}
		}

		/** Wood and fibre: everything a termite eats. Houses are eaten down to their stone shells. */
		static boolean edible(BlockState s) {
			return s.isIn(BlockTags.PLANKS) || s.isIn(BlockTags.WOODEN_STAIRS) || s.isIn(BlockTags.WOODEN_SLABS) || s.isIn(BlockTags.WOODEN_FENCES)
					|| s.isIn(BlockTags.FENCE_GATES) || s.isIn(BlockTags.WOODEN_DOORS) || s.isIn(BlockTags.WOODEN_TRAPDOORS) || s.isIn(BlockTags.WOOL)
					|| s.isIn(BlockTags.WOOL_CARPETS) || s.isIn(BlockTags.WOODEN_BUTTONS) || s.isIn(BlockTags.WOODEN_PRESSURE_PLATES)
					|| s.isOf(Blocks.LADDER) || s.isOf(Blocks.SCAFFOLDING) || s.isOf(Blocks.BOOKSHELF) || s.isOf(Blocks.CRAFTING_TABLE)
					|| s.isOf(Blocks.COMPOSTER) || s.isOf(Blocks.TARGET) || s.isOf(Blocks.BEEHIVE) || s.isOf(Blocks.BEE_NEST) || s.isOf(Blocks.TORCH)
					|| s.isOf(Blocks.WALL_TORCH) || s.isIn(BlockTags.LOGS);
		}

		/** The tunnels under the land give way: a pit opens and swallows what stands over it. */
		private void sinkhole(Vec3d g, double dist) {
			double a = world.random.nextDouble() * MathHelper.TAU;
			int cx = MathHelper.floor(g.x + Math.cos(a) * dist), cz = MathHelper.floor(g.z + Math.sin(a) * dist);
			if (!world.isChunkLoaded(cx >> 4, cz >> 4)) {
				return;
			}
			int r = 3 + world.random.nextInt(5), depth = 7 + world.random.nextInt(8);
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, cx, cz);
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d > r) {
						continue;
					}
					int floor = top - 1 - (int) (depth * (1 - d / r));
					for (int y = top + 14; y > floor; y--) {
						q().add(cx + x, y, cz + z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
					q().add(cx + x, floor, cz + z, (x + z) % 3 == 0 ? Blocks.ROOTED_DIRT.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState(), BlockQueue.ANY);
				}
			}
			Vec3d c = new Vec3d(cx + 0.5, top, cz + 0.5);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()), c.x, c.y, c.z, 80, r * 0.6, 1.5, r * 0.6, 0.2);
			Kit.sound(world, c, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 32.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 16.0F, 0.5F);
			Vfx.shake(world, c, 60, 0.8F);
			for (LivingEntity e : Kit.living(world, c, r + 1)) {
				Kit.push(e, new Vec3d(0, -0.6, 0));
			}
		}

		private void bite(Vec3d g, double front) {
			for (Entity e : Kit.entities(world, g, front + 3, en -> true)) {
				double d = Math.sqrt(e.squaredDistanceTo(g.x, e.getY(), g.z));
				if (d < front - 10) {
					continue;
				}
				if (e instanceof ItemEntity item) {
					item.discard();
				} else if (e instanceof LivingEntity le && !(e instanceof SilverfishEntity) && !Kit.creativeOrSpectator(le)) {
					le.damage(world.getDamageSources().magic(), le instanceof PlayerEntity ? 1.0F : 3.0F);
					if (le instanceof PlayerEntity) {
						le.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 0, false, true));
					}
					world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.getDefaultState()), le.getX(), le.getY() + 0.4, le.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
				}
			}
		}

		/** Raises the hill to height {@code h}: a buttressed shell with vents and four tunnel mouths, and its spires. */
		private void build(Vec3d g, double h) {
			if (!Destruction.enabled(world)) {
				builtH = h;
				return;
			}
			int gx = MathHelper.floor(g.x), gy = MathHelper.floor(g.y) - 1, gz = MathHelper.floor(g.z);
			for (int y = (int) Math.floor(builtH); y < h; y++) {
				int R = (int) Math.ceil(Termite.HILL_R * 1.4) + 2;
				for (int x = -R; x <= R; x++) {
					for (int z = -R; z <= R; z++) {
						double a = Math.atan2(z, x);
						double r = Termite.radius(y, a);
						double d = Math.sqrt(x * x + z * z);
						if (d > r || d < r - 3.5) {
							continue;
						}
						// Tunnel mouths at the four quarters, and vent holes in the skin.
						double quarter = Math.abs(MathHelper.wrapDegrees(Math.toDegrees(a) + 45) % 90);
						if (y < 5 && Math.min(quarter, 90 - quarter) < 6) {
							continue;
						}
						if (d > r - 1 && NewBombShapes.hash(x * 31 + y, z * 17 - y) < 0.025) {
							continue;
						}
						q().add(gx + x, gy + y, gz + z, mud(x, y, z), BlockQueue.ANY);
					}
				}
				for (int i = 0; i < Termite.SPIRES; i++) {
					double[] s = Termite.spire(i);
					double sh = s[3];
					if (y > sh) {
						continue;
					}
					double sr = s[2] * Math.pow(1 - y / sh, 0.9) + 0.6;
					int SR = MathHelper.ceil(sr);
					for (int x = -SR; x <= SR; x++) {
						for (int z = -SR; z <= SR; z++) {
							if (x * x + z * z <= sr * sr) {
								q().add(gx + (int) s[0] + x, gy + y, gz + (int) s[1] + z, mud(x + 7 * i, y, z), BlockQueue.AIR_ONLY);
							}
						}
					}
				}
			}
			builtH = h;
		}

		private static BlockState mud(int x, int y, int z) {
			double k = NewBombShapes.hash(x * 7 + y * 3, z * 11 - y);
			int band = (y / 3) % 4;
			if (band == 0 && k < 0.7) {
				return Blocks.MUD_BRICKS.getDefaultState();
			}
			return k < 0.35 ? Blocks.PACKED_MUD.getDefaultState() : k < 0.6 ? Blocks.COARSE_DIRT.getDefaultState()
					: k < 0.8 ? Blocks.BROWN_TERRACOTTA.getDefaultState() : k < 0.93 ? Blocks.DIRT.getDefaultState() : Blocks.MUD.getDefaultState();
		}

		private void guards(Vec3d g) {
			Kit.sound(world, g, SoundEvents.ENTITY_SILVERFISH_AMBIENT, 64.0F, 0.3F);
			for (int i = 0; i < 12; i++) {
				SilverfishEntity s = EntityType.SILVERFISH.create(world);
				if (s == null) {
					continue;
				}
				double a = i * MathHelper.TAU / 12;
				Vec3d p = Kit.ground(world, g.x + Math.cos(a) * (Termite.HILL_R + 4), g.z + Math.sin(a) * (Termite.HILL_R + 4));
				s.refreshPositionAndAngles(p.x, p.y, p.z, world.random.nextFloat() * 360, 0);
				s.setCustomName(Text.literal("Termite"));
				s.setPersistent();
				world.spawnEntity(s);
			}
		}
	}

	// ====================================================================== THE SUN DISAPPEARS

	/**
	 * The strongest bomb yet. A beam climbs to the sun at noon, the sun gutters and goes out, and the whole world
	 * feels it: night that will not end, a sky with no stars, fog that swallows the horizon, frost on every lens,
	 * water freezing, plants dying, and everyone not standing by a fire slowly freezing. Then the sun's dead corpse
	 * falls out of the black sky and hits the ground. In the blast, the light comes back.
	 */
	public static class SunDisappears extends Base {
		private long day;
		private Vec3d heading;
		private RingSweep cold;
		private com.armageddon.destruction.world.TerrainJob devastation;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				announce("sun_disappears");
				heading = Kit.dir(data.getFloat("Yaw"));
				long now = world.getTimeOfDay();
				day = now - Math.floorMod(now, 24000L);
				world.setTimeOfDay(day + 6000);
				Vfx.newFx(world, FX_SUN, g, heading, (float) Sun.CORPSE_R, 0xFFE0A0, 0, 0, Sun.END);
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
				cold = new RingSweep(Destruction.radius(world, (int) Sun.COLD_R));
			}
			// The clock: high noon while the beam climbs, then a night that will not end.
			if (t < Sun.OUT) {
				world.setTimeOfDay(day + 6000);
			} else if (t < Sun.IMPACT + 40) {
				world.setTimeOfDay(day + 18000);
			}
			if (t == Sun.BEAM) {
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.6F);
				Vfx.light(world, g.add(0, 20, 0), 1.0F, 0xFFE0A0);
			}
			if (t == Sun.HIT) {
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.4F);
				Vfx.flash(world, g, 0.5F, 100000, 0xFFF0C0);
				everyone(Text.literal("The sun flickers.").formatted(Formatting.GOLD, Formatting.ITALIC));
			}
			if (t > Sun.DIM && t < Sun.OUT && t % 30 == 0) {
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
				Vfx.shake(world, g, 100000, 0.4F);
			}
			if (t == Sun.OUT) {
				out(g);
			}
			if (t >= Sun.COLD && t < Sun.IMPACT) {
				freeze(g, t);
			}
			if (t == Sun.FALL) {
				everyone(Text.literal("Something is falling out of the dark.").formatted(Formatting.DARK_RED, Formatting.ITALIC));
				Kit.sound(world, g, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.3F);
			}
			if (t > Sun.FALL && t < Sun.IMPACT && t % 40 == 0) {
				float k = (t - Sun.FALL) / (float) (Sun.IMPACT - Sun.FALL);
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F + k * 0.4F);
				Vfx.shake(world, g, 100000, 0.4F + k * 2.0F);
			}
			if (t == Sun.IMPACT - 20) {
				Vfx.cutscene(world, g, 80);
			}
			if (t == Sun.IMPACT) {
				impact(g);
			}
			if (t == Sun.IMPACT + 60) {
				dawn(g);
			}
			if (devastation != null && t > Sun.IMPACT) {
				// The corpse's blast wave: everything standing for four hundred blocks is torn down and set burning.
				int k = t - Sun.IMPACT;
				devastation.limit = 7.0 * k;
				devastation.step(4000);
				if (devastation.limit < 420 && k % 2 == 0) {
					WorldCutsceneEvents.shockFront(world, g, devastation.limit, 12, 60.0F, owner());
				}
			}
			drain();
			return t >= Sun.END && (devastation == null || devastation.isDone()) || t >= Sun.END + 1200;
		}

		private void everyone(Text msg) {
			for (ServerPlayerEntity p : world.getPlayers()) {
				p.sendMessage(msg, false);
			}
		}

		private void out(Vec3d g) {
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.3F);
			Kit.sound(world, g, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.3F);
			for (ServerPlayerEntity p : world.getPlayers()) {
				p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
						Text.literal("THE SUN IS GONE").formatted(Formatting.DARK_GRAY, Formatting.BOLD)));
				p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
						Text.literal("stay near a fire").formatted(Formatting.GRAY, Formatting.ITALIC)));
			}
			// Everyone in the world: the eclipse grade, with frost that thickens as the cold goes on.
			Vfx.vision(world, g, 3, 100000, 0, 0.022F, 0, 0, 0xFF4D00, Sun.IMPACT - Sun.OUT + 30, 100000);
			Vfx.skyTint(world, g, 100000, 0x000004, 1.0F);
			world.setWeather(0, Sun.END, true, false);
		}

		/** The cold spreads: water freezes, lava sets, plants die, snow settles, and everything without a fire freezes. */
		private void freeze(Vec3d g, int t) {
			if (t == Sun.COLD + 330) {
				Vfx.vision(world, g, 3, 100000, 0, 0.03F, 0, 1, 0xFFC000, Sun.IMPACT - t + 30, 100000);
			}
			double front = (t - Sun.COLD) / (double) (Sun.FALL - Sun.COLD) * Sun.COLD_R;
			if (Destruction.enabled(world)) {
				cold.advance(front, 700, (dx, dz, d) -> chill(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz));
			}
			if (t % 20 == 0) {
				Vfx.skyTint(world, g, 100000, 0x000004, 1.0F);
				for (ServerPlayerEntity p : world.getPlayers()) {
					if (p.isCreative() || p.isSpectator()) {
						continue;
					}
					BlockPos at = p.getBlockPos();
					boolean warm = world.getLightLevel(LightType.BLOCK, at) >= 12 || world.getLightLevel(LightType.BLOCK, at.up()) >= 12;
					if (!warm) {
						p.setFrozenTicks(Math.min(p.getFrozenTicks() + 70, 260));
					}
					world.spawnParticles(p, ParticleTypes.SNOWFLAKE, true, p.getX(), p.getY() + 3, p.getZ(), 20, 8, 3, 8, 0.02);
				}
				for (LivingEntity e : Kit.living(world, g, Math.max(16, front))) {
					if (!(e instanceof PlayerEntity)) {
						e.setFrozenTicks(Math.min(e.getFrozenTicks() + 70, 260));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false));
					}
				}
			}
			if (t % 160 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.3F);
				Vfx.shake(world, g, 100000, 0.6F);
			}
		}

		private void chill(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			BlockPos.Mutable p = new BlockPos.Mutable(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), z);
			for (int i = 0; i < 30; i++) {
				p.move(0, -1, 0);
				BlockState s = world.getBlockState(p);
				if (s.isAir()) {
					continue;
				}
				if (s.isOf(Blocks.WATER)) {
					q().add(p.toImmutable(), Blocks.ICE.getDefaultState(), BlockQueue.ANY);
					break;
				}
				if (s.isOf(Blocks.LAVA)) {
					q().add(p.toImmutable(), Blocks.OBSIDIAN.getDefaultState(), BlockQueue.ANY);
					break;
				}
				if (s.isIn(BlockTags.FIRE)) {
					q().add(p.toImmutable(), Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					continue;
				}
				if (s.isIn(BlockTags.LEAVES)) {
					q().add(p.toImmutable(), (x + z) % 7 == 0 ? Blocks.SNOW_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					continue;
				}
				if (s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.CROPS) || s.isIn(BlockTags.REPLACEABLE_BY_TREES) || s.isIn(BlockTags.SAPLINGS)) {
					q().add(p.toImmutable(), NewBombShapes.hash(x, z) < 0.25 ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					continue;
				}
				if (s.isIn(BlockTags.LOGS)) {
					continue;
				}
				if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.DIRT) || s.isOf(Blocks.PODZOL) || s.isOf(Blocks.FARMLAND)) {
					q().add(p.toImmutable(), Blocks.SNOW_BLOCK.getDefaultState(), BlockQueue.ANY);
					break;
				}
				if (s.isOpaqueFullCube(world, p)) {
					int layers = 1 + (int) (NewBombShapes.hash(x * 3, z * 5) * 3);
					q().add(p.up(), Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers), BlockQueue.AIR_ONLY);
				}
				break;
			}
		}

		/** The corpse of the sun hits: the biggest blast in the arsenal, and the light comes back with it. */
		private void impact(Vec3d g) {
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
			Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.3F);
			Vfx.flash(world, g, 1.0F, 100000, 0xFFF4E0);
			Vfx.screen(world, g, 100000, 1, 1.0F, 30);
			Vfx.impact(world, g, 3, 3);
			Vfx.shake(world, g, 100000, 4.0F);
			Vfx.fov(world, g, 22.0F, 30);
			Vfx.layeredBlast(world, g.add(0, 6, 0), 24.0F, 0xFF7030);
			Vfx.ring(world, g.add(0, 1, 0), 700.0F, 18.0F, 0xFFB060, true, 120);
			Vfx.ring(world, g.add(0, 1, 0), 420.0F, 10.0F, 0xFFFFFF, true, 80);
			Vfx.dome(world, g, 320.0F, 0xFF9040, 70);
			Vfx.pillar(world, g, 40.0F, 0xFFE0A0, false, 160);
			Detonations.blast(world, g, 260, 90.0F, ModDamageTypes.ANNIHILATION, owner(), 7.0, 2.4);
			Detonations.ignite(world, g, 90, 0.08F);
			if (Destruction.enabled(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(g), Destruction.radius(world, 70.0F), CraterJob.Mode.NUKE));
				devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, Destruction.radius(world, 420.0F), MathHelper.floor(g.y) - 1,
						com.armageddon.destruction.world.Devastation.FIRE);
				devastation.limit = 0;
			}
			for (int i = 0; i < 14; i++) {
				final int k = i;
				DetonationManager.schedule(world, 6 + i * 4, () -> {
					double a = k * MathHelper.TAU / 14 + 0.3;
					double d = 95 + (k % 3) * 22;
					Vec3d p = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					Kit.boom(world, p, 7.0F, 0xFF8040, owner(), true);
				});
			}
		}

		private void dawn(Vec3d g) {
			world.setTimeOfDay(day + 24000 + 600);
			world.setWeather(6000, 0, false, false);
			Vfx.vision(world, g, 3, 100000, 0, 0.004F, 0, 1, 0x002000, 30, 100000);
			Vfx.skyTint(world, g, 100000, 0xFFD090, 0.5F);
			for (ServerPlayerEntity p : world.getPlayers()) {
				p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
						Text.literal("DAWN").formatted(Formatting.GOLD, Formatting.BOLD)));
				p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
						Text.literal("the sun is reborn").formatted(Formatting.YELLOW, Formatting.ITALIC)));
				p.setFrozenTicks(0);
			}
		}
	}

	static List<Vec3d> list(Vec3d... v) {
		return new ArrayList<>(Arrays.asList(v));
	}
}
