package net.johnceo.sparklingmutuals

import net.johnceo.sparklingmutuals.safari.HideyhoButton
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HideyhoButtonTest {
    @Test fun `custom server action is preserved and URL actions are rejected`() {
        val event = ClickEvent.Custom(net.minecraft.resources.Identifier.fromNamespaceAndPath("hypixel", "dialogue"),
            java.util.Optional.of(net.minecraft.nbt.StringTag.valueOf("opaque response")))
        assertSame(event, HideyhoButton.find(Component.literal("[Sure]").withStyle { it.withClickEvent(event) }))
        val url = ClickEvent.OpenUrl(java.net.URI("https://example.com"))
        assertNull(HideyhoButton.find(Component.literal("[Sure]").withStyle { it.withClickEvent(url) }))
    }
    @Test fun `button can have split text and an inherited click action`() {
        val event = ClickEvent.RunCommand("/quest accept")
        val button = Component.literal("[").append("Sure").append("]").withStyle { it.withClickEvent(event) }
        val message = Component.literal("Select an option: ").append(button)
            .append(Component.literal(" [No thanks...]").withStyle { it.withClickEvent(ClickEvent.RunCommand("/quest decline")) })
        assertEquals(event, HideyhoButton.find(message))
    }
    @Test fun `shortest Sure match wins and unsupported actions are ignored`() {
        val event = ClickEvent.RunCommand("quest accept")
        val message = Component.literal("Select an option: ").withStyle { it.withClickEvent(ClickEvent.RunCommand("wrong")) }
            .append(Component.literal("[Sure]").withStyle { it.withClickEvent(event) })
        assertEquals(event, HideyhoButton.find(message))
        assertNull(HideyhoButton.find(Component.literal("[Sure]")))
        assertNull(HideyhoButton.find(Component.literal("[Unsure]").withStyle { it.withClickEvent(event) }))
    }
}
