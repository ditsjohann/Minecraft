package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;

/** Signature effects for Celestial I. {@code p.seed()} picks the effect. */
public final class CelestialFx {
	private CelestialFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			default -> null;
		};
	}
}
