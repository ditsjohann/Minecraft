package com.armageddon.client.vfx;

import com.armageddon.Armageddon;
import com.armageddon.client.render.FxSprites;
import com.armageddon.network.VfxPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client particle engine: effect definitions by name, the effects playing now, a shared wind, and the particle
 * budget that keeps busy scenes fast by thinning detail first. Ticked with the client, drawn with the other FX.
 */
public final class FxEngine {
	private static final Map<String, EffectDef> DEFS = new HashMap<>();
	private static final Map<String, EffectDef[]> VARIANTS = new HashMap<>();
	private static final List<EffectInstance> ACTIVE = new ArrayList<>();
	private static EffectInstance[] drawOrder = new EffectInstance[16];
	private static double[] drawDist = new double[16];

	/** Total live particles the engine aims to stay under. */
	public static int maxParticles = 24000;
	/** Shared wind, blocks per tick; drifts slowly. */
	public static float windX = 0.03F, windZ = 0.012F;
	private static int live;
	private static int clock;

	private FxEngine() {
	}

	public static void register(EffectDef def) {
		DEFS.put(def.name, def);
	}

	/**
	 * Registers looks that share one name: each play picks one from its seed, so the same effect does not repeat
	 * itself. Each is also reachable as {@code name#index}.
	 */
	public static void registerVariants(String name, EffectDef... defs) {
		VARIANTS.put(name, defs);
		for (EffectDef d : defs) {
			register(d);
		}
	}

	public static EffectDef get(String name) {
		return resolve(name, 0);
	}

	static EffectDef resolve(String name, long seed) {
		EffectDef[] v = VARIANTS.get(name);
		if (v != null) {
			return v[(int) Math.floorMod(seed ^ (seed >>> 29), (long) v.length)];
		}
		return DEFS.get(name);
	}

	public static boolean active() {
		return !ACTIVE.isEmpty();
	}

	public static int liveParticles() {
		return live;
	}

	public static void handle(VfxPayload p) {
		play(p.effect(), p.x(), p.y(), p.z(), p.dx(), p.dy(), p.dz(), p.scale(), p.color(), p.seed());
	}

	/** Starts an effect on this client. Unknown names are ignored (a newer server talking to an older client). */
	public static EffectInstance play(String name, double x, double y, double z, float dx, float dy, float dz, float scale, int color, long seed) {
		EffectDef def = resolve(name, seed);
		if (def == null) {
			Armageddon.LOGGER.debug("Unknown particle effect {}", name);
			return null;
		}
		EffectInstance fx = create(def, x, y, z, dx, dy, dz, scale, color, seed, cameraDistance(x, y, z));
		ACTIVE.add(fx);
		return fx;
	}

	/**
	 * Starts an effect that rides on something moving at {@code (vx, vy, vz)} blocks per tick: emitters with
	 * {@link EmitterDef.Builder#inherit} carry that much of the motion, so flames keep up with a flying head.
	 */
	public static EffectInstance playMoving(String name, double x, double y, double z, float dx, float dy, float dz, float scale, int color,
			long seed, float vx, float vy, float vz) {
		EffectInstance fx = play(name, x, y, z, dx, dy, dz, scale, color, seed);
		if (fx != null) {
			fx.cvx = vx;
			fx.cvy = vy;
			fx.cvz = vz;
		}
		return fx;
	}

	/** Builds an instance without starting it (the offline preview drives its own). */
	public static EffectInstance create(EffectDef def, double x, double y, double z, float dx, float dy, float dz, float scale, int color,
			long seed, double camDist) {
		return new EffectInstance(def, x, y, z, dx, dy, dz, scale, color, seed, camDist);
	}

