package com.nextask.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SecurityProblemWriterTest {

    @Test
    void writesForbiddenResponseWithJackson3() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new SecurityProblemWriter(mapper).write(response, 403,
                "urn:nextask:problem:access-denied", "Access denied",
                "The authenticated user does not have permission for this operation");

        JsonNode body = mapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(body.path("status").asInt()).isEqualTo(403);
        assertThat(body.path("title").asText()).isEqualTo("Access denied");
    }
}
