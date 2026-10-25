package com.armageddon.destruction.world;

import com.armageddon.util.WorldShapes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;

/**
 * The lasting marks the three original WORLD DESTRUCTION bombs leave on the land. Each one is a
 * different shape and material language instead of the generic crater:
 * <ul>
 * <li>Galactic Smash: the giant's fist pressed into the ground, cosmic-glass cracks, bell ripples.</li>
 * <li>Booming Tick: a stepped bullseye of scorched terracotta terraces with clock-hand trenches.</li>
 * <li>Alien Death Ray: a glass-walled shaft, a salt flat from the wave, and an alien land of end stone,
 * purpur and green crystal spires.</li>
 * </ul>
 */
public final class WorldScars {
	private WorldScars() {
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private static double dist(TerrainJob.Column c, Vec3d g) {
		return Math.hypot(c.x + 0.5 - g.x, c.z + 0.5 - g.z);
	}

	/** Fills a column from {@code from} to {@code to} (inclusive) with {@code s}, capped. */
	private static void fill(TerrainJob.Column c, int from, int to, BlockState s, int cap) {
		for (int y = from; y <= Math.min(to, from + cap); y++) {
			c.set(y, s);
		}
	}

	/** Removes trees and plants standing on the column (up to 40 above the ground). */
	private static void strip(TerrainJob.Column c, int surf) {
		int top = Math.max(c.top(), surf);
		c.clear(surf + 1, Math.min(top, surf + 40) + 1);
	}

	// ====================================================================== GALACTIC SMASH

	/** The fist imprint: knuckle bowls, ridged finger plateau, thumb groove, and a lip of displaced earth. */
	public static TerrainJob fist(ServerWorld w, Vec3d g, Vec3d fwd, double R, int base) {
		Vec3d right = new Vec3d(-fwd.z, 0, fwd.x);
		return TerrainJob.disc(w, g, R * 0.78, c -> {
			double dx = c.x + 0.5 - g.x, dz = c.z + 0.5 - g.z;
			double f = dx * fwd.x + dz * fwd.z, r = dx * right.x + dz * right.z;
			double depth = WorldShapes.smashDepth(f, r, R);
			int surf = c.surface();
			if (depth > 0.5) {
				int floor = base - (int) Math.round(depth);
				c.clear(floor + 1, Math.min(Math.max(c.top(), surf) + 1, floor + 80));
				if (surf < floor) {
					fill(c, surf + 1, floor - 1, Blocks.BLACKSTONE.getDefaultState(), 30);
				}
				// Pressed so hard the ground turned to cosmic glass and crying obsidian.
				double n = c.noise(7);
				BlockState lining = n < 0.5 ? Blocks.CRYING_OBSIDIAN.getDefaultState()
						: n < 0.78 ? Blocks.OBSIDIAN.getDefaultState()
						: n < 0.94 ? Blocks.AMETHYST_BLOCK.getDefaultState()
						: Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
				c.set(floor, lining);
				c.set(floor - 1, Blocks.OBSIDIAN.getDefaultState());
				c.set(floor - 2, Blocks.BLACKSTONE.getDefaultState());
				if (n > 0.975) {
					c.set(floor + 1, Blocks.AMETHYST_CLUSTER.getDefaultState());
				}
				return;
			}
			int lip = (int) Math.round(WorldShapes.smashRim(f, r, R, depth));
			if (lip > 0) {
				BlockState top = c.get(surf);
				if (!top.getFluidState().isEmpty() || top.isIn(BlockTags.LOGS)) {
					return;
				}
				// Earth shoved out from under the fist: churned, scorched dirt piled into a lip.
				BlockState churn = c.noise(3) < 0.5 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.ROOTED_DIRT.getDefaultState();
				fill(c, surf + 1, surf + lip - 1, churn, 20);
				c.set(surf + lip, c.noise(4) < 0.25 ? Blocks.BLACKSTONE.getDefaultState() : top.isOf(Blocks.GRASS_BLOCK) ? churn : top);
				c.set(surf + lip + 1, AIR);
			}
		});
	}

	/** Radial cracks running out from the knuckles, floored with glowing purple glass. */
	public static TerrainJob cracks(ServerWorld w, Vec3d g, Vec3d fwd, double R) {
		Vec3d right = new Vec3d(-fwd.z, 0, fwd.x);
		Map<Long, Integer> cells = new HashMap<>();
		for (double[][] crack : WorldShapes.smashCracks(WorldShapes.seed(g), R)) {
			double total = 0;
			for (int i = 1; i < crack.length; i++) {
				total += Math.hypot(crack[i][0] - crack[i - 1][0], crack[i][1] - crack[i - 1][1]);
			}
			double run = 0;
			for (int i = 1; i < crack.length; i++) {
				double f0 = crack[i - 1][0], r0 = crack[i - 1][1], f1 = crack[i][0], r1 = crack[i][1];
				double len = Math.hypot(f1 - f0, r1 - r0);
				for (double s = 0; s < len; s += 0.6) {
					double u = (run + s) / total;
					double f = f0 + (f1 - f0) * s / len, r = r0 + (r1 - r0) * s / len;
					double x = g.x + fwd.x * f + right.x * r, z = g.z + fwd.z * f + right.z * r;
					double half = MathHelper.lerp(u, 2.2, 0.7);
					int depth = (int) Math.round(MathHelper.lerp(u, 11, 4));
					int hr = MathHelper.ceil(half + 1);
					for (int ox = -hr; ox <= hr; ox++) {
						for (int oz = -hr; oz <= hr; oz++) {
							double d = Math.hypot(ox, oz);
							if (d > half + 1) {
								continue;
							}
							long key = BlockPos.asLong(MathHelper.floor(x) + ox, 0, MathHelper.floor(z) + oz);
							int v = d <= half ? depth << 1 : 1;
							cells.merge(key, v, (a, b) -> (a & 1) == 0 ? ((b & 1) == 0 ? Math.max(a, b) : a) : b);
						}
					}
				}
				run += len;
			}
		}
		long[] cols = new long[cells.size()];
		float[] keys = new float[cells.size()];
		int n = 0;
		for (long k : cells.keySet()) {
			cols[n] = k;
			keys[n] = (float) Math.hypot(BlockPos.unpackLongX(k) + 0.5 - g.x, BlockPos.unpackLongZ(k) + 0.5 - g.z);
			n++;
		}
		return new TerrainJob(w, cols, keys, c -> {
			Integer v = cells.get(BlockPos.asLong(c.x, 0, c.z));
			if (v == null) {
				return;
			}
			int surf = c.surface();
			if ((v & 1) == 1) {
				// The crack's lips blacken into crying obsidian.
				if (c.get(surf).getFluidState().isEmpty()) {
					c.set(surf, Blocks.CRYING_OBSIDIAN.getDefaultState());
				}
				return;
			}
			int depth = v >> 1;
			c.clear(surf - depth + 2, Math.max(c.top(), surf) + 1);
			c.set(surf - depth + 1, Blocks.PURPLE_STAINED_GLASS.getDefaultState());
			c.set(surf - depth, Blocks.PEARLESCENT_FROGLIGHT.getDefaultState());
			c.set(surf - depth - 1, Blocks.AMETHYST_BLOCK.getDefaultState());
		});
	}

	/** The land rings like a struck bell: concentric waves frozen into the terrain around the fist. */
	public static TerrainJob ripples(ServerWorld w, Vec3d g, double R) {
		return TerrainJob.disc(w, g, R * 1.05, WorldShapes.rippleReach(R), c -> {
			double d = dist(c, g);
			int dy = (int) Math.round(WorldShapes.ripple(d, R));
			if (dy == 0) {
				return;
			}
			int surf = c.surface();
			BlockState top = c.get(surf);
			if (!top.getFluidState().isEmpty() || top.isIn(BlockTags.LOGS) || top.isIn(BlockTags.LEAVES) || top.isAir()) {
				return;
			}
			BlockState under = c.get(surf - 1);
			if (under.isAir() || !under.getFluidState().isEmpty()) {
				under = Blocks.DIRT.getDefaultState();
			}
			if (dy > 0) {
				fill(c, surf + 1, surf + dy - 1, under, 12);
				c.set(surf + dy, top);
				c.set(surf + dy + 1, AIR);
				// Crests catch a little cosmic dust.
				if (c.noise(11) < 0.025) {
					c.set(surf + dy + 1, Blocks.SMALL_AMETHYST_BUD.getDefaultState());
				}
			} else {
				c.clear(surf + dy + 1, surf + 1);
				c.set(surf + dy, top);
			}
		});
	}

	// ====================================================================== BOOMING TICK

	private static BlockState bandBlock(int i) {
		return switch (WorldShapes.BAND_PALETTE[MathHelper.clamp(i, 0, WorldShapes.BANDS)]) {
			case 0 -> Blocks.REDSTONE_BLOCK.getDefaultState();
			case 1 -> Blocks.RED_TERRACOTTA.getDefaultState();
			case 2 -> Blocks.TERRACOTTA.getDefaultState();
			case 3 -> Blocks.ORANGE_TERRACOTTA.getDefaultState();
			case 4 -> Blocks.YELLOW_TERRACOTTA.getDefaultState();
			default -> Blocks.WHITE_TERRACOTTA.getDefaultState();
		};
	}

	/** One terrace of the target: levelled to its own step, floored in its colour, edged in scorched stone. */
	public static TerrainJob band(ServerWorld w, Vec3d g, double R, int base, int i) {
		double inner = i == 0 ? 0 : WorldShapes.bandRadius(i - 1, R), outer = WorldShapes.bandRadius(i, R);
		int floor = base - WorldShapes.bandDepth(i);
		BlockState color = bandBlock(i);
		return TerrainJob.disc(w, g, inner, outer, c -> {
			double d = dist(c, g);
			int surf = c.surface();
			c.clear(floor + 1, Math.min(Math.max(c.top(), surf) + 1, floor + 60));
			if (surf < floor) {
				fill(c, surf + 1, floor - 1, Blocks.BLACKSTONE.getDefaultState(), 25);
			}
			BlockState top = color;
			if (outer - d < 1.0) {
				top = Blocks.POLISHED_BLACKSTONE.getDefaultState();
			} else if (c.noise(5) < 0.05 * (WorldShapes.BANDS - i) / WorldShapes.BANDS) {
				top = Blocks.MAGMA_BLOCK.getDefaultState();
			}
			c.set(floor, top);
			c.set(floor - 1, Blocks.BLACKSTONE.getDefaultState());
		});
	}

	/** Twelve clock-hand trenches from the hub to the rim, glowing magma at the bottom. */
	public static TerrainJob spokes(ServerWorld w, Vec3d g, Vec3d fwd, double R, int base) {
		double yaw = Math.atan2(fwd.z, fwd.x);
		Map<Long, Boolean> cells = new HashMap<>();
		for (int k = 0; k < 12; k++) {
			double a = yaw + k * Math.PI / 6;
			double cx = Math.cos(a), cz = Math.sin(a);
			double width = k == 0 ? 1.6 : 1.0;
			for (double s = WorldShapes.bandRadius(1, R); s <= R; s += 0.5) {
				for (double o = -width; o <= width; o += 0.5) {
					cells.put(BlockPos.asLong(MathHelper.floor(g.x + cx * s - cz * o), 0, MathHelper.floor(g.z + cz * s + cx * o)), Boolean.TRUE);
				}
			}
		}
		long[] cols = new long[cells.size()];
		float[] keys = new float[cells.size()];
		int n = 0;
		for (long key : cells.keySet()) {
			cols[n] = key;
			keys[n] = (float) Math.hypot(BlockPos.unpackLongX(key) + 0.5 - g.x, BlockPos.unpackLongZ(key) + 0.5 - g.z);
			n++;
		}
		return new TerrainJob(w, cols, keys, c -> {
			int band = WorldShapes.bandOf(dist(c, g), R);
			if (band > WorldShapes.BANDS) {
				return;
			}
			int floor = base - WorldShapes.bandDepth(band);
			c.clear(floor - 2, floor);
			c.set(floor - 3, Blocks.MAGMA_BLOCK.getDefaultState());
			c.set(floor - 4, Blocks.BLACKSTONE.getDefaultState());
		});
	}

	/** The hub: the tick's hole, a tinted-glass tube dropping to a glowing redstone heart. */
	public static TerrainJob hub(ServerWorld w, Vec3d g, int base) {
		int floor = base - WorldShapes.bandDepth(0);
		int bottom = floor - 24;
		return TerrainJob.disc(w, g, 3.2, c -> {
			double d = dist(c, g);
			if (d < 2.0) {
				c.clear(bottom + 1, floor + 2);
				c.set(bottom, Blocks.REDSTONE_BLOCK.getDefaultState());
			} else {
				for (int y = bottom; y <= floor; y++) {
					c.set(y, (y - bottom) % 6 == 0 ? Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true) : Blocks.TINTED_GLASS.getDefaultState());
				}
			}
		});
	}

