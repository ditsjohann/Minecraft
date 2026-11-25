package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.AiShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI TAKEOVER (city destruction). A black core erupts from the middle of the city and opens a red eye. Static
 * crawls over every screen. Circuit traces race out across the streets and everything they reach is assimilated:
 * glass blacked out, walls turned to black and red, lights gone red. A swarm of drones pours out of the core and
 * hunts anything alive with lasers. Then the eye goes white, alarms sound, and a grid of laser walls expands from the
 * core and slices the city into blocks. The drones return, the core uploads itself into the sky, and an EMP blast
 * ends it.
 */
public final class AiEvents {
	private AiEvents() {
	}

	/** Effect id for {@link com.armageddon.network.FxType#NEW_FX} (client: AiFx). */
	public static final int FX_AI = 62;

	public static class AiTakeover extends SciFiEvents.Base {
		private Vec3d base;
		private int cx, cz;
		private final List<int[]> core = new ArrayList<>();
		private int placed;
		private TerrainJob assimilate, cuts;
		private Set<Long> traces;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				base = Kit.ground(world, c.x, c.z);
				cx = MathHelper.floor(base.x);
				cz = MathHelper.floor(base.z);
				CelestialEvents.announce(world, c, "block.armageddon.ai_takeover");
				Vfx.newFx(world, FX_AI, base, base, (float) AiShapes.R, 0xFF1A10, seed(), 0, AiShapes.END);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 96.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 96.0F, 0.5F);
				Vfx.screen(world, base, 400, 3, 0.5F, 20);
				planCore();
				traces = new HashSet<>();
				for (double[] p : AiShapes.traces(seed())) {
					traces.add(BlockPos.asLong(cx + (int) Math.round(p[0]), 0, cz + (int) Math.round(p[1])));
				}
			}
			if (t % 40 == 0 && t < AiShapes.END - 40) {
				Vfx.skyTint(world, base, 100000, 0x140404, 0.55F);
			}
			if (t >= AiShapes.BOOT && t <= AiShapes.BOOTED) {
				build(t);
			}
			if (t == AiShapes.BOOTED) {
				Vec3d eye = base.add(0, AiShapes.EYE_Y, 0);
				Vfx.play(world, VfxIds.AI_BOOT, eye, 1.0F);
				Vfx.flash(world, eye, 0.3F, 400, 0xFF2010);
				Vfx.screen(world, base, 400, 3, 0.7F, 10);
				Kit.sound(world, eye, SoundEvents.BLOCK_BEACON_ACTIVATE, 128.0F, 0.5F);
				Kit.sound(world, eye, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 128.0F, 0.6F);
			}
			chaos(t);
			if (t > AiShapes.BOOTED && t < AiShapes.EMP && t % 35 == 0) {
				Vfx.screen(world, base, 400, 3, 0.25F + world.random.nextFloat() * 0.2F, 4);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_AMBIENT, 96.0F, 0.4F);
			}
			if (t == AiShapes.NETWORK && Destruction.enabled(world)) {
				assimilate = TerrainJob.disc(world, base, AiShapes.R, this::assimilate);
				assimilate.limit = 0;
			}
			if (assimilate != null && t >= AiShapes.NETWORK) {
				assimilate.limit = t >= AiShapes.NETWORK_END ? AiShapes.R + 1 : AiShapes.front(t);
				assimilate.step(5000);
				if (t % 10 == 0 && t < AiShapes.NETWORK_END) {
					Kit.sound(world, base, SoundEvents.BLOCK_SCULK_SPREAD, 96.0F, 0.6F);
					Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 64.0F, 1.6F);
				}
			}
			if (t >= AiShapes.DRONES && t < AiShapes.UPLOAD && t % 25 == 0) {
				Kit.sound(world, base.add(0, AiShapes.EYE_Y, 0), SoundEvents.ENTITY_BEE_LOOP, 96.0F, 0.5F);
			}
			if (t >= AiShapes.HUNT && t < AiShapes.WARN && t % 12 == 0) {
				hunt(t);
			}
			if (t >= AiShapes.WARN && t < AiShapes.PURGE && t % 8 == 0) {
				Kit.sound(world, base, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 128.0F, t % 16 == 0 ? 1.6F : 1.2F);
				Vfx.screen(world, base, 400, 3, 0.35F, 3);
			}
			if (t == AiShapes.PURGE) {
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 128.0F, 1.4F);
				if (Destruction.enabled(world)) {
					long[] cols = cutColumns();
					float[] keys = new float[cols.length];
					for (int i = 0; i < cols.length; i++) {
						keys[i] = (float) Math.hypot(BlockPos.unpackLongX(cols[i]) + 0.5 - base.x, BlockPos.unpackLongZ(cols[i]) + 0.5 - base.z);
					}
					cuts = new TerrainJob(world, cols, keys, this::cut);
					cuts.limit = 0;
				}
			}
			if (t >= AiShapes.PURGE && t <= AiShapes.PURGE_END + 200) {
				double front = AiShapes.purge(t);
				if (cuts != null) {
					cuts.limit = front;
					cuts.step(6000);
				}
				if (t <= AiShapes.PURGE_END) {
					purgeEntities(front);
					if (t % 6 == 0) {
						Kit.sound(world, base, SoundEvents.BLOCK_FIRE_EXTINGUISH, 96.0F, 0.5F);
						Kit.sound(world, base, SoundEvents.ENTITY_GUARDIAN_ATTACK, 96.0F, 0.6F);
						Vfx.shake(world, base, 400, 0.6F);
					}
				}
			}
			if (t == AiShapes.UPLOAD) {
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, 128.0F, 0.6F);
				Kit.sound(world, base, SoundEvents.BLOCK_END_PORTAL_SPAWN, 128.0F, 1.4F);
			}
			if (t == AiShapes.EMP) {
				emp();
			}
			stepJobs(4000);
			boolean busy = (assimilate != null && !assimilate.isDone()) || (cuts != null && !cuts.isDone());
			return t >= AiShapes.END && !busy || t > AiShapes.END + 1200;
		}

		private int seed() {
			return data.getInt("Seed");
		}

		/** The core: a black chamfered shaft with red seams and bands, on a plinth, narrowing to a crown. */
		private void planCore() {
			int foot = MathHelper.floor(base.y);
			for (int y = -1; y <= AiShapes.CORE_H + 4; y++) {
				int half = y < 2 ? 5 : y > AiShapes.CORE_H ? Math.max(1, 3 - (y - (int) AiShapes.CORE_H)) : 3;
				for (int dx = -half; dx <= half; dx++) {
					for (int dz = -half; dz <= half; dz++) {
						boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
						if (Math.abs(dx) == half && Math.abs(dz) == half && half >= 3) {
							continue;
						}
						int kind;
						if (y < 2) {
							kind = edge ? 1 : 0;
						} else if (!edge) {
							kind = y % 12 == 0 ? 1 : 0;
						} else if ((dx == 0 || dz == 0) && y < AiShapes.CORE_H) {
							kind = 2;
						} else {
							kind = y % 12 == 0 ? 1 : 0;
						}
						core.add(new int[]{cx + dx, foot + y, cz + dz, kind});
					}
				}
			}
		}

		private static BlockState coreBlock(int k) {
			return (switch (k) {
				case 1 -> Blocks.POLISHED_BLACKSTONE;
				case 2 -> Blocks.RED_STAINED_GLASS;
				default -> Blocks.BLACK_CONCRETE;
			}).getDefaultState();
		}

		private void build(int t) {
			int to = (int) (AiShapes.smooth((t - AiShapes.BOOT) / (double) (AiShapes.BOOTED - AiShapes.BOOT)) * core.size());
			for (; placed < Math.min(to, core.size()); placed++) {
				int[] b = core.get(placed);
				put(new BlockPos(b[0], b[1], b[2]), coreBlock(b[3]));
			}
			if (t % 4 == 0) {
				Vfx.shake(world, base, 300, 0.5F);
				Vfx.play(world, VfxIds.AI_RISE, base, 1.0F);
				Kit.sound(world, base, SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 64.0F, 0.5F);
			}
		}

		/**
		 * One column assimilated: the ground turned to a black circuit board with red traces, and everything standing
		 * on it blacked out, its lights gone red.
		 */
		private void assimilate(TerrainJob.Column col) {
			int dx = col.x - cx, dz = col.z - cz;
			if (Math.abs(dx) <= 5 && Math.abs(dz) <= 5) {
				return;
			}
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 140); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isAir() || s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.LOGS)) {
					continue;
				}
				double n = col.noise(y);
				if (s.isIn(net.minecraft.registry.tag.BlockTags.IMPERMEABLE) || s.isOf(Blocks.GLASS_PANE)) {
					col.set(y, (n < 0.88 ? Blocks.BLACK_STAINED_GLASS : Blocks.RED_STAINED_GLASS).getDefaultState());
				} else if (s.isOf(Blocks.SEA_LANTERN) || s.isOf(Blocks.GLOWSTONE) || s.isOf(Blocks.REDSTONE_LAMP)) {
					col.set(y, Blocks.REDSTONE_BLOCK.getDefaultState());
				} else if (s.isOpaqueFullCube(col.world(), BlockPos.ORIGIN) && n < 0.7) {
					col.set(y, (n < 0.55 ? Blocks.BLACK_CONCRETE : Blocks.GRAY_CONCRETE).getDefaultState());
				}
			}
			BlockState g = col.get(surf);
			if (!g.isOpaqueFullCube(col.world(), BlockPos.ORIGIN) && g.getFluidState().isEmpty()) {
				return;
			}
			if (traces.contains(BlockPos.asLong(col.x, 0, col.z))) {
				col.set(surf, (col.noise(3) < 0.08 ? Blocks.REDSTONE_BLOCK : Blocks.RED_CONCRETE).getDefaultState());
			} else if (Math.floorMod(dx, 8) == 0 || Math.floorMod(dz, 8) == 0) {
				col.set(surf, Blocks.GRAY_CONCRETE.getDefaultState());
			} else if (g.getFluidState().isEmpty()) {
				col.set(surf, Blocks.BLACK_CONCRETE.getDefaultState());
			}
		}

		/** The drones hunt: each target is shot by whichever drone is nearest it. */
		private void hunt(int t) {
			List<LivingEntity> prey = new ArrayList<>();
			for (LivingEntity e : Kit.living(world, base, AiShapes.R)) {
				if (!Kit.creativeOrSpectator(e)) {
					prey.add(e);
				}
			}
			java.util.Collections.shuffle(prey, new java.util.Random(t));
			int shots = 0;
			for (LivingEntity e : prey) {
				if (shots++ >= 4) {
					break;
				}
				Vec3d target = e.getPos().add(0, e.getHeight() * 0.6, 0);
				Vec3d best = null;
				double bd = Double.MAX_VALUE;
				for (int i = 0; i < AiShapes.DRONE_COUNT; i++) {
					if (t < AiShapes.launch(i) + 45) {
						continue;
					}
					Vec3d d = base.add(AiShapes.drone(i, t, seed()));
					double dist = d.squaredDistanceTo(target);
					if (dist < bd) {
						bd = dist;
						best = d;
					}
				}
				if (best == null) {
					continue;
				}
				Vfx.beam(world, best, target, 0.25F, 0xFF1A10, 6);
				Vfx.play(world, VfxIds.AI_ZAP, target, 0.8F);
				Kit.sound(world, target, SoundEvents.ENTITY_GUARDIAN_ATTACK, 32.0F, 1.6F);
				e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 5.0F);
				e.setOnFireFor(2);
			}
		}

		/** Missiles, the tripods' beams, and reality itself starting to glitch. */
		private void chaos(int t) {
			for (int k = 0; k < AiShapes.MISSILES; k++) {
				int at = AiShapes.missileAt(k);
				if (t == at) {
					Kit.sound(world, base.add(0, AiShapes.CORE_H, 0), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 128.0F, 0.5F);
				}
				if (t == at + AiShapes.MISSILE_FLIGHT) {
					Vec3d tg = AiShapes.missileTarget(k, seed());
					Vec3d hit = Kit.ground(world, base.x + tg.x, base.z + tg.z);
					Vfx.play(world, VfxIds.AI_MISSILE_HIT, hit, 1.0F);
					Vfx.shake(world, hit, 160, 0.9F);
					Kit.sound(world, hit, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.7F);
					Kit.damage(world, hit, 8, 20.0F, ModDamageTypes.PLASMA, null, owner());
					Destruction.explode(world, owner(), hit.x, hit.y, hit.z, 5.0F, true);
				}
			}
			if (t == AiShapes.WALK - 40) {
				for (int i = 0; i < AiShapes.WALKERS; i++) {
					Vec3d w = AiShapes.walker(i, AiShapes.WALK, seed());
					Vec3d g = Kit.ground(world, base.x + w.x, base.z + w.z);
					Vfx.play(world, VfxIds.AI_RISE, g, 2.0F);
					Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 128.0F, 0.4F);
					Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 128.0F, 0.4F);
					if (Destruction.enabled(world)) {
						Destruction.explode(world, owner(), g.x, g.y, g.z, 4.0F, false);
					}
				}
			}
			if (t >= AiShapes.WALK && t < AiShapes.WALK_END) {
				for (int i = 0; i < AiShapes.WALKERS; i++) {
					if (t % 3 == i % 3) {
						Vec3d b = AiShapes.beam(i, t, seed());
						Vec3d hit = Kit.ground(world, base.x + b.x, base.z + b.z);
						Vfx.play(world, VfxIds.AI_ZAP, hit, 1.6F);
						Kit.damage(world, hit, 4, 12.0F, ModDamageTypes.PLASMA, null, owner());
						if (Destruction.enabled(world) && t % 6 == i % 3) {
							Destruction.explode(world, owner(), hit.x, hit.y, hit.z, 2.4F, true);
						}
					}
					if (t % 16 == i * 5) {
						Vec3d w = AiShapes.walker(i, t, seed());
						Vec3d g = Kit.ground(world, base.x + w.x, base.z + w.z);
						Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_STEP, 128.0F, 0.3F);
						Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.4F);
						Vfx.shake(world, g, 90, 0.5F);
					}
					if (t % 12 == i * 4) {
						Vec3d w = AiShapes.walker(i, t, seed());
						Kit.sound(world, base.add(w).add(0, AiShapes.WALKER_H, 0), SoundEvents.ENTITY_GUARDIAN_ATTACK, 96.0F, 0.6F);
					}
				}
			}
			// Reality glitching: chunks of the city corrupted into checkerboards of wrong blocks.
			if (t >= AiShapes.NETWORK_END - 100 && t < AiShapes.PURGE && t % 8 == 0 && Destruction.enabled(world)) {
				double a = world.random.nextDouble() * MathHelper.TAU, d = 12 + world.random.nextDouble() * (AiShapes.R - 14);
				int x = MathHelper.floor(base.x + Math.cos(a) * d), z = MathHelper.floor(base.z + Math.sin(a) * d);
				int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
				int y = MathHelper.floor(base.y) + world.random.nextInt(Math.max(1, top - MathHelper.floor(base.y) + 1));
				BlockState[] glitch = {Blocks.MAGENTA_CONCRETE.getDefaultState(), Blocks.CYAN_CONCRETE.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState(),
						Blocks.RED_CONCRETE.getDefaultState(), Blocks.LIME_CONCRETE.getDefaultState()};
				int n = 1 + world.random.nextInt(2);
				for (int dx = -n; dx <= n; dx++) {
					for (int dy = -n; dy <= n; dy++) {
						for (int dz = -n; dz <= n; dz++) {
							BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
							if (!world.getBlockState(p).isAir()) {
								put(p, glitch[Math.floorMod(dx + dy * 2 + dz * 3 + t, glitch.length)]);
							}
						}
					}
				}
				Vfx.play(world, VfxIds.AI_GLITCH, new Vec3d(x + 0.5, y + 0.5, z + 0.5), 1.0F);
				Kit.sound(world, new Vec3d(x, y, z), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 48.0F, 1.8F);
			}
		}

		/** Every column on a line of the purge grid within reach. */
		private long[] cutColumns() {
			List<Long> out = new ArrayList<>();
			int r = (int) AiShapes.R;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r && AiShapes.onCut(dx, dz) && (Math.abs(dx) > 6 || Math.abs(dz) > 6)) {
						out.add(BlockPos.asLong(cx + dx, 0, cz + dz));
					}
				}
			}
			long[] a = new long[out.size()];
			for (int i = 0; i < a.length; i++) {
				a[i] = out.get(i);
			}
			return a;
		}

		/** A laser wall passes: everything in the column is cut away, the floor of the cut left glowing. */
		private void cut(TerrainJob.Column col) {
			int surf = col.surface();
			col.clear(surf - 3, Math.min(col.top(), surf + 150));
			col.set(surf - 4, Blocks.MAGMA_BLOCK.getDefaultState());
		}

		/** Whatever is standing on a cut line when the wall reaches it is cut too. */
		private void purgeEntities(double front) {
			for (LivingEntity e : Kit.living(world, base, front)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				double dx = e.getX() - base.x, dz = e.getZ() - base.z;
				if (AiShapes.onCut(dx, dz) || AiShapes.onCut(dx + 0.6, dz + 0.6) || AiShapes.onCut(dx - 0.6, dz - 0.6)) {
					Vfx.play(world, VfxIds.AI_ZAP, e.getPos(), 1.2F);
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 200.0F);
				}
			}
		}

		/** The last act: an EMP blast that throws everything back and leaves every screen snowing. */
		private void emp() {
			Vec3d eye = base.add(0, AiShapes.EYE_Y, 0);
			Vfx.play(world, VfxIds.AI_EMP, base.add(0, 1, 0), 1.0F);
			Vfx.flash(world, eye, 0.45F, 600, 0xE0F0FF);
			Vfx.screen(world, base, 600, 3, 0.9F, 30);
			Vfx.shake(world, base, 600, 1.8F);
			Kit.sound(world, eye, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.3F);
			Kit.sound(world, eye, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
			for (LivingEntity e : Kit.living(world, base, AiShapes.R + 30)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d away = e.getPos().subtract(base);
				double d = Math.max(1, Math.hypot(away.x, away.z));
				double k = Math.max(0.2, 1 - d / (AiShapes.R + 30));
				Kit.push(e, new Vec3d(away.x / d * 2.0 * k, 0.8 * k, away.z / d * 2.0 * k));
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION, null, owner()), (float) (16 * k));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 1));
			}
			if (Destruction.enabled(world)) {
				Destruction.explode(world, owner(), base.x, base.y + AiShapes.EYE_Y, base.z, 8.0F, false);
			}
		}
	}
}
