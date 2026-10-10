package com.armageddon.item;

import com.armageddon.detonation.Detonations;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Pressurised napalm stream: cone damage, ignites everything it touches. */
public class FlamethrowerItem extends GunItem {
	private static final double RANGE = 10.0;

	public FlamethrowerItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.FUEL_CANISTER, 120, 0, 40);
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d m = muzzle(player, 1.0);
		for (int i = 0; i < 5; i++) {
			Vec3d v = look.add(world.random.nextGaussian() * 0.07, world.random.nextGaussian() * 0.07, world.random.nextGaussian() * 0.07).multiply(0.6 + world.random.nextDouble() * 0.35);
			world.spawnParticles(ModParticles.NAPALM_FLAME, m.x, m.y, m.z, 0, v.x, v.y, v.z, 1.0);
		}
		if (heldTicks % 4 == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER, SoundCategory.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.2F);
		}
		if (heldTicks % 2 != 0) {
			return;
		}
		Vec3d eye = player.getEyePos();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(eye, eye).expand(RANGE), e -> e != player && e.isAlive())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d > RANGE || to.normalize().dotProduct(look) < 0.9 || !player.canSee(e)) {
				continue;
			}
			e.setOnFireFor(8.0F);
			e.timeUntilRegen = Math.min(e.timeUntilRegen, 5);
			e.damage(world.getDamageSources().inFire(), 3.0F);
		}
		if (Detonations.blockDamage(world)) {
			BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(look.multiply(RANGE)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockPos firePos = hit.getBlockPos().offset(hit.getSide());
				if (world.getBlockState(firePos).isAir() && AbstractFireBlock.canPlaceAt(world, firePos, hit.getSide())) {
					world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos), Block.NOTIFY_ALL);
				}
			}
		}
	}
}
