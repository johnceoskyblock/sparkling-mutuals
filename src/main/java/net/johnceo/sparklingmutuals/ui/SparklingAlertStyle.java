// Adapted from SafariUtils (c) 2026 Serkosea and MrCloudy2, MIT. See licenses/SafariUtils-MIT.txt.
package net.johnceo.sparklingmutuals.ui;
public record SparklingAlertStyle(
	int stars, int comets, int bursts, int orbits, int confetti,
	int ribbons, int corners, int rays, int shockwaves, int glitterWaves,
	int prisms, int fireworks, int halos, int nebulae, int spirals,
	int curtains, int chromaticRain, int diamonds, int horizonFlares,
	int constellations, int lattice, int embers, int kaleidoscope, int eclipses,
	int runes, int crownRays, int hearts, int mirrorComets, int sweeps,
	int twinHelix, int lightning, int prismFans, int satellites,
	int hourglass, int glyphRain, int novaCrosses, int portalGates,
	int crystalShards, int infinityTrails, int gravityWells,
	int spectrumSteps, int starfallColumns,
	int[] additions,
	int pulse, int frame, int callout, int textShake, int textGradientSpeed,
	int soundSong, int soundTheme, float durationSeconds,
	int soundVolumePercent) {
 public static final int CUSTOM_INDEX=41;
	public static final String PRISMATIC_WHISPER = "Prismatic Whisper";
	public static final String PRISMATIC_GLEAM = "Prismatic Gleam";
	public static final String STARLIGHT_DRIFT = "Starlight Drift";
	public static final String STARLIGHT_WALTZ = "Starlight Waltz";
	public static final String AURORA_RISE = "Aurora Rise";
	public static final String AURORA_CASCADE = "Aurora Cascade";
	public static final String COMET_SHOWER = "Comet Shower";
	public static final String COMET_TEMPEST = "Comet Tempest";
	public static final String FIREWORK_BLOOM = "Firework Bloom";
	public static final String FIREWORK_SYMPHONY = "Firework Symphony";
	public static final String CELESTIAL_CHORUS = "Celestial Chorus";
	public static final String CELESTIAL_CARNIVAL = "Celestial Carnival";
	public static final String NEBULA_SURGE = "Nebula Surge";
	public static final String NEBULA_RHAPSODY = "Nebula Rhapsody";
	public static final String GALACTIC_PULSE = "Galactic Pulse";
	public static final String GALACTIC_TEMPEST = "Galactic Tempest";
	public static final String ASTRAL_REVERIE = "Astral Reverie";
	public static final String ASTRAL_ASCENSION = "Astral Ascension";
	public static final String SUPERNOVA = "Supernova";
	public static final String HYPERNOVA = "Hypernova";
	public static final String COSMIC_JUBILEE = "Cosmic Jubilee";
	public static final String COSMIC_OVERDRIVE = "Cosmic Overdrive";
	public static final String RADIANT_SINGULARITY = "Radiant Singularity";
	public static final String CELESTIAL_TRANSCENDENCE = "Celestial Transcendence";
	public static final String EVENT_HORIZON = "Event Horizon";
	public static final String INFINITE_RADIANCE = "Infinite Radiance";
	public static final String ENDLESS_COSMOS = "Endless Cosmos";
	public static final String CHROMATIC_ZENITH = "Chromatic Zenith";
	public static final String ETHEREAL_GENESIS = "Ethereal Genesis";
	public static final String ETHEREAL_ASCENDANCE = "Ethereal Ascendance";
	public static final String MYTHIC_DOMINION = "Mythic Dominion";
	public static final String MYTHIC_ETERNITY = "Mythic Eternity";
	public static final String CELESTIAL_GENESIS = "Celestial Genesis";
	public static final String CELESTIAL_ASCENDANCE = "Celestial Ascendance";
	public static final String INFINITE_TRANSCENDENCE = "Infinite Transcendence";
	public static final String INFINITE_APOTHEOSIS = "Infinite Apotheosis";
	public static final String ETERNAL_DAWN = "Eternal Dawn";
	public static final String ETERNAL_RESONANCE = "Eternal Resonance";
	public static final String ETERNAL_DOMINION = "Eternal Dominion";
	public static final String ETERNAL_TRANSCENDENCE = "Eternal Transcendence";
	public static final String ETERNAL_APOTHEOSIS = "Eternal Apotheosis";
	public static final String CUSTOM = "Custom";
	public static final String[] NAMES = {
		PRISMATIC_WHISPER, PRISMATIC_GLEAM, STARLIGHT_DRIFT, STARLIGHT_WALTZ,
		AURORA_RISE, AURORA_CASCADE, COMET_SHOWER, COMET_TEMPEST,
		FIREWORK_BLOOM, FIREWORK_SYMPHONY, CELESTIAL_CHORUS, CELESTIAL_CARNIVAL,
		NEBULA_SURGE, NEBULA_RHAPSODY, GALACTIC_PULSE, GALACTIC_TEMPEST,
		ASTRAL_REVERIE, ASTRAL_ASCENSION, SUPERNOVA, HYPERNOVA,
		COSMIC_JUBILEE, COSMIC_OVERDRIVE, RADIANT_SINGULARITY,
		CELESTIAL_TRANSCENDENCE, EVENT_HORIZON, INFINITE_RADIANCE,
		ENDLESS_COSMOS, CHROMATIC_ZENITH,
		ETHEREAL_GENESIS, ETHEREAL_ASCENDANCE, MYTHIC_DOMINION, MYTHIC_ETERNITY,
		CELESTIAL_GENESIS, CELESTIAL_ASCENDANCE, INFINITE_TRANSCENDENCE,
		INFINITE_APOTHEOSIS, ETERNAL_DAWN, ETERNAL_RESONANCE, ETERNAL_DOMINION,
		ETERNAL_TRANSCENDENCE, ETERNAL_APOTHEOSIS, CUSTOM
	};
	private static final int[] ORIGINAL_EFFECT_ORDER = {
		0, 5, 43, 3, 1, 6, 9, 44, 7, 12, 2, 8, 10, 11, 13, 14,
		15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 4, 42
	};
	private static final SparklingAlertStyle[] PRESETS = createPresets();
 public int addition(int index) { return index>=0 && index<additions.length ? additions[index] : 0; }
	public static SparklingAlertStyle preset(int index) {
		return PRESETS[Math.clamp(index, 0, CUSTOM_INDEX - 1)];
	}

	/** Precomputes a monotonic sequence and phases every new effect into later presets. */
	private static SparklingAlertStyle[] createPresets() {
		SparklingAlertStyle[] presets = new SparklingAlertStyle[CUSTOM_INDEX];
		for (int index = 0; index < presets.length; index++) {
			int[] effects = new int[45];
			int baseIndex = Math.min(index, 27);
			int baseBudget = 3 + baseIndex * (ORIGINAL_EFFECT_ORDER.length * 6 - 3) / 27;
			for (int point = 0; point < baseBudget; point++) {
				int effect = ORIGINAL_EFFECT_ORDER[point % ORIGINAL_EFFECT_ORDER.length];
				effects[effect] = Math.min(6, effects[effect] + 1);
			}
			if (index >= 28) {
				int newBudget = Math.min(48, (index - 27) * 6);
				for (int point = 0; point < newBudget; point++) {
					int effect = 29 + point % 8;
					effects[effect] = Math.min(6, effects[effect] + 1);
				}
			}
			if (index >= 36) {
				int newestBudget = (index - 35) * 6;
				for (int point = 0; point < newestBudget; point++) {
					int effect = 37 + point % 5;
					effects[effect] = Math.min(6, effects[effect] + 1);
				}
			}
			int[] additions = new int[15];
			for (int addition = 0; addition < additions.length; addition++) {
				int start = 8 + addition * 2;
				if (index > start) additions[addition] = Math.min(6,
					1 + (index - start) * 5 / Math.max(1, CUSTOM_INDEX - 1 - start));
			}
			int soundTheme = Math.min(12, index * 13 / CUSTOM_INDEX);
			int soundSong = Math.min(10,index*11/CUSTOM_INDEX);
			int duration = switch(soundTheme) { case 0,1 -> 5;case 2,3 -> 8;case 4,5 -> 12;case 6,7 -> 16;case 8,9 -> 20;case 10,11 -> 24;default -> 30; };
			int textShake = index < 4 ? 0
				: Math.min(6, 1 + (index - 4) * 6 / (CUSTOM_INDEX - 4));
			int textGradientSpeed = Math.min(6, 2 + index * 5 / CUSTOM_INDEX);
			presets[index] = new SparklingAlertStyle(
				effects[0], effects[1], effects[2], effects[3], effects[4], effects[5],
				effects[6], effects[7], effects[8], effects[9], effects[10], effects[11],
				effects[12], effects[13], effects[14], effects[15], effects[16], effects[17],
				effects[18], effects[19], effects[20], effects[21], effects[22], effects[23],
				effects[24], effects[25], effects[26], effects[27], effects[28], effects[29],
				effects[30], effects[31], effects[32], effects[33], effects[34], effects[35],
				effects[36], effects[37], effects[38], effects[39], effects[40], effects[41],
				additions, effects[42], effects[43], effects[44],
				textShake, textGradientSpeed,
				soundSong, soundTheme, duration, 100);
		}
		return presets;
	}

 public long displayMillis() { return Math.round(durationSeconds*1000f); }
}
