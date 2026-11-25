---
name: domain-modeling
description: Build and sharpen a project's domain model. Use when discussing codebase terminology, writing or editing a CONTEXT.md, or recording or editing an ADR.
agent_created: true
---

# Domain Modeling

Actively build and sharpen the project's domain model as you design. This is the *active* discipline: challenging terms, inventing edge-case scenarios, and writing the glossary and decisions down the moment they crystallise. (Merely *reading* `CONTEXT.md` for vocabulary is not this skill: that's a one-line habit any skill can do. This skill is for when you're changing the model, not just consuming it.)

**Check the repo's own documentation rules first, and let them win.** Read the entry points it actually ships — `AGENTS.md`, `CLAUDE.md`, `.codebuddy/rules/`, `CONTRIBUTING.md`, `docs/README.md`, `.gitattributes` — before writing anything. This skill says *what* is worth capturing; the repo says *where* it goes and *what shape* it takes, and a repo may well forbid things this skill takes for granted: it may ban copying implementation details into `CONTEXT.md`, require every open question to land in a designated `docs/todo/` instead of in prose, or already own ADR numbering. Follow the repo over the defaults below, and if the two genuinely conflict, say so out loud rather than silently picking one.

## File structure

Most repos have a single context:

```
/
├── CONTEXT.md
├── docs/
│   └── adr/
│       ├── 0001-event-sourced-orders.md
│       └── 0002-postgres-for-write-model.md
└── src/
```

If a `CONTEXT-MAP.md` exists at the root, the repo has multiple contexts. The map points to where each one lives:

```
/
├── CONTEXT-MAP.md
├── docs/
│   └── adr/                          ← system-wide decisions
├── src/
│   ├── ordering/
│   │   ├── CONTEXT.md
│   │   └── docs/adr/                 ← context-specific decisions
│   └── billing/
│       ├── CONTEXT.md
│       └── docs/adr/
```

Create files lazily: only when you have something to write. If no `CONTEXT.md` exists, create one when the first term is resolved. If no `docs/adr/` exists, create it when the first ADR is needed.

## During the session

### Challenge against the glossary

When the user uses a term that conflicts with the existing language in `CONTEXT.md`, call it out immediately. "Your glossary defines 'cancellation' as X, but you seem to mean Y. Which is it?"

### Sharpen fuzzy language

When the user uses vague or overloaded terms, propose a precise canonical term. "You're saying 'account': do you mean the Customer or the User? Those are different things."

### Discuss concrete scenarios

When domain relationships are being discussed, stress-test them with specific scenarios. Invent scenarios that probe edge cases and force the user to be precise about the boundaries between concepts.

### Cross-reference with code

When the user states how something works, check whether the code agrees. If you find a contradiction, surface it: "Your code cancels entire Orders, but you just said partial cancellation is possible. Which is right?"

### Update CONTEXT.md inline

When a term is resolved, update `CONTEXT.md` right there. Don't batch these up: capture them as they happen. Use the format in [CONTEXT-FORMAT.md](./CONTEXT-FORMAT.md).

`CONTEXT.md` should be totally devoid of implementation details. Do not treat `CONTEXT.md` as a spec, a scratch pad, or a repository for implementation decisions. It is a glossary and nothing else.

### Why a term is *rejected* is also a result

Not every resolved term belongs in the glossary. Run it against the bar in [CONTEXT-FORMAT.md](./CONTEXT-FORMAT.md) first ("a concept unique to this context, or a general programming concept?"), and when it fails, say so plainly and record the rejection — plus the rule that follows from it. A session that ends in "we do not call it that, and here is why" is a successful session, not an empty one. Don't pad the glossary to show progress.

### Re-scan the entries you are already in

Any time you touch the glossary, read back over the neighbouring entries before you leave. The rot to look for: **a definition that names a file, a tool, or a command.** That is copied mechanism, not vocabulary — it duplicates whatever document actually owns that mechanism, and the two will drift apart the first time the mechanism changes (this repo's ADR 0006 is exactly that lesson). Replace it with a one-line "where to look" pointer. Cheapest possible catch, and it is invisible until you read for it.

### Offer ADRs sparingly

Only offer to create an ADR when all three are true:

1. **Hard to reverse**: the cost of changing your mind later is meaningful
2. **Surprising without context**: a future reader will wonder "why did they do it this way?"
3. **The result of a real trade-off**: there were genuine alternatives and you picked one for specific reasons

If any of the three is missing, skip the ADR. Use the format in [ADR-FORMAT.md](./ADR-FORMAT.md).
