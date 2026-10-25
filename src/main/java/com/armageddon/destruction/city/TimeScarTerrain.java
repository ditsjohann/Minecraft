package com.armageddon.destruction.city;

import com.armageddon.event.Kit;
import com.armageddon.util.CityShapes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * CHRONO BREAK puts the world back, almost. Time scars mark where the rewind slipped: concentric
 * bands of ground left at different ages (overgrown future, cracked past, sand of deep time,
 * time-frozen calcite), twelve copper hour stones each oxidised to a different hour, and a
 * sundial at the centre whose gnomon shadow points at the moment it happened.
 */
public final class TimeScarTerrain {
	private TimeScarTerrain() {
	}

	private static BlockState era(int ring, double n) {
		return switch (Math.floorMod(ring, 4)) {
			case 0 -> n > 0.5 ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.MOSSY_COBBLESTONE.getDefaultState();
			case 1 -> n > 0.5 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.MOSSY_STONE_BRICKS.getDefaultState();
			case 2 -> n > 0.55 ? Blocks.SMOOTH_SANDSTONE.getDefaultState() : Blocks.SANDSTONE.getDefaultState();
			default -> n > 0.6 ? Blocks.CALCITE.getDefaultState() : Blocks.DIORITE.getDefaultState();
		};
	}

	/** The dial face: era bands every few blocks out to {@code r}, written onto the surface. */
	public static void bands(ServerWorld w, BlockQueue q, BlockPos c, int r, long seed) {
		int band = 6;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < 5 || d > r) {
					continue;
				}
				// Bands are 3 wide with untouched ground between: rings, not a solid disc.
				double inBand = d % band;
				if (inBand > 3) {
					continue;
				}
				int ring = (int) (d / band);
				int x = c.getX() + dx, z = c.getZ() + dz;
				BlockPos top = Kit.surface(w, x, z);
				double n = CityShapes.noise(seed + ring, x, 0, z, 2);
				q.add(top, era(ring, n), BlockQueue.SOLID_ONLY);
				if (Math.floorMod(ring, 4) == 0 && n > 0.75) {
					q.add(top.up(), Blocks.MOSS_CARPET.getDefaultState(), BlockQueue.AIR_ONLY);
				}
			}
		}
	}

	/** Twelve copper hour stones; each has aged by a different amount of time. */
	public static void hours(ServerWorld w, BlockQueue q, BlockPos c, int r, Vec3d heading) {
		double a0 = Math.atan2(heading.z, heading.x);
		BlockState[] age = {Blocks.COPPER_BLOCK.getDefaultState(), Blocks.EXPOSED_COPPER.getDefaultState(),
				Blocks.WEATHERED_COPPER.getDefaultState(), Blocks.OXIDIZED_COPPER.getDefaultState()};
		for (int i = 0; i < 12; i++) {
			double a = a0 + i * MathHelper.TAU / 12;
			int x = c.getX() + (int) Math.round(Math.cos(a) * r), z = c.getZ() + (int) Math.round(Math.sin(a) * r);
			int top = Kit.surface(w, x, z).getY();
			int h = i % 3 == 0 ? 5 : 3;
			for (int y = 1; y <= h; y++) {
				q.add(x, top + y, z, age[(i + y / 2) % 4], BlockQueue.AIR_ONLY);
			}
			q.add(x, top + h + 1, z, Blocks.LIGHTNING_ROD.getDefaultState(), BlockQueue.AIR_ONLY);
		}
	}

	/** A quartz sundial at the centre, its shadow cast in blackstone toward twelve o'clock. */
	public static void sundial(ServerWorld w, BlockQueue q, BlockPos c, int r, Vec3d heading) {
		int top = Kit.surface(w, c.getX(), c.getZ()).getY();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx * dx + dz * dz <= 5) {
					q.add(c.getX() + dx, top, c.getZ() + dz, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState(), BlockQueue.ANY);
				}
			}
		}
		for (int y = 1; y <= 7; y++) {
			q.add(c.getX(), top + y, c.getZ(), Blocks.QUARTZ_PILLAR.getDefaultState(), BlockQueue.AIR_ONLY);
		}
		q.add(c.getX(), top + 8, c.getZ(), Blocks.END_ROD.getDefaultState(), BlockQueue.AIR_ONLY);
		Vec3d f = new Vec3d(heading.x, 0, heading.z).normalize();
		int len = (int) (r * 0.55);
		for (int i = 3; i < len; i++) {
			int x = c.getX() + (int) Math.round(f.x * i), z = c.getZ() + (int) Math.round(f.z * i);
			q.add(Kit.surface(w, x, z), Blocks.POLISHED_BLACKSTONE.getDefaultState(), BlockQueue.SOLID_ONLY);
		}
	}
}
