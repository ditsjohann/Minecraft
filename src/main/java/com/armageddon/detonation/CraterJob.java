package com.armageddon.detonation;

import com.armageddon.registry.ModBlocks;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Incrementally rewrites a region of terrain. Iterates column by column inside a bounding
 * cylinder and spends a per-tick "budget" of block checks, so even a 50-block-radius crater
 * is carved smoothly over a few seconds without a lag spike.
 */
public class CraterJob implements WorldJob {
	public enum Mode {
		/** Bowl crater, vaporised core, scorched/glassed rim, fires, flattened forest. */
		NUKE,
		/** Perfect sphere of nothing. */
		ANTIMATTER,
		/** Flash-freeze: ice, snow, obsidian. */
		CRYO,
		/** Kills life but leaves structures standing. */
		NEUTRON,
		/** Vertical molten shaft (orbital strike). */
		SHAFT
	}

	private static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	final ServerWorld world;
	private final BlockPos center;
	private final float radius;
	private final Mode mode;
	private final int outer;
	private final int minY;
	private final int maxY;
	private final int width;
	private final long total;
	private final float s1, s2, s3;
	private final Random random;
	private long cursor;
	private final BlockPos.Mutable pos = new BlockPos.Mutable();
	private final BlockPos.Mutable above = new BlockPos.Mutable();

	public CraterJob(ServerWorld world, BlockPos center, float radius, Mode mode) {
		this.world = world;
		this.center = center.toImmutable();
		this.radius = radius;
		this.mode = mode;
		this.random = Random.create(center.asLong() ^ world.getTime());
		this.s1 = random.nextFloat() * 6.28F;
		this.s2 = random.nextFloat() * 6.28F;
		this.s3 = random.nextFloat() * 6.28F;
		switch (mode) {
			case NUKE -> {
				this.outer = MathHelper.ceil(radius * 2.0F);
				this.minY = center.getY() - MathHelper.ceil(radius * 0.7F) - 2;
				this.maxY = center.getY() + MathHelper.ceil(radius * 1.4F);
			}
			case ANTIMATTER -> {
				this.outer = MathHelper.ceil(radius + 2);
				this.minY = center.getY() - outer;
				this.maxY = center.getY() + outer;
			}
			case SHAFT -> {
				this.outer = MathHelper.ceil(radius + 2);
				this.minY = center.getY() - 22;
				this.maxY = center.getY() + MathHelper.ceil(radius * 3);
			}
			default -> {
				this.outer = MathHelper.ceil(radius);
				this.minY = center.getY() - MathHelper.ceil(radius * 0.6F);
				this.maxY = center.getY() + MathHelper.ceil(radius * 0.8F);
			}
		}
		this.width = outer * 2 + 1;
		this.total = (long) width * width;
	}

	@Override
	public ServerWorld world() {
		return world;
	}

	@Override
	public boolean isDone() {
		return cursor >= total;
	}

	/** Processes columns until the budget is spent. Returns the remaining budget. */
	@Override
	public int step(int budget) {
		int bottom = Math.max(minY, world.getBottomY());
		int top = Math.min(maxY, world.getTopY() - 1);
		while (budget > 0 && cursor < total) {
			int dx = (int) (cursor % width) - outer;
			int dz = (int) (cursor / width) - outer;
			cursor++;
			budget--;
			double h2 = dx * dx + dz * dz;
			if (h2 > (double) outer * outer) {
				continue;
			}
			int x = center.getX() + dx;
			int z = center.getZ() + dz;
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			WorldChunk chunk = world.getChunk(x >> 4, z >> 4);
			double theta = Math.atan2(dz, dx);
			for (int y = top; y >= bottom; y--) {
				budget--;
				pos.set(x, y, z);
				BlockState state = chunk.getBlockState(pos);
				if (state.isAir()) {
					continue;
				}
				processBlock(state, dx, y - center.getY(), dz, h2, theta);
			}
		}
		return budget;
	}

	private float noise(double theta, double phi) {
		return (float) (0.55 * Math.sin(3 * theta + s1) * Math.cos(2 * phi + s2)
				+ 0.30 * Math.sin(7 * theta + s3 + phi * 3)
				+ 0.15 * Math.sin(13 * theta - s2));
	}

