package com.nextask.workspace;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nextask.workspace.entity.Workspace;

import jakarta.persistence.LockModeType;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select workspace from Workspace workspace where workspace.id = :id")
    Optional<Workspace> findByIdForUpdate(@Param("id") Long id);
}