	// ====================================================================== ALIEN DEATH RAY

	private static boolean electrical(BlockState s) {
		return s.isOf(Blocks.REDSTONE_WIRE) || s.isOf(Blocks.REPEATER) || s.isOf(Blocks.COMPARATOR) || s.isOf(Blocks.REDSTONE_TORCH)
				|| s.isOf(Blocks.REDSTONE_WALL_TORCH) || s.isIn(BlockTags.RAILS) || s.isOf(Blocks.LEVER) || s.isIn(BlockTags.BUTTONS)
				|| s.isOf(Blocks.REDSTONE_LAMP) || s.isOf(Blocks.LIGHTNING_ROD) || s.isOf(Blocks.TRIPWIRE) || s.isOf(Blocks.TRIPWIRE_HOOK);
	}

	/** EMP: every bit of redstone near the surface burns out. Lamps go dark for good. */
	public static TerrainJob emp(ServerWorld w, Vec3d g, double radius) {
		return TerrainJob.disc(w, g, radius, c -> {
			int surf = c.surface();
			for (int y = surf - 6; y <= surf + 14; y++) {
				BlockState s = c.get(y);
				if (!s.isAir() && electrical(s)) {
					c.set(y, s.isOf(Blocks.REDSTONE_LAMP) ? Blocks.TINTED_GLASS.getDefaultState() : AIR);
				}
			}
		});
	}

