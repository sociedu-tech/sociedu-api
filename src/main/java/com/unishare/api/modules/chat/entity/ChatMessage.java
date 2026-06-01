package com.unishare.api.modules.chat.entity;

import com.unishare.api.common.constants.MessageTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String type = MessageTypes.TEXT;

    @Column(name = "is_edited")
    private Boolean edited = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "context_type")
    private String contextType;

    @Column(name = "context_id")
    private UUID contextId;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
