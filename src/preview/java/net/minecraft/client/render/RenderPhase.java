package net.minecraft.client.render;

import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * PREVIEW-ONLY stand-in for Minecraft's RenderPhase: the offline effect preview runs outside the game, where the
 * real class's static setup pulls in the block registry. Only the shapes the effect code links against exist here.
 */
public abstract class RenderPhase {
	protected final String name;

	public RenderPhase(String name, Runnable beginAction, Runnable endAction) {
		this.name = name;
	}

	public static class ShaderProgram extends RenderPhase {
		public ShaderProgram(Supplier<net.minecraft.client.gl.ShaderProgram> supplier) {
			super("program", null, null);
		}

		public ShaderProgram() {
			super("program", null, null);
		}
	}

	public static class Transparency extends RenderPhase {
		public Transparency(String name) {
			super(name, null, null);
		}
	}

	public static class WriteMaskState extends RenderPhase {
		public WriteMaskState() {
			super("mask", null, null);
		}
	}

	public static class Cull extends RenderPhase {
		public Cull() {
			super("cull", null, null);
		}
	}

	public static class DepthTest extends RenderPhase {
		public DepthTest() {
			super("depth", null, null);
		}
	}

	public static class Lightmap extends RenderPhase {
		public Lightmap() {
			super("lightmap", null, null);
		}
	}

	public static class Overlay extends RenderPhase {
		public Overlay() {
			super("overlay", null, null);
		}
	}

	public static class TextureBase extends RenderPhase {
		public TextureBase() {
			super("texture", null, null);
		}
	}

	public static class Texture extends TextureBase {
		public Texture(Identifier id, boolean blur, boolean mipmap) {
		}
	}

	public static final ShaderProgram LIGHTNING_PROGRAM = new ShaderProgram(), COLOR_PROGRAM = new ShaderProgram(),
			ENTITY_SOLID_PROGRAM = new ShaderProgram(), ENTITY_TRANSLUCENT_PROGRAM = new ShaderProgram(), EYES_PROGRAM = new ShaderProgram();
	public static final Transparency NO_TRANSPARENCY = new Transparency("none"), ADDITIVE_TRANSPARENCY = new Transparency("add"),
			LIGHTNING_TRANSPARENCY = new Transparency("lightning"), TRANSLUCENT_TRANSPARENCY = new Transparency("translucent");
	public static final WriteMaskState COLOR_MASK = new WriteMaskState(), ALL_MASK = new WriteMaskState();
	public static final Cull DISABLE_CULLING = new Cull(), ENABLE_CULLING = new Cull();
	public static final DepthTest ALWAYS_DEPTH_TEST = new DepthTest();
	public static final Lightmap ENABLE_LIGHTMAP = new Lightmap();
	public static final Overlay ENABLE_OVERLAY_COLOR = new Overlay();
}
