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
 * A miniature sun: layered photosphere, chromosphere, a huge corona, rotating solar
 * prominences and lens spikes. Also feeds the god-ray shader.
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
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		boolean collapsing = t > StarEntity.RISE + StarEntity.BURN;
		int core = collapsing ? 0xE8F0FF : 0xFFF8E0;
		int body = collapsing ? 0x90B0FF : 0xFFB030;
		int corona = collapsing ? 0x5A7CFF : 0xFF6A10;

		matrices.push();
		matrices.translate(0, 1.0, 0);
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		FxDraw.sphere(glow, m, Vec3d.ZERO, r, eye, core, 1.0F, 0.6F, 16, 24);
		FxDraw.sphere(glow, m, Vec3d.ZERO, r * 1.12, eye, body, 0.25F, 0.9F, 16, 24);
		FxDraw.disc(glow, m, Vec3d.ZERO, basis[0], basis[1], r * 4.5, corona, 0.55F, 0.0F, 48);
		FxDraw.disc(glow, m, Vec3d.ZERO, basis[0], basis[1], r * 2.0, body, 0.8F, 0.0F, 40);
		// Prominences: looping arcs of plasma around the limb.
		for (int k = 0; k < 7; k++) {
			double a = k * MathHelper.TAU / 7 + t * 0.01 * (k % 2 == 0 ? 1 : -1);
			double h = r * (0.4 + 0.3 * Math.sin(t * 0.05 + k * 1.3));
			Vec3d dir = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			Vec3d side = basis[0].multiply(-Math.sin(a)).add(basis[1].multiply(Math.cos(a)));
			Vec3d prev = null;
			for (int i = 0; i <= 8; i++) {
				double u = i / 8.0;
				double lift = Math.sin(u * Math.PI) * h;
				Vec3d p = dir.multiply(r + lift).add(side.multiply((u - 0.5) * r * 0.8));
				if (prev != null) {
					FxDraw.ribbon(glow, m, prev, p, eye, r * 0.12, corona, 0.9F, 0.9F);
				}
				prev = p;
			}
		}
		// Lens spikes.
		for (int k = 0; k < 3; k++) {
			double a = Math.PI / 3 * k + 0.3;
			Vec3d dir = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			FxDraw.ribbon(glow, m, Vec3d.ZERO, dir.multiply(r * 7), eye, r * 0.15, core, 0.8F, 0.0F);
			FxDraw.ribbon(glow, m, Vec3d.ZERO, dir.multiply(-r * 7), eye, r * 0.15, core, 0.8F, 0.0F);
		}
		matrices.pop();

		ClientFx.light(entity.getLerpedPos(tickDelta).add(0, 1, 0), 1.0F, collapsing ? 0xC0D0FF : 0xFFC060);
		ClientFx.tintSky(collapsing ? 0x4060C0 : 0xFF8A30, 0.6F);
		ClientFx.distortion = Math.max(ClientFx.distortion, 0.35F);
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(StarEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
