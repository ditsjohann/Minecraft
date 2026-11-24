package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.Kit;
import com.armageddon.event.events.StrongBombEvents.RingSweep;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.MainStageSet;
import com.armageddon.util.NewBombShapes;
import com.armageddon.util.NewBombShapes.Geode;
import com.armageddon.util.NewBombShapes.Lamp;
import com.armageddon.util.NewBombShapes.Prism;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ten oddities: a little weaker than the strong bombs, each with an effect of its own. SONAR, 8-BIT, MAGNETAR,
 * KA-BOOM (comic ink), AURORA, CENTURY (a time-lapse), PRISM, GEODE, FOSSIL and LAVA LAMP. Several are seen
 * through the depth-aware vision pass (sonar, pixels, comic ink); the rest bring their own client effects.
 */
public final class OddBombEvents {
	public static final int FX_MAGNETAR = 10, FX_COMIC = 11, FX_AURORA = 12, FX_PRISM = 13, FX_GEODE = 14, FX_FOSSIL = 15, FX_LAMP = 16, FX_PIXELS = 17;

	private OddBombEvents() {
	}

	abstract static class Base extends StrongBombEvents.Base {
		void name(String id, Formatting color) {
			Vfx.warning(world, center(), 1);
		}

		/** Every column's top solid block. */
		BlockPos surface(int x, int z) {
			return top(x, z);
		}
	}

	// ====================================================================== SONAR

