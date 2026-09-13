package com.forum.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForumApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper mapper;

    @BeforeEach
    void cleanData() throws IOException {
        Path dataDir = Path.of("target/test-data");
        if (Files.exists(dataDir)) {
            try (var stream = Files.list(dataDir)) {
                stream.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
            }
        } else {
            Files.createDirectories(dataDir);
        }
    }

    @Test
    void fullFlowRespectsMaxDepthAndUnlimitedSetting() throws Exception {
        // register owner
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"owner","password":"secret123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("owner"))
                .andExpect(jsonPath("$.maxReplyDepth").value(3));

        // login owner
        String token = login("owner", "secret123");

        // create discussion
        MvcResult discussionResult = mockMvc.perform(post("/api/discussions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Hello World","content":"First discussion"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxReplyDepth").value(3))
                .andExpect(jsonPath("$.author.username").value("owner"))
                .andReturn();
        UUID discussionId = extractId(discussionResult);

        // direct comment level 1
        UUID c1 = createComment(token, discussionId, "level 1", null);
        // reply level 2
        UUID c2 = createComment(token, discussionId, "level 2", c1);
        // reply level 3 should be allowed (max 3)
        UUID c3 = createComment(token, discussionId, "level 3", c2);

        // level 4 should be rejected with maxDepth 3
        mockMvc.perform(post("/api/discussions/" + discussionId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content":"level 4","parentId":"%s"}
                        """.formatted(c3)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Maximum reply depth exceeded"));

        // verify discussion has maxReplyDepth 3 before change
        mockMvc.perform(get("/api/discussions/" + discussionId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxReplyDepth").value(3));

        // change to unlimited
        mockMvc.perform(patch("/api/users/me/settings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"maxReplyDepth":null}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxReplyDepth").doesNotExist());

        // now level 4 should succeed
        UUID c4 = createComment(token, discussionId, "level 4 unlimited", c3);
        // level 5 also succeeds
        createComment(token, discussionId, "level 5 unlimited", c4);

        // verify tree is nested and maxReplyDepth is now unlimited (null)
        mockMvc.perform(get("/api/discussions/" + discussionId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxReplyDepth").doesNotExist())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments[0].replies[0].replies[0].replies[0].content")
                        .value("level 4 unlimited"));

        // list discussions includes comment count
        mockMvc.perform(get("/api/discussions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentCount").value(5));

        // my discussions
        mockMvc.perform(get("/api/users/me/discussions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(discussionId.toString()));

        // unauthenticated should be 401
        mockMvc.perform(get("/api/discussions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerDuplicateUsernameReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"dup","password":"secret123"}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"dup","password":"secret123"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"loginUser","password":"secret123"}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"loginUser","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void participatingExcludesOwnDiscussions() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"alice","password":"secret123"}
                        """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"bob","password":"secret123"}
                        """))
                .andExpect(status().isCreated());

        String tokenAlice = login("alice", "secret123");
        String tokenBob = login("bob", "secret123");

        MvcResult res = mockMvc.perform(post("/api/discussions")
                .header("Authorization", "Bearer " + tokenAlice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Alice topic","content":"content"}
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        UUID discId = extractId(res);

        createComment(tokenBob, discId, "Hola", null);

        mockMvc.perform(get("/api/users/me/participating")
                .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/users/me/participating")
                .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(discId.toString()));

        mockMvc.perform(get("/api/users/me/discussions")
                .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    private UUID createComment(String token, UUID discussionId, String content, UUID parentId) throws Exception {
        String body = parentId == null
                ? """
                        {"content":"%s"}
                        """.formatted(content)
                : """
                        {"content":"%s","parentId":"%s"}
                        """.formatted(content, parentId);
        MvcResult result = mockMvc.perform(post("/api/discussions/" + discussionId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(node.get("id").asText());
    }
}
