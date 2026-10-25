package com.armageddon.destruction.city;

import com.armageddon.event.Kit;
import com.armageddon.util.CityShapes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * BIG BANG creates instead of destroying: a fused-glass singularity scar, the land painted with the
 * arms of a new galaxy, and above it a newborn system of small planets, each its own world, drifting
 * among nebulae of stained glass and clusters of stars.
 */
public final class CosmosTerrain {
	private CosmosTerrain() {
	}

	/** The point where it began: glassed black ground and a crying-obsidian heart. */
	public static void scar(ServerWorld w, BlockQueue q, BlockPos c, int r, long seed) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > r) {
					continue;
				}
				int x = c.getX() + dx, z = c.getZ() + dz;
				BlockPos top = Kit.surface(w, x, z);
				BlockState s = d < 1.5 ? Blocks.CRYING_OBSIDIAN.getDefaultState()
						: d < r * 0.45 ? Blocks.OBSIDIAN.getDefaultState()
						: CityShapes.hash(seed, x * 7 + z * 131) < 0.55 ? Blocks.TINTED_GLASS.getDefaultState() : Blocks.BLACK_STAINED_GLASS.getDefaultState();
				q.add(top, s, BlockQueue.ANY);
				// Everything standing on the scar is gone.
				for (int y = 1; y <= 6; y++) {
					q.add(top.getX(), top.getY() + y, top.getZ(), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
				}
			}
		}
	}

	/** Two log-spiral arms painted across the ground, as if the land were a map of the new galaxy. */
	public static void galaxy(ServerWorld w, BlockQueue q, BlockPos c, int r, long seed) {
		double twist = 0.32;
		double phase0 = CityShapes.hash(seed, 3) * MathHelper.TAU;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < 9 || d > r) {
					continue;
				}
				double a = Math.atan2(dz, dx);
				// Distance (in angle) to the nearest arm of a two-armed log spiral.
				double armA = Math.log(d / 6.0) / twist + phase0;
				double rel = ((a - armA) % Math.PI + Math.PI * 2) % Math.PI;
				double off = Math.min(rel, Math.PI - rel) * d;
				double width = 2.2 + d * 0.1;
				int x = c.getX() + dx, z = c.getZ() + dz;
				BlockState s;
				if (off < width * 0.35) {
					s = d < r * 0.35 ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
				} else if (off < width) {
					s = CityShapes.hash(seed, x * 31 + z) < 0.5 ? Blocks.PURPUR_BLOCK.getDefaultState() : Blocks.MAGENTA_CONCRETE.getDefaultState();
				} else if (off < width * 1.6) {
					s = Blocks.BLUE_CONCRETE.getDefaultState();
				} else {
					// The dark between the arms, salted with a few stars.
					s = CityShapes.hash(seed, x * 977 + z * 31) < 0.03 ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
				}
				q.add(Kit.surface(w, x, z), s, BlockQueue.SOLID_ONLY);
			}
		}
	}

	/** One newborn world. Each type is its own little biome. */
	public static void planet(BlockQueue q, BlockPos c, double r, int type, boolean ringed, long seed) {
		int ri = MathHelper.ceil(r);
		for (int dy = -ri; dy <= ri; dy++) {
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d > r) {
						continue;
					}
					int x = c.getX() + dx, y = c.getY() + dy, z = c.getZ() + dz;
					boolean skin = d > r - 1.2;
					double n = CityShapes.noise(seed + type, x, y, z, 2.2);
					q.add(x, y, z, material(type, skin, n, dy / r), BlockQueue.AIR_ONLY);
				}
			}
		}
		if (type == 3) {
			// The jungle world grows a tiny tree on top.
			BlockPos top = c.up(ri + 1);
			for (int y = 0; y < 3; y++) {
				q.add(top.up(y), Blocks.JUNGLE_LOG.getDefaultState(), BlockQueue.AIR_ONLY);
			}
			BlockState leaves = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					for (int dy = 2; dy <= 4; dy++) {
						if (Math.abs(dx) + Math.abs(dz) + (dy - 3) * (dy - 3) <= 3) {
							q.add(top.add(dx, dy, dz), leaves, BlockQueue.AIR_ONLY);
						}
					}
				}
			}
		}
		if (ringed) {
			double tilt = 0.25 + CityShapes.hash(seed, type + 77) * 0.35;
			int rr = (int) (r * 2.1);
			for (int dx = -rr; dx <= rr; dx++) {
				for (int dz = -rr; dz <= rr; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < r * 1.5 || d > r * 2.1) {
						continue;
					}
					int dy = (int) Math.round(dx * tilt);
					BlockState s = ((int) d) % 2 == 0 ? Blocks.WHITE_STAINED_GLASS.getDefaultState() : Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState();
					q.add(c.getX() + dx, c.getY() + dy, c.getZ() + dz, s, BlockQueue.AIR_ONLY);
				}
			}
		}
	}

	private static BlockState material(int type, boolean skin, double n, double lat) {
		return switch (type) {
			case 0 -> skin ? (n > 0.55 ? Blocks.AMETHYST_BLOCK : Blocks.CALCITE).getDefaultState()
					: (n > 0.7 ? Blocks.BUDDING_AMETHYST : Blocks.SMOOTH_BASALT).getDefaultState();
			case 1 -> skin ? (n > 0.62 ? Blocks.MAGMA_BLOCK : n > 0.4 ? Blocks.BLACKSTONE : Blocks.BASALT).getDefaultState()
					: Blocks.SHROOMLIGHT.getDefaultState();
			case 2 -> skin ? (n > 0.66 ? Blocks.SEA_LANTERN : n > 0.4 ? Blocks.PRISMARINE : Blocks.DARK_PRISMARINE).getDefaultState()
					: Blocks.PRISMARINE_BRICKS.getDefaultState();
			case 3 -> !skin ? Blocks.ROOTED_DIRT.getDefaultState()
					: n > 0.5 ? Blocks.MOSS_BLOCK.getDefaultState()
					: Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
			case 4 -> skin ? (Math.abs(lat) > 0.6 ? Blocks.SNOW_BLOCK : n > 0.5 ? Blocks.PACKED_ICE : Blocks.BLUE_ICE).getDefaultState()
					: Blocks.PACKED_ICE.getDefaultState();
			case 5 -> skin ? (n > 0.66 ? Blocks.OXIDIZED_COPPER : n > 0.45 ? Blocks.WEATHERED_COPPER : n > 0.25 ? Blocks.EXPOSED_COPPER : Blocks.COPPER_BLOCK).getDefaultState()
					: Blocks.RAW_COPPER_BLOCK.getDefaultState();
			default -> skin ? (n > 0.6 ? Blocks.PURPUR_BLOCK : Blocks.END_STONE).getDefaultState()
					: Blocks.END_STONE_BRICKS.getDefaultState();
		};
	}

	/** A loose cloud of coloured glass: thin enough to see through, dense enough to read as a nebula. */
	public static void nebula(BlockQueue q, BlockPos c, int r, int index, long seed) {
		BlockState[] tints = {
				Blocks.MAGENTA_STAINED_GLASS.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState(),
				Blocks.CYAN_STAINED_GLASS.getDefaultState(), Blocks.BLUE_STAINED_GLASS.getDefaultState(),
				Blocks.PINK_STAINED_GLASS.getDefaultState(), Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState()};
		BlockState a = tints[index % tints.length], b = tints[(index + 2) % tints.length];
		for (int dy = -r / 2; dy <= r / 2; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double e = (dx * dx + dz * dz) / (double) (r * r) + dy * dy * 4.0 / (r * r);
					if (e > 1) {
						continue;
					}
					int x = c.getX() + dx, y = c.getY() + dy, z = c.getZ() + dz;
					double n = CityShapes.noise(seed + 40 + index, x, y, z, 3.5);
					if (n > 0.58 + e * 0.15) {
						q.add(x, y, z, n > 0.68 ? b : a, BlockQueue.AIR_ONLY);
					}
				}
			}
		}
	}

	/** Scattered stars through the new sky. */
	public static void stars(BlockQueue q, Vec3d point, double scale, long seed) {
		BlockState[] kinds = {Blocks.GLOWSTONE.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState(),
				Blocks.PEARLESCENT_FROGLIGHT.getDefaultState(), Blocks.OCHRE_FROGLIGHT.getDefaultState(), Blocks.VERDANT_FROGLIGHT.getDefaultState()};
		for (int i = 0; i < 260; i++) {
			double a = CityShapes.hash(seed, i * 3) * MathHelper.TAU;
			double d = Math.sqrt(CityShapes.hash(seed, i * 3 + 1)) * scale;
			double y = scale * (0.2 + 0.75 * CityShapes.hash(seed, i * 3 + 2));
			BlockPos p = BlockPos.ofFloored(point.x + Math.cos(a) * d, point.y + y, point.z + Math.sin(a) * d);
			q.add(p, kinds[i % kinds.length], BlockQueue.AIR_ONLY);
		}
	}
}
