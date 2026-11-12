
// package com.sprintforge.service;

// import com.sprintforge.dto.CreateIssueRequest;
// import com.sprintforge.entity.Issue;
// import com.sprintforge.repository.IssueRepository;
// import com.sprintforge.repository.ProjectRepository;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.web.server.ResponseStatusException;

// @Service
// public class IssueService {

//     private final IssueRepository issueRepository;
//     private final ProjectRepository projectRepository;

//     public IssueService(
//             IssueRepository issueRepository,
//             ProjectRepository projectRepository) {
//         this.issueRepository = issueRepository;
//         this.projectRepository = projectRepository;
//     }

//     public Issue createIssue(CreateIssueRequest request) {

//         if (!projectRepository.existsById(request.getProjectId())) {
//             throw new ResponseStatusException(
//                     HttpStatus.NOT_FOUND,
//                     "Project not found"
//             );
//         }

//         Issue issue = new Issue();
//         issue.setProjectId(request.getProjectId());
//         issue.setTitle(request.getTitle());
//         issue.setDescription(request.getDescription());

//         if (request.getPriority() != null) {
//             issue.setPriority(request.getPriority());
//         }

//         return issueRepository.save(issue);
//     }
//     public Issue getIssueById(Long id) {
//         return issueRepository.findById(id)
//             .orElseThrow(() -> new ResponseStatusException(
//                     HttpStatus.NOT_FOUND,
//                     "Issue not found"
//             ));
//     }
//     public java.util.List<Issue> getIssuesByProjectId(Long projectId) {
//         if (!projectRepository.existsById(projectId)) {
//             throw new ResponseStatusException(
//                 HttpStatus.NOT_FOUND,
//                 "Project not found"
//             );
//         }
//         return issueRepository.findByProjectId(projectId);
//     }


// }

package com.sprintforge.service;

import com.sprintforge.dto.CreateIssueRequest;
import com.sprintforge.entity.Issue;
import com.sprintforge.repository.IssueRepository;
import com.sprintforge.repository.ProjectRepository;
import com.sprintforge.repository.SprintRepository;
import com.sprintforge.entity.Sprint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sprintforge.dto.UpdateIssueRequest;
import java.util.List;
import java.util.Set;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;

    private static final Set<String> VALID_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    public IssueService(
        IssueRepository issueRepository,
        ProjectRepository projectRepository,
        SprintRepository sprintRepository) {

    this.issueRepository = issueRepository;
    this.projectRepository = projectRepository;
    this.sprintRepository = sprintRepository;
}

    public Issue createIssue(CreateIssueRequest request) {

        // Validate priority before accessing the database.
        String priority = request.getPriority();

        if (priority != null && !VALID_PRIORITIES.contains(priority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Priority must be LOW, MEDIUM, HIGH, or URGENT"
            );
        }

        if (!projectRepository.existsById(request.getProjectId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            );
        }

        Issue issue = new Issue();
        issue.setProjectId(request.getProjectId());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());

        if (priority != null) {
            issue.setPriority(priority);
        }

        return issueRepository.save(issue);
    }

    public Issue getIssueById(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Issue not found"
                ));
    }

    public List<Issue> getIssuesByProjectId(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Project not found"
            );
        }

        return issueRepository.findByProjectId(projectId);
    }

    public Issue updateIssueStatus(Long id, String status) {

    Set<String> validStatuses = Set.of(
        "TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"
    );

    if (status == null || !validStatuses.contains(status)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Status must be TODO, IN_PROGRESS, IN_REVIEW, or DONE"
        );
    }

    Issue issue = issueRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Issue not found"
        ));

    issue.setStatus(status);

    return issueRepository.save(issue);
}


public Issue assignIssueToSprint(Long issueId, Long sprintId) {

    Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Issue not found"
            ));

    Sprint sprint = sprintRepository.findById(sprintId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Sprint not found"
            ));

    if (!issue.getProjectId().equals(sprint.getProjectId())) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Issue and sprint must belong to the same project"
        );
    }

    issue.setSprintId(sprintId);

    return issueRepository.save(issue);
}

public List<Issue> getIssuesBySprintId(Long sprintId) {

    if (!sprintRepository.existsById(sprintId)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Sprint not found"
        );
    }

    return issueRepository.findBySprintId(sprintId);
}


public Issue updateIssue(Long issueId, UpdateIssueRequest request) {

    Issue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Issue not found"
            ));

    String priority = request.getPriority();

    if (priority != null && !VALID_PRIORITIES.contains(priority)) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Priority must be LOW, MEDIUM, HIGH, or URGENT"
        );
    }

    issue.setTitle(request.getTitle());

    if (request.getDescription() != null) {
        issue.setDescription(request.getDescription());
    }

    if (priority != null) {
        issue.setPriority(priority);
    }

    return issueRepository.save(issue);
}

public void deleteIssue(Long issueId) {
    if (!issueRepository.existsById(issueId)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Issue not found"
        );
    }

    issueRepository.deleteById(issueId);
}

}
