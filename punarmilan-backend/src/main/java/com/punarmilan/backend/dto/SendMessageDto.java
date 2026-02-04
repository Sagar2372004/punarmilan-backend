package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageDto {
    private Long receiverId;
    private String content;
    private String messageType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Long repliedToMessageId;
}
