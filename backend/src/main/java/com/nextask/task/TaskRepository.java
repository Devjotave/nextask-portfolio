package com.nextask.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nextask.task.entity.Task;
import com.nextask.task.entity.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"project", "project.workspace", "assignee", "createdBy"})
    Page<Task> findByProject_Id(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "project.workspace", "assignee", "createdBy"})
    Page<Task> findByProject_IdAndStatus(
            Long projectId,
            TaskStatus status,
            Pageable pageable);
}
