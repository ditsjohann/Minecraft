package com.armageddon.client.fx;

import com.armageddon.client.render.FxDraw;
import com.armageddon.client.render.FxRenderLayers;
import com.armageddon.util.WonderShapes.Havoc;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Firework Havoc's giant rocket: a striped paper body turning slowly as it climbs, a gold nose cone, four fins, a
 * long guide stick trailing below like a bottle rocket's, and a roaring jet of sparks at its tail.
 */
final class RocketFx extends FxEffect {
	private static final Vec3d UP = new Vec3d(0, 1, 0);
	private final Vec3d c;

	RocketFx(Vec3d c, int life) {
		super(Math.max(10, life));
		this.c = c;
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (age % 4 == 0) {
			ClientFx.light(c.add(0, Havoc.height(age) - Havoc.BODY * 0.6, 0), 0.9F, 0xFFA040);
		}
		return super.tick(world);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float td, float yaw, float pitch) {
		float a = age + td;
		if (a >= Havoc.CLIMB) {
			return;
		}
		VertexConsumer solid = consumers.getBuffer(FxRenderLayers.SOLID);
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(yaw, pitch);
		// A little wobble as it climbs.
		Vec3d axis = new Vec3d(Math.sin(a * 0.07) * 0.05, 1, Math.cos(a * 0.09) * 0.05).normalize();
		Vec3d tip = c.add(0, Havoc.height(a), 0).subtract(cam);
		Vec3d tail = tip.subtract(axis.multiply(Havoc.BODY));
		double r = Havoc.RADIUS, spin = a * 0.08;
		// The body in spiralling red and white bands.
		int bands = 8;
		for (int k = 0; k < bands; k++) {
			Vec3d p0 = tail.add(axis.multiply(Havoc.BODY * 0.8 * k / bands)), p1 = tail.add(axis.multiply(Havoc.BODY * 0.8 * (k + 1) / bands));
			int col = (k + (int) (spin * 2)) % 2 == 0 ? 0xD81E28 : 0xF2EEE6;
			Solids.tube(solid, m, p0, p1, r, r, 12, col, 1.0F, true);
		}
		Vec3d shoulder = tail.add(axis.multiply(Havoc.BODY * 0.8));
		Solids.tube(solid, m, shoulder, shoulder.add(axis.multiply(Havoc.BODY * 0.06)), r * 1.12, r * 1.12, 12, 0xE0B030, 1.0F, true);
		Solids.tube(solid, m, shoulder.add(axis.multiply(Havoc.BODY * 0.06)), tip, r * 1.05, 0.05, 12, 0xF0C840, 1.0F, true);
		// Four fins at the tail, turning with it.
		for (int k = 0; k < 4; k++) {
			double ang = spin + k * Math.PI / 2;
			Vec3d out = new Vec3d(Math.cos(ang), 0, Math.sin(ang));
			Vec3d root0 = tail.add(out.multiply(r)), root1 = tail.add(axis.multiply(3.0)).add(out.multiply(r));
			Vec3d tipF = tail.add(axis.multiply(-0.8)).add(out.multiply(r + 2.4));
			Solids.quad(solid, m, root0, root1, tipF, tipF.add(axis.multiply(1.2)), 0xD81E28, 1.0F);
			Solids.quad(solid, m, tipF.add(axis.multiply(1.2)), tipF, root1, root0, 0xD81E28, 1.0F);
		}
		// The guide stick trailing below.
		Solids.tube(solid, m, tail.add(new Vec3d(r * 0.7, 0, 0)), tail.add(new Vec3d(r * 0.7, 0, 0)).subtract(axis.multiply(Havoc.STICK)), 0.3, 0.25, 6,
				0x8A6034, 1.0F, true);
		// The fuse end burning: a white-hot core, a long jet of fire and sparks.
		float roar = 0.85F + 0.15F * MathHelper.sin(a * 1.7F);
		Solids.flare(glow, m, tail, cb, 4.5 * roar, 0xFFF2C0, 1.0F);
		FxDraw.beam(glow, m, tail, tail.subtract(axis.multiply(9 * roar)), Vec3d.ZERO, 1.4, 0xFFB040, 0.8F);
		FxDraw.beam(glow, m, tail, tail.subtract(axis.multiply(5)), Vec3d.ZERO, 2.6, 0xFF7020, 0.4F);
		FxKit.halo(consumers, m, cb, tail, 9, 0xFF9030, 0.4F * roar);
	}
}
