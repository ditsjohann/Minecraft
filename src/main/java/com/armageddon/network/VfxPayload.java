package com.armageddon.network;

import com.armageddon.Armageddon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server → client: play a particle-engine effect ({@link com.armageddon.vfx.VfxIds}). One small packet per effect;
 * every client simulates it locally from the same {@code seed}, at its own detail level.
 *
 * @param color tint for effects that take one (0 keeps the effect's own colours)
 */
public record VfxPayload(String effect, double x, double y, double z, float dx, float dy, float dz, float scale, int color,
		long seed) implements CustomPayload {
	public static final CustomPayload.Id<VfxPayload> ID = new CustomPayload.Id<>(Armageddon.id("vfx"));
	public static final PacketCodec<RegistryByteBuf, VfxPayload> CODEC = PacketCodec.of(VfxPayload::write, VfxPayload::read);

	private void write(RegistryByteBuf buf) {
		buf.writeString(effect, 64);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(dx);
		buf.writeFloat(dy);
		buf.writeFloat(dz);
		buf.writeFloat(scale);
		buf.writeInt(color);
		buf.writeLong(seed);
	}

	private static VfxPayload read(RegistryByteBuf buf) {
		return new VfxPayload(buf.readString(64), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readLong());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
