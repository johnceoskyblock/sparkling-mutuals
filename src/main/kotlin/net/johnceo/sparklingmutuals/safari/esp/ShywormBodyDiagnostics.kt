package net.johnceo.sparklingmutuals.safari.esp

import com.google.gson.Gson
import net.johnceo.sparklingmutuals.compat.*
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.shots.ShotVector
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Explicit local diagnostics only. Nearby candidates are not asserted to belong to a worm. */
object ShywormBodyDiagnostics {
    private val gson=Gson()
    private var nextSnapshot=0L
    private fun vector(v:net.minecraft.world.phys.Vec3)=ShotVector(v.x,v.y,v.z)
    private fun finite(vararg v:Float)=v.toList().takeIf{it.all(Float::isFinite)}
    private fun ShotVector.safe()=takeIf{x.isFinite() && y.isFinite() && z.isFinite()}
    private fun item(stack:ItemStack):Map<String,Any?> = mapOf(
        "item" to BuiltInRegistries.ITEM.getKey(stack.item).toString(),
        "texture" to SafariEsp.texture(stack.get(DataComponents.PROFILE)?.partialProfile())?.takeIf{it.matches(Regex("[a-f0-9]{64}"))},
        "dye" to stack.get(DataComponents.DYED_COLOR)?.rgb())

    internal fun capture(client:Minecraft,entities:List<Entity>):List<String> {
        val player=client.player ?: return emptyList()
        val level=client.level ?: return emptyList()
        if(!SafariAssist.inSafari) return emptyList()
        val origin=vector(player.eyePosition)
        val source=entities.take(ShywormBodySelector.SCAN_LIMIT).filter{it !is Player && !it.isRemoved && level.getEntity(it.id)===it}
        val raw=source.map { e -> ShywormBodyEntity(e.id,e.type.toShortString(),vector(e.position()),
            e is ArmorStand && SafariEspRules.identify(SafariEsp.describe(e))?.name=="Shyworm",e.vehicle?.id,e.passengers.take(16).map{it.id}) }
        val selection=ShywormBodySelector.select(raw,origin)
        val snapshot=++nextSnapshot
        val rows=mutableListOf(gson.toJson(mapOf("version" to 1,"kind" to "snapshot","snapshot" to snapshot,
            "capturedAtMillis" to System.currentTimeMillis(),
            "worldTick" to level.gameTime,"scanned" to entities.size.coerceAtMost(ShywormBodySelector.SCAN_LIMIT),
            "scanTruncated" to (entities.size>ShywormBodySelector.SCAN_LIMIT),"headIds" to selection.heads,
            "headsOmitted" to selection.headsOmitted,"candidateCount" to selection.candidates.size,
            "candidatesOmitted" to selection.candidatesOmitted,"coordinates" to "player_eye_relative",
            "membership" to "candidates_only")))
        rows.add(gson.toJson(mapOf("version" to 1,"kind" to "contact_scene","snapshot" to snapshot,
            "state" to net.johnceo.sparklingmutuals.safari.shots.preview.contacts.CapsuleContactScene.debugSummary())))
        val byId=source.associateBy{it.id}
        for(part in selection.candidates) {
            val e=byId[part.id] ?: continue
            val b=e.boundingBox
            val row=linkedMapOf<String,Any?>("version" to 1,"kind" to "candidate","snapshot" to snapshot,
                "id" to e.id,"type" to part.type,"identifiedHead" to part.head,"position" to (part.position-origin).safe(),
                "boundsMin" to (ShotVector(b.minX,b.minY,b.minZ)-origin).safe(),
                "boundsMax" to (ShotVector(b.maxX,b.maxY,b.maxZ)-origin).safe(),
                "rotation" to finite(e.yRot,e.xRot),"velocity" to vector(e.deltaMovement).safe(),
                "invisible" to e.isInvisible,"vehicle" to e.vehicle?.id,"passengers" to e.passengers.take(16).map{it.id},
                "passengersTruncated" to (e.passengers.size>16))
            if(e is ArmorStand) {
                row["small"]=e.isSmall;row["marker"]=e.isMarker
                row["headPose"]=finite(e.headPose.x,e.headPose.y,e.headPose.z)
                row["bodyPose"]=finite(e.bodyPose.x,e.bodyPose.y,e.bodyPose.z)
                row["leftArmPose"]=finite(e.leftArmPose.x,e.leftArmPose.y,e.leftArmPose.z)
                row["rightArmPose"]=finite(e.rightArmPose.x,e.rightArmPose.y,e.rightArmPose.z)
                row["leftLegPose"]=finite(e.leftLegPose.x,e.leftLegPose.y,e.leftLegPose.z)
                row["rightLegPose"]=finite(e.rightLegPose.x,e.rightLegPose.y,e.rightLegPose.z)
                row["equipment"]=EquipmentSlot.entries.take(8).map{mapOf("slot" to it.name.lowercase(),"appearance" to item(e.getItemBySlot(it)))}
            }
            if(e is Display) {
                row["modelVisible"]=!SafariEsp.describe(e).invisible
                val t=e.renderState()?.transformation()?.get(1f)
                if(t!=null) {
                    row["translation"]=finite(t.translation().x(),t.translation().y(),t.translation().z())
                    row["scale"]=finite(t.scale().x(),t.scale().y(),t.scale().z())
                    row["leftRotation"]=finite(t.leftRotation().x(),t.leftRotation().y(),t.leftRotation().z(),t.leftRotation().w())
                    row["rightRotation"]=finite(t.rightRotation().x(),t.rightRotation().y(),t.rightRotation().z(),t.rightRotation().w())
                }
                row["interpolation"]=listOf(e.transformationInterpolationDelay,e.transformationInterpolationDuration,e.posRotInterpolationDuration)
                if(e is Display.ItemDisplay) { row["appearance"]=item(e.itemStack);row["itemTransform"]=e.itemTransform.name.lowercase() }
                if(e is Display.BlockDisplay) {
                    row["block"]=BuiltInRegistries.BLOCK.getKey(e.blockState.block).toString()
                    row["blockState"]=e.blockState.toString().take(512)
                }
            }
            rows.add(gson.toJson(row))
        }
        return rows
    }
}
