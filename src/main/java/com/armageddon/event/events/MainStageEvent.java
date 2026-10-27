package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.ProtectedRegions;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.detonation.Detonations;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.util.MainStageSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MAIN STAGE. No cutscene: it all happens around you. Studio rigging lowers into the sky and a loose
 * light crashes down; the big lamps switch on one by one; the land is struck ring by ring into a taped
 * studio floor; a real stage rises in front of you (planks, red curtains, footlights, a proscenium arch
 * and a MAIN STAGE marquee); a painted-sky wall goes up all the way round with a staircase to an EXIT
 * door; and the lamps sweep the floor, burning it, until the curtain call.
 */
public class MainStageEvent extends WorldEvent {
	/** Block edits per tick, shared by the strike, the stage and the wall. */
	private static final int BUDGET = 2500;

	private record Op(long pos, BlockState state) {
	}

	private final Deque<Op> queue = new ArrayDeque<>();
	private final Map<FallingBlockEntity, Integer> flying = new HashMap<>();
	private Vec3d fwd, right;
	private int floorY, strikeR, wallR;
	private boolean wallQueued, stageQueued;

	@Override
	public boolean tick() {
		int t = age();
		Vec3d g = center();
		if (t == 0) {
			fwd = Kit.dir(data.getFloat("Yaw"));
			right = new Vec3d(-fwd.z, 0, fwd.x);
			floorY = MathHelper.floor(g.y) - 1;
			strikeR = (int) Destruction.radius(world, (float) MainStageSet.STRIKE_R);
			wallR = (int) Destruction.radius(world, (float) MainStageSet.WALL_R);
			CelestialEvents.announce(world, g, "block.armageddon.truman_show");
			// The rig, lamps and beams, from the moment it is lit.
			Vfx.custom(world, FxType.STAGE_SET, g, fwd, (float) MainStageSet.DOME_R, 0xFFF0D0, 0, 0, 0, MainStageSet.FINALE + 300, 1024);
			world.spawnParticles(ParticleTypes.NOTE, g.x, g.y + 2, g.z, 6, 1.5, 0.5, 1.5, 0.5);
			Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 32.0F, 0.4F);
		}
		// Places, everyone: three notes of a fanfare as the rig comes down.
		if (t == 10 || t == 16 || t == 22) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 12.0F, t == 10 ? 1.0F : t == 16 ? 1.26F : 1.5F);
		}
		if (t > 0 && t < MainStageSet.RIG_DOWN && t % 10 == 0) {
			Kit.sound(world, g, SoundEvents.BLOCK_CHAIN_PLACE, 32.0F, 0.5F);
		}
		if (t == MainStageSet.LIGHT_FALL) {
			Vec3d hit = Kit.ground(world, MainStageSet.crash(g, fwd).x, MainStageSet.crash(g, fwd).z);
			Vfx.streak(world, hit.add(0, 70, 0), hit, 2.0F, 0xFFF0C0, MainStageSet.LIGHT_HIT - MainStageSet.LIGHT_FALL);
			Kit.sound(world, g, SoundEvents.BLOCK_CHAIN_BREAK, 32.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 32.0F, 0.4F);
		}
		if (t == MainStageSet.LIGHT_HIT) {
			lightCrash(MainStageSet.crash(g, fwd));
		}
		// "CHUNK": the big studio lights switch on one by one.
		for (int i = 0; i < MainStageSet.LAMPS; i++) {
			if (t == MainStageSet.lampOn(i)) {
				Vec3d l = MainStageSet.lamp(g, fwd, i);
				Kit.sound(world, l, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.45F);
				Kit.sound(world, l, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
				Vfx.shake(world, g, 300, 0.6F);
			}
		}
		if (t >= MainStageSet.STRIKE && t < MainStageSet.STRIKE_END) {
			strike(g, t);
			if (t % 10 == 0) {
				Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 32.0F, 0.4F);
				Vfx.shake(world, g, 300, 0.5F);
			}
		}
		if (t >= MainStageSet.SCORCH && t < MainStageSet.SCORCH_END) {
			scorch(g, t);
		}
		if (t == MainStageSet.STAGE && !stageQueued) {
			stageQueued = true;
			queueStage(g);
			Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.BLOCK_WOOD_PLACE, 64.0F, 0.5F);
		}
		if (t == MainStageSet.WALL && !wallQueued) {
			wallQueued = true;
			queueWall(g);
		}
		if (t == MainStageSet.CURTAIN_CALL) {
			// Showtime: a drum roll, a fanfare, fireworks over the stage.
			Vec3d st = g.add(fwd.multiply(MainStageSet.STAGE_AT)).add(0, MainStageSet.ARCH_H, 0);
			Kit.sound(world, g, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 64.0F, 1.0F);
			Kit.sound(world, g, SoundEvents.ENTITY_PLAYER_LEVELUP, 64.0F, 0.8F);
			for (int i = 0; i < 8; i++) {
				Vfx.firework(world, st.add(right.multiply((i - 3.5) * 5)).add(0, 6 + (i % 3) * 4, 0), 3.0F, i % 5, 0xFFC040 + i * 0x101010);
			}
		}
		if (t > MainStageSet.CURTAIN_CALL && t < MainStageSet.CURTAIN_CALL + 40 && t % 3 == 0) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), 32.0F, 1.0F + world.random.nextFloat() * 0.2F);
		}
		wrap(g, t);
		flying.entrySet().removeIf(en -> {
			if (!en.getKey().isAlive() || t - en.getValue() > 55) {
				en.getKey().discard();
				return true;
			}
			return false;
		});
		drain();
		boolean done = t > MainStageSet.END && queue.isEmpty() && (devastation == null || devastation.isDone());
		return done || t > MainStageSet.END + 1200;
	}

	// ------------------------------------------------------------------ that's a wrap

	private com.armageddon.destruction.world.TerrainJob devastation;

	/**
	 * The set is struck for real: the lamps drop out of the rig and explode where they land, the painted wall
	 * topples outward in burning chunks, and the stage goes up in one last pyrotechnic blast whose shock front
	 * flattens everything for hundreds of blocks beyond the set.
	 */
	private void wrap(Vec3d g, int t) {
		if (t == MainStageSet.WRAP) {
			Kit.title(world, g, 400, net.minecraft.text.Text.literal("THAT'S A WRAP!").formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD),
					net.minecraft.text.Text.empty());
			Kit.sound(world, g, SoundEvents.BLOCK_CHAIN_BREAK, 64.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 64.0F, 0.4F);
		}
		if (t == MainStageSet.WRAP + MainStageSet.LAMP_FALL) {
			for (int i = 0; i < MainStageSet.LAMPS; i++) {
				Vec3d l = MainStageSet.lamp(g, fwd, i);
				Vec3d hit = Kit.ground(world, l.x, l.z);
				Kit.boom(world, hit.add(0, 1, 0), 9.0F, 0xFFC060, owner(), true);
				if (Destruction.enabled(world)) {
					com.armageddon.detonation.DetonationManager.add(new com.armageddon.detonation.CraterJob(world, BlockPos.ofFloored(hit), 9.0F,
							com.armageddon.detonation.CraterJob.Mode.NUKE));
				}
			}
			Vfx.shake(world, g, 400, 2.5F);
		}
		if (t == MainStageSet.WRAP + 50) {
			// The rig itself comes down across the floor.
			for (int k = 0; k < 10; k++) {
				double a = k * Math.PI * 2 / 10 + 0.3;
				Vec3d p = Kit.ground(world, g.x + Math.cos(a) * MainStageSet.WALL_R * 0.6, g.z + Math.sin(a) * MainStageSet.WALL_R * 0.6);
				final Vec3d at = p;
				com.armageddon.detonation.DetonationManager.schedule(world, k * 3, () -> Kit.boom(world, at.add(0, 1, 0), 5.0F, 0xFF9040, owner(), true));
			}
		}
		if (t >= MainStageSet.WALL_FALL && t < MainStageSet.WALL_FALL + 60 && Destruction.enabled(world)) {
			// The painted sky falls outward, a slice of wall at a time, as burning chunks.
			int slices = 60;
			int k = t - MainStageSet.WALL_FALL;
			double a0 = k * Math.PI * 2 / slices, a1 = (k + 1) * Math.PI * 2 / slices;
			for (double a = a0; a < a1; a += 0.5 / wallR) {
				for (int dr = -2; dr <= 2; dr++) {
					int x = MathHelper.floor(g.x + Math.cos(a) * (wallR + dr)), z = MathHelper.floor(g.z + Math.sin(a) * (wallR + dr));
					for (int y = floorY + 1; y <= floorY + MainStageSet.WALL_H + 2; y++) {
						BlockPos p = new BlockPos(x, y, z);
						BlockState st = world.getBlockState(p);
						if (st.isAir() || st.hasBlockEntity()) {
							continue;
						}
						if (world.random.nextInt(14) == 0 && flying.size() < 400) {
							Destruction.record(world, p, st);
							FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p, st);
							fb.setDestroyedOnLanding();
							fb.dropItem = false;
							Kit.velocity(fb, new Vec3d(Math.cos(a) * (0.8 + world.random.nextDouble()), 0.3 + world.random.nextDouble() * 0.6, Math.sin(a) * (0.8 + world.random.nextDouble())));
							flying.put(fb, t);
						} else {
							queue.add(new Op(p.asLong(), Blocks.AIR.getDefaultState()));
						}
					}
				}
			}
			if (k % 6 == 0) {
				Vec3d p = Kit.ground(world, g.x + Math.cos(a0) * wallR, g.z + Math.sin(a0) * wallR);
				Kit.boom(world, p.add(0, 4, 0), 4.0F, 0xFF7030, owner(), false);
			}
		}
		if (t == MainStageSet.FINALE - 40) {
			Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), 64.0F, 1.0F);
			Kit.title(world, g, 400, net.minecraft.text.Text.literal("AND FOR OUR FINAL ACT...").formatted(net.minecraft.util.Formatting.GOLD), net.minecraft.text.Text.empty());
		}
		if (t == MainStageSet.FINALE) {
			Vec3d st = Kit.ground(world, g.x + fwd.x * MainStageSet.STAGE_AT, g.z + fwd.z * MainStageSet.STAGE_AT);
			for (int i = 0; i < 12; i++) {
				final Vec3d p = st.add(right.multiply((i - 5.5) * 3)).add(0, 2 + (i % 3) * 3, 0);
				final int k = i;
				com.armageddon.detonation.DetonationManager.schedule(world, i * 2, () -> {
					Vfx.firework(world, p.add(0, 8, 0), 4.0F, k % 5, 0xFF4040 + k * 0x101010);
					Kit.boom(world, p, 4.0F, 0xFFD040, owner(), true);
				});
			}
			com.armageddon.detonation.DetonationManager.schedule(world, 30, () -> {
				Kit.boom(world, st.add(0, 4, 0), 18.0F, 0xFFB050, owner(), true);
				Vfx.flash(world, st, 1.0F, 1200, 0xFFF0D0);
				Vfx.ring(world, st, 500.0F, 10.0F, 0xFFD080, true, 100);
				Vfx.dome(world, st, 200.0F, 0xFFB050, 60);
				Vfx.pillar(world, st, 20.0F, 0xFFE0A0, false, 120);
				Detonations.blast(world, st, 260, 400.0F, ModDamageTypes.ANNIHILATION, owner(), 60, 8.0);
				if (Destruction.enabled(world)) {
					com.armageddon.detonation.DetonationManager.add(new com.armageddon.detonation.CraterJob(world, BlockPos.ofFloored(st),
							Destruction.radius(world, 34.0F), com.armageddon.detonation.CraterJob.Mode.NUKE));
					devastation = com.armageddon.destruction.world.Devastation.of(world, st, 0, Destruction.radius(world, 280.0F), floorY,
							com.armageddon.destruction.world.Devastation.FIRE);
					devastation.limit = 0;
				}
			});
		}
		if (devastation != null) {
			int k = t - MainStageSet.FINALE - 30;
			devastation.limit = 4.5 * k;
			devastation.step(3500);
			if (devastation.limit < 280 && k % 2 == 0) {
				WorldCutsceneEvents.shockFront(world, center().add(fwd.multiply(MainStageSet.STAGE_AT)), devastation.limit, 8, 30.0F, owner());
			}
		}
	}

	@Override
	public void finish() {
		for (FallingBlockEntity fb : flying.keySet()) {
			fb.discard();
		}
		flying.clear();
	}

	// ------------------------------------------------------------------ the falling light

	private void lightCrash(Vec3d l) {
		Vec3d p = Kit.ground(world, l.x, l.z);
		Vfx.layeredBlast(world, p, 4.0F, 0xFFF0C0);
		Vfx.flash(world, p, 0.35F, 120, 0xFFF4E0);
		Vfx.shake(world, p, 160, 1.6F);
		Vfx.ring(world, p.add(0, 0.3, 0), 18.0F, 1.5F, 0xFFE8C0, true, 30);
		Kit.sound(world, p, SoundEvents.BLOCK_GLASS_BREAK, 24.0F, 0.5F);
		Kit.sound(world, p, SoundEvents.BLOCK_ANVIL_LAND, 24.0F, 0.6F);
		Kit.sound(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 16.0F, 1.1F);
		Kit.damage(world, p, 9, 30.0F, ModDamageTypes.SHRAPNEL, null, owner());
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 40, 2, 1, 2, 0.05);
		world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 1, p.z, 60, 2, 1, 2, 0.4);
		if (!Destruction.enabled(world)) {
			return;
		}
		BlockPos c = BlockPos.ofFloored(p).down();
		Kit.sphere(world, c, 3.6, Blocks.AIR.getDefaultState());
		// The wreck: a black lamp housing on its side, its shattered lens still glowing, chain trailing.
		BlockPos base = c.down(2);
		Direction along = Direction.getFacing(right.x, 0, right.z);
		Direction face = Direction.getFacing(fwd.x, 0, fwd.z);
		for (int k = -2; k <= 2; k++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int ds = -1; ds <= 1; ds++) {
					BlockPos q = base.offset(along, k).up(dy + 1).offset(face, ds);
					boolean shell = Math.abs(dy) == 1 || Math.abs(ds) == 1;
					BlockState s = k == 2 ? (shell ? Blocks.WHITE_STAINED_GLASS.getDefaultState() : Blocks.SEA_LANTERN.getDefaultState())
							: shell ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.IRON_BLOCK.getDefaultState();
					Destruction.set(world, q, s, Kit.FLAGS);
				}
			}
		}
		for (int k = -2; k <= 2; k += 2) {
			Destruction.set(world, base.offset(along, k).up(3), Blocks.IRON_BARS.getDefaultState(), Kit.FLAGS);
		}
		for (int k = 0; k < 4; k++) {
			Destruction.set(world, base.offset(along, -3).up(1 + k), Blocks.CHAIN.getDefaultState(), Kit.FLAGS);
		}
		for (int i = 0; i < 6; i++) {
			BlockPos f = c.add(world.random.nextInt(9) - 4, 2, world.random.nextInt(9) - 4);
			if (world.getBlockState(f).isAir() && !world.getBlockState(f.down()).isAir()) {
				Destruction.set(world, f, Blocks.FIRE.getDefaultState(), Kit.FLAGS);
			}
		}
	}

	// ------------------------------------------------------------------ striking the set

	private BlockState floorAt(int x, int z, Vec3d g) {
		int dx = x - MathHelper.floor(g.x), dz = z - MathHelper.floor(g.z);
		if (Math.floorMod(dx, 12) == 0 || Math.floorMod(dz, 12) == 0) {
			return Blocks.YELLOW_CONCRETE.getDefaultState();
		}
		if (Math.floorMod(dx, 12) == 6 && Math.floorMod(dz, 12) == 6) {
			// Spike marks: where the props used to stand.
			return Blocks.WHITE_CONCRETE.getDefaultState();
		}
		return ((Math.floorDiv(dx, 6) + Math.floorDiv(dz, 6)) & 1) == 0 ? Blocks.BLACK_CONCRETE.getDefaultState() : Blocks.GRAY_CONCRETE.getDefaultState();
	}

	/** One ring of the strike: everything above the floor goes, the floor becomes taped studio concrete. */
	private void strike(Vec3d g, int t) {
		if (!Destruction.enabled(world) || strikeR < 4) {
			return;
		}
		double span = MainStageSet.STRIKE_END - MainStageSet.STRIKE;
		double rOut = strikeR * (1 - (t - MainStageSet.STRIKE) / span);
		double rIn = strikeR * (1 - (t + 1 - MainStageSet.STRIKE) / span);
		int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z);
		int ri = MathHelper.ceil(rOut);
		int liftBudget = Math.min(14, Kit.debrisBudget(world, g));
		BlockPos.Mutable p = new BlockPos.Mutable();
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d >= rOut || d < rIn) {
					continue;
				}
				int x = gx + dx, z = gz + dz;
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
				// A few surface blocks are hauled up into the rigging for the camera.
				if (liftBudget > 0 && top > floorY && world.random.nextInt(5) == 0) {
					p.set(x, top, z);
					BlockState s = world.getBlockState(p);
					if (Kit.liftable(world, p, s, ProtectedRegions.get(world))) {
						Destruction.record(world, p.toImmutable(), s);
						FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(world, p.toImmutable(), s);
						fb.setNoGravity(true);
						fb.setDestroyedOnLanding();
						fb.dropItem = false;
						Kit.velocity(fb, new Vec3d(world.random.nextGaussian() * 0.08, 0.8 + world.random.nextDouble() * 0.6, world.random.nextGaussian() * 0.08));
						flying.put(fb, t);
						liftBudget--;
					}
				}
				for (int y = Math.min(top, floorY + 48); y > floorY; y--) {
					queue.add(new Op(BlockPos.asLong(x, y, z), Blocks.AIR.getDefaultState()));
				}
				BlockState floor = floorAt(x, z, g);
				queue.add(new Op(BlockPos.asLong(x, floorY, z), floor));
				queue.add(new Op(BlockPos.asLong(x, floorY - 1, z), Blocks.GRAY_CONCRETE.getDefaultState()));
				// Shallow dips are filled level; deep holes stay holes.
				for (int y = floorY - 2; y >= floorY - 6; y--) {
					p.set(x, y, z);
					if (!world.getBlockState(p).isAir() && world.getFluidState(p).isEmpty()) {
						break;
					}
					queue.add(new Op(p.asLong(), Blocks.GRAY_CONCRETE.getDefaultState()));
				}
			}
		}
	}

	// ------------------------------------------------------------------ spotlights

	private void scorch(Vec3d g, int t) {
		for (int i = 0; i < MainStageSet.LAMPS; i++) {
			Vec3d s = MainStageSet.spot(g, fwd, i, t);
			int x = MathHelper.floor(s.x), z = MathHelper.floor(s.z);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos top = Kit.surface(world, x, z);
			Vec3d hit = Vec3d.ofCenter(top.up());
			if (t % 2 == 0) {
				world.spawnParticles(ParticleTypes.LARGE_SMOKE, hit.x, hit.y, hit.z, 3, 0.6, 0.2, 0.6, 0.02);
				world.spawnParticles(ParticleTypes.LAVA, hit.x, hit.y, hit.z, 1, 0.4, 0.1, 0.4, 0.0);
			}
			if (Destruction.enabled(world)) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx != 0 && dz != 0) {
							continue;
						}
						BlockPos q = Kit.surface(world, x + dx, z + dz);
						BlockState old = world.getBlockState(q);
						if (!old.hasBlockEntity() && old.getFluidState().isEmpty()) {
							Destruction.set(world, q, dx == 0 && dz == 0 ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.BLACKSTONE.getDefaultState(), Kit.FLAGS);
						}
					}
				}
				if (world.random.nextInt(6) == 0 && world.getBlockState(top.up()).isAir()) {
					Destruction.set(world, top.up(), Blocks.FIRE.getDefaultState(), Kit.FLAGS);
				}
			}
			// The beams don't just scorch: whatever they sweep over blows apart.
			if ((t + i * 5) % 14 == 0) {
				Kit.boom(world, hit, 2.2F, 0xFFB040, owner(), true);
			}
			if (t % 4 == 0) {
				for (LivingEntity e : Kit.living(world, hit, 3.0)) {
					e.setOnFireFor(5.0F);
					e.timeUntilRegen = 0;
					e.damage(ModDamageTypes.source(world, ModDamageTypes.PLASMA, null, owner()), 8.0F);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the main stage

	/**
	 * The stage itself, in front of the bomb facing it: a raised plank floor with a dark apron and footlights, a black
	 * proscenium arch trimmed in gold, red curtains gathered at both sides, a backdrop, and a marquee over the arch
	 * spelling MAIN STAGE in lit blocks.
	 */
	private void queueStage(Vec3d g) {
		if (!Destruction.enabled(world)) {
			return;
		}
		Vec3d front = g.add(fwd.multiply(MainStageSet.STAGE_AT - MainStageSet.STAGE_D / 2));
		int halfW = (int) (MainStageSet.STAGE_W / 2), depth = (int) MainStageSet.STAGE_D, h = (int) MainStageSet.STAGE_H, arch = (int) MainStageSet.ARCH_H;
		int top = floorY + h;
		for (int w = -halfW; w <= halfW; w++) {
			for (int d = 0; d <= depth; d++) {
				Vec3d at = front.add(right.multiply(w)).add(fwd.multiply(d));
				int x = MathHelper.floor(at.x), z = MathHelper.floor(at.z);
				for (int y = floorY + 1; y < top; y++) {
					queue.add(new Op(BlockPos.asLong(x, y, z), (d == 0 ? Blocks.DARK_OAK_PLANKS : Blocks.BLACK_CONCRETE).getDefaultState()));
				}
				BlockState floor = d == 0 && Math.floorMod(w, 3) == 0 ? Blocks.SEA_LANTERN.getDefaultState()
						: Math.floorMod(w + d, 2) == 0 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.OAK_PLANKS.getDefaultState();
				queue.add(new Op(BlockPos.asLong(x, top, z), floor));
				// Clear the air above the stage so nothing left over from the strike sits on it.
				for (int y = top + 1; y <= top + arch; y++) {
					queue.add(new Op(BlockPos.asLong(x, y, z), Blocks.AIR.getDefaultState()));
				}
			}
		}
		// Steps up to the stage at the front.
		for (int k = 0; k < h; k++) {
			for (int w = -3; w <= 3; w++) {
				Vec3d at = front.add(right.multiply(w)).subtract(fwd.multiply(k + 1));
				queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), floorY + h - k - 1, MathHelper.floor(at.z)),
						Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.getFacing(fwd.x, 0, fwd.z))));
			}
		}
		// The proscenium: two pillars and a header, black with a gold edge.
		for (int sgn = -1; sgn <= 1; sgn += 2) {
			for (int y = top + 1; y <= top + arch; y++) {
				for (int k = 0; k < 2; k++) {
					Vec3d at = front.add(right.multiply(sgn * (halfW - k))).add(fwd.multiply(1));
					queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)), (k == 1 ? Blocks.GOLD_BLOCK : Blocks.BLACK_CONCRETE).getDefaultState()));
				}
			}
		}
		for (int w = -halfW; w <= halfW; w++) {
			for (int y = top + arch - 2; y <= top + arch; y++) {
				Vec3d at = front.add(right.multiply(w)).add(fwd.multiply(1));
				queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)), (y == top + arch - 2 ? Blocks.GOLD_BLOCK : Blocks.BLACK_CONCRETE).getDefaultState()));
			}
		}
		// Red curtains gathered at the sides, a pelmet along the top, and a deep red backdrop.
		for (int y = top + 1; y < top + arch - 2; y++) {
			for (int k = 2; k < 7; k++) {
				for (int sgn = -1; sgn <= 1; sgn += 2) {
					double drape = k - 2 + (y - top) * 0.06;
					if (k - 2 > 4 - (y - top) / (double) arch * 2.5) {
						continue;
					}
					Vec3d at = front.add(right.multiply(sgn * (halfW - k))).add(fwd.multiply(2 + (int) (drape * 0.2)));
					queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)), Blocks.RED_WOOL.getDefaultState()));
				}
			}
		}
		for (int w = -halfW + 2; w <= halfW - 2; w++) {
			for (int y = top + arch - 4; y < top + arch - 2; y++) {
				Vec3d at = front.add(right.multiply(w)).add(fwd.multiply(2));
				queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)), Blocks.RED_WOOL.getDefaultState()));
			}
			for (int y = top + 1; y <= top + arch - 2; y++) {
				Vec3d at = front.add(right.multiply(w)).add(fwd.multiply(depth));
				queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)),
						(Math.floorMod(w, 4) == 0 ? Blocks.RED_TERRACOTTA : Blocks.RED_CONCRETE).getDefaultState()));
			}
		}
		// The marquee: MAIN STAGE in yellow blocks on black, ringed with bulbs, standing on the arch.
		String sign = "MAIN STAGE";
		int signW = MainStageSet.width(sign);
		int signY = top + arch + 2;
		for (int px = -2; px < signW + 2; px++) {
			for (int row = -2; row < 7; row++) {
				Vec3d at = front.add(right.multiply(signW / 2.0 - px)).add(fwd.multiply(1));
				int y = signY + 6 - row;
				boolean edge = px == -2 || px == signW + 1 || row == -2 || row == 6;
				boolean lit = row >= 0 && row < 5 && px >= 0 && px < signW && px % 4 != 3 && sign.charAt(px / 4) != ' '
						&& MainStageSet.pixel(sign.charAt(px / 4), px % 4, row);
				BlockState st = edge ? (Math.floorMod(px + row, 2) == 0 ? Blocks.GLOWSTONE : Blocks.GOLD_BLOCK).getDefaultState()
						: lit ? Blocks.YELLOW_CONCRETE.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
				queue.add(new Op(BlockPos.asLong(MathHelper.floor(at.x), y, MathHelper.floor(at.z)), st));
			}
		}
	}

	// ------------------------------------------------------------------ the painted sky

	private BlockState skyPaint(double az, int h, int x, int z) {
		double el = h / MainStageSet.WALL_H * 0.9;
		double c = MainStageSet.cloud(az, el);
		if (c > 0.55) {
			return Blocks.WHITE_CONCRETE.getDefaultState();
		}
		if (c > 0.45 && ((x + z + h) & 1) == 0) {
			// Cloud rims: shaded with a little grey-blue, like brushwork.
			return (c < 0.5 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.WHITE_CONCRETE).getDefaultState();
		}
		double dither0 = (((x * 5 + z * 11 + h * 3) % 8) + 8) % 8 / 8.0;
		if (h < 7) {
			// A warm, hazy painted horizon along the bottom of the wall.
			double warm = 1 - h / 7.0;
			if (dither0 < warm * 0.6) {
				return (h < 3 ? Blocks.ORANGE_TERRACOTTA : Blocks.WHITE_TERRACOTTA).getDefaultState();
			}
			if (dither0 < warm) {
				return Blocks.WHITE_CONCRETE.getDefaultState();
			}
		}
		double k = MathHelper.clamp((h - 18) / 34.0, 0.0, 1.0);
		double dither = (((x * 7 + z * 13 + h * 5) % 8) + 8) % 8 / 8.0;
		return dither < k ? Blocks.BLUE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
	}

	/** A wall of painted sky all the way round, with an EXIT door and a staircase up to it. */
	private void queueWall(Vec3d g) {
		if (!Destruction.enabled(world) || wallR < 24) {
			return;
		}
		int gx = MathHelper.floor(g.x), gz = MathHelper.floor(g.z);
		int top = floorY + (int) MainStageSet.WALL_H;
		int doorY = floorY + (int) MainStageSet.DOOR_H;
		double doorAz = MainStageSet.bearing(fwd);
		String sign = "EXIT";
		int signW = MainStageSet.width(sign);
		List<long[]> columns = new ArrayList<>();
		int ri = wallR + 2;
		for (int dx = -ri; dx <= ri; dx++) {
			for (int dz = -ri; dz <= ri; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < wallR - 1 || d >= wallR + 1) {
					continue;
				}
				double az = Math.atan2(dz, dx);
				columns.add(new long[]{dx, dz, Double.doubleToLongBits(az)});
			}
		}
		// Raise it in a sweep around the circle, starting behind the door.
		columns.sort((a, b) -> Double.compare(rel(Double.longBitsToDouble(a[2]), doorAz), rel(Double.longBitsToDouble(b[2]), doorAz)));
		for (long[] col : columns) {
			int x = gx + (int) col[0], z = gz + (int) col[1];
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}
			double az = Double.longBitsToDouble(col[2]);
			double arc = MathHelper.wrapDegrees(Math.toDegrees(az - doorAz)) / 180.0 * Math.PI * wallR;
			int bottom = Math.min(Kit.surface(world, x, z).getY(), floorY) - 2;
			for (int y = bottom; y <= top; y++) {
				int h = y - floorY;
				BlockState s = skyPaint(az, h, x, z);
				if (Math.abs(arc) <= 1.6 && y >= doorY && y < doorY + 4) {
					s = Blocks.AIR.getDefaultState();
				}
				int px = (int) Math.floor(arc + signW / 2.0 + 0.5);
				int row = doorY + 9 - y;
				if (px >= 0 && px < signW && row >= 0 && row < 5 && px % 4 != 3
						&& MainStageSet.pixel(sign.charAt(px / 4), px % 4, row)) {
					s = Blocks.RED_CONCRETE.getDefaultState();
				}
				queue.add(new Op(BlockPos.asLong(x, y, z), s));
			}
		}
		// White stairs climbing the inside of the wall to the door, from its left.
		Vec3d out = new Vec3d(Math.cos(doorAz), 0, Math.sin(doorAz));
		Vec3d tangent = new Vec3d(-out.z, 0, out.x);
		int steps = (int) MainStageSet.DOOR_H;
		for (int k = 0; k <= steps + 2; k++) {
			double s = k <= 2 ? k - 1.0 : -1.5 - (k - 2);
			int stepTop = k <= 2 ? doorY - 1 : doorY - 1 - (k - 2);
			for (int depth = 2; depth <= 3; depth++) {
				Vec3d at = g.add(out.multiply(wallR - depth)).add(tangent.multiply(s));
				int x = MathHelper.floor(at.x), z = MathHelper.floor(at.z);
				for (int y = Math.min(Kit.surface(world, x, z).getY(), floorY); y < stepTop; y++) {
					queue.add(new Op(BlockPos.asLong(x, y, z), Blocks.SMOOTH_QUARTZ.getDefaultState()));
				}
				BlockState tread = k <= 2 ? Blocks.QUARTZ_BLOCK.getDefaultState()
						: Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.getFacing(tangent.x, 0, tangent.z));
				queue.add(new Op(BlockPos.asLong(x, stepTop, z), tread));
			}
		}
	}

	private static double rel(double az, double from) {
		double d = (az - from + Math.PI) % (Math.PI * 2);
		return d < 0 ? d + Math.PI * 2 : d;
	}

	private void drain() {
		BlockPos.Mutable p = new BlockPos.Mutable();
		int n = 0;
		while (n < BUDGET && !queue.isEmpty()) {
			Op op = queue.poll();
			p.set(op.pos());
			n++;
			if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
				continue;
			}
			if (world.getBlockState(p).hasBlockEntity()) {
				continue;
			}
			Destruction.set(world, p.toImmutable(), op.state(), Kit.FLAGS);
		}
	}
}
