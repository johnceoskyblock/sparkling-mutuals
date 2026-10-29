package net.johnceo.sparklingmutuals.compat

import java.util.concurrent.CompletableFuture
//? if >=26.3 {
/*import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.Minecraft
import org.lwjgl.sdl.SDLDialog
import org.lwjgl.sdl.SDL_DialogFileCallback
import org.lwjgl.system.MemoryUtil
*///?} else {
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
//?}

/** Uses the native dialog library shipped by each Minecraft target. */
object NativeCsvDialog {
    //? if >=26.3 {
    /*private val callbacks = ConcurrentHashMap.newKeySet<SDL_DialogFileCallback>()
    *///?}
    fun choose(): CompletableFuture<String?> {
        val directory = System.getProperty("user.home") + "/Downloads/"
        //? if >=26.3 {
        /*val result = CompletableFuture<String?>()
        val client = Minecraft.getInstance()
        client.execute {
            lateinit var callback: SDL_DialogFileCallback
            callback = SDL_DialogFileCallback.create { _, files, _ ->
                try {
                    if (files == 0L) result.completeExceptionally(IllegalStateException("File dialog failed"))
                    else {
                        val first = MemoryUtil.memGetAddress(files)
                        result.complete(if (first == 0L) null else MemoryUtil.memUTF8(first))
                    }
                } catch (error: Throwable) {
                    result.completeExceptionally(error)
                } finally {
                    callbacks.remove(callback)
                    callback.free()
                }
            }
            callbacks.add(callback)
            try {
                // SDL dialogs are asynchronous and must start on the game's main thread.
                SDLDialog.SDL_ShowOpenFileDialog(callback, 0L, 0L, null, directory, false)
            } catch (error: Throwable) {
                callbacks.remove(callback)
                callback.free()
                result.completeExceptionally(error)
            }
        }
        return result
        *///?} else {
        return CompletableFuture.supplyAsync {
            MemoryStack.stackPush().use { stack ->
                val filters = stack.mallocPointer(1).put(0, stack.UTF8("*.csv"))
                TinyFileDialogs.tinyfd_openFileDialog("Import Warp List CSV (replaces current list)",
                    directory, filters, "CSV files", false)
            }
        }
        //?}
    }
}
