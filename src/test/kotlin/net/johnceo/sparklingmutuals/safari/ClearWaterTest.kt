package net.johnceo.sparklingmutuals.safari

import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariSettings
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.*

class ClearWaterTest {
    @TempDir lateinit var dir: Path
    @Test fun `water surface alpha changes without changing tint lava or disabled rendering`() {
        val original=0xFF336699.toInt()
        assertEquals(0x99336699.toInt(),ClearWater.surfaceColor(original,true,true))
        assertEquals(original,ClearWater.surfaceColor(original,false,true))
        assertEquals(original,ClearWater.surfaceColor(original,true,false))
        assertEquals(0x08336699,ClearWater.surfaceColor(0x08336699,true,true))
        assertTrue(ClearWater.enabled(true,true))
        assertFalse(ClearWater.enabled(true,false));assertFalse(ClearWater.enabled(false,true))
    }
    @Test fun `clear water toggle persists under Safari visual cleanup`() {
        ConfigManager.init(dir)
        assertTrue(ConfigManager.clearWater)
        assertEquals(60, ConfigManager.waterOpacity)
        val settings=SafariSettings()
        settings.safari.visibility.clearWater=false
        settings.safari.visibility.waterOpacity=65;settings.apply()
        ConfigManager.init(dir)
        assertFalse(ConfigManager.clearWater)
        assertEquals(65, ConfigManager.waterOpacity)
        assertEquals("Clear water",SafariSettings.SafariVisibilitySettings::class.java.getField("clearWater")
            .getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java).name)
    }
    @Test fun `surface and overlay hook calls exist on the target Minecraft renderer`() {
        fun calls(resource: String, method: String): List<String> {
            val calls=mutableListOf<String>()
            ClassReader(javaClass.classLoader.getResourceAsStream(resource)).accept(object:ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(a:Int,n:String,d:String,s:String?,e:Array<out String>?):MethodVisitor? =
                    if(n!=method) null else object:MethodVisitor(Opcodes.ASM9) {
                        override fun visitMethodInsn(op:Int,owner:String,name:String,desc:String,itf:Boolean) { calls += "$owner;$name$desc" }
                    }
            },0)
            return calls
        }
        assertTrue(calls("net/minecraft/client/renderer/block/FluidRenderer.class","tesselate").any {
            it=="net/minecraft/client/color/block/BlockTintSource;colorInWorld(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I" })
        //? if >=26.3 {
        /*val overlayMethod = "submit"
        val overlayTarget = "net/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/renderer/state/level/PlayerRenderState\$WaterOverlay;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
        *///? } else if >=26.2 {
        /*val overlayMethod = "submit"
        val overlayTarget = "net/minecraft/client/renderer/ScreenEffectRenderer;submitWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
        *///? } else {
        val overlayMethod = "renderScreenEffect"
        val overlayTarget = "net/minecraft/client/renderer/ScreenEffectRenderer;renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
        //? }
        assertTrue(overlayTarget in calls("net/minecraft/client/renderer/ScreenEffectRenderer.class", overlayMethod))
    }
    @Test fun `opacity slider controls water alpha at endpoints and preserves tint and lower original alpha`() {
        assertEquals(0x00336699,ClearWater.surfaceColor(0xFF336699.toInt(),true,true,0))
        assertEquals(0x80336699.toInt(),ClearWater.surfaceColor(0xFF336699.toInt(),true,true,50))
        assertEquals(0xFF336699.toInt(),ClearWater.surfaceColor(0xFF336699.toInt(),true,true,100))
        assertEquals(0x08336699,ClearWater.surfaceColor(0x08336699,true,true,65))
        assertEquals(0xFF336699.toInt(),ClearWater.surfaceColor(0xFF336699.toInt(),false,true,0))
        val previous=ConfigManager.waterOpacity
        try {
            ConfigManager.waterOpacity=-1;assertEquals(0,ConfigManager.waterOpacity)
            ConfigManager.waterOpacity=101;assertEquals(100,ConfigManager.waterOpacity)
        } finally { ConfigManager.waterOpacity=previous }
    }
}
