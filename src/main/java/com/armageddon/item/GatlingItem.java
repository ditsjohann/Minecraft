package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** The Apocalypse Gatling: a rotary cannon that fires a stream of tiny nuclear warheads. */
public class GatlingItem extends GunItem {
	public GatlingItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.MINI_NUKE, 12, 0, 60);
	}

	@Override
	protected int spinUpTicks() {
		return 24;
	}

	@Override
	protected int fireInterval() {
		return 4;
	}

	@Override
	protected void onTriggerHeld(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		if (heldTicks < spinUpTicks() && heldTicks % 6 == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_SPIN, SoundCategory.PLAYERS, 1.4F, 0.4F + heldTicks * 0.04F);
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		WmdProjectileEntity p = WmdProjectileEntity.fire(world, player, ProjectileKind.GATLING_NUKE, 2.6F, 3.0F);
		Vec3d m = muzzle(player, 1.4);
		p.setPosition(m.x, m.y, m.z);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.NUKE_LAUNCH, SoundCategory.PLAYERS, 1.6F, 1.2F + world.random.nextFloat() * 0.3F);
		Vfx.play(world, VfxIds.GATLING_MUZZLE, m, player.getRotationVec(1.0F), 1.0F, 0);
		recoil(player, 0.12);
	}
}
