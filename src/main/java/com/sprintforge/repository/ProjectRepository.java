
package com.sprintforge.repository;

import com.sprintforge.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
//import com.sprintforge.entity.Workspace;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByWorkspaceIdAndProjectKey(
            Long workspaceId,
            String projectKey
    );

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Project p
        JOIN Workspace w ON p.workspaceId = w.id
        WHERE p.id = :projectId
        AND w.ownerId = :userId
    """)
    boolean isOwnedByUser(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );


@Query("""
    SELECT p
    FROM Project p
    WHERE p.workspaceId IN (
        SELECT w.id
        FROM Workspace w
        WHERE w.ownerId = :userId
    )
""")
List<Project> findAllByUserId(@Param("userId") Long userId);

}
