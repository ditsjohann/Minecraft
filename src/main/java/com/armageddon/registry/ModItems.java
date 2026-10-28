package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.StrikeMarkerEntity;
import com.armageddon.item.*;
import com.armageddon.tier.Tier;
import com.armageddon.tier.Tiers;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	/** Every mod item in creative-tab order. */
	public static final List<Item> ALL = new ArrayList<>();

	public static final TagKey<Block> MINEABLE_PICKAXE = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("mineable/pickaxe"));
	public static final TagKey<Block> MINEABLE_SHOVEL = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("mineable/shovel"));
	public static final TagKey<Block> MINEABLE_DRILL = TagKey.of(RegistryKeys.BLOCK, Armageddon.id("mineable/drill"));

	// ---------------------------------------------------------------- materials
	public static final Item RAW_URANIUM = register("raw_uranium", new WmdItem(new Item.Settings()));
	public static final Item URANIUM_INGOT = register("uranium_ingot", new WmdItem(new Item.Settings()));
	public static final Item ENRICHED_URANIUM = register("enriched_uranium", new WmdItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
	public static final Item PLUTONIUM_CORE = register("plutonium_core", new WmdItem(new Item.Settings().rarity(Rarity.RARE), true));
	public static final Item RAW_TITANIUM = register("raw_titanium", new WmdItem(new Item.Settings()));
	public static final Item TITANIUM_INGOT = register("titanium_ingot", new WmdItem(new Item.Settings()));
	public static final Item TITANIUM_PLATE = register("titanium_plate", new WmdItem(new Item.Settings()));
	public static final Item CIRCUIT_BOARD = register("circuit_board", new WmdItem(new Item.Settings()));
	public static final Item FUSION_CORE = register("fusion_core", new WmdItem(new Item.Settings().rarity(Rarity.RARE)));
	public static final Item ANTIMATTER_CELL = register("antimatter_cell", new WmdItem(new Item.Settings().rarity(Rarity.EPIC).maxCount(16), true));
	public static final Item SINGULARITY_CORE = register("singularity_core", new WmdItem(new Item.Settings().rarity(Rarity.EPIC).maxCount(1), true));

	// ---------------------------------------------------------------- ammunition
	public static final Item MINI_NUKE = register("mini_nuke", new WmdItem(new Item.Settings().rarity(Rarity.RARE).maxCount(8)));
	public static final Item ROCKET = register("rocket", new WmdItem(new Item.Settings().maxCount(16)));
	public static final Item PLASMA_CELL = register("plasma_cell", new WmdItem(new Item.Settings().maxCount(32)));
	public static final Item RAILGUN_SLUG = register("railgun_slug", new WmdItem(new Item.Settings().maxCount(32)));
	public static final Item BULLET_BELT = register("bullet_belt", new WmdItem(new Item.Settings().maxCount(16)));
	public static final Item FUEL_CANISTER = register("fuel_canister", new WmdItem(new Item.Settings().maxCount(16)));
	public static final Item CRYO_CANISTER = register("cryo_canister", new WmdItem(new Item.Settings().maxCount(16)));

	// ---------------------------------------------------------------- heavy weapons
	public static final Item NUKE_LAUNCHER = tier(register("nuke_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> MINI_NUKE, 1, 80, 40, ProjectileKind.MINI_NUKE, 1.9F, 0.0F, 0.9, false, () -> ModSounds.NUKE_LAUNCH)), Tier.EPIC);
	public static final Item ROCKET_LAUNCHER = tier(register("rocket_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.RARE),
			() -> ROCKET, 4, 12, 50, ProjectileKind.ROCKET, 1.5F, 0.5F, 0.3, true, () -> ModSounds.ROCKET_LAUNCH)), Tier.RARE);
	public static final Item MIRV_LAUNCHER = tier(register("mirv_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC),
			() -> ROCKET, 1, 60, 50, ProjectileKind.MIRV, 1.4F, 0.0F, 0.5, false, () -> ModSounds.ROCKET_LAUNCH)), Tier.EPIC);
	public static final Item PLASMA_RIFLE = tier(register("plasma_rifle", new PlasmaRifleItem(new Item.Settings().rarity(Rarity.RARE))), Tier.RARE);
	public static final Item RAILGUN = tier(register("railgun", new RailgunItem(new Item.Settings().rarity(Rarity.EPIC))), Tier.EPIC);
	public static final Item MINIGUN = tier(register("minigun", new MinigunItem(new Item.Settings().rarity(Rarity.RARE))), Tier.RARE);
	public static final Item FLAMETHROWER = tier(register("flamethrower", new FlamethrowerItem(new Item.Settings().rarity(Rarity.UNCOMMON).fireproof())), Tier.UNCOMMON);
	public static final Item ARC_THROWER = tier(register("arc_thrower", new ArcThrowerItem(new Item.Settings().rarity(Rarity.RARE))), Tier.RARE);
	public static final Item CRYO_CANNON = tier(register("cryo_cannon", new LauncherItem(new Item.Settings().rarity(Rarity.RARE),
			() -> CRYO_CANISTER, 4, 25, 40, ProjectileKind.CRYO, 2.2F, 0.3F, 0.15, false, () -> ModSounds.CRYO_BLAST)), Tier.RARE);
	public static final Item SINGULARITY_PROJECTOR = tier(register("singularity_projector", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> ANTIMATTER_CELL, 1, 160, 60, ProjectileKind.SINGULARITY, 0.9F, 0.0F, 0.2, false, () -> ModSounds.BLACK_HOLE)), Tier.EPIC);
	public static final Item ORBITAL_DESIGNATOR = tier(register("orbital_designator", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(24).fireproof(),
			StrikeMarkerEntity.ORBITAL, 384, 300)), Tier.LEGENDARY);
	public static final Item STORM_CALLER = tier(register("storm_caller", new TargetingItem(new Item.Settings().rarity(Rarity.RARE).maxDamage(48),
			StrikeMarkerEntity.STORM, 128, 160)), Tier.EPIC);
	public static final Item GRAVITY_HAMMER = tier(register("gravity_hammer", new GravityHammerItem(ModMaterials.TITANIUM,
			new Item.Settings().rarity(Rarity.EPIC).attributeModifiers(SwordItem.createAttributeModifiers(ModMaterials.TITANIUM, 10, -3.2F)))), Tier.RARE);
	public static final Item PLASMA_BLADE = tier(register("plasma_blade", new PlasmaBladeItem(ModMaterials.PLASMA,
			new Item.Settings().rarity(Rarity.EPIC).fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ModMaterials.PLASMA, 6, -2.2F)))), Tier.EPIC);

	// ---------------------------------------------------------------- insane tier
	public static final Item DUBSTEP_GUN = tier(register("dubstep_gun", new DubstepGunItem(new Item.Settings().rarity(Rarity.EPIC))), Tier.EPIC);
	public static final Item DEATH_RAY = tier(register("death_ray", new DeathRayItem(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.LEGENDARY);
	public static final Item APOCALYPSE_GATLING = tier(register("apocalypse_gatling", new GatlingItem(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.MYTHIC);
	public static final Item ANNIHILATOR_CANNON = tier(register("annihilator_cannon", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> ANTIMATTER_CELL, 1, 100, 60, ProjectileKind.BFG, 0.75F, 0.0F, 0.6, false, () -> ModSounds.BLACK_HOLE)), Tier.LEGENDARY);
	public static final Item METEOR_STAFF = tier(register("meteor_staff", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(32).fireproof(),
			StrikeMarkerEntity.METEOR, 192, 240)), Tier.LEGENDARY);
	public static final Item KINETIC_DESIGNATOR = tier(register("kinetic_designator", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(16).fireproof(),
			StrikeMarkerEntity.KINETIC, 384, 300)), Tier.LEGENDARY);
	public static final Item AIRSTRIKE_BEACON = tier(register("airstrike_beacon", new TargetingItem(new Item.Settings().rarity(Rarity.RARE).maxDamage(16),
			StrikeMarkerEntity.AIRSTRIKE, 256, 300)), Tier.EPIC);
	public static final Item NUKE_BARRAGE = tier(register("nuke_barrage", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(8).fireproof(),
			StrikeMarkerEntity.BARRAGE, 384, 600)), Tier.MYTHIC);
	public static final Item CITY_CAPSULE = register("city_capsule", new CityCapsuleItem(new Item.Settings().rarity(Rarity.EPIC)));
	public static final Item OVERDRIVE_STIM = register("overdrive_stim", new ConsumableItem(new Item.Settings().rarity(Rarity.RARE), ConsumableItem.Kind.OVERDRIVE));

	// ---------------------------------------------------------------- grenades
	public static final Item FRAG_GRENADE = tier(register("frag_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.FRAG)), Tier.UNCOMMON);
	public static final Item PLASMA_GRENADE = tier(register("plasma_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.PLASMA_GRENADE)), Tier.RARE);
	public static final Item CRYO_GRENADE = tier(register("cryo_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.CRYO_GRENADE)), Tier.UNCOMMON);
	public static final Item INCENDIARY_GRENADE = tier(register("incendiary_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.INCENDIARY)), Tier.UNCOMMON);
	public static final Item GRAVITY_GRENADE = tier(register("gravity_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.RARE), ProjectileKind.GRAVITY)), Tier.RARE);
	public static final Item TOXIC_GRENADE = tier(register("toxic_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.TOXIC)), Tier.RARE);
	public static final Item POCKET_NUKE = tier(register("pocket_nuke", new GrenadeItem(new Item.Settings().rarity(Rarity.EPIC), ProjectileKind.POCKET_NUKE)), Tier.EPIC);

	// ---------------------------------------------------------------- tools & gadgets
	public static final Item ATOMIC_PICKAXE = register("atomic_pickaxe", new AreaToolItem(ModMaterials.TITANIUM, MINEABLE_PICKAXE,
			new Item.Settings().rarity(Rarity.RARE).attributeModifiers(MiningToolItem.createAttributeModifiers(ModMaterials.TITANIUM, 1.0F, -2.8F)), 1, 1));
	public static final Item FUSION_SHOVEL = register("fusion_shovel", new AreaToolItem(ModMaterials.TITANIUM, MINEABLE_SHOVEL,
			new Item.Settings().rarity(Rarity.RARE).attributeModifiers(MiningToolItem.createAttributeModifiers(ModMaterials.TITANIUM, 1.5F, -3.0F)), 1, 1));
	public static final Item PLASMA_CUTTER = register("plasma_cutter", new PlasmaCutterItem(ModMaterials.PLASMA,
			new Item.Settings().rarity(Rarity.RARE).fireproof().attributeModifiers(MiningToolItem.createAttributeModifiers(ModMaterials.PLASMA, 5.0F, -3.0F))));
	public static final Item QUANTUM_DRILL = register("quantum_drill", new AreaToolItem(ModMaterials.PLASMA, MINEABLE_DRILL,
			new Item.Settings().rarity(Rarity.EPIC).fireproof().attributeModifiers(MiningToolItem.createAttributeModifiers(ModMaterials.PLASMA, 2.0F, -2.6F)), 1, 3));
	public static final Item DETONATOR = register("detonator", new DetonatorItem(new Item.Settings().rarity(Rarity.UNCOMMON)));
	public static final Item GEIGER_COUNTER = register("geiger_counter", new WmdItem(new Item.Settings().maxCount(1)));
	public static final Item RAD_X = register("rad_x", new ConsumableItem(new Item.Settings(), ConsumableItem.Kind.RAD_X));
	public static final Item NANO_MEDKIT = register("nano_medkit", new ConsumableItem(new Item.Settings().rarity(Rarity.UNCOMMON), ConsumableItem.Kind.MEDKIT));

	// ================================================================ ARSENAL EXPANSION
	// ---------------------------------------------------------------- support gear & ammunition
	public static final Item SOLAR_CELL = tier(register("solar_cell", new WmdItem(new Item.Settings().maxCount(16), true)), Tier.RARE);
	public static final Item RIFT_SHARD = tier(register("rift_shard", new WmdItem(new Item.Settings(), true)), Tier.EPIC);
	public static final Item CELESTIAL_CORE = tier(register("celestial_core", new WmdItem(new Item.Settings().maxCount(1).fireproof(), true)), Tier.CELESTIAL);
	public static final Item WARDEN_WAND = tier(register("warden_wand", new WardenWandItem(new Item.Settings())), Tier.RARE);

	// ---------------------------------------------------------------- uncommon: chaos, no casualties
	public static final Item PARTY_POPPER = tier(register("party_popper", new LauncherItem(new Item.Settings(),
			() -> net.minecraft.item.Items.FIREWORK_ROCKET, 6, 8, 30, ProjectileKind.PARTY_ROCKET, 1.6F, 2.0F, 0.1, false,
			() -> net.minecraft.sound.SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH)), Tier.FUN);
	public static final Item BOUNCE_BOMB = tier(register("bounce_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.BOUNCE_BOMB)), Tier.FUN);
	public static final Item LOOT_TORNADO = tier(register("loot_tornado", new GrenadeItem(new Item.Settings(), ProjectileKind.LOOT_TORNADO)), Tier.FUN);
	public static final Item MOONWALK_MINE = tier(register("moonwalk_mine", new GrenadeItem(new Item.Settings(), ProjectileKind.MOONWALK_MINE)), Tier.FUN);
	public static final Item SNOW_GLOBE_GRENADE = tier(register("snow_globe_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.SNOW_GLOBE)), Tier.FUN);
	public static final Item SLIMEQUAKE_BOMB = tier(register("slimequake_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.SLIMEQUAKE)), Tier.FUN);
	public static final Item HOLOGRAM_BOMB = tier(register("hologram_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.HOLOGRAM)), Tier.FUN);
	public static final Item MAGNET_MAYHEM = tier(register("magnet_mayhem", new GrenadeItem(new Item.Settings(), ProjectileKind.MAGNET)), Tier.FUN);
	public static final Item AIRHORN_CANNON = tier(register("airhorn_cannon", new AirhornItem(new Item.Settings())), Tier.FUN);
	public static final Item RAINBOW_PAINT_BOMB = tier(register("rainbow_paint_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.PAINT_BOMB)), Tier.FUN);

	// ---------------------------------------------------------------- rare: tactical control
	public static final Item SHATTERFROST_BOMB = tier(register("shatterfrost_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.SHATTERFROST)), Tier.RARE);
	public static final Item ARCFLASH_CHARGE = tier(register("arcflash_charge", new GrenadeItem(new Item.Settings(), ProjectileKind.ARCFLASH)), Tier.RARE);
	public static final Item SEISMIC_PULSE_MINE = tier(register("seismic_pulse_mine", new GrenadeItem(new Item.Settings(), ProjectileKind.SEISMIC_MINE)), Tier.RARE);
	public static final Item IMPLOSION_GRENADE = tier(register("implosion_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.IMPLOSION)), Tier.RARE);
	public static final Item MICROSTRIKE_BEACON = tier(register("microstrike_beacon", new CallerItem(new Item.Settings().maxDamage(16),
			160, 200, false, Strikes::microstrike)), Tier.RARE);
	public static final Item PLASMA_BOOMERANG = tier(register("plasma_boomerang", new BoomerangItem(new Item.Settings())), Tier.RARE);
	public static final Item GRAVITY_TETHER_GUN = tier(register("gravity_tether_gun", new TetherItem(new Item.Settings())), Tier.RARE);
	public static final Item ION_FOG_CANISTER = tier(register("ion_fog_canister", new GrenadeItem(new Item.Settings(), ProjectileKind.ION_FOG)), Tier.RARE);
	public static final Item STASIS_MINE = tier(register("stasis_mine", new GrenadeItem(new Item.Settings(), ProjectileKind.STASIS)), Tier.RARE);
	public static final Item EMP_DISRUPTOR = tier(register("emp_disruptor", new GrenadeItem(new Item.Settings(), ProjectileKind.EMP)), Tier.RARE);

	// ---------------------------------------------------------------- epic: experimental physics
	public static final Item ANTIMATTER_LANCE = tier(register("antimatter_lance", new AntimatterLanceItem(new Item.Settings().fireproof())), Tier.EPIC);
	public static final Item CATACLYSM_MORTAR = tier(register("cataclysm_mortar", new LauncherItem(new Item.Settings().fireproof(),
			() -> ROCKET, 1, 50, 40, ProjectileKind.MORTAR, 1.6F, 0.5F, 0.4, false, () -> ModSounds.NUKE_LAUNCH)), Tier.EPIC);
	public static final Item QUANTUM_FRACTURE_BOMB = tier(register("quantum_fracture_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.QUANTUM)), Tier.EPIC);
	public static final Item RIFT_BORER = tier(register("rift_borer", new RiftBorerItem(new Item.Settings().fireproof())), Tier.EPIC);
	public static final Item CRYSTAL_CATACLYSM = tier(register("crystal_cataclysm", new GrenadeItem(new Item.Settings(), ProjectileKind.CRYSTAL_SEED)), Tier.EPIC);
	public static final Item VOIDSTORM_GENERATOR = tier(register("voidstorm_generator", new GrenadeItem(new Item.Settings(), ProjectileKind.VOIDSTORM)), Tier.EPIC);
	public static final Item SIEGEBREAKER_ARRAY = tier(register("siegebreaker_array", new CallerItem(new Item.Settings().maxDamage(24).fireproof(),
			192, 160, false, Strikes::siegebreaker)), Tier.EPIC);
	public static final Item MICRO_EVENT_HORIZON = tier(register("micro_event_horizon", new GrenadeItem(new Item.Settings(), ProjectileKind.MICRO_HORIZON)), Tier.EPIC);
	public static final Item RESONANCE_CANNON = tier(register("resonance_cannon", new ResonanceCannonItem(new Item.Settings())), Tier.EPIC);
	public static final Item MIRROR_DIMENSION_BOMB = tier(register("mirror_dimension_bomb", new GrenadeItem(new Item.Settings(), ProjectileKind.MIRROR)), Tier.EPIC);

	// ---------------------------------------------------------------- legendary: superweapons
	public static final Item SOLSTICE_SUNLANCE = tier(register("solstice_sunlance", new SunlanceItem(new Item.Settings().fireproof())), Tier.LEGENDARY);

	// Plasma and sci-fi: the first hand-held weapons in the cataclysm tiers.
	public static final Item HELIX_DISRUPTOR = tier(register("helix_disruptor", new com.armageddon.item.SciWeapons.HelixDisruptor(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.CELESTIAL);
	public static final Item GRAVITON_LANCE = tier(register("graviton_lance", new com.armageddon.item.SciWeapons.GravitonLance(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.CITY_DESTRUCTION);
	public static final Item SUNFALL_MORTAR = tier(register("sunfall_mortar", new com.armageddon.item.SciWeapons.SunfallMortar(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.CITY_DESTRUCTION);
	public static final Item HALO_CUTTER = tier(register("halo_cutter", new com.armageddon.item.SciWeapons.HaloCutter(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.WORLD_DESTRUCTION);
	public static final Item OBLITERATOR = tier(register("obliterator", new com.armageddon.item.SciWeapons.Obliterator(new Item.Settings().rarity(Rarity.EPIC).fireproof())), Tier.WORLD_DESTRUCTION);
	public static final Item WORLDSPLITTER_HAMMER = tier(register("worldsplitter_hammer", new WorldsplitterItem(ModMaterials.PLASMA,
			new Item.Settings().fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ModMaterials.PLASMA, 14, -3.3F)))), Tier.LEGENDARY);
	public static final Item SOLAR_VERDICT_BEACON = tier(register("solar_verdict_beacon", new CallerItem(new Item.Settings().maxDamage(8).fireproof(),
			256, 600, false, CallerItem.event("solar_verdict"))), Tier.LEGENDARY);
	public static final Item TITANFALL_WARHEAD = tier(register("titanfall_warhead", new CallerItem(new Item.Settings().maxDamage(8).fireproof(),
			256, 400, false, Strikes::titanfall)), Tier.LEGENDARY);
	public static final Item MIDAS_CASCADE = tier(register("midas_cascade", new LauncherItem(new Item.Settings().fireproof(),
			() -> net.minecraft.item.Items.GOLD_BLOCK, 1, 80, 40, ProjectileKind.MIDAS, 1.4F, 0.0F, 0.6, false, () -> ModSounds.PLASMA_FIRE)), Tier.LEGENDARY);
	public static final Item REALITY_SHEAR_PROJECTOR = tier(register("reality_shear_projector", new CallerItem(new Item.Settings().maxDamage(8).fireproof(),
			128, 600, true, CallerItem.event("reality_shear"))), Tier.LEGENDARY);
	public static final Item JUDGMENT_ENGINE = tier(register("judgment_engine", new JudgmentEngineItem(new Item.Settings().fireproof())), Tier.LEGENDARY);

	// ---------------------------------------------------------------- mythic: catastrophes
	public static final Item RIFTFALL_ENGINE = tier(register("riftfall_engine", new CallerItem(new Item.Settings().maxDamage(4).fireproof(),
			384, 1200, true, CallerItem.event("riftfall"))), Tier.MYTHIC);
	public static final Item APOCALYPSE_SPINDLE = tier(register("apocalypse_spindle", new CallerItem(new Item.Settings().maxDamage(4).fireproof(),
			384, 1200, true, CallerItem.event("spindle"))), Tier.MYTHIC);
	public static final Item DEVOURER_CORE = tier(register("devourer_core", new GrenadeItem(new Item.Settings().fireproof(), ProjectileKind.DEVOURER)), Tier.MYTHIC);
	public static final Item EXTINCTION_CHOIR = tier(register("extinction_choir", new CallerItem(new Item.Settings().maxDamage(4).fireproof(),
			384, 1200, true, CallerItem.event("extinction"))), Tier.MYTHIC);

	// ---------------------------------------------------------------- armour
	public static final Item HAZMAT_HELMET = armor("hazmat_helmet", ModMaterials.HAZMAT, ArmorItem.Type.HELMET, 20, "hazmat", Rarity.UNCOMMON);
	public static final Item HAZMAT_SUIT = armor("hazmat_suit", ModMaterials.HAZMAT, ArmorItem.Type.CHESTPLATE, 20, "hazmat", Rarity.UNCOMMON);
	public static final Item HAZMAT_LEGGINGS = armor("hazmat_leggings", ModMaterials.HAZMAT, ArmorItem.Type.LEGGINGS, 20, "hazmat", Rarity.UNCOMMON);
	public static final Item HAZMAT_BOOTS = armor("hazmat_boots", ModMaterials.HAZMAT, ArmorItem.Type.BOOTS, 20, "hazmat", Rarity.UNCOMMON);
	public static final Item JUGGERNAUT_HELMET = armor("juggernaut_helmet", ModMaterials.JUGGERNAUT, ArmorItem.Type.HELMET, 45, "juggernaut", Rarity.EPIC);
	public static final Item JUGGERNAUT_CHESTPLATE = armor("juggernaut_chestplate", ModMaterials.JUGGERNAUT, ArmorItem.Type.CHESTPLATE, 45, "juggernaut", Rarity.EPIC);
	public static final Item JUGGERNAUT_LEGGINGS = armor("juggernaut_leggings", ModMaterials.JUGGERNAUT, ArmorItem.Type.LEGGINGS, 45, "juggernaut", Rarity.EPIC);
	public static final Item JUGGERNAUT_BOOTS = armor("juggernaut_boots", ModMaterials.JUGGERNAUT, ArmorItem.Type.BOOTS, 45, "juggernaut", Rarity.EPIC);
	public static final Item THRUSTER_PACK = armor("thruster_pack", ModMaterials.THRUSTER, ArmorItem.Type.CHESTPLATE, 35, "thruster", Rarity.EPIC);
	public static final Item TITAN_BOOTS = armor("titan_boots", ModMaterials.TITAN, ArmorItem.Type.BOOTS, 40, "titan", Rarity.EPIC);

	private static Item armor(String name, RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, int durability, String set, Rarity rarity) {
		return register(name, new WmdArmorItem(material, type, new Item.Settings().rarity(rarity).maxDamage(type.getMaxDamage(durability)), set));
	}

	private static <T extends Item> T tier(T item, Tier tier) {
		return Tiers.set(item, tier);
	}

	private static <T extends Item> T register(String name, T item) {
		Registry.register(Registries.ITEM, Armageddon.id(name), item);
		ALL.add(item);
		return item;
	}

	public static void init() {
	}

	private ModItems() {
	}
}
