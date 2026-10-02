package com.healthsuite.family.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.family.dto.request.AddFamilyMemberRequest;
import com.healthsuite.family.dto.request.UpdateRecordShareRequest;
import com.healthsuite.family.dto.response.FamilyMemberResponse;
import com.healthsuite.family.dto.response.RecordShareResponse;
import com.healthsuite.family.service.FamilyMeshService;
import com.healthsuite.family.service.FamilyRecordShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
@Tag(name = "Family Network", description = "Link family members by phone number; verified ties grant bidirectional PHR access")
public class FamilyMeshController {

    private final FamilyMeshService familyMeshService;
    private final FamilyRecordShareService recordShareService;

    @Operation(summary = "Send a family link request", description = "Looks up the target user by Bangladeshi phone number and creates a PENDING tie.")
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> addFamilyMember(
            @Valid @RequestBody AddFamilyMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Family member request sent",
                        familyMeshService.addFamilyMember(
                                request.phoneNumber(), request.relationship(), principal.getId())));
    }

    @Operation(summary = "My record-sharing grants", description = "What I currently share with each family member.")
    @GetMapping("/shares")
    public ResponseEntity<ApiResponse<List<RecordShareResponse>>> getMyShares(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(recordShareService.getMyShares(principal.getId())));
    }

    @Operation(summary = "Set what a family member can see",
            description = "Scope ALL shares the full record, SELECTED shares chosen visits, NONE withdraws access.")
    @PutMapping("/shares/{granteeUserId}")
    public ResponseEntity<ApiResponse<RecordShareResponse>> updateShare(
            @PathVariable Long granteeUserId,
            @Valid @RequestBody UpdateRecordShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Sharing updated",
                recordShareService.updateShare(principal.getId(), granteeUserId, request)));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> getMyFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(familyMeshService.getMyFamilyMembers(principal.getId())));
    }

    @Operation(summary = "Accept a pending family link request", description = "Only the targeted member (not the initiator) can accept. Moves status to VERIFIED.")
    @PostMapping("/members/{tieId}/accept")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> acceptRequest(
            @PathVariable Long tieId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Family relationship verified",
                familyMeshService.acceptRequest(tieId, principal.getId())));
    }

    @DeleteMapping("/members/{tieId}")
    public ResponseEntity<ApiResponse<Void>> removeFamilyMember(
            @PathVariable Long tieId,
            @AuthenticationPrincipal UserPrincipal principal) {
        familyMeshService.removeFamilyMember(tieId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Family member removed"));
    }
}
