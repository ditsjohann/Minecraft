package com.armageddon.client.render;

import com.armageddon.entity.WmdProjectileEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Tumbling 3D grenade. */
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
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
