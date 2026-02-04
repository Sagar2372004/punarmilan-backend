package com.punarmilan.backend.dto;

import lombok.Data;

@Data
public class WebhookEventDto {
    private String type;
    private String id;
    private Object data;
}
