package com.armageddon.vfx;

import java.util.List;

/**
 * Names of the particle-engine effects. Shared so the server (commands, weapons) can name an effect the client
 * defines; the effects themselves live in {@code client/vfx/VfxLibrary}.
 */
public final class VfxIds {
	public static final String SPARKS = "sparks";
	public static final String GRENADE_BLAST = "grenade_blast";
	public static final String PLASMA_IMPACT = "plasma_impact";
	public static final String NAPALM = "napalm";
	public static final String SMOKE_COLUMN = "smoke_column";
	public static final String CRYO_BURST = "cryo_burst";
	public static final String VOID_IMPLOSION = "void_implosion";
	public static final String TOXIC_CLOUD = "toxic_cloud";
	public static final String ARC_BURST = "arc_burst";
	public static final String RADIATION = "radiation";
	public static final String EMBER_RAIN = "ember_rain";
	public static final String FIREWORK = "firework";
	public static final String MUZZLE_FLASH = "muzzle_flash";
	public static final String NUKE = "nuke";

	// goo bomb
	public static final String GOO_DRIP = "goo_drip";
	public static final String GOO_PULSE = "goo_pulse";
	public static final String GOO_BURST = "goo_burst";
	public static final String GOO_GLOB = "goo_glob";
	public static final String GOO_SPLAT = "goo_splat";
	// hyperbeam raygun
	public static final String RAY_CHARGE = "ray_charge";
	public static final String RAY_MUZZLE = "ray_muzzle";
	public static final String RAY_HEAD = "ray_head";
	public static final String RAY_BLAST = "ray_blast";
	// cactus
	public static final String CACTUS_DUST = "cactus_dust";
	public static final String CACTUS_SPIKE = "cactus_spike";
	public static final String SPIKE_IMPACT = "spike_impact";
	// blackforge
	public static final String FORGE_EMBERS = "forge_embers";
	public static final String FORGE_STRIKE = "forge_strike";
	public static final String FORGE_SHOCK = "forge_shock";
	public static final String FORGE_SPLASH = "forge_splash";
	// blue infrit
	public static final String SOUL_FLARE = "soul_flare";
	public static final String SOUL_PILLAR = "soul_pillar";
	public static final String SOUL_VORTEX = "soul_vortex";
	public static final String SOUL_TRAIL = "soul_trail";
	public static final String SOUL_SWEEP = "soul_sweep";
	public static final String INFRIT_IMPACT = "infrit_impact";
	public static final String INFRIT_BURST = "infrit_burst";

	public static final List<String> ALL = List.of(SPARKS, GRENADE_BLAST, PLASMA_IMPACT, NAPALM, SMOKE_COLUMN, CRYO_BURST,
			VOID_IMPLOSION, TOXIC_CLOUD, ARC_BURST, RADIATION, EMBER_RAIN, FIREWORK, MUZZLE_FLASH, NUKE,
			GOO_DRIP, GOO_PULSE, GOO_BURST, GOO_GLOB, GOO_SPLAT, RAY_CHARGE, RAY_MUZZLE, RAY_HEAD, RAY_BLAST,
			CACTUS_DUST, CACTUS_SPIKE, SPIKE_IMPACT, FORGE_EMBERS, FORGE_STRIKE, FORGE_SHOCK, FORGE_SPLASH,
			SOUL_FLARE, SOUL_PILLAR, SOUL_VORTEX, SOUL_TRAIL, SOUL_SWEEP, INFRIT_IMPACT, INFRIT_BURST);

	private VfxIds() {
	}
}
