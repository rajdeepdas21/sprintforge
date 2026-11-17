package com.sprintforge.repository;

import com.sprintforge.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END
        FROM Workspace w
        WHERE w.id = :workspaceId
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
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT w
        FROM Workspace w
        WHERE w.ownerId = :userId
           OR EXISTS (
               SELECT wm.workspaceId
               FROM WorkspaceMember wm
               WHERE wm.workspaceId = w.id
                 AND wm.userId = :userId
           )
        """)
    List<Workspace> findAllAccessibleByUser(@Param("userId") Long userId);
}