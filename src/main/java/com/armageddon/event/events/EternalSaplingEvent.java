package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.EternalTree;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ETERNAL SAPLING: a sapling becomes a tree, a bigger tree, then a world tree built from real blocks,
 * whose roots tear out of the trunk and slam into the ground on every side. Timeline in {@link EternalTree}.
 */
public class EternalSaplingEvent extends WorldEvent {
	private static final int BUDGET = 2500;

	private record Put(BlockPos pos, BlockState state, boolean force) {
	}

	private List<Put> queue = new ArrayList<>();
	private int head, stageEnd;
	@SuppressWarnings("unchecked")
	private final List<Put>[] roots = new List[EternalTree.ROOTS];
	private final int[] rootHead = new int[EternalTree.ROOTS];
	private final double[] rootGround = new double[EternalTree.ROOTS];
	private final boolean[] slammed = new boolean[EternalTree.ROOTS];
	private int slams;
	private Vec3d fwd;
	private BlockPos base;
	private Vec3d baseV;
	private double s = 1;
	private int budget;

	@Override
	public boolean tick() {
		int t = age();
		Vec3d g = center();
		if (t == 0) {
			fwd = Kit.dir(data.getFloat("Yaw"));
			base = Kit.surface(world, MathHelper.floor(g.x), MathHelper.floor(g.z)).up();
			baseV = Vec3d.ofBottomCenter(base);
			s = MathHelper.clamp(Destruction.radius(world, 140.0F) / 140.0, 0.3, 1.0);
			// The aura first, so the cutscene can read the tree's scale from it.
			Vfx.custom(world, FxType.ETERNAL_TREE, baseV, baseV, (float) s, 0x60FF90, 0, 0, 0, EternalTree.LIFE + 600, Vfx.FAR);
			WorldCutsceneEvents.begin(this, world, g, "block.armageddon.eternal_sapling", 4, fwd, EternalTree.CINE);
			place(new Put(base, Blocks.OAK_SAPLING.getDefaultState(), false));
			Kit.sound(world, baseV, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 4.0F, 0.6F);
			Vfx.light(world, baseV.add(0, 1, 0), 0.8F, 0x80FF90);
		}
		budget = BUDGET;
		if (t < EternalTree.TREE1_START && t % 15 == 0) {
			world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, baseV.x, baseV.y + 0.5, baseV.z, 12, 0.4, 0.4, 0.4, 0.02);
			Kit.sound(world, baseV, SoundEvents.ITEM_BONE_MEAL_USE, 3.0F, 0.6F + t * 0.01F);
		}
		if (t == EternalTree.TREE1_START) {
			stage(smallTree(), EternalTree.TREE1_END);
			Kit.sound(world, baseV, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 8.0F, 0.6F);
			Vfx.ring(world, baseV.add(0, 0.3, 0), 8.0F, 0.5F, 0x80FF90, false, 20);
		}
		if (t == EternalTree.TREE2_START) {
			stage(bigTree(), EternalTree.TREE2_END);
			Kit.sound(world, baseV, SoundEvents.BLOCK_WOOD_PLACE, 16.0F, 0.4F);
			Vfx.ring(world, baseV.add(0, 0.3, 0), 22.0F, 1.0F, 0x80FF90, true, 30);
			Vfx.shake(world, baseV, 160, 0.6F);
		}
		if (t == EternalTree.WORLD_START) {
			stage(worldTree(), EternalTree.WORLD_END);
			Kit.sound(world, baseV, ModSounds.NUKE_RUMBLE, 32.0F, 0.4F);
			Vfx.ring(world, baseV.add(0, 0.3, 0), 60.0F, 2.0F, 0x80FF90, true, 40);
			Vfx.skyTint(world, g, 100000, 0x2A6A3A, 0.45F);
		}
		drain(t);
		if (t > EternalTree.WORLD_START && t < EternalTree.WORLD_END) {
			if (t % 10 == 0) {
				Vfx.shake(world, baseV, 260, 0.5F + 0.6F * (t - EternalTree.WORLD_START) / (EternalTree.WORLD_END - EternalTree.WORLD_START));
				Kit.sound(world, baseV.add(0, EternalTree.front(s, t), 0), SoundEvents.BLOCK_WOOD_PLACE, 24.0F, 0.3F);
			}
			if (t % 30 == 0) {
				Kit.sound(world, baseV, ModSounds.NUKE_RUMBLE, 32.0F, 0.35F);
			}
		}
		if (t > EternalTree.TREE2_START && t < EternalTree.WORLD_END && t % 2 == 0) {
			clearTrunk(t);
		}
		if (t == EternalTree.WORLD_END) {
			Vfx.light(world, baseV.add(0, EternalTree.height(s), 0), 1.2F, 0xC8FF90);
			Kit.sound(world, baseV.add(0, EternalTree.height(s), 0), SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
		}
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			root(i, t);
		}
		boolean built = head >= queue.size() && slams >= EternalTree.ROOTS;
		return (t > EternalTree.LIFE && built) || t > EternalTree.LIFE + 200;
	}

	@Override
	public void finish() {
		queue = new ArrayList<>();
		head = 0;
	}

	// --- building ---------------------------------------------------------------------------

	/** Swaps in the next stage's blueprint (finishing anything the last one left) to be built by {@code end}. */
	private void stage(List<Put> next, int end) {
		while (head < queue.size()) {
			place(queue.get(head++));
		}
		queue = next;
		head = 0;
		stageEnd = end;
	}

	/** Builds bottom-up at whatever rate finishes the stage on time, within the per-tick budget. */
	private void drain(int t) {
		int left = queue.size() - head;
		if (left <= 0) {
			return;
		}
		int n = Math.min(budget, MathHelper.ceil(left / (double) Math.max(1, stageEnd - t)));
		for (int i = 0; i < n; i++) {
			place(queue.get(head++));
		}
		budget -= n;
	}

	private void place(Put p) {
		if (!world.isChunkLoaded(p.pos())) {
			return;
		}
		BlockState old = world.getBlockState(p.pos());
		if (old.hasBlockEntity()) {
			return;
		}
		if (!p.force() && !(old.isAir() || old.isReplaceable() || old.isIn(BlockTags.LEAVES))) {
			return;
		}
		Destruction.set(world, p.pos(), p.state(), Kit.FLAGS);
	}

	/** Keeps creatures from being entombed in the growing trunk: anything inside it is shoved out. */
	private void clearTrunk(int t) {
		boolean worldStage = t >= EternalTree.WORLD_START;
		double r = worldStage ? EternalTree.trunkRadius(s) + 5 * s + 2 : 4;
		double top = worldStage ? EternalTree.front(s, t) : 30;
		for (LivingEntity e : Kit.living(world, baseV.add(0, top * 0.5, 0), Math.max(r, top))) {
			double dx = e.getX() - baseV.x, dz = e.getZ() - baseV.z;
			double d = Math.sqrt(dx * dx + dz * dz);
			if (d < r && e.getY() > baseV.y - 3 && e.getY() < baseV.y + top) {
				Vec3d out = d < 0.1 ? fwd : new Vec3d(dx / d, 0, dz / d);
				Kit.velocity(e, out.multiply(1.1).add(0, 0.45, 0));
				e.fallDistance = 0;
			}
		}
	}

	/** Blueprint builder: wood always wins over leaves; leaves never overwrite terrain. */
	private final class Plan {
		final Map<Long, Put> map = new LinkedHashMap<>();

		void wood(BlockPos p, BlockState st) {
			map.put(p.asLong(), new Put(p, st, true));
		}

		void leaf(BlockPos p, BlockState st) {
			map.putIfAbsent(p.asLong(), new Put(p, st, false));
		}

		void woodBall(Vec3d c, double r, BlockState st) {
			int ri = MathHelper.ceil(r);
			BlockPos cb = BlockPos.ofFloored(c);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dy = -ri; dy <= ri; dy++) {
					for (int dz = -ri; dz <= ri; dz++) {
						BlockPos p = cb.add(dx, dy, dz);
						if (Vec3d.ofCenter(p).squaredDistanceTo(c) <= r * r) {
							wood(p, st);
						}
					}
				}
			}
		}

		/** A lumpy leaf crown: a full shell with a sparse interior, sprinkled with glowing shroomlights. */
		void crown(Vec3d c, double r, double squash, int salt, boolean glow) {
			int ri = MathHelper.ceil(r * 1.15);
			int ry = MathHelper.ceil(r * squash * 1.15);
			BlockPos cb = BlockPos.ofFloored(c);
			double inner = 1 - 2.6 / r;
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dy = -ry; dy <= ry; dy++) {
					for (int dz = -ri; dz <= ri; dz++) {
						double wobble = 1 + (EternalTree.hash(dx * 31 + dz * 17 + salt, salt) - 0.5) * 0.28;
						double n = Math.sqrt(dx * dx / (r * r) + dy * dy / (r * r * squash * squash) + dz * dz / (r * r)) / wobble;
						if (n > 1) {
							continue;
						}
						BlockPos p = cb.add(dx, dy, dz);
						double h = h3(p, salt);
						if (n < inner && h > 0.1) {
							continue;
						}
						if (glow && h < 0.006) {
							wood(p, Blocks.SHROOMLIGHT.getDefaultState());
						} else {
							leaf(p, leafState(h));
						}
					}
				}
			}
		}

		List<Put> sorted() {
			List<Put> out = new ArrayList<>(map.values());
			out.sort((a, b) -> Double.compare(a.pos().getY() + h3(a.pos(), 7) * 0.9, b.pos().getY() + h3(b.pos(), 7) * 0.9));
			return out;
		}
	}

	private static double h3(BlockPos p, int salt) {
		return EternalTree.hash(p.getX() * 73856093 ^ p.getZ() * 19349663, p.getY() * 83492791 + salt);
	}

	private static BlockState leafState(double h) {
		BlockState st = h < 0.55 ? Blocks.OAK_LEAVES.getDefaultState() : h < 0.82 ? Blocks.AZALEA_LEAVES.getDefaultState()
				: Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState();
		return st.with(LeavesBlock.PERSISTENT, true);
	}

	private List<Put> smallTree() {
		Plan plan = new Plan();
		for (int y = 0; y < 6; y++) {
			plan.wood(base.up(y), Blocks.OAK_LOG.getDefaultState());
		}
		for (int y = 3; y <= 6; y++) {
			int r = y < 5 ? 2 : 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (y == 6 || h3(base.add(dx, y, dz), 3) < 0.5)) {
						continue;
					}
					plan.leaf(base.add(dx, y, dz), Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
				}
			}
		}
		return plan.sorted();
	}

	private List<Put> bigTree() {
		Plan plan = new Plan();
		BlockState wood = Blocks.OAK_WOOD.getDefaultState();
		int h = 26;
		for (int y = -1; y < h; y++) {
			double r = 2.2 - y * 0.03 + (y < 3 ? (3 - y) * 0.6 : 0);
			int ri = MathHelper.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						plan.wood(base.add(dx, y, dz), wood);
					}
				}
			}
		}
		for (int k = 0; k < 5; k++) {
			double a = k * Math.PI * 2 / 5 + EternalTree.hash(k, 11) * 0.6;
			double y0 = 13 + EternalTree.hash(k, 12) * 8, len = 8 + EternalTree.hash(k, 13) * 4, rise = 5 + EternalTree.hash(k, 14) * 3;
			Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
			Vec3d p0 = baseV.add(0, y0, 0), p2 = baseV.add(d.multiply(len)).add(0, y0 + rise, 0);
			for (double u = 0; u <= 1; u += 0.08) {
				plan.woodBall(p0.lerp(p2, u).add(0, Math.sin(u * Math.PI) * 1.5, 0), 1.3 - u * 0.4, wood);
			}
			plan.crown(p2.add(0, 2, 0), 5 + EternalTree.hash(k, 15) * 1.5, 0.7, 20 + k, false);
		}
		plan.crown(baseV.add(0, h + 2, 0), 7.5, 0.65, 30, false);
		return plan.sorted();
	}

	private List<Put> worldTree() {
		Plan plan = new Plan();
		BlockState bark = Blocks.DARK_OAK_WOOD.getDefaultState();
		BlockState vein = Blocks.VERDANT_FROGLIGHT.getDefaultState();
		double hgt = EternalTree.height(s);
		int h = MathHelper.ceil(hgt);
		// Trunk: a thick shell (the hollow core is never seen), with glowing veins spiralling up the bark.
		for (int y = -3; y < h; y++) {
			double r = EternalTree.trunkRadiusAt(s, Math.max(0, y));
			int ri = MathHelper.ceil(r);
			boolean solid = y < 3 || y > h - 4;
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > r || (!solid && d < r - 2.6)) {
						continue;
					}
					double spiral = Math.atan2(dz, dx) / (Math.PI * 2) * 3 + y * 0.025;
					boolean glow = d > r - 1.2 && spiral - Math.floor(spiral) < 0.035;
					plan.wood(base.add(dx, y, dz), glow ? vein : bark);
				}
			}
		}
		// Moss creeps out over the ground around the base.
		int moss = MathHelper.ceil(EternalTree.trunkRadius(s) + 9 * s);
		for (int dx = -moss; dx <= moss; dx++) {
			for (int dz = -moss; dz <= moss; dz++) {
				if (dx * dx + dz * dz <= moss * moss && EternalTree.hash(dx * 7 + 3, dz * 13 + 5) < 0.7) {
					BlockPos top = Kit.surface(world, base.getX() + dx, base.getZ() + dz);
					if (Math.abs(top.getY() - base.getY()) < 6 && world.getBlockState(top).isOpaqueFullCube(world, top)) {
						plan.wood(top, EternalTree.hash(dx, dz) < 0.25 ? Blocks.ROOTED_DIRT.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState());
					}
				}
			}
		}
		// Branches, each ending in a vast crown; one more crown on top.
		for (int k = 0; k < 9; k++) {
			double a = k * Math.PI * 2 / 9 + (EternalTree.hash(k, 41) - 0.5) * 0.4;
			double y0 = hgt * (0.5 + 0.35 * EternalTree.hash(k, 42));
			double len = (26 + 18 * EternalTree.hash(k, 43)) * s, rise = (16 + 14 * EternalTree.hash(k, 44)) * s;
			Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
			Vec3d p0 = baseV.add(0, y0, 0), p1 = baseV.add(d.multiply(len * 0.5)).add(0, y0 + rise * 0.2, 0), p2 = baseV.add(d.multiply(len)).add(0, y0 + rise, 0);
			double steps = Math.max(10, len * 1.6);
			for (int i = 0; i <= steps; i++) {
				double u = i / steps, v = 1 - u;
				Vec3d p = p0.multiply(v * v).add(p1.multiply(2 * v * u)).add(p2.multiply(u * u));
				plan.woodBall(p, (3.4 * s + 0.8) * (1 - u) + 1.3 * u, bark);
			}
			plan.crown(p2.add(0, 3 * s, 0), (12 + 5 * EternalTree.hash(k, 45)) * s, 0.62, 100 + k, true);
			plan.crown(p1.add(0, 4 * s, 0), 7 * s + 2, 0.6, 200 + k, true);
		}
		plan.crown(baseV.add(0, hgt + 4 * s, 0), 19 * s, 0.6, 300, true);
		return plan.sorted();
	}

	// --- roots ---------------------------------------------------------------------------------

	private void root(int i, int t) {
		int launch = EternalTree.rootLaunch(i), slam = EternalTree.rootSlam(i);
		if (t < launch || slammed[i]) {
			return;
		}
		if (roots[i] == null) {
			Vec3d tip = EternalTree.rootTip(baseV, i, s);
			BlockPos tp = BlockPos.ofFloored(tip);
			rootGround[i] = world.isChunkLoaded(tp) ? Kit.ground(world, tip.x, tip.z).y : baseV.y;
			roots[i] = rootPlan(i);
			Kit.sound(world, baseV.add(0, 26 * s, 0), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 32.0F, 0.3F);
			Kit.sound(world, baseV, ModSounds.NUKE_RUMBLE, 24.0F, 0.6F);
		}
		List<Put> r = roots[i];
		int want = (int) Math.min(r.size(), r.size() * (t - launch + 1) / (double) EternalTree.ROOT_GROW);
		while (rootHead[i] < want && budget > 0) {
			place(r.get(rootHead[i]++));
			budget--;
		}
		if ((t >= slam && rootHead[i] >= r.size()) || t >= slam + 10) {
			while (rootHead[i] < r.size()) {
				place(r.get(rootHead[i]++));
			}
			slammed[i] = true;
			slamDown(i);
		}
	}

	private List<Put> rootPlan(int i) {
		Map<Long, Put> map = new LinkedHashMap<>();
		Vec3d tip = EternalTree.rootPoint(baseV, i, s, 1, rootGround[i]);
		double len = tip.distanceTo(baseV) * 1.5;
		int steps = (int) Math.max(30, len * 1.4);
		for (int k = 0; k <= steps; k++) {
			double u = k / (double) steps;
			Vec3d c = EternalTree.rootPoint(baseV, i, s, u, rootGround[i]);
			double r = (3.6 * s + 0.6) * (1 - u) + 1.4 * u;
			int ri = MathHelper.ceil(r);
			BlockPos cb = BlockPos.ofFloored(c);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dy = -ri; dy <= ri; dy++) {
					for (int dz = -ri; dz <= ri; dz++) {
						BlockPos p = cb.add(dx, dy, dz);
						double dd = Vec3d.ofCenter(p).squaredDistanceTo(c);
						if (dd > r * r || map.containsKey(p.asLong())) {
							continue;
						}
						boolean hairy = dd > (r - 1) * (r - 1) && h3(p, 9) < 0.14;
						map.put(p.asLong(), new Put(p, hairy ? Blocks.MANGROVE_ROOTS.getDefaultState() : Blocks.DARK_OAK_WOOD.getDefaultState(), true));
					}
				}
			}
		}
		return new ArrayList<>(map.values());
	}

	private void slamDown(int i) {
		slams++;
		Vec3d tipXZ = EternalTree.rootTip(baseV, i, s);
		Vec3d e = new Vec3d(tipXZ.x, rootGround[i], tipXZ.z);
		double a = EternalTree.rootAngle(i);
		Vec3d out = new Vec3d(Math.cos(a), 0, Math.sin(a));
		// The blast goes off just past the tip, so the buried root stays embedded at the crater's edge.
		Vec3d blast = e.add(out.multiply(4 + 2 * s));
		Destruction.explode(world, null, blast.x, blast.y, blast.z, (float) (4.5 + 2.5 * s), false);
		Kit.ringCrack(world, e, 8 + 6 * s, 2, null, 0.7F);
		for (int k = -1; k <= 1; k++) {
			double fa = a + k * 0.5;
			Kit.fissure(world, e, new Vec3d(Math.cos(fa), 0, Math.sin(fa)), (int) (14 + 10 * s), 3, 2, null);
		}
		Kit.damage(world, e, 14 + 10 * s, 40.0F, ModDamageTypes.SHRAPNEL, null, owner());
		for (LivingEntity le : Kit.living(world, e, 30)) {
			if (!Kit.creativeOrSpectator(le)) {
				Vec3d away = le.getPos().subtract(e);
				Vec3d flat = new Vec3d(away.x, 0, away.z);
				Kit.push(le, (flat.lengthSquared() < 0.01 ? out : flat.normalize()).multiply(0.6).add(0, 0.7, 0));
			}
		}
		Detonations.debris(world, e, 10, 28);
		Vfx.ring(world, e.add(0, 0.5, 0), (float) (40 * s + 14), 2.5F, 0x9A8A6A, true, 40);
		Vfx.ring(world, e.add(0, 0.5, 0), (float) (18 * s + 6), 1.2F, 0xC8FF90, false, 18);
		Vfx.shake(world, e, 300, 1.6F);
		Vfx.fov(world, e, 5.0F, 10);
		Vfx.light(world, e.add(0, 4, 0), 0.9F, 0xFFD8A0);
		world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, e.x, e.y + 1, e.z, 40, 6, 1.5, 6, 0.04);
		Kit.sound(world, e, ModSounds.TITAN_LANDING, 32.0F, 0.5F);
		Kit.sound(world, e, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 24.0F, 0.55F);
		if (slams == 1 || slams == EternalTree.ROOTS) {
			CityDestructionEvents.frames(world, center(), 0);
		}
	}
}
