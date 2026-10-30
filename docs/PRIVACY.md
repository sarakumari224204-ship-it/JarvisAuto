# Privacy & Security Policy

## Privacy & Data Protection Framework

1. **Local-First & Offline By Default**
   - All marriage profiles, customer directory data, conversation history, and local knowledge base documents are stored exclusively on the user's local device in Room SQLite database.
   - The application functions 100% offline using `LocalLLMProvider` or `FallbackTemplateLLMProvider` without requiring internet connectivity or paid APIs.

2. **Hybrid Online Privacy Filter (`PrivacyFilter`)**
   When online AI providers (Gemini, OpenAI, Grok, NVIDIA NIM, Custom) are enabled by the owner, `PrivacyFilter` enforces configurable data redaction:
   - **STRICT (Default)**: Automatically redacts phone numbers, email addresses, contact information, family details, and photo references from outgoing prompts.
   - **STANDARD**: Includes public profile summaries while stripping contact info.
   - **OFFLINE_ONLY**: Prevents any network requests to external AI endpoints.

3. **Secure Keystore API Key Storage (`SecureStorageManager`)**
   - API keys and tokens are stored in Android Keystore-backed encrypted storage.
   - Secret masking (`sk-...abcd`) ensures API keys are never printed in Logcat or cleartext UI.

4. **Media & Photo Privacy Rules**
   - Profile photo references are stored as local file URIs.
   - The system strictly enforces the privacy rule: **Do NOT infer or guess sensitive personal attributes (religion, caste, health, or financial status) from photographs.**

5. **Safety Hierarchy & Anti-Prompt Injection**
   - Customer messages are sanitized and treated strictly as untrusted input.
   - Priority hierarchy:
     `SYSTEM SAFETY > BUSINESS RULES > OWNER INSTRUCTIONS > CUSTOMER CONTENT`
