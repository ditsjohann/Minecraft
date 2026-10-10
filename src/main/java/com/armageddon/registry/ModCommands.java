package com.armageddon.registry;

import com.armageddon.detonation.DetonationManager;
import com.armageddon.entity.BlackHoleEntity;
import com.armageddon.entity.FalloutCloudEntity;
import com.armageddon.entity.StarEntity;
import com.armageddon.entity.StrikeMarkerEntity;
import com.armageddon.world.CityBuildJob;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * /armageddon city [size]  — build a test city around you (size 1-4)
 * /armageddon purge        — remove fallout, black holes, stars, pending strikes and crater jobs
 */
public final class ModCommands {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				CommandManager.literal("armageddon")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("city")
								.executes(ctx -> city(ctx, 2))
								.then(CommandManager.argument("size", IntegerArgumentType.integer(1, 4))
										.executes(ctx -> city(ctx, IntegerArgumentType.getInteger(ctx, "size")))))
						.then(CommandManager.literal("purge").executes(ModCommands::purge))));
	}

	private static int city(CommandContext<ServerCommandSource> ctx, int size) {
		ServerCommandSource source = ctx.getSource();
		BlockPos origin = BlockPos.ofFloored(source.getPosition());
		CityBuildJob.start(source.getWorld(), origin, size);
		source.sendFeedback(() -> Text.translatable("message.armageddon.city.building"), true);
		return 1;
	}

	private static int purge(CommandContext<ServerCommandSource> ctx) {
		ServerCommandSource source = ctx.getSource();
		int removed = DetonationManager.clear();
		for (ServerWorld world : source.getServer().getWorlds()) {
			List<Entity> doomed = new ArrayList<>();
			for (Entity e : world.iterateEntities()) {
				if (e instanceof FalloutCloudEntity || e instanceof BlackHoleEntity || e instanceof StarEntity || e instanceof StrikeMarkerEntity) {
					doomed.add(e);
				}
			}
			doomed.forEach(Entity::discard);
			removed += doomed.size();
		}
		int total = removed;
		source.sendFeedback(() -> Text.translatable("message.armageddon.purged", total), true);
		return total;
	}

	private ModCommands() {
	}
}
