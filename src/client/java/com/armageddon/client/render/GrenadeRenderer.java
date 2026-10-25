package com.armageddon.client.render;

import com.armageddon.entity.WmdProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Tumbling 3D grenade with a blinking arming light and a faint fuse-spark trail. */
public class GrenadeRenderer extends EntityRenderer<WmdProjectileEntity> {
	private final ItemRenderer itemRenderer;

	public GrenadeRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.itemRenderer = ctx.getItemRenderer();
		this.shadowRadius = 0.15F;
	}

	@Override
	public void render(WmdProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		matrices.push();
		matrices.translate(0.0F, 0.15F, 0.0F);
		double speed = entity.getVelocity().length();
		if (!entity.isStuck() && speed > 0.05) {
			float spin = (entity.age + tickDelta) * 30.0F;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin));
		} else {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getId() * 37 % 360));
		}
		matrices.scale(0.85F, 0.85F, 0.85F);
		itemRenderer.renderItem(entity.getStack(), ModelTransformationMode.GROUND, light, OverlayTexture.DEFAULT_UV, matrices, consumers, entity.getWorld(), entity.getId());
		matrices.pop();

		float time = entity.age + tickDelta;
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		Vec3d c = new Vec3d(0, 0.2, 0);
		// Blinks faster the longer it has been live.
		float rate = 0.25F + Math.min(1.2F, time * 0.01F);
		float blink = MathHelper.sin(time * rate) > 0.6F ? 1.0F : 0.15F;
		FxDraw.disc(vc, m, c.add(0, 0.12, 0), cb[0], cb[1], 0.35 * blink + 0.05, 0xFF3020, 0.7F * blink, 0.0F, 10);
		FxDraw.disc(vc, m, c.add(0, 0.12, 0), cb[0], cb[1], 0.07, 0xFFFFFF, blink, 0.0F, 8);
		if (!entity.isStuck() && speed > 0.05) {
			Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
			Vec3d tail = c.subtract(entity.getVelocity().multiply(3.0));
			FxDraw.ribbon(vc, m, c, tail, eye, 0.18, 0xFFA040, 0.45F, 0.0F);
			FxDraw.ribbon(vc, m, c, c.lerp(tail, 0.4), eye, 0.06, 0xFFFFFF, 0.6F, 0.0F);
		}
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
