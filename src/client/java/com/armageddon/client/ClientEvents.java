package com.armageddon.client;

import com.armageddon.client.fx.ClientFx;
import com.armageddon.client.fx.PostFx;
import com.armageddon.client.hud.WmdHud;
import com.armageddon.entity.FalloutCloudEntity;
import com.armageddon.item.GunItem;
import com.armageddon.network.TriggerPayload;
import com.armageddon.registry.ModEffects;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/** Per-tick client logic: automatic triggers, Geiger counter, radiation vision, FX ticking. */
public final class ClientEvents {
	private static boolean triggerSent;

	public static void tick(MinecraftClient mc) {
		ClientFx.tick(mc);
		ClientPlayerEntity player = mc.player;
		if (player == null) {
			triggerSent = false;
			return;
		}
		tickTrigger(mc, player);
		tickRadiation(mc, player);
	}

	private static void tickTrigger(MinecraftClient mc, ClientPlayerEntity player) {
		ItemStack held = player.getMainHandStack();
		boolean want = mc.currentScreen == null && mc.options.useKey.isPressed() && !player.isSpectator()
				&& held.getItem() instanceof GunItem gun && gun.getTrigger() == GunItem.Trigger.AUTO;
		if (want != triggerSent && ClientPlayNetworking.canSend(TriggerPayload.ID)) {
			ClientPlayNetworking.send(new TriggerPayload(want));
			triggerSent = want;
		}
		if (want && held.isOf(ModItems.MINIGUN) && !player.getItemCooldownManager().isCoolingDown(held.getItem())) {
			ClientFx.shake = Math.max(ClientFx.shake, 0.25F);
		}
	}

	private static void tickRadiation(MinecraftClient mc, ClientPlayerEntity player) {
		double total = 0;
		if (mc.world != null) {
			for (Entity e : mc.world.getEntities()) {
				if (e instanceof FalloutCloudEntity cloud) {
					total += cloud.doseAt(player.getPos());
				}
			}
		}
		StatusEffectInstance rad = player.getStatusEffect(ModEffects.RADIATION);
		if (rad != null) {
			total += 0.4 + rad.getAmplifier() * 0.8;
		}
		WmdHud.dose = (float) MathHelper.lerp(0.2, WmdHud.dose, total);

		boolean geiger = false;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(ModItems.GEIGER_COUNTER)) {
				geiger = true;
				break;
			}
		}
		WmdHud.hasGeiger = geiger;
		if (geiger && !mc.isPaused()) {
			float chance = Math.min(0.85F, 0.015F + WmdHud.dose * 0.14F);
			if (player.getRandom().nextFloat() < chance) {
				mc.getSoundManager().play(PositionedSoundInstance.master(ModSounds.GEIGER_CLICK, 0.8F + player.getRandom().nextFloat() * 0.6F, 0.5F));
			}
		}

		// No screen-warping "radiation vision": the Geiger counter and HUD meter carry the danger instead.
		PostFx.radiation = 0;
	}

	private ClientEvents() {
	}
}
