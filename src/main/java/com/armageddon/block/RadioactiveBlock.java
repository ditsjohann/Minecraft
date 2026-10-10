package com.armageddon.block;

import com.armageddon.registry.ModEffects;
import com.armageddon.registry.ModParticles;
import com.armageddon.util.ArmorSets;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Glowing, mildly radioactive block (trinitite, uranium block...). Standing on it irradiates you. */
public class RadioactiveBlock extends Block {
	private final int strength;

	public RadioactiveBlock(int strength, Settings settings) {
		super(settings);
		this.strength = strength;
	}

	@Override
	public MapCodec<? extends Block> getCodec() {
		return createCodec(settings -> new RadioactiveBlock(this.strength, settings));
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (!world.isClient && entity instanceof LivingEntity living && world.getTime() % 20 == 0 && !ArmorSets.isRadiationSealed(living)) {
			living.addStatusEffect(new StatusEffectInstance(ModEffects.RADIATION, 100 * strength, strength - 1));
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(3) == 0) {
			world.addParticle(ModParticles.RADIATION, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
					pos.getZ() + random.nextDouble(), 0, 0.02 + random.nextDouble() * 0.02, 0);
		}
	}
}
