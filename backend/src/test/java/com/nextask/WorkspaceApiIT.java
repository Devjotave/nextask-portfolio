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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.entity.User;
import com.nextask.user.UserRepository;
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
    void createsWorkspaceWithOwnerAndListsItForThatUser() throws Exception {
        User owner = createUser("Workspace Owner");
        long workspaceId = createWorkspace("Product Team", owner.getId());

        mockMvc.perform(get("/api/workspaces")
                        .param("userId", owner.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(workspaceId))
                .andExpect(jsonPath("$.content[0].name").value("Product Team"))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));

        mockMvc.perform(get("/api/workspaces/{workspaceId}/members", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(owner.getId()))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
    }

    @Test
    void preservesAnOwnerWhileMembershipsChange() throws Exception {
        User owner = createUser("Original Owner");
        User member = createUser("Future Owner");
        long workspaceId = createWorkspace("Engineering", owner.getId());

        AddWorkspaceMemberRequest addRequest = new AddWorkspaceMemberRequest(
                member.getId(),
                WorkspaceRole.MEMBER);
        mockMvc.perform(post("/api/workspaces/{workspaceId}/members", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(delete(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        owner.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-must-have-owner"));

        UpdateWorkspaceMemberRequest promote = new UpdateWorkspaceMemberRequest(
                WorkspaceRole.OWNER);
        mockMvc.perform(put(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        member.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(promote)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(delete(
                        "/api/workspaces/{workspaceId}/members/{userId}",
                        workspaceId,
                        owner.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workspaces/{workspaceId}/members", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(member.getId()))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
    }

    @Test
    void updatesAndDeletesWorkspaceWithItsMemberships() throws Exception {
        User owner = createUser("Temporary Owner");
        long workspaceId = createWorkspace("Temporary", owner.getId());
        UpdateWorkspaceRequest update = new UpdateWorkspaceRequest("Renamed Workspace");

        mockMvc.perform(put("/api/workspaces/{workspaceId}", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Workspace"));

        mockMvc.perform(delete("/api/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/workspaces/{workspaceId}", workspaceId))
                .andExpect(status().isNotFound());

        Integer membershipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.workspace_members WHERE workspace_id = ?",
                Integer.class,
                workspaceId);
        assertThat(membershipCount).isZero();
    }

    @Test
    void rejectsDeletingUserThatStillBelongsToWorkspace() throws Exception {
        User owner = createUser("Protected Owner");
        createWorkspace("Protected Workspace", owner.getId());

        mockMvc.perform(delete("/api/users/{id}", owner.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:user-has-workspace-memberships"));
    }

    private User createUser(String name) {
        String email = "phase4-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.saveAndFlush(new User(name, email, "encoded-password"));
        createdUserIds.add(user.getId());
        return user;
    }

    private long createWorkspace(String name, Long ownerId) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(name, ownerId);
        MvcResult creation = mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(creation.getResponse().getContentAsByteArray());
        long workspaceId = response.get("id").asLong();
        createdWorkspaceIds.add(workspaceId);
        return workspaceId;
    }
}
