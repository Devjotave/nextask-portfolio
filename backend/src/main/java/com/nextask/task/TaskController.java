package com.nextask.task;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nextask.api.PageResponse;
import com.nextask.task.api.CreateTaskRequest;
import com.nextask.task.api.TaskResponse;
import com.nextask.task.api.UpdateTaskRequest;
import com.nextask.task.entity.TaskStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = taskService.create(projectId, request);
        return ResponseEntity
                .created(URI.create("/api/tasks/" + task.id()))
                .body(task);
    }

    @GetMapping("/projects/{projectId}/tasks")
    PageResponse<TaskResponse> findAll(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return taskService.findAll(projectId, status, page, size);
    }

    @GetMapping("/tasks/{taskId}")
    TaskResponse findById(@PathVariable Long taskId) {
        return taskService.findById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    TaskResponse update(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, request);
    }

    @DeleteMapping("/tasks/{taskId}")
    ResponseEntity<Void> delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
        return ResponseEntity.noContent().build();
    }
}
