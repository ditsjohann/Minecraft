package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.util.DiscoBeams;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * DISCO BALL cutscene. The ball and its beams are the in-world {@link DiscoBallFx}; this scene only
 * directs the camera around them and adds anamorphic lens streaks on the close-ups.
 */
public class DiscoBallCine extends Cinematics.Cine {
	private static final int SPIN_SHOT = 100, AERIAL = 220, COLD = 330, BRAKE = 460, SILENCE = DiscoBeams.STOP, DROP = DiscoBeams.DROP, FLOOR = 620;

	public DiscoBallCine(Vec3d anchor, Vec3d heading, int life) {
		super(anchor, heading, life);
	}

	private Vec3d ball(float a) {
		return DiscoBeams.ball(anchor, a);
	}

	@Override
	protected void direct(float a, Cinematics.State s) {
		Vec3d ball = ball(a);
		if (a < SPIN_SHOT) {
			// Low in the street, watching it come down.
			double u = Cinematics.ease(a / SPIN_SHOT);
			Vec3d p = anchor.add(fwd.multiply(-34 + u * 6)).add(right.multiply(16)).add(0, 2.4, 0);
			Cinematics.aim(s, p, ball, (float) Cinematics.lerp(64, 74, u), false);
		} else if (a < AERIAL) {
			// Close orbit, pushing in as it spins faster and faster.
			double u = (a - SPIN_SHOT) / (AERIAL - SPIN_SHOT);
			double ang = Math.atan2(-fwd.z, -fwd.x) + u * 1.6;
			double d = Cinematics.lerp(52, 34, Cinematics.ease(u));
			Vec3d p = ball.add(Math.cos(ang) * d, -10 + u * 4, Math.sin(ang) * d);
			Cinematics.aim(s, p, ball, (float) Cinematics.lerp(60, 50, u), false);
		} else if (a < COLD) {
			// High above the city: the beams sweep and burn.
			double u = (a - AERIAL) / (COLD - AERIAL);
			double ang = Math.atan2(fwd.z, fwd.x) + Math.PI * 0.75 + u * 0.7;
			Vec3d p = anchor.add(Math.cos(ang) * 85, 105 + u * 15, Math.sin(ang) * 85);
			Cinematics.aim(s, p, anchor.add(fwd.multiply(20)), 82, false);
		} else if (a < BRAKE) {
			// Cold close-up: slow dolly under the ball as the colour drains away.
			double u = (a - COLD) / (BRAKE - COLD);
			Vec3d p = ball.add(right.multiply(Cinematics.lerp(30, 22, u))).add(fwd.multiply(-26)).add(0, -16 + u * 3, 0);
			Cinematics.aim(s, p, ball, 46, false);
		} else if (a < DROP) {
			// Wide and low: it slows... and stops.
			double u = (a - BRAKE) / (DROP - BRAKE);
			Vec3d p = anchor.add(fwd.multiply(-60)).add(right.multiply(-20)).add(0, 3 + u * 2, 0);
			Cinematics.aim(s, p, Cinematics.lerp(ball, anchor.add(0, 20, 0), 0.3), (float) Cinematics.lerp(60, 52, u), false);
		} else if (a < FLOOR) {
			// THE DROP: wide shot as the shockwave rolls out.
			double u = Cinematics.ease((a - DROP) / (FLOOR - DROP));
			Vec3d p = anchor.add(fwd.multiply(-Cinematics.lerp(70, 95, u))).add(right.multiply(30)).add(0, Cinematics.lerp(8, 30, u), 0);
			Cinematics.aim(s, p, anchor.add(0, 18, 0), (float) Cinematics.lerp(98, 80, u), false);
		} else {
			// Rising over the dance floor as it spreads.
			double u = Cinematics.ease((a - FLOOR) / (lifetime - FLOOR));
			double ang = Math.atan2(-fwd.z, -fwd.x) + u * 0.9;
			Vec3d p = anchor.add(Math.cos(ang) * 70, Cinematics.lerp(55, 130, u), Math.sin(ang) * 70);
			Cinematics.aim(s, p, anchor, 80, false);
		}
	}

	@Override
	protected float handheld(float a) {
		if (a >= 200 && a < COLD) {
			return 0.6F;
		}
		if (a >= DROP && a < DROP + 30) {
			return 1.2F;
		}
		return 0.15F;
	}

	@Override
	public float fade(float a) {
		float silence = (float) Cinematics.clamp01(Math.min((a - SILENCE) / 8.0, (DROP - a) / 3.0)) * 0.85F;
		return Math.max(super.fade(a), Math.max(silence, Math.max(Cinematics.dip(a, AERIAL, 5), Cinematics.dip(a, COLD, 5))));
	}

