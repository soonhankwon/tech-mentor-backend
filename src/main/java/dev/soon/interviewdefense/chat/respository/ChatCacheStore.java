package dev.soon.interviewdefense.chat.respository;

import dev.soon.interviewdefense.chat.domain.Chat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatCacheStore {

    private final Map<Long, Chat> chatMap;
    private final Map<String, Chat> webSocketSessionUserChatMap;

    public ChatCacheStore() {
        this.chatMap = new ConcurrentHashMap<>();
        this.webSocketSessionUserChatMap = new ConcurrentHashMap<>();
    }

    public void cacheChatSessionIdAndChat(String chatSessionId, Chat chat) {
        this.webSocketSessionUserChatMap.put(chatSessionId, chat);
    }

    public <T> Chat getChatByCacheKey(T key) {
        if(key instanceof Long) {
            log.info("cache hit={}", key);
            return chatMap.get(key);
        }
        if(key instanceof String) {
            log.info("cache hit={}", key);
            return webSocketSessionUserChatMap.get(key);
        }
        throw new IllegalArgumentException("invalid key!!");
    }

    public <T> void removeCache(T key) {
        if(key instanceof Long) {
            this.chatMap.remove(key);
            return;
        }
        if(key instanceof String) {
            this.webSocketSessionUserChatMap.remove(key);
        }
    }
}
