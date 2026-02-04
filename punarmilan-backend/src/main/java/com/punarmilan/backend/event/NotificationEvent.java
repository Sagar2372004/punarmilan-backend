package com.punarmilan.backend.event;

import com.punarmilan.backend.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    private final User recipient;
    private final User sender;
    private final String type;
    private final String message;
    private final Object relatedObject;

    public NotificationEvent(Object source, User recipient, User sender, String type, String message,
            Object relatedObject) {
        super(source);
        this.recipient = recipient;
        this.sender = sender;
        this.type = type;
        this.message = message;
        this.relatedObject = relatedObject;
    }
}
