package it.a2w.automator.automation

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object NodeUtils {

    fun flatten(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val out = mutableListOf<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo) {
            out += n
            for (i in 0 until n.childCount) n.getChild(i)?.let(::walk)
        }
        walk(root)
        return out
    }

    fun findByAnyText(root: AccessibilityNodeInfo?, tokens: List<String>): AccessibilityNodeInfo? {
        val lowered = tokens.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        return flatten(root).firstOrNull { node ->
            val values = listOf(node.text?.toString(), node.contentDescription?.toString())
                .filterNotNull().map { it.lowercase() }
            values.any { v -> lowered.any { t -> v.contains(t) } }
        }
    }

    fun findEditable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? =
        flatten(root).firstOrNull {
            it.isEditable || it.className?.toString()?.contains("EditText", true) == true
        }

    fun findEditableNearLabel(root: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        val nodes = flatten(root)
        val idx = nodes.indexOfFirst {
            (it.text?.toString() ?: "").contains(label, true) ||
            (it.contentDescription?.toString() ?: "").contains(label, true)
        }
        if (idx >= 0) {
            for (i in (idx + 1)..minOf(idx + 8, nodes.lastIndex)) {
                val n = nodes[i]
                if (n.isEditable || n.className?.toString()?.contains("EditText", true) == true) return n
            }
        }
        return findEditable(root)
    }

    fun setText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun click(node: AccessibilityNodeInfo?): Boolean {
        var n = node
        repeat(5) {
            if (n == null) return false
            if (n!!.isClickable && n!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n!!.parent
        }
        return false
    }
}