package com.armageddon.item;

import com.armageddon.tier.Tier;
import com.armageddon.tier.Tiers;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

import java.util.List;

public final class Tooltips {
	/** Adds {@code <key>.desc}, {@code <key>.desc2}... lines if the language defines them. */
	/** Applies the arsenal tier colouring to an item's name, if it has a tier. */
	public static Text name(Item item, Text base) {
		Tier tier = Tiers.of(item);
		return tier == null ? base : tier.decorate(base);
	}

	public static void describe(Item item, List<Text> tooltip) {
		Tier tier = Tiers.of(item);
		if (tier != null) {
			tooltip.add(tier.badge());
		}
		String key = item.getTranslationKey() + ".desc";
		Language lang = Language.getInstance();
		if (lang.hasTranslation(key)) {
			tooltip.add(Text.translatable(key).formatted(Formatting.GRAY));
		}
		for (int i = 2; i <= 4; i++) {
			String k = key + i;
			if (lang.hasTranslation(k)) {
				tooltip.add(Text.translatable(k).formatted(Formatting.DARK_GRAY));
			}
		}
	}

	public static Text stat(String key, Object value, Formatting color) {
		return Text.translatable("tooltip.armageddon." + key, value).formatted(color);
	}

	private Tooltips() {
	}
}
