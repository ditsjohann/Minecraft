package com.armageddon.item;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Dubstep Gun. Hold the trigger and it plays a wobble-bass loop; every note is a rainbow
 * sonic beam that throws enemies around. Every second bar, THE DROP detonates at the target.
 */
public class DubstepGunItem extends GunItem {
	/** One bar of 16 steps. Values are wub pitches, 0 = rest. */
	private static final float[] BAR = {0.5F, 0, 0.5F, 0.75F, 0, 0.5F, 1.0F, 0, 0.5F, 0, 0.6F, 0.6F, 0.6F, 0.6F, 1.2F, 1.6F};
	private static final double RANGE = 48;

	public DubstepGunItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.PLASMA_CELL, 160, 0, 30);
	}

	@Override
	protected int fireInterval() {
		return 2;
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		int step = (heldTicks / 2) % 16;
		int bar = (heldTicks / 32) % 2;
		float note = BAR[step];
		if (note <= 0) {
			return;
		}
		Vec3d eye = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0F);
		Hitscan.Result hit = Hitscan.cast(world, player, eye, dir, RANGE);
		Vec3d end = hit.entity() != null ? hit.end() : (hit.block() != null ? hit.end() : eye.add(dir.multiply(RANGE)));
		Vec3d muzzle = muzzle(player, 1.0);

		ModNetworking.sendFx(world, eye, 192, new FxPayload(FxType.DUBSTEP_BEAM, muzzle, end, 0.35F + note * 0.25F, 0xFFFFFF, heldTicks / 2));
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DUBSTEP_WUB, SoundCategory.PLAYERS, 2.0F, note);

		for (Entity e : Hitscan.pierce(world, player, eye, end, 0.8)) {
			if (e instanceof LivingEntity living) {
				living.timeUntilRegen = 0;
			}
			e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, player, player), 3.0F + note * 2);
			e.addVelocity(dir.x * 0.7, 0.45 + note * 0.2, dir.z * 0.7);
			e.velocityModified = true;
		}

		boolean drop = bar == 1 && step == 15;
		if (drop) {
			ModNetworking.sendFx(world, end, 256, new FxPayload(FxType.BASS_PULSE, end, end, 9.0F, 0xFFFFFF, heldTicks));
			world.playSound(null, end.x, end.y, end.z, ModSounds.DUBSTEP_DROP, SoundCategory.PLAYERS, 4.0F, 1.0F);
			world.createExplosion(player, end.x, end.y, end.z, 3.0F, false,
					Detonations.blockDamage(world) ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(end, end).expand(8), x -> x != player && x.isAlive())) {
				e.addVelocity(0, 1.4, 0);
				e.velocityModified = true;
			}
		}
	}
}
