package com.armageddon.client.render;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
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

/** Plasma, cryo and singularity bolts: layered additive glow, a tapered comet tail and a per-kind signature. */
public class EnergyBoltRenderer extends EntityRenderer<WmdProjectileEntity> {
	public EnergyBoltRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(WmdProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		ProjectileKind kind = entity.getKind();
		int color;
		double size;
		switch (kind) {
			case CRYO -> {
				color = 0x8AD8FF;
				size = 0.45;
			}
			case SINGULARITY -> {
				color = 0xA040FF;
				size = 0.8;
			}
			case BFG -> {
				color = 0x5AFF3A;
				size = 1.4;
			}
			case PARTY_ROCKET -> {
				color = MathHelper.hsvToRgb(((entity.age + tickDelta) * 0.05F) % 1.0F, 0.8F, 1.0F);
				size = 0.35;
			}
			case BOOMERANG -> {
				color = 0x40C8FF;
				size = 0.35;
			}
			case MORTAR -> {
				color = 0xB050FF;
				size = 0.7;
			}
			case MIDAS, MIDAS_SPHERE -> {
				color = 0xFFC830;
				size = kind == ProjectileKind.MIDAS ? 0.9 : 0.5;
			}
			case JUDGMENT_BOLT -> {
				color = MathHelper.clamp(entity.getPower(), 0, 100) > 60 ? 0xFF7020 : 0xFFB020;
				size = 0.25 + entity.getPower() * 0.003;
			}
			default -> {
				color = 0x40FFE0;
				size = 0.3;
			}
		}
		float time = entity.age + tickDelta;
		float pulse = 0.85F + 0.15F * MathHelper.sin(time * 1.7F);
		Matrix4f m = matrices.peek().getPositionMatrix();
		Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
		Vec3d c = new Vec3d(0, entity.getHeight() * 0.5, 0);
		VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
		int hot = mix(color, 0xFFFFFF, 0.55F);
		Vec3d v = entity.getVelocity();
		if (v.lengthSquared() > 0.01) {
			// Tapered comet tail: soft colour sheath, brighter body, white-hot filament, fading afterimages.
			FxDraw.ribbon(vc, m, c, c.subtract(v.multiply(2.6)), eye, size * 2.2, color, 0.3F, 0.0F);
			FxDraw.ribbon(vc, m, c, c.subtract(v.multiply(1.8)), eye, size * 1.1, color, 0.75F, 0.0F);
			FxDraw.ribbon(vc, m, c, c.subtract(v.multiply(1.1)), eye, size * 0.35, 0xFFFFFF, 0.95F, 0.0F);
			for (int i = 1; i <= 4; i++) {
				Vec3d p = c.subtract(v.multiply(i * 0.55));
				FxDraw.disc(vc, m, p, basis[0], basis[1], size * (1.2 - i * 0.2), color, 0.35F * (1 - i / 5.0F), 0.0F, 12);
			}
		}
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 4.0 * pulse, color, 0.18F, 0.0F, 24);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 2.4 * pulse, color, 0.45F, 0.0F, 20);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 1.1, hot, 0.9F, 0.1F, 16);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 0.5, 0xFFFFFF, 1.0F, 0.3F, 12);
		// Four-point glint that slowly turns.
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2 + time * 0.04;
			Vec3d d = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			FxDraw.ribbon(vc, m, c, c.add(d.multiply(size * (k % 2 == 0 ? 3.2 : 2.0) * pulse)), eye, size * 0.14, hot, 0.8F, 0.0F);
		}
		if (kind == ProjectileKind.BFG) {
			float spin = time * 0.15F;
			Vec3d r1 = new Vec3d(Math.cos(spin), 0, Math.sin(spin));
			Vec3d r2 = new Vec3d(0, 1, 0);
			FxDraw.ring(vc, m, c, r1, r2, size * 1.6, size * 2.0, 0xB0FF90, 0.0F, 0.8F, 0.0F, 32);
			FxDraw.ring(vc, m, c, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), size * 2.2, size * 2.6, color, 0.0F, 0.6F, 0.0F, 32);
			// Crackling arcs, re-rolled a few times a second.
			int roll = (int) (time * 0.5F);
			for (int k = 0; k < 5; k++) {
				Vec3d prev = c;
				Vec3d dir = new Vec3d(hash(k, roll) - 0.5, hash(k + 9, roll) - 0.5, hash(k + 17, roll) - 0.5).normalize();
				for (int j = 1; j <= 4; j++) {
					Vec3d q = c.add(dir.multiply(size * 0.9 * j)).add(new Vec3d(hash(k * 7 + j, roll) - 0.5, hash(k * 5 + j, roll + 1) - 0.5, hash(k * 3 + j, roll + 2) - 0.5).multiply(size * 0.8));
					FxDraw.ribbon(vc, m, prev, q, eye, size * 0.1, j % 2 == 0 ? 0xFFFFFF : 0xB0FF90, 0.9F, 0.7F);
					prev = q;
				}
			}
		}
		if (kind == ProjectileKind.CRYO) {
			// A turning six-point frost star.
			for (int k = 0; k < 6; k++) {
				double a = k * Math.PI / 3 + time * 0.08;
				Vec3d d = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
				Vec3d side = basis[0].multiply(-Math.sin(a)).add(basis[1].multiply(Math.cos(a)));
				Vec3d tip = c.add(d.multiply(size * 2.4));
				FxDraw.ribbon(vc, m, c, tip, eye, size * 0.22, 0xD8F4FF, 0.85F, 0.2F);
				Vec3d mid = c.add(d.multiply(size * 1.4));
				FxDraw.ribbon(vc, m, mid, mid.add(d.add(side).normalize().multiply(size * 0.6)), eye, size * 0.1, 0xFFFFFF, 0.7F, 0.0F);
				FxDraw.ribbon(vc, m, mid, mid.add(d.subtract(side).normalize().multiply(size * 0.6)), eye, size * 0.1, 0xFFFFFF, 0.7F, 0.0F);
			}
			FxDraw.ring(vc, m, c, basis[0], basis[1], size * 2.6, size * 2.9, 0xBFE8FF, 0.0F, 0.5F, 0.0F, 32);
		}
		if (kind == ProjectileKind.PARTY_ROCKET) {
			for (int i = 0; i < 8; i++) {
				Vec3d p = c.subtract(v.multiply(0.3 + i * 0.4)).add(Math.sin(time * 0.7 + i * 2.1) * 0.3, Math.cos(time * 0.6 + i) * 0.3, Math.sin(time * 0.5 + i * 1.3) * 0.3);
				FxDraw.disc(vc, m, p, basis[0], basis[1], 0.12, MathHelper.hsvToRgb((time * 0.05F + i * 0.12F) % 1.0F, 0.7F, 1.0F), 0.9F * (1 - i / 9.0F), 0.0F, 6);
			}
		}
		if (kind == ProjectileKind.JUDGMENT_BOLT) {
			for (int k = 0; k < 8; k++) {
				double a = k * Math.PI / 4 - time * 0.06;
				Vec3d d = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
				FxDraw.ribbon(vc, m, c.add(d.multiply(size * 1.2)), c.add(d.multiply(size * 4.5)), eye, size * 0.3, 0xFFE8A0, 0.5F, 0.0F);
			}
		}
		if (kind == ProjectileKind.BOOMERANG) {
			// Spinning tri-blade.
			float spin = time * 0.9F;
			Vec3d flatX = new Vec3d(1, 0, 0), flatZ = new Vec3d(0, 0, 1);
			Vec3d above = new Vec3d(0, 5, 0);
			for (int i = 0; i < 3; i++) {
				double a = spin + i * MathHelper.TAU / 3;
				Vec3d tip = new Vec3d(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9);
				Vec3d side = new Vec3d(Math.cos(a + 0.9) * 0.45, 0, Math.sin(a + 0.9) * 0.45);
				FxDraw.ribbon(vc, m, c, c.add(tip), above, 0.25, color, 0.9F, 0.3F);
				FxDraw.ribbon(vc, m, c.add(tip), c.add(side), above, 0.12, 0xFFFFFF, 0.7F, 0.2F);
				// Motion blur ghost of each blade.
				Vec3d ghost = new Vec3d(Math.cos(a - 0.5) * 0.9, 0, Math.sin(a - 0.5) * 0.9);
				FxDraw.ribbon(vc, m, c, c.add(ghost), above, 0.3, color, 0.35F, 0.0F);
			}
			FxDraw.ring(vc, m, c, flatX, flatZ, 0.8, 1.05, color, 0.0F, 0.6F, 0.0F, 24);
		}
		if (kind == ProjectileKind.MIDAS || kind == ProjectileKind.MORTAR) {
			float spin = time * 0.2F;
			FxDraw.ring(vc, m, c, new Vec3d(Math.cos(spin), 0, Math.sin(spin)), new Vec3d(0, 1, 0), size * 1.4, size * 1.8, 0xFFFFFF, 0.0F, 0.6F, 0.0F, 24);
			FxDraw.ring(vc, m, c, new Vec3d(1, 0, 0), new Vec3d(0, Math.sin(spin), Math.cos(spin)), size * 1.6, size * 1.9, color, 0.0F, 0.5F, 0.0F, 24);
		}
		if (kind == ProjectileKind.SINGULARITY) {
			float spin = time * 0.12F;
			Vec3d u = new Vec3d(Math.cos(spin), 0.3, Math.sin(spin)).normalize();
			Vec3d w = new Vec3d(-Math.sin(spin), 0, Math.cos(spin));
			FxDraw.ring(vc, m, c, u, w, size * 0.45, size * 1.1, 0xFFD0FF, 0.0F, 0.9F, 0.0F, 32);
			FxDraw.ring(vc, m, c, basis[0], basis[1], size * 0.36, size * 0.5, 0xFFFFFF, 0.0F, 1.0F, 0.0F, 32);
			for (int k = 0; k < 3; k++) {
				Vec3d prev = null;
				for (int s = 0; s <= 10; s++) {
					double t = s / 10.0;
					double a = k * MathHelper.TAU / 3 + t * 3.5 - time * 0.2;
					Vec3d d = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
					Vec3d p = c.add(d.multiply(size * (2.4 - 2.0 * t)));
					if (prev != null) {
						FxDraw.ribbon(vc, m, prev, p, eye, size * 0.12, color, 0.6F * (float) t, 0.6F * (float) t);
					}
					prev = p;
				}
			}
			VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
			FxDraw.sphere(dark, m, c, size * 0.35, Vec3d.ZERO, 0x000000, 1.0F, 1.0F, 8, 12);
		}
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	private static int mix(int a, int b, float t) {
		int r = (int) MathHelper.lerp(t, (float) ((a >> 16) & 0xFF), (float) ((b >> 16) & 0xFF));
		int g = (int) MathHelper.lerp(t, (float) ((a >> 8) & 0xFF), (float) ((b >> 8) & 0xFF));
		int bl = (int) MathHelper.lerp(t, (float) (a & 0xFF), (float) (b & 0xFF));
		return (r << 16) | (g << 8) | bl;
	}

	private static double hash(int x, int y) {
		long h = x * 374761393L + y * 668265263L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
