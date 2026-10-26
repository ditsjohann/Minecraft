package com.armageddon.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * The 32 CHUNKS dice table: an 8 x 4 grid of chunks around the bomb, each rolled once. Server and client derive
 * the same seed from the bomb's position and heading, so the cutscene can name every roll before the blocks arrive.
 */
public final class ChunkRolls {
	/** Grid width and depth in chunks, and the number of chunks. Cell index = x + z * W. */
	public static final int W = 8, H = 4, CELLS = W * H;
	/** First roll, ticks between rolls, and the cutscene length. */
	public static final int ROLL_START = 90, GAP = 16, CINE = ROLL_START + CELLS * GAP + 70;

	public enum Kind {
		FLOOD("FLOOD", 0x40C0FF, true),
		UPSIDE_DOWN("UPSIDE DOWN", 0xFFB040, true),
		SKY_ISLAND("SKY ISLAND", 0x80FFE0, true),
		ROTATE("ROTATE 90°", 0xC080FF, true),
		SWAP("SWAP", 0xFFFF60, true),
		OCEAN("OCEAN", 0x2060FF, true),
		LAVA("THE FLOOR IS LAVA", 0xFF4010, true),
		VOID("VOID", 0x502080, true),
		RANDOMIZE("RANDOMIZE", 0xFF60C0, true),
		CHECKERBOARD("CHECKERBOARD", 0xFFFFFF, true),
		XRAY("X-RAY", 0xA0FFFF, true),
		PILLARS("PILLARS", 0x60FF60, true),
		MOB_PARTY("MOB PARTY", 0xFF9060, false),
		SHRINK_RAY("SHRINK RAY", 0xFF40FF, true),
		GRAVITY_FLIP("GRAVITY FLIP", 0xB0B0FF, false);

		public final String label;
		public final int color;
		/** Needs the block-damage game rule; otherwise the roll is only shown. */
		public final boolean destructive;

		Kind(String label, int color, boolean destructive) {
			this.label = label;
			this.color = color;
			this.destructive = destructive;
		}
	}

	/** What a flood fills with. {@code height} is how deep it gets; SAND rains down from above instead. */
	public enum Flood {
		SLIME("SLIME", 5), HONEY("HONEY", 4), SAND("SAND RAIN", 10), WOOL("PINK WOOL", 8), GLASS("GLASS", 10),
		GOLD("GOLD", 3), CAKE("CAKE", 1), TNT("LIT TNT", 2), WATER("WATER", 7), POWDER_SNOW("POWDER SNOW", 6),
		MELON("MELONS", 4), LEAVES("LEAVES", 9);

		public final String label;
		public final int height;

		Flood(String label, int height) {
			this.label = label;
			this.height = height;
		}
	}

	/** One chunk's result: the mutation, and a variant (flood type, or the partner chunk of a swap). */
	public record Roll(Kind kind, int variant) {
		public String label() {
			return kind == Kind.FLOOD ? "FLOOD: " + Flood.values()[variant].label : kind == Kind.SWAP ? "SWAP WITH #" + (variant + 1) : kind.label;
		}
	}

	public static long seed(Vec3d anchor, Vec3d heading) {
		long h = MathHelper.floor(anchor.x) * 341873128712L + MathHelper.floor(anchor.z) * 132897987541L;
		h += (long) Math.floor(heading.x * 1000) * 7919L + (long) Math.floor(heading.z * 1000) * 104729L;
		return h ^ 0x5DEECE66DL;
	}

	/** Chunk coordinates of the grid's north-west chunk: the bomb sits in grid cell (4, 2). */
	public static int originChunkX(Vec3d anchor) {
		return (MathHelper.floor(anchor.x) >> 4) - W / 2;
	}

	public static int originChunkZ(Vec3d anchor) {
		return (MathHelper.floor(anchor.z) >> 4) - H / 2;
	}

	/** The order the chunks roll in. */
	public static int[] order(long seed) {
		int[] o = new int[CELLS];
		for (int i = 0; i < CELLS; i++) {
			o[i] = i;
		}
		Random r = new Random(seed);
		for (int i = CELLS - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			int t = o[i];
			o[i] = o[j];
			o[j] = t;
		}
		return o;
	}

	/** What chunk {@code cell} rolls. */
	public static Roll roll(long seed, int cell) {
		Random r = new Random(seed * 31 + cell * 7717L);
		Kind[] kinds = Kind.values();
		Kind k = r.nextInt(4) == 0 ? Kind.FLOOD : kinds[1 + r.nextInt(kinds.length - 1)];
		int variant = 0;
		if (k == Kind.FLOOD) {
			variant = r.nextInt(Flood.values().length);
		} else if (k == Kind.SWAP) {
			variant = (cell + 1 + r.nextInt(CELLS - 1)) % CELLS;
		}
		return new Roll(k, variant);
	}

	/** Tick at which cell {@code order[slot]} rolls. */
	public static int rollTick(int slot) {
		return ROLL_START + slot * GAP;
	}

	private ChunkRolls() {
	}
}
