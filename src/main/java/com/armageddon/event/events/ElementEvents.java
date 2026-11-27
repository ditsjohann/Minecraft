package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.ElementShapes;
import com.armageddon.util.ElementShapes.Basalt;
import com.armageddon.util.ElementShapes.Downburst;
import com.armageddon.util.ElementShapes.Inferno;
import com.armageddon.util.ElementShapes.Maelstrom;
import com.armageddon.util.ElementShapes.Roller;
import com.armageddon.util.ElementShapes.Stride;
import com.armageddon.util.ElementShapes.Thunder;
import com.armageddon.util.ElementShapes.Wrecker;
import com.armageddon.util.FossilShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Three CITY DESTRUCTION bombs, each wrecking a city a different way - the WRECKING BALL (smashed), the FLATTENER
 * (pressed flat), TITAN'S STRIDE (stamped on) - and five elemental CELESTIALS: the INFERNO VORTEX (fire), the MAELSTROM
 * (water), the BASALT UPRISING (earth), the DOWNBURST (air) and the THUNDERHEAD (storm).
 */
public final class ElementEvents {
	private ElementEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: ElementFx). */
	public static final int FX_WRECKER = 72, FX_ROLLER = 73, FX_INFERNO = 74, FX_MAELSTROM = 75;

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	/** A column's coordinates in the heading frame: forward, right. */
	static double[] local(Vec3d base, Vec3d h, double x, double z) {
		return FossilEvents2.local(base, h, x, z);
	}

	/** Every column within the heading-frame box, keyed by {@code key} (columns with a NaN key are left out). */
	static TerrainJob box(PlasmaEvents.PlasmaBase e, double x0, double x1, double z0, double z1, java.util.function.ToDoubleBiFunction<Double, Double> key,
			TerrainJob.Painter painter) {
		List<Long> cols = new ArrayList<>();
		List<Float> keys = new ArrayList<>();
		java.util.Set<Long> seen = new java.util.HashSet<>();
		for (double x = x0; x <= x1; x += 0.5) {
			for (double z = z0; z <= z1; z += 0.5) {
				Vec3d w = e.at(x, 0, z);
				int bx = MathHelper.floor(w.x), bz = MathHelper.floor(w.z);
				long id = BlockPos.asLong(bx, 0, bz);
				if (!seen.add(id)) {
					continue;
				}
				double[] l = local(e.base, e.heading, bx + 0.5, bz + 0.5);
				double k = key.applyAsDouble(l[0], l[1]);
				if (!Double.isNaN(k)) {
					cols.add(id);
					keys.add((float) k);
				}
			}
		}
		long[] c = new long[cols.size()];
		float[] k = new float[cols.size()];
		for (int i = 0; i < c.length; i++) {
			c[i] = cols.get(i);
			k[i] = keys.get(i);
		}
		return new TerrainJob(e.w(), c, k, painter);
	}

	/** The ground a city stands on under a column: through trees, roofs, walls and floors down to earth (or the bomb's own level). */
	static int floor(TerrainJob.Column c, int baseY) {
		int y = c.top();
		for (int i = 0; i < 160 && y > baseY - 12; i++, y--) {
			BlockState s = c.get(y);
			if (FossilEvents2.earth(s) && y <= baseY + 24 || !s.getFluidState().isEmpty()) {
				return y;
			}
		}
		return baseY - 1;
	}

	/** Swept clean: leaves and plants gone, trees snapped off at the stump. */
	static void sweep(TerrainJob.Column c, int surf) {
		for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
			BlockState s = c.get(y);
			if (s.isAir()) {
				continue;
			}
			if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s) || s.isIn(BlockTags.LOGS) && y > surf + 1) {
				c.set(y, Blocks.AIR.getDefaultState());
			}
		}
	}

	// ====================================================================== WRECKING BALL

	/**
	 * WRECKING BALL. A chain comes down out of the sky with a ball of steel on the end as big as a house. It swings
	 * through the city in six great arcs, each in a new direction, punching a clean swath through every building it
	 * meets and throwing the wreckage ahead of it; then the chain snaps and the ball drops into the middle of it all.
	 */
	public static class WreckingBall extends PlasmaEvents.PlasmaBase {
		private final TerrainJob[] swaths = new TerrainJob[Wrecker.SWINGS];
		private boolean landed;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("wrecking_ball");
				Vfx.newFx(world, FX_WRECKER, base, heading, 1.0F, 0x8A9098, 0, 0, Wrecker.END);
				Kit.sound(world, base.add(0, 60, 0), SoundEvents.BLOCK_CHAIN_PLACE, 256.0F, 0.3F);
			}
			Vec3d ball = at(Wrecker.ball(t).x, Wrecker.ball(t).y, Wrecker.ball(t).z);
			Vec3d vel = ball.subtract(at(Wrecker.ball(t - 1).x, Wrecker.ball(t - 1).y, Wrecker.ball(t - 1).z));
			if (t < Wrecker.FIRST && t % 10 == 0) {
				Kit.sound(world, ball, SoundEvents.BLOCK_CHAIN_PLACE, 128.0F, 0.4F);
			}
			for (int k = 0; k < Wrecker.SWINGS; k++) {
				double u = Wrecker.swinging(k, t);
				if (u >= 0 && swaths[k] == null) {
					startSwath(k);
					Kit.sound(world, ball, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 256.0F, 0.3F);
				}
				if (swaths[k] != null && !swaths[k].isDone()) {
					swaths[k].limit = u < 0 ? 2 : u;
					swaths[k].step(5000);
				}
			}
			double low = ball.y - Kit.ground(world, ball.x, ball.z).y;
			if (t >= Wrecker.FIRST && t < Wrecker.SNAP && low < Wrecker.BALL + 40 && vel.length() > 1) {
				// Smashing through: wreckage thrown on ahead of it, dust, and the crunch of it.
				if (t % 2 == 0) {
					for (FallingBlockEntity f : Kit.lift(world, ball.add(vel.normalize().multiply(Wrecker.BALL)), Wrecker.BALL + 2, 7, false, false)) {
						debris.throwOne(f, vel.multiply(0.6 + seed(f, 1) * 0.5).add(0, 0.5 + seed(f, 2) * 0.6, 0));
					}
					Vfx.play(world, VfxIds.WRECK_SMASH, ball, vel.normalize(), 1.0F, 0);
				}
				if (t % 4 == 0) {
					Kit.sound(world, ball, SoundEvents.BLOCK_ANVIL_LAND, 192.0F, 0.3F);
					Kit.sound(world, ball, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 160.0F, 0.5F);
					Vfx.shake(world, ball, 200, 1.0F);
				}
				for (LivingEntity e : Kit.living(world, ball, Wrecker.BALL + 4)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.velocity(e, vel.multiply(0.9).add(0, 0.8, 0));
						e.damage(world.getDamageSources().generic(), 40.0F);
					}
				}
			}
			if (t == Wrecker.SNAP) {
				Kit.sound(world, ball, SoundEvents.BLOCK_CHAIN_BREAK, 256.0F, 0.3F);
				Kit.sound(world, ball, SoundEvents.BLOCK_ANVIL_LAND, 256.0F, 1.6F);
			}
			if (t == Wrecker.LAND && !landed) {
				landed = true;
				Vec3d g = ground(base.x, base.z);
				Vfx.play(world, VfxIds.WRECK_LAND, g, 1.0F);
				Vfx.flash(world, g, 0.4F, 300, 0xE0E4E8);
				Vfx.shake(world, g, 500, 2.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 256.0F, 0.2F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 256.0F, 0.6F);
				bigCrater(g, 22);
				PlasmaEvents.blast(this, g, 45, 40.0F, 2.2, 0);
			}
			debris.tick(world, 0x9098A0, p -> impact(p, 0x9098A0));
			drain();
			boolean jobs = true;
			for (TerrainJob j : swaths) {
				jobs &= j == null || j.isDone();
			}
			return t >= Wrecker.END && jobs && debris.settled();
		}

		/** Swing {@code k}'s swath: every column the ball passes through, keyed by when it gets there. */
		private void startSwath(int k) {
			if (!Destruction.enabled(world)) {
				return;
			}
			Vec3d d = Wrecker.plane(k);
			double reach = Wrecker.CHAIN * Math.sin(Wrecker.MAX);
			double r = Wrecker.BALL + 0.5;
			List<Long> cols = new ArrayList<>();
			List<Float> keys = new ArrayList<>();
			java.util.Set<Long> seen = new java.util.HashSet<>();
			for (double x = -reach; x <= reach; x += 0.5) {
				for (double s = -r; s <= r; s += 0.5) {
					Vec3d l = d.multiply(x).add(-d.z * s, 0, d.x * s);
					Vec3d w = at(l.x, 0, l.z);
					long id = BlockPos.asLong(MathHelper.floor(w.x), 0, MathHelper.floor(w.z));
					if (seen.add(id)) {
						cols.add(id);
						keys.add((float) Wrecker.passes(k, x)[0]);
					}
				}
			}
			long[] c = new long[cols.size()];
			float[] kk = new float[cols.size()];
			for (int i = 0; i < c.length; i++) {
				c[i] = cols.get(i);
				kk[i] = keys.get(i);
			}
			swaths[k] = new TerrainJob(world, c, kk, col -> smash(col, k));
			swaths[k].limit = 0;
		}

		/** The ball passing through a column: a round slice of it is gone. */
		private void smash(TerrainJob.Column c, int k) {
			Vec3d d = Wrecker.plane(k);
			double[] l = local(base, heading, c.x + 0.5, c.z + 0.5);
			double along = l[0] * d.x + l[1] * d.z, off = -l[0] * d.z + l[1] * d.x;
			double half2 = Wrecker.BALL * Wrecker.BALL - off * off;
			if (half2 <= 0) {
				return;
			}
			double half = Math.sqrt(half2);
			double y = base.y + Wrecker.passes(k, along)[1];
			for (int yy = MathHelper.floor(y - half); yy <= MathHelper.ceil(y + half); yy++) {
				if (!c.get(yy).isAir()) {
					c.set(yy, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	// ====================================================================== THE FLATTENER

	/**
	 * THE FLATTENER. A roller of steel a hundred and fifty blocks long comes down out of the sky and lands with a
	 * thud that shakes the whole city; then it rolls right across it. Everything it passes - every tower, house, tree
	 * and car - is pressed flat into a single layer of whatever stood there, so behind it the city is left a map of
	 * itself in coloured blocks. Then it lifts away.
	 */
	public static class Flattener extends PlasmaEvents.PlasmaBase {
		private TerrainJob press;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("flattener");
				Vfx.newFx(world, FX_ROLLER, base, heading, 1.0F, 0x9AA0A8, 0, 0, Roller.END);
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 192.0F, 0.4F);
			}
			Vec3d axle = at(Roller.x(t), 0, 0);
			if (t == Roller.LANDED) {
				Vfx.play(world, VfxIds.ROLLER_LAND, ground(axle.x, axle.z), heading, 1.0F, 0);
				Vfx.shake(world, axle, 500, 2.4F);
				Kit.sound(world, axle, SoundEvents.BLOCK_ANVIL_LAND, 256.0F, 0.2F);
				Kit.sound(world, axle, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.3F);
				if (Destruction.enabled(world)) {
					press = box(this, Roller.FROM - Roller.RADIUS, Roller.TO + Roller.RADIUS, -Roller.HALF, Roller.HALF, (x, z) -> x, this::press);
					press.limit = Roller.FROM + Roller.RADIUS * 0.7;
				}
			}
			if (press != null) {
				press.limit = Math.min(Roller.TO + Roller.RADIUS, Roller.x(t) + Roller.RADIUS * 0.7);
				press.step(6000);
			}
			if (t > Roller.LANDED && t < Roller.ROLL_END) {
				if (t % 3 == 0) {
					for (int i = 0; i < 4; i++) {
						double z = (FossilShapes.hash(t, i) - 0.5) * 2 * Roller.HALF;
						Vec3d p = at(Roller.x(t) + Roller.RADIUS, 0, z);
						Vfx.play(world, VfxIds.ROLLER_DUST, ground(p.x, p.z), heading, 1.0F, 0);
					}
				}
				if (t % 8 == 0) {
					Kit.sound(world, axle, SoundEvents.BLOCK_GRINDSTONE_USE, 256.0F, 0.3F);
					Kit.sound(world, axle, ModSounds.NUKE_RUMBLE, 192.0F, 0.5F);
					Vfx.shake(world, axle, 300, 1.2F);
				}
				if (t % 14 == 0) {
					Kit.sound(world, axle, SoundEvents.BLOCK_STONE_BREAK, 256.0F, 0.3F);
					Kit.sound(world, axle, SoundEvents.BLOCK_GLASS_BREAK, 192.0F, 0.4F);
				}
				for (LivingEntity e : Kit.living(world, axle, Roller.HALF + Roller.RADIUS)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					double[] l = local(base, heading, e.getX(), e.getZ());
					if (Math.abs(l[1]) < Roller.HALF + 1 && Math.abs(l[0] - Roller.x(t)) < Roller.RADIUS) {
						e.damage(world.getDamageSources().generic(), 60.0F);
						Kit.velocity(e, new Vec3d(0, -1, 0));
					} else if (Math.abs(l[1]) < Roller.HALF + 1 && l[0] > Roller.x(t) && l[0] - Roller.x(t) < Roller.RADIUS + 6) {
						// Shoved along in front of it.
						Kit.push(e, heading.multiply(0.6).add(0, 0.3, 0));
					}
				}
			}
			if (t == Roller.LIFT) {
				Kit.sound(world, axle, SoundEvents.BLOCK_CHAIN_BREAK, 256.0F, 0.3F);
				Kit.sound(world, axle, SoundEvents.BLOCK_PISTON_EXTEND, 256.0F, 0.3F);
			}
			drain();
			return t >= Roller.END && (press == null || press.isDone());
		}

		/** Pressed flat: everything over the ground goes, and its top-most block is left lying there as a single layer. */
		private void press(TerrainJob.Column c) {
			int baseY = MathHelper.floor(base.y) - 1;
			int top = c.top();
			BlockState cap = null;
			int y = top;
			for (; y > baseY - 12; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (cap == null && !FossilEvents2.plant(s)) {
					cap = s;
				}
				if (FossilEvents2.earth(s) && y <= baseY + 24 || !s.getFluidState().isEmpty()) {
					break;
				}
			}
			int floor = y;
			if (floor >= top) {
				return;
			}
			for (int yy = top; yy > floor; yy--) {
				if (!c.get(yy).isAir()) {
					c.set(yy, Blocks.AIR.getDefaultState());
				}
			}
			if (cap != null && !FossilEvents2.earth(cap) && cap.isFullCube(c.world(), new BlockPos(c.x, floor + 1, c.z))) {
				c.set(floor + 1, cap.isIn(BlockTags.LOGS) ? Blocks.STRIPPED_OAK_WOOD.getDefaultState() : cap);
			} else if (cap != null && cap.isIn(BlockTags.LEAVES)) {
				c.set(floor + 1, Blocks.MOSS_CARPET.getDefaultState());
			}
		}
	}

	// ====================================================================== TITAN'S STRIDE

	/**
	 * TITAN'S STRIDE. Something you cannot see walks across the city. Its shadow falls first, darkening the ground;
	 * then its foot comes down, and where it lands is a footprint ten blocks deep - heel, sole and toes - with whatever
	 * stood there gone and the earth heaved up round its rim. Nine steps; then it jumps, and comes down with both feet.
	 */
	public static class TitansStride extends PlasmaEvents.PlasmaBase {
		private final List<TerrainJob> prints = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("titans_stride");
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 256.0F, 0.3F);
			}
			for (int k = 0; k < Stride.STEPS + 2; k++) {
				int land = Stride.land(k);
				Vec3d f = Stride.foot(k);
				Vec3d w = at(f.x, 0, f.z);
				Vec3d g = ground(w.x, w.z);
				if (t == land - Stride.SHADOW) {
					Vfx.play(world, VfxIds.STRIDE_SHADOW, g, heading, k >= Stride.STEPS ? 1.3F : 1.0F, 0);
					Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 256.0F, 0.3F);
					Kit.sound(world, g, SoundEvents.ITEM_ELYTRA_FLYING, 192.0F, 0.3F);
				}
				if (t == land) {
					stomp(k, w, g);
				}
			}
			prints.removeIf(j -> {
				j.step(5000);
				return j.isDone();
			});
			debris.tick(world, 0x8A7A62, p -> impact(p, 0x8A7A62));
			drain();
			return t >= Stride.END && prints.isEmpty() && debris.settled();
		}

		/** A foot coming down: the footprint stamped out, the earth heaved up round it, the ground for streets around shaking. */
		private void stomp(int k, Vec3d w, Vec3d g) {
			boolean jump = k >= Stride.STEPS;
			float power = jump ? 1.5F : 1.0F;
			Vfx.play(world, VfxIds.STRIDE_STOMP, g, heading, power, 0);
			Vfx.shake(world, g, 400, 1.8F * power);
			Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.25F);
			Kit.sound(world, g, SoundEvents.ENTITY_RAVAGER_STEP, 256.0F, 0.2F);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 192.0F, 0.4F);
			double scale = jump ? 1.25 : 1.0;
			for (LivingEntity e : Kit.living(world, g, 60 * scale)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				double[] l = local(w, heading, e.getX(), e.getZ());
				if (Stride.inside(l[0], l[1], scale * 1.1)) {
					e.damage(world.getDamageSources().generic(), 200.0F);
				} else {
					double d = Math.max(1, Math.hypot(e.getX() - g.x, e.getZ() - g.z));
					double kk = 1 - d / (60 * scale);
					Vec3d out = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z).multiply(1 / d);
					Kit.push(e, out.multiply(1.4 * kk).add(0, 1.0 * kk, 0));
					e.damage(world.getDamageSources().generic(), (float) (14 * kk));
				}
			}
			// The earth thrown up round the rim of it.
			for (int i = 0; i < 6; i++) {
				double a = i * MathHelper.TAU / 6 + FossilShapes.hash(k, i);
				Vec3d p = ground(g.x + Math.cos(a) * 20 * scale, g.z + Math.sin(a) * 12 * scale);
				for (FallingBlockEntity f : Kit.lift(world, p, 5, 6, true, false)) {
					debris.throwOne(f, new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(0.7 + seed(f, 3) * 0.6).add(0, 0.8 + seed(f, 4) * 0.6, 0));
				}
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			double l = Stride.FOOT_L * scale * 0.6, wd = Stride.FOOT_W * scale * 0.75;
			Vec3d f = Stride.foot(k);
			int baseY = MathHelper.floor(g.y) - 1;
			TerrainJob j = box(this, f.x - l, f.x + l, f.z - wd, f.z + wd, (x, z) -> {
				double dx = x - f.x, dz = z - f.z;
				return Stride.inside(dx, dz, scale * 1.18) ? Math.hypot(dx, dz) : Double.NaN;
			}, c -> print(c, f, scale, baseY));
			prints.add(j);
		}

		/** One column of a footprint: stamped down ten blocks, packed hard at the bottom; just outside it, heaved earth. */
		private void print(TerrainJob.Column c, Vec3d f, double scale, int baseY) {
			double[] l = local(base, heading, c.x + 0.5, c.z + 0.5);
			double dx = l[0] - f.x, dz = l[1] - f.z;
			int surf = Math.min(floor(c, baseY), baseY + 6);
			if (Stride.inside(dx, dz, scale)) {
				int bottom = surf - (int) Math.round(Stride.DEPTH * scale);
				for (int y = c.top(); y > bottom; y--) {
					if (!c.get(y).isAir()) {
						c.set(y, Blocks.AIR.getDefaultState());
					}
				}
				double n = c.noise(131);
				c.set(bottom, (n < 0.45 ? Blocks.PACKED_MUD : n < 0.75 ? Blocks.COARSE_DIRT : Blocks.CRACKED_STONE_BRICKS).getDefaultState());
			} else {
				// The rim: earth squeezed up round the print.
				sweep(c, surf);
				c.set(surf + 1, (c.noise(133) < 0.6 ? Blocks.COARSE_DIRT : Blocks.ROOTED_DIRT).getDefaultState());
				if (c.noise(135) < 0.4) {
					c.set(surf + 2, Blocks.COARSE_DIRT.getDefaultState());
				}
			}
		}
	}

	// ====================================================================== INFERNO VORTEX

	/**
	 * INFERNO VORTEX. Six whirls of fire spin up out of the ground round the bomb and spiral in toward it, each
	 * burning a black spiral scar into the land behind it and catching up whatever it passes. They merge into one
	 * colossal pillar of fire that drags everything round it up into the flames; then it collapses into a sea of flame
	 * that rolls out over everything for a hundred and forty blocks.
	 */
	public static class InfernoVortex extends PlasmaEvents.PlasmaBase {
		private TerrainJob sea;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("inferno_vortex");
				Vfx.newFx(world, FX_INFERNO, base, heading, 1.0F, 0xFF6010, 0, 0, Inferno.END);
				Kit.sound(world, base, SoundEvents.BLOCK_FIRE_AMBIENT, 192.0F, 0.4F);
			}
			if (t < Inferno.END - 60 && t % 40 == 0) {
				Vfx.skyTint(world, base, Inferno.R + 150, 0xD04010, 0.55F);
			}
			if (t >= Inferno.RISE && t < Inferno.MERGE) {
				for (int k = 0; k < Inferno.WHIRLS; k++) {
					Vec3d l = Inferno.whirl(k, t);
					Vec3d g = ground(at(l.x, 0, l.z).x, at(l.x, 0, l.z).z);
					scar(g, 7, t * 7 + k, 10);
					if ((t + k * 5) % 16 == 0) {
						Kit.sound(world, g, SoundEvents.BLOCK_FIRE_AMBIENT, 192.0F, 0.4F);
						Kit.sound(world, g, SoundEvents.ENTITY_BLAZE_SHOOT, 128.0F, 0.3F);
					}
					catchUp(g, 9, 0.7);
				}
			}
			if (t >= Inferno.MERGE && t < Inferno.COLLAPSE) {
				if (t == Inferno.MERGE) {
					Vfx.flash(world, base.add(0, 30, 0), 0.5F, 300, 0xFFA040);
					Kit.sound(world, base, SoundEvents.ENTITY_BLAZE_SHOOT, 256.0F, 0.2F);
					Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 256.0F, 0.4F);
				}
				scar(base, 22, t * 13, 30);
				catchUp(base, 34, 1.2);
				if (t % 12 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_FIRE_AMBIENT, 256.0F, 0.3F);
					Vfx.shake(world, base, 300, 1.2F);
				}
			}
			if (t == Inferno.COLLAPSE) {
				Vfx.play(world, VfxIds.INFERNO_COLLAPSE, base, 1.0F);
				Vfx.flash(world, base.add(0, 20, 0), 0.8F, 600, 0xFFB050);
				Vfx.shake(world, base, 500, 2.2F);
				Kit.sound(world, base, ModSounds.NUKE_DETONATE, 256.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_BLAZE_SHOOT, 256.0F, 0.3F);
				if (Destruction.enabled(world)) {
					sea = TerrainJob.disc(world, base, Inferno.R, this::burn);
					sea.limit = 0;
					Kit.sphere(world, BlockPos.ofFloored(base.add(0, -4, 0)), 7, Blocks.MAGMA_BLOCK.getDefaultState());
				}
			}
			if (sea != null) {
				sea.limit = Inferno.sea(t);
				sea.step(5000);
			}
			if (t > Inferno.COLLAPSE && t < Inferno.COLLAPSE + 60) {
				double front = Inferno.sea(t);
				for (LivingEntity e : Kit.living(world, base, front + 3)) {
					if (!Kit.creativeOrSpectator(e) && Math.hypot(e.getX() - base.x, e.getZ() - base.z) > front - 10) {
						Vec3d out = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z).normalize();
						Kit.push(e, out.multiply(1.4).add(0, 0.5, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 18.0F);
						e.setOnFireFor(15);
					}
				}
			}
			drain();
			return t >= Inferno.END && (sea == null || sea.isDone());
		}

		/** The whirl's track: the land under it burned black, trees gone, in a ring of radius {@code r}. */
		private void scar(Vec3d g, double r, int salt, int n) {
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int i = 0; i < n; i++) {
				double a = ElementShapes.hash(salt, i * 2) * MathHelper.TAU, d = Math.sqrt(ElementShapes.hash(salt, i * 2 + 1)) * r;
				int x = MathHelper.floor(g.x + Math.cos(a) * d), z = MathHelper.floor(g.z + Math.sin(a) * d);
				int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
				for (int y = top; y > top - 30; y--) {
					BlockPos p = new BlockPos(x, y, z);
					BlockState s = world.getBlockState(p);
					if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
						q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					} else if (s.isIn(BlockTags.LOGS)) {
						q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					} else if (!s.isAir()) {
						if (FossilEvents2.earth(s)) {
							double h = FossilShapes.hash(x, z);
							q().add(p, (h < 0.55 ? Blocks.BLACKSTONE : h < 0.85 ? Blocks.COARSE_DIRT : Blocks.MAGMA_BLOCK).getDefaultState(), BlockQueue.ANY);
						}
						break;
					}
				}
			}
		}

		/** Caught by a whirl: lifted, spun round it, set alight. */
		private void catchUp(Vec3d g, double r, double lift) {
			for (LivingEntity e : Kit.living(world, g.add(0, 6, 0), r + 6)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z);
				double d = Math.max(0.5, to.length());
				if (d > r) {
					continue;
				}
				Vec3d tan = new Vec3d(-to.z / d, 0, to.x / d);
				Kit.velocity(e, tan.multiply(0.9).add(to.multiply(-0.15 / d)).add(0, lift * 0.6, 0));
				e.fallDistance = 0;
				if (age() % 10 == 0) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 4.0F);
					e.setOnFireFor(6);
				}
			}
		}

		/** The sea of flame reaching a column: trees burned to charcoal, the ground scorched, fire everywhere. */
		private void burn(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				} else if (s.isIn(BlockTags.LOGS)) {
					c.set(y, Blocks.BASALT.getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (!FossilEvents2.earth(g)) {
				return;
			}
			double n = c.noise(141);
			c.set(surf, (n < 0.4 ? Blocks.COARSE_DIRT : n < 0.7 ? Blocks.NETHERRACK : n < 0.9 ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK).getDefaultState());
			if (c.noise(143) < 0.3 && c.get(surf + 1).isAir()) {
				c.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		}
	}

	// ====================================================================== MAELSTROM

	/**
	 * MAELSTROM. The ground falls away from the bomb outward into a vast bowl, and the sea comes up into it, already
	 * turning: a whirlpool seventy blocks across. Everything round it - the land at its rim, trees, creatures - is
	 * dragged spiralling in and pulled under. Then the vortex erupts: a waterspout a hundred blocks high that comes
	 * down as a deluge, and when it is over there is a deep round lake where the land was.
	 */
	public static class MaelstromBomb extends PlasmaEvents.PlasmaBase {
		private TerrainJob bowl;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("maelstrom");
				Vfx.newFx(world, FX_MAELSTROM, base, heading, 1.0F, 0x2080C0, 0, 0, Maelstrom.END);
				Kit.sound(world, base, SoundEvents.AMBIENT_UNDERWATER_ENTER, 192.0F, 0.4F);
			}
			if (t < Maelstrom.END - 40 && t % 40 == 0) {
				Vfx.skyTint(world, base, Maelstrom.R + 150, 0x3A5A78, 0.5F);
			}
			if (t == Maelstrom.OPEN) {
				Vfx.shake(world, base, 300, 1.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_SPLASH, 256.0F, 0.3F);
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 256.0F, 0.4F);
				if (Destruction.enabled(world)) {
					bowl = TerrainJob.disc(world, base, Maelstrom.R, this::bowl);
					bowl.limit = 0;
				}
			}
			if (bowl != null) {
				bowl.limit = t >= Maelstrom.OPENED ? Maelstrom.R + 1 : Maelstrom.opened(t);
				bowl.step(7000);
			}
			double spin = Maelstrom.spin(t);
			if (t > Maelstrom.OPEN && t < Maelstrom.ERUPT) {
				if (t % 4 == 0 && debris.held.size() < 140) {
					double a = FossilShapes.hash(t, 3) * MathHelper.TAU;
					Vec3d p = ground(base.x + Math.cos(a) * (Maelstrom.R + 6), base.z + Math.sin(a) * (Maelstrom.R + 6));
					tear(p, 8, 6);
				}
				debris.steer(f -> swirl(f, spin));
				debris.eat(f -> Math.hypot(f.getX() - base.x, f.getZ() - base.z) < 6 || f.getY() < base.y - Maelstrom.DEEP + 4);
				for (LivingEntity e : Kit.living(world, base, Maelstrom.R + 14)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					Vec3d to = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z);
					double d = Math.max(1, to.length());
					Vec3d tan = new Vec3d(-to.z / d, 0, to.x / d);
					double down = d < 14 ? -0.25 : 0;
					Kit.velocity(e, tan.multiply(spin * d * 0.6 + 0.2).add(to.multiply(-0.05 / d * 6)).add(0, down + e.getVelocity().y * 0.5, 0));
					if (d < 10 && t % 20 == 0) {
						e.damage(world.getDamageSources().drown(), 4.0F);
					}
				}
				if (t % 20 == 0) {
					Kit.sound(world, base, SoundEvents.WEATHER_RAIN_ABOVE, 256.0F, 0.4F);
					Kit.sound(world, base, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 256.0F, 0.3F);
				}
			}
			if (t == Maelstrom.ERUPT) {
				Vfx.play(world, VfxIds.MAEL_SPOUT, base.add(0, -8, 0), 1.0F);
				Vfx.shake(world, base, 400, 2.0F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_SPLASH, 256.0F, 0.2F);
				Kit.sound(world, base, ModSounds.NUKE_DETONATE, 256.0F, 0.6F);
				for (LivingEntity e : Kit.living(world, base, 24)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.velocity(e, new Vec3d(0, 3.2, 0));
					}
				}
				debris.release(f -> new Vec3d((f.getX() - base.x) * 0.04, 2.0 + seed(f, 5), (f.getZ() - base.z) * 0.04));
			}
			if (t == Maelstrom.RAIN) {
				Vfx.play(world, VfxIds.MAEL_RAIN, base.add(0, 60, 0), 1.0F);
			}
			debris.tick(world, 0x80C0E0, p -> impact(p, 0x80C0E0));
			drain();
			return t >= Maelstrom.END && (bowl == null || bowl.isDone()) && debris.settled();
		}

		/** Torn-up land caught in the whirl: carried round the bowl, faster and lower toward the middle. */
		private Vec3d swirl(FallingBlockEntity f, double spin) {
			Vec3d p = f.getPos().subtract(base);
			double d = Math.max(1, Math.hypot(p.x, p.z));
			double a = Math.atan2(p.z, p.x) + spin * (Maelstrom.R / d) * 0.8;
			double r = d - 0.6;
			double y = -Maelstrom.depth(r) * 0.6 + 2;
			Vec3d target = base.add(Math.cos(a) * r, y, Math.sin(a) * r);
			return toward(f.getPos(), target, 0.35, 1.6);
		}

		/** The bowl reaching a column: the land dug out to the bowl's depth and the sea let into it. */
		private void bowl(TerrainJob.Column c) {
			double r = Math.hypot(c.x + 0.5 - base.x, c.z + 0.5 - base.z);
			int baseY = MathHelper.floor(base.y) - 1;
			int bottom = baseY - (int) Math.round(Maelstrom.depth(r));
			int level = baseY - (int) Maelstrom.LEVEL;
			for (int y = Math.min(c.top(), baseY + 40); y > bottom; y--) {
				BlockState s = c.get(y);
				BlockState want = y <= level ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
				if (s != want && !(s.isAir() && y > level)) {
					c.set(y, want);
				}
			}
			double n = c.noise(151);
			c.set(bottom, (n < 0.5 ? Blocks.SAND : n < 0.75 ? Blocks.GRAVEL : n < 0.9 ? Blocks.CLAY : Blocks.PRISMARINE).getDefaultState());
		}
	}

	// ====================================================================== BASALT UPRISING

	/**
	 * BASALT UPRISING. The land cracks into a honeycomb, and the cells thrust up out of the ground as columns of
	 * basalt, each its own height, taller and taller toward the middle, in a wave rolling out from the bomb - launching
	 * whatever stands on them. Then the middle of it caves in, a pit opening deep into the earth among the columns.
	 */
	public static class BasaltUprising extends PlasmaEvents.PlasmaBase {
		private TerrainJob rise, pit;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("basalt_uprising");
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 192.0F, 0.4F);
			}
			if (t == Basalt.CRACK) {
				Vfx.play(world, VfxIds.BASALT_CRACK, base, 1.0F);
				Vfx.shake(world, base, 300, 1.2F);
				Kit.sound(world, base, SoundEvents.BLOCK_DEEPSLATE_BREAK, 256.0F, 0.3F);
			}
			if (t == Basalt.RISE) {
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.3F);
				if (Destruction.enabled(world)) {
					rise = TerrainJob.disc(world, base, Basalt.R, this::column);
					rise.limit = 0;
				}
			}
			if (rise != null) {
				rise.limit = t >= Basalt.RISEN ? Basalt.R + 1 : Basalt.front(t);
				rise.step(6000);
			}
			if (t > Basalt.RISE && t < Basalt.RISEN) {
				double front = Basalt.front(t);
				if (t % 2 == 0) {
					for (int i = 0; i < 4; i++) {
						double a = FossilShapes.hash(t, i + 11) * MathHelper.TAU;
						Vfx.play(world, VfxIds.BASALT_RISE, ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front), 1.0F);
					}
				}
				if (t % 6 == 0) {
					Kit.sound(world, base.add(heading.multiply(front)), SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 256.0F, 0.4F);
					Kit.sound(world, base.add(heading.multiply(front)), SoundEvents.BLOCK_PISTON_EXTEND, 192.0F, 0.4F);
					Vfx.shake(world, base, 300, 1.0F);
				}
				for (LivingEntity e : Kit.living(world, base, front + 2)) {
					double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
					if (!Kit.creativeOrSpectator(e) && d > front - 4) {
						Kit.velocity(e, new Vec3d(e.getVelocity().x, 1.3 + (1 - d / Basalt.R) * 0.8, e.getVelocity().z));
						e.damage(world.getDamageSources().generic(), 4.0F);
					}
				}
			}
			if (t == Basalt.CAVE) {
				Vfx.play(world, VfxIds.BASALT_CAVE, base, 1.0F);
				Vfx.shake(world, base, 400, 2.0F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.2F);
				Kit.sound(world, base, SoundEvents.BLOCK_DEEPSLATE_BREAK, 256.0F, 0.2F);
				if (Destruction.enabled(world)) {
					pit = TerrainJob.disc(world, base, Basalt.PIT, this::cave);
				}
				for (LivingEntity e : Kit.living(world, base, Basalt.PIT + 4)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.velocity(e, new Vec3d(0, -1.5, 0));
					}
				}
			}
			if (pit != null) {
				pit.step(6000);
			}
			drain();
			return t >= Basalt.END && (rise == null || rise.isDone()) && (pit == null || pit.isDone());
		}

		/** A column of the honeycomb rising: basalt from the ground up to its height, a crack left round its edge. */
		private void column(TerrainJob.Column c) {
			double[] l = local(base, heading, c.x + 0.5, c.z + 0.5);
			double[] cell = Basalt.cell(l[0], l[1]);
			int surf = FossilEvents2.groundY(c);
			sweep(c, surf);
			for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
				if (c.get(y).isIn(BlockTags.LOGS)) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			if (cell[2] > 0.88) {
				// The crack between columns.
				c.set(surf, Blocks.AIR.getDefaultState());
				c.set(surf - 1, Blocks.BLACKSTONE.getDefaultState());
				return;
			}
			int h = Basalt.height(cell[0], cell[1], (int) cell[3], (int) cell[4]);
			for (int y = surf; y < surf + h; y++) {
				c.set(y, (cell[2] > 0.7 ? Blocks.SMOOTH_BASALT : Blocks.BASALT).getDefaultState());
			}
			c.set(surf + h, (cell[2] < 0.35 && c.noise(161) < 0.3 ? Blocks.MAGMA_BLOCK : Blocks.POLISHED_BASALT).getDefaultState());
		}

		/** The middle caving in: a pit deep into the earth, glowing at the bottom. */
		private void cave(TerrainJob.Column c) {
			double r = Math.hypot(c.x + 0.5 - base.x, c.z + 0.5 - base.z);
			int baseY = MathHelper.floor(base.y) - 1;
			int bottom = baseY - (int) Math.round(Basalt.PIT_DEPTH * Math.sqrt(Math.max(0, 1 - (r / Basalt.PIT) * (r / Basalt.PIT))) + 3);
			for (int y = Math.min(c.top(), baseY + 60); y > bottom; y--) {
				if (!c.get(y).isAir()) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			c.set(bottom, (c.noise(171) < 0.5 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).getDefaultState());
		}
	}

	// ====================================================================== DOWNBURST

	/**
	 * DOWNBURST. The air is drawn up out of the land: a vortex of cloud opens in the sky overhead and the ground under
	 * it comes apart - blocks, trees, creatures lifting and floating up into it, turning slowly round. Then the air comes
	 * back down all at once: everything up there is slammed into the ground, and a wall of wind rolls out and flattens
	 * the land for a hundred and thirty blocks.
	 */
	public static class DownburstBomb extends PlasmaEvents.PlasmaBase {
		private TerrainJob flat;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("downburst");
				Vfx.play(world, VfxIds.DOWN_SKY, base.add(0, Downburst.SKY, 0), 1.0F);
				Kit.sound(world, base, SoundEvents.ITEM_ELYTRA_FLYING, 256.0F, 0.3F);
			}
			if (t < Downburst.SLAM && t % 40 == 0) {
				Vfx.skyTint(world, base, Downburst.R + 150, 0x6A7A8A, 0.5F);
			}
			if (t > Downburst.LIFT && t < Downburst.HOVER) {
				double u = (t - Downburst.LIFT) / (double) (Downburst.HOVER - Downburst.LIFT);
				if (t % 3 == 0) {
					tear(base, Downburst.R * (0.35 + 0.65 * u), 12);
				}
				if (t % 20 == 0) {
					Kit.sound(world, base.add(0, 30, 0), SoundEvents.ITEM_ELYTRA_FLYING, 256.0F, 0.4F + (float) u * 0.4F);
				}
				for (LivingEntity e : Kit.living(world, base, Downburst.R)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					Vec3d target = orbit(e.getId(), e.getPos(), t);
					Kit.velocity(e, toward(e.getPos(), target, 0.06, 0.4));
					e.fallDistance = 0;
				}
			}
			if (t > Downburst.LIFT && t < Downburst.SLAM) {
				debris.steer(f -> toward(f.getPos(), orbit(f.getId(), f.getPos(), t), 0.08, 0.9));
			}
			if (t == Downburst.SLAM) {
				Vfx.play(world, VfxIds.DOWN_SLAM, base, 1.0F);
				Vfx.flash(world, base.add(0, 40, 0), 0.5F, 400, 0xDCE8F4);
				Vfx.shake(world, base, 500, 2.6F);
				Kit.sound(world, base, ModSounds.NUKE_DETONATE, 256.0F, 0.6F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 256.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ITEM_ELYTRA_FLYING, 256.0F, 0.2F);
				debris.release(f -> new Vec3d((f.getX() - base.x) * 0.01, -3.0, (f.getZ() - base.z) * 0.01));
				for (LivingEntity e : Kit.living(world, base, Downburst.R + 10)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.velocity(e, new Vec3d(0, -3.0, 0));
						e.damage(world.getDamageSources().generic(), 12.0F);
					}
				}
				if (Destruction.enabled(world)) {
					flat = TerrainJob.disc(world, base, Downburst.SLAM_R, c -> sweepFlat(c));
					flat.limit = 0;
				}
			}
			if (flat != null) {
				flat.limit = Downburst.front(t);
				flat.step(5000);
			}
			if (t > Downburst.SLAM && t < Downburst.SLAM + 40) {
				double front = Downburst.front(t);
				for (LivingEntity e : Kit.living(world, base, front + 3)) {
					double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
					if (!Kit.creativeOrSpectator(e) && d > front - 8) {
						Vec3d out = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z).normalize();
						Kit.push(e, out.multiply(2.0).add(0, 0.3, 0));
						e.damage(world.getDamageSources().generic(), 10.0F);
					}
				}
			}
			debris.tick(world, 0xC0D0E0, p -> impact(p, 0xC0D0E0));
			drain();
			return t >= Downburst.END && (flat == null || flat.isDone()) && debris.settled();
		}

		/** Where something lifted should be heading: up into the vortex and slowly round it. */
		private Vec3d orbit(int id, Vec3d pos, int t) {
			Vec3d rel = pos.subtract(base);
			double d = Math.max(2, Math.hypot(rel.x, rel.z));
			double a = Math.atan2(rel.z, rel.x) + 0.6 / Math.sqrt(d);
			double y = 14 + FossilShapes.hash(id, 7) * Downburst.CEILING * Math.min(1, (t - Downburst.LIFT) / 120.0 + 0.2);
			double r = d * (0.98 + 0.02 * Math.sin(t * 0.05 + id));
			return base.add(Math.cos(a) * r, y, Math.sin(a) * r);
		}

		private void sweepFlat(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			sweep(c, surf);
			BlockState g = c.get(surf);
			if (g.isOf(Blocks.GRASS_BLOCK)) {
				c.set(surf, (c.noise(181) < 0.5 ? Blocks.COARSE_DIRT : Blocks.DIRT).getDefaultState());
			}
		}
	}

	// ====================================================================== THUNDERHEAD

	/**
	 * THUNDERHEAD. A supercell builds over the land - a black anvil of cloud a hundred blocks up, rain sheeting down -
	 * and lightning hammers the ground, over and over. Every strike burns a branching scar into the land, black glass
	 * and scorch spreading out like a fern. Then the storm draws itself in and one last bolt, a superbolt, strikes the
	 * middle: a crater, a flash that whites out the sky, and a giant glowing scar branching out seventy blocks.
	 */
	public static class ThunderheadBomb extends PlasmaEvents.PlasmaBase {
		private int struck;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("thunderhead");
				Vfx.play(world, VfxIds.THUNDER_CLOUD, base.add(0, Thunder.CLOUD, 0), 1.0F);
				Kit.sound(world, base, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 256.0F, 0.5F);
			}
			if (t < Thunder.END - 40 && t % 40 == 0) {
				Vfx.skyTint(world, base, Thunder.R + 150, 0x283040, 0.7F);
			}
			if (t == Thunder.STORM - 20) {
				Vfx.play(world, VfxIds.THUNDER_RAIN, base.add(0, 50, 0), 1.0F);
			}
			while (struck < Thunder.STRIKES && t >= Thunder.strike(struck)[2]) {
				double[] s = Thunder.strike(struck);
				Vec3d w = at(s[0], 0, s[1]);
				strike(ground(w.x, w.z), struck++, false);
			}
			if (t > Thunder.STORM && t < Thunder.STORM_END && t % 9 == 0) {
				Kit.sound(world, base.add(0, Thunder.CLOUD, 0), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 256.0F, 0.6F + (FossilShapes.hash(t, 1) > 0.5 ? 0.3F : 0.0F));
			}
			if (t == Thunder.SUPER) {
				strike(ground(base.x, base.z), 999, true);
			}
			drain();
			return t >= Thunder.END;
		}

		/** A bolt: the arc of it down from the cloud, real lightning, a flash and a branching scar burned into the ground. */
		private void strike(Vec3d g, int i, boolean superbolt) {
			Vec3d top = g.add((FossilShapes.hash(i, 1) - 0.5) * 30, Thunder.CLOUD, (FossilShapes.hash(i, 2) - 0.5) * 30);
			Vfx.arc(world, top, g, superbolt ? 7.0F : 2.4F, 0xD8E6FF);
			if (superbolt) {
				for (int k = 0; k < 4; k++) {
					Vfx.arc(world, top.add((k - 1.5) * 8, 0, (k % 2) * 8), g, 3.0F, 0xC8DCFF);
				}
				Vfx.play(world, VfxIds.THUNDER_SUPER, g, 1.0F);
				Vfx.flash(world, g.add(0, 30, 0), 1.0F, 800, 0xE8F0FF);
				Vfx.screen(world, g, 400, 1, 0.7F, 10);
				Vfx.shake(world, g, 500, 2.6F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 256.0F, 0.6F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 256.0F, 0.4F);
				bigCrater(g, 12);
				PlasmaEvents.blast(this, g, 45, 40.0F, 2.0, 8);
			} else {
				Vfx.play(world, VfxIds.THUNDER_HIT, g, 1.0F);
				Vfx.shake(world, g, 120, 0.8F);
				PlasmaEvents.blast(this, g, 7, 14.0F, 0.6, 5);
			}
			PlasmaEvents.lightning(world, g);
			if (!Destruction.enabled(world)) {
				return;
			}
			if (superbolt) {
				for (int k = 0; k < 6; k++) {
					fern(g, k * MathHelper.TAU / 6 + 0.3, Thunder.SUPER_R * (0.75 + 0.25 * FossilShapes.hash(k, 9)), 4, i * 17 + k, true);
				}
			} else {
				int branches = 3 + (int) (FossilShapes.hash(i, 5) * 3);
				for (int k = 0; k < branches; k++) {
					fern(g, FossilShapes.hash(i, k + 20) * MathHelper.TAU, 9 + FossilShapes.hash(i, k + 30) * 10, 3, i * 17 + k, false);
				}
			}
		}

		/**
		 * A Lichtenberg figure: a branch of scorch running out from {@code from} along {@code angle}, wandering, forking
		 * into smaller branches; glass where it crosses sand, glowing magma down the big branches of the superbolt's.
		 */
		private void fern(Vec3d from, double angle, double len, int depth, int salt, boolean hot) {
			double x = from.x, z = from.z, a = angle;
			for (double s = 0; s < len; s += 1) {
				a += (ElementShapes.hash(salt, (int) s) - 0.5) * 0.7;
				x += Math.cos(a);
				z += Math.sin(a);
				scorch(MathHelper.floor(x), MathHelper.floor(z), hot && depth >= 3, depth >= 3 ? 1 : 0);
				if (depth > 0 && ElementShapes.hash(salt, (int) s + 500) < 0.12) {
					double side = ElementShapes.hash(salt, (int) s + 700) < 0.5 ? -1 : 1;
					fern(new Vec3d(x, from.y, z), a + side * (0.5 + 0.5 * ElementShapes.hash(salt, (int) s + 900)), (len - s) * 0.55, depth - 1, salt * 31 + (int) s,
							hot);
				}
			}
		}

		private void scorch(int x, int z, boolean hot, int width) {
			for (int dx = -width; dx <= width; dx++) {
				for (int dz = -width; dz <= width; dz++) {
					if (Math.abs(dx) + Math.abs(dz) > width) {
						continue;
					}
					int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
					BlockPos p = new BlockPos(x + dx, top, z + dz);
					BlockState s = world.getBlockState(p);
					if (FossilEvents2.plant(s)) {
						q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
						p = p.down();
						s = world.getBlockState(p);
					}
					Block b;
					if (s.isIn(BlockTags.SAND)) {
						b = Blocks.TINTED_GLASS;
					} else if (FossilEvents2.earth(s)) {
						b = hot ? Blocks.MAGMA_BLOCK : (FossilShapes.hash(x + dx, z + dz) < 0.6 ? Blocks.BLACK_CONCRETE_POWDER : Blocks.BLACKSTONE);
					} else {
						continue;
					}
					q().add(p, b.getDefaultState(), BlockQueue.ANY);
				}
			}
		}
	}
}
