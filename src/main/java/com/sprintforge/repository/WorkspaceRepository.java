
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
          AND w.ownerId = :userId
        """)
boolean isOwnedByUser(
        @Param("workspaceId") Long workspaceId,
        @Param("userId") Long userId
);

List<Workspace> findAllByOwnerId(Long ownerId);
boolean existsByIdAndOwnerId(Long id, Long ownerId);

}
