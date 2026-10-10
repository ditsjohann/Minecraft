package com.armageddon.item;

import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 09 Airhorn Cannon: a cone of pure honk. Pushes, never hurts. Sneak for a deeper, wider blast. */
public class AirhornItem extends WmdItem {
	public AirhornItem(Settings settings) {
		super(settings.maxCount(1));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world instanceof ServerWorld server) {
			boolean deep = user.isSneaking();
			float pitch = deep ? 0.5F : 1.4F;
			Vec3d look = user.getRotationVec(1.0F);
			Vec3d from = GunItem.muzzle(user, 0.8);
			double range = deep ? 14 : 10;
			Vfx.soundwave(server, from, from.add(look.multiply(range)), deep ? 3.0F : 2.0F, 0xFFE040, pitch);
			server.playSound(null, from.x, from.y, from.z, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 2.0F, pitch * 1.4F);
			server.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.PLAYERS, 3.0F, pitch);
			double cone = deep ? 0.6 : 0.75;
			for (Entity e : Kit.entities(server, from, range, x -> x != user && !Kit.creativeOrSpectator(x))) {
				Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
				double d = to.length();
				if (d < 0.1 || to.multiply(1 / d).dotProduct(look) < cone) {
					continue;
				}
				double f = 1.0 - d / range;
				Kit.push(e, look.multiply(1.2 + 1.8 * f).add(0, 0.35 + 0.3 * f, 0));
				if (e instanceof LivingEntity l) {
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false));
				}
			}
			GunItem.recoil(user, 0.4);
			user.getItemCooldownManager().set(this, 30);
		}
		return TypedActionResult.success(stack, world.isClient);
	}
}
