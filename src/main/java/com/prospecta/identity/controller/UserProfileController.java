package com.prospecta.identity.controller;

import com.prospecta.identity.dto.UpdateUserProfileRequest;
import com.prospecta.identity.dto.UserProfileResponse;
import com.prospecta.identity.service.UserProfileService;
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

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and organization team management")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        UserProfileResponse response = userProfileService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated user's profile details")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.updateCurrentUserProfile(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "List all user profiles belonging to the active organization")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getOrganizationUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserProfileResponse> response = userProfileService.getUserProfilesByOrganization(pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
