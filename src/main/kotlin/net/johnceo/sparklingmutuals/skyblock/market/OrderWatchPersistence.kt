package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*

/** Local private watches only; <=512KiB each final/staging file, <=1MiB combined. Worker-thread I/O. */
internal class OrderWatchPersistence(private val file: Path) {
    private val stage=file.resolveSibling(file.fileName.toString()+".pending")
    fun read(): String=runCatching {
        if(!Files.isRegularFile(file) || Files.isSymbolicLink(file) || Files.size(file)>LIMIT)return ""
        val bytes=Files.newInputStream(file).use {it.readNBytes(LIMIT+1)}
        if(bytes.size>LIMIT)"" else bytes.toString(Charsets.UTF_8)
    }.getOrDefault("")
    fun write(raw: String): Boolean {
        val bytes=raw.toByteArray(Charsets.UTF_8)
        if(bytes.size>LIMIT || Files.isSymbolicLink(file) || Files.exists(stage))return false
        if(Files.exists(file) && Files.size(file)>LIMIT)return false
        var created=false
        return try {
            Files.createDirectories(file.toAbsolutePath().parent)
            FileChannel.open(stage,CREATE_NEW,WRITE).use {channel ->
                created=true
                val buffer=ByteBuffer.wrap(bytes)
                while(buffer.hasRemaining())channel.write(buffer)
                channel.force(true)
            }
            Files.move(stage,file,ATOMIC_MOVE,REPLACE_EXISTING)
            true
        } catch(_: Exception) {if(created)runCatching {Files.deleteIfExists(stage)};false}
    }
    companion object {private const val LIMIT=512*1024}
}
