package com.armageddon.client.fx;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** A transient client-side visual. Positions are absolute world coordinates. */
public abstract class FxEffect {
	protected int age;
	protected final int lifetime;

	protected FxEffect(int lifetime) {
		this.lifetime = lifetime;
	}

	/** @return false once finished. */
	public boolean tick(ClientWorld world) {
		age++;
		return age < lifetime;
	}

	/** 0..1 progress including partial tick. */
	protected float progress(float tickDelta) {
		return Math.min(1.0F, (age + tickDelta) / lifetime);
	}

	/**
	 * @param m   pose matrix (camera-relative world space)
	 * @param cam camera position — subtract from world coords before emitting vertices
	 */
	public abstract void render(Matrix4f m, VertexConsumerProvider consumers, Vec3d cam, float tickDelta, float camYaw, float camPitch);
}
