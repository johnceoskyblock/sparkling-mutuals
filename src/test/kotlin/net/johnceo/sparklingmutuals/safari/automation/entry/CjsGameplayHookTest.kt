package net.johnceo.sparklingmutuals.safari.automation.entry

import java.util.UUID
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object CjsTakeoverFixture {
    var flow=CjsEntryFlow()
    fun gameplayInput(window: Long,action: Int) { if(window==42L) flow.inputEvent(action>0) }
}

class CjsGameplayHookTest {
    private data class Hook(val mixin:String,val handler:String,val event:Any,val args:Array<Any>,val target:String,val desc:String)
    @Test fun `native keyboard and mouse hooks latch a brief press without cancelling vanilla input`() {
        val c=EntryFixtures.context()
        val frame=CjsEntryFrame(c,CjsRoomSignal(c,7,setOf("Bluebird")),1,true,0,true,true,false,
            true,true,false,false,UUID(0,99),true,true,CjsApproachOutcome.WAITING,null)
        for((mixin,handler,event,args,target,desc) in listOf(
            Hook("CjsEntryKeyboardMixin","cjEntryKey",KeyEvent(87,0,0),arrayOf(42L,1,KeyEvent(87,0,0)),
                "KeyboardHandler","(JILnet/minecraft/client/input/KeyEvent;)V"),
            Hook("CjsEntryMouseMixin","cjEntryMouse",MouseButtonInfo(0,0),arrayOf(42L,MouseButtonInfo(0,0),1),
                "MouseHandler","(JLnet/minecraft/client/input/MouseButtonInfo;I)V"))) {
            val flow=CjsEntryFlow();CjsTakeoverFixture.flow=flow
            assertTrue(flow.tick(frame,6000) is CjsEntryEffect.Approach)
            val instance=hook(mixin)
            val method=instance.javaClass.declaredMethods.single { it.name==handler }.apply { isAccessible=true }
            val press=CallbackInfo("manual input",true)
            method.invoke(instance,*args,press)
            val release=if(event is KeyEvent) arrayOf(42L,0,event) else arrayOf(42L,event,0)
            method.invoke(instance,*release,CallbackInfo("release",true))
            assertFalse(press.isCancelled,"CJ observes input; vanilla must retain it")
            assertEquals(CjsEntryPhase.CANCELLED,flow.phase)
            assertNull(flow.tick(frame.copy(approach=CjsApproachOutcome.COMPLETE),6100))
            val methods=mutableSetOf<String>()
            ClassReader(javaClass.classLoader.getResourceAsStream("net/minecraft/client/$target.class")).accept(
                object:ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(a:Int,n:String,d:String,s:String?,e:Array<out String>?):MethodVisitor? {
                        methods.add(n+d);return null
                    }
                },ClassReader.SKIP_CODE)
            val targetMethod=if(event is KeyEvent) "keyPress" else "onButton"
            assertTrue(targetMethod+desc in methods,"Hook descriptor must exist on actual Minecraft")
        }
    }
    private fun hook(simple: String): Any {
        val name="net.johnceo.sparklingmutuals.mixin.$simple"
        val source=javaClass.classLoader.getResourceAsStream(name.replace('.','/')+".class")
        assertNotNull(source,"Native gameplay takeover hook is required")
        val methods=mutableListOf<String>()
        val locations=mutableListOf<String>()
        val writer=ClassWriter(0)
        val owner="net/johnceo/sparklingmutuals/safari/automation/entry/CjsSafariEntry"
        val fixture="net/johnceo/sparklingmutuals/safari/automation/entry/CjsTakeoverFixture"
        ClassReader(source).accept(object:ClassVisitor(Opcodes.ASM9,writer) {
            override fun visitMethod(a:Int,n:String,d:String,s:String?,e:Array<out String>?):MethodVisitor =
                object:MethodVisitor(Opcodes.ASM9,super.visitMethod(a,n,d,s,e)) {
                    override fun visitAnnotation(desc:String, visible:Boolean):AnnotationVisitor {
                        val original=super.visitAnnotation(desc,visible)
                        return object:AnnotationVisitor(Opcodes.ASM9,original) {
                            override fun visitArray(name:String):AnnotationVisitor {
                                val downstream=super.visitArray(name)
                                return object:AnnotationVisitor(Opcodes.ASM9,downstream) {
                                    override fun visit(n:String?, value:Any) {
                                        if(desc=="Lorg/spongepowered/asm/mixin/injection/Inject;" && name=="method") methods.add(value.toString())
                                        super.visit(n,value)
                                    }
                                    override fun visitAnnotation(n:String?, d:String):AnnotationVisitor =
                                        object:AnnotationVisitor(Opcodes.ASM9,super.visitAnnotation(n,d)) {
                                            override fun visit(n:String,value:Any) {
                                                if(name=="at" && n=="value") locations.add(value.toString())
                                                super.visit(n,value)
                                            }
                                        }
                                }
                            }
                        }
                    }
                    override fun visitFieldInsn(op:Int,o:String,n:String,d:String) =
                        super.visitFieldInsn(op,if(o==owner) fixture else o,n,d.replace(owner,fixture))
                    override fun visitMethodInsn(op:Int,o:String,n:String,d:String,i:Boolean) =
                        super.visitMethodInsn(op,if(o==owner) fixture else o,n,d,i)
                }
        },0)
        assertEquals(listOf(if(simple=="CjsEntryKeyboardMixin") "keyPress" else "onButton"),methods)
        assertEquals(listOf("HEAD"),locations)
        val loader=object:ClassLoader(javaClass.classLoader) {
            fun define()=defineClass(name,writer.toByteArray(),0,writer.toByteArray().size)
        }
        return loader.define().getDeclaredConstructor().newInstance()
    }
}
