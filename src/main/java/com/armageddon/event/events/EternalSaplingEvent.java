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
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ETERNAL SAPLING: a sapling climbs a ladder of ever bigger trees (small, young, big, great, giant, titan,
 * colossus), then becomes a world tree built from real blocks, whose roots tear out of the trunk and slam into
 * the ground on every side, and the land around every one of them turns to moss and forest.
 * Timeline in {@link EternalTree}.
 */
public class EternalSaplingEvent extends WorldEvent {
	private static final int BUDGET = 2500;

	private record Put(BlockPos pos, BlockState state, boolean force) {
	}

	private List<Put> queue = new ArrayList<>();
	/** Forest spreading out where the roots came down, planted with whatever budget the tree leaves over. */
	private final List<Put> greens = new ArrayList<>();
	private int greenHead;
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
		for (int i = 0; i < EternalTree.STAGES; i++) {
			if (t == EternalTree.STAGE_START[i]) {
				// Each stage grows a bigger tree over the last, louder and heavier every time.
				stage(i == 0 ? smallTree() : stageTree(i), EternalTree.STAGE_END[i]);
				double h = EternalTree.stageHeight(i, s);
				Kit.sound(world, baseV, i < 2 ? SoundEvents.BLOCK_AZALEA_LEAVES_PLACE : SoundEvents.BLOCK_WOOD_PLACE, 8.0F + i * 6, 0.7F - i * 0.05F);
				Kit.sound(world, baseV, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 8.0F + i * 6, 0.6F + i * 0.1F);
				if (i >= 3) {
					Kit.sound(world, baseV, ModSounds.NUKE_RUMBLE, 16.0F + i * 6, 0.6F - i * 0.03F);
				}
				Vfx.ring(world, baseV.add(0, 0.3, 0), (float) (h * 0.9 + 4), 0.5F + i * 0.3F, 0x80FF90, i >= 2, 20 + i * 4);
				Vfx.shake(world, baseV, 60 + h * 4, 0.2F + i * 0.12F);
				Vfx.light(world, baseV.add(0, h * 0.7, 0), 0.6F + i * 0.06F, 0xA0FF90);
			}
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
		if (t > EternalTree.STAGE_START[1] && t < EternalTree.WORLD_END && t % 2 == 0) {
			clearTrunk(t);
		}
		if (t == EternalTree.WORLD_END) {
			Vfx.light(world, baseV.add(0, EternalTree.height(s), 0), 1.2F, 0xC8FF90);
			Kit.sound(world, baseV.add(0, EternalTree.height(s), 0), SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
		}
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			root(i, t);
		}
		while (greenHead < greens.size() && budget > 0) {
			place(greens.get(greenHead++));
			budget--;
		}
		boolean built = head >= queue.size() && slams >= EternalTree.ROOTS && greenHead >= greens.size();
		return (t > EternalTree.LIFE && built) || t > EternalTree.LIFE + 200;
	}

	@Override
	public void finish() {
		queue = new ArrayList<>();
		head = 0;
		greens.clear();
		greenHead = 0;
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
		int k = Math.max(0, EternalTree.stageAt(t));
		double r = worldStage ? EternalTree.trunkRadius(s) + 7 * s + 2 : EternalTree.stageRadius(k, s) + 3;
		double top = worldStage ? EternalTree.front(s, t) : EternalTree.stageHeight(k, s) + 4;
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
			crown(c, r, squash, salt, glow, 0);
		}

