package com.prospecta.conversation.controller;

import com.prospecta.campaign.domain.ChannelType;
import com.prospecta.conversation.domain.ConversationStatus;
import com.prospecta.conversation.dto.ConversationDto.*;
import com.prospecta.conversation.service.ConversationService;
import com.prospecta.shared.dto.ApiResponse;
import com.prospecta.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Unified Inbox & Conversations", description = "Unified inbox, omnichannel messaging threads, and AI reply assistant")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @Operation(summary = "Get Unified Inbox conversations (filtered by channel, status, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<ConversationSummaryResponse>>> getConversations(
            @RequestParam(required = false) ChannelType channel,
            @RequestParam(required = false) ConversationStatus status,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ConversationSummaryResponse> response = conversationService.getConversations(channel, status, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation thread with full message history")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getConversationById(@PathVariable UUID id) {
        ConversationDetailResponse response = conversationService.getConversationById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a reply to the prospect across the conversation channel")
    public ResponseEntity<ApiResponse<ConversationMessageDto>> sendReply(
            @PathVariable UUID id,
            @Valid @RequestBody SendReplyRequest request
    ) {
        ConversationMessageDto response = conversationService.sendReply(id, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/{id}/ai/reply")
    @Operation(summary = "AI Copilot: Generate an intelligent suggested reply based on conversation context")
    public ResponseEntity<ApiResponse<AiReplySuggestion>> generateAiReply(@PathVariable UUID id) {
        AiReplySuggestion response = conversationService.generateAiReply(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
