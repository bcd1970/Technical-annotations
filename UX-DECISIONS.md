# UX Decisions — TechNotes Redesign

Single source of truth for mockup-driven UX decisions. One section per mockup from `57-UX-REDESIGN-MOCKUP-PLAN.md`.

`DECISIONS.md` continues to log code/implementation decisions. This file logs **visual + interaction** decisions made before implementation.

---

## Flow per mockup

1. Read MXX blueprint + referenced source docs
2. Propose improvements (numbered list) — user approves which to incorporate
3. Build `mockups/MXX-<slug>.html` with built-in dark/light theme toggle
4. User opens, we tune in rounds
5. Record final approach below (this file)
6. Mark `✅ MXX resolved` next to the row in `57-UX-REDESIGN-MOCKUP-PLAN.md`

---

## Entry template

Each mockup gets a section that looks like this:

```
## MXX — <Mockup name>

**Resolves:** Q0X (or —)
**File:** `mockups/MXX-<slug>.html`
**Date locked:** YYYY-MM-DD

### Improvements applied (vs blueprint)
- …

### Final approach
- …

### Rejected alternatives
- … — *why rejected*

### Open follow-ups
- … (or "none")
```

---

<!-- Locked decisions go below this line, newest first or in MXX order — TBD on first entry -->
