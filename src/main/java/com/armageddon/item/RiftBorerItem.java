package com.armageddon.item;

import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** 24 Rift Borer: a held void drill that bores perfectly round tunnels through anything soft enough. */
public class RiftBorerItem extends GunItem {
	public RiftBorerItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.PLASMA_CELL, 80, 30, 50);
	}

	@Override
	protected int spinUpTicks() {
		return 8;
	}

	@Override
	protected int fireInterval() {
		return 2;
	}

	@Override
	protected void onTriggerHeld(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		if (heldTicks == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_SPIN, SoundCategory.PLAYERS, 1.0F, 0.5F);
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d from = muzzle(player, 0.9);
		Hitscan.Result r = Hitscan.cast(world, player, player.getEyePos(), player.getRotationVec(1.0F), 40);
		Vec3d end = r.end();
		Vfx.continuousBeam(world, from, end, 0.35F, 0x9040FF);
		if (r.entity() != null) {
			r.entity().damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, player, player), 5.0F);
		}
		if (r.block() != null) {
			Kit.sphere(world, BlockPos.ofFloored(end.add(player.getRotationVec(1.0F).multiply(0.5))), 1.8, Blocks.AIR.getDefaultState());
			world.spawnParticles(ModParticles.VOID_SPARK, end.x, end.y, end.z, 6, 0.6, 0.6, 0.6, 0.05);
		}
		if (heldTicks % 6 == 0) {
			world.playSound(null, end.x, end.y, end.z, ModSounds.BLACK_HOLE, SoundCategory.PLAYERS, 0.8F, 1.8F);
		}
	}
}
