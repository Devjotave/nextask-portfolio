package com.nextask;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.nextask.comment.CommentRepository;
import com.nextask.project.ProjectRepository;
import com.nextask.task.TaskRepository;
import com.nextask.user.UserRepository;
import com.nextask.workspace.WorkspaceMemberRepository;
import com.nextask.workspace.WorkspaceRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class NexTaskApplicationTests {

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
    void contextLoads() {
    }
}
