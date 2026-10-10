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
	FINAL_EXISTENCE(400, 0xFF40FF);

	public final int fuse;
	public final int color;

	BombType(int fuse, int color) {
		this.fuse = fuse;
		this.color = color;
	}

	public boolean isStrategic() {
		return this.ordinal() >= NEUTRON.ordinal();
	}

	/** Celestial warheads are world events. */
	public boolean isCelestial() {
		return this.ordinal() >= LAST_LIGHT.ordinal();
	}

	/** Must be lit twice (and are immune to redstone and chain detonation). */
	public boolean needsConfirmation() {
		return this == BLOODSTAR || isCelestial();
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
