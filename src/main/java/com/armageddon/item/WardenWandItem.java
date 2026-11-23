package com.armageddon.item;

import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModComponents;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Warden Wand: click two blocks to create a protected region (full height) that no weapon in
 * the mod can damage. Operators only. Manage regions with /armageddon protect.
 */
public class WardenWandItem extends WmdItem {
	public WardenWandItem(Settings settings) {
		super(settings.maxCount(1), true);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player == null || !(ctx.getWorld() instanceof ServerWorld world)) {
			return ActionResult.SUCCESS;
		}
		if (!player.hasPermissionLevel(2)) {
			player.sendMessage(Text.translatable("message.armageddon.warden.denied").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}
		ItemStack stack = ctx.getStack();
		BlockPos pos = ctx.getBlockPos();
		BlockPos first = stack.get(ModComponents.CORNER);
		if (first == null) {
			stack.set(ModComponents.CORNER, pos);
			Vfx.play(world, VfxIds.WARDEN_SEAL, Vec3d.ofBottomCenter(pos.up()), 0.7F);
			player.sendMessage(Text.translatable("message.armageddon.warden.first", pos.toShortString()).formatted(Formatting.AQUA), true);
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0F, 1.2F);
			return ActionResult.SUCCESS;
		}
		stack.remove(ModComponents.CORNER);
		BlockPos a = new BlockPos(first.getX(), world.getBottomY(), first.getZ());
		BlockPos b = new BlockPos(pos.getX(), world.getTopY(), pos.getZ());
		ProtectedRegions regions = ProtectedRegions.get(world);
		String name = "zone_" + (regions.regions().size() + 1);
		regions.add(name, a, b);
		Vfx.play(world, VfxIds.WARDEN_SEAL, Vec3d.ofBottomCenter(first.up()), 1.3F);
		Vfx.play(world, VfxIds.WARDEN_SEAL, Vec3d.ofBottomCenter(pos.up()), 1.3F);
		Vfx.wirebox(world, new Vec3d(Math.min(a.getX(), b.getX()), pos.getY() - 4, Math.min(a.getZ(), b.getZ())),
				new Vec3d(Math.max(a.getX(), b.getX()) + 1, pos.getY() + 12, Math.max(a.getZ(), b.getZ()) + 1), 0x40FFA0, 100);
		player.sendMessage(Text.translatable("message.armageddon.warden.created", name).formatted(Formatting.GREEN), false);
		world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.5F);
		return ActionResult.SUCCESS;
	}
}
