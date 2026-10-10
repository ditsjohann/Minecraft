package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.block.BombBlock;
import com.armageddon.block.BombType;
import com.armageddon.block.RadioactiveBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.piston.PistonBehavior;
import com.armageddon.item.WmdBlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
	public static final List<Item> BLOCK_ITEMS = new ArrayList<>();
	public static final Map<BombType, BombBlock> BOMBS = new EnumMap<>(BombType.class);

	// --- Ores & storage ---
	public static final Block URANIUM_ORE = register("uranium_ore", new ExperienceDroppingBlock(UniformIntProvider.create(2, 5),
			AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(3.0F, 3.0F).requiresTool().luminance(s -> 3)), Rarity.COMMON);
	public static final Block DEEPSLATE_URANIUM_ORE = register("deepslate_uranium_ore", new ExperienceDroppingBlock(UniformIntProvider.create(2, 5),
			AbstractBlock.Settings.create().mapColor(MapColor.DEEPSLATE_GRAY).strength(4.5F, 3.0F).requiresTool().luminance(s -> 3).sounds(BlockSoundGroup.DEEPSLATE)), Rarity.COMMON);
	public static final Block TITANIUM_ORE = register("titanium_ore", new ExperienceDroppingBlock(UniformIntProvider.create(1, 3),
			AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).strength(3.0F, 3.0F).requiresTool()), Rarity.COMMON);
	public static final Block DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore", new ExperienceDroppingBlock(UniformIntProvider.create(1, 3),
			AbstractBlock.Settings.create().mapColor(MapColor.DEEPSLATE_GRAY).strength(4.5F, 3.0F).requiresTool().sounds(BlockSoundGroup.DEEPSLATE)), Rarity.COMMON);
	public static final Block URANIUM_BLOCK = register("uranium_block", new RadioactiveBlock(2,
			AbstractBlock.Settings.create().mapColor(MapColor.LIME).strength(5.0F, 6.0F).requiresTool().luminance(s -> 10).sounds(BlockSoundGroup.METAL)), Rarity.UNCOMMON);
	public static final Block TITANIUM_BLOCK = register("titanium_block", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(6.0F, 12.0F).requiresTool().sounds(BlockSoundGroup.NETHERITE)), Rarity.COMMON);

	// --- Aftermath / construction ---
	public static final Block TRINITITE = register("trinitite", new RadioactiveBlock(1,
			AbstractBlock.Settings.create().mapColor(MapColor.EMERALD_GREEN).strength(1.5F, 4.0F).requiresTool().luminance(s -> 6).sounds(BlockSoundGroup.GLASS)), Rarity.COMMON);
	public static final Block SCORCHED_EARTH = register("scorched_earth", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(0.6F).sounds(BlockSoundGroup.GRAVEL)), Rarity.COMMON);
	public static final Block CHARRED_LOG = register("charred_log", new PillarBlock(
			AbstractBlock.Settings.create().mapColor(MapColor.BLACK).strength(1.5F).sounds(BlockSoundGroup.WOOD)), Rarity.COMMON);
	public static final Block REINFORCED_CONCRETE = register("reinforced_concrete", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY).strength(40.0F, 3000.0F).requiresTool().pistonBehavior(PistonBehavior.BLOCK)), Rarity.UNCOMMON);
	public static final Block HAZARD_BLOCK = register("hazard_block", new Block(
			AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(5.0F, 6.0F).requiresTool().sounds(BlockSoundGroup.METAL)), Rarity.COMMON);

	// --- Warheads ---
	public static final BombBlock DEMOLITION_CHARGE = bomb("demolition_charge", BombType.DEMOLITION_CHARGE, Rarity.COMMON);
	public static final BombBlock THERMOBARIC_BOMB = bomb("thermobaric_bomb", BombType.THERMOBARIC, Rarity.UNCOMMON);
	public static final BombBlock CRYO_BOMB = bomb("cryo_bomb", BombType.CRYO, Rarity.UNCOMMON);
	public static final BombBlock SINGULARITY_BOMB = bomb("singularity_bomb", BombType.SINGULARITY, Rarity.EPIC);
	public static final BombBlock NEUTRON_BOMB = bomb("neutron_bomb", BombType.NEUTRON, Rarity.RARE);
	public static final BombBlock ATOMIC_BOMB = bomb("atomic_bomb", BombType.ATOMIC, Rarity.RARE);
	public static final BombBlock ANTIMATTER_BOMB = bomb("antimatter_bomb", BombType.ANTIMATTER, Rarity.EPIC);
	public static final BombBlock HYDROGEN_BOMB = bomb("hydrogen_bomb", BombType.HYDROGEN, Rarity.EPIC);

	private static BombBlock bomb(String name, BombType type, Rarity rarity) {
		BombBlock block = register(name, new BombBlock(type, AbstractBlock.Settings.create().mapColor(MapColor.GRAY)
				.strength(2.0F, 1.0F).nonOpaque().sounds(BlockSoundGroup.NETHERITE).pistonBehavior(PistonBehavior.DESTROY)), rarity);
		BOMBS.put(type, block);
		return block;
	}

	private static <T extends Block> T register(String name, T block, Rarity rarity) {
		Identifier id = Armageddon.id(name);
		Registry.register(Registries.BLOCK, id, block);
		Item item = Registry.register(Registries.ITEM, id, new WmdBlockItem(block, new Item.Settings().rarity(rarity)));
		BLOCK_ITEMS.add(item);
		return block;
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
