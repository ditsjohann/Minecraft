package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.util.ChunkRolls;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/**
 * 32 CHUNKS: the 8x4 chunks around the bomb roll the dice one by one (floods of random blocks,
 * upside-down terrain, sky islands, swaps, voids, mob parties...), and every result stays.
 * Rolls come from {@link ChunkRolls} so the client cutscene names each one as it lands.
 */
public class SixteenChunksEvent extends WorldEvent {
	private static final int CINE = ChunkRolls.CINE;
	/** Block writes per tick, drained from the queue. */
	private static final int BUDGET = 2500;
	private static final int LIFT = 30;

	private record Op(BlockPos pos, BlockState state) {
	}

	private static final BlockState AIR = Blocks.AIR.getDefaultState();
	private static final Block[] PALETTE = {
			Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.BRICKS, Blocks.PRISMARINE, Blocks.QUARTZ_BLOCK, Blocks.PURPUR_BLOCK,
			Blocks.END_STONE, Blocks.NETHERRACK, Blocks.SOUL_SAND, Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.MOSS_BLOCK, Blocks.HAY_BLOCK,
			Blocks.BONE_BLOCK, Blocks.SPONGE, Blocks.CRYING_OBSIDIAN, Blocks.AMETHYST_BLOCK, Blocks.COPPER_BLOCK, Blocks.IRON_BLOCK,
			Blocks.EMERALD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.REDSTONE_BLOCK, Blocks.PUMPKIN, Blocks.MELON, Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK,
			Blocks.ICE, Blocks.PACKED_ICE, Blocks.MAGMA_BLOCK, Blocks.SHROOMLIGHT, Blocks.OCHRE_FROGLIGHT, Blocks.TARGET, Blocks.NOTE_BLOCK,
			Blocks.BOOKSHELF, Blocks.MUD_BRICKS, Blocks.BLUE_ICE, Blocks.CHERRY_LEAVES, Blocks.RED_MUSHROOM_BLOCK, Blocks.WARPED_WART_BLOCK};
	private static final Block[] CONCRETE = {
			Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE,
			Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.MAGENTA_CONCRETE,
			Blocks.PINK_CONCRETE, Blocks.WHITE_CONCRETE};
	private static final Set<Block> BORING = Set.of(
			Blocks.STONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK,
			Blocks.PODZOL, Blocks.MYCELIUM, Blocks.DIRT_PATH, Blocks.MUD, Blocks.CLAY, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND,
			Blocks.SANDSTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF, Blocks.CALCITE, Blocks.TERRACOTTA,
			Blocks.NETHERRACK, Blocks.SNOW_BLOCK, Blocks.MOSS_BLOCK, Blocks.SMOOTH_BASALT);
	private static final List<EntityType<?>> PARTY = List.of(
			EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN, EntityType.GOAT, EntityType.LLAMA, EntityType.PANDA,
			EntityType.FOX, EntityType.FROG, EntityType.PARROT, EntityType.RABBIT, EntityType.CAMEL, EntityType.ARMADILLO, EntityType.ALLAY,
			EntityType.VILLAGER, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.SLIME, EntityType.WITCH);

	private final ArrayDeque<Op> queue = new ArrayDeque<>();
	private final int[] base = new int[ChunkRolls.CELLS];
	private Vec3d fwd;
	private long seed;
	private int[] order;
	private int ox, oz;

