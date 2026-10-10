package com.armageddon.detonation;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Erases a sphere one spherical shell at a time — outwards (a wall of light) or inwards
 * (implosion) — with an optional geometric pattern mask and a glowing boundary lining.
 * Shells are enumerated analytically per column, so even huge radii need no precomputation.
 */
public class ShellJob implements WorldJob {
	/** Geometric patterns for The Unmaker. */
	public enum Pattern { NONE, HEX, CUBES, SPIRAL, MIXED }

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	private final ServerWorld world;
	private final BlockPos center;
	private final int radius;
	private final boolean inward;
	private final Pattern pattern;
	@Nullable
	private final BlockState boundary;
	private final float boundaryChance;
	private final BlockState fill;
	private int shell;
	private int columnCursor;
	private final ProtectedRegions regions;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();
	private boolean done;

	public ShellJob(ServerWorld world, BlockPos center, int radius, boolean inward, Pattern pattern,
			@Nullable BlockState boundary, float boundaryChance) {
		this(world, center, radius, inward, pattern, boundary, boundaryChance, Blocks.AIR.getDefaultState());
	}

	public ShellJob(ServerWorld world, BlockPos center, int radius, boolean inward, Pattern pattern,
			@Nullable BlockState boundary, float boundaryChance, BlockState fill) {
		this.world = world;
		this.center = center.toImmutable();
		this.radius = Destruction.radius(world, radius);
		this.inward = inward;
		this.pattern = pattern;
		this.boundary = boundary;
		this.boundaryChance = boundaryChance;
		this.fill = fill;
		this.shell = inward ? this.radius : 0;
		this.regions = ProtectedRegions.get(world);
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	/** Current shell radius, for syncing visuals. */
	public int currentShell() {
		return shell;
	}

	@Override
	public int step(int budget) {
		while (budget > 0 && !done) {
			int r = shell;
			int span = r + 1;
			int width = span * 2 + 1;
			int columns = width * width;
			if (columnCursor >= columns) {
				columnCursor = 0;
				if (inward) {
					shell--;
					done = shell < 0;
				} else {
					shell++;
					done = shell > radius;
				}
				continue;
			}
			int dx = columnCursor % width - span;
			int dz = columnCursor / width - span;
			columnCursor++;
			budget--;
			long h2 = (long) dx * dx + (long) dz * dz;
			long outer2 = (long) (r + 1) * (r + 1);
			if (h2 >= outer2) {
				continue;
			}
			int x = center.getX() + dx, z = center.getZ() + dz;
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			// y range where r <= dist < r+1
			double lo = r * (double) r - h2;
			int yLo = lo <= 0 ? 0 : MathHelper.ceil(Math.sqrt(lo));
			int yHi = MathHelper.ceil(Math.sqrt(outer2 - h2)) - 1;
			for (int dy = yLo; dy <= yHi; dy++) {
				budget -= 2;
				process(x, center.getY() + dy, z, dx, dy, dz, r);
				if (dy != 0) {
					process(x, center.getY() - dy, z, dx, -dy, dz, r);
				}
			}
		}
		return budget;
	}

	private void process(int x, int y, int z, int dx, int dy, int dz, int r) {
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return;
		}
		double d = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
		if (d < r || d >= r + 1) {
			return;
		}
		pos.set(x, y, z);
		BlockState s = world.getBlockState(pos);
		if (s.isAir() && fill.isAir() || s.getHardness(world, pos) < 0 || (!regions.isEmpty() && regions.isProtected(pos))) {
			return;
		}
		if (!mask(dx, dy, dz, r)) {
			return;
		}
		boolean edge = r >= radius - 1;
		if (edge && boundary != null) {
			if (!s.isAir() && world.random.nextFloat() < boundaryChance) {
				Destruction.record(world, pos, s);
				world.setBlockState(pos, boundary, FLAGS);
			}
			return;
		}
		Destruction.record(world, pos, s);
		world.setBlockState(pos, fill, FLAGS);
	}

	/** Pattern mask: which blocks of a shell get erased. */
	private boolean mask(int dx, int dy, int dz, int r) {
		Pattern p = pattern;
		if (p == Pattern.MIXED) {
			p = switch ((r / 8) % 3) {
				case 0 -> Pattern.HEX;
				case 1 -> Pattern.CUBES;
				default -> Pattern.SPIRAL;
			};
			// The inner core is always fully erased.
			if (r < radius * 0.35) {
				return true;
			}
		}
		return switch (p) {
			case NONE, MIXED -> true;
			case HEX -> {
				// Hexagonal columns: axial hex grid in XZ with 6-block cells, 1-block walls survive.
				double q = (dx * 0.57735 - dz / 3.0) / 3.5;
				double rr = (dz * 2 / 3.0) / 3.5;
				double fq = q - Math.floor(q), fr = rr - Math.floor(rr);
				yield !(fq < 0.12 || fr < 0.12 || (fq + fr) % 1.0 < 0.12);
			}
			case CUBES -> ((Math.floorDiv(dx, 6) + Math.floorDiv(dy, 6) + Math.floorDiv(dz, 6)) & 1) == 0 || r < radius * 0.5;
			case SPIRAL -> {
				double angle = Math.atan2(dz, dx);
				double band = (angle / (Math.PI * 2) * 6 + r / 5.0 + dy / 9.0);
				yield (band - Math.floor(band)) < 0.62;
			}
		};
	}
}
