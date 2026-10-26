package net.minecraft.client.render;

/** PREVIEW-ONLY stand-in for Minecraft's RenderLayer (see the preview RenderPhase): identity keys and a builder. */
public abstract class RenderLayer extends RenderPhase {
	public RenderLayer(String name) {
		super(name, null, null);
	}

	public int getExpectedBufferSize() {
		return 256;
	}

	public static MultiPhase of(String name, VertexFormat format, VertexFormat.DrawMode mode, int size, boolean crumbling, boolean translucent,
			MultiPhaseParameters params) {
		return new MultiPhase(name);
	}

	public static final class MultiPhase extends RenderLayer {
		MultiPhase(String name) {
			super(name);
		}
	}

	public static final class MultiPhaseParameters {
		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			public Builder program(RenderPhase.ShaderProgram p) {
				return this;
			}

			public Builder texture(RenderPhase.TextureBase t) {
				return this;
			}

			public Builder transparency(RenderPhase.Transparency t) {
				return this;
			}

			public Builder writeMaskState(RenderPhase.WriteMaskState w) {
				return this;
			}

			public Builder cull(RenderPhase.Cull c) {
				return this;
			}

			public Builder depthTest(RenderPhase.DepthTest d) {
				return this;
			}

			public Builder lightmap(RenderPhase.Lightmap l) {
				return this;
			}

			public Builder overlay(RenderPhase.Overlay o) {
				return this;
			}

			public MultiPhaseParameters build(boolean outline) {
				return new MultiPhaseParameters();
			}
		}
	}
}
