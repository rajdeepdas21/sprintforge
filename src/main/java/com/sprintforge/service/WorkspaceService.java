package com.sprintforge.service;

import com.sprintforge.dto.AddWorkspaceMemberRequest;
import com.sprintforge.dto.CreateWorkspaceRequest;
import com.sprintforge.dto.UpdateWorkspaceMemberRoleRequest;
import com.sprintforge.dto.WorkspaceMemberResponse;
import com.sprintforge.entity.User;
import com.sprintforge.entity.Workspace;
import com.sprintforge.entity.WorkspaceMember;
import com.sprintforge.repository.WorkspaceMemberRepository;
import com.sprintforge.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.UpdateWorkspaceRequest;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.dto.TransferWorkspaceOwnershipRequest;
import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserService userService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;

    public WorkspaceService(
        WorkspaceRepository workspaceRepository,
        UserService userService,
        WorkspaceMemberRepository workspaceMemberRepository,
        ProjectRepository projectRepository) {
    this.workspaceRepository = workspaceRepository;
    this.userService = userService;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.projectRepository = projectRepository;
}
    // Save the workspace and its OWNER membership in one transaction.
    // If either save fails, both are rolled back.
    @Transactional
    public Workspace createWorkspace(
            CreateWorkspaceRequest request, String email) {
        Long userId = userService.getUserIdByEmail(email);

        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setOwnerId(userId);

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // The creator must also have an OWNER row in workspace_members.
        WorkspaceMember owner = new WorkspaceMember(
                savedWorkspace.getId(),
                userId,
                "OWNER");
        workspaceMemberRepository.save(owner);

        return savedWorkspace;
    }

    // Return a workspace only if it exists and the requester owns it
    // or has a membership row for it.
    public Workspace getWorkspaceById(Long id, String email) {
        Long userId = userService.getUserIdByEmail(email);

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!workspaceRepository.isAccessibleByUser(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }

        return workspace;
    }

    // List workspaces the requester owns or belongs to.
    public List<Workspace> getAllWorkspaces(String email) {
        Long userId = userService.getUserIdByEmail(email);
        return workspaceRepository.findAllAccessibleByUser(userId);
    }

    // A workspace member can see the member list; nonmembers cannot.
    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            Long workspaceId, String email) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        Long userId = userService.getUserIdByEmail(email);

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }

        return workspaceMemberRepository
                .findMemberDetailsByWorkspaceId(workspaceId);
    }

    // Add an existing account to this workspace.
    // OWNER and ADMIN can add members; only OWNER can add an ADMIN.
    public WorkspaceMemberResponse addWorkspaceMember(
            Long workspaceId,
            AddWorkspaceMemberRequest request,
            String email) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        Long requesterId = userService.getUserIdByEmail(email);

        // Find the requester's role in this workspace.
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        // Only workspace owners and admins can add users.
        if (!"OWNER".equals(requester.getRole())
                && !"ADMIN".equals(requester.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can add members");
        }

        // Prevent an ADMIN from granting ADMIN privileges to another user.
        if ("ADMIN".equals(request.role())
                && !"OWNER".equals(requester.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the OWNER can add an ADMIN");
        }

        // The user must already have a SprintForge account.
        User newUser = userService.getUserByEmail(request.email());

        // Prevent duplicate membership rows.
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                workspaceId, newUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already a workspace member");
        }

        WorkspaceMember member = new WorkspaceMember(
                workspaceId,
                newUser.getId(),
                request.role());
        workspaceMemberRepository.save(member);

        // Return the new member's public details.
        return new WorkspaceMemberResponse(
                newUser.getId(),
                newUser.getName(),
                newUser.getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }

    // Change an existing member's role.
    // The OWNER cannot be changed here; ownership transfer needs a
    // separate operation. Only the OWNER can grant or change ADMIN roles.
    @Transactional
    public WorkspaceMember updateWorkspaceMemberRole(
            Long workspaceId,
            Long targetUserId,
            UpdateWorkspaceMemberRoleRequest request,
            String requesterEmail) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found");
        }

        Long requesterId = userService.getUserIdByEmail(requesterEmail);

        // Find the requester and verify they belong to this workspace.
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        boolean requesterIsOwner = "OWNER".equals(requester.getRole());
        boolean requesterIsAdmin = "ADMIN".equals(requester.getRole());

        // Members and viewers cannot change anyone's role.
        if (!requesterIsOwner && !requesterIsAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only OWNER or ADMIN can change member roles");
        }

        // Find the member whose role is being changed.
        WorkspaceMember target = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workspace member not found"));

        // This endpoint cannot remove or replace the designated owner.
        if ("OWNER".equals(target.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The OWNER role cannot be changed here");
        }

        // An ADMIN cannot promote someone to ADMIN or change another ADMIN.
        if (!requesterIsOwner
                && ("ADMIN".equals(target.getRole())
                    || "ADMIN".equals(request.role()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the OWNER can change ADMIN roles");
        }

        // The request DTO limits the new role to ADMIN, MEMBER, or VIEWER.
        target.setRole(request.role());
        return workspaceMemberRepository.save(target);
    }

    @Transactional
public void removeWorkspaceMember(
        Long workspaceId,
        Long targetUserId,
        String requesterEmail) {

    if (!workspaceRepository.existsById(workspaceId)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Workspace not found");
    }

    Long requesterId = userService.getUserIdByEmail(requesterEmail);

    WorkspaceMember requester = workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, requesterId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied"));

    boolean requesterIsOwner = "OWNER".equals(requester.getRole());
    boolean requesterIsAdmin = "ADMIN".equals(requester.getRole());

    if (!requesterIsOwner && !requesterIsAdmin) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only OWNER or ADMIN can remove members");
    }

    WorkspaceMember target = workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace member not found"));

    if ("OWNER".equals(target.getRole())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "The OWNER cannot be removed");
    }

    if ("ADMIN".equals(target.getRole()) && !requesterIsOwner) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the OWNER can remove an ADMIN");
    }

    workspaceMemberRepository.delete(target);
}
// Only the workspace's designated owner can rename it.
@Transactional
public Workspace updateWorkspace(
        Long workspaceId,
        UpdateWorkspaceRequest request,
        String requesterEmail) {
    Workspace workspace = findWorkspaceForOwner(workspaceId, requesterEmail);

    workspace.setName(request.getName());
    return workspaceRepository.save(workspace);
}

