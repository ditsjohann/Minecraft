package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.event.Ruin;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.HorrorShapes;
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
				if (t % 40 == 0) {
					e.damage(world.getDamageSources().magic(), 1.0F);
					Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SKELETON_HURT, 16.0F, 0.4F);
				}
			}
		}

		/** The fists close: everything held is broken and thrown down. */
		private void snap() {
			Vec3d c = center();
			Kit.sound(world, c, SoundEvents.ENTITY_PLAYER_BIG_FALL, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 128.0F, 0.4F);
			Vfx.screen(world, c, 400, 4, 0.8F, 3);
			Vfx.shake(world, c, 400, 1.2F);
			for (LivingEntity e : victims) {
				if (!e.isAlive()) {
					continue;
				}
				Vfx.play(world, VfxIds.MANIAC_SNAP, e.getPos().add(0, e.getHeight() * 0.5, 0), Math.max(0.6F, e.getHeight() * 0.6F));
				Kit.sound(world, e.getPos(), SoundEvents.ENTITY_SKELETON_HURT, 32.0F, 0.3F);
				Kit.velocity(e, new Vec3d(0, -2.2, 0));
				e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 34.0F);
			}
			victims.clear();
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

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				CelestialEvents.announce(world, c, "block.armageddon.pumpkin_horseman");
				Vfx.newFx(world, FX_HORSEMAN, Kit.ground(world, c.x, c.z), c, (float) Horseman.ORBIT, 0xFF8020, seed(), 0, Horseman.END);
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
					Vec3d hand = Kit.ground(world, c.x + Horseman.pos(t).x, c.z + Horseman.pos(t).z).add(0, Horseman.HAND, 0);
					Kit.sound(world, hand, SoundEvents.ENTITY_WITCH_CELEBRATE, 96.0F, 0.45F);
					Kit.sound(world, hand, SoundEvents.ITEM_FIRECHARGE_USE, 96.0F, 0.5F);
				}
				if (t == at + Horseman.FLIGHT) {
					pumpkin(c, k);
				}
			}
			if (t == Horseman.REAR) {
				Vec3d p = Kit.ground(world, c.x + Horseman.pos(t).x, c.z + Horseman.pos(t).z);
				Kit.sound(world, p, SoundEvents.ENTITY_HORSE_DEATH, 128.0F, 0.4F);
				Kit.sound(world, p, SoundEvents.ENTITY_HORSE_ANGRY, 128.0F, 0.35F);
				Vfx.shake(world, p, 300, 1.0F);
			}
			if (t >= Horseman.CHARGE && t < Horseman.ASCEND) {
				charge(c, t);
			}
			if (t == Horseman.VANISH) {
				Vec3d p = c.add(Horseman.pos(t)).add(0, Kit.ground(world, c.x, c.z).y - c.y + 20, 0);
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
			Vec3d p = Horseman.pos(t);
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
				Vec3d d = Horseman.dir(t), side = new Vec3d(-d.z, 0, d.x);
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
			Vec3d tg = Horseman.target(k, seed());
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
			Vec3d p = Horseman.pos(t), d = Horseman.dir(t);
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
}
