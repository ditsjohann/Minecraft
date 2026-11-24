package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.event.Kit;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.XenoShapes;
import com.armageddon.vfx.VfxIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * XENOFORM (world destruction). Technology five thousand years past ours, and utterly indifferent. There is no
 * explosion. The bomb goes silent and a point of nothing opens in the air; it unfolds into a hypercube turning
 * through a direction that does not exist. A plane of light passes down through the land and back up, and everything
 * living is marked. Then the land is taken apart: block by block, from the centre out, everything rises in perfectly
 * straight lines and is sorted by colour into rings round the hypercube, a catalogue of the place. Whatever was
 * living goes up with it and is disassembled. What is left is a perfectly flat black plain ruled with lines of light,
 * cut clean at its edge. The rings fold into the hypercube, the hypercube folds into a monolith that sets itself down
 * in the middle of the plain, and a column of light leaves for the sky.
 */
public final class XenoEvents {
	private XenoEvents() {
	}

	/** Effect id for {@link com.armageddon.network.FxType#NEW_FX} (client: XenoFx), and the cinematic scene. */
	public static final int FX_XENO = 58, CINE_XENO = 11;

	public static class Xenoform extends SciFiEvents.Base {
		private Vec3d base, heading;
		private double r;
		private int floorY, cx, cz;
		private TerrainJob harvest;
		private final Set<UUID> tagged = new HashSet<>();

