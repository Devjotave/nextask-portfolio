package com.nextask.workspace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.workspace.api.AddWorkspaceMemberRequest;
import com.nextask.workspace.api.CreateWorkspaceRequest;
import com.nextask.workspace.api.UpdateWorkspaceMemberRequest;
import com.nextask.workspace.api.WorkspaceMemberResponse;
import com.nextask.workspace.api.WorkspaceResponse;
import com.nextask.workspace.entity.WorkspaceRole;
import com.nextask.workspace.error.WorkspaceMustHaveOwnerException;

@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 18, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkspaceService workspaceService;

    @Test
    void createsWorkspace() throws Exception {
        WorkspaceResponse response = new WorkspaceResponse(
                21L,
                "Product Team",
                TIMESTAMP,
                TIMESTAMP);
        when(workspaceService.create(any(CreateWorkspaceRequest.class))).thenReturn(response);

        CreateWorkspaceRequest request = new CreateWorkspaceRequest("Product Team", 7L);

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/workspaces/21"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("Product Team"));
    }

    @Test
    void validatesWorkspaceCreation() throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(" ", null);

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:validation-failed"))
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.errors.ownerId").value("ownerId is required"));
    }

    @Test
    void addsWorkspaceMember() throws Exception {
        WorkspaceMemberResponse response = new WorkspaceMemberResponse(
                8L,
                "Grace Hopper",
                "grace@example.com",
                WorkspaceRole.MEMBER,
                TIMESTAMP);
        when(workspaceService.addMember(
                org.mockito.ArgumentMatchers.eq(21L),
                any(AddWorkspaceMemberRequest.class)))
                .thenReturn(response);

        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest(
                8L,
                WorkspaceRole.MEMBER);

        mockMvc.perform(post("/api/workspaces/21/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/workspaces/21/members/8"))
                .andExpect(jsonPath("$.userId").value(8))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void returnsConflictWhenDemotionWouldRemoveLastOwner() throws Exception {
        when(workspaceService.updateMember(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(7L),
                any(UpdateWorkspaceMemberRequest.class)))
                .thenThrow(new WorkspaceMustHaveOwnerException(21L));

        UpdateWorkspaceMemberRequest request = new UpdateWorkspaceMemberRequest(
                WorkspaceRole.MEMBER);

        mockMvc.perform(put("/api/workspaces/21/members/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:workspace-must-have-owner"));
    }
}
