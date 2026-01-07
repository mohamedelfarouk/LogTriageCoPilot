# CleanLogBundle Spec (v1)

## Purpose

Convert a large, noisy log file (e.g., 500+ lines) into a compact, structured “incident packet” that:

1. preserves the most relevant evidence for a single incident
2. is safe to feed into an LLM (logs treated as data, not instructions)
3. is deterministic and testable

This bundle is the ONLY input the AI is allowed to see.

---

## Definitions

### Log Line

A single line of text in the raw file.

### Event

A logical log entry that may span multiple lines (e.g., a Java stack trace).

- A new event starts at a “header line” (timestamped log format).
- Any following lines that do not match the header pattern are continuations of the current event.

(Real-world log pipelines explicitly handle multiline events such as Java stack traces.)

### Incident

A failure or degraded behavior worth triaging (typically anchored by an ERROR with exception/stacktrace).

### CleanLogBundle

A structured summary of the incident: key metadata + the minimal set of “signal” evidence lines/events.

---

## Input

- `rawLogText`: complete text of a log file (any length)
- optional: `hintRequestId` (if user knows it)
- optional: `hintTimeRange` (if user knows it)

---

## Output: CleanLogBundle (data model)

### Required fields

- `incidentTitle` (string)

  - short human label: “NPE in UserProfileService”, “DB timeout serving degraded response”, etc.

- `timeWindow` (object)

  - `firstTimestamp` (string, ISO-like as present in logs)
  - `lastTimestamp` (string)

- `requestIds` (array of strings)

  - all request/correlation IDs discovered that belong to this incident bundle

- `primaryErrorLine` (string)

  - the single most important ERROR line (the “anchor”)

- `primaryException` (object)

  - `class` (string) e.g., `java.lang.NullPointerException`
  - `message` (string)

- `topAppFrames` (array of strings)

  - 3–5 stack frames from the application namespace (e.g., `com.example...`)

- `causedByChain` (array of objects)

  - each item: `{ class: string, message: string }`
  - extracted from “Caused by:” blocks (if present)

- `signals` (array of strings)

  - up to `MAX_SIGNALS` single-line strings (not raw multiline blocks)
  - “signal” lines are the best supporting evidence + key context

- `componentsDetected` (array of strings)

  - e.g., `["SpringMVC","Redis","Hikari","Oracle"]`
  - derived from logger names, exception classes, keywords

- `securityFlags` (array of objects)

  - each item: `{ type: string, line: string }`
  - example type: `PROMPT_INJECTION_TEXT`

- `noiseDroppedCount` (integer)

  - how many lines/events were discarded as irrelevant noise

- `notes` (string)
  - free-text for important oddities: “200 OK (Degraded) served from cache fallback”

---

## Hard limits (constants)

- `MAX_SIGNALS = 12`
- `MAX_TOP_APP_FRAMES = 5`
- `WINDOW_SECONDS = 15` (time-based window around anchor when RequestId missing)
- `WINDOW_EVENTS_BEFORE = 15`
- `WINDOW_EVENTS_AFTER = 20`

---

## Extraction Algorithm (deterministic)

### Step 1 — Parse raw lines into Events (multiline handling)

1. Identify a “header line” pattern (timestamp + thread + level + logger + message).
2. Start a new Event when a header line appears.
3. If a line is NOT a header line, append it to the current Event as a continuation.
4. Result: `events[]`, each event has:
   - `timestamp` (from header)
   - `thread`
   - `level` (DEBUG/INFO/WARN/ERROR)
   - `logger`
   - `message` (header message)
   - `continuationLines[]` (stacktrace / caused-by lines etc.)

(Handling multiline Java stack traces as a single event is required in real log ingestion.)

### Step 2 — Enrich Events with correlation keys

For each Event, extract:

- `requestId` if present (e.g., `RequestId: ...`)
- `exceptionClass` and `exceptionMessage` if stacktrace present
- keyword tags (e.g., contains “HikariPool”, “Connection refused”, “SQLTimeoutException”, “Degraded”)

