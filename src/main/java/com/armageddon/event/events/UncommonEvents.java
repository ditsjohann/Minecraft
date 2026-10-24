package com.armageddon.event.events;

import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Yellow tier: chaos without killing. None of these deal damage or permanently alter terrain. */
public final class UncommonEvents {
	private UncommonEvents() {
	}

	/** 01 Party Popper: spiral starbursts, smileys, a heart finale, everyone gently launched. */
	public static class Party extends WorldEvent {
		private static final int[] COLORS = {0xFF3A3A, 0xFFB000, 0x5AFF3A, 0x3AF2FF, 0x3A8CFF, 0xB050FF, 0xFF3AD8};

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 4.0F, 1.0F);
				Vfx.firework(world, c.add(0, 2, 0), 2.0F, 0, COLORS[0]);
				for (LivingEntity e : Kit.living(world, c, 6)) {
					Kit.push(e, new Vec3d(0, 0.8, 0));
					e.fallDistance = 0;
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false));
				}
			}
			if (t > 0 && t <= 24 && t % 3 == 0) {
				double a = t * 0.5;
				Vec3d p = c.add(Math.cos(a) * 4, 3 + t * 0.25, Math.sin(a) * 4);
				Vfx.firework(world, p, 1.0F, t % 2 == 0 ? 0 : 1, COLORS[(t / 3) % COLORS.length]);
				Kit.sound(world, p, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0F, 0.8F + t * 0.02F);
			}
			if (t == 18) {
				Vfx.firework(world, c.add(0, 8, 0), 2.0F, 4, 0xFFE040);
			}
			if (t == 32) {
				Vfx.firework(world, c.add(0, 11, 0), 3.0F, 3, 0xFF3A6A);
				Kit.sound(world, c, SoundEvents.ENTITY_PLAYER_LEVELUP, 3.0F, 1.2F);
			}
			return t > 40;
		}
	}

	/** 02 Bounce Bomb: a giant translucent bubble that pings everything skyward, repeatedly. */
	public static class Bounce extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			float r = Math.min(8.0F, t * 0.6F);
			if (t == 0) {
				Vfx.bubble(world, c, 8.0F, 0xB070FF, 110);
				Kit.sound(world, c, SoundEvents.BLOCK_SLIME_BLOCK_PLACE, 3.0F, 0.6F);
			}
			// Last push early enough that everything has landed before the fall-distance reset stops.
			if (t % 6 == 0 && t <= 80) {
				for (Entity e : Kit.entities(world, c, r, x -> !Kit.creativeOrSpectator(x) && !(x instanceof FallingBlockEntity))) {
					if (e instanceof ItemEntity) {
						Kit.push(e, new Vec3d(world.random.nextGaussian() * 0.3, 0.5, world.random.nextGaussian() * 0.3));
					} else if (e.isOnGround() || e.getVelocity().y < 0.1) {
						Kit.push(e, new Vec3d(0, 1.1 + world.random.nextDouble() * 0.3, 0));
						e.fallDistance = 0;
						Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SLIME_JUMP, 1.0F, 0.6F + world.random.nextFloat() * 0.8F);
					}
				}
			}
			for (LivingEntity e : Kit.living(world, c, 10)) {
				e.fallDistance = 0;
			}
			return t > 110;
		}
	}

	/** 03 Loot Tornado: items and stray projectiles orbit in a vortex, then land in a tidy pile. */
	public static class LootTornado extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.spiral(world, c, 6.0F, 6.0F, 0x40E8FF, 200);
				Kit.sound(world, c, SoundEvents.ITEM_ELYTRA_FLYING, 2.0F, 1.6F);
			}
			Vec3d eye = c.add(0, 2.5, 0);
			for (Entity e : Kit.entities(world, c, 18, x -> x instanceof ItemEntity || x instanceof ExperienceOrbEntity || x instanceof ProjectileEntity)) {
				if (e instanceof ProjectileEntity p && p.getOwner() == owner()) {
					continue;
				}
				Vec3d to = eye.subtract(e.getPos());
				double d = Math.max(0.5, to.length());
				Vec3d tangent = new Vec3d(-to.z, 0, to.x).normalize().multiply(0.45);
				double spring = (d - 3.0) * 0.05;
				Vec3d v = to.multiply(spring / d).add(tangent).add(0, (eye.y + Math.sin(t * 0.2 + e.getId()) * 1.5 - e.getY()) * 0.05, 0);
				if (t > 190) {
					v = new Vec3d(0, -0.1, 0);
					e.setPosition(c.x + world.random.nextGaussian() * 0.3, c.y + 0.2, c.z + world.random.nextGaussian() * 0.3);
				}
				Kit.velocity(e, v);
				e.setNoGravity(t < 190);
				if (e instanceof ItemEntity item) {
					item.setPickupDelay(t < 190 ? 10 : 0);
				}
			}
			if (t == 192) {
				Kit.sound(world, c, SoundEvents.ENTITY_ITEM_PICKUP, 2.0F, 0.5F);
			}
			return t > 200;
		}

		@Override
		public void finish() {
			for (Entity e : Kit.entities(world, center(), 20, x -> x instanceof ItemEntity || x instanceof ExperienceOrbEntity || x instanceof ProjectileEntity)) {
				e.setNoGravity(false);
			}
		}
	}

	/** 04 Moonwalk Mine: a low-gravity zone where everything floats and drifts. */
	public static class Moonwalk extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0F, 1.8F);
			}
			if (t % 20 == 0) {
				Vfx.ring(world, c.add(0, 0.2, 0), 10.0F, 0.6F, 0xFF70D0, true, 30);
			}
			for (LivingEntity e : Kit.living(world, c, 10)) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, false, false));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 10, 4, false, false));
				if (t % 15 == 0 && !e.isOnGround()) {
					Kit.push(e, new Vec3d(world.random.nextGaussian() * 0.12, 0.06, world.random.nextGaussian() * 0.12));
				}
			}
			return t > 240;
		}
	}

	/** 05 Snow Globe Grenade: a decorative blizzard dome, frosty outlines, temporary snow. */
	public static class SnowGlobe extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.bubble(world, c, 10.0F, 0xE8F8FF, 200);
				Vfx.spiral(world, c, 9.0F, 8.0F, 0xFFFFFF, 200);
				Kit.sound(world, c, SoundEvents.BLOCK_POWDER_SNOW_PLACE, 3.0F, 0.6F);
				BlockPos.Mutable p = new BlockPos.Mutable();
				for (int dx = -9; dx <= 9; dx++) {
					for (int dz = -9; dz <= 9; dz++) {
						if (dx * dx + dz * dz > 81 || world.random.nextFloat() < 0.35F) {
							continue;
						}
						BlockPos top = Kit.surface(world, MathHelper.floor(c.x) + dx, MathHelper.floor(c.z) + dz);
						p.set(top.up());
						if (world.getBlockState(p).isAir() && Blocks.SNOW.getDefaultState().canPlaceAt(world, p)) {
							TemporaryBlocks.place(world, p.toImmutable(), Blocks.SNOW.getDefaultState(), 900 + world.random.nextInt(300));
						}
					}
				}
			}
			for (LivingEntity e : Kit.living(world, c, 10)) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20, 0, false, false));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 0, false, false));
				// Vanilla thaws 2 per tick outside powder snow, so +4 nets +2.
				e.setFrozenTicks(Math.min(e.getFrozenTicks() + 4, 120));
			}
			return t > 200;
		}
	}

	/** 06 Slimequake: a green shockwave that turns the ground into temporary bouncy slime. */
	public static class Slimequake extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.ring(world, c, 10.0F, 1.2F, 0x5AFF3A, false, 25);
				Kit.sound(world, c, SoundEvents.ENTITY_SLIME_SQUISH, 4.0F, 0.5F);
			}
			if (t < 20) {
				double r = t * 0.5;
				int n = (int) (r * 6) + 1;
				for (int i = 0; i < n; i++) {
					double a = Math.PI * 2 * i / n;
					BlockPos top = Kit.surface(world, MathHelper.floor(c.x + Math.cos(a) * r), MathHelper.floor(c.z + Math.sin(a) * r));
					BlockState s = world.getBlockState(top);
					if (s.isOpaqueFullCube(world, top) && !s.isOf(Blocks.SLIME_BLOCK)) {
						TemporaryBlocks.place(world, top, Blocks.SLIME_BLOCK.getDefaultState(), 300 + world.random.nextInt(60));
					}
				}
				for (LivingEntity e : Kit.living(world, c, r + 1)) {
					if (Math.abs(e.getPos().distanceTo(c) - r) < 1.2) {
						Kit.push(e, new Vec3d(world.random.nextGaussian() * 0.2, 0.6, world.random.nextGaussian() * 0.2));
					}
				}
			}
			return t > 20;
		}
	}

	/** 07 Hologram Bomb: fake blocks, fake explosions, fake clones. All harmless light. */
	public static class Hologram extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				Vfx.hologram(world, c, 9.0F, 160);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_POWER_SELECT, 2.0F, 2.0F);
			}
			if (t > 10 && t < 150 && t % 25 == 0) {
				Vec3d p = c.add(world.random.nextGaussian() * 5, 1 + world.random.nextDouble() * 3, world.random.nextGaussian() * 5);
				Vfx.layeredBlast(world, p, 1.5F, 0x40FFFF);
				Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 1.5F, 1.8F);
			}
			return t > 160;
		}
	}

	/** 08 Magnet Mayhem: metal items and metal blocks orbit in a clanking sphere, then are flung out. */
	public static class Magnet extends WorldEvent {
		private final List<FallingBlockEntity> lifted = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			Vec3d core = c.add(0, 3.5, 0);
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0F, 0.8F);
				BlockPos.Mutable p = new BlockPos.Mutable();
				BlockPos b = BlockPos.ofFloored(c);
				for (int i = 0; i < 600 && lifted.size() < 40 && com.armageddon.destruction.Destruction.enabled(world); i++) {
					p.set(b.getX() + world.random.nextInt(29) - 14, b.getY() + world.random.nextInt(11) - 5, b.getZ() + world.random.nextInt(29) - 14);
					BlockState s = world.getBlockState(p);
					if (com.armageddon.destruction.EnergyFields.isMetal(s) && !s.hasBlockEntity() && !com.armageddon.destruction.Destruction.isProtected(world, p)) {
						com.armageddon.destruction.Destruction.record(world, p, s);
						FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
						fb.setNoGravity(true);
						lifted.add(fb);
					}
				}
			}
			if (t % 4 == 0 && t < 120) {
				Vfx.arc(world, core, core.add(world.random.nextGaussian() * 4, world.random.nextGaussian() * 2, world.random.nextGaussian() * 4), 0.06F, 0xFFD040);
			}
			List<Entity> pulled = new ArrayList<>(lifted);
			pulled.addAll(Kit.entities(world, c, 14, x -> x instanceof ItemEntity item && isMetalItem(item.getStack())));
			for (Entity e : pulled) {
				if (!e.isAlive()) {
					continue;
				}
				Vec3d to = core.subtract(e.getPos());
				double d = Math.max(0.3, to.length());
				if (t < 120) {
					Vec3d orbit = new Vec3d(-to.z, to.y * 0.2, to.x).normalize().multiply(0.35);
					Kit.velocity(e, to.multiply((d - 2.0) * 0.08 / d).add(orbit));
					e.setNoGravity(true);
				} else if (t == 120) {
					Kit.velocity(e, to.multiply(-0.9 / d).add(0, 0.4, 0));
					e.setNoGravity(false);
				}
			}
			if (t == 120) {
				Vfx.ring(world, core, 10.0F, 0.8F, 0xFFD040, false, 20);
				Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_LAND, 2.0F, 0.6F);
			}
			return t > 125;
		}

		private static boolean isMetalItem(ItemStack s) {
			String id = s.getItem().toString();
			return s.isOf(Items.IRON_INGOT) || s.isOf(Items.GOLD_INGOT) || s.isOf(Items.COPPER_INGOT) || s.isOf(Items.IRON_NUGGET)
					|| s.isOf(Items.GOLD_NUGGET) || s.isOf(Items.NETHERITE_INGOT) || id.contains("iron") || id.contains("gold")
					|| id.contains("copper") || id.contains("netherite") || id.contains("chain") || id.contains("titanium");
		}

		@Override
		public void finish() {
			Kit.drop(lifted, Vec3d.ZERO);
		}
	}

	/** 10 Rainbow Paint Bomb: a geyser of colour that splatters surfaces and makes creatures glow. */
	public static class Paint extends WorldEvent {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			int color = data.getInt("Power");
			if (t == 0) {
				Vfx.paint(world, c, 8.0F, color, 600);
				Vfx.spiral(world, c, 2.0F, 7.0F, color == 0 ? 0xFF3AD8 : color, 30);
				Kit.sound(world, c, SoundEvents.ENTITY_SLIME_SQUISH, 3.0F, 1.4F);
				Kit.sound(world, c, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 3.0F, 1.0F);
				for (LivingEntity e : Kit.living(world, c, 8)) {
					if (color == -1) {
						e.removeStatusEffect(StatusEffects.GLOWING);
					} else {
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0, false, false));
					}
				}
			}
			return t > 2;
		}
	}
}
