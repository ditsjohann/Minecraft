"""Lang, recipes, loot, tags and block assets for the arsenal expansion."""
import os

import tex_arsenal as X

NAMES = {
    # support
    "solar_cell": "Solar Cell", "rift_shard": "Rift Shard", "celestial_core": "Celestial Core", "warden_wand": "Warden Wand",
    # uncommon
    "party_popper": "Party Popper Rocket", "bounce_bomb": "Bounce Bomb", "loot_tornado": "Loot Tornado",
    "moonwalk_mine": "Moonwalk Mine", "snow_globe_grenade": "Snow Globe Grenade", "slimequake_bomb": "Slimequake Bomb",
    "hologram_bomb": "Hologram Bomb", "magnet_mayhem": "Magnet Mayhem", "airhorn_cannon": "Airhorn Cannon",
    "rainbow_paint_bomb": "Rainbow Paint Bomb",
    # rare
    "shatterfrost_bomb": "Shatterfrost Bomb", "arcflash_charge": "Arcflash Charge", "seismic_pulse_mine": "Seismic Pulse Mine",
    "implosion_grenade": "Implosion Grenade", "microstrike_beacon": "Microstrike Beacon", "plasma_boomerang": "Plasma Boomerang",
    "gravity_tether_gun": "Gravity Tether Gun", "ion_fog_canister": "Ion Fog Canister", "stasis_mine": "Stasis Mine",
    "emp_disruptor": "EMP Disruptor",
    # epic
    "antimatter_lance": "Antimatter Lance", "cataclysm_mortar": "Cataclysm Mortar", "quantum_fracture_bomb": "Quantum Fracture Bomb",
    "rift_borer": "Rift Borer", "crystal_cataclysm": "Crystal Cataclysm", "voidstorm_generator": "Voidstorm Generator",
    "siegebreaker_array": "Siegebreaker Array", "micro_event_horizon": "Micro Event Horizon", "resonance_cannon": "Resonance Cannon",
    "mirror_dimension_bomb": "Mirror Dimension Bomb",
    # legendary
    "solstice_sunlance": "Solstice Sunlance", "worldsplitter_hammer": "Worldsplitter Hammer",
    "solar_verdict_beacon": "Solar Verdict Beacon", "titanfall_warhead": "Titanfall Warhead", "midas_cascade": "Midas Cascade",
    "reality_shear_projector": "Reality Shear Projector", "judgment_engine": "Judgment Engine",
    # mythic
    "riftfall_engine": "Riftfall Engine", "apocalypse_spindle": "Apocalypse Spindle", "devourer_core": "Devourer Core",
    "extinction_choir": "Extinction Choir",
    # plasma and sci-fi (cataclysm tiers)
    "helix_disruptor": "Helix Disruptor", "graviton_lance": "Graviton Lance", "sunfall_mortar": "Sunfall Mortar",
    "halo_cutter": "Halo Cutter", "obliterator": "Obliterator",
    "hyperbeam_raygun": "Hyperbeam Raygun",
}

BLOCKS = {
    "helios_crown_bomb": "Helios Crown Bomb", "continental_pulse": "Continental Pulse", "dawnstar_payload": "Dawnstar Payload",
    "bloodstar_collapse": "Bloodstar Collapse", "last_light": "THE LAST LIGHT", "zeropoint": "ZEROPOINT ABSOLUTE",
    "unmaker": "THE UNMAKER", "godfall": "GODFALL PROTOCOL", "final_existence": "OMEGA: FINAL EXISTENCE",
    "energy_crystal": "Energy Crystal", "luminous_glass": "Luminous Glass", "gold_fissure": "Molten Gold Fissure",
    "crimson_fissure": "Crimson Fissure", "void_remnant": "Void Remnant", "molten_core": "Molten Core",
}

