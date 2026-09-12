package com.prospecta.task.controller;

import com.prospecta.shared.dto.ApiResponse;
import com.prospecta.task.dto.TaskDto.*;
import com.prospecta.task.service.CommercialTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Commercial Tasks", description = "Management of commercial tasks, calls, reminders, and follow-ups")
public class CommercialTaskController {

    private final CommercialTaskService taskService;

    @GetMapping
    @Operation(summary = "Get commercial tasks list (filtered by status, prospectId)")
    public ResponseEntity<ApiResponse<List<CommercialTaskResponse>>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID prospectId
    ) {
        String filterStatus = (status != null && !"all".equalsIgnoreCase(status)) ? status.toLowerCase() : null;
        List<CommercialTaskResponse> tasks = taskService.getTasks(filterStatus, prospectId);
        return ResponseEntity.ok(ApiResponse.of(tasks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single commercial task by ID")
    public ResponseEntity<ApiResponse<CommercialTaskResponse>> getTaskById(@PathVariable UUID id) {
        CommercialTaskResponse task = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.of(task));
    }

    @PostMapping
    @Operation(summary = "Create a new commercial task")
    public ResponseEntity<ApiResponse<CommercialTaskResponse>> createTask(
            @Valid @RequestBody CreateCommercialTaskRequest request
    ) {
        CommercialTaskResponse task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(task));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an existing commercial task")
    public ResponseEntity<ApiResponse<CommercialTaskResponse>> updateTask(
            @PathVariable UUID id,
            @RequestBody UpdateCommercialTaskRequest request
    ) {
        CommercialTaskResponse task = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.of(task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a commercial task")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
