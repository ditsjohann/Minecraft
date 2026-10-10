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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Procedurally builds a test city: a grid of roads with lane markings and street lights,
 * skyscrapers with window grids and rooftop antennas (taller towards the centre), low-rise
 * blocks and parks — then populates the streets with villagers. Built incrementally.
 */
public class CityBuildJob implements WorldJob {
	private static final int CELL = 22;
	private static final int ROAD = 6;
	private static final int CLEAR_HEIGHT = 48;
	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private static final BlockState[] WALLS = {
			Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState(),
			Blocks.QUARTZ_BLOCK.getDefaultState(), Blocks.CYAN_TERRACOTTA.getDefaultState(), Blocks.BRICKS.getDefaultState(),
			Blocks.POLISHED_ANDESITE.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState()};
	private static final BlockState[] GLASS = {
			Blocks.GLASS.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), Blocks.CYAN_STAINED_GLASS.getDefaultState(),
			Blocks.BLACK_STAINED_GLASS.getDefaultState(), Blocks.GRAY_STAINED_GLASS.getDefaultState()};

	private final ServerWorld world;
	private final BlockPos origin;
	private final int half;
	private final int width;
	private final long seed;
	private long cursor;
	private boolean populated;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	private CityBuildJob(ServerWorld world, BlockPos origin, int size) {
		this.world = world;
		this.origin = origin.toImmutable();
		int cells = 2 + size * 2;
		this.half = cells * CELL / 2;
		this.width = half * 2;
		this.seed = origin.asLong() * 31 + world.getTime();
	}

	public static void start(ServerWorld world, BlockPos origin, int size) {
		DetonationManager.add(new CityBuildJob(world, origin, MathHelper.clamp(size, 1, 4)));
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return populated;
	}

	private enum Lot { ROAD, SIDEWALK, PARK, LOWRISE, TOWER }

	private record Building(Lot type, int height, BlockState wall, BlockState glass, BlockState accent) {
	}

	private Building building(int ci, int cj) {
		Random r = Random.create(seed ^ (ci * 341873128712L + cj * 132897987541L));
		int cells = width / CELL;
		double dc = Math.hypot(ci - (cells - 1) / 2.0, cj - (cells - 1) / 2.0) / (cells / 2.0);
		float roll = r.nextFloat();
		BlockState wall = WALLS[r.nextInt(WALLS.length)];
		BlockState glass = GLASS[r.nextInt(GLASS.length)];
		BlockState accent = r.nextBoolean() ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.GLOWSTONE.getDefaultState();
		if (roll < 0.12F) {
			return new Building(Lot.PARK, 0, wall, glass, accent);
		}
		if (roll < 0.30F) {
			return new Building(Lot.LOWRISE, 6 + r.nextInt(6), wall, glass, accent);
		}
		int h = 16 + (int) ((1.0 - Math.min(1.0, dc)) * 56 * (0.55 + r.nextFloat() * 0.45)) + r.nextInt(8);
		return new Building(Lot.TOWER, h / 4 * 4, wall, glass, accent);
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
		if (!world.isChunkLoaded(x >> 4, z >> 4)) {
			return 1;
		}
		WorldChunk chunk = world.getChunk(x >> 4, z >> 4);
		int y0 = origin.getY();
		int mx = lx % CELL;
		int mz = lz % CELL;
		int ci = lx / CELL;
		int cj = lz / CELL;
		boolean roadX = mx < ROAD;
		boolean roadZ = mz < ROAD;
		boolean road = roadX || roadZ;
		boolean sidewalk = !road && (mx == ROAD || mz == ROAD || mx == CELL - 1 || mz == CELL - 1);
		Building b = building(ci, cj);

		int top = y0 + Math.max(CLEAR_HEIGHT, b.height + 10);
		int work = 0;
		for (int y = y0 - 3; y <= top && y < world.getTopY(); y++) {
			if (y < world.getBottomY()) {
				continue;
			}
			BlockState want = desired(lx, lz, mx, mz, y - y0, road, roadX, roadZ, sidewalk, b);
			pos.set(x, y, z);
			if (chunk.getBlockState(pos) != want) {
				world.setBlockState(pos, want, FLAGS);
			}
			work++;
		}
		return work;
	}

	private BlockState desired(int lx, int lz, int mx, int mz, int dy, boolean road, boolean roadX, boolean roadZ, boolean sidewalk, Building b) {
		if (dy < -1) {
			return Blocks.STONE.getDefaultState();
		}
		if (road) {
			if (dy == -1) {
				boolean centre = roadX && !roadZ ? (mx == 2 || mx == 3) : (!roadX && roadZ && (mz == 2 || mz == 3));
				int along = roadX ? lz : lx;
				if (centre && along % 6 < 3) {
					return Blocks.YELLOW_CONCRETE.getDefaultState();
				}
				return Blocks.BLACK_CONCRETE.getDefaultState();
			}
			return Blocks.AIR.getDefaultState();
		}
		if (sidewalk) {
			if (dy == -1) {
				return Blocks.SMOOTH_STONE.getDefaultState();
			}
			boolean lamp = (mx == ROAD && mz == ROAD) || (mx == CELL - 1 && mz == CELL - 1);
			if (lamp && dy >= 0 && dy < 5) {
				return Blocks.IRON_BARS.getDefaultState();
			}
			if (lamp && dy == 5) {
				return Blocks.SEA_LANTERN.getDefaultState();
			}
			return Blocks.AIR.getDefaultState();
		}
		// Lot interior: [ROAD+1, CELL-2]
		int ix = mx - (ROAD + 1);
		int iz = mz - (ROAD + 1);
		int size = CELL - ROAD - 2;
		return switch (b.type) {
			case PARK -> park(ix, iz, size, dy);
			case LOWRISE, TOWER -> tower(ix, iz, size, dy, b);
			default -> Blocks.AIR.getDefaultState();
		};
	}

	private BlockState park(int ix, int iz, int size, int dy) {
		if (dy == -1) {
			return (ix + iz) % 7 == 0 ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
		}
		for (int[] t : new int[][]{{3, 3}, {size - 4, 3}, {3, size - 4}, {size - 4, size - 4}, {size / 2, size / 2}}) {
			int dx = ix - t[0];
			int dz = iz - t[1];
			if (dx == 0 && dz == 0 && dy >= 0 && dy < 5) {
				return Blocks.OAK_LOG.getDefaultState();
			}
			int d2 = dx * dx + dz * dz + (dy - 5) * (dy - 5);
			if (dy >= 3 && dy <= 7 && d2 <= 6) {
				return Blocks.OAK_LEAVES.getDefaultState().with(net.minecraft.state.property.Properties.PERSISTENT, true);
			}
		}
		return Blocks.AIR.getDefaultState();
	}

	private BlockState tower(int ix, int iz, int size, int dy, Building b) {
		int min = 1;
		int max = size - 2;
		if (dy == -1) {
			return Blocks.POLISHED_ANDESITE.getDefaultState();
		}
		boolean inside = ix >= min && ix <= max && iz >= min && iz <= max;
		int h = b.height;
		if (!inside) {
			return Blocks.AIR.getDefaultState();
		}
		boolean edgeX = ix == min || ix == max;
		boolean edgeZ = iz == min || iz == max;
		boolean wall = edgeX || edgeZ;
		int centre = (min + max) / 2;
		if (dy > h) {
			// Rooftop: parapet, antenna, beacon.
			if (dy == h + 1 && wall) {
				return Blocks.STONE_BRICK_WALL.getDefaultState();
			}
			if (b.type == Lot.TOWER && ix == centre && iz == centre) {
				if (dy <= h + 7) {
					return Blocks.IRON_BARS.getDefaultState();
				}
				if (dy == h + 8) {
					return Blocks.REDSTONE_LAMP.getDefaultState().with(net.minecraft.state.property.Properties.LIT, true);
				}
			}
			if (b.type == Lot.TOWER && dy == h + 1 && ix == centre + 2 && iz == centre - 2) {
				return Blocks.SMOOTH_STONE.getDefaultState();
			}
			return Blocks.AIR.getDefaultState();
		}
		if (dy == h) {
			return Blocks.SMOOTH_STONE.getDefaultState();
		}
		int floor = dy % 4;
		if (!wall) {
			if (floor == 0) {
				return Blocks.SMOOTH_STONE_SLAB.getDefaultState();
			}
			if (floor == 3 && (ix + iz) % 5 == 0) {
				return b.accent;
			}
			return Blocks.AIR.getDefaultState();
		}
		boolean corner = edgeX && edgeZ;
		if (corner || floor == 0) {
			return b.wall;
		}
		// Doors on the ground floor facing the street.
		int along = edgeX ? iz : ix;
		if (dy < 3 && along == centre && (ix == min || iz == min)) {
			return Blocks.AIR.getDefaultState();
		}
		if ((floor == 1 || floor == 2) && along % 3 != 0) {
			return b.glass;
		}
		return b.wall;
	}

	private void populate() {
		Random r = Random.create(seed);
		for (int i = 0; i < 24; i++) {
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
