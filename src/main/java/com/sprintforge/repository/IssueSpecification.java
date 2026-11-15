
package com.sprintforge.repository;

import com.sprintforge.entity.Issue;
import org.springframework.data.jpa.domain.Specification;

public class IssueSpecification {

    private IssueSpecification() {
    }

    public static Specification<Issue> hasProjectId(Long projectId) {
        return (root, query, cb) ->
                cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<Issue> hasStatus(String status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Issue> hasPriority(String priority) {
        return (root, query, cb) ->
                cb.equal(root.get("priority"), priority);
    }

    public static Specification<Issue> hasSprintId(Long sprintId) {
        return (root, query, cb) ->
                cb.equal(root.get("sprintId"), sprintId);
    }

    public static Specification<Issue> titleContains(String title) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                );
    }
}
