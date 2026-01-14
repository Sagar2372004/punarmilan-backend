package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.ConnectionRequestDto;
import com.punarmilan.backend.dto.ConnectionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ConnectionService {

    // Send connection request
    ConnectionResponseDto sendConnectionRequest(ConnectionRequestDto requestDto);
    
    // Respond to connection request
    ConnectionResponseDto respondToRequest(Long requestId, ConnectionResponseDto.ResponseRequest response);
    
    // Withdraw sent request
    void withdrawRequest(Long requestId);
    
    // Get pending requests received
    Page<ConnectionResponseDto> getPendingRequests(Pageable pageable);
    
    // Get sent requests
    Page<ConnectionResponseDto> getSentRequests(Pageable pageable);
    
    // Get connections (accepted requests)
    Page<ConnectionResponseDto> getConnections(Pageable pageable);
    
    // Get request by ID
    ConnectionResponseDto getRequestById(Long requestId);
    
    // Mark request as read
    void markAsRead(Long requestId);
    
    // Get connection stats
    Map<String, Object> getConnectionStats();
    
    // Check if can send request (validation)
    boolean canSendRequest(Long receiverId);
    
    // Check if two users are connected
    boolean areUsersConnected(Long otherUserId);
    
    // Get mutual connections
    Page<ConnectionResponseDto> getMutualConnections(Long userId, Pageable pageable);
    
    // Comment out unimplemented methods for now
   
    // Block/Unblock users
    void blockUser(Long userId);
    void unblockUser(Long userId);
    Page<ConnectionResponseDto.UserBasicDto> getBlockedUsers(Pageable pageable);
    
    // Search connections
  //  Page<ConnectionResponseDto> searchConnections(String name, String city, Pageable pageable);
    
    // Export connections
   // ByteArrayInputStream exportConnectionsToCSV();
    
    // Get connection suggestions
   // Page<ConnectionResponseDto> getConnectionSuggestions(Pageable pageable);
    
    // Check if user is blocked
    boolean isUserBlocked(Long userId);
    
    // Get connection history
  //  Page<ConnectionResponseDto> getConnectionHistory(Pageable pageable);
    
}