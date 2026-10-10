package com.armageddon.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/** Custom render layers for energy effects (additive glow) and the black hole core. */
public final class FxRenderLayers {
	/** Additive, unlit, double-sided, no depth write: lasers, plasma, fireballs, shockwaves. */
	public static final RenderLayer GLOW = RenderLayer.of("armageddon_glow", VertexFormats.POSITION_COLOR,
			VertexFormat.DrawMode.QUADS, 8192, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.LIGHTNING_PROGRAM)
					.transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Additive glow that ignores depth — used for the nuclear fireball core so it reads through its own smoke. */
	public static final RenderLayer GLOW_NO_DEPTH = RenderLayer.of("armageddon_glow_nodepth", VertexFormats.POSITION_COLOR,
			VertexFormat.DrawMode.QUADS, 4096, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.LIGHTNING_PROGRAM)
					.transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Alpha-blended (not additive) colour — darkness, smoke shells. */
	public static final RenderLayer SHADE = RenderLayer.of("armageddon_shade", VertexFormats.POSITION_COLOR,
			VertexFormat.DrawMode.QUADS, 4096, false, true,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.COLOR_PROGRAM)
					.transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderPhase.DISABLE_CULLING)
					.build(false));

	/** Opaque unlit colour — the event horizon. */
	public static final RenderLayer VOID = RenderLayer.of("armageddon_void", VertexFormats.POSITION_COLOR,
			VertexFormat.DrawMode.QUADS, 4096, false, false,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderPhase.COLOR_PROGRAM)
					.transparency(RenderPhase.NO_TRANSPARENCY)
					.writeMaskState(RenderPhase.ALL_MASK)
					.cull(RenderPhase.ENABLE_CULLING)
					.build(false));

	private FxRenderLayers() {
	}
}