// Delete only an empty workspace, cleaning up member rows first.
@Transactional
public void deleteWorkspace(Long workspaceId, String requesterEmail) {
    Workspace workspace = findWorkspaceForOwner(workspaceId, requesterEmail);

    if (projectRepository.existsByWorkspaceId(workspaceId)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delete the workspace's projects before deleting the workspace");
    }

    // Membership rows reference the workspace, so remove them first.
    workspaceMemberRepository.deleteByWorkspaceId(workspaceId);
    workspaceRepository.delete(workspace);
}

// Load the workspace and verify its designated owner is making the change.
private Workspace findWorkspaceForOwner(Long workspaceId, String requesterEmail) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found"));

    Long requesterId = userService.getUserIdByEmail(requesterEmail);

    if (!workspace.getOwnerId().equals(requesterId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the workspace owner can manage the workspace");
    }

    return workspace;
}

// Only the current owner can transfer ownership, and the new owner
// must already belong to this workspace.
@Transactional
public Workspace transferWorkspaceOwnership(
        Long workspaceId,
        TransferWorkspaceOwnershipRequest request,
        String requesterEmail) {

    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Workspace not found"));

    Long requesterId = userService.getUserIdByEmail(requesterEmail);

    // Use the workspace's owner_id as the authoritative owner check.
    if (!workspace.getOwnerId().equals(requesterId)) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the current owner can transfer ownership");
    }

    Long newOwnerId = request.newOwnerId();

    if (requesterId.equals(newOwnerId)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The new owner must be a different user");
    }

    WorkspaceMember currentOwner = workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, requesterId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Current owner membership is missing"));

    WorkspaceMember newOwner = workspaceMemberRepository
            .findByWorkspaceIdAndUserId(workspaceId, newOwnerId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The new owner must already be a workspace member"));

    // Demote the current owner first so the single-OWNER index is respected.
    currentOwner.setRole("ADMIN");
    workspaceMemberRepository.saveAndFlush(currentOwner);

    // Promote the selected existing member and update the workspace record.
    newOwner.setRole("OWNER");
    workspaceMemberRepository.save(newOwner);

    workspace.setOwnerId(newOwnerId);
    return workspaceRepository.save(workspace);
}
}