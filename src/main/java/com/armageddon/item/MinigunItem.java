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
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** Six-barrel rotary cannon: spin-up, then a hitscan round every tick with tracers. */
public class MinigunItem extends GunItem {
	public MinigunItem(Settings settings) {
		super(settings, Trigger.AUTO, () -> ModItems.BULLET_BELT, 200, 0, 60);
	}

	@Override
	protected int spinUpTicks() {
		return 14;
	}

	@Override
	protected void onTriggerHeld(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		if (heldTicks == 0 || (heldTicks < spinUpTicks() && heldTicks % 7 == 0)) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_SPIN, SoundCategory.PLAYERS, 1.0F, 0.6F + heldTicks * 0.05F);
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d dir = Hitscan.spread(player.getRotationVec(1.0F), 2.2, world.random);
		Vec3d start = player.getEyePos();
		Hitscan.Result hit = Hitscan.cast(world, player, start, dir, 72);
		if (hit.entity() != null) {
			if (hit.entity() instanceof LivingEntity living) {
				living.timeUntilRegen = 0;
			}
			hit.entity().damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, player, player), 4.0F);
		} else if (hit.block() != null) {
			BlockState state = world.getBlockState(hit.block().getBlockPos());
			Vec3d p = hit.end();
			if (!state.isAir()) {
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.1);
			}
		}
		ModNetworking.sendFx(world, start, 160, new FxPayload(FxType.TRACER, muzzle(player, 1.1), hit.end(), 0.04F, 0xFFD27A, 0));
		if (heldTicks % 2 == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_FIRE, SoundCategory.PLAYERS, 1.2F, 0.9F + world.random.nextFloat() * 0.3F);
		}
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d m = muzzle(player, 1.1);
		Vfx.play(world, VfxIds.MINIGUN_MUZZLE, m, look, 1.0F, 0);
		Vfx.play(world, VfxIds.BULLET_IMPACT, hit.end(), dir.multiply(-1), 1.0F, 0);
		if (heldTicks % 2 == 0) {
			// Casings out of the right-hand side.
			Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
			Vfx.play(world, VfxIds.SHELL_CASING, muzzle(player, 0.4), right, 1.0F, 0);
		}
	}
}
