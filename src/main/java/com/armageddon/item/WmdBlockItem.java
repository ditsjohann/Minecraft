package com.armageddon.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/** Block item that shows the block's flavour text. */
public class WmdBlockItem extends BlockItem {
	public WmdBlockItem(Block block, Settings settings) {
		super(block, settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		Tooltips.describe(this, tooltip);
	}
}
