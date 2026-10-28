package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.SciShapes;
import com.armageddon.util.SciShapes.Dyson;
import com.armageddon.util.SciShapes.Gravity;
import com.armageddon.util.SciShapes.Ion;
import com.armageddon.util.SciShapes.Resonance;
import com.armageddon.util.SciShapes.Tesla;
import com.armageddon.util.SciShapes.Wave;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The machines and the light of the plasma / sci-fi bombs. A different look from the fire-and-smoke cataclysms:
 * thin hard lines of cyan light, lattices and rings, wireframes, scan planes and lensing. Kept deliberately
 * restrained in brightness so the destruction itself stays visible.
 */
final class SciFx {
	private SciFx() {
	}

	static FxEffect create(int kind, Vec3d a, Vec3d b, FxPayload p) {
		return switch (kind) {
			case 40 -> new IonFx(a, b.x, p.duration());
			case 41 -> new ResonanceFx(a, p.duration());
			case 42 -> new TeslaFx(a, b.x, p.duration());
			case 43 -> new DysonFx(a, b.x, p.duration());
			case 44 -> new GravityFx(a, p.duration());
			case 45 -> new WaveFx(a, p.duration());
			case 46 -> new HelixFx(a, b, p.scale(), p.p1(), p.duration());
			case 47 -> new GravitonFx(a, p.duration());
			case 48 -> new SunfallFx(a, b, (int) p.p1(), p.duration());
			case 49 -> new HaloFx(a, b.x, p.duration());
			default -> null;
		};
	}

	static double ground(double x, double z, double fallback) {
		return NewFx.groundY(x, z, fallback);
	}

	static float fadeIn(float a, float from, float len) {
		return MathHelper.clamp((a - from) / len, 0.0F, 1.0F);
	}

	static float fadeOut(float a, float from, float len) {
		return 1.0F - MathHelper.clamp((a - from) / len, 0.0F, 1.0F);
	}

	/** A lit cube tumbling about {@code (rx, ry)}: the phantom debris of the gravity and wave bombs. */
	static void cube(VertexConsumer solid, Matrix4f m, Vec3d c, double h, double rx, double ry, int rgb) {
		double cx = Math.cos(rx), sx = Math.sin(rx), cy = Math.cos(ry), sy = Math.sin(ry);
		Vec3d u = new Vec3d(cy, 0, -sy).multiply(h);
		Vec3d v = new Vec3d(sy * sx, cx, cy * sx).multiply(h);
		Vec3d w = new Vec3d(sy * cx, -sx, cy * cx).multiply(h);
		Vec3d[][] faces = {{u, v, w}, {u.multiply(-1), v, w.multiply(-1)}, {v, w, u}, {v.multiply(-1), w, u.multiply(-1)}, {w, u, v}, {w.multiply(-1), u, v.multiply(-1)}};
		for (Vec3d[] f : faces) {
			Vec3d n = f[0].normalize();
			int col = Solids.scale(rgb, Solids.light(n));
			Vec3d o = c.add(f[0]);
			Solids.quad(solid, m, o.subtract(f[1]).subtract(f[2]), o.add(f[1]).subtract(f[2]), o.add(f[1]).add(f[2]), o.subtract(f[1]).add(f[2]), col, 1.0F);
		}
	}

	/** Earth colours for phantom debris. */
	static final int[] EARTH = {0x5E9E3A, 0x6A8E34, 0x7A5A3A, 0x6A4A2E, 0x808080, 0x6E6E6E, 0xA0804A, 0x3A7A2A, 0x9A9488};

