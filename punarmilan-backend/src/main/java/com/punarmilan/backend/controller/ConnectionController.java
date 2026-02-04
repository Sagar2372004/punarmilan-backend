package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.ConnectionRequestDto;

import com.punarmilan.backend.dto.ConnectionResponseDto;
import com.punarmilan.backend.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
@Tag(name = "Connection Management", description = "Manage connection requests between users")
@PreAuthorize("hasRole('USER')")
public class ConnectionController {

    private final ConnectionService connectionService;

    @Operation(summary = "Send connection request")
    @PostMapping("/request")
    public ResponseEntity<ConnectionResponseDto> sendConnectionRequest(
            @Valid @RequestBody ConnectionRequestDto requestDto) {
        ConnectionResponseDto response = connectionService.sendConnectionRequest(requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Respond to connection request")
    @PostMapping("/request/{requestId}/respond")
    public ResponseEntity<ConnectionResponseDto> respondToRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ConnectionResponseDto.ResponseRequest responseRequest) {
        ConnectionResponseDto response = connectionService.respondToRequest(requestId, responseRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Withdraw sent request")
    @DeleteMapping("/request/{requestId}")
    public ResponseEntity<Void> withdrawRequest(@PathVariable Long requestId) {
        connectionService.withdrawRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get pending requests received")
    @GetMapping("/requests/pending")
    public ResponseEntity<Page<ConnectionResponseDto>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sentAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ConnectionResponseDto> requests = connectionService.getPendingRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Get sent requests")
    @GetMapping("/requests/sent")
    public ResponseEntity<Page<ConnectionResponseDto>> getSentRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<ConnectionResponseDto> requests = connectionService.getSentRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Get accepted connections")
    @GetMapping("/connections")
    public ResponseEntity<Page<ConnectionResponseDto>> getConnections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("respondedAt").descending());
        Page<ConnectionResponseDto> connections = connectionService.getConnections(pageable);
        return ResponseEntity.ok(connections);
    }

    @Operation(summary = "Get connection request by ID")
    @GetMapping("/request/{requestId}")
    public ResponseEntity<ConnectionResponseDto> getRequestById(@PathVariable Long requestId) {
        ConnectionResponseDto request = connectionService.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    @Operation(summary = "Mark request as read")
    @PatchMapping("/request/{requestId}/read")
    public ResponseEntity<Void> markRequestAsRead(@PathVariable Long requestId) {
        connectionService.markAsRead(requestId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get connection statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        Map<String, Object> stats = connectionService.getConnectionStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Check if can send request to user")
    @GetMapping("/can-send/{userId}")
    public ResponseEntity<Map<String, Boolean>> canSendRequest(@PathVariable Long userId) {
        boolean canSend = connectionService.canSendRequest(userId);
        return ResponseEntity.ok(Map.of("canSend", canSend));
    }

    @Operation(summary = "Check if connected with user")
    @GetMapping("/connected/{userId}")
    public ResponseEntity<Map<String, Boolean>> areUsersConnected(@PathVariable Long userId) {
        boolean connected = connectionService.areUsersConnected(userId);
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    // Comment out unimplemented methods for now
    
    @Operation(summary = "Block a user (prevent future connections)")
    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId) {
        connectionService.blockUser(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unblock a user")
    @PostMapping("/unblock/{userId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        connectionService.unblockUser(userId);
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "Get mutual connections with a user")
    @GetMapping("/mutual/{userId}")
    public ResponseEntity<Page<ConnectionResponseDto>> getMutualConnections(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ConnectionResponseDto> connections = connectionService.getMutualConnections(userId, pageable);
        return ResponseEntity.ok(connections);
    }
    
    
/*
    @Operation(summary = "Search connections")
    @GetMapping("/search")
    public ResponseEntity<Page<ConnectionResponseDto>> searchConnections(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
        Page<ConnectionResponseDto> connections = connectionService.searchConnections(name, city, pageable);
        return ResponseEntity.ok(connections);
    }

    @Operation(summary = "Export connections (CSV)")
    @GetMapping("/export")
    public ResponseEntity<Resource> exportConnections() {
        String filename = "connections_" + LocalDate.now() + ".csv";
        InputStreamResource file = new InputStreamResource(connectionService.exportConnectionsToCSV());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
    }
    */
    
}