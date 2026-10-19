package net.johnceo.sparklingmutuals.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.johnceo.sparklingmutuals.LocalChat
import net.johnceo.sparklingmutuals.safari.shots.*

object ShotCommands {
    val scenarios=ShotScenario.entries.map { it.name.lowercase() }
    val splits=ShotSplit.entries.map { it.name.lowercase() }
    private val outcomes=ShotManualOutcome.entries.map { it.name.lowercase() }
    fun validToken(token:String)=token.matches(Regex("[A-Za-z0-9_-]{1,48}"))
    private fun reply(context:CommandContext<FabricClientCommandSource>,result:ShotCommandResult):Int {
        context.source.sendFeedback(LocalChat.component(result.message));return if(result.ok) 1 else 0
    }
    private fun help(context:CommandContext<FabricClientCommandSource>)=reply(context,ShotCommandResult(true,
        "Shot recorder: local, opt-in observations; no live prediction. Files: ${ShotRecorder.directory}\n"+
        "/sm shots start [10–600 seconds] | status | stop | export\n"+
        "/sm shots label <scenario> <fit|validation|unassigned> | mark <shot-id> <hit|capture|escape|miss|unknown>\n"+
        "Scenarios: ${scenarios.joinToString(", ")}. Manual marks are annotations, not server confirmations."))
    private fun choice(name:String,values:List<String>)=argument(name,StringArgumentType.word()).suggests { _,builder ->
        values.filter { it.startsWith(builder.remainingLowerCase) }.forEach(builder::suggest);builder.buildFuture()
    }
    fun register(root:LiteralArgumentBuilder<FabricClientCommandSource>) {
        root.then(literal("shots").executes(::help)
            .then(literal("help").executes(::help))
            .then(literal("start").executes { reply(it,ShotRecorder.start()) }
                .then(argument("seconds",IntegerArgumentType.integer(10,600)).executes { reply(it,ShotRecorder.start(IntegerArgumentType.getInteger(it,"seconds"))) }))
            .then(literal("status").executes { reply(it,ShotCommandResult(true,ShotRecorder.describe())) })
            .then(literal("stop").executes { reply(it,ShotRecorder.stop(ShotStopReason.USER_STOP)) })
            .then(literal("export").executes { reply(it,ShotRecorder.stop(ShotStopReason.EXPORT)) })
            .then(literal("label").then(choice("scenario",scenarios).then(choice("split",splits).executes {
                val scenario=ShotScenario.entries.firstOrNull { v -> v.name.equals(StringArgumentType.getString(it,"scenario"),true) }
                val split=ShotSplit.entries.firstOrNull { v -> v.name.equals(StringArgumentType.getString(it,"split"),true) }
                reply(it,if(scenario==null || split==null) ShotCommandResult(false,"Choose an enumerated scenario and split from /sm shots help.") else ShotRecorder.label(scenario,split))
            })))
            .then(literal("mark").then(argument("shot-id",StringArgumentType.word()).then(choice("outcome",outcomes).executes {
                val token=StringArgumentType.getString(it,"shot-id")
                val outcome=ShotManualOutcome.entries.firstOrNull { v -> v.name.equals(StringArgumentType.getString(it,"outcome"),true) }
                reply(it,if(!validToken(token) || outcome==null) ShotCommandResult(false,"Use a shot ID and enumerated outcome from /sm shots help.") else ShotRecorder.mark(token,outcome))
            }))))
    }
}
