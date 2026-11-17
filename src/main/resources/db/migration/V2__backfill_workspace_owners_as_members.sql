INSERT INTO workspace_members (workspace_id, user_id, role)
SELECT w.id, w.owner_id, 'OWNER'
FROM workspaces w
WHERE NOT EXISTS (
    SELECT 1
    FROM workspace_members wm
    WHERE wm.workspace_id = w.id
      AND wm.user_id = w.owner_id
);