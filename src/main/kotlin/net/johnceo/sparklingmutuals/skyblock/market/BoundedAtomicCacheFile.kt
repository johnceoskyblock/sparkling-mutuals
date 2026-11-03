package net.johnceo.sparklingmutuals.skyblock.market

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*

/** All writers share an OS lock. Only its exclusive owner may recover the known bounded stage. */
internal object BoundedAtomicCacheFile {
    fun write(file: Path,limit: Int,bytes: ByteArray,stillCurrent: ()->Boolean={true}): Boolean = try {
        val stage=file.resolveSibling(file.fileName.toString()+".pending")
        val lock=file.resolveSibling(file.fileName.toString()+".lock")
        if(bytes.size>limit || listOf(file,stage,lock).any(Files::isSymbolicLink))return false
        Files.createDirectories(file.toAbsolutePath().parent)
        FileChannel.open(lock,CREATE,WRITE).use {owner ->
            val held=owner.tryLock() ?: return false
            held.use {
                if(Files.exists(stage)) {
                    if(!Files.isRegularFile(stage) || Files.size(stage)>limit)return false
                    Files.delete(stage) // No cooperating writer can still own this interrupted stage.
                }
                if(Files.exists(file) && Files.size(file)>limit)return false
                var created=false
                try {
                    if(!stillCurrent())return false
                    FileChannel.open(stage,CREATE_NEW,WRITE).use {channel ->
                        created=true
                        val buffer=ByteBuffer.wrap(bytes)
                        while(buffer.hasRemaining())channel.write(buffer)
                        channel.force(true)
                    }
                    if(!stillCurrent())return false
                    Files.move(stage,file,ATOMIC_MOVE,REPLACE_EXISTING)
                    true
                } finally {if(created)runCatching {Files.deleteIfExists(stage)}}
            }
        }
    } catch(_: Exception) {false}
}
