package it.a2w.automator.data

import android.content.ContentResolver
import android.net.Uri
import org.w3c.dom.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object TableReader {

    fun read(contentResolver: ContentResolver, uri: Uri): List<Map<String, String>> {
        val name = uri.toString().lowercase()
        return if (name.endsWith(".csv")) readCsv(contentResolver, uri) else readXlsx(contentResolver, uri)
    }

    private fun readCsv(cr: ContentResolver, uri: Uri): List<Map<String, String>> {
        cr.openInputStream(uri).use { input ->
            val reader = BufferedReader(InputStreamReader(input))
            val lines = reader.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return emptyList()
            val delimiter = if (lines.first().count { it == ';' } > lines.first().count { it == ',' }) ';' else ','
            val headers = splitCsv(lines.first(), delimiter)
            return lines.drop(1).map { line ->
                val values = splitCsv(line, delimiter)
                headers.mapIndexed { i, h -> h.trim() to values.getOrElse(i) { "" }.trim() }.toMap()
            }
        }
    }

    private fun splitCsv(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> quoted = !quoted
                c == delimiter && !quoted -> {
                    out += sb.toString(); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    private fun readXlsx(cr: ContentResolver, uri: Uri): List<Map<String, String>> {
        val entries = mutableMapOf<String, ByteArray>()
        cr.openInputStream(uri).use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    if (!e.isDirectory) entries[e.name] = zis.readBytes()
                    e = zis.nextEntry
                }
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { bytes ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(bytes.inputStream())
            val nodes = doc.getElementsByTagName("si")
            (0 until nodes.length).map { idx ->
                val si = nodes.item(idx) as Element
                val texts = si.getElementsByTagName("t")
                buildString {
                    for (j in 0 until texts.length) append(texts.item(j).textContent)
                }
            }
        } ?: emptyList()

        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: error("Impossibile trovare il primo foglio Excel")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(sheetBytes.inputStream())

        val rows = doc.getElementsByTagName("row")
        val table = mutableListOf<List<String>>()

        for (r in 0 until rows.length) {
            val row = rows.item(r) as Element
            val cells = row.getElementsByTagName("c")
            val values = mutableMapOf<Int, String>()

            for (c in 0 until cells.length) {
                val cell = cells.item(c) as Element
                val ref = cell.getAttribute("r")
                val col = columnIndex(ref)
                val type = cell.getAttribute("t")
                val vNodes = cell.getElementsByTagName("v")
                val inline = cell.getElementsByTagName("t")
                var value = when {
                    inline.length > 0 -> inline.item(0).textContent
                    vNodes.length == 0 -> ""
                    type == "s" -> sharedStrings.getOrElse(vNodes.item(0).textContent.toIntOrNull() ?: -1) { "" }
                    else -> vNodes.item(0).textContent
                }
                values[col] = value
            }

            val max = values.keys.maxOrNull() ?: -1
            table += (0..max).map { values[it] ?: "" }
        }

        if (table.isEmpty()) return emptyList()
        val headers = table.first().map { it.trim() }
        return table.drop(1).map { row ->
            headers.mapIndexed { i, h -> h to row.getOrElse(i) { "" }.trim() }.toMap()
        }
    }

    private fun columnIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }
        var result = 0
        letters.forEach { ch -> result = result * 26 + (ch.uppercaseChar() - 'A' + 1) }
        return result - 1
    }
}