	private void processBlock(BlockState state, int dx, int dy, int dz, double h2, double theta) {
		float hardness = state.getHardness(world, pos);
		if (hardness < 0) {
			return; // bedrock, barriers, end portal frames...
		}
		switch (mode) {
			case NUKE -> nuke(state, dy, h2, theta, hardness);
			case ANTIMATTER -> antimatter(state, dy, h2, theta);
			case CRYO -> cryo(state, dy, h2);
			case NEUTRON -> neutron(state, dy, h2);
			case SHAFT -> shaft(state, dy, h2, theta);
		}
	}

	private void nuke(BlockState state, int dy, double h2, double theta, float hardness) {
		double horiz = Math.sqrt(h2);
		double phi = Math.atan2(dy, horiz);
		double r = radius * (1.0 + 0.14 * noise(theta, phi));
		double vy = dy < 0 ? dy / 0.55 : dy * 0.75;
		double dist = Math.sqrt(h2 + vy * vy);

		if (dist < r) {
			boolean resistant = state.getBlock().getBlastResistance() >= 1200.0F;
			if (resistant && dist > r * 0.45) {
				return;
			}
			set(Blocks.AIR.getDefaultState());
			return;
		}

		double rel = dist / r; // 1 .. ~2
		if (rel < 1.18 && dy <= 0 && isExposed()) {
			// Glassed crater lining.
			if (state.isIn(BlockTags.SAND) || state.isOf(Blocks.SANDSTONE) || random.nextFloat() < 0.45F) {
				if (state.isOpaqueFullCube(world, pos)) {
					set(random.nextFloat() < 0.6F ? ModBlocks.TRINITITE.getDefaultState() : ModBlocks.SCORCHED_EARTH.getDefaultState());
					return;
				}
			}
		}

		float severity = (float) MathHelper.clamp((2.0 - rel) / 1.0, 0.0, 1.0); // 1 at rim, 0 at outer edge
		if (state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.FLOWERS) || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.CROPS)) {
			if (random.nextFloat() < 0.35F + severity) {
				set(Blocks.AIR.getDefaultState());
			}
			return;
		}
		if (state.isReplaceable() && state.getFluidState().isEmpty()) {
			set(Blocks.AIR.getDefaultState());
			return;
		}
		if (state.isIn(BlockTags.LOGS)) {
			if (random.nextFloat() < 0.4F + severity) {
				BlockState charred = ModBlocks.CHARRED_LOG.getDefaultState();
				if (state.contains(PillarBlock.AXIS)) {
					charred = charred.with(PillarBlock.AXIS, state.get(PillarBlock.AXIS));
				}
				set(charred);
			}
			return;
		}
		if (state.isIn(BlockTags.IMPERMEABLE) || state.isIn(BlockTags.SNOW) || state.isOf(Blocks.ICE)) {
			if (random.nextFloat() < 0.3F + severity) {
				set(Blocks.AIR.getDefaultState());
			}
			return;
		}
		if (state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WOOL) || state.isIn(BlockTags.WOODEN_STAIRS) || state.isIn(BlockTags.WOODEN_SLABS)) {
			if (random.nextFloat() < severity * 0.8F) {
				set(Blocks.AIR.getDefaultState());
			}
			return;
		}
		if ((state.isIn(BlockTags.DIRT) || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.FARMLAND) || state.isOf(Blocks.DIRT_PATH)) && isExposed()) {
			if (random.nextFloat() < 0.25F + severity) {
				set(ModBlocks.SCORCHED_EARTH.getDefaultState());
				maybeFire(0.08F * severity + 0.01F);
			}
			return;
		}
		if (severity > 0.2F && isExposed() && state.isOpaqueFullCube(world, pos)) {
			maybeFire(0.04F * severity);
		}
	}

	private void antimatter(BlockState state, int dy, double h2, double theta) {
		double dist = Math.sqrt(h2 + dy * dy);
		double r = radius * (1.0 + 0.02 * noise(theta, dy));
		if (dist < r) {
			set(Blocks.AIR.getDefaultState());
		} else if (dist < r + 1.5 && state.isOpaqueFullCube(world, pos) && random.nextFloat() < 0.06F) {
			set(Blocks.CRYING_OBSIDIAN.getDefaultState());
		}
	}

	private void cryo(BlockState state, int dy, double h2) {
		double dist = Math.sqrt(h2 + dy * dy * 1.5);
		if (dist > radius) {
			return;
		}
		double rel = dist / radius;
		FluidState fluid = state.getFluidState();
		if (fluid.isIn(FluidTags.WATER)) {
			if (state.isOf(Blocks.WATER) || state.isOf(Blocks.BUBBLE_COLUMN)) {
				set(rel < 0.35 ? Blocks.BLUE_ICE.getDefaultState() : rel < 0.7 ? Blocks.PACKED_ICE.getDefaultState() : Blocks.ICE.getDefaultState());
			}
			return;
		}
		if (fluid.isIn(FluidTags.LAVA)) {
			set(fluid.isStill() ? Blocks.OBSIDIAN.getDefaultState() : Blocks.BASALT.getDefaultState());
			return;
		}
		if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
			set(Blocks.AIR.getDefaultState());
			return;
		}
		if (state.isIn(BlockTags.LEAVES) && random.nextFloat() < 0.5F - rel * 0.3F) {
			set(Blocks.POWDER_SNOW.getDefaultState());
			return;
		}
		if (state.isReplaceable() && fluid.isEmpty() && !state.isOf(Blocks.SNOW)) {
			set(random.nextFloat() < 0.5F ? Blocks.SNOW.getDefaultState() : Blocks.AIR.getDefaultState());
			return;
		}
		if (isExposed() && state.isOpaqueFullCube(world, pos)) {
			if (rel < 0.4 && random.nextFloat() < 0.5F) {
				set(Blocks.PACKED_ICE.getDefaultState());
			} else if (random.nextFloat() < 0.85F) {
				BlockState snow = Blocks.SNOW.getDefaultState();
				if (snow.canPlaceAt(world, above)) {
					world.setBlockState(above, snow, FLAGS);
				}
			}
		}
	}

	private void neutron(BlockState state, int dy, double h2) {
		double dist = Math.sqrt(h2 + dy * dy);
		if (dist > radius) {
			return;
		}
		if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.MYCELIUM) || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.MOSS_BLOCK)) {
			set(Blocks.COARSE_DIRT.getDefaultState());
		} else if (state.isIn(BlockTags.LEAVES)) {
			if (random.nextFloat() < 0.75F) {
				set(Blocks.AIR.getDefaultState());
			}
		} else if (state.isIn(BlockTags.FLOWERS) || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.CROPS)
				|| state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.TALL_GRASS) || state.isOf(Blocks.FERN)) {
			BlockState dead = Blocks.DEAD_BUSH.getDefaultState();
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), FLAGS);
			if (random.nextFloat() < 0.4F && dead.canPlaceAt(world, pos)) {
				world.setBlockState(pos, dead, FLAGS);
			}
		}
	}

	private void shaft(BlockState state, int dy, double h2, double theta) {
		double r = radius * (1.0 + 0.1 * noise(theta, dy * 0.2));
		double horiz = Math.sqrt(h2);
		// Taper the bottom of the shaft into a rounded point.
		double depthFrac = dy < 0 ? -dy / 22.0 : 0.0;
		double localR = r * (1.0 - depthFrac * depthFrac * 0.85);
		if (horiz < localR) {
			if (dy <= -21 && random.nextFloat() < 0.5F) {
				set(Blocks.LAVA.getDefaultState());
			} else {
				set(Blocks.AIR.getDefaultState());
			}
		} else if (horiz < localR + 1.6 && state.isOpaqueFullCube(world, pos)) {
			float roll = random.nextFloat();
			if (roll < 0.35F) {
				set(Blocks.MAGMA_BLOCK.getDefaultState());
			} else if (roll < 0.55F) {
				set(Blocks.OBSIDIAN.getDefaultState());
			} else if (roll < 0.65F) {
				set(ModBlocks.SCORCHED_EARTH.getDefaultState());
			}
		} else if (dy >= -1 && horiz < localR + 6 && isExposed() && state.isOpaqueFullCube(world, pos)) {
			maybeFire(0.15F);
		}
	}

	private boolean isExposed() {
		above.set(pos.getX(), pos.getY() + 1, pos.getZ());
		return world.getBlockState(above).isAir();
	}

	private void maybeFire(float chance) {
		if (random.nextFloat() < chance) {
			above.set(pos.getX(), pos.getY() + 1, pos.getZ());
			if (world.getBlockState(above).isAir() && AbstractFireBlock.canPlaceAt(world, above, Direction.UP)) {
				world.setBlockState(above, AbstractFireBlock.getState(world, above), Block.NOTIFY_ALL);
			}
		}
	}

	private void set(BlockState state) {
		world.setBlockState(pos, state, FLAGS);
	}
}
