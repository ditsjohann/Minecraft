package com.armageddon.tier;

import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/** Item → tier lookup for the arsenal expansion. */
public final class Tiers {
	private static final Map<Item, Tier> TIERS = new IdentityHashMap<>();

	public static <T extends Item> T set(T item, Tier tier) {
		TIERS.put(item, tier);
		return item;
	}

	@Nullable
	public static Tier of(Item item) {
		return TIERS.get(item);
	}

	private Tiers() {
	}
}
