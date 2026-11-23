package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Caller actions that aren't plain scripted events. */
public final class Strikes {
	private Strikes() {
	}

	/** 15 Microstrike Beacon: the strikes follow your crosshair. */
	public static void microstrike(ServerWorld world, PlayerEntity player, Vec3d target) {
		EventEntity.start(world, player.getPos(), "microstrike", player, null);
		Kit.sound(world, player.getPos(), ModSounds.ORBITAL_CHARGE, 1.5F, 1.8F);
	}

	/** 27 Siegebreaker Array: six missiles rise from behind you and converge on the target. */
	public static void siegebreaker(ServerWorld world, PlayerEntity player, Vec3d target) {
		Vfx.grid(world, target.add(0, 0.1, 0), new Vec3d(0, 1, 0), 6.0F, 0xB070FF, 0, 60);
		Vfx.play(world, VfxIds.TARGET_LOCK, target, new Vec3d(0, 1, 0), 1.2F, 0xB070FF);
		Vec3d back = Kit.dir(player.getYaw()).multiply(-6);
		for (int i = 0; i < 6; i++) {
			double side = (i - 2.5) * 2.2;
			Vec3d right = new Vec3d(-back.z, 0, back.x).normalize().multiply(side);
			Vec3d start = player.getPos().add(back).add(right).add(0, 3 + (i % 2), 0);
			Vec3d aim = target.add(world.random.nextGaussian() * 3, 0, world.random.nextGaussian() * 3);
			WmdProjectileEntity p = Kit.projectile(world, ProjectileKind.SIEGE_MISSILE, start, new Vec3d(0, 1.2, 0), player);
			p.setTargetPos(aim);
		}
		Kit.sound(world, player.getPos(), ModSounds.ROCKET_LAUNCH, 3.0F, 0.6F);
	}

	/** 35 Titanfall Warhead: a heavy warhead screams in from orbit along your line of sight. */
	public static void titanfall(ServerWorld world, PlayerEntity player, Vec3d target) {
		Vec3d dir = Kit.dir(player.getYaw());
		Vec3d start = target.subtract(dir.multiply(80)).add(0, 180, 0);
		Vec3d v = target.subtract(start).normalize().multiply(4.0);
		Kit.projectile(world, ProjectileKind.TITAN_MISSILE, start, v, player);
		Vfx.streak(world, start, target, 2.0F, 0xFF9040, 50);
		Vfx.warning(world, target, 0);
		Kit.sound(world, target, ModSounds.JET_FLYBY, 10.0F, 0.4F);
	}
}