	/** What the wave leaves behind: a bleached salt flat of sand, salt crust, prismarine, puddles and kelp. */
	public static TerrainJob saltFlat(ServerWorld w, Vec3d g, double inner, double outer) {
		return TerrainJob.disc(w, g, inner, outer, c -> {
			double d = dist(c, g);
			// Feathered edges on both sides.
			double edge = Math.min((d - inner) / 10.0, (outer - d) / 18.0);
			if (c.noise(2) > edge) {
				return;
			}
			int surf = c.surface();
			BlockState top = c.get(surf);
			if (!top.getFluidState().isEmpty() || top.isAir()) {
				return;
			}
			strip(c, surf);
			double n = c.noise(6);
			c.set(surf - 1, Blocks.SAND.getDefaultState());
			c.set(surf - 2, Blocks.SANDSTONE.getDefaultState());
			if (n < 0.6) {
				c.set(surf, Blocks.SAND.getDefaultState());
			} else if (n < 0.77) {
				c.set(surf, Blocks.WHITE_CONCRETE_POWDER.getDefaultState());
			} else if (n < 0.85) {
				c.set(surf, Blocks.PRISMARINE.getDefaultState());
			} else if (n < 0.88) {
				c.set(surf, Blocks.DARK_PRISMARINE.getDefaultState());
			} else if (n < 0.94) {
				c.set(surf, Blocks.WATER.getDefaultState());
			} else if (n < 0.96) {
				c.set(surf, Blocks.KELP.getDefaultState());
			} else {
				c.set(surf, Blocks.SAND.getDefaultState());
				BlockState coral = (n < 0.98 ? Blocks.DEAD_TUBE_CORAL : Blocks.DEAD_FIRE_CORAL).getDefaultState();
				c.set(surf + 1, coral.contains(Properties.WATERLOGGED) ? coral.with(Properties.WATERLOGGED, false) : coral);
			}
		});
	}

