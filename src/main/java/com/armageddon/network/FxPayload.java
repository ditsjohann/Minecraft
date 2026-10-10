package com.armageddon.network;

import com.armageddon.Armageddon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Vec3d;

/**
 * Server → client visual effect event. One generic packet drives every beam, flash,
 * shockwave and mushroom cloud so the client can build effects far beyond vanilla's
 * particle range limits. {@code p1}, {@code p2} and {@code duration} are per-type parameters.
 */
public record FxPayload(int type, double x, double y, double z, double x2, double y2, double z2,
		float scale, int color, int seed, float p1, float p2, int duration) implements CustomPayload {
	public static final CustomPayload.Id<FxPayload> ID = new CustomPayload.Id<>(Armageddon.id("fx"));
	public static final PacketCodec<RegistryByteBuf, FxPayload> CODEC = PacketCodec.of(FxPayload::write, FxPayload::read);

	public FxPayload(int type, double x, double y, double z, double x2, double y2, double z2, float scale, int color, int seed) {
		this(type, x, y, z, x2, y2, z2, scale, color, seed, 0, 0, 0);
	}

	public FxPayload(int type, Vec3d a, Vec3d b, float scale, int color, int seed) {
		this(type, a.x, a.y, a.z, b.x, b.y, b.z, scale, color, seed);
	}

	public FxPayload(int type, Vec3d a, float scale, int color) {
		this(type, a.x, a.y, a.z, a.x, a.y, a.z, scale, color, 0);
	}

	private void write(RegistryByteBuf buf) {
		buf.writeVarInt(type);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeDouble(x2);
		buf.writeDouble(y2);
		buf.writeDouble(z2);
		buf.writeFloat(scale);
		buf.writeInt(color);
		buf.writeInt(seed);
		buf.writeFloat(p1);
		buf.writeFloat(p2);
		buf.writeVarInt(duration);
	}

	private static FxPayload read(RegistryByteBuf buf) {
		return new FxPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt(), buf.readInt(),
				buf.readFloat(), buf.readFloat(), buf.readVarInt());
	}

	public Vec3d from() {
		return new Vec3d(x, y, z);
	}

	public Vec3d to() {
		return new Vec3d(x2, y2, z2);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
