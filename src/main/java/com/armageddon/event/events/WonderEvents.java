package com.armageddon.event.events;

import com.armageddon.destruction.celestial.BlockQueue;
import com.armageddon.event.Climax;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.WonderShapes;
import com.armageddon.util.WonderShapes.Havoc;
import com.armageddon.util.WonderShapes.Lantern;
import com.armageddon.util.WonderShapes.Shift;
import com.armageddon.vfx.VfxIds;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * The Dimension Shift (a place shifted into the Nether), the Firework Havoc (one giant rocket that sets off a cascade
 * of fireworks) and the Chinese Lantern (lanterns fill the sky and a golden dragon flies over them).
 */
public final class WonderEvents {
	private WonderEvents() {
	}

	/** Effect id for {@link com.armageddon.network.FxType#NEW_FX} (client: WildFx). */
	public static final int FX_LANTERN = 54;

	// ====================================================================== DIMENSION SHIFT

	/**
	 * DIMENSION SHIFT (mythic). A rift tears open in the air and a wave runs out from it: everything it passes is
	 * shifted into the Nether. Grass to nylium, stone to netherrack and blackstone, sand to soul sand, water to lava,
	 * trees to crimson and warped stems with wart for leaves, ores to quartz and nether gold. The sky goes red and ash
	 * falls; the locals arrive.
	 */
	public static class DimensionShift extends WorldEvent {
		private StrongBombEvents.RingSweep sweep;
		private BlockQueue queue;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.dimension_shift");
				queue = BlockQueue.start(world);
				sweep = new StrongBombEvents.RingSweep((int) Shift.R);
				Vfx.play(world, VfxIds.DIM_RIFT, c.add(0, 4, 0), 1.0F);
				Kit.sound(world, c, SoundEvents.BLOCK_PORTAL_TRIGGER, 64.0F, 0.6F);
				Kit.sound(world, c, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.5F);
			}
			if (t % 40 == 0 && t < Shift.END - 40) {
				// The sky over the shifted place stays the Nether's.
				Vfx.skyTint(world, c, Shift.R + 120, 0x5A1008, 0.75F);
			}
			if (t >= Shift.RIFT && t <= Shift.SWEEP_END + 4) {
				double front = Shift.front(t);
				if (queue != null) {
					sweep.advance(front, 500, (dx, dz, d) -> shift(MathHelper.floor(c.x) + dx, MathHelper.floor(c.z) + dz));
				}
				if (t % 4 == 0 && front > 2) {
					Vfx.play(world, VfxIds.DIM_FRONT, c, (float) (front / 20.0));
				}
				if (t % 20 == 0) {
					Kit.sound(world, c, SoundEvents.BLOCK_PORTAL_AMBIENT, 64.0F, 0.5F + (t - Shift.RIFT) * 0.003F);
					Vfx.shake(world, c, Shift.R + 40, 0.5F);
				}
			}
			if (t == Shift.CLOSE) {
				Vfx.play(world, VfxIds.DIM_CLOSE, c.add(0, 4, 0), 1.0F);
				Kit.sound(world, c, SoundEvents.BLOCK_PORTAL_TRAVEL, 48.0F, 1.2F);
				Kit.sound(world, c, SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), 64.0F, 1.0F);
				locals(c);
			}
			if (t > Shift.RIFT && t % 10 == 0) {
				for (ServerPlayerEntity p : world.getPlayers()) {
					if (Math.hypot(p.getX() - c.x, p.getZ() - c.z) < Math.max(12, Shift.front(Math.min(t, Shift.SWEEP_END)))) {
						Vfx.play(world, VfxIds.DIM_ASH, p.getPos(), 1.0F);
					}
				}
			}
			return t >= Shift.END;
		}

		/** One column, from high over the surface to well below it. */
		private void shift(int x, int z) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
			int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			int y1 = Math.min(world.getTopY() - 1, top + 2), y0 = Math.max(world.getBottomY() + 1, ground - (int) Shift.DEPTH);
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (int y = y1; y >= y0; y--) {
				BlockState s = world.getBlockState(p.set(x, y, z));
				if (s.isAir() || s.hasBlockEntity() || s.getHardness(world, p) < 0) {
					continue;
				}
				double h = WonderShapes.hash(x * 31 + y, z * 17 - y);
				BlockState n = nether(s, h, y == ground);
				if (n != null) {
					queue.add(p.toImmutable(), n);
				}
			}
			// A little fire on the new ground, soul fire on the soul sand.
			double h = WonderShapes.hash(x, z);
			if (h < 0.035 && ground + 1 < world.getTopY()) {
				BlockState below = world.getBlockState(p.set(x, ground, z));
				boolean soul = below.isOf(Blocks.SAND) || below.isOf(Blocks.RED_SAND) || below.isOf(Blocks.GRAVEL);
				queue.add(new BlockPos(x, ground + 1, z), (soul ? Blocks.SOUL_FIRE : Blocks.FIRE).getDefaultState(), BlockQueue.AIR);
			}
		}

		/** The Nether's version of a block, or null to leave it. */
		static BlockState nether(BlockState s, double h, boolean surface) {
			if (!s.getFluidState().isEmpty() && s.getFluidState().isIn(FluidTags.WATER)) {
				return s.isOf(Blocks.WATER) && s.getFluidState().isStill() ? (h < 0.9 ? Blocks.LAVA : Blocks.MAGMA_BLOCK).getDefaultState()
						: Blocks.MAGMA_BLOCK.getDefaultState();
			}
			if (s.isOf(Blocks.ICE) || s.isOf(Blocks.PACKED_ICE) || s.isOf(Blocks.BLUE_ICE) || s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.POWDER_SNOW)) {
				return (h < 0.5 ? Blocks.MAGMA_BLOCK : Blocks.BASALT).getDefaultState();
			}
			if (s.isOf(Blocks.SNOW)) {
				return Blocks.AIR.getDefaultState();
			}
			if (s.isIn(BlockTags.LOGS)) {
				BlockState stem = (h < 0.5 ? Blocks.CRIMSON_STEM : Blocks.WARPED_STEM).getDefaultState();
				return s.contains(Properties.AXIS) ? stem.with(Properties.AXIS, s.get(Properties.AXIS)) : stem;
			}
			if (s.isIn(BlockTags.LEAVES)) {
				return (h < 0.06 ? Blocks.SHROOMLIGHT : h < 0.55 ? Blocks.NETHER_WART_BLOCK : Blocks.WARPED_WART_BLOCK).getDefaultState();
			}
			if (s.isIn(BlockTags.PLANKS)) {
				return (h < 0.5 ? Blocks.CRIMSON_PLANKS : Blocks.WARPED_PLANKS).getDefaultState();
			}
			if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.PODZOL) || s.isOf(Blocks.MYCELIUM) || s.isOf(Blocks.MOSS_BLOCK)) {
				return (surface ? (h < 0.5 ? Blocks.CRIMSON_NYLIUM : Blocks.WARPED_NYLIUM) : Blocks.NETHERRACK).getDefaultState();
			}
			if (s.isOf(Blocks.DIRT) || s.isOf(Blocks.COARSE_DIRT) || s.isOf(Blocks.ROOTED_DIRT) || s.isOf(Blocks.FARMLAND) || s.isOf(Blocks.DIRT_PATH)
					|| s.isOf(Blocks.CLAY) || s.isOf(Blocks.MUD)) {
				return Blocks.NETHERRACK.getDefaultState();
			}
			if (s.isOf(Blocks.SAND) || s.isOf(Blocks.RED_SAND) || s.isOf(Blocks.GRAVEL)) {
				return (h < 0.55 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL).getDefaultState();
			}
			if (s.isOf(Blocks.SANDSTONE) || s.isOf(Blocks.RED_SANDSTONE) || s.isOf(Blocks.SMOOTH_SANDSTONE) || s.isOf(Blocks.CUT_SANDSTONE)) {
				return Blocks.SMOOTH_BASALT.getDefaultState();
			}
			if (s.isOf(Blocks.DEEPSLATE) || s.isOf(Blocks.TUFF) || s.isOf(Blocks.COBBLED_DEEPSLATE)) {
				return (h < 0.5 ? Blocks.BLACKSTONE : Blocks.BASALT).getDefaultState();
			}
			if (s.isOf(Blocks.STONE) || s.isOf(Blocks.ANDESITE) || s.isOf(Blocks.DIORITE) || s.isOf(Blocks.GRANITE) || s.isOf(Blocks.CALCITE)) {
				return (h < 0.12 ? Blocks.BLACKSTONE : h < 0.22 ? Blocks.BASALT : h < 0.24 ? Blocks.GLOWSTONE : Blocks.NETHERRACK).getDefaultState();
			}
			if (s.isOf(Blocks.COBBLESTONE) || s.isOf(Blocks.MOSSY_COBBLESTONE)) {
				return Blocks.BLACKSTONE.getDefaultState();
			}
			if (s.isOf(Blocks.STONE_BRICKS) || s.isOf(Blocks.MOSSY_STONE_BRICKS) || s.isOf(Blocks.CRACKED_STONE_BRICKS) || s.isOf(Blocks.BRICKS)) {
				return (h < 0.5 ? Blocks.NETHER_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState();
			}
			if (s.isOf(Blocks.COAL_ORE) || s.isOf(Blocks.IRON_ORE) || s.isOf(Blocks.COPPER_ORE) || s.isOf(Blocks.DEEPSLATE_COAL_ORE)
					|| s.isOf(Blocks.DEEPSLATE_IRON_ORE) || s.isOf(Blocks.DEEPSLATE_COPPER_ORE) || s.isOf(Blocks.REDSTONE_ORE) || s.isOf(Blocks.LAPIS_ORE)) {
				return Blocks.NETHER_QUARTZ_ORE.getDefaultState();
			}
			if (s.isOf(Blocks.GOLD_ORE) || s.isOf(Blocks.DEEPSLATE_GOLD_ORE)) {
				return Blocks.NETHER_GOLD_ORE.getDefaultState();
			}
			if (s.isOf(Blocks.DIAMOND_ORE) || s.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
				return Blocks.ANCIENT_DEBRIS.getDefaultState();
			}
			if (s.isIn(BlockTags.FLOWERS) || s.isIn(BlockTags.SAPLINGS) || s.isIn(BlockTags.CROPS) || s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS)
					|| s.isOf(Blocks.FERN) || s.isOf(Blocks.LARGE_FERN) || s.isOf(Blocks.SWEET_BERRY_BUSH) || s.isOf(Blocks.DEAD_BUSH)) {
				return (h < 0.25 ? Blocks.CRIMSON_ROOTS : h < 0.45 ? Blocks.WARPED_ROOTS : h < 0.55 ? Blocks.CRIMSON_FUNGUS : h < 0.62 ? Blocks.WARPED_FUNGUS
						: Blocks.AIR).getDefaultState();
			}
			return null;
		}

		/** The Nether's people arrive to see what has happened to their place. */
		private void locals(Vec3d c) {
			for (int i = 0; i < 5; i++) {
				Vec3d p = Climax.around(world, c, 8, Shift.R * 0.7);
				ZombifiedPiglinEntity z = EntityType.ZOMBIFIED_PIGLIN.create(world);
				if (z == null) {
					continue;
				}
				z.refreshPositionAndAngles(p.x, p.y + 0.2, p.z, world.random.nextFloat() * 360, 0);
				z.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
				world.spawnEntity(z);
				Vfx.play(world, VfxIds.DIM_RIFT, p.add(0, 1, 0), 0.15F);
			}
		}
	}

	// ====================================================================== FIREWORK HAVOC

	/**
	 * FIREWORK HAVOC (legendary). One firework the size of a tower climbs screaming into the sky and bursts, and out of
	 * the burst comes a cascade: a hundred and fifty real fireworks fired off at every angle, more going up from the
	 * ground in fountains and roman candles, and at the end a golden willow over everything.
	 */
	public static class FireworkHavoc extends WorldEvent {
		private static final int[] PALETTE = {0xFF3040, 0xFF9020, 0xFFE040, 0x40FF70, 0x30C0FF, 0x6050FF, 0xE040FF, 0xFFFFFF};

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.firework_havoc");
				Vfx.play(world, VfxIds.HAVOC_LAUNCH, c, 1.0F);
				Kit.sound(world, c, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 64.0F, 0.4F);
				Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 24.0F, 1.4F);
			}
			if (t < Havoc.CLIMB) {
				Vec3d r = c.add(0, Havoc.height(t), 0);
				Vfx.play(world, VfxIds.HAVOC_TRAIL, r, 1.0F + t * 0.02F);
				if (t % 10 == 0) {
					Kit.sound(world, r, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 48.0F, 0.5F + t * 0.01F);
				}
			}
			Vec3d apex = c.add(0, Havoc.APEX, 0);
			if (t == Havoc.CLIMB) {
				Vfx.play(world, VfxIds.HAVOC_BURST, apex, 1.0F);
				Vfx.flash(world, apex, 0.6F, 400, 0xFFF0E0);
				Vfx.shake(world, c, 200, 1.2F);
				Kit.sound(world, apex, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 128.0F, 0.5F);
				Kit.sound(world, apex, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.6F);
				Kit.sound(world, apex, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 128.0F, 0.8F);
			}
			if (t > Havoc.CLIMB && t <= Havoc.CASCADE_END) {
				// The cascade: real fireworks fired out of the burst at every angle.
				for (int i = 0; i < 2 + (t % 3 == 0 ? 1 : 0); i++) {
					rocket(apex.add(world.random.nextGaussian() * 6, world.random.nextGaussian() * 4, world.random.nextGaussian() * 6), 0.6 + world.random.nextDouble() * 1.0, true);
				}
			}
			if (t >= Havoc.GROUND && t < Havoc.GROUND_END && t % 4 == 0) {
				// On the ground: fountains and roman candles going off all round.
				Vec3d p = Climax.around(world, c, 6, 55);
				Vfx.play(world, VfxIds.HAVOC_FOUNTAIN, p, 1.0F);
				Kit.sound(world, p, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 24.0F, 0.8F + world.random.nextFloat() * 0.4F);
				Kit.damage(world, p, 2.5, 4.0F, ModDamageTypes.PLASMA, null, owner());
				if (world.random.nextInt(3) == 0) {
					rocket(p.add(0, 1, 0), 0.9 + world.random.nextDouble() * 0.5, false);
				}
			}
			if (t == Havoc.FINALE) {
				Vfx.play(world, VfxIds.HAVOC_WILLOW, apex, 1.0F);
				Vfx.flash(world, apex, 0.5F, 400, 0xFFD890);
				Kit.sound(world, apex, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 128.0F, 0.4F);
				Kit.sound(world, apex, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 128.0F, 0.6F);
			}
			return t >= Havoc.END;
		}

		/** One real firework, aimed somewhere random (outward and mostly level from the burst; up from the ground). */
		private void rocket(Vec3d at, double speed, boolean fromBurst) {
			int a = PALETTE[world.random.nextInt(PALETTE.length)], b = PALETTE[world.random.nextInt(PALETTE.length)];
			FireworkExplosionComponent.Type[] shapes = FireworkExplosionComponent.Type.values();
			FireworkExplosionComponent boom = new FireworkExplosionComponent(shapes[world.random.nextInt(shapes.length)], IntList.of(a, b), IntList.of(b),
					world.random.nextBoolean(), world.random.nextBoolean());
			ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
			stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(boom)));
			FireworkRocketEntity e = new FireworkRocketEntity(world, stack, at.x, at.y, at.z, true);
			Vec3d dir;
			if (fromBurst) {
				double yaw = world.random.nextDouble() * MathHelper.TAU, pitch = (world.random.nextDouble() - 0.6) * Math.PI * 0.6;
				dir = new Vec3d(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
			} else {
				dir = new Vec3d(world.random.nextGaussian() * 0.25, 1, world.random.nextGaussian() * 0.25).normalize();
			}
			e.setVelocity(dir.multiply(speed));
			world.spawnEntity(e);
		}
	}

	// ====================================================================== CHINESE LANTERN

	/**
	 * CHINESE LANTERN (fun). Hundreds of paper lanterns rise out of the ground and fill the sky, and a golden, glassy
	 * dragon flies slowly round over them, trailing sparkles; at the end it climbs and bursts into gold. Harmless.
	 */
	public static class ChineseLantern extends WorldEvent {
		/** A pentatonic tune for the flute, one note every six ticks while the lanterns rise. */
		private static final float[] TUNE = {1.0F, 1.12F, 1.26F, 1.5F, 1.68F, 1.5F, 1.26F, 1.12F, 1.0F, 0.84F, 1.0F, 1.26F, 1.12F, 1.0F, 0.84F, 0.75F};

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.chinese_lantern");
				Vfx.newFx(world, FX_LANTERN, c, c, 1.0F, 0xFFC040, 0, 0, Lantern.END);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_USE, 48.0F, 0.6F);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 48.0F, 0.5F);
			}
			if (t < Lantern.BURST && t % 6 == 0) {
				Kit.sound(world, c.add(0, 4, 0), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), 16.0F, TUNE[(t / 6) % TUNE.length]);
			}
			if (t % 48 == 24 && t < Lantern.BURST) {
				Kit.sound(world, c, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 24.0F, 0.8F);
			}
			if (t == Lantern.BURST) {
				Vec3d head = c.add(Lantern.head(t));
				Kit.sound(world, head, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 96.0F, 0.6F);
				Kit.sound(world, head, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 96.0F, 0.9F);
				Kit.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.6F);
			}
			return t >= Lantern.END;
		}
	}
}
