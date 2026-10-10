package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Data-driven damage types (see data/armageddon/damage_type). */
public final class ModDamageTypes {
	public static final RegistryKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
	public static final RegistryKey<DamageType> RADIATION = key("radiation");
	public static final RegistryKey<DamageType> PLASMA = key("plasma");
	public static final RegistryKey<DamageType> RAILGUN = key("railgun");
	public static final RegistryKey<DamageType> ANNIHILATION = key("annihilation");
	public static final RegistryKey<DamageType> ELECTROCUTION = key("electrocution");
	public static final RegistryKey<DamageType> CRYO = key("cryo");
	public static final RegistryKey<DamageType> SHRAPNEL = key("shrapnel");

	private static RegistryKey<DamageType> key(String name) {
		return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Armageddon.id(name));
	}

	public static DamageSource source(World world, RegistryKey<DamageType> key) {
		return world.getDamageSources().create(key);
	}

	public static DamageSource source(World world, RegistryKey<DamageType> key, @Nullable Entity source, @Nullable Entity attacker) {
		return world.getDamageSources().create(key, source, attacker);
	}

	private ModDamageTypes() {
	}
}
