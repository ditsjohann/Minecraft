package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;

/** Signature effects for Celestial II. {@code p.seed()} picks the effect. */
public final class CelestialIIFx {
	private CelestialIIFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			default -> null;
		};
	}
}
