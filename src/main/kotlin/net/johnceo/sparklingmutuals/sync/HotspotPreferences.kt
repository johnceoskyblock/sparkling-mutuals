package net.johnceo.sparklingmutuals.sync
/** All four identifiers exactly once, in the selected priority order. Empty preserves server preference. */
object HotspotPreferences {
 fun parse(value:String): List<String>? = value.split(',').map {it.trim().uppercase(java.util.Locale.ROOT)}
  .takeIf {it.size==4 && it.toSet()==setOf("FOREST","CAVERN","ICY","HAUNTED")}
}
