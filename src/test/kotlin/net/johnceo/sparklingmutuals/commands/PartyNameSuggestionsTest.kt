package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objectweb.asm.*

class PartyNameSuggestionsTest {
    private val names = listOf("JohnCEO", "JohnFounder", "Alice", "alice", "Bob")
    @Test fun `sparkling suggests party names and keyword then completes only the next comparison name`() {
        val input = "/sparkling jO"
        val first = PartyNameSuggestions.sparkling(SuggestionsBuilder(input, 11), names).join()
        assertEquals(listOf("JohnCEO", "JohnFounder"), first.list.map { it.text })
        assertEquals("/sparkling JohnCEO", first.list.first().apply(input))
        val multiple = "/sparkling Alice j"
        val next = PartyNameSuggestions.sparkling(SuggestionsBuilder(multiple, 11), names).join()
        assertEquals("/sparkling Alice JohnCEO", next.list.first().apply(multiple))
        assertTrue(PartyNameSuggestions.sparkling(SuggestionsBuilder("/sparkling Alice a", 11), names).join().isEmpty)
        assertEquals(listOf("party"), PartyNameSuggestions.sparkling(SuggestionsBuilder("/sparkling par", 11), names).join().list.map { it.text })
        assertTrue(PartyNameSuggestions.sparkling(SuggestionsBuilder("/sparkling party ", 11), names).join().isEmpty)
    }
    @Test fun `party command names complete through supported chat wrappers with correct replacement ranges`() {
        for (prefix in listOf("", "/pc ", "/pchat ", "/p chat ", "/party chat ")) {
            for (command in listOf("!missing ", "!ticket ", "!tickets ", "!pb doom ", "!pb wumpa ", "!pb forest ", "!pb cavern ", "!pb icy ", "!pb haunted ")) {
                val input = prefix + command + "jo"
                val suggestions = PartyNameSuggestions.chat(input, input.length, names)!!.join()
                assertEquals(listOf("JohnCEO", "JohnFounder"), suggestions.list.map { it.text }, input)
                assertEquals(prefix + command + "JohnCEO", suggestions.list.first().apply(input))
            }
        }
        val empty = PartyNameSuggestions.chat("!missing ", 9, names)!!.join().list.map { it.text }
        assertEquals(listOf("Alice", "Bob", "JohnCEO", "JohnFounder"), empty)
        assertTrue(PartyNameSuggestions.chat("!pb doom d", 10, names)!!.join().list.any { it.text == "duo" })
        for (input in listOf("hello jo", "/msg Alice jo", "/p invite jo", "!m fo", "!since sp", "!dt jo", "!missing Alice extra"))
            assertNull(PartyNameSuggestions.chat(input, input.length, names), input)
    }
    @Test fun `chat suggestion hook targets exist in actual Minecraft bytecode`() {
        val targets = mutableSetOf<String>()
        ClassReader(javaClass.classLoader.getResourceAsStream("net/minecraft/client/gui/components/CommandSuggestions.class")).accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?): MethodVisitor? =
                    if (n != "updateCommandInfo") null else object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitMethodInsn(op: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            targets += "L$owner;$name$desc"
                        }
                    }
            }, 0)
        assertTrue("Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;" in targets)
        assertTrue("Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;" in targets)
    }
}
