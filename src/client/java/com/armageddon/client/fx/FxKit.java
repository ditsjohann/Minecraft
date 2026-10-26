package com.armageddon.client.fx;

import com.armageddon.client.render.FxBuffers;
import com.armageddon.client.render.FxSprites;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Textured building blocks for the cataclysm effects: billowing dust rings, smoke columns, fire bursts, textured
 * shock rings, spark streaks and glows. All positions are camera-relative; {@code t} is a 0..1 phase.
 */
final class FxKit {
	private FxKit() {
	}

	static VertexConsumer glow(VertexConsumerProvider c) {
		return c.getBuffer(FxSprites.GLOW_LAYER);
	}

	static VertexConsumer smoke(VertexConsumerProvider c) {
		return c.getBuffer(FxSprites.SMOKE_LAYER);
	}

	static double h(int a, int b) {
		return FxSprites.hash(a, b);
	}

	/** A ring of ground-hugging dust billows rolling outward: {@code radius} now, {@code thick} deep. */
	static void dustRing(Matrix4f m, Vec3d[] cb, Vec3d centre, double radius, double thick, int lit, int shadow, float alpha, int salt, List<Effects.Puff> out) {
		int n = MathHelper.clamp((int) (radius / Math.max(2.0, thick * 0.6)), 10, 64);
		for (int i = 0; i < n; i++) {
			double a = i * MathHelper.TAU / n + h(i, salt) * 0.3;
			double rr = radius + (h(i, salt + 1) - 0.5) * thick * 0.8;
			double pr = thick * (0.55 + 0.45 * h(i, salt + 2));
			out.add(new Effects.Puff(centre.add(Math.cos(a) * rr, pr * 0.55, Math.sin(a) * rr), pr, lit, shadow, alpha * (0.7F + 0.3F * (float) h(i, salt + 3))));
		}
	}

	/** A rising column of smoke from {@code base} to {@code height} above it, {@code width} across, drifting. */
	static void column(Vec3d base, double height, double width, float time, int lit, int shadow, float alpha, int salt, List<Effects.Puff> out) {
		int n = MathHelper.clamp((int) (height / Math.max(1.5, width * 0.45)), 4, 40);
		for (int i = 0; i < n; i++) {
			double f = (i + 0.5) / n;
			double sway = Math.sin(time * 0.03 + f * 4 + salt) * width * 0.4 * f;
			double ang = h(i, salt) * MathHelper.TAU;
			Vec3d p = base.add(Math.cos(ang) * width * 0.25 + sway, height * f, Math.sin(ang) * width * 0.25);
			double pr = width * (0.45 + 0.4 * f) * (0.8 + 0.4 * h(i, salt + 1));
			out.add(new Effects.Puff(p, pr, lit, Effects.fade(shadow, lit, (float) f * 0.4F), alpha * (1 - (float) f * 0.35F)));
		}
	}

	/** Draws queued puffs back to front. */
	static void flush(List<Effects.Puff> puffs, VertexConsumerProvider c, Matrix4f m, Vec3d[] cb) {
		if (!puffs.isEmpty()) {
			Effects.drawPuffs(puffs, c.getBuffer(com.armageddon.client.render.FxRenderLayers.SHADE), m, cb, 10);
		}
	}

	static List<Effects.Puff> puffs() {
		return new ArrayList<>(64);
	}

	/** A burst of fire tongues around {@code c}, {@code r} across, cooling from {@code hot} towards ember red with {@code t}. */
	static void fire(VertexConsumerProvider c, Matrix4f m, Vec3d[] cb, Vec3d centre, double r, int hot, float t, float alpha, float time, int salt) {
		VertexConsumer g = glow(c);
		int full = FxSprites.FULL_BRIGHT;
		FxSprites.billboard(g, m, centre, cb, r * 2.2, 0, FxSprites.GLOW, Effects.fade(hot, 0xFF5010, t), alpha * 0.4F * (1 - t), full);
		int n = r > 4 ? 14 : 7;
		for (int k = 0; k < n; k++) {
			Vec3d d = Effects.dir(k, salt);
			Vec3d p = centre.add(d.multiply(r * (0.2 + 0.5 * h(k, salt)))).add(0, r * t * 0.4, 0);
			int col = Effects.fade(Effects.fade(0xFFFFF0, hot, 0.4F + t), Effects.heat(0.3F + t * 0.6F), t * 0.6F);
			FxSprites.billboard(g, m, p, cb, r * (0.5 + 0.35 * h(k, salt + 1)), h(k, salt + 2) * 6 + time * (k % 2 == 0 ? 0.03 : -0.025),
					FxSprites.FIRE + (k & 1), col, alpha * (1 - t * 0.8F), full);
		}
	}

	/** A textured shock ring lying flat on the ground (or any plane given by {@code u}/{@code v}). */
	static void shock(VertexConsumerProvider c, Matrix4f m, Vec3d centre, Vec3d u, Vec3d v, double radius, int rgb, float alpha) {
		FxSprites.sprite(glow(c), m, centre, u, v, radius / 0.8, 0, FxSprites.SHOCK, rgb, rgb, alpha, FxSprites.FULL_BRIGHT);
	}

	static void groundShock(VertexConsumerProvider c, Matrix4f m, Vec3d centre, double radius, int rgb, float alpha) {
		shock(c, m, centre.add(0, 0.25, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), radius, rgb, alpha);
	}

	/** A soft round glow (camera-facing). */
	static void halo(VertexConsumerProvider c, Matrix4f m, Vec3d[] cb, Vec3d p, double r, int rgb, float alpha) {
		FxSprites.billboard(glow(c), m, p, cb, r, 0, FxSprites.GLOW, rgb, alpha, FxSprites.FULL_BRIGHT);
	}

	/** A four-point star flare. */
	static void flare(VertexConsumerProvider c, Matrix4f m, Vec3d[] cb, Vec3d p, double r, int rgb, float alpha, double rot) {
		FxSprites.billboard(glow(c), m, p, cb, r, rot, FxSprites.FLARE, rgb, alpha, FxSprites.FULL_BRIGHT);
	}

	/** {@code n} glowing sparks thrown outward from {@code centre} on ballistic arcs, phase {@code t} 0..1. */
	static void sparks(VertexConsumerProvider c, Matrix4f m, Vec3d centre, double reach, int n, int rgb, float t, float alpha, int salt) {
		if (t <= 0 || t >= 1) {
			return;
		}
		VertexConsumer g = glow(c);
		for (int i = 0; i < n; i++) {
			Vec3d d = Effects.dir(i, salt).add(0, 0.5, 0).normalize();
			double sp = reach * (0.5 + 0.5 * h(i, salt + 5));
			Vec3d p = centre.add(d.multiply(sp * t)).add(0, -reach * 0.6 * t * t, 0);
			Vec3d q = centre.add(d.multiply(sp * Math.max(0, t - 0.06))).add(0, -reach * 0.6 * Math.max(0, t - 0.06) * Math.max(0, t - 0.06), 0);
			FxSprites.streak(g, m, q, p, reach * 0.025 + 0.12, FxSprites.STREAK, rgb, alpha * (1 - t), FxSprites.FULL_BRIGHT);
		}
	}

	/** Sprites need our buffers; outside them (entity renderers) these helpers do nothing useful. */
	static boolean ready() {
		return FxBuffers.current() != null;
	}
}
