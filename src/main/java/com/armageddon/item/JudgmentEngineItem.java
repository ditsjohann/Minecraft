package com.armageddon.item;

import com.armageddon.entity.ProjectileKind;
import com.armageddon.entity.WmdProjectileEntity;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModComponents;
import com.armageddon.registry.ModItems;
import com.armageddon.registry.ModSounds;
import com.armageddon.vfx.VfxIds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/**
 * 38 Judgment Engine: a golden rotary plasma cannon. The longer you hold it, the hotter and
 * faster it gets — and the harder each bolt hits. At 100% it vents and locks for five seconds.
 */
public class JudgmentEngineItem extends GunItem {
	public JudgmentEngineItem(Settings settings) {
		super(settings.component(ModComponents.HEAT, 0), Trigger.AUTO, () -> ModItems.SOLAR_CELL, 60, 20, 60);
	}

	public static int heat(ItemStack stack) {
		return stack.getOrDefault(ModComponents.HEAT, 0);
	}

	@Override
	protected int spinUpTicks() {
		return 12;
	}

	@Override
	public void triggerTick(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		if (player.getItemCooldownManager().isCoolingDown(this)) {
			return;
		}
		if (heldTicks == 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGUN_SPIN, SoundCategory.PLAYERS, 1.0F, 0.7F);
		}
		int h = heat(stack);
		int interval = Math.max(1, 6 - h / 20);
		int spin = spinUpTicks();
		if (heldTicks >= spin && (heldTicks - spin) % interval == 0 && consumeRound(player, stack)) {
			fire(world, player, stack, heldTicks);
			h = Math.min(100, h + 3);
			stack.set(ModComponents.HEAT, h);
			if (h >= 100) {
				stack.set(ModComponents.HEAT, 0);
				player.getItemCooldownManager().set(this, 100);
				Vfx.steam(world, muzzle(player, 0.8), 2.0F);
				Vfx.play(world, VfxIds.JUDGMENT_VENT, muzzle(player, 0.8), 1.0F);
				world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 2.0F, 0.6F);
				player.setOnFireFor(2);
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (!world.isClient && world.getTime() % 4 == 0) {
			int h = heat(stack);
			if (h > 0) {
				stack.set(ModComponents.HEAT, h - 1);
			}
		}
	}

	@Override
	protected void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		int h = heat(stack);
		WmdProjectileEntity p = WmdProjectileEntity.fire(world, player, ProjectileKind.JUDGMENT_BOLT, 3.2F, 1.5F + h / 50.0F);
		p.setPower(h);
		muzzleFlash(world, player, 1.0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLASMA_FIRE, SoundCategory.PLAYERS, 1.2F, 0.7F + h * 0.006F);
		recoil(player, 0.03 + h * 0.0006);
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return heat(stack) > 0 || super.isItemBarVisible(stack);
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		int h = heat(stack);
		return h > 0 ? Math.round(13.0F * h / 100.0F) : super.getItemBarStep(stack);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		int h = heat(stack);
		return h > 0 ? MathHelper.hsvToRgb(0.15F - 0.15F * h / 100.0F, 1.0F, 1.0F) : super.getItemBarColor(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Tooltips.stat("heat", heat(stack), Formatting.GOLD));
	}
}
