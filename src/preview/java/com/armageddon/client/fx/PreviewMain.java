package com.armageddon.client.fx;

import com.armageddon.client.render.FxBuffers;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.client.render.FxSprites;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Offline preview of the client effects and cutscenes: runs the real effect classes against a capturing vertex
 * provider and rasterises what they emit (with a stand-in terrain) in software, then prints each frame as a base64
 * JPEG into the build log. Lets the effects be judged without a GPU. Not part of the mod jar.
 *
 * <p>Scene lines ({@code preview.txt}): {@code name tick [dx dy dz [tx ty tz]]}, camera offset and look target
 * relative to the anchor; cutscenes use their own camera unless an offset is given.
 */
public final class PreviewMain {
	static final int W = 480, H = 270;
	static final Vec3d ANCHOR = new Vec3d(0.5, 64, 0.5);
	static final Vec3d HEADING = new Vec3d(0, 0, 1);
	static int[] atlas;
	static int atlasW, atlasH, frames;

	public static void main(String[] args) throws Exception {
		// Render layers reach into the block and sound registries: bootstrap them like data generation does.
		net.minecraft.SharedConstants.createGameVersion();
		net.minecraft.Bootstrap.initialize();
		BufferedImage at = ImageIO.read(new File(args[1]));
		atlasW = at.getWidth();
		atlasH = at.getHeight();
		atlas = at.getRGB(0, 0, atlasW, atlasH, null, 0, atlasW);
		for (String raw : Files.readAllLines(Path.of(args[0]))) {
			String line = raw.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			try {
				render(line);
			} catch (Throwable t) {
				System.out.println("PREVIEW-ERR " + line + " :: " + t);
				t.printStackTrace(System.out);
			}
		}
	}

	// ------------------------------------------------------------------ scenes

	static FxEffect make(String name) {
		Vec3d a = ANCHOR, h = HEADING;
		return switch (name) {
			case "spear" -> new MegaEffects.Spear(a, 92, 60, 120 + 24 + 60 + 50);
			case "galactic" -> Cinematics.create(Cinematics.GALACTIC_SMASH, a, h, 760);
			case "boom" -> Cinematics.create(Cinematics.BOOM_SURVEY, a, h, 400);
			case "alien" -> Cinematics.create(Cinematics.ALIEN_LAUNCH, a, h, 480);
			case "sapling" -> Cinematics.create(Cinematics.ETERNAL_SAPLING, a, h, 900);
			case "chunks" -> Cinematics.create(Cinematics.SIXTEEN_CHUNKS, a, h, 700);
			case "stage" -> new MainStageFx(payload(FxType.STAGE_SET, a, h, 0, 0, 0, 0, 0, 1400));
			case "termites" -> NewFx.create(payload(FxType.NEW_FX, a, a, 90, 0x6A4020, 4, 0, 0, 820));
			case "sun" -> NewFx.create(payload(FxType.NEW_FX, a, h, 64, 0xFFE0A0, 5, 0, 0, 1500));
			case "ice" -> NewFx.create(payload(FxType.NEW_FX, a, a, 24, 0x60C0FF, 3, 0, 0, 520));
			case "void" -> NewFx.create(payload(FxType.NEW_FX, a.add(0, 22, 0), a, 46, 0x9040FF, 2, 15, 0, 620));
			case "twister" -> new MegaEffects.Twister(a, h, 26, 0.35, 600);
			case "ufo" -> new MegaEffects.Ufo(a.add(-200, 70, 0), a.add(200, 70, 0), 60, 600);
			case "moon" -> new ArsenalEffects.Moon(a, 40, 500, 0xC8C8D0, 400);
			case "nuke" -> new Effects.MushroomCloud(a, 30);
			case "fireball" -> new Effects.Fireball(a.add(0, 6, 0), 12, 6, 60, 0xFFFFFF, 0xFF7020);
			default -> throw new IllegalArgumentException("unknown scene " + name);
		};
	}

	static FxPayload payload(int type, Vec3d a, Vec3d b, float scale, int color, int seed, float p1, float p2, int dur) {
		return new FxPayload(type, a.x, a.y, a.z, b.x, b.y, b.z, scale, color, seed, p1, p2, dur);
	}

