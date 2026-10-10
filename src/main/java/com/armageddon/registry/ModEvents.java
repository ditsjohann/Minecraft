package com.armageddon.registry;

import com.armageddon.item.GunItem;
import com.armageddon.network.ModNetworking;
import com.armageddon.util.ArmorSets;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side glue: automatic weapon triggers, jetpack flight, armour immunities. */
public final class ModEvents {
	private static final Set<UUID> JETPACK_FLIGHT = new HashSet<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ModEvents::tick);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ModNetworking.TRIGGERS.remove(handler.player.getUuid());
			JETPACK_FLIGHT.remove(handler.player.getUuid());
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.isOf(ModDamageTypes.RADIATION) && ArmorSets.isRadiationSealed(entity)) {
				return false;
			}
			if (ArmorSets.hasFullJuggernaut(entity) && (source.isIn(DamageTypeTags.IS_EXPLOSION) || source.isIn(DamageTypeTags.IS_FIRE))) {
				return false;
			}
			return true;
		});
	}

	private static void tick(MinecraftServer server) {
		tickTriggers(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			tickJetpack(player);
		}
	}

	private static void tickTriggers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = ModNetworking.TRIGGERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			if (player == null || !player.isAlive()) {
				it.remove();
				continue;
			}
			ItemStack stack = player.getMainHandStack();
			if (stack.getItem() instanceof GunItem gun && gun.getTrigger() == GunItem.Trigger.AUTO && !player.isSpectator()) {
				gun.triggerTick(player.getServerWorld(), player, stack, entry.getValue());
				entry.setValue(entry.getValue() + 1);
			} else {
				entry.setValue(0);
			}
		}
	}

	private static void tickJetpack(ServerPlayerEntity player) {
		PlayerAbilities abilities = player.getAbilities();
		UUID id = player.getUuid();
		if (player.isCreative() || player.isSpectator()) {
			JETPACK_FLIGHT.remove(id);
			return;
		}
		if (ArmorSets.hasJetpack(player)) {
			if (!abilities.allowFlying) {
				abilities.allowFlying = true;
				player.sendAbilitiesUpdate();
				JETPACK_FLIGHT.add(id);
			}
			if (abilities.flying) {
				player.fallDistance = 0;
				ServerWorld world = player.getServerWorld();
				float yaw = player.getBodyYaw() * MathHelper.RADIANS_PER_DEGREE;
				Vec3d back = player.getPos().add(MathHelper.sin(yaw) * 0.35, 0.75, -MathHelper.cos(yaw) * 0.35);
				Vec3d right = new Vec3d(-MathHelper.cos(yaw), 0, -MathHelper.sin(yaw)).multiply(0.18);
				for (int side = -1; side <= 1; side += 2) {
					Vec3d n = back.add(right.multiply(side));
					world.spawnParticles(ParticleTypes.FLAME, n.x, n.y, n.z, 0, 0, -1, 0, 0.15);
					world.spawnParticles(ModParticles.EMBER, n.x, n.y - 0.2, n.z, 0, 0, -1, 0, 0.1);
				}
				if (player.age % 6 == 0) {
					world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, back.x, back.y - 0.4, back.z, 1, 0.05, 0.05, 0.05, 0.01);
				}
				if (player.age % 10 == 0) {
					world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.JETPACK, SoundCategory.PLAYERS, 0.5F, 1.0F);
				}
				if (player.age % 200 == 0) {
					player.getEquippedStack(EquipmentSlot.CHEST).damage(1, player, EquipmentSlot.CHEST);
				}
			}
		} else if (JETPACK_FLIGHT.remove(id)) {
			abilities.allowFlying = false;
			abilities.flying = false;
			player.sendAbilitiesUpdate();
		}
	}

	private ModEvents() {
	}
}