DESC = {
    "solar_cell": ["Bottled noon. Ammo for the Sunlance and Judgment Engine."],
    "rift_shard": ["A splinter of somewhere else."],
    "celestial_core": ["It hums in a key that doesn't exist."],
    "warden_wand": ["Click two corners: nothing in this mod can damage what's between them.", "Operators only. See /armageddon protect."],
    "party_popper": ["Fires spiralling starbursts, smileys and a heart finale.", "Launches everyone gently skyward. Ammo: firework rockets."],
    "bounce_bomb": ["A huge bubble that boings everything upward. Again. And again."],
    "loot_tornado": ["Whirls dropped items into a glittering funnel, then rains them on the thrower."],
    "moonwalk_mine": ["Low gravity zone. Everyone floats, nobody falls."],
    "snow_globe_grenade": ["Traps the area in a snowy dome. Snow melts away afterwards."],
    "slimequake_bomb": ["The ground turns to bouncing slime for ten seconds."],
    "hologram_bomb": ["Projects flickering decoys that distract mobs."],
    "magnet_mayhem": ["Yanks items, arrows and loose metal into a spinning ball, then lets go."],
    "airhorn_cannon": ["Honk. Pushes a cone of everything away.", "Sneak for a deeper, wider blast."],
    "rainbow_paint_bomb": ["Splatters everything with rainbow paint (cosmetic).", "Sneak-throw for solvent that washes paint away."],
    "shatterfrost_bomb": ["Frost builds, an ice shell grows... then shatters outward."],
    "arcflash_charge": ["Sticky. Branching arcs leap between creatures and metal blocks."],
    "seismic_pulse_mine": ["A ground-hugging circular fissure that knocks everything up."],
    "implosion_grenade": ["Pulls everything into a tight sphere — then throws it all out."],
    "microstrike_beacon": ["Six precision strikes that follow your crosshair."],
    "plasma_boomerang": ["A spinning plasma blade that slices through and flies home."],
    "gravity_tether_gun": ["Yank a creature to you, or grapple yourself to a wall."],
    "ion_fog_canister": ["Luminous fog that outlines everything and blinds hostile targeting."],
    "stasis_mine": ["A hexagonal cage that freezes creatures and projectiles mid-air."],
    "emp_disruptor": ["Lamps die, redstone stalls, guns jam, creepers defuse.", "Builds are untouched. Everything comes back."],
    "antimatter_lance": ["Charge, release: a line of nothing. The boom arrives late."],
    "cataclysm_mortar": ["High-arc purple shell: crater, molten pool, crystal shards, three rings."],
    "quantum_fracture_bomb": ["Terrain splits into glitching fragments that hover... and vanish."],
    "rift_borer": ["Hold to bore perfectly round tunnels through the world."],
    "crystal_cataclysm": ["Energy crystal spires erupt, hum, and detonate one by one."],
    "voidstorm_generator": ["A contained storm of gravity pulses, dark lightning and orbiting rubble."],
    "siegebreaker_array": ["Six missiles rise behind you and converge on the target."],
    "micro_event_horizon": ["Small. Hungry. Short-lived."],
    "resonance_cannon": ["Shatters glass and ice, sets off explosives at range.", "Ammo: amethyst shards."],
    "mirror_dimension_bomb": ["Half the area phases into the mirror world for a minute. Creatures swap sides."],
    "solstice_sunlance": ["Three seconds of gathered sunlight, then a molten line through the world."],
    "helix_disruptor": ["Charge, release: two plasma bolts corkscrew around each other,", "boring a spiral tunnel through anything for 140 blocks, then implode."],
    "graviton_lance": ["Plants a gravity anchor where you aim.", "Everything around it is dragged in and crushed into a crying-obsidian core."],
    "sunfall_mortar": ["Lobs a small plasma sun in a high arc.", "Where it lands, the ground melts into a pool of lava."],
    "halo_cutter": ["Charge to paint a target: an orbital beam draws a ring canyon round it,", "then the whole disc inside drops out of the world."],
    "obliterator": ["Hold: a beam that takes apart everything it touches and keeps going.", "Ammo: antimatter cells."],
    "hyperbeam_raygun": ["Hold for three seconds while it charges (you can barely walk),", "then release: a beam as thick as a house pours out for six seconds, tunnelling nearly 100 blocks.", "Ammo: plasma cells."],
    "worldsplitter_hammer": ["Right-click the ground: eight fissures, lifted earth, a second crushing wave."],
    "solar_verdict_beacon": ["A public five-second countdown. Then nine beams of judgement."],
    "titanfall_warhead": ["Calls a heavy warhead from orbit along your line of sight.", "Delayed secondary detonation."],
    "midas_cascade": ["Splits into escalating golden spheres. The last one glasses the ground gold."],
    "reality_shear_projector": ["A cutting plane through the world. Everything on one side is erased.", "Use twice to confirm."],
    "judgment_engine": ["Golden rotary plasma cannon. Hotter = faster = harder.", "At 100% heat it vents and locks."],
    "riftfall_engine": ["Four sky rifts pour beams, meteors and warheads, then fold shut.", "Use twice to confirm."],
    "apocalypse_spindle": ["Drills a molten shaft, sweeps a rotating trench, then pulses.", "Use twice to confirm."],
    "devourer_core": ["Throws a crimson singularity that crawls forward eating the world."],
    "extinction_choir": ["The sky cracks. Four meteor waves sing down. Then the big one.", "Use twice to confirm."],
}

NEW_BOMBS = {
    "disco_bomb": "Disco Bomb", "glitter_bomb": "Glitter Bomb", "pumpkin_patch_bomb": "Pumpkin Patch Bomb",
    "cobweb_bomb": "Cobweb Bomb", "balloon_bomb": "Balloon Bomb", "sprinkler_bomb": "Sprinkler Bomb",
    "thermite_bomb": "Thermite Bomb", "tesla_bomb": "Tesla Bomb", "sinkhole_bomb": "Sinkhole Bomb",
    "tsunami_bomb": "Tsunami Bomb", "volcano_bomb": "Volcano Bomb", "ice_age_bomb": "Ice Age Bomb",
    "hellmouth_bomb": "Hellmouth", "cyclone_bomb": "Cyclone Bomb", "doomsday_clock": "Doomsday Clock",
    "moonfall": "MOONFALL", "big_bang": "BIG BANG", "world_eater": "WORLD EATER",
    "chrono_break": "CHRONO BREAK", "ragnarok": "RAGNAROK",
}
NEW_BOMBS.update({
    "exploding_cows": "EXPLODING COWS", "deadly_twister": "DEADLY TWISTER", "colossal_spear": "COLOSSAL SPEAR",
    "zombie_apocalypse": "ZOMBIE APOCALYPSE", "ufo_invasion": "UFO INVASION",
    "galactic_smash": "GALACTIC SMASH", "booming_tick": "BOOMING TICK", "alien_death_ray": "ALIEN DEATH RAY",
    "eternal_sapling": "ETERNAL SAPLING", "sixteen_chunks": "16 CHUNKS", "truman_show": "MAIN STAGE",
    "disco_ball": "DISCO BALL",
    "lidar_bomb": "LIDAR STRIKE", "shadow_void": "SHADOW VOID", "spectral_ice": "SPECTRAL ICE",
    "termite_domain": "TERMITE DOMAIN", "sun_disappears": "THE SUN DISAPPEARS",
    "sonar_bomb": "Sonar Bomb", "pixel_bomb": "8-Bit Bomb", "magnetar": "Magnetar", "ink_bomb": "KA-BOOM!",
    "aurora_bomb": "Aurora Bomb", "seasons_bomb": "Century Bomb", "prism_bomb": "Prism Bomb", "geode_bomb": "Geode Bomb",
    "fossil_bomb": "Fossil Bomb", "lava_lamp": "Lava Lamp Bomb",
    "ion_scythe": "ION SCYTHE", "resonance_cascade": "RESONANCE CASCADE", "tesla_dominion": "TESLA DOMINION",
    "dyson_lance": "DYSON LANCE", "gravity_killswitch": "GRAVITY KILLSWITCH", "seismic_tsunami": "SEISMIC TSUNAMI",
    "goo_bomb": "Goo Bomb", "cactus_bomb": "Cactus Bomb", "blackforge": "BLACKFORGE", "blue_infrit": "BLUE INFRIT",
    "plasma_koi": "PLASMA KOI",
    "dimension_shift": "DIMENSION SHIFT", "firework_havoc": "Firework Havoc", "chinese_lantern": "Chinese Lantern",
    "mammoth_fossil": "MAMMOTH FOSSIL", "leviathan_fossil": "LEVIATHAN FOSSIL", "pterosaur_fossil": "Pterosaur Fossil",
    "xenoform": "XENOFORM",
    "terror_maniac": "TERROR MANIAC", "pumpkin_horseman": "PUMPKIN HORSEMAN", "portal_of_hell": "PORTAL OF HELL",
    "ai_takeover": "AI TAKEOVER",
})
BLOCKS.update(NEW_BOMBS)

