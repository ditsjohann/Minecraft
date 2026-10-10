package com.armageddon.client.particle;

import com.armageddon.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.particle.SimpleParticleType;

/** Particle presets. The sprite providers are kept so the FX engine can spawn particles directly. */
public final class ModParticleFactories {
	public static final WmdParticle.Preset NUKE_FIRE = WmdParticle.Preset.builder()
			.colors(0xFFF6D0, 0xFF7A1E, 0x4A403C, 0.22F).alpha(0.92F).size(3.5F, 0.6F, 1.6F).life(260, 520)
			.physics(-0.002F, 0.985F, false).emissive(0.18F).fade(0.02F, 0.25F).spin(0.02F).build();
	public static final WmdParticle.Preset NUKE_SMOKE = WmdParticle.Preset.builder()
			.colors(0x8A7E74, 0x5C5552, 0x3A3634, 0.5F).alpha(0.85F).size(4.0F, 0.7F, 1.8F).life(300, 600)
			.physics(-0.001F, 0.98F, false).fade(0.05F, 0.3F).spin(0.015F).build();
	public static final WmdParticle.Preset SHOCK_DUST = WmdParticle.Preset.builder()
			.colors(0xB09A80, 0x8A7A68, 0x6A6058, 0.5F).alpha(0.7F).size(1.6F, 0.5F, 2.0F).life(40, 90)
			.physics(-0.003F, 0.93F, false).fade(0.05F, 0.5F).spin(0.03F).build();
	public static final WmdParticle.Preset PLASMA_SPARK = WmdParticle.Preset.builder()
			.colors(0xFFFFFF, 0x6FFFF0, 0x1A8CFF, 0.3F).alpha(1.0F).size(0.12F, 1.0F, 0.0F).life(8, 16)
			.physics(0.0F, 0.9F, false).emissive(1.0F).fade(0.0F, 0.4F).build();
	public static final WmdParticle.Preset ARC_SPARK = WmdParticle.Preset.builder()
			.colors(0xFFFFFF, 0xB8E8FF, 0x6A9CFF, 0.4F).alpha(1.0F).size(0.07F, 1.0F, 0.2F).life(3, 8)
			.physics(0.4F, 0.9F, true).emissive(1.0F).fade(0.0F, 0.5F).animated().flicker().build();
	public static final WmdParticle.Preset RADIATION = WmdParticle.Preset.builder()
			.colors(0xD8FF9A, 0x7CFF2E, 0x2E9A10, 0.5F).alpha(0.85F).size(0.06F, 0.4F, 1.0F).life(30, 70)
			.physics(0.0F, 0.98F, false).emissive(1.0F).fade(0.2F, 0.5F).flicker().build();
	public static final WmdParticle.Preset CRYO_MIST = WmdParticle.Preset.builder()
			.colors(0xFFFFFF, 0xCFF2FF, 0x9AD8FF, 0.5F).alpha(0.55F).size(0.45F, 0.5F, 1.6F).life(20, 45)
			.physics(-0.002F, 0.94F, false).fade(0.1F, 0.6F).spin(0.05F).build();
	public static final WmdParticle.Preset VOID_SPARK = WmdParticle.Preset.builder()
			.colors(0xFFE0FF, 0xC060FF, 0x30106A, 0.4F).alpha(1.0F).size(0.1F, 1.0F, 0.3F).life(18, 34)
			.physics(0.0F, 1.0F, false).emissive(0.8F).fade(0.1F, 0.3F).build();
	public static final WmdParticle.Preset NAPALM_FLAME = WmdParticle.Preset.builder()
			.colors(0xFFF2A0, 0xFF5A10, 0x2A2420, 0.55F).alpha(0.9F).size(0.35F, 0.4F, 2.4F).life(14, 22)
			.physics(-0.02F, 0.9F, true).emissive(0.6F).fade(0.0F, 0.35F).animated().spin(0.2F).build();
	public static final WmdParticle.Preset EMBER = WmdParticle.Preset.builder()
			.colors(0xFFF0A0, 0xFF7A1A, 0x8A1A00, 0.5F).alpha(1.0F).size(0.05F, 1.0F, 0.4F).life(20, 45)
			.physics(-0.01F, 0.96F, true).emissive(1.0F).fade(0.0F, 0.4F).flicker().build();
	public static final WmdParticle.Preset TOXIC_GAS = WmdParticle.Preset.builder()
			.colors(0xE8FF6A, 0x9ACD32, 0x4A6A1A, 0.5F).alpha(0.45F).size(0.9F, 0.4F, 1.8F).life(40, 80)
			.physics(-0.001F, 0.96F, true).emissive(0.15F).fade(0.15F, 0.5F).spin(0.04F).build();
	public static final WmdParticle.Preset MUZZLE_FLASH = WmdParticle.Preset.builder()
			.colors(0xFFFFFF, 0xFFE07A, 0xFF8A2A, 0.4F).alpha(1.0F).size(0.45F, 1.0F, 0.3F).life(2, 3)
			.physics(0.0F, 0.0F, false).emissive(1.0F).fade(0.0F, 0.6F).build();

	public static SpriteProvider fireSprites;
	public static SpriteProvider smokeSprites;
	public static SpriteProvider dustSprites;
	public static SpriteProvider glowSprites;

	public static void register() {
		ParticleFactoryRegistry reg = ParticleFactoryRegistry.getInstance();
		reg.register(ModParticles.NUKE_FIRE, sprites -> {
			fireSprites = sprites;
			return factory(sprites, NUKE_FIRE);
		});
		reg.register(ModParticles.NUKE_SMOKE, sprites -> {
			smokeSprites = sprites;
			return factory(sprites, NUKE_SMOKE);
		});
		reg.register(ModParticles.SHOCK_DUST, sprites -> {
			dustSprites = sprites;
			return factory(sprites, SHOCK_DUST);
		});
		reg.register(ModParticles.PLASMA_SPARK, sprites -> {
			glowSprites = sprites;
			return factory(sprites, PLASMA_SPARK);
		});
		reg.register(ModParticles.ARC_SPARK, sprites -> factory(sprites, ARC_SPARK));
		reg.register(ModParticles.RADIATION, sprites -> factory(sprites, RADIATION));
		reg.register(ModParticles.CRYO_MIST, sprites -> factory(sprites, CRYO_MIST));
		reg.register(ModParticles.VOID_SPARK, sprites -> factory(sprites, VOID_SPARK));
		reg.register(ModParticles.NAPALM_FLAME, sprites -> factory(sprites, NAPALM_FLAME));
		reg.register(ModParticles.EMBER, sprites -> factory(sprites, EMBER));
		reg.register(ModParticles.TOXIC_GAS, sprites -> factory(sprites, TOXIC_GAS));
		reg.register(ModParticles.MUZZLE_FLASH, sprites -> factory(sprites, MUZZLE_FLASH));
	}

	private static ParticleFactory<SimpleParticleType> factory(SpriteProvider sprites, WmdParticle.Preset preset) {
		return (type, world, x, y, z, vx, vy, vz) -> new WmdParticle(world, x, y, z, vx, vy, vz, sprites, preset, 1.0F);
	}

	private ModParticleFactories() {
	}
}
