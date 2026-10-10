package com.armageddon.item;

import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

import java.util.List;

public final class Tooltips {
	/** Adds {@code <key>.desc}, {@code <key>.desc2}... lines if the language defines them. */
	public static void describe(Item item, List<Text> tooltip) {
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
