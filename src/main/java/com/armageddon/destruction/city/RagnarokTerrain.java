package com.armageddon.destruction.city;

import com.armageddon.event.Kit;
import com.armageddon.util.CityShapes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * RAGNAROK splits the world in two. Where Surtr's sword strikes, a chasm opens along the dividing line;
 * on one side everything turns
 * to fire (netherrack, magma, blackstone and flame), on the other to frost (packed and blue ice,
 * snow, ice spikes).
 */
public final class RagnarokTerrain {
	private RagnarokTerrain() {
	}

	/** Ginnungagap: the chasm along the dividing line, walled in obsidian that weeps. */
	public static void chasm(ServerWorld w, BlockQueue q, Vec3d c, Vec3d along, int half, int depth, long seed) {
		Vec3d f = new Vec3d(along.x, 0, along.z).normalize();
		Vec3d side = new Vec3d(-f.z, 0, f.x);
		for (int i = -half; i <= half; i++) {
			double taper = 1 - Math.abs(i) / (double) half;
			int width = (int) Math.round(1 + 3.5 * Math.sqrt(taper));
			int cut = (int) Math.round(depth * Math.sqrt(taper)) + 2;
			for (int s = -width - 1; s <= width + 1; s++) {
				Vec3d p = c.add(f.multiply(i)).add(side.multiply(s));
				int x = (int) Math.floor(p.x), z = (int) Math.floor(p.z);
				int top = Kit.surface(w, x, z).getY();
				if (Math.abs(s) <= width) {
					for (int y = top + 4; y > top - cut; y--) {
						q.add(x, y, z, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					}
				} else {
					for (int y = top; y > top - cut; y--) {
						boolean weep = CityShapes.hash(seed, x * 13 + y * 7 + z) < 0.2;
						q.add(x, y, z, weep ? Blocks.CRYING_OBSIDIAN.getDefaultState() : Blocks.OBSIDIAN.getDefaultState(), BlockQueue.NOT_AIR);
					}
				}
			}
		}
	}

	/** Re-skins one surface column for its half of the world. */
	public static void column(ServerWorld w, BlockQueue q, int x, int z, boolean fire, long seed) {
		BlockPos top = Kit.surface(w, x, z);
		BlockState ground = w.getBlockState(top);
		double n = CityShapes.noise(seed + (fire ? 1 : 2), x, 0, z, 3);
		double h = CityShapes.hash(seed, x * 92821 + z);
		boolean wet = !ground.getFluidState().isEmpty();
		if (fire) {
			if (wet) {
				q.add(top, Blocks.OBSIDIAN.getDefaultState(), BlockQueue.ANY);
				return;
			}
			BlockState s = n > 0.68 ? Blocks.MAGMA_BLOCK.getDefaultState() : n < 0.28 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.NETHERRACK.getDefaultState();
			q.add(top, s, BlockQueue.SOLID_ONLY);
			// Plants burn away; some ground keeps burning.
			q.add(top.up(), h < 0.22 && s.isOf(Blocks.NETHERRACK) ? Blocks.FIRE.getDefaultState() : Blocks.AIR.getDefaultState(), BlockQueue.AIR_ONLY);
		} else {
			if (wet) {
				q.add(top, Blocks.ICE.getDefaultState(), BlockQueue.ANY);
				return;
			}
			BlockState s = n > 0.66 ? Blocks.BLUE_ICE.getDefaultState() : n < 0.35 ? Blocks.SNOW_BLOCK.getDefaultState() : Blocks.PACKED_ICE.getDefaultState();
			q.add(top, s, BlockQueue.SOLID_ONLY);
			// Snow layers can't sit on ice, only on snow.
			q.add(top.up(), h < 0.6 && s.isOf(Blocks.SNOW_BLOCK) ? Blocks.SNOW.getDefaultState() : Blocks.AIR.getDefaultState(), BlockQueue.AIR_ONLY);
		}
	}

	/** A spike of the fire or frost half: a tapering basalt column or ice spire. */
	public static void spire(ServerWorld w, BlockQueue q, int x, int z, boolean fire, int height) {
		int top = Kit.surface(w, x, z).getY();
		for (int y = 1; y <= height; y++) {
			int rad = y < height / 3 ? 1 : 0;
			BlockState s = fire ? (y == height ? Blocks.MAGMA_BLOCK : Blocks.BASALT).getDefaultState()
					: (y > height * 0.7 ? Blocks.ICE : Blocks.PACKED_ICE).getDefaultState();
			for (int dx = -rad; dx <= rad; dx++) {
				for (int dz = -rad; dz <= rad; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= rad) {
						q.add(x + dx, top + y, z + dz, s, BlockQueue.AIR_ONLY);
					}
				}
			}
		}
	}
}
