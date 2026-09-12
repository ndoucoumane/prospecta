package com.prospecta.task.service;

import com.prospecta.shared.exception.TaskNotFoundException;
import com.prospecta.shared.security.TenantContextHolder;
import com.prospecta.task.domain.CommercialTask;
import com.prospecta.task.dto.TaskDto.*;
import com.prospecta.task.repository.CommercialTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommercialTaskService {

    private final CommercialTaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<CommercialTaskResponse> getTasks(String status, UUID prospectId) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        List<CommercialTask> tasks = taskRepository.findFiltered(organizationId, status, prospectId);
        return tasks.stream().map(CommercialTaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CommercialTaskResponse getTaskById(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        CommercialTask task = taskRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return CommercialTaskResponse.from(task);
    }

    @Transactional
    public CommercialTaskResponse createTask(CreateCommercialTaskRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();

        UUID prospectUuid = null;
        if (request.getProspectId() != null && !request.getProspectId().isBlank()) {
            try {
                prospectUuid = UUID.fromString(request.getProspectId());
            } catch (IllegalArgumentException ignored) {
            }
        }

        CommercialTask task = CommercialTask.builder()
                .type(request.getType() != null ? request.getType().toLowerCase() : "call")
                .title(request.getTitle())
                .prospectId(prospectUuid)
                .prospectName(request.getProspectName())
                .companyName(request.getCompanyName())
                .phone(request.getPhone())
                .dueDate(request.getDueDate())
                .dueTime(request.getDueTime() != null ? request.getDueTime() : "09:00")
                .assignedTo(request.getAssignedTo() != null ? request.getAssignedTo() : "Moi")
                .status("pending")
                .notes(request.getNotes())
                .build();

        task.setOrganizationId(organizationId);
        CommercialTask saved = taskRepository.save(task);
        log.info("Created commercial task [{}] for organization [{}]", saved.getId(), organizationId);
        return CommercialTaskResponse.from(saved);
    }

    @Transactional
    public CommercialTaskResponse updateTask(UUID id, UpdateCommercialTaskRequest request) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        CommercialTask task = taskRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new TaskNotFoundException(id));

        if (request.getType() != null && !request.getType().isBlank()) {
            task.setType(request.getType().toLowerCase());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            task.setStatus(request.getStatus().toLowerCase());
        }
        if (request.getDueDate() != null && !request.getDueDate().isBlank()) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getDueTime() != null) {
            task.setDueTime(request.getDueTime());
        }
        if (request.getAssignedTo() != null) {
            task.setAssignedTo(request.getAssignedTo());
        }
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        CommercialTask saved = taskRepository.save(task);
        log.info("Updated commercial task [{}] for organization [{}]", saved.getId(), organizationId);
        return CommercialTaskResponse.from(saved);
    }

    @Transactional
    public void deleteTask(UUID id) {
        UUID organizationId = TenantContextHolder.getRequiredOrganizationId();
        CommercialTask task = taskRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
        log.info("Deleted commercial task [{}] for organization [{}]", id, organizationId);
    }
}
