package com.nextask;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nextask.comment.CommentRepository;
import com.nextask.project.ProjectRepository;
import com.nextask.task.TaskRepository;
import com.nextask.user.UserRepository;
import com.nextask.workspace.WorkspaceMemberRepository;
import com.nextask.workspace.WorkspaceRepository;

@SpringBootTest(properties = {
        "spring.profiles.active=production",
        "OPENAPI_ENABLED=false",
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
@AutoConfigureMockMvc
class ProductionOpenApiDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private WorkspaceRepository workspaceRepository;
    @MockitoBean
    private WorkspaceMemberRepository workspaceMemberRepository;
    @MockitoBean
    private ProjectRepository projectRepository;
    @MockitoBean
    private TaskRepository taskRepository;
    @MockitoBean
    private CommentRepository commentRepository;

    @Test
    void doesNotPublishApiDocsOrSwaggerUiInProduction() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound());
    }
}
