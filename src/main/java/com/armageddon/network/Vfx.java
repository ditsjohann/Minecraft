package com.armageddon.network;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Server-side helpers that compose the client FX vocabulary. Weapons build their signature
 * sequences out of these calls (often scheduled over several ticks).
 */
public final class Vfx {
	public static final double RANGE = 1024;
	public static final double FAR = 4096;

	private static void send(ServerWorld w, Vec3d at, double range, FxPayload p) {
		ModNetworking.sendFx(w, at, range, p);
	}

	private static FxPayload p(int type, Vec3d a, Vec3d b, float scale, int color, int seed, float p1, float p2, int duration) {
		return new FxPayload(type, a.x, a.y, a.z, b.x, b.y, b.z, scale, color, seed, p1, p2, duration);
	}

	public static void orb(ServerWorld w, Vec3d c, float r0, float r1, int color, int mode, int duration) {
		send(w, c, FAR, p(FxType.ORB, c, c, r0, color, 0, r1, mode, duration));
	}

	public static void bubble(ServerWorld w, Vec3d c, float r, int color, int duration) {
		send(w, c, RANGE, p(FxType.BUBBLE, c, c, r, color, 0, 0, 0, duration));
	}

	public static void cage(ServerWorld w, Vec3d c, float r, int color, int duration) {
		send(w, c, RANGE, p(FxType.CAGE, c, c, r, color, 0, 0, 0, duration));
	}

	public static void grid(ServerWorld w, Vec3d c, Vec3d normal, float r, int color, int mode, int duration) {
		send(w, c, FAR, p(FxType.GRID, c, normal, r, color, 0, 0, mode, duration));
	}

	public static void portal(ServerWorld w, Vec3d c, Vec3d normal, float r, int color, int duration) {
		send(w, c, FAR, p(FxType.PORTAL, c, normal, r, color, 0, 0, 0, duration));
	}

	public static void skyCrack(ServerWorld w, Vec3d c, float intensity, int color, int style, int duration) {
		send(w, c, FAR, p(FxType.SKY_CRACK, c, c, intensity, color, w.random.nextInt(), 0, style, duration));
	}

	public static void crystal(ServerWorld w, Vec3d base, Vec3d tip, float width, int color, int duration) {
		send(w, base, RANGE, p(FxType.CRYSTAL, base, tip, width, color, 0, 0, 0, duration));
	}

	public static void paint(ServerWorld w, Vec3d c, float r, int color, int duration) {
		send(w, c, 256, p(FxType.PAINT, c, c, r, color, w.random.nextInt(), 0, 0, duration));
	}

	public static void firework(ServerWorld w, Vec3d c, float size, int shape, int color) {
		send(w, c, 512, p(FxType.FIREWORK, c, c, size, color, shape, 0, 0, 0));
	}

	public static void hologram(ServerWorld w, Vec3d c, float r, int duration) {
		send(w, c, 256, p(FxType.HOLOGRAM, c, c, r, 0x40FFFF, w.random.nextInt(), 0, 0, duration));
	}

	public static void mirror(ServerWorld w, Vec3d c, float r, int duration) {
		send(w, c, 256, p(FxType.MIRROR, c, c, r, 0xB060FF, w.random.nextInt(), 0, 0, duration));
	}

	public static void soundwave(ServerWorld w, Vec3d from, Vec3d to, float width, int color, float pitch) {
		send(w, from, 256, p(FxType.SOUNDWAVE, from, to, width, color, 0, pitch, 0, 0));
	}

	public static void screen(ServerWorld w, Vec3d c, double range, int kind, float strength, int duration) {
		send(w, c, range, p(FxType.SCREEN, c, c, strength, 0, kind, 0, 0, duration));
	}

	public static void layeredBlast(ServerWorld w, Vec3d c, float size, int color) {
		send(w, c, Math.max(256, size * 40), p(FxType.LAYERED_BLAST, c, c, size, color, 0, 0, 0, 0));
	}

	public static void wirebox(ServerWorld w, Vec3d min, Vec3d max, int color, int duration) {
		send(w, min, 256, p(FxType.WIREBOX, min, max, 1, color, 0, 0, 0, duration));
	}

	public static void streak(ServerWorld w, Vec3d from, Vec3d to, float width, int color, int duration) {
		send(w, to, RANGE, p(FxType.STREAK, from, to, width, color, 0, 0, 0, duration));
	}

	public static void pillar(ServerWorld w, Vec3d base, float r, int color, boolean rainbow, int duration) {
		send(w, base, FAR, p(FxType.PILLAR, base, base, r, color, 0, 0, rainbow ? 1 : 0, duration));
	}

	public static void dome(ServerWorld w, Vec3d c, float r, int color, int duration) {
		send(w, c, FAR, p(FxType.DOME, c, c, r, color, 0, 0, 0, duration));
	}

	public static void ring(ServerWorld w, Vec3d c, float r, float thickness, int color, boolean dust, int duration) {
		send(w, c, Math.max(512, r * 8), p(FxType.RING, c, c, r, color, 0, thickness, dust ? 1 : 0, duration));
	}

	public static void spiral(ServerWorld w, Vec3d c, float r, float height, int color, int duration) {
		send(w, c, 256, p(FxType.SPIRAL, c, c, r, color, 0, height, 0, duration));
	}

