package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.TemporaryBlocks;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.DiscoBeams;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DISCO BALL. A giant mirror ball is lowered over the city and spins up until its light beams burn
 * everyone they sweep across (harmless: this is a FUN bomb); it cools to blue, slows, stops... silence... then
 * the bass drops, Pigstep starts playing, and the landscape turns into a lit dance floor for five minutes before
 * it all reverts. Beam maths lives in {@link DiscoBeams}.
 */
public class DiscoBallEvent extends WorldEvent {
	/** The dance floor (and the rainbow trees) only last a while: everything reverts after five minutes. */
	private static final int FLOOR_RADIUS = 128, BUDGET = 2500, FLOOR_LIFE = 6000;
	private static final Block[] CONCRETE = {Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
			Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
			Blocks.RED_CONCRETE, Blocks.GREEN_CONCRETE};
	private static final Block[] GLASS = {Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
			Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS,
			Blocks.BLUE_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS};
	private static final Block[] WOOL = {Blocks.MAGENTA_WOOL, Blocks.PINK_WOOL, Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
			Blocks.CYAN_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.BLUE_WOOL, Blocks.PURPLE_WOOL};
	private static final Block[] LIGHTS = {Blocks.SEA_LANTERN, Blocks.GLOWSTONE, Blocks.SHROOMLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT};

	private Vec3d fwd;
	/** Dance-floor columns still to convert, nearest first. */
	private List<int[]> columns;
	private int next;

