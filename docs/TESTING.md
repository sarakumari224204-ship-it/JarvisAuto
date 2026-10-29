# Testing Strategy & Guidelines

## Overview
This document outlines the testing strategy for the Marriage Bureau AI Agent application, covering unit testing, load testing for 10,000+ profiles, RAG context validation, response hallucination blocking, queue retry dynamics, and safety controls.

## Test Categories

### 1. Database & Search Performance Tests
- Verify index performance on 10,000+ profiles.
- Benchmark multi-criteria queries (Age range + City + Gender + Education).
- Verify duplicate detection based on Profile Code, Name, Date of Birth, or Contact.

### 2. Import & Validation Tests
- Test parsing of valid CSV, JSON, and structured text formats.
- Test error reporting for missing mandatory fields or invalid data types.
- Test rollback and transaction atomicity.

### 3. Customer Memory Tests
- Test preference state accumulation across multiple conversation turns.
- Test memory isolation (ensuring customer notes do not mutate marriage profile database facts).

### 4. Teach AI & Safety Tests
- Test Instruction and Rule evaluation priority.
- Test Prompt Injection defense (preventing customer messages from bypassing system rules).
- Test Training Versioning rollback and export/import functionality.

### 5. Local LLM & Response Validator Tests
- Test factual matching: blocking responses that cite fake/unmatched profile IDs or invented attributes.
- Test fallback response generation when hallucination is detected.

### 6. WhatsApp Queue & Automation Tests
- Test message deduplication.
- Test retry logic with exponential backoff.
- Test Emergency STOP halting queue processing immediately.
