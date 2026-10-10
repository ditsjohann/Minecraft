package com.armageddon.client.hud;

import com.armageddon.client.fx.ClientFx;
import com.armageddon.client.fx.PostFx;
import com.armageddon.item.GunItem;
import com.armageddon.item.TargetingItem;
import com.armageddon.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/** Weapon read-outs, Geiger counter, targeting computer and the flash fallback overlay. */
public final class WmdHud {
	/** Smoothed dose rate in arbitrary Sv/h, updated each client tick. */
	public static float dose;
	public static boolean hasGeiger;

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientPlayerEntity player = mc.player;
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();

		if (!PostFx.renderedThisFrame && ClientFx.flash > 0.01F) {
			int a = (int) (MathHelper.clamp(ClientFx.flash, 0, 1) * 255);
			ctx.fill(0, 0, w, h, (a << 24) | (ClientFx.flashColor & 0xFFFFFF));
		}
		if (player == null || mc.options.hudHidden) {
			return;
		}
		TextRenderer font = mc.textRenderer;
		int cx = w / 2;
		int cy = h / 2;
		float tickDelta = counter.getTickDelta(false);

		ItemStack held = player.getMainHandStack();
		if (held.getItem() instanceof GunItem gun) {
			int ammo = GunItem.getAmmo(held);
			int mag = gun.getMagazineSize();
			float cooldown = player.getItemCooldownManager().getCooldownProgress(gun, tickDelta);
			String label = player.getAbilities().creativeMode ? "∞" : ammo + " / " + mag;
			int color = ammo == 0 ? 0xFF5050 : ammo <= mag / 4 ? 0xFFC040 : 0x7AF6FF;
			ctx.drawTextWithShadow(font, Text.literal(label), cx + 12, cy + 8, color);
			if (cooldown > 0) {
				int barW = 30;
				ctx.fill(cx + 12, cy + 19, cx + 12 + barW, cy + 21, 0x80000000);
				ctx.fill(cx + 12, cy + 19, cx + 12 + (int) (barW * (1 - cooldown)), cy + 21, 0xFF7AF6FF);
			}
			if (held.isOf(ModItems.RAILGUN) && player.isUsingItem()) {
				int used = player.getItemUseTime();
				float charge = MathHelper.clamp(used / 28.0F, 0, 1);
				int bw = 60;
				ctx.fill(cx - bw / 2, cy + 24, cx + bw / 2, cy + 27, 0x90000000);
				ctx.fill(cx - bw / 2, cy + 24, cx - bw / 2 + (int) (bw * charge), cy + 27, charge >= 1 ? 0xFFFFFFFF : 0xFF66E0FF);
				if (charge >= 1) {
					ctx.drawCenteredTextWithShadow(font, Text.translatable("hud.armageddon.charged"), cx, cy + 30, 0xFFFFFF);
				}
			}
		} else if (held.getItem() instanceof TargetingItem targeting) {
			BlockHitResult hit = TargetingItem.target(player.getWorld(), player, targeting.getRange());
			boolean lock = hit.getType() == HitResult.Type.BLOCK;
			int col = lock ? 0xFF4040 : 0x808080;
			// Brackets around the crosshair.
			int s = 10;
			ctx.fill(cx - s, cy - s, cx - s + 4, cy - s + 1, 0xFF000000 | col);
			ctx.fill(cx - s, cy - s, cx - s + 1, cy - s + 4, 0xFF000000 | col);
			ctx.fill(cx + s - 4, cy - s, cx + s, cy - s + 1, 0xFF000000 | col);
			ctx.fill(cx + s - 1, cy - s, cx + s, cy - s + 4, 0xFF000000 | col);
			ctx.fill(cx - s, cy + s - 1, cx - s + 4, cy + s, 0xFF000000 | col);
			ctx.fill(cx - s, cy + s - 4, cx - s + 1, cy + s, 0xFF000000 | col);
			ctx.fill(cx + s - 4, cy + s - 1, cx + s, cy + s, 0xFF000000 | col);
			ctx.fill(cx + s - 1, cy + s - 4, cx + s, cy + s, 0xFF000000 | col);
			Text t = lock
					? Text.translatable("hud.armageddon.target", (int) hit.getPos().distanceTo(player.getEyePos()))
					: Text.translatable("hud.armageddon.no_target");
			ctx.drawCenteredTextWithShadow(font, t, cx, cy + 16, col);
		}

		if (hasGeiger) {
			int x = 6;
			int y = 6;
			int color = dose < 0.1F ? 0x7CFF2E : dose < 1.5F ? 0xFFD030 : 0xFF4030;
			ctx.fill(x - 2, y - 2, x + 92, y + 22, 0x70000000);
			ctx.drawTextWithShadow(font, Text.literal("☢ " + String.format("%.2f", dose) + " Sv/h"), x, y, color);
			float level = MathHelper.clamp(dose / 5.0F, 0, 1);
			ctx.fill(x, y + 12, x + 88, y + 16, 0x60000000);
			ctx.fill(x, y + 12, x + (int) (88 * level), y + 16, 0xFF000000 | color);
		}
	}

	private WmdHud() {
	}
}
