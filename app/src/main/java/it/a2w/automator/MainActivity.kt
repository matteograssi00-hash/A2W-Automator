package it.a2w.automator

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import it.a2w.automator.data.AutomationStore
import it.a2w.automator.data.TableReader
import it.a2w.automator.databinding.ActivityMainBinding
import it.a2w.automator.model.AssetJob

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var loadedRows: List<Map<String, String>> = emptyList()

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            try {
                loadedRows = TableReader.read(contentResolver, uri)
                b.txtFileInfo.text = "Righe lette: ${loadedRows.size}"
            } catch (e: Exception) {
                b.txtFileInfo.text = "Errore file: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPickFile.setOnClickListener {
            filePicker.launch(arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv",
                "text/comma-separated-values"
            ))
        }

        b.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        b.btnStart.setOnClickListener {
            val assetColumn = b.edtAssetColumn.text.toString().trim()
            val valueColumn = b.edtValueColumn.text.toString().trim()

            val jobs = loadedRows.mapNotNull { row ->
                val asset = row.entries.firstOrNull { it.key.equals(assetColumn, true) }?.value?.trim().orEmpty()
                val value = row.entries.firstOrNull { it.key.equals(valueColumn, true) }?.value?.trim().orEmpty()
                if (asset.isBlank()) null else AssetJob(asset, value)
            }

            AutomationStore.jobs.clear()
            AutomationStore.jobs.addAll(jobs)
            AutomationStore.currentIndex = 0
            AutomationStore.targetPackage = b.edtPackage.text.toString().trim()
            AutomationStore.fieldLabel = b.edtFieldLabel.text.toString().trim()
            AutomationStore.searchTokens = b.edtSearchTokens.text.toString().split(",").map { it.trim() }
            AutomationStore.editTokens = b.edtEditTokens.text.toString().split(",").map { it.trim() }
            AutomationStore.saveTokens = b.edtSaveTokens.text.toString().split(",").map { it.trim() }

            if (jobs.isEmpty()) {
                b.txtStatus.text = "Stato: nessun asset valido"
                return@setOnClickListener
            }

            AutomationStore.running = true
            AutomationStore.addLog("AVVIO: ${jobs.size} asset")
            b.txtStatus.text = "Stato: avviato (${jobs.size} asset)"
            refreshLog()

            // Porta l'utente fuori dall'automator: da qui basta aprire A2W.
            moveTaskToBack(true)
        }

        b.btnStop.setOnClickListener {
            AutomationStore.running = false
            AutomationStore.addLog("STOP MANUALE")
            b.txtStatus.text = "Stato: fermo"
            refreshLog()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
        b.txtStatus.text =
            if (AutomationStore.running) "Stato: in esecuzione • ${AutomationStore.currentIndex + 1}/${AutomationStore.jobs.size}"
            else "Stato: fermo"
    }

    private fun refreshLog() {
        b.txtLog.text = AutomationStore.log.take(25).joinToString("\n")
    }
}