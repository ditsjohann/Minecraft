package com.armageddon.showcase;

import com.armageddon.registry.Catalog;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModItems;
import com.armageddon.tier.Tier;
import com.armageddon.tier.Tiers;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Screenshot gallery for mod pages.
 * <pre>
 * /armageddon showcase [all|weapons|explosives|bombs|cataclysms|gear|tiers|&lt;tier&gt;]
 * /armageddon showcase item &lt;item&gt;   — one item, large, with name, tier and description
 * /armageddon showcase clear          — remove the gallery and restore the ground
 * </pre>
 * Items float on lit pedestals above a dark floor with a backdrop wall, each with a coloured
 * name tag. Rows step upward away from the camera so every row is visible in one shot.
 */
public final class Showcase {
	public static final String TAG = "armageddon_showcase";
	private static final int COLUMNS = 9;
	private static final int SPACING = 3;

	/** Blocks overwritten by the gallery, restored by {@code clear}. */
	private static final Map<BlockPos, BlockState> RESTORE = new LinkedHashMap<>();
	@Nullable
	private static ServerWorld restoreWorld;

	private Showcase() {
	}

	public static LiteralArgumentBuilder<ServerCommandSource> command(CommandRegistryAccess access) {
		return CommandManager.literal("showcase")
				.executes(ctx -> gallery(ctx, "all"))
				.then(CommandManager.literal("clear").executes(Showcase::clear))
				.then(CommandManager.literal("item")
						.then(CommandManager.argument("item", ItemStackArgumentType.itemStack(access))
								.executes(ctx -> single(ctx, ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem()))))
				.then(CommandManager.argument("section", StringArgumentType.word())
						.suggests((ctx, b) -> CommandSource.suggestMatching(List.of("all", "weapons", "explosives", "bombs", "cataclysms", "gear", "tiers",
								"uncommon", "rare", "epic", "legendary", "mythic", "celestial", "city_destruction", "world_destruction"), b))
						.executes(ctx -> gallery(ctx, StringArgumentType.getString(ctx, "section"))));
	}

	// ------------------------------------------------------------------ sections

	private record Section(Text title, int color, List<Item> items) {
	}

	private static int categoryColor(Catalog.Category c) {
		return switch (c) {
			case WEAPONS -> 0x7AF6FF;
			case EXPLOSIVES -> 0xFFB040;
			case BOMBS -> 0xFF6030;
			case CATACLYSMS -> 0xFF40FF;
			case GEAR -> 0xC8D0D8;
			case NEW -> 0x80FFB0;
		};
	}

