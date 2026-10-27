package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.network.FxPayload;
import com.armageddon.util.SciShapes;
import com.armageddon.util.SciShapes.Horizon;
import com.armageddon.util.SciShapes.Inversion;
import com.armageddon.util.SciShapes.Ion;
import com.armageddon.util.SciShapes.Nanite;
import com.armageddon.util.SciShapes.Phase;
import com.armageddon.util.SciShapes.Tesla;
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
			case 41 -> new PhaseFx(a, p.duration());
			case 42 -> new TeslaFx(a, p.duration());
			case 43 -> new HorizonFx(a, p.duration());
			case 44 -> new NaniteFx(a, p.duration());
			case 45 -> new InversionFx(a, p.duration());
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
				Solids.sphere(solid, m, p, thick * 1.6, Solids.scale(metal, 0.6), 6, 8, true);
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

	// ====================================================================== PHASE SCRUBBER

	/** The hex-lattice dome, the descending scan plane, the floor pulse, and the dome breaking into shards. */
	static final class PhaseFx extends FxEffect {
		private final Vec3d g;

		PhaseFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		/** Point on the dome: azimuth {@code az}, elevation {@code el} (0 at the rim, PI/2 at the crown). */
		private Vec3d dome(double az, double el, double drop) {
			return g.add(Math.cos(az) * Math.cos(el) * Phase.R, Math.sin(el) * Phase.H + drop, Math.sin(az) * Math.cos(el) * Phase.R);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d off = cam.multiply(-1);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			if (a < Phase.DOME) {
				return;
			}
			double drop = a < Phase.DOME_UP ? Math.pow(1 - (a - Phase.DOME) / (Phase.DOME_UP - Phase.DOME), 2) * 160 : 0;
			float on = fadeIn(a, Phase.DOME, 20);
			int lat = 9, lon = 30;
			if (a < Phase.SHATTER) {
				// The field: staggered rows of hexagon-ish cells, drawn as their edges.
				for (int i = 0; i < lat; i++) {
					double el0 = i * (Math.PI / 2) / lat, el1 = (i + 1) * (Math.PI / 2) / lat;
					int n = Math.max(6, (int) (lon * Math.cos(el0)));
					for (int j = 0; j < n; j++) {
						double az0 = (j + (i % 2) * 0.5) * MathHelper.TAU / n, az1 = (j + 1 + (i % 2) * 0.5) * MathHelper.TAU / n;
						Vec3d p0 = dome(az0, el0, drop).add(off), p1 = dome(az1, el0, drop).add(off);
						Vec3d p2 = dome((az0 + az1) * 0.5, el1, drop).add(off);
						float flick = 0.35F + 0.15F * MathHelper.sin(a * 0.2F + i * 1.7F + j * 0.9F);
						FxDraw.ribbon(glow, m, p0, p1, Vec3d.ZERO, 0.35, 0x70D8FF, flick * on, flick * on);
						FxDraw.ribbon(glow, m, p0, p2, Vec3d.ZERO, 0.3, 0x60C0FF, flick * on * 0.7F, flick * on * 0.7F);
					}
				}
				FxDraw.sphere(glow, m, g.add(0, drop - Phase.H * 0.05, 0).add(off), Phase.R, Vec3d.ZERO, 0x3080FF, 0.0F, 0.12F * on, 18, 28);
			}
			// The scan plane: a disc of light with a hard bright rim and a grid ruled across it.
			if (a >= Phase.SCAN && a < Phase.SCAN_END + 10) {
				double py = Phase.plane(a);
				double r = Phase.radiusAt(py);
				Vec3d pc = g.add(0, py, 0).add(off);
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				float k = fadeIn(a, Phase.SCAN, 8) * fadeOut(a, Phase.SCAN_END, 10);
				FxDraw.disc(glow, m, pc, x, z, r, 0x60C8FF, 0.16F * k, 0.08F * k, 64);
				FxDraw.ring(glow, m, pc, x, z, r - 1.2, r, 0xD0F4FF, 0.0F, 0.8F * k, 0.0F, 96);
				for (double u = -r; u <= r; u += 6) {
					double h = Math.sqrt(Math.max(0, r * r - u * u));
					FxDraw.ribbon(glow, m, pc.add(u, 0, -h), pc.add(u, 0, h), Vec3d.ZERO, 0.18, 0xA0E0FF, 0.35F * k, 0.35F * k);
					FxDraw.ribbon(glow, m, pc.add(-h, 0, u), pc.add(h, 0, u), Vec3d.ZERO, 0.18, 0xA0E0FF, 0.35F * k, 0.35F * k);
				}
			}
			if (a >= Phase.PRINT && a < Phase.END) {
				double fr = (a - Phase.PRINT) * 2.5;
				if (fr < Phase.R) {
					Vec3d gc = g.add(0, 0.15, 0).add(off);
					FxDraw.ring(glow, m, gc, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Math.max(0, fr - 3), fr, 0xC0F0FF, 0.0F, 0.6F, 0.0F, 96);
				}
			}
			if (a >= Phase.SHATTER) {
				// The field breaks into hexagonal shards that tumble down and fade.
				float e = a - Phase.SHATTER;
				float fa = fadeOut(e, 40, 50);
				for (int i = 0; i < 160; i++) {
					double az = SciShapes.hash(i, 1) * MathHelper.TAU, el = SciShapes.hash(i, 2) * Math.PI / 2 * 0.95;
					Vec3d p0 = dome(az, el, 0);
					double delay = SciShapes.hash(i, 3) * 20;
					double t = Math.max(0, e - delay);
					Vec3d out = new Vec3d(p0.x - g.x, 0, p0.z - g.z).normalize();
					Vec3d p = p0.add(out.multiply(t * 0.15)).add(0, -0.03 * t * t, 0).add(off);
					double spin = t * 0.1 + i;
					Vec3d u = new Vec3d(Math.cos(spin), Math.sin(spin * 0.7) * 0.5, Math.sin(spin)).normalize();
					Vec3d v = u.crossProduct(new Vec3d(0.2, 1, 0.3)).normalize();
					Vec3d prev = null;
					for (int k = 0; k <= 6; k++) {
						double ang = k * MathHelper.TAU / 6;
						Vec3d q = p.add(u.multiply(Math.cos(ang) * 3.2)).add(v.multiply(Math.sin(ang) * 3.2));
						if (prev != null) {
							FxDraw.ribbon(glow, m, prev, q, Vec3d.ZERO, 0.3, 0x90E0FF, 0.6F * fa, 0.6F * fa);
						}
						prev = q;
					}
					FxDraw.disc(glow, m, p, u, v, 3.0, 0x60B0FF, 0.15F * fa, 0.05F * fa, 6);
				}
			}
		}
	}

	// ====================================================================== TESLA DOMINION

	/** The lattice tower with its coils and crown torus, crawling arcs, the ball lightning, and the tower's fall. */
	static final class TeslaFx extends FxEffect {
		private final Vec3d g;

		TeslaFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
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
			double up = Tesla.risen(a) * Tesla.TOWER_H;
			// It sinks back into the ground at the end, sparking.
			double sink = a > Tesla.FALL ? Math.pow((a - Tesla.FALL) / (double) (Tesla.END - Tesla.FALL), 2) * (Tesla.TOWER_H + 12) : 0;
			Vec3d b0 = base.add(0, up - Tesla.TOWER_H - sink, 0);
			double H = Tesla.TOWER_H;
			int steel = 0x3A3E48, copper = 0xB06A3A;
			// Four legs leaning in, braced every few blocks.
			Vec3d[] legBot = new Vec3d[4], legTop = new Vec3d[4];
			for (int i = 0; i < 4; i++) {
				double ang = i * Math.PI / 2 + Math.PI / 4;
				legBot[i] = b0.add(Math.cos(ang) * 8, 0, Math.sin(ang) * 8);
				legTop[i] = b0.add(Math.cos(ang) * 1.8, H, Math.sin(ang) * 1.8);
				Solids.tube(solid, m, legBot[i], legTop[i], 0.9, 0.5, 6, steel, 1.0F, true);
			}
			for (double y = 6; y < H - 4; y += 7) {
				double k = y / H;
				for (int i = 0; i < 4; i++) {
					Vec3d p = legBot[i].add(legTop[i].subtract(legBot[i]).multiply(k));
					Vec3d q = legBot[(i + 1) % 4].add(legTop[(i + 1) % 4].subtract(legBot[(i + 1) % 4]).multiply(k + 7 / H));
					Vec3d q2 = legBot[(i + 1) % 4].add(legTop[(i + 1) % 4].subtract(legBot[(i + 1) % 4]).multiply(k));
					Solids.tube(solid, m, p, q, 0.3, 0.3, 4, steel, 1.0F, true);
					Solids.tube(solid, m, p, q2, 0.35, 0.35, 4, steel, 1.0F, true);
				}
			}
			// Copper coils up the core, and the crown: a fat torus that glows when it is charged.
			Vec3d core0 = b0.add(0, 2, 0), core1 = b0.add(0, H, 0);
			Solids.tube(solid, m, core0, core1, 1.6, 1.1, 10, copper, 1.0F, true);
			for (double y = 8; y < H - 6; y += 2.2) {
				double rr = 1.8 - y / H * 0.6;
				Solids.tube(solid, m, b0.add(0, y, 0), b0.add(0, y + 1.2, 0), rr + 0.3, rr + 0.3, 10, Solids.scale(copper, 0.85), 1.0F, true);
			}
			Vec3d crown = b0.add(0, H + 6, 0);
			float charged = a >= Tesla.STORM ? 1.0F : fadeIn(a, Tesla.RISEN, 10);
			ring(solid, glow, m, crown, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 6.5, 2.2, 24, 0x8A8E9A, 0x90D8FF, 0.6F * charged, 0);
			Solids.tube(solid, m, core1, crown, 1.1, 0.6, 8, copper, 1.0F, true);
			FxKit.halo(consumers, m, cb, crown, 12, 0x80C8FF, 0.25F * charged);
			// Arcs crawling around the crown and down the legs, re-rolled a few times a second.
			int roll = (int) (a * 0.5F);
			for (int k = 0; k < 6; k++) {
				if (SciShapes.hash(k, roll) < 0.35 || a < Tesla.RISEN) {
					continue;
				}
				double ang = SciShapes.hash(k, roll + 1) * MathHelper.TAU;
				Vec3d p = crown.add(Math.cos(ang) * 6.5, 0, Math.sin(ang) * 6.5);
				for (int j = 0; j < 5; j++) {
					Vec3d q = p.add((SciShapes.hash(k * 7 + j, roll) - 0.5) * 6, -1.5 - SciShapes.hash(j, k + roll) * 3, (SciShapes.hash(k * 5 + j, roll + 9) - 0.5) * 6);
					FxDraw.ribbon(glow, m, p, q, Vec3d.ZERO, 0.25, j % 2 == 0 ? 0xFFFFFF : 0x9AD8FF, 0.8F, 0.6F);
					p = q;
				}
			}
			// Ball lightning: it swells on the crown, ringed in arcs, until it goes.
			if (a >= Tesla.ORB && a < Tesla.DISCHARGE + 6) {
				float k = MathHelper.clamp((a - Tesla.ORB) / (Tesla.DISCHARGE - Tesla.ORB), 0.0F, 1.0F);
				double r = 2 + k * 11;
				Vec3d orb = crown.add(0, 4 + r * 0.6, 0);
				float boom = a > Tesla.DISCHARGE ? fadeOut(a, Tesla.DISCHARGE, 6) : 1.0F;
				FxDraw.sphere(glow, m, orb, r, Vec3d.ZERO, 0xE8F8FF, 0.75F * boom, 0.3F * boom, 14, 20);
				FxKit.halo(consumers, m, cb, orb, r * 2.4, 0x70B8FF, 0.35F * boom);
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
			if (sink > 0 && a < Tesla.END) {
				FxKit.sparks(consumers, m, base.add(0, 2, 0), 18, 60, 0x9AD8FF, (a * 0.05F) % 1.0F, 0.7F, 402);
			}
		}
	}

	// ====================================================================== EVENT HORIZON ENGINE

	/** Three spinning rings, the black hole with its photon ring and lensing halo, the accretion disk, the collapse. */
	static final class HorizonFx extends FxEffect {
		private final Vec3d s;

		HorizonFx(Vec3d s, int life) {
			super(Math.max(40, life));
			this.s = s;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age >= Horizon.OPEN && age < Horizon.GONE && age % 3 == 0) {
				ClientFx.light(s, 0.5F, 0xFFB060);
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			VertexConsumer shade = consumers.getBuffer(FxRenderLayers.SHADE);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d c = s.subtract(cam);
			double spin = 0;
			for (int i = Horizon.OPEN; i < Math.min(a, Horizon.COLLAPSE); i += 4) {
				spin += Horizon.spin(i) * 4;
			}
			// Assembly: the rings fly in from scattered pieces; at the collapse they shrink into the hole.
			double gather = MathHelper.clamp((a - Horizon.ASSEMBLE) / (Horizon.OPEN - Horizon.ASSEMBLE), 0.0, 1.0);
			gather = 1 - Math.pow(1 - gather, 3);
			double shrink = a > Horizon.COLLAPSE ? MathHelper.clamp((a - Horizon.COLLAPSE) / (Horizon.GONE - Horizon.COLLAPSE), 0.0, 1.0) : 0;
			if (a >= Horizon.ASSEMBLE && a < Horizon.GONE) {
				double[] rr = {Horizon.RING_R, Horizon.RING_R * 0.8, Horizon.RING_R * 0.62};
				for (int k = 0; k < 3; k++) {
					double tilt = (k - 1) * 0.6 + spin * (k == 1 ? 0.3 : 0.1) * (k == 2 ? -1 : 1);
					Vec3d u = new Vec3d(1, 0, 0);
					Vec3d v = new Vec3d(0, Math.sin(tilt), Math.cos(tilt));
					if (k == 1) {
						u = new Vec3d(Math.cos(tilt), Math.sin(tilt), 0);
						v = new Vec3d(0, 0, 1);
					}
					double r = rr[k] * (1 - shrink) * (0.3 + 0.7 * gather) + 0.5;
					Vec3d scatter = Cinematics.sphereDir(k, 77).multiply((1 - gather) * 120);
					ring(solid, glow, m, c.add(scatter), u, v, r, 1.3 - shrink, 36, 0x5A5E6A, 0xFFB060,
							(float) (0.3 + 0.5 * gather) * (float) (1 - shrink), spin * (k + 1) * (k == 1 ? -1 : 1));
				}
			}
			if (a < Horizon.OPEN || a >= Horizon.GONE + 40) {
				return;
			}
			float open = fadeIn(a, Horizon.OPEN, 20);
			double hr = 5.5 * open * (a > Horizon.COLLAPSE ? 1 + shrink * 1.5 : 1);
			float alive = a < Horizon.GONE ? 1.0F : fadeOut(a, Horizon.GONE, 40);
			if (a < Horizon.GONE) {
				// The hole: pure black, a thin blazing photon ring, and a dark lensing halo around it.
				Solids.sphere(solid, m, c, hr, 0x000000, 14, 20, false);
				FxDraw.ring(glow, m, c, cb[0], cb[1], hr * 1.05, hr * 1.35, 0xFFE0B0, 0.0F, 0.9F * open, 0.0F, 64);
				FxDraw.ring(shade, m, c, cb[0], cb[1], hr * 1.35, hr * 3.2, 0x000000, 0.55F * open, 0.25F * open, 0.0F, 64);
				// The accretion disk: a flat whirl of hot matter, white at the inner edge, red at the outside.
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				float feed = fadeIn(a, Horizon.FEED, 60) * (a > Horizon.COLLAPSE ? (float) (1 - shrink) : 1.0F);
				FxDraw.ring(glow, m, c, x, z, hr * 1.6, hr * 3.0, 0xFFF0D0, 0.0F, 0.6F * feed, 0.2F * feed, 72);
				FxDraw.ring(glow, m, c, x, z, hr * 3.0, hr * 7.5, 0xFF9040, 0.25F * feed, 0.3F * feed, 0.0F, 72);
				FxDraw.ring(glow, m, c, x, z, hr * 7.5, hr * 13, 0xB03010, 0.12F * feed, 0.08F * feed, 0.0F, 72);
				for (int i = 0; i < 90; i++) {
					double rad = hr * (1.8 + SciShapes.hash(i, 5) * 10);
					double ang = SciShapes.hash(i, 6) * MathHelper.TAU + a * (0.04 + 0.6 / rad) * (1 + spin * 0.05);
					Vec3d p = c.add(Math.cos(ang) * rad, (SciShapes.hash(i, 7) - 0.5) * 1.2, Math.sin(ang) * rad);
					Solids.dot(glow, m, p, cb, 0.6 + SciShapes.hash(i, 8) * 0.8, rad < hr * 4 ? 0xFFE8C0 : 0xFF8A40, 0.7F * feed);
				}
				if (a > Horizon.COLLAPSE) {
					// Relativistic jets out of both poles as it swallows the last of it.
					float j = (float) Math.sin(shrink * Math.PI);
					FxDraw.beam(glow, m, c, c.add(0, 260, 0), Vec3d.ZERO, 3.0 * j, 0xC0D8FF, 0.6F);
					FxDraw.beam(glow, m, c, c.add(0, -Horizon.ALT, 0), Vec3d.ZERO, 3.0 * j, 0xC0D8FF, 0.6F);
				}
			} else {
				// It closes: one ring of light running out over the land.
				float e = a - Horizon.GONE;
				Vec3d gnd = c.add(0, -Horizon.ALT + 1, 0);
				FxKit.groundShock(consumers, m, gnd, 20 + e * 9, 0xFFD8A0, alive);
				FxKit.shock(consumers, m, c, cb[0], cb[1], 10 + e * 6, 0xFFFFFF, alive * 0.7F);
			}
		}
	}

	// ====================================================================== NANITE BLOOM

	/** The canister, its release, a shimmer of nanites over the spreading grey, and the hex light pulse at the end. */
	static final class NaniteFx extends FxEffect {
		private final Vec3d g;

		NaniteFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d c = g.subtract(cam);
			if (a < Nanite.RELEASE + 10) {
				// The canister: a steel cylinder with lit bands, cracking open.
				float crack = fadeIn(a, Nanite.RELEASE - 10, 10);
				Solids.tube(solid, m, c, c.add(0, 2.4, 0), 0.7, 0.7, 10, 0x8A9098, 1.0F, true);
				for (double y = 0.4; y < 2.2; y += 0.6) {
					FxDraw.ring(glow, m, c.add(0, y, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 0.72, 0.8, 0x70E0FF, 0.8F, 0.8F, 0.8F, 20);
				}
				FxKit.halo(consumers, m, cb, c.add(0, 1.2, 0), 2 + crack * 4, 0x70E0FF, 0.3F + crack * 0.4F);
			}
			// Nanite shimmer: tiny metallic glints drifting low over the area the bloom has reached.
			double reach = a < Nanite.FILL ? Math.min(Nanite.R, (a - Nanite.GROW) * 0.35) : Math.max(Nanite.fill(a), Math.min(Nanite.R, (a - Nanite.GROW) * 0.35));
			if (a > Nanite.RELEASE && a < Nanite.PULSE + 60 && reach > 1) {
				int n = 260;
				for (int i = 0; i < n; i++) {
					double r = Math.sqrt(SciShapes.hash(i, 11)) * reach, ang = SciShapes.hash(i, 12) * MathHelper.TAU + a * 0.002;
					double x = g.x + Math.cos(ang) * r, z = g.z + Math.sin(ang) * r;
					double life = (a * 0.03 + SciShapes.hash(i, 13)) % 1.0;
					Vec3d p = new Vec3d(x, ground(x, z, g.y) + 0.6 + life * 2.5, z).subtract(cam);
					float tw = (float) Math.sin(life * Math.PI);
					Solids.dot(glow, m, p, cb, 0.18, SciShapes.hash(i, 14) < 0.3 ? 0x80F0FF : 0xE0E8F0, 0.8F * tw);
				}
			}
			// The pulse: a front of light running out along the hex seams of the new plain.
			if (a >= Nanite.PULSE && a < Nanite.END) {
				double fr = (a - Nanite.PULSE) * 3.0;
				float k = fadeOut(a, Nanite.END - 30, 30);
				Vec3d gc = c.add(0, 0.12, 0);
				FxDraw.ring(glow, m, gc, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), Math.max(0, fr - 4), fr, 0xA0F4FF, 0.0F, 0.7F * k, 0.0F, 120);
				double size = 6.0;
				for (int q = -14; q <= 14; q++) {
					for (int rr = -14; rr <= 14; rr++) {
						double hx = size * Math.sqrt(3) * (q + rr / 2.0), hz = size * 1.5 * rr;
						double d = Math.hypot(hx, hz);
						if (Math.abs(d - fr) > 10 || d > Nanite.R) {
							continue;
						}
						float w = (float) (1 - Math.abs(d - fr) / 10) * k;
						Vec3d hc = gc.add(hx, 0, hz);
						Vec3d prev = null;
						for (int s2 = 0; s2 <= 6; s2++) {
							double ang = s2 * Math.PI / 3 + Math.PI / 6;
							Vec3d p = hc.add(Math.cos(ang) * size, 0, Math.sin(ang) * size);
							if (prev != null) {
								FxDraw.ribbon(glow, m, prev, p, Vec3d.ZERO, 0.3, 0x90F0FF, 0.7F * w, 0.7F * w);
							}
							prev = p;
						}
					}
				}
			}
		}
	}

	// ====================================================================== INVERSION ENGINE

	/** The gyroscope, the mirror plane between pit and island, the streams of light carrying the world up, the break. */
	static final class InversionFx extends FxEffect {
		private final Vec3d g;

		InversionFx(Vec3d g, int life) {
			super(Math.max(40, life));
			this.g = g;
		}

		@Override
		public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
			float a = age + td;
			VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
			VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
			Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
			Vec3d mid = g.add(0, Inversion.HEIGHT * 0.5, 0).subtract(cam);
			float on = fadeIn(a, 0, 30) * (a < Inversion.SHATTER ? 1.0F : fadeOut(a, Inversion.SHATTER, 20));
			if (on > 0) {
				// The engine: two counter-rotating rings around a white core, hanging at the mirror.
				double sp = a * (0.03 + 0.08 * fadeIn(a, Inversion.CHARGE, 70));
				ring(solid, glow, m, mid, new Vec3d(Math.cos(sp), 0, Math.sin(sp)), new Vec3d(0, 1, 0), 9, 0.8, 28, 0x6A6E7A, 0xC0A0FF, 0.6F * on, sp);
				ring(solid, glow, m, mid, new Vec3d(1, 0, 0), new Vec3d(0, Math.cos(-sp), Math.sin(-sp)), 12, 0.8, 32, 0x6A6E7A, 0xA0C0FF, 0.6F * on, -sp);
				FxKit.halo(consumers, m, cb, mid, 6, 0xE0D0FF, 0.5F * on);
				Solids.dot(glow, m, mid, cb, 1.6, 0xFFFFFF, 0.9F * on);
			}
			// The mirror: a disc of rippling light the world passes through on its way up.
			if (a >= Inversion.LIFT - 20 && a < Inversion.SHATTER + 10) {
				float k = fadeIn(a, Inversion.LIFT - 20, 20) * fadeOut(a, Inversion.SHATTER, 10);
				Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);
				double r = Inversion.R + 6;
				FxDraw.disc(glow, m, mid, x, z, r, 0x9080FF, 0.12F * k, 0.05F * k, 64);
				FxDraw.ring(glow, m, mid, x, z, r - 0.8, r, 0xE0D8FF, 0.0F, 0.8F * k, 0.0F, 96);
				for (int i = 0; i < 4; i++) {
					double rr = ((a * 0.6 + i * r / 4) % r);
					FxDraw.ring(glow, m, mid, x, z, rr, rr + 0.6, 0xC0B0FF, 0.0F, 0.35F * k * (float) (1 - rr / r), 0.0F, 72);
				}
			}
			// Streams of light carrying the world up through the mirror while the transfer runs.
			if (a >= Inversion.LIFT && a < Inversion.LIFT_END + 10) {
				float k = fadeOut(a, Inversion.LIFT_END, 10);
				for (int i = 0; i < 70; i++) {
					double r = Math.sqrt(SciShapes.hash(i, 21)) * Inversion.R, ang = SciShapes.hash(i, 22) * MathHelper.TAU;
					double life = (a * 0.025 + SciShapes.hash(i, 23)) % 1.0;
					double y0 = -Inversion.R * 0.3 + life * (Inversion.HEIGHT + Inversion.R * 0.3);
					Vec3d p = g.add(Math.cos(ang) * r, y0, Math.sin(ang) * r).subtract(cam);
					FxDraw.ribbon(glow, m, p, p.add(0, 6, 0), Vec3d.ZERO, 0.3, i % 3 == 0 ? 0xFFFFFF : 0xB0A0FF, 0.0F, 0.6F * k * (float) Math.sin(life * Math.PI));
				}
			}
			if (a >= Inversion.SHATTER && a < Inversion.SHATTER + 50) {
				float e = a - Inversion.SHATTER;
				Vec3d isl = g.add(0, Inversion.HEIGHT, 0).subtract(cam);
				FxKit.shock(consumers, m, isl, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 20 + e * 6, 0xD0C0FF, fadeOut(e, 0, 50));
				FxKit.sparks(consumers, m, isl, 60, 120, 0xC0B0FF, e / 50.0F, fadeOut(e, 10, 40), 403);
			}
		}
	}
}
