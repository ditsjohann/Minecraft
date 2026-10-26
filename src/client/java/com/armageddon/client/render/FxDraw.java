package com.armageddon.client.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Immediate-mode geometry helpers for POSITION_COLOR layers. All coordinates are
 * camera-relative (the matrix is the caller's current pose).
 */
public final class FxDraw {
	public static void vertex(VertexConsumer vc, Matrix4f m, double x, double y, double z, int rgb, float a) {
		vc.vertex(m, (float) x, (float) y, (float) z)
				.color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, MathHelper.clamp((int) (a * 255), 0, 255));
	}

	/** Camera-facing ribbon between two points. {@code eye} is the camera position in the same space. */
	public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d eye, double width, int rgb, float alphaA, float alphaB) {
		Vec3d dir = b.subtract(a);
		Vec3d mid = a.add(b).multiply(0.5);
		Vec3d toEye = eye.subtract(mid);
		Vec3d side = dir.crossProduct(toEye);
		if (side.lengthSquared() < 1.0E-8) {
			side = dir.crossProduct(new Vec3d(0, 1, 0));
			if (side.lengthSquared() < 1.0E-8) {
				side = new Vec3d(1, 0, 0);
			}
		}
		side = side.normalize().multiply(width * 0.5);
		vertex(vc, m, a.x - side.x, a.y - side.y, a.z - side.z, rgb, alphaA);
		vertex(vc, m, a.x + side.x, a.y + side.y, a.z + side.z, rgb, alphaA);
		vertex(vc, m, b.x + side.x, b.y + side.y, b.z + side.z, rgb, alphaB);
		vertex(vc, m, b.x - side.x, b.y - side.y, b.z - side.z, rgb, alphaB);
	}

	/** Layered glowing beam: wide soft halo + bright white core. */
	public static void beam(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, Vec3d eye, double width, int rgb, float alpha) {
		ribbon(vc, m, a, b, eye, width * 3.0, rgb, alpha * 0.18F, alpha * 0.18F);
		ribbon(vc, m, a, b, eye, width * 1.6, rgb, alpha * 0.45F, alpha * 0.45F);
		ribbon(vc, m, a, b, eye, width * 0.55, 0xFFFFFF, alpha * 0.9F, alpha * 0.9F);
	}

	/** Radial gradient disc facing along {@code right x up}. */
	public static void disc(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d right, Vec3d up, double radius, int rgb,
			float alphaCenter, float alphaEdge, int segments) {
		// Soft radial discs become one smooth textured sprite: no polygon fan to facet under shader packs.
		if (alphaEdge <= 0.0F && FxBuffers.current() != null) {
			net.minecraft.client.render.RenderLayer layer = FxBuffers.layerOf(vc);
			net.minecraft.client.render.RenderLayer to = layer == FxRenderLayers.GLOW ? FxSprites.GLOW_LAYER
					: layer == FxRenderLayers.GLOW_NO_DEPTH ? FxSprites.GLOW_TOP_LAYER : layer == FxRenderLayers.SHADE ? FxSprites.SMOKE_LAYER : null;
			if (to != null) {
				int light = to == FxSprites.SMOKE_LAYER ? FxSprites.light(c, 0.0F) : FxSprites.FULL_BRIGHT;
				FxSprites.sprite(FxBuffers.current().getBuffer(to), m, c, right, up, radius * 1.35, 0.0, FxSprites.GLOW, rgb, rgb,
						Math.min(1.0F, alphaCenter * 1.15F), light);
				return;
			}
		}
		for (int i = 0; i < segments; i++) {
			double a0 = MathHelper.TAU * i / segments;
			double a1 = MathHelper.TAU * (i + 1) / segments;
			Vec3d p0 = c.add(right.multiply(Math.cos(a0) * radius)).add(up.multiply(Math.sin(a0) * radius));
			Vec3d p1 = c.add(right.multiply(Math.cos(a1) * radius)).add(up.multiply(Math.sin(a1) * radius));
			vertex(vc, m, c.x, c.y, c.z, rgb, alphaCenter);
			vertex(vc, m, c.x, c.y, c.z, rgb, alphaCenter);
			vertex(vc, m, p0.x, p0.y, p0.z, rgb, alphaEdge);
			vertex(vc, m, p1.x, p1.y, p1.z, rgb, alphaEdge);
		}
	}

	/** Annulus in the plane spanned by right/up with an alpha profile inner → mid → outer. */
	public static void ring(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d right, Vec3d up, double inner, double outer,
			int rgb, float alphaInner, float alphaMid, float alphaOuter, int segments) {
		double mid = (inner + outer) * 0.5;
		for (int i = 0; i < segments; i++) {
			double a0 = MathHelper.TAU * i / segments;
			double a1 = MathHelper.TAU * (i + 1) / segments;
			double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
			band(vc, m, c, right, up, c0, s0, c1, s1, inner, mid, rgb, alphaInner, alphaMid);
			band(vc, m, c, right, up, c0, s0, c1, s1, mid, outer, rgb, alphaMid, alphaOuter);
		}
	}

	private static void band(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d right, Vec3d up, double c0, double s0, double c1, double s1,
			double r0, double r1, int rgb, float a0, float a1) {
		Vec3d p00 = c.add(right.multiply(c0 * r0)).add(up.multiply(s0 * r0));
		Vec3d p01 = c.add(right.multiply(c1 * r0)).add(up.multiply(s1 * r0));
		Vec3d p10 = c.add(right.multiply(c0 * r1)).add(up.multiply(s0 * r1));
		Vec3d p11 = c.add(right.multiply(c1 * r1)).add(up.multiply(s1 * r1));
		vertex(vc, m, p00.x, p00.y, p00.z, rgb, a0);
		vertex(vc, m, p01.x, p01.y, p01.z, rgb, a0);
		vertex(vc, m, p11.x, p11.y, p11.z, rgb, a1);
		vertex(vc, m, p10.x, p10.y, p10.z, rgb, a1);
	}

	/** UV sphere. Alpha can fall off towards the silhouette for a soft "fresnel" glow. */
	public static void sphere(VertexConsumer vc, Matrix4f m, Vec3d c, double radius, Vec3d eye, int rgb, float alphaFacing,
			float alphaEdge, int lat, int lon) {
		net.minecraft.client.render.RenderLayer layer = FxBuffers.current() != null ? FxBuffers.layerOf(vc) : null;
		if (layer == FxRenderLayers.GLOW || layer == FxRenderLayers.GLOW_NO_DEPTH || layer == FxRenderLayers.SHADE) {
			if (alphaFacing >= alphaEdge) {
				// A glowing (or dark) ball reads better as one smooth sprite than as a faceted mesh.
				net.minecraft.client.render.RenderLayer to = layer == FxRenderLayers.SHADE ? FxSprites.SMOKE_LAYER
						: layer == FxRenderLayers.GLOW ? FxSprites.GLOW_LAYER : FxSprites.GLOW_TOP_LAYER;
				Vec3d[] cb = sphereBasis(c);
				int light = to == FxSprites.SMOKE_LAYER ? FxSprites.light(c, 0.0F) : FxSprites.FULL_BRIGHT;
				FxSprites.sprite(FxBuffers.current().getBuffer(to), m, c, cb[0], cb[1], radius * 1.45, 0.0, FxSprites.GLOW, rgb, rgb,
						Math.min(1.0F, alphaFacing * 1.1F + alphaEdge * 0.3F), light);
				return;
			}
			// Rim-lit shells keep their geometry, with enough facets not to show them.
			lat = Math.max(lat, 16);
			lon = Math.max(lon, 24);
		}
		for (int i = 0; i < lat; i++) {
			double t0 = Math.PI * i / lat - Math.PI / 2;
			double t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
			for (int j = 0; j < lon; j++) {
				double p0 = MathHelper.TAU * j / lon;
				double p1 = MathHelper.TAU * (j + 1) / lon;
				Vec3d n00 = unit(t0, p0), n01 = unit(t0, p1), n11 = unit(t1, p1), n10 = unit(t1, p0);
				sphereVertex(vc, m, c, radius, n00, eye, rgb, alphaFacing, alphaEdge);
				sphereVertex(vc, m, c, radius, n01, eye, rgb, alphaFacing, alphaEdge);
				sphereVertex(vc, m, c, radius, n11, eye, rgb, alphaFacing, alphaEdge);
				sphereVertex(vc, m, c, radius, n10, eye, rgb, alphaFacing, alphaEdge);
			}
		}
	}

	private static Vec3d unit(double theta, double phi) {
		double ct = Math.cos(theta);
		return new Vec3d(ct * Math.cos(phi), Math.sin(theta), ct * Math.sin(phi));
	}

	private static void sphereVertex(VertexConsumer vc, Matrix4f m, Vec3d c, double r, Vec3d n, Vec3d eye, int rgb, float aFacing, float aEdge) {
		Vec3d p = c.add(n.multiply(r));
		Vec3d view = eye.subtract(p);
		double len = view.length();
		float facing = len < 1.0E-4 ? 1.0F : (float) Math.abs(n.dotProduct(view.multiply(1.0 / len)));
		vertex(vc, m, p.x, p.y, p.z, rgb, MathHelper.lerp(facing, aEdge, aFacing));
	}

	/** A basis facing the camera (at the origin) from a camera-relative point. */
	static Vec3d[] sphereBasis(Vec3d c) {
		Vec3d f = c.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : c.normalize();
		Vec3d right = f.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1.0E-6) {
			right = new Vec3d(1, 0, 0);
		}
		right = right.normalize();
		return new Vec3d[]{right, right.crossProduct(f).normalize()};
	}

	/** Camera basis vectors from yaw/pitch (degrees). Returns {right, up}. */
	public static Vec3d[] cameraBasis(float yaw, float pitch) {
		Vec3d forward = Vec3d.fromPolar(pitch, yaw);
		Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
		if (right.lengthSquared() < 1.0E-6) {
			right = new Vec3d(1, 0, 0);
		}
		right = right.normalize();
		Vec3d up = right.crossProduct(forward).normalize();
		return new Vec3d[]{right, up};
	}

	/** The client world, or null outside a running game (the offline preview renderer). */
	@org.jetbrains.annotations.Nullable
	public static net.minecraft.client.world.ClientWorld world() {
		net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
		return mc == null ? null : mc.world;
	}

	private FxDraw() {
	}
}