	/** The rocket's wound: a bowl of end stone with green glass shards around a glass-walled shaft into the deep. */
	public static TerrainJob impactShaft(ServerWorld w, Vec3d g, double reach, int base) {
		double shaft = WorldShapes.shaftRadius(reach);
		double bowl = Math.min(32, reach * 0.25);
		return TerrainJob.disc(w, g, bowl, c -> {
			double d = dist(c, g);
			int surf = c.surface();
			int bottom = c.bottomY() + 5;
			if (d < shaft) {
				c.clear(bottom + 1, Math.max(c.top(), surf) + 1);
				c.set(bottom, Blocks.VERDANT_FROGLIGHT.getDefaultState());
				return;
			}
			int floor = base - (int) Math.round(14 * (1 - (d / bowl) * (d / bowl)));
			if (d < shaft + 1.3) {
				// The shaft wall: green glass all the way down, ringed with light every so often.
				for (int y = bottom; y <= floor; y++) {
					int band = Math.floorMod(y, 24);
					c.set(y, band == 0 ? Blocks.VERDANT_FROGLIGHT.getDefaultState()
							: band < 12 ? Blocks.GREEN_STAINED_GLASS.getDefaultState() : Blocks.LIME_STAINED_GLASS.getDefaultState());
				}
				c.clear(floor + 1, Math.max(c.top(), surf) + 1);
				return;
			}
			c.clear(floor + 1, Math.min(Math.max(c.top(), surf) + 1, floor + 60));
			if (surf < floor) {
				fill(c, surf + 1, floor - 1, Blocks.END_STONE.getDefaultState(), 20);
			}
			c.set(floor, c.noise(8) < 0.2 ? Blocks.END_STONE_BRICKS.getDefaultState() : Blocks.END_STONE.getDefaultState());
			c.set(floor - 1, Blocks.END_STONE.getDefaultState());
			// Shards of green glass jutting from the bowl.
			if (c.noise(9) < 0.07) {
				int h = 1 + (int) (c.noise(10) * 4);
				fill(c, floor + 1, floor + h, Blocks.LIME_STAINED_GLASS.getDefaultState(), 6);
			}
		});
	}

