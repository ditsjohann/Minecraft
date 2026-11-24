package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/** Six creative tabs; contents and order come from {@link Catalog}. */
public final class ModItemGroups {
	public static final ItemGroup WEAPONS = tab(Catalog.Category.WEAPONS, () -> ModItems.JUDGMENT_ENGINE);
	public static final ItemGroup EXPLOSIVES = tab(Catalog.Category.EXPLOSIVES, () -> ModItems.POCKET_NUKE);
	public static final ItemGroup BOMBS = tab(Catalog.Category.BOMBS, () -> ModBlocks.HYDROGEN_BOMB);
	public static final ItemGroup CATACLYSMS = tab(Catalog.Category.CATACLYSMS, () -> ModBlocks.GALACTIC_SMASH);
	public static final ItemGroup GEAR = tab(Catalog.Category.GEAR, () -> ModItems.JUGGERNAUT_CHESTPLATE);
	public static final ItemGroup NEW = tab(Catalog.Category.NEW, () -> ModBlocks.PLASMA_KOI);

	private static ItemGroup tab(Catalog.Category category, Supplier<ItemConvertible> icon) {
		return Registry.register(Registries.ITEM_GROUP, Armageddon.id(category.key),
				FabricItemGroup.builder()
						.icon(() -> new ItemStack(icon.get()))
						.displayName(Text.translatable("itemGroup.armageddon." + category.key))
						.entries((ctx, entries) -> {
							for (Item item : Catalog.of(category)) {
								entries.add(item);
							}
						})
						.build());
	}

	public static void init() {
	}

	private ModItemGroups() {
	}
}
