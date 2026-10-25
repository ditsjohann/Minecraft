package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.block.BombBlock;
import com.armageddon.block.BombType;
import com.armageddon.block.RadioactiveBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.piston.PistonBehavior;
import com.armageddon.item.WmdBlockItem;
import com.armageddon.tier.Tier;
import com.armageddon.tier.Tiers;
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
	public static final BombBlock TECTONIC_BOMB = bomb("tectonic_bomb", BombType.TECTONIC, Rarity.RARE);
	public static final BombBlock BASS_DROP_BOMB = bomb("bass_drop_bomb", BombType.BASS_DROP, Rarity.EPIC);
	public static final BombBlock CLUSTER_NUKE = bomb("cluster_nuke", BombType.CLUSTER, Rarity.EPIC);
	public static final BombBlock NOVA_BOMB = bomb("nova_bomb", BombType.NOVA, Rarity.EPIC);
	public static final BombBlock OMEGA_BOMB = bomb("omega_bomb", BombType.OMEGA, Rarity.EPIC);

	// ---------------------------------------------------------------- arsenal expansion: warheads
	public static final BombBlock HELIOS_CROWN_BOMB = tiered(bomb("helios_crown_bomb", BombType.HELIOS_CROWN, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock CONTINENTAL_PULSE = tiered(bomb("continental_pulse", BombType.CONTINENTAL, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock DAWNSTAR_PAYLOAD = tiered(bomb("dawnstar_payload", BombType.DAWNSTAR, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock BLOODSTAR_COLLAPSE = tiered(bomb("bloodstar_collapse", BombType.BLOODSTAR, Rarity.EPIC), Tier.MYTHIC);
	public static final BombBlock LAST_LIGHT = tiered(bomb("last_light", BombType.LAST_LIGHT, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock ZEROPOINT = tiered(bomb("zeropoint", BombType.ZEROPOINT, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock UNMAKER = tiered(bomb("unmaker", BombType.UNMAKER, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock GODFALL = tiered(bomb("godfall", BombType.GODFALL, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock FINAL_EXISTENCE = tiered(bomb("final_existence", BombType.FINAL_EXISTENCE, Rarity.EPIC), Tier.CELESTIAL);

	// ---------------------------------------------------------------- bomb expansion II
	public static final BombBlock DISCO_BOMB = tiered(bomb("disco_bomb", BombType.DISCO, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock GLITTER_BOMB = tiered(bomb("glitter_bomb", BombType.GLITTER, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock PUMPKIN_PATCH_BOMB = tiered(bomb("pumpkin_patch_bomb", BombType.PUMPKIN_PATCH, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock COBWEB_BOMB = tiered(bomb("cobweb_bomb", BombType.COBWEB, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock BALLOON_BOMB = tiered(bomb("balloon_bomb", BombType.BALLOON, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock SPRINKLER_BOMB = tiered(bomb("sprinkler_bomb", BombType.SPRINKLER, Rarity.UNCOMMON), Tier.UNCOMMON);
	public static final BombBlock THERMITE_BOMB = tiered(bomb("thermite_bomb", BombType.THERMITE, Rarity.RARE), Tier.RARE);
	public static final BombBlock TESLA_BOMB = tiered(bomb("tesla_bomb", BombType.TESLA, Rarity.RARE), Tier.RARE);
	public static final BombBlock SINKHOLE_BOMB = tiered(bomb("sinkhole_bomb", BombType.SINKHOLE, Rarity.RARE), Tier.RARE);
	public static final BombBlock TSUNAMI_BOMB = tiered(bomb("tsunami_bomb", BombType.TSUNAMI, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock VOLCANO_BOMB = tiered(bomb("volcano_bomb", BombType.VOLCANO, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock ICE_AGE_BOMB = tiered(bomb("ice_age_bomb", BombType.ICE_AGE, Rarity.EPIC), Tier.LEGENDARY);
	public static final BombBlock HELLMOUTH_BOMB = tiered(bomb("hellmouth_bomb", BombType.HELLMOUTH, Rarity.EPIC), Tier.MYTHIC);
	public static final BombBlock CYCLONE_BOMB = tiered(bomb("cyclone_bomb", BombType.CYCLONE, Rarity.EPIC), Tier.MYTHIC);
	public static final BombBlock DOOMSDAY_CLOCK = tiered(bomb("doomsday_clock", BombType.DOOMSDAY_CLOCK, Rarity.EPIC), Tier.MYTHIC);
	public static final BombBlock MOONFALL = tiered(bomb("moonfall", BombType.MOONFALL, Rarity.EPIC), Tier.CITY_DESTRUCTION);
	public static final BombBlock BIG_BANG = tiered(bomb("big_bang", BombType.BIG_BANG, Rarity.EPIC), Tier.CITY_DESTRUCTION);
	public static final BombBlock WORLD_EATER = tiered(bomb("world_eater", BombType.WORLD_EATER, Rarity.EPIC), Tier.CITY_DESTRUCTION);
	public static final BombBlock CHRONO_BREAK = tiered(bomb("chrono_break", BombType.CHRONO_BREAK, Rarity.EPIC), Tier.CITY_DESTRUCTION);
	public static final BombBlock RAGNAROK = tiered(bomb("ragnarok", BombType.RAGNAROK, Rarity.EPIC), Tier.CITY_DESTRUCTION);
	public static final BombBlock EXPLODING_COWS = tiered(bomb("exploding_cows", BombType.EXPLODING_COWS, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock DEADLY_TWISTER = tiered(bomb("deadly_twister", BombType.DEADLY_TWISTER, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock COLOSSAL_SPEAR = tiered(bomb("colossal_spear", BombType.COLOSSAL_SPEAR, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock ZOMBIE_APOCALYPSE = tiered(bomb("zombie_apocalypse", BombType.ZOMBIE_APOCALYPSE, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock UFO_INVASION = tiered(bomb("ufo_invasion", BombType.UFO_INVASION, Rarity.EPIC), Tier.CELESTIAL);
	public static final BombBlock GALACTIC_SMASH = tiered(bomb("galactic_smash", BombType.GALACTIC_SMASH, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock BOOMING_TICK = tiered(bomb("booming_tick", BombType.BOOMING_TICK, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock ALIEN_DEATH_RAY = tiered(bomb("alien_death_ray", BombType.ALIEN_DEATH_RAY, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock ETERNAL_SAPLING = tiered(bomb("eternal_sapling", BombType.ETERNAL_SAPLING, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock SIXTEEN_CHUNKS = tiered(bomb("sixteen_chunks", BombType.SIXTEEN_CHUNKS, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock TRUMAN_SHOW = tiered(bomb("truman_show", BombType.TRUMAN_SHOW, Rarity.EPIC), Tier.WORLD_DESTRUCTION);
	public static final BombBlock DISCO_BALL = tiered(bomb("disco_ball", BombType.DISCO_BALL, Rarity.EPIC), Tier.WORLD_DESTRUCTION);

	// ---------------------------------------------------------------- arsenal expansion: aftermath blocks
	public static final Block ENERGY_CRYSTAL = register("energy_crystal", new TransparentBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.PURPLE).strength(1.5F).luminance(s -> 12).nonOpaque().sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.allowsSpawning((st, w, p, t) -> false).solidBlock((st, w, p) -> false).suffocates((st, w, p) -> false).blockVision((st, w, p) -> false)), Rarity.RARE);
	public static final Block LUMINOUS_GLASS = register("luminous_glass", new TransparentBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.WHITE).strength(0.6F).luminance(s -> 15).nonOpaque().sounds(BlockSoundGroup.GLASS)
			.allowsSpawning((st, w, p, t) -> false).solidBlock((st, w, p) -> false).suffocates((st, w, p) -> false).blockVision((st, w, p) -> false)), Rarity.EPIC);
	public static final Block GOLD_FISSURE = register("gold_fissure", new MagmaBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.GOLD).strength(1.0F).luminance(s -> 13).sounds(BlockSoundGroup.NETHER_GOLD_ORE).ticksRandomly()
			.allowsSpawning((st, w, p, t) -> false).postProcess((st, w, p) -> true).emissiveLighting((st, w, p) -> true)), Rarity.RARE);
	public static final Block CRIMSON_FISSURE = register("crimson_fissure", new MagmaBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.DARK_RED).strength(1.0F).luminance(s -> 10).sounds(BlockSoundGroup.NETHERRACK).ticksRandomly()
			.allowsSpawning((st, w, p, t) -> false).postProcess((st, w, p) -> true).emissiveLighting((st, w, p) -> true)), Rarity.EPIC);
	public static final Block VOID_REMNANT = register("void_remnant", new Block(AbstractBlock.Settings.create()
			.mapColor(MapColor.BLACK).strength(3.0F, 1200.0F).luminance(s -> 4).sounds(BlockSoundGroup.SCULK)
			.emissiveLighting((st, w, p) -> true)), Rarity.EPIC);
	public static final Block MOLTEN_CORE = register("molten_core", new MagmaBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.ORANGE).strength(0.5F).luminance(s -> 15).sounds(BlockSoundGroup.NETHERRACK)
			.allowsSpawning((st, w, p, t) -> false).postProcess((st, w, p) -> true).emissiveLighting((st, w, p) -> true)), Rarity.UNCOMMON);

	private static BombBlock tiered(BombBlock block, Tier tier) {
		Tiers.set(BLOCK_ITEMS.get(BLOCK_ITEMS.size() - 1), tier);
		return block;
	}

	private static BombBlock bomb(String name, BombType type, Rarity rarity) {
		BombBlock block = register(name, new BombBlock(type, AbstractBlock.Settings.create().mapColor(MapColor.GRAY)
				.strength(2.0F, type.needsConfirmation() ? 3600000.0F : 1.0F).nonOpaque().sounds(BlockSoundGroup.NETHERITE).pistonBehavior(PistonBehavior.DESTROY)), rarity);
		BOMBS.put(type, block);
		return block;
	}

	private static <T extends Block> T register(String name, T block, Rarity rarity) {
		Identifier id = Armageddon.id(name);
		Registry.register(Registries.BLOCK, id, block);
		Item item = Registry.register(Registries.ITEM, id, new WmdBlockItem(block, new Item.Settings().rarity(rarity)));
		if (item instanceof net.minecraft.item.BlockItem bi) {
			bi.appendBlocks(Item.BLOCK_ITEMS, item);
		}
		BLOCK_ITEMS.add(item);
		return block;
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
