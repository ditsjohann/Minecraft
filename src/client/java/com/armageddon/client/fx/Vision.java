package com.armageddon.client.fx;

import com.armageddon.Armageddon;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.JsonEffectShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The depth-aware "vision" pass ({@code shaders/program/armageddon_vision}): drawn over the finished world, before
 * the hand, by copying the frame's colour and depth and letting the shader rebuild where every pixel sits in the
 * world. That is what lets the new bombs see the world as a lidar point cloud, swallow it in a volumetric void,
 * plunge it into an eclipse, ping it with sonar, or redraw it as pixels, comic ink or an insect's eye.
 * The strongest live {@link VisionFx} drives it; if the shader cannot load, the bombs simply play without it.
 */
public final class Vision {
	public static final int LIDAR = 1, VOID = 2, ECLIPSE = 3, SONAR = 4, PIXEL = 5, COMIC = 6, COMPOUND = 7;
	private static final String PROGRAM = "armageddon_vision";

	private static JsonEffectShaderProgram program;
	private static boolean failed;
	private static SimpleFramebuffer copy;
	private static float time;

	public static void invalidate() {
		if (program != null) {
			program.close();
			program = null;
		}
		failed = false;
	}

	/** The strongest vision effect this frame, or null. */
	private static VisionFx strongest(float tickDelta) {
		VisionFx best = null;
		float s = 0.002F;
		for (FxEffect e : ClientFx.EFFECTS) {
			if (e instanceof VisionFx v && !v.replaced) {
				float k = v.strength(tickDelta);
				if (k > s) {
					s = k;
					best = v;
				}
			}
		}
		return best;
	}

	/** Called from {@code WorldRenderEvents.LAST}: the world is finished and the depth buffer still holds it. */
	public static void render(WorldRenderContext ctx) {
		float tickDelta = ctx.tickCounter().getTickDelta(false);
		VisionFx v = strongest(tickDelta);
		if (v == null) {
			return;
		}
		MinecraftClient mc = MinecraftClient.getInstance();
		if (program == null && !failed) {
			try {
				program = new JsonEffectShaderProgram(mc.getResourceManager(), PROGRAM);
			} catch (Exception e) {
				failed = true;
				Armageddon.LOGGER.warn("Armageddon vision shader unavailable; the vision effects will be skipped", e);
			}
		}
		if (program == null) {
			return;
		}
		Framebuffer main = mc.getFramebuffer();
		int w = main.textureWidth, h = main.textureHeight;
		if (copy == null || copy.textureWidth != w || copy.textureHeight != h) {
			if (copy != null) {
				copy.delete();
			}
			copy = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
		}
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.fbo);
		GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
		main.beginWrite(true);

		time += mc.getRenderTickCounter().getLastFrameDuration();
		Vec3d cam = ctx.camera().getPos();
		Matrix4f proj = new Matrix4f(ctx.projectionMatrix());
		Matrix4f inv = new Matrix4f(proj).mul(ctx.positionMatrix()).invert();
		Vec3d o = v.origin.subtract(cam);
		float t = v.age + tickDelta;

		program.bindSampler("DiffuseSampler", copy::getColorAttachment);
		program.bindSampler("DepthSampler", copy::getDepthAttachment);
		program.getUniformByNameOrDummy("ProjMat").set(new Matrix4f().setOrtho(0.0F, w, 0.0F, h, 0.1F, 1000.0F));
		program.getUniformByNameOrDummy("InSize").set((float) w, (float) h);
		program.getUniformByNameOrDummy("OutSize").set((float) w, (float) h);
		program.getUniformByNameOrDummy("InvViewProj").set(inv);
		program.getUniformByNameOrDummy("Targets").set(targets(mc, v, cam, tickDelta));
		program.getUniformByNameOrDummy("Origin").set((float) o.x, (float) o.y, (float) o.z);
		// The camera's position folded into a 512-block window: the lidar's world-locked point lattice stays put
		// (512 is a multiple of its spacing) without float precision trouble far from spawn.
		program.getUniformByNameOrDummy("CamFrac").set((float) (cam.x - Math.floor(cam.x / 512) * 512),
				(float) (cam.y - Math.floor(cam.y / 512) * 512), (float) (cam.z - Math.floor(cam.z / 512) * 512));
		program.getUniformByNameOrDummy("Color").set(((v.color >> 16) & 0xFF) / 255.0F, ((v.color >> 8) & 0xFF) / 255.0F, (v.color & 0xFF) / 255.0F);
		program.getUniformByNameOrDummy("Mode").set((float) v.mode);
		program.getUniformByNameOrDummy("Strength").set(v.strength(tickDelta));
		program.getUniformByNameOrDummy("Time").set(time / 20.0F);
		program.getUniformByNameOrDummy("Radius").set(v.radius(t));
		program.getUniformByNameOrDummy("Reveal").set(v.reveal(t));
		program.getUniformByNameOrDummy("Param").set(v.param(t));
		program.getUniformByNameOrDummy("P11").set(proj.m11());

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		program.enable();
		BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		bb.vertex(0.0F, 0.0F, 500.0F);
		bb.vertex(w, 0.0F, 500.0F);
		bb.vertex(w, h, 500.0F);
		bb.vertex(0.0F, h, 500.0F);
		BufferRenderer.draw(bb.end());
		program.disable();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		main.beginWrite(true);
	}

	/** Up to four living things near the source, camera-relative, packed into the columns of a matrix (w = 1 if used). */
	private static Matrix4f targets(MinecraftClient mc, VisionFx v, Vec3d cam, float tickDelta) {
		Matrix4f m = new Matrix4f().zero();
		if (mc.world == null || (v.mode != LIDAR && v.mode != SONAR)) {
			return m;
		}
		List<LivingEntity> found = new ArrayList<>();
		for (Entity e : mc.world.getEntities()) {
			if (e instanceof LivingEntity le && e != mc.getCameraEntity() && le.isAlive() && e.getPos().squaredDistanceTo(v.origin) < v.maxR * v.maxR) {
				found.add(le);
			}
		}
		found.sort(Comparator.comparingDouble(e -> e.getPos().squaredDistanceTo(cam)));
		for (int i = 0; i < Math.min(4, found.size()); i++) {
			LivingEntity e = found.get(i);
			Vec3d p = e.getLerpedPos(tickDelta).add(0, e.getHeight() * 0.5, 0).subtract(cam);
			m.setColumn(i, new Vector4f((float) p.x, (float) p.y, (float) p.z, 1.0F));
		}
		return m;
	}

	private Vision() {
	}
}
