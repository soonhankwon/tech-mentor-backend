package dev.soon.interviewdefense.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CustomErrorCode {
    NOT_EXISTS_USER_IN_DB("유저정보가 DB에 존재하지 않습니다."),
    NOT_EXISTS_CHATROOM_IN_DB("채팅방정보가 DB에 존재하지 않습니다."),
    NOT_MATCHES_CHAT_ID_AND_USER("채팅방 ID와 유저정보가 일치하지 않습니다."),
    NOT_EXISTS_LATEST_CHAT_MESSAGE("채팅방에 최신 메세지가 존재하지 않습니다."),
    INVALID_FLAG_IN_FRONT("프론트의 Flag 정보가 유효하지 않습니다.");

    private final String message;
}