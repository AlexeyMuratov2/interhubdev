---
name: plan-compliance-review
description: Post-implementation audit that verifies an implementation against an approved plan, architecture document, or task specification. Detects plan drift, shortcuts, partial implementations, and violations of project rules. Use when auditing whether a large task or refactor was implemented correctly, when verifying security/architecture hardening, or when checking that code matches an approved specification.
---

# Plan Compliance Review

This skill is a **post-implementation verification** workflow. It audits how accurately an implementation follows an approved plan, architecture document, or task specification. It is **not** a generic code review: it specifically verifies plan compliance, detects shortcuts and partial implementations, reviews architecture quality, and checks compliance with existing project rules and mandatory documents.

**Core distinction**: "Working code" and "correct implementation" are different. Code may function while still violating the plan or architecture. Treat that as a finding.

**Source of truth**: The approved plan, architecture document, or specification is the **source of truth** for the review. The implementation must not silently redefine or narrow the plan. If the implementation differs from the plan, that difference must be treated as a **deviation** unless there is explicit evidence that the plan was intentionally updated or superseded (e.g. an approved change record or revised doc). Do not assume the implementation is the new spec.

---

## When to Use This Skill

- After a large implementation task is done and you need to verify it matches the approved plan.
- Auditing whether a refactor really followed the architecture plan.
- Checking whether a security hardening or upload/download safety plan was fully implemented.
- Verifying a module split preserved intended boundaries.
- Reviewing whether a new endpoint or feature follows the approved responsibility split.
- Auditing whether a big backend (or frontend) task was implemented fully and without architectural shortcuts.

## When Not to Use

- For routine, small changes with no prior plan or spec.
- For style-only or formatting reviews.
- When the goal is to suggest new features rather than verify existing work.
- When there is no approved plan, architecture doc, or specification to compare against.

---

## Review Principles (Enforce These)

- **Do not assume** that working behavior means the plan was implemented correctly.
- **Do not give full credit** for partial implementation; report the gap.
- **Evidence bar for "fully implemented"**: If evidence is incomplete, ambiguous, indirect, or only partially covers a requirement, the item must **not** be marked as fully implemented. Similarity to the plan is not enough; the implementation must actually satisfy the requirement.
- **Prefer under-crediting over over-crediting**: When evidence is weak or equivocal, classify as partially implemented, not implemented, or insufficiently evidenced rather than giving full credit.
- **Plan is source of truth**: The approved plan/spec is the source of truth. Differences from the plan are deviations unless the plan was explicitly updated or superseded; do not let the implementation silently redefine the plan.
- **"Implemented differently" is not automatically acceptable**: Evaluate each case as either an acceptable alternative (preserves architectural intent, ownership, boundaries, and safety properties) or as a deviation/regression. Do not use "implemented differently" as a soft excuse for plan drift.
- **Hidden scope reductions**: Explicitly check for silently omitted edge cases, non-happy paths, failure handling, rollback/cleanup, migrations, integration updates, validation branches, and required config/docs or retirement of superseded behavior.
- **Complete the audit**: Even if a major blocking problem is found early, **continue and complete the full audit** rather than stopping at the first serious issue. The report must cover all sections.
- **No evidence → not implemented**: If no concrete implementation evidence can be found for a required item, treat it as not implemented or insufficiently evidenced. Absence of evidence must not be converted into optimistic assumptions.
- If the implementation **deviates** from the approved plan without strong justification, report it as a deviation.
- If the implementation **replaces** an architectural requirement with a shortcut, report it as a shortcut or architectural regression.
- If there is **uncertainty**, say so explicitly in the report.
- Prefer **architectural correctness** over convenience.
- Check whether **existing stable flows** were preserved.
- Check whether **boundaries between modules** remain clean.
- Check whether **ownership and responsibilities** remain well separated.
- Check whether **technical entities leak into business modules** or vice versa.
- Check whether **validation and tests** are adequate for risky changes.
- **Do not** rewrite the task or silently reinterpret the original plan into an easier version.

---

## Procedural Workflow

Follow this sequence. Do not skip steps.

### 1. Read the Authoritative Sources

- Read the **approved plan**, **architecture document**, or **implementation specification** first.
- If the user points to a doc (e.g. `docs/secure-file-upload-architecture.md`), read it in full.
- Identify any **referenced** documents (security rules, module boundaries, auth rules, upload safety) and read those that apply.

### 2. Extract Requirements and Constraints

- List **major requirements** (features, behaviors, flows).
- List **constraints** (security, boundaries, layering, dependency direction, error handling).
- List **explicit non-goals** or out-of-scope items if stated.

### 3. Build a Compliance Checklist

- Turn the plan into a **structured checklist** of required obligations.
- Each item should be verifiable (you can point to code or its absence).
- Include: functional requirements, architectural requirements, integration points, validation/testing expectations, and any project-rule obligations.

### 4. Read the Implementation

- Read the **changed code**, **affected files**, and **relevant diff** (e.g. from git or from the user).
- Identify where each plan item is **intended** to be implemented (which module, which class, which flow).

### 5. Compare Item by Item

For **each** checklist item, determine and record:

