package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.Climax;
import com.armageddon.event.Kit;
import com.armageddon.event.Ruin;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.SciShapes;
import com.armageddon.util.WildShapes.Cactus;
import com.armageddon.util.WildShapes.Forge;
import com.armageddon.util.WildShapes.Goo;
import com.armageddon.util.WildShapes.Infrit;
import com.armageddon.util.WildShapes.Koi;
import com.armageddon.vfx.VfxIds;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The goo bomb, the hyperbeam raygun's beam, the cactus, the Blackforge and the Blue Infrit. The particle-engine
 * effects they play are named in {@link VfxIds}; the big models are drawn by the client from {@code WildShapes}.
 */
public final class WildEvents {
	private WildEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: WildFx). */
	public static final int FX_CACTUS = 50, FX_FORGE = 51, FX_INFRIT = 52, FX_KOI = 53;

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	/** Squared distance from a block's centre to a point. */
	static double dist2(BlockPos p, Vec3d c) {
		double dx = p.getX() + 0.5 - c.x, dy = p.getY() + 0.5 - c.y, dz = p.getZ() + 0.5 - c.z;
		return dx * dx + dy * dy + dz * dz;
	}
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	/** Something thrown on an arc (goo globs, cactus spikes): steps like the client's particle, so both land together. */
	static final class Ballistic {
		Vec3d pos, vel;
		int age;

		Ballistic(Vec3d pos, Vec3d vel) {
			this.pos = pos;
			this.vel = vel;
		}

		/**
		 * Advances one tick; returns where it hit something, or null while still flying. {@code by} is the entity the
		 * block test is made for, {@code owner} the one it never hits.
		 */
		Vec3d step(ServerWorld world, Entity by, @Nullable Entity owner, double gravity, double hitRadius) {
			vel = vel.add(0, -gravity, 0);
			Vec3d next = pos.add(vel);
			age++;
			BlockHitResult hit = world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.ANY, by));
			if (hit.getType() == HitResult.Type.BLOCK) {
				return hit.getPos();
			}
			for (LivingEntity le : Kit.living(world, next, hitRadius)) {
				if (le != owner && !Kit.creativeOrSpectator(le)) {
					return le.getPos().add(0, 0.5, 0);
				}
			}
			pos = next;
			return null;
		}
	}

	// ====================================================================== GOO BOMB

	/**
	 * GOO BOMB (epic). An alien goo oozes out and swells, throbbing faster and faster, eating the ground it spreads
	 * into, until it bursts. Globs of acid fly everywhere; whatever they land on turns to goo and, a second later,
	 * dissolves away.
	 */
	public static class GooBomb extends SciFiEvents.Base {
		private final LongOpenHashSet blob = new LongOpenHashSet();
		private final List<Ballistic> globs = new ArrayList<>();
		private final List<Splat> splats = new ArrayList<>();

		record Splat(LongArrayList coated, int at) {
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center().add(0, Goo.LIFT, 0);
			if (t == 0) {
				Kit.sound(world, c, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, 8.0F, 0.5F);
				Vfx.play(world, VfxIds.GOO_PULSE, c, 1.2F);
			}
			if (t < Goo.BURST) {
				double r = Goo.radius(t);
				if (t % 3 == 0) {
					grow(c, r);
				}
				if (t % 4 == 0) {
					double a = world.random.nextDouble() * Math.PI * 2, y = world.random.nextDouble() * 1.6 - 0.4;
					double h = Math.sqrt(Math.max(0, 1 - y * y));
					Vec3d d = new Vec3d(Math.cos(a) * h, y, Math.sin(a) * h);
					Vfx.play(world, VfxIds.GOO_DRIP, c.add(d.multiply(r + 0.3)), d, 1.0F, 0);
				}
				int beat = t < 70 ? 20 : t < 110 ? 12 : 6;
				if (t % beat == 0) {
					Kit.sound(world, c, SoundEvents.ENTITY_SLIME_SQUISH, 6.0F, 0.4F + t * 0.004F);
					Kit.sound(world, c, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, 4.0F, 0.5F);
					Vfx.play(world, VfxIds.GOO_PULSE, c, (float) r);
				}
				if (t % 10 == 0) {
					acid(c, r + 1.5, 2.0F);
				}
			}
			if (t == Goo.BURST) {
				burst(c);
			}
			for (Iterator<Ballistic> it = globs.iterator(); it.hasNext(); ) {
				Ballistic g = it.next();
				Vec3d hit = g.step(world, host, owner(), Goo.GRAVITY, 1.0);
				if (hit != null) {
					splat(hit, t);
					it.remove();
				} else if (g.age > 200 || g.pos.y < world.getBottomY()) {
					it.remove();
				}
			}
			for (Iterator<Splat> it = splats.iterator(); it.hasNext(); ) {
				Splat s = it.next();
				if (t >= s.at) {
					dissolve(s);
					it.remove();
				}
			}
			return t >= Goo.END && globs.isEmpty() && splats.isEmpty() || t > Goo.END + 200;
		}

		/** The blob eats into everything inside its radius and fills it with goo. */
		private void grow(Vec3d c, double r) {
			int ri = MathHelper.ceil(r);
			BlockPos o = BlockPos.ofFloored(c);
			BlockState goo = Blocks.SLIME_BLOCK.getDefaultState();
			for (BlockPos p : BlockPos.iterate(o.add(-ri, -ri, -ri), o.add(ri, ri, ri))) {
				if (dist2(p, c) > r * r || blob.contains(p.asLong())) {
					continue;
				}
				BlockState s = world.getBlockState(p);
				if ((s.isAir() || breakable(p, s) || s.isReplaceable()) && put(p.toImmutable(), goo)) {
					blob.add(p.asLong());
				}
			}
		}

		private void burst(Vec3d c) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			for (long l : blob) {
				p.set(l);
				if (world.getBlockState(p).isOf(Blocks.SLIME_BLOCK)) {
					put(p.toImmutable(), AIR);
				}
			}
			blob.clear();
			Vfx.play(world, VfxIds.GOO_BURST, c, 2.0F);
			// And after it, an acid storm: a green cloud lid and rain hissing down out of it.
			Vfx.play(world, VfxIds.ACID_STORM, c, 1.0F);
			Vfx.flash(world, c, 0.3F, 140, 0x9AFF40);
			Vfx.shake(world, c, 120, 1.1F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 16.0F, 0.6F);
			Kit.sound(world, c, SoundEvents.ENTITY_SLIME_DEATH, 16.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, 16.0F, 0.5F);
			acid(c, 9, 18.0F);
			for (int i = 0; i < Goo.GLOBS; i++) {
				double az = world.random.nextDouble() * Math.PI * 2;
				double el = 0.15 + world.random.nextDouble() * 1.1;
				double speed = 0.55 + world.random.nextDouble() * 0.6;
				Vec3d d = new Vec3d(Math.cos(el) * Math.cos(az), Math.sin(el), Math.cos(el) * Math.sin(az));
				Vec3d start = c.add(d.multiply(Goo.R_MAX * 0.7));
				Vec3d v = d.multiply(speed);
				globs.add(new Ballistic(start, v));
				Vfx.play(world, VfxIds.GOO_GLOB, start, v, (float) speed, 0);
			}
		}

		/** Acid burns: damage, poison and a slowing stickiness. */
		private void acid(Vec3d c, double r, float damage) {
			for (LivingEntity e : Kit.living(world, c, r)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				e.damage(ModDamageTypes.source(world, ModDamageTypes.RADIATION, null, owner()), damage);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1));
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
			}
		}

		/** A glob lands: everything around it turns to goo, and dissolves a moment later. */
		private void splat(Vec3d at, int t) {
			Vfx.play(world, VfxIds.GOO_SPLAT, at, 1.0F);
			Kit.sound(world, at, SoundEvents.ENTITY_SLIME_SQUISH, 3.0F, 0.6F + world.random.nextFloat() * 0.3F);
			Kit.sound(world, at, SoundEvents.BLOCK_LAVA_EXTINGUISH, 2.0F, 1.4F);
			acid(at, 3.5, 8.0F);
			double r = 2.0 + world.random.nextDouble() * 1.3;
			int ri = MathHelper.ceil(r);
			BlockPos o = BlockPos.ofFloored(at);
			LongArrayList coated = new LongArrayList();
			BlockState goo = Blocks.SLIME_BLOCK.getDefaultState();
			for (BlockPos p : BlockPos.iterate(o.add(-ri, -ri, -ri), o.add(ri, ri, ri))) {
				if (dist2(p, at) > r * r) {
					continue;
				}
				BlockState s = world.getBlockState(p);
				if (breakable(p, s) && !s.isOf(Blocks.SLIME_BLOCK) && put(p.toImmutable(), goo)) {
					coated.add(p.asLong());
				}
			}
			if (!coated.isEmpty()) {
				splats.add(new Splat(coated, t + 14 + world.random.nextInt(18)));
			}
		}

		/** The acid finishes its work: the goo and what it covered are gone, but for a few sticky puddles. */
		private void dissolve(Splat s) {
			BlockPos.Mutable p = new BlockPos.Mutable();
			Vec3d at = null;
			for (long l : s.coated) {
				p.set(l);
				if (at == null) {
					at = Vec3d.ofCenter(p);
				}
				if (world.getBlockState(p).isOf(Blocks.SLIME_BLOCK) && world.random.nextFloat() > 0.08F) {
					put(p.toImmutable(), AIR);
				}
			}
			if (at != null) {
				Kit.sound(world, at, SoundEvents.BLOCK_FIRE_EXTINGUISH, 2.0F, 0.6F);
			}
		}
	}

	// ====================================================================== HYPERBEAM RAYGUN

	/**
	 * The hyperbeam: after the long charge, a beam as thick as a house roars out of the raygun and pushes forward,
	 * tunnelling through everything for nearly a hundred blocks, and keeps pouring for six seconds before it thins
	 * away to nothing.
	 */
	public static class Hyperbeam extends SciFiEvents.Base {
		static final int LIFE = 120, FADE = 14;
		static final double SPEED = 2.4, MAX = 96, R = 2.6;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d o = center();
			Vec3d d = new Vec3d(data.getDouble("Dx"), data.getDouble("Dy"), data.getDouble("Dz")).normalize();
			double len = Math.min(MAX, (t + 1) * SPEED);
			Vec3d head = o.add(d.multiply(len));
			if (t == 0) {
				Vfx.play(world, VfxIds.RAY_MUZZLE, o, d, 1.5F, 0);
				Vfx.flash(world, o, 0.25F, 60, 0x90E0FF);
				Kit.sound(world, o, SoundEvents.BLOCK_BEACON_POWER_SELECT, 8.0F, 0.6F);
				Kit.sound(world, o, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 6.0F, 0.6F);
			}
			if (t < LIFE) {
				// Two beams, a wide blue glow and a white-hot core, thinning away over the last moments.
				float w = (float) MathHelper.clamp((LIFE - t) / (double) FADE, 0.0, 1.0);
				Vfx.beam(world, o, head, 2.4F * w + 0.2F, 0x48B8FF, 3);
				Vfx.beam(world, o, head, 0.9F * w + 0.1F, 0xF0FFFF, 3);
				if (t % 2 == 0 && w > 0.3F) {
					Vfx.play(world, VfxIds.RAY_HEAD, head, d.multiply(-1), 1.4F * w, 0);
				}
				if (t % 4 == 0) {
					Kit.sound(world, o, ModSounds.DEATH_RAY, 4.0F * w + 1, 0.7F);
					Vfx.shake(world, o, 48, 0.45F * w);
				}
				if (len < MAX && Destruction.enabled(world)) {
					Kit.sphere(world, BlockPos.ofFloored(head), R, AIR);
					Kit.sphere(world, BlockPos.ofFloored(head.subtract(d.multiply(SPEED * 0.5))), R, AIR);
				}
				if (w > 0.2F) {
					hurt(o, d, len);
				}
			}
			return t >= LIFE;
		}

		/** Everything in the beam takes a beating and is shoved along it. */
		private void hurt(Vec3d o, Vec3d d, double len) {
			Vec3d mid = o.add(d.multiply(len * 0.5));
			for (LivingEntity e : Kit.living(world, mid, len * 0.5 + R + 1)) {
				if (e == owner() || Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d rel = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(o);
				double along = rel.dotProduct(d);
				if (along < 0 || along > len || rel.subtract(d.multiply(along)).length() > R + 0.8) {
					continue;
				}
				e.timeUntilRegen = 0;
				e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 7.0F);
				Kit.push(e, d.multiply(0.5).add(0, 0.15, 0));
			}
		}
	}

	// ====================================================================== CACTUS

	/**
	 * CACTUS BOMB (legendary). A saguaro the size of a tower grows out of the ground, block by block, starts to
	 * turn, and spins faster and faster until its spikes tear off and fly in every direction, each one blasting a
	 * hole where it lands. When the last is gone it slows and stops, and stands there as real blocks; its crown
	 * blooms, and a red sandstorm rolls out and turns the land around it to desert.
	 */
	public static class CactusBomb extends SciFiEvents.Base {
		private final List<Ballistic> spikes = new ArrayList<>();

		/** The axis it turns about: the centre of the block it grows from. */
		private Vec3d pivot() {
			BlockPos b = BlockPos.ofFloored(center());
			return new Vec3d(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d base = pivot();
			if (t == 0) {
				Vfx.newFx(world, FX_CACTUS, base, base, (float) Cactus.TRUNK_H, 0x5AA040, 0, 0, Cactus.SOLID + 4);
				Kit.sound(world, base, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 16.0F, 0.5F);
			}
			if (t >= Cactus.RISE && t < Cactus.RISEN && t % 6 == 0) {
				Vfx.play(world, VfxIds.CACTUS_DUST, base, 1.0F);
				Vfx.shake(world, base, 80, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_GRAVEL_BREAK, 12.0F, 0.5F);
			}
			if (t >= Cactus.SPIN && t < Cactus.STOP && t % 8 == 0) {
				float w = (float) Cactus.omega(t);
				Kit.sound(world, base.add(0, 15, 0), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 6.0F + w * 20, 0.5F + w * 2.5F);
			}
			if (t >= Cactus.FIRE && t <= Cactus.FIRE_END) {
				for (int i = 0; i < Cactus.SPIKES; i++) {
					if (Cactus.fireTick(i) == t) {
						launch(base, i, t);
					}
				}
			}
			for (Iterator<Ballistic> it = spikes.iterator(); it.hasNext(); ) {
				Ballistic s = it.next();
				Vec3d hit = s.step(world, host, owner(), Cactus.GRAVITY, 1.2);
				if (hit != null) {
					impact(hit);
					it.remove();
				} else if (s.age > 240 || s.pos.y < world.getBottomY()) {
					it.remove();
				}
			}
			if (t == Cactus.SOLID) {
				build(base);
				Kit.sound(world, base, SoundEvents.BLOCK_WOOD_PLACE, 16.0F, 0.4F);
			}
			if (t == Cactus.SOLID + 6) {
				bloom(base);
			}
			stepJobs(3000);
			return t >= Cactus.END && spikes.isEmpty() && jobs.isEmpty() || t > Cactus.END + 240;
		}

		private void launch(Vec3d base, int i, int t) {
			double[] o = new double[3];
			double a = Cactus.spin(t);
			Vec3d root = Cactus.turn(Cactus.spike(i, o), a);
			Vec3d dir = Cactus.turn(new Vec3d(o[0], o[1], o[2]), a);
			Vec3d start = base.add(root).add(dir.multiply(1.5));
			// Thrown by the spin as well as shot out: the faster it turns, the more it sweeps sideways.
			Vec3d spin = new Vec3d(-root.z, 0, root.x).multiply(Cactus.omega(t));
			Vec3d v = dir.multiply(Cactus.speed(i)).add(spin).add(0, 0.05 + 0.15 * SciShapes.hash(i, 5), 0);
			spikes.add(new Ballistic(start, v));
			Vfx.play(world, VfxIds.CACTUS_SPIKE, start, v, (float) v.length(), 0);
			Kit.sound(world, start, SoundEvents.ENTITY_ARROW_SHOOT, 4.0F, 0.4F);
		}

		private void impact(Vec3d at) {
			Vfx.play(world, VfxIds.SPIKE_IMPACT, at, 1.0F);
			for (LivingEntity e : Kit.living(world, at, 4)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 16.0F);
				}
			}
			Destruction.explode(world, owner(), at.x, at.y, at.z, 3.4F, false);
		}

		/** The spent cactus, set into exactly the blocks the spinning one was drawn with. */
		private void build(Vec3d base) {
			BlockPos b = BlockPos.ofFloored(base);
			for (int[] v : Cactus.voxels()) {
				put(b.add(v[0], v[1], v[2]), v[3] == 1 ? Blocks.GREEN_TERRACOTTA.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState());
			}
		}

		/** The crown flowers, and a red sandstorm rolls out, leaving desert where it passes. */
		private void bloom(Vec3d base) {
			Vfx.play(world, VfxIds.CACTUS_BLOOM, base.add(0, Cactus.TRUNK_H + 1, 0), 1.0F);
			Vfx.play(world, VfxIds.SUPERBLOOM, base.add(0, Cactus.TRUNK_H + 1, 0), 1.0F);
			Vfx.play(world, VfxIds.SANDSTORM, base, 1.0F);
			Vfx.shake(world, base, 80, 0.5F);
			Kit.sound(world, base, SoundEvents.ITEM_ELYTRA_FLYING, 24.0F, 0.5F);
			Kit.sound(world, base.add(0, Cactus.TRUNK_H, 0), SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, 12.0F, 0.6F);
			DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, base, 6, Cactus.DESERT_R, this::desert), 1.4));
		}

		/** One column of the new desert: red sand and coarse dirt, dead bushes, now and then a little cactus. */
		private void desert(TerrainJob.Column col) {
			int surf = col.surface();
			BlockState g = col.get(surf);
			if (!(g.isIn(BlockTags.DIRT) || g.isOf(Blocks.GRASS_BLOCK) || g.isIn(BlockTags.SAND) || g.isOf(Blocks.GRAVEL))) {
				return;
			}
			for (int y = surf + 1; y <= surf + 2; y++) {
				BlockState s = col.get(y);
				if (s.isReplaceable() || s.isIn(BlockTags.FLOWERS)) {
					col.set(y, AIR);
				}
			}
			double n = col.noise(21);
			BlockState sand = n < 0.6 ? Blocks.RED_SAND.getDefaultState() : n < 0.75 ? Blocks.SAND.getDefaultState()
					: n < 0.9 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.RED_SANDSTONE.getDefaultState();
			col.set(surf, sand);
			if (!col.get(surf - 1).isAir()) {
				col.set(surf - 1, n < 0.75 ? Blocks.RED_SANDSTONE.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
			}
			double p = col.noise(23);
			if (col.get(surf + 1).isAir() && sand.isIn(BlockTags.SAND)) {
				if (p < 0.06) {
					col.set(surf + 1, Blocks.DEAD_BUSH.getDefaultState());
				} else if (p < 0.075) {
					int h = 1 + (int) (col.noise(25) * 3);
					for (int y = 1; y <= h; y++) {
						col.set(surf + y, Blocks.CACTUS.getDefaultState());
					}
				}
			}
		}
	}

	// ====================================================================== BLACKFORGE

	/**
	 * BLACKFORGE (mythic). Anvils start falling out of the sky: a few, then more, faster and faster, wider and wider,
	 * until it pours anvils. Then a hammer the size of a tower comes down out of the dark and slams the ground at the
	 * centre. Every anvil turns to iron, and the land round it breaks into tectonic plates that heave and sink.
	 */
	public static class Blackforge extends SciFiEvents.Base {
		private static final int MAX_ANVILS = 520;
		private double acc;
		private int dropped;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.blackforge");
				Vfx.newFx(world, FX_FORGE, c, c, (float) Forge.PLATE_R, 0xFF5A1A, 0, 0, Forge.END);
				Vfx.skyTint(world, c, 100000, 0x1A1410, 0.55F);
				Kit.sound(world, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 48.0F, 0.5F);
			}
			if (t < Forge.RAIN_END) {
				acc += Forge.rate(t);
				double r = Forge.rainRadius(t);
				while (acc >= 1 && dropped < MAX_ANVILS) {
					acc -= 1;
					dropAnvil(c, r);
				}
				if (t % 20 == 0) {
					Vfx.shake(world, c, 200, 0.2F + t * 0.004F);
				}
			}
			if (t == Forge.HAMMER_IN) {
				Kit.sound(world, c, SoundEvents.ENTITY_WITHER_SHOOT, 48.0F, 0.3F);
			}
			if (t == Forge.STRIKE) {
				strike(c);
			}
			for (int k = 0; k < Forge.SHIFTS.length; k++) {
				if (t == Forge.SHIFTS[k]) {
					shift(c, k + 1);
				}
			}
			if (t == Forge.LIFT) {
				Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_USE, 48.0F, 0.3F);
			}
			stepJobs(6000);
			return t >= Forge.END && jobs.isEmpty();
		}

		/** One anvil, dropped from high above a random spot, falling fast; it dents what it lands on. */
		private void dropAnvil(Vec3d c, double r) {
			Vec3d hit = Climax.around(world, c, 0, r);
			BlockPos from = BlockPos.ofFloored(hit.x, hit.y + 45 + world.random.nextInt(25), hit.z);
			if (!world.isChunkLoaded(from.getX() >> 4, from.getZ() >> 4) || !world.getBlockState(from).isAir()) {
				return;
			}
			dropped++;
			FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, from, Blocks.ANVIL.getDefaultState());
			fb.setHurtEntities(2.0F, 40);
			double v = -1.0 - world.random.nextDouble() * 0.8;
			Kit.velocity(fb, new Vec3d(0, v, 0));
			// When it will land, as vanilla moves it: gravity, then the move, then drag.
			double y = from.getY(), vy = v;
			int ticks = 0;
			while (y > hit.y + 0.5 && ticks < 200) {
				vy -= 0.04;
				y += vy;
				vy *= 0.98;
				ticks++;
			}
			DetonationManager.schedule(world, Math.max(1, ticks - 1), () -> {
				Vfx.play(world, VfxIds.ANVIL_IMPACT, hit, 1.0F);
				if (Destruction.enabled(world)) {
					Kit.sphere(world, BlockPos.ofFloored(hit).down(2), 1.3, AIR);
				}
			});
		}

		/** The hammer lands: every anvil in reach turns to iron, even those still falling. */
		private void strike(Vec3d c) {
			Vfx.flash(world, c, 0.38F, 500, 0xFFB070);
			Vfx.shake(world, c, 600, 1.8F);
			Vfx.light(world, c, 1.4F, 0xFF9040);
			Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_LAND, 128.0F, 0.4F);
			Kit.sound(world, c, ModSounds.NUKE_DETONATE, 64.0F, 0.9F);
			Vfx.play(world, VfxIds.FORGE_STRIKE, c, UP, 2.2F, 0);
			Vfx.play(world, VfxIds.FORGE_SHOCK, c, (float) (Forge.IRON_R / 35.0));
			Vfx.play(world, VfxIds.IRON_WAVE, c, 1.0F);
			Vfx.play(world, VfxIds.FORGE_SPARKFALL, c, 1.0F);
			for (LivingEntity e : Kit.living(world, c, 24)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.NUCLEAR_BLAST, null, owner()), 30.0F);
					Kit.push(e, e.getPos().subtract(c).normalize().multiply(1.5).add(0, 1.0, 0));
				}
			}
			for (net.minecraft.entity.Entity e : Kit.entities(world, c, Forge.IRON_R, e -> e instanceof FallingBlockEntity fb && fb.getBlockState().isIn(BlockTags.ANVIL))) {
				FallingBlockEntity old = (FallingBlockEntity) e;
				BlockPos p = old.getBlockPos();
				Vec3d v = old.getVelocity();
				old.discard();
				if (world.getBlockState(p).isAir()) {
					FallingBlockEntity iron = FallingBlockEntity.spawnFromBlock(world, p, Blocks.IRON_BLOCK.getDefaultState());
					iron.setHurtEntities(2.0F, 40);
					Kit.velocity(iron, v);
				}
			}
			jobs.add(TerrainJob.disc(world, c, 13, col -> {
				int surf = col.surface();
				col.clear(surf - 2, surf + 6);
				col.set(surf - 3, Blocks.IRON_BLOCK.getDefaultState());
			}));
			DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, c, 0, Forge.IRON_R, this::ironify), 3.0));
		}

		/** One column: every anvil in it (they stack) becomes a block of iron. */
		private void ironify(TerrainJob.Column col) {
			int top = col.top();
			for (int y = top; y >= top - 8; y--) {
				if (col.get(y).isIn(BlockTags.ANVIL)) {
					col.set(y, Blocks.IRON_BLOCK.getDefaultState());
				}
			}
		}

		/** The plates move: each rises or sinks (and tilts) a little more; the first pass also opens the cracks. */
		private void shift(Vec3d c, int pass) {
			if (pass == 1) {
				for (int k = 0; k < 7; k++) {
					double a = k * Math.PI * 2 / 7 + 0.3;
					Kit.fissure(world, c.add(Math.cos(a) * Forge.PLATE_IN, 0, Math.sin(a) * Forge.PLATE_IN),
							new Vec3d(Math.cos(a), 0, Math.sin(a)), (int) (Forge.PLATE_R - Forge.PLATE_IN), 5, 2, Blocks.MAGMA_BLOCK.getDefaultState());
				}
			}
			jobs.add(TerrainJob.disc(world, c, Forge.PLATE_IN, Forge.PLATE_R, col -> move(col, c, pass)));
			Vfx.shake(world, c, 400, 1.0F);
			Kit.sound(world, c, SoundEvents.BLOCK_DEEPSLATE_BREAK, 64.0F, 0.3F + pass * 0.05F);
			Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 48.0F, 0.6F);
			for (int k = 0; k < 14; k++) {
				double r = Forge.RINGS[1 + world.random.nextInt(Forge.RINGS.length - 1)];
				double a = world.random.nextDouble() * Math.PI * 2;
				Vfx.play(world, VfxIds.PLATE_DUST, Kit.ground(world, c.x + Math.cos(a) * r, c.z + Math.sin(a) * r), 1.5F);
			}
		}

		/** One column of a plate, raised or lowered by however much further its plate moves this pass. */
		private void move(TerrainJob.Column col, Vec3d c, int pass) {
			double dx = col.x + 0.5 - c.x, dz = col.z + 0.5 - c.z;
			int id = Forge.plate(dx, dz);
			if (id < 0) {
				return;
			}
			double d = Math.sqrt(dx * dx + dz * dz);
			int delta = Forge.offset(id, d, pass) - Forge.offset(id, d, pass - 1);
			int surf = col.surface();
			for (int k = 0; k < Math.abs(delta); k++) {
				if (delta > 0) {
					// Raise: everything from a little below the surface up shifts one block higher.
					for (int y = surf + 1 + k; y > surf - 3 + k; y--) {
						col.set(y, col.get(y - 1));
					}
				} else {
					int top = surf - k;
					for (int y = top - 3; y < top; y++) {
						col.set(y, col.get(y + 1));
					}
					col.set(top, AIR);
				}
			}
		}
	}

	// ====================================================================== BLUE INFRIT

	/**
	 * BLUE INFRIT (celestial). Blue fire draws a summoning circle into the ground, ring by ring and stroke by stroke.
	 * Pillars of it rise from the star's points and the whole circle becomes a vortex. Then a hound's head of blue
	 * flame, big as a house, bursts out of it, circles, and dives on the land six times, each time leaving a burning
	 * pit with lava at its heart. At the end it bursts, and blue fire sweeps out across the country.
	 */
	public static class BlueInfrit extends SciFiEvents.Base {
		private double drawnTo;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				if (!data.contains("Seed")) {
					data.putInt("Seed", world.random.nextInt(1_000_000));
				}
				CelestialEvents.announce(world, c, "block.armageddon.blue_infrit");
				Vfx.newFx(world, FX_INFRIT, c, c, (float) Infrit.R, 0x40C8FF, seed(), 0, Infrit.END);
				Vfx.skyTint(world, c, 100000, 0x020818, 0.7F);
				Kit.sound(world, c, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 48.0F, 0.6F);
			}
			if (t <= Infrit.DRAWN) {
				double to = Infrit.drawn(t + 1);
				for (double u = drawnTo; u < to; u += 1.0 / 900) {
					burn(c.add(Infrit.pen(u)));
				}
				drawnTo = to;
				if (t % 4 == 0) {
					Vfx.play(world, VfxIds.SOUL_FLARE, ground(c.add(Infrit.pen(to))), 1.0F);
				}
				if (t % 6 == 0) {
					Kit.sound(world, c.add(Infrit.pen(to)), SoundEvents.ITEM_FIRECHARGE_USE, 6.0F, 0.6F);
				}
			}
			if (t == Infrit.SUMMON) {
				for (int k = 0; k < 5; k++) {
					Vfx.play(world, VfxIds.SOUL_PILLAR, ground(c.add(Infrit.star(k))), 1.5F);
				}
				Vfx.play(world, VfxIds.SOUL_VORTEX, c, 1.0F);
				Kit.sound(world, c, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 48.0F, 0.4F);
			}
			if (t > Infrit.SUMMON && t < Infrit.EMERGE && t % 8 == 0) {
				Vfx.shake(world, c, 200, 0.3F + (t - Infrit.SUMMON) * 0.007F);
				Kit.sound(world, c, SoundEvents.BLOCK_SOUL_SAND_BREAK, 24.0F, 0.5F);
			}
			if (t == Infrit.EMERGE) {
				Vfx.play(world, VfxIds.INFRIT_IMPACT, c, 2.0F);
				Vfx.flash(world, c, 0.3F, 300, 0x60D0FF);
				Vfx.shake(world, c, 300, 1.2F);
				Kit.sound(world, c, SoundEvents.ENTITY_RAVAGER_ROAR, 64.0F, 0.5F);
				Kit.sound(world, c, SoundEvents.ENTITY_WOLF_GROWL, 64.0F, 0.4F);
			}
			for (int i = 0; i < Infrit.DIVES; i++) {
				if (t == Infrit.impact(i) - 15) {
					Kit.sound(world, c, SoundEvents.ENTITY_WOLF_HOWL, 96.0F, 0.45F);
				}
				if (t == Infrit.impact(i)) {
					dive(c, i);
				}
			}
			if (t == Infrit.BURST) {
				Vec3d head = c.add(Infrit.head(t, seed(), (x, z) -> ground(c.add(x, 0, z)).y - c.y));
				Vfx.play(world, VfxIds.INFRIT_BURST, head, 3.0F);
				Vfx.play(world, VfxIds.SOUL_SWEEP, c, 1.0F);
				Vfx.flash(world, head, 0.35F, 500, 0x80E0FF);
				Vfx.shake(world, c, 500, 1.4F);
				Kit.sound(world, head, ModSounds.NUKE_DETONATE, 64.0F, 1.3F);
				Kit.sound(world, head, SoundEvents.ENTITY_WOLF_DEATH, 96.0F, 0.4F);
				DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, c, Infrit.R, Infrit.SWEEP, this::soulBurn), 2.5));
			}
			stepJobs(6000);
			return t >= Infrit.END && jobs.isEmpty();
		}

		private int seed() {
			return data.getInt("Seed");
		}

		private Vec3d ground(Vec3d p) {
			return Kit.ground(world, p.x, p.z);
		}

		/** The pen burns the circle into the ground: soul soil, and soul fire on it. */
		private void burn(Vec3d p) {
			BlockPos g = Kit.surface(world, MathHelper.floor(p.x), MathHelper.floor(p.z));
			if (put(g, Blocks.SOUL_SOIL.getDefaultState()) || world.getBlockState(g).isOf(Blocks.SOUL_SOIL)) {
				BlockPos up = g.up();
				BlockState s = world.getBlockState(up);
				if (s.isAir() || s.isReplaceable()) {
					put(up, Blocks.SOUL_FIRE.getDefaultState());
				}
			}
		}

		private void dive(Vec3d c, int i) {
			Vec3d tg = Infrit.target(i, seed());
			Vec3d hit = ground(c.add(tg));
			Vfx.play(world, VfxIds.INFRIT_IMPACT, hit, 2.5F);
			Vfx.flash(world, hit, 0.3F, 260, 0x60D0FF);
			Vfx.shake(world, hit, 300, 1.3F);
			Kit.sound(world, hit, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 48.0F, 0.5F);
			Kit.sound(world, hit, SoundEvents.ENTITY_WOLF_GROWL, 48.0F, 0.5F);
			for (LivingEntity e : Kit.living(world, hit, 16)) {
				if (!Kit.creativeOrSpectator(e)) {
					e.setOnFireFor(10);
					e.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION, null, owner()), 40.0F);
				}
			}
			Destruction.explode(world, owner(), hit.x, hit.y, hit.z, 7.0F, false);
			DetonationManager.schedule(world, 2, () -> jobs.add(TerrainJob.disc(world, hit, 13, col -> pit(col, hit))));
		}

		/** A burning pit: lava at the heart, soul soil and blue fire round it, scorched blackstone at the rim. */
		private void pit(TerrainJob.Column col, Vec3d at) {
			double s = 1 - Math.hypot(col.x + 0.5 - at.x, col.z + 0.5 - at.z) / 13.0;
			int surf = col.surface();
			if (s > 0.62) {
				col.set(surf, Blocks.LAVA.getDefaultState());
				col.set(surf - 1, Blocks.MAGMA_BLOCK.getDefaultState());
			} else if (s > 0.25) {
				col.set(surf, col.noise(2) < 0.3 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.SOUL_SOIL.getDefaultState());
				if (col.noise(4) < 0.45 && col.get(surf + 1).isAir() && col.get(surf).isOf(Blocks.SOUL_SOIL)) {
					col.set(surf + 1, Blocks.SOUL_FIRE.getDefaultState());
				}
			} else if (s > 0) {
				col.set(surf, col.noise(6) < 0.5 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.SOUL_SAND.getDefaultState());
			}
		}

		/** The blue fire that sweeps out at the end: plants burn away, grass and earth turn to soul soil, some of it alight. */
		private void soulBurn(TerrainJob.Column col) {
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 12); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isReplaceable()) {
					col.set(y, AIR);
				}
			}
			BlockState g = col.get(surf);
			if ((g.isIn(BlockTags.DIRT) || g.isOf(Blocks.GRASS_BLOCK) || g.isIn(BlockTags.SAND)) && col.noise(11) < 0.55) {
				col.set(surf, Blocks.SOUL_SOIL.getDefaultState());
				if (col.noise(13) < 0.14 && col.get(surf + 1).isAir()) {
					col.set(surf + 1, Blocks.SOUL_FIRE.getDefaultState());
				}
			}
		}
	}

	// ====================================================================== PLASMA KOI

	/**
	 * PLASMA KOI (mythic). A pond of liquid light opens in the ground and seven koi made of plasma circle under its
	 * surface. One by one they leap out, on longer and wilder arcs, and each comes down in a scorching splash. Then
	 * they all leap at once, braid into a dragon that climbs and circles over the pond, and dive back in: the pond
	 * erupts into a geyser and plasma rains over the land.
	 */
	public static class PlasmaKoi extends SciFiEvents.Base {
		private double surface;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				surface = Kit.ground(world, c.x, c.z).y - 0.4;
				CelestialEvents.announce(world, c, "block.armageddon.plasma_koi");
				Vfx.newFx(world, FX_KOI, new Vec3d(c.x, surface, c.z), c, (float) Koi.POND_R, 0x60F0FF, (float) surface, 0, Koi.END);
				Vfx.play(world, VfxIds.KOI_POND, new Vec3d(c.x, surface, c.z), 1.0F);
				Vfx.skyTint(world, c, 100000, 0x081830, 0.45F);
				Kit.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 0.6F);
				Kit.sound(world, c, SoundEvents.AMBIENT_UNDERWATER_ENTER, 48.0F, 0.6F);
			}
			Vec3d pond = new Vec3d(c.x, surface, c.z);
			if (t < Koi.OPEN && Destruction.enabled(world)) {
				dig(c, Koi.POND_R * t / Koi.OPEN, Koi.POND_R * (t + 1) / Koi.OPEN);
			}
			if (t >= Koi.OPEN && t < Koi.GEYSER) {
				if (t % 10 == 0) {
					// The pond itself burns: anything wading in it.
					Kit.damage(world, pond, Koi.POND_R, 5.0F, ModDamageTypes.PLASMA, null, owner());
				}
				if (t % 40 == 0) {
					Kit.sound(world, pond, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 32.0F, 0.6F);
					Kit.sound(world, pond, SoundEvents.BLOCK_BEACON_AMBIENT, 32.0F, 0.8F);
				}
			}
			for (int j = 0; j < Koi.LEAPS; j++) {
				int s = Koi.leapStart(j);
				if (t == s) {
					Vec3d from = pond.add(Koi.swim(Koi.koiOf(j), s));
					Vfx.play(world, VfxIds.KOI_BREACH, new Vec3d(from.x, surface, from.z), 1.0F + j * 0.04F);
					Kit.sound(world, from, SoundEvents.ENTITY_DOLPHIN_JUMP, 32.0F, 0.6F);
					Kit.sound(world, from, ModSounds.PLASMA_IMPACT, 24.0F, 1.4F);
				}
				if (t == s + Koi.flight(j)) {
					Vec3d l = pond.add(Koi.landing(j));
					splash(Kit.ground(world, l.x, l.z), 1.0F + j * 0.06F);
				}
			}
			if (t == Koi.GATHER) {
				Vfx.play(world, VfxIds.KOI_BREACH, pond, 2.5F);
				Kit.sound(world, pond, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 1.2F);
				Kit.sound(world, pond, SoundEvents.ENTITY_DOLPHIN_JUMP, 48.0F, 0.4F);
			}
			if (t > Koi.GATHER && t < Koi.GEYSER && t % 20 == 0) {
				Kit.sound(world, pond.add(Koi.spine(t)), SoundEvents.ENTITY_PHANTOM_FLAP, 48.0F, 0.5F);
				Vfx.shake(world, pond, 200, 0.4F + (t - Koi.GATHER) * 0.006F);
			}
			if (t == Koi.GEYSER - 10) {
				Kit.sound(world, pond, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.6F);
			}
			if (t == Koi.GEYSER) {
				geyser(pond);
			}
			if (t > Koi.GEYSER + 8 && t < Koi.RAIN_END && t % 3 == 0) {
				// The plasma it threw up comes down over the land.
				Vec3d p = Climax.around(world, c, 16, 70);
				Vfx.play(world, VfxIds.KOI_RAIN, p, 1.0F);
				Kit.damage(world, p, 2.5, 6.0F, ModDamageTypes.PLASMA, null, owner());
				if (Destruction.enabled(world) && world.random.nextInt(3) == 0) {
					Detonations.ignite(world, p, 2, 0.3F);
				}
			}
			stepJobs(4000);
			return t >= Koi.END && jobs.isEmpty();
		}

		/** One ring of the pond: hollowed to its plasma bed, a floor of glowing glass. */
		private void dig(Vec3d c, double r0, double r1) {
			int R = (int) Math.ceil(r1);
			int top = MathHelper.floor(surface + 0.4), floor = top - (int) Koi.DEPTH;
			for (int dx = -R; dx <= R; dx++) {
				for (int dz = -R; dz <= R; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < r0 || d >= r1) {
						continue;
					}
					int x = MathHelper.floor(c.x) + dx, z = MathHelper.floor(c.z) + dz;
					if (!world.isChunkLoaded(x >> 4, z >> 4)) {
						continue;
					}
					int bed = floor + (d > Koi.POND_R - 2 ? 1 : 0);
					for (int y = bed + 1; y <= top + 6; y++) {
						Destruction.set(world, new BlockPos(x, y, z), AIR, Kit.FLAGS);
					}
					double h = Koi.hash(x, z);
					Destruction.set(world, new BlockPos(x, bed, z), (h < 0.3 ? Blocks.SEA_LANTERN : Blocks.LIGHT_BLUE_STAINED_GLASS).getDefaultState(), Kit.FLAGS);
				}
			}
		}

		/** A koi comes down: a scorching splash of plasma, a little crater, fire. */
		private void splash(Vec3d at, float k) {
			Vfx.play(world, VfxIds.KOI_SPLASH, at, k);
			Kit.sound(world, at, ModSounds.PLASMA_IMPACT, 32.0F, 0.7F);
			Kit.sound(world, at, SoundEvents.ENTITY_GENERIC_SPLASH, 32.0F, 0.5F);
			Kit.damage(world, at, 4.5 * k, 16.0F * k, ModDamageTypes.PLASMA, null, owner());
			for (LivingEntity e : Kit.living(world, at, 4 * k)) {
				e.setOnFireFor(5);
			}
			if (Destruction.enabled(world)) {
				Kit.sphere(world, BlockPos.ofFloored(at).down(1), 2.0 * k, AIR);
				Detonations.ignite(world, at, (int) (3 * k), 0.25F);
			}
		}

		/** The dragon dives home and the pond erupts. */
		private void geyser(Vec3d pond) {
			Vfx.play(world, VfxIds.KOI_GEYSER, pond, 1.0F);
			Vfx.flash(world, pond, 0.6F, 600, 0xC0F8FF);
			Vfx.shake(world, pond, 600, 2.6F);
			Vfx.fov(world, pond, 14.0F, 24);
			Vfx.light(world, pond.add(0, 10, 0), 1.5F, 0x80F0FF);
			Kit.sound(world, pond, ModSounds.NUKE_DETONATE, 64.0F, 0.9F);
			Kit.sound(world, pond, SoundEvents.ENTITY_GENERIC_SPLASH, 64.0F, 0.3F);
			Kit.sound(world, pond, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.8F);
			Detonations.blast(world, pond, 45, 260.0F, ModDamageTypes.PLASMA, owner(), 20, 3.0);
			if (Destruction.enabled(world)) {
				DetonationManager.add(new com.armageddon.detonation.CraterJob(world, BlockPos.ofFloored(pond), Destruction.radius(world, 18.0F),
						com.armageddon.detonation.CraterJob.Mode.GLASSED).withLining(Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState()));
			}
		}
	}
}
