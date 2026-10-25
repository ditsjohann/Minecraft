package com.armageddon.event;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModGameRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Toolbox shared by all event scripts. */
public final class Kit {
	public static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

	public static List<LivingEntity> living(ServerWorld w, Vec3d c, double r) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(r), LivingEntity::isAlive)) {
			if (e.getPos().squaredDistanceTo(c) <= r * r && !(e instanceof PlayerEntity p && p.isSpectator())) {
				out.add(e);
			}
		}
		return out;
	}

	public static List<Entity> entities(ServerWorld w, Vec3d c, double r, Predicate<Entity> filter) {
		List<Entity> out = new ArrayList<>();
		for (Entity e : w.getOtherEntities(null, new Box(c, c).expand(r))) {
			if (e.getPos().squaredDistanceTo(c) <= r * r && filter.test(e)) {
				out.add(e);
			}
		}
		return out;
	}

	public static void push(Entity e, Vec3d v) {
		e.addVelocity(v);
		e.velocityModified = true;
	}

	public static void velocity(Entity e, Vec3d v) {
		e.setVelocity(v);
		e.velocityModified = true;
	}

	public static boolean creativeOrSpectator(Entity e) {
		return e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator());
	}

	/** Top solid block position at x,z (the block itself, not the air above). */
	public static BlockPos surface(ServerWorld w, int x, int z) {
		return new BlockPos(x, w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
	}

	public static Vec3d ground(ServerWorld w, double x, double z) {
		int ix = MathHelper.floor(x), iz = MathHelper.floor(z);
		return new Vec3d(x, w.getTopY(Heightmap.Type.MOTION_BLOCKING, ix, iz), z);
	}

	public static void sound(ServerWorld w, Vec3d c, SoundEvent s, float volume, float pitch) {
		w.playSound(null, c.x, c.y, c.z, s, SoundCategory.BLOCKS, volume, pitch);
	}

	public static void damage(ServerWorld w, Vec3d c, double r, float amount, RegistryKey<DamageType> type, @Nullable Entity source, @Nullable Entity cause) {
		for (LivingEntity e : living(w, c, r)) {
			double f = 1.0 - e.getPos().distanceTo(c) / r;
			e.timeUntilRegen = 0;
			e.damage(ModDamageTypes.source(w, type, source, cause), (float) Math.max(1.0, amount * f));
		}
	}

	/** How many more debris entities may be spawned near a point. */
	public static int debrisBudget(ServerWorld w, Vec3d c) {
		int max = w.getGameRules().getInt(ModGameRules.MAX_DEBRIS);
		int existing = w.getEntitiesByType(EntityType.FALLING_BLOCK, new Box(c, c).expand(256), e -> true).size();
		return Math.max(0, max - existing);
	}

	public static boolean liftable(ServerWorld w, BlockPos p, BlockState s, ProtectedRegions regions) {
		float h = s.getHardness(w, p);
		return !s.isAir() && h >= 0 && h < 25 && !s.hasBlockEntity() && s.getFluidState().isEmpty()
				&& (regions.isEmpty() || !regions.isProtected(p));
	}

	/**
	 * Tears up to {@code max} blocks out of the terrain (surface first) and turns them into
	 * weightless flying blocks the caller can then steer.
	 */
	public static List<FallingBlockEntity> lift(ServerWorld w, Vec3d c, double r, int max, boolean surfaceOnly, boolean vanishOnLanding) {
		List<FallingBlockEntity> out = new ArrayList<>();
		if (!Destruction.enabled(w)) {
			return out;
		}
		max = Math.min(max, debrisBudget(w, c));
		ProtectedRegions regions = ProtectedRegions.get(w);
		BlockPos.Mutable p = new BlockPos.Mutable();
		int tries = max * 6;
		while (out.size() < max && tries-- > 0) {
			double a = w.random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(w.random.nextDouble()) * r;
			int x = MathHelper.floor(c.x + Math.cos(a) * d);
			int z = MathHelper.floor(c.z + Math.sin(a) * d);
			if (!w.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			if (surfaceOnly) {
				p.set(surface(w, x, z));
			} else {
				p.set(x, MathHelper.floor(c.y + (w.random.nextDouble() * 2 - 1) * r * 0.6), z);
			}
			BlockState s = w.getBlockState(p);
			if (!liftable(w, p, s, regions)) {
				continue;
			}
			Destruction.record(w, p, s);
			FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, p.toImmutable(), s);
			fb.setNoGravity(true);
			if (vanishOnLanding) {
				fb.setDestroyedOnLanding();
			}
			out.add(fb);
		}
		return out;
	}

	/** Releases lifted blocks back to gravity with an optional kick. */
	public static void drop(List<FallingBlockEntity> blocks, Vec3d kick) {
		for (FallingBlockEntity fb : blocks) {
			if (fb.isAlive()) {
				fb.setNoGravity(false);
				velocity(fb, fb.getVelocity().add(kick));
			}
		}
	}

	public static void vanish(List<FallingBlockEntity> blocks) {
		for (FallingBlockEntity fb : blocks) {
			fb.discard();
		}
		blocks.clear();
	}

	/** Immediate small sphere edit (r ≤ ~8). */
	public static int sphere(ServerWorld w, BlockPos c, double r, BlockState fill) {
		if (!Destruction.enabled(w)) {
			return 0;
		}
		int n = 0;
		int ri = MathHelper.ceil(r);
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dy = -ri; dy <= ri; dy++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState s = w.getBlockState(p);
					if (s != fill && Destruction.set(w, p.toImmutable(), fill, FLAGS)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	/** A crack running along the surface: removes {@code depth} blocks per column, optional glowing bottom. */
	public static void fissure(ServerWorld w, Vec3d start, Vec3d dir, int length, int depth, int width, @Nullable BlockState bottom) {
		if (!Destruction.enabled(w)) {
			return;
		}
		Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
		Vec3d side = new Vec3d(-flat.z, 0, flat.x);
		for (int i = 0; i < length; i++) {
			double wobble = Math.sin(i * 0.4 + start.x) * 1.2;
			for (int s = -width / 2; s <= width / 2; s++) {
				Vec3d p = start.add(flat.multiply(i)).add(side.multiply(wobble + s));
				int x = MathHelper.floor(p.x), z = MathHelper.floor(p.z);
				if (!w.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				BlockPos top = surface(w, x, z);
				int d = Math.max(1, depth - Math.abs(s) - i * depth / Math.max(1, length * 2));
				for (int k = 0; k < d; k++) {
					BlockPos q = top.down(k);
					Destruction.set(w, q, (k == d - 1 && bottom != null) ? bottom : Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
				}
			}
		}
	}

	/** A shallow circular crack at radius r. */
	public static void ringCrack(ServerWorld w, Vec3d c, double r, int depth, @Nullable BlockState bottom, float coverage) {
		if (!Destruction.enabled(w)) {
			return;
		}
		int n = (int) (r * Math.PI * 2);
		for (int i = 0; i < n; i++) {
			if (w.random.nextFloat() > coverage) {
				continue;
			}
			double a = Math.PI * 2 * i / n;
			int x = MathHelper.floor(c.x + Math.cos(a) * r);
			int z = MathHelper.floor(c.z + Math.sin(a) * r);
			if (!w.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos top = surface(w, x, z);
			if (Math.abs(top.getY() - c.y) > r) {
				continue;
			}
			for (int k = 0; k < depth; k++) {
				Destruction.set(w, top.down(k), (k == depth - 1 && bottom != null) ? bottom : Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
			}
		}
	}

	/** Sends a big title + subtitle to every player within range. */
	public static void title(ServerWorld w, Vec3d c, double range, Text title, Text subtitle) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(c) <= range * range) {
				// The big title line is drawn at 4x and long bomb names ran off screen: use the half-size
				// subtitle line for the headline, and the action bar for the small print.
				p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
				p.networkHandler.sendPacket(new SubtitleS2CPacket(title));
				p.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
				if (!subtitle.getString().isEmpty()) {
					p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(subtitle));
				}
			}
		}
	}

	public static void broadcast(ServerWorld w, Vec3d c, double range, Text message) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(c) <= range * range) {
				p.sendMessage(message, false);
			}
		}
	}

	/** Spawns a mod projectile with a fixed velocity (meteors, missiles, falling payloads). */
	public static com.armageddon.entity.WmdProjectileEntity projectile(ServerWorld w, com.armageddon.entity.ProjectileKind kind, Vec3d at, Vec3d velocity, @Nullable Entity owner) {
		com.armageddon.entity.WmdProjectileEntity p = new com.armageddon.entity.WmdProjectileEntity(com.armageddon.registry.ModEntities.PROJECTILE, w);
		p.setOwner(owner);
		p.setKind(kind);
		p.setPosition(at.x, at.y, at.z);
		p.setVelocity(velocity);
		w.spawnEntity(p);
		return p;
	}

	/** Drops a meteor from high above onto {@code target}. */
	public static void meteor(ServerWorld w, Vec3d target, @Nullable Entity owner) {
		double a = w.random.nextDouble() * Math.PI * 2;
		Vec3d drift = new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(0.9);
		projectile(w, com.armageddon.entity.ProjectileKind.METEOR, target.subtract(drift.multiply(40)).add(0, 100, 0), drift.add(0, -2.6, 0), owner);
	}

	public static Vec3d dir(float yawDeg) {
		return Vec3d.fromPolar(0, yawDeg);
	}

	private Kit() {
	}
}