- **Fully implemented** — Evidence in code directly and fully matches the requirement. Do not use this when evidence is incomplete, ambiguous, or only partial.
- **Partially implemented** — Only some aspects done; list what is missing.
- **Not implemented** — No supporting code or clearly skipped. If no concrete evidence exists for a required item, use this or "insufficiently evidenced."
- **Implemented differently** — Done in another way than specified. **This is not automatically acceptable.** For each such case you must explicitly evaluate: (a) **Acceptable alternative** — preserves architectural intent, ownership, boundaries, and safety properties; or (b) **Deviation/regression** — weakens intent, ownership, boundaries, or safety. Do not use "implemented differently" as a soft excuse for plan drift; classify the outcome and report it in the deviations section.

### 6. Identify Deviations

- List every case where the implementation **differs** from the approved plan (different approach, different boundary, different flow).
- For each, note: what was planned, what was done, and whether the deviation is justified or not.

### 7. Identify Shortcuts and Simplifications

- List every place the implementer used an **easier local solution** instead of the intended architectural one (e.g. inlining logic that should be in a service, crossing module boundaries, skipping validation or error handling).

### 8. Identify Hidden Scope Reductions

Explicitly check whether the implementation **silently reduced** the approved scope. Look for omitted:

- **Edge cases** and boundary conditions required by the plan.
- **Non-happy paths**: error paths, retries, timeouts, fallbacks.
- **Failure handling**: explicit handling of failures as specified (e.g. validation failures, external service failures).
- **Rollback/cleanup behavior**: transaction rollback, resource cleanup, or compensating actions where the plan requires them.
- **Migrations or backfill steps**: data migrations, schema changes, or backfill jobs that the plan specifies.
- **Integration updates**: changes in other affected modules, APIs, or callers that the plan says must be updated.
- **Validation branches**: all validation paths or rules the plan requires (e.g. by role, by state, by input type).
- **Config or documentation changes**: configuration, feature flags, or documentation the plan requires.
- **Removal or retirement**: decommissioning or retiring superseded behavior where the plan requires it.

List each identified scope reduction with: what the plan required, what is missing in the implementation, and where (file/module) the gap is or where it would belong.

### 9. Architecture Review

Assess and document both **structure** and **architectural intent**:

- **Layering**: Are layers respected (e.g. controller → API → service → repository)?
- **Dependency direction**: Do dependencies point the right way (no cycles, no forbidden imports)?
- **Modular boundaries**: Are module boundaries clean? Any cross-boundary leakage?
- **Responsibility ownership**: Is each responsibility in the right module/class? Does the implementation preserve the **intended** ownership from the plan?
- **Orchestration boundaries**: Are orchestration vs domain boundaries as intended? No logic placed in the wrong layer (e.g. business rules in controller, orchestration in repository).
- **Separation of concerns**: Is the intended separation between business and technical concerns preserved? No technical entities leaking into business modules or vice versa.
- **Leakage**: Do technical entities leak into business modules or vice versa?
- **Long-term design direction**: Does the implementation align with the **intended** design direction described in the plan (extensibility, plug points, future evolution), or did shortcuts block or contradict it?
- **Extensibility**: Can the design evolve as specified, or did shortcuts block it?
- **Safety/security**: Are security constraints (e.g. upload validation, auth) preserved or strengthened?
- **Established flows**: Are existing stable flows preserved, or were they altered without plan approval?

### 10. Rules and Document Compliance

- Check **project rules** (e.g. `.cursor/rules/*`, architecture docs): were they followed?
- Check **referenced** architecture/security documents: were their constraints respected?
- **Rules are binding**: If project rules conflict with the implementation, this is a **violation** even if the code is otherwise clean. Local convenience or reduced effort is not a valid justification for violating project rules. Assess rules/doc compliance **independently** from whether the code "works."
- Flag any violation of: module boundaries, error handling contract, DTO/API contract, allowed dependencies, port/adapter usage.

### 11. Tests and Validation

- Are there **tests** for the new or changed behavior? Where?
- For **risky** changes (security, boundaries, integration), is coverage adequate?
- Is **validation** (input, business rules) present and consistent with the plan?

### 12. Produce the Final Report

Use the **Report Structure** below. Fill every section. Use concrete **file names**, **class names**, and **module names**; avoid vague statements.

---

## Report Structure

Produce the report in this order. Use the exact section titles.

### 1. Overall Verdict

One of:

- **Matches the plan well** — Implementation aligns with the plan; only minor optional improvements.
- **Partially matches the plan** — Several items missing, deviated, or simplified; list in sections 2–4 and 4a.
- **Does not match the plan sufficiently** — Major gaps, shortcuts, or rule violations; blocking fixes required.

### 2. Plan Compliance Matrix

Table or list. For each **major** plan item:

| Requirement | Implementation status | Evidence in code | Gap or concern |
|-------------|-----------------------|------------------|----------------|
| (from plan) | Fully / Partial / Not / Different / Insufficiently evidenced | File/class/method or "none" | Short description |

### 3. Deviations from Plan

- Numbered list of every case where implementation **differs** from the approved plan.
- For each: what was planned, what was done, and whether it is acceptable or a finding.

