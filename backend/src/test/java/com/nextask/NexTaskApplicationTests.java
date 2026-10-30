package com.nextask;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nextask.comment.CommentRepository;
import com.nextask.project.ProjectRepository;
import com.nextask.task.TaskRepository;
import com.nextask.user.UserRepository;
import com.nextask.workspace.WorkspaceMemberRepository;
import com.nextask.workspace.WorkspaceRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
@AutoConfigureMockMvc
class NexTaskApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

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

    @Test
    void publishesOpenApiContractWithoutActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("NexTask API"))
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']['scheme']")
                        .value("bearer"))
                .andExpect(jsonPath("$['paths']['/api/auth/login']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['get']['security'][0]['bearerAuth']")
                        .exists())
                .andExpect(jsonPath("$['paths']['/actuator/health']").doesNotExist());
    }

    @Test
    void exposesSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }

    @Test
    void rejectsMissingOrMalformedTokensWithProblemJson() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        assertRejectedToken("invalid.jwt.token");
    }

    @Test
    void rejectsExpiredTokensAndTokensFromAnotherIssuer() throws Exception {
        Instant now = Instant.now();
        assertRejectedToken(signedToken("nextask-test", now.minusSeconds(600), now.minusSeconds(300)));
        assertRejectedToken(signedToken("another-issuer", now, now.plusSeconds(300)));
    }

    private String signedToken(String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private void assertRejectedToken(String token) throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}