	/** A torus of short tube segments with glowing nodes: the ring machines' building block. */
	static void ring(VertexConsumer solid, VertexConsumer glow, Matrix4f m, Vec3d c, Vec3d u, Vec3d v, double r, double thick, int segs,
			int metal, int light, float lightA, double spin) {
		Vec3d prev = null;
		for (int i = 0; i <= segs; i++) {
			double ang = i * MathHelper.TAU / segs + spin;
			Vec3d p = c.add(u.multiply(Math.cos(ang) * r)).add(v.multiply(Math.sin(ang) * r));
			if (prev != null) {
				Solids.tube(solid, m, prev, p, thick, thick, 6, i % 4 == 0 ? Solids.scale(metal, 0.7) : metal, 1.0F, true);
				FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, thick * 0.5, light, lightA * 0.6F, lightA * 0.6F);
			}
			if (i % 4 == 0 && i < segs) {
				Solids.sphere(solid, m, p, thick * 1.2, Solids.scale(metal, 0.75), 6, 8, true);
			}
			prev = p;
		}
	}

	// ====================================================================== ION SCYTHE

	/** The orbital beam, its contact point, the glowing spiral it leaves, and the curtain of light when it ignites. */
	static final class IonFx extends FxEffect {
		private final Vec3d g;
		private final double phase;

		IonFx(Vec3d g, double phase, int life) {
			super(Math.max(40, life));
			this.g = g;
			this.phase = phase;
		}

		private Vec3d spiral(double th) {
			Vec3d o = Ion.at(th, phase);
			double x = g.x + o.x, z = g.z + o.z;
			return new Vec3d(x, ground(x, z, g.y) + 0.2, z);
		}

		/** When the beam passed angle {@code th}. */
		private static double cutTick(double th) {
			double thMax = Ion.R / (Ion.GAP / MathHelper.TAU);
			double u = th / thMax;
			return Ion.CARVE + u * u * (Ion.CARVE_END - Ion.CARVE);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= Ion.CARVE && age < Ion.CARVE_END) {
				Vec3d p = spiral(Ion.theta(age));
				for (int i = 0; i < 4; i++) {
					world.addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.4, p.z, world.random.nextGaussian() * 0.4, 0.3 + world.random.nextDouble() * 0.4,
							world.random.nextGaussian() * 0.4);
				}
				world.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5, p.z, 0, 0.12, 0);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d off = cam.multiply(-1);
			Vec3d sky = g.add(0, 420, 0).add(off);
			// The lens in orbit: a slow ring of light at the top of the beam.
			float on = fadeIn(a, 0, 30) * fadeOut(a, Ion.IGNITE, 30);
			FxDraw.ring(glow, m, sky, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 16, 20, 0x80D8FF, 0.0F, 0.5F * on, 0.0F, 48);
			FxDraw.ring(glow, m, sky, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 26, 27.5, 0xC0F0FF, 0.0F, 0.4F * on, 0.0F, 48);
			if (a >= Ion.DROP && a < Ion.CARVE_END + 4) {
				double th = Ion.theta(a);
				Vec3d hit = (a < Ion.CARVE ? new Vec3d(g.x, ground(g.x, g.z, g.y), g.z) : spiral(th)).add(off);
				float k = fadeIn(a, Ion.DROP, 6);
				FxDraw.beam(glow, m, sky, hit, Vec3d.ZERO, 1.0 * k, 0xFFFFFF, 0.85F);
				FxDraw.beam(glow, m, sky, hit, Vec3d.ZERO, 3.2 * k, 0x70D0FF, 0.35F);
				FxDraw.beam(glow, m, sky, hit, Vec3d.ZERO, 7.0 * k, 0x3060FF, 0.10F);
				FxKit.flare(consumers, m, cb, hit.add(0, 1, 0), 12, 0xB0E8FF, 0.7F * k, a * 0.02);
				FxKit.halo(consumers, m, cb, hit.add(0, 0.5, 0), 6, 0x80D0FF, 0.5F * k);
				FxKit.fire(consumers, m, cb, hit.add(0, 1.5, 0), 3.0, 0xC0F0FF, (a * 0.05F) % 1.0F, 0.6F, a, 401);
			}
			// The trench: hot white just behind the beam, cooling through cyan to a faint blue thread; during the
			// charge it all brightens and pulses, and at ignition a wall of light rises along it from the centre out.
			double thNow = a >= Ion.CARVE ? Ion.theta(Math.min(a, Ion.CARVE_END)) : 0;
			if (thNow <= 0.05 || a > Ion.END) {
				return;
			}
			double step = 0.12;
			Vec3d prev = null;
			double prevTh = 0;
			float charge = a > Ion.CARVE_END ? MathHelper.clamp((a - Ion.CARVE_END) / (Ion.IGNITE - Ion.CARVE_END), 0.0F, 1.0F) : 0.0F;
			float pulse = 0.75F + 0.25F * MathHelper.sin(a * 0.6F);
			for (double th = 0; th <= thNow; th += step / Math.max(0.3, th * 0.05 + 0.15)) {
				Vec3d p = spiral(th).add(off);
				if (prev != null) {
					float age2 = (float) (a - cutTick(th));
					float cool = MathHelper.clamp(age2 / 70.0F, 0.0F, 1.0F);
					int col = cool < 0.3F ? 0xFFFFFF : cool < 0.7F ? 0x90E0FF : 0x3A70D0;
					float al = (0.8F - 0.55F * cool) * (1 - charge) + charge * 0.85F * pulse;
					if (a >= Ion.IGNITE) {
						al *= fadeOut(a, Ion.IGNITE + 30, 40);
					}
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 1.6 + charge * 1.2, charge > 0 ? 0xA0E8FF : col, al, al);
					if (a >= Ion.IGNITE) {
						double rr = Math.hypot(p.x - (g.x - cam.x), p.z - (g.z - cam.z));
						float w = (float) (a - Ion.IGNITE - rr / 6.0);
						if (w > 0 && w < 50) {
							float h = (float) Math.min(1.0, w / 8.0) * 34.0F;
							float wa = 0.45F * fadeOut(w, 18, 32);
							Vec3d topA = prev.add(0, h, 0), topB = p.add(0, h, 0);
							Solids.quad(glow, m, prev, p, topB, topA, 0x90E0FF, wa * 0.6F);
							FxDraw.ribbon(glow, m, topA, topB, Vec3d.ZERO, 0.6, 0xFFFFFF, wa, wa);
						}
					}
				}
				prev = p;
				prevTh = th;
			}
		}
	}

	// ====================================================================== RESONANCE CASCADE

	/** The tuning fork, its pressure rings warping the air, the collapse front and its dust, the fork shattering. */
	static final class ResonanceFx extends FxEffect {
		private final Vec3d g;

		ResonanceFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age > Resonance.HUM && age < Resonance.CASCADE_END) {
				ClientFx.shake = Math.max(ClientFx.shake, (float) Resonance.hum(age) * 0.8F);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d c = g.subtract(cam);
			double hum = Resonance.hum(a);
			float alive = fadeOut(a, Resonance.SHATTER, 1);
			double rise = MathHelper.clamp((a - Resonance.EMIT) / 30.0, 0.0, 1.0);
			if (alive > 0) {
				// The fork: a stem and two tines that blur as they ring.
				double H = Resonance.FORK_H * rise;
				Vec3d stem = c.add(0, -Resonance.FORK_H * (1 - rise), 0);
				Solids.tube(solid, m, stem, stem.add(0, H * 0.35, 0), 1.6, 1.3, 10, 0x9AA4B0, 1.0F, true);
				Vec3d side = new Vec3d(1, 0, 0);
				for (int sg = -1; sg <= 1; sg += 2) {
					double buzz = Math.sin(a * 2.7) * hum * 0.6 * sg;
					Vec3d root = stem.add(0, H * 0.35, 0).add(side.multiply(sg * 1.6));
					Vec3d tip = stem.add(0, H, 0).add(side.multiply(sg * 2.2 + buzz));
					Solids.tube(solid, m, root, tip, 0.9, 0.7, 8, 0xC8D0DA, 1.0F, true);
					FxDraw.ribbon(glow, m, root, tip, Vec3d.ZERO, 2.2 * hum, 0xA0F0FF, 0.25F * (float) hum, 0.4F * (float) hum);
				}
			}
			// Pressure rings rolling out of the fork, rippled by the note.
			if (a > Resonance.HUM && a < Resonance.CASCADE_END) {
				Vec3d gc = c.add(0, 0.3, 0);
				for (int k = 0; k < 6; k++) {
					double rr = ((a - Resonance.HUM) * 2.2 + k * 18) % (Resonance.R + 10);
					float fa = (float) (0.35 * hum * (1 - rr / (Resonance.R + 10)));
					Vec3d prev = null;
					for (int i = 0; i <= 64; i++) {
						double ang = i * MathHelper.TAU / 64;
						double wob = rr + Math.sin(ang * 9 + a * 0.4) * 1.2 * hum;
						Vec3d p = gc.add(Math.cos(ang) * wob, Math.sin(ang * 5 + a * 0.3) * 0.6 * hum, Math.sin(ang) * wob);
						if (prev != null) {
							FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.5, 0xB0F4FF, fa, fa);
						}
						prev = p;
					}
				}
			}
			// The collapse front: a churning wall of dust where the buildings are coming down.
			if (a >= Resonance.CASCADE && a < Resonance.END) {
				double front = Resonance.front(a);
				float k = fadeOut(a, Resonance.CASCADE_END, 60);
				java.util.List<Effects.Puff> dust = FxKit.puffs();
				FxKit.dustRing(m, cb, c, Math.max(4, front - 3), 8, 0xA09484, 0x3A342C, 0.55F * k, 501, dust);
				FxKit.dustRing(m, cb, c, Math.max(2, front * 0.6), 14, 0x8A8070, 0x2A2620, 0.3F * k, 502, dust);
				FxKit.flush(dust, consumers, m, cb);
			}
			if (a >= Resonance.SHATTER && a < Resonance.SHATTER + 30) {
				float e = a - Resonance.SHATTER;
				FxKit.sparks(consumers, m, c.add(0, Resonance.FORK_H * 0.7, 0), 20, 80, 0xD0F0FF, e / 30.0F, fadeOut(e, 5, 25), 503);
			}
		}
	}

	// ====================================================================== TESLA DOMINION

	/** The lattice tower with its coils and crown torus, crawling arcs, the ball lightning, and the tower falling over. */
	static final class TeslaFx extends FxEffect {
		private final Vec3d g, dir;

		TeslaFx(Vec3d g, double yawDeg, int life) {
			super(Math.max(40, life));
			this.g = g;
			this.dir = Vec3d.fromPolar(0, (float) yawDeg);
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= Tesla.STORM && age < Tesla.DISCHARGE && age % 3 == 0) {
				ClientFx.light(Tesla.crown(g, age), 0.4F, 0x90D0FF);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d base = g.subtract(cam);
			double H = Tesla.TOWER_H;
			double up = Tesla.risen(a) * H;
			// Toppling: it leans, then goes over along its heading, hinged at the base.
			double tip = a > Tesla.FALL ? Math.min(1.0, Math.pow((a - Tesla.FALL) / 30.0, 2)) * Math.PI / 2 : 0;
			float gone = fadeOut(a, Tesla.END - 30, 30);
			if (gone <= 0) {
				return;
			}
			Vec3d hinge = new Vec3d(-dir.z, 0, dir.x);
			java.util.function.Function<Vec3d, Vec3d> T = local -> {
				Vec3d l = local.add(0, up - H, 0);
				double cs = Math.cos(tip), sn = Math.sin(tip);
				double along = l.dotProduct(dir), vert = l.y;
				Vec3d rest = l.subtract(dir.multiply(along)).subtract(0, vert, 0);
				return base.add(rest).add(dir.multiply(along * cs + vert * sn)).add(0, -along * sn + vert * cs, 0);
			};
			int steel = 0x3A3E48, copper = 0xB06A3A;
			Vec3d[] legBot = new Vec3d[4], legTop = new Vec3d[4];
			for (int i = 0; i < 4; i++) {
				double ang = i * Math.PI / 2 + Math.PI / 4;
				legBot[i] = new Vec3d(Math.cos(ang) * 8, 0, Math.sin(ang) * 8);
				legTop[i] = new Vec3d(Math.cos(ang) * 1.8, H, Math.sin(ang) * 1.8);
				Solids.tube(solid, m, T.apply(legBot[i]), T.apply(legTop[i]), 0.9, 0.5, 6, steel, 1.0F, true);
			}
			for (double y = 6; y < H - 4; y += 7) {
				double k = y / H;
				for (int i = 0; i < 4; i++) {
					int j = (i + 1) % 4;
					Vec3d p = legBot[i].add(legTop[i].subtract(legBot[i]).multiply(k));
					Vec3d q = legBot[j].add(legTop[j].subtract(legBot[j]).multiply(k + 7 / H));
					Vec3d q2 = legBot[j].add(legTop[j].subtract(legBot[j]).multiply(k));
					Solids.tube(solid, m, T.apply(p), T.apply(q), 0.3, 0.3, 4, steel, 1.0F, true);
					Solids.tube(solid, m, T.apply(p), T.apply(q2), 0.35, 0.35, 4, steel, 1.0F, true);
				}
			}
			Solids.tube(solid, m, T.apply(new Vec3d(0, 2, 0)), T.apply(new Vec3d(0, H, 0)), 1.6, 1.1, 10, copper, 1.0F, true);
			for (double y = 8; y < H - 6; y += 2.2) {
				double rr = 1.8 - y / H * 0.6;
				Solids.tube(solid, m, T.apply(new Vec3d(0, y, 0)), T.apply(new Vec3d(0, y + 1.2, 0)), rr + 0.3, rr + 0.3, 10, Solids.scale(copper, 0.85), 1.0F, true);
			}
			Vec3d crown = T.apply(new Vec3d(0, H + 6, 0));
			Vec3d cu = T.apply(new Vec3d(1, H + 6, 0)).subtract(crown), cv = T.apply(new Vec3d(0, H + 6, 1)).subtract(crown);
			float charged = (a >= Tesla.STORM ? 1.0F : fadeIn(a, Tesla.RISEN, 10)) * (a > Tesla.FALL ? fadeOut(a, Tesla.FALL, 20) : 1.0F);
			ring(solid, glow, m, crown, cu, cv, 6.5, 2.2, 24, 0x8A8E9A, 0x90D8FF, 0.6F * charged, 0);
			Solids.tube(solid, m, T.apply(new Vec3d(0, H, 0)), crown, 1.1, 0.6, 8, copper, 1.0F, true);
			FxKit.halo(consumers, m, cb, crown, 12, 0x80C8FF, 0.25F * charged);
			int roll = (int) (a * 0.5F);
			for (int k = 0; k < 6 && charged > 0.05F && a >= Tesla.RISEN; k++) {
				if (SciShapes.hash(k, roll) < 0.35) {
					continue;
				}
				double ang = SciShapes.hash(k, roll + 1) * MathHelper.TAU;
				Vec3d p = crown.add(cu.multiply(Math.cos(ang) * 6.5)).add(cv.multiply(Math.sin(ang) * 6.5));
				for (int j = 0; j < 5; j++) {
					Vec3d q = p.add((SciShapes.hash(k * 7 + j, roll) - 0.5) * 6, -1.5 - SciShapes.hash(j, k + roll) * 3, (SciShapes.hash(k * 5 + j, roll + 9) - 0.5) * 6);
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 0.25, j % 2 == 0 ? 0xFFFFFF : 0x9AD8FF, 0.8F * charged, 0.6F * charged);
					p = q;
				}
			}
			if (a >= Tesla.ORB && a < Tesla.DISCHARGE + 6) {
				float k = MathHelper.clamp((a - Tesla.ORB) / (Tesla.DISCHARGE - Tesla.ORB), 0.0F, 1.0F);
				double r = 2 + k * 11;
				Vec3d orb = crown.add(0, 4 + r * 0.6, 0);
				float boom = a > Tesla.DISCHARGE ? fadeOut(a, Tesla.DISCHARGE, 6) : 1.0F;
				FxDraw.sphere(glow, m, orb, r, Vec3d.ZERO, 0xE8F8FF, 0.7F * boom, 0.3F * boom, 14, 20);
				FxKit.halo(consumers, m, cb, orb, r * 2.4, 0x70B8FF, 0.3F * boom);
				for (int j = 0; j < 10; j++) {
					Vec3d d = Cinematics.sphereDir(j + (int) (a * 0.7F) * 13, 41);
					Vec3d p = orb.add(d.multiply(r));
					Vec3d q = p.add(d.multiply(r * 0.8)).add(cb[1].multiply(Math.sin(a + j) * 2));
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 0.35, 0xFFFFFF, 0.8F * boom, 0.0F);
				}
			}
			if (a >= Tesla.DISCHARGE && a < Tesla.DISCHARGE + 40) {
				float e = a - Tesla.DISCHARGE;
				FxKit.groundShock(consumers, m, base.add(0, 0.5, 0), 10 + e * 4, 0xA0D8FF, fadeOut(e, 0, 40));
			}
			if (a > Tesla.FALL + 26 && a < Tesla.FALL + 70) {
				// Where it lands: a line of dust kicked up along the fallen tower.
				java.util.List<Effects.Puff> dust = FxKit.puffs();
				float e = a - Tesla.FALL - 26;
				for (int i = 0; i < 12; i++) {
					Vec3d p = base.add(dir.multiply(6 + i * (H / 12))).add(0, 2 + e * 0.15, 0);
					dust.add(new Effects.Puff(p, 4 + e * 0.12, 0x8A8070, 0x2A2420, 0.6F * fadeOut(e, 10, 34)));
				}
				FxKit.flush(dust, consumers, m, cb);
			}
		}
	}

	// ====================================================================== DYSON LANCE

	/** The mirror swarm, its focus, the beam cutting the canyons with their cooling trails, and the drill. */
	static final class DysonFx extends FxEffect {
		private final Vec3d g;
		private final double yaw;

		DysonFx(Vec3d g, double yaw, int life) {
			super(Math.max(40, life));
			this.g = g;
			this.yaw = yaw;
		}

		private Vec3d focus() {
			return g.add(0, Dyson.MIRROR_ALT, 0);
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int k = 0; k < 3; k++) {
				double along = Dyson.along(k, age);
				if (!Double.isNaN(along)) {
					Vec3d ax = Dyson.axis(k, yaw);
					double x = g.x + ax.x * along, z = g.z + ax.z * along;
					ClientFx.light(new Vec3d(x, ground(x, z, g.y) + 3, z), 0.7F, 0xFFC080);
					for (int i = 0; i < 6; i++) {
						world.addParticle(ParticleTypes.LAVA, x + world.random.nextGaussian() * 3, ground(x, z, g.y) + 1, z + world.random.nextGaussian() * 3, 0, 0, 0);
					}
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw0, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d[] cb = FxDraw.cameraBasis(yaw0, pitch);
			Vec3d f = focus().subtract(cam);
			float on = fadeIn(a, Dyson.SWARM, 40) * fadeOut(a, Dyson.END - 30, 30);
			double focusK = MathHelper.clamp((a - Dyson.SWARM) / (Dyson.FOCUS - Dyson.SWARM), 0.0, 1.0);
			// The swarm: mirror panels on a slow ring, closing in on the focus.
			for (int i = 0; i < 180; i++) {
				if (SciShapes.hash(i, 1) > focusK * 1.4) {
					continue;
				}
				double ring = 60 + SciShapes.hash(i, 2) * 120 * (1.2 - focusK * 0.6);
				double ang = SciShapes.hash(i, 3) * MathHelper.TAU + a * 0.002 * (1 + SciShapes.hash(i, 4));
				double hh = (SciShapes.hash(i, 5) - 0.5) * 40 + 20;
				Vec3d p = f.add(Math.cos(ang) * ring, hh, Math.sin(ang) * ring);
				Vec3d toF = f.subtract(p).normalize();
				Vec3d u = toF.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(3.2);
				Vec3d v = u.crossProduct(toF).normalize().multiply(3.2);
				Solids.quad(solid, m, p.subtract(u).subtract(v), p.add(u).subtract(v), p.add(u).add(v), p.subtract(u).add(v), 0xC8A050, 1.0F);
				if (SciShapes.hash(i, 6) < 0.25) {
					Solids.dot(glow, m, p, cb, 1.6, 0xFFF0C0, 0.7F * on);
				}
			}
			FxKit.halo(consumers, m, cb, f, 10 + focusK * 16, 0xFFE0A0, (float) (0.5 * focusK) * on);
			// The beam and its cut: white-gold, a bright foot, and a trail along the canyon that cools to red.
			for (int k = 0; k < 3; k++) {
				Vec3d ax = Dyson.axis(k, yaw);
				double along = Dyson.along(k, a);
				double end = Double.isNaN(along) ? (a > Dyson.cutStart(k) ? Dyson.L : -Dyson.L - 1) : along;
				if (end < -Dyson.L) {
					continue;
				}
				Vec3d prev = null;
				for (double s2 = -Dyson.L; s2 <= end; s2 += 6) {
					double x = g.x + ax.x * s2, z = g.z + ax.z * s2;
					Vec3d p = new Vec3d(x, ground(x, z, g.y) + 0.4, z).subtract(cam);
					if (prev != null) {
						double cutAt = Dyson.cutStart(k) + (s2 + Dyson.L) / (2 * Dyson.L) * Dyson.CUT_LEN;
						float cool = MathHelper.clamp((float) ((a - cutAt) / 140.0), 0.0F, 1.0F);
						int col = cool < 0.25F ? 0xFFF0C0 : cool < 0.6F ? 0xFF9040 : 0xA02810;
						float al = (0.8F - 0.55F * cool) * on;
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 4.0 - cool * 2, col, al, al);
					}
					prev = p;
				}
				if (!Double.isNaN(along)) {
					double x = g.x + ax.x * along, z = g.z + ax.z * along;
					Vec3d hit = new Vec3d(x, ground(x, z, g.y), z).subtract(cam);
					FxDraw.beam(glow, m, f, hit, Vec3d.ZERO, 2.0, 0xFFFFFF, 0.85F);
					FxDraw.beam(glow, m, f, hit, Vec3d.ZERO, 6.0, 0xFFD080, 0.3F);
					FxDraw.beam(glow, m, f, hit, Vec3d.ZERO, 14.0, 0xFF9040, 0.08F);
					FxKit.flare(consumers, m, cb, hit.add(0, 2, 0), 22, 0xFFE0B0, 0.6F, a * 0.02);
					FxKit.fire(consumers, m, cb, hit.add(0, 3, 0), 7, 0xFFD090, (a * 0.06F) % 1.0F, 0.8F, a, 601 + k);
					FxKit.sparks(consumers, m, hit.add(0, 1, 0), 26, 50, 0xFFB060, (a * 0.05F) % 1.0F, 0.8F, 611 + k);
				}
			}
			if (a >= Dyson.DRILL && a < Dyson.END) {
				float k = fadeIn(a, Dyson.DRILL, 10) * fadeOut(a, Dyson.END - 20, 20);
				Vec3d hit = g.subtract(cam).add(0, -40, 0);
				FxDraw.beam(glow, m, f, hit, Vec3d.ZERO, 6.0 * k, 0xFFFFFF, 0.8F);
				FxDraw.beam(glow, m, f, hit, Vec3d.ZERO, 16.0 * k, 0xFFC070, 0.25F);
				FxKit.groundShock(consumers, m, g.subtract(cam).add(0, 0.5, 0), 20 + (a - Dyson.DRILL) * 0.4, 0xFFD090, 0.6F * k);
			}
		}
	}

	// ====================================================================== GRAVITY KILLSWITCH

	/** The generator, the land floating up as a slow storm of tumbling blocks, and the slam back down. */
	static final class GravityFx extends FxEffect {
		private static final int DEBRIS = 1100;
		private final Vec3d g;
		private final double[] gy = new double[DEBRIS];

		GravityFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
			java.util.Arrays.fill(gy, Double.NaN);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d c = g.subtract(cam).add(0, 5, 0);
			float alive = fadeOut(a, Gravity.SLAM, 4);
			if (alive > 0) {
				double sp = a * (0.03 + 0.06 * fadeIn(a, Gravity.HUM, 60));
				ring(solid, glow, m, c, new Vec3d(Math.cos(sp), 0, Math.sin(sp)), new Vec3d(0, 1, 0), 4.5, 0.4, 20, 0x4A4458, 0xB0A0FF, 0.6F * alive, sp);
				ring(solid, glow, m, c, new Vec3d(1, 0, 0), new Vec3d(0, Math.cos(-sp), Math.sin(-sp)), 6, 0.4, 24, 0x4A4458, 0x9080FF, 0.6F * alive, -sp);
				Solids.sphere(solid, m, c, 1.8, 0x0A0612, 10, 14, false);
				FxKit.halo(consumers, m, cb, c, 5, 0xA090FF, 0.4F * alive);
			}
			// Ten times more of the land than the server can carry floats up as phantom blocks over the real ones.
			if (a > Gravity.OFF && a < Gravity.SLAM + 16) {
				double rise = Gravity.rise(a) * (1 - Gravity.fall(a));
				double reach = Gravity.R * Math.min(1.0, (a - Gravity.OFF) / 90.0);
				for (int i = 0; i < DEBRIS; i++) {
					double r = Math.sqrt(SciShapes.hash(i, 1)) * Gravity.R;
					if (r > reach) {
						continue;
					}
					double ang = SciShapes.hash(i, 2) * MathHelper.TAU;
					double x = g.x + Math.cos(ang) * r, z = g.z + Math.sin(ang) * r;
					if (Double.isNaN(gy[i])) {
						gy[i] = ground(x, z, g.y);
					}
					double lag = SciShapes.hash(i, 3);
					double h = Gravity.CEIL * (0.15 + 0.85 * lag) * rise + Math.sin(a * 0.03 + i) * 1.5 * rise;
					Vec3d p = new Vec3d(x + Math.sin(a * 0.01 + i) * 2 * rise, gy[i] + 0.5 + h, z + Math.cos(a * 0.013 + i) * 2 * rise).subtract(cam);
					if (p.lengthSquared() > 260 * 260) {
						continue;
					}
					double spin = a * (0.02 + lag * 0.05) * rise;
					cube(solid, m, p, 0.5 + SciShapes.hash(i, 4) * 0.35, spin + i, spin * 0.7 + i * 2, EARTH[i % EARTH.length]);
				}
				FxDraw.sphere(glow, m, g.subtract(cam), Gravity.R, Vec3d.ZERO, 0x8070FF, 0.0F, 0.06F * alive, 16, 24);
			}
			if (a >= Gravity.SLAM && a < Gravity.SLAM + 60) {
				float e = a - Gravity.SLAM;
				Vec3d gc = g.subtract(cam).add(0, 0.5, 0);
				FxKit.groundShock(consumers, m, gc, 10 + e * 6, 0xC8C0B0, fadeOut(e, 10, 50));
				java.util.List<Effects.Puff> dust = FxKit.puffs();
				FxKit.dustRing(m, cb, gc, Math.min(Gravity.R, 10 + e * 5), 10, 0xA09888, 0x3A3630, 0.6F * fadeOut(e, 20, 40), 701, dust);
				FxKit.dustRing(m, cb, gc, Gravity.R * 0.5, 16, 0x8A8274, 0x2A2824, 0.45F * fadeOut(e, 20, 40), 702, dust);
				FxKit.flush(dust, consumers, m, cb);
			}
		}
	}

	// ====================================================================== SEISMIC TSUNAMI

	/** The piston hammer and the wave of rock: a curling crest of tumbling earth with spray behind it. */
	static final class WaveFx extends FxEffect {
		private final Vec3d g;

		WaveFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		/** How far down the ram is (0 hauled up, 1 on the ground): it drops fast on each blow and is winched back up. */
		private double ram(float a) {
			for (int i = 0; i < Wave.SLAMS; i++) {
				double d = a - Wave.slam(i);
				if (d > -6 && d <= 0) {
					double u = (d + 6) / 6.0;
					return u * u;
				}
				if (d > 0 && d < 18 && i < Wave.SLAMS - 1) {
					return 1 - d / 18.0;
				}
			}
			return a > Wave.slam(Wave.SLAMS - 1) ? 1.0 : 0.0;
		}

		@Override
		public boolean tick(ClientWorld world) {
			for (int i = 0; i < Wave.SLAMS; i++) {
				if (age == Wave.slam(i)) {
					ClientFx.shake = Math.max(ClientFx.shake, 2.0F + i);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d c = g.subtract(cam);
			float hammer = fadeIn(a, 0, 20) * fadeOut(a, Wave.WAVE + 120, 40);
			if (hammer > 0) {
				// The frame and the ram, hauled up between blows and dropped.
				for (int i = 0; i < 4; i++) {
					double ang = i * Math.PI / 2 + Math.PI / 4;
					Vec3d p = c.add(Math.cos(ang) * 9, 0, Math.sin(ang) * 9);
					Solids.tube(solid, m, p, p.add(0, 34, 0), 1.2, 1.0, 6, 0x3A3E48, 1.0F, true);
				}
				Solids.tube(solid, m, c.add(-9, 34, -9), c.add(9, 34, 9), 1.2, 1.2, 6, 0x3A3E48, 1.0F, true);
				Solids.tube(solid, m, c.add(-9, 34, 9), c.add(9, 34, -9), 1.2, 1.2, 6, 0x3A3E48, 1.0F, true);
				double y = 4 + (1 - ram(a)) * 20;
				Solids.tube(solid, m, c.add(0, y, 0), c.add(0, y + 10, 0), 5.0, 5.0, 14, 0x9AA2AE, 1.0F, true);
				Solids.tube(solid, m, c.add(0, y + 10, 0), c.add(0, 34, 0), 1.6, 1.6, 8, 0x6A707A, 1.0F, true);
			}
			if (a < Wave.WAVE || a > Wave.END) {
				return;
			}
			double front = Wave.front(a);
			double H = Wave.height(front);
			if (H < 0.5) {
				return;
			}
			// The crest: rows of tumbling earth stacked into a curling wall, throwing spray off the top.
			int segs = 300;
			java.util.List<Effects.Puff> spray = FxKit.puffs();
			for (int i = 0; i < segs; i++) {
				double ang = i * MathHelper.TAU / segs;
				double cx = Math.cos(ang), cz = Math.sin(ang);
				double gx0 = g.x + cx * front, gz0 = g.z + cz * front;
				if (new Vec3d(gx0 - cam.x, 0, gz0 - cam.z).lengthSquared() > 280 * 280) {
					continue;
				}
				double gy0 = ground(gx0, gz0, g.y);
				for (int j = 0; j < 6; j++) {
					double u = j / 5.0;
					double back = -u * 3 + u * u * 5;
					double h = H * u + Math.sin(a * 0.3 + i + j) * 0.4;
					double rr = front + back;
					Vec3d p = new Vec3d(g.x + cx * rr, gy0 + h, g.z + cz * rr).subtract(cam);
					double sz = 0.9 + SciShapes.hash(i, j) * 0.7;
					cube(solid, m, p, sz, a * 0.1 + i, a * 0.13 + j, EARTH[(i * 7 + j * 3) % EARTH.length]);
				}
				if (i % 10 == 0) {
					Vec3d top = new Vec3d(g.x + cx * (front + 1), gy0 + H + 1, g.z + cz * (front + 1)).subtract(cam);
					spray.add(new Effects.Puff(top, 3 + H * 0.15, 0xA09480, 0x3A3428, 0.5F));
				}
			}
			FxKit.dustRing(m, cb, c, Math.max(2, front - 6), 8, 0x9A8E7A, 0x3A3428, 0.45F, 801, spray);
			FxKit.flush(spray, consumers, m, cb);
		}
	}

	// ====================================================================== WEAPONS

	/** Helix Disruptor: two bolts corkscrewing down the line with fading spiral trails behind them. */
	static final class HelixFx extends FxEffect {
		private final Vec3d start, dir, u, v;
		private final double len, speed;

		HelixFx(Vec3d start, Vec3d dir, double len, double speed, int life) {
			super(Math.max(20, life));
			this.start = start;
			this.dir = dir.normalize();
			this.len = len;
			this.speed = speed;
			Vec3d up = Math.abs(this.dir.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
			this.u = this.dir.crossProduct(up).normalize();
			this.v = u.crossProduct(this.dir).normalize();
		}

		private Vec3d at(double s, int k) {
			double th = s * 0.32 + k * Math.PI;
			return start.add(dir.multiply(s)).add(u.multiply(Math.cos(th) * 2.4)).add(v.multiply(Math.sin(th) * 2.4));
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			double head = Math.min(len, a * speed);
			float gone = head >= len ? fadeOut(a, (float) (len / speed), 30) : 1.0F;
			for (int k = 0; k < 2; k++) {
				Vec3d prev = null;
				for (double s = Math.max(0, head - 40); s <= head; s += 0.6) {
					Vec3d p = at(s, k).subtract(cam);
					if (prev != null) {
						float f = (float) (1 - (head - s) / 40.0) * gone;
						FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.5 + f * 0.4, k == 0 ? 0x90F0FF : 0xC0A0FF, 0.8F * f, 0.8F * f);
					}
					prev = p;
				}
				if (head < len) {
					Vec3d h = at(head, k).subtract(cam);
					FxKit.halo(consumers, m, cb, h, 2.2, k == 0 ? 0x80E8FF : 0xB090FF, 0.7F);
					Solids.dot(glow, m, h, cb, 0.6, 0xFFFFFF, 0.9F);
				}
			}
			if (head < len) {
				FxDraw.beam(glow, m, start.add(dir.multiply(Math.max(0, head - 30))).subtract(cam), start.add(dir.multiply(head)).subtract(cam), Vec3d.ZERO, 0.3, 0xE0F8FF, 0.4F);
			}
		}
	}

	/** Graviton anchor: a black core in turning rings, the light bending into it in streaks. */
	static final class GravitonFx extends FxEffect {
		private final Vec3d c;

		GravitonFx(Vec3d c, int life) {
			super(Math.max(20, life));
			this.c = c;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d p = c.subtract(cam);
			float on = fadeIn(a, 0, 8) * fadeOut(a, 92, 12);
			double r = 1.2 + 1.8 * MathHelper.clamp(a / 90.0, 0.0, 1.0);
			Solids.sphere(solid, m, p, r, 0x050208, 10, 14, false);
			FxDraw.ring(glow, m, p, cb[0], cb[1], r * 1.05, r * 1.4, 0xD0B0FF, 0.0F, 0.8F * on, 0.0F, 40);
			FxDraw.ring(shade, m, p, cb[0], cb[1], r * 1.4, r * 4.0, 0x000000, 0.45F * on, 0.2F * on, 0.0F, 40);
			double sp = a * 0.15;
			ring(solid, glow, m, p, new Vec3d(Math.cos(sp), 0, Math.sin(sp)), new Vec3d(0, 1, 0), r * 3, 0.25, 18, 0x3A3448, 0xB090FF, 0.5F * on, sp);
			ring(solid, glow, m, p, new Vec3d(1, 0, 0), new Vec3d(0, Math.cos(-sp * 0.7), Math.sin(-sp * 0.7)), r * 3.8, 0.25, 22, 0x3A3448, 0x9070FF, 0.5F * on, -sp);
			for (int i = 0; i < 30; i++) {
				double life = (a * 0.05 + SciShapes.hash(i, 1)) % 1.0;
				Vec3d d = Cinematics.sphereDir(i, 91);
				double far = 18 * (1 - life);
				Vec3d q0 = p.add(d.multiply(far + 2)), q1 = p.add(d.multiply(far));
				FxDraw.ribbon(glow, m, q0, q1, Vec3d.ZERO, 0.2, 0xC8B0FF, 0.0F, 0.6F * on * (float) life);
			}
		}
	}

	/** Sunfall Mortar: a little sun on a high arc, trailing fire, and the heat of the pool where it lands. */
	static final class SunfallFx extends FxEffect {
		private final Vec3d from, to;
		private final int flight;

		SunfallFx(Vec3d from, Vec3d to, int flight, int life) {
			super(Math.max(20, life));
			this.from = from;
			this.to = to;
			this.flight = Math.max(10, flight);
		}

		private Vec3d arc(double t) {
			double u = MathHelper.clamp(t / flight, 0.0, 1.0);
			double h = Math.max(20, from.distanceTo(to) * 0.45);
			return from.lerp(to, u).add(0, Math.sin(u * Math.PI) * h, 0);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			if (a < flight) {
				Vec3d p = arc(a).subtract(cam);
				FxDraw.sphere(glow, m, p, 1.4, Vec3d.ZERO, 0xFFF0C0, 0.9F, 0.4F, 10, 14);
				FxKit.halo(consumers, m, cb, p, 4.5, 0xFFA040, 0.6F);
				FxKit.fire(consumers, m, cb, p, 1.8, 0xFFD080, (a * 0.1F) % 1.0F, 0.8F, a, 901);
				Vec3d prev = null;
				for (double t = Math.max(0, a - 12); t <= a; t += 1) {
					Vec3d q = arc(t).subtract(cam);
					if (prev != null) {
						float f = (float) (1 - (a - t) / 12.0);
						FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.9 * f, 0xFF8030, 0.6F * f, 0.6F * f);
					}
					prev = q;
				}
			} else {
				float e = a - flight;
				Vec3d g = to.subtract(cam);
				float k = fadeOut(e, 20, 70);
				FxKit.groundShock(consumers, m, g.add(0, 0.4, 0), 4 + e * 1.4, 0xFFB050, fadeOut(e, 0, 20));
				FxKit.halo(consumers, m, cb, g.add(0, 1, 0), 18, 0xFF6020, 0.35F * k);
				java.util.List<Effects.Puff> smoke = FxKit.puffs();
				FxKit.column(g, 10 + Math.min(e, 60) * 0.5, 4 + Math.min(e, 60) * 0.1, a, 0x6A5E56, 0x1C1816, 0.5F * k, 902, smoke);
				FxKit.flush(smoke, consumers, m, cb);
			}
		}
	}

	/** Halo Cutter: the target circle, the orbital beam drawing the ring, and the glowing cut it leaves. */
	static final class HaloFx extends FxEffect {
		private final Vec3d g;
		private final double yaw;
		private static final int CUT = 50, CUT_LEN = 120, DROP = 185;
		private static final double R = 34;

		HaloFx(Vec3d g, double yaw, int life) {
			super(Math.max(20, life));
			this.g = g;
			this.yaw = yaw;
		}

		private Vec3d onRing(double ang) {
			double x = g.x + Math.cos(ang) * R, z = g.z + Math.sin(ang) * R;
			return new Vec3d(x, ground(x, z, g.y) + 0.3, z);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw0, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw0, pitch);
			float mark = fadeIn(a, 0, 10) * fadeOut(a, CUT + CUT_LEN, 20);
			// The painted target: a red ring with ticks crawling round it.
			Vec3d prev = null;
			for (int i = 0; i <= 96; i++) {
				Vec3d p = onRing(i * MathHelper.TAU / 96).subtract(cam);
				if (prev != null) {
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.35, 0xFF4030, 0.7F * mark, 0.7F * mark);
				}
				prev = p;
			}
			for (int i = 0; i < 12; i++) {
				double ang = i * MathHelper.TAU / 12 + a * 0.02;
				Vec3d p0 = onRing(ang).subtract(cam);
				Vec3d p1 = g.add(Math.cos(ang) * (R - 4), 0, Math.sin(ang) * (R - 4));
				p1 = new Vec3d(p1.x, ground(p1.x, p1.z, g.y) + 0.3, p1.z).subtract(cam);
				FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 0.3, 0xFF6040, 0.6F * mark, 0.0F);
			}
			if (a < CUT || a > DROP + 40) {
				return;
			}
			double u = MathHelper.clamp((a - CUT) / CUT_LEN, 0.0, 1.0);
			double ang = yaw + u * MathHelper.TAU;
			// The cut so far, cooling from white to deep red behind the beam.
			prev = null;
			for (double s = 0; s <= u * MathHelper.TAU; s += 0.04) {
				Vec3d p = onRing(yaw + s).subtract(cam);
				if (prev != null) {
					float cool = (float) MathHelper.clamp((u * MathHelper.TAU - s) / 2.5, 0.0, 1.0);
					float al = (0.85F - 0.6F * cool) * fadeOut(a, DROP, 40);
					FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 2.4 - cool, cool < 0.3F ? 0xFFF0D0 : 0xFF6020, al, al);
				}
				prev = p;
			}
			if (u < 1) {
				Vec3d hit = onRing(ang).subtract(cam);
				Vec3d sky = g.add(0, 400, 0).subtract(cam);
				FxDraw.beam(glow, m, sky, hit, Vec3d.ZERO, 1.4, 0xFFFFFF, 0.85F);
				FxDraw.beam(glow, m, sky, hit, Vec3d.ZERO, 4.5, 0xFF7040, 0.25F);
				FxKit.flare(consumers, m, cb, hit.add(0, 1.5, 0), 14, 0xFFD0A0, 0.6F, a * 0.03);
				FxKit.sparks(consumers, m, hit.add(0, 1, 0), 16, 40, 0xFFA040, (a * 0.06F) % 1.0F, 0.8F, 911);
			}
		}
	}
}
