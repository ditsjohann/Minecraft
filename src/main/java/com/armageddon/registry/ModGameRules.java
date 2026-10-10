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

	/** Upper bound for any WMD destruction radius (blocks). */
	public static final GameRules.Key<GameRules.IntRule> MAX_RADIUS =
			GameRuleRegistry.register("armageddonMaxRadius", GameRules.Category.MISC, GameRuleFactory.createIntRule(160, 4, 512));
	/** Celestial superweapons can be disabled entirely. */
	public static final GameRules.Key<GameRules.BooleanRule> ALLOW_CELESTIAL =
			GameRuleRegistry.register("armageddonAllowCelestial", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));
	/** Records every destroyed block so /armageddon restore can undo it (uses memory). */
	public static final GameRules.Key<GameRules.BooleanRule> RECORD_RESTORE =
			GameRuleRegistry.register("armageddonRecordRestore", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
	/** Hard cap on simultaneously flying debris entities spawned by the mod. */
	public static final GameRules.Key<GameRules.IntRule> MAX_DEBRIS =
			GameRuleRegistry.register("armageddonMaxDebris", GameRules.Category.MISC, GameRuleFactory.createIntRule(400, 0, 5000));

	public static void init() {
	}

	private ModGameRules() {
	}
}
