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

	public static final List<String> ALL = List.of(SPARKS, GRENADE_BLAST, PLASMA_IMPACT, NAPALM, SMOKE_COLUMN, CRYO_BURST,
			VOID_IMPLOSION, TOXIC_CLOUD, ARC_BURST, RADIATION, EMBER_RAIN, FIREWORK, MUZZLE_FLASH, NUKE);

	private VfxIds() {
	}
}
