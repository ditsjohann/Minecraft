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
	AIR_BOMB(0.06, -1, false, () -> ModItems.ROCKET);

	public final double gravity;
	/** Ticks before a thrown charge goes off; -1 = impact fused. */
	public final int fuse;
	public final boolean sticky;
	private final Supplier<Item> item;

	ProjectileKind(double gravity, int fuse, boolean sticky, Supplier<Item> item) {
		this.gravity = gravity;
		this.fuse = fuse;
		this.sticky = sticky;
		this.item = item;
	}

	public Item item() {
		return item.get();
	}

	public boolean isGrenade() {
		return fuse > 0;
	}

	public boolean isEnergy() {
		return this == PLASMA || this == CRYO || this == SINGULARITY || this == BFG;
	}

	public boolean isRocket() {
		return this == ROCKET || this == MIRV || this == MINI_NUKE || this == BOMBLET || this == METEOR || this == ROD
				|| this == GATLING_NUKE || this == FALLING_NUKE || this == AIR_BOMB;
	}

	public static ProjectileKind byId(int id) {
		ProjectileKind[] v = values();
		return v[Math.floorMod(id, v.length)];
	}
}
