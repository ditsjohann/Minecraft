package com.armageddon.entity;

import com.armageddon.registry.ModItems;
import net.minecraft.item.Item;

import java.util.function.Supplier;

/** Behaviour profile for {@link WmdProjectileEntity}. */
public enum ProjectileKind {
	MINI_NUKE(0.035, -1, false, () -> ModItems.MINI_NUKE),
	ROCKET(0.0, -1, false, () -> ModItems.ROCKET),
	MIRV(0.0, -1, false, () -> ModItems.ROCKET),
	BOMBLET(0.05, -1, false, () -> ModItems.MINI_NUKE),
	PLASMA(0.0, -1, false, () -> ModItems.PLASMA_CELL),
	CRYO(0.012, -1, false, () -> ModItems.CRYO_CANISTER),
	SINGULARITY(0.0, -1, false, () -> ModItems.ANTIMATTER_CELL),
	FRAG(0.05, 50, false, () -> ModItems.FRAG_GRENADE),
	PLASMA_GRENADE(0.05, 40, true, () -> ModItems.PLASMA_GRENADE),
	CRYO_GRENADE(0.05, 45, false, () -> ModItems.CRYO_GRENADE),
	INCENDIARY(0.05, 40, false, () -> ModItems.INCENDIARY_GRENADE),
	GRAVITY(0.05, 45, false, () -> ModItems.GRAVITY_GRENADE),
	TOXIC(0.05, 40, false, () -> ModItems.TOXIC_GRENADE),
	POCKET_NUKE(0.05, 70, false, () -> ModItems.POCKET_NUKE),
	/** Burning space rock from the Meteor Staff. */
	METEOR(0.04, -1, false, () -> ModItems.METEOR_STAFF),
	/** Tungsten kinetic penetrator ("Rods from God"). */
	ROD(0.08, -1, false, () -> ModItems.KINETIC_DESIGNATOR),
	/** Big green plasma orb that zaps everything near its path. */
	BFG(0.0, -1, false, () -> ModItems.ANTIMATTER_CELL),
	/** Small low-yield nuke fired by the Apocalypse Gatling. */
	GATLING_NUKE(0.02, -1, false, () -> ModItems.MINI_NUKE),
	/** Free-falling warhead dropped by barrages and cluster bombs. */
	FALLING_NUKE(0.06, -1, false, () -> ModItems.MINI_NUKE),
	/** Iron bomb dropped by an airstrike. */
	AIR_BOMB(0.06, -1, false, () -> ModItems.ROCKET),

	// ---- Uncommon (non-lethal) ----
	PARTY_ROCKET(0.0, -1, false, () -> ModItems.PARTY_POPPER, "party"),
	BOUNCE_BOMB(0.05, 30, false, () -> ModItems.BOUNCE_BOMB, "bounce"),
	LOOT_TORNADO(0.05, 25, false, () -> ModItems.LOOT_TORNADO, "loot_tornado"),
	MOONWALK_MINE(0.05, 20, false, () -> ModItems.MOONWALK_MINE, "moonwalk"),
	SNOW_GLOBE(0.05, 35, false, () -> ModItems.SNOW_GLOBE_GRENADE, "snow_globe"),
	SLIMEQUAKE(0.05, 35, false, () -> ModItems.SLIMEQUAKE_BOMB, "slimequake"),
	HOLOGRAM(0.05, 30, false, () -> ModItems.HOLOGRAM_BOMB, "hologram"),
	MAGNET(0.05, 30, false, () -> ModItems.MAGNET_MAYHEM, "magnet"),
	PAINT_BOMB(0.05, 25, false, () -> ModItems.RAINBOW_PAINT_BOMB, "paint"),
	// ---- Rare ----
	SHATTERFROST(0.05, 35, false, () -> ModItems.SHATTERFROST_BOMB, "shatterfrost"),
	ARCFLASH(0.05, 40, true, () -> ModItems.ARCFLASH_CHARGE, "arcflash"),
	SEISMIC_MINE(0.05, 40, false, () -> ModItems.SEISMIC_PULSE_MINE, "seismic_pulse"),
	IMPLOSION(0.05, 40, false, () -> ModItems.IMPLOSION_GRENADE, "implosion"),
	BOOMERANG(0.0, -1, false, () -> ModItems.PLASMA_BOOMERANG),
	ION_FOG(0.05, 30, false, () -> ModItems.ION_FOG_CANISTER, "ion_fog"),
	STASIS(0.05, 25, false, () -> ModItems.STASIS_MINE, "stasis"),
	EMP(0.05, 35, false, () -> ModItems.EMP_DISRUPTOR, "emp"),
	// ---- Epic ----
	MORTAR(0.045, -1, false, () -> ModItems.CATACLYSM_MORTAR, "cataclysm"),
	QUANTUM(0.05, 35, false, () -> ModItems.QUANTUM_FRACTURE_BOMB, "quantum_fracture"),
	CRYSTAL_SEED(0.05, 30, false, () -> ModItems.CRYSTAL_CATACLYSM, "crystals"),
	VOIDSTORM(0.05, 30, false, () -> ModItems.VOIDSTORM_GENERATOR, "voidstorm"),
	SIEGE_MISSILE(0.0, -1, false, () -> ModItems.ROCKET),
	MICRO_HORIZON(0.05, 30, false, () -> ModItems.MICRO_EVENT_HORIZON),
	MIRROR(0.05, 35, false, () -> ModItems.MIRROR_DIMENSION_BOMB, "mirror"),
	// ---- Legendary ----
	TITAN_MISSILE(0.0, -1, false, () -> ModItems.ROCKET, "titanfall"),
	MIDAS(0.03, -1, false, () -> ModItems.MIDAS_CASCADE),
	MIDAS_SPHERE(0.05, -1, false, () -> ModItems.MIDAS_CASCADE),
	JUDGMENT_BOLT(0.0, -1, false, () -> ModItems.SOLAR_CELL),
	// ---- Mythic ----
	DEVOURER(0.05, 25, false, () -> ModItems.DEVOURER_CORE);

	public final double gravity;
	/** Ticks before a thrown charge goes off; -1 = impact fused. */
	public final int fuse;
	public final boolean sticky;
	private final Supplier<Item> item;
	/** If set, detonation starts this world event instead of a hard-coded blast. */
	public final String event;

	ProjectileKind(double gravity, int fuse, boolean sticky, Supplier<Item> item) {
		this(gravity, fuse, sticky, item, null);
	}

	ProjectileKind(double gravity, int fuse, boolean sticky, Supplier<Item> item, String event) {
		this.gravity = gravity;
		this.fuse = fuse;
		this.sticky = sticky;
		this.item = item;
		this.event = event;
	}

	public Item item() {
		return item.get();
	}

	public boolean isGrenade() {
		return fuse > 0;
	}

	public boolean isEnergy() {
		return this == PLASMA || this == CRYO || this == SINGULARITY || this == BFG || this == PARTY_ROCKET || this == BOOMERANG
				|| this == MORTAR || this == MIDAS || this == MIDAS_SPHERE || this == JUDGMENT_BOLT;
	}

	public boolean isRocket() {
		return this == ROCKET || this == MIRV || this == MINI_NUKE || this == BOMBLET || this == METEOR || this == ROD
				|| this == GATLING_NUKE || this == FALLING_NUKE || this == AIR_BOMB || this == SIEGE_MISSILE || this == TITAN_MISSILE;
	}

	public static ProjectileKind byId(int id) {
		ProjectileKind[] v = values();
		return v[Math.floorMod(id, v.length)];
	}
}
