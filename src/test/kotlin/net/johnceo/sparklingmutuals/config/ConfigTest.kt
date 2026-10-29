package net.johnceo.sparklingmutuals.config

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.SafariRun
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConfigTest {
    @Test fun `curated defaults exclude account data and never replace existing configs`() {
        val defaults = FreshInstallDefaults.main()
        assertFalse(defaults.containsKey("apiKey"))
        assertFalse(defaults.stringPropertyNames().any { it.startsWith("pb.") || it.contains("uuid", true) })
        ConfigManager.init(dir); ContestConfig.init(dir)
        assertEquals(1, ConfigManager.safariTicketPreference)
        assertFalse(AppearanceConfig.panels.getValue("progress").borderEnabled)
        assertEquals(.015104167f, SafariHud.PROGRESS.layout.x)
        val contest = ContestConfig.layoutFor(640, 360).bounds(640, 360, 150, 42)
        assertEquals(129, contest.x); assertEquals(9, contest.y)
        val main = "shinyDetection=false\nsafariTicketPreference=0\nhud.progress.x=0.25\nhud.progress.scale=1.4\n"
        val legacyContest = "hudX=41\nhudY=22\nhudScale=1.2\ncontestWarnMinutes=5\n"
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), main)
        Files.writeString(dir.resolve("sparkling-mutuals-contest.properties"), legacyContest)
        ConfigManager.init(dir); ContestConfig.init(dir)
        assertFalse(ConfigManager.shinyDetection)
        assertEquals(0, ConfigManager.safariTicketPreference)
        assertEquals(.25f, SafariHud.PROGRESS.layout.x)
        assertEquals(1.4f, SafariHud.PROGRESS.layout.effectiveScale(320, 240, 100, 100))
        assertEquals(41, ContestConfig.layoutFor(320, 240).bounds(320, 240, 150, 42).x)
        assertEquals(listOf(5), ContestConfig.warningMinutes())
        assertEquals(main, Files.readString(dir.resolve("sparkling-mutuals.properties")))
        assertEquals(legacyContest, Files.readString(dir.resolve("sparkling-mutuals-contest.properties")))
    }
    @Test fun `single projection switch preserves old master choice and drops the obsolete gate on save`() {
        val file=dir.resolve("sparkling-mutuals.properties")
        for(master in listOf(false,true)) for(oldGate in listOf(false,true)) {
            Files.writeString(file,"experimentalCapsulePath=$master\nallowUncalibratedCapsuleEstimates=$oldGate\ncapsulePathWidth=4.0\nshowCapsuleGuideThroughTerrain=false\n")
            ConfigManager.init(dir)
            assertEquals(master,ConfigManager.experimentalCapsulePath)
            val settings=SafariSettings()
            settings.safari.experimentalCapsulePath=!master
            settings.apply()
            ConfigManager.init(dir)
            assertEquals(!master,ConfigManager.experimentalCapsulePath)
            assertEquals(4f,ConfigManager.capsulePathWidth)
            assertFalse(ConfigManager.showCapsuleGuideThroughTerrain)
            val saved=java.util.Properties().also { Files.newInputStream(file).use(it::load) }
            assertFalse(saved.containsKey("allowUncalibratedCapsuleEstimates"),"Obsolete second gate must not survive a settings save")
        }
    }
    @Test fun `through terrain display preference loads from saved settings`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"),"showCapsuleGuideThroughTerrain=false\n")
        ConfigManager.init(dir)
        assertFalse(ConfigManager.showCapsuleGuideThroughTerrain)
    }
    @Test fun `through terrain toggle saves through the settings screen and defaults on for old files`() {
        ConfigManager.init(dir);assertTrue(ConfigManager.showCapsuleGuideThroughTerrain)
        val settings=SafariSettings();settings.safari.showCapsuleGuideThroughTerrain=false;settings.apply()
        ConfigManager.init(dir);assertFalse(ConfigManager.showCapsuleGuideThroughTerrain)
        Files.writeString(dir.resolve("sparkling-mutuals.properties"),"showCapsuleGuideThroughTerrain=invalid\n")
        ConfigManager.init(dir);assertTrue(ConfigManager.showCapsuleGuideThroughTerrain)
    }
    @Test fun `projection width persists and remains finite for old and invalid files`() {
        ConfigManager.init(dir);assertEquals(3f,ConfigManager.capsulePathWidth)
        val settings=SafariSettings();settings.safari.capsulePathWidth=9f;settings.apply()
        ConfigManager.init(dir);assertEquals(5f,ConfigManager.capsulePathWidth)
        Files.writeString(dir.resolve("sparkling-mutuals.properties"),"capsulePathWidth=NaN\n")
        ConfigManager.init(dir);assertEquals(3f,ConfigManager.capsulePathWidth)
    }
    @TempDir lateinit var dir: Path
    @Test fun `capsule preview defaults on and saved opt out survives settings application and reload`() {
        val old=ConfigManager.experimentalCapsulePath
        try {
            ConfigManager.init(dir)
            assertTrue(ConfigManager.experimentalCapsulePath)
            Files.writeString(dir.resolve("sparkling-mutuals.properties"),"experimentalCapsulePath=invalid\n")
            ConfigManager.init(dir)
            assertTrue(ConfigManager.experimentalCapsulePath)
            val settings=SafariSettings()
            settings.safari.experimentalCapsulePath=false
            settings.apply()
            ConfigManager.experimentalCapsulePath=true
            ConfigManager.init(dir)
            assertFalse(ConfigManager.experimentalCapsulePath)
            assertFalse(SafariSettings().safari.experimentalCapsulePath)
        } finally { ConfigManager.experimentalCapsulePath=old }
    }
    @Test fun `HUD layouts and personal bests survive actual configuration saving`() {
        ConfigManager.init(dir)
        ConfigManager.personalBests.record("CAPTURE! You caught a Doomspiral!", SafariRun(1000), 62000)
        SafariHud.CAPTURES.layout.x = .4f
        SafariHud.CAPTURES.layout.scale = 1.3f
        ConfigManager.catchCountPanel = true
        ConfigManager.save()
        ConfigManager.init(dir)
        assertEquals(61000L, ConfigManager.personalBests.time("Doomspiral", duo = true))
        assertNull(ConfigManager.personalBests.time("Wumpa", duo = true))
        assertEquals(.4f, SafariHud.CAPTURES.layout.x)
        assertEquals(1.3f, SafariHud.CAPTURES.layout.scale)
        assertTrue(ConfigManager.catchCountPanel)
        val saved = Files.readString(dir.resolve("sparkling-mutuals.properties"))
        assertFalse(saved.contains("pb."))
        assertTrue(Files.readString(dir.resolve("sparkling-mutuals-pbs.properties")).contains("pb.duo.doomspiralMillis=61000"))
        assertTrue(saved.contains("hud.captures.scale=1.3"))
    }

    @Test fun `legacy PBs migrate automatically without losing other settings`() {
        val main = dir.resolve("sparkling-mutuals.properties")
        val previous = dir.resolve("Sparkling Mutual's PBs.properties")
        Files.writeString(previous, "pb.duo.wumpaMillis=30000\n")
        Files.writeString(main, "apiKey=keep-me\npb.doomspiralMillis=61000\npb.unique.forestMillis=90000\npb.party.friend.name=Friend\npb.party.friend.wumpaMillis=70000\n")
        ConfigManager.init(dir)
        val separate = dir.resolve("sparkling-mutuals-pbs.properties")
        assertTrue(Files.exists(separate))
        assertFalse(Files.exists(previous))
        assertEquals(30000L, ConfigManager.personalBests.time("Wumpa", duo = true))
        assertFalse(Files.readString(main).contains("pb."))
        assertEquals("keep-me", ConfigManager.apiKey)
        assertEquals(61000L, ConfigManager.personalBests.time("Doomspiral", duo = true))
        assertTrue(Files.readString(separate).contains("pb.party.friend.times.duo.WumpaMillis=70000"))
        ConfigManager.init(dir)
        assertEquals(61000L, ConfigManager.personalBests.time("Doomspiral", duo = true))
        assertEquals(90000L, ConfigManager.personalBests.time("Forest", net.johnceo.sparklingmutuals.safari.completion.BiomePbType.UNIQUE))
    }
    @Test fun `migration merges existing PB file keeping faster times`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "pb.duo.doomspiralMillis=60000\npb.duo.wumpaMillis=80000\n")
        Files.writeString(dir.resolve("sparkling-mutuals-pbs.properties"), "pb.duo.doomspiralMillis=50000\npb.duo.wumpaMillis=90000\n")
        ConfigManager.init(dir)
        assertEquals(50000L, ConfigManager.personalBests.time("Doomspiral", duo = true))
        assertEquals(80000L, ConfigManager.personalBests.time("Wumpa", duo = true))
    }

    @Test
    fun `legacy API configuration gets the new defaults and preserves its key`() {
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "apiKey=example-key\n")
        ConfigManager.init(dir)
        assertEquals("example-key", ConfigManager.apiKey)
        assertEquals(23, ConfigManager.warpDelaySeconds)
        assertFalse(ConfigManager.warpAlertsEnabled)
        assertTrue(ConfigManager.partyCommandsEnabled)
    }

    @Test
    fun `warp settings survive a restart and invalid values fall back safely`() {
        ConfigManager.init(dir)
        ConfigManager.warpAlertsEnabled = true
        ConfigManager.warpDelaySeconds = 28
        ConfigManager.partyCommandsEnabled = false
        ConfigManager.save()
        ConfigManager.init(dir)
        assertTrue(ConfigManager.warpAlertsEnabled)
        assertEquals(28, ConfigManager.warpDelaySeconds)
        assertFalse(ConfigManager.partyCommandsEnabled)
        Files.writeString(dir.resolve("sparkling-mutuals.properties"), "warpDelaySeconds=invalid\n")
        ConfigManager.init(dir)
        assertEquals(23, ConfigManager.warpDelaySeconds)
    }

    @Test
    fun `warning presets parse spaces and none survives saving`() {
        ContestConfig.init(dir)
        assertEquals(listOf(3, 1), ContestConfig.warningMinutes())
        ContestConfig.contestWarnMinutes = "3, 1"
        ContestConfig.save()
        ContestConfig.init(dir)
        assertEquals(listOf(3, 1), ContestConfig.warningMinutes())
        ContestConfig.contestWarnEnabled = false
        ContestConfig.contestWarnMinutes = ""
        ContestConfig.save()
        ContestConfig.init(dir)
        assertTrue(ContestConfig.warningMinutes().isEmpty())
    }
}
