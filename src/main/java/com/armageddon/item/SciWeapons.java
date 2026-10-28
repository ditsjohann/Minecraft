package com.armageddon.item;

import com.armageddon.destruction.Destruction;
import com.armageddon.event.EventEntity;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.Hitscan;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The plasma and sci-fi weapons: the first hand-held guns in the cataclysm tiers. Each one is its own way of taking
 * the world apart: a corkscrew boring bolt, a gravity anchor that crushes, a lobbed sun that melts, an orbital ring
 * cut, and a beam that simply erases.
 */
public final class SciWeapons {
	private SciWeapons() {
	}

	static void charging(World world, LivingEntity user, int used, int full, int color, float pitch) {
		if (!(user instanceof PlayerEntity p) || !(world instanceof ServerWorld server)) {
			return;
		}
		if (used == 1) {
			Vfx.charge(server, p.getId(), GunItem.muzzle(p, 1.0), 2.0F, color, full);
		}
		if (used % 8 == 0 && used <= full) {
			server.playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ORBITAL_CHARGE, SoundCategory.PLAYERS, 0.7F, pitch + used * 0.02F);
		}
	}

	// ====================================================================== HELIX DISRUPTOR

	/** CELESTIAL. Charge, release: two plasma bolts corkscrew around each other, boring a spiral tunnel, then implode. */
	public static class HelixDisruptor extends GunItem {
		public HelixDisruptor(Settings settings) {
			super(settings, Trigger.CHARGE, () -> ModItems.PLASMA_CELL, 4, 50, 50);
		}

		@Override
		protected int chargeTicks() {
			return 30;
		}

		@Override
		protected void onCharging(World world, LivingEntity user, ItemStack stack, int used) {
			charging(world, user, used, chargeTicks(), 0x80F0FF, 0.8F);
		}

		@Override
		protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
			Vec3d dir = player.getRotationVec(1.0F);
			EventEntity.start(world, muzzle(player, 1.2), "helix_bolt", player, nbt -> {
				nbt.putDouble("Dx", dir.x);
				nbt.putDouble("Dy", dir.y);
				nbt.putDouble("Dz", dir.z);
			});
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLASMA_IMPACT, SoundCategory.PLAYERS, 1.6F, 0.6F);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6F, 1.8F);
			recoil(player, 0.9);
		}
	}

	// ====================================================================== GRAVITON LANCE

	/** CITY DESTRUCTION. Plants a gravity anchor where you aim: everything around it is pulled in and crushed. */
	public static class GravitonLance extends GunItem {
		public GravitonLance(Settings settings) {
			super(settings, Trigger.SEMI, () -> ModItems.ANTIMATTER_CELL, 2, 120, 60);
		}

		@Override
		protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
			BlockHitResult hit = TargetingItem.target(world, player, 180);
			Vec3d at = hit.getPos();
			Vfx.beam(world, muzzle(player, 1.0), at, 0.6F, 0xB080FF, 6);
			EventEntity.start(world, at, "graviton", player, null);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BLACK_HOLE_COLLAPSE, SoundCategory.PLAYERS, 1.2F, 1.4F);
			recoil(player, 0.6);
		}
	}

	// ====================================================================== SUNFALL MORTAR

	/** CITY DESTRUCTION. Lobs a small plasma sun in a high arc; where it lands, the ground melts into a lava pool. */
	public static class SunfallMortar extends GunItem {
		public SunfallMortar(Settings settings) {
			super(settings, Trigger.SEMI, () -> ModItems.SOLAR_CELL, 3, 90, 70);
		}

		@Override
		protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
			BlockHitResult hit = TargetingItem.target(world, player, 160);
			Vec3d from = muzzle(player, 1.0), at = hit.getPos();
			int flight = 20 + (int) (from.distanceTo(at) * 0.18);
			EventEntity.start(world, at, "sunfall", player, nbt -> {
				nbt.putDouble("Fx", from.x);
				nbt.putDouble("Fy", from.y);
				nbt.putDouble("Fz", from.z);
				nbt.putInt("Flight", flight);
			});
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.4F, 0.5F);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLASMA_IMPACT, SoundCategory.PLAYERS, 1.0F, 0.6F);
			recoil(player, 0.8);
		}
	}

	// ====================================================================== HALO CUTTER

	/**
	 * WORLD DESTRUCTION. Charge to paint a target: an orbital beam comes down and draws a ring around it, a canyon
	 * thirty blocks deep, and then the whole disc inside drops out of the world.
	 */
	public static class HaloCutter extends GunItem {
		public HaloCutter(Settings settings) {
			super(settings, Trigger.CHARGE, () -> ModItems.ANTIMATTER_CELL, 1, 260, 80);
		}

		@Override
		protected int chargeTicks() {
			return 40;
		}

		@Override
		protected void onCharging(World world, LivingEntity user, ItemStack stack, int used) {
			charging(world, user, used, chargeTicks(), 0xFF6040, 0.5F);
		}

		@Override
		protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
			BlockHitResult hit = TargetingItem.target(world, player, 240);
			Vec3d at = hit.getPos();
			Vfx.beam(world, muzzle(player, 1.0), at, 0.4F, 0xFF4030, 10);
			float yaw = player.getYaw();
			EventEntity.start(world, at, "halo_cut", player, nbt -> nbt.putFloat("Yaw", yaw));
			world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 1.4F);
		}
	}

	// ====================================================================== OBLITERATOR

	/** WORLD DESTRUCTION. Hold: a beam that takes apart everything it touches, block by block, and keeps going. */
	public static class Obliterator extends GunItem {
		private static final double RANGE = 220;

		public Obliterator(Settings settings) {
			super(settings, Trigger.AUTO, () -> ModItems.ANTIMATTER_CELL, 240, 0, 60);
		}

		@Override
		protected int spinUpTicks() {
			return 12;
		}

		@Override
		protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
			Vec3d eye = player.getEyePos();
			Vec3d dir = player.getRotationVec(1.0F);
			BlockHitResult block = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(RANGE)), RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, player));
			boolean hit = block.getType() == HitResult.Type.BLOCK;
			Vec3d end = hit ? block.getPos() : eye.add(dir.multiply(RANGE));
			Vec3d m = muzzle(player, 1.0);
			Vfx.continuousBeam(world, m, end, 1.4F, 0xE070FF);
			if (heldTicks % 4 == 0) {
				world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DEATH_RAY, SoundCategory.PLAYERS, 1.3F, 1.5F);
			}
			for (Entity e : Hitscan.pierce(world, player, eye, end, 1.2)) {
				if (e instanceof LivingEntity living) {
					living.timeUntilRegen = 0;
				}
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, player, player), 10.0F);
			}
			if (hit && Destruction.enabled(world)) {
				erase(world, block.getBlockPos(), 3.4);
			}
			player.addVelocity(-dir.x * 0.02, 0, -dir.z * 0.02);
			player.velocityModified = true;
		}

		/** A ball of the world at the beam's end comes apart into light. */
		private static void erase(ServerWorld world, BlockPos c, double r) {
			int ri = MathHelper.ceil(r);
			int sparks = 0;
			for (BlockPos p : BlockPos.iterate(c.add(-ri, -ri, -ri), c.add(ri, ri, ri))) {
				if (p.getSquaredDistance(c) > r * r) {
					continue;
				}
				BlockState s = world.getBlockState(p);
				if (s.isAir() || s.hasBlockEntity() || s.getHardness(world, p) < 0) {
					continue;
				}
				Destruction.set(world, p.toImmutable(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
				if (sparks++ < 8) {
					world.spawnParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.06);
				}
			}
		}
	}
}
