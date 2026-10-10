package com.armageddon.item;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** A continuous annihilation beam that bores tunnels through terrain and cooks anything in its path. */
public class DeathRayItem extends GunItem {
	private static final double RANGE = 96;

	public DeathRayItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.FUSION_CORE, 300, 0, 40);
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d eye = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0F);
		BlockHitResult block = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(RANGE)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		Vec3d end = block.getType() == HitResult.Type.MISS ? eye.add(dir.multiply(RANGE)) : block.getPos();

		ModNetworking.sendFx(world, eye, 256, new FxPayload(FxType.DEATH_RAY, muzzle(player, 1.0), end, 0.3F, 0xFF2A40, heldTicks));
		if (heldTicks % 5 == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DEATH_RAY, SoundCategory.PLAYERS, 1.2F, 0.8F + world.random.nextFloat() * 0.1F);
		}
		for (Entity e : Hitscan.pierce(world, player, eye, end, 0.6)) {
			if (e instanceof LivingEntity living && heldTicks % 3 == 0) {
				living.timeUntilRegen = 0;
			}
			e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, player, player), 4.0F);
			e.setOnFireFor(6.0F);
		}
		if (block.getType() == HitResult.Type.BLOCK && heldTicks % 2 == 0 && Detonations.blockDamage(world)) {
			melt(world, block.getBlockPos(), block.getSide());
		}
	}

	private static void melt(ServerWorld world, BlockPos center, Direction side) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2) {
						continue;
					}
					BlockPos p = center.add(dx, dy, dz);
					BlockState s = world.getBlockState(p);
					float h = s.getHardness(world, p);
					if (s.isAir() || h < 0 || h > 50) {
						continue;
					}
					world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
				}
			}
		}
		BlockPos rim = center.offset(side.getOpposite(), 2);
		BlockState r = world.getBlockState(rim);
		if (!r.isAir() && r.getHardness(world, rim) >= 0 && r.getHardness(world, rim) < 50 && world.random.nextFloat() < 0.3F) {
			world.setBlockState(rim, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
		}
		BlockPos fire = center.offset(side);
		if (world.random.nextFloat() < 0.2F && world.getBlockState(fire).isAir() && AbstractFireBlock.canPlaceAt(world, fire, side)) {
			world.setBlockState(fire, AbstractFireBlock.getState(world, fire), Block.NOTIFY_ALL);
		}
	}
}