	@Override
	public Cinematics.Caption caption(float a) {
		Cinematics.Caption c;
		if ((c = Cinematics.cap(a, 14, 92, "DISCO BALL", "the party is starting", 0xFF80FF)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 120, 170, "SPIN", "", 0x80FFFF)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 176, 216, "FASTER", "", 0xFFFF60)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 236, 318, "IT'S GETTING HOT", "the light is burning everything it touches", 0xFF8040)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 360, 448, "...cooling down", "", 0x80C8FF)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 476, 516, "it stops.", "", 0xFFFFFF)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, 528, 556, "wait for it", "", 0x808080)) != null) {
			return c;
		}
		if ((c = Cinematics.cap(a, DROP, DROP + 40, "DROP", "", 0xFF40FF)) != null) {
			return c;
		}
		return Cinematics.cap(a, FLOOR + 6, lifetime - 6, "THE WORLD IS A DISCO", "", 0x60FFFF);
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age < DROP) {
			ClientFx.tintSky(age < COLD ? 0x140828 : 0x061430, 0.8F);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		Vec3d b = ball(a).subtract(cam);
		double light = DiscoBeams.intensity(a);

		// The silence: one hard white spotlight from the rafters onto the stopped ball.
		if (a >= SILENCE - 10 && a < DROP) {
			float k = (float) Cinematics.clamp01((a - SILENCE + 10) / 14.0);
			Solids.tube(glow, m, b.add(0, 120, 0), b, 1.0, DiscoBeams.RADIUS * 1.25, 14, 0xFFFFFF, 0.07F * k, false);
			Solids.dot(glow, m, b.add(0, DiscoBeams.RADIUS * 0.6, 0), cb, DiscoBeams.RADIUS * 0.8, 0xFFFFFF, 0.25F * k);
		}

		// THE DROP: rainbow shock rings racing out over the ground, a column of light and a wall of glitter dust.
		if (a >= DROP && a < DROP + 90) {
			float k = a - DROP;
			Vec3d g = anchor.subtract(cam).add(0, 0.5, 0);
			Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
			for (int i = 0; i < 6; i++) {
				double rr = (k - i * 5) * 4.5;
				if (rr > 1) {
					int col = DiscoBeams.color(i * 4, a * 3);
					FxDraw.ring(glow, m, g, x, z, rr, rr + 3 + i, col, 0.0F, 0.75F * (1 - k / 90F), 0.0F, 96);
				}
			}
			FxDraw.beam(glow, m, g, g.add(0, 220, 0), Vec3d.ZERO, 6 * (1 - k / 90F), 0xFF80FF, 0.7F * (1 - k / 90F));
			Cinematics.dustWall(shade, m, cb, g, 12 + k * 3.8, 10 + k * 0.15, DiscoBeams.color((int) k, a), 0.45F * (1 - k / 90F), 13);
		}

		// Anamorphic streaks and lens ghosts across the ball on the close-ups.
		boolean close = (a >= SPIN_SHOT && a < AERIAL) || (a >= COLD && a < BRAKE);
		if (!close || light < 0.05) {
			return;
		}
		int col = DiscoBeams.color((int) (a * 0.2), a);
		float k = (float) light * (0.6F + 0.4F * (float) Math.sin(a * 0.9));
		FxDraw.ribbon(glow, m, b, b.add(cb[0].multiply(90)), Vec3d.ZERO, 1.6, col, 0.55F * k, 0.0F);
		FxDraw.ribbon(glow, m, b, b.subtract(cb[0].multiply(90)), Vec3d.ZERO, 1.6, col, 0.55F * k, 0.0F);
		FxDraw.ribbon(glow, m, b, b.add(cb[0].multiply(40)), Vec3d.ZERO, 0.5, 0xFFFFFF, 0.8F * k, 0.0F);
		FxDraw.ribbon(glow, m, b, b.subtract(cb[0].multiply(40)), Vec3d.ZERO, 0.5, 0xFFFFFF, 0.8F * k, 0.0F);
		Vec3d forward = Vec3d.fromPolar(pitch, yaw);
		Vec3d centre = forward.multiply(b.length());
		double[] at = {0.45, 0.7, 1.25, 1.6, 1.9};
		for (int i = 0; i < at.length; i++) {
			Vec3d gp = b.add(centre.subtract(b).multiply(at[i]));
			Solids.dot(glow, m, gp, cb, DiscoBeams.RADIUS * (0.25 + 0.2 * (i % 3)), DiscoBeams.color(i * 5, a), 0.18F * k);
		}
	}
}
