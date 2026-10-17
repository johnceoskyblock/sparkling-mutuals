package net.johnceo.sparklingmutuals.safari.shots.preview.contacts
object ShywormContactParts {
    const val BODY_TEXTURE="a684e00e7394cb0c84c082e4dbc7c7e91ea67f6bc718a1aace7f99596b65d422"
    /** Fitting evidence: seven separate head-item segments, no stable passenger/ID ownership link.
     * A bounded geometric connection is provisional. Ambiguous components are deliberately omitted. */
    fun support(targets:List<ContactTarget>):Map<String,List<ContactTarget>> {
        // Never truncate a component: a dropped connector can hide a competing head.
        val worms=targets.filter{it.species=="Shyworm" && it.surface==null && it.bounds.valid()}.distinctBy{it.key}
        if(worms.size>64) return emptyMap()
        fun center(t:ContactTarget)=t.bounds.min.lerp(t.bounds.max,.5)
        val positions=worms.associate{it.key to center(it)}
        val neighbors=worms.associate{a->a.key to worms.filter{b->a!==b && positions.getValue(a.key).distanceSquared(positions.getValue(b.key))<=1.75*1.75}}
        val result=linkedMapOf<String,List<ContactTarget>>()
        val uniqueHead=hashMapOf<String,String>()
        val seen=hashSetOf<String>()
        for(start in worms) {
            if(!seen.add(start.key)) continue
            val queue=ArrayDeque<ContactTarget>();queue.add(start);val component=mutableListOf<ContactTarget>()
            while(queue.isNotEmpty()) {
                val t=queue.removeFirst();component.add(t)
                for(next in neighbors.getValue(t.key)) if(seen.add(next.key)) queue.add(next)
            }
            val head=component.singleOrNull{!it.bodyPart} ?: continue
            component.forEach{uniqueHead[it.key]=head.key}
        }
        for(part in worms.filter{it.bodyPart}) {
            val head=uniqueHead[part.key] ?: continue
            val queue=ArrayDeque<List<ContactTarget>>();queue.add(listOf(part))
            val visited=hashSetOf(part.key)
            while(queue.isNotEmpty()) {
                val path=queue.removeFirst();val last=path.last()
                if(last.key==head) { result[part.key]=path;break }
                if(path.size>=8) continue
                for(next in neighbors.getValue(last.key)) if(visited.add(next.key)) queue.add(path+next)
            }
        }
        return result.toMap()
    }
}
