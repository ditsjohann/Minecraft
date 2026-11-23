package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.destruction.celestial2.BlockQueue;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.CelestialIIShapes;
import com.armageddon.util.Paths;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Celestial expansion II: five long, loud, heavily animated world events. Each one leaves its own mark on the
 * land, recognisable from the air: a cow-print countryside with a cow-shaped crater, a scoured twister scar
 * lined with windrows and felled trees, a rune sigil nailed down by a spear, a creeping undead blight, and
 * crop circles with abduction shafts.
 */
public final class CelestialIIEvents {
	private CelestialIIEvents() {
	}

	/** Surface block at x, z; in an unloaded chunk, a harmless spot at the world floor (never loads the chunk). */
	static BlockPos top(ServerWorld w, double x, double z) {
		int ix = MathHelper.floor(x), iz = MathHelper.floor(z);
		if (!w.isChunkLoaded(ix >> 4, iz >> 4)) {
			return new BlockPos(ix, w.getBottomY(), iz);
		}
		return Kit.surface(w, ix, iz);
	}

	/** Block state that reads as air in unloaded chunks instead of loading them. */
	static BlockState at(ServerWorld w, BlockPos p) {
		return w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) ? w.getBlockState(p) : Blocks.AIR.getDefaultState();
	}

	/** Natural ground that the events may repaint (never builds, ores or block entities). */
	static boolean natural(BlockState s) {
		return s.isIn(BlockTags.DIRT) || s.isIn(BlockTags.SAND) || s.isIn(BlockTags.BASE_STONE_OVERWORLD) || s.isOf(Blocks.GRAVEL)
				|| s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.CLAY) || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.SANDSTONE)
				|| s.isOf(Blocks.TERRACOTTA) || s.isOf(Blocks.PACKED_ICE);
	}

	/** Sends one of this group's signature client effects. */
	static void fx(ServerWorld w, int effect, Vec3d a, Vec3d b, float scale, int color, float p1, float p2, int duration) {
		Vfx.custom(w, FxType.CELESTIAL2_FX, a, b, scale, color, effect, p1, p2, duration, Vfx.FAR);
	}

	static final int MILK_CROWN = 1, TWISTER_WAKE = 2, RUNE_SIGIL = 3, BLIGHT_FOG = 4, SPORE_BURST = 5, CROP_STAMP = 6, ABDUCTION = 7;

	/** Base for this group: a lazily-created budgeted block queue. */
	abstract static class Event extends WorldEvent {
		private BlockQueue queue;

		BlockQueue q() {
			if (queue == null) {
				queue = new BlockQueue(world);
			}
			return queue;
		}
	}

	// ====================================================================== EXPLODING COWS

	/**
	 * Pastoral absurdity. Cows rain down and land as milk splats, not explosions: cream-white bowls with splash
	 * rays, a milk-drop crown and a cowbell clang. Then one enormous cow, whose landing presses a cow-shaped
	 * crater into the land and stamps a black-and-white cow print across the whole countryside.
	 */
	public static class ExplodingCows extends Event {
		private static final int BIG_AT = 540;
		private final Map<CowEntity, Integer> cows = new HashMap<>();
		private final Map<CowEntity, Float> scales = new HashMap<>();
		private CowEntity big;

		private void spawn(Vec3d c, float scale, double spread, double height) {
			if (cows.size() > 70) {
				return;
			}
			CowEntity cow = EntityType.COW.create(world);
			if (cow == null) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(world.random.nextDouble()) * spread;
			double x = c.x + Math.cos(a) * d, z = c.z + Math.sin(a) * d;
			cow.refreshPositionAndAngles(x, Kit.ground(world, x, z).y + height, z, world.random.nextFloat() * 360, 0);
			EntityAttributeInstance scaleAttr = cow.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
			if (scaleAttr != null) {
				scaleAttr.setBaseValue(scale);
			}
			// NoAI mobs never run travel(), so gravity does nothing: the event moves the cows itself (see fall()).
			cow.setAiDisabled(true);
			cow.setNoGravity(true);
			cow.setInvulnerable(true);
			cow.setPersistent();
			cow.setVelocity(world.random.nextGaussian() * 0.05, -0.9, world.random.nextGaussian() * 0.05);
			world.spawnEntity(cow);
			// Its shadow swells on the ground where it is going to land.
			Vfx.play(world, scale >= 12 ? VfxIds.COW_SHADOW_BIG : VfxIds.COW_SHADOW, Kit.ground(world, x, z), scale >= 12 ? 12.0F : 0.6F + scale * 0.8F);
			cows.put(cow, age());
			scales.put(cow, scale);
			world.playSound(null, x, cow.getY(), z, SoundEvents.ENTITY_COW_AMBIENT, net.minecraft.sound.SoundCategory.HOSTILE,
					Math.min(8.0F, 1.0F + scale * 0.5F), Math.max(0.2F, 1.7F - scale * 0.12F));
		}

		/** A cow lands: a cream bowl with splash rays, a milk crown, a moo and a cowbell. */
		private void splat(CowEntity cow, float scale) {
			Vec3d p = cow.getPos();
			double rb = 1.2 + scale * 1.6;
			fx(world, MILK_CROWN, p, p, (float) (rb * 1.5), 0, 0, 0, (int) (34 + rb * 3));
			Vfx.shake(world, p, 48 + rb * 10, Math.min(1.6F, 0.2F + scale * 0.2F));
			Vfx.play(world, VfxIds.MILK_SPLASH, p, 0.5F + scale * 0.6F);
			Kit.sound(world, p, SoundEvents.ENTITY_COW_HURT, 6.0F + scale, Math.max(0.3F, 1.4F - scale * 0.15F));
			Kit.sound(world, p, SoundEvents.ENTITY_SLIME_SQUISH, 6.0F + scale * 2, Math.max(0.3F, 0.9F - scale * 0.08F));
			Kit.sound(world, p, SoundEvents.BLOCK_BELL_USE, 4.0F + scale, Math.max(0.5F, 1.6F - scale * 0.15F));
			for (LivingEntity e : Kit.living(world, p, rb * 2.4)) {
				if (e instanceof CowEntity) {
					continue;
				}
				Vec3d out = e.getPos().subtract(p);
				double f = 1 - out.length() / (rb * 2.4);
				e.damage(ModDamageTypes.source(world, ModDamageTypes.NUCLEAR_BLAST, null, owner()), (float) (4 + scale * 5 * f));
				Kit.push(e, out.normalize().multiply(0.6 * f).add(0, 0.35 + 0.3 * f, 0));
			}
			BlockPos b = BlockPos.ofFloored(p);
			int ri = (int) Math.ceil(rb);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double r = Math.sqrt(dx * dx + dz * dz);
					if (r > rb) {
						continue;
					}
					BlockPos s = top(world, b.getX() + dx, b.getZ() + dz);
					int depth = (int) Math.round((1 - (r / rb) * (r / rb)) * rb * 0.45);
					for (int k = 0; k < depth; k++) {
						q().put(s.down(k), Blocks.AIR.getDefaultState());
					}
					q().put(s.down(depth), cream(dx, dz));
				}
			}
			// Splash rays thrown out over the grass.
			int rays = 7 + (int) scale;
			for (int k = 0; k < rays; k++) {
				double ang = MathHelper.TAU * k / rays + world.random.nextDouble() * 0.4;
				double len = rb * (1.6 + world.random.nextDouble() * 1.2);
				for (double d = rb; d < len; d += 0.8) {
					BlockPos s = top(world, p.x + Math.cos(ang) * d, p.z + Math.sin(ang) * d);
					if (natural(at(world, s))) {
						q().put(s, d > len - 1.2 ? Blocks.WHITE_CONCRETE_POWDER.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
					}
				}
			}
			cow.discard();
		}

		private BlockState cream(int x, int z) {
			double h = CelestialIIShapes.hash(x * 7 + 3, z * 11 + 5);
			return (h < 0.55 ? Blocks.WHITE_CONCRETE : h < 0.85 ? Blocks.WHITE_TERRACOTTA : Blocks.CALCITE).getDefaultState();
		}

		/** The enormous cow lands: a cow-shaped crater and a cow print across the countryside. */
		private void imprint(CowEntity cow) {
			Vec3d p = cow.getPos();
			BlockPos b = BlockPos.ofFloored(p);
			fx(world, MILK_CROWN, p, p, 34.0F, 0, 0, 0, 120);
			Vfx.ring(world, p.add(0, 0.4, 0), 110.0F, 4.0F, 0xFFF6E8, true, 70);
			Vfx.fov(world, p, 14.0F, 30);
			Vfx.shake(world, p, 700, 3.0F);
			Vfx.impact(world, p, 1, 2);
			Kit.sound(world, p, SoundEvents.ENTITY_COW_DEATH, 64.0F, 0.3F);
			Kit.sound(world, p, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.5F);
			Kit.sound(world, p, SoundEvents.ENTITY_SLIME_SQUISH, 64.0F, 0.25F);
			Detonations.blast(world, p, 90, 800.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 0, 4.0);
			Vfx.play(world, VfxIds.COW_DOOM, p, 1.0F);
			// Once the spray hangs in the air, the sun comes through it.
			com.armageddon.detonation.DetonationManager.schedule(world, 60, () -> Vfx.play(world, VfxIds.MILK_RAINBOW, p, 1.0F));
			// Boom: the last cow goes off like a warhead of milk.
			Kit.boom(world, p.add(0, 3, 0), 15.0F, 0xFFF6E8, owner(), false);
			Vfx.dome(world, p, 60.0F, 0xFFFFFF, 40);
			Vfx.flash(world, p, 0.8F, 2000, 0xFFFFFF);
			int salt = (int) (Math.abs(p.x) * 13 + Math.abs(p.z) * 7);
			double s = 17;
			Vec3d f = Kit.dir(cow.getYaw()), side = new Vec3d(-f.z, 0, f.x);
			int reach = (int) Destruction.radius(world, 72.0F);
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					double u = dx * f.x + dz * f.z, v = dx * side.x + dz * side.z;
					BlockPos sp = top(world, b.getX() + dx, b.getZ() + dz);
					BlockState patch = (CelestialIIShapes.cowPatch(b.getX() + dx, b.getZ() + dz, salt) ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE).getDefaultState();
					if (CelestialIIShapes.cowSilhouette(u, v, s)) {
						// The cow-shaped crater, floored with its own hide pattern.
						for (int k = 0; k < 6; k++) {
							q().put(sp.down(k), Blocks.AIR.getDefaultState());
						}
						q().put(sp.down(6), patch);
					} else if (CelestialIIShapes.cowSilhouette(u * 0.94, v * 0.9, s)) {
						// A splash rim heaped up around the outline.
						q().put(sp.up(), Blocks.WHITE_CONCRETE.getDefaultState());
					} else if (d < reach * (0.8 + 0.2 * CelestialIIShapes.noise(dx / 9.0, dz / 9.0, salt + 3)) && natural(at(world, sp))) {
						q().put(sp, patch);
					}
				}
			}
			// The milky crater: a deep round bowl blown into the middle of the print, glazed white and pooled with milk.
			double cr = Destruction.radius(world, 22.0F), depthMax = cr * 0.45;
			int ci = (int) Math.ceil(cr) + 2;
			for (int dx = -ci; dx <= ci; dx++) {
				for (int dz = -ci; dz <= ci; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz) + CelestialIIShapes.noise(dx / 5.0, dz / 5.0, salt + 9) * 1.5;
					BlockPos sp = top(world, b.getX() + dx, b.getZ() + dz);
					if (d > cr + 2) {
						continue;
					}
					if (d > cr) {
						// A splashed lip of cream thrown up around the bowl.
						q().put(sp.up(), cream(dx, dz));
						continue;
					}
					int depth = (int) Math.round((1 - (d / cr) * (d / cr)) * depthMax);
					for (int k = -3; k < depth; k++) {
						q().put(sp.down(k), Blocks.AIR.getDefaultState());
					}
					q().put(sp.down(depth), cream(dx, dz));
					q().put(sp.down(depth + 1), Blocks.CALCITE.getDefaultState());
					// Milk pools in the deepest part of the bowl.
					int pool = depth - (int) (depthMax * 0.55);
					for (int k = 0; k < pool; k++) {
						q().put(sp.down(depth - 1 - k), Blocks.POWDER_SNOW.getDefaultState());
					}
				}
			}
			// Hay bales flung across the print.
			for (int k = 0; k < 60; k++) {
				double ang = world.random.nextDouble() * MathHelper.TAU, d = 20 + world.random.nextDouble() * (reach - 20);
				BlockPos sp = top(world, p.x + Math.cos(ang) * d, p.z + Math.sin(ang) * d).up();
				Direction.Axis axis = world.random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
				q().put(sp, Blocks.HAY_BLOCK.getDefaultState().with(PillarBlock.AXIS, axis));
				if (world.random.nextInt(3) == 0) {
					q().put(sp.up(), Blocks.HAY_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
				}
			}
			cow.discard();
		}

		/** Moves a falling cow one tick (accelerating, tumbling); true once it reaches the ground or water. */
		private boolean fall(CowEntity cow, float scale) {
			Vec3d v = cow.getVelocity();
			v = new Vec3d(v.x * 0.92, Math.max(-3.2, Math.min(v.y, -0.6) - 0.06), v.z * 0.92);
			Vec3d next = cow.getPos().add(v);
			double floor = Kit.ground(world, next.x, next.z).y;
			boolean landed = next.y <= floor || !at(world, BlockPos.ofFloored(next)).getFluidState().isEmpty();
			if (landed) {
				next = new Vec3d(next.x, Math.max(floor, next.y), next.z);
			}
			cow.setVelocity(v);
			cow.setPosition(next);
			// Tumble: small cows flail, big ones turn slowly.
			float spin = 14.0F / Math.max(1.0F, scale * 0.6F);
			cow.setYaw(cow.getYaw() + spin);
			cow.setBodyYaw(cow.getYaw());
			cow.setHeadYaw(cow.getYaw());
			return landed;
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.exploding_cows");
				Vfx.skyTint(world, c, 100000, 0xE8F0FF, 0.5F);
				Kit.sound(world, c, SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.5F);
			}
			// Waves: tiny, small, medium, large: then the cow.
			if (t >= 20 && t < 150 && t % 3 == 0) {
				spawn(c, 0.45F, 36, 70 + world.random.nextInt(30));
				spawn(c, 0.45F, 36, 70 + world.random.nextInt(30));
			} else if (t >= 150 && t < 280 && t % 4 == 0) {
				spawn(c, 1.0F, 42, 80 + world.random.nextInt(30));
			} else if (t >= 280 && t < 400 && t % 8 == 0) {
				spawn(c, 2.6F, 48, 90 + world.random.nextInt(30));
			} else if (t >= 400 && t < 500 && t % 14 == 0) {
				spawn(c, 5.5F, 50, 100 + world.random.nextInt(30));
			}
			if (t == 150 || t == 280 || t == 400) {
				// A lowing chord that drops each wave.
				Kit.sound(world, c, SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.9F - t * 0.001F);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 32.0F, 1.2F - t * 0.001F);
			}
			if (t == BIG_AT - 20) {
				Vfx.warning(world, c, 2);
				Vfx.fov(world, c, 8.0F, 20);
			}
			if (t == BIG_AT && big == null) {
				spawn(c, 16.0F, 0.1, 230);
				for (CowEntity cow : cows.keySet()) {
					if (scales.get(cow) >= 12) {
						big = cow;
					}
				}
				if (big != null) {
					big.setVelocity(0, -0.8, 0);
					Vfx.pillar(world, Kit.ground(world, big.getX(), big.getZ()), 16.0F, 0xFFFFFF, false, 120);
				}
			}
			if (big != null && big.isAlive() && t % 8 == 0) {
				Kit.sound(world, big.getPos(), SoundEvents.ENTITY_COW_AMBIENT, 64.0F, 0.2F);
				Vfx.shake(world, big.getPos(), 400, 0.6F + Math.max(0, (230 - (float) (big.getY() - c.y))) * 0.004F);
			}
			List<CowEntity> done = new ArrayList<>();
			for (Map.Entry<CowEntity, Integer> en : cows.entrySet()) {
				CowEntity cow = en.getKey();
				if (!cow.isAlive()) {
					done.add(cow);
					continue;
				}
				float sc = scales.get(cow);
				boolean landed = fall(cow, sc);
				if ((t + cow.getId()) % 4 == 0) {
					Vfx.play(world, VfxIds.COW_STREAK, cow.getPos().add(0, sc * 0.7, 0), new Vec3d(0, 1, 0), Math.max(0.5F, sc), 0);
				}
				if (landed || t - en.getValue() > 260) {
					if (sc >= 12) {
						imprint(cow);
					} else {
						splat(cow, sc);
					}
					done.add(cow);
				}
			}
			for (CowEntity cow : done) {
				cows.remove(cow);
				scales.remove(cow);
			}
			return t > BIG_AT + 200;
		}

		@Override
		public void finish() {
			for (CowEntity cow : cows.keySet()) {
				cow.discard();
			}
			cows.clear();
		}
	}

	// ====================================================================== DEADLY TWISTER

	/**
	 * A mile-high twister wanders across the land. It does not leave craters: it leaves a scar. The ground along its
	 * path is scoured to coarse dirt, gravel and stone with spiral claw-grooves, debris heaps up in windrows along both
	 * edges, trees are torn out and laid flat pointing along the spin, and it ropes out over a funnel-shaped pit.
	 */
	public static class DeadlyTwister extends Event {
		private static final int LIFE = 760;
		private static final double R = 24, SPEED = 0.33;
		private final List<FallingBlockEntity> blocks = new ArrayList<>();
		private Vec3d start, dir;

		private static final BlockState[] SCOUR = {Blocks.COARSE_DIRT.getDefaultState(), Blocks.GRAVEL.getDefaultState(),
				Blocks.COARSE_DIRT.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.COBBLESTONE.getDefaultState()};
		private static final BlockState[] DEBRIS = {Blocks.COBBLESTONE.getDefaultState(), Blocks.GRAVEL.getDefaultState(),
				Blocks.COARSE_DIRT.getDefaultState(), Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.DIRT.getDefaultState(),
				Blocks.OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X)};

		private static boolean scarred(BlockState s) {
			for (BlockState x : SCOUR) {
				if (s == x) {
					return true;
				}
			}
			return false;
		}

		/** Scours the disc under the funnel and heaps windrows on its flanks. */
		private void scar(Vec3d c, int t) {
			BlockPos b = BlockPos.ofFloored(c);
			Vec3d vel = Paths.twister(start, dir, SPEED, t + 1).subtract(Paths.twister(start, dir, SPEED, t));
			Vec3d fwd = new Vec3d(vel.x, 0, vel.z).normalize();
			int ri = (int) (R * 1.25);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double r = Math.sqrt(dx * dx + dz * dz);
					if (r > R * 1.25) {
						continue;
					}
					BlockPos s = top(world, b.getX() + dx, b.getZ() + dz);
					BlockState st = at(world, s);
					if (r < R * 0.6) {
						if (!natural(st) || scarred(st)) {
							continue;
						}
						if (at(world, s.up()).isReplaceable()) {
							q().put(s.up(), Blocks.AIR.getDefaultState());
						}
						double ang = Math.atan2(dz, dx);
						boolean groove = Math.sin(ang * 3 - r * 0.35) > 0.82 && r > 3;
						BlockState mat = SCOUR[(int) (CelestialIIShapes.noise((b.getX() + dx) / 3.0, (b.getZ() + dz) / 3.0, 77) * SCOUR.length * 0.999)];
						if (groove) {
							q().put(s, Blocks.AIR.getDefaultState());
							q().put(s.down(), mat);
						} else {
							q().put(s, mat);
						}
					} else if (r > R * 0.95) {
						// Windrows only on the flanks: what sits beside the funnel stays at the scar's edge.
						double along = Math.abs(dx * fwd.x + dz * fwd.z);
						if (along > R * 0.45 || scarred(st) || !natural(st)) {
							continue;
						}
						double hn = CelestialIIShapes.noise((b.getX() + dx) / 4.0, (b.getZ() + dz) / 4.0, 79);
						int h = (int) (hn * 3.2 * (1 - Math.abs(r - R * 1.1) / (R * 0.15)));
						for (int k = 1; k <= h; k++) {
							q().put(s.up(k), DEBRIS[(int) (CelestialIIShapes.hash(b.getX() + dx + k, b.getZ() + dz) * DEBRIS.length * 0.999)]);
						}
					}
				}
			}
		}

		/** Tears out trees around the funnel and lays them flat along the spin. */
		private void fell(Vec3d c) {
			for (int k = 0; k < 18; k++) {
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double d = R * (0.8 + world.random.nextDouble() * 1.1);
				int x = MathHelper.floor(c.x + Math.cos(ang) * d), z = MathHelper.floor(c.z + Math.sin(ang) * d);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
				int ground = Kit.surface(world, x, z).getY();
				int logs = 0;
				BlockState log = null;
				for (int y = ground + 1; y <= topY; y++) {
					BlockState s = at(world, new BlockPos(x, y, z));
					if (s.isIn(BlockTags.LOGS)) {
						logs++;
						log = s;
					}
				}
				if (logs < 3 || log == null || !log.contains(PillarBlock.AXIS)) {
					continue;
				}
				for (int dx = -3; dx <= 3; dx++) {
					for (int dz = -3; dz <= 3; dz++) {
						for (int y = ground + 1; y <= topY + 1; y++) {
							BlockPos p = new BlockPos(x + dx, y, z + dz);
							BlockState s = at(world, p);
							if (s.isIn(BlockTags.LEAVES) || (dx == 0 && dz == 0 && s.isIn(BlockTags.LOGS))) {
								q().put(p, Blocks.AIR.getDefaultState());
							}
						}
					}
				}
				// Laid down along the spin (counter-clockwise tangent).
				Vec3d tan = new Vec3d(-Math.sin(ang), 0, Math.cos(ang));
				Direction.Axis axis = Math.abs(tan.x) > Math.abs(tan.z) ? Direction.Axis.X : Direction.Axis.Z;
				for (int i = 0; i < logs + 1; i++) {
					BlockPos s = top(world, x + tan.x * i, z + tan.z * i).up();
					q().put(s, log.with(PillarBlock.AXIS, axis));
				}
			}
		}

		/** Where it ropes out: a funnel-shaped pit with spiral grooves. */
		private void funnel(Vec3d c) {
			BlockPos b = BlockPos.ofFloored(c);
			int ri = (int) R;
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double r = Math.sqrt(dx * dx + dz * dz);
					if (r > R) {
						continue;
					}
					double ang = Math.atan2(dz, dx);
					int depth = (int) (Math.pow(1 - r / R, 1.4) * 11 + (Math.sin(ang * 4 - r * 0.5) > 0.6 ? 1 : 0));
					BlockPos s = top(world, b.getX() + dx, b.getZ() + dz);
					for (int k = 0; k < depth; k++) {
						q().put(s.down(k), Blocks.AIR.getDefaultState());
					}
					q().put(s.down(depth), SCOUR[(dx * 31 + dz * 17 & 0x7FFF) % SCOUR.length]);
				}
			}
		}

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start = center();
				dir = Kit.dir(data.getFloat("Yaw"));
				CelestialEvents.announce(world, start, "block.armageddon.deadly_twister");
				Vfx.twister(world, start, dir, (float) R, (float) SPEED, LIFE);
				fx(world, TWISTER_WAKE, start, dir, (float) R, 0, (float) SPEED, 0, LIFE);
				Vfx.skyTint(world, start, 100000, 0x2A323A, 0.8F);
				Kit.sound(world, start, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.3F);
			}
			Vec3d p = Paths.twister(start, dir, SPEED, t);
			Vec3d c = Kit.ground(world, p.x, p.z);
			host.setPosition(c.x, c.y, c.z);
			double ramp = Math.min(1.0, t / 60.0) * (t > LIFE - 60 ? (LIFE - t) / 60.0 : 1.0);
			blocks.removeIf(fb -> !fb.isAlive());
			if (t % 4 == 0 && blocks.size() < 220 && t < LIFE - 80) {
				blocks.addAll(Kit.lift(world, c, R * 0.9, 9, true, true));
			}
			for (FallingBlockEntity fb : blocks) {
				if (!fb.isAlive()) {
					continue;
				}
				Vec3d off = fb.getPos().subtract(c);
				double ang = Math.atan2(off.z, off.x) + 0.26;
				double h = Math.min(120, Math.max(0, off.y) + 0.6);
				double r = MathHelper.clamp(3 + h * 0.2, 3, R);
				Vec3d target = c.add(Math.cos(ang) * r, h, Math.sin(ang) * r);
				Kit.velocity(fb, target.subtract(fb.getPos()).multiply(0.4));
			}
			for (Entity e : Kit.entities(world, c, 78, x -> !(x instanceof FallingBlockEntity) && !Kit.creativeOrSpectator(x)
					&& !(x instanceof com.armageddon.event.EventEntity))) {
				Vec3d d = new Vec3d(c.x - e.getX(), 0, c.z - e.getZ());
				double dist = d.length();
				double k = MathHelper.clamp(1.25 - dist / 78.0, 0.0, 1.25) * ramp;
				if (k <= 0.01 || dist < 0.5) {
					continue;
				}
				Vec3d tangent = new Vec3d(-d.z, 0, d.x).normalize().multiply(0.55 * k);
				Vec3d inward = d.normalize().multiply(0.34 * k);
				double lift = dist < R * 1.6 ? 0.32 * k : 0.04 * k;
				Kit.push(e, tangent.add(inward).add(0, lift, 0));
				e.fallDistance = 0;
				if (e instanceof LivingEntity l && dist < R && t % 5 == 0) {
					l.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 6.0F);
				}
			}
			if (t > 20 && t < LIFE - 40 && t % 6 == 0) {
				scar(c, t);
			}
			if (t > 20 && t % 8 == 4 && t < LIFE - 40) {
				fell(c);
			}
			if (t == LIFE - 30) {
				funnel(c);
				// The calm after: leaves drifting down through slanting sun.
				Vfx.play(world, VfxIds.TWISTER_CALM, c, new Vec3d(0.3, -1, 0.2), 1.0F, 0);
				Kit.sound(world, c, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 64.0F, 0.4F);
			}
			if (t % 6 == 0) {
				Vfx.shake(world, c, 620, (float) (1.4 * ramp));
			}
			if (t % 2 == 0 && t < LIFE - 30) {
				Vfx.play(world, VfxIds.TWISTER_BASE, c, (float) (0.4 + 0.6 * ramp));
			}
			if (t % 30 == 0 && t < LIFE - 60) {
				// Rain driven along the storm's track.
				Vfx.play(world, VfxIds.TWISTER_RAIN, c, new Vec3d(dir.x * 0.5, -1, dir.z * 0.5), 1.0F, 0);
			}
			if (t % 14 == 0) {
				Kit.sound(world, c, SoundEvents.ITEM_ELYTRA_FLYING, 24.0F, 0.3F);
				Kit.sound(world, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 24.0F, 0.5F);
			}
			if (t % 10 == 0) {
				Vec3d bolt = c.add(world.random.nextGaussian() * 20, 0, world.random.nextGaussian() * 20);
				Vec3d hit = Kit.ground(world, bolt.x, bolt.z);
				Vfx.arc(world, c.add(0, 90, 0), hit, 0.8F, 0xE0F0FF);
				Vfx.play(world, VfxIds.STORM_STRIKE, hit, 1.0F);
			}
			if (t >= LIFE) {
				Kit.drop(blocks, new Vec3d(0, 0.3, 0));
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			Kit.drop(blocks, Vec3d.ZERO);
		}
	}

	// ====================================================================== COLOSSAL SPEAR

	/**
	 * A spear the size of a building is driven into the world like a nail. The impact thrusts basalt spikes up in a
	 * ring and splits the ground into spiral fractures; as it drills to the core, a rune sigil burns itself into the
	 * land around the shaft, one glyph after another, until the whole circle is lit.
	 */
	public static class ColossalSpear extends Event {
		/** About five seconds of approach; must match MegaEffects.Spear.THROW on the client. */
		private static final int THROW = 120;
		private int surfaceY, bottomY, steps, sigilDone;
		private final List<long[]> sigil = new ArrayList<>();

		/** A basalt spike leaning outward from the impact. */
		private void spike(BlockPos b, double ang, double rs, double len, double rb) {
			Vec3d axis = new Vec3d(Math.cos(ang) * 0.5, 1, Math.sin(ang) * 0.5).normalize();
			Vec3d base = new Vec3d(b.getX() + 0.5 + Math.cos(ang) * rs, top(world, b.getX() + Math.cos(ang) * rs, b.getZ() + Math.sin(ang) * rs).getY() - 1,
					b.getZ() + 0.5 + Math.sin(ang) * rs);
			for (double s = 0; s < len; s += 0.6) {
				double r = rb * (1 - s / len) + 0.35;
				Vec3d c = base.add(axis.multiply(s));
				int ir = (int) Math.ceil(r);
				for (int dx = -ir; dx <= ir; dx++) {
					for (int dy = -ir; dy <= ir; dy++) {
						for (int dz = -ir; dz <= ir; dz++) {
							if (dx * dx + dy * dy + dz * dz <= r * r) {
								BlockState mat = s > len * 0.7 ? Blocks.SMOOTH_QUARTZ.getDefaultState()
										: s < len * 0.2 ? Blocks.CALCITE.getDefaultState() : Blocks.QUARTZ_PILLAR.getDefaultState();
								q().put(BlockPos.ofFloored(c.x + dx, c.y + dy, c.z + dz), mat);
							}
						}
					}
				}
			}
		}

		/** Spiral fractures splitting the ground outward, floored with magma and lipped with obsidian. */
		private void fractures(BlockPos b, double from, double to) {
			for (int arm = 0; arm < 8; arm++) {
				double a0 = MathHelper.TAU * arm / 8 + 0.3;
				for (double r = from; r < to; r += 0.5) {
					double ang = a0 + (r - 14) * 0.035;
					BlockPos s = top(world, b.getX() + Math.cos(ang) * r, b.getZ() + Math.sin(ang) * r);
					int depth = r < 30 ? 3 : 2;
					for (int k = 0; k < depth; k++) {
						q().put(s.down(k), Blocks.AIR.getDefaultState());
					}
					q().put(s.down(depth), Blocks.GLOWSTONE.getDefaultState());
					BlockPos lip = top(world, b.getX() + Math.cos(ang + 0.6 / r) * (r + 0.8), b.getZ() + Math.sin(ang + 0.6 / r) * (r + 0.8));
					if (natural(at(world, lip))) {
						q().put(lip, Blocks.SMOOTH_QUARTZ.getDefaultState());
					}
				}
			}
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			BlockPos b = BlockPos.ofFloored(c);
			if (t == 0) {
				surfaceY = Kit.surface(world, b.getX(), b.getZ()).getY();
				bottomY = world.getBottomY() + 6;
				int depth = Math.max(20, surfaceY - bottomY);
				steps = (int) Math.ceil(depth / 0.6);
				CelestialEvents.announce(world, c, "block.armageddon.colossal_spear");
				Vfx.skyTint(world, c, 100000, 0xFFF0C8, 0.55F);
				Vfx.pillar(world, c, 10.0F, 0xFFF0C0, false, THROW);
				Vfx.grid(world, c.add(0, 0.2, 0), new Vec3d(0, 1, 0), 34.0F, 0xFFB050, 1, THROW + 4);
				Vfx.spear(world, c, 92.0F, depth, THROW + 24 + steps + 50);
				Kit.sound(world, c, ModSounds.ORBITAL_CHARGE, 64.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
			}
			if (t < THROW) {
				// Heaven announces it: chimes and bells as it falls, feathers of light drifting down over the target.
				if (t % 20 == 0) {
					Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 48.0F, 0.8F + t * 0.006F);
					Kit.sound(world, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 48.0F, 0.5F + t * 0.008F);
				}
				world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y + 40, c.z, 6, 30, 20, 30, 0.02);
				if (t == THROW - 30) {
					Kit.sound(world, c, SoundEvents.ENTITY_PHANTOM_SWOOP, 64.0F, 0.4F);
					Vfx.shake(world, c, 800, 0.8F);
				}
			}
			if (t == THROW) {
				fx(world, RUNE_SIGIL, c, c, CelestialIIShapes.SIGIL_R, 0xFFC860, steps, 0, 24 + steps + 160);
				Vfx.flash(world, c, 0.35F, 400, 0xFFE0B0);
				Vfx.shake(world, c, 800, 3.0F);
				Vfx.impact(world, c, 0, 2);
				Vfx.fov(world, c, 10.0F, 18);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 64.0F, 0.6F);
				Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.3F);
				Detonations.blast(world, c, 70, 3000.0F, ModDamageTypes.ANNIHILATION, owner(), 0, 6.0);
				// The sigil's lines, burned in later in the order the client lights them.
				for (int dx = -CelestialIIShapes.SIGIL_R - 1; dx <= CelestialIIShapes.SIGIL_R + 1; dx++) {
					for (int dz = -CelestialIIShapes.SIGIL_R - 1; dz <= CelestialIIShapes.SIGIL_R + 1; dz++) {
						int code = CelestialIIShapes.sigil(dx, dz);
						if (code > 0) {
							double ang = Math.atan2(dz, dx);
							if (ang < 0) {
								ang += MathHelper.TAU;
							}
							sigil.add(new long[]{Double.doubleToLongBits(ang), dx, dz, code});
						}
					}
				}
				sigil.sort((x, y) -> Double.compare(Double.longBitsToDouble(x[0]), Double.longBitsToDouble(y[0])));
			}
			// The ring of spikes punching up one after another, then the fractures running outward.
			if (t >= THROW + 4 && t < THROW + 4 + 14 * 3 && (t - THROW - 4) % 3 == 0) {
				int i = (t - THROW - 4) / 3;
				double ang = MathHelper.TAU * i / 14 + CelestialIIShapes.hash(i, 51) * 0.3;
				spike(b, ang, 16 + CelestialIIShapes.hash(i, 52) * 10, 11 + CelestialIIShapes.hash(i, 53) * 10, 1.8 + CelestialIIShapes.hash(i, 54) * 1.2);
				Vec3d at = new Vec3d(c.x + Math.cos(ang) * 20, c.y, c.z + Math.sin(ang) * 20);
				Kit.sound(world, at, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, 32.0F, 0.4F);
				Kit.sound(world, at, SoundEvents.BLOCK_BASALT_BREAK, 32.0F, 0.5F);
				Kit.damage(world, at, 6, 30.0F, ModDamageTypes.SHRAPNEL, null, owner());
			}
			if (t >= THROW + 10 && t < THROW + 30) {
				double r0 = 14 + (t - THROW - 10) * 2.1;
				fractures(b, r0, r0 + 2.1);
			}
			int drill = t - (THROW + 24);
			if (drill >= 0 && drill < steps && Destruction.enabled(world)) {
				double y = surfaceY - drill * 0.6;
				double psi = drill * 0.38;
				int R = 12;
				for (int layer = 0; layer < 2; layer++) {
					int yy = (int) Math.floor(y) - layer;
					for (int dx = -R - 2; dx <= R + 2; dx++) {
						for (int dz = -R - 2; dz <= R + 2; dz++) {
							double d2 = dx * dx + dz * dz;
							BlockPos p = new BlockPos(b.getX() + dx, yy, b.getZ() + dz);
							if (d2 <= R * R) {
								Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
							} else if (d2 <= (R + 2) * (R + 2)) {
								double ang = Math.atan2(dz, dx);
								double diff = Math.abs(MathHelper.wrapDegrees((float) Math.toDegrees(ang - psi)));
								if (diff < 28 && layer == 0) {
									TemporaryBlocks.place(world, p, ModBlocksRef.molten(), 500);
								}
							}
						}
					}
				}
				if (drill % 4 == 0) {
					Vfx.shake(world, c, 700, 0.9F);
					Kit.sound(world, c, SoundEvents.BLOCK_GRINDSTONE_USE, 40.0F, 0.3F + drill * 0.001F);
					Kit.damage(world, new Vec3d(c.x, Math.max(y, bottomY), c.z), R + 3, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
					world.spawnParticles(ParticleTypes.LAVA, c.x, surfaceY + 2, c.z, 10, R * 0.7, 2, R * 0.7, 0.0);
				}
			}
			// Burn in the part of the sigil the drill has "paid for".
			if (drill >= 0 && sigilDone < sigil.size()) {
				double sweep = CelestialIIShapes.sigilLit(Math.min(drill, steps) / (double) steps) * MathHelper.TAU;
				int before = sigilDone;
				while (sigilDone < sigil.size() && Double.longBitsToDouble(sigil.get(sigilDone)[0]) <= sweep) {
					long[] e = sigil.get(sigilDone++);
					BlockPos s = top(world, b.getX() + e[1], b.getZ() + e[2]);
					BlockState mat = e[3] == 2 ? Blocks.SMOOTH_QUARTZ.getDefaultState()
							: ((e[1] + e[2]) % 5 == 0 ? Blocks.SEA_LANTERN : Blocks.GOLD_BLOCK).getDefaultState();
					q().put(s, mat);
				}
				if (sigilDone - before > 0 && drill % 10 == 0) {
					Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 24.0F, 0.6F + (float) (sweep / MathHelper.TAU) * 0.6F);
				}
			}
			if (drill == steps) {
				// The core answers: a geyser of light up the shaft, the whole sigil rings, and the land blossoms white.
				Vfx.pillar(world, c, 9.0F, 0xFFF4D0, false, 220);
				Vfx.skyTint(world, c, 100000, 0xFFF8E0, 0.6F);
				blossom(b);
				Vfx.ring(world, c.add(0, 0.6, 0), CelestialIIShapes.SIGIL_R + 6, 2.0F, 0xFFC860, false, 50);
				Vfx.shake(world, c, 800, 2.0F);
				Kit.sound(world, c, ModSounds.NUKE_DETONATE, 64.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.4F);
				world.spawnParticles(ParticleTypes.END_ROD, c.x, surfaceY + 4, c.z, 400, 5, 30, 5, 0.2);
				if (Destruction.enabled(world)) {
					for (int dx = -9; dx <= 9; dx++) {
						for (int dz = -9; dz <= 9; dz++) {
							if (dx * dx + dz * dz <= 81) {
								Destruction.set(world, new BlockPos(b.getX() + dx, bottomY, b.getZ() + dz), Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
							}
						}
					}
				}
			}
			return drill > steps + 60;
		}

		/** Heaven on earth: white flowers, lilies and lanterns of light spread over the grass around the sigil. */
		private void blossom(BlockPos b) {
			int reach = (int) Destruction.radius(world, 70.0F);
			BlockState[] flowers = {Blocks.LILY_OF_THE_VALLEY.getDefaultState(), Blocks.OXEYE_DAISY.getDefaultState(),
					Blocks.WHITE_TULIP.getDefaultState(), Blocks.AZURE_BLUET.getDefaultState()};
			for (int i = 0; i < reach * reach / 6; i++) {
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double d = CelestialIIShapes.SIGIL_R + 2 + world.random.nextDouble() * (reach - CelestialIIShapes.SIGIL_R);
				BlockPos s = top(world, b.getX() + Math.cos(ang) * d, b.getZ() + Math.sin(ang) * d);
				BlockState ground = at(world, s);
				if (!ground.isOf(Blocks.GRASS_BLOCK) || !at(world, s.up()).isAir()) {
					continue;
				}
				q().put(s.up(), world.random.nextInt(40) == 0 ? Blocks.END_ROD.getDefaultState() : flowers[world.random.nextInt(flowers.length)]);
			}
		}
	}

	/** Tiny indirection so the event file doesn't need a hard import of ModBlocks for one block. */
	static final class ModBlocksRef {
		static BlockState molten() {
			return com.armageddon.registry.ModBlocks.MOLTEN_CORE.getDefaultState();
		}
	}

	// ====================================================================== ZOMBIE APOCALYPSE

	/**
	 * Not a blast but a disease. A blight creeps out from the bomb in ragged fingers: grass turns to podzol,
	 * mycelium and soul soil, trees lose their leaves, bones break the surface, gravestones and broken fences
	 * appear, and a low green fog rolls over it all. The undead tide grows on top of it, and each giant that
	 * falls bursts into a sickly green-glass crater.
	 */
	public static class ZombieApocalypse extends Event {
		private static final int LIFE = 1820;
		private static final int GIANT_FUSE = 420;
		private final List<ZombieEntity> zombies = new ArrayList<>();
		private final Map<GiantEntity, Integer> giants = new HashMap<>();
		/** Where each giant is currently lumbering: a random spot in the blight, re-rolled on arrival or every so often. */
		private final Map<GiantEntity, Vec3d> wander = new HashMap<>();

		private Vec3d wanderGoal(Vec3d c) {
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = 20 + world.random.nextDouble() * 110;
			return new Vec3d(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
		}

		private static final BlockState[] BLIGHT = {Blocks.PODZOL.getDefaultState(), Blocks.MYCELIUM.getDefaultState(),
				Blocks.COARSE_DIRT.getDefaultState(), Blocks.SOUL_SOIL.getDefaultState(), Blocks.ROOTED_DIRT.getDefaultState(),
				Blocks.PODZOL.getDefaultState()};

		private static boolean blight(BlockState s) {
			for (BlockState x : BLIGHT) {
				if (s == x) {
					return true;
				}
			}
			return s.isOf(Blocks.SOUL_SAND);
		}

		private void spawnZombie(Vec3d c, double minR, double maxR) {
			if (zombies.size() >= 150) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			double d = minR + world.random.nextDouble() * (maxR - minR);
			Vec3d g = Kit.ground(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d);
			ZombieEntity z = EntityType.ZOMBIE.create(world);
			if (z == null) {
				return;
			}
			z.refreshPositionAndAngles(g.x, g.y, g.z, world.random.nextFloat() * 360, 0);
			z.setPersistent();
			z.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100000, 0, false, false));
			z.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100000, 0, false, false));
			EntityAttributeInstance follow = z.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
			if (follow != null) {
				follow.setBaseValue(80);
			}
			world.spawnEntity(z);
			zombies.add(z);
			// They claw up out of the ground.
			world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PODZOL.getDefaultState()),
					g.x, g.y + 0.2, g.z, 20, 0.4, 0.1, 0.4, 0.1);
			world.spawnParticles(ParticleTypes.SOUL, g.x, g.y + 0.5, g.z, 3, 0.3, 0.5, 0.3, 0.05);
		}

		private void spawnGiant(Vec3d c) {
			if (giants.size() >= 6) {
				return;
			}
			double a = world.random.nextDouble() * Math.PI * 2;
			Vec3d g = Kit.ground(world, c.x + Math.cos(a) * 80, c.z + Math.sin(a) * 80);
			GiantEntity giant = EntityType.GIANT.create(world);
			if (giant == null) {
				return;
			}
			giant.refreshPositionAndAngles(g.x, g.y, g.z, 0, 0);
			giant.setPersistent();
			world.spawnEntity(giant);
			giants.put(giant, age());
			Vfx.ring(world, g, 30.0F, 2.0F, 0x40C040, true, 30);
			Vfx.shake(world, g, 400, 1.2F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_EMERGE, 64.0F, 0.4F);
			world.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PODZOL.getDefaultState()),
					g.x, g.y + 1, g.z, 300, 4, 1, 4, 0.3);
		}

		/** A giant bursts: a bowl lined with green glass and slime, ribs of bone around its rim. */
		private void giantBoom(GiantEntity giant) {
			Vec3d p = giant.getPos();
			fx(world, SPORE_BURST, p, p, 14.0F, 0x9CFF6A, 0, 0, 160);
			Vfx.ring(world, p.add(0, 0.5, 0), 80.0F, 3.0F, 0x70FF50, true, 50);
			Vfx.shake(world, p, 500, 2.0F);
			Vfx.impact(world, p, 2, 1);
			Kit.sound(world, p, ModSounds.NUKE_DETONATE, 64.0F, 0.9F);
			Kit.sound(world, p, SoundEvents.ENTITY_SLIME_DEATH, 64.0F, 0.3F);
			Kit.sound(world, p, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 64.0F, 0.4F);
			world.spawnParticles(ModParticles.TOXIC_GAS, p.x, p.y + 4, p.z, 300, 10, 6, 10, 0.15);
			Detonations.blast(world, p, 40, 400.0F, ModDamageTypes.NUCLEAR_BLAST, owner(), 0, 3.0);
			for (LivingEntity e : Kit.living(world, p, 30)) {
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 1));
			}
			BlockPos b = BlockPos.ofFloored(p);
			int R = (int) Destruction.radius(world, 11.0F);
			for (int dx = -R - 2; dx <= R + 2; dx++) {
				for (int dz = -R - 2; dz <= R + 2; dz++) {
					for (int dy = -8; dy <= 10; dy++) {
						double d = Math.sqrt(dx * dx + dz * dz + (dy * 1.6) * (dy * 1.6));
						BlockPos at = b.add(dx, dy, dz);
						if (d < R) {
							q().put(at, Blocks.AIR.getDefaultState());
						} else if (d < R + 1.6 && dy <= 0) {
							double h = CelestialIIShapes.hash(at.getX() * 5 + at.getY(), at.getZ() * 3);
							q().put(at, (h < 0.55 ? Blocks.LIME_STAINED_GLASS : h < 0.85 ? Blocks.GREEN_STAINED_GLASS : Blocks.SLIME_BLOCK).getDefaultState());
						}
					}
				}
			}
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					q().put(b.add(dx, -(int) (R / 1.6), dz), Blocks.SLIME_BLOCK.getDefaultState());
				}
			}
			// Ribs: curved bone columns leaning in over the bowl.
			for (int k = 0; k < 8; k++) {
				double ang = MathHelper.TAU * k / 8;
				for (int i = 0; i < 6; i++) {
					double rr = R + 1 - i * 0.6;
					BlockPos rim = top(world, b.getX() + Math.cos(ang) * (R + 1.5), b.getZ() + Math.sin(ang) * (R + 1.5));
					q().put(new BlockPos(MathHelper.floor(b.getX() + Math.cos(ang) * rr), rim.getY() + 1 + i, MathHelper.floor(b.getZ() + Math.sin(ang) * rr)),
							Blocks.BONE_BLOCK.getDefaultState());
				}
			}
			giant.discard();
		}

		/** Spreads the blight over a sample of columns inside its current reach. */
		private void creep(Vec3d c, double r) {
			BlockPos b = BlockPos.ofFloored(c);
			for (int i = 0; i < 260; i++) {
				double ang = world.random.nextDouble() * MathHelper.TAU;
				double d = Math.sqrt(world.random.nextDouble()) * r * 1.17;
				double dx = Math.cos(ang) * d, dz = Math.sin(ang) * d;
				if (!CelestialIIShapes.blighted(dx, dz, r)) {
					continue;
				}
				int x = b.getX() + MathHelper.floor(dx), z = b.getZ() + MathHelper.floor(dz);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				// Trees lose their crowns first.
				BlockPos crown = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z);
				if (at(world, crown).isIn(BlockTags.LEAVES)) {
					q().put(crown, Blocks.AIR.getDefaultState());
				}
				BlockPos s = Kit.surface(world, x, z);
				BlockState st = at(world, s);
				if (blight(st) || !natural(st)) {
					continue;
				}
				double n = CelestialIIShapes.noise(x / 5.0, z / 5.0, 101);
				BlockState mat = st.isIn(BlockTags.SAND) ? Blocks.SOUL_SAND.getDefaultState() : BLIGHT[(int) (n * BLIGHT.length * 0.999)];
				q().put(s, mat);
				BlockPos up = s.up();
				if (!at(world, up).isAir() && !at(world, up).isReplaceable()) {
					continue;
				}
				double roll = world.random.nextDouble();
				if (roll < 0.05) {
					q().put(up, Blocks.DEAD_BUSH.getDefaultState());
				} else if (roll < 0.058) {
					// A gravestone.
					q().put(up, Blocks.CRACKED_STONE_BRICKS.getDefaultState());
					q().put(up.up(), Blocks.STONE_BRICK_SLAB.getDefaultState());
				} else if (roll < 0.064) {
					// Ribs breaking the surface.
					q().put(up, Blocks.BONE_BLOCK.getDefaultState());
					q().put(up.up(), Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X));
				} else if (roll < 0.071) {
					// A broken run of fence.
					Direction.Axis axis = world.random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
					int len = 1 + world.random.nextInt(3);
					for (int k = 0; k < len; k++) {
						BlockPos f = top(world, x + (axis == Direction.Axis.X ? k : 0), z + (axis == Direction.Axis.Z ? k : 0)).up();
						q().put(f, Blocks.SPRUCE_FENCE.getDefaultState());
					}
				} else if (roll < 0.075) {
					q().put(up, Blocks.COBWEB.getDefaultState());
				} else if (roll < 0.0775) {
					q().put(up, Blocks.SOUL_LANTERN.getDefaultState());
				}
			}
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.zombie_apocalypse");
				fx(world, BLIGHT_FOG, c, c, (float) CelestialIIShapes.BLIGHT_MAX, 0x8FA878, 0, 0, LIFE + 120);
				Vfx.skyTint(world, c, 100000, 0x1C3A18, 0.8F);
				Vfx.screen(world, c, 100000, 0, 0.3F, LIFE - 200);
				Kit.sound(world, c, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 64.0F, 0.4F);
			}
			double r = CelestialIIShapes.blightRadius(t);
			if (t % 4 == 0 && r > 2) {
				creep(c, r);
			}
			// A heartbeat under the ground that quickens as the blight grows.
			int beat = Math.max(14, 50 - t / 50);
			if (t % beat == 0) {
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 48.0F, 0.6F);
			}
			if (t % 90 == 45) {
				Kit.sound(world, c, SoundEvents.ENTITY_ZOMBIE_AMBIENT, 48.0F, 0.4F);
			}
			if (t < LIFE - 250) {
				int every = Math.max(2, 10 - t / 180);
				if (t % every == 0) {
					int n = 1 + t / 150;
					for (int i = 0; i < n; i++) {
						spawnZombie(c, 28, Math.max(36, Math.min(80, r)));
					}
				}
			}
			if (t == 300 || t == 620 || t == 940 || t == 1160 || t == 1360) {
				spawnGiant(c);
			}
			zombies.removeIf(z -> !z.isAlive());
			List<GiantEntity> dead = new ArrayList<>();
			for (Map.Entry<GiantEntity, Integer> en : giants.entrySet()) {
				GiantEntity giant = en.getKey();
				if (!giant.isAlive()) {
					// Killed: it goes off. Removed without dying (e.g. Peaceful despawn): just forget it.
					if (giant.isDead()) {
						giantBoom(giant);
					}
					dead.add(giant);
					continue;
				}
				// Giants have no AI of their own: they lumber about the blight at random, wherever their feet take them.
				Vec3d goal = wander.get(giant);
				if (goal == null || (t - en.getValue()) % 240 == 0 || Math.hypot(goal.x - giant.getX(), goal.z - giant.getZ()) < 6) {
					goal = wanderGoal(c);
					wander.put(giant, goal);
				}
				Vec3d to = new Vec3d(goal.x - giant.getX(), 0, goal.z - giant.getZ());
				if (to.length() > 3) {
					Vec3d v = to.normalize().multiply(0.26);
					giant.setVelocity(v.x, giant.getVelocity().y, v.z);
					giant.velocityModified = true;
					giant.setYaw((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
					giant.setBodyYaw(giant.getYaw());
					giant.setHeadYaw(giant.getYaw());
				}
				if (t % 2 == 0 && Destruction.enabled(world)) {
					BlockPos f = giant.getBlockPos();
					Vec3d dirTo = to.length() > 0.1 ? to.normalize() : Vec3d.ZERO;
					BlockPos ahead = BlockPos.ofFloored(giant.getX() + dirTo.x * 4, giant.getY(), giant.getZ() + dirTo.z * 4);
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							for (int dy = 0; dy <= 18; dy++) {
								BlockPos p = ahead.add(dx, dy, dz);
								if (dx * dx + dz * dz <= 20 && !at(world, p).isAir()) {
									Destruction.set(world, p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
								}
							}
						}
					}
					if (f.getY() > 0 && giant.isOnGround() && to.length() > 3 && giant.horizontalCollision) {
						giant.setVelocity(giant.getVelocity().x, 0.6, giant.getVelocity().z);
					}
				}
				if (t % 14 == 0) {
					Vec3d foot = giant.getPos();
					Vfx.shake(world, foot, 260, 0.9F);
					Vfx.ring(world, foot.add(0, 0.2, 0), 14.0F, 1.0F, 0x70A060, true, 10);
					Kit.sound(world, foot, SoundEvents.ENTITY_IRON_GOLEM_STEP, 32.0F, 0.2F);
					// Not Kit.damage: that would hit the giant itself (and the other giants) every stomp.
					for (LivingEntity e : Kit.living(world, foot, 8)) {
						if (e instanceof GiantEntity) {
							continue;
						}
						e.timeUntilRegen = 0;
						e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, giant, owner()), (float) Math.max(1.0, 24.0 * (1.0 - e.getPos().distanceTo(foot) / 8.0)));
					}
					// Each footprint leaves the blight behind it.
					BlockPos fp = giant.getBlockPos();
					for (int dx = -2; dx <= 2; dx++) {
						for (int dz = -2; dz <= 2; dz++) {
							BlockPos s = top(world, fp.getX() + dx, fp.getZ() + dz);
							if (natural(at(world, s))) {
								q().put(s, Blocks.SOUL_SOIL.getDefaultState());
							}
						}
					}
				}
				if (t - en.getValue() > GIANT_FUSE) {
					giantBoom(giant);
					dead.add(giant);
				}
			}
			dead.forEach(giants::remove);
			dead.forEach(wander::remove);
			if (t >= LIFE) {
				for (ZombieEntity z : zombies) {
					world.spawnParticles(ParticleTypes.POOF, z.getX(), z.getY() + 1, z.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
					z.discard();
				}
				zombies.clear();
				return true;
			}
			return false;
		}

		@Override
		public void finish() {
			for (ZombieEntity z : zombies) {
				z.discard();
			}
			for (GiantEntity g : giants.keySet()) {
				g.discard();
			}
			zombies.clear();
			giants.clear();
		}
	}

	// ====================================================================== UFO INVASION

	/**
	 * They are not here to blow us up: they are here to write on us. The beam scorches a glowing glass trail across
	 * the land, and every pulse prints a precise crop circle where it touches down; every third pulse lifts a perfect
	 * cylinder of ground straight up into the ship, leaving a clean shaft behind.
	 */
	public static class UfoInvasion extends Event {
		private static final int LIFE = 640;
		/** Scout saucers flanking the mothership: lane offset, altitude above the mothership's, start tick. */
		private static final double[][] SCOUTS = {{-150, -50, 30}, {150, -40, 60}, {-280, -60, 110}, {280, -55, 140}};
		private static final float SCOUT_R = 24.0F;
		private Vec3d from, to, dir;
		private int stamps;
		private Vec3d[] scoutFrom, scoutTo;

		private int scoutLife(int k) {
			return LIFE - (int) SCOUTS[k][2];
		}

		/** A scout's beam: no writing, just a trail of explosions where it touches down. */
		private void scouts(int t) {
			for (int k = 0; k < SCOUTS.length; k++) {
				int st = (int) SCOUTS[k][2], life = scoutLife(k), a = t - st;
				if (a == 0) {
					Vfx.ufo(world, scoutFrom[k], scoutTo[k], SCOUT_R, life);
				}
				if (a < life * 0.12 || a > life * 0.88 || a % 6 != 0) {
					continue;
				}
				Vec3d ship = Paths.ufo(scoutFrom[k], scoutTo[k], a, life);
				Vec3d b = Paths.ufoBeam(new Vec3d(ship.x, 0, ship.z), dir, a);
				if (!world.isChunkLoaded(BlockPos.ofFloored(b))) {
					continue;
				}
				Vec3d g = Kit.ground(world, b.x, b.z);
				Kit.boom(world, g.add(0, 0.5, 0), 2.6F + world.random.nextFloat() * 0.8F, 0x60B8FF, owner(), true);
				Vfx.play(world, VfxIds.PLASMA_IMPACT, g.add(0, 0.5, 0), new Vec3d(0, 1, 0), 1.6F, 0x60B8FF);
				world.spawnParticles(ParticleTypes.FLAME, g.x, g.y + 1, g.z, 20, 2, 1, 2, 0.08);
			}
		}

		private void cropCircle(Vec3d g, int index) {
			fx(world, CROP_STAMP, g, g, 1.0F, index, 0, 0, 240);
			Vfx.play(world, VfxIds.CROP_PULSE, g, 1.0F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 48.0F, 0.6F);
			Kit.sound(world, g, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 48.0F, 0.8F);
			List<CelestialIIShapes.Mark> marks = CelestialIIShapes.crop(index);
			double rot = CelestialIIShapes.cropAngle(index);
			double cos = Math.cos(rot), sin = Math.sin(rot);
			BlockPos b = BlockPos.ofFloored(g);
			for (int dx = -30; dx <= 30; dx++) {
				for (int dz = -30; dz <= 30; dz++) {
					double lx = dx * cos + dz * sin, lz = -dx * sin + dz * cos;
					if (!CelestialIIShapes.onCrop(marks, lx, lz)) {
						continue;
					}
					BlockPos s = top(world, b.getX() + dx, b.getZ() + dz);
					BlockState st = at(world, s);
					if (!natural(st)) {
						continue;
					}
					// Laid flat: grass becomes path, anything else is bleached.
					if (at(world, s.up()).isReplaceable()) {
						q().put(s.up(), Blocks.AIR.getDefaultState());
					}
					q().put(s, st.isIn(BlockTags.DIRT) ? Blocks.DIRT_PATH.getDefaultState() : Blocks.WHITE_CONCRETE_POWDER.getDefaultState());
				}
			}
		}

		private void abduct(Vec3d g, double shipY) {
			double r = 3.6;
			fx(world, ABDUCTION, g, g, (float) r, 0x80D8FF, (float) Math.max(20, shipY - g.y), 0, 110);
			Vfx.play(world, VfxIds.ABDUCT_LIFT, g, new Vec3d(0, Math.max(20, shipY - g.y), 0), 1.0F, 0);
			Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 48.0F, 0.4F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 0.5F);
			BlockPos b = BlockPos.ofFloored(g);
			int surface = top(world, g.x, g.z).getY();
			for (int dx = -5; dx <= 5; dx++) {
				for (int dz = -5; dz <= 5; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= r) {
						for (int y = surface + 24; y >= surface - 14; y--) {
							q().put(new BlockPos(b.getX() + dx, y, b.getZ() + dz), Blocks.AIR.getDefaultState());
						}
					} else if (d <= r + 1.1) {
						BlockPos s = top(world, b.getX() + dx, b.getZ() + dz);
						q().put(s, Blocks.BLUE_STAINED_GLASS.getDefaultState());
					}
				}
			}
			for (Entity e : Kit.entities(world, g, 6, x -> !Kit.creativeOrSpectator(x) && !(x instanceof com.armageddon.event.EventEntity))) {
				Kit.push(e, new Vec3d(0, 1.4, 0));
				if (e instanceof LivingEntity l) {
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 3));
				}
			}
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				dir = Kit.dir(data.getFloat("Yaw"));
				double alt = Kit.ground(world, c.x, c.z).y + 170;
				from = new Vec3d(c.x - dir.x * 560, alt, c.z - dir.z * 560);
				to = new Vec3d(c.x + dir.x * 560, alt, c.z + dir.z * 560);
				CelestialEvents.announce(world, c, "block.armageddon.ufo_invasion");
				Vfx.ufo(world, from, to, 95.0F, LIFE);
				Vec3d side = new Vec3d(-dir.z, 0, dir.x);
				scoutFrom = new Vec3d[SCOUTS.length];
				scoutTo = new Vec3d[SCOUTS.length];
				for (int k = 0; k < SCOUTS.length; k++) {
					Vec3d off = side.multiply(SCOUTS[k][0]).add(0, SCOUTS[k][1], 0);
					scoutFrom[k] = from.add(off).subtract(dir.multiply(60));
					scoutTo[k] = to.add(off);
				}
				Vfx.skyTint(world, c, 100000, 0x0A1624, 0.6F);
				Kit.sound(world, c, ModSounds.DEATH_RAY, 64.0F, 0.3F);
			}
			if (scoutFrom != null) {
				scouts(t);
			}
			Vec3d ship = Paths.ufo(from, to, t, LIFE);
			if (t % 4 == 0 && t < LIFE) {
				// The drive's cold haze, shed under the hull as it passes.
				Vfx.play(world, VfxIds.UFO_DRIVE, ship.add(0, -12, 0), 3.5F);
			}
			if (t % 20 == 0) {
				Kit.sound(world, new Vec3d(ship.x, c.y, ship.z), ModSounds.DEATH_RAY, 64.0F, 0.35F);
				Vfx.shake(world, new Vec3d(ship.x, c.y, ship.z), 500, 0.8F);
			}
			boolean on = t > LIFE * 0.12 && t < LIFE * 0.88;
			Vec3d b = on ? Paths.ufoBeam(new Vec3d(ship.x, 0, ship.z), dir, t) : null;
			// Far out of view the beam only paints the sky: carving unloaded chunks would load them (lag) and hit bedrock.
			if (on && world.isChunkLoaded(BlockPos.ofFloored(b))) {
				Vec3d g = Kit.ground(world, b.x, b.z);
				// The scorched trail: a ribbon of blue glass with light buried under its spine.
				BlockPos bp = BlockPos.ofFloored(g);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						BlockPos s = top(world, bp.getX() + dx, bp.getZ() + dz);
						BlockState st = at(world, s);
						if (natural(st) || st.isOf(Blocks.DIRT_PATH)) {
							q().put(s, (dx == 0 && dz == 0 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.BLUE_STAINED_GLASS).getDefaultState());
							if (dx == 0 && dz == 0 && t % 3 == 0) {
								q().put(s.down(), Blocks.SEA_LANTERN.getDefaultState());
							}
						}
					}
				}
				// Tractor ring: things near the beam are lifted toward the ship before the core vaporises them.
				for (Entity e : Kit.entities(world, g, 22, x -> !Kit.creativeOrSpectator(x) && !(x instanceof FallingBlockEntity)
						&& !(x instanceof com.armageddon.event.EventEntity))) {
					Vec3d in = new Vec3d(g.x - e.getX(), 0, g.z - e.getZ());
					Kit.push(e, in.multiply(0.03).add(0, 0.16, 0));
					e.fallDistance = 0;
				}
				if (t % 2 == 0) {
					Kit.damage(world, g, 5, 1000.0F, ModDamageTypes.ANNIHILATION, null, owner());
					Kit.damage(world, g.add(0, 25, 0), 8, 1000.0F, ModDamageTypes.ANNIHILATION, null, owner());
				}
				if (t % CelestialIIShapes.CROP_EVERY == 0) {
					int index = stamps++;
					cropCircle(g, index);
					if (index % 3 == 2) {
						abduct(g, ship.y);
					}
				}
				if (t % 8 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 24.0F, 0.5F);
				}
			}
			return t > LIFE + 30;
		}
	}
}
