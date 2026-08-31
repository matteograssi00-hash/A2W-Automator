import it.a2w.automator.automation.NodeUtils.click
import it.a2w.automator.automation.NodeUtils.findByAnyText
import it.a2w.automator.automation.NodeUtils.findEditable
import it.a2w.automator.automation.NodeUtils.findEditableNearLabel
import it.a2w.automator.automation.NodeUtils.setText

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import it.a2w.automator.data.AutomationStore
import it.a2w.automator.automation.NodeUtils.*

class A2WAccessibilityService : AccessibilityService() {
    private enum class Step { HOME, MENU, ASSET_MENU, SEARCH, SUBMIT_SEARCH, OPEN_RESULT, EDIT_INFO, EDIT_FORM, FILL_DESCRIPTION, SAVE, VERIFY, NEXT_HOME }
    private var step = Step.HOME
    private var busy = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!AutomationStore.running || busy) return
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        if (AutomationStore.targetPackage.isNotBlank() && !pkg.equals(AutomationStore.targetPackage, true)) return
        busy = true
        handler.postDelayed({ try { runStep() } finally { busy = false } }, 650)
    }

    private fun runStep() {
        val root = rootInActiveWindow ?: return
        val job = AutomationStore.currentJob() ?: return finishAll()
        when(step) {
            Step.HOME -> { step = Step.MENU; AutomationStore.addLog("HOME A2W") }
            Step.MENU -> {
                val menu = findByAnyText(root, listOf("Menu", "Apri menu", "Navigazione"))
                if (click(menu) || tapPercent(.07f,.07f)) step = Step.ASSET_MENU
            }
            Step.ASSET_MENU -> {
                val asset = findByAnyText(root, listOf("Asset"))
                if (click(asset)) { AutomationStore.addLog("MENU > ASSET"); step = Step.SEARCH }
            }
            Step.SEARCH -> {
                val field = findEditableNearLabel(root,"Codice") ?: findEditable(root)
                if (setText(field, job.asset)) { AutomationStore.addLog("CERCA ${job.asset}"); step = Step.SUBMIT_SEARCH }
            }
            Step.SUBMIT_SEARCH -> {
                // Search button may be keyboard IME or icon without accessible text.
                val search = findByAnyText(root, listOf("Cerca","Search"))
                if (click(search) || tapPercent(.94f,.92f)) step = Step.OPEN_RESULT
            }
            Step.OPEN_RESULT -> {
                val result = findByAnyText(root,listOf(job.asset))
                if (click(result)) { AutomationStore.addLog("APRO ${job.asset}"); step = Step.EDIT_INFO }
            }
            Step.EDIT_INFO -> {
                // In RIEPILOGO the edit/details flow is reached through the asset card, then pencil.
                val code = findByAnyText(root,listOf(job.asset))
                if (click(code) || tapPercent(.50f,.18f)) step = Step.EDIT_FORM
            }
            Step.EDIT_FORM -> {
                val edit = findByAnyText(root,listOf("Modifica","Edit"))
                if (click(edit) || tapPercent(.52f,.94f)) { AutomationStore.addLog("MODIFICA"); step = Step.FILL_DESCRIPTION }
            }
            Step.FILL_DESCRIPTION -> {
                val desc = findEditableNearLabel(root,"Descrizione")
                if (setText(desc, job.value)) { AutomationStore.addLog("DESCRIZIONE = ${job.value}"); step = Step.SAVE }
            }
            Step.SAVE -> {
                val save = findByAnyText(root,listOf("Salva","Save"))
                if (click(save) || tapPercent(.52f,.94f)) step = Step.VERIFY
            }
            Step.VERIFY -> {
                val hasCode = findByAnyText(root,listOf(job.asset)) != null
                val hasDescription = findByAnyText(root,listOf(job.value)) != null
                if (hasCode && hasDescription) {
                    job.status="OK"; AutomationStore.addLog("OK ${job.asset} - VERIFICATO"); step=Step.NEXT_HOME
                }
            }
            Step.NEXT_HOME -> {
                // User-confirmed sequence: after save A2W returns to RIEPILOGO; press the house icon.
                val home = findByAnyText(root,listOf("Home","Casa"))
                if (click(home) || tapPercent(.07f,.07f)) {
                    AutomationStore.currentIndex++
                    if (AutomationStore.currentIndex >= AutomationStore.jobs.size) finishAll() else step=Step.MENU
                }
            }
        }
    }

    private fun tapPercent(xp: Float, yp: Float): Boolean {
        val dm = resources.displayMetrics
        val p = Path().apply { moveTo(dm.widthPixels*xp, dm.heightPixels*yp) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,80)).build()
        return dispatchGesture(g,null,null)
    }

    private fun finishAll(){ AutomationStore.running=false; AutomationStore.addLog("FINE: ${AutomationStore.jobs.size} asset") }
    override fun onInterrupt(){ AutomationStore.running=false; AutomationStore.addLog("INTERROTTO") }
}
