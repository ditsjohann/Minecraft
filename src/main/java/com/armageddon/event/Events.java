package com.armageddon.event;

import com.armageddon.event.events.BombEvents;
import com.armageddon.event.events.CelestialEvents;
import com.armageddon.event.events.CelestialIIEvents;
import com.armageddon.event.events.WorldCutsceneEvents;
import com.armageddon.event.events.EternalSaplingEvent;
import com.armageddon.event.events.SixteenChunksEvent;
import com.armageddon.event.events.MainStageEvent;
import com.armageddon.event.events.DiscoBallEvent;
import com.armageddon.event.events.OddBombEvents;
import com.armageddon.event.events.StrongBombEvents;
import com.armageddon.event.events.CityDestructionEvents;
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
		EVENTS.put("moonfall", CityDestructionEvents.Moonfall::new);
		EVENTS.put("big_bang", CityDestructionEvents.BigBang::new);
		EVENTS.put("world_eater", CityDestructionEvents.WorldEater::new);
		EVENTS.put("chrono_break", CityDestructionEvents.ChronoBreak::new);
		EVENTS.put("ragnarok", CityDestructionEvents.Ragnarok::new);
		// Celestial expansion II
		EVENTS.put("exploding_cows", CelestialIIEvents.ExplodingCows::new);
		EVENTS.put("deadly_twister", CelestialIIEvents.DeadlyTwister::new);
		EVENTS.put("colossal_spear", CelestialIIEvents.ColossalSpear::new);
		EVENTS.put("zombie_apocalypse", CelestialIIEvents.ZombieApocalypse::new);
		EVENTS.put("ufo_invasion", CelestialIIEvents.UfoInvasion::new);
		// True world destruction
		EVENTS.put("galactic_smash", WorldCutsceneEvents.GalacticSmash::new);
		EVENTS.put("booming_tick", WorldCutsceneEvents.BoomingTick::new);
		EVENTS.put("alien_death_ray", WorldCutsceneEvents.AlienDeathRay::new);
		EVENTS.put("eternal_sapling", EternalSaplingEvent::new);
		EVENTS.put("sixteen_chunks", SixteenChunksEvent::new);
		EVENTS.put("truman_show", MainStageEvent::new);
		EVENTS.put("disco_ball", DiscoBallEvent::new);
		// The strong five and the oddities
		EVENTS.put("lidar_bomb", StrongBombEvents.LidarStrike::new);
		EVENTS.put("shadow_void", StrongBombEvents.ShadowVoid::new);
		EVENTS.put("spectral_ice", StrongBombEvents.SpectralIce::new);
		EVENTS.put("termite_domain", StrongBombEvents.TermiteDomain::new);
		EVENTS.put("sun_disappears", StrongBombEvents.SunDisappears::new);
		EVENTS.put("sonar_bomb", OddBombEvents.Sonar::new);
		EVENTS.put("pixel_bomb", OddBombEvents.EightBit::new);
		EVENTS.put("magnetar", OddBombEvents.Magnetar::new);
		EVENTS.put("ink_bomb", OddBombEvents.KaBoom::new);
		EVENTS.put("aurora_bomb", OddBombEvents.Aurora::new);
		EVENTS.put("seasons_bomb", OddBombEvents.Century::new);
		EVENTS.put("prism_bomb", OddBombEvents.PrismBomb::new);
		EVENTS.put("geode_bomb", OddBombEvents.GeodeBomb::new);
		EVENTS.put("fossil_bomb", OddBombEvents.Fossil::new);
		EVENTS.put("lava_lamp", OddBombEvents.LavaLamp::new);
		EVENTS.put("ion_scythe", com.armageddon.event.events.SciFiEvents.IonScythe::new);
		EVENTS.put("helix_bolt", com.armageddon.event.events.SciFiEvents.HelixBolt::new);
		EVENTS.put("graviton", com.armageddon.event.events.SciFiEvents.Graviton::new);
		EVENTS.put("sunfall", com.armageddon.event.events.SciFiEvents.Sunfall::new);
		EVENTS.put("halo_cut", com.armageddon.event.events.SciFiEvents.HaloCut::new);
		EVENTS.put("resonance_cascade", com.armageddon.event.events.SciFiEvents.ResonanceCascade::new);
		EVENTS.put("tesla_dominion", com.armageddon.event.events.SciFiEvents.TeslaDominion::new);
		EVENTS.put("dyson_lance", com.armageddon.event.events.SciFiEvents.DysonLance::new);
		EVENTS.put("gravity_killswitch", com.armageddon.event.events.SciFiEvents.GravityKillswitch::new);
		EVENTS.put("seismic_tsunami", com.armageddon.event.events.SciFiEvents.SeismicTsunami::new);
		EVENTS.put("goo_bomb", com.armageddon.event.events.WildEvents.GooBomb::new);
		EVENTS.put("hyperbeam", com.armageddon.event.events.WildEvents.Hyperbeam::new);
		EVENTS.put("cactus_bomb", com.armageddon.event.events.WildEvents.CactusBomb::new);
		EVENTS.put("blackforge", com.armageddon.event.events.WildEvents.Blackforge::new);
		EVENTS.put("plasma_koi", com.armageddon.event.events.WildEvents.PlasmaKoi::new);
		EVENTS.put("blue_infrit", com.armageddon.event.events.WildEvents.BlueInfrit::new);
		// Celestial
		EVENTS.put("last_light", CelestialEvents.LastLight::new);
		EVENTS.put("zeropoint", CelestialEvents.Zeropoint::new);
		EVENTS.put("unmaker", CelestialEvents.Unmaker::new);
		EVENTS.put("godfall", CelestialEvents.Godfall::new);
		EVENTS.put("final_existence", CelestialEvents.FinalExistence::new);
	}

	public static java.util.Set<String> ids() {
		return java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(EVENTS.keySet()));
	}

	@Nullable
	public static WorldEvent create(String id) {
		Supplier<WorldEvent> s = EVENTS.get(id);
		return s == null ? null : s.get();
	}

	private Events() {
	}
}
