package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.DiscoBeams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The DISCO BALL in the world: a mirrored ball on a chain whose facets glint as it spins, and the
 * light beams it throws across the land (same maths as the server's burns, see {@link DiscoBeams}).
 * After the drop it keeps turning slowly and pulses the screen hue on the beat.
 */
public class DiscoBallFx extends FxEffect {
	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);
	private final Vec3d anchor;
	private final double radius;

	public DiscoBallFx(FxPayload p) {
		super(Math.max(40, p.duration()));
		this.anchor = new Vec3d(p.x(), p.y(), p.z());
		this.radius = p.scale() > 0 ? p.scale() : DiscoBeams.RADIUS;
	}

	static int mix(int a, int b, double t) {
		int r = (int) MathHelper.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	@Override
	public boolean tick(ClientWorld world) {
		double t = age;
		double light = DiscoBeams.intensity(t);
		Vec3d ball = DiscoBeams.ball(anchor, t);
		if (light > 0.05) {
			ClientFx.light(ball, (float) (0.35 + 0.55 * light), DiscoBeams.color(age % DiscoBeams.BEAMS, t));
		}
		int k = age - DiscoBeams.DROP;
		if (k == 0) {
			ClientFx.hueKick(1.0F);
		} else if (k > 0 && k % 10 == 0 && age < lifetime - 30) {
			// The beat.
			ClientFx.hueKick(k % 20 == 0 ? 0.5F : 0.28F);
			ClientFx.light(ball, 0.7F, DiscoBeams.color(k / 10, t));
		}
		if (k >= 0 && k < 260 && age < lifetime - 30) {
			confetti(world, k);
		}
		return super.tick(world);
	}

	/** Confetti raining around whoever is on the dance floor (a burst on the drop, then a steady drizzle). */
	private void confetti(ClientWorld world, int k) {
		Vec3d cam = ClientFx.cameraPos();
		if (Math.hypot(cam.x - anchor.x, cam.z - anchor.z) > 170) {
			return;
		}
		int n = k < 30 ? 22 : 7;
		for (int i = 0; i < n; i++) {
			int c = MathHelper.hsvToRgb(world.random.nextFloat(), 0.75F, 1.0F);
			Vector3f col = new Vector3f(((c >> 16) & 0xFF) / 255.0F, ((c >> 8) & 0xFF) / 255.0F, (c & 0xFF) / 255.0F);
			double x = cam.x + world.random.nextGaussian() * 14, z = cam.z + world.random.nextGaussian() * 14;
			double y = cam.y + 8 + world.random.nextDouble() * 14;
			world.addImportantParticle(new DustParticleEffect(col, 1.1F + world.random.nextFloat() * 0.9F), true, x, y, z,
					world.random.nextGaussian() * 0.04, -0.06, world.random.nextGaussian() * 0.04);
		}
	}

	private static double tileHash(int tx, int tz, int salt) {
		return Solids.hash(tx * 31 + tz * 1031, salt);
	}

	/**
	 * After the drop the real dance floor lights up in patterns: client-side glow quads laid over the
	 * 3x3 tiles the server built (same grid), changing pattern every eight beats. Only tiles near the
	 * camera are drawn.
	 */
	private void floorGlow(ClientWorld world, VertexConsumer glow, Matrix4f m, Vec3d cam, double t) {
		double k = t - DiscoBeams.DROP - 25;
		if (k <= 0) {
			return;
		}
		float fadeOut = (float) MathHelper.clamp((lifetime - t) / 40.0, 0.0, 1.0);
		int beat = (int) (k / 10);
		float pulse = (float) (1.0 - (k % 10) / 10.0) * 0.55F + 0.3F;
		int mode = (beat / 8) % 4;
		int lodR = 42;
		int ctx = Math.floorDiv(MathHelper.floor(cam.x), 3), ctz = Math.floorDiv(MathHelper.floor(cam.z), 3);
		double reach = Math.min(128, 30 + k * 6);
		for (int tx = ctx - lodR / 3; tx <= ctx + lodR / 3; tx++) {
			for (int tz = ctz - lodR / 3; tz <= ctz + lodR / 3; tz++) {
				double cx = tx * 3 + 1.5, cz = tz * 3 + 1.5;
				double d = Math.hypot(cx - anchor.x, cz - anchor.z);
				if (d > reach || Math.hypot(cx - cam.x, cz - cam.z) > lodR) {
					continue;
				}
				boolean lit = switch (mode) {
					case 0 -> Math.floorMod((int) (d / 7) - beat, 4) == 0;
					case 1 -> ((tx + tz + beat) & 1) == 0;
					case 2 -> Math.floorMod((int) (Math.atan2(cz - anchor.z, cx - anchor.x) / MathHelper.TAU * 12 + d / 18) - beat, 3) == 0;
					default -> tileHash(tx, tz, beat) > 0.62;
				};
				if (!lit) {
					continue;
				}
				int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MathHelper.floor(cx), MathHelper.floor(cz));
				int col = MathHelper.hsvToRgb((float) ((tileHash(tx, tz, 3) + beat * 0.13) % 1.0), 0.8F, 1.0F);
				double yy = y + 0.04 - cam.y;
				double x0 = tx * 3 - cam.x + 0.1, x1 = tx * 3 + 3 - cam.x - 0.1, z0 = tz * 3 - cam.z + 0.1, z1 = tz * 3 + 3 - cam.z - 0.1;
				float a = 0.55F * pulse * fadeOut;
				Solids.quad(glow, m, new Vec3d(x0, yy, z0), new Vec3d(x0, yy, z1), new Vec3d(x1, yy, z1), new Vec3d(x1, yy, z0), col, a);
			}
		}
	}

	/** Afterparty lasers: thin, saturated, fast-sweeping fans that strobe with the beat. */
	private void lasers(ClientWorld world, VertexConsumer glow, Matrix4f m, Vec3d b, Vec3d cam, double t) {
		double k = t - DiscoBeams.DROP - 20;
		if (k <= 0 || t > lifetime - 30) {
			return;
		}
		int beat = (int) (k / 10);
		if ((beat & 3) == 3) {
			return;
		}
		int[] cols = {0x30FF60, 0xFF2050, 0x30E0FF, 0xFFE030};
		int col = cols[(beat / 4) % cols.length];
		for (int i = 0; i < 12; i++) {
			double az = i * MathHelper.TAU / 12 + Math.sin(t * 0.06 + i * 0.7) * 1.1 + t * 0.02;
			double dip = Math.toRadians(18 + 22 * (0.5 + 0.5 * Math.sin(t * 0.11 + i * 1.3)));
			Vec3d d = new Vec3d(Math.cos(az) * Math.cos(dip), -Math.sin(dip), Math.sin(az) * Math.cos(dip));
			Vec3d bw = b.add(cam);
			double reach = (bw.y - anchor.y) / Math.max(0.05, -d.y);
			Vec3d hw = bw.add(d.multiply(reach));
			double gy = MegaEffects.ground(world, hw.x, hw.z);
			Vec3d to = new Vec3d(hw.x, Math.max(gy, anchor.y - 20), hw.z).subtract(cam);
			Vec3d from = b.add(d.multiply(radius));
			FxDraw.ribbon(glow, m, from, to, Vec3d.ZERO, 0.12, 0xFFFFFF, 0.9F, 0.9F);
			FxDraw.ribbon(glow, m, from, to, Vec3d.ZERO, 0.55, col, 0.45F, 0.45F);
			FxDraw.disc(glow, m, to.add(0, 0.1, 0), X, Z, 1.2, col, 0.9F, 0.0F, 10);
		}
	}

	/** The classic disco-ball spots: little reflected lights crawling over the ground as it turns. */
	private void spots(ClientWorld world, VertexConsumer glow, Matrix4f m, Vec3d b, Vec3d cam, double spin, double t, double strength, double cold) {
		if (strength <= 0.02) {
			return;
		}
		Vec3d bw = b.add(cam);
		for (int k = 0; k < 110; k++) {
			Vec3d d = Cinematics.sphereDir(k, 57);
			if (d.y > -0.12) {
				continue;
			}
			d = new Vec3d(d.x * Math.cos(spin) - d.z * Math.sin(spin), d.y, d.x * Math.sin(spin) + d.z * Math.cos(spin));
			double reach = (bw.y - anchor.y) / -d.y;
			Vec3d hw = bw.add(d.multiply(reach));
			if (Math.hypot(hw.x - cam.x, hw.z - cam.z) > 140) {
				continue;
			}
			double gy = MegaEffects.ground(world, hw.x, hw.z);
			Vec3d p = new Vec3d(hw.x, Math.max(gy, anchor.y - 20) + 0.06, hw.z).subtract(cam);
			int col = k % 4 == 0 ? mix(DiscoBeams.color(k, t), 0x8ED0FF, cold) : mix(0xFFFFFF, 0xBFE4FF, cold);
			float tw = (float) (0.6 + 0.4 * Math.sin(t * 0.5 + k));
			FxDraw.disc(glow, m, p, X, Z, 0.9 + 0.5 * Solids.hash(k, 58), col, (float) (0.75 * strength) * tw, 0.0F, 10);
		}
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		ClientWorld world = com.armageddon.client.render.FxDraw.world();
		if (world == null) {
			return;
		}
		double t = age + td;
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d b = DiscoBeams.ball(anchor, t).subtract(cam);
		double spin = DiscoBeams.angle(t);
		double cold = DiscoBeams.blue(t);
		double light = DiscoBeams.intensity(t);

		// Chain and cap.
		Solids.tube(solid, m, b.add(0, radius * 0.9, 0), b.add(0, radius + 280, 0), 0.6, 0.6, 6, 0x4A4A55, 1.0F, true);
		Solids.tube(solid, m, b.add(0, radius * 0.82, 0), b.add(0, radius * 1.18, 0), radius * 0.26, radius * 0.2, 10, 0x8A8A96, 1.0F, true);

		// Mirror tiles: the pattern turns with the spin and random tiles flash.
		int base = mix(0xC4C8D4, 0x8ED0FF, cold);
		int step = (int) (t * 0.6);
		Solids.ellipsoid(solid, m, b, radius, radius, radius, 18, 36, (i, j, n) -> {
			double az = Math.atan2(n.z, n.x) - spin;
			int jj = Math.floorMod((int) Math.floor(az / MathHelper.TAU * 36), 36);
			int cell = i * 64 + jj;
			if (Solids.hash(cell, step) > 0.94) {
				return 0xFFFFFF;
			}
			return Solids.scale(base, 0.62 + 0.5 * Solids.hash(cell, 9));
		}, true, 1.0F);

		// Glints on the facets that face the camera.
		Vec3d toCam = b.multiply(-1).normalize();
		for (int k = 0; k < 36; k++) {
			Vec3d d = Cinematics.sphereDir(k, 31);
			d = new Vec3d(d.x * Math.cos(spin) - d.z * Math.sin(spin), d.y, d.x * Math.sin(spin) + d.z * Math.cos(spin));
			double facing = d.dotProduct(toCam);
			if (facing < 0.45) {
				continue;
			}
			float tw = (float) (0.5 + 0.5 * Math.sin(t * 0.8 + k * 2.3)) * (float) facing;
			Vec3d p = b.add(d.multiply(radius * 1.02));
			Solids.dot(glow, m, p, cb, radius * 0.14, 0xFFFFFF, tw);
			if (tw > 0.75) {
				FxDraw.ribbon(glow, m, p.subtract(cb[0].multiply(radius * 0.6)), p.add(cb[0].multiply(radius * 0.6)), Vec3d.ZERO, 0.25, 0xE0F0FF, tw * 0.6F, tw * 0.6F);
				FxDraw.ribbon(glow, m, p.subtract(cb[1].multiply(radius * 0.4)), p.add(cb[1].multiply(radius * 0.4)), Vec3d.ZERO, 0.25, 0xE0F0FF, tw * 0.5F, tw * 0.5F);
			}
		}
		Solids.dot(glow, m, b, cb, radius * 2.4, DiscoBeams.color(0, t), (float) (0.08 + 0.22 * light));
		// Rim light and a slow halo of colour around the ball.
		FxDraw.sphere(glow, m, b, radius * 1.04, Vec3d.ZERO, mix(0xFFFFFF, 0x8ED0FF, cold), 0.0F, 0.35F, 12, 24);
		for (int k = 0; k < 3; k++) {
			double ra = spin * (k == 1 ? -0.4 : 0.3) + k * 2.1;
			Vec3d u = new Vec3d(Math.cos(ra), 0.25 * (k - 1), Math.sin(ra)).normalize(), v = new Vec3d(0, 1, 0).crossProduct(u).normalize();
			double rr = radius * (1.5 + 0.25 * k);
			FxDraw.ring(glow, m, b, u, v, rr, rr + radius * 0.06, DiscoBeams.color(k * 8, t), 0.0F, (float) (0.15 + 0.35 * light), 0.0F, 48);
		}
		double spotK = t < DiscoBeams.DESCEND_END ? 0 : t < DiscoBeams.STOP ? Math.max(0.25, light) : t < DiscoBeams.DROP ? 0 : 0.6;
		spots(world, glow, m, b, cam, spin, t, spotK, cold);
		if (t > DiscoBeams.DROP) {
			floorGlow(world, glow, m, cam, t);
			lasers(world, glow, m, b, cam, t);
		}

		// The beams.
		if (light > 0.01) {
			boolean hot = DiscoBeams.hot(t);
			int bottom = world.getBottomY() + 1;
			for (int i = 0; i < DiscoBeams.BEAMS; i++) {
				Vec3d hw = DiscoBeams.hit(anchor, i, t);
				double gy = MegaEffects.ground(world, hw.x, hw.z);
				if (gy <= bottom) {
					gy = anchor.y;
				}
				Vec3d to = new Vec3d(hw.x, gy, hw.z).subtract(cam);
				Vec3d from = b.add(DiscoBeams.dir(i, t).multiply(radius * 0.95));
				int col = DiscoBeams.color(i, t);
				FxDraw.beam(glow, m, from, to, Vec3d.ZERO, 0.8 + 0.7 * light, col, (float) (0.75 * light));
				// Volumetric cone, and dust motes drifting through it (near the camera only).
				Solids.tube(glow, m, from, to, 0.4, 3.2, 8, col, (float) (0.08 * light), false);
				Vec3d mid = from.add(to).multiply(0.5);
				if (mid.lengthSquared() < 140 * 140) {
					for (int q = 0; q < 6; q++) {
						double u = (Solids.hash(i * 7 + q, 41) + t * 0.004 * (1 + q % 3)) % 1.0;
						Vec3d mp = from.add(to.subtract(from).multiply(u)).add((Solids.hash(q, i) - 0.5) * 2.0 * u, (Solids.hash(q + 9, i) - 0.5) * 2.0 * u, (Solids.hash(q + 19, i) - 0.5) * 2.0 * u);
						float tw = (float) (0.5 + 0.5 * Math.sin(t * 0.3 + q * 2 + i));
						Solids.dot(glow, m, mp, cb, 0.22, 0xFFFFFF, (float) (0.7 * light) * tw);
					}
				}
				FxDraw.disc(glow, m, to.add(0, 0.15, 0), X, Z, 3.4, col, (float) (0.9 * light), 0.0F, 16);
				if (hot) {
					FxDraw.disc(glow, m, to.add(0, 0.25, 0), X, Z, 1.7, 0xFFF0C0, (float) (0.9 * light), 0.0F, 12);
					FxDraw.beam(glow, m, to, to.add(0, 5, 0), Vec3d.ZERO, 0.7, 0xFF7A30, (float) (0.35 * light));
				}
			}
		}
	}
}
