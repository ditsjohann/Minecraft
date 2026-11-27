package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.detonation.CraterJob;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.PlasmaShapes;
import com.armageddon.util.PlasmaShapes.Arch;
import com.armageddon.util.PlasmaShapes.Bottle;
import com.armageddon.util.PlasmaShapes.Geyser;
import com.armageddon.util.PlasmaShapes.Pinch;
import com.armageddon.util.PlasmaShapes.Plasmoid;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Five plasma bombs, each a different way plasma behaves and a different way of wrecking the land: the PLASMOID SWARM
 * (ball lightning that ricochets, burns through things and splits), the SOLAR ARCH (a flux rope that carries the land
 * round its loop, then lashes down and erupts), the FUSION GEYSER (a blowout that tears the land into the sky and
 * throws it back as meteors), the Z-PINCH (a column that drags the land in and eats it, then collapses into a crater)
 * and the MAGNETIC BOTTLE (a caged sun that pulls everything into orbit, then whips the land with its snapping field
 * lines, slices it and detonates). Most of what they look like is the particle engine (PlasmaBombVfx); the Swarm and
 * the Bottle also have a client effect (PlasmaBombFx).
 */
public final class PlasmaEvents {
	private PlasmaEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: PlasmaBombFx). */
	public static final int FX_PLASMOID = 68, FX_BOTTLE = 69;

	private static final Vec3d UP = new Vec3d(0, 1, 0);

	/** Burns and blasts: everything within {@code r} of {@code c} hurt (less further out), thrown, set alight. */
	static void blast(PlasmaBase e, Vec3d c, double r, float damage, double push, int fire) {
		for (LivingEntity v : Kit.living(e.w(), c, r)) {
			if (Kit.creativeOrSpectator(v)) {
				continue;
			}
			Vec3d to = v.getPos().subtract(c);
			double d = Math.max(0.5, to.length());
			double k = 1 - d / r;
			if (push > 0) {
				Kit.push(v, to.multiply(1 / d).multiply(push * k).add(0, 0.5 * push * k, 0));
			}
			v.damage(ModDamageTypes.source(e.w(), ModDamageTypes.PLASMA), (float) (damage * (0.3 + 0.7 * k)));
			if (fire > 0) {
				v.setOnFireFor(fire);
			}
		}
	}

	/** A real bolt of lightning (it sets fires) where block damage is on, a harmless one where it is off. */
	static void lightning(ServerWorld w, Vec3d at) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(at);
			bolt.setCosmetic(!Destruction.enabled(w));
			w.spawnEntity(bolt);
		}
	}

	static boolean isGround(BlockState g) {
		return FossilEvents2.earth(g);
	}

	/**
	 * Torn-up terrain in flight: blocks lifted out of the ground that a bomb steers through the air, then lets go of.
	 * Once let go they are watched until they come down, and the bomb is told where each one lands.
	 */
	static final class Debris {
		final List<FallingBlockEntity> held = new ArrayList<>();
		private final Map<FallingBlockEntity, Vec3d> thrown = new HashMap<>();

		void add(List<FallingBlockEntity> blocks) {
			held.addAll(blocks);
		}

		/** Sets every held block's velocity (blocks per tick); {@code null} lets go of it in place. */
		void steer(Function<FallingBlockEntity, Vec3d> velocity) {
			held.removeIf(fb -> !fb.isAlive());
			for (FallingBlockEntity fb : held) {
				Vec3d v = velocity.apply(fb);
				if (v != null) {
					Kit.velocity(fb, v);
				}
			}
		}

		/** Lets go of every held block with a kick; they fall from here. */
		void release(Function<FallingBlockEntity, Vec3d> kick) {
			for (FallingBlockEntity fb : held) {
				if (fb.isAlive()) {
					throwOne(fb, kick.apply(fb));
				}
			}
			held.clear();
		}

		void throwOne(FallingBlockEntity fb, Vec3d kick) {
			fb.setNoGravity(false);
			Kit.velocity(fb, kick);
			thrown.put(fb, fb.getPos());
		}

		/** Swallows held blocks that {@code eat} says are done (they vanish). */
		void eat(java.util.function.Predicate<FallingBlockEntity> eat) {
			held.removeIf(fb -> {
				if (fb.isAlive() && eat.test(fb)) {
					fb.discard();
					return true;
				}
				return !fb.isAlive();
			});
		}

		/** Follows thrown blocks down, calling {@code land} where each hits; trails burning plasma off a few of them. */
		void tick(ServerWorld w, int color, java.util.function.Consumer<Vec3d> land) {
			Iterator<Map.Entry<FallingBlockEntity, Vec3d>> it = thrown.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<FallingBlockEntity, Vec3d> e = it.next();
				if (!e.getKey().isAlive()) {
					land.accept(e.getValue());
					it.remove();
				} else {
					e.setValue(e.getKey().getPos());
				}
			}
			int n = 0;
			for (FallingBlockEntity fb : held) {
				if (n++ % 5 == (int) (w.getTime() % 5)) {
					Vfx.play(w, VfxIds.DEBRIS_EMBER, fb.getPos(), fb.getVelocity(), 1.0F, color);
				}
			}
			for (FallingBlockEntity fb : thrown.keySet()) {
				if (n++ % 3 == (int) (w.getTime() % 3)) {
					Vfx.play(w, VfxIds.DEBRIS_EMBER, fb.getPos(), fb.getVelocity(), 1.0F, color);
				}
			}
		}

		boolean settled() {
			return held.isEmpty() && thrown.isEmpty();
		}
	}

	abstract static class PlasmaBase extends OddBombEvents.Base {
		Vec3d heading, base;
		final Debris debris = new Debris();

		ServerWorld w() {
			return world;
		}

		void start(String id) {
			name(id, Formatting.LIGHT_PURPLE);
			heading = Kit.dir(data.getFloat("Yaw"));
			Vec3d g = center();
			base = Kit.ground(world, g.x, g.z);
		}

		Vec3d ground(double x, double z) {
			return Kit.ground(world, x, z);
		}

		Vec3d at(double x, double y, double z) {
			return base.add(FossilShapes.frame(heading, x, y, z));
		}

		/** Tears up {@code n} blocks round {@code c} into the debris in flight. */
		List<FallingBlockEntity> tear(Vec3d c, double r, int n) {
			List<FallingBlockEntity> l = Kit.lift(world, c, r, n, true, false);
			debris.add(l);
			return l;
		}

		/** A small crater with a scorched, half-molten rim. */
		void crater(Vec3d at, double r) {
			if (!Destruction.enabled(world)) {
				return;
			}
			Kit.sphere(world, BlockPos.ofFloored(at.add(0, -0.8, 0)), r, Blocks.AIR.getDefaultState());
			Kit.ringCrack(world, at, r + 1.5, 1, Blocks.MAGMA_BLOCK.getDefaultState(), 0.45F);
		}

		/** A big bowl crater, glassed and burning, carved over the next ticks. */
		void bigCrater(Vec3d at, double r) {
			if (Destruction.enabled(world)) {
				DetonationManager.add(new CraterJob(world, BlockPos.ofFloored(at.add(0, -1, 0)), (float) r, CraterJob.Mode.NUKE));
			}
		}

		/** Burns a hole straight through whatever is at {@code c}: trees, walls, the edge of a hill. */
		void burn(Vec3d c, double r) {
			if (!Destruction.enabled(world)) {
				return;
			}
			int R = MathHelper.ceil(r);
			BlockPos o = BlockPos.ofFloored(c);
			for (int x = -R; x <= R; x++) {
				for (int y = -R; y <= R; y++) {
					for (int z = -R; z <= R; z++) {
						if (x * x + y * y + z * z <= r * r) {
							q().add(o.add(x, y, z), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						}
					}
				}
			}
		}

		/**
		 * A molten trench along {@code pts} (on the ground, spaced under a block apart): {@code width} across,
		 * {@code depth} deep, its floor glowing, now and then a pool of lava in it.
		 */
		void trench(List<Vec3d> pts, double width, int depth, double lava) {
			if (!Destruction.enabled(world)) {
				return;
			}
			java.util.Set<Long> done = new java.util.HashSet<>();
			int R = MathHelper.ceil(width);
			for (Vec3d p : pts) {
				for (int dx = -R; dx <= R; dx++) {
					for (int dz = -R; dz <= R; dz++) {
						if (dx * dx + dz * dz > width * width) {
							continue;
						}
						int x = MathHelper.floor(p.x) + dx, z = MathHelper.floor(p.z) + dz;
						if (!done.add(BlockPos.asLong(x, 0, z))) {
							continue;
						}
						double edge = Math.sqrt(dx * dx + dz * dz) / Math.max(1, width);
						int d = Math.max(1, (int) Math.round(depth * (1 - edge * edge * 0.6)));
						BlockPos top = surface(x, z);
						for (int y = -d + 1; y <= 4; y++) {
							q().add(top.up(y), Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						}
						double h = FossilShapes.hash(x, z);
						BlockState floor = (h < lava ? Blocks.LAVA : h < 0.55 ? Blocks.MAGMA_BLOCK : h < 0.8 ? Blocks.BLACKSTONE : Blocks.BASALT).getDefaultState();
						q().add(top.down(d), floor, BlockQueue.ANY);
					}
				}
			}
		}

		/** A block of thrown debris coming down: it bursts in burning plasma and scorches where it lands. */
		void impact(Vec3d at, int color) {
			Vfx.play(world, VfxIds.DEBRIS_IMPACT, at, UP, 1.0F, color);
			blast(this, at, 3.5, 6.0F, 0.4, 3);
			if (Destruction.enabled(world) && FossilShapes.hash(MathHelper.floor(at.x), MathHelper.floor(at.z)) < 0.35) {
				BlockPos top = surface(MathHelper.floor(at.x), MathHelper.floor(at.z));
				q().add(top.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}

		/** Velocity toward {@code target}, eased and capped at {@code max} blocks a tick. */
		static Vec3d toward(Vec3d from, Vec3d target, double ease, double max) {
			Vec3d v = target.subtract(from).multiply(ease);
			double l = v.length();
			return l > max ? v.multiply(max / l) : v;
		}

		static double seed(FallingBlockEntity fb, int k) {
			return FossilShapes.hash(fb.getId(), k);
		}
	}

	// ====================================================================== PLASMOID SWARM

	/**
	 * PLASMOID SWARM. A core of crackling violet plasma gathers over the bomb, jerking and spitting, and fires out ball
	 * lightning: eighteen big plasmoids that ricochet over the land in wild hops, burning straight through every tree,
	 * wall and fence in their way, scorching the ground where they slam down and arcing to each other and to anything
	 * alive. Each one bursts and splits into three smaller ones that carry on, some calling lightning down as they go.
	 * Then the core fires a last volley in every direction and detonates.
	 */
	public static class PlasmoidSwarm extends PlasmaBase {
		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("plasmoid_swarm");
				Vfx.newFx(world, FX_PLASMOID, base, heading, 1.0F, 0xC080FF, 0, 0, Plasmoid.END);
				Vfx.play(world, VfxIds.PLASMOID_CORE, base.add(0, Plasmoid.CORE_Y, 0), 1.0F);
				Kit.sound(world, base, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 48.0F, 0.6F);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 1.4F);
			}
			Vec3d core = base.add(0, Plasmoid.CORE_Y, 0);
			if (t < Plasmoid.BURST && t % 7 == 0) {
				Kit.sound(world, core, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 32.0F, 1.4F + (t % 5) * 0.15F);
			}
			if (t < Plasmoid.BURST && t > Plasmoid.CHARGE && t % 9 == 0) {
				// The core lashes out at the ground round it.
				double a = FossilShapes.hash(t, 9) * MathHelper.TAU, r = 4 + FossilShapes.hash(t, 10) * 8;
				Vec3d g = ground(base.x + Math.cos(a) * r, base.z + Math.sin(a) * r);
				Vfx.arc(world, core, g, 1.2F, 0xC090FF);
				Vfx.play(world, VfxIds.PLASMOID_SKIP, g, 1.2F);
				scorch(g);
			}
			for (Plasmoid.Ball b : Plasmoid.BALLS) {
				if (t < b.start() || t > b.end()) {
					continue;
				}
				Vec3d p = orb(b, t);
				if (t == b.start()) {
					Kit.sound(world, p, SoundEvents.ENTITY_BLAZE_SHOOT, 48.0F, 1.4F + b.gen() * 0.2F);
					Kit.sound(world, p, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 32.0F, 1.8F);
				}
				// Burning through whatever is in the way.
				if ((t + b.id()) % 2 == 0) {
					burn(p, 0.7 + b.size() * 0.65);
				}
				if (Plasmoid.lands(b, t)) {
					Vec3d g = ground(p.x, p.z);
					Vfx.play(world, VfxIds.PLASMOID_SKIP, g, (float) b.size());
					Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 24.0F, 1.6F);
					scorch(g);
				}
				if ((t + b.id()) % 6 == 0) {
					for (LivingEntity e : Kit.living(world, p, b.size() + 3)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION), 5.0F);
							double a = FossilShapes.hash(t, e.getId()) * MathHelper.TAU;
							Kit.push(e, new Vec3d(Math.cos(a) * 0.8, 0.5, Math.sin(a) * 0.8));
						}
					}
				}
				if (t == b.end()) {
					pop(b, p);
				}
			}
			if (t == Plasmoid.FINALE) {
				Kit.sound(world, core, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 1.2F);
				Kit.sound(world, core, SoundEvents.ENTITY_WITHER_SHOOT, 96.0F, 0.6F);
			}
			if (t == Plasmoid.BURST) {
				Vfx.play(world, VfxIds.PLASMOID_BURST, core, 1.0F);
				Vfx.flash(world, core, 0.7F, 300, 0xE0C0FF);
				Vfx.shake(world, core, 240, 1.8F);
				Kit.sound(world, core, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.6F);
				Kit.sound(world, core, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.8F);
				bigCrater(base, 9);
				blast(this, core, 22, 22.0F, 2.0, 5);
				for (int i = 0; i < 8; i++) {
					double a = i * MathHelper.TAU / 8 + 0.3, r = 10 + FossilShapes.hash(i, 13) * 14;
					lightning(world, ground(base.x + Math.cos(a) * r, base.z + Math.sin(a) * r));
				}
			}
			drain();
			return t >= Plasmoid.END;
		}

		/** A plasmoid bursting: a blast (the big ones a real explosion), a crater, sometimes lightning. */
		private void pop(Plasmoid.Ball b, Vec3d p) {
			Vec3d g = ground(p.x, p.z);
			Vfx.play(world, VfxIds.PLASMOID_POP, p, (float) (0.7 + b.size() * 0.5));
			Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 48.0F, 1.1F + b.gen() * 0.2F);
			Vfx.shake(world, p, 50, 0.4F + (float) b.size() * 0.3F);
			if (b.gen() == 0) {
				Kit.sound(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 1.4F);
				Kit.blast(world, g.add(0, 0.5, 0), 1.6F, null, Destruction.enabled(world));
				crater(g, 2.6);
			} else {
				crater(g, 1.4 + b.size());
				if (FossilShapes.hash(b.id(), 17) < (b.gen() == 1 ? 0.35 : 0.15)) {
					lightning(world, g);
				}
			}
			blast(this, p, 4 + b.size() * 3, 10.0F + (float) b.size() * 4, 1.2, 3);
		}

		/** A scorch where it slams down: the ground blackened, glowing, sometimes alight. */
		private void scorch(Vec3d g) {
			if (!Destruction.enabled(world)) {
				return;
			}
			int x = MathHelper.floor(g.x), z = MathHelper.floor(g.z);
			BlockPos top = surface(x, z);
			if (!isGround(world.getBlockState(top))) {
				return;
			}
			double h = FossilShapes.hash(x * 3, z * 5);
			q().add(top, (h < 0.4 ? Blocks.MAGMA_BLOCK : h < 0.75 ? Blocks.COARSE_DIRT : Blocks.BLACKSTONE).getDefaultState(), BlockQueue.ANY);
			if (h > 0.6) {
				q().add(top.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}

		/** A plasmoid in the world, riding over the ground. */
		private Vec3d orb(Plasmoid.Ball b, int t) {
			Vec3d l = Plasmoid.pos(b, t);
			Vec3d w = at(l.x, 0, l.z);
			return new Vec3d(w.x, ground(w.x, w.z).y + l.y, w.z);
		}
	}

	// ====================================================================== SOLAR ARCH

	/**
	 * SOLAR ARCH. Two footpoints burst open either side of the bomb, fountaining plasma, and a twisted rope of it
	 * arches between them like a loop on the face of the sun; the land under it is torn up and carried round and round
	 * the loop. It snaps and lashes down flat in front of it, burning a molten trench in a great half-ring and flinging
	 * everything it carried; then it erupts forward and the eruption peels the land up in front of it, chars every
	 * tree, sets the ground alight, glazes the sand and leaves lava running in the scorch.
	 */
	public static class SolarArch extends PlasmaBase {
		private Vec3d right;
		private TerrainJob wedge;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("solar_arch");
				right = new Vec3d(-heading.z, 0, heading.x);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
			}
			Vec3d fa = foot(-1), fb = foot(1);
			if (t == Arch.FEET) {
				for (Vec3d f : new Vec3d[]{fa, fb}) {
					Vfx.play(world, VfxIds.ARCH_FOOT, f, UP, 1.0F, 0);
					Kit.sound(world, f, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.7F);
					Kit.sound(world, f, SoundEvents.ITEM_FIRECHARGE_USE, 64.0F, 0.5F);
					Vfx.shake(world, f, 80, 0.7F);
					crater(f, 3.5);
				}
			}
			if (t == Arch.ARCH) {
				Vfx.play(world, VfxIds.ARCH_ROPE, base, right, 1.0F, 0);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 96.0F, 0.5F);
			}
			if (t > Arch.ARCH && t < Arch.SNAP) {
				if (t % 20 == 0) {
					Vec3d top = base.add(0, Arch.height(t), 0);
					Kit.sound(world, top, SoundEvents.BLOCK_FIRE_AMBIENT, 64.0F, 0.5F);
					Kit.sound(world, top, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F + (t - Arch.ARCH) * 0.004F);
				}
				// The land under it is torn up into the loop.
				if (t > Arch.ARCH + 30 && t % 5 == 0 && debris.held.size() < 120) {
					tear(base, Arch.SPAN + 3, 6);
					Kit.sound(world, base, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 48.0F, 0.6F);
				}
				debris.steer(fb2 -> ride(fb2, t));
				if (t % 10 == 0) {
					for (int i = 0; i <= 16; i++) {
						Vec3d p = base.add(FossilShapes.frame(heading, Arch.rope(Math.PI * i / 16, t)));
						for (LivingEntity e : Kit.living(world, p, 4)) {
							if (!Kit.creativeOrSpectator(e)) {
								e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 5.0F);
								e.setOnFireFor(5);
							}
						}
					}
					if (Destruction.enabled(world) && t % 40 == 0) {
						double r = 2.5 + (t - Arch.ARCH) / 60.0;
						Kit.ringCrack(world, fa, r, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
						Kit.ringCrack(world, fb, r, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
					}
				}
			}
			if (t == Arch.SNAP) {
				Vec3d top = base.add(0, Arch.height(t), 0);
				Kit.sound(world, top, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.6F);
				Kit.sound(world, top, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 128.0F, 0.4F);
				Vfx.shake(world, top, 200, 1.0F);
			}
			if (t >= Arch.SNAP && t < Arch.SNAP + Arch.LASH) {
				debris.steer(fb2 -> ride(fb2, t));
			}
			if (t == Arch.SNAP + Arch.LASH) {
				lash();
			}
			if (wedge != null) {
				wedge.limit = t >= Arch.BLAST_END ? Arch.REACH + 1 : Arch.front(t);
				wedge.step(3500);
			}
			if (t > Arch.SNAP + Arch.LASH && t <= Arch.BLAST_END) {
				double front = Arch.front(t);
				if (t % 3 == 0) {
					for (int k = -2; k <= 2; k++) {
						double a = Math.atan2(heading.z, heading.x) + k * Arch.WEDGE / 2.2;
						Vec3d p = ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front);
						Vfx.play(world, VfxIds.ARCH_SCORCH, p, (float) (1.4 - 0.6 * front / Arch.REACH));
					}
					// The eruption peels the land up in front of it and throws it on ahead.
					double a = Math.atan2(heading.z, heading.x) + (FossilShapes.hash(t, 7) - 0.5) * 2 * Arch.WEDGE;
					Vec3d p = ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front);
					for (FallingBlockEntity f : Kit.lift(world, p, 5, 8, true, false)) {
						debris.throwOne(f, heading.multiply(0.9 + seed(f, 1) * 0.8).add(0, 0.7 + seed(f, 2) * 0.6, 0));
					}
				}
				if (t % 6 == 0) {
					Vec3d p = base.add(heading.multiply(front));
					Kit.sound(world, p, SoundEvents.BLOCK_FIRE_AMBIENT, 128.0F, 0.6F);
					Kit.sound(world, p, SoundEvents.ENTITY_BLAZE_SHOOT, 96.0F, 0.5F);
				}
				for (LivingEntity e : Kit.living(world, base, front + 3)) {
					if (Kit.creativeOrSpectator(e) || !inWedge(e.getX(), e.getZ())) {
						continue;
					}
					double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
					if (d > front - 6) {
						Kit.push(e, heading.multiply(1.4).add(0, 0.6, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 14.0F);
						e.setOnFireFor(8);
					}
				}
			}
			debris.tick(world, 0xFFA030, p -> impact(p, 0xFFA030));
			drain();
			return t >= Arch.END && (wedge == null || wedge.isDone()) && debris.settled();
		}

		/** A block caught in the loop: carried along the rope from foot to foot, wound round it. */
		private Vec3d ride(FallingBlockEntity f, int t) {
			Vec3d p = f.getPos().subtract(base);
			double z = p.x * right.x + p.z * right.z;
			double th = Math.acos(MathHelper.clamp(-z / Arch.SPAN, -1, 1));
			th += (seed(f, 3) < 0.5 ? 1 : -1) * 0.09;
			th = MathHelper.clamp(th, 0.15, Math.PI - 0.15);
			double wind = t * 0.15 + seed(f, 4) * MathHelper.TAU, r = 1.5 + seed(f, 5) * 2;
			Vec3d target = base.add(FossilShapes.frame(heading, Arch.rope(th, t))).add(Math.cos(wind) * r, Math.sin(wind) * r, 0);
			return toward(f.getPos(), target, 0.25, 1.4);
		}

		/** The rope lashing down: a molten trench in a half-ring in front, everything it carried flung, the eruption set off. */
		private void lash() {
			int t = age();
			List<Vec3d> pts = new ArrayList<>();
			for (int i = 0; i <= 90; i++) {
				Vec3d l = Arch.rope(Math.PI * i / 90, t);
				Vec3d w = base.add(FossilShapes.frame(heading, l.x, 0, l.z));
				pts.add(ground(w.x, w.z));
				if (i % 9 == 0) {
					Vfx.play(world, VfxIds.ARCH_LASH, pts.get(pts.size() - 1), heading, 1.0F, 0);
				}
			}
			trench(pts, 2.2, 4, 0.08);
			Vec3d apex = base.add(heading.multiply(Arch.height(t)));
			Vfx.play(world, VfxIds.ARCH_CME, apex.add(0, 3, 0), heading, 1.0F, 0);
			Vfx.flash(world, apex, 0.7F, 400, 0xFFD080);
			Vfx.shake(world, apex, 300, 1.8F);
			Kit.sound(world, apex, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.5F);
			Kit.sound(world, apex, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
			Kit.sound(world, apex, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.6F);
			for (Vec3d p : pts) {
				for (LivingEntity e : Kit.living(world, p, 3.5)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 24.0F);
						e.setOnFireFor(8);
					}
				}
			}
			debris.release(f -> heading.multiply(1.2 + seed(f, 6) * 1.2).add(right.multiply((seed(f, 7) - 0.5) * 1.4)).add(0, 0.8 + seed(f, 8) * 0.8, 0));
			if (Destruction.enabled(world)) {
				wedge = TerrainJob.disc(world, base, Arch.REACH, this::scorch);
				wedge.limit = 0;
			}
		}

		private Vec3d foot(int side) {
			Vec3d p = base.add(right == null ? Vec3d.ZERO : right.multiply(side * Arch.SPAN));
			return ground(p.x, p.z);
		}

		private boolean inWedge(double x, double z) {
			double dx = x - base.x, dz = z - base.z, d = Math.hypot(dx, dz);
			if (d < 4) {
				return true;
			}
			return Math.acos(MathHelper.clamp((dx * heading.x + dz * heading.z) / d, -1, 1)) <= Arch.WEDGE + 6 / Math.max(d, 6);
		}

		/** Swept by the eruption: trees charred, plants burned, the ground scorched and molten, sand glazed, lava running. */
		private void scorch(TerrainJob.Column c) {
			if (!inWedge(c.x + 0.5, c.z + 0.5)) {
				return;
			}
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 30); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				} else if (s.isIn(BlockTags.LOGS)) {
					c.set(y, (c.noise(y) < 0.6 ? Blocks.BASALT : Blocks.BLACKSTONE).getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (g.isIn(BlockTags.SAND)) {
				c.set(surf, (c.noise(81) < 0.6 ? Blocks.ORANGE_STAINED_GLASS : Blocks.GLASS).getDefaultState());
				return;
			}
			if (!isGround(g)) {
				return;
			}
			double n = c.noise(83);
			if (n < 0.012) {
				c.set(surf, Blocks.LAVA.getDefaultState());
				return;
			}
			Block top = n < 0.22 ? Blocks.MAGMA_BLOCK : n < 0.6 ? Blocks.COARSE_DIRT : n < 0.82 ? Blocks.BLACKSTONE : Blocks.NETHERRACK;
			c.set(surf, top.getDefaultState());
			if (c.noise(85) < 0.12 && c.get(surf + 1).isAir()) {
				c.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		}
	}

	// ====================================================================== FUSION GEYSER

	/**
	 * FUSION GEYSER. The ground glows and cracks in rings over a pocket of fusing plasma. It blows out: a crater opens
	 * and a geyser of cyan plasma roars eighty blocks into the sky, tearing up the land all round and spiralling it up
	 * the column; anything standing near is thrown up with it. At the top the torn-up land is flung out in every
	 * direction and comes down as burning meteors, while the plasma itself rains back down, the biggest drops blowing
	 * little craters. The vent is left a lake of lava.
	 */
	public static class FusionGeyser extends PlasmaBase {
		private int drops;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("fusion_geyser");
				Vfx.play(world, VfxIds.GEYSER_BULGE, base, 1.0F);
				Kit.sound(world, base, SoundEvents.BLOCK_LAVA_AMBIENT, 64.0F, 0.5F);
			}
			if (t < Geyser.ERUPT && t % 15 == 0) {
				float k = t / (float) Geyser.ERUPT;
				Vfx.shake(world, base, 80, 0.3F + 0.7F * k);
				Kit.sound(world, base, SoundEvents.BLOCK_LAVA_POP, 48.0F, 0.5F + k);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_AMBIENT, 48.0F, 0.6F + k);
				if (Destruction.enabled(world)) {
					Kit.ringCrack(world, base, 3 + k * 8, 1, Blocks.MAGMA_BLOCK.getDefaultState(), 0.5F);
				}
			}
			if (t == Geyser.ERUPT) {
				Vfx.play(world, VfxIds.GEYSER_ERUPT, base, 1.0F);
				Vfx.flash(world, base.add(0, 20, 0), 0.6F, 300, 0xB0FFFF);
				Vfx.shake(world, base, 260, 1.8F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_BLAZE_SHOOT, 128.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 96.0F, 1.2F);
				bigCrater(base, 8);
				blast(this, base, 12, 18.0F, 2.0, 5);
			}
			// The land round it is torn up and drawn into the column.
			if (t > Geyser.ERUPT && t < Geyser.FLING - 10 && t % 3 == 0) {
				tear(base, Geyser.TORN, 14);
				if (t % 9 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 96.0F, 0.5F);
				}
			}
			if (t > Geyser.ERUPT && t < Geyser.FLING) {
				debris.steer(f -> spiral(f, t));
			}
			if (t == Geyser.FLING) {
				// Flung from the top in every direction.
				debris.release(f -> {
					Vec3d out = new Vec3d(f.getX() - base.x, 0, f.getZ() - base.z);
					out = out.lengthSquared() < 1.0E-3 ? heading : out.normalize();
					return out.multiply(0.8 + seed(f, 9) * 1.4).add(0, 0.3 + seed(f, 10) * 0.6, 0);
				});
				Kit.sound(world, base.add(0, 60, 0), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
				Kit.sound(world, base.add(0, 60, 0), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.7F);
			}
			if (t > Geyser.ERUPT && t < Geyser.RAIN_END && t % 4 == 0) {
				// The column throws everything in it skyward.
				for (LivingEntity e : Kit.living(world, base.add(0, 10, 0), 12)) {
					if (!Kit.creativeOrSpectator(e) && Math.hypot(e.getX() - base.x, e.getZ() - base.z) < 7) {
						Kit.velocity(e, new Vec3d((e.getX() - base.x) * 0.05, 1.6, (e.getZ() - base.z) * 0.05));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 4.0F);
						e.setOnFireFor(4);
					}
				}
				if (t % 20 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_FIRE_AMBIENT, 128.0F, 0.4F);
					Kit.sound(world, base, SoundEvents.BLOCK_LAVA_AMBIENT, 128.0F, 0.6F);
				}
			}
			if (t == Geyser.RAIN) {
				Vfx.play(world, VfxIds.GEYSER_RAIN, base.add(0, 50, 0), 1.0F);
			}
			if (t >= Geyser.RAIN + 25 && t < Geyser.RAIN_END) {
				for (int i = 0; i < 2; i++) {
					land(drops++);
				}
				if (t % 6 == 0) {
					Kit.sound(world, base.add(Geyser.drop(drops).multiply(0.6)), SoundEvents.BLOCK_FIRE_EXTINGUISH, 64.0F, 0.6F);
				}
			}
			if (t == Geyser.LAKE) {
				lake();
			}
			debris.tick(world, 0x40F0FF, p -> impact(p, 0x40F0FF));
			drain();
			return t >= Geyser.END && debris.settled();
		}

		/** A torn-up block in the column: wound round it, climbing fast. */
		private Vec3d spiral(FallingBlockEntity f, int t) {
			Vec3d p = f.getPos().subtract(base);
			double h = Math.hypot(p.x, p.z), a = Math.atan2(p.z, p.x) + 0.25 + seed(f, 11) * 0.1;
			double r = Math.max(3, h - 1.2) * (h > 9 ? 1 : 0) + (h <= 9 ? 3 + seed(f, 12) * 4 : 0);
			double top = 40 + seed(f, 13) * 35;
			double y = Math.min(top, p.y + 1.4);
			Vec3d target = base.add(Math.cos(a) * r, y, Math.sin(a) * r);
			return toward(f.getPos(), target, 0.5, 1.8);
		}

		/** A drop of plasma coming down: a hiss, a scorch, a little fire; every fifth a burning bomb that blows a crater. */
		private void land(int i) {
			Vec3d l = Geyser.drop(i);
			Vec3d g = ground(base.x + l.x, base.z + l.z);
			boolean big = i % 5 == 0;
			Vfx.play(world, big ? VfxIds.DEBRIS_IMPACT : VfxIds.GEYSER_SPLASH, g, UP, big ? 1.6F : 1.0F, 0x40F0FF);
			blast(this, g, big ? 5 : 3, big ? 10.0F : 6.0F, big ? 0.8 : 0, 4);
			if (!Destruction.enabled(world)) {
				return;
			}
			if (big) {
				crater(g, 2.4);
				return;
			}
			BlockPos top = surface(MathHelper.floor(g.x), MathHelper.floor(g.z));
			BlockState s = world.getBlockState(top);
			if (s.isIn(BlockTags.LEAVES)) {
				q().add(top, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
				return;
			}
			if (!isGround(s)) {
				return;
			}
			double n = PlasmaShapes.hash(i, 34);
			Block b = n < 0.35 ? Blocks.MAGMA_BLOCK : n < 0.6 ? Blocks.BLACKSTONE : n < 0.8 ? Blocks.CYAN_TERRACOTTA : Blocks.LIGHT_BLUE_STAINED_GLASS;
			q().add(top, b.getDefaultState(), BlockQueue.ANY);
			if (PlasmaShapes.hash(i, 35) < 0.4) {
				q().add(top.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}

		/** The vent left a lake of lava ringed with glass fused cyan. */
		private void lake() {
			Kit.sound(world, base, SoundEvents.BLOCK_LAVA_AMBIENT, 128.0F, 0.4F);
			Kit.sound(world, base, SoundEvents.ITEM_BUCKET_EMPTY_LAVA, 128.0F, 0.5F);
			if (!Destruction.enabled(world)) {
				return;
			}
			int bx = MathHelper.floor(base.x), bz = MathHelper.floor(base.z);
			for (int x = -9; x <= 9; x++) {
				for (int z = -9; z <= 9; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d > 9) {
						continue;
					}
					BlockPos top = surface(bx + x, bz + z);
					if (d < 5.5) {
						q().add(top.up(), Blocks.LAVA.getDefaultState(), BlockQueue.AIR_ONLY);
						q().add(top, Blocks.MAGMA_BLOCK.getDefaultState(), BlockQueue.ANY);
					} else if (d < 7) {
						q().add(top, (FossilShapes.hash(x, z) < 0.5 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.OBSIDIAN).getDefaultState(), BlockQueue.ANY);
					}
				}
			}
		}
	}

	// ====================================================================== Z-PINCH

	/**
	 * Z-PINCH. A column of blue-white plasma stands up out of the bomb and magnetic rings crush it. Its field rips the
	 * land up for thirty blocks round and drags it spiralling in, and the column eats it; everything alive is hauled
	 * toward it. It breaks into beads that are squeezed out and fly off to land as explosions. Then it collapses into
	 * a fusion flash: a crater opens where it stood with a fused core in it, lightning stands round the rim, and a
	 * shockwave rolls out that throws the land outward and strips everything flat.
	 */
	public static class ZPinch extends PlasmaBase {
		private TerrainJob strip;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("z_pinch");
				Vfx.play(world, VfxIds.PINCH_COLUMN, base, new Vec3d(0, Pinch.HEIGHT, 0), 1.0F, 0);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 1.4F);
			}
			if (t < Pinch.COLLAPSE) {
				float k = t / (float) Pinch.COLLAPSE;
				if (t % 16 == 0) {
					Kit.sound(world, base.add(0, 20, 0), SoundEvents.BLOCK_BEACON_AMBIENT, 96.0F, 0.5F + 1.3F * k);
					Kit.sound(world, base.add(0, 20, 0), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 48.0F, 1.5F);
				}
				if (t > Pinch.SQUEEZE) {
					// The land is ripped up and dragged in.
					if (t % 3 == 0) {
						tear(base, Pinch.DRAG * (0.5 + 0.5 * k), 8 + (int) (10 * k));
					}
					if (t % 10 == 0) {
						Kit.sound(world, base, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 96.0F, 0.5F);
						Kit.sound(world, base, SoundEvents.BLOCK_CHAIN_BREAK, 64.0F, 0.6F);
					}
					debris.steer(f -> drag(f, k));
					debris.eat(f -> Math.hypot(f.getX() - base.x, f.getZ() - base.z) < Pinch.RADIUS * 0.7);
					if (t % 5 == 0) {
						for (LivingEntity e : Kit.living(world, base, Pinch.DRAG)) {
							if (Kit.creativeOrSpectator(e)) {
								continue;
							}
							Vec3d to = new Vec3d(base.x - e.getX(), 0, base.z - e.getZ());
							double d = Math.max(0.5, to.length());
							Kit.push(e, to.multiply(1 / d).multiply(0.25 + 0.6 * k).add(0, 0.15 * k, 0));
							if (d < Pinch.RADIUS + 1.5) {
								e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION), 8.0F);
							}
						}
					}
				}
			}
			for (int k = 0; k < Pinch.N_BEADS; k++) {
				int b = Pinch.bead(k);
				if (t == b) {
					Vec3d from = base.add(Pinch.beadFrom(k));
					Vec3d to = ground(base.x + Pinch.beadTo(k).x, base.z + Pinch.beadTo(k).z);
					Vec3d d = to.subtract(from);
					Vfx.play(world, VfxIds.PINCH_BEAD, from, d.normalize(), (float) (d.length() / Pinch.BEAD_FLIGHT), 0);
					Kit.sound(world, from, SoundEvents.ENTITY_BLAZE_SHOOT, 96.0F, 1.2F);
					Kit.sound(world, from, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 64.0F, 1.4F);
				}
				if (t == b + Pinch.BEAD_FLIGHT) {
					Vec3d to = ground(base.x + Pinch.beadTo(k).x, base.z + Pinch.beadTo(k).z);
					Vfx.play(world, VfxIds.PINCH_POP, to, 1.0F);
					Vfx.shake(world, to, 100, 1.0F);
					Kit.blast(world, to.add(0, 0.5, 0), 2.6F, null, Destruction.enabled(world));
					crater(to, 4);
					blast(this, to, 10, 16.0F, 1.6, 4);
				}
			}
			if (t == Pinch.COLLAPSE) {
				collapse();
			}
			if (strip != null) {
				strip.limit = Pinch.front(t);
				strip.step(4000);
			}
			if (t > Pinch.COLLAPSE && t <= Pinch.COLLAPSE + 40) {
				double front = Pinch.front(t);
				if (t % 2 == 0) {
					// The shockwave throws the land outward ahead of it.
					double a = FossilShapes.hash(t, 23) * MathHelper.TAU;
					Vec3d p = ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front);
					for (FallingBlockEntity f : Kit.lift(world, p, 5, 10, true, false)) {
						Vec3d out = new Vec3d(Math.cos(a), 0, Math.sin(a));
						debris.throwOne(f, out.multiply(1.2 + seed(f, 14) * 1.0).add(0, 0.6 + seed(f, 15) * 0.6, 0));
					}
				}
				for (LivingEntity e : Kit.living(world, base, front + 2)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					Vec3d to = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z);
					double d = Math.max(0.5, to.length());
					if (d > front - 6) {
						double f = 1 - d / (Pinch.R + 4);
						Kit.push(e, to.multiply(1 / d).multiply(2.4 * f).add(0, 0.9 * f, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), (float) (26 * f));
					}
				}
			}
			debris.tick(world, 0xA0B8FF, p -> impact(p, 0xA0B8FF));
			drain();
			return t >= Pinch.END && (strip == null || strip.isDone()) && debris.settled();
		}

		/** A block in the field: spiralling in and up toward the column, faster as the pinch tightens. */
		private Vec3d drag(FallingBlockEntity f, float k) {
			Vec3d p = f.getPos().subtract(base);
			double h = Math.max(0.5, Math.hypot(p.x, p.z));
			double ux = p.x / h, uz = p.z / h;
			double spin = 0.5 + 0.8 * k, in = 0.3 + 0.9 * k;
			double ty = 6 + (1 - h / Pinch.DRAG) * 34 + seed(f, 16) * 8;
			return new Vec3d(-uz * spin - ux * in, MathHelper.clamp((ty - p.y) * 0.08, -0.6, 0.8), ux * spin - uz * in);
		}

		/** The column falling in on itself: a fusion flash, a crater, a fused core, lightning, the land thrown out. */
		private void collapse() {
			Vec3d mid = base.add(0, Pinch.HEIGHT * 0.4, 0);
			Vfx.play(world, VfxIds.PINCH_COLLAPSE, base, 1.0F);
			Vfx.flash(world, mid, 1.0F, 700, 0xE8F0FF);
			Vfx.screen(world, base, 300, 1, 0.7F, 10);
			Vfx.shake(world, base, 500, 2.4F);
			Kit.sound(world, mid, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
			Kit.sound(world, mid, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.5F);
			Kit.sound(world, mid, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.5F);
			// What it had dragged in but not yet eaten is hurled back out.
			debris.release(f -> {
				Vec3d out = new Vec3d(f.getX() - base.x, 0, f.getZ() - base.z);
				out = out.lengthSquared() < 1.0E-3 ? heading : out.normalize();
				return out.multiply(1.8 + seed(f, 17) * 1.2).add(0, 0.6 + seed(f, 18) * 0.8, 0);
			});
			bigCrater(base, Pinch.CRATER);
			for (int i = 0; i < 10; i++) {
				double a = i * MathHelper.TAU / 10 + 0.2, r = Pinch.CRATER + 3 + FossilShapes.hash(i, 19) * 12;
				Vec3d p = ground(base.x + Math.cos(a) * r, base.z + Math.sin(a) * r);
				DetonationManager.schedule(world, 4 + i * 3, () -> lightning(world, p));
			}
			if (Destruction.enabled(world)) {
				strip = TerrainJob.disc(world, base, Pinch.CRATER, Pinch.R, this::strip);
				strip.limit = 0;
				// The fused core: everything it ate, crushed into a ball at the bottom of the crater.
				DetonationManager.schedule(world, 80, () -> {
					Vec3d g = Kit.ground(world, base.x, base.z);
					Kit.sphere(world, BlockPos.ofFloored(g.add(0, 2, 0)), 3.4, Blocks.CRYING_OBSIDIAN.getDefaultState());
					Kit.sphere(world, BlockPos.ofFloored(g.add(0, 2, 0)), 1.8, Blocks.MAGMA_BLOCK.getDefaultState());
				});
			}
		}

		/** Swept by the shockwave: leaves and plants gone, trees snapped off at the stump, the topsoil scoured. */
		private void strip(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			for (int y = Math.min(c.top(), surf + 30); y > surf; y--) {
				BlockState s = c.get(y);
				if (s.isAir()) {
					continue;
				}
				if (s.isIn(BlockTags.LEAVES) || FossilEvents2.plant(s)) {
					c.set(y, Blocks.AIR.getDefaultState());
				} else if (s.isIn(BlockTags.LOGS) && y > surf + 2) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			BlockState g = c.get(surf);
			if (g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.PODZOL) || g.isOf(Blocks.MYCELIUM)) {
				double n = c.noise(91);
				c.set(surf, (n < 0.55 ? Blocks.COARSE_DIRT : n < 0.75 ? Blocks.DIRT : Blocks.GRAVEL).getDefaultState());
			}
		}
	}

	// ====================================================================== MAGNETIC BOTTLE

	/**
	 * MAGNETIC BOTTLE. A little sun of magenta plasma forms over the bomb, held in a cage of glowing field lines, and
	 * sinks slowly toward the ground. Its magnetism tears up the land and lifts everything alive, and pulls it all into
	 * orbit round it; field lines start flaring out and lashing the ground. The bottle fails: all twelve lines snap
	 * and whip down, carving molten trenches outward like spokes, and everything in orbit is flung out. Jets burst
	 * from its poles - one drilling a shaft deep into the earth - and a ring of plasma flies out of its equator,
	 * slicing through everything at waist height and throwing what stood above. Then what is left of the sun
	 * implodes and detonates.
	 */
	public static class MagneticBottle extends PlasmaBase {
		private TerrainJob cut;
		private int drilled;

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				start("magnetic_bottle");
				Vfx.newFx(world, FX_BOTTLE, base, heading, 1.0F, 0xFF50E0, 0, 0, Bottle.END);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.8F);
				Kit.sound(world, base, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.5F);
			}
			Vec3d sun = base.add(0, Bottle.y(t), 0);
			if (t < Bottle.FAIL) {
				float k = t / (float) Bottle.FAIL;
				if (t % 14 == 0) {
					Kit.sound(world, sun, SoundEvents.BLOCK_BEACON_AMBIENT, 96.0F, 0.5F + 1.2F * k);
					Kit.sound(world, sun, SoundEvents.BLOCK_PORTAL_AMBIENT, 48.0F, 0.6F + k);
				}
				if (t > Bottle.HOLD) {
					// Its magnetism tears the land up and pulls it all into orbit.
					if (t % 4 == 0 && debris.held.size() < 160) {
						tear(base, Bottle.PULL, 5);
						if (t % 16 == 0) {
							Kit.sound(world, base, SoundEvents.BLOCK_CHAIN_PLACE, 64.0F, 0.5F);
						}
					}
					debris.steer(f -> orbit(f, sun, t, 7 + seed(f, 20) * 8));
					if (t % 2 == 0) {
						for (LivingEntity e : Kit.living(world, sun, Bottle.PULL)) {
							if (Kit.creativeOrSpectator(e)) {
								continue;
							}
							Vec3d v = toward(e.getPos(), orbitPoint(e.getId(), e.getPos(), sun, t, 10), 0.12, 0.6);
							Kit.velocity(e, v);
							e.fallDistance = 0;
							if (e.getPos().distanceTo(sun) < Bottle.SUN + 3 && t % 10 == 0) {
								e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 5.0F);
								e.setOnFireFor(3);
							}
						}
					}
				}
			}
			for (int k = 0; k < Bottle.LASHES + Bottle.FLARES.length; k++) {
				if (t == Bottle.lashAt(k)) {
					lash(k, sun);
				}
			}
			if (t == Bottle.FAIL) {
				Vfx.play(world, VfxIds.BOTTLE_SNAP, sun, 1.0F);
				Vfx.flash(world, sun, 0.7F, 300, 0xFFB0F0);
				Vfx.shake(world, sun, 260, 1.8F);
				Kit.sound(world, sun, SoundEvents.BLOCK_GLASS_BREAK, 128.0F, 0.5F);
				Kit.sound(world, sun, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 1.0F);
				Kit.sound(world, sun, SoundEvents.BLOCK_CHAIN_BREAK, 128.0F, 0.5F);
				// Everything in orbit flung outward.
				debris.release(f -> {
					Vec3d out = f.getPos().subtract(sun);
					out = out.lengthSquared() < 1.0E-3 ? heading : out.normalize();
					return out.multiply(1.6 + seed(f, 21) * 1.2).add(0, 0.5, 0);
				});
				for (LivingEntity e : Kit.living(world, sun, Bottle.PULL + 4)) {
					if (!Kit.creativeOrSpectator(e)) {
						Vec3d out = e.getPos().subtract(sun).normalize();
						Kit.velocity(e, out.multiply(2.0).add(0, 0.8, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 10.0F);
					}
				}
			}
			if (t == Bottle.JETS) {
				Vfx.play(world, VfxIds.BOTTLE_JET, sun, UP, 1.0F, 0);
				Vfx.play(world, VfxIds.BOTTLE_JET, sun, new Vec3d(0, -1, 0), 1.0F, 0);
				Kit.sound(world, sun, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.6F);
				Kit.sound(world, sun, SoundEvents.ENTITY_BLAZE_SHOOT, 128.0F, 0.5F);
			}
			if (t > Bottle.JETS && t <= Bottle.JETS + 50) {
				drill(t - Bottle.JETS);
			}
			if (t == Bottle.RING) {
				Vfx.play(world, VfxIds.BOTTLE_RING, sun, 1.0F);
				Kit.sound(world, sun, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 128.0F, 0.5F);
				Kit.sound(world, sun, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.7F);
				if (Destruction.enabled(world)) {
					cut = TerrainJob.disc(world, base, 2, Bottle.R, this::slice);
					cut.limit = 0;
				}
			}
			if (cut != null) {
				cut.limit = Bottle.ring(t);
				cut.step(4000);
			}
			if (t > Bottle.RING && t <= Bottle.RING_END) {
				double r = Bottle.ring(t), r0 = Bottle.ring(t - 1);
				if (t % 4 == 0) {
					Kit.sound(world, base.add(heading.multiply(r)), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 96.0F, 0.6F);
				}
				if (t % 2 == 0) {
					// Whatever stood above the cut is thrown off by it.
					for (int i = 0; i < 3; i++) {
						double a = FossilShapes.hash(t, 31 + i) * MathHelper.TAU;
						Vec3d p = base.add(Math.cos(a) * r, 0, Math.sin(a) * r);
						for (FallingBlockEntity f : Kit.lift(world, p, 3, 4, true, false)) {
							debris.throwOne(f, new Vec3d(Math.cos(a), 0, Math.sin(a)).multiply(1.4 + seed(f, 22)).add(0, 0.4, 0));
						}
					}
				}
				for (LivingEntity e : Kit.living(world, base, r + 2)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					double d = Math.hypot(e.getX() - base.x, e.getZ() - base.z);
					double band = e.getY() - ground(e.getX(), e.getZ()).y;
					if (d >= r0 - 2 && d <= r + 2 && band > -1 && band < 4) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 22.0F);
						e.setOnFireFor(6);
					}
				}
			}
			if (t == Bottle.NOVA) {
				nova();
			}
			debris.tick(world, 0xFF50E0, p -> impact(p, 0xFF50E0));
			drain();
			return t >= Bottle.END && (cut == null || cut.isDone()) && debris.settled();
		}

		/** Where something in orbit should be heading: round the sun's axis, rising and falling through the loops. */
		private static Vec3d orbitPoint(int id, Vec3d pos, Vec3d sun, int t, double r) {
			Vec3d rel = pos.subtract(sun);
			double a = Math.atan2(rel.z, rel.x) + 0.09 + FossilShapes.hash(id, 24) * 0.05;
			double y = Math.sin(t * 0.045 + id) * r * 0.55;
			return sun.add(Math.cos(a) * r, y, Math.sin(a) * r);
		}

		private Vec3d orbit(FallingBlockEntity f, Vec3d sun, int t, double r) {
			return toward(f.getPos(), orbitPoint(f.getId(), f.getPos(), sun, t, r), 0.18, 1.3);
		}

		/** A field line whipping down: a molten trench curving out from under the sun, sparks all along it. */
		private void lash(int k, Vec3d sun) {
			List<Vec3d> pts = new ArrayList<>();
			for (double u = 0; u <= 1.0001; u += 0.02) {
				Vec3d l = Bottle.lash(k, u);
				pts.add(ground(base.x + l.x, base.z + l.z));
			}
			boolean flare = k >= Bottle.LASHES;
			for (int i = 0; i < pts.size(); i += 6) {
				Vfx.play(world, VfxIds.BOTTLE_LASH, pts.get(i), flare ? 0.8F : 1.2F);
			}
			Vec3d end = pts.get(pts.size() - 1);
			Kit.sound(world, end, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 96.0F, flare ? 1.4F : 1.0F);
			Kit.sound(world, pts.get(pts.size() / 2), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 96.0F, 0.5F);
			Vfx.shake(world, end, 120, flare ? 0.6F : 1.0F);
			trench(pts, flare ? 1.6 : 2.2, flare ? 3 : 5, 0.06);
			for (Vec3d p : pts) {
				for (LivingEntity e : Kit.living(world, p, 3)) {
					if (!Kit.creativeOrSpectator(e)) {
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 18.0F);
						e.setOnFireFor(6);
						Kit.push(e, new Vec3d(0, 0.9, 0));
					}
				}
			}
		}

		/** The lower jet drilling down: a shaft opening deeper each tick, its walls glowing molten. */
		private void drill(int dt) {
			if (!Destruction.enabled(world)) {
				return;
			}
			int to = (int) (Bottle.SHAFT * Math.min(1, dt / 40.0));
			int bx = MathHelper.floor(base.x), bz = MathHelper.floor(base.z), by = MathHelper.floor(base.y);
			for (; drilled <= to; drilled++) {
				int y = by - drilled;
				for (int x = -4; x <= 4; x++) {
					for (int z = -4; z <= 4; z++) {
						double d = Math.sqrt(x * x + z * z);
						BlockPos p = new BlockPos(bx + x, y, bz + z);
						if (d <= 3.2) {
							q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						} else if (d <= 4.2 && PlasmaShapes.hash(x * 7 + y, z) < 0.45) {
							q().add(p, Blocks.MAGMA_BLOCK.getDefaultState(), BlockQueue.SOLID_ONLY);
						}
					}
				}
			}
		}

		/**
		 * The ring passing: whatever stands one to three blocks over the ground is cut away. Where the column is a
		 * building, its ground floor is cut at the bomb's own level, so whole buildings are sliced through.
		 */
		private void slice(TerrainJob.Column c) {
			int surf = FossilEvents2.groundY(c);
			int by = MathHelper.floor(base.y) - 1;
			if (surf > by + 4 && !isGround(c.get(surf))) {
				surf = by;
			}
			int top = c.top();
			for (int y = surf + 1; y <= surf + 3 && y <= top + 1; y++) {
				BlockState s = c.get(y);
				if (!s.isAir() && !s.isOf(Blocks.BEDROCK)) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			BlockState below = c.get(surf);
			if (below.isIn(BlockTags.LOGS) && c.noise(95) < 0.5) {
				c.set(surf, Blocks.MAGMA_BLOCK.getDefaultState());
			}
		}

		/** What is left of the sun implodes and detonates: a white-out, a crater, a shockwave over everything. */
		private void nova() {
			Vec3d c = base.add(0, Bottle.LOW, 0);
			Vfx.play(world, VfxIds.BOTTLE_NOVA, c, 1.0F);
			Vfx.flash(world, c, 1.0F, 700, 0xFFE0FA);
			Vfx.screen(world, c, 300, 1, 0.7F, 10);
			Vfx.shake(world, c, 500, 2.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.4F);
			Kit.sound(world, c, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.6F);
			bigCrater(base, Bottle.CRATER);
			blast(this, c, 50, 30.0F, 2.6, 6);
			for (int i = 0; i < 6; i++) {
				double a = i * MathHelper.TAU / 6, r = Bottle.CRATER + 6;
				Vec3d p = ground(base.x + Math.cos(a) * r, base.z + Math.sin(a) * r);
				DetonationManager.schedule(world, 3 + i * 2, () -> lightning(world, p));
			}
		}
	}
}
