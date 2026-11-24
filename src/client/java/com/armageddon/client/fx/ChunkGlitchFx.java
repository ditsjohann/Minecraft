package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.ChunkRolls;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * 32 CHUNKS overlay: glowing chunk borders and corner pillars over the 8x4 grid, a scan plane
 * sweeping through it, glitch noise on whichever chunk is rolling, and a faint tint of each result.
 * Timing comes from {@link ChunkRolls}, so it lines up with the server without further packets.
 */
public class ChunkGlitchFx extends FxEffect {
	private final long seed;
	private final int[] order;
	private final double x0, z0, groundY;
	private final int color;

	public ChunkGlitchFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		Vec3d anchor = new Vec3d(p.x(), p.y(), p.z());
		Vec3d heading = new Vec3d(p.x2(), p.y2(), p.z2());
		this.seed = ChunkRolls.seed(anchor, heading);
		this.order = ChunkRolls.order(seed);
		this.x0 = ChunkRolls.originChunkX(anchor) * 16;
		this.z0 = ChunkRolls.originChunkZ(anchor) * 16;
		this.groundY = p.p1();
		this.color = p.color();
	}

	/** Glitch intensity 0..1 of a cell at time {@code a}. */
	private float glitch(int cell, float a) {
		return window(a - ChunkRolls.rollTick(slotOf(cell)));
	}

	private static float window(float d) {
		if (d < -14 || d > 12) {
			return 0;
		}
		return d < 0 ? (d + 14) / 14.0F : 1 - d / 12.0F;
	}

	/** Whether the cell's result has landed yet. */
	private boolean rolled(int cell, float a) {
		return a >= ChunkRolls.rollTick(slotOf(cell));
	}

	@Override
	public boolean tick(ClientWorld world) {
		for (int cell = 0; cell < ChunkRolls.CELLS; cell++) {
			float g = glitch(cell, age);
			if (g > 0.3F && age % 2 == 0) {
				// Static crawling over the rolling chunk, and data pouring down its corner pillars.
				double cx = x0 + (cell % ChunkRolls.W) * 16 + 8, cz = z0 + (cell / ChunkRolls.W) * 16 + 8;
				com.armageddon.client.vfx.FxEngine.play(com.armageddon.vfx.VfxIds.CHUNK_STATIC, cx, groundY, cz, 0, 1, 0, 1.0F, color, age * 131L + cell);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		float env = Math.min(1.0F, a / 20.0F) * (float) MathHelper.clamp((lifetime - a) / 40.0, 0.0, 1.0);
		if (env <= 0.01F) {
			return;
		}
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		double y = groundY + 1.1;
		float pulse = 0.55F + 0.25F * MathHelper.sin(a * 0.2F);
		// Chunk borders and corner pillars.
		int sx = ChunkRolls.W * 16, sz = ChunkRolls.H * 16;
		for (int i = 0; i <= ChunkRolls.W; i++) {
			double lx = x0 + i * 16;
			Vec3d ax = new Vec3d(lx, y, z0).subtract(cam), bx = new Vec3d(lx, y, z0 + sz).subtract(cam);
			FxDraw.ribbon(glow, m, ax, bx, Vec3d.ZERO, 0.5, color, pulse * env, pulse * env);
			FxDraw.ribbon(glow, m, ax, bx, Vec3d.ZERO, 0.15, 0xFFFFFF, env, env);
			for (int j = 0; j <= ChunkRolls.H; j++) {
				Vec3d c = new Vec3d(x0 + i * 16, groundY - 2, z0 + j * 16).subtract(cam);
				FxDraw.beam(glow, m, c, c.add(0, 48, 0), Vec3d.ZERO, 0.35, color, 0.45F * env);
			}
		}
		for (int j = 0; j <= ChunkRolls.H; j++) {
			double lz = z0 + j * 16;
			Vec3d az = new Vec3d(x0, y, lz).subtract(cam), bz = new Vec3d(x0 + sx, y, lz).subtract(cam);
			FxDraw.ribbon(glow, m, az, bz, Vec3d.ZERO, 0.5, color, pulse * env, pulse * env);
			FxDraw.ribbon(glow, m, az, bz, Vec3d.ZERO, 0.15, 0xFFFFFF, env, env);
		}
		// A scan plane sweeping up through the grid: a faint sheet of light with a fine voxel lattice.
		double sy = groundY - 8 + (a * 0.9) % 56;
		rect(glow, m, cam, x0, z0, sx, sz, sy, 0.7, 0x80FFFF, 0.7F * env);
		Vec3d s0 = new Vec3d(x0, sy, z0).subtract(cam);
		Solids.quad(glow, m, s0, s0.add(0, 0, sz), s0.add(sx, 0, sz), s0.add(sx, 0, 0), 0x60E0FF, 0.06F * env);
		for (int i = 1; i < sx / 4; i++) {
			if (i % 4 == 0) {
				continue;
			}
			float la = 0.16F * env * (0.6F + 0.4F * MathHelper.sin(a * 0.3F + i));
			FxDraw.ribbon(glow, m, s0.add(i * 4, 0, 0), s0.add(i * 4, 0, sz), Vec3d.ZERO, 0.08, 0xA0FFFF, la, la);
		}
		for (int i = 1; i < sz / 4; i++) {
			if (i % 4 == 0) {
				continue;
			}
			float la = 0.16F * env * (0.6F + 0.4F * MathHelper.sin(a * 0.3F + i));
			FxDraw.ribbon(glow, m, s0.add(0, 0, i * 4), s0.add(sx, 0, i * 4), Vec3d.ZERO, 0.08, 0xA0FFFF, la, la);
		}
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		if (a < ChunkRolls.rollTick(ChunkRolls.CELLS - 1) + 60) {
			machine(glow, consumers.getBuffer(FxRenderLayers.SHADE), m, cb, cam, a, env);
		}
		// Rolling chunks glitch; settled ones keep a faint tint of their result.
		for (int cell = 0; cell < ChunkRolls.CELLS; cell++) {
			double cx = x0 + (cell % ChunkRolls.W) * 16, cz = z0 + (cell / ChunkRolls.W) * 16;
			boolean r = rolled(cell, a);
			int tint = r ? ChunkRolls.roll(seed, cell).kind().color : color;
			if (r) {
				Vec3d p0 = new Vec3d(cx, y + 0.05, cz).subtract(cam);
				FxDraw.vertex(glow, m, p0.x, p0.y, p0.z, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x, p0.y, p0.z + 16, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x + 16, p0.y, p0.z + 16, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x + 16, p0.y, p0.z, tint, 0.12F * env);
			}
			reveal(glow, m, cb, cam, cell, cx, cz, a, env);
			float g = glitch(cell, a);
			if (g <= 0.01F) {
				continue;
			}
			// Before the result lands the colours cycle like a slot machine.
			int frame = (int) (a * 1.5F);
			int slot = slotOf(cell);
			boolean spinning = a >= ChunkRolls.rollTick(slot) - 14 && a < ChunkRolls.rollTick(slot);
			int col = spinning ? ChunkRolls.Kind.values()[Math.floorMod(frame + cell, ChunkRolls.Kind.values().length)].color : tint;
			for (int k = 0; k < 4; k++) {
				rect(glow, m, cam, cx, cz, 16, 16, groundY - 4 + k * 10, 0.5, col, g * env);
			}
			for (int e = 0; e < 4; e++) {
				Vec3d c = new Vec3d(cx + (e & 1) * 16, groundY - 4, cz + (e >> 1) * 16).subtract(cam);
				FxDraw.beam(glow, m, c, c.add(0, 34, 0), Vec3d.ZERO, 0.6, col, g * env);
			}
			// Horizontal tear lines at random heights, re-rolled every couple of frames.
			for (int i = 0; i < 14; i++) {
				double h1 = Solids.hash(i, frame * 7 + cell);
				double h2 = Solids.hash(i + 31, frame * 3 + cell);
				double ly = groundY - 2 + h1 * 30;
				boolean alongX = (i & 1) == 0;
				double off = h2 * 16;
				Vec3d p = alongX ? new Vec3d(cx, ly, cz + off) : new Vec3d(cx + off, ly, cz);
				Vec3d q = alongX ? p.add(16, 0, 0) : p.add(0, 0, 16);
				int c2 = Solids.hash(i, frame) < 0.5 ? col : (Solids.hash(i, frame + 1) < 0.5 ? 0xFFFFFF : 0x40FFFF);
				FxDraw.ribbon(glow, m, p.subtract(cam), q.subtract(cam), Vec3d.ZERO, 0.4 + h2 * 1.2, c2, 0.8F * g * env, 0.8F * g * env);
			}
		}
	}

	/**
	 * The jackpot machine: a holographic slot machine hanging over the grid with three reels. Each roll spins them
	 * through the outcome colours and stops them one after another on the result; the marquee lights chase round the
	 * frame and flash when a result lands.
	 */
	private void machine(VertexConsumer glow, VertexConsumer shade, Matrix4f m, Vec3d[] cb, Vec3d cam, float a, float env) {
		int slot = -1;
		for (int s = 0; s < ChunkRolls.CELLS; s++) {
			if (a >= ChunkRolls.rollTick(s) - 14) {
				slot = s;
			}
		}
		Vec3d right = new Vec3d(cb[0].x, 0, cb[0].z);
		right = right.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : right.normalize();
		Vec3d up = new Vec3d(0, 1, 0);
		Vec3d c = new Vec3d(x0 + ChunkRolls.W * 8, groundY + 58, z0 + ChunkRolls.H * 8).subtract(cam);
		double w = 34, h = 14;
		Vec3d[] f = {c.add(right.multiply(-w)).add(up.multiply(-h)), c.add(right.multiply(w)).add(up.multiply(-h)),
				c.add(right.multiply(w)).add(up.multiply(h)), c.add(right.multiply(-w)).add(up.multiply(h))};
		float landed = slot < 0 ? 0 : (float) MathHelper.clamp(1 - (a - ChunkRolls.rollTick(slot)) / 20.0, 0.0, 1.0);
		landed = a >= (slot < 0 ? 0 : ChunkRolls.rollTick(slot)) ? landed : 0;
		for (int e = 0; e < 4; e++) {
			FxDraw.ribbon(glow, m, f[e], f[(e + 1) % 4], Vec3d.ZERO, 0.8, 0xFFD040, (0.7F + 0.3F * landed) * env, (0.7F + 0.3F * landed) * env);
		}
		Solids.quad(shade, m, f[0], f[1], f[2], f[3], 0x180828, 0.7F * env);
		// Marquee lights chasing round the frame.
		int bulbs = 56;
		for (int i = 0; i < bulbs; i++) {
			double u = i / (double) bulbs * 4;
			int e = (int) u;
			Vec3d p = f[e].lerp(f[(e + 1) % 4], u - e);
			boolean on = ((i + (int) (a * 0.5)) % 4 == 0) || landed > 0.3F && ((int) (a * 0.6)) % 2 == 0;
			Solids.dot(glow, m, p, cb, on ? 1.0 : 0.5, on ? 0xFFF4C0 : 0xFF9030, (on ? 0.95F : 0.4F) * env);
		}
		// Three reels: tiles scrolling past a window, stopping in turn on the result's colour.
		ChunkRolls.Kind[] kinds = ChunkRolls.Kind.values();
		for (int r = 0; r < 3; r++) {
			Vec3d wc = c.add(right.multiply((r - 1) * 21));
			int result = slot < 0 ? -1 : ChunkRolls.roll(seed, order[slot]).kind().ordinal();
			double stopAt = slot < 0 ? 0 : ChunkRolls.rollTick(slot) - 8 + r * 4;
			boolean spinning = slot >= 0 && a < stopAt;
			double scroll = spinning ? a * 0.9 : 0;
			for (int k = -1; k <= 1; k++) {
				double off = (scroll % 1.0) - k;
				if (Math.abs(off) > 1.0) {
					continue;
				}
				int idx = spinning ? Math.floorMod((int) Math.floor(scroll) + k + r * 2, kinds.length) : (k == 0 && result >= 0 ? result : -1);
				if (idx < 0) {
					continue;
				}
				double hh = 8.5, ww = 8.5;
				Vec3d tc = wc.add(up.multiply(-off * hh * 2));
				double clip = Math.max(0, 1 - Math.abs(off));
				int col = kinds[idx].color;
				Solids.quad(shade, m, tc.add(right.multiply(-ww)).add(up.multiply(-hh * clip)), tc.add(right.multiply(ww)).add(up.multiply(-hh * clip)),
						tc.add(right.multiply(ww)).add(up.multiply(hh * clip)), tc.add(right.multiply(-ww)).add(up.multiply(hh * clip)), col, 0.92F * env);
				// A diamond symbol on the tile.
				Solids.quad(shade, m, tc.add(up.multiply(-hh * 0.6 * clip)), tc.add(right.multiply(ww * 0.55)), tc.add(up.multiply(hh * 0.6 * clip)),
						tc.add(right.multiply(-ww * 0.55)), 0xFFFFFF, 0.8F * env);
			}
			Vec3d[] wf = {wc.add(right.multiply(-9)).add(up.multiply(-9)), wc.add(right.multiply(9)).add(up.multiply(-9)),
					wc.add(right.multiply(9)).add(up.multiply(9)), wc.add(right.multiply(-9)).add(up.multiply(9))};
			for (int e = 0; e < 4; e++) {
				FxDraw.ribbon(glow, m, wf[e], wf[(e + 1) % 4], Vec3d.ZERO, 0.35, 0xFFFFFF, 0.7F * env, 0.7F * env);
			}
		}
	}

	/**
	 * The moment a result lands: a curtain of light in its colour rises up the chunk's four sides and data
	 * pixels float up out of it and dissolve.
	 */
	private void reveal(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d cam, int cell, double cx, double cz, float a, float env) {
		int slot = slotOf(cell);
		float d = a - ChunkRolls.rollTick(slot);
		if (d < 0 || d > 34) {
			return;
		}
		int col = ChunkRolls.roll(seed, cell).kind().color;
		float k = (1 - d / 34F) * env;
		double h = 6 + d * 1.3;
		double gy = groundY - 1;
		Vec3d[] c = {new Vec3d(cx, gy, cz), new Vec3d(cx + 16, gy, cz), new Vec3d(cx + 16, gy, cz + 16), new Vec3d(cx, gy, cz + 16)};
		for (int e = 0; e < 4; e++) {
			Vec3d p = c[e].subtract(cam), q = c[(e + 1) % 4].subtract(cam);
			FxDraw.vertex(glow, m, p.x, p.y, p.z, col, 0.45F * k);
			FxDraw.vertex(glow, m, q.x, q.y, q.z, col, 0.45F * k);
			FxDraw.vertex(glow, m, q.x, q.y + h, q.z, col, 0.0F);
			FxDraw.vertex(glow, m, p.x, p.y + h, p.z, col, 0.0F);
		}
		for (int i = 0; i < 26; i++) {
			double px = cx + Solids.hash(i, cell * 7) * 16, pz = cz + Solids.hash(i + 50, cell * 7) * 16;
			double py = gy + d * (0.4 + 0.8 * Solids.hash(i + 90, cell));
			Vec3d p = new Vec3d(px, py, pz).subtract(cam);
			FxDraw.disc(glow, m, p, cb[0], cb[1], 0.35 + 0.25 * Solids.hash(i, 3), i % 3 == 0 ? 0xFFFFFF : col, 0.9F * k, 0.9F * k, 4);
		}
	}

	private int slotOf(int cell) {
		for (int slot = 0; slot < ChunkRolls.CELLS; slot++) {
			if (order[slot] == cell) {
				return slot;
			}
		}
		return 0;
	}

	/** Rectangle outline {@code sx} by {@code sz} at height {@code y}. */
	private static void rect(VertexConsumer glow, Matrix4f m, Vec3d cam, double x, double z, double sx, double sz, double y, double width, int rgb, float alpha) {
		Vec3d a = new Vec3d(x, y, z).subtract(cam), b = new Vec3d(x + sx, y, z).subtract(cam);
		Vec3d c = new Vec3d(x + sx, y, z + sz).subtract(cam), d = new Vec3d(x, y, z + sz).subtract(cam);
		FxDraw.ribbon(glow, m, a, b, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, b, c, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, c, d, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, d, a, Vec3d.ZERO, width, rgb, alpha, alpha);
	}
}
