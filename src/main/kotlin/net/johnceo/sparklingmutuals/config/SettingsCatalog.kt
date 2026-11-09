package net.johnceo.sparklingmutuals.config

import java.lang.reflect.Field

/** Reflect only explicit UI metadata; properties and tracking objects remain the owners of behavior. */
class SettingsCatalog(settings: SafariSettings) {
    data class Page(val name: String, val rows: List<Row>)
    data class Row(val owner: Any, val field: Field, val name: String, val description: String,
                   val path: String, val children: List<Row> = emptyList()) {
        val group get()=field.isAnnotationPresent(Accordion::class.java)
        val standalone get()=field.isAnnotationPresent(StandaloneDescription::class.java)
        val primaryToggle get()=children.firstOrNull { it.field.name=="enabled" && it.field.isAnnotationPresent(ConfigEditorBoolean::class.java) }
        fun descriptionText(): net.minecraft.network.chat.Component {
            val text = net.minecraft.network.chat.Component.literal(description.substringBeforeLast('\n', ""))
            val lastLine = net.minecraft.network.chat.Component.literal(description.substringAfterLast('\n'))
            field.getAnnotation(DescriptionColor::class.java)?.let { color ->
                lastLine.withStyle { it.withColor(color.rgb) }
            }
            if (text.string.isNotEmpty()) text.append("\n")
            return text.append(lastLine)
        }
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
    private val locations=java.util.IdentityHashMap<Row,String>().apply {
        val routes=mapOf("General" to "HUDs → Overview","HUDs" to "HUDs → HUDs","Customization" to "HUDs → Appearance",
            "Modes" to "Safari → Modes","Safari helpers" to "Safari → Helpers","Miria contest" to "Safari → Contest",
            "Banner" to "Safari → Banner","Party Interactions" to "Party → Interactions","Sparkle Warping" to "Party → Sparkle Warping",
            "History" to "History → History","API key" to "Advanced → API","SkyBlock" to "Advanced → SkyBlock","ESP" to "ESP → Highlights")
        fun collect(rows: List<Row>,parent: String) { rows.forEach { row ->
            put(row,parent);collect(row.children,"$parent → ${row.name}")
        } }
        pages.forEach { collect(it.rows,routes[it.name] ?: "ESP → ${it.name}") }
    }
    fun location(row: Row): String=locations.getValue(row)

    /** Keep different bindings distinct, but show shared appearance aliases only once. */
    fun search(query: String): List<Row> {
        val words=query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val seen=mutableSetOf<Pair<Any,Field>>()
        return buildList {
            fun visit(rows: List<Row>) { rows.forEach { row ->
                if(!row.group && words.all {
                    (location(row)+" "+row.name+" "+row.description).contains(it,true)
                } && seen.add(row.owner to row.field)) add(row)
                visit(row.children)
            } }
            pages.forEach { visit(it.rows) }
        }
    }
    fun defaultExpanded(): Set<String> = pages.flatMap { it.rows }.filter {
        it.group && it.children.size<=4
    }.mapTo(mutableSetOf()) { it.path }

    fun expansionFor(target: SettingsTarget): Set<String> {
        val (page,chain)=locate(target) ?: return emptySet()
        val paths=chain.filter { it.group }.mapTo(mutableSetOf()) { it.path }
        fun open(row: Row) { if(row.group) { paths.add(row.path);row.children.forEach(::open) } }
        (listOf(pages[page].rows)+chain.dropLast(1).map { it.children }).flatten()
            .filter { it.field.name.contains("appearance",true) }.forEach(::open)
        return paths
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
