package com.armageddon;

import com.armageddon.detonation.DetonationManager;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.*;
import com.armageddon.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Armageddon Arsenal — a stylized weapons-of-mass-destruction mod.
 * Common entrypoint: registers every block, item, entity, effect and system.
 */
public class Armageddon implements ModInitializer {
	public static final String MOD_ID = "armageddon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModSounds.init();
		ModParticles.init();
		ModEffects.init();
		ModComponents.init();
		ModMaterials.init();
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
		ModItemGroups.init();
		ModGameRules.init();
		ModWorldGen.init();
		ModNetworking.init();
		DetonationManager.init();
		com.armageddon.destruction.TemporaryBlocks.init();
		com.armageddon.destruction.EnergyFields.init();
		ModEvents.init();
		ModCommands.init();
		LOGGER.info("Armageddon Arsenal armed. {} items registered.", ModItems.ALL.size());
	}
}
