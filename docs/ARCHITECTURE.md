# Marriage Bureau AI Agent Architecture

## Overview
The **Marriage Bureau AI Agent** is a privacy-first, standalone Android application designed for 6GB RAM devices. It manages marriage profiles, tracks customer memory, listens for WhatsApp Business notifications, matches profiles using local RAG, validates LLM responses against factual DB entries to prevent hallucinations, and automates or drafts WhatsApp responses locally with zero paid external LLM dependencies.

## Key Architectural Modules

1. **Database Layer (`com.marriagebureau.ai.data.db`)**
   - Built on Room / SQLite.
   - Optimized schema with indexes for 10,000+ marriage profiles (`profileCode`, `age`, `city`, `gender`, `maritalStatus`).
   - Customer Memory DB (`CustomerEntity`, `ConversationMessageEntity`, `CustomerRequirementEntity`).
   - Persistent Message Queue (`QueueMessageEntity`) for tracking inbound WhatsApp notifications.
   - Teach AI Storage (`InstructionEntity`, `RuleEntity`, `ExampleConversationEntity`, `KnowledgeChunkEntity`, `TrainingVersionEntity`).

2. **Teach AI & Knowledge Subsystem (`com.marriagebureau.ai.teach`)**
   - Identity Management (Name, Bureau Name, Persona, Tone, Languages).
   - Rules & Instructions Engine with priority ordering and toggle flags.
   - Example Conversations Repository (categorized by intent).
   - Knowledge Base Text Chunker & Local Retriever (PDF/TXT/JSON/URL).
   - Teach Correction System & Training Versioning (reversible snapshots).

3. **Local LLM & RAG Engine (`com.marriagebureau.ai.ai`)**
   - `LocalLLM` interface supporting compact quantized local models (0.5B - 1.5B) and template fallback execution.
   - `RAGPipeline`: Combines Intent Detection, Requirement Extraction, Customer Memory, Profile DB Search, Knowledge Base Search, and Teach Rules into a compact prompt context.
   - `ResponseValidator`: Factual guardrail enforcing non-hallucination. Checks generated profiles against source DB records.

4. **WhatsApp Automation Layer (`com.marriagebureau.ai.service`)**
   - `WhatsAppNotificationService`: Listens for Android notification intent from WhatsApp Business.
   - RemoteInput reply mechanism for automated or approved sending.
   - Automation Modes: Draft Only (default), Human Approval, and Low-Risk Auto Reply.
   - Emergency Stop state machine to immediately halt processing and sending.

5. **Presentation Layer (`com.marriagebureau.ai.ui`)**
   - Jetpack Compose single-activity architecture with Navigation.
   - Screens: Dashboard, Marriage Profiles, Customers, Conversations, Teach AI, Knowledge Base, Local LLM Settings, WhatsApp Queue, System Diagnostics.

6. **Safety & Privacy Shielding**
   - Strict separation between System Safety, Business Rules, Owner Instructions, and Customer Content.
   - Customer input sanitization preventing prompt injection attacks.
