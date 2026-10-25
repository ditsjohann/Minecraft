package com.armageddon.client.render;

import com.armageddon.client.fx.ClientFx;
import com.armageddon.entity.StarEntity;
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
 * A miniature sun: a granulated photosphere with limb darkening, chromosphere, streaming corona,
 * looping prominences that periodically erupt as flares, and an anamorphic lens streak.
 * Also feeds the god-ray shader.
 */
public class StarRenderer extends EntityRenderer<StarEntity> {
	public StarRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(StarEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(StarEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		float r = entity.getRadius(tickDelta);
		if (r <= 0.05F) {
			return;
		}
		float t = entity.getAgeTicks() + tickDelta;
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta).add(0, 1.0, 0));
		Vec3d[] cb = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		boolean collapsing = t > StarEntity.RISE + StarEntity.BURN;
		int core = collapsing ? 0xF0F4FF : 0xFFFBEA;
		int body = collapsing ? 0x90B0FF : 0xFFB030;
		int corona = collapsing ? 0x5A7CFF : 0xFF6A10;
		int deep = collapsing ? 0x2A3AA0 : 0xC02A08;
		boolean near = eye.length() < r * 60;

		matrices.push();
		matrices.translate(0, 1.0, 0);
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);

		// Wide atmosphere first, so the bright layers sit on top of it.
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 7.5, deep, 0.25F, 0.0F, 48);
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 4.5, corona, 0.45F, 0.0F, 48);
		corona(glow, m, cb, eye, r, t, corona, body);

		// Photosphere: hot centre, darker orange limb, then a thin bright chromosphere rim.
		FxDraw.sphere(glow, m, Vec3d.ZERO, r, eye, core, 1.0F, 0.35F, 18, 28);
		FxDraw.sphere(glow, m, Vec3d.ZERO, r * 1.02, eye, body, 0.2F, 0.85F, 18, 28);
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 0.98, r * 1.14, mix(body, 0xFFFFFF, 0.4F), 0.0F, 0.9F, 0.0F, 64);
		if (near) {
			granules(glow, m, cb, eye, r, t, core, body);
		}
		prominences(glow, m, cb, eye, r, t, corona, core);
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 2.1, body, 0.75F, 0.0F, 40);

		// Lens: a long anamorphic streak and six soft spikes.
		streak(glow, m, cb, eye, r, mix(body, 0xA0C8FF, 0.45F));
		for (int k = 0; k < 6; k++) {
			double a = Math.PI / 3 * k + 0.26 + t * 0.002;
			Vec3d dir = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
			double len = r * (k % 2 == 0 ? 8.0 : 5.0);
			FxDraw.ribbon(glow, m, dir.multiply(r * 0.6), dir.multiply(len), eye, r * 0.12, core, 0.75F, 0.0F);
		}
		matrices.pop();

		ClientFx.light(entity.getLerpedPos(tickDelta).add(0, 1, 0), 1.0F, collapsing ? 0xC0D0FF : 0xFFC060);
		ClientFx.tintSky(collapsing ? 0x4060C0 : 0xFF8A30, 0.6F);
		ClientFx.distortion = Math.max(ClientFx.distortion, 0.35F);
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	/** Horizontal anamorphic flare: bright in the middle, tapering to nothing at both ends. */
	private static void streak(VertexConsumer vc, Matrix4f m, Vec3d[] cb, Vec3d eye, double r, int rgb) {
		FxDraw.ribbon(vc, m, Vec3d.ZERO, cb[0].multiply(r * 16), eye, r * 0.2, rgb, 0.7F, 0.0F);
		FxDraw.ribbon(vc, m, Vec3d.ZERO, cb[0].multiply(-r * 16), eye, r * 0.2, rgb, 0.7F, 0.0F);
		FxDraw.ribbon(vc, m, Vec3d.ZERO, cb[0].multiply(r * 7), eye, r * 0.07, 0xFFFFFF, 0.9F, 0.0F);
		FxDraw.ribbon(vc, m, Vec3d.ZERO, cb[0].multiply(-r * 7), eye, r * 0.07, 0xFFFFFF, 0.9F, 0.0F);
	}

	/** Coronal streamers: soft radial petals of uneven length that slowly breathe and turn. */
	private static void corona(VertexConsumer vc, Matrix4f m, Vec3d[] cb, Vec3d eye, double r, float t, int corona, int body) {
		int n = 22;
		for (int i = 0; i < n; i++) {
			double a = i * MathHelper.TAU / n + t * 0.003 + hash(i, 3) * 0.2;
			double len = r * (2.2 + 2.2 * hash(i, 7) + 0.6 * Math.sin(t * 0.04 + i * 2.3));
			Vec3d dir = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
			FxDraw.ribbon(vc, m, dir.multiply(r * 0.9), dir.multiply(len), eye, r * (0.5 + 0.4 * hash(i, 11)), i % 3 == 0 ? body : corona, 0.35F, 0.0F);
		}
	}

	/** Convection cells: bright spots dancing on the visible face. */
	private static void granules(VertexConsumer vc, Matrix4f m, Vec3d[] cb, Vec3d eye, double r, float t, int core, int body) {
		Vec3d view = eye.normalize();
		for (int i = 0; i < 70; i++) {
			double th = hash(i, 1) * MathHelper.TAU + t * 0.004;
			double y = hash(i, 2) * 2 - 1;
			double rr = Math.sqrt(1 - y * y);
			Vec3d n = new Vec3d(Math.cos(th) * rr, y, Math.sin(th) * rr);
			double facing = n.dotProduct(view);
			if (facing < 0.15) {
				continue;
			}
			float life = 0.5F + 0.5F * MathHelper.sin(t * (0.08F + (float) hash(i, 4) * 0.1F) + i);
			FxDraw.disc(vc, m, n.multiply(r * 1.005), cb[0], cb[1], r * (0.07 + 0.08 * hash(i, 5)), i % 4 == 0 ? body : core, 0.55F * life * (float) facing, 0.0F, 8);
		}
	}

	/** Looping plasma arcs on the limb; every so often one tears loose and erupts outward as a flare. */
	private static void prominences(VertexConsumer vc, Matrix4f m, Vec3d[] cb, Vec3d eye, double r, float t, int corona, int core) {
		for (int k = 0; k < 7; k++) {
			double a = k * MathHelper.TAU / 7 + t * 0.01 * (k % 2 == 0 ? 1 : -1);
			double cycle = ((t + k * 37) % 160) / 160.0;
			boolean erupting = k % 3 == 0 && cycle > 0.7;
			double burst = erupting ? (cycle - 0.7) / 0.3 : 0;
			double h = r * (0.4 + 0.3 * Math.sin(t * 0.05 + k * 1.3) + burst * 3.5);
			float fade = erupting ? (float) (1 - burst) : 1.0F;
			Vec3d dir = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
			Vec3d side = cb[0].multiply(-Math.sin(a)).add(cb[1].multiply(Math.cos(a)));
			Vec3d prev = null;
			for (int i = 0; i <= 12; i++) {
				double u = i / 12.0;
				double lift = Math.sin(u * Math.PI) * h;
				Vec3d p = dir.multiply(r + lift).add(side.multiply((u - 0.5) * r * (0.8 + burst)));
				if (prev != null) {
					FxDraw.ribbon(vc, m, prev, p, eye, r * 0.2, corona, 0.55F * fade, 0.55F * fade);
					FxDraw.ribbon(vc, m, prev, p, eye, r * 0.06, core, 0.9F * fade, 0.9F * fade);
				}
				prev = p;
			}
		}
	}

	private static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	private static int mix(int a, int b, float t) {
		int r = (int) MathHelper.lerp(t, (float) ((a >> 16) & 0xFF), (float) ((b >> 16) & 0xFF));
		int g = (int) MathHelper.lerp(t, (float) ((a >> 8) & 0xFF), (float) ((b >> 8) & 0xFF));
		int bl = (int) MathHelper.lerp(t, (float) (a & 0xFF), (float) (b & 0xFF));
		return (r << 16) | (g << 8) | bl;
	}

	@Override
	public Identifier getTexture(StarEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
