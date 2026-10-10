package com.armageddon.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Plasma cutter axe: fells an entire tree in one cut. Sneak for a single log. */
public class PlasmaCutterItem extends AxeItem {
	private static boolean breaking;

	public PlasmaCutterItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
		boolean result = super.postMine(stack, world, state, pos, miner);
		if (breaking || world.isClient || !state.isIn(BlockTags.LOGS) || !(miner instanceof ServerPlayerEntity player) || player.isSneaking()) {
			return result;
		}
		breaking = true;
		try {
			Set<BlockPos> seen = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(pos);
			seen.add(pos);
			int count = 0;
			while (!queue.isEmpty() && count < 192) {
				BlockPos p = queue.poll();
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = 0; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos n = p.add(dx, dy, dz);
							if (seen.add(n) && world.getBlockState(n).isIn(BlockTags.LOGS)) {
								queue.add(n);
								player.interactionManager.tryBreakBlock(n);
								count++;
								if (stack.isEmpty()) {
									return result;
								}
							}
						}
					}
				}
			}
		} finally {
			breaking = false;
		}
		return result;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
	}
}
