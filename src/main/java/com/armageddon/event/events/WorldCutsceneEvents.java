package com.armageddon.event.events;

import com.armageddon.destruction.Destruction;
import com.armageddon.destruction.world.TerrainJob;
import com.armageddon.destruction.world.VoxelQueue;
import com.armageddon.destruction.world.WorldScars;
import com.armageddon.detonation.CraterJob;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import com.armageddon.detonation.DetonationManager;
import com.armageddon.detonation.Detonations;
import com.armageddon.event.EventEntity;
import com.armageddon.event.Kit;
import com.armageddon.event.WorldEvent;
import com.armageddon.network.FxType;
import com.armageddon.network.Vfx;
import com.armageddon.registry.ModDamageTypes;
import com.armageddon.registry.ModSounds;
import com.armageddon.util.WorldShapes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The true WORLD DESTRUCTION tier. Each event runs a scripted camera cinematic on every player's
 * client (the scenery and camera live there) while the server keeps the real destruction on the
 * exact same timeline, so the world is already breaking when control is handed back.
 */
public final class WorldCutsceneEvents {
	private WorldCutsceneEvents() {
	}

	/** Players cannot be hurt while the camera is out of their hands. */
	static void protect(ServerWorld w, int ticks) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks, 4, false, false));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ticks, 2, false, false));
		}
	}

	static void begin(WorldEvent e, ServerWorld w, Vec3d g, String key, int scene, Vec3d heading, int cineTicks) {
		CelestialEvents.announce(w, g, key);
		Vfx.cutscene(w, g, cineTicks);
		Vfx.cinematic(w, g, scene, heading, cineTicks);
		protect(w, cineTicks + 80);
	}

	// ====================================================================== shared

	private static final int BUDGET = 2500;

	/** Steps terrain jobs in order with one shared per-tick budget. */
	static void stepJobs(List<TerrainJob> jobs, int budget) {
		for (TerrainJob j : jobs) {
			if (budget <= 0) {
				return;
			}
			if (!j.isDone()) {
				budget = j.step(budget);
			}
		}
	}

	/**
	 * The visible edge of a cataclysm's blast: chunks of the surface torn up off the passing front and thrown
	 * outward, and anything standing in it hammered and thrown.
	 */
	static void shockFront(ServerWorld w, Vec3d g, double front, int blocks, float damage, @org.jetbrains.annotations.Nullable net.minecraft.entity.Entity owner) {
		if (Destruction.enabled(w)) {
			for (int i = 0; i < blocks; i++) {
				double a = w.random.nextDouble() * Math.PI * 2;
				double d = front - w.random.nextDouble() * 8;
				int x = MathHelper.floor(g.x + Math.cos(a) * d), z = MathHelper.floor(g.z + Math.sin(a) * d);
				if (!w.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}
				net.minecraft.util.math.BlockPos p = Kit.surface(w, x, z);
				net.minecraft.block.BlockState st = w.getBlockState(p);
				if (!Kit.liftable(w, p, st, com.armageddon.destruction.ProtectedRegions.get(w))) {
					continue;
				}
				Destruction.record(w, p, st);
				net.minecraft.entity.FallingBlockEntity fb = net.minecraft.entity.FallingBlockEntity.spawnFromBlock(w, p, st);
				fb.setDestroyedOnLanding();
				Kit.velocity(fb, new Vec3d(Math.cos(a) * (0.6 + w.random.nextDouble()), 0.9 + w.random.nextDouble() * 1.2, Math.sin(a) * (0.6 + w.random.nextDouble())));
			}
		}
		for (LivingEntity e : Kit.living(w, g, front + 12)) {
			double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
			if (Math.abs(d - front) < 12 && !Kit.creativeOrSpectator(e)) {
				Vec3d out = new Vec3d(e.getX() - g.x, 0, e.getZ() - g.z).normalize();
				e.damage(ModDamageTypes.source(w, ModDamageTypes.ANNIHILATION, null, owner), damage);
				Kit.push(e, out.multiply(2.2).add(0, 1.0, 0));
			}
		}
	}

	/** Steps a devastation sweep: the front moves at {@code speed} blocks a tick from {@code start}. */
	static void sweep(TerrainJob job, int k, double speed, int budget) {
		if (job != null) {
			job.limit = k * speed;
			job.step(budget);
		}
	}

	static boolean allDone(List<TerrainJob> jobs) {
		for (TerrainJob j : jobs) {
			if (!j.isDone()) {
				return false;
			}
		}
		return true;
	}

	static int groundY(ServerWorld w, Vec3d g) {
		return Kit.surface(w, MathHelper.floor(g.x), MathHelper.floor(g.z)).getY();
	}

	// ====================================================================== GALACTIC SMASH

	/**
	 * A cosmic giant tears the sky open, looks around, winds up, and punches the ground so hard the planet
	 * rings. What it leaves: its fist pressed into the land (four knuckle bowls of crying obsidian and
	 * amethyst, the folded fingers, the thumb), cracks of glowing cosmic glass, and the ground frozen in
	 * concentric bell ripples. Palette: violet, magenta, gold. Sound: a struck bell that rings out.
	 */
	public static class GalacticSmash extends WorldEvent {
		/** STARFALL: the spent giant breaks up into its stars and they come down; HEART: its core falls last. */
		private static final int CINE = 760, SMASH = 460, STARFALL = 690, HEART = STARFALL + 150;
		private Vec3d fwd;
		private double R;
		private final List<TerrainJob> jobs = new ArrayList<>();
		private TerrainJob fist, cracks, ripples, devastation;
		private double blast;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				begin(this, world, g, "block.armageddon.galactic_smash", 1, fwd, CINE);
				Vfx.skyCrack(world, g, 1.0F, 0xA070FF, 2, 220);
				Vfx.skyTint(world, g, 100000, 0x1A1040, 0.8F);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.25F);
			}
			if (t > 20 && t < SMASH && t % 20 == 0) {
				Vfx.shake(world, g, 4096, 0.2F + 1.4F * t / SMASH);
			}
			if (t >= 150 && t < 330 && t % 25 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 0.35F);
			}
			if (t == 300) {
				frames(g, 0);
				Vfx.fov(world, g, 8.0F, 10);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.3F);
			}
			if (t == 330) {
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.3F);
				Vfx.spiral(world, g.add(fwd.multiply(230)).add(0, 340, 0), 150.0F, 120.0F, 0xFFB070, 130);
			}
			if (t == 420 || t == 440) {
				frames(g, t == 420 ? 1 : 2, 0);
				Vfx.fov(world, g, 10.0F, 12);
			}
			if (t == SMASH - 8) {
				frames(g, 3, 1, 0, 2);
			}
			if (t == SMASH) {
				smash(g);
			}
			if (t > SMASH) {
				int k = t - SMASH;
				// The fist sinks in first, the cracks race out, and the ground rings outward like a bell.
				fist.limit = 10.0 * k;
				cracks.limit = R * 0.2 + 5.0 * k;
				ripples.limit = R * 1.05 + 3.0 * k;
				stepJobs(jobs, BUDGET);
				// The real blast: everything standing for three hundred blocks is torn down as the front passes.
				sweep(devastation, k, 4.5, 3500);
				if (devastation != null && k * 4.5 < blast) {
					shockFront(world, g, k * 4.5, 10, 30.0F, owner());
				}
				double front = R * 0.8 + 3.0 * k;
				if (k < 300 && k % 20 == 0) {
					// Each bell stroke throws things standing on the passing wave.
					for (LivingEntity e : Kit.living(world, g, front + 30)) {
						double d = Math.hypot(e.getX() - g.x, e.getZ() - g.z);
						if (Math.abs(d - front) < 18 && e.isOnGround()) {
							e.damage(ModDamageTypes.source(world, ModDamageTypes.SHRAPNEL, null, owner()), 8.0F);
							Kit.push(e, new Vec3d(0, 0.8, 0));
						}
					}
					float ring = 1.0F - k / 300.0F;
					Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.5F + 0.15F * (k / 60));
					Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 48.0F * ring + 8, 0.5F);
					Vfx.ring(world, g, (float) front, 3.0F, 0xC080FF, true, 30);
					Vfx.shake(world, g, 100000, 0.4F + 1.0F * ring);
				}
			}
			if (t > SMASH + 10 && t < SMASH + 310 && t % 4 == 0) {
				// The ejecta comes back down: burning chunks of the city land all over the map and go off.
				double ang = world.random.nextDouble() * Math.PI * 2;
				double d = R * (0.6 + world.random.nextDouble() * 1.4);
				Vec3d hit = Kit.ground(world, g.x + Math.cos(ang) * d, g.z + Math.sin(ang) * d);
				Vfx.streak(world, hit.add(world.random.nextGaussian() * 40, 160, world.random.nextGaussian() * 40), hit, 2.0F, 0xFF7030, 8);
				DetonationManager.schedule(world, 8, () -> {
					Kit.boom(world, hit.add(0, 0.5, 0), 3.0F + world.random.nextFloat() * 3.0F, 0xFF7030, owner(), true);
					if (Destruction.enabled(world) && world.random.nextInt(3) == 0) {
						Detonations.ignite(world, hit, 6, 0.3F);
					}
				});
			}
			if (t == SMASH + 60) {
				Vfx.skyTint(world, g, 100000, 0x2A1010, 0.8F);
			}
			if (t == STARFALL) {
				// The giant comes apart into the stars it was made of, and they fall.
				Vfx.skyTint(world, g, 100000, 0x100830, 0.9F);
				Vfx.skyCrack(world, g, 0.7F, 0xC0A0FF, 2, HEART - STARFALL + 40);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 64.0F, 0.4F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.4F);
			}
			if (t > STARFALL && t < HEART && t % 3 == 0) {
				// Each star is a collapsed thing: where it lands the ground is pulled into it and crushed, not blown out.
				Vec3d hit = com.armageddon.event.Climax.around(world, g, R * 0.6, Math.max(R * 1.5, blast * 0.95));
				double size = 4.0 + world.random.nextDouble() * 4.0;
				com.armageddon.event.Climax.fall(world, hit, 300, world.random.nextBoolean() ? 0xC8B0FF : 0x80A0FF, 2.4F, 10, () -> {
					Vfx.ring(world, hit.add(0, 0.5, 0), (float) size * 3, 1.5F, 0xB090FF, false, 30);
					Vfx.light(world, hit.add(0, 2, 0), 0.5F, 0xB090FF);
					com.armageddon.event.Ruin.implode(world, hit.add(0, size * 0.3, 0), size, 26);
				});
			}
			if (t == HEART) {
				// Last of all its heart, white-hot, straight down onto ground zero.
				Vfx.flash(world, g.add(0, 300, 0), 0.6F, 100000, 0xFFFFFF);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.3F);
				com.armageddon.event.Climax.strike(world, g, 500, 0xFFFFFF, 24.0F, (float) Math.min(46, R * 0.5), CraterJob.Mode.ANTIMATTER, 20, owner());
				DetonationManager.schedule(world, 20, () -> {
					CityDestructionEvents.frames(world, g, 3, 1, 3);
					Vfx.pillar(world, g, 20.0F, 0xD0C0FF, false, 120);
					Vfx.ring(world, g.add(0, 1, 0), (float) blast, 8.0F, 0xC0A0FF, true, 80);
					Vfx.shake(world, g, 100000, 3.0F);
					Vfx.fov(world, g, 22.0F, 40);
					Detonations.blast(world, g, R * 2, 3000.0F, ModDamageTypes.ANNIHILATION, owner(), R, 8.0);
				});
			}
			return t > HEART + 60 && (jobs.isEmpty() || allDone(jobs)) && (devastation == null || devastation.isDone()) || t > SMASH + 1800;
		}

		private void smash(Vec3d g) {
			frames(g, 3, 1, 0, 2, 3);
			Vfx.fov(world, g, 24.0F, 40);
			Vfx.shake(world, g, 100000, 3.0F);
			Vfx.layeredBlast(world, g, 80.0F, 0xFF7030);
			Vfx.layeredBlast(world, g.add(0, 30, 0), 55.0F, 0xC080FF);
			Vfx.dome(world, g, 320.0F, 0xFFB070, 120);
			Vfx.ring(world, g, 600.0F, 8.0F, 0xFF8040, true, 140);
			Vfx.ring(world, g, 300.0F, 6.0F, 0xC080FF, true, 120);
			Vfx.ring(world, g, 180.0F, 4.0F, 0xFFD080, true, 90);
			Vfx.pillar(world, g, 22.0F, 0xFFB070, false, 160);
			Vfx.flash(world, g, 1.0F, 100000, 0xFFE0C0);
			Vfx.screen(world, g, 100000, 1, 0.7F, 10);
			Vfx.light(world, g.add(0, 30, 0), 1.5F, 0xFF9050);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.4F);
			Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.4F);
			Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 64.0F, 0.5F);
			Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.3F);
			Detonations.blast(world, g, 420, 20000.0F, ModDamageTypes.ANNIHILATION, owner(), 200, 14.0);
			R = Destruction.radius(world, 140.0F);
			int base = groundY(world, g);
			fist = WorldScars.fist(world, g, fwd, R, base);
			cracks = WorldScars.cracks(world, g, fwd, R);
			ripples = WorldScars.ripples(world, g, R);
			if (Destruction.enabled(world)) {
				jobs.add(fist);
				jobs.add(cracks);
				jobs.add(ripples);
				blast = Destruction.radius(world, 320.0F);
				devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, blast, base, com.armageddon.destruction.world.Devastation.VOID);
			}
			Detonations.debris(world, g, 90, 260);
			if (Destruction.enabled(world)) {
				Detonations.ignite(world, g, (int) Math.min(80, R * 0.6), 0.08F);
			}
			Vfx.custom(world, FxType.WORLD_FX, g, fwd, (float) R, 0xC080FF, 1, base, 0, 1500, Vfx.FAR);
		}

		private void frames(Vec3d g, int... styles) {
			CityDestructionEvents.frames(world, g, styles);
		}
	}

	// ====================================================================== BOOMING TICK

	/**
	 * A clock appears in the sky over the bomb and starts to tick. Every five seconds its hand moves on one
	 * numeral, and that numeral breaks off the face and falls, burning, onto the bomb: each boom bigger than
	 * the last, for a full minute. Every boom levels one more ring of land into a terrace, each a step lower
	 * towards the centre and coloured from blood red to pale: by midnight the land is a stepped bullseye of
	 * scorched terracotta with twelve glowing clock-hand trenches and the tick's hole at the hub.
	 * Sound: ticks, bass drums, twelve chimes.
	 */
	public static class BoomingTick extends WorldEvent {
		private static final int BOOMS = 12, GAP = 100, LEAD = 60, T0 = LEAD + BOOMS * GAP, CINE = 400, FINAL = T0 + CINE - 4;
		/** After the twelfth chime the clock stops, and with it everything; when it starts again, every bomb at once. */
		private static final int STOP = FINAL + 12 * 16 + 12, GO = STOP + 70;
		private Vec3d clock;
		private Vec3d fwd;
		private double R;
		private int base;
		private final List<TerrainJob> jobs = new ArrayList<>();
		private TerrainJob front, devastation;
		private int devastStart;
		private boolean handsPending;
		private int frontStart;
		private double frontFrom;

		private void addBand(Vec3d g, int i, int t) {
			if (Destruction.enabled(world)) {
				front = WorldScars.band(world, g, R, base, i);
				front.limit = 0;
				frontStart = t;
				frontFrom = i == 0 ? 0 : WorldShapes.bandRadius(i - 1, R);
				jobs.add(front);
			}
			double inner = i == 0 ? 0 : WorldShapes.bandRadius(i - 1, R);
			Vfx.custom(world, FxType.WORLD_FX, g, g, (float) WorldShapes.bandRadius(i, R), WorldShapes.bandColor(i), 3, (float) inner,
					base - WorldShapes.bandDepth(i), 70, Vfx.FAR);
		}

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				R = Destruction.radius(world, 130.0F);
				base = groundY(world, g);
				CelestialEvents.announce(world, g, "block.armageddon.booming_tick");
				clock = g.add(0, WorldShapes.CLOCK_H, 0);
				Vfx.custom(world, FxType.WORLD_FX, clock, fwd, (float) WorldShapes.CLOCK_R, 0xFFC870, 6, GAP, LEAD, T0 + 20, Vfx.FAR);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
			}
			int beat = t - LEAD;
			if (beat >= -WorldShapes.CLOCK_DROP && beat < BOOMS * GAP - WorldShapes.CLOCK_DROP && Math.floorMod(beat + WorldShapes.CLOCK_DROP, GAP) == 0) {
				// The hand reaches the next numeral and it breaks off the face, falling burning onto the bomb.
				int n = (beat + WorldShapes.CLOCK_DROP) / GAP + 1;
				Vec3d from = WorldShapes.clockNumeral(clock, fwd, n);
				Vfx.streak(world, from, g.add(0, 1, 0), 3.0F + n * 0.4F, WorldShapes.bandColor(n - 1), WorldShapes.CLOCK_DROP);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 64.0F, 1.2F - n * 0.05F);
				Kit.sound(world, from, SoundEvents.ENTITY_BLAZE_SHOOT, 64.0F, 0.5F);
			}
			if (t < T0 && t % 20 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 8.0F + t * 0.04F, 0.6F + t * 0.0012F);
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 6.0F + t * 0.02F, 1.6F);
				// A visible heartbeat that grows with every tick.
				Vfx.ring(world, g.add(0, 0.3, 0), 2.0F + t * 0.03F, 0.4F, 0xFF3030, false, 12);
				Vfx.orb(world, g.add(0, 1.2, 0), 0.4F + t * 0.0015F, 0.2F + t * 0.001F, 0xFF3030, 0, 12);
			}
			if (beat >= 0 && beat < BOOMS * GAP && beat % GAP == 0) {
				int i = beat / GAP;
				double r = WorldShapes.bandRadius(i, R);
				int color = WorldShapes.bandColor(i);
				// Big from the first one, and every boom bigger than the last.
				Kit.boom(world, g.add(0, 1, 0), (float) Math.min(30.0, 3.0 + r * 0.22), color, owner(), true);
				Vfx.layeredBlast(world, g, (float) (r * 0.3 + 2.0), color);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), (float) Math.min(64, 8.0 + i * 5.5), (float) Math.max(0.35, 1.2 - i * 0.07));
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), (float) Math.min(64, 8.0 + i * 5), 0.5F);
				Vfx.shake(world, g, 256 + r * 10, (float) Math.min(3.0, 0.8 + i * 0.2));
				Vfx.fov(world, g, (float) Math.min(14.0, 3 + i * 1.2), 14);
				Detonations.blast(world, g, r * 1.4 + 8, (float) (40 + i * i * 10), ModDamageTypes.NUCLEAR_BLAST, owner(), r * 0.6, 1.5 + i * 0.3);
				Detonations.debris(world, g, Math.max(6, r * 0.6), (int) Math.min(120, 10 + r));
				Vfx.ring(world, g, (float) r + 4, 2.0F, color, true, 40);
				addBand(g, i, t);
			}
			if (t == T0) {
				Vfx.cutscene(world, g, CINE);
				Vfx.cinematic(world, g, 2, fwd, CINE);
				protect(world, CINE + 80);
			}
			if (t >= T0 && t < T0 + 330) {
				// The cutscene's carpet-bombing: real bombs, walking outward from the terraces over the whole countryside.
				int k = t - T0;
				for (int i = 0; i < 1 + k / 60; i++) {
					double a = world.random.nextDouble() * Math.PI * 2;
					double d = R * 0.9 + world.random.nextDouble() * (40 + k * 0.6);
					Vec3d p = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					Vfx.layeredBlast(world, p, 4.0F + world.random.nextInt(6), WorldShapes.bandColor(world.random.nextInt(BOOMS)));
					if (k % 2 == 0) {
						Kit.boom(world, p.add(0, 0.5, 0), 3.5F + world.random.nextFloat() * 3.5F, 0xFF6020, owner(), true);
					}
				}
				if (k % 6 == 0) {
					Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 40.0F, 0.5F);
				}
			}
			if (t == FINAL) {
				// Midnight: the last terrace, the twelve hands, the hole at the hub.
				CityDestructionEvents.frames(world, g, 3, 1, 0, 2);
				Vfx.fov(world, g, 22.0F, 50);
				Vfx.shake(world, g, 100000, 3.0F);
				Vfx.layeredBlast(world, g, 70.0F, 0xFFB060);
				Vfx.pillar(world, g, 10.0F, 0xFF4020, false, 200);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
				Detonations.blast(world, g, 500, 20000.0F, ModDamageTypes.ANNIHILATION, owner(), 250, 14.0);
				addBand(g, BOOMS, t);
				handsPending = Destruction.enabled(world);
				if (Destruction.enabled(world)) {
					// Midnight's shock front: everything left standing beyond the terraces is torn down.
					devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, Destruction.radius(world, 330.0F), base,
							com.armageddon.destruction.world.Devastation.FIRE);
					devastation.limit = 0;
					devastStart = t;
				}
				Detonations.debris(world, g, 80, 150);
				Vfx.custom(world, FxType.WORLD_FX, g, fwd, (float) R, 0xFFB060, 2, base, 0, 1400, Vfx.FAR);
			}
			if (t > FINAL && t < FINAL + 12 * 16 && (t - FINAL) % 16 == 0) {
				// Twelve chimes, each lighting the next terrace ring outward.
				int k = (t - FINAL) / 16;
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_USE, 64.0F, 0.6F - k * 0.015F);
				Vfx.ring(world, g, (float) WorldShapes.bandRadius(k + 1, R), 2.5F, WorldShapes.bandColor(k + 1), true, 40);
			}
			if (front != null) {
				front.limit = frontFrom + 3.0 * (t - frontStart) + 1;
			}
			if (handsPending && front != null && front.isDone()) {
				// Hands and hub go in after the last terrace, or its floor would fill their trenches back in.
				handsPending = false;
				jobs.add(WorldScars.spokes(world, g, fwd, R, base));
				jobs.add(WorldScars.hub(world, g, base));
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 64.0F, 0.5F);
			}
			stepJobs(jobs, BUDGET);
			if (devastation != null) {
				int k = t - devastStart;
				// Only beyond the terraces (they are levelled already): start the front at their rim.
				devastation.limit = R + k * 4.0;
				devastation.step(3500);
				if (devastation.limit < 330 && k % 2 == 0) {
					shockFront(world, g, devastation.limit, 8, 30.0F, owner());
				}
			}
			if (t >= STOP && t < GO) {
				int k = t - STOP;
				if (k == 0) {
					// Time stops: the world goes grey and silent, nothing can move.
					Vfx.skyTint(world, g, 100000, 0x505058, 1.0F);
					Vfx.screen(world, g, 100000, 0, 0.95F, GO - STOP);
					Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.3F);
				}
				if (k % 4 == 0) {
					com.armageddon.event.Climax.freeze(world, g, 600, 10);
				}
				if (k % 20 == 10) {
					// The one thing still moving: a tick, slower each time.
					Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 64.0F, 0.6F - k * 0.004F);
				}
			}
			if (t == GO) {
				// ...and it all goes off at once: every street, every field, the whole map in one beat.
				CityDestructionEvents.frames(world, g, 3, 2, 3, 0);
				Vfx.flash(world, g, 1.0F, 100000, 0xFFE0B0);
				Vfx.shake(world, g, 100000, 3.0F);
				Vfx.skyTint(world, g, 100000, 0x401808, 0.95F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.25F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.4F);
				double reach = Destruction.radius(world, 330.0F);
				for (int i = 0; i < 70; i++) {
					Vec3d p = com.armageddon.event.Climax.around(world, g, R * 0.5, reach);
					float size = 4.0F + world.random.nextFloat() * 5.0F;
					int color = WorldShapes.bandColor(world.random.nextInt(BOOMS));
					DetonationManager.schedule(world, world.random.nextInt(8), () -> {
						Kit.boom(world, p.add(0, 0.5, 0), size, color, owner(), true);
						if (Destruction.enabled(world)) {
							DetonationManager.add(new CraterJob(world, net.minecraft.util.math.BlockPos.ofFloored(p), size + 2, CraterJob.Mode.NUKE));
						}
					});
				}
			}
			return t > GO + 60 && !handsPending && allDone(jobs) && (devastation == null || devastation.isDone()) || t > FINAL + 1600;
		}
	}

	// ====================================================================== ALIEN DEATH RAY

	/**
	 * A cutscene to an alien world where a colossal ray cannon rises out of the planet, charges and fires at Earth.
	 * Back home the sky splits around a thin targeting beam; an EMP burns out every circuit, a tsunami leaves a
	 * bleached salt flat, then the ray itself comes down: a crater, a glass-walled shaft punched through the world,
	 * and the alien world spreading out of it (end stone, purpur roads, chorus and glowing green crystal spires).
	 * Palette: green, violet, bone white. Sound: machinery, a rising charge whine, the ray's roar, the sea.
	 */
	public static class AlienDeathRay extends WorldEvent {
		private static final int CINE = 480, ARRIVE = 350;
		/** After the ray, the harvest: tractor beams come down around the town and lift what is left of it away. */
		private static final int HARVEST = 200, HARVEST_END = HARVEST + 190, BEAMS = 6;
		private final List<Vec3d> beamAt = new ArrayList<>();
		private final List<com.armageddon.event.Climax.Vortex> lifts = new ArrayList<>();
		private Vec3d fwd;
		private double reach;
		private int base;
		private final List<TerrainJob> jobs = new ArrayList<>();
		private TerrainJob terraform, devastation;
		private VoxelQueue spires;
		private int finalAt = -1;

		@Override
		public boolean tick() {
			int t = age();
			Vec3d g = center();
			if (t == 0) {
				fwd = Kit.dir(data.getFloat("Yaw"));
				reach = Destruction.radius(world, 120.0F);
				begin(this, world, g, "block.armageddon.alien_death_ray", 3, fwd, CINE);
				Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.4F);
			}
			cutsceneSound(g, t);
			if (t == CINE - 14) {
				CityDestructionEvents.frames(world, g, 3, 1, 0, 2, 3);
			}
			if (t == CINE - 6) {
				// Home: the sky splits around a thin targeting beam that will widen into the ray.
				Vfx.skyCrack(world, g, 1.0F, 0x60FFA0, 0, ARRIVE + 40);
				Vfx.beam(world, g.add(0, 1, 0), g.add(0, 1400, 0), 1.6F, 0x80FFC0, ARRIVE - 40);
				Vfx.skyTint(world, g, 100000, 0x20083A, 0.8F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.4F);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.3F);
			}
			int k = t - CINE;
			if (k == 0) {
				base = groundY(world, g);
			}
			if (k == 20) {
				// EMP: every circuit on the surface burns out; blue static crawls over the ground.
				Vfx.ring(world, g, 2200.0F, 20.0F, 0x60A0FF, false, 90);
				Vfx.screen(world, g, 100000, 3, 0.4F, 30);
				Vfx.screen(world, g, 100000, 0, 0.5F, 80);
				Vfx.flash(world, g, 0.3F, 4000, 0x80C0FF);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 2.0F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.5F);
				for (ServerPlayerEntity p : world.getPlayers()) {
					for (int i = 0; i < p.getInventory().size(); i++) {
						net.minecraft.item.ItemStack s = p.getInventory().getStack(i);
						if (s.getItem() instanceof com.armageddon.item.GunItem) {
							p.getItemCooldownManager().set(s.getItem(), 1200);
						}
					}
				}
				EventEntity.start(world, g, "emp", owner(), nbt -> nbt.putInt("R", 64));
				if (Destruction.enabled(world)) {
					jobs.add(WorldScars.emp(world, g, Destruction.radius(world, 96.0F)));
				}
				Vfx.custom(world, FxType.WORLD_FX, g, g, (float) Destruction.radius(world, 96.0F), 0x60A0FF, 4, 0, 0, 140, Vfx.FAR);
			}
			if (k == 150) {
				EventEntity.start(world, g, "tsunami", owner(), nbt -> nbt.putFloat("Scale", 2.6F));
				Vfx.skyTint(world, g, 100000, 0x103060, 0.8F);
			}
			if (k == 230 && Destruction.enabled(world)) {
				// The water drains away and leaves the land bleached: a salt flat around where the alien land will be.
				TerrainJob salt = WorldScars.saltFlat(world, g, reach * 0.8, Math.min(Destruction.radius(world, 256.0F), reach * 1.7));
				salt.limit = reach * 0.8;
				jobs.add(salt);
			}
			if (k > 230) {
				for (TerrainJob j : jobs) {
					if (j != terraform && k < ARRIVE) {
						j.limit = Math.max(j.limit, reach * 0.8 + 4.0 * (k - 230));
					}
				}
			}
			if (k >= 20 && k < ARRIVE && k % 20 == 0) {
				Vfx.shake(world, g, 100000, 0.5F + 1.0F * k / ARRIVE);
				Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
			}
			if (k > 0 && k < ARRIVE - 40 && k % 30 == 0) {
				Kit.sound(world, g, ModSounds.DEATH_RAY, 48.0F, 0.3F + k * 0.001F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_AMBIENT, 64.0F, 0.5F);
			}
			if (k == ARRIVE - 40) {
				// The ray itself: the thin beam swells into a column of green fire as wide as a building.
				Vfx.beam(world, g.add(0, 1, 0), g.add(0, 1400, 0), 30.0F, 0xA0FFD0, 180);
				Vfx.pillar(world, g, 24.0F, 0x60FFA0, false, 180);
				Vfx.flash(world, g, 0.7F, 100000, 0xC0FFE0);
				Vfx.shake(world, g, 100000, 2.0F);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.4F);
				Kit.sound(world, g, ModSounds.ORBITAL_STRIKE, 64.0F, 0.4F);
			}
			if (k > ARRIVE - 40 && k < ARRIVE) {
				Kit.damage(world, g, 30, 200.0F, ModDamageTypes.ANNIHILATION, null, owner());
				if (k % 5 == 0) {
					Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.4F + (k - ARRIVE + 40) * 0.01F);
				}
			}
			if (k == ARRIVE - 16) {
				CityDestructionEvents.frames(world, g, 1, 0, 2);
				Vfx.fov(world, g, 14.0F, 20);
			}
			if (k == ARRIVE) {
				impact(g);
				finalAt = t;
			}
			if (finalAt >= 0) {
				int f = t - finalAt;
				if (devastation != null) {
					// The ray's blast wave: the whole countryside is levelled ahead of the alien land.
					devastation.limit = 6.0 * f;
					devastation.step(3500);
					if (devastation.limit < 330 && f % 2 == 0) {
						shockFront(world, g, devastation.limit, 10, 40.0F, owner());
					}
				}
				if (terraform != null) {
					// The alien land spreads out of the shaft like a stain.
					terraform.limit = 30 + 2.2 * f;
				}
				if (f > 0 && f < 240 && f % 20 == 0) {
					Vfx.ring(world, g, (float) Math.min(reach, 30 + 2.2 * f), 3.0F, 0x60FFA0, true, 30);
					Kit.sound(world, g, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, 48.0F, 0.5F);
					Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 48.0F, 0.6F);
				}
				harvest(g, f);
				if (spires == null && terraform != null && terraform.isDone()) {
					spires = WorldScars.spires(world, g, reach);
					Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 64.0F, 0.5F);
				}
			}
			int budget = BUDGET;
			if (spires != null && !spires.isDone()) {
				// Spires grow a slice at a time so they visibly rise.
				budget = BUDGET - (BUDGET / 2 - spires.step(BUDGET / 2));
			}
			stepJobs(jobs, budget);
			boolean built = allDone(jobs) && (terraform == null || spires != null && spires.isDone());
			return finalAt >= 0 && (t > finalAt + HARVEST_END + 20 && built && (devastation == null || devastation.isDone()) || t > finalAt + 1700);
		}

		/**
		 * The harvest: six tractor beams drop out of the dark around the ruins. Each one pulls up the ground and the
		 * buildings under it, spinning, into the sky, and lifts anyone caught in it; then the beams cut out together
		 * and leave a glassed shaft where each one stood.
		 */
		private void harvest(Vec3d g, int f) {
			if (f == HARVEST) {
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.6F);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.8F);
				for (int i = 0; i < BEAMS; i++) {
					double a = i * Math.PI * 2 / BEAMS + world.random.nextDouble() * 0.5;
					double d = reach * (0.35 + 0.45 * world.random.nextDouble());
					Vec3d p = Kit.ground(world, g.x + Math.cos(a) * d, g.z + Math.sin(a) * d);
					beamAt.add(p);
					lifts.add(new com.armageddon.event.Climax.Vortex(p, 0.45, 0.18, 0.55, 140));
					Vfx.beam(world, p, p.add(0, 600, 0), 7.0F, 0x80FFB0, HARVEST_END - HARVEST);
					Vfx.pillar(world, p, 12.0F, 0x60FFA0, false, HARVEST_END - HARVEST);
					Vfx.ring(world, p.add(0, 0.4, 0), 16.0F, 1.5F, 0x80FFB0, false, HARVEST_END - HARVEST);
				}
			}
			if (f > HARVEST && f < HARVEST_END) {
				for (int i = 0; i < beamAt.size(); i++) {
					Vec3d p = beamAt.get(i);
					double r = Math.min(16, 6 + (f - HARVEST) * 0.08);
					if (f % 3 == i % 3) {
						lifts.get(i).feed(Kit.lift(world, p, r, 5, true, true));
					}
					if (f % 10 == 0) {
						for (LivingEntity e : Kit.living(world, p, r + 2)) {
							if (!Kit.creativeOrSpectator(e)) {
								e.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.LEVITATION, 16, 3, false, false));
							}
						}
					}
					if (f % 30 == i * 5) {
						Kit.sound(world, p, SoundEvents.BLOCK_BEACON_AMBIENT, 48.0F, 0.6F);
					}
				}
			}
			for (com.armageddon.event.Climax.Vortex v : lifts) {
				v.tick();
			}
			if (f == HARVEST_END) {
				CityDestructionEvents.frames(world, g, 1, 0);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_DEACTIVATE, 64.0F, 0.4F);
				for (int i = 0; i < beamAt.size(); i++) {
					Vec3d p = beamAt.get(i);
					lifts.get(i).release();
					// Whatever the beam was holding is taken apart where it stood: a clean, glowing-edged shaft into the ground.
					Vfx.flash(world, p, 0.25F, 300, 0x80FFB0);
					Kit.damage(world, p, 12, 40.0F, ModDamageTypes.ANNIHILATION, null, owner());
					com.armageddon.event.Ruin.disintegrate(world, p.add(0, -2, 0), 10, Blocks.VERDANT_FROGLIGHT.getDefaultState(), ParticleTypes.HAPPY_VILLAGER, 30);
				}
				lifts.clear();
			}
		}

		@Override
		public void finish() {
			for (com.armageddon.event.Climax.Vortex v : lifts) {
				v.release();
			}
			super.finish();
		}

		/** The cinematic's sound track, on the same ticks as Cinematics.AlienLaunch. */
		private void cutsceneSound(Vec3d g, int t) {
			int reveal = 80, aim = 130, charge = 150, fire = 262, hit = 340;
			if (t == reveal) {
				// The silo opens and the cannon rises out of the planet.
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_EMERGE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_IRON_DOOR_OPEN, 64.0F, 0.4F);
			}
			if (t > reveal && t < aim && t % 8 == 0) {
				Kit.sound(world, g, SoundEvents.BLOCK_PISTON_EXTEND, 48.0F, 0.4F + (t - reveal) * 0.004F);
			}
			if (t == aim - 2) {
				Kit.sound(world, g, SoundEvents.BLOCK_ANVIL_LAND, 64.0F, 0.3F);
			}
			if (t >= aim && t < charge + 20 && t % 6 == 0) {
				// Slewing onto Earth: grinding gears.
				Kit.sound(world, g, SoundEvents.BLOCK_GRINDSTONE_USE, 48.0F, 0.4F);
			}
			if (t == charge) {
				Kit.sound(world, g, ModSounds.RAILGUN_CHARGE, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_ACTIVATE, 64.0F, 0.5F);
			}
			if (t > charge && t < fire && t % 8 == 0) {
				// The whine climbs as the charge builds.
				float u = (t - charge) / (float) (fire - charge);
				Kit.sound(world, g, SoundEvents.BLOCK_BEACON_POWER_SELECT, 48.0F, 0.5F + u * 1.5F);
				Kit.sound(world, g, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 32.0F, 0.5F + u);
			}
			if (t == 168 || t == 186 || t == 204 || t == 222 || t == 240 || t == 252) {
				// Every cut lands on a hit.
				Kit.sound(world, g, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 64.0F, 0.6F + (t - charge) * 0.004F);
			}
			if (t == 172 || t == 178 || t == 184) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 1.0F);
			}
			if (t == 200) {
				Kit.sound(world, g, ModSounds.ORBITAL_CHARGE, 64.0F, 0.6F);
			}
			if (t == 236) {
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 64.0F, 0.5F);
			}
			if (t >= 252 && t < fire && t % 2 == 0) {
				Kit.sound(world, g, ModSounds.BOMB_BEEP, 64.0F, 1.6F);
			}
			if (t == fire) {
				CityDestructionEvents.frames(world, g, 0, 1, 0);
				Vfx.shake(world, g, 100000, 1.8F);
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.5F);
				Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.6F);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.7F);
				Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.5F);
			}
			if (t > fire && t < hit && t % 10 == 0) {
				Kit.sound(world, g, ModSounds.DEATH_RAY, 64.0F, 0.45F);
				Kit.sound(world, g, ModSounds.BLACK_HOLE, 48.0F, 0.6F);
			}
			if (t == hit) {
				CityDestructionEvents.frames(world, g, 3, 1);
				Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 64.0F, 0.3F);
				Kit.sound(world, g, SoundEvents.BLOCK_BELL_RESONATE, 64.0F, 0.4F);
			}
			if (t > hit && t < CINE - 20 && t % 20 == 0) {
				Kit.sound(world, g, ModSounds.NUKE_RUMBLE, 64.0F, 0.3F);
			}
		}

		private void impact(Vec3d g) {
			CityDestructionEvents.frames(world, g, 3, 1, 0, 2);
			Vfx.fov(world, g, 24.0F, 50);
			Vfx.shake(world, g, 100000, 3.5F);
			Vfx.layeredBlast(world, g, 120.0F, 0x60FFA0);
			Vfx.layeredBlast(world, g.add(0, 30, 0), 90.0F, 0xA070FF);
			Vfx.layeredBlast(world, g.add(0, 80, 0), 60.0F, 0xFFFFFF);
			Vfx.custom(world, FxType.NUKE, g, g, 90.0F, 0x80FFC0, 0, 0, 0, 0, Vfx.FAR);
			Vfx.dome(world, g, 450.0F, 0x60FFA0, 140);
			Vfx.ring(world, g, 1600.0F, 12.0F, 0xA070FF, true, 180);
			Vfx.ring(world, g, 700.0F, 8.0F, 0x60FFA0, true, 120);
			Vfx.pillar(world, g, 30.0F, 0x60FFA0, false, 260);
			Vfx.flash(world, g, 1.0F, 100000, 0xE0FFF0);
			Vfx.screen(world, g, 100000, 1, 0.8F, 14);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.25F);
			Kit.sound(world, g, ModSounds.NUKE_DETONATE, 64.0F, 0.45F);
			Kit.sound(world, g, ModSounds.ANTIMATTER, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 64.0F, 0.3F);
			Kit.sound(world, g, SoundEvents.BLOCK_END_PORTAL_SPAWN, 64.0F, 0.6F);
			Detonations.blast(world, g, 900, 100000.0F, ModDamageTypes.ANNIHILATION, owner(), 420, 24.0);
			if (Destruction.enabled(world)) {
				// A real crater (it carves in a few ticks), so the shaft and the alien land are cut into a wound, not flat ground.
				DetonationManager.add(new com.armageddon.detonation.CraterJob(world, net.minecraft.util.math.BlockPos.ofFloored(g),
						Destruction.radius(world, 60.0F), com.armageddon.detonation.CraterJob.Mode.NUKE));
				jobs.add(0, WorldScars.impactShaft(world, g, reach, base));
				terraform = WorldScars.terraform(world, g, reach);
				terraform.limit = 30;
				jobs.add(terraform);
				devastation = com.armageddon.destruction.world.Devastation.of(world, g, 0, Destruction.radius(world, 330.0F), base,
						com.armageddon.destruction.world.Devastation.ALIEN);
				devastation.limit = 0;
			}
			Detonations.debris(world, g, 90, 260);
			Vfx.skyTint(world, g, 100000, 0x183020, 0.8F);
			Vfx.custom(world, FxType.WORLD_FX, g, fwd, (float) reach, 0x60FFA0, 5, base, 0, 1500, Vfx.FAR);
		}
	}
}
