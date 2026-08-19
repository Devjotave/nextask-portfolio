package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.auth.JwtTokenService;
import com.nextask.entity.User;
import com.nextask.user.UserRepository;
import com.nextask.user.api.UserResponse;
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.api.UpdateWorkspaceMemberRequest;
import com.nextask.workspace.api.UpdateWorkspaceRequest;
import com.nextask.workspace.entity.WorkspaceRole;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceApiIT {

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
    void createsWorkspaceWithAuthenticatedOwnerAndListsIt() throws Exception {
        User owner = createUser("Workspace Owner");
        String ownerToken = tokenFor(owner);
        long workspaceId = createWorkspace("Product Team", ownerToken);

        mockMvc.perform(get("/api/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(workspaceId))
                .andExpect(jsonPath("$.content[0].name").value("Product Team"))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));

        mockMvc.perform(get("/api/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(owner.getId()))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
    }

    @Test
    void preservesAnOwnerWhileOwnerManagesMemberships() throws Exception {
        User owner = createUser("Original Owner");
        User member = createUser("Future Owner");
        String ownerToken = tokenFor(owner);
        String memberToken = tokenFor(member);
        long workspaceId = createWorkspace("Engineering", ownerToken);

        AddWorkspaceMemberRequest addRequest = new AddWorkspaceMemberRequest(
                member.getId(),
                WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(delete(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-must-have-owner"));

        UpdateWorkspaceMemberRequest promote = new UpdateWorkspaceMemberRequest(
                WorkspaceRole.OWNER);
        mockMvc.perform(put(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(promote)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(delete(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        owner.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(member.getId()))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
    }

    @Test
    void memberCanReadButCannotManageWorkspace() throws Exception {
        User owner = createUser("Owner");
        User member = createUser("Member");
        String ownerToken = tokenFor(owner);
        String memberToken = tokenFor(member);
        long workspaceId = createWorkspace("Protected", ownerToken);

        AddWorkspaceMemberRequest addRequest = new AddWorkspaceMemberRequest(
                member.getId(),
                WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/workspaces/{workspaceId}", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/workspaces/{workspaceId}", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new UpdateWorkspaceRequest("Forbidden rename"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-access-denied"));
    }

    @Test
    void updatesAndDeletesWorkspaceWithItsMemberships() throws Exception {
        User owner = createUser("Temporary Owner");
        String ownerToken = tokenFor(owner);
        long workspaceId = createWorkspace("Temporary", ownerToken);
        UpdateWorkspaceRequest update = new UpdateWorkspaceRequest("Renamed Workspace");

        mockMvc.perform(put("/api/workspaces/{workspaceId}", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Workspace"));

        mockMvc.perform(delete("/api/workspaces/{workspaceId}", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/workspaces/{workspaceId}", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isNotFound());

        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.workspace_members WHERE workspace_id = ?",
                Integer.class,
                workspaceId);
        assertThat(membershipCount).isZero();
    }

    @Test
    void rejectsDeletingAuthenticatedUserThatStillBelongsToWorkspace() throws Exception {
        User owner = createUser("Protected Owner");
        String ownerToken = tokenFor(owner);
        createWorkspace("Protected Workspace", ownerToken);

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:user-has-workspace-memberships"));
    }

    private User createUser(String name) {
        String email = "phase6-workspace-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.saveAndFlush(new User(name, email, "encoded-password"));
        createdUserIds.add(user.getId());
        return user;
    }

    private long createWorkspace(String name, String ownerToken) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(name);
        MvcResult creation = mockMvc.perform(post("/api/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(creation.getResponse().getContentAsByteArray());
        long workspaceId = response.get("id").asLong();
        createdWorkspaceIds.add(workspaceId);
        return workspaceId;
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
