package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.event.Ruin;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.HorrorShapes;
import com.armageddon.util.HorrorShapes.Maniac;
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
}
