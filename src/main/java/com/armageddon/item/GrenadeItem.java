package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GrenadeItem extends WmdItem {
	private final ProjectileKind kind;

	public GrenadeItem(Settings settings, ProjectileKind kind) {
		super(settings.maxCount(16), kind == ProjectileKind.POCKET_NUKE);
		this.kind = kind;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.7F, 0.6F);
		if (!world.isClient) {
			WmdProjectileEntity.fire(world, user, kind, 1.3F, 1.0F);
		}
		user.getItemCooldownManager().set(this, 12);
		stack.decrementUnlessCreative(1, user);
		return TypedActionResult.success(stack, world.isClient);
	}
}
