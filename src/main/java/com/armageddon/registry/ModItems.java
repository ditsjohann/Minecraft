package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.StrikeMarkerEntity;
import com.armageddon.item.*;
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
	public static final Item NUKE_LAUNCHER = register("nuke_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> MINI_NUKE, 1, 80, 40, ProjectileKind.MINI_NUKE, 1.9F, 0.0F, 0.9, false, () -> ModSounds.NUKE_LAUNCH));
	public static final Item ROCKET_LAUNCHER = register("rocket_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.RARE),
			() -> ROCKET, 4, 12, 50, ProjectileKind.ROCKET, 1.5F, 0.5F, 0.3, true, () -> ModSounds.ROCKET_LAUNCH));
	public static final Item MIRV_LAUNCHER = register("mirv_launcher", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC),
			() -> ROCKET, 1, 60, 50, ProjectileKind.MIRV, 1.4F, 0.0F, 0.5, false, () -> ModSounds.ROCKET_LAUNCH));
	public static final Item PLASMA_RIFLE = register("plasma_rifle", new PlasmaRifleItem(new Item.Settings().rarity(Rarity.RARE)));
	public static final Item RAILGUN = register("railgun", new RailgunItem(new Item.Settings().rarity(Rarity.EPIC)));
	public static final Item MINIGUN = register("minigun", new MinigunItem(new Item.Settings().rarity(Rarity.RARE)));
	public static final Item FLAMETHROWER = register("flamethrower", new FlamethrowerItem(new Item.Settings().rarity(Rarity.UNCOMMON).fireproof()));
	public static final Item ARC_THROWER = register("arc_thrower", new ArcThrowerItem(new Item.Settings().rarity(Rarity.RARE)));
	public static final Item CRYO_CANNON = register("cryo_cannon", new LauncherItem(new Item.Settings().rarity(Rarity.RARE),
			() -> CRYO_CANISTER, 4, 25, 40, ProjectileKind.CRYO, 2.2F, 0.3F, 0.15, false, () -> ModSounds.CRYO_BLAST));
	public static final Item SINGULARITY_PROJECTOR = register("singularity_projector", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> ANTIMATTER_CELL, 1, 160, 60, ProjectileKind.SINGULARITY, 0.9F, 0.0F, 0.2, false, () -> ModSounds.BLACK_HOLE));
	public static final Item ORBITAL_DESIGNATOR = register("orbital_designator", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(24).fireproof(),
			StrikeMarkerEntity.ORBITAL, 384, 300));
	public static final Item STORM_CALLER = register("storm_caller", new TargetingItem(new Item.Settings().rarity(Rarity.RARE).maxDamage(48),
			StrikeMarkerEntity.STORM, 128, 160));
	public static final Item GRAVITY_HAMMER = register("gravity_hammer", new GravityHammerItem(ModMaterials.TITANIUM,
			new Item.Settings().rarity(Rarity.EPIC).attributeModifiers(SwordItem.createAttributeModifiers(ModMaterials.TITANIUM, 10, -3.2F))));
	public static final Item PLASMA_BLADE = register("plasma_blade", new PlasmaBladeItem(ModMaterials.PLASMA,
			new Item.Settings().rarity(Rarity.EPIC).fireproof().attributeModifiers(SwordItem.createAttributeModifiers(ModMaterials.PLASMA, 6, -2.2F))));

	// ---------------------------------------------------------------- insane tier
	public static final Item DUBSTEP_GUN = register("dubstep_gun", new DubstepGunItem(new Item.Settings().rarity(Rarity.EPIC)));
	public static final Item DEATH_RAY = register("death_ray", new DeathRayItem(new Item.Settings().rarity(Rarity.EPIC).fireproof()));
	public static final Item APOCALYPSE_GATLING = register("apocalypse_gatling", new GatlingItem(new Item.Settings().rarity(Rarity.EPIC).fireproof()));
	public static final Item ANNIHILATOR_CANNON = register("annihilator_cannon", new LauncherItem(new Item.Settings().rarity(Rarity.EPIC).fireproof(),
			() -> ANTIMATTER_CELL, 1, 100, 60, ProjectileKind.BFG, 0.75F, 0.0F, 0.6, false, () -> ModSounds.BLACK_HOLE));
	public static final Item METEOR_STAFF = register("meteor_staff", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(32).fireproof(),
			StrikeMarkerEntity.METEOR, 192, 240));
	public static final Item KINETIC_DESIGNATOR = register("kinetic_designator", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(16).fireproof(),
			StrikeMarkerEntity.KINETIC, 384, 300));
	public static final Item AIRSTRIKE_BEACON = register("airstrike_beacon", new TargetingItem(new Item.Settings().rarity(Rarity.RARE).maxDamage(16),
			StrikeMarkerEntity.AIRSTRIKE, 256, 300));
	public static final Item NUKE_BARRAGE = register("nuke_barrage", new TargetingItem(new Item.Settings().rarity(Rarity.EPIC).maxDamage(8).fireproof(),
			StrikeMarkerEntity.BARRAGE, 384, 600));
	public static final Item CITY_CAPSULE = register("city_capsule", new CityCapsuleItem(new Item.Settings().rarity(Rarity.EPIC)));
	public static final Item OVERDRIVE_STIM = register("overdrive_stim", new ConsumableItem(new Item.Settings().rarity(Rarity.RARE), ConsumableItem.Kind.OVERDRIVE));

	// ---------------------------------------------------------------- grenades
	public static final Item FRAG_GRENADE = register("frag_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.FRAG));
	public static final Item PLASMA_GRENADE = register("plasma_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.PLASMA_GRENADE));
	public static final Item CRYO_GRENADE = register("cryo_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.CRYO_GRENADE));
	public static final Item INCENDIARY_GRENADE = register("incendiary_grenade", new GrenadeItem(new Item.Settings(), ProjectileKind.INCENDIARY));
	public static final Item GRAVITY_GRENADE = register("gravity_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.RARE), ProjectileKind.GRAVITY));
	public static final Item TOXIC_GRENADE = register("toxic_grenade", new GrenadeItem(new Item.Settings().rarity(Rarity.UNCOMMON), ProjectileKind.TOXIC));
	public static final Item POCKET_NUKE = register("pocket_nuke", new GrenadeItem(new Item.Settings().rarity(Rarity.EPIC), ProjectileKind.POCKET_NUKE));

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
