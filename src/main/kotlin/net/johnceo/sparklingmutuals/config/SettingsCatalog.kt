package net.johnceo.sparklingmutuals.config

import java.lang.reflect.Field

/** Reflect only explicit UI metadata; properties and tracking objects remain the owners of behavior. */
class SettingsCatalog(settings: SafariSettings) {
    data class Page(val name: String, val rows: List<Row>)
    data class Row(val owner: Any, val field: Field, val name: String, val description: String,
                   val path: String, val children: List<Row> = emptyList()) {
        val group get()=field.isAnnotationPresent(Accordion::class.java)
        val standalone get()=field.isAnnotationPresent(StandaloneDescription::class.java)
        fun read(): Any?=field.get(owner)
        fun write(value: Any) { field.set(owner,value) }
        fun matches(query: String): Boolean=(name+" "+description+" "+path).contains(query,true)
    }
    val pages=settings.javaClass.declaredFields.mapNotNull { field ->
        field.getAnnotation(Category::class.java)?.let { meta ->
            field.isAccessible=true
            Page(meta.name,rows(field.get(settings),meta.name))
        }
    }
    /** Returns the owning page and ancestor chain without depending on displayed labels. */
    fun locate(target: SettingsTarget): Pair<Int,List<Row>>? {
        val page=pages.indexOfFirst { it.name==target.page }
        if(page<0) return null
        var rows=pages[page].rows
        val chain=mutableListOf<Row>()
        for(field in target.fields) {
            val row=rows.firstOrNull { it.field.name==field } ?: return null
            chain.add(row);rows=row.children
        }
        return if(chain.isEmpty()) null else page to chain
    }
    companion object {
        private fun fields(type: Class<*>): List<Field> =
            (type.superclass?.takeUnless { it==Any::class.java }?.let(::fields) ?: emptyList())+type.declaredFields
        private fun rows(owner: Any,path: String): List<Row> = fields(owner.javaClass).mapNotNull { field ->
            field.getAnnotation(ConfigOption::class.java)?.let { meta ->
                field.isAccessible=true
                Row(owner,field,meta.name,meta.desc,path+" / "+meta.name,
                    if(field.isAnnotationPresent(Accordion::class.java)) rows(field.get(owner),path+" / "+meta.name) else emptyList())
            }
        }
    }
}
