package com.armageddon.client.render;

import com.armageddon.entity.DevourerEntity;
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

/** The Devourer: a crimson singularity with a jagged, churning maw and tendrils of torn light. */
public class DevourerRenderer extends EntityRenderer<DevourerEntity> {
	public DevourerRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(DevourerEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(DevourerEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		float r = entity.getRadius();
		float time = entity.age + tickDelta;
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		double core = r * 0.45 * (1 + 0.05 * Math.sin(time * 0.6));

		matrices.push();
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		FxDraw.disc(glow, m, Vec3d.ZERO, basis[0], basis[1], r * 3.0, 0x600008, 0.45F, 0.0F, 40);
		FxDraw.ring(glow, m, Vec3d.ZERO, basis[0], basis[1], core, core * 1.4, 0xFF3020, 0.0F, 1.0F, 0.0F, 64);
		// Jagged maw: spikes around the horizon.
		for (int i = 0; i < 18; i++) {
			double a = i * MathHelper.TAU / 18 + time * 0.03;
			double len = core * (1.6 + 0.5 * Math.sin(time * 0.4 + i * 2.1));
			Vec3d d = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			FxDraw.ribbon(glow, m, d.multiply(core), d.multiply(len), Vec3d.ZERO, core * 0.22, 0xFF5030, 0.8F, 0.0F);
		}
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 9.0F));
		Matrix4f dm = matrices.peek().getPositionMatrix();
		Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 1, 0);
		FxDraw.ring(glow, dm, Vec3d.ZERO, x, z, core * 1.3, core * 2.6, 0xFF2010, 0.2F, 0.7F, 0.0F, 64);
		FxDraw.ring(glow, dm, Vec3d.ZERO, x, z, core * 2.4, core * 4.0, 0x900020, 0.0F, 0.35F, 0.0F, 64);
		matrices.pop();
		VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
		FxDraw.sphere(dark, m, Vec3d.ZERO, core, eye, 0x000000, 1.0F, 1.0F, 14, 22);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(DevourerEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
