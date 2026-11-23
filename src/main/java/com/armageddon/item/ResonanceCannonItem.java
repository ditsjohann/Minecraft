package com.armageddon.item;

import com.armageddon.block.BombBlock;
import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.EnergyFields;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** 29 Resonance Cannon: tuned sound that shatters glass and ice, and sets off explosives at range. */
public class ResonanceCannonItem extends GunItem {
	private static final double RANGE = 36;

	public ResonanceCannonItem(Settings settings) {
		super(settings, Trigger.SEMI, () -> Items.AMETHYST_SHARD, 8, 25, 40);
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		Vec3d from = muzzle(player, 0.9);
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d end = from.add(look.multiply(RANGE));
		Vfx.soundwave(world, from, end, 2.5F, 0x70E0FF, 0.7F);
		Vfx.play(world, VfxIds.RESONANCE_WAVE, from, end.subtract(from), 1.0F, 0);
		world.playSound(null, from.x, from.y, from.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 3.0F, 0.6F);
		world.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5F, 1.6F);
		for (double t = 4; t < RANGE; t += 6) {
			Vec3d p = from.add(look.multiply(t));
			EnergyFields.shatter(world, p, 3);
			EnergyFields.emit(world, EnergyFields.Energy.SONIC, p, 3, 20, player);
			primeExplosives(world, player, BlockPos.ofFloored(p), 3);
		}
		for (Entity e : Kit.entities(world, from.add(look.multiply(RANGE / 2)), RANGE / 2 + 2, x -> x != player)) {
			Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(from);
			double d = to.length();
			if (d < 0.1 || to.multiply(1 / d).dotProduct(look) < 0.9) {
				continue;
			}
			Kit.push(e, look.multiply(1.4).add(0, 0.3, 0));
			if (e instanceof LivingEntity l) {
				l.damage(world.getDamageSources().sonicBoom(player), 10.0F);
			}
		}
		recoil(player, 0.6);
	}

	private static void primeExplosives(ServerWorld world, PlayerEntity player, BlockPos c, int r) {
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState s = world.getBlockState(p);
					if (Destruction.isProtected(world, p)) {
						continue;
					}
					if (s.isOf(Blocks.TNT)) {
						TntBlock.primeTnt(world, p.toImmutable());
						world.removeBlock(p.toImmutable(), false);
					} else if (s.getBlock() instanceof BombBlock bomb && !bomb.getBombType().needsConfirmation()) {
						BombBlock.arm(world, p.toImmutable(), s, player, 40);
					}
				}
			}
		}
	}
}
