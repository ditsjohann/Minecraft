package com.armageddon.registry;

import com.armageddon.Armageddon;
import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.entity.FalloutCloudEntity;
import com.armageddon.entity.PrimedBombEntity;
import com.armageddon.entity.StarEntity;
import com.armageddon.entity.StrikeMarkerEntity;
import com.armageddon.entity.WmdProjectileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	/** Rockets, mini-nukes, bomblets — rendered as oriented 3D models. */
	public static final EntityType<WmdProjectileEntity> PROJECTILE = register("projectile",
			EntityType.Builder.<WmdProjectileEntity>create(WmdProjectileEntity::new, SpawnGroup.MISC)
					.dimensions(0.4F, 0.4F).maxTrackingRange(12).trackingTickInterval(1));
	/** Plasma / cryo / singularity bolts — rendered as glowing energy. */
	public static final EntityType<WmdProjectileEntity> ENERGY_BOLT = register("energy_bolt",
			EntityType.Builder.<WmdProjectileEntity>create(WmdProjectileEntity::new, SpawnGroup.MISC)
					.dimensions(0.3F, 0.3F).maxTrackingRange(8).trackingTickInterval(1).makeFireImmune());
	/** Thrown grenades. */
	public static final EntityType<WmdProjectileEntity> GRENADE = register("grenade",
			EntityType.Builder.<WmdProjectileEntity>create(WmdProjectileEntity::new, SpawnGroup.MISC)
					.dimensions(0.3F, 0.3F).maxTrackingRange(8).trackingTickInterval(2));
	public static final EntityType<PrimedBombEntity> PRIMED_BOMB = register("primed_bomb",
			EntityType.Builder.<PrimedBombEntity>create(PrimedBombEntity::new, SpawnGroup.MISC)
					.dimensions(0.98F, 0.9F).maxTrackingRange(16).trackingTickInterval(10).makeFireImmune());
	public static final EntityType<BlackHoleEntity> BLACK_HOLE = register("black_hole",
			EntityType.Builder.<BlackHoleEntity>create(BlackHoleEntity::new, SpawnGroup.MISC)
					.dimensions(1.0F, 1.0F).maxTrackingRange(24).trackingTickInterval(5).makeFireImmune());
	public static final EntityType<FalloutCloudEntity> FALLOUT_CLOUD = register("fallout_cloud",
			EntityType.Builder.<FalloutCloudEntity>create(FalloutCloudEntity::new, SpawnGroup.MISC)
					.dimensions(1.0F, 1.0F).maxTrackingRange(32).trackingTickInterval(100).makeFireImmune());
	public static final EntityType<StrikeMarkerEntity> STRIKE_MARKER = register("strike_marker",
			EntityType.Builder.<StrikeMarkerEntity>create(StrikeMarkerEntity::new, SpawnGroup.MISC)
					.dimensions(0.5F, 0.5F).maxTrackingRange(16).trackingTickInterval(20).makeFireImmune());

	public static final EntityType<StarEntity> STAR = register("star",
			EntityType.Builder.<StarEntity>create(StarEntity::new, SpawnGroup.MISC)
					.dimensions(2.0F, 2.0F).maxTrackingRange(32).trackingTickInterval(2).makeFireImmune());

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		return Registry.register(Registries.ENTITY_TYPE, Armageddon.id(name), builder.build(Armageddon.id(name).toString()));
	}

	public static void init() {
	}

	private ModEntities() {
	}
}
