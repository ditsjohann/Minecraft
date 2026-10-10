package com.armageddon.registry;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public final class ModGameRules {
	/** When false, WMDs still hurt entities and look spectacular but leave terrain untouched. */
	public static final GameRules.Key<GameRules.BooleanRule> BLOCK_DAMAGE =
			GameRuleRegistry.register("armageddonBlockDamage", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	/** When false, nukes do not leave radioactive fallout zones behind. */
	public static final GameRules.Key<GameRules.BooleanRule> FALLOUT =
			GameRuleRegistry.register("armageddonFallout", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	/** Max blocks the crater carver may process per tick (performance knob). */
	public static final GameRules.Key<GameRules.IntRule> CRATER_SPEED =
			GameRuleRegistry.register("armageddonCraterSpeed", GameRules.Category.MISC, GameRuleFactory.createIntRule(60000, 1000, 1000000));

	public static void init() {
	}

	private ModGameRules() {
	}
}
