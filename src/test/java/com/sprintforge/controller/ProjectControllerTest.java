
package com.sprintforge.controller;

import com.sprintforge.dto.CreateProjectRequest;
import com.sprintforge.entity.Project;
import com.sprintforge.service.ProjectService;
import com.sprintforge.config.JwtAuthenticationFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.isNull;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createProjectReturns201WhenRequestIsValid() throws Exception {

        Project project = new Project();
        project.setWorkspaceId(1L);
        project.setName("SprintForge Backend");
        project.setProjectKey("SFB");

       when(projectService.createProject(
        any(CreateProjectRequest.class),
        isNull()))
        .thenReturn(project);

        CreateProjectRequest request = new CreateProjectRequest();
        request.setWorkspaceId(1L);
        request.setName("SprintForge Backend");
        request.setProjectKey("SFB");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SprintForge Backend"))
                .andExpect(jsonPath("$.projectKey").value("SFB"))
                .andExpect(jsonPath("$.workspaceId").value(1));
    }

    @Test
    void createProjectReturns400WhenProjectKeyIsLowercase() throws Exception {

        CreateProjectRequest request = new CreateProjectRequest();
        request.setWorkspaceId(1L);
        request.setName("Test Project");
        request.setProjectKey("sfb");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getProjectByIdReturns200WhenProjectExists() throws Exception {

        Project project = new Project();
        project.setWorkspaceId(1L);
        project.setName("SprintForge Backend");
        project.setProjectKey("SFB");

        when(projectService.getProjectById(1L, null))
        .thenReturn(project);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SprintForge Backend"))
                .andExpect(jsonPath("$.projectKey").value("SFB"))
                .andExpect(jsonPath("$.workspaceId").value(1));
    }

    @Test
    void getProjectByIdReturns404WhenProjectDoesNotExist() throws Exception {

        when(projectService.getProjectById(9999L,null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project not found"
                ));

        mockMvc.perform(get("/api/projects/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void getAllProjectsReturns200AndProjectList() throws Exception {

        Project first = new Project();
        first.setWorkspaceId(1L);
        first.setName("SprintForge Backend");
        first.setProjectKey("SFB");

        Project second = new Project();
        second.setWorkspaceId(1L);
        second.setName("Validation Test");
        second.setProjectKey("SF2");

        when(projectService.getAllProjects(null))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("SprintForge Backend"))
                .andExpect(jsonPath("$[1].name").value("Validation Test"));
    }

    @Test
    void getAllProjectsReturnsEmptyArrayWhenNoProjectsExist() throws Exception {

        when(projectService.getAllProjects(null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
