package com.sprintforge.repository;

import com.sprintforge.entity.WorkspaceMember;
import com.sprintforge.entity.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {

    List<WorkspaceMember> findAllByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(
            Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @Query("""
        SELECT new com.sprintforge.dto.WorkspaceMemberResponse(
            u.id, u.name, u.email, wm.role, wm.joinedAt
        )
        FROM WorkspaceMember wm
        JOIN User u ON u.id = wm.userId
        WHERE wm.workspaceId = :workspaceId
        """)
    List<com.sprintforge.dto.WorkspaceMemberResponse> findMemberDetailsByWorkspaceId(
            @Param("workspaceId") Long workspaceId);
    
    @Query("""
    SELECT wm.role
    FROM WorkspaceMember wm
    WHERE wm.workspaceId = :workspaceId
      AND wm.userId = :userId
    """)
Optional<String> findRole(
        @Param("workspaceId") Long workspaceId,
        @Param("userId") Long userId);


        // Removes member rows before their workspace is deleted.
void deleteByWorkspaceId(Long workspaceId);
}