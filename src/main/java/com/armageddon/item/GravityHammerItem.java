package com.armageddon.item;

import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** Hits launch enemies skyward. Right-click: seismic ground slam that hurls everything away. */
public class GravityHammerItem extends SwordItem {
	public GravityHammerItem(ToolMaterial material, Settings settings) {
		super(material, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		Vec3d away = target.getPos().subtract(attacker.getPos()).normalize();
		target.addVelocity(away.x * 1.4, 0.8, away.z * 1.4);
		target.velocityModified = true;
		return super.postHit(stack, target, attacker);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world instanceof ServerWorld server) {
			slam(server, user);
			user.getItemCooldownManager().set(this, 50);
			stack.damage(2, user, LivingEntity.getSlotForHand(hand));
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	private static void slam(ServerWorld world, PlayerEntity user) {
		Vec3d c = user.getPos();
		double radius = 8.0;
		ModNetworking.sendFx(world, c, 128, new FxPayload(FxType.SHOCKWAVE, c.add(0, 0.1, 0), (float) radius, 0xB58CFF));
		ModNetworking.sendFx(world, c, 48, new FxPayload(FxType.SHAKE, c, 1.2F, 0));
		world.playSound(null, c.x, c.y, c.z, ModSounds.GRAVITY_SLAM, SoundCategory.PLAYERS, 2.5F, 0.6F);
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), e -> e != user && e.isAlive())) {
			Vec3d d = e.getPos().subtract(c);
			double dist = d.length();
			if (dist > radius) {
				continue;
			}
			double f = 1.0 - dist / radius;
			Vec3d dir = dist < 0.01 ? new Vec3d(0, 1, 0) : d.multiply(1.0 / dist);
			e.damage(world.getDamageSources().playerAttack(user), (float) (6 + 10 * f));
			e.addVelocity(dir.x * 2.2 * f, 0.6 + 1.0 * f, dir.z * 2.2 * f);
			e.velocityModified = true;
		}
		BlockPos below = user.getBlockPos().down();
		for (int i = 0; i < 40; i++) {
			double a = i / 40.0 * Math.PI * 2;
			for (double r = 1.5; r < radius; r += 2.5) {
				BlockPos bp = BlockPos.ofFloored(c.x + Math.cos(a) * r, c.y - 0.5, c.z + Math.sin(a) * r);
				BlockState state = world.getBlockState(bp);
				if (!state.isAir()) {
					world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), bp.getX() + 0.5, bp.getY() + 1.1, bp.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.3);
				}
			}
		}
		world.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2, c.z, 3, 1, 0.1, 1, 0);
		if (below.getY() < world.getBottomY()) {
			return;
		}
		user.addVelocity(0, 0.4, 0);
		user.velocityModified = true;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
	}
}
