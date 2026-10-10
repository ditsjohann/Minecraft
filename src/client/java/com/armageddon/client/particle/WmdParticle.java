package com.armageddon.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

/**
 * Data-driven billboard particle: three-stop colour ramp, size curve, emissive "hot" phase,
 * fade in/out, optional sprite animation and spin. Every custom particle in the mod is one of these.
 */
public class WmdParticle extends SpriteBillboardParticle {
	private final Preset preset;
	private final SpriteProvider sprites;
	private final float baseSize;
	private final float spin;

	public WmdParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz,
			SpriteProvider sprites, Preset preset, float sizeMultiplier) {
		super(world, x, y, z);
		this.preset = preset;
		this.sprites = sprites;
		this.velocityX = vx;
		this.velocityY = vy;
		this.velocityZ = vz;
		this.maxAge = preset.lifeMin + (preset.lifeMax > preset.lifeMin ? random.nextInt(preset.lifeMax - preset.lifeMin + 1) : 0);
		this.baseSize = preset.size * sizeMultiplier * (0.75F + random.nextFloat() * 0.5F);
		this.scale = baseSize * preset.sizeStart;
		this.gravityStrength = preset.gravity;
		this.velocityMultiplier = preset.drag;
		this.collidesWithWorld = preset.collide;
		this.spin = preset.spin * (random.nextFloat() - 0.5F);
		this.angle = random.nextFloat() * MathHelper.TAU;
		this.prevAngle = angle;
		if (preset.animated) {
			this.setSpriteForAge(sprites);
		} else {
			this.setSprite(sprites);
		}
		applyColor(0.0F);
		if (baseSize > 1.0F) {
			this.setBoundingBoxSpacing(baseSize, baseSize);
		}
	}

	private void applyColor(float t) {
		int c;
		if (t < preset.mid) {
			c = lerpColor(preset.c0, preset.c1, preset.mid <= 0 ? 1 : t / preset.mid);
		} else {
			c = lerpColor(preset.c1, preset.c2, preset.mid >= 1 ? 1 : (t - preset.mid) / (1 - preset.mid));
		}
		this.red = ((c >> 16) & 0xFF) / 255.0F;
		this.green = ((c >> 8) & 0xFF) / 255.0F;
		this.blue = (c & 0xFF) / 255.0F;
		float fadeIn = preset.fadeIn <= 0 ? 1 : Math.min(1.0F, t / preset.fadeIn);
		float fadeOut = Math.min(1.0F, (1.0F - t) / Math.max(0.01F, preset.fadeOut));
		this.alpha = preset.alpha * fadeIn * fadeOut;
	}

	static int lerpColor(int a, int b, float t) {
		t = MathHelper.clamp(t, 0, 1);
		int r = (int) MathHelper.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) {
			return;
		}
		float t = this.age / (float) this.maxAge;
		applyColor(t);
		this.scale = baseSize * MathHelper.lerp(t, preset.sizeStart, preset.sizeEnd);
		this.prevAngle = this.angle;
		this.angle += spin;
		if (preset.animated) {
			this.setSpriteForAge(sprites);
		}
		if (preset.flicker && random.nextInt(6) == 0) {
			this.alpha *= 0.4F;
		}
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public int getBrightness(float tint) {
		float t = this.age / (float) this.maxAge;
		if (t < preset.emissiveUntil) {
			return LightmapTextureManager.MAX_LIGHT_COORDINATE;
		}
		return super.getBrightness(tint);
	}

	/** Immutable tuning table. */
	public record Preset(int c0, int c1, int c2, float mid, float alpha, float size, float sizeStart, float sizeEnd,
			int lifeMin, int lifeMax, float gravity, float drag, boolean collide, float emissiveUntil,
			float fadeIn, float fadeOut, boolean animated, float spin, boolean flicker) {

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {
			private int c0 = 0xFFFFFF, c1 = 0xFFFFFF, c2 = 0xFFFFFF;
			private float mid = 0.5F, alpha = 1.0F, size = 0.1F, sizeStart = 1.0F, sizeEnd = 1.0F;
			private int lifeMin = 20, lifeMax = 30;
			private float gravity = 0.0F, drag = 0.96F;
			private boolean collide = false;
			private float emissiveUntil = 0.0F, fadeIn = 0.0F, fadeOut = 0.3F;
			private boolean animated = false, flicker = false;
			private float spin = 0.0F;

			public Builder colors(int c0, int c1, int c2, float mid) {
				this.c0 = c0;
				this.c1 = c1;
				this.c2 = c2;
				this.mid = mid;
				return this;
			}

			public Builder color(int c) {
				return colors(c, c, c, 0.5F);
			}

			public Builder alpha(float a) {
				this.alpha = a;
				return this;
			}

			public Builder size(float size, float start, float end) {
				this.size = size;
				this.sizeStart = start;
				this.sizeEnd = end;
				return this;
			}

			public Builder life(int min, int max) {
				this.lifeMin = min;
				this.lifeMax = max;
				return this;
			}

			public Builder physics(float gravity, float drag, boolean collide) {
				this.gravity = gravity;
				this.drag = drag;
				this.collide = collide;
				return this;
			}

			public Builder emissive(float until) {
				this.emissiveUntil = until;
				return this;
			}

			public Builder fade(float in, float out) {
				this.fadeIn = in;
				this.fadeOut = out;
				return this;
			}

			public Builder animated() {
				this.animated = true;
				return this;
			}

			public Builder spin(float spin) {
				this.spin = spin;
				return this;
			}

			public Builder flicker() {
				this.flicker = true;
				return this;
			}

			public Preset build() {
				return new Preset(c0, c1, c2, mid, alpha, size, sizeStart, sizeEnd, lifeMin, lifeMax, gravity, drag, collide,
						emissiveUntil, fadeIn, fadeOut, animated, spin, flicker);
			}
		}
	}
}
