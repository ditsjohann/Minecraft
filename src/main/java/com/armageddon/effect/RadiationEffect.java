package com.armageddon.effect;

import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.ArmorSets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Radiation sickness. Deals unblockable damage over time, scaling with amplifier,
 * and adds hunger/nausea at higher doses. Full hazmat suits neutralise it.
 */
public class RadiationEffect extends StatusEffect {
	public RadiationEffect() {
		super(StatusEffectCategory.HARMFUL, 0x7CFF2E);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = Math.max(10, 50 >> amplifier);
		return duration % interval == 0;
	}

	@Override
	public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (entity.getWorld().isClient) {
			return true;
		}
		if (ArmorSets.hasFullHazmat(entity)) {
			return true;
		}
		entity.damage(ModDamageTypes.source(entity.getWorld(), ModDamageTypes.RADIATION), 1.0F + amplifier);
		if (amplifier >= 1) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, amplifier - 1, true, false));
		}
		if (amplifier >= 2) {
			entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1, true, false));
		}
		return true;
	}
}
