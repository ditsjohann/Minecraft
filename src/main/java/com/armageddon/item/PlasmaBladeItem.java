package com.armageddon.item;

import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

/** Superheated plasma sword. Hits ignite; right-click to phase-dash through enemies. */
public class PlasmaBladeItem extends SwordItem {
	public PlasmaBladeItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		target.setOnFireFor(5.0F);
		if (attacker.getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ModParticles.PLASMA_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.15);
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world instanceof ServerWorld server) {
			Vec3d look = user.getRotationVec(1.0F);
			Vec3d flat = new Vec3d(look.x, Math.max(-0.2, Math.min(0.35, look.y)), look.z).normalize();
			Vec3d start = user.getPos().add(0, 0.9, 0);
			BlockHitResult wall = world.raycast(new RaycastContext(start, start.add(flat.multiply(9)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
			double dist = wall.getType() == HitResult.Type.MISS ? 9 : Math.max(0, wall.getPos().distanceTo(start) - 0.8);
			Vec3d end = start.add(flat.multiply(dist));
			for (Entity e : Hitscan.pierce(world, user, start, end, 1.2)) {
				e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, user, user), 14.0F);
				e.setOnFireFor(6.0F);
			}
			user.setVelocity(flat.multiply(dist * 0.42).add(0, 0.15, 0));
			user.velocityModified = true;
			user.fallDistance = 0;
			ModNetworking.sendFx(server, start, 128, new FxPayload(FxType.BEAM, start, end, 0.35F, 0xFF4FD8, 10));
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.BLADE_DASH, SoundCategory.PLAYERS, 1.5F, 1.2F);
			user.getItemCooldownManager().set(this, 40);
			stack.damage(1, user, LivingEntity.getSlotForHand(hand));
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
	}
}
