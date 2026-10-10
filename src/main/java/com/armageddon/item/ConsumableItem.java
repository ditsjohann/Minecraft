package com.armageddon.item;

import com.armageddon.registry.ModEffects;
import com.armageddon.registry.ModSounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Rad-X injector and Nano-Medkit. */
public class ConsumableItem extends WmdItem {
	public enum Kind { RAD_X, MEDKIT }

	private final Kind kind;

	public ConsumableItem(Settings settings, Kind kind) {
		super(settings.maxCount(16));
		this.kind = kind;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (!world.isClient) {
			switch (kind) {
				case RAD_X -> {
					user.removeStatusEffect(ModEffects.RADIATION);
					user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));
				}
				case MEDKIT -> {
					user.heal(12.0F);
					user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
					user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 1));
				}
			}
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.INJECT, SoundCategory.PLAYERS, 1.0F, 1.0F);
		}
		user.getItemCooldownManager().set(this, 40);
		stack.decrementUnlessCreative(1, user);
		return TypedActionResult.success(stack, world.isClient);
	}
}
