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
			int a = (int) (MathHelper.clamp(ClientFx.flash, 0, ClientFx.FLASH_MAX) * 255);
			ctx.fill(0, 0, w, h, (a << 24) | (ClientFx.flashColor & 0xFFFFFF));
		}
		float bars = MathHelper.lerp(counter.getTickDelta(false), ClientFx.letterboxPrev, ClientFx.letterbox);
		if (bars > 0.001F) {
			float e = bars * bars * (3 - 2 * bars);
			int bar = (int) (h * 0.12F * e);
			ctx.fill(0, 0, w, bar, 0xFF000000);
			ctx.fill(0, h - bar, w, h, 0xFF000000);
			// A soft lip and a hairline on the inner edge of each bar.
			int lip = Math.max(1, (int) (6 * e));
			ctx.fillGradient(0, bar, w, bar + lip, (int) (0x90 * e) << 24, 0);
			ctx.fillGradient(0, h - bar - lip, w, h - bar, 0, (int) (0x90 * e) << 24);
			int line = ((int) (0x22 * e) << 24) | 0xFFFFFF;
			ctx.fill(0, bar, w, bar + 1, line);
			ctx.fill(0, h - bar - 1, w, h - bar, line);
		}
		com.armageddon.client.fx.Cinematics.Cine cine = ClientFx.activeCine();
		if (cine != null) {
			try {
				cine.drawHud(ctx, w, h, cine.time(counter.getTickDelta(false)));
			} catch (RuntimeException ex) {
				com.armageddon.Armageddon.LOGGER.error("Armageddon cutscene overlay failed", ex);
			}
			com.armageddon.client.fx.Cinematics.Caption cap = cine.caption(cine.time(counter.getTickDelta(false)));
			if (cap != null && cap.alpha() > 0.02F) {
				caption(ctx, mc.textRenderer, w, h, cap);
			}
		}
		if (player == null || mc.options.hudHidden) {
			return;
		}
		TextRenderer font = mc.textRenderer;
		int cx = w / 2;
		int cy = h / 2;
		float tickDelta = counter.getTickDelta(false);

		ItemStack held = player.getMainHandStack();
		long now = mc.world != null ? mc.world.getTime() : 0;
		if (held.getItem() instanceof GunItem gun) {
			int ammo = GunItem.getAmmo(held);
			int mag = gun.getMagazineSize();
			float cooldown = player.getItemCooldownManager().getCooldownProgress(gun, tickDelta);
			boolean creative = player.getAbilities().creativeMode;
			int color = creative ? 0x7AF6FF : ammo == 0 ? 0xFF5050 : ammo <= mag / 4 ? 0xFFC040 : 0x7AF6FF;
			// Low ammo pulses.
			float pulse = !creative && ammo <= mag / 4 ? 0.6F + 0.4F * MathHelper.sin((now + tickDelta) * 0.6F) : 1.0F;
			int x = cx + 14, y = cy + 7;
			ctx.fill(x - 3, y - 2, x + 46, y + 18, 0x58000000);
			ctx.fill(x - 3, y - 2, x - 2, y + 18, 0xFF000000 | color);
			String count = creative ? "∞" : String.valueOf(ammo);
			ctx.getMatrices().push();
			ctx.getMatrices().translate(x, y, 0);
			ctx.getMatrices().scale(1.25F, 1.25F, 1);
			ctx.drawTextWithShadow(font, Text.literal(count), 0, 0, alpha(pulse) | color);
			ctx.getMatrices().pop();
			if (!creative) {
				ctx.drawTextWithShadow(font, Text.literal("/" + mag), x + (int) (font.getWidth(count) * 1.25F) + 2, y + 3, 0xA0A0A0);
			}
			// Magazine: pips for small magazines, a segmented gauge for big ones.
			int segs = Math.min(mag, 20);
			int filled = creative ? segs : (int) Math.ceil(segs * (double) ammo / Math.max(1, mag));
			int sw = Math.max(1, 42 / segs);
			for (int k = 0; k < segs; k++) {
				int px = x + k * sw;
				ctx.fill(px, y + 13, px + Math.max(1, sw - 1), y + 15, k < filled ? 0xFF000000 | color : 0x50FFFFFF);
			}
			if (cooldown > 0) {
				ctx.fill(x, y + 16, x + 42, y + 17, 0x60000000);
				ctx.fill(x, y + 16, x + (int) (42 * (1 - cooldown)), y + 17, 0xFFFFFFFF);
			}
			if (held.isOf(ModItems.RAILGUN) && player.isUsingItem()) {
				int used = player.getItemUseTime();
				float charge = MathHelper.clamp(used / 28.0F, 0, 1);
				int bw = 70;
				ctx.fill(cx - bw / 2 - 1, cy + 23, cx + bw / 2 + 1, cy + 28, 0x90000000);
				int fillCol = charge >= 1 ? 0xFFFFFFFF : 0xFF66E0FF;
				ctx.fillGradient(cx - bw / 2, cy + 24, cx - bw / 2 + (int) (bw * charge), cy + 27, fillCol, 0xFF2A80C0);
				for (int k = 1; k < 4; k++) {
					ctx.fill(cx - bw / 2 + bw * k / 4, cy + 24, cx - bw / 2 + bw * k / 4 + 1, cy + 27, 0x60000000);
				}
				if (charge >= 1) {
					float blink = 0.6F + 0.4F * MathHelper.sin((now + tickDelta) * 0.8F);
					ctx.drawCenteredTextWithShadow(font, Text.translatable("hud.armageddon.charged"), cx, cy + 31, alpha(blink) | 0xFFFFFF);
				}
			}
		} else if (held.getItem() instanceof TargetingItem targeting) {
			BlockHitResult hit = TargetingItem.target(player.getWorld(), player, targeting.getRange());
			boolean lock = hit.getType() == HitResult.Type.BLOCK;
			int col = lock ? 0xFF4040 : 0x909090;
			// Brackets that close in and breathe when locked.
			float t = (now + tickDelta) * 0.25F;
			int s = lock ? 9 + (int) (2 * MathHelper.sin(t)) : 13;
			int c = 0xFF000000 | col;
			bracket(ctx, cx - s, cy - s, 1, 1, c);
			bracket(ctx, cx + s, cy - s, -1, 1, c);
			bracket(ctx, cx - s, cy + s, 1, -1, c);
			bracket(ctx, cx + s, cy + s, -1, -1, c);
			if (lock) {
				ctx.fill(cx - 1, cy - s - 4, cx + 1, cy - s - 1, c);
				ctx.fill(cx - 1, cy + s + 1, cx + 1, cy + s + 4, c);
				ctx.fill(cx - s - 4, cy - 1, cx - s - 1, cy + 1, c);
				ctx.fill(cx + s + 1, cy - 1, cx + s + 4, cy + 1, c);
			}
			Text t2 = lock
					? Text.translatable("hud.armageddon.target", (int) hit.getPos().distanceTo(player.getEyePos()))
					: Text.translatable("hud.armageddon.no_target");
			int tw = font.getWidth(t2);
			ctx.fill(cx - tw / 2 - 4, cy + 18, cx + tw / 2 + 4, cy + 29, 0x58000000);
			ctx.drawCenteredTextWithShadow(font, t2, cx, cy + 20, col);
		}

		if (hasGeiger) {
			int x = 6;
			int y = 6;
			float level = MathHelper.clamp(dose / 5.0F, 0, 1);
			int color = dose < 0.1F ? 0x7CFF2E : dose < 1.5F ? 0xFFD030 : 0xFF4030;
			boolean danger = dose >= 1.5F;
			float blink = danger ? 0.55F + 0.45F * MathHelper.sin((now + tickDelta) * 0.9F) : 1.0F;
			ctx.fill(x - 3, y - 3, x + 104, y + 25, 0x78000000);
			ctx.fill(x - 3, y - 3, x + 104, y - 2, (danger ? alpha(blink) : 0xFF000000) | color);
			ctx.drawTextWithShadow(font, Text.literal("☢"), x, y, alpha(blink) | color);
			ctx.drawTextWithShadow(font, Text.literal(String.format("%.2f", dose)), x + 11, y, 0xFFFFFF);
			ctx.drawTextWithShadow(font, Text.literal("Sv/h"), x + 13 + font.getWidth(String.format("%.2f", dose)), y, 0x909090);
			// Twelve segments shading green to amber to red; the live ones glow.
			int segs = 12;
			int lit = (int) Math.ceil(segs * level);
			for (int k = 0; k < segs; k++) {
				float f = k / (segs - 1.0F);
				int seg = f < 0.45F ? 0x7CFF2E : f < 0.8F ? 0xFFD030 : 0xFF4030;
				int sx = x + k * 8;
				ctx.fill(sx, y + 12, sx + 7, y + 19, k < lit ? 0xFF000000 | seg : 0x30FFFFFF);
				if (k < lit) {
					ctx.fill(sx, y + 12, sx + 7, y + 13, 0x60FFFFFF);
				}
			}
		}
	}

	private static int alpha(float a) {
		return MathHelper.clamp((int) (a * 255), 8, 255) << 24;
	}

	private static void bracket(DrawContext ctx, int x, int y, int dx, int dy, int c) {
		int x2 = x + dx * 5, y2 = y + dy * 5;
		ctx.fill(Math.min(x, x2), Math.min(y, y + dy), Math.max(x, x2), Math.max(y, y + dy), c);
		ctx.fill(Math.min(x, x + dx), Math.min(y, y2), Math.max(x, x + dx), Math.max(y, y2), c);
	}

	/**
	 * Cutscene caption: letters drift together as it fades in and reveal one by one, over a soft dark
	 * plate with a thin accent line in the caption's colour.
	 */
	private static void caption(DrawContext ctx, TextRenderer f, int w, int h, com.armageddon.client.fx.Cinematics.Caption cap) {
		float a = MathHelper.clamp(cap.alpha(), 0.0F, 1.0F);
		float ease = a * a * (3 - 2 * a);
		String title = cap.title();
		int n = title.length();
		float spacing = 1.0F + (1.0F - ease) * 4.0F;
		float raw = 0;
		for (int i = 0; i < n; i++) {
			raw += f.getWidth(String.valueOf(title.charAt(i))) + (i < n - 1 ? spacing : 0);
		}
		float scale = n <= 3 ? 6.0F : Math.min(3.2F, w * 0.8F / Math.max(1, raw));
		scale *= 1.0F + (1.0F - ease) * 0.08F;
		float cy = h * 0.70F;
		// Soft plate: a horizontal band fading out to both sides.
		float plateH = 9 * scale + (cap.sub().isEmpty() ? 6 : 22);
		int slices = 24;
		float plateW = raw * scale + 80;
		for (int k = 0; k < slices; k++) {
			float u0 = k / (float) slices, u1 = (k + 1) / (float) slices;
			float mid = (u0 + u1) * 0.5F;
			float fall = 1.0F - (float) Math.pow(Math.abs(mid * 2 - 1), 2.0);
			int al = (int) (0x70 * fall * ease);
			int x0 = (int) (w / 2.0F - plateW / 2 + plateW * u0), x1 = (int) (w / 2.0F - plateW / 2 + plateW * u1);
			ctx.fillGradient(x0, (int) (cy - 6 * scale - 4), x1, (int) cy, 0, al << 24);
			ctx.fillGradient(x0, (int) cy, x1, (int) (cy - 6 * scale - 4 + plateH), al << 24, 0);
		}
		// Title, letter by letter.
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w / 2.0F, cy, 0);
		ctx.getMatrices().scale(scale, scale, 1);
		float x = -raw / 2;
		for (int i = 0; i < n; i++) {
			String ch = String.valueOf(title.charAt(i));
			float ca = MathHelper.clamp(a * (n * 0.6F + 1) - i * 0.6F, 0.0F, 1.0F);
			if (ca > 0.02F) {
				ctx.getMatrices().push();
				ctx.getMatrices().translate(x, -4 + (1 - ca) * 2, 0);
				ctx.drawText(f, ch, 0, 0, alpha(ca) | (cap.color() & 0xFFFFFF), true);
				ctx.getMatrices().pop();
			}
			x += f.getWidth(ch) + spacing;
		}
		ctx.getMatrices().pop();
		// Accent line opening out from the centre.
		float lineW = raw * scale * 0.6F * ease;
		int ly = (int) (cy + 6 * scale);
		int lc = cap.color() & 0xFFFFFF;
		int la = (int) (0xC0 * ease);
		int steps = 10;
		for (int k = 0; k < steps; k++) {
			float u0 = k / (float) steps, u1 = (k + 1) / (float) steps;
			int al = (int) (la * (1 - u0));
			ctx.fill((int) (w / 2.0F + lineW * u0), ly, (int) (w / 2.0F + lineW * u1) + 1, ly + 1, (al << 24) | lc);
			ctx.fill((int) (w / 2.0F - lineW * u1) - 1, ly, (int) (w / 2.0F - lineW * u0), ly + 1, (al << 24) | lc);
		}
		if (!cap.sub().isEmpty()) {
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2.0F, ly + 5, 0);
			ctx.getMatrices().scale(1.4F, 1.4F, 1);
			String sub = cap.sub().toUpperCase(java.util.Locale.ROOT);
			ctx.drawCenteredTextWithShadow(f, Text.literal(sub), 0, 0, alpha(a * 0.9F) | 0xD0D0D0);
			ctx.getMatrices().pop();
		}
	}

	private WmdHud() {
	}
}
