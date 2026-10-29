# Privacy & Data Security Policy

## Privacy Guarantees

1. **Local-First & Offline By Default**
   - All marriage profiles, customer details, conversation history, and local knowledge base documents are stored exclusively on the user's local device in Room SQLite database.
   - Local LLM inference runs directly on the device with zero paid external API dependency or cloud streaming of profile details.

2. **No Data Overwriting / Source of Truth**
   - Customer memory and conversation notes are stored separately from Marriage Profiles.
   - Marriage profiles are the sole source of truth for match facts.

3. **Media & Sensitive Photo Restrictions**
   - Profile photos are stored as local file references.
   - Photo metadata does NOT automatically infer or guess sensitive personal attributes (religion, caste, health, or financial status).

4. **Security Controls**
   - Sensitive operations require human approval when enabled.
   - Prompt Injection Shielding ensures incoming WhatsApp messages cannot modify system safety policies or exfiltrate private database fields.
