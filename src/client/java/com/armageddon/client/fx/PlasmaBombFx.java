package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.vfx.FxEngine;
import com.armageddon.network.FxPayload;
import com.armageddon.util.PlasmaShapes.Bottle;
import com.armageddon.util.PlasmaShapes.Plasmoid;
import com.armageddon.vfx.VfxIds;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The parts of the plasma bombs the particle engine does not draw: the Plasmoid Swarm's orbs (and the arcs between
 * them and their core) and the Magnetic Bottle's sun, its cage of field lines, its jets and its slicing ring.
 */
final class PlasmaBombFx {
	private PlasmaBombFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 68 -> new PlasmoidFx(a, p.duration());
			case 69 -> new BottleFx(a, p.duration());
			default -> null;
		};
	}

	private static final Vec3d X = new Vec3d(1, 0, 0), Z = new Vec3d(0, 0, 1);

	private static float smooth(double u) {
		u = MathHelper.clamp(u, 0.0, 1.0);
		return (float) (u * u * (3 - 2 * u));
	}

	// ====================================================================== PLASMOID SWARM

	/** Ball lightning: each orb a flickering knot of violet light with sparks jumping off it; arcs from the core to the newest. */
	static final class PlasmoidFx extends FxEffect {
		private final Vec3d c;

		PlasmoidFx(Vec3d c, int life) {
			super(Math.max(40, life));
			this.c = c;
		}

		/** Orb {@code k} in the world at tick {@code t}, riding over the ground. */
		private Vec3d orb(int k, double t) {
			Vec3d l = Plasmoid.pos(k, t);
			if (l == null) {
				return null;
			}
			double x = c.x + l.x, z = c.z + l.z;
			return new Vec3d(x, NewFx.groundY(x, z, c.y) + l.y, z);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			for (int k = 0; k < Plasmoid.N; k++) {
				Vec3d p = orb(k, t), q = orb(k, t - 1);
				if (p == null || q == null) {
					continue;
				}
				if ((t + k) % 2 == 0) {
					Vec3d v = p.subtract(q);
					FxEngine.playMoving(VfxIds.PLASMOID_TRAIL, p.x, p.y, p.z, 0, 1, 0, (float) (0.6 + Plasmoid.size(k) * 0.4), 0, t * 31L + k,
							(float) v.x, (float) v.y, (float) v.z);
				}
				if ((t + k) % 8 == 0) {
					ClientFx.light(p, 0.35F, 0xB070FF);
				}
			}
			if (t < Plasmoid.BURST && t % 6 == 0) {
				ClientFx.light(c.add(0, Plasmoid.CORE_Y, 0), 0.6F, 0xC090FF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d core = c.add(0, Plasmoid.CORE_Y, 0).subtract(cam);
			if (a < Plasmoid.BURST) {
				float grow = smooth(a / Plasmoid.CHARGE);
				float pulse = 0.85F + 0.15F * MathHelper.sin(a * 0.9F) + 0.1F * MathHelper.sin(a * 2.3F);
				FxKit.halo(consumers, m, cb, core, 7 * grow * pulse, 0x9040FF, 0.35F);
				FxKit.halo(consumers, m, cb, core, 3.2 * grow * pulse, 0xE0C0FF, 0.7F);
				Solids.flare(glow, m, core, cb, 2.0 * grow, 0xFFFFFF, 0.8F * grow);
				for (int i = 0; i < 4; i++) {
					int seed = (int) a * 7 + i * 131;
					double th = Effects.hash(seed, 1) * MathHelper.TAU, ph = Effects.hash(seed, 2) * Math.PI;
					Vec3d out = new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th)).multiply(3 + 3 * Effects.hash(seed, 3));
					Effects.bolt(glow, m, core, core.add(out), 0.08, 0xB080FF, 0.7F * grow, seed);
				}
			}
			for (int k = 0; k < Plasmoid.N; k++) {
				Vec3d w = orb(k, a);
				if (w == null) {
					continue;
				}
				Vec3d p = w.subtract(cam);
				double s = Plasmoid.size(k);
				float fl = 0.75F + 0.25F * MathHelper.sin(a * 1.7F + k * 2) * MathHelper.sin(a * 0.9F + k);
				float born = smooth((a - Plasmoid.launch(k)) / 6.0);
				// Swelling just before it goes.
				float swell = 1 + 0.5F * smooth((a - (Plasmoid.pop(k) - 12)) / 12.0);
				FxKit.halo(consumers, m, cb, p, s * 3.0 * fl * swell * born, 0x8A40FF, 0.4F);
				FxKit.halo(consumers, m, cb, p, s * 1.4 * fl * swell * born, 0xE8D0FF, 0.75F);
				Solids.dot(glow, m, p, cb, s * 0.45 * swell, 0xFFFFFF, 0.9F * born);
				int seed = (int) (a * 0.5F) * 17 + k * 97;
				for (int i = 0; i < 2; i++) {
					double th = Effects.hash(seed + i, 5) * MathHelper.TAU, ph = Effects.hash(seed + i, 6) * Math.PI;
					Vec3d out = new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th)).multiply(s * (1.5 + Effects.hash(seed + i, 7)));
					Effects.bolt(glow, m, p, p.add(out), 0.05, 0xC090FF, 0.6F * born, seed + i);
				}
				// An arc back to the core while it is still close.
				if (a < Plasmoid.BURST && a - Plasmoid.launch(k) < 30) {
					float link = 1 - (float) (a - Plasmoid.launch(k)) / 30;
					Effects.bolt(glow, m, core, p, 0.1, 0xB080FF, 0.6F * link, (int) a * 3 + k);
				}
			}
		}
	}

	// ====================================================================== MAGNETIC BOTTLE

	/**
	 * The bottle: a sun of magenta plasma with a white core, caged by glowing field lines that wind round it and pulse,
	 * the cage buckling as the end nears; when it fails, the jets out of its poles and the ring racing out of it.
	 */
	static final class BottleFx extends FxEffect {
		private static final int MERIDIANS = 12;
		private final Vec3d c;

		BottleFx(Vec3d c, int life) {
			super(Math.max(40, life));
			this.c = c;
		}

		private Vec3d sun(double t) {
			return c.add(0, Bottle.y(t), 0);
		}

		@Override
		public boolean tick(ClientWorld world) {
			int t = age;
			Vec3d s = sun(t);
			if (t > 20 && t < Bottle.FAIL && t % 6 == 0) {
				FxEngine.play(VfxIds.BOTTLE_FIELD, s.x, s.y, s.z, 0, 1, 0, 1.0F, 0, t * 41L);
			}
			if (t < Bottle.RING + 10 && t % 6 == 0) {
				ClientFx.light(s, 0.7F + 0.5F * smooth((t - Bottle.HOLD) / 190.0), 0xFF60E0);
			}
			if (t >= Bottle.RING && t < Bottle.RING_END && t % 4 == 0) {
				double r = Bottle.ring(t);
				ClientFx.light(c.add(r, 2, 0), 0.6F, 0xFF70E0);
				ClientFx.light(c.add(-r, 2, 0), 0.6F, 0xFF70E0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d s = sun(a).subtract(cam);
			float form = smooth(a / 50.0);
			float strain = smooth((a - Bottle.HOLD) / (double) (Bottle.FAIL - Bottle.HOLD));
			// The sun, until the bottle has emptied.
			float alive = 1 - smooth((a - Bottle.RING) / 20.0);
			if (alive > 0.01F) {
				double r = Bottle.SUN * form * (1 + 0.06 * MathHelper.sin(a * 0.4F) + 0.25 * strain);
				FxDraw.sphere(glow, m, s, r, Vec3d.ZERO, 0xFF40D0, 0.25F * alive, 0.7F * alive, 12, 18);
				FxDraw.sphere(glow, m, s, r * 0.62, Vec3d.ZERO, 0xFFD0F4, 0.8F * alive, 0.4F * alive, 10, 14);
				FxKit.halo(consumers, m, cb, s, r * 3.2, 0xFF40C0, 0.35F * alive);
				FxKit.halo(consumers, m, cb, s, r * 1.5, 0xFFFFFF, 0.5F * alive);
				// Hot spots churning over its face.
				for (int i = 0; i < 6; i++) {
					double th = a * (0.03 + 0.01 * i) * (1 + 2 * strain) + i * 1.1, ph = 1.2 + Math.sin(a * 0.02 + i) * 0.7;
					Vec3d q = s.add(Math.sin(ph) * Math.cos(th) * r * 0.95, Math.cos(ph) * r * 0.95, Math.sin(ph) * Math.sin(th) * r * 0.95);
					Solids.dot(glow, m, q, cb, r * 0.3, 0xFFF0FA, 0.35F * alive);
				}
			}
			// The cage: dipole field lines (r = L sin^2 theta), spinning, a pulse running along each, buckling under strain.
			float cage = form * (1 - smooth((a - Bottle.FAIL) / 14.0));
			if (cage > 0.01F) {
				double spin = a * (0.02 + 0.05 * strain);
				for (int shell = 0; shell < 2; shell++) {
					double L = Bottle.CAGE * (shell == 0 ? 0.78 : 1.0);
					double th0 = Math.asin(Math.min(1, Math.sqrt(Bottle.SUN * 1.05 / L)));
					for (int k = 0; k < MERIDIANS; k++) {
						double phi = MathHelper.TAU * k / MERIDIANS + spin * (shell == 0 ? 1 : -0.7) + shell * 0.26;
						int col = (k + shell) % 3 == 0 ? 0xFFFFFF : shell == 0 ? 0xFF60E0 : 0x60C8FF;
						Vec3d prev = null;
						int n = 24;
						for (int i = 0; i <= n; i++) {
							double th = th0 + (Math.PI - 2 * th0) * i / n;
							double rr = L * Math.sin(th) * Math.sin(th);
							// Buckling: a kink that grows and travels round the cage.
							double kink = strain * strain * 1.6 * Math.sin(th * 3 + a * 0.3 + k) * Math.sin(th);
							double pp = phi + kink * 0.12;
							Vec3d q = s.add(Math.sin(th) * Math.cos(pp) * (rr + kink), Math.cos(th) * rr, Math.sin(th) * Math.sin(pp) * (rr + kink));
							if (a > Bottle.FAIL) {
								// Snapping: flung outward and broken.
								double fly = (a - Bottle.FAIL) * 0.6;
								q = q.add(q.subtract(s).normalize().multiply(fly));
							}
							if (prev != null && !(a > Bottle.FAIL && (i + k) % 3 == 0)) {
								float pulse = (float) (0.35 + 0.65 * Math.pow(0.5 + 0.5 * Math.sin(th * 6 - a * 0.35 + k), 6));
								float al = cage * (0.25F + 0.45F * pulse) * (1 + strain * 0.5F);
								FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.22 + 0.15 * pulse, col, al, al);
							}
							prev = q;
						}
					}
				}
			}
			// The jets out of both poles.
			if (a >= Bottle.JETS && a < Bottle.JETS + 80) {
				float on = smooth((a - Bottle.JETS) / 4.0) * (1 - smooth((a - Bottle.JETS - 50) / 30.0));
				double w = 2.6 * (1 + 0.1 * MathHelper.sin(a * 1.3F));
				FxDraw.beam(glow, m, s, s.add(0, 140, 0), Vec3d.ZERO, w, 0xFF50E0, 0.8F * on);
				double depth = Bottle.y(a) + Bottle.SHAFT * Math.min(1, (a - Bottle.JETS) / 40.0);
				FxDraw.beam(glow, m, s, s.add(0, -depth, 0), Vec3d.ZERO, w, 0xFF50E0, 0.8F * on);
				FxKit.halo(consumers, m, cb, s.add(0, -depth, 0), 5, 0xFF80E8, 0.6F * on);
			}
			// The ring racing out of the equator.
			if (a >= Bottle.RING && a < Bottle.RING_END + 12) {
				double r = Bottle.ring(a);
				float on = 1 - smooth((a - Bottle.RING_END) / 12.0);
				Vec3d ring = c.add(0, Bottle.LOW, 0).subtract(cam);
				FxDraw.ring(glow, m, ring, X, Z, Math.max(0, r - 1.6), r + 1.6, 0xFF50D0, 0.0F, 0.55F * on, 0.0F, 128);
				FxDraw.ring(glow, m, ring, X, Z, Math.max(0, r - 0.35), r + 0.35, 0xFFFFFF, 0.0F, 0.9F * on, 0.0F, 128);
				FxDraw.ring(glow, m, ring, X, Z, Math.max(0, r - 7), r, 0xFF40C0, 0.0F, 0.12F * on, 0.0F, 96);
			}
		}
	}
}