		/**
		 * As above with a leaf palette (0 green, 1 blossom, 2 deep), and, for the glowing crowns, glow berries,
		 * spore blossoms and hanging roots dangling from the underside.
		 */
		void crown(Vec3d c, double r, double squash, int salt, boolean glow, int palette) {
			List<BlockPos> under = new ArrayList<>();
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
							leaf(p, leafState(h, palette));
							if (glow && dy < -ry * 0.25 && n > inner) {
								under.add(p);
							}
						}
					}
				}
			}
			for (BlockPos p : under) {
				if (map.containsKey(p.down().asLong())) {
					continue;
				}
				double h = h3(p, salt + 77);
				if (h < 0.07) {
					// Glow berries: a short vine ending in a lit berry.
					int len = 1 + (int) (h3(p, salt + 78) * 4);
					for (int k = 1; k <= len; k++) {
						BlockState vine = (k == len ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).getDefaultState().with(Properties.BERRIES, h3(p, k) < 0.7);
						leaf(p.down(k), vine);
					}
				} else if (h < 0.095) {
					leaf(p.down(), Blocks.SPORE_BLOSSOM.getDefaultState());
				} else if (h < 0.13) {
					leaf(p.down(), Blocks.HANGING_ROOTS.getDefaultState());
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
		return leafState(h, 0);
	}

	/** Leaf mix per crown: 0 lush green, 1 blossom (cherry and flowering azalea), 2 deep (dark oak and jungle). */
	private static BlockState leafState(double h, int palette) {
		BlockState st = switch (palette) {
			case 1 -> h < 0.5 ? Blocks.CHERRY_LEAVES.getDefaultState() : h < 0.8 ? Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState()
					: Blocks.AZALEA_LEAVES.getDefaultState();
			case 2 -> h < 0.45 ? Blocks.DARK_OAK_LEAVES.getDefaultState() : h < 0.75 ? Blocks.JUNGLE_LEAVES.getDefaultState()
					: Blocks.AZALEA_LEAVES.getDefaultState();
			default -> h < 0.5 ? Blocks.OAK_LEAVES.getDefaultState() : h < 0.78 ? Blocks.AZALEA_LEAVES.getDefaultState()
					: Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState();
		};
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

	/**
	 * One rung of the ladder: a trunk of height and radius from {@link EternalTree}, more and longer branches the
	 * higher the stage, each with its own crown, and a crown on top. The species changes as it climbs: oak, then
	 * birch-barked, then dark oak, then mangrove-flared, with blossom and glow creeping into the leaves.
	 */
	private List<Put> stageTree(int i) {
		Plan plan = new Plan();
		BlockState wood = switch (i) {
			case 1 -> Blocks.BIRCH_WOOD.getDefaultState();
			case 2, 3 -> Blocks.OAK_WOOD.getDefaultState();
			case 4 -> Blocks.SPRUCE_WOOD.getDefaultState();
			case 5 -> Blocks.MANGROVE_WOOD.getDefaultState();
			default -> Blocks.DARK_OAK_WOOD.getDefaultState();
		};
		double hgt = EternalTree.stageHeight(i, s), rad = EternalTree.stageRadius(i, s);
		int h = MathHelper.ceil(hgt);
		for (int y = -1; y < h; y++) {
			double r = rad * (1 - 0.35 * y / hgt) + (y < 3 ? (3 - y) * rad * 0.3 : 0);
			int ri = MathHelper.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						BlockPos p = base.add(dx, y, dz);
						// Moss climbs the bark of the older trees.
						boolean mossy = i >= 3 && y < hgt * 0.3 && dx * dx + dz * dz > (r - 1) * (r - 1) && h3(p, 70 + i) < 0.35;
						plan.wood(p, mossy ? Blocks.MOSS_BLOCK.getDefaultState() : wood);
					}
				}
			}
		}
		int branches = 3 + i;
		for (int k = 0; k < branches; k++) {
			double a = k * Math.PI * 2 / branches + EternalTree.hash(k, 11 + i) * 0.6;
			double y0 = hgt * (0.45 + 0.3 * EternalTree.hash(k, 12 + i));
			double len = hgt * (0.3 + 0.15 * EternalTree.hash(k, 13 + i)), rise = hgt * (0.18 + 0.1 * EternalTree.hash(k, 14 + i));
			Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
			Vec3d p0 = baseV.add(0, y0, 0), p2 = baseV.add(d.multiply(len)).add(0, y0 + rise, 0);
			double thick = Math.max(0.9, rad * 0.5);
			for (double u = 0; u <= 1; u += Math.min(0.08, 1.2 / len)) {
				plan.woodBall(p0.lerp(p2, u).add(0, Math.sin(u * Math.PI) * hgt * 0.05, 0), thick * (1 - u * 0.5), wood);
			}
			plan.crown(p2.add(0, 2, 0), Math.max(3, hgt * (0.18 + 0.05 * EternalTree.hash(k, 15 + i))), 0.68, 20 + k + i * 31, i >= 5, i >= 4 ? k % 3 : 0);
		}
		plan.crown(baseV.add(0, hgt + 2, 0), Math.max(3.5, hgt * 0.28), 0.65, 30 + i, i >= 5, i >= 4 ? 1 : 0);
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
					boolean outer = d > r - 1.2;
					boolean glow = outer && spiral - Math.floor(spiral) < 0.035;
					BlockPos p = base.add(dx, y, dz);
					BlockState st = bark;
					if (glow) {
						// Three veins, three colours.
						int arm = Math.floorMod((int) Math.floor(spiral), 3);
						st = arm == 0 ? vein : arm == 1 ? Blocks.PEARLESCENT_FROGLIGHT.getDefaultState() : Blocks.OCHRE_FROGLIGHT.getDefaultState();
					} else if (outer) {
						// Bark texture: vertical streaks of stripped wood, mangrove burls and moss creeping up from the roots.
						double streak = EternalTree.hash((int) Math.floor(Math.atan2(dz, dx) * 9), 61);
						double n = h3(p, 62);
						if (y < 22 * s && n < 0.45 - y / (60 * s)) {
							st = Blocks.MOSS_BLOCK.getDefaultState();
						} else if (streak < 0.18) {
							st = Blocks.STRIPPED_DARK_OAK_WOOD.getDefaultState();
						} else if (n < 0.05) {
							st = Blocks.MANGROVE_WOOD.getDefaultState();
						}
					}
					plan.wood(p, st);
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
						// Undergrowth: moss carpet, azalea bushes, pink petals, ferns.
						double u = EternalTree.hash(dx * 19 + 7, dz * 23 + 11);
						BlockPos up = top.up();
						if (u < 0.08) {
							plan.leaf(up, (u < 0.04 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).getDefaultState());
						} else if (u < 0.2) {
							plan.leaf(up, Blocks.PINK_PETALS.getDefaultState().with(Properties.FLOWER_AMOUNT, 1 + (int) (u * 40) % 4)
									.with(Properties.HORIZONTAL_FACING, Direction.fromHorizontal((int) (u * 100) % 4)));
						} else if (u < 0.3) {
							plan.leaf(up, Blocks.FERN.getDefaultState());
						} else if (u < 0.55) {
							plan.leaf(up, Blocks.MOSS_CARPET.getDefaultState());
						}
					}
				}
			}
		}
		// Branches, each ending in a vast crown; one more crown on top.
		for (int k = 0; k < 11; k++) {
			double a = k * Math.PI * 2 / 11 + (EternalTree.hash(k, 41) - 0.5) * 0.4;
			double y0 = hgt * (0.5 + 0.35 * EternalTree.hash(k, 42));
			double len = (36 + 24 * EternalTree.hash(k, 43)) * s, rise = (22 + 18 * EternalTree.hash(k, 44)) * s;
			Vec3d d = new Vec3d(Math.cos(a), 0, Math.sin(a));
			Vec3d p0 = baseV.add(0, y0, 0), p1 = baseV.add(d.multiply(len * 0.5)).add(0, y0 + rise * 0.2, 0), p2 = baseV.add(d.multiply(len)).add(0, y0 + rise, 0);
			double steps = Math.max(10, len * 1.6);
			for (int i = 0; i <= steps; i++) {
				double u = i / steps, v = 1 - u;
				Vec3d p = p0.multiply(v * v).add(p1.multiply(2 * v * u)).add(p2.multiply(u * u));
				plan.woodBall(p, (4.4 * s + 0.8) * (1 - u) + 1.3 * u, bark);
			}
			plan.crown(p2.add(0, 3 * s, 0), (15 + 6 * EternalTree.hash(k, 45)) * s, 0.62, 100 + k, true, k % 3);
			plan.crown(p1.add(0, 4 * s, 0), 9 * s + 2, 0.6, 200 + k, true, (k + 1) % 3);
		}
		plan.crown(baseV.add(0, hgt + 4 * s, 0), 25 * s, 0.6, 300, true, 1);
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
						Put ex = map.get(p.asLong());
						if (dd > r * r || (ex != null && ex.force())) {
							continue;
						}
						boolean shell = dd > (r - 1) * (r - 1);
						double hh = h3(p, 9);
						double side = Vec3d.ofCenter(p).y - c.y;
						BlockState st;
						if (shell && hh < 0.14) {
							st = Blocks.MANGROVE_ROOTS.getDefaultState();
						} else if (shell && side > r * 0.45 && hh < 0.4) {
							st = Blocks.MOSS_BLOCK.getDefaultState();
						} else if (shell && hh > 0.93) {
							st = Blocks.ROOTED_DIRT.getDefaultState();
						} else {
							st = Blocks.DARK_OAK_WOOD.getDefaultState();
						}
						map.put(p.asLong(), new Put(p, st, true));
						if (shell && side < -r * 0.5 && hh > 0.86 && u < 0.85) {
							BlockPos d = p.down();
							map.putIfAbsent(d.asLong(), new Put(d, Blocks.HANGING_ROOTS.getDefaultState(), false));
						}
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
		forest(e, (int) Math.round(18 + 16 * s), i);
		Vfx.ring(world, e.add(0, 0.6, 0), (float) (18 + 16 * s), 1.5F, 0x60D060, false, 50);
		Kit.sound(world, e, SoundEvents.BLOCK_MOSS_PLACE, 32.0F, 0.5F);
		Kit.sound(world, e, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 32.0F, 0.5F);
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, e.x, e.y + 1, e.z, 120, 10, 2, 10, 0.05);
	}

	/**
	 * Where a root comes down, life follows: the ground around it turns to moss, grass, podzol and rooted dirt, with
	 * moss carpet, ferns, azaleas, flowers, mushrooms and berry bushes on top and a few young trees among them.
	 */
	private void forest(Vec3d c, int r, int salt) {
		int cx = MathHelper.floor(c.x), cz = MathHelper.floor(c.z);
		BlockState[] plants = {Blocks.FERN.getDefaultState(), Blocks.LARGE_FERN.getDefaultState(), Blocks.AZALEA.getDefaultState(),
				Blocks.FLOWERING_AZALEA.getDefaultState(), Blocks.LILY_OF_THE_VALLEY.getDefaultState(), Blocks.BLUE_ORCHID.getDefaultState(),
				Blocks.BROWN_MUSHROOM.getDefaultState(), Blocks.RED_MUSHROOM.getDefaultState(), Blocks.SWEET_BERRY_BUSH.getDefaultState(),
				Blocks.SHORT_GRASS.getDefaultState()};
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				double edge = r * (0.8 + 0.25 * EternalTree.hash(dx * 7 + salt, dz * 11 + salt));
				if (d > edge || !world.isChunkLoaded(new BlockPos(cx + dx, 0, cz + dz))) {
					continue;
				}
				BlockPos top = Kit.surface(world, cx + dx, cz + dz);
				BlockState st = world.getBlockState(top);
				if (!(st.isIn(BlockTags.DIRT) || st.isIn(BlockTags.SAND) || st.isIn(BlockTags.BASE_STONE_OVERWORLD) || st.isOf(Blocks.GRAVEL)
						|| st.isOf(Blocks.SNOW_BLOCK) || st.isOf(Blocks.COARSE_DIRT))) {
					continue;
				}
				double h = EternalTree.hash((cx + dx) * 31 + salt, (cz + dz) * 17);
				BlockState ground = h < 0.55 ? Blocks.MOSS_BLOCK.getDefaultState() : h < 0.78 ? Blocks.GRASS_BLOCK.getDefaultState()
						: h < 0.9 ? Blocks.PODZOL.getDefaultState() : Blocks.ROOTED_DIRT.getDefaultState();
				greens.add(new Put(top, ground, true));
				double u = EternalTree.hash((cx + dx) * 13 + 5, (cz + dz) * 29 + salt);
				BlockPos up = top.up();
				if (u < 0.012 && d > 4) {
					youngTree(up, h < 0.5);
				} else if (u < 0.3) {
					greens.add(new Put(up, ground.isOf(Blocks.MOSS_BLOCK) && u < 0.15 ? Blocks.MOSS_CARPET.getDefaultState()
							: plants[(int) (u * 1000) % plants.length], false));
				}
			}
		}
	}

	/** A young oak or birch springing up in the new forest. */
	private void youngTree(BlockPos at, boolean birch) {
		BlockState log = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).getDefaultState();
		BlockState leaves = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT, true);
		int h = 4 + (int) (EternalTree.hash(at.getX(), at.getZ()) * 3);
		for (int y = h - 2; y <= h + 1; y++) {
			int r = y >= h ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && y >= h) {
						continue;
					}
					greens.add(new Put(at.add(dx, y, dz), leaves, false));
				}
			}
		}
		for (int y = 0; y < h; y++) {
			greens.add(new Put(at.up(y), log, true));
		}
	}
}