BLOCK_DESC = {
    "ai_takeover": "CITY DESTRUCTION. A black core erupts and opens a red eye. Circuit traces race across the streets and assimilate everything they reach, blacking out the city. Drones pour out and hunt with lasers. Then the eye goes white, alarms sound, and laser walls slice the city into blocks. The core uploads itself into the sky and an EMP ends it. Light twice.",
    "portal_of_hell": "CELESTIAL. The ground quakes and a gothic gate of blackstone the height of a cathedral heaves up out of the earth. Its arch ignites into a vortex of hellfire with an eye in it that watches you. Souls and fireballs pour out, lava falls from its sill, things come through, and a clawed arm reaches out and slams the ground. Then the portal implodes, leaving the gate cracked over scorched land. Light twice.",
    "pumpkin_horseman": "CELESTIAL. Night falls orange and violet, and out of a geyser of ghost fire gallops a headless rider the height of a tower, scythe in one hand and a blazing jack-o'-lantern in the other. He circles the land leaving burning hoofprints, hurls the lantern six times, then charges straight through with his scythe and rides into the sky, leaving the land hallowed. Light twice.",
    "terror_maniac": "CELESTIAL. The colour drains from the world, the sky turns to old blood, and black veins crawl out pulsing with a heartbeat. A thing of roots with a cracked white mask rises from a rift, weeping blood under a blood moon, lifts everything living into the air bleeding while a bell tolls and blood rains, and then bursts them. It shrieks, bursts into a storm of moths, and the land rots. Light twice.",
    "xenoform": "WORLD DESTRUCTION. Technology five thousand years past ours. No explosion: a hypercube unfolds over the land, scans it, then takes it apart block by block and sorts it by colour into rings, a catalogue of the place. Everything living goes up with it. It leaves a flat black plain ruled with light, a monolith, and a column of light leaving for the sky.",
    "mammoth_fossil": "A frozen fissure opens and a woolly mammoth's skeleton rises out of the permafrost, tusks curling. Its eyes catch a cold blue light and it trumpets: an ice age rolls out over the land, freezing water, burying it in snow and bursting spikes of ice out of the ground. It stomps the frozen ground to pieces, trumpets a last time as lines of giant ice spikes race out across the land, and a glacier closes over it, leaving it entombed in ice. Light twice.",
    "leviathan_fossil": "The ancient sea floods back over the land as a ghost, with jellyfish and shoals drifting through it, and the fossil of a sea serpent breaches out of the ground in great arches. Its skull rears up with its jaws wide, its eyes light, and the whole ghost sea gathers into one great curling wave that rolls out across the land, sweeping everything before it and leaving a sea floor of sand, clay and dead coral. Light twice.",
    "pterosaur_fossil": "Bones burst out of the ground and assemble in the air into a pterosaur thirty blocks across, with fire in its eyes and the ghost of its wings between the bones. It circles high and dives three times, screeching, blasting the ground where it pulls up. Then it lands on a spire of bone and turns to stone, leaving its fossil perched there.",
    "dimension_shift": "A rift tears open in the air and a wave runs out from it, shifting everything it passes into the Nether: nylium and netherrack, soul sand, crimson and warped trees, lava where the water was, a red sky and falling ash. Then the locals arrive. Light twice.",
    "firework_havoc": "One firework the size of a tower climbs screaming into the sky and bursts, setting off a cascade of fireworks at every angle while fountains and roman candles go off all over the ground. Ends in a golden willow.",
    "chinese_lantern": "Hundreds of paper lanterns rise and fill the sky while a golden dragon flies slowly round over them to a flute tune, and at the end climbs and bursts into gold. Harmless.",
    "plasma_koi": "A pond of liquid light opens in the ground and seven koi made of plasma circle beneath its surface. One by one they leap out on wilder and wilder arcs, scorching wherever they land. Then they leap together, braid into a dragon, and dive back in: the pond erupts and plasma rains on the land. Light twice.",
    "goo_bomb": "An alien goo oozes out and swells, throbbing faster and faster, eating into the ground, until it bursts. Acid globs fly everywhere, and whatever they land on turns to goo and dissolves.",
    "cactus_bomb": "A cactus the size of a tower grows out of the ground and spins faster and faster until its spikes tear off and fly in every direction, blowing holes wherever they land. Then it blooms, and a red sandstorm turns the land to desert.",
    "blackforge": "Anvils start falling out of the sky: a few, then more, faster and faster, until it pours anvils. Then a hammer as tall as a tower slams the ground: every anvil turns to iron, and the land around breaks into tectonic plates that heave and sink. Light twice.",
    "blue_infrit": "CELESTIAL. Blue fire draws a summoning circle into the ground. Pillars of it rise, the circle becomes a vortex, and a hound's head of blue flame bursts out and dives on the land six times, leaving burning pits of lava. Then blue fire sweeps the country. Light twice.",
    "ion_scythe": "An orbital ion cannon cuts a spiral trench of molten glass through everything, charges it, then ignites the whole spiral at once. Light twice.",
    "resonance_cascade": "CELESTIAL. A tuning fork hums until it finds the town's frequency; then every building, wall and tree shakes itself apart into rubble and the ground goes to mud. Light twice.",
    "tesla_dominion": "CELESTIAL. A tesla tower rises and arcs at everything tall; the lightning chains through buildings, blowing them apart, and ploughs furrows where it earths. Then the ball lightning goes. Light twice.",
    "dyson_lance": "WORLD DESTRUCTION. A swarm of orbital mirrors focuses the sun into one beam that cuts three canyons across the map into a six-pointed star, then drills the centre. Camera cutscene. Light twice.",
    "gravity_killswitch": "CELESTIAL. Gravity switches off: the town floats up and tumbles. Then it comes back many times over and slams everything down. Camera cutscene. Light twice.",
    "seismic_tsunami": "WORLD DESTRUCTION. A piston hammer slams the ground until the land itself becomes a wave, rolling out and scraping everything down to bare stone. Camera cutscene. Light twice.",
    "lidar_bomb": "The world becomes a lidar point cloud. Four scan pulses tag every living thing, then TARGETS LOCKED: strikes walk the map and the mast overloads. Light twice.",
    "shadow_void": "A black orb drinks the light: a growing void of darkness swallows the ground shell by shell, inhales... and exhales a shockwave of shadow. Light twice.",
    "spectral_ice": "Ice spikes erupt into a pyramid with one point. The apex lights blue, the spikes glow, and the white apex sprays a storm of ice shards. Light twice.",
    "termite_domain": "CITY DESTRUCTION. Thousands of ants pour out and eat everything alive, then raise a seventy-block termite hill. Light twice.",
    "sun_disappears": "WORLD DESTRUCTION. The strongest yet. The sun goes out: endless night, killing cold, frozen water, dead plants. Stay by a fire. Then the dead sun falls. Light twice.",
    "sonar_bomb": "Echoes roll out and the ground heaves with every one, shattering glass; then they converge in a boom that shakes the land around it to pieces.",
    "pixel_bomb": "GAME START: the world drops to low-res, on screen and in the blocks themselves. GAME OVER: an 8-bit explosion.",
    "magnetar": "A neutron star tears the land up into orbit, grinds it to dust, cuts trenches with its pulsar jets, then collapses into a hole and fires its orbit off. Light twice.",
    "ink_bomb": "The world turns to comic ink. Giant KABOOM letters rise, comic blasts go POW, then the letters blow up.",
    "aurora_bomb": "An aurora unrolls overhead, then its rays strike down and take the land apart into coloured light, shaft by shaft.",
    "seasons_bomb": "A year in forty seconds as the sun races by, then a century passes in an instant.",
    "prism_bomb": "A floating prism splits a beam from the sky into seven rainbow beams that burn coloured trenches. Light twice.",
    "geode_bomb": "The ground turns to amethyst crystal from the inside out, throws crystal spikes, rings louder and louder, and shatters into a hollow.",
    "fossil_bomb": "A colossal skeleton rises from a fissure. Its eyes catch fire, and its roar tears a widening cone out of the land in front of it. Light twice.",
    "lava_lamp": "A three-storey lava lamp heats up until the glass cracks and it throws molten goo everywhere.",
    "exploding_cows": "CELESTIAL. Cows fall from the sky as bombs, wave after wave, bigger each time. Then one enormous cow. Light twice.",
    "deadly_twister": "CELESTIAL. A mile-high twister crosses the land and drags players from safety. Light twice.",
    "colossal_spear": "A building-sized spear lands, then spins down through the world, drilling a hole to the core. Light twice.",
    "zombie_apocalypse": "An undead tide that keeps growing, five stomping giants, and a green detonation each. Light twice.",
    "ufo_invasion": "CELESTIAL. A city-wide saucer crosses the sky with a blue beam of death, then leaves. Light twice.",
    "galactic_smash": "WORLD DESTRUCTION. A cosmic giant appears, looks around, and punches the ground. Camera cutscene. Light twice.",
    "booming_tick": "WORLD DESTRUCTION. It starts as a laughable pop. Sixty seconds later, the Earth. Camera cutscene. Light twice.",
    "alien_death_ray": "WORLD DESTRUCTION. A cutscene to another world, a rocket, an EMP, a tsunami, the biggest boom. Light twice.",
    "eternal_sapling": "CITY DESTRUCTION. A sapling climbs eight ever-bigger trees to a world tree whose roots slam down; then it blooms and the forest takes the city back. Light twice.",
    "sixteen_chunks": "Sixteen chunks, sixteen dice rolls: most turn into one random block all the way through, the rest get floods, x-ray, the floor is lava and worse. Camera cutscene. Light twice.",
    "truman_show": "CITY DESTRUCTION. Lights, rigging, action: the world around you is struck into a studio set, with a main stage and a painted-sky wall. Light twice.",
    "disco_ball": "FUN. A giant disco ball spins up and dazzles everyone, cools to blue, stops... then the bass, Pigstep, and a dance floor that fades after five minutes. Camera cutscene. Light twice.",
    "disco_bomb": "A dance floor, rainbow rings and a beat that bounces everyone. Harmless.",
    "glitter_bomb": "A storm of sparkle. Everything glows for thirty seconds. Harmless.",
    "pumpkin_patch_bomb": "Dozens of jack-o'-lanterns sprout from the ground, then wither away.",
    "cobweb_bomb": "A web bursts outward and sticks to everything. It dissolves later.",
    "balloon_bomb": "A bouquet of balloons lifts everyone gently skyward, then pops.",
    "sprinkler_bomb": "A rain cloud: puts out fires, grows plants, heals.",
    "thermite_bomb": "Burns a glowing shaft straight down through the ground.",
    "tesla_bomb": "Eight seconds of lightning around the blast point.",
    "sinkhole_bomb": "The ground funnels away into a deep conical pit.",
    "tsunami_bomb": "A three-block wall of water races outward and sweeps everything away.",
    "volcano_bomb": "A cone of basalt rises, then erupts lava bombs for ten seconds.",
    "ice_age_bomb": "A creeping wave of frost: frozen water, snowy land, towering ice spires.",
    "hellmouth_bomb": "The ground opens into a lava-floored pit. Hellfire pillars.",
    "cyclone_bomb": "A tornado that wanders, ripping up the ground and everything on it. Light twice.",
    "doomsday_clock": "A sweeping hand carves a trench; every hour a strike; midnight ends it.",
    "moonfall": "CITY DESTRUCTION. The moon leaves orbit, breaks the sound barrier, and lands. Light twice.",
    "big_bang": "Silence, a point, then everything outward: a new solar system, alone in a void. Light twice.",
    "world_eater": "CITY DESTRUCTION. A gigantic cosmic worm tunnels through the land, devouring everything near its mouth. Light twice.",
    "chrono_break": "Time stops; a sphere of world is erased, then rewound, almost.",
    "ragnarok": "CITY DESTRUCTION. Lightning, meteors, quakes and volcanoes, then a burning sword splits the world into fire and frost. Light twice.",
    "helios_crown_bomb": "Twelve golden pillars race outward, burn a ring, then collapse to the centre.",
    "continental_pulse": "Five expanding waves of cracks seamed with molten gold.",
    "dawnstar_payload": "A golden star rises, gathers a ring of debris, then falls.",
    "bloodstar_collapse": "A crimson star bleeds the sky, cracks the land, then implodes. Light twice.",
    "last_light": "World event. A white star shines the world pale, then outward. Light twice.",
    "zeropoint": "World event. Time stops; everything falls into a pinprick.",
    "unmaker": "World event. The area is unmade shell by shell. Light twice.",
    "godfall": "World event. The sky opens. Light twice.",
    "final_existence": "CITY DESTRUCTION. World event. Gravity reverses, the land rises, the sky breaks. Light twice.",
    "energy_crystal": "Humming crystal left behind by Crystal Cataclysm.",
    "luminous_glass": "Glass fused from pure light.",
    "gold_fissure": "Still molten. Don't stand on it.",
    "crimson_fissure": "The wound a Bloodstar leaves.",
    "void_remnant": "What remains where nothing remains.",
    "molten_core": "Cooling slowly.",
}

