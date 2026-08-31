package it.a2w.automator.data

import it.a2w.automator.model.AssetJob
import java.util.concurrent.CopyOnWriteArrayList

object AutomationStore {
    val jobs = CopyOnWriteArrayList<AssetJob>()
    @Volatile var running = false
    @Volatile var currentIndex = 0

    @Volatile var targetPackage = ""
    @Volatile var fieldLabel = "Descrizione"
    @Volatile var searchTokens = listOf("Cerca", "Ricerca")
    @Volatile var editTokens = listOf("Modifica", "Edit")
    @Volatile var saveTokens = listOf("Salva", "Conferma")

    val log = CopyOnWriteArrayList<String>()

    fun addLog(line: String) {
        log.add(0, line)
        while (log.size > 100) log.removeLast()
    }

    fun currentJob(): AssetJob? = jobs.getOrNull(currentIndex)
}