	@Override
	public boolean tick() {
		int t = age();
		Vec3d g = center();
		if (t == 0) {
			fwd = Kit.dir(data.getFloat("Yaw"));
			WorldCutsceneEvents.begin(this, world, g, "block.armageddon.disco_ball", 7, fwd, DiscoBeams.CINE);
			Vfx.custom(world, FxType.DISCO_BALL, g, g, (float) DiscoBeams.RADIUS, 0, 0, (float) DiscoBeams.HANG, 0, DiscoBeams.TOTAL, Vfx.FAR);
			Vfx.skyTint(world, g, 100000, 0x140828, 0.75F);
			Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
		}
		Vec3d ball = DiscoBeams.ball(g, t);
		if (t < DiscoBeams.DESCEND_END && t % 6 == 0) {
			Kit.sound(world, ball, SoundEvents.BLOCK_CHAIN_PLACE, 24.0F, 0.5F);
		}
		// The spin: a whirr that climbs with the speed.
		double w = DiscoBeams.omega(t) / DiscoBeams.MAX_SPIN;
		if (t >= DiscoBeams.SPIN_START && t < DiscoBeams.STOP && t % 6 == 0) {
			Kit.sound(world, ball, ModSounds.MINIGUN_SPIN, (float) (8 + 40 * w), (float) (0.5 + 1.3 * w));
		}
		if (t >= DiscoBeams.SPIN_START && t < DiscoBeams.STOP && t % 20 == 0) {
			Kit.sound(world, g, ModSounds.DUBSTEP_WUB, 32.0F, (float) (0.7 + 0.5 * w));
		}
		if (t == DiscoBeams.SPIN_FULL - 60) {
			Kit.title(world, g, 100000, Text.literal("FASTER").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), Text.empty());
		}
		if (DiscoBeams.hot(t)) {
			burn(g, t);
		}
		if (t == 300) {
			Vfx.skyTint(world, g, 100000, 0x0A2050, 0.85F);
			Kit.sound(world, ball, ModSounds.FREEZE, 48.0F, 0.6F);
		}
		if (t == DiscoBeams.STOP) {
			Kit.sound(world, ball, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.4F);
			Vfx.skyTint(world, g, 100000, 0x000000, 0.97F);
			Vfx.screen(world, g, 100000, 2, 0.6F, DiscoBeams.DROP - DiscoBeams.STOP - 8);
		}
		if (t == DiscoBeams.DROP - 12) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 64.0F, 0.5F);
		}
		if (t == DiscoBeams.DROP) {
			drop(g);
		}
		if (t > DiscoBeams.DROP) {
			afterparty(g, t - DiscoBeams.DROP);
		}
		if (columns != null && next < columns.size()) {
			buildFloor(g);
		}
		return t >= DiscoBeams.TOTAL;
	}

	// ---------------------------------------------------------------- heat

	/** The beams dazzle rather than burn: whatever they sweep over sparkles and glows, and nobody gets hurt. */
	private void burn(Vec3d g, int t) {
		Vec3d ball = DiscoBeams.ball(g, t);
		List<LivingEntity> near = t % 4 == 0 ? Kit.living(world, g, 230) : List.of();
		for (int i = 0; i < DiscoBeams.BEAMS; i++) {
			Vec3d h = DiscoBeams.hit(g, i, t);
			int x = MathHelper.floor(h.x), z = MathHelper.floor(h.z);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			Vec3d hit = Kit.ground(world, h.x, h.z);
			if (t % 3 == i % 3) {
				world.spawnParticles(ParticleTypes.END_ROD, hit.x, hit.y + 0.5, hit.z, 3, 0.6, 0.3, 0.6, 0.04);
				world.spawnParticles(ParticleTypes.NOTE, hit.x, hit.y + 1.0, hit.z, 1, 0.8, 0.2, 0.8, 1.0);
			}
			for (LivingEntity e : near) {
				if (Kit.creativeOrSpectator(e) || segmentDistance(e.getPos().add(0, e.getHeight() / 2, 0), ball, hit) > 2.6) {
					continue;
				}
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, false));
			}
		}
	}

	private static double segmentDistance(Vec3d p, Vec3d a, Vec3d b) {
		Vec3d ab = b.subtract(a);
		double len = ab.lengthSquared();
		double k = len < 1.0E-6 ? 0 : MathHelper.clamp(p.subtract(a).dotProduct(ab) / len, 0.0, 1.0);
		return p.distanceTo(a.add(ab.multiply(k)));
	}

	// ---------------------------------------------------------------- the drop

	private void drop(Vec3d g) {
		CityDestructionEvents.frames(world, g, 1, 0, 2, 1);
		Kit.sound(world, g, ModSounds.DUBSTEP_DROP, 64.0F, 0.5F);
		Kit.sound(world, g, ModSounds.DUBSTEP_WUB, 64.0F, 0.45F);
		Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.6F);
		// And the music starts: Pigstep, on the records channel, loud enough to cover the whole dance floor.
		world.playSound(null, g.x, g.y + 2, g.z, SoundEvents.MUSIC_DISC_PIGSTEP.value(), net.minecraft.sound.SoundCategory.RECORDS, 12.0F, 1.0F);
		Kit.broadcast(world, g, 400, Text.literal("\u266B Now playing: Lena Raine - Pigstep").formatted(Formatting.LIGHT_PURPLE));
		Vfx.skyTint(world, g, 100000, 0x501070, 0.9F);
		Vfx.shake(world, g, 100000, 3.0F);
		Vfx.fov(world, g, 20.0F, 24);
		Vfx.flash(world, g, 0.35F, 2000, 0xFF60FF);
		int[] cols = {0xFF40FF, 0x40FFFF, 0xFFFF40, 0xFF4040, 0x40FF60};
		for (int k = 0; k < 5; k++) {
			final int kk = k;
			com.armageddon.detonation.DetonationManager.schedule(world, k * 4, () ->
					Vfx.ring(world, g.add(0, 0.5, 0), 260.0F + kk * 40, 6.0F, cols[kk], true, 70 + kk * 10));
		}
		Vfx.dome(world, g, 180.0F, 0xFF80FF, 50);
		Vfx.pillar(world, DiscoBeams.ball(g, DiscoBeams.DROP).add(0, -DiscoBeams.HANG, 0), 6.0F, 0xFFFFFF, true, 80);
		// Everybody bounces.
		for (LivingEntity e : Kit.living(world, g, 170)) {
			if (Kit.creativeOrSpectator(e)) {
				continue;
			}
			Vec3d out = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z);
			double d = out.length();
			out = d < 0.1 ? Vec3d.ZERO : out.multiply(1 / d);
			Kit.push(e, out.multiply(0.7 * (1 - d / 170)).add(0, 1.3, 0));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 140, 0, false, false));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 400, 0, false, false));
		}
		Kit.title(world, g, 100000, Text.literal("DROP").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
				Text.literal("the world is a disco now").formatted(Formatting.AQUA));
		if (Destruction.enabled(world)) {
			columns = new ArrayList<>();
			int r = Destruction.radius(world, FLOOR_RADIUS);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						columns.add(new int[]{dx, dz});
					}
				}
			}
			columns.sort(Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
			next = 0;
		}
	}

	private void afterparty(Vec3d g, int k) {
		if (k % 10 == 0 && k < DiscoBeams.TOTAL - DiscoBeams.DROP - 30) {
			boolean down = k % 20 == 0;
			Kit.sound(world, g, ModSounds.DUBSTEP_WUB, 40.0F, down ? 0.5F : 0.62F);
			if (down) {
				Vfx.ring(world, g.add(0, 0.6, 0), 40.0F + (k % 60), 1.5F, CityDestructionEvents.hsv(k * 0.031F), false, 18);
				// Little hops on the beat for anything standing on the dance floor.
				for (LivingEntity e : Kit.living(world, g, 110)) {
					if (e.isOnGround() && !Kit.creativeOrSpectator(e)) {
						Kit.push(e, new Vec3d(0, 0.42, 0));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------- dance floor

	private static int tileHash(int tx, int tz) {
		int h = tx * 73856093 ^ tz * 19349663;
		h ^= h >>> 13;
		return h & 0x7FFFFFFF;
	}

	private void buildFloor(Vec3d g) {
		int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z);
		int budget = BUDGET;
		BlockPos.Mutable p = new BlockPos.Mutable();
		while (budget > 0 && next < columns.size()) {
			int[] c = columns.get(next++);
			int x = gx + c[0], z = gz + c[1];
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			int tx = Math.floorDiv(x, 3), tz = Math.floorDiv(z, 3);
			int h = tileHash(tx, tz);
			int topLeaves = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			int floorY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			// Trees above the floor turn rainbow: leaves to wool, trunks to concrete, by height.
			for (int y = floorY + 1; y <= Math.min(topLeaves + 1, floorY + 40); y++) {
				p.set(x, y, z);
				BlockState s = world.getBlockState(p);
				if (s.isIn(BlockTags.LEAVES)) {
					TemporaryBlocks.place(world, p.toImmutable(), WOOL[Math.floorMod(y + (h & 3), WOOL.length)].getDefaultState(), FLOOR_LIFE);
					budget--;
				} else if (s.isIn(BlockTags.LOGS)) {
					TemporaryBlocks.place(world, p.toImmutable(), CONCRETE[Math.floorMod(y, CONCRETE.length)].getDefaultState(), FLOOR_LIFE);
					budget--;
				}
			}
			p.set(x, floorY, z);
			BlockState top = world.getBlockState(p);
			if (top.isAir() || !top.getFluidState().isEmpty() || top.isIn(BlockTags.LOGS) || top.isIn(BlockTags.LEAVES)) {
				continue;
			}
			int colour = h % CONCRETE.length;
			if (((tx + tz) & 1) == 0) {
				// Glowing tile: stained glass over a light.
				TemporaryBlocks.place(world, p.toImmutable(), GLASS[colour].getDefaultState(), FLOOR_LIFE);
				p.set(x, floorY - 1, z);
				TemporaryBlocks.place(world, p.toImmutable(), LIGHTS[(h >>> 5) % LIGHTS.length].getDefaultState(), FLOOR_LIFE);
				budget -= 2;
			} else {
				TemporaryBlocks.place(world, p.toImmutable(), CONCRETE[colour].getDefaultState(), FLOOR_LIFE);
				budget--;
			}
		}
	}

	@Override
	public void finish() {
		columns = null;
	}
}
