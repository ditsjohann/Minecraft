package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.city.BlockQueue;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.event.Kit;
import com.armageddon.event.events.StrongBombEvents.RingSweep;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.FossilShapes;
import com.armageddon.util.FossilShapes.Leviathan;
import com.armageddon.util.FossilShapes.Mammoth;
import com.armageddon.util.FossilShapes.Ptero;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Three more fossils: the MAMMOTH (an ice age rolls out from its trumpet), the LEVIATHAN (a sea serpent breaching
 * through a ghost of the ancient sea, ending in a tidal wave) and the PTEROSAUR (a skeleton that flies, dive-bombs and
 * crumbles).
 */
public final class FossilEvents {
	private FossilEvents() {
	}

	/** Effect ids for {@link com.armageddon.network.FxType#NEW_FX} (client: FossilFx). */
	public static final int FX_MAMMOTH = 55, FX_LEVIATHAN = 56, FX_PTERO = 57;

	/**
	 * A skeleton planned block by block: each entry is {x, y, z, material, order}. It is placed in order, so it can
	 * rise from the bottom up or breach from tail to head.
	 */
	public static final class Bones {
		public final List<int[]> list = new ArrayList<>();
		int order;

		void ball(Vec3d c, double r, int kind) {
			int R = MathHelper.ceil(r);
			for (int x = -R; x <= R; x++) {
				for (int y = -R; y <= R; y++) {
					for (int z = -R; z <= R; z++) {
						if (x * x + y * y + z * z <= r * r + 0.3) {
							list.add(new int[]{MathHelper.floor(c.x) + x, MathHelper.floor(c.y) + y, MathHelper.floor(c.z) + z, kind, order});
						}
					}
				}
			}
		}

		public void line(Vec3d a, Vec3d b, double ra, double rb, int kind) {
			double len = a.distanceTo(b);
			for (double s = 0; s <= len; s += 0.5) {
				double u = s / Math.max(len, 0.01);
				ball(a.lerp(b, u), ra + (rb - ra) * u, kind);
			}
		}

		void line(Vec3d a, Vec3d b, double r, int kind) {
			line(a, b, r, r, kind);
		}

		public void dot(Vec3d p, int kind) {
			list.add(new int[]{MathHelper.floor(p.x), MathHelper.floor(p.y), MathHelper.floor(p.z), kind, order});
		}

		/** Drops repeated voxels, the later entry's material winning. */
		void dedupe() {
			java.util.LinkedHashMap<Long, int[]> m = new java.util.LinkedHashMap<>();
			for (int[] b : list) {
				m.put(BlockPos.asLong(b[0], b[1], b[2]), b);
			}
			list.clear();
			list.addAll(m.values());
		}

		void sortByHeight() {
			dedupe();
			list.sort((a, b) -> Integer.compare(a[1], b[1]));
		}

		void sortByOrder() {
			dedupe();
			list.sort((a, b) -> Integer.compare(a[4], b[4]));
		}
	}

	// ====================================================================== MAMMOTH

	/**
	 * MAMMOTH FOSSIL. A frozen fissure opens and a woolly mammoth's skeleton rises out of the permafrost, tusks curling,
	 * ice still clinging to its feet. Its eyes catch a cold blue light. It trumpets, and an ice age rolls out over the
	 * land: water freezes, snow piles up, spikes of ice burst out of the ground, everything caught is frozen. Then it
	 * stomps twice and the frozen ground shatters around it.
	 */
	public static class MammothFossil extends OddBombEvents.Base {
		private static BlockState mat(int k) {
			return (switch (k) {
				case 1 -> Blocks.SEA_LANTERN;
				case 2 -> Blocks.SMOOTH_QUARTZ;
				case 3 -> Blocks.PACKED_ICE;
				case 4 -> Blocks.BLUE_ICE;
				default -> Blocks.BONE_BLOCK;
			}).getDefaultState();
		}

