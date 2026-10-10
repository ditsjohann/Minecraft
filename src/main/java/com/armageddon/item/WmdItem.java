package com.armageddon.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/** Plain item with flavour-text tooltip from lang key {@code item.armageddon.<id>.desc}. */
public class WmdItem extends Item {
	private final boolean glint;

	public WmdItem(Settings settings) {
		this(settings, false);
	}

	public WmdItem(Settings settings, boolean glint) {
		super(settings);
		this.glint = glint;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return glint || super.hasGlint(stack);
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