	/** The alien world takes over: end stone and purpur roads, vegetation gone, chorus growing. */
	public static TerrainJob terraform(ServerWorld w, Vec3d g, double reach) {
		double inner = Math.min(32, reach * 0.25);
		return TerrainJob.disc(w, g, inner, reach, c -> {
			double d = dist(c, g);
			double p = d < reach * 0.8 ? 1 : (reach - d) / (reach * 0.2);
			if (c.noise(3) > p) {
				return;
			}
			int surf = c.surface();
			BlockState top = c.get(surf);
			if (!top.getFluidState().isEmpty() || top.isAir()) {
				return;
			}
			strip(c, surf);
			double ang = Math.atan2(c.z + 0.5 - g.z, c.x + 0.5 - g.x);
			boolean road = Math.sin(ang * 9 + d * 0.06) > 0.975;
			BlockState ground = road ? Blocks.PURPUR_BLOCK.getDefaultState()
					: c.noise(4) < 0.14 ? Blocks.END_STONE_BRICKS.getDefaultState()
					: Blocks.END_STONE.getDefaultState();
			c.set(surf, ground);
			c.set(surf - 1, Blocks.END_STONE.getDefaultState());
			c.set(surf - 2, Blocks.END_STONE.getDefaultState());
			if (!road && c.noise(12) < 0.012) {
				int h = 2 + (int) (c.noise(13) * 4);
				BlockState stalk = Blocks.CHORUS_PLANT.getDefaultState().with(ConnectingBlock.UP, true).with(ConnectingBlock.DOWN, true);
				for (int y = 1; y < h; y++) {
					c.set(surf + y, stalk);
				}
				c.set(surf + h, Blocks.CHORUS_FLOWER.getDefaultState());
			}
		});
	}

	/** Green crystal spires with froglight hearts, queued bottom-up so they visibly grow out of the ground. */
	public static VoxelQueue spires(ServerWorld w, Vec3d g, double reach) {
		VoxelQueue q = new VoxelQueue(w);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (double[] s : WorldShapes.spires(WorldShapes.seed(g), reach)) {
			int bx = MathHelper.floor(g.x + s[0]), bz = MathHelper.floor(g.z + s[1]);
			if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
				continue;
			}
			int ground = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 2;
			int h = (int) s[2];
			double r0 = s[3];
			for (int y = 0; y <= h; y++) {
				double ry = r0 * Math.pow(1 - y / (double) (h + 1), 0.8);
				double cx = bx + 0.5 + s[4] * y, cz = bz + 0.5 + s[5] * y;
				int rr = MathHelper.ceil(ry);
				for (int ox = -rr; ox <= rr; ox++) {
					for (int oz = -rr; oz <= rr; oz++) {
						double x = MathHelper.floor(cx) + ox + 0.5, z = MathHelper.floor(cz) + oz + 0.5;
						double dd = Math.hypot(x - cx, z - cz);
						if (dd > ry + 0.3) {
							continue;
						}
						BlockState st = dd < Math.max(0.6, ry * 0.35) && y < h - 1 ? Blocks.VERDANT_FROGLIGHT.getDefaultState()
								: (y / 3) % 2 == 0 ? Blocks.LIME_STAINED_GLASS.getDefaultState() : Blocks.GREEN_STAINED_GLASS.getDefaultState();
						q.add(p.set(MathHelper.floor(x), ground + y, MathHelper.floor(z)), st, y);
					}
				}
			}
		}
		return q;
	}
}
