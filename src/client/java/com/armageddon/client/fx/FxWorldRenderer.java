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
		for (FxEffect effect : ClientFx.EFFECTS) {
			effect.render(m, consumers, cam, tickDelta, camera.getYaw(), camera.getPitch());
		}
		consumers.draw(FxRenderLayers.SHADE);
		consumers.draw(FxRenderLayers.GLOW);
		consumers.draw(FxRenderLayers.GLOW_NO_DEPTH);
	}

	private FxWorldRenderer() {
	}
}
