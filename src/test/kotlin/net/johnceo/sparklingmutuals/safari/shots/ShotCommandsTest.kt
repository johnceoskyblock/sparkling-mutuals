package net.johnceo.sparklingmutuals.safari.shots

import net.johnceo.sparklingmutuals.commands.ShotCommands
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotCommandsTest {
    @Test fun `actual command tree exposes bounded duration and enumerated scenario suggestions`() {
        val root=net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("sm")
        ShotCommands.register(root)
        val dispatcher=com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>()
        val node=dispatcher.register(root)
        assertEquals(setOf("help","start","status","stop","export","label","mark"),node.getChild("shots").children.map { it.name }.toSet())
        val seconds=node.getChild("shots").getChild("start").getChild("seconds") as com.mojang.brigadier.tree.ArgumentCommandNode<*,*>
        val type=seconds.type as com.mojang.brigadier.arguments.IntegerArgumentType
        assertEquals(10,type.minimum);assertEquals(600,type.maximum)
        val suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("sm shots label stationary_",null)).get().list.map { it.text }
        assertEquals(setOf("stationary_level","stationary_up","stationary_down"),suggestions.toSet())
    }
    @Test fun `command choices are enumerated and imported paths are never shot tokens`() {
        assertTrue(ShotCommands.scenarios.contains("stationary_level"))
        assertEquals(listOf("fit","validation","unassigned"),ShotCommands.splits)
        assertFalse(ShotCommands.validToken("../../private"));assertFalse(ShotCommands.validToken("a b"))
        assertTrue(ShotCommands.validToken("s12"));assertFalse(ShotCommands.validToken("a".repeat(49)))
    }
}
