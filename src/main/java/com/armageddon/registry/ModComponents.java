package com.armageddon.registry;

import com.armageddon.Armageddon;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.UnaryOperator;

public final class ModComponents {
	/** Rounds currently loaded in a weapon's magazine. */
	public static final ComponentType<Integer> AMMO = register("ammo",
			b -> b.codec(Codecs.NONNEGATIVE_INT).packetCodec(PacketCodecs.VAR_INT));

	/** Charges/bombs linked to a detonator. */
	public static final ComponentType<List<BlockPos>> LINKED_CHARGES = register("linked_charges",
			b -> b.codec(BlockPos.CODEC.listOf()).packetCodec(BlockPos.PACKET_CODEC.collect(PacketCodecs.toList())));

	/** Barrel heat of the Judgment Engine (0-100). */
	public static final ComponentType<Integer> HEAT = register("heat",
			b -> b.codec(Codecs.NONNEGATIVE_INT).packetCodec(PacketCodecs.VAR_INT));

	/** First corner chosen with the Warden Wand. */
	public static final ComponentType<BlockPos> CORNER = register("corner",
			b -> b.codec(BlockPos.CODEC).packetCodec(BlockPos.PACKET_CODEC));

	private static <T> ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builder) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Armageddon.id(name), builder.apply(ComponentType.builder()).build());
	}

	public static void init() {
	}

	private ModComponents() {
	}
}
