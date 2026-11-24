package com.armageddon.world;

import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.WorldJob;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Procedurally builds a test city: a grid of roads with lane markings and street lights, and on the lots between
 * them skyscrapers (taller toward the centre), low-rise blocks, parks, and a whole catalogue of oddly shaped
 * buildings made to be fun to knock down: round glass towers, twisted towers, inverted pyramids, helixes, TV towers
 * with a pod, art-deco setback spires. Some blocks of four lots are merged into one landmark: a stadium, a glass dome,
 * a Ferris wheel, a glass pyramid, twin towers joined by a skybridge, or a megatower. Then villagers fill the streets.
 * Sizes 1 to 6; the bigger the city, the taller it gets. Built incrementally, column by column.
 */
public class CityBuildJob implements WorldJob {
	private static final int CELL = 22;
	private static final int ROAD = 6;
	/** A single lot's buildable interior, and a merged two-by-two landmark's. */
	private static final int LOT = CELL - ROAD - 2, MEGA = 2 * CELL - ROAD - 2;
	private static final int CLEAR_HEIGHT = 48;
	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private static final M[] WALLS = {
			M.WHITE_CONCRETE, M.LIGHT_GRAY_CONCRETE, M.GRAY_CONCRETE,
			M.QUARTZ_BLOCK, M.CYAN_TERRACOTTA, M.BRICKS,
			M.POLISHED_ANDESITE, M.BLACK_CONCRETE, M.SMOOTH_SANDSTONE};
	private static final M[] GLASS = {
			M.GLASS, M.LIGHT_BLUE_STAINED_GLASS, M.CYAN_STAINED_GLASS,
			M.BLACK_STAINED_GLASS, M.GRAY_STAINED_GLASS};
	private static final M[] GONDOLAS = {
			M.RED_CONCRETE, M.YELLOW_CONCRETE, M.LIME_CONCRETE,
			M.LIGHT_BLUE_CONCRETE, M.MAGENTA_CONCRETE, M.ORANGE_CONCRETE};

	/**
	 * What the city is made of. The layout works in these, so it can be looked at outside the game; each turns into
	 * its block state only when the city is actually built.
	 */
	public enum M {
		AIR(() -> Blocks.AIR.getDefaultState()),
		BLACK_CONCRETE(() -> Blocks.BLACK_CONCRETE.getDefaultState()),
		BLACK_STAINED_GLASS(() -> Blocks.BLACK_STAINED_GLASS.getDefaultState()),
		BRICKS(() -> Blocks.BRICKS.getDefaultState()),
		CYAN_STAINED_GLASS(() -> Blocks.CYAN_STAINED_GLASS.getDefaultState()),
		CYAN_TERRACOTTA(() -> Blocks.CYAN_TERRACOTTA.getDefaultState()),
		DIRT_PATH(() -> Blocks.DIRT_PATH.getDefaultState()),
		GLASS(() -> Blocks.GLASS.getDefaultState()),
		GLOWSTONE(() -> Blocks.GLOWSTONE.getDefaultState()),
		GOLD_BLOCK(() -> Blocks.GOLD_BLOCK.getDefaultState()),
		GRASS_BLOCK(() -> Blocks.GRASS_BLOCK.getDefaultState()),
		GRAY_CONCRETE(() -> Blocks.GRAY_CONCRETE.getDefaultState()),
		GRAY_STAINED_GLASS(() -> Blocks.GRAY_STAINED_GLASS.getDefaultState()),
		IRON_BARS(() -> Blocks.IRON_BARS.getDefaultState()),
		IRON_BLOCK(() -> Blocks.IRON_BLOCK.getDefaultState()),
		LIGHT_BLUE_CONCRETE(() -> Blocks.LIGHT_BLUE_CONCRETE.getDefaultState()),
		LIGHT_BLUE_STAINED_GLASS(() -> Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState()),
		LIGHT_GRAY_CONCRETE(() -> Blocks.LIGHT_GRAY_CONCRETE.getDefaultState()),
		LIME_CONCRETE(() -> Blocks.LIME_CONCRETE.getDefaultState()),
		MAGENTA_CONCRETE(() -> Blocks.MAGENTA_CONCRETE.getDefaultState()),
		MOSS_BLOCK(() -> Blocks.MOSS_BLOCK.getDefaultState()),
		OAK_LOG(() -> Blocks.OAK_LOG.getDefaultState()),
		ORANGE_CONCRETE(() -> Blocks.ORANGE_CONCRETE.getDefaultState()),
		POLISHED_ANDESITE(() -> Blocks.POLISHED_ANDESITE.getDefaultState()),
		QUARTZ_BLOCK(() -> Blocks.QUARTZ_BLOCK.getDefaultState()),
		RED_CONCRETE(() -> Blocks.RED_CONCRETE.getDefaultState()),
		SEA_LANTERN(() -> Blocks.SEA_LANTERN.getDefaultState()),
		SMOOTH_SANDSTONE(() -> Blocks.SMOOTH_SANDSTONE.getDefaultState()),
		SMOOTH_STONE(() -> Blocks.SMOOTH_STONE.getDefaultState()),
		SMOOTH_STONE_SLAB(() -> Blocks.SMOOTH_STONE_SLAB.getDefaultState()),
		STONE(() -> Blocks.STONE.getDefaultState()),
		STONE_BRICK_WALL(() -> Blocks.STONE_BRICK_WALL.getDefaultState()),
		WHITE_CONCRETE(() -> Blocks.WHITE_CONCRETE.getDefaultState()),
		YELLOW_CONCRETE(() -> Blocks.YELLOW_CONCRETE.getDefaultState()),
		LIT_LAMP(() -> Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true)),
		OAK_LEAVES(() -> Blocks.OAK_LEAVES.getDefaultState().with(Properties.PERSISTENT, true));

