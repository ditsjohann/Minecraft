package com.armageddon.client.fx;

import com.armageddon.client.vfx.FxEngine;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;

/**
 * A nuke's blast front rolling out over the real world. Each tick it samples the ground along the front and throws up
 * what is actually there: soil in its own colour, spray off water, leaves stripped out of tree crowns, powder off snow,
 * and where the front hits a wall, a building or a hillside the dust climbs the face and debris is thrown off the top.
 * Samples are biased toward the viewer so the part you can see is the part that is alive.
 */
final class NukeSurge extends FxEffect {
	/** Blocks per tick the front travels. */
	private static final double SPEED = 2.2;
	private static final int SAMPLES = 10;
	private static final double VIEW = 220;

	private final Vec3d c;
	private final double r0, r1;

	NukeSurge(Vec3d c, double radius) {
		super((int) Math.ceil(radius * 2.6 / SPEED) + 2);
		this.c = c;
		this.r0 = radius * 0.6;
		this.r1 = radius * 3.2;
	}

	@Override
	public boolean tick(ClientWorld world) {
		if (world != null) {
			double front = Math.min(r1, r0 + age * SPEED);
			Vec3d cam = ClientFx.cameraPos();
			double camBearing = Math.atan2(cam.z - c.z, cam.x - c.x);
			double camDist = Math.hypot(cam.x - c.x, cam.z - c.z);
			// The arc of the front the viewer can see well: wide when close to it, narrow when far.
			double spread = camDist < front ? Math.PI : Math.min(Math.PI, VIEW / Math.max(1, front));
			for (int i = 0; i < SAMPLES; i++) {
				double a = i % 2 == 0 ? world.random.nextDouble() * MathHelper.TAU : camBearing + (world.random.nextDouble() - 0.5) * 2 * spread;
				double ca = Math.cos(a), sa = Math.sin(a);
				double x = c.x + ca * front, z = c.z + sa * front;
				if ((x - cam.x) * (x - cam.x) + (z - cam.z) * (z - cam.z) > VIEW * VIEW) {
					continue;
				}
				sample(world, x, z, ca, sa, i);
			}
		}
		return super.tick(world);
	}

	private void sample(ClientWorld world, double x, double z, double ca, double sa, int i) {
		int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
		if (!world.getChunkManager().isChunkLoaded(bx >> 4, bz >> 4)) {
			return;
		}
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, bx, bz);
		int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
		if (ground <= world.getBottomY() + 1) {
			return;
		}
		// A step behind the front: how high the ground was where the front just came from.
		int bbx = MathHelper.floor(x - ca * 4), bbz = MathHelper.floor(z - sa * 4);
		int behind = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bbx, bbz);
		float dx = (float) ca, dz = (float) sa;
		long seed = age * 7919L + i * 131L + bx * 31L + bz;
		BlockPos below = new BlockPos(bx, ground - 1, bz);
		BlockState s = world.getBlockState(below);

		if (behind > world.getBottomY() + 1 && ground - behind >= 3) {
			// A face: a wall, a building, a hillside. The dust climbs it and the top is thrown back off it.
			int h = ground - behind;
			BlockState face = world.getBlockState(new BlockPos(bx, behind + Math.min(h, 3), bz));
			FxEngine.play(VfxIds.SURGE_WALL, x - ca * 1.5, behind, z - sa * 1.5, dx, 0, dz, Math.min(3.0F, h / 6.0F + 0.5F), dust(world, face, below), seed);
			return;
		}
		if (top - ground >= 2) {
			// A tree crown: the leaves are stripped out of it and carried.
			BlockState leaf = world.getBlockState(new BlockPos(bx, top - 1, bz));
			FxEngine.play(VfxIds.SURGE_LEAVES, x, top - 1.5, z, dx, 0, dz, 1.0F, leaf.getMapColor(world, below).color, seed);
		}
		if (!s.getFluidState().isEmpty() && s.getFluidState().isIn(FluidTags.WATER)
				|| !world.getFluidState(below).isEmpty() && world.getFluidState(below).isIn(FluidTags.WATER)) {
			FxEngine.play(VfxIds.SURGE_SPRAY, x, ground, z, dx, 0, dz, 1.0F, 0, seed);
			return;
		}
		if (s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.POWDER_SNOW) || world.getBlockState(below.up()).isOf(Blocks.SNOW)) {
			FxEngine.play(VfxIds.SURGE_SPRAY, x, ground, z, dx, 0, dz, 1.0F, 0xF4F8FF, seed);
			return;
		}
		FxEngine.play(VfxIds.SURGE_DUST, x, ground, z, dx, 0, dz, 1.0F, dust(world, s, below), seed);
	}

	/** What colour this ground throws up: its own map colour, soil under grass, dulled toward dust. */
	private static int dust(ClientWorld world, BlockState s, BlockPos pos) {
		int rgb;
		if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.MYCELIUM) || s.isOf(Blocks.PODZOL) || s.isIn(BlockTags.LEAVES) || s.isAir()) {
			rgb = 0x976D4D;
		} else {
			rgb = s.getMapColor(world, pos).color;
			if (rgb == 0) {
				rgb = 0x8A7A60;
			}
		}
		return Effects.fade(rgb, 0xA89C8C, 0.35F);
	}

	@Override
	public void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch) {
	}
}
