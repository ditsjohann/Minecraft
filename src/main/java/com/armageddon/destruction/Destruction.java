package com.armageddon.destruction;

import com.armageddon.registry.ModGameRules;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Single choke point for every terrain edit the mod makes. Enforces protected regions and the
 * block-damage game rule, clamps radii to the configured maximum, and (optionally) records a
 * journal so {@code /armageddon restore} can undo destruction.
 */
public final class Destruction {
	private static final int JOURNAL_LIMIT = 4_000_000;

	private record Entry(ServerWorld world, long pos, BlockState original) {
	}

	private static final Deque<Entry> JOURNAL = new ArrayDeque<>();

	public static boolean enabled(ServerWorld world) {
		return world.getGameRules().getBoolean(ModGameRules.BLOCK_DAMAGE);
	}

	/** Clamps a requested destruction radius to the world's {@code armageddonMaxRadius} game rule. */
	public static float radius(ServerWorld world, float requested) {
		int max = world.getGameRules().getInt(ModGameRules.MAX_RADIUS);
		return Math.min(requested, max);
	}

	public static int radius(ServerWorld world, int requested) {
		return MathHelper.floor(radius(world, (float) requested));
	}

	public static boolean isProtected(ServerWorld world, BlockPos pos) {
		ProtectedRegions regions = ProtectedRegions.get(world);
		return regions.isProtected(pos);
	}

	/** Sets a block unless protected or disabled. Records the original for restoration. */
	public static boolean set(ServerWorld world, BlockPos pos, BlockState state, int flags) {
		if (!enabled(world) || isProtected(world, pos)) {
			return false;
		}
		BlockState old = world.getBlockState(pos);
		if (old == state) {
			return false;
		}
		if (old.getHardness(world, pos) < 0) {
			return false;
		}
		record(world, pos, old);
		return world.setBlockState(pos, state, flags);
	}

	/** Records the original state of a position (used by jobs that write blocks directly for speed). */
	public static void record(ServerWorld world, BlockPos pos, BlockState original) {
		if (!world.getGameRules().getBoolean(ModGameRules.RECORD_RESTORE)) {
			return;
		}
		if (JOURNAL.size() >= JOURNAL_LIMIT) {
			JOURNAL.pollFirst();
		}
		JOURNAL.addLast(new Entry(world, pos.asLong(), original));
	}

	/** Restores up to {@code max} recorded edits, newest first. Returns how many were restored. */
	public static int restore(int max) {
		int n = 0;
		BlockPos.Mutable p = new BlockPos.Mutable();
		while (n < max && !JOURNAL.isEmpty()) {
			Entry e = JOURNAL.pollLast();
			p.set(e.pos);
			e.world.setBlockState(p, e.original, net.minecraft.block.Block.NOTIFY_LISTENERS);
			n++;
		}
		return n;
	}

	public static int journalSize() {
		return JOURNAL.size();
	}

	/** Vanilla-style explosion that respects protected regions and the block-damage rule. */
	public static void explode(ServerWorld world, @Nullable Entity source, double x, double y, double z, float power, boolean fire) {
		boolean grief = enabled(world);
		world.createExplosion(source, null, grief ? BEHAVIOR : null, x, y, z, power, fire && grief,
				grief ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
	}

	private static final ExplosionBehavior BEHAVIOR = new ExplosionBehavior() {
		@Override
		public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
			if (world instanceof ServerWorld server && isProtected(server, pos)) {
				return false;
			}
			if (world instanceof ServerWorld server) {
				record(server, pos, state);
			}
			return true;
		}
	};

	private Destruction() {
	}
}
