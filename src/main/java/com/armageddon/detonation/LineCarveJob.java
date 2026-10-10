package com.armageddon.detonation;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.destruction.TemporaryBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Carves a smooth cylindrical tunnel/trench between two points (beams, lances, drills).
 * Optionally lines the walls with an edge block — permanently, or temporarily so a glowing
 * boundary fades back to the original rock.
 */
public class LineCarveJob implements WorldJob {
	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private final ServerWorld world;
	private final Vec3d start;
	private final Vec3d dir;
	private final double length;
	private final double radius;
	@Nullable
	private final BlockState edge;
	private final float edgeChance;
	private final int edgeTicks;
	private final double step;
	private double t;
	private final Set<Long> done = new HashSet<>();
	private final ProtectedRegions regions;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();

	/**
	 * @param edge       block to line the tunnel walls with (null = none)
	 * @param edgeTicks  if &gt; 0 the edge is temporary and reverts after this many ticks
	 */
	public LineCarveJob(ServerWorld world, Vec3d start, Vec3d end, double radius, @Nullable BlockState edge, float edgeChance, int edgeTicks) {
		this.world = world;
		this.start = start;
		Vec3d d = end.subtract(start);
		this.length = d.length();
		this.dir = length < 1.0E-6 ? new Vec3d(0, -1, 0) : d.multiply(1.0 / length);
		this.radius = Math.min(radius, Destruction.radius(world, (float) radius));
		this.edge = edge;
		this.edgeChance = edgeChance;
		this.edgeTicks = edgeTicks;
		this.step = Math.max(0.5, this.radius * 0.5);
		this.regions = ProtectedRegions.get(world);
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return t > length;
	}

	@Override
	public int step(int budget) {
		int r = MathHelper.ceil(radius + 1);
		while (budget > 0 && t <= length) {
			Vec3d c = start.add(dir.multiply(t));
			t += step;
			int cx = MathHelper.floor(c.x), cy = MathHelper.floor(c.y), cz = MathHelper.floor(c.z);
			if (!world.isChunkLoaded(cx >> 4, cz >> 4)) {
				continue;
			}
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						budget--;
						pos.set(cx + dx, cy + dy, cz + dz);
						if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY()) {
							continue;
						}
						// Distance from the line, not the sphere centre, keeps the bore perfectly cylindrical.
						Vec3d p = Vec3d.ofCenter(pos).subtract(start);
						double along = p.dotProduct(dir);
						if (along < -0.5 || along > length + 0.5) {
							continue;
						}
						double perp = p.subtract(dir.multiply(along)).length();
						long key = pos.asLong();
						if (perp < radius) {
							if (done.add(key)) {
								carve();
							}
						} else if (edge != null && perp < radius + 1.0 && done.add(key ^ 0x5DEECE66DL)) {
							line();
						}
					}
				}
			}
		}
		return budget;
	}

	private void carve() {
		BlockState s = world.getBlockState(pos);
		if (s.isAir() || s.getHardness(world, pos) < 0 || (!regions.isEmpty() && regions.isProtected(pos))) {
			return;
		}
		Destruction.record(world, pos, s);
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), FLAGS);
	}

	private void line() {
		BlockState s = world.getBlockState(pos);
		if (s.isAir() || !s.isOpaqueFullCube(world, pos) || s.getHardness(world, pos) < 0 || world.random.nextFloat() > edgeChance) {
			return;
		}
		if (edgeTicks > 0) {
			TemporaryBlocks.place(world, pos, edge, edgeTicks + world.random.nextInt(Math.max(1, edgeTicks / 2)));
		} else {
			Destruction.set(world, pos.toImmutable(), edge, Block.NOTIFY_LISTENERS);
		}
	}
}
