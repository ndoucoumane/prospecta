package com.prospecta.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import com.prospecta.shared.exception.TaskNotFoundException;
import com.prospecta.task.dto.TaskDto.*;
import com.prospecta.task.service.CommercialTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommercialTaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommercialTaskService taskService;

    @InjectMocks
    private CommercialTaskController taskController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/tasks returns task list")
    void shouldReturnTaskList() throws Exception {
        CommercialTaskResponse task = CommercialTaskResponse.builder()
                .id(UUID.randomUUID().toString())
                .type("call")
                .title("Appel de qualification")
                .dueDate("2026-09-12")
                .status("pending")
                .build();

        when(taskService.getTasks(null, null)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Appel de qualification"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks creates task successfully")
    void shouldCreateTask() throws Exception {
        CreateCommercialTaskRequest request = CreateCommercialTaskRequest.builder()
                .type("call")
                .title("Nouveau rappel client")
                .dueDate("2026-09-15")
                .build();

        CommercialTaskResponse response = CommercialTaskResponse.builder()
                .id(UUID.randomUUID().toString())
                .type("call")
                .title("Nouveau rappel client")
                .dueDate("2026-09-15")
                .status("pending")
                .build();

        when(taskService.createTask(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Nouveau rappel client"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/conv-1 returns 404 RESOURCE_NOT_FOUND (not 500)")
    void shouldReturn404ForNonUuidId() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/conv-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 404 when task not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTaskById(id)).thenThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/api/v1/tasks/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TASK_NOT_FOUND"));
    }
}
