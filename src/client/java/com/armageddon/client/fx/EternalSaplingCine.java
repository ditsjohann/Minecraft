package com.armageddon.client.fx;

import com.armageddon.util.EternalTree;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import static com.armageddon.client.fx.Cinematics.aim;
import static com.armageddon.client.fx.Cinematics.cap;
import static com.armageddon.client.fx.Cinematics.clamp01;
import static com.armageddon.client.fx.Cinematics.dip;
import static com.armageddon.client.fx.Cinematics.ease;
import static com.armageddon.client.fx.Cinematics.lerp;

/**
 * ETERNAL SAPLING cutscene. There is no scenery of its own: the server grows the tree out of real blocks on
 * the {@link EternalTree} timeline and the camera films it, from a macro shot of the sapling to the roots
 * slamming down around the world tree. Ground-level shots stay near the anchor, where terrain is loaded.
 */
public class EternalSaplingCine extends Cinematics.Cine {
	private static final int MACRO = 60, SMALL = 130, CUT_WORLD = 265, CRANE = 400, CUT_ROOTS = 525, CLOSE = 600, BACK = 655, HERO = 700;
	private final Vec3d base;
	private final double s;
	private final EternalTreeFx tree;
	/** The root whose landing gets the close-up. */
	private final int hero;

	public EternalSaplingCine(Vec3d anchor, Vec3d heading, int life) {
		super(anchor, heading, life);
		this.tree = EternalTreeFx.near(anchor);
		this.base = tree != null ? tree.base : anchor.add(0, -0.5, 0);
		this.s = tree != null ? tree.s : 1.0;
		int best = 0;
		for (int i = 0; i < EternalTree.ROOTS; i++) {
			if (Math.abs(EternalTree.rootSlam(i) - 628) < Math.abs(EternalTree.rootSlam(best) - 628)) {
				best = i;
			}
		}
		this.hero = best;
	}

	private double top() {
		return EternalTree.height(s);
	}

	private Vec3d orbit(double ang, double dist, double y) {
		Vec3d d = fwd.multiply(-Math.cos(ang)).add(right.multiply(Math.sin(ang)));
		return base.add(d.multiply(dist)).add(0, y, 0);
	}

	private double rootGround(int i) {
		return tree != null ? tree.rootGround(net.minecraft.client.MinecraftClient.getInstance().world, i) : base.y;
	}

	@Override
	protected void direct(float a, Cinematics.State s0) {
		if (a < MACRO) {
			// Macro: down at sapling height, circling it.
			aim(s0, orbit(0.4 + a * 0.012, 3.2, 0.7), base.add(0, 0.45, 0), 45, false);
		} else if (a < SMALL) {
			double u = ease((a - MACRO) / (double) (SMALL - MACRO));
			aim(s0, orbit(1.1 + a * 0.006, lerp(4, 13, u), lerp(1, 5, u)), base.add(0, lerp(0.6, 4, u), 0), (float) lerp(48, 62, u), false);
		} else if (a < CUT_WORLD) {
			double u = ease((a - SMALL) / (double) (CUT_WORLD - SMALL));
			aim(s0, orbit(1.6 + a * 0.004, lerp(18, 42, u), lerp(3, 12, u)), base.add(0, lerp(6, 18, u), 0), 66, false);
		} else if (a < CRANE) {
			// Worm's-eye at the foot of the world tree, tilting up with the growth front.
			double front = EternalTree.front(s, a);
			Vec3d p = base.add(fwd.multiply(-(36 * Math.max(0.6, s) + 8))).add(right.multiply(12)).add(0, 2, 0);
			aim(s0, p, base.add(0, Math.max(10, front * 0.85 + 6), 0), 82, false);
		} else if (a < CUT_ROOTS) {
			double u = ease((a - CRANE) / (double) (CUT_ROOTS - CRANE));
			double front = Math.min(top(), EternalTree.front(s, a));
			aim(s0, orbit(-0.6 + u * 1.3, 70 * s + 22, lerp(25, top() * 1.15, u)), base.add(0, front * 0.75, 0), 70, false);
		} else if (a < CLOSE) {
			// Aerial: every root slamming down around the tree.
			double u = (a - CUT_ROOTS) / (double) (CLOSE - CUT_ROOTS);
			aim(s0, orbit(0.25 + u * 0.15, 90, 150 * s + 25), base.add(0, 15, 0), 80, false);
		} else if (a < BACK) {
			// Down on the ground as one root comes down right in front of the camera.
			Vec3d tip = EternalTree.rootTip(base, hero, s);
			double gy = rootGround(hero);
			Vec3d tipG = new Vec3d(tip.x, gy, tip.z);
			Vec3d toBase = base.subtract(tip).multiply(1, 0, 1).normalize();
			Vec3d side = new Vec3d(-toBase.z, 0, toBase.x);
			double u = (a - CLOSE) / (double) (BACK - CLOSE);
			aim(s0, tipG.add(toBase.multiply(44 - u * 6)).add(side.multiply(14)).add(0, 4, 0), tipG.add(0, lerp(30, 6, ease(u * 1.6)), 0), 74, false);
		} else if (a < HERO) {
			double u = ease((a - BACK) / (double) (HERO - BACK));
			aim(s0, orbit(0.5 + u * 0.2, 95, lerp(150 * s + 25, 190 * s + 30, u)), base.add(0, 20, 0), 78, false);
		} else {
			double u = (a - HERO) / (double) (lifetime - HERO);
			aim(s0, orbit(2.2 + u * 0.35, 96, 38), base.add(0, top() * 0.62, 0), 72, false);
		}
	}

	@Override
	protected float handheld(float a) {
		if (a < SMALL) {
			return 0.1F;
		}
		if (a >= CUT_WORLD && a < CUT_ROOTS) {
			return 0.35F + 0.4F * (float) clamp01((a - CUT_WORLD) / 200.0);
		}
		if (a >= CUT_ROOTS && a < HERO) {
			for (int i = 0; i < EternalTree.ROOTS; i++) {
				float d = a - EternalTree.rootSlam(i);
				if (d >= 0 && d < 8) {
					return 1.6F - d * 0.15F;
				}
			}
			return 0.45F;
		}
		return 0.2F;
	}

	@Override
	public float fade(float a) {
		return Math.max(super.fade(a), Math.max(Math.max(dip(a, CUT_WORLD, 6), dip(a, CUT_ROOTS, 7)), Math.max(dip(a, CLOSE, 5), dip(a, BACK, 5))));
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age < CUT_WORLD) {
			ClientFx.light(base.add(0, Math.max(1, tree != null ? tree.height(age) : 1), 0), 0.5F, 0xA0FF90);
		} else if (age < CUT_ROOTS) {
			ClientFx.light(base.add(0, Math.min(top(), EternalTree.front(s, age)) + 6, 0), 0.75F, 0xC8FF90);
		} else {
			for (int i = 0; i < EternalTree.ROOTS; i++) {
				if (age == EternalTree.rootSlam(i)) {
					Vec3d tip = EternalTree.rootTip(base, i, s);
					ClientFx.light(new Vec3d(tip.x, rootGround(i) + 4, tip.z), 1.0F, 0xFFD8A0);
				}
			}
			ClientFx.light(base.add(0, top() + 6, 0), 0.5F, 0xC8FF90);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
	}
}
