package com.armageddon.item;

import com.armageddon.detonation.Detonations;
import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Electromagnetic railgun. Charge, release: an instant slug that pierces every entity on the
 * line and punches through up to a dozen soft blocks.
 */
public class RailgunItem extends GunItem {
	private static final double RANGE = 160;

	public RailgunItem(Settings settings) {
		super(settings, Trigger.CHARGE, () -> ModItems.RAILGUN_SLUG, 4, 20, 50);
	}

	@Override
	protected int chargeTicks() {
		return 28;
	}

	@Override
	protected void onCharging(World world, LivingEntity user, ItemStack stack, int usedTicks) {
		if (usedTicks == 1 && !world.isClient) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.RAILGUN_CHARGE, SoundCategory.PLAYERS, 1.2F, 0.8F);
		}
		if (world instanceof ServerWorld server && user instanceof PlayerEntity p && usedTicks % 2 == 0) {
			float charge = Math.min(1.0F, usedTicks / (float) chargeTicks());
			Vfx.play(server, VfxIds.RAIL_CHARGE, muzzle(p, 0.8), p.getRotationVec(1.0F), 0.4F + charge * 0.8F, 0);
		}
		if (usedTicks == chargeTicks() && !world.isClient) {
			world.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 0.6F, 2.0F);
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0F);
		Vec3d end = start.add(dir.multiply(RANGE));

		// Punch through soft blocks.
		boolean griefing = Detonations.blockDamage(world);
		int broken = 0;
		BlockPos last = null;
		for (double t = 0; t < RANGE; t += 0.3) {
			Vec3d p = start.add(dir.multiply(t));
			BlockPos bp = BlockPos.ofFloored(p);
			if (bp.equals(last)) {
				continue;
			}
			last = bp;
			BlockState state = world.getBlockState(bp);
			if (state.isAir() || state.getCollisionShape(world, bp).isEmpty()) {
				continue;
			}
			float hardness = state.getHardness(world, bp);
			if (griefing && hardness >= 0 && hardness < 3.0F && broken < 12) {
				world.breakBlock(bp, true, player);
				broken++;
				continue;
			}
			end = p;
			break;
		}

		float damage = 34.0F;
		for (Entity e : Hitscan.pierce(world, player, start, end, 0.4)) {
			if (e instanceof LivingEntity living) {
				living.timeUntilRegen = 0;
			}
			e.damage(ModDamageTypes.source(world, ModDamageTypes.RAILGUN, player, player), damage);
			e.addVelocity(dir.x * 1.2, 0.3, dir.z * 1.2);
			e.velocityModified = true;
			damage *= 0.85F;
		}
		Vec3d muzzle = muzzle(player, 1.0);
		ModNetworking.sendFx(world, start, 256, new FxPayload(FxType.BEAM, muzzle, end, 0.18F, 0x66E0FF, 22));
		ModNetworking.sendFx(world, start, 32, new FxPayload(FxType.SHAKE, start, 0.6F, 0));
		Vfx.play(world, VfxIds.RAIL_MUZZLE, muzzle, dir, 1.0F, 0);
		Vfx.play(world, VfxIds.RAIL_ION, muzzle, end.subtract(muzzle), 1.0F, 0);
		Vfx.play(world, VfxIds.RAIL_IMPACT, end, dir.multiply(-1), 1.0F, 0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RAILGUN_FIRE, SoundCategory.PLAYERS, 3.0F, 1.0F);
		recoil(player, 0.5);
	}
}
