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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Event horizon (pure black sphere), photon ring, tilted rotating accretion disc with a hot
 * inner edge, and a violet halo. Gravitational lensing is added by the post shader.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
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
		Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		double core = r * 0.5;

		matrices.push();
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		// Halo + photon ring (camera facing).
		FxDraw.disc(glow, m, Vec3d.ZERO, basis[0], basis[1], r * 3.2, 0x5A1AA0, 0.35F, 0.0F, 40);
		FxDraw.ring(glow, m, Vec3d.ZERO, basis[0], basis[1], core * 1.0, core * 1.35, 0xFFD8A0, 0.0F, 0.95F, 0.0F, 64);
		FxDraw.ring(glow, m, Vec3d.ZERO, basis[0], basis[1], core * 1.3, core * 2.1, 0xB070FF, 0.0F, 0.4F, 0.0F, 48);

		// Accretion disc, tilted and spinning.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(18.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 6.0F));
		Matrix4f dm = matrices.peek().getPositionMatrix();
		Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
		FxDraw.ring(glow, dm, Vec3d.ZERO, x, z, core * 1.25, core * 2.0, 0xFFF0D0, 0.0F, 0.9F, 0.2F, 72);
		FxDraw.ring(glow, dm, Vec3d.ZERO, x, z, core * 1.9, core * 3.6, 0xFF7A30, 0.25F, 0.55F, 0.0F, 72);
		FxDraw.ring(glow, dm, Vec3d.ZERO, x, z, core * 3.2, core * 5.0, 0x8A3AFF, 0.0F, 0.25F, 0.0F, 72);
		// Hot spots orbiting in the disc.
		for (int i = 0; i < 6; i++) {
			double a = i * MathHelper.TAU / 6 + time * 0.08 * (1 + i % 3);
			double rr = core * (1.6 + (i % 3) * 0.6);
			Vec3d p = new Vec3d(Math.cos(a) * rr, 0, Math.sin(a) * rr);
			FxDraw.disc(glow, dm, p, x, z, core * 0.35, 0xFFFFFF, 0.6F, 0.0F, 10);
		}
		matrices.pop();

		// Event horizon.
		VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
		FxDraw.sphere(dark, m, Vec3d.ZERO, core, eye, 0x000000, 1.0F, 1.0F, 16, 24);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(BlackHoleEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
