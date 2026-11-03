package net.johnceo.sparklingmutuals.skyblock.market

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ShardSaleContainerCompatibilityTest {
    @Test fun `shadowed slot field must not inject property methods over other mods public accessors`() {
        val collisions=mutableListOf<String>()
        val stream=javaClass.classLoader.getResourceAsStream("net/johnceo/sparklingmutuals/mixin/ShardSaleContainerMixin.class")!!
        stream.use {ClassReader(it).accept(object: ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(access: Int,name: String,descriptor: String,signature: String?,exceptions: Array<out String>?): MethodVisitor? {
                if(name in setOf("getHoveredSlot","setHoveredSlot"))collisions.add("$name$descriptor access=$access")
                return null
            }
        },ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)}
        assertTrue(collisions.isEmpty(),"Generated property methods collide with NoFrills' public hovered-slot accessor: $collisions")
    }
}
