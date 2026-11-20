package net.johnceo.sparklingmutuals.safari.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*

class CritterNamePresentationHooksTest {
    private fun calls(resource: String, method: String, descriptor: String?=null): List<String> {
        val result=mutableListOf<String>()
        ClassReader(javaClass.classLoader.getResourceAsStream(resource)).accept(object: ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(a: Int,n: String,d: String,s: String?,e: Array<out String>?): MethodVisitor? =
                if(n!=method || descriptor!=null && descriptor!=d)null else object: MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(op: Int,owner: String,name: String,desc: String,itf: Boolean) {result.add("$owner;$name$desc")}
                }
        },0)
        return result
    }
    @Test fun `chat aliases hook line wrapping after logging rather than changing stored messages`() {
        assertTrue("net/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;" in
            calls("net/minecraft/client/multiplayer/chat/GuiMessage.class","splitLines"))
        assertTrue(calls("net/minecraft/client/gui/components/ChatComponent.class","extractRenderState").isNotEmpty())
    }
    @Test fun `hunting box hook receives a tooltip list after native item tooltip collection`() {
        val calls=calls("net/minecraft/client/gui/GuiGraphicsExtractor.class","setTooltipForNextFrame","(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V")
        val collect=calls.indexOf("net/minecraft/client/gui/screens/Screen;getTooltipFromItem(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
        //? if >=26.3 {
        /*val target="net/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;Z)V"
        *///? } else {
        val target="net/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
        //? }
        val render=calls.indexOf(target)
        assertTrue(collect>=0 && render>collect)
    }
}
