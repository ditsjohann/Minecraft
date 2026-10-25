package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.DiscoBeams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
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
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		ClientWorld world = MinecraftClient.getInstance().world;
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
				FxDraw.disc(glow, m, to.add(0, 0.15, 0), X, Z, 3.4, col, (float) (0.9 * light), 0.0F, 16);
				if (hot) {
					FxDraw.disc(glow, m, to.add(0, 0.25, 0), X, Z, 1.7, 0xFFF0C0, (float) (0.9 * light), 0.0F, 12);
					FxDraw.beam(glow, m, to, to.add(0, 5, 0), Vec3d.ZERO, 0.7, 0xFF7A30, (float) (0.35 * light));
				}
			}
		}
	}
}
