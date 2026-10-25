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
 * MOONFALL's lasting mark: the moon itself, sunk into the ground. A pitted shell of dark maria
 * basalt and pale highland stone around a hollow amethyst geode, a ring of upthrust crust where it
 * shoved the land aside, a blanket of grey regolith, and the pockmarks of the fragments that fell
 * with it.
 */
public final class MoonTerrain {
	private MoonTerrain() {
	}

	/** Where the moon's centre ends up: well down in its own crater. */
	public static BlockPos centre(BlockPos ground, int r) {
		return ground.up((int) (r * 0.45));
	}

	public static void moon(ServerWorld w, BlockQueue q, BlockPos ground, int r, long seed) {
		BlockPos c = centre(ground, r);
		int shell = 3;
		// Craters on the moon's own face: shallow dents in the shell radius by direction.
		Vec3d[] craters = new Vec3d[9];
		double[] craterR = new double[craters.length];
		for (int i = 0; i < craters.length; i++) {
			double a = CityShapes.hash(seed, i) * MathHelper.TAU;
			double y = 0.1 + CityShapes.hash(seed, i + 9) * 0.85;
			double h = Math.sqrt(1 - y * y);
			craters[i] = new Vec3d(Math.cos(a) * h, y, Math.sin(a) * h);
			craterR[i] = 0.12 + CityShapes.hash(seed, i + 19) * 0.16;
		}
		int ground0 = ground.getY();
		// Ordered bottom-up so the moon visibly settles into place.
		for (int dy = -r; dy <= r; dy++) {
			int y = c.getY() + dy;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d > r + 0.5) {
						continue;
					}
					double nx = dx / Math.max(d, 1e-3), ny = dy / Math.max(d, 1e-3), nz = dz / Math.max(d, 1e-3);
					double dent = 0;
					for (int i = 0; i < craters.length; i++) {
						double dot = nx * craters[i].x + ny * craters[i].y + nz * craters[i].z;
						double ang = Math.acos(MathHelper.clamp(dot, -1, 1));
						if (ang < craterR[i]) {
							double k = ang / craterR[i];
							dent = Math.max(dent, (1 - k * k) * 3.2 - (k > 0.8 ? (k - 0.8) * 6 : 0));
						}
					}
					double outer = r - dent;
					int x = c.getX() + dx, z = c.getZ() + dz;
					if (d > outer + 0.5) {
						// Above the dented surface but inside the old sphere: clear only what's below ground level.
						if (y <= ground0) {
							q.add(x, y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						}
						continue;
					}
					if (d > outer - shell) {
						q.add(x, y, z, surface(seed, x, y, z, d > outer - 1), BlockQueue.ANY);
					} else if (d > outer - shell - 1.5) {
						// The hollow inside is a geode.
						double n = CityShapes.noise(seed + 7, x, y, z, 3);
						q.add(x, y, z, n > 0.62 ? Blocks.AMETHYST_BLOCK.getDefaultState() : Blocks.CALCITE.getDefaultState(), BlockQueue.ANY);
					} else if (d > outer - shell - 2.5 && ny < -0.2 && CityShapes.noise(seed + 9, x, y, z, 2) > 0.72) {
						q.add(x, y, z, Blocks.AMETHYST_CLUSTER.getDefaultState(), BlockQueue.ANY);
					} else {
						q.add(x, y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
				}
			}
		}
	}

	private static BlockState surface(long seed, int x, int y, int z, boolean skin) {
		double maria = CityShapes.noise(seed + 1, x, y, z, 11);
		double grit = CityShapes.noise(seed + 2, x, y, z, 2.5);
		if (maria > 0.6) {
			return grit > 0.7 ? Blocks.BASALT.getDefaultState() : Blocks.SMOOTH_BASALT.getDefaultState();
		}
		if (!skin) {
			return Blocks.TUFF.getDefaultState();
		}
		if (grit > 0.78) {
			return Blocks.CALCITE.getDefaultState();
		}
		return grit < 0.3 ? Blocks.TUFF.getDefaultState() : Blocks.END_STONE.getDefaultState();
	}

	/** The crust the moon shoved aside: a ring of raised deepslate and tuff around the impact. */
	public static void berm(ServerWorld w, BlockQueue q, BlockPos ground, int r, long seed) {
		int in = r + 1, out = r + 16;
		for (int dx = -out; dx <= out; dx++) {
			for (int dz = -out; dz <= out; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < in || d > out) {
					continue;
				}
				double k = (d - in) / (out - in);
				double wave = 0.6 + 0.4 * CityShapes.noise(seed + 3, dx, 0, dz, 7);
				int h = (int) Math.round(Math.sin(Math.pow(k, 0.7) * Math.PI) * 8 * wave);
				int x = ground.getX() + dx, z = ground.getZ() + dz;
				int top = Kit.surface(w, x, z).getY();
				for (int y = 1; y <= h; y++) {
					BlockState s = y == h ? (CityShapes.hash(seed, x * 31 + z) < 0.5 ? Blocks.GRAVEL.getDefaultState() : Blocks.TUFF.getDefaultState())
							: (y < h / 2 ? Blocks.DEEPSLATE.getDefaultState() : Blocks.COBBLED_DEEPSLATE.getDefaultState());
					q.add(x, top + y, z, s, BlockQueue.AIR_ONLY);
				}
			}
		}
	}

	/** Moon dust settling over everything: the land around goes grey. */
	public static void regolith(ServerWorld w, BlockQueue q, BlockPos ground, int r, long seed) {
		int out = (int) (r * 2.6);
		for (int dx = -out; dx <= out; dx++) {
			for (int dz = -out; dz <= out; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < r + 1 || d > out) {
					continue;
				}
				double keep = 1 - (d - r) / (out - r);
				int x = ground.getX() + dx, z = ground.getZ() + dz;
				if (CityShapes.hash(seed + 4, x * 92821 + z) > keep * 0.95 + 0.05) {
					continue;
				}
				BlockPos top = Kit.surface(w, x, z);
				double n = CityShapes.noise(seed + 5, x, 0, z, 4);
				BlockState s = n > 0.6 ? Blocks.LIGHT_GRAY_CONCRETE_POWDER.getDefaultState()
						: n > 0.35 ? Blocks.GRAY_CONCRETE_POWDER.getDefaultState() : Blocks.GRAVEL.getDefaultState();
				q.add(top, s, BlockQueue.SOLID_ONLY);
			}
		}
	}

	/** A fragment's pockmark: a small bowl with a pale rim. */
	public static void pock(ServerWorld w, BlockQueue q, int x0, int z0, int r, long seed) {
		BlockPos top = Kit.surface(w, x0, z0);
		for (int dx = -r - 2; dx <= r + 2; dx++) {
			for (int dz = -r - 2; dz <= r + 2; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				int x = x0 + dx, z = z0 + dz;
				if (d <= r) {
					int depth = (int) Math.round(Math.sqrt(Math.max(0, r * r - d * d)) * 0.7);
					int ty = Kit.surface(w, x, z).getY();
					for (int y = 0; y < depth; y++) {
						q.add(x, ty - y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
					q.add(x, ty - depth, z, Blocks.TUFF.getDefaultState(), BlockQueue.SOLID_ONLY);
				} else if (d <= r + 2 && CityShapes.hash(seed, x * 13 + z * 7) < 0.7) {
					int ty = Kit.surface(w, x, z).getY();
					q.add(x, ty + 1, z, (d < r + 1 ? Blocks.CALCITE : Blocks.GRAVEL).getDefaultState(), BlockQueue.AIR_ONLY);
				}
			}
		}
	}
}
