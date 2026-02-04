package com.punarmilan.backend.dto;

import lombok.Data;

@Data
public class NotificationRequestDto {
    private String type; // EMAIL, PUSH, IN_APP
    private String recipient;
    private String subject;
    private String message;
    private String templateName;
    private Object templateData;
}