	static void render(String line) throws Exception {
		String[] t = line.split("\\s+");
		String name = t[0];
		int tick = Integer.parseInt(t[1]);
		FxEffect e = make(name);
		for (int i = 0; i < tick; i++) {
			try {
				e.tick(null);
			} catch (Throwable ignored) {
				// Ticks that need a real world (particles, sounds) just stop early.
			}
			e.age = i + 1;
		}
		Vec3d cam;
		float yaw, pitch, fov = 70;
		if (t.length >= 5) {
			cam = ANCHOR.add(Double.parseDouble(t[2]), Double.parseDouble(t[3]), Double.parseDouble(t[4]));
			Vec3d target = t.length >= 8 ? ANCHOR.add(Double.parseDouble(t[5]), Double.parseDouble(t[6]), Double.parseDouble(t[7])) : ANCHOR.add(0, 10, 0);
			Vec3d d = target.subtract(cam).normalize();
			pitch = (float) Math.toDegrees(-Math.asin(d.y));
			yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		} else if (e instanceof Cinematics.Cine c) {
			Cinematics.State s = c.frame(0);
			cam = s.pos;
			yaw = s.yaw;
			pitch = s.pitch;
			if (s.fov > 0) {
				fov = s.fov;
			}
		} else {
			cam = ANCHOR.add(-70, 45, -70);
			Vec3d d = ANCHOR.add(0, 10, 0).subtract(cam).normalize();
			pitch = (float) Math.toDegrees(-Math.asin(d.y));
			yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		}
		Raster r = new Raster(cam, yaw, pitch, fov);
		boolean space = e instanceof Cinematics.Cine c && c.frame(0).space;
		r.sky(space);
		if (!space) {
			terrain(r, cam);
		}
		Capture cap = new Capture();
		FxBuffers.install(cap, cap.owner::get);
		e.render(new Matrix4f(), cap, cam, 0.0F, yaw, pitch);
		FxBuffers.install(null, null);
		cap.drawInto(r);
		emit(name + "@" + tick + (t.length >= 5 ? " cam" : ""), r.image());
	}

	/** A stand-in landscape: checkered fields and a scattering of blocky buildings, for scale. */
	static void terrain(Raster r, Vec3d cam) {
		Random rnd = new Random(7);
		double gy = ANCHOR.y;
		int cell = 16;
		for (int i = -24; i < 24; i++) {
			for (int j = -24; j < 24; j++) {
				int col = ((i + j) & 1) == 0 ? 0x5E8A3A : 0x6A9444;
				if (rnd.nextInt(9) == 0) {
					col = 0x8A7A58;
				}
				double x0 = ANCHOR.x + i * cell, z0 = ANCHOR.z + j * cell;
				r.quadOpaque(new Vec3d(x0, gy, z0).subtract(cam), new Vec3d(x0 + cell, gy, z0).subtract(cam),
						new Vec3d(x0 + cell, gy, z0 + cell).subtract(cam), new Vec3d(x0, gy, z0 + cell).subtract(cam), col);
			}
		}
		for (int k = 0; k < 60; k++) {
			double x = ANCHOR.x + (rnd.nextDouble() - 0.5) * 500, z = ANCHOR.z + (rnd.nextDouble() - 0.5) * 500;
			if (Math.hypot(x - ANCHOR.x, z - ANCHOR.z) < 30) {
				continue;
			}
			double w = 6 + rnd.nextDouble() * 10, hgt = 5 + rnd.nextDouble() * 25;
			r.box(new Vec3d(x, gy, z).subtract(cam), w, hgt, rnd.nextBoolean() ? 0x9A9690 : 0x8A6A4A);
		}
	}

