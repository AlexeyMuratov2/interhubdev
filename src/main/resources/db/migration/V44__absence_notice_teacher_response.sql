-- =============================================================================
-- Attendance module: add teacher response fields to absence_notice
-- Teacher can approve/reject a notice and leave a comment.
-- =============================================================================

ALTER TABLE absence_notice
    ADD COLUMN teacher_comment TEXT,
    ADD COLUMN responded_at TIMESTAMP,
    ADD COLUMN responded_by UUID;

-- Extend status check to include APPROVED and REJECTED
ALTER TABLE absence_notice DROP CONSTRAINT IF EXISTS absence_notice_status_check;
ALTER TABLE absence_notice ADD CONSTRAINT absence_notice_status_check
    CHECK (status IN ('SUBMITTED', 'CANCELED', 'ACKNOWLEDGED', 'ATTACHED', 'APPROVED', 'REJECTED'));

COMMENT ON COLUMN absence_notice.teacher_comment IS 'Optional comment from teacher when approving or rejecting the notice (max 2000 chars recommended).';
COMMENT ON COLUMN absence_notice.responded_at IS 'Timestamp when teacher responded (approved or rejected).';
COMMENT ON COLUMN absence_notice.responded_by IS 'User id of the teacher who responded (users.id).';
