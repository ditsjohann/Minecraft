package com.armageddon.block;

import com.armageddon.detonation.Detonations;
import com.armageddon.entity.StarEntity;
import com.armageddon.event.EventEntity;
import com.armageddon.entity.StrikeMarkerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/** Every placeable warhead. Fuse is in ticks. */
public enum BombType {
	DEMOLITION_CHARGE(20, 0xFFAA33),
	THERMOBARIC(100, 0xFF5511),
	CRYO(140, 0x88DDFF),
	SINGULARITY(160, 0xAA44FF),
	NEUTRON(160, 0x66FF44),
	ATOMIC(200, 0xFFDD55),
	ANTIMATTER(200, 0xEE66FF),
	HYDROGEN(300, 0xFFFFFF),
	TECTONIC(120, 0xA08060),
	BASS_DROP(60, 0xFF40FF),
	CLUSTER(160, 0xFFAA22),
	NOVA(200, 0xFFD040),
	OMEGA(300, 0xFF2020),
	// Arsenal expansion: event-driven warheads.
	HELIOS_CROWN(100, 0xFFB000),
	CONTINENTAL(120, 0xFFA020),
	DAWNSTAR(120, 0xFFD040),
	BLOODSTAR(200, 0xFF2020),
	LAST_LIGHT(200, 0xFFFFFF),
	ZEROPOINT(200, 0x60F0FF),
	UNMAKER(200, 0xE0E0FF),
	GODFALL(200, 0xFFE8A0),
	FINAL_EXISTENCE(400, 0xFF40FF),
	// Bomb expansion II: uncommon
	DISCO(60, 0xFF40FF),
	GLITTER(60, 0xFFE8A0),
	PUMPKIN_PATCH(60, 0xFF8020),
	COBWEB(60, 0xE8E8E8),
	BALLOON(60, 0xFF70B0),
	SPRINKLER(60, 0x60B0FF),
	// rare
	THERMITE(80, 0xFF6A10),
	TESLA(80, 0x80C0FF),
	SINKHOLE(80, 0x8A6A40),
	// legendary
	TSUNAMI(120, 0x2080FF),
	VOLCANO(120, 0xFF4010),
	ICE_AGE(120, 0xBFEFFF),
	// mythic
	HELLMOUTH(200, 0xC01010),
	CYCLONE(200, 0xA0B0C0),
	DOOMSDAY_CLOCK(200, 0xFF3030),
	// world destruction
	MOONFALL(400, 0xC8C8D0),
	BIG_BANG(400, 0xFFFFFF),
	WORLD_EATER(400, 0x6010A0),
	CHRONO_BREAK(400, 0x50FFD0),
	RAGNAROK(400, 0xFF1010),
	// celestial expansion II
	EXPLODING_COWS(300, 0xFFFFFF),
	DEADLY_TWISTER(300, 0x8090A0),
	COLOSSAL_SPEAR(300, 0xFFB000),
	ZOMBIE_APOCALYPSE(300, 0x40C040),
	UFO_INVASION(300, 0x40A0FF),
	// true world destruction
	GALACTIC_SMASH(600, 0xA070FF),
	BOOMING_TICK(600, 0xFF4040),
	ALIEN_DEATH_RAY(600, 0x60FFA0),
	// world destruction II
	ETERNAL_SAPLING(600, 0x40E060),
	SIXTEEN_CHUNKS(600, 0xFF40FF),
	TRUMAN_SHOW(600, 0x80C8FF),
	DISCO_BALL(600, 0xFFE0FF),
	// the strong five
	LIDAR_BOMB(300, 0x60FFFF),
	SHADOW_VOID(300, 0x6020C0),
	SPECTRAL_ICE(300, 0x80D0FF),
	TERMITE_DOMAIN(400, 0x8A5A30),
	SUN_DISAPPEARS(600, 0xFFD060),
	// the oddities
	SONAR_BOMB(120, 0x40FF80),
	PIXEL_BOMB(120, 0xFF40FF),
	MAGNETAR(200, 0x80C0FF),
	INK_BOMB(120, 0xFFE020),
	AURORA_BOMB(120, 0x40FF90),
	SEASONS_BOMB(200, 0xE0A040),
	PRISM_BOMB(200, 0xFFFFFF),
	GEODE_BOMB(120, 0xC080FF),
	FOSSIL_BOMB(200, 0xE8E0C8),
	LAVA_LAMP(120, 0xFF6020),
	// plasma and sci-fi
	ION_SCYTHE(200, 0x80E0FF),
	RESONANCE_CASCADE(300, 0x60C8FF),
	TESLA_DOMINION(300, 0x90D8FF),
	DYSON_LANCE(400, 0xFFB060),
	GRAVITY_KILLSWITCH(400, 0x70E0FF),
	SEISMIC_TSUNAMI(400, 0xC0A0FF),
	// the wild ones
	GOO_BOMB(100, 0x80FF30),
	CACTUS_BOMB(120, 0x5AA040),
	BLACKFORGE(200, 0xFF5A1A),
	BLUE_INFRIT(300, 0x40C8FF),
	// plasma
	PLASMA_KOI(200, 0x60F0FF),
	DIMENSION_SHIFT(200, 0x8030FF),
	FIREWORK_HAVOC(100, 0xFF60C0),
	CHINESE_LANTERN(80, 0xFF4020),
	// fossils
	MAMMOTH_FOSSIL(200, 0x9CE0FF),
	LEVIATHAN_FOSSIL(200, 0x40E0D0),
	PTEROSAUR_FOSSIL(160, 0xFF9040);

