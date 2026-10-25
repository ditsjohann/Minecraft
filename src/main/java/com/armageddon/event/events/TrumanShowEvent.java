package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.TrumanSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * THE TRUMAN SHOW. The client plays the cutscene: a live broadcast, a studio light falling out of a
 * painted sky, the door in the sky, the studio outside, and the order to cut the transmission. On the
 * same timeline the server strikes the set for real: a crater where the light lands, the ground
 * peeled away into a taped studio floor, spotlights that scorch it, and a wall of painted sky with a
 * staircase up to an EXIT door.
 */
public class TrumanShowEvent extends WorldEvent {
	private static final int CINE = TrumanSet.CINE;
	/** Block edits per tick, shared by the strike and the wall. */
	private static final int BUDGET = 2500;

	private record Op(long pos, BlockState state) {
	}

	private final Deque<Op> queue = new ArrayDeque<>();
	private final Map<FallingBlockEntity, Integer> flying = new HashMap<>();
	private Vec3d fwd, right;
	private int floorY, strikeR, wallR;
	private boolean wallQueued;

	@Override
	public boolean tick() {
		int t = age();
		Vec3d g = center();
		if (t == 0) {
			fwd = Kit.dir(data.getFloat("Yaw"));
			right = new Vec3d(-fwd.z, 0, fwd.x);
			floorY = MathHelper.floor(g.y) - 1;
			strikeR = (int) Destruction.radius(world, (float) TrumanSet.STRIKE_R);
			wallR = (int) Destruction.radius(world, (float) TrumanSet.WALL_R);
			WorldCutsceneEvents.begin(this, world, g, "block.armageddon.truman_show", 6, fwd, CINE);
			world.spawnParticles(ParticleTypes.NOTE, g.x, g.y + 2, g.z, 6, 1.5, 0.5, 1.5, 0.5);
		}
		// The cheerful three-note "good morning" chime of the opening broadcast.
		if (t == 20 || t == 26 || t == 32) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 4.0F, t == 20 ? 1.0F : t == 26 ? 1.26F : 1.5F);
		}
		if (t == TrumanSet.LIGHT_FALL) {
			Kit.sound(world, g, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 32.0F, 0.4F);
		}
		if (t == TrumanSet.LIGHT_HIT) {
			lightCrash(TrumanSet.crash(g, fwd));
		}
		if (t == TrumanSet.DOOR_OPEN) {
			Kit.sound(world, g, SoundEvents.BLOCK_IRON_DOOR_OPEN, 16.0F, 0.6F);
		}
		if (t == TrumanSet.CUT - 10) {
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.5F);
		}
		// Colour bars and the test tone.
		if (t >= TrumanSet.CUT && t < TrumanSet.BARS_END && t % 4 == 0) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 3.0F, 1.4F);
		}
		// "CHUNK": the big studio lights switch on one by one.
		for (int i = 0; i < TrumanSet.LAMPS; i++) {
			if (t == TrumanSet.lampOn(i)) {
				Vec3d l = TrumanSet.lamp(g, fwd, i);
				Kit.sound(world, l, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.45F);
				Kit.sound(world, l, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
				Vfx.shake(world, g, 300, 0.6F);
			}
		}
		if (t >= TrumanSet.STRIKE && t < TrumanSet.STRIKE_END) {
			strike(g, t);
			if (t % 10 == 0) {
				Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 32.0F, 0.4F);
				Vfx.shake(world, g, 300, 0.5F);
			}
		}
		if (t >= TrumanSet.SCORCH && t < TrumanSet.SCORCH_END) {
			scorch(g, t);
		}
		if (t == CINE - 4) {
			// The broken rig, hanging lamps and their beams stay visible in the world afterwards.
			Vfx.custom(world, FxType.TRUMAN_SET, g, fwd, (float) TrumanSet.DOME_R, 0xFFF0D0, 0, 0, 0,
					TrumanSet.SCORCH_END - (CINE - 4) + 60, 1024);
		}
		if (t == TrumanSet.WALL && !wallQueued) {
			wallQueued = true;
			queueWall(g);
		}
		flying.entrySet().removeIf(en -> {
			if (!en.getKey().isAlive() || t - en.getValue() > 55) {
				en.getKey().discard();
				return true;
			}
			return false;
		});
		drain();
		boolean done = t > TrumanSet.SCORCH_END && queue.isEmpty();
		return done || t > TrumanSet.SCORCH_END + 1200;
	}

	@Override
	public void finish() {
		for (FallingBlockEntity fb : flying.keySet()) {
			fb.discard();
		}
		flying.clear();
	}

	// ------------------------------------------------------------------ the falling light

	private void lightCrash(Vec3d l) {
		Vec3d p = Kit.ground(world, l.x, l.z);
		Vfx.layeredBlast(world, p, 4.0F, 0xFFF0C0);
		Vfx.flash(world, p, 0.35F, 120, 0xFFF4E0);
		Vfx.shake(world, p, 160, 1.6F);
		Vfx.ring(world, p.add(0, 0.3, 0), 18.0F, 1.5F, 0xFFE8C0, true, 30);
		Kit.sound(world, p, SoundEvents.BLOCK_GLASS_BREAK, 24.0F, 0.5F);
		Kit.sound(world, p, SoundEvents.BLOCK_ANVIL_LAND, 24.0F, 0.6F);
		Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 16.0F, 1.1F);
		Kit.damage(world, p, 7, 14.0F, ModDamageTypes.SHRAPNEL, null, owner());
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 40, 2, 1, 2, 0.05);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1, p.z, 60, 2, 1, 2, 0.4);
		if (!Destruction.enabled(world)) {
			return;
		}
		BlockPos c = BlockPos.ofFloored(p).down();
		Kit.sphere(world, c, 3.6, Blocks.AIR.getDefaultState());
		// The wreck: a black lamp housing on its side, its shattered lens still glowing, chain trailing.
		BlockPos base = c.down(2);
		Direction along = Direction.getFacing(right.x, 0, right.z);
		Direction face = Direction.getFacing(fwd.x, 0, fwd.z);
		for (int k = -2; k <= 2; k++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int ds = -1; ds <= 1; ds++) {
					BlockPos q = base.offset(along, k).up(dy + 1).offset(face, ds);
					boolean shell = Math.abs(dy) == 1 || Math.abs(ds) == 1;
					BlockState s = k == 2 ? (shell ? Blocks.WHITE_STAINED_GLASS.getDefaultState() : Blocks.SEA_LANTERN.getDefaultState())
							: shell ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.IRON_BLOCK.getDefaultState();
					Destruction.set(world, q, s, Kit.FLAGS);
				}
			}
		}
		for (int k = -2; k <= 2; k += 2) {
			Destruction.set(world, base.offset(along, k).up(3), Blocks.IRON_BARS.getDefaultState(), Kit.FLAGS);
		}
		for (int k = 0; k < 4; k++) {
			Destruction.set(world, base.offset(along, -3).up(1 + k), Blocks.CHAIN.getDefaultState(), Kit.FLAGS);
		}
		for (int i = 0; i < 6; i++) {
			BlockPos f = c.add(world.random.nextInt(9) - 4, 2, world.random.nextInt(9) - 4);
			if (world.getBlockState(f).isAir() && !world.getBlockState(f.down()).isAir()) {
				Destruction.set(world, f, Blocks.FIRE.getDefaultState(), Kit.FLAGS);
			}
		}
	}

	// ------------------------------------------------------------------ striking the set

	private BlockState floorAt(int x, int z, Vec3d g) {
		int dx = x - MathHelper.floor(g.x), dz = z - MathHelper.floor(g.z);
		if (Math.floorMod(dx, 12) == 0 || Math.floorMod(dz, 12) == 0) {
			return Blocks.YELLOW_CONCRETE.getDefaultState();
		}
		if (Math.floorMod(dx, 12) == 6 && Math.floorMod(dz, 12) == 6) {
			// Spike marks: where the props used to stand.
			return Blocks.WHITE_CONCRETE.getDefaultState();
		}
		return ((Math.floorDiv(dx, 6) + Math.floorDiv(dz, 6)) & 1) == 0 ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
	}

	/** One ring of the strike: everything above the floor goes, the floor becomes taped studio concrete. */
	private void strike(Vec3d g, int t) {
		if (!Destruction.enabled(world) || strikeR < 4) {
			return;
		}
		double span = TrumanSet.STRIKE_END - TrumanSet.STRIKE;
		double rOut = strikeR * (1 - (t - TrumanSet.STRIKE) / span);
		double rIn = strikeR * (1 - (t + 1 - TrumanSet.STRIKE) / span);
		int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z);
		int ri = MathHelper.ceil(rOut);
		int liftBudget = Math.min(14, Kit.debrisBudget(world, g));
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d >= rOut || d < rIn) {
					continue;
				}
				int x = gx + dx, z = gz + dz;
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
				// A few surface blocks are hauled up into the rigging for the camera.
				if (liftBudget > 0 && top > floorY && world.random.nextInt(5) == 0) {
					p.set(x, top, z);
					BlockState s = world.getBlockState(p);
					if (Kit.liftable(world, p, s, ProtectedRegions.get(world))) {
						Destruction.record(world, p.toImmutable(), s);
						FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
						fb.setNoGravity(true);
						fb.setDestroyedOnLanding();
						fb.dropItem = false;
						Kit.velocity(fb, new Vec3d(world.random.nextGaussian() * 0.08, 0.8 + world.random.nextDouble() * 0.6, world.random.nextGaussian() * 0.08));
						flying.put(fb, t);
						liftBudget--;
					}
				}
				for (int y = Math.min(top, floorY + 48); y > floorY; y--) {
					queue.add(new Op(BlockPos.asLong(x, y, z), Blocks.AIR.getDefaultState()));
				}
				BlockState floor = floorAt(x, z, g);
				queue.add(new Op(BlockPos.asLong(x, floorY, z), floor));
				queue.add(new Op(BlockPos.asLong(x, floorY - 1, z), Blocks.GRAY_CONCRETE.getDefaultState()));
				// Shallow dips are filled level; deep holes stay holes.
				for (int y = floorY - 2; y >= floorY - 6; y--) {
					p.set(x, y, z);
					if (!world.getBlockState(p).isAir() && world.getFluidState(p).isEmpty()) {
						break;
					}
					queue.add(new Op(p.asLong(), Blocks.GRAY_CONCRETE.getDefaultState()));
				}
			}
		}
	}

	// ------------------------------------------------------------------ spotlights

	private void scorch(Vec3d g, int t) {
		for (int i = 0; i < TrumanSet.LAMPS; i++) {
			Vec3d s = TrumanSet.spot(g, fwd, i, t);
			int x = MathHelper.floor(s.x), z = MathHelper.floor(s.z);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos top = Kit.surface(world, x, z);
			Vec3d hit = Vec3d.ofCenter(top.up());
			if (t % 2 == 0) {
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, hit.x, hit.y, hit.z, 3, 0.6, 0.2, 0.6, 0.02);
				world.spawnParticles(ParticleTypes.LAVA, hit.x, hit.y, hit.z, 1, 0.4, 0.1, 0.4, 0.0);
			}
			if (Destruction.enabled(world)) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx != 0 && dz != 0) {
							continue;
						}
						BlockPos q = Kit.surface(world, x + dx, z + dz);
						BlockState old = world.getBlockState(q);
						if (!old.hasBlockEntity() && old.getFluidState().isEmpty()) {
							Destruction.set(world, q, dx == 0 && dz == 0 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.BLACKSTONE.getDefaultState(), Kit.FLAGS);
						}
					}
				}
				if (world.random.nextInt(6) == 0 && world.getBlockState(top.up()).isAir()) {
					Destruction.set(world, top.up(), Blocks.FIRE.getDefaultState(), Kit.FLAGS);
				}
			}
			if (t % 4 == 0) {
				for (LivingEntity e : Kit.living(world, hit, 3.0)) {
					e.setOnFireFor(5.0F);
					e.timeUntilRegen = 0;
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 4.0F);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the painted sky

	private BlockState skyPaint(double az, int h, int x, int z) {
		double el = h / TrumanSet.WALL_H * 0.9;
		double c = TrumanSet.cloud(az, el);
		if (c > 0.55) {
			return Blocks.WHITE_CONCRETE.getDefaultState();
		}
		if (c > 0.45 && ((x + z + h) & 1) == 0) {
			// Cloud rims: shaded with a little grey-blue, like brushwork.
			return (c < 0.5 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE).getDefaultState();
		}
		double dither0 = (((x * 5 + z * 11 + h * 3) % 8) + 8) % 8 / 8.0;
		if (h < 7) {
			// A warm, hazy painted horizon along the bottom of the wall.
			double warm = 1 - h / 7.0;
			if (dither0 < warm * 0.6) {
				return (h < 3 ? Blocks.ORANGE_TERRACOTTA : Blocks.WHITE_TERRACOTTA).getDefaultState();
			}
			if (dither0 < warm) {
				return Blocks.WHITE_CONCRETE.getDefaultState();
			}
		}
		double k = MathHelper.clamp((h - 18) / 34.0, 0.0, 1.0);
		double dither = (((x * 7 + z * 13 + h * 5) % 8) + 8) % 8 / 8.0;
		return dither < k ? Blocks.BLUE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
	}

	/** A wall of painted sky all the way round, with an EXIT door and a staircase up to it. */
	private void queueWall(Vec3d g) {
		if (!Destruction.enabled(world) || wallR < 24) {
			return;
		}
		int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z);
		int top = floorY + (int) TrumanSet.WALL_H;
		int doorY = floorY + (int) TrumanSet.DOOR_H;
		double doorAz = TrumanSet.bearing(fwd);
		String sign = "EXIT";
		int signW = TrumanSet.width(sign);
		List<long[]> columns = new ArrayList<>();
		int ri = wallR + 2;
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < wallR - 1 || d >= wallR + 1) {
					continue;
				}
				double az = Math.atan2(dz, dx);
				columns.add(new long[]{dx, dz, Double.doubleToLongBits(az)});
			}
		}
		// Raise it in a sweep around the circle, starting behind the door.
		columns.sort((a, b) -> Double.compare(rel(Double.longBitsToDouble(a[2]), doorAz), rel(Double.longBitsToDouble(b[2]), doorAz)));
		for (long[] col : columns) {
			int x = gx + (int) col[0], z = gz + (int) col[1];
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			double az = Double.longBitsToDouble(col[2]);
			double arc = MathHelper.wrapDegrees(Math.toDegrees(az - doorAz)) / 180.0 * Math.PI * wallR;
			int bottom = Math.min(Kit.surface(world, x, z).getY(), floorY) - 2;
			for (int y = bottom; y <= top; y++) {
				int h = y - floorY;
				BlockState s = skyPaint(az, h, x, z);
				if (Math.abs(arc) <= 1.6 && y >= doorY && y < doorY + 4) {
					s = Blocks.AIR.getDefaultState();
				}
				int px = (int) Math.floor(arc + signW / 2.0 + 0.5);
				int row = doorY + 9 - y;
				if (px >= 0 && px < signW && row >= 0 && row < 5 && px % 4 != 3
						&& TrumanSet.pixel(sign.charAt(px / 4), px % 4, row)) {
					s = Blocks.RED_CONCRETE.getDefaultState();
				}
				queue.add(new Op(BlockPos.asLong(x, y, z), s));
			}
		}
		// White stairs climbing the inside of the wall to the door, from its left.
		Vec3d out = new Vec3d(Math.cos(doorAz), 0, Math.sin(doorAz));
		Vec3d tangent = new Vec3d(-out.z, 0, out.x);
		int steps = (int) TrumanSet.DOOR_H;
		for (int k = 0; k <= steps + 2; k++) {
			double s = k <= 2 ? k - 1.0 : -1.5 - (k - 2);
			int stepTop = k <= 2 ? doorY - 1 : doorY - 1 - (k - 2);
			for (int depth = 2; depth <= 3; depth++) {
				Vec3d at = g.add(out.multiply(wallR - depth)).add(tangent.multiply(s));
				int x = MathHelper.floor(at.x), z = MathHelper.floor(at.z);
				for (int y = Math.min(Kit.surface(world, x, z).getY(), floorY); y < stepTop; y++) {
					queue.add(new Op(BlockPos.asLong(x, y, z), Blocks.SMOOTH_QUARTZ.getDefaultState()));
				}
				BlockState tread = k <= 2 ? Blocks.QUARTZ_BLOCK.getDefaultState()
						: Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.getFacing(tangent.x, 0, tangent.z));
				queue.add(new Op(BlockPos.asLong(x, stepTop, z), tread));
			}
		}
	}

	private static double rel(double az, double from) {
		double d = (az - from + Math.PI) % (Math.PI * 2);
		return d < 0 ? d + Math.PI * 2 : d;
	}

	private void drain() {
		BlockPos.Mutable p = new BlockPos.Mutable();
		int n = 0;
		while (n < BUDGET && !queue.isEmpty()) {
			Op op = queue.poll();
			p.set(op.pos());
			n++;
			if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
				continue;
			}
			if (world.getBlockState(p).hasBlockEntity()) {
				continue;
			}
			Destruction.set(world, p.toImmutable(), op.state(), Kit.FLAGS);
		}
	}
}