Correlation IDs/trace fields exist to correlate logs for the same transaction/request.

### Step 3 — Grouping (best-effort correlation)

Create candidate groups in priority order:

1. Group by `requestId` (strongest)
2. Else group by `(thread + time proximity bucket)`
3. Else group by `(logger/component + time proximity bucket)`

### Step 4 — Anchor selection (choose the “main failure”)

Find anchor event using ranking:

- First priority: `level=ERROR` AND has exception/stacktrace
- Next: `level=ERROR` with strong failure keywords (rollback, timeout, refused)
- Tie-breakers:
  - contains app frames (`com.example...`)
  - followed by “HTTP 500”, “rolled back”, “degraded”
  - within a group that has a request completion line

Anchor outputs:

- `primaryErrorLine` = anchor header line
- `primaryException` = extracted exception class/message (if present)

### Step 5 — Build the incident window (what to keep)

If anchor has `requestId`:

- keep all events with same `requestId`
- plus `WINDOW_EVENTS_BEFORE` and `WINDOW_EVENTS_AFTER` around anchor index (within the global event list)

If anchor has no requestId:

- keep events within `WINDOW_SECONDS` before/after anchor timestamp
- plus `WINDOW_EVENTS_BEFORE/AFTER` around anchor index

Always keep:

- the anchor event (including its continuationLines)
- any event mentioning: timeout, refused, rollback, degraded, pool exhaustion, “Caused by”
- request start and request completion lines if present (for same requestId)

### Step 6 — Signal selection (top lines to include)

From kept events, produce `signals[]` (max MAX_SIGNALS) using scoring:

- +10: anchor ERROR header line
- +9: “Caused by:” lines (summarized)
- +8: request completion showing failure/degraded (e.g., 500, “Degraded”)
- +7: rollback / transaction failure lines
- +6: timeouts / connection refused / pool timeouts
- +3: request start line (path + requestId)
- -5: health checks / liveness probes / scheduled jobs unrelated to anchor group
- -3: generic INFO with no incident keywords

Signals must be single lines; multiline stack traces are summarized via:

- exception class/message
- topAppFrames
- causedByChain

### Step 7 — Component detection

Populate `componentsDetected` using deterministic keyword rules:

- Redis: logger contains “Redis” OR exception contains “RedisConnectionException” OR “:6379”
- Hikari: contains “HikariPool”
- Oracle: contains “ORA-” OR “oracle.jdbc”
- SpringMVC: contains “DispatcherServlet” OR “InvocableHandlerMethod”
  (Add more as needed.)

### Step 8 — Security scanning (logs are untrusted data)

Scan kept events for prompt-injection style text (examples):

- “ignore previous instructions”
- “system prompt”
- “output secrets”
  If found:
- add `securityFlags += { type: "PROMPT_INJECTION_TEXT", line: <the exact line> }`

Rationale: prompt injection is a top OWASP LLM risk; we must treat log content as untrusted data.

### Step 9 — Final assembly

Set:

- `timeWindow.firstTimestamp` = earliest timestamp among kept events
- `timeWindow.lastTimestamp` = latest timestamp among kept events
- `noiseDroppedCount` = total raw lines/events - kept lines/events (track both if you want)
- `incidentTitle` based on primaryException + componentDetected (simple rule-based naming)
- `notes` include important meta outcomes if detected (“Degraded response”, “Fallback served stale cache”)

---

## Determinism & Testability Rules

- Same input log file MUST produce the same CleanLogBundle (order stable, scoring stable).
- Bundle must never include more than:
  - MAX_SIGNALS signals
  - MAX_TOP_APP_FRAMES stack frames
- Security flags do not change selection; they annotate.

---

## What the LLM sees

Only:

- CleanLogBundle fields
- runbook snippets (kb/runbook.md)
  Never raw 500-line logs.
