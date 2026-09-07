package com.prospecta.prospect.dto;

import com.prospecta.prospect.domain.LeadScoreLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoreResult {

    private int score;
    private LeadScoreLevel level;
    private List<String> reasons;
}
