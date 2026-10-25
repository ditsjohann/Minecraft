package com.armageddon.client.render;

import com.armageddon.entity.DevourerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * The Devourer: a crimson singularity with a churning maw of teeth, a heartbeat glow, tendrils of
 * torn light curling into it, and a double whirlpool of embers being swallowed.
 */
public class DevourerRenderer extends EntityRenderer<DevourerEntity> {
	public DevourerRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public boolean shouldRender(DevourerEntity entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(DevourerEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		float r = entity.getRadius();
		float time = entity.age + tickDelta;
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d[] cb = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		// Heartbeat: a double thump every 30 ticks.
		double beatT = (time % 30) / 30.0;
		double heart = Math.exp(-Math.pow((beatT - 0.1) * 18, 2)) + 0.6 * Math.exp(-Math.pow((beatT - 0.25) * 18, 2));
		double core = r * 0.45 * (1 + 0.06 * heart);

		matrices.push();
		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer glow = consumers.getBuffer(FxRenderLayers.GLOW);
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 3.6, 0x300004, 0.5F, 0.0F, 40);
		FxDraw.disc(glow, m, Vec3d.ZERO, cb[0], cb[1], r * 2.0, 0x900010, (float) (0.35 + 0.3 * heart), 0.0F, 40);

		// Whirlpools: two counter-rotating tilted discs of embers.
		whirl(glow, m, core, time, 70, 1.0, 0xFF2010, 0x900020);
		whirl(glow, m, core * 0.9, time, -55, -1.3, 0xFF6A30, 0x600018);

		// Tendrils of light curling inward.
		for (int k = 0; k < 8; k++) {
			Vec3d prev = null;
			for (int s = 0; s <= 14; s++) {
				double t = s / 14.0;
				double a = k * MathHelper.TAU / 8 + t * 2.6 - time * 0.05;
				double rad = core * (4.2 - 3.0 * t);
				Vec3d dir = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
				Vec3d p = dir.multiply(rad).add(eye.normalize().multiply(Math.sin(t * 5 + k) * core * 0.3));
				if (prev != null) {
					float al = (float) (Math.sin(t * Math.PI) * 0.6);
					FxDraw.ribbon(glow, m, prev, p, eye, core * 0.12 * (1.3 - t), t > 0.6 ? 0xFFB090 : 0xFF3020, al, al);
				}
				prev = p;
			}
		}

		// Jagged maw: two rows of teeth around the horizon.
		for (int row = 0; row < 2; row++) {
			int teeth = 18 + row * 6;
			for (int i = 0; i < teeth; i++) {
				double a = i * MathHelper.TAU / teeth + time * (row == 0 ? 0.03 : -0.045);
				double len = core * (1.5 + 0.55 * Math.sin(time * 0.4 + i * 2.1 + row) + 0.3 * heart);
				Vec3d d = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
				Vec3d side = cb[0].multiply(-Math.sin(a)).add(cb[1].multiply(Math.cos(a)));
				Vec3d base = d.multiply(core * (1.0 + row * 0.12));
				Vec3d tip = d.multiply(len * (1.0 - row * 0.2)).add(side.multiply(core * 0.15));
				FxDraw.ribbon(glow, m, base, tip, eye, core * (0.24 - row * 0.06), row == 0 ? 0xFF5030 : 0xFFC0A0, 0.85F, 0.0F);
			}
		}
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], core, core * 1.35, 0xFF3020, 0.0F, 1.0F, 0.0F, 64);
		FxDraw.ring(glow, m, Vec3d.ZERO, cb[0], cb[1], core * 0.98, core * 1.06, 0xFFE0D0, 0.0F, (float) (0.6 + 0.4 * heart), 0.0F, 64);

		VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
		FxDraw.sphere(dark, m, Vec3d.ZERO, core, eye, 0x000000, 1.0F, 1.0F, 14, 22);
		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	/** A tilted spinning disc of ember streaks spiralling down into the core. */
	private static void whirl(VertexConsumer vc, Matrix4f m, double core, float time, double tiltDeg, double dirSpin, int hot, int cold) {
		double tilt = Math.toRadians(tiltDeg);
		Vec3d u = new Vec3d(1, 0, 0);
		Vec3d v = new Vec3d(0, Math.sin(tilt), Math.cos(tilt));
		FxDraw.ring(vc, m, Vec3d.ZERO, u, v, core * 1.3, core * 2.6, hot, 0.2F, 0.6F, 0.0F, 64);
		FxDraw.ring(vc, m, Vec3d.ZERO, u, v, core * 2.4, core * 4.2, cold, 0.0F, 0.35F, 0.0F, 64);
		for (int i = 0; i < 16; i++) {
			double t = ((time * 0.012 + i / 16.0) % 1.0);
			double rad = core * (4.0 - 2.7 * t);
			double a = i * 2.39 + dirSpin * (time * 0.05 + t * 5);
			Vec3d p = u.multiply(Math.cos(a) * rad).add(v.multiply(Math.sin(a) * rad));
			Vec3d q = u.multiply(Math.cos(a - 0.35 * dirSpin) * rad * 1.05).add(v.multiply(Math.sin(a - 0.35 * dirSpin) * rad * 1.05));
			FxDraw.ribbon(vc, m, q, p, Vec3d.ZERO.add(u.crossProduct(v).multiply(core * 50)), core * 0.1, i % 3 == 0 ? 0xFFFFFF : hot, 0.0F, 0.8F * (float) Math.sin(t * Math.PI));
		}
	}

	@Override
	public Identifier getTexture(DevourerEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
