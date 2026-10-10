package com.armageddon.destruction;

import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.network.ModNetworking;
import com.armageddon.registry.ModDamageTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Short-lived energy residues left by weapons. When a new detonation lands inside a field of a
 * reacting energy type, a reaction fires:
 * <ul>
 *   <li>Plasma/solar + cryo → explosive thermal shock and steam</li>
 *   <li>Electric + metal debris → chain lightning</li>
 *   <li>Gravity field → bends passing projectiles</li>
 *   <li>Sonic + brittle blocks → shattering</li>
 *   <li>Solar + ice/snow → rapid melting and steam</li>
 *   <li>Antimatter + anything → unstable interference</li>
 * </ul>
 */
public final class EnergyFields {
	public enum Energy { PLASMA, CRYO, ELECTRIC, GRAVITY, SONIC, SOLAR, ANTIMATTER, VOID }

	public record Field(ServerWorld world, Energy energy, Vec3d center, double radius, long expires) {
		boolean contains(Vec3d p) {
			return p.squaredDistanceTo(center) <= radius * radius;
		}
	}

	private static final List<Field> FIELDS = new ArrayList<>();

	public static void init() {
		ServerTickEvents.END_WORLD_TICK.register(world -> FIELDS.removeIf(f -> f.world == world && world.getTime() > f.expires));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> FIELDS.clear());
	}

	/** Leaves a residue and immediately reacts with any overlapping residues. */
	public static void emit(ServerWorld world, Energy energy, Vec3d center, double radius, int ticks, @Nullable Entity cause) {
		List<Field> overlapping = new ArrayList<>();
		for (Field f : FIELDS) {
			if (f.world == world && f.energy != energy && f.center.distanceTo(center) < f.radius + radius) {
				overlapping.add(f);
			}
		}
		for (Field f : overlapping) {
			react(world, energy, f, center, radius, cause);
		}
		FIELDS.add(new Field(world, energy, center, radius, world.getTime() + ticks));
		if (FIELDS.size() > 256) {
			FIELDS.remove(0);
		}
	}

	@Nullable
	public static Field strongest(ServerWorld world, Energy energy, Vec3d at) {
		Field best = null;
		for (Field f : FIELDS) {
			if (f.world == world && f.energy == energy && f.contains(at) && (best == null || f.radius > best.radius)) {
				best = f;
			}
		}
		return best;
	}

	private static boolean pair(Energy a, Energy b, Energy x, Energy y) {
		return (a == x && b == y) || (a == y && b == x);
	}

	private static void react(ServerWorld world, Energy incoming, Field f, Vec3d at, double radius, @Nullable Entity cause) {
		Energy other = f.energy;
		Vec3d mid = at.add(f.center).multiply(0.5);
		if (pair(incoming, other, Energy.PLASMA, Energy.CRYO) || pair(incoming, other, Energy.SOLAR, Energy.CRYO)) {
			// Thermal shock: violent steam explosion + shatter all ice.
			ModNetworking.sendFx(world, mid, 256, new FxPayload(FxType.STEAM, mid, (float) Math.max(4, radius), 0xE8F4FF));
			Destruction.explode(world, cause, mid.x, mid.y, mid.z, 4.5F, false);
			melt(world, mid, (int) Math.min(10, radius + 2), true);
		} else if (pair(incoming, other, Energy.SOLAR, Energy.ELECTRIC) || pair(incoming, other, Energy.ELECTRIC, Energy.SONIC)
				|| (incoming == Energy.ELECTRIC || other == Energy.ELECTRIC)) {
			chainLightning(world, mid, Math.max(8, radius + 4), cause);
		} else if (pair(incoming, other, Energy.SONIC, Energy.CRYO) || incoming == Energy.SONIC || other == Energy.SONIC) {
			shatter(world, mid, (int) Math.min(12, radius + 3));
		} else if (incoming == Energy.ANTIMATTER || other == Energy.ANTIMATTER) {
			ModNetworking.sendFx(world, mid, 256, new FxPayload(FxType.GLITCH, mid, (float) Math.max(4, radius), 0xE080FF));
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(mid, mid).expand(radius), LivingEntity::isAlive)) {
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, cause), 6.0F);
			}
		} else if (incoming == Energy.SOLAR || other == Energy.SOLAR) {
			melt(world, mid, (int) Math.min(12, radius + 2), false);
		}
	}

	/** Electric discharge jumps between metal blocks and creatures. */
	public static void chainLightning(ServerWorld world, Vec3d center, double radius, @Nullable Entity cause) {
		List<Vec3d> nodes = new ArrayList<>();
		BlockPos c = BlockPos.ofFloored(center);
		int r = (int) Math.min(10, radius);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int i = 0; i < 400 && nodes.size() < 12; i++) {
			p.set(c.getX() + world.random.nextInt(r * 2 + 1) - r, c.getY() + world.random.nextInt(r + 1) - r / 2, c.getZ() + world.random.nextInt(r * 2 + 1) - r);
			if (isMetal(world.getBlockState(p))) {
				nodes.add(Vec3d.ofCenter(p));
			}
		}
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(radius), LivingEntity::isAlive)) {
			nodes.add(e.getPos().add(0, e.getHeight() / 2, 0));
			e.timeUntilRegen = 0;
			e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION, null, cause), 6.0F);
		}
		Vec3d prev = center;
		for (Vec3d n : nodes) {
			ModNetworking.sendFx(world, prev, 256, new FxPayload(FxType.ARC, prev, n, 0.12F, 0x9FD8FF, world.random.nextInt()));
			prev = n;
		}
	}

	public static boolean isMetal(BlockState s) {
		return s.isOf(Blocks.IRON_BLOCK) || s.isOf(Blocks.IRON_BARS) || s.isOf(Blocks.GOLD_BLOCK) || s.isOf(Blocks.COPPER_BLOCK)
				|| s.isOf(Blocks.LIGHTNING_ROD) || s.isOf(Blocks.CHAIN) || s.isOf(Blocks.ANVIL) || s.isOf(Blocks.CAULDRON)
				|| s.isOf(Blocks.RAIL) || s.isOf(Blocks.POWERED_RAIL) || s.isOf(Blocks.IRON_DOOR) || s.isOf(Blocks.IRON_TRAPDOOR)
				|| s.isOf(Blocks.HOPPER) || s.isOf(Blocks.NETHERITE_BLOCK) || s.isIn(BlockTags.RAILS);
	}

	public static boolean isBrittle(BlockState s) {
		return s.isIn(BlockTags.IMPERMEABLE) || s.isIn(BlockTags.ICE) || s.isOf(Blocks.GLOWSTONE) || s.isOf(Blocks.SEA_LANTERN)
				|| s.isOf(Blocks.GLASS_PANE) || s.isIn(BlockTags.LEAVES) || s.isOf(Blocks.AMETHYST_BLOCK) || s.isOf(Blocks.BUDDING_AMETHYST)
				|| s.isOf(Blocks.AMETHYST_CLUSTER) || s.isOf(Blocks.REDSTONE_LAMP) || s.isIn(BlockTags.TERRACOTTA)
				|| s.getBlock() instanceof net.minecraft.block.PaneBlock || s.getBlock() instanceof net.minecraft.block.StainedGlassPaneBlock;
	}

	/** Sonic shattering of brittle blocks in a sphere. */
	public static void shatter(ServerWorld world, Vec3d center, int r) {
		BlockPos c = BlockPos.ofFloored(center);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState s = world.getBlockState(p);
					if (!s.isAir() && isBrittle(s) && Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL)) {
						world.syncWorldEvent(2001, p, Block.getRawIdFromState(s));
					}
				}
			}
		}
	}

	/** Solar melting: ice → water, snow → air, with steam. {@code shock} turns ice to air instead. */
	public static void melt(ServerWorld world, Vec3d center, int r, boolean shock) {
		BlockPos c = BlockPos.ofFloored(center);
		BlockPos.Mutable p = new BlockPos.Mutable();
		boolean any = false;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState s = world.getBlockState(p);
					if (s.isIn(BlockTags.ICE)) {
						any |= Destruction.set(world, p, shock ? Blocks.AIR.getDefaultState() : Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
					} else if (s.isIn(BlockTags.SNOW) || s.isOf(Blocks.POWDER_SNOW)) {
						any |= Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
					}
				}
			}
		}
		if (any) {
			ModNetworking.sendFx(world, center, 256, new FxPayload(FxType.STEAM, center, r, 0xF0F8FF));
		}
	}

	/** Gravity fields curve projectile paths. Called from projectile ticks. */
	public static Vec3d bend(ServerWorld world, Vec3d pos, Vec3d velocity) {
		Field g = strongest(world, Energy.GRAVITY, pos);
		if (g == null) {
			return velocity;
		}
		Vec3d to = g.center.subtract(pos);
		double d = Math.max(1.0, to.length());
		return velocity.add(to.multiply(0.08 / d));
	}

	private EnergyFields() {
	}
}
