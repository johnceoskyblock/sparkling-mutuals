package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
import net.johnceo.sparklingmutuals.safari.shots.*
object ContactSceneBoundary {
 /** Vanilla fences/walls extend above their owning voxel; query the bounded one-cell neighborhood. */
 fun collisionCells(from:ShotVector,to:ShotVector):List<Triple<Int,Int,Int>> = cells(from,to).flatMap{(x,y,z)->
  (-1..1).flatMap{dx->(-1..1).flatMap{dy->(-1..1).map{dz->Triple(x+dx,y+dy,z+dz)}}}
 }.distinct()
 /** A bounded supercover includes both sides of a diagonal/corner crossing. */
 fun cells(from:ShotVector,to:ShotVector):List<Triple<Int,Int,Int>> {
  if(!from.finite() || !to.finite() || from.distanceSquared(to)>1.0000001) return emptyList()
  val cells=ArrayList<Triple<Int,Int,Int>>()
  for(x in kotlin.math.floor(minOf(from.x,to.x)).toInt()..kotlin.math.floor(maxOf(from.x,to.x)).toInt())
   for(y in kotlin.math.floor(minOf(from.y,to.y)).toInt()..kotlin.math.floor(maxOf(from.y,to.y)).toInt())
    for(z in kotlin.math.floor(minOf(from.z,to.z)).toInt()..kotlin.math.floor(maxOf(from.z,to.z)).toInt()) cells.add(Triple(x,y,z))
  return cells.toList()
 }
 fun preflight(from:ShotVector,to:ShotVector,loaded:(Int,Int,Int)->Boolean,query:(Int,Int,Int)->Unit):Boolean {
  val cells=cells(from,to)
  if(cells.isEmpty() || cells.any{!loaded(it.first,it.second,it.third)}) return false
  cells.forEach{query(it.first,it.second,it.third)};return true
 }
}
