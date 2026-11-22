package com.armageddon.item;

import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/** Tesla arc projector: chain lightning that jumps between up to six targets. */
public class ArcThrowerItem extends GunItem {
	public ArcThrowerItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.PLASMA_CELL, 30, 0, 40);
	}

	@Override
	protected int fireInterval() {
		return 6;
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d start = muzzle(player, 1.0);
		LivingEntity first = null;
		double bestDot = 0.93;
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(eye, eye).expand(24), e -> e != player && e.isAlive() && !e.isSpectator())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double dot = to.normalize().dotProduct(look);
			if (to.length() <= 24 && dot > bestDot && player.canSee(e)) {
				bestDot = dot;
				first = e;
			}
		}
		int seed = world.random.nextInt();
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ARC_ZAP, SoundCategory.PLAYERS, 1.2F, 0.8F + world.random.nextFloat() * 0.4F);
		Vfx.play(world, VfxIds.ARC_MUZZLE, start, look, 1.0F, 0);
		if (first == null) {
			Hitscan.Result miss = Hitscan.cast(world, player, eye, look, 18);
			ModNetworking.sendFx(world, start, 160, new FxPayload(FxType.ARC, start, miss.end(), 0.08F, 0xA8E6FF, seed));
			Vfx.play(world, VfxIds.ARC_BURST, miss.end(), 0.5F);
			return;
		}
		Set<Entity> hit = new HashSet<>();
		Vec3d from = start;
		LivingEntity current = first;
		float damage = 10.0F;
		for (int jump = 0; jump < 6 && current != null; jump++) {
			Vec3d at = current.getPos().add(0, current.getHeight() * 0.5, 0);
			ModNetworking.sendFx(world, from, 160, new FxPayload(FxType.ARC, from, at, 0.09F, 0xA8E6FF, seed + jump));
			Vfx.play(world, VfxIds.ARC_BURST, at, 0.7F);
			current.timeUntilRegen = 0;
			current.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION, player, player), damage);
			current.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 4, false, false));
			hit.add(current);
			damage *= 0.8F;
			from = at;
			LivingEntity next = null;
			double best = 8.0 * 8.0;
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, current.getBoundingBox().expand(8), e -> e != player && e.isAlive() && !hit.contains(e))) {
				double d = e.squaredDistanceTo(at);
				if (d < best) {
					best = d;
					next = e;
				}
			}
			current = next;
		}
	}
}
