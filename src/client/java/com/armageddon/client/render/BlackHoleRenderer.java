package com.armageddon.client.render;

import com.armageddon.entity.BlackHoleEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Event horizon, photon rings, a tilted accretion disc that is Doppler-brightened on the side
 * spinning towards the camera, the lensed far side of the disc arching over the shadow, polar
 * jets and infalling streamers. Gravitational lensing of the world is added by the post shader.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
	private static final int[] DISC = {0xFFFFFF, 0xFFF0C8, 0xFFC870, 0xFF8A30, 0xF0502A, 0xC0306A, 0x7A30C8, 0x3A1A90};

	public BlackHoleRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(BlackHoleEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(BlackHoleEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		float r = entity.getCurrentRadius(tickDelta);
		if (r <= 0.01F) {
			return;
		}
		float time = entity.age + tickDelta;
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d[] cb = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		Vec3d view = eye.lengthSquared() > 1.0E-6 ? eye.normalize() : new Vec3d(0, 0, 1);
		double core = r * 0.5;
		// Fewer segments when it is a dot on the horizon.
		int seg = eye.length() > core * 160 ? 40 : 96;

		matrices.push();
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);

		// Halo: deep violet glow fading into a faint blue rim.
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 4.6, 0x1A0840, 0.35F, 0.0F, 48);
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 2.6, 0x5A20B0, 0.32F, 0.0F, 48);
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 2.2, r * 3.4, 0x4060FF, 0.0F, 0.12F, 0.0F, 64);

		// Disc frame: a fixed tilt that slowly precesses.
		double tilt = Math.toRadians(17), prec = time * 0.0015;
		Vec3d n = new Vec3d(Math.sin(tilt) * Math.cos(prec), Math.cos(tilt), Math.sin(tilt) * Math.sin(prec)).normalize();
		Vec3d u = n.crossProduct(new Vec3d(0, 0, 1));
		u = (u.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : u).normalize();
		Vec3d v = n.crossProduct(u).normalize();

		accretion(glow, m, u, v, view, core * 1.22, core * 5.4, seg, time);
		// Hot inner edge (the last stable orbit).
		FxDraw.ring(glow, m, Vec3d.ZERO, u, v, core * 1.18, core * 1.42, 0xFFFFFF, 0.0F, 0.95F, 0.0F, seg);

		// The far side of the disc, bent up over (and under) the shadow by gravity.
		lensedArc(glow, m, cb, core * 1.12, core * 1.95, seg / 2, 0xFFD8A0, 0.85F, true, time);
		lensedArc(glow, m, cb, core * 1.08, core * 1.45, seg / 2, 0xFF9A50, 0.4F, false, time);

		// Photon rings: razor thin, almost white.
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], core * 1.0, core * 1.09, 0xFFFFFF, 0.0F, 1.0F, 0.0F, seg);
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], core * 1.06, core * 1.32, 0xFFC890, 0.0F, 0.5F, 0.0F, seg);

		jets(glow, m, cb, eye, n, core, r, time);
		streamers(glow, m, u, v, core, time);

		// Event horizon.
		VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
		FxDraw.sphere(dark, m, Vec3d.ZERO, core, eye, 0x000000, 1.0F, 1.0F, 16, 24);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	/**
	 * Concentric bands from white-hot inside to violet outside. Each segment is brightened and
	 * whitened where the gas moves towards the viewer, and streaked by turbulence that orbits
	 * faster on the inside (Keplerian shear).
	 */
	private static void accretion(VertexConsumer vc, Matrix4f m, Vec3d u, Vec3d v, Vec3d view, double inner, double outer, int seg, float time) {
		int bands = DISC.length - 1;
		for (int k = 0; k < bands; k++) {
			double r0 = inner + (outer - inner) * Math.pow(k / (double) bands, 1.35);
			double r1 = inner + (outer - inner) * Math.pow((k + 1) / (double) bands, 1.35);
			float a0 = (float) Math.pow(1 - k / (double) bands, 1.2), a1 = (float) Math.pow(1 - (k + 1) / (double) bands, 1.2);
			double omega = 0.16 / Math.pow(1 + k, 1.5);
			for (int j = 0; j < seg; j++) {
				double t0 = j * MathHelper.TAU / seg, t1 = (j + 1) * MathHelper.TAU / seg;
				float b0 = doppler(u, v, view, t0), b1 = doppler(u, v, view, t1);
				float s0 = streak(t0, k, time, omega), s1 = streak(t1, k, time, omega);
				int c00 = shift(DISC[k], b0), c01 = shift(DISC[k], b1), c10 = shift(DISC[k + 1], b0), c11 = shift(DISC[k + 1], b1);
				Vec3d d0 = u.multiply(Math.cos(t0)).add(v.multiply(Math.sin(t0)));
				Vec3d d1 = u.multiply(Math.cos(t1)).add(v.multiply(Math.sin(t1)));
				Vec3d p00 = d0.multiply(r0), p01 = d1.multiply(r0), p11 = d1.multiply(r1), p10 = d0.multiply(r1);
				FxDraw.vertex(vc, m, p00.x, p00.y, p00.z, c00, 0.85F * a0 * b0 * s0);
				FxDraw.vertex(vc, m, p01.x, p01.y, p01.z, c01, 0.85F * a0 * b1 * s1);
				FxDraw.vertex(vc, m, p11.x, p11.y, p11.z, c11, 0.85F * a1 * b1 * s1);
				FxDraw.vertex(vc, m, p10.x, p10.y, p10.z, c10, 0.85F * a1 * b0 * s0);
			}
		}
	}

	/** Relativistic beaming: 0.3 on the receding side, up to 1.3 on the approaching side. */
	private static float doppler(Vec3d u, Vec3d v, Vec3d view, double t) {
		Vec3d tangent = u.multiply(-Math.sin(t)).add(v.multiply(Math.cos(t)));
		double toward = tangent.dotProduct(view);
		return (float) MathHelper.clamp(0.8 + 0.5 * toward, 0.3, 1.3);
	}

	private static float streak(double t, int band, float time, double omega) {
		double phase = t * (5 + band) - time * omega * (6 + band);
		return 0.72F + 0.28F * (float) Math.sin(phase + band * 1.7) * (float) Math.sin(phase * 0.37 + band);
	}

	/** Approaching gas shifts toward white, receding gas toward deep red. */
	private static int shift(int rgb, float doppler) {
		float k = MathHelper.clamp(doppler - 0.8F, -0.5F, 0.5F);
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		if (k > 0) {
			r += (int) ((255 - r) * k);
			g += (int) ((255 - g) * k);
			b += (int) ((255 - b) * k * 0.8F);
		} else {
			g = (int) (g * (1 + k));
			b = (int) (b * (1 + k * 1.4F));
		}
		return (MathHelper.clamp(r, 0, 255) << 16) | (MathHelper.clamp(g, 0, 255) << 8) | MathHelper.clamp(b, 0, 255);
	}

	/** Half ring in screen space: the image of the disc's far side wrapping over the top or bottom of the shadow. */
	private static void lensedArc(VertexConsumer vc, Matrix4f m, Vec3d[] cb, double inner, double outer, int seg, int rgb, float alpha, boolean top, float time) {
		double mid = (inner + outer) * 0.5;
		for (int j = 0; j < seg; j++) {
			double t0 = Math.PI * j / seg, t1 = Math.PI * (j + 1) / seg;
			// Brighter towards one end (the approaching side), fading at both tips.
			float e0 = (float) Math.sin(t0) * (0.55F + 0.45F * (float) Math.cos(t0)), e1 = (float) Math.sin(t1) * (0.55F + 0.45F * (float) Math.cos(t1));
			float f0 = alpha * Math.max(0.15F, e0) * (0.9F + 0.1F * MathHelper.sin(time * 0.3F + j)), f1 = alpha * Math.max(0.15F, e1) * (0.9F + 0.1F * MathHelper.sin(time * 0.3F + j + 1));
			double sgn = top ? 1 : -1;
			Vec3d d0 = cb[0].multiply(Math.cos(t0)).add(cb[1].multiply(Math.sin(t0) * sgn));
			Vec3d d1 = cb[0].multiply(Math.cos(t1)).add(cb[1].multiply(Math.sin(t1) * sgn));
			band(vc, m, d0, d1, inner, mid, rgb, 0.0F, f0, 0.0F, f1);
			band(vc, m, d0, d1, mid, outer, rgb, f0, 0.0F, f1, 0.0F);
		}
	}

	private static void band(VertexConsumer vc, Matrix4f m, Vec3d d0, Vec3d d1, double r0, double r1, int rgb, float a00, float a10, float a01, float a11) {
		FxDraw.vertex(vc, m, d0.x * r0, d0.y * r0, d0.z * r0, rgb, a00);
		FxDraw.vertex(vc, m, d1.x * r0, d1.y * r0, d1.z * r0, rgb, a01);
		FxDraw.vertex(vc, m, d1.x * r1, d1.y * r1, d1.z * r1, rgb, a11);
		FxDraw.vertex(vc, m, d0.x * r1, d0.y * r1, d0.z * r1, rgb, a10);
	}

	/** Twin relativistic jets with bright knots racing outward. */
	private static void jets(VertexConsumer vc, Matrix4f m, Vec3d[] cb, Vec3d eye, Vec3d n, double core, double r, float time) {
		double len = r * 10;
		float flicker = 0.85F + 0.15F * MathHelper.sin(time * 0.9F);
		for (int s = -1; s <= 1; s += 2) {
			Vec3d dir = n.multiply(s);
			Vec3d base = dir.multiply(core * 0.9);
			FxDraw.ribbon(vc, m, base, dir.multiply(len), eye, core * 1.1, 0x6A7CFF, 0.35F * flicker, 0.0F);
			FxDraw.ribbon(vc, m, base, dir.multiply(len * 0.8), eye, core * 0.45, 0xB8D0FF, 0.7F * flicker, 0.0F);
			FxDraw.ribbon(vc, m, base, dir.multiply(len * 0.5), eye, core * 0.14, 0xFFFFFF, 0.9F, 0.0F);
			for (int i = 0; i < 5; i++) {
				double t = ((time * 0.025 + i / 5.0) % 1.0);
				Vec3d p = base.add(dir.multiply(t * len * 0.85));
				FxDraw.disc(vc, m, p, cb[0], cb[1], core * (0.5 - 0.35 * t), 0xD8E4FF, 0.8F * (float) (1 - t), 0.0F, 10);
			}
		}
	}

	/** Gas streams spiralling in from the outer disc. */
	private static void streamers(VertexConsumer vc, Matrix4f m, Vec3d u, Vec3d v, double core, float time) {
		for (int k = 0; k < 3; k++) {
			Vec3d prev = null;
			for (int s = 0; s <= 24; s++) {
				double t = s / 24.0;
				double rad = core * (6.5 - 5.2 * t);
				double ang = k * MathHelper.TAU / 3 + t * 4.2 + time * 0.03;
				Vec3d p = u.multiply(Math.cos(ang) * rad).add(v.multiply(Math.sin(ang) * rad)).add(u.crossProduct(v).multiply(Math.sin(t * 9 + k) * core * 0.15));
				if (prev != null) {
					float al = (float) (Math.sin(t * Math.PI) * 0.45);
					FxDraw.ribbon(vc, m, prev, p, Vec3d.ZERO.add(u.crossProduct(v).multiply(core * 40)), core * 0.18 * (1.2 - t), t > 0.7 ? 0xFFE0B0 : 0xFF7A40, al, al);
				}
				prev = p;
			}
		}
	}

	@Override
	public Identifier getTexture(BlackHoleEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
