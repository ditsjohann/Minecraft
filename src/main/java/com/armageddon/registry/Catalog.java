package com.armageddon.registry;

import com.armageddon.block.BombBlock;
import com.armageddon.item.AirhornItem;
import com.armageddon.item.AreaToolItem;
import com.armageddon.item.BoomerangItem;
import com.armageddon.item.CallerItem;
import com.armageddon.item.DetonatorItem;
import com.armageddon.item.GrenadeItem;
import com.armageddon.item.GunItem;
import com.armageddon.item.LauncherItem;
import com.armageddon.item.PlasmaCutterItem;
import com.armageddon.item.TargetingItem;
import com.armageddon.item.TetherItem;
import com.armageddon.tier.Tier;
import com.armageddon.tier.Tiers;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * One place that decides where every item belongs and in what order: used by the creative tabs
 * and the showcase gallery so both always agree.
 */
public final class Catalog {
	public enum Category {
		WEAPONS("weapons"),
		EXPLOSIVES("explosives"),
		BOMBS("bombs"),
		CATACLYSMS("cataclysms"),
		GEAR("gear"),
		/** The newest bombs, gathered in their own tab for now. */
		NEW("new");

		public final String key;

		Category(String key) {
			this.key = key;
		}
	}

	private Catalog() {
	}

	/** The bombs made most recently: they live in the "New" tab until it is folded back into the others. */
	private static final Set<com.armageddon.block.BombType> NEWEST = Set.of(
			com.armageddon.block.BombType.GOO_BOMB, com.armageddon.block.BombType.CACTUS_BOMB, com.armageddon.block.BombType.BLACKFORGE,
			com.armageddon.block.BombType.BLUE_INFRIT, com.armageddon.block.BombType.PLASMA_KOI, com.armageddon.block.BombType.DIMENSION_SHIFT,
			com.armageddon.block.BombType.FIREWORK_HAVOC, com.armageddon.block.BombType.CHINESE_LANTERN,
			com.armageddon.block.BombType.MAMMOTH_FOSSIL, com.armageddon.block.BombType.LEVIATHAN_FOSSIL, com.armageddon.block.BombType.PTEROSAUR_FOSSIL,
			com.armageddon.block.BombType.XENOFORM, com.armageddon.block.BombType.TERROR_MANIAC,
			com.armageddon.block.BombType.PUMPKIN_HORSEMAN,
			com.armageddon.block.BombType.PORTAL_OF_HELL);

	private static Set<Item> ammo() {
		return Set.of(ModItems.MINI_NUKE, ModItems.ROCKET, ModItems.PLASMA_CELL, ModItems.RAILGUN_SLUG, ModItems.BULLET_BELT,
				ModItems.FUEL_CANISTER, ModItems.CRYO_CANISTER, ModItems.SOLAR_CELL);
	}

	/** Power on one scale for old (vanilla rarity) and new (arsenal tier) items: 0 common .. 8 world destruction. */
	public static int power(Item item) {
		Tier tier = Tiers.of(item);
		if (tier != null) {
			return tier.ordinal() + 1;
		}
		return item.getDefaultStack().getRarity().ordinal();
	}

	public static Category category(Item item) {
		if (item instanceof BlockItem bi) {
			if (bi.getBlock() instanceof BombBlock bomb) {
				if (NEWEST.contains(bomb.getBombType())) {
					return Category.NEW;
				}
				return bomb.getBombType().isCelestial() ? Category.CATACLYSMS : Category.BOMBS;
			}
			return Category.GEAR;
		}
		if (item instanceof DetonatorItem) {
			return Category.BOMBS;
		}
		if (item instanceof GrenadeItem || item instanceof BoomerangItem) {
			return Category.EXPLOSIVES;
		}
		if (item instanceof GunItem || item instanceof SwordItem || item instanceof TargetingItem || item instanceof CallerItem
				|| item instanceof AirhornItem || item instanceof TetherItem) {
			return Category.WEAPONS;
		}
		return Category.GEAR;
	}

	/** Sub-group inside a tab, so similar things sit together. */
	static int kind(Item item) {
		return switch (category(item)) {
			case WEAPONS -> item instanceof LauncherItem ? 1 : item instanceof GunItem ? 0
					: item instanceof AirhornItem || item instanceof TetherItem ? 2 : item instanceof SwordItem ? 3 : 4;
			case BOMBS -> item instanceof DetonatorItem ? 0 : 1;
			case GEAR -> item instanceof ArmorItem ? 0 : item instanceof AreaToolItem || item instanceof PlasmaCutterItem ? 1
					: item instanceof BlockItem ? 5 : ammo().contains(item) ? 3 : isGadget(item) ? 2 : 4;
			default -> 0;
		};
	}

	private static boolean isGadget(Item item) {
		return item == ModItems.GEIGER_COUNTER || item == ModItems.RAD_X || item == ModItems.NANO_MEDKIT || item == ModItems.OVERDRIVE_STIM
				|| item == ModItems.CITY_CAPSULE || item == ModItems.WARDEN_WAND;
	}

	/** Every mod item (items first, then blocks) in registration order. */
	public static List<Item> all() {
		List<Item> out = new ArrayList<>(ModItems.ALL);
		out.addAll(ModBlocks.BLOCK_ITEMS);
		return out;
	}

	/** Items of one category, grouped by kind and then ordered by power (registration order breaks ties). */
	public static List<Item> of(Category category) {
		List<Item> all = all();
		List<Item> out = new ArrayList<>();
		for (Item item : all) {
			if (category(item) == category) {
				out.add(item);
			}
		}
		out.sort(Comparator.comparingInt(Catalog::kind).thenComparingInt(Catalog::power).thenComparingInt(all::indexOf));
		return out;
	}
}
