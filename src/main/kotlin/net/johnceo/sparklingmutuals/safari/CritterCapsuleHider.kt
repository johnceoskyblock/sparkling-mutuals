/*
 * Adapted from SkyHanni 9.1.0 CritterCapsuleHider.kt (LGPL-2.1).
 * Copyright SkyHanni contributors. See licenses/SkyHanni-LGPL-2.1.txt.
 * Modified 2026-10-05 for Sparkling Mutuals' Fabric renderer and properties configuration.
 */
package net.johnceo.sparklingmutuals.safari

import net.johnceo.sparklingmutuals.config.ConfigManager
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity

object CritterCapsuleHider {
    fun hidden(entity: Entity, cameraX: Double, cameraY: Double, cameraZ: Double): Boolean {
        if (!SafariAssist.inSafari) return false
        val flying = entity is Display.ItemDisplay
        if (if (flying) !ConfigManager.hideFlyingCapsules else !ConfigManager.hideGroundCapsules) return false
        val stack = when (entity) {
            is Display.ItemDisplay -> entity.itemStack
            is ItemEntity -> entity.item
            else -> return false
        }
        val id = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("id", "")
        return CritterCapsuleRules.hidden(true, flying, id, entity.distanceToSqr(cameraX, cameraY, cameraZ),
            ConfigManager.hideGroundCapsules, ConfigManager.hideFlyingCapsules, ConfigManager.capsuleHideDistance)
    }
}
