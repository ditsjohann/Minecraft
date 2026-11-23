package com.armageddon.item;

import com.armageddon.event.events.LegendaryEvents;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 31 Solstice Sunlance: three seconds of gathering sunlight, then a molten line through the world. */
public class SunlanceItem extends GunItem {
	private static final double RANGE = 160;

	public SunlanceItem(Settings settings) {
		super(settings, Trigger.CHARGE, () -> ModItems.SOLAR_CELL, 3, 80, 60);
	}

	@Override
	protected int chargeTicks() {
		return 60;
	}

	@Override
	protected void onCharging(World world, LivingEntity user, ItemStack stack, int used) {
		if (!(user instanceof PlayerEntity p)) {
			return;
		}
		if (world instanceof ServerWorld server) {
			if (used == 1) {
				Vfx.charge(server, p.getId(), muzzle(p, 1.0), 3.0F, 0xFFD040, chargeTicks());
				Vfx.skyTint(server, p.getPos(), 96, 0xFFD080, 0.3F);
			}
			if (used % 10 == 0 && used <= chargeTicks()) {
				server.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 0.8F, 0.6F + used * 0.02F);
			}
			// Motes of sunlight stream in from all around, wider as it fills.
			if (used % 2 == 0) {
				Vfx.play(server, VfxIds.SUNLANCE_CHARGE, muzzle(p, 1.0), p.getRotationVec(1.0F), 0.6F + Math.min(1, used / (float) chargeTicks()), 0);
			}
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		BlockHitResult hit = TargetingItem.target(world, player, RANGE);
		Vec3d end = hit.getPos();
		LegendaryEvents.sunlance(world, player, muzzle(player, 1.0), end);
		recoil(player, 1.5);
	}
}
