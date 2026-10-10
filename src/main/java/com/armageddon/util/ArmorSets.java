package com.armageddon.util;

import com.armageddon.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

public final class ArmorSets {
	public static boolean hasFullHazmat(LivingEntity entity) {
		return wearing(entity, ModItems.HAZMAT_HELMET, ModItems.HAZMAT_SUIT, ModItems.HAZMAT_LEGGINGS, ModItems.HAZMAT_BOOTS);
	}

	public static boolean hasFullJuggernaut(LivingEntity entity) {
		return wearing(entity, ModItems.JUGGERNAUT_HELMET, ModItems.JUGGERNAUT_CHESTPLATE, ModItems.JUGGERNAUT_LEGGINGS, ModItems.JUGGERNAUT_BOOTS);
	}

	/** Juggernaut helmet + any chest (incl. thruster pack) counts as sealed against radiation too. */
	public static boolean isRadiationSealed(LivingEntity entity) {
		return hasFullHazmat(entity) || hasFullJuggernaut(entity);
	}

	public static boolean hasJetpack(LivingEntity entity) {
		return entity.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.THRUSTER_PACK);
	}

	private static boolean wearing(LivingEntity e, Item head, Item chest, Item legs, Item feet) {
		return e.getEquippedStack(EquipmentSlot.HEAD).isOf(head)
				&& e.getEquippedStack(EquipmentSlot.CHEST).isOf(chest)
				&& e.getEquippedStack(EquipmentSlot.LEGS).isOf(legs)
				&& e.getEquippedStack(EquipmentSlot.FEET).isOf(feet);
	}

	private ArmorSets() {
	}
}
