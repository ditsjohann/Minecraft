package com.armageddon.client;

import com.armageddon.Armageddon;
import com.armageddon.client.fx.ClientFx;
import com.armageddon.client.fx.FxWorldRenderer;
import com.armageddon.client.fx.PostFx;
import com.armageddon.client.hud.WmdHud;
import com.armageddon.client.particle.ModParticleFactories;
import com.armageddon.client.render.BlackHoleRenderer;
import com.armageddon.client.render.EnergyBoltRenderer;
import com.armageddon.client.render.GrenadeRenderer;
import com.armageddon.client.render.PrimedBombRenderer;
import com.armageddon.client.render.ProjectileModelRenderer;
import com.armageddon.client.render.StarRenderer;
import com.armageddon.network.FxPayload;
import com.armageddon.registry.ModBlocks;
import com.armageddon.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class ArmageddonClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModParticleFactories.register();
		com.armageddon.client.vfx.VfxLibrary.bootstrap();
		com.armageddon.entity.WmdProjectileEntity.clientTrail = com.armageddon.client.vfx.WeaponVfx::trail;

		EntityRendererRegistry.register(ModEntities.PROJECTILE, ProjectileModelRenderer::new);
		EntityRendererRegistry.register(ModEntities.ENERGY_BOLT, EnergyBoltRenderer::new);
		EntityRendererRegistry.register(ModEntities.GRENADE, GrenadeRenderer::new);
		EntityRendererRegistry.register(ModEntities.PRIMED_BOMB, PrimedBombRenderer::new);
		EntityRendererRegistry.register(ModEntities.BLACK_HOLE, BlackHoleRenderer::new);
		EntityRendererRegistry.register(ModEntities.FALLOUT_CLOUD, EmptyEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.STRIKE_MARKER, EmptyEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.STAR, StarRenderer::new);
		EntityRendererRegistry.register(ModEntities.DEVOURER, com.armageddon.client.render.DevourerRenderer::new);
		EntityRendererRegistry.register(ModEntities.WORLD_EVENT, EmptyEntityRenderer::new);

		ModelLoadingPlugin.register(ctx -> ctx.addModels(
				ProjectileModelRenderer.ROCKET_MODEL, ProjectileModelRenderer.MINI_NUKE_MODEL, ProjectileModelRenderer.BOMBLET_MODEL,
				ProjectileModelRenderer.METEOR_MODEL, ProjectileModelRenderer.ROD_MODEL));

		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), ModBlocks.BOMBS.values().toArray(new Block[0]));
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), ModBlocks.ENERGY_CRYSTAL, ModBlocks.LUMINOUS_GLASS);

		ClientPlayNetworking.registerGlobalReceiver(FxPayload.ID, (payload, context) -> ClientFx.handle(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.armageddon.network.VfxPayload.ID, (payload, context) -> com.armageddon.client.vfx.FxEngine.handle(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientFx.clear();
			com.armageddon.client.vfx.FxEngine.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::tick);
		WorldRenderEvents.AFTER_ENTITIES.register(FxWorldRenderer::renderEarly);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FxWorldRenderer::render);
		// The effects' own program. If it ever fails to load, effects fall back to the vanilla textured program.
		net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(ctx -> {
			try {
				ctx.register(com.armageddon.Armageddon.id("fx"), net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR,
						com.armageddon.client.render.FxSprites::setProgram);
			} catch (java.io.IOException | RuntimeException e) {
				com.armageddon.client.render.FxSprites.setProgram(null);
				com.armageddon.Armageddon.LOGGER.error("Armageddon effect shader failed to load; using the fallback", e);
			}
		});
		WorldRenderEvents.LAST.register(com.armageddon.client.fx.Vision::render);
		WorldRenderEvents.LAST.register(PostFx::updateLens);
		HudRenderCallback.EVENT.register(WmdHud::render);

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return Armageddon.id("post_fx");
			}

			@Override
			public void reload(ResourceManager manager) {
				PostFx.invalidate();
				com.armageddon.client.fx.Vision.invalidate();
			}
		});
	}
}
