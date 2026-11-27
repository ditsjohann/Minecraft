package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.WorldBombShapes;
import com.armageddon.util.WorldBombShapes.Creeper;
import com.armageddon.util.WorldBombShapes.Wyrm;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Two WORLD DESTRUCTION bombs: the MEGA CREEPER (a creeper the height of a mountain goes off and rains creepers over
 * the world) and the ELDER WYRM (a dragon of black scale and magma breathes canyons of fire across the world).
 * They build on the plasma bombs' base: flying debris, trenches, craters.
 */
public final class WorldBombEvents {
	private WorldBombEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: WorldBombFx). */
	public static final int FX_CREEPER = 70, FX_WYRM = 71;

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	// ====================================================================== MEGA CREEPER

	/**
	 * MEGA CREEPER. The sky goes a sick green and the ground heaves; a creeper as tall as a mountain rises out of it,
	 * shedding earth, and stands there staring. Then the hiss: it swells and flashes white, slowly, then faster and
	 * faster - and goes off. A crater seventy blocks across, a white-out, a shockwave that strips the land for three
	 * hundred blocks; and the blast rains creepers down over the whole of it, each going off where it lands. When the
	 * dust settles its face is pressed into the floor of the crater.
	 */
	public static class MegaCreeper extends PlasmaEvents.PlasmaBase {
		private TerrainJob wave;
		private final List<CreeperEntity> falling = new ArrayList<>();
		private int dropped;
		private boolean faced;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("mega_creeper");
				Vfx.newFx(world, FX_CREEPER, base, heading, 1.0F, 0x40C040, 0, 0, Creeper.END);
				Kit.sound(world, base, SoundEvents.AMBIENT_CAVE.value(), 128.0F, 0.5F);
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 96.0F, 0.6F);
			}
			if (t < Creeper.BOOM && t % 40 == 0) {
				Vfx.skyTint(world, base, Creeper.R + 200, 0x60C040, 0.3F + 0.4F * (float) Creeper.fuse(t));
			}
			if (t == Creeper.RISE) {
				Vfx.play(world, VfxIds.CREEPER_RISE, base, 1.6F);
				Vfx.shake(world, base, 300, 1.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.3F);
				if (Destruction.enabled(world)) {
					Kit.ringCrack(world, base, 26, 3, null, 0.7F);
				}
			}
			if (t > Creeper.RISE && t < Creeper.RISEN) {
				if (t % 5 == 0) {
					// Earth sliding off it as it comes up, thrown out round its feet.
					Vec3d at = base.add(FossilShapes.frame(heading, (FossilShapes.hash(t, 1) - 0.5) * 30, 0, (FossilShapes.hash(t, 2) - 0.5) * 20));
					for (FallingBlockEntity f : Kit.lift(world, at, 8, 10, true, false)) {
						Vec3d out = new Vec3d(f.getX() - base.x, 0, f.getZ() - base.z);
						out = out.lengthSquared() < 1.0E-3 ? heading : out.normalize();
						debris.throwOne(f, out.multiply(0.5 + seed(f, 1) * 0.8).add(0, 0.6 + seed(f, 2) * 0.8, 0));
					}
					Vfx.play(world, VfxIds.CREEPER_RISE, ground(at.x, at.z), 0.7F);
					Vfx.shake(world, base, 200, 0.6F);
				}
				if (t % 12 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 128.0F, 0.4F);
					Kit.sound(world, base, SoundEvents.BLOCK_GRAVEL_BREAK, 128.0F, 0.3F);
				}
			}
			if (t >= Creeper.HISS && t < Creeper.BOOM) {
				double f = Creeper.fuse(t);
				int every = Math.max(6, (int) (40 * (1 - f)));
				if ((t - Creeper.HISS) % every == 0) {
					Kit.sound(world, base.add(0, 60, 0), SoundEvents.ENTITY_CREEPER_PRIMED, 128.0F, 0.45F + 0.35F * (float) f);
				}
				if (t % 6 == 0) {
					Vfx.shake(world, base, 300, (float) (0.3 + 1.2 * f));
				}
			}
			if (t == Creeper.BOOM) {
				boom();
			}
			if (wave != null) {
				wave.limit = t > Creeper.BOOM + 70 ? Creeper.R + 1 : front(t);
				wave.step(4500);
			}
			if (t > Creeper.BOOM && t < Creeper.BOOM + 70 && t % 2 == 0) {
				// The shockwave throws the land up ahead of it.
				double front = front(t);
				for (int i = 0; i < 2; i++) {
					double a = FossilShapes.hash(t, 40 + i) * MathHelper.TAU;
					Vec3d p = ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front);
					for (FallingBlockEntity fb : Kit.lift(world, p, 6, 8, true, false)) {
						debris.throwOne(fb, new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(1.2 + seed(fb, 3)).add(0, 0.7 + seed(fb, 4) * 0.6, 0));
					}
				}
				for (LivingEntity e : Kit.living(world, base, front + 3)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
					if (d > front - 8) {
						double k = 1 - d / (Creeper.R + 20);
						Vec3d out = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z).normalize();
						Kit.push(e, out.multiply(2.4 * k).add(0, 0.9 * k, 0));
						e.damage(world.getDamageSources().explosion(null, null), (float) (30 * k));
					}
				}
			}
			// The creeper rain.
			while (dropped < Creeper.CREEPERS && t >= Creeper.drop(dropped)[2]) {
				spawnCreeper(dropped++);
			}
			falling.removeIf(c -> {
				if (!c.isAlive()) {
					return true;
				}
				c.fallDistance = 0;
				if (c.isOnGround() || c.isTouchingWater()) {
					c.ignite();
					return true;
				}
				if (age() % 3 == 0) {
					Vfx.play(world, VfxIds.CREEPER_FALL, c.getPos(), c.getVelocity(), 1.0F, 0);
				}
				return false;
			});
			if (t >= Creeper.FACE && !faced && !DetonationManager.isBusy()) {
				face();
			}
			debris.tick(world, 0x60C040, p -> impact(p, 0x60C040));
			drain();
			boolean waited = t >= Creeper.END + 1200;
			return t >= Creeper.END && (faced || waited) && falling.isEmpty() && (wave == null || wave.isDone()) && debris.settled();
		}

		private static double front(double t) {
			double u = MathHelper.clamp((t - Creeper.BOOM) / 70.0, 0.0, 1.0);
			return Creeper.CRATER * 0.8 + (Creeper.R - Creeper.CRATER * 0.8) * (1 - (1 - u) * (1 - u));
		}

		/** It goes off: a crater, a white-out, everything near killed, the shockwave set rolling. */
		private void boom() {
			Vec3d c = base.add(0, 40, 0);
			Vfx.play(world, VfxIds.MEGA_BOOM, base, 1.0F);
			Vfx.flash(world, c, 1.0F, 1200, 0xF4FFF0);
			Vfx.screen(world, c, 600, 1, 0.8F, 14);
			Vfx.shake(world, c, 900, 3.0F);
			Kit.sound(world, c, ModSounds.NUKE_DETONATE, 256.0F, 0.5F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.3F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.5F);
			Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 256.0F, 0.4F);
			bigCrater(base, Creeper.CRATER);
			for (LivingEntity e : Kit.living(world, base, Creeper.CRATER + 30)) {
				if (!Kit.creativeOrSpectator(e)) {
					double d = e.getPos().distanceTo(base);
					e.damage(world.getDamageSources().explosion(null, null), (float) (120 * (1 - d / (Creeper.CRATER + 40))));
				}
			}
			if (Destruction.enabled(world)) {
				wave = TerrainJob.disc(world, base, Creeper.CRATER * 0.8, Creeper.R, this::strip);
				wave.limit = 0;
			}
		}

		/** One creeper of the rain, high over the land; a few of them charged. */
		private void spawnCreeper(int i) {
			double[] d = Creeper.drop(i);
			Vec3d at = at(d[0], 0, d[1]);
			Vec3d g = ground(at.x, at.z);
			CreeperEntity c = EntityType.CREEPER.create(world);
			if (c == null) {
				return;
			}
			c.refreshPositionAndAngles(g.x, g.y + 36 + WorldBombShapes.hash(i, 3) * 20, g.z, (float) (WorldBombShapes.hash(i, 4) * 360), 0);
			if (WorldBombShapes.hash(i, 5) < 0.18) {
				NbtCompound n = new NbtCompound();
				c.writeCustomDataToNbt(n);
				n.putBoolean("powered", true);
				c.readCustomDataFromNbt(n);
			}
			Kit.velocity(c, new Vec3d((WorldBombShapes.hash(i, 6) - 0.5) * 0.6, -0.4, (WorldBombShapes.hash(i, 7) - 0.5) * 0.6));
			world.spawnEntity(c);
			falling.add(c);
			if (i % 6 == 0) {
				Kit.sound(world, g.add(0, 30, 0), SoundEvents.ENTITY_CREEPER_PRIMED, 64.0F, 0.9F);
			}
		}

		/** Swept by the shockwave: trees blown down, plants and leaves stripped, the ground scoured. */
		private void strip(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s) || s.isIn(BlockTags.LOGS) && y > surf + 1) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.PODZOL) || g.isOf(Blocks.MYCELIUM)) {
				double n = c.noise(101);
				c.set(surf, (n < 0.5 ? Blocks.COARSE_DIRT : n < 0.8 ? Blocks.DIRT : Blocks.GRAVEL).getDefaultState());
			}
		}

		/** Its face, pressed into the crater floor: eight by eight, read from above, the top of it toward where it faced. */
		private void face() {
			faced = true;
			Kit.sound(world, base, SoundEvents.ENTITY_CREEPER_HURT, 128.0F, 0.3F);
			if (!Destruction.enabled(world)) {
				return;
			}
			int half = 4 * Creeper.FACE_PX;
			Block[] green = {Blocks.LIME_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.LIME_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.MOSS_BLOCK, Blocks.LIME_CONCRETE};
			for (int x = -half - 2; x < half + 2; x++) {
				for (int z = -half - 2; z < half + 2; z++) {
					Vec3d w = at(x + 0.5, 0, z + 0.5);
					int bx = MathHelper.floor(w.x), bz = MathHelper.floor(w.z);
					BlockPos top = surface(bx, bz);
					if (x < -half || x >= half || z < -half || z >= half) {
						// A black frame round it.
						q().add(top, Blocks.BLACK_CONCRETE.getDefaultState(), BlockQueue.ANY);
						continue;
					}
					int px = (z + half) / Creeper.FACE_PX, py = (half - 1 - x) / Creeper.FACE_PX;
					Block b = Creeper.face(px, py) ? Blocks.BLACK_CONCRETE : green[(int) (FossilShapes.hash(px * 13 + 1, py * 7 + 3) * green.length)];
					q().add(top, b.getDefaultState(), BlockQueue.ANY);
				}
			}
		}
	}

	// ====================================================================== ELDER WYRM

	/**
	 * ELDER WYRM. The sky turns to blood and ash begins to fall. The ground splits for a hundred and forty blocks and a
	 * dragon of black scale and magma, its wings a hundred and eighty blocks across, claws its way up out of the
	 * world and roars. It takes to the sky and makes three passes low over the land, breathing rivers of fire that
	 * carve canyons four hundred blocks long, molten at the bottom, the land burning either side. It hovers over the
	 * middle and beats its wings: a gale that flattens the forests for a hundred and fifty blocks. It lands, its chest
	 * glowing white-hot, and breathes a nova of fire straight down: a crater, and a firestorm that sets everything
	 * alight for a hundred and twenty blocks. Then it climbs into the red sky and is gone.
	 */
	public static class ElderWyrm extends PlasmaEvents.PlasmaBase {
		private TerrainJob gale, storm;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("elder_wyrm");
				Vfx.newFx(world, FX_WYRM, base, heading, 1.0F, 0xFF4010, 0, 0, Wyrm.END);
				Vfx.play(world, VfxIds.WYRM_ASH, base, 1.0F);
				Kit.sound(world, base, ModSounds.NUKE_RUMBLE, 128.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.AMBIENT_CAVE.value(), 128.0F, 0.4F);
			}
			if (t < Wyrm.END - 60 && t % 40 == 0) {
				Vfx.skyTint(world, base, 600, 0xB01808, 0.7F);
			}
			Vec3d body = at(Wyrm.pos(t).x, Wyrm.pos(t).y, Wyrm.pos(t).z);
			if (t == Wyrm.SPLIT) {
				Vfx.shake(world, base, 400, 1.8F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.3F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_EMERGE, 128.0F, 0.4F);
				if (Destruction.enabled(world)) {
					Kit.fissure(world, base.subtract(heading.multiply(Wyrm.SPLIT_LEN * 0.75)), heading, (int) Wyrm.SPLIT_LEN, 16, 14, Blocks.MAGMA_BLOCK.getDefaultState());
				}
				for (int i = 0; i <= 10; i++) {
					Vec3d p = base.add(heading.multiply(-Wyrm.SPLIT_LEN * 0.75 + Wyrm.SPLIT_LEN * i / 10.0));
					Vfx.play(world, VfxIds.WYRM_RISE, ground(p.x, p.z), 1.2F);
				}
			}
			if (t > Wyrm.EMERGE && t < Wyrm.ROAR && t % 6 == 0) {
				Vec3d g = ground(body.x, body.z);
				Vfx.play(world, VfxIds.WYRM_RISE, g, 1.4F);
				Vfx.shake(world, g, 300, 1.0F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_DIG, 128.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.4F);
				for (FallingBlockEntity f : Kit.lift(world, g, 14, 14, true, false)) {
					Vec3d out = new Vec3d(f.getX() - g.x, 0, f.getZ() - g.z);
					out = out.lengthSquared() < 1.0E-3 ? heading : out.normalize();
					debris.throwOne(f, out.multiply(0.6 + seed(f, 1)).add(0, 0.9 + seed(f, 2) * 0.8, 0));
				}
			}
			if (t == Wyrm.ROAR) {
				Vec3d head = body.add(heading.multiply(30)).add(0, 30, 0);
				Vfx.play(world, VfxIds.WYRM_ROAR, head, heading, 1.0F, 0);
				Vfx.shake(world, head, 500, 2.0F);
				Kit.sound(world, head, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 256.0F, 0.4F);
				Kit.sound(world, head, SoundEvents.ENTITY_RAVAGER_ROAR, 256.0F, 0.3F);
				Kit.sound(world, head, SoundEvents.ENTITY_WARDEN_ROAR, 256.0F, 0.4F);
				for (LivingEntity e : Kit.living(world, head, 90)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 2));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
						Kit.push(e, e.getPos().subtract(head).normalize().multiply(1.2).add(0, 0.4, 0));
					}
				}
			}
			if (t == Wyrm.TAKEOFF + 6) {
				Kit.sound(world, body, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 256.0F, 0.4F);
				Vfx.play(world, VfxIds.WYRM_GALE, ground(body.x, body.z), 0.8F);
				for (int i = 0; i < 6; i++) {
					double a = i * MathHelper.TAU / 6;
					Vec3d p = ground(body.x + Math.cos(a) * 20, body.z + Math.sin(a) * 20);
					for (FallingBlockEntity f : Kit.lift(world, p, 6, 6, true, false)) {
						debris.throwOne(f, new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(1.4).add(0, 0.6, 0));
					}
				}
			}
			if (!Wyrm.grounded(t) && t % 22 == 0) {
				Kit.sound(world, body, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 256.0F, 0.45F);
			}
			int k = Wyrm.breathing(t);
			if (k >= 0) {
				breathe(k, t);
			}
			for (int b : Wyrm.BEATS) {
				if (t == b) {
					beat();
				}
			}
			if (gale != null) {
				gale.limit = Wyrm.gale(t);
				gale.step(4500);
			}
			if (t == Wyrm.LAND) {
				Vfx.play(world, VfxIds.WYRM_RISE, base, 2.0F);
				Vfx.shake(world, base, 400, 1.8F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 192.0F, 0.3F);
				Kit.sound(world, base, SoundEvents.ENTITY_RAVAGER_STEP, 192.0F, 0.3F);
				if (Destruction.enabled(world)) {
					Kit.ringCrack(world, base, 30, 3, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
				}
			}
			if (t >= Wyrm.CHARGE && t < Wyrm.NOVA && t % 10 == 0) {
				float c = (float) Wyrm.charge(t);
				Kit.sound(world, body, SoundEvents.BLOCK_FIRE_AMBIENT, 192.0F, 0.4F + c);
				Kit.sound(world, body, SoundEvents.BLOCK_BEACON_AMBIENT, 192.0F, 0.5F + c);
				Vfx.shake(world, body, 300, 0.4F + c);
			}
			if (t == Wyrm.NOVA) {
				nova();
			}
			if (storm != null) {
				storm.limit = Wyrm.firestorm(t);
				storm.step(4500);
			}
			if (t > Wyrm.NOVA && t < Wyrm.NOVA + 60) {
				double front = Wyrm.firestorm(t);
				Vec3d c = novaPoint();
				for (LivingEntity e : Kit.living(world, c, front + 3)) {
					if (!Kit.creativeOrSpectator(e) && e.getPos().distanceTo(c) > front - 10) {
						Vec3d out = new Vec3d(e.getX() - c.x, 0, e.getZ() - c.z).normalize();
						Kit.push(e, out.multiply(1.6).add(0, 0.6, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 20.0F);
						e.setOnFireFor(15);
					}
				}
			}
			if (t == Wyrm.DEPART + 20) {
				Kit.sound(world, body, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 256.0F, 0.5F);
				Kit.sound(world, body, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 256.0F, 0.4F);
			}
			debris.tick(world, 0xFF5010, p -> impact(p, 0xFF5010));
			drain();
			return t >= Wyrm.END && (gale == null || gale.isDone()) && (storm == null || storm.isDone()) && debris.settled();
		}

		private Vec3d novaPoint() {
			Vec3d p = at(Wyrm.NOVA_AHEAD, 0, 0);
			return ground(p.x, p.z);
		}

		/** A breath of fire striking the land: a molten canyon carved under it, the land either side set ablaze. */
		private void breathe(int k, int t) {
			Vec3d l = Wyrm.breathHit(k, t);
			Vec3d w = at(l.x, 0, l.z);
			Vec3d g = ground(w.x, w.z);
			if (t % 2 == 0) {
				Vfx.play(world, VfxIds.WYRM_SCORCH, g, FossilShapes.frame(heading, Wyrm.runDir(k)), 1.0F, 0);
			}
			if (t % 6 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_FIRE_AMBIENT, 192.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_BLAZE_SHOOT, 192.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
				Vfx.shake(world, g, 160, 1.0F);
			}
			List<Vec3d> pts = new ArrayList<>();
			pts.add(g);
			trench(pts, Wyrm.CANYON_W, (int) Wyrm.CANYON_D, 0.22);
			if (Destruction.enabled(world)) {
				for (int i = 0; i < 26; i++) {
					double a = WorldBombShapes.hash(t * 31 + i, 1) * MathHelper.TAU, r = Wyrm.CANYON_W + 1 + WorldBombShapes.hash(t * 31 + i, 2) * Wyrm.SCORCH;
					burnColumn(MathHelper.floor(g.x + Math.cos(a) * r), MathHelper.floor(g.z + Math.sin(a) * r));
				}
			}
			for (LivingEntity e : Kit.living(world, g, Wyrm.SCORCH + 4)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 30.0F);
					e.setOnFireFor(15);
				}
			}
		}

		/** A column of land caught in the fire: the treetop burning, the ground scorched and alight. */
		private void burnColumn(int x, int z) {
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
			BlockPos p = new BlockPos(x, top, z);
			BlockState s = world.getBlockState(p);
			if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
				q().add(p, Blocks.FIRE.getDefaultState(), BlockQueue.ANY);
				return;
			}
			if (FossilEvents2.earth(s)) {
				double h = FossilShapes.hash(x, z);
				q().add(p, (h < 0.3 ? Blocks.NETHERRACK : h < 0.6 ? Blocks.COARSE_DIRT : h < 0.85 ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK).getDefaultState(), BlockQueue.ANY);
				q().add(p.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}

		/** A wingbeat over the middle: a gale rolls out, flattening the forest, throwing everything. */
		private void beat() {
			Vfx.play(world, VfxIds.WYRM_GALE, base, 2.0F);
			Vfx.shake(world, base, 400, 1.6F);
			Kit.sound(world, base, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 256.0F, 0.3F);
			Kit.sound(world, base, SoundEvents.ITEM_ELYTRA_FLYING, 256.0F, 0.5F);
			if (gale == null && Destruction.enabled(world)) {
				gale = TerrainJob.disc(world, base, Wyrm.GALE, this::flatten);
				gale.limit = 0;
			}
			for (LivingEntity e : Kit.living(world, base, Wyrm.GALE)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d out = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z);
				double d = Math.max(1, out.length());
				double k = 1 - d / Wyrm.GALE;
				Kit.push(e, out.multiply(1 / d).multiply(2.2 * k + 0.3).add(0, 0.5 * k, 0));
				e.damage(world.getDamageSources().generic(), (float) (8 * k));
			}
			for (int i = 0; i < 8; i++) {
				double a = FossilShapes.hash(age(), i) * MathHelper.TAU, r = 10 + FossilShapes.hash(i, age()) * 60;
				Vec3d p = ground(base.x + Math.cos(a) * r, base.z + Math.sin(a) * r);
				for (FallingBlockEntity f : Kit.lift(world, p, 5, 6, true, false)) {
					debris.throwOne(f, new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(1.6 + seed(f, 5)).add(0, 0.5 + seed(f, 6) * 0.5, 0));
				}
			}
		}

		/** Swept by the gale: every tree blown flat, the leaves and plants torn away. */
		private void flatten(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s) || s.isIn(BlockTags.LOGS) && y > surf + 1) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
		}

		/** The nova: breathed straight down in front of it - a crater, a white-out, the firestorm. */
		private void nova() {
			Vec3d c = novaPoint();
			Vfx.play(world, VfxIds.WYRM_NOVA, c, 1.0F);
			Vfx.flash(world, c.add(0, 20, 0), 1.0F, 1000, 0xFFE0B0);
			Vfx.screen(world, c, 500, 1, 0.7F, 12);
			Vfx.shake(world, c, 800, 2.8F);
			Kit.sound(world, c, ModSounds.NUKE_DETONATE, 256.0F, 0.5F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 256.0F, 0.3F);
			Kit.sound(world, c, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 256.0F, 0.6F);
			Kit.sound(world, c, SoundEvents.ENTITY_BLAZE_SHOOT, 256.0F, 0.3F);
			bigCrater(c, Wyrm.CRATER);
			for (LivingEntity e : Kit.living(world, c, Wyrm.CRATER + 20)) {
				if (!Kit.creativeOrSpectator(e)) {
					double d = e.getPos().distanceTo(c);
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), (float) (100 * (1 - d / (Wyrm.CRATER + 30))));
					e.setOnFireFor(20);
				}
			}
			if (Destruction.enabled(world)) {
				storm = TerrainJob.disc(world, c, Wyrm.CRATER * 0.7, Wyrm.FIRESTORM, this::firestorm);
				storm.limit = 0;
			}
		}

		/** The firestorm reaching a column: trees burned to charcoal, the ground scorched, everything alight. */
		private void firestorm(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 40); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				} else if (s.isIn(BlockTags.LOGS)) {
					c.set(y, (c.noise(y) < 0.5 ? Blocks.BASALT : Blocks.BLACKSTONE).getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (!FossilEvents2.earth(g)) {
				return;
			}
			double n = c.noise(111);
			c.set(surf, (n < 0.35 ? Blocks.NETHERRACK : n < 0.6 ? Blocks.COARSE_DIRT : n < 0.8 ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK).getDefaultState());
			if (c.noise(113) < 0.35 && c.get(surf + 1).isAir()) {
				c.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		}
	}
}
