package com.armageddon.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Instant-hit ray casting for beam weapons. */
public final class Hitscan {
	public record Result(Vec3d end, @Nullable Entity entity, @Nullable BlockHitResult block) {
	}

	/** First entity or block along the ray. */
	public static Result cast(World world, Entity shooter, Vec3d start, Vec3d dir, double range) {
		Vec3d end = start.add(dir.multiply(range));
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, shooter));
		Vec3d stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getPos();
		Entity best = null;
		Vec3d bestHit = null;
		double bestDist = Double.MAX_VALUE;
		for (Entity e : world.getOtherEntities(shooter, new Box(start, stop).expand(1.0), Hitscan::targetable)) {
			Optional<Vec3d> hit = e.getBoundingBox().expand(0.3).raycast(start, stop);
			if (hit.isPresent()) {
				double d = start.squaredDistanceTo(hit.get());
				if (d < bestDist) {
					bestDist = d;
					best = e;
					bestHit = hit.get();
				}
			}
		}
		if (best != null) {
			return new Result(bestHit, best, null);
		}
		return new Result(stop, null, blockHit.getType() == HitResult.Type.MISS ? null : blockHit);
	}

	/** Every entity the segment passes through, nearest first. */
	public static List<Entity> pierce(World world, Entity shooter, Vec3d start, Vec3d end, double margin) {
		List<Entity> out = new ArrayList<>();
		for (Entity e : world.getOtherEntities(shooter, new Box(start, end).expand(margin + 1.0), Hitscan::targetable)) {
			if (e.getBoundingBox().expand(margin).raycast(start, end).isPresent() || e.getBoundingBox().expand(margin).contains(start)) {
				out.add(e);
			}
		}
		out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(start)));
		return out;
	}

	public static boolean targetable(Entity e) {
		return e.isAlive() && !e.isSpectator() && (e.canHit() || e instanceof LivingEntity);
	}

	/** Rotates a direction by a random cone (degrees). */
	public static Vec3d spread(Vec3d dir, double degrees, net.minecraft.util.math.random.Random random) {
		double s = Math.toRadians(degrees);
		return dir.add(random.nextGaussian() * s, random.nextGaussian() * s, random.nextGaussian() * s).normalize();
	}

	private Hitscan() {
	}
}
