package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.network.Vfx;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

/**
 * Semi-automatic projectile launcher: nuke launcher, homing rocket pod, MIRV launcher,
 * cryo cannon, singularity projector.
 */
public class LauncherItem extends GunItem {
	private final ProjectileKind kind;
	private final float speed;
	private final float divergence;
	private final double recoil;
	private final boolean homing;
	private final Supplier<SoundEvent> sound;

	public LauncherItem(Settings settings, Supplier<Item> ammo, int magazine, int cooldown, int reload,
			ProjectileKind kind, float speed, float divergence, double recoil, boolean homing, Supplier<SoundEvent> sound) {
		super(settings, Trigger.SEMI, ammo, magazine, cooldown, reload);
		this.kind = kind;
		this.speed = speed;
		this.divergence = divergence;
		this.recoil = recoil;
		this.homing = homing;
		this.sound = sound;
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		WmdProjectileEntity p = WmdProjectileEntity.fire(world, player, kind, speed, divergence);
		Vec3d m = muzzle(player, 1.0);
		p.setPosition(m.x, m.y, m.z);
		if (homing) {
			p.setHomingTarget(WmdProjectileEntity.findLockTarget(player, 96, 14));
		}
		if (kind == ProjectileKind.MIRV) {
			p.setVelocity(p.getVelocity().add(0, 0.35, 0));
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(), sound.get(), SoundCategory.PLAYERS, 2.0F, 0.9F + world.random.nextFloat() * 0.2F);
		String muzzleFx = switch (kind) {
			case ROCKET, MIRV, MINI_NUKE -> VfxIds.ROCKET_BACKBLAST;
			case CRYO -> VfxIds.CRYO_MUZZLE;
			case BFG -> VfxIds.BFG_MUZZLE;
			default -> null;
		};
		if (muzzleFx != null) {
			Vfx.play(world, muzzleFx, m, player.getRotationVec(1.0F), 1.0F, 0);
		} else {
			muzzleFlash(world, player, 1.0);
		}
		if (recoil > 0) {
			recoil(player, recoil);
		}
	}
}
