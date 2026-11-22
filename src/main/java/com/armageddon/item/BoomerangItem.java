package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** 16 Plasma Boomerang: a spinning plasma blade that slices through targets and flies home. */
public class BoomerangItem extends WmdItem {
	public BoomerangItem(Settings settings) {
		super(settings.maxCount(1));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.BLADE_DASH, SoundCategory.PLAYERS, 1.0F, 1.4F);
		if (!world.isClient) {
			WmdProjectileEntity.fire(world, user, ProjectileKind.BOOMERANG, 1.8F, 0.0F);
			Vfx.play((ServerWorld) world, VfxIds.THROW_WHOOSH,
					user.getEyePos().add(user.getRotationVec(1.0F).multiply(0.8)), user.getRotationVec(1.0F), 1.0F, 0);
			// The blade itself is the projectile; it puts itself back in the inventory on return.
			stack.decrement(1);
		}
		return TypedActionResult.success(stack, world.isClient);
	}
}
