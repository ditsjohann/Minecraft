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
	public Cinematics.Caption caption(float a) {
		Cinematics.Caption c;
		if ((c = Cinematics.cap(a, 8, 72, "16 CHUNKS", "every chunk rolls the dice", 0xFF60FF)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, PRE_REROLL + 4, ChunkRolls.REROLL - 2, "REROLL", "everything. at once.", 0xFF4060)) != null) {
			return c;
		}
		return Cinematics.cap(a, ChunkRolls.REROLL + 70, lifetime - 6, "GOOD LUCK.", "", 0xFFFFFF);
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

	// --- broadcast overlay: mini-map and slot reel ----------------------------------------

	@Override
	public void drawHud(DrawContext ctx, int w, int h, float a) {
		if (a < 70 || a > lifetime - 12) {
			return;
		}
		TextRenderer f = MinecraftClient.getInstance().textRenderer;
		int cs = Math.max(10, h / 28);
		int mx = 14, my = h / 2 - cs * 2;
		ctx.fill(mx - 4, my - 16, mx + cs * 4 + 4, my + cs * 4 + 4, 0x90000000);
		ctx.drawTextWithShadow(f, Text.literal("16 CHUNKS"), mx, my - 12, 0xFFFF60FF);
		int frame = (int) (a * 1.5F);
		for (int cell = 0; cell < 16; cell++) {
			int slot = slotOf(cell);
			int x = mx + (cell % 4) * cs, y = my + (cell / 4) * cs;
			int col = 0x303040;
			if (a >= ChunkRolls.rerollTick(slot)) {
				col = ChunkRolls.roll(seed, 1, cell).kind().color;
			} else if (a >= ChunkRolls.rerollTick(slot) - 14 || (a >= ChunkRolls.rollTick(slot) - 14 && a < ChunkRolls.rollTick(slot))) {
				col = ChunkRolls.Kind.values()[Math.floorMod(frame + cell, ChunkRolls.Kind.values().length)].color;
			} else if (a >= ChunkRolls.rollTick(slot)) {
				col = ChunkRolls.roll(seed, 0, cell).kind().color;
			}
			ctx.fill(x + 1, y + 1, x + cs - 1, y + cs - 1, 0xE0000000 | col);
		}
		int s = activeSlot(a);
		if (s >= 0) {
			int cell = order[s];
			int at = ChunkRolls.rollTick(s);
			int x = mx + (cell % 4) * cs, y = my + (cell / 4) * cs;
			int blink = ((int) (a * 0.5F) & 1) == 0 ? 0xFFFFFFFF : 0xFFFF60FF;
			ctx.fill(x, y, x + cs, y + 1, blink);
			ctx.fill(x, y + cs - 1, x + cs, y + cs, blink);
			ctx.fill(x, y, x + 1, y + cs, blink);
			ctx.fill(x + cs - 1, y, x + cs, y + cs, blink);
			ChunkRolls.Roll roll = ChunkRolls.roll(seed, 0, cell);
			String label;
			int color;
			float scale;
			if (a < at) {
				// The reel: names flicker past, slowing as it lands.
				int idx = (int) (a * (a > at - 5 ? 0.6F : 1.6F)) + cell;
				ChunkRolls.Kind k = ChunkRolls.Kind.values()[Math.floorMod(idx, ChunkRolls.Kind.values().length)];
				label = k.label;
				color = 0xFFFFFF;
				scale = 2.4F;
			} else {
				label = roll.label();
				color = roll.kind().color;
				scale = 3.0F + 0.8F * (float) Math.max(0, 1 - (a - at) / 4.0);
			}
			reel(ctx, f, w, h, "CHUNK #" + (cell + 1) + "  •  ROLL " + (s + 1) + " / 16", label, color, scale);
		} else if (a >= PRE_REROLL && a < ChunkRolls.rerollTick(15) + 20) {
			int idx = (int) (a * 1.6F);
			String label = a < ChunkRolls.REROLL ? ChunkRolls.Kind.values()[Math.floorMod(idx, ChunkRolls.Kind.values().length)].label : "x16";
			reel(ctx, f, w, h, "ALL CHUNKS", label, a < ChunkRolls.REROLL ? 0xFFFFFF : 0xFF4060, a < ChunkRolls.REROLL ? 2.4F : 4.0F);
		}
	}

	private void reel(DrawContext ctx, TextRenderer f, int w, int h, String head, String label, int color, float scale) {
		int y = (int) (h * 0.2F);
		int boxW = Math.max(f.getWidth(label) * (int) Math.ceil(scale) + 24, 160);
		ctx.fill(w / 2 - boxW / 2, y - 18, w / 2 + boxW / 2, y + (int) (scale * 10) + 6, 0xA0000000);
		ctx.drawCenteredTextWithShadow(f, Text.literal(head), w / 2, y - 14, 0xFFC8C8C8);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w / 2.0F, y, 0);
		ctx.getMatrices().scale(scale, scale, 1);
		ctx.drawCenteredTextWithShadow(f, Text.literal(label), 0, 0, 0xFF000000 | color);
		ctx.getMatrices().pop();
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
}
