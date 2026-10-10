package com.armageddon.world;

import com.armageddon.Armageddon;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/** Adds uranium and titanium ore veins (defined in data/armageddon/worldgen) to the Overworld. */
public final class ModWorldGen {
	public static final RegistryKey<PlacedFeature> URANIUM_ORE = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Armageddon.id("ore_uranium"));
	public static final RegistryKey<PlacedFeature> TITANIUM_ORE = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Armageddon.id("ore_titanium"));

	public static void init() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, URANIUM_ORE);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, TITANIUM_ORE);
	}

	private ModWorldGen() {
	}
}
