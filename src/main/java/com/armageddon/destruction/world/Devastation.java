package com.armageddon.destruction.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Real destruction for the cataclysms, as a front that sweeps outward: inside {@code crater} a bowl is carved and
 * lined; out to {@code blast} everything standing on the land (buildings, trees, walls, roads' furniture) is torn
 * down with a falloff, the ground is scorched, glassed near the centre and set on fire, and a lip of rubble rings
 * the crater. Set {@link TerrainJob#limit} to the front's radius each tick to show it travelling.
 */
public final class Devastation {
	/** Materials: crater lining (two), scorched ground near the centre, and how much fire to leave. */
	public record Palette(BlockState lining, BlockState liningAlt, BlockState scorch, BlockState glass, float fire) {
	}

	public static final Palette FIRE = new Palette(Blocks.BLACKSTONE.getDefaultState(), Blocks.MAGMA_BLOCK.getDefaultState(),
			Blocks.COARSE_DIRT.getDefaultState(), Blocks.TINTED_GLASS.getDefaultState(), 0.07F);
	public static final Palette VOID = new Palette(Blocks.OBSIDIAN.getDefaultState(), Blocks.CRYING_OBSIDIAN.getDefaultState(),
			Blocks.BLACKSTONE.getDefaultState(), Blocks.PURPLE_STAINED_GLASS.getDefaultState(), 0.02F);
	public static final Palette ALIEN = new Palette(Blocks.END_STONE.getDefaultState(), Blocks.PURPUR_BLOCK.getDefaultState(),
			Blocks.END_STONE.getDefaultState(), Blocks.LIME_STAINED_GLASS.getDefaultState(), 0.03F);
	public static final Palette MOON = new Palette(Blocks.TUFF.getDefaultState(), Blocks.SMOOTH_BASALT.getDefaultState(),
			Blocks.GRAVEL.getDefaultState(), Blocks.TINTED_GLASS.getDefaultState(), 0.01F);
	public static final Palette FROST = new Palette(Blocks.PACKED_ICE.getDefaultState(), Blocks.BLUE_ICE.getDefaultState(),
			Blocks.SNOW_BLOCK.getDefaultState(), Blocks.ICE.getDefaultState(), 0.0F);

	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private Devastation() {
	}

	/** The ground itself (stone, soil, sand, ice, fluids), as opposed to what stands on it. */
	public static boolean natural(BlockState s) {
		return s.isIn(BlockTags.BASE_STONE_OVERWORLD) || s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.SAND) || s.isOf(Blocks.GRAVEL)
				|| s.isOf(Blocks.CLAY) || s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.ICE) || s.isOf(Blocks.PACKED_ICE) || s.isIn(BlockTags.TERRACOTTA)
				|| s.isOf(Blocks.SANDSTONE) || s.isOf(Blocks.RED_SANDSTONE) || !s.getFluidState().isEmpty() || s.isIn(BlockTags.COAL_ORES)
				|| s.isIn(BlockTags.IRON_ORES) || s.isOf(Blocks.BEDROCK) || s.isOf(Blocks.OBSIDIAN) || s.isOf(Blocks.CALCITE) || s.isOf(Blocks.TUFF);
	}

	public static TerrainJob of(net.minecraft.server.world.ServerWorld world, Vec3d g, double crater, double blast, int base, Palette pal) {
		double depth = Math.max(4, crater * 0.32);
		return TerrainJob.disc(world, g, blast, c -> {
			double d = Math.hypot(c.x + 0.5 - g.x, c.z + 0.5 - g.z);
			int surf = c.surface();
			double n = c.noise(3);
			if (d < crater) {
				// The bowl: everything above the floor goes, the floor is lined and glassed at the bottom.
				double k = d / crater;
				int floor = base - (int) Math.round(depth * (1 - k * k) + (n - 0.5) * 2);
				int top = Math.max(c.top(), surf);
				c.clear(floor + 1, Math.min(top, floor + 90) + 1);
				c.set(floor, k < 0.35 && n < 0.5 ? pal.glass() : n < 0.25 ? pal.liningAlt() : pal.lining());
				if (k < 0.8) {
					c.set(floor - 1, pal.lining());
				}
				return;
			}
			double s = 1 - (d - crater) / Math.max(1, blast - crater);
			// Rubble lip just outside the crater.
			if (crater > 0 && d < crater * 1.2) {
				int lip = (int) Math.round((1 - (d - crater) / (crater * 0.2)) * (2 + n * 4));
				for (int y = 1; y <= lip; y++) {
					c.set(surf + y, n < 0.3 ? Blocks.GRAVEL.getDefaultState() : n < 0.6 ? Blocks.COBBLESTONE.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
				}
				surf += lip;
			}
			// Everything standing on the ground is shredded; the nearer, the more completely.
			double tear = MathHelper.clamp(s * 1.7, 0.0, 1.0);
			int top = Math.min(c.top(), surf + 64);
			for (int y = top; y > surf - 1 && y > c.bottomY(); y--) {
				BlockState st = c.get(y);
				if (st.isAir()) {
					continue;
				}
				if (y <= surf && natural(st)) {
					break;
				}
				if (c.noise(y + 11) < tear) {
					c.set(y, AIR);
				}
			}
			// Scorch the ground: blasted near the middle, burnt further out, untouched at the very edge.
			BlockState g0 = c.get(surf);
			if (s > 0.15 && (g0.isOf(Blocks.GRASS_BLOCK) || g0.isOf(Blocks.PODZOL) || g0.isOf(Blocks.MYCELIUM) || g0.isOf(Blocks.MOSS_BLOCK))) {
				c.set(surf, s > 0.55 || n < s ? pal.scorch() : Blocks.DIRT.getDefaultState());
			} else if (s > 0.75 && g0.isIn(BlockTags.SAND) && n < 0.6) {
				c.set(surf, pal.glass());
			}
			if (s > 0.6 && n < (s - 0.6) * 0.8 && natural(g0)) {
				c.set(surf, pal.liningAlt());
			}
			if (pal.fire() > 0 && c.noise(77) < pal.fire() * (0.4 + s) && c.get(surf + 1).isAir() && c.get(surf).isOpaqueFullCube(c.world(), new net.minecraft.util.math.BlockPos(c.x, surf, c.z))) {
				c.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		});
	}
}
