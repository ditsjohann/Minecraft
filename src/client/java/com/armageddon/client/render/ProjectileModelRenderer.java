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
			case TITAN_MISSILE -> 3.2F;
			case SIEGE_MISSILE -> 1.3F;
			default -> 1.0F;
		};
		matrices.scale(s, s, s);
		if (model != null) {
			itemRenderer.renderItem(entity.getStack(), ModelTransformationMode.NONE, false, matrices, consumers, light, OverlayTexture.DEFAULT_UV, model);
		}
		float time = entity.age + tickDelta;
		if (kind == ProjectileKind.TITAN_MISSILE || kind == ProjectileKind.SIEGE_MISSILE) {
			Matrix4f tm = matrices.peek().getPositionMatrix();
			VertexConsumer tvc = consumers.getBuffer(FxRenderLayers.GLOW);
			float f = 0.8F + 0.2F * MathHelper.sin(time * 5.1F);
			int c = kind == ProjectileKind.TITAN_MISSILE ? 0xFF9040 : 0xB070FF;
			plume(tvc, tm, new Vec3d(-0.6, 0, 0), 3.0 * f, 0.5, c, time);
			FxDraw.sphere(tvc, tm, new Vec3d(-0.6, 0, 0), 0.35 * f, new Vec3d(-3, 0, 0), c, 0.3F, 0.9F, 6, 10);
			// Nav lights on the hull.
			float blink = MathHelper.sin(time * 0.6F) > 0.5F ? 1.0F : 0.0F;
			FxDraw.sphere(tvc, tm, new Vec3d(0.3, 0.18, 0), 0.05, new Vec3d(0, 3, 0), 0xFF2020, blink, blink, 4, 6);
		}
		if (kind == ProjectileKind.METEOR || kind == ProjectileKind.ROD) {
			Matrix4f m = matrices.peek().getPositionMatrix();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float flicker = 0.8F + 0.2F * MathHelper.sin(time * 4.3F);
			boolean meteor = kind == ProjectileKind.METEOR;
			int hot = meteor ? 0xFF6A10 : 0xBFE8FF;
			int core = meteor ? 0xFFE0A0 : 0xFFFFFF;
			// Plasma sheath: hot shock cap on the nose, glowing coma around it.
			FxDraw.sphere(vc, m, Vec3d.ZERO, 1.1 * flicker, new Vec3d(0, 0, 3), meteor ? 0xC02A08 : 0x6A8AFF, 0.08F, 0.5F, 8, 12);
			FxDraw.sphere(vc, m, Vec3d.ZERO, 0.75 * flicker, new Vec3d(0, 0, 3), hot, 0.15F, 0.8F, 8, 12);
			FxDraw.sphere(vc, m, new Vec3d(0.35, 0, 0), 0.45, new Vec3d(3, 0, 0), core, 0.6F, 0.2F, 6, 10);
			Vec3d nose = new Vec3d(0.6, 0, 0);
			// Tail as three crossed sheets, spinning with the body, so it reads from any angle.
			for (int k = 0; k < 3; k++) {
				double a = k * Math.PI / 3;
				Vec3d side = new Vec3d(-1, Math.cos(a) * 4, Math.sin(a) * 4);
				FxDraw.ribbon(vc, m, nose, nose.add(-7.5 * flicker, 0, 0), side, 1.4, hot, 0.65F, 0.0F);
				FxDraw.ribbon(vc, m, nose, nose.add(-4.0, 0, 0), side, 0.6, core, 0.9F, 0.0F);
			}
			// Shed sparks along the tail.
			for (int i = 0; i < 6; i++) {
				double u = ((time * 0.15 + i / 6.0) % 1.0);
				Vec3d p = new Vec3d(-u * 6, Math.sin(i * 2.1 + time * 0.3) * 0.6 * u, Math.cos(i * 1.7 + time * 0.3) * 0.6 * u);
				FxDraw.sphere(vc, m, p, 0.12 * (1 - u) + 0.04, new Vec3d(0, 3, 3), core, 0.9F * (float) (1 - u), 0.5F * (float) (1 - u), 4, 6);
			}
		} else if (kind != ProjectileKind.BOMBLET && kind != ProjectileKind.AIR_BOMB && kind != ProjectileKind.FALLING_NUKE) {
			// Engine plume at the tail (-X in model space).
			Matrix4f m = matrices.peek().getPositionMatrix();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float flicker = 0.75F + 0.25F * MathHelper.sin(time * 3.1F);
			Vec3d tail = new Vec3d(-0.55, 0, 0);
			FxDraw.disc(vc, m, tail, new Vec3d(0, 1, 0), new Vec3d(0, 0, 1), 0.42 * flicker, 0xFFB050, 0.9F, 0.0F, 12);
			FxDraw.disc(vc, m, tail, new Vec3d(0, 1, 0), new Vec3d(0, 0, 1), 0.18, 0xFFFFFF, 1.0F, 0.0F, 10);
			plume(vc, m, tail, 1.8 * flicker, 0.32, 0xFF8A2A, time);
		} else {
			// Free-falling bombs: a blinking arming light on the casing.
			Matrix4f m = matrices.peek().getPositionMatrix();
			VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
			float blink = MathHelper.sin(time * 0.8F) > 0.3F ? 1.0F : 0.1F;
			FxDraw.sphere(vc, m, new Vec3d(0.1, 0.32, 0), 0.07, new Vec3d(0, 3, 0), 0xFF2020, blink, blink * 0.6F, 4, 6);
			FxDraw.disc(vc, m, new Vec3d(0.1, 0.32, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 0.3 * blink, 0xFF3020, 0.5F * blink, 0.0F, 8);
		}
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
	}

	/**
	 * Rocket exhaust along -X: crossed sheets for an orange outer flame and a white inner cone,
	 * with flickering shock diamonds.
	 */
	private static void plume(VertexConsumer vc, Matrix4f m, Vec3d tail, double len, double width, int color, float time) {
		for (int k = 0; k < 2; k++) {
			Vec3d side = k == 0 ? new Vec3d(-0.5, 3, 0) : new Vec3d(-0.5, 0, 3);
			FxDraw.ribbon(vc, m, tail, tail.add(-len, 0, 0), side, width, color, 0.85F, 0.0F);
			FxDraw.ribbon(vc, m, tail, tail.add(-len * 0.45, 0, 0), side, width * 0.45, 0xFFF2D0, 1.0F, 0.0F);
			FxDraw.ribbon(vc, m, tail, tail.add(-len * 2.2, 0, 0), side, width * 1.8, color, 0.18F, 0.0F);
		}
		for (int i = 1; i <= 3; i++) {
			double x = -len * 0.18 * i;
			float f = (0.6F + 0.4F * MathHelper.sin(time * 2.3F + i)) * (1 - i / 4.0F);
			FxDraw.sphere(vc, m, tail.add(x, 0, 0), width * 0.45 * (1 - i * 0.15), new Vec3d(0, 3, 3), 0xFFFFFF, 0.6F * f, 0.2F * f, 4, 6);
		}
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
