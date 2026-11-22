package com.armageddon.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ModNetworking {
	/** Players currently holding the trigger of an automatic weapon → ticks held. */
	public static final Map<UUID, Integer> TRIGGERS = new HashMap<>();

	public static void init() {
		PayloadTypeRegistry.playS2C().register(FxPayload.ID, FxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(VfxPayload.ID, VfxPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TriggerPayload.ID, TriggerPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TriggerPayload.ID, (payload, context) -> {
			UUID id = context.player().getUuid();
			if (payload.held()) {
				TRIGGERS.putIfAbsent(id, 0);
			} else {
				TRIGGERS.remove(id);
			}
		});
	}

	/** Sends an effect to every player in the world within {@code range} blocks of {@code origin}. */
	public static void sendFx(ServerWorld world, Vec3d origin, double range, FxPayload payload) {
		double r2 = range * range;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(origin) <= r2 && ServerPlayNetworking.canSend(player, FxPayload.ID)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	/** Sends a particle-engine effect to every player in the world within {@code range} blocks of {@code origin}. */
	public static void sendVfx(ServerWorld world, Vec3d origin, double range, VfxPayload payload) {
		double r2 = range * range;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(origin) <= r2 && ServerPlayNetworking.canSend(player, VfxPayload.ID)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private ModNetworking() {
	}
}
