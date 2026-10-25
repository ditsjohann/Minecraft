package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;

/** Signature effects for City Destruction. {@code p.seed()} picks the effect. */
public final class CityFx {
	private CityFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			default -> null;
		};
	}
}
