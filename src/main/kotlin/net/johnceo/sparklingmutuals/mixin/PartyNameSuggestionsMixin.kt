package net.johnceo.sparklingmutuals.mixin

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.johnceo.sparklingmutuals.commands.PartyNameSuggestions
import net.minecraft.client.gui.components.CommandSuggestions
import net.minecraft.client.multiplayer.ClientSuggestionProvider
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import java.util.concurrent.CompletableFuture

/** Keep vanilla's suggestion UI and Tab cycling, replacing only known mod command name arguments. */
@Mixin(CommandSuggestions::class)
class PartyNameSuggestionsMixin {
    @WrapOperation(method = ["updateCommandInfo"], at = [At(value = "INVOKE",
        target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;")])
    private fun partyCommand(dispatcher: CommandDispatcher<ClientSuggestionProvider>, parse: ParseResults<ClientSuggestionProvider>,
        cursor: Int, original: Operation<CompletableFuture<Suggestions>>): CompletableFuture<Suggestions> =
        PartyNameSuggestions.chat(parse.reader.string, cursor) ?: original.call(dispatcher, parse, cursor)

    @WrapOperation(method = ["updateCommandInfo"], at = [At(value = "INVOKE",
        target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggest(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;")])
    private fun partyChat(candidates: Iterable<String>, builder: SuggestionsBuilder,
        original: Operation<CompletableFuture<Suggestions>>): CompletableFuture<Suggestions> =
        PartyNameSuggestions.chat(builder.input, builder.input.length) ?: original.call(candidates, builder)
}
