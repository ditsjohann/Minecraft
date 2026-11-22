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
		if (ClientFx.EFFECTS.isEmpty() && !com.armageddon.client.vfx.FxEngine.active()) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		Camera camera = ctx.camera();
		Vec3d cam = camera.getPos();
		float tickDelta = ctx.tickCounter().getTickDelta(false);
		MatrixStack matrices = new MatrixStack();
		Matrix4f m = matrices.peek().getPositionMatrix();
		// Our own buffers: one per layer, so interleaved layers never flush each other (and shader packs get lit layers).
		com.armageddon.client.render.FxBuffers consumers = com.armageddon.client.render.FxBuffers.begin();
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
				if (effect instanceof Cinematics.Cine c) {
					// A cutscene that can't draw must still give the camera back.
					c.done = true;
				}
			}
		}
		if (broken != null) {
			ClientFx.EFFECTS.removeAll(broken);
		}
		com.armageddon.client.vfx.FxEngine.render(m, consumers, cam, tickDelta, camera.getYaw(), camera.getPitch());
		consumers.finish();
	}

	private FxWorldRenderer() {
	}
}
