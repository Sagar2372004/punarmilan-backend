package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.ConnectionRequestDto;
import com.punarmilan.backend.dto.ConnectionResponseDto;
import com.punarmilan.backend.dto.ConnectionResponseDto.ResponseRequest;
import com.punarmilan.backend.dto.ConnectionResponseDto.UserBasicDto;
import com.punarmilan.backend.entity.ConnectionRequest;
import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.BadRequestException;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.ConnectionRequestRepository;
import com.punarmilan.backend.repository.ProfileRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.ConnectionService;
import com.punarmilan.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRequestRepository connectionRequestRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Override
    public ConnectionResponseDto sendConnectionRequest(ConnectionRequestDto requestDto) {
        User currentUser = getCurrentUser();
        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validations
        validateConnectionRequest(currentUser, receiver);

        // Check if user is blocked
        if (isUserBlockedBy(currentUser, receiver)) {
            throw new BadRequestException("You cannot send a request to this user. You have been blocked.");
        }

        // Check if you have blocked this user
        if (isUserBlockedBy(receiver, currentUser)) {
            throw new BadRequestException("You have blocked this user. Unblock them first to send a request.");
        }

        // Check if request already exists
        connectionRequestRepository.findBySenderAndReceiver(currentUser, receiver).ifPresent(existing -> {
            if (existing.getStatus() == ConnectionRequest.Status.PENDING) {
                throw new BadRequestException("Connection request already sent");
            } else if (existing.getStatus() == ConnectionRequest.Status.ACCEPTED) {
                throw new BadRequestException("You are already connected with this user");
            } else if (existing.getStatus() == ConnectionRequest.Status.REJECTED) {
                throw new BadRequestException("Your previous request was rejected");
            } else if (existing.getStatus() == ConnectionRequest.Status.BLOCKED) {
                throw new BadRequestException("You cannot send a request to this user");
            }
        });

        // Create and save connection request
        ConnectionRequest request = ConnectionRequest.builder()
                .sender(currentUser)
                .receiver(receiver)
                .message(requestDto.getMessage())
                .status(ConnectionRequest.Status.PENDING)
                .read(false)
                .build();

        ConnectionRequest savedRequest = connectionRequestRepository.save(request);
        log.info("Connection request sent from {} to {}", currentUser.getEmail(), receiver.getEmail());

        // Send notification to receiver
        notificationService.createNotification(
                receiver.getId(),
                "CONNECTION_REQUEST",
                "New Connection Request",
                String.format("%s wants to connect with you", getUserDisplayName(currentUser)),
                savedRequest.getId(),
                "CONNECTION_REQUEST"
        );

        return mapToResponseDto(savedRequest);
    }

    @Override
    public ConnectionResponseDto respondToRequest(Long requestId, ResponseRequest response) {
        ConnectionRequest request = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        User currentUser = getCurrentUser();

        // Verify current user is the receiver
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to respond to this request");
        }

        // Verify request is still pending
        if (request.getStatus() != ConnectionRequest.Status.PENDING) {
            throw new BadRequestException("This request has already been responded to");
        }

        // Check if sender has blocked the receiver
        if (isUserBlockedBy(request.getSender(), currentUser)) {
            throw new BadRequestException("You cannot respond to this request. The sender has blocked you.");
        }

        // Update request status
        if (response.isAccept()) {
            request.setStatus(ConnectionRequest.Status.ACCEPTED);
            request.setResponseMessage(response.getResponseMessage());

            // Send notification to sender
            notificationService.createNotification(
                    request.getSender().getId(),
                    "CONNECTION_ACCEPTED",
                    "Connection Request Accepted",
                    String.format("%s accepted your connection request", getUserDisplayName(currentUser)),
                    request.getId(),
                    "CONNECTION_REQUEST"
            );

            log.info("Connection request {} accepted", requestId);
        } else {
            request.setStatus(ConnectionRequest.Status.REJECTED);
            request.setResponseMessage(response.getResponseMessage());

            // Send notification to sender
            notificationService.createNotification(
                    request.getSender().getId(),
                    "CONNECTION_REJECTED",
                    "Connection Request Declined",
                    String.format("%s declined your connection request", getUserDisplayName(currentUser)),
                    request.getId(),
                    "CONNECTION_REQUEST"
            );

            log.info("Connection request {} rejected", requestId);
        }

        request.setRead(true);
        ConnectionRequest updatedRequest = connectionRequestRepository.save(request);

        return mapToResponseDto(updatedRequest);
    }

    @Override
    public void withdrawRequest(Long requestId) {
        ConnectionRequest request = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        User currentUser = getCurrentUser();

        // Verify current user is the sender
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only withdraw requests you sent");
        }

        // Verify request is still pending
        if (request.getStatus() != ConnectionRequest.Status.PENDING) {
            throw new BadRequestException("Cannot withdraw a request that has been responded to");
        }

        request.setStatus(ConnectionRequest.Status.WITHDRAWN);
        connectionRequestRepository.save(request);

        log.info("Connection request {} withdrawn by sender", requestId);
    }

    @Override
    public void blockUser(Long userId) {
        User currentUser = getCurrentUser();
        User userToBlock = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Cannot block yourself
        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("You cannot block yourself");
        }

        // Check if already blocked
        boolean alreadyBlocked = connectionRequestRepository
                .findBySenderAndReceiver(currentUser, userToBlock)
                .map(cr -> cr.getStatus() == ConnectionRequest.Status.BLOCKED)
                .orElse(false);
        
        if (alreadyBlocked) {
            throw new BadRequestException("User is already blocked");
        }

        // Create or update connection request to blocked status
        ConnectionRequest existingRequest = connectionRequestRepository
                .findBySenderAndReceiver(currentUser, userToBlock)
                .orElse(null);

        if (existingRequest != null) {
            existingRequest.setStatus(ConnectionRequest.Status.BLOCKED);
            existingRequest.setMessage("User blocked by sender");
            connectionRequestRepository.save(existingRequest);
        } else {
            // Create a new blocked request
            ConnectionRequest blockedRequest = ConnectionRequest.builder()
                    .sender(currentUser)
                    .receiver(userToBlock)
                    .status(ConnectionRequest.Status.BLOCKED)
                    .message("User blocked by sender")
                    .read(true)
                    .build();
            connectionRequestRepository.save(blockedRequest);
        }

        // Also check reverse direction and update if pending
        connectionRequestRepository
                .findBySenderAndReceiver(userToBlock, currentUser)
                .ifPresent(request -> {
                    if (request.getStatus() == ConnectionRequest.Status.PENDING) {
                        request.setStatus(ConnectionRequest.Status.REJECTED);
                        request.setResponseMessage("User blocked you");
                        connectionRequestRepository.save(request);
                    }
                });

        log.info("User {} blocked by {}", userToBlock.getEmail(), currentUser.getEmail());
    }

    @Override
    public void unblockUser(Long userId) {
        User currentUser = getCurrentUser();
        User userToUnblock = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find the blocked request
        ConnectionRequest blockedRequest = connectionRequestRepository
                .findBySenderAndReceiver(currentUser, userToUnblock)
                .orElseThrow(() -> new BadRequestException("User is not blocked"));

        if (blockedRequest.getStatus() != ConnectionRequest.Status.BLOCKED) {
            throw new BadRequestException("User is not blocked");
        }

        // Delete the blocked request
        connectionRequestRepository.delete(blockedRequest);

        log.info("User {} unblocked by {}", userToUnblock.getEmail(), currentUser.getEmail());
    }

    @Override
    public Page<UserBasicDto> getBlockedUsers(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Get all requests where current user is sender and status is BLOCKED
        List<ConnectionRequest> blockedRequests = connectionRequestRepository.findAll().stream()
                .filter(request -> request.getSender().getId().equals(currentUser.getId()))
                .filter(request -> request.getStatus() == ConnectionRequest.Status.BLOCKED)
                .collect(Collectors.toList());
        
        // Map to UserBasicDto
        List<UserBasicDto> blockedUsers = blockedRequests.stream()
                .map(request -> {
                    User blockedUser = request.getReceiver();
                    Profile profile = profileRepository.findByUser(blockedUser)
                            .orElse(new Profile());
                    
                    return UserBasicDto.builder()
                            .id(blockedUser.getId())
                            .email(blockedUser.getEmail())
                            .fullName(profile.getFullName())
                            .gender(profile.getGender())
                            .age(profile.getAge())
                            .city(profile.getCity())
                            .profilePhotoUrl(profile.getProfilePhotoUrl())
                            .isVerified(profile.isVerified())
                            .build();
                })
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), blockedUsers.size());
        List<UserBasicDto> paginatedList = blockedUsers.subList(start, end);
        
        return new PageImpl<>(paginatedList, pageable, blockedUsers.size());
    }

    @Override
    public boolean isUserBlocked(Long userId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if current user is blocked by other user
        return isUserBlockedBy(otherUser, currentUser);
    }

    @Override
    public Page<ConnectionResponseDto> getPendingRequests(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ConnectionRequest> requests = connectionRequestRepository
                .findByReceiverAndStatus(currentUser, ConnectionRequest.Status.PENDING, pageable);

        return requests.map(this::mapToResponseDto);
    }

    @Override
    public Page<ConnectionResponseDto> getSentRequests(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ConnectionRequest> requests = connectionRequestRepository
                .findBySenderAndStatus(currentUser, ConnectionRequest.Status.PENDING, pageable);

        return requests.map(this::mapToResponseDto);
    }

    @Override
    public Page<ConnectionResponseDto> getConnections(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ConnectionRequest> connections = connectionRequestRepository
                .findConnectionsByUser(currentUser, pageable);

        return connections.map(this::mapToResponseDto);
    }

    @Override
    public ConnectionResponseDto getRequestById(Long requestId) {
        ConnectionRequest request = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        User currentUser = getCurrentUser();

        // Verify current user is either sender or receiver
        if (!request.getSender().getId().equals(currentUser.getId()) &&
            !request.getReceiver().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to view this request");
        }

        return mapToResponseDto(request);
    }

    @Override
    public void markAsRead(Long requestId) {
        ConnectionRequest request = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        User currentUser = getCurrentUser();

        // Verify current user is the receiver
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not authorized to mark this request as read");
        }

        request.setRead(true);
        connectionRequestRepository.save(request);
        log.debug("Connection request {} marked as read", requestId);
    }

    @Override
    public Map<String, Object> getConnectionStats() {
        User currentUser = getCurrentUser();

        Map<String, Object> stats = new HashMap<>();

        // Count pending requests
        long pendingRequests = connectionRequestRepository
                .findByReceiverAndStatus(currentUser, ConnectionRequest.Status.PENDING, Pageable.unpaged())
                .getTotalElements();
        stats.put("pendingRequests", pendingRequests);

        // Count sent requests
        long sentRequests = connectionRequestRepository
                .findBySenderAndStatus(currentUser, ConnectionRequest.Status.PENDING, Pageable.unpaged())
                .getTotalElements();
        stats.put("sentRequests", sentRequests);

        // Count connections
        long connections = connectionRequestRepository
                .findConnectionsByUser(currentUser, Pageable.unpaged())
                .getTotalElements();
        stats.put("connections", connections);

        // Count blocked users
        long blockedUsers = connectionRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(currentUser.getId()))
                .filter(r -> r.getStatus() == ConnectionRequest.Status.BLOCKED)
                .count();
        stats.put("blockedUsers", blockedUsers);

        // Count today's sent requests
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todaysRequests = connectionRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(currentUser.getId()))
                .filter(r -> r.getSentAt() != null && r.getSentAt().isAfter(todayStart))
                .count();
        stats.put("todaysRequests", todaysRequests);
        stats.put("dailyLimit", 20); // Your daily limit

        return stats;
    }

    @Override
    public boolean canSendRequest(Long receiverId) {
        User currentUser = getCurrentUser();

        if (currentUser.getId().equals(receiverId)) {
            return false; // Cannot send request to self
        }

        try {
            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
            // Check if user is blocked
            if (isUserBlockedBy(currentUser, receiver) || isUserBlockedBy(receiver, currentUser)) {
                return false;
            }
            
            validateConnectionRequest(currentUser, receiver);
            return true;
        } catch (BadRequestException | ResourceNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean areUsersConnected(Long otherUserId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if there's an accepted connection request in either direction
        return connectionRequestRepository.areUsersConnected(currentUser, otherUser);
    }

    @Override
    public Page<ConnectionResponseDto> getMutualConnections(Long userId, Pageable pageable) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get current user's connections
        List<ConnectionRequest> myConnections = connectionRequestRepository
                .findConnectionsByUser(currentUser, Pageable.unpaged())
                .getContent();

        // Get target user's connections
        List<ConnectionRequest> targetConnections = connectionRequestRepository
                .findConnectionsByUser(targetUser, Pageable.unpaged())
                .getContent();

        // Find mutual connections
        List<ConnectionResponseDto> mutualConnections = new ArrayList<>();
        for (ConnectionRequest connection : myConnections) {
            User connectedUser = connection.getSender().getId().equals(currentUser.getId()) ?
                    connection.getReceiver() : connection.getSender();

            // Check if this user is also connected to targetUser
            boolean isMutual = targetConnections.stream()
                    .anyMatch(targetConnection ->
                            targetConnection.getSender().getId().equals(connectedUser.getId()) ||
                            targetConnection.getReceiver().getId().equals(connectedUser.getId()));

            if (isMutual) {
                mutualConnections.add(mapToResponseDto(connection));
            }
        }

        // Create paginated result
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), mutualConnections.size());
        List<ConnectionResponseDto> paginatedList = mutualConnections.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, mutualConnections.size());
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateConnectionRequest(User sender, User receiver) {
        // Cannot send to self
        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Cannot send connection request to yourself");
        }

        // Check if receiver exists and is active
        if (!receiver.isActive()) {
            throw new BadRequestException("User account is not active");
        }

        // Check if receiver has completed profile
        Profile receiverProfile = profileRepository.findByUser(receiver)
                .orElseThrow(() -> new BadRequestException("User profile not found"));

        if (!receiverProfile.isProfileComplete()) {
            throw new BadRequestException("User profile is not complete");
        }

        // Check gender compatibility
        Profile senderProfile = profileRepository.findByUser(sender)
                .orElseThrow(() -> new BadRequestException("Please complete your profile first"));

        String senderGender = senderProfile.getGender();
        String receiverGender = receiverProfile.getGender();

        // Traditional matchmaking: Male can only connect with Female and vice versa
        if (senderGender != null && receiverGender != null) {
            if ("Male".equalsIgnoreCase(senderGender) && !"Female".equalsIgnoreCase(receiverGender)) {
                throw new BadRequestException("You can only send connection requests to Female profiles");
            }
            if ("Female".equalsIgnoreCase(senderGender) && !"Male".equalsIgnoreCase(receiverGender)) {
                throw new BadRequestException("You can only send connection requests to Male profiles");
            }
        }

        // Check daily limit
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayRequests = connectionRequestRepository.findAll().stream()
                .filter(r -> r.getSender().getId().equals(sender.getId()))
                .filter(r -> r.getSentAt() != null && r.getSentAt().isAfter(todayStart))
                .count();

        if (todayRequests >= 20) { // Limit to 20 requests per day
            throw new BadRequestException("Daily connection request limit reached (20 requests per day)");
        }
    }

    private String getUserDisplayName(User user) {
        return profileRepository.findByUser(user)
                .map(Profile::getFullName)
                .orElse(user.getEmail());
    }

    private boolean isUserBlockedBy(User blocker, User blockedUser) {
        return connectionRequestRepository
                .findBySenderAndReceiver(blocker, blockedUser)
                .map(cr -> cr.getStatus() == ConnectionRequest.Status.BLOCKED)
                .orElse(false);
    }

    private ConnectionResponseDto mapToResponseDto(ConnectionRequest request) {
        Profile senderProfile = profileRepository.findByUser(request.getSender())
                .orElse(new Profile());
        Profile receiverProfile = profileRepository.findByUser(request.getReceiver())
                .orElse(new Profile());

        User currentUser = getCurrentUser();
        boolean isSender = request.getSender().getId().equals(currentUser.getId());
        boolean isReceiver = request.getReceiver().getId().equals(currentUser.getId());
        
        // Check if user is blocked
        boolean isBlocked = request.getStatus() == ConnectionRequest.Status.BLOCKED;

        return ConnectionResponseDto.builder()
                .id(request.getId())
                .requestId(request.getId())
                .sender(ConnectionResponseDto.SenderInfo.builder()
                        .id(request.getSender().getId())
                        .email(request.getSender().getEmail())
                        .fullName(senderProfile.getFullName())
                        .gender(senderProfile.getGender())
                        .age(senderProfile.getAge())
                        .city(senderProfile.getCity())
                        .profilePhotoUrl(senderProfile.getProfilePhotoUrl())
                        .isVerified(senderProfile.isVerified())
                        .occupation(senderProfile.getOccupation())
                        .education(senderProfile.getEducationLevel())
                        .build())
                .senderProfilePhoto(senderProfile.getProfilePhotoUrl())
                .receiver(ConnectionResponseDto.ReceiverInfo.builder()
                        .id(request.getReceiver().getId())
                        .email(request.getReceiver().getEmail())
                        .fullName(receiverProfile.getFullName())
                        .gender(receiverProfile.getGender())
                        .age(receiverProfile.getAge())
                        .city(receiverProfile.getCity())
                        .profilePhotoUrl(receiverProfile.getProfilePhotoUrl())
                        .isVerified(receiverProfile.isVerified())
                        .occupation(receiverProfile.getOccupation())
                        .education(receiverProfile.getEducationLevel())
                        .build())
                .receiverProfilePhoto(receiverProfile.getProfilePhotoUrl())
                .status(request.getStatus().name())
                .message(request.getMessage())
                .read(request.isRead())
                .sentAt(request.getSentAt())
                .respondedAt(request.getRespondedAt())
                .responseMessage(request.getResponseMessage())
                .canWithdraw(isSender && request.getStatus() == ConnectionRequest.Status.PENDING)
                .canAccept(isReceiver && request.getStatus() == ConnectionRequest.Status.PENDING)
                .canReject(isReceiver && request.getStatus() == ConnectionRequest.Status.PENDING)
                .isBlocked(isBlocked)
                .build();
    }
}