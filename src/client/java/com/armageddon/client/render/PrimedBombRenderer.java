package com.armageddon.client.render;

import com.armageddon.block.BombType;
import com.armageddon.entity.PrimedBombEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.TntMinecartEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Armed warhead: flashing block model that swells before detonation, wrapped in a warning glow
 * that pulses in time with the countdown beeps, a turning hazard ring on the ground, sparks that
 * spiral off the casing, and a beacon for the strategic and celestial tiers.
 */
public class PrimedBombRenderer extends EntityRenderer<PrimedBombEntity> {
	private final BlockRenderManager blocks;

	public PrimedBombRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.blocks = ctx.getBlockRenderManager();
		this.shadowRadius = 0.5F;
	}

	@Override
	public void render(PrimedBombEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		matrices.push();
		int fuse = entity.getFuse();
		float f = fuse - tickDelta + 1.0F;
		if (f < 10.0F) {
			float g = 1.0F - f / 10.0F;
			g = MathHelper.clamp(g, 0.0F, 1.0F);
			g *= g;
			g *= g;
			float s = 1.0F + g * 0.3F;
			matrices.scale(s, s, s);
		}
		matrices.translate(-0.5F, 0.0F, -0.5F);
		boolean flash = fuse / 5 % 2 == 0;
		TntMinecartEntityRenderer.renderFlashingBlock(blocks, entity.getBlockState(), matrices, consumers, light, flash);
		matrices.pop();

		BombType type = entity.getBombType();
		float time = entity.age + tickDelta;
		float armed = 1.0F - MathHelper.clamp(f / Math.max(1, type.fuse), 0.0F, 1.0F);
		// Same cadence as the server's countdown beeps, so the glow throbs on every beep.
		int interval = fuse > 100 ? 20 : fuse > 40 ? 10 : fuse > 15 ? 5 : 2;
		float phase = ((f % interval) + interval) % interval / interval;
		float beat = (float) Math.pow(phase, 3.0);
		int color = type.color == 0xFFFFFF ? 0xFFE0E0 : type.color;
		boolean celestial = type.isCelestial();
		if (celestial) {
			color = MathHelper.hsvToRgb((time * 0.01F) % 1.0F, 0.55F, 1.0F);
		}

		Matrix4f m = matrices.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
		Vec3d[] cb = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d mid = new Vec3d(0, 0.5, 0);
		Vec3d x = new Vec3d(1, 0, 0), z = new Vec3d(0, 0, 1);

		// Warning glow around the casing.
		float glowA = 0.18F + 0.5F * beat * (0.4F + 0.6F * armed);
		FxDraw.disc(vc, m, mid, cb[0], cb[1], 1.4 + armed * 1.2 + beat * 0.5, color, glowA, 0.0F, 24);
		FxDraw.disc(vc, m, mid, cb[0], cb[1], 0.8 + beat * 0.3, 0xFFFFFF, glowA * 0.6F, 0.0F, 16);

		// Hazard ring: dashed, turning, tightening as the fuse runs out.
		double rr = 2.4 - armed * 0.9;
		double spin = time * (0.03 + armed * 0.12);
		int dashes = 12;
		for (int i = 0; i < dashes; i++) {
			double a0 = spin + i * MathHelper.TAU / dashes, a1 = a0 + MathHelper.TAU / dashes * 0.55;
			arc(vc, m, new Vec3d(0, 0.04, 0), a0, a1, rr, rr + 0.18, color, 0.35F + 0.55F * beat);
		}
		FxDraw.ring(vc, m, new Vec3d(0, 0.03, 0), x, z, rr * 1.2, rr * 1.32, color, 0.0F, 0.25F + 0.3F * beat, 0.0F, 40);
		FxDraw.disc(vc, m, new Vec3d(0, 0.02, 0), x, z, rr * 1.3, color, 0.12F + 0.15F * beat, 0.0F, 32);

		// Sparks spiralling up off the casing, more of them as it gets closer.
		int sparks = 4 + (int) (armed * 10);
		for (int i = 0; i < sparks; i++) {
			double u = ((time * (0.02 + 0.01 * (i % 3)) + i * 0.137) % 1.0);
			double ang = i * 2.4 + time * 0.25 + u * 6;
			double rad = 0.6 + u * 0.9;
			Vec3d p = new Vec3d(Math.cos(ang) * rad, 0.4 + u * 2.4, Math.sin(ang) * rad);
			FxDraw.disc(vc, m, p, cb[0], cb[1], 0.09 * (1 - u) + 0.03, i % 3 == 0 ? 0xFFFFFF : color, 0.9F * (float) (1 - u), 0.0F, 6);
		}

		// The last second: rays leaking out of the seams.
		if (fuse < 20) {
			float k = 1.0F - f / 20.0F;
			for (int i = 0; i < 8; i++) {
				double a = i * MathHelper.TAU / 8 + time * 0.05;
				Vec3d dir = cb[0].multiply(Math.cos(a)).add(cb[1].multiply(Math.sin(a)));
				FxDraw.ribbon(vc, m, mid, mid.add(dir.multiply(1.5 + k * 3.5)), eye, 0.25 + k * 0.3, i % 2 == 0 ? 0xFFFFFF : color, 0.7F * k, 0.0F);
			}
		}

		if (type.isStrategic() || celestial) {
			Vec3d top = new Vec3d(0, 1.15, 0);
			float pulse = 0.5F + 0.5F * beat;
			FxDraw.disc(vc, m, top, cb[0], cb[1], 0.6 + pulse * 0.5, 0xFF2020, pulse, 0.0F, 16);
			FxDraw.disc(vc, m, top, cb[0], cb[1], 0.22, 0xFFFFFF, pulse, 0.0F, 10);
			int beamCol = celestial ? color : 0xFF3030;
			FxDraw.beam(vc, m, top, top.add(0, 10 + armed * 30, 0), eye, 0.14 + armed * 0.1, beamCol, pulse * 0.6F);
			// Scan rings riding up the beacon.
			for (int i = 0; i < 3; i++) {
				double h = ((time * 0.04 + i / 3.0) % 1.0);
				FxDraw.ring(vc, m, top.add(0, h * 8, 0), x, z, 0.5 + h * 0.6, 0.6 + h * 0.6, beamCol, 0.0F, 0.7F * (float) (1 - h), 0.0F, 20);
			}
		}
		if (celestial) {
			// Prismatic halo and orbiting motes for the world-ending tiers.
			for (int k = 0; k < 2; k++) {
				double tilt = time * (k == 0 ? 0.02 : -0.03);
				Vec3d u = new Vec3d(Math.cos(tilt), 0.35 * (k == 0 ? 1 : -1), Math.sin(tilt)).normalize();
				Vec3d v = new Vec3d(-Math.sin(tilt), 0, Math.cos(tilt));
				FxDraw.ring(vc, m, mid, u, v, 1.5 + k * 0.4, 1.62 + k * 0.4, MathHelper.hsvToRgb((time * 0.01F + k * 0.5F) % 1.0F, 0.6F, 1.0F), 0.0F, 0.8F, 0.0F, 48);
			}
			for (int i = 0; i < 6; i++) {
				double a = i * MathHelper.TAU / 6 + time * 0.06;
				Vec3d p = mid.add(Math.cos(a) * 1.9, Math.sin(time * 0.1 + i) * 0.4, Math.sin(a) * 1.9);
				FxDraw.disc(vc, m, p, cb[0], cb[1], 0.18, MathHelper.hsvToRgb((i / 6.0F + time * 0.01F) % 1.0F, 0.5F, 1.0F), 0.9F, 0.0F, 8);
			}
		}
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	/** Flat arc segment in the ground plane. */
	private static void arc(VertexConsumer vc, Matrix4f m, Vec3d c, double a0, double a1, double r0, double r1, int rgb, float alpha) {
		int n = 4;
		for (int i = 0; i < n; i++) {
			double t0 = a0 + (a1 - a0) * i / n, t1 = a0 + (a1 - a0) * (i + 1) / n;
			FxDraw.vertex(vc, m, c.x + Math.cos(t0) * r0, c.y, c.z + Math.sin(t0) * r0, rgb, alpha);
			FxDraw.vertex(vc, m, c.x + Math.cos(t1) * r0, c.y, c.z + Math.sin(t1) * r0, rgb, alpha);
			FxDraw.vertex(vc, m, c.x + Math.cos(t1) * r1, c.y, c.z + Math.sin(t1) * r1, rgb, alpha * 0.4F);
			FxDraw.vertex(vc, m, c.x + Math.cos(t0) * r1, c.y, c.z + Math.sin(t0) * r1, rgb, alpha * 0.4F);
		}
	}

	@Override
	public Identifier getTexture(PrimedBombEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
