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

import org.joml.Matrix4f;

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
	/** Effects created while ticking other effects (avoids concurrent modification). */
	static final List<FxEffect> EFFECTS_PENDING = new ArrayList<>();
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
	/** Rainbow hue-shift strength 0..1 (dubstep). */
	public static float hue;
	/** Screen-space shockwave ripple: progress 0..1 (1 = finished). */
	public static float ripple = 1.0F;
	/** Sky / fog tint colour and strength 0..1. */
	public static int skyTint = 0xFF5020;
	public static float skyTintStrength;
	/** Brightest current light source for god rays (world space) and its strength. */
	public static Vec3d lightPos;
	public static float lightStrength;
	public static int lightColor = 0xFFF0C0;

	private record Delayed(int at, Runnable action) {
	}

	private static int clock;

	public static List<FxEffect> effects() {
		return EFFECTS;
	}

	public static void clear() {
		EFFECTS_PENDING.clear();
		if (cine != null) {
			MinecraftClient.getInstance().options.hudHidden = hudWasHidden;
			cine = null;
		}
		letterbox = letterboxPrev = 0;
		letterboxHold = impactTicks = 0;
		fovPunch = 0;
		java.util.Arrays.fill(SCREEN, 0);
		java.util.Arrays.fill(SCREEN_HOLD, 0);
		EFFECTS.clear();
		DELAYED.clear();
		flash = 0;
		shake = 0;
		distortion = 0;
		hue = 0;
		ripple = 1.0F;
		skyTintStrength = 0;
		lightStrength = 0;
		lightPos = null;
	}

	public static void hueKick(float amount) {
		hue = Math.max(hue, amount);
	}

	public static void tintSky(int color, float strength) {
		if (strength >= skyTintStrength) {
			skyTint = color;
			skyTintStrength = Math.min(1.0F, strength);
		}
	}

	public static void light(Vec3d pos, float strength, int color) {
		if (strength >= lightStrength) {
			lightPos = pos;
			lightStrength = Math.min(1.5F, strength);
			lightColor = color;
		}
	}

	/** Lightning-style sky flash. */
	public static void skyFlash(int ticks) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world != null) {
			mc.world.setLightningTicksLeft(Math.max(mc.world.getLightningTicksLeft(), ticks));
		}
	}

	public static float cameraYaw() {
		return MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
	}

	/** World-event screen grading: 0 desaturate, 1 white-out, 2 black-out, 3 static, 4 invert. */
	public static final float[] SCREEN = new float[5];
	private static final int[] SCREEN_HOLD = new int[5];

	/** Letterbox amount 0..1 (eased) and its remaining hold. */
	public static float letterbox, letterboxPrev;
	public static int letterboxHold;
	/** Impact frame: remaining ticks and style. */
	public static int impactTicks, impactStyle;
	/** FOV punch in degrees, decaying. */
	public static float fovPunch, fovPunchDecay = 0.85F;

	/** The running scripted cinematic, if any. It owns the camera until it finishes. */
	public static Cinematics.Cine cine;
	private static boolean hudWasHidden;

	public static Cinematics.Cine activeCine() {
		return cine != null && !cine.done ? cine : null;
	}

	public static void impact(int style, int frames) {
		impactStyle = style;
		impactTicks = Math.max(impactTicks, Math.max(1, frames));
	}

	public static void screen(int kind, float strength, int hold) {
		if (kind < 0 || kind >= SCREEN.length) {
			return;
		}
		if (kind == 1) {
			// White-outs are brief and partial: a full white screen for a second is just unpleasant.
			strength = Math.min(strength, 0.45F);
			hold = Math.min(hold, 4);
		}
		SCREEN[kind] = Math.max(SCREEN[kind], MathHelper.clamp(strength, 0.0F, 1.0F));
		SCREEN_HOLD[kind] = Math.max(SCREEN_HOLD[kind], hold);
	}

	/** True while effects are being ticked; anything added then is queued (adding mid-iteration would crash). */
	private static boolean ticking;

	public static void add(FxEffect effect) {
		if (ticking) {
			if (EFFECTS.size() + EFFECTS_PENDING.size() < 512) {
				EFFECTS_PENDING.add(effect);
			}
			return;
		}
		if (EFFECTS.size() < 512) {
			EFFECTS.add(effect);
		}
	}

	public static void later(int ticks, Runnable action) {
		DELAYED.add(new Delayed(clock + Math.max(0, ticks), action));
	}

	public static Vec3d cameraPos() {
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc.gameRenderer.getCamera().getPos();
	}

	/** Adds a flash that falls off with distance. */
	public static void flash(Vec3d at, double range, float strength, int color) {
		double d = cameraPos().distanceTo(at);
		float f = (float) (Math.min(strength, FLASH_MAX) * MathHelper.clamp(1.0 - d / range, 0.0, 1.0));
		if (f > flash) {
			flash = f;
			flashColor = color;
		}
		distortion = Math.max(distortion, f);
	}

	/** Comfort limits: raw shake/flash values come from many effects, these keep the screen readable. */
	public static final float SHAKE_SCALE = 0.35F, SHAKE_MAX = 1.1F, FLASH_MAX = 0.55F;

	/** The camera wobble actually applied (degrees): scaled down and soft-capped so big blasts never get violent. */
	public static float shakeView() {
		float s = shake * SHAKE_SCALE;
		return s <= 0.0F ? 0.0F : SHAKE_MAX * s / (SHAKE_MAX + s);
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

	public static void playGlobal(SoundEvent sound, float volume, float pitch) {
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
			case FxType.OMEGA -> omega(world, a, s);
			case FxType.OMEGA_CHARGE -> {
				add(new InsaneEffects.Pillar(a, 3.0, (int) s + 10, 0xFF2030, false));
				add(new Effects.Ring(a, 120, 0xFF2030, (int) s, 3.0, true));
				tintSky(0x400008, 0.7F);
				shake(a, 600, 0.6F);
			}
			case FxType.KINETIC_IMPACT -> {
				add(new InsaneEffects.DustColumn(a, s));
				add(new InsaneEffects.Dome(a, s * 4, 30, 0xFFF0D0));
				add(new Effects.Ring(a, s * 6, 0xFFE0B0, 40, 2.0, true));
				add(new Effects.Fireball(a, s * 0.6, 3, 16, 0xFFFFFF, 0xFF8040));
				flash(a, 300, 0.6F, 0xFFF4E0);
				shockArrival(a, s, 2.5F, 1.4F);
				skyFlash(3);
			}
			case FxType.BFG_BURST -> {
				add(new Effects.Fireball(a, s, 4, 30, 0xEFFFE0, 0x30FF20));
				add(new InsaneEffects.Dome(a, s * 3, 24, 0x5AFF3A));
				add(new Effects.Ring(a, s * 3, 0x5AFF3A, 30, 1.5, true));
				flash(a, s * 20, 0.7F, 0xC8FFB0);
				tintSky(0x20FF30, 0.5F);
				light(a, 0.9F, 0x80FF60);
				burst(world, a, ModParticles.RADIATION, 200, 1.2);
				shake(a, s * 10, 2.0F);
			}
			case FxType.DUBSTEP_BEAM -> add(new InsaneEffects.DubstepBeam(a, b, s, p.seed()));
			case FxType.BASS_PULSE -> {
				add(new InsaneEffects.BassPulse(a, s, p.seed()));
				hueKick(1.0F);
				shake(a, s * 4, 1.0F);
				ripple = 0.0F;
				tintSky(InsaneEffects.rainbow(p.seed() * 0.13F), 0.5F);
			}
			case FxType.BASS_DROP -> {
				add(new InsaneEffects.Pillar(a, 4.0, 80, 0xFFFFFF, true));
				add(new InsaneEffects.Dome(a, s * 1.5, 40, 0xFF60FF));
				for (int i = 0; i < 6; i++) {
					add(new InsaneEffects.BassPulse(a, s * (0.6 + i * 0.3), i * 3));
				}
				add(new Effects.Fireball(a, 10, 4, 30, 0xFFFFFF, 0xFF40FF));
				flash(a, 400, 1.0F, 0xFFD8FF);
				hueKick(1.0F);
				ripple = 0.0F;
				shake(a, 300, 3.0F);
				tintSky(0xFF40FF, 1.0F);
				light(a.add(0, 6, 0), 1.2F, 0xFF80FF);
				skyFlash(4);
			}
			case FxType.JET_FLYOVER -> add(new InsaneEffects.JetFlyover(a, b));
			case FxType.DEATH_RAY -> {
				FxEffect beam = new InsaneEffects.ContinuousBeam(a, b, s, p.color());
				add(beam);
				distortion = Math.max(distortion, 0.15F);
			}
			case FxType.HEAT_RAY -> {
				add(new InsaneEffects.ContinuousBeam(a, b, s, p.color()));
				burst(world, b, ParticleTypes.FLAME, 8, 0.2);
			}
			case FxType.SUPERNOVA -> {
				add(new InsaneEffects.Supernova(a, s));
				flash(a, 2000, 1.0F, 0xE8F0FF);
				light(a, 1.5F, 0xE0E8FF);
				tintSky(0x6A8CFF, 1.0F);
				skyFlash(5);
			}
			case FxType.QUAKE -> {
				shake(a, s * 4, 2.2F);
				add(new Effects.Ring(a, s, 0xA08060, 20, 1.0, true));
				distortion = Math.max(distortion, 0.3F);
			}
			case FxType.CUTSCENE -> letterboxHold = Math.max(letterboxHold, p.duration());
			case FxType.IMPACT -> impact(p.seed(), p.duration());
			case FxType.FOV -> {
				fovPunch = Math.max(fovPunch, Math.min(s * 0.6F, 14.0F));
				fovPunchDecay = (float) Math.pow(0.02, 1.0 / Math.max(2, p.duration()));
			}
			case FxType.MOON -> add(new ArsenalEffects.Moon(a, s, p.p1(), p.color(), p.duration()));
			case FxType.CINEMATIC -> {
				Cinematics.Cine c = Cinematics.create(p.seed(), a, b, p.duration());
				MinecraftClient client = MinecraftClient.getInstance();
				if (cine == null) {
					// Only remember the player's own setting, never one we forced ourselves.
					hudWasHidden = client.options.hudHidden;
				} else {
					cine.done = true;
				}
				cine = c;
				add(c);
				client.options.hudHidden = true;
			}
			case FxType.TWISTER -> add(new MegaEffects.Twister(a, b, s, p.p1(), p.duration()));
			case FxType.UFO -> add(new MegaEffects.Ufo(a, b, s, p.duration()));
			case FxType.SPEAR -> add(new MegaEffects.Spear(a, s, p.p1(), p.duration()));
			case FxType.ALIEN_ROCKET -> add(new MegaEffects.AlienRocket(a, b, s, p.duration()));
			case FxType.STEAM -> ArsenalEffects.steam(world, a, s);
			case FxType.GLITCH -> add(new ArsenalEffects.Glitch(a, s, p.color(), p.seed(), p.duration()));
			case FxType.ORB -> add(new ArsenalEffects.Orb(a, s, p.p1(), p.color(), (int) p.p2(), p.duration()));
			case FxType.BUBBLE -> add(new ArsenalEffects.Bubble(a, s, p.color(), p.duration()));
			case FxType.CAGE -> add(new ArsenalEffects.Cage(a, s, p.color(), p.duration()));
			case FxType.GRID -> add(new ArsenalEffects.Grid(a, b, s, p.color(), (int) p.p2(), p.duration()));
			case FxType.PORTAL -> add(new ArsenalEffects.Portal(a, b, s, p.color(), p.duration()));
			case FxType.SKY_CRACK -> {
				add(new ArsenalEffects.SkyCrack(a, s, p.color(), (int) p.p2(), p.seed(), p.duration()));
				skyFlash(4);
				playGlobal(SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 0.3F);
			}
			case FxType.CRYSTAL -> add(new ArsenalEffects.Crystal(a, b, s, p.color(), p.duration()));
			case FxType.PAINT -> {
				if (p.color() == -1) {
					for (FxEffect e : EFFECTS) {
						if (e instanceof ArsenalEffects.Paint paint && paint.c.distanceTo(a) < s + paint.r) {
							paint.washed = true;
						}
					}
				} else {
					add(new ArsenalEffects.Paint(world, a, s, p.color(), p.seed(), p.duration()));
				}
			}
			case FxType.FIREWORK -> ArsenalEffects.firework(world, a, s, p.seed(), p.color());
			case FxType.HOLOGRAM -> add(new ArsenalEffects.Hologram(a, s, p.color(), p.seed(), p.duration()));
			case FxType.MIRROR -> add(new ArsenalEffects.Mirror(a, s, p.color(), p.seed(), p.duration()));
			case FxType.SOUNDWAVE -> add(new ArsenalEffects.Soundwave(a, b, s, p.color(), p.p1()));
			case FxType.SCREEN -> screen(p.seed(), s, p.duration());
			case FxType.LAYERED_BLAST -> ArsenalEffects.layeredBlast(world, a, s, p.color());
			case FxType.WIREBOX -> add(new ArsenalEffects.Wirebox(a, b, p.color(), p.duration()));
			case FxType.STREAK -> add(new ArsenalEffects.Streak(a, b, s, p.color(), p.duration()));
			case FxType.PILLAR -> add(new InsaneEffects.Pillar(a, s, Math.max(2, p.duration()), p.color(), p.p2() > 0));
			case FxType.DOME -> add(new InsaneEffects.Dome(a, s, Math.max(2, p.duration()), p.color()));
			case FxType.RING -> add(new Effects.Ring(a, s, p.color(), Math.max(2, p.duration()), Math.max(0.2, p.p1()), p.p2() > 0));
			case FxType.SPIRAL -> add(new ArsenalEffects.Spiral(a, s, p.p1(), p.color(), p.duration()));
			case FxType.CHARGE -> add(new ArsenalEffects.Charge(p.seed(), a, s, p.color(), p.duration()));
			case FxType.SKY_TINT -> tintSky(p.color(), s);
			case FxType.LIGHT -> light(a, s, p.color());
			case FxType.FLASH -> flash(a, Math.max(16, p.p1()), s, p.color());
			case FxType.SLICE -> add(new ArsenalEffects.Slice(a, b, s, p.p1(), p.color(), p.duration()));
			case FxType.WARNING -> add(new ArsenalEffects.Warning(a, p.seed()));
			case FxType.GRAVITY_FLIP -> add(new ArsenalEffects.GravityFlip(a, s, p.duration()));
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
		add(new InsaneEffects.Dome(at, radius * 7, (int) (radius * 7 / 5.0) + 10, 0xFFE8C8));
		add(new Lightning(at, radius));
		light(at.add(0, radius * 0.5, 0), 1.5F, 0xFFE0A0);
		tintSky(0xFF6A20, 1.0F);
		skyFlash(3);
		shockArrival(at, radius, 3.5F, 1.0F);
	}

	private static void omega(ClientWorld world, Vec3d at, float radius) {
		add(new Effects.Antimatter(at, radius));
		add(new InsaneEffects.Dome(at, radius * 9, 70, 0xFF6060));
		add(new InsaneEffects.Dome(at, radius * 5, 50, 0xFFFFFF));
		add(new Effects.Ring(at, radius * 14, 0xFF4030, 110, radius * 0.6, true));
		add(new InsaneEffects.Supernova(at.add(0, radius * 0.4, 0), radius * 0.6));
		tintSky(0xC01008, 1.0F);
		light(at.add(0, radius, 0), 1.5F, 0xFFC0A0);
		flash(at, 6000, 1.0F, 0xFFFFFF);
		skyFlash(6);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			add(new Effects.Arc(at.add(0, radius * 0.6, 0), at.add(Math.cos(a) * radius * 1.4, radius * (0.2 + RANDOM.nextDouble()), Math.sin(a) * radius * 1.4),
					radius * 0.04, 0xFFD0FF, RANDOM.nextInt()));
		}
	}

	/** Lightning tendrils crackling through a fresh fireball. */
	private static class Lightning extends FxEffect {
		private final Vec3d center;
		private final double radius;

		Lightning(Vec3d center, double radius) {
			super(70);
			this.center = center;
			this.radius = radius;
		}

		@Override
		public boolean tick(ClientWorld world) {
			if (age % 4 == 0 && RANDOM.nextFloat() < 0.8F) {
				double rise = Math.min(1.0, age / 40.0) * radius * 3;
				Vec3d c = center.add(0, rise, 0);
				Vec3d d = new Vec3d(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.6, RANDOM.nextGaussian()).normalize().multiply(radius * (0.8 + RANDOM.nextDouble()));
				EFFECTS_PENDING.add(new Effects.Arc(c, c.add(d), Math.max(0.15, radius * 0.02), 0xD8E8FF, RANDOM.nextInt()));
				if (RANDOM.nextFloat() < 0.3F) {
					skyFlash(2);
				}
			}
			return super.tick(world);
		}

		@Override
		public void render(Matrix4f m, net.minecraft.client.render.VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float yaw, float pitch) {
		}
	}

	/** Shake + boom arrive later the further away you are (speed of sound ≈ 17 blocks/tick). */
	private static void shockArrival(Vec3d at, float radius, float shakeStrength, float pitch) {
		double d = cameraPos().distanceTo(at);
		int delay = (int) (d / 17.0);
		float vol = (float) MathHelper.clamp(1.0 - d / (radius * 60.0), 0.08, 1.0);
		later(delay, () -> {
			shake = Math.max(shake, shakeStrength * (float) MathHelper.clamp(1.0 - d / (radius * 25.0), 0.05, 1.0));
			if (d < radius * 30) {
				ripple = 0.0F;
			}
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
		ticking = true;
		try {
			EFFECTS.removeIf(e -> {
				try {
					return !e.tick(mc.world);
				} catch (RuntimeException ex) {
					com.armageddon.Armageddon.LOGGER.error("Armageddon effect {} failed while ticking; removing it", e.getClass().getSimpleName(), ex);
					if (e instanceof Cinematics.Cine c) {
						c.done = true;
					}
					return true;
				}
			});
		} finally {
			ticking = false;
		}
		if (!EFFECTS_PENDING.isEmpty()) {
			EFFECTS.addAll(EFFECTS_PENDING);
			EFFECTS_PENDING.clear();
		}
		for (int k = 0; k < SCREEN.length; k++) {
			if (SCREEN_HOLD[k] > 0) {
				SCREEN_HOLD[k]--;
			} else {
				SCREEN[k] *= k == 1 ? 0.8F : 0.9F;
				if (SCREEN[k] < 0.005F) {
					SCREEN[k] = 0;
				}
			}
		}
		if (cine != null && cine.done) {
			cine = null;
			MinecraftClient.getInstance().options.hudHidden = hudWasHidden;
		}
		letterboxPrev = letterbox;
		if (letterboxHold > 0) {
			letterboxHold--;
			letterbox = Math.min(1.0F, letterbox + 0.08F);
		} else {
			letterbox = Math.max(0.0F, letterbox - 0.05F);
		}
		if (impactTicks > 0) {
			impactTicks--;
		}
		fovPunch *= fovPunchDecay;
		if (fovPunch < 0.05F) {
			fovPunch = 0;
		}
		hue *= 0.9F;
		if (hue < 0.01F) {
			hue = 0;
		}
		if (ripple < 1.0F) {
			ripple = Math.min(1.0F, ripple + 0.04F);
		}
		skyTintStrength *= skyTintStrength > 0.5F ? 0.997F : 0.99F;
		if (skyTintStrength < 0.01F) {
			skyTintStrength = 0;
		}
		lightStrength *= 0.975F;
		if (lightStrength < 0.02F) {
			lightStrength = 0;
			lightPos = null;
		}

		flash = Math.min(flash, FLASH_MAX) * 0.82F;
		if (flash < 0.003F) {
			flash = 0;
		}
		shake *= 0.86F;
		if (shake < 0.01F) {
			shake = 0;
		}
		distortion = Math.min(0.6F, Math.max(distortion * 0.95F, shakeView() * 0.1F));
		if (distortion < 0.003F) {
			distortion = 0;
		}
	}

	private ClientFx() {
	}
}
