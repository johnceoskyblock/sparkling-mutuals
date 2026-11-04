package net.johnceo.sparklingmutuals.safari.automation.entry

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import sun.misc.Unsafe

/** Native selector/team rules with inert entities; no game, connection or synthetic movement. */
class CjsCrowdedApproachTest {
    private val adapter=CjsApproachInput()
    @Test fun `three overlapping party players with disabled collision do not block the approach`() {
        val team=PlayerTeam(Scoreboard(),"party").apply { collisionRule=Team.CollisionRule.NEVER }
        val self=entity(team,true);val manager=entity(null,false)
        val joiners=listOf(entity(team,false),entity(team,false))
        assertTrue(joiners.none { adapter.blocksApproach(self,manager,it) })
    }
    @Test fun `pushable players still block when their team allows pushing`() {
        val self=entity(null,true);val manager=entity(null,false)
        assertTrue(adapter.blocksApproach(self,manager,entity(null,false)))
        val team=PlayerTeam(Scoreboard(),"blocked").apply { collisionRule=Team.CollisionRule.NEVER }
        assertFalse(adapter.blocksApproach(self,manager,entity(team,false)))
    }
    @Test fun `solid entities remain obstacles even when pushing is disabled`() {
        val team=PlayerTeam(Scoreboard(),"party").apply { collisionRule=Team.CollisionRule.NEVER }
        val self=entity(team,true);val manager=entity(null,false)
        val obstacle=entity(team,false,solid=true)
        assertTrue(adapter.blocksApproach(self,manager,obstacle))
        assertFalse(adapter.blocksApproach(self,manager,manager))
    }
    companion object {
        init { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap() }
        private val unsafe=Unsafe::class.java.getDeclaredField("theUnsafe").let { it.isAccessible=true;it.get(null) as Unsafe }
        private val loader=object:ClassLoader(CjsCrowdedApproachTest::class.java.classLoader) {
            fun define(name:String,bytes:ByteArray)=defineClass(name,bytes,0,bytes.size)
        }
        private val levelClass=ClassWriter(0).let { w ->
            w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,"cjtest/Level",null,"net/minecraft/world/level/Level",null)
            w.visitMethod(Opcodes.ACC_PUBLIC,"isClientSide","()Z",null,null).apply {
                visitCode();visitInsn(Opcodes.ICONST_1);visitInsn(Opcodes.IRETURN);visitMaxs(1,1);visitEnd()
            };w.visitEnd();loader.define("cjtest.Level",w.toByteArray())
        }
        private val level=unsafe.allocateInstance(levelClass) as Level
        private val entityClass=ClassWriter(0).let { w ->
            val owner="cjtest/Player"
            w.visit(Opcodes.V21,Opcodes.ACC_PUBLIC,owner,null,"net/minecraft/world/entity/player/Player",null)
            fun field(name:String,desc:String,method:String) {
                w.visitField(Opcodes.ACC_PUBLIC,name,desc,null,null).visitEnd()
                w.visitMethod(Opcodes.ACC_PUBLIC,method,"()$desc",null,null).apply {
                    visitCode();visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,owner,name,desc)
                    visitInsn(if(desc=="Z") Opcodes.IRETURN else Opcodes.ARETURN);visitMaxs(1,1);visitEnd()
                }
            }
            field("team","Lnet/minecraft/world/scores/PlayerTeam;","getTeam")
            field("world","Lnet/minecraft/world/level/Level;","level")
            field("local","Z","isLocalPlayer")
            for((method,value) in listOf("isAlive" to true,"isPushable" to true,"isSpectator" to false)) {
                w.visitMethod(Opcodes.ACC_PUBLIC,method,"()Z",null,null).apply {
                    visitCode();visitInsn(if(value) Opcodes.ICONST_1 else Opcodes.ICONST_0)
                    visitInsn(Opcodes.IRETURN);visitMaxs(1,1);visitEnd()
                }
            }
            w.visitField(Opcodes.ACC_PUBLIC,"solid","Z",null,null).visitEnd()
            w.visitMethod(Opcodes.ACC_PUBLIC,"canBeCollidedWith","(Lnet/minecraft/world/entity/Entity;)Z",null,null).apply {
                visitCode();visitVarInsn(Opcodes.ALOAD,0);visitFieldInsn(Opcodes.GETFIELD,owner,"solid","Z")
                visitInsn(Opcodes.IRETURN);visitMaxs(1,2);visitEnd()
            }
            // Inert fixture has no vehicle; retain the real Entity.canCollideWith implementation.
            w.visitMethod(Opcodes.ACC_PUBLIC,"isPassengerOfSameVehicle","(Lnet/minecraft/world/entity/Entity;)Z",null,null).apply {
                visitCode();visitInsn(Opcodes.ICONST_0);visitInsn(Opcodes.IRETURN);visitMaxs(1,2);visitEnd()
            }
            w.visitEnd();loader.define("cjtest.Player",w.toByteArray())
        }
        private fun entity(team:PlayerTeam?,local:Boolean,solid:Boolean=false):Player =
            (unsafe.allocateInstance(entityClass) as Player).also {
                entityClass.getField("team").set(it,team);entityClass.getField("world").set(it,level)
                entityClass.getField("local").setBoolean(it,local);entityClass.getField("solid").setBoolean(it,solid)
            }
    }
}
