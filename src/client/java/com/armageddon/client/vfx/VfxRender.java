package com.armageddon.client.vfx;

import com.armageddon.client.render.FxSprites;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.Arrays;

/**
 * Turns an emitter's particles into textured quads from the FX atlas ({@link FxSprites}): camera-facing, upright,
 * stretched along motion or flat on the ground. Smoke is sorted back to front; glow is additive and is not.
 */
final class VfxRender {
	/** Smoke emitters at or below this many particles are depth-sorted; bigger ones draw in pool order. */
	private static final int SORT_LIMIT = 2048;
	private static long[] order = new long[256];

	/** Camera basis for this frame: right, up, and the upright-right (right with no vertical part). */
	static float rx, ry, rz, ux, uy, uz, hx, hz;

	private VfxRender() {
	}

	static void basis(float yaw, float pitch) {
		double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
		// Forward from Minecraft yaw/pitch (degrees), as Vec3d.fromPolar does.
		float fx = (float) (-Math.sin(y) * Math.cos(p)), fy = (float) -Math.sin(p), fz = (float) (Math.cos(y) * Math.cos(p));
		float ax = -fz, az = fx;
		float al = (float) Math.sqrt(ax * ax + az * az);
		if (al < 1.0E-5F) {
			ax = 1;
			az = 0;
			al = 1;
		}
		rx = ax / al;
		ry = 0;
		rz = az / al;
		// up = right × forward
		ux = ry * fz - rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy - ry * fx;
		float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
		ux /= ul;
		uy /= ul;
		uz /= ul;
		hx = rx;
		hz = rz;
	}

	static void draw(EmitterInstance em, VertexConsumer vc, Matrix4f m, double camX, double camY, double camZ, float td) {
		ParticlePool p = em.pool;
		int n = p.count;
		if (n == 0) {
			return;
		}
		EmitterDef d = em.def;
		EffectInstance fx = em.fx;
		float bx = (float) (fx.ox - camX), by = (float) (fx.oy - camY), bz = (float) (fx.oz - camZ);
		boolean smoke = d.layer == EmitterDef.Layer.SMOKE;
		boolean sorted = smoke && n <= SORT_LIMIT;
		if (sorted) {
			if (order.length < n) {
				order = new long[Integer.highestOneBit(n) * 2];
			}
			for (int i = 0; i < n; i++) {
				float cx = bx + p.x[i], cy = by + p.y[i], cz = bz + p.z[i];
				float d2 = cx * cx + cy * cy + cz * cz;
				order[i] = ((long) Float.floatToIntBits(d2) << 32) | i;
			}
			Arrays.sort(order, 0, n);
		}
		int tr = 255, tg = 255, tb = 255;
		if (d.tint && fx.tint != 0) {
			tr = (fx.tint >> 16) & 0xFF;
			tg = (fx.tint >> 8) & 0xFF;
			tb = fx.tint & 0xFF;
		}
		for (int k = 0; k < n; k++) {
			// Sorted ascending by distance: walk it backwards so the far particles draw first.
			int i = sorted ? (int) order[n - 1 - k] : k;
			float t = MathHelper.clamp((p.age[i] - 1 + td) / p.life[i], 0.0F, 1.0F);
			float cx = bx + MathHelper.lerp(td, p.px[i], p.x[i]);
			float cy = by + MathHelper.lerp(td, p.py[i], p.y[i]);
			float cz = bz + MathHelper.lerp(td, p.pz[i], p.z[i]);
			float half = p.size[i] * d.sizeCurve.at(t);
			if (half <= 0.001F) {
				continue;
			}
			int argb = d.color.at(t);
			float a = d.alpha * d.alphaCurve.at(t) * ((argb >>> 24) / 255.0F);
			float dist = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
			if (smoke) {
				// Smoke right at the eye would fill the screen with one flat card: thin it out instead.
				if (dist < half * 2.0F) {
					a *= MathHelper.clamp((dist - half * 0.3F) / (half * 1.7F), 0.0F, 1.0F);
				}
			} else {
				// The glare limiter: additive light that covers more of the view gets fainter.
				float cover = half / Math.max(1.0F, dist);
				a *= MathHelper.clamp(1.25F - 0.75F * cover, 0.16F, 1.0F);
			}
			if (a <= 0.004F) {
				continue;
			}
			int al = Math.min(255, (int) (a * 255));
			float kTop = 1.0F;
			if (smoke) {
				int packed = p.light[i];
				float e = d.emissive.at(t);
				if (e > 0.001F) {
					int block = Math.max(LightmapTextureManager.getBlockLightCoordinates(packed), (int) (e * 15));
					packed = LightmapTextureManager.pack(block, LightmapTextureManager.getSkyLightCoordinates(packed));
				}
				kTop = FxSprites.brightness(packed);
			}
			float kBot = kTop * d.shade;
			int r = ((argb >> 16) & 0xFF) * tr / 255, g = ((argb >> 8) & 0xFF) * tg / 255, b = (argb & 0xFF) * tb / 255;
			int rt = (int) (r * kTop), gt = (int) (g * kTop), bt = (int) (b * kTop);
			int rb = (int) (r * kBot), gb = (int) (g * kBot), bb = (int) (b * kBot);
			int cell = d.cell + (d.cells > 1 ? Math.min(d.cells - 1, (int) (p.seed[i] * d.cells)) : 0);
			float u0 = (cell % 4) * 0.25F + 0.002F, v0 = (cell / 4) * 0.25F + 0.002F;
			float u1 = u0 + 0.246F, v1 = v0 + 0.246F;
			float rot = MathHelper.lerp(td, p.prot[i], p.rot[i]);
			switch (d.mode) {
				case BILLBOARD -> quad(vc, m, cx, cy, cz, rx, ry, rz, ux, uy, uz, half, rot, u0, v0, u1, v1, rt, gt, bt, rb, gb, bb, al);
				case UPRIGHT -> quad(vc, m, cx, cy, cz, hx, 0, hz, 0, 1, 0, half, 0, u0, v0, u1, v1, rt, gt, bt, rb, gb, bb, al);
				case FLAT -> quad(vc, m, cx, cy, cz, 1, 0, 0, 0, 0, 1, half, rot, u0, v0, u1, v1, rt, gt, bt, rb, gb, bb, al);
				case STRETCHED -> {
					float vx = p.vx[i], vy = p.vy[i], vz = p.vz[i];
					float sp = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
					if (sp < 1.0E-3F) {
						quad(vc, m, cx, cy, cz, rx, ry, rz, ux, uy, uz, half, rot, u0, v0, u1, v1, rt, gt, bt, rb, gb, bb, al);
					} else {
						streak(vc, m, cx, cy, cz, vx / sp, vy / sp, vz / sp, half, half + sp * d.stretch * 0.5F, u0, v0, u1, v1, rt, gt, bt, al);
					}
				}
			}
		}
	}