	static void emit(String name, BufferedImage img) throws Exception {
		File dir = new File("build/fx-previews");
		dir.mkdirs();
		ImageIO.write(img, "jpg", new File(dir, String.format("%02d_%s.jpg", ++frames, name.replace(' ', '_').replace('@', '_'))));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "jpg", out);
		System.out.println("PREVIEW " + name + " " + out.size() + " bytes");
	}

	// ------------------------------------------------------------------ capture

	enum Mode { OPAQUE, ALPHA, ADD, ADD_TOP }

	/** Records quads per layer, in submission order. */
	static final class Capture implements VertexConsumerProvider {
		final Map<VertexConsumer, RenderLayer> owner = new IdentityHashMap<>();
		final List<Sink> sinks = new ArrayList<>();
		final Map<RenderLayer, Sink> byLayer = new IdentityHashMap<>();

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			Sink s = byLayer.computeIfAbsent(layer, l -> {
				Sink n = new Sink(mode(l), l == FxSprites.SMOKE_LAYER || l == FxSprites.GLOW_LAYER || l == FxSprites.GLOW_TOP_LAYER);
				sinks.add(n);
				return n;
			});
			owner.put(s, layer);
			return s;
		}

		static Mode mode(RenderLayer l) {
			if (l == FxRenderLayers.SOLID || l == FxRenderLayers.VOID) {
				return Mode.OPAQUE;
			}
			if (l == FxRenderLayers.GLOW || l == FxSprites.GLOW_LAYER) {
				return Mode.ADD;
			}
			if (l == FxRenderLayers.GLOW_NO_DEPTH || l == FxSprites.GLOW_TOP_LAYER) {
				return Mode.ADD_TOP;
			}
			return Mode.ALPHA;
		}

		void drawInto(Raster r) {
			for (Mode m : Mode.values()) {
				for (Sink s : sinks) {
					if (s.mode == m) {
						s.drawInto(r);
					}
				}
			}
		}
	}

	static final class Sink implements VertexConsumer {
		final Mode mode;
		final boolean textured;
		final List<float[]> verts = new ArrayList<>();
		float[] cur;

		Sink(Mode mode, boolean textured) {
			this.mode = mode;
			this.textured = textured;
		}

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			cur = new float[]{x, y, z, 1, 1, 1, 1, 0, 0};
			verts.add(cur);
			return this;
		}

		@Override
		public VertexConsumer color(int r, int g, int b, int a) {
			if (cur != null) {
				cur[3] = r / 255.0F;
				cur[4] = g / 255.0F;
				cur[5] = b / 255.0F;
				cur[6] = a / 255.0F;
			}
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			if (cur != null) {
				cur[7] = u;
				cur[8] = v;
			}
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		void drawInto(Raster r) {
			for (int i = 0; i + 3 < verts.size(); i += 4) {
				r.poly(new float[][]{verts.get(i), verts.get(i + 1), verts.get(i + 2), verts.get(i + 3)}, mode, textured);
			}
		}
	}

	// ------------------------------------------------------------------ software rasteriser

	static final class Raster {
		final float[] cr = new float[W * H], cg = new float[W * H], cb = new float[W * H], depth = new float[W * H];
		final Vec3d right, up, fwd;
		final double focal;

		Raster(Vec3d cam, float yaw, float pitch, float fov) {
			fwd = Vec3d.fromPolar(pitch, yaw);
			Vec3d r0 = fwd.crossProduct(new Vec3d(0, 1, 0));
			right = r0.lengthSquared() < 1.0E-6 ? new Vec3d(1, 0, 0) : r0.normalize();
			up = right.crossProduct(fwd).normalize();
			focal = (H / 2.0) / Math.tan(Math.toRadians(fov) / 2);
			java.util.Arrays.fill(depth, Float.MAX_VALUE);
		}

		void sky(boolean space) {
			for (int y = 0; y < H; y++) {
				float k = y / (float) H;
				for (int x = 0; x < W; x++) {
					int i = y * W + x;
					if (space) {
						cr[i] = 0.01F;
						cg[i] = 0.01F;
						cb[i] = 0.03F;
					} else {
						cr[i] = 0.45F + 0.25F * k;
						cg[i] = 0.62F + 0.18F * k;
						cb[i] = 0.95F;
					}
				}
			}
		}

		void quadOpaque(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int rgb) {
			float r = ((rgb >> 16) & 255) / 255.0F, g = ((rgb >> 8) & 255) / 255.0F, bl = (rgb & 255) / 255.0F;
			poly(new float[][]{v(a, r, g, bl), v(b, r, g, bl), v(c, r, g, bl), v(d, r, g, bl)}, Mode.OPAQUE, false);
		}

		static float[] v(Vec3d p, float r, float g, float b) {
			return new float[]{(float) p.x, (float) p.y, (float) p.z, r, g, b, 1, 0, 0};
		}

		void box(Vec3d base, double w, double h, int rgb) {
			double s = w / 2;
			Vec3d[] c = new Vec3d[8];
			for (int i = 0; i < 8; i++) {
				c[i] = base.add((i & 1) == 0 ? -s : s, (i & 2) == 0 ? 0 : h, (i & 4) == 0 ? -s : s);
			}
			int[][] f = {{2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
			double[] shade = {1.0, 0.75, 0.75, 0.6, 0.6};
			for (int k = 0; k < f.length; k++) {
				int col = scale(rgb, shade[k]);
				quadOpaque(c[f[k][0]], c[f[k][1]], c[f[k][2]], c[f[k][3]], col);
			}
		}

		static int scale(int rgb, double k) {
			return ((int) (((rgb >> 16) & 255) * k) << 16) | ((int) (((rgb >> 8) & 255) * k) << 8) | (int) ((rgb & 255) * k);
		}

		/** Clip a polygon of camera-relative vertices against the near plane, then fan-rasterise it. */
		void poly(float[][] in, Mode mode, boolean textured) {
			int n = in.length;
			double[][] vs = new double[n][];
			for (int i = 0; i < n; i++) {
				float[] p = in[i];
				Vec3d q = new Vec3d(p[0], p[1], p[2]);
				vs[i] = new double[]{q.dotProduct(right), q.dotProduct(up), q.dotProduct(fwd), p[3], p[4], p[5], p[6], p[7], p[8]};
			}
			List<double[]> out = new ArrayList<>();
			double near = 0.1;
			for (int i = 0; i < n; i++) {
				double[] a = vs[i], b = vs[(i + 1) % n];
				boolean ain = a[2] > near, bin = b[2] > near;
				if (ain) {
					out.add(a);
				}
				if (ain != bin) {
					double t = (near - a[2]) / (b[2] - a[2]);
					double[] c = new double[a.length];
					for (int k = 0; k < a.length; k++) {
						c[k] = a[k] + (b[k] - a[k]) * t;
					}
					out.add(c);
				}
			}
			for (int i = 1; i + 1 < out.size(); i++) {
				tri(out.get(0), out.get(i), out.get(i + 1), mode, textured);
			}
		}

		void tri(double[] a, double[] b, double[] c, Mode mode, boolean textured) {
			double[][] v = {a, b, c};
			double[] sx = new double[3], sy = new double[3], iz = new double[3];
			for (int i = 0; i < 3; i++) {
				iz[i] = 1 / v[i][2];
				sx[i] = W / 2.0 + v[i][0] * iz[i] * focal;
				sy[i] = H / 2.0 - v[i][1] * iz[i] * focal;
			}
			double area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0]);
			if (Math.abs(area) < 1.0E-9) {
				return;
			}
			int x0 = Math.max(0, (int) Math.floor(Math.min(sx[0], Math.min(sx[1], sx[2])))), x1 = Math.min(W - 1, (int) Math.ceil(Math.max(sx[0], Math.max(sx[1], sx[2]))));
			int y0 = Math.max(0, (int) Math.floor(Math.min(sy[0], Math.min(sy[1], sy[2])))), y1 = Math.min(H - 1, (int) Math.ceil(Math.max(sy[0], Math.max(sy[1], sy[2]))));
			if (x0 > x1 || y0 > y1 || (long) (x1 - x0) * (y1 - y0) > 4_000_000L) {
				return;
			}
			for (int y = y0; y <= y1; y++) {
				double py = y + 0.5;
				for (int x = x0; x <= x1; x++) {
					double px = x + 0.5;
					double w0 = ((sx[1] - px) * (sy[2] - py) - (sx[2] - px) * (sy[1] - py)) / area;
					double w1 = ((sx[2] - px) * (sy[0] - py) - (sx[0] - px) * (sy[2] - py)) / area;
					double w2 = 1 - w0 - w1;
					if (w0 < 0 || w1 < 0 || w2 < 0) {
						continue;
					}
					double zi = w0 * iz[0] + w1 * iz[1] + w2 * iz[2];
					float z = (float) (1 / zi);
					int i = y * W + x;
					if (mode != Mode.ADD_TOP && z > depth[i]) {
						continue;
					}
					double[] at = new double[6];
					for (int k = 0; k < 6; k++) {
						at[k] = (w0 * v[0][3 + k] * iz[0] + w1 * v[1][3 + k] * iz[1] + w2 * v[2][3 + k] * iz[2]) / zi;
					}
					double r = at[0], g = at[1], bl = at[2], al = at[3];
					if (textured) {
						int tx = Math.min(atlasW - 1, Math.max(0, (int) (at[4] * atlasW))), ty = Math.min(atlasH - 1, Math.max(0, (int) (at[5] * atlasH)));
						int tex = atlas[ty * atlasW + tx];
						double ta = ((tex >>> 24) & 255) / 255.0;
						r *= ((tex >> 16) & 255) / 255.0;
						g *= ((tex >> 8) & 255) / 255.0;
						bl *= (tex & 255) / 255.0;
						al *= ta;
					}
					switch (mode) {
						case OPAQUE -> {
							cr[i] = (float) r;
							cg[i] = (float) g;
							cb[i] = (float) bl;
							depth[i] = z;
						}
						case ALPHA -> {
							cr[i] = (float) (cr[i] * (1 - al) + r * al);
							cg[i] = (float) (cg[i] * (1 - al) + g * al);
							cb[i] = (float) (cb[i] * (1 - al) + bl * al);
						}
						default -> {
							cr[i] += (float) (r * al);
							cg[i] += (float) (g * al);
							cb[i] += (float) (bl * al);
						}
					}
				}
			}
		}

		BufferedImage image() {
			BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
			for (int i = 0; i < W * H; i++) {
				int r = tone(cr[i]), g = tone(cg[i]), b = tone(cb[i]);
				img.setRGB(i % W, i / W, (r << 16) | (g << 8) | b);
			}
			return img;
		}

		static int tone(float v) {
			double x = Math.max(0, v);
			x = x > 1 ? 1 - 0.25 * Math.exp(-(x - 1) * 2) * 0 : x;
			return (int) Math.min(255, Math.round(Math.min(1.0, x) * 255));
		}
	}

	private PreviewMain() {
	}
}