	public final int fuse;
	public final int color;

	BombType(int fuse, int color) {
		this.fuse = fuse;
		this.color = color;
	}

	public boolean isStrategic() {
		return this.ordinal() >= NEUTRON.ordinal();
	}

	/** Each bomb's tier, set when its block is registered; everything below is derived from it. */
	private static final java.util.EnumMap<BombType, com.armageddon.tier.Tier> TIERS = new java.util.EnumMap<>(BombType.class);

	public static void assign(BombType type, com.armageddon.tier.Tier tier) {
		TIERS.put(type, tier);
	}

	public com.armageddon.tier.Tier tier() {
		return TIERS.getOrDefault(this, com.armageddon.tier.Tier.EPIC);
	}

	/**
	 * The FUN tier: toys. Harmless, or only temporary (the Disco Ball's dance floor reverts), so they are never
	 * gated by the celestial game rule and sit with the ordinary bombs.
	 */
	public boolean isFun() {
		return tier() == com.armageddon.tier.Tier.FUN;
	}

	/** Celestial and above: the cataclysms, gated by the celestial game rule and kept in their own tab. */
	public boolean isCelestial() {
		return tier().ordinal() >= com.armageddon.tier.Tier.CELESTIAL.ordinal();
	}

	/** The CITY DESTRUCTION tier: a city-sized area or more is remade. */
	public boolean isCityDestruction() {
		return tier() == com.armageddon.tier.Tier.CITY_DESTRUCTION;
	}

	/** The true WORLD DESTRUCTION tier: the world enders. */
	public boolean isWorldDestruction() {
		return tier() == com.armageddon.tier.Tier.WORLD_DESTRUCTION;
	}

	/**
	 * Must be lit twice (and are immune to redstone and chain detonation): everything Mythic and above, and the Disco
	 * Ball because it takes the camera.
	 */
	public boolean needsConfirmation() {
		return this == DISCO_BALL || tier().ordinal() >= com.armageddon.tier.Tier.MYTHIC.ordinal();
	}

	public void detonate(ServerWorld world, Vec3d pos, @Nullable Entity cause) {
		switch (this) {
			case DEMOLITION_CHARGE -> Detonations.demolition(world, pos, cause);
			case THERMOBARIC -> Detonations.thermobaric(world, pos, cause, 11.0F);
			case CRYO -> Detonations.cryo(world, pos, cause, 36);
			case SINGULARITY -> Detonations.singularity(world, pos, cause, 9.0F, 600, true);
			case NEUTRON -> Detonations.neutron(world, pos, cause, 110);
			case ATOMIC -> Detonations.nuke(world, pos, cause, 26);
			case ANTIMATTER -> Detonations.antimatter(world, pos, cause, 30);
			case HYDROGEN -> Detonations.nuke(world, pos, cause, 52);
			case TECTONIC -> StrikeMarkerEntity.spawn(world, pos, StrikeMarkerEntity.TECTONIC, cause);
			case BASS_DROP -> StrikeMarkerEntity.spawn(world, pos, StrikeMarkerEntity.BASS_DROP, cause);
			case CLUSTER -> Detonations.clusterNuke(world, pos, cause);
			case NOVA -> StarEntity.spawn(world, pos, cause);
			case OMEGA -> StrikeMarkerEntity.spawn(world, pos, StrikeMarkerEntity.OMEGA, cause);
			default -> EventEntity.start(world, pos, name().toLowerCase(java.util.Locale.ROOT), cause,
					nbt -> nbt.putFloat("Yaw", cause != null ? cause.getYaw() : 0.0F));
		}
	}

	public static BombType byId(int id) {
		BombType[] values = values();
		return values[Math.floorMod(id, values.length)];
	}
}
