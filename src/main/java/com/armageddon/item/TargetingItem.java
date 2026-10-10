package com.armageddon.item;

import com.armageddon.entity.StrikeMarkerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/** Orbital Strike Designator / Storm Caller: paint a target, something terrible arrives. */
public class TargetingItem extends Item {
	private final int mode;
	private final double range;
	private final int cooldown;

	public TargetingItem(Settings settings, int mode, double range, int cooldown) {
		super(settings);
		this.mode = mode;
		this.range = range;
		this.cooldown = cooldown;
	}

	public double getRange() {
		return range;
	}

	public static BlockHitResult target(World world, PlayerEntity player, double range) {
		Vec3d eye = player.getEyePos();
		Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(range));
		return world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, player));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		BlockHitResult hit = target(world, user, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			if (!world.isClient) {
				world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0F, 0.5F);
			}
			return TypedActionResult.fail(stack);
		}
		if (world instanceof ServerWorld server) {
			StrikeMarkerEntity.spawn(server, hit.getPos(), mode, user);
			user.getItemCooldownManager().set(this, cooldown);
			stack.damage(1, user, LivingEntity.getSlotForHand(hand));
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
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
