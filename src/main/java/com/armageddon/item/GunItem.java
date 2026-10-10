package com.armageddon.item;

import com.armageddon.registry.ModComponents;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for every firearm. Handles magazines, reloading from inventory ammo,
 * three trigger styles and the ammo bar.
 * <ul>
 *   <li>SEMI — one shot per click (launchers, cannons)</li>
 *   <li>AUTO — fires while the use key is held; driven by {@code TriggerPayload} so the
 *       player keeps full movement speed</li>
 *   <li>CHARGE — hold to charge, release to fire (railgun)</li>
 * </ul>
 */
public abstract class GunItem extends Item {
	public enum Trigger { SEMI, AUTO, CHARGE }

	protected final Trigger trigger;
	protected final Supplier<Item> ammo;
	protected final int magazine;
	protected final int cooldown;
	protected final int reloadTicks;

	protected GunItem(Settings settings, Trigger trigger, Supplier<Item> ammo, int magazine, int cooldown, int reloadTicks) {
		super(settings.maxCount(1).component(ModComponents.AMMO, magazine));
		this.trigger = trigger;
		this.ammo = ammo;
		this.magazine = magazine;
		this.cooldown = cooldown;
		this.reloadTicks = reloadTicks;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public int getMagazineSize() {
		return magazine;
	}

	public static int getAmmo(ItemStack stack) {
		return stack.getOrDefault(ModComponents.AMMO, 0);
	}

	// ------------------------------------------------------------------ firing hooks

	/** Fires one shot. Only called server-side after a round was consumed. */
	protected abstract void fire(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks);

	/** AUTO only: ticks of spin-up before the first round. */
	protected int spinUpTicks() {
		return 0;
	}

	/** AUTO only: ticks between rounds. */
	protected int fireInterval() {
		return 1;
	}

	/** AUTO: called every tick the trigger is held, before spin-up completes as well. */
	protected void onTriggerHeld(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
	}

	/** CHARGE only: ticks to full charge. */
	protected int chargeTicks() {
		return 20;
	}

	// ------------------------------------------------------------------ vanilla plumbing

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		switch (trigger) {
			case AUTO -> {
				// Handled by the trigger packet; do nothing here so movement isn't slowed.
				return TypedActionResult.fail(stack);
			}
			case CHARGE -> {
				if (getAmmo(stack) <= 0 && !user.getAbilities().creativeMode && !hasAmmoInInventory(user)) {
					dryFire(world, user);
					return TypedActionResult.fail(stack);
				}
				user.setCurrentHand(hand);
				return TypedActionResult.consume(stack);
			}
			default -> {
				if (!world.isClient && world instanceof ServerWorld server) {
					if (consumeRound(user, stack)) {
						fire(server, user, stack, 0);
						user.getItemCooldownManager().set(this, cooldown);
					}
				}
				return TypedActionResult.consume(stack);
			}
		}
	}

	/** Server tick while the AUTO trigger is held (main hand only). */
	public void triggerTick(ServerWorld world, PlayerEntity player, ItemStack stack, int heldTicks) {
		if (player.getItemCooldownManager().isCoolingDown(this)) {
			return;
		}
		onTriggerHeld(world, player, stack, heldTicks);
		int spin = spinUpTicks();
		if (heldTicks >= spin && (heldTicks - spin) % fireInterval() == 0) {
			if (consumeRound(player, stack)) {
				fire(world, player, stack, heldTicks);
			}
		}
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (trigger == Trigger.CHARGE) {
			int used = getMaxUseTime(stack, user) - remainingUseTicks;
			onCharging(world, user, stack, used);
		}
	}

	protected void onCharging(World world, LivingEntity user, ItemStack stack, int usedTicks) {
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (trigger != Trigger.CHARGE || !(user instanceof PlayerEntity player) || !(world instanceof ServerWorld server)) {
			return;
		}
		int used = getMaxUseTime(stack, user) - remainingUseTicks;
		if (used >= chargeTicks() && consumeRound(player, stack)) {
			fire(server, player, stack, used);
			player.getItemCooldownManager().set(this, cooldown);
		}
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return trigger == Trigger.CHARGE ? 72000 : 0;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return trigger == Trigger.CHARGE ? UseAction.BOW : UseAction.NONE;
	}

	// ------------------------------------------------------------------ ammo

	/** Takes one round from the magazine; auto-reloads from inventory when empty. */
	protected boolean consumeRound(PlayerEntity player, ItemStack stack) {
		if (player.getAbilities().creativeMode) {
			return true;
		}
		int loaded = getAmmo(stack);
		if (loaded > 0) {
			stack.set(ModComponents.AMMO, loaded - 1);
			return true;
		}
		if (reload(player, stack)) {
			player.getItemCooldownManager().set(this, reloadTicks);
		} else {
			dryFire(player.getWorld(), player);
			player.getItemCooldownManager().set(this, 10);
		}
		return false;
	}

	private boolean reload(PlayerEntity player, ItemStack stack) {
		PlayerInventory inv = player.getInventory();
		Item ammoItem = ammo.get();
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (s.isOf(ammoItem)) {
				s.decrement(1);
				stack.set(ModComponents.AMMO, magazine);
				player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RELOAD, SoundCategory.PLAYERS, 1.0F, 1.0F);
				return true;
			}
		}
		return false;
	}

	private boolean hasAmmoInInventory(PlayerEntity player) {
		PlayerInventory inv = player.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).isOf(ammo.get())) {
				return true;
			}
		}
		return false;
	}

	protected void dryFire(World world, PlayerEntity player) {
		world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DRY_FIRE, SoundCategory.PLAYERS, 0.8F, 1.2F);
	}

	// ------------------------------------------------------------------ helpers

	/** World-space muzzle position, offset to the right hand side of the view. */
	public static Vec3d muzzle(PlayerEntity player, double forward) {
		Vec3d look = player.getRotationVec(1.0F);
		float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double side = player.getMainArm() == Arm.RIGHT ? 1.0 : -1.0;
		Vec3d right = new Vec3d(-MathHelper.cos(yaw), 0, -MathHelper.sin(yaw)).multiply(side);
		return player.getEyePos().add(look.multiply(forward)).add(right.multiply(0.28)).add(0, -0.18, 0);
	}

	protected static void muzzleFlash(ServerWorld world, PlayerEntity player, double forward) {
		Vec3d m = muzzle(player, forward);
		world.spawnParticles(ModParticles.MUZZLE_FLASH, m.x, m.y, m.z, 1, 0, 0, 0, 0);
	}

	protected static void recoil(PlayerEntity player, double strength) {
		Vec3d look = player.getRotationVec(1.0F);
		player.addVelocity(-look.x * strength, -look.y * strength * 0.5, -look.z * strength);
		player.velocityModified = true;
	}

	// ------------------------------------------------------------------ UI

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getAmmo(stack) < magazine;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		return Math.round(13.0F * getAmmo(stack) / magazine);
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		float f = getAmmo(stack) / (float) magazine;
		return MathHelper.hsvToRgb(0.5F * f, 0.8F, 1.0F);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		Tooltips.describe(this, tooltip);
		tooltip.add(Text.translatable("tooltip.armageddon.ammo", getAmmo(stack), magazine, ammo.get().getName())
				.formatted(Formatting.AQUA));
		tooltip.add(Text.translatable("tooltip.armageddon.trigger." + trigger.name().toLowerCase()).formatted(Formatting.DARK_AQUA));
	}

	@Override
	public Text getName(ItemStack stack) {
		return Tooltips.name(this, super.getName(stack));
	}
}
