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

type AddMemberInput = {
  email: string;
  role: string;
};

type UpdateRoleInput = {
  userId: number;
  role: string;
};

export default function WorkspaceMembersPage() {
  const { workspaceId: workspaceIdParam } = useParams();
  const workspaceId = Number(workspaceIdParam);
  const isValidWorkspaceId = Number.isInteger(workspaceId) && workspaceId > 0;

  const { email, logout, token } = useAuth();
  const queryClient = useQueryClient();

  const [newMemberEmail, setNewMemberEmail] = useState("");
  const [newMemberRole, setNewMemberRole] = useState("MEMBER");
  const [newOwnerId, setNewOwnerId] = useState("");

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

  const currentMember = membersQuery.data?.find(
    (member) => member.email.toLowerCase() === email.toLowerCase(),
  );
  const currentRole = currentMember?.role;
  const isOwner = currentRole === "OWNER";
  const canManageMembers = isOwner || currentRole === "ADMIN";

  function refreshWorkspaceData() {
    void queryClient.invalidateQueries({
      queryKey: ["workspace-members", workspaceId, email],
    });
    void queryClient.invalidateQueries({
      queryKey: ["workspace", workspaceId, email],
    });
    void queryClient.invalidateQueries({
      queryKey: ["workspaces", email],
    });
  }

  const addMemberMutation = useMutation({
    mutationFn: (input: AddMemberInput) => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      return apiRequest<WorkspaceMember>(
        `/api/workspaces/${workspaceId}/members`,
        {
          method: "POST",
          token,
          body: JSON.stringify(input),
        },
      );
    },
    onSuccess: () => {
      setNewMemberEmail("");
      setNewMemberRole("MEMBER");
      refreshWorkspaceData();
    },
  });

  const updateRoleMutation = useMutation({
    mutationFn: (input: UpdateRoleInput) => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      return apiRequest<WorkspaceMember>(
        `/api/workspaces/${workspaceId}/members/${input.userId}/role`,
        {
          method: "PATCH",
          token,
          body: JSON.stringify({ role: input.role }),
        },
      );
    },
    onSuccess: refreshWorkspaceData,
  });

  const removeMemberMutation = useMutation({
    mutationFn: (userId: number) => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      return apiRequest<void>(
        `/api/workspaces/${workspaceId}/members/${userId}`,
        {
          method: "DELETE",
          token,
        },
      );
    },
    onSuccess: refreshWorkspaceData,
  });

  const transferOwnershipMutation = useMutation({
    mutationFn: (targetUserId: number) => {
      if (!token)
        throw new Error("Your session expired. Please sign in again.");

      return apiRequest<Workspace>(`/api/workspaces/${workspaceId}/owner`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ newOwnerId: targetUserId }),
      });
    },
    onSuccess: () => {
      setNewOwnerId("");
      refreshWorkspaceData();
    },
  });

  function handleAddMember(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    addMemberMutation.mutate({
      email: newMemberEmail.trim(),
      role: newMemberRole,
    });
  }

  function handleTransferOwnership(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const targetId = Number(newOwnerId);
    const target = membersQuery.data?.find(
      (member) => member.userId === targetId,
    );

    if (
      !target ||
      !window.confirm(`Transfer workspace ownership to ${target.name}?`)
    ) {
      return;
    }

    transferOwnershipMutation.mutate(targetId);
  }

  function handleRemoveMember(member: WorkspaceMember) {
    if (window.confirm(`Remove ${member.name} from this workspace?`)) {
      removeMemberMutation.mutate(member.userId);
    }
  }

  const mutationError =
    addMemberMutation.error ??
    updateRoleMutation.error ??
    removeMemberMutation.error ??
    transferOwnershipMutation.error;

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
        <Link className="back-link" to={`/workspaces/${workspaceId}`}>
          ← Back to projects
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
                <p className="eyebrow">WORKSPACE MEMBERS</p>
                <h1>{workspaceQuery.data.name}</h1>
                <p className="dashboard-description">
                  Manage who can access this workspace.
                </p>
              </div>

              {currentRole && <span className="role-pill">{currentRole}</span>}
            </div>

            {membersQuery.isPending && (
              <p className="workspace-status">Loading members…</p>
            )}

            {membersQuery.isError && (
              <p className="error-message" role="alert">
                Could not load members: {membersQuery.error.message}
              </p>
            )}

            {canManageMembers && (
              <form className="member-form" onSubmit={handleAddMember}>
                <label className="workspace-name-field" htmlFor="member-email">
                  Add existing user
                  <input
                    autoComplete="off"
                    id="member-email"
                    onChange={(event) => setNewMemberEmail(event.target.value)}
                    placeholder="person@example.com"
                    required
                    type="email"
                    value={newMemberEmail}
                  />
                </label>

                <label className="priority-field" htmlFor="member-role">
                  Workspace role
                  <select
                    id="member-role"
                    onChange={(event) => setNewMemberRole(event.target.value)}
                    value={newMemberRole}
                  >
                    {isOwner && <option value="ADMIN">ADMIN</option>}
                    <option value="MEMBER">MEMBER</option>
                    <option value="VIEWER">VIEWER</option>
                  </select>
                </label>

                <button
                  className="primary-button workspace-create-button"
                  disabled={addMemberMutation.isPending}
                  type="submit"
                >
                  {addMemberMutation.isPending ? "Adding…" : "Add member"}
                </button>
              </form>
            )}

            {isOwner && membersQuery.isSuccess && (
              <form
                className="member-form transfer-form"
                onSubmit={handleTransferOwnership}
              >
                <label className="workspace-name-field" htmlFor="new-owner">
                  Transfer ownership
                  <select
                    id="new-owner"
                    onChange={(event) => setNewOwnerId(event.target.value)}
                    required
                    value={newOwnerId}
                  >
                    <option value="">Choose a current member</option>
                    {membersQuery.data
                      .filter((member) => member.role !== "OWNER")
                      .map((member) => (
                        <option key={member.userId} value={member.userId}>
                          {member.name} — {member.email}
                        </option>
                      ))}
                  </select>
                </label>

                <button
                  className="secondary-action-button"
                  disabled={transferOwnershipMutation.isPending || !newOwnerId}
                  type="submit"
                >
                  {transferOwnershipMutation.isPending
                    ? "Transferring…"
                    : "Transfer ownership"}
                </button>
              </form>
            )}

            {mutationError && (
              <p className="error-message" role="alert">
                {mutationError.message}
              </p>
            )}

            {membersQuery.isSuccess && (
              <div className="member-list">
                {membersQuery.data.map((member) => {
                  const canManageThisMember =
                    canManageMembers &&
                    member.role !== "OWNER" &&
                    (isOwner || member.role !== "ADMIN");

                  return (
                    <article className="member-card" key={member.userId}>
                      <div className="member-identity">
                        <span className="member-avatar">
                          {member.name.trim().charAt(0).toUpperCase()}
                        </span>
                        <div>
                          <h2>{member.name}</h2>
                          <p>{member.email}</p>
                          <small>
                            Joined{" "}
                            {new Date(member.joinedAt).toLocaleDateString()}
                          </small>
                        </div>
                      </div>

                      <div className="member-actions">
                        {canManageThisMember ? (
                          <>
                            <label className="member-role-control">
                              <span className="sr-only">
                                Role for {member.name}
                              </span>
                              <select
                                aria-label={`Role for ${member.name}`}
                                onChange={(event) =>
                                  updateRoleMutation.mutate({
                                    userId: member.userId,
                                    role: event.target.value,
                                  })
                                }
                                value={member.role}
                              >
                                {isOwner && (
                                  <option value="ADMIN">ADMIN</option>
                                )}
                                <option value="MEMBER">MEMBER</option>
                                <option value="VIEWER">VIEWER</option>
                              </select>
                            </label>

                            <button
                              className="remove-member-button"
                              disabled={removeMemberMutation.isPending}
                              onClick={() => handleRemoveMember(member)}
                              type="button"
                            >
                              Remove
                            </button>
                          </>
                        ) : (
                          <span className="role-pill">{member.role}</span>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </>
        )}
      </section>
    </main>
  );
}
