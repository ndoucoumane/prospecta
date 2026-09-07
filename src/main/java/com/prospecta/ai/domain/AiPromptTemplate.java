package com.prospecta.ai.domain;

import com.prospecta.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "ai_prompt_templates")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", columnDefinition = "TEXT")
    private String userPromptTemplate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
