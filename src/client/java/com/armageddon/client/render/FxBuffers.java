package com.armageddon.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The effect renderer's own vertex buffers: every effect layer gets a dedicated allocator, so effects can freely
 * interleave glow, smoke and solid geometry without one layer flushing another mid-draw, and the layers are drawn
 * in a fixed, sensible order.
 *
 * <p>With an Iris shader pack in use, the flat-colour layers (solid bodies, shade, glow) are redirected to entity
 * programs with real normals, which packs light, fog and composite properly; the colour-only programs they use
 * otherwise come out faceted or wrongly shaded under most packs.
 */
public final class FxBuffers implements VertexConsumerProvider {
	private static final Identifier WHITE = Identifier.of("armageddon", "textures/fx/white.png");

	static final RenderLayer SOLID_LIT = RenderLayer.of("armageddon_solid_lit", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
			VertexFormat.DrawMode.QUADS, 65536, true, false,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.ENTITY_SOLID_PROGRAM)
					.texture(new RenderPhase.Texture(WHITE, false, false))
					.transparency(RenderPhase.NO_TRANSPARENCY)
					.cull(RenderPhase.DISABLE_CULLING)
					.lightmap(RenderPhase.ENABLE_LIGHTMAP)
					.overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
					.build(false));

	static final RenderLayer SHADE_LIT = RenderLayer.of("armageddon_shade_lit", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
			VertexFormat.DrawMode.QUADS, 65536, true, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.ENTITY_TRANSLUCENT_PROGRAM)
					.texture(new RenderPhase.Texture(WHITE, false, false))
					.transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
					.cull(RenderPhase.DISABLE_CULLING)
					.lightmap(RenderPhase.ENABLE_LIGHTMAP)
					.overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.build(false));

	static final RenderLayer GLOW_LIT = RenderLayer.of("armageddon_glow_lit", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
			VertexFormat.DrawMode.QUADS, 65536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.EYES_PROGRAM)
					.texture(new RenderPhase.Texture(WHITE, false, false))
					.transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	static final RenderLayer GLOW_TOP_LIT = RenderLayer.of("armageddon_glow_top_lit", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
			VertexFormat.DrawMode.QUADS, 16384, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.EYES_PROGRAM)
					.texture(new RenderPhase.Texture(WHITE, false, false))
					.transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Draw order: opaque bodies first, then smoke, then everything additive. */
	private static final List<RenderLayer> ORDER = List.of(FxRenderLayers.SOLID, SOLID_LIT, FxRenderLayers.VOID, FxRenderLayers.SHADE, SHADE_LIT,
			FxSprites.SMOKE_LAYER, FxSprites.BLEND_LAYER, FxRenderLayers.GLOW, GLOW_LIT, FxSprites.GLOW_LAYER, FxRenderLayers.GLOW_NO_DEPTH, GLOW_TOP_LIT,
			FxSprites.GLOW_TOP_LAYER, FxSprites.BLEND_TOP_LAYER);

	private static FxBuffers current;
	private static Method irisInstance, irisInUse;
	private static boolean irisChecked;

	private final VertexConsumerProvider.Immediate immediate;
	private final Map<VertexConsumer, RenderLayer> owners = new IdentityHashMap<>();
	private boolean shaders, blend;
	private Premultiplied smoke, glow, glowTop;

	private FxBuffers() {
		LinkedHashMap<RenderLayer, BufferAllocator> fixed = new LinkedHashMap<>();
		for (RenderLayer l : ORDER) {
			fixed.put(l, new BufferAllocator(l.getExpectedBufferSize()));
		}
		immediate = VertexConsumerProvider.immediate(fixed, new BufferAllocator(262144));
	}

	/** The provider for this frame (call {@link #finish()} after the effects have drawn). */
	public static FxBuffers begin() {
		if (current == null) {
			current = new FxBuffers();
		}
		current.owners.clear();
		current.shaders = shaderPack();
		// One sorted premultiplied pass for smoke and glow, unless a shader pack is in charge (it wants vanilla programs).
		current.blend = !current.shaders && FxSprites.blendReady();
		current.smoke = current.glow = current.glowTop = null;
		return current;
	}

	@Nullable
	public static VertexConsumerProvider current() {
		return override != null ? override : current;
	}

	/** Which effect layer a consumer handed out this frame draws into (null if it is not one of ours). */
	@Nullable
	public static RenderLayer layerOf(VertexConsumer vc) {
		if (override != null) {
			return overrideOwner.apply(vc);
		}
		return current == null ? null : current.owners.get(vc);
	}

	private static VertexConsumerProvider override;
	private static java.util.function.Function<VertexConsumer, RenderLayer> overrideOwner;

	/** For the offline preview renderer: route effects into its own provider instead of the game's buffers. */
	public static void install(VertexConsumerProvider provider, java.util.function.Function<VertexConsumer, RenderLayer> owner) {
		override = provider;
		overrideOwner = owner;
	}

	public boolean shaderPackActive() {
		return shaders;
	}

	@Override
	public VertexConsumer getBuffer(RenderLayer layer) {
		VertexConsumer out;
		if (shaders && layer == FxRenderLayers.SOLID || shaders && layer == FxRenderLayers.VOID) {
			out = new LitQuads(immediate.getBuffer(SOLID_LIT), false, false);
		} else if (shaders && layer == FxRenderLayers.SHADE) {
			out = new LitQuads(immediate.getBuffer(SHADE_LIT), false, true);
		} else if (shaders && layer == FxRenderLayers.GLOW) {
			out = new LitQuads(immediate.getBuffer(GLOW_LIT), true, false);
		} else if (shaders && layer == FxRenderLayers.GLOW_NO_DEPTH) {
			out = new LitQuads(immediate.getBuffer(GLOW_TOP_LIT), true, false);
		} else if (blend && layer == FxSprites.SMOKE_LAYER) {
			if (smoke == null) {
				smoke = new Premultiplied(immediate.getBuffer(FxSprites.BLEND_LAYER), false);
			}
			out = smoke;
		} else if (blend && layer == FxSprites.GLOW_LAYER) {
			if (glow == null) {
				glow = new Premultiplied(immediate.getBuffer(FxSprites.BLEND_LAYER), true);
			}
			out = glow;
		} else if (blend && layer == FxSprites.GLOW_TOP_LAYER) {
			if (glowTop == null) {
				glowTop = new Premultiplied(immediate.getBuffer(FxSprites.BLEND_TOP_LAYER), true);
			}
			out = glowTop;
		} else {
			out = immediate.getBuffer(layer);
		}
		owners.put(out, layer);
		return out;
	}

	public void finish() {
		for (RenderLayer l : ORDER) {
			immediate.draw(l);
		}
		immediate.draw();
		owners.clear();
	}

	/**
	 * Writes sprite quads into {@link FxSprites#BLEND_LAYER} with premultiplied colour: smoke keeps its alpha (it
	 * covers what is behind it), glow gets alpha 0 (it only adds light). Both end up in one buffer that the game sorts
	 * back to front, so they layer correctly against each other.
	 */
	static final class Premultiplied implements VertexConsumer {
		private final VertexConsumer out;
		private final boolean additive;

		Premultiplied(VertexConsumer out, boolean additive) {
			this.out = out;
			this.additive = additive;
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			out.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			out.color(red * alpha / 255, green * alpha / 255, blue * alpha / 255, additive ? 0 : alpha);
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			out.texture(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}
	}

	/** True if an Iris shader pack is rendering (looked up reflectively, so Iris stays optional). */
	public static boolean shaderPack() {
		if (!irisChecked) {
			irisChecked = true;
			if (FabricLoader.getInstance().isModLoaded("iris")) {
				try {
					Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
					irisInstance = api.getMethod("getInstance");
					irisInUse = api.getMethod("isShaderPackInUse");
				} catch (ReflectiveOperationException | LinkageError e) {
					irisInstance = null;
				}
			}
		}
		if (irisInstance == null) {
			return false;
		}
		try {
			return (Boolean) irisInUse.invoke(irisInstance.invoke(null));
		} catch (ReflectiveOperationException | RuntimeException e) {
			return false;
		}
	}

	/**
	 * Turns position+colour quads into entity vertices: a face normal (turned to face the camera, so double-sided
	 * geometry is lit on the side you see), a white texel, the overlay and a light value. Additive glow is
	 * premultiplied by its alpha, because the emissive program blends one-to-one.
	 */
	static final class LitQuads implements VertexConsumer {
		private final VertexConsumer out;
		private final boolean premultiply, sampleLight;
		private final float[] xyz = new float[12];
		private final int[] rgba = new int[16];
		private int n;

		LitQuads(VertexConsumer out, boolean premultiply, boolean sampleLight) {
			this.out = out;
			this.premultiply = premultiply;
			this.sampleLight = sampleLight;
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			if (n == 4) {
				n = 0;
			}
			xyz[n * 3] = x;
			xyz[n * 3 + 1] = y;
			xyz[n * 3 + 2] = z;
			rgba[n * 4] = 255;
			rgba[n * 4 + 1] = 255;
			rgba[n * 4 + 2] = 255;
			rgba[n * 4 + 3] = 255;
			n++;
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			int i = (n - 1) * 4;
			if (i < 0) {
				return this;
			}
			rgba[i] = red;
			rgba[i + 1] = green;
			rgba[i + 2] = blue;
			rgba[i + 3] = alpha;
			if (n == 4) {
				flush();
			}
			return this;
		}

		private void flush() {
			float ax = xyz[6] - xyz[0], ay = xyz[7] - xyz[1], az = xyz[8] - xyz[2];
			float bx = xyz[9] - xyz[3], by = xyz[10] - xyz[4], bz = xyz[11] - xyz[5];
			float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			float cx = (xyz[0] + xyz[3] + xyz[6] + xyz[9]) * 0.25F, cy = (xyz[1] + xyz[4] + xyz[7] + xyz[10]) * 0.25F, cz = (xyz[2] + xyz[5] + xyz[8] + xyz[11]) * 0.25F;
			if (len < 1.0E-6F) {
				nx = -cx;
				ny = -cy;
				nz = -cz;
				len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (len < 1.0E-6F) {
					nx = 0;
					ny = 1;
					nz = 0;
					len = 1;
				}
			}
			nx /= len;
			ny /= len;
			nz /= len;
			// The camera is at the origin: face the normal towards it.
			if (nx * cx + ny * cy + nz * cz > 0) {
				nx = -nx;
				ny = -ny;
				nz = -nz;
			}
			int light = sampleLight ? FxSprites.light(new Vec3d(cx, cy, cz), 0.0F) : FxSprites.FULL_BRIGHT;
			for (int k = 0; k < 4; k++) {
				int r = rgba[k * 4], g = rgba[k * 4 + 1], b = rgba[k * 4 + 2], a = rgba[k * 4 + 3];
				if (premultiply) {
					r = r * a / 255;
					g = g * a / 255;
					b = b * a / 255;
					a = 255;
				}
				out.vertex(xyz[k * 3], xyz[k * 3 + 1], xyz[k * 3 + 2]).color(r, g, b, a).texture(0.5F, 0.5F)
						.overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
			}
			n = 0;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}
	}
}
