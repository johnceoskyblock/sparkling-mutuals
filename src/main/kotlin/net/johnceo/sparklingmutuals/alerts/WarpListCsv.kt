package net.johnceo.sparklingmutuals.alerts

import java.io.PushbackReader
import java.io.Reader

/** Only the header and its ten name rows belong to a Warp List export. */
object WarpListCsv {
    fun read(source: Reader): Map<String, List<String>> {
        val rows = records(source)
        val headers = rows.firstOrNull() ?: throw IllegalArgumentException("CSV is empty.")
        val known = WarpList.critters.associateBy(String::lowercase)
        val columns = headers.mapIndexedNotNull { index, header ->
            known[header.trim().removePrefix("\uFEFF").trim().lowercase()]?.let { index to it }
        }
        require(columns.isNotEmpty()) { "No supported critter headers found in row 1." }
        require(columns.map { it.second }.distinct().size == columns.size) { "Duplicate critter headers in row 1." }
        val result = WarpList.critters.associateWith { emptyList<String>() }.toMutableMap()
        columns.forEach { (column, critter) ->
            result[critter] = rows.drop(1).mapNotNull { it.getOrNull(column)?.trim() }
                .filter { it.matches(Regex("[A-Za-z0-9_]{1,16}")) }.distinctBy(String::lowercase).take(10)
        }
        return result
    }

    private fun records(source: Reader): List<List<String>> {
        val input = PushbackReader(source, 1)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closedQuote = false
        var started = false
        var characters = 0
        fun endField() { row.add(field.toString()); field.setLength(0); closedQuote = false; started = false }
        while (rows.size < 11) {
            val code = input.read()
            if (code == -1) {
                require(!quoted) { "Unclosed CSV quote in rows 1-11." }
                if (started || closedQuote || field.isNotEmpty() || row.isNotEmpty()) { endField(); rows.add(row) }
                break
            }
            require(++characters <= 1_048_576) { "CSV rows 1-11 are too large." }
            val c = code.toChar()
            // UTF-8 BOM may precede a quoted first header.
            if (characters == 1 && c == '\uFEFF') continue
            if (quoted) {
                if (c == '"') {
                    val next = input.read()
                    if (next == '"'.code) field.append('"')
                    else { quoted = false; closedQuote = true; if (next != -1) input.unread(next) }
                } else field.append(c)
                continue
            }
            when (c) {
                ',' -> endField()
                '\r', '\n' -> {
                    if (c == '\r') { val next = input.read(); if (next != -1 && next != '\n'.code) input.unread(next) }
                    endField(); rows.add(row); row = mutableListOf()
                }
                '"' -> {
                    require(!started && field.isEmpty() && !closedQuote) { "Invalid CSV quote in rows 1-11." }
                    quoted = true; started = true
                }
                else -> {
                    require(!closedQuote || c.isWhitespace()) { "Invalid text after a CSV quote." }
                    if (!closedQuote) { field.append(c); started = true }
                }
            }
        }
        return rows
    }
}