	/**
	 * The world goes green and black: an echo rolls out every second, lighting every edge it passes. The rings are
	 * physical: they knock things back and shatter glass, ice and leaves. Then every echo turns and rushes back in,
	 * and the bomb answers with one sonic boom.
	 */
	public static class Sonar extends Base {
		static final float SPEED = 1.6F, SPACING = 28, RANGE = 120;
		static final int CONVERGE = 300, BOOM = 342, END = 380;
		private final Map<Integer, Integer> lastRing = new HashMap<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("sonar_bomb", Formatting.GREEN);
				Vfx.vision(world, g, 4, RANGE, SPEED, SPACING, 0, 0, 0xFFFFFF, CONVERGE + 10, RANGE + 80);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 32.0F, 0.8F);
			}
			if (t < CONVERGE) {
				double front = t * SPEED;
				if (t % 18 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 48.0F, 0.5F);
					Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F, 0.5F);
				}
				rings(g, front);
				if (front < RANGE && Destruction.enabled(world)) {
					shatter(g, front);
					if (t % 3 == 0) {
						// Each echo is a pressure wave you can see in the ground: it heaves and throws the surface.
						com.armageddon.event.Climax.groundWave(world, g, front, 2.5, 8, 0.45);
					}
				}
			}
			if (t == CONVERGE) {
				Vfx.vision(world, g, 4, RANGE, -RANGE / (BOOM - CONVERGE), SPACING * 0.5F, 0, RANGE, 0xFFFFFF, BOOM - CONVERGE + 30, RANGE + 80);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.6F);
				for (LivingEntity e : Kit.living(world, g, RANGE)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.push(e, g.subtract(e.getPos()).normalize().multiply(0.6).add(0, 0.2, 0));
					}
				}
			}
			if (t == BOOM) {
				// The converged boom: not fire, sound. A circle of land around the bomb is shaken to pieces and everything
				// brittle for a hundred blocks bursts at once.
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.7F);
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.5F);
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.SONAR_BOOM, g, 1.0F);
				Vfx.shake(world, g, 400, 2.0F);
				Detonations.blast(world, g, 40, 22.0F, ModDamageTypes.ANNIHILATION, owner(), 3.0, 1.0);
				for (int k = 0; k < 8; k++) {
					double a = k * MathHelper.TAU / 8;
					com.armageddon.event.Ruin.cone(world, g, new Vec3d(Math.cos(a), 0, Math.sin(a)), 34, 24, 4, 2.2);
				}
				for (double r = 6; r < RANGE; r += 6) {
					shatter(g, r);
				}
			}
			drain();
			return t >= END;
		}

		/** Every echo ring that sweeps over a living thing shoves it and stings it, once per ring. */
		private void rings(Vec3d g, double front) {
			for (LivingEntity e : Kit.living(world, g, Math.min(front, RANGE))) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				double d = e.getPos().distanceTo(g);
				int ring = (int) Math.floor((front - d) / SPACING);
				Integer last = lastRing.get(e.getId());
				if (last != null && last == ring) {
					continue;
				}
				lastRing.put(e.getId(), ring);
				Vec3d out = e.getPos().subtract(g).multiply(1, 0, 1);
				out = out.lengthSquared() < 0.01 ? new Vec3d(1, 0, 0) : out.normalize();
				Kit.push(e, out.multiply(0.7).add(0, 0.35, 0));
				e.damage(world.getDamageSources().sonicBoom(host), 3.0F);
			}
		}

		/** Glass, ice and leaves on the leading ring shatter. */
		private void shatter(Vec3d g, double front) {
			for (int i = 0; i < 90; i++) {
				double a = world.random.nextDouble() * MathHelper.TAU;
				int x = MathHelper.floor(g.x + Math.cos(a) * front), z = MathHelper.floor(g.z + Math.sin(a) * front);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				BlockPos top = surface(x, z);
				for (int y = 0; y < 18; y++) {
					BlockPos p = top.up(14 - y);
					BlockState s = world.getBlockState(p);
					if (s.isIn(BlockTags.LEAVES) || s.isOf(Blocks.GLASS) || s.isOf(Blocks.GLASS_PANE) || s.isIn(BlockTags.IMPERMEABLE)
							|| s.isOf(Blocks.ICE) || s.isOf(Blocks.PACKED_ICE) || s.getBlock() instanceof net.minecraft.block.StainedGlassPaneBlock) {
						if (world.random.nextInt(3) == 0) {
							com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.SONAR_SHATTER, Vec3d.ofCenter(p), 1.0F);
						}
						q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						if (world.random.nextInt(6) == 0) {
							Kit.sound(world, Vec3d.ofCenter(p), SoundEvents.BLOCK_GLASS_BREAK, 4.0F, 0.8F + world.random.nextFloat() * 0.6F);
						}
					}
				}
			}
		}
	}

	// ====================================================================== 8-BIT

	/**
	 * GAME START. The world inside the blast goes low-res: on screen it drops to a dithered sixteen-colour picture,
	 * and in the world every 3x3x3 cell of blocks collapses into its majority block, so the land really is drawn in
	 * fat pixels. A chiptune plays. Then GAME OVER: an 8-bit explosion scatters coloured pixels everywhere.
	 */
	public static class EightBit extends Base {
		static final int CELL = 3, RADIUS = 42, DOWNSCALE = 20, DOWNSCALE_END = 260, OVER = 300, END = 380;
		private static final int[] TUNE = {0, 4, 7, 12, 7, 4, 0, 4, 7, 11, 14, 11, 7, 2, 5, 9};
		private RingSweep cells;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("pixel_bomb", Formatting.LIGHT_PURPLE);
				Kit.title(world, g, 160, Text.literal("GAME START").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), Text.literal("PRESS ANY KEY").formatted(Formatting.AQUA));
				Vfx.vision(world, g, 5, 70, 0.4F, 14, 0, 3, 0xFFFFFF, OVER + 40, 200);
				Vfx.newFx(world, FX_PIXELS, g, g, 40, 0xFF40FF, 0, 0, END);
				cells = new RingSweep(RADIUS / CELL);
			}
			if (t % 4 == 0 && t < OVER) {
				int note = TUNE[(t / 4) % TUNE.length];
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 24.0F, (float) Math.pow(2, (note - 12) / 12.0));
				if (t % 16 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 24.0F, 0.7F);
				}
			}
			if (t >= DOWNSCALE && t < DOWNSCALE_END && Destruction.enabled(world)) {
				double front = (t - DOWNSCALE) / (double) (DOWNSCALE_END - DOWNSCALE - 20) * RADIUS / CELL;
				cells.advance(front, 10, (cx, cz, d) -> downscale(g, cx, cz));
			}
			if (t == OVER) {
				Kit.title(world, g, 200, Text.literal("GAME OVER").formatted(Formatting.RED, Formatting.BOLD), Text.literal("INSERT COIN").formatted(Formatting.YELLOW));
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 64.0F, 0.5F);
				Kit.blast(world, g.add(0, 1, 0), 5.0F, owner(), true);
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.PIXEL_BURST, g.add(0, 1, 0), 1.0F);
				List<FallingBlockEntity> bits = Kit.lift(world, g, 18, 50, true, false);
				Block[] palette = {Blocks.MAGENTA_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE};
				for (int i = 0; i < bits.size(); i++) {
					FallingBlockEntity fb = bits.get(i);
					Vec3d out = fb.getPos().subtract(g).multiply(1, 0, 1);
					out = out.lengthSquared() < 0.01 ? new Vec3d(1, 0, 0) : out.normalize();
					fb.setNoGravity(false);
					Kit.velocity(fb, out.multiply(0.6 + world.random.nextDouble() * 0.8).add(0, 0.9 + world.random.nextDouble() * 0.6, 0));
					// Retint the flung block as a pixel of colour.
					FallingBlockEntity tinted = FallingBlockEntity.spawnFromBlock(world, fb.getBlockPos().up(3), palette[i % palette.length].getDefaultState());
					tinted.setVelocity(fb.getVelocity());
					fb.discard();
				}
			}
			drain();
			return t >= END;
		}

		/** One 3x3 column of cells: each 3x3x3 cell becomes its majority block (air included). */
		private void downscale(Vec3d g, int cx, int cz) {
			int x0 = MathHelper.floor(g.x) + cx * CELL - 1, z0 = MathHelper.floor(g.z) + cz * CELL - 1;
			if (!world.isChunkLoaded(x0 >> 4, z0 >> 4)) {
				return;
			}
			int top = surface(x0 + 1, z0 + 1).getY();
			int y0 = Math.floorDiv(top - 9, CELL) * CELL;
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = y0; y < top + 15; y += CELL) {
				Map<BlockState, Integer> votes = new HashMap<>();
				boolean blocked = false;
				for (int i = 0; i < 27 && !blocked; i++) {
					p.set(x0 + i % 3, y + (i / 3) % 3, z0 + i / 9);
					BlockState s = world.getBlockState(p);
					if (s.hasBlockEntity()) {
						blocked = true;
					}
					BlockState key = s.isAir() || s.isReplaceable() && s.getFluidState().isEmpty() ? Blocks.AIR.getDefaultState() : s.getBlock().getDefaultState();
					votes.merge(key, 1, Integer::sum);
				}
				if (blocked) {
					continue;
				}
				BlockState win = votes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
				for (int i = 0; i < 27; i++) {
					q().add(x0 + i % 3, y + (i / 3) % 3, z0 + i / 9, win, BlockQueue.ANY);
				}
			}
		}
	}

	// ====================================================================== MAGNETAR

	/**
	 * A neutron star with a magnetic field a thousand trillion times Earth's blinks into existence above the bomb.
	 * Its field lines turn with it; everything iron is ripped out of the ground and the buildings (ore included) and
	 * swept into orbit, iron armour drags its wearers in, and the pulsar's jets sweep the land like a lighthouse.
	 * Then it collapses and flings its whole orbit outwards.
	 */
	public static class Magnetar extends Base {
		static final double H = 18, REACH = 48;
		static final int PULL = 20, COLLAPSE = 440, END = 520;
		private final List<FallingBlockEntity> orbit = new ArrayList<>();

		static Vec3d axis(double t) {
			double tilt = Math.toRadians(32), phi = t * 0.05;
			return new Vec3d(Math.sin(tilt) * Math.cos(phi), Math.cos(tilt), Math.sin(tilt) * Math.sin(phi));
		}

		static boolean ferrous(BlockState s) {
			return s.isOf(Blocks.IRON_BLOCK) || s.isOf(Blocks.IRON_BARS) || s.isOf(Blocks.IRON_DOOR) || s.isOf(Blocks.IRON_TRAPDOOR)
					|| s.isIn(BlockTags.RAILS) || s.isIn(BlockTags.ANVIL) || s.isOf(Blocks.CAULDRON) || s.isOf(Blocks.CHAIN)
					|| s.isOf(Blocks.LANTERN) || s.isOf(Blocks.SOUL_LANTERN) || s.isOf(Blocks.IRON_ORE) || s.isOf(Blocks.DEEPSLATE_IRON_ORE)
					|| s.isOf(Blocks.RAW_IRON_BLOCK) || s.isOf(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE) || s.isOf(Blocks.LODESTONE)
					|| s.isOf(Blocks.TRIPWIRE_HOOK) || s.isOf(Blocks.PISTON) || s.isOf(Blocks.STICKY_PISTON);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			Vec3d star = g.add(0, H, 0);
			if (t == 0) {
				name("magnetar", Formatting.AQUA);
				Vfx.newFx(world, FX_MAGNETAR, star, g, (float) REACH, 0x80C0FF, 0, 0, END);
				Kit.sound(world, star, ModSounds.ANTIMATTER, 48.0F, 0.6F);
				Kit.sound(world, star, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 0.6F);
			}
			if (t >= PULL && t < COLLAPSE) {
				if (t % 20 == 0) {
					Kit.sound(world, star, SoundEvents.BLOCK_BEACON_AMBIENT, 48.0F, 0.5F + (t % 60) / 120.0F);
				}
				if (Destruction.enabled(world) && orbit.size() < 260) {
					rip(g, t);
				}
				grind(star);
				steer(star, t);
				drag(star);
				jets(star, t);
			}
			if (t == COLLAPSE) {
				collapse(star);
			}
			drain();
			return t >= END;
		}

		/**
		 * The field is strong enough to tear anything loose, not just iron: the top of the land within a reach that
		 * grows as the star spins up is ripped up into orbit, iron first and hardest.
		 */
		private void rip(Vec3d g, int t) {
			double reach = REACH * Math.min(1.0, 0.25 + (t - PULL) / 260.0);
			BlockPos.Mutable p = new BlockPos.Mutable();
			int took = 0;
			for (int i = 0; i < 60 && took < 6; i++) {
				double a = world.random.nextDouble() * MathHelper.TAU, dd = Math.sqrt(world.random.nextDouble()) * reach;
				int x = MathHelper.floor(g.x + Math.cos(a) * dd), z = MathHelper.floor(g.z + Math.sin(a) * dd);
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				p.set(x, world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z);
				if (Destruction.isProtected(world, p)) {
					continue;
				}
				BlockState s = world.getBlockState(p);
				if (s.isAir() || s.hasBlockEntity() || !s.getFluidState().isEmpty() || s.getHardness(world, p) < 0 || s.getHardness(world, p) > 30) {
					continue;
				}
				took++;
				Destruction.record(world, p.toImmutable(), s);
				FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
				fb.setNoGravity(true);
				fb.setDestroyedOnLanding();
				orbit.add(fb);
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.MAG_LIFT, new Vec3d(x + 0.5, p.getY() + 0.5, z + 0.5), 1.0F);
				if (orbit.size() >= 260) {
					return;
				}
			}
		}

		/** Blocks in orbit grind against each other and turn to dust, so the field keeps eating more of the land. */
		private void grind(Vec3d star) {
			for (FallingBlockEntity fb : orbit) {
				if (fb.isAlive() && world.random.nextFloat() < 0.012F) {
					com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.MAG_GRIND, fb.getPos(), 1.0F);
					fb.discard();
				}
			}
		}

		/** Each block falls into an inclined orbit around the star. */
		private void steer(Vec3d star, int t) {
			for (int i = 0; i < orbit.size(); i++) {
				FallingBlockEntity fb = orbit.get(i);
				if (!fb.isAlive()) {
					continue;
				}
				fb.timeFalling = 1;
				Vec3d to = star.subtract(fb.getPos());
				double d = to.length();
				double want = 5 + (i % 9) * 1.1;
				Vec3d n = axis(t).add(new Vec3d(Math.cos(i), 0, Math.sin(i)).multiply(0.3)).normalize();
				Vec3d tangent = n.crossProduct(to).normalize().multiply(0.9);
				Vec3d radial = to.multiply(1 / Math.max(d, 0.1)).multiply(MathHelper.clamp((d - want) * 0.12, -0.5, 1.4));
				Kit.velocity(fb, tangent.add(radial));
			}
			orbit.removeIf(fb -> !fb.isAlive());
		}

		/** Iron-clad mobs and players, minecarts and iron items are dragged in. */
		private void drag(Vec3d star) {
			for (Entity e : Kit.entities(world, star, REACH * 1.6, en -> true)) {
				double pull = 0;
				if (e instanceof LivingEntity le && !(e instanceof PlayerEntity pl && (pl.isCreative() || pl.isSpectator()))) {
					for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
						ItemStack st = le.getEquippedStack(slot);
						if (st.getItem() instanceof ArmorItem armor && (armor.getMaterial() == ArmorMaterials.IRON || armor.getMaterial() == ArmorMaterials.CHAIN)) {
							pull += 0.05;
						}
					}
					if (e instanceof net.minecraft.entity.passive.IronGolemEntity) {
						pull = 0.25;
					}
				} else if (e instanceof AbstractMinecartEntity || e instanceof ItemEntity) {
					pull = 0.15;
				}
				if (pull > 0) {
					Vec3d to = star.subtract(e.getPos());
					Kit.push(e, to.normalize().multiply(pull).add(0, 0.04, 0));
					if (to.length() < 3 && e instanceof LivingEntity le) {
						le.damage(world.getDamageSources().magic(), 2.0F);
					}
				}
			}
		}

		/** The pulsar's lower jet sweeps the ground: scorch, fire and a burn. */
		private void jets(Vec3d star, int t) {
			Vec3d down = axis(t).multiply(-1);
			if (down.y > -0.2) {
				return;
			}
			double k = H / -down.y;
			Vec3d hit = Kit.ground(world, star.x + down.x * k, star.z + down.z * k);
			Kit.damage(world, hit, 3.5, 6.0F, ModDamageTypes.PLASMA, null, owner());
			if (t % 4 == 0) {
				// The jet cuts: a sweeping trench of land taken apart into light.
				com.armageddon.event.Ruin.disintegrate(world, hit, 2.6, Blocks.SEA_LANTERN.getDefaultState(), ParticleTypes.END_ROD, 4);
			}
			if (t % 5 == 0) {
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.MAG_JET_HIT, hit, 1.0F);
				Kit.sound(world, hit, ModSounds.PLASMA_IMPACT, 6.0F, 1.2F);
			}
		}

		private void collapse(Vec3d star) {
			Kit.sound(world, star, ModSounds.BLACK_HOLE_COLLAPSE, 64.0F, 0.8F);
			Vfx.screen(world, star, 200, 3, 0.6F, 24);
			// It collapses into a point and the orbit is fired off as a storm of debris; where the star was is a hole.
			com.armageddon.event.Ruin.implode(world, star, 7, 18);
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.MAG_COLLAPSE, star, 1.0F);
			for (FallingBlockEntity fb : orbit) {
				if (fb.isAlive()) {
					fb.setNoGravity(false);
					Vec3d out = fb.getPos().subtract(star);
					out = out.lengthSquared() < 0.01 ? new Vec3d(0, 1, 0) : out.normalize();
					Kit.velocity(fb, out.multiply(2.6).add(0, 0.5, 0));
				}
			}
			orbit.clear();
		}

		@Override
		public void finish() {
			for (FallingBlockEntity fb : orbit) {
				if (fb.isAlive()) {
					fb.setNoGravity(false);
				}
			}
			super.finish();
		}
	}

	// ====================================================================== KA-BOOM (comic)

	/**
	 * The world is redrawn in ink, starting at the bomb and spreading out: outlines, cel bands, halftone dots and
	 * paper. Giant block letters spelling KABOOM rise from the ground, comic blasts go off all around with their
	 * words written in the air, and then the letters themselves blow up.
	 */
	public static class KaBoom extends Base {
		static final int LETTERS = 30, LETTERS_END = 130, BLASTS = 140, BOOM = 320, END = 420;
		static final String WORD = "KABOOM";
		static final int PX = 3;
		private static final String[] WORDS = {"POW", "BAM", "ZAP", "WHAM", "KRAK", "BOOM", "BLAM", "THWACK"};
		private Vec3d heading;
		private final List<int[]> letter = new ArrayList<>();
		private int placed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("ink_bomb", Formatting.YELLOW);
				heading = Kit.dir(data.getFloat("Yaw"));
				Vfx.vision(world, g, 6, 90, 0.7F, 0, 0, 0, 0xFFFFFF, END - 10, 200);
				Kit.sound(world, g, SoundEvents.ITEM_BOOK_PAGE_TURN, 32.0F, 0.6F);
				plan(g);
			}
			if (t >= LETTERS && t <= LETTERS_END) {
				int to = (int) ((t - LETTERS) / (double) (LETTERS_END - LETTERS) * letter.size());
				for (; placed < Math.min(to, letter.size()); placed++) {
					int[] b = letter.get(placed);
					q().add(b[0], b[1], b[2], b[3] == 0 ? Blocks.BLACK_CONCRETE.getDefaultState()
							: b[3] == 1 ? Blocks.YELLOW_CONCRETE.getDefaultState() : b[3] == 2 ? Blocks.ORANGE_CONCRETE.getDefaultState() : Blocks.RED_CONCRETE.getDefaultState(), BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Kit.sound(world, g, SoundEvents.BLOCK_WOOL_PLACE, 32.0F, 0.6F);
				}
			}
			for (int i = 0; i < 8; i++) {
				if (t == BLASTS + i * 20) {
					double a = i * 2.39996 + 1.0;
					double d = 18 + (i % 4) * 9;
					Vec3d p = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					blast(p, 3.6F, i);
				}
			}
			if (t == BOOM) {
				Vec3d mid = g.add(heading.multiply(30)).add(0, 8, 0);
				blast(mid, 7.0F, 5);
				for (int k = -2; k <= 2; k++) {
					Vec3d side = new Vec3d(-heading.z, 0, heading.x).multiply(k * 14);
					final Vec3d p = mid.add(side);
					DetonationManager.schedule(world, 4 + (k + 2) * 3, () -> {
						Kit.blast(world, p, 4.0F, owner(), true);
						com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.COMIC_BLAST, p, 4.0F / 3.6F);
					});
				}
			}
			drain();
			return t >= END;
		}

		private void blast(Vec3d p, float size, int word) {
			Kit.blast(world, p, size, owner(), true);
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.COMIC_BLAST, p, size / 3.6F);
			Vfx.impact(world, p, 0, 2);
			Vfx.newFx(world, FX_COMIC, p.add(0, 4 + size, 0), p, size * 2.2F, word % 2 == 0 ? 0xFFE020 : 0xFF5020, word % WORDS.length, 0, 50);
			Kit.sound(world, p, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 32.0F, 0.5F);
		}

		/** Lays out the six letters as fat pixels: black rim, yellow-orange-red fill from top to bottom. */
		private void plan(Vec3d g) {
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			int w = MainStageSet.width(WORD) * PX;
			Vec3d origin = g.add(heading.multiply(30)).subtract(right.multiply(w / 2.0));
			int gy = (int) Math.floor(Kit.ground(world, origin.x + right.x * w / 2, origin.z + right.z * w / 2).y);
			for (int c = 0; c < WORD.length(); c++) {
				for (int col = 0; col < 3; col++) {
					for (int row = 0; row < 5; row++) {
						if (!MainStageSet.pixel(WORD.charAt(c), col, row)) {
							continue;
						}
						for (int a = 0; a < PX; a++) {
							for (int b = 0; b < PX; b++) {
								int u = (c * 4 + col) * PX + a;
								int v = (4 - row) * PX + b;
								boolean rim = a == 0 || b == 0 || a == PX - 1 || b == PX - 1;
								int fill = rim ? 0 : row < 2 ? 1 : row < 4 ? 2 : 3;
								Vec3d at = origin.add(right.multiply(u));
								for (int depth = 0; depth < 2; depth++) {
									Vec3d d = at.add(heading.multiply(depth));
									letter.add(new int[]{MathHelper.floor(d.x), gy + 1 + v, MathHelper.floor(d.z), depth == 0 ? fill : 0});
								}
							}
						}
					}
				}
			}
			letter.sort((x, y) -> Integer.compare(x[1], y[1]));
		}
	}

	// ====================================================================== AURORA

	/**
	 * The sky darkens and an aurora unrolls overhead, curtains of green and violet light rippling across it. Then it
	 * comes down: its rays strike the ground one after another, charging the air and glazing the earth in coloured
	 * glass, and finally the whole aurora spirals into one column of light.
	 */
	public static class Aurora extends Base {
		static final int STRIKES = 120, STRIKES_END = 420, FINALE = 460, END = 600;
		static final int[] COLORS = {0x40FF90, 0x30E0FF, 0xB040FF, 0x80FF40, 0xFF40C0};
		static final Block[] GLOW = {Blocks.VERDANT_FROGLIGHT, Blocks.SEA_LANTERN, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT};

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("aurora_bomb", Formatting.GREEN);
				Vfx.newFx(world, FX_AURORA, g, g, 140, 0x40FF90, 0, 0, END);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 0.4F);
			}
			if (t % 20 == 0 && t < FINALE) {
				Vfx.skyTint(world, g, 300, 0x041018, 0.9F);
			}
			if (t % 40 == 10 && t < FINALE) {
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F, 0.4F + world.random.nextFloat() * 0.3F);
			}
			if (t >= STRIKES && t < STRIKES_END && t % 12 == 0) {
				int k = (t - STRIKES) / 12;
				Vec3d p = CityDestructionEvents.around(world, g, 100);
				strike(p, COLORS[k % COLORS.length], k);
			}
			if (t == FINALE) {
				com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.AURORA_CROWN, g, 1.0F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.8F);
				Kit.damage(world, g, 14, 40.0F, ModDamageTypes.PLASMA, null, owner());
				com.armageddon.event.Ruin.disintegrate(world, g, 11, Blocks.SEA_LANTERN.getDefaultState(), ParticleTypes.END_ROD, 40);
			}
			drain();
			return t >= END;
		}

		private void strike(Vec3d p, int color, int k) {
			com.armageddon.network.Vfx.play(world, com.armageddon.vfx.VfxIds.AURORA_STRIKE, p, new Vec3d(0, 1, 0), 1.0F, color);
			Kit.sound(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 24.0F, 1.6F);
			for (LivingEntity e : Kit.living(world, p, 8)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 30, 1, false, true));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false));
				}
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			// The ray takes the land apart in its own colour: a deep round shaft of light where it touched.
			Kit.damage(world, p, 6, 16.0F, ModDamageTypes.PLASMA, null, owner());
			BlockState light = GLOW[k % GLOW.length].getDefaultState();
			for (int i = 0; i < 3; i++) {
				com.armageddon.event.Ruin.disintegrate(world, p.add(0, -i * 3.5, 0), 4.2 - i * 0.6, light, ParticleTypes.END_ROD, 10 + i * 4);
			}
		}
	}

	// ====================================================================== CENTURY

	/**
	 * A time-lapse bomb. The sun and moon race across the sky while, around the bomb, a whole year plays in forty
	 * seconds: spring blossom and flowers, summer growth and ripe crops, autumn leaf fall, winter snow and ice.
	 * Then a century passes in an instant: stone cracks and mosses, wood rots, glass falls out, the young grow up.
	 */
	public static class Century extends Base {
		static final int R = 48, SPRING = 20, SUMMER = 100, AUTUMN = 180, WINTER = 260, AGES = 360, END = 420;
		private RingSweep sweep;
		private int phase = -1;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("seasons_bomb", Formatting.GOLD);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 48.0F, 0.5F);
			}
			if (t < AGES) {
				world.setTimeOfDay(world.getTimeOfDay() + 60);
			}
			int ph = t >= WINTER ? 3 : t >= AUTUMN ? 2 : t >= SUMMER ? 1 : t >= SPRING ? 0 : -1;
			if (ph != phase && ph >= 0) {
				phase = ph;
				sweep = new RingSweep(R);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 48.0F, 0.7F + ph * 0.15F);
			}
			if (phase >= 0 && t < AGES && sweep != null && Destruction.enabled(world)) {
				double front = (t - new int[]{SPRING, SUMMER, AUTUMN, WINTER}[phase]) * 0.8;
				final int p = phase;
				sweep.advance(front, 400, (dx, dz, d) -> season(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz, p));
			}
			if (t < AGES && t % 3 == 0) {
				particles(g, phase);
			}
			if (t == AGES) {
				Kit.title(world, g, 200, Text.literal("A CENTURY PASSES").formatted(Formatting.GRAY, Formatting.BOLD), Text.empty());
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.3F);
				Vfx.screen(world, g, 200, 0, 0.7F, 60);
				Vfx.shake(world, g, 200, 1.2F);
				Kit.boom(world, g.add(0, 1, 0), 4.0F, 0xC0A060, owner(), false);
				age(g);
				sweep = new RingSweep(R);
			}
			if (t > AGES && sweep != null && Destruction.enabled(world)) {
				sweep.advance((t - AGES) * 2.5, 600, (dx, dz, d) -> decay(MathHelper.floor(g.x) + dx, MathHelper.floor(g.z) + dz));
			}
			drain();
			return t >= END;
		}

		private void particles(Vec3d g, int ph) {
			for (int i = 0; i < 6; i++) {
				Vec3d p = CityDestructionEvents.around(world, g, R);
				switch (ph) {
					case 0 -> world.spawnParticles(ParticleTypes.CHERRY_LEAVES, p.x, p.y + 6, p.z, 4, 3, 2, 3, 0.02);
					case 1 -> world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y + 1, p.z, 3, 2, 1, 2, 0.02);
					case 2 -> world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.ORANGE_CONCRETE_POWDER.getDefaultState()), p.x, p.y + 8, p.z, 6, 3, 2, 3, 0.02);
					case 3 -> world.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 8, p.z, 10, 4, 3, 4, 0.02);
					default -> {
					}
				}
			}
		}

		private void season(int x, int z, int ph) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			BlockPos top = BlockPos.ofFloored(x, world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z) - 1, z);
			BlockState s = world.getBlockState(top);
			double h = NewBombShapes.hash(x, z);
			switch (ph) {
				case 0 -> {
					if (s.isIn(BlockTags.LEAVES) && h < 0.4) {
						q().add(top, (h < 0.2 ? Blocks.CHERRY_LEAVES : Blocks.FLOWERING_AZALEA_LEAVES).getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true), BlockQueue.ANY);
					} else if (s.isOf(Blocks.GRASS_BLOCK) && h < 0.3) {
						Block[] fl = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.OXEYE_DAISY, Blocks.PINK_TULIP};
						q().add(top.up(), fl[(int) (h * 100) % fl.length].getDefaultState(), BlockQueue.AIR_ONLY);
					}
				}
				case 1 -> {
					if (s.isOf(Blocks.GRASS_BLOCK) && h > 0.5) {
						q().add(top.up(), Blocks.SHORT_GRASS.getDefaultState(), BlockQueue.AIR_ONLY);
					} else if (s.getBlock() instanceof CropBlock crop) {
						q().add(top, crop.withAge(crop.getMaxAge()), BlockQueue.ANY);
					}
				}
				case 2 -> {
					if (s.isIn(BlockTags.LEAVES) && h < 0.6) {
						q().add(top, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					} else if (s.isIn(BlockTags.FLOWERS) || s.isOf(Blocks.SHORT_GRASS)) {
						q().add(top, h < 0.4 ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.AIR.getDefaultState(), BlockQueue.ANY);
					}
				}
				case 3 -> {
					if (s.isOf(Blocks.WATER)) {
						q().add(top, Blocks.ICE.getDefaultState(), BlockQueue.ANY);
					} else if (s.isOpaqueFullCube(world, top)) {
						q().add(top.up(), Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 1 + (int) (h * 2)), BlockQueue.AIR_ONLY);
					}
				}
				default -> {
				}
			}
		}

		/** The young grow up and everyone feels their age. */
		private void age(Vec3d g) {
			for (LivingEntity e : Kit.living(world, g, R + 10)) {
				if (e instanceof PassiveEntity pe && pe.isBaby()) {
					pe.setBaby(false);
				}
				if (e instanceof PlayerEntity pl && !Kit.creativeOrSpectator(pl)) {
					pl.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 1, false, true));
					pl.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 400, 1, false, true));
					pl.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 2, false, true));
				}
			}
		}

		/** A hundred years on a column: stone cracks and mosses, wood rots away, glass falls out, snow melts. */
		private void decay(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			int top = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, x, z);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = top; y > top - 24; y--) {
				p.set(x, y, z);
				BlockState s = world.getBlockState(p);
				double h = NewBombShapes.hash(x * 13 + y, z * 7 - y);
				BlockState to = null;
				if (s.isOf(Blocks.STONE_BRICKS)) {
					to = h < 0.5 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.MOSSY_STONE_BRICKS.getDefaultState();
				} else if (s.isOf(Blocks.COBBLESTONE)) {
					to = h < 0.6 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : null;
				} else if (s.isIn(BlockTags.PLANKS) || s.isIn(BlockTags.WOODEN_STAIRS) || s.isIn(BlockTags.WOODEN_SLABS) || s.isIn(BlockTags.WOODEN_FENCES)) {
					to = h < 0.35 ? Blocks.AIR.getDefaultState() : null;
				} else if (s.isOf(Blocks.GLASS) || s.isOf(Blocks.GLASS_PANE)) {
					to = h < 0.6 ? Blocks.AIR.getDefaultState() : null;
				} else if (s.isOf(Blocks.SNOW) || s.isOf(Blocks.ICE)) {
					to = s.isOf(Blocks.ICE) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
				} else if (s.isOf(Blocks.STONE) && h < 0.1) {
					to = Blocks.COBBLESTONE.getDefaultState();
				} else if (s.isOf(Blocks.DIRT) && h < 0.3 && world.getBlockState(p.up()).isAir()) {
					to = Blocks.GRASS_BLOCK.getDefaultState();
				}
				if (to != null) {
					q().add(p.toImmutable(), to, BlockQueue.ANY);
				}
				if (y < top - 4 && s.isOpaqueFullCube(world, p) && !s.isIn(BlockTags.PLANKS)) {
					break;
				}
			}
			if (NewBombShapes.hash(x, z) < 0.06) {
				BlockPos b = BlockPos.ofFloored(x, top, z);
				if (world.getBlockState(b.down()).isOf(Blocks.GRASS_BLOCK)) {
					q().add(b, Blocks.OAK_SAPLING.getDefaultState(), BlockQueue.AIR_ONLY);
				}
			}
		}
	}

	// ====================================================================== PRISM

	/**
	 * A floating glass prism catches a white beam out of the sky and splits it into seven. The rainbow beams sweep
	 * across the land, each burning a trench in its own colour, then fold back together into one white blast.
	 */
	public static class PrismBomb extends Base {
		static final Block[] TRENCH = {Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
				Blocks.LIGHT_BLUE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.PURPLE_CONCRETE};

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("prism_bomb", Formatting.WHITE);
				Vfx.newFx(world, FX_PRISM, g, g, (float) Prism.REACH, 0xFFFFFF, 0, 0, Prism.END);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 48.0F, 0.5F);
			}
			if (t == Prism.CHARGE) {
				Kit.sound(world, g, ModSounds.RAILGUN_CHARGE, 64.0F, 0.6F);
			}
			if (t == Prism.SPLIT) {
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 1.0F);
				Vfx.flash(world, g, 0.4F, 200, 0xFFFFFF);
			}
			if (t >= Prism.SPLIT && t < Prism.CONVERGE) {
				for (int i = 0; i < Prism.BEAMS; i++) {
					burn(g, i, t);
				}
				if (t % 10 == 0) {
					Kit.sound(world, g, ModSounds.DEATH_RAY, 48.0F, 0.8F + (t % 30) / 60.0F);
				}
			}
			if (t == Prism.CONVERGE + 10) {
				Kit.boom(world, g.add(0, 2, 0), 8.0F, 0xFFFFFF, owner(), true);
				Vfx.flash(world, g, 0.7F, 300, 0xFFFFFF);
				for (int i = 0; i < Prism.BEAMS; i++) {
					final int c = Prism.COLORS[i];
					final int k = i;
					DetonationManager.schedule(world, i * 3, () -> Vfx.ring(world, g.add(0, 1, 0), 60.0F + k * 18, 3.0F, c, true, 45));
				}
			}
			drain();
			return t >= Prism.END;
		}

		private void burn(Vec3d g, int i, int t) {
			Vec3d h = Prism.hit(g, i, t);
			Vec3d p = Kit.ground(world, h.x, h.z);
			Kit.damage(world, p, 2.8, 5.0F, ModDamageTypes.PLASMA, null, owner());
			if (t % 3 == i % 3) {
				world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y + 0.3, p.z, 3, 0.5, 0.2, 0.5, 0.05);
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			BlockState col = TRENCH[i].getDefaultState();
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					BlockPos b = surface(MathHelper.floor(p.x) + x, MathHelper.floor(p.z) + z);
					q().add(b, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
					q().add(b.down(), col, BlockQueue.ANY);
				}
			}
			if (world.random.nextInt(12) == 0) {
				q().add(BlockPos.ofFloored(p), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}
	}

	// ====================================================================== GEODE

	/**
	 * The ground cracks in a ring and a giant geode heaves up through it: a basalt crust, a calcite band and a
	 * hollow of amethyst full of crystal clusters. It starts to ring, louder and higher, until it sprays shards of
	 * amethyst across the land; each lands as a little blast that leaves a crystal growing out of the crater.
	 */
	public static class GeodeBomb extends Base {
		private final List<int[]> shell = new ArrayList<>();
		private int placed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("geode_bomb", Formatting.LIGHT_PURPLE);
				Vfx.newFx(world, FX_GEODE, g, g, (float) Geode.R, 0xC080FF, 0, 0, Geode.END);
				Kit.ringCrack(world, g, Geode.R + 3, 6, Blocks.SMOOTH_BASALT.getDefaultState(), 0.8F);
				Kit.sound(world, g, SoundEvents.BLOCK_DEEPSLATE_BREAK, 48.0F, 0.4F);
			}
			if (t == Geode.RISE) {
				// Not a geode built on the land: the land itself turns to crystal, rings, and shatters into a hollow.
				com.armageddon.event.Ruin.crystallize(world, Geode.centre(g).add(0, -2, 0), Geode.R + 1, com.armageddon.event.Ruin.AMETHYST,
						Geode.RISEN - Geode.RISE, Geode.SHATTER - Geode.RISEN);
			}
			if (t >= Geode.RISE && t <= Geode.RISEN) {
				if (t % 5 == 0) {
					Vfx.shake(world, g, 80, 0.5F);
					Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, 32.0F, 0.5F + (t - Geode.RISE) / 200.0F);
				}
			}

			if (t >= Geode.RESONATE && t < Geode.SPRAY && t % 6 == 0) {
				Kit.sound(world, Geode.centre(g), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F, 0.6F + (t - Geode.RESONATE) / 50.0F);
			}
			for (int i = 0; i < Geode.SHARDS; i++) {
				if (t == Geode.launch(i)) {
					Kit.sound(world, Geode.from(g, i), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 24.0F, 1.0F + (i % 4) * 0.2F);
				}
				if (t == Geode.launch(i) + Geode.flight(i)) {
					land(g, i);
				}
			}
			if (t == Geode.SHATTER) {
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 64.0F, 0.5F);
				Kit.damage(world, Geode.centre(g), Geode.R + 6, 30.0F, ModDamageTypes.SHRAPNEL, null, owner());
			}
			drain();
			return t >= Geode.END;
		}

		/** Shell layout, bottom to top: basalt crust, calcite, amethyst (some budding), clusters inside, hollow core. */
		private void plan(Vec3d g) {
			Vec3d c = Geode.centre(g);
			int R = (int) Math.ceil(Geode.R) + 1;
			int cx = MathHelper.floor(c.x), cy = MathHelper.floor(c.y), cz = MathHelper.floor(c.z);
			for (int y = -R; y <= R; y++) {
				for (int x = -R; x <= R; x++) {
					for (int z = -R; z <= R; z++) {
						double d = Math.sqrt(x * x + y * y + z * z) + (NewBombShapes.hash(x * 7 + y, z * 5 - y) - 0.5) * 0.9;
						if (d > Geode.R) {
							continue;
						}
						// A jagged hole broken open at the top.
						if (y > Geode.R * 0.62 && NewBombShapes.hash(x, z) < 0.8) {
							continue;
						}
						int layer = d > Geode.R - 1.3 ? 0 : d > Geode.R - 2.4 ? 1 : d > Geode.R - 3.5 ? 2 : d > Geode.R - 4.5 ? 3 : 4;
						shell.add(new int[]{cx + x, cy + y, cz + z, layer, y});
					}
				}
			}
			shell.sort((a, b) -> Integer.compare(a[1], b[1]));
		}

		private BlockState state(int layer, int y) {
			return switch (layer) {
				case 0 -> Blocks.SMOOTH_BASALT.getDefaultState();
				case 1 -> Blocks.CALCITE.getDefaultState();
				case 2 -> world.random.nextInt(8) == 0 ? Blocks.BUDDING_AMETHYST.getDefaultState() : Blocks.AMETHYST_BLOCK.getDefaultState();
				case 3 -> world.random.nextInt(5) == 0 ? Blocks.AMETHYST_CLUSTER.getDefaultState().with(AmethystClusterBlock.FACING, y < 0 ? Direction.UP : Direction.DOWN)
						: Blocks.AIR.getDefaultState();
				default -> Blocks.AIR.getDefaultState();
			};
		}

		private void land(Vec3d g, int i) {
			Vec3d l = Geode.land(g, i);
			Vec3d p = Kit.ground(world, l.x, l.z);
			Kit.sound(world, p, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 12.0F, 1.2F);
			Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 8.0F, 1.4F);
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.getDefaultState()), p.x, p.y + 0.5, p.z, 30, 0.8, 0.6, 0.8, 0.2);
			Kit.damage(world, p, 4.0, 8.0F, ModDamageTypes.SHRAPNEL, null, owner());
			if (!Destruction.enabled(world)) {
				return;
			}
			com.armageddon.event.Ruin.crystallize(world, p.add(0, -1, 0), 2.6, com.armageddon.event.Ruin.AMETHYST, 5, 30);
		}
	}

	// ====================================================================== FOSSIL

	/**
	 * The earth splits along a line and the bones of something enormous rise out of it: tail, spine, ribs, legs and
	 * a skull longer than a house. Its empty eyes catch fire. It roars, twice, and the roar is a weapon: a cone of
	 * sound that throws everything in front of it away and tears the ground.
	 */
	public static class Fossil extends Base {
		static final int SPLIT = 0, RISE = 40, RISEN = 200, EYES = 215, ROAR = 240, ROAR2 = 300, END = 360;
		private Vec3d heading;
		private final List<int[]> bones = new ArrayList<>();
		private int placed;
		private Vec3d skull, eyes;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("fossil_bomb", Formatting.WHITE);
				heading = Kit.dir(data.getFloat("Yaw"));
				Kit.fissure(world, g.subtract(heading.multiply(10)), heading, 56, 8, 7, null);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 32.0F, 0.4F);
				Vfx.shake(world, g, 120, 1.0F);
				plan(g);
			}
			if (t >= RISE && t <= RISEN) {
				int to = (int) ((t - RISE) / (double) (RISEN - RISE) * bones.size());
				for (; placed < Math.min(to, bones.size()); placed++) {
					int[] b = bones.get(placed);
					q().add(b[0], b[1], b[2], b[3] == 1 ? Blocks.SHROOMLIGHT.getDefaultState() : Blocks.BONE_BLOCK.getDefaultState(), BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Kit.sound(world, g, SoundEvents.ENTITY_SKELETON_STEP, 32.0F, 0.4F);
					world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()), g.x, g.y + 2, g.z, 40, 12, 2, 12, 0.1);
				}
			}
			if (t == EYES) {
				Vfx.newFx(world, FX_FOSSIL, eyes, heading, 1.0F, 0xFF3010, 0, 0, END - EYES);
				Kit.sound(world, eyes, SoundEvents.ENTITY_BLAZE_SHOOT, 32.0F, 0.4F);
			}
			if (t == ROAR || t == ROAR2) {
				roar(t == ROAR2 ? 1.4 : 1.0);
			}
			drain();
			return t >= END;
		}

		/** A theropod in bone: lists every block with its height, so it can rise from the bottom up. */
		private void plan(Vec3d g) {
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			Vec3d base = Kit.ground(world, g.x, g.z);
			// Spine: tail tip (u = -28) to neck (u = 16), as an arc.
			for (double u = -28; u <= 16; u += 0.5) {
				double y = 15 + 5 * Math.cos(u / 22.0) - (u < -10 ? (u + 10) * (u + 10) * 0.025 : 0);
				double r = u < -14 ? 0.6 : 1.4;
				ball(base.add(heading.multiply(u)).add(0, y, 0), r, 0);
				// Vertebra spines along the back.
				if (((int) (u * 2)) % 4 == 0 && u > -16) {
					ball(base.add(heading.multiply(u)).add(0, y + 2.2, 0), 0.6, 0);
				}
			}
			// Ribs: arches hanging from the spine over the chest.
			for (int k = 0; k < 7; k++) {
				double u = -6 + k * 2.6;
				double y = 15 + 5 * Math.cos(u / 22.0);
				double depth = 7.5 - Math.abs(k - 3) * 0.9;
				for (double a = 0; a <= Math.PI; a += 0.12) {
					Vec3d off = right.multiply(Math.cos(a) * 4.5).add(0, -Math.sin(a) * depth, 0);
					ball(base.add(heading.multiply(u)).add(0, y, 0).add(off), 0.55, 0);
				}
			}
			// Two legs: thigh, shin, foot with claws.
			for (int side = -1; side <= 1; side += 2) {
				Vec3d hip = base.add(heading.multiply(-6)).add(right.multiply(side * 3.6)).add(0, 15.5, 0);
				Vec3d knee = hip.add(heading.multiply(3.5)).add(0, -7.5, 0);
				Vec3d ankle = knee.add(heading.multiply(-2.5)).add(0, -6, 0);
				line(hip, knee, 1.6);
				line(knee, ankle, 1.1);
				line(ankle, ankle.add(heading.multiply(3.5)).add(0, -2, 0), 0.8);
				for (int c = -1; c <= 1; c++) {
					Vec3d toe = ankle.add(heading.multiply(3.5)).add(0, -2, 0);
					line(toe, toe.add(heading.multiply(2.5)).add(right.multiply(c * 1.2)).add(0, -0.5, 0), 0.5);
				}
				// Tiny arms.
				Vec3d shoulder = base.add(heading.multiply(10)).add(right.multiply(side * 2.5)).add(0, 16, 0);
				line(shoulder, shoulder.add(heading.multiply(2.5)).add(0, -3, 0), 0.5);
			}
			// The skull: a long box with a gaping jaw, teeth and two eye sockets.
			double neckY = 15 + 5 * Math.cos(16 / 22.0);
			skull = base.add(heading.multiply(20)).add(0, neckY + 1.5, 0);
			line(base.add(heading.multiply(16)).add(0, neckY, 0), skull, 1.6);
			for (double u = 0; u <= 10; u += 0.5) {
				double hgt = 3.2 - u * 0.12;
				for (double v = -2.6 + u * 0.08; v <= 2.6 - u * 0.08; v += 0.5) {
					Vec3d top = skull.add(heading.multiply(u)).add(right.multiply(v));
					ball(top.add(0, hgt, 0), 0.5, 0);
					ball(top.add(0, 0.2, 0), 0.5, 0);
					if (Math.abs(v) > 2.0 - u * 0.08) {
						line(top.add(0, 0.2, 0), top.add(0, hgt, 0), 0.45);
					}
					// The lower jaw, dropped open.
					ball(top.add(0, -2.6 - u * 0.25, 0), 0.5, 0);
				}
				if (((int) (u * 2)) % 3 == 0 && u > 1) {
					for (int side = -1; side <= 1; side += 2) {
						Vec3d edge = skull.add(heading.multiply(u)).add(right.multiply(side * (2.2 - u * 0.08)));
						line(edge, edge.add(0, -1.4, 0), 0.4);
					}
				}
			}
			eyes = skull.add(heading.multiply(3)).add(0, 2.4, 0);
			for (int side = -1; side <= 1; side += 2) {
				Vec3d e = eyes.add(right.multiply(side * 2.4));
				bones.add(new int[]{MathHelper.floor(e.x), MathHelper.floor(e.y), MathHelper.floor(e.z), 1});
			}
			bones.sort((a, b) -> Integer.compare(a[1], b[1]));
		}

		private void ball(Vec3d c, double r, int kind) {
			int R = MathHelper.ceil(r);
			for (int x = -R; x <= R; x++) {
				for (int y = -R; y <= R; y++) {
					for (int z = -R; z <= R; z++) {
						if (x * x + y * y + z * z <= r * r + 0.3) {
							bones.add(new int[]{MathHelper.floor(c.x) + x, MathHelper.floor(c.y) + y, MathHelper.floor(c.z) + z, kind});
						}
					}
				}
			}
		}

		private void line(Vec3d a, Vec3d b, double r) {
			double len = a.distanceTo(b);
			for (double s = 0; s <= len; s += 0.5) {
				ball(a.add(b.subtract(a).multiply(s / Math.max(len, 0.01))), r, 0);
			}
		}

		/** A cone of sound out of the jaws: everything in front is thrown and hurt, the ground in front torn up. */
		private void roar(double power) {
			Kit.sound(world, skull, SoundEvents.ENTITY_RAVAGER_ROAR, 64.0F, 0.3F);
			Kit.sound(world, skull, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.5F);
			Vfx.shake(world, skull, 160, (float) (1.2 * power));
			Vec3d mouth = skull.add(heading.multiply(10));
			for (int k = 0; k < 6; k++) {
				Vfx.soundwave(world, mouth, mouth.add(heading.multiply(60)).add(0, -10, 0), (float) (6 + k * 2), 0xFFE0C0, 0.5F + k * 0.1F);
			}
			for (LivingEntity e : Kit.living(world, mouth, 70)) {
				Vec3d to = e.getPos().subtract(mouth);
				double d = to.length();
				if (d < 0.1 || to.normalize().dotProduct(heading) < Math.cos(Math.toRadians(35)) || Kit.creativeOrSpectator(e)) {
					continue;
				}
				double k = 1 - d / 70;
				Kit.push(e, heading.multiply(2.6 * k * power).add(0, 0.9 * k, 0));
				e.damage(world.getDamageSources().sonicBoom(host), (float) (14 * k * power));
			}
			// The roar tears a widening cone out of the land in front of it, the ground flung away down its length.
			com.armageddon.event.Ruin.cone(world, Kit.ground(world, mouth.x, mouth.z), heading, 60 * power, 26, (int) (5 * power), 3.0);
		}
	}

	// ====================================================================== LAVA LAMP

	/**
	 * A lava lamp three storeys tall builds itself over the bomb: copper base, glass column, copper cap. Inside, huge
	 * blobs of glowing goo rise and sink, faster and faster as it heats up, until the glass cracks, pane by pane...
	 * and the lamp bursts, throwing its goo across the land in molten splats.
	 */
	public static class LavaLamp extends Base {
		private final List<int[]> parts = new ArrayList<>();
		private final List<BlockPos> glass = new ArrayList<>();
		private int placed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("lava_lamp", Formatting.GOLD);
				Vfx.newFx(world, FX_LAMP, g, g, (float) Lamp.R, 0xFF6020, 0, 0, Lamp.END);
				Kit.sound(world, g, SoundEvents.BLOCK_COPPER_PLACE, 32.0F, 0.6F);
				plan(g);
			}
			if (t >= Lamp.BUILD && t <= Lamp.BUILT) {
				int to = (int) ((t - Lamp.BUILD) / (double) (Lamp.BUILT - Lamp.BUILD) * parts.size());
				for (; placed < Math.min(to, parts.size()); placed++) {
					int[] b = parts.get(placed);
					q().add(b[0], b[1], b[2], part(b[3]), BlockQueue.ANY);
				}
			}
			if (t == Lamp.BUILT + 2) {
				for (int k = 0; k < 4; k++) {
					TemporaryBlocks.place(world, BlockPos.ofFloored(g.add(0, Lamp.BASE_H + 4 + k * 7, 0)), Blocks.LIGHT.getDefaultState(), Lamp.BURST - Lamp.BUILT);
				}
			}
			if (t >= Lamp.HEAT && t < Lamp.BURST && t % 25 == 0) {
				Kit.sound(world, g.add(0, 15, 0), SoundEvents.BLOCK_LAVA_POP, 32.0F, 0.5F + (t - Lamp.HEAT) / 600.0F);
				Kit.sound(world, g.add(0, 15, 0), SoundEvents.BLOCK_LAVA_AMBIENT, 32.0F, 0.6F);
			}
			if (t >= Lamp.CRACK && t < Lamp.BURST && t % 4 == 0 && !glass.isEmpty() && Destruction.enabled(world)) {
				BlockPos p = glass.remove(world.random.nextInt(glass.size()));
				q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
				Kit.sound(world, Vec3d.ofCenter(p), SoundEvents.BLOCK_GLASS_BREAK, 16.0F, 0.7F);
			}
			if (t == Lamp.BURST) {
				burst(g);
			}
			for (int i = 0; i < Lamp.FLYERS; i++) {
				if (t == Lamp.BURST + Lamp.flight(i)) {
					splat(g, i);
				}
			}
			drain();
			return t >= Lamp.END;
		}

		private void plan(Vec3d g) {
			int cx = MathHelper.floor(g.x), cy = MathHelper.floor(g.y), cz = MathHelper.floor(g.z);
			int top = (int) (Lamp.BASE_H + Lamp.H);
			for (int y = 0; y < top + 6; y++) {
				double r = y < Lamp.BASE_H ? Lamp.R + 1.5 - y * 0.15 : y < top ? Lamp.R + 0.4 * Math.sin((y - Lamp.BASE_H) / Lamp.H * Math.PI) : Lamp.R - (y - top) * 0.9;
				int R = MathHelper.ceil(r);
				for (int x = -R; x <= R; x++) {
					for (int z = -R; z <= R; z++) {
						double d = Math.sqrt(x * x + z * z);
						if (d > r) {
							continue;
						}
						boolean skin = d > r - 1.1;
						int kind;
						if (y < Lamp.BASE_H || y >= top) {
							kind = skin || y == 0 || y >= top ? (y % 3 == 0 ? 1 : 0) : 3;
						} else {
							if (!skin) {
								kind = 3;
							} else {
								kind = 2;
								glass.add(new BlockPos(cx + x, cy + y, cz + z));
							}
						}
						parts.add(new int[]{cx + x, cy + y, cz + z, kind});
					}
				}
			}
		}

		private static BlockState part(int kind) {
			return switch (kind) {
				case 0 -> Blocks.CUT_COPPER.getDefaultState();
				case 1 -> Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();
				case 2 -> Blocks.ORANGE_STAINED_GLASS.getDefaultState();
				default -> Blocks.AIR.getDefaultState();
			};
		}

		private void burst(Vec3d g) {
			Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.4F);
			Kit.boom(world, g.add(0, Lamp.BASE_H + Lamp.H * 0.5, 0), 5.0F, 0xFF6020, owner(), false);
			if (Destruction.enabled(world)) {
				for (BlockPos p : glass) {
					q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
				}
			}
			glass.clear();
		}

		/** A blob of goo lands: a molten splat with a little fire and, for the big ones, a pool of lava. */
		private void splat(Vec3d g, int i) {
			Vec3d l = Lamp.land(g, i);
			Vec3d p = Kit.ground(world, l.x, l.z);
			Kit.sound(world, p, SoundEvents.BLOCK_LAVA_EXTINGUISH, 16.0F, 0.6F);
			Kit.sound(world, p, SoundEvents.ENTITY_SLIME_SQUISH, 16.0F, 0.5F);
			world.spawnParticles(ParticleTypes.LAVA, p.x, p.y + 0.5, p.z, 20, 1, 0.5, 1, 0.2);
			Kit.damage(world, p, 3.5, 7.0F, ModDamageTypes.PLASMA, null, owner());
			for (LivingEntity e : Kit.living(world, p, 3.5)) {
				e.setOnFireFor(6);
			}
			if (!Destruction.enabled(world)) {
				return;
			}
			int r = i < Lamp.BLOBS ? 3 : 1;
			BlockPos c = BlockPos.ofFloored(p);
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					if (x * x + z * z <= r * r + 1) {
						BlockPos b = surface(c.getX() + x, c.getZ() + z);
						q().add(b, Blocks.MAGMA_BLOCK.getDefaultState(), BlockQueue.SOLID_ONLY);
						if (world.random.nextInt(4) == 0) {
							q().add(b.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
						}
					}
				}
			}
			if (i < Lamp.BLOBS / 2) {
				q().add(surface(c.getX(), c.getZ()), Blocks.LAVA.getDefaultState(), BlockQueue.ANY);
			}
		}
	}
}
