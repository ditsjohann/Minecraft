package com.armageddon.client.fx;

import com.armageddon.network.FxPayload;
import com.armageddon.util.CelestialTimeline;

/** Signature effects for Celestial I. {@code p.seed()} picks the effect. */
public final class CelestialFx {
	private CelestialFx() {
	}

	public static FxEffect create(FxPayload p) {
		return switch (p.seed()) {
			case CelestialTimeline.FX_LAST_LIGHT -> new CelestialLastLightFx(p);
			case CelestialTimeline.FX_ZEROPOINT -> new CelestialZeropointFx(p);
			case CelestialTimeline.FX_UNMAKER -> new CelestialUnmakerFx(p);
			case CelestialTimeline.FX_SPEAR -> new CelestialSpearFx(p);
			case CelestialTimeline.FX_TEMPLE -> new CelestialTempleFx(p);
			case CelestialTimeline.FX_SHATTER -> new CelestialShatterFx(p);
			default -> null;
		};
	}
}
