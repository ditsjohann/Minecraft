package com.armageddon.client.mixin;

import com.armageddon.client.fx.ClientFx;
import com.armageddon.client.fx.Cinematics;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screen shake from shockwaves and heavy recoil. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	public abstract float getYaw();

	@Shadow
	public abstract float getPitch();

	@Shadow
	protected abstract void setPos(Vec3d pos);

	@Shadow
	private boolean thirdPerson;

	/** Scripted cinematics take over the camera entirely. */
	@Inject(method = "update", at = @At("TAIL"))
	private void armageddon$cinematic(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		Cinematics.Cine cine = ClientFx.activeCine();
		if (cine == null) {
			return;
		}
		try {
			Cinematics.State s = cine.frame(tickDelta);
			this.setPos(s.pos);
			this.setRotation(s.yaw, s.pitch);
			this.thirdPerson = true;
		} catch (RuntimeException ex) {
			com.armageddon.Armageddon.LOGGER.error("Armageddon cinematic camera failed; ending the cutscene", ex);
			cine.done = true;
		}
	}

	@Inject(method = "update", at = @At("TAIL"))
	private void armageddon$shake(CallbackInfo ci) {
		float s = ClientFx.shake;
		if (s <= 0.0F) {
			return;
		}
		double t = System.nanoTime() / 1.0E9;
		float dx = (float) (Math.sin(t * 47.0) + Math.sin(t * 89.0) * 0.5) * s;
		float dy = (float) (Math.cos(t * 53.0) + Math.sin(t * 71.0) * 0.5) * s;
		this.setRotation(this.getYaw() + dx, MathHelper.clamp(this.getPitch() + dy, -90.0F, 90.0F));
	}
}
