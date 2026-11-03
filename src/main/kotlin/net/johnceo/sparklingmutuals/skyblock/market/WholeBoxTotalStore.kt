package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.file.Path
import java.nio.file.Files

internal class WholeBoxTotalStore(private val file: Path) {
    fun read(): String=runCatching {
        if(!Files.isRegularFile(file) || Files.isSymbolicLink(file) || Files.size(file)>WholeBoxLedger.LIMIT)return ""
        val bytes=Files.newInputStream(file).use {it.readNBytes(WholeBoxLedger.LIMIT+1)}
        if(bytes.size>WholeBoxLedger.LIMIT)""else bytes.toString(Charsets.UTF_8)
    }.getOrDefault("")
    fun write(raw: String)=BoundedAtomicCacheFile.write(file,WholeBoxLedger.LIMIT,raw.toByteArray(Charsets.UTF_8))
}
