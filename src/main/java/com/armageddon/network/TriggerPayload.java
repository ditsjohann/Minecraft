package com.armageddon.network;

import com.armageddon.Armageddon;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: the use key is held/released while holding an automatic weapon. */
public record TriggerPayload(boolean held) implements CustomPayload {
	public static final CustomPayload.Id<TriggerPayload> ID = new CustomPayload.Id<>(Armageddon.id("trigger"));
	public static final PacketCodec<RegistryByteBuf, TriggerPayload> CODEC =
			PacketCodecs.BOOL.xmap(TriggerPayload::new, TriggerPayload::held).cast();

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
