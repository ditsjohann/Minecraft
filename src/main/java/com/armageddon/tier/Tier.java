package com.armageddon.tier;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;

/**
 * Arsenal rarity tiers. Uncommon/Rare/Epic match vanilla's colours; Legendary, Mythic and
 * Celestial are new. Celestial names shimmer and occasionally scramble like secret text.
 */
public enum Tier {
	UNCOMMON(0xFFFF55, "uncommon"),
	RARE(0x55FFFF, "rare"),
	EPIC(0xFF55FF, "epic"),
	LEGENDARY(0xFFB000, "legendary"),
	MYTHIC(0xFF2020, "mythic"),
	CELESTIAL(0xFFFFFF, "celestial");

	public final int color;
	public final String key;

	Tier(int color, String key) {
		this.color = color;
		this.key = key;
	}

	/** Recolours an item name for this tier. */
	public Text decorate(Text name) {
		return switch (this) {
			case CELESTIAL -> shimmer(name.getString());
			case MYTHIC -> name.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(pulse(0xFF2020, 0x8A0000))).withBold(true));
			case LEGENDARY -> name.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(pulse(0xFFC830, 0xFF8A00))).withBold(true));
			default -> name.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
		};
	}

	/** Tooltip badge, e.g. "◆ LEGENDARY". */
	public Text badge() {
		MutableText t = Text.translatable("tier.armageddon." + key);
		if (this == CELESTIAL) {
			return shimmer("✦ " + t.getString() + " ✦");
		}
		return Text.literal("◆ ").append(t).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(this.ordinal() >= LEGENDARY.ordinal()));
	}

	private static int pulse(int a, int b) {
		float t = (MathHelper.sin((System.currentTimeMillis() % 100000L) / 300.0F) + 1.0F) * 0.5F;
		int r = (int) MathHelper.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	/** Per-character iridescent gradient that drifts over time; some letters flicker as obfuscated glyphs. */
	private static Text shimmer(String s) {
		MutableText out = Text.empty();
		long now = System.currentTimeMillis();
		float phase = (now % 4000L) / 4000.0F;
		long flickerSlot = now / 120L;
		for (int i = 0; i < s.length(); i++) {
			float hue = (phase + i * 0.045F) % 1.0F;
			int rgb = MathHelper.hsvToRgb(0.5F + 0.35F * MathHelper.sin(hue * MathHelper.TAU), 0.35F, 1.0F);
			boolean flicker = s.charAt(i) != ' ' && ((flickerSlot * 31 + i * 17) % 23 == 0);
			Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb)).withBold(true).withObfuscated(flicker);
			out.append(Text.literal(String.valueOf(s.charAt(i))).setStyle(style));
		}
		return out;
	}
}
