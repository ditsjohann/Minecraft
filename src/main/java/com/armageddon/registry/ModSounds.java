package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final SoundEvent NUKE_DETONATE = register("nuke.detonate");
	public static final SoundEvent NUKE_RUMBLE = register("nuke.rumble");
	public static final SoundEvent BOMB_ARMED = register("bomb.armed");
	public static final SoundEvent BOMB_BEEP = register("bomb.beep");
	public static final SoundEvent PLASMA_FIRE = register("weapon.plasma_fire");
	public static final SoundEvent PLASMA_IMPACT = register("weapon.plasma_impact");
	public static final SoundEvent RAILGUN_CHARGE = register("weapon.railgun_charge");
	public static final SoundEvent RAILGUN_FIRE = register("weapon.railgun_fire");
	public static final SoundEvent MINIGUN_FIRE = register("weapon.minigun_fire");
	public static final SoundEvent MINIGUN_SPIN = register("weapon.minigun_spin");
	public static final SoundEvent FLAMETHROWER = register("weapon.flamethrower");
	public static final SoundEvent ARC_ZAP = register("weapon.arc_zap");
	public static final SoundEvent CRYO_BLAST = register("weapon.cryo_blast");
	public static final SoundEvent ROCKET_LAUNCH = register("weapon.rocket_launch");
	public static final SoundEvent NUKE_LAUNCH = register("weapon.nuke_launch");
	public static final SoundEvent RELOAD = register("weapon.reload");
	public static final SoundEvent DRY_FIRE = register("weapon.dry_fire");
	public static final SoundEvent BLACK_HOLE = register("black_hole.hum");
	public static final SoundEvent BLACK_HOLE_COLLAPSE = register("black_hole.collapse");
	public static final SoundEvent ORBITAL_CHARGE = register("orbital.charge");
	public static final SoundEvent ORBITAL_STRIKE = register("orbital.strike");
	public static final SoundEvent GRAVITY_SLAM = register("weapon.gravity_slam");
	public static final SoundEvent BLADE_DASH = register("weapon.blade_dash");
	public static final SoundEvent GEIGER_CLICK = register("item.geiger_click");
	public static final SoundEvent JETPACK = register("item.jetpack");
	public static final SoundEvent DETONATOR = register("item.detonator");
	public static final SoundEvent INJECT = register("item.inject");
	public static final SoundEvent ANTIMATTER = register("bomb.antimatter");
	public static final SoundEvent FREEZE = register("bomb.freeze");
	public static final SoundEvent DUBSTEP_WUB = register("weapon.dubstep_wub");
	public static final SoundEvent DUBSTEP_DROP = register("weapon.dubstep_drop");
	public static final SoundEvent DEATH_RAY = register("weapon.death_ray");
	public static final SoundEvent JET_FLYBY = register("strike.jet_flyby");
	public static final SoundEvent TITAN_LANDING = register("item.titan_landing");

	public static final RegistryEntry<SoundEvent> EQUIP_POWER_ARMOR = registerEntry("item.equip_power_armor");
	public static final RegistryEntry<SoundEvent> EQUIP_HAZMAT = registerEntry("item.equip_hazmat");

	private static SoundEvent register(String name) {
		Identifier id = Armageddon.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	private static RegistryEntry<SoundEvent> registerEntry(String name) {
		Identifier id = Armageddon.id(name);
		return Registry.registerReference(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {
	}

	private ModSounds() {
	}
}
