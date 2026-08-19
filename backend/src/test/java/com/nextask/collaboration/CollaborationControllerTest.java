package com.nextask.collaboration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.comment.CommentController;
import com.nextask.comment.CommentService;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.project.ProjectController;
import com.nextask.project.ProjectService;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.project.api.ProjectResponse;
import com.nextask.task.TaskController;
import com.nextask.task.TaskService;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.TaskResponse;
import com.nextask.task.entity.TaskPriority;
import com.nextask.task.entity.TaskStatus;
import com.nextask.workspace.error.WorkspaceAccessDeniedException;

@WebMvcTest({ProjectController.class, TaskController.class, CommentController.class})
@AutoConfigureMockMvc(addFilters = false)
class CollaborationControllerTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private CommentService commentService;

    @Test
    void createsProjectAtNestedWorkspaceEndpoint() throws Exception {
        ProjectResponse response = new ProjectResponse(
                20L,
                10L,
                "Product",
                "Roadmap",
                1L,
                TIMESTAMP,
                TIMESTAMP);
        when(projectService.create(eq(10L), any(CreateProjectRequest.class)))
                .thenReturn(response);

        CreateProjectRequest request = new CreateProjectRequest(
                "Product",
                "Roadmap");

        mockMvc.perform(post("/api/workspaces/10/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/projects/20"))
                .andExpect(jsonPath("$.workspaceId").value(10))
                .andExpect(jsonPath("$.name").value("Product"));
    }

    @Test
    void validatesTaskCreation() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                " ",
                null,
                null,
                -1,
                null,
                null);

        mockMvc.perform(post("/api/projects/20/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:validation-failed"))
                .andExpect(jsonPath("$.errors.title").value("title is required"))
                .andExpect(jsonPath("$.errors.priority").value("priority is required"))
                .andExpect(jsonPath("$.errors.position")
                        .value("position must not be negative"));
    }

    @Test
    void createsTaskInTodo() throws Exception {
        TaskResponse response = new TaskResponse(
                30L,
                20L,
                "API contract",
                null,
                TaskStatus.TODO,
                TaskPriority.HIGH,
                0,
                2L,
                1L,
                null,
                0L,
                TIMESTAMP,
                TIMESTAMP);
        when(taskService.create(eq(20L), any(CreateTaskRequest.class))).thenReturn(response);

        CreateTaskRequest request = new CreateTaskRequest(
                "API contract",
                null,
                TaskPriority.HIGH,
                0,
                2L,
                null);

        mockMvc.perform(post("/api/projects/20/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tasks/30"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void rejectsAuthenticatedUserOutsideWorkspace() throws Exception {
        when(commentService.create(eq(30L), any(CreateCommentRequest.class)))
                .thenThrow(new WorkspaceAccessDeniedException(10L));
        CreateCommentRequest request = new CreateCommentRequest("Review");

        mockMvc.perform(post("/api/tasks/30/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-access-denied"));
    }
}
