import { useState, type SubmitEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Link } from "react-router";
type Workspace = {
  id: number;
  name: string;
  ownerId: number;
};

export default function DashboardPage() {
  const { email, logout, token } = useAuth();
  const queryClient = useQueryClient();
  const [workspaceName, setWorkspaceName] = useState("");

  // Load the workspaces visible to this signed-in user.
  const workspacesQuery = useQuery({
    queryKey: ["workspaces", email],
    queryFn: () => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Workspace[]>("/api/workspaces", { token });
    },
  });

  // Create a workspace, then refresh the list so it appears immediately.
  const createWorkspaceMutation = useMutation({
    mutationFn: (name: string) => {
      if (!token) {
        throw new Error("Your session expired. Please sign in again.");
      }

      return apiRequest<Workspace>("/api/workspaces", {
        method: "POST",
        token,
        body: JSON.stringify({ name }),
      });
    },
    onSuccess: () => {
      setWorkspaceName("");
      void queryClient.invalidateQueries({
        queryKey: ["workspaces", email],
      });
    },
  });

  function handleCreateWorkspace(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const cleanedName = workspaceName.trim();
    if (cleanedName) {
      createWorkspaceMutation.mutate(cleanedName);
    }
  }

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div className="brand">
          <span className="brand-mark">SF</span>
          <span>SprintForge</span>
        </div>

        <button className="signout-button" onClick={logout} type="button">
          Sign out
        </button>
      </header>

      <section className="dashboard-main">
        <div className="dashboard-intro">
          <p className="eyebrow">WORKSPACE OVERVIEW</p>
          <h1>Your workspaces</h1>
          <p className="dashboard-description">
            Signed in as <strong>{email}</strong>
          </p>
        </div>

        <form
          className="create-workspace-form"
          onSubmit={handleCreateWorkspace}
        >
          <label className="workspace-name-field" htmlFor="workspace-name">
            New workspace
            <input
              id="workspace-name"
              maxLength={120}
              onChange={(event) => setWorkspaceName(event.target.value)}
              placeholder="For example, Product Team"
              required
              value={workspaceName}
            />
          </label>

          <button
            className="primary-button workspace-create-button"
            disabled={createWorkspaceMutation.isPending}
            type="submit"
          >
            {createWorkspaceMutation.isPending
              ? "Creating…"
              : "Create workspace"}
          </button>
        </form>

        {createWorkspaceMutation.isError && (
          <p className="error-message" role="alert">
            {createWorkspaceMutation.error.message}
          </p>
        )}

        {workspacesQuery.isPending && (
          <p className="workspace-status">Loading your workspaces…</p>
        )}

        {workspacesQuery.isError && (
          <p className="error-message" role="alert">
            Could not load workspaces: {workspacesQuery.error.message}
          </p>
        )}

        {workspacesQuery.isSuccess && workspacesQuery.data.length === 0 && (
          <div className="workspace-empty-state">
            <span className="empty-state-mark">W</span>
            <h2>No workspaces yet</h2>
            <p>Create a workspace above to start organizing your projects.</p>
          </div>
        )}

        {workspacesQuery.isSuccess && workspacesQuery.data.length > 0 && (
          <>
            <p className="workspace-count">
              {workspacesQuery.data.length}{" "}
              {workspacesQuery.data.length === 1 ? "workspace" : "workspaces"}
            </p>

            <div className="workspace-grid">
              {workspacesQuery.data.map((workspace) => (
                <Link
                  className="workspace-card workspace-card-link"
                  key={workspace.id}
                  to={`/workspaces/${workspace.id}`}
                >
                  <span className="workspace-card-mark">W</span>
                  <div>
                    <h2>{workspace.name}</h2>
                    <p>Workspace #{workspace.id}</p>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </section>
    </main>
  );
}
