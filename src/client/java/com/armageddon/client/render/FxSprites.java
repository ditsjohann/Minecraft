package com.armageddon.client.render;

import com.armageddon.Armageddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Textured camera-facing sprites from the procedural FX atlas ({@code textures/fx/atlas.png}): soft smoke puffs,
 * dust, fire, glows, flares, rings and streaks. They use a plain textured program with the world's light baked into
 * the vertex colour, which shader packs (Iris) handle cleanly, instead of flat polygon fans that facet and shade oddly.
 */
public final class FxSprites {
	public static final Identifier ATLAS = Armageddon.id("textures/fx/atlas.png");
	public static final int SMOKE = 0, DUST = 6, FIRE = 8, GLOW = 10, FLARE = 11, RING = 12, STREAK = 13, SHOCK = 14, WISP = 15;
	public static final int FULL_BRIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

	/** Textured colour without a cut-off: soft edges fade all the way out (the particle shader discards below 10%). */
	private static final RenderPhase.ShaderProgram PARTICLE = new RenderPhase.ShaderProgram(net.minecraft.client.render.GameRenderer::getPositionTexColorProgram);

	/** Alpha-blended, lit by the world: smoke, dust, ash. */
	public static final RenderLayer SMOKE_LAYER = RenderLayer.of("armageddon_fx_smoke", VertexFormats.POSITION_TEXTURE_COLOR,
			VertexFormat.DrawMode.QUADS, 65536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(PARTICLE)
					.texture(new RenderPhase.Texture(ATLAS, true, false))
					.transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Additive and emissive: fire, glows, flares, sparks, shock rings. */
	public static final RenderLayer GLOW_LAYER = RenderLayer.of("armageddon_fx_glow", VertexFormats.POSITION_TEXTURE_COLOR,
			VertexFormat.DrawMode.QUADS, 65536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(PARTICLE)
					.texture(new RenderPhase.Texture(ATLAS, true, false))
					.transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Additive and emissive, drawn through everything (cores that must read through their own smoke). */
	public static final RenderLayer GLOW_TOP_LAYER = RenderLayer.of("armageddon_fx_glow_top", VertexFormats.POSITION_TEXTURE_COLOR,
			VertexFormat.DrawMode.QUADS, 16384, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(PARTICLE)
					.texture(new RenderPhase.Texture(ATLAS, true, false))
					.transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	private FxSprites() {
	}

	/**
	 * One sprite: atlas {@code cell}, centred on camera-relative {@code c}, spanning {@code half} along the camera
	 * basis {@code cb} (or any plane), turned by {@code rot} radians. The top edge is tinted {@code top}, the bottom
	 * {@code bottom}, so a puff can be sunlit above and shadowed below.
	 */
	public static void sprite(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d right, Vec3d up, double half, double rot, int cell,
			int top, int bottom, float alpha, int light) {
		if (cell >= FIRE) {
			alpha *= glare(c, half);
		}
		if (alpha <= 0.003F || half <= 0.0) {
			return;
		}
		double cs = Math.cos(rot) * half, sn = Math.sin(rot) * half;
		Vec3d r = right.multiply(cs).add(up.multiply(sn));
		Vec3d u = up.multiply(cs).subtract(right.multiply(sn));
		float u0 = (cell % 4) * 0.25F + 0.002F, v0 = (cell / 4) * 0.25F + 0.002F;
		float u1 = u0 + 0.246F, v1 = v0 + 0.246F;
		int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
		put(vc, m, c.subtract(r).subtract(u), u0, v1, bottom, a, light);
		put(vc, m, c.add(r).subtract(u), u1, v1, bottom, a, light);
		put(vc, m, c.add(r).add(u), u1, v0, top, a, light);
		put(vc, m, c.subtract(r).add(u), u0, v0, top, a, light);
	}

	/** A camera-facing sprite with one colour. */
	/**
	 * Glare limiter for additive light: a glow that fills the view (a flare hundreds of blocks wide, a fireball the
	 * camera is inside) fades the more of the screen it covers, so a blast never turns everything into flat white.
	 * {@code c} is camera-relative, so its length is the distance to the eye.
	 */
	static float glare(Vec3d c, double half) {
		double cover = half / Math.max(1.0, c.length());
		return (float) MathHelper.clamp(1.25 - 0.75 * cover, 0.16, 1.0);
	}

	public static void billboard(VertexConsumer vc, Matrix4f m, Vec3d c, Vec3d[] cb, double half, double rot, int cell, int rgb, float alpha, int light) {
		sprite(vc, m, c, cb[0], cb[1], half, rot, cell, rgb, rgb, alpha, light);
	}

	/** A sprite stretched between two points (streaks, sparks, trails), facing the camera. */
	public static void streak(VertexConsumer vc, Matrix4f m, Vec3d a, Vec3d b, double width, int cell, int rgb, float alpha, int light) {
		Vec3d d = b.subtract(a);
		Vec3d mid = a.add(b).multiply(0.5);
		Vec3d side = d.crossProduct(mid);
		if (side.lengthSquared() < 1.0E-8 || alpha <= 0.003F) {
			return;
		}
		side = side.normalize().multiply(width * 0.5);
		float u0 = (cell % 4) * 0.25F + 0.002F, v0 = (cell / 4) * 0.25F + 0.002F;
		float u1 = u0 + 0.246F, v1 = v0 + 0.246F;
		int al = MathHelper.clamp((int) (alpha * 255), 0, 255);
		put(vc, m, a.subtract(side), u0, v1, rgb, al, light);
		put(vc, m, b.subtract(side), u1, v1, rgb, al, light);
		put(vc, m, b.add(side), u1, v0, rgb, al, light);
		put(vc, m, a.add(side), u0, v0, rgb, al, light);
	}

	private static void put(VertexConsumer vc, Matrix4f m, Vec3d p, float u, float v, int rgb, int a, int light) {
		float k = light == FULL_BRIGHT ? 1.0F : brightness(light);
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).texture(u, v)
				.color((int) (((rgb >> 16) & 0xFF) * k), (int) (((rgb >> 8) & 0xFF) * k), (int) ((rgb & 0xFF) * k), a);
	}

	/** How bright a packed light value looks right now: block light, or sky light dimmed by the time of day. */
	public static float brightness(int packed) {
		ClientWorld w = com.armageddon.client.render.FxDraw.world();
		float day = w == null ? 1.0F : 1.0F - w.getAmbientDarkness() / 11.0F * 0.88F;
		float block = LightmapTextureManager.getBlockLightCoordinates(packed) / 15.0F;
		float sky = LightmapTextureManager.getSkyLightCoordinates(packed) / 15.0F * day;
		float b = Math.max(block, sky);
		return 0.1F + 0.9F * b * b * (3 - 2 * b);
	}

	/**
	 * World light at a camera-relative point, so smoke darkens at night and in shadow. {@code glow} 0..1 adds block
	 * light for smoke lit from inside by fire.
	 */
	public static int light(Vec3d rel, float glow) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientWorld w = mc == null ? null : mc.world;
		if (w == null) {
			return FULL_BRIGHT;
		}
		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		BlockPos p = BlockPos.ofFloored(rel.x + cam.x, rel.y + cam.y, rel.z + cam.z);
		int packed = w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) ? WorldRenderer.getLightmapCoordinates(w, p) : FULL_BRIGHT;
		int block = Math.max(LightmapTextureManager.getBlockLightCoordinates(packed), (int) (glow * 15));
		int sky = Math.max(LightmapTextureManager.getSkyLightCoordinates(packed), 4);
		return LightmapTextureManager.pack(block, sky);
	}

	/** Stable pseudo-random 0..1 per index, for picking cells and spins. */
	public static double hash(int a, int b) {
		long h = a * 374761393L + b * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}
}
