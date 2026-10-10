package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class ModMaterials {
	public static final RegistryEntry<ArmorMaterial> HAZMAT = armor("hazmat", 2, 4, 3, 2, 3, 12, ModSounds.EQUIP_HAZMAT, 0.0F, 0.0F,
			() -> Ingredient.ofItems(ModItems.TITANIUM_PLATE));
	public static final RegistryEntry<ArmorMaterial> JUGGERNAUT = armor("juggernaut", 6, 10, 8, 5, 12, 15, ModSounds.EQUIP_POWER_ARMOR, 5.0F, 0.25F,
			() -> Ingredient.ofItems(ModItems.TITANIUM_PLATE));
	public static final RegistryEntry<ArmorMaterial> THRUSTER = armor("thruster", 3, 6, 8, 3, 8, 15, ModSounds.EQUIP_POWER_ARMOR, 2.0F, 0.0F,
			() -> Ingredient.ofItems(ModItems.TITANIUM_PLATE));

	public static final RegistryEntry<ArmorMaterial> TITAN = armor("titan", 6, 9, 10, 5, 12, 20, ModSounds.EQUIP_POWER_ARMOR, 4.0F, 0.2F,
			() -> Ingredient.ofItems(ModItems.TITANIUM_PLATE));

	public static final ToolMaterial TITANIUM = new Tool(2400, 11.0F, 4.0F, 18, BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			() -> Ingredient.ofItems(ModItems.TITANIUM_INGOT));
	public static final ToolMaterial PLASMA = new Tool(3600, 16.0F, 6.0F, 22, BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			() -> Ingredient.ofItems(ModItems.FUSION_CORE));

	private static RegistryEntry<ArmorMaterial> armor(String name, int boots, int legs, int chest, int helmet, int body, int enchantability,
			RegistryEntry<SoundEvent> sound, float toughness, float knockbackResistance, Supplier<Ingredient> repair) {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		defense.put(ArmorItem.Type.BOOTS, boots);
		defense.put(ArmorItem.Type.LEGGINGS, legs);
		defense.put(ArmorItem.Type.CHESTPLATE, chest);
		defense.put(ArmorItem.Type.HELMET, helmet);
		defense.put(ArmorItem.Type.BODY, body);
		ArmorMaterial material = new ArmorMaterial(defense, enchantability, sound, repair,
				List.of(new ArmorMaterial.Layer(Armageddon.id(name))), toughness, knockbackResistance);
		return Registry.registerReference(Registries.ARMOR_MATERIAL, Armageddon.id(name), material);
	}

	private record Tool(int durability, float speed, float attack, int enchantability, TagKey<Block> inverseTag,
			Supplier<Ingredient> repair) implements ToolMaterial {
		@Override
		public int getDurability() {
			return durability;
		}

		@Override
		public float getMiningSpeedMultiplier() {
			return speed;
		}

		@Override
		public float getAttackDamage() {
			return attack;
		}

		@Override
		public TagKey<Block> getInverseTag() {
			return inverseTag;
		}

		@Override
		public int getEnchantability() {
			return enchantability;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return repair.get();
		}
	}

	public static void init() {
	}

	private ModMaterials() {
	}

}
