package com.prospecta.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prospecta.ai.dto.AiDto.CompanyAnalysisResult;
import com.prospecta.ai.dto.AiDto.GenerateMessageRequest;
import com.prospecta.ai.dto.AiDto.GenerateMessageResult;
import com.prospecta.ai.service.CompanyAnalysisService;
import com.prospecta.ai.service.MessageGenerationService;
import com.prospecta.ai.service.ProspectSummaryService;
import com.prospecta.ai.service.QuotaService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CompanyAnalysisService companyAnalysisService;

    @Mock
    private ProspectSummaryService prospectSummaryService;

    @Mock
    private MessageGenerationService messageGenerationService;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private AiController aiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/companies/{id}/ai/analyze - should return structured AI analysis")
    void shouldAnalyzeCompany() throws Exception {
        UUID companyId = UUID.randomUUID();
        CompanyAnalysisResult result = CompanyAnalysisResult.builder()
                .summary("Entreprise sénégalaise en croissance")
                .industry("COMMERCE")
                .painPoints(List.of("Relances manuelles"))
                .opportunities(List.of("WhatsApp"))
                .recommendedApproach("Approche consultative")
                .confidence(0.91)
                .build();

        when(companyAnalysisService.analyzeCompany(companyId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/ai/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").value("Entreprise sénégalaise en croissance"))
                .andExpect(jsonPath("$.data.industry").value("COMMERCE"))
                .andExpect(jsonPath("$.data.confidence").value(0.91));
    }

    @Test
    @DisplayName("POST /api/v1/ai/messages/generate - should generate personalized message")
    void shouldGenerateMessage() throws Exception {
        GenerateMessageRequest request = GenerateMessageRequest.builder()
                .channel("WHATSAPP")
                .prospectId(UUID.randomUUID())
                .offerDescription("Plateforme de vente automatisée")
                .build();

        GenerateMessageResult result = GenerateMessageResult.builder()
                .channel("WHATSAPP")
                .body("Bonjour M. Ndiaye, seriez-vous disponible pour un court échange ?")
                .callToAction("Dispo jeudi 10h ?")
                .build();

        when(messageGenerationService.generateMessage(any(GenerateMessageRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/messages/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.data.body").isNotEmpty());
    }
}
