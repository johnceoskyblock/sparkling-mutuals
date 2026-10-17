package net.johnceo.sparklingmutuals.safari.automation

import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Controlled game context replaces only Minecraft-dependent observation, not the production decision or hooks. */
object EntryBoundaryFixture {
    var stage = EntryStage.SAFARI
    var entryTarget = true
    var downtime = false
    var kick = true
    var ready = true
    var shiftHeld = false
    private fun blocked() = entryTarget && EntryGuardDecision.block(stage, downtime, kick, ready, false, shiftHeld)
    fun shouldBlock(entity: Entity?) = blocked()
    fun shouldBlockTicket(player: Player?, hand: InteractionHand?) = blocked()
    fun entryInput(input: ContainerInput, container: Int, slot: Int) =
        SafariManagerGuard.ticketInput(input, shiftHeld, entryTarget)
    fun shouldBlockEntry(container: Int, slot: Int) = blocked()
}

class EntryBoundaryTest {
    private fun invokeBoundary(mixin: Any, name: String, hand: InteractionHand, input: ContainerInput): CallbackInfo {
        val method = mixin.javaClass.declaredMethods.single { it.name == name }.apply { isAccessible = true }
        val returns = name in listOf("guard", "guardTicket", "guardTicketOn")
        val callback: CallbackInfo = if (returns) CallbackInfoReturnable<InteractionResult>(name, true) else CallbackInfo(name, true)
        val args = method.parameterTypes.map { t -> when {
            t == Int::class.javaPrimitiveType -> 0
            t == InteractionHand::class.java -> hand
            t == ContainerInput::class.java -> input
            CallbackInfo::class.java.isAssignableFrom(t) -> callback
            else -> null
        } }.toTypedArray()
        method.invoke(mixin, *args)
        if (callback.isCancelled && returns) assertSame(InteractionResult.FAIL, (callback as CallbackInfoReturnable<*>).returnValue)
        return callback
    }
    @Test fun `every entry route blocks repeated attempts until full presence and blocks later departures`() {
        val mixin = boundary()
        EntryBoundaryFixture.stage = EntryStage.SAFARI
        EntryBoundaryFixture.entryTarget = true
        EntryBoundaryFixture.downtime = false
        EntryBoundaryFixture.kick = false
        for (name in listOf("guard", "guardAttack", "guardTicket", "guardTicketOn", "guardEntry")) {
            for (hand in InteractionHand.entries) for (input in ContainerInput.entries) {
                EntryBoundaryFixture.ready = false
                repeat(100) { assertTrue(invokeBoundary(mixin, name, hand, input).isCancelled, "$name $hand $input") }
                EntryBoundaryFixture.ready = true
                assertFalse(invokeBoundary(mixin, name, hand, input).isCancelled, "$name must release on full presence")
                EntryBoundaryFixture.ready = false
                assertTrue(invokeBoundary(mixin, name, hand, input).isCancelled, "$name must block a later departure")
            }
        }
    }
    @Test fun `holding Shift bypasses every blocked route only until released`() {
        val mixin = boundary()
        EntryBoundaryFixture.stage = EntryStage.SAFARI
        EntryBoundaryFixture.entryTarget = true
        EntryBoundaryFixture.ready = false
        EntryBoundaryFixture.kick = true
        EntryBoundaryFixture.downtime = true
        try {
            for (name in listOf("guard", "guardAttack", "guardTicket", "guardTicketOn", "guardEntry")) {
                EntryBoundaryFixture.shiftHeld = true
                assertFalse(invokeBoundary(mixin, name, InteractionHand.MAIN_HAND, ContainerInput.entries.first()).isCancelled)
                EntryBoundaryFixture.shiftHeld = false
                assertTrue(invokeBoundary(mixin, name, InteractionHand.MAIN_HAND, ContainerInput.entries.first()).isCancelled)
            }
        } finally { EntryBoundaryFixture.shiftHeld = false }
    }
    @Test fun `entry GUI modifier converts only Shift ticket quick move to a normal selection`() {
        val mixin = boundary()
        val handler = mixin.javaClass.declaredMethods.single { it.name == "entryClickInput" }.apply { isAccessible = true }
        try {
            for (entry in listOf(false, true)) for (shift in listOf(false, true)) for (input in ContainerInput.entries) {
                EntryBoundaryFixture.entryTarget = entry
                EntryBoundaryFixture.shiftHeld = shift
                val expected = if (entry && shift && input == ContainerInput.QUICK_MOVE) ContainerInput.PICKUP else input
                assertEquals(expected, handler.invoke(mixin, input, 0, 0, 0, input, null))
            }
        } finally { EntryBoundaryFixture.shiftHeld = false }
    }
    private fun boundary(): Any {
        val name = "net.johnceo.sparklingmutuals.mixin.SafariManagerMixin"
        val reader = ClassReader(javaClass.classLoader.getResourceAsStream(name.replace('.', '/') + ".class"))
        val writer = ClassWriter(0)
        val guard = "net/johnceo/sparklingmutuals/safari/automation/SafariManagerGuard"
        val fixture = "net/johnceo/sparklingmutuals/safari/automation/EntryBoundaryFixture"
        reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?): MethodVisitor {
                return object : MethodVisitor(Opcodes.ASM9, super.visitMethod(a, n, d, s, e)) {
                    override fun visitFieldInsn(op: Int, owner: String, name: String, desc: String) {
                        super.visitFieldInsn(op, if (owner == guard) fixture else owner, name, desc.replace(guard, fixture))
                    }
                    override fun visitMethodInsn(op: Int, owner: String, name: String, desc: String, itf: Boolean) {
                        super.visitMethodInsn(op, if (owner == guard) fixture else owner, name, desc, itf)
                    }
                }
            }
        }, 0)
        val loader = object : ClassLoader(javaClass.classLoader) {
            fun define() = defineClass(name, writer.toByteArray(), 0, writer.toByteArray().size)
        }
        return loader.define().getDeclaredConstructor().newInstance()
    }
    @Test fun `actual mixin handlers consume blocked input across Manager attack tickets and GUI routes`() {
        val mixin = boundary()
        for (stage in listOf(EntryStage.TORRHUS, EntryStage.SAFARI)) {
            EntryBoundaryFixture.stage = stage
            EntryBoundaryFixture.downtime = true
            EntryBoundaryFixture.kick = true
            EntryBoundaryFixture.ready = false
            for (hand in InteractionHand.entries) {
                for (name in listOf("guard", "guardAttack", "guardTicket", "guardTicketOn", "guardEntry")) {
                    for (input in ContainerInput.entries) for (entry in listOf(true, false)) {
                        EntryBoundaryFixture.entryTarget = entry
                        val callback = invokeBoundary(mixin, name, hand, input)
                        assertEquals(entry, callback.isCancelled, "$stage $name $hand entry=$entry")
                    }
                }
            }
        }
    }
}
