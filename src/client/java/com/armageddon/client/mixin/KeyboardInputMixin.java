package com.armageddon.client.mixin;

import com.armageddon.client.fx.ClientFx;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While a cinematic owns the camera, the player stands still. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void armageddon$lock(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		if (ClientFx.activeCine() == null) {
			return;
		}
		Input input = (Input) (Object) this;
		input.movementForward = 0;
		input.movementSideways = 0;
		input.pressingForward = input.pressingBack = input.pressingLeft = input.pressingRight = false;
		input.jumping = false;
		input.sneaking = false;
	}
}
