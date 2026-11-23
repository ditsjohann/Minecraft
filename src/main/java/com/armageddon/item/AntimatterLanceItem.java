package com.armageddon.item;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 21 Antimatter Lance: charge, release — a white-violet line that erases a sphere where it lands. The boom arrives late. */
public class AntimatterLanceItem extends GunItem {
	private static final double RANGE = 192;

	public AntimatterLanceItem(Settings settings) {
		super(settings, Trigger.CHARGE, () -> ModItems.ANTIMATTER_CELL, 2, 60, 80);
	}

	@Override
	protected int chargeTicks() {
		return 40;
	}

	@Override
	protected void onCharging(World world, LivingEntity user, ItemStack stack, int used) {
		if (world instanceof ServerWorld server && user instanceof PlayerEntity p) {
			if (used == 1) {
				Vfx.charge(server, p.getId(), muzzle(p, 1.0), 1.5F, 0xE070FF, chargeTicks());
				server.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ANTIMATTER, SoundCategory.PLAYERS, 1.0F, 1.8F);
			}
			if (used % 3 == 0 && used <= chargeTicks()) {
				Vfx.play(server, VfxIds.LANCE_CHARGE, muzzle(p, 1.0), p.getRotationVec(1.0F), 0.5F + used / (float) chargeTicks(), 0);
			}
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d from = muzzle(player, 1.0);
		Vec3d dir = player.getRotationVec(1.0F);
		Hitscan.Result r = Hitscan.cast(world, player, player.getEyePos(), dir, RANGE);
		Vec3d end = r.end();
		Vfx.beam(world, from, end, 0.9F, 0xE070FF, 18);
		Vfx.beam(world, from, end, 0.35F, 0xFFFFFF, 20);
		Vfx.orb(world, end, 4.5F, 0.0F, 0x100018, 1, 20);
		Vfx.play(world, VfxIds.LANCE_MUZZLE, from, dir, 1.0F, 0);
		Vfx.play(world, VfxIds.LANCE_TRAIL, from, end.subtract(from), 1.0F, 0);
		Vfx.play(world, VfxIds.ANTIMATTER_FLASH, end, 1.2F);
		for (Entity e : Hitscan.pierce(world, player, from, end, 1.0)) {
			e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, player, player), 45.0F);
		}
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new net.minecraft.util.math.Box(end, end).expand(5), e -> e != player)) {
			e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, player, player), 30.0F);
		}
		if (Destruction.enabled(world)) {
			DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(end), 4.5F, CraterJob.Mode.ANTIMATTER));
		}
		EnergyFields.emit(world, EnergyFields.Energy.ANTIMATTER, end, 6, 100, player);
		recoil(player, 1.2);
		world.playSound(null, from.x, from.y, from.z, ModSounds.RAILGUN_FIRE, SoundCategory.PLAYERS, 3.0F, 0.5F);
		// The sonic boom catches up a moment later.
		double delay = Math.min(30, end.distanceTo(from) / 8);
		DetonationManager.schedule(world, (int) delay + 4, () -> {
			world.playSound(null, end.x, end.y, end.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 6.0F, 0.6F);
			world.playSound(null, end.x, end.y, end.z, ModSounds.ANTIMATTER, SoundCategory.PLAYERS, 6.0F, 1.0F);
			Vfx.ring(world, end, 18.0F, 1.0F, 0xE070FF, false, 15);
			Vfx.shake(world, end, 64, 1.4F);
		});
	}
}
