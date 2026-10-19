package net.johnceo.sparklingmutuals.misc.phantomleaf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*

class PhantomleafSoundHookTest {
    @Test fun `sound hook targets vanilla volume read after sound resolution instead of unsafe method entry`() {
        val at = mutableMapOf<String, Any>()
        val loader = javaClass.classLoader
        ClassReader(loader.getResourceAsStream("net/johnceo/sparklingmutuals/mixin/PhantomleafSoundMixin.class")).accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?): MethodVisitor? {
                    if (n != "phantomleafSound") return null
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                            if (descriptor != "Lorg/spongepowered/asm/mixin/injection/Inject;") return null
                            return object : AnnotationVisitor(Opcodes.ASM9) {
                                override fun visitArray(name: String): AnnotationVisitor? = if (name != "at") null else
                                    object : AnnotationVisitor(Opcodes.ASM9) {
                                        override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor =
                                            object : AnnotationVisitor(Opcodes.ASM9) {
                                                override fun visit(name: String, value: Any) { at[name] = value }
                                            }
                                    }
                            }
                        }
                    }
                }
            }, 0)
        assertEquals("INVOKE", at["value"], "HEAD reads pitch before AbstractSoundInstance.sound is initialized")
        assertEquals("Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F", at["target"])
        assertEquals(0, at["ordinal"])

        val calls = mutableListOf<String>()
        ClassReader(loader.getResourceAsStream("net/minecraft/client/sounds/SoundEngine.class")).accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?): MethodVisitor? {
                    if (n != "play" || !d.startsWith("(Lnet/minecraft/client/resources/sounds/SoundInstance;)")) return null
                    return object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, itf: Boolean) {
                            if (owner == "net/minecraft/client/resources/sounds/SoundInstance") calls += name
                        }
                    }
                }
            }, 0)
        assertTrue(calls.indexOf("resolve") >= 0)
        assertTrue(calls.indexOf("getVolume") > calls.indexOf("resolve"),
            "The actual Minecraft hook target must occur after sound resolution")
    }
}
