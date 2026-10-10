package com.armageddon.block;

import com.armageddon.entity.PrimedBombEntity;
import com.armageddon.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * A placeable warhead. Armed with flint &amp; steel, a fire charge, redstone, a linked detonator,
 * or by being caught in another explosion (sympathetic detonation).
 */
public class BombBlock extends Block {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

	private final BombType type;
	private final VoxelShape shapeNS;
	private final VoxelShape shapeEW;

	public BombBlock(BombType type, AbstractBlock.Settings settings) {
		super(settings);
		this.type = type;
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
		if (type == BombType.DEMOLITION_CHARGE) {
			this.shapeNS = Block.createCuboidShape(3, 0, 2, 13, 6, 14);
			this.shapeEW = Block.createCuboidShape(2, 0, 3, 14, 6, 13);
		} else {
			this.shapeNS = Block.createCuboidShape(1, 0, 0, 15, 14, 16);
			this.shapeEW = Block.createCuboidShape(0, 0, 1, 16, 14, 15);
		}
	}

	public BombType getBombType() {
		return type;
	}

	@Override
	public MapCodec<? extends Block> getCodec() {
		return createCodec(settings -> new BombBlock(this.type, settings));
	}

	@Override
	public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(FACING).getAxis() == Direction.Axis.Z ? shapeNS : shapeEW;
	}

	@Override
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!oldState.isOf(state.getBlock()) && world.isReceivingRedstonePower(pos)) {
			arm(world, pos, state, null, type.fuse);
		}
	}

	@Override
	public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
		if (world.isReceivingRedstonePower(pos)) {
			arm(world, pos, state, null, type.fuse);
		}
	}

	@Override
	public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
		if (!world.isClient) {
			// Sympathetic detonation: a short, randomised fuse so chains ripple outwards.
			int fuse = 10 + world.random.nextInt(20);
			PrimedBombEntity.spawn(world, pos, type, getDefaultState(), explosion.getCausingEntity(), fuse);
		}
	}

	@Override
	public boolean shouldDropItemsOnExplosion(Explosion explosion) {
		return false;
	}

	@Override
	public ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(Items.FLINT_AND_STEEL) && !stack.isOf(Items.FIRE_CHARGE)) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}
		arm(world, pos, state, player, type.fuse);
		if (!player.isCreative()) {
			if (stack.isOf(Items.FLINT_AND_STEEL)) {
				stack.damage(1, player, LivingEntity.getSlotForHand(hand));
			} else {
				stack.decrement(1);
			}
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (type.isStrategic() && random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.2 + random.nextDouble() * 0.6,
					pos.getY() + 0.9, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
		}
	}

	/** Removes the block and spawns a ticking primed warhead in its place. */
	public static void arm(World world, BlockPos pos, BlockState state, @Nullable LivingEntity igniter, int fuse) {
		if (world.isClient || !(state.getBlock() instanceof BombBlock bomb)) {
			return;
		}
		world.removeBlock(pos, false);
		PrimedBombEntity.spawn(world, pos, bomb.type, state, igniter, fuse);
		world.playSound(null, pos, ModSounds.BOMB_ARMED, SoundCategory.BLOCKS, 2.0F, 1.0F);
		world.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 0.6F);
	}
}
