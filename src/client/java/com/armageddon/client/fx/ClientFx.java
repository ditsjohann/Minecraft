package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;
import com.armageddon.network.FxType;
import com.armageddon.registry.ModParticles;
import com.armageddon.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client visual-effect engine. Receives {@link FxPayload}s, keeps a list of live effects,
 * and drives the screen-space state (flash, shake, chromatic aberration) used by the
 * post-processing shader and camera mixin.
 */
public final class ClientFx {
	static final List<FxEffect> EFFECTS = new ArrayList<>();
	private static final List<Delayed> DELAYED = new ArrayList<>();
	private static final Random RANDOM = Random.create();

	/** 0..1 white-out. */
	public static float flash;
	/** Colour of the current flash. */
	public static int flashColor = 0xFFFFFF;
	/** Camera shake amplitude (degrees). */
	public static float shake;
	/** Chromatic aberration / heat distortion strength 0..1. */
	public static float distortion;

	private record Delayed(int at, Runnable action) {
	}

	private static int clock;

	public static List<FxEffect> effects() {
		return EFFECTS;
	}

	public static void clear() {
		EFFECTS.clear();
		DELAYED.clear();
		flash = 0;
		shake = 0;
		distortion = 0;
	}

	public static void add(FxEffect effect) {
		if (EFFECTS.size() < 512) {
			EFFECTS.add(effect);
		}
	}

	public static void later(int ticks, Runnable action) {
		DELAYED.add(new Delayed(clock + Math.max(0, ticks), action));
	}

	private static Vec3d cameraPos() {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc.gameRenderer.getCamera().getPos();
	}

	/** Adds a flash that falls off with distance. */
	public static void flash(Vec3d at, double range, float strength, int color) {
		double d = cameraPos().distanceTo(at);
		float f = (float) (strength * MathHelper.clamp(1.0 - d / range, 0.0, 1.0));
		if (f > flash) {
			flash = f;
			flashColor = color;
		}
		distortion = Math.max(distortion, f);
	}

	public static void shake(Vec3d at, double range, float strength) {
		double d = cameraPos().distanceTo(at);
		float f = (float) (strength * MathHelper.clamp(1.0 - d / range, 0.0, 1.0));
		shake = Math.max(shake, f);
	}

	private static void playAt(SoundEvent sound, Vec3d at, float volume, float pitch) {
		MinecraftClient mc = MinecraftClient.getInstance();
		mc.getSoundManager().play(new PositionedSoundInstance(sound, SoundCategory.BLOCKS, volume, pitch, RANDOM, at.x, at.y, at.z));
	}

	private static void playGlobal(SoundEvent sound, float volume, float pitch) {
		MinecraftClient mc = MinecraftClient.getInstance();
		mc.getSoundManager().play(new PositionedSoundInstance(sound.getId(), SoundCategory.BLOCKS, volume, pitch, RANDOM,
				false, 0, SoundInstance.AttenuationType.NONE, 0, 0, 0, true));
	}

	// ------------------------------------------------------------------ dispatch

	public static void handle(FxPayload p) {
		MinecraftClient mc = MinecraftClient.getInstance();
		ClientWorld world = mc.world;
		if (world == null) {
			return;
		}
		Vec3d a = p.from();
		Vec3d b = p.to();
		float s = p.scale();
		switch (p.type()) {
			case FxType.NUKE -> nuke(world, a, s);
			case FxType.ANTIMATTER -> {
				add(new Effects.Antimatter(a, s));
				add(new Effects.Ring(a, s * 6, 0xD090FF, 90, s * 0.6, true));
				shockArrival(a, s, 2.0F, 0.8F);
			}
			case FxType.NEUTRON -> {
				flash(a, s * 4, 0.85F, 0xB8FF9A);
				add(new Effects.Fireball(a, 6, 4, 25, 0xEFFFD0, 0x60FF30));
				add(new Effects.Ring(a, s, 0x7CFF2E, 70, 3.0, false));
				add(new Effects.Ring(a, s * 0.6, 0xC8FFA0, 50, 1.5, false));
				shake(a, s * 2, 1.0F);
				burst(world, a, ModParticles.RADIATION, 120, 1.2);
			}
			case FxType.CRYO_WAVE -> {
				flash(a, s * 3, 0.35F, 0xD8F4FF);
				add(new Effects.Ring(a, s, 0xA8E8FF, 40, Math.max(1.0, s * 0.12), false));
				add(new Effects.Fireball(a, s * 0.25, 4, 18, 0xFFFFFF, 0x6AC8FF));
				burst(world, a, ModParticles.CRYO_MIST, (int) (30 + s * 6), 0.15 + s * 0.02);
				burst(world, a, ParticleTypes.SNOWFLAKE, (int) (40 + s * 4), 0.4 + s * 0.03);
				shake(a, s * 2, Math.min(1.5F, s * 0.1F));
			}
			case FxType.FIREBALL -> {
				add(new Effects.Fireball(a, s * 0.9, 3, 14 + (int) (s * 2), 0xFFF0C0, p.color()));
				burst(world, a, ParticleTypes.FLAME, (int) (10 + s * 6), 0.12 + s * 0.04);
				burst(world, a, ModParticles.EMBER, (int) (10 + s * 5), 0.25 + s * 0.05);
				burst(world, a, ParticleTypes.LARGE_SMOKE, (int) (6 + s * 3), 0.05 + s * 0.02);
				flash(a, s * 8, Math.min(0.35F, s * 0.04F), 0xFFE0B0);
				shake(a, s * 6, Math.min(1.2F, s * 0.15F));
			}
			case FxType.BEAM -> add(new Effects.Beam(a, b, s, p.color(), Math.max(4, p.seed())));
			case FxType.ARC -> add(new Effects.Arc(a, b, s, p.color(), p.seed()));
			case FxType.TRACER -> add(new Effects.Beam(a, b, s, p.color(), 3));
			case FxType.ORBITAL_TARGET -> add(new Effects.OrbitalTarget(a, (int) s));
			case FxType.ORBITAL_BEAM -> {
				add(new Effects.OrbitalBeam(a, s));
				add(new Effects.Ring(a, s * 6, 0x8FE6FF, 50, 2.0, true));
				flash(a, 200, 0.7F, 0xD8F6FF);
				shake(a, 120, 2.0F);
			}
			case FxType.SHOCKWAVE -> add(new Effects.Ring(a, s, p.color(), (int) (14 + s * 0.8), Math.max(0.8, s * 0.1), s > 6));
			case FxType.COLLAPSE -> {
				add(new Effects.Fireball(a, s * 0.8, 3, 20, 0xFFFFFF, 0x8040FF));
				add(new Effects.Ring(a, s * 4, 0xC080FF, 40, 1.5, false));
				flash(a, s * 20, 0.6F, 0xE0C8FF);
				shake(a, s * 8, 1.5F);
				burst(world, a, ModParticles.VOID_SPARK, 150, 0.8);
			}
			case FxType.PLASMA_BURST -> {
				add(new Effects.Fireball(a, 0.6 * s, 2, 8, 0xFFFFFF, p.color()));
				burst(world, a, ModParticles.PLASMA_SPARK, (int) (14 * s), 0.25);
			}
			case FxType.SHAKE -> shake = Math.max(shake, s);
			default -> {
			}
		}
	}

