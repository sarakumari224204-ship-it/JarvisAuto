# Marriage Bureau AI Agent Architecture

## Overview
The **Marriage Bureau AI Agent** is a privacy-first, standalone Android application designed for 6GB RAM devices. It manages marriage profiles, tracks customer memory, listens for WhatsApp Business notifications, matches profiles using local RAG, validates LLM responses against factual DB entries to prevent hallucinations, and supports a Universal Hybrid AI Provider framework with automated local fallback.

## Universal Hybrid AI Provider Framework (`com.marriagebureau.ai.ai`)

The application implements a universal `LLMProvider` abstraction supporting 7 provider adapters:

1. **GeminiProvider**: Google Gemini REST API (`gemini-1.5-flash`, `gemini-1.5-pro`).
2. **OpenAIProvider**: Official OpenAI ChatGPT API (`gpt-4o-mini`, `gpt-4o`).
3. **GrokProvider**: xAI Grok REST API (`grok-beta`, `grok-2`).
4. **NvidiaNimProvider**: NVIDIA NIM microservice endpoints (`meta/llama-3.1-8b-instruct`).
5. **OpenAICompatibleProvider**: Generic custom OpenAI-compatible endpoint (configurable Base URL, API Key, Model, and Custom Headers).
6. **LocalLLMProvider**: On-device 0.5B - 1.5B quantized GGUF model runner.
7. **FallbackTemplateLLMProvider**: 100% offline local rule & template inference engine for instant fallback.

### Multi-Tier Fallback & Cost Controls
- **HybridLLMManager**: Orchestrates provider configuration, connection tests (`testConnection`), model discovery (`fetchModels`), rate-limiting exponential backoff (HTTP 429), daily/monthly usage budget limits, and multi-tier fallback:
  `Selected Online Provider -> Secondary Configured Online Provider -> Local LLM -> Fallback Template Engine`
- **SecureStorageManager**: Android Keystore-backed encrypted storage for API keys with secret masking (`sk-...abcd`) and zero Logcat leakage.

## Privacy & Safety Shielding (`PrivacyFilter` & `ResponseValidator`)

1. **PrivacyFilter**:
   - `STRICT` (Default): Redacts private contact details, phone numbers, email addresses, family metadata, and photo references before sending prompts to online LLM providers.
   - `STANDARD`: Includes matched profile summary attributes while stripping contact info.
   - `OFFLINE_ONLY`: Blocks external network prompts completely.
2. **ResponseValidator**: Factual guardrail enforcing non-hallucination. Checks generated LLM outputs against matched DB profiles and blocks/sanitizes any responses citing non-existent profiles.
3. **Prompt Injection Shielding**: Strict priority hierarchy:
   `SYSTEM SAFETY RULES > BUSINESS RULES > OWNER INSTRUCTIONS > CUSTOMER MESSAGES`

## WhatsApp Business Integration Layer (`com.marriagebureau.ai.service`)

- **WhatsAppNotificationService**: Listens for incoming WhatsApp Business notifications (`com.whatsapp.w4b` & `com.whatsapp`) via `NotificationListenerService` and attempts direct `RemoteInput` notification reply.
- **WhatsAppAccessibilityService**: Safe Accessibility fallback that auto-sends replies when RemoteInput is restricted by Android OS or WhatsApp.
- **QueueRepositoryImpl**: Persistent Room SQLite queue with 60-second message deduplication, exponential retry backoff, and `EmergencyStopController` kill-switch.

## Database & Memory Layer (`com.marriagebureau.ai.data.db`)

- **Marriage Profile DB**: Room SQLite database with multi-column indexes (`profileCode`, `age`, `city`, `gender`, `maritalStatus`). Benchmark search latency: **5.3 ms** across 10,000 profiles.
- **Customer Memory DB**: Persistent multi-turn requirement extraction (Gender, Age range, Location, Education, Shown Profiles).
- **Teach AI DB**: Identity, priority-ranked rules, instructions, example conversations, Teach Correction ("What should you have said?"), and reversible training version snapshots.
- **Knowledge Base DB**: Document chunking (~200 words), keyword indexing, and scheme-validated URL content parser.
