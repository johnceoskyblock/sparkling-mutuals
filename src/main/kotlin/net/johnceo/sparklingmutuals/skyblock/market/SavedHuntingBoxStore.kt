package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path

/** Private ownership only; <=64 KiB final + <=64 KiB stage, sharing the existing worker. */
internal class SavedHuntingBoxStore(private val file: Path) {
    fun read(): String=runCatching {
        if(!Files.isRegularFile(file) || Files.isSymbolicLink(file) || Files.size(file)>LIMIT)return ""
        val bytes=Files.newInputStream(file).use {it.readNBytes(LIMIT+1)}
        if(bytes.size>LIMIT)"" else bytes.toString(Charsets.UTF_8)
    }.getOrDefault("")
    fun write(raw: String)=BoundedAtomicCacheFile.write(file,LIMIT,raw.toByteArray(Charsets.UTF_8))
    companion object {const val LIMIT=64*1024}
}
