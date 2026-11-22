package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.DetonationManager;
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
import com.armageddon.vfx.VfxIds;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
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
	public static final int FX_CACTUS = 50, FX_FORGE = 51, FX_INFRIT = 52;

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
	 * tunnelling through everything, holds, and goes off where it ends.
	 */
	public static class Hyperbeam extends SciFiEvents.Base {
		static final int LIFE = 52;
		static final double SPEED = 2.4, MAX = 40, R = 2.6;

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
				// Two beams: a wide blue glow and a white-hot core.
				Vfx.beam(world, o, head, 2.4F, 0x48B8FF, 3);
				Vfx.beam(world, o, head, 0.9F, 0xF0FFFF, 3);
				if (t % 2 == 0) {
					Vfx.play(world, VfxIds.RAY_HEAD, head, d.multiply(-1), 1.4F, 0);
				}
				if (t % 4 == 0) {
					Kit.sound(world, o, ModSounds.DEATH_RAY, 4.0F, 0.7F);
					Vfx.shake(world, o, 48, 0.45F);
				}
				if (len < MAX && Destruction.enabled(world)) {
					Kit.sphere(world, BlockPos.ofFloored(head), R, AIR);
					Kit.sphere(world, BlockPos.ofFloored(head.subtract(d.multiply(SPEED * 0.5))), R, AIR);
				}
				hurt(o, d, len);
			}
			if (t == LIFE) {
				Kit.boom(world, head, 3.0F, 0x60C8FF, owner(), true);
				Vfx.play(world, VfxIds.RAY_BLAST, head, d.multiply(-1), 2.0F, 0);
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
	 * CACTUS BOMB (legendary). A saguaro the size of a tower grows out of the ground, starts to turn, and spins
	 * faster and faster until its spikes tear off and fly in every direction, each one blasting a hole where it
	 * lands. When the last is gone it slows, stops, and sets into a real, bald cactus of blocks.
	 */
	public static class CactusBomb extends SciFiEvents.Base {
		private final List<Ballistic> spikes = new ArrayList<>();

		@Override
		public boolean tick() {
			int t = age();
			Vec3d base = center();
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
				build(base, Cactus.angle(Cactus.STOP));
				Kit.sound(world, base, SoundEvents.BLOCK_WOOD_PLACE, 16.0F, 0.4F);
			}
			return t >= Cactus.END && spikes.isEmpty() || t > Cactus.END + 240;
		}

		private void launch(Vec3d base, int i, int t) {
			double[] o = new double[3];
			double a = Cactus.angle(t);
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

		/** The spent cactus, set into blocks where it stood: ribbed, bald and turned the way it stopped. */
		private void build(Vec3d base, double a) {
			if (!Destruction.enabled(world)) {
				return;
			}
			BlockPos b = BlockPos.ofFloored(base);
			column(b, 0, 0, Cactus.TRUNK_R, 0, Cactus.TRUNK_H, a);
			for (double[] arm : Cactus.ARMS) {
				Vec3d out = Cactus.turn(new Vec3d(Math.cos(arm[0]), 0, Math.sin(arm[0])), a);
				for (double s = 0; s <= arm[2]; s += 0.5) {
					ball(b, out.x * s, arm[1], out.z * s, Cactus.ARM_R, a);
				}
				column(b, out.x * arm[2], out.z * arm[2], Cactus.ARM_R, arm[1], arm[1] + arm[3], a);
			}
		}

		private void column(BlockPos b, double x, double z, double r, double y0, double y1, double a) {
			for (double y = y0; y <= y1; y += 1) {
				// Rounded off at the top.
				double rr = y > y1 - r ? Math.sqrt(Math.max(0, r * r - (y - (y1 - r)) * (y - (y1 - r)))) : r;
				disc(b, x, y, z, rr, a);
			}
		}

		private void ball(BlockPos b, double x, double y, double z, double r, double a) {
			for (double dy = -r; dy <= r; dy += 1) {
				disc(b, x, y + dy, z, Math.sqrt(Math.max(0, r * r - dy * dy)), a);
			}
		}

		private void disc(BlockPos b, double x, double y, double z, double r, double a) {
			int ri = MathHelper.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz > r * r + 0.5) {
						continue;
					}
					// Ribs: bands of darker green running up the stem, turned with it.
					double ang = Math.atan2(dz, dx) - a;
					boolean rib = Math.floorMod((int) Math.floor(ang / (Math.PI * 2) * 14), 2) == 0;
					boolean edge = dx * dx + dz * dz > (r - 1) * (r - 1);
					BlockState s = edge && rib ? Blocks.GREEN_TERRACOTTA.getDefaultState() : Blocks.GREEN_CONCRETE.getDefaultState();
					put(b.add(MathHelper.floor(x) + dx, MathHelper.floor(y), MathHelper.floor(z) + dz), s);
				}
			}
		}
	}

	// ====================================================================== BLACKFORGE

	/**
	 * BLACKFORGE (mythic). The ground cracks open on a glow from far below. A black anvil the size of a hall rises
	 * out of it, and a hammer as tall as a tower comes down out of the sky. It strikes four times. Every blow throws
	 * a storm of molten sparks and hammers a wider ring of the land flat into slag, basalt and magma. Then it rains
	 * molten iron, the hammer goes back up into the dark, and the anvil sinks.
	 */
	public static class Blackforge extends SciFiEvents.Base {
		@Override
		public boolean tick() {
			int t = age();
			Vec3d c = center();
			if (t == 0) {
				CelestialEvents.announce(world, c, "block.armageddon.blackforge");
				Vfx.newFx(world, FX_FORGE, c, c, (float) Forge.BANDS[Forge.BANDS.length - 1], 0xFF5A1A, 0, 0, Forge.END);
				Vfx.skyTint(world, c, 100000, 0x200804, 0.6F);
				Kit.sound(world, c, ModSounds.NUKE_RUMBLE, 48.0F, 0.5F);
			}
			if (t == Forge.CRACK + 2) {
				for (int k = 0; k < 4; k++) {
					double a = Math.PI / 4 + k * Math.PI / 2;
					Kit.fissure(world, c, new Vec3d(Math.cos(a), 0, Math.sin(a)), 50, 4, 2, Blocks.MAGMA_BLOCK.getDefaultState());
				}
				Vfx.play(world, VfxIds.FORGE_EMBERS, c, 1.0F);
				Vfx.shake(world, c, 200, 0.8F);
				Kit.sound(world, c, SoundEvents.BLOCK_BASALT_BREAK, 48.0F, 0.4F);
			}
			if (t == Forge.RISE) {
				// Make room: whatever stands where the anvil comes up is swept away.
				jobs.add(TerrainJob.disc(world, c, 19, col -> col.clear(col.surface() + 1, col.surface() + 40)));
			}
			if (t >= Forge.RISE && t < Forge.RISEN && t % 5 == 0) {
				Vfx.shake(world, c, 200, 0.6F);
				Kit.sound(world, c, SoundEvents.BLOCK_DEEPSLATE_BREAK, 32.0F, 0.4F);
			}
			if (t == Forge.HAMMER_IN) {
				Kit.sound(world, c, SoundEvents.ENTITY_WITHER_SHOOT, 48.0F, 0.3F);
			}
			int k = Forge.strikeAt(t);
			if (k >= 0) {
				strike(c, k);
			}
			if (t >= Forge.RAIN && t < Forge.RAIN_END && t % 3 == 0) {
				rain(c);
			}
			if (t == Forge.LIFT) {
				Kit.sound(world, c, SoundEvents.BLOCK_ANVIL_USE, 48.0F, 0.3F);
			}
			stepJobs(6000);
			return t >= Forge.END && jobs.isEmpty();
		}

		private void strike(Vec3d c, int k) {
			Vec3d top = c.add(0, Forge.ANVIL_H, 0);
			double r0 = k == 0 ? 0 : Forge.BANDS[k - 1], r1 = Forge.BANDS[k];
			Vfx.flash(world, top, 0.35F, 400, 0xFF8030);
			Vfx.shake(world, c, 500, 1.2F + k * 0.3F);
			Vfx.light(world, top, 1.2F, 0xFF7020);
			Kit.sound(world, top, SoundEvents.BLOCK_ANVIL_LAND, 96.0F, 0.5F - k * 0.05F);
			Kit.sound(world, top, ModSounds.NUKE_DETONATE, 48.0F, 1.2F - k * 0.1F);
			Vfx.play(world, VfxIds.FORGE_STRIKE, top, UP, 1.0F + k * 0.4F, 0);
			Vfx.play(world, VfxIds.FORGE_SHOCK, c, (float) (r1 / 35.0));
			for (int j = 0; j < 8; j++) {
				double r = r0 + (r1 - r0) * j / 8.0;
				DetonationManager.schedule(world, 1 + (int) (r / 5), () -> Climax.groundWave(world, c, r, 4, 24, 0.6));
			}
			DetonationManager.add(new Ruin.Front(world, TerrainJob.disc(world, c, Math.max(r0, 18), r1, this::forged), 5.0));
			for (LivingEntity e : Kit.living(world, c, r1)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				double d = Math.hypot(e.getX() - c.x, e.getZ() - c.z);
				if (d >= r0 - 4) {
					e.damage(ModDamageTypes.source(world, ModDamageTypes.NUCLEAR_BLAST, null, owner()), 24.0F);
					e.setOnFireFor(6);
					Kit.push(e, new Vec3d(0, 1.1, 0));
				}
			}
		}

		/** One column of the land under the hammer: swept bare and beaten into slag. */
		private void forged(TerrainJob.Column col) {
			int surf = col.surface();
			col.clear(surf + 1, Math.min(col.top(), surf + 40));
			double n = col.noise(3);
			BlockState s = n < 0.05 ? Blocks.MAGMA_BLOCK.getDefaultState()
					: n < 0.35 ? Blocks.BLACKSTONE.getDefaultState()
					: n < 0.65 ? Blocks.BASALT.getDefaultState()
					: n < 0.9 ? Blocks.SMOOTH_BASALT.getDefaultState() : Blocks.POLISHED_BLACKSTONE.getDefaultState();
			if (col.noise(7) < 0.012) {
				s = Blocks.LAVA.getDefaultState();
			}
			col.set(surf, s);
			if (!col.get(surf - 1).isAir()) {
				col.set(surf - 1, Blocks.BLACKSTONE.getDefaultState());
			}
		}

		/** A drop of molten iron, falling from the dark onto the hammered land. */
		private void rain(Vec3d c) {
			Vec3d hit = Climax.around(world, c, 12, Forge.BANDS[Forge.BANDS.length - 1]);
			Climax.fall(world, hit, 120, 0xFF6020, 1.0F, 18, () -> {
				Vfx.play(world, VfxIds.FORGE_SPLASH, hit, 1.0F);
				Kit.sound(world, hit, SoundEvents.BLOCK_LAVA_EXTINGUISH, 6.0F, 0.6F);
				BlockPos p = BlockPos.ofFloored(hit);
				put(p.down(), Blocks.MAGMA_BLOCK.getDefaultState());
				if (world.getBlockState(p).isAir()) {
					put(p, Blocks.FIRE.getDefaultState());
				}
				for (LivingEntity e : Kit.living(world, hit, 3)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.setOnFireFor(8);
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 10.0F);
					}
				}
			});
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
}
