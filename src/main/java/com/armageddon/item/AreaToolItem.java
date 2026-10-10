package com.armageddon.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/**
 * Mines a (2r+1)x(2r+1) face, {@code depth} blocks deep, around the targeted block.
 * Sneak to mine a single block.
 */
public class AreaToolItem extends MiningToolItem {
	private static boolean breaking;

	private final int radius;
	private final int depth;

	public AreaToolItem(ToolMaterial material, TagKey<Block> effective, Settings settings, int radius, int depth) {
		super(material, effective, settings);
		this.radius = radius;
		this.depth = depth;
	}

	@Override
	public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
		boolean result = super.postMine(stack, world, state, pos, miner);
		if (breaking || world.isClient || !(miner instanceof ServerPlayerEntity player) || player.isSneaking()) {
			return result;
		}
		Direction face = facing(world, player);
		float centerHardness = state.getHardness(world, pos);
		breaking = true;
		try {
			for (int d = 0; d < depth; d++) {
				for (int a = -radius; a <= radius; a++) {
					for (int b = -radius; b <= radius; b++) {
						if (d == 0 && a == 0 && b == 0) {
							continue;
						}
						BlockPos target = offset(pos, face, a, b, d);
						BlockState s = world.getBlockState(target);
						float h = s.getHardness(world, target);
						if (s.isAir() || h < 0 || h > centerHardness + 5.0F || !stack.isSuitableFor(s)) {
							continue;
						}
						player.interactionManager.tryBreakBlock(target);
						if (stack.isEmpty()) {
							return result;
						}
					}
				}
			}
		} finally {
			breaking = false;
		}
		return result;
	}

	static Direction facing(World world, ServerPlayerEntity player) {
		BlockHitResult hit = world.raycast(new RaycastContext(player.getEyePos(), player.getEyePos().add(player.getRotationVec(1.0F).multiply(6)),
				RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
		return hit.getType() == HitResult.Type.BLOCK ? hit.getSide() : Direction.getEntityFacingOrder(player)[0].getOpposite();
	}

	private static BlockPos offset(BlockPos pos, Direction face, int a, int b, int d) {
		BlockPos back = pos.offset(face.getOpposite(), d);
		return switch (face.getAxis()) {
			case X -> back.add(0, a, b);
			case Y -> back.add(a, 0, b);
			case Z -> back.add(a, b, 0);
		};
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
	}
}
