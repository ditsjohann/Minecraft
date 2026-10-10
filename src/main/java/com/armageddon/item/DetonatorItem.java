package com.armageddon.item;

import com.armageddon.block.BombBlock;
import com.armageddon.block.BombType;
import com.armageddon.registry.ModComponents;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote detonator. Sneak + right-click warheads/charges to link (up to 16).
 * Right-click in the air to fire every linked charge at once.
 */
public class DetonatorItem extends Item {
	public static final int MAX_LINKS = 16;

	public DetonatorItem(Settings settings) {
		super(settings.maxCount(1).component(ModComponents.LINKED_CHARGES, List.of()));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		PlayerEntity player = context.getPlayer();
		if (player == null || !player.isSneaking() || !(world.getBlockState(pos).getBlock() instanceof BombBlock)) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			ItemStack stack = context.getStack();
			List<BlockPos> links = new ArrayList<>(stack.getOrDefault(ModComponents.LINKED_CHARGES, List.of()));
			if (links.remove(pos)) {
				player.sendMessage(Text.translatable("message.armageddon.detonator.unlinked", links.size()).formatted(Formatting.YELLOW), true);
			} else if (links.size() >= MAX_LINKS) {
				player.sendMessage(Text.translatable("message.armageddon.detonator.full").formatted(Formatting.RED), true);
			} else {
				links.add(pos.toImmutable());
				player.sendMessage(Text.translatable("message.armageddon.detonator.linked", links.size()).formatted(Formatting.GREEN), true);
			}
			stack.set(ModComponents.LINKED_CHARGES, List.copyOf(links));
			world.playSound(null, pos, ModSounds.DETONATOR, SoundCategory.PLAYERS, 0.8F, 1.8F);
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		List<BlockPos> links = stack.getOrDefault(ModComponents.LINKED_CHARGES, List.of());
		if (links.isEmpty()) {
			return TypedActionResult.pass(stack);
		}
		if (!world.isClient) {
			int fired = 0;
			for (BlockPos pos : links) {
				if (!world.isChunkLoaded(pos)) {
					continue;
				}
				BlockState state = world.getBlockState(pos);
				if (state.getBlock() instanceof BombBlock bomb) {
					int fuse = bomb.getBombType() == BombType.DEMOLITION_CHARGE ? 2 + fired * 2 : 60;
					BombBlock.arm(world, pos, state, user, fuse);
					fired++;
				}
			}
			stack.set(ModComponents.LINKED_CHARGES, List.of());
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.DETONATOR, SoundCategory.PLAYERS, 1.0F, 1.0F);
			user.sendMessage(Text.translatable("message.armageddon.detonator.fired", fired).formatted(Formatting.GOLD), true);
			user.getItemCooldownManager().set(this, 20);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return !stack.getOrDefault(ModComponents.LINKED_CHARGES, List.of()).isEmpty();
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
		tooltip.add(Text.translatable("tooltip.armageddon.linked", stack.getOrDefault(ModComponents.LINKED_CHARGES, List.of()).size(), MAX_LINKS)
				.formatted(Formatting.GOLD));
	}
}
