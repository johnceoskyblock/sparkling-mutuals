package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Files
import java.nio.file.Path

/** Local private watches only; <=512KiB each final/staging file, <=1MiB combined. Worker-thread I/O. */
internal class OrderWatchPersistence(private val file: Path) {
    fun read(): String=runCatching {
        if(!Files.isRegularFile(file) || Files.isSymbolicLink(file) || Files.size(file)>LIMIT)return ""
        val bytes=Files.newInputStream(file).use {it.readNBytes(LIMIT+1)}
        if(bytes.size>LIMIT)"" else bytes.toString(Charsets.UTF_8)
    }.getOrDefault("")
    fun write(raw: String): Boolean = BoundedAtomicCacheFile.write(file,LIMIT,raw.toByteArray(Charsets.UTF_8))
    companion object {private const val LIMIT=512*1024}
}
