package com.armageddon.event;

import com.armageddon.event.events.BombEvents;
import com.armageddon.event.events.CelestialEvents;
import com.armageddon.event.events.WorldDestructionEvents;
import com.armageddon.event.events.EpicEvents;
import com.armageddon.event.events.LegendaryEvents;
import com.armageddon.event.events.MythicEvents;
import com.armageddon.event.events.RareEvents;
import com.armageddon.event.events.UncommonEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registry of scripted world events by id. */
public final class Events {
	private static final Map<String, Supplier<WorldEvent>> EVENTS = new HashMap<>();

	static {
		// Uncommon
		EVENTS.put("party", UncommonEvents.Party::new);
		EVENTS.put("bounce", UncommonEvents.Bounce::new);
		EVENTS.put("loot_tornado", UncommonEvents.LootTornado::new);
		EVENTS.put("moonwalk", UncommonEvents.Moonwalk::new);
		EVENTS.put("snow_globe", UncommonEvents.SnowGlobe::new);
		EVENTS.put("slimequake", UncommonEvents.Slimequake::new);
		EVENTS.put("hologram", UncommonEvents.Hologram::new);
		EVENTS.put("magnet", UncommonEvents.Magnet::new);
		EVENTS.put("paint", UncommonEvents.Paint::new);
		// Rare
		EVENTS.put("shatterfrost", RareEvents.Shatterfrost::new);
		EVENTS.put("arcflash", RareEvents.Arcflash::new);
		EVENTS.put("seismic_pulse", RareEvents.SeismicPulse::new);
		EVENTS.put("implosion", RareEvents.Implosion::new);
		EVENTS.put("microstrike", RareEvents.Microstrike::new);
		EVENTS.put("tether", RareEvents.Tether::new);
		EVENTS.put("ion_fog", RareEvents.IonFog::new);
		EVENTS.put("stasis", RareEvents.Stasis::new);
		EVENTS.put("emp", RareEvents.Emp::new);
		// Epic
		EVENTS.put("cataclysm", EpicEvents.Cataclysm::new);
		EVENTS.put("quantum_fracture", EpicEvents.QuantumFracture::new);
		EVENTS.put("crystals", EpicEvents.Crystals::new);
		EVENTS.put("voidstorm", EpicEvents.Voidstorm::new);
		EVENTS.put("mirror", EpicEvents.Mirror::new);
		// Legendary
		EVENTS.put("helios_crown", LegendaryEvents.HeliosCrown::new);
		EVENTS.put("worldsplit", LegendaryEvents.Worldsplit::new);
		EVENTS.put("solar_verdict", LegendaryEvents.SolarVerdict::new);
		EVENTS.put("titanfall", LegendaryEvents.Titanfall::new);
		EVENTS.put("reality_shear", LegendaryEvents.RealityShear::new);
		EVENTS.put("continental", LegendaryEvents.Continental::new);
		EVENTS.put("dawnstar", LegendaryEvents.Dawnstar::new);
		// Mythic
		EVENTS.put("bloodstar", MythicEvents.Bloodstar::new);
		EVENTS.put("riftfall", MythicEvents.Riftfall::new);
		EVENTS.put("spindle", MythicEvents.Spindle::new);
		EVENTS.put("extinction", MythicEvents.Extinction::new);
		// Bomb expansion II
		EVENTS.put("disco", BombEvents.Disco::new);
		EVENTS.put("glitter", BombEvents.Glitter::new);
		EVENTS.put("pumpkin_patch", BombEvents.PumpkinPatch::new);
		EVENTS.put("cobweb", BombEvents.Cobweb::new);
		EVENTS.put("balloon", BombEvents.Balloon::new);
		EVENTS.put("sprinkler", BombEvents.Sprinkler::new);
		EVENTS.put("thermite", BombEvents.Thermite::new);
		EVENTS.put("tesla", BombEvents.Tesla::new);
		EVENTS.put("sinkhole", BombEvents.Sinkhole::new);
		EVENTS.put("tsunami", BombEvents.Tsunami::new);
		EVENTS.put("volcano", BombEvents.Volcano::new);
		EVENTS.put("ice_age", BombEvents.IceAge::new);
		EVENTS.put("hellmouth", BombEvents.Hellmouth::new);
		EVENTS.put("cyclone", BombEvents.Cyclone::new);
		EVENTS.put("doomsday_clock", BombEvents.DoomsdayClock::new);
		// World destruction
		EVENTS.put("moonfall", WorldDestructionEvents.Moonfall::new);
		EVENTS.put("big_bang", WorldDestructionEvents.BigBang::new);
		EVENTS.put("world_eater", WorldDestructionEvents.WorldEater::new);
		EVENTS.put("chrono_break", WorldDestructionEvents.ChronoBreak::new);
		EVENTS.put("ragnarok", WorldDestructionEvents.Ragnarok::new);
		// Celestial
		EVENTS.put("last_light", CelestialEvents.LastLight::new);
		EVENTS.put("zeropoint", CelestialEvents.Zeropoint::new);
		EVENTS.put("unmaker", CelestialEvents.Unmaker::new);
		EVENTS.put("godfall", CelestialEvents.Godfall::new);
		EVENTS.put("final_existence", CelestialEvents.FinalExistence::new);
	}

	@Nullable
	public static WorldEvent create(String id) {
		Supplier<WorldEvent> s = EVENTS.get(id);
		return s == null ? null : s.get();
	}

	private Events() {
	}
}
