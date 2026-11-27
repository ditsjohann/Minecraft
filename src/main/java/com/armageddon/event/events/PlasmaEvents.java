package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Five plasma bombs, each a different way plasma behaves: the PLASMOID SWARM (ball lightning), the SOLAR ARCH (a
 * coronal loop that erupts), the FUSION GEYSER (an eruption that rains back down), the Z-PINCH (a column crushed by
 * its own field) and the MAGNETIC BOTTLE (a caged sun whose cage fails). Most of what they look like is the particle
 * engine (PlasmaVfx); the Swarm and the Bottle also have a client effect (PlasmaFx) for their orbs, sun and cage.
 */
public final class PlasmaEvents {
	private PlasmaEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: PlasmaFx). */
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

	abstract static class PlasmaBase extends OddBombEvents.Base {
		Vec3d heading, base;

		net.minecraft.server.world.ServerWorld w() {
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

		/** A small crater with a scorched, half-molten rim. */
		void crater(Vec3d at, double r) {
			if (!Destruction.enabled(world)) {
				return;
			}
			Kit.sphere(world, BlockPos.ofFloored(at.add(0, -0.8, 0)), r, Blocks.AIR.getDefaultState());
			Kit.ringCrack(world, at, r + 1.5, 1, Blocks.MAGMA_BLOCK.getDefaultState(), 0.45F);
		}
	}

	static boolean ground(BlockState g) {
		return FossilEvents2.earth(g);
	}

	// ====================================================================== PLASMOID SWARM

	/**
	 * PLASMOID SWARM. A core of crackling violet plasma gathers over the bomb and starts spitting out ball lightning,
	 * one after another: each plasmoid hops and wanders out over the land on its own path, arcing to whatever it passes,
	 * and bursts where it ends up, leaving a scorched little crater. When the last has gone, the core bursts too.
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
			if (t < Plasmoid.BURST && t % 12 == 0) {
				Kit.sound(world, core, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 24.0F, 1.6F + (t % 5) * 0.1F);
			}
			for (int k = 0; k < Plasmoid.N; k++) {
				if (t == Plasmoid.launch(k)) {
					Kit.sound(world, core, SoundEvents.ENTITY_BLAZE_SHOOT, 48.0F, 1.6F);
					Kit.sound(world, core, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 32.0F, 1.8F);
				}
				Vec3d p = orb(k, t);
				if (p == null) {
					continue;
				}
				if ((t + k) % 10 == 0) {
					Kit.sound(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 16.0F, 1.8F);
					for (LivingEntity e : Kit.living(world, p, Plasmoid.size(k) + 2.5)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION), 4.0F);
						}
					}
				}
				if (t == Plasmoid.pop(k)) {
					Vec3d g = ground(p.x, p.z);
					Vfx.play(world, VfxIds.PLASMOID_POP, p, (float) (0.8 + Plasmoid.size(k) * 0.3));
					Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 48.0F, 1.3F);
					Kit.sound(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 32.0F, 1.6F);
					Vfx.shake(world, p, 40, 0.5F);
					crater(g, 2.2 + Plasmoid.size(k) * 0.5);
					blast(this, p, 6, 12.0F, 1.0, 3);
				}
			}
			if (t == Plasmoid.BURST) {
				Vfx.play(world, VfxIds.PLASMOID_BURST, core, 1.0F);
				Vfx.flash(world, core, 0.5F, 200, 0xE0C0FF);
				Vfx.shake(world, core, 160, 1.2F);
				Kit.sound(world, core, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.8F);
				Kit.sound(world, core, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 96.0F, 1.0F);
				crater(base, 5);
				blast(this, core, 16, 18.0F, 1.6, 4);
			}
			drain();
			return t >= Plasmoid.END;
		}

		/** Plasmoid {@code k} in the world, riding over the ground (null when it is not out). */
		private Vec3d orb(int k, int t) {
			Vec3d l = Plasmoid.pos(k, t);
			if (l == null) {
				return null;
			}
			Vec3d w = base.add(l.x, 0, l.z);
			return new Vec3d(w.x, ground(w.x, w.z).y + l.y, w.z);
		}
	}

	// ====================================================================== SOLAR ARCH

	/**
	 * SOLAR ARCH. Two footpoints burst open either side of the bomb, fountaining plasma, and a twisted rope of it
	 * arches between them like a loop on the face of the sun, climbing and winding tighter. It snaps: the arch erupts
	 * forward as a coronal mass ejection and a storm of plasma sweeps out over a long wedge of land in front of it,
	 * charring every tree, setting the ground alight and glazing the sand.
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
					crater(f, 3);
				}
			}
			if (t == Arch.ARCH) {
				Vfx.play(world, VfxIds.ARCH_ROPE, base, right, 1.0F, 0);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 96.0F, 0.5F);
			}
			if (t > Arch.ARCH && t < Arch.SNAP) {
				if (t % 20 == 0) {
					Kit.sound(world, base.add(0, Arch.height(t), 0), SoundEvents.BLOCK_FIRE_AMBIENT, 64.0F, 0.5F);
					Kit.sound(world, base.add(0, Arch.height(t), 0), SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F + (t - Arch.ARCH) * 0.004F);
				}
				if (t % 10 == 0) {
					// Anything under the arch is scorched by it.
					for (int i = 0; i <= 16; i++) {
						double th = Math.PI * i / 16;
						Vec3d p = base.add(right.multiply(-Arch.SPAN * Math.cos(th))).add(0, Arch.height(t) * Math.sin(th), 0);
						for (LivingEntity e : Kit.living(world, p, 4)) {
							if (!Kit.creativeOrSpectator(e)) {
								e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 5.0F);
								e.setOnFireFor(5);
							}
						}
					}
					if (Destruction.enabled(world) && t % 40 == 0) {
						// The footpoints keep melting.
						double r = 2.5 + (t - Arch.ARCH) / 60.0;
						Kit.ringCrack(world, fa, r, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
						Kit.ringCrack(world, fb, r, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
					}
				}
			}
			if (t == Arch.SNAP) {
				Vec3d top = base.add(0, Arch.height(t), 0);
				Vfx.play(world, VfxIds.ARCH_CME, top, heading, 1.0F, 0);
				Vfx.flash(world, top, 0.6F, 400, 0xFFD080);
				Vfx.shake(world, top, 300, 1.6F);
				Kit.sound(world, top, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.6F);
				Kit.sound(world, top, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
				Kit.sound(world, top, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.6F);
				if (Destruction.enabled(world)) {
					wedge = TerrainJob.disc(world, base, Arch.REACH, this::scorch);
					wedge.limit = 0;
				}
			}
			if (wedge != null) {
				wedge.limit = t >= Arch.BLAST_END ? Arch.REACH + 1 : Arch.front(t);
				wedge.step(3500);
			}
			if (t > Arch.SNAP && t <= Arch.BLAST_END) {
				double front = Arch.front(t);
				if (t % 3 == 0) {
					for (int k = -2; k <= 2; k++) {
						double a = Math.atan2(heading.z, heading.x) + k * Arch.WEDGE / 2.2;
						Vec3d p = ground(base.x + Math.cos(a) * front, base.z + Math.sin(a) * front);
						Vfx.play(world, VfxIds.ARCH_SCORCH, p, (float) (1.4 - 0.6 * front / Arch.REACH));
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
						Kit.push(e, heading.multiply(1.2).add(0, 0.5, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 14.0F);
						e.setOnFireFor(8);
					}
				}
			}
			drain();
			return t >= Arch.END && (wedge == null || wedge.isDone());
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

		/** Swept by the eruption: trees charred, plants burned, the ground scorched and molten, sand glazed. */
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
			if (!ground(g)) {
				return;
			}
			double n = c.noise(83);
			Block top = n < 0.22 ? Blocks.MAGMA_BLOCK : n < 0.6 ? Blocks.COARSE_DIRT : n < 0.82 ? Blocks.BLACKSTONE : Blocks.NETHERRACK;
			c.set(surf, top.getDefaultState());
			if (c.noise(85) < 0.1 && c.get(surf + 1).isAir()) {
				c.set(surf + 1, Blocks.FIRE.getDefaultState());
			}
		}
	}

	// ====================================================================== FUSION GEYSER

	/**
	 * FUSION GEYSER. The ground glows and cracks in rings over a pocket of fusing plasma. It erupts: a geyser of cyan
	 * plasma eighty blocks high, spreading into a mushroom at the top, and it rains back down in burning drops over
	 * everything around, each one scorching where it lands. The vent is left a glassy, molten pit.
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
					Kit.ringCrack(world, base, 3 + k * 6, 1, Blocks.MAGMA_BLOCK.getDefaultState(), 0.5F);
				}
			}
			if (t == Geyser.ERUPT) {
				Vfx.play(world, VfxIds.GEYSER_ERUPT, base, 1.0F);
				Vfx.flash(world, base.add(0, 20, 0), 0.5F, 300, 0xB0FFFF);
				Vfx.shake(world, base, 240, 1.6F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_BLAZE_SHOOT, 128.0F, 0.4F);
				Kit.sound(world, base, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 96.0F, 1.2F);
				vent();
			}
			if (t > Geyser.ERUPT && t < Geyser.RAIN_END && t % 4 == 0) {
				// The column throws everything in it skyward.
				for (LivingEntity e : Kit.living(world, base.add(0, 6, 0), 9)) {
					if (!Kit.creativeOrSpectator(e)) {
						Kit.push(e, new Vec3d(0, 1.2, 0));
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
			drain();
			return t >= Geyser.END;
		}

		/** The vent: a pit blown open, lined with molten rock and glass fused cyan. */
		private void vent() {
			if (!Destruction.enabled(world)) {
				return;
			}
			Kit.sphere(world, BlockPos.ofFloored(base.add(0, -2, 0)), 6, Blocks.AIR.getDefaultState());
			for (int i = 0; i < 70; i++) {
				double a = PlasmaShapes.hash(i, 31) * MathHelper.TAU, r = 6 + PlasmaShapes.hash(i, 32) * 2;
				BlockPos top = surface(MathHelper.floor(base.x + Math.cos(a) * r), MathHelper.floor(base.z + Math.sin(a) * r));
				double n = PlasmaShapes.hash(i, 33);
				q().add(top, (n < 0.4 ? Blocks.MAGMA_BLOCK : n < 0.75 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.CYAN_TERRACOTTA).getDefaultState(),
						BlockQueue.SOLID_ONLY);
			}
			Kit.ringCrack(world, base, 10, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.6F);
		}

		/** A drop of plasma coming down: a hiss, a scorch, a little fire. */
		private void land(int i) {
			Vec3d l = Geyser.drop(i);
			Vec3d g = ground(base.x + l.x, base.z + l.z);
			Vfx.play(world, VfxIds.GEYSER_SPLASH, g, 1.0F);
			blast(this, g, 3, 6.0F, 0, 4);
			if (!Destruction.enabled(world)) {
				return;
			}
			BlockPos top = surface(MathHelper.floor(g.x), MathHelper.floor(g.z));
			BlockState s = world.getBlockState(top);
			if (s.isIn(BlockTags.LEAVES)) {
				q().add(top, Blocks.AIR.getDefaultState(), BlockQueue.ANY);
				return;
			}
			if (!ground(s)) {
				return;
			}
			double n = PlasmaShapes.hash(i, 34);
			Block b = n < 0.35 ? Blocks.MAGMA_BLOCK : n < 0.6 ? Blocks.BLACKSTONE : n < 0.8 ? Blocks.CYAN_TERRACOTTA : Blocks.LIGHT_BLUE_STAINED_GLASS;
			q().add(top, b.getDefaultState(), BlockQueue.ANY);
			if (PlasmaShapes.hash(i, 35) < 0.4) {
				q().add(top.up(), Blocks.FIRE.getDefaultState(), BlockQueue.AIR_ONLY);
			}
		}
	}

	// ====================================================================== Z-PINCH

	/**
	 * Z-PINCH. A column of blue-white plasma stands up out of the bomb, nearly sixty blocks tall, and rings of magnetic
	 * field slide up and down it, crushing it; it narrows and breaks into beads, dragging everything nearby toward it.
	 * The beads are squeezed out one by one and fly off to burst on the land. Then the column collapses into itself:
	 * a fusion flash, and a shockwave rolls out that strips the land flat, snapping the trees off at the stump.
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
				if (t > Pinch.SQUEEZE && t % 5 == 0) {
					// The field drags everything nearby in toward the column.
					for (LivingEntity e : Kit.living(world, base, 22)) {
						if (Kit.creativeOrSpectator(e)) {
							continue;
						}
						Vec3d to = new Vec3d(base.x - e.getX(), 0, base.z - e.getZ());
						double d = Math.max(0.5, to.length());
						Kit.push(e, to.multiply(0.12 * k / d * 4));
						if (d < Pinch.RADIUS + 1.5) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.ELECTROCUTION), 6.0F);
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
					Kit.sound(world, from, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 64.0F, 1.4F);
				}
				if (t == b + Pinch.BEAD_FLIGHT) {
					Vec3d to = ground(base.x + Pinch.beadTo(k).x, base.z + Pinch.beadTo(k).z);
					Vfx.play(world, VfxIds.PINCH_POP, to, 1.0F);
					Vfx.shake(world, to, 80, 0.8F);
					Kit.sound(world, to, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.9F);
					crater(to, 3.2);
					blast(this, to, 9, 14.0F, 1.2, 3);
				}
			}
			if (t == Pinch.COLLAPSE) {
				Vec3d mid = base.add(0, Pinch.HEIGHT * 0.4, 0);
				Vfx.play(world, VfxIds.PINCH_COLLAPSE, base, 1.0F);
				Vfx.flash(world, mid, 0.9F, 600, 0xE8F0FF);
				Vfx.screen(world, base, 300, 1, 0.6F, 8);
				Vfx.shake(world, base, 400, 2.0F);
				Kit.sound(world, mid, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 128.0F, 0.4F);
				Kit.sound(world, mid, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 0.5F);
				Kit.sound(world, mid, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 128.0F, 0.5F);
				if (Destruction.enabled(world)) {
					Kit.sphere(world, BlockPos.ofFloored(base.add(0, -3, 0)), 9, Blocks.AIR.getDefaultState());
					Kit.ringCrack(world, base, 12, 2, Blocks.MAGMA_BLOCK.getDefaultState(), 0.7F);
					strip = TerrainJob.disc(world, base, 10, Pinch.R, this::strip);
					strip.limit = 0;
				}
			}
			if (strip != null) {
				strip.limit = Pinch.front(t);
				strip.step(4000);
			}
			if (t > Pinch.COLLAPSE && t <= Pinch.COLLAPSE + 40) {
				double front = Pinch.front(t);
				for (LivingEntity e : Kit.living(world, base, front + 2)) {
					if (Kit.creativeOrSpectator(e)) {
						continue;
					}
					Vec3d to = new Vec3d(e.getX() - base.x, 0, e.getZ() - base.z);
					double d = Math.max(0.5, to.length());
					if (d > front - 6) {
						double f = 1 - d / (Pinch.R + 4);
						Kit.push(e, to.multiply(1 / d).multiply(2.2 * f).add(0, 0.8 * f, 0));
						e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), (float) (24 * f));
					}
				}
			}
			drain();
			return t >= Pinch.END && (strip == null || strip.isDone());
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
	 * sinks slowly toward the ground, spinning faster, burning brighter, the cage starting to buckle. The bottle fails:
	 * the lines snap, jets burst out of both poles - one up into the sky, one drilling a shaft deep into the earth -
	 * and a ring of plasma flies out of its equator, slicing through every tree, wall and creature standing at waist
	 * height for sixty blocks around.
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
				if (t > Bottle.HOLD && t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, sun, Bottle.CAGE)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA), 4.0F);
							e.setOnFireFor(3);
						}
					}
				}
			}
			if (t == Bottle.FAIL) {
				Vfx.play(world, VfxIds.BOTTLE_SNAP, sun, 1.0F);
				Vfx.flash(world, sun, 0.6F, 300, 0xFFB0F0);
				Vfx.shake(world, sun, 200, 1.4F);
				Kit.sound(world, sun, SoundEvents.BLOCK_GLASS_BREAK, 128.0F, 0.5F);
				Kit.sound(world, sun, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 128.0F, 1.2F);
				Kit.sound(world, sun, SoundEvents.BLOCK_CHAIN_BREAK, 128.0F, 0.5F);
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
			drain();
			return t >= Bottle.END && (cut == null || cut.isDone());
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
				for (int x = -3; x <= 3; x++) {
					for (int z = -3; z <= 3; z++) {
						double d = Math.sqrt(x * x + z * z);
						BlockPos p = new BlockPos(bx + x, y, bz + z);
						if (d <= 2.4) {
							q().add(p, Blocks.AIR.getDefaultState(), BlockQueue.NOT_AIR);
						} else if (d <= 3.3 && PlasmaShapes.hash(x * 7 + y, z) < 0.4) {
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
			if (surf > by + 4 && !ground(c.get(surf))) {
				surf = by;
			}
			int top = c.top();
			for (int y = surf + 1; y <= surf + 3 && y <= top + 1; y++) {
				BlockState s = c.get(y);
				if (!s.isAir() && !s.isOf(Blocks.BEDROCK)) {
					c.set(y, Blocks.AIR.getDefaultState());
				}
			}
			// The stumps left glowing where they were cut.
			BlockState below = c.get(surf);
			if (below.isIn(BlockTags.LOGS) && c.noise(95) < 0.5) {
				c.set(surf, Blocks.MAGMA_BLOCK.getDefaultState());
			}
		}
	}

	static double hash(int i, int k) {
		return FossilShapes.hash(i, k);
	}
}