	private static double cameraDistance(double x, double y, double z) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.gameRenderer == null) {
			return 0;
		}
		Vec3d c = mc.gameRenderer.getCamera().getPos();
		return Math.sqrt(c.squaredDistanceTo(x, y, z));
	}

	/** How much of an emitter's spawning survives the current load. Hero layers are thinned last. */
	static float budget(EmitterDef.Priority p) {
		float use = live / (float) Math.max(1, maxParticles);
		return switch (p) {
			case HERO -> use < 1.25F ? 1.0F : 0.0F;
			case NORMAL -> use < 0.8F ? 1.0F : use < 1.0F ? 1.0F - (use - 0.8F) / 0.2F * 0.7F : 0.0F;
			case DETAIL -> use < 0.55F ? 1.0F : (float) MathHelper.clamp(1.0 - (use - 0.55) / 0.3, 0.0, 1.0);
		};
	}

	public static void tick(MinecraftClient mc) {
		ClientWorld world = mc.world;
		if (world == null) {
			if (!ACTIVE.isEmpty()) {
				clear();
			}
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		clock++;
		// A slow, gusting wind that every smoke layer shares.
		windX = 0.03F + 0.015F * MathHelper.sin(clock * 0.0021F) + 0.006F * MathHelper.sin(clock * 0.031F);
		windZ = 0.012F + 0.012F * MathHelper.cos(clock * 0.0017F);
		step(world);
	}

	/** Advances every playing effect one tick ({@code world} may be null: the offline preview). */
	public static void step(ClientWorld world) {
		int n = 0;
		for (int i = 0; i < ACTIVE.size(); ) {
			EffectInstance fx = ACTIVE.get(i);
			boolean alive;
			try {
				alive = fx.tick(world);
			} catch (RuntimeException ex) {
				Armageddon.LOGGER.error("Particle effect {} failed while ticking; removing it", fx.def.name, ex);
				alive = false;
			}
			if (alive) {
				n += fx.particles();
				i++;
			} else {
				ACTIVE.set(i, ACTIVE.get(ACTIVE.size() - 1));
				ACTIVE.remove(ACTIVE.size() - 1);
			}
		}
		live = n;
	}

	/** Draws every playing effect (far ones first, so their smoke sits behind nearer smoke). */
	public static void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
		int n = ACTIVE.size();
		if (n == 0) {
			return;
		}
		if (drawOrder.length < n) {
			drawOrder = new EffectInstance[n * 2];
			drawDist = new double[n * 2];
		}
		for (int i = 0; i < n; i++) {
			EffectInstance fx = ACTIVE.get(i);
			drawOrder[i] = fx;
			drawDist[i] = cam.squaredDistanceTo(fx.ox, fx.oy, fx.oz);
		}
		// Insertion sort, far to near: there are only ever a handful of effects.
		for (int i = 1; i < n; i++) {
			EffectInstance fx = drawOrder[i];
			double d = drawDist[i];
			int j = i - 1;
			while (j >= 0 && drawDist[j] < d) {
				drawOrder[j + 1] = drawOrder[j];
				drawDist[j + 1] = drawDist[j];
				j--;
			}
			drawOrder[j + 1] = fx;
			drawDist[j + 1] = d;
		}
		VfxRender.basis(yaw, pitch);
		for (int i = 0; i < n; i++) {
			renderInstance(drawOrder[i], m, consumers, cam, tickDelta);
			drawOrder[i] = null;
		}
	}

	/** Draws one instance; the camera basis must be set ({@link #render} or {@link #renderAlone}). */
	static void renderInstance(EffectInstance fx, Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta) {
		for (EmitterInstance em : fx.emitters) {
			try {
				VfxRender.draw(em, consumers.getBuffer(layer(em.def.layer)), m, cam.x, cam.y, cam.z, tickDelta);
			} catch (RuntimeException ex) {
				Armageddon.LOGGER.error("Particle emitter {} of {} failed while rendering", em.def.name, fx.def.name, ex);
			}
		}
	}

	/** Draws a single instance outside the engine (the offline preview). */
	public static void renderAlone(EffectInstance fx, Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
		VfxRender.basis(yaw, pitch);
		renderInstance(fx, m, consumers, cam, tickDelta);
	}

	/** Advances an instance outside the engine (the offline preview, with no world). */
	public static boolean tickAlone(EffectInstance fx, ClientWorld world) {
		return fx.tick(world);
	}

	private static net.minecraft.client.render.RenderLayer layer(EmitterDef.Layer l) {
		return switch (l) {
			case GLOW -> FxSprites.GLOW_LAYER;
			case GLOW_TOP -> FxSprites.GLOW_TOP_LAYER;
			case SMOKE -> FxSprites.SMOKE_LAYER;
		};
	}

	public static void clear() {
		ACTIVE.clear();
		live = 0;
	}

	/** One line for the debug command: effects, particles, budget use. */
	public static String stats() {
		return ACTIVE.size() + " effects, " + live + " / " + maxParticles + " particles";
	}
}
