package com.example.week01;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.tasks.seed-demo=false")
@AutoConfigureMockMvc
class TaskApiTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void servesHealthAndDashboard() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("id=\"task-rows\"")));
    }

    @Test
    void createsReadsUpdatesAndDeletesATask() throws Exception {
        var created = mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  Prepare demo  ","description":"Review the endpoints","status":"TODO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Prepare demo"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse();

        var task = json.readTree(created.getContentAsByteArray());
        int id = task.path("id").intValue();
        String path = "/api/tasks/" + id;
        assertThat(created.getHeader("Location"), endsWith(path));

        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Review the endpoints"));
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Demo ready","status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Demo ready"))
                .andExpect(jsonPath("$.description").value(""))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.createdAt").value(task.path("createdAt").asText()));
        mvc.perform(delete(path))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void reportsInvalidFieldsAndMissingTasks() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isString())
                .andExpect(jsonPath("$.errors.status").isString());
        mvc.perform(put("/api/tasks/999").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Demo ready","status":"DONE"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tasks/999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks/invalid")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "\"UNKNOWN\""})
    void rejectsInvalidStatusValues(String statusValue) throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Demo\",\"status\":" + statusValue + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