	private static void nuke(ClientWorld world, Vec3d at, float radius) {
		// Light travels instantly.
		flash(at, radius * 40, 1.0F, 0xFFF6E0);
		add(new Effects.Fireball(at, radius * 1.15, 18, 110, 0xFFFFF0, 0xFF5A10));
		add(new Effects.MushroomCloud(at, radius));
		add(new Effects.Ring(at, radius * 9, 0xFFE0B0, (int) (radius * 9 / 4.0), radius * 0.4, true));
		add(new Effects.Ring(at, radius * 3, 0xFFFFFF, 30, radius * 0.25, false));
		for (int i = 0; i < 4; i++) {
			world.addImportantParticle(ParticleTypes.EXPLOSION_EMITTER, true, at.x, at.y, at.z, 0, 0, 0);
		}
		shockArrival(at, radius, 3.5F, 1.0F);
	}

	/** Shake + boom arrive later the further away you are (speed of sound ≈ 17 blocks/tick). */
	private static void shockArrival(Vec3d at, float radius, float shakeStrength, float pitch) {
		double d = cameraPos().distanceTo(at);
		int delay = (int) (d / 17.0);
		float vol = (float) MathHelper.clamp(1.0 - d / (radius * 60.0), 0.08, 1.0);
		later(delay, () -> {
			shake = Math.max(shake, shakeStrength * (float) MathHelper.clamp(1.0 - d / (radius * 25.0), 0.05, 1.0));
			playGlobal(ModSounds.NUKE_DETONATE, vol, pitch);
			playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), vol, 0.5F * pitch);
		});
		later(delay + 8, () -> playGlobal(ModSounds.NUKE_RUMBLE, vol * 0.9F, 0.7F));
		later(delay + 40, () -> playGlobal(ModSounds.NUKE_RUMBLE, vol * 0.6F, 0.5F));
	}

	private static void burst(ClientWorld world, Vec3d at, net.minecraft.particle.ParticleEffect type, int count, double speed) {
		for (int i = 0; i < count; i++) {
			Vec3d v = new Vec3d(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().multiply(speed * (0.3 + RANDOM.nextDouble()));
			world.addImportantParticle(type, true, at.x, at.y, at.z, v.x, v.y, v.z);
		}
	}

	// ------------------------------------------------------------------ tick

	public static void tick(MinecraftClient mc) {
		clock++;
		if (mc.world == null) {
			if (!EFFECTS.isEmpty() || !DELAYED.isEmpty()) {
				clear();
			}
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		if (!DELAYED.isEmpty()) {
			List<Delayed> due = new ArrayList<>();
			Iterator<Delayed> it = DELAYED.iterator();
			while (it.hasNext()) {
				Delayed d = it.next();
				if (d.at <= clock) {
					due.add(d);
					it.remove();
				}
			}
			due.forEach(d -> d.action.run());
		}
		EFFECTS.removeIf(e -> !e.tick(mc.world));

		flash *= flash > 0.6F ? 0.95F : 0.88F;
		if (flash < 0.003F) {
			flash = 0;
		}
		shake *= 0.9F;
		if (shake < 0.01F) {
			shake = 0;
		}
		distortion = Math.max(distortion * 0.97F, shake * 0.15F);
		if (distortion < 0.003F) {
			distortion = 0;
		}
	}

	private ClientFx() {
	}
}
