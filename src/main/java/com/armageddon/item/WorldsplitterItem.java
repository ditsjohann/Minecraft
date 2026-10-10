package com.armageddon.item;

import com.armageddon.event.EventEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** 33 Worldsplitter Hammer: right-click on the ground to split it in eight directions. */
public class WorldsplitterItem extends SwordItem {
	public WorldsplitterItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		Vec3d away = target.getPos().subtract(attacker.getPos()).normalize();
		target.addVelocity(away.x * 2.0, 1.0, away.z * 2.0);
		target.velocityModified = true;
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (!user.isOnGround()) {
			return TypedActionResult.fail(stack);
		}
		if (world instanceof ServerWorld server) {
			EventEntity.start(server, user.getPos(), "worldsplit", user, nbt -> nbt.putFloat("Yaw", user.getYaw()));
			user.getItemCooldownManager().set(this, 200);
			stack.damage(5, user, LivingEntity.getSlotForHand(hand));
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
	}

	@Override
	public Text getName(ItemStack stack) {
		return Tooltips.name(this, super.getName(stack));
	}
}
