package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.EternalTree;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/**
 * The Eternal Sapling's aura around the real-block tree: a soft green glow column, life energy spiralling up
 * the bark, pollen and leaves drifting from the canopy, and pulses running out along each root as it lands.
 * Its age runs in step with the server event (both start on the same tick).
 */
public class EternalTreeFx extends FxEffect {
	/** The most recent tree, so its cutscene can frame the real scale and base. */
	static EternalTreeFx latest;

	final Vec3d base;
	final double s;
	private final double[] rootGround = new double[EternalTree.ROOTS];

	public EternalTreeFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.base = new Vec3d(p.x(), p.y(), p.z());
		this.s = MathHelper.clamp(p.scale(), 0.3F, 1.0F);
		java.util.Arrays.fill(rootGround, Double.NaN);
		latest = this;
	}

	/** Scale and base of the tree near {@code anchor}, if its aura arrived. */
	static EternalTreeFx near(Vec3d anchor) {
		EternalTreeFx t = latest;
		return t != null && t.base.squaredDistanceTo(anchor) < 64 * 64 ? t : null;
	}

	/** Visible height of the tree at {@code a} ticks: the sapling, each rung of the ladder, then the world tree's growth front. */
	double height(double a) {
		int k = EternalTree.stageAt(a);
		if (k < 0) {
			return 0.8;
		}
		if (a < EternalTree.WORLD_START) {
			double from = k == 0 ? 1 : EternalTree.stageHeight(k - 1, s);
			double u = Cinematics.clamp01((a - EternalTree.STAGE_START[k]) / (double) (EternalTree.STAGE_END[k] - EternalTree.STAGE_START[k]));
			return from + (EternalTree.stageHeight(k, s) - from) * u;
		}
		return Math.max(EternalTree.stageHeight(EternalTree.STAGES - 1, s), Math.min(EternalTree.height(s), EternalTree.front(s, a)));
	}

	double trunkR(double a, double y) {
		if (a >= EternalTree.WORLD_START) {
			return EternalTree.trunkRadiusAt(s, y);
		}
		int k = EternalTree.stageAt(a);
		return k < 0 ? 0.3 : EternalTree.stageRadius(k, s);
	}

	/** Ground height at root {@code i}'s tip, looked up once the chunk is around. */
	double rootGround(ClientWorld world, int i) {
		if (Double.isNaN(rootGround[i]) && world != null) {
			Vec3d tip = EternalTree.rootTip(base, i, s);
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(tip.x), MathHelper.floor(tip.z));
			rootGround[i] = y > world.getBottomY() + 1 ? y : base.y;
		}
		return Double.isNaN(rootGround[i]) ? base.y : rootGround[i];
	}

	@Override
	public boolean tick(ClientWorld world) {
		Vec3d cam = ClientFx.cameraPos();
		boolean near = cam.squaredDistanceTo(base) < 360 * 360;
		float fade = (float) Cinematics.clamp01((lifetime - age) / 200.0);
		if (near && fade > 0) {
			double h = height(age);
			if (age < EternalTree.WORLD_START) {
				if (age % 3 == 0) {
					world.addImportantParticle(ParticleTypes.HAPPY_VILLAGER, true, base.x + (world.random.nextDouble() - 0.5) * h,
							base.y + world.random.nextDouble() * h, base.z + (world.random.nextDouble() - 0.5) * h, 0, 0.02, 0);
				}
			} else {
				double crown = Math.max(20, h * 0.8);
				double spread = 45 * s;
				int n = (int) (6 * fade);
				for (int i = 0; i < n; i++) {
					double x = base.x + world.random.nextGaussian() * spread * 0.5, z = base.z + world.random.nextGaussian() * spread * 0.5;
					double y = base.y + crown + world.random.nextGaussian() * 10 * s;
					world.addImportantParticle(i % 3 == 0 ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.SPORE_BLOSSOM_AIR, true, x, y, z, 0, -0.02, 0);
				}
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double y = world.random.nextDouble() * h;
				double r = trunkR(age, y) + 0.8;
				world.addImportantParticle(ParticleTypes.END_ROD, true, base.x + Math.cos(ang) * r, base.y + y, base.z + Math.sin(ang) * r, 0, 0.25, 0);
				// Petals drifting down around whoever is standing under the canopy.
				if (h > EternalTree.height(s) * 0.75 && Math.hypot(cam.x - base.x, cam.z - base.z) < 60 * s + 10) {
					for (int i = 0; i < 3; i++) {
						world.addImportantParticle(ParticleTypes.CHERRY_LEAVES, true, cam.x + world.random.nextGaussian() * 10,
								cam.y + 6 + world.random.nextDouble() * 10, cam.z + world.random.nextGaussian() * 10, 0, -0.02, 0);
					}
				}
			}
		}
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			if (age == EternalTree.rootLaunch(i)) {
				rootGround(world, i);
			}
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		float fade = (float) Cinematics.clamp01((lifetime - a) / 200.0) * (float) Cinematics.clamp01(a / 20.0);
		if (fade <= 0.01F) {
			return;
		}
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d b = base.subtract(cam);
		double h = height(a);
		boolean world = a >= EternalTree.WORLD_START;
		double width = world ? EternalTree.trunkRadius(s) * 3.2 : Math.max(1.5, h * 0.5);
		FxDraw.ribbon(glow, m, b, b.add(0, h + 4, 0), Vec3d.ZERO, width, 0x60FF90, 0.12F * fade, 0.0F);
		Solids.dot(glow, m, b.add(0, 0.4, 0), cb, world ? 24 * s : 3 + h * 0.3, 0x80FF90, 0.25F * fade);
		// Life energy climbing the bark.
		int motes = world ? 40 : 10;
		for (int k = 0; k < motes; k++) {
			double y = ((a * (world ? 0.9 : 0.15) + k * h / motes) % Math.max(1, h));
			double ang = k * 2.39996 + y * 0.08 + a * 0.01;
			double r = trunkR(a, y) + 0.5;
			Vec3d p = b.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
			Solids.dot(glow, m, p, cb, world ? 0.9 + s * 0.6 : 0.25, k % 4 == 0 ? 0xFFF0A0 : 0xA0FFB0, 0.85F * fade);
		}
		if (world) {
			double top = Math.min(EternalTree.height(s), EternalTree.front(s, a));
			Solids.dot(glow, m, b.add(0, top + 6 * s, 0), cb, 34 * s, 0xC8FF90, 0.14F * fade);
			// Rings of life pulsing up the trunk like a heartbeat.
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			for (int k = 0; k < 3; k++) {
				double y = ((a * 1.6 + k * top / 3) % Math.max(1, top));
				double r = EternalTree.trunkRadiusAt(s, y) + 0.6;
				float pk = (float) (1 - y / Math.max(1, top));
				FxDraw.ring(glow, m, b.add(0, y, 0), x, z, r, r + 1.6 * s + 0.4, k == 0 ? 0xFFF0A0 : 0x90FFB0, 0.0F, 0.75F * fade * pk, 0.0F, 40);
			}
			double crownY = EternalTree.height(s) * 0.72;
			if (top > crownY) {
				float grown = (float) Cinematics.clamp01((top - crownY) / 30.0) * fade;
				godRays(glow, m, b, a, grown);
				fireflies(glow, m, cb, b, a, grown);
			}
		}
		// Each root, as it lands, sends a pulse of light racing down its length.
		ClientWorld cw = com.armageddon.client.render.FxDraw.world();
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			float u = (a - EternalTree.rootLaunch(i)) / (EternalTree.ROOT_GROW + 14.0F);
			if (u <= 0 || u >= 1) {
				continue;
			}
			double gy = rootGround(cw, i);
			Vec3d p = EternalTree.rootPoint(base, i, s, u, gy).subtract(cam);
			Vec3d q = EternalTree.rootPoint(base, i, s, Math.max(0, u - 0.08), gy).subtract(cam);
			FxDraw.beam(glow, m, q, p, Vec3d.ZERO, 2.2 * s + 0.6, 0xC8FF90, 0.8F * fade);
			Solids.dot(glow, m, p, cb, 5 * s + 1.5, 0xFFFFD0, 0.7F * fade);
		}
		// Each landing raises a rolling wall of dust and splits the ground with light.
		VertexConsumer shade = null, solid = null;
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			float k = a - EternalTree.rootSlam(i);
			if (k <= 0 || k >= 90) {
				continue;
			}
			Vec3d tip = EternalTree.rootTip(base, i, s);
			Vec3d g = new Vec3d(tip.x, rootGround(cw, i) + 0.3, tip.z).subtract(cam);
			if (g.lengthSquared() > 260 * 260) {
				continue;
			}
			if (shade == null) {
				shade = consumers.getBuffer(FxRenderLayers.SHADE);
				solid = consumers.getBuffer(FxRenderLayers.SOLID);
			}
			float kf = 1 - k / 90F;
			Cinematics.dustWall(shade, m, cb, g, 6 + k * 0.9 * (0.6 + s), 5 + k * 0.12, 0x8A7A62, 0.6F * kf * fade, 40 + i);
			Cinematics.groundCracks(glow, m, g, Math.min(28 * s + 8, 4 + k * 1.2), 9, 50 + i, 0xFFC860, 0.85F * kf * fade);
			Cinematics.debris(solid, glow, m, g, k, 18, 1.2, 1.6, 0.06, 60 + i, 0x4A3A28, 0xC8FF90);
		}
	}

	/** Shafts of warm light slanting down through gaps in the canopy, swaying slowly. */
	private void godRays(VertexConsumer glow, Matrix4f m, Vec3d b, float a, float k) {
		double crown = EternalTree.height(s) * 0.8;
		double spread = 40 * s;
		Vec3d slant = new Vec3d(0.35, -1, 0.2).normalize();
		for (int i = 0; i < 12; i++) {
			double ang = Solids.hash(i, 71) * MathHelper.TAU;
			double r = spread * (0.25 + 0.75 * Solids.hash(i, 72));
			Vec3d top = b.add(Math.cos(ang) * r, crown - 6 * s + Math.sin(a * 0.02 + i) * 2, Math.sin(ang) * r);
			double len = (top.y + 0.5) / -slant.y;
			Vec3d bottom = top.add(slant.multiply(len));
			float flicker = (float) (0.55 + 0.45 * Math.sin(a * 0.035 + i * 1.7));
			Solids.tube(glow, m, top, bottom, 1.2 * s + 0.6, 3.6 * s + 1.2, 8, 0xFFF0C0, 0.05F * k * flicker, false);
			FxDraw.disc(glow, m, bottom.add(0, 0.2, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 4 * s + 1.5, 0xFFF0C0, 0.25F * k * flicker, 0.0F, 14);
		}
	}

	/** Fireflies and pollen sparks drifting lazily through and under the canopy. */
	private void fireflies(VertexConsumer glow, Matrix4f m, Vec3d[] cb, Vec3d b, float a, float k) {
		double crown = EternalTree.height(s) * 0.7;
		for (int i = 0; i < 70; i++) {
			double ang = Solids.hash(i, 81) * MathHelper.TAU + a * 0.004 * (1 + Solids.hash(i, 82));
			double r = 10 + 45 * s * Solids.hash(i, 83);
			double y = crown * (0.15 + 0.85 * Solids.hash(i, 84)) + Math.sin(a * 0.05 + i) * 2.5;
			Vec3d p = b.add(Math.cos(ang) * r, y, Math.sin(ang) * r);
			float blink = (float) Math.max(0, Math.sin(a * 0.09 + i * 2.3));
			Solids.dot(glow, m, p, cb, 0.35 + 0.3 * s, i % 5 == 0 ? 0xFFE080 : 0xC8FF70, 0.9F * k * blink);
		}
	}
}
