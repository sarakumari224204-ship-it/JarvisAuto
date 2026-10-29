package com.marriagebureau.ai.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName != "com.whatsapp.w4b" && packageName != "com.whatsapp") {
            return
        }

        if (EmergencyStopController.isEmergencyStopped) {
            return
        }

        val pendingReply = PendingAccessibilityReplyHolder.consumePendingReply() ?: return

        val rootNode = rootInActiveWindow ?: return

        // 1. Locate message input field (EditText)
        val inputNodes = rootNode.findAccessibilityNodeInfosByViewId("$packageName:id/entry")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Message") }
            .ifEmpty { findEditTextNodes(rootNode) }

        if (inputNodes.isNotEmpty()) {
            val inputNode = inputNodes.first()
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingReply)
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            // 2. Locate Send button
            val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("$packageName:id/send")
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Send") }
                .ifEmpty { rootNode.findAccessibilityNodeInfosByText("भेजें") }

            if (sendNodes.isNotEmpty()) {
                val sendBtn = sendNodes.first()
                sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    override fun onInterrupt() {}

    private fun findEditTextNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        if (node.className == "android.widget.EditText") {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            list.addAll(findEditTextNodes(child))
        }
        return list
    }

    companion object {
        var isAccessibilityServiceEnabled = false
            internal set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isAccessibilityServiceEnabled = true
    }
}

object PendingAccessibilityReplyHolder {
    @Volatile
    private var pendingReplyText: String? = null

    fun setPendingReply(text: String) {
        pendingReplyText = text
    }

    fun consumePendingReply(): String? {
        val text = pendingReplyText
        pendingReplyText = null
        return text
    }
}