### 4. Shortcuts and Simplifications

- Numbered list of places where an **easier solution** was used instead of the intended architectural one.
- Include file/class/module and a one-line description of the shortcut.

### 4a. Hidden Scope Reductions

- List every case where the implementation **silently reduced** the approved scope (omitted edge cases, non-happy paths, failure handling, rollback/cleanup, migrations, integration updates, validation branches, config/docs, or retirement of superseded behavior).
- For each: what the plan required, what is missing, and where the gap is or would belong. If none found, state "None identified" explicitly.

### 5. Architecture Review

- **Summary**: Architecture preserved / improved / degraded.
- **Architectural intent**: Does the implementation preserve intended responsibility ownership, orchestration boundaries, and separation of business vs technical concerns? Does it align with the long-term design direction in the plan?
- **Module boundaries**: Clean / violated (with examples).
- **Ownership and dependency direction**: Correct / issues (with examples).
- **Abstraction quality**: Appropriate / leaky or over-coupled.
- **Maintainability**: Aligns with plan / technical debt or fragility introduced.

### 6. Rules and Document Compliance

- Which project rules or docs were checked (list them).
- For each: Compliant / Violation (with location and short description).
- **Note**: A violation is a violation even if the code is otherwise clean; local convenience is not a justification. Compliance is assessed independently of whether the code "works."

### 7. Risks

- What can **break** (e.g. edge cases, missing validation).
- What remains **fragile** (e.g. tight coupling, missing tests).
- What is **under-validated** (e.g. security path not tested).
- What **technical debt** was introduced.

### 8. Required Fixes

- **Blocking** issues that must be corrected before the work is considered complete.
- Each item: what to fix, where (file/module), and why it is blocking.

### 9. Optional Improvements

- Non-blocking but useful improvements (tests, docs, small refactors).

---

## Project Rules and Documents

- **Always** look for violations of **project rules** (e.g. in `.cursor/rules/` or project docs).
- **Always** check **architectural contracts** (module boundaries, allowed dependencies, ports/adapters).
- **Always** check **security constraints** (auth, upload safety, validation) when they appear in the plan or referenced docs.
- **Rules remain binding**: If project rules conflict with the implementation, this is a **violation** even if the code is otherwise clean. Local convenience or reduced effort is **not** a valid justification for violating project rules. Check rules and doc compliance **independently** from whether the code "works."
- Flag **weakening** of previously established safe flows.
- Flag places where the implementation **technically works** but **breaks long-term project structure**.

If the user or the plan references specific documents (architecture, security audit, auth rules, module boundary rules, upload safety), **read and use them** before judging compliance. Do not hardcode specific filenames unless the user or plan names them; prefer "project's authoritative documents" and then list what you checked.

---

## Evidence and Concreteness

- **Every finding** must reference **concrete evidence**: file path, class/method, or diff location.
- **No evidence → not implemented or insufficiently evidenced**: If no concrete implementation evidence can be found for a required plan item, treat it as **not implemented** or **insufficiently evidenced**. Do **not** convert absence of evidence into optimistic assumptions (e.g. "likely implemented elsewhere" without a reference).
- Avoid abstract statements like "validation could be stronger"; instead: "Validation in `X.java` does not check Y as required by the plan (section Z)."
- For "partially implemented" or "implemented differently", state **exactly** what is present and what is missing or changed.

---

## Common Failure Modes to Watch For

During the audit, watch for these patterns and treat them as findings when present:

- **Partial implementation presented as complete**: Main flow implemented; edge cases, error paths, or integration points omitted without plan approval.
- **Correct naming, wrong ownership**: Classes/modules named as in the plan but responsibility or logic placed in the wrong layer or module.
- **Correct layering on paper, wrong layer in practice**: Structure looks layered but business logic in controller, orchestration in repository, or validation missing from the intended layer.
- **DTO/entity leakage**: Entities or internal types exposed across module boundaries; DTOs not used where the plan or rules require them.
- **Tests covering only the happy path**: New behavior has tests but failure cases, security-sensitive paths, or boundary conditions are untested.
- **Preserved API surface, broken architectural meaning**: Endpoints or methods exist and "work" but violate ownership, use wrong dependencies, or bypass intended abstractions.
- **Code that works but weakens long-term structure**: Implementation achieves the immediate goal but introduces coupling, bypasses ports/adapters, or makes future changes harder than the plan intended.

---

## Example Use Cases

- **Refactor audit**: "We refactored module X; verify it matches `docs/refactor-plan.md` and module-boundary rules."
- **Security hardening**: "Check that the upload flow matches `docs/secure-file-upload-architecture.md` and that no shortcuts were taken."
- **Module split**: "We split service A into modules B and C; verify boundaries and ownership match the approved design."
- **New endpoint**: "We added endpoint E; verify it follows the approved responsibility split and composition/domain rules."
- **Large backend task**: "Audit whether the implementation of feature F matches the approved spec and project architecture."

Use this skill whenever an **approved plan or spec exists** and you need a **strict, evidence-based audit** of whether the implementation matches it and respects project rules.