		private final java.util.function.Supplier<BlockState> make;
		private BlockState state;

		M(java.util.function.Supplier<BlockState> make) {
			this.make = make;
		}

		public BlockState state() {
			if (state == null) {
				state = make.get();
			}
			return state;
		}
	}

	private final ServerWorld world;
	private final BlockPos origin;
	private final int half;
	private final int width;
	private final int cells;
	private final double tall;
	private final int size;
	private final long seed;
	private long cursor;
	private boolean populated;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	private CityBuildJob(ServerWorld world, BlockPos origin, int size) {
		this.world = world;
		this.origin = origin.toImmutable();
		this.size = size;
		this.cells = 2 + size * 2;
		this.half = cells * CELL / 2;
		this.width = half * 2;
		this.tall = 0.8 + size * 0.15;
		this.seed = origin.asLong() * 31 + world.getTime();
	}

	/** For looking at a city's layout outside the game: everything but the world. */
	CityBuildJob(BlockPos origin, int size, long seed) {
		this.world = null;
		this.origin = origin.toImmutable();
		this.size = size;
		this.cells = 2 + size * 2;
		this.half = cells * CELL / 2;
		this.width = half * 2;
		this.tall = 0.8 + size * 0.15;
		this.seed = seed;
	}

	/** The block wanted at a spot of the city, by its offset from the north-west corner and height above the street. */
	public M blockAt(int lx, int lz, int dy) {
		return desired(lx, lz, dy, building(lx / CELL, lz / CELL));
	}

	public int width() {
		return width;
	}

	public static CityBuildJob layout(BlockPos origin, int size, long seed) {
		return new CityBuildJob(origin, size, seed);
	}

	public static void start(ServerWorld world, BlockPos origin, int size) {
		DetonationManager.add(new CityBuildJob(world, origin, MathHelper.clamp(size, 1, 6)));
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return populated;
	}

	private enum Lot {
		ROAD, SIDEWALK, PARK, LOWRISE, TOWER,
		// Oddly shaped buildings on a single lot.
		CYLINDER, TWISTED, INVERTED, HELIX, SPIRE, SETBACK,
		// Landmarks on a merged block of four lots.
		STADIUM, DOME, FERRIS, PYRAMID, TWIN, MEGATOWER;

		boolean mega() {
			return ordinal() >= STADIUM.ordinal();
		}
	}

	private record Building(Lot type, int height, M wall, M glass, M accent) {
	}

	/** How close a lot is to the middle of the city (0 at the centre, 1 at the edge). */
	private double centrality(double ci, double cj) {
		return Math.min(1.0, Math.hypot(ci - (cells - 1) / 2.0, cj - (cells - 1) / 2.0) / (cells / 2.0));
	}

	/** True if the block of four lots whose corner is (gi, gj) is merged into a landmark. */
	private boolean merged(int gi, int gj) {
		if (gi + 1 >= cells || gj + 1 >= cells) {
			return false;
		}
		Random r = Random.create(seed ^ (gi * 912367L + gj * 57719L) ^ 0x5DEECE66DL);
		return r.nextFloat() < 0.24F;
	}

	private Building building(int ci, int cj) {
		int gi = ci / 2 * 2, gj = cj / 2 * 2;
		if (merged(gi, gj)) {
			return landmark(gi, gj);
		}
		Random r = Random.create(seed ^ (ci * 341873128712L + cj * 132897987541L));
		double dc = centrality(ci, cj);
		float roll = r.nextFloat();
		M wall = WALLS[r.nextInt(WALLS.length)];
		M glass = GLASS[r.nextInt(GLASS.length)];
		M accent = r.nextBoolean() ? M.SEA_LANTERN : M.GLOWSTONE;
		int h = (int) ((16 + (1.0 - dc) * 56 * (0.55 + r.nextFloat() * 0.45) + r.nextInt(8)) * tall) / 4 * 4;
		if (roll < 0.10F) {
			return new Building(Lot.PARK, 0, wall, glass, accent);
		}
		if (roll < 0.24F) {
			return new Building(Lot.LOWRISE, 6 + r.nextInt(6), wall, glass, accent);
		}
		if (roll < 0.58F) {
			return new Building(Lot.TOWER, h, wall, glass, accent);
		}
		Lot[] odd = {Lot.CYLINDER, Lot.TWISTED, Lot.INVERTED, Lot.HELIX, Lot.SPIRE, Lot.SETBACK};
		Lot type = odd[r.nextInt(odd.length)];
		if (type == Lot.INVERTED) {
			h = Math.min(h, (int) (44 * tall));
		}
		return new Building(type, Math.max(20, h), wall, glass, accent);
	}

	private Building landmark(int gi, int gj) {
		Random r = Random.create(seed ^ (gi * 77731L + gj * 99991L));
		double dc = centrality(gi + 0.5, gj + 0.5);
		Lot[] types = dc < 0.35 ? new Lot[]{Lot.MEGATOWER, Lot.TWIN, Lot.DOME, Lot.MEGATOWER} : new Lot[]{Lot.STADIUM, Lot.DOME, Lot.FERRIS, Lot.PYRAMID, Lot.TWIN};
		Lot type = types[r.nextInt(types.length)];
		M wall = WALLS[r.nextInt(WALLS.length)];
		M glass = GLASS[r.nextInt(GLASS.length)];
		int h = switch (type) {
			case MEGATOWER -> (int) ((90 + (1 - dc) * 40) * tall);
			case TWIN -> (int) ((56 + (1 - dc) * 40) * tall);
			default -> 24;
		};
		return new Building(type, h / 4 * 4, wall, glass, M.SEA_LANTERN);
	}

	@Override
	public int step(int budget) {
		long total = (long) width * width;
		while (budget > 0 && cursor < total) {
			int lx = (int) (cursor % width);
			int lz = (int) (cursor / width);
			cursor++;
			budget -= buildColumn(lx, lz);
		}
		if (cursor >= total && !populated) {
			populate();
			populated = true;
		}
		return budget;
	}

	private int buildColumn(int lx, int lz) {
		int x = origin.getX() - half + lx;
		int z = origin.getZ() - half + lz;
		int cost = 0;
		if (!world.isChunkLoaded(x >> 4, z >> 4)) {
			// Big cities reach past what is loaded: load it (that costs).
			cost += 300;
		}
		WorldChunk chunk = world.getChunk(x >> 4, z >> 4);
		int y0 = origin.getY();
		int ci = lx / CELL, cj = lz / CELL;
		Building b = building(ci, cj);
		int top = Math.min(world.getTopY() - 1, y0 + Math.max(CLEAR_HEIGHT, b.height + 30));
		int reads = 0;
		for (int y = y0 - 3; y <= top; y++) {
			if (y < world.getBottomY()) {
				continue;
			}
			BlockState want = desired(lx, lz, y - y0, b).state();
			pos.set(x, y, z);
			reads++;
			if (chunk.getBlockState(pos) != want) {
				world.setBlockState(pos, want, FLAGS);
				cost++;
			}
		}
		return cost + 1 + reads / 8;
	}

	private M desired(int lx, int lz, int dy, Building b) {
		if (dy < -1) {
			return M.STONE;
		}
		int mx = lx % CELL, mz = lz % CELL;
		int ci = lx / CELL, cj = lz / CELL;
		boolean mega = b.type.mega();
		int gi = ci / 2 * 2, gj = cj / 2 * 2;
		// Inside a landmark block, the roads between its four lots are built over.
		boolean roadX = mx < ROAD && !(mega && ci == gi + 1);
		boolean roadZ = mz < ROAD && !(mega && cj == gj + 1);
		if (roadX || roadZ) {
			if (dy == -1) {
				boolean centre = roadX && !roadZ ? (mx == 2 || mx == 3) : (!roadX && roadZ && (mz == 2 || mz == 3));
				int along = roadX ? lz : lx;
				return centre && along % 6 < 3 ? M.YELLOW_CONCRETE : M.BLACK_CONCRETE;
			}
			return M.AIR;
		}
		int ix, iz, span;
		if (mega) {
			ix = (ci - gi) * CELL + mx - (ROAD + 1);
			iz = (cj - gj) * CELL + mz - (ROAD + 1);
			span = MEGA;
		} else {
			ix = mx - (ROAD + 1);
			iz = mz - (ROAD + 1);
			span = LOT;
		}
		boolean sidewalk = ix < 0 || iz < 0 || ix >= span || iz >= span;
		if (sidewalk) {
			if (dy == -1) {
				return M.SMOOTH_STONE;
			}
			boolean lamp = (ix == -1 && iz == -1) || (ix == span && iz == span);
			if (lamp && dy >= 0 && dy < 5) {
				return M.IRON_BARS;
			}
			if (lamp && dy == 5) {
				return M.SEA_LANTERN;
			}
			return M.AIR;
		}
		M s = switch (b.type) {
			case PARK -> park(ix, iz, LOT, dy);
			case LOWRISE, TOWER -> tower(ix, iz, LOT, dy, b);
			case CYLINDER -> cylinder(ix, iz, dy, b);
			case TWISTED -> twisted(ix, iz, dy, b);
			case INVERTED -> inverted(ix, iz, dy, b);
			case HELIX -> helix(ix, iz, dy, b);
			case SPIRE -> spire(ix, iz, dy, b);
			case SETBACK -> setback(ix, iz, dy, b, LOT, new double[]{6.5, 5.0, 3.5, 2.0});
			case STADIUM -> stadium(ix, iz, dy);
			case DOME -> dome(ix, iz, dy, b);
			case FERRIS -> ferris(ix, iz, dy);
			case PYRAMID -> pyramid(ix, iz, dy, b);
			case TWIN -> twin(ix, iz, dy, b);
			case MEGATOWER -> setback(ix, iz, dy, b, MEGA, new double[]{16.5, 12.5, 8.5, 5.0});
			default -> M.AIR;
		};
		if (dy == -1 && (s == null || s == M.AIR)) {
			return b.type == Lot.PARK || b.type == Lot.FERRIS ? M.GRASS_BLOCK : M.POLISHED_ANDESITE;
		}
		return s == null ? M.AIR : s;
	}

	// ---------------------------------------------------------------- the ordinary lots

	private M park(int ix, int iz, int size, int dy) {
		if (dy == -1) {
			return (ix + iz) % 7 == 0 ? M.DIRT_PATH : M.GRASS_BLOCK;
		}
		for (int[] t : new int[][]{{3, 3}, {size - 4, 3}, {3, size - 4}, {size - 4, size - 4}, {size / 2, size / 2}}) {
			M tree = tree(ix - t[0], iz - t[1], dy);
			if (tree != null) {
				return tree;
			}
		}
		return M.AIR;
	}

	private static M tree(int dx, int dz, int dy) {
		if (dx == 0 && dz == 0 && dy >= 0 && dy < 5) {
			return M.OAK_LOG;
		}
		int d2 = dx * dx + dz * dz + (dy - 5) * (dy - 5);
		if (dy >= 3 && dy <= 7 && d2 <= 6) {
			return M.OAK_LEAVES;
		}
		return null;
	}

	private M tower(int ix, int iz, int size, int dy, Building b) {
		int min = 1;
		int max = size - 2;
		if (dy == -1) {
			return M.POLISHED_ANDESITE;
		}
		boolean inside = ix >= min && ix <= max && iz >= min && iz <= max;
		int h = b.height;
		if (!inside) {
			return M.AIR;
		}
		boolean edgeX = ix == min || ix == max;
		boolean edgeZ = iz == min || iz == max;
		boolean wall = edgeX || edgeZ;
		int centre = (min + max) / 2;
		if (dy > h) {
			// Rooftop: parapet, antenna, beacon.
			if (dy == h + 1 && wall) {
				return M.STONE_BRICK_WALL;
			}
			if (b.type == Lot.TOWER && ix == centre && iz == centre) {
				if (dy <= h + 7) {
					return M.IRON_BARS;
				}
				if (dy == h + 8) {
					return M.LIT_LAMP;
				}
			}
			if (b.type == Lot.TOWER && dy == h + 1 && ix == centre + 2 && iz == centre - 2) {
				return M.SMOOTH_STONE;
			}
			return M.AIR;
		}
		if (dy == h) {
			return M.SMOOTH_STONE;
		}
		int floor = dy % 4;
		if (!wall) {
			if (floor == 0) {
				return M.SMOOTH_STONE_SLAB;
			}
			if (floor == 3 && (ix + iz) % 5 == 0) {
				return b.accent;
			}
			return M.AIR;
		}
		boolean corner = edgeX && edgeZ;
		if (corner || floor == 0) {
			return b.wall;
		}
		// Doors on the ground floor facing the street.
		int along = edgeX ? iz : ix;
		if (dy < 3 && along == centre && (ix == min || iz == min)) {
			return M.AIR;
		}
		if ((floor == 1 || floor == 2) && along % 3 != 0) {
			return b.glass;
		}
		return b.wall;
	}

	/** The usual facade: a wall band at each floor, windows between, a pier every third block. */
	private static M facade(int dy, int along, Building b) {
		int floor = dy % 4;
		return floor != 0 && Math.floorMod(along, 3) != 0 ? b.glass : b.wall;
	}

	/** Inside the walls: a floor slab every four blocks and a light now and then. */
	private static M interior(int dy, int ix, int iz, Building b) {
		if (dy % 4 == 0) {
			return M.SMOOTH_STONE_SLAB;
		}
		return dy % 4 == 3 && (ix + iz) % 5 == 0 ? b.accent : M.AIR;
	}

	// ---------------------------------------------------------------- the odd ones

	/** A round glass tower with ring floors, capped with a glass dome and a mast. */
	private M cylinder(int ix, int iz, int dy, Building b) {
		double c = (LOT - 1) / 2.0, R = 6.3;
		double dx = ix - c, dz = iz - c, d = Math.hypot(dx, dz);
		int h = b.height;
		if (dy < 0 || d > R) {
			return null;
		}
		if (dy > h) {
			double cap = R * Math.sqrt(Math.max(0, 1 - Math.pow((dy - h) / 5.0, 2)));
			if (d <= cap && d > cap - 1.2) {
				return b.glass;
			}
			if (d < 0.8 && dy <= h + 14) {
				return dy == h + 14 ? M.LIT_LAMP : M.IRON_BARS;
			}
			return null;
		}
		if (d > R - 1.1) {
			int along = (int) Math.round(Math.atan2(dz, dx) * R);
			return dy % 4 == 0 ? b.wall : Math.floorMod(along, 4) == 0 ? b.wall : b.glass;
		}
		return interior(dy, ix, iz, b);
	}

	/** A square tower whose floors turn as it rises, a hundred degrees from foot to crown. */
	private M twisted(int ix, int iz, int dy, Building b) {
		double c = (LOT - 1) / 2.0, hs = 5.2;
		int h = b.height;
		if (dy < 0 || dy > h + 1) {
			return null;
		}
		double th = -dy * (1.75 / Math.max(1, h));
		double dx = ix - c, dz = iz - c;
		double u = dx * Math.cos(th) - dz * Math.sin(th), v = dx * Math.sin(th) + dz * Math.cos(th);
		double m = Math.max(Math.abs(u), Math.abs(v));
		if (m > hs + 0.5) {
			return null;
		}
		if (dy == h + 1) {
			return m > hs - 0.5 ? M.STONE_BRICK_WALL : null;
		}
		if (dy == h) {
			return M.SMOOTH_STONE;
		}
		if (m > hs - 1.1) {
			boolean corner = Math.abs(Math.abs(u) - Math.abs(v)) < 0.9;
			int along = (int) Math.round(Math.abs(u) > Math.abs(v) ? v : u);
			return corner ? b.wall : facade(dy, along, b);
		}
		return interior(dy, ix, iz, b);
	}

	/** An inverted pyramid: a slim foot flaring out floor by floor to a broad roof. Begging to be toppled. */
	private M inverted(int ix, int iz, int dy, Building b) {
		double c = (LOT - 1) / 2.0;
		int h = b.height;
		if (dy < 0 || dy > h + 1) {
			return null;
		}
		double hs = Math.min(6.4, 1.8 + dy * (4.6 / Math.max(1, h * 0.8)));
		double dx = Math.abs(ix - c), dz = Math.abs(iz - c), m = Math.max(dx, dz);
		if (m > hs) {
			return null;
		}
		if (dy == h + 1) {
			return m > hs - 1 ? M.STONE_BRICK_WALL : null;
		}
		if (dy == h || m > hs - 1) {
			return m > hs - 1 && dy % 4 != 0 && dy != h && ((int) (dx + dz)) % 3 != 0 ? b.glass : b.wall;
		}
		return interior(dy, ix, iz, b);
	}

	/** A round core with a band of rooms winding up round it like a staircase. */
	private M helix(int ix, int iz, int dy, Building b) {
		double c = (LOT - 1) / 2.0;
		double dx = ix - c, dz = iz - c, d = Math.hypot(dx, dz);
		int h = b.height;
		if (dy < 0 || dy > h + 4) {
			return null;
		}
		if (d < 2.6) {
			return dy > h ? (d < 1 && dy == h + 4 ? M.SEA_LANTERN : null) : d > 1.6 ? b.wall : interior(dy, ix, iz, b);
		}
		if (d > 6.6 || dy > h) {
			return null;
		}
		double ang = Math.atan2(dz, dx);
		double turn = dy * (MathHelper.TAU / 24.0);
		double diff = MathHelper.wrapDegrees(Math.toDegrees(ang - turn)) / 180.0 * Math.PI;
		if (Math.abs(diff) > 1.1) {
			return null;
		}
		boolean shell = d > 5.6 || Math.abs(diff) > 0.85 || dy % 4 == 0;
		return shell ? (dy % 4 == 0 || Math.abs(diff) > 0.85 ? b.wall : b.glass) : M.AIR;
	}

	/** A TV tower: a slim shaft on three buttresses, a glass pod two-thirds up, and a long mast. */
	private M spire(int ix, int iz, int dy, Building b) {
		double c = (LOT - 1) / 2.0;
		double dx = ix - c, dz = iz - c, d = Math.hypot(dx, dz);
		int h = b.height;
		if (dy < 0) {
			return null;
		}
		// The pod.
		double podY = h * 0.68, pr = 5.6;
		double pd = Math.sqrt(dx * dx + dz * dz + Math.pow((dy - podY) * 1.3, 2));
		if (pd <= pr) {
			if (pd > pr - 1.1) {
				return Math.abs(dy - podY) < 1 ? b.wall : b.glass;
			}
			return Math.abs(dy - podY) < 0.5 ? M.SMOOTH_STONE_SLAB : M.AIR;
		}
		if (d < 1.6 && dy <= h) {
			return b.wall;
		}
		if (d < 0.8 && dy > h && dy <= h + 18) {
			return dy == h + 18 || dy % 6 == 0 ? M.LIT_LAMP : M.IRON_BARS;
		}
		// Three buttresses sloping in to the shaft.
		if (dy < 14) {
			for (int k = 0; k < 3; k++) {
				double a = k * MathHelper.TAU / 3;
				double reach = 6.2 * (1 - dy / 14.0) + 1.2;
				double px = Math.cos(a) * reach, pz = Math.sin(a) * reach;
				if (Math.hypot(dx - px, dz - pz) < 1.0) {
					return b.wall;
				}
			}
		}
		return null;
	}

	/** An art-deco tower stepping back in tiers to a spire, gold at each setback. */
	private M setback(int ix, int iz, int dy, Building b, int span, double[] tiers) {
		double c = (span - 1) / 2.0;
		int h = b.height;
		if (dy < 0) {
			return null;
		}
		double[] at = {0.45, 0.7, 0.88, 1.0};
		double hs = -1;
		int tier = -1;
		for (int k = 0; k < tiers.length; k++) {
			if (dy <= h * at[k]) {
				hs = tiers[k];
				tier = k;
				break;
			}
		}
		double dx = Math.abs(ix - c), dz = Math.abs(iz - c), m = Math.max(dx, dz);
		if (hs < 0) {
			// The spire.
			int spire = h / 5;
			if (dx < 0.6 && dz < 0.6 && dy <= h + spire) {
				return dy == h + spire ? M.LIT_LAMP : M.IRON_BARS;
			}
			if (m < 1.6 && dy <= h + spire / 3) {
				return M.GOLD_BLOCK;
			}
			return null;
		}
		if (m > hs) {
			return null;
		}
		int tierTop = (int) (h * at[tier]);
		if (m > hs - 1) {
			if (dy >= tierTop - 1) {
				return M.GOLD_BLOCK;
			}
			boolean rib = Math.floorMod((int) (dx > dz ? iz : ix), 3) == 0;
			return rib || dy % 4 == 0 ? b.wall : b.glass;
		}
		return dy == tierTop ? M.SMOOTH_STONE : interior(dy, ix, iz, b);
	}

	// ---------------------------------------------------------------- landmarks

	/** An oval stadium: banked stands in two colours round a pitch with white lines, floodlight masts at the corners. */
	private M stadium(int ix, int iz, int dy) {
		double c = (MEGA - 1) / 2.0;
		double ex = (ix - c) / 17.5, ez = (iz - c) / 14.0;
		double e = Math.sqrt(ex * ex + ez * ez);
		if (dy == -1) {
			if (e < 0.5) {
				boolean line = Math.abs(ix - c) < 0.6 || Math.abs(e - 0.18) < 0.03 || Math.abs(Math.abs(ex) - 0.42) < 0.03;
				return line ? M.WHITE_CONCRETE : ((int) (ix / 3)) % 2 == 0 ? M.GRASS_BLOCK : M.MOSS_BLOCK;
			}
			return M.SMOOTH_STONE;
		}
		// Floodlight masts.
		for (int[] p : new int[][]{{2, 2}, {MEGA - 3, 2}, {2, MEGA - 3}, {MEGA - 3, MEGA - 3}}) {
			if (ix == p[0] && iz == p[1]) {
				return dy < 26 ? M.IRON_BARS : dy <= 28 ? M.SEA_LANTERN : null;
			}
		}
		if (e > 1.0 || e < 0.5) {
			return null;
		}
		double stand = (e - 0.5) / 0.42 * 13;
		if (e > 0.92) {
			// The outer wall, with arches.
			if (dy > 14) {
				return null;
			}
			boolean arch = dy < 4 && Math.floorMod((int) Math.round(Math.atan2(ez, ex) * 20), 5) == 0;
			return arch ? M.AIR : dy == 14 ? M.SMOOTH_STONE : M.WHITE_CONCRETE;
		}
		if (dy <= stand) {
			return ((int) stand) % 2 == 0 ? M.RED_CONCRETE : M.WHITE_CONCRETE;
		}
		return null;
	}

	/** A glass dome on a drum, ribbed, with a lantern at its crown. */
	private M dome(int ix, int iz, int dy, Building b) {
		double c = (MEGA - 1) / 2.0, R = 16.5;
		double dx = ix - c, dz = iz - c, d = Math.hypot(dx, dz);
		if (dy < 0) {
			return null;
		}
		int drum = 6;
		if (dy < drum) {
			if (d <= R && d > R - 1.1) {
				int along = (int) Math.round(Math.atan2(dz, dx) * R);
				return dy < 4 && Math.floorMod(along, 8) == 0 ? M.AIR : dy == drum - 1 || Math.floorMod(along, 4) == 0 ? b.wall : b.glass;
			}
			return dy == 0 && d < R ? M.POLISHED_ANDESITE : null;
		}
		double y = dy - drum;
		double r = Math.sqrt(dx * dx + dz * dz + y * y);
		if (r <= R && r > R - 1.2) {
			double lon = Math.atan2(dz, dx), lat = Math.asin(Math.min(1, y / R));
			boolean rib = Math.abs(lon * 8 / Math.PI - Math.round(lon * 8 / Math.PI)) * (R - y * 0.5) < 0.9;
			boolean ring = Math.abs(lat * 10 / Math.PI - Math.round(lat * 10 / Math.PI)) < 0.1;
			return rib || ring ? b.wall : b.glass;
		}
		if (d < 2.2 && y >= R - 1.5 && y <= R + 4) {
			return y > R + 3 ? M.SEA_LANTERN : d < 1.3 ? b.glass : b.wall;
		}
		return null;
	}

	/** A Ferris wheel in a little park: A-frame legs, a hub, spokes, a rim, and coloured gondolas hanging off it. */
	private M ferris(int ix, int iz, int dy) {
		double c = (MEGA - 1) / 2.0, hubY = 21, R = 17;
		if (dy == -1) {
			return (ix + iz) % 9 == 0 ? M.DIRT_PATH : M.GRASS_BLOCK;
		}
		for (int[] t : new int[][]{{3, 3}, {MEGA - 4, 3}, {3, MEGA - 4}, {MEGA - 4, MEGA - 4}, {3, 18}, {MEGA - 4, 18}}) {
			M tree = tree(ix - t[0], iz - t[1], dy);
			if (tree != null) {
				return tree;
			}
		}
		double ox = ix - c, oz = iz - c, oy = dy - hubY;
		double d = Math.hypot(oz, oy);
		// The wheel stands in the plane across the lot.
		if (Math.abs(ox) < 1.0) {
			if (d <= R + 0.6 && d > R - 0.6) {
				return M.WHITE_CONCRETE;
			}
			if (d < 1.6) {
				return M.IRON_BLOCK;
			}
			double deg = Math.floorMod((long) Math.round(Math.toDegrees(Math.atan2(oy, oz)) * 10), 300L) / 10.0;
			double off = Math.toRadians(Math.min(deg, 30 - deg));
			if (d < R && d * Math.sin(off) < 0.6) {
				return M.IRON_BARS;
			}
		}
		// Gondolas hanging below twelve points of the rim.
		if (Math.abs(ox) < 1.6) {
			for (int k = 0; k < 12; k++) {
				double a = k * MathHelper.TAU / 12;
				double gz = Math.cos(a) * R, gy = Math.sin(a) * R - 2.2;
				if (Math.abs(oz - gz) < 1.5 && Math.abs(oy - gy) < 1.2) {
					return GONDOLAS[k % GONDOLAS.length];
				}
			}
		}
		// The A-frame legs, from the hub down to the ground either side.
		if (dy < hubY) {
			double u = dy / hubY;
			for (int s = -1; s <= 1; s += 2) {
				double lx = s * (5 - 3.2 * u);
				for (int f = -1; f <= 1; f += 2) {
					double lz = f * 9 * (1 - u);
					if (Math.abs(ox - lx) < 0.8 && Math.abs(oz - lz) < 0.8) {
						return M.IRON_BLOCK;
					}
				}
			}
		}
		return null;
	}

	/** A glass pyramid with a frame of lines across its faces. */
	private M pyramid(int ix, int iz, int dy, Building b) {
		double c = (MEGA - 1) / 2.0, base = 17, apex = 25;
		double dx = Math.abs(ix - c), dz = Math.abs(iz - c), m = Math.max(dx, dz);
		if (dy < 0 || dy > apex) {
			return null;
		}
		double hs = base * (1 - dy / apex);
		if (m > hs + 0.5) {
			return null;
		}
		if (m > hs - 1.0) {
			boolean frame = Math.floorMod((int) (dx + dz + dy), 4) == 0 || Math.abs(dx - dz) < 0.6;
			return frame ? M.IRON_BLOCK : b.glass;
		}
		return dy == 0 ? M.POLISHED_ANDESITE : null;
	}

	/** Twin towers joined by a two-storey skybridge. */
	private M twin(int ix, int iz, int dy, Building b) {
		double c = (MEGA - 1) / 2.0;
		int h = b.height;
		if (dy < 0) {
			return null;
		}
		for (int s = -1; s <= 1; s += 2) {
			double tx = c + s * 10;
			double dx = Math.abs(ix - tx), dz = Math.abs(iz - c), m = Math.max(dx, dz);
			if (m <= 6.5 && dy <= h) {
				if (m > 5.5) {
					return dy == h ? M.SMOOTH_STONE : facade(dy, (int) (dx > dz ? iz : ix), b);
				}
				return dy == h ? M.SMOOTH_STONE : interior(dy, ix, iz, b);
			}
			if (dx < 0.6 && dz < 0.6 && dy > h && dy <= h + 16) {
				return dy == h + 16 ? M.LIT_LAMP : M.IRON_BARS;
			}
		}
		int bridge = (int) (h * 0.6) / 4 * 4;
		if (Math.abs(ix - c) < 4.5 && Math.abs(iz - c) < 2.5 && dy >= bridge && dy <= bridge + 8) {
			boolean shell = Math.abs(iz - c) > 1.5 || dy == bridge || dy == bridge + 8;
			return shell ? (dy == bridge || dy == bridge + 8 || dy == bridge + 4 ? b.wall : b.glass) : (dy == bridge + 4 ? M.SMOOTH_STONE_SLAB : M.AIR);
		}
		return null;
	}

	private void populate() {
		Random r = Random.create(seed);
		int n = 12 * size;
		for (int i = 0; i < n; i++) {
			int lx = r.nextInt(width);
			int lz = (r.nextInt(width / CELL)) * CELL + 2 + r.nextInt(2);
			if (r.nextBoolean()) {
				int t = lx;
				lx = lz;
				lz = t;
			}
			int x = origin.getX() - half + lx;
			int z = origin.getZ() - half + lz;
			VillagerEntity v = EntityType.VILLAGER.create(world);
			if (v != null) {
				v.refreshPositionAndAngles(x + 0.5, origin.getY(), z + 0.5, r.nextFloat() * 360, 0);
				v.initialize(world, world.getLocalDifficulty(v.getBlockPos()), SpawnReason.EVENT, null);
				world.spawnEntity(v);
			}
		}
	}
}
