package com.armageddon.client.vfx;

import com.armageddon.client.fx.ClientFx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * A whole effect: emitters started on a timeline, plus cues (flash, shake, light, sound) that hook into the rest of
 * the client FX. Cues can be "carried by the air": they land later the further the camera is, at the speed of
 * sound, so a distant blast flashes first and booms after.
 */
public final class EffectDef {
	public record Entry(int at, EmitterDef emitter) {
	}

	/** Runs on the client when its tick comes; only with a live world (never in the offline preview). */
	@FunctionalInterface
	public interface Action {
		void run(EffectInstance fx);
	}

	public record Cue(int at, Action action, boolean sonic) {
	}

	/** Blocks per tick: about 340 m/s. */
	static final float SOUND_SPEED = 17.0F;

	final String name;
	final List<Entry> entries;
	final List<Cue> cues;
	final int maxAge;
	/** Distance (blocks at scale 1) at which the effect starts to thin out, and where it is thinnest. */
	final float lodNear, lodFar;
	final int lastEntry;
	/** Tint used when the effect is played without one. */
	final int tint;

	private EffectDef(Builder b) {
		name = b.name;
		List<Entry> e = new ArrayList<>(b.entries);
		e.sort(Comparator.comparingInt(Entry::at));
		entries = List.copyOf(e);
		cues = List.copyOf(b.cues);
		maxAge = b.maxAge;
		lodNear = b.lodNear;
		lodFar = b.lodFar;
		lastEntry = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).at;
		tint = b.tint;
	}

	public String name() {
		return name;
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public static final class Builder {
		private final String name;
		private final List<Entry> entries = new ArrayList<>();
		private final List<Cue> cues = new ArrayList<>();
		private int maxAge = 1200;
		private float lodNear = 48, lodFar = 320;
		private int tint;

		private Builder(String name) {
			this.name = name;
		}

		public Builder emit(int at, EmitterDef emitter) {
			entries.add(new Entry(at, emitter));
			return this;
		}

		public Builder emit(EmitterDef emitter) {
			return emit(0, emitter);
		}

		public Builder cue(int at, Action action) {
			cues.add(new Cue(at, action, false));
			return this;
		}

		/** A cue that travels at the speed of sound from the effect to the camera. */
		public Builder sonic(int at, Action action) {
			cues.add(new Cue(at, action, true));
			return this;
		}

		/** Safety cap on how long the effect may run. */
		public Builder maxAge(int ticks) {
			maxAge = ticks;
			return this;
		}

		public Builder lod(float near, float far) {
			lodNear = near;
			lodFar = far;
			return this;
		}

		/** The tint for {@link EmitterDef.Builder#tinted()} emitters when the effect is played without one. */
		public Builder tint(int rgb) {
			tint = rgb;
			return this;
		}

		public EffectDef build() {
			return new EffectDef(this);
		}
	}

	// ------------------------------------------------------------------ ready-made cue actions

	/** The screen flash, fading out over {@code range} blocks (both scale with the effect). */
	public static Action flash(float strength, float range, int rgb) {
		return fx -> ClientFx.flash(fx.origin(), range * fx.scale, strength, rgb);
	}

	public static Action shake(float strength, float range) {
		return fx -> ClientFx.shake(fx.origin(), range * fx.scale, strength);
	}

	/** The shared scene light (sky and terrain tint toward the blast). */
	public static Action light(float strength, int rgb) {
		return fx -> ClientFx.light(fx.origin(), strength, rgb);
	}

	/** A positioned sound; {@code sound} is looked up only when it plays. */
	public static Action sound(Supplier<SoundEvent> sound, float volume, float pitch) {
		return fx -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			Vec3d o = fx.origin();
			float p = pitch * (0.95F + fx.cueRng.next() * 0.1F);
			mc.getSoundManager().play(new PositionedSoundInstance(sound.get(), SoundCategory.BLOCKS, volume * fx.scale, p,
					Random.create(fx.seed), o.x, o.y, o.z));
		};
	}

	/** Starts another effect from this one's origin (same direction, scale and tint). */
	public static Action chain(String effect) {
		return fx -> FxEngine.play(effect, fx.ox, fx.oy, fx.oz, fx.dir[0], fx.dir[1], fx.dir[2], fx.scale, fx.tint, fx.seed * 31 + 7);
	}
}
