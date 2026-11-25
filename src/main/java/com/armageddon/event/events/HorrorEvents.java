package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.event.Ruin;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.HorrorShapes;
import com.armageddon.util.HorrorShapes.HellGate;
import com.armageddon.util.HorrorShapes.Horseman;
import com.armageddon.util.HorrorShapes.Maniac;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The horror celestials: the TERROR MANIAC, the PUMPKIN HORSEMAN and the PORTAL OF HELL.
 */
public final class HorrorEvents {
	private HorrorEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: HorrorFx). */
	public static final int FX_MANIAC = 59, FX_HORSEMAN = 60, FX_HELLGATE = 61;
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	// ====================================================================== TERROR MANIAC

	/**
	 * TERROR MANIAC (celestial). The colour drains out of the world and the sky goes the colour of old blood, with
	 * red lightning crawling through the clouds. A heartbeat starts under the ground, and black veins crawl out from
	 * a crack in the earth, pulsing. Out of the crack rises a thing of roots and sinew, taller than the trees, with a
	 * cracked white mask for a face. It lifts its hands, and everything living lifts with them into the air, twisting,
	 * held there while a bell tolls. Then it closes its fists, and they all break. It shrieks, comes apart into a storm
	 * of black moths, and the land it stood over rots: dead grass, bare trees, wither roses everywhere.
	 */
	public static class TerrorManiac extends SciFiEvents.Base {
		private List<double[]> veins;
		private int placed;
		private final List<LivingEntity> victims = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				CelestialEvents.announce(world, c, "block.armageddon.terror_maniac");
				Vec3d g = Kit.ground(world, c.x, c.z);
				Vfx.newFx(world, FX_MANIAC, g, Kit.dir(yaw()), (float) Maniac.R, 0xFF2010, seed(), 0, Maniac.END);
				Vfx.screen(world, c, 400, 0, 0.55F, Maniac.SHATTER);
				Kit.sound(world, c, SoundEvents.AMBIENT_CAVE.value(), 64.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSEST, 64.0F, 0.5F);
			}
			if (t % 40 == 0 && t < Maniac.END - 40) {
				Vfx.skyTint(world, c, 100000, 0x1C0204, 0.85F);
			}
			// The heartbeat under the ground, quickening.
			int beat = t < Maniac.CURSE ? 34 : 18;
			if (t % beat == 0 && t < Maniac.SHATTER) {
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 64.0F, 0.5F);
			}
			if (t % 70 == 35 && t < Maniac.SHATTER) {
				Kit.sound(world, c, SoundEvents.AMBIENT_CAVE.value(), 64.0F, 0.4F + world.random.nextFloat() * 0.3F);
			}
			if (t >= Maniac.VEINS && t <= Maniac.VEINS_END) {
				crawl(c, Maniac.reach(t));
			}
			if (t == Maniac.RISE) {
				Vfx.play(world, VfxIds.MANIAC_RIFT, Kit.ground(world, c.x, c.z), 1.0F);
				Vfx.play(world, VfxIds.MANIAC_GEYSER, Kit.ground(world, c.x, c.z), 1.0F);
				Kit.sound(world, c, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 96.0F, 0.4F);
				Kit.sound(world, c, SoundEvents.ENTITY_SLIME_SQUISH, 96.0F, 0.3F);
				splat(Kit.ground(world, c.x, c.z), 9);
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_EMERGE, 96.0F, 0.4F);
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_DIG, 96.0F, 0.4F);
				Vfx.shake(world, c, 300, 1.2F);
				if (Destruction.enabled(world)) {
					Kit.fissure(world, c.subtract(Kit.dir(yaw()).multiply(9)), Kit.dir(yaw()), 18, 8, 4, Blocks.MAGMA_BLOCK.getDefaultState());
				}
			}
			if (t > Maniac.RISE && t < Maniac.RISEN && t % 10 == 0) {
				Vfx.shake(world, c, 300, 0.6F);
				Kit.sound(world, c, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 64.0F, 0.4F);
			}
			// Blood rain: spatter falling on the land while the curse lasts.
			if (t >= Maniac.CURSE && t < Maniac.SHATTER + 60 && t % 4 == 0 && Destruction.enabled(world)) {
				double a = world.random.nextDouble() * MathHelper.TAU, d = Math.sqrt(world.random.nextDouble()) * Maniac.R;
				splat(Kit.ground(world, c.x + Math.cos(a) * d, c.z + Math.sin(a) * d), 1 + world.random.nextInt(2));
			}
			if (t >= Maniac.CURSE && t < Maniac.SHATTER && t % 50 == 25) {
				Kit.sound(world, c, SoundEvents.ENTITY_GHAST_SCREAM, 64.0F, 0.35F + world.random.nextFloat() * 0.2F);
			}
			if (t == Maniac.CURSE) {
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_ROAR, 96.0F, 0.4F);
				Kit.sound(world, c, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 96.0F, 0.5F);
			}
			if (t >= Maniac.CURSE && t < Maniac.SNAP) {
				curse(c, t);
			}
			if (t == Maniac.SNAP) {
				snap();
			}
			if (t == Maniac.SHRIEK) {
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_ROAR, 128.0F, 0.6F);
				Kit.sound(world, c, SoundEvents.ENTITY_GHAST_SCREAM, 128.0F, 0.5F);
				Vfx.screen(world, c, 400, 3, 0.6F, 40);
				Vfx.shake(world, c, 400, 1.6F);
			}
			if (t == Maniac.SHATTER) {
				Vec3d chest = Kit.ground(world, c.x, c.z).add(0, Maniac.TALL * 0.55, 0);
				Vfx.play(world, VfxIds.MANIAC_SHATTER, chest, 1.0F);
				Vfx.flash(world, chest, 0.3F, 400, 0xFF2010);
				Vfx.shake(world, c, 400, 1.4F);
				Kit.sound(world, chest, SoundEvents.ENTITY_PHANTOM_DEATH, 128.0F, 0.4F);
				Kit.sound(world, chest, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
				Destruction.explode(world, owner(), c.x, Kit.ground(world, c.x, c.z).y, c.z, 9.0F, false);
				DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, c, Maniac.ROT, this::rot), 1.6));
			}
			stepJobs(5000);
			return t >= Maniac.END && jobs.isEmpty();
		}

		private int seed() {
			return data.getInt("Seed");
		}

		/** The veins crawl: each point they reach is turned to wart and crimson, with a glowing node now and then. */
		private void crawl(Vec3d c, double reach) {
			if (veins == null) {
				veins = Maniac.veins(seed());
				veins.sort((a, b) -> Double.compare(a[2], b[2]));
			}
			boolean on = Destruction.enabled(world);
			int n = 0;
			for (; placed < veins.size() && veins.get(placed)[2] <= reach; placed++) {
				double[] v = veins.get(placed);
				if (!on) {
					continue;
				}
				int r = MathHelper.ceil(v[3]);
				for (int ox = -r; ox <= r; ox++) {
					for (int oz = -r; oz <= r; oz++) {
						double d = Math.hypot(ox, oz);
						if (d > v[3]) {
							continue;
						}
						BlockPos s = Kit.surface(world, MathHelper.floor(c.x + v[0]) + ox, MathHelper.floor(c.z + v[1]) + oz);
						boolean node = d < 0.6 && ((int) v[2]) % 13 == 0;
						put(s, (node ? Blocks.SHROOMLIGHT : d < v[3] * 0.55 ? Blocks.NETHER_WART_BLOCK : Blocks.CRIMSON_NYLIUM).getDefaultState());
						BlockState above = world.getBlockState(s.up());
						if (!above.isAir() && (above.isReplaceable() || above.isIn(BlockTags.FLOWERS))) {
							put(s.up(), AIR);
						}
					}
				}
				if (n++ % 12 == 0) {
					Kit.sound(world, c.add(v[0], 0, v[1]), SoundEvents.BLOCK_SCULK_SPREAD, 24.0F, 0.5F);
				}
			}
		}

		/** Everything living is lifted into the air and held there, twisting, blind, while a bell tolls. */
		private void curse(Vec3d c, int t) {
			if (t % 10 == 0) {
				for (LivingEntity e : Kit.living(world, c, Maniac.R)) {
					if (!Kit.creativeOrSpectator(e) && !victims.contains(e)) {
						victims.add(e);
						Vfx.play(world, VfxIds.MANIAC_CURSE, e.getPos(), Math.max(0.6F, e.getHeight() * 0.6F));
					}
				}
			}
			if (t % 40 == 0) {
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_USE, 128.0F, 0.45F);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 96.0F, 0.5F);
			}
			if (t % 55 == 20) {
				Vfx.screen(world, c, 400, 3, 0.25F + world.random.nextFloat() * 0.2F, 3);
			}
			victims.removeIf(e -> !e.isAlive() || e.isRemoved());
			for (LivingEntity e : victims) {
				double lift = 7 + HorrorShapes.hash(e.getId(), 3) * 7;
				double floor = Kit.ground(world, e.getX(), e.getZ()).y;
				double want = floor + lift;
				double vy = MathHelper.clamp((want - e.getY()) * 0.08, -0.2, 0.3);
				double jx = 0, jz = 0;
				if (world.random.nextFloat() < 0.08F) {
					// A twitch.
					jx = (world.random.nextDouble() - 0.5) * 0.5;
					jz = (world.random.nextDouble() - 0.5) * 0.5;
					vy += 0.15;
				}
				Kit.velocity(e, new Vec3d(e.getVelocity().x * 0.6 + jx, vy, e.getVelocity().z * 0.6 + jz));
				e.fallDistance = 0;
				if (t % 20 == 0) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0, false, false));
					Vfx.play(world, VfxIds.MANIAC_CURSE, e.getPos(), Math.max(0.6F, e.getHeight() * 0.6F));
				}
				if (t % 6 == 0) {
					// They bleed, dripping onto the ground under them.
					Vfx.play(world, VfxIds.MANIAC_BLEED, e.getPos().add(0, e.getHeight() * 0.5, 0), Math.max(0.6F, e.getHeight() * 0.5F));
				}
				if (t % 40 == 0) {
					e.damage(world.getDamageSources().magic(), 1.0F);
					Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SKELETON_HURT, 16.0F, 0.4F);
					Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SLIME_SQUISH_SMALL, 16.0F, 0.5F);
					if (Destruction.enabled(world)) {
						splat(Kit.ground(world, e.getX(), e.getZ()), 1);
					}
				}
			}
		}

		/** The fists close: everything held is broken and thrown down. */
		private void snap() {
			Vec3d c = center();
			Kit.sound(world, c, SoundEvents.ENTITY_PLAYER_BIG_FALL, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 128.0F, 0.4F);
			Vfx.screen(world, c, 400, 4, 0.8F, 3);
			Vfx.flash(world, c, 0.5F, 400, 0xB00000);
			Vfx.shake(world, c, 400, 1.2F);
			Kit.sound(world, c, SoundEvents.ENTITY_SLIME_SQUISH, 128.0F, 0.3F);
			for (LivingEntity e : victims) {
				if (!e.isAlive()) {
					continue;
				}
				Vfx.play(world, VfxIds.MANIAC_SNAP, e.getPos().add(0, e.getHeight() * 0.5, 0), Math.max(0.6F, e.getHeight() * 0.6F));
				Vfx.play(world, VfxIds.MANIAC_GORE, e.getPos().add(0, e.getHeight() * 0.5, 0), Math.max(0.8F, e.getHeight() * 0.7F));
				Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SKELETON_HURT, 32.0F, 0.3F);
				Kit.sound(world, e.getPos(), SoundEvents.BLOCK_HONEY_BLOCK_BREAK, 32.0F, 0.5F);
				if (Destruction.enabled(world)) {
					splat(Kit.ground(world, e.getX(), e.getZ()), 3);
				}
				Kit.velocity(e, new Vec3d(0, -2.2, 0));
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 34.0F);
			}
			victims.clear();
		}

		/** A splash of blood on the ground: an irregular pool of reds, darkest at its heart. */
		private void splat(Vec3d at, int r) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.hypot(dx, dz);
					double rag = HorrorShapes.hash(MathHelper.floor(at.x) + dx * 7, MathHelper.floor(at.z) + dz * 13);
					if (d > r + 0.3 - rag * 0.9) {
						continue;
					}
					BlockPos s = Kit.surface(world, MathHelper.floor(at.x) + dx, MathHelper.floor(at.z) + dz);
					BlockState g = world.getBlockState(s);
					if (!g.isOpaqueFullCube(world, s) || g.hasBlockEntity()) {
						continue;
					}
					put(s, (d < r * 0.45 ? Blocks.RED_CONCRETE : rag < 0.5 ? Blocks.NETHER_WART_BLOCK : Blocks.RED_TERRACOTTA).getDefaultState());
				}
			}
		}

		/** The rot: grass dies to podzol and mud, leaves fall away, flowers turn to wither roses. */
		private void rot(TerrainJob.Column col) {
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 30); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isIn(BlockTags.LEAVES) && col.noise(y) < 0.85) {
					col.set(y, AIR);
				} else if (s.isIn(BlockTags.FLOWERS) || s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.FERN)) {
					col.set(y, col.noise(y + 3) < 0.5 ? Blocks.WITHER_ROSE.getDefaultState() : AIR);
				}
			}
			BlockState g = col.get(surf);
			if (g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.DIRT) || g.isOf(Blocks.MOSS_BLOCK)) {
				double n = col.noise(17);
				col.set(surf, (n < 0.45 ? Blocks.PODZOL : n < 0.7 ? Blocks.COARSE_DIRT : n < 0.85 ? Blocks.MUD : Blocks.SOUL_SOIL).getDefaultState());
				if (col.noise(19) < 0.05 && col.get(surf + 1).isAir()) {
					col.set(surf + 1, Blocks.WITHER_ROSE.getDefaultState());
				}
			}
		}
	}

	// ====================================================================== PUMPKIN HORSEMAN

	/**
	 * PUMPKIN HORSEMAN (celestial). Night falls orange and violet and a low mist creeps over the ground. Ghost fire
	 * bursts out of the earth and out of it gallops a rider the height of a tower on a horse made of night, with a
	 * burning mane, ghost fire pouring from where his head should be, a scythe in one hand and a blazing
	 * jack-o'-lantern in the other. He gallops round the land leaving burning hoofprints and hurls the lantern six
	 * times; each one bursts into a crater ringed with grinning pumpkins, and a new one kindles in his hand. Then he
	 * rears, charges straight through the middle cutting a burning trench with his scythe, and rides up into the sky
	 * and is gone, leaving the land hallowed: pumpkins, cobwebs, dead bushes and soul fire everywhere.
	 */
	public static class PumpkinHorseman extends SciFiEvents.Base {
		private boolean trenched;
		private double rot;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t > 0 && rot == 0 && data.contains("Yaw")) {
				Vec3d heading = Kit.dir(yaw());
				rot = Math.atan2(heading.z, heading.x);
			}
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				CelestialEvents.announce(world, c, "block.armageddon.pumpkin_horseman");
				Vec3d heading = Kit.dir(yaw());
				rot = Math.atan2(heading.z, heading.x);
				Vfx.newFx(world, FX_HORSEMAN, Kit.ground(world, c.x, c.z), heading, (float) Horseman.ORBIT, 0xFF8020, seed(), 0, Horseman.END);
				Kit.sound(world, c, SoundEvents.ENTITY_WITCH_CELEBRATE, 64.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 64.0F, 0.6F);
			}
			if (t % 40 == 0 && t < Horseman.END - 40) {
				Vfx.skyTint(world, c, 100000, 0x1E0A24, 0.75F);
			}
			if (t == Horseman.APPEAR) {
				Vfx.play(world, VfxIds.HORSE_APPEAR, Kit.ground(world, c.x, c.z), 1.0F);
				Vfx.flash(world, c, 0.3F, 300, 0xFF9030);
				Vfx.shake(world, c, 300, 1.0F);
				Kit.sound(world, c, SoundEvents.ENTITY_HORSE_ANGRY, 128.0F, 0.4F);
				Kit.sound(world, c, SoundEvents.ENTITY_BLAZE_SHOOT, 96.0F, 0.4F);
			}
			if (t >= Horseman.APPEAR && t < Horseman.VANISH) {
				gallop(c, t);
			}
			for (int k = 0; k < Horseman.THROWS; k++) {
				int at = Horseman.throwAt(k);
				if (t == at) {
					Vec3d hand = Kit.ground(world, c.x + Horseman.pos(t, rot).x, c.z + Horseman.pos(t, rot).z).add(0, Horseman.HAND, 0);
					Kit.sound(world, hand, SoundEvents.ENTITY_WITCH_CELEBRATE, 96.0F, 0.45F);
					Kit.sound(world, hand, SoundEvents.ITEM_FIRECHARGE_USE, 96.0F, 0.5F);
				}
				if (t == at + Horseman.FLIGHT) {
					pumpkin(c, k);
				}
			}
			if (t == Horseman.REAR) {
				Vec3d p = Kit.ground(world, c.x + Horseman.pos(t, rot).x, c.z + Horseman.pos(t, rot).z);
				Kit.sound(world, p, SoundEvents.ENTITY_HORSE_DEATH, 128.0F, 0.4F);
				Kit.sound(world, p, SoundEvents.ENTITY_HORSE_ANGRY, 128.0F, 0.35F);
				Vfx.shake(world, p, 300, 1.0F);
			}
			if (t >= Horseman.CHARGE && t < Horseman.ASCEND) {
				charge(c, t);
			}
			if (t == Horseman.VANISH) {
				Vec3d p = c.add(Horseman.pos(t, rot)).add(0, Kit.ground(world, c.x, c.z).y - c.y + 20, 0);
				Vfx.play(world, VfxIds.HORSE_VANISH, p, 1.0F);
				Vfx.flash(world, p, 0.35F, 400, 0xFF9030);
				Kit.sound(world, p, SoundEvents.ENTITY_WITCH_CELEBRATE, 128.0F, 0.4F);
				Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
				DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, c, Horseman.HALLOW, this::hallow), 1.8));
			}
			stepJobs(5000);
			return t >= Horseman.END && jobs.isEmpty();
		}

		private int seed() {
			return data.getInt("Seed");
		}

		/** The gallop: thundering hooves, burning hoofprints, and anything in the way trampled. */
		private void gallop(Vec3d c, int t) {
			if (t < Horseman.GALLOP && t % 6 != 0) {
				return;
			}
			Vec3d p = Horseman.pos(t, rot);
			if (p.y > 2) {
				return;
			}
			Vec3d g = Kit.ground(world, c.x + p.x, c.z + p.z);
			if (t % 7 == 0) {
				Kit.sound(world, g, SoundEvents.ENTITY_HORSE_GALLOP, 96.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 24.0F, 1.6F);
				Vfx.shake(world, g, 80, 0.5F);
			}
			if (t % 4 == 0) {
				Vec3d d = Horseman.dir(t, rot), side = new Vec3d(-d.z, 0, d.x);
				int hoof = (t / 4) % 4;
				Vec3d h = g.add(d.multiply((hoof < 2 ? 5 : -5.5) * Horseman.BIG)).add(side.multiply((hoof % 2 == 0 ? 2.4 : -2.4) * Horseman.BIG));
				Vfx.play(world, VfxIds.HORSE_HOOF, Kit.ground(world, h.x, h.z), 1.0F);
				if (Destruction.enabled(world)) {
					BlockPos s = Kit.surface(world, MathHelper.floor(h.x), MathHelper.floor(h.z));
					for (int ox = -1; ox <= 1; ox++) {
						for (int oz = -1; oz <= 1; oz++) {
							put(s.add(ox, 0, oz), (Math.abs(ox) + Math.abs(oz) < 2 ? Blocks.SOUL_SOIL : Blocks.COARSE_DIRT).getDefaultState());
						}
					}
					if (world.getBlockState(s.up()).isAir() || world.getBlockState(s.up()).isReplaceable()) {
						put(s.up(), Blocks.SOUL_FIRE.getDefaultState());
					}
				}
			}
			if (t % 5 == 0) {
				for (LivingEntity e : Kit.living(world, g, 12)) {
					if (!Kit.creativeOrSpectator(e)) {
						Vec3d away = e.getPos().subtract(g).normalize();
						Kit.push(e, away.multiply(1.4).add(0, 0.8, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 14.0F);
					}
				}
			}
		}

		/** A lantern lands: it bursts into a burning crater ringed with grinning pumpkins, and bats scatter out of it. */
		private void pumpkin(Vec3d c, int k) {
			Vec3d tg = Horseman.target(k, seed(), rot);
			Vec3d hit = Kit.ground(world, c.x + tg.x, c.z + tg.z);
			Vfx.play(world, VfxIds.HORSE_PUMPKIN, hit, 1.0F);
			Vfx.flash(world, hit, 0.3F, 300, 0xFF9030);
			Vfx.shake(world, hit, 200, 1.2F);
			Kit.sound(world, hit, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.6F);
			Kit.sound(world, hit, SoundEvents.ENTITY_WITCH_CELEBRATE, 64.0F, 0.6F);
			for (LivingEntity e : Kit.living(world, hit, 12)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.setOnFireFor(8);
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 30.0F);
				}
			}
			Destruction.explode(world, owner(), hit.x, hit.y, hit.z, 6.5F, true);
			DetonationManager.schedule(world, 3, () -> {
				if (!Destruction.enabled(world)) {
					return;
				}
				for (int i = 0; i < 10; i++) {
					double a = i * MathHelper.TAU / 10;
					BlockPos s = Kit.surface(world, MathHelper.floor(hit.x + Math.cos(a) * 8), MathHelper.floor(hit.z + Math.sin(a) * 8));
					BlockState lantern = (i % 2 == 0 ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN).getDefaultState()
							.with(CarvedPumpkinBlock.FACING, net.minecraft.util.math.Direction.fromHorizontal((int) Math.floorMod(Math.round(a / (Math.PI / 2)) + 1, 4)));
					if (world.getBlockState(s.up()).isAir() || world.getBlockState(s.up()).isReplaceable()) {
						put(s.up(), lantern);
					}
				}
				for (int i = 0; i < 3; i++) {
					var bat = EntityType.BAT.create(world);
					if (bat != null) {
						bat.refreshPositionAndAngles(hit.x, hit.y + 3, hit.z, 0, 0);
						world.spawnEntity(bat);
					}
				}
			});
		}

		/** The charge: a burning trench cut straight through the middle, and everything near it cut down. */
		private void charge(Vec3d c, int t) {
			Vec3d p = Horseman.pos(t, rot), d = Horseman.dir(t, rot);
			Vec3d g = Kit.ground(world, c.x + p.x, c.z + p.z);
			if (t == Horseman.CHARGE) {
				Kit.sound(world, g, SoundEvents.ENTITY_HORSE_ANGRY, 128.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ITEM_TRIDENT_RIPTIDE_3.value(), 128.0F, 0.5F);
				if (Destruction.enabled(world) && !trenched) {
					trenched = true;
					Vec3d from = Kit.ground(world, c.x + p.x, c.z + p.z);
					Kit.fissure(world, from, d, (int) (Horseman.ORBIT * 2), 6, 5, Blocks.SOUL_SOIL.getDefaultState());
				}
			}
			if (t % 3 == 0) {
				Vfx.play(world, VfxIds.HORSE_SLASH, g, d, 1.0F, 0);
				Kit.sound(world, g, SoundEvents.ENTITY_HORSE_GALLOP, 128.0F, 0.5F);
				Vfx.shake(world, g, 120, 0.9F);
				for (LivingEntity e : Kit.living(world, g, 12)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.push(e, d.multiply(1.8).add(0, 1.0, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 45.0F);
					}
				}
			}
		}

		/** What he leaves behind: pumpkins on the grass, cobwebs in the trees, dead bushes, soul fire here and there. */
		private void hallow(TerrainJob.Column col) {
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 24); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isIn(BlockTags.LEAVES) && col.noise(y) < 0.05 && col.get(y - 1).isAir()) {
					col.set(y - 1, Blocks.COBWEB.getDefaultState());
				}
			}
			BlockState g = col.get(surf);
			BlockState above = col.get(surf + 1);
			if (!(g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.DIRT) || g.isOf(Blocks.PODZOL)) || !(above.isAir() || above.isReplaceable())) {
				return;
			}
			double n = col.noise(23);
			if (n < 0.012) {
				col.set(surf + 1, Blocks.JACK_O_LANTERN.getDefaultState());
			} else if (n < 0.03) {
				col.set(surf + 1, (col.noise(5) < 0.5 ? Blocks.PUMPKIN : Blocks.CARVED_PUMPKIN).getDefaultState());
			} else if (n < 0.07) {
				col.set(surf, Blocks.COARSE_DIRT.getDefaultState());
				col.set(surf + 1, Blocks.DEAD_BUSH.getDefaultState());
			} else if (n < 0.085) {
				col.set(surf, Blocks.SOUL_SOIL.getDefaultState());
				col.set(surf + 1, Blocks.SOUL_FIRE.getDefaultState());
			} else if (n < 0.3) {
				col.set(surf, Blocks.PODZOL.getDefaultState());
			}
		}
	}

	// ====================================================================== PORTAL OF HELL

	/**
	 * PORTAL OF HELL (celestial). The ground quakes and a gothic gate of blackstone the height of a cathedral heaves
	 * itself up out of the earth, facing whoever lit it: spiked, veined with magma, rimmed with crying obsidian. Its
	 * arch ignites into a whirling vortex of hellfire and an eye opens in it, and watches. Souls pour out, fireballs
	 * rain on the land, lava falls from its sill and things come through. Then a clawed arm the size of a tower
	 * reaches out of it, raises its fist and slams the ground. The eye closes, the portal implodes, and the gate is
	 * left standing, cracked, over a scorched hellscape.
	 */
	public static class PortalOfHell extends SciFiEvents.Base {
		private Vec3d base, f, side;
		private final List<int[]> frame = new ArrayList<>();
		private int placed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				base = Kit.ground(world, c.x, c.z);
				f = Kit.dir(yaw()).negate();
				side = new Vec3d(-f.z, 0, f.x);
				CelestialEvents.announce(world, c, "block.armageddon.portal_of_hell");
				Vfx.newFx(world, FX_HELLGATE, base, f, (float) HellGate.W, 0xFF4010, seed(), 0, HellGate.END);
				Kit.sound(world, c, SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), 96.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_DIG, 96.0F, 0.4F);
				plan();
			}
			if (t % 40 == 0 && t < HellGate.END - 40) {
				Vfx.skyTint(world, c, 100000, 0x2A0402, 0.8F);
			}
			if (t < HellGate.RISEN && t % 8 == 0) {
				Vfx.shake(world, base, 300, 0.4F + t * 0.004F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_DIG, 64.0F, 0.4F);
			}
			if (t >= HellGate.RISE && t <= HellGate.RISEN) {
				build(t);
			}
			if (t == HellGate.OPEN) {
				Vec3d eye = eye();
				Vfx.play(world, VfxIds.HELLGATE_OPEN, eye, f, 1.0F, 0);
				Vfx.flash(world, eye, 0.35F, 400, 0xFF5010);
				Vfx.shake(world, eye, 400, 1.2F);
				Kit.sound(world, eye, SoundEvents.BLOCK_END_PORTAL_SPAWN, 128.0F, 0.5F);
				Kit.sound(world, eye, SoundEvents.ENTITY_BLAZE_SHOOT, 128.0F, 0.4F);
			}
			if (t == HellGate.EYE) {
				Kit.sound(world, eye(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 128.0F, 0.4F);
			}
			if (t > HellGate.OPEN && t < HellGate.IMPLODE) {
				if (t % 6 == 0) {
					Vfx.play(world, VfxIds.HELLGATE_SOULS, eye(), f, 1.0F, 0);
				}
				if (t % 40 == 0) {
					Kit.sound(world, eye(), SoundEvents.ENTITY_GHAST_AMBIENT, 128.0F, 0.4F);
					Kit.sound(world, eye(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 96.0F, 0.5F);
				}
			}
			if (t == HellGate.LAVA) {
				lavafall();
			}
			for (int w = 0; w < HellGate.WAVES; w++) {
				if (t == HellGate.waveAt(w)) {
					horde(w);
				}
			}
			if (t == HellGate.ARM) {
				Kit.sound(world, eye(), SoundEvents.ENTITY_WARDEN_EMERGE, 128.0F, 0.3F);
			}
			if (t == HellGate.REACH) {
				Kit.sound(world, eye(), SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.3F);
				Vfx.shake(world, eye(), 300, 0.8F);
			}
			if (t == HellGate.SLAM) {
				slam();
			}
			if (t > HellGate.SLAM && t < HellGate.ARM_END + 40) {
				drag(t);
			}
			if (t == HellGate.CLOSE) {
				Kit.sound(world, eye(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 128.0F, 0.6F);
			}
			if (t == HellGate.IMPLODE) {
				implode();
			}
			stepJobs(5000);
			return t >= HellGate.END && jobs.isEmpty();
		}

		private int seed() {
			return data.getInt("Seed");
		}

		private Vec3d local(double u, double v, double w) {
			return base.add(side.multiply(u)).add(0, v, 0).add(f.multiply(w));
		}

		private Vec3d eye() {
			return local(0, HellGate.EYE_Y, 0);
		}

		/** Every block of the gate, its spikes and its steps, ordered bottom up, with what each is made of. */
		private void plan() {
			java.util.Map<Long, int[]> m = new java.util.LinkedHashMap<>();
			double reach = HellGate.W + HellGate.T;
			for (double u = -reach; u <= reach; u += 0.5) {
				for (double v = -1; v <= HellGate.APEX + HellGate.T; v += 0.5) {
					if (!HellGate.frame(u, v) && v >= 0) {
						continue;
					}
					if (v < 0 && Math.abs(u) > reach + 3) {
						continue;
					}
					for (double w = -HellGate.DEPTH; w <= HellGate.DEPTH; w += 0.5) {
						double h = HorrorShapes.hash((int) (u * 2) * 31 + (int) (w * 2), (int) (v * 2));
						int kind = v < 0 ? 0 : HellGate.inside(u, v, 1.2) ? 3 : h < 0.07 ? 4 : h < 0.1 ? 5 : Math.abs(w) > HellGate.DEPTH - 0.6 && h < 0.4 ? 1 : 0;
						add(m, local(u, v, w), kind);
					}
				}
			}
			// Steps of blackstone in front.
			for (double u = -reach - 3; u <= reach + 3; u += 0.5) {
				for (double w = HellGate.DEPTH; w <= HellGate.DEPTH + 6; w += 0.5) {
					add(m, local(u, -1, w), 0);
					if (w < HellGate.DEPTH + 3) {
						add(m, local(u, 0, w), 1);
					}
				}
			}
			// Spikes along the outside of the arch.
			double ro = 2 * HellGate.W + HellGate.T;
			for (int sgn = -1; sgn <= 1; sgn += 2) {
				for (double v = 5; v <= HellGate.SIDES; v += 6) {
					spike(m, sgn * reach, v, sgn, 0.25, 5);
				}
				for (double th = 0.12; th < Math.PI / 3 - 0.05; th += 0.16) {
					double u = -HellGate.W + ro * Math.cos(th), v = HellGate.SIDES + ro * Math.sin(th);
					spike(m, sgn * u, v, sgn * Math.cos(th), Math.sin(th), 5 + (th > 0.9 ? 2 : 0));
				}
			}
			spike(m, 0, HellGate.APEX + HellGate.T - 0.5, 0, 1, 10);
			frame.addAll(m.values());
			frame.sort((a, b) -> Integer.compare(a[1], b[1]));
		}

		private void spike(java.util.Map<Long, int[]> m, double u, double v, double nu, double nv, double len) {
			double n = Math.hypot(nu, nv);
			for (double s = 0; s <= len; s += 0.5) {
				int kind = s > len - 1.5 ? 3 : 2;
				add(m, local(u + nu / n * s, v + nv / n * s, 0), kind);
				if (s < len * 0.4) {
					add(m, local(u + nu / n * s, v + nv / n * s, 1), kind);
					add(m, local(u + nu / n * s, v + nv / n * s, -1), kind);
				}
			}
		}

		private static void add(java.util.Map<Long, int[]> m, Vec3d p, int kind) {
			int x = MathHelper.floor(p.x), y = MathHelper.floor(p.y), z = MathHelper.floor(p.z);
			m.put(BlockPos.asLong(x, y, z), new int[]{x, y, z, kind});
		}

		private static BlockState mat(int k) {
			return (switch (k) {
				case 1 -> Blocks.CHISELED_POLISHED_BLACKSTONE;
				case 2 -> Blocks.OBSIDIAN;
				case 3 -> Blocks.CRYING_OBSIDIAN;
				case 4 -> Blocks.MAGMA_BLOCK;
				case 5 -> Blocks.GILDED_BLACKSTONE;
				default -> Blocks.POLISHED_BLACKSTONE_BRICKS;
			}).getDefaultState();
		}

		/** The gate heaving up out of the earth, row by row, and the arch cleared for what comes through. */
		private void build(int t) {
			if (!Destruction.enabled(world)) {
				return;
			}
			int to = (int) (HellGate.risen(t + 1) * frame.size());
			for (; placed < Math.min(to, frame.size()); placed++) {
				int[] b = frame.get(placed);
				put(new BlockPos(b[0], b[1], b[2]), mat(b[3]));
			}
			if (t % 4 == 0) {
				int v = (int) (HellGate.risen(t) * (HellGate.APEX + 4));
				Vfx.play(world, VfxIds.HELLGATE_RISE, local(0, 0, 0), 1.0F);
				Kit.sound(world, local(0, v, 0), SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, 64.0F, 0.4F);
			}
			if (t == HellGate.RISEN) {
				for (double u = -HellGate.W + 0.5; u < HellGate.W; u += 1) {
					for (double v = 1; v < HellGate.APEX; v += 1) {
						if (HellGate.opening(u, v)) {
							for (int w = -2; w <= 2; w++) {
								BlockPos p = BlockPos.ofFloored(local(u, v, w));
								if (!world.getBlockState(p).isAir()) {
									put(p, AIR);
								}
							}
						}
					}
				}
			}
		}

		/** Lava spills over the gate's sill and runs out across the ground in front of it. */
		private void lavafall() {
			Kit.sound(world, local(0, 1, 3), SoundEvents.BLOCK_LAVA_AMBIENT, 128.0F, 0.5F);
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int u = -3; u <= 3; u++) {
				for (int w = 3; w <= 9; w += 3) {
					Vec3d p = local(u, 0, w);
					BlockPos g = Kit.surface(world, MathHelper.floor(p.x), MathHelper.floor(p.z)).up();
					put(g, Blocks.LAVA.getDefaultState());
				}
			}
		}

		/**
		 * A wave comes through: ghasts drifting out of the arch, blazes, wither skeletons, magma cubes and zoglins
		 * spilling out of its foot, each with a burst of fire. The waves grow.
		 */
		private void horde(int wave) {
			Vec3d door = local(0, 3, 2);
			Kit.sound(world, door, SoundEvents.BLOCK_PORTAL_TRAVEL, 128.0F, 0.5F);
			Kit.sound(world, door, SoundEvents.ENTITY_GHAST_WARN, 128.0F, 0.5F);
			Vfx.play(world, VfxIds.HELLGATE_EMERGE, local(0, HellGate.EYE_Y * 0.6, 1), f, 1.0F, 0);
			List<EntityType<? extends net.minecraft.entity.mob.MobEntity>> types = new ArrayList<>(List.of(EntityType.BLAZE, EntityType.WITHER_SKELETON,
					EntityType.MAGMA_CUBE));
			if (wave % 2 == 0 || wave >= 4) {
				types.add(EntityType.GHAST);
			}
			if (wave >= 2) {
				types.add(EntityType.BLAZE);
				types.add(EntityType.ZOGLIN);
			}
			if (wave >= 5) {
				types.add(EntityType.WITHER_SKELETON);
				types.add(EntityType.MAGMA_CUBE);
			}
			for (int i = 0; i < types.size(); i++) {
				var mob = types.get(i).create(world);
				if (mob == null) {
					continue;
				}
				boolean ghast = types.get(i) == EntityType.GHAST;
				double across = (HorrorShapes.hash(wave * 13 + i, 7) - 0.5) * (HellGate.W * 1.4);
				Vec3d at = local(across, ghast ? HellGate.EYE_Y + (HorrorShapes.hash(wave, i) - 0.5) * 10 : 1.5, ghast ? 4 : 3);
				mob.refreshPositionAndAngles(at.x, at.y, at.z, (float) Math.toDegrees(Math.atan2(-f.x, f.z)), 0);
				mob.initialize(world, world.getLocalDifficulty(BlockPos.ofFloored(at)), net.minecraft.entity.SpawnReason.EVENT, null);
				mob.setVelocity(f.multiply(ghast ? 0.5 : 0.9).add(0, ghast ? 0.1 : 0.35, 0));
				world.spawnEntity(mob);
				Vfx.play(world, VfxIds.HELLGATE_SOULS, at, f, 0.7F, 0);
			}
		}

		private final List<net.minecraft.entity.Entity> dragged = new ArrayList<>();

		/**
		 * The claw comes down open and digs in: a crater, four gouges torn back toward the gate, everything near it
		 * crushed and burning.
		 */
		private void slam() {
			Vec3d p = local(0, 0, HellGate.SLAM_AT);
			Vec3d hit = Kit.ground(world, p.x, p.z);
			Vfx.play(world, VfxIds.HELLGATE_SLAM, hit, 1.0F);
			Vfx.flash(world, hit, 0.35F, 400, 0xFF5010);
			Vfx.shake(world, hit, 400, 2.2F);
			Kit.sound(world, hit, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.3F);
			Kit.sound(world, hit, SoundEvents.ENTITY_RAVAGER_STUNNED, 128.0F, 0.3F);
			Kit.sound(world, hit, SoundEvents.ENTITY_WARDEN_DIG, 128.0F, 0.4F);
			for (LivingEntity e : Kit.living(world, hit, 24)) {
				if (!Kit.creativeOrSpectator(e)) {
					Vec3d away = e.getPos().subtract(hit);
					double d = Math.max(1, away.length());
					double k = 1 - d / 24;
					Kit.push(e, away.normalize().multiply(1.2 * k).add(0, 0.9 * k, 0));
					e.setOnFireFor(6);
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), (float) (40 * k + 8));
				}
			}
			Destruction.explode(world, owner(), hit.x, hit.y, hit.z, 8.0F, true);
			// Four claw gouges dragged back toward the gate.
			if (Destruction.enabled(world)) {
				for (int k = -1; k <= 2; k++) {
					Vec3d from = hit.add(side.multiply((k - 0.5) * 4.5));
					Kit.fissure(world, from, f.negate(), 18, 5, 2, Blocks.MAGMA_BLOCK.getDefaultState());
				}
			}
		}

		/**
		 * The claw closes on the ground it dug into, tears out a great chunk of it, and drags it - and anything
		 * standing on it - back through the portal.
		 */
		private void drag(int t) {
			Vec3d hl = HellGate.hand(t);
			Vec3d hand = local(hl.x, hl.y, hl.z);
			Vec3d door = local(0, HellGate.EYE_Y * 0.6, -2);
			if (t == HellGate.SLAM + 10) {
				Vec3d hit = Kit.ground(world, local(0, 0, HellGate.SLAM_AT).x, local(0, 0, HellGate.SLAM_AT).z);
				Kit.sound(world, hit, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 128.0F, 0.3F);
				Kit.sound(world, hit, SoundEvents.ENTITY_WARDEN_ROAR, 128.0F, 0.5F);
				Vfx.shake(world, hit, 300, 1.6F);
				dragged.addAll(Kit.lift(world, hit.add(0, -3, 0), 11, 320, false, true));
				for (LivingEntity e : Kit.living(world, hit, 12)) {
					if (!Kit.creativeOrSpectator(e)) {
						dragged.add(e);
					}
				}
			}
			dragged.removeIf(e -> e.isRemoved() || !e.isAlive());
			for (net.minecraft.entity.Entity e : dragged) {
				// Clustered round the fist while it grips, then hauled after it into the portal.
				Vec3d goal = t < HellGate.WITHDRAW ? hand.add(0, 4, 0) : door;
				Vec3d to = goal.subtract(e.getPos());
				double d = to.length();
				if (t >= HellGate.WITHDRAW && Math.abs(e.getPos().subtract(base).dotProduct(f)) < 2.5 && d < HellGate.W * 1.6) {
					// Through the portal: gone.
					if (e instanceof LivingEntity l) {
						l.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 10000.0F);
					}
					e.discard();
					continue;
				}
				double speed = t < HellGate.WITHDRAW ? 0.35 : 0.9;
				Vec3d jitter = new Vec3d(world.random.nextGaussian(), world.random.nextGaussian(), world.random.nextGaussian()).multiply(0.05);
				Kit.velocity(e, to.normalize().multiply(Math.min(d * 0.2, speed)).add(jitter));
				e.fallDistance = 0;
			}
			if (t % 10 == 0 && t >= HellGate.WITHDRAW && t < HellGate.ARM_END) {
				Kit.sound(world, hand, SoundEvents.BLOCK_GRAVEL_BREAK, 128.0F, 0.4F);
				Kit.sound(world, door, SoundEvents.BLOCK_PORTAL_TRAVEL, 96.0F, 0.4F);
			}
		}

		/** The portal collapses into itself; the gate cracks; the land round it is scorched. */
		private void implode() {
			Vec3d eye = eye();
			Vfx.play(world, VfxIds.HELLGATE_IMPLODE, eye, f, 1.0F, 0);
			Vfx.flash(world, eye, 0.45F, 400, 0xFFA060);
			Vfx.shake(world, eye, 400, 1.6F);
			Kit.sound(world, eye, SoundEvents.BLOCK_BEACON_DEACTIVATE, 128.0F, 0.4F);
			Kit.sound(world, eye, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int[] b : frame) {
				double h = HorrorShapes.hash(b[0] * 7 + b[2], b[1] * 13);
				BlockPos p = new BlockPos(b[0], b[1], b[2]);
				if (h < 0.12 && b[1] > base.y + 2) {
					put(p, AIR);
				} else if (h < 0.22) {
					put(p, Blocks.MAGMA_BLOCK.getDefaultState());
				}
			}
			DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, base, HellGate.SCORCH, this::scorch), 1.6));
		}

		/** Scorched earth: netherrack and magma, soul sand, fires burning; the gate's own footprint left alone. */
		private void scorch(TerrainJob.Column col) {
			Vec3d rel = new Vec3d(col.x + 0.5 - base.x, 0, col.z + 0.5 - base.z);
			if (Math.abs(rel.dotProduct(side)) <= HellGate.W + HellGate.T + 4 && Math.abs(rel.dotProduct(f)) <= HellGate.DEPTH + 7) {
				return;
			}
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 24); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isReplaceable()) {
					col.set(y, AIR);
				}
			}
			BlockState g = col.get(surf);
			if (!(g.isIn(BlockTags.DIRT) || g.isOf(Blocks.GRASS_BLOCK) || g.isIn(BlockTags.SAND) || g.isOf(Blocks.STONE) || g.isOf(Blocks.GRAVEL))) {
				return;
			}
			double n = col.noise(29);
			double d = Math.sqrt(rel.x * rel.x + rel.z * rel.z) / HellGate.SCORCH;
			if (n > 0.85 - d * 0.5) {
				return;
			}
			col.set(surf, (n < 0.06 ? Blocks.MAGMA_BLOCK : n < 0.16 ? Blocks.SOUL_SAND : n < 0.2 ? Blocks.BLACKSTONE : Blocks.NETHERRACK).getDefaultState());
			if (col.noise(31) < 0.06 && col.get(surf + 1).isAir()) {
				col.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		}
	}
}
