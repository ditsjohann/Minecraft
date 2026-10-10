package com.armageddon.item;

import com.armageddon.event.EventEntity;
import com.armageddon.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 17 Gravity Tether Gun: yank a creature to you, or grapple yourself to a surface. */
public class TetherItem extends WmdItem {
	public TetherItem(Settings settings) {
		super(settings.maxCount(1));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world instanceof ServerWorld server) {
			Vec3d eye = user.getEyePos();
			Vec3d look = user.getRotationVec(1.0F);
			double range = 40;
			BlockHitResult block = TargetingItem.target(world, user, range);
			double max = block.getType() == HitResult.Type.BLOCK ? block.getPos().distanceTo(eye) : range;
			Entity target = null;
			double best = max;
			for (Entity e : world.getOtherEntities(user, new Box(eye, eye.add(look.multiply(max))).expand(1.5), e -> e.isAlive() && !e.isSpectator())) {
				var hit = e.getBoundingBox().expand(0.5).raycast(eye, eye.add(look.multiply(max)));
				if (hit.isPresent() && hit.get().distanceTo(eye) < best) {
					best = hit.get().distanceTo(eye);
					target = e;
				}
			}
			if (target == null && block.getType() != HitResult.Type.BLOCK) {
				return TypedActionResult.fail(stack);
			}
			final Entity t = target;
			EventEntity.start(server, user.getPos(), "tether", user, nbt -> {
				if (t != null) {
					nbt.putInt("Target", t.getId());
				} else {
					nbt.putDouble("Ax", block.getPos().x);
					nbt.putDouble("Ay", block.getPos().y);
					nbt.putDouble("Az", block.getPos().z);
				}
			});
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.ARC_ZAP, SoundCategory.PLAYERS, 1.0F, 0.6F);
			user.getItemCooldownManager().set(this, 30);
		}
		return TypedActionResult.success(stack, world.isClient);
	}
}
