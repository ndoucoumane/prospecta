package com.prospecta.conversation.controller;

import com.prospecta.conversation.dto.ConversationDto.ConversationDetailResponse;
import com.prospecta.conversation.service.ConversationService;
import com.prospecta.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController conversationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conversationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/conversations/conv-1 returns 404 RESOURCE_NOT_FOUND (not 500)")
    void shouldReturn404ForMockStringId() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/conv-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Resource with identifier 'conv-1' not found"));
    }

    @Test
    @DisplayName("GET /api/v1/conversations/{id} returns conversation detail when valid UUID")
    void shouldReturnConversationDetail() throws Exception {
        UUID convId = UUID.randomUUID();
        ConversationDetailResponse detail = ConversationDetailResponse.builder()
                .id(convId)
                .messages(Collections.emptyList())
                .build();

        when(conversationService.getConversationById(eq(convId))).thenReturn(detail);

        mockMvc.perform(get("/api/v1/conversations/" + convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(convId.toString()));
    }
}