	/** A quad spanning {@code half} along the (rotated) axes; the top edge takes the top colour, the bottom the other. */
	private static void quad(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float ax, float ay, float az,
			float bx, float by, float bz, float half, float rot, float u0, float v0, float u1, float v1,
			int rt, int gt, int bt, int rb, int gb, int bb, int a) {
		float cs = MathHelper.cos(rot) * half, sn = MathHelper.sin(rot) * half;
		float rX = ax * cs + bx * sn, rY = ay * cs + by * sn, rZ = az * cs + bz * sn;
		float uX = bx * cs - ax * sn, uY = by * cs - ay * sn, uZ = bz * cs - az * sn;
		vc.vertex(m, cx - rX - uX, cy - rY - uY, cz - rZ - uZ).texture(u0, v1).color(rb, gb, bb, a);
		vc.vertex(m, cx + rX - uX, cy + rY - uY, cz + rZ - uZ).texture(u1, v1).color(rb, gb, bb, a);
		vc.vertex(m, cx + rX + uX, cy + rY + uY, cz + rZ + uZ).texture(u1, v0).color(rt, gt, bt, a);
		vc.vertex(m, cx - rX + uX, cy - rY + uY, cz - rZ + uZ).texture(u0, v0).color(rt, gt, bt, a);
	}

	/** A camera-facing streak of half-length {@code len} along unit {@code d}, {@code half} wide. */
	private static void streak(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float dx, float dy, float dz,
			float half, float len, float u0, float v0, float u1, float v1, int r, int g, int b, int a) {
		// Side = d × (eye → particle), so the streak shows its full width to the camera.
		float sx = dy * cz - dz * cy, sy = dz * cx - dx * cz, sz = dx * cy - dy * cx;
		float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1.0E-5F) {
			return;
		}
		sx = sx / sl * half;
		sy = sy / sl * half;
		sz = sz / sl * half;
		float ax = cx - dx * len, ay = cy - dy * len, az = cz - dz * len;
		float ex = cx + dx * len, ey = cy + dy * len, ez = cz + dz * len;
		vc.vertex(m, ax - sx, ay - sy, az - sz).texture(u0, v1).color(r, g, b, a);
		vc.vertex(m, ex - sx, ey - sy, ez - sz).texture(u1, v1).color(r, g, b, a);
		vc.vertex(m, ex + sx, ey + sy, ez + sz).texture(u1, v0).color(r, g, b, a);
		vc.vertex(m, ax + sx, ay + sy, az + sz).texture(u0, v0).color(r, g, b, a);
	}
}
