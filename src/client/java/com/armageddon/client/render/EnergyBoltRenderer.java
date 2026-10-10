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

/** Plasma, cryo and singularity bolts: layered additive glow with a velocity streak. */
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
		float pulse = 0.85F + 0.15F * MathHelper.sin((entity.age + tickDelta) * 1.7F);
		Matrix4f m = matrices.peek().getPositionMatrix();
		Vec3d[] basis = FxDraw.cameraBasis(this.dispatcher.camera.getYaw(), this.dispatcher.camera.getPitch());
		Vec3d c = new Vec3d(0, entity.getHeight() * 0.5, 0);
		VertexConsumer vc = consumers.getBuffer(FxRenderLayers.GLOW);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 2.6 * pulse, color, 0.45F, 0.0F, 20);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 1.1, color, 0.9F, 0.1F, 16);
		FxDraw.disc(vc, m, c, basis[0], basis[1], size * 0.5, 0xFFFFFF, 1.0F, 0.3F, 12);
		Vec3d v = entity.getVelocity();
		if (v.lengthSquared() > 0.01) {
			Vec3d eye = this.dispatcher.camera.getPos().subtract(entity.getLerpedPos(tickDelta));
			FxDraw.beam(vc, m, c, c.subtract(v.multiply(1.6)), eye, size * 0.9, color, 0.7F);
		}
		if (kind == ProjectileKind.BFG) {
			float spin = (entity.age + tickDelta) * 0.15F;
			Vec3d r1 = new Vec3d(Math.cos(spin), 0, Math.sin(spin));
			Vec3d r2 = new Vec3d(0, 1, 0);
			FxDraw.ring(vc, m, c, r1, r2, size * 1.6, size * 2.0, 0xB0FF90, 0.0F, 0.8F, 0.0F, 32);
			FxDraw.ring(vc, m, c, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), size * 2.2, size * 2.6, color, 0.0F, 0.6F, 0.0F, 32);
		}
		if (kind == ProjectileKind.BOOMERANG) {
			// Spinning tri-blade.
			float spin = (entity.age + tickDelta) * 0.9F;
			Vec3d flatX = new Vec3d(1, 0, 0), flatZ = new Vec3d(0, 0, 1);
			for (int i = 0; i < 3; i++) {
				double a = spin + i * MathHelper.TAU / 3;
				Vec3d tip = new Vec3d(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9);
				Vec3d side = new Vec3d(Math.cos(a + 0.9) * 0.45, 0, Math.sin(a + 0.9) * 0.45);
				FxDraw.ribbon(vc, m, c, c.add(tip), Vec3d.ZERO.add(0, 5, 0), 0.25, color, 0.9F, 0.3F);
				FxDraw.ribbon(vc, m, c.add(tip), c.add(side), Vec3d.ZERO.add(0, 5, 0), 0.12, 0xFFFFFF, 0.7F, 0.2F);
			}
			FxDraw.ring(vc, m, c, flatX, flatZ, 0.8, 1.05, color, 0.0F, 0.6F, 0.0F, 24);
		}
		if (kind == ProjectileKind.MIDAS || kind == ProjectileKind.MORTAR) {
			float spin = (entity.age + tickDelta) * 0.2F;
			FxDraw.ring(vc, m, c, new Vec3d(Math.cos(spin), 0, Math.sin(spin)), new Vec3d(0, 1, 0), size * 1.4, size * 1.8, 0xFFFFFF, 0.0F, 0.6F, 0.0F, 24);
		}
		if (kind == ProjectileKind.SINGULARITY) {
			VertexConsumer dark = consumers.getBuffer(FxRenderLayers.VOID);
			FxDraw.sphere(dark, m, c, size * 0.35, Vec3d.ZERO, 0x000000, 1.0F, 1.0F, 8, 12);
		}
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(WmdProjectileEntity entity) {
		return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
	}
}
