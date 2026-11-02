package net.johnceo.sparklingmutuals.config

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NativeSettingsTest {
    @TempDir lateinit var dir: Path
    @Test fun `native catalog exposes every category and only supported controls`() {
        ConfigManager.init(dir);ContestConfig.init(dir)
        val settings=SafariSettings();val catalog=SettingsCatalog(settings)
        assertSame(settings.customization.huds.progress,settings.tracking.progressHud.appearance)
        assertSame(settings.customization.huds.objectives,settings.tracking.objectivesHud.appearance)
        assertSame(settings.customization.huds.doomThrow,settings.safari.capture.doomAppearance)
        assertEquals(13,catalog.pages.size)
        fun visit(rows: List<SettingsCatalog.Row>) {
            rows.forEach { row ->
                if(row.group) { assertTrue(row.children.isNotEmpty(),row.path);visit(row.children) }
                else if(!row.standalone) {
                    val field=row.field
                    assertTrue(field.isAnnotationPresent(ConfigEditorBoolean::class.java) || field.isAnnotationPresent(ConfigEditorButton::class.java) ||
                        field.isAnnotationPresent(ConfigEditorColour::class.java) || field.isAnnotationPresent(ConfigEditorDropdown::class.java) ||
                        field.isAnnotationPresent(ConfigEditorSlider::class.java) || field.isAnnotationPresent(ConfigEditorText::class.java),row.path)
                    field.getAnnotation(ConfigEditorButton::class.java)?.let { assertTrue(row.read() is Runnable || settings.isValidRunnable(it.runnableId),row.path) }
                }
            }
        }
        catalog.pages.forEach { visit(it.rows) }
        assertNotNull(catalog.locate(SettingsTarget("HUDs",listOf("progressHud","profit"))))
        assertNotNull(catalog.locate(SettingsTarget("HUDs",listOf("contestHud","score"))))
        val edits=catalog.pages.first { it.name=="Party Interactions" }.rows
        assertTrue(edits.none { it.name == "Hotspot announcer" })
        assertTrue(catalog.pages.first { it.name=="Modes" }.rows.all { it.children.none { row -> row.name.endsWith("mode",true) && !row.group } })
    }
    @Test fun `replacement colors preserve alpha hue and legacy speed encoding`() {
        assertEquals(0x80445566.toInt(),SettingsColor.argb("0:128:68:85:102",0))
        assertEquals(0xFFFF0000.toInt(),SettingsColor.argb("255:255:255:0:0",0))
        assertEquals(0xFF00FFFF.toInt(),SettingsColor.argb("255:255:255:0:0",500))
        assertEquals("11:128:18:52:86",SettingsColor.fromHex("#123456","11:128:1:2:3"))
        assertNull(SettingsColor.fromHex("red","0:255:1:2:3"))
    }
    @Test fun `public release versions compare numerically without treating failures as current`() {
        assertTrue(ModUpdates.compareVersions("v3.10.0","3.9.9")!!>0)
        assertEquals(0,ModUpdates.compareVersions("v3.6.3","3.6.3+mc26.1.2"))
        assertNull(ModUpdates.compareVersions("unavailable","3.6.3"))
    }
    @Test fun `release notes display headings bullets and command names without markdown syntax`() {
        val blocks=ReleaseNotes.parse("## Getting started\n- Use `/sm` and **settings**.\n\n\n[Releases](https://example.com)")
        assertEquals(listOf(ReleaseNotes.Kind.HEADING,ReleaseNotes.Kind.BULLET,ReleaseNotes.Kind.SPACE,ReleaseNotes.Kind.TEXT),blocks.map { it.kind })
        assertEquals("Getting started",blocks.first().text)
        assertEquals("Use /sm and settings.",blocks[1].text)
        assertEquals("Releases",blocks.last().text)
    }
}
