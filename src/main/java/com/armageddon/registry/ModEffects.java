package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.effect.RadiationEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModEffects {
	public static final RegistryEntry<StatusEffect> RADIATION =
			Registry.registerReference(Registries.STATUS_EFFECT, Armageddon.id("radiation"), new RadiationEffect());

	public static void init() {
	}

	private ModEffects() {
	}
}
