package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.mixin.ShotUseMixin
import net.johnceo.sparklingmutuals.mixin.ShotPacketMixin
import net.minecraft.world.InteractionResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class ShotObservationBoundaryTest {
    @Test fun `live entity map cleared between bounded batches does not stop recording`() {
        // EntityLookup uses this Java map. Its retained iterator can return null after a later tick clears it.
        val entities=it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap<String>()
        entities.put(1,"first");entities.put(2,"removed-second");entities.put(3,"removed-third")
        val iterator=entities.values.iterator();val visited=mutableListOf<String>()
        val budget=ShotTestFixtures.Budget();val sink=ShotTestFixtures.Sink(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),sink,budget)
        val adapter=ShotObservationAdapter(ShotTestFixtures.zero) { stamp,o -> session.accept(stamp,o) }
        assertTrue(adapter.scanBatch(iterator,1,ShotStamp(0,0)) { entity:String -> visited.add(entity) })
        entities.clear()
        // Empty slots still consume the allowance, so each call advances only one retained slot.
        assertTrue(adapter.scanBatch(iterator,1,ShotStamp(50_000,1)) { entity:String -> visited.add(entity) })
        assertFalse(adapter.scanBatch(iterator,1,ShotStamp(100_000,2)) { entity:String -> visited.add(entity) })
        assertEquals(listOf("first"),visited);assertTrue(session.status().recording)
        session.accept(ShotStamp(200_000,4),ShotTestFixtures.input("s1"))
        val capsule=ShotObservation.Capsule("c1",1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero,discovery=ShotDiscovery.SPAWN)
        session.accept(ShotStamp(250_000,5),capsule)
        session.accept(ShotStamp(300_000,6),ShotObservation.Packet("c1",ShotUpdateKind.POSITION_SYNC,listOf(ShotPathPoint(ShotVector(0.0,0.0,1.0)))))
        session.accept(ShotStamp(350_000,7),capsule.copy(action=ShotEntityAction.REMOVED,removal=ShotRemoval.SERVER_REMOVE))
        session.stop(ShotStamp(400_000,8),ShotStopReason.USER_STOP)
        assertTrue(sink.records().last()["complete"].asBoolean)
        assertFalse(sink.records().last()["gaps"].asBoolean)
        assertEquals(1,sink.records().count { it["kind"]?.asString=="packet" })
        assertEquals(0,budget.usedBytes)
    }
    @Test fun `real discovery failure still escapes scan for recorder diagnostics`() {
        val adapter=ShotObservationAdapter(ShotTestFixtures.zero) { _,_ -> }
        val failure=NullPointerException("real callback failure")
        val actual=assertThrows(NullPointerException::class.java) {
            adapter.scanBatch(listOf("valid entity").iterator(),1,ShotStamp(0,0)) { throw failure }
        }
        assertSame(failure,actual)
    }
    @Test fun `lossless multi batch scan before input does not poison a later complete trace`() {
        val budget=ShotTestFixtures.Budget();val sink=ShotTestFixtures.Sink(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),sink,budget)
        val adapter=ShotObservationAdapter(ShotTestFixtures.zero) { stamp,o -> session.accept(stamp,o) }
        val iterator=(1..257).iterator();val visited=mutableListOf<Int>()
        assertTrue(adapter.scanBatch(iterator,256,ShotStamp(0,0),visited::add))
        assertFalse(adapter.scanBatch(iterator,256,ShotStamp(50_000,1),visited::add));assertEquals(257,visited.size)
        session.accept(ShotStamp(1_000_000,20),ShotTestFixtures.input("s1"))
        val capsule=ShotObservation.Capsule("c1",1,ShotVariant.ORDINARY,ShotEntityAction.DISCOVERED,ShotTestFixtures.zero,discovery=ShotDiscovery.SPAWN)
        session.accept(ShotStamp(1_100_000,22),capsule)
        session.accept(ShotStamp(2_000_000,40),capsule.copy(action=ShotEntityAction.REMOVED,removal=ShotRemoval.SERVER_REMOVE))
        session.stop(ShotStamp(3_000_000,60),ShotStopReason.USER_STOP)
        assertTrue(sink.records().last()["complete"].asBoolean);assertFalse(sink.records().last()["gaps"].asBoolean)
    }
    @Test fun `actual pending evidence overflow remains incomplete even before an input`() {
        val cache=ShotEntityCache();repeat(257) { cache.pendingSpawn(it,"private$it",ShotTestFixtures.zero,0) }
        val budget=ShotTestFixtures.Budget();val sink=ShotTestFixtures.Sink(budget)
        val session=ShotSession(ShotTestFixtures.metadata(),ShotLimits(),sink,budget)
        cache.takeGaps().forEach { session.accept(ShotStamp(0,0),ShotObservation.Gap(it)) }
        session.accept(ShotStamp(1_000_000,20),ShotTestFixtures.input("s1"));session.stop(ShotStamp(2_000_000,40),ShotStopReason.USER_STOP)
        assertFalse(sink.records().last()["complete"].asBoolean);assertTrue(sink.records().last()["gaps"].asBoolean)
    }
    @Test fun `packet boundary retires reused id before translating the new variant without a tick`() {
        val cache=ShotEntityCache();val emitted=mutableListOf<ShotObservation>()
        val adapter=ShotObservationAdapter(ShotTestFixtures.zero,cache) { _,o -> emitted.add(o) }
        val old=cache.discover(7,"old-private",ShotVariant.ORDINARY,null,ShotDiscovery.SPAWN,0)!!
        old.base=ShotTestFixtures.zero
        val fresh=adapter.packetEntry(7,"new-private",ShotStamp(100,0)) {
            cache.pendingSpawn(7,"new-private",ShotVector(10.0,0.0,0.0),100)
            cache.discover(7,"new-private",ShotVariant.MASTERFUL,null,ShotDiscovery.SPAWN,100)
        }!!
        assertNotEquals(old.token,fresh.token);assertEquals(ShotVariant.MASTERFUL,fresh.variant)
        val retired=emitted.single() as ShotObservation.Capsule
        assertEquals(old.token,retired.token);assertEquals(ShotRemoval.UNLOADED,retired.removal)
        val spawn=ShotPacketAdapter.translate(fresh,ShotPacketUpdate.Spawn(7,"new-private",ShotVector(10.0,0.0,0.0)),ShotTestFixtures.zero,100)
        val again=adapter.packetEntry(7,"new-private",ShotStamp(110,0)) { error("same lifetime must survive") }!!
        val move=ShotPacketAdapter.translate(again,ShotPacketUpdate.Move(7,listOf(ShotPathPoint(ShotVector(1.0,0.0,0.0)))),ShotTestFixtures.zero,110)
        assertEquals(fresh.token,spawn.token);assertEquals(fresh.token,move.token)
        assertEquals(ShotVector(11.0,0.0,0.0),move.resolvedEnd);assertEquals(1,emitted.size)
    }
    @Test fun `compiled item return callbacks preserve pass fail and success without a recorder`() {
        ShotObservationHooks.bind(null,{ error("off must not read a clock") },{ throw AssertionError(it) })
        val mixin=ShotUseMixin()
        for(name in listOf("airReturn","blockReturn")) {
            val method=ShotUseMixin::class.java.declaredMethods.single { it.name==name }.also { it.isAccessible=true }
            for(result in listOf(InteractionResult.PASS,InteractionResult.FAIL,InteractionResult.SUCCESS)) {
                val callback=CallbackInfoReturnable("use",true,result)
                val args=arrayOfNulls<Any>(method.parameterCount);args[args.lastIndex]=callback
                method.invoke(mixin,*args)
                assertSame(result,callback.returnValue);assertFalse(callback.isCancelled)
            }
        }
    }
    @Test fun `all seven compiled packet callbacks remain passive while unbound`() {
        ShotObservationHooks.bind(null,{ error("off") },{ throw AssertionError(it) })
        // Real immutable motion packet traverses the actual compiled callback without inspecting the world.
        val packet=net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(7,net.minecraft.world.phys.Vec3.ZERO)
        val method=ShotPacketMixin::class.java.declaredMethods.single { it.name=="motion" }.also { it.isAccessible=true }
        val callback=org.spongepowered.asm.mixin.injection.callback.CallbackInfo("motion",true)
        method.invoke(ShotPacketMixin(),packet,callback)
        assertFalse(callback.isCancelled)
        assertEquals(7,ShotPacketMixin::class.java.declaredMethods.size)
    }
}
