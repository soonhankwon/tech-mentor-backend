package dev.soon.interviewdefense.chat.domain;

import dev.soon.interviewdefense.chat.controller.dto.ChatRoomReqDto;
import dev.soon.interviewdefense.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@ToString
@Getter
@Entity
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChatTopic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private LocalDateTime createAt;

    public Chat(ChatRoomReqDto dto, User user) {
        this.topic = dto.topic();
        this.user = user;
        this.createAt = LocalDateTime.now();
    }
}