package net.johnceo.sparklingmutuals.safari.automation.entry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*

class CjsPhysicalInputTest {
    @Test fun `SDL button masks distinguish left middle right and invalid bindings`() {
        assertTrue(cjsSdlMouseDown(1,1))
        assertFalse(cjsSdlMouseDown(1,2))
        assertTrue(cjsSdlMouseDown(4,3))
        assertFalse(cjsSdlMouseDown(4,1))
        assertTrue(cjsSdlMouseDown(2,2))
        assertTrue(cjsSdlMouseDown(Int.MIN_VALUE,32))
        assertFalse(cjsSdlMouseDown(-1,0));assertFalse(cjsSdlMouseDown(-1,33))
        assertFalse(cjsSdlMouseDown(0,3))
    }
    @Test fun `compiled physical input uses the active target keyboard and mouse backend`() {
        val calls=mutableSetOf<String>()
        val resource="net/johnceo/sparklingmutuals/safari/automation/entry/CjsApproachInput.class"
        ClassReader(javaClass.classLoader.getResourceAsStream(resource)).accept(object:ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(a:Int,n:String,d:String,s:String?,e:Array<out String>?):MethodVisitor? {
                if(!n.startsWith("physical")) return null
                return object:MethodVisitor(Opcodes.ASM9) {
                    override fun visitMethodInsn(op:Int,o:String,n:String,d:String,i:Boolean) { calls.add(o+"."+n+d) }
                }
            }
        },ClassReader.SKIP_DEBUG)
        //? if >=26.3 {
        /*assertTrue("com/mojang/blaze3d/platform/InputConstants.isKeyDown(I)Z" in calls)
        assertTrue("org/lwjgl/sdl/SDLMouse.nSDL_GetMouseState(JJ)I" in calls)
        assertFalse(calls.any { it.startsWith("org/lwjgl/glfw/") })
        *///? } else {
        assertTrue("com/mojang/blaze3d/platform/InputConstants.isKeyDown(Lcom/mojang/blaze3d/platform/Window;I)Z" in calls)
        assertTrue("org/lwjgl/glfw/GLFW.glfwGetMouseButton(JI)I" in calls)
        //? }
    }
}
