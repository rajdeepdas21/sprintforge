import { useState, type SubmitEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
type Workspace = {
  id: number;
  name: string;
  ownerId: number;
};

type WorkspaceMember = {
  userId: number;
  name: string;
  email: string;
  role: string;
  joinedAt: string;
};

type Project = {
  id: number;
  workspaceId: number;
  name: string;
  projectKey: string;
};

type CreateProjectInput = {
  name: string;
  projectKey: string;
};

export default function WorkspaceProjectsPage() {
  const { workspaceId: workspaceIdParam } = useParams();
  const workspaceId = Number(workspaceIdParam);
  const isValidWorkspaceId = Number.isInteger(workspaceId) && workspaceId > 0;

  const { email, logout, token } = useAuth();
  const queryClient = useQueryClient();
  const [projectName, setProjectName] = useState("");
  const [projectKey, setProjectKey] = useState("");

  const workspaceQuery = useQuery({
    queryKey: ["workspace", workspaceId, email],
    enabled: isValidWorkspaceId,
    queryFn: () => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");
      return apiRequest<Workspace>(`/api/workspaces/${workspaceId}`, { token });
    },
  });

  const membersQuery = useQuery({
    queryKey: ["workspace-members", workspaceId, email],
    enabled: isValidWorkspaceId,
    queryFn: () => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");
      return apiRequest<WorkspaceMember[]>(
        `/api/workspaces/${workspaceId}/members`,
        { token },
      );
    },
  });

  const projectsQuery = useQuery({
    queryKey: ["projects", email],
    enabled: isValidWorkspaceId,
    queryFn: async () => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      const projects = await apiRequest<Project[]>("/api/projects", { token });
      return projects.filter((project) => project.workspaceId === workspaceId);
    },
  });

  const currentRole = membersQuery.data?.find(
    (member) => member.email.toLowerCase() === email.toLowerCase(),
  )?.role;

  const canCreateProject = currentRole === "OWNER" || currentRole === "ADMIN";

  const createProjectMutation = useMutation({
    mutationFn: (input: CreateProjectInput) => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      return apiRequest<Project>("/api/projects", {
        method: "POST",
        token,
        body: JSON.stringify({
          workspaceId,
          name: input.name,
          projectKey: input.projectKey,
        }),
      });
    },
    onSuccess: () => {
      setProjectName("");
      setProjectKey("");
      void queryClient.invalidateQueries({ queryKey: ["projects", email] });
    },
  });

  function handleCreateProject(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const cleanedName = projectName.trim();
    const cleanedKey = projectKey.trim().toUpperCase();

    if (cleanedName && cleanedKey) {
      createProjectMutation.mutate({
        name: cleanedName,
        projectKey: cleanedKey,
      });
    }
  }

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
        <Link className="back-link" to="/app">
          ← All workspaces
        </Link>

        {!isValidWorkspaceId && (
          <p className="error-message" role="alert">
            That workspace address is not valid.
          </p>
        )}

        {isValidWorkspaceId && workspaceQuery.isPending && (
          <p className="workspace-status">Loading workspace…</p>
        )}

        {isValidWorkspaceId && workspaceQuery.isError && (
          <p className="error-message" role="alert">
            Could not load workspace: {workspaceQuery.error.message}
          </p>
        )}

        {isValidWorkspaceId && workspaceQuery.isSuccess && (
          <>
            <div className="projects-heading">
              <div>
                <p className="eyebrow">WORKSPACE</p>
                <h1>{workspaceQuery.data.name}</h1>
                <p className="dashboard-description">
                  Projects in this workspace
                </p>
              </div>

              <div className="heading-actions">
                {currentRole && (
                  <span className="role-pill">{currentRole}</span>
                )}

                <Link
                  className="small-action-button small-action-link"
                  to={`/workspaces/${workspaceId}/members`}
                >
                  Manage members
                </Link>
              </div>
            </div>

            {membersQuery.isError && (
              <p className="error-message" role="alert">
                Could not check your workspace role:{" "}
                {membersQuery.error.message}
              </p>
            )}

            {membersQuery.isPending && (
              <p className="workspace-status">Checking your workspace role…</p>
            )}

            {canCreateProject && (
              <form
                className="create-workspace-form"
                onSubmit={handleCreateProject}
              >
                <label className="workspace-name-field" htmlFor="project-name">
                  New project
                  <input
                    id="project-name"
                    maxLength={120}
                    onChange={(event) => setProjectName(event.target.value)}
                    placeholder="For example, Web Platform"
                    required
                    value={projectName}
                  />
                </label>

                <label className="project-key-field" htmlFor="project-key">
                  Project key
                  <input
                    id="project-key"
                    maxLength={10}
                    minLength={2}
                    onChange={(event) =>
                      setProjectKey(
                        event.target.value
                          .toUpperCase()
                          .replace(/[^A-Z0-9]/g, "")
                          .slice(0, 10),
                      )
                    }
                    placeholder="WEB"
                    required
                    value={projectKey}
                  />
                </label>

                <button
                  className="primary-button workspace-create-button"
                  disabled={createProjectMutation.isPending}
                  type="submit"
                >
                  {createProjectMutation.isPending
                    ? "Creating…"
                    : "Create project"}
                </button>
              </form>
            )}

            {currentRole === "MEMBER" && (
              <p className="workspace-status">
                You can view projects. Only workspace owners and admins can
                create them.
              </p>
            )}

            {createProjectMutation.isError && (
              <p className="error-message" role="alert">
                {createProjectMutation.error.message}
              </p>
            )}

            {projectsQuery.isPending && (
              <p className="workspace-status">Loading projects…</p>
            )}

            {projectsQuery.isError && (
              <p className="error-message" role="alert">
                Could not load projects: {projectsQuery.error.message}
              </p>
            )}

            {projectsQuery.isSuccess && projectsQuery.data.length === 0 && (
              <div className="workspace-empty-state">
                <span className="empty-state-mark">P</span>
                <h2>No projects yet</h2>
                <p>
                  {canCreateProject
                    ? "Create the first project for this workspace above."
                    : "A workspace owner or admin can create the first project."}
                </p>
              </div>
            )}

            {projectsQuery.isSuccess && projectsQuery.data.length > 0 && (
              <div className="workspace-grid project-grid">
                {projectsQuery.data.map((project) => (
                  <Link
                    className="workspace-card workspace-card-link"
                    key={project.id}
                    to={`/projects/${project.id}`}
                  >
                    <span className="workspace-card-mark project-card-mark">
                      {project.projectKey.slice(0, 2)}
                    </span>
                    <div>
                      <h2>{project.name}</h2>
                      <p>
                        {project.projectKey} · Project #{project.id}
                      </p>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </main>
  );
}
