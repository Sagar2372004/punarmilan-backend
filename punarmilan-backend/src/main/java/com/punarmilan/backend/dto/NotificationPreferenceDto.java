package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {
    private boolean emailAlerts;
    private boolean webNotifications;
    private boolean matchMail;
    private boolean visitorAlerts;
    private boolean messageAlerts;
    private boolean shortlistAlerts;
}
