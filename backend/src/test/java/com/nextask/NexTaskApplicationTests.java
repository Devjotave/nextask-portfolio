package com.nextask;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @Test
    void contextLoads() {
    }
}
