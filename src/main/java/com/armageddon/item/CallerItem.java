package com.armageddon.item;

import com.armageddon.event.EventEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paint a target and call something down. Catastrophic callers ask for a second click within
 * five seconds before they commit.
 */
public class CallerItem extends Item {
	@FunctionalInterface
	public interface Action {
		void call(ServerWorld world, PlayerEntity player, Vec3d target);
	}

	private static final Map<UUID, Long> PENDING = new HashMap<>();

	private final double range;
	private final int cooldown;
	private final boolean confirm;
	private final Action action;

	public CallerItem(Settings settings, double range, int cooldown, boolean confirm, Action action) {
		super(settings.maxCount(1));
		this.range = range;
		this.cooldown = cooldown;
		this.confirm = confirm;
		this.action = action;
	}

	/** Shorthand: start a scripted event at the target, passing the caller's yaw. */
	public static Action event(String id) {
		return (world, player, target) -> EventEntity.start(world, target, id, player, nbt -> nbt.putFloat("Yaw", player.getYaw()));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		BlockHitResult hit = TargetingItem.target(world, user, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			if (!world.isClient) {
				world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0F, 0.5F);
			}
			return TypedActionResult.fail(stack);
		}
		if (world instanceof ServerWorld server) {
			if (confirm) {
				long now = world.getTime();
				Long pending = PENDING.get(user.getUuid());
				if (pending == null || now - pending > 100) {
					PENDING.put(user.getUuid(), now);
					user.sendMessage(Text.translatable("message.armageddon.confirm").formatted(Formatting.RED, Formatting.BOLD), true);
					world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0F, 0.5F);
					return TypedActionResult.success(stack, false);
				}
				PENDING.remove(user.getUuid());
			}
			action.call(server, user, hit.getPos());
			user.getItemCooldownManager().set(this, cooldown);
			if (stack.isDamageable()) {
				stack.damage(1, user, LivingEntity.getSlotForHand(hand));
			}
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
		if (confirm) {
			tooltip.add(Text.translatable("tooltip.armageddon.confirm").formatted(Formatting.DARK_RED));
		}
	}

	@Override
	public Text getName(ItemStack stack) {
		return Tooltips.name(this, super.getName(stack));
	}
}
