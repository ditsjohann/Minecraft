package com.armageddon.item;

import com.armageddon.world.CityBuildJob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/** Use on the ground to construct a full test city to nuke. */
public class CityCapsuleItem extends WmdItem {
	public CityCapsuleItem(Settings settings) {
		super(settings.maxCount(16), true);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getWorld() instanceof ServerWorld world) {
			PlayerEntity player = context.getPlayer();
			CityBuildJob.start(world, context.getBlockPos().up(), 2);
			world.playSound(null, context.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 2.0F, 0.6F);
			if (player != null) {
				player.sendMessage(Text.translatable("message.armageddon.city.building").formatted(Formatting.AQUA), true);
			}
			context.getStack().decrementUnlessCreative(1, player);
		}
		return ActionResult.success(context.getWorld().isClient);
	}
}