	/**
	 * "all" (the creative tabs, in order), one category key, "tiers" (every arsenal tier), or one
	 * tier key.
	 */
	private static List<Section> sections(String filter) {
		List<Section> out = new ArrayList<>();
		for (Catalog.Category c : Catalog.Category.values()) {
			if (filter.equals("all") || filter.equals(c.key)) {
				out.add(new Section(Text.translatable("itemGroup.armageddon." + c.key), categoryColor(c), Catalog.of(c)));
			}
		}
		List<Item> all = Catalog.all();
		for (Tier tier : Tier.values()) {
			if (!filter.equals("tiers") && !filter.equals(tier.key)) {
				continue;
			}
			List<Item> items = all.stream().filter(i -> Tiers.of(i) == tier).toList();
			if (!items.isEmpty()) {
				out.add(new Section(Text.translatable("tier.armageddon." + tier.key), tier.color, items));
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ gallery

	private static int gallery(CommandContext<ServerCommandSource> ctx, String filter) throws CommandSyntaxException {
		ServerCommandSource src = ctx.getSource();
		ServerPlayerEntity player = src.getPlayerOrThrow();
		ServerWorld world = src.getWorld();
		List<Section> sections = sections(filter.toLowerCase(Locale.ROOT));
		if (sections.isEmpty()) {
			src.sendError(Text.literal("Unknown showcase section: " + filter));
			return 0;
		}
		clearEntities(world);
		restore();
		restoreWorld = world;

		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockPos origin = player.getBlockPos().offset(forward, 3);
		int baseY = origin.getY() - 1;
		float yaw = forward.getOpposite().asRotation();

		int row = 0;
		int width = COLUMNS * SPACING;
		int startCol = -(width / 2) + 1;
		int count = 0;
		for (Section section : sections) {
			// Section header on its own row.
			int step = row;
			Vec3d headerPos = cell(origin, forward, right, 0, row, step).add(0, 1.6, 0);
			floorRow(world, origin, forward, right, row, step, width, section.color);
			text(world, headerPos, yaw, Text.empty().append(section.title).styled(s -> s.withColor(TextColor.fromRgb(section.color)).withBold(true)), 3.0F, 0x00000000);
			row++;
			for (int i = 0; i < section.items.size(); i++) {
				int col = i % COLUMNS;
				if (col == 0) {
					step = row;
					floorRow(world, origin, forward, right, row, step, width, section.color);
				}
				Vec3d base = cell(origin, forward, right, startCol + col * SPACING, row, step);
				pedestal(world, BlockPos.ofFloored(base).down(), section.color);
				Item item = section.items.get(i);
				// The glass pedestal fills this cell's first block; the item floats above it.
				itemDisplay(world, base.add(0, 1.75, 0), yaw, item, 1.15F);
				text(world, base.add(0, 2.55, 0), yaw, label(item), 0.55F, 0x60000000);
				count++;
				if (col == COLUMNS - 1 || i == section.items.size() - 1) {
					row++;
				}
			}
		}
		backdrop(world, origin, forward, right, row, width);
		int total = count;
		src.sendFeedback(() -> Text.literal("Showcase built: " + total + " items. Use /armageddon showcase clear to remove it.")
				.formatted(Formatting.GREEN), false);
		return total;
	}

	/** World position of a cell: rows recede and rise like stadium seating. */
	private static Vec3d cell(BlockPos origin, Direction forward, Direction right, int col, int row, int step) {
		BlockPos p = origin.offset(forward, row * SPACING).offset(right, col).up(step);
		return Vec3d.ofBottomCenter(p);
	}

	private static void floorRow(ServerWorld world, BlockPos origin, Direction forward, Direction right, int row, int step, int width, int color) {
		for (int d = 0; d < SPACING; d++) {
			for (int c = -width / 2 - 1; c <= width / 2 + 1; c++) {
				BlockPos p = origin.offset(forward, row * SPACING + d - 1).offset(right, c).up(step - 1);
				boolean edge = d == SPACING - 1 && Math.abs(c) % 3 == 0;
				set(world, p, edge ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState());
				// Clear the air above the shelf.
				for (int h = 1; h <= 5; h++) {
					set(world, p.up(h), Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	private static void pedestal(ServerWorld world, BlockPos p, int color) {
		set(world, p, Blocks.POLISHED_BLACKSTONE.getDefaultState());
		set(world, p.up(), glass(color).getDefaultState());
	}

	private static Block glass(int color) {
		return switch (color) {
			case 0xFFFF55 -> Blocks.YELLOW_STAINED_GLASS;
			case 0x55FFFF -> Blocks.LIGHT_BLUE_STAINED_GLASS;
			case 0xFF55FF -> Blocks.MAGENTA_STAINED_GLASS;
			case 0xFFB000 -> Blocks.ORANGE_STAINED_GLASS;
			case 0xFF2020 -> Blocks.RED_STAINED_GLASS;
			case 0xFFFFFF -> Blocks.WHITE_STAINED_GLASS;
			case 0xFF0000 -> Blocks.BLACK_STAINED_GLASS;
			default -> Blocks.GRAY_STAINED_GLASS;
		};
	}

	private static void backdrop(ServerWorld world, BlockPos origin, Direction forward, Direction right, int rows, int width) {
		BlockPos back = origin.offset(forward, rows * SPACING);
		for (int c = -width / 2 - 1; c <= width / 2 + 1; c++) {
			for (int h = -1; h <= rows + 6; h++) {
				set(world, back.offset(right, c).up(h), h % 6 == 5 ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState());
			}
		}
	}

	private static void set(ServerWorld world, BlockPos p, BlockState state) {
		BlockPos key = p.toImmutable();
		BlockState old = world.getBlockState(key);
		if (old.hasBlockEntity() || old.getHardness(world, key) < 0) {
			return;
		}
		RESTORE.putIfAbsent(key, old);
		world.setBlockState(key, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
	}

	// ------------------------------------------------------------------ single hero shot

	private static int single(CommandContext<ServerCommandSource> ctx, Item item) throws CommandSyntaxException {
		ServerCommandSource src = ctx.getSource();
		ServerPlayerEntity player = src.getPlayerOrThrow();
		ServerWorld world = src.getWorld();
		clearEntities(world);
		Vec3d look = Vec3d.fromPolar(0, player.getYaw());
		Vec3d at = player.getEyePos().add(look.multiply(3.2)).add(0, -0.4, 0);
		float yaw = player.getYaw() + 180.0F;
		itemDisplay(world, at, yaw, item, 2.6F);
		text(world, at.add(0, 1.55, 0), yaw, label(item), 1.0F, 0x80000000);
		String desc = item.getTranslationKey() + ".desc";
		if (Language.getInstance().hasTranslation(desc)) {
			MutableText lines = Text.translatable(desc).formatted(Formatting.GRAY);
			String desc2 = desc + "2";
			if (Language.getInstance().hasTranslation(desc2)) {
				lines.append("\n").append(Text.translatable(desc2).formatted(Formatting.DARK_GRAY));
			}
			text(world, at.add(0, -1.45, 0), yaw, lines, 0.6F, 0x80000000);
		}
		src.sendFeedback(() -> Text.literal("Showcasing ").append(item.getName()), false);
		return 1;
	}

	private static Text label(Item item) {
		Tier tier = Tiers.of(item);
		int color = tier != null ? tier.color : 0xE0E0E0;
		MutableText name = Text.translatable(item.getTranslationKey())
				.styled(s -> s.withColor(TextColor.fromRgb(color)).withBold(tier != null && tier.ordinal() >= Tier.LEGENDARY.ordinal()));
		if (tier != null) {
			name.append(Text.literal("\n◆ ").append(Text.translatable("tier.armageddon." + tier.key))
					.styled(s -> s.withColor(TextColor.fromRgb(color)).withBold(false)));
		}
		return name;
	}

	// ------------------------------------------------------------------ display entities

	private static NbtList floats(float... v) {
		NbtList l = new NbtList();
		for (float f : v) {
			l.add(NbtFloat.of(f));
		}
		return l;
	}

	private static NbtCompound base(String type, Vec3d pos, float yaw) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", type);
		NbtList p = new NbtList();
		p.add(net.minecraft.nbt.NbtDouble.of(pos.x));
		p.add(net.minecraft.nbt.NbtDouble.of(pos.y));
		p.add(net.minecraft.nbt.NbtDouble.of(pos.z));
		nbt.put("Pos", p);
		nbt.put("Rotation", floats(yaw, 0.0F));
		NbtList tags = new NbtList();
		tags.add(net.minecraft.nbt.NbtString.of(TAG));
		nbt.put("Tags", tags);
		NbtCompound light = new NbtCompound();
		light.putInt("sky", 15);
		light.putInt("block", 15);
		nbt.put("brightness", light);
		nbt.putFloat("view_range", 4.0F);
		return nbt;
	}

	private static void transform(NbtCompound nbt, float scale) {
		NbtCompound t = new NbtCompound();
		t.put("translation", floats(0, 0, 0));
		t.put("left_rotation", floats(0, 0, 0, 1));
		t.put("scale", floats(scale, scale, scale));
		t.put("right_rotation", floats(0, 0, 0, 1));
		nbt.put("transformation", t);
	}

	private static void itemDisplay(ServerWorld world, Vec3d pos, float yaw, Item item, float scale) {
		NbtCompound nbt = base("minecraft:item_display", pos, yaw);
		nbt.put("item", new ItemStack(item).encode(world.getRegistryManager()));
		nbt.putString("item_display", "gui");
		nbt.putString("billboard", "fixed");
		nbt.putFloat("shadow_radius", 0.6F);
		nbt.putFloat("shadow_strength", 0.8F);
		transform(nbt, scale);
		spawn(world, nbt);
	}

	private static void text(ServerWorld world, Vec3d pos, float yaw, Text text, float scale, int background) {
		NbtCompound nbt = base("minecraft:text_display", pos, yaw);
		nbt.putString("text", Text.Serialization.toJsonString(text, world.getRegistryManager()));
		nbt.putString("billboard", "fixed");
		nbt.putString("alignment", "center");
		nbt.putInt("background", background);
		nbt.putInt("line_width", 200);
		nbt.putBoolean("shadow", true);
		transform(nbt, scale);
		spawn(world, nbt);
	}

	private static void spawn(ServerWorld world, NbtCompound nbt) {
		Entity e = EntityType.loadEntityWithPassengers(nbt, world, entity -> entity);
		if (e != null) {
			world.spawnEntity(e);
		}
	}

	// ------------------------------------------------------------------ cleanup

	private static void clearEntities(ServerWorld world) {
		List<Entity> doomed = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e.getCommandTags().contains(TAG)) {
				doomed.add(e);
			}
		}
		doomed.forEach(Entity::discard);
	}

	private static void restore() {
		if (restoreWorld != null) {
			List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(RESTORE.entrySet());
			// Restore in reverse so lanterns and walls come down before the ground returns.
			for (int i = entries.size() - 1; i >= 0; i--) {
				restoreWorld.setBlockState(entries.get(i).getKey(), entries.get(i).getValue(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}
		RESTORE.clear();
		restoreWorld = null;
	}

	private static int clear(CommandContext<ServerCommandSource> ctx) {
		int n = RESTORE.size();
		clearEntities(ctx.getSource().getWorld());
		restore();
		ctx.getSource().sendFeedback(() -> Text.literal("Showcase removed (" + n + " blocks restored)").formatted(Formatting.YELLOW), false);
		return 1;
	}
}
