package com.armageddon.client.fx;

import com.armageddon.client.render.FxRenderLayers;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Draws every live {@link FxEffect} after translucent terrain. */
public final class FxWorldRenderer {
	public static void render(WorldRenderContext ctx) {
		if (ClientFx.EFFECTS.isEmpty()) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		Camera camera = ctx.camera();
		Vec3d cam = camera.getPos();
		float tickDelta = ctx.tickCounter().getTickDelta(false);
		MatrixStack matrices = new MatrixStack();
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
		java.util.List<FxEffect> broken = null;
		for (FxEffect effect : ClientFx.EFFECTS) {
			try {
				effect.render(m, consumers, cam, tickDelta, camera.getYaw(), camera.getPitch());
			} catch (RuntimeException ex) {
				com.armageddon.Armageddon.LOGGER.error("Armageddon effect {} failed while rendering; removing it", effect.getClass().getSimpleName(), ex);
				if (broken == null) {
					broken = new java.util.ArrayList<>();
				}
				broken.add(effect);
			}
		}
		if (broken != null) {
			ClientFx.EFFECTS.removeAll(broken);
		}
		consumers.draw(FxRenderLayers.SOLID);
		consumers.draw(FxRenderLayers.VOID);
		consumers.draw(FxRenderLayers.SHADE);
		consumers.draw(FxRenderLayers.GLOW);
		consumers.draw(FxRenderLayers.GLOW_NO_DEPTH);
	}

	private FxWorldRenderer() {
	}
}
