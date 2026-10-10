package com.armageddon.client.fx;

import com.armageddon.Armageddon;
import com.armageddon.entity.BlackHoleEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Full-screen post-processing pass ({@code shaders/post/armageddon_fx.json}):
 * nuclear flash bloom, chromatic aberration + heat haze, radiation sickness vision (green
 * tint, film grain, vignette) and real-time gravitational lensing around black holes.
 */
public final class PostFx {
	private static final Identifier EFFECT = Armageddon.id("shaders/post/armageddon_fx.json");

	private static PostEffectProcessor processor;
	private static boolean failed;
	private static int width = -1;
	private static int height = -1;
	private static float time;

	/** Radiation-vision intensity 0..1, set by the HUD/tick logic. */
	public static float radiation;
	private static float lensX, lensY, lensR, lensStrength;
	private static float godX, godY, godStrength;
	/** True if the shader rendered this frame (HUD falls back to an overlay otherwise). */
	public static boolean renderedThisFrame;

	public static void invalidate() {
		if (processor != null) {
			processor.close();
			processor = null;
		}
		failed = false;
		width = height = -1;
	}

	private static boolean needed() {
		return ClientFx.flash > 0.002F || ClientFx.distortion > 0.002F || radiation > 0.002F || lensStrength > 0.0F
				|| godStrength > 0.0F || ClientFx.hue > 0.0F || ClientFx.ripple < 1.0F
				|| ClientFx.SCREEN[0] > 0 || ClientFx.SCREEN[1] > 0 || ClientFx.SCREEN[2] > 0 || ClientFx.SCREEN[3] > 0 || ClientFx.SCREEN[4] > 0;
	}

	/** Called at the end of GameRenderer#renderWorld (after the hand, before the HUD). */
	public static void render(float tickDelta) {
		renderedThisFrame = false;
		if (!needed()) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (processor == null && !failed) {
			try {
				processor = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), EFFECT);
			} catch (Exception e) {
				failed = true;
				Armageddon.LOGGER.warn("Armageddon post-processing shader unavailable, falling back to overlays", e);
			}
		}
		if (processor == null) {
			return;
		}
		Framebuffer fb = mc.getFramebuffer();
		if (fb.textureWidth != width || fb.textureHeight != height) {
			width = fb.textureWidth;
			height = fb.textureHeight;
			processor.setupDimensions(width, height);
		}
		time += mc.getRenderTickCounter().getLastFrameDuration();
		processor.setUniforms("Flash", MathHelper.clamp(ClientFx.flash, 0.0F, 1.0F));
		processor.setUniforms("FlashR", ((ClientFx.flashColor >> 16) & 0xFF) / 255.0F);
		processor.setUniforms("FlashG", ((ClientFx.flashColor >> 8) & 0xFF) / 255.0F);
		processor.setUniforms("FlashB", (ClientFx.flashColor & 0xFF) / 255.0F);
		processor.setUniforms("Distortion", MathHelper.clamp(ClientFx.distortion, 0.0F, 1.0F));
		processor.setUniforms("Radiation", MathHelper.clamp(radiation, 0.0F, 1.0F));
		processor.setUniforms("GTime", time / 20.0F);
		processor.setUniforms("LensX", lensX);
		processor.setUniforms("LensY", lensY);
		processor.setUniforms("LensR", lensR);
		processor.setUniforms("LensStrength", lensStrength);
		processor.setUniforms("GodX", godX);
		processor.setUniforms("GodY", godY);
		processor.setUniforms("GodStrength", godStrength);
		processor.setUniforms("GodR", ((ClientFx.lightColor >> 16) & 0xFF) / 255.0F);
		processor.setUniforms("GodG", ((ClientFx.lightColor >> 8) & 0xFF) / 255.0F);
		processor.setUniforms("GodB", (ClientFx.lightColor & 0xFF) / 255.0F);
		processor.setUniforms("Ripple", ClientFx.ripple);
		processor.setUniforms("Hue", MathHelper.clamp(ClientFx.hue, 0.0F, 1.0F));
		processor.setUniforms("Desaturate", ClientFx.SCREEN[0]);
		processor.setUniforms("Whiteout", ClientFx.SCREEN[1]);
		processor.setUniforms("Blackout", ClientFx.SCREEN[2]);
		processor.setUniforms("Static", ClientFx.SCREEN[3]);
		processor.setUniforms("Invert", ClientFx.SCREEN[4]);

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		processor.render(tickDelta);
		fb.beginWrite(true);
		renderedThisFrame = true;
	}

	/** Finds the most prominent on-screen black hole and projects it into screen space. */
	public static void updateLens(WorldRenderContext ctx) {
		lensStrength = 0;
		updateGodRays(ctx);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) {
			return;
		}
		Vec3d cam = ctx.camera().getPos();
		float tickDelta = ctx.tickCounter().getTickDelta(false);
		Matrix4f view = ctx.positionMatrix();
		Matrix4f proj = ctx.projectionMatrix();
		float best = 0;
		for (Entity e : mc.world.getEntities()) {
			if (!(e instanceof BlackHoleEntity hole)) {
				continue;
			}
			float r = hole.getCurrentRadius(tickDelta);
			if (r <= 0.05F) {
				continue;
			}
			Vec3d p = e.getLerpedPos(tickDelta).subtract(cam);
			Vector4f v = new Vector4f((float) p.x, (float) p.y, (float) p.z, 1.0F);
			view.transform(v);
			proj.transform(v);
			if (v.w <= 0.1F) {
				continue;
			}
			float sx = v.x / v.w * 0.5F + 0.5F;
			float sy = v.y / v.w * 0.5F + 0.5F;
			float screenR = r * 0.55F * proj.m11() / v.w * 0.5F;
			if (sx < -0.5F || sx > 1.5F || sy < -0.5F || sy > 1.5F) {
				continue;
			}
			if (screenR > best) {
				best = screenR;
				lensX = sx;
				lensY = sy;
				lensR = Math.min(screenR, 0.6F);
				lensStrength = 1.0F;
			}
		}
	}

	private static void updateGodRays(WorldRenderContext ctx) {
		godStrength = 0;
		Vec3d light = ClientFx.lightPos;
		if (light == null || ClientFx.lightStrength <= 0) {
			return;
		}
		Vec3d p = light.subtract(ctx.camera().getPos());
		Vector4f v = new Vector4f((float) p.x, (float) p.y, (float) p.z, 1.0F);
		ctx.positionMatrix().transform(v);
		ctx.projectionMatrix().transform(v);
		if (v.w <= 0.1F) {
			return;
		}
		godX = v.x / v.w * 0.5F + 0.5F;
		godY = v.y / v.w * 0.5F + 0.5F;
		float offscreen = Math.max(Math.abs(godX - 0.5F), Math.abs(godY - 0.5F));
		float fade = MathHelper.clamp(1.0F - (offscreen - 0.5F) * 2.0F, 0.0F, 1.0F);
		godStrength = Math.min(1.0F, ClientFx.lightStrength) * fade;
	}

	private PostFx() {
	}
}
