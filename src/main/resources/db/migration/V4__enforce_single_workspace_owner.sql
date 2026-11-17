DO $$
BEGIN
    IF EXISTS (
        SELECT workspace_id
        FROM workspace_members
        WHERE role = 'OWNER'
        GROUP BY workspace_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Multiple OWNER memberships exist. Review them manually before applying this migration.';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_workspace_members_single_owner
    ON workspace_members (workspace_id)
    WHERE role = 'OWNER';