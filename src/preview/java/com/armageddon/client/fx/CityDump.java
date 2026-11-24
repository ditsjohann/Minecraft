package com.armageddon.client.fx;

import com.armageddon.world.CityBuildJob;
import net.minecraft.util.math.BlockPos;

import java.io.PrintWriter;

/** Writes a test city's blocks out as a voxel list, for looking at the layout outside the game. */
public final class CityDump {
	public static void main(String[] args) throws Exception {
		int size = Integer.parseInt(args[1]);
		long seed = Long.parseLong(args[2]);
		CityBuildJob city = CityBuildJob.layout(BlockPos.ORIGIN, size, seed);
		int w = city.width();
		try (PrintWriter out = new PrintWriter(args[0])) {
			for (int x = 0; x < w; x++) {
				for (int z = 0; z < w; z++) {
					for (int y = -1; y < 200; y++) {
						CityBuildJob.M s = city.blockAt(x, z, y);
						if (s == CityBuildJob.M.AIR) {
							continue;
						}
						// Only what could be seen: skip blocks buried on every side.
						boolean buried = y >= 0 && x > 0 && z > 0 && x < w - 1 && z < w - 1 && solid(city, x, z, y + 1) && solid(city, x + 1, z, y)
								&& solid(city, x - 1, z, y) && solid(city, x, z + 1, y) && solid(city, x, z - 1, y);
						if (!buried) {
							out.println(x + " " + y + " " + z + " " + s.name().toLowerCase());
						}
					}
				}
			}
		}
	}

	private static boolean solid(CityBuildJob city, int x, int z, int y) {
		return city.blockAt(x, z, y) != CityBuildJob.M.AIR;
	}
}