		@Override
		public boolean tick() {
			int t = age();
			if (t == 0) {
				Vec3d g = center();
				base = Kit.ground(world, g.x, g.z);
				heading = Kit.dir(yaw());
				r = Destruction.radius(world, (float) XenoShapes.R);
				floorY = MathHelper.floor(base.y) + XenoShapes.FLOOR;
				cx = MathHelper.floor(base.x);
				cz = MathHelper.floor(base.z);
				WorldCutsceneEvents.begin(this, world, base, "block.armageddon.xenoform", CINE_XENO, heading, XenoShapes.CINE);
				Vfx.newFx(world, FX_XENO, base, heading, (float) r, 0xE0D8FF, 0, 0, XenoShapes.END);
				Kit.sound(world, base, SoundEvents.BLOCK_END_PORTAL_SPAWN, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 96.0F, 0.4F);
				if (Destruction.enabled(world)) {
					harvest = TerrainJob.disc(world, base, r, this::paint);
					harvest.limit = -1;
				}
			}
			if (t % 40 == 0 && t < XenoShapes.END - 40) {
				Vfx.skyTint(world, base, 100000, 0x08040E, 0.7F);
			}
			if (t == XenoShapes.UNFOLD) {
				Vfx.play(world, VfxIds.XENO_UNFOLD, base.add(0, XenoShapes.H, 0), 1.0F);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_ACTIVATE, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 96.0F, 0.5F);
				Vfx.shake(world, base, 100000, 0.4F);
			}
			if (t > XenoShapes.UNFOLD && t < XenoShapes.DEPART && t % 30 == 0) {
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_AMBIENT, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.5F);
			}
			if (t == XenoShapes.SCAN) {
				Kit.sound(world, base, SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 96.0F, 0.6F);
			}
			if (t >= XenoShapes.SCAN && t <= XenoShapes.SCAN_END && t % 4 == 0) {
				scan(base.y + XenoShapes.scanY(t));
			}
			if (t >= XenoShapes.HARVEST) {
				double front = XenoShapes.front(t, r);
				if (harvest != null) {
					harvest.limit = t >= XenoShapes.HARVEST_END ? r + 1 : front;
					harvest.step(t >= XenoShapes.HARVEST_END ? 6000 : 4500);
				}
				if (t < XenoShapes.HARVEST_END) {
					lift(front, t);
					if (t % 20 == 0) {
						Kit.sound(world, base, SoundEvents.BLOCK_BEACON_POWER_SELECT, 96.0F, 0.5F + (float) (front / r) * 0.4F);
						Kit.sound(world, base, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 64.0F, 0.5F);
						Vfx.shake(world, base, 100000, 0.3F);
					}
				}
			}
			if (t == XenoShapes.COMPRESS) {
				Kit.sound(world, base, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), 96.0F, 0.5F);
				Kit.sound(world, base, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, 96.0F, 0.5F);
			}
			if (t == XenoShapes.LAND) {
				land();
			}
			if (t == XenoShapes.DEPART) {
				Vec3d top = new Vec3d(base.x, floorY + XenoShapes.MONOLITH + 1, base.z);
				Vfx.play(world, VfxIds.XENO_DEPART, top, 1.0F);
				Vfx.flash(world, top, 0.5F, 100000, 0xF0F0FF);
				Kit.sound(world, top, SoundEvents.BLOCK_BEACON_DEACTIVATE, 96.0F, 0.5F);
				Kit.sound(world, top, SoundEvents.BLOCK_END_PORTAL_SPAWN, 96.0F, 1.4F);
			}
			boolean done = harvest == null || harvest.isDone();
			return t >= XenoShapes.END && done || t > XenoShapes.END + 1500;
		}

		/** The plane of light passing: everything living it crosses is marked, and glows from then on. */
		private void scan(double y) {
			for (LivingEntity e : Kit.living(world, base, r)) {
				if (Math.abs(e.getY() - y) < 3 && tagged.add(e.getUuid())) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 1200, 0, false, false));
					Vfx.play(world, VfxIds.XENO_TAG, e.getPos(), 1.0F);
					Kit.sound(world, e.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 16.0F, 0.6F);
				}
			}
		}

		/**
		 * Inside the front, everything goes up in a straight line toward the rings, and at the top it is taken apart.
		 */
		private void lift(double front, int t) {
			double top = base.y + XenoShapes.H - 16;
			for (Entity e : Kit.entities(world, base, front + 2, x -> x instanceof LivingEntity || x instanceof ItemEntity)) {
				if (e instanceof LivingEntity l && Kit.creativeOrSpectator(l)) {
					continue;
				}
				Vec3d to = new Vec3d(base.x - e.getX(), 0, base.z - e.getZ());
				if (Math.hypot(to.x, to.z) > front) {
					continue;
				}
				if (e.getY() > top) {
					Vfx.play(world, VfxIds.XENO_DEREZ, e.getPos().add(0, e.getHeight() * 0.5, 0), Math.max(0.6F, e.getWidth()));
					if (e instanceof LivingEntity l) {
						Kit.sound(world, e.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 32.0F, 0.4F);
						l.damage(ModDamageTypes.source(world, ModDamageTypes.ANNIHILATION), 10000.0F);
					} else {
						e.discard();
					}
					continue;
				}
				Kit.velocity(e, new Vec3d(to.x * 0.004, 0.42, to.z * 0.004));
				e.fallDistance = 0;
				if (e instanceof LivingEntity l && t % 10 == 0) {
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 40, 0, false, false));
				}
			}
		}

		/** One column of the harvest: everything above the new floor taken, the floor ruled with light, the rim cut clean. */
		private void paint(TerrainJob.Column c) {
			int dx = c.x - cx, dz = c.z - cz;
			double d = Math.sqrt(dx * dx + dz * dz);
			int top = Math.min(c.top(), floorY + 160);
			if (d > r - 1.5) {
				// The rim: a perfectly smooth black wall from the floor up to where the land was.
				int land = Math.max(c.surface(), floorY);
				for (int y = floorY; y <= land; y++) {
					c.set(y, Blocks.BLACK_CONCRETE.getDefaultState());
				}
				c.clear(land + 1, top);
				return;
			}
			c.clear(floorY + 1, top);
			boolean grid = Math.floorMod(dx, 10) == 0 || Math.floorMod(dz, 10) == 0;
			boolean ring = Math.abs(d - Math.round(d / 24.0) * 24.0) < 0.5 && d > 4;
			if (grid || ring) {
				c.set(floorY, (ring ? Blocks.PURPLE_STAINED_GLASS : Blocks.CYAN_STAINED_GLASS).getDefaultState());
				c.set(floorY - 1, Blocks.SEA_LANTERN.getDefaultState());
			} else {
				c.set(floorY, Blocks.BLACK_CONCRETE.getDefaultState());
				BlockState below = c.get(floorY - 1);
				if (below.isAir() || !below.getFluidState().isEmpty()) {
					c.set(floorY - 1, Blocks.BLACK_CONCRETE.getDefaultState());
				}
			}
		}

		/**
		 * The hypercube sets itself down as a monolith in the ratio 1:4:9, ringed with light, with eight needles
		 * standing round it on the plain.
		 */
		private void land() {
			Vec3d foot = new Vec3d(base.x, floorY + 1, base.z);
			Vfx.play(world, VfxIds.XENO_LAND, foot, 1.0F);
			Vfx.shake(world, foot, 100000, 1.6F);
			Vfx.flash(world, foot, 0.4F, 100000, 0xE0E0FF);
			Kit.sound(world, foot, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 96.0F, 0.5F);
			Kit.sound(world, foot, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 96.0F, 0.3F);
			Kit.damage(world, foot, 12, 10000.0F, ModDamageTypes.ANNIHILATION, null, owner());
			Vec3d side = new Vec3d(-heading.z, 0, heading.x);
			BlockState black = Blocks.BLACK_CONCRETE.getDefaultState();
			for (int w = -8; w < 8; w++) {
				for (int dd = -2; dd < 2; dd++) {
					for (int y = 0; y < XenoShapes.MONOLITH; y++) {
						Vec3d p = foot.add(side.multiply(w + 0.5)).add(heading.multiply(dd + 0.5)).add(0, y, 0);
						put(BlockPos.ofFloored(p), black);
					}
				}
			}
			// A ring of light round its foot.
			for (int k = 0; k < 96; k++) {
				double a = k * MathHelper.TAU / 96;
				BlockPos p = BlockPos.ofFloored(foot.add(Math.cos(a) * 13, -1, Math.sin(a) * 13));
				put(p, Blocks.SEA_LANTERN.getDefaultState());
			}
			// Eight needles.
			for (int k = 0; k < 8; k++) {
				double a = k * MathHelper.TAU / 8 + Math.atan2(heading.z, heading.x);
				Vec3d n = foot.add(Math.cos(a) * r * 0.55, 0, Math.sin(a) * r * 0.55);
				int tall = 12 + (int) (XenoShapes.hash(k, 3) * 8);
				for (int y = 0; y < tall; y++) {
					put(BlockPos.ofFloored(n.add(0, y, 0)), black);
				}
				put(BlockPos.ofFloored(n.add(0, tall, 0)), Blocks.END_ROD.getDefaultState());
			}
		}
	}
}
