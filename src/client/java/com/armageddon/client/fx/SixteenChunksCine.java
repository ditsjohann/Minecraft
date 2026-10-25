package com.armageddon.client.fx;

import com.armageddon.util.ChunkRolls;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * 16 CHUNKS cutscene: rise from the bomb to a top-down view of the 4x4 grid, a slot-machine roll for
 * every chunk (with swoops down into a few of them), then the REROLL where all sixteen go at once.
 * The schedule and results come from {@link ChunkRolls}, the same table the server rolls from.
 */
public class SixteenChunksCine extends Cinematics.Cine {
	/** Rolls the camera swoops down into instead of watching from above. */
	private static final int[] SWOOPS = {2, 6, 10, 13};
	private static final int PRE_REROLL = ChunkRolls.REROLL - 40;
	private final long seed;
	private final int[] order;
	private final Vec3d grid;
	private final double x0, z0;

	public SixteenChunksCine(Vec3d anchor, Vec3d heading, int life) {
		super(anchor, heading, life);
		this.seed = ChunkRolls.seed(anchor, heading);
		this.order = ChunkRolls.order(seed);
		this.x0 = ChunkRolls.originChunkX(anchor) * 16;
		this.z0 = ChunkRolls.originChunkZ(anchor) * 16;
		this.grid = new Vec3d(x0 + 32, anchor.y, z0 + 32);
	}

	private Vec3d cellCenter(int cell) {
		return new Vec3d(x0 + (cell % 4) * 16 + 8, anchor.y, z0 + (cell / 4) * 16 + 8);
	}

	/** Index of the round-0 roll in progress (spinning or just landed) at time {@code a}, or -1. */
	private int activeSlot(float a) {
		for (int slot = 0; slot < 16; slot++) {
			float d = a - ChunkRolls.rollTick(slot);
			if (d >= -14 && d < ChunkRolls.GAP - 14) {
				return slot;
			}
		}
		return -1;
	}

	// --- camera -----------------------------------------------------------------------

	@Override
	protected void direct(float a, Cinematics.State s) {
		double spin = a * 0.004;
		if (a < 75) {
			// From the bomb up into the sky, tipping over to look straight down at the grid.
			double u = ease(a / 75.0);
			Vec3d from = anchor.subtract(fwd.multiply(9)).add(0, 3, 0);
			Vec3d to = grid.add(Math.cos(spin) * 22, 92, Math.sin(spin) * 22);
			aim(s, lerp(from, to, u), lerp(anchor.add(0, 1, 0), grid, u), (float) lerp(70, 64, u), false);
			return;
		}
		if (a >= PRE_REROLL) {
			// Lower, faster orbit for the reroll, then a long pull up and away.
			double k = a - PRE_REROLL;
			double ang = spin + k * k * 0.00012 + k * 0.004;
			double rad = a < ChunkRolls.REROLL + 60 ? lerp(22, 42, ease(k / 40.0)) : lerp(42, 30, ease((a - ChunkRolls.REROLL - 60) / 80.0));
			double h = a < ChunkRolls.REROLL + 60 ? lerp(92, 58, ease(k / 40.0)) : lerp(58, 125, ease((a - ChunkRolls.REROLL - 60) / 80.0));
			aim(s, grid.add(Math.cos(ang) * rad, h, Math.sin(ang) * rad), grid, (float) lerp(64, 82, ease(k / 40.0)), false);
			return;
		}
		Vec3d pos = grid.add(Math.cos(spin) * 22, 92, Math.sin(spin) * 22);
		Vec3d target = grid;
		double fov = 64;
		// Swoop down into a few of the rolls and back up again.
		for (int slot : SWOOPS) {
			float d = a - ChunkRolls.rollTick(slot);
			if (d < -10 || d > 34) {
				continue;
			}
			double k = d < 0 ? ease((d + 10) / 10.0) : d > 24 ? 1 - ease((d - 24) / 10.0) : 1;
			Vec3d cc = cellCenter(order[slot]);
			Vec3d out = new Vec3d(cc.x - grid.x, 0, cc.z - grid.z);
			out = out.lengthSquared() < 1 ? fwd.multiply(-1) : out.normalize();
			double orbit = d * 0.012;
			Vec3d side = new Vec3d(out.x * Math.cos(orbit) - out.z * Math.sin(orbit), 0, out.x * Math.sin(orbit) + out.z * Math.cos(orbit));
			Vec3d close = cc.add(side.multiply(24)).add(0, 13, 0);
			pos = lerp(pos, close, k);
			target = lerp(target, cc.add(0, 3, 0), k);
			fov = lerp(fov, 76, k);
		}
		aim(s, pos, target, (float) fov, false);
	}