EXTRA = {
    "itemGroup.armageddon.weapons": "Armageddon: Weapons",
    "itemGroup.armageddon.explosives": "Armageddon: Explosives",
    "itemGroup.armageddon.bombs": "Armageddon: Bombs",
    "itemGroup.armageddon.cataclysms": "Armageddon: Cataclysms",
    "itemGroup.armageddon.gear": "Armageddon: Gear & Materials",
    "itemGroup.armageddon.new": "Armageddon: New",
    "tier.armageddon.city_destruction": "CITY DESTRUCTION",
    "tier.armageddon.world_destruction": "WORLD DESTRUCTION",
    "message.armageddon.world_destruction": "Cutscene event — it will not be subtle",
    "message.armageddon.moonfall.title": "THE MOON IS FALLING",
    "message.armageddon.moonfall.sub": "There is nowhere to hide",
    "message.armageddon.ragnarok.title": "THE BLADE RISES",
    "message.armageddon.ragnarok.sub": "Ragnarok",
    "message.armageddon.hellmouth": "The ground is opening",
    "message.armageddon.cyclone": "A cyclone is forming",
    "message.armageddon.doomsday": "The clock has started",
    "tier.armageddon.fun": "FUN", "tier.armageddon.uncommon": "UNCOMMON", "tier.armageddon.rare": "RARE", "tier.armageddon.epic": "EPIC",
    "tier.armageddon.legendary": "LEGENDARY", "tier.armageddon.mythic": "MYTHIC", "tier.armageddon.celestial": "CELESTIAL",
    "tooltip.armageddon.confirm": "Catastrophic: use twice within 5 s to confirm",
    "tooltip.armageddon.heat": "Barrel heat: %s%%",
    "message.armageddon.confirm": "⚠ Catastrophic weapon — use again within 5 seconds to confirm",
    "message.armageddon.confirm_celestial": "⚠ WORLD EVENT — light again within 5 seconds to confirm",
    "message.armageddon.celestial_disabled": "Celestial weapons are disabled (gamerule armageddonAllowCelestial)",
    "message.armageddon.celestial_armed": "⚠ %s armed at %s %s %s — detonation in %s seconds",
    "message.armageddon.world_event": "A world event has begun",
    "message.armageddon.world_event_at": "✦ %s ✦ has begun at %s %s %s",
    "message.armageddon.mythic_warning": "⚠ MYTHIC THREAT ⚠",
    "message.armageddon.bloodstar": "A crimson star is rising",
    "message.armageddon.riftfall": "The sky is tearing open",
    "message.armageddon.spindle": "The Spindle descends",
    "message.armageddon.extinction": "The choir has begun to sing",
    "message.armageddon.impact": "IMPACT IMMINENT",
    "message.armageddon.gravity": "GRAVITY HAS REVERSED",
    "message.armageddon.solar_verdict": "☀ A Solar Verdict has been called. Clear the area.",
    "message.armageddon.verdict_sub": "Solar Verdict",
    "message.armageddon.warden.denied": "Only operators can protect regions",
    "message.armageddon.warden.first": "First corner set at %s — click the second corner",
    "message.armageddon.warden.created": "Protected region %s created",
    "message.armageddon.restored": "Restored %s blocks (%s still recorded)",
    "message.armageddon.protect.added": "Protected region %s added",
    "message.armageddon.protect.removed": "Protected region %s removed",
    "message.armageddon.protect.missing": "No protected region named %s",
    "message.armageddon.protect.list": "%s protected region(s):",
    "entity.armageddon.devourer": "Devourer",
    "entity.armageddon.world_event": "World Event",
    "gamerule.armageddonMaxRadius": "Armageddon: maximum destruction radius",
    "gamerule.armageddonAllowCelestial": "Armageddon: allow Celestial world events",
    "gamerule.armageddonRecordRestore": "Armageddon: record destruction for /armageddon restore",
    "gamerule.armageddonMaxDebris": "Armageddon: maximum flying debris blocks",
}


