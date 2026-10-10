package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** Fully automatic plasma bolts. */
public class PlasmaRifleItem extends GunItem {
	public PlasmaRifleItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.PLASMA_CELL, 40, 0, 40);
	}

	@Override
	protected int fireInterval() {
		return 3;
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		WmdProjectileEntity bolt = WmdProjectileEntity.fire(world, player, ProjectileKind.PLASMA, 3.4F, 1.2F);
		Vec3d m = muzzle(player, 0.9);
		bolt.setPosition(m.x, m.y, m.z);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLASMA_FIRE, SoundCategory.PLAYERS, 0.9F, 0.9F + world.random.nextFloat() * 0.25F);
		muzzleFlash(world, player, 0.9);
	}
}
