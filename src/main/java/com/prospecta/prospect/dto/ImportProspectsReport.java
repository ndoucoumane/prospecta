package com.prospecta.prospect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportProspectsReport {

    private int total;
    private int created;
    private int duplicates;
    private int invalid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
