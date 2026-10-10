package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
	public static final ItemGroup ARSENAL = Registry.register(Registries.ITEM_GROUP, Armageddon.id("arsenal"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(ModItems.NUKE_LAUNCHER))
					.displayName(Text.translatable("itemGroup.armageddon.arsenal"))
					.entries((ctx, entries) -> {
						for (Item item : ModItems.ALL) {
							entries.add(item);
						}
					})
					.build());

	public static final ItemGroup WARHEADS = Registry.register(Registries.ITEM_GROUP, Armageddon.id("warheads"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(ModBlocks.HYDROGEN_BOMB))
					.displayName(Text.translatable("itemGroup.armageddon.warheads"))
					.entries((ctx, entries) -> {
						for (Item item : ModBlocks.BLOCK_ITEMS) {
							entries.add(item);
						}
					})
					.build());

	public static void init() {
	}

	private ModItemGroups() {
	}
}