		private Vec3d heading, base;
		private Bones bones = new Bones();
		private int placed;
		private RingSweep sweep;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("mammoth_fossil", Formatting.AQUA);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				Kit.fissure(world, base.subtract(heading.multiply(16)), heading, 40, 7, 9, Blocks.PACKED_ICE.getDefaultState());
				Kit.sound(world, g, SoundEvents.BLOCK_GLASS_BREAK, 48.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 32.0F, 0.4F);
				Vfx.shake(world, g, 120, 1.0F);
				bones = plan(base, heading);
				sweep = new RingSweep((int) Mammoth.R);
			}
			if (t >= Mammoth.RISE && t <= Mammoth.RISEN) {
				int to = (int) ((t - Mammoth.RISE) / (double) (Mammoth.RISEN - Mammoth.RISE) * bones.list.size());
				for (; placed < Math.min(to, bones.list.size()); placed++) {
					int[] b = bones.list.get(placed);
					q().add(b[0], b[1], b[2], mat(b[3]), BlockQueue.ANY);
				}
				if (t % 6 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_GLASS_BREAK, 32.0F, 0.5F);
					Kit.sound(world, base, SoundEvents.ENTITY_SKELETON_STEP, 32.0F, 0.4F);
					Vfx.play(world, VfxIds.MAMMOTH_HEAVE, base, heading, 1.0F, 0);
				}
			}
			if (t == Mammoth.EYES) {
				Vfx.newFx(world, FX_MAMMOTH, base, heading, 1.0F, 0x80E0FF, 0, 0, Mammoth.END - Mammoth.EYES);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 48.0F, 0.5F);
			}
			if (t == Mammoth.TRUMPET) {
				Vec3d mouth = base.add(Mammoth.skull(heading)).add(heading.multiply(5)).add(0, -3, 0);
				Kit.sound(world, mouth, SoundEvents.GOAT_HORN_SOUNDS.get(2).value(), 96.0F, 0.5F);
				Kit.sound(world, mouth, SoundEvents.ENTITY_RAVAGER_ROAR, 64.0F, 1.2F);
				Kit.sound(world, mouth, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 64.0F, 0.7F);
				Vfx.shake(world, mouth, 160, 1.0F);
			}
			if (t >= Mammoth.TRUMPET && t < Mammoth.END - 40 && t % 40 == (Mammoth.TRUMPET % 40)) {
				Vfx.skyTint(world, base, Mammoth.R + 100, 0xB8D8F0, 0.55F);
			}
			if (t >= Mammoth.TRUMPET && t <= Mammoth.FREEZE_END + 4) {
				double front = Mammoth.front(t);
				if (Destruction.enabled(world)) {
					sweep.advance(front, 700, (dx, dz, d) -> freeze(MathHelper.floor(base.x) + dx, MathHelper.floor(base.z) + dz, d));
				}
				if (t % 10 == 0) {
					for (LivingEntity e : Kit.living(world, base, front)) {
						if (!Kit.creativeOrSpectator(e)) {
							e.setFrozenTicks(Math.min(e.getFrozenTicks() + 60, e.getMinFreezeDamageTicks() + 160));
							e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
						}
					}
				}
				if (t % 20 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_POWDER_SNOW_PLACE, 64.0F, 0.5F);
					Kit.sound(world, base, SoundEvents.ITEM_ELYTRA_FLYING, 48.0F, 0.6F);
				}
			}
			if (t == Mammoth.STOMP || t == Mammoth.STOMP2) {
				stomp(t == Mammoth.STOMP2 ? 1.4 : 1.0);
			}
			if (t == Mammoth.FINALE) {
				// The last trumpet: the eyes blaze, the air freezes white.
				Vec3d mouth = base.add(Mammoth.skull(heading)).add(heading.multiply(5)).add(0, -3, 0);
				Vfx.play(world, VfxIds.MAMMOTH_TRUMPET, mouth, heading, 1.6F, 0);
				Vfx.flash(world, mouth, 0.35F, 400, 0xD8F4FF);
				Vfx.screen(world, base, 400, 1, 0.35F, 4);
				Vfx.shake(world, base, 400, 1.4F);
				Kit.sound(world, mouth, SoundEvents.GOAT_HORN_SOUNDS.get(5).value(), 128.0F, 0.4F);
				Kit.sound(world, mouth, SoundEvents.ENTITY_RAVAGER_ROAR, 128.0F, 0.9F);
				Kit.sound(world, mouth, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 128.0F, 0.5F);
			}
			if (t >= Mammoth.SPIKES && t <= Mammoth.SPIKES_END) {
				spikes(t);
			}
			if (t == Mammoth.ENTOMB) {
				entomb();
			}
			drain();
			return t >= Mammoth.END;
		}

		private double spiked;

		/** Twelve lines of ice spikes racing out from the mammoth, taller and taller, throwing everything aside. */
		private void spikes(int t) {
			double reach = Mammoth.spikes(t);
			for (double u = spiked == 0 ? 8 : spiked + 3.2; u < reach; u += 3.2) {
				spiked = u;
				for (int k = 0; k < Mammoth.SPIKE_LINES; k++) {
					double ang = k * MathHelper.TAU / Mammoth.SPIKE_LINES + Math.atan2(heading.z, heading.x) + 0.13;
					double wob = Math.sin(u * 0.2 + k) * 2.5;
					Vec3d dir = new Vec3d(Math.cos(ang), 0, Math.sin(ang)), side = new Vec3d(-dir.z, 0, dir.x);
					Vec3d at = base.add(dir.multiply(u)).add(side.multiply(wob));
					Vec3d g = Kit.ground(world, at.x, at.z);
					double tall = 5 + u * 0.32 + FossilShapes.hash((int) u, k) * 5;
					if (Destruction.enabled(world)) {
						spike(g, dir.multiply(0.35).add(0, 1, 0).normalize(), tall, 1.3 + u * 0.018);
					}
					if (((int) u) % 3 == 0) {
						Vfx.play(world, VfxIds.MAMMOTH_SPIKE, g, (float) (0.8 + u / Mammoth.SPIKE_REACH));
					}
					for (LivingEntity e : Kit.living(world, g, 5)) {
						if (!Kit.creativeOrSpectator(e)) {
							Kit.push(e, dir.multiply(1.0).add(0, 1.4, 0));
							e.damage(ModDamageTypes.source(world, ModDamageTypes.CRYO), 18.0F);
							e.setFrozenTicks(e.getMinFreezeDamageTicks() + 200);
						}
					}
				}
			}
			if (t % 4 == 0) {
				Kit.sound(world, base.add(heading.multiply(reach * 0.5)), SoundEvents.BLOCK_GLASS_BREAK, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 96.0F, 0.5F);
				Vfx.shake(world, base, 300, 0.9F);
			}
		}

		/** One jagged spike of packed ice with a blue core, leaning outward. */
		private void spike(Vec3d foot, Vec3d dir, double tall, double r) {
			for (double s = -1; s <= tall; s += 0.5) {
				double u = Math.max(0, s / tall);
				double rr = r * (1 - u) + 0.2;
				Vec3d p = foot.add(dir.multiply(s));
				int R = MathHelper.ceil(rr);
				for (int x = -R; x <= R; x++) {
					for (int z = -R; z <= R; z++) {
						if (x * x + z * z <= rr * rr + 0.2) {
							BlockPos b = BlockPos.ofFloored(p.add(x, 0, z));
							q().add(b, (x == 0 && z == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).getDefaultState(), BlockQueue.ANY);
						}
					}
				}
			}
		}

		/** A glacier closes over the skeleton: it is left entombed in a dome of clear ice, like a find in a museum. */
		private void entomb() {
			Vfx.play(world, VfxIds.MAMMOTH_STOMP, base, 1.8F);
			Vfx.flash(world, base.add(0, 15, 0), 0.4F, 400, 0xE0F8FF);
			Kit.sound(world, base, SoundEvents.BLOCK_GLASS_PLACE, 128.0F, 0.4F);
			Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 128.0F, 0.5F);
			Kit.sound(world, base, SoundEvents.BLOCK_BEACON_DEACTIVATE, 96.0F, 0.6F);
			if (!Destruction.enabled(world)) {
				return;
			}
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			Vec3d mid = base.add(heading.multiply(3)).add(0, 12, 0);
			double ra = 26, rb = 11, rc = 18;
			for (int x = -28; x <= 28; x++) {
				for (int z = -28; z <= 28; z++) {
					for (int y = -14; y <= 22; y++) {
						Vec3d p = new Vec3d(x, y, z);
						double a = p.dotProduct(heading) / ra, b = p.dotProduct(right) / rb, cc = y / rc;
						double d = Math.sqrt(a * a + b * b + cc * cc);
						if (d <= 1.0 && d > 1 - 1.5 / rb && mid.y + y >= base.y) {
							q().add(BlockPos.ofFloored(mid.add(p)), Blocks.ICE.getDefaultState(), BlockQueue.AIR_ONLY);
						}
					}
				}
			}
		}

		/** A woolly mammoth in bone: barrel ribs, a high hump of spines, pillar legs, a domed skull and huge tusks. */
		public static Bones plan(Vec3d base, Vec3d heading) {
			Bones bones = new Bones();
			Vec3d h = heading, right = new Vec3d(-h.z, 0, h.x), up = new Vec3d(0, 1, 0);
			Vec3d o = base;
			// Spine, high over the shoulders, the tail drooping.
			for (double u = -13; u <= 11; u += 0.5) {
				double y = spineY(u);
				bones.ball(o.add(h.multiply(u)).add(0, y, 0), u < -9 ? 0.6 : 1.3, 0);
			}
			// The hump: tall spines over the shoulders.
			for (double u = -6; u <= 9; u += 1.5) {
				double tall = 1.5 + 6.5 * Math.exp(-Math.pow((u - 4) / 4.5, 2));
				Vec3d s = o.add(h.multiply(u)).add(0, spineY(u), 0);
				bones.line(s, s.add(h.multiply(-0.6)).add(0, tall, 0), 0.6, 0.4, 0);
			}
			// Barrel ribs.
			for (int k = 0; k < 9; k++) {
				double u = -5 + k * 1.6;
				double y = spineY(u);
				double depth = 8.5 - Math.abs(k - 4) * 0.7, width = 5.2 - Math.abs(k - 4) * 0.25;
				for (double a = 0; a <= Math.PI; a += 0.1) {
					Vec3d off = right.multiply(Math.cos(a) * width).add(0, -Math.sin(a) * depth * 0.85 - (1 - Math.abs(Math.cos(a))) * 0.6, 0);
					bones.ball(o.add(h.multiply(u)).add(0, y, 0).add(off), 0.55, 0);
				}
			}
			// The pelvis, a bar across the hips with blades hanging from it.
			Vec3d hip = o.add(h.multiply(-8)).add(0, spineY(-8), 0);
			bones.line(hip.add(right.multiply(-3.8)), hip.add(right.multiply(3.8)), 1.0, 0);
			for (int side = -1; side <= 1; side += 2) {
				bones.line(hip.add(right.multiply(side * 3.4)), hip.add(right.multiply(side * 2.5)).add(0, -3, 0).add(h.multiply(-1)), 0.8, 0);
			}
			// Four pillar legs on round feet.
			for (int side = -1; side <= 1; side += 2) {
				for (int leg = 0; leg < 2; leg++) {
					double u = leg == 0 ? 6 : -8;
					Vec3d top = o.add(h.multiply(u)).add(right.multiply(side * 3.8)).add(0, spineY(u) - 1, 0);
					Vec3d joint = o.add(h.multiply(u + (leg == 0 ? 0.6 : 1.2))).add(right.multiply(side * 3.9)).add(0, 8, 0);
					Vec3d foot = o.add(h.multiply(u + 0.3)).add(right.multiply(side * 3.9)).add(0, 1.4, 0);
					bones.line(top, joint, 1.7, 1.3, 0);
					bones.line(joint, foot, 1.3, 1.1, 0);
					bones.ball(foot.add(0, -0.4, 0), 1.6, 0);
					// Permafrost still clinging to the feet.
					for (int c = 0; c < 3; c++) {
						double ha = FossilShapes.hash(side * 7 + leg * 3, c) * MathHelper.TAU;
						Vec3d ice = foot.add(Math.cos(ha) * 2.2, -0.5 + c * 0.8, Math.sin(ha) * 2.2);
						bones.ball(ice, 1.0 + FossilShapes.hash(side + leg * 5, c + 9) * 1.2, c == 1 ? 4 : 3);
					}
				}
			}
			// Icicles hanging from the ribcage.
			for (int k = 0; k < 7; k++) {
				double u = -4 + k * 1.8;
				Vec3d bottom = o.add(h.multiply(u)).add(right.multiply((FossilShapes.hash(k, 3) - 0.5) * 4)).add(0, spineY(u) - 8.2, 0);
				bones.line(bottom, bottom.add(0, -1.5 - FossilShapes.hash(k, 4) * 2.5, 0), 0.4, 3);
			}
			// The neck and the domed skull, with a dropped jaw.
			Vec3d s = o.add(Mammoth.skull(h));
			bones.line(o.add(h.multiply(11)).add(0, spineY(11), 0), s.add(h.multiply(-2)), 1.5, 0);
			bones.ball(s.add(0, 1, 0), 3.6, 0);
			bones.ball(s.add(h.multiply(1.8)).add(0, -1.6, 0), 3.0, 0);
			bones.line(s.add(h.multiply(1.5)).add(0, -4.2, 0), s.add(h.multiply(4.5)).add(0, -4.5, 0), 0.9, 0);
			// Two huge tusks curling forward, down, out and up, the tips turning in.
			for (int side = -1; side <= 1; side += 2) {
				Vec3d p = s.add(h.multiply(3.5)).add(right.multiply(side * 1.6)).add(0, -2.2, 0);
				double theta = -1.15;
				int steps = 30;
				for (int i = 0; i < steps; i++) {
					Vec3d dir = h.multiply(Math.cos(theta)).add(up.multiply(Math.sin(theta))).add(right.multiply(side * (0.5 - i * 0.03)));
					Vec3d n = p.add(dir.normalize().multiply(0.75));
					double r = 1.15 - 0.7 * i / (double) steps;
					bones.line(p, n, r, 2);
					p = n;
					theta += 0.11;
				}
			}
			// The eyes, deep in the dome.
			for (int side = -1; side <= 1; side += 2) {
				bones.dot(s.add(h.multiply(2.9)).add(right.multiply(side * 2.5)).add(0, 0.8, 0), 1);
			}
			bones.sortByHeight();
			return bones;
		}

		private static double spineY(double u) {
			return 16 + 3.5 * Math.exp(-Math.pow((u - 5) / 5.0, 2)) - (u < -8 ? (u + 8) * (u + 8) * 0.12 : 0);
		}

		/** The ice age reaching one column: water freezes, snow piles up, now and then a spike of ice. */
		private void freeze(int x, int z, double d) {
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				return;
			}
			BlockPos top = surface(x, z);
			BlockState s = world.getBlockState(top);
			double h = FossilShapes.hash(x * 31, z * 17);
			if (!s.getFluidState().isEmpty()) {
				if (s.getFluidState().isIn(FluidTags.WATER)) {
					q().add(top, (d < 24 ? Blocks.BLUE_ICE : h < 0.3 ? Blocks.PACKED_ICE : Blocks.ICE).getDefaultState(), BlockQueue.ANY);
				} else if (s.getFluidState().isIn(FluidTags.LAVA)) {
					q().add(top, Blocks.OBSIDIAN.getDefaultState(), BlockQueue.ANY);
				}
				return;
			}
			if (s.isOf(Blocks.GRASS_BLOCK) || s.isOf(Blocks.DIRT) || s.isOf(Blocks.PODZOL) || s.isOf(Blocks.MYCELIUM) || s.isOf(Blocks.COARSE_DIRT)) {
				if (h < 0.18) {
					q().add(top, Blocks.SNOW_BLOCK.getDefaultState(), BlockQueue.ANY);
				}
			}
			// Snow piles deeper near the mammoth.
			int layers = MathHelper.clamp(1 + (int) (h * 3 + (1 - d / Mammoth.R) * 4), 1, 7);
			BlockPos above = top.up();
			BlockState a = world.getBlockState(above);
			if (a.isAir() || a.isReplaceable() && !a.isOf(Blocks.SNOW)) {
				q().add(above, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers), BlockQueue.ANY);
			}
			// Snow on the treetops too.
			int canopy = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
			if (canopy - 1 > top.getY() && world.getBlockState(new BlockPos(x, canopy - 1, z)).isIn(BlockTags.LEAVES)) {
				q().add(new BlockPos(x, canopy, z), Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 1 + (int) (h * 2)), BlockQueue.AIR_ONLY);
			}
			// Spikes of ice bursting out of the ground.
			if (d > 16 && FossilShapes.hash(x, z) < 0.0035) {
				int tall = 5 + (int) (FossilShapes.hash(z, x) * 12);
				for (int y = 0; y < tall; y++) {
					double r = 1.6 * (1 - y / (double) tall) + 0.3;
					for (int ox = -2; ox <= 2; ox++) {
						for (int oz = -2; oz <= 2; oz++) {
							if (ox * ox + oz * oz <= r * r) {
								q().add(new BlockPos(x + ox, top.getY() + 1 + y, z + oz), (y % 4 == 3 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).getDefaultState(),
										BlockQueue.ANY);
							}
						}
					}
				}
			}
		}

		/** A front foot comes down: the frozen ground shatters in rings and everything near is thrown and frozen. */
		private void stomp(double power) {
			Vec3d right = new Vec3d(-heading.z, 0, heading.x);
			Vec3d foot = Kit.ground(world, base.x + heading.x * 6 + right.x * 3.9, base.z + heading.z * 6 + right.z * 3.9);
			Kit.sound(world, foot, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.4F);
			Kit.sound(world, foot, SoundEvents.ENTITY_RAVAGER_STEP, 64.0F, 0.3F);
			Kit.sound(world, foot, SoundEvents.BLOCK_GLASS_BREAK, 64.0F, 0.5F);
			Vfx.shake(world, foot, 140, (float) (1.4 * power));
			Vfx.play(world, VfxIds.MAMMOTH_STOMP, foot, (float) power);
			Kit.ringCrack(world, base, 14 * power, 3, Blocks.PACKED_ICE.getDefaultState(), 0.7F);
			Kit.ringCrack(world, base, 26 * power, 2, Blocks.ICE.getDefaultState(), 0.5F);
			for (LivingEntity e : Kit.living(world, base, 40 * power)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(base);
				double d = Math.max(1, Math.hypot(to.x, to.z));
				double k = 1 - d / (40 * power);
				Kit.push(e, new Vec3d(to.x / d * 1.6 * k, 0.9 * k + 0.3, to.z / d * 1.6 * k));
				e.damage(ModDamageTypes.source(world, ModDamageTypes.CRYO), (float) (12 * k * power));
				e.setFrozenTicks(e.getMinFreezeDamageTicks() + 200);
			}
		}
	}

	// ====================================================================== LEVIATHAN

	/**
	 * LEVIATHAN FOSSIL. The ancient sea comes back as a ghost: a shimmering surface floods in over the land, with
	 * ammonites and fish drifting through it. The fossil of the serpent that swam it breaches out of the ground in
	 * great arches, tail first, and its skull rears up out of the sea with its jaws wide. Its eyes light, and a
	 * whole ghost sea gathers and rolls out in front of it as one great wave, leaving the land a sea floor.
	 */
	public static class LeviathanFossil extends OddBombEvents.Base {
		private static BlockState mat(int k) {
			return (switch (k) {
				case 1 -> Blocks.SEA_LANTERN;
				case 2 -> Blocks.PRISMARINE;
				case 3 -> Blocks.DARK_PRISMARINE;
				default -> Blocks.BONE_BLOCK;
			}).getDefaultState();
		}

		private Vec3d heading, base, origin;
		private Bones bones = new Bones();
		private int placed;
		private TerrainJob seabed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("leviathan_fossil", Formatting.DARK_AQUA);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				origin = Kit.ground(world, base.x + heading.x * Leviathan.HEAD, base.z + heading.z * Leviathan.HEAD);
				Vfx.newFx(world, FX_LEVIATHAN, base, heading, 1.0F, 0x40E0D0, 0, 0, Leviathan.END);
				Kit.sound(world, base, SoundEvents.AMBIENT_UNDERWATER_ENTER, 64.0F, 0.6F);
				Kit.sound(world, base, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 64.0F, 0.5F);
				bones = plan(base, heading);
			}
			if (t >= Leviathan.BREACH && t <= Leviathan.BREACHED) {
				int to = (int) ((t - Leviathan.BREACH) / (double) (Leviathan.BREACHED - Leviathan.BREACH) * bones.list.size());
				int lastOrder = -1;
				for (; placed < Math.min(to, bones.list.size()); placed++) {
					int[] b = bones.list.get(placed);
					q().add(b[0], b[1], b[2], mat(b[3]), BlockQueue.ANY);
					lastOrder = b[4];
				}
				if (t % 5 == 0 && lastOrder >= 0) {
					// Breaching: earth and spray bursting where the spine is coming out of the ground.
					double u = Leviathan.TAIL + lastOrder * 0.5;
					Vec3d p = Kit.ground(world, base.x + heading.x * u, base.z + heading.z * u);
					Vfx.play(world, VfxIds.LEVI_BREACH, p, heading, 1.0F, 0);
					Kit.sound(world, p, SoundEvents.ENTITY_SKELETON_STEP, 32.0F, 0.4F);
					Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_SPLASH, 32.0F, 0.5F);
				}
				if (t % 30 == 0) {
					Kit.sound(world, base, SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, 64.0F, 0.5F);
				}
			}
			// Under the ghost sea: everyone swims.
			if (t % 20 == 0 && Leviathan.sea(t) > 0.5) {
				for (LivingEntity e : Kit.living(world, base, Leviathan.SEA_R)) {
					if (e.getY() < base.y + Leviathan.SEA_H) {
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, true, false));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, true, false));
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, true, false));
					}
				}
			}
			if (t == Leviathan.EYES) {
				Kit.sound(world, base, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 96.0F, 0.6F);
			}
			if (t == Leviathan.WAVE) {
				Vec3d mouth = base.add(heading.multiply(Leviathan.HEAD + 6)).add(0, Leviathan.spine(Leviathan.HEAD), 0);
				Kit.sound(world, mouth, SoundEvents.ENTITY_RAVAGER_ROAR, 96.0F, 0.35F);
				Kit.sound(world, mouth, SoundEvents.ENTITY_WARDEN_ROAR, 96.0F, 0.5F);
				Vfx.play(world, VfxIds.LEVI_ROAR, mouth, heading, 1.0F, 0);
				Vfx.shake(world, mouth, 160, 1.2F);
			}
			if (t >= Leviathan.WAVE && t <= Leviathan.WAVE_END + 60) {
				wave(t);
			}
			drain();
			return t >= Leviathan.END;
		}

		/** The serpent: an undulating spine of vertebrae with spines and ribs, four paddles, and a long open-jawed skull. */
		public static Bones plan(Vec3d base, Vec3d heading) {
			Bones bones = new Bones();
			Plan sv = new Plan(bones, base, heading);
			Vec3d h = heading, right = new Vec3d(-h.z, 0, h.x), up = new Vec3d(0, 1, 0);
			for (double u = Leviathan.TAIL; u <= Leviathan.HEAD; u += 0.5) {
				bones.order = (int) Math.round((u - Leviathan.TAIL) / 0.5);
				Vec3d c = sv.at(u);
				double r = u < -40 ? 0.7 + (u + 52) / 12.0 * 1.1 : u > 20 ? 2.0 : 1.8;
				sv.ballAbove(c, r, 0);
				if (((int) Math.round(u * 2)) % 3 == 0 && u > -44) {
					// A tall spine tipped with petrified sea-green, and a pair of short ribs.
					double tall = u > 20 ? 1.6 : 2.8;
					Vec3d tip = c.add(0, tall + r, 0).subtract(h.multiply(0.5));
					sv.lineAbove(c, tip, 0.5, 0);
					sv.ballAbove(tip, 0.6, 2);
					if (u > -30 && u < 22) {
						for (int side = -1; side <= 1; side += 2) {
							Vec3d mid = c.add(right.multiply(side * 2.4)).add(0, -1.2, 0);
							sv.lineAbove(c, mid, 0.45, 0);
							sv.lineAbove(mid, mid.add(right.multiply(side * 0.6)).add(0, -1.8, 0), 0.4, 0);
						}
					}
				}
			}
			// Four paddles.
			for (double u : new double[]{6, -16}) {
				bones.order = (int) Math.round((u - Leviathan.TAIL) / 0.5);
				Vec3d c = sv.at(u);
				for (int side = -1; side <= 1; side += 2) {
					Vec3d elbow = c.add(right.multiply(side * 4)).add(0, -1.5, 0).subtract(h.multiply(1));
					sv.lineAbove(c, elbow, 0.9, 0);
					for (int f = -1; f <= 1; f++) {
						sv.lineAbove(elbow, elbow.add(right.multiply(side * 3.5)).add(0, -1.5, 0).subtract(h.multiply(1.5 + f * 1.2)), 0.45, 0);
					}
				}
			}
			// The skull: a long jaw opened wide, the upper tilted up and the lower dropped, lined with teeth.
			bones.order = (int) Math.round((Leviathan.HEAD - Leviathan.TAIL) / 0.5) + 1;
			Vec3d s = sv.at(Leviathan.HEAD);
			Vec3d fu = h.multiply(Math.cos(0.3)).add(up.multiply(Math.sin(0.3)));
			Vec3d nu = up.multiply(Math.cos(0.3)).subtract(h.multiply(Math.sin(0.3)));
			Vec3d fl = h.multiply(Math.cos(-0.45)).add(up.multiply(Math.sin(-0.45)));
			for (double d = 0; d <= 14; d += 0.5) {
				double w = 2.6 - d * 0.13, hgt = 2.2 - d * 0.12;
				Vec3d pu = s.add(fu.multiply(d)), pl = s.add(fl.multiply(d));
				for (double v = -w; v <= w; v += 0.5) {
					bones.ball(pu.add(right.multiply(v)), 0.5, 0);
					bones.ball(pu.add(right.multiply(v)).add(nu.multiply(hgt)), 0.5, 0);
					if (d <= 13) {
						bones.ball(pl.add(right.multiply(v)), 0.5, 0);
					}
				}
				if (((int) (d * 2)) % 3 == 0 && d > 1) {
					for (int side = -1; side <= 1; side += 2) {
						Vec3d eu = pu.add(right.multiply(side * w));
						bones.line(eu, eu.add(nu.multiply(hgt)), 0.45, 0);
						bones.line(eu, eu.subtract(nu.multiply(1.5)), 0.35, 0);
						Vec3d el = pl.add(right.multiply(side * w));
						bones.line(el, el.add(0, 1.4, 0), 0.35, 0);
					}
				}
			}
			for (int side = -1; side <= 1; side += 2) {
				bones.dot(s.add(fu.multiply(3)).add(nu.multiply(2.0)).add(right.multiply(side * 2.4)), 1);
			}
			bones.sortByOrder();
			return bones;
		}

		/** The planner's state: where the serpent lies, and only the bones out of the ground. */
		private record Plan(Bones bones, Vec3d base, Vec3d heading) {
			Vec3d at(double u) {
				Vec3d right = new Vec3d(-heading.z, 0, heading.x);
				return base.add(heading.multiply(u)).add(right.multiply(Math.sin(u * 0.08) * 6)).add(0, Leviathan.spine(u), 0);
			}

			void ballAbove(Vec3d c, double r, int kind) {
				if (c.y + r >= base.y - 1) {
					int before = bones.list.size();
					bones.ball(c, r, kind);
					// Only what is out of the ground: the rest of it is still down there.
					bones.list.subList(before, bones.list.size()).removeIf(b -> b[1] < base.y - 1);
				}
			}

			void lineAbove(Vec3d a, Vec3d b, double r, int kind) {
				double len = a.distanceTo(b);
				for (double s = 0; s <= len; s += 0.5) {
					ballAbove(a.lerp(b, s / Math.max(len, 0.01)), r, kind);
				}
			}
		}

		/** True if a point lies in the wave's arc, ahead of the skull. */
		private boolean inArc(double x, double z) {
			double dx = x - origin.x, dz = z - origin.z;
			double d = Math.hypot(dx, dz);
			if (d < 1) {
				return true;
			}
			return Math.acos(MathHelper.clamp((dx * heading.x + dz * heading.z) / d, -1, 1)) <= Leviathan.WAVE_ARC;
		}

		/** The tidal wave rolling out: everything in its path swept along and half drowned, the land left a sea floor. */
		private void wave(int t) {
			double front = Leviathan.wave(t);
			if (t == Leviathan.WAVE) {
				Kit.sound(world, origin, SoundEvents.ENTITY_GENERIC_SPLASH, 128.0F, 0.3F);
				Kit.sound(world, origin, SoundEvents.AMBIENT_UNDERWATER_EXIT, 128.0F, 0.5F);
				if (Destruction.enabled(world)) {
					seabed = TerrainJob.disc(world, origin, Leviathan.WAVE_REACH, this::seabed);
					seabed.limit = 0;
				}
			}
			if (seabed != null) {
				seabed.limit = t > Leviathan.WAVE_END ? Leviathan.WAVE_REACH + 1 : front - 4;
				seabed.step(3000);
			}
			if (t > Leviathan.WAVE_END) {
				return;
			}
			if (t % 3 == 0) {
				for (int k = -4; k <= 4; k++) {
					double ang = Math.atan2(heading.z, heading.x) + k * Leviathan.WAVE_ARC / 4.5;
					Vec3d at = Kit.ground(world, origin.x + Math.cos(ang) * front, origin.z + Math.sin(ang) * front);
					Vfx.play(world, VfxIds.LEVI_CREST, at, new Vec3d(Math.cos(ang), 0, Math.sin(ang)), (float) (1.2 - 0.6 * front / Leviathan.WAVE_REACH), 0);
				}
			}
			if (t % 8 == 0) {
				Vec3d at = origin.add(heading.multiply(front));
				Kit.sound(world, at, SoundEvents.WEATHER_RAIN_ABOVE, 128.0F, 0.4F);
				Kit.sound(world, at, SoundEvents.ENTITY_GENERIC_SPLASH, 128.0F, 0.5F);
				Vfx.shake(world, at, 120, 0.8F);
			}
			for (LivingEntity e : Kit.living(world, origin, front + 4)) {
				if (Kit.creativeOrSpectator(e) || !inArc(e.getX(), e.getZ())) {
					continue;
				}
				Vec3d out = new Vec3d(e.getX() - origin.x, 0, e.getZ() - origin.z);
				double d = out.length();
				if (d < front - 8 || d > front + 3) {
					continue;
				}
				out = out.normalize();
				Kit.velocity(e, out.multiply(1.1).add(0, 0.45, 0));
				e.extinguish();
				if (t % 10 == 0) {
					e.damage(world.getDamageSources().drown(), 7.0F);
				}
			}
		}

		/** Where the wave has been: plants swept away, the ground left sand, gravel and clay, dead coral, sea pickles. */
		private void seabed(TerrainJob.Column col) {
			if (!inArc(col.x + 0.5, col.z + 0.5)) {
				return;
			}
			int surf = col.surface();
			for (int y = Math.min(col.top(), surf + 16); y > surf; y--) {
				BlockState s = col.get(y);
				if (s.isIn(BlockTags.LEAVES) || s.isIn(BlockTags.FLOWERS) || s.isReplaceable() || s.isIn(BlockTags.SAPLINGS)) {
					col.set(y, Blocks.AIR.getDefaultState());
				}
			}
			BlockState g = col.get(surf);
			if (!(g.isIn(BlockTags.DIRT) || g.isOf(Blocks.GRASS_BLOCK) || g.isOf(Blocks.STONE) || g.isIn(BlockTags.SAND) || g.isOf(Blocks.GRAVEL))) {
				return;
			}
			double n = col.noise(41);
			col.set(surf, (n < 0.55 ? Blocks.SAND : n < 0.75 ? Blocks.GRAVEL : n < 0.85 ? Blocks.CLAY : Blocks.PRISMARINE).getDefaultState());
			double m = col.noise(43);
			if (col.get(surf + 1).isAir()) {
				if (m < 0.025) {
					col.set(surf + 1, Blocks.DEAD_BRAIN_CORAL_BLOCK.getDefaultState());
				} else if (m < 0.05) {
					col.set(surf + 1, Blocks.DEAD_TUBE_CORAL_BLOCK.getDefaultState());
				} else if (m < 0.07) {
					col.set(surf + 1, Blocks.DEAD_FIRE_CORAL_FAN.getDefaultState());
				} else if (m < 0.08) {
					col.set(surf + 1, Blocks.SEA_PICKLE.getDefaultState().with(net.minecraft.state.property.Properties.WATERLOGGED, false));
				} else if (m < 0.085) {
					col.set(surf + 1, Blocks.BONE_BLOCK.getDefaultState());
				}
			}
		}
	}

	// ====================================================================== PTEROSAUR

	/**
	 * PTEROSAUR FOSSIL. Bones burst out of the ground and assemble in the air into a pterosaur thirty blocks across,
	 * fire in its eyes and a ghost of its wings between the bones. It takes off and circles high, then dives three
	 * times, screeching, and each dive ends in a blast where it pulls up. A spire of bone grows under it; it lands on
	 * it, and crumbles to dust.
	 */
	public static class PterosaurFossil extends OddBombEvents.Base {
		private static final int SPIRE_START = Ptero.PERCH - 90;
		private Vec3d heading, base;
		private Bones spire = new Bones();
		private int placed;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				name("pterosaur_fossil", Formatting.GOLD);
				heading = Kit.dir(data.getFloat("Yaw"));
				base = Kit.ground(world, g.x, g.z);
				Kit.fissure(world, base.subtract(heading.multiply(10)), heading, 22, 5, 5, null);
				Vfx.newFx(world, FX_PTERO, base, heading, 1.0F, 0xFF8030, 0, 0, Ptero.END);
				Vfx.play(world, VfxIds.PTERO_BURST, base, 1.0F);
				Kit.sound(world, base, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 48.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_SKELETON_AMBIENT, 48.0F, 0.4F);
				Vfx.shake(world, base, 100, 0.8F);
				spire = planSpire(base);
			}
			if (t > 6 && t < Ptero.ASSEMBLE && t % 5 == 0) {
				Kit.sound(world, base.add(0, 10, 0), SoundEvents.ENTITY_SKELETON_STEP, 48.0F, 0.5F + t * 0.01F);
			}
			if (t == Ptero.TAKEOFF) {
				Kit.sound(world, base, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_PHANTOM_AMBIENT, 96.0F, 0.4F);
			}
			if (t > Ptero.TAKEOFF && t < Ptero.PERCH && t % 18 == 0) {
				Vec3d p = base.add(Ptero.pos(t));
				Kit.sound(world, p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 64.0F, 0.6F);
			}
			for (int k = 0; k < Ptero.DIVES.length; k++) {
				int d = t - Ptero.DIVES[k];
				if (d == 0) {
					Vec3d p = base.add(Ptero.pos(t));
					Kit.sound(world, p, SoundEvents.ENTITY_PHANTOM_SWOOP, 96.0F, 0.4F);
					Kit.sound(world, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 96.0F, 1.5F);
				}
				if (d == 10) {
					Vec3d p = base.add(Ptero.pos(t));
					Vec3d tgt = Kit.ground(world, base.x + Ptero.target(k).x, base.z + Ptero.target(k).z);
					Vfx.soundwave(world, p, tgt, 6.0F, 0xFFB060, 1.4F);
				}
				if (d == 24) {
					strike(k);
				}
			}
			if (t >= SPIRE_START && t < SPIRE_START + 60) {
				int to = (int) ((t - SPIRE_START) / 60.0 * spire.list.size());
				for (; placed < Math.min(to, spire.list.size()); placed++) {
					int[] b = spire.list.get(placed);
					q().add(b[0], b[1], b[2], b[3] == 1 ? Blocks.SHROOMLIGHT.getDefaultState() : Blocks.BONE_BLOCK.getDefaultState(), BlockQueue.ANY);
				}
				if (t % 8 == 0) {
					Kit.sound(world, base, SoundEvents.BLOCK_BONE_BLOCK_PLACE, 48.0F, 0.5F);
				}
			}
			if (t == Ptero.PERCH) {
				Vec3d top = base.add(0, Ptero.SPIRE, 0);
				Kit.sound(world, top, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 96.0F, 0.4F);
				Kit.sound(world, top, SoundEvents.ENTITY_PHANTOM_AMBIENT, 96.0F, 0.3F);
				Vfx.shake(world, top, 60, 0.6F);
			}
			if (t == Ptero.CRUMBLE) {
				Vec3d top = base.add(0, Ptero.SPIRE + 4, 0);
				Kit.sound(world, top, SoundEvents.ENTITY_SKELETON_DEATH, 96.0F, 0.4F);
				Kit.sound(world, top, SoundEvents.BLOCK_BONE_BLOCK_BREAK, 96.0F, 0.5F);
				Kit.sound(world, top, SoundEvents.BLOCK_SAND_BREAK, 96.0F, 0.5F);
				Vfx.play(world, VfxIds.PTERO_CRUMBLE, top, 1.0F);
				petrify();
				rubble();
			}
			drain();
			return t >= Ptero.END;
		}

		/** A dive strikes: a blast where the pterosaur pulls up, a crater, the ground cracked round it. */
		private void strike(int k) {
			Vec3d tgt = Kit.ground(world, base.x + Ptero.target(k).x, base.z + Ptero.target(k).z);
			double power = 1 + k * 0.25;
			Kit.sound(world, tgt, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.5F);
			Kit.sound(world, tgt, SoundEvents.ENTITY_PHANTOM_DEATH, 96.0F, 0.5F);
			Vfx.play(world, VfxIds.PTERO_IMPACT, tgt, (float) power);
			Vfx.shake(world, tgt, 120, (float) (1.0 * power));
			Kit.sphere(world, BlockPos.ofFloored(tgt.add(0, -1, 0)), 4.5 * power, Blocks.AIR.getDefaultState());
			Kit.ringCrack(world, tgt, 9 * power, 2, null, 0.6F);
			for (LivingEntity e : Kit.living(world, tgt, 16 * power)) {
				if (Kit.creativeOrSpectator(e)) {
					continue;
				}
				Vec3d to = e.getPos().subtract(tgt);
				double d = Math.max(0.5, to.length());
				double f = 1 - d / (16 * power);
				Kit.push(e, to.normalize().multiply(1.8 * f).add(0, 0.8 * f, 0));
				e.damage(world.getDamageSources().sonicBoom(host), (float) (16 * f * power));
			}
		}

		/** The spire of bone it lands on, tapering, with a nest of claws at the top. */
		public static Bones planSpire(Vec3d base) {
			Bones spire = new Bones();
			for (int y = 0; y <= Ptero.SPIRE; y++) {
				double u = y / Ptero.SPIRE;
				double r = 2.8 - 1.6 * u + 0.5 * Math.sin(y * 0.9);
				spire.ball(base.add(Math.sin(y * 0.4) * 0.8, y, Math.cos(y * 0.4) * 0.8), r, 0);
			}
			Vec3d top = base.add(0, Ptero.SPIRE, 0);
			for (int k = 0; k < 6; k++) {
				double a = k * MathHelper.TAU / 6;
				Vec3d out = top.add(Math.cos(a) * 2.5, 0.5, Math.sin(a) * 2.5);
				spire.line(top, out, 0.6, 0);
				spire.line(out, out.add(Math.cos(a) * 1.0, 2.2, Math.sin(a) * 1.0), 0.45, 0);
			}
			spire.dot(top.add(0, -2, 0), 1);
			spire.sortByHeight();
			return spire;
		}

		/** It turns to stone where it perches: its skeleton left standing on the spire, bone for bone, eyes still smouldering. */
		private void petrify() {
			if (!Destruction.enabled(world)) {
				return;
			}
			com.armageddon.util.PteroPose.Pose pose = com.armageddon.util.PteroPose.pose(Ptero.CRUMBLE, heading);
			Bones fossil = new Bones();
			for (com.armageddon.util.PteroPose.Seg seg : pose.segs) {
				double sc = com.armageddon.util.PteroPose.SC;
				fossil.line(base.add(seg.a()), base.add(seg.b()), Math.max(0.45, seg.ra() * sc), Math.max(0.45, seg.rb() * sc), 0);
			}
			for (Vec3d e : pose.eyes) {
				fossil.dot(base.add(e), 1);
			}
			fossil.dedupe();
			for (int[] b : fossil.list) {
				q().add(b[0], b[1], b[2], (b[3] == 1 ? Blocks.SHROOMLIGHT : Blocks.BONE_BLOCK).getDefaultState(), BlockQueue.ANY);
			}
		}

		/** Bones shaken loose, scattered round the spire's foot. */
		private void rubble() {
			if (!Destruction.enabled(world)) {
				return;
			}
			for (int i = 0; i < 40; i++) {
				double a = FossilShapes.hash(i, 51) * MathHelper.TAU, r = 3 + FossilShapes.hash(i, 52) * 12;
				int x = MathHelper.floor(base.x + Math.cos(a) * r), z = MathHelper.floor(base.z + Math.sin(a) * r);
				BlockPos top = surface(x, z).up();
				q().add(top, Blocks.BONE_BLOCK.getDefaultState(), BlockQueue.AIR_ONLY);
				if (FossilShapes.hash(i, 53) < 0.4) {
					q().add(top.up(), Blocks.BONE_BLOCK.getDefaultState(), BlockQueue.AIR_ONLY);
				}
			}
		}
	}
}
