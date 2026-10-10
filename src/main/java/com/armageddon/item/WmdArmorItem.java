package com.armageddon.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class WmdArmorItem extends ArmorItem {
	private final String setKey;

	public WmdArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Settings settings, String setKey) {
		super(material, type, settings);
		this.setKey = setKey;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
		tooltip.add(Text.translatable("tooltip.armageddon.set." + setKey).formatted(Formatting.GOLD));
	}
}
