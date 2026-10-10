package com.armageddon.client.render;

import com.armageddon.Armageddon;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Renders rockets, mini-nukes and bomblets as 3D models pointing along their flight path, with engine glow. */
public class ProjectileModelRenderer extends EntityRenderer<WmdProjectileEntity> {
	public static final Identifier ROCKET_MODEL = Armageddon.id("entity/rocket");
	public static final Identifier MINI_NUKE_MODEL = Armageddon.id("entity/mini_nuke");
	public static final Identifier BOMBLET_MODEL = Armageddon.id("entity/bomblet");
	public static final Identifier METEOR_MODEL = Armageddon.id("entity/meteor");
	public static final Identifier ROD_MODEL = Armageddon.id("entity/rod");

	private final ItemRenderer itemRenderer;

	public ProjectileModelRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.itemRenderer = ctx.getItemRenderer();
	}

	@Override
	public void render(WmdProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		ProjectileKind kind = entity.getKind();
		Identifier modelId = switch (kind) {
			case MINI_NUKE, GATLING_NUKE, FALLING_NUKE -> MINI_NUKE_MODEL;
			case BOMBLET, AIR_BOMB -> BOMBLET_MODEL;
			case METEOR -> METEOR_MODEL;
			case ROD -> ROD_MODEL;
			default -> ROCKET_MODEL;
		};
		BakedModel model = MinecraftClient.getInstance().getBakedModelManager().getModel(modelId);

		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((entity.age + tickDelta) * (kind == ProjectileKind.METEOR ? 9.0F : 25.0F)));
		if (kind == ProjectileKind.METEOR) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((entity.age + tickDelta) * 7.0F));
		}
		float s = switch (kind) {
			case BOMBLET -> 0.6F;
			case GATLING_NUKE -> 0.55F;
			case AIR_BOMB -> 1.3F;
			case METEOR -> 2.6F;
			case ROD -> 1.6F;
			default -> 1.0F;
		};
		matrices.scale(s, s, s);
		if (model != null) {
			itemRenderer.renderItem(entity.getStack(), ModelTransformationMode.NONE, false, matrices, consumers, light, OverlayTexture.DEFAULT_UV, model);
		}
		if (kind == ProjectileKind.METEOR || kind == ProjectileKind.ROD) {
			Matrix4f m = matrices.peek().getPositionMatrix();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float flicker = 0.8F + 0.2F * MathHelper.sin((entity.age + tickDelta) * 4.3F);
			int hot = kind == ProjectileKind.METEOR ? 0xFF6A10 : 0xBFE8FF;
			FxDraw.sphere(vc, m, Vec3d.ZERO, 0.75 * flicker, new Vec3d(0, 0, 3), hot, 0.15F, 0.8F, 8, 12);
			Vec3d nose = new Vec3d(0.6, 0, 0);
			FxDraw.ribbon(vc, m, nose, nose.add(-6.0 * flicker, 0, 0), new Vec3d(-1, 4, 0), 1.2, hot, 0.9F, 0.0F);
			FxDraw.ribbon(vc, m, nose, nose.add(-6.0 * flicker, 0, 0), new Vec3d(-1, 0, 4), 1.2, hot, 0.9F, 0.0F);
			FxDraw.ribbon(vc, m, nose, nose.add(-3.0, 0, 0), new Vec3d(-1, 4, 4), 0.5, 0xFFFFFF, 1.0F, 0.0F);
		} else if (kind != ProjectileKind.BOMBLET && kind != ProjectileKind.AIR_BOMB && kind != ProjectileKind.FALLING_NUKE) {
			// Engine plume at the tail (-X in model space).
			Matrix4f m = matrices.peek().getPositionMatrix();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float flicker = 0.75F + 0.25F * MathHelper.sin((entity.age + tickDelta) * 3.1F);
			Vec3d tail = new Vec3d(-0.55, 0, 0);
			FxDraw.disc(vc, m, tail, new Vec3d(0, 1, 0), new Vec3d(0, 0, 1), 0.35 * flicker, 0xFFB050, 0.9F, 0.0F, 12);
			FxDraw.disc(vc, m, tail, new Vec3d(0, 1, 0), new Vec3d(0, 0, 1), 0.16, 0xFFFFFF, 1.0F, 0.0F, 10);
			FxDraw.ribbon(vc, m, tail, tail.add(-1.4 * flicker, 0, 0), new Vec3d(-0.5, 3, 0), 0.3, 0xFF8A2A, 0.8F, 0.0F);
			FxDraw.ribbon(vc, m, tail, tail.add(-1.4 * flicker, 0, 0), new Vec3d(-0.5, 0, 3), 0.3, 0xFF8A2A, 0.8F, 0.0F);
		}
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