	@Override
	public boolean tick() {
		int t = age();
		Vec3d g = center();
		if (t == 0) {
			fwd = Kit.dir(data.getFloat("Yaw"));
			seed = ChunkRolls.seed(g, fwd);
			order = ChunkRolls.order(seed);
			ox = ChunkRolls.originChunkX(g);
			oz = ChunkRolls.originChunkZ(g);
			for (int cell = 0; cell < ChunkRolls.CELLS; cell++) {
				base[cell] = loaded(cell) ? Kit.surface(world, minX(cell) + 8, minZ(cell) + 8).getY() : Integer.MIN_VALUE;
			}
			WorldCutsceneEvents.begin(this, world, g, "block.armageddon.sixteen_chunks", 5, fwd, CINE);
			Vfx.custom(world, FxType.CHUNK_GLITCH, g, fwd, 128.0F, 0xFF40FF, 0, (float) Kit.ground(world, g.x, g.z).y, 0, CINE + 300, Vfx.FAR);
			Vfx.skyTint(world, g, 100000, 0x301040, 0.5F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 32.0F, 0.6F);
		}
		for (int slot = 0; slot < ChunkRolls.CELLS; slot++) {
			int at = ChunkRolls.rollTick(slot);
			if (t < at - 14 || t > at) {
				continue;
			}
			Vec3d cc = cellCenter(order[slot]);
			if (t < at && t % 2 == 0) {
				// The reel spinning: clicks rising in pitch.
				Kit.sound(world, cc, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 16.0F, 0.6F + (t - at + 14) * 0.08F);
			}
			if (t == at) {
				apply(order[slot], ChunkRolls.roll(seed, order[slot]));
			}
		}
		if (t == ChunkRolls.rollTick(ChunkRolls.CELLS - 1) + 20) {
			// Every chunk has rolled: a fanfare, and the results stay.
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.GRID_FANFARE, g, 1.0F);
			Kit.sound(world, g, SoundEvents.ENTITY_PLAYER_LEVELUP, 48.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 48.0F, 1.0F);
		}
		int crack = ChunkRolls.rollTick(ChunkRolls.CELLS - 1) + 50;
		if (t == crack - 16) {
			// The machine overloads: the reel noise comes back all at once, too fast.
			Vfx.glitch(world, g, 140.0F, 0xFF40FF, 30);
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 48.0F, 2.0F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 48.0F, 1.4F);
		}
		if (t == crack) {
			crackGrid(g);
		}
		flush();
		return (t > CINE + 40 && t > crack + 10 && queue.isEmpty()) || t > CINE + 2400;
	}

	/**
	 * The finale: the world gives up pretending it is one piece. Every chunk border splits open into a deep trench,
	 * so the 32 rolled chunks stand apart like tiles, and glitch pillars blow out of every corner where they meet.
	 */
	private void crackGrid(Vec3d g) {
		CityDestructionEvents.frames(world, g, 3, 1, 3);
		Vfx.flash(world, g, 0.6F, 100000, 0xFF60FF);
		Vfx.shake(world, g, 100000, 2.2F);
		Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.5F);
		Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
		Kit.sound(world, g, com.armageddon.registry.ModSounds.NUKE_RUMBLE, 64.0F, 0.4F);
		for (int cell = 0; cell < ChunkRolls.CELLS; cell++) {
			if (base[cell] == Integer.MIN_VALUE || !loaded(cell)) {
				continue;
			}
			int x0 = minX(cell), z0 = minZ(cell), lo = Math.max(world.getBottomY() + 1, base[cell] - 24), hi = hi(cell) + 10;
			boolean east = cell % ChunkRolls.W == ChunkRolls.W - 1, south = cell / ChunkRolls.W == ChunkRolls.H - 1;
			for (int i = 0; i < 16; i++) {
				for (int y = hi; y >= lo; y--) {
					queue.add(new Op(new BlockPos(x0, y, z0 + i), AIR));
					queue.add(new Op(new BlockPos(x0 + i, y, z0), AIR));
					if (east) {
						queue.add(new Op(new BlockPos(x0 + 15, y, z0 + i), AIR));
					}
					if (south) {
						queue.add(new Op(new BlockPos(x0 + i, y, z0 + 15), AIR));
					}
				}
			}
			// A glitch pillar out of this tile's corner, colour by colour around the grid.
			Vec3d corner = new Vec3d(x0, base[cell] + 1, z0);
			int col = net.minecraft.util.math.MathHelper.hsvToRgb(cell / (float) ChunkRolls.CELLS, 0.7F, 1.0F);
			int delay = cell % ChunkRolls.W * 2 + cell / ChunkRolls.W * 3;
			com.armageddon.detonation.DetonationManager.schedule(world, delay, () -> {
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.CORNER_GEYSER, corner, new Vec3d(0, 1, 0), 1.0F, col);
				// The corners glitch out of existence rather than blowing up.
				com.armageddon.event.Ruin.disintegrate(world, corner, 4.0, Blocks.PEARLESCENT_FROGLIGHT.getDefaultState(), net.minecraft.particle.ParticleTypes.END_ROD, 14);
			});
		}
	}

	// --- grid ------------------------------------------------------------------------

	private int minX(int cell) {
		return (ox + cell % ChunkRolls.W) * 16;
	}

	private int minZ(int cell) {
		return (oz + cell / ChunkRolls.W) * 16;
	}

	private boolean loaded(int cell) {
		return world.isChunkLoaded(ox + cell % ChunkRolls.W, oz + cell / ChunkRolls.W);
	}

	private int lo(int cell) {
		return Math.max(world.getBottomY(), base[cell] - 20);
	}

	private int hi(int cell) {
		return Math.min(world.getTopY() - 1, base[cell] + 30);
	}

	private Vec3d cellCenter(int cell) {
		int y = base[cell] == Integer.MIN_VALUE ? (int) center().y : base[cell] + 1;
		return new Vec3d(minX(cell) + 8, y, minZ(cell) + 8);
	}

	private int columnTop(int x, int z) {
		return world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
	}

	private void put(int x, int y, int z, BlockState s) {
		if (!s.hasBlockEntity()) {
			queue.add(new Op(new BlockPos(x, y, z), s));
		}
	}

	private BlockState[][][] snapshot(int cell, int lo, int hi) {
		BlockState[][][] snap = new BlockState[16][hi - lo + 1][16];
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= hi; y++) {
					snap[x][y - lo][z] = world.getBlockState(p.set(minX(cell) + x, y, minZ(cell) + z));
				}
			}
		}
		return snap;
	}

	private void flush() {
		int n = 0;
		while (n < BUDGET && !queue.isEmpty()) {
			Op op = queue.poll();
			if (world.getBlockState(op.pos).hasBlockEntity()) {
				continue;
			}
			Destruction.set(world, op.pos, op.state, Kit.FLAGS);
			n++;
		}
	}

	// --- the dice --------------------------------------------------------------------

	private void apply(int cell, ChunkRolls.Roll roll) {
		if (base[cell] == Integer.MIN_VALUE) {
			return;
		}
		ChunkRolls.Kind kind = roll.kind();
		Vec3d cc = cellCenter(cell);
		int lo = lo(cell), hi = hi(cell);
		Vfx.wirebox(world, new Vec3d(minX(cell), lo, minZ(cell)), new Vec3d(minX(cell) + 16, hi + 1, minZ(cell) + 16), kind.color, 40);
		com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.CHUNK_REROLL, new Vec3d(cc.x, lo + 1, cc.z), new Vec3d(0, 1, 0), 1.0F, kind.color);
		Vfx.shake(world, cc, 120, 0.6F);
		Kit.sound(world, cc, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 24.0F, 0.5F + world.random.nextFloat());
		Kit.sound(world, cc, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 24.0F, 0.6F);
		if (kind.destructive && !Destruction.enabled(world)) {
			return;
		}
		switch (kind) {
			case BLOCK -> fillBlock(cell, lo, hi, ChunkRolls.block(roll.variant()).getDefaultState());
			case FLOOD -> flood(cell, ChunkRolls.Flood.values()[roll.variant()]);
			case UPSIDE_DOWN -> upsideDown(cell, lo, hi);
			case SKY_ISLAND -> skyIsland(cell, lo, hi);
			case ROTATE -> rotate(cell, lo, hi);
			case SWAP -> swap(cell, roll.variant());
			case OCEAN -> ocean(cell, hi);
			case LAVA -> lava(cell);
			case VOID -> voidOut(cell, hi);
			case RANDOMIZE -> randomize(cell, lo, hi);
			case CHECKERBOARD -> checkerboard(cell, lo, hi);
			case XRAY -> xray(cell, lo, hi);
			case PILLARS -> pillars(cell, lo, hi);
			case MOB_PARTY -> mobParty(cell);
			case SHRINK_RAY -> shrink(cell, hi);
			case GRAVITY_FLIP -> gravityFlip(cell, lo, hi);
		}
	}

	private void flood(int cell, ChunkRolls.Flood f) {
		BlockState fill = switch (f) {
			case SLIME -> Blocks.SLIME_BLOCK.getDefaultState();
			case HONEY -> Blocks.HONEY_BLOCK.getDefaultState();
			case SAND -> Blocks.SAND.getDefaultState();
			case WOOL -> Blocks.PINK_WOOL.getDefaultState();
			case GLASS -> Blocks.GLASS.getDefaultState();
			case GOLD -> Blocks.GOLD_BLOCK.getDefaultState();
			case CAKE -> Blocks.CAKE.getDefaultState();
			case TNT -> Blocks.TNT.getDefaultState();
			case WATER -> Blocks.WATER.getDefaultState();
			case POWDER_SNOW -> Blocks.POWDER_SNOW.getDefaultState();
			case MELON -> Blocks.MELON.getDefaultState();
			case LEAVES -> Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
		};
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int X = minX(cell) + x, Z = minZ(cell) + z;
				int top = columnTop(X, Z);
				// Sand is hung in the sky and left to rain down.
				int from = f == ChunkRolls.Flood.SAND ? top + 12 : top;
				for (int y = from; y < from + f.height && y < world.getTopY(); y++) {
					BlockState s = world.getBlockState(p.set(X, y, Z));
					if (s.isAir() || s.isReplaceable()) {
						put(X, y, Z, fill);
					}
				}
			}
		}
		if (f == ChunkRolls.Flood.TNT) {
			for (int i = 0; i < 14; i++) {
				double x = minX(cell) + 1 + world.random.nextDouble() * 14, z = minZ(cell) + 1 + world.random.nextDouble() * 14;
				TntEntity tnt = new TntEntity(world, x, columnTop((int) Math.floor(x), (int) Math.floor(z)) + 2.5, z, null);
				tnt.setFuse(30 + i * 5);
				world.spawnEntity(tnt);
			}
		}
	}

	private void upsideDown(int cell, int lo, int hi) {
		BlockState[][][] snap = snapshot(cell, lo, hi);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= hi; y++) {
					BlockState s = snap[x][hi - y][z];
					if (s != snap[x][y - lo][z]) {
						put(minX(cell) + x, y, minZ(cell) + z, s);
					}
				}
			}
		}
	}

	private void skyIsland(int cell, int lo, int hi) {
		int top = Math.min(hi, world.getTopY() - 1 - LIFT);
		if (top < lo) {
			return;
		}
		BlockState[][][] snap = snapshot(cell, lo, top + LIFT);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= top + LIFT; y++) {
					int src = y - LIFT;
					// Everything moves up thirty blocks; what is left behind is a pit under a floating island.
					BlockState s = src >= lo ? snap[x][src - lo][z] : AIR;
					if (s != snap[x][y - lo][z]) {
						put(minX(cell) + x, y, minZ(cell) + z, s);
					}
				}
			}
		}
	}

	private void rotate(int cell, int lo, int hi) {
		BlockState[][][] snap = snapshot(cell, lo, hi);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= hi; y++) {
					// Clockwise: old (x, z) lands at (15 - z, x), facing turned with it.
					BlockState s = snap[x][y - lo][z].rotate(BlockRotation.CLOCKWISE_90);
					int nx = 15 - z, nz = x;
					if (s != snap[nx][y - lo][nz]) {
						put(minX(cell) + nx, y, minZ(cell) + nz, s);
					}
				}
			}
		}
	}

	private void swap(int cell, int other) {
		if (base[other] == Integer.MIN_VALUE) {
			return;
		}
		int lo = Math.min(lo(cell), lo(other)), hi = Math.max(hi(cell), hi(other));
		BlockState[][][] a = snapshot(cell, lo, hi), b = snapshot(other, lo, hi);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= hi; y++) {
					BlockState sa = a[x][y - lo][z], sb = b[x][y - lo][z];
					if (sa != sb) {
						put(minX(cell) + x, y, minZ(cell) + z, sb);
						put(minX(other) + x, y, minZ(other) + z, sa);
					}
				}
			}
		}
		com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.CHUNK_SWAP, cellCenter(cell).add(0, 2, 0), cellCenter(other).subtract(cellCenter(cell)), 1.0F, 0);
	}

	private void ocean(int cell, int hi) {
		int b = base[cell];
		int floor = Math.max(world.getBottomY() + 1, b - 13);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = floor; y <= hi; y++) {
					BlockState s = y == floor ? Blocks.SAND.getDefaultState() : y <= b ? Blocks.WATER.getDefaultState() : AIR;
					put(minX(cell) + x, y, minZ(cell) + z, s);
				}
			}
		}
		Vec3d cc = cellCenter(cell);
		for (int i = 0; i < 14; i++) {
			EntityType<?> type = i < 6 ? EntityType.SQUID : i < 12 ? EntityType.COD : EntityType.DOLPHIN;
			spawn(type, cc.add(world.random.nextDouble() * 12 - 6, 0.5, world.random.nextDouble() * 12 - 6));
		}
	}

	private void lava(int cell) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int X = minX(cell) + x, Z = minZ(cell) + z;
				int top = columnTop(X, Z) - 1;
				put(X, top, Z, Blocks.LAVA.getDefaultState());
				put(X, top - 1, Z, Blocks.MAGMA_BLOCK.getDefaultState());
			}
		}
	}

	private void voidOut(int cell, int hi) {
		int floor = Math.max(world.getBottomY(), base[cell] - 40);
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = floor; y <= hi; y++) {
					put(minX(cell) + x, y, minZ(cell) + z, AIR);
				}
			}
		}
	}

	/** The common roll: the whole chunk, every solid block of it, becomes one block. */
	private void fillBlock(int cell, int lo, int hi, BlockState to) {
		forSolid(cell, lo, hi, (x, y, z, s) -> to);
	}

	private void randomize(int cell, int lo, int hi) {
		forSolid(cell, lo, hi, (x, y, z, s) -> PALETTE[world.random.nextInt(PALETTE.length)].getDefaultState());
	}

	private void checkerboard(int cell, int lo, int hi) {
		forSolid(cell, lo, hi, (x, y, z, s) -> ((x + y + z) & 1) == 0 ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
	}

	private void xray(int cell, int lo, int hi) {
		forSolid(cell, lo, hi, (x, y, z, s) -> BORING.contains(s.getBlock()) ? Blocks.GLASS.getDefaultState() : null);
	}

	private void pillars(int cell, int lo, int hi) {
		int b = base[cell];
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				int X = minX(cell) + x, Z = minZ(cell) + z;
				long h = (X >> 1) * 73428767L ^ (Z >> 1) * 912931L ^ seed;
				h = (h ^ (h >>> 13)) * 1274126177L;
				int height = b - 6 + (int) ((h >>> 20) & 31);
				for (int y = b - 4; y <= hi; y++) {
					BlockState s = y > height ? AIR : CONCRETE[Math.floorMod(y - b, CONCRETE.length)].getDefaultState();
					put(X, y, Z, s);
				}
			}
		}
	}

	private void mobParty(int cell) {
		for (int i = 0; i < 36; i++) {
			double x = minX(cell) + 1 + world.random.nextDouble() * 14, z = minZ(cell) + 1 + world.random.nextDouble() * 14;
			Entity e = spawn(PARTY.get(world.random.nextInt(PARTY.size())), new Vec3d(x, columnTop((int) Math.floor(x), (int) Math.floor(z)) + 0.2, z));
			if (e != null && e.getType() == EntityType.SHEEP) {
				e.setCustomName(Text.literal("jeb_"));
			}
		}
		Vec3d cc = cellCenter(cell);
		for (int i = 0; i < 6; i++) {
			Vfx.firework(world, cc.add(world.random.nextGaussian() * 6, 10 + world.random.nextInt(10), world.random.nextGaussian() * 6), 2.5F, i % 4, 0xFF60C0 + i * 0x1020);
		}
		Kit.sound(world, cc, SoundEvents.ENTITY_VILLAGER_CELEBRATE, 16.0F, 1.0F);
	}

	private void shrink(int cell, int hi) {
		int b = base[cell];
		int lo = Math.max(world.getBottomY(), b - 12);
		int top = Math.min(world.getTopY() - 1, b + 35);
		BlockState[][][] snap = snapshot(cell, lo, top);
		int layers = (top - lo + 1) / 4;
		// The whole chunk is carved down a few blocks...
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = Math.max(lo, b - 3); y <= hi; y++) {
					if (!snap[x][y - lo][z].isAir()) {
						put(minX(cell) + x, y, minZ(cell) + z, AIR);
					}
				}
			}
		}
		// ...and a quarter-scale copy of it sits in the middle.
		for (int mx = 0; mx < 4; mx++) {
			for (int mz = 0; mz < 4; mz++) {
				for (int my = 0; my < layers; my++) {
					BlockState pick = AIR;
					int solid = 0;
					for (int dx = 0; dx < 4; dx++) {
						for (int dz = 0; dz < 4; dz++) {
							for (int dy = 0; dy < 4; dy++) {
								BlockState s = snap[mx * 4 + dx][my * 4 + dy][mz * 4 + dz];
								if (!s.isAir() && s.getFluidState().isEmpty()) {
									solid++;
									pick = s;
								}
							}
						}
					}
					if (solid >= 20) {
						put(minX(cell) + 6 + mx, b - 3 + my, minZ(cell) + 6 + mz, pick);
					}
				}
			}
		}
		Vfx.beam(world, cellCenter(cell).add(0, 40, 0), cellCenter(cell), 2.0F, 0xFF40FF, 20);
	}

	private void gravityFlip(int cell, int lo, int hi) {
		Box box = new Box(minX(cell), lo, minZ(cell), minX(cell) + 16, hi + 40, minZ(cell) + 16);
		for (Entity e : world.getOtherEntities(null, box, x -> !Kit.creativeOrSpectator(x) && !(x instanceof com.armageddon.event.EventEntity))) {
			if (e instanceof LivingEntity l) {
				l.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 100, 3, false, true));
			} else {
				Kit.push(e, new Vec3d(0, 1.2, 0));
			}
		}
		Vec3d cc = cellCenter(cell);
		Vfx.gravityFlip(world, cc, 12.0F, 100);
		if (Destruction.enabled(world)) {
			for (FallingBlockEntity fb : Kit.lift(world, cc, 8, 70, true, true)) {
				fb.setNoGravity(false);
				Kit.velocity(fb, new Vec3d(world.random.nextGaussian() * 0.1, 0.9 + world.random.nextDouble() * 0.8, world.random.nextGaussian() * 0.1));
			}
		}
	}

	private Entity spawn(EntityType<?> type, Vec3d at) {
		Entity e = type.create(world);
		if (e == null) {
			return null;
		}
		e.refreshPositionAndAngles(at.x, at.y, at.z, world.random.nextFloat() * 360, 0);
		if (e instanceof MobEntity m) {
			m.setPersistent();
		}
		world.spawnEntity(e);
		return e;
	}

	@FunctionalInterface
	private interface Remap {
		/** New state for a solid block at world (x, y, z), or null to keep it. */
		BlockState map(int x, int y, int z, BlockState s);
	}

	private void forSolid(int cell, int lo, int hi, Remap remap) {
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = lo; y <= hi; y++) {
					int X = minX(cell) + x, Z = minZ(cell) + z;
					BlockState s = world.getBlockState(p.set(X, y, Z));
					if (s.isAir() || !s.getFluidState().isEmpty()) {
						continue;
					}
					BlockState n = remap.map(X, y, Z, s);
					if (n != null && n != s) {
						put(X, y, Z, n);
					}
				}
			}
		}
	}

	@Override
	public void finish() {
		queue.clear();
	}
}
