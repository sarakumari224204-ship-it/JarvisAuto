package com.marriagebureau.ai.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.marriagebureau.ai.MarriageBureauApp
import com.marriagebureau.ai.ai.FallbackTemplateLLMEngine
import com.marriagebureau.ai.ai.RAGPipeline
import com.marriagebureau.ai.ai.ResponseValidator
import com.marriagebureau.ai.data.repository.CustomerRepositoryImpl
import com.marriagebureau.ai.data.repository.ProfileRepositoryImpl
import com.marriagebureau.ai.knowledge.repository.KnowledgeRepositoryImpl
import com.marriagebureau.ai.teach.repository.TeachRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WhatsAppNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Check if notification is from WhatsApp Business or regular WhatsApp
        if (packageName != "com.whatsapp.w4b" && packageName != "com.whatsapp") {
            return
        }

        if (EmergencyStopController.isEmergencyStopped) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (text.isBlank() || title.isBlank()) return

        serviceScope.launch {
            processIncomingNotification(title, text, sbn.notification)
        }
    }

    private suspend fun processIncomingNotification(
        senderName: String,
        messageText: String,
        notification: Notification?
    ) {
        val app = MarriageBureauApp.instance
        val db = app.database

        val customerRepo = CustomerRepositoryImpl(db.customerDao())
        val profileRepo = ProfileRepositoryImpl(db.profileDao())
        val teachRepo = TeachRepositoryImpl(db.teachDao())
        val knowledgeRepo = KnowledgeRepositoryImpl(db.knowledgeDao())
        val queueRepo = QueueRepositoryImpl(db.queueDao())

        // 1. Get or create customer
        val customer = customerRepo.getOrCreateCustomer(
            whatsappId = senderName.replace("\\s+".toRegex(), "_"),
            displayName = senderName
        )

        // 2. Add incoming customer message to customer memory
        customerRepo.addMessage(customer.customerId, com.marriagebureau.ai.data.db.entity.MessageSender.CUSTOMER, messageText)

        // 3. Enqueue message
        val queueItem = queueRepo.enqueueInboundMessage(
            customerId = customer.customerId,
            whatsappId = customer.whatsappIdentifier,
            text = messageText
        ) ?: return

        // 4. Run RAG Pipeline
        val llmEngine = FallbackTemplateLLMEngine()
        val ragPipeline = RAGPipeline(profileRepo, customerRepo, knowledgeRepo, teachRepo, llmEngine)

        val (ragResult, rawResponse) = ragPipeline.buildContextAndGenerate(customer.customerId, messageText)

        // 5. Response Validation & Hallucination Blocking
        val validator = ResponseValidator()
        val validation = validator.validateAndSanitize(rawResponse, ragResult.matchedProfiles)

        val finalResponse = validation.sanitizedResponse

        // Save generated response in queue
        queueRepo.updateMessageStatus(
            messageId = queueItem.messageId,
            status = QueueStatus.READY_TO_SEND,
            generatedResponse = finalResponse
        )

        // Save agent response in customer memory
        customerRepo.addMessage(customer.customerId, com.marriagebureau.ai.data.db.entity.MessageSender.AGENT, finalResponse)

        // 6. Attempt RemoteInput reply if available
        if (notification != null && !EmergencyStopController.isEmergencyStopped) {
            val replied = attemptRemoteInputReply(notification, finalResponse)
            if (replied) {
                queueRepo.updateMessageStatus(
                    messageId = queueItem.messageId,
                    status = QueueStatus.SENT
                )
            }
        }
    }

    private fun attemptRemoteInputReply(notification: Notification, responseText: String): Boolean {
        val actions = notification.actions ?: return false
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.allowFreeFormInput) {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, responseText)
                    RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
                    return try {
                        action.actionIntent.send(this, 0, intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
        return false
    }
}
