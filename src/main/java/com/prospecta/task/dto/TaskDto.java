package com.prospecta.task.dto;

import com.prospecta.task.domain.CommercialTask;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class TaskDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommercialTaskResponse {
        private String id;
        private String type;
        private String title;
        private String prospectId;
        private String prospectName;
        private String companyName;
        private String phone;
        private String dueDate;
        private String dueTime;
        private String assignedTo;
        private String status;
        private String notes;
        private String createdAt;
        private String updatedAt;

        public static CommercialTaskResponse from(CommercialTask task) {
            return CommercialTaskResponse.builder()
                    .id(task.getId() != null ? task.getId().toString() : null)
                    .type(task.getType())
                    .title(task.getTitle())
                    .prospectId(task.getProspectId() != null ? task.getProspectId().toString() : null)
                    .prospectName(task.getProspectName())
                    .companyName(task.getCompanyName())
                    .phone(task.getPhone())
                    .dueDate(task.getDueDate())
                    .dueTime(task.getDueTime())
                    .assignedTo(task.getAssignedTo())
                    .status(task.getStatus())
                    .notes(task.getNotes())
                    .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : Instant.now().toString())
                    .updatedAt(task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : Instant.now().toString())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommercialTaskRequest {
        @NotBlank(message = "Task type is required")
        private String type;

        @NotBlank(message = "Task title is required")
        private String title;

        private String prospectId;
        private String prospectName;
        private String companyName;
        private String phone;

        @NotBlank(message = "Due date is required")
        private String dueDate;

        private String dueTime;
        private String assignedTo;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCommercialTaskRequest {
        private String type;
        private String title;
        private String status;
        private String dueDate;
        private String dueTime;
        private String assignedTo;
        private String notes;
    }
}