	@Override
	protected float handheld(float a) {
		if (a >= ChunkRolls.REROLL && a < ChunkRolls.REROLL + 60) {
			return 1.0F;
		}
		return 0.2F;
	}

	@Override
	public boolean tick(ClientWorld world) {
		for (int slot = 0; slot < 16; slot++) {
			if (age == ChunkRolls.rollTick(slot) || age == ChunkRolls.rerollTick(slot)) {
				int round = age == ChunkRolls.rollTick(slot) ? 0 : 1;
				ClientFx.light(cellCenter(order[slot]).add(0, 6, 0), 0.7F, ChunkRolls.roll(seed, round, order[slot]).kind().color);
			}
		}
		if (age >= PRE_REROLL && age < ChunkRolls.REROLL) {
			ClientFx.hue = Math.max(ClientFx.hue, 0.25F * (age - PRE_REROLL) / 40.0F);
		}
		return super.tick(world);
	}

	// --- broadcast overlay: mini-map and slot machine ---------------------------------------

	private static final ChunkRolls.Kind[] KINDS = ChunkRolls.Kind.values();

	private static int argb(int alpha, int rgb) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	private static int scale(int rgb, float k) {
		int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * k), g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * k), b = (int) Math.min(255, (rgb & 0xFF) * k);
		return (r << 16) | (g << 8) | b;
	}

	/** A 1px frame plus a soft outer glow in {@code rgb}. */
	private static void frame(DrawContext ctx, int x1, int y1, int x2, int y2, int rgb, int alpha, int glow) {
		for (int g = glow; g >= 1; g--) {
			int a = alpha * (glow - g + 1) / (glow * 4);
			ctx.fill(x1 - g, y1 - g, x2 + g, y1 - g + 1, argb(a, rgb));
			ctx.fill(x1 - g, y2 + g - 1, x2 + g, y2 + g, argb(a, rgb));
			ctx.fill(x1 - g, y1 - g, x1 - g + 1, y2 + g, argb(a, rgb));
			ctx.fill(x2 + g - 1, y1 - g, x2 + g, y2 + g, argb(a, rgb));
		}
		ctx.fill(x1, y1, x2, y1 + 1, argb(alpha, rgb));
		ctx.fill(x1, y2 - 1, x2, y2, argb(alpha, rgb));
		ctx.fill(x1, y1, x1 + 1, y2, argb(alpha, rgb));
		ctx.fill(x2 - 1, y1, x2, y2, argb(alpha, rgb));
	}

	@Override
	public void drawHud(DrawContext ctx, int w, int h, float a) {
		if (a < 70 || a > lifetime - 12) {
			return;
		}
		float in = (float) Cinematics.clamp01((a - 70) / 10.0) * (float) Cinematics.clamp01((lifetime - 12 - a) / 10.0);
		TextRenderer f = MinecraftClient.getInstance().textRenderer;
		miniMap(ctx, f, h, a, in);
		int s = activeSlot(a);
		if (s >= 0) {
			int cell = order[s];
			int at = ChunkRolls.rollTick(s);
			ChunkRolls.Roll roll = ChunkRolls.roll(seed, 0, cell);
			machine(ctx, f, w, h, a, at, roll.kind(), roll.label(), "CHUNK #" + (cell + 1), "ROLL " + (s + 1) + " / 16", cell, in);
		} else if (a >= PRE_REROLL && a < ChunkRolls.rerollTick(15) + 20) {
			int at = ChunkRolls.REROLL;
			machine(ctx, f, w, h, a, at, ChunkRolls.Kind.RANDOMIZE, "x16", "ALL CHUNKS", "REROLL", 7, in);
		}
	}

	/** The 4x4 grid in the corner: dark slots that light up in each result's colour, the rolling one strobing. */
	private void miniMap(DrawContext ctx, TextRenderer f, int h, float a, float in) {
		int cs = Math.max(11, h / 26);
		int mx = 16, my = h / 2 - cs * 2;
		int alpha = (int) (230 * in);
		ctx.fillGradient(mx - 6, my - 20, mx + cs * 4 + 6, my + cs * 4 + 18, argb((int) (200 * in), 0x140A1E), argb((int) (200 * in), 0x05030A));
		frame(ctx, mx - 6, my - 20, mx + cs * 4 + 6, my + cs * 4 + 18, 0xFF60FF, (int) (160 * in), 3);
		ctx.drawTextWithShadow(f, Text.literal("16 CHUNKS"), mx, my - 14, argb(alpha, 0xFF80FF));
		int done = 0;
		int frameIdx = (int) (a * 1.5F);
		for (int cell = 0; cell < 16; cell++) {
			int slot = slotOf(cell);
			int x = mx + (cell % 4) * cs, y = my + (cell / 4) * cs;
			int col = 0x24202E;
			boolean landed = false, spinning = false;
			if (a >= ChunkRolls.rerollTick(slot)) {
				col = ChunkRolls.roll(seed, 1, cell).kind().color;
				landed = true;
			} else if (a >= ChunkRolls.rerollTick(slot) - 14 || (a >= ChunkRolls.rollTick(slot) - 14 && a < ChunkRolls.rollTick(slot))) {
				col = KINDS[Math.floorMod(frameIdx + cell, KINDS.length)].color;
				spinning = true;
			} else if (a >= ChunkRolls.rollTick(slot)) {
				col = ChunkRolls.roll(seed, 0, cell).kind().color;
				landed = true;
			}
			if (landed) {
				done++;
				float pulse = 0.75F + 0.25F * MathHelper.sin(a * 0.15F + cell);
				ctx.fillGradient(x + 1, y + 1, x + cs - 1, y + cs - 1, argb(alpha, scale(col, 1.15F * pulse)), argb(alpha, scale(col, 0.55F * pulse)));
				ctx.fill(x + 2, y + 2, x + cs - 2, y + 3, argb(alpha / 2, 0xFFFFFF));
			} else {
				ctx.fill(x + 1, y + 1, x + cs - 1, y + cs - 1, argb(spinning ? alpha : alpha * 3 / 4, spinning ? col : 0x24202E));
			}
		}
		// Progress bar.
		int bw = cs * 4;
		ctx.fill(mx, my + cs * 4 + 6, mx + bw, my + cs * 4 + 9, argb(alpha / 2, 0x301838));
		ctx.fill(mx, my + cs * 4 + 6, mx + bw * Math.min(16, done) / 16, my + cs * 4 + 9, argb(alpha, 0xFF60FF));
	}

	/**
	 * The slot machine: a chrome cabinet with a vertical reel that whips past and decelerates onto the
	 * result, a payline, then a flash in the result's colour and the full roll name.
	 */
	private void machine(DrawContext ctx, TextRenderer f, int w, int h, float a, int at, ChunkRolls.Kind result, String full,
			String head, String sub, int salt, float in) {
		int cx = w / 2, cy = (int) (h * 0.2F) + 6;
		int rowH = 22, boxW = 230, half = boxW / 2;
		int top = cy - rowH - 6, bottom = cy + rowH + 6;
		int alpha = (int) (235 * in);
		boolean landed = a >= at;
		float since = a - at;
		int glowCol = landed ? result.color : 0xFF60FF;
		// Cabinet.
		ctx.fillGradient(cx - half - 10, top - 22, cx + half + 10, bottom + 14, argb((int) (210 * in), 0x241432), argb((int) (210 * in), 0x08040E));
		frame(ctx, cx - half - 10, top - 22, cx + half + 10, bottom + 14, glowCol, alpha, landed ? 6 : 3);
		ctx.drawCenteredTextWithShadow(f, Text.literal(head + "   " + sub), cx, top - 16, argb(alpha, 0xE0D0F0));
		// Bulbs chasing round the top edge.
		for (int i = 0; i < 18; i++) {
			int bx = cx - half - 4 + i * (boxW + 8) / 17;
			boolean on = ((int) (a * 0.6F) + i) % 3 == 0 || (landed && since < 20 && ((int) (a) & 1) == 0);
			ctx.fill(bx - 1, top - 5, bx + 2, top - 2, argb(alpha, on ? 0xFFE060 : 0x503820));
		}
		// Reel window.
		ctx.fillGradient(cx - half, top, cx + half, bottom, argb(alpha, 0xF4F0FF), argb(alpha, 0xC8C0D8));
		ctx.enableScissor(cx - half, top, cx + half, bottom);
		double u = Cinematics.clamp01((a - (at - 14)) / 14.0);
		double target = result.ordinal() + KINDS.length * 3;
		double pos = landed ? target : target - 26 * Math.pow(1 - u, 3);
		int base = (int) Math.floor(pos);
		for (int i = base - 2; i <= base + 2; i++) {
			double off = (i - pos) * rowH;
			int ty = (int) (cy + off) - 4;
			ChunkRolls.Kind k = KINDS[Math.floorMod(i + salt, KINDS.length)];
			if (i == (int) Math.round(target) && landed) {
				k = result;
			}
			float dim = (float) Math.max(0.25, 1 - Math.abs(off) / (rowH * 1.4));
			ctx.getMatrices().push();
			ctx.getMatrices().translate(cx, ty, 0);
			ctx.getMatrices().scale(1.6F, 1.6F, 1);
			ctx.drawCenteredTextWithShadow(f, Text.literal(k.label), 0, -2, argb((int) (255 * dim * in), scale(k.color, 0.7F)));
			ctx.getMatrices().pop();
		}
		ctx.disableScissor();
		// Motion blur shading at the edges of the window, and the payline.
		ctx.fillGradient(cx - half, top, cx + half, top + 12, argb((int) (170 * in), 0x18101E), argb(0, 0x18101E));
		ctx.fillGradient(cx - half, bottom - 12, cx + half, bottom, argb(0, 0x18101E), argb((int) (170 * in), 0x18101E));
		ctx.fill(cx - half - 6, cy - 1, cx - half + 4, cy + 1, argb(alpha, 0xFF3060));
		ctx.fill(cx + half - 4, cy - 1, cx + half + 6, cy + 1, argb(alpha, 0xFF3060));
		// The landing: a flash in the result colour, an expanding frame and the full name underneath.
		if (landed) {
			if (since < 8) {
				float k = 1 - since / 8F;
				ctx.fill(cx - half, top, cx + half, bottom, argb((int) (150 * k * in), result.color));
				int grow = (int) (since * 5);
				frame(ctx, cx - half - 10 - grow, top - 22 - grow, cx + half + 10 + grow, bottom + 14 + grow, result.color, (int) (200 * k * in), 2);
			}
			float pop = 1.0F + 0.5F * (float) Math.max(0, 1 - since / 5.0);
			ctx.getMatrices().push();
			ctx.getMatrices().translate(cx, bottom + 26, 0);
			ctx.getMatrices().scale(2.2F * pop, 2.2F * pop, 1);
			ctx.drawCenteredTextWithShadow(f, Text.literal(full), 0, 0, argb(alpha, result.color));
			ctx.getMatrices().pop();
			// Sparkles flying off the cabinet.
			if (since < 24) {
				for (int i = 0; i < 14; i++) {
					double ang = Solids.hash(i, salt + at) * Math.PI * 2;
					double d = 20 + since * (6 + 4 * Solids.hash(i, salt + 1));
					int sx = (int) (cx + Math.cos(ang) * (half + d)), sy = (int) (cy + Math.sin(ang) * (rowH + d * 0.6));
					int sa = (int) (255 * (1 - since / 24F) * in);
					ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, argb(sa, i % 2 == 0 ? 0xFFFFFF : result.color));
				}
			}
		}
	}

	private int slotOf(int cell) {
		for (int slot = 0; slot < 16; slot++) {
			if (order[slot] == cell) {
				return slot;
			}
		}
		return 0;
	}

	private static double ease(double t) {
		return Cinematics.ease(t);
	}

	private static double lerp(double a, double b, double t) {
		return Cinematics.lerp(a, b, t);
	}

	private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
		return Cinematics.lerp(a, b, t);
	}

	private static void aim(Cinematics.State s, Vec3d pos, Vec3d target, float fov, boolean space) {
		Cinematics.aim(s, pos, target, fov, space);
	}

	/** No scenery of its own: it films the real chunks, and ChunkGlitchFx draws the grid. */
	@Override
	public void render(org.joml.Matrix4f m, net.minecraft.client.render.VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
	}
}
