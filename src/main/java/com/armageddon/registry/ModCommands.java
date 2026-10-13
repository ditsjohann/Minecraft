package com.armageddon.registry;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.entity.DevourerEntity;
import com.armageddon.event.EventEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.BlockPosArgumentType;
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
 * /armageddon purge        — remove fallout, black holes, stars, pending strikes, world events and crater jobs
 * /armageddon protect add|remove|list — regions no weapon can damage
 * /armageddon showcase [section] | item <item> | clear — screenshot gallery
 * /armageddon restore [count] — undo recorded destruction (needs gamerule armageddonRecordRestore)
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
						.then(CommandManager.literal("purge").executes(ModCommands::purge))
						.then(com.armageddon.showcase.Showcase.command(registryAccess))
						.then(CommandManager.literal("event")
								.then(CommandManager.argument("id", StringArgumentType.word())
										.suggests((ctx, b) -> net.minecraft.command.CommandSource.suggestMatching(com.armageddon.event.Events.ids(), b))
										.executes(ModCommands::startEvent)))
						.then(CommandManager.literal("restore")
								.executes(ctx -> restore(ctx, 100000))
								.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
										.executes(ctx -> restore(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
						.then(CommandManager.literal("protect")
								.then(CommandManager.literal("add")
										.then(CommandManager.argument("name", StringArgumentType.word())
												.then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
														.then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
																.executes(ModCommands::protectAdd)))))
								.then(CommandManager.literal("remove")
										.then(CommandManager.argument("name", StringArgumentType.word())
												.executes(ModCommands::protectRemove)))
								.then(CommandManager.literal("list").executes(ModCommands::protectList)))));
	}

	/** Starts a scripted event at your feet, facing where you look (testing without crafting bombs). */
	private static int startEvent(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		String id = StringArgumentType.getString(ctx, "id");
		if (!com.armageddon.event.Events.ids().contains(id)) {
			ctx.getSource().sendError(Text.literal("Unknown event: " + id));
			return 0;
		}
		Entity player = ctx.getSource().getEntityOrThrow();
		float yaw = player.getYaw();
		EventEntity.start(ctx.getSource().getWorld(), player.getPos(), id, player, nbt -> nbt.putFloat("Yaw", yaw));
		ctx.getSource().sendFeedback(() -> Text.literal("Started event " + id), true);
		return 1;
	}

	private static int restore(CommandContext<ServerCommandSource> ctx, int count) {
		int n = Destruction.restore(count);
		int left = Destruction.journalSize();
		ctx.getSource().sendFeedback(() -> Text.translatable("message.armageddon.restored", n, left), true);
		return n;
	}

	private static int protectAdd(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		String name = StringArgumentType.getString(ctx, "name");
		BlockPos a = BlockPosArgumentType.getBlockPos(ctx, "from");
		BlockPos b = BlockPosArgumentType.getBlockPos(ctx, "to");
		ProtectedRegions.get(ctx.getSource().getWorld()).add(name, a, b);
		ctx.getSource().sendFeedback(() -> Text.translatable("message.armageddon.protect.added", name), true);
		return 1;
	}

	private static int protectRemove(CommandContext<ServerCommandSource> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		boolean ok = ProtectedRegions.get(ctx.getSource().getWorld()).remove(name);
		ctx.getSource().sendFeedback(() -> Text.translatable(ok ? "message.armageddon.protect.removed" : "message.armageddon.protect.missing", name), true);
		return ok ? 1 : 0;
	}

	private static int protectList(CommandContext<ServerCommandSource> ctx) {
		List<ProtectedRegions.Region> list = ProtectedRegions.get(ctx.getSource().getWorld()).regions();
		ctx.getSource().sendFeedback(() -> Text.translatable("message.armageddon.protect.list", list.size()), false);
		for (ProtectedRegions.Region r : list) {
			ctx.getSource().sendFeedback(() -> Text.literal(" - " + r.name() + "  [" + r.minX() + " " + r.minY() + " " + r.minZ()
					+ "] -> [" + r.maxX() + " " + r.maxY() + " " + r.maxZ() + "]"), false);
		}
		return list.size();
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
				if (e instanceof FalloutCloudEntity || e instanceof BlackHoleEntity || e instanceof StarEntity || e instanceof StrikeMarkerEntity
						|| e instanceof DevourerEntity) {
					doomed.add(e);
				} else if (e instanceof EventEntity ev) {
					ev.abort();
					removed++;
				}
			}
			doomed.forEach(Entity::discard);
			removed += TemporaryBlocks.revertAll(world);
			removed += doomed.size();
		}
		int total = removed;
		source.sendFeedback(() -> Text.translatable("message.armageddon.purged", total), true);
		return total;
	}

	private ModCommands() {
	}
}
