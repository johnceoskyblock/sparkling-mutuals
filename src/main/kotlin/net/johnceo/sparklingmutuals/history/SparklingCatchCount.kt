package net.johnceo.sparklingmutuals.history

import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

/** SafariUtils-style local catch numbers. Existing history is the only source; never fetch on catch. */
object SparklingCatchCount {
    fun number(entry: SparklingEntry, history: List<SparklingEntry>) = history.count {
        it.at <= entry.at && it.critter.equals(entry.critter,true)
    }
    fun text(entry: SparklingEntry, history: List<SparklingEntry>): String {
        val suffix=if(entry.unique) "NEW!" else "#${number(entry,history).coerceAtLeast(1)}"
        return "Sparkling ${entry.critter} caught! $suffix"
    }
    fun component(entry: SparklingEntry, history: List<SparklingEntry>): Component = LocalChat.component(
        Component.literal(text(entry,history)).withStyle { style ->
            style.withColor(SafariRoster.color(entry.critter) and 0xFFFFFF)
                .withHoverEvent(HoverEvent.ShowText(Component.literal(
                    "Catch number in your saved/imported history, including credited party catches. #2 is the second catch, not the second duplicate.")))
        })
}
