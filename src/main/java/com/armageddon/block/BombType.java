package com.armageddon.block;

import com.armageddon.detonation.Detonations;
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
	HYDROGEN(300, 0xFFFFFF);

	public final int fuse;
	public final int color;

	BombType(int fuse, int color) {
		this.fuse = fuse;
		this.color = color;
	}

	public boolean isStrategic() {
		return this.ordinal() >= NEUTRON.ordinal();
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
		}
	}

	public static BombType byId(int id) {
		BombType[] values = values();
		return values[Math.floorMod(id, values.length)];
	}
}
