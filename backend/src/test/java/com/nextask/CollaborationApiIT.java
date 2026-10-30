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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nextask.auth.JwtTokenService;
import com.nextask.comment.api.CreateCommentRequest;
import com.nextask.comment.api.UpdateCommentRequest;
import com.nextask.entity.User;
import com.nextask.project.api.CreateProjectRequest;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.UpdateTaskRequest;
import com.nextask.task.entity.TaskPriority;
import com.nextask.task.entity.TaskStatus;
import com.nextask.user.UserRepository;
import com.nextask.user.api.UserResponse;
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

    @Autowired
    private JwtTokenService tokenService;

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
    void usesTokenIdentityThroughoutProjectTaskAndCommentFlow() throws Exception {
        User owner = createUser("Phase 6 Owner");
        User member = createUser("Phase 6 Member");
        String ownerToken = tokenFor(owner);
        String memberToken = tokenFor(member);
        long workspaceId = createWorkspace(ownerToken);
        addMember(workspaceId, member.getId(), ownerToken);
        long projectId = createProject(workspaceId, ownerToken);
        OffsetDateTime dueAt = OffsetDateTime.of(
                2026, 9, 1, 18, 0, 0, 0, ZoneOffset.UTC);
        long taskId = createTask(projectId, member.getId(), dueAt, ownerToken);
        long commentId = createComment(taskId, memberToken);

        mockMvc.perform(get("/api/workspaces/{workspaceId}/projects", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(projectId))
                .andExpect(jsonPath("$.content[0].createdById").value(owner.getId()));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.content[0].assigneeId").value(member.getId()))
                .andExpect(jsonPath("$.content[0].createdById").value(owner.getId()))
                .andExpect(jsonPath("$.content[0].version").value(0));

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(commentId))
                .andExpect(jsonPath("$.content[0].authorId").value(member.getId()))
                .andExpect(jsonPath("$.content[0].authorName").value("Phase 6 Member"));

        UpdateTaskRequest taskUpdate = new UpdateTaskRequest(
                "Implement API",
                "Contract updated",
                TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM,
                0,
                null,
                null);
        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andExpect(jsonPath("$.version").value(1));

        UpdateCommentRequest commentUpdate = new UpdateCommentRequest("Approved");
        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(commentUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Approved"));
    }

    @Test
    void rejectsOutsideAssigneeAndOutsideAuthenticatedCommentAuthor() throws Exception {
        User owner = createUser("Workspace Owner");
        User outsider = createUser("Workspace Outsider");
        String ownerToken = tokenFor(owner);
        String outsiderToken = tokenFor(outsider);
        long workspaceId = createWorkspace(ownerToken);
        long projectId = createProject(workspaceId, ownerToken);

        CreateTaskRequest invalidTask = new CreateTaskRequest(
                "Invalid assignment",
                null,
                TaskPriority.HIGH,
                0,
                outsider.getId(),
                null);
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidTask)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:user-must-belong-to-workspace"));

        long taskId = createTask(projectId, null, null, ownerToken);
        CreateCommentRequest invalidComment = new CreateCommentRequest(
                "I should not comment");
        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidComment)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-access-denied"));
    }

    @Test
    void onlyCommentAuthorOrOwnerCanEditComment() throws Exception {
        User owner = createUser("Comment Owner");
        User author = createUser("Comment Author");
        User anotherMember = createUser("Another Member");
        String ownerToken = tokenFor(owner);
        String authorToken = tokenFor(author);
        String anotherToken = tokenFor(anotherMember);
        long workspaceId = createWorkspace(ownerToken);
        addMember(workspaceId, author.getId(), ownerToken);
        addMember(workspaceId, anotherMember.getId(), ownerToken);
        long projectId = createProject(workspaceId, ownerToken);
        long taskId = createTask(projectId, null, null, ownerToken);
        long commentId = createComment(taskId, authorToken);
        UpdateCommentRequest update = new UpdateCommentRequest("Unauthorized edit");

        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(anotherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new UpdateCommentRequest("Owner moderation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Owner moderation"));
    }

    @Test
    void deletingProjectCascadesTasksAndComments() throws Exception {
        User owner = createUser("Cascade Owner");
        String ownerToken = tokenFor(owner);
        long workspaceId = createWorkspace(ownerToken);
        long projectId = createProject(workspaceId, ownerToken);
        long taskId = createTask(projectId, null, null, ownerToken);
        long commentId = createComment(taskId, ownerToken);

        mockMvc.perform(delete("/api/projects/{projectId}", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
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
        String email = "phase6-collaboration-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.saveAndFlush(new User(name, email, "encoded-password"));
        createdUserIds.add(user.getId());
        return user;
    }

    private long createWorkspace(String ownerToken) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Phase 6 Workspace");
        long workspaceId = postAndReadId("/api/workspaces", request, ownerToken);
        createdWorkspaceIds.add(workspaceId);
        return workspaceId;
    }

    private void addMember(Long workspaceId, Long userId, String ownerToken) throws Exception {
        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
                userId,
                WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated());
    }

    private long createProject(Long workspaceId, String token) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phase 6 Project",
                "Integration flow");
        return postAndReadId(
                "/api/workspaces/" + workspaceId + "/projects",
                request,
                token);
    }

    private long createTask(
            Long projectId,
            Long assigneeId,
            OffsetDateTime dueAt,
            String token) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "Implement API",
                "Project, task and comments",
                TaskPriority.HIGH,
                1,
                assigneeId,
                dueAt);
        return postAndReadId("/api/projects/" + projectId + "/tasks", request, token);
    }

    private long createComment(Long taskId, String token) throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Ready for review");
        return postAndReadId("/api/tasks/" + taskId + "/comments", request, token);
    }

    private long postAndReadId(String path, Object request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.get("id").asLong();
    }

    private String tokenFor(User user) {
        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
        return tokenService.issue(response).accessToken();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