def lang():
    out = {}
    for k, v in NAMES.items():
        out[f"item.armageddon.{k}"] = v
    for k, v in BLOCKS.items():
        out[f"block.armageddon.{k}"] = v
    for k, lines in DESC.items():
        for i, line in enumerate(lines):
            out[f"item.armageddon.{k}.desc" + ("" if i == 0 else str(i + 1))] = line
    for k, v in BLOCK_DESC.items():
        out[f"block.armageddon.{k}.desc"] = v
    out.update(EXTRA)
    return out


NEW_BLOCKS = ["energy_crystal", "luminous_glass", "gold_fissure", "crimson_fissure", "void_remnant", "molten_core"]


def blocks(write_json, ASSETS, DATA, NS):
    bm = os.path.join(ASSETS, "models/block")
    im = os.path.join(ASSETS, "models/item")
    bs = os.path.join(ASSETS, "blockstates")
    for name in NEW_BLOCKS:
        write_json(os.path.join(bm, name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
        write_json(os.path.join(im, name + ".json"), {"parent": f"{NS}:block/{name}"})
        write_json(os.path.join(bs, name + ".json"), {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        drop = {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"{NS}:blocks/{name}"}
        if name in ("molten_core",):
            drop = {"type": "minecraft:block", "pools": [], "random_sequence": f"{NS}:blocks/{name}"}
        write_json(os.path.join(DATA, "loot_table/blocks", name + ".json"), drop)


def anim(write_json, path, frametime):
    write_json(path + ".mcmeta", {"animation": {"frametime": frametime, "interpolate": True}})


def bomb_recipes2(shaped):
    cat = "redstone"
    X = {"ion_scythe": ("minecraft:beacon", "minecraft:lightning_rod"),
         "exploding_cows": ("minecraft:cow_spawn_egg", "minecraft:milk_bucket"),
         "deadly_twister": ("minecraft:elytra", "minecraft:wind_charge"),
         "colossal_spear": ("minecraft:trident", "minecraft:netherite_block"),
         "zombie_apocalypse": ("minecraft:zombie_head", "minecraft:rotten_flesh"),
         "ufo_invasion": ("minecraft:end_crystal", "minecraft:ender_eye")}
    for name, (core, side) in X.items():
        shaped(name, ["CSC", "SKS", "CSC"], {"C": "celestial_core", "S": side, "K": core}, category=cat)
    W = {"galactic_smash": "big_bang", "booming_tick": "chrono_break", "alien_death_ray": "moonfall",
         "eternal_sapling": "world_eater", "sixteen_chunks": "ragnarok", "truman_show": "chrono_break", "disco_ball": "disco_bomb",
         "lidar_bomb": "orbital_designator", "shadow_void": "singularity_bomb", "spectral_ice": "ice_age_bomb",
         "termite_domain": "world_eater", "sun_disappears": "galactic_smash",
         "resonance_cascade": "sonar_bomb", "tesla_dominion": "tesla_bomb", "dyson_lance": "solstice_sunlance",
         "gravity_killswitch": "gravity_grenade", "seismic_tsunami": "tectonic_bomb"}
    O = {"sonar_bomb": "minecraft:sculk_shrieker", "pixel_bomb": "minecraft:note_block", "magnetar": "minecraft:lodestone",
         "ink_bomb": "minecraft:ink_sac", "aurora_bomb": "minecraft:prismarine_crystals", "seasons_bomb": "minecraft:clock",
         "prism_bomb": "minecraft:glass", "geode_bomb": "minecraft:amethyst_block", "fossil_bomb": "minecraft:bone_block",
         "lava_lamp": "minecraft:lava_bucket"}
    for name, core in O.items():
        shaped(name, ["RAR", "NCN", "RAR"], {"R": "rift_shard", "A": "antimatter_cell", "N": "minecraft:nether_star", "C": core}, category=cat)
    for name, core in W.items():
        shaped(name, ["CXC", "NKN", "CXC"], {"C": "celestial_core", "X": "singularity_core", "N": "minecraft:nether_star", "K": core}, category=cat)


def bomb_recipes3(shaped):
    cat = "redstone"
    shaped("goo_bomb", ["SLS", "LTL", "SLS"], {"S": "minecraft:slime_block", "L": "minecraft:lime_dye", "T": "minecraft:tnt"}, category=cat)
    shaped("cactus_bomb", ["CSC", "SNS", "CSC"], {"C": "minecraft:cactus", "S": "minecraft:sand", "N": "atomic_bomb"}, category=cat)
    shaped("blackforge", ["IAI", "MNM", "IBI"], {"I": "minecraft:netherite_ingot", "A": "minecraft:anvil", "M": "minecraft:magma_block",
                                               "N": "minecraft:nether_star", "B": "minecraft:blackstone"}, category=cat)
    shaped("blue_infrit", ["CSC", "FKF", "CSC"], {"C": "celestial_core", "S": "minecraft:soul_lantern", "F": "minecraft:soul_campfire",
                                                "K": "minecraft:wither_skeleton_skull"}, category=cat)
    shaped("dimension_shift", ["OGO", "CNC", "OGO"], {"O": "minecraft:crying_obsidian", "G": "minecraft:ghast_tear", "C": "minecraft:crimson_nylium",
                                                    "N": "minecraft:nether_star"}, category=cat)
    shaped("firework_havoc", ["RFR", "FTF", "RFR"], {"R": "minecraft:firework_rocket", "F": "minecraft:firework_star", "T": "minecraft:tnt"}, category=cat)
    shaped("chinese_lantern", ["PGP", "PLP", "PGP"], {"P": "minecraft:paper", "G": "minecraft:gold_nugget", "L": "minecraft:lantern"}, category=cat)
    shaped("mammoth_fossil", ["BIB", "PFP", "BIB"], {"B": "minecraft:bone_block", "I": "minecraft:blue_ice", "P": "minecraft:packed_ice",
                                                   "F": "fossil_bomb"}, category=cat)
    shaped("leviathan_fossil", ["BPB", "HFH", "BPB"], {"B": "minecraft:bone_block", "P": "minecraft:prismarine", "H": "minecraft:heart_of_the_sea",
                                                     "F": "fossil_bomb"}, category=cat)
    shaped("terror_maniac", ["RWR", "SCS", "RWR"], {"R": "minecraft:wither_rose", "W": "minecraft:wither_skeleton_skull", "S": "minecraft:sculk_catalyst",
                                                  "C": "celestial_core"}, category=cat)
    shaped("pumpkin_horseman", ["JHJ", "SCS", "JHJ"], {"J": "minecraft:jack_o_lantern", "H": "minecraft:hay_block", "S": "minecraft:soul_lantern",
                                                     "C": "celestial_core"}, category=cat)
    shaped("portal_of_hell", ["OBO", "FCF", "OBO"], {"O": "minecraft:crying_obsidian", "B": "minecraft:blaze_rod", "F": "minecraft:fire_charge",
                                                   "C": "celestial_core"}, category=cat)
    shaped("ai_takeover", ["RCR", "OTO", "RCR"], {"R": "minecraft:redstone_block", "C": "minecraft:comparator", "O": "minecraft:observer",
                                                "T": "celestial_core"}, category=cat)
    shaped("xenoform", ["EGE", "NCN", "EGE"], {"E": "minecraft:end_crystal", "G": "minecraft:echo_shard", "N": "minecraft:nether_star",
                                             "C": "celestial_core"}, category=cat)
    shaped("pterosaur_fossil", ["MBM", "BFB", "MBM"], {"M": "minecraft:phantom_membrane", "B": "minecraft:bone_block", "F": "fossil_bomb"}, category=cat)
    shaped("plasma_koi", ["SFS", "PNP", "SFS"], {"S": "minecraft:sea_lantern", "F": "minecraft:tropical_fish", "P": "minecraft:prismarine_crystals",
                                               "N": "minecraft:nether_star"}, category=cat)


def bomb_recipes(shaped):
    bomb_recipes2(shaped)
    bomb_recipes3(shaped)
    P, C, F = "titanium_plate", "circuit_board", "fusion_core"
    cat = "redstone"
    U = {"disco_bomb": "minecraft:note_block", "glitter_bomb": "minecraft:glowstone_dust", "pumpkin_patch_bomb": "minecraft:carved_pumpkin",
         "cobweb_bomb": "minecraft:cobweb", "balloon_bomb": "minecraft:slime_ball", "sprinkler_bomb": "minecraft:water_bucket"}
    for name, core in U.items():
        shaped(name, ["TIT", "ICI", "TIT"], {"T": "minecraft:tnt", "I": "minecraft:iron_ingot", "C": core}, category=cat)
    R = {"thermite_bomb": "minecraft:blaze_powder", "tesla_bomb": "minecraft:lightning_rod", "sinkhole_bomb": "minecraft:piston"}
    for name, core in R.items():
        shaped(name, ["PTP", "TCT", "PTP"], {"P": P, "T": "minecraft:tnt", "C": core}, category=cat)
    L = {"tsunami_bomb": "minecraft:prismarine_crystals", "volcano_bomb": "minecraft:magma_block", "ice_age_bomb": "minecraft:blue_ice"}
    for name, core in L.items():
        shaped(name, ["PFP", "SCS", "PFP"], {"P": P, "F": F, "S": "solar_cell", "C": core}, category=cat)
    M = {"hellmouth_bomb": "minecraft:netherite_ingot", "cyclone_bomb": "minecraft:elytra", "doomsday_clock": "minecraft:clock"}
    for name, core in M.items():
        shaped(name, ["RAR", "NCN", "RAR"], {"R": "rift_shard", "A": "antimatter_cell", "N": "minecraft:nether_star", "C": core}, category=cat)
    W = {"moonfall": "minecraft:end_crystal", "big_bang": "last_light", "world_eater": "devourer_core",
         "chrono_break": "zeropoint", "ragnarok": "extinction_choir"}
    for name, core in W.items():
        shaped(name, ["CXC", "KOK", "CXC"], {"C": "celestial_core", "X": "singularity_core", "K": core, "O": "omega_bomb"}, category=cat)


def recipes(shaped, shapeless):
    bomb_recipes(shaped)
    Ti, P, C, F = "titanium_ingot", "titanium_plate", "circuit_board", "fusion_core"
    shaped("solar_cell", [" G ", "GSG", " T "], {"G": "minecraft:glass", "S": "minecraft:glowstone", "T": Ti}, count=4)
    shaped("rift_shard", ["OEO", "EAE", "OEO"], {"O": "minecraft:crying_obsidian", "E": "minecraft:ender_pearl", "A": "antimatter_cell"}, count=2, category="misc")
    shaped("celestial_core", ["RNR", "SUS", "RNR"], {"R": "rift_shard", "N": "minecraft:nether_star", "S": "singularity_core", "U": "solar_cell"}, category="misc")
    shaped("warden_wand", ["  E", " B ", "B  "], {"E": "minecraft:emerald", "B": "minecraft:blaze_rod"})
    # Uncommon
    shaped("party_popper", ["FFF", "PCP", "T  "], {"F": "minecraft:firework_rocket", "P": "minecraft:pink_dye", "C": C, "T": Ti})
    U = {"bounce_bomb": "minecraft:slime_ball", "loot_tornado": "minecraft:hopper", "moonwalk_mine": "minecraft:phantom_membrane",
         "snow_globe_grenade": "minecraft:snow_block", "slimequake_bomb": "minecraft:slime_block", "hologram_bomb": "minecraft:glass_pane",
         "magnet_mayhem": "minecraft:lodestone", "rainbow_paint_bomb": "minecraft:white_dye"}
    for name, core in U.items():
        shaped(name, [" I ", "ICI", " G "], {"I": "minecraft:iron_nugget", "C": core, "G": "minecraft:gunpowder"}, count=4)
    shaped("airhorn_cannon", ["NNI", "BC ", "I  "], {"N": "minecraft:note_block", "I": "minecraft:iron_ingot", "B": "minecraft:goat_horn", "C": C})
    # Rare
    R = {"shatterfrost_bomb": "minecraft:blue_ice", "arcflash_charge": "minecraft:lightning_rod", "seismic_pulse_mine": "minecraft:piston",
         "implosion_grenade": "minecraft:ender_pearl", "ion_fog_canister": "minecraft:glow_ink_sac", "stasis_mine": "minecraft:amethyst_shard",
         "emp_disruptor": "minecraft:redstone_block"}
    for name, core in R.items():
        shaped(name, [" P ", "TCT", " P "], {"P": P, "C": core, "T": "minecraft:tnt"}, count=4)
    shaped("microstrike_beacon", [" R ", "PCP", "PSP"], {"R": "minecraft:redstone_torch", "P": P, "C": C, "S": "railgun_slug"})
    shaped("plasma_boomerang", ["PPC", "  P", "  P"], {"P": "plasma_cell", "C": C})
    shaped("gravity_tether_gun", ["EPP", "LC ", "T  "], {"E": "minecraft:ender_pearl", "P": P, "L": "minecraft:lead", "C": C, "T": Ti})
    # Epic
    shaped("antimatter_lance", ["AAA", "PFP", "T  "], {"A": "antimatter_cell", "P": P, "F": F, "T": Ti})
    shaped("cataclysm_mortar", ["OMO", "OFO", "PPP"], {"O": "minecraft:obsidian", "M": "mini_nuke", "F": F, "P": P})
    shaped("rift_borer", ["RRP", "DFC", " TP"], {"R": "rift_shard", "P": P, "D": "quantum_drill", "F": F, "C": C, "T": Ti})
    shaped("siegebreaker_array", ["RRR", "CFC", "PPP"], {"R": "rocket", "C": C, "F": F, "P": P})
    shaped("resonance_cannon", ["AAP", "BFC", "T  "], {"A": "minecraft:amethyst_block", "P": P, "B": "minecraft:bell", "F": F, "C": C, "T": Ti})
    E = {"quantum_fracture_bomb": "rift_shard", "crystal_cataclysm": "minecraft:amethyst_block", "voidstorm_generator": "singularity_core",
         "micro_event_horizon": "antimatter_cell", "mirror_dimension_bomb": "rift_shard"}
    for name, core in E.items():
        shaped(name, ["PGP", "GCG", "PGP"], {"P": P, "G": "minecraft:glass", "C": core}, count=2)
    # Legendary
    shaped("solstice_sunlance", ["SSS", "GFG", "T  "], {"S": "solar_cell", "G": "minecraft:gold_block", "F": F, "T": Ti})
    shaped("worldsplitter_hammer", ["GBG", "GNG", " T "], {"G": "minecraft:gold_block", "B": "titanium_block", "N": "minecraft:nether_star", "T": Ti})
    shaped("solar_verdict_beacon", ["SSS", "SBS", "CFC"], {"S": "solar_cell", "B": "minecraft:beacon", "C": C, "F": F})
    shaped("titanfall_warhead", ["NNN", "CFC", "PUP"], {"N": "mini_nuke", "C": C, "F": F, "P": P, "U": "minecraft:netherite_ingot"})
    shaped("midas_cascade", ["GGG", "PFP", "T  "], {"G": "minecraft:gold_block", "P": P, "F": F, "T": Ti})
    shaped("reality_shear_projector", ["RGR", "CSC", "PTP"], {"R": "rift_shard", "G": "minecraft:glass", "C": C, "S": "singularity_core", "P": P, "T": Ti})
    # Plasma and sci-fi
    shaped("helix_disruptor", ["PPC", "RFR", "T  "], {"P": "plasma_cell", "C": "celestial_core", "R": "rift_shard", "F": F, "T": Ti})
    shaped("graviton_lance", ["AAS", "CFC", "T  "], {"A": "antimatter_cell", "S": "singularity_core", "C": "celestial_core", "F": F, "T": Ti})
    shaped("sunfall_mortar", ["SSS", "CFC", "OOO"], {"S": "solar_cell", "C": "celestial_core", "F": F, "O": "minecraft:obsidian"})
    shaped("halo_cutter", ["NAN", "CFC", "SAS"], {"N": "minecraft:nether_star", "A": "antimatter_cell", "C": "celestial_core", "F": F, "S": "singularity_core"})
    shaped("hyperbeam_raygun", ["PPG", "CFC", "T  "], {"P": "plasma_cell", "G": "minecraft:glowstone", "C": C, "F": F, "T": Ti})
    shaped("obliterator", ["AAA", "NFC", "S  "], {"A": "antimatter_cell", "N": "minecraft:nether_star", "F": F, "C": "celestial_core", "S": "singularity_core"})
    shaped("judgment_engine", ["SSG", "MFC", " PP"], {"S": "solar_cell", "G": "minecraft:gold_block", "M": "minigun", "F": F, "C": C, "P": P})
    shaped("helios_crown_bomb", ["SGS", "GFG", "SGS"], {"S": "solar_cell", "G": "minecraft:gold_block", "F": F}, category="redstone")
    shaped("continental_pulse", ["TGT", "GBG", "TGT"], {"T": "tectonic_bomb", "G": "minecraft:gold_block", "B": "atomic_bomb"}, category="redstone")
    shaped("dawnstar_payload", ["SGS", "GNG", "SGS"], {"S": "solar_cell", "G": "minecraft:gold_block", "N": "nova_bomb"}, category="redstone")
    # Mythic
    shaped("riftfall_engine", ["RRR", "RSR", "CNC"], {"R": "rift_shard", "S": "singularity_core", "C": C, "N": "minecraft:nether_star"})
    shaped("apocalypse_spindle", [" R ", "ONO", " S "], {"R": "minecraft:end_rod", "O": "minecraft:crying_obsidian", "N": "minecraft:nether_star", "S": "solar_cell"})
    shaped("devourer_core", ["RAR", "ASA", "RAR"], {"R": "rift_shard", "A": "antimatter_cell", "S": "singularity_core"})
    shaped("extinction_choir", ["BBB", "MNM", "CFC"], {"B": "minecraft:bell", "M": "meteor_staff", "N": "minecraft:nether_star", "C": C, "F": F})
    shaped("bloodstar_collapse", ["RAR", "ANA", "RAR"], {"R": "minecraft:redstone_block", "A": "antimatter_bomb", "N": "minecraft:nether_star"}, category="redstone")
    # Celestial
    shaped("last_light", ["SCS", "CHC", "SCS"], {"S": "solar_cell", "C": "celestial_core", "H": "helios_crown_bomb"}, category="redstone")
    shaped("zeropoint", ["ICI", "COC", "ICI"], {"I": "minecraft:blue_ice", "C": "celestial_core", "O": "cryo_bomb"}, category="redstone")
    shaped("unmaker", ["RCR", "CAC", "RCR"], {"R": "rift_shard", "C": "celestial_core", "A": "antimatter_bomb"}, category="redstone")
    shaped("godfall", ["GCG", "CDC", "GCG"], {"G": "minecraft:gold_block", "C": "celestial_core", "D": "dawnstar_payload"}, category="redstone")
    shaped("final_existence", ["CCC", "COC", "CCC"], {"C": "celestial_core", "O": "omega_bomb"}, category="redstone")


PICKAXE = NEW_BLOCKS + list(NEW_BOMBS) + ["helios_crown_bomb", "continental_pulse", "dawnstar_payload", "bloodstar_collapse",
                        "last_light", "zeropoint", "unmaker", "godfall", "final_existence"]
