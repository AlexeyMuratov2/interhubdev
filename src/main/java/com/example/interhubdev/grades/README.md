# Grades (Progress) module

## What is the grade_entry ledger?

The **grade_entry** table is a **ledger** of point allocations: each row is one allocation or correction of points to a student for a given offering (subject delivery to a group). Points are not tied to a specific lesson by default; they can optionally reference a lesson (`lesson_id`) and/or a homework submission (`homework_submission_id`). When a submission is present, it is considered the primary reference.

- **student_id** — student profile (students.id)
- **offering_id** — group subject offering
- **points** — amount (DECIMAL 6,2); can be negative for corrections
- **type_code** — SEMINAR | EXAM | COURSEWORK | HOMEWORK | OTHER | CUSTOM (CUSTOM requires **type_label**)
- **status** — ACTIVE (counts in totals) or VOIDED (soft-deleted, excluded from sums)

## How totals and breakdown work

- **Total points** for a student in an offering = sum of `points` over all rows with `status = 'ACTIVE'` (and optional `graded_at` in [from, to]).
- **Breakdown by type** = same sum grouped by `type_code` (and for CUSTOM, the label is stored but aggregation is by code).
- VOIDED entries are never included in total or breakdown; they can be shown in the entry list when `includeVoided=true` for audit.

## REST API (summary)

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/grades/entries | Create single entry |
| POST | /api/grades/entries/bulk | Bulk create (all-or-nothing) for one offering |
| PUT | /api/grades/entries/{id} | Update entry (ACTIVE only) |
| DELETE | /api/grades/entries/{id} | Void entry (soft delete) |
| GET | /api/grades/entries/{id} | Get one entry |
| GET | /api/grades/students/{studentId}/offerings/{offeringId} | Entries + total + breakdownByType |
| GET | /api/grades/groups/{groupId}/offerings/{offeringId}/summary | Per-student total + breakdown for group |

Query params for list/summary: `from`, `to` (ISO datetime), `includeVoided` (default false).

## Bulk create

- **POST /api/grades/entries/bulk**: one `offeringId`, common `typeCode` (and `typeLabel` if CUSTOM), optional `description`, `lessonSessionId`, `gradedAt`; **items** = list of `{ studentId, points, homeworkSubmissionId? }`.
- Strategy: **all-or-nothing** (one invalid item rolls back the whole transaction).

## Corrections and history

- To correct: either **void** the old entry and create a new one (full history), or **update** the existing entry (simpler; `graded_at` can be left or changed). The module is prepared for future audit (e.g. version or separate history table).

## Access

Only users with **TEACHER** or **ADMIN** / **MODERATOR** / **SUPER_ADMIN** can create, update, void, or view grades.
