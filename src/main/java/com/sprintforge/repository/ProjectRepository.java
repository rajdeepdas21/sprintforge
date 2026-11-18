package com.sprintforge.repository;

import com.sprintforge.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByWorkspaceIdAndProjectKey(Long workspaceId, String projectKey);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Project p
        JOIN Workspace w ON p.workspaceId = w.id
        WHERE p.id = :projectId
          AND (
              w.ownerId = :userId
              OR EXISTS (
                  SELECT wm.workspaceId
                  FROM WorkspaceMember wm
                  WHERE wm.workspaceId = w.id
                    AND wm.userId = :userId
              )
          )
        """)
    boolean isAccessibleByUser(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT p
        FROM Project p
        JOIN Workspace w ON p.workspaceId = w.id
        WHERE w.ownerId = :userId
           OR EXISTS (
               SELECT wm.workspaceId
               FROM WorkspaceMember wm
               WHERE wm.workspaceId = w.id
                 AND wm.userId = :userId
           )
        """)
    List<Project> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT p.workspaceId FROM Project p WHERE p.id = :projectId")
    Optional<Long> findWorkspaceIdByProjectId(@Param("projectId") Long projectId);

    // Check for a duplicate project key while excluding the project being updated.
boolean existsByWorkspaceIdAndProjectKeyAndIdNot(
        Long workspaceId, String projectKey, Long id);

        // Used to block workspace deletion while it still contains projects.
boolean existsByWorkspaceId(Long workspaceId);
}