package com.punarmilan.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private PhotoDto senderPhotoUrl;
    private Long receiverId;
    private String receiverName;
    private PhotoDto receiverPhotoUrl;
    private Long conversationId;
    private String content;
    private String messageType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private boolean read;
    private boolean delivered;
    private Long repliedToMessageId;
    private MessageDto repliedToMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private boolean isMine; // For frontend to identify own messages
}
