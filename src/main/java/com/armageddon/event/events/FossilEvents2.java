package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.event.Kit;
import com.armageddon.event.events.FossilEvents.Bones;
import com.armageddon.network.Vfx;
import com.armageddon.util.FossilPoses;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.FossilShapes.Amber;
import com.armageddon.util.FossilShapes.Ammonite;
import com.armageddon.util.FossilShapes.Sabre;
import com.armageddon.util.PteroPose.Seg;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Three more fossils: the AMBER (a giant dragonfly sealed in a drop of amber, freed, then sealed again), the
 * SABRE-TOOTH (a sabre-toothed cat's hunt at dusk) and the AMMONITE (a coiled shell that presses the land into its
 * own imprint).
 */
public final class FossilEvents2 {
	private FossilEvents2() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: FossilFx2). */
	public static final int FX_AMBER = 65, FX_SABRE = 66, FX_AMMONITE = 67;

	/** A point in the heading's frame relative to the bomb's ground, in the world. */
	static Vec3d at(Vec3d base, Vec3d heading, Vec3d local) {
		return base.add(FossilShapes.frame(heading, local));
	}

	/** The heading's-frame coordinates (forward, right) of a world column. */
	static double[] local(Vec3d base, Vec3d heading, double x, double z) {
		double dx = x - base.x, dz = z - base.z;
		return new double[]{dx * heading.x + dz * heading.z, -dx * heading.z + dz * heading.x};
	}

	static boolean earth(BlockState g) {
		return g.isIn(BlockTags.DIRT) || g.isOf(Blocks.GRASS_BLOCK) || g.isIn(BlockTags.SAND) || g.isOf(Blocks.GRAVEL)
				|| g.isIn(BlockTags.BASE_STONE_OVERWORLD) || g.isOf(Blocks.CLAY) || g.isIn(BlockTags.TERRACOTTA) || g.isOf(Blocks.SNOW_BLOCK);
	}

	/**
	 * The real ground under a column: its surface counts tree trunks as ground, so this looks down through logs,
	 * leaves, plants and snow to the first block of earth, rock or water (at most 48 down).
	 */
	static int groundY(TerrainJob.Column c) {
		int y = c.top();
		for (int i = 0; i < 48 && y > c.bottomY(); i++, y--) {
			BlockState s = c.get(y);
			if (!(s.isAir() || s.isIn(BlockTags.LOGS) || s.isIn(BlockTags.LEAVES) || plant(s) || s.isOf(Blocks.VINE) || s.isOf(Blocks.BEE_NEST))) {
				return y;
			}
		}
		return c.surface();
	}

	static boolean plant(BlockState s) {
		return s.isReplaceable() || s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.SAPLINGS) || s.isOf(Blocks.SNOW);
	}

	// ====================================================================== AMBER

	/**
	 * AMBER FOSSIL. A drop of amber the size of a house wells up out of the ground, a giant dragonfly caught inside it.
	 * It glows; resin floods out over the land, glazing the ground with honey and turning the trees to amber glass. The
	 * amber cracks and shatters, and the dragonfly comes alive: it beats free, circles with a swarm of glowing insects
	 * and makes three strafing runs low over the land. Then it comes back, and resin wells up round it again: it is
	 * left sealed in a drop of amber over the bomb, and everything caught in the resin is encased in amber with it.
	 */
	public static class AmberFossil extends OddBombEvents.Base {
		private Vec3d heading, base;
		private Bones shell = new Bones(), seal = new Bones();
		private int placed, sealed;
		private TerrainJob resin, tide;
		private final java.util.Set<Long> spared = new java.util.HashSet<>();

		private static BlockState mat(int k) {
			return (switch (k) {
				case 1 -> Blocks.YELLOW_STAINED_GLASS;
				case 2 -> Blocks.BROWN_TERRACOTTA;
				case 3 -> Blocks.CYAN_TERRACOTTA;
				case 4 -> Blocks.LIGHT_BLUE_TERRACOTTA;
				case 5 -> Blocks.WHITE_STAINED_GLASS;
				case 6 -> Blocks.HONEYCOMB_BLOCK;
				default -> Blocks.ORANGE_STAINED_GLASS;
			}).getDefaultState();
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("amber_fossil", Formatting.GOLD);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				shell = drop(base, heading, Amber.CY, false);
				Vfx.newFx(world, FX_AMBER, base, heading, 1.0F, 0xFFA020, 0, 0, Amber.END);
				Vfx.play(world, VfxIds.AMBER_HEAVE, base, 1.0F);
				Kit.ringCrack(world, base, 9, 2, Blocks.HONEY_BLOCK.getDefaultState(), 0.6F);
				Kit.sound(world, base, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, 48.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 32.0F, 0.5F);
				Vfx.shake(world, base, 100, 0.8F);
				floor();
			}
			if (t >= Amber.RISE && t <= Amber.RISEN) {
				int to = (int) ((t - Amber.RISE) / (double) (Amber.RISEN - Amber.RISE) * shell.list.size());
				for (; placed < Math.min(to, shell.list.size()); placed++) {
					int[] b = shell.list.get(placed);
					q().add(b[0], b[1], b[2], mat(b[3]), BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 32.0F, 0.5F);
					Kit.sound(world, base, SoundEvents.BLOCK_GLASS_PLACE, 32.0F, 0.6F);
					Vfx.play(world, VfxIds.AMBER_HEAVE, base, 0.6F);
				}
			}
			if (t == Amber.RISEN) {
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.5F);
			}
			if (t >= Amber.CRACK && t < Amber.END - 60 && t % 40 == 0) {
				Vfx.skyTint(world, base, Amber.R + 100, 0xFFA040, 0.45F);
			}
			if (t >= Amber.CRACK && t < Amber.SHATTER && t % 14 == 0) {
				float k = (t - Amber.CRACK) / (float) (Amber.SHATTER - Amber.CRACK);
				Kit.sound(world, base.add(0, Amber.CY, 0), SoundEvents.BLOCK_GLASS_BREAK, 24.0F + 40.0F * k, 0.4F + 0.3F * k);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 32.0F, 0.5F);
				Vfx.shake(world, base, 80, 0.3F + 0.5F * k);
			}
			if (t == Amber.OOZE) {
				Kit.sound(world, base, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 96.0F, 0.4F);
				if (Destruction.enabled(world)) {
					resin = TerrainJob.disc(world, base, Amber.R, this::resin);
					resin.limit = 0;
				}
			}
			if (resin != null) {
				resin.limit = t >= Amber.OOZE_END ? Amber.R + 1 : Amber.front(t);
				resin.step(3000);
			}
			if (t >= Amber.OOZE && t <= Amber.OOZE_END) {
				if (t % 10 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 64.0F, 0.5F);
				}
				if (t % 10 == 5) {
					for (LivingEntity e : Kit.living(world, base, Amber.front(t))) {
						if (!Kit.creativeOrSpectator(e) && e.isOnGround()) {
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3));
						}
					}
				}
			}
			if (t == Amber.SHATTER) {
				shatter();
			}
			if (t == Amber.SHATTER + 14) {
				shards();
			}
			if (t > Amber.SHATTER && t < Amber.ENTOMB) {
				Vec3d p = at(base, heading, Amber.pos(t));
				if (t % 12 == 0) {
					Kit.sound(world, p, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 48.0F, 0.4F);
					Kit.sound(world, p, SoundEvents.ENTITY_PHANTOM_FLAP, 48.0F, 1.6F);
				}
				for (int k = 0; k < Amber.ZIPS.length; k++) {
					if (t == Amber.ZIPS[k]) {
						Kit.sound(world, p, SoundEvents.ENTITY_PHANTOM_SWOOP, 96.0F, 0.6F);
						Kit.sound(world, p, SoundEvents.ITEM_ELYTRA_FLYING, 96.0F, 0.8F);
					}
				}
				if (Amber.low(t)) {
					strafe(p);
				}
			}
			if (t == Amber.ENTOMB) {
				entomb();
			}
			if (t > Amber.ENTOMB && sealed < seal.list.size()) {
				int to = (int) ((t - Amber.ENTOMB) / 60.0 * seal.list.size());
				for (; sealed < Math.min(to, seal.list.size()); sealed++) {
					int[] b = seal.list.get(sealed);
					q().add(b[0], b[1], b[2], mat(b[3]), b[3] == 0 || b[3] == 1 ? BlockQueue.AIR_ONLY : BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Kit.sound(world, base.add(0, Amber.SEAL_Y, 0), SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 48.0F, 0.6F);
				}
			}
			if (t == Amber.TIDE) {
				Kit.sound(world, base, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 128.0F, 0.3F);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 128.0F, 0.3F);
				if (Destruction.enabled(world)) {
					// Whoever is standing in it keeps a pocket of air.
					for (LivingEntity e : Kit.living(world, base, Amber.R + 2)) {
						BlockPos f = e.getBlockPos();
						for (int x = -1; x <= 1; x++) {
							for (int z = -1; z <= 1; z++) {
								for (int y = -1; y <= 2 + (int) e.getHeight(); y++) {
									spared.add(f.add(x, y, z).asLong());
								}
							}
						}
					}
					tide = TerrainJob.disc(world, base, Amber.R, this::tide);
					tide.limit = 0;
				}
			}
			if (tide != null) {
				tide.limit = t >= Amber.TIDE_END ? Amber.R + 1 : Amber.tide(t);
				tide.step(3000);
			}
			if (t > Amber.TIDE && t < Amber.TIDE_END && t % 8 == 0) {
				Vec3d edge = base.add(heading.multiply(Amber.tide(t)));
				Kit.sound(world, edge, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 96.0F, 0.4F);
				Kit.sound(world, edge, SoundEvents.BLOCK_GLASS_PLACE, 96.0F, 0.5F);
			}
			drain();
			return t >= Amber.END && (tide == null || tide.isDone());
		}

		/** The amber tide reaching a column: everything up to {@value Amber#TIDE_H} over the ground set in amber. */
		private void tide(TerrainJob.Column c) {
			double d = Math.hypot(c.x + 0.5 - base.x, c.z + 0.5 - base.z);
			if (d < 10) {
				return;
			}
			int g = groundY(c);
			for (int y = g + 1; y <= g + Amber.TIDE_H; y++) {
				if (spared.contains(BlockPos.asLong(c.x, y, c.z))) {
					continue;
				}
				BlockState s = c.get(y);
				if (s.isAir() || plant(s) || s.isIn(BlockTags.LEAVES)) {
					c.set(y, (y == g + Amber.TIDE_H && c.noise(y + 3) < 0.25 ? Blocks.YELLOW_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS).getDefaultState());
				}
			}
		}

		/** Shards of the burst drop speared into the ground all round, leaning out, yellow at the tips. */
		private void shards() {
			Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, 96.0F, 0.6F);
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int i = 0; i < Amber.SHARDS; i++) {
				double[] sh = Amber.shard(i);
				Vec3d foot = at(base, heading, new Vec3d(sh[0], 0, sh[1]));
				foot = Kit.ground(world, foot.x, foot.z).add(0, -1, 0);
				Vec3d out = FossilShapes.frame(heading, Math.cos(sh[2]), 0, Math.sin(sh[2]));
				Vec3d tip = foot.add(out.multiply(sh[3] * 0.45)).add(0, sh[3], 0);
				Bones b = new Bones();
				b.line(foot, foot.lerp(tip, 0.7), 1.0, 0.6, 0);
				b.line(foot.lerp(tip, 0.7), tip, 0.6, 0.45, 1);
				b.dedupe();
				for (int[] v : b.list) {
					q().add(v[0], v[1], v[2], mat(v[3]), BlockQueue.ANY);
				}
			}
		}

		/** The drop's skin: amber glass with yellow veins, rising from the ground up. */
		static Bones drop(Vec3d base, Vec3d heading, double cy, boolean thick) {
			Bones b = new Bones();
			int rx = MathHelper.ceil(Amber.RX) + 1, ry = MathHelper.ceil(Amber.RY) + 1;
			Vec3d c = base.add(0, cy, 0);
			double skin = (thick ? 1.6 : 1.2) / Amber.RZ;
			for (int x = -rx; x <= rx; x++) {
				for (int z = -rx; z <= rx; z++) {
					for (int y = -ry; y <= ry; y++) {
						double wy = c.y + y;
						if (wy < base.y) {
							continue;
						}
						double[] l = local(Vec3d.ZERO, heading, x, z);
						double d = Amber.drop(l[0], y, l[1]);
						if (d <= 1 && d > 1 - skin) {
							int vein = FossilShapes.hash(x * 7 + y, z * 3 - y) < 0.12 ? 1 : 0;
							b.dot(new Vec3d(MathHelper.floor(c.x) + x + 0.5, wy, MathHelper.floor(c.z) + z + 0.5), vein);
						}
					}
				}
			}
			b.sortByHeight();
			return b;
		}

		/** Honey under the drop, so its floor reads as amber too. */
		private void floor() {
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int x = -9; x <= 9; x++) {
				for (int z = -9; z <= 9; z++) {
					if (x * x + z * z <= 81) {
						BlockPos top = surface(MathHelper.floor(base.x) + x, MathHelper.floor(base.z) + z);
						q().add(top, Blocks.HONEY_BLOCK.getDefaultState(), BlockQueue.SOLID_ONLY);
					}
				}
			}
		}

		/** Resin reaching a column: trees turn to amber glass, plants are swallowed, the ground is glazed with honey. */
		private void resin(TerrainJob.Column c) {
			double d = Math.hypot(c.x + 0.5 - base.x, c.z + 0.5 - base.z);
			if (d < 9) {
				return;
			}
			int surf = groundY(c);
			for (int y = Math.min(c.top(), surf + 28); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES)) {
					c.set(y, (c.noise(y) < 0.72 ? Blocks.ORANGE_STAINED_GLASS : Blocks.YELLOW_STAINED_GLASS).getDefaultState());
				} else if (plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (!g.getFluidState().isEmpty()) {
				if (g.getFluidState().isIn(FluidTags.WATER)) {
					c.set(surf, Blocks.ORANGE_STAINED_GLASS.getDefaultState());
				}
				return;
			}
			if (!earth(g)) {
				return;
			}
			double n = c.noise(51);
			Block glaze = n < 0.42 ? Blocks.HONEY_BLOCK : n < 0.7 ? Blocks.HONEYCOMB_BLOCK : n < 0.86 ? Blocks.ORANGE_TERRACOTTA : Blocks.YELLOW_TERRACOTTA;
			c.set(surf, glaze.getDefaultState());
			// Now and then a bead of resin standing on the ground.
			double m = c.noise(53);
			if (m < 0.012 && c.get(surf + 1).isAir()) {
				c.set(surf + 1, Blocks.HONEY_BLOCK.getDefaultState());
				if (m < 0.004) {
					c.set(surf + 2, Blocks.ORANGE_STAINED_GLASS.getDefaultState());
				}
			}
		}

		/** The amber bursts: its skin is gone in a storm of shards, everyone near thrown back. */
		private void shatter() {
			Vec3d c = base.add(0, Amber.CY, 0);
			Vfx.play(world, VfxIds.AMBER_SHATTER, c, 1.0F);
			Vfx.flash(world, c, 0.4F, 300, 0xFFD080);
			Vfx.shake(world, c, 200, 1.4F);
			Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 128.0F, 0.5F);
			Kit.sound(world, c, SoundEvents.BLOCK_GLASS_BREAK, 128.0F, 0.8F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.6F);
			Kit.sound(world, c, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, 96.0F, 0.5F);
			for (int[] b : shell.list) {
				q().add(b[0], b[1], b[2], Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
			}
			for (LivingEntity e : Kit.living(world, c, 22)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(c);
				double dist = Math.max(1, to.length());
				double k = 1 - dist / 22;
				Kit.push(e, to.multiply(1 / dist).multiply(1.4 * k).add(0, 0.6 * k, 0));
				e.damage(world.getDamageSources().generic(), (float) (10 * k));
			}
		}

		/** A strafing run skimming the ground: everything under it is cut and thrown aside by the wind of its wings. */
		private void strafe(Vec3d p) {
			Vec3d g = Kit.ground(world, p.x, p.z);
			if ((age() & 1) == 0) {
				Vfx.play(world, VfxIds.AMBER_GUST, g, 1.0F);
			}
			if (Destruction.enabled(world)) {
				// The wind of its wings cuts a swath through the trees and grass.
				int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z), gy = MathHelper.floor(g.y);
				for (int x = -3; x <= 3; x++) {
					for (int z = -3; z <= 3; z++) {
						if (x * x + z * z > 10) {
							continue;
						}
						for (int y = -1; y <= 12; y++) {
							BlockPos b = new BlockPos(gx + x, gy + y, gz + z);
							BlockState s = world.getBlockState(b);
							if (s.isIn(BlockTags.LEAVES) || !s.isAir() && plant(s)) {
								q().add(b, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
							}
						}
					}
				}
			}
			for (LivingEntity e : Kit.living(world, p, 7)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d away = new Vec3d(e.getX() - p.x, 0, e.getZ() - p.z);
				away = away.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : away.normalize();
				Kit.push(e, away.multiply(1.3).add(0, 0.7, 0));
				e.damage(world.getDamageSources().generic(), 9.0F);
			}
			if (age() % 6 == 0) {
				Kit.sound(world, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 64.0F, 0.5F);
			}
		}

		/** Resin wells up round the dragonfly where it hovers: it is sealed in a new drop, everything caught in the resin with it. */
		private void entomb() {
			Vec3d c = base.add(0, Amber.SEAL_Y, 0);
			Vfx.play(world, VfxIds.AMBER_SEAL, c, 1.0F);
			Vfx.flash(world, c, 0.3F, 300, 0xFFC060);
			Kit.sound(world, c, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.BLOCK_BEACON_DEACTIVATE, 96.0F, 0.6F);
			Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 96.0F, 0.4F);
			if (!Destruction.enabled(world)) {
				return;
			}
			// The fossil goes in first, then the drop closes over it from the bottom up.
			Bones fossil = new Bones();
			FossilPoses.Fly fly = FossilPoses.fly(Amber.ENTOMB, heading, true);
			for (Seg s : fly.segs) {
				fossil.line(base.add(s.a()), base.add(s.b()), Math.max(0.45, s.ra()), Math.max(0.45, s.rb()), s.fire() ? 3 : 2);
			}
			for (Vec3d e : fly.eyes) {
				fossil.dot(base.add(e), 4);
			}
			for (Vec3d[] w : fly.wings) {
				for (double u = 0; u <= 1.001; u += 0.06) {
					for (double v = 0.15; v <= 0.85; v += 0.25) {
						Vec3d lead = w[0].lerp(w[1], u), trail = w[3].lerp(w[2], u);
						fossil.dot(base.add(lead.lerp(trail, v)), 5);
					}
				}
			}
			fossil.dedupe();
			for (int[] b : fossil.list) {
				q().add(b[0], b[1], b[2], mat(b[3]), BlockQueue.ANY);
			}
			seal = drop(base, heading, Amber.SEAL_Y, true);
			// A stalk of resin holding it up off the ground.
			Bones stalk = new Bones();
			double bottom = Amber.SEAL_Y - Amber.RY;
			stalk.line(base.add(0, -0.5, 0), base.add(0, bottom + 0.5, 0), 3.2, 1.6, 6);
			for (int[] b : stalk.list) {
				seal.list.add(0, b);
			}
			// Everything caught in the resin is encased in amber where it stands.
			for (LivingEntity e : Kit.living(world, base, Amber.R)) {
				if (Kit.creativeOrSpectator(e) || !e.isOnGround()) {
					continue;
				}
				BlockPos f = e.getBlockPos();
				int h = Math.max(2, MathHelper.ceil(e.getHeight()) + 1);
				int w = e.getWidth() > 1.2F ? 2 : 1;
				for (int x = -w; x <= w; x++) {
					for (int z = -w; z <= w; z++) {
						for (int y = 0; y <= h; y++) {
							boolean wall = Math.abs(x) == w || Math.abs(z) == w || y == h;
							if (wall) {
								q().add(f.add(x, y, z), Blocks.ORANGE_STAINED_GLASS.getDefaultState(), BlockQueue.AIR_ONLY);
							}
						}
					}
				}
				e.damage(world.getDamageSources().inWall(), 6.0F);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 4));
			}
		}
	}

	// ====================================================================== SABRE-TOOTH

	/**
	 * SABRE-TOOTH FOSSIL. The light goes red as dusk falls all at once; a pair of eyes burns in the gloom, and something
	 * unseen rakes glowing claw marks across the land, twice. Then the ground bursts and a sabre-toothed cat's skeleton
	 * as big as a house claws its way out, wrapped in a ghost of ember flesh, and roars. It prowls round and pounces
	 * three times, each landing raking the ground again. It leaps to the middle and a colossal ghost of its skull rises
	 * over it, jaws gaping wider and wider, and bites: its sabres tear two great trenches into the land. Then the cat
	 * turns to stone where it stands, roaring, and is left there.
	 */
	public static class SabreFossil extends OddBombEvents.Base {
		private Vec3d heading, base;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("sabre_fossil", Formatting.GOLD);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				Vfx.newFx(world, FX_SABRE, base, heading, 1.0F, 0xFF6A20, 0, 0, Sabre.END);
				Vfx.play(world, VfxIds.SABRE_DUSK, base, 1.0F);
				Kit.sound(world, base, SoundEvents.ENTITY_WOLF_GROWL, 96.0F, 0.3F);
				Kit.sound(world, base, SoundEvents.AMBIENT_CAVE.value(), 96.0F, 0.6F);
			}
			if (t < Sabre.END - 60 && t % 40 == 0) {
				Vfx.skyTint(world, base, Sabre.R + 120, t >= Sabre.GIANT ? 0xC01808 : 0x9A3010, t >= Sabre.GIANT ? 0.75F : 0.6F);
			}
			if (t > 10 && t < Sabre.EMERGE && t % 30 == 15) {
				Kit.sound(world, base.add(FossilShapes.frame(heading, -28, 0, 10)), SoundEvents.ENTITY_WOLF_GROWL, 64.0F, 0.25F);
			}
			for (int k = 0; k < Sabre.N_RAKES; k++) {
				if (t == Sabre.rakeAt(k)) {
					rake(k);
				}
			}
			if (t == Sabre.EMERGE) {
				Vfx.play(world, VfxIds.SABRE_EMERGE, base, 1.0F);
				Vfx.shake(world, base, 120, 1.2F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_EMERGE, 96.0F, 0.6F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.5F);
				if (Destruction.enabled(world)) {
					Kit.ringCrack(world, base, 7, 2, Blocks.COARSE_DIRT.getDefaultState(), 0.6F);
					Kit.ringCrack(world, base, 12, 1, null, 0.4F);
				}
			}
			if (t > Sabre.EMERGE && t < Sabre.EMERGED && t % 8 == 0) {
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_SKELETON_STEP, 48.0F, 0.4F);
				Vfx.play(world, VfxIds.SABRE_EMERGE, base, 0.45F);
			}
			if (t == Sabre.ROAR) {
				roar(1.0F);
			}
			if (t >= Sabre.STALK && t < Sabre.FINAL && !Sabre.leaping(t) && !Sabre.crouching(t) && t % 10 == 0) {
				Vec3d p = at(base, heading, Sabre.pos(t));
				Kit.sound(world, p, SoundEvents.ENTITY_IRON_GOLEM_STEP, 64.0F, 0.45F);
				Kit.sound(world, p, SoundEvents.ENTITY_SKELETON_STEP, 64.0F, 0.4F);
			}
			for (int k = 0; k < Sabre.POUNCES.length; k++) {
				int p = Sabre.POUNCES[k];
				if (t == p) {
					Kit.sound(world, at(base, heading, Sabre.pos(t)), SoundEvents.ENTITY_WOLF_GROWL, 96.0F, 0.3F);
				}
				if (t == p + Sabre.CROUCH) {
					Kit.sound(world, at(base, heading, Sabre.pos(t)), SoundEvents.ENTITY_RAVAGER_ROAR, 96.0F, 1.1F);
					Kit.sound(world, at(base, heading, Sabre.pos(t)), SoundEvents.ENTITY_PHANTOM_SWOOP, 96.0F, 0.5F);
				}
				if (t == p + Sabre.CROUCH + Sabre.LEAP) {
					land(Sabre.target(k), 1.0 + k * 0.2);
				}
			}
			if (t == Sabre.FINAL) {
				Kit.sound(world, at(base, heading, Sabre.pos(t)), SoundEvents.ENTITY_WOLF_GROWL, 128.0F, 0.25F);
			}
			if (t == Sabre.FINAL + Sabre.CROUCH) {
				Kit.sound(world, at(base, heading, Sabre.pos(t)), SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 1.0F);
			}
			if (t == Sabre.LAND) {
				land(Vec3d.ZERO, 1.6);
			}
			if (t == Sabre.GIANT) {
				Vec3d sky = base.add(FossilShapes.frame(heading, -Sabre.GIANT_BACK, Sabre.GIANT_UP, 0));
				Kit.sound(world, sky, SoundEvents.ENTITY_WARDEN_ROAR, 128.0F, 0.4F);
				Kit.sound(world, sky, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 128.0F, 0.4F);
				Kit.sound(world, sky, SoundEvents.BLOCK_BELL_RESONATE, 128.0F, 0.4F);
				Vfx.shake(world, base, 200, 0.8F);
			}
			if (t > Sabre.GIANT && t < Sabre.BITE && t % 12 == 0) {
				Kit.sound(world, base, SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.3F + (t - Sabre.GIANT) * 0.004F);
				Vfx.shake(world, base, 200, 0.4F + (t - Sabre.GIANT) * 0.01F);
			}
			if (t == Sabre.BITE) {
				bite();
			}
			if (t == Sabre.PETRIFY) {
				petrify();
			}
			drain();
			return t >= Sabre.END;
		}

		/** The ground under the body, relative to the bomb. */
		private double groundAt(Vec3d local) {
			Vec3d w = at(base, heading, new Vec3d(local.x, 0, local.z));
			return Kit.ground(world, w.x, w.z).y - base.y;
		}

		private FossilPoses.Cat pose(int t) {
			return FossilPoses.cat(t, 0, groundAt(Sabre.pos(t)), heading);
		}

		private void roar(float power) {
			Vec3d mouth = base.add(pose(age()).mouth);
			Kit.sound(world, mouth, SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.5F);
			Kit.sound(world, mouth, SoundEvents.ENTITY_WARDEN_ROAR, 128.0F, 0.7F);
			Kit.sound(world, mouth, SoundEvents.ENTITY_WOLF_GROWL, 128.0F, 0.3F);
			Vfx.play(world, VfxIds.SABRE_ROAR, mouth, FossilShapes.frame(heading, Sabre.facing(age())), power, 0);
			Vfx.shake(world, mouth, 160, power);
			for (LivingEntity e : Kit.living(world, mouth, 24 * power)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
				}
			}
		}

		/** Three claw marks raked into the ground, glowing at the bottom, everything in their way cut. */
		private void rake(int k) {
			double[] r = Sabre.rake(k);
			Vec3d dir = FossilShapes.frame(heading, Math.cos(r[2]), 0, Math.sin(r[2])), side = new Vec3d(-dir.z, 0, dir.x);
			Vec3d start = at(base, heading, new Vec3d(r[0], 0, r[1]));
			Vec3d mid = Kit.ground(world, start.x + dir.x * r[3] * 0.5, start.z + dir.z * r[3] * 0.5);
			Kit.sound(world, mid, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 128.0F, 0.4F);
			Kit.sound(world, mid, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 128.0F, 0.5F);
			Kit.sound(world, mid, SoundEvents.ENTITY_RAVAGER_ROAR, 96.0F, 0.6F);
			Vfx.shake(world, mid, 100, 0.8F);
			for (double u = 0; u <= r[3]; u += 3) {
				Vec3d p = Kit.ground(world, start.x + dir.x * u, start.z + dir.z * u);
				Vfx.play(world, VfxIds.SABRE_RAKE, p, dir, (float) (0.8 + 0.4 * Math.sin(Math.PI * u / r[3])), 0);
			}
			for (LivingEntity e : Kit.living(world, mid, r[3] * 0.5 + 4)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(start);
				double along = to.x * dir.x + to.z * dir.z, off = Math.abs(to.x * side.x + to.z * side.z);
				if (along > -2 && along < r[3] + 2 && off < 5) {
					e.damage(world.getDamageSources().generic(), 14.0F);
					Kit.push(e, side.multiply(to.x * side.x + to.z * side.z > 0 ? 1.0 : -1.0).add(0, 0.6, 0));
				}
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int c = -1; c <= 1; c++) {
				for (double u = 0; u <= r[3]; u += 0.5) {
					double f = Math.sin(Math.PI * u / r[3]);
					// Claws curve a little, the middle one longest and deepest.
					double bend = Math.sin(u / r[3] * 2.2) * 1.2 * c;
					int depth = (int) Math.round((c == 0 ? 3.2 : 2.4) * f);
					if (depth <= 0 || (c != 0 && (u < r[3] * 0.08 || u > r[3] * 0.92))) {
						continue;
					}
					Vec3d p = start.add(dir.multiply(u)).add(side.multiply(c * 2.6 + bend));
					int x = MathHelper.floor(p.x), z = MathHelper.floor(p.z);
					BlockPos top = surface(x, z);
					for (int y = 0; y < depth; y++) {
						q().add(top.down(y), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
					for (int y = 1; y <= 4; y++) {
						q().add(top.up(y), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
					BlockState floor = (FossilShapes.hash(x, z) < 0.45 ? Blocks.MAGMA_BLOCK : Blocks.COARSE_DIRT).getDefaultState();
					q().add(top.down(depth), floor, BlockQueue.SOLID_ONLY);
				}
			}
		}

		/** A landing: earth and dust blasted up, the ground cracked round the paws, everything near thrown and hurt, a fresh rake. */
		private void land(Vec3d local, double power) {
			Vec3d g = Kit.ground(world, at(base, heading, local).x, at(base, heading, local).z);
			Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.ENTITY_RAVAGER_STEP, 128.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 128.0F, 0.5F);
			Vfx.play(world, VfxIds.SABRE_POUNCE, g, (float) power);
			Vfx.shake(world, g, 160, (float) (1.2 * power));
			if (Destruction.enabled(world)) {
				Kit.ringCrack(world, g, 7 * power, 2, Blocks.COARSE_DIRT.getDefaultState(), 0.6F);
			}
			for (LivingEntity e : Kit.living(world, g, 13 * power)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(g);
				double d = Math.max(0.5, to.length());
				double f = 1 - d / (13 * power);
				Kit.push(e, to.multiply(1 / d).multiply(1.7 * f).add(0, 0.9 * f, 0));
				e.damage(world.getDamageSources().generic(), (float) (16 * f * power));
			}
		}

		/**
		 * The ghost skull's bite: its jaws slam shut and its sabres tear two trenches deep into the land in front of the
		 * cat, glowing at the bottom; the ground between them heaves and everything there is crushed.
		 */
		private void bite() {
			Vec3d c = at(base, heading, new Vec3d((Sabre.BITE_FROM + Sabre.BITE_TO) * 0.5, 0, 0));
			for (int s = -1; s <= 1; s += 2) {
				Vec3d hit = Kit.ground(world, at(base, heading, new Vec3d(Sabre.BITE_FROM + 2, 0, s * Sabre.BITE_GAP)).x,
						at(base, heading, new Vec3d(Sabre.BITE_FROM + 2, 0, s * Sabre.BITE_GAP)).z);
				Vfx.play(world, VfxIds.SABRE_BITE, hit, heading, 1.0F, 0);
			}
			Vfx.flash(world, c.add(0, 10, 0), 0.6F, 400, 0xFF9050);
			Vfx.shake(world, c, 400, 2.2F);
			Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_LAND, 128.0F, 0.3F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.5F);
			for (LivingEntity e : Kit.living(world, c, (Sabre.BITE_TO - Sabre.BITE_FROM) * 0.5 + 10)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				double[] l = local(base, heading, e.getX(), e.getZ());
				if (l[0] > Sabre.BITE_FROM - 4 && l[0] < Sabre.BITE_TO + 4 && Math.abs(l[1]) < Sabre.BITE_GAP + 8) {
					e.damage(world.getDamageSources().generic(), Math.abs(l[1]) < Sabre.BITE_GAP + 3 ? 40.0F : 16.0F);
					Kit.push(e, new Vec3d(0, 1.2, 0).add(FossilShapes.frame(heading, 0, 0, Math.signum(l[1]) * 1.2)));
				}
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int s = -1; s <= 1; s += 2) {
				for (double u = Sabre.BITE_FROM; u <= Sabre.BITE_TO; u += 0.5) {
					double k = (u - Sabre.BITE_FROM) / (Sabre.BITE_TO - Sabre.BITE_FROM);
					int depth = (int) Math.round(Sabre.BITE_DEPTH * (1 - 0.7 * k) * Math.min(1, k * 8 + 0.3));
					double w = Sabre.BITE_W * (1 - 0.5 * k);
					for (double v = -w; v <= w; v += 0.5) {
						Vec3d p = at(base, heading, new Vec3d(u, 0, s * Sabre.BITE_GAP + v));
						int x = MathHelper.floor(p.x), z = MathHelper.floor(p.z);
						BlockPos top = surface(x, z);
						for (int y = -depth; y <= 5; y++) {
							q().add(top.up(y), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						}
						q().add(top.down(depth + 1), (FossilShapes.hash(x, z) < 0.6 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).getDefaultState(), BlockQueue.SOLID_ONLY);
					}
				}
			}
			// Earth thrown up along the lips of the trenches.
			Kit.ringCrack(world, c, 14, 2, Blocks.COARSE_DIRT.getDefaultState(), 0.5F);
		}

		/** The ghost burns off and the cat is left standing in stone, roaring, sabres bared. */
		private void petrify() {
			FossilPoses.Cat cat = pose(Sabre.PETRIFY);
			Vec3d body = base.add(cat.body);
			Vfx.play(world, VfxIds.SABRE_STONE, body, 1.0F);
			Kit.sound(world, body, SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.5F);
			Kit.sound(world, body, SoundEvents.BLOCK_BONE_BLOCK_PLACE, 96.0F, 0.4F);
			Kit.sound(world, body, SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 96.0F, 0.5F);
			Vfx.shake(world, body, 120, 0.8F);
			if (!Destruction.enabled(world)) {
				return;
			}
			Bones bones = new Bones();
			double sc = FossilPoses.CAT_SC;
			for (Seg s : cat.segs) {
				bones.line(base.add(s.a()), base.add(s.b()), Math.max(0.45, s.ra() * sc), Math.max(0.45, s.rb() * sc), s.fire() ? 1 : 0);
			}
			for (Vec3d e : cat.eyes) {
				bones.dot(base.add(e), 2);
			}
			bones.dedupe();
			for (int[] b : bones.list) {
				BlockState s = (b[3] == 1 ? Blocks.SMOOTH_QUARTZ : b[3] == 2 ? Blocks.SHROOMLIGHT : Blocks.BONE_BLOCK).getDefaultState();
				q().add(b[0], b[1], b[2], s, BlockQueue.ANY);
			}
		}
	}

	// ====================================================================== AMMONITE

	/**
	 * AMMONITE FOSSIL. An ammonite's shell, coiled like a wheel and taller than a house, grows up out of the ground
	 * chamber by chamber, its sutures lighting in pearl as each wall closes. The ghost of the animal reaches out of its
	 * mouth, tentacles feeling over the land. Then a spiral of light unwinds out of it across the countryside, three
	 * turns out to seventy blocks, and everything it passes is turned to stone: the land is pressed into the imprint of
	 * a giant ammonite, ridged and ribbed, with the shell standing at its heart.
	 */
	public static class AmmoniteFossil extends OddBombEvents.Base {
		private static final double RIB = 0.27;
		private Vec3d heading, base;
		private Bones shell = new Bones();
		private int placed;
		private TerrainJob imprint;

		private static BlockState mat(int k) {
			return (switch (k) {
				case 1 -> Blocks.BONE_BLOCK;
				case 2 -> Blocks.SMOOTH_QUARTZ;
				case 3 -> Blocks.PEARLESCENT_FROGLIGHT;
				default -> Blocks.CALCITE;
			}).getDefaultState();
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("ammonite_fossil", Formatting.LIGHT_PURPLE);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				shell = plan(base, heading);
				Vfx.newFx(world, FX_AMMONITE, base, heading, 1.0F, 0xF0C0E0, 0, 0, Ammonite.END);
				Vfx.play(world, VfxIds.AMMO_HEAVE, base, heading, 1.0F, 0);
				Kit.fissure(world, base.subtract(heading.multiply(14)), heading, 28, 3, 5, Blocks.CALCITE.getDefaultState());
				Kit.sound(world, base, SoundEvents.BLOCK_DEEPSLATE_BREAK, 64.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 32.0F, 0.4F);
				Vfx.shake(world, base, 100, 0.8F);
			}
			if (t >= Ammonite.COIL && t <= Ammonite.COILED + 1) {
				double grown = Ammonite.grown(t);
				int to = (int) Math.round(grown * 20);
				while (placed < shell.list.size() && shell.list.get(placed)[4] <= to) {
					int[] b = shell.list.get(placed++);
					q().add(b[0], b[1], b[2], mat(b[3]), BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Vec3d p = at(base, heading, Ammonite.coil(grown));
					Vfx.play(world, VfxIds.AMMO_GROW, p, (float) (0.5 + Ammonite.rho(grown) / 10));
					Kit.sound(world, p, SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 48.0F, 0.6F);
					Kit.sound(world, p, SoundEvents.BLOCK_BONE_BLOCK_PLACE, 48.0F, 0.5F);
				}
				if (t % 24 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 64.0F, 0.6F + (t - Ammonite.COIL) * 0.002F);
				}
			}
			if (t == Ammonite.AWAKEN) {
				Kit.sound(world, base, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, 96.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 96.0F, 0.6F);
			}
			if (t == Ammonite.SPIRAL) {
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 128.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 128.0F, 0.5F);
				if (Destruction.enabled(world)) {
					imprint = job();
					imprint.limit = 0;
				}
			}
			if (imprint != null) {
				imprint.limit = t >= Ammonite.SPIRAL_END ? Double.MAX_VALUE : Ammonite.unwound(t) / Ammonite.PHI_MAX;
				imprint.step(4000);
			}
			if (t >= Ammonite.SPIRAL && t <= Ammonite.SPIRAL_END) {
				Vec3d local = Ammonite.ground(Ammonite.unwound(t));
				Vec3d w = at(base, heading, local);
				Vec3d f = Kit.ground(world, w.x, w.z);
				if (t % 2 == 0) {
					Vfx.play(world, VfxIds.AMMO_FRONT, f, (float) (0.8 + local.length() / 60));
				}
				if (t % 5 == 0) {
					Kit.sound(world, f, SoundEvents.BLOCK_STONE_BREAK, 96.0F, 0.5F);
					Kit.sound(world, f, SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 96.0F, 0.4F);
					Vfx.shake(world, f, 60, 0.6F);
				}
				if (t % 4 == 0) {
					petrify(f, 7);
				}
			}
			if (t == Ammonite.PULSE) {
				pulse();
			}
			if (t == Ammonite.FADE) {
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 96.0F, 0.4F);
			}
			drain();
			return t >= Ammonite.END && (imprint == null || imprint.isDone());
		}

		/**
		 * The shell: a log-spiral tube squashed sideways, built from the innermost whorl out, with ribs, whiter inner
		 * whorls, a hollow living chamber and a rim of pearl round its mouth. Each entry's order is 20 x theta.
		 */
		public static Bones plan(Vec3d base, Vec3d heading) {
			Bones b = new Bones();
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			for (double th = 0.6; th <= Ammonite.THETA_MAX; th += 0.4 / Math.max(Ammonite.rho(th), 0.6)) {
				b.order = (int) Math.round(th * 20);
				Vec3d c = at(base, heading, Ammonite.coil(th));
				Vec3d out = FossilShapes.frame(heading, Ammonite.out(th));
				double tr = Math.max(0.6, Ammonite.K * Ammonite.rho(th)), w = Math.max(0.5, tr * Ammonite.WIDTH);
				boolean rib = th > 3 && (th / 0.3) % 1.0 < 0.22;
				boolean lip = th > Ammonite.THETA_MAX - 0.4;
				boolean living = th > Ammonite.THETA_MAX - Ammonite.LIVING;
				int kind = lip ? 3 : rib ? 1 : th < 9 ? 2 : 0;
				double ra = rib ? tr * 1.12 : tr, rw = rib ? w * 1.12 : w;
				for (double a = -ra; a <= ra + 1.0E-6; a += 0.5) {
					for (double s = -rw; s <= rw + 1.0E-6; s += 0.5) {
						double e = (a / ra) * (a / ra) + (s / rw) * (s / rw);
						if (e > 1 || living && !lip && e < 0.5) {
							continue;
						}
						Vec3d p = c.add(out.multiply(a)).add(right.multiply(s));
						if (p.y >= base.y - 1) {
							b.dot(p, kind);
						}
					}
				}
			}
			b.sortByOrder();
			return b;
		}

		/** Every column of the imprint, keyed by how far along the spiral of light reaches it (0 to 1). */
		private TerrainJob job() {
			int r = MathHelper.ceil(Ammonite.R) + 2;
			int cx = MathHelper.floor(base.x), cz = MathHelper.floor(base.z);
			List<long[]> cols = new ArrayList<>();
			List<Float> keys = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double[] l = local(base, heading, cx + dx + 0.5, cz + dz + 0.5);
					double[] n = Ammonite.nearest(l[0], l[1]);
					if (n == null) {
						continue;
					}
					cols.add(new long[]{BlockPos.asLong(cx + dx, 0, cz + dz)});
					keys.add((float) (n[0] / Ammonite.PHI_MAX));
				}
			}
			long[] c = new long[cols.size()];
			float[] k = new float[cols.size()];
			for (int i = 0; i < c.length; i++) {
				c[i] = cols.get(i)[0];
				k[i] = keys.get(i);
			}
			return new TerrainJob(world, c, k, this::imprint);
		}

		/**
		 * One column of the imprint: a ridge along the spiral's line, ribs across each whorl, troughs between them a block
		 * down, all of it stone; trees are left petrified and everything growing is gone.
		 */
		private void imprint(TerrainJob.Column c) {
			double[] l = local(base, heading, c.x + 0.5, c.z + 0.5);
			// The shell stands here.
			if (Math.abs(l[1]) < 4.5 && Math.abs(l[0]) < 16) {
				return;
			}
			double[] n = Ammonite.nearest(l[0], l[1]);
			if (n == null) {
				return;
			}
			int surf = groundY(c);
			for (int y = Math.min(c.top(), surf + 28); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				} else if (s.isIn(BlockTags.LOGS)) {
					c.set(y, Blocks.DRIPSTONE_BLOCK.getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (!g.getFluidState().isEmpty()) {
				if (g.getFluidState().isIn(FluidTags.WATER)) {
					c.set(surf, Blocks.TUFF.getDefaultState());
				}
				return;
			}
			if (!earth(g) && !g.isOf(Blocks.CALCITE)) {
				return;
			}
			double phi = n[0], d = n[1], rk = n[2];
			double r = Math.hypot(l[0], l[1]);
			if (r < Ammonite.AG * 0.8) {
				c.set(surf, Blocks.CALCITE.getDefaultState());
				return;
			}
			double ridge = 0.9 + rk * 0.02;
			double ribWidth = 1.3 / Math.max(rk, 1);
			boolean rib = (phi % RIB) / RIB < ribWidth / RIB;
			if (Math.abs(d) < ridge) {
				c.set(surf, Blocks.CALCITE.getDefaultState());
				c.set(surf + 1, Blocks.CALCITE.getDefaultState());
				if (rk > 22 && Math.abs(d) < ridge * 0.55) {
					c.set(surf + 2, Blocks.SMOOTH_QUARTZ.getDefaultState());
				}
			} else if (rib) {
				c.set(surf, Blocks.CALCITE.getDefaultState());
				c.set(surf + 1, Blocks.BONE_BLOCK.getDefaultState());
			} else {
				double m = c.noise(71);
				Block floor = m < 0.68 ? Blocks.TUFF : m < 0.86 ? Blocks.DRIPSTONE_BLOCK : Blocks.SMOOTH_BASALT;
				c.set(surf, Blocks.AIR.getDefaultState());
				c.set(surf - 1, floor.getDefaultState());
			}
		}

		/** Caught by the spiral: turned to stone where they stand. */
		private void petrify(Vec3d at, double r) {
			for (LivingEntity e : Kit.living(world, at, r)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 5));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 160, 2));
				e.damage(world.getDamageSources().magic(), 8.0F);
				Vfx.play(world, VfxIds.AMMO_PETRIFY, e.getPos(), 1.0F);
			}
		}

		/** The shell's last pulse of pearl light: a ring of force rolls out over the stone. */
		private void pulse() {
			Vec3d c = at(base, heading, Ammonite.coil(Ammonite.THETA_MAX * 0.6));
			Vfx.play(world, VfxIds.AMMO_PULSE, base, 1.0F);
			Vfx.flash(world, c, 0.45F, 400, 0xFFE0F4);
			Vfx.shake(world, base, 300, 1.3F);
			Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 128.0F, 0.5F);
			Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.5F);
			for (LivingEntity e : Kit.living(world, base, Ammonite.R)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(base);
				double d = Math.max(1, Math.hypot(to.x, to.z));
				double k = 1 - d / Ammonite.R;
				Kit.push(e, new Vec3d(to.x / d * 1.5 * k, 0.6 * k + 0.2, to.z / d * 1.5 * k));
				e.damage(world.getDamageSources().magic(), (float) (12 * k));
			}
		}
	}
}
