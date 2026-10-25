package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;

/** Signature effects for Galactic Smash, Booming Tick and Alien Death Ray. {@code p.seed()} picks the effect. */
public final class WorldFx {
	private WorldFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			default -> null;
		};
	}
}
