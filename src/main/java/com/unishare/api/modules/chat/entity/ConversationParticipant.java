package com.unishare.api.modules.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "conversation_participants")
@Getter
@Setter
@NoArgsConstructor
public class ConversationParticipant {

    @EmbeddedId
    private ConversationParticipantId id;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
