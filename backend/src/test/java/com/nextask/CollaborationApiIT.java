package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.comment.api.UpdateCommentRequest;
import com.nextask.entity.User;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.UpdateTaskRequest;
import com.nextask.task.entity.TaskPriority;
import com.nextask.task.entity.TaskStatus;
import com.nextask.user.UserRepository;
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.entity.WorkspaceRole;

@SpringBootTest
@AutoConfigureMockMvc
class CollaborationApiIT {

    private final List<Long> createdWorkspaceIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeCreatedData() {
        createdWorkspaceIds.forEach(workspaceId -> jdbcTemplate.update(
                "DELETE FROM dbo.workspaces WHERE id = ?",
                workspaceId));
        createdUserIds.forEach(userId -> jdbcTemplate.update(
                "DELETE FROM dbo.users WHERE id = ?",
                userId));
    }

    @Test
    void runsProjectTaskAndCommentFlow() throws Exception {
        User owner = createUser("Phase 5 Owner");
        User member = createUser("Phase 5 Member");
        long workspaceId = createWorkspace(owner.getId());
        addMember(workspaceId, member.getId());
        long projectId = createProject(workspaceId, owner.getId());
        OffsetDateTime dueAt = OffsetDateTime.of(
                2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC);
        long taskId = createTask(projectId, owner.getId(), member.getId(), dueAt);
        long commentId = createComment(taskId, member.getId());

        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(projectId));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.content[0].assigneeId").value(member.getId()))
                .andExpect(jsonPath("$.content[0].version").value(0));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(commentId))
                .andExpect(jsonPath("$.content[0].authorName").value("Phase 5 Member"));

        UpdateTaskRequest taskUpdate = new UpdateTaskRequest(
                "Implement API",
                "Contract updated",
                TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM,
                0,
                null,
                null);
        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));

        UpdateCommentRequest commentUpdate = new UpdateCommentRequest("Approved");
        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(commentUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Approved"));
    }

    @Test
    void rejectsAssigneeAndCommentAuthorOutsideWorkspace() throws Exception {
        User owner = createUser("Workspace Owner");
        User outsider = createUser("Workspace Outsider");
        long workspaceId = createWorkspace(owner.getId());
        long projectId = createProject(workspaceId, owner.getId());

        CreateTaskRequest invalidTask = new CreateTaskRequest(
                "Invalid assignment",
                null,
                TaskPriority.HIGH,
                0,
                outsider.getId(),
                owner.getId(),
                null);
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidTask)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:user-must-belong-to-workspace"));

        long taskId = createTask(projectId, owner.getId(), null, null);
        CreateCommentRequest invalidComment = new CreateCommentRequest(
                "I should not comment",
                outsider.getId());
        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidComment)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:user-must-belong-to-workspace"));
    }

    @Test
    void deletingProjectCascadesTasksAndComments() throws Exception {
        User owner = createUser("Cascade Owner");
        long workspaceId = createWorkspace(owner.getId());
        long projectId = createProject(workspaceId, owner.getId());
        long taskId = createTask(projectId, owner.getId(), null, null);
        long commentId = createComment(taskId, owner.getId());

        mockMvc.perform(delete("/api/projects/{projectId}", projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isNotFound());

        Integer taskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.tasks WHERE id = ?",
                Integer.class,
                taskId);
        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.comments WHERE id = ?",
                Integer.class,
                commentId);
        assertThat(taskCount).isZero();
        assertThat(commentCount).isZero();
    }

    private User createUser(String name) {
        String email = "phase5-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.saveAndFlush(new User(name, email, "encoded-password"));
        createdUserIds.add(user.getId());
        return user;
    }

    private long createWorkspace(Long ownerId) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Phase 5 Workspace", ownerId);
        long workspaceId = postAndReadId("/api/workspaces", request);
        createdWorkspaceIds.add(workspaceId);
        return workspaceId;
    }

    private void addMember(Long workspaceId, Long userId) throws Exception {
        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
                userId,
                WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated());
    }

    private long createProject(Long workspaceId, Long creatorId) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phase 5 Project",
                "Integration flow",
                creatorId);
        return postAndReadId("/api/workspaces/" + workspaceId + "/projects", request);
    }

    private long createTask(
            Long projectId,
            Long creatorId,
            Long assigneeId,
            OffsetDateTime dueAt) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "Implement API",
                "Project, task and comments",
                TaskPriority.HIGH,
                1,
                assigneeId,
                creatorId,
                dueAt);
        return postAndReadId("/api/projects/" + projectId + "/tasks", request);
    }

    private long createComment(Long taskId, Long authorId) throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Ready for review", authorId);
        return postAndReadId("/api/tasks/" + taskId + "/comments", request);
    }

    private long postAndReadId(String path, Object request) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.get("id").asLong();
    }
}
