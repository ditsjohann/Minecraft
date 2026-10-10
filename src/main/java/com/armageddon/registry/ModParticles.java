package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModParticles {
	/** Hot, glowing mushroom-cloud fire that cools into smoke. */
	public static final SimpleParticleType NUKE_FIRE = register("nuke_fire", true);
	/** Huge grey fallout smoke. */
	public static final SimpleParticleType NUKE_SMOKE = register("nuke_smoke", true);
	/** Ground dust kicked up by shockwaves. */
	public static final SimpleParticleType SHOCK_DUST = register("shock_dust", true);
	public static final SimpleParticleType PLASMA_SPARK = register("plasma_spark", false);
	public static final SimpleParticleType ARC_SPARK = register("arc_spark", false);
	public static final SimpleParticleType RADIATION = register("radiation", false);
	public static final SimpleParticleType CRYO_MIST = register("cryo_mist", false);
	public static final SimpleParticleType VOID_SPARK = register("void_spark", true);
	public static final SimpleParticleType NAPALM_FLAME = register("napalm_flame", false);
	public static final SimpleParticleType EMBER = register("ember", false);
	public static final SimpleParticleType TOXIC_GAS = register("toxic_gas", false);
	public static final SimpleParticleType MUZZLE_FLASH = register("muzzle_flash", false);

	private static SimpleParticleType register(String name, boolean alwaysSpawn) {
		return Registry.register(Registries.PARTICLE_TYPE, Armageddon.id(name), FabricParticleTypes.simple(alwaysSpawn));
	}

	public static void init() {
	}

	private ModParticles() {
	}
}
