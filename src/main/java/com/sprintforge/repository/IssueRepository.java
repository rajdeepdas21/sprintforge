
package com.sprintforge.repository;

import com.sprintforge.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long>,
        JpaSpecificationExecutor<Issue> {

    List<Issue> findByProjectId(Long projectId);

    List<Issue> findBySprintId(Long sprintId);

    // Used to prevent deleting a project or sprint that still has issues.
boolean existsByProjectId(Long projectId);
boolean existsBySprintId(Long sprintId);
}
