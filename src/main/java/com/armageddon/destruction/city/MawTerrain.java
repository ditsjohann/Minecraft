package com.armageddon.destruction.city;

import com.armageddon.event.Kit;
import com.armageddon.util.CityShapes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * WORLD EATER leaves the land bitten: overlapping bowls with sawtooth tooth-mark rims and a few
 * broken teeth left standing, sculk spreading out of every wound like saliva, gnaw grooves between
 * the bites, and in the middle the throat, a spiral pit winding down into the dark.
 */
public final class MawTerrain {
	private MawTerrain() {
	}

	public static void bite(ServerWorld w, BlockQueue q, BlockPos c, double r, double toward, long seed) {
		int teeth = 14;
		int ri = MathHelper.ceil(r * 1.12) + 4;
		int depth = (int) Math.max(5, r * 0.85);
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				double a = Math.atan2(dz, dx);
				double edge = r * CityShapes.toothEdge(a - toward, teeth);
				int x = c.getX() + dx, z = c.getZ() + dz;
				int top = Kit.surface(w, x, z).getY();
				if (d <= edge) {
					// A bowl, steeper at the rim like a bite, not a blast.
					double k = d / edge;
					int cut = (int) Math.round(depth * Math.sqrt(1 - k * k * k));
					for (int y = top + 8; y > top - cut; y--) {
						q.add(x, y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
					double wet = CityShapes.noise(seed, x, 0, z, 3);
					q.add(x, top - cut, z, wet > 0.45 ? Blocks.SCULK.getDefaultState() : Blocks.DEEPSLATE.getDefaultState(), BlockQueue.ANY);
				} else if (d <= edge + 4) {
					// Saliva: sculk soaking outward from the wound.
					double soak = 1 - (d - edge) / 4;
					if (CityShapes.hash(seed, x * 7919 + z) < soak * 0.8) {
						q.add(x, top, z, Blocks.SCULK.getDefaultState(), BlockQueue.SOLID_ONLY);
					}
				}
			}
		}
		// A few broken teeth left behind on the rim, leaning in.
		for (int i = 0; i < 4; i++) {
			double a = toward + Math.PI + (CityShapes.hash(seed, i) - 0.5) * 2.2;
			int x = c.getX() + (int) Math.round(Math.cos(a) * (r - 1)), z = c.getZ() + (int) Math.round(Math.sin(a) * (r - 1));
			int top = Kit.surface(w, x, z).getY();
			int h = 4 + (int) (CityShapes.hash(seed, i + 9) * 5);
			for (int y = 1; y <= h; y++) {
				int lean = (int) Math.round(-y * 0.35);
				int tx = x + (int) Math.round(Math.cos(a) * lean), tz = z + (int) Math.round(Math.sin(a) * lean);
				BlockState s = y == h ? Blocks.BONE_BLOCK.getDefaultState() : (y > h / 2 ? Blocks.CALCITE : Blocks.BONE_BLOCK).getDefaultState();
				q.add(tx, top + y, tz, s, BlockQueue.AIR_ONLY);
				if (y < h - 1) {
					q.add(tx + 1, top + y, tz, s, BlockQueue.AIR_ONLY);
				}
			}
		}
	}

	/** Long shallow grooves between the bites, as if something dragged its teeth across the ground. */
	public static void gnaw(ServerWorld w, BlockQueue q, BlockPos c, double reach, long seed) {
		for (int g = 0; g < 7; g++) {
			double a0 = CityShapes.hash(seed, g) * MathHelper.TAU;
			double rad = reach * (0.35 + 0.6 * CityShapes.hash(seed, g + 20));
			double span = 0.6 + CityShapes.hash(seed, g + 40) * 0.8;
			int steps = (int) (rad * span);
			for (int s = 0; s < steps; s++) {
				double a = a0 + span * s / steps;
				for (int lane = 0; lane < 3; lane++) {
					double rr = rad + (lane - 1) * 2.5;
					int x = c.getX() + (int) Math.round(Math.cos(a) * rr), z = c.getZ() + (int) Math.round(Math.sin(a) * rr);
					int top = Kit.surface(w, x, z).getY();
					q.add(x, top, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					q.add(x, top - 1, z, (lane == 1 ? Blocks.SCULK : Blocks.AIR).getDefaultState(), BlockQueue.NOT_AIR);
				}
			}
		}
	}

	/** The throat: a round pit with a spiral ledge winding down into darkness, slick with sculk. */
	public static void throat(ServerWorld w, BlockQueue q, BlockPos c, int r, int depth, long seed) {
		int bottom = Math.max(w.getBottomY() + 2, c.getY() - depth);
		for (int y = c.getY() + 10; y >= bottom; y--) {
			double ledgeAngle = (c.getY() - y) * 0.22;
			for (int dx = -r - 2; dx <= r + 2; dx++) {
				for (int dz = -r - 2; dz <= r + 2; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					int x = c.getX() + dx, z = c.getZ() + dz;
					if (d <= r) {
						double a = Math.atan2(dz, dx);
						double rel = ((a - ledgeAngle) % MathHelper.TAU + MathHelper.TAU) % MathHelper.TAU;
						boolean ledge = d > r - 4 && rel < 0.55 && y < c.getY();
						q.add(x, y, z, ledge ? Blocks.DEEPSLATE_TILES.getDefaultState() : Blocks.AIR.getDefaultState(), ledge ? BlockQueue.ANY : BlockQueue.NOT_AIR);
					} else if (d <= r + 2 && y < c.getY() + 1) {
						double n = CityShapes.noise(seed + 3, x, y, z, 2.5);
						BlockState s = CityShapes.hash(seed, x * 31 + y * 17 + z) < 0.006 ? Blocks.SCULK_CATALYST.getDefaultState()
								: n > 0.45 ? Blocks.SCULK.getDefaultState() : Blocks.DEEPSLATE.getDefaultState();
						q.add(x, y, z, s, BlockQueue.NOT_AIR);
					}
				}
			}
		}
	}
}
