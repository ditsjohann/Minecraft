package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.ChunkRolls;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * 16 CHUNKS overlay: glowing chunk borders and corner pillars over the 4x4 grid, a scan plane
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

	/** Glitch intensity 0..1 of a cell at time {@code a}, and which round's result it is showing (-1: none yet). */
	private float glitch(int cell, float a) {
		float g = 0;
		for (int slot = 0; slot < 16; slot++) {
			if (order[slot] != cell) {
				continue;
			}
			float d0 = a - ChunkRolls.rollTick(slot), d1 = a - ChunkRolls.rerollTick(slot);
			g = Math.max(g, window(d0));
			g = Math.max(g, window(d1));
		}
		return g;
	}

	private static float window(float d) {
		if (d < -14 || d > 12) {
			return 0;
		}
		return d < 0 ? (d + 14) / 14.0F : 1 - d / 12.0F;
	}

	private int round(int cell, float a) {
		for (int slot = 0; slot < 16; slot++) {
			if (order[slot] == cell) {
				return a >= ChunkRolls.rerollTick(slot) ? 1 : a >= ChunkRolls.rollTick(slot) ? 0 : -1;
			}
		}
		return -1;
	}

	@Override
	public boolean tick(ClientWorld world) {
		for (int cell = 0; cell < 16; cell++) {
			float g = glitch(cell, age);
			if (g > 0.3F) {
				for (int i = 0; i < 4; i++) {
					double x = x0 + (cell % 4) * 16 + world.random.nextDouble() * 16, z = z0 + (cell / 4) * 16 + world.random.nextDouble() * 16;
					world.addParticle(i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD, x, groundY + world.random.nextDouble() * 20, z, 0, 0.05, 0);
				}
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
		for (int i = 0; i <= 4; i++) {
			double lx = x0 + i * 16, lz = z0 + i * 16;
			Vec3d ax = new Vec3d(lx, y, z0).subtract(cam), bx = new Vec3d(lx, y, z0 + 64).subtract(cam);
			Vec3d az = new Vec3d(x0, y, lz).subtract(cam), bz = new Vec3d(x0 + 64, y, lz).subtract(cam);
			FxDraw.ribbon(glow, m, ax, bx, Vec3d.ZERO, 0.5, color, pulse * env, pulse * env);
			FxDraw.ribbon(glow, m, az, bz, Vec3d.ZERO, 0.5, color, pulse * env, pulse * env);
			FxDraw.ribbon(glow, m, ax, bx, Vec3d.ZERO, 0.15, 0xFFFFFF, env, env);
			FxDraw.ribbon(glow, m, az, bz, Vec3d.ZERO, 0.15, 0xFFFFFF, env, env);
			for (int j = 0; j <= 4; j++) {
				Vec3d c = new Vec3d(x0 + i * 16, groundY - 2, z0 + j * 16).subtract(cam);
				FxDraw.beam(glow, m, c, c.add(0, 48, 0), Vec3d.ZERO, 0.35, color, 0.45F * env);
			}
		}
		// A scan plane sweeping up through the grid.
		double sy = groundY - 8 + (a * 0.9) % 56;
		rect(glow, m, cam, x0, z0, 64, sy, 0.7, 0x80FFFF, 0.7F * env);
		// Rolling chunks glitch; settled ones keep a faint tint of their result.
		for (int cell = 0; cell < 16; cell++) {
			double cx = x0 + (cell % 4) * 16, cz = z0 + (cell / 4) * 16;
			int r = round(cell, a);
			int tint = r >= 0 ? ChunkRolls.roll(seed, r, cell).kind().color : color;
			if (r >= 0) {
				Vec3d p0 = new Vec3d(cx, y + 0.05, cz).subtract(cam);
				FxDraw.vertex(glow, m, p0.x, p0.y, p0.z, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x, p0.y, p0.z + 16, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x + 16, p0.y, p0.z + 16, tint, 0.12F * env);
				FxDraw.vertex(glow, m, p0.x + 16, p0.y, p0.z, tint, 0.12F * env);
			}
			float g = glitch(cell, a);
			if (g <= 0.01F) {
				continue;
			}
			// Before the result lands the colours cycle like a slot machine.
			int frame = (int) (a * 1.5F);
			int slot = slotOf(cell);
			boolean spinning = (a >= ChunkRolls.rollTick(slot) - 14 && a < ChunkRolls.rollTick(slot))
					|| (a >= ChunkRolls.rerollTick(slot) - 14 && a < ChunkRolls.rerollTick(slot));
			int col = spinning ? ChunkRolls.Kind.values()[Math.floorMod(frame + cell, ChunkRolls.Kind.values().length)].color : tint;
			for (int k = 0; k < 4; k++) {
				rect(glow, m, cam, cx, cz, 16, groundY - 4 + k * 10, 0.5, col, g * env);
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

	private int slotOf(int cell) {
		for (int slot = 0; slot < 16; slot++) {
			if (order[slot] == cell) {
				return slot;
			}
		}
		return 0;
	}

	/** Square outline of side {@code size} at height {@code y}. */
	private static void rect(VertexConsumer glow, Matrix4f m, Vec3d cam, double x, double z, double size, double y, double width, int rgb, float alpha) {
		Vec3d a = new Vec3d(x, y, z).subtract(cam), b = new Vec3d(x + size, y, z).subtract(cam);
		Vec3d c = new Vec3d(x + size, y, z + size).subtract(cam), d = new Vec3d(x, y, z + size).subtract(cam);
		FxDraw.ribbon(glow, m, a, b, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, b, c, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, c, d, Vec3d.ZERO, width, rgb, alpha, alpha);
		FxDraw.ribbon(glow, m, d, a, Vec3d.ZERO, width, rgb, alpha, alpha);
	}
}
