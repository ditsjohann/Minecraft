package com.armageddon.client.render;

import com.armageddon.entity.PrimedBombEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.TntMinecartEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Armed warhead: flashing block model that swells before detonation, plus a warning beacon. */
public class PrimedBombRenderer extends EntityRenderer<PrimedBombEntity> {
	private final BlockRenderManager blocks;

	public PrimedBombRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.blocks = ctx.getBlockRenderManager();
		this.shadowRadius = 0.5F;
	}

	@Override
	public void render(PrimedBombEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		matrices.push();
		int fuse = entity.getFuse();
		float f = fuse - tickDelta + 1.0F;
		if (f < 10.0F) {
			float g = 1.0F - f / 10.0F;
			g = MathHelper.clamp(g, 0.0F, 1.0F);
			g *= g;
			g *= g;
			float s = 1.0F + g * 0.3F;
			matrices.scale(s, s, s);
		}
		matrices.translate(-0.5F, 0.0F, -0.5F);
		boolean flash = fuse / 5 % 2 == 0;
		TntMinecartEntityRenderer.renderFlashingBlock(blocks, entity.getBlockState(), matrices, consumers, light, flash);
		matrices.pop();

		if (entity.getBombType().isStrategic()) {
			float pulse = 0.5F + 0.5F * MathHelper.sin((entity.age + tickDelta) * (0.3F + Math.max(0, 200 - fuse) * 0.006F));
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
			Vec3d top = new Vec3d(0, 1.15, 0);
			FxDraw.disc(vc, matrices.peek().getPositionMatrix(), top, basis[0], basis[1], 0.6 + pulse * 0.4, 0xFF2020, pulse, 0.0F, 16);
			FxDraw.beam(vc, matrices.peek().getPositionMatrix(), top, top.add(0, 6, 0),
					this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta)), 0.12, 0xFF3030, pulse * 0.6F);
		}
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(PrimedBombEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
