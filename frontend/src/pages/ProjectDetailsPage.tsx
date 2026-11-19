import { useEffect, useState, type SubmitEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";

type Project = {
  id: number;
  workspaceId: number;
  name: string;
  projectKey: string;
};

type WorkspaceMember = {
  userId: number;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
};

type Sprint = {
  id: number;
  projectId: number;
  name: string;
  startDate: string | null;
  endDate: string | null;
  status: string;
};

type Issue = {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: string;
  priority: string;
  sprintId: number | null;
  assigneeId: number | null;
};

type IssuePage = {
  content: Issue[];
};

type CreateSprintInput = {
  name: string;
  startDate: string;
  endDate: string;
};

type CreateIssueInput = {
  title: string;
  description: string;
  priority: string;
};
type UpdateIssueInput = {
  id: number;
  title: string;
  description: string;
  priority: string;
};
const ISSUE_STATUSES = ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"];
const ISSUE_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"];

export default function ProjectDetailsPage() {
  const { projectId: projectIdParam } = useParams();
  const projectId = Number(projectIdParam);
  const isValidProjectId = Number.isInteger(projectId) && projectId > 0;

  const { email, logout, token } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const [sprintName, setSprintName] = useState("");
  const [sprintStartDate, setSprintStartDate] = useState("");
  const [sprintEndDate, setSprintEndDate] = useState("");
  const [sprintDateError, setSprintDateError] = useState("");
  const [issueTitle, setIssueTitle] = useState("");
  const [issueDescription, setIssueDescription] = useState("");
  const [issuePriority, setIssuePriority] = useState("MEDIUM");
  const [projectEditName, setProjectEditName] = useState("");
  const [projectEditKey, setProjectEditKey] = useState("");

  const issueQueryKey = ["issues", projectId, email] as const;
  const sprintQueryKey = ["sprints", projectId, email] as const;

  const [editingIssueId, setEditingIssueId] = useState<number | null>(null);
  const [editIssueTitle, setEditIssueTitle] = useState("");
  const [editIssueDescription, setEditIssueDescription] = useState("");
  const [editIssuePriority, setEditIssuePriority] = useState("MEDIUM");
  const projectQuery = useQuery({
    queryKey: ["project", projectId, email],
    enabled: isValidProjectId,
    queryFn: () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Project>(`/api/projects/${projectId}`, { token });
    },
  });

  // Fill the edit fields after the project loads or refreshes.
  useEffect(() => {
    if (projectQuery.data) {
      setProjectEditName(projectQuery.data.name);
      setProjectEditKey(projectQuery.data.projectKey);
    }
  }, [projectQuery.data]);

  const workspaceId = projectQuery.data?.workspaceId;

  const membersQuery = useQuery({
    queryKey: ["workspace-members", workspaceId, email],
    enabled: workspaceId !== undefined,
    queryFn: () => {
      if (!token || workspaceId === undefined) {
        throw new Error("Could not determine the project workspace.");
      }

      return apiRequest<WorkspaceMember[]>(
        `/api/workspaces/${workspaceId}/members`,
        { token },
      );
    },
  });

  const sprintsQuery = useQuery({
    queryKey: sprintQueryKey,
    enabled: isValidProjectId,
    queryFn: () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Sprint[]>(`/api/sprints?projectId=${projectId}`, {
        token,
      });
    },
  });

  const issuesQuery = useQuery({
    queryKey: issueQueryKey,
    enabled: isValidProjectId,
    queryFn: async () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      const params = new URLSearchParams({
        projectId: String(projectId),
        page: "0",
        size: "100",
        sortBy: "id",
        direction: "DESC",
      });

      const result = await apiRequest<IssuePage>(
        `/api/issues?${params.toString()}`,
        { token },
      );

      return result.content;
    },
  });

  const currentMember = membersQuery.data?.find(
    (member) => member.email.toLowerCase() === email.toLowerCase(),
  );
  const role = currentMember?.role;
  const canManageProject = role === "OWNER" || role === "ADMIN";
  const canManageSprints = canManageProject;
  const canEditIssues =
    role === "OWNER" || role === "ADMIN" || role === "MEMBER";

  const updateProjectMutation = useMutation({
    mutationFn: () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Project>(`/api/projects/${projectId}`, {
        method: "PUT",
        token,
        body: JSON.stringify({
          name: projectEditName.trim(),
          projectKey: projectEditKey.trim().toUpperCase(),
        }),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["project", projectId, email],
      });
      void queryClient.invalidateQueries({
        queryKey: ["projects", email],
      });
    },
  });

  const deleteProjectMutation = useMutation({
    mutationFn: () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<void>(`/api/projects/${projectId}`, {
        method: "DELETE",
        token,
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["projects", email],
      });
      queryClient.removeQueries({ queryKey: ["project", projectId, email] });
      queryClient.removeQueries({ queryKey: issueQueryKey });
      queryClient.removeQueries({ queryKey: sprintQueryKey });

      const parentWorkspaceId = projectQuery.data?.workspaceId;
      navigate(
        parentWorkspaceId ? `/workspaces/${parentWorkspaceId}` : "/app",
        { replace: true },
      );
    },
  });

  const createSprintMutation = useMutation({
    mutationFn: (input: CreateSprintInput) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Sprint>("/api/sprints", {
        method: "POST",
        token,
        body: JSON.stringify({
          projectId,
          name: input.name,
          startDate: input.startDate || null,
          endDate: input.endDate || null,
        }),
      });
    },
    onSuccess: () => {
      setSprintName("");
      setSprintStartDate("");
      setSprintEndDate("");
      void queryClient.invalidateQueries({ queryKey: sprintQueryKey });
    },
  });

  const createIssueMutation = useMutation({
    mutationFn: (input: CreateIssueInput) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Issue>("/api/issues", {
        method: "POST",
        token,
        body: JSON.stringify({
          projectId,
          title: input.title,
          description: input.description || null,
          priority: input.priority,
        }),
      });
    },
    onSuccess: () => {
      setIssueTitle("");
      setIssueDescription("");
      setIssuePriority("MEDIUM");
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });
  const updateIssueMutation = useMutation({
    mutationFn: ({ id, ...input }: UpdateIssueInput) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Issue>(`/api/issues/${id}`, {
        method: "PATCH",
        token,
        body: JSON.stringify(input),
      });
    },
    onSuccess: () => {
      setEditingIssueId(null);
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });

  const deleteIssueMutation = useMutation({
    mutationFn: (id: number) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<void>(`/api/issues/${id}`, {
        method: "DELETE",
        token,
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });
  const issueStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Issue>(`/api/issues/${id}/status`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ status }),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });

  const sprintStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Sprint>(`/api/sprints/${id}/status`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ status }),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: sprintQueryKey });
    },
  });

  const issueSprintMutation = useMutation({
    mutationFn: ({
      issueId,
      sprintId,
    }: {
      issueId: number;
      sprintId: number | null;
    }) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      if (sprintId === null) {
        return apiRequest<Issue>(`/api/issues/${issueId}/sprint`, {
          method: "DELETE",
          token,
        });
      }

      return apiRequest<Issue>(`/api/issues/${issueId}/sprint`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ sprintId }),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });

  const issueAssigneeMutation = useMutation({
    mutationFn: ({
      issueId,
      assigneeEmail,
    }: {
      issueId: number;
      assigneeEmail: string | null;
    }) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      if (assigneeEmail === null) {
        return apiRequest<Issue>(`/api/issues/${issueId}/assignee`, {
          method: "DELETE",
          token,
        });
      }

      return apiRequest<Issue>(`/api/issues/${issueId}/assignee`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ email: assigneeEmail }),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: issueQueryKey });
    },
  });

  function handleUpdateProject(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    updateProjectMutation.mutate();
  }

  function handleDeleteProject() {
    const projectName = projectQuery.data?.name ?? "this project";

    if (
      window.confirm(
        `Delete "${projectName}"? The project can only be deleted after its sprints and issues are removed.`,
      )
    ) {
      deleteProjectMutation.mutate();
    }
  }

  function handleCreateSprint(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (sprintStartDate && sprintEndDate && sprintStartDate > sprintEndDate) {
      setSprintDateError("The start date must be before the end date.");
      return;
    }

    setSprintDateError("");
    createSprintMutation.mutate({
      name: sprintName.trim(),
      startDate: sprintStartDate,
      endDate: sprintEndDate,
    });
  }

  function handleCreateIssue(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    createIssueMutation.mutate({
      title: issueTitle.trim(),
      description: issueDescription.trim(),
      priority: issuePriority,
    });
  }
  function beginEditingIssue(issue: Issue) {
    setEditingIssueId(issue.id);
    setEditIssueTitle(issue.title);
    setEditIssueDescription(issue.description ?? "");
    setEditIssuePriority(issue.priority);
  }

  function handleUpdateIssue(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (editingIssueId === null) return;

    updateIssueMutation.mutate({
      id: editingIssueId,
      title: editIssueTitle.trim(),
      // Send an empty string when clearing the description.
      description: editIssueDescription.trim(),
      priority: editIssuePriority,
    });
  }

  function handleDeleteIssue(issue: Issue) {
    if (window.confirm(`Delete "${issue.title}"? This cannot be undone.`)) {
      deleteIssueMutation.mutate(issue.id);
    }
  }
  const assignedMemberById = new Map(
    (membersQuery.data ?? []).map((member) => [member.userId, member]),
  );

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <Link className="brand" to="/app">
          <span className="brand-mark">SF</span>
          <span>SprintForge</span>
        </Link>

        <button className="signout-button" onClick={logout} type="button">
          Sign out
        </button>
      </header>

      <section className="dashboard-main">
        {projectQuery.data && (
          <Link
            className="back-link"
            to={`/workspaces/${projectQuery.data.workspaceId}`}
          >
            ← Back to {projectQuery.data.name} workspace
          </Link>
        )}

        {!isValidProjectId && (
          <p className="error-message" role="alert">
            That project address is not valid.
          </p>
        )}

        {isValidProjectId && projectQuery.isPending && (
          <p className="workspace-status">Loading project…</p>
        )}

        {isValidProjectId && projectQuery.isError && (
          <p className="error-message" role="alert">
            Could not load project: {projectQuery.error.message}
          </p>
        )}

        {isValidProjectId && projectQuery.isSuccess && (
          <>
            <div className="projects-heading">
              <div>
                <p className="eyebrow">{projectQuery.data.projectKey}</p>
                <h1>{projectQuery.data.name}</h1>
                <p className="dashboard-description">
                  Sprints and issues for this project
                </p>
              </div>

              {role && <span className="role-pill">{role}</span>}
            </div>

            {membersQuery.isError && (
              <p className="error-message" role="alert">
                Could not load workspace members: {membersQuery.error.message}
              </p>
            )}

            {canManageProject && (
              <form
                className="create-workspace-form project-form project-settings-form"
                onSubmit={handleUpdateProject}
              >
                <label
                  className="workspace-name-field"
                  htmlFor="edit-project-name"
                >
                  Project name
                  <input
                    id="edit-project-name"
                    maxLength={120}
                    onChange={(event) => setProjectEditName(event.target.value)}
                    required
                    value={projectEditName}
                  />
                </label>

                <label className="project-key-field" htmlFor="edit-project-key">
                  Project key
                  <input
                    id="edit-project-key"
                    maxLength={10}
                    minLength={2}
                    onChange={(event) =>
                      setProjectEditKey(
                        event.target.value
                          .toUpperCase()
                          .replace(/[^A-Z0-9]/g, "")
                          .slice(0, 10),
                      )
                    }
                    required
                    value={projectEditKey}
                  />
                </label>

                <button
                  className="primary-button workspace-create-button"
                  disabled={updateProjectMutation.isPending}
                  type="submit"
                >
                  {updateProjectMutation.isPending ? "Saving…" : "Save project"}
                </button>

                <button
                  className="remove-member-button"
                  disabled={deleteProjectMutation.isPending}
                  onClick={handleDeleteProject}
                  type="button"
                >
                  {deleteProjectMutation.isPending
                    ? "Deleting…"
                    : "Delete project"}
                </button>
              </form>
            )}

            {updateProjectMutation.isError && (
              <p className="error-message" role="alert">
                Could not update project: {updateProjectMutation.error.message}
              </p>
            )}

            {deleteProjectMutation.isError && (
              <p className="error-message" role="alert">
                Could not delete project: {deleteProjectMutation.error.message}
              </p>
            )}

            {canManageSprints && (
              <form
                className="create-workspace-form project-form"
                onSubmit={handleCreateSprint}
              >
                <label className="workspace-name-field" htmlFor="sprint-name">
                  New sprint
                  <input
                    id="sprint-name"
                    maxLength={120}
                    onChange={(event) => setSprintName(event.target.value)}
                    placeholder="For example, Sprint 1"
                    required
                    value={sprintName}
                  />
                </label>

                <label className="date-field" htmlFor="sprint-start">
                  Start date
                  <input
                    id="sprint-start"
                    onChange={(event) => setSprintStartDate(event.target.value)}
                    type="date"
                    value={sprintStartDate}
                  />
                </label>

                <label className="date-field" htmlFor="sprint-end">
                  End date
                  <input
                    id="sprint-end"
                    onChange={(event) => setSprintEndDate(event.target.value)}
                    type="date"
                    value={sprintEndDate}
                  />
                </label>

                <button
                  className="primary-button workspace-create-button"
                  disabled={createSprintMutation.isPending}
                  type="submit"
                >
                  {createSprintMutation.isPending ? "Creating…" : "Add sprint"}
                </button>
              </form>
            )}

            {sprintDateError && (
              <p className="error-message" role="alert">
                {sprintDateError}
              </p>
            )}

            {createSprintMutation.isError && (
              <p className="error-message" role="alert">
                {createSprintMutation.error.message}
              </p>
            )}

            <section className="project-section">
              <div className="section-heading">
                <h2>Sprints</h2>
                {canManageSprints && (
                  <span className="section-note">Owner or admin actions</span>
                )}
              </div>

              {sprintsQuery.isPending && (
                <p className="workspace-status">Loading sprints…</p>
              )}

              {sprintsQuery.isError && (
                <p className="error-message" role="alert">
                  Could not load sprints: {sprintsQuery.error.message}
                </p>
              )}

              {sprintsQuery.isSuccess && sprintsQuery.data.length === 0 && (
                <p className="workspace-status">No sprints created yet.</p>
              )}

              {sprintsQuery.isSuccess && sprintsQuery.data.length > 0 && (
                <div className="sprint-list">
                  {sprintsQuery.data.map((sprint) => (
                    <article className="sprint-card" key={sprint.id}>
                      <div>
                        <h3>{sprint.name}</h3>
                        <p>
                          {sprint.startDate ?? "No start date"}
                          {" → "}
                          {sprint.endDate ?? "No end date"}
                        </p>
                      </div>

                      <span className="role-pill">{sprint.status}</span>

                      {canManageSprints && sprint.status === "PLANNED" && (
                        <button
                          className="small-action-button"
                          disabled={sprintStatusMutation.isPending}
                          onClick={() =>
                            sprintStatusMutation.mutate({
                              id: sprint.id,
                              status: "ACTIVE",
                            })
                          }
                          type="button"
                        >
                          Start sprint
                        </button>
                      )}

                      {canManageSprints && sprint.status === "ACTIVE" && (
                        <button
                          className="small-action-button"
                          disabled={sprintStatusMutation.isPending}
                          onClick={() =>
                            sprintStatusMutation.mutate({
                              id: sprint.id,
                              status: "COMPLETED",
                            })
                          }
                          type="button"
                        >
                          Complete sprint
                        </button>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </section>

            {sprintStatusMutation.isError && (
              <p className="error-message" role="alert">
                {sprintStatusMutation.error.message}
              </p>
            )}

            {canEditIssues && (
              <form
                className="create-workspace-form project-form"
                onSubmit={handleCreateIssue}
              >
                <label className="workspace-name-field" htmlFor="issue-title">
                  New issue
                  <input
                    id="issue-title"
                    maxLength={200}
                    onChange={(event) => setIssueTitle(event.target.value)}
                    placeholder="For example, Add login page"
                    required
                    value={issueTitle}
                  />
                </label>

                <label
                  className="workspace-name-field"
                  htmlFor="issue-description"
                >
                  Description
                  <input
                    id="issue-description"
                    maxLength={2000}
                    onChange={(event) =>
                      setIssueDescription(event.target.value)
                    }
                    placeholder="Optional details"
                    value={issueDescription}
                  />
                </label>

                <label className="priority-field" htmlFor="issue-priority">
                  Priority
                  <select
                    id="issue-priority"
                    onChange={(event) => setIssuePriority(event.target.value)}
                    value={issuePriority}
                  >
                    {ISSUE_PRIORITIES.map((priority) => (
                      <option key={priority} value={priority}>
                        {priority}
                      </option>
                    ))}
                  </select>
                </label>

                <button
                  className="primary-button workspace-create-button"
                  disabled={createIssueMutation.isPending}
                  type="submit"
                >
                  {createIssueMutation.isPending ? "Creating…" : "Add issue"}
                </button>
              </form>
            )}

            {createIssueMutation.isError && (
              <p className="error-message" role="alert">
                {createIssueMutation.error.message}
              </p>
            )}

            <section className="project-section">
              <div className="section-heading">
                <h2>Issues</h2>
                <span className="section-note">
                  Showing up to 100 newest issues
                </span>
              </div>

              {issuesQuery.isPending && (
                <p className="workspace-status">Loading issues…</p>
              )}

              {issuesQuery.isError && (
                <p className="error-message" role="alert">
                  Could not load issues: {issuesQuery.error.message}
                </p>
              )}

              {issuesQuery.isSuccess && issuesQuery.data.length === 0 && (
                <p className="workspace-status">No issues created yet.</p>
              )}

              {issuesQuery.isSuccess && issuesQuery.data.length > 0 && (
                <div className="issue-list">
                  {issuesQuery.data.map((issue) => {
                    const assignedMember = issue.assigneeId
                      ? assignedMemberById.get(issue.assigneeId)
                      : undefined;

                    return (
                      <article className="issue-card" key={issue.id}>
                        <div className="issue-information">
                          <h3>{issue.title}</h3>
                          <p>{issue.description || "No description"}</p>
                          <span className="issue-priority">
                            {issue.priority}
                          </span>
                        </div>

                        {canEditIssues && (
                          <div className="issue-record-actions">
                            <button
                              className="small-action-button"
                              onClick={() => beginEditingIssue(issue)}
                              type="button"
                            >
                              Edit
                            </button>

                            <button
                              className="remove-member-button"
                              disabled={deleteIssueMutation.isPending}
                              onClick={() => handleDeleteIssue(issue)}
                              type="button"
                            >
                              Delete
                            </button>
                          </div>
                        )}

                        {editingIssueId === issue.id && (
                          <form
                            className="issue-edit-form"
                            onSubmit={handleUpdateIssue}
                          >
                            <label>
                              Title
                              <input
                                maxLength={200}
                                onChange={(event) =>
                                  setEditIssueTitle(event.target.value)
                                }
                                required
                                value={editIssueTitle}
                              />
                            </label>

                            <label>
                              Description
                              <input
                                maxLength={2000}
                                onChange={(event) =>
                                  setEditIssueDescription(event.target.value)
                                }
                                value={editIssueDescription}
                              />
                            </label>

                            <label>
                              Priority
                              <select
                                onChange={(event) =>
                                  setEditIssuePriority(event.target.value)
                                }
                                value={editIssuePriority}
                              >
                                {ISSUE_PRIORITIES.map((priority) => (
                                  <option key={priority} value={priority}>
                                    {priority}
                                  </option>
                                ))}
                              </select>
                            </label>

                            <button
                              className="primary-button workspace-create-button"
                              disabled={updateIssueMutation.isPending}
                              type="submit"
                            >
                              {updateIssueMutation.isPending
                                ? "Saving…"
                                : "Save issue"}
                            </button>

                            <button
                              className="secondary-action-button"
                              onClick={() => setEditingIssueId(null)}
                              type="button"
                            >
                              Cancel
                            </button>
                          </form>
                        )}

                        {canEditIssues && (
                          <div className="issue-controls">
                            <label>
                              Status
                              <select
                                aria-label={`Status for ${issue.title}`}
                                onChange={(event) =>
                                  issueStatusMutation.mutate({
                                    id: issue.id,
                                    status: event.target.value,
                                  })
                                }
                                value={issue.status}
                              >
                                {ISSUE_STATUSES.map((status) => (
                                  <option key={status} value={status}>
                                    {status.replaceAll("_", " ")}
                                  </option>
                                ))}
                              </select>
                            </label>

                            <label>
                              Sprint
                              <select
                                aria-label={`Sprint for ${issue.title}`}
                                onChange={(event) =>
                                  issueSprintMutation.mutate({
                                    issueId: issue.id,
                                    sprintId: event.target.value
                                      ? Number(event.target.value)
                                      : null,
                                  })
                                }
                                value={issue.sprintId ?? ""}
                              >
                                <option value="">No sprint</option>
                                {(sprintsQuery.data ?? []).map((sprint) => (
                                  <option key={sprint.id} value={sprint.id}>
                                    {sprint.name}
                                  </option>
                                ))}
                              </select>
                            </label>

                            <label>
                              Assignee
                              <select
                                aria-label={`Assignee for ${issue.title}`}
                                onChange={(event) =>
                                  issueAssigneeMutation.mutate({
                                    issueId: issue.id,
                                    assigneeEmail: event.target.value || null,
                                  })
                                }
                                value={assignedMember?.email ?? ""}
                              >
                                <option value="">Unassigned</option>
                                {(membersQuery.data ?? []).map((member) => {
                                  const isCurrentUser =
                                    member.email.toLowerCase() ===
                                    email.toLowerCase();

                                  return (
                                    <option
                                      disabled={
                                        isCurrentUser &&
                                        member.userId !== issue.assigneeId
                                      }
                                      key={member.userId}
                                      value={member.email}
                                    >
                                      {member.name}
                                      {isCurrentUser ? " (you)" : ""}
                                    </option>
                                  );
                                })}
                              </select>
                            </label>
                          </div>
                        )}

                        {!canEditIssues && (
                          <span className="role-pill">{issue.status}</span>
                        )}
                      </article>
                    );
                  })}
                </div>
              )}
            </section>

            {(issueStatusMutation.isError ||
              issueSprintMutation.isError ||
              issueAssigneeMutation.isError) && (
              <p className="error-message" role="alert">
                {issueStatusMutation.error?.message ||
                  issueSprintMutation.error?.message ||
                  issueAssigneeMutation.error?.message}
              </p>
            )}
            {updateIssueMutation.isError && (
              <p className="error-message" role="alert">
                Could not update issue: {updateIssueMutation.error.message}
              </p>
            )}

            {deleteIssueMutation.isError && (
              <p className="error-message" role="alert">
                Could not delete issue: {deleteIssueMutation.error.message}
              </p>
            )}
          </>
        )}
      </section>
    </main>
  );
}