	public static void charge(ServerWorld w, int entityId, Vec3d near, float size, int color, int duration) {
		send(w, near, 256, p(FxType.CHARGE, near, near, size, color, entityId, 0, 0, duration));
	}

	public static void skyTint(ServerWorld w, Vec3d c, double range, int color, float strength) {
		send(w, c, range, p(FxType.SKY_TINT, c, c, strength, color, 0, 0, 0, 0));
	}

	public static void light(ServerWorld w, Vec3d c, float strength, int color) {
		send(w, c, FAR, p(FxType.LIGHT, c, c, strength, color, 0, 0, 0, 0));
	}

	public static void flash(ServerWorld w, Vec3d c, float strength, float range, int color) {
		send(w, c, Math.max(range, 64), p(FxType.FLASH, c, c, strength, color, 0, range, 0, 0));
	}

	public static void shake(ServerWorld w, Vec3d c, double range, float strength) {
		send(w, c, range, new FxPayload(FxType.SHAKE, c, strength, 0));
	}

	public static void arc(ServerWorld w, Vec3d a, Vec3d b, float width, int color) {
		send(w, a, 512, new FxPayload(FxType.ARC, a, b, width, color, w.random.nextInt()));
	}

	public static void beam(ServerWorld w, Vec3d a, Vec3d b, float width, int color, int life) {
		send(w, a, 1024, new FxPayload(FxType.BEAM, a, b, width, color, life));
	}

	public static void continuousBeam(ServerWorld w, Vec3d a, Vec3d b, float width, int color) {
		send(w, a, 1024, new FxPayload(FxType.DEATH_RAY, a, b, width, color, 0));
	}

	public static void warning(ServerWorld w, Vec3d c, int tier) {
		send(w, c, FAR, p(FxType.WARNING, c, c, 1, 0xFF2020, tier, 0, 0, 0));
	}

	public static void gravityFlip(ServerWorld w, Vec3d c, float r, int duration) {
		send(w, c, FAR, p(FxType.GRAVITY_FLIP, c, c, r, 0xFFFFFF, 0, 0, 0, duration));
	}

	public static void steam(ServerWorld w, Vec3d c, float r) {
		send(w, c, 256, new FxPayload(FxType.STEAM, c, r, 0xF0F8FF));
	}

	public static void glitch(ServerWorld w, Vec3d c, float r, int color, int duration) {
		send(w, c, RANGE, p(FxType.GLITCH, c, c, r, color, w.random.nextInt(), 0, 0, duration));
	}

	/** A shimmering cutting plane through {@code c} (normal horizontal), {@code half} wide, {@code height} tall. */
	public static void slice(ServerWorld w, Vec3d c, Vec3d normal, float half, float height, int color, int duration) {
		send(w, c, FAR, p(FxType.SLICE, c, normal, half, color, 0, height, 0, duration));
	}

	/** Letterbox bars for a cutscene. */
	public static void cutscene(ServerWorld w, Vec3d c, int ticks) {
		send(w, c, FAR * 4, p(FxType.CUTSCENE, c, c, 1, 0, 0, 0, 0, ticks));
	}

	/** Anime impact frame. style: 0 ink, 1 inverted ink, 2 red ink, 3 white. */
	public static void impact(ServerWorld w, Vec3d c, int style, int frames) {
		send(w, c, FAR * 4, p(FxType.IMPACT, c, c, 1, 0, style, 0, 0, frames));
	}

	/** Sharp field-of-view kick that eases back. */
	public static void fov(ServerWorld w, Vec3d c, float degrees, int ticks) {
		send(w, c, FAR * 4, p(FxType.FOV, c, c, degrees, 0, 0, 0, 0, ticks));
	}

	/** A moon falling onto {@code ground}. */
	public static void moon(ServerWorld w, Vec3d ground, float radius, float startHeight, int ticks) {
		send(w, ground, FAR * 4, p(FxType.MOON, ground, ground, radius, 0xC8C8D0, 0, startHeight, 0, ticks));
	}

	public static void cinematic(ServerWorld w, Vec3d anchor, int scene, Vec3d heading, int ticks) {
		send(w, anchor, FAR * 4, p(FxType.CINEMATIC, anchor, heading, 1, 0, scene, 0, 0, ticks));
	}

	public static void twister(ServerWorld w, Vec3d start, Vec3d dir, float radius, float speed, int ticks) {
		send(w, start, FAR * 2, p(FxType.TWISTER, start, dir, radius, 0, 0, speed, 0, ticks));
	}

	public static void ufo(ServerWorld w, Vec3d from, Vec3d to, float radius, int ticks) {
		send(w, from.lerp(to, 0.5), FAR * 2, p(FxType.UFO, from, to, radius, 0, 0, 0, 0, ticks));
	}

	public static void spear(ServerWorld w, Vec3d target, float length, float sinkDepth, int ticks) {
		send(w, target, FAR * 2, p(FxType.SPEAR, target, target, length, 0, 0, sinkDepth, 0, ticks));
	}

	public static void alienRocket(ServerWorld w, Vec3d from, Vec3d to, float length, int ticks) {
		send(w, to, FAR * 4, p(FxType.ALIEN_ROCKET, to, from, length, 0, 0, 0, 0, ticks));
	}

	private Vfx() {
	}
}
